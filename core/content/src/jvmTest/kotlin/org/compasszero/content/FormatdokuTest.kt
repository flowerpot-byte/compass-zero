package org.compasszero.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// docs/PACK-FORMAT.md ist die verbindliche Referenz fuer .czp -- wer ein Paket
// baut, richtet sich danach und nicht nach ContentLimits.kt. Genau deshalb ist
// eine veraltete Zahl dort schlimmer als eine fehlende: Sie sieht aus wie eine
// Zusage.
//
// Zweimal am 17.08.2026 aufgefallen, beide Male in derselben Tabelle:
//   * "Wortvorkommen im Suchindex je Paket | 400 000" -- der Code liess seit
//     dem 28.07.2026 nur 300 000 zu. Ein Paket nach Vorschrift waere
//     abgewiesen worden.
//   * "Eintraege pro Paket | 10 000" -- diese Grenze gibt es gar nicht.
//     Gezaehlt wird je Datei: tips und guides 5 000, pois 10 000,
//     agriculture 200 Kapitel, phrases 500.
//
// Der Test kann keine Prosa pruefen. Er haelt nur fest, dass die ZAHL dort
// steht, wo sie hingehoert -- das haette beide Faelle gefangen.
class FormatdokuTest {

    private companion object {
        // Das Dokument trennt Tausender mit einem geschuetzten Leerzeichen.
        const val SCHMALES_LEERZEICHEN = ' '
    }

    private fun repoRoot(): File {
        val fromProperty = System.getProperty("compasszero.repoRoot")
        if (fromProperty != null) return File(fromProperty)
        var dir = File(".").absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("settings.gradle.kts nicht gefunden")
        }
        return dir
    }

    /** 300000 -> "300 000", so wie die Zahlen im Dokument gesetzt sind. */
    private fun mitLuecken(wert: Int): String {
        if (wert < 1000) return wert.toString()
        return wert.toString().reversed().chunked(3).joinToString(" ").reversed()
    }

    // Zwei Anlaeufe waren zu lasch, beide blieben bei verfaelschter Zahl gruen:
    //   1. "steht die Zahl irgendwo im Dokument?" -- sie stand noch in einem
    //      anderen Absatz.
    //   2. "steht sie in derselben ZEILE wie die Beschriftung?" -- in der Zeile
    //      zur Wortgrenze steht sie ZWEIMAL, als Tabellenwert und gleich
    //      daneben in der Begruendung.
    // Geprueft wird deshalb die WERTSPALTE, wo es eine gibt; sonst die Zeile.
    private fun stehtRichtig(doku: String, beschriftung: String, wert: Int, spalte: Int?): Boolean {
        val schmal = mitLuecken(wert)
        val schreibweisen = listOf(
            schmal,
            schmal.replace(' ', SCHMALES_LEERZEICHEN),
            wert.toString(),
        )
        return doku.lineSequence()
            .filter { it.contains(beschriftung) }
            .any { zeile ->
                val bereich =
                    if (spalte != null && zeile.trimStart().startsWith("|")) {
                        zeile.split("|").getOrNull(spalte) ?: ""
                    } else {
                        zeile
                    }
                schreibweisen.any { bereich.contains(it) }
            }
    }

    @Test
    fun dieZahlenInPackFormatStimmenMitDenGrenzenImCodeUeberein() {
        val doku = File(repoRoot(), "docs/PACK-FORMAT.md").readText()

        // Beschriftung im Dokument, Name der Grenze, Wert, Spalte (null = Zeile).
        val erwartet = listOf(
            Erwartung("Wortvorkommen im Suchindex", "MAX_SUCHINDEX_WORTVORKOMMEN",
                ContentLimits.MAX_SUCHINDEX_WORTVORKOMMEN, 2),
            Erwartung("Durchsuchbarer Text je Paket", "MAX_SUCHTEXT_ZEICHEN",
                ContentLimits.MAX_SUCHTEXT_ZEICHEN, 2),
            Erwartung("JSON-Einzelwerte je Datei", "MAX_JSON_ELEMENTS",
                ContentLimits.MAX_JSON_ELEMENTS, 2),
            Erwartung("JSON-Verschachtelungstiefe", "MAX_JSON_DEPTH",
                ContentLimits.MAX_JSON_DEPTH, 2),
            Erwartung("Einträge je Inhaltsdatei", "MAX_ITEMS_PER_FILE",
                ContentLimits.MAX_ITEMS_PER_FILE, 2),
            Erwartung("Einträge je Inhaltsdatei", "MAX_POIS", ContentLimits.MAX_POIS, 2),
            Erwartung("Einträge je Inhaltsdatei", "MAX_CHAPTERS", ContentLimits.MAX_CHAPTERS, 2),
            Erwartung("Einträge je Inhaltsdatei", "MAX_PHRASES_PER_FILE",
                ContentLimits.MAX_PHRASES_PER_FILE, 2),
            Erwartung("Kennungen (`id`)", "MAX_ID_LENGTH", ContentLimits.MAX_ID_LENGTH, null),
            Erwartung("Paket-Kennung", "MAX_PACK_ID_LENGTH", ContentLimits.MAX_PACK_ID_LENGTH, null),
            Erwartung("Anzeigename", "MAX_TITLE_LENGTH", ContentLimits.MAX_TITLE_LENGTH, null),
        )

        val fehlen = erwartet.filterNot { stehtRichtig(doku, it.beschriftung, it.wert, it.spalte) }
        assertTrue(
            fehlen.isEmpty(),
            "docs/PACK-FORMAT.md nennt diese Grenzen nicht mehr mit ihrem heutigen Wert: " +
                fehlen.joinToString { "\"${it.beschriftung}\" muesste ${it.wert} nennen (${it.name})" } +
                ". Wer ein Paket nach dem Dokument baut, bekaeme es abgewiesen -- " +
                "entweder die Zahl im Dokument nachziehen oder die Grenze im Code.",
        )
    }

    private data class Erwartung(
        val beschriftung: String,
        val name: String,
        val wert: Int,
        val spalte: Int?,
    )
}
