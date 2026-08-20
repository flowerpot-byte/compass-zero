# Grundregeln — Compass Zero

**Diese Regeln sind unumstößlich.** Sie dürfen NUR durch eine direkte, unmissverständliche Entscheidung der Projektleitung geändert werden — nicht durch Ableitung, Bequemlichkeit, Zeitdruck oder die Annahme "das ergibt hier mehr Sinn". Im Zweifel: Regel befolgen und nachfragen, nicht eigenständig abweichen.

## 1. Qualität vor Zeit und Kosten

Diese App kann in Extremsituationen über Leben und Tod entscheiden. Deshalb gilt für die gesamte Entwicklung:

- **Keine Kompromisse zugunsten von Coding-Zeit oder Coding-Kosten.** Wenn eine gründlichere, aber aufwändigere Lösung sicherer/verlässlicher ist, wird sie gewählt.
- **Keine Shortcuts.** Kein "funktioniert erstmal", kein TODO-Code in Release-Pfaden, keine übersprungene Fehlerbehandlung bei kritischen Funktionen.
- **Regelmäßige, kritische Selbstprüfung.** Arbeit wird gegengeprüft und selbst kritisch bewertet, nicht einfach als fertig gemeldet. Bugs müssen unbedingt vermieden werden — jeder Fehler ist im Zweifel ein Fehler mit potenziell lebensgefährlicher Konsequenz, kein kosmetisches Problem.
- Bei jeder sicherheits- oder inhaltskritischen Änderung: bewusst fragen "was passiert, wenn diese Information/Funktion im Ernstfall falsch oder kaputt ist?" — und danach handeln.

## 2. Kein Netzwerkcode — niemals, ohne Ausnahme

- Die App darf zu keinem Zeitpunkt Netzwerkzugriff haben. Keine Server-Calls, kein Tracking, keine Analytics-SDKs, keine Crash-Reporting-Dienste, kein "nur für Updates" o. Ä.
- Die Android-Berechtigung für Internetzugriff wird grundsätzlich NICHT im Manifest deklariert — das ist eine vom Betriebssystem erzwungene, für jeden nachprüfbare Garantie, kein bloßes Versprechen im Code.
- Inhalts-Pakete (Karten/Sprachen/Regionen) werden ausschließlich lokal eingespielt: vorinstalliert, manuell kopiert, oder per Geräte-zu-Geräte-Übertragung (Bluetooth/NFC/QR) — nie über das Internet.
- **WLAN-Direct und Wi-Fi Aware sind ausgeschlossen** (entschieden am 28.07.2026). Beide verlangen unter Android die Internet-Berechtigung, weil ihr Datenkanal ein gewöhnlicher IP-Socket ist — sie sind damit mit dieser Regel unvereinbar. Bluetooth ist der einzige Weg für ganze Pakete; NFC und QR nur für Kleinstdaten.

## 3. Handschrift: schlanker Code, nüchterner Auftritt

Grund: Die Zielgruppe prüft, wem sie glaubt. Ein Projekt, das über sich selbst redet statt über seine Belege, verliert genau dort.

- **Code-Stil:** Code liest sich, als habe ihn ein erfahrener Mensch geschrieben — schlank, keine Überkommentierung, keine generischen Namen, kein unnötiges Overengineering, keine Floskeln. Ein Kommentar erklärt, warum etwas so ist, nicht was die nächste Zeile tut.
- **Über die Werkstatt wird nicht berichtet.** Womit gearbeitet wurde, gehört weder in Kommentare noch in Commit-Nachrichten, READMEs, Metadaten oder Lizenzdateien. Wer das Projekt beurteilen will, beurteilt Code, Inhalt und Belege.
- Das Projekt tritt nach außen als Hobby-/Community-Projekt auf.

## 4. Verlässlichkeit der Inhalte vor allem

- Überlebenstipps, Bauanleitungen und der Landwirtschafts-/Zivilisations-Guide werden strukturell/durchsuchbar von uns entworfen, aber die **Fakten stammen aus geprüften, öffentlichen/anerkannten Quellen** und werden gegengecheckt, bevor sie in die App kommen.
- Im Zweifel: der konservativere, sicherere Rat wird gewählt, nicht der schnellere oder interessantere. **Das gilt für den Rat, nicht für die Tatsachen:** Eine Aussage, die der eigenen Quelle widerspricht, wird nicht dadurch zulässig, dass sie vorsichtig klingt.
- **Jede Quelle wird selbst geladen und im Volltext gelesen**, bevor etwas übernommen wird — nie aus der Zusammenfassung einer Suchmaschine oder eines Werkzeugs. (Anlass: Eine solche Zusammenfassung hatte Kohlendioxid und Kohlenmonoxid vermischt — zwei Gase mit gegensätzlichem Verhalten.)
- Recherchekanal ist frei wählbar (Firecrawl, Websuche, direkter Abruf). Entscheidend ist nicht das Werkzeug, sondern der vorstehende Punkt.
- Jede Quelle wird dokumentiert (Nachvollziehbarkeit, ähnlich der Drittlizenzen-Praxis bei Geoscout2.0). Die Belegangabe nennt Dokument und Abrufdatum bzw. Fassung — nicht nur den Namen der Organisation.
- **Zweiter Zweig „niemand kommt".** Endet ein Inhalt bei „Notruf", „bis der Rettungsdienst übernimmt" oder „zum Arzt", bekommt er einen zweiten Zweig für den Fall, dass niemand kommt — denn das ist die Grundannahme dieser App. Dieser Zweig wird im Text ausdrücklich als Einordnung gekennzeichnet („nicht aus der Quelle"), damit unterscheidbar bleibt, was belegt ist und was Abwägung.
- Die Erste-Hilfe-Kategorie trägt einen Hinweis, dass sie **keinen Erste-Hilfe-Kurs ersetzt**.
- **Die Suche ist Teil des Inhalts.** Ein Inhalt gilt erst als fertig, wenn gemessen wurde, worauf die naheliegenden Stichwörter führen. Ein Titel ist keine Überschrift, sondern das am stärksten gewichtete Suchfeld.

## 5. Sicherheitsarchitektur ist Kernprinzip, kein Zusatzfeature

- Inhalts-Pakete enthalten ausschließlich Daten (JSON/Text/Bilder) — niemals ausführbaren Code.
- Jedes Paket ist digital signiert; die App vertraut nur bekannten öffentlichen Schlüsseln. Unsignierte/unbekannt signierte Pakete werden klar als nicht verifiziert markiert.
- Geräte-zu-Geräte-Austausch (Bluetooth/NFC/QR) durchläuft dieselbe Signaturprüfung wie manuell kopierte Dateien — kein Sonderweg.
- **Schlüssel-Widerruf** (entschieden am 28.07.2026): Zwei Schlüsselrollen. Ein Wurzelschlüssel wird offline aufbewahrt und unterschreibt ausschließlich Erklärungen, nie Inhalte; Inhaltsschlüssel unterschreiben Pakete. In der App eingebaut ist nur der Wurzelschlüssel. Erklärungen sind kleine signierte Dateien eigener Art (`.czs`, gleicher Envelope wie `.czp`, anderes Magic) und reisen dieselben Wege wie Pakete. Drei Arten: Schlüssel benennen, Schlüssel entziehen, Paket zurückrufen. Der Zustand wächst nur in eine Richtung — es gibt kein „Entzug aufheben". Inhalte eines entzogenen Schlüssels bleiben **lesbar**, mit dauerhafter, nicht wegklickbarer Warnung; sie werden nicht gelöscht.
- **Rückstufungs-Schutz** (entschieden am 28.07.2026): Die App merkt sich je Paket-Kennung die höchste je angenommene Version. Die Marke steigt nur und überlebt das Löschen des Pakets. Ältere Versionen werden abgelehnt, mit ausdrücklichem Notausgang für den Fall einer defekten neuen Fassung; der Notausgang senkt die Marke nicht. Kein Zeitstempel als Anker.
- Veröffentlichte APK-Releases sind signiert und mit SHA-256-Prüfsummen versehen, damit Echtheit auch offline überprüfbar ist.
- Builds sind reproduzierbar, damit Dritte den Quellcode gegen die veröffentlichte APK verifizieren können.
- Nur die technisch zwingend nötigen Berechtigungen werden angefragt.

## 6. Datenschutz absolut

- Null Datensammlung. Kein Tracking, keine Analytics, keine Diagnosedaten, keine Telemetrie — unter keinen Umständen, auch nicht anonymisiert oder optional.
