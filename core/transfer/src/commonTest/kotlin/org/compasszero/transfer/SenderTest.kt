package org.compasszero.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

private class PufferQuelle(private val inhalt: ByteArray) : Datenquelle {
    private var gelesen = 0
    override fun lies(puffer: ByteArray, offset: Int, laenge: Int): Int {
        val rest = inhalt.size - gelesen
        if (rest <= 0) return 0
        val menge = minOf(rest, laenge)
        inhalt.copyInto(puffer, offset, gelesen, gelesen + menge)
        gelesen += menge
        return menge
    }
}

class SenderTest {

    private fun angebot(groesse: Long) =
        Rahmen.Angebot(groesse, ByteArray(TransferFormat.HASH_SIZE), "paket.czp")

    private fun sender(inhalt: ByteArray, angesagt: Long = inhalt.size.toLong()) =
        Sender(PufferQuelle(inhalt), angebot(angesagt))

    private fun fehlerVon(antwort: SendeAntwort): TransferFehler = when (antwort) {
        is SendeAntwort.Fehlgeschlagen -> antwort.fehler
        else -> fail("erwartet war ein Fehler, bekommen: $antwort")
    }

    private fun rahmenVon(antwort: SendeAntwort): Rahmen = when (antwort) {
        is SendeAntwort.Senden -> antwort.rahmen
        else -> fail("erwartet war ein Rahmen zum Senden, bekommen: $antwort")
    }

    @Test
    fun vollstaendigerLaufSendetAllesUndSchliesstAb() {
        val inhalt = ByteArray(500) { (it % 91).toByte() }
        val s = sender(inhalt)

        assertIs<Rahmen.Angebot>(rahmenVon(s.beginne()))
        val gesammelt = ArrayList<Byte>()
        var antwort = s.rahmenEmpfangen(Rahmen.Annahme)
        while (true) {
            when (val rahmen = rahmenVon(antwort)) {
                is Rahmen.Daten -> gesammelt.addAll(rahmen.bytes.toList())
                is Rahmen.Fertig -> break
                else -> fail("unerwarteter Rahmen: $rahmen")
            }
            antwort = s.naechstes()
        }

        assertEquals(SendeZustand.Abgeschlossen, s.zustand)
        assertEquals(500L, s.gesendeteBytes)
        assertTrue(gesammelt.toByteArray().contentEquals(inhalt))
    }

    @Test
    fun zweitesBeginnenWirdAbgelehnt() {
        val s = sender(ByteArray(10))
        s.beginne()
        assertEquals(TransferFehler.RahmenAusserDerReihe, fehlerVon(s.beginne()))
    }

    @Test
    fun annahmeVorDemAngebotWirdAbgelehnt() {
        val s = sender(ByteArray(10))
        assertEquals(TransferFehler.RahmenAusserDerReihe, fehlerVon(s.rahmenEmpfangen(Rahmen.Annahme)))
    }

    @Test
    fun ablehnungIstKeinFehlerSondernEinErgebnis() {
        val s = sender(ByteArray(10))
        s.beginne()
        val antwort = s.rahmenEmpfangen(Rahmen.Ablehnung(Ablehnungsgrund.KeinPlatz))
        val abgelehnt = assertIs<SendeAntwort.Abgelehnt>(antwort)
        assertEquals(Ablehnungsgrund.KeinPlatz, abgelehnt.grund)
        assertEquals(SendeZustand.Beendet, s.zustand)
    }

    @Test
    fun abbruchDerGegenseiteBeendet() {
        val s = sender(ByteArray(100))
        s.beginne()
        s.rahmenEmpfangen(Rahmen.Annahme)
        val antwort = s.rahmenEmpfangen(Rahmen.Abbruch(Abbruchgrund.Zeitueberschreitung))
        val abgebrochen = assertIs<SendeAntwort.Abgebrochen>(antwort)
        assertEquals(Abbruchgrund.Zeitueberschreitung, abgebrochen.grund, "der Grund der Gegenseite geht verloren")
        assertEquals(SendeZustand.Beendet, s.zustand)
    }

    // Rahmen, die nur vom Sender kommen duerfen. Wer sie schickt, spricht ein
    // anderes Protokoll, als er behauptet.
    @Test
    fun rahmenDerFalschenRichtungWerdenAbgelehnt() {
        for (fremd in listOf(Rahmen.Fertig, Rahmen.Daten(byteArrayOf(1)), angebot(5))) {
            val s = sender(ByteArray(10))
            s.beginne()
            assertEquals(TransferFehler.RahmenAusserDerReihe, fehlerVon(s.rahmenEmpfangen(fremd)))
        }
    }

    // Die Quelle liefert mehr, als angesagt wurde: die Datei hat sich waehrend
    // des Sendens geaendert. Das Angebot samt Pruefsumme stimmt dann nicht mehr.
    @Test
    fun mehrDatenAlsAngesagtBrechenAb() {
        val s = sender(ByteArray(100), angesagt = 40)
        s.beginne()
        assertEquals(TransferFehler.ZuVieleDaten, fehlerVon(s.rahmenEmpfangen(Rahmen.Annahme)))
        assertEquals(SendeZustand.Beendet, s.zustand)
    }

    @Test
    fun wenigerDatenAlsAngesagtBrechenAb() {
        val s = sender(ByteArray(30), angesagt = 100)
        s.beginne()
        var antwort = s.rahmenEmpfangen(Rahmen.Annahme)
        while (antwort is SendeAntwort.Senden && antwort.rahmen is Rahmen.Daten) {
            antwort = s.naechstes()
        }
        assertEquals(TransferFehler.ZuWenigDaten, fehlerVon(antwort))
    }

    // Die eigene Datei laesst sich nicht mehr lesen -- entfernte Speicherkarte,
    // geloeschtes Paket. Das darf nicht als Fehler der Gegenseite erscheinen.
    @Test
    fun eineScheiterndeQuelleBrichtMitEigenerUrsacheAb() {
        val kaputt = object : Datenquelle {
            override fun lies(puffer: ByteArray, offset: Int, laenge: Int): Int = error("Datei weg")
        }
        val s = Sender(kaputt, angebot(100))
        s.beginne()
        val antwort = s.rahmenEmpfangen(Rahmen.Annahme)
        val fehler = assertIs<SendeAntwort.Fehlgeschlagen>(antwort)
        assertEquals(TransferFehler.QuelleNichtLesbar, fehler.fehler)
        assertEquals(Abbruchgrund.Lesefehler, fehler.senden?.grund)
        assertEquals(SendeZustand.Beendet, s.zustand)
    }

    // Eine Quelle, die mehr meldet als der Puffer fasst, ist ein Fehler auf der
    // eigenen Seite -- ungeprueft ergaebe das eine Bereichsverletzung mitten im
    // Senden statt einer benannten Ursache.
    @Test
    fun eineLuegendeQuelleBrichtSauberAb() {
        val luegt = object : Datenquelle {
            override fun lies(puffer: ByteArray, offset: Int, laenge: Int): Int = laenge + 1
        }
        val s = Sender(luegt, angebot(100_000))
        s.beginne()
        val fehler = assertIs<SendeAntwort.Fehlgeschlagen>(s.rahmenEmpfangen(Rahmen.Annahme))
        assertEquals(TransferFehler.QuelleNichtLesbar, fehler.fehler)
    }

    @Test
    fun naechstesVorDerAnnahmeWirdAbgelehnt() {
        val s = sender(ByteArray(10))
        s.beginne()
        assertEquals(TransferFehler.RahmenAusserDerReihe, fehlerVon(s.naechstes()))
    }

    @Test
    fun nachDemEndeGehtNichtsMehr() {
        val inhalt = ByteArray(20)
        val s = sender(inhalt)
        s.beginne()
        var antwort = s.rahmenEmpfangen(Rahmen.Annahme)
        while (rahmenVon(antwort) !is Rahmen.Fertig) antwort = s.naechstes()
        assertEquals(SendeZustand.Abgeschlossen, s.zustand)
        assertEquals(TransferFehler.RahmenAusserDerReihe, fehlerVon(s.naechstes()))
    }

    // Kein Rahmen darf groesser werden als der Empfangspuffer der Gegenseite,
    // sonst lehnt sie ihn ab, bevor sie ihn liest.
    @Test
    fun rahmenBleibenInnerhalbDerObergrenze() {
        val inhalt = ByteArray(TransferFormat.MAX_RAHMEN_NUTZLAST * 3 + 17)
        val s = sender(inhalt)
        s.beginne()
        var antwort = s.rahmenEmpfangen(Rahmen.Annahme)
        var rahmenZahl = 0
        while (true) {
            when (val rahmen = rahmenVon(antwort)) {
                is Rahmen.Daten -> {
                    assertTrue(
                        rahmen.bytes.size <= TransferFormat.MAX_RAHMEN_NUTZLAST,
                        "Rahmen mit ${rahmen.bytes.size} Bytes ist zu gross",
                    )
                    assertTrue(rahmen.bytes.isNotEmpty(), "leerer Datenrahmen")
                    rahmenZahl++
                }
                is Rahmen.Fertig -> break
                else -> fail("unerwarteter Rahmen: $rahmen")
            }
            antwort = s.naechstes()
        }
        assertTrue(rahmenZahl >= 4, "erwartet waren mehrere Rahmen, gezaehlt: $rahmenZahl")
        assertEquals(inhalt.size.toLong(), s.gesendeteBytes)
    }

    // Beide Zustandsmaschinen gegeneinander: was der Sender erzeugt, muss der
    // Empfaenger annehmen — sonst passen die beiden Haelften nicht zusammen.
    @Test
    fun senderUndEmpfaengerVerstehenSichVollstaendig() {
        val inhalt = ByteArray(9_000) { (it % 251).toByte() }
        val senke = object : Datensenke {
            val bytes = ArrayList<Byte>()
            var verworfen = false
            var abgeschlossen = false
            override fun schreibe(bytes: ByteArray, offset: Int, laenge: Int) {
                for (i in offset until offset + laenge) this.bytes.add(bytes[i])
            }
            override fun abschliessen() { abgeschlossen = true }
            override fun verwirf() { verworfen = true }
        }
        val pruefsumme = object : Pruefsumme {
            override fun fuettere(bytes: ByteArray, offset: Int, laenge: Int) = Unit
            override fun abschluss(): ByteArray = ByteArray(TransferFormat.HASH_SIZE)
        }
        val empfaenger = Empfaenger(senke, pruefsumme)
        val s = Sender(PufferQuelle(inhalt), angebot(inhalt.size.toLong()))

        // Angebot hin, Annahme zurueck.
        val angebotsRahmen = rahmenVon(s.beginne())
        assertIs<Antwort.Frage>(empfaenger.rahmenEmpfangen(angebotsRahmen))
        assertIs<Rahmen.Annahme>((empfaenger.entscheide(true) as Antwort.Senden).rahmen)

        var antwort = s.rahmenEmpfangen(Rahmen.Annahme)
        while (true) {
            val rahmen = rahmenVon(antwort)
            val beimEmpfaenger = empfaenger.rahmenEmpfangen(rahmen)
            if (rahmen is Rahmen.Fertig) {
                val fertig = assertIs<Antwort.Fertig>(beimEmpfaenger)
                assertEquals(inhalt.size.toLong(), fertig.bytes)
                break
            }
            assertEquals(Antwort.Nichts, beimEmpfaenger)
            antwort = s.naechstes()
        }

        assertTrue(senke.bytes.toByteArray().contentEquals(inhalt))
        assertTrue(!senke.verworfen)
    }
}
