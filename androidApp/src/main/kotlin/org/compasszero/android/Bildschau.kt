package org.compasszero.android

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Eine Skizze bildschirmfuellend, gedreht und vergroesserbar.
 *
 * WOZU: siehe `Bildlupe` -- dort steht die Messung, aus der das hier folgt.
 * Kurz: Im Artikel steht eine 900 Punkte breite Zeichnung in einer Spalte von
 * rund 1000 Punkten. Ihre kleinste Beschriftung kommt damit bei etwa 14
 * Punkten heraus. Wer sie lesen will, muss sie vergroessern koennen.
 *
 * ES GIBT KEINE ZWEITE ACTIVITY DAFUER. Die Schau haengt sich als oberste
 * Ebene in das Fenster, das ohnehin offen ist. Grund: Eine zweite Activity
 * haette einen eigenen Lebenslauf, muesste das Bild ueber ein Intent bekommen
 * (und damit entweder serialisieren oder erneut aus dem Paket lesen) und im
 * Manifest stehen. Fuer eine Ansicht, die mit der Zurueck-Taste wieder
 * verschwindet, ist das alles Aufwand ohne Gegenwert.
 *
 * DAS BILD WIRD NICHT ERNEUT GELESEN. Es kommt fertig aus `Skizzen.laden`,
 * also aus dem geprueften Paket -- die Schau haelt nur eine Referenz. Sie
 * gibt das Bild beim Schliessen wieder her, damit ein grosses Bitmap nicht
 * ueber die ganze Sitzung im Speicher haengt.
 */
class Bildschau(private val gastgeber: Activity, private val stil: Stil, private val dichte: Float) {

    private var ebene: FrameLayout? = null

    val offen: Boolean
        get() = ebene != null

    fun zeige(bild: Bitmap, beschriftung: String) {
        schliesse()
        // Nicht `android.R.id.content`, sondern die Ebene, die die Randmasse
        // der Systemleisten kennt -- Begruendung bei MainActivity.ueberlagerung.
        val flaeche = (gastgeber as? MainActivity)?.ueberlagerung() ?: return

        val leinwand = Leinwand(bild)
        val kopf = TextView(gastgeber).apply {
            text = beschriftung
            setTextColor(stil.gedaempft)
            textSize = 13f
            typeface = stil.textSchrift
            val a = (12 * dichte).toInt()
            setPadding(a, a, a, a)
        }
        val hinweis = TextView(gastgeber).apply {
            text = "Ziehen verschiebt · Zurück schließt"
            setTextColor(stil.gedaempft)
            textSize = 12f
            typeface = stil.textSchrift
            gravity = Gravity.CENTER
        }

        // ZWEI KNOEPFE UND NICHT NUR DIE ZWEI-FINGER-GESTE. Zwei Gruende, und
        // der zweite ist der wichtigere:
        // 1. Eine Geste kann man nicht sehen. Wer die Lupe zum ersten Mal
        //    aufmacht, weiss nicht, dass sie mehr kann als anzeigen.
        // 2. Dieses Handbuch wird im Zweifel MIT EINER HAND bedient -- die
        //    andere haelt einen Verband, eine Lampe oder ein Kind fest. Eine
        //    Geste mit zwei Fingern setzt zwei freie Finger derselben Hand
        //    voraus, ein Knopf einen Daumen.
        // Die Geste bleibt zusaetzlich, sie kostet nichts.
        val kleiner = knopf("−") { leinwand.stufeKleiner() }
        val groesser = knopf("+") { leinwand.stufeGroesser() }

        val fussreihe = LinearLayout(gastgeber).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val a = (8 * dichte).toInt()
            setPadding(a, a, a, a)
            addView(kleiner, LinearLayout.LayoutParams(WRAP, WRAP))
            addView(hinweis, LinearLayout.LayoutParams(0, WRAP, 1f))
            addView(groesser, LinearLayout.LayoutParams(WRAP, WRAP))
        }

        val spalte = LinearLayout(gastgeber).apply {
            orientation = LinearLayout.VERTICAL
            addView(kopf, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(leinwand, LinearLayout.LayoutParams(MATCH, 0, 1f))
            addView(fussreihe, LinearLayout.LayoutParams(MATCH, WRAP))
        }

        val neu = FrameLayout(gastgeber).apply {
            setBackgroundColor(stil.hintergrund)
            // Faengt jede Beruehrung ab: Was darunter liegt, darf nicht
            // mitreagieren, solange die Schau oben liegt.
            isClickable = true
            addView(spalte, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        flaeche.addView(neu, FrameLayout.LayoutParams(MATCH, MATCH))
        ebene = neu
    }

    // Gross genug fuer einen Daumen: rund 48 Punkte im Quadrat sind das Mass,
    // unter dem eine Flaeche auf einem Telefon nicht mehr sicher zu treffen ist.
    private fun knopf(zeichen: String, tun: () -> Unit) = TextView(gastgeber).apply {
        text = zeichen
        contentDescription = if (zeichen == "+") "Vergrößern" else "Verkleinern"
        setTextColor(stil.text)
        textSize = 26f
        typeface = stil.textSchrift
        gravity = Gravity.CENTER
        val a = (18 * dichte).toInt()
        setPadding(a, a / 2, a, a / 2)
        minWidth = (48 * dichte).toInt()
        minHeight = (48 * dichte).toInt()
        setOnClickListener { tun() }
    }

    /** Antwortet mit true, wenn die Schau offen war und geschlossen wurde. */
    fun schliesse(): Boolean {
        val offene = ebene ?: return false
        (offene.parent as? ViewGroup)?.removeView(offene)
        ebene = null
        return true
    }

    /**
     * Die eigentliche Flaeche. Sie haelt drei Zahlen und einen Schalter --
     * Massstab, Verschiebung in x und y, gedreht ja/nein -- und baut daraus bei
     * jedem Zeichnen die Matrix neu.
     *
     * WARUM NEU STATT FORTGESCHRIEBEN: Eine Matrix, an der man immer weiter
     * dreht und schiebt, sammelt Rundungsfehler und laesst sich nicht mehr
     * begrenzen, ohne sie zu zerlegen. Aus vier Zahlen aufgebaut ist jeder
     * Zustand jederzeit gueltig und die Grenzen sind einfache Vergleiche --
     * genau die, die `Bildlupe` prueft.
     */
    private inner class Leinwand(val bild: Bitmap) : View(gastgeber) {

        private val stift = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val matrix = Matrix()

        private var gedreht = false
        private var massstab = 1f
        private var verschobenX = 0f
        private var verschobenY = 0f
        private var eingepasst = 1f

        // Nach der Drehung tauschen Breite und Hoehe die Rollen.
        private val inhaltBreite: Float
            get() = (if (gedreht) bild.height else bild.width) * massstab
        private val inhaltHoehe: Float
            get() = (if (gedreht) bild.width else bild.height) * massstab

        private val zoomer = ScaleGestureDetector(
            gastgeber,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    setzeMassstab(massstab * d.scaleFactor, d.focusX, d.focusY)
                    return true
                }
            },
        )

        private val gesten = GestureDetector(
            gastgeber,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                    verschobenX -= dx
                    verschobenY -= dy
                    haltGrenzen()
                    invalidate()
                    return true
                }

                // Doppelt tippen springt zwischen "ganz sichtbar" und dreifach.
                // Die Stelle, auf die getippt wurde, bleibt dabei stehen --
                // sonst sucht man nach dem Sprung, was man gerade ansah.
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val ziel = if (massstab > eingepasst * 1.05f) eingepasst else eingepasst * 3f
                    setzeMassstab(ziel, e.x, e.y)
                    return true
                }

                override fun onDown(e: MotionEvent): Boolean = true
            },
        )

        // Eine Stufe ist das Anderthalbfache. Von "ganz sichtbar" bis zum
        // Achtfachen sind das fuenf Tipps -- wenige genug, um hinzukommen,
        // viele genug, um nicht daran vorbeizuschiessen.
        fun stufeGroesser() = setzeMassstab(massstab * STUFE, width / 2f, height / 2f)

        fun stufeKleiner() = setzeMassstab(massstab / STUFE, width / 2f, height / 2f)

        override fun onSizeChanged(b: Int, h: Int, alt: Int, altH: Int) {
            super.onSizeChanged(b, h, alt, altH)
            gedreht = Bildlupe.drehenLohnt(bild.width, bild.height, b, h)
            eingepasst = if (gedreht) {
                Bildlupe.einpassung(bild.height, bild.width, b, h)
            } else {
                Bildlupe.einpassung(bild.width, bild.height, b, h)
            }
            massstab = eingepasst
            haltGrenzen()
        }

        private fun setzeMassstab(gewuenscht: Float, brennpunktX: Float, brennpunktY: Float) {
            val vorher = massstab
            massstab = Bildlupe.begrenzeMassstab(gewuenscht, eingepasst)
            // Der Punkt unter dem Finger bleibt unter dem Finger.
            val faktor = massstab / vorher
            verschobenX = brennpunktX - (brennpunktX - verschobenX) * faktor
            verschobenY = brennpunktY - (brennpunktY - verschobenY) * faktor
            haltGrenzen()
            invalidate()
        }

        private fun haltGrenzen() {
            verschobenX = Bildlupe.begrenzeVerschiebung(verschobenX, inhaltBreite, width.toFloat())
            verschobenY = Bildlupe.begrenzeVerschiebung(verschobenY, inhaltHoehe, height.toFloat())
        }

        @Suppress("ClickableViewAccessibility")
        override fun onTouchEvent(e: MotionEvent): Boolean {
            zoomer.onTouchEvent(e)
            // Waehrend zwei Finger zoomen, wird nicht zusaetzlich geschoben --
            // sonst wandert das Bild bei jeder Zoomgeste mit davon.
            if (!zoomer.isInProgress) gesten.onTouchEvent(e)
            return true
        }

        override fun onDraw(leinwand: Canvas) {
            matrix.reset()
            if (gedreht) {
                // GEGEN den Uhrzeigersinn gedreht: Damit liegt die OBERKANTE
                // des Bildes an der LINKEN Kante des Schirms, und man liest
                // es, indem man das Telefon nach RECHTS kippt. Das ist die
                // Richtung, in die ein Telefon in der rechten Hand von selbst
                // geht -- andersherum muesste man das Handgelenk verdrehen.
                // Am 04.08.2026 am Geraet beide Richtungen angesehen.
                matrix.postRotate(-90f)
                matrix.postTranslate(0f, bild.width.toFloat())
            }
            matrix.postScale(massstab, massstab)
            matrix.postTranslate(verschobenX, verschobenY)
            leinwand.drawColor(Color.TRANSPARENT)
            leinwand.drawBitmap(bild, matrix, stift)
        }
    }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val STUFE = 1.5f
    }
}
