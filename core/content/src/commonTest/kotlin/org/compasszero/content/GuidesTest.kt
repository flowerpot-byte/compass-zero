package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuidesTest {

    private fun guide(
        id: String = "beispiel-anleitung",
        title: String = "Beispielanleitung",
        category: String = "werkzeug",
        summary: String = "Kurzbeschreibung.",
        materials: List<Material> = listOf(Material("Beispielmaterial", "2 Stueck")),
        tools: List<String> = listOf("Beispielwerkzeug"),
        steps: List<GuideStep> = listOf(GuideStep("Erster Schritt.", image = "assets/schritt1.png")),
        difficulty: Int = 2,
        sources: List<SourceRef> = listOf(SourceRef("Beispielquelle", "Abschnitt 1")),
    ) = BuildGuide(id, title, category, summary, materials, tools, steps, difficulty, sources)

    private fun codesOf(
        vararg guides: BuildGuide,
        schema: Int = 1,
        groups: List<ContentGroup> = emptyList(),
    ): List<String> {
        val log = ProblemLog()
        GuidesCheck.validate(GuidesFile(schema, guides.toList(), groups), log)
        return log.all.map { it.code }
    }

    // Bei Bauanleitungen IST die Kategorie die Untergruppe. Erklaert die Datei
    // Gruppen, muss jede benutzte Kategorie eine haben -- sonst faellt die
    // Anleitung aus der Bereichsansicht und ist nur noch ueber die Suche zu
    // finden.
    @Test
    fun kategorieOhneGruppeIstFatal() {
        val gruppen = listOf(ContentGroup("werkzeug", "Werkzeug herstellen und instand halten"))
        assertEquals(emptyList(), codesOf(guide(category = "werkzeug"), groups = gruppen))
        assertTrue("group-unknown" in codesOf(guide(category = "feuer"), groups = gruppen))
    }

    // Ohne Gruppenliste bleibt alles wie bisher: ein aelteres Paket muss
    // unveraendert laden.
    @Test
    fun paketOhneGruppenBleibtGueltig() {
        assertEquals(emptyList(), codesOf(guide(category = "feuer")))
    }

    @Test
    fun validGuidePasses() {
        assertEquals(emptyList(), codesOf(guide()))
    }

    @Test
    fun schemaGate() {
        assertEquals(listOf("schema-unsupported"), codesOf(guide(), schema = 0))
    }

    @Test
    fun stepsAreMandatory() {
        assertTrue("steps-missing" in codesOf(guide(steps = emptyList())))
        assertTrue("step-invalid" in codesOf(guide(steps = listOf(GuideStep(" ")))))
        assertTrue("material-invalid" in codesOf(guide(materials = listOf(Material(" ")))))
    }

    // Eine Anleitung ohne Materialliste ist erlaubt; steht eine da, gilt sie
    // weiter unveraendert.
    @Test
    fun anleitungOhneMaterialListeIstErlaubt() {
        assertEquals(emptyList(), codesOf(guide(materials = emptyList())))
    }

    @Test
    fun boundsAreEnforced() {
        assertTrue("steps-too-many" in codesOf(guide(steps = List(ContentLimits.MAX_STEPS + 1) { GuideStep("s") })))
        assertTrue("materials-too-many" in codesOf(guide(materials = List(ContentLimits.MAX_MATERIALS + 1) { Material("m") })))
        assertTrue("tools-too-many" in codesOf(guide(tools = List(ContentLimits.MAX_TOOLS + 1) { "w" })))
        assertTrue("tool-invalid" in codesOf(guide(tools = listOf("x".repeat(ContentLimits.MAX_TOOL_LENGTH + 1)))))
        assertTrue("difficulty-invalid" in codesOf(guide(difficulty = 0)))
        assertTrue("difficulty-invalid" in codesOf(guide(difficulty = 4)))
        assertTrue("summary-invalid" in codesOf(guide(summary = "x".repeat(ContentLimits.MAX_SUMMARY_LENGTH + 1))))
    }

    @Test
    fun assetReferencesAreChecked() {
        val schritt = "Erster Schritt."
        assertTrue("asset-ref-invalid" in codesOf(guide(steps = listOf(GuideStep(schritt, image = "../boese.png")))))
        assertTrue("asset-ref-invalid" in codesOf(guide(steps = listOf(GuideStep(schritt, image = "assets/..")))))
        assertEquals(emptyList(), codesOf(guide(steps = listOf(GuideStep(schritt, image = "")))))
    }

    @Test
    fun unreadableStepTextIsRejected() {
        assertTrue("step-invalid" in codesOf(guide(steps = listOf(GuideStep("​")))))
        assertTrue("material-invalid" in codesOf(guide(materials = listOf(Material("​")))))
    }

    @Test
    fun sourcesAreMandatory() {
        assertTrue("sources-missing" in codesOf(guide(sources = emptyList())))
    }

    @Test
    fun duplicateIdsAreFatal() {
        assertTrue("id-duplicate" in codesOf(guide(), guide()))
    }

    @Test
    fun warningTextBounded() {
        assertTrue("warning-invalid" in codesOf(
            guide(steps = listOf(GuideStep("s", warning = "x".repeat(ContentLimits.MAX_NOTE_LENGTH + 1))))
        ))
    }

    @Test
    fun warnhinweisUndMengeMuessenLesbarSein() {
        val unsichtbar = 0x200B.toChar().toString().repeat(3)
        assertTrue("warning-invalid" in codesOf(guide(steps = listOf(GuideStep("Erster Schritt.", warning = unsichtbar)))))
        assertTrue("material-invalid" in codesOf(guide(materials = listOf(Material("Holz", amount = unsichtbar)))))
        assertTrue("material-invalid" in codesOf(guide(materials = listOf(Material("Holz", note = unsichtbar)))))
        // Umgedrehte Schreibrichtung wuerde "500 g" als "g 005" anzeigen.
        val umgedreht = 0x202E.toChar() + "500 g"
        assertTrue("material-invalid" in codesOf(guide(materials = listOf(Material("Holz", amount = umgedreht)))))
        // Kurze, echte Mengenangaben bleiben erlaubt.
        assertEquals(emptyList(), codesOf(guide(materials = listOf(Material("Holz", amount = "5 m", note = "trocken lagern")))))
    }
}
