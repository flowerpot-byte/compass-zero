package org.compasszero.android

import android.graphics.Color
import android.graphics.Paint
import org.compasszero.karte.Kartenformat

/**
 * Farben und Strichbreiten der Karte.
 *
 * Zwei Darstellungen, wie im uebrigen Programm: das Blatt und der Sparmodus.
 *
 * Der Sparmodus fuellt KEINE Flaeche. Das ist keine Sparsamkeit aus Bequemheit,
 * sondern dieselbe Regel wie ueberall: kein Band, kein Raster, keine zweite
 * Flaeche. Auf einem OLED-Schirm kostet jedes helle Bildpunktfeld Strom, und
 * wer im Ernstfall die letzte Ladung einteilt, braucht die Linien -- Kueste,
 * Fluss, Weg -- und nicht die Farbe dazwischen.
 */
class Kartenstil private constructor(
    val hintergrund: Int,
    private val fuellung: IntArray,
    private val strich: IntArray,
    private val breite: FloatArray,
    val beschriftung: Int,
    val beschriftungRand: Int,
    /** Wasserstellen: Quelle, Brunnen, Trinkwasser. */
    val punktfarbeWasser: Int,
    /** Alles andere: Gipfel, Pass, Huette, Hoehle, Aussicht. */
    val punktfarbeSonst: Int,
    /** Der schlichte Punkt einer Ortschaft -- er markiert nur die Stelle. */
    val punktfarbeOrt: Int,
    val dichte: Float,
    /**
     * Ob die Gelaendeform als Schummerung unter die Karte gelegt wird.
     *
     * Im Sparmodus nicht: Eine Schummerung ist eine FLAECHE, und Flaechen gibt
     * es dort nicht -- kein Band, kein Raster, keine zweite Flaeche. Auf einem
     * OLED-Schirm kostet sie ausserdem in jedem Bildpunkt Strom, den man im
     * Ernstfall besser fuer die Linien ausgibt.
     */
    val schummert: Boolean,
) {

    /**
     * Der Signalton bleibt dem Wasser vorbehalten.
     *
     * Beim ersten Blick auf die fertige Karte war jeder Punkt gleich laut
     * orange -- und weil OpenStreetMap jede Kuppe als Gipfel fuehrt, war die
     * Karte ein Feld aus orangen Tupfen, in dem eine Quelle nicht mehr
     * auffiel. Genau umgekehrt muss es sein: Wasser entscheidet in drei Tagen
     * ueber Leben und Tod, ein Aussichtspunkt nie.
     */
    fun punktfarbe(punktart: Int): Int = when (Kartenformat.punktartName(punktart)) {
        "quelle", "brunnen", "trinkwasser", "wasserturm" -> punktfarbeWasser
        else -> punktfarbeSonst
    }

    fun fuellt(sorte: Int): Boolean = fuellung[sorte] != Color.TRANSPARENT

    fun fuellfarbe(sorte: Int): Int = fuellung[sorte]

    fun zeichnetStrich(sorte: Int): Boolean = strich[sorte] != Color.TRANSPARENT

    fun strichfarbe(sorte: Int): Int = strich[sorte]

    /**
     * Strichbreite in Bildpunkten der Kachel (256 je Kante).
     *
     * DIE BREITE HAENGT AN DER ZOOMSTUFE, und der Grund steht in der
     * Rueckmeldung vom 18.08.2026: Bei Zoom 7 war die Karte ein weisses
     * Gewirr. Das lag nicht an der Farbe -- auf dieser Stufe sind ALLE weissen
     * Linien Hauptstrassen, und quer durch Deutschland ist dieses Netz von
     * Haus aus dicht. Mit 2,4 Bildpunkten Breite wachsen die Striche dort
     * zusammen, und aus einem Netz wird eine Flaeche.
     *
     * Nah dagegen ist das Umgekehrte richtig: Ein Pfad, dem man wirklich
     * folgt, darf kein Haar sein.
     */
    fun strichbreite(sorte: Int, zoom: Int): Float {
        val grund = breite[sorte]
        if (sorte !in Kartenformat.WEG_HAUPT..Kartenformat.WEG_PFAD) return grund
        return when {
            zoom <= 8 -> grund * 0.6f
            zoom == 9 -> grund * 0.8f
            zoom >= 12 -> grund * 1.4f
            else -> grund
        }
    }

    fun neuerPinsel(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /** Grenzen werden gestrichelt gezeichnet -- eine Grenze ist kein Weg. */
    fun gestrichelt(sorte: Int): Boolean =
        sorte == Kartenformat.GRENZE || sorte == Kartenformat.GRENZE_REGION

    companion object {

        private const val SORTEN = 16

        /**
         * Die Reihenfolge, in der gezeichnet wird. Was spaeter kommt, liegt
         * oben. Sie ist nicht die Reihenfolge im Dateiformat -- eine Wiese
         * unter dem Wald, der Wald unter dem Wasser, das Wasser unter dem Weg.
         */
        val REIHENFOLGE = intArrayOf(
            Kartenformat.OFFEN,
            Kartenformat.WALD,
            Kartenformat.SUMPF,
            Kartenformat.GLETSCHER,
            Kartenformat.SIEDLUNG,
            Kartenformat.WASSER,
            Kartenformat.FLUSS,
            Kartenformat.BACH,
            Kartenformat.WEG_PFAD,
            Kartenformat.WEG_NEBEN,
            Kartenformat.WEG_HAUPT,
            // Grenzen zuletzt: Sie liegen ueber allem, weil sie keine Sache
            // im Gelaende sind, sondern eine Auskunft ueber das Gelaende.
            Kartenformat.GRENZE_REGION,
            Kartenformat.GRENZE,
        )

        fun normal(dichte: Float): Kartenstil {
            val fuellung = IntArray(SORTEN) { Color.TRANSPARENT }
            val strich = IntArray(SORTEN) { Color.TRANSPARENT }
            val breite = FloatArray(SORTEN)

            // Papierton als Land, damit die Karte zum uebrigen Blatt passt.
            fuellung[Kartenformat.OFFEN] = Color.rgb(240, 237, 226)
            fuellung[Kartenformat.WALD] = Color.rgb(203, 216, 197)
            fuellung[Kartenformat.SUMPF] = Color.rgb(206, 214, 205)
            fuellung[Kartenformat.GLETSCHER] = Color.rgb(232, 238, 242)
            fuellung[Kartenformat.SIEDLUNG] = Color.rgb(228, 222, 210)
            fuellung[Kartenformat.WASSER] = Color.rgb(169, 196, 212)

            strich[Kartenformat.WASSER] = Color.rgb(133, 165, 186)
            strich[Kartenformat.GLETSCHER] = Color.rgb(178, 196, 208)
            strich[Kartenformat.FLUSS] = Color.rgb(133, 165, 186)
            strich[Kartenformat.BACH] = Color.rgb(150, 180, 200)
            strich[Kartenformat.WEG_HAUPT] = Color.rgb(150, 96, 60)
            strich[Kartenformat.WEG_NEBEN] = Color.rgb(120, 112, 100)
            strich[Kartenformat.WEG_PFAD] = Color.rgb(140, 106, 78)

            breite[Kartenformat.WASSER] = 0.6f
            breite[Kartenformat.GLETSCHER] = 0.6f
            breite[Kartenformat.FLUSS] = 1.5f
            breite[Kartenformat.BACH] = 0.8f
            breite[Kartenformat.WEG_HAUPT] = 1.8f
            breite[Kartenformat.WEG_NEBEN] = 1.1f
            breite[Kartenformat.WEG_PFAD] = 0.8f

            // Ein eigener Ton, der keinem Gelaendemerkmal gehoert -- sonst
            // liest man eine Grenze als Weg oder als Bachlauf.
            //
            // KRAEFTIGER ALS ZUERST GEDACHT, und das ist kein Geschmack: Mit
            // rgb(126,88,128) auf 1,6 Bildpunkten waren die Grenzen auf dem
            // Geraet zwar vorhanden, aber neben den braunen Strassen so blass,
            // dass sie am 05.08.2026 schlicht vermisst wurden. Eine
            // Staatsgrenze ist die wichtigste Linie einer Uebersichtskarte --
            // an ihr haengen Sprache, Notrufnummer und Recht. Sie muss die
            // auffaelligste Linie sein, nicht die unauffaelligste.
            strich[Kartenformat.GRENZE] = Color.rgb(104, 58, 116)
            strich[Kartenformat.GRENZE_REGION] = Color.rgb(150, 118, 156)
            breite[Kartenformat.GRENZE] = 2.6f
            breite[Kartenformat.GRENZE_REGION] = 1.2f

            return Kartenstil(
                hintergrund = Color.rgb(246, 244, 237),
                fuellung = fuellung,
                strich = strich,
                breite = breite,
                beschriftung = Color.rgb(45, 42, 38),
                beschriftungRand = Color.argb(210, 250, 249, 246),
                punktfarbeWasser = Color.rgb(214, 90, 26),
                punktfarbeOrt = Color.rgb(45, 42, 38),
                punktfarbeSonst = Color.rgb(96, 92, 86),
                dichte = dichte,
                schummert = true,
            )
        }

        fun sparmodus(dichte: Float): Kartenstil {
            val fuellung = IntArray(SORTEN) { Color.TRANSPARENT }
            val strich = IntArray(SORTEN) { Color.TRANSPARENT }
            val breite = FloatArray(SORTEN)

            // FARBE, UND ZWAR AUS STROMGRUENDEN -- nicht trotz ihnen.
            //
            // Hier standen bis zum 18.08.2026 lauter Grautoene zwischen 140
            // und 255. Der Gedanke dahinter war, auf einem OLED-Schirm Strom
            // zu sparen. Er war falsch herum: WEISS IST DIE TEUERSTE FARBE,
            // die ein OLED kennt, weil es dafuer alle drei Subpixel voll
            // ansteuert. Eine blaue Linie leuchtet nur den blauen Subpixel an
            // und kostet damit WENIGER als dieselbe Linie in Weiss. Es gab
            // also nie einen Stromgrund fuer die Einheitsfarbe -- nur einen
            // Denkfehler.
            //
            // Was er angerichtet hat, steht in der Rueckmeldung vom
            // 18.08.2026: "die karte ist noch absolut unuebersichtlich dadurch
            // das alle linien nur weiss sind und sich so nicht unterscheiden
            // lassen." Grautoene, die auf dem Bildschirm nebeneinander noch
            // verschieden aussehen, sind es als kantengeglaettete Haarlinie
            // nicht mehr: Der Glaettungsrand frisst genau den
            // Helligkeitsunterschied auf, an dem man sie unterscheiden sollte.
            //
            // Deshalb jetzt EIGENE FARBTOENE statt eigener Helligkeiten.
            // Wasser blau, Wege warm, Grenzen violett -- die Zuordnung, die
            // jeder von gedruckten Karten kennt, und drei Toene, die auch als
            // ein Bildpunkt breite Linie noch auseinanderzuhalten sind.
            strich[Kartenformat.WASSER] = Color.rgb(70, 150, 255)
            strich[Kartenformat.GLETSCHER] = Color.rgb(120, 235, 235)
            strich[Kartenformat.FLUSS] = Color.rgb(90, 175, 255)
            strich[Kartenformat.BACH] = Color.rgb(40, 110, 210)
            strich[Kartenformat.WEG_HAUPT] = Color.WHITE
            // Deutlich gedaempfter als die Hauptstrasse. Vorher lagen beide
            // bei ueber 200 und waren als Haarlinie dasselbe Weiss.
            strich[Kartenformat.WEG_NEBEN] = Color.rgb(165, 158, 145)
            strich[Kartenformat.WEG_PFAD] = Color.rgb(235, 150, 60)

            // Auch die BREITEN weiter auseinander. Vorher lagen sechs Sorten
            // zwischen 0,9 und 1,8 -- auf dem Geraet ist das der Unterschied
            // zwischen einem Haar und einem etwas dickeren Haar.
            breite[Kartenformat.WASSER] = 1.0f
            breite[Kartenformat.GLETSCHER] = 1.0f
            breite[Kartenformat.FLUSS] = 2.0f
            breite[Kartenformat.BACH] = 0.8f
            breite[Kartenformat.WEG_HAUPT] = 2.4f
            breite[Kartenformat.WEG_NEBEN] = 1.4f
            breite[Kartenformat.WEG_PFAD] = 0.9f

            // Auch im Sparmodus: Ohne Umriss weiss man bei einem Binnenland
            // nicht, wo es aufhoert. Die Staatsgrenze ist hier die hellste
            // Linie ueberhaupt -- teurer als jede andere auf einem
            // OLED-Schirm, aber die Strich-Punkt-Linie laesst drei Viertel der
            // Strecke schwarz, und an einer Staatsgrenze haengen Sprache,
            // Notrufnummer und Recht. Die Landesgrenze bleibt daneben leise.
            strich[Kartenformat.GRENZE] = Color.rgb(220, 130, 255)
            strich[Kartenformat.GRENZE_REGION] = Color.rgb(130, 80, 160)
            breite[Kartenformat.GRENZE] = 2.0f
            breite[Kartenformat.GRENZE_REGION] = 0.9f

            return Kartenstil(
                hintergrund = Color.BLACK,
                fuellung = fuellung,
                strich = strich,
                breite = breite,
                beschriftung = Color.WHITE,
                // SCHWARZER RAND UM DIE SCHRIFT, und er kostet nichts.
                //
                // Vorher stand hier TRANSPARENT, aus der richtigen Sorge, im
                // Sparmodus keinen hellen Bildpunkt zu verschwenden. Nur ist
                // ein SCHWARZER Rand auf schwarzem Grund genau kein heller
                // Bildpunkt -- er loescht nur dort, wo eine Linie oder ein
                // Punkt unter der Schrift liegt. Ohne ihn klebten die Namen
                // aufeinander und auf den Linien; Rueckmeldung vom 06.08.2026:
                // "vorallem im notfallmodus ist die karte unuebersichtlich."
                beschriftungRand = Color.BLACK,
                punktfarbeWasser = Color.rgb(255, 214, 0),
                punktfarbeSonst = Color.rgb(150, 150, 150),
                // Der Ortspunkt ist im Sparmodus GEDAEMPFT, nicht weiss.
                // Auf schwarzem Grund war er vorher das Lauteste im ganzen
                // Bild -- ein greller Fleck neben jedem Namen, obwohl der Name
                // die Auskunft traegt und der Punkt nur die Stelle.
                punktfarbeOrt = Color.rgb(130, 130, 130),
                dichte = dichte,
                schummert = false,
            )
        }
    }
}
