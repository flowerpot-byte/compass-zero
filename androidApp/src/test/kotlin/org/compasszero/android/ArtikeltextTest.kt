package org.compasszero.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Der Artikelsatz liest die Gliederung aus dem Text, statt sie im Text zu
// verlangen. Diese Pruefung haelt fest, WAS er dabei als Gliederung erkennt --
// sonst faellt eine Aenderung an der Regel erst am Geraet auf, und dort sieht
// man sie nur, wenn man den richtigen Tipp aufschlaegt.
class ArtikeltextTest {

    @Test
    fun einGrossgeschriebenerAnfangWirdBisZumLetztenSatzzeichenHervorgehoben() {
        // Bis zum Doppelpunkt, nicht bis zum Komma davor.
        val zeile = "EINORDNUNG DIESES PAKETS, NICHT AUS DER QUELLE: Die Leitlinie spricht."
        val laenge = Artikeltext.kickerLaenge(zeile)
        assertEquals("EINORDNUNG DIESES PAKETS, NICHT AUS DER QUELLE:", zeile.take(laenge))
    }

    @Test
    fun ohneDoppelpunktEndetDieHervorhebungAmKomma() {
        val zeile = "SO WIRD SIE ANGEWENDET, und die Reihenfolge ist der ganze Punkt:"
        val laenge = Artikeltext.kickerLaenge(zeile)
        assertEquals("SO WIRD SIE ANGEWENDET,", zeile.take(laenge))
    }

    @Test
    fun gewoehnlicherSatzBekommtKeineHervorhebung() {
        assertEquals(0, Artikeltext.kickerLaenge("Zuerst der Satz, der über allem steht."))
        // Ein einzelnes betontes Wort ist keine Ueberschrift -- sonst waere die
        // halbe Sammlung fett, denn Betonung in Grossbuchstaben ist die einzige
        // Auszeichnung, die es im Paket gibt.
        assertEquals(0, Artikeltext.kickerLaenge("NICHT zudecken, sondern offen lassen."))
    }

    @Test
    fun eineGanzGrosseZeileGiltAlsZwischenueberschrift() {
        val zeilen = Artikeltext.zerlege("WAS DARAUS FOLGT\n\nDer Abstieg wirkt immer.")
        assertEquals(Artikeltext.Art.UEBERSCHRIFT, zeilen[0].art)
        assertEquals(Artikeltext.Art.FLIESSTEXT, zeilen[1].art)
    }

    @Test
    fun aufzaehlungenUndAblaeufeBekommenEinenEinzug() {
        val zeilen = Artikeltext.zerlege("— Erstens etwas\n— Zweitens etwas\n\n1. Zuerst dies\n2. Dann das")
        assertTrue(zeilen.all { it.art == Artikeltext.Art.AUFZAEHLUNG })
        assertTrue(zeilen.all { it.einzug > 0 })
        // Eine Ziffer am Zeilenanfang darf nicht als Hervorhebung durchgehen.
        assertTrue(zeilen.all { it.kicker == 0 })
    }

    @Test
    fun nurDieErsteZeileEinesAbsatzesTraegtDieHervorhebung() {
        val zeilen = Artikeltext.zerlege("ERSTE ZEILE MIT KICKER: und Text.\nZWEITE ZEILE MIT KICKER: und Text.")
        assertTrue(zeilen[0].kicker > 0)
        assertEquals(0, zeilen[1].kicker)
    }

    @Test
    fun absatzendenWerdenErkannt() {
        val zeilen = Artikeltext.zerlege("Eins\nZwei\n\nDrei")
        assertEquals(listOf(false, true, true), zeilen.map { it.absatzende })
    }

    @Test
    fun leerzeilenUndUeberfluessigeLeerzeichenFallenWeg() {
        val zeilen = Artikeltext.zerlege("  Eins  \n\n   \n\n  Zwei  ")
        assertEquals(listOf("Eins", "Zwei"), zeilen.map { it.text })
    }

    // Gegenprobe am echten Satzbau: Ein sehr langer Grossbuchstaben-Satz wird
    // NICHT zum fetten Block. Sonst waere die Hervorhebung schlimmer als keine.
    @Test
    fun einSehrLangerGrossbuchstabenSatzBleibtUnhervorgehoben() {
        val lang = "DIES IST EIN SEHR LANGER SATZ IN GROSSBUCHSTABEN DER DEUTLICH " +
            "UEBER HUNDERTZWANZIG ZEICHEN LANG IST UND DESHALB NICHT ALS BLOCK GESETZT WIRD."
        assertTrue(lang.length > 120)
        assertEquals(0, Artikeltext.kickerLaenge(lang))
    }
    // Ein langer Vorspann mit Doppelpunkt wird zur eigenen Zeile; ein kurzer
    // bleibt im Satz. Fett ueber zwei Zeilen mitten im Absatz ist ein Klotz.
    @Test
    fun einLangerVorspannWirdZurEigenenZeile() {
        val zeilen = Artikeltext.zerlege(
            "EIN VERBREITETES ÜBERLEBENSHANDBUCH SAGT ETWAS ANDERES: Es nennt Ruhe und Sauerstoff.",
        )
        assertEquals(2, zeilen.size)
        assertEquals(Artikeltext.Art.UEBERSCHRIFT, zeilen[0].art)
        assertEquals("EIN VERBREITETES ÜBERLEBENSHANDBUCH SAGT ETWAS ANDERES:", zeilen[0].text)
        assertEquals("Es nennt Ruhe und Sauerstoff.", zeilen[1].text)
        assertTrue(zeilen[1].absatzende)
    }

    @Test
    fun einKurzerVorspannBleibtImSatz() {
        val zeilen = Artikeltext.zerlege("SO GEHT ES: erst dies, dann das.")
        assertEquals(1, zeilen.size)
        assertEquals(Artikeltext.Art.FLIESSTEXT, zeilen[0].art)
        assertTrue(zeilen[0].kicker > 0)
    }
}
