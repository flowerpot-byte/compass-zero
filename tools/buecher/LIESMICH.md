# Gescannte Bücher einlesen

Für das Projekt stehen drei gedruckte Bücher zur Verfügung, die als
Themenlandkarte dienen (siehe `LIZENZANFRAGEN.md`, Abschnitt „Grundsatz für
Wissen aus geschützten Büchern"). Sie werden mit einer Scanner-App auf dem
Telefon abfotografiert. Dieses Werkzeug holt die Scans ab und macht sie
durchsuchbar.

## Warum es das gibt

Die Scanner-App liefert **reine Bild-PDFs** — Texterkennung gibt es dort nur
gegen Bezahlung. Ohne erkannten Text wäre jede Seite nur als Foto lesbar, was
bei mehreren hundert Seiten nicht praktikabel ist. Die Erkennung läuft deshalb
hier auf dem Rechner mit Tesseract.

## Voraussetzungen

- **Tesseract** unter `C:\Program Files\Tesseract-OCR\tesseract.exe`
  (Sprachen: `eng` ist installiert, `deu` nicht — die Bücher sind englisch).
  Falls es fehlt: `winget install --id UB-Mannheim.TesseractOCR`
- **Telefon per Kabel**, USB-Debugging eingeschaltet, Rückfrage auf dem
  Telefon bestätigt. Prüfen mit
  `& "$env:USERPROFILE\Android\Sdk\platform-tools\adb.exe" devices` — das
  Telefon muss neben `emulator-5554` auftauchen.
- Die Scans müssen **auf dem Telefon liegen**, nicht nur in der Cloud der App.
  In der Scanner-App: Teilen → „Kopie speichern", dann landen sie unter
  `/sdcard/Download`.

## Aufrufe

    python tools/buecher/einlesen.py --liste
    python tools/buecher/einlesen.py
    python tools/buecher/einlesen.py --nur alton

Der erste Aufruf zeigt nur, was auf dem Telefon liegt und was davon schon
eingelesen ist. Der zweite holt alles Neue und liest es ein. Bereits
eingelesene Dateien werden übersprungen — ein zweiter Lauf kostet nichts. Wer
eine Datei neu einlesen will, löscht ihre `.txt`.

Vor jedem Aufruf in PowerShell:

    Set-Location <Projektordner>
    $env:PYTHONIOENCODING = "utf-8"

und Python mit vollem Pfad aufrufen
(`$env:LOCALAPPDATA\Programs\Python\Python312\python.exe`).

## Was dabei herauskommt

Unter `work/quellen/buecher/` liegen je Scan zwei Dateien: die PDF und eine
`.txt` mit Seitenmarken `===== Seite N =====`. Der Ordner ist wie alle
Rohquellen **nicht versioniert**.

Gemessen an einer Probe vom 04.08.2026: 42 Seiten ergaben 7850 Wörter, die
Erkennung war auf normalen Textseiten praktisch fehlerfrei — einschließlich
Silbentrennung am Zeilenende. Rechne mit etwa drei Sekunden je Seite, also
rund zehn Minuten für ein 200-seitiges Kapitelpaket.

Titelseiten und Kapitel-Trennblätter liefern nur eine Handvoll Wörter. Das ist
kein Fehler der Erkennung, sondern steht so im Buch.

## Und dann?

**Der Text ist Recherchematerial, nicht Inhalt.** Es gilt unverändert Regel 4
und der Grundsatz aus `LIZENZANFRAGEN.md`:

- Fakten daraus in eigenen Worten formulieren, nie abschreiben.
- Eigene Gliederung behalten — eine kapitelweise Nacherzählung wäre eine
  Bearbeitung, auch mit anderen Worten.
- Bei Dosierungen und Grenzwerten zusätzlich eine Primärquelle heranziehen.
- **Keine Abbildungen übernehmen, auch nicht nachgezeichnet.** Eine
  nachgezeichnete Abbildung ist eine Bearbeitung, kein freier Fakt. Die
  Zeichnungen taugen als Hinweis darauf, *welche* Skizze ein Tipp braucht —
  die Skizze selbst entsteht neu aus dem Text.

Was noch fehlt und wofür die Bücher gebraucht werden, steht als Lückenliste in
`ROADMAP.md`. Nach dem Fund des gemeinfreien SOF Medical Handbook sind die
größten verbliebenen Lücken **Schwangerschaft und Geburt**, **chronisch Kranke
ohne Nachschub** und der gesamte **Agrikultur-Bereich**.
