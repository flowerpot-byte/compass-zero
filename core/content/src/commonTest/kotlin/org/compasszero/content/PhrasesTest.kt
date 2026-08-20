package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhrasesTest {

    private val source = SourceRef("Beispielquelle", "Abschnitt 1")
    private val sprachen = listOf("de", "en", "fr")

    private fun phrase(
        id: String = "ja-nein",
        text: Map<String, String> = mapOf(
            "de" to "Ja. Nein. Ich weiss nicht.",
            "en" to "Yes. No. Don't know.",
            "fr" to "Oui. Non. Je ne sais pas.",
        ),
        note: Map<String, String> = emptyMap(),
    ) = Phrase(id, text, note)

    private fun group(
        id: String = "grunddaten",
        title: Map<String, String> = mapOf("de" to "Zur Person", "en" to "About you", "fr" to "Vous concernant"),
        sources: List<SourceRef> = listOf(source),
        phrases: List<Phrase> = listOf(phrase()),
    ) = PhraseGroup(id, title, sources, phrases)

    private fun codesOf(vararg groups: PhraseGroup, schema: Int = 1, languages: List<String> = sprachen): List<String> {
        val log = ProblemLog()
        PhrasesCheck.validate(PhrasesFile(schema, languages, groups.toList()), log)
        return log.all.map { it.code }
    }

    @Test
    fun validMinimalFilePasses() {
        assertEquals(emptyList(), codesOf(group()))
    }

    @Test
    fun schemaGate() {
        assertEquals(listOf("schema-unsupported"), codesOf(group(), schema = 2))
    }

    @Test
    fun missingLanguageInTextIsFatal() {
        val unvollstaendig = phrase(text = mapOf("de" to "Ja.", "en" to "Yes."))
        assertTrue("phrase-language-missing" in codesOf(group(phrases = listOf(unvollstaendig))))
    }

    @Test
    fun missingLanguageInTitleIsFatal() {
        val unvollstaendig = group(title = mapOf("de" to "Titel", "en" to "Title"))
        assertTrue("title-language-missing" in codesOf(unvollstaendig))
    }

    // Unsichtbare Zeichen wie ein Nullbreiten-Leerzeichen taeuschen eine
    // ausgefuellte Uebersetzung nur vor.
    @Test
    fun unreadableTextIsFatal() {
        val unsichtbar = 0x200B.toChar().toString()
        val kaputt = phrase(text = mapOf("de" to unsichtbar, "en" to "Yes.", "fr" to "Oui."))
        assertTrue("phrase-invalid" in codesOf(group(phrases = listOf(kaputt))))
    }

    @Test
    fun unreadableTitleIsFatal() {
        val unsichtbar = 0x200B.toChar().toString()
        val kaputt = group(title = mapOf("de" to unsichtbar, "en" to "Title", "fr" to "Titre"))
        assertTrue("title-invalid" in codesOf(kaputt))
    }

    @Test
    fun duplicatePhraseIdAcrossGroupsIsFatal() {
        val a = group(id = "gruppe-a", phrases = listOf(phrase(id = "doppelt")))
        val b = group(id = "gruppe-b", phrases = listOf(phrase(id = "doppelt")))
        assertTrue("id-duplicate" in codesOf(a, b))
    }

    @Test
    fun duplicateGroupIdIsFatal() {
        assertTrue("id-duplicate" in codesOf(group(), group()))
    }

    @Test
    fun emptyLanguagesIsFatal() {
        assertEquals(listOf("languages-empty"), codesOf(group(), languages = emptyList()))
    }

    @Test
    fun duplicateLanguagesIsFatal() {
        assertTrue("languages-duplicate" in codesOf(group(), languages = listOf("de", "de", "en")))
    }

    @Test
    fun invalidLanguageCodeIsFatal() {
        assertTrue("language-invalid" in codesOf(group(), languages = listOf("Deutsch!", "en", "fr")))
    }

    @Test
    fun tooLongTextIsFatal() {
        val lang = phrase(
            text = mapOf("de" to "x".repeat(ContentLimits.MAX_PHRASE_LENGTH + 1), "en" to "Yes.", "fr" to "Oui."),
        )
        assertTrue("phrase-invalid" in codesOf(group(phrases = listOf(lang))))
    }

    @Test
    fun groupWithoutSourceIsFatal() {
        assertTrue("sources-missing" in codesOf(group(sources = emptyList())))
    }

    @Test
    fun tooManyGroupsIsFatal() {
        val viele = Array(ContentLimits.MAX_PHRASE_GROUPS + 1) { group(id = "gruppe-$it") }
        assertTrue("too-many-items" in codesOf(*viele))
    }

    @Test
    fun tooManyPhrasesTotalIsFatal() {
        val vielePhrasen = (0..ContentLimits.MAX_PHRASES_PER_FILE).map { phrase(id = "phrase-$it") }
        assertTrue("too-many-items" in codesOf(group(phrases = vielePhrasen)))
    }

    // Ein Sprachschluessel ausserhalb der deklarierten Liste ist kein Fehler,
    // nur ein Hinweis -- Vorwaertskompatibilitaet fuer neuere Katalogfassungen.
    @Test
    fun undeclaredLanguageIsWarningOnly() {
        val log = ProblemLog()
        val mitZusatzsprache = phrase(
            text = mapOf("de" to "Ja.", "en" to "Yes.", "fr" to "Oui.", "es" to "Si."),
        )
        PhrasesCheck.validate(PhrasesFile(1, sprachen, listOf(group(phrases = listOf(mitZusatzsprache)))), log)
        assertEquals(listOf("language-undeclared"), log.all.map { it.code })
        assertTrue(log.all.single().severity == Severity.Warning)
    }

    @Test
    fun undeclaredLanguageInNoteIsWarningOnly() {
        val log = ProblemLog()
        val mitNotiz = phrase(note = mapOf("de" to "Zum Zeigen.", "es" to "Para mostrar."))
        PhrasesCheck.validate(PhrasesFile(1, sprachen, listOf(group(phrases = listOf(mitNotiz)))), log)
        assertEquals(listOf("language-undeclared"), log.all.map { it.code })
    }

    @Test
    fun idRules() {
        assertTrue("id-invalid" in codesOf(group(id = "Grossbuchstaben")))
        assertTrue("id-invalid" in codesOf(group(phrases = listOf(phrase(id = "-fuehrender-strich")))))
    }

    // Manifest- und Paket-Verdrahtung: gleiche Behandlung wie bei den uebrigen
    // Inhaltsarten (siehe PackParserTest fuer tips/guides/agriculture/pois).
    @Test
    fun manifestWithPhrasesKindAndFileLoads() {
        val manifest =
            """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["phrases"]}"""
        val phrasesJson = """{"schema":1,"languages":["de","en"],"groups":[{"id":"grunddaten",""" +
            """"title":{"de":"Zur Person","en":"About you"},"sources":[{"name":"Quelle","detail":"Abschnitt 1"}],""" +
            """"phrases":[{"id":"ja-nein","text":{"de":"Ja. Nein.","en":"Yes. No."}}]}]}"""
        val files = mapOf(
            "manifest.json" to manifest.encodeToByteArray(),
            "content/phrases.json" to phrasesJson.encodeToByteArray(),
        )
        val result = PackParser.parse(files, emptySet())
        val pack = assertNotNull(result.pack, result.problems.map { it.code }.toString())
        assertEquals(emptyList(), result.problems)
        assertEquals(listOf("grunddaten"), pack.phrases.map { it.id })
        assertEquals(listOf("de", "en"), pack.phraseLanguages)
    }

    @Test
    fun manifestWithPhrasesKindButNoFileIsFatal() {
        val manifest =
            """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["phrases"]}"""
        val files = mapOf("manifest.json" to manifest.encodeToByteArray())
        val result = PackParser.parse(files, emptySet())
        assertNull(result.pack)
        assertTrue(result.problems.any { it.code == "content-missing" && it.where == "content/phrases.json" })
    }

    // Phrasen wie "Haben Sie Schmerzen?" duerfen die Notfall-Treffer der Tipps
    // in der Suche nicht verdraengen -- deshalb landen sie nicht im Index.
    @Test
    fun phrasesStayOutOfSearchIndex() {
        val manifest = PackManifest(1, "org.compasszero.test", 1, "de", "Testpaket", 0, listOf("tips", "phrases"))
        val pack = LoadedPack(
            manifest = manifest,
            tips = listOf(
                Tip("wasser-abkochen", "Wasser abkochen", "wasser", "Beispieltext.", listOf("trinkwasser"), listOf(source)),
            ),
            phrases = listOf(
                group(
                    phrases = listOf(
                        phrase(text = mapOf("de" to "Haben Sie Schmerzen?", "en" to "Are you in pain?", "fr" to "Avez-vous mal?")),
                    ),
                ),
            ),
            phraseLanguages = sprachen,
        )
        val index = SearchIndex.build(pack)
        assertEquals(emptyList(), index.search("schmerzen"))
        assertEquals(listOf("wasser-abkochen"), index.search("trinkwasser").map { it.id })
    }
}
