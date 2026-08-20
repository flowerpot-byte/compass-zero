package org.compasszero.security

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PackFormatTest {

    private val key = ByteArray(32) { it.toByte() }
    private val sig = ByteArray(64) { (it + 1).toByte() }

    private fun header(payloadSize: Long = 5L) = PackFormat.buildHeader(key, sig, payloadSize)

    @Test
    fun buildAndParseRoundtrip() {
        val bytes = header(payloadSize = 5L)
        assertEquals(PackFormat.HEADER_SIZE, bytes.size)
        val result = PackFormat.parseHeader(bytes, fileSize = PackFormat.HEADER_SIZE + 5L)
        val ok = assertIs<HeaderResult.Ok>(result)
        assertEquals(1, ok.header.version)
        assertContentEquals(key, ok.header.signerKey)
        assertContentEquals(sig, ok.header.signature)
        assertEquals(5L, ok.header.payloadSize)
    }

    @Test
    fun tooShortIsDamaged() {
        val result = PackFormat.parseHeader(ByteArray(10), fileSize = 10L)
        assertIs<HeaderResult.Damaged>(result)
    }

    @Test
    fun wrongMagicIsDamaged() {
        val bytes = header()
        bytes[0] = 0x58
        assertIs<HeaderResult.Damaged>(PackFormat.parseHeader(bytes, PackFormat.HEADER_SIZE + 5L))
    }

    @Test
    fun futureVersionIsUnsupported() {
        val bytes = header()
        bytes[5] = 2
        val result = PackFormat.parseHeader(bytes, PackFormat.HEADER_SIZE + 5L)
        val unsupported = assertIs<HeaderResult.Unsupported>(result)
        assertEquals(2, unsupported.version)
    }

    @Test
    fun sizeMismatchIsDamaged() {
        val result = PackFormat.parseHeader(header(payloadSize = 5L), fileSize = PackFormat.HEADER_SIZE + 6L)
        val damaged = assertIs<HeaderResult.Damaged>(result)
        assertEquals(DamageKind.SizeMismatch, damaged.kind)
    }

    @Test
    fun negativeOrHugePayloadIsDamaged() {
        assertIs<HeaderResult.Damaged>(
            PackFormat.parseHeader(header(payloadSize = -1L), fileSize = PackFormat.HEADER_SIZE - 1L)
        )
        val huge = PackFormat.MAX_PAYLOAD_BYTES + 1
        assertIs<HeaderResult.Damaged>(
            PackFormat.parseHeader(header(payloadSize = huge), fileSize = PackFormat.HEADER_SIZE + huge)
        )
    }

    @Test
    fun signedPortionCoversHeaderWithoutSignature() {
        val portion = PackFormat.signedPortion(PackFormat.VERSION, key, 5L)
        assertEquals(46, portion.size)
        val full = header(5L)
        assertContentEquals(full.copyOfRange(0, 38), portion.copyOfRange(0, 38))
        assertContentEquals(full.copyOfRange(102, 110), portion.copyOfRange(38, 46))
    }

    @Test
    fun signedPortionUsesTheGivenVersion() {
        assertEquals(2, PackFormat.signedPortion(2, key, 5L)[5].toInt())
    }

    @Test
    fun headerFieldsSitAtTheDocumentedOffsets() {
        val bytes = header(payloadSize = 0x0102030405060708L)
        assertContentEquals(byteArrayOf(0x43, 0x5A, 0x50, 0x31), bytes.copyOfRange(0, 4))
        assertContentEquals(byteArrayOf(0, 1), bytes.copyOfRange(4, 6))
        assertContentEquals(key, bytes.copyOfRange(6, 38))
        assertContentEquals(sig, bytes.copyOfRange(38, 102))
        assertContentEquals(
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08),
            bytes.copyOfRange(102, 110),
        )
    }
}
