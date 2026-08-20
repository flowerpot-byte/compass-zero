package org.compasszero.security

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PackEntriesTest {

    private val seed = Ed25519.generateSeed()
    private val trust = TrustStore(listOf(TrustedKey("t", Ed25519.publicKeyFromSeed(seed))))

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun open(payload: ByteArray, limits: PackLimits = PackLimits.DEFAULT): OpenedPack {
        val dir = File.createTempFile("czentries", null).let { it.delete(); it.mkdirs(); it }
        val payloadFile = File(dir, "payload.zip").apply { writeBytes(payload) }
        val pack = File(dir, "test.czp")
        PackWriter.write(payloadFile, seed, pack)
        return PackVerifier(trust).open(pack, limits)
    }

    private fun damageOf(opened: OpenedPack): DamageKind =
        assertIs<PackVerdict.Damaged>(opened.verdict).damage.kind

    @Test
    fun listsEntriesWithRealSizes() {
        val opened = open(zip(
            "manifest.json" to ByteArray(10),
            "content/tips.json" to ByteArray(70),
            "assets/bild.png" to ByteArray(5),
        ))
        assertIs<PackVerdict.Trusted>(opened.verdict)
        assertEquals(
            listOf("assets/bild.png" to 5L, "content/tips.json" to 70L, "manifest.json" to 10L),
            opened.entries.sortedBy { it.name }.map { it.name to it.size },
        )
    }

    @Test
    fun contentFilesCarryExactBytes() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val opened = open(zip("manifest.json" to bytes, "content/tips.json" to ByteArray(8) { 6 }))
        assertContentEquals(bytes, opened.contentFiles["manifest.json"])
        assertEquals(setOf("manifest.json", "content/tips.json"), opened.contentFiles.keys)
    }

    @Test
    fun forbiddenNamesAreDamaged() {
        val bad = listOf("../evil", "content/../x", "/absolut", "content\\win", "content//leer", "notes.txt", "CON:")
        for (name in bad) {
            val opened = open(zip("manifest.json" to ByteArray(1), name to ByteArray(1)))
            assertEquals(DamageKind.EntryNameForbidden, damageOf(opened), "erwartet Ablehnung fuer $name")
        }
    }

    @Test
    fun tooManyEntriesAreDamaged() {
        val entries = Array(4) { "content/f$it.json" to ByteArray(1) }
        val opened = open(zip("manifest.json" to ByteArray(1), *entries), PackLimits(maxEntries = 4))
        assertEquals(DamageKind.TooManyEntries, damageOf(opened))
    }

    @Test
    fun entryByteCapCountsUnpackedBytes() {
        val opened = open(zip("manifest.json" to ByteArray(11)), PackLimits(maxEntryBytes = 10))
        assertEquals(DamageKind.EntryTooLarge, damageOf(opened))
    }

    @Test
    fun totalByteCapIsEnforced() {
        val opened = open(
            zip("manifest.json" to ByteArray(6), "content/a.json" to ByteArray(6)),
            PackLimits(maxTotalBytes = 10),
        )
        assertEquals(DamageKind.PackTooLarge, damageOf(opened))
    }

    @Test
    fun contentByteCapIsEnforcedSeparately() {
        val opened = open(
            zip("manifest.json" to ByteArray(6), "content/a.json" to ByteArray(6)),
            PackLimits(maxContentBytes = 10),
        )
        assertEquals(DamageKind.PackTooLarge, damageOf(opened))
    }

    @Test
    fun nameLengthCapIsEnforced() {
        val long = "content/" + "a".repeat(300) + ".json"
        val opened = open(zip("manifest.json" to ByteArray(1), long to ByteArray(1)))
        assertEquals(DamageKind.EntryNameForbidden, damageOf(opened))
    }

    @Test
    fun versteckteDateienUndPunktnamenWerdenAbgelehnt() {
        // Ein Eintrag "content/.json" hat keinen Dateinamen, nur eine Endung.
        // Solche Namen entstehen nicht durch Zufall, sondern durch Basteln.
        assertTrue(PackNames.problem("content/.json") != null)
        assertTrue(PackNames.problem("content/.versteckt") != null)
        assertTrue(PackNames.problem("assets/.png") != null)
    }

    @Test
    fun packNamesRulesAreExplicit() {
        assertEquals(null, PackNames.problem("manifest.json"))
        assertEquals(null, PackNames.problem("content/tips.json"))
        assertEquals(null, PackNames.problem("assets/bild-1_klein.png"))
        for (bad in listOf("", "..", "../x", "/x", "content//x", "content/./x", "x.json", "assets/ä.png", "a".repeat(201))) {
            assertTrue(PackNames.problem(bad) != null, "muss abgelehnt werden: $bad")
        }
    }
}
