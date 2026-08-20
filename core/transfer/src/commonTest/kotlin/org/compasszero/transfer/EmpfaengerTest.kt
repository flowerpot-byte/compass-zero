package org.compasszero.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

private class SammelSenke : Datensenke {
    val bytes = ArrayList<Byte>()
    var verworfen = false
    var abgeschlossen = false
    override fun schreibe(bytes: ByteArray, offset: Int, laenge: Int) {
        for (i in offset until offset + laenge) this.bytes.add(bytes[i])
    }
    override fun abschliessen() {
        abgeschlossen = true
    }
    override fun verwirf() {
        verworfen = true
        bytes.clear()
    }
}

// Eine Senke, die ab dem n-ten Schreibversuch scheitert -- voller Datentraeger,
// entfernte Speicherkarte. Auf alten Geraeten der Regelfall.
private class SperrigeSenke(private val abSchreibvorgang: Int, private val beimAbschliessen: Boolean = false) : Datensenke {
    var verworfen = false
    private var schreibvorgaenge = 0
    override fun schreibe(bytes: ByteArray, offset: Int, laenge: Int) {
        schreibvorgaenge++
        if (!beimAbschliessen && schreibvorgaenge >= abSchreibvorgang) error("Datentraeger voll")
    }
    override fun abschliessen() {
        if (beimAbschliessen) error("konnte nicht abgelegt werden")
    }
    override fun verwirf() {
        verworfen = true
    }
}

// Eine Pruefsumme, die nur zaehlt und summiert. Fuer die Zustandsmaschine kommt
// es nicht auf ein echtes SHA-256 an, sondern darauf, dass sie genau die Bytes
// sieht, die auch in der Senke landen.
private class ZaehlPruefsumme : Pruefsumme {
    var gesehen = 0L
    private var summe = 0
    override fun fuettere(bytes: ByteArray, offset: Int, laenge: Int) {
        gesehen += laenge
        for (i in offset until offset + laenge) summe = (summe + (bytes[i].toInt() and 0xFF)) and 0xFF
    }
    override fun abschluss(): ByteArray = ByteArray(TransferFormat.HASH_SIZE) { if (it == 0) summe.toByte() else 0 }
}

class EmpfaengerTest {

    private fun sollPruefsumme(inhalt: ByteArray): ByteArray {
        var summe = 0
        for (b in inhalt) summe = (summe + (b.toInt() and 0xFF)) and 0xFF
        return ByteArray(TransferFormat.HASH_SIZE) { if (it == 0) summe.toByte() else 0 }
    }

    private fun angebot(inhalt: ByteArray, name: String = "paket.czp") =
        Rahmen.Angebot(inhalt.size.toLong(), sollPruefsumme(inhalt), name)

    private class Aufbau(val empfaenger: Empfaenger, val senke: SammelSenke)

    private fun neu(): Aufbau {
        val senke = SammelSenke()
        return Aufbau(Empfaenger(senke, ZaehlPruefsumme()), senke)
    }

    private fun fehlerVon(antwort: Antwort): TransferFehler = when (antwort) {
        is Antwort.Fehlgeschlagen -> antwort.fehler
        else -> fail("erwartet war ein Fehler, bekommen: $antwort")
    }

    @Test
    fun vollstaendigeUebertragungLaeuftDurch() {
        val inhalt = ByteArray(300) { (it % 97).toByte() }
        val (empfaenger, senke) = neu().let { it.empfaenger to it.senke }

        assertTrue(empfaenger.rahmenEmpfangen(angebot(inhalt)) is Antwort.Frage)
        assertTrue(empfaenger.entscheide(true) is Antwort.Senden)
        assertEquals(Antwort.Nichts, empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt.copyOfRange(0, 100))))
        assertEquals(Antwort.Nichts, empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt.copyOfRange(100, 300))))

        val ende = empfaenger.rahmenEmpfangen(Rahmen.Fertig)
        assertIs<Antwort.Fertig>(ende, "erwartet war Fertig, bekommen: $ende")
        assertEquals(300L, ende.bytes)
        assertTrue(senke.bytes.toByteArray().contentEquals(inhalt))
        assertEquals(false, senke.verworfen)
        assertTrue(senke.abgeschlossen, "der Erfolgsabschluss der Senke lief nicht")
    }

    @Test
    fun ersterRahmenMussEinAngebotSein() {
        val a = neu()
        assertEquals(TransferFehler.RahmenAusserDerReihe, fehlerVon(a.empfaenger.rahmenEmpfangen(Rahmen.Fertig)))
    }

    @Test
    fun datenVorDerEntscheidungWerdenAbgelehnt() {
        val inhalt = ByteArray(10) { 1 }
        val a = neu()
        a.empfaenger.rahmenEmpfangen(angebot(inhalt))
        assertEquals(
            TransferFehler.RahmenAusserDerReihe,
            fehlerVon(a.empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt))),
        )
        assertEquals(0, a.senke.bytes.size)
    }

    @Test
    fun zweitesAngebotWirdAbgelehnt() {
        val inhalt = ByteArray(10) { 1 }
        val a = neu()
        a.empfaenger.rahmenEmpfangen(angebot(inhalt))
        a.empfaenger.entscheide(true)
        assertEquals(
            TransferFehler.RahmenAusserDerReihe,
            fehlerVon(a.empfaenger.rahmenEmpfangen(angebot(inhalt))),
        )
    }

    // Ein Byte mehr als angesagt darf die Senke nie erreichen: sonst waechst die
    // Datei auf dem Geraet weiter, waehrend die Ansage laengst gerissen ist.
    @Test
    fun zuVieleDatenBrechenSofortAb() {
        val inhalt = ByteArray(100) { 5 }
        val a = neu()
        a.empfaenger.rahmenEmpfangen(angebot(inhalt))
        a.empfaenger.entscheide(true)
        a.empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt))

        val antwort = a.empfaenger.rahmenEmpfangen(Rahmen.Daten(byteArrayOf(9)))
        assertEquals(TransferFehler.ZuVieleDaten, fehlerVon(antwort))
        assertTrue(a.senke.verworfen)
    }

    @Test
    fun einzelnerZuGrosserRahmenBrichtAb() {
        val inhalt = ByteArray(10) { 5 }
        val a = neu()
        a.empfaenger.rahmenEmpfangen(angebot(inhalt))
        a.empfaenger.entscheide(true)

        val zuViel = ByteArray(11) { 5 }
        assertEquals(TransferFehler.ZuVieleDaten, fehlerVon(a.empfaenger.rahmenEmpfangen(Rahmen.Daten(zuViel))))
        assertEquals(0, a.senke.bytes.size)
    }

    @Test
    fun zuWenigDatenWerdenErkannt() {
        val inhalt = ByteArray(100) { 5 }
        val a = neu()
        a.empfaenger.rahmenEmpfangen(angebot(inhalt))
        a.empfaenger.entscheide(true)
        a.empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt.copyOfRange(0, 60)))

        assertEquals(TransferFehler.ZuWenigDaten, fehlerVon(a.empfaenger.rahmenEmpfangen(Rahmen.Fertig)))
        assertTrue(a.senke.verworfen)
    }

    // Vollstaendig angekommen, aber anders als angesagt: das ist ein
    // Uebertragungsfehler und muss von "manipuliert" unterscheidbar bleiben.
    @Test
    fun falschePruefsummeWirdErkannt() {
        val inhalt = ByteArray(50) { 3 }
        val a = neu()
        val verdrehtesAngebot = Rahmen.Angebot(inhalt.size.toLong(), ByteArray(TransferFormat.HASH_SIZE) { 0x77 }, "p.czp")
        a.empfaenger.rahmenEmpfangen(verdrehtesAngebot)
        a.empfaenger.entscheide(true)
        a.empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt))

        assertEquals(TransferFehler.PruefsummeFalsch, fehlerVon(a.empfaenger.rahmenEmpfangen(Rahmen.Fertig)))
        assertTrue(a.senke.verworfen)
    }

    @Test
    fun ablehnungBeendetSauber() {
        val inhalt = ByteArray(10) { 1 }
        val a = neu()
        a.empfaenger.rahmenEmpfangen(angebot(inhalt))

        val antwort = a.empfaenger.entscheide(false, Ablehnungsgrund.KeinPlatz)
        assertIs<Antwort.Senden>(antwort)
        val rahmen = antwort.rahmen
        assertIs<Rahmen.Ablehnung>(rahmen)
        assertEquals(Ablehnungsgrund.KeinPlatz, rahmen.grund)
        assertEquals(EmpfangsZustand.Beendet, a.empfaenger.zustand)
    }

    @Test
    fun abbruchDesSendersBeendet() {
        val inhalt = ByteArray(10) { 1 }
        val a = neu()
        a.empfaenger.rahmenEmpfangen(angebot(inhalt))
        a.empfaenger.entscheide(true)

        val antwort = a.empfaenger.rahmenEmpfangen(Rahmen.Abbruch(Abbruchgrund.NutzerBricht))
        val abgebrochen = assertIs<Antwort.Abgebrochen>(antwort)
        assertEquals(Abbruchgrund.NutzerBricht, abgebrochen.grund, "der Grund der Gegenseite geht verloren")
        assertTrue(a.senke.verworfen)
        assertEquals(EmpfangsZustand.Beendet, a.empfaenger.zustand)
    }

    // Nach dem Ende darf nichts mehr laufen: ein weiterer Rahmen waere ein
    // zweiter Empfang auf derselben Verbindung, an dem niemand mehr vorbeisieht.
    @Test
    fun nachDemEndeGehtNichtsMehr() {
        val inhalt = ByteArray(10) { 1 }
        val a = neu()
        a.empfaenger.rahmenEmpfangen(angebot(inhalt))
        a.empfaenger.entscheide(true)
        a.empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt))
        assertTrue(a.empfaenger.rahmenEmpfangen(Rahmen.Fertig) is Antwort.Fertig)

        assertEquals(
            TransferFehler.RahmenAusserDerReihe,
            fehlerVon(a.empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt))),
        )
        assertEquals(
            TransferFehler.RahmenAusserDerReihe,
            fehlerVon(a.empfaenger.rahmenEmpfangen(Rahmen.Fertig)),
        )
    }

    @Test
    fun entscheidungOhneAngebotWirdAbgelehnt() {
        val a = neu()
        assertEquals(TransferFehler.RahmenAusserDerReihe, fehlerVon(a.empfaenger.entscheide(true)))
    }

    @Test
    fun zweiteEntscheidungWirdAbgelehnt() {
        val inhalt = ByteArray(10) { 1 }
        val a = neu()
        a.empfaenger.rahmenEmpfangen(angebot(inhalt))
        a.empfaenger.entscheide(true)
        assertEquals(TransferFehler.RahmenAusserDerReihe, fehlerVon(a.empfaenger.entscheide(true)))
    }

    // Die Pruefsumme muss genau die Bytes sehen, die in der Senke landen —
    // sonst pruefte sie etwas anderes, als gespeichert wurde.
    @Test
    fun pruefsummeSiehtGenauDieGespeichertenBytes() {
        val inhalt = ByteArray(250) { (it % 13).toByte() }
        val senke = SammelSenke()
        val summe = ZaehlPruefsumme()
        val empfaenger = Empfaenger(senke, summe)
        empfaenger.rahmenEmpfangen(angebot(inhalt))
        empfaenger.entscheide(true)
        empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt.copyOfRange(0, 130)))
        empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt.copyOfRange(130, 250)))
        assertTrue(empfaenger.rahmenEmpfangen(Rahmen.Fertig) is Antwort.Fertig)

        assertEquals(senke.bytes.size.toLong(), summe.gesehen)
        assertEquals(250L, summe.gesehen)
    }

    @Test
    fun fortschrittIstAblesbar() {
        val inhalt = ByteArray(100) { 1 }
        val a = neu()
        a.empfaenger.rahmenEmpfangen(angebot(inhalt))
        a.empfaenger.entscheide(true)
        assertEquals(0L, a.empfaenger.empfangeneBytes)
        a.empfaenger.rahmenEmpfangen(Rahmen.Daten(inhalt.copyOfRange(0, 40)))
        assertEquals(40L, a.empfaenger.empfangeneBytes)
        assertEquals(100L, a.empfaenger.angesagteBytes)
    }
}
