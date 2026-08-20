package org.compasszero.security

import java.io.InputStream
import java.security.SecureRandom
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

class Ed25519Verification internal constructor(private val signer: Ed25519Signer) {

    fun update(bytes: ByteArray, offset: Int, length: Int) {
        signer.update(bytes, offset, length)
    }

    fun matches(signature: ByteArray): Boolean {
        if (signature.size != PackFormat.SIGNATURE_SIZE) return false
        return try {
            signer.verifySignature(signature)
        } catch (_: RuntimeException) {
            false
        }
    }
}

object Ed25519 {

    const val SEED_SIZE = 32
    private const val CHUNK = 64 * 1024

    fun generateSeed(): ByteArray {
        val seed = ByteArray(SEED_SIZE)
        SecureRandom().nextBytes(seed)
        return seed
    }

    fun publicKeyFromSeed(seed: ByteArray): ByteArray {
        require(seed.size == SEED_SIZE) { "seed must be $SEED_SIZE bytes" }
        return Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded
    }

    fun sign(seed: ByteArray, prefix: ByteArray, payload: InputStream): ByteArray {
        require(seed.size == SEED_SIZE) { "seed must be $SEED_SIZE bytes" }
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(seed, 0))
        signer.update(prefix, 0, prefix.size)
        val buffer = ByteArray(CHUNK)
        while (true) {
            val read = payload.read(buffer)
            if (read < 0) break
            signer.update(buffer, 0, read)
        }
        return signer.generateSignature()
    }

    // Streaming-Pruefung: der Aufrufer schiebt die Bytes selbst durch, damit derselbe
    // Lesedurchlauf gleichzeitig den Inhalt entpacken kann.
    fun startVerify(publicKey: ByteArray, prefix: ByteArray): Ed25519Verification? {
        if (publicKey.size != Ed25519PublicKeyParameters.KEY_SIZE) return null
        return try {
            val signer = Ed25519Signer()
            signer.init(false, Ed25519PublicKeyParameters(publicKey, 0))
            signer.update(prefix, 0, prefix.size)
            Ed25519Verification(signer)
        } catch (_: RuntimeException) {
            null
        }
    }

    fun verify(publicKey: ByteArray, signature: ByteArray, prefix: ByteArray, payload: InputStream): Boolean {
        val check = startVerify(publicKey, prefix) ?: return false
        val buffer = ByteArray(CHUNK)
        while (true) {
            val read = payload.read(buffer)
            if (read < 0) break
            check.update(buffer, 0, read)
        }
        return check.matches(signature)
    }
}
