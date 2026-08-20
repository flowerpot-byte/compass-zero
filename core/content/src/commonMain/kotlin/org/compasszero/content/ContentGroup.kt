package org.compasszero.content

import kotlinx.serialization.Serializable

/**
 * Eine Untergruppe innerhalb eines Bereichs -- die Zwischenueberschrift, unter
 * der Bauanleitungen und Agrikultur-Kapitel in der Liste stehen.
 *
 * Bewusst OHNE Kategoriefeld, anders als [TipGroup]: Tipps verteilen sich ueber
 * mehrere Kategorien (Erste Hilfe, Medizin, Wasser ...), deshalb muss dort jede
 * Gruppe sagen, wohin sie gehoert. Bauanleitungen und Agrikultur sind jeweils
 * schon EIN Bereich; eine Kategorie danebenzustellen waere ein zweites Feld mit
 * immer demselben Wert -- und damit eine Stelle, an der zwei Angaben
 * auseinanderlaufen koennen.
 *
 * Die Reihenfolge der Gruppen ist die Reihenfolge dieser Liste. Sie wird von
 * Hand gesetzt: was zuerst gebraucht wird, steht oben. Weder aus dem Namen noch
 * aus der Zahl der Eintraege liesse sich das ableiten.
 */
@Serializable
class ContentGroup(val id: String, val title: String)

internal object GroupCheck {

    /**
     * Prueft die Gruppenliste einer Datei und gibt die bekannten Kennungen
     * zurueck. Doppelte Kennungen sind fatal: welche Gruppe dann gewinnt, waere
     * Zufall der Reihenfolge, und Eintraege verschwaenden unter der falschen
     * Ueberschrift.
     */
    fun validateGroups(groups: List<ContentGroup>, where: String, problems: ProblemLog): Set<String> {
        if (groups.size > ContentLimits.MAX_TIP_GROUPS) {
            problems.fatal("too-many-items", where, "${groups.size} Gruppen")
            return emptySet()
        }
        Checks.uniqueIds(groups.map { it.id }, where, problems)
        for (gruppe in groups) {
            val at = "$where#$GRUPPE${gruppe.id.take(100)}"
            Checks.id(gruppe.id, at, problems)
            Checks.title(gruppe.title, at, problems)
        }
        return groups.map { it.id }.toSet()
    }

    /**
     * Eine Gruppe, die es nicht gibt, wuerde den Eintrag aus der Bereichsansicht
     * fallen lassen -- er stuende dann nur noch ueber die Suche. In einem
     * Notfallhandbuch ist ein Eintrag, den man nur findet, wenn man schon weiss,
     * wie er heisst, kein Schoenheitsfehler. Deshalb fatal.
     *
     * Leer bleibt erlaubt und heisst "keine Gruppe".
     */
    fun pruefeZugehoerigkeit(group: String, bekannt: Set<String>, at: String, problems: ProblemLog) {
        if (group.isEmpty()) return
        if (group !in bekannt) {
            problems.fatal("group-unknown", at, group.take(100))
        }
    }

    private const val GRUPPE = "gruppe:"
}
