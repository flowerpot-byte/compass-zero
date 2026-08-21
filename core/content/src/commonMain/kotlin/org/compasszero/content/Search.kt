package org.compasszero.content

enum class ContentKind { Tip, Guide, Agriculture }

class SearchHit(val kind: ContentKind, val id: String, val title: String, val score: Int)

internal object Tokenizer {

    // Nur Zusammenziehungen, die im Deutschen dieselbe Bedeutung tragen. Alles
    // andere bleibt stehen: eine Zeichentabelle fuer lateinische Schrift wuerde
    // kyrillische, griechische oder ostasiatische Pakete unsuchbar machen.
    private val FOLD = mapOf(
        'ä' to "ae", 'ö' to "oe", 'ü' to "ue", 'ß' to "ss",
    )

    // DIE ZWEITE FALTUNG, seit dem 21.08.2026. Sie deckt den Fall ab, dass
    // jemand die Umlaut-Taste gar nicht benutzt: "korper" statt "Körper",
    // "gefahrlich" statt "gefährlich", "notig" statt "nötig".
    //
    // WARUM DAS NOETIG WAR: Gemessen an 51 Woertern, die ein Mensch ohne
    // Umlaut-Taste tippen wuerde, fanden 37 im ganzen Paket NICHTS -- darunter
    // korper, gefahrlich, notig, flussigkeit, oberflache, erkaltung, ubelkeit,
    // krauter, gemuse, uberleben. Die erste Faltung macht aus "ö" ein "oe";
    // wer stattdessen "o" tippt, trifft damit keinen Wortanfang.
    //
    // WARUM BEIDE FORMEN INS VERZEICHNIS GEHEN und nicht nur die zweite: "Müller"
    // wird oft "Mueller" geschrieben. Wer so tippt, muss den Text weiterhin
    // finden. Beide Formen kosten zusammen rund 7 500 zusaetzliche Woerter und
    // 51 000 zusaetzliche Vorkommen -- gemessen am Europa-Paket vom 21.08.2026,
    // also 14 % des Wortschatz-Budgets und weit innerhalb der Vorkommensgrenze.
    private val FOLD_OHNE_UMLAUT = mapOf(
        'ä' to "a", 'ö' to "o", 'ü' to "u", 'ß' to "ss",
    )

    // Schriften, die keine Wortabstaende setzen. Dort findet eine Suche nach
    // Wortanfaengen nur den Satzanfang, deshalb laufen sie ueber den Volltext.
    // Koreanisch fehlt hier bewusst: es setzt Wortabstaende wie europaeische
    // Sprachen und braucht deshalb das Wortverzeichnis mit seiner Gewichtung.
    fun istOhneWortabstand(codePunkt: Int): Boolean =
        codePunkt in 0x0E00..0x0EFF || // Thai und Lao
        codePunkt in 0x1000..0x109F || // Birmanisch
        codePunkt in 0x1780..0x17FF || // Khmer
        codePunkt in 0x3040..0x30FF || // Kana
        codePunkt in 0x3400..0x4DBF ||
        codePunkt in 0x4E00..0x9FFF ||
        codePunkt in 0xF900..0xFAFF ||
        codePunkt in 0xFF66..0xFF9D || // halbbreite Kana
        codePunkt in 0x20000..0x3FFFF // seltene ostasiatische Zeichen

    // Ein einzelnes Zeichen kann ein vollstaendiges Wort sein — im Koreanischen
    // ebenso wie im Chinesischen. Das ist unabhaengig davon, ob die Schrift
    // Wortabstaende setzt: Koreanisch tut das und braucht trotzdem Ein-Zeichen-Woerter.
    fun istEinzelzeichenWort(codePunkt: Int): Boolean =
        istOhneWortabstand(codePunkt) || codePunkt in 0xAC00..0xD7AF

    fun enthaeltOhneWortabstand(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            val (codePunkt, weite) = codePunktBei(text, i)
            if (istOhneWortabstand(codePunkt)) return true
            i += weite
        }
        return false
    }

    // Eine einzige Aufbereitung fuer alles: Wortliste, Volltext und Anfrage. Waeren
    // sie unterschiedlich, faende ein abgetippter Titel sich selbst nicht.
    fun suchform(text: String): String? = suchformen(text)?.mitUmlaut

    /**
     * Beide Schreibweisen aus EINEM Durchlauf.
     *
     * WARUM IN EINEM DURCHLAUF UND NICHT NACHTRAEGLICH: Aus der fertigen ersten
     * Form laesst sich die zweite nicht mehr sauber gewinnen. Dort steht "ue"
     * sowohl fuer ein "ü" als auch fuer ein echtes "u" gefolgt von "e" -- aus
     * "Quelle" wuerde "qulle", aus "Feuer" "feur", aus "bauen" "baun". Hier ist
     * beim Falten noch bekannt, welches Zeichen wirklich dastand.
     *
     * ohneUmlaut ist null, wenn der Text gar keinen Umlaut enthaelt. Dann gibt
     * es nichts Zweites zu speichern.
     */
    fun suchformen(text: String): Suchformen? {
        val vereinheitlicht = Texts.suchform(text) ?: return null
        val out = StringBuilder(vereinheitlicht.length)
        // ERST BEIM ERSTEN UMLAUT ANLEGEN: Die allermeisten Felder haben keinen.
        // Ein zweiter Puffer je Feld kostete beim Bau eines Pakets an der Grenze
        // mehr Speicher, als am Ende uebrig war.
        var ohne: StringBuilder? = null
        var i = 0
        while (i < vereinheitlicht.length) {
            val (codePunkt, weite) = codePunktBei(vereinheitlicht, i)
            i += weite
            if (istMarkierung(codePunkt)) continue
            if (weite == 1) {
                val klein = vereinheitlicht[i - 1].lowercaseChar()
                val gefaltet = FOLD[klein]
                if (gefaltet != null) {
                    val zweite = FOLD_OHNE_UMLAUT.getValue(klein)
                    if (ohne == null && zweite != gefaltet) {
                        ohne = StringBuilder(vereinheitlicht.length).append(out)
                    }
                    out.append(gefaltet)
                    ohne?.append(zweite)
                } else {
                    out.append(klein)
                    ohne?.append(klein)
                }
            } else {
                // Auch oberhalb der Grundebene gibt es Gross- und Kleinschreibung
                // (Deseret, Adlam, Warang Citi). Wuerde das Zeichen hier
                // unveraendert durchgereicht, faende eine kleingeschriebene
                // Anfrage grossgeschriebenen Text nicht.
                val gesenkt = vereinheitlicht.substring(i - weite, i).lowercase()
                out.append(gesenkt)
                ohne?.append(gesenkt)
            }
        }
        return Suchformen(out.toString(), ohne?.toString())
    }

    class Suchformen(val mitUmlaut: String, val ohneUmlaut: String?)

    /**
     * Aus der zweiten Schreibweise nur die Woerter, die sich von der ersten
     * wirklich unterscheiden.
     *
     * WARUM DAS NOETIG IST: Die zweite Form entsteht fuer das GANZE Feld, sobald
     * darin irgendwo ein Umlaut steht. Wuerde sie ungefiltert mitgezaehlt, ginge
     * jedes Wort dieses Feldes ein zweites Mal ins Verzeichnis -- auch die ohne
     * Umlaut. Am 21.08.2026 hat genau das die Wortvorkommen von 554 115 auf
     * 1 094 089 getrieben und das Paket unlesbar gemacht.
     *
     * Beide Formen entstehen aus demselben Text, und keine der beiden Faltungen
     * aendert etwas an den Wortgrenzen. Die Listen sind deshalb gleich lang und
     * stehen Wort fuer Wort untereinander.
     */
    fun nurAbweichende(erste: List<String>, ohneUmlaut: String?): List<String> {
        if (ohneUmlaut == null) return emptyList()
        val zweite = tokensAusSuchform(ohneUmlaut)
        if (zweite.size != erste.size) return zweite
        val out = ArrayList<String>()
        for (i in erste.indices) if (zweite[i] != erste[i]) out.add(zweite[i])
        return out
    }

    fun tokens(text: String): List<String> {
        val aufbereitet = suchform(text) ?: return emptyList()
        return tokensAusSuchform(aufbereitet)
    }

    fun tokensAusSuchform(aufbereitet: String): List<String> {
        val out = ArrayList<String>()
        val current = StringBuilder()
        var ohneAbstand = false
        var einzelzeichenWort = false

        fun abschliessen() {
            val mindestens = if (einzelzeichenWort) 1 else 2
            if (current.length >= mindestens) out.add(current.toString())
            current.setLength(0)
            ohneAbstand = false
            einzelzeichenWort = false
        }

        var i = 0
        while (i < aufbereitet.length) {
            val (codePunkt, weite) = codePunktBei(aufbereitet, i)
            if (istSichtbarerCodepunkt(codePunkt)) {
                val jetztOhneAbstand = istOhneWortabstand(codePunkt)
                // Schriftwechsel trennt Woerter: in japanischem Text steht "pH"
                // ohne Leerzeichen am Nachbarzeichen. Ohne diese Trennung waeren
                // genau die Fachbegriffe unauffindbar, auf die es ankommt.
                if (current.isNotEmpty() && jetztOhneAbstand != ohneAbstand) abschliessen()
                ohneAbstand = jetztOhneAbstand
                if (istEinzelzeichenWort(codePunkt)) einzelzeichenWort = true
                current.append(aufbereitet, i, i + weite)
            } else {
                abschliessen()
            }
            i += weite
        }
        abschliessen()
        return out
    }

    fun codePunktBei(text: String, i: Int): Pair<Int, Int> {
        val c = text[i]
        if (c.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
            return (0x10000 + ((c.code - 0xD800) shl 10) + (text[i + 1].code - 0xDC00)) to 2
        }
        return c.code to 1
    }
}

// Das Wortverzeichnis liegt in gleichlangen Feldern statt in verschachtelten
// Abbildungen: fuer jedes Wort ein Eintrag, dahinter ein zusammenhaengender
// Bereich mit Dokumentnummern und Gewichten. Eine Abbildung je Wort kostete auf
// alten Geraeten ein Vielfaches — bei hunderttausend verschiedenen Woertern war
// das der Unterschied zwischen laufender und abgestuerzter Suche.
class SearchIndex private constructor(
    private val terms: Array<String>,
    private val termStart: IntArray,
    private val trefferDoc: IntArray,
    private val trefferGewicht: IntArray,
    private val docs: List<Doc>,
    private val volltext: Map<Int, List<Pair<String, Int>>>,
    // Die Rueckfallebene fuer Anfragen ohne Umlaut, siehe suche unten. Beide
    // Felder sind nach dem Wort sortiert und gleich lang: termsOhneUmlaut haelt
    // die umlautfreie Schreibweise, zeigtAuf die Nummer des Wortes im
    // Hauptverzeichnis. Die Trefferlisten werden NICHT verdoppelt.
    private val termsOhneUmlaut: Array<String>,
    private val zeigtAuf: IntArray,
) {

    internal class Doc(val kind: ContentKind, val id: String, val title: String)

    /**
     * Sucht erst wie immer. Findet das nichts, wird die Anfrage ein zweites Mal
     * gegen die umlautfreien Schreibweisen gehalten.
     *
     * WARUM ALS RUECKFALL UND NICHT GLEICHRANGIG: Gleichrangig macht die Suche
     * unschaerfer, nicht besser. Am 21.08.2026 gemessen: Werden beide
     * Schreibweisen zusammen ins Hauptverzeichnis gelegt, trifft "hoch"
     * ploetzlich jedes "hoechstens", "kalt" jedes "Kaelte", "gefahr" jedes
     * "gefaehrdet" -- vier festgenagelte Notfall-Wege sind sofort umgefallen.
     * Als Rueckfall aendert sich an keiner Reihenfolge etwas, die heute stimmt;
     * es kommt nur etwas dort dazu, wo vorher gar nichts stand.
     *
     * WOFUER DAS DA IST: Wer die Umlaut-Taste nicht benutzt, tippt "korper",
     * "gefahrlich", "notig", "erkaltung", "krauter". Von 51 solchen Woertern
     * fanden am 21.08.2026 37 im ganzen Paket NICHTS.
     */
    fun search(query: String, limit: Int = 20): List<SearchHit> {
        val treffer = sucheMit(query, limit, false)
        if (treffer.isNotEmpty()) return treffer
        return sucheMit(query, limit, true)
    }

    private fun sucheMit(query: String, limit: Int, ohneUmlaut: Boolean): List<SearchHit> {
        if (limit <= 0) return emptyList()
        val worte = Tokenizer.tokens(query)
        if (worte.isEmpty()) return emptyList()

        val jeWort = ArrayList<Map<Int, Int>>(worte.size)
        for (token in worte) {
            // Woerter aus Schriften ohne Wortabstand laufen in die Volltextsuche:
            // dort gibt es keine Wortanfaenge, an denen sich ein Verzeichnis
            // festmachen liesse. Jedes andere Wort zaehlt als Wortanfang, nicht nur
            // das letzte — sonst haengt das Ergebnis an der Tippreihenfolge.
            val treffer = if (Tokenizer.enthaeltOhneWortabstand(token)) {
                matchVolltext(token)
            } else {
                matchPrefix(token, ohneUmlaut)
            }
            // Ein Wort, das im ganzen Handbuch nirgends vorkommt, ist ein
            // Tippfehler oder gehoert nicht hierher. Dann bleibt die Anfrage leer:
            // Es gibt nichts, worauf sich ein Teiltreffer stuetzen koennte.
            if (treffer.isEmpty()) return emptyList()
            jeWort.add(treffer)
        }

        var scores: Map<Int, Int>? = jeWort[0]
        for (i in 1 until jeWort.size) {
            scores = verbinde(scores, jeWort[i])
            if (scores == null) break
        }
        if (scores != null) return besteTreffer(scores, limit)

        return besteTeiltreffer(jeWort, limit)
    }

    // Alle Woerter kommen vor, aber nie im selben Eintrag. Frueher gab es dann
    // NICHTS — und zwar genau bei den Anfragen, die jemand unter Druck tippt:
    // "mein kind atmet nicht" blieb leer, waehrend "person atmet nicht mehr" den
    // richtigen Eintrag sofort fand. Ein Wort mehr im Satz durfte nicht daran
    // entscheiden, ob ein Handbuch antwortet oder schweigt.
    //
    // Deshalb zaehlt hier zuerst, WIE VIELE Woerter ein Eintrag trifft, und erst
    // danach das Gewicht. Ausgegeben wird nur die beste erreichbare Anzahl: Wer
    // drei von vier Woertern hat, verdraengt den, der eines hat.
    private fun besteTeiltreffer(jeWort: List<Map<Int, Int>>, limit: Int): List<SearchHit> {
        val anzahl = HashMap<Int, Int>()
        val summe = HashMap<Int, Int>()
        for (treffer in jeWort) {
            for ((doc, score) in treffer) {
                anzahl[doc] = (anzahl[doc] ?: 0) + 1
                summe[doc] = (summe[doc] ?: 0) + score
            }
        }
        var beste = 0
        for (n in anzahl.values) if (n > beste) beste = n
        val engereWahl = HashMap<Int, Int>()
        for ((doc, n) in anzahl) if (n == beste) engereWahl[doc] = summe.getValue(doc)
        return besteTreffer(engereWahl, limit)
    }

    private fun verbinde(bisher: Map<Int, Int>?, neu: Map<Int, Int>): Map<Int, Int>? {
        if (neu.isEmpty()) return null
        if (bisher == null) return neu
        val merged = HashMap<Int, Int>()
        for ((doc, score) in neu) {
            val before = bisher[doc] ?: continue
            merged[doc] = before + score
        }
        return if (merged.isEmpty()) null else merged
    }

    // Auch im Volltext zaehlt, wo der Treffer steht: ein Wort im Titel wiegt
    // schwerer als eines tief im Fliesstext. Ohne das waere die Rangfolge in
    // ostasiatischen Paketen flach, und der erste Treffer waere Zufall.
    private fun matchVolltext(teil: String): Map<Int, Int> {
        val matches = HashMap<Int, Int>()
        for ((doc, felder) in volltext) {
            for ((text, gewicht) in felder) {
                if (!text.contains(teil)) continue
                if (gewicht > (matches[doc] ?: 0)) matches[doc] = gewicht
            }
        }
        return matches
    }

    // Punktzahlen sind kleine ganze Zahlen, deshalb wird nach Punktzahl in Faecher
    // einsortiert und nur so weit weitergearbeitet, bis genug Treffer beisammen
    // sind.
    private fun besteTreffer(scores: Map<Int, Int>, limit: Int): List<SearchHit> {
        val faecher = HashMap<Int, MutableList<Int>>()
        for ((doc, score) in scores) {
            faecher.getOrPut(score) { ArrayList() }.add(doc)
        }
        val out = ArrayList<SearchHit>(minOf(limit, scores.size))
        for (score in faecher.keys.sortedDescending()) {
            if (out.size >= limit) break
            val fach = faecher.getValue(score)
            // Innerhalb eines Fachs feste Reihenfolge, damit das Ergebnis
            // reproduzierbar bleibt.
            fach.sortWith(
                compareBy({ docs[it].title }, { docs[it].kind.ordinal }, { docs[it].id })
            )
            for (doc in fach) {
                if (out.size >= limit) break
                out.add(SearchHit(docs[doc].kind, docs[doc].id, docs[doc].title, score))
            }
        }
        return out
    }

    // Das Vokabular ist sortiert, deshalb reicht die Einstiegsstelle und ein Lauf,
    // solange das Praefix passt. Ein Vollscan bei jedem Tastendruck waere auf
    // alten Geraeten sofort spuerbar.
    // Die Ebene wird DURCHGEREICHT und nicht in einem Feld gemerkt. Ein Feld
    // waere ein stiller Fehler, sobald zwei Suchen gleichzeitig laufen -- die
    // eine wuerde der anderen die Ebene unter den Fuessen wegziehen, und heraus
    // kaeme ein Ergebnis, das niemand nachvollziehen kann.
    private fun matchPrefix(token: String, rueckfall: Boolean): Map<Int, Int> {
        val liste = if (rueckfall) termsOhneUmlaut else terms
        var low = 0
        var high = liste.size
        while (low < high) {
            val mid = (low + high) / 2
            if (liste[mid] < token) low = mid + 1 else high = mid
        }
        val matches = HashMap<Int, Int>()
        var i = low
        while (i < liste.size && liste[i].startsWith(token)) {
            val bonus = if (liste[i] == token) 1 else 0
            // In der Rueckfallebene steht hier nur die Nummer des Wortes im
            // Hauptverzeichnis; die Trefferliste ist dieselbe.
            val wort = if (rueckfall) zeigtAuf[i] else i
            for (t in termStart[wort] until termStart[wort + 1]) {
                val score = trefferGewicht[t] + bonus
                val doc = trefferDoc[t]
                if (score > (matches[doc] ?: 0)) matches[doc] = score
            }
            i++
        }
        return matches
    }

    companion object {

        private const val WEIGHT_TITLE = 5
        private const val WEIGHT_KEYWORD = 3
        private const val WEIGHT_SUMMARY = 2
        private const val WEIGHT_BODY = 1

        fun build(pack: LoadedPack): SearchIndex {
            val docs = ArrayList<Doc>()
            val volltext = HashMap<Int, List<Pair<String, Int>>>()
            // Jedes Wort wird sofort auf eine Nummer abgebildet und je Eintrag
            // sofort zusammengefasst. Erst sammeln und spaeter zusammenfassen
            // haette den Spitzenbedarf an die Zahl der Wortvorkommen gebunden —
            // bei sich wiederholendem Text ein Vielfaches.
            val wortNummer = HashMap<String, Int>()
            val worte = ArrayList<String>()
            // Die Rueckfallebene: umlautfreie Schreibweise -> Nummer des Wortes
            // im Hauptverzeichnis. Sie wird beim Bauen gefuellt, weil nur dort
            // noch bekannt ist, ob ein "ue" aus einem "ü" stammt oder aus einem
            // echten "u" gefolgt von "e" -- aus dem fertigen Wort "quelle" liesse
            // sich das nicht mehr unterscheiden.
            val ohneUmlautZuWort = HashMap<String, Int>()
            var paarWort = IntArray(1024)
            var paarDoc = IntArray(1024)
            var paarGewicht = IntArray(1024)
            var paare = 0

            fun merke(wort: Int, doc: Int, gewicht: Int) {
                if (paare == paarWort.size) {
                    paarWort = paarWort.copyOf(paare * 2)
                    paarDoc = paarDoc.copyOf(paare * 2)
                    paarGewicht = paarGewicht.copyOf(paare * 2)
                }
                paarWort[paare] = wort
                paarDoc[paare] = doc
                paarGewicht[paare] = gewicht
                paare++
            }

            // Jedes Feld wird EINZELN aufbereitet. Wuerden die Felder erst
            // verbunden und dann aufbereitet, faellt das Ausdehnungsbudget aus
            // Texts.suchform nur einmal an statt je Feld -- und ein Verbund, der
            // es reisst, liefert null. Das warf frueher das ganze Feld stumm aus
            // dem Verzeichnis: Der Inhalt stand in der App, war aber ueber die
            // Suche nicht auffindbar. Bei einem Notfallhandbuch ist das ein
            // Ausfall der Kernfunktion, und er meldet sich nirgends.
            fun add(kind: ContentKind, id: String, title: String, felder: List<Pair<List<String>, Int>>) {
                val docIndex = docs.size
                docs.add(Doc(kind, id, title))
                val volleTeile = ArrayList<Pair<String, Int>>(felder.size)
                // Nur die Woerter dieses einen Eintrags, also klein und kurzlebig.
                val proEintrag = HashMap<Int, Int>()
                for ((teile, weight) in felder) {
                    for (teil in teile) {
                        val formen = Tokenizer.suchformen(teil) ?: continue
                        val aufbereitet = formen.mitUmlaut
                        if (Tokenizer.enthaeltOhneWortabstand(aufbereitet)) volleTeile.add(aufbereitet to weight)
                        val ersteTokens = Tokenizer.tokensAusSuchform(aufbereitet)
                        // Die umlautfreie Schreibweise wird hier nur GEMERKT, nicht
                        // ins Hauptverzeichnis gelegt: Beide Listen entstehen aus
                        // demselben Text, keine der beiden Faltungen verschiebt eine
                        // Wortgrenze, also stehen sie Wort fuer Wort untereinander.
                        val zweiteTokens = formen.ohneUmlaut
                            ?.let { Tokenizer.tokensAusSuchform(it) }
                            ?.takeIf { it.size == ersteTokens.size }
                        for ((stelle, token) in ersteTokens.withIndex()) {
                            // Woerter aus Schriften ohne Wortabstand sind ganze Saetze.
                            // Als Wortliste kosten sie viel Speicher und finden nur
                            // Satzanfaenge — die deckt der Volltext ab. Gemischte Felder
                            // geben ihre lateinischen Woerter trotzdem ins Verzeichnis.
                            if (Tokenizer.enthaeltOhneWortabstand(token)) continue
                            val nummer = wortNummer.getOrPut(token) {
                                worte.add(token)
                                worte.size - 1
                            }
                            val bisher = proEintrag[nummer]
                            if (bisher == null || weight > bisher) proEintrag[nummer] = weight
                            val ohne = zweiteTokens?.get(stelle)
                            if (ohne != null && ohne != token) ohneUmlautZuWort[ohne] = nummer
                        }
                    }
                }
                for ((nummer, gewicht) in proEintrag) merke(nummer, docIndex, gewicht)
                if (volleTeile.isNotEmpty()) volltext[docIndex] = volleTeile
            }

            for (tip in pack.tips) {
                add(
                    ContentKind.Tip, tip.id, tip.title,
                    listOf(
                        listOf(tip.title) to WEIGHT_TITLE,
                        tip.keywords to WEIGHT_KEYWORD,
                        listOf(tip.body) to WEIGHT_BODY,
                    ),
                )
            }
            for (guide in pack.guides) {
                add(
                    ContentKind.Guide, guide.id, guide.title,
                    listOf(
                        listOf(guide.title) to WEIGHT_TITLE,
                        listOf(guide.summary) to WEIGHT_SUMMARY,
                        guide.steps.map { it.text } to WEIGHT_BODY,
                        // Material- und Werkzeugliste zaehlen mit, aber nur auf
                        // Fliesstext-Gewicht. Vorher standen sie ueberhaupt
                        // nicht im Verzeichnis: Wer nach einem Werkstoff sucht,
                        // den nur die Materialliste nennt, fand die Anleitung
                        // gar nicht. In der Praxis fiel das kaum auf, weil die
                        // Schritte dieselben Woerter noch einmal nennen -- aber
                        // "kaum" ist bei einem Notfallhandbuch keine Antwort.
                        // Hoeheres Gewicht waere falsch: Eine Zutat ist kein
                        // Titel, und eine Anleitung soll keinen Notfall-Tipp
                        // verdraengen, nur weil das Wort in ihrer Stueckliste
                        // vorkommt.
                        guide.materials.flatMap { listOf(it.item, it.amount, it.note) } to WEIGHT_BODY,
                        guide.tools to WEIGHT_BODY,
                    ),
                )
            }
            for (chapter in pack.agriculture) {
                add(
                    ContentKind.Agriculture, chapter.id, chapter.title,
                    listOf(
                        listOf(chapter.title) to WEIGHT_TITLE,
                        chapter.sections.map { it.heading } to WEIGHT_KEYWORD,
                        chapter.sections.map { it.body } to WEIGHT_BODY,
                    ),
                )
            }

            // Ab hier wird die Zuordnung Wort -> Nummer nicht mehr gebraucht. Sie
            // haelt bei hunderttausenden verschiedenen Woertern zweistellige
            // Megabytes und lebte bisher waehrend des gesamten Indexaufbaus
            // weiter -- genau in der Spitze, in der der Speicher knapp wird.
            wortNummer.clear()
            return baue(worte, paarWort, paarDoc, paarGewicht, paare, docs, volltext, ohneUmlautZuWort)
        }

        // Ordnet die Woerter alphabetisch und legt die Treffer in
        // zusammenhaengende Bereiche. Sortiert wird ueber Zaehlen statt ueber
        // Vergleiche von Objekten — das kommt ohne Zwischenkopien aus.
        private fun baue(
            worte: List<String>,
            paarWort: IntArray,
            paarDoc: IntArray,
            paarGewicht: IntArray,
            paare: Int,
            docs: List<Doc>,
            volltext: Map<Int, List<Pair<String, Int>>>,
            ohneUmlautZuWort: Map<String, Int>,
        ): SearchIndex {
            // Der Rang wird ueber eine sortierte Reihenfolge der Nummern bestimmt,
            // nicht ueber eine zweite Wortkarte. Eine solche Karte haette jedes
            // verschiedene Wort ein weiteres Mal gehalten -- bei
            // hunderttausenden Woertern zweistellige Megabytes, und das genau in
            // der Spitze, in der auf alten Geraeten der Speicher knapp wird.
            val reihenfolge = worte.indices.sortedBy { worte[it] }
            val rangVon = IntArray(worte.size)
            for (rang in reihenfolge.indices) rangVon[reihenfolge[rang]] = rang
            val terms = Array(worte.size) { worte[reihenfolge[it]] }

            val anzahlJeRang = IntArray(terms.size + 1)
            for (i in 0 until paare) anzahlJeRang[rangVon[paarWort[i]] + 1]++
            for (i in 1..terms.size) anzahlJeRang[i] += anzahlJeRang[i - 1]
            val start = anzahlJeRang.copyOf()

            val trefferDoc = IntArray(paare)
            val trefferGewicht = IntArray(paare)
            val naechste = start.copyOf()
            for (i in 0 until paare) {
                val rang = rangVon[paarWort[i]]
                val stelle = naechste[rang]++
                trefferDoc[stelle] = paarDoc[i]
                trefferGewicht[stelle] = paarGewicht[i]
            }

            // Die Rueckfallebene: fuer jedes Wort mit Umlaut die umlautfreie
            // Schreibweise, sortiert, mit Verweis auf das Wort selbst. Woerter,
            // deren umlautfreie Form es ohnehin schon gibt, bleiben weg -- die
            // findet die normale Suche.
            // GEPRUEFT WIRD MIT BINAERER SUCHE, NICHT MIT EINER MENGE: Ein
            // HashSet ueber alle Woerter haelt bei 300 000 Woertern zweistellige
            // Megabytes -- und zwar genau in der Spitze des Indexaufbaus, in der
            // auf alten Geraeten der Speicher knapp wird. Am 21.08.2026 sind die
            // beiden Grenzwert-Tests genau daran gestorben. terms ist sortiert,
            // also kostet die Frage nichts.
            fun istSchonWort(wort: String): Boolean {
                var low = 0
                var high = terms.size - 1
                while (low <= high) {
                    val mid = (low + high) / 2
                    val c = terms[mid].compareTo(wort)
                    if (c == 0) return true
                    if (c < 0) low = mid + 1 else high = mid - 1
                }
                return false
            }
            val paareOhne = ArrayList<Pair<String, Int>>(ohneUmlautZuWort.size)
            for ((ohne, nummer) in ohneUmlautZuWort) {
                // Gibt es die umlautfreie Schreibweise ohnehin als eigenes Wort,
                // findet sie die normale Suche schon -- dann waere der Eintrag
                // hier nur Ballast.
                if (istSchonWort(ohne)) continue
                paareOhne.add(ohne to rangVon[nummer])
            }
            paareOhne.sortBy { it.first }
            val termsOhne = Array(paareOhne.size) { paareOhne[it].first }
            val zeigtAuf = IntArray(paareOhne.size) { paareOhne[it].second }

            return SearchIndex(
                terms, start, trefferDoc, trefferGewicht, docs, volltext, termsOhne, zeigtAuf,
            )
        }
    }
}
