package org.compasszero.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// Mehrere Tipps verweisen im Fliesstext auf andere ("siehe den eigenen Tipp
// dazu"). Solche Verweise sind fuer die Maschine unsichtbar: Wird der gemeinte
// Tipp umbenannt oder entfernt, zeigt der Satz ins Leere, und niemand merkt es.
//
// In einem Notfallhandbuch ist das kein Schoenheitsfehler. Der Verweis in
// "Ohne Uebung: nur druecken" fuehrt zur Kinder-Wiederbelebung, und die
// Oberbauchkompression verweist auf die Saeuglings-Fassung -- fehlt die, wendet
// jemand das Erwachsenen-Verfahren auf ein Baby an.
//
// Deshalb stehen die Paare hier ausdruecklich. Wer einen Tipp entfernt oder
// umbenennt, faellt hier auf.
class QuerverweiseTest {

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
    private fun paketLaden(): LoadedPack {
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
        return result.pack ?: error("Paket laedt nicht: ${result.problems}")
    }

    private fun tipps(): Map<String, Tip> = paketLaden().tips.associateBy { it.id }

    // Nicht nur Tipps verweisen auf Tipps. Die Bauanleitungen tun es auch --
    // die Schneehoehle nennt den Kohlenmonoxid-Tipp, die Flussquerung den
    // Grabenfuss. Solche Verweise sind genauso zerbrechlich, standen aber bis
    // zum 02.08.2026 in keiner Pruefung, weil hier nur die Tipps geladen
    // wurden. Deshalb liefert das hier den durchsuchbaren Text JEDES Eintrags,
    // egal welcher Art.
    private fun verweistexte(): Map<String, String> {
        val pack = paketLaden()
        val out = HashMap<String, String>()
        for (tip in pack.tips) out[tip.id] = tip.body
        for (guide in pack.guides) {
            out[guide.id] = guide.summary + "\n" + guide.steps.joinToString("\n") { it.text }
        }
        for (kapitel in pack.agriculture) {
            out[kapitel.id] = kapitel.sections.joinToString("\n") { it.heading + "\n" + it.body }
        }
        return out
    }

    // Und dasselbe fuer die Ziele: Ein Verweis kann auch auf eine Bauanleitung
    // oder ein Agrikultur-Kapitel zeigen ("siehe die Anleitung Fallen stellen").
    private fun alleTitel(): Map<String, String> {
        val pack = paketLaden()
        val out = HashMap<String, String>()
        for (tip in pack.tips) out[tip.id] = tip.title
        for (guide in pack.guides) out[guide.id] = guide.title
        for (kapitel in pack.agriculture) out[kapitel.id] = kapitel.title
        return out
    }

    // Wer verweist worauf. Der dritte Eintrag ist die Wortgruppe, an der der
    // Verweis im Text haengt -- verschwindet sie, ist der Verweis weg und das
    // Paar gehoert geloescht.
    private val verweise = listOf(
        Triple("erste-hilfe-nur-druecken", "erste-hilfe-wiederbelebung-kind", "siehe den eigenen Tipp dazu"),
        Triple("erste-hilfe-ersticken-oberbauchkompression", "erste-hilfe-ersticken-kind", "siehe die eigenen Tipps dazu"),
        Triple("erste-hilfe-ersticken-oberbauchkompression", "erste-hilfe-ersticken-saeugling", "siehe die eigenen Tipps dazu"),
        Triple("erste-hilfe-wunde-verbote", "erste-hilfe-starke-blutung", "Tipp zum Stillen starker Blutungen"),
        Triple("erste-hilfe-blutung-kopf-rumpf", "erste-hilfe-druckverband", "im eigenen Tipp dazu"),
        Triple("erste-hilfe-entscheidung-nach-atemkontrolle", "erste-hilfe-ersticken-erkennen", "siehe die Tipps dazu"),
        Triple("erste-hilfe-entscheidung-nach-atemkontrolle", "erste-hilfe-stabile-seitenlage", "stabile Seitenlage"),
        // Aus dem Stimmigkeits-Durchgang: Die drei Erwachsenen-Ersticken-Tipps
        // nennen jetzt ihren Geltungsbereich und verweisen weiter.
        Triple("erste-hilfe-ersticken-erkennen", "erste-hilfe-ersticken-kind", "siehe die eigenen Tipps dazu"),
        Triple("erste-hilfe-ersticken-kann-husten", "erste-hilfe-ersticken-saeugling", "siehe die eigenen Tipps dazu"),
        Triple("erste-hilfe-ersticken-rueckenschlaege", "erste-hilfe-ersticken-saeugling", "siehe die eigenen Tipps dazu"),
        // Der Kinder-Tipp verweist auf die Drucktechnik, die es vorher nicht gab.
        Triple("erste-hilfe-wiederbelebung-kind", "erste-hilfe-herzdruckmassage-kind", "Herzdruckmassage bei Kindern"),
        // Die drei Erkennen-Tipps sagen jetzt, wo das Handeln steht.
        Triple("erste-hilfe-hitzschlag-erkennen", "erste-hilfe-hitzschlag-handeln", "Hitzschlag versorgen"),
        Triple("erste-hilfe-erfrierungen-erkennen", "erste-hilfe-erfrierungen-versorgen", "Erfrierungen versorgen"),
        Triple("erste-hilfe-knochenbruch-erkennen", "erste-hilfe-knochenbruch-versorgen", "Knochenbruch versorgen"),
        // Die Wund-Tipps nennen alle den Vorrang der Blutstillung.
        Triple("erste-hilfe-wunde-bedecken", "erste-hilfe-starke-blutung", "Tipp zum Stillen starker Blutungen"),
        Triple("erste-hilfe-fremdkoerper-in-wunde", "erste-hilfe-starke-blutung", "Tipp zum Stillen starker Blutungen"),
        // Aus dem Vergiftungs-Durchgang: Die beiden Tipps, die selbst ueber die
        // Seitenlage entscheiden, sagen jetzt auch, wo die Handgriffe stehen.
        Triple("erste-hilfe-vergiftung-erkennen", "erste-hilfe-seitenlage-handgriffe", "Stabile Seitenlage: Handgriffe"),
        Triple("erste-hilfe-vergiftung-atemwege", "erste-hilfe-seitenlage-handgriffe", "Stabile Seitenlage: Handgriffe"),
        // Und der Rettungs-Tipp nennt die Ausnahme, in der Retten toedlich ist.
        Triple("erste-hilfe-vergiftung-atemwege", "erste-hilfe-kohlendioxid", "Kohlendioxid: nicht selbst hineingehen"),
        Triple("erste-hilfe-erbrechen-helfen", "erste-hilfe-stabile-seitenlage", "siehe den Tipp dazu"),
        // Brandrauch: der Rettungs-Tipp nennt jetzt beide toedlichen Ausnahmen.
        Triple("erste-hilfe-vergiftung-atemwege", "erste-hilfe-brandrauch-nicht-hineingehen", "Rauch im Haus: nicht hineingehen"),
        Triple("erste-hilfe-rauchvergiftung-helfen", "erste-hilfe-seitenlage-handgriffe", "Stabile Seitenlage: Handgriffe"),
        // Kohlenmonoxid: verweist auf den Rauch-Selbstschutz und auf die Handgriffe.
        Triple("erste-hilfe-kohlenmonoxid-handeln", "erste-hilfe-brandrauch-nicht-hineingehen", "Rauch im Haus: nicht hineingehen"),
        Triple("erste-hilfe-kohlenmonoxid-handeln", "erste-hilfe-seitenlage-handgriffe", "Stabile Seitenlage: Handgriffe"),
        Triple("erste-hilfe-kohlendioxid", "erste-hilfe-kohlenmonoxid-erkennen", "Kohlenmonoxid: unsichtbar und geruchlos"),
        Triple("erste-hilfe-kohlendioxid", "erste-hilfe-kohlenmonoxid-handeln", "Kohlenmonoxid: was zu tun ist"),
        // Durchfall: alle vier Nebentipps fuehren zum Trinken-Tipp zurueck, denn
        // der Fluessigkeitsersatz ist die eigentliche Behandlung.
        Triple("erste-hilfe-austrocknung-erkennen", "erste-hilfe-durchfall-trinken", "Durchfall: mehr trinken"),
        Triple("erste-hilfe-durchfall-nicht-trinken", "erste-hilfe-durchfall-trinken", "Durchfall: mehr trinken"),
        Triple("erste-hilfe-trinkloesung-selbst-ansetzen", "erste-hilfe-durchfall-trinken", "Durchfall: mehr trinken"),
        Triple("erste-hilfe-durchfall-hilfe-holen", "erste-hilfe-durchfall-trinken", "Durchfall: mehr trinken"),
        Triple("erste-hilfe-durchfall-trinken", "erste-hilfe-durchfall-nicht-trinken", "Durchfall: was nicht trinken"),
        Triple("erste-hilfe-durchfall-trinken", "erste-hilfe-trinkloesung-selbst-ansetzen", "Trinklösung selbst ansetzen"),
        Triple("erste-hilfe-austrocknung-erkennen", "erste-hilfe-seitenlage-handgriffe", "Stabile Seitenlage: Handgriffe"),
        // Das Wasser fuer die Trinkloesung ist in dieser Lage der wahrscheinliche
        // Ausloeser des Durchfalls -- die Wasser-Tipps und die Durchfall-Tipps
        // kannten einander vorher in keine Richtung.
        Triple("erste-hilfe-trinkloesung-selbst-ansetzen", "wasser-abkochen", "Wasser abkochen"),
        Triple("erste-hilfe-erbrechen-helfen", "erste-hilfe-durchfall-trinken", "Durchfall: mehr trinken"),
        // Zink und die Medikamenten-Warnung fuehren beide zum Trinken zurueck:
        // Beide koennten sonst als Ersatz dafuer gelesen werden.
        Triple("erste-hilfe-durchfall-zink", "erste-hilfe-durchfall-trinken", "Durchfall: mehr trinken"),
        Triple("erste-hilfe-durchfall-medikamente", "erste-hilfe-durchfall-trinken", "Durchfall: mehr trinken"),
        // Der Essens-Tipp verlangt Seife; ohne den Verweis bekaeme jemand ohne
        // Seife keinen Ersatz genannt, obwohl das Paket ihn kennt.
        Triple("hygiene-essen", "hygiene-haendewaschen", "Händewaschen, auch ohne Seife"),
        Triple("erste-hilfe-antibiotika-reste", "erste-hilfe-durchfall-trinken", "Durchfall: mehr trinken"),
        // Abbindung: Der vorhandene Blutungs-Tipp sagte "fast jede Blutung laesst
        // sich durch Druck stillen" und schwieg ueber die uebrigen.
        Triple("erste-hilfe-starke-blutung", "erste-hilfe-abbinden", "Abbinden (Tourniquet)"),
        Triple("erste-hilfe-abbinden", "erste-hilfe-starke-blutung", "Blutung stillen"),
        Triple("erste-hilfe-abbinden", "erste-hilfe-blutung-kopf-rumpf", "Blutung: Kopf, Rumpf, Bein"),
        Triple("erste-hilfe-amputat", "erste-hilfe-abbinden", "Abbinden (Tourniquet)"),
        Triple("erste-hilfe-amputat", "erste-hilfe-starke-blutung", "Blutung stillen"),
        // "Jede Wunde wird keimfrei bedeckt" gilt fuer den Brustkorb NICHT.
        Triple("erste-hilfe-wunde-bedecken", "erste-hilfe-offene-brustwunde", "Offene Brustwunde: nicht zudecken"),
        // Aus dem ERC-Durchgang: Der Rumpf-Tipp deckt den Brustkorb mit ab,
        // und das Abbinden hat eine ausdrueckliche Gegenanzeige.
        Triple("erste-hilfe-blutung-kopf-rumpf", "erste-hilfe-offene-brustwunde", "Offene Brustwunde: nicht zudecken"),
        Triple("erste-hilfe-abbinden", "erste-hilfe-schlangenbiss", "Schlangenbiss: ruhig halten, nichts aussaugen"),
        Triple("erste-hilfe-nur-druecken", "erste-hilfe-ertrinken", "Ertrinken: nicht hinterherspringen"),
        // Die Leitlinie verbietet die Seitenlage nach einem Trauma. Beide
        // Tipps, die dorthin schicken, nennen die Ausnahme jetzt.
        Triple("erste-hilfe-stabile-seitenlage", "erste-hilfe-wirbelsaeule", "Wirbelsäulenverletzung: nicht in die Seitenlage"),
        Triple("erste-hilfe-entscheidung-nach-atemkontrolle", "erste-hilfe-wirbelsaeule", "Wirbelsäulenverletzung: nicht in die Seitenlage"),
        Triple("erste-hilfe-seitenlage-handgriffe", "erste-hilfe-wirbelsaeule", "Wirbelsäulenverletzung: nicht in die Seitenlage"),
        Triple("erste-hilfe-unterkuehlung-stadium-zwei", "erste-hilfe-seitenlage-handgriffe", "Stabile Seitenlage: Handgriffe"),
        Triple("erste-hilfe-hitzschlag-handeln", "erste-hilfe-seitenlage-handgriffe", "Stabile Seitenlage: Handgriffe"),
        Triple("erste-hilfe-allergischer-schock", "erste-hilfe-insektenstich-mund", "Insektenstich im Mund oder Rachen"),
        // Zahnmedizin: Beide Tipps, die das Ziehen eines Zahnes erwaehnen,
        // fuehren zu der Stelle, an der die Grenze steht -- Besteck, Betaeubung
        // und Uebung. Ohne diesen Verweis liest sich "muss gezogen werden" wie
        // eine Erlaubnis.
        Triple("medizin-zahn-abgebrochen", "medizin-zahnabszess-ohne-zahnarzt", "Zahnabszess: was ohne Zahnarzt hilft"),
        Triple("medizin-zahn-abgebrochen", "medizin-zahnabszess-erkennen", "Zahnabszess erkennen"),
        Triple("medizin-zahnabszess-ohne-zahnarzt", "medizin-zahnabszess-erkennen", "Zahnabszess erkennen"),
        // Insulin: Der Rahmen-Tipp schickt bei Unterzuckerung weiter, und das
        // ist die Gefahr beim Wechseln -- nicht der zu hohe Wert.
        Triple("medizin-insulin-wechseln-grundsatz", "erste-hilfe-unterzuckerung", "Unterzuckerung: Zucker geben"),
        // Hoehenkrankheit: Die Erkennen-Tipps schicken beide zum Absteigen, und
        // der Erkennen-Tipp nennt die Kohlenmonoxid-Verwechslung -- ein Ofen im
        // Zelt macht dieselben Beschwerden.
        Triple("medizin-hoehenkrankheit", "erste-hilfe-kohlenmonoxid-erkennen", "Kohlenmonoxid: unsichtbar und geruchlos"),
        Triple("medizin-hoehenkrankheit", "medizin-hoehe-gefaehrlich", "Höhenkrankheit: die zwei gefährlichen Formen"),
        Triple("medizin-hoehe-gefaehrlich", "medizin-hoehe-absteigen", "Höhenkrankheit: absteigen ist die Behandlung"),
        // Kopfschmerz in grosser Hoehe hat eine eigene Ursache, und im Auge
        // gilt dieselbe Regel wie bei jeder Wunde: nichts herausziehen.
        Triple("medizin-kopfschmerz", "medizin-hoehenkrankheit", "Höhenkrankheit erkennen"),
        Triple("medizin-augenverletzung", "erste-hilfe-fremdkoerper-in-wunde", "Fremdkörper in der Wunde"),
        // Der Blasen-Tipp muss zur Niere weiterfuehren -- der Unterschied
        // zwischen laestig und ernst haengt an zwei Zeichen.
        Triple("medizin-blasenentzuendung", "medizin-nierenbeckenentzuendung", "Nierenbeckenentzündung: wenn es die Niere erreicht"),
        // Blinddarm: Der Erkennen-Tipp muss zu den Verwechslungen fuehren, und
        // die Verwechslungen zurueck zur Niere.
        Triple("medizin-blinddarm", "medizin-blinddarm-verwechslung", "Bauchschmerz rechts unten: was sonst dahinterstecken kann"),
        Triple("medizin-blinddarm-verwechslung", "medizin-nierenbeckenentzuendung", "Nierenbeckenentzündung: wenn es die Niere erreicht"),
        // Gelbsucht ueber Wasser: Die einzige wirksame Massnahme ist Vorbeugung,
        // und die steht in drei bestehenden Tipps. Verschwinden die, zeigt der
        // Verweis ins Leere.
        Triple("medizin-gelbsucht-wasser", "wasser-abkochen", "Wasser abkochen"),
        Triple("medizin-gelbsucht-wasser", "hygiene-haendewaschen", "Händewaschen, auch ohne Seife"),
        Triple("medizin-gelbsucht-wasser", "hygiene-ausscheidungen", "Stuhl entsorgen ohne Toilette"),
        // Nagetiere: Der Abwehr-Tipp sagt, WIE man sie fernhaelt, der andere,
        // WARUM man das auch bei vollen Vorraeten tut. Einzeln gelesen wirkt
        // der eine wie Ordnungsliebe und der andere wie Pech.
        Triple("nahrung-vorraete-nager-abwehren", "medizin-nager-krankheiten", "Was Ratten und Mäuse übertragen"),
        Triple("medizin-nager-krankheiten", "nahrung-vorraete-nager-abwehren", "Ratten und Mäuse von den Vorräten fernhalten"),
        // Seife sieden erzeugt Aschenlauge, und die ist aetzend. Ohne den
        // Verweis endet die Anleitung mit einem Topf voll Lauge und ohne den
        // Satz, was bei einem Spritzer ins Auge zu tun ist.
        Triple("hygiene-seife-selbst", "erste-hilfe-veraetzung", "Verätzung: spülen, bis der Schmerz nachlässt"),
        Triple("hygiene-seife-selbst", "hygiene-haendewaschen", "Händewaschen, auch ohne Seife"),
        // Lungenentzuendung: Der Erkennen-Tipp sagt, woran man sie erkennt. Ob
        // sie kippt, entscheidet sich an fuenf anderen Zeichen -- und die
        // Erwachsenengrenze von 30 Atemzuegen waere bei einem Kind falsch.
        Triple("medizin-lungenentzuendung", "medizin-lungenentzuendung-kippt", "Wann eine Lungenentzündung kippt"),
        Triple("medizin-lungenentzuendung-kippt", "medizin-lungenentzuendung-kind", "Wenn ein Kind zu schnell atmet: Lungenentzündung"),
        Triple("medizin-bronchitis", "medizin-lungenentzuendung", "Lungenentzündung erkennen"),
        Triple("medizin-bronchitis", "medizin-lungenentzuendung-kippt", "Wann eine Lungenentzündung kippt"),
        Triple("medizin-bronchitis", "medizin-antibiotika-wann-keines", "Antibiotika: wann keines gegeben wird"),
        // Tuberkulose: Ein langer Husten hat haeufigere Ursachen, und das
        // Absondern hat einen eigenen Tipp -- ohne ihn liest sich "mehrere
        // Wochen absondern" wie eine Anweisung, jemanden wegzusperren.
        Triple("medizin-tuberkulose", "medizin-bronchitis", "Bronchitis: warum ein Antibiotikum meist nichts bringt"),
        Triple("medizin-tuberkulose", "medizin-kranke-absondern", "Kranke absondern: die Hände zählen mehr als die Tür"),
        // Seele und Belastung: Der Zusammenbruch sieht dem Entzug aehnlich, und
        // der eine ist toedlich. Zittern hat bei Kaelte eine ganz andere
        // Ursache. Beide Verwechslungen fuehren in die falsche Behandlung.
        Triple("medizin-belastungsreaktion", "medizin-alkoholentzug-toedlich", "Alkoholentzug kann töten, Opiatentzug nicht"),
        Triple("medizin-belastungsreaktion", "taktisch-gruppe-tragen", "Zusammen durchkommen: reden hilft messbar"),
        Triple("medizin-belastungsreaktion", "taktisch-helfer-schuetzen", "Wer hilft, hält das nicht endlos aus"),
        Triple("medizin-angst-beruhigen", "erste-hilfe-unterkuehlung-stadium-eins", "Unterkühlung Stadium I: Person zittert noch"),
        Triple("taktisch-helfer-schuetzen", "medizin-belastungsreaktion", "Wenn jemand zusammenbricht: vier Dinge, die helfen"),
        // Saeugling: Die beiden Tipps sind zwei Haelften einer Entscheidung --
        // reicht die Milch, und was gibt man, wenn nicht. Und beim Ersatz ist
        // das Wasser der gefaehrlichste Teil, nicht die Milch.
        Triple("medizin-stillen-genug", "medizin-muttermilch-ersatz", "Wenn nicht gestillt werden kann: die Rangfolge"),
        Triple("medizin-muttermilch-ersatz", "medizin-stillen-genug", "Stillen: bekommt das Kind genug?"),
        Triple("medizin-muttermilch-ersatz", "wasser-abkochen", "Wasser abkochen"),
        // Kindbettfieber: Der Weg dorthin fuehrt ueber die Geburt selbst, und
        // die Antibiotika-Regel gilt hier mit einer Ausnahme.
        Triple("medizin-kindbettfieber", "erste-hilfe-geburt-danach", "Nach der Geburt: Nabelschnur und Nachgeburt"),
        Triple("medizin-kindbettfieber", "medizin-antibiotika-wann-keines", "Antibiotika: wann keines gegeben wird"),
        // Fruehschwangerschaft: Beide Tipps fuehren zum Schock, weil bei
        // beiden die sichtbare Blutungsmenge in die Irre fuehrt. Und die
        // Fehlgeburt fuehrt zum Kindbettfieber, weil die Infektionszeichen
        // dieselben sind.
        Triple("medizin-eileiterschwangerschaft", "erste-hilfe-schock", "Schock erkennen: blass, kalt, schneller Puls"),
        Triple("medizin-eileiterschwangerschaft", "medizin-blinddarm-verwechslung", "Bauchschmerz rechts unten: was sonst dahinterstecken kann"),
        Triple("medizin-fehlgeburt", "medizin-kindbettfieber", "Kindbettfieber: drei Zeichen in den Tagen danach"),
        Triple("medizin-fehlgeburt", "erste-hilfe-schock", "Schock erkennen: blass, kalt, schneller Puls"),
        Triple("medizin-fehlgeburt", "erste-hilfe-gebaermutter-zusammenziehen", "Gebärmutter zusammenziehen: anlegen, tasten, nicht ziehen"),
        // Schwangerschaft und Mittel: Die vier Verweise sind die Stellen, an
        // denen der Tipp eine Zahl oder eine Regel NICHT selbst nennt, sondern
        // auf die belegte Fassung zeigt.
        Triple("medizin-schwangerschaft-mittel", "erste-hilfe-wanderroete", "Wanderröte erkennen"),
        Triple("medizin-schwangerschaft-mittel", "erste-hilfe-kohlenmonoxid-erkennen", "Kohlenmonoxid: unsichtbar und geruchlos"),
        Triple("medizin-schwangerschaft-mittel", "wasser-abkochen", "Wasser abkochen"),
        Triple("medizin-schwangerschaft-mittel", "hinweis-angaben-ohne-gewaehr", "Alle Angaben ohne Gewähr"),
        // Aus der Paket-Durchsicht vom 03.08.2026: Diese beiden Tipps nennen
        // Milligramm-Angaben und waren die einzigen ohne den Vorbehalts-Absatz,
        // den der Merkzettel dafuer verlangt.
        Triple("erste-hilfe-brustschmerzen", "hinweis-angaben-ohne-gewaehr", "Alle Angaben ohne Gewähr"),
        Triple("erste-hilfe-durchfall-zink", "hinweis-angaben-ohne-gewaehr", "Alle Angaben ohne Gewähr"),
        // Nachgetragen 03.08.2026: Der Anaphylaxie-Tipp nennt Adrenalin-Mengen
        // und war beim Vorbehalts-Durchgang am 02.08. uebersehen worden.
        Triple("erste-hilfe-allergischer-schock", "hinweis-angaben-ohne-gewaehr", "Alle Angaben ohne Gewähr"),
        // Der Mond ist die zweite Wahl -- der Tipp sagt das und zeigt auf die
        // erste. Ohne den Verweis liest sich eine grobe Methode wie die beste.
        Triple("orientierung-mond", "orientierung-polarstern", "Nachts nach dem Polarstern gehen"),
        // Bei drei Gefahren gilt die gegenteilige Richtung. Der Tipp muss auf
        // beide Faelle zeigen, die er nur streift.
        Triple("taktisch-wohin-schutz", "taktisch-hochwasser", "Hochwasser: fünfzehn Zentimeter reichen"),
        Triple("taktisch-wohin-schutz", "erste-hilfe-strahlung", "Atomschlag und Strahlung: weggehen, ausziehen, waschen"),
        // Feldhygiene: Die Latrine braucht die Waschgelegenheit daneben, und
        // der Abfall haengt am selben Faden wie die Nagetiere.
        Triple("hygiene-latrine-bauen", "hygiene-haendewaschen", "Händewaschen, auch ohne Seife"),
        Triple("hygiene-latrine-bauen", "hygiene-ausscheidungen", "Stuhl entsorgen ohne Toilette"),
        Triple("hygiene-abfall-lager", "nahrung-vorraete-nager-abwehren", "Ratten und Mäuse von den Vorräten fernhalten"),
        // Die Beurteilung einer Wasserstelle fuehrt in vier Richtungen: zur
        // Latrine (Abstand), zum Meerwasser-Verbot und zu den beiden
        // Aufbereitungs-Tipps, damit niemand am Ende ohne Wasser dasteht.
        Triple("wasser-stelle-beurteilen", "hygiene-ausscheidungen", "Stuhl entsorgen ohne Toilette"),
        Triple("wasser-stelle-beurteilen", "wasser-falsche-fluessigkeiten", "Diese vier Flüssigkeiten machen den Durst schlimmer"),
        Triple("wasser-stelle-beurteilen", "wasser-truebes-wasser-vorbehandeln", "Trübes Wasser absetzen und filtern"),
        Triple("wasser-stelle-beurteilen", "wasser-abkochen", "Wasser abkochen"),
        // Trinkplan und Obergrenze gehoeren zusammen: Wer nur einen der
        // beiden Tipps liest, trinkt entweder zu wenig oder zu schnell.
        Triple("wasser-durst-kein-ratgeber", "wasser-zu-viel-trinken", "Zu viel auf einmal trinken kann töten"),
        Triple("wasser-durst-kein-ratgeber", "erste-hilfe-salzmangel", "Salzmangel: Muskeln verkrampfen nach dem Schwitzen"),
        Triple("wasser-durst-kein-ratgeber", "wasser-tagesbedarf", "Trinkwasser: Bedarf pro Tag"),
        // Warnung und Falschmeldung sind zwei Haelften derselben Frage:
        // amtliche Warnungen WEITERGEBEN, Geruechte NICHT.
        Triple("taktisch-sirene-verstehen", "taktisch-falschmeldungen", "Falschmeldungen erkennen: vier Fragen"),
        Triple("taktisch-sirene-verstehen", "taktisch-notgepaeck", "Notgepäck: zwanzig Dinge, und eine Mappe"),
        Triple("taktisch-falschmeldungen", "taktisch-sirene-verstehen", "Sirene: Heulton und Dauerton bedeuten das Gegenteil"),
        // Der Kinder-Tipp gehoert zu den beiden anderen Tipps ueber
        // Belastung -- einer fuer den Einzelnen, einer fuer die Gruppe.
        Triple("medizin-kindern-erklaeren", "medizin-belastungsreaktion", "Wenn jemand zusammenbricht: vier Dinge, die helfen"),
        Triple("medizin-kindern-erklaeren", "taktisch-gruppe-tragen", "Zusammen durchkommen: reden hilft messbar"),
        Triple("taktisch-vorsorge-hilfsmittel", "taktisch-notgepaeck", "Notgepäck: zwanzig Dinge, und eine Mappe"),
        Triple("taktisch-vorsorge-hilfsmittel", "medizin-bluthochdruck-ohne-mittel", "Hochdruck: was ohne Tabletten hilft"),
        // Der Brand-Ablauf fuehrt zu den drei Tipps, die das Danach
        // behandeln -- Rauch einatmen, jemanden herausholen, Brandwunde.
        Triple("erste-hilfe-brand-reihenfolge", "erste-hilfe-rauchvergiftung-erkennen", "Rauchvergiftung erkennen"),
        Triple("erste-hilfe-brand-reihenfolge", "erste-hilfe-brandrauch-nicht-hineingehen", "Rauch im Haus: nicht hineingehen"),
        Triple("erste-hilfe-brand-reihenfolge", "erste-hilfe-verbrennung-kuehlen", "Verbrennung: mindestens zehn Minuten kühlen"),
        // Die Zwei-Waende-Regel und der Richtungs-Tipp gehoeren zusammen:
        // Bei Explosion nach unten, bei Hochwasser nach oben.
        Triple("taktisch-zwei-waende", "taktisch-wohin-schutz", "Hoch, tief oder raus: der Fehler ist die falsche Richtung"),
        Triple("taktisch-dokumente-sichern", "taktisch-notgepaeck", "Notgepäck: zwanzig Dinge, und eine Mappe"),
        // Kochen und Heizen ohne Strom sind zwei verschiedene Geraetelisten
        // mit derselben Gefahr dahinter. Beide muessen zum CO-Melder fuehren.
        Triple("taktisch-kochen-ohne-strom", "erste-hilfe-kohlenmonoxid-melder", "Warnmelder für Kohlenmonoxid anbringen"),
        Triple("taktisch-kochen-ohne-strom", "erste-hilfe-kohlenmonoxid-erkennen", "Kohlenmonoxid: unsichtbar und geruchlos"),
        Triple("taktisch-kochen-ohne-strom", "taktisch-heizung-faellt-aus", "Wohnung wird kalt: womit du drinnen heizen darfst"),
        // Der gewoehnliche Stich fuehrt zu den beiden Faellen, in denen er
        // ein Notfall wird: Allergie und Stich im Rachen.
        Triple("erste-hilfe-bienen-wespenstich", "erste-hilfe-allergischer-schock", "Allergischer Schock (Anaphylaxie)"),
        Triple("erste-hilfe-bienen-wespenstich", "erste-hilfe-insektenstich-mund", "Insektenstich im Mund oder Rachen"),
        // Die beiden Vogel-Tipps zeigen aufeinander: Wer Eier findet, soll
        // wissen, dass der Vogel selbst essbar ist -- und umgekehrt.
        Triple("nahrung-voegel-eier", "nahrung-voegel-fangen", "Vögel: alle essbar, und wo man sie erwischt"),
        Triple("nahrung-voegel-fangen", "nahrung-voegel-eier", "Eier aus dem Nest: die Nahrung, die nachwächst"),
        // Die drei Tipps zum Essen und Trinken nach einem Atomschlag beginnen
        // alle mit dem Verweis auf den Sofortschutz. Faellt der weg, faengt
        // jemand mit dem Kochen an, bevor er sich gewaschen hat.
        Triple("wasser-nach-atomschlag", "erste-hilfe-strahlung", "Atomschlag und Strahlung: weggehen, ausziehen, waschen"),
        Triple("nahrung-atomschlag-tiere", "erste-hilfe-strahlung", "Atomschlag und Strahlung: weggehen, ausziehen, waschen"),
        Triple("nahrung-atomschlag-pflanzen", "erste-hilfe-strahlung", "Atomschlag und Strahlung: weggehen, ausziehen, waschen"),
        // Kein Verfahren gegen radioaktive Teilchen toetet Keime -- deshalb
        // muss der Weg zum Abkochen von dort aus offen bleiben.
        Triple("wasser-nach-atomschlag", "wasser-abkochen", "Wasser abkochen"),
        Triple("wasser-nach-atomschlag", "wasser-schnee-eis", "Schnee und Eis: erst schmelzen, dann trinken"),
        Triple("wasser-nach-atomschlag", "wasser-zu-viel-trinken", "Zu viel auf einmal trinken kann töten"),
        // Eier sind nach der Quelle selbst dann sicher, wenn sie waehrend des
        // Niederschlags gelegt wurden -- der Weg dorthin steht beim Nest.
        Triple("nahrung-atomschlag-tiere", "nahrung-voegel-eier", "Eier aus dem Nest: die Nahrung, die nachwächst"),
        // Die beiden Tipps zum Bergen von Toten trennen bewusst, was innerlich
        // passiert, von der Einteilung der Arbeit -- und zeigen aufeinander.
        Triple("medizin-tote-bergen-was-kommt", "medizin-tote-bergen-einteilen", "Tote bergen: wie man die Arbeit einteilt"),
        Triple("medizin-tote-bergen-einteilen", "medizin-tote-bergen-was-kommt", "Tote bergen: was auf einen zukommt"),
        Triple("medizin-tote-bergen-was-kommt", "medizin-belastungsreaktion", "Wenn jemand zusammenbricht: vier Dinge, die helfen"),
        // Der Grabplatz ist eine Uebertragung dieses Pakets aus der
        // Latrinen-Regel -- faellt die weg, steht die Zahl ohne Grundlage da.
        Triple("medizin-tote-bergen-einteilen", "hygiene-ausscheidungen", "Stuhl entsorgen ohne Toilette"),
        // Die starke Regelblutung fuehrt zu den beiden Faellen, die anders
        // behandelt werden: Schwangerschaft ausserhalb der Gebaermutter und
        // Infektion.
        Triple("medizin-regelblutung-stark", "medizin-eileiterschwangerschaft", "Eileiterschwangerschaft: der Notfall mit wenig Blut"),
        Triple("medizin-regelblutung-stark", "medizin-antibiotika-wann-keines", "Antibiotika: wann keines gegeben wird"),
        // Seit dem 03.08.2026 nennt der Tipp eine Arzneimenge. Damit haengt der
        // Vorbehalt daran -- und die Warnung, woran man die Magenblutung
        // erkennt, die dieses Mittel selbst ausloesen kann.
        Triple("medizin-regelblutung-stark", "hinweis-angaben-ohne-gewaehr", "Alle Angaben ohne Gewähr"),
        Triple("medizin-regelblutung-stark", "medizin-magengeschwuer", "Schwarzer, teeriger Stuhl ist ein Notfall"),
        // Eiseinbruch, 03.08.2026: Die beiden neuen Tipps hoeren da auf, wo die
        // Unterkuehlung anfaengt, und verweisen aufeinander. Der Rettungs-Tipp
        // nennt ausserdem die Reihenfolge beim Ertrinken, die dort anders ist.
        Triple("erste-hilfe-eiseinbruch-selbstrettung", "erste-hilfe-unterkuehlung-stadium-eins", "Unterkühlung Stadium I: Person zittert noch"),
        Triple("erste-hilfe-eiseinbruch-selbstrettung", "erste-hilfe-unterkuehlung-stadium-zwei", "Unterkühlung Stadium II: nur den Oberkörper wärmen"),
        Triple("erste-hilfe-eiseinbruch-retten", "erste-hilfe-unterkuehlung-stadium-zwei", "Unterkühlung Stadium II: nur den Oberkörper wärmen"),
        Triple("erste-hilfe-eiseinbruch-retten", "erste-hilfe-ertrinken", "Ertrinken: nicht hinterherspringen"),
        Triple("erste-hilfe-eiseinbruch-retten", "erste-hilfe-eiseinbruch-selbstrettung", "Eingebrochen: erst die Atmung, dann heraus"),
        // Verhuetung, 03.08.2026: Der Tipp haengt an zwei vorhandenen -- der
        // unregelmaessige Zyklus macht die Kalenderart unbrauchbar, und das
        // volle Stillen ist Bedingung fuer die Wirksamkeit.
        Triple("medizin-verhuetung-ohne-nachschub", "medizin-regelblutung-stark", "Starke Regelblutung: was normal ist und was nicht"),
        Triple("medizin-verhuetung-ohne-nachschub", "medizin-stillen-genug", "Stillen: bekommt das Kind genug?"),
        // Lawine, 03.08.2026: Die beiden Tipps verweisen aufeinander -- wer
        // sucht, und wer selbst erfasst wird. Der Rettungs-Tipp hoert bei der
        // Unterkuehlung auf und verweist dorthin.
        Triple("erste-hilfe-lawine-verschuettet", "erste-hilfe-lawine-selbst-erfasst", "Wenn die Lawine dich erfasst: die letzten Sekunden"),
        Triple("erste-hilfe-lawine-verschuettet", "erste-hilfe-unterkuehlung-stadium-zwei", "Unterkühlung Stadium II: nur den Oberkörper wärmen"),
        Triple("erste-hilfe-lawine-selbst-erfasst", "erste-hilfe-lawine-verschuettet", "Lawine: jemand ist verschüttet"),
        // Regen vom Dach, 03.08.2026: Der Tipp deckt das AUFFANGEN und verweist
        // fuer die Entkeimung auf die vorhandenen Aufbereitungs-Tipps, statt sie
        // zu wiederholen.
        Triple("wasser-regen-vom-dach", "wasser-abkochen", "Wasser abkochen"),
        Triple("wasser-regen-vom-dach", "wasser-chlor-entkeimung", "Trinkwasser mit Chlor entkeimen"),
        Triple("wasser-regen-vom-dach", "wasser-solare-entkeimung-sodis", "Wasser mit Sonnenlicht entkeimen (SODIS)"),
        Triple("wasser-regen-vom-dach", "wasser-truebes-wasser-vorbehandeln", "Trübes Wasser absetzen und filtern"),
        // Epilepsie, 03.08.2026: Der Tipp behandelt die Dauerbehandlung und
        // verweist fuer den Anfall selbst auf die beiden Akut-Tipps.
        Triple("medizin-epilepsie-mittel-gehen-aus", "erste-hilfe-krampfanfall", "Krampfanfall: nichts festhalten, nichts in den Mund"),
        Triple("medizin-epilepsie-mittel-gehen-aus", "erste-hilfe-krampfanfall-dringend", "Krampfanfall: wann es dringend wird"),
        // Kerzen ziehen, 03.08.2026: haengt an drei vorhandenen Tipps -- das
        // Auslassen des Fettes, die Loeschregel fuer Fett und Wachs, und die
        // Nager, fuer die Talg Futter ist.
        Triple("taktisch-licht-kerzen-ziehen", "hygiene-seife-selbst", "Seife selbst machen aus Fett und Asche"),
        Triple("taktisch-licht-kerzen-ziehen", "erste-hilfe-brand-reihenfolge", "Es brennt: die Reihenfolge, in der gehandelt wird"),
        Triple("taktisch-licht-kerzen-ziehen", "nahrung-vorraete-nager-abwehren", "Ratten und Mäuse von den Vorräten fernhalten"),
        // Monatshygiene, 03.08.2026: verweist auf die Seifenherstellung und auf
        // den medizinischen Teil, den es laengst gibt.
        Triple("medizin-monatshygiene-ohne-vorrat", "hygiene-seife-selbst", "Seife selbst machen aus Fett und Asche"),
        Triple("medizin-monatshygiene-ohne-vorrat", "medizin-regelblutung-stark", "Starke Regelblutung: was normal ist und was nicht"),
        // Abgelaufene Medikamente, 03.08.2026: haengt an vier vorhandenen --
        // dem Vorbehalt, den Antibiotika-Regeln, dem Insulin und der Epilepsie.
        Triple("medizin-abgelaufene-medikamente", "hinweis-angaben-ohne-gewaehr", "Alle Angaben ohne Gewähr"),
        Triple("medizin-abgelaufene-medikamente", "medizin-antibiotika-wann-keines", "Antibiotika: wann keines gegeben wird"),
        Triple("medizin-abgelaufene-medikamente", "medizin-insulin-lagern", "Insulin ohne Kühlung lagern"),
        Triple("medizin-abgelaufene-medikamente", "medizin-epilepsie-mittel-gehen-aus", "Epilepsie: wenn die Anfallsmittel knapp werden"),
        // Sturm, 03.08.2026: verweist auf das Radio im Notgepaeck und auf die
        // herabgerissene Leitung nach dem Sturm.
        Triple("taktisch-sturm-verhalten", "taktisch-notgepaeck", "Notgepäck: zwanzig Dinge, und eine Mappe"),
        Triple("taktisch-sturm-verhalten", "erste-hilfe-stromunfall", "Stromunfall: erst abschalten, dann anfassen"),
        // Sterben begleiten, 03.08.2026: schliesst an das Bergen der Toten an
        // und verweist fuer die Schmerzen auf den vorhandenen Tipp.
        Triple("medizin-sterben-begleiten", "medizin-tote-bergen-was-kommt", "Tote bergen: was auf einen zukommt"),
        Triple("medizin-sterben-begleiten", "medizin-schmerzen-ohne-arzt", "Schmerz behandeln, wenn es keine Tabletten gibt"),
        // Psychopharmaka, 03.08.2026: haengt am Vorbehalt und an den beiden
        // Tipps der Gruppe Seele und Belastung.
        Triple("medizin-psychopharmaka-gehen-aus", "hinweis-angaben-ohne-gewaehr", "Alle Angaben ohne Gewähr"),
        Triple("medizin-psychopharmaka-gehen-aus", "medizin-kopf-in-der-krise", "Was der Kopf in einer langen Krise tut"),
        Triple("medizin-psychopharmaka-gehen-aus", "medizin-belastungsreaktion", "Wenn jemand zusammenbricht: vier Dinge, die helfen"),
        // Nachtrag: Der Lithium-Teil warnt vor entzuendungshemmenden
        // Schmerzmitteln und nennt dabei den Regelblutungs-Tipp, der eine
        // Ibuprofen-Menge enthaelt.
        Triple("medizin-psychopharmaka-gehen-aus", "medizin-regelblutung-stark", "Starke Regelblutung: was normal ist und was nicht"),
        // Baum faellen, 03.08.2026: verweist auf die Absprache, bevor jemand
        // allein losgeht.
        Triple("taktisch-baum-faellen", "taktisch-weggehen-absprache", "Wenn einer weggeht: fünf Fragen vorher"),
        // Die beiden Vorratslisten fuehren zu den Tipps, die sagen, was mit den
        // aufgelisteten Dingen zu tun ist -- eine Liste ohne das ist nur Einkauf.
        Triple("taktisch-hausapotheke", "erste-hilfe-trinkloesung-selbst-ansetzen", "Trinklösung selbst ansetzen"),
        Triple("taktisch-hausapotheke", "erste-hilfe-verbrennung-kuehlen", "Verbrennung: mindestens zehn Minuten kühlen"),
        Triple("taktisch-hausapotheke", "erste-hilfe-wunde-bedecken", "Wunde bedecken"),
        Triple("taktisch-hausapotheke", "taktisch-vorsorge-hilfsmittel", "Wer auf Hilfsmittel angewiesen ist: die eigene Vorsorge"),
        Triple("taktisch-hausapotheke", "taktisch-notgepaeck", "Notgepäck: zwanzig Dinge, und eine Mappe"),
        Triple("taktisch-vorrat-haushalt", "wasser-vorratsdauer", "Trinkwasser: Vorrat für zehn Tage"),
        Triple("taktisch-vorrat-haushalt", "taktisch-kochen-ohne-strom", "Kochen ohne Strom: drinnen oder nur draußen"),
        Triple("taktisch-vorrat-haushalt", "taktisch-heizung-faellt-aus", "Wohnung wird kalt: womit du drinnen heizen darfst"),
        Triple("taktisch-vorrat-haushalt", "taktisch-warm-anziehen", "Sechs Regeln, damit Kleidung wärmt"),
        Triple("taktisch-vorrat-haushalt", "taktisch-hausapotheke", "Hausapotheke: die vollständige Liste"),
        Triple("taktisch-vorrat-haushalt", "taktisch-ausfall-folgen", "Was der Ausfall alles mitnimmt"),
        // Die Einteilung einer Gruppe endet bei dem, was sie zusammenhaelt, und
        // bei dem Signal, das sie herausholt.
        Triple("taktisch-gruppe-einteilen", "taktisch-gruppe-tragen", "Zusammen durchkommen: reden hilft messbar"),
        Triple("taktisch-gruppe-einteilen", "orientierung-signale-dreiergruppen", "Notsignale: alles in Dreiergruppen"),
        // Die Flussquerung endet bei nassen Fuessen -- und die sind bei Kaelte
        // die eigentliche Gefahr, nicht das Wasser selbst.
        Triple("gewaesser-durchqueren", "erste-hilfe-grabenfuss", "Grabenfuß: nasse Füße zerstören ohne Frost"),
        // Verweise, die aus einer Bauanleitung herausfuehren. Bis zum
        // 02.08.2026 konnte dieser Test sie gar nicht ausdruecken, weil er nur
        // Tipps kannte -- sie standen also voellig ungeprueft da.
        Triple("notunterstand-schnee", "erste-hilfe-kohlenmonoxid-erkennen", "Kohlenmonoxid: unsichtbar und geruchlos"),
        Triple("notunterstand-schnee", "notunterstand-lean-to", "Notunterkunft aus Naturmaterial: der Lean-to"),
        Triple("notunterstand-schnee", "taktisch-gruppe-einteilen", "Eine Gruppe einteilen: Aufgaben, Wache, Buch"),
        Triple("wasser-nach-atomschlag", "wasser-schnee-eis", "Schnee und Eis: erst schmelzen, dann trinken"),
        Triple("werkzeug-klinge-behelfsmaessig", "seilwerk-grundknoten", "Knoten und Lashings fürs Überleben: die Grundknoten"),
        Triple("nahrung-voegel-fangen", "fallen-stellen", "Fallen stellen"),
        Triple("nahrung-voegel-fangen", "beute-versorgen", "Beute versorgen und haltbar machen"),
        // Die Rangfolge der Tiergefahr fuehrt zu allen vier Einzelfaellen; ohne
        // sie waere sie nur eine Statistik.
        Triple("erste-hilfe-tiere-rangfolge", "erste-hilfe-bienen-wespenstich", "Bienen- und Wespenstich: die erste Minute zählt"),
        Triple("erste-hilfe-tiere-rangfolge", "erste-hilfe-allergischer-schock", "Allergischer Schock (Anaphylaxie)"),
        Triple("erste-hilfe-tiere-rangfolge", "erste-hilfe-schlangenbiss", "Schlangenbiss: ruhig halten, nichts aussaugen"),
        Triple("erste-hilfe-tiere-rangfolge", "erste-hilfe-zecke-entfernen", "Zeckenstich: Zecke sofort entfernen"),
        // Die Irrtuemer und der Nicht-Nahrungs-Nutzen von Pflanzen fuehren
        // beide zurueck auf die Bestimmung der Art -- ohne die taugt keines von
        // beidem.
        Triple("nahrung-giftpflanzen-irrtuemer", "nahrung-pflanzen-meiden", "Acht Zeichen, bei denen du gar nicht erst probierst"),
        Triple("nahrung-giftpflanzen-irrtuemer", "nahrung-giftpflanzen-namentlich", "Giftpflanzen mit Namen — und die Verwechslungen"),
        Triple("nahrung-pflanzen-ohne-essen", "erste-hilfe-kontaktgift", "Kontaktgifte auf der Haut"),
        Triple("nahrung-pflanzen-ohne-essen", "erste-hilfe-kohlenmonoxid-erkennen", "Kohlenmonoxid: unsichtbar und geruchlos"),
        Triple("nahrung-pflanzen-ohne-essen", "seilwerk-grundknoten", "Knoten und Lashings fürs Überleben: die Grundknoten"),
        Triple("nahrung-pflanzen-ohne-essen", "feuer-ohne-zuendmittel", "Zünden ohne Streichhölzer: Zunder und sechs Wege"),
        // Heilpflanzen fuehren zurueck auf das, was sie NICHT ersetzen:
        // Blutstillung, Fluessigkeitsersatz und die Bestimmung der Art.
        Triple("medizin-heilpflanzen-grenzen", "erste-hilfe-starke-blutung", "Blutung stillen"),
        Triple("medizin-heilpflanzen-grenzen", "erste-hilfe-durchfall-trinken", "Durchfall: mehr trinken"),
        Triple("medizin-heilpflanzen-grenzen", "nahrung-giftpflanzen-namentlich", "Giftpflanzen mit Namen — und die Verwechslungen"),
        Triple("medizin-heilpflanzen-grenzen", "nahrung-giftpflanzen-irrtuemer", "Drei Sätze über Giftpflanzen, die falsch sind"),
        // Kochen ohne Topf endet bei der Frage, WO gekocht werden darf; die
        // Haut endet bei der Frage, wie Kleidung ueberhaupt waermt.
        Triple("nahrung-kochen-ohne-topf", "taktisch-kochen-ohne-strom", "Kochen ohne Strom: drinnen oder nur draußen"),
        Triple("nahrung-haut-verwerten", "beute-versorgen", "Beute versorgen und haltbar machen"),
        Triple("nahrung-haut-verwerten", "taktisch-warm-anziehen", "Sechs Regeln, damit Kleidung wärmt"),
        // Die Schnur-Anleitung fuehrt zu den Pflanzen, die Fasern geben, und zu
        // den Bindungen, fuer die die Schnur ueberhaupt gemacht wird.
        Triple("seilwerk-schnur-selbst", "nahrung-pflanzen-ohne-essen", "Schnur, Zunder, Dämmung aus dem, was wächst"),
        Triple("seilwerk-schnur-selbst", "seilwerk-grundknoten", "Knoten und Lashings fürs Überleben: die Grundknoten"),
        // Permanganat teilt die Grenze mit allen chemischen Mitteln — der
        // Verweis dorthin ist der sicherheitsrelevante Teil des Tipps.
        Triple("wasser-kaliumpermanganat", "wasser-abkochen", "Wasser abkochen"),
        Triple("wasser-kaliumpermanganat", "wasser-chlor-entkeimung", "Trinkwasser mit Chlor entkeimen"),
        Triple("wasser-kaliumpermanganat", "wasser-truebes-wasser-vorbehandeln", "Trübes Wasser absetzen und filtern"),
        // Der Pflanzensaft haengt an derselben Bedingung wie das Essen von
        // Wildpflanzen: die Art sicher erkennen.
        Triple("wasser-pflanzensaft", "nahrung-pflanzen-meiden", "Acht Zeichen, bei denen du gar nicht erst probierst"),
        Triple("wasser-pflanzensaft", "wasser-pflanzenbeutel", "Beutel um einen belaubten Ast: Trinkbares aus Blättern"),
        // Die Bodenzeichen ergaenzen die beiden vorhandenen Signal-Tipps; ohne
        // sie waere das Zeichen zwar sichtbar, aber ohne Aussage.
        Triple("orientierung-bodenzeichen", "orientierung-signale-dreiergruppen", "Notsignale: alles in Dreiergruppen"),
        Triple("orientierung-bodenzeichen", "orientierung-signale-spiegel", "Spiegel: das stärkste Signal bei Sonne"),
        // Die Begegnung mit Fremden endet dort, wo sie kippt — dann gilt nicht
        // mehr Hoeflichkeit, sondern Deeskalation und die Grenze zum Gehen.
        Triple("taktisch-fremden-begegnen", "taktisch-deeskalation", "Deeskalation: seitlich stehen, leise reden"),
        Triple("taktisch-fremden-begegnen", "taktisch-abbrechen", "Abbrechen ist erlaubt"),
        // Die sechs Reaktionen fuehren zu dem, was man dagegen TUT — der Tipp
        // selbst erklaert nur, was passiert.
        Triple("medizin-kopf-in-der-krise", "medizin-belastungsreaktion", "Wenn jemand zusammenbricht: vier Dinge, die helfen"),
        Triple("medizin-kopf-in-der-krise", "medizin-angst-beruhigen", "Angst und Panik: drei Übungen ohne Mittel"),
        Triple("medizin-kopf-in-der-krise", "taktisch-gruppe-tragen", "Zusammen durchkommen: reden hilft messbar"),
        // Die beiden Pruefungen am Tier gehoeren zusammen: Die erste findet nur
        // das Offensichtliche, die zweite das Innere.
        Triple("nahrung-tier-vor-dem-schlachten", "nahrung-tier-nach-dem-schlachten", "Nach dem Schlachten: was den ganzen Körper verwirft"),
        Triple("nahrung-tier-nach-dem-schlachten", "nahrung-tier-vor-dem-schlachten", "Ist das Tier gesund? Die Prüfung vor dem Schlachten"),
        Triple("nahrung-tier-nach-dem-schlachten", "beute-versorgen", "Beute versorgen und haltbar machen"),
        // Die langsamen Konservierverfahren grenzen sich ausdruecklich von den
        // schnellen ab -- ohne diesen Verweis liest man zwei Anleitungen zum
        // selben Thema und haelt eine davon fuer ueberfluessig.
        Triple("nahrung-fleisch-monate", "beute-versorgen", "Beute versorgen und haltbar machen"),
        Triple("nahrung-fleisch-monate", "nahrung-tier-nach-dem-schlachten", "Nach dem Schlachten: was den ganzen Körper verwirft"),
        // Die Beute-Anleitung deckt jetzt auch Nutztiere ab und fuehrt zu den
        // beiden Pruefungen und zu den langsamen Konservierverfahren.
        Triple("beute-versorgen", "nahrung-tier-vor-dem-schlachten", "Ist das Tier gesund? Die Prüfung vor dem Schlachten"),
        Triple("beute-versorgen", "nahrung-tier-nach-dem-schlachten", "Nach dem Schlachten: was den ganzen Körper verwirft"),
        Triple("beute-versorgen", "nahrung-fleisch-monate", "Vorrat für Monate: pökeln, räuchern, dörren"),
        // Die Fuenf-Fragen-Absprache haengt an der Gruppeneinteilung und an der
        // Frage, was bei einer Begegnung gilt.
        Triple("taktisch-weggehen-absprache", "taktisch-gruppe-einteilen", "Eine Gruppe einteilen: Aufgaben, Wache, Buch"),
        Triple("taktisch-weggehen-absprache", "taktisch-fremden-begegnen", "Fremden begegnen: einer nach dem anderen"),
        // Vitamin C zerstoert Chlor und Jod. Der Verweis auf die Wartezeit ist
        // deshalb der sicherheitsrelevante Teil dieser Ergaenzung.
        Triple("wasser-geschmack-belueften", "wasser-chlor-entkeimung", "Trinkwasser mit Chlor entkeimen"),
        // Die Sichtungs-Ordnung sagt, was vor und nach den Handgriffen kommt —
        // ohne den Verweis auf die Handgriffe bleibt sie Theorie.
        Triple("erste-hilfe-sichtung-ordnung", "erste-hilfe-mehrere-verletzte", "Mehrere Verletzte: erst alle ansprechen"),
        Triple("erste-hilfe-sichtung-ordnung", "erste-hilfe-notruf-112", "Notruf 112"),
        Triple("erste-hilfe-sichtung-ordnung", "taktisch-helfer-schuetzen", "Wer hilft, hält das nicht endlos aus"),
    )

    // Ein Verweis, der den Titel des gemeinten Tipps ausschreibt, ist der
    // hilfreichste -- und der zerbrechlichste: Wird der gemeinte Tipp
    // umbenannt, steht im verweisenden Text ein Titel, den es nicht mehr gibt,
    // und der Nutzer sucht im Ernstfall nach etwas, das die Suche nicht kennt.
    // Der Test oben faellt darauf nicht herein, denn die alte Wortgruppe steht
    // ja noch da. Dieser hier faellt darauf herein.
    @Test
    fun ausgeschriebeneTitelInVerweisenStimmenNochMitDemZielUeberein() {
        val alle = alleTitel()
        val titel = alle.values.toSet()
        for ((von, nach, wortgruppe) in verweise) {
            val zielTitel = alle[nach] ?: continue
            // Nur Paare pruefen, deren Wortgruppe ein Titel sein will. Das sind
            // alle, die genau so heissen wie irgendein Tipp -- unabhaengig von
            // der Zeichensetzung. (Vorher wurde nur auf Doppelpunkt und
            // Gedankenstrich geprueft; Titel ohne beides fielen durchs Raster.)
            if (wortgruppe !in titel) continue
            assertTrue(
                zielTitel == wortgruppe,
                "$von schreibt den Titel \"$wortgruppe\" aus, aber $nach heisst inzwischen " +
                    "\"$zielTitel\" — der Verweis nennt einen Eintrag, den es nicht mehr gibt",
            )
        }
    }

    @Test
    fun jederQuerverweisFuehrtZuEinemVorhandenenTipp() {
        val alle = alleTitel()
        val texte = verweistexte()
        for ((von, nach, wortgruppe) in verweise) {
            val quelltext = texte[von]
            assertTrue(quelltext != null, "der verweisende Eintrag $von fehlt")
            assertTrue(
                nach in alle,
                "$von verweist auf $nach — dieser Eintrag fehlt, der Verweis zeigt ins Leere",
            )
            assertTrue(
                quelltext.contains(wortgruppe),
                "in $von steht die Verweisstelle \"$wortgruppe\" nicht mehr — Paar veraltet",
            )
        }
    }

    // Die Kinder- und Saeuglings-Fassungen sind der Grund, warum die
    // Erwachsenen-Anleitungen ueberhaupt eine Grenze nennen. Verschwindet eine
    // davon, muss auch die Grenze im Erwachsenen-Tipp weg -- sonst verweist sie
    // auf nichts.
    @Test
    fun dieAltersfassungenSindVollstaendig() {
        val alle = tipps()
        for (kennung in listOf(
            "erste-hilfe-wiederbelebung-kind",
            "erste-hilfe-ersticken-kind",
            "erste-hilfe-ersticken-saeugling",
        )) {
            assertTrue(kennung in alle, "die Altersfassung $kennung fehlt")
        }
        // Und die Erwachsenen-Anleitungen muessen ihre Grenze nennen.
        val ohneUebung = alle.getValue("erste-hilfe-nur-druecken")
        assertTrue(ohneUebung.body.contains("Kind"), "die Regel fuer Ungeuebte nennt die Kinder-Ausnahme nicht")
        val oberbauch = alle.getValue("erste-hilfe-ersticken-oberbauchkompression")
        assertTrue(
            oberbauch.body.contains("Säuglingen") || oberbauch.body.contains("Säugling"),
            "die Oberbauchkompression nennt die Saeuglings-Ausnahme nicht",
        )
        // Die Altersgrenze selbst muss dastehen: Wer nicht weiss, ab wann ein
        // Kind kein Saeugling mehr ist, kann die richtige Fassung nicht waehlen.
        for (kennung in listOf("erste-hilfe-ersticken-saeugling", "erste-hilfe-wiederbelebung-kind")) {
            assertTrue(
                alle.getValue(kennung).body.contains("unter einem Jahr"),
                "$kennung nennt die Altersgrenze nicht",
            )
        }
        // Und die Drucktechnik fuer Kinder muss ihre eigene Tiefe nennen -- die
        // Erwachsenen-Angabe waere fuer ein Kind falsch.
        val kindDruck = alle.getValue("erste-hilfe-herzdruckmassage-kind")
        assertTrue(kindDruck.body.contains("Drittel"), "die Drucktiefe fuer Kinder fehlt")
        assertTrue(kindDruck.body.contains("Zwei-Daumen"), "die Technik fuer Saeuglinge fehlt")
    }

    // Die Handliste oben ist gepflegt, und genau das ist ihre Schwaeche: Sie
    // kennt nur die Paare, die jemand eingetragen hat. Am 03.08.2026 waren das
    // rund 190 von 397 ausgeschriebenen Verweisen. Die uebrigen 200 waren
    // ungeprueft -- und VIER davon nannten Eintraege, die es nicht mehr gab:
    //   "Naehen oder offen lassen"                        (heisst jetzt "... : die Entscheidung")
    //   "Darmverschluss: Erbrechen, das nach Stuhl riecht" (heisst jetzt "... wenn das Erbrochene ...")
    //   "Essbarkeitstest: dreizehn Schritte"  (zweimal)    (heisst jetzt "... , ueber einen Tag")
    // Wer im Ernstfall nach dem genannten Titel sucht, findet nichts.
    //
    // Diese Pruefung braucht keine Pflege: Sie sammelt jeden ausgeschriebenen
    // Verweis selbst ein.
    // Ein Kapitel verweist auch auf SEINE EIGENEN Abschnitte ("wie im Abschnitt
    // ... beschrieben"). Die tragen keinen eigenen Eintrag, sind fuer den Leser
    // aber genauso ein Sprungziel. Deshalb zaehlen hier zusaetzlich alle
    // Abschnitts-Ueberschriften mit -- sonst meldet der Waechter fuenf
    // einwandfreie Verweise als tot (gemessen am 21.08.2026).
    //
    // Verglichen wird ohne Ruecksicht auf Gross- und Kleinschreibung: In einem
    // Absatz, der ganz in Grossbuchstaben steht, wird der zitierte Titel
    // mitgeschrien. Fuer die Suche ist das folgenlos, denn die zerlegt ohnehin
    // in Kleinbuchstaben.
    private fun alleSprungziele(pack: LoadedPack): Set<String> {
        val out = HashSet<String>()
        for (tip in pack.tips) out.add(tip.title.lowercase())
        for (guide in pack.guides) out.add(guide.title.lowercase())
        for (kapitel in pack.agriculture) {
            out.add(kapitel.title.lowercase())
            for (abschnitt in kapitel.sections) out.add(abschnitt.heading.lowercase())
        }
        return out
    }

    // Was in Anfuehrungszeichen steht und trotzdem kein Verweis ist. Die weite
    // Fassung in Verweiszitate greift auch bei Saetzen wie "die Quelle nennt
    // ..." -- gewollt, denn so wurde ein toter Verweis auf einen Titel
    // gefunden, den es nie gab. Der Preis sind diese neun Stellen, jede einzeln
    // nachgesehen und als echtes Zitat bestaetigt.
    private val keineVerweise = setOf(
        // Woertliche Rede aus einem Beispiel.
        "Halt einfach ein bisschen Ausschau",
        // Wortlaut aus dem Waffengesetz, keine Ueberschrift dieses Pakets.
        "Waffenerwerb, Waffenbesitz und Waffentragen",
        "Stahlruten, Totschläger oder Schlagringe",
        // Posten aus einer Materialliste, kein Sprungziel.
        "Holzleim oder Harz (optional)",
        // Platzhalter in einem Satz ueber Flaechenangaben.
        "X Quadratmeter pro Person",
        // Titel und Bezeichnungen aus den englischsprachigen Quellen.
        "Scotch one-horse coup cart",
        "Wood Fuel in Wartime",
        "Wheat Production in the Eastern United States",
        "Cost of Using Horses on Corn-Belt Farms",
        "Sieben und ähnliche Geräte, oder Worfeln",
    )

    @Test
    fun jedesVerweisZitatNenntEinenVorhandenenEintrag() {
        val pack = paketLaden()
        val titel = Verweiszitate.titelZuKennung(pack)
        val ziele = alleSprungziele(pack)
        val tote = ArrayList<String>()
        for ((kennung, text) in Verweiszitate.texteJeEintrag(pack)) {
            for ((_, zitiert) in Verweiszitate.finde(text)) {
                if (zitiert.lowercase() in ziele) continue
                if (zitiert in keineVerweise) continue
                // Beinahe-Treffer nennen: fast immer wurde das Ziel umbenannt.
                val naheliegend = titel.keys.filter {
                    it.startsWith(zitiert.take(20)) || zitiert.startsWith(it.take(20))
                }
                tote.add(
                    "$kennung schreibt den Verweis \"$zitiert\" aus — einen Eintrag mit diesem " +
                        "Titel gibt es nicht. " +
                        if (naheliegend.isEmpty()) {
                            "Es gibt auch keinen aehnlichen; entweder ist das Ziel weg oder der " +
                                "Satz meint gar keinen Verweis."
                        } else {
                            "Gemeint ist vermutlich: ${naheliegend.joinToString(" / ") { "\"$it\"" }}"
                        },
                )
            }
        }
        assertTrue(tote.isEmpty(), tote.joinToString("\n\n"))
    }

    // Und die Rangfolge dazu. Ein ausgeschriebener Titel traegt seine Woerter in
    // den zitierenden Eintrag; der taucht dadurch in Suchen auf, zu denen er
    // nichts zu sagen hat. Ertraeglich ist das, solange der Eintrag, DEM der
    // Titel gehoert, davor steht -- wer "Behelfstrage" sucht, muss die
    // Behelfstrage zuerst bekommen und nicht den Tipp, der sie nur erwaehnt.
    //
    // Am 03.08.2026 nachgemessen: ueber alle 397 Verweise und 701 geliehenen
    // Woerter gab es KEINEN einzigen Ueberholvorgang. Der Test haelt diesen
    // Stand fest.
    @Test
    fun keinZitatUeberholtDenEintragDemDerTitelGehoert() {
        val pack = paketLaden()
        val index = SearchIndex.build(pack)
        val titel = Verweiszitate.titelZuKennung(pack)
        val ueberholt = ArrayList<String>()
        for ((kennung, text) in Verweiszitate.texteJeEintrag(pack)) {
            val zitate = Verweiszitate.finde(text)
            if (zitate.isEmpty()) continue
            val eigene = Verweiszitate.eigeneWoerter(pack, kennung, text)
            for ((_, zitiert) in zitate) {
                val eigner = titel[zitiert] ?: continue
                for (wort in Verweiszitate.gelieheneWoerter(zitiert, eigene)) {
                    val liste = index.search(wort, limit = 8).map { it.id }
                    val rangZitierer = liste.indexOf(kennung)
                    if (rangZitierer < 0) continue
                    val rangEigner = liste.indexOf(eigner)
                    if (rangEigner in 0 until rangZitierer) continue
                    ueberholt.add(
                        "\"$wort\" stammt bei $kennung nur aus dem ausgeschriebenen Verweis " +
                            "\"$zitiert\" — trotzdem steht $kennung dort auf Rang " +
                            "${rangZitierer + 1}, und $eigner, dem der Titel gehoert, " +
                            (if (rangEigner < 0) "gar nicht in den ersten 8." else "erst auf Rang ${rangEigner + 1}.") +
                            " Entweder einen anderen Titel zitieren oder den Verweis ohne " +
                            "ausgeschriebenen Titel formulieren.",
                    )
                }
            }
        }
        assertTrue(ueberholt.isEmpty(), ueberholt.joinToString("\n\n"))
    }
    // Der Waechter oben sucht genau EIN Zeichenpaar: „ als Anfang und “ als
    // Ende. Steht am Schluss stattdessen das gerade Zeichen, findet er das
    // Zitat gar nicht -- kein Fehler, keine Warnung, gruener Lauf.
    //
    // Am 20.08.2026 nachgezaehlt: 31 Eintraege hatten so ein falsches
    // Schlusszeichen. Geprueft wurden dadurch 803 Verweise, vorhanden waren
    // 879. Von den 76 unsichtbaren zeigten ACHT ins Leere -- fuenf abgekuerzte
    // Titel und ein Thema, das gar kein Eintrag ist. Alle berichtigt.
    //
    // Diese Pruefung haelt den Stand fest. Sie sagt nichts ueber den Inhalt
    // eines Zitats -- nur darueber, dass der Waechter es ueberhaupt sehen
    // kann.
    @Test
    fun jedeAnfuehrungWirdRichtigGeschlossen() {
        val offen = ArrayList<String>()
        for ((kennung, text) in Verweiszitate.texteJeEintrag(paketLaden())) {
            val auf = text.count { it == Verweiszitate.AUF }
            val zu = text.count { it == Verweiszitate.ZU }
            if (auf == zu) continue
            offen.add(
                "$kennung oeffnet $auf Anfuehrungen und schliesst $zu davon " +
                    "richtig. Ein gerades Schlusszeichen macht den Verweis fuer " +
                    "die Pruefung oben unsichtbar -- er kann dann ins Leere " +
                    "zeigen, ohne dass es jemand merkt.",
            )
        }
        assertTrue(offen.isEmpty(), offen.joinToString("\n\n"))
    }
}
