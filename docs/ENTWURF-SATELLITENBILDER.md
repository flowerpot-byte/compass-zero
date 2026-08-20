# Entwurf: Satellitenbilder als eigenes Paket

Stand 17.08.2026. **Entwurf, nichts davon ist gebaut.** Max hat am 17.08.2026
gewünscht, dass man „wie als eigenes Paket auch Satellitenbilder für die Karte
runterladen kann". Bevor daran gebaut wird, stehen hier die zwei Fragen, an
denen es scheitern kann — Lizenz und Größe —, mit gerechneten Zahlen.

## Die Lizenz entscheidet, welche Bilder überhaupt in Frage kommen

**Nicht möglich:** Google, Bing, Esri, Mapbox und die Landesvermessungsämter.
Ihre Luftbilder sind schöner und feiner, aber keines darf weitergegeben oder
offline mitgeliefert werden. Ein Paket daraus wäre nicht verteilbar, und die
App lebt davon, dass man sie weiterreichen kann.

**Möglich sind zwei:**

| Quelle | Auflösung | Lizenz | Eignung |
|---|---|---|---|
| **Sentinel-2** (ESA/Copernicus) | **10 m** | frei, mit Namensnennung | die realistische Wahl |
| Landsat 8/9 (USGS) | 30 m | gemeinfrei | zu grob — ein Haus ist ein Drittel Bildpunkt |

**Berichtigt am 18.08.2026, nachdem der Rechtshinweis im Volltext gelesen
wurde** (gesichert unter `work/quellen/satellit/`): Hier stand, Sentinel-2 sei
„keine neue Lizenzlage, sondern dieselbe" wie Copernicus WorldDEM-30. Das
stimmt nicht — und der Unterschied geht zu unseren Gunsten.

WorldDEM-30 ist ein Airbus-Produkt mit wörtlich vorgeschriebenem Hinweis, der
auf „all rights reserved" endet, und mit der Pflicht, den Haftungsausschluss
an jeden Weiterempfänger durchzureichen. Der Sentinel-Rechtshinweis kennt
beides nicht. Er gewährt „free, full and open access" und ausdrücklich
Vervielfältigung, Verbreitung, öffentliche Wiedergabe, Bearbeitung und
Kombination mit anderen Daten. Verlangt wird ein Satz:

> Contains modified Copernicus Sentinel data [Jahr]

(„modified", weil wir zuschneiden, umrechnen und neu packen).

**Der Bezugsweg ist geklärt und braucht kein Konto.** Die Aufnahmen liegen
offen im STAC-Katalog von Element 84 (`earth-search.aws.element84.com`,
Collection `sentinel-2-c1-l2a`); das Echtfarbenbild je Kachel heißt
`TCI.tif`. Am 18.08.2026 geprüft: Für Salzburg und Umgebung gibt es für den
Sommer 2025 fünf Aufnahmen mit unter drei Prozent Wolken, die beste mit 0,0
Prozent. Der sonst übliche Weg über das Copernicus Data Space Ecosystem
verlangt eine Anmeldung — ein Bauwerkzeug, das ein Konto braucht, kann
niemand nachvollziehen, der das Projekt nachbaut.

**Ebenfalls geprüft und ausgeschieden: EOX Sentinel-2 cloudless.** Fertig
entwölkte Mosaike, technisch die bequemste Wahl — aber nicht-kommerzielle
Nutzung nur unter CC BY-NC-SA 4.0, kommerzielle nur gegen gekaufte Lizenz,
und Sub-Lizenzieren ist ohne gesonderte Vereinbarung und Gebühr untersagt.
Genau das täte jeder, der ein Paket per Bluetooth weiterreicht. Damit fällt
EOX aus demselben Grund aus wie Google, Bing und Esri.

## Wie fein es überhaupt sein kann — und warum darüber Schluss ist

Sentinel-2 liefert **10 Meter je Bildpunkt**. Umgerechnet auf die
Zoomstufen der Karte (bei 48 Grad Nord):

| Zoom | Meter je Bildpunkt | Verhältnis zur Quelle |
|---|---|---|
| 12 | 25,6 m | gröber als die Quelle |
| **13** | **12,8 m** | **passt zur Quelle** |
| 14 | 6,4 m | **erfundene Schärfe** — hochgerechnet, nicht gemessen |

**Zoom 13 ist deshalb die Obergrenze.** Ab 14 würde das Paket zwei- bis
viermal so groß und zeigte Einzelheiten, die in den Daten nicht stehen. Das
ist derselbe Grundsatz, der schon an zwei anderen Stellen gilt: Die Karte
schreibt „Übersicht vergrößert" dazu, wenn sie eine gröbere Stufe hochrechnet,
und die Höhendaten weisen erfundene Spannweiten ab.

## Die Größe — gerechnet, nicht geschätzt

Kachelzahl aus der Fläche in Web-Mercator, 256 × 256 Bildpunkte je Kachel,
**20 kB je JPEG-Kachel** (üblich für Satellitenbild bei brauchbarer Güte):

| Gebiet | Zoom 12 | Zoom 13 | Zoom 14 |
|---|---:|---:|---:|
| Österreich | 75 MB | **301 MB** | 1,2 GB |
| Deutschland | 287 MB | 1,1 GB | 4,5 GB |
| Europa | 8,0 GB | 32,1 GB | 128,4 GB |

**Daraus folgt dieselbe Aufteilung wie bei der Vektorkarte:** Satellitenbilder
gibt es **nur je Region**, nie für Europa. Ein Regionspaket auf Zoom 13 liegt
bei rund 300 MB und damit in derselben Größenordnung wie das vorhandene
Detailpaket Österreich (311 MB) — das ist tragbar.

## Format: `.czb`, gebaut wie `.czk`

Kein neues Verfahren, sondern das vorhandene mit anderem Kachelinhalt:

* **Umschlag identisch zur `.czk`** — Kennung, Fassung, Ed25519-Unterschrift,
  Kachelverzeichnis, binär durchsuchbar. Damit gilt dieselbe Signaturprüfung,
  und `packsign` braucht nur einen weiteren Befehl.
* **Kachelinhalt: ein JPEG**, nicht mit Deflate gepackt (JPEG ist schon
  komprimiert; ein zweiter Durchgang kostet Zeit und bringt nichts).
* **Kopf zusätzlich:** Aufnahmezeitraum (von/bis als Datum). Ein
  Satellitenbild ist eine Momentaufnahme — wer im Gelände danach geht, muss
  wissen, ob es von diesem Frühjahr stammt oder von vor vier Jahren.

## Wie es in der App liegt

Als **unterste Schicht**, die Vektorkarte darüber. Dafür spricht dreierlei:

1. Wege, Namen und eigene Punkte bleiben lesbar — auf einem Satellitenbild
   allein findet sich niemand zurecht.
2. Die vorhandene Ebenen-Reihe bekommt einen Schalter mehr („Bild"), und der
   Schalter „Ruhig" wirkt weiter auf die Linien darüber.
3. Fehlt das Bildpaket, ändert sich nichts — genau wie bei der Höhendatei.

## Was Max entscheiden muss

1. **Sentinel-2 als Quelle?** Wenn ja, ist die Lizenzfrage dieselbe wie bei
   der Geländeform und damit geklärt.
2. **Welche Region zuerst?** Ein Paket, nicht mehrere — als Probe, ob 300 MB
   und die Bedienung taugen.
3. **Zoom 13 als Obergrenze** — oder doch 14, mit vierfacher Größe und
   Schärfe, die in den Daten nicht steckt.

## Was danach zu bauen wäre

In dieser Reihenfolge, jeder Schritt für sich prüfbar:

1. `packsign` um `bild-signieren` und `bild-pruefen` erweitern (kleinster
   Schritt, nutzt die vorhandene Umschlagsprüfung).
2. `tools/karte/bilder_holen.py` — Sentinel-2-Kacheln beziehen, wolkenarme
   Aufnahmen wählen, nach Web-Mercator umrechnen, als `.czb` schreiben.
   **Das ist der große Posten**, und er läuft auf dem PC, nicht auf dem Gerät.
3. `core/karte`: Bildkacheln lesen (Umschlag ist schon da).
4. Android: als unterste Schicht zeichnen, Schalter „Bild", Namensnennung
   ergänzen.
