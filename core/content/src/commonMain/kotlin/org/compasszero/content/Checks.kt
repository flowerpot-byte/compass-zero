package org.compasszero.content

internal object Checks {

    fun id(value: String, where: String, problems: ProblemLog) {
        if (value.length > ContentLimits.MAX_ID_LENGTH || !ContentLimits.ID_PATTERN.matches(value)) {
            problems.fatal("id-invalid", where, value.take(100))
        }
    }

    fun uniqueIds(ids: List<String>, where: String, problems: ProblemLog) {
        val seen = HashSet<String>()
        for (id in ids) {
            if (!seen.add(id)) problems.fatal("id-duplicate", where, id.take(100))
        }
    }

    // Ein einzelnes Zeichen genuegt: in ostasiatischen Schriften ist es ein
    // vollstaendiges Wort und damit ein gueltiger Titel.
    fun title(value: String, where: String, problems: ProblemLog) {
        benennendesFeld(value, ContentLimits.MAX_TITLE_LENGTH, "title-invalid", where, problems)
    }

    // Alles, was etwas benennt statt es zu beschreiben: Titel, Ueberschriften,
    // Schlagwoerter, Material- und Werkzeugnamen. Dieselbe Ein-Zeichen-Regel wie
    // beim Titel -- sonst laedt ein japanisches Paket mit den natuerlichen
    // Ueberschriften Wasser, Feuer, Essen gar nicht erst, und zwar nicht nur das
    // Feld, sondern das ganze Paket.
    //
    // Fliesstext bleibt bei mindestens zwei Zeichen: dort ist ein einzelnes
    // Zeichen kein Wort, sondern ein leeres Feld.
    fun benennendesFeld(value: String, max: Int, code: String, where: String, problems: ProblemLog) {
        if (value.length > max || !Texts.isUsable(value, mindestens = 1)) {
            problems.fatal(code, where, "unlesbar oder laenger als $max")
        }
    }

    fun category(value: String, where: String, problems: ProblemLog) {
        if (value.length > ContentLimits.MAX_CATEGORY_LENGTH || !ContentLimits.CATEGORY_PATTERN.matches(value)) {
            problems.fatal("category-invalid", where, value.take(60))
        }
    }

    // Absaetze sind nur dort erlaubt, wo tatsaechlich Fliesstext steht: im Tipp,
    // im Abschnitt eines Kapitels, in der Zusammenfassung und im Schritt einer
    // Anleitung. NICHT in der Belegangabe einer Quelle -- die steht einzeilig
    // unter dem Text und wuerde sonst den Quellenblock auseinanderreissen.
    fun text(
        value: String,
        max: Int,
        code: String,
        where: String,
        problems: ProblemLog,
        absaetzeErlaubt: Boolean = false,
    ) {
        if (value.length > max || !Texts.isUsable(value, absaetzeErlaubt = absaetzeErlaubt)) {
            problems.fatal(code, where, "unlesbar oder laenger als $max")
        }
    }

    fun optionalText(
        value: String,
        max: Int,
        code: String,
        where: String,
        problems: ProblemLog,
        mindestens: Int = 2,
    ) {
        if (value.isEmpty()) return
        if (value.length > max || !Texts.isUsable(value, mindestens)) {
            problems.fatal(code, where, "unlesbar oder laenger als $max")
        }
    }

    fun sources(list: List<SourceRef>, where: String, problems: ProblemLog) {
        if (list.isEmpty()) {
            problems.fatal("sources-missing", where, "jeder Wissenseintrag braucht mindestens eine belegte Quelle")
            return
        }
        if (list.size > ContentLimits.MAX_SOURCES) {
            problems.fatal("sources-too-many", where, "${list.size}")
        }
        for (source in list) {
            if (source.name.length > ContentLimits.MAX_NAME_LENGTH || !Texts.isUsable(source.name)) {
                problems.fatal("source-invalid", where, source.name.take(100))
                continue
            }
            if (source.detail.isEmpty()) {
                problems.fatal("source-detail-missing", where, source.name.take(100))
                continue
            }
            text(source.detail, ContentLimits.MAX_NOTE_LENGTH, "source-invalid", where, problems)
        }
    }

    fun assetRef(value: String, where: String, problems: ProblemLog) {
        if (value.isNotEmpty() && !ContentLimits.ASSET_PATTERN.matches(value)) {
            problems.fatal("asset-ref-invalid", where, value.take(100))
        }
    }

    // Die Dringlichkeitsfelder eines Eintrags.
    //
    // FATAL UND NICHT NUR EINE WARNUNG: Ein Feld, das es nicht gibt, laesst den
    // Eintrag lautlos von der Startseite fallen -- er stuende dann nur noch
    // ueber die Suche. Genau dieselbe Ueberlegung wie bei einer unbekannten
    // Themengruppe, und bei einem Notfallhandbuch ist das kein
    // Schoenheitsfehler.
    //
    // EINE LEERE LISTE IST ERLAUBT: Ein aelteres Paket kennt das Feld noch
    // nicht, und ein Paket, das deswegen gar nicht mehr laedt, waere schlimmer
    // als eines ohne Kacheln. Dass im deutschen Paket jeder Eintrag ein Feld
    // traegt, sichert ein Test ab, nicht das Format.
    fun situations(values: List<String>, where: String, problems: ProblemLog) {
        if (values.size > ContentLimits.MAX_SITUATIONS) {
            problems.fatal("situations-too-many", where, "${values.size}")
            return
        }
        val seen = HashSet<String>()
        for (value in values) {
            if (value !in Situations.KENNUNGEN) {
                problems.fatal("situation-unknown", where, value.take(100))
            } else if (!seen.add(value)) {
                problems.fatal("situation-duplicate", where, value.take(100))
            }
        }
    }
}
