package org.compasszero.karte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// Dasselbe wie FormatdokuTest im Inhaltsmodul, hier fuer die Karte:
// docs/KARTEN-FORMAT.md ist die Referenz fuer .czk. Wer ein eigenes Werkzeug
// schreibt, richtet sich danach -- eine Zahl, die dort anders steht als im
// Code, kostet ihn eine Karte, die formal richtig ist und trotzdem nicht
// gelesen wird.
//
// Anlass: Am 17.08.2026 standen in docs/PACK-FORMAT.md gleich zwei Zahlen, die
// der Code nicht mehr hergab. Im Kartendokument fehlten die Obergrenzen bis
// dahin ganz; sie stehen jetzt drin und werden hier festgehalten.
class KartenformatDokuTest {

    private fun repoRoot(): File {
        val fromProperty = System.getProperty("compasszero.repoRoot")
        if (fromProperty != null) return File(fromProperty)
        var dir = File(".").absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("settings.gradle.kts nicht gefunden")
        }
        return dir
    }

    /** 500000 -> "500 000", so wie die Zahlen im Dokument gesetzt sind. */
    private fun mitLuecken(wert: Int): String {
        if (wert < 1000) return wert.toString()
        return wert.toString().reversed().chunked(3).joinToString(" ").reversed()
    }

    // Geprueft wird die Wertspalte der Tabellenzeile, nicht die ganze Zeile:
    // In einer Begruendung kann dieselbe Zahl ein zweites Mal stehen, und dann
    // bemerkt eine Zeilensuche die Luege nicht. Genau das ist beim
    // Schwestertest zweimal passiert.
    private fun stehtInWertspalte(doku: String, beschriftung: String, wert: Int): Boolean {
        val schmal = mitLuecken(wert)
        val schreibweisen = listOf(schmal, schmal.replace(' ', ' '), wert.toString())
        return doku.lineSequence()
            .filter { it.contains(beschriftung) && it.trimStart().startsWith("|") }
            .any { zeile ->
                val wertspalte = zeile.split("|").getOrNull(2) ?: ""
                schreibweisen.any { wertspalte.contains(it) }
            }
    }

    @Test
    fun dieObergrenzenInKartenFormatStimmenMitDemCodeUeberein() {
        val doku = File(repoRoot(), "docs/KARTEN-FORMAT.md").readText()
        val erwartet = listOf(
            Triple("Objekte je Kachel", "KACHEL_MAX_OBJEKTE", Kartenformat.KACHEL_MAX_OBJEKTE),
            Triple("Punkte je Kachel", "KACHEL_MAX_PUNKTE", Kartenformat.KACHEL_MAX_PUNKTE),
            Triple("Namen je Kachel", "KACHEL_MAX_NAMEN", Kartenformat.KACHEL_MAX_NAMEN),
            Triple("Bytes je Name", "NAME_MAX_BYTES", Kartenformat.NAME_MAX_BYTES),
        )
        val fehlen = erwartet.filterNot { stehtInWertspalte(doku, it.first, it.third) }
        assertTrue(
            fehlen.isEmpty(),
            "docs/KARTEN-FORMAT.md nennt diese Obergrenzen nicht mit ihrem heutigen Wert: " +
                fehlen.joinToString { "\"${it.first}\" muesste ${it.third} nennen (${it.second})" } +
                ". Entweder die Zahl im Dokument nachziehen oder die Grenze im Code.",
        )
    }

    // Dieselbe Absicherung fuer die Hoehendatei. Sie traegt KEINE Unterschrift,
    // und die Begruendung dafuer haengt genau an diesen Schranken: Eine
    // erfundene Spannweite macht aus flachem Land eine Wand. Wer sie
    // stillschweigend weitet, nimmt der Begruendung den Boden -- deshalb stehen
    // sie jetzt im Dokument und werden hier festgehalten.
    @Test
    fun dieObergrenzenInHoehenFormatStimmenMitDemCodeUeberein() {
        val doku = File(repoRoot(), "docs/HOEHEN-FORMAT.md").readText()
        val erwartet = listOf(
            Triple("Kachelkante", "KANTE_MAX", Hoehenformat.KANTE_MAX),
            Triple("tiefster Wert", "METER_MIN", Hoehenformat.METER_MIN),
            Triple("höchster Wert", "METER_MAX", Hoehenformat.METER_MAX),
        )
        val fehlen = erwartet.filterNot { (beschriftung, _, wert) ->
            doku.lineSequence()
                .filter { it.contains(beschriftung) && it.trimStart().startsWith("|") }
                .any { zeile ->
                    val wertspalte = zeile.split("|").getOrNull(2) ?: ""
                    val zahl = kotlin.math.abs(wert)
                    wertspalte.contains(mitLuecken(zahl)) || wertspalte.contains(zahl.toString())
                }
        }
        assertTrue(
            fehlen.isEmpty(),
            "docs/HOEHEN-FORMAT.md nennt diese Obergrenzen nicht mit ihrem heutigen Wert: " +
                fehlen.joinToString { "\"${it.first}\" muesste ${it.third} nennen (${it.second})" },
        )
    }

    @Test
    fun derKopfUndDasRasterStehenRichtigImDokument() {
        val doku = File(repoRoot(), "docs/KARTEN-FORMAT.md").readText()
        assertTrue(
            doku.contains("Kopf, ${Kartenformat.KOPF_BYTES} Bytes"),
            "Die Kopfgroesse ${Kartenformat.KOPF_BYTES} steht nicht mehr so im Dokument",
        )
        assertTrue(
            doku.contains("${Kartenformat.RASTER} Einheiten je Kachelkante"),
            "Das Kachelraster ${Kartenformat.RASTER} steht nicht mehr so im Dokument",
        )
        assertTrue(
            doku.contains("Eintrag ist ${Kartenformat.EINTRAG_BYTES} Bytes"),
            "Die Eintragsgroesse ${Kartenformat.EINTRAG_BYTES} steht nicht mehr so im Dokument",
        )
        assertTrue(
            doku.contains("Rand von ${Kartenformat.RAND} Einheiten"),
            "Der Ueberstand ${Kartenformat.RAND} steht nicht mehr so im Dokument",
        )
    }
}
