package org.compasszero.content

import java.text.Normalizer

// NFKC zieht auch die Formen zusammen, die Werkzeuge erzeugen: Ligaturen aus
// PDF-Auszuegen, voll- und halbbreite Zeichen.
internal actual fun vereinheitlicheBegrenzt(text: String, maxAusgabe: Int): String? {
    if (text.length > maxAusgabe) return null
    // Der haeufige Fall: nichts zu tun, nichts zu belegen.
    if (Normalizer.isNormalized(text, Normalizer.Form.NFKC)) return text

    val out = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
        // Stueckweise arbeiten, damit ein Feld voller Ligaturen den Speicher nicht
        // sprengt, bevor die Grenze ueberhaupt geprueft werden kann. Ein Leerzeichen
        // waere die schoenste Bruchstelle, aber es muss auch ohne gehen: sonst
        // wandert ein Feld ohne einen einzigen Abstand am Stueck durch.
        val ende = stueckEnde(text, i)
        // Keine tragfaehige Bruchstelle: Dann wird nicht geraten. An einer
        // ungeprueften Stelle zu trennen ergaebe eine Aufbereitung, die von der
        // Stueckelung abhaengt -- derselbe Text faende sich je nach Laenge selbst
        // nicht mehr. Ein Feld ohne eine einzige tragfaehige Stelle auf
        // zwoelftausend Zeichen ist ohnehin kein Fliesstext.
        if (ende < 0) return null
        val stueck = Normalizer.normalize(text.substring(i, ende), Normalizer.Form.NFKC)
        if (out.length + stueck.length > maxAusgabe) return null
        out.append(stueck)
        i = ende
    }
    return out.toString()
}

// Sucht ab der Wunschlaenge die naechste sichere Bruchstelle. "Sicher" wird nicht
// geraten, sondern nachgerechnet: in einem kleinen Fenster um die Stelle muss das
// getrennte Vereinheitlichen dasselbe ergeben wie das zusammenhaengende. Sonst
// koennten Zeichen ueber die Bruchstelle hinweg zusammenwachsen — koreanische
// Bausteine tun genau das — und derselbe Text saehe je nach Laenge anders aus.
// Liefert -1, wenn es im Suchfenster keine tragfaehige Stelle gibt.
private fun stueckEnde(text: String, start: Int): Int {
    if (start + STUECK >= text.length) return text.length
    var ende = start + STUECK
    val grenze = minOf(text.length, start + STUECK * 4)
    while (ende < grenze) {
        if (bruchstelleTraegt(text, ende)) return ende
        ende++
    }
    return -1
}

private fun bruchstelleTraegt(text: String, ende: Int): Boolean {
    if (text[ende].isLowSurrogate()) return false
    val von = maxOf(0, ende - FENSTER)
    val bis = minOf(text.length, ende + FENSTER)
    val getrennt = Normalizer.normalize(text.substring(von, ende), Normalizer.Form.NFKC) +
        Normalizer.normalize(text.substring(ende, bis), Normalizer.Form.NFKC)
    return getrennt == Normalizer.normalize(text.substring(von, bis), Normalizer.Form.NFKC)
}

private const val FENSTER = 8

private const val STUECK = 4096

private val SICHTBARE_TYPEN = setOf(
    Character.UPPERCASE_LETTER,
    Character.LOWERCASE_LETTER,
    Character.TITLECASE_LETTER,
    Character.MODIFIER_LETTER,
    Character.OTHER_LETTER,
    Character.DECIMAL_DIGIT_NUMBER,
    Character.LETTER_NUMBER,
    Character.OTHER_NUMBER,
)

private val VERBOTENE_TYPEN = setOf(
    Character.CONTROL,
    Character.FORMAT,
    Character.SURROGATE,
    Character.PRIVATE_USE,
    Character.LINE_SEPARATOR,
    Character.PARAGRAPH_SEPARATOR,
)

private val MARKIERUNGS_TYPEN = setOf(
    Character.NON_SPACING_MARK,
    Character.COMBINING_SPACING_MARK,
    Character.ENCLOSING_MARK,
)

internal actual fun istSichtbarerCodepunkt(codePunkt: Int): Boolean =
    Character.getType(codePunkt).toByte() in SICHTBARE_TYPEN

internal actual fun istVerboteneKategorie(codePunkt: Int): Boolean =
    Character.getType(codePunkt).toByte() in VERBOTENE_TYPEN

internal actual fun istMarkierung(codePunkt: Int): Boolean =
    Character.getType(codePunkt).toByte() in MARKIERUNGS_TYPEN
