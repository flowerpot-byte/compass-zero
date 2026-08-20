package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Ein Paket kann alle Regeln einhalten, sauber laden -- und trotzdem Inhalt
// enthalten, den die Suche nie findet. Solange die Felder erst verbunden und
// dann fuer die Suche aufbereitet wurden, war genau das moeglich: Das
// Ausdehnungsbudget faellt beim Verbund nur einmal an statt je Feld, und ein
// Verbund, der es reisst, verschwand stumm aus dem Wortverzeichnis.
//
// Fuer ein Notfallhandbuch ist "Inhalt da, aber nicht auffindbar" der
// gefaehrlichere Fehler, weil er sich nirgends meldet.
class SuchindexVollstaendigkeitTest {

    private val quelle = listOf(SourceRef("Beispielquelle", "Abschnitt 1"))

    private fun manifest(arten: List<String>) =
        PackManifest(1, "org.compasszero.test", 1, "de", "Testpaket", 0, arten)

    // Ein Feld, das sich beim Vereinheitlichen stark ausdehnt, aber fuer sich
    // allein sicher unter der Grenze bleibt: die arabische Segensligatur wird zu
    // achtzehn Zeichen.
    //
    // Die Zahl ist gerechnet, nicht geraten. Bei n Ligaturen und m Zeichen
    // Klartext gilt fuer ein Einzelfeld 18n + m <= 4(n+m) + 512, also
    // n <= (3m + 512)/14 — bei m = 11 sind das 38. Werden drei solche Felder
    // erst verbunden, faellt der Zuschlag von 512 nur einmal an:
    // n <= (9m + 512)/42, also 14. Genau in dieser Luecke zwischen 14 und 38
    // verschwand der Inhalt frueher stumm aus der Suche.
    private fun dehnbaresFeld(merkwort: String, ligaturen: Int = 30): String =
        "ﷺ".repeat(ligaturen) + " " + merkwort

    @Test
    fun jedesFeldEinesKapitelsBleibtAuffindbar() {
        val pack = LoadedPack(
            manifest = manifest(listOf("agriculture")),
            agriculture = listOf(
                Chapter(
                    "boden", "Boden vorbereiten",
                    listOf(
                        Section("Erster Abschnitt", dehnbaresFeld("bodenprobe")),
                        Section("Zweiter Abschnitt", dehnbaresFeld("kompostgabel")),
                        Section("Dritter Abschnitt", dehnbaresFeld("saatgutlager")),
                    ),
                    quelle,
                ),
            ),
        )
        // Erst pruefen, dass so ein Paket ueberhaupt geladen wuerde -- sonst
        // prueft der Test etwas, das die App nie zu sehen bekaeme.
        for (abschnitt in pack.agriculture.single().sections) {
            assertTrue(Texts.isUsable(abschnitt.body), "das Feld waere gar nicht zulaessig")
        }

        val index = SearchIndex.build(pack)
        for (wort in listOf("bodenprobe", "kompostgabel", "saatgutlager")) {
            assertEquals(
                listOf("boden"), index.search(wort).map { it.id },
                "\"$wort\" steht im Paket, ist aber ueber die Suche nicht auffindbar",
            )
        }
    }

    @Test
    fun jederSchrittEinerAnleitungBleibtAuffindbar() {
        val pack = LoadedPack(
            manifest = manifest(listOf("guides")),
            guides = listOf(
                BuildGuide(
                    "wasserfilter", "Wasserfilter bauen", "wasser", "Kurzfassung.",
                    listOf(Material("Sand")), emptyList(),
                    listOf(
                        GuideStep(dehnbaresFeld("kiesschicht")),
                        GuideStep(dehnbaresFeld("holzkohle")),
                        GuideStep(dehnbaresFeld("auffangbehaelter")),
                    ),
                    1, quelle,
                ),
            ),
        )
        val index = SearchIndex.build(pack)
        for (wort in listOf("kiesschicht", "holzkohle", "auffangbehaelter")) {
            assertEquals(
                listOf("wasserfilter"), index.search(wort).map { it.id },
                "\"$wort\" steht im Paket, ist aber ueber die Suche nicht auffindbar",
            )
        }
    }

    @Test
    fun jedesSchlagwortEinesTippsBleibtAuffindbar() {
        val pack = LoadedPack(
            manifest = manifest(listOf("tips")),
            tips = listOf(
                Tip(
                    "wasser-abkochen", "Wasser abkochen", "wasser", "Beispieltext.",
                    listOf("trinkwasser", "entkeimung", "hochgebirge"), quelle,
                ),
            ),
        )
        val index = SearchIndex.build(pack)
        for (wort in listOf("trinkwasser", "entkeimung", "hochgebirge")) {
            assertEquals(listOf("wasser-abkochen"), index.search(wort).map { it.id }, wort)
        }
    }

    // Gross- und Kleinschreibung gibt es auch oberhalb der Grundebene. Wird dort
    // nicht kleingeschrieben, findet eine kleingeschriebene Anfrage den
    // grossgeschriebenen Text nicht -- und zwar lautlos.
    @Test
    fun auchZeichenOberhalbDerGrundebeneWerdenKleingeschrieben() {
        // Deseret: U+10400 ist der Grossbuchstabe zu U+10428.
        val gross = "𐐀𐐁"
        val klein = "𐐨𐐩"
        assertEquals(
            Tokenizer.suchform(klein), Tokenizer.suchform(gross),
            "gross und klein geschriebener Text ergibt unterschiedliche Suchformen",
        )

        val pack = LoadedPack(
            manifest = manifest(listOf("tips")),
            tips = listOf(Tip("t1", "Titel", "wasser", "Beispieltext $gross dahinter.", emptyList(), quelle)),
        )
        val index = SearchIndex.build(pack)
        assertEquals(
            listOf("t1"), index.search(klein).map { it.id },
            "kleingeschriebene Anfrage findet den grossgeschriebenen Text nicht",
        )
    }

    // Die Mengenpruefung vor dem Laden und der spaetere Indexaufbau muessen
    // dieselben Felder sehen. Weichen sie ab, laesst die eine ein Paket durch,
    // an dem die andere scheitert -- oder umgekehrt.
    @Test
    fun mengenpruefungUndIndexaufbauSehenDieselbenFelder() {
        val pack = LoadedPack(
            manifest = manifest(listOf("tips", "guides", "agriculture")),
            tips = listOf(
                Tip("t1", "Titel eins", "wasser", "Fliesstext eins.", listOf("alpha", "beta"), quelle),
            ),
            guides = listOf(
                BuildGuide(
                    "g1", "Titel zwei", "werkzeug", "Kurzfassung zwei.",
                    listOf(Material("Material")), emptyList(),
                    listOf(GuideStep("Schritt eins."), GuideStep("Schritt zwei.")), 1, quelle,
                ),
            ),
            agriculture = listOf(
                Chapter("a1", "Titel drei", listOf(Section("Kopf", "Rumpf eins."), Section("Kopf zwei", "Rumpf zwei.")), quelle),
            ),
        )

        val index = SearchIndex.build(pack)
        // Jedes eingespeiste Wort muss sich wiederfinden lassen.
        for ((wort, id) in listOf(
            "alpha" to "t1", "beta" to "t1", "fliesstext" to "t1",
            "kurzfassung" to "g1", "schritt" to "g1",
            "rumpf" to "a1", "kopf" to "a1",
        )) {
            assertTrue(index.search(wort).any { it.id == id }, "\"$wort\" fehlt im Verzeichnis von $id")
        }
    }
}
