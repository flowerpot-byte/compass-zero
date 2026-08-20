package org.compasszero.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Die Lupe entscheidet drei Dinge: wie gross ein Bild aufgeht, ob es gedreht
// aufgeht und wie weit man es schieben darf. Alle drei sind Rechnung und
// deshalb hier pruefbar -- am Geraet faellt ein Fehler darin erst auf, wenn
// jemand das eine Bild aufschlaegt, bei dem er sich auswirkt.
class BildlupeTest {

    // Die tatsaechlichen Masse: eine Skizze des Pakets auf einem gewoehnlichen
    // Telefon, gemessen am 04.08.2026 am Emulator.
    private val skizzeBreit = 900
    private val skizzeHoch = 650
    private val schirmBreit = 1080
    private val schirmHoch = 2160

    @Test
    fun dieEinpassungNimmtDieEngereSeite() {
        // Breite erlaubt 1,2 -- Hoehe erlaubt viel mehr. Die Breite gewinnt.
        val m = Bildlupe.einpassung(skizzeBreit, skizzeHoch, schirmBreit, schirmHoch)
        assertEquals(1080f / 900f, m, 0.0001f)
    }

    @Test
    fun eineQuerformatigeSkizzeWirdGedrehtDeutlichGroesser() {
        assertTrue(Bildlupe.drehenLohnt(skizzeBreit, skizzeHoch, schirmBreit, schirmHoch))
        val aufrecht = Bildlupe.einpassung(skizzeBreit, skizzeHoch, schirmBreit, schirmHoch)
        val gedreht = Bildlupe.einpassung(skizzeHoch, skizzeBreit, schirmBreit, schirmHoch)
        // Der Zugewinn ist gut ein Drittel und NICHT das Doppelte: Begrenzt
        // wird in beiden Lagen die Breite, nicht die Hoehe. Diese Pruefung
        // steht hier, weil genau diese Verwechslung beim Bauen passiert ist.
        assertEquals(1080f / 650f, gedreht, 0.0001f)
        assertEquals(1080f / 900f, aufrecht, 0.0001f)
        assertTrue(gedreht > aufrecht * 1.35f, "gedreht $gedreht gegen aufrecht $aufrecht")
    }

    // Gegenprobe: Was hochkant ist, darf NICHT gedreht werden. Sonst legt
    // jemand den Kopf schief, um ein Bild zu sehen, das dadurch kleiner wird.
    @Test
    fun einHochformatigesBildWirdNichtGedreht() {
        assertFalse(Bildlupe.drehenLohnt(650, 900, schirmBreit, schirmHoch))
    }

    // Gegenprobe: Auf einem breiten Schirm (Tablet quer) bringt die Drehung
    // einer Querzeichnung nichts -- sie passt schon.
    @Test
    fun aufBreitemSchirmWirdEineQuerzeichnungNichtGedreht() {
        assertFalse(Bildlupe.drehenLohnt(skizzeBreit, skizzeHoch, 2160, 1080))
    }

    @Test
    fun kleinerAlsGanzSichtbarGehtNicht() {
        assertEquals(2f, Bildlupe.begrenzeMassstab(0.3f, 2f), 0.0001f)
        assertEquals(2f, Bildlupe.begrenzeMassstab(2f, 2f), 0.0001f)
    }

    @Test
    fun ueberDasAchtfacheHinausGehtNicht() {
        assertEquals(16f, Bildlupe.begrenzeMassstab(99f, 2f), 0.0001f)
    }

    @Test
    fun einInhaltKleinerAlsDieFlaecheStehtMittig() {
        // Egal, wohin gewischt wurde: 400 breit in 1000 -> immer 300.
        assertEquals(300f, Bildlupe.begrenzeVerschiebung(-800f, 400f, 1000f), 0.0001f)
        assertEquals(300f, Bildlupe.begrenzeVerschiebung(500f, 400f, 1000f), 0.0001f)
    }

    @Test
    fun einGrossererInhaltLaesstKeinenRandInsLeereLaufen() {
        // 3000 breit in 1000: von 0 (linke Kante) bis -2000 (rechte Kante).
        assertEquals(0f, Bildlupe.begrenzeVerschiebung(250f, 3000f, 1000f), 0.0001f)
        assertEquals(-2000f, Bildlupe.begrenzeVerschiebung(-9000f, 3000f, 1000f), 0.0001f)
        assertEquals(-742f, Bildlupe.begrenzeVerschiebung(-742f, 3000f, 1000f), 0.0001f)
    }

    // Ein Bild ohne Masse darf die Ansicht nicht mit einer Division durch null
    // umbringen -- lieber unveraendert anzeigen als abstuerzen.
    @Test
    fun einLeeresBildErgibtDenMassstabEins() {
        assertEquals(1f, Bildlupe.einpassung(0, 0, schirmBreit, schirmHoch), 0.0001f)
        assertEquals(1f, Bildlupe.einpassung(skizzeBreit, skizzeHoch, 0, 0), 0.0001f)
    }
}
