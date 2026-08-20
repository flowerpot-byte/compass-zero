package org.compasszero.content

// Alle Obergrenzen fuer Paketinhalte an einer Stelle. Sie schuetzen alte Geraete
// vor Speicherlast und die App vor absichtlich aufgeblaehten Paketen.
object ContentLimits {
    // Bytegrenze allein schuetzt nicht vor Speichernot: entscheidend ist, wie viele
    // Einzelwerte in der Datei stecken. Beide Grenzen wirken vor dem Dekodieren.
    const val MAX_JSON_BYTES = 4 * 1024 * 1024
    const val MAX_JSON_ELEMENTS = 450_000
    const val MAX_JSON_DEPTH = 60
    const val MAX_KINDS = 20
    const val MAX_PROBLEMS = 500
    const val MAX_ID_LENGTH = 80
    const val MAX_PACK_ID_LENGTH = 120
    const val MAX_TITLE_LENGTH = 200
    const val MAX_BODY_LENGTH = 64_000
    const val MAX_SUMMARY_LENGTH = 1_000
    const val MAX_STEP_LENGTH = 4_000
    const val MAX_NOTE_LENGTH = 500
    const val MAX_NAME_LENGTH = 120
    const val MAX_CATEGORY_LENGTH = 40
    const val MAX_KEYWORD_LENGTH = 40
    const val MAX_KEYWORDS = 20
    const val MAX_SOURCES = 10
    const val MAX_TOOLS = 40
    const val MAX_TOOL_LENGTH = 80
    const val MAX_MATERIALS = 100
    const val MAX_STEPS = 100
    const val MAX_SECTIONS = 100
    const val MAX_CHAPTERS = 200
    const val MAX_ITEMS_PER_FILE = 5_000
    const val MAX_TIP_GROUPS = 200
    // Mehr Dringlichkeitsfelder als es gibt, kann ein Eintrag nicht tragen.
    const val MAX_SITUATIONS = 6
    // Der durchsuchbare Text liegt beim Suchen vollstaendig im Speicher. Diese
    // Grenze haelt den Suchindex auf alten Geraeten tragbar und ist gemessen,
    // nicht geschaetzt.
    const val MAX_SUCHTEXT_ZEICHEN = 4_000_000
    // Das Zeichenbudget allein schuetzt nicht: kurze, lauter verschiedene Woerter
    // bleiben weit darunter und sprengen trotzdem das Wortverzeichnis, weil jedes
    // Wort dort einen eigenen Eintrag braucht.
    //
    // Nachgemessen am 28.07.2026 mit einem Paket, das GLEICHZEITIG alle uebrigen
    // Grenzen ausschoepft (5 000 Tipps, 200 Kapitel mit je 10 Abschnitten,
    // 10 000 POIs) und dessen Woerter im ganzen Paket einmalig sind -- der
    // schlimmste Fall zugleich fuer Vorkommen und Verschiedenheit. Bei 96 MB
    // Heap: 596 573 Wortvorkommen und 3 986 078 Suchzeichen laden und bauen den
    // Index sauber durch, mit 17 MB lebendem Modell. Darueber greift die
    // Zeichengrenze, bevor die Wortzahl ueberhaupt kritisch wird.
    //
    // Die frueheren 400 000 stammten aus einer Messung an einem Paket aus lauter
    // Agrarkapiteln; damals starb ein volles Paket schon bei rund 368 000. Den
    // Unterschied machen zwei Korrekturen aus derselben Nacht: Die Felder werden
    // nicht mehr zu Riesenketten verbunden, bevor sie fuer die Suche aufbereitet
    // werden, und die Zuordnung Wort zu Nummer wird vor dem Bau des
    // Verzeichnisses freigegeben.
    //
    // 500 000 seit dem 20.08.2026. Davor 450 000 (17.08.2026), davor 300 000 --
    // und beide Male war die Zahl erreicht, bevor jemand nachgemessen hatte:
    // Ein einziger neuer Eintrag machte das Paket unlesbar, und fertige,
    // gepruefte Eintraege lagen auf Halde.
    //
    // WAS AM 20.08.2026 GEMESSEN WURDE -- und was an der ersten Messung
    // desselben Tages falsch war:
    //
    // Die erste Fassung dieser Begruendung nannte "602 400 Wortvorkommen, jedes
    // Wort verschieden". Das stimmte nicht. Der Wortbaukasten des Messtests
    // hatte vier Stellen zu je 26 Buchstaben, also 456 976 moegliche Woerter.
    // Ab dieser Zahl WIEDERHOLTEN sich die Woerter, und gemessen wurde nicht
    // mehr der teuerste Fall, sondern ein billigerer. Der Baukasten hat seither
    // fuenf Stellen (11,9 Millionen Woerter); die Messung wurde wiederholt.
    //
    // Nachgemessen bei 96 MB Heap, jedes Wort wirklich verschieden, je Stufe
    // viermal:
    //   550 391 / 600 004 / 610 008 / 620 010 Vorkommen -> laedt und indiziert
    //   630 012 und darueber                            -> Speicherueberlauf
    //                                                       beim Bau des Index
    // Die Wand steht also zwischen 620 010 und 630 012.
    //
    // Das echte Europa-Paket liegt bei 477 698 Wortvorkommen, aber nur rund
    // 32 000 VERSCHIEDENEN Woertern -- jedes Wort kommt im Schnitt fuenfzehnmal
    // vor. Sein Wortverzeichnis ist damit rund zwanzigmal kleiner als das des
    // Messpakets bei gleicher Vorkommenszahl; gemessen wurden 35 MB fuer Laden
    // und Index zusammen.
    //
    // 500 000 liegt rund ein Fuenftel unter der gemessenen Wand des teuersten
    // denkbaren Pakets, und der wirkliche Fall ist um ein Vielfaches billiger.
    //
    // WER WEITER ANHEBEN WILL, sollte wissen, dass diese Grenze das FALSCHE misst.
    // Der Speicher haengt an der Zahl der VERSCHIEDENEN Woerter, nicht an der
    // Zahl der Vorkommen. Eine Grenze auf die Verschiedenheit waere naeher an der
    // Sache und liesse echten Inhalt viel weiter wachsen, ohne ein aufgeblaehtes
    // Paket durchzulassen. Das ist eine Aenderung an einer Schutzgrenze und
    // gehoert deshalb entschieden, nicht nebenbei gemacht.
    //
    // Gemessen wird so: ein Paket aus lauter verschiedenen Woertern bauen, bei
    // 96 MB Heap laden UND indizieren. AltgeraetSpeicherTest tut genau das an
    // der jeweils geltenden Grenze.
    const val MAX_SUCHINDEX_WORTVORKOMMEN = 500_000
    const val MAX_POIS = 10_000
    const val MAX_PHRASE_GROUPS = 40
    const val MAX_PHRASES_PER_FILE = 500
    const val MAX_PHRASE_LANGUAGES = 16
    const val MAX_PHRASE_LENGTH = 300
    const val MAX_PHRASE_NOTE_LENGTH = 500

    val ID_PATTERN = Regex("[a-z0-9][a-z0-9-]*")
    val PACK_ID_PATTERN = Regex("[a-z0-9]+(\\.[a-z0-9-]+)+")
    val LANGUAGE_PATTERN = Regex("[a-z]{2,3}(-[A-Za-z0-9]{2,8})*")
    val CATEGORY_PATTERN = Regex("[a-z0-9][a-z0-9-]*")
    // Muss mit einem Buchstaben oder einer Ziffer beginnen, damit "assets/.." und
    // "assets/." gar nicht erst entstehen koennen.
    val ASSET_PATTERN = Regex("assets/[A-Za-z0-9][A-Za-z0-9._-]*")
}
