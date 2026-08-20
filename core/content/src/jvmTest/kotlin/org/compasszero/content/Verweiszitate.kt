package org.compasszero.content

/**
 * Findet die Stellen, an denen ein Eintrag den Titel eines anderen AUSSCHREIBT.
 *
 * Warum es das gibt: Am 03.08.2026 habe ich in den Tipp "Kontaktgifte auf der
 * Haut" einen Verweis auf "Wiederbelebung Erwachsener: druecken, nicht
 * beatmen" geschrieben. Damit stand das Wort "beatmen" im Koerper eines
 * Vergiftungs-Tipps, und die Suche danach fuehrte wieder mitten in die
 * Vergiftung hinein -- genau das, wogegen der Tipp Monate vorher umbenannt
 * worden war. Ich hatte kein einziges Schlagwort angefasst.
 *
 * Ein Querverweis ist eben kein neutraler Zeiger: Der zitierte Titel ist Text
 * im zitierenden Eintrag und wird mitindiziert.
 *
 * Die Handliste in QuerverweiseTest deckt das nicht ab -- sie kennt nur die
 * Paare, die jemand eingetragen hat. Am 03.08.2026 waren das rund 190 von 397
 * ausgeschriebenen Verweisen; vier davon nannten Eintraege, die es nicht mehr
 * gab. Deshalb sammelt das hier ALLE ein, ohne Pflege.
 */
internal object Verweiszitate {

    const val AUF = '„'
    const val ZU = '“'

    // Ein Zitat ist ein Verweis, wenn kurz davor ein Verweiswort steht. Alles
    // andere in Anfuehrungszeichen ist gewoehnliches Zitat (Quellentitel,
    // woertliche Rede) und geht uns nichts an.
    private val VERWEISWORT = Regex(
        "(siehe|Siehe|Tipp|Tipps|Anleitung|Kapitel|steht in|steht im|steht unter)[^$AUF]{0,30}$",
    )

    // "siehe A und B": beim zweiten Zitat steht kein eigenes Verweiswort mehr.
    // Ohne diese Regel blieben 33 der 397 Verweise ungeprueft.
    private val BRUECKE = Regex("^\\s*(und|oder|sowie|,|;)\\s*$")

    /** Bereich im Text (samt Anfuehrungszeichen) und der zitierte Titel. */
    fun finde(text: String): List<Pair<IntRange, String>> {
        val out = ArrayList<Pair<IntRange, String>>()
        var i = 0
        var letztesEnde = -1
        var letztesWarVerweis = false
        while (true) {
            val auf = text.indexOf(AUF, i)
            if (auf < 0) break
            val zu = text.indexOf(ZU, auf + 1)
            if (zu < 0) break
            val inhalt = text.substring(auf + 1, zu)
            var istVerweis = VERWEISWORT.containsMatchIn(text.substring(maxOf(0, auf - 40), auf))
            if (!istVerweis && letztesWarVerweis && letztesEnde in 0..auf) {
                if (BRUECKE.matches(text.substring(letztesEnde, auf))) istVerweis = true
            }
            if (istVerweis && inhalt.length <= ContentLimits.MAX_TITLE_LENGTH) {
                out.add((auf..zu) to inhalt)
            }
            letztesEnde = zu + 1
            letztesWarVerweis = istVerweis
            i = zu + 1
        }
        return out
    }

    /** Der durchsuchbare Fliesstext JEDES Eintrags, egal welcher Art. */
    fun texteJeEintrag(pack: LoadedPack): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (t in pack.tips) out[t.id] = t.body
        for (g in pack.guides) out[g.id] = g.summary + "\n" + g.steps.joinToString("\n") { it.text }
        for (k in pack.agriculture) {
            out[k.id] = k.sections.joinToString("\n") { it.heading + "\n" + it.body }
        }
        return out
    }

    /** Titel -> Kennung, ueber alle Arten. */
    fun titelZuKennung(pack: LoadedPack): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (t in pack.tips) out[t.title] = t.id
        for (g in pack.guides) out[g.title] = g.id
        for (k in pack.agriculture) out[k.title] = k.id
        return out
    }

    /**
     * Die Woerter, die der Eintrag AUS EIGENER KRAFT mitbringt: sein Titel,
     * seine Schlagwoerter und sein Text ohne die ausgeschriebenen Verweise.
     * Alles, was danach nur noch im Zitat steht, ist geliehen.
     */
    fun eigeneWoerter(pack: LoadedPack, id: String, text: String): Set<String> {
        val zitate = finde(text)
        val sb = StringBuilder()
        var pos = 0
        for ((bereich, _) in zitate) {
            sb.append(text, pos, bereich.first)
            pos = bereich.last + 1
        }
        sb.append(text, pos, text.length)

        val tip = pack.tips.firstOrNull { it.id == id }
        if (tip != null) {
            sb.append(' ').append(tip.title)
            for (k in tip.keywords) sb.append(' ').append(k)
        } else {
            val guide = pack.guides.firstOrNull { it.id == id }
            if (guide != null) {
                sb.append(' ').append(guide.title)
            } else {
                pack.agriculture.firstOrNull { it.id == id }?.let { sb.append(' ').append(it.title) }
            }
        }
        return Tokenizer.tokens(sb.toString()).toSet()
    }

    /**
     * Woerter, die NUR aus dem Zitat stammen. Verglichen wird wie in der Suche
     * ueber Wortanfaenge: steht im eigenen Text "waermen", bedient das die
     * Anfrage "waerme" mit, und das Wort ist nicht geliehen.
     */
    fun gelieheneWoerter(zitierterTitel: String, eigene: Set<String>): List<String> =
        Tokenizer.tokens(zitierterTitel).distinct().filter { w -> eigene.none { it.startsWith(w) } }
}
