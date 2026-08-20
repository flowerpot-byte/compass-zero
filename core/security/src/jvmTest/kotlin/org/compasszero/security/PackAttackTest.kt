package org.compasszero.security

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Angriffe, die vor der Ueberarbeitung entweder abgestuerzt sind oder ungeprueften
// Inhalt ausgeliefert haben. Jeder Test haelt genau einen dieser Faelle fest.
class PackAttackTest {

    private val seed = Ed25519.generateSeed()
    private val trust = TrustStore(listOf(TrustedKey("Testschluessel", Ed25519.publicKeyFromSeed(seed))))

    private fun tempDir(): File = File.createTempFile("angriff", null).let { it.delete(); it.mkdirs(); it }

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

    // Unkomprimierter Eintrag: nur so lassen sich die Nutzdaten in der fertigen
    // Paketdatei gezielt manipulieren, wie es ein Angreifer taete.
    private fun storedZipOf(name: String, bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.setMethod(ZipOutputStream.STORED)
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
        return out.toByteArray()
    }

    private fun packOf(payload: ByteArray, name: String = "test.czp"): File {
        val dir = tempDir()
        val payloadFile = File(dir, "payload.bin").apply { writeBytes(payload) }
        val pack = File(dir, name)
        PackWriter.write(payloadFile, seed, pack)
        return pack
    }

    private fun replaceAll(bytes: ByteArray, from: ByteArray, to: ByteArray): ByteArray {
        require(from.size == to.size)
        val out = bytes.copyOf()
        outer@ for (i in 0..out.size - from.size) {
            for (j in from.indices) {
                if (out[i + j] != from[j]) continue@outer
            }
            to.copyInto(out, i)
        }
        return out
    }

    @Test
    fun brokenUtf8InEntryNameIsRejectedNotThrown() {
        val raw = zipOf("manifest.json" to byteArrayOf(1))
        val patched = replaceAll(raw, "manifest.jso".encodeToByteArray(), byteArrayOf(-1, -2, 0x6E, 0x69, 0x66, 0x65, 0x73, 0x74, 0x2E, 0x6A, 0x73, 0x6F))
        val opened = PackVerifier(trust).open(packOf(patched))
        val verdict = assertIs<PackVerdict.Damaged>(opened.verdict)
        assertEquals(DamageKind.EntryNameForbidden, verdict.damage.kind)
        assertTrue(opened.contentFiles.isEmpty())
    }

    @Test
    fun oversizeEntryIsRejectedBeforeBuffering() {
        // Ein Eintrag jenseits der Eintragsgrenze darf niemals im Speicher landen.
        val big = ByteArray(PackLimits.DEFAULT.maxEntryBytes.toInt() + 1)
        val opened = PackVerifier(trust).open(packOf(zipOf("content/gross.json" to big)))
        val verdict = assertIs<PackVerdict.Damaged>(opened.verdict)
        assertEquals(DamageKind.EntryTooLarge, verdict.damage.kind)
    }

    @Test
    fun appendedBytesDuringVerificationAreNotDelivered() {
        val payload = zipOf("manifest.json" to ByteArray(64) { 7 })
        val pack = packOf(payload)
        val signedSize = pack.length()
        RandomAccessFile(pack, "rw").use {
            it.seek(signedSize)
            it.write("UNGEPRUEFTE ZUSATZBYTES".encodeToByteArray())
        }
        val opened = PackVerifier(trust).open(pack)
        // Angehaengte Bytes veraendern die Dateigroesse, also passt der Header nicht mehr.
        val verdict = assertIs<PackVerdict.Damaged>(opened.verdict)
        assertEquals(DamageKind.SizeMismatch, verdict.damage.kind)
        assertTrue(opened.contentFiles.isEmpty())
    }

    @Test
    fun contentComesFromTheSignedPassNotFromLaterDiskReads() {
        val original = "TRINKWASSER IMMER 3 MINUTEN ABKOCHEN".encodeToByteArray()
        val pack = packOf(storedZipOf("manifest.json", original))
        val opened = PackVerifier(trust).open(pack)
        assertIs<PackVerdict.Trusted>(opened.verdict)
        val delivered = opened.contentFiles["manifest.json"]
        assertTrue(delivered != null && delivered.decodeToString() == original.decodeToString())

        // Nach dem Oeffnen die Datei in-place manipulieren: der bereits gelesene
        // Inhalt darf sich nicht mehr aendern, weil er aus dem signierten Durchlauf stammt.
        val tampered = "TRINKWASSER MUSS NICHT ABGEKOCHT"
            .padEnd(original.size, ' ')
            .encodeToByteArray()
        val bytes = pack.readBytes()
        val patched = replaceAll(bytes, original, tampered)
        pack.writeBytes(patched)
        assertEquals(original.decodeToString(), opened.contentFiles["manifest.json"]!!.decodeToString())

        // Und ein erneutes Oeffnen muss die Manipulation erkennen.
        val second = PackVerifier(trust).open(pack)
        assertIs<PackVerdict.BadSignature>(second.verdict)
        assertTrue(second.contentFiles.isEmpty())
    }

    @Test
    fun compressionBombeIsStoppedByTotalCap() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.setLevel(Deflater.BEST_COMPRESSION)
            zip.putNextEntry(ZipEntry("content/bombe.json"))
            val chunk = ByteArray(1 shl 20)
            repeat(64) { zip.write(chunk) }
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
        }
        val opened = PackVerifier(trust).open(packOf(out.toByteArray()), )
        val verdict = assertIs<PackVerdict.Damaged>(opened.verdict)
        assertTrue(verdict.damage.kind == DamageKind.EntryTooLarge || verdict.damage.kind == DamageKind.PackTooLarge)
        assertTrue(opened.contentFiles.isEmpty())
    }

    @Test
    fun forbiddenNamesNeverReachTheContentMap() {
        val raw = zipOf("manifest.json" to byteArrayOf(1), "content/harmlos.json" to "PWNED".encodeToByteArray())
        val patched = replaceAll(
            raw,
            "content/harmlos.json".encodeToByteArray(),
            "../../etc/passwd0000".encodeToByteArray(),
        )
        val opened = PackVerifier(trust).open(packOf(patched))
        val verdict = assertIs<PackVerdict.Damaged>(opened.verdict)
        assertEquals(DamageKind.EntryNameForbidden, verdict.damage.kind)
        assertNull(opened.contentFiles["../../etc/passwd0000"])
    }

    @Test
    fun duplicateEntryNamesAreRejectedInTheOnlyReadPath() {
        val raw = zipOf("manifest.json" to byteArrayOf(1), "manifest.jsoQ" to byteArrayOf(2))
        val patched = replaceAll(raw, "manifest.jsoQ".encodeToByteArray(), "manifest.json".encodeToByteArray())
        val opened = PackVerifier(trust).open(packOf(patched))
        val verdict = assertIs<PackVerdict.Damaged>(opened.verdict)
        assertEquals(DamageKind.DuplicateEntry, verdict.damage.kind)
    }

    @Test
    fun badSignatureOutranksContainerDamage() {
        // Kaputter Container UND kaputte Signatur: der Nutzer muss "manipuliert" erfahren,
        // nicht "beschaedigt" — sonst wirkt ein Angriff wie ein Uebertragungsfehler.
        val pack = packOf(ByteArray(400) { (it * 7).toByte() })
        val bytes = pack.readBytes()
        bytes[PackFormat.HEADER_SIZE + 5] = (bytes[PackFormat.HEADER_SIZE + 5].toInt() xor 0x55).toByte()
        pack.writeBytes(bytes)
        assertIs<PackVerdict.BadSignature>(PackVerifier(trust).open(pack).verdict)
    }

    @Test
    fun garbagePayloadWithValidSignatureHasNoUsableEntries() {
        val opened = PackVerifier(trust).open(packOf(ByteArray(400) { (it * 7).toByte() }))
        val verdict = assertIs<PackVerdict.Damaged>(opened.verdict)
        assertEquals(DamageKind.NoEntries, verdict.damage.kind)
        assertTrue(opened.contentFiles.isEmpty())
    }

    @Test
    fun onlyDirectoryEntriesMeansNoEntries() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("content/"))
            zip.closeEntry()
        }
        val opened = PackVerifier(trust).open(packOf(out.toByteArray()))
        val verdict = assertIs<PackVerdict.Damaged>(opened.verdict)
        assertEquals(DamageKind.NoEntries, verdict.damage.kind)
    }

    @Test
    fun foreignButValidPublicKeyInHeaderIsBadSignature() {
        // Nicht nur ein gekipptes Bit: ein echter, gueltiger Fremdschluessel im Header.
        val pack = packOf(zipOf("manifest.json" to byteArrayOf(1)))
        val foreign = Ed25519.publicKeyFromSeed(Ed25519.generateSeed())
        val bytes = pack.readBytes()
        foreign.copyInto(bytes, 6)
        pack.writeBytes(bytes)
        assertIs<PackVerdict.BadSignature>(PackVerifier(trust).open(pack).verdict)
    }

    @Test
    fun lyingDeclaredSizesAreIgnored() {
        // Deklarierte Groesse im lokalen Header auf 0xFFFFFFFF setzen, echte Bytes bleiben klein.
        val raw = zipOf("manifest.json" to ByteArray(40) { 3 })
        val bytes = raw.copyOf()
        // Lokaler Header: Signatur 0x04034b50, danach 18 Bytes bis zu comp/uncomp size.
        for (offset in intArrayOf(18, 22)) {
            for (i in 0 until 4) bytes[offset + i] = -1
        }
        val opened = PackVerifier(trust).open(packOf(bytes))
        // Entweder als kaputter Container erkannt oder mit echten Groessen gelesen —
        // auf keinen Fall darf die gelogene Groesse uebernommen werden.
        when (val verdict = opened.verdict) {
            is PackVerdict.Damaged -> assertEquals(DamageKind.ContainerBroken, verdict.damage.kind)
            else -> assertEquals(40L, opened.entries.single().size)
        }
    }

    @Test
    fun writerSelfVerifiesAndLeavesNoPartialFile() {
        val dir = tempDir()
        val payload = File(dir, "p.bin").apply { writeBytes(zipOf("manifest.json" to ByteArray(128) { 9 })) }
        val target = File(dir, "gut.czp")
        PackWriter.write(payload, seed, target)
        assertIs<PackVerdict.Trusted>(PackVerifier(trust).open(target).verdict)

        val missing = File(dir, "fehlt.bin")
        val broken = File(dir, "kaputt.czp")
        runCatching { PackWriter.write(missing, seed, broken) }
        assertTrue(!broken.exists(), "bei Fehlern darf kein halbes Paket liegen bleiben")
    }
}
