package org.compasszero.transfer

// Wohin die empfangenen Bytes gehen. Die Zustandsmaschine kennt keine Dateien:
// so laesst sie sich auf jeder Plattform und in Tests ohne Datentraeger fahren.
//
// Genau eine der beiden Abschlussmethoden laeuft am Ende: abschliessen() bei
// Erfolg, verwirf() sonst. Beide gehoeren in die Schnittstelle — steht der
// Erfolgsabschluss nur an der konkreten Umsetzung, vergisst ihn der Aufrufer,
// und eine ungeschriebene Restmenge macht aus einem sauberen Empfang ein
// scheinbar beschaedigtes Paket.
interface Datensenke {
    fun schreibe(bytes: ByteArray, offset: Int, laenge: Int)

    // Muss alles Geschriebene dauerhaft ablegen. Scheitert das, gilt der Empfang
    // als fehlgeschlagen — eine abgeschnittene Datei darf nie als fertig gelten.
    fun abschliessen()

    fun verwirf()
}

interface Pruefsumme {
    fun fuettere(bytes: ByteArray, offset: Int, laenge: Int)
    fun abschluss(): ByteArray
}

enum class EmpfangsZustand { ErwarteAngebot, ErwarteEntscheidung, Empfaengt, Abgeschlossen, Beendet }

sealed interface Antwort {
    data object Nichts : Antwort

    class Senden(val rahmen: Rahmen) : Antwort

    // Der Aufrufer muss den Nutzer fragen und danach entscheide() rufen.
    class Frage(val angebot: Rahmen.Angebot) : Antwort

    class Fertig(val bytes: Long) : Antwort

    // Die Gegenseite hat abgebrochen. Kein Fehler auf unserer Seite, und der
    // Grund wird durchgereicht, damit die Oberflaeche ihn benennen kann.
    class Abgebrochen(val grund: Abbruchgrund) : Antwort

    // senden ist der Rahmen, der dem Gegenueber noch mitzuteilen ist, oder null,
    // wenn die Verbindung ohnehin schon beendet war.
    class Fehlgeschlagen(val fehler: TransferFehler, val senden: Rahmen.Abbruch?) : Antwort
}

// Nimmt ein Paket entgegen und gibt es nur dann als vollstaendig aus, wenn genau
// so viele Bytes ankamen wie angesagt und die Pruefsumme dazu passt.
//
// Was hier NICHT passiert: eine Vertrauensentscheidung. Die empfangenen Bytes
// sind unvertrauenswuerdig, bis core/security sie in seinem einen Lesedurchlauf
// geprueft hat — genau wie eine von Hand kopierte Datei. Die Pruefsumme aus dem
// Angebot findet Uebertragungsfehler, sonst nichts: der Sender waehlt sie selbst.
class Empfaenger(private val ziel: Datensenke, private val pruefsumme: Pruefsumme) {

    var zustand: EmpfangsZustand = EmpfangsZustand.ErwarteAngebot
        private set

    var angesagteBytes: Long = 0
        private set

    var empfangeneBytes: Long = 0
        private set

    private var sollPruefsumme = ByteArray(0)

    private val schonBeendet: Boolean
        get() = zustand == EmpfangsZustand.Abgeschlossen || zustand == EmpfangsZustand.Beendet

    fun rahmenEmpfangen(rahmen: Rahmen): Antwort {
        // Nach dem Ende ist Schluss. Ein weiterer Rahmen waere ein zweiter
        // Empfang auf derselben Verbindung, den niemand mehr beaufsichtigt —
        // und ein bereits fertiges Paket darf dabei nicht verworfen werden.
        if (schonBeendet) return Antwort.Fehlgeschlagen(TransferFehler.RahmenAusserDerReihe, null)
        return when (rahmen) {
            is Rahmen.Angebot -> nimmAngebot(rahmen)
            is Rahmen.Daten -> nimmDaten(rahmen)
            is Rahmen.Fertig -> schliesseAb()
            is Rahmen.Abbruch -> {
                raeumeAuf()
                zustand = EmpfangsZustand.Beendet
                Antwort.Abgebrochen(rahmen.grund)
            }
            // Annahme und Ablehnung laufen in die andere Richtung. Wer sie
            // schickt, spricht ein anderes Protokoll, als er behauptet.
            is Rahmen.Annahme, is Rahmen.Ablehnung -> beende(TransferFehler.RahmenAusserDerReihe)
        }
    }

    fun entscheide(annehmen: Boolean, grund: Ablehnungsgrund = Ablehnungsgrund.NutzerLehntAb): Antwort {
        // Dieselbe Sperre wie beim Rahmenempfang. Ohne sie wuerde ein zweiter
        // Aufruf — ein doppelter Fingertipp genuegt — ein fertig empfangenes
        // Paket wieder loeschen.
        if (schonBeendet) return Antwort.Fehlgeschlagen(TransferFehler.RahmenAusserDerReihe, null)
        if (zustand != EmpfangsZustand.ErwarteEntscheidung) {
            return beende(TransferFehler.RahmenAusserDerReihe)
        }
        if (!annehmen) {
            raeumeAuf()
            zustand = EmpfangsZustand.Beendet
            return Antwort.Senden(Rahmen.Ablehnung(grund))
        }
        zustand = EmpfangsZustand.Empfaengt
        return Antwort.Senden(Rahmen.Annahme)
    }

    // Abbruch auf Wunsch des Nutzers. Ohne diesen Weg bliebe dem Aufrufer nur,
    // an der Zustandsmaschine vorbei aufzuraeumen — mit einem Zustand, der
    // weiterlaeuft, als sei nichts geschehen.
    fun abbrich(grund: Abbruchgrund = Abbruchgrund.NutzerBricht): Antwort {
        if (schonBeendet) return Antwort.Fehlgeschlagen(TransferFehler.RahmenAusserDerReihe, null)
        raeumeAuf()
        zustand = EmpfangsZustand.Beendet
        return Antwort.Senden(Rahmen.Abbruch(grund))
    }

    private fun nimmAngebot(angebot: Rahmen.Angebot): Antwort {
        if (zustand != EmpfangsZustand.ErwarteAngebot) return beende(TransferFehler.RahmenAusserDerReihe)
        // Zweite Verteidigungslinie: Der Codec prueft dasselbe, aber die
        // Zustandsmaschine darf sich nicht darauf verlassen, dass jeder Rahmen
        // durch ihn kam.
        if (angebot.groesse <= 0 || angebot.groesse > TransferFormat.MAX_PAKET_BYTES) {
            return beende(TransferFehler.GroesseUnmoeglich)
        }
        if (angebot.pruefsumme.size != TransferFormat.HASH_SIZE) {
            return beende(TransferFehler.AngebotUnvollstaendig)
        }
        angesagteBytes = angebot.groesse
        sollPruefsumme = angebot.pruefsumme
        zustand = EmpfangsZustand.ErwarteEntscheidung
        return Antwort.Frage(angebot)
    }

    private fun nimmDaten(daten: Rahmen.Daten): Antwort {
        if (zustand != EmpfangsZustand.Empfaengt) return beende(TransferFehler.RahmenAusserDerReihe)
        // Vor dem Schreiben pruefen: ein Byte ueber der Ansage darf die Senke nie
        // erreichen, sonst waechst die Datei weiter, waehrend die Ansage laengst
        // gerissen ist.
        if (empfangeneBytes + daten.bytes.size > angesagteBytes) {
            return beende(TransferFehler.ZuVieleDaten)
        }
        // Voller Datentraeger, entfernte Speicherkarte: auf einem alten Geraet
        // der Regelfall, nicht der Sonderfall. Ohne diesen Fang liefe die
        // Ausnahme aus der Zustandsmaschine heraus, das Halbfertige bliebe
        // liegen und saehe beim naechsten Start aus wie ein ganzes Paket.
        // Gefangen wird jede Ausnahme, nicht nur eine Dateiausnahme: Was immer
        // die Senke wirft, das Halbfertige muss weg. Die Art des Fehlers aendert
        // daran nichts.
        try {
            ziel.schreibe(daten.bytes, 0, daten.bytes.size)
            pruefsumme.fuettere(daten.bytes, 0, daten.bytes.size)
        } catch (fehlschlag: Exception) {
            return beende(TransferFehler.ZielNichtBeschreibbar, Abbruchgrund.Lesefehler)
        }
        empfangeneBytes += daten.bytes.size
        return Antwort.Nichts
    }

    private fun schliesseAb(): Antwort {
        if (zustand != EmpfangsZustand.Empfaengt) return beende(TransferFehler.RahmenAusserDerReihe)
        if (empfangeneBytes < angesagteBytes) return beende(TransferFehler.ZuWenigDaten)
        if (!pruefsumme.abschluss().contentEquals(sollPruefsumme)) {
            return beende(TransferFehler.PruefsummeFalsch)
        }
        // Erst wenn das Geschriebene wirklich liegt, gilt der Empfang als fertig.
        // Sonst ginge eine ungeschriebene Restmenge als "Paket beschaedigt" durch
        // die Signaturpruefung — ein Transportfehler, der wie Manipulation aussaehe.
        try {
            ziel.abschliessen()
        } catch (fehlschlag: Exception) {
            return beende(TransferFehler.ZielNichtBeschreibbar, Abbruchgrund.Lesefehler)
        }
        zustand = EmpfangsZustand.Abgeschlossen
        return Antwort.Fertig(empfangeneBytes)
    }

    private fun beende(
        fehler: TransferFehler,
        grund: Abbruchgrund = Abbruchgrund.Protokollfehler,
    ): Antwort {
        raeumeAuf()
        zustand = EmpfangsZustand.Beendet
        return Antwort.Fehlgeschlagen(fehler, Rahmen.Abbruch(grund))
    }

    // Aufraeumen scheitert nicht: Wir sind bereits auf dem Fehlerweg, und ein
    // zweiter Fehler duerfte die erste Ursache nicht verdecken.
    private fun raeumeAuf() {
        runCatching { ziel.verwirf() }
    }
}
