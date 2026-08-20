package org.compasszero.security

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun ByteArray.letzteKopfstelleVor(stelle: Int): Int {
    val signatur = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    for (i in stelle - 4 downTo 0) {
        if (this[i] == signatur[0] && this[i + 1] == signatur[1] &&
            this[i + 2] == signatur[2] && this[i + 3] == signatur[3]
        ) {
            return i
        }
    }
    return -1
}

private fun ByteArray.indexOfSubarray(muster: ByteArray): Int {
    outer@ for (i in 0..size - muster.size) {
        for (j in muster.indices) {
            if (this[i + j] != muster[j]) continue@outer
        }
        return i
    }
    return -1
}

class BildLadenTest {

    private val seed = Ed25519.generateSeed()
    private val trust = TrustStore(listOf(TrustedKey("Testschluessel", Ed25519.publicKeyFromSeed(seed))))
    private val bild = ByteArray(2048) { (it % 251).toByte() }

    private fun tempDir(): File = File.createTempFile("bild", null).let { it.delete(); it.mkdirs(); it }

    private fun paket(dir: File, dateiname: String = "test.czp"): File {
        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.setMethod(ZipOutputStream.STORED)
            for ((name, bytes) in listOf("manifest.json" to ByteArray(20) { 1 }, "assets/bild.png" to bild)) {
                val entry = ZipEntry(name)
                entry.size = bytes.size.toLong()
                entry.compressedSize = bytes.size.toLong()
                val crc = java.util.zip.CRC32()
                crc.update(bytes)
                entry.crc = crc.value
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        val payloadFile = File(dir, "payload.zip").apply { writeBytes(payload.toByteArray()) }
        val pack = File(dir, dateiname)
        PackWriter.write(payloadFile, seed, pack)
        return pack
    }

    @Test
    fun bildKommtNurMitPassenderPruefsumme() {
        val pack = paket(tempDir())
        val opened = PackVerifier(trust).open(pack)
        assertIs<PackVerdict.Trusted>(opened.verdict)
        val eintrag = opened.assets.single()

        val gelesen = assertIs<AssetRead.Ok>(opened.readAsset(eintrag))
        assertContentEquals(bild, gelesen.bytes)
    }

    @Test
    fun nachtraeglichVerandertesBildWirdAbgelehnt() {
        // Der eigentliche Zweck: Bilder bleiben im Paket und werden erst spaeter
        // gelesen. Zwischen Pruefung und Anzeige darf sie niemand austauschen.
        // Ein Angreifer wuerde die ZIP-eigene Pruefzahl mitkorrigieren — die ist
        // keine Sicherheitspruefung. Genau so wird hier manipuliert.
        val pack = paket(tempDir())
        val opened = PackVerifier(trust).open(pack)
        val eintrag = opened.assets.single()

        val bytes = pack.readBytes()
        val start = bytes.indexOfSubarray(bild)
        assertTrue(start > 0, "Bilddaten im Paket nicht gefunden")

        val veraendert = bild.copyOf()
        veraendert[500] = 0x42
        veraendert[501] = 0x42
        veraendert[502] = 0x42
        veraendert.copyInto(bytes, start)

        val kopfStart = bytes.letzteKopfstelleVor(start)
        assertTrue(kopfStart >= 0, "lokalen ZIP-Kopf nicht gefunden")
        val crc = java.util.zip.CRC32().apply { update(veraendert) }.value
        for (i in 0 until 4) {
            bytes[kopfStart + 14 + i] = ((crc shr (8 * i)) and 0xFF).toByte()
        }
        pack.writeBytes(bytes)

        val urteil = assertIs<AssetRead.Damaged>(opened.readAsset(eintrag))
        assertEquals(DamageKind.HashMismatch, urteil.damage.kind)
    }

    @Test
    fun fehlendesBildWirdGemeldet() {
        val dir = tempDir()
        val pack = paket(dir)
        val opened = PackVerifier(trust).open(pack)
        // Ein Eintrag aus einem anderen Paket gehoert nicht hierher.
        val fremd = PackVerifier(trust).open(paket(tempDir(), "anderes.czp")).assets.single()
        assertIs<AssetRead.Ok>(opened.readAsset(fremd)) // gleicher Inhalt, gleicher Hash

        val bytes = pack.readBytes()
        val start = bytes.indexOfSubarray(bild)
        bytes[start] = (bytes[start].toInt() xor 0xFF).toByte()
        pack.writeBytes(bytes)
        assertIs<AssetRead.Damaged>(opened.readAsset(fremd))
    }

    @Test
    fun eintragAusFremdemPaketWirdAbgelehnt() {
        // Ein Eintrag mit anderem Hash gehoert nicht zu diesem Paket und darf
        // nicht als Anker durchgehen.
        val opened = PackVerifier(trust).open(paket(tempDir()))
        val anderesBild = ByteArray(64) { 9 }
        val anderesDir = tempDir()
        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(ByteArray(20) { 1 })
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("assets/bild.png"))
            zip.write(anderesBild)
            zip.closeEntry()
        }
        val payloadFile = File(anderesDir, "p.zip").apply { writeBytes(payload.toByteArray()) }
        val anderesPaket = File(anderesDir, "anderes.czp")
        PackWriter.write(payloadFile, seed, anderesPaket)
        val fremderEintrag = PackVerifier(trust).open(anderesPaket).assets.single()

        val urteil = assertIs<AssetRead.Damaged>(opened.readAsset(fremderEintrag))
        assertEquals(DamageKind.HashMismatch, urteil.damage.kind)
    }

    @Test
    fun ohneGueltigeSignaturGibtEsKeinenZugang() {
        // Frueher liess sich ein Bild an der Signaturpruefung vorbei lesen, indem
        // man Datei und Eintrag selbst zusammensetzte. Der Weg existiert nicht mehr:
        // Bilder kommen nur ueber ein geoeffnetes, geprueftes Paket.
        val pack = paket(tempDir())
        val gueltig = PackVerifier(trust).open(pack)
        val eintrag = gueltig.assets.single()

        val bytes = pack.readBytes()
        bytes[6] = (bytes[6].toInt() xor 1).toByte()
        pack.writeBytes(bytes)

        val kaputt = PackVerifier(trust).open(pack)
        assertIs<PackVerdict.BadSignature>(kaputt.verdict)
        assertTrue(kaputt.assets.isEmpty())
        // Ohne haltende Signatur gibt es nicht einmal einen Dateibezug, ueber den
        // sich etwas nachladen liesse.
        val urteil = assertIs<AssetRead.Damaged>(kaputt.readAsset(eintrag))
        assertEquals(DamageKind.Unreadable, urteil.damage.kind)
    }

    @Test
    fun nurBilderDuerfenSoGelesenWerden() {
        val opened = PackVerifier(trust).open(paket(tempDir()))
        val manifest = opened.entries.single { it.name == "manifest.json" }
        val urteil = assertIs<AssetRead.Damaged>(opened.readAsset(manifest))
        assertEquals(DamageKind.EntryNameForbidden, urteil.damage.kind)
    }

    @Test
    fun zuGrossesBildWirdAbgelehnt() {
        val opened = PackVerifier(trust).open(paket(tempDir()))
        val eintrag = opened.assets.single()
        val urteil = assertIs<AssetRead.Damaged>(
            opened.readAsset(eintrag, PackLimits(maxEntryBytes = 100))
        )
        assertEquals(DamageKind.EntryTooLarge, urteil.damage.kind)
    }

    @Test
    fun kaputtePaketdateiWirdGemeldet() {
        val pack = paket(tempDir())
        val opened = PackVerifier(trust).open(pack)
        val eintrag = opened.assets.single()
        pack.writeBytes(ByteArray(50))
        assertIs<AssetRead.Damaged>(opened.readAsset(eintrag))
    }

    @Test
    fun abbruchBeimNachladenWirdGemeldet() {
        val opened = PackVerifier(trust).open(paket(tempDir()))
        val eintrag = opened.assets.single()
        Thread.currentThread().interrupt()
        try {
            assertIs<AssetRead.Damaged>(opened.readAsset(eintrag))
        } finally {
            Thread.interrupted()
        }
        assertIs<AssetRead.Ok>(opened.readAsset(eintrag))
    }

    @Test
    fun ausgetauschteDateiMitRiesigemVorlaufWirdGestoppt() {
        // Wer die Paketdatei auf der Platte ersetzt, darf die App nicht minutenlang
        // beschaeftigen koennen.
        val dir = tempDir()
        val pack = paket(dir)
        val opened = PackVerifier(trust).open(pack)
        val eintrag = opened.assets.single()

        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("content/vorlauf.json"))
            val block = ByteArray(1 shl 20)
            repeat(40) { zip.write(block) }
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("assets/bild.png"))
            zip.write(bild)
            zip.closeEntry()
        }
        val payloadFile = File(dir, "gross.zip").apply { writeBytes(payload.toByteArray()) }
        PackWriter.write(payloadFile, seed, pack)

        val urteil = assertIs<AssetRead.Damaged>(
            opened.readAsset(eintrag, PackLimits(maxTotalBytes = 4L * 1024 * 1024))
        )
        assertEquals(DamageKind.PackTooLarge, urteil.damage.kind)
    }
}
