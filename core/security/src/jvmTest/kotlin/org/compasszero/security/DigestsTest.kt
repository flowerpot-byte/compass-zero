package org.compasszero.security

import kotlin.test.Test
import kotlin.test.assertEquals

class DigestsTest {

    @Test
    fun sha256KnownVector() {
        val hash = Digests.sha256("abc".encodeToByteArray())
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Hex.encode(hash),
        )
    }

    @Test
    fun fingerprintIsLowercaseHexOfSha256() {
        val key = ByteArray(32) { it.toByte() }
        assertEquals(Hex.encode(Digests.sha256(key)), Digests.fingerprint(key))
        assertEquals(64, Digests.fingerprint(key).length)
    }
}
