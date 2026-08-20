package org.compasszero.content

import kotlinx.serialization.Serializable

@Serializable
class Tip(
    val id: String,
    val title: String,
    val category: String,
    val body: String,
    val keywords: List<String> = emptyList(),
    val sources: List<SourceRef>,
    // Steht bewusst hinter sources: so bleibt jeder bestehende Aufruf gueltig,
    // und ein Paket ohne Gruppen laedt unveraendert.
    val group: String = "",
    // Eine Zeichnung zum Tipp, als Pfad im Paket ("assets/...").
    //
    // Bis zum 29.07.2026 konnten nur Bauanleitungs-Schritte und
    // Agrikultur-Abschnitte ein Bild tragen -- das sind acht von 221
    // Eintraegen. Gerade die Erste-Hilfe-Tipps sind aber die, bei denen eine
    // Zeichnung am meisten traegt: Eine Flaeche in Prozent oder eine Lage des
    // Koerpers laesst sich zeichnen und nur muehsam beschreiben.
    //
    // Leer heisst "kein Bild" und ist der Normalfall. Steht etwas drin, muss
    // die Datei im Paket liegen, sonst meldet der Parser "asset-missing".
    val image: String = "",
    // Die Dringlichkeitsfelder, unter denen dieser Tipp auf der Startseite
    // steht -- siehe Situations. MEHRWERTIG, und das ist der Punkt: Wer blutet,
    // gehoert unter "Jetzt sofort" UND unter "Verletzt". Steht bewusst NEBEN
    // category und ersetzt sie nicht; die Kategorie traegt weiter die
    // Themengruppen, die Tests und den fachlichen Zusammenhang.
    val situations: List<String> = emptyList(),
)

// Eine Themengruppe buendelt Tipps derselben Kategorie unter einer Zwischen-
// ueberschrift. Sie ordnet nur die Ansicht: die Suche kennt sie nicht, und ein
// Tipp ohne Gruppe bleibt gueltig. Die Reihenfolge der Gruppen ist die
// Reihenfolge in dieser Liste -- sortiert wird nach Dringlichkeit, und die
// laesst sich weder aus dem Namen noch aus der Zahl der Tipps ableiten.
@Serializable
class TipGroup(val id: String, val title: String, val category: String)

@Serializable
class TipsFile(val schema: Int, val tips: List<Tip>, val groups: List<TipGroup> = emptyList())

internal object TipsCheck {

    fun validate(file: TipsFile, problems: ProblemLog) {
        val where = "content/tips.json"
        if (file.schema != 1) {
            problems.fatal("schema-unsupported", where, "schema ${file.schema}")
            return
        }
        if (file.tips.size > ContentLimits.MAX_ITEMS_PER_FILE) {
            problems.fatal("too-many-items", where, "${file.tips.size}")
            return
        }
        if (file.groups.size > ContentLimits.MAX_TIP_GROUPS) {
            problems.fatal("too-many-items", where, "${file.groups.size} Gruppen")
            return
        }
        Checks.uniqueIds(file.tips.map { it.id }, where, problems)
        Checks.uniqueIds(file.groups.map { it.id }, where, problems)

        val kategorieDerGruppe = HashMap<String, String>()
        for (gruppe in file.groups) {
            val at = "$where#$GRUPPE${gruppe.id.take(100)}"
            Checks.id(gruppe.id, at, problems)
            Checks.title(gruppe.title, at, problems)
            Checks.category(gruppe.category, at, problems)
            kategorieDerGruppe[gruppe.id] = gruppe.category
        }

        for (tip in file.tips) {
            val at = "$where#${tip.id.take(100)}"
            Checks.id(tip.id, at, problems)
            Checks.title(tip.title, at, problems)
            Checks.category(tip.category, at, problems)
            Checks.text(tip.body, ContentLimits.MAX_BODY_LENGTH, "body-invalid", at, problems, absaetzeErlaubt = true)
            if (tip.keywords.size > ContentLimits.MAX_KEYWORDS) {
                problems.fatal("keywords-too-many", at, "${tip.keywords.size}")
            }
            for (keyword in tip.keywords) {
                Checks.benennendesFeld(keyword, ContentLimits.MAX_KEYWORD_LENGTH, "keyword-invalid", at, problems)
            }
            pruefeGruppe(tip, kategorieDerGruppe, at, problems)
            Checks.situations(tip.situations, at, problems)
            Checks.assetRef(tip.image, at, problems)
            Checks.sources(tip.sources, at, problems)
        }
    }

    // Eine Gruppe, die es nicht gibt, wuerde den Tipp aus der Kategorie-Ansicht
    // fallen lassen -- er stuende dann nur noch ueber die Suche. Und eine Gruppe
    // aus einer fremden Kategorie setzte ihn unter eine Ueberschrift, die etwas
    // anderes ankuendigt. Beides ist bei einem Notfallhandbuch kein
    // Schoenheitsfehler, deshalb sind beide Faelle fatal.
    private fun pruefeGruppe(
        tip: Tip,
        kategorieDerGruppe: Map<String, String>,
        at: String,
        problems: ProblemLog,
    ) {
        if (tip.group.isEmpty()) return
        val kategorie = kategorieDerGruppe[tip.group]
        if (kategorie == null) {
            problems.fatal("group-unknown", at, tip.group.take(100))
            return
        }
        if (kategorie != tip.category) {
            problems.fatal("group-category-mismatch", at, "${tip.group} ist $kategorie, Tipp ist ${tip.category}")
        }
    }

    private const val GRUPPE = "gruppe:"
}
