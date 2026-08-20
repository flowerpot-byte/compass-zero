package org.compasszero.karte

import java.io.File
import java.util.zip.Deflater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.compasszero.security.Ed25519
import org.compasszero.security.PackFormat
import org.compasszero.security.PackVerdict
import org.compasszero.security.PackWriter
import org.compasszero.security.TrustStore
import org.compasszero.security.TrustedKey

/**
 * Prueft den Umschlag um eine Kartendatei.
 *
 * Die Karte ist der erste Datenbestand des Projekts, der nicht mehr ins APK
 * passt und deshalb einzeln reist -- ueber eine Speicherkarte, ueber
 * Bluetooth, von einem fremden Rechner. Ab da ist die Unterschrift das
 * einzige, was zwischen dem Nutzer und einer erfundenen Karte steht. Eine
 * erfundene Karte ist keine Kleinigkeit: Sie kann eine Quelle zeigen, wo
 * keine ist.
 */
class KartenumschlagTest {

    private fun packe(roh: ByteArray): ByteArray {
        val packer = Deflater(9)
        packer.setInput(roh)
        packer.finish()
        val aus = ByteArray(roh.size * 2 + 64)
        val n = packer.deflate(aus)
        packer.end()
        return aus.copyOf(n)
    }

    private fun kachelbytes(): ByteArray {
        val b = ArrayList<Byte>()
        fun roh(w: Int) = b.add((w and 0xFF).toByte())
        fun varint(w: Int) {
            var r = w
            while (r >= 128) {
                b.add(((r and 127) or 128).toByte()); r = r ushr 7
            }
            b.add(r.toByte())
        }
        fun zigzag(w: Int) = varint((w shl 1) xor (w shr 31))
        roh(Kartenformat.FASSUNG)
        varint(0)
        varint(1)
        roh(Kartenformat.GRENZE)
        varint(1)
        roh(Kartenformat.Art.LINIE)
        varint(0)
        varint(0)
        varint(2)
        zigzag(10); zigzag(20)
        zigzag(300); zigzag(-40)
        return b.toByteArray()
    }

    /** Baut eine blanke, gueltige Kartendatei mit einer Kachel. */
    private fun blankeKarte(ziel: File) {
        val inhalt = packe(kachelbytes())
        val anfang = Kartenformat.KOPF_BYTES + Kartenformat.EINTRAG_BYTES
        val kopf = ByteArray(Kartenformat.KOPF_BYTES)
        Kartenformat.KENNUNG.copyInto(kopf, 0)
        kopf[8] = Kartenformat.FASSUNG.toByte()
        kopf[9] = 12
        kopf[10] = 5
        kopf[11] = 5
        schreibeInt(kopf, 12, 96_000_000)
        schreibeInt(kopf, 16, 464_000_000)
        schreibeInt(kopf, 20, 172_000_000)
        schreibeInt(kopf, 24, 490_000_000)
        schreibeInt(kopf, 28, 1)
        for (i in 0 until 8) kopf[32 + i] = (anfang.toLong() shr (8 * i)).toByte()

        ziel.outputStream().use { aus ->
            aus.write(kopf)
            aus.write(5)
            for (i in 0 until 4) aus.write((16 shr (8 * i)) and 0xFF)
            for (i in 0 until 4) aus.write((11 shr (8 * i)) and 0xFF)
            for (i in 0 until 8) aus.write(((anfang.toLong() shr (8 * i)) and 0xFF).toInt())
            for (i in 0 until 4) aus.write((inhalt.size shr (8 * i)) and 0xFF)
            aus.write(inhalt)
        }
    }

    private fun schreibeInt(roh: ByteArray, p: Int, w: Int) {
        for (i in 0 until 4) roh[p + i] = (w shr (8 * i)).toByte()
    }

    private fun temp(name: String) =
        File.createTempFile("umschlag-$name", ".czk").also { it.delete(); it.deleteOnExit() }

    private val samen = ByteArray(32) { (it * 7 + 3).toByte() }
    private val oeffentlich by lazy { Ed25519.publicKeyFromSeed(samen) }

    private fun signierteKarte(name: String): File {
        val blank = temp("$name-blank")
        blankeKarte(blank)
        val ziel = temp("$name-signiert")
        PackWriter.writeMitPruefsumme(blank.readBytes(), samen, ziel, PackFormat.KARTE_MAGIC)
        return ziel
    }

    @Test
    fun eineSignierteKarteWirdGeoeffnetUndDerSigniererGenannt() {
        val datei = signierteKarte("gut")
        val speicher = TrustStore(listOf(TrustedKey("entwicklung", oeffentlich)))
        Kartenumschlag.oeffne(datei, speicher).use { offen ->
            assertTrue(offen.unterschrieben)
            assertTrue(offen.geprueft)
            assertEquals("entwicklung", (offen.urteil as PackVerdict.Trusted).signer.name)
            // Und die Karte dahinter ist wirklich lesbar -- ein Umschlag, der
            // aufgeht, sagt noch nichts ueber seinen Inhalt.
            val kachel = offen.datei.kachel(5, 16, 11)
            assertTrue(kachel != null)
            assertEquals(Kartenformat.GRENZE, kachel!!.sorte[0].toInt())
            assertEquals(listOf(10, 310), kachel.x.toList())
        }
    }

    @Test
    fun eineFremdSignierteKarteWirdGeoeffnetAberNichtAlsGeprueftGemeldet() {
        val datei = signierteKarte("fremd")
        Kartenumschlag.oeffne(datei, TrustStore(emptyList())).use { offen ->
            assertTrue(offen.unterschrieben)
            assertTrue(!offen.geprueft)
            assertTrue(offen.urteil is PackVerdict.UnknownSigner)
        }
    }

    @Test
    fun eineKarteOhneUmschlagWirdGeoeffnetUndAlsUnsigniertGemeldet() {
        val datei = temp("blank")
        blankeKarte(datei)
        Kartenumschlag.oeffne(datei, TrustStore(emptyList())).use { offen ->
            assertNull(offen.urteil)
            assertTrue(!offen.unterschrieben)
            assertTrue(!offen.geprueft)
            assertEquals(1, offen.datei.kachelzahl)
        }
    }

    /**
     * Ein einziges gekipptes Bit in der Nutzlast muss auffallen -- und die
     * Karte darf danach NICHT geoeffnet werden. Das ist der Unterschied zu
     * einer unbekannt signierten Karte: dort weiss man nur nicht, wer sie
     * gemacht hat; hier weiss man, dass sie nicht mehr die ist, die jemand
     * unterschrieben hat.
     */
    @Test
    fun eineVeraenderteKarteWirdAbgelehnt() {
        val datei = signierteKarte("kaputt")
        val bytes = datei.readBytes()
        val mitte = PackFormat.HEADER_SIZE + (bytes.size - PackFormat.HEADER_SIZE) / 2
        bytes[mitte] = (bytes[mitte].toInt() xor 1).toByte()
        datei.writeBytes(bytes)
        val speicher = TrustStore(listOf(TrustedKey("entwicklung", oeffentlich)))
        val fehler = assertFailsWith<Kartenfehler> { Kartenumschlag.oeffne(datei, speicher) }
        assertTrue(fehler.message!!.contains("MANIPULIERT"))
    }

    @Test
    fun eineVerdreheteUnterschriftWirdAbgelehnt() {
        val datei = signierteKarte("unterschrift")
        val bytes = datei.readBytes()
        bytes[40] = (bytes[40].toInt() xor 0x20).toByte()
        datei.writeBytes(bytes)
        val speicher = TrustStore(listOf(TrustedKey("entwicklung", oeffentlich)))
        assertFailsWith<Kartenfehler> { Kartenumschlag.oeffne(datei, speicher) }
    }

    /**
     * Der Grund, warum die Kennung im unterschriebenen Teil steht: Ohne sie
     * waere eine Unterschrift ueber ein Inhaltspaket auch fuer eine
     * Kartendatei derselben Groesse gueltig, und ein Paket liesse sich als
     * Karte unterschieben.
     */
    @Test
    fun einePaketUnterschriftDecktKeineKarte() {
        val blank = temp("kreuz-blank")
        blankeKarte(blank)
        val alsPaket = temp("kreuz-paket")
        PackWriter.write(blank.readBytes(), samen, alsPaket, PackFormat.MAGIC)
        val speicher = TrustStore(listOf(TrustedKey("entwicklung", oeffentlich)))
        val fehler = assertFailsWith<Kartenfehler> { Kartenumschlag.oeffne(alsPaket, speicher) }
        assertTrue(fehler.message!!.contains("weder eine Karte"))
    }

    @Test
    fun eineFremdeDateiWirdAbgelehnt() {
        val datei = temp("fremd-datei")
        datei.writeBytes(ByteArray(200) { 0x7A })
        assertFailsWith<Kartenfehler> { Kartenumschlag.oeffne(datei, TrustStore(emptyList())) }
    }

    @Test
    fun eineAbgeschnitteneSignierteKarteWirdAbgelehnt() {
        val datei = signierteKarte("kurz")
        val ganz = datei.readBytes()
        datei.writeBytes(ganz.copyOf(ganz.size - 5))
        val speicher = TrustStore(listOf(TrustedKey("entwicklung", oeffentlich)))
        assertFailsWith<Kartenfehler> { Kartenumschlag.oeffne(datei, speicher) }
    }

    /**
     * Ein Anhang hinter der signaturgedeckten Nutzlast darf nicht erreichbar
     * sein. Sonst liesse sich an eine gueltige Karte eine zweite anhaengen und
     * ueber einen praeparierten Verzeichniseintrag ansteuern.
     */
    @Test
    fun einAnhangHinterDerNutzlastMachtDieDateiUngueltig() {
        val datei = signierteKarte("anhang")
        val ganz = datei.readBytes()
        datei.writeBytes(ganz + ByteArray(64) { 0x11 })
        val speicher = TrustStore(listOf(TrustedKey("entwicklung", oeffentlich)))
        assertFailsWith<Kartenfehler> { Kartenumschlag.oeffne(datei, speicher) }
    }
}
