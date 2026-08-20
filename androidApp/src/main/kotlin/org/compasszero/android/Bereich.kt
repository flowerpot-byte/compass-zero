package org.compasszero.android

import android.app.Activity
import android.view.View
import android.widget.ScrollView

// Ein Bereich ist eine der vier Ansichten aus der unteren Leiste. Jede baut ihre
// Oberflaeche selbst und bekommt sie beim Wechsel des Stils neu aufgebaut; einen
// eigenen Zustand darf sie behalten, solange er den Neubau uebersteht.
interface Bereich {

    val name: String

    val bild: Int

    val gebaut: Boolean
        get() = true

    fun baue(b: Bausteine): View

    // Antwortet mit true, wenn der Bereich die Zurueck-Taste selbst verbraucht
    // hat -- etwa weil eine Detailansicht offen war.
    fun zurueck(): Boolean = false

    // Wird aufgerufen, wenn der Bereich in der unteren Leiste erneut angetippt
    // wird, waehrend er schon aktiv ist. Ein Lexikon mit offener Suche soll dann
    // zu den Kacheln zurueckspringen statt untaetig zu bleiben.
    fun aufAnfang() = Unit

    // Ein Bereich, der etwas laufen laesst -- einen Sensor etwa --, schaltet es
    // hier ab. Aufgerufen wird das beim Wechsel zu einem anderen Bereich und
    // wenn die App in den Hintergrund geht. Ohne diese beiden Stellen liefe der
    // Magnetfeldsensor weiter, waehrend jemand im Lexikon liest, und zoege
    // Strom fuer nichts.
    fun anhalten() = Unit

    fun fortsetzen() = Unit
}

// Ein Bereich, den es noch nicht gibt. Er sagt das gerade heraus, statt eine
// leere Flaeche zu zeigen oder gar nicht erst auf einen Tipp zu reagieren.
class UngebauterBereich(
    private val gastgeber: Activity,
    override val name: String,
    override val bild: Int,
    private val geplant: String,
) : Bereich {

    override val gebaut = false

    override fun baue(b: Bausteine): View {
        val spalte = b.spalte().apply {
            setPadding(0, b.stil.abstand * 2, 0, b.stil.abstand)
        }
        spalte.addView(b.ueberschrift("$name — noch nicht gebaut"), b.breit())
        spalte.addView(b.fliesstext(geplant), b.breit())
        return ScrollView(gastgeber).apply {
            addView(spalte)
            setBackgroundColor(b.stil.hintergrund)
        }
    }
}
