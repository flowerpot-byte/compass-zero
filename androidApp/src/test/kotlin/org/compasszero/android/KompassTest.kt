package org.compasszero.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Die Rechnung des Kompasses steht hier, nicht am Geraet. Am Emulator laesst
// sich zwar zeigen, dass vier eingespeiste Feldrichtungen sauber 90 Grad
// auseinander liegen -- der absolute Wert weicht dort aber um ein paar Grad ab,
// und einen Fehler im Nulldurchgang faengt ein Screenshot ohnehin nicht.
class KompassTest {

    @Test
    fun himmelsrichtungTrifftDieAchtStriche() {
        assertEquals("N", Kompass.himmelsrichtung(0f))
        assertEquals("O", Kompass.himmelsrichtung(90f))
        assertEquals("S", Kompass.himmelsrichtung(180f))
        assertEquals("W", Kompass.himmelsrichtung(270f))
        assertEquals("NO", Kompass.himmelsrichtung(45f))
        assertEquals("SW", Kompass.himmelsrichtung(225f))
    }

    // Der Fall, an dem eine Kompassanzeige typischerweise scheitert: knapp vor
    // und knapp hinter Norden. Wer hier "NW" statt "N" zeigt, schickt jemanden
    // im Gelaende in die falsche Richtung.
    @Test
    fun kurzVorUndNachNordenBleibtEsNorden() {
        assertEquals("N", Kompass.himmelsrichtung(359f))
        assertEquals("N", Kompass.himmelsrichtung(1f))
        assertEquals("N", Kompass.himmelsrichtung(337.5f + 0.1f))
        assertEquals("NW", Kompass.himmelsrichtung(330f))
    }

    @Test
    fun derAbstandLaeuftUeberDenNulldurchgang() {
        assertEquals(2f, Kompass.abstandGrad(359f, 1f))
        assertEquals(2f, Kompass.abstandGrad(1f, 359f))
        assertEquals(180f, Kompass.abstandGrad(0f, 180f))
        assertEquals(0f, Kompass.abstandGrad(90f, 90f))
    }

    // Der eigentliche Grund fuer die Glaettung ueber Sinus und Kosinus: Beim
    // Sprung von 350 auf 10 Grad muss der Zwischenwert bei oder um Null liegen.
    // Wuerde ueber den Gradwert gemittelt, landete er bei 180 -- die Nadel
    // schluege auf dem Weg von Nord nach Nord einmal nach Sueden aus.
    @Test
    fun dieGlaettungLaeuftNichtQuerDurchDenKreis() {
        val m = Kompass.Mittel()
        m.schritt(350f, 1f)
        val zwischen = m.schritt(10f, 0.5f)
        assertTrue(
            Kompass.abstandGrad(zwischen, 0f) < 1f,
            "der Zwischenwert liegt bei $zwischen statt bei etwa 0 — die Glättung " +
                "rechnet über den Gradwert statt über Sinus und Kosinus",
        )
    }

    @Test
    fun dieGlaettungNaehertSichDemZielAn() {
        val m = Kompass.Mittel()
        var grad = m.schritt(0f, 1f)
        repeat(40) { grad = m.schritt(90f, Kompass.GLAETTUNG) }
        assertTrue(
            Kompass.abstandGrad(grad, 90f) < 1f,
            "nach 40 Schritten steht die Glättung bei $grad statt nahe 90",
        )
    }

    // DER FEHLER, DER AM GERAET AUFFIEL und den kein bisheriger Test fing: Bei
    // einer exakten Kehrtwende um 180 Grad heben sich die Sinus-Anteile auf.
    // Wer den geglaetteten Zeiger zwischendurch auf Laenge eins zurueckrechnet,
    // bleibt fuer immer am Ausgangswert stehen -- der Kompass friert ein und
    // zeigt weiter Nord, waehrend man nach Sueden laeuft.
    @Test
    fun eineKehrtwendeUmGenauHundertachtzigGradKommtAn() {
        val m = Kompass.Mittel()
        var grad = m.schritt(0f, 1f)
        var schritte = 0
        while (Kompass.abstandGrad(grad, 180f) > 2f && schritte < 300) {
            grad = m.schritt(180f, Kompass.GLAETTUNG)
            schritte++
        }
        assertTrue(
            schritte < 300,
            "die Glättung steht bei $grad und kommt bei einer Kehrtwende nie an",
        )
        assertTrue(schritte <= 60, "die Kehrtwende braucht $schritte Messungen")
    }

    // Dieselbe Falle eine Spur daneben: knapp nicht 180 Grad. Hier rettet die
    // Rechnung sich von selbst, der Fall gehoert aber daneben, damit klar
    // bleibt, dass es um den exakten Gegenwert ging.
    @Test
    fun auchEineFastKehrtwendeKommtAn() {
        val m = Kompass.Mittel()
        var grad = m.schritt(0f, 1f)
        repeat(120) { grad = m.schritt(179f, Kompass.GLAETTUNG) }
        assertTrue(Kompass.abstandGrad(grad, 179f) < 2f, "steht bei $grad statt bei 179")
    }

    // Wie viele Messungen es braucht, bis der Wert steht. Auf einem echten
    // Geraet liefert der Sensor rund 16 Messungen je Sekunde -- die Zahl hier
    // ist also ungefaehr die Zeit in Sechzehnteln einer Sekunde. Steigt sie
    // unbemerkt an, wird der Kompass traege, ohne dass ein Test rot wird.
    @Test
    fun dieGlaettungStehtNachHoechstensDreissigMessungen() {
        val m = Kompass.Mittel()
        var grad = m.schritt(0f, 1f)
        var schritte = 0
        while (Kompass.abstandGrad(grad, 90f) > 2f && schritte < 200) {
            grad = m.schritt(90f, Kompass.GLAETTUNG)
            schritte++
        }
        assertTrue(schritte in 1..30, "die Glättung braucht $schritte Messungen bis auf 2 Grad heran")
    }

    @Test
    fun zuSchraegWirdErkannt() {
        assertTrue(Kompass.istFlach(0.0, 0.0))
        assertTrue(Kompass.istFlach(20.0, -20.0))
        assertTrue(!Kompass.istFlach(0.0, 40.0), "40 Grad Kippung gilt noch als flach")
        assertTrue(!Kompass.istFlach(-90.0, 0.0), "aufrecht gehaltenes Gerät gilt als flach")
    }
}
