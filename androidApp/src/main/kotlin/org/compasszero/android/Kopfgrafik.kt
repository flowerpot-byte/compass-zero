package org.compasszero.android

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

// Der Grund der Kopfzone: ein feines Halbtonraster, das nach rechts hin
// ausduennt, und eine grosse organische Flaeche, die von rechts hereinragt und
// aus dem Bild laeuft.
//
// Das sind die beiden Mittel der vorgegebenen Vorlage: ein Punktraster von
// grob nach fein und eine selbstbewusste weiche Vollflaeche. Beides zusammen
// gibt der Marke ein Bild statt nur einer Zeile.
//
// ALS DRAWABLE UND NICHT ALS VIEW. Ein Hintergrund bekommt seine Groesse vom
// Element, das ihn traegt. Als eigene View in einem FrameLayout hat die Grafik
// am 29.07. den ganzen Bildschirm eingenommen, weil eine Kind-View mit
// MATCH_PARENT in einem Elter mit WRAP_CONTENT den ganzen Rest bekommt.
//
// ZWEI ZUSAGEN:
//  1. IM SPARMODUS WIRD SIE GAR NICHT ERST GEBAUT -- der Sparmodus-Zweig in
//     kopfzeile() kehrt vorher zurueck. Sie kostet dort kein Pixel.
//  2. KEINE BEWEGUNG. Ein einziges draw, kein Animator, kein invalidate.
class Kopfgrafik(
    private val flaechenfarbe: Int,
    private val punktfarbe: Int,
) : Drawable() {

    private val pinsel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val form = Path()

    override fun draw(leinwand: Canvas) {
        val b = bounds
        val breite = b.width().toFloat()
        val hoehe = b.height().toFloat()
        if (breite <= 0f || hoehe <= 0f) return

        leinwand.save()
        leinwand.translate(b.left.toFloat(), b.top.toFloat())
        punktfeld(leinwand, breite, hoehe)
        flaeche(leinwand, breite, hoehe)
        leinwand.restore()
    }

    // Feines Raster mit gleichbleibendem Abstand und schrumpfendem Punkt: Der
    // Abstand darf sich nicht aendern, sonst wirkt es wie ein Fehler und nicht
    // wie ein Verlauf. Die Punkte sind bewusst klein -- ein Halbton, kein
    // Tupfenmuster.
    private fun punktfeld(leinwand: Canvas, breite: Float, hoehe: Float) {
        val schritt = hoehe / 14f
        if (schritt <= 0.5f) return
        val feldbreite = breite * 0.30f
        pinsel.color = punktfarbe
        var y = schritt * 0.5f
        while (y < hoehe) {
            var x = schritt * 0.5f
            while (x < feldbreite) {
                val anteil = (x / feldbreite).coerceIn(0f, 1f)
                val radius = schritt * 0.15f * (1f - anteil * anteil)
                if (radius > 0.25f) {
                    // Bewusst zurueckhaltend: Das Raster liegt jetzt HINTER der
                    // Wortmarke. Ein kraeftiger Punkt frisst dort die Schrift.
                    pinsel.alpha = (150 * (1f - anteil * 0.8f)).toInt().coerceIn(0, 255)
                    leinwand.drawCircle(x, y, radius, pinsel)
                }
                x += schritt
            }
            y += schritt
        }
        pinsel.alpha = 255
    }

    // Eine geschlossene Form aus zwei weichen Boegen, die rechts, oben und
    // unten aus dem Bild laeuft. Sie sitzt bewusst NICHT mittig: Eine Form, die
    // den Rand beruehrt, wirkt wie ein Ausschnitt aus etwas Groesserem; eine,
    // die frei in der Mitte schwebt, wie ein Fleck.
    //
    // Die Kontrollpunkte sind auf ein breites, flaches Band gerechnet. Bei
    // einer hohen schmalen Flaeche ergaebe dieselbe Kurve einen senkrechten
    // Streifen -- deshalb haengen sie an der BREITE, nicht an der Hoehe.
    private fun flaeche(leinwand: Canvas, breite: Float, hoehe: Float) {
        form.reset()
        val kante = breite * 1.06f
        // Der Einzug bestimmt, wie weit die Form nach links reicht. Sie muss
        // rechts vom Schriftzug bleiben -- die Marke steht linksbuendig.
        val einzug = breite * 0.68f

        // Sie tritt WEIT oberhalb und unterhalb des Bandes ein und aus. Eine
        // Form, deren Anfang und Ende man sieht, ist ein Klecks; eine, die oben
        // und unten aus dem Bild laeuft, ist ein Ausschnitt aus etwas
        // Groesserem. Genau das ist das Mittel der Vorlage.
        //
        // Die beiden Flanken haben ABSICHTLICH verschiedene Kruemmung: die
        // obere holt lang und flach aus, die untere kehrt kurz und straff
        // zurueck. Gleiche Radien oben und unten ergaeben einen Halbkreis --
        // erkennbar konstruiert. So entsteht ein Tropfen, der nach unten kippt.
        form.moveTo(kante, -hoehe * 0.55f)
        // obere Flanke: langer flacher Bogen bis zum weitesten Punkt bei 0,58
        form.cubicTo(
            breite * 0.94f, -hoehe * 0.02f,
            einzug - breite * 0.02f, hoehe * 0.12f,
            einzug, hoehe * 0.58f,
        )
        // untere Flanke: kurz und straff zurueck an den rechten Rand
        form.cubicTo(
            einzug + breite * 0.02f, hoehe * 0.92f,
            breite * 0.88f, hoehe * 1.02f,
            kante, hoehe * 1.45f,
        )
        form.close()

        pinsel.color = flaechenfarbe
        pinsel.alpha = 255
        leinwand.drawPath(form, pinsel)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(filter: ColorFilter?) = Unit

    @Deprecated("Von Drawable vorgegeben", ReplaceWith("PixelFormat.OPAQUE"))
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
