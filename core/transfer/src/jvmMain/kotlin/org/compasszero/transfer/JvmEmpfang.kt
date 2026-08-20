package org.compasszero.transfer

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest

class Sha256Pruefsumme : Pruefsumme {

    private val digest = MessageDigest.getInstance("SHA-256")

    override fun fuettere(bytes: ByteArray, offset: Int, laenge: Int) {
        digest.update(bytes, offset, laenge)
    }

    override fun abschluss(): ByteArray = digest.digest()
}

// Nimmt die empfangenen Bytes in einer Datei auf.
//
// Die Datei entsteht erst beim ersten Schreiben, nicht schon beim Anlegen der
// Senke: Angebot und Entscheidung des Nutzers kommen vorher, und eine leere
// Datei, die nach einer Ablehnung liegen bleibt, hat niemand bestellt.
//
// Das Ziel gehoert ins app-eigene Verzeichnis: dort kommt zwischen Empfang und
// Signaturpruefung niemand an die Datei, und es braucht auf keinem Android-Stand
// eine Berechtigung.
class DateiSenke(private val ziel: File) : Datensenke {

    private enum class Stand { Bereit, Offen, Abgeschlossen, Verworfen }

    private var stand = Stand.Bereit
    private var strom: OutputStream? = null

    override fun schreibe(bytes: ByteArray, offset: Int, laenge: Int) {
        val offen = when (stand) {
            Stand.Bereit -> ziel.outputStream().buffered().also { strom = it; stand = Stand.Offen }
            Stand.Offen -> strom ?: throw IOException("Senke ist offen, hat aber keinen Strom")
            Stand.Abgeschlossen, Stand.Verworfen -> throw IOException("Senke ist bereits geschlossen")
        }
        offen.write(bytes, offset, laenge)
    }

    // Scheitert das Schliessen hier, koennen Bytes fehlen. Der Fehler wird
    // durchgereicht, statt eine abgeschnittene Datei als fertig auszugeben.
    override fun abschliessen() {
        if (stand == Stand.Verworfen) throw IOException("Senke wurde bereits verworfen")
        if (stand == Stand.Abgeschlossen) return
        val offen = strom
        stand = Stand.Abgeschlossen
        strom = null
        offen?.use { it.flush() }
    }

    // Laeuft auf dem Fehlerweg. Ein Fehler beim Schliessen aendert hier nichts
    // mehr, das Ziel wird ohnehin geloescht — er darf aber nicht die eigentliche
    // Fehlerursache verdecken. Mehrfaches Aufrufen ist zulaessig.
    override fun verwirf() {
        val offen = strom
        strom = null
        stand = Stand.Verworfen
        if (offen != null) runCatching { offen.close() }
        // Ein halbes Paket darf nie liegen bleiben: beim naechsten Start saehe es
        // aus wie ein vollstaendiges.
        ziel.delete()
    }
}

// Liest einen Byte-Strom fuer den Rahmenleser. Liefert 0 am Ende, wie die
// Schnittstelle es verlangt — java.io meldet das Ende mit -1.
class StromQuelle(private val strom: java.io.InputStream) : Datenquelle {
    override fun lies(puffer: ByteArray, offset: Int, laenge: Int): Int {
        val gelesen = strom.read(puffer, offset, laenge)
        return if (gelesen < 0) 0 else gelesen
    }
}
