package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertTrue

// Terms.kt ist die Fachwortliste (Glossar). Anders als jedes andere Inhaltsfeld
// im Paket (Titel, Schlagwoerter, Ueberschriften, Quellenangaben ...) trug
// TermsCheck bis zum 20.08.2026 weder eine Obergrenze fuer die Anzahl der
// Eintraege noch eine Laengengrenze fuer `wort`/`erklaerung`/`auch`, und prueft
// die Lesbarkeit nur ueber `isBlank()` statt ueber `Texts.isUsable`. Ein Paket
// konnte damit ein Vielfaches dessen enthalten, was jede Schwesterdatei
// (tips, guides, agriculture, pois, phrases) zulaesst -- bis an die
// JSON-Gesamtgrenzen (450 000 Elemente, 4 MiB je Datei), voellig ungeprueft und
// von keinem Sicherheitsnetz erfasst: Terms fliessen nicht in
// `pruefeSuchtextMenge` ein.
class TermsTest {

    private fun codesOf(vararg terms: Term, schema: Int = 1): List<String> {
        val log = ProblemLog()
        TermsCheck.validate(TermsFile(schema, terms.toList()), log)
        return log.all.map { it.code }
    }

    // Jede andere Inhaltsdatei lehnt zu viele Eintraege fatal ab
    // (Tips.kt, Guides.kt, Agriculture.kt, Pois.kt, Phrases.kt). Terms.kt tat
    // das nicht: eine sechsstellige Zahl winziger Eintraege ging unbeanstandet
    // durch, obwohl kein anderer Inhaltstyp das erlaubt.
    @Test
    fun zuVieleEintraegeSindFatal() {
        val viele = Array(ContentLimits.MAX_ITEMS_PER_FILE + 1) { Term("wort$it", "Eine kurze Erklaerung.") }
        assertTrue("too-many-items" in codesOf(*viele))
    }

    // Titel, Schlagwoerter, Ueberschriften -- ausnahmslos jedes benennende Feld
    // im Paket hat eine Laengengrenze. `wort` hatte keine: ein einzelner
    // Glossareintrag konnte tausende Zeichen lang sein.
    @Test
    fun sehrLangesWortIstFatal() {
        val lang = Term("x".repeat(10_000), "Eine kurze Erklaerung.")
        assertTrue("term-invalid" in codesOf(lang))
    }

    // Dieselbe Luecke bei der Erklaerung: nur eine Mindestlaenge (15 Zeichen)
    // war geprueft, keine Hoechstlaenge.
    @Test
    fun sehrLangeErklaerungIstFatal() {
        val lang = Term("Bauchfell", "E".repeat(10_000))
        assertTrue("term-invalid" in codesOf(lang))
    }

    // `auch` (alternative Schreibungen) hatte weder eine Listen- noch eine
    // Feldlaengengrenze.
    @Test
    fun zuVieleOderZuLangeAuchFormenSindFatal() {
        val zuViele = Term("Bauchfell", "Eine kurze Erklaerung.", auch = List(1000) { "form$it" })
        assertTrue("term-invalid" in codesOf(zuViele))

        val zuLang = Term("Bauchfell", "Eine kurze Erklaerung.", auch = listOf("y".repeat(1000)))
        assertTrue("term-invalid" in codesOf(zuLang))
    }

    // `isBlank()` erkennt nur Leerraum, keine unsichtbaren Zeichen. Ein Wort
    // aus lauter Nullbreiten-Leerzeichen (U+200B) ist nicht "blank" und wurde
    // bisher angenommen, obwohl es fuer niemanden lesbar ist -- genau der
    // Angriff, vor dem `Texts.isUsable` ueberall sonst im Paket schuetzt.
    @Test
    fun unsichtbaresWortIstFatal() {
        val nullbreitenLeerzeichen = 0x200B.toChar().toString()
        val unsichtbar = Term(nullbreitenLeerzeichen.repeat(3), "Eine kurze Erklaerung.")
        assertTrue("term-invalid" in codesOf(unsichtbar))
    }
}
