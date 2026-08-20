package org.compasszero.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Der Sparmodus ist kein abgedunkelter Normalmodus, sondern eine eigene
// Darstellung. Was hier steht, sind die Zusagen, die man ihm ansehen koennen
// muss, ohne die App zu starten -- und die beim naechsten Umbau der Oberflaeche
// still verlorengehen koennten.
class StilTest {

    private val dichte = 2.75f

    // Die Bewegungsdauer kommt aus genau einer Stelle. Steht dort null, faellt
    // jede Bewegung aus: Bausteine.antippbewegung entfernt den Animator, und
    // Bausteine.aufgehen kehrt sofort zurueck. Waere hier eine kleine Dauer
    // statt null, liefe im Ernstfall weiter eine Bildwiederholung fuer Zierde.
    @Test
    fun imSparmodusGibtEsKeineBewegung() {
        assertEquals(0L, Stil.sparmodus(dichte).bewegungMs)
    }

    @Test
    fun imNormalmodusGibtEsBewegungUndSieBleibtKurz() {
        val ms = Stil.normal(dichte).bewegungMs
        assertTrue(ms > 0, "im Normalmodus soll es eine Rueckmeldung geben")
        // Alles darueber fuehlt sich nicht mehr wie eine Rueckmeldung an,
        // sondern wie eine Verzoegerung -- und auf einem alten Geraet wartet
        // der Nutzer dann sichtbar auf die Oberflaeche.
        assertTrue(ms <= 200, "eine Bewegung ueber 200 ms bremst die Bedienung: $ms")
    }

    // Farben lassen sich hier nicht pruefen: android.graphics.Color ist im
    // reinen JVM-Test eine Attrappe und liefert fuer jeden Ton dieselbe Null.
    // Dass sich Flaeche und Raster im Normalmodus vom Blatt abheben und im
    // Sparmodus nicht, ist deshalb am Geraet zu sehen und nicht hier.

    // Im Sparmodus ist alles groesser: gelesen wird unter Stress, oft mit
    // klammen Haenden und schlechtem Licht.
    @Test
    fun imSparmodusIstAllesGroesser() {
        val normal = Stil.normal(dichte)
        val spar = Stil.sparmodus(dichte)
        assertTrue(spar.textGroesse > normal.textGroesse)
        assertTrue(spar.listenGroesse > normal.listenGroesse)
        assertTrue(spar.titelGroesse > normal.titelGroesse)
        assertTrue(spar.abstand > normal.abstand)
    }
}
