package org.compasszero.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Jeder Tipp fuer sich kann richtig sein und die Sammlung trotzdem falsch.
// Der Nutzer sieht im Ernstfall genau einen Tipp -- den, den die Suche ihm
// zuerst zeigt. Was im Nachbartipp steht, liest er nicht.
//
// Die Faelle hier stammen alle aus einem Pruefdurchgang am 28.07.2026 und sind
// an der echten Suche ueber das echte Paket gemessen, nicht geschaetzt. Sie
// pruefen nicht Formulierungen, sondern die Gefahr dahinter.
class ZusammenspielTest {

    private fun repoRoot(): File {
        val fromProperty = System.getProperty("compasszero.repoRoot")
        if (fromProperty != null) return File(fromProperty)
        var dir = File(".").absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("settings.gradle.kts nicht gefunden")
        }
        return dir
    }

    // Die Dateiliste hier muss die Arten im Manifest spiegeln: Eine dort
    // genannte Art ohne Datei ergibt "content-missing", eine Datei ohne Art
    // eine "file-ignored"-Warnung. Wer eine Art ergaenzt, zieht hier nach.
    private fun paket(): LoadedPack {
        val ordner = File(repoRoot(), "content/europe-de/paket")
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to File(ordner, "manifest.json").readBytes(),
                "content/tips.json" to File(ordner, "content/tips.json").readBytes(),
                "content/guides.json" to File(ordner, "content/guides.json").readBytes(),
                "content/agriculture.json" to File(ordner, "content/agriculture.json").readBytes(),
                "content/terms.json" to File(ordner, "content/terms.json").readBytes(),
            ),
            emptySet(),
        )
        return result.pack ?: error("Paket laedt nicht: ${result.problems}")
    }

    // Die gefaehrlichste Verwechslung im ganzen Paket: Schnappatmung sieht aus
    // wie Atmung. Wer einen Menschen im Kreislaufstillstand in die Seitenlage
    // legt statt zu druecken, laesst ihn sterben.
    //
    // Deshalb darf kein Tipp die Seitenlage von der Atmung abhaengig machen,
    // ohne zu sagen, was als Atmung zaehlt. Tipps, die nur weiterverweisen
    // ("gehoert in die stabile Seitenlage, siehe den Tipp dazu"), verlangen vom
    // Leser gar keine Beurteilung und sind deshalb nicht betroffen.
    @Test
    fun keinTippLaesstDieSeitenlageAnUngepruefterAtmungHaengen() {
        for (tip in paket().tips) {
            val entscheidetSelbst = tip.body.contains("Seitenlage") &&
                (tip.body.contains("atmet") || tip.body.contains("Atmung"))
            if (!entscheidetSelbst) continue
            assertTrue(
                tip.body.contains("ausreichend") || tip.body.contains("nicht normal"),
                "${tip.id} macht die Seitenlage von der Atmung abhaengig, sagt aber nicht, " +
                    "was als Atmung zaehlt — Schnappatmung wuerde als Atmung durchgehen",
            )
        }
    }

    // Zwei Tipps geben bei Gas das genaue Gegenteil an: einer sagt "so schnell
    // wie moeglich herausholen", der andere "keinesfalls hineingehen". Gemessen
    // liefert die Anfrage "gas" beide mit gleicher Punktzahl. Wer nur einen
    // davon sieht, muss das Unterscheidungsmerkmal darin finden.
    @Test
    fun dieBeidenGasAnweisungenNennenIhreGrenzeSelbst() {
        val tipps = paket().tips.associateBy { it.id }
        val herausholen = tipps.getValue("erste-hilfe-vergiftung-atemwege")
        val nichtHinein = tipps.getValue("erste-hilfe-kohlendioxid")

        assertTrue(
            herausholen.body.contains("Kohlendioxid"),
            "der Rettungs-Tipp nennt die Ausnahme nicht, in der Retten toedlich ist",
        )
        // Das Merkmal, an dem der Nutzer die Ausnahme erkennt, muss dastehen --
        // ein blosser Verweis auf einen Stoffnamen hilft ihm nicht, denn den
        // Stoff sieht und riecht er nicht.
        for (ort in listOf("Gärkeller", "Brunnenschächte", "Höhlen")) {
            assertTrue(
                herausholen.body.contains(ort),
                "der Rettungs-Tipp nennt \"$ort\" nicht — der Nutzer kann die Ausnahme nicht erkennen",
            )
        }
        assertTrue(
            nichtHinein.body.contains("KEINEN eigenmächtigen Rettungsversuch"),
            "der Kohlendioxid-Tipp verbietet den eigenen Rettungsversuch nicht mehr deutlich",
        )
    }

    // Kohlenmonoxid ist der Stoff, der bei Stromausfall aus Grill, Kocher und
    // Notheizung kommt -- also der Kernfall dieser App.
    //
    // Gemessen: Die Wortanfangs-Suche schickte "kohlenmonoxid" und "kohlen" auf
    // den Kohlendioxid-Tipp. Dessen Aussagen (schwerer als Luft, sammelt sich
    // am Boden) gelten fuer Kohlenmonoxid NICHT -- wer danach gebueckt in einen
    // Raum geht, um "unter dem Gas zu bleiben", ist ungeschuetzt.
    //
    // Seit dem 28.07.2026 gibt es eigene Kohlenmonoxid-Tipps. Damit gilt: Die
    // naheliegenden Woerter muessen dort landen, und der Kohlendioxid-Tipp muss
    // die Verwechslung weiterhin ausschliessen -- er bleibt fuer "kohlen" und
    // "kohle" in der Trefferliste.
    @Test
    fun werKohlenmonoxidSuchtWirdNichtStillschweigendAufKohlendioxidGelenkt() {
        val pack = paket()
        val index = SearchIndex.build(pack)
        val tipps = pack.tips.associateBy { it.id }

        // Die Woerter, mit denen jemand in einem laengeren Stromausfall sucht.
        // Ohne sie fand die Suche am 29.07.2026 zu keinem davon etwas, obwohl
        // genau das der Anlassfall dieser App ist. "petroleum" fehlt bewusst:
        // Petroleumoefen stehen in keiner der geprueften Quellen.
        for (
            anfrage in listOf(
                "kohlenmonoxid", "co", "grill", "holzkohle", "gasherd", "heizung", "generator",
                "stromausfall", "notheizung", "kamin", "gastherme", "kocher", "gaskocher",
                "stromerzeuger", "auspuff", "abgase", "ofen", "notstrom",
            )
        ) {
            val treffer = index.search(anfrage, limit = 8).map { it.id }
            assertTrue(
                treffer.any { it.startsWith("erste-hilfe-kohlenmonoxid") },
                "\"$anfrage\" findet keinen Kohlenmonoxid-Tipp, gefunden: $treffer",
            )
        }
        // Die beiden Gase stehen in der Suche nebeneinander; wer beim falschen
        // landet, muss das dort erfahren -- die Eigenschaften des einen gelten
        // fuer das andere nicht.
        for (anfrage in listOf("kohlen", "kohle")) {
            val treffer = index.search(anfrage, limit = 8).map { it.id }
            if ("erste-hilfe-kohlendioxid" !in treffer) continue
            assertTrue(
                tipps.getValue("erste-hilfe-kohlendioxid").body.contains("Kohlenmonoxid ist ein anderes Gas"),
                "\"$anfrage\" fuehrt zum Kohlendioxid-Tipp, und der grenzt Kohlenmonoxid nicht ab",
            )
        }
    }

    // "Hol die Person aus dem Gefahrenbereich" ist bei eingeatmeten Giften
    // richtig -- ausser dort, wo der Helfer damit selbst stirbt. Zwei solche
    // Faelle sind belegt: verrauchte Raeume (DFV) und tiefliegende, geschlossene
    // Raeume mit Kohlendioxid (DRK).
    //
    // Wer nur den Rettungs-Tipp sieht, muss beide Ausnahmen darin finden. Der
    // Fall "rauch" war vorher der schlimmste: Das Wort war selbst hinzugefuegt,
    // stand in keiner Quelle, und der Tipp war damit der einzige Treffer fuer
    // "rauch" -- mit der Aussage "hol die Person heraus".
    @Test
    fun werZumHerausholenAuffordertNenntDieToedlichenAusnahmen() {
        for (tip in paket().tips) {
            if (!tip.body.contains("aus dem Gefahrenbereich")) continue
            val text = tip.body.lowercase()
            // Kohlenmonoxid gehoert dazu, und zwar als der gefaehrlichste der
            // drei: Ein Raum mit Kohlenmonoxid sieht voellig normal aus. Die
            // Aufzaehlung darf deshalb auch nicht abzaehlbar sein ("zwei
            // Ausnahmen") -- eine geschlossene Liste erklaert alles Uebrige
            // stillschweigend fuer betretbar.
            for (ausnahme in listOf("verrauchte", "kohlendioxid", "kohlenmonoxid")) {
                assertTrue(
                    text.contains(ausnahme),
                    "${tip.id} sagt \"hol die Person heraus\", nennt aber die Ausnahme " +
                        "\"$ausnahme\" nicht — dort waere das Hineingehen toedlich",
                )
            }
        }
    }

    // Wer eines dieser Woerter tippt, steht moeglicherweise gerade in dem Raum.
    // Der erste Treffer muss ihm sagen, was zu tun ist -- die Nummer ist das
    // Mindeste. Eine reine Beschreibung an erster Stelle ist wertlos.
    //
    // Gemessen am 29.07.2026: Der Kohlenmonoxid-Tipp "unsichtbar und geruchlos"
    // stand bei JEDEM dieser Woerter oben, weil beide CO-Tipps punktgleich
    // liegen und der alphabetische Ausgleich "u" vor "w" stellt. Er enthielt
    // weder Notruf noch Eigenschutz noch die Aufforderung hinauszugehen.
    // Ein frueherer Test prueft nur "gas" -- dort gewinnt zufaellig der
    // Kohlendioxid-Tipp, und der Fehler blieb unsichtbar.
    @Test
    fun beiGasWoerternNenntDerErsteTrefferDieNummer() {
        val pack = paket()
        val index = SearchIndex.build(pack)
        val tipps = pack.tips.associateBy { it.id }
        for (
            anfrage in listOf(
                "gas", "rauch", "brandrauch", "brennt", "feuer", "qualm", "kohlenmonoxid",
                "kohlendioxid", "co", "grill", "holzkohle", "ofen", "kamin", "gastherme",
                "heizung", "notheizung", "generator", "notstrom", "abgase", "gasherd",
            )
        ) {
            val erster = index.search(anfrage, limit = 5).map { it.id }.firstOrNull()
            assertTrue(erster != null, "\"$anfrage\" findet nichts")
            // Der erste Treffer muss ein TIPP sein. Am 11.08.2026 hat eine neue
            // Bauanleitung ("Holzkohle brennen: der Meiler") bei "holzkohle" den
            // Notfall-Tipp von Platz eins verdraengt -- und dieser Test stuerzte
            // dabei mit "Key ... is missing in the map" ab, statt den Grund zu
            // nennen. Wer eines dieser Woerter tippt, steht moeglicherweise
            // gerade in dem Raum; eine Bauanleitung hilft ihm dort nicht.
            val tip = tipps[erster] ?: throw AssertionError(
                "\"$anfrage\" zeigt zuerst \"$erster\" — das ist kein Tipp, sondern eine " +
                    "Anleitung oder ein Kapitel. Bei diesen Woertern muss an erster Stelle " +
                    "der Notfall-Tipp mit der Nummer stehen. Titel des neuen Eintrags aendern.",
            )
            assertTrue(
                tip.body.contains("112"),
                "\"$anfrage\" zeigt zuerst \"${tip.title}\" — dort steht keine Nummer, " +
                    "also keine Handlung, sondern nur eine Beschreibung",
            )
        }
    }

    // Bei Durchfall ist der Fluessigkeitsersatz die Behandlung, nicht eine
    // Begleitmassnahme -- viele Todesfaelle entstehen durch Austrocknung.
    // Gemessen am 29.07.2026: "durchfall" zeigt den Erkennen-Tipp zuerst, weil
    // beide Tipps punktgleich liegen und "A" vor "D" kommt. Der erste Treffer
    // muss deshalb selbst zum Trinken auffordern, nicht nur beschreiben.
    @Test
    fun beiDurchfallWoerternStehtDasTrinkenGanzOben() {
        val pack = paket()
        val index = SearchIndex.build(pack)
        val tipps = pack.tips.associateBy { it.id }
        for (anfrage in listOf("durchfall", "austrocknung", "dehydriert", "brechdurchfall", "magen")) {
            val treffer = index.search(anfrage, limit = 3).map { it.id }
            assertTrue(treffer.isNotEmpty(), "\"$anfrage\" findet nichts")
            val tip = tipps.getValue(treffer.first())
            assertTrue(
                tip.body.contains("getrunken") || tip.body.contains("Flüssigkeit") ||
                    tip.body.contains("trinken"),
                "\"$anfrage\" zeigt zuerst \"${tip.title}\" — dort steht nicht, dass getrunken wird",
            )
            // Und die eigentliche Behandlung muss unter den ersten drei stehen.
            // Gemessen am 28.07.2026: Nach dem Zufuegen der Zink- und
            // Medikamenten-Tipps rutschte sie auf Platz vier, weil bei
            // Punktgleichstand der Titel alphabetisch entscheidet und beide
            // neuen Titel mit "Durchfall" begannen. Wer drei Treffer sieht,
            // haette die Behandlung nicht mehr gesehen.
            assertTrue(
                "erste-hilfe-durchfall-trinken" in treffer,
                "\"$anfrage\" zeigt den Behandlungs-Tipp nicht unter den ersten drei: $treffer",
            )
        }
    }

    // Der Zucker im Rezept fuer die Trinkloesung steht neben der Warnung vor
    // gezuckerten Getraenken. Gemessen liefert "zucker" beide Tipps. Ohne einen
    // Satz dazu liest sich das als Widerspruch -- und im Zweifel laesst jemand
    // den Zucker weg, der zur Wirkung gehoert.
    @Test
    fun dasRezeptErklaertDenScheinbarenWiderspruchBeimZucker() {
        val tipps = paket().tips.associateBy { it.id }
        val rezept = tipps.getValue("erste-hilfe-trinkloesung-selbst-ansetzen")
        assertTrue(
            rezept.body.contains("kein Widerspruch"),
            "das Rezept grenzt sich nicht gegen die Warnung vor gezuckerten Getränken ab",
        )
        // Und zwar mit dem Grund, den die Quelle selbst liefert -- nicht mit
        // einer Behauptung. Ohne ihn laesst jemand den Zucker weg, und dann
        // nimmt der Darm auch das Salz und das Wasser nicht auf.
        assertTrue(
            rezept.body.contains("wirkungslos"),
            "der belegte Grund fuer den Zucker fehlt",
        )
        // Und die Warnung darf nicht pauschal jeden Zucker verbieten.
        val warnung = tipps.getValue("erste-hilfe-durchfall-nicht-trinken")
        assertTrue(
            warnung.body.contains("gesüßte Getränke") || warnung.body.contains("gesüßte") ||
                warnung.body.contains("Mit Zucker gesüßte Getränke"),
            "die Warnung nennt nicht, dass es um gesüßte GETRÄNKE geht",
        )
    }

    // Die WHO sagt "kein praktischer Nutzen, nie angezeigt" ueber Durchfallmittel
    // ausdruecklich fuer KINDER. Fuer Erwachsene stellt dieselbe Quelle fest,
    // dass stopfende Mittel die Zahl der Stuhlgaenge senken koennen.
    //
    // Am 28.07.2026 stand der Satz ohne diese Grenze im Paket. Das ist keine
    // vorsichtige Verkuerzung, sondern eine Tatsachenbehauptung, die die eigene
    // Quelle widerlegt -- und sie kann einen Erwachsenen ohne Arzt davon
    // abhalten, etwas zu nehmen, das seinen Fluessigkeitsverlust senkt.
    @Test
    fun derMedikamentenTippTrenntKinderVonErwachsenen() {
        val tip = paket().tips.associateBy { it.id }
            .getValue("erste-hilfe-durchfall-medikamente")
        assertTrue(tip.body.contains("Bei Kindern"), "der Tipp nennt den Geltungsbereich Kinder nicht")
        assertTrue(tip.body.contains("Bei Erwachsenen"), "der Tipp sagt nicht, was fuer Erwachsene gilt")
        // Und die Gefahren muessen konkret dastehen, nicht als "gefaehrliche
        // Nebenwirkungen" zusammengefasst -- der zweite Punkt ist der Grund,
        // warum es bei blutigem Durchfall der falsche Moment waere.
        assertTrue(tip.body.contains("Lähmung des Darms"), "die Darmlaehmung fehlt")
        assertTrue(
            tip.body.contains("Erreger langsamer ausgeschieden"),
            "der Hinweis auf die verlaengerte Ansteckung fehlt",
        )
        // Der Titel darf ueber Antibiotika nichts behaupten: Die Quelle nennt
        // drei Faelle, in denen sie verlaesslich helfen.
        assertFalse(
            tip.title.contains("nicht helfen") || tip.title.contains("was nicht hilft"),
            "der Titel behauptet ueber Antibiotika das Gegenteil des eigenen Textes",
        )
    }

    // Der Zink-Tipp empfiehlt einen Vorrat. Dabei stand einmal "Es ist billig
    // und lange haltbar" -- beides steht in keiner Quelle, und die
    // Haltbarkeits-Behauptung haette dazu gefuehrt, dass niemand das
    // Verfallsdatum prueft. Ausserdem ist die Dosis nur mit dem Wort
    // "elementares" eindeutig: Die Zahl auf einer Packung meint nicht immer
    // dasselbe, und Erwachsenentabletten liegen weit ueber dem Bereich.
    @Test
    fun derZinkTippBehauptetNichtsUeberPreisUndHaltbarkeit() {
        val tip = paket().tips.associateBy { it.id }.getValue("erste-hilfe-durchfall-zink")
        for (behauptung in listOf("haltbar", "billig", "günstig")) {
            assertFalse(
                tip.body.contains(behauptung),
                "${tip.id} behauptet \"$behauptung\" — das steht in keiner Quelle",
            )
        }
        assertTrue(tip.body.contains("elementares"), "die Dosis ist ohne \"elementares\" nicht eindeutig")
        assertTrue(tip.body.contains("Kinderpräparat"), "der Hinweis auf das Kinderpraeparat fehlt")
        // Und der Vorratsrat ist eine eigene Uebertragung, keine Quellenaussage.
        assertTrue(
            tip.body.contains("nicht aus der Quelle"),
            "die eigene Einordnung ist nicht als solche gekennzeichnet",
        )
    }

    // Wer nach einem Medikament sucht, hat die Packung in der Hand. Alle Woerter,
    // die auf so einer Packung oder im Sprachgebrauch stehen, muessen zum
    // Medikamenten-Tipp fuehren -- gemessen fanden sie am 28.07.2026 nichts.
    @Test
    fun medikamentenWoerterFuehrenZumMedikamentenTipp() {
        val index = SearchIndex.build(paket())
        for (
            anfrage in listOf(
                "imodium", "loperamid", "durchfallmittel", "kohletabletten", "antibiotika",
                "tabletten", "hausmittel", "blutiger durchfall",
            )
        ) {
            val treffer = index.search(anfrage, limit = 5).map { it.id }
            assertTrue(
                "erste-hilfe-durchfall-medikamente" in treffer,
                "\"$anfrage\" fuehrt nicht zum Medikamenten-Tipp, gefunden: $treffer",
            )
        }
    }

    // "Jede Wunde wird keimfrei bedeckt" ist die Grundregel des Pakets -- und
    // fuer eine offene Brustverletzung genau falsch. Die ERC-Leitlinie 2025 sagt
    // dort: freilassen, keinen Verband, nicht abdecken. Wer nur den allgemeinen
    // Wund-Tipp sieht, verschliesst eine Wunde, die offen bleiben muss.
    @Test
    fun derAllgemeineWundTippNenntDieBrustkorbAusnahme() {
        val tipps = paket().tips.associateBy { it.id }
        val bedecken = tipps.getValue("erste-hilfe-wunde-bedecken")
        assertTrue(
            bedecken.body.contains("Brustkorbs") || bedecken.body.contains("Brustwunde"),
            "der allgemeine Wund-Tipp nennt die Brustkorb-Ausnahme nicht",
        )
        // Kleinschreibung vergleichen: Das Paket betont mit GROSSBUCHSTABEN,
        // seit die Sternchen-Auszeichnung raus ist. Ein Test, der auf die
        // Schreibweise achtet, faellt bei jeder Betonung um.
        val brust = tipps.getValue("erste-hilfe-offene-brustwunde").body.lowercase()
        assertTrue(
            brust.contains("frei") && brust.contains("keinen") && brust.contains("verband"),
            "der Brustwunden-Tipp sagt das Verbot nicht mehr deutlich",
        )
    }

    // Die ERC-Leitlinie 2025 verbietet die Seitenlage nach einem Trauma:
    // "In Situationen wie agonale Atmung oder Trauma duerfen Sie die Person
    // NICHT in Seitenlage bringen." Das Paket deckte die Schnappatmungs-Haelfte
    // dieses Satzes ab und die Trauma-Haelfte gar nicht -- ein Sturz auf den
    // Kopf haette in die Seitenlage gefuehrt.
    //
    // Die Ausnahme muss nicht in jedem Tipp stehen, der die Seitenlage
    // erwaehnt -- das blaehte jeden Vergiftungs- und Durchfall-Tipp auf. Sie
    // gehoert dorthin, wo die Lage tatsaechlich entschieden und ausgefuehrt
    // wird. Alle uebrigen muessen dorthin verweisen, damit niemand die
    // Handgriffe anwendet, ohne die Ausnahme gelesen zu haben.
    @Test
    fun werZurSeitenlageSchicktNenntDieTraumaAusnahme() {
        val tipps = paket().tips.associateBy { it.id }
        val entscheidende = listOf(
            "erste-hilfe-stabile-seitenlage",
            "erste-hilfe-seitenlage-handgriffe",
            "erste-hilfe-entscheidung-nach-atemkontrolle",
        )
        for (kennung in entscheidende) {
            assertTrue(
                tipps.getValue(kennung).body.contains("Wirbelsäule"),
                "$kennung entscheidet ueber die Seitenlage, nennt aber die Trauma-Ausnahme nicht",
            )
        }
        for (tip in tipps.values) {
            if (tip.id in entscheidende) continue
            if (!tip.body.contains("in die stabile Seitenlage")) continue
            if (tip.body.contains("NICHT in die stabile Seitenlage")) continue
            assertTrue(
                tip.body.contains("Stabile Seitenlage: Handgriffe") ||
                    tip.body.contains("siehe den Tipp dazu") ||
                    tip.body.contains("Wirbelsäule"),
                "${tip.id} schickt in die Seitenlage, ohne auf die Handgriffe zu verweisen — " +
                    "dort steht die Trauma-Ausnahme",
            )
        }
    }

    // Das Abbinden hat in derselben Quelle eine ausdrueckliche Gegenanzeige:
    // Beim Schlangenbiss ist es verboten. Ohne diesen Satz waere "abbinden" der
    // erste Treffer fuer genau die Lage, in der es schadet.
    @Test
    fun derAbbindeTippNenntDieGegenanzeige() {
        val tip = paket().tips.associateBy { it.id }.getValue("erste-hilfe-abbinden")
        assertTrue(tip.body.contains("Schlangenbiss"), "die Gegenanzeige Schlangenbiss fehlt")
        // Und die Sackgasse ohne Geraet muss beantwortet sein, sonst greift
        // jemand zum Schnuersenkel.
        assertTrue(
            tip.body.contains("kein Tourniquet"),
            "der Tipp sagt nicht, was ohne Tourniquet gilt",
        )
    }

    // "Ohne Uebung nur druecken" hat zwei Ausnahmen, nicht eine. Eine
    // abzaehlbare Liste erklaert alles Uebrige stillschweigend fuer abgedeckt --
    // dieselbe Falle wie bei den Gas-Tipps.
    @Test
    fun dieRegelOhneUebungNenntBeideAusnahmen() {
        val tip = paket().tips.associateBy { it.id }.getValue("erste-hilfe-nur-druecken")
        assertTrue(tip.body.contains("Kind"), "die Kinder-Ausnahme fehlt")
        assertTrue(tip.body.contains("Ertrinken"), "die Ertrinkens-Ausnahme fehlt")
    }

    // Die Unterzuckerung ist der einzige Notfall im Paket, der sich mit
    // Haushaltsmitteln vollstaendig beheben laesst -- und ihre Zeichen (ploetzlich
    // verwirrt, schwach) teilt sie mit Unterkuehlung, Hitzschlag, Austrocknung
    // und Gehirnerschuetterung. Umso wichtiger ist die Gegenanzeige: Wer nicht
    // reagiert, darf nichts Suesses in den Mund bekommen.
    @Test
    fun derUnterzuckerungsTippNenntDieAspirationsGrenze() {
        val tip = paket().tips.associateBy { it.id }.getValue("erste-hilfe-unterzuckerung")
        assertTrue(
            tip.body.contains("NICHT reagiert"),
            "die Grenze fuer nicht reagierende Personen fehlt",
        )
        assertTrue(tip.body.contains("Atemwege"), "der Grund (Verschlucken) fehlt")
        // Und die Mengen, die den Tipp erst brauchbar machen.
        for (menge in listOf("15 bis 20 Gramm", "50 bis 100 Milliliter", "2,5 Gramm")) {
            assertTrue(tip.body.contains(menge), "die Menge \"$menge\" fehlt")
        }
    }

    // "Schock" meint im Alltagsdeutsch den Kreislaufschock. Gemessen fuehrte die
    // Anfrage zum Defibrillator-Tipp, weil dort der Stromstoss gemeint war.
    @Test
    fun schockFuehrtNichtZumDefibrillator() {
        val erster = SearchIndex.build(paket()).search("schock", limit = 3).map { it.id }.firstOrNull()
        assertTrue(erster != null, "\"schock\" findet nichts")
        assertTrue(
            erster != "erste-hilfe-aed-anwenden",
            "\"schock\" zeigt zuerst den Defibrillator — gemeint ist im Deutschen der Kreislaufschock",
        )
    }

    // Kohlenmonoxid ist nur rund drei Prozent leichter als Luft. Es schichtet
    // sich nicht, es durchmischt -- es gibt keine sichere Stelle im Raum.
    //
    // Am 29.07.2026 stand in den CO-Tipps "es sammelt sich also nicht wie
    // Kohlendioxid am Boden". Der Satz war als Abgrenzung gemeint und hat den
    // Fehler in die Gegenrichtung gebaut: direkt neben der Beschreibung eines
    // Kohlendioxid-"Sees" am Boden liest sich das als "unten ist es besser".
    // Genau davor warnt der Test weiter oben. Deshalb: keine Ortsaussagen ueber
    // Kohlenmonoxid, in keine Richtung.
    @Test
    fun keinTippBehauptetWoKohlenmonoxidImRaumSteht() {
        for (tip in paket().tips) {
            if (!tip.body.contains("Kohlenmonoxid")) continue
            for (behauptung in listOf("leichter als Luft", "sammelt sich nicht", "steigt nach oben")) {
                assertFalse(
                    tip.body.contains(behauptung),
                    "${tip.id} sagt \"$behauptung\" ueber Kohlenmonoxid — jede Ortsaussage " +
                        "legt eine sichere Stelle im Raum nahe, und die gibt es nicht",
                )
            }
        }
    }

    // Wer im Brand "rauch" tippt, muss zuerst lesen, dass er nicht hineingehen
    // darf -- nicht, woran man eine Rauchvergiftung erkennt. Gemessen am
    // 28.07.2026: Ohne das Wort "Rauch" als eigenes Titelwort bekam der
    // Selbstschutz-Tipp keine Titelpunkte ("rauch" ist kein Anfang von
    // "Brandrauch") und landete hinter dem Erkennen-Tipp.
    @Test
    fun beiRauchStehtDerSelbstschutzGanzOben() {
        val pack = paket()
        val erster = SearchIndex.build(pack).search("rauch", limit = 5).map { it.id }.firstOrNull()
        assertTrue(erster != null, "\"rauch\" findet nichts")
        assertTrue(
            erster == "erste-hilfe-brandrauch-nicht-hineingehen",
            "\"rauch\" zeigt zuerst $erster statt des Selbstschutz-Tipps",
        )
    }

    // Wer mitten in einer Wiederbelebung "beatmen" tippt, liest den ersten
    // Treffer und sonst nichts. Fuer einen ungeuebten Erwachsenen-Helfer ist
    // "nur druecken, nicht beatmen" die richtige Antwort -- bei einem Kind
    // waere sie falsch, dort kommen die Beatmungen zuerst.
    //
    // Frueher stand hier bei Punktgleichstand "Kontaktgifte: nicht beatmen"
    // ganz oben, ein Vergiftungs-Tipp mitten in der Wiederbelebung. Seit der
    // Umbenennung faellt er aus der Trefferliste. Bleiben muss aber die Regel
    // dahinter: Was zuerst erscheint und das Beatmen verneint, muss die
    // Kinder-Ausnahme selbst nennen.
    @Test
    fun derErsteTrefferZuBeatmenLaesstDieKinderAusnahmeNichtWeg() {
        val pack = paket()
        val index = SearchIndex.build(pack)
        val tipps = pack.tips.associateBy { it.id }
        val erster = index.search("beatmen", limit = 5).map { it.id }.firstOrNull()
        assertTrue(erster != null, "\"beatmen\" findet gar nichts")
        val tip = tipps.getValue(erster)
        if (!tip.title.contains("nicht beatmen") && !tip.body.contains("nicht beatmen")) return
        assertTrue(
            tip.body.contains("Kind"),
            "\"beatmen\" zeigt zuerst \"${tip.title}\" — der Tipp verneint das Beatmen, " +
                "sagt aber nicht, dass bei Kindern die Beatmungen zuerst kommen",
        )
    }

    // Und der Vergiftungs-Tipp darf dort gar nicht mehr auftauchen.
    @Test
    fun beatmenFuehrtNichtInDieVergiftungsTipps() {
        val treffer = SearchIndex.build(paket()).search("beatmen", limit = 8).map { it.id }
        assertFalse(
            "erste-hilfe-kontaktgift" in treffer,
            "\"beatmen\" fuehrt wieder zum Kontaktgift-Tipp: $treffer",
        )
    }

    // Anleitungen, die eine ansprechbare Person voraussetzen, muessen das sagen.
    // Beim Erbrechen ist der Unterschied entscheidend: Bei einer bewusstlosen
    // Person genuegt das Zur-Seite-Drehen des Kopfes nicht, sie gehoert in die
    // stabile Seitenlage.
    @Test
    fun anleitungenFuerWachePersonenNennenIhrenGeltungsbereich() {
        val tipps = paket().tips.associateBy { it.id }
        val erbrechen = tipps.getValue("erste-hilfe-erbrechen-helfen")
        assertTrue(
            erbrechen.body.contains("bei Bewusstsein"),
            "der Erbrechen-Tipp sagt nicht, dass er eine wache Person voraussetzt",
        )
        assertTrue(
            erbrechen.body.contains("stabile Seitenlage"),
            "der Erbrechen-Tipp sagt nicht, was bei einer bewusstlosen Person gilt",
        )
    }

    // Das Auftauen einer Erfrierung ist die einzige Massnahme im Paket, die
    // ausdruecklich nur gilt, WEIL keine Hilfe kommt. Sie haengt an drei
    // Bedingungen, und die dritte ist die gefaehrlichste: Auftauen und wieder
    // Gefrieren ist schlimmer als durchgehend gefroren. Faellt eine der
    // Bedingungen aus dem Text, wird aus einer Anleitung ein Schaden.
    @Test
    fun dasAuftauenBeiErfrierungenNenntAlleDreiBedingungen() {
        val tip = paket().tips.associateBy { it.id }
            .getValue("erste-hilfe-erfrierungen-versorgen")
        for (bedingung in listOf("zwei Stunden", "wieder gefriert", "ALLE erfüllt")) {
            assertTrue(tip.body.contains(bedingung), "die Bedingung \"$bedingung\" fehlt")
        }
        assertTrue(tip.body.contains("37 bis 39 Grad"), "die Wassertemperatur fehlt")
        // Die gefuehllose Stelle merkt eine Verbrennung nicht. Ohne dieses
        // Verbot nimmt jemand das Feuer, das ohnehin brennt.
        assertTrue(
            tip.body.contains("kein Feuer"),
            "das Verbot anderer Wärmequellen fehlt",
        )
    }

    // Bei der Unterkuehlung ab Stadium II sagte die deutsche Laien-Anleitung
    // pauschal "gar nicht aufwaermen", zwei Fachquellen dagegen "aktiv waermen".
    // Nach dem Nachschlagen in der dritten Quelle ist klar, dass es kein
    // Widerspruch ist, sondern eine Verkuerzung: Gefaehrlich ist Waerme an Armen
    // und Beinen, weil kaltes Blut zum Herzen zurueckkommt. Waerme am
    // Oberkoerper ist richtig.
    //
    // Damit haengt an diesem Tipp eine Unterscheidung, die genau falsch
    // herum gelesen werden kann. Beide Haelften muessen dastehen: das Verbot
    // fuer die Gliedmassen UND die Anweisung fuer den Oberkoerper. Faellt eine
    // weg, wird aus dem Tipp entweder Untaetigkeit oder ein Herzstillstand.
    @Test
    fun derUnterkuehlungsTippTrenntGliedmassenVomOberkoerper() {
        val tipps = paket().tips.associateBy { it.id }
        val zwei = tipps.getValue("erste-hilfe-unterkuehlung-stadium-zwei")
        assertTrue(
            zwei.body.contains("keine Wärmflasche an den Gliedmaßen"),
            "das Verbot für Arme und Beine fehlt",
        )
        assertTrue(
            zwei.body.contains("Achseln, Brust und Rücken"),
            "die Stelle, an die die Wärme gehört, fehlt",
        )
        // Ohne den Grund bleibt es eine Willkuerregel, und Willkuerregeln
        // werden im Ernstfall nicht befolgt.
        assertTrue(
            zwei.body.contains("kaltes Blut zum Herzen"),
            "der Grund (Nachkühlung) fehlt",
        )
        // Der Titel darf das Verbot nicht mehr pauschal behaupten.
        assertFalse(
            zwei.title.contains("nicht mehr aufwärmen"),
            "der Titel behauptet das Gegenteil des eigenen Textes",
        )
        // Und das Essen und Trinken trennt die beiden Stadien.
        val eins = tipps.getValue("erste-hilfe-unterkuehlung-stadium-eins")
        assertTrue(
            eins.body.contains("ab dem zweiten Stadium gibt es nichts mehr zu essen"),
            "der Tipp zu Stadium I grenzt das Trinken nicht gegen Stadium II ab",
        )
    }

    // Das Paket sagt an zwei Stellen das Gegenteil ueber dasselbe: "Wunde nicht
    // auswaschen" (deutsche Laien-Erstversorgung, die den Arzt in Stunden
    // voraussetzt) und "mit sauberem Wasser reinigen" (internationale Leitlinie
    // fuer oberflaechliche Wunden). Beides ist belegt, und die Bedingung ist der
    // ganze Unterschied. Wer nur eine der beiden Stellen liest, muss sie
    // mitlesen -- sonst ist es ein Widerspruch statt einer Verzweigung.
    @Test
    fun dieBeidenWundRegelnNennenIhreBedingung() {
        val tipps = paket().tips.associateBy { it.id }
        val verbote = tipps.getValue("erste-hilfe-wunde-verbote")
        assertTrue(
            verbote.body.contains("kehrt sich die Regel um"),
            "der Verbots-Tipp sagt nicht, dass die Verbote an den erreichbaren Arzt gebunden sind",
        )
        assertTrue(
            verbote.body.contains("oberflächliche"),
            "die Umkehrung ist nicht auf oberflächliche Wunden begrenzt",
        )
        // Ohne die Infektionszeichen ist "versorge sie ueber Tage" wertlos: Der
        // Leser wuesste nicht, worauf er wartet.
        val bedecken = tipps.getValue("erste-hilfe-wunde-bedecken")
        for (zeichen in listOf("rot, violett oder dunkler", "Fieber")) {
            assertTrue(bedecken.body.contains(zeichen), "das Infektionszeichen \"$zeichen\" fehlt")
        }
        assertTrue(
            bedecken.body.contains("NICHT bedeckt"),
            "die Regel für die infizierte Wunde fehlt",
        )
    }

    // Beim Schlangenbiss stehen zwei Binden nebeneinander, die entgegengesetzt
    // wirken: der verbotene Druckverband und die empfohlene ruhigstellende
    // Binde. Ohne den Unterschied liest sich das Verbot als "nichts darum
    // herum" -- und die Ruhigstellung ist genau die Massnahme, die zaehlt,
    // wenn die Klinik weit ist.
    @Test
    fun derSchlangenbissTippTrenntDruckverbandVonRuhigstellung() {
        val tip = paket().tips.associateBy { it.id }.getValue("erste-hilfe-schlangenbiss")
        assertTrue(
            tip.body.contains("NICHT dehnbaren Binde"),
            "die ruhigstellende Binde fehlt oder ist nicht als nicht dehnbar bezeichnet",
        )
        assertTrue(
            tip.body.contains("nicht Druck, sondern Bewegungslosigkeit"),
            "der Unterschied zum Druckverband ist nicht ausgesprochen",
        )
    }

    // Die Abbindung ist die einzige Massnahme im Paket, die ein Koerperteil
    // kosten kann. Die internationale Leitlinie beantwortet die Frage nach dem
    // Loesen bewusst NICHT -- sie behaelt sie einer aerztlichen Anleitung vor.
    // Genau das gehoert in den Tipp, statt einer erfundenen Regel: Fuer die
    // Lage, fuer die diese App gebaut ist, ist "irgendwann abnehmen" der
    // gefaehrlichste Satz, den jemand hier lesen koennte.
    @Test
    fun derAbbindeTippErfindetKeineRegelZumLoesen() {
        val tip = paket().tips.associateBy { it.id }.getValue("erste-hilfe-abbinden")
        assertTrue(
            tip.body.contains("ärztlicher Anleitung"),
            "der Abbinde-Tipp sagt nicht mehr, wem die Entscheidung zum Lösen vorbehalten ist",
        )
        assertTrue(
            tip.body.contains("keine Laienanweisung"),
            "der Abbinde-Tipp benennt die Grenze der Quelle nicht — dann fuellt sie jemand selbst",
        )
        // Und keine Bastelanleitung. Die Leitlinie erlaubt die behelfsmaessige
        // Abbindung, aber die gemessenen Ausfallraten muessen danebenstehen,
        // sonst liest sich "darf angelegt werden" als "funktioniert".
        assertTrue(
            tip.body.contains("1 von 22"),
            "die gemessene Ausfallrate behelfsmäßiger Abbindungen fehlt",
        )
    }

    // Wer am Bein nicht zum Stehen bringt, was blutet, braucht die Abbindung --
    // und der Tipp zu "Kopf, Rumpf ODER Bein" ist der einzige, den er dann
    // vielleicht liest. Der Satz "an Kopf und Rumpf wird nicht abgebunden" ist
    // dort richtig und war beim Schreiben beinahe ein Fehler: ohne die Ausnahme
    // fuer das Bein liest er sich als generelles Verbot.
    @Test
    fun derKopfUndRumpfTippSchicktAmBeinZumAbbinden() {
        val tip = paket().tips.associateBy { it.id }
            .getValue("erste-hilfe-blutung-kopf-rumpf")
        assertTrue(tip.body.contains("Am Bein"), "die Ausnahme für das Bein fehlt")
        assertTrue(
            tip.body.contains("Abbinden (Tourniquet)"),
            "der Tipp verweist am Bein nicht auf die Abbindung",
        )
    }

    // Ein Warnmelder ist ein Messgeraet, kein Freibrief. Der Vorsorge-Tipp darf
    // sich nicht so lesen lassen, als duerfte mit Melder ein Grill oder ein
    // Stromerzeuger in den Raum -- genau diese Geraete sind der Anlassfall
    // dieser App, und die Quelle sagt ausdruecklich, dass ihr Betrieb in
    // geschlossenen Raeumen selbst bei geoeffneten Fenstern toedlich enden kann.
    //
    // Der Alarm-Tipp hat die Umkehrung: Ein Melder setzt sich selbsttaetig
    // zurueck, sobald geluftet wird. Er schweigt dann, obwohl die Ursache steht.
    // Ohne diesen Satz waere "der Alarm ist aus" der Anlass zurueckzugehen.
    @Test
    fun dieBeidenMelderTippsLassenKeineFalscheSicherheitZu() {
        val tipps = paket().tips.associateBy { it.id }
        val melder = tipps.getValue("erste-hilfe-kohlenmonoxid-melder")
        assertTrue(
            melder.body.contains("geschlossenen Räumen"),
            "der Melder-Tipp nennt die Grenze nicht, die kein Melder verschiebt",
        )
        assertTrue(
            melder.body.contains("kein Ersatz für einen Rauchwarnmelder"),
            "der Melder-Tipp grenzt sich nicht gegen den Rauchwarnmelder ab",
        )
        // Er ist der einzige Tipp im Paket, der Hoehenangaben neben
        // Kohlenmonoxid nennt -- Montagehoehen fuer das Geraet. Damit daraus
        // kein Rueckschluss auf einen sicheren Platz im Raum wird, muss der
        // Satz danebenstehen.
        assertTrue(
            melder.body.contains("Eine sichere Stelle im Raum gibt es nicht"),
            "der Melder-Tipp nennt Montagehöhen, ohne die Stelle-im-Raum-Regel danebenzustellen",
        )
        val alarm = tipps.getValue("erste-hilfe-kohlenmonoxid-alarm")
        assertTrue(
            alarm.body.contains("nicht zurück ins Haus"),
            "der Alarm-Tipp verbietet das Zurückgehen nicht mehr",
        )
        assertTrue(
            alarm.body.contains("nicht, dass die Ursache behoben ist"),
            "der Alarm-Tipp sagt nicht, dass ein verstummter Melder nichts beweist",
        )
    }

    // Jeder Tipp, der zum Notruf auffordert, muss die Nummer nennen. Sie ist das
    // Einzige, was in diesem Moment wirklich gebraucht wird, und niemand sucht
    // dafuer einen zweiten Tipp.
    @Test
    fun jederNotrufHinweisNenntDieNummer() {
        for (tip in paket().tips) {
            if (!tip.body.contains("Notruf")) continue
            assertTrue(
                tip.body.contains("112"),
                "${tip.id} fordert den Notruf, nennt aber die Nummer nicht",
            )
        }
    }

    // Die am 28.07.2026 eingearbeiteten zweiten Zweige tragen ihre Grenzen im
    // Text, und die Grenzen sind der sicherheitskritische Teil: Ohne sie wird
    // aus einer eng gefassten Ausnahme der Quelle eine allgemeine Anleitung.
    @Test
    fun dieNeuenZweitenZweigeNennenIhreGrenzen() {
        val tipps = paket().tips.associateBy { it.id }

        // Geradeziehen eines Bruchs: nur kalt und blass, nur ausgebildete Helfer.
        val bruch = tipps.getValue("erste-hilfe-knochenbruch-versorgen")
        assertTrue(bruch.body.contains("kalt und blass"), "die Durchblutungs-Bedingung fehlt")
        assertTrue(bruch.body.contains("ausgebildet"), "die Ausbildungs-Bedingung fehlt")

        // Brustwunde: Abdeckung nur nicht-luftdicht, und abgedichtet ist
        // schlimmer als offen.
        val brust = tipps.getValue("erste-hilfe-offene-brustwunde")
        assertTrue(brust.body.contains("nicht luftdichte"), "die Verbandsart fehlt")
        assertTrue(
            brust.body.contains("gefährlicher als eine offene"),
            "die Warnung vor der abgedichteten Wunde fehlt",
        )

        // Asthma ohne Spray: die Quelle nennt nur Beruhigung und Haltung -- der
        // Zweig darf nicht wie eine Behandlung klingen.
        val asthma = tipps.getValue("erste-hilfe-asthma")
        assertTrue(asthma.body.contains("ersetzt keine Behandlung"), "die Einordnung fehlt")

        // Anaphylaxie: Ersatzmittel nur nach Ruecksprache, und nie statt
        // vorhandenem Adrenalin.
        val schock = tipps.getValue("erste-hilfe-allergischer-schock")
        assertTrue(schock.body.contains("ärztlicher Rücksprache"), "die Rücksprache-Bedingung fehlt")
        assertTrue(
            schock.body.contains("ersetzen in keinem Fall das Adrenalin"),
            "der Vorrang des Adrenalins fehlt",
        )

        // Suizidgedanken: Verbunden bleiben ersetzt nicht den Versuch, Hilfe zu
        // erreichen.
        val suizid = tipps.getValue("erste-hilfe-suizidgedanken")
        assertTrue(
            suizid.body.contains("trotzdem versucht, Hilfe zu erreichen"),
            "der Vorrang der Hilfe bei konkreter Drohung fehlt",
        )

        // Alle sechs Zweige sind als Einordnung gekennzeichnet.
        for (
            kennung in listOf(
                "erste-hilfe-knochenbruch-versorgen", "erste-hilfe-offene-brustwunde",
                "erste-hilfe-asthma", "erste-hilfe-allergischer-schock",
                "erste-hilfe-suizidgedanken", "erste-hilfe-petermaennchen",
            )
        ) {
            assertTrue(
                tipps.getValue(kennung).body.contains("Einordnung für den Fall, dass niemand kommt"),
                "$kennung kennzeichnet den zweiten Zweig nicht",
            )
        }
    }

    // Die Leitlinie 2025 hat Herzstillstand, Seitenlage und Schocklage in EINEN
    // Ablauf zusammengefuehrt. Drei Aussagen daraus entscheiden im Ernstfall
    // ueber Minuten, und alle drei fehlten hier bis zum 17.08.2026:
    // auf der Seite kann niemand druecken, Schnappatmung gehoert nicht auf die
    // Seite, und nach einem Unfall bleibt der Mensch auf dem Ruecken.
    @Test
    fun dieSeitenlageNenntDenWegZurueckAufDenRuecken() {
        val tipps = paket().tips.associateBy { it.id }

        for (kennung in listOf("erste-hilfe-stabile-seitenlage", "erste-hilfe-seitenlage-handgriffe")) {
            assertTrue(
                tipps.getValue(kennung).body.contains("AUF DEN RÜCKEN GEDREHT"),
                "$kennung sagt nicht, dass vor der Herzdruckmassage zurueckgedreht wird",
            )
        }

        val handgriffe = tipps.getValue("erste-hilfe-seitenlage-handgriffe")
        // Beides ist seit 2025 erlaubt -- wer das nicht weiss, haelt mitten im
        // Ablauf an, weil der Arm nicht so liegt wie im Kurs.
        assertTrue(
            handgriffe.body.contains("GESTRECKT oder ANGEWINKELT"),
            "die Freigabe fuer den gestreckten Arm fehlt",
        )
        // Der Handgriff fuer den Fall, dass der Helfer viel kleiner ist.
        assertTrue(
            handgriffe.body.contains("ZUERST DAS KNIE AUFSTELLEN"),
            "der Griff bei grossem Groessenunterschied fehlt",
        )

        val lage = tipps.getValue("erste-hilfe-stabile-seitenlage")
        assertTrue(lage.body.contains("SCHNAPPATMUNG"), "die Schnappatmung-Ausnahme fehlt")
        assertTrue(
            lage.body.contains("BLEIBT SIE AUF DEM RÜCKEN LIEGEN"),
            "die Trauma-Ausnahme nennt die Rueckenlage nicht",
        )
    }

    // Der einzige Eintrag des Pakets, der auf einer Messreihe an VIER Menschen
    // steht statt auf einer Leitlinie -- weil die Leitlinie selbst festhaelt,
    // dass es zum Alleinsein beim Ersticken keine Untersuchungen gibt. Genau
    // deshalb muss er seine Grenzen selbst nennen, und der Husten muss vorn
    // stehen bleiben: er war in derselben Messreihe das Kraeftigste.
    @Test
    fun derAlleinErstickenTippNenntSeineGrenzen() {
        val allein = paket().tips.associateBy { it.id }
            .getValue("erste-hilfe-ersticken-allein")

        assertTrue(
            allein.body.contains("KEINE UNTERSUCHUNGEN"),
            "der Eintrag verschweigt, dass die Leitlinie hier nichts hergibt",
        )
        assertTrue(
            allein.body.contains("Gemessen wurde DRUCK, nicht Überleben"),
            "die Grenze der Messung fehlt",
        )
        assertTrue(
            allein.body.contains("Solange noch Husten geht, wird gehustet"),
            "der Vorrang des Hustens fehlt",
        )
        // Die Kante gehoert unter die Rippen, nicht darauf.
        assertTrue(
            allein.body.contains("UNTERHALB DER RIPPEN"),
            "die Stelle fuer die Lehne ist nicht eingegrenzt",
        )
        // Nicht erst Hilfe suchen -- dafuer reicht die Zeit nicht.
        assertTrue(
            allein.body.contains("NICHT ERST NACH HILFE SUCHST"),
            "der Zeitdruck fehlt",
        )
    }

    // Vier Einzelheiten aus dem Volltext der Erste-Hilfe-Leitlinie 2025, die in
    // den Kurzfassungen nicht vorkommen. Jede davon ist der Unterschied
    // zwischen "man weiss ungefaehr, was gemeint ist" und "man kann es tun".
    @Test
    fun dieNachtraegeAusDemVolltext2025StehenDrin() {
        val tipps = paket().tips.associateBy { it.id }

        // Ohne Messgeraet gibt es keine Zahl -- also braucht es eine.
        val zucker = tipps.getValue("erste-hilfe-unterzuckerung")
        assertTrue(zucker.body.contains("70 MG/DL"), "der Grenzwert fehlt")
        assertTrue(
            zucker.body.contains("AUSSENSEITE DES OBERSCHENKELS"),
            "wohin das Glukagon kommt, steht nicht da",
        )

        // Im Anfall bringt kaum jemand Spruehstoss und Einatmen zusammen.
        assertTrue(
            tipps.getValue("erste-hilfe-asthma").body.contains("VORSCHALTKAMMER"),
            "die Vorschaltkammer fehlt",
        )

        // Die Reihenfolge, die man im Schreck umdreht -- und die Regel fuer
        // alle, die kein Thermometer fuer den Koerperkern haben.
        val hitze = tipps.getValue("erste-hilfe-hitzschlag-handeln")
        assertTrue(
            hitze.body.contains("KÜHLEN KOMMT VOR TRANSPORTIEREN"),
            "der Merksatz zur Reihenfolge fehlt",
        )
        assertTrue(
            hitze.body.contains("15 MINUTEN LANG GEKÜHLT"),
            "die Regel ohne Thermometer fehlt",
        )

        // Wer stundenlang halten muss, haelt mit den Ellbogen, nicht mit den
        // Armen -- und laesst die Ohren frei.
        val hals = tipps.getValue("erste-hilfe-wirbelsaeule")
        assertTrue(hals.body.contains("ELLBOGEN AUF DEN BODEN"), "der Halt fuer die Ellbogen fehlt")
        assertTrue(hals.body.contains("OHREN BLEIBEN FREI"), "die freien Ohren fehlen")
    }

    // Zwei Zoegern-Momente vor einem AED, die beide toedlich sein koennen und
    // beide durch einen Satz aufzuloesen sind: "Darf ich das bei einem Kind?"
    // und "Muss ich ihr wirklich den Brustkorb freimachen?"
    @Test
    fun derAedTippNimmtBeideZoegernMomente() {
        val aed = paket().tips.associateBy { it.id }.getValue("erste-hilfe-aed-anwenden")

        // Ohne Kinder-Einstellung wird das Geraet trotzdem benutzt.
        assertTrue(
            aed.body.contains("IM ERWACHSENEN-BETRIEB BENUTZT"),
            "der Fall 'Geraet kennt keine Kinder' fehlt",
        )
        assertTrue(aed.body.contains("UNTER 25 KILOGRAMM"), "die Gewichtsgrenze fehlt")
        // Beim kleinen Kind liegen die Pads anders.
        assertTrue(
            aed.body.contains("ZWISCHEN DIE SCHULTERBLÄTTER"),
            "die Pad-Lage beim kleinen Kind fehlt",
        )
        // Der belegte Grund, warum Frauen seltener geholfen wird.
        assertTrue(
            aed.body.contains("Lebensrettung geht vor Schamgefühl"),
            "die Scheu vor dem Freimachen ist nicht angesprochen",
        )
    }

    // Zwei Quellen, zwei Normbereiche fuer Kinder -- und ein Zweijaehriger mit
    // Puls 140 liegt je nach Fassung im Normbereich oder darueber. Der Eintrag
    // muss den Unterschied offenlegen UND sagen, was daraus folgt; ein
    // stillschweigend gewaehlter Satz Zahlen waere hier das Gefaehrliche.
    @Test
    fun dieKinderNormwerteLegenIhrenUnterschiedOffen() {
        val u = paket().tips.associateBy { it.id }
            .getValue("erste-hilfe-koerperliche-untersuchung")

        assertTrue(u.body.contains("110 bis 180"), "die europaeischen Herzwerte fehlen")
        assertTrue(u.body.contains("25 bis 60"), "die europaeischen Atemwerte fehlen")
        assertTrue(
            u.body.contains("WEITEREN liegt, ist ein Alarmzeichen"),
            "die Regel zum Umgang mit den zwei Fassungen fehlt",
        )
        // Gute Werte sind keine Entwarnung.
        assertTrue(
            u.body.contains("PLÖTZLICH UND OHNE VORZEICHEN"),
            "die Warnung aus der Kinder-Leitlinie fehlt",
        )
    }

    // Aspirin liegt in fast jeder Hausapotheke, es ist das Mittel, das die
    // Erwachsenen kennen -- und beim fiebernden Kind ist es das falsche.
    // Diese Warnung fehlte im ganzen Paket.
    @Test
    fun derFieberTippVerbietetAspirinFuerKinder() {
        val fieber = paket().tips.associateBy { it.id }
            .getValue("erste-hilfe-fieber-versorgen")

        assertTrue(fieber.body.contains("KEIN ASPIRIN FÜR KINDER"), "die Warnung fehlt")
        assertTrue(fieber.body.contains("REYE-SYNDROM"), "der Grund fehlt")
        // Und die Mengen, die an ihre Stelle treten.
        assertTrue(
            fieber.body.contains("15 Milligramm je Kilogramm"),
            "die Paracetamol-Menge fehlt",
        )
        assertTrue(
            fieber.body.contains("Alle Angaben ohne Gewähr"),
            "die Mengen stehen ohne den Vorbehalt da",
        )
    }

    // Bei der Guertelrose entscheidet ein einziges Zeichen darueber, ob jemand
    // abwartet oder losgeht: Blaeschen am Lid oder auf der Nasenspitze. Dahinter
    // steht das Auge.
    @Test
    fun derGuertelroseTippNenntDasAugeUndDenZeitpunkt() {
        val g = paket().tips.associateBy { it.id }.getValue("medizin-guertelrose")

        // Das Erkennungszeichen.
        assertTrue(g.body.contains("GEHT NICHT ÜBER DIE MITTE"), "die Mittellinie fehlt")
        // Der Notfall.
        assertTrue(g.body.contains("NASENSPITZE"), "das Zeichen am Gesicht fehlt")
        assertTrue(g.body.contains("ERBLINDUNG"), "die Folge fehlt")
        // Und dass das Mittel frueh gegeben werden muss, nicht spaet.
        assertTrue(
            g.body.contains("SCHON IN DER SCHMERZPHASE VOR DEM AUSSCHLAG"),
            "der Zeitpunkt der Behandlung fehlt",
        )
    }

    // Windpocken sind ansteckend, BEVOR man etwas sieht. Daraus folgt der ganze
    // Umgang damit: Nicht das kranke Kind wird weggesperrt -- dafuer ist es zu
    // spaet --, sondern die gefaehrdete Person kommt aus dem Raum.
    @Test
    fun derWindpockenTippZiehtDieRichtigeFolgerung() {
        val tipps = paket().tips.associateBy { it.id }
        val w = tipps.getValue("medizin-windpocken")

        assertTrue(
            w.body.contains("BEVOR DER AUSSCHLAG KOMMT"),
            "dass die Ansteckung vor dem Ausschlag beginnt, fehlt",
        )
        assertTrue(
            w.body.contains("TRENNT ZU SPÄT"),
            "die Folgerung aus der fruehen Ansteckung fehlt",
        )
        assertTrue(
            w.body.contains("DIE GEFÄHRDETE PERSON IN SICHERHEIT GEBRACHT"),
            "es steht nicht da, WER aus dem Raum muss",
        )
        // Das Erkennungszeichen.
        assertTrue(w.body.contains("STERNENHIMMEL"), "das Erkennungszeichen fehlt")
        // Und die Rueckverbindung zur Aspirin-Warnung.
        assertTrue(w.body.contains("KEIN ASPIRIN"), "der Verweis auf die Aspirin-Regel fehlt")

        // Die Guertelrose sagt jetzt, was sie ueberhaupt weitergibt -- naemlich
        // Windpocken, nicht Guertelrose.
        val g = tipps.getValue("medizin-guertelrose")
        assertTrue(
            g.body.contains("bekommt WINDPOCKEN"),
            "die Guertelrose sagt nicht, was sie weitergibt",
        )
        assertTrue(
            g.body.contains("VOLLSTÄNDIGE ABDECKEN DER STELLEN"),
            "die wirksamste Massnahme gegen die Weitergabe fehlt",
        )
    }

    // Beim Kreuzschmerz zeigen Rat und Reflex in entgegengesetzte Richtungen:
    // Der Schmerz sagt "hinlegen", die Quelle sagt "bloss nicht". Und daneben
    // steht der eine Fall, der eine Frist hat.
    @Test
    fun derHexenschussTippStelltSichGegenDenReflex() {
        val h = paket().tips.associateBy { it.id }.getValue("erste-hilfe-hexenschuss")

        assertTrue(h.body.contains("BETTRUHE IST NICHT ANGEZEIGT"), "der Kern des Rats fehlt")
        // Der Notfall darunter -- und er ist an Blase und Darm zu erkennen,
        // nicht am Schmerz.
        assertTrue(
            h.body.contains("KONTROLLE ÜBER BLASE ODER DARM"),
            "das Zeichen der Einklemmung fehlt",
        )
        assertTrue(h.body.contains("12 BIS 24 STUNDEN"), "die Frist fehlt")
        // Das zweite Zeichen, das niemand von selbst prueft.
        assertTrue(h.body.contains("SATTEL AUFSITZT"), "die Sattel-Taubheit fehlt")
        // Mengenangabe nur mit Vorbehalt.
        assertTrue(
            h.body.contains("Alle Angaben ohne Gewähr"),
            "die Ibuprofen-Menge steht ohne den Vorbehalt da",
        )
    }

    // Die Liste essbarer Pflanzen stammt aus einer nordamerikanischen Quelle
    // und fuehrt die Buchecker ohne jede Bedingung. Roh macht sie krank. Eine
    // Sammelliste, die das verschweigt, schickt Leute richtig sammeln und
    // falsch essen -- deshalb muessen BEIDE Eintraege es sagen.
    @Test
    fun dieBucheckerStehtNirgendsOhneIhreBedingung() {
        val tipps = paket().tips.associateBy { it.id }

        val b = tipps.getValue("nahrung-bucheckern")
        assertTrue(b.body.contains("ROH SIND SIE LEICHT GIFTIG"), "die Bedingung fehlt")
        assertTrue(
            b.body.contains("RÖSTEN, BRATEN ODER ÜBERBRÜHEN"),
            "was die Stoffe unschaedlich macht, steht nicht da",
        )
        // Der Sortiertrick, den man beim Ueberbruehen geschenkt bekommt.
        assertTrue(
            b.body.contains("WAS OBEN SCHWIMMT, WIRD AUSSORTIERT"),
            "der Schwimmtest fehlt",
        )

        // Und die Sammelliste darf die Buchecker nicht mehr nackt fuehren.
        assertTrue(
            tipps.getValue("nahrung-essbares-gruen-namentlich").body
                .contains("BUCHECKERN SIND ROH LEICHT GIFTIG"),
            "die Sammelliste nennt die Buchecker weiter ohne Bedingung",
        )
    }

    // Giersch ist essbar und steht massenhaft -- aber er gehoert in die Familie
    // mit den toedlichsten Verwechslungen. Ein Eintrag, der das Sammeln
    // empfiehlt, ohne das Bestimmen davorzustellen, waere hier gefaehrlich.
    @Test
    fun derGierschTippStelltDasBestimmenVorDasSammeln() {
        val g = paket().tips.associateBy { it.id }.getValue("nahrung-giersch")

        // Das Merkmal, das ihn von allen anderen Doldenbluetlern trennt.
        assertTrue(g.body.contains("DER STÄNGEL IST DREIKANTIG"), "die Drei-Regel fehlt")
        // Und der Griff, der im Zweifel entscheidet.
        assertTrue(
            g.body.contains("GIERSCH HAT EINEN KANTIGEN STÄNGEL"),
            "das Unterscheidungsmerkmal zur Hundspetersilie fehlt",
        )
        // Wer hier toetet.
        assertTrue(
            g.body.contains("GEFLECKTE SCHIERLING IST DER GIFTIGSTE"),
            "die gefaehrlichste Verwechslung ist nicht benannt",
        )
        // Der wichtigste Satz: nicht probieren, sondern bestimmen.
        assertTrue(
            g.body.contains("BEI DOLDENBLÜTLERN GILT DER ESSBARKEITSTEST NICHT"),
            "der Eintrag laesst den Essbarkeitstest als Ausweg offen",
        )
        assertTrue(
            g.body.contains("NUR PFLÜCKEN, WAS MAN SICHER BESTIMMEN KANN"),
            "die Grundregel fehlt",
        )
    }

    // Die Vogelmiere ist im Winter oft das einzige frische Gruen -- und sie
    // wird nur ueber ein einziges Merkmal sicher: die eine Haarreihe am
    // Staengel. Ohne das Merkmal ist der Eintrag eine Einladung zum Raten.
    @Test
    fun derVogelmiereTippNenntSeinErkennungsmerkmal() {
        val v = paket().tips.associateBy { it.id }.getValue("nahrung-vogelmiere")

        assertTrue(v.body.contains("EINREIHIG BEHAART"), "das Erkennungsmerkmal fehlt")
        // Die Bluete, die man falsch zaehlt.
        assertTrue(v.body.contains("SCHEINBAR ZEHN"), "der Blueten-Trick fehlt")
        // Und die Abgrenzung, ebenfalls am Staengel und ebenfalls fuer die Finger.
        assertTrue(
            v.body.contains("VIERKANTIGEN STÄNGEL"),
            "die Abgrenzung zum Acker-Gauchheil fehlt",
        )
    }

    // Eicheln sind roh ungeniessbar, und die Sammelliste fuehrte auch sie ohne
    // Bedingung. Anders als bei einer Giftpflanze wird hier nichts entgiftet,
    // sondern etwas herausgewaschen -- und die Probe dafuer braucht kein
    // Geraet, sondern nur ein Auge.
    @Test
    fun derEichelTippNenntDieProbeOhneGeraet() {
        val tipps = paket().tips.associateBy { it.id }
        val e = tipps.getValue("nahrung-eicheln")

        assertTrue(e.body.contains("ROH UND DIREKT VOM BAUM SIND EICHELN UNGENIESSBAR"), "die Bedingung fehlt")
        // Der Endpunkt, an dem man aufhoert zu waessern.
        assertTrue(
            e.body.contains("BIS ES FARBLOS BLEIBT"),
            "der Endpunkt des Waesserns fehlt",
        )
        assertTrue(
            e.body.contains("SOLANGE SICH DAS WASSER NOCH FÄRBT"),
            "die Probe ist nicht als Probe benannt",
        )

        // Und die Sammelliste fuehrt auch die Eichel nicht mehr nackt.
        assertTrue(
            tipps.getValue("nahrung-essbares-gruen-namentlich").body
                .contains("EICHELN SIND ROH UNGENIESSBAR"),
            "die Sammelliste nennt die Eichel weiter ohne Bedingung",
        )
    }

    // Ein Gerstenkorn ist harmlos -- solange niemand daran herumdrueckt. Genau
    // dazu verleitet ein sichtbarer Eiterpunkt, und genau dann, wenn kein Arzt
    // erreichbar ist, ist die Versuchung am groessten. Beides muss dastehen:
    // das Verbot und die Grenze, ab der es doch ernst wird.
    @Test
    fun derGerstenkornTippVerbietetDasDrueckenUndNenntDieGrenze() {
        val g = paket().tips.associateBy { it.id }.getValue("medizin-gerstenkorn")

        assertTrue(g.body.contains("NICHT AN DER ENTZÜNDUNG HERUMDRÜCKEN"), "das Verbot fehlt")
        assertTrue(g.body.contains("Augenhöhle"), "wohin es sich ausbreiten kann, fehlt")
        // Die Unterscheidung, die man mit einem Finger trifft.
        assertTrue(
            g.body.contains("AUF DRUCK NICHT WEH"),
            "die Druckprobe zur Unterscheidung fehlt",
        )
        // Und die Grenze, ab der ein weiter Weg sich lohnt.
        assertTrue(
            g.body.contains("BEWEGUNG DES AUGES WEHTUT"),
            "die Zeichen hinter dem Lid fehlen",
        )
        // Die Quelle sagt selbst, dass ihre Massnahmen unbelegt sind -- das
        // darf der Eintrag nicht verschweigen.
        assertTrue(
            g.body.contains("KEINE STUDIEN GIBT"),
            "die Ehrlichkeit der Quelle ueber die Beweislage fehlt",
        )
    }

    // Nach der Katastrophe ist die Gefahr still. Zwei Saetze entscheiden hier:
    // kein offenes Licht in einem beschaedigten Gebaeude, und den Keller
    // langsam auspumpen -- das Zweite kennt fast niemand und es rettet das Haus.
    @Test
    fun derRueckkehrTippNenntDieZweiUnbekanntenRegeln() {
        val g = paket().tips.associateBy { it.id }
            .getValue("taktisch-zurueck-ins-gebaeude")

        // Gas plus offene Flamme.
        assertTrue(
            g.body.contains("NICHT MIT OFFENER FLAMME GELEUCHTET"),
            "das Verbot offener Flammen fehlt",
        )
        // Die Zahl, die den Keller rettet.
        assertTrue(
            g.body.contains("EIN DRITTEL DES WASSERS PRO TAG"),
            "die Auspumpregel fehlt",
        )
        assertTrue(
            g.body.contains("WÄNDE KÖNNEN EINSTÜRZEN"),
            "der Grund fuer die Auspumpregel fehlt",
        )
        // Und der Satz, der keine Auslegung braucht.
        assertTrue(
            g.body.contains("WIRD ES SOFORT VERLASSEN"),
            "die Einsturzregel fehlt",
        )
    }

    // Die Knoblauchsrauke riecht NICHT von selbst -- man geht daran vorbei.
    // Und der Geruch, den sie beim Zerreiben hergibt, ist eine Bestaetigung
    // und keine Essbarkeitspruefung. Beides muss dastehen, sonst wird aus dem
    // Test eine Erlaubnis, an jeder Pflanze zu riechen und sie dann zu essen.
    @Test
    fun derKnoblauchsraukeTippBegrenztDenGeruchstest() {
        val k = paket().tips.associateBy { it.id }
            .getValue("nahrung-knoblauchsrauke")

        assertTrue(k.body.contains("SIE RIECHT NICHT VON SELBST"), "der Haken am Merkmal fehlt")
        assertTrue(
            k.body.contains("Er ersetzt das Hinsehen nicht"),
            "die Grenze des Geruchstests fehlt",
        )
        // Zweijaehrig -- sonst haelt man sie fuer zwei Pflanzen.
        assertTrue(k.body.contains("ZWEIJÄHRIG"), "die zwei Erscheinungsformen fehlen")
    }

    // Ein Merkblatt der Feuerwehr sagt bei Erstickungs-Stillstand "zunaechst
    // zweimal beatmen", die aktuelle Leitlinie sagt fuer ALLE Erwachsenen
    // "ohne Training nur druecken". Der Widerspruch stand seit Wochen als
    // offene Frage in der ROADMAP. Der Eintrag muss beide Saetze nennen UND
    // den Ausweg, der ohne Entscheidung auskommt -- sonst zoegert jemand,
    // der beide Fassungen kennt, im schlechtesten Moment.
    @Test
    fun derNurDrueckenTippLegtDenRauchgasStreitOffen() {
        val t = paket().tips.associateBy { it.id }.getValue("erste-hilfe-nur-druecken")

        assertTrue(
            t.body.contains("SIE MACHT DIESE AUSNAHME NICHT"),
            "die Aussage der aktuellen Leitlinie zum Rauchgas fehlt",
        )
        assertTrue(
            t.body.contains("ZUNÄCHST ZWEIMAL ZU BEATMEN"),
            "die abweichende aeltere Empfehlung wird verschwiegen",
        )
        // Der Ausweg, der die Frage gar nicht entscheiden muss.
        assertTrue(
            t.body.contains("WER BEATMEN KANN UND WILL, BEATMET"),
            "die aufloesende Regel fehlt",
        )
        // Und die Grenze des eigenen Wissens.
        assertTrue(
            t.body.contains("das für dieses Paket nicht vorlag"),
            "der Vorbehalt zum Sonderlagen-Kapitel fehlt",
        )
    }

    // Der Grabenfuss-Eintrag endete an "aerztliche Behandlung erreichen" -- und
    // genau dort steht der Leser dieses Pakets meistens allein. Der zweite
    // Zweig muss zwei Dinge sagen: dass NICHTS geschnitten wird, und woran man
    // merkt, dass es keine Fusssache mehr ist.
    @Test
    fun derGrabenfussTippBeantwortetDasEndeOhneArzt() {
        val g = paket().tips.associateBy { it.id }.getValue("erste-hilfe-grabenfuss")

        assertTrue(
            g.body.contains("EINORDNUNG FÜR DEN FALL, DASS NIEMAND KOMMT"),
            "der zweite Zweig fehlt oder ist nicht gekennzeichnet",
        )
        assertTrue(
            g.body.contains("ES WIRD NICHTS WEGGESCHNITTEN"),
            "das Schneideverbot fehlt",
        )
        assertTrue(
            g.body.contains("NICHT AM FUSS, SONDERN AM FIEBER"),
            "die Grenze zum lebensbedrohlichen Verlauf fehlt",
        )
    }

    // Der Eintrag sagte "Trinken allein reicht dann nicht mehr" und hoerte auf.
    // Das laesst jemanden ohne Klinik aufgeben -- obwohl DIESELBE Quelle fuer
    // genau den Fall ohne Infusion einen Weg hat. Der muss dastehen, samt der
    // Grenze, an der er endet.
    @Test
    fun derAustrocknungsTippLaesstNiemandenAufgeben() {
        val a = paket().tips.associateBy { it.id }
            .getValue("erste-hilfe-austrocknung-erkennen")

        assertTrue(
            a.body.contains("WIRD TROTZDEM GETRUNKEN"),
            "der Weg ohne Infusion fehlt",
        )
        assertTrue(
            a.body.contains("20 MILLILITER JE KILOGRAMM"),
            "die Menge fehlt",
        )
        assertTrue(
            a.body.contains("Alle Angaben ohne Gewähr"),
            "die Menge steht ohne den Vorbehalt da",
        )
        // Und die Grenze, die auch ohne Klinik gilt.
        assertTrue(
            a.body.contains("BEKOMMT NICHTS IN DEN MUND"),
            "die Grenze bei getruebtem Bewusstsein fehlt",
        )
    }

    // Einwand vom 28.07.2026: Die Wiederbelebungs-Regeln setzen still voraus,
    // dass jemand kommt und uebernimmt. Die Leitlinie denkt den Fall ohne
    // Telefon tatsaechlich mit -- und gibt zu, dass sie keine Zahl dafuer hat.
    // Beides muss dastehen, samt der Folgerung fuer den Fall, dass es
    // ueberhaupt keine Hilfe zu holen gibt.
    @Test
    fun derNotrufTippDenktDenEinzelnenHelferOhneTelefonMit() {
        val n = paket().tips.associateBy { it.id }.getValue("erste-hilfe-notruf-112")

        assertTrue(
            n.body.contains("KEINE UNTERSUCHUNG DAZU"),
            "die offene Beweislage der Leitlinie fehlt",
        )
        assertTrue(
            n.body.contains("WER NIEMANDEN HOLEN KANN, GEHT NICHT LOS"),
            "die Folgerung fuer den Fall ohne erreichbare Hilfe fehlt",
        )
        // Und der bessere Weg, solange jemand in der Naehe sein koennte.
        assertTrue(
            n.body.contains("laut rufen UND sofort"),
            "das Rufen mit sofortigem Beginn fehlt",
        )
    }

    // Die drei Unterkuehlungs-Stadien haengen am Zittern und daran, wann es
    // AUFHOERT. Ein Saeugling zittert nie -- wer auf dieses Zeichen wartet,
    // wartet auf etwas, das nicht kommt. Genau das muss dastehen, samt dem
    // Zeichen, das an seine Stelle tritt.
    @Test
    fun derKinderUnterkuehlungsTippNimmtDasZitternAlsMassstabWeg() {
        val tipps = paket().tips.associateBy { it.id }
        val k = tipps.getValue("erste-hilfe-unterkuehlung-kind")

        assertTrue(
            k.body.contains("SÄUGLINGE KÖNNEN NOCH NICHT DURCH ZITTERN WÄRME ERZEUGEN"),
            "der Kernsatz fehlt",
        )
        assertTrue(
            k.body.contains("wartet auf etwas, das nie kommt"),
            "die Folgerung aus dem fehlenden Zittern fehlt",
        )
        // Das Zeichen, das an seine Stelle tritt.
        assertTrue(
            k.body.contains("BLÄSSE ODER BLAUVERFÄRBUNG DER HAUT"),
            "das Ersatzzeichen fehlt",
        )
        // Und die Ursachen, an die niemand denkt.
        assertTrue(k.body.contains("TRAGEGESTELLEN"), "die Erfrierungsursache Tragegestell fehlt")

        // Der Erwachsenen-Eintrag darf weiter am Zittern haengen -- er ist fuer
        // Erwachsene richtig. Aber es muss den Kinder-Eintrag daneben geben.
        assertTrue(
            tipps.containsKey("erste-hilfe-unterkuehlung-stadium-eins"),
            "der Erwachsenen-Eintrag ist weg",
        )
    }

    // "Bei Kindern kann der Bedarf hoeher liegen" ist zu ungenau, wenn Wasser
    // eingeteilt werden muss. Der Eintrag braucht die Zahlen -- und vor allem
    // den Satz, der die Einteilung umdreht: pro Kilogramm braucht ein
    // Saeugling fast das Vierfache eines Erwachsenen.
    @Test
    fun derWasserbedarfsTippSagtWoNichtGekuerztWird() {
        val w = paket().tips.associateBy { it.id }.getValue("wasser-tagesbedarf")

        assertTrue(
            w.body.contains("130 Milliliter je Kilogramm"),
            "der Wert fuer Saeuglinge fehlt",
        )
        assertTrue(
            w.body.contains("KÜRZT DESHALB NICHT BEIM KLEINSTEN ZUERST"),
            "die Folgerung fuer die Einteilung fehlt",
        )
        // Der am haeufigsten uebersehene Fall.
        assertTrue(w.body.contains("STILLENDE"), "der hoechste Wert der Tabelle fehlt")
        // Und der Umstand, den man im Winter vergisst.
        assertTrue(
            w.body.contains("TROCKENE KALTE LUFT"),
            "der erhoehte Bedarf bei trockener Kaelte fehlt",
        )
    }

    // Kleine Kinder ueberhitzen aus drei Gruenden schneller, und das erste
    // Zeichen ist gegen die Erwartung: rotes Gesicht bei KUEHLER Haut. Dazu
    // die Zahl, die eine Vorratsplanung umwirft -- bei 30 Grad das Doppelte
    // bis Dreifache des gewoehnlichen Bedarfs.
    @Test
    fun derHitzeTippFuerKleineNenntZahlUndDasFalscheZeichen() {
        val h = paket().tips.associateBy { it.id }.getValue("erste-hilfe-hitze-kleinste")

        assertTrue(
            h.body.contains("1,5 BIS 3 LITER"),
            "die Trinkmenge bei Hitze fehlt",
        )
        assertTrue(
            h.body.contains("UND KÜHLE HAUT"),
            "das gegen die Erwartung laufende Zeichen fehlt",
        )
        // Die Probe ohne Geraet.
        assertTrue(
            h.body.contains("ZWISCHEN DEN SCHULTERBLÄTTERN"),
            "die Handprobe fehlt",
        )
        // Und die Folgerung fuer die Vorratsplanung.
        assertTrue(
            h.body.contains("VERDOPPELT BIS VERDREIFACHT"),
            "der Vergleich zum gewoehnlichen Bedarf fehlt",
        )
    }

    // Ein eingewachsener Nagel klingt nach Kleinigkeit. Wer nicht mehr gehen
    // kann, ist in einer Krise ein anderer Mensch -- und die Quelle nennt die
    // Knochenentzuendung als Folge der Vernachlaessigung. Drei Dinge muessen
    // dastehen: die Folge, der Handgriff, und der Splitter, der es
    // zurueckkommen laesst.
    @Test
    fun derNagelTippNenntFolgeHandgriffUndDenRueckfallgrund() {
        val n = paket().tips.associateBy { it.id }
            .getValue("medizin-eingewachsener-nagel")

        assertTrue(n.body.contains("KNOCHENENTZÜNDUNG"), "die Folge der Vernachlaessigung fehlt")
        assertTrue(
            n.body.contains("NAGELECKE ANHEBEN UND MIT EINER KLEINEN SCHERE ENTFERNEN"),
            "die einfache Fassung des Eingriffs fehlt",
        )
        assertTrue(
            n.body.contains("NADELFÖRMIGEN NAGELSPLITTERN"),
            "der Grund fuer den Rueckfall fehlt",
        )
        // Und die Warnung, die nur an Fingern und Zehen gilt.
        assertTrue(
            n.body.contains("NIEMALS EINES MIT ADRENALIN"),
            "die Adrenalin-Warnung fehlt",
        )
    }

    // Bei Madenwuermern scheitert die Behandlung fast immer an denselben zwei
    // Saetzen: nach zwei Wochen wiederholen, und ALLE im Haushalt behandeln.
    // Und wenn kein Mittel da ist, ist der Eintrag nur dann etwas wert, wenn
    // er den Kreislauf beschreibt, den man stattdessen unterbricht.
    @Test
    fun derMadenwurmTippNenntBeideFehlerUndDenWegOhneMittel() {
        val m = paket().tips.associateBy { it.id }.getValue("medizin-madenwurm")

        assertTrue(
            m.body.contains("NACH ZWEI WOCHEN WIEDERHOLT"),
            "die zweite Gabe fehlt",
        )
        assertTrue(
            m.body.contains("ALLE IM HAUSHALT WERDEN BEHANDELT"),
            "die Behandlung aller fehlt",
        )
        // Der Weg ohne Mittel haengt an den Fingernaegeln und am Morgenwaschen.
        assertTrue(m.body.contains("KURZ SCHNEIDEN"), "der wirksamste Handgriff fehlt")
        assertTrue(
            m.body.contains("BETTZEUG NICHT AUSSCHÜTTELN"),
            "die Warnung vor dem Ausschuetteln fehlt",
        )
        // Und die Einordnung, die die Angst herausnimmt.
        assertTrue(
            m.body.contains("UNANGENEHM, ABER NICHT GEFÄHRLICH"),
            "die Einordnung der Gefahr fehlt",
        )
    }
}
