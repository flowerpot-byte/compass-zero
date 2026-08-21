package org.compasszero.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Haelt fest, wie viel Arbeitsspeicher die Bilder EINES Eintrags belegen,
 * wenn die App ihn anzeigt.
 *
 * WARUM DAS ZAEHLT: Android entpackt ein PNG als ARGB_8888 -- vier Byte je
 * Bildpunkt, unabhaengig davon, wie klein die Datei ist. Eine Tafel mit
 * 1700 mal 1560 Punkten wiegt auf der Platte 356 KB und im Speicher 10,6 MB,
 * das Dreissigfache. Und die Ansicht haelt ALLE Bilder eines Kapitels
 * gleichzeitig, weil jedes in einer eigenen ImageView haengt.
 *
 * Gemessen am 21.08.2026 bei 331 Bildern: der schwerste Eintrag
 * (agrikultur-frischkaese, zwei Tafeln) kam auf 19,1 MB, der Schnitt ueber
 * alle 111 bebilderten Eintraege auf 6,1 MB. Die Halde, gegen die
 * AltgeraetSpeicherTest rechnet, sind 96 MB -- und die teilt sich das Bild
 * mit dem Suchindex.
 *
 * DIE GRENZE IST BEWUSST NAH AM GEMESSENEN WERT. Sie soll nicht Luft lassen,
 * sondern auffallen: Wer ein Kapitel auf drei grosse Tafeln bringt, soll das
 * hier merken und nicht erst auf einem alten Telefon. Wird sie erreicht, gibt
 * es drei Wege -- die Tafel kleiner rechnen, sie auf zwei Kapitel verteilen,
 * oder die Grenze bewusst anheben. Stillschweigend wachsen soll sie nicht.
 */
class BildspeicherTest {

    private companion object {
        /** Vier Byte je Bildpunkt, so entpackt Android ein PNG. */
        const val BYTE_JE_PUNKT = 4

        /** Obergrenze fuer die Bilder EINES Eintrags, in Byte. */
        const val MAX_JE_EINTRAG = 24L * 1024 * 1024
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

    /**
     * Liest Breite und Hoehe aus dem PNG-Kopf.
     *
     * Der Kopf steht fest: acht Byte Signatur, vier Byte Laenge, vier Byte
     * "IHDR", dann Breite und Hoehe als je vier Byte, hoechstwertiges zuerst.
     * Das ganze Bild zu laden waere hier Verschwendung -- gebraucht werden
     * sechzehn Byte.
     */
    private fun masze(datei: File): Pair<Int, Int> {
        val kopf = ByteArray(24)
        datei.inputStream().use {
            val gelesen = it.read(kopf)
            check(gelesen == kopf.size) { "PNG-Kopf zu kurz: ${datei.name}" }
        }
        check(kopf[1] == 'P'.code.toByte() && kopf[2] == 'N'.code.toByte()) {
            "Keine PNG-Datei: ${datei.name}"
        }
        fun zahl(ab: Int): Int =
            (kopf[ab].toInt() and 0xFF shl 24) or
                (kopf[ab + 1].toInt() and 0xFF shl 16) or
                (kopf[ab + 2].toInt() and 0xFF shl 8) or
                (kopf[ab + 3].toInt() and 0xFF)
        return zahl(16) to zahl(20)
    }

    /** Alle "image"-Felder eines Eintrags, ohne den JSON-Baum zu bauen. */
    private fun bilderJeEintrag(datei: File): Map<String, List<String>> {
        val text = datei.readText()
        val ergebnis = LinkedHashMap<String, MutableList<String>>()
        val idMuster = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
        val bildMuster = Regex("\"image\"\\s*:\\s*\"assets/([^\"]+)\"")
        // Die Eintraege stehen der Reihe nach in der Datei: Jedes Bild gehoert
        // zu der zuletzt gelesenen id. Eine id auf oberster Ebene erkennt man
        // daran, dass sie NICHT innerhalb eines Bildpfades steht.
        var aktuell: String? = null
        var stelle = 0
        while (stelle < text.length) {
            val id = idMuster.find(text, stelle)
            val bild = bildMuster.find(text, stelle)
            if (id == null && bild == null) break
            if (bild == null || (id != null && id.range.first < bild.range.first)) {
                aktuell = id!!.groupValues[1]
                stelle = id.range.last + 1
            } else {
                ergebnis.getOrPut(aktuell ?: "(ohne id)") { mutableListOf() }
                    .add(bild.groupValues[1])
                stelle = bild.range.last + 1
            }
        }
        return ergebnis
    }

    @Test
    fun keinEintragSprengtDenBildspeicher() {
        val paket = File(repoRoot(), "content/europe-de/paket")
        val assets = File(paket, "assets")
        val zuGross = mutableListOf<String>()
        var schwerster = 0L
        var schwersterName = ""

        for (datei in listOf("guides.json", "agriculture.json", "tips.json")) {
            val quelle = File(paket, "content/$datei")
            if (!quelle.exists()) continue
            for ((eintrag, bilder) in bilderJeEintrag(quelle)) {
                var summe = 0L
                for (name in bilder) {
                    val bild = File(assets, name)
                    assertTrue(bild.exists(), "Bild fehlt: $name (in $eintrag)")
                    val (breite, hoehe) = masze(bild)
                    summe += breite.toLong() * hoehe * BYTE_JE_PUNKT
                }
                if (summe > schwerster) {
                    schwerster = summe
                    schwersterName = eintrag
                }
                if (summe > MAX_JE_EINTRAG) {
                    zuGross += "$eintrag: ${summe / 1024 / 1024} MB aus ${bilder.size} Bild(ern)"
                }
            }
        }

        assertTrue(schwerster > 0, "Kein einziges Bild gefunden -- der Test misst nichts.")
        assertTrue(
            zuGross.isEmpty(),
            "Diese Eintraege belegen entpackt mehr als ${MAX_JE_EINTRAG / 1024 / 1024} MB " +
                "und koennen ein altes Telefon aus dem Speicher werfen:\n" +
                zuGross.joinToString("\n") { "  $it" } +
                "\nEntweder die Tafeln kleiner rechnen, auf mehrere Eintraege verteilen " +
                "oder die Grenze im Test bewusst anheben.",
        )
        println(
            "Schwerster Eintrag: $schwersterName mit ${schwerster / 1024 / 1024} MB " +
                "(Grenze ${MAX_JE_EINTRAG / 1024 / 1024} MB)",
        )
    }
}
