package org.compasszero.security

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PackVerifierTest {

    private val seed = Ed25519.generateSeed()
    private val publicKey = Ed25519.publicKeyFromSeed(seed)
    private val signer = TrustedKey("Testschluessel", publicKey)
    private val manifestBytes = ByteArray(300) { (it % 251).toByte() }

    private fun tempDir(): File = File.createTempFile("czpack", null).let {
        it.delete(); it.mkdirs(); it
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
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

    private fun writePack(
        dir: File = tempDir(),
        payload: ByteArray = zipOf("manifest.json" to manifestBytes),
        mutate: (ByteArray) -> ByteArray = { it },
    ): File {
        val payloadFile = File(dir, "payload.bin").apply { writeBytes(payload) }
        val pack = File(dir, "test.czp")
        PackWriter.write(payloadFile, seed, pack)
        pack.writeBytes(mutate(pack.readBytes()))
        return pack
    }

    private fun verifier(vararg keys: TrustedKey) = PackVerifier(TrustStore(keys.toList()))

    @Test
    fun trustedRoundtrip() {
        val opened = verifier(signer).open(writePack())
        val verdict = assertIs<PackVerdict.Trusted>(opened.verdict)
        assertEquals("Testschluessel", verdict.signer.name)
        assertContentEquals(manifestBytes, opened.contentFiles["manifest.json"])
        assertEquals(listOf("manifest.json"), opened.entries.map { it.name })
        assertEquals(300L, opened.entries.single().size)
    }

    @Test
    fun unknownSignerStillDeliversContent() {
        val opened = verifier().open(writePack())
        val verdict = assertIs<PackVerdict.UnknownSigner>(opened.verdict)
        assertEquals(Digests.fingerprint(publicKey), verdict.fingerprint)
        assertContentEquals(manifestBytes, opened.contentFiles["manifest.json"])
    }

    @Test
    fun tamperedPayloadByteIsBadSignature() {
        val opened = verifier(signer).open(writePack(mutate = { bytes ->
            bytes[bytes.size - 40] = (bytes[bytes.size - 40].toInt() xor 1).toByte()
            bytes
        }))
        assertIs<PackVerdict.BadSignature>(opened.verdict)
        assertTrue(opened.contentFiles.isEmpty())
        assertTrue(opened.entries.isEmpty())
    }

    @Test
    fun tamperedSignerKeyIsBadSignature() {
        val opened = verifier(signer).open(writePack(mutate = { bytes ->
            bytes[6] = (bytes[6].toInt() xor 1).toByte()
            bytes
        }))
        assertIs<PackVerdict.BadSignature>(opened.verdict)
    }

    @Test
    fun appendedBytesAreDamaged() {
        val opened = verifier(signer).open(writePack(mutate = { it + 1 }))
        assertEquals(DamageKind.SizeMismatch, assertIs<PackVerdict.Damaged>(opened.verdict).damage.kind)
    }

    @Test
    fun truncatedFileIsDamaged() {
        val opened = verifier(signer).open(writePack(mutate = { it.copyOfRange(0, 50) }))
        assertEquals(DamageKind.TooShort, assertIs<PackVerdict.Damaged>(opened.verdict).damage.kind)
    }

    @Test
    fun wrongMagicIsDamaged() {
        val opened = verifier(signer).open(writePack(mutate = { bytes ->
            bytes[0] = 0x58
            bytes
        }))
        assertEquals(DamageKind.MagicMismatch, assertIs<PackVerdict.Damaged>(opened.verdict).damage.kind)
    }

    @Test
    fun futureVersionIsUnsupported() {
        val opened = verifier(signer).open(writePack(mutate = { bytes ->
            bytes[5] = 9
            bytes
        }))
        assertEquals(9, assertIs<PackVerdict.Unsupported>(opened.verdict).version)
    }

    @Test
    fun missingFileIsDamaged() {
        val opened = verifier(signer).open(File(tempDir(), "fehlt.czp"))
        assertEquals(DamageKind.Unreadable, assertIs<PackVerdict.Damaged>(opened.verdict).damage.kind)
    }

    @Test
    fun emptyPayloadHasNoEntries() {
        val dir = tempDir()
        val payload = File(dir, "leer.bin").apply { writeBytes(ByteArray(0)) }
        val pack = File(dir, "leer.czp")
        PackWriter.write(payload, seed, pack)
        val opened = verifier(signer).open(pack)
        assertEquals(DamageKind.NoEntries, assertIs<PackVerdict.Damaged>(opened.verdict).damage.kind)
    }

    @Test
    fun openReleasesFileHandle() {
        val pack = writePack()
        verifier(signer).open(pack)
        assertTrue(pack.delete(), "Datei muss nach open() wieder loeschbar sein")
    }

    @Test
    fun assetsAreListedWithHashesButNotBuffered() {
        val bild = ByteArray(1024) { 4 }
        val opened = verifier(signer).open(
            writePack(payload = zipOf("manifest.json" to manifestBytes, "assets/bild.png" to bild))
        )
        assertIs<PackVerdict.Trusted>(opened.verdict)
        assertEquals(listOf("assets/bild.png"), opened.assets.map { it.name })
        assertEquals(Hex.encode(Digests.sha256(bild)), opened.assets.single().sha256)
        assertTrue("assets/bild.png" !in opened.contentFiles)
    }

    @Test
    fun writerRejectsBadSeed() {
        val dir = tempDir()
        val payload = File(dir, "p.bin").apply { writeBytes(byteArrayOf(1)) }
        assertFailsWith<IllegalArgumentException> {
            PackWriter.write(payload, ByteArray(31), File(dir, "out.czp"))
        }
    }
}
