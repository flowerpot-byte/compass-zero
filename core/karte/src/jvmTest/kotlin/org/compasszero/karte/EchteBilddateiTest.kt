package org.compasszero.karte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Liest eine WIRKLICH gebaute Datei, wenn eine dasteht.
 *
 * Der eigentliche Lesertest baut seine Dateien selbst -- das muss er, sonst
 * laeuft er nach einem frischen Klon nicht. Damit prueft er aber nur, dass
 * der Leser seine eigene Vorstellung vom Format einhaelt. Ob Werkzeug und
 * Leser DIESELBE Vorstellung haben, zeigt erst eine Datei aus
 * `tools/karte/bilder_bauen.py`. Liegt keine da, ist hier nichts zu tun.
 */
class EchteBilddateiTest {

    @Test
    fun eineGebauteDateiLaesstSichLesen() {
        val ordner = File(System.getProperty("compasszero.repoRoot") ?: ".", "work/karte")
        val datei = ordner.listFiles()?.firstOrNull { it.name.endsWith(".czb") } ?: return
        Bilddatei.oeffne(datei).use { d ->
            assertTrue(d.kachelzahl > 0, "${datei.name} hat keine Kacheln")
            assertTrue(d.zoomGroesste <= Bilddatei.MAX_ZOOM, "Zoom ${d.zoomGroesste} zu hoch")
            var gelesen = 0
            for (z in d.zoomKleinste..d.zoomGroesste) {
                val k = d.kachel(z, kachelX(d.west + 0.01, z), kachelY(d.nord - 0.01, z))
                if (k != null) {
                    // Jedes JPEG faengt mit FF D8 an. Kaeme hier etwas anderes,
                    // waeren Verzeichnis und Inhalt gegeneinander verschoben --
                    // und das Bild laege im Gelaende an der falschen Stelle.
                    assertTrue(
                        k.roh.size > 2 &&
                            k.roh[0] == 0xFF.toByte() && k.roh[1] == 0xD8.toByte(),
                        "Kachel $z/${k.x}/${k.y} ist kein JPEG",
                    )
                    gelesen++
                }
            }
            assertTrue(gelesen > 0, "keine einzige Kachel gefunden in ${datei.name}")
        }
    }

    private fun kachelX(lon: Double, z: Int): Int =
        ((lon + 180.0) / 360.0 * (1 shl z)).toInt()

    private fun kachelY(lat: Double, z: Int): Int {
        val r = Math.toRadians(lat)
        return ((1.0 - Math.log(Math.tan(r) + 1.0 / Math.cos(r)) / Math.PI) / 2.0 * (1 shl z)).toInt()
    }
}
