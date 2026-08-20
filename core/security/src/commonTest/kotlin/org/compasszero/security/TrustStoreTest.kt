package org.compasszero.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TrustStoreTest {

    private val keyA = TrustedKey("Maintainer", ByteArray(32) { 1 })
    private val keyB = TrustedKey("Zweitschluessel", ByteArray(32) { 2 })

    @Test
    fun findsKeyByFullByteComparison() {
        val store = TrustStore(listOf(keyA, keyB))
        assertEquals("Zweitschluessel", store.find(ByteArray(32) { 2 })?.name)
    }

    @Test
    fun unknownKeyYieldsNull() {
        val store = TrustStore(listOf(keyA))
        assertNull(store.find(ByteArray(32) { 9 }))
    }

    @Test
    fun emptyStoreYieldsNull() {
        assertNull(TrustStore(emptyList()).find(ByteArray(32)))
    }

    @Test
    fun wrongSizeLookupYieldsNull() {
        val store = TrustStore(listOf(keyA))
        assertNull(store.find(ByteArray(31)))
    }

    @Test
    fun duplicateKeysRejected() {
        assertFailsWith<IllegalArgumentException> {
            TrustStore(listOf(keyA, TrustedKey("Kopie", ByteArray(32) { 1 })))
        }
    }

    @Test
    fun trustedKeyValidation() {
        assertFailsWith<IllegalArgumentException> { TrustedKey(" ", ByteArray(32)) }
        assertFailsWith<IllegalArgumentException> { TrustedKey("x", ByteArray(31)) }
    }

    @Test
    fun storedKeyIsDefensivelyCopied() {
        val raw = ByteArray(32) { 5 }
        val key = TrustedKey("k", raw)
        raw[0] = 99
        assertEquals(5, key.publicKey[0])
    }
}
