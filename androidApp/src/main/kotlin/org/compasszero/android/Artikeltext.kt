package org.compasszero.android

/**
 * Liest die Gliederung, die ein Eintragstext schon mit sich traegt.
 *
 * WARUM ES DAS GIBT (Rueckmeldung vom 04.08.2026): "Die Uebersichtlichkeit der
 * einzelnen Artikel ist sehr wichtig, da man sich schnell zurechtfinden muss."
 * Bis dahin kippte die Detailansicht den ganzen `body` in EINE TextView -- bei
 * einem Tipp regelmaessig ueber zweitausend Zeichen am Stueck, ohne jede
 * Stufe. Wer im Ernstfall den einen Absatz sucht, der zaehlt, liest eine Wand.
 *
 * DER INHALT WIRD DAFUER NICHT ANGEFASST. Die Texte tragen ihre Ordnung laengst:
 *   - Absaetze als Leerzeile,
 *   - Betonung in GROSSBUCHSTABEN (Sternchen gibt es nicht, es gibt keinen
 *     Renderer dafuer -- siehe Merkzettel),
 *   - Aufzaehlungen mit Gedankenstrich, Ablaeufe mit "1." am Zeilenanfang.
 * Diese Klasse macht daraus Angaben, aus denen `Bausteine.artikel` die Seite
 * baut. Kein Tipp muss umgeschrieben werden.
 *
 * Hier steht bewusst NUR die Textanalyse und kein einziger View -- so laesst sie
 * sich ohne laufende App pruefen, und genau das tut `ArtikeltextTest`.
 */
object Artikeltext {

    /** Art einer Zeile, nach der die Darstellung sich richtet. */
    enum class Art {
        /** Ganz aus Grossbuchstaben: im Text als Zwischenueberschrift gemeint. */
        UEBERSCHRIFT,

        /** Aufzaehlung oder nummerierter Ablauf -- braucht haengenden Einzug. */
        AUFZAEHLUNG,

        /** Gewoehnlicher Satz, moeglicherweise mit hervorgehobenem Anfang. */
        FLIESSTEXT,
    }

    class Zeile(
        val text: String,
        val art: Art,
        /** Zeichen am Anfang, die hervorgehoben werden (0 = keine). */
        val kicker: Int,
        /** Zeichenbreiten fuer den haengenden Einzug (0 = keiner). */
        val einzug: Int,
        /** Letzte Zeile ihres Absatzes -- danach kommt der groessere Sprung. */
        val absatzende: Boolean,
    )

    private val ABSATZ = Regex("\n[ \t]*\n")

    fun zerlege(text: String): List<Zeile> {
        val aus = ArrayList<Zeile>()
        for (absatz in text.split(ABSATZ)) {
            val zeilen = absatz.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            zeilen.forEachIndexed { nr, roh ->
                val einzug = einzugslaenge(roh)
                val kicker = if (einzug == 0 && nr == 0) kickerLaenge(roh) else 0
                val letzte = nr == zeilen.size - 1
                val rest = roh.drop(kicker).trim()
                when {
                    einzug > 0 ->
                        aus.add(Zeile(roh, Art.AUFZAEHLUNG, 0, einzug, letzte))

                    kicker >= roh.length ->
                        aus.add(Zeile(roh, Art.UEBERSCHRIFT, 0, 0, letzte))

                    // Ein LANGER Vorspann, der mit Doppelpunkt endet, ist eine
                    // Ueberschrift und kein hervorgehobener Satzanfang. Fett
                    // mitten im Absatz ueber zwei Zeilen ist ein Klotz; als
                    // eigene Zeile ist es die Stufe, die man beim Blaettern
                    // sucht. Gemessen am Geraet am 04.08.2026.
                    kicker >= UEBERSCHRIFT_AB && roh[kicker - 1] == ':' && rest.isNotEmpty() -> {
                        aus.add(Zeile(roh.take(kicker), Art.UEBERSCHRIFT, 0, 0, false))
                        aus.add(Zeile(rest, Art.FLIESSTEXT, 0, 0, letzte))
                    }

                    else -> aus.add(Zeile(roh, Art.FLIESSTEXT, kicker, 0, letzte))
                }
            }
        }
        return aus
    }

    /**
     * Wie viele Zeichen der Zeilenanfang einnimmt, wenn die Zeile ein
     * Aufzaehlungs- oder Ablaufpunkt ist -- 0, wenn sie keiner ist.
     */
    fun einzugslaenge(zeile: String): Int {
        for (marke in MARKEN) {
            if (zeile.startsWith(marke)) return marke.length
        }
        val ziffern = zeile.takeWhile { it.isDigit() }
        if (ziffern.isNotEmpty() && ziffern.length <= 2) {
            val rest = zeile.drop(ziffern.length)
            if (rest.startsWith(". ") || rest.startsWith(") ")) return ziffern.length + 2
        }
        return 0
    }

    /**
     * Laenge des hervorzuhebenden Anfangs einer Zeile, 0 wenn es keinen gibt.
     *
     * Gesucht ist der Teil, den der Text selbst durch GROSSSCHREIBUNG
     * auszeichnet, bis zu seinem letzten Satzzeichen vor dem ersten
     * Kleinbuchstaben. Beispiele aus dem Paket:
     *   "DER FEHLER, DER SIE WERTLOS MACHT: sie auflegen und loslassen."
     *      -> bis einschliesslich des Doppelpunkts
     *   "SO WIRD SIE ANGEWENDET, und die Reihenfolge ist der Punkt:"
     *      -> bis einschliesslich des Kommas
     *   "Zuerst der Satz, der ueber allem steht"
     *      -> 0, weil schon das zweite Zeichen klein ist
     *
     * MINDESTENS VIER GROSSBUCHSTABEN, damit ein einzelnes betontes Wort am
     * Zeilenanfang ("NICHT zudecken") nicht zur Ueberschrift wird, und
     * hoechstens 120 Zeichen, damit ein langer Grossbuchstaben-Satz nicht als
     * fetter Block dasteht.
     */
    fun kickerLaenge(zeile: String): Int {
        var buchstaben = 0
        var letzteGrenze = 0
        var i = 0
        while (i < zeile.length) {
            val c = zeile[i]
            if (c.isLetter()) {
                if (c.isLowerCase()) break
                buchstaben++
            } else if (!(c.isDigit() || c == ' ' || c in ZULAESSIG)) {
                break
            }
            if (c in GRENZEN && buchstaben >= MINDEST_BUCHSTABEN) letzteGrenze = i + 1
            i++
        }
        val ganzeZeile = i >= zeile.length && buchstaben >= MINDEST_BUCHSTABEN
        val laenge = if (ganzeZeile) zeile.length else letzteGrenze
        return if (laenge in 1..HOECHSTLAENGE) laenge else 0
    }

    // Ab dieser Laenge wird ein Vorspann zur eigenen Zeile statt fett im Satz.
    private const val UEBERSCHRIFT_AB = 30

    private val MARKEN = listOf("— ", "– ", "- ", "• ")
    private const val MINDEST_BUCHSTABEN = 4
    private const val HOECHSTLAENGE = 120
    private const val ZULAESSIG = ",.:;-–—()/„“\"'!?&%+"
    private const val GRENZEN = ",.:;!?"
}
