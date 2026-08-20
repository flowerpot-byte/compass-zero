# Übertragungsformat `CZT1`

Beschreibt, wie zwei Geräte ein fertiges `.czp`-Paket austauschen. Dieses
Dokument ist die verbindliche Referenz für den Rahmenaufbau und den Ablauf;
das Paketformat selbst steht in [`PACK-FORMAT.md`](PACK-FORMAT.md).

Umgesetzt in `core/transfer` (plattformneutral, ohne Netzwerk- oder Funkcode).
Welcher Funkweg darunter liegt, ist eine getrennte Entscheidung und für das
Format ohne Belang — verlangt wird nur ein zuverlässiger Byte-Strom in beide
Richtungen.

## Was der Transport leistet — und was nicht

**Er transportiert. Mehr nicht.** Empfangene Bytes sind unvertrauenswürdig, bis
`core/security` sie in seinem einen Lesedurchlauf geprüft hat — genau wie eine
von Hand kopierte Datei. Es gibt keinen Sonderweg für „per Funk empfangen".

Daraus folgen drei Dinge, die bewusst fehlen:

- **Keine Verschlüsselung.** Pakete sind öffentliche Inhalte. Wer mithört,
  erfährt nichts Schützenswertes; die Integrität sichert die Signatur im Paket.
- **Keine Absender-Prüfung.** Vertrauen entsteht ausschließlich durch die
  Signatur *im* Paket. Ein „von Bekannten per Funk" empfangenes Paket ist
  genauso unverifiziert wie ein gefundener USB-Stick.
- **Kein Wiederaufsetzen** abgebrochener Übertragungen. Bei üblichen Paketgrößen
  ist neu senden billiger als Wiederaufsetz-Logik im sicherheitsnahen Pfad.

Die Prüfsumme im Angebot ist ein **Detektor für Übertragungsfehler**, kein
Sicherheitsmerkmal: Der Sender wählt sie selbst. Sie trennt „unterwegs
verstümmelt" von „gefälscht" — mehr nicht. Belegt durch den Test
`sauberUebertragenesFaelschungBleibtEineFaelschung`: eine Fälschung samt
passender Prüfsumme läuft sauber durch den Transport und wird von der
Signaturprüfung als `BadSignature` erkannt.

## Begrüßung

Einmalig, in beide Richtungen:

| Offset | Größe | Feld |
|---|---|---|
| 0 | 4 | Magic `43 5A 54 31` ("CZT1") |
| 4 | 2 | Protokollversion, aktuell `1` |

Eine fremde Version wird benannt und die Verbindung beendet — nie geraten.

## Rahmen

Alle Mehrbyte-Zahlen sind Big-Endian und vorzeichenlos, wie im Paketformat.

| Offset | Größe | Feld |
|---|---|---|
| 0 | 1 | Typ |
| 1 | 4 | Nutzlastlänge |
| 5 | … | Nutzlast |

**Die Längenprüfung greift vor jeder Speicherbelegung.** Mehr als 65 536 Byte
Nutzlast werden abgelehnt, ohne dass etwas belegt wird; ein Längenfeld mit
gesetztem obersten Bit ergäbe eine negative Zahl und wird ebenso abgelehnt. Der
Empfangspuffer hat feste Größe — es wird nie nach fremder Ansage alloziert.

Der Rahmenleser liegt deshalb im gemeinsamen Teil (`RahmenLeser`) und nicht in
der Funkschicht: Er ist die Stelle, an der diese Grenze wirkt. Läge er je
Plattform vor, hätte jede ihre eigene Fassung der Grenze — und eine davon würde
sie vergessen. Ein Ende des Stroms zwischen zwei Rahmen ist ein sauberer
Abschluss; ein Ende mitten im Rahmen ist ein Fehler.

| Typ | Richtung | Nutzlast |
|---|---|---|
| `01` Angebot | Sender → Empfänger | 8 Byte Gesamtgröße, 32 Byte SHA-256, 2 Byte Namenslänge, Name (UTF-8, höchstens 200 Byte) |
| `02` Annahme | Empfänger → Sender | leer |
| `03` Ablehnung | Empfänger → Sender | 1 Byte Grund |
| `04` Daten | Sender → Empfänger | 1 bis 65 536 Byte Rohdaten |
| `05` Fertig | Sender → Empfänger | leer |
| `06` Abbruch | beide | 1 Byte Grund |

Gründe sind feste Aufzählungen mit unveränderlichen Zahlenwerten, damit die
Oberfläche je Ursache eine eigene, übersetzte Meldung zeigen kann:
Ablehnung `1` kein Platz, `2` Nutzer lehnt ab, `3` zu groß, `4` beschäftigt;
Abbruch `1` Nutzer bricht ab, `2` Zeitüberschreitung, `3` Protokollfehler,
`4` Lesefehler.

### Strenge Regeln, und warum

Fünf der acht bisherigen Prüfdurchgänge fanden dasselbe Muster: Bytes, die
mitlaufen, aber von niemandem angesehen werden. Deshalb gilt hier:

- **Leere Rahmen dürfen keine Nutzlast tragen.** Ein Feld, das niemand ansieht,
  ist ein Kanal für beliebige Daten unter dem Deckmantel der Übertragung.
- **Das Angebot muss auf das Byte genau so lang sein wie angesagt.** Kein
  Anhängsel hinter dem Namen.
- **Unbekannte Rahmentypen und unbekannte Grund-Codes werden abgelehnt**, nicht
  übersprungen. Was wir nicht verstehen, transportieren wir nicht.
- **Ein Datenrahmen ohne Inhalt wird abgelehnt.** Er brächte die Übertragung
  nicht voran; beliebig viele davon hielten das Gerät endlos beschäftigt, ohne je
  gegen die angesagte Größe zu laufen.
- **Der Name im Angebot durchläuft dieselbe Lesbarkeitsprüfung wie Paketfelder**
  (`Texts.isUsable`). Unsichtbare Zeichen würden in der Oberfläche einen anderen
  Text vortäuschen als den übertragenen; kaputte UTF-8-Bytes werden zu
  Ersatzzeichen und fallen in dieselbe Prüfung. Besteht der Name sie nicht, wird
  er durch einen Platzhalter ersetzt und als ersetzt gekennzeichnet — die
  Übertragung läuft weiter. An einem Anzeigefeld darf kein Paketaustausch
  scheitern; die Sicherheitsentscheidung fällt ohnehin erst bei der
  Signaturprüfung. Auf dem Schreibweg gilt das Gegenteil: Was der Leseweg
  beanstanden würde, erzeugen wir gar nicht erst.

Kein JSON im Transferprotokoll: Der JSON-Parser ist zwar gehärtet, aber es gibt
keinen Grund, ihn unauthentifizierten Bytes vom Funk auszusetzen. Feste
Binärfelder mit harten Grenzen sind hier die kleinere Angriffsfläche.

## Ablauf

1. **Angebot** — Gesamtgröße, SHA-256, Anzeigename. Bewusst mehr nicht: Alles
   vor der Signaturprüfung ist eine Behauptung des Senders. Kennung, Version und
   Sprache liest die App später aus dem *geprüften* Paket. Der Anzeigename
   erscheint in der Oberfläche ausdrücklich als unbestätigte Angabe.
2. **Entscheidung** — Der Empfänger prüft den freien Platz und fragt den Nutzer.
   Annahme oder Ablehnung mit Grund.
3. **Übertragung** — Datenrahmen in eine Datei im app-eigenen Verzeichnis. Der
   Fortschritt ist ablesbar (empfangene ÷ angesagte Bytes).
4. **Abschluss** — Die Bytezahl muss exakt der Ansage entsprechen, die
   nachgerechnete SHA-256 dem Angebot. Abweichung heißt Übertragungsfehler; das
   Halbfertige wird gelöscht.
5. **Prüfung** — `PackVerifier` liest die Datei im Ein-Durchlauf-Verfahren.
   Urteil und Verhalten exakt wie bei manuell kopierten Dateien.
6. **Übernahme** — Nur bei ladbarem Urteil wandert die Datei ins
   Paketverzeichnis; sonst wird sie gelöscht und das Urteil angezeigt.

Die Prüfung „mehr Bytes als angesagt" greift **vor** dem Schreiben: Ein Byte
über der Ansage erreicht den Datenträger nie. Jeder Fehlschlag verwirft das
Halbfertige — ein halbes Paket sähe beim nächsten Start aus wie ein
vollständiges. Das gilt auch, wenn der Datenträger selbst versagt (voll,
entfernt): Der Fehler wird gefangen, aufgeräumt und als eigene Ursache gemeldet,
nicht als Paketfehler.

Nach dem Ende nimmt die Zustandsmaschine nichts mehr an — über **beide**
Eingänge, den Rahmenempfang wie die Nutzerentscheidung. Ein bereits fertiges
Paket wird dabei nie angetastet; ein doppelter Fingertipp auf „Annehmen" darf es
nicht löschen.

Der Empfang gilt erst als fertig, wenn das Geschriebene wirklich abgelegt ist.
Bliebe eine Restmenge ungeschrieben, ginge die abgeschnittene Datei anschließend
als beschädigtes oder manipuliertes Paket durch die Signaturprüfung — ein
Transportfehler, der wie ein Angriff aussähe.

Beide Seiten können auf Nutzerwunsch abbrechen (`abbrich`), und der Grund eines
Abbruchs der Gegenseite wird durchgereicht, damit die Oberfläche „Gegenüber hat
abgebrochen" von „Gegenüber konnte nicht mehr schreiben" unterscheiden kann.

Auf der Sendeseite gilt dasselbe für die eigene Seite: Lässt sich das Paket
nicht mehr lesen — entfernte Speicherkarte, gelöschte Datei —, bricht der Sender
mit einer **eigenen** Ursache ab, damit die Oberfläche nicht dem Gegenüber
anlastet, was hier schiefging.

Auf der Sendeseite werden ausschließlich Pakete aus der eigenen Bibliothek
angeboten — nie beliebige Dateien vom Gerät. Ändert sich die Datei während des
Sendens (mehr oder weniger Bytes als angesagt), bricht der Sender ab: Die
Prüfsumme im Angebot passt dann nicht mehr zum Inhalt.

## Diagnose: Übertragungsfehler und Manipulation auseinanderhalten

Beides darf nie verwechselt gemeldet werden — sonst sähe ein Angriff aus wie ein
Funkloch oder umgekehrt.

| Beobachtung | Meldung |
|---|---|
| Bytezahl oder Prüfsumme weichen ab | Übertragungsfehler, erneut versuchen |
| Vollständig angekommen, Signatur ungültig | Paket manipuliert oder beschädigt |
| Vollständig angekommen, Signatur gültig, Schlüssel unbekannt | nicht verifiziert, mit deutlicher Warnung |

## Grenzen

| Grenze | Wert | Grund |
|---|---|---|
| Nutzlast je Rahmen | 65 536 Byte | feste Puffergröße auf beiden Seiten |
| Name im Angebot | 200 Byte | Anzeige, nicht Datenkanal |
| Paketgröße | wie `.czp` (Envelope + höchstens 2 000 000 000 Byte Nutzlast) | aus dem Paketformat abgeleitet, nicht doppelt gepflegt |
