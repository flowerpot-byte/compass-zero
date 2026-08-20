package org.compasszero.transfer

// Woher die zu sendenden Bytes kommen. Liefert 0, wenn nichts mehr da ist.
interface Datenquelle {
    fun lies(puffer: ByteArray, offset: Int, laenge: Int): Int
}

enum class SendeZustand { Bereit, ErwarteAntwort, Sendet, Abgeschlossen, Beendet }

sealed interface SendeAntwort {
    class Senden(val rahmen: Rahmen) : SendeAntwort

    // Kein Fehler, sondern ein Ergebnis: die Gegenseite will das Paket nicht.
    class Abgelehnt(val grund: Ablehnungsgrund) : SendeAntwort

    // Die Gegenseite hat abgebrochen. Der Grund wird durchgereicht, damit die
    // Oberflaeche "Empfaenger hat abgebrochen" von "Empfaenger konnte nicht
    // mehr schreiben" unterscheiden kann.
    class Abgebrochen(val grund: Abbruchgrund) : SendeAntwort

    class Fehlgeschlagen(val fehler: TransferFehler, val senden: Rahmen.Abbruch?) : SendeAntwort
}

// Bietet ein Paket an und sendet es Stueck fuer Stueck. Angeboten werden nur
// Pakete aus der eigenen Bibliothek; gesendet werden genau die Bytes, die
// ankamen — hier wird nichts neu unterschrieben und nichts veraendert.
//
// Der Aufrufer treibt: beginne(), den erhaltenen Rahmen senden, Antworten der
// Gegenseite in rahmenEmpfangen() geben, danach abwechselnd senden und
// naechstes() rufen, bis Fertig kommt oder ein Fehler.
class Sender(
    private val quelle: Datenquelle,
    private val angebot: Rahmen.Angebot,
    puffergroesse: Int = TransferFormat.MAX_RAHMEN_NUTZLAST,
) {

    init {
        require(puffergroesse in 1..TransferFormat.MAX_RAHMEN_NUTZLAST) {
            "Puffer muss zwischen 1 und ${TransferFormat.MAX_RAHMEN_NUTZLAST} Bytes liegen"
        }
    }

    private val puffer = ByteArray(puffergroesse)

    var zustand: SendeZustand = SendeZustand.Bereit
        private set

    var gesendeteBytes: Long = 0
        private set

    fun beginne(): SendeAntwort {
        if (zustand != SendeZustand.Bereit) return beende(TransferFehler.RahmenAusserDerReihe)
        zustand = SendeZustand.ErwarteAntwort
        return SendeAntwort.Senden(angebot)
    }

    fun rahmenEmpfangen(rahmen: Rahmen): SendeAntwort {
        if (zustand == SendeZustand.Abgeschlossen || zustand == SendeZustand.Beendet) {
            return SendeAntwort.Fehlgeschlagen(TransferFehler.RahmenAusserDerReihe, null)
        }
        return when (rahmen) {
            is Rahmen.Annahme -> {
                if (zustand != SendeZustand.ErwarteAntwort) return beende(TransferFehler.RahmenAusserDerReihe)
                zustand = SendeZustand.Sendet
                naechstes()
            }
            is Rahmen.Ablehnung -> {
                if (zustand != SendeZustand.ErwarteAntwort) return beende(TransferFehler.RahmenAusserDerReihe)
                zustand = SendeZustand.Beendet
                SendeAntwort.Abgelehnt(rahmen.grund)
            }
            is Rahmen.Abbruch -> {
                zustand = SendeZustand.Beendet
                SendeAntwort.Abgebrochen(rahmen.grund)
            }
            // Angebot, Daten und Fertig laufen in die andere Richtung.
            is Rahmen.Angebot, is Rahmen.Daten, is Rahmen.Fertig ->
                beende(TransferFehler.RahmenAusserDerReihe)
        }
    }

    fun naechstes(): SendeAntwort {
        if (zustand != SendeZustand.Sendet) {
            // Nach dem Ende wird kein weiterer Abbruch mehr erzeugt: Ein Aufrufer,
            // der das Gesendete stumpf abschickt, wuerde sonst eine tote
            // Verbindung mit Abbruch-Rahmen zuschuetten.
            if (zustand == SendeZustand.Abgeschlossen || zustand == SendeZustand.Beendet) {
                return SendeAntwort.Fehlgeschlagen(TransferFehler.RahmenAusserDerReihe, null)
            }
            return beende(TransferFehler.RahmenAusserDerReihe)
        }
        val gelesen = try {
            quelle.lies(puffer, 0, puffer.size)
        } catch (fehlschlag: Exception) {
            // Die eigene Datei laesst sich nicht mehr lesen -- entfernte
            // Speicherkarte, geloeschtes Paket. Die Gegenseite erfaehrt den Grund,
            // statt dass die Ausnahme aus der Zustandsmaschine herausfliegt.
            return beende(TransferFehler.QuelleNichtLesbar, Abbruchgrund.Lesefehler)
        }
        // Eine Quelle, die mehr liefert als der Puffer fasst, ist ein Fehler auf
        // der eigenen Seite. Ungeprueft ergaebe das eine Bereichsverletzung
        // mitten im Senden statt einer benannten Ursache.
        if (gelesen > puffer.size) return beende(TransferFehler.QuelleNichtLesbar, Abbruchgrund.Lesefehler)
        if (gelesen <= 0) {
            // Die Quelle ist leer. Weniger als angesagt heisst: die Datei hat
            // sich waehrend des Sendens verkuerzt — dann stimmt auch die
            // Pruefsumme im Angebot nicht mehr.
            if (gesendeteBytes < angebot.groesse) return beende(TransferFehler.ZuWenigDaten)
            zustand = SendeZustand.Abgeschlossen
            return SendeAntwort.Senden(Rahmen.Fertig)
        }
        if (gesendeteBytes + gelesen > angebot.groesse) return beende(TransferFehler.ZuVieleDaten)
        gesendeteBytes += gelesen
        // Eigenes Feld je Rahmen: der Puffer wird beim naechsten Lesen
        // ueberschrieben, und der Codec kopiert nicht.
        return SendeAntwort.Senden(Rahmen.Daten(puffer.copyOfRange(0, gelesen)))
    }

    // Abbruch auf Wunsch des Nutzers, mit echtem Grund statt Protokollfehler.
    fun abbrich(grund: Abbruchgrund = Abbruchgrund.NutzerBricht): SendeAntwort {
        if (zustand == SendeZustand.Abgeschlossen || zustand == SendeZustand.Beendet) {
            return SendeAntwort.Fehlgeschlagen(TransferFehler.RahmenAusserDerReihe, null)
        }
        zustand = SendeZustand.Beendet
        return SendeAntwort.Senden(Rahmen.Abbruch(grund))
    }

    private fun beende(
        fehler: TransferFehler,
        grund: Abbruchgrund = Abbruchgrund.Protokollfehler,
    ): SendeAntwort {
        zustand = SendeZustand.Beendet
        return SendeAntwort.Fehlgeschlagen(fehler, Rahmen.Abbruch(grund))
    }
}
