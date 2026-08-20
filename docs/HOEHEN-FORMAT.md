# Höhenformat `.czh`

Stand 04.08.2026. Fassung 1. **Entworfen und gelesen, noch nicht befüllt** —
warum, steht unten unter „Offen".

Die Geländeform ist die sechste Datensorte aus `ROADMAP.md`. Sie kommt als
einzige nicht aus OpenStreetMap, sondern aus einem Höhenmodell, und sie liegt
deshalb in einer **eigenen Datei** neben der Karte.

## Warum eine eigene Datei und nicht eine weitere Schicht in der `.czk`

Zwei Gründe, und der zweite wiegt schwerer.

**Erstens technisch:** Höhen sind ein Raster, keine Linienzüge. Sie in ein
Format zu pressen, das für Kanten und Flächen gebaut ist, kostet ein Vielfaches
und bringt nichts.

**Zweitens rechtlich:** Die Kartendaten stammen aus OpenStreetMap und stehen
unter der Open Database License. Die Höhendaten stehen unter der Lizenz ihres
Anbieters — bei Copernicus mit einem wörtlich vorgeschriebenen Quellenhinweis,
der auf „all rights reserved" endet. Eine Lizenz mit Weitergabepflicht verlangt,
dass das GANZE abgeleitete Werk unter ihr steht; das können die Höhendaten nicht
leisten. In getrennten Dateien steht jede unter genau einer Lizenz, und keine
erbt die Auflagen der anderen. Einzelheiten in `LIZENZANFRAGEN.md`.

## Aufbau

Wie bei der `.czk`: Kopf, Kachelverzeichnis, gepackte Kacheln. Zahlen in Little
Endian, `varint` und `zigzag` wie dort.

### Kopf, 40 Bytes

| Versatz | Größe | Bedeutung |
|---:|---:|---|
| 0 | 8 | Kennung `CZHOEHE1` |
| 8 | 1 | Fassung, hier `1` |
| 9 | 1 | Kachelkante in Stützstellen als Zweierpotenz (z. B. `6` für 64) |
| 10 | 1 | kleinste Zoomstufe |
| 11 | 1 | größte Zoomstufe |
| 12 | 4 | westlichste Länge, Zehnmillionstel Grad |
| 16 | 4 | südlichste Breite |
| 20 | 4 | östlichste Länge |
| 24 | 4 | nördlichste Breite |
| 28 | 4 | Anzahl Kacheln |
| 32 | 8 | Versatz des ersten Kachelinhalts |

Das Kachelverzeichnis ist Byte für Byte dasselbe wie bei der `.czk`
(21 Bytes je Eintrag, sortiert nach Zoom/X/Y, binär durchsuchbar).

### Kachelinhalt, entpackt

```
uint8   Fassung des Kachelaufbaus, hier 1
int16   Grundhöhe in Metern (der kleinste Wert der Kachel)
uint8   Schrittweite in Metern je Einheit (1, 2, 5 oder 10)
        dann Kante × Kante Werte, zeilenweise von oben nach unten:
zigzag  Unterschied zum linken Nachbarn;
        der erste Wert jeder Zeile gegen denselben Punkt der Zeile darüber,
        der allererste gegen die Grundhöhe
```

**Warum Unterschiede und keine Absolutwerte:** Gelände ist glatt. Zwischen zwei
Stützstellen liegen meist wenige Meter Unterschied, und ein Zickzack-Varint
braucht dafür ein einziges Byte. Absolutwerte kosteten immer zwei.

**Warum eine Schrittweite:** Im Flachland genügt ein Meter, im Hochgebirge sind
zehn Meter Auflösung mehr, als eine Schummerung sichtbar macht. Die Schrittweite
steht je Kachel und macht aus einem Alpenquadrat mit 3000 Metern Spannweite
dieselbe Byte-Zahl wie aus einem Marschland.

**Das Raster reicht eine Stützstelle über die Kachel hinaus.** Die Kachel
selbst liegt auf den Stützstellen `1` bis `Kante−2`; `0` und `Kante−1` sind ein
Rand, der nur für die Neigungsrechnung da ist. Ohne ihn hat der äußerste Punkt
keinen Nachbarn, seine Neigung wird auf null gerechnet, und jede Kachel hellt
an ihrer Kante auf — nebeneinander ergibt das ein sichtbares Gitter über der
ganzen Karte. Am 04.08.2026 im Bildschirmfoto gesehen, nicht in einer Zahl.

Bei `Kante = 64` bleiben damit 62 nutzbare Stützstellen je Seite. Die
Abstände in der Tabelle unten sind darauf bezogen.

**Kein Wert für „unbekannt":** Ein Loch im Höhenmodell wird beim Bauen
interpoliert, nicht in die Datei getragen. Eine Karte, die stellenweise keine
Höhe kennt, müsste das anzeigen können — und ein Schummerungsbild mit Löchern
sieht aus wie ein Fehler im Gerät, nicht wie fehlende Daten.

## Obergrenzen beim Lesen

Die Zahlen müssen begrenzt sein: Eine erfundene Spannweite macht aus flachem
Land eine Wand, und eine erfundene Kantenlänge füllt den Speicher.

| Grenze | Wert | Grund |
|---|---|---|
| Kachelkante | 512 Stützstellen | größte Kante, die angenommen wird (512 × 512) |
| tiefster Wert | −500 m | das Tote Meer liegt bei −430 m |
| höchster Wert | 9000 m | der Mount Everest liegt bei 8849 m |
| Schrittweite | 1, 2, 5 oder 10 m | je Kachel, siehe oben |

Werte außerhalb sind kein Gelände, sondern ein Datenfehler oder eine
gefälschte Datei. Eine Datei, die sie reißt, wird nicht gelesen — die Karte
bleibt dann flach, was der App nichts ausmacht.

## Wie fein, und was das kostet

Gerechnet, nicht gemessen — es liegen noch keine Höhendaten vor:

| Stützstellenabstand | Stützstellen für Europa | erwartete Größe |
|---|---:|---:|
| 90 m | 1,23 Milliarden | rund 1,2 GB — zu groß |
| 250 m | 160 Millionen | rund 160 MB |
| **500 m** | **40 Millionen** | **rund 40 MB** |

Landfläche Europas mit 10 Millionen km² angesetzt, nach dem Varint-Verhalten
rund ein Byte je Stützstelle.

**Vorschlag:** 500 m für das Übersichtspaket, 90 m für Detailpakete je Region.
Eine Schummerung bei Zoomstufe 10 zeigt bei 500 m Abstand noch jeden Talzug;
feiner wird erst sichtbar, wenn man auch die Pfade sieht.

## Was die App damit macht

Die Höhen werden **nicht** als eigene Ebene gezeichnet, sondern beim Bauen des
Kachelbildes einmal in dieses hineingerechnet — als Schummerung unter allem
anderen. Das kostet keine Bildrate: Das Kachelbild entsteht ohnehin nur einmal
je Kachel (siehe `Kachelmaler`).

Im Sparmodus fällt die Schummerung ersatzlos aus. Sie ist eine Fläche, und
Flächen gibt es dort nicht.

## Offen — und warum hier Schluss ist

**Die Quelle ist noch nicht entschieden, weil es eine Lizenzfrage ist und
keine technische.** Zwei Modelle sind am 04.08.2026 geprüft worden, beide
Lizenztexte selbst geladen:

- **GMTED2010 (USGS)** ist für Europa ausgeschieden. Gemeinfrei ist der
  Bericht, nicht jeder Bildpunkt: Nördlich von 60°N in Eurasien stammen die
  Höhen aus NGA-DTED‑1, und Löcher wurden unter anderem mit SPOT 5 Reference3D
  gefüllt — co-produziert von Spot Image und IGN. Die Alpen haben SRTM-Löcher.
- **Copernicus WorldDEM‑30 / GLO‑90 (ESA)** erlaubt in Artikel 4 ausdrücklich
  Vervielfältigung, Verbreitung und Bearbeitung, kostenlos und unbefristet,
  verlangt dafür aber einen wörtlichen Quellenhinweis und die Weitergabe des
  Haftungsausschlusses.

Der Bau der Datei (`tools/karte/hoehen_bauen.py`) und die Schummerung im
Renderer werden gebaut, sobald die Quelle freigegeben ist. Format und Leser
stehen schon, weil sie von der Quelle unabhängig sind — jedes Höhenmodell
liefert dasselbe Raster.

## Unterschrift

    packsign hoehen-signieren --key NAME.secret --in HOEHEN.czh --out HOEHEN-signiert.czh
    packsign hoehen-pruefen   --in HOEHEN.czh [--keys LISTE.txt]

Derselbe Umschlag wie bei der `.czk`, mit eigener Kennung `CZH1`. Vor dem
Unterschreiben wird **jede Kachel entpackt und gelesen** — das Verzeichnis
prüft der Leser schon beim Öffnen, was in den Kacheln steht, sieht er nie. Eine
Kachel, die sich nicht lesen lässt, fällt in der App als fehlende Schummerung
auf, und das sieht aus wie flaches Land.

**Bis zum 18.08.2026 trug diese Datei bewusst keine Unterschrift.** Die
Begründung war: nur Zahlen, und einer gefälschten Schummerung folgt niemand —
anders als einem erfundenen Brunnen. Das stimmt für die Schummerung auch
weiterhin. Nur ist sie nicht mehr alles, was aus diesen Zahlen wird: Die
Höhenangabe unter dem Finger kommt aus derselben Datei, und danach entscheidet
jemand, ob er über einen Sattel geht oder außenherum. Als drei weitere Formate
einen Umschlag bekamen, war der vierte kein Aufwand mehr, sondern nur noch eine
Ausnahme, die man hätte erklären müssen. **Max hat sie deshalb aufgehoben.**

Eine Datei ohne Umschlag wird weiterhin gelesen und als unsigniert
gekennzeichnet — dieselbe Regel wie bei allen anderen. Näheres in
`docs/SIGNATUR-ZUSATZDATEIEN.md`.

**Auch die Beigabe im APK ist unterschrieben.** `androidApp/build.gradle.kts`
kopiert `work/karte/oesterreich-hoehen.czh` in die Assets; seit dem 18.08.2026
liegt dort die signierte Fassung, und die Kartenseite meldet dafür
„Geländeform: Signatur geprüft“.
