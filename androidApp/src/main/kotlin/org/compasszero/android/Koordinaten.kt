package org.compasszero.android

import kotlin.math.abs

/**
 * Liest eine Ortsangabe, so wie ein Mensch sie aufschreibt.
 *
 * WARUM ES DAS GIBT (Rueckmeldung vom 18.08.2026): "ich will auch per
 * suchfunktion nach staedten, doerfern, laendern, koordinaten ... suchen
 * koennen." Koordinaten sind davon der Teil, der ohne jedes Verzeichnis
 * auskommt -- er ist reines Rechnen.
 *
 * WARUM SO VIELE SCHREIBWEISEN: Weil eine Koordinate im Ernstfall abgelesen
 * und abgetippt wird -- von einem Zettel, aus einem Funkspruch, von einem
 * anderen Geraet. Die eine Fassung, die jemand gerade zur Hand hat, ist
 * genau die, die er eintippt. Wer ihn zwingt, sie vorher umzurechnen, hat die
 * Suche fuer den einen Fall unbrauchbar gemacht, fuer den sie gebaut wurde.
 *
 * WAS ABSICHTLICH NICHT ERRATEN WIRD: die Reihenfolge ohne Anhaltspunkt.
 * Steht keine Himmelsrichtung dabei, gilt Breite zuerst -- so steht es auf
 * jeder Karte und in jedem Funkverkehr. "13 47" wird deshalb NICHT
 * stillschweigend zu Salzburg umgedreht, nur weil das plausibler waere.
 */
object Koordinaten {

    class Ort(val breite: Double, val laenge: Double)

    private val ZAHL = Regex("""[-+]?\d+(?:[.,]\d+)?""")

    /**
     * Versucht, aus einer Eingabe eine Stelle zu lesen. Gibt null zurueck,
     * wenn nichts Brauchbares darin steht -- lieber nichts als ein Sprung
     * irgendwohin.
     */
    fun lies(eingabe: String): Ort? {
        val text = eingabe.trim()
        if (text.isEmpty()) return null

        val teile = zerlege(text) ?: return null
        val (erste, zweite) = teile
        // Die Himmelsrichtung entscheidet, was Breite und was Laenge ist --
        // sie ist der einzige verlaessliche Anhaltspunkt in der Eingabe.
        val breite = when {
            erste.richtung in "NS" -> erste
            zweite.richtung in "NS" -> zweite
            else -> erste
        }
        val laenge = if (breite === erste) zweite else erste
        // Zwei Angaben derselben Achse sind keine Stelle, sondern ein
        // Tippfehler ("N 47 N 13").
        if (breite.richtung in "EOW" || laenge.richtung in "NS") return null

        val b = breite.wert * vorzeichen(breite.richtung, negativ = 'S')
        val l = laenge.wert * vorzeichen(laenge.richtung, negativ = 'W')
        if (abs(b) > 90.0 || abs(l) > 180.0) return null
        return Ort(b, l)
    }

    private class Teil(val wert: Double, val richtung: Char)

    private fun vorzeichen(richtung: Char, negativ: Char): Double =
        if (richtung == negativ) -1.0 else 1.0

    /**
     * Zerlegt die Eingabe in genau zwei Winkelangaben.
     *
     * Getrennt wird an der Himmelsrichtung, am Semikolon, am Komma oder am
     * Leerraum -- in dieser Reihenfolge, weil das Komma in
     * "47,8 13,05" ein Dezimalzeichen ist und in "47.8, 13.05" ein Trenner.
     */
    private fun zerlege(text: String): Pair<Teil, Teil>? {
        val gross = text.uppercase().replace('É', 'E')
        val stellen = ArrayList<Teil>()

        // Erst die Angaben mit Buchstaben: "47.8N", "N47.8", "47°48'N".
        val mitRichtung = Regex("""([NSEOW])\s*([^NSEOW]+)|([^NSEOW]+?)\s*([NSEOW])""")
        var gefunden = false
        for (treffer in mitRichtung.findAll(gross)) {
            val richtung = (treffer.groupValues[1] + treffer.groupValues[4]).firstOrNull() ?: continue
            val rumpf = treffer.groupValues[2] + treffer.groupValues[3]
            val winkel = winkelAus(rumpf) ?: continue
            stellen.add(Teil(winkel, richtung))
            gefunden = true
        }
        if (gefunden) {
            return if (stellen.size == 2) stellen[0] to stellen[1] else null
        }

        // Sonst zwei Winkel ohne Buchstaben. Das Semikolon trennt immer, das
        // Komma nur, wenn es nicht als Dezimalzeichen gebraucht wird.
        val roh = when {
            ';' in gross -> gross.split(';')
            gross.count { it == ',' } == 1 -> gross.split(',')
            else -> gross.split(Regex("\\s+"))
        }
        val stuecke = roh.map { it.trim() }.filter { it.isNotEmpty() }
        val winkel = stuecke.mapNotNull { winkelAus(it) }
        if (winkel.size == 2) return Teil(winkel[0], ' ') to Teil(winkel[1], ' ')

        // ZWEI, VIER ODER SECHS ZAHLEN, sonst nichts. "47 30 0 13 0 0" sind
        // zwei Angaben in Grad, Minuten, Sekunden -- so steht es auf Zetteln,
        // wenn das Gradzeichen fehlt. Bei drei oder fuenf Zahlen laesst sich
        // die Mitte nicht bestimmen, und Raten waere hier das Schlimmste:
        // Eine falsch geteilte Angabe zeigt auf eine Stelle, die es gibt --
        // nur die falsche.
        val zahlen = ZAHL.findAll(gross)
            .mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }
            .toList()
        if (zahlen.size != 4 && zahlen.size != 6) return null
        val haelfte = zahlen.size / 2
        val ersteHaelfte = winkelAusZahlen(zahlen.subList(0, haelfte)) ?: return null
        val zweiteHaelfte = winkelAusZahlen(zahlen.subList(haelfte, zahlen.size)) ?: return null
        return Teil(ersteHaelfte, ' ') to Teil(zweiteHaelfte, ' ')
    }

    /**
     * Liest einen einzelnen Winkel: dezimal oder in Grad, Minuten, Sekunden.
     *
     * Die Zahlen werden der Reihe nach genommen, nicht die Zeichen dazwischen
     * gedeutet: Auf einem Handy kommt statt des Gradzeichens leicht ein
     * Sternchen oder gar nichts, und "47 48 30" ist dieselbe Angabe wie
     * "47°48'30\"".
     */
    private fun winkelAus(roh: String): Double? {
        val zahlen = ZAHL.findAll(roh)
            .map { it.value.replace(',', '.').toDoubleOrNull() }
            .toList()
        if (zahlen.any { it == null } || zahlen.isEmpty() || zahlen.size > 3) return null
        return winkelAusZahlen(zahlen.filterNotNull())
    }

    private fun winkelAusZahlen(werte: List<Double>): Double? {
        if (werte.isEmpty() || werte.size > 3) return null
        // Minuten und Sekunden duerfen nicht ueber 60 liegen -- sonst ist es
        // keine Winkelangabe, sondern eine verrutschte Zeile.
        if (werte.size >= 2 && (werte[1] < 0 || werte[1] >= 60.0)) return null
        if (werte.size == 3 && (werte[2] < 0 || werte[2] >= 60.0)) return null
        val grad = werte[0]
        val rest = (werte.getOrElse(1) { 0.0 }) / 60.0 +
            (werte.getOrElse(2) { 0.0 }) / 3600.0
        // Das Vorzeichen gehoert zum ganzen Winkel, nicht nur zum Gradanteil:
        // -47°30' ist 47,5 Grad nach Sueden und nicht 46,5.
        return if (grad < 0) grad - rest else grad + rest
    }

    /** Wie die App eine Stelle schreibt -- dieselbe Form wie in der Standzeile. */
    fun schreibe(breite: Double, laenge: Double): String =
        "%.4f°%s %.4f°%s".format(
            abs(breite), if (breite < 0) "S" else "N",
            abs(laenge), if (laenge < 0) "W" else "O",
        )
}
