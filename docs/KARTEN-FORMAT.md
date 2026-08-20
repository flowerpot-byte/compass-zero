# Kartenformat `.czk`

Stand 04.08.2026. Fassung 1.

## Lizenz der Kartendaten — ODbL, nicht CC BY-SA

**Entschieden am 04.08.2026.** Die Karte entsteht aus
OpenStreetMap-Daten. OSM steht unter der **Open Database License 1.0**. Eine
`.czk` ist damit eine abgeleitete Datenbank im Sinne der ODbL und muss unter
ODbL weitergegeben werden, mit dem Hinweis:

> Kartendaten © OpenStreetMap-Mitwirkende, ODbL 1.0

Das ist eine **Berichtigung**, keine Wahl: Die Roadmap hatte am 28.07.2026
„CC BY-SA für die Inhaltspakete" festgelegt. Für Tipps und Anleitungen stimmt
das; für die Karte stimmt es nicht, und es ist bis zum 04.08.2026 niemandem
aufgefallen. Die Kartendatei trägt deshalb ihre eigene Lizenzangabe, getrennt
von der der Inhaltspakete.

Die Geländeform steht noch einmal unter einer anderen Lizenz und liegt
deshalb in einer eigenen Datei — siehe [`HOEHEN-FORMAT.md`](HOEHEN-FORMAT.md).

Dieses Format traegt die Offline-Karte. Es ist eigens fuer diese App gemacht,
und der Grund dafuer ist nicht Eigensinn: Jede fertige Kartenbibliothek fuer
Android bringt entweder eine Netz-Berechtigung mit (MapLibre verlangt
`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` und beide
Ortungsrechte -- der Berechtigungswaechter in `androidApp/build.gradle.kts`
bricht den Bau daran ab) oder sie setzt eine hoehere Android-Fassung voraus,
als hier zugesagt ist. Beides ist mit den Grundregeln nicht vereinbar.

## Was drinsteht -- und was bewusst nicht

Sechs Datensorten, festgelegt in `ROADMAP.md` und umgesetzt in
`tools/karte/sorten.py`:

| Sorte | Herkunft | ab Zoom |
|---|---|---|
| Wasserflaechen | `natural=water`, `landuse=reservoir/basin` | 4 |
| Fluesse, Kanaele, Kuestenlinie | `waterway=river/canal`, `natural=coastline` | 8 |
| Baeche | `waterway=stream` | 11 |
| Gletscher | `natural=glacier` | 6 |
| Wald | `natural=wood`, `landuse=forest` | 7 |
| Sumpf | `natural=wetland` | 9 |
| Offenland | Wiese, Acker, Heide, Geroell, Fels | 11 |
| Siedlungsflaechen | `landuse=residential/industrial/...` | 7 |
| Hauptstrassen | `motorway`, `trunk`, `primary` | 8 |
| Nebenstrassen | `secondary` bis `residential` | 10 |
| Pfade und Wege | `track`, `path`, `bridleway`, `steps` | 11 |
| Punkte | Quelle, Brunnen, Gipfel, Pass, Huette, Hoehle | 10 |
| Ortsnamen | `place=city/town/village/hamlet` | 4 |

**Nicht enthalten:** Einzelgebaeude, Hausnummern, Gehsteige, Zufahrten,
Parkplatzwege, Geschaefte, Oeffnungszeiten, Verkehrszeichen. Das ist der weit
groesste Teil der Rohdaten und auf einer Karte fuer den Notfall wertlos.

**Noch nicht enthalten:** die Gelaendeform. Sie ist die sechste Datensorte der
Roadmap, kommt aber aus einem Hoehenmodell und nicht aus OpenStreetMap. Ihr
Platzbedarf und ihre Lizenz sind getrennt zu klaeren.

## Warum die Karte nicht im APK liegt

Ein Ueberblickspaket fuer ganz Europa ist rund 300 MB gross (gemessen, siehe
unten). Eine App dieser Groesse laesst sich nicht mehr von Hand weitergeben,
und wer nur Mitteleuropa braucht, zahlte fuer Skandinavien mit. Die Karte
reist deshalb als eigene Datei neben der App -- denselben Weg wie die
Inhaltspakete, mit derselben Signaturpruefung.

## Aufbau der Datei

Alle Zahlen liegen in Little Endian. `varint` ist ein Sieben-Bit-Varint mit
Fortsetzungsbit wie in Protocol Buffers; `zigzag` ist ein Varint ueber
`(n << 1) xor (n >> 63)`, damit kleine negative Zahlen genauso billig sind wie
kleine positive.

### Kopf, 40 Bytes

| Versatz | Groesse | Bedeutung |
|---:|---:|---|
| 0 | 8 | Kennung `CZKARTE1` |
| 8 | 1 | Fassung, hier `1` |
| 9 | 1 | Kachelraster als Zweierpotenz, hier `12` (also 4096) |
| 10 | 1 | kleinste Zoomstufe |
| 11 | 1 | groesste Zoomstufe |
| 12 | 4 | westlichste Laenge, in Zehnmillionstel Grad (`int32`) |
| 16 | 4 | suedlichste Breite |
| 20 | 4 | oestlichste Laenge |
| 24 | 4 | noerdlichste Breite |
| 28 | 4 | Anzahl Kacheln (`uint32`) |
| 32 | 8 | Versatz des ersten Kachelinhalts (`uint64`) |

### Kachelverzeichnis

Direkt hinter dem Kopf, ein Eintrag je Kachel, **aufsteigend sortiert nach
(Zoom, X, Y)**. Damit findet der Leser eine Kachel mit einer binaeren Suche
und muss nichts in den Speicher laden.

| Groesse | Bedeutung |
|---:|---|
| 1 | Zoomstufe |
| 4 | Kachel-X (`uint32`) |
| 4 | Kachel-Y (`uint32`) |
| 8 | Versatz des Inhalts in der Datei (`uint64`) |
| 4 | Laenge des gepackten Inhalts (`uint32`) |

Ein Eintrag ist 21 Bytes gross.

### Kachelinhalt

Jede Kachel ist einzeln mit Deflate gepackt. Entpackt sieht sie so aus:

```
uint8   Fassung des Kachelaufbaus, hier 1
varint  Anzahl Namen
  je Name:  varint Anzahl Bytes, dann UTF-8
varint  Anzahl Schichten
  je Schicht:
    uint8   Sorte
    varint  Anzahl Objekte
      je Objekt:
        uint8   Art       0 Linie, 1 Aussenring, 2 Punkt, 3 Innenring
        varint  Punktart  nur bei Punkten belegt, sonst 0
        varint  Namensnummer + 1   (0 heisst: kein Name)
        varint  Anzahl Stuetzpunkte
        zigzag  dx, dy je Stuetzpunkt
```

Der erste Stuetzpunkt eines Objekts wird gegen die **linke obere Ecke seiner
Kachel** geschrieben, jeder weitere gegen seinen Vorgaenger. Ein Objekt haengt
damit an keinem anderen Objekt und an keiner anderen Kachel -- eine kaputte
Kachel verdirbt ihre Nachbarn nicht.

Die Koordinaten liegen im Kachelraster: 4096 Einheiten je Kachelkante.
Werte ausserhalb von 0..4095 sind erlaubt und normal -- ein Objekt, das ueber
den Rand ragt, wird mit einem Rand von 64 Einheiten geschnitten, damit
Linienbreiten und Beschriftungen an der Naht nicht abreissen.

## Obergrenzen beim Lesen einer Kachel

Eine Kachel kommt aus einer Datei, die von außen ins Gerät gelangt ist. Ohne
Obergrenzen könnte eine erfundene Längenangabe den Speicher füllen, bevor
überhaupt eine Prüfung greift. Die Werte stehen in `Kartenformat.kt` und sind
**bewusst großzügig**: Sie sollen eine erfundene Zahl abfangen, nicht eine
ungewöhnliche echte Karte ablehnen.

| Grenze | Wert | Grund |
|---|---|---|
| Kachel entpackt | 8 MiB | die größte echte Kachel liegt deutlich darunter |
| Objekte je Kachel | 500 000 | größter Wert, den ein Längenfeld tragen darf |
| Punkte je Kachel | 2 000 000 | dito, für die Koordinatenliste |
| Namen je Kachel | 65 536 | am 04.08.2026 stand die Grenze bei 20 000 und hat eine **gültige** Kachel abgewiesen — seitdem großzügig |
| Bytes je Name | 240 | ein Name ist eine Beschriftung, kein Textfeld |

Wer ein eigenes Werkzeug schreibt, das `.czk` erzeugt, hält sich an diese
Zahlen — sonst liest die App die Kachel nicht, obwohl sie formal richtig
aufgebaut ist.

## Zoomstaffelung und Vereinfachung

Die Karte gibt es in zwei Sorten Paketen. Die Aufteilung ist gemessen, nicht
geschaetzt:

| Paket | Zoom | Toleranz | Europa |
|---|---|---|---:|
| Ueberblick, immer dabei | 4 bis 10 | 1 Bildpunkt | rund 300 MB |
| Detail, je Region | 11 bis 14 | 0,25 Bildpunkte | Oesterreich 311 MB |

Die Toleranz ist der groesste Abstand, den eine vereinfachte Linie von der
urspruenglichen haben darf, gemessen in Bildpunkten der Kachel (16
Rastereinheiten sind ein Bildpunkt). Vereinfacht wird mit Douglas-Peucker.

**Warum zwei verschiedene Toleranzen:** Der Ueberblick dient der Orientierung
im Grossen; dort ist ein Bildpunkt Abweichung nicht zu sehen. Die Detailkarte
dient der Wegfindung; dort will man wissen, wo der Pfad wirklich langgeht.

## Gemessen am 04.08.2026

Gemessen an Oesterreich, weil klein und zugleich der haerteste Fall: dichtes
Wegenetz, starkes Relief. Rohdaten von Geofabrik, Stand 03.08.2026,
805 997 426 Bytes.

- Nach dem Filtern der sechs Sorten: **4 125 164 Objekte, 75 827 343
  Stuetzpunkte** (Flaechen aus Relationen zusammengebaut, nichts verworfen).
- Ohne Vereinfachung, nur auf das Kachelraster gerundet: 556,4 MB fuer alle
  Stufen. Das ist die **obere Schranke**, kein Ergebnis.
- Mit Douglas-Peucker bei 1 Bildpunkt: Ueberblick z4-z10 **6,9 MB**,
  Detail z11-z14 **201,6 MB**.
- Mit Douglas-Peucker bei 0,25 Bildpunkten: Ueberblick 15,4 MB, Detail
  310,6 MB.

Hochgerechnet auf Europa ueber das Verhaeltnis der Rohdaten (Europa
34 742 724 589 Bytes, also 43,1-mal Oesterreich):

| | Ueberblick z4-z10 | Detail z11-z14 |
|---|---:|---:|
| 0,25 Bildpunkte | 664 MB | 13,4 GB |
| 1,00 Bildpunkt | **297 MB** | 8,7 GB |

**Daraus folgt die Aufteilung:** Ein Ueberblickspaket fuer Europa ist
moeglich, ein Detailpaket fuer Europa nicht. Detail gibt es nur je Region.

Eine Zahl zum Einordnen, weil sie leicht falsch gelesen wird: Beim
Herauszoomen um eine Stufe bleiben rund 0,68 der Bytes uebrig, nicht ein
Viertel. Ein Viertel gaelte nur fuer glatte Formen. Kuesten, Waldraender und
Flusslaeufe sind zerklueftet -- sie behalten beim Verkleinern
verhaeltnismaessig mehr Stuetzpunkte. Die ganze Kacheltreppe kostet deshalb
rund das Zweieinhalbfache ihrer feinsten Stufe.

## Unterschrift

Eine Karte, die nicht im APK liegt, reist einzeln und braucht eine eigene
Unterschrift. Der Umschlag ist derselbe wie beim Inhaltspaket (110 Bytes Kopf,
dann die Nutzlast), aber mit eigener Kennung `CZK1`. **Die Kennung steht im
unterschriebenen Teil**, damit eine Unterschrift über ein Inhaltspaket keine
Kartendatei gleicher Größe deckt.

**Unterschrieben wird die Prüfsumme, nicht die Datei.** Reines Ed25519 braucht
zwei Durchgänge über die Nachricht und muss sie deshalb vollständig im Speicher
halten. Am 04.08.2026 ist genau daran das Öffnen einer 346-MB-Karte auf dem
Gerät gescheitert — der Puffer wuchs auf 128 MB und der Speicher war zu Ende,
bevor die Karte offen war. Unterschrieben wird deshalb
`Vorspann || SHA-256(Nutzlast)`; die Prüfsumme entsteht im Durchlauf, der
Speicherbedarf ist konstant, die Aussage bleibt dieselbe.

Eine Karte mit **kaputter** Unterschrift wird nicht geöffnet. Eine Karte mit
**unbekanntem** Signierer schon — dort weiß man nur nicht, wer sie gemacht hat,
und die Oberfläche sagt das dauerhaft.

    packsign karte-signieren --key entwicklung.secret --in karte.czk --out karte-signiert.czk
    packsign karte-pruefen   --in karte-signiert.czk --keys trust.txt

## Grenzen brauchen einen eigenen Weg

**Wer die Karte neu baut, muss diese zwei Schritte mitnehmen** — ohne sie
fehlen die Ländergrenzen wieder, und man sieht es erst auf dem Gerät.

    python tools/karte/grenzen_holen.py relationen europe-latest.osm.pbf grenzwege.txt
    python tools/karte/grenzen_holen.py wege europe-latest.osm.pbf grenzwege.txt \
           grenzen.geom --index knoten.idx
    python tools/karte/grenzen_ketten.py grenzen.geom grenzen-ketten.geom
    python tools/karte/bauen.py europe.geom karte.czk --zoom 4 10 \
           --ohne grenze,grenze-region --zusatz grenzen-ketten.geom

Warum das nicht der normale Weg über `auslesen.py` erledigt:

1. **Die Beschriftung steht an der falschen Stelle.** `auslesen.py` nimmt eine
   Grenze nur mit, wenn der WEG selbst `admin_level` trägt. In Deutschland
   trägt ein Wegstück an der Staatsgrenze meist nur `admin_level=8` — die
   Gemeindegrenze, die dort entlangläuft. Dass es zugleich Staatsgrenze ist,
   steht allein in der Relation. Über die Relationen kommen 19 312 statt
   11 786 Wegstücke zusammen.

2. **Eine Grenze ist eine Kette, kein Strich.** Sie besteht aus vielen kurzen
   Stücken; an jeder anstoßenden Gemeindegrenze fängt ein neues an. Einzeln
   weitergereicht fallen sie durch `MINDESTMASS` (16 Rastereinheiten, auf
   Zoom 4 rund zehn Kilometer). Gemessen für die Kachel z4/8/5 über
   Mitteleuropa: 7572 Stücke, davon 7267 verschluckt — in der gebauten Karte
   standen dort tatsächlich 303. Deutschlands Landgrenzen waren Konfetti.

   `grenzen_ketten.py` fügt sie an ihren gemeinsamen Endpunkten zusammen:
   14 990 Stücke werden zu 197 Ketten. Damit greift der Filter nicht mehr, das
   Vereinfachen kann an einer 900-km-Kette wirklich sparen, und das
   Strich-Punkt-Muster läuft durch, statt bei jedem Stück neu anzufangen.
   `bauen.py` nimmt Grenzen zusätzlich vom Filter aus, weil auch als Kette
   noch Inseln und Exklaven wegfielen.

Der zweite Durchgang benutzt den Knotenindex des ersten Auslesens wieder.
**Nicht über `with_locations()`** — pyosmium hängt den Knotenspeicher vor jeden
Filter, und die Indexdatei wächst dabei still weiter (an Österreich gemessen:
1,41 → 2,80 GB). Ein gewachsener Index ist unbrauchbar, weil die Suche darin
eine binäre Suche über durchgehend sortierte Einträge ist. Das Werkzeug öffnet
die Tabelle deshalb direkt, fragt sie nur mit `get()` und prüft am Ende die
Dateigröße nach.

## Mehrere Dateien ergeben eine Karte

Überblick (z4–z10) und Detail (z11–z14) sind getrennte Dateien. Die App öffnet
**alle** gefundenen und legt sie übereinander: je Datei die feinste Stufe, die
sie zu einem Feld hat, dann von grob nach fein gezeichnet. Die Beigabe im APK
ist immer dabei, damit ein danebengelegtes Detailpaket nicht die Übersicht
verdrängt.

**Warum übereinander und nicht ausgewählt:** Ein Detailpaket ist an seinem
Gebietsrand abgeschnitten. Am 06.08.2026 an der Salzach gemessen — die App nahm
für jedes Feld das Österreich-Paket, weil es dort als erstes eine Kachel hatte;
dessen grobe Kachel enthielt aber nur die österreichische Seite, und die
deutsche Hälfte blieb weiß, obwohl die Europakarte dort Daten hat. Wo das feine
Paket zeichnet, deckt es die Übersicht mit seinen Flächen zu; wo es nichts hat,
bleibt die Übersicht stehen.

Fehlt einer Datei die verlangte Stufe ganz, wird ihr Ausschnitt aus einer
gröberen Stufe **vergrößert nachgezeichnet** — dieselben Linienzüge in groß,
keine Bildvergrößerung, höchstens vier Stufen weit. Die Standzeile schreibt
dann „Übersicht vergrößert": Grobe Linien dürfen nicht mit vermessenen
Einzelheiten verwechselt werden.

## Werkzeuge

| Schritt | Werkzeug |
|---|---|
| Sorten festlegen | `tools/karte/sorten.py` |
| Rohdaten zaehlen | `tools/karte/zaehlen.py` |
| Geometrien herausschreiben | `tools/karte/auslesen.py` |
| Format messen | `tools/karte/packmass.py` |
| Vereinfachung messen | `tools/karte/vereinfachen.py` |
| Kartendatei bauen | `tools/karte/bauen.py` |

Gelesen wird die Datei von `core/karte`.
