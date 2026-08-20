package org.compasszero.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// Im Ernstfall tippt jemand ein Stichwort und braucht die passende Anleitung --
// nicht irgendeine. Dieser Test faehrt die Suche gegen das ECHTE Inhaltspaket
// und haelt fest, dass die naheliegenden Notfall-Woerter zum richtigen Tipp
// fuehren.
//
// Gemessen wurde am 28.07.2026: Die Suche vergleicht Wortanfaenge, deshalb
// findet "erstickt" das Wort "ersticken" NICHT. Deutsch beugt zu stark dafuer.
// Die gebraeuchlichen Formen stehen deshalb in den Schlagwoertern der Tipps --
// dieser Test sichert genau das ab.
class NotfallSucheTest {

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
    private fun index(): SearchIndex {
        val paket = File(repoRoot(), "content/europe-de/paket")
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to File(paket, "manifest.json").readBytes(),
                "content/tips.json" to File(paket, "content/tips.json").readBytes(),
                "content/guides.json" to File(paket, "content/guides.json").readBytes(),
                "content/agriculture.json" to File(paket, "content/agriculture.json").readBytes(),
                "content/terms.json" to File(paket, "content/terms.json").readBytes(),
            ),
            emptySet(),
        )
        return SearchIndex.build(result.pack ?: error("Paket laedt nicht: ${result.problems}"))
    }

    private fun findet(index: SearchIndex, anfrage: String, erwartet: String) {
        val treffer = index.search(anfrage, limit = 8).map { it.id }
        assertTrue(
            erwartet in treffer,
            "\"$anfrage\" findet $erwartet nicht. Gefunden: ${if (treffer.isEmpty()) "nichts" else treffer.joinToString()}",
        )
    }

    @Test
    fun umgangssprachlicheNotfallwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        // Gebeugte und umgangssprachliche Formen -- genau das, was jemand unter
        // Stress tippt, und genau das, was die Wortanfangs-Suche nicht von selbst
        // findet.
        findet(index, "erstickt", "erste-hilfe-ersticken-erkennen")
        findet(index, "verschluckt", "erste-hilfe-ersticken-rueckenschlaege")
        findet(index, "baby", "erste-hilfe-ersticken-saeugling")
        findet(index, "kleinkind", "erste-hilfe-ersticken-saeugling")
        findet(index, "blutet", "erste-hilfe-starke-blutung")
        findet(index, "gebrochen", "erste-hilfe-knochenbruch-erkennen")
        findet(index, "erfroren", "erste-hilfe-erfrierungen-versorgen")
        findet(index, "kalt", "erste-hilfe-unterkuehlung-stadium-eins")
        findet(index, "frieren", "erste-hilfe-waermeerhalt")
        findet(index, "reanimation", "erste-hilfe-herzdruckmassage")
        findet(index, "wiederbeleben", "erste-hilfe-herzdruckmassage")
        findet(index, "ohnmacht", "erste-hilfe-stabile-seitenlage")
        findet(index, "verletzt", "erste-hilfe-wunde-bedecken")
        // Gemessen am 28.07.2026: Ohne diese Schlagwoerter fand "tabletten" nur
        // die Chlortabletten zur Wasserentkeimung, und "pilze" gar nichts --
        // obwohl das die beiden haeufigsten Vergiftungsanlaesse ueberhaupt sind.
        findet(index, "tabletten", "erste-hilfe-vergiftung-erkennen")
        findet(index, "pilze", "erste-hilfe-vergiftung-erkennen")
        findet(index, "reinigungsmittel", "erste-hilfe-vergiftung-erkennen")
        findet(index, "giftig", "erste-hilfe-vergiftung-erkennen")
    }

    // Am 28.07.2026 beim Einbau der Kategorie Taktisch gemessen und gefangen:
    // "flucht" fuehrte auf einmal auf den Deeskalations-Tipp statt auf den
    // brennenden Raum, und "gefahr" schob die Vergiftung ueber die Atemwege aus
    // den ersten Treffern. Beides haengt an Schlagwoertern, die sich leicht
    // wieder einschleichen -- deshalb steht es hier fest.
    @Test
    fun neueKategorienVerdraengenKeineNotfallTreffer() {
        val index = index()
        ersterTreffer(index, "flucht", "erste-hilfe-brandrauch-sofort-raus")
        ersterTreffer(index, "gefahr", "erste-hilfe-vergiftung-atemwege")
        ersterTreffer(index, "abstand", "hygiene-ausscheidungen")
        ersterTreffer(index, "sicherheit", "erste-hilfe-eigenschutz")
    }

    // Gemessen am 19.08.2026, als die beiden Wassertipps dazukamen. Der Punkt
    // ist nicht, dass die Tipps existieren, sondern dass die Woerter, die
    // jemand wirklich tippt, an ihnen ankommen -- "wie trueb ist zu trueb"
    // tippt niemand, "trueb" und "brunnen" schon.
    @Test
    fun wasserStichwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        ersterTreffer(index, "trüb", "wasser-truebung-messen-muenze")
        ersterTreffer(index, "trübung", "wasser-truebung-messen-muenze")
        findet(index, "münze", "wasser-truebung-messen-muenze")
        findet(index, "ntu", "wasser-truebung-messen-muenze")
        findet(index, "schwebstoffe", "wasser-truebung-messen-muenze")

        ersterTreffer(index, "brunnen", "wasser-brunnen-nach-hochwasser")
        ersterTreffer(index, "hochwasser", "wasser-brunnen-nach-hochwasser")
        ersterTreffer(index, "überschwemmung", "wasser-brunnen-nach-hochwasser")
        findet(index, "chlorgas", "wasser-brunnen-nach-hochwasser")
        findet(index, "kalkhypochlorit", "wasser-brunnen-nach-hochwasser")

        // Und die Gegenprobe: Die Neuzugaenge duerfen den Bestand nicht
        // verdraengen.
        ersterTreffer(index, "wasser", "wasser-truebes-wasser-vorbehandeln")
        ersterTreffer(index, "abkochen", "wasser-abkochen")

        // BEI "chlor" WIRD ABSICHTLICH NUR AUF VORHANDENSEIN GEPRUEFT, nicht
        // auf den ersten Platz. Am 19.08.2026 gemessen und nachgesehen, woran
        // es liegt: Beide Tipps bekommen fuer "chlor" dieselbe Punktzahl, und
        // bei Gleichstand sortiert besteTreffer() nach dem TITEL --
        // "Freies Chlor..." steht alphabetisch vor "Trinkwasser mit Chlor...".
        // Eine Zusicherung auf Platz eins wuerde also nicht die Suchqualitaet
        // festhalten, sondern einen Anfangsbuchstaben, und beim naechsten
        // Titel irgendwo im Paket grundlos anschlagen. Beide stehen in der
        // Liste, beide sind eindeutig betitelt -- das ist, was zaehlt.
        findet(index, "chlor", "wasser-chlor-entkeimung")
        findet(index, "chlor", "wasser-freies-chlor-messen")
    }

    // Gemessen am 19.08.2026 beim Einbau der zweiten Wasserrunde.
    @Test
    fun wasserRundeZweiFuehrtZumRichtigenTipp() {
        val index = index()
        ersterTreffer(index, "freies chlor", "wasser-freies-chlor-messen")
        ersterTreffer(index, "restchlor", "wasser-freies-chlor-messen")
        ersterTreffer(index, "chlorgehalt", "wasser-freies-chlor-messen")
        ersterTreffer(index, "dpd", "wasser-freies-chlor-messen")

        ersterTreffer(index, "abkochgebot", "wasser-leitung-nach-ausfall")
        ersterTreffer(index, "leitung", "wasser-leitung-nach-ausfall")
        ersterTreffer(index, "wasserleitung", "wasser-leitung-nach-ausfall")
        ersterTreffer(index, "druckabfall", "wasser-leitung-nach-ausfall")
        ersterTreffer(index, "rohrbruch", "wasser-leitung-nach-ausfall")

        ersterTreffer(index, "filter", "wasser-filterarten")
        ersterTreffer(index, "keramikfilter", "wasser-filterarten")
        ersterTreffer(index, "membranfilter", "wasser-filterarten")
        ersterTreffer(index, "viren", "wasser-filterarten")
    }

    // Gemessen am 19.08.2026. "soldaten" fuehrte vorher auf den Grabenfuss und
    // "miliz" auf gar nichts -- wer das unter Druck tippt, meint keine
    // Fusspflege. Beide sind deshalb Schlagwoerter des Kontrollpunkt-Tipps.
    @Test
    fun kontrollpunktStichwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        ersterTreffer(index, "kontrollpunkt", "taktisch-kontrollpunkt")
        ersterTreffer(index, "straßensperre", "taktisch-kontrollpunkt")
        ersterTreffer(index, "schranke", "taktisch-kontrollpunkt")
        ersterTreffer(index, "bewaffnete", "taktisch-kontrollpunkt")
        ersterTreffer(index, "soldaten", "taktisch-kontrollpunkt")
        ersterTreffer(index, "miliz", "taktisch-kontrollpunkt")

        // Gegenprobe: Der Neuzugang darf die Gewalt-Grundlagen nicht kapern.
        ersterTreffer(index, "gewalt", "taktisch-worum-es-geht")
        ersterTreffer(index, "deeskalation", "taktisch-deeskalation")

        // Der Hinterhalt dazu, gemessen am 19.08.2026.
        ersterTreffer(index, "hinterhalt", "taktisch-hinterhalt")
        ersterTreffer(index, "aufgelauert", "taktisch-hinterhalt")
        ersterTreffer(index, "fluchtweg", "taktisch-hinterhalt")
        ersterTreffer(index, "konvoi", "taktisch-hinterhalt")
        findet(index, "überfall", "taktisch-hinterhalt")
    }

    // Gemessen am 19.08.2026. Vor diesem Eintrag fand das Paket zu
    // "blindgaenger", "munition", "granate", "sprengkoerper" und
    // "kampfmittel" NICHTS -- eine Luecke, die in Deutschland zaehlt, wo
    // Munition aus dem Zweiten Weltkrieg bis heute gefunden wird.
    @Test
    fun fundmunitionStichwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        ersterTreffer(index, "blindgänger", "taktisch-blindgaenger")
        ersterTreffer(index, "fundmunition", "taktisch-blindgaenger")
        ersterTreffer(index, "munition", "taktisch-blindgaenger")
        ersterTreffer(index, "granate", "taktisch-blindgaenger")
        ersterTreffer(index, "sprengkörper", "taktisch-blindgaenger")
        ersterTreffer(index, "kampfmittel", "taktisch-blindgaenger")
        ersterTreffer(index, "bombe", "taktisch-blindgaenger")
        findet(index, "sprengfalle", "taktisch-blindgaenger")

        // Die Minen dazu, gemessen am 19.08.2026.
        ersterTreffer(index, "mine", "taktisch-minen-gegenden")
        ersterTreffer(index, "minenfeld", "taktisch-minen-gegenden")
        ersterTreffer(index, "stolperdraht", "taktisch-minen-gegenden")
        ersterTreffer(index, "feldweg", "taktisch-minen-gegenden")
        ersterTreffer(index, "kampfgebiet", "taktisch-minen-gegenden")

        // Gegenprobe: "mine" darf die Vitamine nicht mitnehmen und umgekehrt.
        ersterTreffer(index, "vitamine", "agrikultur-bienen")

        // Beschuss, gemessen am 19.08.2026. "bombardierung" fand vorher nichts.
        ersterTreffer(index, "beschuss", "taktisch-beschuss-verhalten")
        ersterTreffer(index, "artillerie", "taktisch-beschuss-verhalten")
        ersterTreffer(index, "granatwerfer", "taktisch-beschuss-verhalten")
        ersterTreffer(index, "einschlag", "taktisch-beschuss-verhalten")
        ersterTreffer(index, "bombardierung", "taktisch-beschuss-verhalten")
        ersterTreffer(index, "deckung", "taktisch-beschuss-verhalten")

        // Und die Gegenprobe dazu: "explosion" gehoert weiter der Regel der
        // zwei Waende -- dort steht, WOHIN man geht, hier nur, WANN.
        ersterTreffer(index, "explosion", "taktisch-zwei-waende")
    }

    // Gemessen am 19.08.2026. Vor diesen beiden Eintraegen fand das Paket zu
    // "marschtempo", "gehgeschwindigkeit", "nachtmarsch", "nachtsicht" und
    // "rotlicht" NICHTS -- die Luecke stand seit dem 04.08.2026 in der
    // Buchauswertung als groesste verbliebene.
    @Test
    fun unterwegsStichwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        ersterTreffer(index, "marschtempo", "orientierung-marschtempo")
        ersterTreffer(index, "gehgeschwindigkeit", "orientierung-marschtempo")
        ersterTreffer(index, "langsamster", "orientierung-marschtempo")
        ersterTreffer(index, "steigung", "orientierung-marschtempo")
        ersterTreffer(index, "marsch", "orientierung-marschtempo")

        ersterTreffer(index, "nachtmarsch", "orientierung-nachts-gehen")
        ersterTreffer(index, "nachtsicht", "orientierung-nachts-gehen")
        ersterTreffer(index, "rotlicht", "orientierung-nachts-gehen")
        ersterTreffer(index, "taschenlampe", "orientierung-nachts-gehen")
        ersterTreffer(index, "stolpern", "orientierung-nachts-gehen")

        // Gegenprobe: Der Bestand der Orientierung bleibt, wo er war.
        ersterTreffer(index, "kompass", "orientierung-behelfskompass")
        ersterTreffer(index, "polarstern", "orientierung-polarstern")
        ersterTreffer(index, "entfernung", "orientierung-schritte-zaehlen")
    }

    // Gemessen am 19.08.2026. Der Eintrag zum versinkenden Auto hiess zuerst
    // "Auto im Wasser ..." -- und hat damit bei der Eingabe "wasser" die
    // Wasseraufbereitung vom ersten Platz verdraengt. Der Titelwaechter hat es
    // gemeldet, der Titel traegt das Wort jetzt nicht mehr, und die Gegenprobe
    // unten haelt es fest.
    @Test
    fun fahrzeugStichwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        ersterTreffer(index, "auto versinkt", "taktisch-auto-im-wasser")
        ersterTreffer(index, "ertrinken im auto", "taktisch-auto-im-wasser")
        ersterTreffer(index, "türverriegelung", "taktisch-auto-im-wasser")

        ersterTreffer(index, "bremsen versagen", "taktisch-bremsen-versagen")
        ersterTreffer(index, "bremsversagen", "taktisch-bremsen-versagen")
        ersterTreffer(index, "handbremse", "taktisch-bremsen-versagen")
        ersterTreffer(index, "herunterschalten", "taktisch-bremsen-versagen")
        ersterTreffer(index, "airbag", "taktisch-bremsen-versagen")

        // Die Gegenprobe, um die es hier wirklich geht.
        ersterTreffer(index, "wasser", "wasser-truebes-wasser-vorbehandeln")
        ersterTreffer(index, "hochwasser", "wasser-brunnen-nach-hochwasser")
    }

    // Gemessen am 19.08.2026. Vorher fand das Paket zu "wolken",
    // "wettervorhersage", "halo" und "wetterumschwung" nichts.
    @Test
    fun wetterStichwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        ersterTreffer(index, "wolken", "taktisch-wolken-lesen")
        ersterTreffer(index, "wetter", "taktisch-wolken-lesen")
        ersterTreffer(index, "wettervorhersage", "taktisch-wolken-lesen")
        ersterTreffer(index, "halo", "taktisch-wolken-lesen")
        ersterTreffer(index, "wetterumschwung", "taktisch-wolken-lesen")
        ersterTreffer(index, "schäfchenwolken", "taktisch-wolken-lesen")

        // Gegenprobe: Im Notfall zaehlt der Blitzschlag, nicht die Wolkenkunde.
        ersterTreffer(index, "gewitter", "erste-hilfe-blitzschlag")
        ersterTreffer(index, "sturm", "taktisch-sturm-verhalten")

        // Die Himmelszeichen dazu, gemessen am 19.08.2026. Vorher fand das
        // Paket zu keinem dieser Woerter etwas.
        ersterTreffer(index, "abendrot", "taktisch-himmelszeichen")
        ersterTreffer(index, "morgenrot", "taktisch-himmelszeichen")
        ersterTreffer(index, "talnebel", "taktisch-himmelszeichen")
        ersterTreffer(index, "regenschatten", "taktisch-himmelszeichen")
        ersterTreffer(index, "senke", "taktisch-himmelszeichen")

        // Und die Abgrenzung: Die Wolken bleiben bei den Wolken.
        ersterTreffer(index, "wolken", "taktisch-wolken-lesen")
        ersterTreffer(index, "hof um den mond", "taktisch-wolken-lesen")
    }

    // Gemessen am 19.08.2026. Chemie war vorher ein blinder Fleck: zu
    // "chemieunfall", "giftwolke", "kampfstoff", "reizgas", "dekontamination"
    // und "abdichten" fand das Paket nichts.
    @Test
    fun chemieStichwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        ersterTreffer(index, "giftwolke", "taktisch-giftwolke")
        ersterTreffer(index, "chemieunfall", "taktisch-giftwolke")
        ersterTreffer(index, "gasaustritt", "taktisch-giftwolke")
        ersterTreffer(index, "kampfstoff", "taktisch-giftwolke")
        ersterTreffer(index, "reizgas", "taktisch-giftwolke")
        ersterTreffer(index, "tanklaster", "taktisch-giftwolke")

        ersterTreffer(index, "abgedichteter raum", "taktisch-abgedichteter-raum")
        ersterTreffer(index, "schutzraum", "taktisch-abgedichteter-raum")
        ersterTreffer(index, "dekontamination", "taktisch-abgedichteter-raum")
        ersterTreffer(index, "fenster abkleben", "taktisch-abgedichteter-raum")

        // DIE GEGENPROBE, UM DIE ES HIER GEHT. Der zweite Tipp hiess zuerst
        // "... bei Chemie nach oben, bei Strahlung nach unten" -- und hat damit
        // bei der Eingabe "strahlung" den Erste-Hilfe-Tipp vom ersten Platz
        // verdraengt. Gefunden hat es diese Messung; der Titelwaechter kannte
        // das Wort damals noch nicht. Jetzt kennt er es, und hier steht es fest.
        ersterTreffer(index, "strahlung", "erste-hilfe-strahlung")
        ersterTreffer(index, "atomschlag", "erste-hilfe-strahlung")
        ersterTreffer(index, "sirene", "taktisch-sirene-verstehen")
    }

    // Gemessen am 19.08.2026. Der Eintrag hiess zuerst "Im kalten Wasser
    // treiben ..." und hat damit gleich drei Anfragen gekapert: "wasser" von
    // der Aufbereitung, "kalt" von der Unterkuehlung und "auskuehlen" vom
    // Waermeerhalt -- die letzten beiden ueber Schlagwoerter, die mit
    // denselben Buchstaben anfangen. Titel und Schlagwoerter sind geaendert;
    // die Gegenproben unten halten es fest.
    @Test
    fun kaltwasserStichwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        ersterTreffer(index, "ins wasser gefallen", "erste-hilfe-kaltes-wasser-treiben")
        ersterTreffer(index, "über bord", "erste-hilfe-kaltes-wasser-treiben")
        ersterTreffer(index, "gekentert", "erste-hilfe-kaltes-wasser-treiben")
        ersterTreffer(index, "schwimmweste", "erste-hilfe-kaltes-wasser-treiben")

        // Die Selbstrettung bleibt vorn, wo sie hingehoert -- sie ist der
        // dringlichere Fall.
        ersterTreffer(index, "eingebrochen", "erste-hilfe-eiseinbruch-selbstrettung")
        ersterTreffer(index, "eiswasser", "erste-hilfe-eiseinbruch-selbstrettung")

        // Und die drei gekaperten Anfragen, zurueck an ihrem Platz.
        ersterTreffer(index, "wasser", "wasser-truebes-wasser-vorbehandeln")
        ersterTreffer(index, "auskühlen", "erste-hilfe-waermeerhalt")
        ersterTreffer(index, "unterkühlung", "erste-hilfe-unterkuehlung-stadium-eins")
        ersterTreffer(index, "dünnes eis", "taktisch-eis-und-schnee-gehen")
    }

    // Gemessen am 19.08.2026, und der Anlass ist ein Fehler in meiner eigenen
    // Messweise: Luecken hatte ich bis dahin gesucht, indem ich Titel und Text
    // nach einem Wort durchsucht habe. Das geht an den SCHLAGWOERTERN vorbei --
    // "fettbrand" steht in keinem Titel und in keinem Text, aber als Schlagwort,
    // und der Fettbrand ist laengst abgedeckt. Mit der Suche selbst gemessen,
    // war die echte Luecke eine andere: "verirrt" fuehrte auf den Schattenstock,
    // also auf die Frage, wie man Norden findet. Wer das tippt, fragt aber, was
    // jetzt zu tun ist.
    @Test
    fun verirrtFuehrtZumRichtigenTipp() {
        val index = index()
        ersterTreffer(index, "verirrt", "orientierung-verlaufen")
        ersterTreffer(index, "verlaufen", "orientierung-verlaufen")
        ersterTreffer(index, "orientierung verloren", "orientierung-verlaufen")

        // Und die Gegenprobe: Die beiden Verfahren bleiben, wo sie hingehoeren.
        // "wo bin ich" ist die Frage nach dem Standort, nicht nach dem Verhalten.
        ersterTreffer(index, "wo bin ich", "orientierung-kreuzpeilung")
        ersterTreffer(index, "norden finden", "orientierung-schattenstock")
        ersterTreffer(index, "fettbrand", "erste-hilfe-brand-reihenfolge")

        // Gemessen am 19.08.2026: "jemand fehlt" fand nichts und "kind
        // vermisst" fuehrte auf die Hirnhautentzuendung. Damals fuehrte
        // "vermisst" auf den Eintrag zur ABSPRACHE VORHER -- mangels eines
        // besseren Ziels, ausdruecklich ohne neuen Eintrag.
        //
        // Am selben Tag spaeter geaendert: Es gibt jetzt einen eigenen
        // Eintrag fuer den Fall, dass jemand NICHT ZURUECKGEKOMMEN ist. Wer
        // "vermisst" tippt, hat das Problem gerade -- der gehoert nach vorn.
        // Die Absprache vorher bleibt ueber ihre eigenen Woerter erreichbar,
        // und das sichern die zwei Gegenproben darunter ab.
        ersterTreffer(index, "vermisst", "taktisch-vermisst-wegpunkte")
        ersterTreffer(index, "jemand fehlt", "taktisch-vermisst-wegpunkte")
        ersterTreffer(index, "wenn einer weggeht", "taktisch-weggehen-absprache")
        ersterTreffer(index, "absprache vorher", "taktisch-weggehen-absprache")
        ersterTreffer(index, "kind vermisst", "taktisch-vermisst-wegpunkte")

        // Gegenprobe: Die Notfaelle der Kleinen bleiben unberuehrt.
        ersterTreffer(index, "kind", "erste-hilfe-ersticken-kind")
        ersterTreffer(index, "säugling", "erste-hilfe-ersticken-saeugling")
        ersterTreffer(index, "baby", "erste-hilfe-ersticken-saeugling")
    }

    @Test
    fun taktischeStichwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        findet(index, "deeskalation", "taktisch-deeskalation")
        findet(index, "aggressiv", "taktisch-anzeichen-gewalt")
        findet(index, "gewalt", "taktisch-drei-phasen")
        findet(index, "angriff", "taktisch-nicht-hineingehen")
    }

    private fun ersterTreffer(index: SearchIndex, anfrage: String, erwartet: String) {
        val treffer = index.search(anfrage, limit = 8).map { it.id }
        assertTrue(
            treffer.firstOrNull() == erwartet,
            "\"$anfrage\" fuehrt zuerst auf ${treffer.firstOrNull() ?: "nichts"}, erwartet $erwartet",
        )
    }

    @Test
    fun dieWichtigstenFachwoerterFuehrenZumRichtigenTipp() {
        val index = index()
        findet(index, "ersticken", "erste-hilfe-ersticken-oberbauchkompression")
        findet(index, "säugling", "erste-hilfe-ersticken-saeugling")
        findet(index, "kind", "erste-hilfe-wiederbelebung-kind")
        findet(index, "atmung", "erste-hilfe-atmung-pruefen")
        findet(index, "bewusstlos", "erste-hilfe-stabile-seitenlage")
        findet(index, "blutung", "erste-hilfe-starke-blutung")
        findet(index, "druckverband", "erste-hilfe-druckverband")
        findet(index, "wunde", "erste-hilfe-wunde-verbote")
        findet(index, "unterkühlung", "erste-hilfe-unterkuehlung-stadium-eins")
        findet(index, "hitzschlag", "erste-hilfe-hitzschlag-handeln")
        findet(index, "knochen", "erste-hilfe-knochenbruch-versorgen")
        findet(index, "wasser", "wasser-abkochen")
    }

    private fun unterDenErsten(index: SearchIndex, anfrage: String, erwartet: String, wieviele: Int) {
        val treffer = index.search(anfrage, limit = wieviele).map { it.id }
        assertTrue(
            erwartet in treffer,
            "\"$anfrage\" zeigt $erwartet nicht unter den ersten $wieviele. " +
                "Gefunden: ${treffer.joinToString()}",
        )
    }

    private fun zuerst(index: SearchIndex, anfrage: String, erwartet: String) {
        val treffer = index.search(anfrage, limit = 8).map { it.id }
        assertTrue(
            treffer.firstOrNull() == erwartet,
            "\"$anfrage\" fuehrt zuerst auf ${treffer.firstOrNull() ?: "nichts"}, erwartet $erwartet. " +
                "Gefunden: ${treffer.joinToString()}",
        )
    }

    // Gemessen am 28.07.2026 beim Einbau der Nahrungs-Tipps. Jede dieser Zeilen
    // stand vorher anders da: Die neuen Tipps hatten den Latrinen-, den
    // Vergiftungs- und den Durchfall-Tipp aus dem ersten Platz gedraengt, weil
    // die Suche bei Gleichstand alphabetisch nach Titel sortiert. Ein Tipp zum
    // Ausnehmen von Wild darf nicht vor einer Notfall-Anleitung stehen.
    @Test
    fun dieNahrungsTippsVerdraengenKeinenNotfallTipp() {
        val index = index()
        zuerst(index, "kot", "hygiene-ausscheidungen")
        zuerst(index, "pilze", "erste-hilfe-vergiftung-erkennen")
        // Am 29.07.2026 geaendert. Bis dahin stand hier der Durchfall-Tipp --
        // nicht weil er der beste Treffer fuer "fieber" waere, sondern weil es
        // zu Fieber ueberhaupt keinen eigenen Eintrag gab. Das Wort kam in
        // fast zwanzig Tipps als Warnzeichen vor, und nirgends stand, ab wann
        // Fieber gefaehrlich ist. Jetzt gibt es den Eintrag, und er gehoert
        // nach oben. Die urspruengliche Absicht dieser Zeile -- kein
        // Nahrungs-Tipp draengt eine Notfall-Anleitung weg -- bleibt erhalten
        // und wird sogar genauer geprueft: Der Durchfall-Tipp muss weiterhin
        // unter den ersten dreien stehen.
        zuerst(index, "fieber", "erste-hilfe-fieber-gefahr")
        unterDenErsten(index, "fieber", "erste-hilfe-durchfall-hilfe-holen", 3)
        zuerst(index, "rauch", "erste-hilfe-brandrauch-nicht-hineingehen")
        zuerst(index, "kühlen", "erste-hilfe-amputat")
        zuerst(index, "wärme", "erste-hilfe-waermeerhalt")
        zuerst(index, "auskühlen", "erste-hilfe-waermeerhalt")
        // Die beiden Krankheits-Tipps (Trichinellose, Tularaemie) zaehlen lange
        // Symptomlisten auf und greifen damit in fremde Notfaelle hinein.
        zuerst(index, "husten", "erste-hilfe-ersticken-kann-husten")
        zuerst(index, "brustschmerzen", "erste-hilfe-brustschmerzen")
        zuerst(index, "handschuhe", "erste-hilfe-wunde-bedecken")
        zuerst(index, "erbrechen", "erste-hilfe-erbrechen-helfen")
        // Und umgekehrt: Wer "fleisch" sucht, braucht zuerst die Regel, die das
        // Fleisch sicher macht -- nicht die Beschreibung, wie es verdirbt.
        zuerst(index, "fleisch", "nahrung-wildfleisch-durchgaren")
        // Zwei Wege, ein Erreger: Wer den Namen der Krankheit sucht, will die
        // Anzeichen; wer "hasenpest" oder "abbalgen" tippt, will den Schutz.
        zuerst(index, "tularämie", "nahrung-tularaemie-anzeichen")
        zuerst(index, "hasenpest", "nahrung-hasenpest-beim-verarbeiten")
        zuerst(index, "hase", "nahrung-hasenpest-beim-verarbeiten")
        // Der Haftungshinweis und der Kurs-Hinweis sind Meta-Tipps. Sie duerfen
        // nie vor einer Handlungsanweisung stehen. Gemessen am 28.07.2026: Mit
        // "Erste-Hilfe-Kurs" im Titel gewann der Kurs-Hinweis "hilfe" und
        // "arzt" -- der Titel wurde deshalb umgeschrieben.
        zuerst(index, "hilfe", "erste-hilfe-durchfall-hilfe-holen")
        zuerst(index, "arzt", "erste-hilfe-durchfall-hilfe-holen")
        zuerst(index, "notruf", "erste-hilfe-notruf-112")
        // Umgekehrt: Wer nach einer Mengenangabe fragt, soll zuerst lesen, was
        // fuer alle Mengenangaben im Paket gilt.
        zuerst(index, "dosierung", "hinweis-angaben-ohne-gewaehr")
        // Gemessen am 28.07.2026 beim Einbau der Fisch-Tipps: Der
        // Petermaennchen-Tipp hiess zuerst "Stich mit heissem Wasser
        // behandeln" und gewann damit "wasser" -- den zentralsten Suchbegriff
        // des Pakets -- gegen alle drei Entkeimungs-Tipps. Mit "Stich" im
        // Titel haette er ausserdem den Insektenstich im Rachen verdraengt.
        zuerst(index, "wasser", "wasser-truebes-wasser-vorbehandeln")
        zuerst(index, "stich", "erste-hilfe-insektenstich-mund")
        // Und die neuen Fisch-Tipps sollen fuehren, wo sie hingehoeren.
        zuerst(index, "fisch", "nahrung-fisch-durchgaren")
        zuerst(index, "botulismus", "nahrung-einkochen-botulismus")
        // Zecken: Wer gerade eine im Bein hat, braucht das Entfernen und nicht
        // die Beschreibung der Wanderroete. Gemessen am 28.07.2026 stand es
        // umgekehrt, weil "Zeckenstich" im Titel des Erkennen-Tipps stand.
        zuerst(index, "zecke", "erste-hilfe-zecke-entfernen")
        zuerst(index, "zeckenstich", "erste-hilfe-zecke-entfernen")
        zuerst(index, "wanderröte", "erste-hilfe-wanderroete")
        zuerst(index, "borreliose", "erste-hilfe-borreliose-behandeln")
        zuerst(index, "fsme", "erste-hilfe-fsme")
        zuerst(index, "antibiotika", "erste-hilfe-antibiotika-reste")
    }

    // Gemessen am 28.07.2026 beim Einbau der beiden CO-Melder-Tipps. Ihre Titel
    // greifen in drei fremde Wortfelder: "Kohlenmonoxid" (beide bestehenden
    // CO-Tipps), "raus" und "sofort" (Brandrauch) sowie "Haus". Bei
    // Punktgleichstand entscheidet der Titel alphabetisch, und beide neuen
    // Titel beginnen mit "W" -- deshalb bleiben die bestehenden Tipps vorn.
    // Das ist kein Zufall, sondern der Grund fuer die Titelwahl: Ein
    // Vorsorge-Tipp darf nicht vor einer Notfall-Anleitung stehen.
    @Test
    fun dieCoMelderTippsVerdraengenKeinenNotfallTipp() {
        val index = index()
        // Was im Ernstfall zaehlt, bleibt vorn.
        zuerst(index, "kohlenmonoxid", "erste-hilfe-kohlenmonoxid-erkennen")
        zuerst(index, "co", "erste-hilfe-kohlenmonoxid-erkennen")
        zuerst(index, "raus", "erste-hilfe-brandrauch-sofort-raus")
        zuerst(index, "sofort", "erste-hilfe-brandrauch-sofort-raus")
        zuerst(index, "haus", "erste-hilfe-brandrauch-nicht-hineingehen")
        zuerst(index, "rauchmelder", "erste-hilfe-brandrauch-sofort-raus")
        zuerst(index, "vergiftung", "erste-hilfe-vergiftung-erkennen")
        zuerst(index, "lüften", "erste-hilfe-kohlenmonoxid-handeln")
        // Der Haftungshinweis traegt "warnung" als Schlagwort. Ein Titel mit
        // "Warnung" statt "Warnmelder" haette ihn verdraengt -- deshalb steht
        // dort "Warnmelder".
        zuerst(index, "warnung", "hinweis-angaben-ohne-gewaehr")
        // Die Geraete und Anlaesse teilen sich beide Tipps mit dem
        // Erkennen-Tipp. Wer sie tippt, steht moeglicherweise im Raum.
        for (
            anfrage in listOf(
                "grill", "kamin", "ofen", "heizung", "gastherme", "stromausfall", "notheizung",
            )
        ) {
            zuerst(index, anfrage, "erste-hilfe-kohlenmonoxid-erkennen")
        }
        // Und umgekehrt: die neuen Woerter fuehren dorthin, wo sie hingehoeren.
        zuerst(index, "melder", "erste-hilfe-kohlenmonoxid-melder")
        zuerst(index, "co-melder", "erste-hilfe-kohlenmonoxid-melder")
        zuerst(index, "warnmelder", "erste-hilfe-kohlenmonoxid-melder")
        zuerst(index, "en 50291", "erste-hilfe-kohlenmonoxid-melder")
        zuerst(index, "batterie", "erste-hilfe-kohlenmonoxid-melder")
        zuerst(index, "wohnwagen", "erste-hilfe-kohlenmonoxid-melder")
        // "alarm" fand vor dieser Runde gar nichts. Es muss zum Verhalten
        // fuehren, nicht zur Anschaffung.
        zuerst(index, "alarm", "erste-hilfe-kohlenmonoxid-alarm")
        zuerst(index, "evakuieren", "erste-hilfe-kohlenmonoxid-alarm")
        zuerst(index, "nachbarn", "erste-hilfe-kohlenmonoxid-alarm")
    }

    // Wer bei einem Kind sucht, darf nicht bei der Erwachsenen-Anleitung landen,
    // ohne die Kinder-Fassung ueberhaupt zu sehen -- bei Saeuglingen ist das
    // Verfahren ein anderes, und das falsche kann toedlich sein.
    @Test
    fun beiKindernFuehrtDieSucheZuDenKinderTipps() {
        val index = index()
        for (wort in listOf("kind", "kinder", "baby", "säugling", "kleinkind")) {
            val treffer = index.search(wort, limit = 8).map { it.id }
            assertTrue(
                treffer.any { it.contains("kind") || it.contains("saeugling") },
                "\"$wort\" fuehrt zu keinem Kinder-Tipp, gefunden: $treffer",
            )
        }
    }

    // Wer allein ist und sich verschluckt, tippt nicht "Fremdkoerperverlegung".
    // Und die vorhandene Helfer-Kette darf dadurch nicht verrutschen: Wer
    // "ersticken" sucht, braucht weiter zuerst das Vorgehen am anderen.
    @Test
    fun derAlleinFallBeimErstickenIstFindbarUndVerdraengtNichts() {
        val index = index()
        for (wort in listOf("stuhllehne", "tischkante", "allein", "bissen")) {
            zuerst(index, wort, "erste-hilfe-ersticken-allein")
        }
        findet(index, "niemand da zum helfen", "erste-hilfe-ersticken-allein")

        // Unveraendert: die Helfer-Kette bleibt vorn.
        val beiErsticken = index.search("ersticken", limit = 5).map { it.id }
        assertTrue(
            "erste-hilfe-ersticken-allein" !in beiErsticken,
            "der Allein-Tipp hat sich vor die Helfer-Kette geschoben: $beiErsticken",
        )
    }

    /**
     * Die sechs Eintraege vom 17./18.08.2026 -- und zwar mit den Woertern, die
     * jemand wirklich tippt.
     *
     * SIE LAGEN SECHS WOCHEN AUF HALDE, weil das Wortbudget voll war; eingebaut
     * wurden sie erst, als die Grenze auf 450 000 gesetzt wurde. Genau bei
     * einem solchen Schwung ist die Suchprobe faellig: Ein Eintrag, den man
     * unter Druck nicht findet, ist nicht geschrieben.
     *
     * Gesucht wird mit UMLAUTEN und ueber SearchIndex -- nicht mit
     * `nachschlagen.py` und nicht in ASCII-Umschrift. Am 16.08.2026 sind auf
     * genau diesem Weg sieben Luecken gemeldet worden, die es nicht gab.
     */
    @Test
    fun dieNeuenEintraegeSindUnterDruckAuffindbar() {
        val index = index()

        // Entzuendetes Nagelbett: niemand kennt das Wort, alle kennen den Finger.
        findet(index, "nagel entzündet", "medizin-nagelbettentzuendung")
        findet(index, "eiter finger", "medizin-nagelbettentzuendung")

        // Etwas im Ohr -- der haeufigste Anlass ist ein Kind oder ein Insekt.
        findet(index, "insekt im ohr", "medizin-fremdkoerper-ohr")
        findet(index, "etwas im ohr", "medizin-fremdkoerper-ohr")

        // Lichtempfindlichkeit durch Medikamente: Der Betroffene sucht nach dem,
        // was er sieht -- Sonnenbrand --, nicht nach der Ursache.
        //
        // HIER STEHT "MEDIKAMENT" UND NICHT "TABLETTEN", und das ist eine
        // Entscheidung und kein Zufall: "sonnenbrand tabletten" fand am
        // 18.08.2026 gar nichts, weil der Eintrag das Wort Tabletten nirgends
        // benutzt. Es als Schlagwort nachzutragen war der naheliegende Griff --
        // und hat sofort "erste-hilfe-vergiftung-erkennen" aus den Treffern zu
        // "tabletten" verdraengt. Ein Sonnenausschlag darf eine Vergiftung
        // nicht zudecken; das Schlagwort ist deshalb wieder raus.
        findet(index, "sonnenbrand medikament", "medizin-lichtempfindlichkeit-medikamente")
        findet(index, "lichtempfindlich", "medizin-lichtempfindlichkeit-medikamente")

        // Umgeknickt ist das Wort, Verstauchung die Diagnose.
        findet(index, "umgeknickt", "erste-hilfe-verstauchung-zerrung")
        findet(index, "verstaucht", "erste-hilfe-verstauchung-zerrung")

        // Beim Auge zaehlt, dass der harmlose Anlass zum ernsten Befund fuehrt.
        findet(index, "blaues auge", "medizin-schlag-aufs-auge")
        findet(index, "doppelbilder", "medizin-schlag-aufs-auge")

        // Schlaf: die Beschreibung des Zustands, nicht sein Fachwort.
        findet(index, "kann nicht schlafen", "medizin-schlaf-in-der-krise")
        findet(index, "wach liegen", "medizin-schlaf-in-der-krise")
    }

    /**
     * Was die sechs Neuen NICHT verdraengen duerfen.
     *
     * Ein neuer Eintrag nimmt Platz in der Trefferliste weg, und die
     * Titelgewichtung sorgt dafuer, dass er sich weit vorn einsortiert. Bei
     * "auge" und "ohr" stehen die vorhandenen Eintraege fuer die AKUTEN Faelle
     * -- Veraetzung, Fremdkoerper im Auge --, und die duerfen nicht hinter
     * einem blauen Auge verschwinden.
     */
    @Test
    fun dieNeuenEintraegeVerdraengenDieAkutenNicht() {
        val index = index()
        for ((anfrage, muss) in listOf(
            "auge" to "medizin-augenverletzung",
            "augen" to "medizin-augenverletzung",
        )) {
            val treffer = index.search(anfrage, limit = 5).map { it.id }
            assertTrue(
                muss in treffer,
                "\"$anfrage\" zeigt $muss nicht mehr unter den ersten fünf: $treffer",
            )
        }
    }

    private fun ersteTreffer(index: SearchIndex, anfrage: String, erwartet: String) {
        val treffer = index.search(anfrage, limit = 5).map { it.id }
        assertTrue(
            treffer.firstOrNull() == erwartet,
            "\"$anfrage\" muss zuerst $erwartet zeigen. Gefunden: " +
                if (treffer.isEmpty()) "nichts" else treffer.joinToString(),
        )
    }

    /**
     * EIN KIND ATMET NICHT -- die schlimmste Anfrage, die dieses Handbuch
     * bekommen kann, und am 19.08.2026 hat sie dreifach versagt:
     *
     * "kind atmet nicht" fuehrte auf die LUNGENENTZUENDUNG, weil deren Titel
     * "Wenn ein Kind zu schnell atmet" sowohl "Kind" als auch "atmet" im
     * staerksten Feld traegt -- die Wiederbelebung stand nicht einmal unter den
     * ersten vier. "kind bewusstlos" fuehrte auf die Hirnhautentzuendung, weil
     * das Wort "bewusstlos" im Wiederbelebungs-Eintrag gar nicht vorkam. Und
     * "kind reanimieren" fand NICHTS, weil dort "Reanimation" steht und die
     * Suche nur Wortanfaenge kennt: Das laengere getippte Wort passt auf das
     * kuerzere geschriebene nicht.
     *
     * Behoben wurde das am Inhalt, nicht an der Rangfolge: Der Eintrag heisst
     * jetzt nach der LAGE ("Kind atmet nicht") statt nach der MASSNAHME
     * ("Wiederbelebung Kind"), und die fehlenden Woerter sind Schlagwoerter.
     * Der Titel ist das staerkste Suchfeld -- er gehoert dem, der gesucht wird.
     */
    @Test
    fun einKindDasNichtAtmetFuehrtSofortAufDieWiederbelebung() {
        val index = index()
        for (anfrage in listOf(
            "kind atmet nicht",
            "mein kind atmet nicht",
            "baby atmet nicht",
            "säugling atmet nicht",
            "kind bewusstlos",
            "kind reagiert nicht",
            "kind reanimieren",
            "kind wiederbeleben",
        )) {
            ersteTreffer(index, anfrage, "erste-hilfe-wiederbelebung-kind")
        }
    }

    /**
     * Und die Gegenprobe: Der Lungenentzuendungs-Eintrag behaelt seine eigene
     * Frage. Ein Kind, das ZU SCHNELL atmet, ist ein anderer Fall als eines,
     * das NICHT atmet -- und die Verwechslung darf in keine der beiden
     * Richtungen gehen.
     */
    @Test
    fun dasSchnellAtmendeKindBleibtBeiDerLungenentzuendung() {
        val index = index()
        ersteTreffer(index, "kind zu schnell atmet", "medizin-lungenentzuendung-kind")
        // Beim Erwachsenen bleibt die Entscheidung an der Atmung vorn: Sie
        // trennt Seitenlage und Wiederbelebung und gilt fuer jedes Alter.
        ersteTreffer(index, "atmet nicht", "erste-hilfe-entscheidung-nach-atemkontrolle")
        ersteTreffer(index, "person atmet nicht mehr", "erste-hilfe-entscheidung-nach-atemkontrolle")
        ersteTreffer(index, "bewusstlos", "erste-hilfe-entscheidung-nach-atemkontrolle")
    }

    /**
     * Und dass eine Anfrage aus mehreren Woertern ueberhaupt etwas findet.
     *
     * Gemessen am 19.08.2026 an 25 nachgestellten Notfall-Anfragen: sieben
     * blieben leer, darunter "mein kind atmet nicht", "hund hat mich gebissen"
     * und "deckung suchen" -- obwohl es zu jeder einen passenden Eintrag gibt.
     * Der Grund lag nicht am Inhalt, sondern daran, dass frueher JEDES Wort in
     * DEMSELBEN Eintrag stehen musste.
     */
    @Test
    fun ganzeSaetzeLaufenNichtInsLeere() {
        val index = index()
        for (anfrage in listOf(
            "mein kind atmet nicht",
            "hund hat mich gebissen",
            "deckung suchen",
            "wo verstecken bei beschuss",
            "mir ist sehr kalt",
            "essen finden im wald",
            "ich friere stark",
        )) {
            assertTrue(
                index.search(anfrage, limit = 5).isNotEmpty(),
                "\"$anfrage\" findet nichts -- unter Druck tippt niemand ein einzelnes Wort",
            )
        }
        findet(index, "hund hat mich gebissen", "medizin-tollwut")
        // Seit dem Scharfschuetzen-Eintrag beantworten ZWEI Eintraege diese
        // Frage: der eine den Unterschied zwischen Deckung und Sichtschutz,
        // der andere das Verhalten bei Artillerie. Beide muessen kommen.
        findet(index, "deckung suchen", "taktisch-beschuss-verhalten")
        findet(index, "deckung suchen", "taktisch-schuetzenfeuer")
    }


    /**
     * Wonach jemand tippt, und wie das Handbuch es nennt.
     *
     * Gemessen am 19.08.2026, und zwar an drei Faellen, in denen genau diese
     * Luecke klaffte:
     *
     * "blutung stoppen" fuehrte auf die goldene Stunde und den Schlangenbiss,
     * weil das Handbuch "Blutung stillen" sagt. Wer blutet, tippt "stoppen".
     *
     * "starke blutung" fuehrte auf die REGELBLUTUNG, weil deren Eintrag das
     * Wort "stark" im Titel traegt und der Blutstillungs-Eintrag nicht.
     */
    @Test
    fun dasWortDesLaienFuehrtZumWortDesHandbuchs() {
        val index = index()
        ersteTreffer(index, "blutung stoppen", "erste-hilfe-starke-blutung")
        ersteTreffer(index, "blutung stillen", "erste-hilfe-starke-blutung")
        ersteTreffer(index, "starke blutung", "erste-hilfe-starke-blutung")
    }

    /**
     * Und die Gegenprobe: Eine zu starke Regelblutung ist etwas anderes als
     * eine spritzende Wunde. Beide Fragen muessen bei ihrem eigenen Eintrag
     * bleiben -- eine Frau, die wegen ihrer Periode sucht, darf nicht bei der
     * Blutstillung landen, und umgekehrt erst recht nicht.
     */
    @Test
    fun dieRegelblutungBehaeltIhreEigeneFrage() {
        val index = index()
        ersteTreffer(index, "regelblutung", "medizin-regelblutung-stark")
        ersteTreffer(index, "starke regelblutung", "medizin-regelblutung-stark")
        ersteTreffer(index, "periode zu stark", "medizin-regelblutung-stark")
    }


    /**
     * Zwei Anfragen, bei denen das Wort in beide Richtungen zeigt.
     *
     * "unter strom" fuehrte am 19.08.2026 auf "Lagern ohne Strom" -- also auf
     * den Vorratsschrank, waehrend jemand an einer Leitung haengt. Und
     * "person im wasser" fuehrte auf den HEXENSCHUSS, weil dort "Person" und
     * "Wasser" beilaeufig vorkommen.
     *
     * Beides sind Worte, die es im Handbuch zweimal gibt: Strom als Versorgung
     * und Strom als Unfall, Wasser als Vorrat und Wasser als Gefahr. Der
     * NOTFALL muss vorn stehen -- wer nur die Versorgung sucht, hat Zeit zum
     * Weiterlesen; wer jemanden aus dem Wasser holt, nicht.
     */
    @Test
    fun beiDoppeldeutigenWoerternStehtDerNotfallVorn() {
        val index = index()
        ersteTreffer(index, "unter strom", "erste-hilfe-stromunfall")
        ersteTreffer(index, "steht unter strom", "erste-hilfe-stromunfall")
        ersteTreffer(index, "stromschlag", "erste-hilfe-stromunfall")
        ersteTreffer(index, "person im wasser", "erste-hilfe-ertrinken")
        ersteTreffer(index, "ertrinken", "erste-hilfe-ertrinken")
        // Gegenprobe: Der Stromausfall behaelt seine eigenen Fragen.
        ersteTreffer(index, "kein strom", "agrikultur-lagern-ohne-strom")
        ersteTreffer(index, "strom ist weg", "agrikultur-lagern-ohne-strom")
    }


    /**
     * Woerter aus dem Alltag, die im Handbuch anders heissen. Gemessen am
     * 19.08.2026, und die Ergebnisse waren teils grotesk:
     *
     *   "fluchtrucksack"        fand NICHTS -- der Eintrag heisst "Notgepaeck".
     *   "ich habe angst"        fuehrte auf BLUTHOCHDRUCK.
     *   "kind hat angst"        fuehrte auf Fieber und Pseudokrupp.
     *   "handy tot"             fuehrte auf Unterkuehlung Stadium III und auf
     *                           "Tote bergen" -- das Wort "tot" nahm die
     *                           Anfrage woertlich.
     *   "brunnenwasser trinken" fuehrte auf die Hasenpest beim Verarbeiten.
     *   "regenwasser trinken"   fand den Eintrag zum Regenwasser nicht, weil
     *                           dort nur "regenwasser" ohne "trinken" stand.
     *
     * In allen Faellen gab es den richtigen Eintrag -- er kannte nur das Wort
     * nicht, mit dem jemand danach sucht.
     */
    @Test
    fun alltagswoerterFindenDenPassendenEintrag() {
        val index = index()
        ersteTreffer(index, "fluchtrucksack", "taktisch-notgepaeck")
        ersteTreffer(index, "notfallrucksack", "taktisch-notgepaeck")
        ersteTreffer(index, "was mitnehmen", "taktisch-notgepaeck")
        ersteTreffer(index, "ich habe angst", "medizin-angst-beruhigen")
        ersteTreffer(index, "kind hat angst", "medizin-kindern-erklaeren")
        ersteTreffer(index, "handy tot", "taktisch-ausfall-folgen")
        ersteTreffer(index, "handy leer", "taktisch-ausfall-folgen")
        ersteTreffer(index, "regenwasser trinken", "wasser-regen-vom-dach")
        ersteTreffer(index, "brunnenwasser trinken", "wasser-brunnen-nach-hochwasser")
        // Und was dabei nicht verrutschen darf: Die Angst-Uebungen bleiben der
        // erste Treffer zu "angst" und "panik" -- der Kinder-Eintrag steht
        // dahinter, nicht davor.
        ersteTreffer(index, "angst", "medizin-angst-beruhigen")
        ersteTreffer(index, "panik", "medizin-angst-beruhigen")
    }


    /**
     * Drei Faelle derselben Sorte: Das getippte Wort ist LAENGER oder
     * ZUSAMMENGESETZT, und die Suche kennt nur Wortanfaenge.
     *
     * "ohrenschmerzen" fand den Eintrag "Ohrenschmerz" nicht -- die Mehrzahl
     * findet die Einzahl nicht, nur umgekehrt. Erster Treffer waren die
     * HALSSCHMERZEN. "blutdruckmittel" trifft "bluthochdruck" nicht, weil das
     * zusammengesetzte Wort mit anderen Buchstaben weitergeht.
     *
     * Das ist keine Ausnahme, sondern eine Eigenschaft der Wortanfangs-Suche.
     * Wo sie zuschlaegt, hilft nur das Wort selbst als Schlagwort.
     */
    @Test
    fun laengereUndZusammengesetzteWoerterFindenIhrenEintrag() {
        val index = index()
        ersteTreffer(index, "ohrenschmerzen", "medizin-ohrenschmerz")
        ersteTreffer(index, "ohr tut weh", "medizin-ohrenschmerz")
        ersteTreffer(index, "blutdruckmittel alle", "medizin-bluthochdruck-ohne-mittel")
        ersteTreffer(index, "blutdrucktabletten alle", "medizin-bluthochdruck-ohne-mittel")
        ersteTreffer(index, "zucker zu hoch", "medizin-ueberzuckerung")
        ersteTreffer(index, "blutzucker hoch", "medizin-ueberzuckerung")
        // Und die Gegenprobe, die hier besonders zaehlt: Ueberzuckerung und
        // Unterzuckerung sind entgegengesetzte Notfaelle und duerfen sich
        // nicht gegenseitig verdraengen.
        ersteTreffer(index, "überzuckerung", "medizin-ueberzuckerung")
        ersteTreffer(index, "unterzuckerung", "erste-hilfe-unterzuckerung")
    }


    /**
     * Wo man die Nacht verbringt, wenn einen der Abend unterwegs erwischt.
     *
     * Gemessen am 19.08.2026: "wo uebernachten" fand NICHTS, "unterschlupf"
     * fuehrte auf die TOLLWUT, "wo schlafen" auf Obstbaeume und aufs Gerben
     * ohne Rinde. Die Unterstand-Anleitungen gibt es -- aber Anleitungen haben
     * keine Schlagwoerter, also war ueber diese Woerter nichts zu erreichen.
     *
     * Das Marschtempo dagegen stand laengst da und war nur unter "gehtempo"
     * und "wie oft pause" nicht auffindbar. Kein zweiter Eintrag, sondern die
     * fehlenden Woerter.
     */
    @Test
    fun derSchlafplatzUndDasTempoSindAuffindbar() {
        val index = index()
        ersteTreffer(index, "wo übernachten", "unterwegs-nachtlager-hoehe")
        ersteTreffer(index, "wo schlafen", "unterwegs-nachtlager-hoehe")
        ersteTreffer(index, "nachtlager", "unterwegs-nachtlager-hoehe")
        ersteTreffer(index, "talgrund", "unterwegs-nachtlager-hoehe")
        ersteTreffer(index, "kaltluft", "unterwegs-nachtlager-hoehe")
        ersteTreffer(index, "unterschlupf", "unterwegs-nachtlager-hoehe")
        ersteTreffer(index, "gehtempo", "orientierung-marschtempo")
        ersteTreffer(index, "wie oft pause", "orientierung-marschtempo")
        // Und was der neue Eintrag NICHT wegnehmen darf: Die Wetterzeichen
        // behalten "senke" und "lagerplatz", denn dort steht das Warum.
        findet(index, "senke", "taktisch-himmelszeichen")
        findet(index, "lagerplatz", "taktisch-himmelszeichen")
    }


    /**
     * Drei aus dem Rundumschlag vom 19.08.2026, der sonst sauber war:
     *
     *   "chlor dosierung"    fuehrte auf KALIUMPERMANGANAT. Wie viel Chlor ins
     *                        Wasser gehoert, ist die gefaehrlichste Frage des
     *                        ganzen Kapitels -- und sie fand ihren Eintrag
     *                        nicht, weil dort nur "chlor" ohne "dosierung"
     *                        stand.
     *   "splitter im körper" fuehrte auf VITAMINMANGEL.
     *   "eingeschneit"       fand NICHTS.
     */
    @Test
    fun derRundumschlagVomNeunzehnten() {
        val index = index()
        ersteTreffer(index, "chlor dosierung", "wasser-chlor-entkeimung")
        ersteTreffer(index, "wie viel chlor", "wasser-chlor-entkeimung")
        ersteTreffer(index, "chlortabletten", "wasser-chlor-entkeimung")
        ersteTreffer(index, "splitter im körper", "erste-hilfe-fremdkoerper-in-wunde")
        ersteTreffer(index, "steckt in der wunde", "erste-hilfe-fremdkoerper-in-wunde")
        ersteTreffer(index, "eingeschneit", "taktisch-eis-und-schnee-gehen")
        ersteTreffer(index, "schneesturm", "taktisch-eis-und-schnee-gehen")
        // Gegenprobe: Der Nachweis, dass das Entkeimen gewirkt hat, behaelt
        // seine eigene Frage -- Dosieren und Messen sind zwei Schritte.
        ersteTreffer(index, "freies chlor", "wasser-freies-chlor-messen")
        ersteTreffer(index, "chlor messen", "wasser-freies-chlor-messen")
    }


    /**
     * Wie eine Gruppe zusammen unterwegs ist -- gemessen am 19.08.2026 fanden
     * "gruppe zusammenhalten", "vorausgehen", "kundschafter" und
     * "marschordnung" NICHTS, und "wer geht vorne" fuehrte auf Huehner und
     * aufs Schafschlachten.
     *
     * Es gab drei benachbarte Eintraege -- Gruppe einteilen (Wache, Aufgaben,
     * Buch), Wenn einer weggeht (fuenf Fragen) und Marschtempo (Geschwindigkeit
     * und Pausen) -- aber keinen zum Marschieren selbst.
     */
    @Test
    fun dieGruppeUnterwegsFindetIhrenEintrag() {
        val index = index()
        ersteTreffer(index, "gruppe zusammenhalten", "orientierung-gruppe-marschieren")
        ersteTreffer(index, "wer geht vorne", "orientierung-gruppe-marschieren")
        ersteTreffer(index, "kundschafter", "orientierung-gruppe-marschieren")
        ersteTreffer(index, "marschordnung", "orientierung-gruppe-marschieren")
        ersteTreffer(index, "sammelpunkt", "orientierung-gruppe-marschieren")
        ersteTreffer(index, "anschluss verloren", "orientierung-gruppe-marschieren")
        // Und die drei Nachbarn behalten ihre Fragen.
        ersteTreffer(index, "gruppe einteilen", "taktisch-gruppe-einteilen")
        ersteTreffer(index, "wache", "taktisch-gruppe-einteilen")
        ersteTreffer(index, "treffpunkt", "taktisch-weggehen-absprache")
        // Seit es einen eigenen Vermissten-Eintrag gibt, fuehrt "jemand
        // fehlt" dorthin -- der Absprache-Eintrag behaelt "treffpunkt".
        ersteTreffer(index, "jemand fehlt", "taktisch-vermisst-wegpunkte")
        ersteTreffer(index, "gehtempo", "orientierung-marschtempo")
    }


    /**
     * Eine ANLEITUNG hat keine Schlagwoerter -- ihr einziges Stellrad ist die
     * Kurzfassung. Und die wirkt nur mit der genauen Wortform.
     *
     * "fluss überqueren" fuehrte am 19.08.2026 auf die HOEHENLINIEN und
     * "fluss durchwaten" auf NICHTS, obwohl die Anleitung "Einen Fluss
     * durchqueren" heisst: Das Handbuch sagt "durchqueren", gesucht wird
     * "ueberqueren" und "durchwaten".
     *
     * Beim ersten Versuch stand in der Kurzfassung "wo man ueberquert" -- und
     * es aenderte GAR NICHTS, weil "ueberquert" nicht mit "ueberqueren"
     * anfaengt. Dieselbe Falle wie bei "kind reanimieren". Erst die
     * Grundform im Text hat gewirkt.
     */
    @Test
    fun dieAnleitungZumFlussFindetIhreWoerter() {
        val index = index()
        ersteTreffer(index, "fluss überqueren", "gewaesser-durchqueren")
        ersteTreffer(index, "fluss durchwaten", "gewaesser-durchqueren")
        ersteTreffer(index, "durchwaten", "gewaesser-durchqueren")
        ersteTreffer(index, "fluss durchqueren", "gewaesser-durchqueren")
        ersteTreffer(index, "wo fluss queren", "gewaesser-durchqueren")
        // Das Floss behaelt seine eigene Frage.
        ersteTreffer(index, "floß bauen", "gelaende-floss-bauen")
    }


    /**
     * Einem Wasserlauf folgen -- der klassische Weg zurueck zu Menschen, und
     * am 19.08.2026 war er nicht auffindbar: "fluss folgen" fuehrte auf
     * "Nachts gehen", "bach folgen" auf HUEHNER, "zivilisation finden" auf
     * NICHTS.
     *
     * Der Titel traegt bewusst "Bach" und nicht "Fluss": Das Wort Fluss gehoert
     * der Anleitung zum Durchqueren, und ein Titelwort haette sie verdraengt.
     * Die Fluss-Woerter stehen deshalb in den Schlagwoertern.
     */
    @Test
    fun demWasserlaufFolgenIstAuffindbar() {
        val index = index()
        ersteTreffer(index, "fluss folgen", "orientierung-wasserlauf-folgen")
        ersteTreffer(index, "bach folgen", "orientierung-wasserlauf-folgen")
        ersteTreffer(index, "flussabwärts", "orientierung-wasserlauf-folgen")
        ersteTreffer(index, "zivilisation finden", "orientierung-wasserlauf-folgen")
        ersteTreffer(index, "wo sind menschen", "orientierung-wasserlauf-folgen")
        // Und das Queren bleibt beim Queren -- das ist die Anleitung, an der
        // es um Leben geht.
        ersteTreffer(index, "fluss", "gewaesser-durchqueren")
        ersteTreffer(index, "fluss überqueren", "gewaesser-durchqueren")
        ersteTreffer(index, "fluss durchqueren", "gewaesser-durchqueren")
        ersteTreffer(index, "richtung halten", "orientierung-zielmarken")
    }


    /**
     * Den Weg ansehen, bevor man ihn geht. Gemessen am 19.08.2026 fuehrte
     * "route planen" auf den GARTENPLAN, "aussichtspunkt" und
     * "vorher anschauen" fanden NICHTS, und "totes gelände" fuehrte auf
     * "Totes Gewebe erkennen" -- zwei voellig verschiedene Sachen, die sich
     * nur ein Wort teilen.
     */
    @Test
    fun denWegVorherAnsehenIstAuffindbar() {
        val index = index()
        ersteTreffer(index, "route planen", "orientierung-weg-vorher-ansehen")
        ersteTreffer(index, "vorher anschauen", "orientierung-weg-vorher-ansehen")
        ersteTreffer(index, "aussichtspunkt", "orientierung-weg-vorher-ansehen")
        ersteTreffer(index, "totes gelände", "orientierung-weg-vorher-ansehen")
        ersteTreffer(index, "auf baum steigen", "orientierung-weg-vorher-ansehen")
        ersteTreffer(index, "hang einschätzen", "orientierung-weg-vorher-ansehen")
        // Die Nachbarn behalten ihre Fragen: das Umgehen, das Richtunghalten
        // und -- wichtiger als es klingt -- das tote GEWEBE.
        ersteTreffer(index, "hindernis", "orientierung-hindernis-umgehen")
        ersteTreffer(index, "umweg", "orientierung-hindernis-umgehen")
        ersteTreffer(index, "richtung halten", "orientierung-zielmarken")
        ersteTreffer(index, "totes gewebe", "erste-hilfe-totes-gewebe-erkennen")
    }


    /**
     * "schlimmster kopfschmerz" fand am 19.08.2026 NICHTS -- dabei steht genau
     * dieser Satz im Eintrag: Der ERSTE oder der SCHLIMMSTE Kopfschmerz des
     * Lebens gehoert sofort in aerztliche Hand. Es ist das bekannteste
     * Warnzeichen ueberhaupt, und es war nicht auffindbar.
     */
    @Test
    fun derSchlimmsteKopfschmerzDesLebensIstAuffindbar() {
        val index = index()
        ersteTreffer(index, "schlimmster kopfschmerz", "medizin-kopfschmerz")
        ersteTreffer(index, "stärkster kopfschmerz", "medizin-kopfschmerz")
        ersteTreffer(index, "kopfschmerz", "medizin-kopfschmerz")
        // Und der steife Nacken bleibt bei der Hirnhautentzuendung.
        ersteTreffer(index, "steifer nacken", "medizin-hirnhautentzuendung")
    }


    /**
     * DIE WARNZEICHEN SELBST -- also genau die Woerter, mit denen ein Laie
     * beschreibt, was er sieht. Gemessen am 19.08.2026, und es war der
     * schlechteste Befund des ganzen Durchgangs:
     *
     *   "druck auf der brust"    fuehrte auf das GEDRAENGE und auf die
     *                            Hirnhautentzuendung. Das ist die
     *                            Standardbeschreibung eines Herzinfarkts.
     *   "schmerz in den arm"     fuehrte auf das Kompartmentsyndrom und aufs
     *                            NAGELBETT -- der ausstrahlende Schmerz ist das
     *                            zweite Leitzeichen desselben Infarkts.
     *   "verwaschene sprache"    fand NICHTS. Beim Schlaganfall zaehlt jede
     *                            Minute, und das ist eines der drei Zeichen,
     *                            nach denen ueberhaupt gefragt wird.
     *   "erbricht blut"          fuehrte auf den Schock und auf TYPHUS.
     *   "zuckt am ganzen körper" fuehrte aufs NAGELBETT.
     *   "starke bauchschmerzen"  fuehrte auf Fieber und Nebennierenschwaeche.
     *
     * In jedem Fall gab es den richtigen Eintrag. Er kannte nur das Wort nicht,
     * mit dem ein Mensch beschreibt, was er vor sich hat.
     */
    @Test
    fun dieWarnzeichenFuehrenZumNotfallEintrag() {
        val index = index()
        ersteTreffer(index, "druck auf der brust", "erste-hilfe-brustschmerzen")
        ersteTreffer(index, "schmerz in den arm", "erste-hilfe-brustschmerzen")
        ersteTreffer(index, "brustschmerz", "erste-hilfe-brustschmerzen")
        ersteTreffer(index, "verwaschene sprache", "erste-hilfe-schlaganfall")
        ersteTreffer(index, "gesicht hängt", "erste-hilfe-schlaganfall")
        ersteTreffer(index, "erbricht blut", "medizin-magengeschwuer")
        ersteTreffer(index, "schwarzer stuhl", "medizin-magengeschwuer")
        ersteTreffer(index, "zuckt am ganzen körper", "erste-hilfe-krampfanfall")
        ersteTreffer(index, "starke bauchschmerzen", "medizin-bauchfellentzuendung")
        ersteTreffer(index, "bauch bretthart", "medizin-bauchfellentzuendung")
        // Und die Eintraege, die dabei nichts abgeben duerfen.
        ersteTreffer(index, "gedränge", "taktisch-gedraenge")
        ersteTreffer(index, "kompartmentsyndrom", "erste-hilfe-kompartmentsyndrom")
    }


    /**
     * Zweiter Durchgang durch die Beschreibungen, am 19.08.2026:
     *
     *   "haut ist gelb"            fuehrte aufs SCHLACHTTIER und auf LAEUSE.
     *   "urin ist dunkel"          fuehrte auf den KOMPOST.
     *   "dickes bein"              fuehrte aufs GERBEN OHNE RINDE.
     *   "zittert stark"            fuehrte auf die Eis-Selbstrettung statt auf
     *                              die Unterkuehlung, bei der das Zittern das
     *                              namengebende Zeichen ist.
     *   "kein puls"                fuehrte aufs Kompartmentsyndrom.
     *   "fühlt sich sterbenskrank" fand NICHTS -- dabei ist genau das der Satz,
     *                              mit dem eine Sepsis sich ankuendigt.
     *
     * Bei "kein puls" entscheidet ein Gleichstand ueber die alphabetische
     * Reihenfolge der Titel. Das ist hier ABSICHTLICH festgeschrieben: Wenn ein
     * neuer Eintrag diese Anfrage uebernimmt, soll es auffallen.
     */
    @Test
    fun zweiterDurchgangDurchDieBeschreibungen() {
        val index = index()
        ersteTreffer(index, "haut ist gelb", "medizin-gelbsucht-wasser")
        ersteTreffer(index, "urin ist dunkel", "medizin-gelbsucht-wasser")
        ersteTreffer(index, "dickes bein", "medizin-thrombose")
        ersteTreffer(index, "geschwollenes bein", "medizin-thrombose")
        ersteTreffer(index, "zittert stark", "erste-hilfe-unterkuehlung-stadium-eins")
        ersteTreffer(index, "kein puls", "erste-hilfe-entscheidung-nach-atemkontrolle")
        ersteTreffer(index, "herz steht", "erste-hilfe-entscheidung-nach-atemkontrolle")
        ersteTreffer(index, "fühlt sich sterbenskrank", "medizin-sepsis")
        // Und was seine Frage behaelt.
        ersteTreffer(index, "gelbsucht", "medizin-gelbsucht-wasser")
        ersteTreffer(index, "thrombose", "medizin-thrombose")
        ersteTreffer(index, "sepsis", "medizin-sepsis")
        ersteTreffer(index, "eingebrochen im eis", "erste-hilfe-eiseinbruch-retten")
    }


    /**
     * Dritter Durchgang, 19.08.2026 -- und der erste Fund darin ist der
     * unangenehmste des ganzen Abends:
     *
     *   "roter streifen am arm"  fuehrte auf BEEREN. Der rote Streifen, der von
     *                            einer Wunde zum Rumpf zieht, ist das Zeichen,
     *                            dass die Entzuendung sich ausbreitet -- das
     *                            Wort stand als "roter streifen" im
     *                            Sepsis-Eintrag, aber mit "am arm" dahinter
     *                            fand die Suche nichts Gemeinsames mehr.
     *   "wunde eitert"           fuehrte auf die TIERGESUNDHEIT.
     *   "lippen schwellen an"    fuehrte auf Wiederernaehrung und
     *                            Schwangerschaftsvergiftung -- dabei ist das
     *                            das Zeichen, das dem Ersticken vorausgeht.
     */
    @Test
    fun dritterDurchgangDurchDieBeschreibungen() {
        val index = index()
        ersteTreffer(index, "roter streifen am arm", "medizin-sepsis")
        ersteTreffer(index, "roter streifen", "medizin-sepsis")
        ersteTreffer(index, "wunde eitert", "medizin-wundrose")
        ersteTreffer(index, "wunde entzündet", "medizin-wundrose")
        ersteTreffer(index, "lippen schwellen an", "erste-hilfe-allergischer-schock")
        ersteTreffer(index, "zunge schwillt", "erste-hilfe-allergischer-schock")
        // Und die Gegenprobe -- die Beeren behalten ihre Beeren.
        ersteTreffer(index, "beeren", "agrikultur-beeren")
        ersteTreffer(index, "abszess", "medizin-abszess-eroeffnen")
        ersteTreffer(index, "wundrose", "medizin-wundrose")
    }


    /**
     * "kann kaum atmen" fuehrte am 19.08.2026 auf die UEBERZUCKERUNG. Es gibt
     * keinen allgemeinen Eintrag "Atemnot einordnen" -- die vorhandenen sind
     * nach URSACHE benannt (Spannung im Brustkorb, Asthma, Lungenentzuendung,
     * allergischer Schock). Die Anfrage geht deshalb auf "Ersticken erkennen":
     * Das ist die eine Ursache, die ein Umstehender in Sekunden beheben kann,
     * und der Eintrag trennt sie ausdruecklich von den anderen.
     *
     * DAS BLEIBT EIN NOTBEHELF. Ein eigener Einordnungs-Eintrag zur Atemnot
     * waere besser -- er fehlt, und das ist notiert, nicht geraten.
     */
    @Test
    fun ploetzlicheAtemnotLandetNichtBeiDerUeberzuckerung() {
        val index = index()
        ersteTreffer(index, "kann kaum atmen", "erste-hilfe-ersticken-erkennen")
        ersteTreffer(index, "bekommt keine luft", "erste-hilfe-ersticken-erkennen")
    }


    /**
     * WIE EIN ELTERNTEIL SUCHT. Gemessen am 19.08.2026 -- und das ist die
     * wahrscheinlichste Benutzerin dieses Handbuchs ueberhaupt:
     *
     *   "kind wacht nicht auf"       fuehrte auf die GUERTELROSE.
     *   "kind ist ganz schlaff"      fuehrte auf BIENEN.
     *   "kind ist heiß"              fuehrte auf einen Fremdkoerper im OHR.
     *   "kind hat sich verbrannt"    fuehrte auf BIENEN.
     *   "kind gefallen"              fuehrte aufs GEDRAENGE.
     *   "kind hat kopf angeschlagen" fuehrte auf den SONNENSTICH.
     *
     * Die Eintraege waren alle da. Sie sind nach dem Befund benannt --
     * Bewusstsein pruefen, Fieber, Verbrennung, Gehirnerschuetterung -- und
     * kannten die Woerter nicht, mit denen jemand ein Kind beschreibt.
     */
    @Test
    fun einElternteilFindetDenPassendenEintrag() {
        val index = index()
        ersteTreffer(index, "kind wacht nicht auf", "erste-hilfe-reaktion-pruefen")
        ersteTreffer(index, "kind ist ganz schlaff", "erste-hilfe-fieber-gefahr")
        ersteTreffer(index, "kind ist heiß", "erste-hilfe-fieber-gefahr")
        ersteTreffer(index, "kind schreit dauernd", "erste-hilfe-fieber-gefahr")
        ersteTreffer(index, "kind hat sich verbrannt", "erste-hilfe-verbrennung-kuehlen")
        ersteTreffer(index, "kind gefallen", "erste-hilfe-gehirnerschuetterung")
        ersteTreffer(index, "kind hat kopf angeschlagen", "erste-hilfe-gehirnerschuetterung")
        // Und was dabei nicht verrutschen darf.
        ersteTreffer(index, "kind hat fieber", "erste-hilfe-fieber-gefahr")
        ersteTreffer(index, "gehirnerschütterung", "erste-hilfe-gehirnerschuetterung")
        ersteTreffer(index, "bewusstsein prüfen", "erste-hilfe-reaktion-pruefen")
        ersteTreffer(index, "kind atmet nicht", "erste-hilfe-wiederbelebung-kind")
    }


    /**
     * Wer jemanden pflegt, der liegt. Gemessen am 19.08.2026:
     * "wund gelegen" fuehrte auf die TIERGESUNDHEIT, "druckstellen" auf den
     * AXTSTIEL -- der Eintrag fuehrte "druckstelle" in der Einzahl, und die
     * Mehrzahl findet die Einzahl nicht.
     */
    @Test
    fun wundgelegenFindetSeinenEintrag() {
        val index = index()
        ersteTreffer(index, "wund gelegen", "medizin-wundliegen")
        ersteTreffer(index, "druckstellen", "medizin-wundliegen")
        ersteTreffer(index, "liegt nur noch", "medizin-wundliegen")
        ersteTreffer(index, "dekubitus", "medizin-wundliegen")
    }


    /**
     * Vierter Durchgang, 19.08.2026:
     *
     *   "husten geht nicht weg"  fuehrte auf den RIPPENBRUCH -- obwohl der
     *                            Tuberkulose-Eintrag woertlich "Husten, der
     *                            nicht aufhoert" heisst. Ein anderes Wort fuer
     *                            dieselbe Sache, und schon war er weg.
     *   "stuhl ist weiß"         fuehrte auf den MADENWURM.
     *   "alter mensch gestürzt"  fuehrte aufs GEDRAENGE.
     *   "hat sich übergeben"     fuehrte auf den Hitzschlag.
     *   "wasser wird knapp"      fuehrte auf den Brunnen und auf
     *                            Epilepsie-Mittel.
     */
    @Test
    fun vierterDurchgangDurchDieBeschreibungen() {
        val index = index()
        ersteTreffer(index, "husten geht nicht weg", "medizin-tuberkulose")
        ersteTreffer(index, "husten seit wochen", "medizin-tuberkulose")
        ersteTreffer(index, "stuhl ist weiß", "medizin-gelbsucht-wasser")
        ersteTreffer(index, "alter mensch gestürzt", "erste-hilfe-beckenbruch")
        ersteTreffer(index, "hüfte gebrochen", "erste-hilfe-beckenbruch")
        ersteTreffer(index, "hat sich übergeben", "erste-hilfe-erbrechen-helfen")
        ersteTreffer(index, "wasser wird knapp", "wasser-vorratsdauer")
        // Gegenprobe: Madenwurm und Gedraenge behalten ihre Fragen.
        ersteTreffer(index, "madenwurm", "medizin-madenwurm")
        ersteTreffer(index, "gedränge", "taktisch-gedraenge")
        ersteTreffer(index, "erbrechen", "erste-hilfe-erbrechen-helfen")
    }


    /**
     * Stillen. Gemessen am 19.08.2026 fuehrte "zu wenig milch" auf einen
     * AUSGESCHLAGENEN ZAHN und "stillen tut weh" aufs Abbinden.
     *
     * Und eine Eigenheit des deutschen Wortes, die bleibt: "stillen" heisst
     * beides -- ein Kind stillen und eine Blutung stillen. Beide Eintraege
     * tragen es im Titel und liegen deshalb gleichauf; entschieden wird
     * alphabetisch. Das ist hier NICHT festgeschrieben, weil keine der beiden
     * Reihenfolgen die richtigere ist: Wer das Wort allein tippt, bekommt
     * beide Eintraege zu sehen, und das ist die ehrlichste Antwort.
     */
    @Test
    fun dasStillenFindetSeineEintraege() {
        val index = index()
        ersteTreffer(index, "zu wenig milch", "medizin-stillen-genug")
        ersteTreffer(index, "stillen tut weh", "medizin-stillen-genug")
        ersteTreffer(index, "muttermilch", "medizin-stillen-genug")
        ersteTreffer(index, "blutung stillen", "erste-hilfe-starke-blutung")
    }


    /**
     * Brustentzuendung beim Stillen. "mastitis" fand am 19.08.2026 NICHTS und
     * "brustentzündung" fuehrte auf "Antibiotika: wann keines gegeben wird".
     *
     * DER TITEL TRAEGT ABSICHTLICH WEDER "BRUST" NOCH "ENTZUENDUNG" NOCH
     * "STILLEN" ALS EIGENES WORT: "brust" gehoert den Brustschmerzen und der
     * offenen Brustwunde, "entzündung" der Sepsis, "stillen" teilen sich die
     * Blutstillung und "Stillen: bekommt das Kind genug?". Ein Titelwort wiegt
     * 5 Punkte und haette jedem davon den ersten Platz genommen. "Milchstau"
     * ist der Fachbegriff, den niemand sonst benutzt -- und die Laienwoerter
     * stehen in den Schlagwoertern.
     */
    @Test
    fun dieBrustentzuendungIstAuffindbarUndNimmtNiemandemEtwasWeg() {
        val index = index()
        ersteTreffer(index, "mastitis", "medizin-brustentzuendung-stillen")
        ersteTreffer(index, "brustentzündung", "medizin-brustentzuendung-stillen")
        ersteTreffer(index, "milchstau", "medizin-brustentzuendung-stillen")
        ersteTreffer(index, "brust ist rot", "medizin-brustentzuendung-stillen")
        ersteTreffer(index, "abstillen", "medizin-brustentzuendung-stillen")
        // Die Gegenprobe, und sie ist der eigentliche Grund fuer diesen Test.
        ersteTreffer(index, "druck auf der brust", "erste-hilfe-brustschmerzen")
        ersteTreffer(index, "brustschmerz", "erste-hilfe-brustschmerzen")
        ersteTreffer(index, "entzündung", "medizin-sepsis")
        ersteTreffer(index, "abszess", "medizin-abszess-eroeffnen")
        ersteTreffer(index, "blutung stillen", "erste-hilfe-starke-blutung")
    }


    /**
     * Fuenfter Durchgang, 19.08.2026 -- Hygiene und das, worueber niemand
     * gerne nachliest:
     *
     *   "leiche im haus"       fuehrte auf Brandrauch und Vorratshaltung.
     *   "haare voller läuse"   fuehrte auf ZIEGENMILCH.
     *   "müll wohin"           fuehrte auf den bewussten Versatz beim Gehen.
     */
    @Test
    fun fuenfterDurchgangHygieneUndTote() {
        val index = index()
        ersteTreffer(index, "leiche im haus", "medizin-tote-bergen-was-kommt")
        ersteTreffer(index, "toter im haus", "medizin-tote-bergen-was-kommt")
        ersteTreffer(index, "haare voller läuse", "medizin-laeuse")
        ersteTreffer(index, "müll wohin", "hygiene-abfall-lager")
        ersteTreffer(index, "fliegen überall", "hygiene-abfall-lager")
        ersteTreffer(index, "läuse", "medizin-laeuse")
        ersteTreffer(index, "abfall", "hygiene-abfall-lager")
    }


    /**
     * Zwei Fragen, die keine Diagnose sind: "wem kann ich glauben" fuehrte am
     * 19.08.2026 auf Alkoholentzug und Wasserfilter, "keine kraft mehr" aufs
     * Gedraenge und auf versagende Bremsen.
     *
     * "halte das nicht aus" bleibt unerreichbar und ist NICHT festgeschrieben:
     * Der Satz besteht fast nur aus Allerweltswoertern, und die stehen in
     * hunderten Eintraegen. Ohne eine Gewichtung nach Seltenheit ist das nicht
     * zu loesen -- siehe MERKZETTEL.
     */
    @Test
    fun fragenDieKeineDiagnoseSind() {
        val index = index()
        ersteTreffer(index, "wem kann ich glauben", "taktisch-falschmeldungen")
        ersteTreffer(index, "gerücht", "taktisch-falschmeldungen")
        ersteTreffer(index, "keine kraft mehr", "medizin-kopf-in-der-krise")
        ersteTreffer(index, "panik", "medizin-angst-beruhigen")
    }


    /**
     * Gefunden werden. Gemessen am 19.08.2026 fuehrte
     * "auf sich aufmerksam machen" auf KARTOFFELN, "wie finden die mich" auf
     * Huelsenfruechte, und "rauchzeichen" fand NICHTS -- obwohl der Eintrag
     * "Notsignale: alles in Dreiergruppen" ausdruecklich Signalrauch nennt.
     */
    @Test
    fun werGefundenWerdenWillFindetDieNotsignale() {
        val index = index()
        ersteTreffer(index, "auf sich aufmerksam machen", "orientierung-signale-dreiergruppen")
        ersteTreffer(index, "wie finden die mich", "orientierung-signale-dreiergruppen")
        ersteTreffer(index, "rauchzeichen", "orientierung-signale-dreiergruppen")
        ersteTreffer(index, "hilfe rufen", "orientierung-signale-dreiergruppen")
        ersteTreffer(index, "suchtrupp", "orientierung-signale-dreiergruppen")
        // Die Nachbarn behalten ihre Fragen.
        ersteTreffer(index, "spiegel blinken", "orientierung-signale-spiegel")
        ersteTreffer(index, "bodenzeichen", "orientierung-bodenzeichen")
        ersteTreffer(index, "notruf", "erste-hilfe-notruf-112")
    }


    /**
     * Die Kernfrage beim Sammeln. Gemessen am 19.08.2026 fuehrte
     * "ist das essbar" auf HUELSENFRUECHTE und "kann man das essen"
     * zusaetzlich auf "Sterben begleiten" -- beides Eintraege, die die Woerter
     * beilaeufig enthalten. Der richtige ist "Acht Zeichen, bei denen du gar
     * nicht erst probierst".
     *
     * Dazu "vogel fangen" (fuehrte auf HUEHNER) und "riecht komisch", das
     * NICHTS fand, obwohl es die uebliche Beschreibung fuer verdorbenes
     * Fleisch ist.
     */
    @Test
    fun dieFrageObManEsEssenKann() {
        val index = index()
        ersteTreffer(index, "ist das essbar", "nahrung-pflanzen-meiden")
        ersteTreffer(index, "kann man das essen", "nahrung-pflanzen-meiden")
        ersteTreffer(index, "giftig oder nicht", "nahrung-pflanzen-meiden")
        ersteTreffer(index, "vogel fangen", "nahrung-voegel-fangen")
        ersteTreffer(index, "riecht komisch", "nahrung-fleisch-faeulnis-erkennen")
        // Und was seine eigene Frage behaelt.
        ersteTreffer(index, "beeren", "agrikultur-beeren")
    }


    /**
     * Anleitungen wieder -- und wieder ueber die Kurzfassung, weil sie kein
     * Schlagwortfeld haben. Gemessen am 19.08.2026:
     *
     *   "seil knüpfen"         fand NICHTS.
     *   "schnur selber machen" fand NICHTS -- und zwar, weil das Wort "selber"
     *                          im ganzen Handbuch nirgends vorkam. Es benutzt
     *                          durchgehend "selbst". Ein Wort, das nirgends
     *                          steht, laesst die Anfrage leer; das ist die
     *                          Regel gegen Tippfehler, und hier hat sie ein
     *                          voellig normales deutsches Wort getroffen.
     *   "axt stiel gebrochen"  fuehrte aufs Schaerfen.
     *   "dach abdichten"       fuehrte auf ZIEGEL BRENNEN.
     *
     * Was NICHT gemacht wurde: "knoten" allein fuehrt weiter auf den
     * medizinischen Knoten (Abszess, Brust). Um das zu drehen, muesste der
     * Anleitungstitel "Grundknoten" zu "Knoten" werden -- und ein Titelwort
     * wiegt 5 Punkte und haette den Abszess verdraengt. Beide Bedeutungen sind
     * echt; die Rope-Anleitung ist ueber "seil knüpfen" erreichbar.
     */
    @Test
    fun anleitungenUeberIhreKurzfassung() {
        val index = index()
        ersteTreffer(index, "seil knüpfen", "seilwerk-grundknoten")
        ersteTreffer(index, "schnur selber machen", "seilwerk-schnur-selbst")
        ersteTreffer(index, "axt stiel gebrochen", "werkzeug-axtstiel")
        ersteTreffer(index, "dach abdichten", "unterkunft-dach-decken")
        ersteTreffer(index, "dach decken", "unterkunft-dach-decken")
        ersteTreffer(index, "loch im dach", "unterkunft-dach-decken")
    }


    /**
     * Strahlung. Der Bereich war weitgehend sauber, zwei Stellen nicht:
     * "jod wann nehmen" fuehrte am 19.08.2026 auf die HALSSCHMERZEN -- und
     * beim Jod entscheidet der Zeitpunkt alles, der Eintrag heisst nicht
     * umsonst "nur auf Ansage". "kontaminiert" fand NICHTS, weil der Eintrag
     * nur "kontamination" fuehrte.
     */
    @Test
    fun beimJodEntscheidetDerZeitpunkt() {
        val index = index()
        ersteTreffer(index, "jod wann nehmen", "strahlung-jodtabletten")
        ersteTreffer(index, "jod einnehmen", "strahlung-jodtabletten")
        ersteTreffer(index, "jodtabletten", "strahlung-jodtabletten")
        ersteTreffer(index, "kontaminiert", "erste-hilfe-strahlung")
        // Und die Halsschmerzen behalten ihre eigene Frage.
        ersteTreffer(index, "halsschmerzen", "medizin-halsschmerzen")
    }


    /**
     * Kaelte und Hitze, gemessen am 19.08.2026:
     *
     *   "finger weiß und taub"  fuehrte auf BIENEN und auf einen Schlag aufs
     *                           Auge -- dabei ist genau das das Leitzeichen
     *                           einer Erfrierung.
     *   "zu heiß"               fuehrte aufs Mahlen und aufs Lagern ohne Strom.
     *   "kreislauf bei hitze"   fuehrte auf Huelsenfruechte und MADENWURM.
     */
    @Test
    fun kaelteUndHitzeFindenIhreEintraege() {
        val index = index()
        ersteTreffer(index, "finger weiß und taub", "erste-hilfe-erfrierungen-erkennen")
        ersteTreffer(index, "weiße finger", "erste-hilfe-erfrierungen-erkennen")
        ersteTreffer(index, "taube finger", "erste-hilfe-erfrierungen-erkennen")
        ersteTreffer(index, "zu heiß", "erste-hilfe-hitzschlag-erkennen")
        ersteTreffer(index, "kreislauf bei hitze", "erste-hilfe-hitzschlag-erkennen")
        ersteTreffer(index, "zu viel sonne", "erste-hilfe-hitzschlag-erkennen")
        // Und die Nachbarn behalten ihre Fragen.
        ersteTreffer(index, "sonnenstich", "erste-hilfe-sonnenstich")
        ersteTreffer(index, "hitze", "erste-hilfe-hitze-vorbeugen")
        ersteTreffer(index, "zehen gefroren", "erste-hilfe-erfrierungen-versorgen")
    }


    /**
     * Auch KAPITEL haben kein Schlagwortfeld -- bei ihnen sind Ueberschrift und
     * Text das Suchfeld. Und wieder entschied die Wortform:
     *
     *   "garten anlegen"  fuehrte auf den BRUNNEN, obwohl im Text
     *                     "nach Lust angelegt" steht -- "angelegt" faengt nicht
     *                     mit "anlegen" an.
     *   "gemüse einlegen" fuehrte auf den Hygiene-Eintrag, obwohl im Kapitel
     *                     "eingelegt" steht.
     *
     * Das ist derselbe Befund wie bei "ueberquert" gegen "ueberqueren" und bei
     * "selber" gegen "selbst". Wer will, dass ein Wort gefunden wird, schreibt
     * die Grundform.
     */
    @Test
    fun kapitelUeberIhrenText() {
        val index = index()
        ersteTreffer(index, "garten anlegen", "agrikultur-gartenplan")
        ersteTreffer(index, "garten planen", "agrikultur-gartenplan")
        ersteTreffer(index, "gemüse einlegen", "agrikultur-milchsaeuregaerung")
        ersteTreffer(index, "sauerkraut", "agrikultur-milchsaeuregaerung")
        // Und das Einkochen behaelt seine eigene Frage -- es ist das
        // Verfahren, bei dem Botulismus droht.
        ersteTreffer(index, "einkochen", "nahrung-einkochen-botulismus")
    }


    /**
     * Zwei Anfragen aus dem Bereich, in dem Minuten zaehlen. Gemessen am
     * 19.08.2026:
     *
     *   "wehen wie oft"         fuehrte auf "Vlies beurteilen" und aufs
     *                           Marschtempo -- dabei ist der Wehenabstand
     *                           genau die Groesse, an der sich entscheidet, ob
     *                           noch Zeit bleibt.
     *   "zahn tut höllisch weh" fand NICHTS. Der Grund war wieder ein Wort,
     *                           das im ganzen Handbuch nirgends stand:
     *                           "hoellisch". Dieselbe Falle wie bei "selber".
     */
    @Test
    fun wehenabstandUndZahnschmerz() {
        val index = index()
        ersteTreffer(index, "wehen wie oft", "erste-hilfe-geburt-erkennen")
        ersteTreffer(index, "wehenabstand", "erste-hilfe-geburt-erkennen")
        ersteTreffer(index, "wehen", "erste-hilfe-geburt-erkennen")
        ersteTreffer(index, "zahn tut höllisch weh", "medizin-zahnschmerz-einordnen")
        ersteTreffer(index, "zahnschmerz", "medizin-zahnschmerz-einordnen")
    }


    /**
     * DIE FUENFUNDZWANZIG VOM ANFANG DER NACHT, noch einmal durchgemessen.
     *
     * Am 19.08.2026 begann alles mit 25 nachgestellten Notfall-Anfragen, von
     * denen SIEBEN leer blieben. Am Ende der Nacht blieb keine mehr leer -- und
     * die letzten zwei, die noch falsch landeten, stehen hier:
     *
     *   "jemand ist bewusstlos" fuehrte auf den KOPFSCHMERZ.
     *   "mir ist sehr kalt"     fuehrte auf den Schock und auf eine
     *                           Behelfsklinge, nicht auf die Unterkuehlung.
     *
     * Dieser Test ist die Schlussprobe: Wenn eine dieser Anfragen wieder
     * danebengeht, ist etwas zurueckgefallen.
     */
    @Test
    fun diePruefungVomAnfangDerNacht() {
        val index = index()
        ersteTreffer(index, "jemand ist bewusstlos", "erste-hilfe-entscheidung-nach-atemkontrolle")
        ersteTreffer(index, "person atmet nicht mehr", "erste-hilfe-entscheidung-nach-atemkontrolle")
        ersteTreffer(index, "mein kind atmet nicht", "erste-hilfe-wiederbelebung-kind")
        ersteTreffer(index, "mir ist sehr kalt", "erste-hilfe-unterkuehlung-stadium-eins")
        ersteTreffer(index, "ich friere stark", "erste-hilfe-unterkuehlung-stadium-eins")
        ersteTreffer(index, "blutung stoppen", "erste-hilfe-starke-blutung")
        ersteTreffer(index, "hund hat mich gebissen", "medizin-tollwut")
        ersteTreffer(index, "schlange hat gebissen", "erste-hilfe-schlangenbiss")
        ersteTreffer(index, "wie finde ich norden", "orientierung-schattenstock")
        ersteTreffer(index, "feuer machen ohne feuerzeug", "feuer-ohne-zuendmittel")
        // Und keine der fuenfundzwanzig darf jemals wieder leer ausgehen.
        for (anfrage in listOf(
            "deckung suchen", "wo verstecken bei beschuss", "essen finden im wald",
            "wasser sauber machen", "knochen gebrochen was tun", "hilfe bei schock",
            "kein strom was tun", "wunde richtig reinigen", "was kann ich essen",
        )) {
            assertTrue(
                index.search(anfrage, limit = 5).isNotEmpty(),
                "\"$anfrage\" findet nichts -- das war der Befund, mit dem diese Nacht anfing",
            )
        }
    }


    /**
     * ALTE UND VOLKSTUEMLICHE WOERTER -- die, mit denen aeltere Menschen
     * suchen. Gemessen am 19.08.2026:
     *
     *   "kohlenmonoxyd"     fand NICHTS. Das ist die alte Schreibweise mit y,
     *                       und dieses Gas toetet lautlos. Der Eintrag hatte
     *                       die Schlagwortgrenze von 20 schon erreicht, also
     *                       steht die alte Schreibweise jetzt im TEXT.
     *   "gehirnschlag"      fand NICHTS -- "schlagfluss" stand schon da.
     *   "grippaler infekt"  fuehrte auf MALARIA.
     *   "magenverstimmung"  fand NICHTS.
     *
     * Was dabei GUT war und hier festgehalten wird, damit es so bleibt:
     * "schlagfluss", "herzkasper", "zuckerkrankheit" und "schwindsucht" fanden
     * ihre Eintraege bereits.
     */
    @Test
    fun alteUndVolkstuemlicheWoerter() {
        val index = index()
        ersteTreffer(index, "kohlenmonoxyd", "erste-hilfe-kohlenmonoxid-erkennen")
        ersteTreffer(index, "gehirnschlag", "erste-hilfe-schlaganfall")
        ersteTreffer(index, "schlagfluss", "erste-hilfe-schlaganfall")
        ersteTreffer(index, "herzkasper", "erste-hilfe-brustschmerzen")
        ersteTreffer(index, "grippaler infekt", "medizin-bronchitis")
        ersteTreffer(index, "magenverstimmung", "erste-hilfe-erbrechen-helfen")
        ersteTreffer(index, "schwindsucht", "medizin-tuberkulose")
        ersteTreffer(index, "kreislaufkollaps", "erste-hilfe-ohnmacht")
    }


    /**
     * OESTERREICHISCHE UND SCHWEIZERISCHE WOERTER. Gemessen am 19.08.2026 --
     * und zwei davon sind ernst:
     *
     *   "notarzt" fand NICHTS. Das Wort kam im ganzen Handbuch NULL Mal vor,
     *             obwohl es das gebraeuchlichste deutsche Wort fuer den Arzt
     *             ist, der zum Notfall kommt.
     *   "spital"  fand NICHTS. In Oesterreich und der Schweiz heisst das
     *             Krankenhaus so; das Handbuch sagt "Klinik" (34 Eintraege)
     *             und "Krankenhaus" (11).
     *   "gelse"   fand NICHTS -- das oesterreichische Wort fuer die Muecke.
     *
     * Das Paket heisst "europe-de" und richtet sich nicht nur an Deutschland.
     * Wer Eintraege schreibt, sollte die regionalen Woerter mitdenken.
     */
    @Test
    fun regionaleWoerterFindenIhreEintraege() {
        val index = index()
        ersteTreffer(index, "notarzt", "erste-hilfe-notruf-112")
        ersteTreffer(index, "spital", "erste-hilfe-notruf-112")
        ersteTreffer(index, "rettung rufen", "erste-hilfe-notruf-112")
        ersteTreffer(index, "krankenwagen", "erste-hilfe-notruf-112")
        ersteTreffer(index, "ins krankenhaus", "erste-hilfe-notruf-112")
        ersteTreffer(index, "gelse", "hygiene-abfall-lager")
        ersteTreffer(index, "erdäpfel", "agrikultur-kartoffel")
        // Und der Notruf bleibt, was er ist.
        ersteTreffer(index, "notruf", "erste-hilfe-notruf-112")
        ersteTreffer(index, "112", "erste-hilfe-notruf-112")
    }


    /**
     * AUS SICHT DES HELFERS statt des Betroffenen. Wer daneben steht, fragt
     * anders -- und am 19.08.2026 fuehrten seine Fragen woandershin:
     *
     *   "darf ich ihn bewegen"    fuehrte aufs TROCKNEN. Dabei entscheidet die
     *                             Antwort darueber, ob jemand gelaehmt bleibt.
     *   "wen zuerst versorgen"    verlor gegen "BEUTE versorgen" -- also gegen
     *                             das Zerlegen eines erlegten Tieres.
     *   "was sage ich am telefon" fuehrte auf den Gartenplan.
     */
    @Test
    fun dieFragenDesHelfers() {
        val index = index()
        ersteTreffer(index, "darf ich ihn bewegen", "erste-hilfe-verletzten-bewegen")
        ersteTreffer(index, "wie trage ich ihn", "erste-hilfe-verletzten-bewegen")
        ersteTreffer(index, "wen zuerst versorgen", "erste-hilfe-mehrere-verletzte")
        ersteTreffer(index, "mehrere verletzte", "erste-hilfe-mehrere-verletzte")
        ersteTreffer(index, "was sage ich am telefon", "erste-hilfe-notruf-112")
        // Und das Zerlegen der Beute behaelt seine eigene Frage.
        ersteTreffer(index, "beute versorgen", "beute-versorgen")
    }


    /**
     * Der Behelfskompass kannte drei Wege, die Nadel zu magnetisieren --
     * Magnet, Seide, Haar. Der vierte stand in derselben Quelle und fehlte:
     * mit STROM. Eine Batterie hat man im Ernstfall eher als ein Stueck Seide.
     *
     * WAS DABEI SCHIEFGING, und der Waechter hat es abgefangen: Ich hatte
     * "batterie kompass" als Schlagwort eingetragen. Das gab dem Kompass das
     * Wort "batterie" mit Schlagwortgewicht -- und verdraengte damit den
     * KOHLENMONOXID-MELDER vom ersten Platz. Eine leere Melderbatterie toetet;
     * ein Kompass ohne Batterie nicht. Der vierte Weg steht deshalb nur im
     * TEXT, und "batterie" gehoert weiter dem Melder.
     */
    @Test
    fun derBehelfskompassKenntAuchDenStrom() {
        val index = index()
        ersteTreffer(index, "nadel magnetisieren", "orientierung-behelfskompass")
        ersteTreffer(index, "kompass bauen", "orientierung-behelfskompass")
        findet(index, "nadel mit batterie", "orientierung-behelfskompass")
        // Und die Gegenprobe, um die es hier eigentlich geht.
        ersteTreffer(index, "batterie", "erste-hilfe-kohlenmonoxid-melder")
    }
}
