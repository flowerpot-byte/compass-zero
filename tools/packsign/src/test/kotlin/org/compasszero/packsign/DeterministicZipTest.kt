package org.compasszero.packsign

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeterministicZipTest {

    private fun tempDir(): File = File.createTempFile("dzip", null).let { it.delete(); it.mkdirs(); it }

    private fun fill(dir: File, order: List<String>) {
        for (name in order) {
            val file = File(dir, name)
            file.parentFile.mkdirs()
            file.writeBytes(name.encodeToByteArray())
        }
    }

    @Test
    fun sameContentYieldsIdenticalBytesRegardlessOfCreationOrder() {
        val a = tempDir().also { fill(it, listOf("manifest.json", "content/tips.json", "assets/bild.png")) }
        val b = tempDir().also { fill(it, listOf("assets/bild.png", "content/tips.json", "manifest.json")) }
        val zipA = File(a.parentFile, "a.zip")
        val zipB = File(b.parentFile, "b.zip")
        val namesA = DeterministicZip.write(a, zipA)
        val namesB = DeterministicZip.write(b, zipB)
        assertEquals(namesA, namesB)
        assertEquals(listOf("assets/bild.png", "content/tips.json", "manifest.json"), namesA)
        assertContentEquals(zipA.readBytes(), zipB.readBytes())
    }

    @Test
    fun repeatedRunsAreByteIdentical() {
        val dir = tempDir().also { fill(it, listOf("manifest.json")) }
        val first = File(dir.parentFile, "eins.zip")
        val second = File(dir.parentFile, "zwei.zip")
        DeterministicZip.write(dir, first)
        DeterministicZip.write(dir, second)
        assertContentEquals(first.readBytes(), second.readBytes())
    }

    @Test
    fun osJunkIsSkipped() {
        val dir = tempDir().also { fill(it, listOf("manifest.json", "Thumbs.db", "desktop.ini")) }
        val names = DeterministicZip.write(dir, File(dir.parentFile, "j.zip"))
        assertEquals(listOf("manifest.json"), names)
    }

    @Test
    fun forbiddenNamesAreRejected() {
        val dir = tempDir().also { fill(it, listOf("manifest.json", "notizen.txt")) }
        assertFailsWith<IllegalArgumentException> {
            DeterministicZip.write(dir, File(dir.parentFile, "n.zip"))
        }
    }

    @Test
    fun missingManifestIsRejected() {
        val dir = tempDir().also { fill(it, listOf("content/tips.json")) }
        assertFailsWith<IllegalArgumentException> {
            DeterministicZip.write(dir, File(dir.parentFile, "ohne.zip"))
        }
    }

    @Test
    fun emptyDirectoryIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            DeterministicZip.write(tempDir(), File(tempDir(), "leer.zip"))
        }
    }

    @Test
    fun timeZoneDoesNotChangeTheResult() {
        // Der feste Zeitstempel ist der ganze Grund fuer setTimeLocal. Waere er
        // zeitzonenabhaengig, koennte niemand ein Paket anderswo nachbauen.
        val dir = tempDir().also { fill(it, listOf("manifest.json", "content/tips.json")) }
        val original = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Pacific/Kiritimati"))
            val a = File(dir.parentFile, "kiritimati.zip").also { DeterministicZip.write(dir, it) }
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Pacific/Midway"))
            val b = File(dir.parentFile, "midway.zip").also { DeterministicZip.write(dir, it) }
            assertContentEquals(a.readBytes(), b.readBytes())
        } finally {
            java.util.TimeZone.setDefault(original)
        }
    }

    @Test
    fun symbolicLinksAreNotFollowed() {
        val dir = tempDir().also { fill(it, listOf("manifest.json")) }
        val outside = tempDir().also { fill(it, listOf("geheim.json")) }
        val link = File(dir, "content").toPath()
        try {
            java.nio.file.Files.createSymbolicLink(link, outside.toPath())
        } catch (_: Exception) {
            return // ohne Rechte fuer Verknuepfungen ist hier nichts zu pruefen
        }
        val names = DeterministicZip.write(dir, File(dir.parentFile, "link.zip"))
        assertEquals(listOf("manifest.json"), names)
    }

    @Test
    fun nonAsciiFileNamesAreRejected() {
        val dir = tempDir().also { fill(it, listOf("manifest.json")) }
        File(dir, "content").mkdirs()
        File(dir, "content/übung.json").writeBytes(byteArrayOf(1))
        assertFailsWith<IllegalArgumentException> {
            DeterministicZip.write(dir, File(dir.parentFile, "umlaut.zip"))
        }
    }
}
