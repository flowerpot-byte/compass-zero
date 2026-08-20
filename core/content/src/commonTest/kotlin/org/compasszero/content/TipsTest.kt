package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TipsTest {

    private val source = SourceRef("Beispielquelle", "Abschnitt 1")

    private fun tip(
        id: String = "beispiel-tipp",
        title: String = "Beispieltitel",
        category: String = "wasser",
        body: String = "Beispieltext ohne Fachinhalt.",
        keywords: List<String> = listOf("beispiel"),
        group: String = "",
        sources: List<SourceRef> = listOf(source),
    ) = Tip(id, title, category, body, keywords, sources, group)

    private fun codesOf(
        vararg tips: Tip,
        schema: Int = 1,
        groups: List<TipGroup> = emptyList(),
    ): List<String> {
        val log = ProblemLog()
        TipsCheck.validate(TipsFile(schema, tips.toList(), groups), log)
        return log.all.map { it.code }
    }

    @Test
    fun validTipsPass() {
        assertEquals(emptyList(), codesOf(tip(), tip(id = "zweiter-tipp")))
    }

    @Test
    fun schemaGate() {
        assertEquals(listOf("schema-unsupported"), codesOf(tip(), schema = 2))
    }

    @Test
    fun missingSourcesAreFatal() {
        assertTrue("sources-missing" in codesOf(tip(sources = emptyList())))
    }

    @Test
    fun blankSourceNameIsFatal() {
        assertTrue("source-invalid" in codesOf(tip(sources = listOf(SourceRef("  ", "Abschnitt 1")))))
    }

    // Ohne Belegangabe ist die Quelle nur der Name einer Organisation und die
    // Aussage nicht mehr nachzuschlagen.
    @Test
    fun quelleOhneBelegangabeIstFatal() {
        assertTrue("source-detail-missing" in codesOf(tip(sources = listOf(SourceRef("Beispielquelle", "")))))
        assertTrue("source-invalid" in codesOf(tip(sources = listOf(SourceRef("Beispielquelle", " ")))))
        assertTrue(
            "source-invalid" in codesOf(
                tip(sources = listOf(SourceRef("Beispielquelle", "x".repeat(ContentLimits.MAX_NOTE_LENGTH + 1))))
            )
        )
    }

    @Test
    fun idRules() {
        assertTrue("id-invalid" in codesOf(tip(id = "Grossbuchstaben")))
        assertTrue("id-invalid" in codesOf(tip(id = "-fuehrender-strich")))
        assertTrue("id-invalid" in codesOf(tip(id = "a".repeat(81))))
        assertTrue("id-duplicate" in codesOf(tip(), tip()))
    }

    @Test
    fun fieldRules() {
        assertTrue("title-invalid" in codesOf(tip(title = " ")))
        assertTrue("category-invalid" in codesOf(tip(category = "Wasser & Feuer")))
        assertTrue("body-invalid" in codesOf(tip(body = "")))
        assertTrue("body-invalid" in codesOf(tip(body = "x".repeat(ContentLimits.MAX_BODY_LENGTH + 1))))
        assertTrue("keyword-invalid" in codesOf(tip(keywords = listOf(" "))))
        assertTrue("keywords-too-many" in codesOf(tip(keywords = List(21) { "k$it" })))
    }

    @Test
    fun tooManyTipsAreFatal() {
        val many = Array(ContentLimits.MAX_ITEMS_PER_FILE + 1) { tip(id = "tipp-$it") }
        assertTrue("too-many-items" in codesOf(*many))
    }

    // Themengruppen ordnen nur die Ansicht. Ein Paket ohne sie bleibt gueltig --
    // sonst waere jedes bereits verteilte Paket mit einem Schlag ungueltig.
    @Test
    fun tippsOhneGruppeBleibenGueltig() {
        assertEquals(emptyList(), codesOf(tip(), tip(id = "zweiter-tipp")))
    }

    @Test
    fun gueltigeGruppenZuordnungPasst() {
        val gruppe = TipGroup("wasser-aufbereiten", "Wasser aufbereiten", "wasser")
        assertEquals(emptyList(), codesOf(tip(group = "wasser-aufbereiten"), groups = listOf(gruppe)))
    }

    // Eine Gruppe, die es nicht gibt, laesst den Tipp aus der Kategorie-Ansicht
    // fallen: auffindbar bliebe er nur noch ueber die Suche.
    @Test
    fun unbekannteGruppeIstFatal() {
        assertTrue("group-unknown" in codesOf(tip(group = "gibt-es-nicht")))
    }

    // Und eine Gruppe aus einer fremden Kategorie setzte den Tipp unter eine
    // Ueberschrift, die etwas anderes ankuendigt.
    @Test
    fun gruppeAusFremderKategorieIstFatal() {
        val gruppe = TipGroup("eh-blutung", "Starke Blutung", "erste-hilfe")
        assertTrue(
            "group-category-mismatch" in codesOf(
                tip(category = "wasser", group = "eh-blutung"),
                groups = listOf(gruppe),
            ),
        )
    }

    @Test
    fun gruppenRegelnWieBeiTipps() {
        val doppelt = listOf(
            TipGroup("eh-blutung", "Starke Blutung", "erste-hilfe"),
            TipGroup("eh-blutung", "Noch einmal", "erste-hilfe"),
        )
        assertTrue("id-duplicate" in codesOf(tip(), groups = doppelt))
        assertTrue("id-invalid" in codesOf(tip(), groups = listOf(TipGroup("Gross", "Titel", "wasser"))))
        assertTrue("title-invalid" in codesOf(tip(), groups = listOf(TipGroup("gruppe", " ", "wasser"))))
        assertTrue("category-invalid" in codesOf(tip(), groups = listOf(TipGroup("gruppe", "Titel", "Gross"))))
        val viele = List(ContentLimits.MAX_TIP_GROUPS + 1) { TipGroup("g$it", "Titel", "wasser") }
        assertTrue("too-many-items" in codesOf(tip(), groups = viele))
    }

    @Test
    fun problemsCarryLocation() {
        val log = ProblemLog()
        TipsCheck.validate(TipsFile(1, listOf(tip(id = "kaputt!", title = " "))), log)
        assertTrue(log.all.all { it.where.contains("tips") })
        assertEquals(2, log.all.size)
    }
}
