package org.compasszero.transfer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class RahmenCodecTest {

    private fun hash(fuellung: Byte = 7): ByteArray = ByteArray(TransferFormat.HASH_SIZE) { fuellung }

    private fun gelesen(rahmen: Rahmen): Rahmen {
        val bytes = RahmenCodec.schreibe(rahmen)
        assertEquals(TransferFormat.KOPF_SIZE + nutzlastLaenge(bytes), bytes.size)
        return when (val ergebnis = RahmenCodec.lies(bytes[0].toInt() and 0xFF, nutzlast(bytes))) {
            is RahmenErgebnis.Ok -> ergebnis.rahmen
            is RahmenErgebnis.Fehler -> fail("unerwarteter Fehler: ${ergebnis.fehler}")
        }
    }

    private fun nutzlastLaenge(bytes: ByteArray): Int =
        ((bytes[1].toInt() and 0xFF) shl 24) or ((bytes[2].toInt() and 0xFF) shl 16) or
            ((bytes[3].toInt() and 0xFF) shl 8) or (bytes[4].toInt() and 0xFF)

    private fun nutzlast(bytes: ByteArray): ByteArray = bytes.copyOfRange(TransferFormat.KOPF_SIZE, bytes.size)

    private fun fehlerBeim(typ: Int, nutzlast: ByteArray): TransferFehler =
        when (val ergebnis = RahmenCodec.lies(typ, nutzlast)) {
            is RahmenErgebnis.Ok -> fail("Rahmen wurde angenommen, erwartet war eine Ablehnung")
            is RahmenErgebnis.Fehler -> ergebnis.fehler
        }

    private fun angebotNutzlast(
        groesse: Long = 4096,
        pruefsumme: ByteArray = hash(),
        name: ByteArray = "paket.czp".encodeToByteArray(),
        angesagteNamensLaenge: Int = name.size,
    ): ByteArray {
        val out = ByteArray(8 + TransferFormat.HASH_SIZE + 2 + name.size)
        for (i in 0 until 8) out[i] = (groesse ushr ((7 - i) * 8)).toByte()
        pruefsumme.copyInto(out, 8)
        out[8 + TransferFormat.HASH_SIZE] = (angesagteNamensLaenge ushr 8).toByte()
        out[9 + TransferFormat.HASH_SIZE] = angesagteNamensLaenge.toByte()
        name.copyInto(out, 10 + TransferFormat.HASH_SIZE)
        return out
    }

    @Test
    fun angebotUeberlebtHinUndRueckweg() {
        val rahmen = gelesen(Rahmen.Angebot(123_456, hash(3), "Basispaket Deutsch"))
        val angebot = rahmen as Rahmen.Angebot
        assertEquals(123_456, angebot.groesse)
        assertEquals("Basispaket Deutsch", angebot.name)
        assertTrue(angebot.pruefsumme.contentEquals(hash(3)))
    }

    @Test
    fun leereRahmenUeberlebenHinUndRueckweg() {
        assertTrue(gelesen(Rahmen.Annahme) is Rahmen.Annahme)
        assertTrue(gelesen(Rahmen.Fertig) is Rahmen.Fertig)
    }

    @Test
    fun gruendeUeberlebenHinUndRueckweg() {
        for (grund in Ablehnungsgrund.entries) {
            assertEquals(grund, (gelesen(Rahmen.Ablehnung(grund)) as Rahmen.Ablehnung).grund)
        }
        for (grund in Abbruchgrund.entries) {
            assertEquals(grund, (gelesen(Rahmen.Abbruch(grund)) as Rahmen.Abbruch).grund)
        }
    }

    @Test
    fun datenUeberlebenHinUndRueckweg() {
        val inhalt = ByteArray(1000) { (it % 251).toByte() }
        val daten = gelesen(Rahmen.Daten(inhalt)) as Rahmen.Daten
        assertTrue(daten.bytes.contentEquals(inhalt))
    }

    @Test
    fun grussWirdErkannt() {
        val gruss = RahmenCodec.schreibeGruss()
        assertEquals(TransferFormat.GRUSS_SIZE, gruss.size)
        assertEquals(GrussErgebnis.Ok, RahmenCodec.liesGruss(gruss))
    }

    @Test
    fun fremdesMagicWirdAbgelehnt() {
        val gruss = RahmenCodec.schreibeGruss()
        gruss[1] = 0x00
        assertEquals(GrussErgebnis.Fehler(TransferFehler.GrussUnbekannt), RahmenCodec.liesGruss(gruss))
    }

    // Eine fremde Protokollversion wird benannt, nicht geraten: die Oberflaeche
    // soll sagen koennen, welche Fassung das Gegenueber spricht.
    @Test
    fun fremdeVersionWirdBenannt() {
        val gruss = RahmenCodec.schreibeGruss()
        gruss[5] = 9
        assertEquals(GrussErgebnis.FremdeVersion(9), RahmenCodec.liesGruss(gruss))
    }

    @Test
    fun zuKurzerGrussWirdAbgelehnt() {
        assertEquals(
            GrussErgebnis.Fehler(TransferFehler.GrussUnvollstaendig),
            RahmenCodec.liesGruss(ByteArray(TransferFormat.GRUSS_SIZE - 1)),
        )
    }

    @Test
    fun unbekannterRahmentypWirdAbgelehnt() {
        assertEquals(TransferFehler.RahmenUnbekannt, fehlerBeim(0x42, ByteArray(0)))
    }

    // Leere Rahmen duerfen keine Nutzlast tragen. Ein Feld, das niemand ansieht,
    // ist ein Kanal fuer beliebige Daten unter dem Deckmantel der Uebertragung.
    @Test
    fun leereRahmenDuerfenKeineNutzlastTragen() {
        assertEquals(TransferFehler.RahmenLaengeFalsch, fehlerBeim(TransferFormat.TYP_ANNAHME, byteArrayOf(0)))
        assertEquals(TransferFehler.RahmenLaengeFalsch, fehlerBeim(TransferFormat.TYP_FERTIG, byteArrayOf(0)))
    }

    @Test
    fun grundRahmenBrauchenGenauEinByte() {
        assertEquals(TransferFehler.RahmenLaengeFalsch, fehlerBeim(TransferFormat.TYP_ABLEHNUNG, ByteArray(0)))
        assertEquals(TransferFehler.RahmenLaengeFalsch, fehlerBeim(TransferFormat.TYP_ABLEHNUNG, ByteArray(2)))
        assertEquals(TransferFehler.RahmenLaengeFalsch, fehlerBeim(TransferFormat.TYP_ABBRUCH, ByteArray(2)))
    }

    @Test
    fun unbekannterGrundWirdAbgelehnt() {
        assertEquals(TransferFehler.GrundUnbekannt, fehlerBeim(TransferFormat.TYP_ABLEHNUNG, byteArrayOf(99)))
        assertEquals(TransferFehler.GrundUnbekannt, fehlerBeim(TransferFormat.TYP_ABBRUCH, byteArrayOf(0)))
    }

    // Ein Datenrahmen ohne Inhalt bringt die Uebertragung nicht voran. Wer beliebig
    // viele davon schickt, haelt das Geraet endlos beschaeftigt, ohne je gegen die
    // angesagte Groesse zu laufen.
    @Test
    fun leererDatenrahmenWirdAbgelehnt() {
        assertEquals(TransferFehler.RahmenLaengeFalsch, fehlerBeim(TransferFormat.TYP_DATEN, ByteArray(0)))
    }

    @Test
    fun angebotMitFalscherLaengeWirdAbgelehnt() {
        assertEquals(TransferFehler.AngebotUnvollstaendig, fehlerBeim(TransferFormat.TYP_ANGEBOT, ByteArray(20)))
        // Ein Byte mehr als der angesagte Name: genau die Nische, in der sich
        // unbeachtete Daten mitschicken liessen.
        val zuLang = angebotNutzlast() + byteArrayOf(0)
        assertEquals(TransferFehler.AngebotUnvollstaendig, fehlerBeim(TransferFormat.TYP_ANGEBOT, zuLang))
    }

    @Test
    fun angebotMitZuLangemNamenWirdAbgelehnt() {
        val name = ByteArray(TransferFormat.MAX_NAME_BYTES + 1) { 'a'.code.toByte() }
        assertEquals(TransferFehler.NameZuLang, fehlerBeim(TransferFormat.TYP_ANGEBOT, angebotNutzlast(name = name)))
    }

    // Der Name ist Anzeige, kein Datenkanal. Ein unlesbarer Name wird ersetzt und
    // als ersetzt gekennzeichnet -- daran darf keine Uebertragung scheitern.
    @Test
    fun angebotMitKaputtemNamenBekommtEinenPlatzhalter() {
        // Alleinstehendes Fortsetzungsbyte: kein gueltiges UTF-8.
        val name = byteArrayOf(0x80.toByte(), 0x41, 0x42)
        val ergebnis = RahmenCodec.lies(TransferFormat.TYP_ANGEBOT, angebotNutzlast(name = name))
        val ok = assertIs<RahmenErgebnis.Ok>(ergebnis)
        val angebot = assertIs<Rahmen.Angebot>(ok.rahmen)
        assertTrue(angebot.nameErsetzt, "der Platzhalter ist nicht als solcher gekennzeichnet")
        assertEquals(TransferFormat.ERSATZNAME, angebot.name)
    }

    // Unsichtbare Zeichen im Namen wuerden in der Oberflaeche einen anderen Text
    // vortaeuschen, als wirklich uebertragen wurde.
    @Test
    fun angebotMitUnsichtbaremNamenBekommtEinenPlatzhalter() {
        // Drei Nullbreiten-Leerzeichen, bewusst als Fluchtfolge geschrieben:
        // im Quelltext waeren sie nicht zu sehen.
        val name = "​​​".encodeToByteArray()
        val ergebnis = RahmenCodec.lies(TransferFormat.TYP_ANGEBOT, angebotNutzlast(name = name))
        val ok = assertIs<RahmenErgebnis.Ok>(ergebnis)
        assertTrue(assertIs<Rahmen.Angebot>(ok.rahmen).nameErsetzt)
    }

    @Test
    fun angebotOhneInhaltWirdAbgelehnt() {
        assertEquals(TransferFehler.GroesseUnmoeglich, fehlerBeim(TransferFormat.TYP_ANGEBOT, angebotNutzlast(groesse = 0)))
    }

    @Test
    fun zuGrossesAngebotWirdAbgelehnt() {
        val zuGross = TransferFormat.MAX_PAKET_BYTES + 1
        assertEquals(TransferFehler.GroesseUnmoeglich, fehlerBeim(TransferFormat.TYP_ANGEBOT, angebotNutzlast(groesse = zuGross)))
    }

    // Ein gesetztes oberstes Bit ergaebe als Long eine negative Groesse. Ohne
    // eigene Pruefung liefe daraus eine Schleife, die nie endet.
    @Test
    fun angebotMitNegativerGroesseWirdAbgelehnt() {
        assertEquals(TransferFehler.GroesseUnmoeglich, fehlerBeim(TransferFormat.TYP_ANGEBOT, angebotNutzlast(groesse = -1)))
    }

    @Test
    fun angebotMitFalscherNamensLaengeWirdAbgelehnt() {
        val nutzlast = angebotNutzlast(angesagteNamensLaenge = 3)
        assertEquals(TransferFehler.AngebotUnvollstaendig, fehlerBeim(TransferFormat.TYP_ANGEBOT, nutzlast))
    }

    @Test
    fun zuGrosseRahmenWerdenVorDemLesenAbgelehnt() {
        assertEquals(
            TransferFehler.RahmenZuGross,
            RahmenCodec.pruefeNutzlastLaenge(TransferFormat.MAX_RAHMEN_NUTZLAST + 1),
        )
        assertEquals(null, RahmenCodec.pruefeNutzlastLaenge(TransferFormat.MAX_RAHMEN_NUTZLAST))
        // Ein negativer Wert entsteht aus einem Laengenfeld mit gesetztem obersten Bit.
        assertEquals(TransferFehler.RahmenZuGross, RahmenCodec.pruefeNutzlastLaenge(-1))
    }

    @Test
    fun datenrahmenAnDerObergrenzeGehenDurch() {
        val inhalt = ByteArray(TransferFormat.MAX_RAHMEN_NUTZLAST) { 1 }
        val daten = gelesen(Rahmen.Daten(inhalt)) as Rahmen.Daten
        assertEquals(TransferFormat.MAX_RAHMEN_NUTZLAST, daten.bytes.size)
    }
}
