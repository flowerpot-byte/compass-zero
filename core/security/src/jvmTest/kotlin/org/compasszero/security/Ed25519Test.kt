package org.compasszero.security

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Ed25519Test {

    private class Vector(val seed: String, val publicKey: String, val message: String, val signature: String)

    private val rfc8032 = listOf(
        Vector(
            seed = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60",
            publicKey = "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a",
            message = "",
            signature = "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
        ),
        Vector(
            seed = "4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb",
            publicKey = "3d4017c3e843895a92b70aa74d1b7ebc9c982ccf2ec4968cc0cd55f12af4660c",
            message = "72",
            signature = "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00",
        ),
        Vector(
            seed = "c5aa8df43f9f837bedb7442f31dcb7b166d38535076f094b85ce3a2e0b4458f7",
            publicKey = "fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025",
            message = "af82",
            signature = "6291d657deec24024827e69c3abe01a30ce548a284743a445e3680d7db5ac3ac18ff9b538d16f290ae67f760984dc6594a7c15e9716ed28dc027beceea1ec40a",
        ),
    )

    private fun stream(hex: String) = ByteArrayInputStream(Hex.decode(hex))

    @Test
    fun publicKeyDerivationMatchesRfc() {
        for (v in rfc8032) {
            assertContentEquals(Hex.decode(v.publicKey), Ed25519.publicKeyFromSeed(Hex.decode(v.seed)))
        }
    }

    @Test
    fun signatureMatchesRfc() {
        for (v in rfc8032) {
            val sig = Ed25519.sign(Hex.decode(v.seed), ByteArray(0), stream(v.message))
            assertEquals(v.signature, Hex.encode(sig))
        }
    }

    @Test
    fun verifyAcceptsRfcSignatures() {
        for (v in rfc8032) {
            assertTrue(
                Ed25519.verify(Hex.decode(v.publicKey), Hex.decode(v.signature), ByteArray(0), stream(v.message))
            )
        }
    }

    @Test
    fun prefixIsPartOfSignedMessage() {
        val v = rfc8032[1]
        // Gleiche Bytes, anders auf Prefix und Payload verteilt, muessen dieselbe Signatur ergeben.
        val viaPrefix = Ed25519.sign(Hex.decode(v.seed), Hex.decode(v.message), ByteArrayInputStream(ByteArray(0)))
        assertEquals(v.signature, Hex.encode(viaPrefix))
        assertFalse(
            Ed25519.verify(Hex.decode(v.publicKey), Hex.decode(v.signature), byteArrayOf(1), stream(v.message))
        )
    }

    @Test
    fun flippedBitFailsVerification() {
        val v = rfc8032[2]
        val sig = Hex.decode(v.signature)
        sig[0] = (sig[0].toInt() xor 1).toByte()
        assertFalse(Ed25519.verify(Hex.decode(v.publicKey), sig, ByteArray(0), stream(v.message)))
    }

    @Test
    fun malformedInputsReturnFalseInsteadOfThrowing() {
        val v = rfc8032[0]
        assertFalse(Ed25519.verify(ByteArray(31), Hex.decode(v.signature), ByteArray(0), stream(v.message)))
        assertFalse(Ed25519.verify(Hex.decode(v.publicKey), ByteArray(63), ByteArray(0), stream(v.message)))
        assertFalse(Ed25519.verify(ByteArray(32), Hex.decode(v.signature), ByteArray(0), stream(v.message)))
    }

    @Test
    fun generatedKeysRoundtrip() {
        val seed = Ed25519.generateSeed()
        assertEquals(32, seed.size)
        val publicKey = Ed25519.publicKeyFromSeed(seed)
        val sig = Ed25519.sign(seed, byteArrayOf(7), ByteArrayInputStream(byteArrayOf(8, 9)))
        assertTrue(Ed25519.verify(publicKey, sig, byteArrayOf(7), ByteArrayInputStream(byteArrayOf(8, 9))))
        assertFalse(Ed25519.verify(publicKey, sig, byteArrayOf(7), ByteArrayInputStream(byteArrayOf(8))))
    }
}
