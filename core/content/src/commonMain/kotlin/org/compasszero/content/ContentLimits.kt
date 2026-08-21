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
    // Grenze haelt den Suchindex auf alten Geraeten tragbar.
    //
    // AM 21.08.2026 VON 4 000 000 AUF 4 500 000 ANGEHOBEN -- und dabei stellte
    // sich heraus, dass die alte Zahl zwar "gemessen" hiess, aber nie ein Test
    // sie zusammen mit dem vollen Wortschatz ausgereizt hat.
    // einPaketAnAllenGrenzenLaedtUndIndiziert fuellt agriculture mit kurzen
    // Woertern und bleibt bei rund einem Drittel des Zeichenbudgets.
    //
    // Jetzt gemessen, bei 96 MB Heap und 299 003 verschiedenen Woertern, mit
    // einem Paket aus zwei Dateien (ein einzelnes agriculture.json waere
    // groesser als MAX_JSON_BYTES):
    //   3 712 400 Zeichen / 467 200 Vorkommen -> laedt und indiziert
    //   4 472 400 Zeichen / 562 200 Vorkommen -> laedt und indiziert
    //   4 792 400 Zeichen / 602 200 Vorkommen -> laedt und indiziert
    //   5 320 400 Zeichen / 668 200 Vorkommen -> OutOfMemoryError
    //   5 792 400 Zeichen / 727 200 Vorkommen -> OutOfMemoryError
    // Die Wand steht zwischen 4 792 400 und 5 320 400 Zeichen, solange der
    // Wortschatz voll ausgereizt ist. 4 500 000 liegt rund ein Sechstel unter
    // dem letzten Stand, der noch getragen hat.
    //
    // DIE DREI GRENZEN HAENGEN ZUSAMMEN, das war vorher nirgends festgehalten:
    // Wer die Zeichen deckelt, deckelt die Vorkommen mit -- bei kurzen Woertern
    // von fuenf Zeichen passen in 4 500 000 Zeichen hoechstens 750 000
    // Vorkommen. Genau dieser Fall ist am 21.08.2026 mitgemessen worden:
    //   4 412 400 Zeichen / 737 200 Vorkommen / 299 003 verschiedene -> traegt.
    //
    // WER WEITER ANHEBEN WILL, misst mit AltgeraetSpeicherTest.messeAmBudget:
    // Wortlaenge, Woerter je Abschnitt und Woerter je Tipp einstellen, bis die
    // gemeinte Groesse am Anschlag liegt, dann bei 96 MB laden UND indizieren.
    const val MAX_SUCHTEXT_ZEICHEN = 4_500_000
    // Das Zeichenbudget allein schuetzt nicht: kurze, lauter verschiedene Woerter
    // bleiben weit darunter und sprengen trotzdem das Wortverzeichnis, weil jedes
    // Wort dort einen eigenen Eintrag braucht.
    //
    // Am 28.07.2026 nachgemessen mit einem Paket, das GLEICHZEITIG alle uebrigen
    // Grenzen ausschoepft (5 000 Tipps, 200 Kapitel mit je 10 Abschnitten,
    // 10 000 POIs). Bei 96 MB Heap: 596 573 Wortvorkommen und 3 986 078
    // Suchzeichen laden und bauen den Index sauber durch, mit 17 MB lebendem
    // Modell.
    //
    // ACHTUNG, DIESE MESSUNG WAR NICHT DER SCHLIMMSTE FALL, obwohl sie sich
    // dafuer ausgab: Sie behauptete, die Woerter seien im ganzen Paket
    // einmalig. Der Wortbaukasten des Messtests konnte damals aber nur
    // 456 976 verschiedene Woerter bilden (vier Stellen zu je 26 Buchstaben)
    // und wiederholte sich stillschweigend darueber. Von den 596 573
    // Vorkommen waren also rund 140 000 Wiederholungen, und das Wortverzeichnis
    // war entsprechend billiger als angenommen. Gemessen wurde damit ein
    // gemischter Fall, nicht der teuerste. Aufgefallen am 20.08.2026.
    //
    // Die frueheren 400 000 stammten aus einer Messung an einem Paket aus lauter
    // Agrarkapiteln; damals starb ein volles Paket schon bei rund 368 000. Den
    // Unterschied machen zwei Korrekturen aus derselben Nacht: Die Felder werden
    // nicht mehr zu Riesenketten verbunden, bevor sie fuer die Suche aufbereitet
    // werden, und die Zuordnung Wort zu Nummer wird vor dem Bau des
    // Verzeichnisses freigegeben.
    //
    // Die Zahl war 300 000, dann 450 000 (17.08.2026), dann 500 000
    // (20.08.2026) -- und jedes Mal war sie erreicht, bevor jemand nachgemessen
    // hatte: Ein einziger neuer Eintrag machte das Paket unlesbar, und fertige,
    // gepruefte Eintraege lagen auf Halde. Seit dem 20.08.2026 ist sie nur noch
    // die grobe zweite Schranke; die Hauptgrenze zaehlt darunter den Wortschatz.
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
    //
    // AM 21.08.2026 VON 600 000 AUF 750 000 ANGEHOBEN, auf Max' ausdrueckliche
    // Ansage. Warum das jetzt traegt und im Juli nicht:
    //   - Die alte Zahl stammte aus der Zeit, als es NUR diese eine Grenze gab.
    //     Damals war jedes zusaetzliche Vorkommen im teuersten Fall zugleich ein
    //     zusaetzliches VERSCHIEDENES Wort, und genau daran ist der Speicher
    //     gestorben (die Wand lag zwischen 620 010 und 630 012).
    //   - Seit dem 20.08.2026 gibt es die eigene Grenze auf die Verschiedenheit
    //     (300 000). Der teure Fall von damals ist damit gar nicht mehr baubar:
    //     Ein Paket mit 750 000 lauter verschiedenen Woertern wird schon von
    //     MAX_SUCHINDEX_VERSCHIEDENE_WOERTER abgelehnt, lange bevor der Index
    //     gebaut wird.
    //   - Der schlimmste Fall, den es noch gibt, ist beides gleichzeitig am
    //     Anschlag: 300 000 * 130 Byte + 750 000 * 17 Byte = rund 52 MB von
    //     96 MB. Vorher waren es rund 49 MB -- der Zuwachs kostet also gut
    //     2,5 MB, kein Vielfaches.
    // Nachgemessen am 21.08.2026 mit den Tests dieser Datei bei 96 MB Heap:
    //   682 400 Vorkommen (90 % der neuen Grenze) -> laedt mit Vorwarnung
    //   762 400 Vorkommen (ueber der neuen Grenze) -> wird abgelehnt
    //   Paket an allen Grenzen zugleich            -> laedt und indiziert
    //
    // WAS DAMIT NICHT ANGEHOBEN IST: MAX_SUCHTEXT_ZEICHEN. Fuer das echte
    // Europa-Paket ist das seit dem 21.08.2026 die engste Grenze (3,6 von
    // 4 Millionen Zeichen). Eine Anhebung dort braucht eine eigene Messung mit
    // einem Testpaket, das die Zeichen ueber mehrere Dateien verteilt -- ein
    // einzelnes agriculture.json stoesst vorher an MAX_JSON_BYTES.
    const val MAX_SUCHINDEX_WORTVORKOMMEN = 750_000

    // UND DIE GRENZE, DIE WIRKLICH DEN SPEICHER BESCHREIBT: wie viele
    // VERSCHIEDENE Woerter im Wortverzeichnis stehen duerfen.
    //
    // Am 20.08.2026 getrennt gemessen, bei 96 MB Heap und festgehaltener
    // jeweils anderer Groesse:
    //   Wortschatz 100 000 -> 300 000, bei 1 000 000 Vorkommen: +25,9 MB,
    //     also rund 130 Byte je verschiedenem Wort.
    //   Vorkommen 500 000 -> 1 000 000, bei 30 000 verschiedenen Woertern:
    //     +8,4 MB, also rund 17 Byte je Vorkommen.
    // Ein verschiedenes Wort kostet damit ungefaehr das Achtfache eines
    // weiteren Vorkommens. Die alte Grenze zaehlte die billige Groesse und
    // bremste echten Inhalt aus, lange bevor Speichernot drohte: Das
    // Europa-Paket hatte 485 939 Vorkommen, aber nur 32 316 verschiedene
    // Woerter.
    //
    // 300 000 ist der vorsichtige Rand des gemessenen Bereichs (300 000 bis
    // 516 675 tragen mit einem Fuenftel Abstand). Schlimmster Fall an beiden
    // Grenzen zugleich: 300 000 * 130 Byte + 600 000 * 17 Byte = rund 49 MB
    // von 96 MB. Fuer echten Inhalt ist das kein Zaun mehr: Bei 51,7 neuen
    // verschiedenen Woertern je Eintrag reichte es fuer mehrere tausend
    // weitere Eintraege -- vorher greift MAX_SUCHTEXT_ZEICHEN.
    //
    // WER SIE ANHEBEN WILL, misst wieder getrennt: Wortschatz und Vorkommen
    // einzeln veraendern, bei 96 MB laden UND indizieren. AltgeraetSpeicherTest
    // tut genau das an beiden geltenden Grenzen.
    const val MAX_SUCHINDEX_VERSCHIEDENE_WOERTER = 300_000
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
