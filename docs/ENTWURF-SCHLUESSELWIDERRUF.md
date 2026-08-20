# Entwurf: Schlüssel-Widerruf

**Stand 11.08.2026. Das ist ein ENTWURF, kein Bauplan zum Sofort-Umsetzen.**
Ausdrücklich zur Durchsicht gedacht. Gebaut ist nichts. Er führt eine
**neue Dateiart** ein — das wird nicht nebenbei entschieden.

Die Entscheidung ist am 28.07.2026 gefallen und steht in `RULES.md`,
Regel 5. Das Papier setzt sie nur auseinander.

## Das Problem in einem Satz

Ist ein Signaturschlüssel einmal kompromittiert, gibt es ohne Widerruf kein
Mittel, ihn zu entwerten — außer eine neue App-Version zu verteilen. Und die
bekommt ausgerechnet die Zielgruppe nicht: offline, im Krisenfall.

## Die zwei Schlüsselrollen

| Rolle | Wird aufbewahrt | Unterschreibt |
|---|---|---|
| **Wurzelschlüssel** | offline, getrennt | **ausschließlich Erklärungen**, nie Inhalte |
| **Inhaltsschlüssel** | im Arbeitsablauf | Pakete (`.czp`) |

**In der App fest eingebaut ist nur der Wurzelschlüssel.** Inhaltsschlüssel
werden durch eine Erklärung benannt — oder entzogen.

Der Gewinn: Ein kompromittierter Inhaltsschlüssel lässt sich entwerten,
ohne die App auszutauschen. Der Wurzelschlüssel wird nie für Alltagsarbeit
angefasst und ist deshalb ein sehr viel kleineres Ziel.

## Die neue Dateiart `.czs`

**Gleicher Envelope wie `.czp`, anderes Magic.** Nach `docs/PACK-FORMAT.md`
steht das Magic in den ersten vier Bytes (`43 5A 50 31`, „CZP1"); die
Ed25519-Signatur liegt ab Offset 38 und deckt
`Magic ‖ Version ‖ Signierer-Key ‖ Payload-Länge ‖ Payload`.

**Vorschlag: `43 5A 53 31` („CZS1").** Weil das Magic Teil der signierten
Nachricht ist, kann eine Erklärung nicht als Paket durchgehen und umgekehrt
— die Verwechslung ist kryptografisch ausgeschlossen, nicht nur durch eine
Prüfung im Code. Das ist der eigentliche Grund für ein eigenes Magic.

**Warum KEIN Anbau ans Paketformat:** Ein zusätzlicher erlaubter Eintrag in
`.czp` würde bereits verteilte Pakete und die festgenagelte ZIP-Prüfung
berühren. Eine eigene Dateiart lässt alles Bestehende unangetastet.

Erklärungen reisen **dieselben Wege wie Pakete** — vorinstalliert, kopiert,
per Bluetooth, NFC oder QR. Kein Netz, wie überall.

## Die drei Arten von Erklärung

1. **Schlüssel benennen** — ein Inhaltsschlüssel wird als vertrauenswürdig
   eingeführt.
2. **Schlüssel entziehen** — ein Inhaltsschlüssel gilt nicht mehr.
3. **Paket zurückrufen** — eine bestimmte Paket-Kennung samt Version gilt
   als fehlerhaft, auch wenn ihr Schlüssel weiter gültig ist.

Der dritte Fall ist der, den man zuerst vergisst und am ehesten braucht:
nicht der Schlüssel ist kaputt, sondern der Inhalt.

## Warum der Zustand nur in eine Richtung wächst

**Es gibt kein „Entzug aufheben".** Gäbe es das, ließe sich mit einer alten
Erklärung der Zustand zurückdrehen — wer eine entzogene Erklärung
aufbewahrt, könnte einen entzogenen Schlüssel wiederbeleben. Ein Widerruf,
der sich zurücknehmen lässt, ist keiner.

Praktisch heißt das: Die App führt eine Liste, die nur wächst. Ein
versehentlicher Entzug wird nicht rückgängig gemacht, sondern durch einen
**neuen Schlüssel** geheilt. Das ist unbequem und richtig.

Dasselbe Prinzip trägt schon der Rückstufungs-Schutz
(`ENTWURF-RUECKSTUFUNGSSCHUTZ.md`) — beide Zustände steigen nur.

## Was mit entzogenen Inhalten geschieht

**Sie bleiben lesbar, mit dauerhafter, nicht wegklickbarer Warnung.** Das
ist Max' Entscheidung vom 28.07.2026, und die Begründung steht in der
ROADMAP: Wer im Ernstfall kein zweites Nachschlagewerk hat, dem nützt eine
App nichts, die sich selbst leert.

Das ergänzt die vorhandene Urteilstabelle aus `PACK-FORMAT.md`, die heute
`Trusted`, `UnknownSigner` und `BadSignature` kennt. Vorschlag für ein
viertes Urteil: **`Revoked` — Signatur in sich gültig, Schlüssel oder Paket
aber entzogen. Wird geladen, mit ständiger Warnung.** `BadSignature` bleibt
das einzige Urteil, das niemals lädt.

## Tests, ohne die das Ganze nichts wert ist

1. Eine Erklärung mit gültiger Wurzelsignatur wird angenommen.
2. Eine Erklärung mit **Inhalts**schlüssel signiert wird **abgelehnt** —
   nur die Wurzel darf Erklärungen unterschreiben.
3. Eine `.czs`-Datei, deren Magic auf „CZP1" gefälscht ist, scheitert an der
   Signatur (weil das Magic mitsigniert ist) — und umgekehrt.
4. Ein entzogener Schlüssel führt zu `Revoked`, **nicht** zu `BadSignature`,
   und der Inhalt bleibt erreichbar.
5. Die Warnung lässt sich nicht dauerhaft wegklicken.
6. Eine ältere Erklärung kann einen Entzug **nicht** aufheben.
7. Ein Paket-Rückruf trifft genau die genannte Kennung und Version, keine
   andere.
8. Der Zustand überlebt einen Neustart der App und das Löschen des Pakets.

## Offene Fragen für Max

1. **Wie wird eine Erklärung in der Oberfläche sichtbar?** Eine Datei, die
   still den Vertrauenszustand ändert, ist schwer zu durchschauen. Mein
   Vorschlag: eine eigene Ansicht „Was dieses Gerät für gültig hält" mit
   allen benannten und entzogenen Schlüsseln samt Datum.
2. **Soll eine Erklärung auch beim ersten Start aus den Beigaben gelesen
   werden**, oder nur, wenn sie ausdrücklich eingespielt wird?
3. **Bekommt der Wurzelschlüssel selbst einen Nachfolge-Weg?** Wenn er
   verloren geht, ist das System eingefroren. Das ist womöglich hinnehmbar —
   aber es sollte eine bewusste Entscheidung sein, keine Nebenwirkung.
