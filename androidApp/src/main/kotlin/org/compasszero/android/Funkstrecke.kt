package org.compasszero.android

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import org.compasszero.transfer.Datenquelle
import org.compasszero.transfer.Datensenke
import org.compasszero.transfer.Pruefsumme

/**
 * Die Andockstellen zwischen Android und `core/transfer`.
 *
 * `core/transfer` kennt weder Dateien noch Funk -- es kennt `Datenquelle` und
 * `Datensenke`. Hier stehen die vier Stuecke, die diese beiden Schnittstellen
 * auf einem Android-Geraet ausfuellen, und sonst nichts. Die Zustandsmaschine
 * bleibt dort drueben, wo sie ohne Geraet pruefbar ist.
 */
object Funk {

    /**
     * Die Kennung des Dienstes auf dem Funkkanal.
     *
     * Fest und eigens fuer dieses Programm gewuerfelt: Zwei Compass-Zero-Geraete
     * finden einander daran, und ein fremdes Programm stolpert nicht hinein.
     * SIE DARF SICH NIE AENDERN -- eine neue Kennung heisst, dass eine neuere
     * Ausgabe eine aeltere nicht mehr erreicht.
     */
    val KENNUNG: UUID = UUID.fromString("6f2b1d54-9c3a-4e77-bf10-2a5c8e0d7431")

    /** Was im Dienstverzeichnis des Geraets steht. */
    const val DIENSTNAME = "Compass Zero Paketaustausch"

    /**
     * Wie lange eine Uebertragung ungefaehr dauert, in Sekunden.
     *
     * 150 kB/s ist der mittlere Wert aus den Messungen im Entwurf; RFCOMM
     * schwankt je nach Geraet zwischen 100 und 250. Die Zahl ist eine
     * Groessenordnung und wird auch so angezeigt -- sie soll jemanden davon
     * abhalten, eine 400-MB-Karte ueber Funk zu schicken, und nicht eine
     * Ankunftszeit versprechen.
     */
    fun dauerSekunden(bytes: Long): Long = (bytes / 150_000L).coerceAtLeast(1L)

    fun dauertext(bytes: Long): String {
        val s = dauerSekunden(bytes)
        return when {
            s < 90 -> "etwa $s Sekunden"
            s < 5400 -> "etwa ${(s + 30) / 60} Minuten"
            else -> "etwa %.1f Stunden".format(s / 3600.0)
        }
    }
}

/**
 * Ein Byte-Strom als Datenquelle -- die Funkverbindung, aus der Rahmen gelesen
 * werden.
 *
 * `InputStream.read` gibt bei Stromende -1 zurueck, und genau das erwartet
 * `RahmenLeser`: Werte <= 0 bedeuten dort Ende. Deshalb steht hier keine
 * Umrechnung.
 */
class StromQuelle(private val strom: InputStream) : Datenquelle {
    override fun lies(puffer: ByteArray, offset: Int, laenge: Int): Int =
        try {
            strom.read(puffer, offset, laenge)
        } catch (fehler: Exception) {
            // Eine abgerissene Verbindung ist ein Stromende und kein Absturz.
            // Der Leser macht daraus "unvollstaendiger Rahmen", und die
            // Oberflaeche kann das benennen.
            -1
        }
}

/** Eine Datei als Datenquelle -- das Paket, das gesendet wird. */
class DateiQuelle(datei: File) : Datenquelle, AutoCloseable {

    private val strom = datei.inputStream().buffered()

    override fun lies(puffer: ByteArray, offset: Int, laenge: Int): Int =
        strom.read(puffer, offset, laenge)

    override fun close() {
        try {
            strom.close()
        } catch (fehler: Exception) {
            // Beim Schliessen einer Lesequelle ist nichts mehr zu retten.
        }
    }
}

/**
 * Die Zieldatei eines Empfangs.
 *
 * ZUERST ".teil", ERST BEIM ABSCHLUSS DER RICHTIGE NAME. Wer waehrend der
 * Uebertragung abbricht -- Funkloch, leerer Akku, Abbruchknopf --, hinterlaesst
 * sonst eine halbe Datei unter einem Namen, den der Lader fuer ein Paket haelt.
 * Dieselbe Regel gilt beim Einlesen einer Karte von Hand, aus demselben Grund.
 *
 * Und der Abschluss legt die Datei NICHT an ihren Platz: Er legt sie in den
 * Eingang. Was von einem fremden Geraet kommt, ist bis zur Signaturpruefung
 * unvertrauenswuerdig -- ein Paket ueber Funk ist kein besseres Paket.
 */
class DateiSenke(private val ziel: File) : Datensenke {

    private val teil = File(ziel.parentFile, ziel.name + ".teil")
    private val strom = teil.outputStream().buffered()
    private var offen = true

    override fun schreibe(bytes: ByteArray, offset: Int, laenge: Int) {
        strom.write(bytes, offset, laenge)
    }

    override fun abschliessen() {
        // Erst alles hinausschreiben, dann umbenennen. Ohne das Leeren des
        // Puffers steht der Name schon richtig da, waehrend das Ende der Datei
        // noch im Speicher liegt.
        strom.flush()
        strom.close()
        offen = false
        if (ziel.exists() && !ziel.delete()) {
            teil.delete()
            error("die vorhandene Datei ${ziel.name} liess sich nicht ersetzen")
        }
        if (!teil.renameTo(ziel)) {
            teil.delete()
            error("die empfangene Datei liess sich nicht ablegen")
        }
    }

    override fun verwirf() {
        if (offen) {
            try {
                strom.close()
            } catch (fehler: Exception) {
                // Der naechste Schritt loescht die Datei ohnehin.
            }
            offen = false
        }
        teil.delete()
    }
}

/**
 * SHA-256 fuer die Pruefsumme im Angebot.
 *
 * SIE IST KEIN SICHERHEITSMERKMAL. Der Sender waehlt sie selbst; sie findet
 * Uebertragungsfehler und sonst nichts. Die Sicherheitsentscheidung faellt in
 * `core/security` an der Signatur im Paket -- danach, getrennt, und mit einem
 * eigenen Lesedurchlauf.
 */
class Sha256Pruefsumme : Pruefsumme {

    private val kern = MessageDigest.getInstance("SHA-256")

    override fun fuettere(bytes: ByteArray, offset: Int, laenge: Int) {
        kern.update(bytes, offset, laenge)
    }

    override fun abschluss(): ByteArray = kern.digest()
}

/** Bildet die Pruefsumme ueber eine ganze Datei, fuer das Angebot. */
fun pruefsummeVon(datei: File): ByteArray {
    val kern = MessageDigest.getInstance("SHA-256")
    datei.inputStream().use { strom ->
        val puffer = ByteArray(64 * 1024)
        while (true) {
            val gelesen = strom.read(puffer)
            if (gelesen <= 0) break
            kern.update(puffer, 0, gelesen)
        }
    }
    return kern.digest()
}

/** Schreibt fertige Rahmenbytes und sorgt dafuer, dass sie das Geraet verlassen. */
class Rahmenschreiber(private val strom: OutputStream) {
    fun schreibe(bytes: ByteArray) {
        strom.write(bytes)
        // Ohne flush liegt der letzte Rahmen im Puffer, waehrend beide Seiten
        // aufeinander warten. Bei einer Zustandsmaschine, die auf Antwort
        // wartet, ist das kein langsamer Ablauf, sondern ein Stillstand.
        strom.flush()
    }
}
