package org.compasszero.content

import kotlinx.serialization.Serializable

/**
 * Ein Fachwort und was es heisst.
 *
 * WARUM ES DIESE ART GIBT: Das Paket ist bewusst in einfacher Sprache
 * geschrieben, aber ganz ohne Fachwoerter geht es nicht -- ein Bauchfell heisst
 * Bauchfell, und eine Sehne ist keine Ader. Max am 06.08.2026: "fach begriffe
 * wie z.b. regenbogenhaut muessen erklaert werden."
 *
 * Nachgezaehlt im Bestand: Es sind wenige, aber sie stehen an entscheidenden
 * Stellen -- Bauchfell 14 Mal, Sehne 11, Schleimhaut 6, dazu Rippenfell,
 * Brustfell, Hornhaut, Bindehaut, Regenbogenhaut, Kallus. Wer eines davon
 * nicht kennt, versteht den Satz nicht, in dem es steht, und der Satz ist oft
 * der entscheidende.
 *
 * WARUM IM PAKET UND NICHT IM PROGRAMM: Die Erklaerungen sind Inhalt. Sie
 * gehoeren unter dieselbe Unterschrift wie die Tipps, werden mit dem Paket
 * ausgetauscht und muessen in einem englischen Paket englisch sein.
 *
 * [wort] ist die Grundform, wie sie im Text steht. [auch] nennt weitere
 * Schreibungen und Beugungen, die dasselbe meinen -- "Sehnen" neben "Sehne",
 * "Bauchfells" neben "Bauchfell". Ohne diese Liste faende man das Wort im
 * Fliesstext nur zufaellig.
 */
@Serializable
class Term(
    val wort: String,
    val erklaerung: String,
    val auch: List<String> = emptyList(),
)

@Serializable
class TermsFile(val schema: Int, val terms: List<Term>)

internal object TermsCheck {

    fun validate(file: TermsFile, problems: ProblemLog) {
        val where = "content/terms.json"
        if (file.schema != 1) {
            problems.fatal("schema-unsupported", where, "terms schema ${file.schema}")
            return
        }
        val gesehen = HashSet<String>()
        for (term in file.terms) {
            if (term.wort.isBlank()) {
                problems.fatal("term-empty", where, "ein Fachwort ohne Wort")
                continue
            }
            // Kleingeschrieben vergleichen: "Sehne" und "sehne" waeren sonst
            // zwei Eintraege, und im Text traefe mal der eine, mal der andere.
            if (!gesehen.add(term.wort.lowercase())) {
                problems.fatal("term-duplicate", where, term.wort)
            }
            for (weitere in term.auch) {
                if (!gesehen.add(weitere.lowercase())) {
                    problems.fatal("term-duplicate", where, "${term.wort} -> $weitere")
                }
            }
            // Eine Erklaerung, die das Wort nur wiederholt, hilft niemandem.
            if (term.erklaerung.length < 15) {
                problems.fatal("term-short", where, term.wort)
            }
        }
    }
}
