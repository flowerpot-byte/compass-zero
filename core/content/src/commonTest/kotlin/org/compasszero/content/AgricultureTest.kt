package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgricultureTest {

    private fun chapter(
        id: String = "beispiel-kapitel",
        title: String = "Beispielkapitel",
        sections: List<Section> = listOf(Section("Beispielabschnitt", "Beispieltext.")),
        sources: List<SourceRef> = listOf(SourceRef("Beispielquelle", "Abschnitt 1")),
        group: String = "",
    ) = Chapter(id, title, sections, sources, group = group)

    private fun codesOf(
        vararg chapters: Chapter,
        schema: Int = 1,
        groups: List<ContentGroup> = emptyList(),
    ): List<String> {
        val log = ProblemLog()
        AgricultureCheck.validate(AgricultureFile(schema, chapters.toList(), groups), log)
        return log.all.map { it.code }
    }

    @Test
    fun validChapterPasses() {
        assertEquals(emptyList(), codesOf(chapter()))
    }

    @Test
    fun schemaGate() {
        assertEquals(listOf("schema-unsupported"), codesOf(chapter(), schema = 3))
    }

    @Test
    fun chaptersAndSectionsAreMandatory() {
        assertTrue("chapters-missing" in codesOf())
        assertTrue("sections-missing" in codesOf(chapter(sections = emptyList())))
    }

    @Test
    fun boundsAreEnforced() {
        val manyChapters = Array(ContentLimits.MAX_CHAPTERS + 1) { chapter(id = "kapitel-$it") }
        assertTrue("too-many-items" in codesOf(*manyChapters))
        assertTrue("sections-too-many" in codesOf(
            chapter(sections = List(ContentLimits.MAX_SECTIONS + 1) { Section("h", "b") })
        ))
        assertTrue("heading-invalid" in codesOf(chapter(sections = listOf(Section(" ", "b")))))
        assertTrue("body-invalid" in codesOf(chapter(sections = listOf(Section("h", "")))))
    }

    // Ein Kapitel, dessen Gruppe es nicht gibt, faellt aus der Bereichsansicht
    // und stuende nur noch ueber die Suche -- wer den Titel nicht kennt, findet
    // es nie. Deshalb fatal und nicht nur eine Warnung.
    @Test
    fun unbekannteGruppeIstFatal() {
        val gruppen = listOf(ContentGroup("boden", "Boden, Beet und Pflege"))
        assertEquals(emptyList(), codesOf(chapter(group = "boden"), groups = gruppen))
        assertTrue("group-unknown" in codesOf(chapter(group = "gibtsnicht"), groups = gruppen))
    }

    // Ein aelteres Paket kennt gar keine Gruppen. Es muss unveraendert laden,
    // sonst waere jede Gliederung ein Bruch fuer alles schon Verteilte.
    @Test
    fun paketOhneGruppenBleibtGueltig() {
        assertEquals(emptyList(), codesOf(chapter()))
    }

    @Test
    fun doppelteGruppenkennungIstFatal() {
        val doppelt = listOf(ContentGroup("boden", "Boden"), ContentGroup("boden", "Beet"))
        assertTrue("id-duplicate" in codesOf(chapter(), groups = doppelt))
    }

    @Test
    fun assetAndSourceRules() {
        assertTrue("asset-ref-invalid" in codesOf(
            chapter(sections = listOf(Section("h", "b", image = "http://boese")))
        ))
        assertTrue("sources-missing" in codesOf(chapter(sources = emptyList())))
        assertTrue("id-duplicate" in codesOf(chapter(), chapter()))
    }
}
