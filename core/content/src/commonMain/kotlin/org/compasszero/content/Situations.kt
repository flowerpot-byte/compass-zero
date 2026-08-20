package org.compasszero.content

/**
 * Die sechs Dringlichkeitsfelder der Startseite.
 *
 * WARUM ES SIE GIBT: Zwei Kategorien trugen 221 der 315 Tipps. Wer "Erste
 * Hilfe" antippte, bekam eine Liste mit 137 Eintraegen -- vom Notruf ueber die
 * Seitenlage bis zur Geburt. Das ist keine Gliederung, das ist ein Stapel, und
 * kein Rand und keine Schrift macht 137 gleichrangige Zeilen uebersichtlich.
 *
 * WARUM NEBEN [Tip.category] UND NICHT STATT DESSEN (entschieden am
 * 04.08.2026, Variante B): `category` ist EIN String, ein Tipp gehoert damit
 * zu genau EINER Kategorie. Sechs Dringlichkeitsfelder wuerden bei jedem Tipp
 * eine Entweder-oder-Entscheidung erzwingen, die es sachlich nicht gibt -- wer
 * blutet, ist "jetzt sofort" UND "verletzt". Ausserdem traegt jede
 * Themengruppe selbst eine Kategorie; weicht sie ab, laedt das Paket gar nicht
 * mehr.
 *
 * Der ausschlaggebende Grund war die Rueckholbarkeit: Gefaellt die Ansicht
 * nicht, wird diese Liste zurueckgesetzt und kein Inhaltspaket zurueckgebaut.
 *
 * DIE REIHENFOLGE IST DIE ANZEIGEREIHENFOLGE und nach Dringlichkeit gesetzt:
 * zuerst, was in Minuten toetet; dann, was in Tagen toetet; dann, was ueber
 * Wochen entscheidet. Sie ist weder alphabetisch noch aus der Zahl der
 * Eintraege ableitbar.
 */
class Situation(val id: String, val title: String)

object Situations {

    const val JETZT_SOFORT = "jetzt-sofort"
    const val VERLETZT = "verletzt"
    const val KRANK = "krank"
    const val AM_LEBEN_BLEIBEN = "am-leben-bleiben"
    const val UNTERWEGS = "unterwegs"
    const val LAGE_UND_GEFAHR = "lage-und-gefahr"

    /**
     * Die einzige Aufzaehlung. Alles andere -- die Kacheln der App, die Pruefung
     * beim Laden eines Pakets, die Testabdeckung -- wird HIERAUS abgeleitet.
     *
     * Am 29.07.2026 sind zwei Listen auseinandergelaufen, die von Hand
     * gleichgehalten werden mussten, und sechs Tipps standen doppelt. Kein Test
     * hat es gemerkt, weil beide Listen fuer sich stimmig waren.
     */
    val ALLE: List<Situation> = listOf(
        Situation(JETZT_SOFORT, "Jetzt sofort"),
        Situation(VERLETZT, "Verletzt"),
        Situation(KRANK, "Krank"),
        Situation(AM_LEBEN_BLEIBEN, "Am Leben bleiben"),
        Situation(UNTERWEGS, "Unterwegs"),
        Situation(LAGE_UND_GEFAHR, "Lage und Gefahr"),
    )

    val KENNUNGEN: Set<String> = ALLE.map { it.id }.toSet()

    fun titel(id: String): String = ALLE.firstOrNull { it.id == id }?.title ?: id
}
