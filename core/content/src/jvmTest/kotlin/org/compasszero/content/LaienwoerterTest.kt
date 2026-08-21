package org.compasszero.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// Niemand tippt im Ernstfall das Fachwort. Er tippt "Schuerfwunde", nicht
// "Abschuerfung"; "Holzbock", nicht "Zecke"; "Brunnenwasser", nicht
// "Trinkwassergewinnung". Dieser Test haelt fest, dass die gebraeuchlichen
// Alltagswoerter zum richtigen Eintrag fuehren.
//
// ER IST AUS EINER MESSUNG ENTSTANDEN, NICHT AUS EINER VERMUTUNG. Am
// 12.08.2026 wurden 133 Alltagswoerter durch die Suche gefahren. Vierzehn
// davon fanden nichts oder das Falsche, obwohl der passende Eintrag im Paket
// stand. Zwei Beispiele, die zeigen, worum es geht:
//
//   "Schuerfwunde"   fuehrte auf "Offene Brustwunde: nicht zudecken".
//                    Wer sich das Knie aufschlaegt, bekam eine Anleitung fuer
//                    eine durchdringende Brustverletzung.
//   "Brunnenwasser"  fuehrte auf "Hasenpest beim Verarbeiten".
//                    Wer wissen will, ob das Wasser trinkbar ist, bekam eine
//                    Anleitung zum Zerlegen von Hasen.
//
// Beide Faelle sind behoben. Dieser Test sorgt dafuer, dass sie nicht
// zurueckkommen -- ein neuer Eintrag mit einem passenden Wort im Titel wuerde
// sie sonst still wieder verdraengen.
//
// WER HIER EINEN FEHLSCHLAG SIEHT, hat wahrscheinlich gerade einen Eintrag
// geschrieben, dessen Titel eines dieser Woerter enthaelt. Die Loesung ist
// fast nie, die Erwartung hier zu aendern, sondern das Wort aus dem TITEL zu
// nehmen (im Fliesstext darf es stehen, das wiegt viel weniger).
class LaienwoerterTest {

    private fun repoRoot(): File {
        val fromProperty = System.getProperty("compasszero.repoRoot")
        if (fromProperty != null) return File(fromProperty)
        var dir = File(".").absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("settings.gradle.kts nicht gefunden")
        }
        return dir
    }

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

    // Tiefe 3 statt 8: Bei einem Alltagswort reicht es nicht, dass der Eintrag
    // irgendwo unter den ersten acht auftaucht. Wer ihn im Ernstfall nicht auf
    // den ersten Blick sieht, sieht ihn nicht.
    private fun fuehrtAuf(index: SearchIndex, wort: String, erwartet: String) {
        val treffer = index.search(wort, limit = 3).map { it.id }
        assertTrue(
            erwartet in treffer,
            "\"$wort\" fuehrt nicht auf $erwartet. Gefunden: " +
                if (treffer.isEmpty()) "NICHTS" else treffer.joinToString(),
        )
    }

    @Test
    fun alltagswoerterFuerKrankheitenUndVerletzungenFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "fallsucht", "erste-hilfe-krampfanfall")
        fuehrtAuf(index, "holzbock", "erste-hilfe-zecke-entfernen")
        fuehrtAuf(index, "platzwunde", "erste-hilfe-wunde-bedecken")
        fuehrtAuf(index, "schürfwunde", "erste-hilfe-wunde-ausspuelen")
        fuehrtAuf(index, "aufgeschürft", "erste-hilfe-wunde-ausspuelen")
        fuehrtAuf(index, "furunkel", "medizin-abszess-eroeffnen")
        fuehrtAuf(index, "eiterbeule", "medizin-abszess-eroeffnen")
        fuehrtAuf(index, "wundbrand", "erste-hilfe-totes-gewebe-erkennen")
        fuehrtAuf(index, "bluterguss", "medizin-schmerzen-ohne-arzt")
        // Sonnenstich ist medizinisch NICHT dasselbe wie Hitzschlag. Das Wort
        // fuehrt trotzdem hierher, weil es vorher auf gar nichts fuehrte und
        // dieser Eintrag die gefaehrliche Form erkennen lehrt. Ein eigener
        // Eintrag zum Sonnenstich steht als Luecke im SNAPSHOT -- wenn er
        // kommt, gehoert diese Zeile auf ihn umgestellt.
        fuehrtAuf(index, "sonnenstich", "erste-hilfe-hitzschlag-erkennen")
    }

    @Test
    fun alltagswoerterFuerWasserVorratUndHaushaltFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "brunnenwasser", "wasser-stelle-beurteilen")
        fuehrtAuf(index, "bachwasser", "wasser-stelle-beurteilen")
        fuehrtAuf(index, "pfütze", "wasser-stelle-beurteilen")
        fuehrtAuf(index, "abgestanden", "wasser-geschmack-belueften")
        fuehrtAuf(index, "notvorrat", "taktisch-vorrat-haushalt")
        fuehrtAuf(index, "hamstern", "taktisch-vorrat-haushalt")
        fuehrtAuf(index, "madig", "nahrung-fleisch-faeulnis-erkennen")
        fuehrtAuf(index, "gestank", "hygiene-abfall-lager")
        fuehrtAuf(index, "beeren sammeln", "nahrung-pflanzen-meiden")
        fuehrtAuf(index, "wildkräuter", "nahrung-pflanzen-meiden")
        // Anleitungen haben kein Schlagwortfeld. Dieses Wort steht deshalb in
        // der Werkzeugliste beider Anleitungen -- wer sie umschreibt, faellt
        // hier auf.
        fuehrtAuf(index, "wetzstein", "werkzeug-klinge-schaerfen")
    }

    @Test
    fun alltagswoerterFuerLageUndBetroffeneFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "einbrecher", "taktisch-anzeichen-gewalt")
        fuehrtAuf(index, "plünderer", "taktisch-anzeichen-gewalt")
        fuehrtAuf(index, "überfall", "taktisch-anzeichen-gewalt")
        fuehrtAuf(index, "alte menschen", "taktisch-vorsorge-hilfsmittel")
        fuehrtAuf(index, "behindert", "taktisch-vorsorge-hilfsmittel")
        fuehrtAuf(index, "pflegebedürftig", "taktisch-vorsorge-hilfsmittel")
        fuehrtAuf(index, "kein empfang", "taktisch-ausfall-folgen")
        fuehrtAuf(index, "kein netz", "taktisch-ausfall-folgen")
        // "hund" MUSS zur Tollwut fuehren, nicht zu einem Marschkniff. Am
        // 12.08.2026 tat es das nicht: Das Schlagwort "hundertmeter" in der
        // Entfernungsschaetzung fing die Anfrage ab, weil die Suche
        // Wortanfaenge vergleicht. Das Schlagwort ist gestrichen; diese Zeile
        // sorgt dafuer, dass es nicht zurueckkommt.
        fuehrtAuf(index, "hund", "medizin-tollwut")
    }

    // Die wichtigste der vier Pruefungen. Hier stehen keine Fachwoerter und
    // keine Volksnamen, sondern SAETZE -- das, was jemand tippt, waehrend
    // daneben ein Mensch am Boden liegt. Gemessen am 12.08.2026: Von 69
    // solchen Formulierungen fuehrten sechzehn ins Leere oder ins Falsche.
    // Die schlimmsten drei:
    //
    //   "bekommt keine Luft"  -> Schafe halten, Seife sieden, Bienen
    //   "blut spritzt"        -> Huehner schlachten
    //   "haengender mundwinkel" -> NICHTS
    //
    // Das sind keine Schoenheitsfehler. Wer in dieser Lage die Suche benutzt,
    // hat keine zweite Anfrage frei.
    @Test
    fun ganzeSaetzeAusDemErnstfallFuehrenZumRichtigenEintrag() {
        val index = index()
        // Atemnot und Ersticken
        fuehrtAuf(index, "keine luft", "erste-hilfe-ersticken-erkennen")
        fuehrtAuf(index, "bekommt keine luft", "erste-hilfe-ersticken-erkennen")
        fuehrtAuf(index, "würgt", "erste-hilfe-ersticken-erkennen")
        fuehrtAuf(index, "steckt im hals", "erste-hilfe-ersticken-erkennen")
        fuehrtAuf(index, "erstickt gleich", "erste-hilfe-ersticken-erkennen")
        // Blausucht. "wird blau" steht hier NICHT: Das kollidiert mit dem
        // Anleitungstitel "Den Meiler bauen: aus Holz WIRD Kohle", und
        // Titelwoerter wiegen am schwersten. Der Erstickungs-Eintrag steht
        // dort auf Platz zwei. Die Formulierungen, die eindeutig sind,
        // fuehren dagegen sauber -- und die stehen hier.
        fuehrtAuf(index, "blau", "erste-hilfe-ersticken-erkennen")
        fuehrtAuf(index, "blaue lippen", "erste-hilfe-ersticken-erkennen")
        fuehrtAuf(index, "blau im gesicht", "erste-hilfe-ersticken-erkennen")
        // Blutung
        fuehrtAuf(index, "blut spritzt", "erste-hilfe-starke-blutung")
        fuehrtAuf(index, "hoher blutverlust", "erste-hilfe-starke-blutung")
        // Schlaganfall -- die zwei Zeichen, die Laien beschreiben
        fuehrtAuf(index, "hängender mundwinkel", "erste-hilfe-schlaganfall")
        fuehrtAuf(index, "kann nicht sprechen", "erste-hilfe-schlaganfall")
        // Vergiftung und Veraetzung
        fuehrtAuf(index, "putzmittel getrunken", "erste-hilfe-veraetzung")
        fuehrtAuf(index, "pilz gegessen", "erste-hilfe-vergiftung-erkennen")
        // Allergie
        fuehrtAuf(index, "zunge dick", "erste-hilfe-allergischer-schock")
        // Krampfanfall, wie Umstehende ihn schildern
        fuehrtAuf(index, "schäumt", "erste-hilfe-krampfanfall-dringend")
        fuehrtAuf(index, "verdreht die augen", "erste-hilfe-krampfanfall-dringend")
        // Kopfverletzung
        fuehrtAuf(index, "kopf aufgeschlagen", "erste-hilfe-gehirnerschuetterung")
    }

    // Wie ANGEHOERIGE es schildern -- nicht der Betroffene selbst. Das ist der
    // haeufigste Fall: Jemand steht daneben und beschreibt, was er sieht.
    @Test
    fun schilderungenVonAngehoerigenFuehrenZumRichtigenEintrag() {
        val index = index()
        // Saeugling und Kind
        fuehrtAuf(index, "fontanelle", "erste-hilfe-austrocknung-erkennen")
        fuehrtAuf(index, "baby trinkt nicht", "erste-hilfe-austrocknung-erkennen")
        fuehrtAuf(index, "kind fiebert", "erste-hilfe-fieber-gefahr")
        fuehrtAuf(index, "kind schlapp", "erste-hilfe-fieber-gefahr")
        // Veraenderte Bewusstseinslage
        fuehrtAuf(index, "redet wirr", "erste-hilfe-reaktion-pruefen")
        fuehrtAuf(index, "erkennt mich nicht", "erste-hilfe-reaktion-pruefen")
        fuehrtAuf(index, "reagiert kaum", "erste-hilfe-reaktion-pruefen")
        // Unfall mit Wucht
        fuehrtAuf(index, "autounfall", "erste-hilfe-verletzung-beurteilen")
        fuehrtAuf(index, "überfahren", "erste-hilfe-verletzung-beurteilen")
        fuehrtAuf(index, "leiter gestürzt", "erste-hilfe-verletzung-beurteilen")
        fuehrtAuf(index, "vom dach gefallen", "erste-hilfe-verletzung-beurteilen")
        // Wasser
        fuehrtAuf(index, "aus dem wasser gezogen", "erste-hilfe-ertrinken")
        // Schlaganfallzeichen, die Laien beschreiben
        fuehrtAuf(index, "doppelt sehen", "erste-hilfe-schlaganfall")
        fuehrtAuf(index, "plötzlich blind", "erste-hilfe-schlaganfall")
        // Herz
        fuehrtAuf(index, "herzrasen", "erste-hilfe-brustschmerzen")
        fuehrtAuf(index, "herz rast", "erste-hilfe-brustschmerzen")
        // Geburt
        fuehrtAuf(index, "fruchtwasser", "erste-hilfe-geburt-erkennen")
        // Schock
        fuehrtAuf(index, "durchgeschwitzt", "erste-hilfe-schock")
        fuehrtAuf(index, "schweissausbruch", "erste-hilfe-schock")
    }

    // Giftpflanzen bei ihrem deutschen Namen. Vor dem 12.08.2026 fand das
    // Paket KEINE der toedlichen mitteleuropaeischen Pflanzen: "Tollkirsche"
    // fuehrte auf ein leeres Asthmaspray, "Fingerhut" auf die Bienen und aufs
    // Naehen, "Eisenhut" auf gar nichts -- und der Eisenhut ist die giftigste
    // Pflanze Mitteleuropas.
    @Test
    fun giftpflanzenBeiIhremDeutschenNamenFuehrenZumRichtigenEintrag() {
        val index = index()
        // sehr giftig
        fuehrtAuf(index, "eisenhut", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "tollkirsche", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "bilsenkraut", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "herbstzeitlose", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "seidelbast", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "stechapfel", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "engelstrompete", "nahrung-giftpflanzen-mitteleuropa")
        // giftig
        fuehrtAuf(index, "fingerhut", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "maiglöckchen", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "goldregen", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "eibe", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "pfaffenhütchen", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "aronstab", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "efeu", "nahrung-giftpflanzen-mitteleuropa")
        // haut
        fuehrtAuf(index, "bärenklau", "nahrung-giftpflanzen-mitteleuropa")
        // die beiden Entwarnungen, nach denen genauso gesucht wird
        fuehrtAuf(index, "vogelbeere", "nahrung-giftpflanzen-mitteleuropa")
        fuehrtAuf(index, "eicheln", "nahrung-giftpflanzen-mitteleuropa")
    }

    // Die Gegenprobe zu den Giftpflanzen: die ESSBAREN bei ihrem deutschen
    // Namen. Vorher fand "Sauerampfer", "Wegerich", "Sauerklee", "Wegwarte",
    // "Taglilie" und "Kaffeeersatz" nichts, "Klette" fuehrte aufs
    // Flussdurchqueren und "Portulak" auf Beerenstraeucher.
    //
    // ZWEI WOERTER STEHEN HIER MIT ABSICHT NICHT: "spitzwegerich" gehoert
    // weiter zu medizin-heilpflanzen-grenzen (die Warnung vor den Grenzen der
    // Heilpflanzen ist die konservativere erste Antwort), und "wildkraeuter"
    // bleibt bei nahrung-pflanzen-meiden -- erst die acht Ausschlusszeichen,
    // dann die Namensliste.
    @Test
    fun essbareWildpflanzenBeiIhremDeutschenNamenFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "brennnessel", "nahrung-essbares-gruen-namentlich")
        fuehrtAuf(index, "löwenzahn", "nahrung-essbares-gruen-namentlich")
        fuehrtAuf(index, "klette", "nahrung-essbares-gruen-namentlich")
        fuehrtAuf(index, "rohrkolben", "nahrung-essbares-gruen-namentlich")
        fuehrtAuf(index, "sauerampfer", "nahrung-essbares-gruen-namentlich")
        fuehrtAuf(index, "wegerich", "nahrung-essbares-gruen-namentlich")
        fuehrtAuf(index, "hagebutte", "nahrung-essbares-gruen-namentlich")
        fuehrtAuf(index, "sauerklee", "nahrung-essbares-gruen-namentlich")
        fuehrtAuf(index, "wegwarte", "nahrung-essbares-gruen-namentlich")
        fuehrtAuf(index, "taglilie", "nahrung-essbares-gruen-namentlich")
        fuehrtAuf(index, "kaffeeersatz", "nahrung-essbares-gruen-namentlich")
        // und die beiden, die NICHT wandern durften
        fuehrtAuf(index, "spitzwegerich", "medizin-heilpflanzen-grenzen")
        fuehrtAuf(index, "wildkräuter", "nahrung-pflanzen-meiden")
    }

    @Test
    fun pilzwoerterFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "knollenblätterpilz", "nahrung-knollenblaetterpilz")
        fuehrtAuf(index, "pilzvergiftung", "nahrung-knollenblaetterpilz")
        fuehrtAuf(index, "giftpilz", "nahrung-knollenblaetterpilz")
        fuehrtAuf(index, "champignon", "nahrung-knollenblaetterpilz")
        fuehrtAuf(index, "pilz sammeln", "nahrung-knollenblaetterpilz")
        // "pilze" gehoert NICHT hierher. NotfallSucheTest haelt fest, dass das
        // Wort zuerst auf die Vergiftungserkennung fuehren muss -- wer "pilze"
        // tippt, hat womoeglich schon gegessen. Beim Einbau dieses Eintrags am
        // 12.08.2026 hat genau dieser Test angeschlagen; die Schlagwoerter
        // "pilze sammeln" und "pilze essen" wurden daraufhin gestrichen.
        fuehrtAuf(index, "pilze", "erste-hilfe-vergiftung-erkennen")
    }

    @Test
    fun knopfzellenwoerterFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "knopfzelle", "erste-hilfe-knopfzelle")
        fuehrtAuf(index, "knopfbatterie", "erste-hilfe-knopfzelle")
        fuehrtAuf(index, "uhrenbatterie", "erste-hilfe-knopfzelle")
        fuehrtAuf(index, "speiseröhre", "erste-hilfe-knopfzelle")
        // "batterie" gehoert NICHT hierher: NotfallSucheTest haelt fest, dass
        // das Wort zuerst auf den CO-Melder fuehren muss (Batterie wechseln).
        // Beim Einbau am 12.08.2026 hat genau das angeschlagen; das Schlagwort
        // "batterie" wurde daraufhin gestrichen. Die Zusammensetzungen
        // ("knopfbatterie", "uhrenbatterie") stoeren nicht, weil die Suche
        // Wortanfaenge vergleicht.
        fuehrtAuf(index, "batterie", "erste-hilfe-kohlenmonoxid-melder")
    }

    @Test
    fun masernwoerterFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "masern", "medizin-masern")
        fuehrtAuf(index, "masernverdacht", "medizin-masern")
        fuehrtAuf(index, "kinderkrankheit", "medizin-masern")
        fuehrtAuf(index, "hornhaut trüb", "medizin-masern")
        // Diese beiden duerfen NICHT wandern und sind deshalb hier verankert:
        // "absondern" gehoert zum allgemeinen Eintrag ueber das Trennen
        // Kranker, "ansteckend" zum Haendewaschen. Beim Einbau der Masern am
        // 12.08.2026 hatten die Schlagwoerter "hochansteckend" und
        // "husten schnupfen ausschlag" sie kurzzeitig verdraengt.
        fuehrtAuf(index, "absondern", "medizin-kranke-absondern")
        fuehrtAuf(index, "hochansteckend", "medizin-kranke-absondern")
    }

    @Test
    fun keuchhustenwoerterFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "keuchhusten", "medizin-keuchhusten")
        fuehrtAuf(index, "stickhusten", "medizin-keuchhusten")
        fuehrtAuf(index, "hustenanfall", "medizin-keuchhusten")
        fuehrtAuf(index, "atempause", "medizin-keuchhusten")
        fuehrtAuf(index, "keuchen", "medizin-keuchhusten")
        // Diese drei duerfen NICHT wandern. Beim Einbau am 12.08.2026 hatte der
        // erste Titel ("...wochenlange Anfaelle, und warum Hustenmittel
        // schaden") sie alle drei verdraengt: "husten" gehoert zum Ersticken,
        // "anfaelle" zur Epilepsie, "geben" zur Unterzuckerung. Der Titel
        // heisst jetzt "Keuchhusten: wochenlang, und was dabei schadet".
        fuehrtAuf(index, "husten", "erste-hilfe-ersticken-kann-husten")
        fuehrtAuf(index, "anfälle", "medizin-epilepsie-mittel-gehen-aus")
        fuehrtAuf(index, "geben", "erste-hilfe-unterzuckerung")
    }

    @Test
    fun typhuswoerterFuehrenZumRichtigenEintrag() {
        val index = index()
        // "typhus" fuehrte vor dem 12.08.2026 auf medizin-blinddarm-verwechslung.
        fuehrtAuf(index, "typhus", "medizin-typhus")
        fuehrtAuf(index, "typhusverdacht", "medizin-typhus")
        fuehrtAuf(index, "darmdurchbruch", "medizin-typhus")
        // "fieber" gehoert weiter dem Fieber-Eintrag; der erste Titel dieses
        // Eintrags trug das Wort und hat ihn verdraengt (TitelwaechterTest).
        fuehrtAuf(index, "fieber", "erste-hilfe-fieber-gefahr")
    }

    @Test
    fun hirnhautentzuendungFuehrtZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "hirnhautentzündung", "medizin-hirnhautentzuendung")
        fuehrtAuf(index, "meningitis", "medizin-hirnhautentzuendung")
        fuehrtAuf(index, "meningokokken", "medizin-hirnhautentzuendung")
        fuehrtAuf(index, "steifer nacken", "medizin-hirnhautentzuendung")
        fuehrtAuf(index, "glastest", "medizin-hirnhautentzuendung")
        // "flecken" hat eine Geschichte: Es gehoerte dem Schimmelfleisch-Eintrag,
        // wurde am 12.08.2026 erst vom Schlagwort "blauer Fleck" abgezogen
        // (zurueckgenommen), dann still vom Keuchhusten-Schlagwort "rote
        // flecken im auge" (ebenfalls zurueckgenommen). Jetzt fuehrt es auf die
        // Hirnhautentzuendung, und das ist Absicht: Der nicht wegdrueckbare
        // Fleck ist die lebensbedrohliche Bedeutung. Der Schimmelfleisch-
        // Eintrag steht auf Platz zwei, also auf demselben Bildschirm.
        fuehrtAuf(index, "flecken", "medizin-hirnhautentzuendung")
        fuehrtAuf(index, "flecken", "nahrung-fleisch-schimmel-und-stickigkeit")
        // Und der bleibt, wo er ist:
        fuehrtAuf(index, "ausschlag", "erste-hilfe-fieber-gefahr")
    }

    @Test
    fun unterernaehrungswoerterFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "unterernährt", "medizin-unterernaehrt-erste-tage")
        fuehrtAuf(index, "ausgehungert", "medizin-unterernaehrt-erste-tage")
        fuehrtAuf(index, "zuckerwasser", "medizin-unterernaehrt-erste-tage")
        fuehrtAuf(index, "auszehrung", "medizin-unterernaehrt-erste-tage")
        // Diese beiden duerfen NICHT wandern. Beim Einbau am 13.08.2026 hatten
        // die Schlagwoerter "waermflasche" und "unterkuehlt und mager" sie
        // verdraengt; beide wurden gestrichen.
        fuehrtAuf(index, "wärmflasche", "erste-hilfe-erfrierungen-versorgen")
        fuehrtAuf(index, "unterkühlt", "erste-hilfe-unterkuehlung-stadium-eins")
    }

    @Test
    fun atemwegsverschlusswoerterFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "diphtherie", "medizin-diphtherie-kehldeckel")
        fuehrtAuf(index, "kehldeckelentzündung", "medizin-diphtherie-kehldeckel")
        fuehrtAuf(index, "epiglottitis", "medizin-diphtherie-kehldeckel")
        fuehrtAuf(index, "grauer belag", "medizin-diphtherie-kehldeckel")
        fuehrtAuf(index, "sabbert", "medizin-diphtherie-kehldeckel")
        // Diese beiden duerfen NICHT wandern. Beim Einbau am 13.08.2026 hatten
        // der Titel ("...nicht in den Rachen schauen") und das Schlagwort
        // "kann nicht schlucken" sie verdraengt: "rachen" gehoert dem
        // Insektenstich im Mund, "schlucken" den Halsschmerzen. Der Titel
        // heisst jetzt "...nicht hineinschauen".
        fuehrtAuf(index, "rachen", "erste-hilfe-insektenstich-mund")
        fuehrtAuf(index, "schlucken", "medizin-halsschmerzen")
    }

    @Test
    fun sonnenstichFuehrtNichtMehrAufDenHitzschlag() {
        val index = index()
        // "sonnenstich" fuehrte bis zum 17.08.2026 auf erste-hilfe-hitzschlag-
        // erkennen. Medizinisch ist es NICHT dasselbe: Beim Sonnenstich ist der
        // Koerper normal temperiert, nur Kopf und Nacken sind betroffen.
        fuehrtAuf(index, "sonnenstich", "erste-hilfe-sonnenstich")
        fuehrtAuf(index, "kühlpad", "erste-hilfe-sonnenstich")
        fuehrtAuf(index, "ohrensausen", "erste-hilfe-sonnenstich")
        // Und diese beiden bleiben, wo sie waren:
        fuehrtAuf(index, "hitzschlag", "erste-hilfe-hitzschlag-erkennen")
        fuehrtAuf(index, "steifer nacken", "medizin-hirnhautentzuendung")
    }

    // Aeltere und volkstuemliche Namen. Der Inhalt war jeweils da, nur das Wort
    // fehlte -- gemessen am 17.08.2026: fuenf davon fanden GAR NICHTS, und
    // "zuckerkrankheit" fuehrte auf das Oeffnen eines Abszesses. Wer diese
    // Woerter benutzt, ist oft aelter und im Ernstfall allein.
    @Test
    fun altmodischeNamenFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "schlagfluss", "erste-hilfe-schlaganfall")
        fuehrtAuf(index, "hirnschlag", "erste-hilfe-schlaganfall")
        fuehrtAuf(index, "herzkasper", "erste-hilfe-brustschmerzen")
        fuehrtAuf(index, "wassersucht", "medizin-herzschwaeche-ohne-mittel")
        fuehrtAuf(index, "zuckerkrankheit", "medizin-ueberzuckerung")
        fuehrtAuf(index, "schwindelanfall", "erste-hilfe-ohnmacht")
        fuehrtAuf(index, "kreislaufkollaps", "erste-hilfe-ohnmacht")
        // Diese trugen schon vorher richtig und sind hier nur festgehalten:
        fuehrtAuf(index, "aderpresse", "erste-hilfe-abbinden")
        fuehrtAuf(index, "fallsucht", "erste-hilfe-krampfanfall")
        fuehrtAuf(index, "schwindsucht", "medizin-tuberkulose")
        fuehrtAuf(index, "wundstarrkrampf", "medizin-starrkrampf")
    }

    @Test
    fun gedraengewoerterFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "gedränge", "taktisch-gedraenge")
        fuehrtAuf(index, "menschenmenge", "taktisch-gedraenge")
        fuehrtAuf(index, "massenpanik", "taktisch-gedraenge")
        fuehrtAuf(index, "eingekeilt", "taktisch-gedraenge")
        // "druck" gehoert dem Blutdruckmessen -- das ist die haeufigere Frage.
        // Der erste Titel ("...sondern der Druck") hatte es verdraengt.
        fuehrtAuf(index, "druck", "medizin-blutdruck-messen")
    }

    @Test
    fun reihenfolgewoerterFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "abcde", "erste-hilfe-reihenfolge-erste-sekunden")
        fuehrtAuf(index, "womit anfangen", "erste-hilfe-reihenfolge-erste-sekunden")
        fuehrtAuf(index, "was mache ich zuerst", "erste-hilfe-reihenfolge-erste-sekunden")
        fuehrtAuf(index, "wo fange ich an", "erste-hilfe-reihenfolge-erste-sekunden")
        fuehrtAuf(index, "erstbeurteilung", "erste-hilfe-reihenfolge-erste-sekunden")
        // "sekunden" gehoert dem Lawinen-Eintrag: dort zaehlen sie wirklich.
        // Der erste Titel ("Die ersten Sekunden...") hatte es verdraengt.
        fuehrtAuf(index, "sekunden", "erste-hilfe-lawine-selbst-erfasst")
    }

    // Die vierte Messrunde, 21.08.2026: 104 Alltagswoerter aus den Bereichen
    // Feuer/Werkzeug, Tierhaltung, Anbau sowie Naehen und Weben. Die Bereiche
    // standen im SNAPSHOT ausdruecklich als "noch nicht abgesucht".
    //
    // Vier echte Fehltreffer, alle mit derselben Ursache: Die Suche vergleicht
    // WORTANFAENGE. "knoten" passt deshalb nicht auf "Grundknoten", "heu"
    // nicht auf "Heumachen". Beide Anleitungen waren also fuer ihr eigenes
    // Hauptwort unsichtbar.
    //
    //   "knoten"  fand die Knotenanleitung UEBERHAUPT NICHT (Platz 1 war
    //             "Abszess eroeffnen" -- gemeint war dort ein Knoten unter
    //             der Haut).
    //   "heu"     fand das Heukapitel erst auf Platz 4, hinter Nasenspuelung,
    //             Kerbtieren und dem Heulton der Sirene.
    //   "zunder"  fand die Zuendanleitung nicht, obwohl deren erster Schritt
    //             "Zuerst der Zunder" heisst -- Schritttext wiegt zu wenig.
    //
    // Behoben durch Titel und Ueberschrift, nicht durch Umschreiben der
    // Inhalte. "grundknoten" steht hier mit, weil die erste Fassung der
    // Berichtigung dieses Wort versehentlich entfernt hatte.
    @Test
    fun alltagswoerterFuerFeuerWerkzeugUndTierhaltungFuehrenZumRichtigenEintrag() {
        val index = index()
        fuehrtAuf(index, "knoten", "seilwerk-grundknoten")
        fuehrtAuf(index, "grundknoten", "seilwerk-grundknoten")
        fuehrtAuf(index, "lashing", "seilwerk-grundknoten")
        fuehrtAuf(index, "zunder", "feuer-ohne-zuendmittel")
        fuehrtAuf(index, "streichholz", "feuer-ohne-zuendmittel")
        fuehrtAuf(index, "heu", "agrikultur-heu")
        fuehrtAuf(index, "silage", "agrikultur-heu")
        // Diese waren schon vorher richtig und stehen hier, damit sie es
        // bleiben -- sie haengen an denselben Titeln.
        fuehrtAuf(index, "melken", "agrikultur-milch-sauber")
        fuehrtAuf(index, "klaue", "agrikultur-tiergesundheit")
        fuehrtAuf(index, "euter", "agrikultur-tiergesundheit")
        fuehrtAuf(index, "milbe", "agrikultur-milben-laeuse")
        fuehrtAuf(index, "kompost", "agrikultur-kompost")
        fuehrtAuf(index, "fruchtfolge", "agrikultur-fruchtfolge")
        fuehrtAuf(index, "gerben", "agrikultur-gerben")
        fuehrtAuf(index, "garn", "agrikultur-garn-machen")
    }

    // Sieben Bauanleitungen fuehren eine Wasserwaage in ihrer Werkzeugliste,
    // ohne im Text zu sagen, was man ohne sie tut: Stampflehmwand,
    // Feldsteinmauer, Schornstein, Wasserrad, Brunnen, Pumpe, Tisch und Bank.
    // Im SNAPSHOT stand das als offene Luecke ("Offen: Wasserwaage (7)").
    //
    // Sie ist KEINE Luecke: "Waagerecht und senkrecht pruefen ohne gekauftes
    // Werkzeug" gibt es, und die Suche fuehrt von jedem naheliegenden Wort
    // dorthin -- gemessen am 21.08.2026. Damit ist der Weg von der
    // Bauanleitung zur Ersatzloesung die SUCHE, nicht ein Satz im Text. Genau
    // deshalb stehen die Wege hier: Wer den Titel dieser Anleitung aendert,
    // kappt die Verbindung fuer alle sieben auf einmal, ohne dass ein
    // Querverweis kaputtgeht -- es gibt naemlich keinen.
    @Test
    fun ohneWasserwaageFuehrtJedesWortZurErsatzanleitung() {
        val index = index()
        fuehrtAuf(index, "wasserwaage", "werkzeug-waagerecht-senkrecht")
        fuehrtAuf(index, "waagerecht", "werkzeug-waagerecht-senkrecht")
        fuehrtAuf(index, "senkrecht", "werkzeug-waagerecht-senkrecht")
        fuehrtAuf(index, "senklot", "werkzeug-waagerecht-senkrecht")
        fuehrtAuf(index, "schlauchwaage", "werkzeug-waagerecht-senkrecht")
        fuehrtAuf(index, "rechter winkel", "werkzeug-waagerecht-senkrecht")
    }
}
