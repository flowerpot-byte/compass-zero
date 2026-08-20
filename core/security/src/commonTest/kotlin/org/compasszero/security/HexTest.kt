package org.compasszero.security

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HexTest {

    @Test
    fun roundtrip() {
        val bytes = byteArrayOf(0x00, 0x7F, -0x80, -0x01, 0x0A)
        assertEquals("007f80ff0a", Hex.encode(bytes))
        assertContentEquals(bytes, Hex.decode("007f80ff0a"))
    }

    @Test
    fun decodeAcceptsUpperCase() {
        assertContentEquals(byteArrayOf(-0x01, 0x00), Hex.decode("FF00"))
    }

    @Test
    fun decodeRejectsOddLength() {
        assertFailsWith<IllegalArgumentException> { Hex.decode("abc") }
    }

    @Test
    fun decodeRejectsNonHex() {
        assertFailsWith<IllegalArgumentException> { Hex.decode("zz") }
    }
}
