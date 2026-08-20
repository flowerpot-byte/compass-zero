package org.compasszero.content

// Ein Text gilt nur dann als brauchbar, wenn ein Mensch ihn wirklich lesen kann.
// Ohne diese Pruefung liesse sich die Quellenpflicht mit unsichtbaren Zeichen
// aushebeln: ein Tipp mit "Quelle" aus Fuellzeichen saehe geprueft aus und waere
// in Wahrheit leer. Dasselbe gilt fuer den Warnhinweis einer Bauanleitung.
//
// Gezaehlt wird nur, was Unicode als Buchstabe oder Ziffer fuehrt, abzueglich
// einer Liste bekannter Fuellzeichen, die genau das vortaeuschen. Die Pruefung
// laeuft ueber vollstaendige Codepunkte — eine Pruefung je Zeichenhaelfte wuerde
// alles oberhalb der Grundebene ungeprueft durchwinken.
object Texts {

    // Zeichen, die Unicode als Buchstaben fuehrt, die aber nichts anzeigen.
    private val FUELLZEICHEN = setOf(
        0x115F, // Hangul Choseong Filler
        0x1160, // Hangul Jungseong Filler
        0x3164, // Hangul Filler
        0xFFA0, // Halfwidth Hangul Filler
        0x2800, // Braille-Muster ohne Punkte
        0x180E, // mongolischer Vokaltrenner
        0xFFFD, // Ersatzzeichen aus kaputten Daten
    )

    // Beim Vereinheitlichen fuer die Suche kann Text laenger werden: eine
    // arabische Ligatur wird zu achtzehn Zeichen. Ein Feld, das dabei um mehr als
    // das Vierfache waechst, ist kein Fliesstext mehr, sondern ein Speicherangriff
    // auf den Suchindex — und darf deshalb gar nicht erst ins Paket.
    const val MAX_AUSDEHNUNG = 4

    // Der feste Zuschlag laesst kurze Felder mit einzelnen stark ausdehnenden
    // Zeichen durch — eine arabische Segensligatur wird allein zu achtzehn
    // Zeichen. Bei langen Feldern bleibt die Vervielfachung die wirksame Grenze.
    const val AUSDEHNUNG_ZUSCHLAG = 512

    fun suchform(value: String): String? {
        val grenze = value.length * MAX_AUSDEHNUNG + AUSDEHNUNG_ZUSCHLAG
        return vereinheitlicheBegrenzt(value, grenze)
    }

    // Mehr als eine Handvoll Zeichen auf einem Grundzeichen ist kein Text mehr.
    // Solche Ketten wuerden ausserdem das stueckweise Aufbereiten aushebeln, weil
    // sie ueber jede Bruchstelle hinweg zusammenwirken.
    private const val MAX_MARKIERUNGEN_AM_STUECK = 8

    // Der Zeilenvorschub ist das EINZIGE Steuerzeichen, das in einem Fliesstext
    // erlaubt sein kann, und auch nur dort -- nicht in Titeln, Schlagwoertern
    // oder Quellenangaben, denn die stehen einzeilig in Listen.
    //
    // Grund fuer die Ausnahme: Ein medizinischer Tipp von zweitausend Zeichen
    // in einem einzigen Absatz ist unter Stress kaum zu erfassen. Am
    // 29.07.2026 ausdruecklich so entschieden.
    //
    // Zugelassen ist ausschliesslich U+000A. Wagenruecklauf, Zeilen- und
    // Absatztrenner (U+2028, U+2029), Vertikaltabulator und Seitenvorschub
    // bleiben verboten: Sie sehen im Text gleich aus, verhalten sich beim
    // Rendern aber unterschiedlich, und mehrere Schreibweisen fuer denselben
    // Umbruch waeren eine Quelle stiller Unterschiede zwischen Paketen.
    private const val ZEILENVORSCHUB = 0x0A

    // Zwei Umbrueche am Stueck sind ein Absatz. Mehr ist kein Absatz mehr,
    // sondern Fuellung -- damit liesse sich ein Tipp optisch leeren, ohne dass
    // die Laengengrenze anschlaegt.
    private const val MAX_UMBRUECHE_AM_STUECK = 2

    fun isUsable(value: String, mindestens: Int = 2, absaetzeErlaubt: Boolean = false): Boolean {
        if (suchform(value) == null) return false
        var markierungen = 0
        var umbrueche = 0
        var sichtbar = 0
        var i = 0
        while (i < value.length) {
            val c = value[i]
            val codePunkt: Int
            if (c.isHighSurrogate()) {
                if (i + 1 >= value.length || !value[i + 1].isLowSurrogate()) return false
                codePunkt = 0x10000 + ((c.code - 0xD800) shl 10) + (value[i + 1].code - 0xDC00)
                i += 2
            } else {
                if (c.isLowSurrogate()) return false
                codePunkt = c.code
                i++
            }
            if (codePunkt == ZEILENVORSCHUB) {
                if (!absaetzeErlaubt) return false
                if (++umbrueche > MAX_UMBRUECHE_AM_STUECK) return false
                markierungen = 0
                continue
            }
            umbrueche = 0
            if (codePunkt in FUELLZEICHEN) return false
            // Markierungszeichen der Ebene 14 (U+E0000 bis U+E007F) und der
            // Privatgebrauch der Ebenen 15/16 gehoeren nicht in einen Text. Die
            // Variantenselektoren U+E0100 bis U+E01EF dagegen schon: sie legen in
            // japanischen Namen die richtige Schreibung fest.
            if (codePunkt in 0xE0000..0xE00FF || codePunkt >= 0xF0000) return false
            if (istVerboteneKategorie(codePunkt)) return false
            if (istMarkierung(codePunkt)) {
                if (++markierungen > MAX_MARKIERUNGEN_AM_STUECK) return false
            } else {
                markierungen = 0
            }
            if (istSichtbarerCodepunkt(codePunkt)) sichtbar++
        }
        return sichtbar >= mindestens
    }
}
