package org.compasszero.content

class LoadedPack(
    val manifest: PackManifest,
    val tips: List<Tip> = emptyList(),
    val tipGroups: List<TipGroup> = emptyList(),
    val guides: List<BuildGuide> = emptyList(),
    val agriculture: List<Chapter> = emptyList(),
    val pois: List<Poi> = emptyList(),
    val poiAttribution: String = "",
    val phrases: List<PhraseGroup> = emptyList(),
    val phraseLanguages: List<String> = emptyList(),
    val terms: List<Term> = emptyList(),
    // Die Untergruppen der beiden grossen Bereiche. Stehen hinten, damit jeder
    // bestehende Aufruf gueltig bleibt.
    val guideGroups: List<ContentGroup> = emptyList(),
    val agricultureGroups: List<ContentGroup> = emptyList(),
)

class LoadResult(val pack: LoadedPack?, val problems: List<PackProblem>)

object PackParser {

    // Der durchsuchbare Text landet beim Suchen vollstaendig im Speicher. Ein
    // Paket, das diese Grenze reisst, wuerde die App auf einem alten Geraet beim
    // Aufbau der Suche umbringen — also wird es gar nicht erst geladen, statt
    // spaeter stillschweigend halbe Ergebnisse zu liefern.
    //
    // Zwei unabhaengige Grenzen, weil zwei unabhaengige Angriffe dahinterstehen:
    // Zeichenmenge (viel Text) und Wortvorkommen (viele kurze, verschiedene
    // Woerter). Ein Paket aus lauter kurzen Einmal-Woertern bleibt weit unter dem
    // Zeichenbudget und wuerde trotzdem das Wortverzeichnis sprengen, weil dort
    // jedes Wort einen eigenen Eintrag kostet, nicht jedes Zeichen.
    private fun pruefeSuchtextMenge(
        tips: List<Tip>,
        guides: List<BuildGuide>,
        agriculture: List<Chapter>,
        problems: ProblemLog,
    ) {
        var zeichen = 0L
        // Woerter aus Schriften ohne Wortabstand laufen nie ins Wortverzeichnis
        // (siehe SearchIndex.build) — sie zaehlen deshalb hier auch nicht mit.
        var wortvorkommen = 0L
        // Gezaehlt wird die Fassung, die spaeter wirklich im Speicher liegt: beim
        // Vereinheitlichen kann ein Zeichen zu mehreren werden, und genau die
        // stehen im Suchindex.
        fun zaehle(text: String) {
            val aufbereitet = Tokenizer.suchform(text)
            zeichen += (aufbereitet?.length ?: text.length).toLong()
            if (aufbereitet == null) return
            for (token in Tokenizer.tokensAusSuchform(aufbereitet)) {
                if (!Tokenizer.enthaeltOhneWortabstand(token)) wortvorkommen++
            }
        }
        // Gezaehlt wird Feld fuer Feld, und genau so baut SearchIndex.build auch
        // sein Verzeichnis auf. Trennzeichen kommen keine dazu: Die Felder werden
        // nirgends mehr zu einer Kette verbunden, weil ein Verbund das
        // Ausdehnungsbudget nur einmal bekaeme und dabei ganze Felder stumm aus
        // der Suche fallen wuerden.
        for (tip in tips) {
            zaehle(tip.title)
            zaehle(tip.body)
            tip.keywords.forEach(::zaehle)
        }
        for (guide in guides) {
            zaehle(guide.title)
            zaehle(guide.summary)
            guide.steps.forEach { zaehle(it.text) }
        }
        for (chapter in agriculture) {
            zaehle(chapter.title)
            chapter.sections.forEach { zaehle(it.heading); zaehle(it.body) }
        }
        if (zeichen > ContentLimits.MAX_SUCHTEXT_ZEICHEN) {
            problems.fatal(
                "content-too-large-for-search",
                "pack",
                "$zeichen Zeichen, erlaubt sind ${ContentLimits.MAX_SUCHTEXT_ZEICHEN}",
            )
        }
        if (wortvorkommen > ContentLimits.MAX_SUCHINDEX_WORTVORKOMMEN) {
            problems.fatal(
                "content-too-many-search-terms",
                "pack",
                "$wortvorkommen Wortvorkommen, erlaubt sind ${ContentLimits.MAX_SUCHINDEX_WORTVORKOMMEN}",
            )
        }
    }

    fun parse(files: Map<String, ByteArray>, assetNames: Set<String>): LoadResult {
        val problems = ProblemLog()
        val manifestBytes = files["manifest.json"]
            ?: run {
                problems.fatal("manifest-missing", "manifest.json", "pack has no manifest")
                return LoadResult(null, problems.all)
            }
        val manifest = decodeGuarded<PackManifest>(manifestBytes, "manifest.json", problems)
            ?: return LoadResult(null, problems.all)
        val kinds = ManifestCheck.validate(manifest, problems)

        var tips = emptyList<Tip>()
        var tipGroups = emptyList<TipGroup>()
        var guides = emptyList<BuildGuide>()
        var guideGroups = emptyList<ContentGroup>()
        var agriculture = emptyList<Chapter>()
        var agricultureGroups = emptyList<ContentGroup>()
        var pois = emptyList<Poi>()
        var poiAttribution = ""
        var phrases = emptyList<PhraseGroup>()
        var terms = emptyList<Term>()
        var phraseLanguages = emptyList<String>()

        for (kind in kinds) {
            val name = "content/$kind.json"
            val bytes = files[name]
            if (bytes == null) {
                problems.fatal("content-missing", name, "declared in manifest but not in pack")
                continue
            }
            when (kind) {
                "tips" -> decodeGuarded<TipsFile>(bytes, name, problems)?.let {
                    TipsCheck.validate(it, problems)
                    tips = it.tips
                    tipGroups = it.groups
                }
                "guides" -> decodeGuarded<GuidesFile>(bytes, name, problems)?.let {
                    GuidesCheck.validate(it, problems)
                    guides = it.guides
                    guideGroups = it.groups
                }
                "agriculture" -> decodeGuarded<AgricultureFile>(bytes, name, problems)?.let {
                    AgricultureCheck.validate(it, problems)
                    agriculture = it.chapters
                    agricultureGroups = it.groups
                }
                "pois" -> decodeGuarded<PoisFile>(bytes, name, problems)?.let {
                    pois = PoisCheck.validate(it, problems)
                    poiAttribution = it.attribution
                }
                // Phrasen fliessen absichtlich nicht in pruefeSuchtextMenge und nie
                // in SearchIndex.build ein: Ein Satz wie "Haben Sie Schmerzen?"
                // wuerde in der Woerterliste neben den Notfall-Tipps stehen und
                // deren Treffer verdraengen. Ob Phrasen ueberhaupt in die Suche
                // gehoeren, ist noch nicht entschieden -- das wird erst am
                // fertigen Katalog gemessen.
                "terms" -> decodeGuarded<TermsFile>(bytes, name, problems)?.let {
                    TermsCheck.validate(it, problems)
                    terms = it.terms
                }
                "phrases" -> decodeGuarded<PhrasesFile>(bytes, name, problems)?.let {
                    PhrasesCheck.validate(it, problems)
                    phrases = it.groups
                    phraseLanguages = it.languages
                }
            }
        }

        val expected = kinds.map { "content/$it.json" }.toSet()
        for (name in files.keys.sorted()) {
            if (name.startsWith("content/") && name !in expected) {
                problems.warn("file-ignored", name, "not declared in manifest kinds")
            }
        }

        val referenced = HashSet<String>()
        for (tip in tips) if (tip.image.isNotEmpty()) referenced.add(tip.image)
        for (guide in guides) for (step in guide.steps) if (step.image.isNotEmpty()) referenced.add(step.image)
        for (chapter in agriculture) for (section in chapter.sections) if (section.image.isNotEmpty()) referenced.add(section.image)
        for (ref in referenced.sorted()) {
            if (ref !in assetNames) problems.warn("asset-missing", ref, "referenced image not in pack")
        }
        for (asset in assetNames.sorted()) {
            if (asset !in referenced) problems.warn("asset-unused", asset, "asset never referenced")
        }

        pruefeSuchtextMenge(tips, guides, agriculture, problems)

        if (problems.hasFatal) return LoadResult(null, problems.all)
        return LoadResult(
            LoadedPack(
                manifest, tips, tipGroups, guides, agriculture, pois, poiAttribution,
                phrases, phraseLanguages, terms, guideGroups, agricultureGroups,
            ),
            problems.all,
        )
    }
}
