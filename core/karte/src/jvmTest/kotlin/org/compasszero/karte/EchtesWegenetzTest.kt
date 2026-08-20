package org.compasszero.karte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Rechnet auf einem WIRKLICH gebauten Wegenetz, wenn eines dasteht.
 *
 * `WegenetzTest` baut ein Netz aus fuenf Knoten, dessen richtige Antwort man
 * nachrechnen kann. Was er nicht zeigen kann: ob `tools/karte/wege_bauen.py`
 * und dieser Leser dieselbe Vorstellung vom Format haben, und ob die
 * Wegesuche auf 76 000 Knoten in vertretbarer Zeit fertig wird.
 *
 * Bei der Bilddatei hat genau dieser Gegencheck den Sortierfehler gefunden.
 */
class EchtesWegenetzTest {

    @Test
    fun aufEchtenDatenKommtEineBrauchbareRouteHeraus() {
        val ordner = File(System.getProperty("compasszero.repoRoot") ?: ".", "work/karte")
        val datei = ordner.listFiles()?.firstOrNull { it.name.endsWith(".czw") } ?: return
        Wegenetz.oeffne(datei).use { w ->
            assertTrue(w.knotenzahl > 1000, "${datei.name} hat nur ${w.knotenzahl} Knoten")
            assertTrue(w.kantenzahl > 1000, "${datei.name} hat nur ${w.kantenzahl} Kanten")

            // Zwei Knoten weit auseinander suchen: der westlichste und der
            // oestlichste. Zwischen Nachbarn zu routen zeigt nichts.
            var west = 0
            var ost = 0
            for (i in 0 until w.knotenzahl) {
                if (w.laengeVon(i) < w.laengeVon(west)) west = i
                if (w.laengeVon(i) > w.laengeVon(ost)) ost = i
            }
            val luftlinie = Wegenetz.entfernung(
                w.breiteVon(west), w.laengeVon(west),
                w.breiteVon(ost), w.laengeVon(ost),
            )
            assertTrue(luftlinie > 1000, "die beiden Knoten liegen nur $luftlinie m auseinander")

            val begonnen = System.currentTimeMillis()
            val route = w.route(west, ost)
            val dauer = System.currentTimeMillis() - begonnen
            // Eine Route MUSS es nicht geben -- ein Netz kann in Inseln
            // zerfallen, und das ist kein Fehler. Kommt aber eine, muss sie
            // stimmen.
            if (route == null) return

            assertTrue(
                dauer < 20_000,
                "die Wegesuche brauchte $dauer ms -- auf einem Handy waere das unbrauchbar",
            )
            // EIN WEG KANN NIE KUERZER SEIN ALS DIE LUFTLINIE. Kaeme hier
            // etwas Kuerzeres heraus, waeren Laengen und Geometrie
            // gegeneinander verrutscht -- und eine Route, die zu kurz
            // gerechnet ist, laesst jemanden mit zu wenig Wasser losgehen.
            assertTrue(
                route.meter >= luftlinie - 1.0,
                "Route ist ${route.meter} m, Luftlinie aber $luftlinie m",
            )
            assertTrue(route.punkte.size >= 2, "Route hat nur ${route.punkte.size} Punkte")

            // Anfang und Ende der gezeichneten Linie muessen wirklich an den
            // beiden Knoten liegen. Sonst faengt die Linie irgendwo an.
            val anfang = route.punkte.first()
            val ende = route.punkte.last()
            assertTrue(
                Wegenetz.entfernung(anfang[0], anfang[1], w.breiteVon(west), w.laengeVon(west)) < 50,
                "die Linie faengt nicht am Startknoten an",
            )
            assertTrue(
                Wegenetz.entfernung(ende[0], ende[1], w.breiteVon(ost), w.laengeVon(ost)) < 50,
                "die Linie endet nicht am Zielknoten",
            )

            // Die Linie darf keine Spruenge machen. Ein Sprung heisst, dass
            // eine Kante verkehrt herum angehaengt wurde -- auf dem Bild
            // sieht das aus wie ein Blitz quer durch die Landschaft.
            var groessterSprung = 0.0
            for (i in 0 until route.punkte.size - 1) {
                val a = route.punkte[i]
                val b = route.punkte[i + 1]
                val d = Wegenetz.entfernung(a[0], a[1], b[0], b[1])
                if (d > groessterSprung) groessterSprung = d
            }
            assertTrue(
                groessterSprung < 5000,
                "die Route springt um ${groessterSprung.toInt()} m -- eine Kante liegt verkehrt herum",
            )
        }
    }
}
