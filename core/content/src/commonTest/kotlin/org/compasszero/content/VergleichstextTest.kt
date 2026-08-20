package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Der Satzkatalog wird nicht ueber das Suchverzeichnis gefunden -- Phrasen
// stehen dort bewusst nicht drin, weil sie Notfall-Tipps verdraengen wuerden.
// Er bekommt stattdessen sein eigenes Filterfeld, und das muss dieselbe
// Aufbereitung benutzen wie die Suche, sonst findet dieselbe Eingabe an zwei
// Stellen Verschiedenes.
class VergleichstextTest {

    @Test
    fun grossUndKleinschreibungSpieltKeineRolle() {
        assertTrue(Vergleichstext.enthaelt("Ich habe Schmerzen", "schmerzen"))
        assertTrue(Vergleichstext.enthaelt("ich habe schmerzen", "SCHMERZEN"))
    }

    // Umlaute werden auf ihre ausgeschriebene Form gefaltet, genau wie im
    // Suchverzeichnis: ue, oe, ae, ss. Wer auf dem Handy keinen Umlaut tippen
    // will, schreibt ihn aus und findet trotzdem.
    @Test
    fun umlauteWerdenGefaltet() {
        assertTrue(Vergleichstext.enthaelt("Mir ist übel", "uebel"))
        assertTrue(Vergleichstext.enthaelt("Mir ist übel", "übel"))
        assertTrue(Vergleichstext.enthaelt("Straße", "strasse"))
        assertTrue(Vergleichstext.enthaelt("Ich bin schwanger", "SCHWANGER"))
    }

    // GRENZE, ausdruecklich festgehalten: Der blosse Grundbuchstabe findet den
    // Umlaut NICHT -- "ubel" findet "übel" nicht, weil aus dem Umlaut "ue"
    // wird. Das ist dieselbe Regel wie im Suchverzeichnis, und sie steht hier,
    // damit sie niemand aus Versehen einzeln fuer den Satzkatalog aendert:
    // Zwei verschiedene Aufbereitungen fuer dieselbe Eingabe waeren schlimmer
    // als diese Grenze.
    @Test
    fun derBlosseGrundbuchstabeFindetDenUmlautNicht() {
        assertFalse(Vergleichstext.enthaelt("Mir ist übel", "ubel"))
    }

    // Mitten im Satz, nicht nur am Wortanfang: In einem Satzkatalog sucht man
    // nach dem Wort, das man im Kopf hat, nicht nach dem Satzanfang.
    @Test
    fun auchMittenImSatz() {
        assertTrue(Vergleichstext.enthaelt("Ich bin allergisch gegen Penicillin", "penicillin"))
        assertTrue(Vergleichstext.enthaelt("Ich bin allergisch gegen Penicillin", "allergisch"))
    }

    @Test
    fun leereAnfragePasstImmer() {
        assertTrue(Vergleichstext.enthaelt("beliebig", ""))
    }

    @Test
    fun wasNichtDrinStehtPasstNicht() {
        assertFalse(Vergleichstext.enthaelt("Ich habe Schmerzen", "durst"))
    }

    // Fremdsprachige Saetze sind ebenfalls durchsuchbar, solange man schreibt,
    // was dasteht. Akzente werden NICHT abgestreift: Eine allgemeine
    // Umschrifttabelle fuer lateinische Schrift wuerde kyrillische,
    // griechische und ostasiatische Pakete unsuchbar machen -- diese
    // Entscheidung steht im Suchverzeichnis und gilt hier genauso.
    @Test
    fun auchFremdeSprachenSindDurchsuchbar() {
        assertTrue(Vergleichstext.enthaelt("Gdzie boli?", "boli"))
        assertTrue(Vergleichstext.enthaelt("¿Dónde le duele?", "duele"))
        assertTrue(Vergleichstext.enthaelt("¿Dónde le duele?", "dónde"))
        assertFalse(Vergleichstext.enthaelt("¿Dónde le duele?", "donde"))
    }
}
