package org.compasszero.content

import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Kein Blatt darf unten buendig abschliessen.
 *
 * ANLASS: Das Werkzeug, das einen Stich aufs Blatt legt, begrenzt seine
 * BREITE, nie seine Hoehe. Ein hoher Stich laeuft deshalb still unten aus dem
 * Blatt heraus -- am 21.08.2026 bei der Verdampfpfanne aufgefallen, wo die
 * Zeichnung 49 Punkte ueber den Rand hinausging und die Fusszeile ueberlagerte.
 * Bei der Nachschau fanden sich neun weitere Blaetter aus einem frueheren Lauf,
 * deren letzte Textzeile ohne jeden Rand am Blattende klebte und deren
 * Unterlaengen angeschnitten waren.
 *
 * Verloren war dabei kein Text -- aber ein Blatt, das unten abgeschnitten
 * aussieht, laesst den Leser zu Recht fragen, ob ihm etwas fehlt. Bei einem
 * Handbuch, in dem es auf Vollstaendigkeit ankommt, ist das kein
 * Schoenheitsfehler.
 *
 * Geprueft werden nur die untersten Zeilen, und die liest der Test einzeln aus
 * der Datei statt das ganze Bild zu entpacken -- sonst waeren es bei 337
 * Blaettern rund 330 Millionen Bildpunkte fuer eine Handvoll Zeilen.
 */
class BlattrandTest {

    private companion object {
        /** So viele Zeilen am unteren Rand muessen frei von Zeichnung sein. */
        const val FREIE_ZEILEN = 8

        /** Wie weit ein Punkt vom Blattgrund abweichen darf, ohne zu zaehlen. */
        const val TOLERANZ = 45
    }

    private fun repoRoot(): File {
        val fromProperty = System.getProperty("compasszero.repoRoot")
        if (fromProperty != null) return File(fromProperty)
        var dir = File(".").absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("settings.gradle.kts nicht gefunden")
        }
        return dir
    }

    @Test
    fun keinBlattSchliesstBuendigAb() {
        val assets = File(repoRoot(), "content/europe-de/paket/assets")
        val blaetter = (assets.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".png") }
            .sortedBy { it.name }
        assertTrue(blaetter.isNotEmpty(), "Keine Blaetter gefunden -- der Test misst nichts.")

        val buendig = mutableListOf<String>()
        for (datei in blaetter) {
            ImageIO.createImageInputStream(datei).use { strom ->
                val leser = ImageIO.getImageReaders(strom).next()
                leser.setInput(strom, true)
                val breite = leser.getWidth(0)
                val hoehe = leser.getHeight(0)
                val vorgabe = leser.defaultReadParam
                // Nur der untere Streifen -- und eine Zeile weiter oben als
                // Vergleich, denn der Blattgrund steht nicht in der Datei.
                val ab = maxOf(0, hoehe - FREIE_ZEILEN)
                vorgabe.sourceRegion = java.awt.Rectangle(0, ab, breite, hoehe - ab)
                val streifen = leser.read(0, vorgabe)
                leser.dispose()

                // Der Blattgrund ist der haeufigste Wert in der ALLERLETZTEN
                // Zeile eines heilen Blattes -- also nehmen wir den haeufigsten
                // Wert des ganzen Streifens als Bezug. Steht dort Zeichnung,
                // faellt sie als Ausreisser auf.
                val haeufig = HashMap<Int, Int>()
                for (y in 0 until streifen.height) {
                    for (x in 0 until streifen.width step 3) {
                        val v = streifen.getRGB(x, y)
                        haeufig[v] = (haeufig[v] ?: 0) + 1
                    }
                }
                val grund = haeufig.maxByOrNull { it.value }!!.key
                val grundHell = maxOf(
                    (grund shr 16) and 0xFF,
                    maxOf((grund shr 8) and 0xFF, grund and 0xFF),
                )
                var dunkel = 0
                for (y in 0 until streifen.height) {
                    for (x in 0 until streifen.width step 3) {
                        val v = streifen.getRGB(x, y)
                        val hell = maxOf(
                            (v shr 16) and 0xFF,
                            maxOf((v shr 8) and 0xFF, v and 0xFF),
                        )
                        if (hell < grundHell - TOLERANZ) dunkel++
                    }
                }
                if (dunkel > 6) buendig += "${datei.name}: $dunkel Punkte Zeichnung in den " +
                    "untersten $FREIE_ZEILEN Zeilen"
            }
        }

        assertTrue(
            buendig.isEmpty(),
            "Diese Blaetter schliessen unten buendig ab und sehen abgeschnitten aus:\n" +
                buendig.joinToString("\n") { "  $it" } +
                "\nEntweder das Blatt hoeher rechnen oder unten Blattgrund anfuegen.",
        )
    }
}
