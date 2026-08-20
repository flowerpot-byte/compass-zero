package org.compasszero.android

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Die Laenderliste.
 *
 * Sie steht als Quelltext da und wird von Hand gepflegt -- genau deshalb
 * braucht sie Pruefungen: Ein Tippfehler in einer Koordinate faellt sonst erst
 * auf, wenn jemand im Ernstfall im falschen Land landet.
 */
class LaenderTest {

    @Test
    fun einLandWirdGefunden() {
        val land = assertNotNull(Laender.suche("Österreich"))
        assertEquals("Wien", land.hauptstadt)
        assertTrue(abs(land.breite - 48.21) < 0.05, "${land.breite}")
        assertTrue(abs(land.laenge - 16.37) < 0.05, "${land.laenge}")
    }

    @Test
    fun schreibweisenUndSprachenSindEgal() {
        for (fassung in listOf("Österreich", "oesterreich", "OESTERREICH", "Austria", "austria")) {
            val land = assertNotNull(Laender.suche(fassung), "\"$fassung\" nicht gefunden")
            assertEquals("Wien", land.hauptstadt, "\"$fassung\" fuehrt woandershin")
        }
        assertEquals("Berlin", Laender.suche("Germany")?.hauptstadt)
        assertEquals("Prag", Laender.suche("cesko")?.hauptstadt)
    }

    @Test
    fun nurDerGanzeNameZaehlt() {
        // Ein Anfangsvergleich waere hier falsch: "s" wuerde die Schweiz
        // liefern und "d" Deutschland -- ein Sprung ueber tausend Kilometer
        // als Antwort auf einen halb getippten Ortsnamen.
        assertNull(Laender.suche("d"))
        assertNull(Laender.suche("deutsch"))
        assertNull(Laender.suche("s"))
        assertNull(Laender.suche(""))
    }

    @Test
    fun alleStellenLiegenAufDerErde() {
        for (land in Laender.alle()) {
            assertTrue(
                land.breite in -90.0..90.0 && land.laenge in -180.0..180.0,
                "${land.name} liegt bei ${land.breite}/${land.laenge}",
            )
            // Europa und Nachbarn -- was weit ausserhalb liegt, ist ein
            // Zahlendreher und kein Land dieser Liste.
            assertTrue(
                land.breite in 30.0..72.0 && land.laenge in -25.0..45.0,
                "${land.name} (${land.hauptstadt}) liegt bei ${land.breite}/${land.laenge} " +
                    "und damit ausserhalb des Kartenbereichs -- Zahlendreher?",
            )
        }
    }

    @Test
    fun jederEintragHatNamenUndHauptstadt() {
        for (land in Laender.alle()) {
            assertTrue(land.name.isNotBlank(), "Eintrag ohne Namen")
            assertTrue(land.hauptstadt.isNotBlank(), "${land.name} ohne Hauptstadt")
        }
    }

    @Test
    fun keinNameKommtZweimalVor() {
        // Zwei Zeilen mit demselben Namen hiessen: eine davon ist tot, und
        // welche, sieht man dem Quelltext nicht an.
        val schluessel = Laender.alle().map { org.compasszero.karte.Namensdatei.falte(it.name) }
        val doppelt = schluessel.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue(doppelt.isEmpty(), "doppelte Eintraege: ${doppelt.keys}")
    }

    @Test
    fun dieselbeHauptstadtHeisstDieselbeStelle() {
        // "Deutschland" und "Germany" sind zwei Zeilen fuer dasselbe Land.
        // Laufen ihre Koordinaten auseinander, springt die Suche je nach
        // Schreibweise woandershin.
        val nachStadt = Laender.alle().groupBy { it.hauptstadt }
        for ((stadt, eintraege) in nachStadt) {
            val erste = eintraege.first()
            for (weiterer in eintraege) {
                assertTrue(
                    abs(weiterer.breite - erste.breite) < 0.0001 &&
                        abs(weiterer.laenge - erste.laenge) < 0.0001,
                    "$stadt steht bei ${erste.name} und ${weiterer.name} verschieden",
                )
            }
        }
    }
}
