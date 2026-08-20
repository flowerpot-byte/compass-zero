package org.compasszero.content

import kotlinx.serialization.Serializable

@Serializable
class Phrase(
    val id: String,
    val text: Map<String, String>,
    val note: Map<String, String> = emptyMap(),
)

@Serializable
class PhraseGroup(
    val id: String,
    val title: Map<String, String>,
    val sources: List<SourceRef>,
    val phrases: List<Phrase>,
)

@Serializable
class PhrasesFile(val schema: Int, val languages: List<String>, val groups: List<PhraseGroup>)

// Der Phrasenkatalog fuer den Uebersetzer-Bereich: kurze, vorformulierte Saetze
// zum Zeigen, nicht zum Vorlesen. Anders als bei den uebrigen Inhaltsarten steht
// hinter jedem Feld eine ganze Sprachliste statt eines einzelnen Textes, deshalb
// pruefen die eigenen Regeln hier die Vollstaendigkeit dieser Liste, statt nur
// Laenge und Lesbarkeit eines einzelnen Feldes.
internal object PhrasesCheck {

    fun validate(file: PhrasesFile, problems: ProblemLog) {
        val where = "content/phrases.json"
        if (file.schema != 1) {
            problems.fatal("schema-unsupported", where, "schema ${file.schema}")
            return
        }
        val languages = validateLanguages(file.languages, where, problems)
        if (languages.isEmpty()) return

        if (file.groups.size > ContentLimits.MAX_PHRASE_GROUPS) {
            problems.fatal("too-many-items", where, "${file.groups.size}")
            return
        }
        val phraseCount = file.groups.sumOf { it.phrases.size }
        if (phraseCount > ContentLimits.MAX_PHRASES_PER_FILE) {
            problems.fatal("too-many-items", where, "$phraseCount")
            return
        }

        Checks.uniqueIds(file.groups.map { it.id }, where, problems)
        // Phrasen-Kennungen sind ueber die ganze Datei eindeutig, nicht nur je
        // Gruppe: eine Kennung adressiert am Ende einen Eintrag im ganzen Katalog.
        Checks.uniqueIds(file.groups.flatMap { group -> group.phrases.map { it.id } }, where, problems)

        for (group in file.groups) {
            val at = "$where#${group.id.take(100)}"
            Checks.id(group.id, at, problems)
            validateLocalized(
                group.title, languages, ContentLimits.MAX_TITLE_LENGTH,
                missingCode = "title-language-missing", invalidCode = "title-invalid",
                at, problems,
            )
            Checks.sources(group.sources, at, problems)
            for (phrase in group.phrases) {
                validatePhrase(phrase, languages, at, problems)
            }
        }
    }

    private fun validatePhrase(phrase: Phrase, languages: Set<String>, groupAt: String, problems: ProblemLog) {
        val at = "$groupAt.${phrase.id.take(100)}"
        Checks.id(phrase.id, at, problems)
        validateLocalized(
            phrase.text, languages, ContentLimits.MAX_PHRASE_LENGTH,
            missingCode = "phrase-language-missing", invalidCode = "phrase-invalid",
            at, problems,
        )
        // Die Notiz ist optional und darf eine Teilmenge der Sprachen abdecken --
        // anders als Titel und Text braucht sie keine Vollstaendigkeitspruefung.
        for ((lang, value) in phrase.note) {
            if (lang !in languages) {
                problems.warn("language-undeclared", at, lang.take(40))
                continue
            }
            if (value.length > ContentLimits.MAX_PHRASE_NOTE_LENGTH || !Texts.isUsable(value, mindestens = 1)) {
                problems.fatal("phrase-note-invalid", at, "$lang: unlesbar oder laenger als ${ContentLimits.MAX_PHRASE_NOTE_LENGTH}")
            }
        }
    }

    // Eine deklarierte Sprache muss vorhanden und lesbar sein -- eine halb
    // uebersetzte Zeile taeuscht Verlaesslichkeit vor, die sie nicht hat. Ein
    // Schluessel ausserhalb der deklarierten Liste ist dagegen nur ein Hinweis:
    // er stammt vermutlich aus einer neueren Fassung des Katalogs.
    private fun validateLocalized(
        values: Map<String, String>,
        languages: Set<String>,
        max: Int,
        missingCode: String,
        invalidCode: String,
        where: String,
        problems: ProblemLog,
    ) {
        for (lang in languages) {
            val value = values[lang]
            if (value == null) {
                problems.fatal(missingCode, where, lang)
                continue
            }
            // Ein einzelnes Zeichen genuegt: in ostasiatischen Schriften ist es
            // ein vollstaendiges Wort und ein gueltiger Satz zum Zeigen.
            if (value.length > max || !Texts.isUsable(value, mindestens = 1)) {
                problems.fatal(invalidCode, where, "$lang: unlesbar oder laenger als $max")
            }
        }
        for (lang in values.keys) {
            if (lang !in languages) problems.warn("language-undeclared", where, lang.take(40))
        }
    }

    private fun validateLanguages(languages: List<String>, where: String, problems: ProblemLog): Set<String> {
        if (languages.isEmpty()) {
            problems.fatal("languages-empty", where, "phrase catalog needs at least one language")
            return emptySet()
        }
        if (languages.size > ContentLimits.MAX_PHRASE_LANGUAGES) {
            problems.fatal("languages-too-many", where, "${languages.size}")
            return emptySet()
        }
        if (languages.size != languages.distinct().size) {
            problems.fatal("languages-duplicate", where, languages.joinToString(",").take(120))
        }
        val valid = HashSet<String>()
        for (lang in languages) {
            if (!ContentLimits.LANGUAGE_PATTERN.matches(lang)) {
                problems.fatal("language-invalid", where, lang.take(40))
            } else {
                valid.add(lang)
            }
        }
        return valid
    }
}
