# Namensverzeichnis `.czn`

Stand 18.08.2026. Fassung 1. Gebaut von `tools/karte/namen_bauen.py`,
gelesen von `core/karte/Namensdatei.kt`.

Orte, Städte, Dörfer, Weiler, Gipfel, Pässe, Quellen, Brunnen, Hütten,
Höhlen, Krankenhäuser, Apotheken — alles, was in der Karte einen Namen trägt,
einmal nachschlagbar.

## Warum es diese Datei gibt

Die Namen stehen längst in der `.czk`. Nur liegen sie **je Kachel verstreut**:
Wer „Berchtesgaden" sucht, müsste die ganze Karte durchpflügen, und das sind
bei einer Detailkarte 770 MB. Hier liegen sie einmal, nach Suchschlüssel
sortiert, und eine Suche kostet ein gutes Dutzend Sprünge in der Datei.

**Gebaut wird aus der vorhandenen Karte, nicht aus OpenStreetMap.** Das ist
kein Umweg, sondern der Punkt: Wer eine Karte hat, kann sich das Verzeichnis
selbst bauen — ohne Netz, ohne Konto, ohne den Rohdatensatz von 60 GB. Und es
kann nichts enthalten, was nicht auch auf der Karte steht.

## Der Suchschlüssel steht mit in der Datei

Neben dem Namen („St. Veit") liegt sein **gefalteter Schlüssel** („st veit"):
klein geschrieben, Umlaute aufgelöst (ä→ae), Satzzeichen zu Leerraum.

Das kostet Platz und ist trotzdem richtig so. Würde der Leser den Schlüssel
selbst bilden, müssten zwei Umsetzungen — eine in Python, eine in Kotlin — für
alle Zeiten **exakt** dasselbe tun. Laufen sie auseinander, findet die Suche
einzelne Namen nicht mehr, und **niemand merkt, welche**. Ein Verzeichnis, das
still Lücken hat, ist schlimmer als keines.

Die Anfrage wird beim Suchen gefaltet, die Schlüssel in der Datei nie.
`EchteNamensdateiTest` prüft genau das an einer gebauten Datei: Jeder Name muss
sich mit sich selbst wiederfinden lassen.

**Und genau dieses Auseinanderlaufen ist am 18.08.2026 passiert.** Stehen
geblieben ist es an einem einzigen Namen unter 51 805: einer Apotheke namens
„Pharma-Ko³“. Das Werkzeug ließ die hochgestellte Drei im Schlüssel stehen
(Pythons `isalnum()` nimmt sie an), der Leser macht ein Leerzeichen daraus
(Javas `isLetterOrDigit` nicht). Der Schlüssel stand damit in der Datei und war
durch keine Anfrage zu bilden.

Die Regel lautet seither an beiden Stellen gleich: **Buchstabe oder
Dezimalziffer bleibt, alles andere wird zu Leerraum.** Gefunden hat es nicht
ein Test, sondern `packsign namen-signieren` — es weigert sich, ein
Verzeichnis zu unterschreiben, dessen Schlüssel die Faltung nicht selbst
erzeugen würde. Das ist die verlässlichere Schranke: Sie prüft die wirklich
gebaute Datei, Eintrag für Eintrag.

## Aufbau der Datei

Zahlen in Little Endian, `varint` wie im Kartenformat.

### Kopf, 32 Bytes

| Versatz | Größe | Bedeutung |
|---:|---:|---|
| 0 | 8 | Kennung `CZNAME01` |
| 8 | 1 | Fassung, hier `1` |
| 9 | 3 | frei, `0` |
| 12 | 4 | Anzahl Einträge (`uint32`) |
| 16 | 4 | westlichste Länge, Zehnmillionstel Grad (`int32`) |
| 20 | 4 | südlichste Breite |
| 24 | 4 | östlichste Länge |
| 28 | 4 | nördlichste Breite |

Das Gebiet stammt aus dem Kopf der Karte, aus der gebaut wurde — damit die App
sagen kann, welches Verzeichnis für welche Gegend gilt.

### Versatztabelle

Direkt hinter dem Kopf, ein `uint32` je Eintrag: wo der Eintrag in der Datei
beginnt. **Aufsteigend sortiert**, und das wird beim Öffnen geprüft und nicht
angenommen: Die Suche ist binär und fände auf einer verdrehten Tabelle nicht
etwa nichts, sondern **den falschen Ort** — und wer danach losgeht, geht in die
falsche Richtung.

### Einträge

Sortiert nach dem Schlüssel, **byteweise verglichen**. Nicht nach den
Sortierregeln einer Sprache: Python und Kotlin ordnen dann verschieden, und die
binäre Suche findet die Hälfte nicht mehr.

```
varint  Länge des Schlüssels
bytes   Schlüssel, UTF-8, gefaltet
varint  Länge des Namens
bytes   Name, UTF-8, so wie er auf der Karte steht
int32   Länge  (Zehnmillionstel Grad)
int32   Breite
uint8   Sorte aus dem Kartenformat (12 = Punkt, 13 = Ort)
uint8   Punktart aus dem Kartenformat; bei einem Ort 0
```

## Obergrenzen beim Lesen

| Grenze | Wert | Grund |
|---|---:|---|
| Einträge | 5 000 000 | deckt einen ganzen Kontinent mit Reserve |
| Schlüssel | 240 Bytes | ein Name ist eine Beschriftung, kein Textfeld |
| Name | 240 Bytes | dito |

## Wie gesucht wird

**Nur der Anfang**, nicht irgendwo im Wort. Wer „burg" tippt, meint Burghausen
und nicht Salzburg; eine Suche, die beides liefert, schüttet die Liste zu.

Die Faltung sorgt dafür, dass „St. Veit", „st veit" und „ST VEIT" dieselbe
Anfrage sind.

## Was drinsteht — gemessen

Aus der beigelegten Übersichtskarte (`karte.czk`, Zoom 4 bis 10):

| Art | Anzahl |
|---|---:|
| Orte | 5 700 |
| Pässe | 179 |
| Krankenhäuser | 23 |

Zusammen 5 902 Einträge in 0,2 MB. Dass Quellen, Hütten und Apotheken fehlen,
liegt nicht am Verzeichnis: Diese Punkte gibt es in der Karte erst ab Zoom 10,
und die Übersichtskarte endet dort. Aus einer Detailkarte kommen sie mit.

## Bauen

    python tools/karte/namen_bauen.py work/karte/deutschland-nord.czk \
        --aus work/karte/deutschland-nord.czn
    python tools/karte/namen_bauen.py <karte> --zaehlen

Unterschriebene Karten werden erkannt und ihr Umschlag übersprungen. **Geprüft
wird die Unterschrift dabei nicht** — das Werkzeug baut aus einer Datei, die
schon auf dem eigenen Rechner liegt; die Vertrauensfrage stellt das Gerät, wenn
es die Karte öffnet. Ein Werkzeug, das hier „geprüft" meldete, würde eine
Sicherheit behaupten, die es nicht herstellt.

Gebaut wird von der **feinsten Zoomstufe abwärts**: Dort steht ein Ort an
seiner genauesten Stelle. Was auf einer gröberen Stufe noch einmal auftaucht,
ist derselbe Ort mit ungenauerer Lage und wird verworfen.

## Unterschrift

    packsign namen-signieren --key NAME.secret --in NAMEN.czn --out NAMEN-signiert.czn
    packsign namen-pruefen   --in NAMEN.czn  [--keys LISTE.txt]

Derselbe Umschlag wie bei der `.czk`, mit eigener Kennung `CZN1`. Vor dem
Unterschreiben geht das Werkzeug alle Einträge durch und prüft, was der Leser
selbst nicht prüfen kann: dass die **Schlüssel** aufsteigend stehen — mit genau
dem Vergleich, den die Suche benutzt. Der Leser sieht nur die Versatztabelle;
steht die Reihenfolge kopf, findet die binäre Suche nicht etwa nichts, sondern
den falschen Ort. Dazu: jeder Schlüssel muss so gefaltet sein, wie die Suche
eine Anfrage faltet, jeder Name belegt sein und jede Stelle im angegebenen
Rahmen liegen. Näheres in `docs/SIGNATUR-ZUSATZDATEIEN.md`.

Eine Datei ohne Umschlag wird gelesen und als unsigniert gekennzeichnet.
