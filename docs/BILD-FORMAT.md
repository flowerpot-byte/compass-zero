# Bildformat `.czb`

Stand 18.08.2026. Fassung 1. Gebaut von `tools/karte/bilder_bauen.py`.

Satellitenbilder als eigenes, nachladbares Paket — der Untergrund unter der
Vektorkarte. Wer wissen will, ob eine Fläche Wald oder Wiese ist, ob ein Hang
felsig oder bewachsen ist, ob ein Bach noch Wasser führt, sieht es hier und
nicht in der Zeichnung.

## Lizenz der Bilddaten — Copernicus Sentinel

Die Bilder stammen aus **Sentinel-2** (ESA/Copernicus). Der Rechtshinweis der
Europäischen Kommission gewährt ausdrücklich Vervielfältigung, Verbreitung,
öffentliche Wiedergabe, Bearbeitung und Kombination mit anderen Daten, „free,
full and open". Verlangt wird ein Satz bei jeder Weitergabe — und da wir
zuschneiden, umrechnen und neu packen, ist es die Fassung für bearbeitete
Daten:

> Contains modified Copernicus Sentinel data [Jahr]

**Das ist eine andere Lizenz als die der Geländeform** und eine deutlich
einfachere. Deshalb liegen Bilder, Gelände und Vektorkarte in drei getrennten
Dateien: So steht jede unter genau einer Lizenz, und keine erbt die Auflagen
der anderen. Einzelheiten und die geprüften Alternativen stehen in
`work/quellen/satellit/LIZENZ.md`.

## Zweite Quelle für den groben Überblick: NASA GIBS

Dazugekommen am 19.08.2026, weil Sentinel-2 für Kontinent-Maßstab nicht taugt —
und der Grund ist Rechnerei, keine Bequemlichkeit:

**Der Aufwand hängt an der Fläche, nicht an der Zoomstufe.** Eine grobe Kachel
deckt viele Sentinel-Aufnahmen ab, und aus jeder muss gelesen werden. Gemessen
am 19.08.2026: Europa auf Zoom 9, ein Neuntel des Gebiets → **rund 300
Minuten**. Über GIBS: **2038 Kacheln für ganz Europa, Zoom 3–8, 13,9 MB, 154
Sekunden** — ein Abruf, ein fertiges JPEG, im selben Kachelraster.

Genommen wird `BlueMarble_NextGeneration`, weil es aus einem ganzen Monat
**wolkenfrei** zusammengesetzt ist. Die tagesaktuellen Schichten (MODIS, VIIRS)
wären feiner, zeigen aber die Wolken des gewählten Tages — und dafür gilt
derselbe Satz wie für die Wolkengrenze beim Bauen: Ein Bild voller Wolken sieht
im Gelände aus wie Schnee und führt in die Irre.

**Die Rechtslage ist eine andere als bei Copernicus, und sie ist noch nicht
abschließend geklärt.** Die NASA bittet in ihrer GIBS-Dokumentation um eine
Namensnennung (im Volltext gelesen und gesichert). Die förmliche Data Use Policy
war am 19.08.2026 nicht abrufbar. Einzelheiten samt dem, was ausdrücklich NICHT
belegt ist, in `work/quellen/satellit/LIZENZ.md`.

**Bis zur Freigabe durch Max gilt: bauen und benutzen ja, veröffentlichen
nein.** Der Überblick trägt dann eine eigene Nennung neben dem
Copernicus-Satz.

## Warum Zoom 14 die Grenze ist

Sentinel-2 liefert **10 Meter je Bildpunkt**. Nachgerechnet bei 48 Grad Nord:

| Zoomstufe | Meter je Bildpunkt | |
|---|---:|---|
| 12 | 25,6 | gröber als die Quelle |
| 13 | 12,8 | gröber als die Quelle — ein Fünftel wird weggeworfen |
| **14** | **6,4** | alles drin, was in den Daten steckt |
| 15 | 3,2 | reine Vergrößerung, kein neuer Bildpunkt |

**Bis zum 18.08.2026 stand hier Zoom 13 als Grenze, und das war zu streng.**
Max hat am Gerät gesehen, dass es unscharf aussieht — zu Recht: Auf Zoom 13
liegt ein Bildpunkt der Karte bei 12,8 Metern, die Aufnahme kann aber 10. Erst
Zoom 14 holt alles heraus. Ab 15 wird nur noch vergrößert: vierfache Datei,
kein einziger neuer Bildpunkt — das Werkzeug lehnt es ab.

Der Preis steht unten in der Tabelle: Zoom 14 kostet das Vierfache von 13.

## Wie groß das wird — gerechnet, nicht geschätzt

Gemessen an `salzburg.czb`: **14,0 kB je Kachel** (JPEG, Qualität 82). Daraus:

| Gebiet | bis Zoom 13 | bis Zoom 14 | bis Zoom 15 |
|---|---:|---:|---:|
| Österreich | 0,3 GB | **1,2 GB** | 4,8 GB |
| Deutschland | 1,1 GB | **4,5 GB** | 18,1 GB |
| beide zusammen | 1,6 GB | **6,2 GB** | 24,6 GB |
| Welt (nur Land) | 0,20 TB | **0,79 TB** | 3,16 TB |

Die Weltzahl rechnet mit 29 Prozent Landanteil zwischen 60 Grad Süd und 75
Grad Nord. Der Platz ist dabei nicht das Problem — die Bauzeit ist es: Jede
Kachel einzeln über das Netz zu holen dauert rund drei Sekunden, und das sind
bei 55 Millionen Kacheln keine Rechnung mehr, sondern ein Ding der
Unmöglichkeit. Für ein ganzes Land lädt man die Aufnahmen erst herunter
(Österreich sind rund 40 Sentinel-Kacheln, gut 10 GB) und baut dann von der
Platte.

Zum Vergleich: Landsat 8/9 wäre gemeinfrei, löst aber 30 Meter auf. Ein Haus
ist dort ein Drittel Bildpunkt.

## Aufbau der Datei

Wie bei der `.czk`: Kopf, Kachelverzeichnis, Kachelinhalte. Zahlen in Little
Endian.

### Kopf, 48 Bytes

| Versatz | Größe | Bedeutung |
|---:|---:|---|
| 0 | 8 | Kennung `CZBILD01` |
| 8 | 1 | Fassung, hier `1` |
| 9 | 1 | Kachelkante als Zweierpotenz, hier `8` (also 256) |
| 10 | 1 | kleinste Zoomstufe |
| 11 | 1 | größte Zoomstufe |
| 12 | 4 | westlichste Länge, Zehnmillionstel Grad (`int32`) |
| 16 | 4 | südlichste Breite |
| 20 | 4 | östlichste Länge |
| 24 | 4 | nördlichste Breite |
| 28 | 4 | Anzahl Kacheln (`uint32`) |
| 32 | 8 | Versatz des ersten Kachelinhalts (`uint64`) |
| 40 | 4 | früheste Aufnahme als `JJJJMMTT` (`uint32`) |
| 44 | 4 | späteste Aufnahme als `JJJJMMTT` (`uint32`) |

**Die beiden Daten am Ende sind der Grund für die acht zusätzlichen Bytes.**
Ein Satellitenbild ist eine Momentaufnahme. Wer im Gelände danach geht, muss
wissen, ob es von diesem Sommer stammt oder von vor vier Jahren: Ein
Kahlschlag, ein Neubaugebiet, ein verlandeter Teich — das Bild zeigt den
Zustand des Aufnahmetags und nicht den von heute. Die App muss das anzeigen
können, ohne die Datei zu durchsuchen.

### Kachelverzeichnis

Direkt hinter dem Kopf, ein Eintrag je Kachel, **aufsteigend sortiert nach
(Zoom, X, Y)** — dieselbe Ordnung wie bei der `.czk`, damit dieselbe binäre
Suche greift.

| Größe | Bedeutung |
|---:|---|
| 1 | Zoomstufe |
| 4 | Kachel-X (`uint32`) |
| 4 | Kachel-Y (`uint32`) |
| 8 | Versatz des Inhalts in der Datei (`uint64`) |
| 4 | Länge des Inhalts (`uint32`) |

Ein Eintrag ist 21 Bytes groß. X und Y zählen im üblichen Kachelraster
(Web-Mercator, Y von Norden nach Süden) — dasselbe wie bei der Vektorkarte,
sonst lägen die Bilder verschoben unter der Zeichnung.

### Kachelinhalt

**Ein JPEG, sonst nichts.** Nicht zusätzlich mit Deflate gepackt: JPEG ist
bereits komprimiert, ein zweiter Durchgang kostet beim Öffnen Zeit und bringt
nichts. Qualität 82 — darüber wächst die Datei schneller als das Bild besser
wird, darunter franst die Waldkante aus.

Kacheln ohne Bildinhalt werden **weggelassen** und stehen nicht im
Verzeichnis. Eine schwarze Kachel wäre eine Aussage („hier ist es dunkel"),
eine fehlende ist keine.

## Obergrenzen beim Lesen

Eine Datei kommt von außen ins Gerät. Es gelten dieselben Grundsätze wie bei
der `.czk`: Vor jeder Speicheranforderung wird die Längenangabe geprüft, nie
danach.

| Grenze | Wert | Grund |
|---|---:|---|
| Kachel | 2 MiB | ein 256er-JPEG liegt bei 20 bis 60 kB; alles darüber ist erfunden |
| Kacheln je Datei | 2 000 000 | deckt Deutschland bei Zoom 14 mit Reserve |
| Zoomstufen | 0 bis 14 | die Obergrenze steht oben |

## Wie es gebaut wird

    python tools/karte/bilder_bauen.py --gebiet 12.6 47.4 13.6 48.0 \
        --zoom 8 14 --aus work/karte/salzburg.czb

Drei Entscheidungen im Werkzeug, jede aus einer Messung und nicht aus einer
Überlegung — Einzelheiten stehen im Kopf der Datei:

1. **Das Umrechnungs-VRT wird auf die Kachel gelegt**, nicht ein großes VRT
   aufgespannt und daraus geschnitten. Ein `WarpedVRT` erlaubt keine Lesung
   über seinen Rand hinaus, und genau die bräuchte jede Kachel am Rand einer
   Aufnahme.
2. **Nur die feinste Stufe kommt aus der Quelle**, alle gröberen entstehen
   durch Halbieren. Jede Stufe einzeln zu lesen kostete vier Kacheln auf Zoom
   11 volle 417 Sekunden.
3. **Je Kartenblatt nur die wolkenärmste Aufnahme.** Mit allen 25 Aufnahmen
   eines Sommers brauchten 16 Kacheln 235 Sekunden, mit vieren 44.

Der Preis der dritten Entscheidung ist sichtbar und gehört gesagt:
Nachbarblätter stammen aus verschiedenen Wochen, und an ihrer Naht springt die
Farbe des Waldes. Für eine Karte, auf der man sich zurechtfindet, ist das
hinnehmbar.

## Unterschrift

    packsign bild-signieren --key NAME.secret --in BILDER.czb --out BILDER-signiert.czb
    packsign bild-pruefen   --in BILDER.czb [--keys LISTE.txt]

Derselbe Umschlag wie bei der `.czk`, mit eigener Kennung `CZB1`. Vor dem
Unterschreiben liest das Werkzeug **jede Kachel** und lässt sie durch dieselbe
Bildprüfung laufen wie die Bilder in einem Inhaltspaket; Bytes hinter dem
Bildende reisen nicht mit. Näheres in `docs/SIGNATUR-ZUSATZDATEIEN.md`.

Eine Datei ohne Umschlag wird gelesen und als unsigniert gekennzeichnet.

Leser (`core/karte/Bilddatei.kt`) und Anzeige als Untergrund unter der
Vektorkarte stehen seit dem 18.08.2026.
