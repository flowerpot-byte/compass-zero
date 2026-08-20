package org.compasszero.karte

/**
 * Die festen Groessen des Kartenformats. Der Aufbau der Datei steht in
 * `docs/KARTEN-FORMAT.md`.
 *
 * Die Reihenfolge der Sorten und Punktarten ist Teil des Dateiformats. Sie
 * darf wachsen, aber nie umsortiert werden -- sonst zeigt eine aeltere Karte
 * Gipfel, wo Brunnen stehen.
 */
object Kartenformat {

    val KENNUNG = byteArrayOf(
        'C'.code.toByte(), 'Z'.code.toByte(), 'K'.code.toByte(), 'A'.code.toByte(),
        'R'.code.toByte(), 'T'.code.toByte(), 'E'.code.toByte(), '1'.code.toByte(),
    )

    const val FASSUNG = 1
    const val KOPF_BYTES = 40
    const val EINTRAG_BYTES = 21

    /** Einheiten je Kachelkante. */
    const val RASTER = 4096

    /** Ueberstand, um den eine Kachel ueber ihren Rand hinaus gefuellt ist. */
    const val RAND = 64

    /**
     * Groesste Kachel, die noch angenommen wird -- entpackt.
     *
     * Eine Kachel kommt aus einer Datei, die von aussen ins Geraet gelangt.
     * Ohne Grenze kann eine gefaelschte Kachel den Speicher fuellen, bevor
     * irgendeine Pruefung greift. Gemessen ist die groesste echte Kachel
     * deutlich kleiner; die Grenze liegt bewusst weit darueber, damit sie
     * keine gueltige Karte ablehnt.
     */
    const val KACHEL_MAX_BYTES = 8 * 1024 * 1024

    /** Groesster Wert, den ein Laengenfeld einer Kachel tragen darf. */
    const val KACHEL_MAX_OBJEKTE = 500_000
    const val KACHEL_MAX_PUNKTE = 2_000_000
    // Grosszuegig: Die Grenze soll eine erfundene Zahl abfangen, nicht eine
    // ungewoehnliche echte Karte ablehnen. Am 04.08.2026 stand sie bei 20 000
    // und hat eine gueltige Kachel abgewiesen -- der Fehler lag im Bauwerkzeug,
    // die Grenze war trotzdem zu knapp gewaehlt.
    const val KACHEL_MAX_NAMEN = 65_536
    const val NAME_MAX_BYTES = 240

    object Art {
        const val LINIE = 0
        const val AUSSENRING = 1
        const val PUNKT = 2
        const val INNENRING = 3
    }

    /** Reihenfolge wie in `tools/karte/auslesen.py`. Darf wachsen, nie umsortiert werden. */
    val SORTEN = arrayOf(
        "wasser", "fluss", "bach", "wald", "offen", "sumpf", "gletscher",
        "siedlung", "weg-haupt", "weg-neben", "weg-pfad", "weg-fein",
        "punkt", "ort", "grenze", "grenze-region",
    )

    const val WASSER = 0
    const val FLUSS = 1
    const val BACH = 2
    const val WALD = 3
    const val OFFEN = 4
    const val SUMPF = 5
    const val GLETSCHER = 6
    const val SIEDLUNG = 7
    const val WEG_HAUPT = 8
    const val WEG_NEBEN = 9
    const val WEG_PFAD = 10
    const val WEG_FEIN = 11
    const val PUNKT = 12
    const val ORT = 13
    const val GRENZE = 14
    const val GRENZE_REGION = 15

    /** Reihenfolge wie `ARTEN` in `tools/karte/sorten.py`. */
    val PUNKTARTEN = arrayOf(
        "unbekannt",
        "quelle", "brunnen", "trinkwasser", "wasserturm",
        "gipfel", "sattel", "pass", "hoehle",
        "huette", "unterstand", "aussicht",
        "krankenhaus", "apotheke", "notruftelefon",
        "grossstadt", "stadt", "dorf", "weiler", "einzellage",
    )

    fun sorteName(nummer: Int): String =
        if (nummer in SORTEN.indices) SORTEN[nummer] else "unbekannt"

    fun punktartName(nummer: Int): String =
        if (nummer in PUNKTARTEN.indices) PUNKTARTEN[nummer] else "unbekannt"
}

/** Ein Kartenfehler, der die Datei als unbrauchbar ausweist. */
class Kartenfehler(nachricht: String) : Exception(nachricht)
