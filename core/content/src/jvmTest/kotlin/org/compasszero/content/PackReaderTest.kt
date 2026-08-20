package org.compasszero.content

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.compasszero.security.DamageKind
import org.compasszero.security.Ed25519
import org.compasszero.security.PackVerdict
import org.compasszero.security.PackWriter
import org.compasszero.security.TrustStore
import org.compasszero.security.TrustedKey

class PackReaderTest {

    private val seed = Ed25519.generateSeed()
    private val trust = TrustStore(listOf(TrustedKey("Testschluessel", Ed25519.publicKeyFromSeed(seed))))

    private val manifestJson =
        """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["tips"]}"""
    private val tipsJson =
        """{"schema":1,"tips":[{"id":"tipp-eins","title":"Titel Eins","category":"wasser","body":"Beispieltext.","sources":[{"name":"Quelle A","detail":"Abschnitt 1"}]}]}"""

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

    private fun packFile(payload: ByteArray, mutate: (ByteArray) -> ByteArray = { it }): File {
        val dir = File.createTempFile("czreader", null).let { it.delete(); it.mkdirs(); it }
        val payloadFile = File(dir, "payload.zip").apply { writeBytes(payload) }
        val pack = File(dir, "test.czp")
        PackWriter.write(payloadFile, seed, pack)
        pack.writeBytes(mutate(pack.readBytes()))
        return pack
    }

    private fun defaultPayload() = zipOf(
        "manifest.json" to manifestJson.encodeToByteArray(),
        "content/tips.json" to tipsJson.encodeToByteArray(),
    )

    @Test
    fun trustedPackLoadsEndToEnd() {
        val outcome = PackReader.read(packFile(defaultPayload()), trust)
        assertIs<PackVerdict.Trusted>(outcome.verdict)
        val result = assertNotNull(outcome.result)
        val pack = assertNotNull(result.pack)
        assertEquals(listOf("tipp-eins"), pack.tips.map { it.id })
        assertEquals(emptyList(), result.problems)
    }

    @Test
    fun unknownSignerStillParses() {
        val outcome = PackReader.read(packFile(defaultPayload()), TrustStore(emptyList()))
        assertIs<PackVerdict.UnknownSigner>(outcome.verdict)
        assertNotNull(assertNotNull(outcome.result).pack)
    }

    @Test
    fun tamperedPackYieldsNoContent() {
        val pack = packFile(defaultPayload()) { bytes ->
            bytes[bytes.size - 30] = (bytes[bytes.size - 30].toInt() xor 1).toByte()
            bytes
        }
        val outcome = PackReader.read(pack, trust)
        assertIs<PackVerdict.BadSignature>(outcome.verdict)
        assertNull(outcome.result)
    }

    @Test
    fun damagedContainerYieldsNoContent() {
        val outcome = PackReader.read(packFile(ByteArray(500) { (it * 3).toByte() }), trust)
        val verdict = assertIs<PackVerdict.Damaged>(outcome.verdict)
        assertEquals(DamageKind.NoEntries, verdict.damage.kind)
        assertNull(outcome.result)
    }

    @Test
    fun assetsArePassedAsNamesOnly() {
        val payload = zipOf(
            "manifest.json" to manifestJson.encodeToByteArray(),
            "content/tips.json" to tipsJson.encodeToByteArray(),
            "assets/verwaist.png" to ByteArray(64),
        )
        val outcome = PackReader.read(packFile(payload), trust)
        val result = assertNotNull(outcome.result)
        assertNotNull(result.pack)
        assertTrue(result.problems.any { it.code == "asset-unused" && it.where == "assets/verwaist.png" })
    }

    @Test
    fun contentProblemsSurviveTheBridge() {
        val badTips = """{"schema":1,"tips":[{"id":"x","title":"T","category":"c","body":"B","sources":[]}]}"""
        val payload = zipOf(
            "manifest.json" to manifestJson.encodeToByteArray(),
            "content/tips.json" to badTips.encodeToByteArray(),
        )
        val outcome = PackReader.read(packFile(payload), trust)
        val result = assertNotNull(outcome.result)
        assertNull(result.pack)
        assertTrue(result.problems.any { it.code == "sources-missing" })
    }
}
