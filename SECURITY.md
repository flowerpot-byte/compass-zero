# Sicherheit

## Eine Schwachstelle melden

> **HIER MUSS NOCH EINE KONTAKTADRESSE EINGETRAGEN WERDEN.**
> `<<< E-Mail-Adresse für Sicherheitsmeldungen — bitte eintragen >>>`
> Wenn möglich mit einem öffentlichen PGP-Schlüssel (Fingerabdruck hier
> abdrucken), damit Meldungen verschlüsselt ankommen können.

**Bitte keine öffentlichen Issues für Sicherheitslücken.** Ein offenes Issue
ist eine Anleitung für jeden, der zuerst hinsieht.

Nützlich in einer Meldung:

* betroffene Fassung (App-Version oder Commit) und Gerät bzw. Android-Version,
* was ein Angreifer damit erreichen kann — nicht nur, was kaputt ist,
* der kürzeste Weg, es nachzustellen; eine Beispieldatei ist willkommen,
* wie lange gewartet werden soll, bevor Sie selbst darüber sprechen.

Es wird eine Empfangsbestätigung geben, sobald jemand die Meldung gelesen
hat, und danach eine Einschätzung, ob und wie es behoben wird. Dies ist ein
Freizeitprojekt ohne Bereitschaftsdienst; feste Fristen werden hier nicht
versprochen, weil sie nicht gehalten werden könnten.

**Besonders wichtig sind Funde, die eine dieser Zusagen brechen:**

1. Die App nimmt zu keiner Zeit Netzwerkverbindungen auf.
2. Ein Inhaltspaket kann nichts ausführen.
3. Ein Paket ohne gültige, bekannte Unterschrift wird nicht als vertrauenswürdig
   angezeigt.
4. Es werden keinerlei Nutzungsdaten erhoben oder gespeichert.

## Was in den Blick gehört

Die Angriffsfläche liegt fast vollständig dort, wo fremde Daten ins Gerät
kommen:

* das Einlesen und Prüfen von Paketen (`core/security`, `core/content`),
* der Geräte-zu-Geräte-Empfang über Bluetooth (`core/transfer`),
* die Karten- und Bilddateien und ihr Zeichnen (`core/karte`),
* das Ablegen empfangener Dateien im App-Speicher (Dateinamen, Pfade).

Ein Absturz beim Lesen einer beschädigten Datei ist bereits ein Befund: Diese
App wird in Lagen benutzt, in denen niemand eine neue Fassung nachliefern kann.

## Das Vertrauensmodell in Kurzform

**Alles, was von außen kommt, ist eine Datei — und jede Datei trägt ihre
Unterschrift bei sich.** Inhaltspakete (`.czp`) und die Zusatzdateien für
Karten, Bilder, Namen, Wege und Höhen sind Datencontainer mit einem
Umschlag, der eine **Ed25519**-Unterschrift über den gesamten Inhalt enthält.
Geprüft wird immer die ganze Datei, nicht ein Inhaltsverzeichnis, und es
dürfen keine Bytes darin liegen, die zu keinem Eintrag gehören.

**Zwei Schlüsselrollen** (festgelegt, siehe `RULES.md`, Regel 5):

* Ein **Wurzelschlüssel** wird offline aufbewahrt und unterschreibt
  ausschließlich Erklärungen — niemals Inhalte. Nur er gehört fest in die App.
* **Inhaltsschlüssel** unterschreiben Pakete. Sie werden dem Gerät durch eine
  vom Wurzelschlüssel unterschriebene Erklärung bekannt gemacht.

**Der Vertrauensspeicher** auf dem Gerät hält die bekannten öffentlichen
Schlüssel. Ein Paket, dessen Unterschrift von keinem bekannten Schlüssel
stammt, wird nicht abgewiesen, aber dauerhaft und deutlich als **nicht
verifiziert** ausgewiesen — die Entscheidung, es trotzdem zu lesen, trifft
sichtbar der Mensch.

**Noch nicht gebaut, aber entschieden und ausgearbeitet** — hier steht, was
kommt, damit niemand es für vorhanden hält:

* **Erklärungen** (`.czs`): kleine, vom Wurzelschlüssel unterschriebene
  Dateien, die dieselben Wege reisen wie Pakete — kopiert, per Bluetooth, per
  QR. Drei Arten: einen Schlüssel benennen, einen Schlüssel entziehen, ein
  Paket zurückrufen. Der Zustand wächst nur in eine Richtung; ein Entzug
  lässt sich nicht aufheben. Inhalte eines entzogenen Schlüssels bleiben
  **lesbar**, mit dauerhafter, nicht wegklickbarer Warnung — in einer Lage
  ohne Netz ist ein plötzlich leeres Handbuch die schlechtere Gefahr.
* **Rückstufungs-Schutz**: Die App merkt sich je Paket-Kennung die höchste je
  angenommene Version. Die Marke steigt nur und überlebt das Löschen des
  Pakets; ältere Fassungen werden abgelehnt. Für eine defekte neue Fassung
  gibt es einen ausdrücklichen Notausgang, der die Marke nicht senkt.
  Zeitstempel dienen nirgends als Anker, denn ein Gerät ohne Netz hat keine
  verlässliche Uhr.

Bis dahin gilt: Ein einmal bekannter Schlüssel bleibt bekannt, und die
Versionsprüfung eines Pakets stützt sich allein auf das, was im Paket steht.

**Was das Modell nicht leistet:** Es beweist die Herkunft einer Datei, nicht
die Richtigkeit ihres Inhalts. Wer den Wurzelschlüssel besitzt, kann alles
erklären — deshalb liegt er offline. Und wer ein Gerät physisch in der Hand
hält, ist außerhalb dessen, was diese App abwehren kann.

Ausführlich: [`docs/PACK-FORMAT.md`](docs/PACK-FORMAT.md),
[`docs/SIGNATUR-ZUSATZDATEIEN.md`](docs/SIGNATUR-ZUSATZDATEIEN.md),
[`docs/ENTWURF-SCHLUESSELWIDERRUF.md`](docs/ENTWURF-SCHLUESSELWIDERRUF.md) und
[`docs/ENTWURF-RUECKSTUFUNGSSCHUTZ.md`](docs/ENTWURF-RUECKSTUFUNGSSCHUTZ.md).
Die beiden letzten sind Entwürfe: Die Entscheidungen stehen, gebaut ist noch
nicht alles.

## Veröffentlichte Fassungen prüfen

APK-Releases werden signiert und mit SHA-256-Prüfsumme veröffentlicht, damit
sich Echtheit auch ohne Netz nachrechnen lässt. Die Verteilung läuft ohne
App-Store; die Prüfsumme ist damit die einzige Echtheitsprüfung, die Nutzer
haben. Sie gehört nachgerechnet, nicht überflogen.
