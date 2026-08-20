# Compass Zero — Konzept & Architektur

Stand: 2026-07-27 (erster Entwurf, noch kein Code)

## 1. Vision

Ein quelloffenes, komplett offline laufendes Überlebens-Handbuch fürs Handy — auch für alte, leistungsschwache Geräte. Durchsuchbare Überlebenstipps, Bauanleitungen, ein Landwirtschafts-/Zivilisations-Aufbau-Guide und eine Offline-Karte mit Wasserstellen, Aussichtspunkten und Wegpunkten. Kein Internet, keine Datensammlung, kein Tracking. Inhalte digital signiert gegen Manipulation. Sparmodus für Ernstfälle mit minimalem Akkuverbrauch.

Zielgruppe: Menschen, die sich auf Katastrophen-/Krisen-/Kriegs-/Stromausfall-Szenarien vorbereiten wollen ("Prepper"-Community und allgemein Interessierte), oft mit älteren Android-Geräten, oft ohne verlässlichen Internetzugang, teils grundsätzlich skeptisch gegenüber großen Tech-Anbietern.

## 2. Nicht verhandelbare Eckpfeiler

Siehe [`../RULES.md`](../RULES.md) für die vollständige, verbindliche Fassung. Kurzfassung:

- Kein Internetzugriff, niemals
- Keine Datensammlung/kein Tracking
- Schlanker, handgeschriebener Code-Stil; nüchterner Auftritt nach außen
- Inhaltliche Verlässlichkeit hat oberste Priorität (Fehler können Leben kosten)
- Sicherheitsarchitektur (Signaturen, kein Ausführungscode in Paketen) ist Kernprinzip
- Qualität vor Zeit/Kosten, keine Shortcuts, konsequente Selbstprüfung

## 3. Architektur

**Kotlin Multiplatform (KMP).** Ein gemeinsames `core`-Modul enthält alles, was nicht UI ist. Jede Plattform bekommt eine eigene native Oberfläche darüber. Reihenfolge: Android zuerst (natives Ziel für V1), Desktop (Windows/Linux/Mac) und iOS folgen später auf derselben `core`-Basis.

**Module:**

- `core/content` — Datenmodell, Prüfung und Lader für Überlebenstipps, Bauanleitungen, Landwirtschafts-/Zivilisations-Guide, Offline-Karten-POIs; dazu der Suchindex über alle Wissensarten (gebaut)
- `core/security` — Signaturprüfung für Inhalts-Pakete, Verwaltung vertrauenswürdiger Schlüssel (gebaut)
- `tools/packsign` — Maintainer-Werkzeug zum Erzeugen, Signieren und Prüfen von Paketen (gebaut)
- `core/transfer` — Geräte-zu-Geräte-Austausch von Paketen (Bluetooth, WLAN-Direct, NFC, QR-Code), nutzt `core/security` für jede eingehende Übertragung ausnahmslos
- `androidApp` — UI (Normal- + Sparmodus), Kartenrendering, Einstellungen
- `desktopApp`, `iosApp` — später, gleiche `core`-Basis, eigene schlanke Oberfläche

**Grundsatz beim Lesen von Paketen:** Prüfung und Entpacken laufen in einem einzigen Lesedurchlauf; Inhalt wird nur herausgegeben, wenn die Signatur am Ende hält. Ein Ablauf aus „erst prüfen, dann nachlesen" wäre angreifbar — Einzelheiten in [`PACK-FORMAT.md`](PACK-FORMAT.md).

**Inhalts-Pakete als Dateien.** Jedes Paket (z. B. "Europa-Karte", "Englisch-Sprachpaket", "Bauanleitungen-Basis") ist eine einzelne signierte Datendatei (JSON/Text/Bilder — nie Code). Die Standard-App bringt ein Europa-Paket vorinstalliert mit. Andere Versionen bringen andere Pakete mit (Welt, einzelne Kontinente, einzelne Länder, weitere Sprachen), oder Nutzer legen zusätzliche Pakete manuell oder per Geräte-zu-Geräte-Transfer ab. Ein Gradle-basiertes Build-System stellt automatisch verschiedene Kombinationen aus vorinstallierten Paketen zu fertigen APKs zusammen.

## 4. Sicherheitsmodell

- Keine Internet-Berechtigung im Android-Manifest — vom Betriebssystem erzwungen, nicht nur versprochen.
- Inhalts-Pakete sind strikt Daten; der Parser kann grundsätzlich keinen Code ausführen.
- Jedes Paket ist digital signiert. Die App vertraut eingebauten öffentlichen Schlüsseln (z. B. dem des Maintainers). Unsignierte/unbekannt signierte Pakete werden klar als "nicht verifiziert" markiert; Standardverhalten ist Warnung, nicht stille Ablehnung oder stille Annahme.
- Geräte-zu-Geräte-Übertragung nutzt exakt dieselbe Signaturprüfung wie manuell kopierte Dateien.
- Veröffentlichte APK-Releases sind signiert, mit SHA-256-Prüfsumme, damit Echtheit auch offline (z. B. vorher notierte Prüfsumme) verifizierbar ist.
- Reproduzierbare Builds, damit Dritte den veröffentlichten Quellcode gegen die APK verifizieren können.
- Minimal-Berechtigungen: nur technisch zwingend Nötiges (z. B. Bluetooth für P2P-Transfer).

## 5. Design-Sprache

**Normal-Modus:** Editorial-Look — organische schwarze Blob-Formen als grafisches Element, viel Weißraum, gepunktete Textur als dezenter Bildplatzhalter, Serifen-Überschriften kombiniert mit fettem Sans für Akzente. Ergänzt um Einflüsse aus der Design-Sprache von Nothing (Phones): monochromes Grundgerüst mit genau einem Signalakzentton (z. B. Warnorange nur für kritische Hinweise), Dot-Matrix-Schrift für Zahlen/Koordinaten/Status, und ein "sichtbare Innereien"-Prinzip, übertragen auf Transparenz der Inhaltsquellen (jede Info zeigt nachvollziehbar ihre Quelle).

**Sparmodus (Ernstfall):** Kein abgeschwächter Dark-Mode, sondern ein eigener, radikal reduzierter Render-Pfad — reines Schwarz als Hintergrund (spart auf OLED-Displays real Akku), Text in Weiß/Gelb, sehr groß, reine serifenlose Schrift, keine Blob-Formen/Texturen/Bilder/Animationen, einspaltiges Layout mit großen Tippflächen. Ziel: maximale Lesbarkeit und Bedienbarkeit unter Stress, minimaler Akkuverbrauch.

**Navigation (V1, beschlossen 27.07.2026):** Vier Hauptbereiche statt Inhalts-Säulen als Navigation:

1. **Lexikon/Suche** — ein durchsuchbares Nachschlagewerk über Überlebenstipps, Bauanleitungen und Landwirtschafts-/Zivilisations-Guide hinweg, mit Filtern nach Art und Kategorie (die drei Wissensarten bleiben im Datenmodell getrennt; das Lexikon ist die gemeinsame Ansicht darüber).
2. **Karte** — Offline-Karte mit POIs.
3. **Übersetzer** — V1: fester Phrasenkatalog; in der Oberfläche klar als ausbaubare Funktion gekennzeichnet (spätere Ausbaustufen: freie Übersetzung, Kamera-OCR).
4. **Einstellungen/Verbindung** — Paketverwaltung, Geräte-zu-Geräte-Austausch, Sparmodus-Umschaltung.

Für diese vier Bereiche gilt der Icon-Satz in `design/logo/bereich-icons.svg` (Buch mit Lupe, Faltkarte, Sprechblasen, Regler) — entschieden am 27.07.2026.

**Marke:** Als Wortmarke gilt `design/logo/wortmarke.svg` (Serifen-„Compass" über „ZERO" im Punktraster, die Null mit orangem Schrägstrich).

**App-Icon:** `design/logo/app-icon.svg` — ein Kompass, der auf dem aufgeschlagenen Handbuch liegt, nach rechts unten versetzt, sodass er wie ein aufgelegter Gegenstand wirkt (entschieden am 27.07.2026; die mittige Variante liegt als `design/logo/handbuch-kompass.svg` daneben und ist verworfen).

Für Android kommt zusätzlich `design/logo/app-icon-adaptiv.svg` zum Einsatz: dasselbe Motiv verkleinert und mittig, weil das Betriebssystem Icons rund oder abgerundet zuschneidet und beim versetzten Original der Kompass angeschnitten würde. Der Buchkörper ist schwarz — die Hintergrundebene des Icons muss deshalb hell sein (Papierton), sonst verschwindet das Buch.

Für Android-Themed-Icons liegt `design/logo/app-icon-einfarbig.svg` bereit: eine einzige Silhouette, in die Falz, Kompassring und Nadel als Löcher geschnitten sind. Der Kompass liegt dort vollständig auf der Buchfläche — ragte er darüber hinaus, kippte die Form außerhalb des Buches um und der Ring würde massiv statt hohl.

**Kompass (V1):** Die App zeigt einen einfachen Kompass aus dem Magnetfeldsensor des Geräts. Wichtigster Zweck ist nicht die Anzeige selbst, sondern das automatische Ausrichten der Karte in Blickrichtung — im Gelände ist eine gedrehte Karte deutlich leichter zu lesen. Die Ausrichtung ist abschaltbar (feste Nordausrichtung), weil der Sensor in der Nähe von Metall oder Elektronik falsch zeigen kann; darauf muss die Oberfläche hinweisen. Der Kompass braucht keine Berechtigung und keine Ortung.

## 6. Content-Strategie

- Struktur, Durchsuchbarkeit und Aufbau der Inhalte werden von uns entworfen.
- Die fachlichen Fakten (Erste Hilfe, Bautechnik, Landwirtschaft, Navigation) stammen aus öffentlichen, anerkannten Quellen und werden gegengeprüft, bevor sie übernommen werden — siehe Regel 4 in [`../RULES.md`](../RULES.md).
- Recherche zu Fakten erfolgt später aus öffentlichen Quellen.
- Jede übernommene Faktenquelle wird dokumentiert (Nachvollziehbarkeit, Lizenzprüfung analog zur Drittlizenzen-Praxis bei Geoscout2.0).

## 7. V1-Umfang

Alle vier Inhalts-Säulen sind von Anfang an Teil von Version 1 (kein reduzierter MVP):

1. Überlebens-Tipps-Datenbank (durchsuchbarer Text)
2. Offline-Karte mit POIs (Wasserstellen, Aussichtspunkte, Wegpunkte)
3. Bauanleitungen (Seilzug, Wasserfilter, Generator, u. a.)
4. Landwirtschafts-/Zivilisations-Aufbau-Guide

Zusätzlich fest in V1:

- Sparmodus (radikal reduzierte, akkusparende Darstellung)
- Geräte-zu-Geräte-Paketaustausch mit vollständiger Signaturprüfung
- Übersetzer Stufe 1: fester Phrasenkatalog (auch im Sparmodus), in der Oberfläche sichtbar als ausbaubare Funktion gekennzeichnet — am 27.07.2026 aus dem Backlog vorgezogen

## 8. Plattform-Rollout

1. Android (nativ, `core` + `androidApp`) — V1-Ziel
2. Desktop (Windows/Linux/Mac über KMP) — später, gleiche `core`-Basis
3. iOS — später, gleiche `core`-Basis

Entwicklung/Debugging erfolgt am PC über Android Studio (Emulator mit einstellbaren alten Android-Versionen/schwacher Hardware simuliert; echtes altes Gerät zusätzlich für reale Akkumessungen empfohlen).

## 9. Später / Backlog (bewusst nicht in V1)

- Offline-Übersetzung Ausbaustufen (Stufe 1, der feste Phrasenkatalog, ist seit 27.07.2026 fester V1-Umfang): später freie Texteingabe-Übersetzung via kleines Offline-Sprachmodell (z. B. Argos-Translate-Ansatz), zuletzt Kamera-OCR zum Lesen/Übersetzen von Schildern — jeweils nur in der "voll ausgestatteten" Version, nicht im Sparmodus.
- Weitere Regionen-/Sprachpakete über die Standard-Europa-Version hinaus.
- Desktop- und iOS-Versionen.

## 10. Namensfindung

Gewählt: **Compass Zero** — "Zero" steht für null Netzwerkverbindung.
