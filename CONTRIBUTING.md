# Mitarbeiten

Compass Zero ist ein Nachschlagewerk für Lagen, in denen niemand mehr
nachfragen kann. Ein falscher Satz darin ist kein Schönheitsfehler. Deshalb
steht die Fehlermeldung zum Inhalt in diesem Text an erster Stelle — noch vor
allem Technischen.

Verbindlich für alles Weitere sind die Grundregeln in
[`RULES.md`](RULES.md).

---

## 1. Einen Inhaltsfehler melden — der wichtigste Abschnitt

**Wenn ein Eintrag fachlich falsch, gefährlich oder missverständlich ist:
bitte melden, auch bei Unsicherheit.** Ein Hinweis, der sich als unbegründet
herausstellt, kostet eine Stunde Nachlesen. Ein Fehler, den niemand meldet,
kostet möglicherweise mehr.

Ein Issue mit dem Titel `Inhalt: <Kennung des Eintrags>` und diesen Angaben:

1. **Welcher Eintrag.** Die Kennung steht unter jedem Eintrag in der App
   (z. B. `erste-hilfe-gift-geschluckt-trinken`) und in den JSON-Dateien
   unter `content/`.
2. **Was daran falsch ist**, in einem Satz.
3. **Wogegen es falsch ist** — die Quelle, die es besser weiß: Dokument,
   Fassung oder Auflage, Seite, und wenn möglich der Wortlaut der
   maßgeblichen Stelle. Ein Link allein reicht nur, wenn die Stelle dort ohne
   Anmeldung im Volltext lesbar ist.
4. **Wie dringend.** Bitte offen benennen, wenn die Befolgung des Eintrags
   jemandem schaden könnte — solche Meldungen werden vorgezogen.

**Gegen einen Beleg diskutieren wir nicht mit Meinung.** Ein Eintrag wird
geändert, wenn eine benannte Quelle es trägt, nicht weil eine Formulierung
gefälliger klingt. Umgekehrt gilt aber genauso: Eine Aussage, die ihrer
eigenen angegebenen Quelle widerspricht, wird korrigiert, auch wenn sie
vorsichtig klingt.

Was besonders willkommen ist:

* Zahlen und Dosierungen, die von der genannten Quelle abweichen.
* Einträge, deren Rat für Kinder, Schwangere oder chronisch Kranke nicht
  taugt, ohne dass das dort steht.
* Suchwörter, die ins Leere oder auf das Falsche führen. Die Suche ist Teil
  des Inhalts: Wer im Ernstfall „Blut" eingibt und auf einem Kochkapitel
  landet, hat einen Fehler gefunden.
* Übersetzungs- und Verständlichkeitsfehler. Wenn ein Satz erst zweimal
  gelesen werden muss, ist er für den Ernstfall zu langsam.

Eine **Sicherheitslücke in der Software** gehört nicht hierher, sondern in
[`SECURITY.md`](SECURITY.md).

## 2. Inhalt beisteuern

Neue Einträge sind willkommen, unterliegen aber derselben Pflicht wie alle
vorhandenen (Regel 4 in [`RULES.md`](RULES.md)):

* **Eine benannte Quelle je Eintrag, selbst geladen und im Volltext
  gelesen.** Nicht die Zusammenfassung einer Suchmaschine, nicht aus dem
  Gedächtnis. Die Belegangabe nennt Dokument und Abrufdatum bzw. Fassung.
* **Die Lizenz der Quelle muss geklärt sein** und die Weitergabe unter
  CC BY-SA erlauben. Was das in der Praxis heißt, steht in
  [`content/europe-de/LICENSE`](content/europe-de/LICENSE); der
  Einzelnachweis-Stil ist an `content/europe-de/QUELLEN.md` abzulesen.
* **Eigene Formulierung und eigene Gliederung.** Auch Auswahl und Anordnung
  sind geschützt — eine kapitelweise Nacherzählung ist eine Bearbeitung, auch
  mit anderen Worten.
* **Zweiter Zweig „niemand kommt".** Endet ein Rat bei „Notruf" oder „zum
  Arzt", braucht er einen zweiten Zweig für den Fall, dass niemand kommt —
  und dieser Zweig wird im Text als Einordnung gekennzeichnet, damit
  unterscheidbar bleibt, was belegt ist und was Abwägung.
* **Die Suche mitliefern.** Zu jedem neuen Eintrag gehört, worauf die
  naheliegenden Stichwörter jetzt führen. Ein Titel ist keine Überschrift,
  sondern das am stärksten gewichtete Suchfeld.

## 3. Bauen

Voraussetzungen:

* **JDK 21.** Die Module setzen die Toolchain auf 21; eine andere JDK-Version
  im `PATH` ist in Ordnung, solange Gradle eine 21er findet.
  Notfalls `JAVA_HOME` auf ein JDK 21 setzen.
* **Android-SDK** mit API 35, gefunden über `ANDROID_HOME` — nur nötig, wenn
  auch `:androidApp` gebaut werden soll. Für die `core`-Module und
  `tools/packsign` genügt das JDK.
* Kein Netzzugang zur Laufzeit, aber beim ersten Bauen lädt Gradle seine
  Abhängigkeiten. Der Wrapper liegt mit fester Prüfsumme im Repo.

```
export JAVA_HOME=/pfad/zu/jdk-21
export ANDROID_HOME=/pfad/zu/android-sdk
./gradlew build
```

Nur die Bibliotheken und das Paketwerkzeug, ohne Android-Anteile:

```
./gradlew :core:security:jvmTest :core:content:jvmTest :core:karte:jvmTest \
          :core:transfer:jvmTest :tools:packsign:test
```

Zwei Dinge, die dabei auffallen sollen und nicht abgeschaltet werden dürfen:

* `allWarningsAsErrors` ist eingeschaltet. Eine Warnung ist ein Fehler.
* Die Aufgabe `keineLeerenQuelldateien` hängt vor jedem Bauen. Sie fällt, wenn
  eine Kotlin-Quelldatei 0 Byte hat — eine leere Testdatei kompiliert
  fehlerfrei und nimmt lautlos die Absicherung mit.

## 4. Ein Inhaltspaket prüfen

Pakete (`.czp`) sind signierte ZIP-Container, die ausschließlich Daten
enthalten. Prüfen mit dem mitgelieferten Werkzeug:

```
./gradlew :tools:packsign:installDist
tools/packsign/build/install/packsign/bin/packsign verify --in paket.czp --keys vertraut.txt
```

`vertraut.txt` enthält je Zeile `name=PublicKeyHex`. Ohne `--keys` wird der
Aufbau geprüft und der Inhalt aufgelistet, die Unterschrift aber als nicht
verifiziert gemeldet — das ist eine gültige Antwort, kein Fehler.

Die vollständige Beschreibung des Formats steht in
[`docs/PACK-FORMAT.md`](docs/PACK-FORMAT.md), die der signierten Zusatzdateien
(Karten, Bilder, Namen, Wege, Höhen) in
[`docs/SIGNATUR-ZUSATZDATEIEN.md`](docs/SIGNATUR-ZUSATZDATEIEN.md).

## 5. Was nicht angenommen wird

Diese Punkte sind nicht verhandelbar. Ein Pull Request, der einen davon
berührt, wird ohne inhaltliche Diskussion geschlossen:

* **Netzwerkcode jeder Art.** Keine Server-Aufrufe, keine Analytik, kein
  Absturzbericht, kein Aktualisierungskanal, keine Bibliothek, die im
  Manifest `INTERNET`, `ACCESS_NETWORK_STATE` oder `ACCESS_WIFI_STATE`
  deklariert. Dass die App diese Berechtigung nicht besitzt, ist eine vom
  Betriebssystem erzwungene und für jeden nachprüfbare Zusage — sie wird
  nicht gegen Bequemlichkeit eingetauscht. (Aus demselben Grund sind
  Wi-Fi Direct und Wi-Fi Aware ausgeschlossen: Ihr Datenkanal ist ein
  gewöhnlicher IP-Socket.)
* **Ausführbarer Code in Inhaltspaketen.** Pakete enthalten JSON, Text und
  Bilder. Keine Skripte, kein HTML mit Skript, keine Vorlagensprache, nichts,
  was ausgewertet statt angezeigt wird.
* **Quellen ohne geklärte Lizenz.** Auch dann nicht, wenn der Inhalt fachlich
  ideal wäre, und auch nicht „vorläufig". Sammlungen ohne eigenen
  Lizenznachweis gelten nicht als Nachweis.
* **Telemetrie, Kontenzwang, Werbung, Bezahlschranken** in jeder Form.
* Bibliotheken, die eine dieser Zusagen nur „in der Voreinstellung" einhalten.

## 6. Stil

* Code liest sich schlank und wird von Menschen gelesen: sprechende Namen,
  keine Überkommentierung. Ein Kommentar erklärt, **warum** etwas so ist —
  am liebsten mit dem Vorfall, der dazu geführt hat.
* Commit-Nachrichten in der Form `bereich: was sich ändert`, im
  Präsens, ohne Werbung.
* Bezeichner und Kommentare im Bestand sind überwiegend deutsch. Neue Arbeit
  fügt sich ein, statt zwei Sprachwelten nebeneinanderzustellen.
* Tests gehören dazu. Was das echte Auslieferungspaket betrifft, wird gegen
  das echte Paket geprüft, nicht gegen ein Beispiel.
