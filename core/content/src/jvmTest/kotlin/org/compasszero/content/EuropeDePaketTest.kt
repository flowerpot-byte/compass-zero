package org.compasszero.content

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Prueft das erste echte Inhaltspaket (content/europe-de) genau so, wie es die
// App spaeter laden wuerde: gegen den echten Parser und alle Inhaltsregeln,
// nicht nur von Hand gegen die Doku gelesen.
class EuropeDePaketTest {

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
    // Die Bilder liegen als Dateien daneben und wandern beim Packen ins Paket.
    // Wird hier eine leere Liste uebergeben, meldet die Pruefung jedes
    // Skizzen-Bild als fehlend -- und ein tatsaechlich fehlendes Bild fiele
    // nicht mehr auf, weil ohnehin alle als fehlend gelten.
    private fun skizzennamen(paket: File): Set<String> =
        (File(paket, "assets").listFiles() ?: emptyArray())
            .filter { it.isFile }
            .map { "assets/${it.name}" }
            .toSet()

    @Test
    fun basispaketDeLaedtOhneProblem() {
        val paket = File(repoRoot(), "content/europe-de/paket")
        val manifestBytes = File(paket, "manifest.json").readBytes()
        val tipsBytes = File(paket, "content/tips.json").readBytes()
        val guidesBytes = File(paket, "content/guides.json").readBytes()
        val agricultureBytes = File(paket, "content/agriculture.json").readBytes()

        val result = PackParser.parse(
            mapOf(
                "manifest.json" to manifestBytes,
                "content/tips.json" to tipsBytes,
                "content/guides.json" to guidesBytes,
                "content/agriculture.json" to agricultureBytes,
                "content/terms.json" to File(paket, "content/terms.json").readBytes(),
            ),
            skizzennamen(paket),
        )

        // Das Basispaket laedt mit GENAU EINER Meldung, seit dem 21.08.2026:
        // der Vorwarnung, dass 90 % der erlaubten WORTVORKOMMEN erreicht sind.
        //
        // Diese Zeile ist bewusst so eng gefasst, dass sie nichts durchlaesst:
        // Erlaubt ist nur dieser eine Code, jede andere Meldung faellt weiterhin
        // auf. Der eigentliche Waechter bleibt SuchbudgetTest -- dort stehen die
        // harten Grenzen, und der Kommentar dieses Tests sagte schon vorher, dass
        // ein gerissenes Budget DORT auffallen soll und nicht hier.
        //
        // WAS DIE MELDUNG BEDEUTET, EHRLICH: Sie zaehlt die Groesse, die
        // ContentLimits selbst als die FALSCHE bezeichnet. Der Speicher haengt an
        // der Zahl der VERSCHIEDENEN Woerter, nicht an der Zahl der Vorkommen --
        // ein verschiedenes Wort kostet rund 130 Byte, ein weiteres Vorkommen
        // rund 17. Das Europa-Paket steht am 21.08.2026 bei rund 34 400
        // verschiedenen Woertern von 300 000 erlaubten (11 %) und bei rund
        // 540 000 Vorkommen von 600 000 (90 %). Zusammen sind das rund 4,5 MB
        // plus 9,2 MB, also knapp 14 MB von 96 MB Heap.
        //
        // WER DIE GRENZE ANHEBEN WILL, findet die Messanleitung in ContentLimits.
        // Dort steht auch, warum das nicht nebenbei passiert: Es ist eine
        // Aenderung an einer Schutzgrenze und gehoert entschieden.
        assertEquals(
            listOf("content-search-terms-near-limit"),
            result.problems.map { it.code },
            "Unerwartete Probleme: ${result.problems}",
        )
        val pack = result.pack ?: error("Paket wurde trotz leerer Problemliste nicht geladen")
        // Die Zahl ist eine Buchhaltungs-Schranke: Sie faellt auf, wenn ein Tipp
        // still verschwindet. Sie wird nur MIT einem bewusst hinzugefuegten Tipp
        // hochgezaehlt, nie um einen Fehler durchzulassen.
        // 347 -> 348 am 10.08.2026: "Aflatoxin: das Pilzgift, das man nicht
        // wegkochen kann" (nahrung-schimmelgetreide-aflatoxin).
        // 348 -> 349 am 10.08.2026: "Tage zaehlen: Strichliste, Mondlauf,
        // Sonnenwende" (orientierung-tage-zaehlen).
        // 349 -> 350 am 10.08.2026: "Trauer: der normale Verlauf und die
        // Zeichen, dass er stockt" (medizin-trauer).
        // 350 -> 351 am 10.08.2026: "Blutverduenner gehen aus"
        // (medizin-gerinnungshemmer-gehen-aus).
        // 351 -> 352 am 12.08.2026: "Entfernung schaetzen: die eigene Schrittzahl auf hundert Meter"
        // (orientierung-schritte-zaehlen).
        // 352 -> 353 am 12.08.2026: "Bewusst daneben zielen, damit man weiss, wohin man abbiegen muss"
        // (orientierung-bewusster-versatz).
        // 353 -> 354 am 12.08.2026: "Um ein Hindernis herum, ohne die Richtung zu verlieren"
        // (orientierung-hindernis-umgehen).
        // 354 -> 355 am 12.08.2026: "Wo bin ich? Den eigenen Standort aus zwei Sichtlinien finden"
        // (orientierung-kreuzpeilung).
        // 355 -> 356 am 12.08.2026: "Hoehenlinien lesen: Kuppe, Sattel, Tal und Grat auf der Karte erkennen"
        // (orientierung-hoehenlinien).
        // 356 -> 357 am 12.08.2026: "Wie weit ist das? Schaetzen, ohne hinzugehen"
        // (orientierung-entfernung-schaetzen).
        // 357 -> 358 am 12.08.2026: "Zielmarken: eine Richtung halten, ohne staendig den Kompass abzulesen"
        // (orientierung-zielmarken).
        // 358 -> 359 am 12.08.2026: "Giftpflanzen bei uns: vier Stufen, und welche wirklich toeten"
        // (nahrung-giftpflanzen-mitteleuropa).
        // 359 -> 360 am 12.08.2026: "Essbares Gruen mit Namen: Brennnessel, Loewenzahn, Klette und andere"
        // (nahrung-essbares-gruen-namentlich).
        // 360 -> 361 am 12.08.2026: "Der Knollenblaetterpilz und die truegerische Besserung"
        // (nahrung-knollenblaetterpilz).
        // 361 -> 362 am 12.08.2026: "Knopfzelle: solange sie in der Speiseroehre steckt, zaehlt jede Stunde"
        // (erste-hilfe-knopfzelle).
        // 362 -> 363 am 12.08.2026: "Masern: die Zeichen, die Absonderung und das Vitamin A"
        // (medizin-masern).
        // 363 -> 364 am 12.08.2026: "Keuchhusten: wochenlange Anfaelle, und warum Hustenmittel schaden"
        // (medizin-keuchhusten).
        // 364 -> 365 am 12.08.2026: "Typhus: schleichendes Fieber, und die Wendung, die toetet"
        // (medizin-typhus).
        // 365 -> 366 am 12.08.2026: "Hirnhautentzuendung: steifer Nacken, und der Fleck, der nicht weggeht"
        // (medizin-hirnhautentzuendung).
        // 366 -> 367 am 13.08.2026: "Schwer unterernaehrt: die ersten zwei Tage entscheiden"
        // (medizin-unterernaehrt-erste-tage).
        // 367 -> 368 am 13.08.2026: "Diphtherie und Kehldeckelentzuendung: nicht in den Rachen schauen"
        // (medizin-diphtherie-kehldeckel).
        // 368 -> 369 am 17.08.2026: "Sonnenstich: heisser Kopf, kuehler Koerper"
        // (erste-hilfe-sonnenstich).
        // 369 -> 370 am 17.08.2026: "Gedraenge: nicht die Fuesse toeten, sondern der Druck"
        // (taktisch-gedraenge).
        // 370 -> 371 am 17.08.2026: "Die ersten Sekunden: in welcher Reihenfolge man hinsieht"
        // (erste-hilfe-reihenfolge-erste-sekunden).
        // 371 -> 372 am 17.08.2026: "Allein und niemand hinter dir: der Stoss gegen die
        // Stuhllehne"
        // (erste-hilfe-ersticken-allein).
        // 372 -> 373 am 17.08.2026: "Guertelrose: nur eine Koerperhaelfte"
        // (medizin-guertelrose).
        // 373 -> 374 am 17.08.2026: "Windpocken: alle Stadien auf einmal, und wer
        // wegbleiben muss"
        // (medizin-windpocken).
        // 374 -> 375 am 17.08.2026: "Hexenschuss und Ischias: Bettruhe macht es schlimmer"
        // (erste-hilfe-hexenschuss).
        // 375 -> 376 am 17.08.2026: "Bucheckern: erst ueberbruehen, dann roesten"
        // (nahrung-bucheckern).
        // 376 -> 377 am 17.08.2026: "Giersch: drei mal drei, und wer ihm aehnlich sieht"
        // (nahrung-giersch).
        // 377 -> 378 am 17.08.2026: "Vogelmiere: eine einzige Haarreihe verraet sie"
        // (nahrung-vogelmiere).
        // 378 -> 379 am 17.08.2026: "Eicheln: der Brotbaum, aber erst nach zwei Tagen"
        // (nahrung-eicheln).
        // 379 -> 380 am 17.08.2026: "Gerstenkorn und Hagelkorn am Lid"
        // (medizin-gerstenkorn).
        // 380 -> 381 am 17.08.2026: "Nach dem Unglueck zurueck ins Gebaeude"
        // (taktisch-zurueck-ins-gebaeude).
        // 381 -> 382 am 17.08.2026: "Knoblauchsrauke: erst beim Zerreiben riecht sie"
        // (nahrung-knoblauchsrauke).
        // 382 -> 383 am 17.08.2026: "Unterkühlung bei den Kleinsten: das Zittern
        // fehlt"
        // (erste-hilfe-unterkuehlung-kind).
        // 383 -> 384 am 17.08.2026: "Die Kleinsten im Sommer: 1,5 bis 3 Liter am Tag"
        // (erste-hilfe-hitze-kleinste).
        // 384 -> 385 am 17.08.2026: "Eingewachsener Zehennagel: die Ecke muss heraus"
        // (medizin-eingewachsener-nagel).
        // 385 -> 386 am 17.08.2026: "Madenwurm: das Jucken kommt nachts"
        // (medizin-madenwurm).
        // 386 -> 387 am 17.08.2026: "Haut reagiert ploetzlich auf Sonne: erst ans Medikament denken"
        // (medizin-lichtempfindlichkeit-medikamente).
        // 387 -> 388 am 17.08.2026: "Nagelbettentzuendung: nicht in die Fingerkuppe schneiden"
        // (medizin-nagelbettentzuendung).
        // 388 -> 389 am 17.08.2026: "Fremdkoerper im Ohr: der erste Versuch ist der beste"
        // (medizin-fremdkoerper-ohr).
        // 389 -> 390 am 18.08.2026: "Schlaf in der Krise: aufstehen, wenn es nicht geht"
        // (medizin-schlaf-in-der-krise).
        // 390 -> 391 am 18.08.2026: "Schlag aufs Auge: wann es mehr ist als ein Veilchen"
        // (medizin-schlag-aufs-auge).
        // 391 -> 392 am 18.08.2026: "Verstauchung und Zerrung: kuehlen, hochlegen, frueh wieder bewegen"
        // (erste-hilfe-verstauchung-zerrung).
        // 392 -> 394 am 19.08.2026: "Wie trueb ist zu trueb? Die Muenze im Eimer"
        // (wasser-truebung-messen-muenze) und "Brunnen nach Hochwasser: reinigen,
        // entkeimen, leerpumpen" (wasser-brunnen-nach-hochwasser).
        // 394 -> 397 am 19.08.2026: "Freies Chlor: der Beweis, dass das Entkeimen
        // gewirkt hat" (wasser-freies-chlor-messen), "Wenn die Leitung wieder
        // laeuft: das erste Wasser ist verdaechtig" (wasser-leitung-nach-ausfall)
        // und "Filter: was welcher kann, und was keiner kann" (wasser-filterarten).
        // 397 -> 398 am 19.08.2026: "Kontrollpunkt: langsam werden, Haende
        // sichtbar, nichts anbieten" (taktisch-kontrollpunkt).
        // 398 -> 399 am 19.08.2026: "Blindgaenger und Fundmunition: anfassen
        // ist der Fehler" (taktisch-blindgaenger).
        // 399 -> 400 am 19.08.2026: "Minen: die fuenf Gegenden, in denen sie
        // liegen" (taktisch-minen-gegenden).
        // 400 -> 401 am 19.08.2026: "Beschuss: warum der erste Einschlag
        // nichts beweist" (taktisch-beschuss-verhalten).
        // 401 -> 403 am 19.08.2026: "Marschtempo: drei Kilometer in der
        // Stunde, bergauf zwei" (orientierung-marschtempo) und "Nachts gehen:
        // vierzig Minuten fuer die Augen" (orientierung-nachts-gehen).
        // 403 -> 405 am 19.08.2026: "Auto im Wasser: das Fenster ist der
        // Ausgang, nicht die Tuer" (taktisch-auto-im-wasser) und "Bremsen weg:
        // herunterschalten, Handbremse in Stoessen" (taktisch-bremsen-versagen).
        // 405 -> 406 am 19.08.2026: "Wolken lesen: was in den naechsten
        // Stunden kommt" (taktisch-wolken-lesen).
        // 406 -> 407 am 19.08.2026: "Hinterhalt: die Frage vorher ist
        // wichtiger als jede Reaktion" (taktisch-hinterhalt).
        // 407 -> 409 am 19.08.2026: "Giftwolke: gegen den Wind, nicht mit ihm"
        // (taktisch-giftwolke) und "Abgedichteter Raum: bei Chemie nach oben,
        // bei Strahlung nach unten" (taktisch-abgedichteter-raum).
        // 409 -> 410 am 19.08.2026: "Im kalten Wasser treiben: anziehen
        // lassen, klein machen" (erste-hilfe-kaltes-wasser-treiben).
        // 410 -> 411 am 19.08.2026: "Verlaufen: stehen bleiben, bevor du
        // weitergehst" (orientierung-verlaufen).
        // 411 -> 412 am 19.08.2026: "Zeichen am Himmel: Abendrot, Talnebel,
        // klare Nacht" (taktisch-himmelszeichen).
        // 412 -> 413 am 19.08.2026: "Wo die Nacht verbracht wird: Talgrund
        // oder oben bleiben" -- "wo uebernachten" fand vorher NICHTS, und
        // "unterschlupf" fuehrte auf die Tollwut.
        // 413 -> 414 am 19.08.2026: "Zu mehreren unterwegs: getrennt wird
        // der Vorderste" -- "gruppe zusammenhalten", "wer geht vorne",
        // "kundschafter" und "marschordnung" fanden vorher nichts Passendes.
        // 414 -> 415 am 19.08.2026: "Einem Bach abwaerts folgen: der Weg
        // zu Menschen" -- "fluss folgen", "flussabwaerts" und "zivilisation
        // finden" fanden vorher nichts Passendes.
        // 415 -> 416 am 19.08.2026: "Vorher hinsehen: was ein
        // Aussichtspunkt zeigt und was nicht" -- "route planen" fuehrte
        // vorher auf den GARTENPLAN.
        // 416 -> 417 am 19.08.2026: "Milchstau: nicht abstillen, sondern
        // oefter anlegen" -- "mastitis" fand vorher NICHTS und
        // "brustentzündung" fuehrte auf "Antibiotika: wann keines gegeben
        // wird".
        // 417 -> 418 am 11.08.2026: "Unbemerkt bleiben: Bewegung, Umriss, Glanz, Geraeusch, Geruch"
        // (taktisch-nicht-auffallen).
        // 418 -> 419 am 11.08.2026: "Scharfschuetze: Versteck ist nicht dasselbe wie Schutz"
        // (taktisch-schuetzenfeuer).
        // 419 -> 420 am 11.08.2026: "Schuesse hoeren: der Knall verraet die Entfernung"
        // (taktisch-was-man-hoert).
        // 420 -> 421 am 11.08.2026: "Entfuehrung: nicht fliehen, ruhig bleiben, Anweisungen befolgen"
        // (taktisch-entfuehrung).
        // 421 -> 422 am 11.08.2026: "Festgehalten oder durchsucht: ruhig bleiben, nichts Ploetzliches tun"
        // (taktisch-festgehalten-werden).
        // 422 -> 423 am 11.08.2026: "Hangrutschung: Warnzeichen erkennen, seitlich weg"
        // (taktisch-hangrutschung).
        // 423 -> 424 am 11.08.2026: "Lebensmittelvergiftung: was die Zeit bis zur Krankheit verraet"
        // (medizin-lebensmittelkeime).
        // 424 -> 425 am 11.08.2026: "Bezug zur Wirklichkeit verloren: erkennen und begleiten"
        // (medizin-psychose-begleiten).
        // 425 -> 426 am 11.08.2026: "Klebestreifen statt Naht: wann es reicht, wann nicht"
        // (erste-hilfe-wunde-klebestreifen).
        // 426 -> 427 am 11.08.2026: "Die Karte einnorden und wissen, wo man wirklich steht"
        // (orientierung-karte-einnorden).
        // 427 -> 428 am 11.08.2026: "Fahrzeug in gefaehrlicher Gegend: Abstand halten, keine Waffen an Bord"
        // (taktisch-fahrzeug-gefahrengebiet).
        // 428 -> 429 am 11.08.2026: "Die eigene Lage einschaetzen: Regeln setzen, bevor es kippt"
        // (taktisch-lage-eigene-regeln).
        // 429 -> 430 am 11.08.2026: "Luftangriff: die Warnzeichen vor der Bombe"
        // (taktisch-luftangriff).
        // 430 -> 431 am 11.08.2026: "Der Weg zum Versteck: Umwege statt Strassen, keine Spur zurueck"
        // (taktisch-versteck-und-weg).
        // 431 -> 432 am 11.08.2026: "Verletzt unter Beschuss: nur die Abbindung, der Rest wartet"
        // (erste-hilfe-verletzt-unter-beschuss).
        // 432 -> 433 am 11.08.2026: "Nicht zurueckgekommen: Wegpunkte, Zettel, Grenze"
        // (taktisch-vermisst-wegpunkte).
        // 433 -> 434 am 11.08.2026: "Sandsaecke und Splitterschutz: das Haus herrichten"
        // (taktisch-splitterschutz).
        // 434 -> 435 am 11.08.2026: "Ueberfallen werden: hergeben statt wehren"
        // (taktisch-ueberfall).
        // 435 -> 436 am 11.08.2026: "Sexueller Uebergriff: keine Schuld, keine einzige Antwort"
        // (taktisch-sexueller-uebergriff).
        // 436 -> 437 am 11.08.2026: "Handzeichen fuer lautlose Verstaendigung"
        // (taktisch-handzeichen).
        // 437 -> 438 am 11.08.2026: "Den Vorrat verteilen, nicht verteidigen"
        // (taktisch-vorrat-verteilen).
        // 438 -> 439 am 11.08.2026: "Lange eingeklemmt: die Gefahr kommt oft erst beim Befreien"
        // (erste-hilfe-crush-syndrom).
        // 439 -> 440 am 11.08.2026: "Ohne Essen: wie lange es geht  und warum man sofort sucht"
        // (medizin-ohne-essen-wie-lange).
        // 440 -> 441 am 11.08.2026: "Brand im Haus: loeschen, wenn niemand kommt"
        // (taktisch-brand-selbst-loeschen).
        // 441 -> 442 am 11.08.2026: "Verstopfung: was ohne Apotheke wirklich hilft"
        // (medizin-verstopfung).
        // 442 -> 443 am 11.08.2026: "Moos weist nicht nach Norden: was Baumwuchs und Landschaft verraten"
        // (orientierung-naturzeichen).
        // 443 -> 444 am 11.08.2026: "Was vom Vorrat zuerst dran ist"
        // (nahrung-vorraete-reihenfolge).
        // 444 -> 445 am 11.08.2026: "Bleiche statt Tabletten: Wasser entkeimen"
        // (wasser-bleiche-entkeimung).
        // 445 -> 446 am 11.08.2026: "Sparsam kochen: Brennstoff strecken"
        // (nahrung-kochen-sparsam).
        // 446 -> 447 am 11.08.2026: "Kein Netz mehr: eine Kontaktperson an einem anderen Ort, und ein Treffpunkt"
        // (taktisch-kontaktperson-treffpunkt).
        // 447 -> 448 am 11.08.2026: "Allein und verletzt: was zuerst zaehlt"
        // (erste-hilfe-allein-verletzt).
        // 448 -> 449 am 11.08.2026: "Aufruhr und Pluenderung: wenn die Menge feindselig wird"
        // (taktisch-aufruhr-pluenderung).
        // 449 -> 450 am 11.08.2026: "Wanzen: sie ueberleben ein Jahr, ohne zu fressen"
        // (medizin-wanzen).
        // 450 -> 451 am 11.08.2026: "Sich bewegen, wenn man gesehen werden kann: Sprung, Kriechgang, wann was"
        // (taktisch-bewegung-beobachtet).
        // 451 -> 452 am 11.08.2026: "Beschuss zu Fuss im Freien: weg von den Einschlaegen, nicht abwarten"
        // (taktisch-beschuss-zu-fuss).
        // 452 -> 453 am 11.08.2026: "Sicherungshalt: wie eine Gruppe rastet, ohne ueberrascht zu werden"
        // (taktisch-sicherungshalt).
        // 453 -> 454 am 11.08.2026: "Beobachten: erst der ganze Blick, dann Streifen fuer Streifen"
        // (taktisch-bereich-absuchen).
        // 454 -> 455 am 11.08.2026: "Melden, damit jemand etwas anfangen kann: fuenf Angaben in fester Reihenfolge"
        // (taktisch-meldung-aufbauen).
        // 455 -> 456 am 11.08.2026: "Schnell aufbrechen: die Reihenfolge, die eine Gruppe zusammenhaelt"
        // (taktisch-schnell-aufbrechen).
        // 456 -> 457 am 11.08.2026: "Pfeifton oder Lichtblitz: sich verstaendigen, ohne sich zu sehen"
        // (taktisch-pfeifton-lichtzeichen).
        // 457 -> 458 am 11.08.2026: "Wenn der Kiefer nicht mehr zugeht: mit den Daumen zurueckdruecken"
        // (erste-hilfe-kiefer-ausgerenkt).
        // 458 -> 459 am 11.08.2026: "Kopfschlag mit Blut oder klarer Fluessigkeit aus Ohr oder Nase: sofort ernst nehmen"
        // (erste-hilfe-kopfverletzung-warnzeichen).
        // 459 -> 460 am 11.08.2026: "Quallenstich und Seeigel: was am Strand wirklich hilft"
        // (erste-hilfe-quallenstich-seeigel).
        // 460 -> 461 am 11.08.2026: "Schwindel: welche Art es ist, und was dagegen hilft"
        // (medizin-schwindel).
        // 461 -> 462 am 11.08.2026: "Depression und Manie: wenn die Stimmung krank wird"
        // (medizin-depression-manie).
        // 462 -> 463 am 11.08.2026: "Schimmel am Vorrat: wegschneiden oder ganz weg"
        // (nahrung-schimmel-am-vorrat).
        // 463 -> 464 am 11.08.2026: "Blaualgen: Abkochen macht das Gift nicht unschaedlich"
        // (wasser-blaualgen-abkochen-hilft-nicht).
        // 464 -> 465 am 11.08.2026: "Hangrichtung: Standort finden, wenn man nichts sieht"
        // (orientierung-hangrichtung-standort).
        // 465 -> 466 am 11.08.2026: "Verschuettet: klopfen statt rufen"
        // (taktisch-verschuettet-klopfen).
        // 466 -> 467 am 11.08.2026: "Kuehlschrank und Gefriertruhe im Stromausfall"
        // (nahrung-kuehlschrank-stromausfall).
        // 467 -> 468 am 11.08.2026: "Gasgeruch: nichts anschalten, sofort alle hinaus"
        // (taktisch-gasgeruch-sofort).
        // 468 -> 469 am 11.08.2026: "Notstromaggregat und Generator: sicher betreiben"
        // (taktisch-generator-sicher-betreiben).
        // 469 -> 470 am 11.08.2026: "Neugeborenes warm halten: Haut an Haut gegen das Auskuehlen"
        // (medizin-neugeborenes-warm-halten).
        // 470 -> 471 am 11.08.2026: "Neugeborenes: Gelbsucht erkennen und einen schwachen Saeugling ernaehren"
        // (medizin-neugeborenes-gelbsucht-ernaehren).
        // 471 -> 472 am 11.08.2026: "Hund und Katze: mitnehmen, giftige Lebensmittel und Hitze"
        // (taktisch-haustiere-mitnehmen).
        // 472 -> 473 am 11.08.2026: "Vom Flaechenbrand eingeschlossen: was jetzt zaehlt"
        // (taktisch-flaechenbrand-eingeschlossen).
        // 473 -> 474 am 11.08.2026: "Schornsteinbrand: was sofort zu tun ist"
        // (taktisch-schornsteinbrand).
        // 474 -> 475 am 11.08.2026: "Einen Toten begraben: Tiefe und Abstand"
        // (medizin-tote-begraben-wie).
        // 475 -> 476 am 20.08.2026: "Offene Druckstelle: reinigen, abdecken, beobachten"
        // (medizin-wundliegen-versorgen).
        // 476 -> 477 am 20.08.2026: "Umlagern im Bett: mit dem Laken ziehen, nicht schleifen"
        // (medizin-bettlaegerige-umlagern).
        // 477 -> 478 am 20.08.2026: "Waschen im Bett, wenn Aufstehen nicht geht"
        // (medizin-bettlaegerige-waschen).
        // 478 -> 479 am 20.08.2026: "Wund im Intimbereich: Feuchtigkeit statt Druck"
        // (medizin-inkontinenz-hautpflege).
        // 479 -> 480 am 20.08.2026: "Anweisen und wiederholen lassen: so kommt es an"
        // (taktisch-befehl-rueckmeldung).
        // 480 -> 481 am 20.08.2026: "Entscheiden unter Zeitdruck: besser jetzt als perfekt"
        // (taktisch-entscheidung-zeitdruck).
        // 481 -> 482 am 20.08.2026: "Wenn die Fuehrung ausfaellt: wer uebernimmt"
        // (taktisch-fuehrung-vertretung).
        // 482 -> 483 am 20.08.2026: "Wache stehen: drei Regeln und die Uebergabe"
        // (taktisch-wache-uebergabe).
        // 483 -> 484 am 20.08.2026: "Eine offene Stelle als Gruppe ueberqueren: erst pruefen, dann nacheinander"
        // (taktisch-offene-stelle-gruppe-ueberqueren).
        // 484 -> 485 am 20.08.2026: "Sich durch eine Stadt bewegen, ohne gesehen zu werden: Waende, Ecken, Fenster"
        // (taktisch-stadt-unbemerkt-bewegen).
        // 485 -> 486 am 20.08.2026: "Die letzte Reserve steckt im Speicher"
        // (wasser-speicher-anzapfen).
        // 486 -> 487 am 20.08.2026: "Hauptabsperrhahn zu, dann laeuft das Rohr leer"
        // (wasser-rohre-haupthahn).
        // 487 -> 488 am 20.08.2026: "Heizung und Bett: zwei Speicher, die man nicht anzapft"
        // (wasser-heizung-bett-tabu).
        // 488 -> 489 am 20.08.2026: "Spuelkasten, Eiswuerfel, Konservenlake: was sonst noch zaehlt"
        // (wasser-spuelkasten-eiswuerfel-konserve).
        // 489 -> 490 am 20.08.2026: "Pool und Zisterne: wozu die Reserve taugt, wozu nicht"
        // (wasser-pool-zisterne-grenzen).
        // 490 -> 491 am 20.08.2026: "Massstab und Zeichen: eine Karte wirklich lesen koennen"
        // (orientierung-massstab-zeichen).
        // 491 -> 492 am 20.08.2026: "Steigung berechnen: wie steil ein Hang wirklich ist"
        // (orientierung-steigung-berechnen).
        // 492 -> 493 am 20.08.2026: "Eine Strasse, ein Grat oder ein Bach als Leitlinie: wann man sie verlaesst"
        // (orientierung-leitlinie).
        // 493 -> 494 am 20.08.2026: "Wo ist das? Einen fernen Punkt, den man nur sieht, auf der Karte finden"
        // (orientierung-fernpunkt-peilen).
        // 494 -> 495 am 20.08.2026: "Beschaedigte Konserven: wann noch sicher, wann nicht mehr"
        // (nahrung-konserven-beschaedigt).
        // 495 -> 496 am 20.08.2026: "Esskastanien sammeln: die Rosskastanie sicher ausschliessen"
        // (nahrung-esskastanien-rosskastanie).
        // 496 -> 497 am 20.08.2026: "Gruene oder keimende Kartoffeln: was noch zu retten ist"
        // (nahrung-kartoffeln-gruen-keimend).
        // 497 -> 498 am 20.08.2026: "Reste wiedererhitzen: was das rettet, und was nicht"
        // (nahrung-reste-wiedererhitzen).
        // 498 -> 499 am 20.08.2026: "Einen Lagerplatz waehlen: was der Platz selbst mitbringt"
        // (taktisch-lagerplatz-waehlen).
        // 499 -> 500 am 20.08.2026: "Feuchtigkeit und Schimmel im geheizten Raum"
        // (taktisch-schimmel-geheizter-raum).
        // 500 -> 501 am 20.08.2026: "Abstand: der erste und wichtigste Schritt zurueck"
        // (taktisch-abstand-zuerst).
        // 501 -> 502 am 20.08.2026: "Fluchtwege vorher festlegen: wohin man laeuft"
        // (taktisch-fluchtweg-vorher).
        // 502 -> 503 am 20.08.2026: "Laut werden: wann es hilft, wann nicht"
        // (taktisch-laut-werden).
        // 503 -> 504 am 20.08.2026: "Nach der Bedrohung: was der Koerper noch tut"
        // (taktisch-nach-bedrohung-koerper).
        // 504 -> 505 am 20.08.2026: "Grundhaltung: Haende oben und offen"
        // (taktisch-selbstschutz-grundhaltung).
        // 505 -> 506 am 20.08.2026: "Gepackt, gegriffen, umklammert: sich loesen"
        // (taktisch-griff-loesen).
        // 506 -> 507 am 20.08.2026: "Wuergegriff loesen: vorn, hinten, am Boden"
        // (taktisch-wuergegriff-loesen).
        // 507 -> 508 am 20.08.2026: "Fallen ohne Verletzung, und sofort wieder hoch"
        // (taktisch-fallen-aufstehen).
        // 508 -> 509 am 20.08.2026: "Trefferstellen ohne Kraft: nur um Abstand zu gewinnen"
        // (taktisch-trefferstellen-abstand).
        // 509 -> 510 am 20.08.2026: "Notwehr: was das Gesetz erlaubt"
        // (taktisch-notwehr-rechtslage).
        // 510 -> 511 am 20.08.2026: "Bodenkampf: die vier Grundpositionen"
        // (taktisch-bodenkampf-grundpositionen).
        // 511 -> 512 am 20.08.2026: "Wuergegriff ansetzen: Wirkung und Zeit"
        // (taktisch-wuergegriff-anwenden).
        // 512 -> 513 am 20.08.2026: "Hebel an Arm, Handgelenk und Schulter"
        // (taktisch-gelenkhebel).
        // 513 -> 514 am 20.08.2026: "Wuerfe: einen Angreifer kontrolliert zu Boden bringen"
        // (taktisch-wuerfe-zu-boden-bringen).
        // 514 -> 515 am 20.08.2026: "Schlaege und Tritte: die Waffen des Koerpers"
        // (taktisch-schlaege-tritte-koerperwaffen).
        // 515 -> 516 am 20.08.2026: "Gewaltstufen: Kontrolle vor Schaden"
        // (taktisch-gewaltstufen-einordnung).
        // 516 -> 517 am 20.08.2026: "Wuerge-Takedown von hinten: endet mit Genickbruch"
        // (taktisch-wuerge-takedown-hinten).
        // 517 -> 518 am 20.08.2026: "Abstand und Waffe: was fast immer entscheidet"
        // (taktisch-abstand-und-waffe).
        // 518 -> 519 am 20.08.2026: "Stock und Stab: halten, schlagen, abwehren"
        // (taktisch-stock-und-stab).
        // 519 -> 520 am 20.08.2026: "Messer: Griff, Stand und Abwehr"
        // (taktisch-messer-griff-abwehr).
        // 520 -> 521 am 20.08.2026: "Unbewaffnet gegen eine Waffe: entwaffnen als letzter Ausweg"
        // (taktisch-entwaffnen-letzter-ausweg).
        // 521 -> 522 am 20.08.2026: "Spaten, Riemen, Helm: Schutz aus dem, was da ist"
        // (taktisch-behelfsgegenstaende).
        // 522 -> 523 am 20.08.2026: "Besitzen oder fuehren: der Unterschied, der zaehlt"
        // (taktisch-waffenrecht-besitz-fuehren).
        // 523 -> 524 am 20.08.2026: "Klinge und Bauart: was du dabei haben darfst"
        // (taktisch-klingenlaenge-und-bauart).
        // 524 -> 525 am 20.08.2026: "Spruehgeraet, Alarm und Verbotszone: was zusaetzlich gilt"
        // (taktisch-spruehgeraet-alarm-verbotszone).
        // 525 -> 526 am 20.08.2026: "Steckengeblieben: was das Fahrzeug freibekommt, was es tiefer setzt"
        // (taktisch-fahrzeug-steckengeblieben).
        // 526 -> 527 am 20.08.2026: "Abschleppen und Anschieben: wo das Seil sicher haelt"
        // (taktisch-fahrzeug-abschleppen).
        // 527 -> 528 am 20.08.2026: "Furt beurteilen, bevor man mit dem Fahrzeug hineinfaehrt"
        // (taktisch-furt-beurteilen).
        // 528 -> 529 am 20.08.2026: "Strasse beschaedigt: wie man weiterkommt, ohne stehenzubleiben"
        // (taktisch-strasse-beschaedigt-befahren).
        // 529 -> 530 am 20.08.2026: "Trinken, Kochen, Waschen: der Tagesbedarf einer Gruppe"
        // (wasser-bedarf-trinken-kochen-waschen).
        // 530 -> 531 am 20.08.2026: "Kanister, Faesser und Tanks: reinigen und entkeimen"
        // (wasser-behaelter-reinigen-entkeimen).
        // 531 -> 532 am 20.08.2026: "Entkeimtes im Vorrat: noch gut oder nicht mehr?"
        // (wasser-vorrat-noch-gut).
        // 532 -> 533 am 20.08.2026: "Die Ausgabestelle fuer eine Gruppe: Abstand und Sauberkeit"
        // (wasser-ausgabestelle-gruppe).
        // 533 -> 534 am 20.08.2026: "Fahrzeug oder Geraet abdecken, ohne dass die Abdeckung selbst auffaellt"
        // (taktisch-fahrzeug-abdecken).
        // 534 -> 535 am 20.08.2026: "Licht bei Nacht: wie weit ein Lichtschein zu sehen ist"
        // (taktisch-licht-bei-nacht).
        // 535 -> 536 am 20.08.2026: "Tarnmaterial frisch halten: warum Welken schlimmer ist als gar nichts aufzulegen"
        // (taktisch-tarnmaterial-frisch-halten).
        // 536 -> 537 am 20.08.2026: "Der eigene Trampelpfad: warum man nicht immer denselben Weg geht"
        // (taktisch-eigener-trampelpfad).
        // 537 -> 538 am 20.08.2026: "Latrine, Kueche, Trinkstelle: die Abstaende im Lager"
        // (hygiene-lagerabstaende).
        // 538 -> 539 am 20.08.2026: "Geschirr spuelen ohne Spuele: drei Becken nacheinander"
        // (hygiene-geschirr-drei-becken).
        // 539 -> 540 am 20.08.2026: "Wer nicht kochen darf, wenn jemand krank ist"
        // (hygiene-kochverbot-krank).
        // 540 -> 541 am 20.08.2026: "Fliegen bekaempfen: die Brutstaette zuerst"
        // (hygiene-fliegen-bekaempfen).
        // 541 -> 542 am 20.08.2026: "Allein tragen: Huckepack, Rucksack- und Guertelgriff"
        // (erste-hilfe-huckepack-rucksacktrage).
        // 542 -> 543 am 20.08.2026: "Zu zweit tragen: welcher Griff zu welcher Strecke"
        // (erste-hilfe-zu-zweit-tragen).
        // 543 -> 544 am 20.08.2026: "Bewusstlos tragen: Kopf, Haende und Atmung sichern"
        // (erste-hilfe-bewusstlosen-tragen-sichern).
        // 544 -> 545 am 20.08.2026: "Trage auf Treppen und am Hang: Kopf oder Fuss voran"
        // (erste-hilfe-trage-treppe-hang).
        // 545 -> 546 am 21.08.2026: "Richtig zuschlagen: Faust, Handgelenk, Trefferzeit"
        // (taktisch-richtig-zuschlagen).
        // 546 -> 547 am 21.08.2026: "Allein gegen mehrere: stehen bleiben, Ruecken schuetzen"
        // (taktisch-allein-gegen-mehrere).
        // 547 -> 548 am 21.08.2026: "Zu mehreren gegen mehrere: gestaffelt statt gleichzeitig"
        // (taktisch-gruppe-gegen-mehrere).
        // 548 -> 549 am 21.08.2026: "Schwitzkasten am Boden loesen"
        // (taktisch-schwitzkasten-boden-loesen).
        // 549 -> 550 am 21.08.2026: "Knie-Aufsitzer: die fuenfte Position"
        // (taktisch-knie-aufsitzer).
        // 550 -> 551 am 21.08.2026: "Guard passieren: geschlossen und offen"
        // (taktisch-guard-passieren).
        // 551 -> 552 am 21.08.2026: "Beinhebel: Fussgelenk und Knie"
        // (taktisch-beinhebel).
        // 552 -> 553 am 21.08.2026: "Position wechseln, wenn der Gegner sich wehrt"
        // (taktisch-position-wechseln-boden).
        // 553 -> 554 am 21.08.2026: "Unbewaffnet gegen eine lange Waffe: sechs Angriffswinkel"
        // (taktisch-lange-waffe-abwehr-winkel).
        // 554 -> 555 am 21.08.2026: "Mistgabel, Spaten, Stange: Griff und Stoss mit einer langen Waffe"
        // (taktisch-lange-waffe-griff-stoss).
        // 555 -> 556 am 21.08.2026: "Messer gegen Messer: vier Angriffsabfolgen"
        // (taktisch-messer-gegen-messer-abfolgen).
        // 556 -> 557 am 21.08.2026: "Eine Bewegung lernen: erst in Einzelschritten, dann im Tempo"
        // (taktisch-technik-lernschritte).
        // 557 -> 558 am 21.08.2026: "Zu zweit ueben: die Regeln, damit niemand verletzt wird"
        // (taktisch-uebungspartner-sicherheit).
        // 558 -> 559 am 21.08.2026: "Posten ausschalten: Anschleichen und neun Techniken"
        // (taktisch-posten-ausschalten).
        // 559 -> 560 am 21.08.2026: "Wache stehen, ohne zum leichten Ziel zu werden"
        // (taktisch-wache-schwachstellen).
        // 560 -> 561 am 21.08.2026: "Wuergen und Hebeln am Boden: die einzelnen Griffe"
        // (taktisch-wuergen-hebeln-am-boden).
        // 561 -> 562 am 21.08.2026: "Spaten, Riemen, Stock gegen eine Klinge: die letzte Stufe"
        // (taktisch-behelfswaffe-letzte-stufe).
        // 562 -> 563 am 21.08.2026: "Clinch: den Angreifer heranziehen und halten"
        // (taktisch-clinch-heranziehen).
        // 563 -> 564 am 21.08.2026: "Schwitzkasten im Stehen loesen"
        // (taktisch-schwitzkasten-stehen-loesen).
        // 564 -> 565 am 21.08.2026: "Zu den Beinen durchtauchen: ein- und beidbeinig"
        // (taktisch-beinangriff-durchtauchen).
        // 565 -> 566 am 21.08.2026: "Den Angreifer an die Wand draengen"
        // (taktisch-wand-draengen).
        // 566 -> 567 am 21.08.2026: "Bodenkampf: die Uebergaenge zwischen den Positionen"
        // (taktisch-bodenkampf-uebergaenge).
        // 567 -> 568 am 21.08.2026: "Armstreckhebel und Kragenwuerger am Boden"
        // (taktisch-armstreckhebel-kragenwuerger).
        // 568 -> 569 am 21.08.2026: "Wuergegriff von hinten: Wurf und Schulterhebel als weitere Antworten"
        // (taktisch-wuergegriff-hinten-alternativen).
        // 569 -> 570 am 21.08.2026: "Wuergen ohne Kleidung: direkter Griff an Kehlkopf und Halsschlagadern"
        // (taktisch-wuergen-ohne-kleidung).
        // 570 -> 571 am 21.08.2026: "Schlagformen und Kombinationen"
        // (taktisch-schlagformen-kombinationen).
        // 571 -> 572 am 21.08.2026: "Trittformen und Distanzwechsel"
        // (taktisch-tritte-distanzwechsel).
        // 572 -> 573 am 21.08.2026: "Eine Uebungslage aufbauen: Situation erklaeren, Regeln vorher festlegen"
        // (taktisch-uebungslage-aufbauen).
        // 573 -> 574 am 21.08.2026: "Verbotene Techniken im Wettkampf: was sie verraten"
        // (taktisch-wettkampf-verbotene-techniken).
        // 574 -> 575 am 21.08.2026: "Eine ueberwaeltigte Person sichern und begleiten"
        // (taktisch-person-sichern-begleiten).
        assertEquals(575, pack.tips.size)
        // Dieselbe Schranke fuer Bauanleitungen und Agrikultur-Kapitel. Sie hat
        // bis zum 10.08.2026 gefehlt, und das war mit 27 Kapiteln noch zu
        // verschmerzen. An diesem Tag sind sechs dazugekommen; ab dieser
        // Groesse faellt ein still verschwundenes Kapitel niemandem mehr auf,
        // weil die Zahl in der Kopfzeile der App nur die SUMME zeigt.
        // Hochgezaehlt wird auch hier nur MIT einem bewusst hinzugefuegten
        // Eintrag, und daneben kommt in den Kommentar, welcher es war.
        // 17 Bauanleitungen seit dem 10.08.2026 (Axtstiel, Messer schaerfen,
        // Werkzeug schaerfen).
        // 17 -> 18 am 11.08.2026: "Kochkiste bauen und richtig benutzen"
        // (kochkiste-bauen).
        // 18 -> 19 am 11.08.2026: "Abort bauen: fliegendicht und mit Behaelter"
        // (hygiene-abort-bauen), erste Anleitung der neuen Gruppe "hygiene".
        // 19 -> 20 am 11.08.2026: "Doerre bauen: Horde, Schrank und Sonne"
        // (nahrung-doerre-bauen).
        // 20 -> 21 am 11.08.2026: "Sandfilter bauen: trinkbar machen ohne
        // Brennstoff" (wasser-sandfilter-bauen), erste Anleitung der neuen
        // Gruppe "wasser".
        // 21 -> 22 am 11.08.2026: "Lasten heben: Dreibock, Bockmast und
        // Ankerpunkte" (seilwerk-heben-und-verankern).
        // 22 -> 23 am 11.08.2026: "Floss bauen: Gepaeck trocken
        // hinueberbringen" (gelaende-floss-bauen).
        // 23 -> 24 am 11.08.2026: "Den Meiler bauen: aus Holz wird Kohle"
        // (werkzeug-holzkohle-meiler).
        // 24 -> 25 am 11.08.2026: "Gefaesse aus Ton: pruefen, formen, trocknen"
        // (werkstoffe-toepfern).
        // 25 -> 26 am 12.08.2026: "Raeucherkammer bauen und Fisch raeuchern"
        // (nahrung-raeucherkammer-bauen).
        // 26 -> 27 am 12.08.2026: "Stampflehmwand bauen: ein Haus aus der Erde, auf der es steht"
        // (unterkunft-stampflehmwand).
        // 27 -> 28 am 12.08.2026: "Dach decken: Stroh, Reet oder Holzschindeln"
        // (unterkunft-dach-decken).
        // 28 -> 29 am 12.08.2026: "Mit Feldstein mauern: Fundament, Verband und Ecken"
        // (unterkunft-feldstein-mauern).
        // 29 -> 30 am 12.08.2026: "Ziegel selbst machen: formen, trocknen, im Meiler brennen"
        // (werkstoffe-ziegel-brennen).
        // 30 -> 31 am 12.08.2026: "Schornstein und Ofenrohr richtig bauen und anschliessen"
        // (feuer-schornstein-ofenrohr).
        // 31 -> 32 am 12.08.2026: "Schuhe selbst machen: Mokassin, Riemenschuh und Sohle"
        // (werkstoffe-schuhe-mokassin).
        // 32 -> 33 am 12.08.2026: "Wasserrad: was ein Bach hergibt und wie man es misst"
        // (werkzeug-wasserrad-messen).
        // 33 -> 34 am 12.08.2026: "Filzen: aus Wolle wird Stoff, ohne Spinnen und Weben"
        // (werkstoffe-filzen-wolle).
        // 34 -> 35 am 12.08.2026: "Faerben mit Pflanzen: Beize, Farbe und was giftig ist"
        // (werkstoffe-faerben-pflanzen).
        // 35 -> 36 am 12.08.2026: "Stiche, Zuschnitt und Passform ohne Schnittmuster"
        // (werkstoffe-stiche-zuschnitt).
        // 36 -> 37 am 12.08.2026: "Kalk brennen: der Stapel als Ofen"
        // (werkstoffe-kalk-brennen).
        // 37 -> 38 am 11.08.2026: "Bogen und Pfeile bauen"
        // (jagd-bogen-bauen).
        // 38 -> 39 am 11.08.2026: "Lehmofen bauen: feste Kochstelle mit wenig Brennstoff"
        // (kochstelle-lehmofen-bauen).
        // 39 -> 40 am 11.08.2026: "Einen Brunnen von Hand graben: Schacht sichern und heben"
        // (wasser-brunnen-graben).
        // 40 -> 41 am 11.08.2026: "Pumpe, Widder und Windrad: Wasser heben ohne Strom"
        // (wasser-heben-ohne-strom).
        // 41 -> 42 am 11.08.2026: "Koerbe flechten: Material, Boden, Rand"
        // (werkstoffe-koerbe-flechten).
        // 42 -> 43 am 11.08.2026: "Werkbank bauen: mit Saegeboecken und Schneidlade"
        // (werkzeug-werkbank-bauen).
        // 43 -> 44 am 11.08.2026: "Schlitten und Ziehtrage bauen: Lasten ziehen ohne Raeder"
        // (gelaende-schlitten-ziehtrage).
        // 44 -> 45 am 11.08.2026: "Rinne, Fallrohr und Fass: Regen vom Dach auffangen"
        // (wasser-rinne-fass-bauen).
        // 45 -> 46 am 11.08.2026: "Siebe und Raetter bauen: Rahmen, Geflecht und Gefaelle"
        // (werkzeug-sieb-raetter-bauen).
        // 46 -> 47 am 11.08.2026: "Ein Bett ueber dem Boden bauen"
        // (unterkunft-bett-erhoeht).
        // 47 -> 48 am 11.08.2026: "Tuer und Fensterladen aus Brettern bauen"
        // (unterkunft-tuer-fensterladen).
        // 48 -> 49 am 11.08.2026: "Eine Leiter bauen: Kerbleiter und Sprossenleiter"
        // (werkzeug-leiter-bauen).
        // 49 -> 50 am 11.08.2026: "Tisch und Bank aus rohem Holz bauen"
        // (unterkunft-tisch-bank-bauen).
        // 50 -> 51 am 11.08.2026: "Schubkarre und Handwagen bauen: alles bis auf das Rad"
        // (werkzeug-schubkarre-handwagen).
        // 51 -> 52 am 11.08.2026: "Ein Eimer aus dem, was da ist"
        // (werkstoffe-eimer-behelfsmaessig).
        // 52 -> 53 am 11.08.2026: "Laengen abnehmen und uebertragen ohne Zollstock"
        // (werkzeug-laengen-ohne-zollstock).
        // 53 -> 54 am 11.08.2026: "Waagerecht und senkrecht pruefen ohne gekauftes Werkzeug"
        // (werkzeug-waagerecht-senkrecht).
        // 53 -> 54 am 11.08.2026: "Waagerecht und senkrecht pruefen ohne gekauftes Werkzeug"
        // (werkzeug-waagerecht-senkrecht).
        // 54 -> 55 am 20.08.2026: "Loecher bohren ohne Bohrmaschine: Ahle, Nagelbohrer, Bohrwinde"
        // (werkzeug-loch-ohne-bohrmaschine).
        // 55 -> 56 am 20.08.2026: "Griff oder Stiel neu einsetzen: Hammer, Meissel und Feile"
        // (werkzeug-griff-hammer-meissel-feile).
        // 56 -> 57 am 20.08.2026: "Leiter richtig anlegen: Winkel, Stand und Sicherung"
        // (werkzeug-leiter-anlegen).
        // 57 -> 58 am 20.08.2026: "Kleber und Dichtmasse aus Kiefernharz"
        // (werkstoffe-kleber-kiefernharz).
        // 58 -> 59 am 20.08.2026: "Werkzeug richten: Verbogenes strecken und ein verschlagenes Ende neu haerten"
        // (werkzeug-verbogen-richten).
        // 59 -> 60 am 20.08.2026: "Eine schwere Last zu zweit heben, tragen und absetzen"
        // (gelaende-schwere-last-zu-zweit).
        // 60 -> 61 am 20.08.2026: "Zerbrochenes Eisen zusammenschmieden: zwei Enden verschweissen"
        // (werkzeug-schmiedeschweissen).
        // 61 -> 62 am 20.08.2026: "Zugluft finden: wo es im Raum wirklich zieht"
        // (unterkunft-zugluft-finden).
        // 62 -> 63 am 20.08.2026: "Fenster und Tueren gegen Kaelte abdichten"
        // (unterkunft-fenster-tuer-abdichten).
        assertEquals(63, pack.guides.size)
        // 27 -> 35 am 10.08.2026: Bienenvolk halten, Einen Brunnen anlegen und
        // schuetzen, Obstbaeume schneiden und veredeln, Beerenobst, Zaeune,
        // Nutztiere (Klauen, Euter, Wuermer), Seife und Waesche ohne Nachschub,
        // Zwiebeln anbauen, trocknen, lagern, Winterfutter (Heu und Silage),
        // Scheitholz (schlagen, stapeln, ablagern).
        // 37 -> 38 am 11.08.2026: "Mistbeet und Kaltkasten: Waerme ohne
        // Brennstoff" (agrikultur-mistbeet).
        // 38 -> 39 am 11.08.2026: "Brot backen: Triebmittel selbst machen
        // und den Teig fuehren" (agrikultur-brot).
        // 39 -> 40 am 11.08.2026: "Ein Schwein schlachten: der ganze
        // Ablauf" (agrikultur-schwein-schlachten).
        // 40 -> 41 am 11.08.2026: "Kalk loeschen und verwenden: Moertel, Tuenche, Stallhygiene"
        // (agrikultur-kalk).
        // 41 -> 42 am 12.08.2026: "Gerben ohne Rinde: mit Hirn oder mit Alaun"
        // (agrikultur-gerben-ohne-rinde).
        // 42 -> 43 am 12.08.2026: "Das Vlies: Wollfett, Schur und wie man Wolle beurteilt"
        // (agrikultur-vlies-beurteilen).
        // 43 -> 44 am 12.08.2026: "Butter machen: von der Milch bis zum Fass"
        // (agrikultur-butter).
        // 44 -> 45 am 12.08.2026: "Kaese machen: Bruch schneiden, pressen, reifen"
        // (agrikultur-kaese).
        // 45 -> 46 am 11.08.2026: "Maisgriess und Maismehl: die Mahlgrade und was man daraus isst"
        // (agrikultur-mais-mehl).
        // 46 -> 47 am 11.08.2026: "Milben und Laeuse am Huhn erkennen und loswerden"
        // (agrikultur-milben-laeuse).
        // 47 -> 48 am 11.08.2026: "Saubere Milch: Kuh melken, kuehlen, Gefaesse reinhalten"
        // (agrikultur-milch-sauber).
        // 48 -> 49 am 11.08.2026: "Dreschen und Worfeln: von der Garbe zum sauberen Korn"
        // (agrikultur-dreschen-worfeln).
        // 49 -> 50 am 11.08.2026: "Kartoffelkrankheiten erkennen und eindaemmen"
        // (agrikultur-kartoffel-krankheiten).
        // 50 -> 51 am 11.08.2026: "Ziegen halten: Stall, Futter und Zucht"
        // (agrikultur-ziegen-halten).
        // 51 -> 52 am 11.08.2026: "Enten und Gaense halten: was beim Wassergefluegel anders ist"
        // (agrikultur-enten-gaense-halten).
        // 52 -> 53 am 11.08.2026: "Fett und Oel ohne Handel: Oelsaaten pressen und Schmalz auslassen"
        // (agrikultur-fett-oel).
        // 53 -> 54 am 11.08.2026: "Weizen, Hafer und Gerste: welches Korn auf welchen Boden gehoert"
        // (agrikultur-weizen-hafer-gerste).
        // 54 -> 55 am 11.08.2026: "Kuh halten: Stall, Futter und Kalbung"
        // (agrikultur-kuh-halten).
        // 55 -> 56 am 11.08.2026: "Essig machen: aus Obst wird Saeure"
        // (agrikultur-essig-machen).
        // 56 -> 57 am 11.08.2026: "Einen Fischteich anlegen und besetzen"
        // (agrikultur-fischteich).
        // 57 -> 58 am 11.08.2026: "Das Feld bewaessern: Furche, Streifen und Flut ohne Pumpe"
        // (agrikultur-feld-bewaessern).
        // 58 -> 59 am 11.08.2026: "Sirup machen: Suesse aus Ruebe, Ahorn und Sorghum"
        // (agrikultur-sirup).
        // 59 -> 60 am 11.08.2026: "Zugpferd: Geschirr, Futter und Anlernen"
        // (agrikultur-zugpferd).
        // 60 -> 61 am 11.08.2026: "Einen Erdkeller bauen"
        // (agrikultur-erdkeller-bauen).
        // 61 -> 62 am 11.08.2026: "Einen Obstbaum pflanzen und durchbringen"
        // (agrikultur-obstbaum-pflanzen).
        // 62 -> 63 am 11.08.2026: "Champignons zuechten: Mist, Brut und Deckerde im Keller"
        // (agrikultur-pilzzucht).
        // 63 -> 64 am 11.08.2026: "Nutztiere im Winter: Futterrechnung und Auswahl"
        // (agrikultur-tiere-überwintern).
        // 64 -> 65 am 11.08.2026: "Seil pflegen und spleissen: halten, was man hat"
        // (agrikultur-seilpflege).
        // 65 -> 66 am 11.08.2026: "Poekeln nach Gewicht: Salz, Zucker und Salpeter in der richtigen Menge"
        // (agrikultur-poekelmengen).
        // 66 -> 67 am 11.08.2026: "Ein Huhn schlachten: der ganze Ablauf"
        // (agrikultur-huhn-schlachten).
        // 67 -> 68 am 11.08.2026: "Salz gewinnen: Sole sieden und Steinsalz erkennen"
        // (agrikultur-salz-gewinnen).
        // 68 -> 69 am 11.08.2026: "Sonnenblumen anbauen: fuer die eigene Oelsaat"
        // (agrikultur-sonnenblumen-anbauen).
        // 69 -> 70 am 20.08.2026: "Zucht ohne Inzucht: eine kleine Herde ueber Jahre halten"
        // (agrikultur-zucht-ohne-inzucht).
        // 70 -> 71 am 20.08.2026: "Verwaistes Lamm oder Zicklein aufziehen: Amme oder Flasche"
        // (agrikultur-jungtier-verwaist).
        // 71 -> 72 am 20.08.2026: "Ein Tier anbinden, ohne dass es sich erwuergen kann"
        // (agrikultur-tier-sicher-anbinden).
        // 72 -> 73 am 20.08.2026: "Ein groesseres Tier niederlegen und fuer eine Behandlung festhalten"
        // (agrikultur-tier-niederlegen-behandeln).
        // 73 -> 74 am 20.08.2026: "Sauren Boden erkennen und mit Kalk verbessern"
        // (agrikultur-boden-saeure).
        // 74 -> 75 am 20.08.2026: "Wenn der Frost zu frueh kommt: Beet schuetzen, Ernte retten"
        // (agrikultur-fruehfrost).
        // 75 -> 76 am 20.08.2026: "Moehre, Kohl und Ruebe ueberwintern fuer eigenes Saatgut"
        // (agrikultur-saatgut-ueberwintern).
        // 76 -> 77 am 20.08.2026: "Saatgut ueber Jahre halten und die Keimprobe machen"
        // (agrikultur-saatgut-lagern).
        // 77 -> 78 am 20.08.2026: "Frischkaese machen: dicklegen, abtropfen, salzen"
        // (agrikultur-frischkaese).
        // 78 -> 79 am 20.08.2026: "Wurst machen: Braet mischen, wuerzen, abfuellen"
        // (agrikultur-wurst-machen).
        // 79 -> 80 am 20.08.2026: "Talg auslassen: was aus Rinder- und Schaffett wird"
        // (agrikultur-talg-auslassen).
        // 80 -> 81 am 20.08.2026: "Was zuerst anbauen, wenn Flaeche und Kraft knapp sind"
        // (agrikultur-auswahl-womit-anfangen).
        // 81 -> 82 am 20.08.2026: "Kalorien und Eiweiss im Vergleich: was ein Kilo wirklich bringt"
        // (agrikultur-kalorien-eiweiss-vergleich).
        // 82 -> 83 am 20.08.2026: "Ausfallsicherheit vergleichen: was in einem schlechten Jahr noch steht"
        // (agrikultur-ausfallsicherheit-vergleich).
        // 83 -> 84 am 20.08.2026: "Was im ersten Jahr traegt, und was erst spaeter kommt"
        // (agrikultur-erstes-jahr-vs-spaeter).
        // 84 -> 85 am 20.08.2026: "Arbeit und Werkzeug: was mit der Hand zu schaffen ist"
        // (agrikultur-werkzeug-arbeit-vergleich).
        assertEquals(85, pack.agriculture.size)
        val ids = pack.tips.map { it.id }
        assertEquals(ids.toSet().size, ids.size, "doppelte Tipp-ID")
        val anleitungsIds = pack.guides.map { it.id }
        assertEquals(anleitungsIds.toSet().size, anleitungsIds.size, "doppelte Anleitungs-ID")
        val kapitelIds = pack.agriculture.map { it.id }
        assertEquals(kapitelIds.toSet().size, kapitelIds.size, "doppelte Kapitel-ID")
        for (tip in pack.tips) {
            assertTrue(tip.sources.isNotEmpty(), "Tipp ${tip.id} ohne Quelle")
        }
        // Eine Bauanleitung oder ein Kapitel ohne Quelle ist derselbe Fehler wie
        // ein Tipp ohne Quelle -- gepruceft wurde bisher nur der Tipp.
        for (anleitung in pack.guides) {
            assertTrue(anleitung.sources.isNotEmpty(), "Anleitung ${anleitung.id} ohne Quelle")
        }
        for (kapitel in pack.agriculture) {
            assertTrue(kapitel.sources.isNotEmpty(), "Kapitel ${kapitel.id} ohne Quelle")
        }
        assertEquals(
            setOf("wasser", "erste-hilfe", "medizin", "nahrung", "hinweis", "taktisch", "orientierung"),
            pack.tips.map { it.category }.toSet(),
        )
    }

    // Am 28.07.2026 stand in drei Tipps ein "Û" (U+00DB), wo ein "ß" hingehoert
    // -- ein Byte daneben, entstanden beim Einbau. Die Woerter sahen fast
    // richtig aus ("auÛerhalb"), und die Inhaltspruefung schlug nicht an, weil
    // "Û" ein regulaerer Buchstabe ist. Nebenwirkung in der Suche: "ß" wird zu
    // "ss" gefaltet, "Û" nicht -- die Woerter waren nicht mehr auffindbar.
    //
    // Diese Pruefung gilt nur fuer das deutsche Paket; das Paketformat selbst
    // muss jede Schrift tragen und darf so etwas nicht verbieten.
    @Test
    fun dasDeutschePaketEnthaeltKeineVerfaelschtenBuchstaben() {
        val paket = File(repoRoot(), "content/europe-de/paket")
        val pack = PackParser.parse(
            mapOf(
                "manifest.json" to File(paket, "manifest.json").readBytes(),
                "content/tips.json" to File(paket, "content/tips.json").readBytes(),
                "content/guides.json" to File(paket, "content/guides.json").readBytes(),
                "content/agriculture.json" to File(paket, "content/agriculture.json").readBytes(),
                "content/terms.json" to File(paket, "content/terms.json").readBytes(),
            ),
            emptySet(),
        ).pack ?: error("Paket laedt nicht")
        // Buchstaben mit Diakritika, die im Deutschen nicht vorkommen. Wer hier
        // auftaucht, ist fast immer ein verrutschter Umlaut oder ein Byte-Fehler.
        val fremd = "ÀÁÂÃÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕØÙÚÛÝÞàáâãåæçèéêëìíîïðñòóôõøùúûýþ"
        for (tip in pack.tips) {
            for ((feld, text) in listOf("title" to tip.title, "body" to tip.body)) {
                val treffer = text.filter { it in fremd }
                assertTrue(
                    treffer.isEmpty(),
                    "${tip.id}.$feld enthaelt \"$treffer\" — im Deutschen gibt es diese " +
                        "Buchstaben nicht, das ist ein verrutschter Umlaut",
                )
            }
        }
        // Und keine Auszeichnung, fuer die es keinen Renderer gibt: Sternchen
        // wuerden im Ernstfall als Zeichensalat im Text stehen.
        for (tip in pack.tips) {
            assertTrue(!tip.body.contains("**"), "${tip.id} enthaelt Sternchen-Auszeichnung")
        }
    }

    // Die Wiederbelebungs-Kette ist der lebenskritischste Inhalt im Paket. Wenn
    // eine dieser Angaben verloren geht oder sich still aendert, faellt es hier
    // auf und nicht erst im Ernstfall.
    @Test
    fun kernzahlenDerWiederbelebungStehenUnveraendertImPaket() {
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
        val pack = result.pack ?: error("Paket laedt nicht: ${result.problems}")
        val tipps = pack.tips.associateBy { it.id }

        val druck = tipps["erste-hilfe-herzdruckmassage"] ?: error("Tipp zur Herzdruckmassage fehlt")
        assertTrue(druck.body.contains("mindestens fünf und höchstens sechs Zentimeter"), "Drucktiefe fehlt")
        assertTrue(druck.body.contains("100 bis 120"), "Frequenz fehlt")

        val atmung = tipps["erste-hilfe-atmung-pruefen"] ?: error("Tipp zur Atemkontrolle fehlt")
        assertTrue(atmung.body.contains("zehn Sekunden"), "Grenze der Atemkontrolle fehlt")

        val ungeuebt = tipps["erste-hilfe-nur-druecken"] ?: error("Tipp fuer ungeuebte Helfer fehlt")
        assertTrue(ungeuebt.body.contains("ohne Beatmung"), "Kernaussage fuer Ungeuebte fehlt")
        // Die Regel "nur druecken" gilt NUR fuer Erwachsene. Bei einem Kind wird
        // zuerst beatmet -- steht das nicht dabei, fuehrt der Tipp in die Irre.
        assertTrue(ungeuebt.body.contains("Kind"), "der Hinweis auf Kinder fehlt")
        val kind = tipps["erste-hilfe-wiederbelebung-kind"] ?: error("Tipp zur Wiederbelebung bei Kindern fehlt")
        assertTrue(kind.body.contains("fünf Beatmungen"), "die fuenf Beatmungen fehlen")
        assertTrue(kind.body.contains("15 zu 2"), "das Verhaeltnis mit Kinder-Ausbildung fehlt")

        // Die Mengen der selbst angesetzten Trinkloesung sind so kernkritisch wie
        // die Drucktiefe: Wer zu viel Salz nimmt, schadet statt zu helfen, und
        // die Zahlen stehen nur an dieser einen Stelle im Paket.
        val loesung = tipps["erste-hilfe-trinkloesung-selbst-ansetzen"]
            ?: error("Tipp zur selbst angesetzten Trinkloesung fehlt")
        // Das Haushaltsmass ist hier wichtiger als die Gramm: Wer in dieser Lage
        // steckt, hat einen Loeffel und keine Waage.
        //
        // Achtung, hier lag am 28.07.2026 ein gefaehrlicher Fehler: Das
        // WHO-Handbuch von 2005 setzt 3 g Salz mit "one level teaspoonful"
        // gleich. Ein gestrichener Teeloeffel Salz wiegt aber deutlich mehr, und
        // die WHO selbst nennt in ihrer Anleitung fuer die Allgemeinheit
        // (WHO-EM/CSR/594/E) einen HALBEN Teeloeffel je Liter. Der Tipp hatte
        // das ganze Mass uebernommen und damit auf die doppelte Salzmenge
        // gefuehrt -- genau in die Richtung, vor der dieselbe Quelle warnt.
        assertTrue(loesung.body.contains("HALBEN gestrichenen Teelöffel"), "das halbe Teeloeffel-Mass fehlt")
        assertTrue(
            loesung.body.contains("nicht ein ganzer"),
            "die Abgrenzung gegen den ganzen Teeloeffel fehlt — sie ist der eigentliche Schutz",
        )
        assertTrue(loesung.body.contains("sechs Teelöffel Zucker"), "die Zuckermenge fehlt")
        assertTrue(loesung.body.contains("einen Liter"), "die Wassermenge fehlt oder ist nicht eindeutig")
        assertTrue(loesung.body.contains("3 Gramm Salz"), "die Grammangabe fuer Salz fehlt")
        // Der Vorrang der fertigen Loesung, den die WHO ausdruecklich setzt.
        assertTrue(loesung.body.contains("Vorrang"), "der Vorrang der fertigen ORS-Loesung fehlt")
        // Und die Warnung, was zu viel Salz anrichtet. Ohne sie ist die Angabe
        // nur eine Zahl, die man auch grosszuegig lesen kann.
        assertTrue(
            loesung.body.contains("Mehr Salz ist nicht besser"),
            "die Warnung vor zu viel Salz fehlt",
        )

        // Wo der GRC die Quelle ist, muss das Leitlinienjahr dranstehen. Sonst
        // liesse sich unbemerkt auf eine aeltere Ausgabe zurueckfallen, und die
        // Zahlen im Text saehen weiter richtig aus. Geprueft wird die Jahreszahl,
        // nicht eine Schreibweise: Die Poster heissen "Guidelines 2025", der
        // Taschenleitfaden "Reanimation 2025 - Leitlinien kompakt". Andere
        // Organisationen tragen ihre eigene Belegform (Dokument und Abrufdatum).
        val ersteHilfe = pack.tips.filter { it.category == "erste-hilfe" }
        assertTrue(ersteHilfe.size >= 47, "die Erste-Hilfe-Kategorie ist geschrumpft: ${ersteHilfe.size}")
        for (tip in ersteHilfe) {
            assertTrue(tip.sources.isNotEmpty(), "Tipp ${tip.id} ohne Quelle")
            for (quelle in tip.sources.filter { it.name.contains("GRC") }) {
                assertTrue(
                    quelle.detail.contains("2025"),
                    "Tipp ${tip.id} nennt den GRC ohne Leitlinienjahr",
                )
            }
        }
    }

    // Das Format erlaubt einen Eintrag OHNE Dringlichkeitsfeld -- ein aelteres
    // Paket kennt es noch nicht, und eines, das deswegen gar nicht mehr laedt,
    // waere schlimmer als eines ohne Kacheln. Fuer DIESES Paket ist das aber
    // ein Fehler: Ein Eintrag ohne Feld faellt aus dem Kachelgitter und steht
    // nur noch ueber die Suche da. Genau so sind am 29.07.2026 sechs
    // Orientierungs-Tipps doppelt gelandet, ohne dass ein Test es merkte.
    @Test
    fun jederEintragStehtUnterMindestensEinemDringlichkeitsfeld() {
        val paket = File(repoRoot(), "content/europe-de/paket")
        val pack = PackParser.parse(
            mapOf(
                "manifest.json" to File(paket, "manifest.json").readBytes(),
                "content/tips.json" to File(paket, "content/tips.json").readBytes(),
                "content/guides.json" to File(paket, "content/guides.json").readBytes(),
                "content/agriculture.json" to File(paket, "content/agriculture.json").readBytes(),
                "content/terms.json" to File(paket, "content/terms.json").readBytes(),
            ),
            emptySet(),
        ).pack ?: error("Paket laedt nicht")

        // Der Haftungshinweis ist die einzige Ausnahme: Er steht in der App
        // unter dem Kachelgitter und bewusst nicht darin.
        for (tip in pack.tips.filter { it.category != "hinweis" }) {
            assertTrue(tip.situations.isNotEmpty(), "Tipp ${tip.id} steht unter keinem Feld")
        }
        for (guide in pack.guides) {
            assertTrue(guide.situations.isNotEmpty(), "Anleitung ${guide.id} steht unter keinem Feld")
        }
        for (kapitel in pack.agriculture) {
            assertTrue(kapitel.situations.isNotEmpty(), "Kapitel ${kapitel.id} steht unter keinem Feld")
        }

        // Und keine leere Kachel: Ein Feld ohne Eintraege waere eine tote
        // Flaeche auf der Startseite.
        val belegt = (
            pack.tips.flatMap { it.situations } +
                pack.guides.flatMap { it.situations } +
                pack.agriculture.flatMap { it.situations }
            ).groupingBy { it }.eachCount()
        for (feld in Situations.ALLE) {
            assertTrue((belegt[feld.id] ?: 0) > 0, "Das Feld \"${feld.title}\" ist leer")
        }
    }

    // Die Angaben aus den Buchkapiteln, bei denen eine stille Aenderung im
    // Ernstfall schadet. Sie stehen wie die Wiederbelebungs-Kernzahlen jeweils
    // nur an EINER Stelle im Paket, und alle sind gegen eine Primaerquelle
    // gemessen -- nicht gegen das Buch, das an mehreren dieser Stellen andere
    // Zahlen nennt.
    @Test
    fun kernaussagenAusDenBuchkapitelnStehenUnveraendertImPaket() {
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
        val pack = result.pack ?: error("Paket laedt nicht: ${result.problems}")
        val tipps = pack.tips.associateBy { it.id }

        fun nenne(id: String, vararg stuecke: String) {
            val tip = tipps[id] ?: error("Tipp $id fehlt")
            for (stueck in stuecke) {
                assertTrue(tip.body.contains(stueck), "$id: \"$stueck\" fehlt")
            }
        }

        // Jod: Menge, Wartezeit und die beiden Grenzen. Das Buch nennt 12 bis 16
        // Tropfen je Gallone, also rund ein Drittel davon je Liter -- uebernommen
        // ist die Vorschrift mit der Feldflasche, weil sie die vorsichtigere ist.
        nenne(
            "wasser-jod-entkeimung",
            "5 Tropfen auf klares Wasser", "10 Tropfen", "30 Minuten",
            "KRYPTOSPORIDIEN", "Schilddrüsenerkrankung",
        )
        // Nasenspuelung: Ohne diesen Satz ist der Tipp eine Anleitung zu einer
        // seltenen, aber toedlichen Infektion.
        nenne(
            "medizin-nasenspuelung",
            "ABGEKOCHTEM, DESTILLIERTEM ODER STERILEM WASSER", "3 bis 5 Minuten", "24 Stunden",
        )
        // Erdbeben: die drei ueberholten Ratschlaege. Sie stehen in vielen
        // Handbuechern noch als richtig.
        nenne("taktisch-erdbeben", "TÜRRAHMEN", "NACH DRAUSSEN LAUFEN", "LIEGEN BLEIBEN")
        // Malaria: dass der Fiebertyp bei der gefaehrlichen Form NICHT traegt.
        nenne("medizin-malaria", "NICHT zu einem rhythmischen Wechselfieber", "7 TAGE")
        // Masken: das Ventil dreht den Fremdschutz um.
        nenne("medizin-maskenklassen", "UNGEFILTERT", "94 Prozent")
        // Lindan: die Warnung, nicht nur das Verbot.
        nenne("medizin-lindan", "KRAMPFANFÄLLE UND TODESFÄLLE", "50 Kilogramm")
        // Scheidenpilz: der Satz, der vor der falschen Selbstbehandlung schuetzt.
        nenne("medizin-scheidenpilz", "EIN DRITTEL", "SCHWANGERSCHAFT WIRD FLUCONAZOL NICHT")
        // Sterilisation: die Temperatur und die Ehrlichkeit dazu.
        nenne("hygiene-keimfrei-sauber", "30 Minuten bei 121 Grad", "SPOREN überleben")
    }
}
