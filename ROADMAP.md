# Roadmap — Compass Zero

Stand: 2026-07-27. Das Grundgerüst der Kernbibliotheken steht und ist getestet.

> **BLOCKIEREND seit 17.08.2026: Das Europa-Paket ist an seiner Wortgrenze
> angekommen — noch rund ein Eintrag Platz.** Zahlen und die Entscheidung, die
> nur Max treffen kann, stehen unten unter
> „Das Wortbudget des Europa-Pakets ist voll".

## Version 1 (alle Punkte sind V1-Ziel, kein reduzierter MVP)

### Zielplattformen

**Von Max am 28.07.2026 festgelegt: GrapheneOS ist Pflichtziel, nicht Beiwerk.**
Die App soll nicht nur auf gewöhnlichem Android (und später PC) laufen, sondern
ausdrücklich auch auf GrapheneOS. Das passt zur Zielgruppe: Wer GrapheneOS
installiert, will genau das, was dieses Projekt verspricht — keine
Datensammlung, keine Abhängigkeit von fremden Diensten, nachprüfbare Software.

Was daraus folgt und beim Bau der Android-Stufe zu beachten ist:

- [ ] **Keine Google-Play-Dienste als Voraussetzung.** Auf GrapheneOS sind sie
      standardmäßig nicht vorhanden. Für dieses Projekt ohnehin ausgeschlossen
      (Regel 2), aber es ist beim Wählen jeder Bibliothek zu prüfen: Viele
      Android-Bibliotheken ziehen GMS still mit herein
- [ ] **Berechtigungen streng nach Regel 5 halten.** GrapheneOS kann jede
      Berechtigung einzeln verweigern und ersetzt den Speicherzugriff durch
      Storage Scopes. Die App muss auch dann brauchbar bleiben, wenn ihr der
      Nutzer nur den Zugriff auf eine einzige ausgewählte Datei erlaubt — das
      Einspielen von Inhalts-Paketen ist der Testfall
- [ ] **Bluetooth-Übertragung gegen die strengere Rechteverwaltung prüfen**,
      sobald `core/transfer` die Android-Schicht bekommt
- [ ] **Auf einem GrapheneOS-Gerät testen, nicht nur im Emulator.** Offiziell
      unterstützt werden Pixel-Geräte; ohne echtes Gerät bleibt es eine
      Behauptung
- [ ] Reproduzierbare Builds und die SHA-256-Prüfsummen (siehe Build & Vertrieb)
      wiegen für diese Zielgruppe schwerer als für jede andere — sie sind der
      einzige Weg, die APK ohne Store zu überprüfen

### Grundgerüst
- [x] KMP-Projekt aufgesetzt (Gradle-Wrapper mit fester Prüfsumme, Abhängigkeits-Prüfsummen, Versionskatalog). Bisher nur JVM-Ziel; das Android-Ziel und `androidApp` kommen in der nächsten Baustufe (braucht das Android-SDK)
- [x] Content-Datenmodell definiert (Tipps, Bauanleitungen, Landwirtschaft/Zivilisation, Karten-POIs) samt Prüfung, Fehlersammlung und Quellenpflicht
- [x] Format für Inhalts-Pakete festgelegt und dokumentiert (siehe [`docs/PACK-FORMAT.md`](docs/PACK-FORMAT.md))
- [x] `core/security` — Signaturprüfung in einem einzigen Lesedurchlauf, Vertrauensspeicher, gehärteter Container
- [x] `tools/packsign` — Werkzeug zum Erzeugen, Signieren und Prüfen von Paketen (deterministisch, prüft Inhalte vor dem Signieren)
- [ ] `core/transfer` — Geräte-zu-Geräte-Austausch, durchgängig über `core/security` geprüft. Rahmenformat und Codec stehen (plattformneutral, ohne SDK testbar); Zustandsmaschinen und der Android-Transport fehlen noch. **Entschieden am 28.07.2026 durch Max:** Bluetooth ist der einzige Weg für ganze Pakete, NFC und QR nur für Kleinstdaten. WLAN-Direct und Wi-Fi Aware sind ausgeschlossen, weil sie die Internet-Berechtigung verlangen; Regel 2 ist entsprechend geändert. Damit ist der Android-Transport nicht mehr blockiert

### Inhalte (Fakten aus geprüften öffentlichen Quellen, siehe RULES.md #4)
- [x] **LÜCKENLISTE aus dem Inhaltsverzeichnis von „The Ultimate Survival
      Medicine Guide"** (Max hat am 28.07.2026 Fotos des Inhaltsverzeichnisses
      geschickt; ein Inhaltsverzeichnis ist kein geschütztes Werk, übernommen
      ist ausschließlich die Themenliste, kein Satz Text). Abgeglichen mit
      unseren 123 Tipps — **diese Themen fehlen bei uns komplett** und sind
      nach Nutzen für die Grundannahme „niemand kommt, jahrelang" sortiert:
      1. **Chronisch Kranke ohne Nachschub** — Diabetes, Bluthochdruck,
         Epilepsie, Schilddrüse. Was tut jemand, dessen Insulin oder
         Blutdruckmittel zur Neige geht? Das trifft in einer langen Krise
         Millionen und steht bei uns nirgends
      2. **Zahnmedizin** — Zahnschmerz, Abszess, verlorene Füllung. Ein
         Zahnabszess kann tödlich enden und ist ohne Zahnarzt ein reales
         Dauerproblem
      3. **Infektionen erkennen und einordnen** — Blinddarmentzündung (und
         was sie nachahmt), Harnwegsinfekt, Wundrose, Pilzinfektionen,
         Hepatitis
      4. **Atemwegsinfekte** und **Krankenzimmer einrichten** (Isolierung,
         Pflege, Ansteckung im Haushalt begrenzen)
      5. **Schwangerschaft und Geburt ohne Hebamme**
      6. **Medikamentenvorrat**: was gehört hin, wie lagert man, was ist nach
         Ablauf noch brauchbar
      7. **Viele Verletzte gleichzeitig (Sichtung/Triage)** und
         **Verletztentransport** ohne Rettungsdienst
      8. **Läuse** (Zecken haben wir), **Höhenkrankheit**, **Kopfschmerz**,
         **Augenverletzungen**, **Ohrenschmerz**, **Hämorrhoiden**
      9. ~~**Naturheilmittel**~~ — ERLEDIGT 02.08.2026 als „Heilpflanzen: was sie
         können und wo es aufhört“. Genau nach der Vorgabe gebaut: die vier
         Zubereitungsarten, die kurze Liste dessen, was in Mitteleuropa wächst,
         und vor allem die Grenzen — nur für die Notlage, langsamere Wirkung,
         eine Dosierung und die Wurmmittel bewusst weggelassen samt Begründung
      **ALLE NEUN THEMEN SIND INZWISCHEN ABGEDECKT — nachgezählt am
      17.08.2026.** Chronisch Kranke: 10 Einträge (Insulin in fünf Stufen,
      Blutdruck, Epilepsie, Schilddrüse). Zahnmedizin: 6. Infektionen: 7
      (Blinddarm samt Verwechslungen, Harnwege, Wundrose, Hautpilz,
      Gelbsucht, Hepatitis). Atemwege und Krankenzimmer: 5. Geburt: 4.
      Medikamentenvorrat: 3. Sichtung und Transport: 3. Der Rest von
      Punkt 8: Läuse, Höhenkrankheit (3), Kopfschmerz, Augenverletzung,
      Ohrenschmerz, Hämorrhoiden. Damit ist dieser Punkt geschlossen.
      Für jedes dieser Themen gilt unverändert Regel 4: eigene, frei nutzbare
      Primärquelle suchen und im Volltext lesen. Das Buch selbst ist NICHT die
      Quelle. Aus dem Rückentext des „SAS Survival Handbook" kommen zusätzlich
      als Themen: Notgepäck/Überlebensausrüstung, Lager einrichten,
      Werkzeugbau, Nahrungssuche, sowie „zu Hause überleben, wenn alle
      Versorgung ausfällt"
- [ ] **Max' drei gekaufte Bücher: Wissen umformuliert nutzen — ENTSCHIEDEN
      von Max am 28.07.2026.** Max besitzt „The Self-Sufficient Life and How
      to Live It" (Seymour), „The Ultimate Survival Medicine Guide" (Alton)
      und das „SAS Survival Handbook" (Wiseman). Seine Weisung: Inhalte in
      eigenen Worten umformulieren und als Erfahrungswissen kennzeichnen.
      Seine Begründung, die trägt: Fakten und Wissen sind nicht
      urheberrechtlich schützbar, nur die konkrete Ausdrucksform — wer liest,
      lernt und eigenständig formuliert, verletzt nichts.
      **Zwei Grenzen, die dabei einzuhalten sind:** (a) Auch *Auswahl und
      Anordnung* eines Werkes sind geschützt. Eine kapitelweise Nacherzählung
      wäre eine Bearbeitung, auch mit anderen Worten — also eigene Gliederung
      behalten und Fakten je Thema aus mehreren Quellen zusammenführen.
      (b) Bei Dosierungen, Grenzwerten und allem, was bei einem Zahlendreher
      tötet, zusätzlich eine Primärquelle heranziehen. Das ist keine
      Lizenzfrage, sondern Regel 1.
      **Praktisch:** Die Bücher liegen nicht digital vor. Nutzbar ist nur, was
      Max fotografiert; die Inhaltsverzeichnisse hat er am 28.07. geschickt
      (Lückenliste oben), die Bücher selbst sind in ein paar Tagen wieder bei
      ihm.
      **Freie Gegenstücke, am 28.07.2026 geprüft — und das Ergebnis ist
      ernüchternd** (Auswertung in `work/recherche-grosswerke/quellenlage.md`):
      *Hesperian* („Where There Is No Doctor"/„…No Dentist") untersagt
      ausdrücklich jede Nutzung „in any digital format" — für eine App also
      unbrauchbar, obwohl es fachlich das ideale Werk wäre. *FAO* und
      *WHO/UNICEF* verwenden durchgängig NC-Klauseln, die mit der gewählten
      CC-BY-SA unvereinbar sind. Frei weitergebbar sind stattdessen einzelne
      **Peace-Corps-Handbücher** mit eigener Freigabeklausel im Impressum:
      Lebensmittel trocknen und lagern (T0020, 1984), Appropriate Community
      Technology (Lehmbau, Sparöfen, Solartechnik; über Appropedia CC BY-SA
      3.0), Wasser/Sanitär-Fallstudien (CS-4, 1984). **Warnung für später:** Die in Prepper-Kreisen beliebte Sammlung CD3WD enthält
      nachweislich auch geschützte Werke (u. a. Seymours „Self-Sufficient
      Gardener") — solche Sammlungen sind KEIN Lizenzbeleg, jede Datei einzeln
      prüfen
- [ ] **Dauerauftrag von Max (28.07.2026): Die Datenbank muss EXTREM umfangreich
      werden.** Begründung wörtlich: nach einer Katastrophe kann die App der
      einzige Wissenszugang sein, „und das für Jahre". Der heutige Bestand ist
      ein Anfang, kein Zustand. Wer nichts Dringenderes hat,
      baut Inhalt aus — unter unveränderter Quellenpflicht (Regel 4): lieber
      stetig und belegt wachsen als schnell und erfunden
- [ ] **Bilder und Skizzen — von Max am 28.07.2026 zweimal angemahnt, zuletzt
      ausdrücklich für die Bauanleitungen: „mir gefällt nicht, dass es
      keinerlei Skizzen gibt, sowas hilft oft enorm".**
      **Teil (a) und (b) sind am 28.07.2026 erledigt, (c) ist angefangen:**
      Die App zeigt Paketbilder jetzt an, über `OpenedPack.readAsset` mit
      Prüfung gegen die Prüfsumme aus dem signierten Durchlauf; was schiefgeht,
      wird benannt (fehlend = Hinweis, beschädigt = Warnung). Im Sparmodus
      bleiben Skizzen aus, der Hinweis darauf bleibt stehen.
      **Vier Skizzen sind gezeichnet** (SVG-Quellen in `design/skizzen/`, PNG im
      Paket unter `assets/`): Lean-to-Dachbalken mit Wind auf die Rückseite,
      Laubhütten-Dämmschicht mit dem Meter-Maß, Dakota-Feuerloch als
      Querschnitt, Flaschenzug 3:1 mit durchnummerierten tragenden Seillinien.
      **Erfahrung aus diesem Durchgang, für die nächsten Skizzen:** Drei der
      vier waren im ersten Anlauf falsch und sind erst nach dem Rendern
      aufgefallen — eine Skizze gilt erst als fertig, wenn sie gerendert
      angesehen wurde. Die Dakota-Fassung zeigte eine durchgehende Grube statt
      zweier verbundener Löcher, die Laubhütte einen zu dünnen Mantel (bei
      einem Meter Laub ist die Schicht dicker als der Innenraum breit, und eine
      dünne Zeichnung unterläuft die Warnung des Schrittes), beim Lean-to
      endeten die Windpfeile in der Luft. Pfeil-Widerhaken werden aus der
      Pfeilrichtung gerechnet, nicht geschätzt.
      **Noch offen:** Knoten und Lashings (`seilwerk-grundknoten`) — bewusst
      ausgelassen, weil eine falsch gezeichnete Knotenführung die gefährlichste
      Skizze im ganzen Paket wäre und mehr Sorgfalt braucht als die übrigen.
      Dazu die Tipp-Skizzen (Seitenlage, Druckverband, Verschlucken); `Tip` hat
      dafür weiterhin kein `image`-Feld.
      Ursprünglicher Auftrag, unverändert gültig: Gilt für Tipps UND
      Bauanleitungen; bei Bauanleitungen ist der Bedarf am größten (Lean-to,
      Knoten, Flaschenzug sind ohne Bild schwer zu verstehen). `GuideStep` hat
      bereits ein `image`-Feld, `Tip` noch nicht.**
      Reiner Fließtext ist im Ernstfall zu langsam zu erfassen; Handgriffe
      (Seitenlage, Druckverband, Verschlucken) brauchen Skizzen. Drei Teile:
      (a) Formatentscheidung — `Tip` hat heute kein Bildfeld, `GuideStep` und
      `Section` haben eins; ein optionales `image` am Tipp ist additiv und
      bricht keine bestehenden Pakete. (b) Die App muss Paketbilder überhaupt
      erst anzeigen, mit Prüfung gegen die Prüfsumme aus dem signierten
      Durchlauf (`OpenedPack.readAsset`, siehe Sicherheits-Abschnitt).
      (c) Die Skizzen selbst: eigene, schlichte Strichzeichnungen (SVG als
      Quelle, PNG ins Paket), jede gegen den Quelltext des zugehörigen Tipps
      geprüft, bevor sie hineinkommt — eine falsche Skizze ist gefährlicher
      als keine
- [ ] Überlebens-Tipps-Datenbank: Inhalt sammeln, kuratieren, Quellen dokumentieren. **Stand 17.08.2026: 384 Tipps, 37 Bauanleitungen, 45 Agrikultur-Kapitel = 466 Einträge** (Erste Hilfe 158, Medizin 109, Nahrung 43, Taktisch 36, Wasser 21, Orientierung 16). Der Dauerauftrag bleibt offen, die Zahl darunter ist der Stand. Alter Stand 28.07.2026: 114 Tipps in `content/europe-de/paket/` (9 Wasser, 89 Erste Hilfe, 15 Nahrung, 1 Hinweis), jede Angabe am Original gegengelesen und von unabhängigen Prüfdurchgängen gegen die Quellen gehalten. Der packbare Teil liegt in `paket/`, die Quellendokumentation daneben — sonst lässt sich das Paket nicht packen
- [ ] **Inhaltsbereich „Jagen, Fischen, Tiere ausnehmen und zubereiten"** (von
      Max am 28.07.2026 beauftragt). **Angefangen am 28.07.2026** mit acht
      Tipps zu Wild und Wildfleisch aus drei BfR-Dokumenten (Stellungnahme
      045/2018 zu Parasiten, 047/2006 zur sensorischen Beurteilung, Information
      01/2006 für Jäger), alle im Volltext gelesen; neue Kategorie `nahrung`.
      Kern ist die Regel 72 °C im Innersten für 2 Minuten und die Korrektur
      dreier verbreiteter Irrtümer (Einfrieren, Räuchern und Pökeln machen
      Wildfleisch nicht sicher; Fuchsbandwurm überträgt sich nicht über das
      Fleisch; Abspülen und Abwischen verteilen den Schmutz, statt ihn zu
      entfernen). Dazu zwei Tipps zur
      Tularämie (Hasenpest) aus dem RKI-Ratgeber in der Fassung von Januar
      2026 — Hasen und Kaninchen stecken schon beim Abbalgen an, nicht erst
      beim Essen. **Fischen ist seit dem 28.07.2026 in den
      Grundlagen abgedeckt** (BVL-Seite zur Fischhygiene, BfR-FAQ zu akuten
      Lebensmittelvergiftungen vom 18.08.2025, Giftinformationszentrum Bonn zum
      Petermännchen): garen mit der Gabelprobe, roh nur nach mehreren Tagen bei
      −20 °C, Frische erkennen, Histamin lässt sich nicht wegkochen, und der
      Petermännchen-Stich. Beim Lesen mit aufgenommen, weil es zur
      Vorratshaltung gehört und tödlich enden kann: Botulismus beim
      Selbsteinkochen (zweimal 100 °C im Abstand von ein bis zwei Tagen,
      aufgetriebene Konserven vernichten).
      **Noch offen in diesem Bereich:** Zerlegen und Zubereiten vollständig,
      Fangmethoden, ein eigener Tipp zu Zeckenstichen (bisher nur am Rand
      erwähnt, obwohl in Mitteleuropa häufiger als alles andere in dieser
      Kategorie), der Fischbandwurm (am 28.07. gesucht, aber nur
      Sekundärquellen gefunden), und eine belegte Faustregel, wie sich die
      72 °C bei Fleisch ohne Thermometer einhalten lassen — für Fisch ist diese
      Lücke mit der Gabelprobe des BVL geschlossen. Einzelheiten in
      `content/europe-de/QUELLEN.md`, Abschnitt „Kategorie: Nahrung"
- [ ] **Drei Inhaltslücken aus dem Stimmigkeits-Durchgang vom 28.07.2026**, je
      mit eigener Quelle zu schließen:
      *Kinder außerhalb des Erstickens* — es gibt keine kindgerechten Angaben bei
      Blutung (nur „deutlich früher gefährlich" ohne Zahl), Unterkühlung, Hitze
      oder Trinkwasserbedarf. Die Wiederbelebung ist seit dem 28.07. abgedeckt.
      **Am 28.07. geprüft und verworfen:** Das Erste-Hilfe-Kapitel der
      ERC-Leitlinie 2025 macht bei diesen drei Themen **keine** Unterscheidung
      nach Alter — kindspezifisch sind dort nur die Adrenalin-Dosierungen bei
      Anaphylaxie und der halbe Teelöffel Haushaltszucker bei Unterzuckerung.
      Braucht also weiterhin eine andere Quelle.
      *Durchfall, Erbrechen, Flüssigkeitsersatz* — **erledigt am 28.07.2026**
      mit fünf Tipps aus dem WHO-Handbuch „The Treatment of Diarrhoea“:
      Austrocknung erkennen, mehr trinken und was, was gerade nicht zu trinken
      ist, Trinklösung selbst ansetzen, wann Hilfe nötig wird.
      *Vergiftung* — **erledigt am 28.07.2026**: Allgemeine Anzeichen,
      Vergiftung über die Atemwege, Kohlendioxid, Kontaktgifte und Hilfe beim
      Erbrechen aus der DRK-Anleitung; **Kohlenmonoxid** (Grill, Gasherd,
      Heizgerät in geschlossenen Räumen, Abgase) aus den DFV-Merkblättern,
      inklusive der Abgrenzung zum Kohlendioxid. **Achtung beim Weiterschreiben:**
      Ortsaussagen über Kohlenmonoxid sind im Paket verboten (Test
      `keinTippBehauptetWoKohlenmonoxidImRaumSteht`) — es durchmischt sich und
      es gibt keine sichere Stelle im Raum
- [x] ~~**Brandrauch**~~ — **erledigt am 28.07.2026, noch am selben Abend.**
      Das Wort war aus „Vergiftung über die Atemwege" entfernt worden, weil es
      dort selbst hinzugefügt war; gemessen war dieser Tipp der einzige Treffer
      für `rauch` — mit der Aufforderung, einen brennenden Raum zu betreten.
      Geschlossen mit vier Tipps aus den „Erste-Hilfe kompakt"-Ausgaben des
      Deutschen Feuerwehrverbands (Empfehlungen des Bundesfeuerwehrarztes):
      nicht ohne Atemschutz hinein, sofort raus (zwei bis vier Minuten),
      Rauchvergiftung erkennen, Rauchvergiftung versorgen
- [x] ~~**Teelöffel-Maß für die Trinklösung prüfen**~~ — **erledigt am
      28.07.2026, und es war ernst.** Das WHO-Handbuch von 2005 setzt 3 g Salz
      mit „one level teaspoonful“ gleich; der Tipp hatte das übernommen und
      hätte ohne Waage auf etwa die doppelte Salzmenge geführt. Die WHO selbst
      nennt in ihren Anleitungen für die Allgemeinheit einen **halben** Teelöffel
      je Liter (Dokument WHO-EM/CSR/594/E und die Cholera-Fragen, Stand 03/2023).
      Berichtigt, mit ausdrücklichem „nicht ein ganzer“ und der Warnung, was zu
      viel Salz anrichtet
- [x] ~~**Zuckermenge für die Trinklösung vereinheitlichen.**~~ — erledigt am
      28.07.2026. Das Laien-Maß stand im Tipp bereits vorn; umgestellt wurde der
      Waagen-Satz am Ende, der die 18 Gramm des WHO-Handbuchs und die sechs
      Teelöffel der Allgemein-Anleitung nebeneinander nannte, ohne zu sagen,
      welche Angabe gilt. Jetzt steht ausdrücklich da, dass die Quellen
      auseinandergehen und das Löffelmaß vorgeht. Die Widersprüchlichkeit zu
      verschweigen wäre schlechter gewesen, als sie zu benennen
- [x] ~~**Zink bei Durchfall**~~ — erledigt am 28.07.2026: eigener Tipp mit Dosis
      (10–20 mg/Tag, 10–14 Tage), auf Kinder begrenzt, mit dem Hinweis, dass es
      das Trinken nicht ersetzt, und vor allem als Vorratsposten
- [x] ~~**Vorbeugung: Händewaschen und Lebensmittelhygiene**~~ — erledigt am
      28.07.2026: drei Tipps (Händewaschen auch ohne Seife – Asche oder Erde;
      Essen sicher zubereiten; Entsorgung von Stuhl mit dem Zehn-Meter-Abstand
      zur Wasserstelle)
- [x] ~~**Durchfallmittel**~~ — nicht geplant gewesen, beim Lesen der Quelle
      gefunden und sofort aufgenommen: „never be given to children below 5 years“.
      In einer Krise greift jemand nach der Packung im Schrank
- [x] ~~**Anhaltender Durchfall, Antibiotika-Reste, Weiteressen**~~ — alle drei
      erledigt am 28.07.2026 mit eigenen Tipps aus dem WHO-Handbuch
- [x] ~~**Behelfsmäßiges Tourniquet.**~~ — **beantwortet am 28.07.2026, und die
      Antwort ist Nein.** Quelle gefunden und im Volltext gelesen: die
      IFRC-Leitlinie „International first aid, resuscitation, and education
      guidelines 2020". Sie erlaubt die behelfsmäßige Abbindung, wenn nichts
      anderes da ist, nennt aber die gemessenen Ausfallraten: Kunststoff-Knebel
      brachen in 70 Prozent der Anlagen, Ledergürtel rissen in 45,8 Prozent, ohne
      Knebel zum Verdrehen war der Druck in **jeder** Simulation zu niedrig
      (1 von 22 gelang); beim Boston-Marathon-Anschlag wurde von 27 angelegten
      behelfsmäßigen Abbindungen keine einzige als wirksam bewertet. Deshalb
      steht im Paket **keine** Bastelanleitung, sondern die belegte Begründung
      dafür, dass Weiterdrücken mit der Hand nicht die zweitbeste Lösung ist,
      sondern das Wirksame. Eingearbeitet in `erste-hilfe-abbinden`
- [x] ~~**Weitere Themen aus dem ERC-Erste-Hilfe-Kapitel**~~ — erledigt am
      28.07.2026: Unterzuckerung, Anaphylaxie, Brustschmerzen und Suizidgedanken
      sind eingebaut. Schlaganfall, Asthma und Opioid-Überdosierung auf Max'
      Weisung ebenfalls — er hat entschieden, dass alles mit medizinischer
      Notwendigkeit hineingehört. Damit ist das
      Erste-Hilfe-Kapitel der ERC-Leitlinie 2025 ausgewertet
- [ ] **Zweiten Zweig „niemand kommt" in alle betroffenen Tipps einarbeiten.**
      ENTSCHIEDEN am 28.07.2026 durch Max und in `RULES.md` (Regel 4)
      aufgenommen: Endet ein Inhalt bei „Notruf", „bis der Rettungsdienst
      übernimmt" oder „zum Arzt", bekommt er einen zweiten Zweig, ausdrücklich
      als Einordnung gekennzeichnet.
      **Stand 28.07.2026 — und die Aufgabe hat sich geändert.** Gefunden und im
      Volltext gelesen ist die IFRC-Leitlinie „International first aid,
      resuscitation, and education guidelines 2020" (476 Seiten). Sie hat einen
      **eigenen Kontext „Remote"** — „medical care could be hours or days away" —
      und zu vielen Themen eigene Absätze „Local adaptations" für Lagen mit
      langen Wartezeiten auf den Rettungsdienst. Damit ist der zweite Zweig für
      viele Tipps **belegbar** statt nur abwägbar. Einzelheiten im
      Quellenabschnitt „IFRC" in `content/europe-de/QUELLEN.md`.
      Fertig: `erste-hilfe-gehirnerschuetterung`, `erste-hilfe-schlaganfall`,
      die beiden CO-Melder-Tipps, die Blutungs-Gruppe
      (`erste-hilfe-starke-blutung`, `erste-hilfe-blutung-kopf-rumpf`,
      `erste-hilfe-druckverband`, `erste-hilfe-abbinden`) und die Wunden-Gruppe
      (`erste-hilfe-wunde-verbote`, `erste-hilfe-wunde-bedecken`,
      `erste-hilfe-wunde-tetanus`, `erste-hilfe-fremdkoerper-in-wunde`) und die
      Kälte-Gruppe (`erste-hilfe-erfrierungen-versorgen`,
      `erste-hilfe-unterkuehlung-stadium-eins`,
      `erste-hilfe-unterkuehlung-stadium-zwei`), dazu `erste-hilfe-amputat` und
      `erste-hilfe-hitzschlag-handeln`.
      **Zweiter Auswertungsdurchgang erledigt am 28.07.2026:** Eingearbeitet
      sind Knochenbruch, offene Brustwunde, Asthma, Anaphylaxie ohne
      Adrenalin, Suizidgedanken (alle belegt) und Petermännchen (als Abwägung
      gekennzeichnet); die Grenzen stehen im Test
      `dieNeuenZweitenZweigeNennenIhreGrenzen`. **Geprüft, aber die IFRC-Quelle
      trägt nichts:** Insektenstich im Mund, Rauchvergiftung, Nasenbluten,
      Brustschmerzen, Kontaktgift, Botulismus, anhaltender Durchfall — diese
      sieben brauchen andere Quellen oder bleiben ohne zweiten Zweig
      (Einzelheiten in QUELLEN.md, Abschnitt IFRC)
- [x] ~~**Schlangenbiss: Ruhigstellung fehlt.**~~ — erledigt am 28.07.2026 auf
      Max' ausdrückliche Weisung, samt Erklärung des Unterschieds. Der Tipp
      verbietet den Druckverband weiterhin und sagt jetzt dazu, warum das nicht
      „nichts darum herum" heißt: Ein Druckverband soll eine Blutung stoppen und
      presst dafür; die ruhigstellende, **nicht dehnbare** Binde soll nur
      Bewegung verhindern, weil Bewegung das Gift schneller verteilt. Die
      Druck-Ruhigstellung mit Polster unter der Binde bleibt Geübten vorbehalten.
      Aus derselben Quelle dazugekommen: Wunde mit sauberem Wasser waschen,
      betroffene Stelle auf Herzhöhe oder tiefer, auf dem gebissenen Bein nicht
      laufen, notfalls tragen. Festgehalten in
      `derSchlangenbissTippTrenntDruckverbandVonRuhigstellung`
- [x] ~~**Aktives Wärmen bei Unterkühlung ab Stadium II.**~~ — **aufgelöst am
      28.07.2026 durch eine dritte Quelle**, nach Max' Weisung, bei
      widersprüchlichen Quellen weiterzusuchen statt den Widerspruch nur zu
      vermerken. Geladen und gelesen: die Praxisleitlinie der Wilderness Medical
      Society zur Unterkühlung außerhalb des Krankenhauses (Fassung 2019). Sie
      nennt den Mechanismus, den die DRK-Anleitung meint, aber nicht ausspricht:
      Gefährlich ist nicht Wärme an sich, sondern Wärme an **Armen und Beinen**
      und jede unnötige Bewegung, weil dadurch kaltes Blut zum Herzen
      zurückfließt. Wärme an **Achseln, Brust und Rücken** ist dagegen
      ausdrücklich verlangt. Damit stehen zwei unabhängige Fachquellen gegen eine
      verkürzte Laien-Anleitung, und die eine erklärt die andere. Umgesetzt: Der
      Tipp verbietet, was belegt schadet, und verlangt, was belegt hilft; dazu
      waagerechte Lage, Isolierung mit wasserdichter Schicht, nasse Kleidung erst
      am geschützten Ort und aufgeschnitten, ab diesem Stadium nichts mehr zu
      essen und zu trinken. Der Titel hieß „nicht mehr aufwärmen" und behauptete
      damit das Gegenteil des eigenen Textes — er ist mitgeändert
- [x] **Herzdruckmassage ohne Rettungsdienst — GEKLÄRT am 17.08.2026.** Max' Einwand vom 28.07.2026:
      Die Regel „ohne Übung nur drücken" geht stillschweigend davon aus, dass
      Rettungskräfte kommen und übernehmen. Trifft das nicht zu, ändert sich die
      Abwägung möglicherweise — ebenso bei „wann darf man aufhören". Im
      ERC-Volltext 2025 nachgeschlagen, mit drei Ergebnissen. ERSTENS: Die Regel
      „ohne Training nur drücken“ gilt ausdrücklich „for all adults in cardiac
      arrest“ — sie hängt NICHT daran, ob jemand übernimmt. ZWEITENS: „Wann darf
      man aufhören“ hatte den zweiten Zweig bereits. DRITTENS, und das war neu:
      Die Leitlinie behandelt den einzelnen Helfer OHNE Telefon oder Netz
      ausdrücklich — rufen und sofort beginnen; nur wenn niemand antwortet,
      unterbrechen und losgehen, dann so schnell wie möglich; und sie gibt zu,
      dass es keine Untersuchung dazu gibt, wie lange vorher wiederbelebt
      werden soll. Steht jetzt in „Notruf 112“, samt der Folgerung für dieses
      Paket: Wer niemanden holen kann, geht nicht los
- [x] ~~**CO-Warnmelder und Verhalten bei CO-Alarm.**~~ — erledigt am
      28.07.2026 mit zwei Tipps, `erste-hilfe-kohlenmonoxid-melder`
      (Kauf nach DIN EN 50291, Montageorte, Alarmschwellen, kein Ersatz für den
      Rauchwarnmelder) und `erste-hilfe-kohlenmonoxid-alarm` (raus mit allen,
      112, nicht zurück ins Haus). **Die benannte Quelle trug das Thema nicht:**
      Die DFV-Fachempfehlung Nr. 04/2012 ist geladen und im Volltext gelesen —
      die alte Adresse läuft ins Leere, das PDF liegt jetzt unter
      `app/uploads/2020/05/DFV-Fachempfehlung_Einsatzstrategien_CO-Notfall.pdf` —,
      sie richtet sich aber an Einsatzkräfte und sagt zu Warnmeldern in Wohnungen
      nichts. Getragen wird der Inhalt stattdessen von vier DFV-Meldungen
      (2017, 2021, 2022, 2026) und den Seiten der Initiative zur Prävention von
      Kohlenmonoxid-Vergiftungen. Einzelheiten, die offengelegte Interessenlage
      der Initiative und alles bewusst Weggelassene in `content/europe-de/QUELLEN.md`,
      Abschnitt „Kohlenmonoxid-Warnmelder"
- [x] ~~**Trinken bei geschlucktem Gift?**~~ — **erledigt am 28.07.2026** nach
      Max' Weisung „so viele Quellen wie möglich": elf Quellen im Volltext
      gesammelt, drei belegte, einander widersprechende Linien gefunden
      (deutsche Giftzentren: kleine Schlucke, keine Milch / DGUV+BVKJ: nichts
      ohne Fachstelle / USA: Wasser ODER Milch bei Ätzendem). Entschieden für
      die Linie der deutschen Giftinformationszentren, DGUV-Gegenposition im
      Tipp offengelegt. Neuer Tipp `erste-hilfe-gift-geschluckt-trinken`;
      Befund, Begründung und gemessene Suche in QUELLEN.md
- [ ] **Kinder-Lücken: Quellen gesichert, Einarbeitung offen** (Stand
      28.07.2026 abends, Auswertung in
      `work/recherche-medizin-luecken/quellenlage.md`): **Hitze bei Kindern — ERLEDIGT am 17.08.2026**
      (DGUV/BZgA, mit Trinkmengen; Eintrag „Die Kleinsten im Sommer“), **Trinkwasserbedarf nach Alter — ERLEDIGT am 17.08.2026**
      (DGE-Referenzwerte, eingearbeitet in „Trinkwasser: Bedarf pro Tag“), **Unterkühlung bei Kindern — ERLEDIGT am 17.08.2026** mit dem
      Eintrag „Unterkühlung beim Kind: das Zittern fehlt“ (BAG-Volltext), Blutverlust bei Kindern (nur Prozent-Klassen und das
      Warnzeichen „Blutdruck fällt spät" — kein Laien-Schwellwert gefunden).
      Vor der Übernahme jedes Zitat im gesicherten Volltext nachschlagen
- [x] **Wiederbelebung nach Rauchgas: Beatmung zuerst? — GEKLÄRT am 17.08.2026.** Das DFV-Blatt
      „Brandgasinhalation" (2015) sagt für den Erstickungs-bedingten Stillstand
      „zunächst zwei Mal beatmen", der Tipp `erste-hilfe-nur-druecken` sagt für
      ungeübte Helfer das Gegenteil — belegt aus den GRC-Leitlinien 2025, der
      aktuellen Primärquelle. Bewusst **nicht** eigenmächtig geändert: eine
      neuere Primärquelle durch eine ältere Sekundärquelle zu ersetzen wäre
      falsch herum. Die Frage ist trotzdem echt, denn für Kinder kennt das Paket
      diese Ausnahme bereits. Im ERC-Volltext 2025 (Basic Life Support) nachgeschlagen: Die Leitlinie sagt
      für ALLE Erwachsenen im Kreislaufstillstand „If you are not trained to
      provide rescue breaths, perform continuous chest compressions“ — **keine
      Ausnahme für Rauchgas.** Beide Fassungen stehen jetzt offen im Tipp,
      zusammen mit der Regel, die ohne Entscheidung auskommt (wer beatmen kann
      und will, beatmet; wer nicht, drückt) und dem Vorbehalt, dass das
      Sonderlagen-Kapitel der Leitlinie nicht vorlag
- [ ] **Was steht in einem Tipp, wenn niemand kommt?** **Nachgemessen am
      17.08.2026: 158 Erste-Hilfe-Einträge, 82 mit Hilfe-Verweis, 18 ohne
      zweiten Zweig** — und die Hälfte davon braucht keinen, weil es
      Erkennungs- oder Zwischenschritte sind, die an einen Behandlungs-Eintrag
      mit Zweig weiterreichen. Zwei echte Lücken sind geschlossen (Grabenfuß,
      schwere Austrocknung). Der ursprüngliche Vermerk sagte: 23 der Erste-Hilfe-Tipps
      enden bei „Notruf", „bis der Rettungsdienst da ist" oder „zum Arzt". Für
      die Wundversorgung ist das in `QUELLEN.md` als Lücke festgehalten — es
      zieht sich aber durch die ganze Kategorie und ist keine Sammlung von
      Einzelstellen, sondern eine Entscheidung auf Paketebene
- [ ] **Bauanleitungen als eigener Bereich** (von Max am 28.07.2026 beauftragt).
      Das Datenmodell dafür steht seit dem Grundgerüst (`Guides.kt`,
      Materialliste optional, Schritte Pflicht) und die Suche deckt es bereits
      ab — Inhalt gibt es noch keinen. Gewünscht sind ausdrücklich:
      Unterstand/Shelter, Seilzug, Töpfern, Wasserfilter, Generator, und
      **Werkzeugbau: Bogen, Messer und Ähnliches**.
      **Begründung und Grenze, mit Max am 28.07.2026 besprochen:** Ein Bogen und
      ein Messer sind zuerst Werkzeuge — Jagd und Wildverarbeitung stehen schon
      als Kategorie im Paket, und das Gerät dazu fehlt. Dass ein solches Werkzeug
      im Notfall auch der Abwehr eines Tieres oder eines Angreifers dient, ist
      Teil des Zwecks und kein Grund, es wegzulassen. **Nicht ins Paket kommt,
      was ausschließlich dazu dient, Menschen zu verletzen** — Sprengsätze,
      Schusswaffen, Fallen gegen Personen, Gifte.
      **Die eigentliche Hürde ist Regel 4, nicht die Entscheidung:** Für jede
      Anleitung braucht es eine geprüfte Quelle, selbst geladen und im Volltext
      gelesen. Erfundene Bauanleitungen sind hier besonders gefährlich, weil
      Werkzeugbau Verletzungen erzeugt. Quellenkandidaten: Freilichtmuseen und
      experimentelle Archäologie (Bogen, Keramik, Kordel), forstwirtschaftliche
      und handwerkliche Fachstellen, Pfadfinder-Handbücher (Knoten, Seilzug,
      Unterstand). Jede Anleitung bekommt einen eigenen Sicherheitsabschnitt
- [ ] **Kategorie „Taktisch": Gefahr vermeiden** — **angefangen am 28.07.2026
      mit acht Tipps**, beide IFRC-Abschnitte im Volltext gelesen: worum es geht
      (Vorbeugung vor allem), die drei Phasen einer Gewaltlage mit ihren
      Prioritäten, Eigenschutz vor Versorgung, Anleiten aus der Ferne, die
      Anzeichen bevorstehender Gewalt, die Deeskalations-Technik, das erlaubte
      Abbrechen und die Ausrüstungsliste. Einzelheiten samt der
      Übersetzungsentscheidung zu „three-sided bandages" und der gemessenen
      Suche in `content/europe-de/QUELLEN.md`, Abschnitt „Kategorie: Taktisch".
      **Noch offen in dieser Kategorie:** Sichtbarkeit vermeiden, Verhalten an
      Kontrollpunkten, Fluchtwege planen, einen Unterschlupf absichern, Schutz
      vor Tieren. Die IFRC-Leitlinie trägt davon nichts — dafür braucht es eine
      zweite Quelle, sonst wird es erfunden.
      (Ursprünglicher Auftrag:) Kern ist nicht der Kampf,
      sondern das Nichtstattfinden: Lagen erkennen, Sichtbarkeit vermeiden,
      Verhalten an Kontrollpunkten, Deeskalation, Fluchtwege, einen Unterschlupf
      absichern, Umgang mit Verletzungen durch Gewalt. Dafür liegt bereits eine
      geprüfte Quelle im Haus: Die IFRC-Leitlinie 2020 hat einen eigenen
      Kontext „Conflict" (S. 38 ff.) und einen Abschnitt „De-escalation
      techniques for violent behaviour" (S. 120). Dazu kommt der Schutz vor
      Tieren, der zum Werkzeugbau gehört
- [ ] **Lexikon als Übersicht, nicht nur als Suchfeld** (von Max am 28.07.2026
      beauftragt). Im Ernstfall weiß niemand, welches Wort er tippen soll. Die
      Startansicht bekommt Kategorie-Kacheln mit den Icons aus
      `design/logo/bereich-icons.svg`, die Suche bleibt darüber. Kategorien:
      Erste Hilfe, Medizin, Wasser, Nahrung, Bauanleitungen, Agrikultur,
      Taktisch. **Kacheln und Themengruppen sind seit dem 28.07.2026 gebaut**
      (siehe unten), offen bleibt nur der Kategorie-Schnitt.
      **Offen:** `medizin` von `erste-hilfe` zu trennen ist ein
      Schnitt durch bestehende Inhalte — Erste Hilfe ist die Minute danach,
      Medizin die Woche danach (Wundinfektion, Vorräte, Dosierungen). Vor dem
      Umhängen die Suche messen, sonst verschieben sich die Notfall-Treffer
- [x] ~~**Titel und Gruppierung im Lexikon**~~ — **erledigt am 28.07.2026.**
      Anlass war Max' Rückmeldung, die Überschriften seien unübersichtlich,
      schlecht sortiert und wirkten „mehr wie der gesamte Inhalt anstatt eine
      Überschrift, nach der man sucht". 91 Titel umgestellt (Stichwort vorn,
      kein Satz; sicherheitskritische Unterscheidungen bleiben ausdrücklich
      stehen), vorher 10 zwingende Schlagwörter ergänzt, 30 Themengruppen als
      Zwischenüberschriften, sortiert nach Dringlichkeit statt alphabetisch.
      Gemessen vorher und nachher an 270 Suchwörtern; kein Wort hat seinen
      Treffer verloren. Die wichtigste beabsichtigte Verschiebung: `blutung`
      führt jetzt zuerst auf „Blutung stillen" statt auf „Blutung: Kopf, Rumpf,
      Bein".
      **Sieben Titel des Entwurfs sind durchgefallen und neu gefasst**, jeweils
      weil sie einen Notfall-Treffer verdrängt haben — Einzelheiten stehen in
      der Commit-Nachricht von `e300e71`. Der Entwurf selbst hatte diese sieben
      nicht vorhergesehen; das ist der Grund, warum die Messung Pflicht ist und
      nicht die Vorhersage.
      **Rest aus dem Entwurf, bewusst nicht übernommen:** Der Vorschlag wollte,
      dass `warnmelder` künftig auf den Alarm-Tipp führt statt auf den
      Vorsorge-Tipp. Das ließ sich nicht haben, ohne dass derselbe Titel bei
      `co`, `raus`, `sofort`, `melder` und `lüften` Notfall-Tipps verdrängt —
      Regel: bestehende Sicherheitstests werden nicht aufgeweicht. Wenn der
      Wunsch bleibt, braucht er einen anderen Weg als den Titel
- [ ] Seilzug, Wasserfilter, Generator — Teil des Bauanleitungs-Bereichs oben
- [ ] Landwirtschafts-/Zivilisations-Aufbau-Guide: Inhalt recherchieren und aufbereiten
- [ ] Offline-Karte: Basis-Kartendaten Europa + POI-Datensatz (Wasserstellen, Aussichtspunkte, Wegpunkte), Lizenzprüfung

### Karte
- [ ] **Wie groß darf die Europa-Karte werden? — Max' Frage vom 29.07.2026,
      mit gemessenen Zahlen beantwortet.** Max' Vorgabe dazu: „die app darf
      auch in die paar hundert megabyte gehen wenn benötigt aber grundsätzlich
      natürlich je kleiner desto besser". Skizzen sind ihm wichtig und beim
      Platz ausdrücklich kein Problem.
      **Gemessen am 29.07.2026** (HTTP-Kopfabfrage bei Geofabrik, keine
      Schätzung): Europa als OSM-Rohdaten **32,3 GB**, Deutschland 4,5 GB,
      Österreich 0,75 GB, Schweiz 0,50 GB.
      **Diese 32 GB sind aber die falsche Zahl**, denn sie enthalten alles:
      jedes Haus, jede Hausnummer, jede Parkbank, jeden Briefkasten. Eine
      Überlebenskarte braucht davon fast nichts. Gebraucht werden sechs Sorten:
      Küstenlinie und Gewässer, Geländeform, Bodenbedeckung (Wald, Offenland,
      Sumpf, Gletscher), Wege von der Autobahn bis zum Pfad, Siedlungen als
      Flächen statt als Einzelgebäude, und Punkte wie Quellen, Brunnen, Hütten,
      Gipfel, Pässe.
      **Damit sind ein paar hundert Megabyte für ganz Europa realistisch.** Die
      zwei Kostentreiber sind (a) das Gelände, das aus einem groben Höhenmodell
      kommt statt aus dicht gezeichneten Höhenlinien, und (b) die Wege, weil
      Pfade in den Alpen dicht liegen. Beides lässt sich über die Zoomtiefe
      steuern: Hausnummern-Zoom braucht niemand, ein Pfad muss sichtbar sein.
      **Vorgehen, bevor irgendetwas gebaut wird:** Erst die Pipeline an EINEM
      Land messen (Schweiz oder Österreich, weil klein und zugleich der härteste
      Fall — dichtes Wegenetz und starkes Relief), den Verkleinerungsfaktor
      bestimmen und dann auf Europa hochrechnen. Eine Zahl aus einer Messung
      ist mehr wert als eine aus einer Überlegung.
      **Aufteilung:** Ein Europa-Überblickspaket, das immer dabei ist, plus
      Detailpakete je Region zum Danebenlegen. So bleibt die App klein und wer
      seine Gegend genau braucht, holt sie sich dazu
- [ ] **Regionen-Wissen mit Kartenbezug — von Max in der Nacht zum 29.07.2026
      beauftragt.** Wörtlich: „eine weitere Datenbank info die sinvoll wäre und
      zur karte verknüpft werden könnte wäre wo man welche nahrung (pilze tiere
      usw) findet sowie andere sicherheits und überlebens wichtige infos zu
      regionen (auch aus taktischer sicht (dinge wie erhöhte und leicht zu
      verteidigende plätze und wo materialien wachsen usw))".
      Das ist eine eigene Wissensart, kein Tipp: Ein Tipp gilt überall, dieses
      Wissen gilt **an einem Ort oder in einem Landschaftstyp**. Drei Sorten
      zeichnen sich ab: (a) Nahrung — welche Pilze, Pflanzen und Tiere wo und
      wann vorkommen; (b) Material — wo Weide, Birke, Lehm, Feuerstein, Schilf
      zu finden sind; (c) Lage — erhöhte, übersichtliche, schwer zugängliche
      Orte, Wasserverfügbarkeit, Wege und Engstellen.
      **Zwei Entwurfsfragen, bevor gebaut wird:** Erstens der Anker — an
      Koordinaten (dann gehört es zu den POIs) oder an einen Landschaftstyp
      („Laubwald, Nordhang, feucht"), der sich auf der Karte einfärben lässt?
      Der Typ-Anker trägt weiter, weil er ohne vollständige Kartierung
      funktioniert und auf jede Region passt. Zweitens die Quellenpflicht: Bei
      Pilzen und Pflanzen ist eine Verwechslung tödlich — hier gilt Regel 4
      besonders streng, und essbare Arten dürfen nur mit eindeutigen
      Unterscheidungsmerkmalen gegenüber ihren Doppelgängern hinein, sonst gar
      nicht.
      **Achtung Lizenz:** Verbreitungskarten von Arten sind meist geschützt.
      Freie Quellen prüfen (GBIF-Daten stehen unter CC-BY/CC0, Artenlisten der
      Landesämter, Floraweb); vor der Übernahme wie immer selbst laden
- [ ] Kompass aus dem Magnetfeldsensor (beschlossen 27.07.2026): eigene Anzeige plus automatisches Ausrichten der Karte in Blickrichtung, abschaltbar, mit Hinweis auf mögliche Fehlweisung durch Metall/Elektronik. Ohne Berechtigung, ohne Ortung

### UI / Design
- [ ] Navigation mit vier Bereichen: Lexikon/Suche (Tipps + Bauanleitungen + Landwirtschaft mit Filtern), Karte, Übersetzer, Einstellungen/Verbindung (beschlossen 27.07.2026); Icons: `design/logo/bereich-icons.svg`
- [x] Wortmarke (`design/logo/wortmarke.svg`) und App-Icon (`design/logo/app-icon.svg`, für Android zusätzlich `app-icon-adaptiv.svg`) stehen fest
- [x] Icon-Dateien für Android erzeugen (**erledigt, geprüft am 17.08.2026: mipmap-mdpi bis -xxxhdpi und mipmap-anydpi-v26 mit ic_launcher.xml liegen vor**) (Launcher-Icon in allen Größen aus `app-icon.svg`, adaptives Icon aus `app-icon-adaptiv.svg` mit hellem Hintergrund; die einfarbige Fassung für Themed Icons liegt als `app-icon-einfarbig.svg` bereit) — gehört in die Baustufe mit `androidApp`
- [x] ~~**Gestaltung: Max' Rückmeldung vom 28.07.2026 — „sieht grafisch noch
      sehr schlecht aus".**~~ — **erster Durchgang erledigt am 28.07.2026.**
      Alle vier benannten Lücken sind zu: eine Kopfzone mit eigener Fläche, die
      bis an beide Blattränder läuft, darunter ein Streifen aus dem Punktraster
      der Wortmarke; gefüllte Kacheln statt bloßer Rahmen (leere Kacheln
      bleiben beim Rahmen, damit „hier ist etwas" und „hier ist noch nichts"
      unterscheidbar sind); das Punktraster als Bildsprache, zusätzlich als
      Trenner vor dem Quellenblock; Serife für Kachelnamen und feste
      Zeichenbreite mit weiten Abständen für Zahlen und Zustandsangaben.
      Sparmodus unangetastet und am Gerät dagegengehalten.
      **Max' Urteil steht noch aus** — wenn es ihm weiterhin nicht gefällt,
      wird zurückgebaut, nicht verteidigt
- [x] ~~**Bewegung, aber sparsam** (von Max am 28.07.2026 gewünscht)~~ —
      **erledigt am 28.07.2026.** Antipp-Rückmeldung über den
      `StateListAnimator` des Systems (kein eigener Berührungshorcher, damit
      die Bedienungshilfen unberührt bleiben) an Kacheln, Bereichsleiste und
      Zurück-Zeile; weiches Aufgehen bei Ansichts- und Bereichswechsel, aber
      nur beim tatsächlichen Wechsel — die Suche baut die Liste bei jedem
      Tastendruck neu. Die Dauer steht an einer Stelle im Stil und ist im
      Sparmodus null. Gemessen an einer Bildschirmaufnahme: Kachel geht beim
      Antippen von 167 auf 164 Bildpunkte zurück, Seitenwechsel laufen über
      drei Bilder bei 20 Bildern je Sekunde.
      **Offen, weil nicht messbar:** Die Gegenprobe im Sparmodus ließ sich
      nicht filmen — `screenrecord` liefert auf dem Emulator bei fast
      unbewegtem schwarzem Bild eine Datei mit einem einzigen dekodierbaren
      Bild. Die Zusage ist stattdessen in `StilTest` festgenagelt. Auf einem
      echten Gerät wäre sie noch anzusehen
- [ ] Normal-Modus UI (Editorial-Design + Nothing-Einflüsse, siehe DESIGN.md #5)
- [ ] Sparmodus UI (eigener Render-Pfad, reines Schwarz, große Schrift, keine Grafik-Extras)
- [ ] Umschaltung zwischen Normal- und Sparmodus

### Übersetzer (am 27.07.2026 aus dem Backlog in V1 vorgezogen)
- [x] ~~**Entscheidung: Woher kommen die Übersetzungen?**~~ — **ENTSCHIEDEN von
      Max am 28.07.2026:** Das „Emergency Multilingual Phrasebook" des British
      Red Cross wird übernommen; Max klärt die Lizenz selbst direkt beim
      British Red Cross. **SPERRE: Vor der Lizenz-Zusage wird nichts
      veröffentlicht, was den Phrasenkatalog enthält; vor jeder
      Veröffentlichung ist das zu prüfen.** Umgesetzt am 28.07.2026:
      62 Sätze in sechs Sprachen (de/en/fr/es/it/pl) in
      `content/europe-de/paket/content/phrases.json`, Übernahme-Entscheidungen
      und Quirks in `content/europe-de/QUELLEN.md`, Abschnitt „Übersetzer".
      **Die Datei gehört wegen der Sperre nicht zu diesem Repo** — das
      Basispaket führt die Art `phrases` deshalb nicht im Manifest.
      Weitere 30 Sprachdateien liegen auf derselben Spiegelung bereit
      (raems.com), wenn mehr Sprachen gewünscht sind.
      Der ursprüngliche Befund (Fachlichkeit und freie Lizenz schließen sich
      aus) bleibt darunter dokumentiert:
      *Fachlich belastbar, aber nicht weitergebbar:* das „Emergency Multilingual
      Phrasebook" des British Red Cross (62 medizinische Sätze in 36 Sprachen,
      mit Mitteln des britischen Gesundheitsministeriums erstellt, von der
      British Association for Emergency Medicine getragen). Genau die Form, die
      wir brauchen — aber es steht unter keiner freien Lizenz und darf nicht in
      ein CC-BY-SA-Paket. Der Abruf scheiterte am 28.07. zusätzlich an den
      Servern (401 bzw. 403), gelesen ist es also nicht.
      *Lizenzrechtlich passend, aber fachlich ungeprüft:* das Refugee Phrasebook
      (CC0, Open Knowledge Foundation) und die Wikivoyage-Sprachführer
      (CC BY-SA). Beides ist Gemeinschaftsarbeit ohne fachliche Abnahme. Bei
      „Ich bin allergisch gegen Penicillin" ist eine schiefe Übersetzung kein
      Schönheitsfehler.
      *Weder noch:* „Basic Emergency Care" von WHO und IKRK steht unter
      CC BY-NC-SA. Das NC verträgt sich nicht mit der für die Inhalte gewählten
      CC-BY-SA und nicht mit GPLv3 für den Code.
      **Vorschlag, falls keine bessere Quelle auftaucht:** Den Katalog in zwei
      Stufen bauen. Stufe eins nur Sätze, bei denen eine schiefe Übersetzung
      nicht tödlich endet (Hilfe, Wasser, Arzt, „ich verstehe nicht", Zahlen,
      Richtungen) — Quelle Wikivoyage, CC BY-SA, im Paket je Satz belegt.
      Medizinische Aussagen bleiben draußen, bis entweder das British Red Cross
      um Erlaubnis gefragt wurde (kostet nichts außer einer E-Mail) oder je
      Sprache jemand gegenliest. Die Oberfläche muss dann sagen, welche Stufe
      ein Satz hat — die Unterscheidung ist der Inhalt, nicht Beiwerk
- [ ] Phrasenkatalog-Inhalte: Kernphrasen festlegen und übersetzen, Quellen/Qualitätssicherung wie bei allen Inhalten
- [x] ~~Neue Inhaltsart `phrases` im Paketformat~~ — erledigt am 28.07.2026:
      Modell, Prüfregeln (Vollständigkeit jeder deklarierten Sprache ist
      Pflichtprüfung), Parser-Anbindung, Abschnitt in `docs/PACK-FORMAT.md`,
      21 neue Tests. Phrasen sind BEWUSST NICHT im Suchindex — Sätze wie
      „Haben Sie Schmerzen?" würden die Notfall-Tipps verdrängen; ob und wie
      sie durchsuchbar werden, wird erst gemessen, dann entschieden
- [ ] Übersetzer-Bereich in der App: fester Phrasenkatalog (mehrsprachig, auch im Sparmodus), in der Oberfläche sichtbar als ausbaubare Funktion gekennzeichnet (spätere Stufen: freie Übersetzung, Kamera-OCR)

### Build & Vertrieb
- [x] ~~**Tote Fracht im APK**~~ — gefunden und ausgeräumt am 28.07.2026, als
      Max nach der voraussichtlichen App-Größe fragte. Gemessen war die
      Release-Fassung 4,23 MB; davon waren **1,2 MB Nachschlagetabellen des
      Post-Quanten-Verfahrens Picnic** aus der Krypto-Bibliothek — mehr als ein
      Viertel der App für etwas, das nirgends benutzt wird. Aus der ganzen
      Bibliothek werden genau drei Klassen gebraucht, alle für Ed25519.
      Ausgeschlossen sind nur diese Tabellen, nicht die Bibliothek. Danach
      **2,88 MB**. Signaturprüfung danach am Gerät gegengeprüft: „Signatur
      geprüft: entwicklung" steht unverändert da.
      **Noch offen:** `isMinifyEnabled` steht auf `false`. R8 würde den
      Programmcode (2,6 MB) voraussichtlich deutlich verkleinern, braucht aber
      sorgfältige Keep-Regeln — eine reflexiv geladene Krypto-Klasse, die
      wegoptimiert wird, fällt erst im Ernstfall auf. Nichts für nebenbei.
      **Größenschätzung für die fertige Standard-Version** (Stand 28.07.2026):
      Text ist billig — 129 Tipps mit allen Quellenangaben kosten 213 KB, also
      auch zweitausend Einträge nur rund 3 MB. Teuer werden Skizzen, rund 30 KB
      je Stück. Ohne Karte landet die App realistisch bei 15 bis 25 MB. Die
      Karte gehört nicht ins APK, sondern in eigene Paketdateien daneben; wie
      grob oder fein die werden, ist noch zu messen und zu entscheiden
- [ ] Gradle-Produktvarianten für Regionen-/Sprach-/Polish-Kombinationen
- [ ] Signierte Release-Pipeline (APK-Signierung, SHA-256-Prüfsummen)
- [ ] Reproduzierbare Builds einrichten und dokumentieren
- [ ] GitHub-Repo vorbereiten (README, Open-Source-Veröffentlichung).
      **Lizenz entschieden am 28.07.2026 durch Max: GPLv3 für den Code, CC BY-SA
      für die Inhaltspakete.** GPLv3 verhindert, dass jemand eine geschlossene
      Bezahlversion daraus macht, ohne die Verbreitung einzuschränken; CC BY-SA
      passt zu Inhalten, die aus öffentlichen Quellen zusammengetragen sind.
      Beim Anlegen: `LICENSE` (GPLv3) und eine eigene Lizenzangabe für
      `content/`, damit die beiden nicht vermischt werden
- [ ] **Vertriebsweg: APK-Direktverteilung, kein Play Store** (entschieden am
      28.07.2026 durch Max). Kostet nichts. Drei Folgen, die daran hängen:
      (a) Die signierte Release-Pipeline mit SHA-256-Prüfsummen ist damit die
      **einzige** Echtheitsprüfung, die Nutzer haben — es gibt keine Store-Instanz,
      die dazwischensteht. Der Punkt oben wird dadurch wichtiger, nicht
      unwichtiger.
      (b) Es gibt **keinen Update-Kanal**. Genau deshalb ist der
      Schlüssel-Widerruf über signierte Erklärungen (Regel 5) kein Luxus: Eine
      neue App-Version erreicht die Zielgruppe nicht.
      (c) Nutzer müssen unter Android „Installation aus unbekannten Quellen"
      erlauben. Das gehört in eine kurze, verständliche Anleitung neben den
      Download — samt Anleitung, wie die SHA-256-Prüfsumme geprüft wird.
      F-Droid bliebe als kostenlose Alternative offen (verlangt reproduzierbare
      Builds, die ohnehin auf der Liste stehen) — von Max nicht gewünscht, hier
      nur der Vollständigkeit halber vermerkt
- [ ] Veröffentlichungs-Export: die Veröffentlichung erfolgt als kuratierter Export ohne interne Arbeitsdateien und ohne die bisherige Git-Historie. Vor dem Export ist zu entscheiden, wie mit `RULES.md` und dem Zielgruppen-Absatz in `docs/DESIGN.md` umgegangen wird — beide benennen Regel 3 ausdrücklich und würden sie beim Veröffentlichen selbst verletzen (Entscheidung liegt bei Max)

- [x] ~~**Signalfarbe in alten Zeichnungen prüfen**~~ — **erledigt am
      04.08.2026.** Sieben Zeichnungen hatten drei oder mehr getrennte Stellen
      in der Signalfarbe; damit zeigt sie nichts mehr. Alle sieben sind
      durchgegangen, jede einzeln und mit dem Tipptext daneben:
      `fallen-trichter` (vier Stellen, davon zwei bloße Maßangaben),
      `wasserloch-ufer` (eine Erklärung und ein Maß), `latrine-arten` (drei
      Handgriffe neben der einen Gefahr, dem Abstand zum Grundwasser),
      `herzdruck-handlage` (vier Stellen; geblieben ist der Handort, weil die
      Zeichnung als einzige DEN beisteuert), `angst-beruhigen` (drei
      Betonungen entfärbt; geblieben ist die Papiertüte, weil sich Panik von
      außen nicht von Asthma, Lungenembolie oder einem Herzproblem
      unterscheiden lässt), `schulter-einrenken` (drei Verfahrensangaben;
      geblieben ist der Fuß MIT Socke oder Schuh, weil nur dort der Verletzte
      Schaden nimmt).
      **`notgeburt` ist bewusst anders behandelt worden.** Dort gibt es
      WIRKLICH mehrere tödliche Punkte — die vier Verbote, ein nicht atmendes
      Neugeborenes, „glitschig". Sie auf einen zu reduzieren wäre falsch
      gewesen. Der tatsächliche Mangel war ein anderer: Die Zeichnung benutzte
      ZWEI verschiedene Rottöne (#a3231a neben #d65a1a) für dieselbe Sache.
      Jetzt tragen alle Gefahrenstellen die Signalfarbe des Bestandes, und
      Gegenstände (das Baby, die Aufzählungspunkte) sind neutral.
      Zählen lässt sich das mit einem Blick auf die y-Werte aller
      `<text ... fill="#d65a1a">` je Datei.

## Backlog / Später (bewusst nicht V1)

- Offline-Übersetzung Stufe 1 (fester Phrasenkatalog): am 27.07.2026 in V1 vorgezogen, siehe oben
- [ ] Offline-Übersetzung Stufe 2: freie Texteingabe via kleines Offline-Sprachmodell
- [ ] Offline-Übersetzung Stufe 3: Kamera-OCR + Übersetzung für Schilder
- [ ] Desktop-Version (Windows/Linux/Mac, gleiche `core`-Basis)
- [ ] iOS-Version
- [ ] Weitere Regionen-/Sprachpakete über Europa hinaus (Welt, Kontinente, einzelne Länder)

## Sicherheits-Entscheidungen — von Max am 28.07.2026 entschieden, noch nicht gebaut

Die Entscheidungen sind gefallen und stehen jetzt verbindlich in `RULES.md`
(Regel 5). **Gebaut ist davon nichts.** Was hier steht, ist die Bauaufgabe.

- [ ] **Schlüssel-Widerruf bauen.** ENTSCHIEDEN: Vorschlag angenommen; Inhalte eines entzogenen Schlüssels bleiben lesbar mit dauerhafter Warnung, sie werden nicht gelöscht. Heute gibt es keinen Weg, einen kompromittierten
      Signaturschlüssel zu entwerten, außer eine neue App-Version zu verteilen —
      die ausgerechnet die Zielgruppe (offline, im Krisenfall) nicht bekommt.
      *Vorschlag:* zwei Schlüsselrollen. Ein **Wurzelschlüssel** wird offline
      aufbewahrt und unterschreibt ausschließlich Erklärungen, nie Inhalte;
      **Inhaltsschlüssel** unterschreiben Pakete. In der App eingebaut ist nur
      der Wurzelschlüssel. Erklärungen sind kleine signierte Dateien einer
      eigenen Art (`.czs`, gleicher Envelope wie `.czp`, anderes Magic) und
      reisen dieselben Wege wie Pakete. Drei Arten: Schlüssel benennen,
      Schlüssel entziehen, Paket zurückrufen. Der Zustand wächst **nur** in eine
      Richtung — es gibt kein „Entzug aufheben", sonst ließe sich mit einer alten
      Erklärung der Zustand zurückdrehen. Bewusst **kein** Anbau ans Paketformat:
      ein zusätzlicher erlaubter Eintrag in `.czp` würde bereits verteilte Pakete
      und die festgenagelte ZIP-Prüfung berühren.
      Die früher offene Wertungsfrage ist entschieden: lesbar lassen mit
      dauerhafter, nicht wegklickbarer Warnung statt löschen — wer im Ernstfall
      kein zweites Nachschlagewerk hat, dem nützt eine App nichts, die sich selbst
      leert.
- [ ] **Rückstufungs-Schutz bauen.** ENTSCHIEDEN: Vorschlag angenommen. Ein altes, gültig signiertes Paket mit inzwischen
      korrigierten Überlebenshinweisen verifiziert unverändert.
      *Vorschlag:* Die App merkt sich je Paket-Kennung die höchste je angenommene
      Version. Diese Marke steigt nur und **überlebt das Löschen des Pakets** —
      sonst genügte Löschen und Neuinstallieren. Eine ältere Version wird
      standardmäßig abgelehnt, mit einem ausdrücklichen Notausgang für den Fall,
      dass eine neue Fassung defekt ist; der Notausgang senkt die Marke nicht.
      Kein Zeitstempel als Anker — offline gibt es keine verlässliche Uhr, und
      `created` im Manifest ist ausdrücklich unverbindlich.
      Dazu gehört eine Lücke im Werkzeug: Wird beim Korrigieren die
      Versionsnummer vergessen, existieren zwei Inhalte unter derselben Kennung
      und Version. Dagegen hilft ein Verzeichnis `(Kennung, Version) →
      Prüfsumme` in `packsign`, das die Unterschrift verweigert, wenn die
      Prüfsumme zu einem bekannten Paar abweicht. Klein, unabhängig von allen
      Entscheidungen oben — aber es führt eine neue Dateiart und eine
      Aufrufoption ein, deshalb nicht auf eigene Faust gebaut.
- [ ] Bild-Integrität beim Anzeigen: Bilder bleiben im Paket und werden erst beim Anzeigen gelesen. Ihr Hash aus dem signierten Durchlauf liegt vor und muss beim Laden geprüft werden (in der UI-Baustufe umzusetzen). Der Weg dorthin ist seit dem 28.07.2026 erreichbar: `PackReader.read` reicht das geöffnete Paket mit durch, `OpenedPack.readAsset` prüft gegen die Prüfsumme

## Offene Befunde aus den Prüfdurchgängen (Stand 28.07.2026)

Behoben sind: stiller Feldverlust im Suchindex, ungeprüfte Bilder beim
Signieren, der weggeworfene `OpenedPack` in `PackReader.read`, Ein-Zeichen-Namen
in ostasiatischen Schriften, sowie im Transfer-Modul das Löschen fertiger Pakete
durch einen zweiten Tastendruck, der ungefangene Schreibfehler und die
unverdrahtete Rahmen-Größengrenze.

Dazu am 28.07.2026 aus dem Vergiftungs-Durchgang: die Seitenlage-Entscheidung
hing in zwei Tipps an „ist Atmung vorhanden" statt an „atmet ausreichend"
(Schnappatmung wäre durchgegangen), zwei Gas-Tipps gaben ohne
Unterscheidungsmerkmal Gegenteiliges an, `rauch` führte in einen brennenden
Raum, `beatmen` zeigte zuerst einen Vergiftungs-Tipp, dem Erbrechen-Tipp fehlte
die Bewusstseins-Voraussetzung, und vier selbst hinzugefügte Begründungssätze
sind ersatzlos gestrichen. Alles behoben und in `ZusammenspielTest.kt`
festgeschrieben; die Einzelheiten stehen in `content/europe-de/QUELLEN.md`.

Offen:

- [x] ~~Wortvorkommen-Grenze neu messen~~ — erledigt 28.07.2026. Gemessen mit
      einem Paket an *allen* Grenzen gleichzeitig: 596 573 Vorkommen und
      3 986 078 Suchzeichen laufen bei 96 MB Heap durch (17 MB lebendes Modell),
      darüber greift die Zeichengrenze zuerst. Die Grenze bleibt bei 300 000,
      weil nur eine Paketform gemessen wurde. Dauertest
      `einPaketAnAllenGrenzenLaedtUndIndiziert`
- [x] ~~`SearchIndex.baue`: zweite vollständige Wortkarte~~ — erledigt
      28.07.2026: Der Rang kommt jetzt aus einer sortierten Reihenfolge der
      Wortnummern statt aus einer zweiten Karte über alle Wörter
- [x] ~~Bytes hinter der Bildkennung~~ — erledigt 28.07.2026: PNG-Abschnittskette
      wird bis zum Endabschnitt nachgerechnet, beim JPEG Anfang und Endezeichen.
      Rest-Einschränkung im Code vermerkt: Ein wohlgeformtes Bild kann in seinen
      erlaubten Zusatzabschnitten weiterhin Daten tragen
- [ ] **Zwei Grenzen der Suche**, am 28.07.2026 an den echten Inhalten gemessen.
      Beide sind Entwurfsfragen, keine Fehler — deshalb nicht eigenmächtig
      geändert:
      *Erstens:* Die Suche vergleicht **Wortanfänge**. „erstickt" findet
      „ersticken" also nicht, „kalt" findet „Kälte" nicht. Deutsch beugt zu stark
      dafür. Als Sofortmaßnahme stehen die gebräuchlichen Formen jetzt in den
      Schlagwörtern der Tipps (abgesichert durch `NotfallSucheTest`) — das ist
      eine Krücke, keine Lösung. Eine echte Lösung wäre Wortstammbildung oder
      eine Teilwortsuche; beides ändert Speicherbedarf und Trefferqualität und
      gehört gemessen, bevor es entschieden wird.
      *Zweitens:* Bei gleicher Punktzahl entscheidet die **alphabetische**
      Reihenfolge des Titels. Wer „ersticken" sucht, bekommt deshalb zuerst die
      Technik-Tipps und nicht den Erkennen-Tipp. Für eine Notfallliste wäre eine
      Triage-Reihenfolge sinnvoller (erst erkennen und entscheiden, dann Technik)
      — das braucht ein Feld im Datenmodell und ist eine Entscheidung
- [ ] Unbekannte JSON-Felder in angemeldeten Inhaltsdateien reisen unbeachtet
      unter der Unterschrift mit (bis zur 4-MiB-Dateigrenze). Beim Signieren
      mindestens melden — die Vorwärtskompatibilität braucht nur die App, nicht
      das Werkzeug
- [x] ~~`ZipAufbau` nimmt die deklarierte Packgröße als Wahrheit~~ — erledigt
      28.07.2026: Jeder Eintrag wird probeweise entpackt, nachgerechnet werden
      entpackte Größe, Prüfzahl und die verbrauchten Eingabebytes
- [x] ~~`packsign sign` prüft andere Bytes, als es signiert~~ — erledigt
      28.07.2026: Der Payload wird einmal gelesen; aus denselben Bytes wird
      geprüft, signiert und geschrieben
- [ ] Transfer: Hinhalten mit 1-Byte-Datenrahmen. Kein Speicherproblem, aber aus
      drei Minuten werden gut dreißig. Braucht einen Mindestdurchsatz oder ein
      Gesamtbudget im Aufrufer — eine Zeitüberschreitung je Rahmen greift nicht
- [x] ~~Transfer: Quelle, die mehr liefert als der Puffer fasst~~ — erledigt
      28.07.2026: gefangen und mit eigener Ursache gemeldet, samt Lesefehlern der
      eigenen Datei
- [ ] Transfer: Nebenläufigkeit festlegen, bevor die Bluetooth-Schicht entsteht.
      `Empfaenger` und `Sender` sind unsynchronisiert; kommen Nutzerentscheidung
      und Rahmenempfang aus verschiedenen Threads, sind Zustand und Zähler
      ungeschützt
- [ ] Transfer: freien Plattenplatz vor der Annahme prüfen (gehört in die
      Plattformschicht; `Ablehnungsgrund.KeinPlatz` wird bisher von keinem
      Produktivcode erzeugt)
- [x] ~~`SearchText.jvm.kt` prüft im Rückfall die Bruchstelle nicht~~ — erledigt
      28.07.2026: Gibt es im Suchfenster keine tragfähige Stelle, wird nicht mehr
      an einer ungeprüften getrennt, sondern das Feld abgelehnt. An einer
      ungeprüften Stelle zu trennen ergäbe eine Aufbereitung, die von der
      Stückelung abhängt — derselbe Text fände sich je nach Länge selbst nicht

## Formatentscheidungen vom 28.07.2026 (entschieden, noch nicht gebaut)

- [x] ~~**`materials` bei Bauanleitungen optional machen.**~~ — erledigt am
      28.07.2026. `materials` hat jetzt einen Vorgabewert, die Prüfung
      `materials-missing` ist entfallen; Grenzen und Feldprüfungen einer
      vorhandenen Liste gelten unverändert, `steps` bleibt Pflicht. **Damit ist
      der Weg offen, aber noch nicht gegangen:** die mehrteiligen
      Erste-Hilfe-Abläufe (stabile Seitenlage, Wiederbelebung, Ersticken) liegen
      weiterhin als aufgeteilte Fließtext-Tipps im Paket. Das Umstellen ist eine
      eigene Aufgabe — und keine reine Formsache, weil jeder Tipp heute für sich
      allein trägt und beim Zusammenlegen die Suchtreffer neu zu messen sind
- [x] ~~**`SourceRef.detail` zur Pflicht machen.**~~ — erledigt am 28.07.2026.
      Feld ohne Vorgabewert, dazu eine eigene Meldung `source-detail-missing`
      für ein leeres `detail`. Vor dem Umstellen geprüft: alle 89 Tipps trugen
      die Angabe bereits, am Bestand war nichts zu ändern. **Offen für Max:**
      Ein Paket ohne `detail` ist damit ungültig, obwohl `schema` weiter auf `1`
      steht. Da noch kein Paket veröffentlicht ist, ist das folgenlos; würde
      später etwas verteilt, wäre eine solche Verschärfung ein Schema-Wechsel
- [x] ~~**Hinweis „ersetzt keinen Erste-Hilfe-Kurs".**~~ — erledigt am
      28.07.2026 als Tipp `erste-hilfe-kein-kursersatz`. Der Titel lautet
      bewusst „Warum Lesen allein den Kurs nicht ersetzt" und nicht
      „…Erste-Hilfe-Kurs": Gemessen hätte die zweite Fassung die Anfragen
      `hilfe` und `arzt` gewonnen und damit eine Handlungsanweisung verdrängt
- [ ] **Haftungshinweis in der Oberfläche zeigen.** Der Hinweistext liegt seit
      dem 28.07.2026 als Tipp `hinweis-angaben-ohne-gewaehr` im Paket (neue
      Kategorie `hinweis`) und als Abschnitt in `README.md`. Über die Suche
      erreichbar zu sein reicht rechtlich aber nicht — er muss **beim ersten
      Start** gezeigt und bestätigt werden und in der Erste-Hilfe-Kategorie
      dauerhaft sichtbar sein. Gehört in die UI-Baustufe.
      **Zwei Punkte für Max:** (a) Der Text ist von Laien geschrieben; vor einer
      Veröffentlichung sollte ein Anwalt draufschauen, insbesondere zu
      Produkthaftung und zu den Grenzen des Haftungsausschlusses nach § 309
      BGB. (b) Sauberer als ein Tipp wäre ein eigenes Pflichtfeld im
      `manifest.json`, das die App immer anzeigt — das wäre aber eine
      Formatänderung und ist deshalb nicht auf eigene Faust gebaut

## Das Wortbudget des Europa-Pakets ist voll (Fund vom 17.08.2026)

Beim Einbauen eines Tipps schlug das Paket mit `content-too-many-search-terms`
fehl und **lud gar nicht mehr**. Der Tipp wurde deshalb sofort wieder
ausgebaut; der Stand im Repository lädt und ist grün. Der fertige, belegte
Eintrag liegt als `work/wartend/tip-nagelbett.json` bereit (`work/` ist nicht
im Repository).

**Gemessen am Stand mit 468 Einträgen** (Sonde mit derselben Rechnung wie
`PackParser.pruefeSuchtextMenge`):

| | belegt | erlaubt | frei |
|---|---|---|---|
| Wortvorkommen | 298 832 | 300 000 | **1 168** |
| Suchzeichen | 1 993 831 | 4 000 000 | 2 006 169 |

Ein durchschnittlicher Eintrag kostet **638 Wortvorkommen**. Es passen also
noch **etwa anderthalb Einträge** hinein, dann steht die Inhaltsarbeit an
diesem Paket. Der abgewiesene Tipp allein wog rund 1 400.

**Der Kern des Fundes:** Bei echtem Fließtext bindet die *Wortgrenze* schon
bei **halb ausgeschöpftem Zeichenbudget**. Die beiden Grenzen wurden gegen
Angriffsformen ausgelegt (viele kurze, verschiedene Wörter); für gewöhnliche
Prosa stehen sie in einem Verhältnis, das nicht zueinander passt. Deshalb kam
die Wand ohne Vorwarnung — es gibt keine Prüfung, die das Näherkommen meldet,
nur die eine, die am Ende hart abweist.

**Was NICHT eigenmächtig geändert wurde:** `MAX_SUCHINDEX_WORTVORKOMMEN` in
`ContentLimits.kt`. Die 300 000 sind eine bewusste Entwurfsentscheidung mit
ausgeschriebener Begründung: Nachgemessen wurden am 28.07.2026 **596 573**
Wortvorkommen, die bei 96 MB Heap sauber durchlaufen — die Grenze liegt also
mit Absicht bei gut der Hälfte des Gemessenen, weil nur *eine* Paketform
gemessen wurde. Das ist eine Formatentscheidung und gehört Max.

**Zur Auswahl (Empfehlung zuerst):**

1. **Grenze auf 450 000 anheben.** Verdoppelt den Platz für Inhalte, ohne
   eine Formatänderung am Paket selbst — die Zahl ist nur eine Prüfschwelle,
   kein Schemamerkmal.

   **Am 17.08.2026 nachgemessen**, damit die Entscheidung auf Zahlen steht.
   Gemessen wurde `SearchIndex.build` im Testlauf mit **96 MB Heap** (die
   Einstellung aus `core/content/build.gradle.kts`, also die Altgerätelage),
   mit einem Paket aus **lauter einmaligen Wörtern** — dem schlimmsten Fall
   fürs Wortverzeichnis. Echte Inhalte wiederholen sich stark und sind
   billiger.

   | Wortvorkommen | Index | Bauzeit |
   |---|---|---|
   | 298 400 (heutige Grenze) | 22 MB | 159 ms |
   | **448 400 (dieser Vorschlag)** | **29 MB** | **123 ms** |
   | 598 400 | 41 MB | 217 ms |
   | 648 400 | 43 MB | 418 ms |
   | 674 400 | 44 MB | 467 ms |
   | 698 400 | **OutOfMemoryError** | — |

   Die Wand liegt also zwischen **674 400 und 698 400**. Damit sitzt die
   heutige Grenze bei 43 % davon, der Vorschlag bei 65 % — immer noch ein
   Drittel Abstand, und das im ungünstigsten Fall.

   **Und dieselbe Messung mit ECHTER Wortmischung**, weil der Index je
   VERSCHIEDENEM Wort kostet und nicht je Vorkommen. Im wirklichen Paket sind
   nur **7,8 %** der Vorkommen verschiedene Wörter (298 832 Vorkommen, 23 322
   verschiedene — im Schnitt 12,8 Vorkommen je Wort). Mit diesem Verhältnis:

   | Wortvorkommen | Index | Bauzeit |
   |---|---|---|
   | 452 400 | **6 MB** | 148 ms |
   | 702 400 | 9 MB | 129 ms |
   | 1 002 400 | 12 MB | 108 ms |
   | 1 402 400 | 18 MB | 158 ms |

   Echter Text ist also rund **fünfmal billiger** als der schlimmste Fall
   (6 statt 29 MB bei 450 000). Selbst 1,4 Millionen Vorkommen kosten weniger
   als das, was heute im schlimmsten Fall erlaubt ist.

   **Daraus folgt etwas für die Wahl der Zahl:** Für echte Prosa bindet gar
   nicht der Speicher, sondern das **Zeichenbudget**. Das Paket braucht 6,67
   Zeichen je Wortvorkommen; die 4 000 000 erlaubten Zeichen entsprechen damit
   rund **600 000 Wortvorkommen**. Die heutige Wortgrenze von 300 000 liegt
   also bei der Hälfte dessen, was das Zeichenbudget ohnehin zulässt — daher
   der Eindruck „halb leer", der heute Nacht zur Wand geführt hat.

   * **450 000** — bequem in jeder Hinsicht (6 MB echt, 29 MB im schlimmsten
     Fall), verdoppelt den Platz, lässt die beiden Budgets aber weiter
     auseinanderlaufen.
   * **600 000** — bringt Wort- und Zeichenbudget für echte Inhalte zur
     Deckung, sodass künftig eine Grenze zählt statt zweier. Im schlimmsten
     Fall 41 MB Index; das läuft, hat aber weniger Luft.

   **Was diese Messungen NICHT sind:** Sie messen den Indexbau auf einem
   Desktop-JVM. Auf dem Gerät hält die App gleichzeitig das geparste Modell,
   Bilder und die Kartenkacheln. Vor dem Umstellen gehört die Gegenprobe auf
   ein echtes altes Gerät — die Zahlen sagen aber, dass weder 450 000 noch
   600 000 knapp kalkuliert wären.
2. **Das Paket teilen** (etwa Erste Hilfe / Medizin / Landwirtschaft als
   eigene `.czp`). Sauberste Lösung für die Zukunft und ohnehin vorgesehen,
   aber die Suche über mehrere Pakete hinweg ist noch nicht gebaut, und alle
   Querverweise zwischen Tipps müssten paketübergreifend funktionieren.
3. **Nichts ändern und den Bestand kürzen.** Wäre die schlechteste Wahl: Es
   würde bedeuten, belegte Inhalte wegen einer Zahl zu streichen, die
   doppelt so hoch sein dürfte.

**Stand am 18.08.2026, damit die Entscheidung nicht abstrakt bleibt:** Es
warten **vier fertige, belegte Einträge** in `work/wartend/` — Nagelbett-
entzündung, Fremdkörper im Ohr, Lichtempfindlichkeit durch Medikamente,
Verstauchung und Zerrung. Alle vier haben die echte Prüfung von
`einbauen.py --pruefen-ohne-schreiben` bestanden. Zusammen brauchen sie
**rund 3 600 Wortvorkommen** (geschätzt über 6,67 Zeichen je Vorkommen, dem
gemessenen Verhältnis dieses Pakets); frei sind **1 168**. Es fehlen also
etwa **2 450** — kein einziger der vier passt allein hinein, der kleinste
bräuchte schon zwei Drittel des Rests.

**Vorher zu erledigen, unabhängig von der Entscheidung:** eine Prüfung, die
schon bei 90 % Auslastung warnt, statt bei 100 % hart abzuweisen — damit die
Wand nie wieder mitten in einem Arbeitspaket auftaucht.

Nebenbei behoben: `docs/PACK-FORMAT.md` nannte für dieselbe Grenze noch
**400 000** und berief sich auf die alte Messung (450 000/452 000). Der Wert
war seit dem 28.07.2026 falsch — die verbindliche Formatreferenz widersprach
dem Code. Jetzt steht dort 300 000 mit der aktuellen Begründung.

## Kartenansicht: das Wichtigste steht unter dem Falz (Fund vom 17.08.2026)

Beim Durchklicken der App gemessen, nicht geschätzt. Auf der Kartenansicht
liegt unterhalb des sichtbaren Bereichs:

* die **Lageanzeige** „Zoom 2.2 · 55.4281°N 7.7831°O" — sie ist im
  Ausgangszustand auf **23 von 68 Bildpunkten Höhe** beschnitten (der
  Textknoten endet exakt an der Unterkante des Rollbereichs bei y=2055),
* die Herkunft der Kartendateien samt Signaturstand und Kachelzahl,
* der Hinweis, dass die App den Standort nicht kennt und nie kennen wird,
* **die Namensnennung: „Kartendaten © OpenStreetMap-Mitwirkende, ODbL 1.0"**
  und der Copernicus-/Airbus-Hinweis zur Geländeform,
* der ganze Abschnitt **Kompass** mit Rose, Gradanzeige und den Hinweisen zu
  Fehlweisung und Störung durch Metall.

**Warum das mehr ist als eine Kleinigkeit:** Die Seite lässt sich zwar
rollen, aber **nur mit einem Wisch NEBEN der Karte**. Wischt man auf der
Karte — die den größten Teil des Bildes einnimmt und zum Wischen einlädt —,
verschiebt man die Karte statt die Seite. Wer das nicht weiß, sieht die
Lageanzeige nie vollständig und den Kompass gar nicht.

Zwei Punkte für Max, beide seine Entscheidung:

1. **Die Namensnennung.** ODbL verlangt sie; sie ist vorhanden, aber im
   Auslieferungszustand unsichtbar. Ob das reicht, gehört auf die Liste vor
   der Veröffentlichung — zusammen mit der Frage zum
   British-Red-Cross-Phrasebook.
2. **Die Aufteilung der Kartenseite.** Dass die Lageanzeige angeschnitten ist
   und der Kompass unter dem Falz liegt, ist eine Gestaltungsfrage
   (Kartenhöhe kürzen? Lageanzeige über die Karte? Kompass auf eine eigene
   Seite?). Deshalb hier notiert und nicht nachts umgebaut.

Nicht betroffen: Im Sparmodus ist die Lageanzeige vollständig sichtbar — die
Aufteilung dort ist gedrängter.

**Nachtrag vom 17.08.2026, und dieser Punkt wiegt schwerer als die
Gestaltungsfrage:** An genau dieser Stelle unter dem Falz steht auch die
Warnung **„Diese Karte ist NICHT GEPRÜFT"**. Am Emulator nachgestellt — eine
mit fremdem Schlüssel signierte `.czk` in den Kartenordner gelegt:

* Die Erkennung arbeitet einwandfrei. Die Warnung nennt die schuldige Datei
  beim Namen und mit Fingerabdruck („zz-fremd.czk … Signierer UNBEKANNT
  (c1731d859d16d661)") und steht groß und orange da; im Quelltext ist sie
  ausdrücklich so gebaut, dass sie sich nicht wegtippen lässt.
* **Sichtbar ist sie in der Ausgangsansicht trotzdem nicht.** Die Seite endet
  dort bei der Lageanzeige; die Warnung beginnt darunter. Wer die Kartenseite
  öffnet und nicht zufällig neben der Karte wischt, sieht sie nie.

Damit ist die Aufteilung nicht nur unschön: Der eine Satz, der einen Menschen
davon abhalten soll, sich auf eine untergeschobene Karte zu verlassen, steht
außerhalb des Bildes. Belege:
`work/belege/2026-08-17/karte-nicht-geprueft.png`.

## Zwei Zeichnungen liegen fertig da, sind aber nicht eingehängt (17.08.2026)

`bilder_pruefen.py` meldete sie als „liegt da, aber nicht in git". Sie sind
jetzt versioniert (`81600fc`), und es sind die **einzigen zwei von 226
Zeichenquellen ohne PNG im Paket** — bei ihnen ist die Kette SVG → PNG →
Verweis nach dem ersten Schritt stehengeblieben.

* `kohlenmonoxid-erkennen.svg` — gehört zu `erste-hilfe-kohlenmonoxid-erkennen`
  („Sicher bist du erst draußen"). Inhaltlich geprüft, deckt sich mit dem Tipp.
* `seitenlage-wann-nicht.svg` — gehört zu `erste-hilfe-stabile-seitenlage`.
  **War inhaltlich falsch und ist berichtigt:** Die Zeichnung führte von
  „Atmung setzt aus" geradewegs auf „sofort Herzdruckmassage beginnen", ohne
  das Zurückdrehen auf den Rücken. Auf der Seite lässt sich nicht drücken.
  Der Tipp selbst war bereits berichtigt; die Zeichnung stammte aus der Zeit
  davor und hätte den Fehler als **Bild** wieder hereingeholt.

Beide Eintrage haben derzeit **kein** Bild. Ob sie eines bekommen sollen, ist
eine Gestaltungsentscheidung und gehört Max — deshalb nicht nachts eingehängt.
Zum Fertigmachen fehlen nur zwei Schritte: rastern (Chrome headless) nach
`content/europe-de/paket/assets/`, dann das Feld `image`
im jeweiligen Tipp setzen und `bilder_pruefen.py` nochmal laufen lassen.
**Das Wortbudget steht dem nicht im Weg** — `image` zählt nicht in den
Suchindex.

## Reproduzierbare Builds: gemessen, und die Lage ist gut (17.08.2026)

Der Punkt „Reproduzierbare Builds einrichten und dokumentieren" stand offen,
ohne dass je nachgesehen wurde, wo man steht. Jetzt nachgemessen: **zweimal
aus gelöschtem `androidApp/build` gebaut**, der zweite Lauf zusätzlich mit
`--rerun-tasks --no-build-cache`.

**Ergebnis: Der Inhalt ist bereits Byte für Byte reproduzierbar.**

| | Lauf 1 | Lauf 2 |
|---|---|---|
| Größe | 21 361 703 | 21 361 703 |
| Einträge im Archiv | 55 | 55 |
| Einträge mit abweichender Prüfsumme | \(0\) | |
| Einträge mit abweichendem Zeitstempel | \(0\) | |
| abweichende Bytes gesamt | **610** | |

Die 610 Bytes liegen alle zwischen Position 21 356 435 und 21 357 045 — also
**im APK-Signaturblock** (dessen Kennung „APK Sig Block 42" steht in beiden
Läufen an derselben Stelle 21 357 935).

**Warum das kein Mangel ist:** Die v1-Signatur ist byteweise identisch —
`META-INF/CERT.RSA`, `CERT.SF` und `MANIFEST.MF` haben in beiden Läufen
dieselbe Prüfsumme. Abweichend ist nur der v2-Block, der außerhalb der
Archiveinträge liegt. Der Schlüssel ist RSA-4096; die v2-Signatur benutzt
eine Zufallskomponente in der Auffüllung, deshalb unterscheiden sich zwei
Signaturen über **denselben** Inhalt. Das ist erwartetes Verhalten und genau
der Fall, den eine Prüfung durch Dritte abdeckt: Verglichen wird der
signierte Inhalt, nicht die Signaturbytes.

**Was damit noch zu tun bleibt** (der Punkt ist also kleiner als gedacht):

1. ~~Den Vergleich als Anleitung festschreiben~~ — erledigt am 17.08.2026 als
   **`tools/app/apk_vergleichen.py`**. Der Griff nimmt einem genau das ab,
   woran ein Handvergleich scheitert: Er meldet Inhaltsunterschiede als
   Fehler und Unterschiede im Signaturblock als das, was sie sind. Aufruf und
   die zwei Baubefehle stehen im Kopf der Datei.
   Rückgabewert 0 heißt „der Inhalt reproduziert".
   In beide Richtungen geprüft: gegen die zwei echten Läufe meldet er 0, gegen
   eine wirklich andere APK 1 samt der abweichenden Einträge.
2. Gegenprobe auf einem **zweiten Rechner**; hier lief beides auf derselben
   Maschine mit demselben JDK.
3. Die SHA-256-Prüfsummen je Veröffentlichung ablegen.
4. **Den Weg vom Klon zur ausgelieferten APK dokumentieren** — siehe unten.

### Aus einem frischen Klon: was fehlt (gemessen am 17.08.2026)

Das Repo wurde in ein leeres Verzeichnis geklont und dort gebaut. Zwei
Ergebnisse:

**Erstens ein Fehler, und der ist behoben** (`731a4ab`): Der Bau brach an
allen drei Übernahme-Aufgaben ab, weil `work/` nicht im Repo liegt. Der
Kommentar im Bauskript verspricht ausdrücklich das Gegenteil — ein fehlendes
Paket dürfe den Bau nicht abbrechen. Ursache war Gradle 8: Eine benannte,
aber fehlende Eingabedatei ist dort ein Fehler, und `optional(true)` fängt
nur den Fall ab, dass gar keine benannt wurde. Die Absicherung war still
wirkungslos geworden. Jetzt läuft der Klon-Bau durch.

**Zweitens die eigentliche Lücke:** Was dabei herauskommt, ist die App
**ohne Daten** — 3,5 MB statt 21,4 MB. Denn diese drei Dateien sind
bewusst nicht im Repo (`.gitignore`), und ohne sie fehlen Inhalt, Karte und
Gelände:

| fehlt im Klon | kommt aus |
|---|---|
| `work/build/europe-de.czp` | `packsign pack` + `sign` über `content/europe-de/paket` |
| `work/karte/oesterreich-ueberblick.czk` | Kartenpipeline unter `tools/karte` |
| `work/karte/oesterreich-hoehen.czh` | Höhenpipeline unter `tools/karte` |

Dazu kommt der **Signaturschlüssel**: `work/devkey/entwicklung.secret` ist
ebenfalls nicht im Repo — zu Recht, aber damit kann ein Dritter das Paket
nicht identisch signieren. Für die Prüfung ist das kein Hindernis (verglichen
wird der signierte Inhalt), es gehört nur beschrieben.

**Was daraus folgt:** „Reproduzierbar" heißt für dieses Projekt nicht „aus dem
Klon fällt dieselbe APK", sondern: Aus dem Klon plus den dokumentierten
Pipelines muss derselbe **Inhalt** entstehen. Genau das gehört aufgeschrieben —
und es ist die Voraussetzung dafür, dass F-Droid überhaupt in Frage kommt.

## Offene Fragen für später

- ~~Konkretes Dateiformat/Bibliothek für Kartenrendering~~ — **entschieden von
  Max am 04.08.2026: eigenes Format, eigener Renderer auf Android-Canvas.**
  Einzelheiten in [`docs/KARTEN-FORMAT.md`](docs/KARTEN-FORMAT.md).
  MapLibre ist mit Beleg ausgeschieden: seine Bibliothek deklariert `INTERNET`,
  `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` und beide Ortungsrechte, woran
  der Berechtigungswächter in `androidApp/build.gradle.kts` den Bau abbricht.
  Mapsforge und VTM kämen ohne Berechtigung aus, bauen aber gegen minSdk 23 und
  brächten ein zweites, unsigniertes Dateiformat neben `.czp` mit.
  **Noch offen daran:** Eine `.czk` trägt bisher keinen eigenen signierten
  Umschlag. Solange die Karte als Beigabe im APK liegt, deckt die
  APK-Unterschrift sie ab; für große Karten daneben (Europa, rund 300 MB)
  braucht `.czk` denselben Umschlag wie `.czp`. Bis dahin wird eine selbst
  hinzugefügte Karte in der Oberfläche dauerhaft als NICHT GEPRÜFT
  ausgewiesen (Regel 5)
- **Braucht die Übersichtskarte Landesgrenzen?** Beim Ansehen der fertigen
  Karte am 04.08.2026 aufgefallen: Österreich hat keine Küste, und die
  Bodenbedeckung beginnt erst bei Zoomstufe 7 — auf der Übersicht steht das
  Land deshalb ohne Umriss da, nur mit seinen Flüssen. Für Europa fällt das
  weniger auf, weil die Küstenlinie die Form trägt. Grenzen wären eine SIEBTE
  Datensorte (`boundary=administrative`, `admin_level=2`) und damit eine
  Erweiterung der in dieser Roadmap festgelegten sechs — deshalb nicht auf
  eigene Faust gebaut. Entscheidung liegt bei Max
- **Geländeform ist noch nicht gemessen.** Sie ist die sechste Datensorte,
  kommt aber aus einem Höhenmodell und nicht aus OpenStreetMap. Weder ihr
  Platzbedarf noch die Lizenz der in Frage kommenden Höhendaten sind geprüft
- Welche konkreten öffentlichen Quellen für Erste-Hilfe-/Bautechnik-/Landwirtschafts-Fakten
- ~~Lizenzwahl~~ — **entschieden am 28.07.2026:** GPLv3 für den Code, CC BY-SA
  für die Inhaltspakete. Einzelheiten und die Folgen des Vertriebswegs im
  Abschnitt Build & Vertrieb. Die Lizenz hat keinen Einfluss auf die Kosten;
  der gewählte Weg (APK-Direktverteilung) kostet nichts
- ~~Konkrete Auswahl/Format der vertrauenswürdigen Signaturschlüssel-Verwaltung~~ — entschieden: Ed25519 (32 Byte roher Schlüssel), Vertrauensspeicher mit vollem Byte-Vergleich, Schlüsselverwaltung über `tools/packsign`; Einzelheiten in [`docs/PACK-FORMAT.md`](docs/PACK-FORMAT.md)
