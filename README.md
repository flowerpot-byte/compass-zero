# Compass Zero

Quelloffenes, komplett offline laufendes Überlebens-Handbuch fürs Handy (auch alte Geräte) — für Katastrophen-, Krisen- und Apokalypse-Szenarien ohne Internet.

Durchsuchbare Überlebenstipps, Bauanleitungen, ein Landwirtschafts-/Zivilisations-Aufbau-Guide und eine Offline-Karte mit Wasserstellen, Aussichtspunkten und weiteren Wegpunkten. Kein Internet, keine Datensammlung, kein Tracking. Inhalte digital signiert gegen Manipulation. Zwei Darstellungsmodi: ein gestaltetes Normal-Design und ein radikal reduzierter, akkusparender Sparmodus für den Ernstfall.

**Status:** In Entwicklung. Die Kernbibliotheken stehen und sind getestet: Paketformat, Signaturprüfung, Inhalts-Schemata und Suche. Die Android-App folgt als nächste Baustufe.

## Aufbau

- `core/security` — Paketformat `.czp`, Ed25519-Signaturprüfung, Vertrauensspeicher
- `core/content` — Datenmodell und Prüfung der Inhalte, Paket-Lader, Suchindex
- `core/karte` — Kartenformat und Zeichnen, ohne fremde Kartenbibliothek
- `core/transfer` — Geräte-zu-Geräte-Übertragung von Paketen
- `tools/packsign` — Pakete erzeugen, signieren und prüfen
- `content/europe-de` — das Basispaket samt Belegliste `QUELLEN.md`

## Bauen

Gebaut wird mit Gradle (Wrapper im Repo, feste Prüfsumme) und Kotlin Multiplatform.

Voraussetzungen:

- **JDK 21** — alle Module setzen die Toolchain darauf. Notfalls `JAVA_HOME`
  auf ein JDK 21 zeigen lassen.
- **Android-SDK mit API 35**, gefunden über `ANDROID_HOME`. Nur nötig für
  `:androidApp`; die Bibliotheken und `tools/packsign` bauen ohne.
- Beim ersten Lauf lädt Gradle seine Abhängigkeiten aus dem Netz. Die
  fertige App tut das nie.

```
export JAVA_HOME=/pfad/zu/jdk-21
export ANDROID_HOME=/pfad/zu/android-sdk
./gradlew build
```

Einzelheiten, auch zum Beisteuern von Inhalten, in
[`CONTRIBUTING.md`](CONTRIBUTING.md).

## Eine App mit Inhalt bauen

`./gradlew build` allein ergibt eine App **ohne Handbuch**: Der Inhalt reist als
signiertes Paket (`.czp`), und das liegt nicht im Repo — nur die Quelltexte
unter `content/europe-de/paket/`. Das Paket entsteht in drei Schritten, und der
Schlüssel dafür ist deiner:

```
./gradlew :tools:packsign:installDist
P=tools/packsign/build/install/packsign/bin/packsign

mkdir -p work/build
$P keygen --dir work/devkey --name entwicklung
$P pack   --in content/europe-de/paket --out work/build/europe-de.zip
$P sign   --key work/devkey/entwicklung.secret \
          --in work/build/europe-de.zip --out work/build/europe-de.czp

./gradlew :androidApp:assembleDebug
```

Der Bau kopiert `work/build/europe-de.czp` selbst in die App-Beigaben; von Hand
in `androidApp/src/main/assets/` zu kopieren bringt nichts, das wird
überschrieben. Fehlt die Datei ganz, baut die App trotzdem — sie startet dann
ohne Handbuch.

**Was oben in der App steht, wenn du selbst signiert hast:** `UNBEKANNTER
SIGNIERER` und der Fingerabdruck deines Schlüssels. Das ist kein Fehler,
sondern die ehrliche Auskunft: Alle 600-und-mehr Einträge sind da und
durchsuchbar, aber die App kennt deinen Schlüssel nicht. Sie kennt genau einen,
und der steht als `SCHLUESSEL_ENTWICKLUNG` im Quelltext. Wer will, dass sein
eigenes Paket als geprüft gilt, trägt dort seinen öffentlichen Schlüssel aus
`work/devkey/entwicklung.public` ein.

Die Unterschrift entscheidet **nicht** darüber, ob ein mitgeliefertes Paket
geladen wird — sie sagt nur, woher es kommt. Anders bei einem Paket, das von
einem anderen Gerät hereinkommt: Das muss von einem bekannten Schlüssel
stammen, dieselbe Paketkennung tragen und neuer sein als das, was schon da ist.
Die Einzelheiten stehen in [`SECURITY.md`](SECURITY.md).

## Ein Inhaltspaket prüfen

Ein Paket (`.czp`) ist ein signierter Container, der ausschließlich Daten
enthält — nie ausführbaren Code. Prüfen:

```
./gradlew :tools:packsign:installDist
tools/packsign/build/install/packsign/bin/packsign verify --in paket.czp --keys vertraut.txt
```

`vertraut.txt` enthält je Zeile `name=PublicKeyHex`. Ohne `--keys` werden
Aufbau und Inhalt geprüft und aufgelistet, die Unterschrift aber als nicht
verifiziert gemeldet. Das Format ist vollständig in
[`docs/PACK-FORMAT.md`](docs/PACK-FORMAT.md) beschrieben; zum Vertrauensmodell
siehe [`SECURITY.md`](SECURITY.md).

## Der Übersetzer wird ohne Satzliste ausgeliefert

Der Übersetzer-Bereich ist gebaut und bedienbar, sein **Satzkatalog fehlt in
dieser Fassung**. Vorgesehen war dafür ein veröffentlichtes
Notfall-Phrasenblatt für die Verständigung zwischen Helfer und Patient ohne
gemeinsame Sprache — es steht unter keiner freien Lizenz. Eine Anfrage beim
Rechteinhaber läuft und ist unbeantwortet; bis eine Antwort vorliegt, wird die
Datei nicht weitergegeben. Der Bereich sagt das offen an: „Dieses Inhaltspaket
enthält keinen Phrasenkatalog."

Wer einen eigenen, frei lizenzierten Katalog hat, kann ihn als
`content/phrases.json` in ein Paket legen und `phrases` in die Arten des
Manifests eintragen — das Format ist beschrieben und die Prüfung dafür ist
vorhanden. Stand der Lizenzfrage:
[`LIZENZANFRAGEN.md`](LIZENZANFRAGEN.md).

## Lizenz

- **Programmcode:** GNU General Public License Version 3 — [`LICENSE`](LICENSE).
- **Inhalte** unter `content/`: Creative Commons BY-SA 4.0 —
  [`content/europe-de/LICENSE`](content/europe-de/LICENSE). Die Faktengrundlage
  sind gemeinfreie US-Bundeswerke (17 U.S.C. §105) und Fachliteratur mit
  abgelaufener Schutzfrist; der Einzelnachweis steht in
  [`content/europe-de/QUELLEN.md`](content/europe-de/QUELLEN.md).

Beides liegt bewusst getrennt, damit die Bedingungen nicht vermischt werden.

## Projektdateien

- [`docs/DESIGN.md`](docs/DESIGN.md) — Konzept, Architektur, Sicherheitsmodell, Design-Sprache
- [`docs/PACK-FORMAT.md`](docs/PACK-FORMAT.md) — verbindliche Referenz für das Paketformat `.czp`
- [`RULES.md`](RULES.md) — feste Grundregeln des Projekts
- [`ROADMAP.md`](ROADMAP.md) — Aufgaben- und Fortschrittsliste, V1-Umfang und Backlog
- [`docs/MINDMAP.md`](docs/MINDMAP.md) — Mindmap über das gesamte Projekt
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — Inhaltsfehler melden, bauen, was nicht angenommen wird
- [`SECURITY.md`](SECURITY.md) — Schwachstellen melden, Vertrauensmodell

## Warum "Compass Zero"

"Zero" steht für null Netzwerkverbindung — der Kompass, der auch dann funktioniert, wenn nichts anderes mehr geht.

## Haftung und Gewährleistung

Die Inhalte dieser App sind ein Nachschlagewerk, kein Ersatz für Fachleute.
Jede Angabe stammt aus einer benannten, öffentlich zugänglichen Quelle, ist dort
im Volltext nachgelesen und im Paket mit Beleg versehen (siehe
[`content/europe-de/QUELLEN.md`](content/europe-de/QUELLEN.md)). Trotzdem gilt
sie **ohne Gewähr für Richtigkeit, Vollständigkeit und Aktualität**.

- Die Inhalte ersetzen keine ärztliche Beratung, Untersuchung oder Behandlung
  und keinen Erste-Hilfe-Kurs. Wer Hilfe erreichen kann, soll Hilfe holen — in
  Europa über die 112.
- Mengen- und Dosierungsangaben sind aus der jeweils genannten Quelle
  übernommen und beziehen sich auf den dort beschriebenen Zusammenhang. Sie
  sind keine ärztliche Verordnung und berücksichtigen weder Alter,
  Körpergewicht, Vorerkrankungen, eine Schwangerschaft noch andere eingenommene
  Mittel.
- Die Nutzung geschieht auf eigene Verantwortung. Eine Haftung für Schäden aus
  der Nutzung dieser Inhalte wird ausgeschlossen, soweit das gesetzlich
  zulässig ist. Wo das Gesetz einen Ausschluss nicht zulässt — bei Vorsatz, bei
  grober Fahrlässigkeit und bei Schäden an Leben, Körper und Gesundheit —, gilt
  er nicht.

Derselbe Hinweis liegt als Tipp `hinweis-angaben-ohne-gewaehr` im Inhaltspaket,
damit er auch offline und ohne diese Datei erreichbar ist.
