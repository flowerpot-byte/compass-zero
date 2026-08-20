package org.compasszero.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Die Sinnbilder stehen an vier Stellen: in der Liste ALLE (Reihenfolge der
// Auswahl), in NAMEN (Beschriftung), in der Pruefung gueltig() und in der
// Zeichnung maleWegpunktzeichen. Laufen die auseinander, faellt es niemandem
// auf -- die Auswahl zeigt dann einen Knopf ohne Namen, oder die Karte einen
// leeren Ring.
//
// Die Zeichnung selbst laesst sich hier nicht pruefen (sie braucht ein
// Canvas); sie hat deshalb einen Rueckfall auf das Fadenkreuz. Was dieser
// Test halten kann, ist die Verabredung dahinter.
class SinnbildTest {

    @Test
    fun jedesSinnbildAusDerListeHatEinenNamen() {
        val ohneNamen = Sinnbild.ALLE.filter { Sinnbild.NAMEN[it] == null }
        assertTrue(
            ohneNamen.isEmpty(),
            "Diese Sinnbilder stehen in ALLE, haben aber keinen Namen: $ohneNamen — " +
                "die Auswahl zeigte dafuer einen leeren Knopf.",
        )
    }

    @Test
    fun esGibtKeinenNamenOhneSinnbild() {
        val ohneEintrag = Sinnbild.NAMEN.keys.filter { it !in Sinnbild.ALLE }
        assertTrue(
            ohneEintrag.isEmpty(),
            "Diese Namen gehoeren zu keinem Sinnbild aus ALLE: $ohneEintrag",
        )
    }

    @Test
    fun gueltigLaesstGenauDieBekanntenDurch() {
        for (wert in Sinnbild.ALLE) {
            assertEquals(wert, Sinnbild.gueltig(wert), "$wert muesste gueltig sein")
        }
    }

    @Test
    fun allesUnbekannteWirdZuKeins() {
        // Die Datei mit den eigenen Punkten ist die einzige unsignierte im
        // Programm. Was von dort kommt, darf nichts Neues erfinden.
        for (unfug in listOf("rakete", "WASSER", " wasser", "wasser ", "<script>", "")) {
            if (unfug == Sinnbild.KEINS) continue
            assertEquals(
                Sinnbild.KEINS,
                Sinnbild.gueltig(unfug),
                "\"$unfug\" haette zu KEINS werden muessen",
            )
        }
    }

    @Test
    fun keinsStehtVorneInDerAuswahlUndHintenInDerListe() {
        // Zwei verschiedene Reihenfolgen mit Absicht: In der Auswahl ist "ohne"
        // die erste Moeglichkeit, in der Wegpunktliste stehen die Punkte ohne
        // Sinnbild hinten. Beides ist anderswo festgeschrieben; hier wird nur
        // die Voraussetzung gehalten, dass KEINS ueberhaupt in ALLE steht.
        assertEquals(Sinnbild.KEINS, Sinnbild.ALLE.first())
        assertEquals(Sinnbild.ALLE.size, Sinnbild.ALLE.distinct().size, "doppelte Eintraege in ALLE")
    }
}
