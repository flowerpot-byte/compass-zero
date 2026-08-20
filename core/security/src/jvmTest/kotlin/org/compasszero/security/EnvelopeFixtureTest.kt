package org.compasszero.security

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Nagelt das Format auf feste Bytes fest. Aendert ein spaeterer Umbau Schreiber und
// Leser gleichzeitig, bleiben alle Roundtrip-Tests gruen — dieser hier nicht. Genau
// das ist der Punkt: bereits verteilte Pakete auf SD-Karten muessen lesbar bleiben.
class EnvelopeFixtureTest {

    private val seed = Hex.decode("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
    private val payload = "compass-zero-testvektor".encodeToByteArray()

    private val expectedPublicKey = "03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8"
    private val expectedSignature =
        "a13810ba55170937400af057a4a369cfff09358378bb018e62fd89c1dd910037" +
            "c5de5f01e793ef902d4ed14723616c697c3bbc735bc4bd375913c2cc38872e09"

    @Test
    fun signerKeyMatchesTheFixedSeed() {
        assertEquals(expectedPublicKey, Hex.encode(Ed25519.publicKeyFromSeed(seed)))
    }

    @Test
    fun signatureOverTheFixedPayloadIsStable() {
        val publicKey = Ed25519.publicKeyFromSeed(seed)
        val prefix = PackFormat.signedPortion(PackFormat.VERSION, publicKey, payload.size.toLong())
        val signature = Ed25519.sign(seed, prefix, ByteArrayInputStream(payload))
        assertEquals(expectedSignature, Hex.encode(signature))
    }

    @Test
    fun headerLayoutIsByteExact() {
        val publicKey = Ed25519.publicKeyFromSeed(seed)
        val signature = Hex.decode(expectedSignature)
        val header = PackFormat.buildHeader(publicKey, signature, payload.size.toLong())

        assertEquals(110, header.size)
        assertEquals("435a5031", Hex.encode(header.copyOfRange(0, 4)))
        assertEquals("0001", Hex.encode(header.copyOfRange(4, 6)))
        assertContentEquals(publicKey, header.copyOfRange(6, 38))
        assertContentEquals(signature, header.copyOfRange(38, 102))
        assertEquals("0000000000000017", Hex.encode(header.copyOfRange(102, 110)))
    }

    @Test
    fun signedMessageIsHeaderWithoutSignaturePlusPayload() {
        val publicKey = Ed25519.publicKeyFromSeed(seed)
        val prefix = PackFormat.signedPortion(PackFormat.VERSION, publicKey, payload.size.toLong())
        assertEquals("435a5031" + "0001" + Hex.encode(publicKey) + "0000000000000017", Hex.encode(prefix))
        assertTrue(
            Ed25519.verify(publicKey, Hex.decode(expectedSignature), prefix, ByteArrayInputStream(payload))
        )
    }
}
