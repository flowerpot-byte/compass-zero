# Unterschriften für Bild, Namensverzeichnis, Wegenetz und Geländeform

Stand 18.08.2026. **Gebaut und geprüft.** Der Entwurf, aus dem diese Datei
hervorgegangen ist, stand unter `ENTWURF-SIGNATUR-ZUSATZDATEIEN.md`.

Vier Dateiarten liegen neben der Karte: `.czb` (Satellitenbilder), `.czn`
(Namensverzeichnis), `.czw` (Wegenetz) und `.czh` (Geländeform). Alle vier
tragen jetzt dieselbe Unterschrift wie eine Karte, und die App sagt bei jeder
einzelnen, was sie davon hält.

## Warum sie eine eigene Unterschrift brauchen

Die Geländeform (`.czh`) war lange die begründete Ausnahme — nur Höhenzahlen,
und einer gefälschten Schummerung folgt niemand. Am 18.08.2026 hat Max sie
aufgehoben, und zu Recht: Aus denselben Zahlen kommt die Höhenangabe unter dem
Finger, und danach entscheidet jemand, ob er über einen Sattel geht oder
außenherum. Bei den drei anderen war der Fall von Anfang an klar:

| Datei | Was eine Fälschung anrichtet |
|---|---|
| `.czn` | Ein Eintrag „Krankenhaus" an einer Stelle, wo keines ist. Jemand geht dorthin, statt zum echten. |
| `.czw` | Eine Kante über eine Brücke, die es nicht gibt — oder an einer Felswand entlang. Die Route sieht dabei völlig normal aus. |
| `.czb` | Ein Bild, das eine Furt zeigt, wo keine ist, oder einen Wald, wo eine Mure liegt. |
| `.czh` | Eine Höhenangabe, nach der ein Anstieg harmlos aussieht, den man nicht schafft. |

Das Gemeinsame: **Man sieht der Fälschung nichts an.** Bei einer falschen
Textstelle stutzt man vielleicht; bei einer Linie auf einer Karte nicht.

## Wie es gebaut ist

Dieselbe Kette wie bei Karten, an vier Stellen:

1. **Eigene Kennung im unterschriebenen Teil** — `PackFormat.BILD_MAGIC`
   (`CZB1`), `NAME_MAGIC` (`CZN1`), `WEGE_MAGIC` (`CZW1`), `HOEHEN_MAGIC`
   (`CZH1`). Dadurch deckt eine Unterschrift über ein Wegenetz kein
   Namensverzeichnis gleicher Größe. Die vierten Bytes unterscheiden sich
   außerdem von den blanken Kennungen der Dateien selbst (`CZBILD01`,
   `CZNAME01`, `CZWEG001`, `CZHOEHE1`), damit der Leser am Anfang erkennt,
   **ob** ein Umschlag da ist.
2. **Eine Prüfung für alle vier** — `core/karte/Zusatzumschlag.kt`. Viermal
   dieselbe Prüfung zu schreiben hieße, dass sie viermal richtig sein muss und
   eine spätere Berichtigung an drei Stellen vergessen wird.
3. **Versatz statt Kopie** — jeder Leser (`Bilddatei`, `Namensdatei`,
   `Wegenetz`, `Hoehendatei`) nimmt beim Öffnen einen Versatz entgegen und
   rechnet ihn auf jeden Sprung auf. Die Versätze in den Verzeichnissen zählen **ab dem
   Inhalt**, nicht ab dem Dateianfang.
4. **`packsign`-Befehle** — je Format ein Paar, siehe unten.

## Die Regel, auf die es ankommt

> Was das Werkzeug nicht verstanden hat, unterschreibt es nicht.

Eine Unterschrift sagt damit nicht nur „von mir", sondern auch „ich habe es
gelesen". Vor dem Unterschreiben geht `packsign` deshalb die ganze Datei durch
— mit **demselben Leser, den auch die App benutzt** — und weist ab, was nicht
aufgeht:

**`.czb`** — jede Kachel wird gelesen und muss ein sauberes JPEG sein, geprüft
mit derselben Bildprüfung wie die Bilder in einem Inhaltspaket. Ein Eintrag im
Verzeichnis muss außerdem genau die Kachel liefern, auf die er zeigt (der
Leser darf sonst eine gröbere Stufe zurückgeben).

**`.czn`** — die Schlüssel müssen **aufsteigend** stehen, geprüft mit genau dem
Vergleich, den die Suche benutzt (`Namensdatei.vergleicheSchluessel`). Der
Leser allein merkt das nicht: Er prüft nur die Versatztabelle. Steht die
Reihenfolge kopf, findet die binäre Suche nicht etwa nichts, sondern den
falschen Ort. Dazu: jeder Schlüssel muss so gefaltet sein, wie die Suche eine
Anfrage faltet — sonst steht der Name in der Datei und ist trotzdem nicht zu
tippen —, jeder Name muss belegt sein, und jede Stelle im angegebenen Rahmen
liegen.

**`.czh`** — jede Kachel wird entpackt und gelesen. Das Verzeichnis prüft der
Leser schon beim Öffnen; was in den Kacheln steht, sieht er nie — und eine
Kachel, die sich nicht lesen lässt, fällt in der App als fehlende Schummerung
auf, also als flaches Land.

**`.czw`** — die Geometrie jeder Kante wird gelesen. Sie muss an ihren Knoten
anfangen und aufhören, und die angegebene Länge muss zur nachgemessenen
passen. Der Leser prüft beim Öffnen nur die Kantenköpfe; die Geometrie bleibt
sonst bis zur ersten Route auf der Platte.

**Und danach noch einmal:** Nach dem Schreiben wird die fertige Datei
**hinter dem Umschlag** ein zweites Mal durchgesehen. Wer den Umschlag beim
Rechnen vergisst, liest um seine Länge verschoben; ohne diese zweite Durchsicht
fällt das erst auf dem Telefon auf, und dort sieht es aus wie eine kaputte
Datei.

## Die Befehle

```
packsign bild-signieren  --key NAME.secret --in BILDER.czb --out BILDER-signiert.czb
packsign bild-pruefen    --in BILDER.czb [--keys LISTE.txt]
packsign namen-signieren --key NAME.secret --in NAMEN.czn  --out NAMEN-signiert.czn
packsign namen-pruefen   --in NAMEN.czn  [--keys LISTE.txt]
packsign wege-signieren  --key NAME.secret --in WEGE.czw   --out WEGE-signiert.czw
packsign wege-pruefen    --in WEGE.czw   [--keys LISTE.txt]
packsign hoehen-signieren --key NAME.secret --in HOEHEN.czh --out HOEHEN-signiert.czh
packsign hoehen-pruefen   --in HOEHEN.czh  [--keys LISTE.txt]
```

Rückgabewerte wie bei `karte-pruefen`: `0` geprüft und Signierer bekannt,
`1` unsigniert / Signierer unbekannt / unbrauchbar, `2` Bedienfehler oder
Inhalt nicht in Ordnung.

## Unsigniert wird gekennzeichnet, nicht abgewiesen

Von Max am 18.08.2026 entschieden — dieselbe Regel wie bei Karten und aus
demselben Grund: Wer sich sein Wegenetz mit `wege_bauen.py` selbst aus den
Rohdaten baut, soll es benutzen können. Die App sagt dann dazu, dass sie die
Herkunft nicht belegen kann.

Eine **kaputte** Unterschrift ist etwas anderes als eine unbekannte: Dann wird
die Datei gar nicht erst geöffnet. Bei einer unbekannten weiß man nur nicht,
wer sie gemacht hat; bei einer kaputten weiß man, dass sie nicht mehr die ist,
die jemand unterschrieben hat.

## Gegenproben

`tools/packsign/src/test/.../ZusatzdateienTest.kt` baut die Proben selbst, damit
sie nach einem frischen Klon laufen. Geprüft wird nicht, dass eine Unterschrift
entsteht — das täte auch ein Werkzeug, das alles blind durchwinkt —, sondern
dass Dateien abgewiesen werden, denen man den Fehler nicht ansieht:

* ein Namensverzeichnis in verdrehter Reihenfolge (Versatztabelle in Ordnung),
* eine Kante mit gelogener Länge (Linie in Ordnung),
* eine Bildkachel mit Bytes hinter dem Bildende,
* ein Höhenraster mit Fremdbytes dahinter,
* eine Datei im falschen Umschlag,
* eine fremd unterschriebene Datei,
* ein umgekipptes Byte in einer fertig signierten Datei.

Zusätzlich an den echten Dateien durchgespielt (18.08.2026, `salzburg.czb` mit
707 Kacheln, `europa-namen.czn` mit 5902 Namen, `salzburg.czw` mit 75 970
Knoten und 93 095 Kanten, `oesterreich-hoehen.czh` mit 691 Kacheln): alle vier
werden unterschrieben und wiedererkannt, und dieselben Fälschungen von Hand
nachgebaut werden abgewiesen. Die Rückgabewerte dafür wurden **nicht hinter
einer Pipe** gelesen (siehe MERKZETTEL).

Am Gerät nachgesehen, nicht nur am Rechner: Die Kartenseite meldet für alle
vier „Signatur geprüft: entwicklung“, und die Suche findet im signierten
Namensverzeichnis, was sie finden soll — damit stimmt nicht nur die
Unterschrift, sondern auch die Versatzrechnung hinter dem Umschlag.

## Wieviel Schlupf die Wegeprüfung lässt

Gemessen an der gebauten Salzburg-Datei: Die Linien fangen auf **0,0 Meter**
genau an ihren Knoten an, und die größte Abweichung zwischen angegebener und
nachgemessener Länge liegt bei **0,50 Metern** über alle 93 095 Kanten — das
ist das Runden auf ganze Meter beim Bauen. Erlaubt sind deshalb 0,5 m am
Endpunkt und 1,0 m (oder 0,5 %) bei der Länge. Wer großzügiger prüft, prüft
nichts mehr; wer strenger prüft, weist gebaute Dateien ab.

## Offen

**Schlüssel:** Karten, Pakete und jetzt auch diese vier werden mit
`entwicklung` signiert. Vor einer Veröffentlichung gehört das an den
Wurzelschlüssel-Ablauf aus `docs/DESIGN.md`.
