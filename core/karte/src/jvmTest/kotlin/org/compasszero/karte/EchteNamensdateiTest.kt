package org.compasszero.karte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Liest ein WIRKLICH gebautes Namensverzeichnis, wenn eines dasteht.
 *
 * WARUM ES DIESEN TEST BRAUCHT: `NamensdateiTest` baut seine Dateien selbst
 * -- das muss er, sonst laeuft er nach einem frischen Klon nicht. Damit
 * prueft er aber nur, dass der Leser seine eigene Vorstellung vom Format
 * einhaelt. Ob `tools/karte/namen_bauen.py` und dieser Leser DIESELBE
 * Vorstellung haben, zeigt erst eine gebaute Datei.
 *
 * Bei der Bilddatei hat genau dieser Gegencheck den Sortierfehler gefunden,
 * der sonst erst auf dem Geraet aufgefallen waere -- und dort nicht als
 * Fehler, sondern als Bild an der falschen Stelle.
 *
 * Liegt keine Datei da, tut der Test nichts. Das ist der Preis dafuer, dass
 * die gebauten Karten nicht im Repo liegen koennen.
 */
class EchteNamensdateiTest {

    @Test
    fun einGebautesVerzeichnisLaesstSichLesenUndDurchsuchen() {
        val ordner = File(System.getProperty("compasszero.repoRoot") ?: ".", "work/karte")
        val datei = ordner.listFiles()?.firstOrNull { it.name.endsWith(".czn") } ?: return
        Namensdatei.oeffne(datei).use { d ->
            assertTrue(d.anzahl > 0, "${datei.name} ist leer")

            // Der erste und der letzte Eintrag muessen sich lesen lassen --
            // an den Raendern faellt eine verschobene Versatztabelle auf.
            val vorne = d.suche("a", hoechstens = 1)
            val hinten = d.suche("z", hoechstens = 1)
            assertTrue(
                vorne.isNotEmpty() || hinten.isNotEmpty(),
                "weder mit a noch mit z war etwas zu finden",
            )

            // DIE PROBE, DIE ZAEHLT: Ein Name, den die Datei selbst liefert,
            // muss sich mit sich selbst wiederfinden lassen. Faltet das
            // Werkzeug anders als der Leser, geht genau das schief -- und im
            // Betrieb faende man einzelne Namen nicht, ohne zu wissen, welche.
            val stichproben = listOf("a", "b", "s", "m", "w")
                .flatMap { d.suche(it, hoechstens = 3) }
                .take(12)
            assertTrue(stichproben.isNotEmpty(), "keine Stichprobe zu bekommen")
            for (treffer in stichproben) {
                val wieder = d.suche(treffer.name, hoechstens = 60).map { it.name }
                assertTrue(
                    treffer.name in wieder,
                    "\"${treffer.name}\" findet sich selbst nicht wieder -- " +
                        "Werkzeug und Leser falten verschieden",
                )
                assertTrue(
                    treffer.breite in -90.0..90.0 && treffer.laenge in -180.0..180.0,
                    "\"${treffer.name}\" liegt bei ${treffer.breite}/${treffer.laenge}",
                )
            }
        }
    }
}
