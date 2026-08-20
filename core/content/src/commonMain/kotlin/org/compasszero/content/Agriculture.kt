package org.compasszero.content

import kotlinx.serialization.Serializable

@Serializable
class Section(val heading: String, val body: String, val image: String = "")

@Serializable
class Chapter(
    val id: String,
    val title: String,
    val sections: List<Section>,
    val sources: List<SourceRef>,
    // Die Dringlichkeitsfelder der Startseite -- siehe Situations.
    val situations: List<String> = emptyList(),
    // Die Untergruppe innerhalb der Agrikultur. Steht ganz hinten, damit jeder
    // bestehende Aufruf gueltig bleibt und ein aelteres Paket ohne Gruppen
    // unveraendert laedt: leer heisst "keine Gruppe", die Ansicht haengt solche
    // Kapitel hinten an.
    val group: String = "",
)

@Serializable
class AgricultureFile(
    val schema: Int,
    val chapters: List<Chapter>,
    val groups: List<ContentGroup> = emptyList(),
)

internal object AgricultureCheck {

    fun validate(file: AgricultureFile, problems: ProblemLog) {
        val where = "content/agriculture.json"
        if (file.schema != 1) {
            problems.fatal("schema-unsupported", where, "schema ${file.schema}")
            return
        }
        if (file.chapters.isEmpty()) {
            problems.fatal("chapters-missing", where, "guide has no chapters")
            return
        }
        if (file.chapters.size > ContentLimits.MAX_CHAPTERS) {
            problems.fatal("too-many-items", where, "${file.chapters.size}")
            return
        }
        Checks.uniqueIds(file.chapters.map { it.id }, where, problems)
        val gruppen = GroupCheck.validateGroups(file.groups, where, problems)
        for (chapter in file.chapters) {
            val at = "$where#${chapter.id.take(100)}"
            Checks.id(chapter.id, at, problems)
            Checks.title(chapter.title, at, problems)
            validateSections(chapter.sections, at, problems)
            Checks.sources(chapter.sources, at, problems)
            Checks.situations(chapter.situations, at, problems)
            GroupCheck.pruefeZugehoerigkeit(chapter.group, gruppen, at, problems)
        }
    }

    private fun validateSections(sections: List<Section>, at: String, problems: ProblemLog) {
        if (sections.isEmpty()) {
            problems.fatal("sections-missing", at, "chapter has no sections")
            return
        }
        if (sections.size > ContentLimits.MAX_SECTIONS) {
            problems.fatal("sections-too-many", at, "${sections.size}")
        }
        for (section in sections) {
            Checks.benennendesFeld(section.heading, ContentLimits.MAX_TITLE_LENGTH, "heading-invalid", at, problems)
            Checks.text(section.body, ContentLimits.MAX_BODY_LENGTH, "body-invalid", at, problems, absaetzeErlaubt = true)
            Checks.assetRef(section.image, at, problems)
        }
    }
}
