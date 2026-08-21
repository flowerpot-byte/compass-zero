package org.compasszero.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Am 17.08.2026 lief das Paket ohne Vorwarnung in die Wortgrenze: Ein fertiger
// Tipp wurde eingebaut, und danach lud das GANZE Paket nicht mehr --
// content-too-many-search-terms, 300 229 von 300 000. Der Fund selbst steht in
// ROADMAP.md; hier steht das Messgeraet dazu.
//
// Zweck ist der Bericht, nicht das Verbot: Wer Inhalte schreibt, soll den
// Abstand zur Grenze als Zahl sehen, bevor er einen Nachmittag in einen Eintrag
// steckt, der nicht mehr hineinpasst. Und reisst das Budget doch, faellt hier
// ein Satz mit Zahlen statt einer rohen Fatal-Meldung aus dem Parser.
//
// Gezaehlt wird nach derselben Regel wie in PackParser.pruefeSuchtextMenge --
// Feld fuer Feld, ohne die Felder zu verketten. Wird die Regel dort geaendert,
// gehoert sie hier nachgezogen; die Gegenprobe unten schlaegt sonst an.
class SuchbudgetTest {

    private fun repoRoot(): File {
        val fromProperty = System.getProperty("compasszero.repoRoot")
        if (fromProperty != null) return File(fromProperty)
        var dir = File(".").absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("settings.gradle.kts nicht gefunden")
        }
        return dir
    }

    private class Zaehlung {
        var zeichen = 0L
        var wortvorkommen = 0L
        val verschiedene = HashSet<String>()
        val proEintrag = mutableListOf<Pair<String, Long>>()

        fun zaehle(text: String): Long {
            val formen = Tokenizer.suchformen(text)
            zeichen += (formen?.mitUmlaut?.length ?: text.length).toLong()
            if (formen == null) return 0
            var n = 0L
            val erste = Tokenizer.tokensAusSuchform(formen.mitUmlaut)
            for (token in erste) {
                if (Tokenizer.enthaeltOhneWortabstand(token)) continue
                n++
                verschiedene.add(token)
            }
            // Genau wie PackParser: Die Rueckfallebene der Suche zaehlt beim
            // Wortschatz mit, aber nicht bei den Vorkommen. Zaehlte dieser
            // Bericht anders als die Pruefung, waere er wertlos -- er soll ja
            // vorhersagen, wann die Pruefung anschlaegt.
            for (token in Tokenizer.nurAbweichende(erste, formen.ohneUmlaut)) {
                if (Tokenizer.enthaeltOhneWortabstand(token)) continue
                verschiedene.add(token)
            }
            wortvorkommen += n
            return n
        }
    }

    private fun zaehleBasispaket(): Zaehlung {
        val paket = File(repoRoot(), "content/europe-de/paket")
        val json = Json { ignoreUnknownKeys = true }
        val z = Zaehlung()

        val tips = json.parseToJsonElement(File(paket, "content/tips.json").readText())
        for (e in tips.jsonObject.getValue("tips").jsonArray) {
            val o = e.jsonObject
            var n = z.zaehle(o.getValue("title").jsonPrimitive.content)
            n += z.zaehle(o.getValue("body").jsonPrimitive.content)
            o["keywords"]?.jsonArray?.forEach { n += z.zaehle(it.jsonPrimitive.content) }
            z.proEintrag += o.getValue("id").jsonPrimitive.content to n
        }
        val guides = json.parseToJsonElement(File(paket, "content/guides.json").readText())
        for (e in guides.jsonObject.getValue("guides").jsonArray) {
            val o = e.jsonObject
            var n = z.zaehle(o.getValue("title").jsonPrimitive.content)
            o["summary"]?.jsonPrimitive?.content?.let { n += z.zaehle(it) }
            o["steps"]?.jsonArray?.forEach { s ->
                n += z.zaehle(s.jsonObject.getValue("text").jsonPrimitive.content)
            }
            z.proEintrag += o.getValue("id").jsonPrimitive.content to n
        }
        val agri = json.parseToJsonElement(File(paket, "content/agriculture.json").readText())
        for (e in agri.jsonObject.getValue("chapters").jsonArray) {
            val o = e.jsonObject
            var n = z.zaehle(o.getValue("title").jsonPrimitive.content)
            o["sections"]?.jsonArray?.forEach { s ->
                n += z.zaehle(s.jsonObject.getValue("heading").jsonPrimitive.content)
                n += z.zaehle(s.jsonObject.getValue("body").jsonPrimitive.content)
            }
            z.proEintrag += o.getValue("id").jsonPrimitive.content to n
        }
        return z
    }

    @Test
    fun dasBasispaketPasstNochInsSuchbudget() {
        val z = zaehleBasispaket()
        val grenze = ContentLimits.MAX_SUCHINDEX_WORTVORKOMMEN.toLong()
        val frei = grenze - z.wortvorkommen
        val mittel = z.wortvorkommen / z.proEintrag.size

        val grenzeWortschatz = ContentLimits.MAX_SUCHINDEX_VERSCHIEDENE_WOERTER.toLong()
        val wortschatz = z.verschiedene.size.toLong()
        val mittelZeichen = z.zeichen / z.proEintrag.size
        // Wie viele neue Woerter ein Eintrag im Schnitt MITBRINGT, ist etwas
        // anderes als seine Wortzahl: Das meiste steht schon im Verzeichnis.
        val mittelNeueWoerter = maxOf(1L, wortschatz / z.proEintrag.size)

        println("Suchbudget des Basispakets (${z.proEintrag.size} Eintraege):")
        println("  Wortvorkommen  ${z.wortvorkommen} von $grenze, frei $frei")
        println("  Wortschatz     $wortschatz von $grenzeWortschatz, frei ${grenzeWortschatz - wortschatz}")
        println("  Suchzeichen    ${z.zeichen} von ${ContentLimits.MAX_SUCHTEXT_ZEICHEN}")
        // Es gilt die Grenze, die ZUERST greift -- eine Zahl, die nur die
        // bequemste der drei nennt, waere eine Falle.
        val nachVorkommen = frei / mittel
        val nachZeichen = (ContentLimits.MAX_SUCHTEXT_ZEICHEN - z.zeichen) / mittelZeichen
        val nachWortschatz = (grenzeWortschatz - wortschatz) / mittelNeueWoerter
        val passenNoch = minOf(nachVorkommen, nachZeichen, nachWortschatz)
        val engste = when (passenNoch) {
            nachZeichen -> "Suchzeichen"
            nachVorkommen -> "Wortvorkommen"
            else -> "Wortschatz"
        }
        println(
            "  Mittel je Eintrag $mittel Woerter / $mittelZeichen Zeichen / $mittelNeueWoerter neue Woerter",
        )
        println(
            "  Es reicht noch fuer $passenNoch " +
                (if (passenNoch == 1L) "Eintrag" else "Eintraege") +
                " -- am engsten ist $engste " +
                "(Vorkommen $nachVorkommen, Zeichen $nachZeichen, Wortschatz $nachWortschatz)",
        )
        println("  Groesste Eintraege:")
        z.proEintrag.sortedByDescending { it.second }.take(5).forEach {
            println("    ${it.second}\t${it.first}")
        }

        assertTrue(
            z.wortvorkommen <= grenze,
            "Das Suchbudget ist gerissen: ${z.wortvorkommen} Wortvorkommen, erlaubt sind $grenze " +
                "(${z.wortvorkommen - grenze} zu viel). Damit laedt das PAKET NICHT MEHR, nicht nur " +
                "der neue Eintrag. Was jetzt gilt, steht in ROADMAP.md unter \"Das Wortbudget des " +
                "Europa-Pakets ist voll\" -- die Grenze wird nicht einfach angehoben, das ist eine " +
                "Entwurfsentscheidung.",
        )
        assertTrue(
            wortschatz <= grenzeWortschatz,
            "Der Wortschatz ist gerissen: $wortschatz verschiedene Woerter, erlaubt sind " +
                "$grenzeWortschatz. Das ist die Groesse, an der der Speicher wirklich haengt -- " +
                "sie wird nicht einfach angehoben, das ist eine Entwurfsentscheidung.",
        )
        assertTrue(
            z.zeichen <= ContentLimits.MAX_SUCHTEXT_ZEICHEN,
            "Zeichenbudget gerissen: ${z.zeichen} von ${ContentLimits.MAX_SUCHTEXT_ZEICHEN}",
        )
    }

    // Gegenprobe gegen Auseinanderlaufen: Der Parser rechnet dieselbe Summe. Zaehlt
    // er anders als dieser Test, faellt es hier auf und nicht erst dann, wenn ein
    // Eintrag durchrutscht, den der Parser abweist.
    @Test
    fun dieEigeneRechnungStimmtMitDerDesParsersUeberein() {
        val z = zaehleBasispaket()
        val paket = File(repoRoot(), "content/europe-de/paket")
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to File(paket, "manifest.json").readBytes(),
                "content/tips.json" to File(paket, "content/tips.json").readBytes(),
                "content/guides.json" to File(paket, "content/guides.json").readBytes(),
                "content/agriculture.json" to File(paket, "content/agriculture.json").readBytes(),
                "content/terms.json" to File(paket, "content/terms.json").readBytes(),
            ),
            emptySet(),
        )
        val parserMeldetZuViel = result.problems.any { it.code == "content-too-many-search-terms" }
        val eigeneRechnungSagtZuViel = z.wortvorkommen > ContentLimits.MAX_SUCHINDEX_WORTVORKOMMEN
        assertTrue(
            parserMeldetZuViel == eigeneRechnungSagtZuViel,
            "Parser und Budgetrechnung sind uneins: Parser meldet zu viel = $parserMeldetZuViel, " +
                "eigene Rechnung = $eigeneRechnungSagtZuViel bei ${z.wortvorkommen} Wortvorkommen. " +
                "Wurde PackParser.pruefeSuchtextMenge geaendert, gehoert die Zaehlung hier nachgezogen.",
        )
    }
}
