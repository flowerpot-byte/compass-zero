package org.compasszero.karte

import java.io.File
import java.util.zip.Deflater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Prueft die Kartendatei -- Kopf, Verzeichnis, Suche und Entpacken.
 *
 * Eine Kartendatei kommt von aussen ins Geraet: ueber eine Speicherkarte, ueber
 * Bluetooth, von einem fremden Rechner. Sie ist damit die groesste Angriffs-
 * flaeche, die die App hat, und zugleich die einzige, die im Ernstfall gebraucht
 * wird. Deshalb prueft dieser Test nicht nur, ob eine richtige Datei gelesen
 * wird, sondern vor allem, ob eine falsche zuverlaessig abgewiesen wird, statt
 * die App mitzureissen.
 */
class KartendateiTest {

    private fun packe(roh: ByteArray): ByteArray {
        val packer = Deflater(9)
        packer.setInput(roh)
        packer.finish()
        val aus = ByteArray(roh.size * 2 + 64)
        val n = packer.deflate(aus)
        packer.end()
        return aus.copyOf(n)
    }

    /** Eine Kachel mit einer einzigen Linie. */
    private fun kachelbytes(punkte: Int = 2): ByteArray {
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
        roh(Kartenformat.FLUSS)
        varint(1)
        roh(Kartenformat.Art.LINIE)
        varint(0)
        varint(0)
        varint(punkte)
        repeat(punkte) { zigzag(100); zigzag(50) }
        return b.toByteArray()
    }

    private fun schreibeKarte(
        ziel: File,
        kacheln: List<Triple<Int, Int, Int>>,
        kennung: ByteArray = Kartenformat.KENNUNG,
        verdreheReihenfolge: Boolean = false,
        versatzVerbiegen: Long = 0L,
        laengeUeberschreiben: Int? = null,
    ) {
        val inhalte = kacheln.map { packe(kachelbytes()) }
        val anfang = Kartenformat.KOPF_BYTES + Kartenformat.EINTRAG_BYTES * kacheln.size
        val kopf = ByteArray(Kartenformat.KOPF_BYTES)
        kennung.copyInto(kopf, 0, 0, minOf(8, kennung.size))
        kopf[8] = Kartenformat.FASSUNG.toByte()
        kopf[9] = 12
        kopf[10] = kacheln.minOf { it.first }.toByte()
        kopf[11] = kacheln.maxOf { it.first }.toByte()
        schreibeInt(kopf, 12, 96_000_000)
        schreibeInt(kopf, 16, 464_000_000)
        schreibeInt(kopf, 20, 172_000_000)
        schreibeInt(kopf, 24, 490_000_000)
        schreibeInt(kopf, 28, kacheln.size)
        schreibeLong(kopf, 32, anfang.toLong())

        val reihenfolge = if (verdreheReihenfolge) kacheln.indices.reversed().toList()
        else kacheln.indices.toList()

        ziel.outputStream().use { aus ->
            aus.write(kopf)
            var versatz = anfang.toLong()
            val versaetze = IntArray(kacheln.size)
            for (i in kacheln.indices) versaetze[i] = 0
            val stellen = HashMap<Int, Long>()
            var laufend = anfang.toLong()
            for (i in kacheln.indices) {
                stellen[i] = laufend
                laufend += inhalte[i].size
            }
            for (i in reihenfolge) {
                val (z, x, y) = kacheln[i]
                aus.write(z)
                schreibeIntStrom(aus, x)
                schreibeIntStrom(aus, y)
                schreibeLongStrom(aus, stellen[i]!! + versatzVerbiegen)
                schreibeIntStrom(aus, laengeUeberschreiben ?: inhalte[i].size)
            }
            versatz = anfang.toLong()
            for (i in kacheln.indices) {
                aus.write(inhalte[i])
                versatz += inhalte[i].size
            }
        }
    }

    private fun schreibeInt(roh: ByteArray, p: Int, w: Int) {
        roh[p] = w.toByte()
        roh[p + 1] = (w shr 8).toByte()
        roh[p + 2] = (w shr 16).toByte()
        roh[p + 3] = (w shr 24).toByte()
    }

    private fun schreibeLong(roh: ByteArray, p: Int, w: Long) {
        for (i in 0 until 8) roh[p + i] = (w shr (8 * i)).toByte()
    }

    private fun schreibeIntStrom(aus: java.io.OutputStream, w: Int) {
        for (i in 0 until 4) aus.write((w shr (8 * i)) and 0xFF)
    }

    private fun schreibeLongStrom(aus: java.io.OutputStream, w: Long) {
        for (i in 0 until 8) aus.write(((w shr (8 * i)) and 0xFF).toInt())
    }

    private fun temp(name: String): File =
        File.createTempFile("karte-$name", ".czk").also { it.deleteOnExit() }

    @Test
    fun einfacheKarteWirdGelesen() {
        val datei = temp("gut")
        schreibeKarte(datei, listOf(Triple(5, 16, 11), Triple(5, 17, 11), Triple(6, 33, 22)))
        Kartendatei.oeffne(datei).use { karte ->
            assertEquals(3, karte.kachelzahl)
            assertEquals(5, karte.zoomKleinste)
            assertEquals(6, karte.zoomGroesste)
            assertEquals(96_000_000, karte.westen)
            assertEquals(490_000_000, karte.norden)

            val kachel = karte.kachel(5, 17, 11)
            assertTrue(kachel != null)
            assertEquals(1, kachel!!.objekte)
            assertEquals(Kartenformat.FLUSS, kachel.sorte[0].toInt())
            assertEquals(5, kachel.zoom)
            assertEquals(17, kachel.kachelX)
            assertEquals(11, kachel.kachelY)

            assertEquals(2, karte.kachelliste(5).size)
            assertEquals(1, karte.kachelliste(6).size)
        }
    }

    @Test
    fun eineFehlendeKachelGibtNullUndKeinenFehler() {
        val datei = temp("luecke")
        schreibeKarte(datei, listOf(Triple(5, 16, 11)))
        Kartendatei.oeffne(datei).use { karte ->
            assertNull(karte.kachel(5, 99, 99))
            assertNull(karte.kachel(7, 16, 11))
        }
    }

    @Test
    fun eineFremdeDateiWirdAbgelehnt() {
        val datei = temp("fremd")
        schreibeKarte(datei, listOf(Triple(5, 16, 11)), kennung = "NICHTCZK".encodeToByteArray())
        val fehler = assertFailsWith<Kartenfehler> { Kartendatei.oeffne(datei) }
        assertTrue(fehler.message!!.contains("keine Kartendatei"))
    }

    @Test
    fun einUnsortiertesVerzeichnisWirdAbgelehnt() {
        val datei = temp("unsortiert")
        schreibeKarte(
            datei,
            listOf(Triple(5, 16, 11), Triple(5, 17, 11), Triple(5, 18, 11)),
            verdreheReihenfolge = true,
        )
        val fehler = assertFailsWith<Kartenfehler> { Kartendatei.oeffne(datei) }
        assertTrue(fehler.message!!.contains("sortiert"))
    }

    @Test
    fun einVersatzHinterDasDateiendeWirdAbgelehnt() {
        val datei = temp("versatz")
        schreibeKarte(datei, listOf(Triple(5, 16, 11)), versatzVerbiegen = 1_000_000L)
        val fehler = assertFailsWith<Kartenfehler> { Kartendatei.oeffne(datei) }
        assertTrue(fehler.message!!.contains("unbrauchbar"))
    }

    @Test
    fun eineErfundeneKachellaengeWirdAbgelehnt() {
        val datei = temp("laenge")
        schreibeKarte(datei, listOf(Triple(5, 16, 11)), laengeUeberschreiben = 900_000_000)
        val fehler = assertFailsWith<Kartenfehler> { Kartendatei.oeffne(datei) }
        assertTrue(fehler.message!!.contains("unbrauchbar"))
    }

    @Test
    fun eineLeereKachellaengeWirdAbgelehnt() {
        val datei = temp("leer")
        schreibeKarte(datei, listOf(Triple(5, 16, 11)), laengeUeberschreiben = 0)
        assertFailsWith<Kartenfehler> { Kartendatei.oeffne(datei) }
    }

    @Test
    fun eineAbgeschnitteneDateiWirdAbgelehnt() {
        val datei = temp("kurz")
        schreibeKarte(datei, listOf(Triple(5, 16, 11), Triple(5, 17, 11)))
        val ganz = datei.readBytes()
        datei.writeBytes(ganz.copyOf(ganz.size - 10))
        assertFailsWith<Kartenfehler> { Kartendatei.oeffne(datei) }
    }

    @Test
    fun beschaedigteKachelbytesWerdenGemeldet() {
        val fehler = assertFailsWith<Kartenfehler> {
            Kartendatei.entpacke(ByteArray(40) { 0x5A }, "5/16/11")
        }
        assertTrue(fehler.message!!.contains("5/16/11"))
    }

    /**
     * Eine kleine Datei, die sich beim Entpacken ins Riesige aufblaeht, ist der
     * billigste Weg, ein Telefon zum Absturz zu bringen: ein paar hundert Bytes
     * Aufwand fuer den Angreifer, ein voller Arbeitsspeicher fuer das Opfer.
     */
    @Test
    fun eineAufblaehendeKachelWirdAbgebrochen() {
        val riesig = ByteArray(Kartenformat.KACHEL_MAX_BYTES * 2)
        val gepackt = packe(riesig)
        assertTrue(gepackt.size < 200_000, "Testvoraussetzung: die Bombe muss klein sein")
        val fehler = assertFailsWith<Kartenfehler> { Kartendatei.entpacke(gepackt, "bombe") }
        assertTrue(fehler.message!!.contains("zu gross"))
    }

    @Test
    fun eineFehlendeDateiWirdGemeldet() {
        val fehler = assertFailsWith<Kartenfehler> {
            Kartendatei.oeffne(File("gibtesnicht-4711.czk"))
        }
        assertTrue(fehler.message!!.contains("fehlt"))
    }
}
