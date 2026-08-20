# Wegenetz `.czw`

Stand 18.08.2026. Fassung 1. Gebaut von `tools/karte/wege_bauen.py`.

Das Netz für die Routenberechnung: Knoten, Kanten, Längen. Getrennt von der
Karte, weil es etwas anderes ist — die Karte zeigt, wo ein Weg **liegt**, das
Netz weiß, wo er **hinführt**.

## Warum aus den Rohdaten und nicht aus der fertigen Karte

Naheliegend wäre, das Netz aus der `.czk` zu gewinnen; die Wege stehen ja
darin. Es geht nicht, und der Grund ist keine Bequemlichkeit:

Die `.czk` speichert Wege als **Striche je Kachel** — an den Kachelgrenzen
geschnitten, vereinfacht, ohne Knotenkennungen. Woher ein Weg kommt und wo er
einen anderen **kreuzt**, steht dort nicht. Zwei Striche, die sich auf dem Bild
berühren, können eine Brücke und ein Bach sein.

Ein Wegenetz daraus zu erraten hieße, **Abzweigungen zu erfinden**. Und eine
erfundene Abzweigung schickt jemanden in eine Richtung, aus der er umkehren
muss — im Gebirge, mit Gepäck, womöglich im Dunkeln.

In den Rohdaten steht beides ausdrücklich: Dieselbe Knotenkennung in zwei Wegen
bedeutet eine echte Kreuzung, eine verschiedene bedeutet keine.

## Was als begehbar gilt

| Art | Aufschlag | Art | Aufschlag |
|---|---:|---|---:|
| `path`, `footway`, `pedestrian` | 1,0 | `service` | 1,4 |
| `track`, `bridleway`, `cycleway` | 1,1 | `tertiary` | 1,5 |
| `living_street` | 1,2 | `secondary` | 1,8 |
| `residential`, `unclassified` | 1,3 | `primary` | 2,2 |
| `steps` | 1,6 | | |

**Der Aufschlag ist ein Faktor auf die Länge, kein Zeitwert.** Ein Wanderweg
zählt so lang, wie er ist; eine Landstraße zählt das Anderthalbfache, weil man
sie zu Fuß meiden will — **ohne sie zu verbieten**. Wer keinen anderen Weg hat,
bekommt sie trotzdem vorgeschlagen.

**Ausdrücklich nicht dabei:** Autobahnen und Schnellstraßen (`motorway`,
`trunk` samt Auffahrten). Sie sind zu Fuß nicht begehbar, und ein Weg darüber
wäre kein unbequemer Vorschlag, sondern ein lebensgefährlicher.

Wege mit `foot=no` oder `access=no` fallen weg. In der Probe um Salzburg waren
das 20 684 Stück.

## Aufbau der Datei

Zahlen in Little Endian.

### Kopf, 40 Bytes

| Versatz | Größe | Bedeutung |
|---:|---:|---|
| 0 | 8 | Kennung `CZWEG001` |
| 8 | 1 | Fassung, hier `1` |
| 9 | 3 | frei, `0` |
| 12 | 4 | Anzahl Knoten (`uint32`) |
| 16 | 4 | Anzahl Kanten (`uint32`) |
| 20 | 4 | westlichste Länge, Zehnmillionstel Grad (`int32`) |
| 24 | 4 | südlichste Breite |
| 28 | 4 | östlichste Länge |
| 32 | 4 | nördlichste Breite |
| 36 | 4 | frei |

### Knoten

Ein Eintrag je Knoten, je 8 Bytes: `int32` Länge, `int32` Breite. Die Nummer
eines Knotens ist seine Stelle in dieser Liste.

### Versatztabelle der Kanten

Ein `uint32` je Kante: wo sie in der Datei beginnt.

### Kanten

```
uint32  Knoten A
uint32  Knoten B
uint16  Aufschlag mal 100 (100 = 1,0)
uint32  Länge in Metern
uint16  Anzahl Stützpunkte
  je Punkt: int32 Länge, int32 Breite
```

Die Stützpunkte sind die **volle Geometrie** zwischen A und B, damit sich eine
berechnete Route zeichnen lässt — eine Route, die als gerade Linie über einen
Berg gezogen wird, sieht aus wie ein Vorschlag und ist keiner.

**Kanten sind in beide Richtungen begehbar.** Einbahnstraßen gelten für
Fahrzeuge; zu Fuß gibt es sie nicht.

## Was die Datei bewusst nicht enthält

**Keine Nachbarschaftstabelle.** Wer welche Kanten an einem Knoten hat, rechnet
der Leser beim Öffnen einmal aus — bei 93 000 Kanten sind das Millisekunden,
und die Datei bleibt einfach. Eine zweite Tabelle, die dasselbe noch einmal
sagt, kann außerdem widersprüchlich werden.

**Keine Höhen.** Ein Anstieg gehört in die Wegekosten, und die Geländedaten
liegen bereits als `.czh` daneben. Das ist eine eigene Sache und keine des
Formats.

## Gemessen

Aus `austria-latest.osm.pbf` für den Ausschnitt 12,9/47,7 bis 13,2/47,9
(Salzburg und Umgebung):

| | |
|---|---:|
| Knoten | 75 970 |
| Kanten | 93 095 |
| Wegenetz | 4 723 km |
| Datei | 5,5 MB |

Davon 1 153 km Feldwege, 476 km Pfade, 471 km Fußwege, 816 km Wohnstraßen.

## Bauen

    python tools/karte/wege_bauen.py work/karte/austria-latest.osm.pbf \
        --gebiet 12.9 47.7 13.2 47.9 --aus work/karte/salzburg.czw
    python tools/karte/wege_bauen.py <pbf> --gebiet ... --zaehlen

**Für einen Landesauszug gedacht, nicht für einen Kontinent.** Das Werkzeug
hält im ersten Durchgang alle Wege und Knotennummern der Datei im Speicher; bei
Österreich sind das 2,4 Millionen Wege und 23 Millionen Knoten und es geht
gerade noch. `europe-latest.osm.pbf` mit 34 GB würde jeden Rechner sprengen.
Wer ein größeres Gebiet braucht, schneidet die Rohdaten vorher zu.

## Unterschrift

    packsign wege-signieren --key NAME.secret --in WEGE.czw --out WEGE-signiert.czw
    packsign wege-pruefen   --in WEGE.czw  [--keys LISTE.txt]

Derselbe Umschlag wie bei der `.czk`, mit eigener Kennung `CZW1`. Vor dem
Unterschreiben liest das Werkzeug die **Geometrie jeder Kante** — die bleibt
beim Öffnen sonst auf der Platte. Sie muss an ihren Knoten anfangen und
aufhören, und die angegebene Länge muss zur nachgemessenen passen; erlaubt sind
0,5 m am Endpunkt und 1,0 m (oder 0,5 %) bei der Länge, gemessen an der
gebauten Salzburg-Datei. Näheres in `docs/SIGNATUR-ZUSATZDATEIEN.md`.

Eine Datei ohne Umschlag wird gelesen und als unsigniert gekennzeichnet.
