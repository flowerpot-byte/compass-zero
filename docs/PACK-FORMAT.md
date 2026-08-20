# Paketformat `.czp`

Inhalts-Pakete für Compass Zero sind einzelne signierte Datendateien. Sie enthalten ausschließlich Daten (JSON, Bilder), niemals ausführbaren Code. Jedes Paket ist mit Ed25519 signiert und komplett offline prüfbar. Dieses Dokument ist die verbindliche Referenz für App, Werkzeuge und Paketautoren.

## Envelope

Alle Mehrbyte-Zahlen sind Big-Endian und vorzeichenlos.

| Offset | Größe | Feld |
|---|---|---|
| 0 | 4 | Magic `43 5A 50 31` ("CZP1") |
| 4 | 2 | Formatversion, aktuell `1` |
| 6 | 32 | Ed25519-Public-Key des Signierers (roh) |
| 38 | 64 | Ed25519-Signatur |
| 102 | 8 | Payload-Länge in Bytes |
| 110 | … | Payload (ZIP-Container) |

Die signierte Nachricht ist `Magic ‖ Version ‖ Signierer-Key ‖ Payload-Länge ‖ Payload`, also der gesamte Header ohne das Signaturfeld, gefolgt vom Payload. Damit ist jedes Feld, das das Verhalten der App beeinflusst, von der Signatur gedeckt.

Regeln:

- Die Payload-Länge muss exakt `Dateigröße − 110` sein. Angehängte oder abgeschnittene Bytes machen die Datei ungültig ("beschädigt"), nicht "teilweise lesbar".
- Payload-Längen über 2 000 000 000 Bytes werden abgelehnt (Altgeräte-Speicher, Int-sichere Verarbeitung).
- Formatversion ≠ 1 ergibt "nicht unterstützt" mit Versionsangabe; die App rät niemals, wie zukünftige Versionen zu lesen wären.

## Prüfablauf und Urteile

Ein Paket wird in **einem einzigen Lesedurchlauf** geöffnet. Dieselben Bytes fließen gleichzeitig in die Signaturprüfung und in den Entpacker: Inhaltsdateien werden dabei in den Speicher gelesen, Bilder nur gezählt und gehasht. Erst wenn die Signatur am Ende des Durchlaufs hält, wird der gelesene Inhalt herausgegeben.

Das ist der Kern der Manipulationssicherheit. Ein Ablauf aus "erst prüfen, dann von der Platte nachlesen" wäre angreifbar: zwischen Prüfung und Nutzung ließe sich der Inhalt derselben Datei austauschen, und die App würde manipulierte Überlebenshinweise als geprüft anzeigen. Deshalb gibt es keinen Weg, den Payload nach der Prüfung ein zweites Mal von der Platte zu lesen.

| Urteil | Bedeutung | Inhalt verfügbar |
|---|---|---|
| `Trusted` | Signatur gültig, Schlüssel im Vertrauensspeicher | ja |
| `UnknownSigner` | Signatur in sich gültig, Schlüssel unbekannt | ja, mit deutlicher Warnung in der Oberfläche |
| `BadSignature` | Signatur passt nicht zum Inhalt | **nein, niemals** |
| `Unsupported` | neuere Formatversion | nein |
| `Damaged` | Datei oder Container unbrauchbar (siehe Ursachen unten) | nein |
| `Aborted` | Prüfung wurde abgebrochen (z. B. Nutzer verlässt den Bildschirm) | nein |

Reihenfolge der Urteile: Eine ungültige Signatur schlägt jede Container-Diagnose. Ein manipuliertes Paket wird also als "manipuliert" gemeldet und nicht als "beschädigt" — sonst sähe ein Angriff wie ein Übertragungsfehler aus.

Unbekannt signierte Pakete werden nicht still abgelehnt und nicht still angenommen, sondern klar als "nicht verifiziert" markiert.

**Schadensursachen** (`Damage.kind`) sind eine feste Aufzählung, damit die Oberfläche je Ursache eine eigene, übersetzte Meldung zeigen kann: `TooShort`, `MagicMismatch`, `SizeMismatch`, `PayloadTooLarge`, `Unreadable`, `ContainerBroken`, `EntryNameForbidden`, `DuplicateEntry`, `TooManyEntries`, `EntryTooLarge`, `PackTooLarge`, `NoEntries`, `HashMismatch` (ein nachgeladenes Bild passt nicht zu seiner Prüfsumme aus dem signierten Durchlauf).

Ein Sonderfall bleibt unvermeidbar: Wird die Versionsnummer im Header manipuliert, meldet die App `Unsupported`, bevor sie die Signatur prüfen kann. Der Hinweistext muss deshalb offenlassen, dass auch eine Manipulation die Ursache sein kann — eine Offline-App kann kein Update nachladen.

## Vertrauensmodell

- Der Vertrauensspeicher enthält benannte Ed25519-Public-Keys (32 Bytes roh). Verglichen werden immer die vollen Schlüsselbytes, nie nur ein Hash.
- Der Fingerabdruck eines Schlüssels ist SHA-256 über die 32 rohen Schlüsselbytes, klein geschrieben in Hex (64 Zeichen). Er dient Menschen zum Abgleich, nicht der Software als Identität.
- Doppelte Schlüssel im Vertrauensspeicher sind ein Konfigurationsfehler und werden abgelehnt.

Bekannte Lücken, für die noch eine Entscheidung fehlt (siehe [`../ROADMAP.md`](../ROADMAP.md)): Es gibt keinen Widerruf für einen kompromittierten Schlüssel außer einer neuen App-Version, und ein altes, gültig signiertes Paket verifiziert unverändert, auch wenn seine Inhalte inzwischen korrigiert wurden.

## Bilder und ihre Prüfsummen

Bilder werden nicht in den Speicher geladen — auf alten Geräten wäre das nicht tragbar. Für jedes Bild hält die App aus dem signierten Durchlauf Name, entpackte Größe und SHA-256-Prüfsumme. Beim späteren Anzeigen muss die geladene Bilddatei gegen diese Prüfsumme geprüft werden; nur so gilt auch für Bilder derselbe Manipulationsschutz wie für Texte.

## Payload-Container (ZIP)

Erlaubte Einträge:

- `manifest.json` (genau einmal, Pflicht)
- `content/…` (Inhaltsdateien, JSON)
- `assets/…` (Bilder)

**Der Container darf keine Bytes enthalten, die zu keinem Eintrag gehören.** Kein Kommentar, keine Zusatzfelder in den Einträgen, keine Größenangaben in einem Nachspann hinter den Daten, nichts hinter dem ZIP-Verzeichnis. Solche Nischen liest kein Werkzeug an, sie trügen aber die Unterschrift des Maintainers — ein bequemer Weg, beliebige Daten unter fremdem Vertrauen weiterzureichen. `packsign sign` rechnet den Aufbau nach und verweigert die Unterschrift, wenn etwas nicht aufgeht.

Namensregeln: nur `A–Z a–z 0–9 . _ - /`, maximal 200 Zeichen, keine leeren Segmente, kein `.` oder `..`, kein führender Schrägstrich, kein Backslash. **Jeder Namensteil muss mit einem Buchstaben oder einer Ziffer beginnen** — damit fallen Einträge ohne Dateinamen (`content/.json`) und versteckte Dateien (`.gitkeep`) heraus; solche Namen entstehen nicht beim Zusammenstellen von Inhalten, sondern beim Basteln am Paket. Verzeichniseinträge werden ignoriert. Doppelte Namen, Einträge außerhalb der erlaubten Wurzeln oder ein Container ohne Einträge gelten als beschädigt.

**Deklarierte ZIP-Größenfelder werden grundsätzlich ignoriert.** Gezählt werden die tatsächlich entpackten Bytes gegen harte Grenzen (Schutz vor Zip-Bomben):

| Grenze | Wert | Grund |
|---|---|---|
| Einträge je Inhaltsdatei | `tips` und `guides` je 5 000 · `agriculture` 200 Kapitel · `pois` 10 000 · `phrases` 500 | Speicher und Laufzeit auf Altgeräten. **Eine Grenze „pro Paket" gibt es nicht** — geprüft wird Datei für Datei (`Tips.kt`, `Guides.kt`, `Pois.kt`) |
| Bytes pro Eintrag (entpackt) | 8 MiB | ein Eintrag wird beim Lesen vollständig im Speicher gehalten; alte Geräte haben oft nur 96–128 MB Heap |
| Inhaltsdateien zusammen (entpackt) | 24 MiB | so viel darf gleichzeitig im Speicher liegen |
| Bytes gesamt (entpackt) | 512 MiB | Obergrenze für den gesamten Durchlauf |
| JSON-Datei | 4 MiB | wird komplett geparst |
| JSON-Einzelwerte je Datei | 450 000 | eine kleine Datei mit Millionen winziger Werte braucht sonst hunderte Megabyte Speicher. Gezählt werden Zeichenketten, geöffnete Klammern und Trennkommas — ein POI mit Name und Notiz kostet rund 19 Einzelwerte |
| JSON-Verschachtelungstiefe | 60 | Schutz vor Stapelüberlauf durch feindliches JSON |
| Durchsuchbarer Text je Paket | 4 000 000 Zeichen | dieser Text liegt beim Suchen vollständig im Speicher; die Zahl ist auf einem Gerät mit 96 MB nachgemessen |
| Wortvorkommen im Suchindex je Paket | 500 000 | das Zeichenbudget allein schützt nicht: viele kurze, verschiedene Wörter bleiben weit darunter und sprengen trotzdem das Wortverzeichnis, weil dort jedes Wort einen eigenen Eintrag kostet. Am 20.08.2026 neu gemessen, und zwar im TEUERSTEN Fall: Ein Paket mit 602 400 Wortvorkommen, bei dem JEDES WORT VERSCHIEDEN ist, lädt und indiziert bei 96 MB Heap; darüber greift die JSON-Größengrenze zuerst. Das echte Europa-Paket ist weit billiger — 448 572 Vorkommen, aber nur 31 026 verschiedene Wörter, jedes im Schnitt 14,5 mal. 500 000 bleibt rund ein Sechstel unter dem gemessenen schlimmsten Fall. Vorher 450 000 (17.08.2026), davor 300 000 |
| Verlängerung beim Vereinheitlichen | höchstens 4-fach plus 512 Zeichen | manche Zeichen werden beim Zusammenziehen für die Suche länger — eine arabische Ligatur wird zu achtzehn Zeichen. Ein Feld, das dabei stärker wächst, ist kein Fließtext, sondern ein Speicherangriff auf den Suchindex |

Eine lange Prüfung ist abbrechbar: Wird der Thread unterbrochen, endet der Durchlauf mit `Aborted`, statt das Gerät weiter zu beschäftigen.

## Inhaltsdateien

Gemeinsame Regeln:

- Jede Datei trägt ein Feld `schema`; diese Spezifikation beschreibt `schema: 1`. Andere Werte lehnt die App für die jeweilige Datei ab.
- Unbekannte JSON-Felder werden ignoriert (Vorwärtskompatibilität).
- Die Datei muss gültiges UTF-8 sein. Kaputte Bytes werden abgelehnt, nicht stillschweigend durch Ersatzzeichen ersetzt — sonst stünden unlesbare Stellen mitten in einem Überlebenstext.
- **Doppelte Schlüssel im selben JSON-Objekt sind verboten.** Wer die Datei prüft, liest sonst den ersten Wert, während die App den zweiten verwendet. Genau so ließe sich ein geprüfter Inhalt nachträglich umdeuten.
- **Texte müssen lesbar sein.** Ein Feld gilt nur als ausgefüllt, wenn es mindestens zwei Buchstaben oder Ziffern enthält und keine unsichtbaren Zeichen (Nullbreiten-Zeichen, Schreibrichtungs-Steuerzeichen, weiche Trennstriche, Ersatzzeichen, Steuerzeichen). Ohne diese Regel ließe sich die Quellenpflicht mit einem einzigen unsichtbaren Zeichen aushebeln: ein Tipp sähe geprüft aus und wäre in Wahrheit leer.
- Ein fataler Fehler irgendwo im Paket verhindert das Laden des gesamten Pakets; gemeldet werden trotzdem alle gefundenen Probleme auf einmal.
- **Quellenpflicht:** Tipps, Bauanleitungen und Guide-Kapitel müssen mindestens eine dokumentierte Quelle (`sources`) tragen. Wissen ohne Quelle kommt nicht in die App.
- Kennungen (`id`): Kleinbuchstaben/Ziffern/Bindestrich, beginnend mit Buchstabe oder Ziffer, max. 80 Zeichen, eindeutig je Datei.
- Bildverweise: exakt `assets/<dateiname>`; verwiesene Bilder müssen im Paket liegen (sonst Warnung), unbenutzte Bilder erzeugen eine Warnung.

### `manifest.json`

| Feld | Pflicht | Regel |
|---|---|---|
| `schema` | ja | `1` |
| `id` | ja | Paket-Kennung, Punktnotation (`org.compasszero.base.de`), max. 120 Zeichen |
| `version` | ja | ganze Zahl ≥ 1, höher = neuer |
| `language` | ja | Sprachcode wie `de`, `en`, `de-AT` |
| `title` | ja | Anzeigename, max. 200 Zeichen |
| `created` | nein | Erstellzeit in Millisekunden seit 1970, `0` = unbekannt (kein Zukunfts-Check: offline gibt es keine verlässliche Uhr) |
| `kinds` | ja | Liste aus `tips`, `guides`, `agriculture`, `pois`, `phrases`; unbekannte Werte werden mit Warnung übersprungen |

```json
{"schema":1,"id":"org.compasszero.base.de","version":1,"language":"de","title":"Basispaket Deutsch","kinds":["tips","guides","agriculture","pois"]}
```

Für jeden bekannten Eintrag in `kinds` muss `content/<kind>.json` im Paket liegen. Nicht deklarierte `content/`-Dateien werden mit Warnung ignoriert.

### `content/tips.json`

Tipp-Felder: `id`, `title` (≤200), `category` (Kleinbuchstaben/Ziffern/Bindestrich, ≤40), `body` (Fließtext ≤64 000), `keywords` (optional, ≤20 Stück, je ≤40), `group` (optional, Kennung einer Themengruppe), `image` (optional, Pfad einer Zeichnung im Paket), `sources` (Pflicht, 1–10; `name` ≤120, `detail` Pflicht und ≤500). `detail` nennt Dokument und Abrufdatum bzw. Fassung — ohne diese Angabe lässt sich eine Aussage nicht zurückverfolgen, deshalb ist ein Paket ohne `detail` ungültig. Maximal 5 000 Tipps je Datei.

**Zeichnung am Tipp.** `image` verweist wie bei Anleitungsschritten und Kapitelabschnitten auf eine Datei im Paket (`assets/…`). Fehlt die Datei, meldet der Parser `asset-missing`; ein Bild, auf das niemand verweist, meldet er als `asset-unused`. Die App stellt die Zeichnung ÜBER den Text — bei Tipps von mehreren tausend Zeichen fände sie darunter niemand, der sie im Ernstfall braucht.

**Absätze im Fließtext.** In `body` (Tipp und Kapitelabschnitt), `summary` und `text` eines Anleitungsschritts ist der Zeilenvorschub `U+000A` erlaubt; eine Leerzeile trennt zwei Absätze. Höchstens zwei Umbrüche am Stück — mehr ist kein Absatz mehr, sondern Füllung, mit der sich ein Eintrag optisch leeren ließe, ohne dass die Längengrenze anschlägt. **Nur `U+000A`**: Wagenrücklauf, `U+2028`, `U+2029`, Vertikaltabulator und Seitenvorschub bleiben verboten, weil sie im Text gleich aussehen, sich beim Rendern aber unterschiedlich verhalten — mehrere Schreibweisen für denselben Umbruch wären eine Quelle stiller Unterschiede zwischen Paketen.

In allen anderen Feldern bleibt der Umbruch verboten: Titel, Schlagwörter, Namen, Material- und Werkzeugangaben und die Belegangabe einer Quelle stehen einzeilig in Listen. Entschieden am 29.07.2026 durch Max, weil ein medizinischer Tipp von zweitausend Zeichen in einem einzigen Absatz unter Stress kaum zu erfassen ist.

Der Titel ist nicht nur eine Überschrift, sondern das am stärksten gewichtete Suchfeld (Titel 5, `keywords` 3, Fließtext 1, plus 1 bei genauer Wortgleichheit). Verglichen werden Wortanfänge, nicht Wortteile: `ersticken` findet *Erstickungsgefahr* nicht. Wörter, die aus einem Titel fallen, gehören deshalb in `keywords`, sonst verliert der Eintrag den Treffer.

**Themengruppen.** `groups` ist optional und ordnet nur die Ansicht: die Suche kennt Gruppen nicht, und ein Paket ohne sie bleibt gültig. Gruppen-Felder: `id`, `title` (≤200), `category`. Die Reihenfolge der Gruppen in der Liste ist ihre Anzeigereihenfolge — sie ist nach Dringlichkeit gesetzt und lässt sich weder aus dem Namen noch aus der Zahl der Tipps ableiten. Innerhalb einer Gruppe gilt die Reihenfolge der Tipps in `tips`. Maximal 200 Gruppen.

Zwei Fälle sind fatal, nicht nur Warnungen: ein `group`, das in `groups` nicht vorkommt (`group-unknown`), und eine Gruppe aus einer anderen Kategorie als der Tipp (`group-category-mismatch`). Im ersten Fall fiele der Tipp aus der Kategorie-Ansicht und wäre nur noch über die Suche zu finden, im zweiten stünde er unter einer Überschrift, die etwas anderes ankündigt. Ein Tipp ganz ohne `group` ist dagegen erlaubt und erscheint am Ende seiner Kategorie.

```json
{"schema":1,
 "tips":[{"id":"wasser-abkochen","title":"Wasser abkochen","category":"wasser","body":"…","keywords":["trinkwasser"],"group":"wasser-aufbereiten","sources":[{"name":"…","detail":"…"}]}],
 "groups":[{"id":"wasser-aufbereiten","title":"Wasser aufbereiten","category":"wasser"}]}
```

### `content/guides.json`

Anleitungs-Felder: `id`, `title`, `category`, `summary` (≤1 000), `materials` (optional, ≤100; `item` ≤120, `amount` ≤40, `note` ≤500), `tools` (optional, ≤40 Stück, je ≤80), `steps` (Pflicht, 1–100; `text` ≤4 000, optional `image`, optional `warning` ≤500), `difficulty` (1–3), `sources` (Pflicht). Nicht jeder Ablauf braucht Material — die stabile Seitenlage etwa braucht nur zwei Hände. Die Schritte sind der Inhalt und bleiben Pflicht.

```json
{"schema":1,"guides":[{"id":"wasserfilter","title":"Wasserfilter bauen","category":"wasser","summary":"…","materials":[{"item":"…","amount":"…"}],"steps":[{"text":"…","image":"assets/filter1.png","warning":"…"}],"difficulty":2,"sources":[{"name":"…","detail":"…"}]}]}
```

### `content/agriculture.json`

Kapitel-Felder: `id`, `title`, `sections` (Pflicht, 1–100; `heading` ≤200, `body` ≤64 000, optional `image`), `sources` (Pflicht). Maximal 200 Kapitel.

```json
{"schema":1,"chapters":[{"id":"boden","title":"Boden vorbereiten","sections":[{"heading":"…","body":"…"}],"sources":[{"name":"…","detail":"…"}]}]}
```

### `content/pois.json`

Kopf: `attribution` (Pflicht, ≤500; Herkunftsvermerk des Datensatzes, z. B. Lizenzhinweis). POI-Felder: `id`, `kind` (`water`, `viewpoint`, `waypoint`, `shelter`), `lat`/`lon` (endlich, −90…90 / −180…180), optional `name` (≤120), `note` (≤500), `elevation` (−500…9000 m). Maximal 10 000 POIs je Paket — diese Zahl ist mit der Grenze für JSON-Einzelwerte abgestimmt und auf einem Gerät mit 96 MB Arbeitsspeicher nachgemessen. Größere Gebiete werden auf mehrere Pakete aufgeteilt.

Unbekannte `kind`-Werte werden mit Warnung übersprungen (neue Paketarten sollen alte Apps nicht brechen). Kaputte Koordinaten sind dagegen fatal: das ist ein Datenfehler, kein Zukunftsmerkmal.

```json
{"schema":1,"attribution":"…","pois":[{"id":"q1","kind":"water","lat":47.42,"lon":13.05,"name":"Quelle am Weg","elevation":1180}]}
```

### `content/phrases.json`

Phrasenkatalog für den Übersetzer-Bereich: kurze, vorformulierte Sätze zum Zeigen statt zum Vorlesen, gebündelt in Themengruppen. Anders als bei den übrigen Inhaltsarten trägt jedes Text- und Titelfeld hier keinen einzelnen String, sondern eine Übersetzung je Sprache.

Kopf: `languages` (Pflicht, 1–16 Sprachcodes wie bei `manifest.language`, keine Doppelten). Gruppen-Felder: `id`, `title` (je deklarierter Sprache ≤200), `sources` (Pflicht, wie bei den übrigen Arten), `phrases` (Liste). Phrasen-Felder: `id` (eindeutig über die ganze Datei, nicht nur je Gruppe), `text` (je deklarierter Sprache ≤300), `note` (optional, darf eine Teilmenge der Sprachen abdecken, je vorhandener Sprache ≤500).

Für `title` und `text` muss **jede** in `languages` deklarierte Sprache vorhanden und lesbar sein — eine halb übersetzte Zeile würde Verlässlichkeit vortäuschen, die sie nicht hat. Ein Sprachschlüssel, der nicht in `languages` steht, führt nur zu einer Warnung (Vorwärtskompatibilität). Maximal 40 Gruppen und zusammen maximal 500 Phrasen je Datei.

Phrasen fließen bewusst nicht in den Suchindex und nicht in die Prüfung der durchsuchbaren Textmenge ein: Ein Satz wie "Haben Sie Schmerzen?" würde in der Trefferliste neben den Notfall-Tipps stehen und deren Treffer verdrängen. Ob Phrasen später doch durchsuchbar werden sollen, ist offen und wird erst am fertigen Katalog entschieden.

```json
{"schema":1,"languages":["de","en","fr"],"groups":[{"id":"grunddaten","title":{"de":"Zur Person","en":"About you","fr":"Vous concernant"},"sources":[{"name":"…","detail":"…"}],"phrases":[{"id":"ja-nein","text":{"de":"Ja. Nein. Ich weiß nicht.","en":"Yes. No. Don't know.","fr":"Oui. Non. Je ne sais pas."},"note":{"de":"Zum Zeigen."}}]}]}
```

## Pakete erzeugen und signieren

Das Werkzeug `tools/packsign` (im Repo) erledigt alle Schritte:

```
packsign keygen --dir keys --name maintainer
packsign pack   --in inhalte/ --out payload.zip
packsign sign   --key keys/maintainer.secret --in payload.zip --out paket.czp
packsign verify --in paket.czp --keys vertraut.txt
```

- **`sign` prüft das fertige Paket, bevor es liegen bleibt:** es liest es genau so, wie die App es lesen würde, und verwirft es, wenn der Inhalt abgelehnt würde. Ein von Hand gebauter Payload kann die Prüfung also nicht umgehen, indem er `pack` überspringt. Ein Werkzeug, das kaputte Überlebenshinweise signiert, wäre schlimmer als keins — die Unterschrift gäbe dem Fehler Glaubwürdigkeit.
- `sign` zeigt den Fingerabdruck des tatsächlich verwendeten Schlüssels an, damit eine Verwechslung sofort auffällt, und weist eine `.public`-Datei ab.
- Der ZIP-Bau ist deterministisch: Einträge nach Namen sortiert, feste Zeitstempel (2000-01-01, zeitzonenunabhängig), Kompressionsstufe 9, keine Verzeichniseinträge, Betriebssystem-Müll wird übersprungen, symbolische Verknüpfungen und Windows-Junctions werden nicht verfolgt (sonst wanderten Dateien von außerhalb des Inhaltsordners unbemerkt ins signierte Paket). Harte Verknüpfungen sind vom echten Pfad nicht unterscheidbar; dagegen schützt, dass jede Datei im Paket auch verwendet werden muss — unbenutzte Bilder und nicht im Manifest angemeldete Inhaltsdateien lehnt das Werkzeug ab. Gleicher Eingabeordner ergibt Byte-identische Pakete und damit gleiche Signaturen; die Deflate-Ausgabe ist je JDK-Generation stabil, für reproduzierbare Pakete wird die JDK-Hauptversion mit dokumentiert.
- `*.secret`-Dateien (Seeds) niemals committen oder weitergeben; `keys/` und `*.secret` sind in diesem Repo git-ignoriert.
- `verify` meldet Signaturstatus, Signierer bzw. Fingerabdruck und den Container-Inhalt; Exit-Code 0 nur bei vertrauenswürdiger Signatur.
