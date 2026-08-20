package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTest {

    private val source = listOf(SourceRef("Beispielquelle", "Abschnitt 1"))

    private val pack = LoadedPack(
        manifest = PackManifest(1, "org.compasszero.test", 1, "de", "Testpaket", 0, listOf("tips")),
        tips = listOf(
            Tip("wasser-abkochen", "Wasser abkochen", "wasser", "Beispieltext mit filter darin.", listOf("trinkwasser"), source),
            Tip("feuer-machen", "Feuer machen", "feuer", "Beispieltext erwaehnt wasser nur beilaeufig.", emptyList(), source),
        ),
        guides = listOf(
            BuildGuide(
                "wasserfilter-bauen", "Wasserfilter bauen", "werkzeug", "Beispielzusammenfassung.",
                listOf(Material("Beispielmaterial")), emptyList(),
                listOf(GuideStep("Beispielschritt.")), 1, source,
            ),
        ),
        agriculture = listOf(
            Chapter("gewaesser-nutzen", "Gewässer nutzen", listOf(Section("Beispiel", "Beispieltext.")), source),
        ),
    )

    private val index = SearchIndex.build(pack)

    @Test
    fun titleBeatsPrefixBeatsBody() {
        val hits = index.search("wasser")
        assertEquals(listOf("wasser-abkochen", "wasserfilter-bauen", "feuer-machen"), hits.map { it.id })
        assertEquals(listOf(6, 5, 2), hits.map { it.score })
    }

    @Test
    fun umlautFoldingWorksBothWays() {
        assertEquals(listOf("gewaesser-nutzen"), index.search("gewässer").map { it.id })
        assertEquals(listOf("gewaesser-nutzen"), index.search("GEWAESSER").map { it.id })
    }

    @Test
    fun allTermsMustMatch() {
        assertEquals(listOf("wasser-abkochen"), index.search("wasser filter").map { it.id })
        assertEquals(emptyList(), index.search("wasser hologramm").map { it.id })
    }

    /**
     * Kommen alle Woerter vor, stehen aber nie im selben Eintrag, gibt es die
     * beste Teilmenge statt nichts.
     *
     * ANLASS, und es war der schlimmste denkbare: "mein kind atmet nicht" fand
     * am 19.08.2026 NICHTS, waehrend "person atmet nicht mehr" den richtigen
     * Eintrag sofort brachte. Ein Wort mehr im Satz entschied darueber, ob das
     * Handbuch antwortet oder schweigt. Von 25 nachgestellten Notfall-Anfragen
     * liefen sieben ins Leere.
     */
    @Test
    fun bestPartialMatchWhenNoDocumentHasEveryWord() {
        // "feuer" steht nur im einen Eintrag, "filter" nur im anderen.
        assertEquals(
            listOf("feuer-machen", "wasser-abkochen"),
            index.search("feuer filter").map { it.id },
        )
    }

    /**
     * Und die Reihenfolge im Teiltreffer: Erst zaehlt, WIE VIELE Woerter ein
     * Eintrag trifft, danach erst das Gewicht. Sonst verdraengt ein einzelnes
     * Wort im Titel zwei Woerter im Text -- und genau das fuehrte bei
     * "kind atmet nicht" auf die Lungenentzuendung statt auf die Wiederbelebung.
     */
    @Test
    fun partialMatchCountsWordsBeforeWeight() {
        // "wasserfilter-bauen" traegt "wasser" im Titel und wiegt damit schwerer
        // als das "filter" im Fliesstext -- trifft aber nur ein Wort von dreien.
        assertEquals(
            listOf("feuer-machen", "wasser-abkochen"),
            index.search("feuer wasser filter").map { it.id },
        )
    }

    /**
     * Der Teiltreffer greift NUR, wenn jedes Wort irgendwo vorkommt. Ein Wort,
     * das im ganzen Handbuch fehlt, ist ein Tippfehler oder gehoert nicht
     * hierher -- dann bleibt die Anfrage leer, statt beliebige Eintraege zum
     * uebrigen Wort auszuwerfen.
     */
    @Test
    fun unknownWordStillYieldsNothing() {
        assertEquals(emptyList(), index.search("wasser hologramm").map { it.id })
        assertEquals(emptyList(), index.search("feuer filter hologramm").map { it.id })
    }

    @Test
    fun keywordsAreIndexed() {
        assertEquals(listOf("wasser-abkochen"), index.search("trinkwasser").map { it.id })
    }

    @Test
    fun midWordSubstringDoesNotMatch() {
        assertTrue(index.search("filter abkochen").isNotEmpty())
        assertEquals(emptyList(), index.search("asser").map { it.id })
    }

    @Test
    fun emptyAndTinyQueriesYieldNothing() {
        assertEquals(emptyList(), index.search(""))
        assertEquals(emptyList(), index.search("  !? "))
        assertEquals(emptyList(), index.search("a"))
    }

    @Test
    fun limitIsRespected() {
        assertEquals(1, index.search("wasser", limit = 1).size)
        assertEquals(3, index.search("wasser", limit = 500).size)
    }

    @Test
    fun otherScriptsAreSearchable() {
        val fremd = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(
                Tip("russisch", "Вода кипячение", "wasser", "Кипятить воду десять минут.", emptyList(), source),
                Tip("tuerkisch", "Suyu kaynatmak", "wasser", "Çakmak taşı kullan.", emptyList(), source),
                Tip("polnisch", "Przegotuj wodę", "wasser", "Woda musi wrzeć.", emptyList(), source),
                Tip("norwegisch", "Sjøvann", "wasser", "Åpen ild er farlig.", emptyList(), source),
            ),
        )
        val fremdIndex = SearchIndex.build(fremd)
        assertEquals(listOf("russisch"), fremdIndex.search("вода").map { it.id })
        assertEquals(listOf("russisch"), fremdIndex.search("кипячение").map { it.id })
        assertEquals(listOf("tuerkisch"), fremdIndex.search("kaynatmak").map { it.id })
        assertEquals(listOf("tuerkisch"), fremdIndex.search("çakmak").map { it.id })
        assertEquals(listOf("polnisch"), fremdIndex.search("wodę").map { it.id })
        assertEquals(listOf("norwegisch"), fremdIndex.search("sjøvann").map { it.id })
        assertEquals(listOf("norwegisch"), fremdIndex.search("åpen").map { it.id })
    }

    @Test
    fun wordOrderDoesNotChangeTheResult() {
        assertEquals(index.search("wasser filter").map { it.id }, index.search("filter wasser").map { it.id })
        assertEquals(index.search("wasser abkoch").map { it.id }, index.search("abkoch wasser").map { it.id })
        assertEquals(listOf("wasser-abkochen"), index.search("trinkw wasser").map { it.id })
        assertEquals(listOf("wasserfilter-bauen"), index.search("wasser bauen").map { it.id })
    }

    @Test
    fun limitZeroYieldsNothing() {
        assertEquals(emptyList(), index.search("wasser", limit = 0))
    }

    @Test
    fun searchStaysFastOnLargePacks() {
        val viele = LoadedPack(
            manifest = pack.manifest,
            tips = (1..3000).map {
                Tip("tipp-$it", "Titel Nummer $it", "kategorie", "Beispieltext Nummer $it mit vielen Woertern darin.", emptyList(), source)
            },
        )
        val grosserIndex = SearchIndex.build(viele)
        // Erst warmlaufen lassen, dann messen. Ohne das steckt die
        // Aufwaermzeit der Laufzeitumgebung im Mittelwert, und der Test
        // schlaegt auf einer ausgelasteten Maschine falsch an -- am
        // 29.07.2026 zweimal, ohne dass sich an der Suche etwas geaendert
        // haette. Die Schwellen bleiben unveraendert; gemessen wird jetzt
        // das, was der Test zu messen behauptet.
        repeat(20) { grosserIndex.search("ti") }
        repeat(20) { grosserIndex.search("zz") }
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        repeat(20) { grosserIndex.search("ti") }
        val proAnfrage = start.elapsedNow().inWholeMicroseconds / 20
        // Die verbleibende Arbeit haengt an der Zahl der Treffer, nicht mehr an der
        // Groesse des Vokabulars — "ti" passt hier auf jeden einzelnen Eintrag.
        assertTrue(proAnfrage < 20_000, "kurzes Praefix darf nicht bremsen, war ${proAnfrage}us")

        val start2 = kotlin.time.TimeSource.Monotonic.markNow()
        repeat(20) { grosserIndex.search("zz") }
        val ohneTreffer = start2.elapsedNow().inWholeMicroseconds / 20
        assertTrue(ohneTreffer < 2_000, "Anfrage ohne Treffer muss sofort zurueckkommen, war ${ohneTreffer}us")
    }

    @Test
    fun deterministicTieBreakByTitle() {
        val twin = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(
                Tip("zwei", "Beispiel Bravo", "kategorie", "Text.", emptyList(), source),
                Tip("eins", "Beispiel Alpha", "kategorie", "Text.", emptyList(), source),
            ),
        )
        val hits = SearchIndex.build(twin).search("beispiel")
        assertEquals(listOf("Beispiel Alpha", "Beispiel Bravo"), hits.map { it.title })
    }

    @Test
    fun gleicherTextUnterschiedlichGespeichertFindetSich() {
        // "Flaeche" mit einem Zeichen gegen "Flaeche" mit Trennzeichen: dieselbe
        // Anzeige, unterschiedliche Bytes. Beide muessen gleich durchsuchbar sein.
        val zusammen = "Dachfl" + 0x00E4.toChar() + "che"
        val zerlegt = "Dachfla" + 0x0308.toChar() + "che"
        val zwei = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(
                Tip("zusammen", zusammen, "wasser", "Regenwasser auffangen.", emptyList(), source),
                Tip("zerlegt", zerlegt, "wasser", "Regenwasser auffangen.", emptyList(), source),
            ),
        )
        val index2 = SearchIndex.build(zwei)
        assertEquals(setOf("zusammen", "zerlegt"), index2.search("dachflaeche").map { it.id }.toSet())
    }

    @Test
    fun tuerkischesGrossesIFindetSich() {
        val tuerkisch = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(Tip("stadt", "İstanbul", "ort", "Beispieltext.", emptyList(), source)),
        )
        val index2 = SearchIndex.build(tuerkisch)
        assertEquals(listOf("stadt"), index2.search("istanbul").map { it.id })
        assertEquals(listOf("stadt"), index2.search("istan").map { it.id })
    }

    @Test
    fun ostasiatischeTexteSindDurchsuchbar() {
        val cjk = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(
                Tip("wasser-cjk", "飲料水の確保", "wasser", "水を十分に沸騰させる。", emptyList(), source),
                Tip("feuer-cjk", "火起こし", "feuer", "乾いた木を使う。", emptyList(), source),
            ),
        )
        val index2 = SearchIndex.build(cjk)
        // Ein einzelnes Zeichen ist dort ein ganzes Wort.
        assertEquals(listOf("wasser-cjk"), index2.search("水").map { it.id })
        assertEquals(listOf("feuer-cjk"), index2.search("火").map { it.id })
        // Und auch mitten im Satz muss etwas auffindbar sein.
        assertEquals(listOf("wasser-cjk"), index2.search("確保").map { it.id })
    }

    @Test
    fun grenzeWirdNichtStillGekappt() {
        val viele = LoadedPack(
            manifest = pack.manifest,
            tips = (1..150).map { Tip("tipp-$it", "Titel $it", "kategorie", "Beispieltext.", emptyList(), source) },
        )
        assertEquals(150, SearchIndex.build(viele).search("titel", limit = 500).size)
    }

    @Test
    fun gemischteAnfrageVerlangtWeiterhinAlleWoerter() {
        // Ein lateinisches Wort in der Anfrage darf nicht verlorengehen, nur weil
        // auch ein ostasiatisches dabei ist.
        val gemischt = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(
                Tip("a", "Wasser 確保", "wasser", "Beispieltext.", emptyList(), source),
                Tip("b", "Feuer 確保", "feuer", "Beispieltext.", emptyList(), source),
            ),
        )
        val index2 = SearchIndex.build(gemischt)
        assertEquals(listOf("a"), index2.search("wasser 確保").map { it.id })
        assertEquals(listOf("a"), index2.search("確保 wasser").map { it.id })
        assertEquals(emptyList(), index2.search("hologramm 確保").map { it.id })
    }

    @Test
    fun halbbreiteZeichenFindenSichWieVollbreite() {
        val katakana = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(Tip("benzin", "ガソリン", "brennstoff", "Beispieltext.", emptyList(), source)),
        )
        val index2 = SearchIndex.build(katakana)
        assertEquals(listOf("benzin"), index2.search("ガソリン").map { it.id })
        assertEquals(listOf("benzin"), index2.search("ｶﾞｿﾘﾝ").map { it.id })
    }

    @Test
    fun ligaturenUndVollbreiteFormenFindenSich() {
        val sonderformen = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(Tip("filter", "Wasserﬁlter bauen", "wasser", "Beispieltext.", emptyList(), source)),
        )
        val index2 = SearchIndex.build(sonderformen)
        assertEquals(listOf("filter"), index2.search("wasserfilter").map { it.id })
        assertEquals(listOf("filter"), index2.search("ｗａｓｓｅｒｆｉｌｔｅｒ").map { it.id })
    }

    @Test
    fun lateinischeFachwoerterInOstasiatischemTextSindAuffindbar() {
        // In japanischem Text steht "pH" ohne Leerzeichen am Nachbarzeichen. Genau
        // solche Fachbegriffe sind im Ernstfall entscheidend.
        val fachlich = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(
                Tip("t1", "pH値を測る", "wasser", "Beispieltext.", emptyList(), source),
                Tip("t2", "UV放射", "wasser", "Beispieltext.", emptyList(), source),
            ),
        )
        val index2 = SearchIndex.build(fachlich)
        assertEquals(listOf("t1"), index2.search("ph").map { it.id })
        assertEquals(listOf("t2"), index2.search("uv").map { it.id })
        assertEquals(listOf("t1"), index2.search("値").map { it.id })
    }

    @Test
    fun rangfolgeGiltAuchImVolltext() {
        val japanisch = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(
                Tip("titel-treffer", "飲料水", "wasser", "Beispieltext ohne das Wort.", emptyList(), source),
                Tip("text-treffer", "Feuer machen", "feuer", "飲料水はここにある。", emptyList(), source),
            ),
        )
        val hits = SearchIndex.build(japanisch).search("飲料水")
        assertEquals(listOf("titel-treffer", "text-treffer"), hits.map { it.id })
        assertTrue(hits[0].score > hits[1].score, "Titeltreffer muss schwerer wiegen: " + hits.map { it.score })
    }

    @Test
    fun koreanischNutztDasWortverzeichnis() {
        // Koreanisch setzt Wortabstaende — es braucht Praefixsuche und Gewichtung.
        val koreanisch = LoadedPack(
            manifest = pack.manifest,
            tips = listOf(
                Tip("k1", "물 끓이기", "wasser", "Beispieltext.", emptyList(), source),
                Tip("k2", "불 피우기", "feuer", "물을 끓여 마셔라.", emptyList(), source),
            ),
        )
        val index2 = SearchIndex.build(koreanisch)
        val hits = index2.search("물")
        assertEquals(listOf("k1", "k2"), hits.map { it.id })
        assertTrue(hits[0].score > hits[1].score, "Titeltreffer muss schwerer wiegen: " + hits.map { it.score })
    }
}
