package org.compasszero.content

import kotlinx.serialization.Serializable

@Serializable
class Material(val item: String, val amount: String = "", val note: String = "")

@Serializable
class GuideStep(val text: String, val image: String = "", val warning: String = "")

@Serializable
class BuildGuide(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val materials: List<Material> = emptyList(),
    val tools: List<String> = emptyList(),
    val steps: List<GuideStep>,
    val difficulty: Int,
    val sources: List<SourceRef>,
    // Die Dringlichkeitsfelder der Startseite -- siehe Situations. Steht ganz
    // hinten, damit jeder bestehende Aufruf gueltig bleibt.
    val situations: List<String> = emptyList(),
)

/**
 * Die Untergruppen der Bauanleitungen. Ihre Kennungen sind die Werte, die in
 * [BuildGuide.category] stehen -- eine Anleitung bekommt KEIN zweites
 * Gruppenfeld. Die Kategorie ist bei Bauanleitungen bereits die Untergruppe;
 * ein eigenes Feld daneben waere dieselbe Angabe zweimal und damit eine Stelle,
 * an der beide auseinanderlaufen koennen.
 */
@Serializable
class GuidesFile(
    val schema: Int,
    val guides: List<BuildGuide>,
    val groups: List<ContentGroup> = emptyList(),
)

internal object GuidesCheck {

    fun validate(file: GuidesFile, problems: ProblemLog) {
        val where = "content/guides.json"
        if (file.schema != 1) {
            problems.fatal("schema-unsupported", where, "schema ${file.schema}")
            return
        }
        if (file.guides.size > ContentLimits.MAX_ITEMS_PER_FILE) {
            problems.fatal("too-many-items", where, "${file.guides.size}")
            return
        }
        Checks.uniqueIds(file.guides.map { it.id }, where, problems)
        val gruppen = GroupCheck.validateGroups(file.groups, where, problems)
        for (guide in file.guides) {
            val at = "$where#${guide.id.take(100)}"
            Checks.id(guide.id, at, problems)
            Checks.title(guide.title, at, problems)
            Checks.category(guide.category, at, problems)
            // Nur pruefen, wenn die Datei ueberhaupt Gruppen erklaert: ein
            // aelteres Paket ohne Gruppenliste bleibt gueltig und zeigt die
            // Anleitungen wie bisher am Stueck.
            if (file.groups.isNotEmpty()) {
                GroupCheck.pruefeZugehoerigkeit(guide.category, gruppen, at, problems)
            }
            Checks.text(guide.summary, ContentLimits.MAX_SUMMARY_LENGTH, "summary-invalid", at, problems, absaetzeErlaubt = true)
            validateMaterials(guide.materials, at, problems)
            validateTools(guide.tools, at, problems)
            validateSteps(guide.steps, at, problems)
            if (guide.difficulty !in 1..3) {
                problems.fatal("difficulty-invalid", at, "${guide.difficulty}")
            }
            Checks.sources(guide.sources, at, problems)
            Checks.situations(guide.situations, at, problems)
        }
    }

    // Eine Anleitung ohne Materialliste ist erlaubt: die stabile Seitenlage
    // braucht nichts als zwei Haende und waere als Ablauf mit echten Schritten
    // besser aufgehoben als in aufgeteiltem Fliesstext. Die Schritte bleiben
    // Pflicht -- sie sind der Inhalt.
    private fun validateMaterials(materials: List<Material>, at: String, problems: ProblemLog) {
        if (materials.size > ContentLimits.MAX_MATERIALS) {
            problems.fatal("materials-too-many", at, "${materials.size}")
        }
        for (material in materials) {
            if (material.item.length > ContentLimits.MAX_NAME_LENGTH || !Texts.isUsable(material.item, mindestens = 1)) {
                problems.fatal("material-invalid", at, material.item.take(100))
                continue
            }
            // Mengenangabe und Hinweis sind optional, muessen aber lesbar sein,
            // wenn sie da sind: "500 g" mit umgedrehter Schreibrichtung zeigt "g 005".
            Checks.optionalText(material.amount, ContentLimits.MAX_CATEGORY_LENGTH, "material-invalid", at, problems, mindestens = 1)
            Checks.optionalText(material.note, ContentLimits.MAX_NOTE_LENGTH, "material-invalid", at, problems)
        }
    }

    private fun validateTools(tools: List<String>, at: String, problems: ProblemLog) {
        if (tools.size > ContentLimits.MAX_TOOLS) {
            problems.fatal("tools-too-many", at, "${tools.size}")
        }
        for (tool in tools) {
            if (tool.length > ContentLimits.MAX_TOOL_LENGTH || !Texts.isUsable(tool, mindestens = 1)) {
                problems.fatal("tool-invalid", at, tool.take(100))
            }
        }
    }

    private fun validateSteps(steps: List<GuideStep>, at: String, problems: ProblemLog) {
        if (steps.isEmpty()) {
            problems.fatal("steps-missing", at, "a build guide needs at least one step")
            return
        }
        if (steps.size > ContentLimits.MAX_STEPS) {
            problems.fatal("steps-too-many", at, "${steps.size}")
        }
        for (step in steps) {
            Checks.text(step.text, ContentLimits.MAX_STEP_LENGTH, "step-invalid", at, problems, absaetzeErlaubt = true)
            Checks.assetRef(step.image, at, problems)
            // Der Warnhinweis ist das Sicherheitsfeld einer Anleitung. Eine leere
            // Warnzeile aus unsichtbaren Zeichen laesst die Anleitung vollstaendig
            // aussehen und verschweigt die Gefahr.
            Checks.optionalText(step.warning, ContentLimits.MAX_NOTE_LENGTH, "warning-invalid", at, problems)
        }
    }
}
