package org.compasszero.android

import android.graphics.Color
import android.graphics.Typeface

// Zwei getrennte Darstellungen, kein abgeschwaechter Dunkelmodus. Der Sparmodus
// hat reines Schwarz (auf OLED kostet ein schwarzes Pixel keinen Strom), grosse
// serifenlose Schrift und keine Zierde. Was hier nicht steht, gibt es dort auch
// nicht: keine Blob-Formen, keine Texturen, keine Bilder, keine Bewegung.
class Stil(
    val hintergrund: Int,
    // Zweiter Papierton fuer Flaechen, die sich vom Blatt abheben sollen:
    // Kopfzone und Kacheln. Ein blosser Rahmen laesst eine Kachel wie einen
    // leeren Kasten aussehen, eine Flaeche macht sie zu einem Gegenstand.
    val flaeche: Int,
    // Die Punkte der Wortmarke als Flaechenmuster. Sie sind das einzige
    // grafische Zeichen, das die Marke mitbringt -- ohne sie hat die Oberflaeche
    // keine eigene Bildsprache.
    val raster: Int,
    // Der Ton der grossen organischen Flaeche in der Kopfzone. Kein reines
    // Schwarz: Auf Papierton wirkt reines Schwarz wie ein Loch, ein sehr
    // dunkles Braunschwarz wie Druckfarbe. Im Sparmodus wird die Flaeche gar
    // nicht gezeichnet, dort traegt der Wert deshalb nichts.
    val marke: Int,
    val text: Int,
    val gedaempft: Int,
    val signal: Int,
    val trennlinie: Int,
    // Familie und Schnitt statt fertiger Schriften: Eine Schrift entsteht erst
    // beim ersten Zugriff. So laesst sich der Stil auch ausserhalb einer
    // laufenden App bauen und seine Zusagen pruefen.
    private val ueberschriftFamilie: String,
    private val ueberschriftSchnitt: Int,
    private val textFamilie: String,
    private val textSchnitt: Int,
    val titelGroesse: Float,
    val textGroesse: Float,
    val listenGroesse: Float,
    val abstand: Int,
    // Die einzige Stelle, an der eine Bewegungsdauer steht. Der Sparmodus setzt
    // sie auf null, und damit faellt jede Bewegung aus -- nicht abgeschwaecht,
    // sondern gar nicht. Wer im Ernstfall jede Reserve braucht, bezahlt keine
    // Bildwiederholung fuer Zierde.
    val bewegungMs: Long,
) {
    val ueberschriftSchrift: Typeface by lazy { Typeface.create(ueberschriftFamilie, ueberschriftSchnitt) }
    val textSchrift: Typeface by lazy { Typeface.create(textFamilie, textSchnitt) }

    companion object {

        // Editorial: Papierton, Serifen fuer Ueberschriften, genau ein
        // Signalton — Orange bleibt kritischen Hinweisen vorbehalten.
        fun normal(dichte: Float) = Stil(
            hintergrund = Color.rgb(250, 249, 246),
            flaeche = Color.rgb(242, 239, 231),
            raster = Color.rgb(203, 198, 186),
            marke = Color.rgb(22, 20, 18),
            text = Color.rgb(17, 17, 17),
            gedaempft = Color.rgb(110, 108, 104),
            signal = Color.rgb(214, 90, 26),
            trennlinie = Color.rgb(226, 223, 216),
            ueberschriftFamilie = "serif",
            ueberschriftSchnitt = Typeface.NORMAL,
            textFamilie = "sans-serif",
            textSchnitt = Typeface.NORMAL,
            titelGroesse = 26f,
            textGroesse = 16f,
            listenGroesse = 18f,
            abstand = (16 * dichte).toInt(),
            bewegungMs = 140,
        )

        // Flaeche und Raster tragen hier dieselbe Farbe wie der Hintergrund: Der
        // Sparmodus zeichnet sie ohnehin nicht, und ein zweiter Ton waere auf
        // OLED bezahlter Strom fuer Zierde.
        fun sparmodus(dichte: Float) = Stil(
            hintergrund = Color.BLACK,
            flaeche = Color.BLACK,
            raster = Color.BLACK,
            marke = Color.BLACK,
            text = Color.WHITE,
            gedaempft = Color.rgb(190, 190, 190),
            signal = Color.rgb(255, 214, 0),
            trennlinie = Color.rgb(60, 60, 60),
            ueberschriftFamilie = "sans-serif",
            ueberschriftSchnitt = Typeface.BOLD,
            textFamilie = "sans-serif",
            textSchnitt = Typeface.NORMAL,
            titelGroesse = 30f,
            textGroesse = 21f,
            listenGroesse = 24f,
            abstand = (20 * dichte).toInt(),
            bewegungMs = 0,
        )
    }
}
