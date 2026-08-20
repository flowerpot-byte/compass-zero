package org.compasszero.transfer

import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Die Dateisenke hat genau eine Aufgabe: kein halbes Paket bleibt liegen. Dass
// sie dafuer bis zuletzt keinen eigenen Test hatte, war die groessere Luecke als
// jeder einzelne Fehler darin.
class DateiSenkeTest {

    private fun tempDir(): File = File.createTempFile("czsenke", null).let {
        it.delete(); it.mkdirs(); it
    }

    @Test
    fun dieDateiEntstehtErstBeimErstenSchreiben() {
        val ziel = File(tempDir(), "paket.czp")
        val senke = DateiSenke(ziel)
        assertTrue(!ziel.exists(), "die Datei wurde schon vor der Entscheidung angelegt")

        senke.schreibe(ByteArray(10), 0, 10)
        assertTrue(ziel.exists())
        senke.abschliessen()
        assertEquals(10L, ziel.length())
    }

    @Test
    fun verwerfenLoeschtDasHalbfertige() {
        val ziel = File(tempDir(), "paket.czp")
        val senke = DateiSenke(ziel)
        senke.schreibe(ByteArray(4096), 0, 4096)
        senke.verwirf()
        assertTrue(!ziel.exists(), "die halbe Datei blieb liegen")
    }

    @Test
    fun verwerfenIstMehrfachErlaubt() {
        val ziel = File(tempDir(), "paket.czp")
        val senke = DateiSenke(ziel)
        senke.schreibe(ByteArray(10), 0, 10)
        senke.verwirf()
        senke.verwirf()
        assertTrue(!ziel.exists())
    }

    @Test
    fun verwerfenOhneJedenSchreibvorgangGehtDurch() {
        val ziel = File(tempDir(), "paket.czp")
        DateiSenke(ziel).verwirf()
        assertTrue(!ziel.exists())
    }

    // Eine vertauschte Reihenfolge im Aufrufer darf nicht still erfolgreich
    // sein: sonst gilt eine geloeschte Datei als fertig abgelegt.
    @Test
    fun abschliessenNachDemVerwerfenScheitert() {
        val ziel = File(tempDir(), "paket.czp")
        val senke = DateiSenke(ziel)
        senke.schreibe(ByteArray(10), 0, 10)
        senke.verwirf()
        assertFailsWith<Exception> { senke.abschliessen() }
    }

    @Test
    fun schreibenNachDemAbschliessenScheitert() {
        val ziel = File(tempDir(), "paket.czp")
        val senke = DateiSenke(ziel)
        senke.schreibe(ByteArray(10), 0, 10)
        senke.abschliessen()
        assertFailsWith<Exception> { senke.schreibe(ByteArray(1), 0, 1) }
    }

    @Test
    fun alleBytesStehenNachDemAbschliessenAufDerPlatte() {
        val ziel = File(tempDir(), "paket.czp")
        val senke = DateiSenke(ziel)
        val inhalt = ByteArray(70_000) { (it % 251).toByte() }
        var offen = 0
        while (offen < inhalt.size) {
            val stueck = minOf(9_000, inhalt.size - offen)
            senke.schreibe(inhalt, offen, stueck)
            offen += stueck
        }
        senke.abschliessen()
        assertTrue(ziel.readBytes().contentEquals(inhalt), "die abgelegten Bytes weichen ab")
    }
}

// Der Rahmenleser ist die Stelle, an der die Groessengrenze wirkt, bevor
// irgendetwas belegt wird.
class RahmenLeserTest {

    private fun leserFuer(vararg bloecke: ByteArray): RahmenLeser {
        val alles = bloecke.fold(ByteArray(0)) { a, b -> a + b }
        return RahmenLeser(StromQuelle(ByteArrayInputStream(alles)))
    }

    @Test
    fun grussUndRahmenWerdenNacheinanderGelesen() {
        val leser = leserFuer(
            RahmenCodec.schreibeGruss(),
            RahmenCodec.schreibe(Rahmen.Daten(byteArrayOf(1, 2, 3))),
            RahmenCodec.schreibe(Rahmen.Fertig),
        )
        assertEquals(GrussErgebnis.Ok, leser.liesGruss())
        val ersteR = assertIs<LeseErgebnis.Ok>(leser.liesRahmen())
        assertTrue(assertIs<Rahmen.Daten>(ersteR.rahmen).bytes.contentEquals(byteArrayOf(1, 2, 3)))
        assertIs<Rahmen.Fertig>(assertIs<LeseErgebnis.Ok>(leser.liesRahmen()).rahmen)
        assertEquals(LeseErgebnis.Ende, leser.liesRahmen())
    }

    // Ein Laengenfeld von zwei Milliarden darf kein Feld von zwei Milliarden
    // Bytes anfordern. Auf einem Geraet mit 96 MB waere das der Absturz.
    @Test
    fun riesigesLaengenfeldWirdAbgewiesenStattBelegt() {
        val kopf = byteArrayOf(TransferFormat.TYP_DATEN.toByte(), 0x7F, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val leser = leserFuer(kopf)
        val fehler = assertIs<LeseErgebnis.Fehler>(leser.liesRahmen())
        assertEquals(TransferFehler.RahmenZuGross, fehler.fehler)
    }

    @Test
    fun negativesLaengenfeldWirdAbgewiesen() {
        val kopf = byteArrayOf(TransferFormat.TYP_DATEN.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val fehler = assertIs<LeseErgebnis.Fehler>(leserFuer(kopf).liesRahmen())
        assertEquals(TransferFehler.RahmenZuGross, fehler.fehler)
    }

    @Test
    fun abgeschnittenerRahmenIstEinFehlerKeinEnde() {
        val voll = RahmenCodec.schreibe(Rahmen.Daten(ByteArray(100) { 7 }))
        val fehler = assertIs<LeseErgebnis.Fehler>(leserFuer(voll.copyOfRange(0, voll.size - 10)).liesRahmen())
        assertEquals(TransferFehler.RahmenUnvollstaendig, fehler.fehler)
    }

    @Test
    fun abgeschnittenerKopfIstEinFehler() {
        val fehler = assertIs<LeseErgebnis.Fehler>(leserFuer(byteArrayOf(4, 0, 0)).liesRahmen())
        assertEquals(TransferFehler.RahmenUnvollstaendig, fehler.fehler)
    }

    @Test
    fun sauberesEndeZwischenZweiRahmenIstKeinFehler() {
        assertEquals(LeseErgebnis.Ende, leserFuer(ByteArray(0)).liesRahmen())
    }

    // Ein Strom liefert selten alles auf einmal; das ist bei Funk der Normalfall
    // und darf keinen Rahmen zerreissen.
    @Test
    fun haeppchenweiseZustellungZerreisstKeinenRahmen() {
        val bytes = RahmenCodec.schreibe(Rahmen.Daten(ByteArray(5_000) { (it % 97).toByte() }))
        val zaehe = object : Datenquelle {
            private var stelle = 0
            override fun lies(puffer: ByteArray, offset: Int, laenge: Int): Int {
                if (stelle >= bytes.size) return 0
                val menge = minOf(7, laenge, bytes.size - stelle)
                bytes.copyInto(puffer, offset, stelle, stelle + menge)
                stelle += menge
                return menge
            }
        }
        val rahmen = assertIs<LeseErgebnis.Ok>(RahmenLeser(zaehe).liesRahmen())
        assertEquals(5_000, assertIs<Rahmen.Daten>(rahmen.rahmen).bytes.size)
    }
}
