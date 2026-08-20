package org.compasszero.security

import java.io.InputStream
import java.security.MessageDigest

object Digests {

    fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)

    /**
     * Pruefsumme im Durchlauf, ohne die Daten im Speicher zu halten.
     *
     * Fuer eine Kartendatei von einigen hundert Megabyte ist das der einzige
     * gangbare Weg: Ein Telefon hat keine 350 MB Halde uebrig.
     */
    fun sha256Stream(quelle: InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val puffer = ByteArray(64 * 1024)
        while (true) {
            val gelesen = quelle.read(puffer)
            if (gelesen < 0) break
            if (gelesen > 0) digest.update(puffer, 0, gelesen)
        }
        return digest.digest()
    }

    fun fingerprint(publicKey: ByteArray): String =
        Hex.encode(sha256(publicKey))
}
