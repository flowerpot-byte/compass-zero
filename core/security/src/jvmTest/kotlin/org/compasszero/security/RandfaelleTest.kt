package org.compasszero.security

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RandfaelleTest {

    private val seed = Ed25519.generateSeed()
    private val trust = TrustStore(listOf(TrustedKey("Testschluessel", Ed25519.publicKeyFromSeed(seed))))

    private fun tempDir(): File = File.createTempFile("rand", null).let { it.delete(); it.mkdirs(); it }

    private fun paket(dir: File, name: String = "test.czp", inhalt: ByteArray = ByteArray(64) { 3 }): File {
        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(inhalt)
            zip.closeEntry()
        }
        val payloadFile = File(dir, "$name.zip").apply { writeBytes(payload.toByteArray()) }
        val pack = File(dir, name)
        PackWriter.write(payloadFile, seed, pack)
        return pack
    }

    @Test
    fun abbruchWirdGemeldetStattWeiterzurechnen() {
        val pack = paket(tempDir())
        Thread.currentThread().interrupt()
        try {
            assertIs<PackVerdict.Aborted>(PackVerifier(trust).open(pack).verdict)
        } finally {
            // Zustand wieder aufraeumen, sonst faerbt er auf die naechsten Tests ab.
            Thread.interrupted()
        }
        assertIs<PackVerdict.Trusted>(PackVerifier(trust).open(pack).verdict)
    }

    @Test
    fun verzeichnisStattDateiIstUnlesbar() {
        val dir = tempDir()
        val urteil = PackVerifier(trust).open(dir).verdict
        assertEquals(DamageKind.Unreadable, assertIs<PackVerdict.Damaged>(urteil).damage.kind)
    }

    @Test
    fun leereDateiIstZuKurz() {
        val leer = File(tempDir(), "leer.czp").apply { writeBytes(ByteArray(0)) }
        val urteil = PackVerifier(trust).open(leer).verdict
        assertEquals(DamageKind.TooShort, assertIs<PackVerdict.Damaged>(urteil).damage.kind)
    }

    @Test
    fun dateiGenauKopfgrossOhneInhalt() {
        val dir = tempDir()
        val payloadFile = File(dir, "leer.bin").apply { writeBytes(ByteArray(0)) }
        val pack = File(dir, "nur-kopf.czp")
        PackWriter.write(payloadFile, seed, pack)
        assertEquals(PackFormat.HEADER_SIZE.toLong(), pack.length())
        assertEquals(DamageKind.NoEntries, assertIs<PackVerdict.Damaged>(PackVerifier(trust).open(pack).verdict).damage.kind)
    }

    @Test
    fun gleichesPaketAusMehrerenStraengenGleichzeitig() {
        // Die Oberflaeche kann dieselbe Datei aus mehreren Straengen oeffnen; das
        // darf weder haengen noch unterschiedliche Urteile ergeben.
        val pack = paket(tempDir())
        val pool = Executors.newFixedThreadPool(8)
        try {
            val aufgaben = (1..64).map {
                pool.submit<String> {
                    val opened = PackVerifier(trust).open(pack)
                    "${opened.verdict::class.simpleName}:${opened.contentFiles["manifest.json"]?.size}"
                }
            }
            val ergebnisse = aufgaben.map { it.get(60, TimeUnit.SECONDS) }.toSet()
            assertEquals(setOf("Trusted:64"), ergebnisse)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun schreibenUeberschreibtNurNachErfolg() {
        val dir = tempDir()
        val vorhandenes = paket(dir, "ziel.czp", ByteArray(64) { 1 })
        val alterInhalt = vorhandenes.readBytes()

        val fehlenderPayload = File(dir, "fehlt.zip")
        runCatching { PackWriter.write(fehlenderPayload, seed, vorhandenes) }
        assertTrue(vorhandenes.readBytes().contentEquals(alterInhalt), "vorhandenes Paket wurde beschaedigt")
        assertTrue(File(dir, "ziel.czp.unfertig").exists().not(), "Zwischendatei liegt noch herum")
    }

    @Test
    fun paketMitVielenKleinenEintraegenBleibtBeherrschbar() {
        val dir = tempDir()
        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
            repeat(5000) {
                zip.putNextEntry(ZipEntry("content/datei-$it.json"))
                zip.write(byteArrayOf(1, 2, 3))
                zip.closeEntry()
            }
        }
        val payloadFile = File(dir, "viele.zip").apply { writeBytes(payload.toByteArray()) }
        val pack = File(dir, "viele.czp")
        PackWriter.write(payloadFile, seed, pack)

        val opened = PackVerifier(trust).open(pack)
        assertIs<PackVerdict.Trusted>(opened.verdict)
        assertEquals(5001, opened.entries.size)
    }

    @Test
    fun zuVieleEintraegeWerdenAbgelehnt() {
        val dir = tempDir()
        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            repeat(12_000) {
                zip.putNextEntry(ZipEntry("content/datei-$it.json"))
                zip.write(byteArrayOf(1))
                zip.closeEntry()
            }
        }
        val payloadFile = File(dir, "zuviele.zip").apply { writeBytes(payload.toByteArray()) }
        val pack = File(dir, "zuviele.czp")
        PackWriter.write(payloadFile, seed, pack)

        val urteil = PackVerifier(trust).open(pack).verdict
        assertEquals(DamageKind.TooManyEntries, assertIs<PackVerdict.Damaged>(urteil).damage.kind)
    }
}
