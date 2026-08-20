package org.compasszero.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Die Wege, auf denen ein Empfang schiefgeht. Alle drei Faelle hier stammen aus
// einem Pruefdurchgang, der sie am Code gefunden hat, bevor sie jemanden ein
// Paket gekostet haben.
class EmpfangsFehlerwegeTest {

    private class Protokollsenke(
        private val scheitertAbSchreibvorgang: Int = Int.MAX_VALUE,
        private val scheitertBeimAbschliessen: Boolean = false,
    ) : Datensenke {
        var geschrieben = 0L
        var verworfen = false
        var abgeschlossen = false
        private var vorgaenge = 0

        override fun schreibe(bytes: ByteArray, offset: Int, laenge: Int) {
            vorgaenge++
            if (vorgaenge >= scheitertAbSchreibvorgang) error("Datentraeger voll")
            geschrieben += laenge
        }

        override fun abschliessen() {
            if (scheitertBeimAbschliessen) error("konnte nicht abgelegt werden")
            abgeschlossen = true
        }

        override fun verwirf() {
            verworfen = true
            geschrieben = 0
        }
    }

    private class NullPruefsumme : Pruefsumme {
        override fun fuettere(bytes: ByteArray, offset: Int, laenge: Int) = Unit
        override fun abschluss(): ByteArray = ByteArray(TransferFormat.HASH_SIZE)
    }

    private fun angebot(groesse: Int) =
        Rahmen.Angebot(groesse.toLong(), ByteArray(TransferFormat.HASH_SIZE), "paket.czp")

    private fun bisEmpfang(senke: Datensenke, groesse: Int): Empfaenger {
        val empfaenger = Empfaenger(senke, NullPruefsumme())
        empfaenger.rahmenEmpfangen(angebot(groesse))
        empfaenger.entscheide(true)
        return empfaenger
    }

    // Ein zweiter Fingertipp auf "Annehmen" darf kein fertig empfangenes Paket
    // loeschen. Genau das ist passiert, solange die Sperre nur am Rahmenempfang
    // hing und nicht an der Entscheidung.
    @Test
    fun entscheidungNachDemAbschlussLaesstDasPaketInRuhe() {
        val senke = Protokollsenke()
        val empfaenger = bisEmpfang(senke, 100)
        empfaenger.rahmenEmpfangen(Rahmen.Daten(ByteArray(100)))
        assertIs<Antwort.Fertig>(empfaenger.rahmenEmpfangen(Rahmen.Fertig))
        assertTrue(senke.abgeschlossen)

        val nochmal = empfaenger.entscheide(true)
        val fehler = assertIs<Antwort.Fehlgeschlagen>(nochmal)
        assertEquals(TransferFehler.RahmenAusserDerReihe, fehler.fehler)
        assertTrue(!senke.verworfen, "das fertige Paket wurde geloescht")
        assertEquals(EmpfangsZustand.Abgeschlossen, empfaenger.zustand, "der Abschluss wurde zurueckgenommen")
    }

    @Test
    fun zweitesAnnehmenWaehrendDesEmpfangsBrichtSauberAb() {
        val senke = Protokollsenke()
        val empfaenger = bisEmpfang(senke, 100)
        empfaenger.rahmenEmpfangen(Rahmen.Daten(ByteArray(50)))

        val fehler = assertIs<Antwort.Fehlgeschlagen>(empfaenger.entscheide(true))
        assertEquals(TransferFehler.RahmenAusserDerReihe, fehler.fehler)
        assertTrue(senke.verworfen, "das Halbfertige blieb liegen")
    }

    // Voller Datentraeger mitten im Empfang. Ohne Fang liefe die Ausnahme aus der
    // Zustandsmaschine heraus und die Rumpfdatei bliebe liegen.
    @Test
    fun schreibfehlerRaeumtAufUndMeldetDieEigeneUrsache() {
        val senke = Protokollsenke(scheitertAbSchreibvorgang = 2)
        val empfaenger = bisEmpfang(senke, 300)
        assertEquals(Antwort.Nichts, empfaenger.rahmenEmpfangen(Rahmen.Daten(ByteArray(100))))

        val antwort = empfaenger.rahmenEmpfangen(Rahmen.Daten(ByteArray(100)))
        val fehler = assertIs<Antwort.Fehlgeschlagen>(antwort)
        assertEquals(TransferFehler.ZielNichtBeschreibbar, fehler.fehler)
        assertEquals(Abbruchgrund.Lesefehler, fehler.senden?.grund)
        assertTrue(senke.verworfen, "das Halbfertige blieb liegen")
        assertEquals(EmpfangsZustand.Beendet, empfaenger.zustand)
    }

    // Scheitert erst das Ablegen, waere die Datei abgeschnitten. Als "fertig"
    // gemeldet ginge sie anschliessend als beschaedigtes oder manipuliertes
    // Paket durch die Signaturpruefung -- ein Transportfehler, der wie ein
    // Angriff aussaehe.
    @Test
    fun fehlerBeimAblegenGiltNichtAlsFertig() {
        val senke = Protokollsenke(scheitertBeimAbschliessen = true)
        val empfaenger = bisEmpfang(senke, 100)
        empfaenger.rahmenEmpfangen(Rahmen.Daten(ByteArray(100)))

        val antwort = empfaenger.rahmenEmpfangen(Rahmen.Fertig)
        val fehler = assertIs<Antwort.Fehlgeschlagen>(antwort)
        assertEquals(TransferFehler.ZielNichtBeschreibbar, fehler.fehler)
        assertTrue(senke.verworfen, "die abgeschnittene Datei blieb liegen")
        assertEquals(EmpfangsZustand.Beendet, empfaenger.zustand)
    }

    @Test
    fun nutzerabbruchRaeumtAufUndNenntDenGrund() {
        val senke = Protokollsenke()
        val empfaenger = bisEmpfang(senke, 500)
        empfaenger.rahmenEmpfangen(Rahmen.Daten(ByteArray(200)))

        val antwort = empfaenger.abbrich()
        val senden = assertIs<Antwort.Senden>(antwort)
        val abbruch = assertIs<Rahmen.Abbruch>(senden.rahmen)
        assertEquals(Abbruchgrund.NutzerBricht, abbruch.grund)
        assertTrue(senke.verworfen)
        assertEquals(EmpfangsZustand.Beendet, empfaenger.zustand)
    }

    @Test
    fun nutzerabbruchNachDemAbschlussLoeschtNichts() {
        val senke = Protokollsenke()
        val empfaenger = bisEmpfang(senke, 10)
        empfaenger.rahmenEmpfangen(Rahmen.Daten(ByteArray(10)))
        assertIs<Antwort.Fertig>(empfaenger.rahmenEmpfangen(Rahmen.Fertig))

        assertIs<Antwort.Fehlgeschlagen>(empfaenger.abbrich())
        assertTrue(!senke.verworfen, "das fertige Paket wurde geloescht")
    }

    // Die Zustandsmaschine darf sich nicht darauf verlassen, dass jeder Rahmen
    // durch den Codec kam -- der prueft dasselbe, ist aber nicht erzwungen.
    @Test
    fun unmoeglicheAngebotswerteWerdenAuchOhneCodecAbgewiesen() {
        val zuGross = Rahmen.Angebot(Long.MAX_VALUE, ByteArray(TransferFormat.HASH_SIZE), "p.czp")
        val leer = Rahmen.Angebot(0, ByteArray(TransferFormat.HASH_SIZE), "p.czp")
        val kurzeSumme = Rahmen.Angebot(100, ByteArray(4), "p.czp")

        for ((rahmen, erwartet) in listOf(
            zuGross to TransferFehler.GroesseUnmoeglich,
            leer to TransferFehler.GroesseUnmoeglich,
            kurzeSumme to TransferFehler.AngebotUnvollstaendig,
        )) {
            val empfaenger = Empfaenger(Protokollsenke(), NullPruefsumme())
            val fehler = assertIs<Antwort.Fehlgeschlagen>(empfaenger.rahmenEmpfangen(rahmen))
            assertEquals(erwartet, fehler.fehler, "Angebot ${rahmen.groesse}/${rahmen.pruefsumme.size}")
        }
    }
}
