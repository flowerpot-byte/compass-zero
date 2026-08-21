# Quellen — Basispaket Deutsch

Dokumentation der Faktenquellen zu `paket/content/tips.json`, gemäß Regel 4
in [`../../RULES.md`](../../RULES.md).

**Zum Aufbau dieses Ordners:** Der packbare Teil liegt in `paket/`; diese
Datei liegt bewusst daneben. Das Paketformat erlaubt im Container nur
`manifest.json`, `content/…` und `assets/…` — läge die Quellendokumentation
mit drin, ließe sich das Paket gar nicht erst packen. Jeder Tipp verweist zusätzlich in seinem
eigenen `sources`-Feld auf den Quellennamen; hier stehen die vollständigen
Belege inklusive URL und Abrufdatum.

## Kategorie: Wasser

Recherchiert am 28.07.2026.

### Bundesamt für Bevölkerungsschutz und Katastrophenhilfe (BBK)

- Ratgeber "Vorsorgen für Krisen und Katastrophen" (Stand 11/2025), PDF:
  https://www.bbk.bund.de/SharedDocs/Downloads/DE/Mediathek/Publikationen/Buergerinformationen/Ratgeber/BBK-Vorsorgen-fuer-Krisen-und-Katastrophen.pdf
- Webseite "Essen und Trinken bevorraten":
  https://www.bbk.bund.de/DE/Warnung-Vorsorge/Vorsorge/So-koennen-Sie-sich-vorbereiten/Bevorraten/bevorraten_node.html

Verwendet für: `wasser-tagesbedarf`, `wasser-vorratsdauer`.

**Korrigiert am 28.07.2026:** Der Tipp `wasser-tagesbedarf` sagte „mehrere Wochen
ohne Essen". Der BBK nennt **drei Wochen** — das ist der konservativere und
belegte Wert, und Regel 4 verlangt im Zweifel den konservativeren.

### Weltgesundheitsorganisation (WHO)

- Technical Note 5, "Emergency treatment of drinking-water at the point of use":
  https://cdn.who.int/media/docs/default-source/wash-documents/who-tn-05-emergency-treatment-of-drinking-water-at-the-point-of-use.pdf

Verwendet für: `wasser-chlor-entkeimung`, `wasser-truebes-wasser-vorbehandeln`,
`wasser-lagerung`, `wasser-solare-entkeimung-sodis`,
`wasser-geschmack-belueften`, mitbelegend für `wasser-abkochen`.

**Entfernt am 28.07.2026:** Der zusätzlich zitierte Technical Brief
WHO/FWC/WSH/15.02 („Boil water") war unter beiden offiziellen IRIS-Adressen
nicht abrufbar (HTTP 403). Die beiden Aussagen, die nur er getragen hätte —
die Ein-Minuten-Regel und „funktioniert auch bei trübem Wasser" — sind deshalb
neu belegt worden, siehe unten.

### US-Umweltbehörde (EPA)

- "Emergency Disinfection of Drinking Water":
  https://www.epa.gov/ground-water-and-drinking-water/emergency-disinfection-drinking-water
  (abgerufen am 28.07.2026)

Verwendet für: `wasser-abkochen`, `wasser-abkochen-hoehenlage`, mitbelegend für
`wasser-chlor-entkeimung`.

**Art der Prüfung:** Die Seite wurde im Browser geöffnet und selbst gelesen.
Dabei hat sich eine **sachliche Korrektur** ergeben: Der Tipp `wasser-abkochen`
behauptete, Abkochen funktioniere „auch bei trübem Wasser". Die EPA schreibt das
Gegenteil einer Sorglosigkeit vor — trübes Wasser soll **zuerst absetzen und
durch ein sauberes Tuch, Papiertuch oder einen Kaffeefilter gefiltert werden**,
und erst danach wird gekocht. Der Satz ist ersetzt. Die Ein-Minuten-Regel steht
dort ebenfalls wörtlich und ist jetzt korrekt der EPA zugeschrieben statt der
WHO; die EPA selbst führt die grundsätzliche Wirksamkeit des Abkochens auf die
WHO zurück.

Dieselbe Filter-Vorstufe fehlte auch bei `wasser-chlor-entkeimung` und ist dort
ergänzt, zusammen mit dem Händewaschen und dem Hinweis, dass die doppelte Menge
auch bei sehr kaltem Wasser gilt.

## Kategorie: Erste Hilfe

Recherchiert und gegengeprüft am 28.07.2026.

### Deutscher Rat für Wiederbelebung (GRC) / European Resuscitation Council (ERC)

Beide Poster in der Fassung **Guidelines 2025**:

- „Lebensrettende Basismaßnahmen (BLS) bei Erwachsenen – Schritt für Schritt"
  (Referenz im Dokument: `Poster_BLS StepbyStep_Algorithmus_GER_2025 über GRC`):
  https://www.grc-org.de/files/Contentpages/document/7_BLSStepbyStep.pdf
- „Lebensrettende Basismaßnahmen (BLS)", Algorithmus
  (Referenz im Dokument: `Poster_BLS_Algorithmus_GER_2025 über GRC`):
  https://www.grc-org.de/files/Contentpages/document/9_BLS.pdf

Verwendet für die Tipps der Wiederbelebungs-Kette (`erste-hilfe-eigenschutz` bis `erste-hilfe-wann-aufhoeren`) sowie mitbelegend für
`erste-hilfe-entscheidung-nach-atemkontrolle` und `erste-hilfe-stabile-seitenlage`.

**Art der Prüfung:** Beide Dokumente wurden heruntergeladen und Wort für Wort
am Original gegengelesen — das Schritt-für-Schritt-Poster als gerenderte
Seite, weil es reine Bilddaten enthält und sich nicht als Text auslesen lässt.
Jede Zahl in den Tipps (10 Sekunden Atemkontrolle, 5–6 cm Drucktiefe,
100–120/min, Verhältnis 30:2, 1 Sekunde je Beatmung, höchstens 10 Sekunden
Unterbrechung, AED nur binnen einer Minute) stammt unmittelbar aus diesen
beiden Postern. Die Kernzahlen sind zusätzlich durch den Test
`EuropeDePaketTest.kernzahlenDerWiederbelebungStehenUnveraendertImPaket`
gegen stille Veränderung gesichert.

**Leitlinienfassung:** Maßgeblich ist die Fassung **2025**, nicht die vielfach
zitierte von 2021. Die Kernzahlen sind unverändert; neu deutlich
herausgestellt ist die Regel „ohne BLS-Training nur Thoraxkompressionen".

### ERC-Leitlinie Erste Hilfe 2025 (über „Reanimation 2025 – Leitlinien kompakt“)

- „Reanimation 2025 – Leitlinien kompakt“, Kapitel **Erste Hilfe**, Abschnitte
  Trauma-Notfälle und Umweltnotfälle (Seiten 228–244):
  https://www.grc-org.de/files/Contentpages/document/260429_TB_Reanimation.pdf
  (abgerufen am 28.07.2026; PDF geladen, 272 Seiten, Text lokal herausgezogen
  und gelesen)

Verwendet für: `erste-hilfe-abbinden`, `erste-hilfe-offene-brustwunde`,
`erste-hilfe-gehirnerschuetterung`, `erste-hilfe-amputat`,
`erste-hilfe-ertrinken` sowie mitbelegend für `erste-hilfe-starke-blutung` und
`erste-hilfe-wunde-bedecken`.

**Vierter Teil aus demselben Kapitel, 28.07.2026 — und drei bewusste
Nicht-Übernahmen.** Aufgenommen wurden:

- **Unterzuckerung** (S. 234). Der einzige Notfall im Paket, der sich mit
  Haushaltsmitteln vollständig beheben lässt: 15–20 g Traubenzucker, sonst eine
  Handvoll Bonbons oder 50–100 ml Fruchtsaft, bei Kindern ein halber Teelöffel
  Haushaltszucker unter die Zunge. Wiederholung nach 15 Minuten, leichte Mahlzeit
  nach 5–10 Minuten. Und die Gegenanzeige, die den Tipp erst sicher macht: „Bei
  Personen, die nicht reagieren, darf aufgrund der Aspirationsgefahr kein Zucker
  oral gegeben werden." Der Risikofaktor der Quelle ist die Dauerlage dieser App:
  „Personen mit Diabetes **oder chronischer Unterernährung**".
- **Anaphylaxie** (S. 233) mit vollständiger Zeichenliste und den
  Adrenalin-Dosierungen nach Alter. Das Paket hatte mit `insektenstich-mund`
  schon die halbe Tür; beide verweisen jetzt aufeinander.
- **Brustschmerzen** (S. 234): Aspirin-Kautablette 150–500 mg, außer bei bekannter
  Aspirin-Allergie; Nitro-Spray bei bekannter Angina pectoris; bei der Person
  bleiben.
- **Suizidgedanken** (S. 237). Braucht keine Ausrüstung, nur Worte — und in einer
  langen Krise mit Isolation ist das kein Randthema.

**Zunächst nicht übernommen — auf ausdrückliche Weisung am 28.07.2026 doch
aufgenommen.** Die Begründungen für das Weglassen stehen unten; entschieden
wurde, dass alles mit medizinischer Notwendigkeit ins Paket gehört, und dabei
bleibt es. Die drei Tipps tragen deshalb ausdrücklich, wo die
Leitlinie dünn ist: Der Schlaganfall-Tipp sagt selbst, dass die Quelle zur
Überbrückung ohne Rettungsdienst nichts hergibt; der Asthma-Tipp sagt, dass die
Leitlinie nicht mehr enthält als den einen Satz; der Opioid-Tipp sagt, dass
Naloxon kaum jemand im Haus hat und die Wiederbelebung das Entscheidende bleibt.

Die ursprünglichen Bedenken, zur Nachvollziehbarkeit:

- **Schlaganfall** (S. 237). Die Leitlinie empfiehlt im Kern ein
  Beurteilungsblatt, „um die Zeit bis zum Erkennen und Hilferuf zu verkürzen",
  und Sauerstoff nur für Geschulte. Der ganze Wert liegt im schnellen Transport
  in die Klinik. Ohne erreichbares Krankenhaus ändert das Erkennen an der
  Handlung nichts. Für einen brauchbaren Tipp bräuchte es eine andere Quelle für
  das, was man beim Warten tut.
- **Asthma** (S. 233). Der ganze Abschnitt ist ein Satz: „Helfen Sie
  Asthmatikern, die Probleme mit der Verwendung ihres Bronchodilatators haben."
  Zu dünn für einen eigenen Tipp.
- **Opioid-Überdosierung** (S. 235). Die eigentliche Maßnahme ist Naloxon —
  ein Medikament, das praktisch niemand vorrätig hat, plus Schulungsanforderung.
  Ohne Naloxon schrumpft der Abschnitt auf „CPR beginnen und 112 rufen", und das
  steht bereits vollständig im Paket. Der Tipp brächte Umfang ohne eine einzige
  neue Handlung — und jeder zusätzliche Tipp verdünnt die Trefferlisten.

Beim Messen kam noch ein Altbefund heraus: Die Anfrage `schock` führte zum
Defibrillator-Tipp, weil dort der Stromstoß gemeint ist. Im Alltagsdeutsch meint
„Schock" den Kreislaufschock. Das Schlagwort ist dort entfernt — wer den
Defibrillator sucht, tippt `defibrillator` oder `aed`.

**Nachtrag vom 28.07.2026 — der schwerste Befund war ein Altfehler, den erst die
neuen Tipps erreichbar gemacht haben.** Die Leitlinie schreibt auf S. 231:

> „In Situationen wie agonale Atmung oder **Trauma** dürfen Sie die Person NICHT
> in Seitenlage bringen.“

Das Paket deckte die Schnappatmungs-Hälfte dieses Satzes vorbildlich ab — und
die Trauma-Hälfte mit keinem Wort. Der neue Gehirnerschütterungs-Tipp ist genau
die Tür dorthin: Sturz auf den Kopf, Person schläfrig, atmet → Paket sagte
Seitenlage, Leitlinie sagt NICHT. Jetzt gibt es einen eigenen Tipp zur
Wirbelsäule (aus dem Abschnitt „Bewegungseinschränkung der Halswirbelsäule“,
S. 237/238), und die drei Tipps, die über die Seitenlage entscheiden, nennen die
Ausnahme.

Der Test dazu hat beim Bauen sofort **zwei weitere** Tipps gefunden, die in die
Seitenlage schicken, ohne auf die Handgriffe zu verweisen — Unterkühlung
Stadium II und Hitzschlag. Beide verweisen jetzt.

**Weitere Befunde desselben Durchgangs, alle behoben:**

- **„oberhalb der Wunde“ beim Abbinden stand in keiner Quelle** — das Wort
  „oberhalb“ kommt im ganzen Dokument genau einmal vor, in anderem Zusammenhang.
  Und es war gefährlich mehrdeutig, weil das Paket selbst an anderer Stelle sagt,
  Arm oder Bein sollten hochgehalten werden: Bei hochgehaltenem Arm zeigt
  „oberhalb“ zur Hand, also auf die wirkungslose Seite. Jetzt „zwischen Wunde und
  Rumpf, also zum Körper hin“.
- **Die Leitlinie verbietet das Abbinden beim Schlangenbiss ausdrücklich** (S. 244:
  „Legen Sie keinen Druckverband an, verwenden Sie kein Eis oder Wärme und legen
  Sie keine Tourniquets an. Schneiden Sie die Wunde nicht auf und versuchen Sie
  niemals, das Gift auszusaugen.“). Das Paket hatte zum Schlangenbiss gar nichts —
  und `abbinden` war der erste Treffer für eine Lage, in der es schadet. Jetzt ein
  eigener Tipp und die Gegenwarnung im Abbinde-Tipp.
- **Der Abbinde-Tipp las sich als Vierstufenleiter.** Die Quelle hat zwei Zweige:
  Steht die Blutung unter dem Handdruck, Druckverband; steht sie nicht, sofort
  abbinden. Wer erst einen Druckverband probiert, verliert genau die Minuten, die
  „so schnell wie möglich“ retten soll. Die Verzweigung steht jetzt sichtbar da.
- **Ohne Tourniquet stand der Leser vor einer Sackgasse.** Die Quelle sagt zum
  Improvisieren nichts; der belegbare konservative Rat ist „weiter drücken“. Ohne
  diesen Satz greift jemand zum Schnürsenkel, und eine schmale Schnur richtet
  mehr Schaden an, als sie Blutung stillt.
- **Der Rumpf-Tipp deckt den Brustkorb mit ab.** „An Kopf und Rumpf … kein
  keimarmes Material auf die Blutungsstelle pressen“ hätte zum Zudrücken einer
  offenen Brustwunde geführt — also zu genau dem, was S. 238 verbietet. Ausnahme
  ergänzt.
- **„Ohne Übung nur drücken“ nannte eine Ausnahme, nicht zwei.** Beim Ertrinken
  kommen fünf Atemspenden zuerst, auch beim Erwachsenen. Eine abzählbare
  Ausnahmenliste erklärt alles Übrige stillschweigend für abgedeckt — dieselbe
  Falle wie bei den Gas-Tipps.
- **Der Gehirnerschütterungs-Tipp führte „reagiert nicht“ als Anzeichen** und
  hatte als einzige Handlung „Aktivität beenden, zum Arzt“. Wer eine nicht
  reagierende Person vor sich hat, hätte den Ruhe-und-Arzt-Weg genommen statt
  Atemkontrolle und Wiederbelebung. Jetzt steht die Grenze ausdrücklich da, und
  „reagiert nicht“ heißt „ist schwer erweckbar“.
- Zum Amputat: „Halten Sie das Amputat stets gekühlt“ war weggefallen, und „tut
  dieselben Dienste“ war stärker als das „können Sie … verwenden“ der Quelle.
- Suchlücken: `verblutet`, `kopfverletzung`, `gestürzt`, `messer`, `aderpresse`
  fanden **gar nichts**; `stich` führte zur Brustwunde statt zum Insektenstich im
  Rachen; `arm ab` und `bein ab` fanden den Amputat-Tipp nicht.

**Eine Grundsatzfrage, die dieser Durchgang aufgeworfen hat und die noch zu
entscheiden ist:** Quer durch das Erste-Hilfe-Kapitel enden Anweisungen mit
„Notruf 112“, „bis der Rettungsdienst übernimmt“, „zum Arzt“, „ins Krankenhaus“.
Das ist quellentreu und muss so bleiben. Aber es ist die Standardannahme einer
Leitlinie für ein Land mit funktionierendem Rettungsdienst — und die
Grundannahme dieser App ist die gegenteilige. Soll **jeder** Tipp, dessen
Handlung auf externer Hilfe endet, einen zweiten Zweig für „niemand kommt“
bekommen? Beim Gehirnerschütterungs-Tipp ist so ein Zweig testweise ergänzt und
als eigene Einordnung gekennzeichnet. Wenn das die Regel werden soll, gehört sie
in `RULES.md`, nicht in Einzelkorrekturen.

**Wie dieses Kapitel gefunden wurde — und was es beantwortet.** Gesucht war
eigentlich etwas anderes: kindgerechte Angaben zu Blutung, Unterkühlung, Hitze
und Trinkwasserbedarf. **Die gibt es dort nicht** — das Erste-Hilfe-Kapitel
macht bei diesen drei Themen keine Unterscheidung nach Alter; kindspezifisch sind
nur die Adrenalin-Dosierungen bei Anaphylaxie und der halbe Teelöffel
Haushaltszucker bei Unterzuckerung. Die Kinder-Lücke bleibt also offen und
braucht eine andere Quelle.

Gefunden wurde stattdessen ein ganzes Kapitel, das dem Paket fehlte — und zwar
aus der **aktuellsten Primärquelle**, die das Paket ohnehin führt.

**Damit ist ein dokumentierter offener Punkt entschieden: die Abbindung.** In
`QUELLEN.md` stand bisher „Abbindung durch Laien (Beleglage für eine potenziell
verstümmelnde Maßnahme zu dünn)“. Die ERC-Leitlinie 2025 sagt es für
Laienhelfer unmissverständlich:

> „Bei lebensbedrohlichen Blutungen an Extremitäten, die durch direkten manuellen
> Druck nicht gestillt werden können, muss so schnell wie möglich ein Tourniquet
> angelegt werden.“
>
> „Notieren Sie die Zeit der Anlage auf der Blutsperre.“

Der vorhandene Tipp `erste-hilfe-starke-blutung` sagte „fast jede Blutung lässt
sich durch genügend starken Druck stillen“ und schwieg darüber, was bei den
übrigen gilt. Er verweist jetzt weiter.

**Noch offen dazu:** Die Leitlinie beschreibt **nicht**, wie sich ein Tourniquet
behelfsmäßig herstellen lässt. Für eine Lage ohne Ausrüstung ist das eine echte
Lücke; erfunden wird dazu nichts. Steht in der Roadmap.

**Der kontraintuitivste Tipp des ganzen Pakets** ist ebenfalls von dort:

> „Lassen Sie eine offene Brustverletzung frei, damit sie ungehindert mit der
> Außenwelt in Kontakt bleiben kann. Legen Sie keinen Verband an und decken Sie
> die Wunde nicht ab.“

Das steht dem Grundsatz des Pakets entgegen („Jede Wunde wird … keimfrei
bedeckt“) — und der Reflex jedes Helfers ist, sie zuzudecken. `wunde-bedecken`
nennt die Ausnahme jetzt ausdrücklich, und ein Test hält beide Seiten fest.

**Und eine Lücke, die das Paket bewusst offengelassen hatte, ist geschlossen:**
Zum Ertrinken hieß es hier, die DRK-Seite gebe dafür zu wenig her. Die
ERC-Leitlinie hat einen eigenen Abschnitt — mit dem Satz, auf den es ankommt
(„Gehen Sie nicht ins Wasser, wenn Sie nicht in Wasserrettung geschult sind“) und
mit der Besonderheit, dass beim Ertrinken **fünf Atemspenden** vor der
Herzdruckmassage kommen, anders als sonst bei Erwachsenen.

### Deutsches Rotes Kreuz (DRK)

- Erste-Hilfe-Anleitung „Stabile Seitenlage":
  https://www.drk.de/hilfe-in-deutschland/erste-hilfe/stabile-seitenlage/
  (abgerufen am 28.07.2026)

Verwendet für: `erste-hilfe-stabile-seitenlage`,
`erste-hilfe-seitenlage-handgriffe`, `erste-hilfe-entscheidung-nach-atemkontrolle`.
Im Browser geöffnet und selbst gelesen; alle fünf Schritte, die Voraussetzung
(bewusstlos *und* ausreichende Atmung) und die Kontrollhinweise stammen
unmittelbar von dort.

Der Tipp zur Entscheidung nach der Atemkontrolle verbindet zwei Quellen: Die
Regel „keine oder keine normale Atmung ⇒ Herzdruckmassage" stammt vom GRC, die
Regel „bewusstlos mit ausreichender Atmung ⇒ Seitenlage" vom DRK. Beide wurden
am Original gelesen, und sie widersprechen einander nicht.

- Erste-Hilfe-Anleitung „Bedrohliche Blutungen":
  https://www.drk.de/hilfe-in-deutschland/erste-hilfe/blutungen-und-blutstillung/blutungen/
  (abgerufen am 28.07.2026)

Verwendet für: `erste-hilfe-starke-blutung`, `erste-hilfe-blutung-kopf-rumpf`,
`erste-hilfe-nasenbluten`, und mitbelegend für `erste-hilfe-druckverband`
(Weiterdrücken bis zur Übernahme, Druck auf eine Auflage statt auf die bloße
Wunde). Im Browser geöffnet und selbst gelesen: der Vorrang der Blutstillung vor
der Infektionsgefahr, das Arbeiten an der liegenden Person, das Weiterdrücken
bis zur Übernahme durch den Rettungsdienst und die Angabe zum Blutverlust.

**Bewusst weggelassen:** Die Seite nennt bei Frieren, Zittern und blasser Haut
das erhöhte Lagern der Beine. Genau dieser Punkt ist einer der ungeklärten
Widersprüche (Schocklagerung, siehe unten) — er ist deshalb nicht übernommen.

- Erste-Hilfe-Anleitung „Erste Hilfe bei Erstickungsgefahr":
  https://www.drk.de/hilfe-in-deutschland/erste-hilfe/ersticken/
  (abgerufen am 28.07.2026)

Verwendet für: `erste-hilfe-ersticken-erkennen`,
`erste-hilfe-ersticken-kann-husten`, `erste-hilfe-ersticken-rueckenschlaege`,
`erste-hilfe-ersticken-oberbauchkompression`, `erste-hilfe-insektenstich-mund`.
Im Browser geöffnet und selbst gelesen.

**Die Alters-Lücke ist inzwischen mit einer zweiten Quelle geschlossen:**

- „Reanimation 2025 – Leitlinien kompakt", Abschnitt „Fremdkörperverlegung der
  Atemwege" (Kinder):
  https://www.grc-org.de/files/Contentpages/document/260429_TB_Reanimation.pdf
  (abgerufen am 28.07.2026, Seiten 171–172)

Verwendet für: `erste-hilfe-ersticken-kind`, `erste-hilfe-ersticken-saeugling`.
Das PDF wurde heruntergeladen und der Abschnitt direkt gelesen.

**Warum das nötig war:** Ein Prüfdurchgang hat als *kritisch* gemeldet, dass der
Oberbauchkompressions-Tipp keine Altersgrenze nannte. Die DRK-Seite beschreibt
ausschließlich das Vorgehen bei Erwachsenen — bei einem **Säugling** sind Stöße
in den Oberbauch das falsche Verfahren; dort gelten Kompressionen auf den
**Brustkorb** mit der Zwei-Daumen-Technik. Ein Elternteil mit erstickendem
Säugling hätte im Paket eine Anweisung gefunden, die für sein Kind nicht gilt.
Der Erwachsenen-Tipp nennt die Grenze jetzt ausdrücklich, und für Kinder und
Säuglinge gibt es eigene Tipps aus der GRC-Quelle.

**Die zweite Lücke bleibt:** Der fünfte Fall der DRK-Seite, die Rettung Ertrinkender, besteht nur aus zwei
  Zeilen (Notruf, bei Bewusstlosigkeit ohne normale Atmung wiederbeleben) plus
  dem Hinweis auf Eigenschutz. Daraus ist bewusst **kein** eigener Tipp
  geworden: Ein Tipp, der zum Thema Ertrinken nur das sagt, würde mehr
  suggerieren, als er hergibt. Ertrinken ist als Ursache im Erkennen-Tipp
  genannt und braucht sonst eine eigene, ausführlichere Quelle.

- Erste-Hilfe-Anleitung „Vergiftungen und Hilfe bei Erbrechen":
  https://www.drk.de/hilfe-in-deutschland/erste-hilfe/vergiftungen-und-hilfe-bei-erbrechen/
  (abgerufen am 28.07.2026)

**Kohlenmonoxid — die Lücke ist am 28.07.2026 geschlossen worden.** Die
DRK-Seite behandelt Kohlen*dioxid*; Kohlen*monoxid* kommt dort nicht vor. Die
Quelle dafür sind die beiden DFV-Ausgaben weiter unten. Der entscheidende
Unterschied steht jetzt belegt in beiden Tipps: **Kohlenmonoxid ist leichter als
Luft** und sammelt sich nicht wie Kohlendioxid am Boden. Wer das verwechselt und
sich bückt, um „unter dem Gas zu bleiben", ist nicht geschützt.

Verwendet für: `erste-hilfe-vergiftung-erkennen`,
`erste-hilfe-vergiftung-atemwege`, `erste-hilfe-kohlendioxid`,
`erste-hilfe-kontaktgift`, `erste-hilfe-erbrechen-helfen`. Im Browser geöffnet
und selbst gelesen.

**Wichtige Abgrenzung, die beim Recherchieren fast schiefgegangen wäre:** Die
Seite behandelt **Kohlendioxid** (CO₂), nicht **Kohlenmonoxid** (CO). Das sind
verschiedene Gase mit verschiedenem Verhalten — CO₂ ist schwerer als Luft und
sammelt sich als „See" am Boden, CO nicht. Eine Suchmaschinen-Zusammenfassung
hatte beides vermischt und die CO₂-Beschreibung als Antwort auf eine Frage nach
Kohlenmonoxid geliefert. Übernommen wurde ausschließlich, was auf der Seite
selbst steht, und der Tipp heißt entsprechend „Kohlendioxid".

**Die eigentliche Lücke bleibt damit offen:** Kohlen**monoxid** aus Grill,
Campingkocher, Stromerzeuger oder Notheizung in Innenräumen — der typische
Zweittote bei Stromausfall — ist auf dieser Seite **nicht** behandelt und
deshalb auch nicht im Paket. Das braucht eine eigene Quelle und steht so in der
Roadmap.

- Erste-Hilfe-Anleitung „Grundsätzliche Verbote bei der Wundbehandlung":
  https://www.drk.de/hilfe-in-deutschland/erste-hilfe/wundbehandlung/verbote-bei-wunden/
  (abgerufen am 28.07.2026)
- Erste-Hilfe-Anleitung „Wundversorgung, Verbände und Hilfsmittel"
  (der Seitentitel im Browser lautet abweichend „Wundbedeckung" — beide Namen
  stehen deshalb in der Quellenangabe, damit die Seite offline wiederfindbar
  bleibt):
  https://www.drk.de/hilfe-in-deutschland/erste-hilfe/wundbehandlung/wundversorgung-und-verbaende/
  (abgerufen am 28.07.2026)

Verwendet für: `erste-hilfe-wunde-verbote`, `erste-hilfe-wunde-bedecken`,
`erste-hilfe-fremdkoerper-in-wunde`, `erste-hilfe-wunde-tetanus`. Beide Seiten
im Browser geöffnet und selbst gelesen.

**Ein Widerspruch im Paket, bewusst aufgelöst:** Die Verbote-Seite untersagt es,
eine Wunde zu berühren. Die Seite „Bedrohliche Blutungen" verlangt bei starker
Blutung genau das Gegenteil — notfalls in die Wunde hineindrücken, eine mögliche
Infektion in Kauf nehmen. Beim DRK stehen beide auf getrennten Seiten mit
eigenem Zusammenhang; in einer durchsuchbaren Tippliste prallen sie aufeinander,
und ein Laie unter Stress hat keinen Weg zu wissen, welche Regel vorgeht.
Deshalb nennt `erste-hilfe-wunde-verbote` den Vorrang der Blutstillung
ausdrücklich und verweist auf den Blutungs-Tipp. Der Zusatz ist keine eigene
Wertung: Er steht wörtlich so auf der Blutungs-Seite, die deshalb als zweite
Quelle geführt wird.

**Die Verbrennung ist damit nicht abgehandelt.** Sie kommt im ganzen Paket nur
als Nebensatz in der Ausnahmeliste vor („mit Wasser kühlen") — ohne Dauer,
Temperatur oder Flächengrenze. Genau diese Angaben sind einer der vier
ungeklärten Widersprüche (siehe unten). Der Halbsatz schließt die Lücke nicht.

**Die bekannte Lücke bleibt sichtbar:** Beide Seiten enden bei „schnellstmöglich
zum Arzt". Für tagelange Isolation ohne erreichbaren Arzt gibt es dort keine
Anleitung, und es ist auch keine erfunden worden — der Satz steht in den Tipps
so, wie die Quelle ihn formuliert. Das ist die schmerzhafteste offene Stelle des
ganzen Kapitels und braucht eine eigene, belastbare Quelle.

- Erste-Hilfe-Anleitung „Druckverband anlegen":
  https://www.drk.de/hilfe-in-deutschland/erste-hilfe/erste-hilfe-online/blutungen-und-blutstillung/druckverband/
  (abgerufen am 28.07.2026)

Verwendet für: `erste-hilfe-druckverband`,
`erste-hilfe-druckverband-dreiecktuch`. Im Browser geöffnet und selbst gelesen.
Damit ist der vom Prüfdurchgang bemängelte Verweis ins Leere geschlossen: Der
Druckverband wird jetzt erklärt, statt nur angeordnet zu werden.

**Offene Lücke, ausdrücklich nicht gefüllt:** Die Seite sagt **nichts** darüber,
was als Druckpolster taugt. Ein erster Entwurf hatte hier „alles Feste, das sich
umwickeln lässt" — das beschreibt einen Stein, ein Feuerzeug und ein Handy, also
genau die Gegenstände, deren harte Kanten sich ins Gewebe drücken. Der Satz ist
entfernt und **nicht** durch eine eigene Faustregel ersetzt worden; dafür braucht
es eine belegbare Quelle. Ebenfalls offen: was zu tun ist, wenn Blut durch den
Verband tritt, und wie man erkennt, dass der Verband abschnürt. Beides wiegt
schwerer, wenn kein Rettungsdienst kommt — die Quelle unterstellt den Normalfall.

**Bewusste Abweichung:** Die Seite transportiert über Fotos, dass die
Bindengänge über dem Polster mit Zug angelegt werden. Im reinen Text geht das
verloren, deshalb steht „fest darüber – der Zug ist das, was den Druck erzeugt"
im Tipp; gedeckt ist das durch die Blutungen-Seite, die „genügend starken Druck"
verlangt.

- Erste-Hilfe-Anleitung „Knochenbrüche":
  https://www.drk.de/hilfe-in-deutschland/erste-hilfe/knochenbruch/knochenbrueche/
  (abgerufen am 28.07.2026)

Verwendet für: `erste-hilfe-knochenbruch-erkennen`,
`erste-hilfe-knochenbruch-versorgen`. Ebenfalls im Browser geöffnet und selbst
gelesen. Die Anleitung nennt zusätzlich „Schockbekämpfung" als Maßnahme; was das
konkret heißt, ist Teil des ungeklärten Widerspruchs zur Schocklagerung und
deshalb nicht in den Tipp übernommen.

- Erste-Hilfe-Anleitung „Erfrierungen und Unterkühlungen":
  https://www.drk.de/hilfe-in-deutschland/erste-hilfe/erfrierungen-und-unterkuehlungen/
  (abgerufen am 28.07.2026)

Verwendet für: `erste-hilfe-unterkuehlung-stadium-eins`,
`erste-hilfe-unterkuehlung-stadium-zwei`, `erste-hilfe-erfrierungen-erkennen`,
`erste-hilfe-erfrierungen-versorgen`, `erste-hilfe-waermeerhalt`. Im Browser geöffnet und selbst gelesen.

**Damit ist die fünfte der offenen Fragen geklärt** — allerdings nicht durch eine
eigene Entscheidung, sondern weil die Quelle selbst die Unterscheidung trifft:
Warme, gezuckerte Getränke gehören zu Stadium I und setzen Bewusstsein voraus; ab
Stadium II sind Aufwärmversuche ausdrücklich verboten. Beide Stadien haben
deshalb einen eigenen Tipp, und der zu Stadium II benennt die Verwechslung
ausdrücklich. Die übrigen vier Widersprüche bleiben offen.

- Erste-Hilfe-Anleitung „Hitzschlag – was tun?":
  https://www.drk.de/hilfe-in-deutschland/erste-hilfe/hitzschlag/
  (abgerufen am 28.07.2026)

Verwendet für: `erste-hilfe-hitzschlag-erkennen`,
`erste-hilfe-hitzschlag-handeln`, `erste-hilfe-hitze-vorbeugen`. Im Browser
geöffnet und selbst gelesen.

**Berührt die offene Frage zur Lagerung:** Die Anleitung nennt für den
Hitzschlag drei verschiedene Lagerungen je nach Zustand — Oberkörper hoch (immer),
zusätzlich Beine hoch (bei Benommenheit), stabile Seitenlage (bei
Bewusstlosigkeit). Der mittlere Punkt ist übernommen, weil er hier eine
zustandsbezogene Anweisung der Quelle für genau dieses Krankheitsbild ist —
nicht die allgemeine Schocklagerung, die zwischen DRK und ERC strittig bleibt.
Der Unterschied ist bewusst gemacht und nicht übersehen worden.

**Bewusste Abweichung von der Reihenfolge der Quelle:** Die Seite listet
Notruf und Kühlen erst an fünfter und siebter Stelle. Im Tipp stehen sie vorn,
weil das Kühlen beim Hitzschlag die entscheidende Maßnahme ist. Die Inhalte sind
unverändert, nur die Reihenfolge ist bewusst geändert.

### Internationale Föderation der Rotkreuz- und Rothalbmond-Gesellschaften (IFRC)

Geladen und ausgewertet am 28.07.2026 für den zweiten Zweig „niemand kommt":

- „International first aid, resuscitation, and education guidelines 2020",
  476 Seiten, PDF von `ifrc.org` heruntergeladen und lokal in Text umgewandelt
  (abgerufen am 28.07.2026). Liegt unter `work/zweig/` (nicht versioniert)

**Warum diese Quelle für dieses Projekt anders wiegt als alle bisherigen.** Alle
deutschen Laienquellen im Paket enden bei „Notruf" oder „zum Arzt" — das ist in
`QUELLEN.md` mehrfach als die schmerzhafteste Lücke festgehalten. Die
IFRC-Leitlinie kennt dafür einen **eigenen Kontext**: „Remote refers to a delay
in medical care and access to additional resources … medical care could be hours
or days away" (S. 30). Zu vielen Einzelthemen gibt es dort eigene Absätze
„Local adaptations" mit der Einleitung „If in an area with limited resources
(e.g., a wilderness or remote environment with longer wait times to EMS)".

Das ist genau die Grundannahme dieser App — **belegt**, statt von uns
abgeleitet. Damit ist der Roadmap-Punkt „zweiter Zweig" nicht mehr in erster
Linie eine Abwägungsaufgabe, sondern eine Auswertungsaufgabe. Übernommen ist
bisher nur die Blutungs-Gruppe; der Rest steht in der Roadmap.

**Zweiter Auswertungsdurchgang am 28.07.2026 (dreizehn offene Themen).** Der
Arbeitsstand mit allen Zitaten liegt unter `work/recherche-zweig2/entwurf.md`;
jedes tragende Zitat wurde vor der Übernahme einzeln im Volltext nachgeschlagen.
Eingearbeitet, je mit eigener IFRC-Belegangabe am Tipp:

- `erste-hilfe-knochenbruch-versorgen` (S. 223–224): Schienen für den holprigen
  Transport, behelfsmäßig aus Zeitungspapier, Holz oder einem zusammengerollten
  Pullover; die eng gefasste Ausnahme Geradeziehen **nur** bei kalter, blasser
  Gliedmaße durch dafür Ausgebildete in abgelegener Lage. Beim Übernehmen wurde
  ein selbst formulierter Technik-Satz (Polsterung, Gelenke ober- und unterhalb)
  wieder gestrichen — die Quelle trägt ihn nicht.
- `erste-hilfe-offene-brustwunde` (S. 195–196): beim langen Transport nur eine
  nicht luftdichte Abdeckung, laufend prüfen, weil geronnenes Blut sie
  abdichten kann; abgedichtet ist gefährlicher als offen.
- `erste-hilfe-asthma` (S. 180–182): ohne Inhalator und Sauerstoff bleiben
  Beruhigung (offenes Fenster) und Haltung (sitzend, Arme aufgestützt) — als
  Erleichterung gekennzeichnet, nicht als Behandlung.
- `erste-hilfe-allergischer-schock` (S. 280–281): ohne Autoinjektor
  Antihistaminika oder Kortison **nach ärztlicher Rücksprache**; die
  Rücksprache-Grenze der Quelle steht ausdrücklich im Tipp, ebenso der Vorrang
  vorhandenen Adrenalins.
- `erste-hilfe-suizidgedanken` (S. 371–373): Verbunden bleiben („befriending")
  mit belegter Wirkung auf das seelische Leid; die fünf Prinzipien nach
  Hobfoll et al.; der Versuch, Hilfe zu erreichen, bleibt bei konkreter
  Drohung bestehen.
- `erste-hilfe-petermaennchen` (S. 250–251, als Abwägung gekennzeichnet): Die
  Beobachtungs- und Tetanusregeln der Quelle gelten allgemein für
  Wassertier-Verletzungen und sind nicht eigens auf fehlende Hilfe gemünzt —
  der Zweig sagt das dazu.

**Geprüft und ohne Ergebnis — die Leitlinie trägt für „niemand kommt" nichts
bei:** Insektenstich im Mund (nur Überbrückung „while waiting for medical
care"), Rauchvergiftung (kein eigener Abschnitt), Nasenbluten (kein
Abschnitt), Brustschmerzen (nur ein Ausbildungshinweis zur Vorbeugung),
Kontaktgift über die Haut (das Spülen im Burns-Kapitel meint Verätzung, nicht
systemische Aufnahme — keine Übertragung), Botulismus (kommt im gesamten
Dokument nicht vor), anhaltender Durchfall (die WHO-Quelle des Tipps ist
selbst schon auf ressourcenarme Lagen zugeschnitten). Diese sieben brauchen
andere Quellen oder bleiben ohne zweiten Zweig; nichts davon wurde erfunden.
Die Grenzen der sechs eingearbeiteten Zweige sind in
`ZusammenspielTest.dieNeuenZweitenZweigeNennenIhreGrenzen` festgeschrieben.

**Ausgewertet und übernommen (Abschnitt „Severe bleeding", S. 187–194):**

- Die verletzte Person soll, wenn sie kann, **zuerst selbst** auf ihre Wunde
  drücken (First aid step 1). Das steht im Paket jetzt in `erste-hilfe-starke-blutung`
  — es hält die Hände des Helfers frei, was ohne Rettungsdienst zählt.
- Handschuhe, sonst Plastiktüte, sonst Tuch; und wenn gar nichts da ist, die
  bloße Hand, solange sie keine offene Wunde hat: „the risk of infection is very
  low" (S. 189).
- Durchgeblutete Auflage: zweite darüber und **mehr** Druck — aber nicht immer
  weitere Lagen. Charlton et al. (2018) haben gemessen, dass der an der Wunde
  ankommende Druck mit der Zahl der Lagen sinkt (S. 194).
- **Haltung.** Charlton et al. (2019): Mit durchgestreckten Armen hielten Helfer
  den Zieldruck über drei Minuten in 100 Prozent der Zeit, mit angewinkelten
  Armen in 63,7 Prozent (S. 193 f.). Das klingt nach einer Kleinigkeit und ist
  der Unterschied zwischen „drei Minuten" und „solange es dauert" — deshalb steht
  es im zweiten Zweig von `erste-hilfe-druckverband`.
- Schock ist bei starkem Blutverlust zu erwarten: flach auf den Rücken, warm
  halten (First aid step 6).

**Die wichtigste Aussage ist eine, die die Quelle NICHT macht.** Zur Abbindung
steht dort: „Once a tourniquet has been applied, keep it in place until EMS
arrives", und für Sonderlagen ausdrücklich: „the release of the tourniquet should
only be considered under the guidance of a medical professional" (S. 188). Für
den Fall, dass niemand kommt, gibt es also **keine Laienanweisung zum Lösen** —
und das ist kein Versäumnis der Leitlinie, sondern ihre Warnung.

Genau so steht es jetzt in `erste-hilfe-abbinden`. Die Versuchung war groß, hier
eine Regel zu bilden („nach zwei Stunden lockern"); solche Regeln stehen im Netz
reichlich herum. Sie wäre erfunden gewesen. Stattdessen zieht der Tipp die
Folgerung, die tatsächlich trägt: Wenn das Lösen keine Laienmaßnahme ist, dann
entscheidet sich alles beim **Anlegen** — abgebunden wird erst, wenn fester Druck
die lebensbedrohliche Blutung wirklich nicht stillt. Ein Test hält das fest:
`derAbbindeTippErfindetKeineRegelZumLoesen`.

**Damit ist auch der Roadmap-Punkt „behelfsmäßiges Tourniquet" beantwortet — mit
Nein.** Die Leitlinie erlaubt die behelfsmäßige Abbindung ausdrücklich, wenn
nichts anderes da ist, liefert aber im Education review die gemessenen Zahlen
(McCarty et al., 2019, S. 194): Knebel aus Kunststoff brachen in 70 Prozent der
Anlagen; bei Holzknebel mit Ledergürtel riss der Gürtel in 45,8 Prozent; ohne
Knebel zum Verdrehen war der Druck in **jeder** Simulation zu niedrig, nur 1 von
22 Anlagen gelang. Dazu die dort zitierte Auswertung von King et al. (2015): Beim
Anschlag auf den Boston-Marathon wurden 27 behelfsmäßige Abbindungen angelegt,
und keine einzige wurde nachträglich als wirksam bewertet.

Eine Bastelanleitung steht deshalb **nicht** im Paket. Der Satz „Hast du kein
Tourniquet, dann drück weiter mit der Hand" stand dort schon vorher aus der
ERC-Leitlinie; er ist jetzt belegt begründet, statt nur behauptet.

**Beim Schreiben beinahe selbst gebaut, und deshalb hier vermerkt:** Der Satz
„an Kopf und Rumpf wird nicht abgebunden" wurde in `erste-hilfe-blutung-kopf-rumpf`
ergänzt — einen Tipp, dessen Titel „Kopf, Rumpf **oder Bein**" lautet. Ohne die
Ausnahme für das Bein hätte er sich als generelles Abbindeverbot gelesen, und
zwar für den einen Körperteil, an dem die Abbindung Leben rettet. Berichtigt und
mit `derKopfUndRumpfTippSchicktAmBeinZumAbbinden` festgehalten.

**Schlangenbiss (S. 254 f.), eingebaut am 28.07.2026 auf ausdrückliche
Weisung.** Die
Leitlinie bestätigt alle vier Verbote des Pakets (keine Abbindung, kein
Aussaugen, keine Kälte, kein Aufschneiden). Sie verlangt aber zusätzlich und
ausdrücklich für abgelegene Lagen etwas, das fehlte: das betroffene Glied mit
einer **nicht dehnbaren** Binde ruhigstellen, notfalls mit Kleidung, und die
Wunde mit sauberem Wasser waschen.

Das Heikle daran ist die Verwechslungsgefahr, und sie ist im Tipp jetzt
ausgesprochen statt umgangen: Ein **Druckverband** soll eine Blutung stoppen und
presst dafür auf die Wunde — deshalb ist er hier verboten. Die **ruhigstellende
Binde** soll nichts pressen, sondern Bewegung verhindern, weil Bewegung das Gift
schneller im Körper verteilt. Ohne diesen Satz liest sich „kein Druckverband" als
„nichts darum herum", und dann fällt genau die Maßnahme weg, die zählt, wenn die
Klinik weit ist. Die **Druck-Ruhigstellung** (Polster unter der Binde) nennt die
Leitlinie ausdrücklich nur für Geübte; im Tipp steht das ebenso.

Dazu aus denselben Abschnitten: betroffene Stelle auf Herzhöhe oder tiefer, auf
dem gebissenen Bein nicht laufen, solange es eine andere Möglichkeit gibt, und
notfalls tragen (behelfsmäßige Trage oder Sitz aus den verschränkten Armen zweier
Helfer). Test: `derSchlangenbissTippTrenntDruckverbandVonRuhigstellung`.

**Wunden (Abschnitt „Cuts and grazes", S. 202 f.) — hier kehrt sich eine Regel
des Pakets um.** `erste-hilfe-wunde-verbote` verbietet nach deutscher
Laien-Erstversorgung ausdrücklich das Auswaschen und jedes Desinfektionsmittel.
Die IFRC-Leitlinie verlangt für oberflächliche Schnitt- und Schürfwunden das
Gegenteil: mit sauberem Wasser reinigen, am besten lauwarm und mit etwas Druck,
und wenn kein sauberes Wasser da ist, mit einem Desinfektionsmittel.

Das ist kein Widerspruch, sondern eine Verzweigung: Die deutsche Regel setzt
stillschweigend voraus, dass die endgültige Versorgung in Stunden folgt. Folgt
sie nicht, wird die Infektion zur größeren Gefahr als das Reinigen. Beide
Zweige stehen jetzt im Tipp, mit der Bedingung dazu und begrenzt auf
oberflächliche Wunden — tiefe und stark blutende bleiben ausgenommen, Fremdkörper
bleiben in jedem Fall drin. Test: `dieBeidenWundRegelnNennenIhreBedingung`.

Übernommen für die Versorgung über Tage (`erste-hilfe-wunde-bedecken`):

- Ohne Wasserhahn eine saubere, unbenutzte Wasserflasche anstechen und damit
  einen Strahl auf die Wunde drücken (Local adaptation, S. 203). Das ist die
  einzige Stelle in allen bisher gelesenen Quellen, die diese Frage überhaupt
  beantwortet.
- Verband täglich von außen ansehen: sauber → nicht wechseln; sichtbar mit Blut
  oder klarer Flüssigkeit verfärbt → abnehmen, reinigen, neu (Recovery, S. 203).
- Infektionszeichen: Haut rings um die Wunde rot, violett oder dunkler, warm und
  schmerzhaft, oder Fieber.
- „Never cover an infected wound; seek medical help." Der Tipp gibt beides
  wieder — und sagt offen, dass die Quelle danach nichts mehr hergibt, wenn keine
  ärztliche Hilfe kommt. Erfunden wird an dieser Stelle nichts.

Beim Fremdkörper liefert die Leitlinie den Grund, der im Paket fehlte: Der
Gegenstand bleibt drin, weil er die Wunde wie ein Pfropfen verschließen kann und
das Ziehen sie öffnet (S. 188 und 190). Beim Tetanus bleibt es bei der ehrlichen
Auskunft: kein Hausmittel, keine Behandlung ohne Arzt — deshalb ist es ein
Vorsorge-Tipp.

**Erfrierungen (Abschnitt „Frostbite", S. 349 f.) — das sauberste Beispiel für
den zweiten Zweig im ganzen Paket.** Die Leitlinie knüpft das Auftauen selbst an
die Bedingung, um die es hier geht: „Warming of frozen body parts should be done
only if the appropriate resources are available, **medical care is more than two
hours away** and there is no risk of refreezing." Damit steht die Verzweigung in
der Quelle und muss nicht von uns gebaut werden. Das bisherige „keine aktive
Wärme" aus der DRK-Anleitung bleibt richtig, solange Hilfe absehbar ist.

Übernommen: Wasserbad von 37 bis 39 °C, bis die Stelle rot oder violett wird und
sich weich und geschmeidig anfühlt (meist rund 30 Minuten); ausdrücklich keine
anderen Wärmequellen (Feuer, Heizgerät, Ofen, erhitzte Steine) wegen der
Verbrennungsgefahr an einer gefühllosen Stelle; nicht reiben; danach keimfreie
Gaze, bei mehreren Fingern oder Zehen Gaze **zwischen** jeden einzelnen; danach
vor erneutem Gefrieren schützen.

**Dosierung, mit eigenem Vorbehalts-Absatz wie vorgegeben:** hohe Dosis
Ibuprofen 400–800 mg, ersatzweise niedrige Dosis Acetylsalicylsäure 75–80 mg,
„this may improve healing". Der Vorbehalt nennt ausdrücklich, dass die Quelle an
dieser Stelle **keine** Kinderdosis angibt — dann bleibt es beim Aufwärmen ohne
Mittel. Muster ist `erste-hilfe-borreliose-behandeln`.
Nicht übernommen aus demselben Abschnitt: „Topical aloe vera may be applied."
Das ändert keine Entscheidung und ist in der Lage, für die dieses Paket gebaut
ist, praktisch nie verfügbar.
Test: `dasAuftauenBeiErfrierungenNenntAlleDreiBedingungen`.

**Unterkühlung (Abschnitt „Hypothermia", S. 343–345) — und hier ein
Quellenwiderspruch, der NICHT aufgelöst ist.** Unstrittig und übernommen ist
alles, was den Wärmeverlust betrifft und im Paket fehlte: eine Sperre zwischen
Person und Boden (über den Boden geht Wärme schneller verloren als an die Luft),
Kopf und Rumpf mitbedecken, Schlafsack als bestes passives Mittel, nasse Kleidung
bei starker Unterkühlung **aufschneiden** statt ausziehen, damit sich die Person
kaum bewegt, und warmes gezuckertes Getränk oder energiereiches Essen, solange
sie schlucken kann.

Strittig war das aktive Wärmen. Die IFRC-Leitlinie sagt: „If the person is **not
shivering**, the first aid provider should actively warm them preferably using an
electric heating blanket." Die DRK-Anleitung, aus der
`erste-hilfe-unterkuehlung-stadium-zwei` stammt, verbietet ab dem zweiten Stadium
genau das — und das zweite Stadium ist gerade dadurch gekennzeichnet, dass das
Zittern aufhört. Es ging also um dieselbe Person mit gegenteiliger Anweisung.

### Wilderness Medical Society (WMS) — und wie der Widerspruch aufgelöst wurde

Nach der Weisung vom 28.07.2026 wird bei widersprüchlichen Quellen nicht mehr
nur der Widerspruch vermerkt, sondern nachgeschlagen, bis eine Entscheidung
möglich ist. Dafür geladen und im Volltext gelesen:

- Wilderness Medical Society, „Practice Guidelines for the Out-of-Hospital
  Evaluation and Treatment of Accidental Hypothermia", Fassung 2019 (Wilderness
  Environ Med 30(4S):S47–S69) — gelesen in der Zusammenfassung der WMS selbst
  (`wms.org`, 4 Seiten PDF) und in der Aufbereitung von American Family Physician
  2020;102(9):571 mit der vollständigen Stadientabelle (`aafp.org`), beide
  abgerufen am 28.07.2026. Diese Leitlinie ist für genau die Lage geschrieben, um
  die es hier geht: Behandlung außerhalb des Krankenhauses, in der Wildnis,
  mit langen Wegen

**Damit ist es kein Widerspruch mehr, sondern eine Verkürzung.** Die WMS nennt den
Mechanismus, den die DRK-Anleitung meint, aber nicht ausspricht: *Afterdrop* —
„a continued drop in core temperature after removal from the cold environment due
to the return of cooler blood from extremities to core". Gefährlich ist demnach
nicht Wärme an sich, sondern **Wärme an Armen und Beinen** und **jede unnötige
Bewegung**: „This afterdrop can be caused by patient movement, warm water
immersion, or other heat applied to extremities."

Die Stadientabelle der WMS ist eindeutig. Bei mäßiger Unterkühlung (Zittern
verlangsamt oder hört auf, Person noch bei Bewusstsein) steht dort: „Handle
gently. Keep horizontal. **No food or drink.** Insulate/vapor barrier. **Active
heating to upper trunk.**" Und zur Stelle: „Active external heat sources should be
applied to the axillae, chest, and back."

Damit stehen zwei unabhängige Fachquellen (IFRC 2020, WMS 2019) gegen eine
verkürzte Laien-Anleitung, und die eine Quelle liefert zugleich die Erklärung für
die andere. **Entschieden und umgesetzt**, statt vorgelegt: Der Tipp verbietet
jetzt genau das, was belegt schadet — Wärme an den Gliedmaßen, warmes Bad,
Reiben, unnötige Bewegung —, und verlangt genau das, was belegt hilft: Wärme an
Achseln, Brust und Rücken, waagerechte Lage, Isolierung mit wasserdichter Schicht
besonders um Kopf und Nacken und zum Boden hin, nasse Kleidung erst am
geschützten Ort und dann aufgeschnitten, ab diesem Stadium nichts mehr zu essen
und zu trinken, und alle 20 bis 30 Minuten die erwärmte Haut ansehen, weil
unterkühlte Haut eine Verbrennung nicht meldet.

**Der Titel musste mit.** Er hieß „Unterkühlung Stadium II: nicht mehr aufwärmen"
und behauptete damit das Gegenteil des eigenen Textes. Die erste neue Fassung
„…: Wärme nur an den Oberkörper" fiel in der Messung durch: „Wärme" im Titel holt
6 Punkte, und weil „U" vor „W" sortiert, hätte der Tipp `erste-hilfe-waermeerhalt`
die Anfrage `wärme` abgenommen — der Test
`dieNahrungsTippsVerdraengenKeinenNotfallTipp` hat das gefangen. Der Titel lautet
jetzt „Unterkühlung Stadium II: nur den Oberkörper wärmen"; „wärmen" ist nur
Wortanfang und bekommt deshalb 5 statt 6 Punkte.
Test: `derUnterkuehlungsTippTrenntGliedmassenVomOberkoerper`.

**Nebenbefund zur Arbeitsweise, teuer gelernt:** `./gradlew build` allein prüft
Inhaltsänderungen **nicht**. Die Tests lesen das Paket zur Laufzeit aus dem
Dateisystem; für Gradle bleibt der Test-Task „UP-TO-DATE", und der Lauf meldet
grün, ohne einen einzigen Inhaltstest ausgeführt zu haben. Nur
`./gradlew clean build --rerun-tasks --no-build-cache` prüft wirklich — und die
Testzahl gehört danach nachgezählt. Beim Umbenennen des Unterkühlungs-Tipps
meldete der schnelle Lauf „BUILD SUCCESSFUL", während zwei Tests in Wahrheit
fehlschlugen.

### Bewusst noch nicht übernommen

Zur Kategorie Erste Hilfe liegt eine deutlich umfangreichere Recherche vor
(Blutungen, Verbrennungen, Unterkühlung, Hitze, Schock, Knochenbrüche,
Wundversorgung). Davon ist **nichts** übernommen, solange die Fundstelle nicht
selbst am Original gelesen ist. Die stabile Seitenlage war ursprünglich
ebenfalls in dieser Liste und ist inzwischen aufgenommen, weil die DRK-Seite
nachgelesen wurde — genau dieser Schritt fehlt den übrigen Themen noch.

Fünf Punkte brauchen zusätzlich eine Entscheidung, weil anerkannte
Organisationen einander widersprechen — sie werden bewusst nicht durch eine
eigene Wahl aufgelöst:

1. Kühldauer bei Verbrennungen: DRK und ERC nennen mindestens 20 Minuten,
   Malteser und Johanniter nennen keine Minutenangabe, sondern „bis die
   Schmerzen nachlassen". Auch Flächengrenze und Wassertemperatur weichen ab.
2. Brandsalbe: Malteser empfiehlt sie, das DRK verbietet Salben auf Wunden.
3. Schocklagerung: DRK legt die Beine hoch, eine ERC-Passage nennt nur
   Rückenlage.
4. Druckpunkt der Herzdruckmassage: GRC schreibt „untere Hälfte des
   Brustbeins", DRK „unteres Drittel". Beide meinen dieselbe Stelle. Der Tipp
   im Paket verwendet die GRC-Formulierung zusammen mit der Angabe „Mitte des
   Brustkorbs", die in beiden Quellen wortgleich vorkommt — das ist keine
   Entscheidung zwischen den Quellen, sondern deren gemeinsamer Nenner.
5. ~~Wärme bei Unterkühlung~~ — **geklärt am 28.07.2026**: Die DRK-Anleitung
   unterscheidet die Stadien selbst. Warme, gezuckerte Getränke gehören zu
   Stadium I und setzen Bewusstsein voraus; ab Stadium II sind Aufwärmversuche
   verboten. Beide Stadien haben jetzt einen eigenen Tipp.

### Weltgesundheitsorganisation (WHO) — Durchfall und Flüssigkeitsersatz

- „The Treatment of Diarrhoea — A manual for physicians and other senior health
  workers“, Department of Child and Adolescent Health and Development:
  https://iris.who.int/server/api/core/bitstreams/df59ceab-2498-4a64-bd93-ad7566addb4e/content
  (abgerufen am 28.07.2026, PDF heruntergeladen, 50 Seiten, Text lokal
  herausgezogen und gelesen — nicht über eine Zusammenfassung)

Verwendet für: `erste-hilfe-austrocknung-erkennen`,
`erste-hilfe-durchfall-trinken`, `erste-hilfe-durchfall-nicht-trinken`,
`erste-hilfe-trinkloesung-selbst-ansetzen`, `erste-hilfe-durchfall-hilfe-holen`.

**Warum diese Lücke geschlossen wurde:** Neun Wassertipps arbeiten daran,
Krankheit durch Wasser zu verhindern; keiner sagte, was zu tun ist, wenn es
trotzdem passiert. Austrocknung durch Durchfall ist der klassische Todesfall in
genau dieser Lage. Die WHO dazu wörtlich: „Many diarrhoeal deaths are caused by
dehydration."

**Aus derselben Quelle am 28.07.2026 nachgezogen — fünf weitere Tipps, die ein
Prüfdurchgang als fehlend benannt hat:**

- **Zink** ist im WHO-Handbuch **Regel 2 von vier** im Behandlungsplan für
  zuhause, keine Randbemerkung: „it is now recommended that zinc (10-20 mg/day)
  be given for 10 to 14 days to all children with diarrhoea“. Es fehlte
  vollständig. Für eine Lage ohne Apotheke ist die Darreichungsform der Quelle
  (Saft, Schmelztablette) nicht herstellbar — deshalb steht Zink jetzt vor allem
  als **Vorratsposten** im Paket, mit der Dosis und mit dem ausdrücklichen
  Zusatz, dass es das Trinken nicht ersetzt. Die Empfehlung gilt für Kinder.
- **Durchfallmittel und Antibiotika.** Der Fund, nach dem niemand gefragt hatte,
  und der praktisch wichtigste dieses Durchgangs: „‘Antidiarrhoeal’ drugs and
  anti-emetics have no practical benefits … Some have dangerous, and sometimes
  fatal, side-effects. **These drugs should never be given to children below 5
  years.**“ In einer Krise greift jemand genau nach der Packung im Schrank.
  Ebenso belegt: „Antimicrobials should not be used routinely“ — verlässlich
  helfen sie nur bei blutigem Durchfall, bei Cholera-Verdacht mit schwerer
  Austrocknung und bei ernsten Infektionen außerhalb des Darms.
- **Händewaschen**, mit dem Satz, für den es diese App gibt: „Good handwashing
  requires the use of soap **or a local substitute, such as ashes or soil**, and
  enough water to rinse the hands thoroughly.“ Dasselbe Mittel nennt
  `wasser-chlor-entkeimung` bereits.
- **Lebensmittelhygiene** — die sieben Punkte aus Abschnitt 11.5, vollständig
  übernommen.
- **Entsorgung von Stuhl** — fester Platz und sofort vergraben, wenn keine
  Toilette geht; Latrinen mehr als zehn Meter von der Wasserstelle und unterhalb
  davon (11.3 und 11.6). Das schließt die Brücke zu den Wasser-Tipps in der
  Gegenrichtung.

**Drei weitere Tipps aus derselben Quelle, 28.07.2026** — die letzten Lücken,
die ein Prüfdurchgang benannt hatte:

- **Anhaltender Durchfall (Abschnitt 7).** „This is diarrhoea, with or without
  blood, that begins acutely and lasts at least 14 days." Für eine Lage ohne Arzt
  der wertvollste Abschnitt des Handbuchs, weil er den Fall beschreibt, der den
  Akutmoment überdauert. Mit der ausdrücklichen Warnung „Routine treatment of
  persistent diarrhoea with antimicrobials is not effective and should not be
  given" und der Sieben-Tage-Regel aus 7.6.1.
- **Antibiotika-Reste aus dem Schrank (Tabelle 4).** Die Liste der Mittel, die
  bei Ruhr unwirksam sind, „irrespective of the sensitivity of local strains …
  They should never be given to treat presumed shigellosis": Metronidazol,
  Streptomycin, Tetrazykline, Chloramphenicol, Sulfonamide, Amoxicillin,
  Nitrofurane, Aminoglykoside, Cephalosporine der ersten und zweiten Generation.
  Das ist ziemlich genau der Bestand einer Hausapotheke an Restpackungen. Der
  Tipp nennt **nicht**, welches Mittel stattdessen wirkt — das hinge von den
  örtlichen Erregern ab und wäre eine Aufforderung zur Selbstmedikation.
- **Weiteressen als Behandlung (2.4 und Regel 3).** „Children who die from
  diarrhoea, **despite good management of dehydration**, are usually
  malnourished." Dazu die umsetzbaren Angaben: alle drei bis vier Stunden
  anbieten, und nach dem Durchfall mindestens zwei Wochen lang eine Mahlzeit mehr
  als sonst.

Beim Einbau hat der Test `beiDurchfallWoerternStehtDasTrinkenGanzOben` sofort
angeschlagen: Die neuen Titel („Bei Durchfall essen…", „Durchfall, der nicht
aufhört…") standen alphabetisch vor dem Behandlungs-Tipp und drängten ihn aus
den ersten drei Treffern. Umbenannt in „Weiter essen bei Durchfall…" und „Wenn
der Durchfall nach zwei Wochen nicht aufhört". **Das ist inzwischen das dritte
Mal, dass ein neuer Titel die Reihenfolge kippt** — wer hier weiterarbeitet,
sollte neue Tipps zu einem bestehenden Thema nicht mit dem Themenwort beginnen
lassen.

**Nachtrag vom 28.07.2026 — der Befund, der die Richtung umdreht.** Der Satz
„Durchfallmittel haben keinen praktischen Nutzen“ stand ohne Geltungsbereich im
Paket. In der Quelle steht er zweimal, und beide Male mit einer Grenze:
„‘Antidiarrhoeal’ drugs and anti-emetics have no practical benefits **for
children**“ (2.6) und „never indicated for the treatment of acute diarrhoea **in
children**“ (10.2).

Für Erwachsene sagt dieselbe Quelle im selben Abschnitt das Gegenteil:
„These opiate or opiate like drugs and other inhibitors of intestinal motility
**may reduce the frequency of stool passage in adults**.“

Das ist kein vorsichtiges Weglassen, sondern eine **Tatsachenbehauptung, die die
eigene Quelle widerlegt** — und sie hätte einen Erwachsenen ohne Arzt davon
abhalten können, etwas zu nehmen, das seinen Flüssigkeitsverlust senkt. Regel 4
(„im Zweifel der konservativere Rat“) rechtfertigt das nicht: Ein falscher Fakt
wird nicht dadurch richtig, dass er vorsichtig klingt.

Der Tipp trennt jetzt ausdrücklich. Für Kinder gilt die strengere der beiden
Fassungen der Quelle — 10.2 sagt „None of these agents should be given to
**infants or children**“, nicht „unter fünf Jahren“. Für Erwachsene spricht das
Handbuch keine Empfehlung aus; genannt werden nur die belegte Wirkung und die
belegten Gefahren, darunter die zuvor fehlende: „they may prolong infection by
delaying elimination of the causative organisms“. Ergänzt wurden auÛerdem
Kohletabletten, die die Quelle namentlich führt („activated charcoal … None,
however, has proven practical value“) und die im deutschen Haushalt das
Hausmittel Nummer eins sind.

Der Titel hieß „Durchfallmittel und Antibiotika: was nicht hilft“ und behauptete
damit über Antibiotika das Gegenteil des eigenen Textes — die Quelle nennt drei
Fälle, in denen sie verlässlich helfen. Jetzt rein beschreibend.

**Ebenfalls behoben:**

- Der Zink-Tipp sagte „Es ist billig und lange haltbar“. **Beides steht in
  keiner Quelle.** Das Handbuch macht zur Haltbarkeit von Zink keine Angabe; die
  einzige Haltbarkeitsangabe im ganzen Dokument betrifft reisbasiertes ORS. Und
  „affordable“ steht dort als Auswahlkriterium, nicht als Preisaussage. Gestrichen
  — die Behauptung hätte auch dazu geführt, dass niemand das Verfallsdatum prüft.
- Der Zink-Tipp empfahl einen Vorrat, ohne zu sagen **was**. Was in deutschen
  Haushalten liegt, sind Erwachsenentabletten mit meist 25 oder 50 mg; die
  WHO-Angabe sind 10–20 mg **elementares** Zink. Jetzt steht ausdrücklich
  Kinderpräparat da, dazu, dass die Quelle keine untere Altersgrenze nennt und zu
  Säuglingen unter sechs Monaten schweigt. Der Vorratsrat ist als eigene
  Einordnung gekennzeichnet, wie beim SODIS-Tipp.
- „Schmelztablette“ war die falsche Darreichungsform: Die Quelle sagt
  „dispersible tablets“ — in Wasser auflösbar, nicht im Mund zergehend.
- Der Händewasch-Tipp hieß „auch ohne Seife“. Die Quelle sagt „requires the use
  of soap **or a local substitute**“ — das ist ein Ersatz, kein Verzicht. Wer nur
  den Titel überfliegt, hätte „bloßes Wasser reicht“ mitgenommen. Umbenannt.
- Der Essens-Tipp verlangte Seife (so steht es in 11.5) und nannte den Ersatz
  nicht, den der Nachbartipp kennt. Verweis ergänzt — jeder Tipp muss allein
  tragen.
- „wer ihn richtig entsorgt, unterbricht die Ansteckung“ war stärker als die
  Quelle („can help to interrupt“). Und der Hauptsatz fehlte: „Every family needs
  access to a clean, functioning latrine.“ Für eine Vorsorge-App ist das der
  nützlichere Teil.
- Die Zehn-Meter-Regel gilt in der Quelle den **Latrinen**; die Ausdehnung auf
  den behelfsmäßigen Platz ist jetzt als solche kenntlich.
- **Ein Widerspruch im Paket, den erst die neuen Tipps erzeugt haben:**
  `wasser-tagesbedarf` veranschlagt zwei Liter je Person und Tag und plant für
  Hygiene **nichts** ein — während die drei Hygiene-Tipps fünfmal tägliches
  Händewaschen und das Spülen allen Geschirrs verlangen. Die Quelle löst das
  selbst auf (11.3): „The amount of water available to families has as much
  impact on the incidence of diarrhoeal diseases as the quality of water.“ Der
  Satz steht jetzt im Wasser-Tipp.
- Suchlücken geschlossen: `imodium`, `loperamid`, `kohletabletten`, `hausmittel`,
  `blutiger durchfall`, `klo`, `obst`, `abstand`, `ansteckend` fanden **gar
  nichts**. `brunnen` führte zu zwei Gas-Tipps und zu nichts über Wasserqualität;
  jetzt steht die Zehn-Meter-Regel an erster Stelle.

**Eine Entscheidung, die ich bewusst anders getroffen habe als vorgeschlagen:**
Der Medikamenten-Tipp steht bei der blanken Anfrage `durchfall` weit hinten, weil
sein Titelwort „Durchfallmittel“ den Genauigkeits-Bonus nicht bekommt. Ihn nach
vorn zu holen hätte einen der drei Kern-Tipps (erkennen, trinken, Hilfe holen)
verdrängt. Wer nach einem Medikament sucht, tippt den Namen des Medikaments — und
alle diese Wörter führen jetzt direkt hin; ein Test hält das fest.

**Beim Einbau gemessen und korrigiert:** Die beiden neuen Titel begannen mit
„Durchfall“ und schoben bei Punktgleichstand den Behandlungs-Tipp auf Platz vier
— wer drei Treffer sieht, hätte die Behandlung nicht mehr gesehen. Umbenannt in
„Zink bei Durchfall – für Kinder“ und „Durchfallmittel und Antibiotika: was nicht
hilft“. Dabei aufgefallen: Die Suche **summiert die Feldgewichte nicht** — pro
Wort zählt nur das höchste Feld, danach entscheidet der Titel alphabetisch. Wer
künftig Reihenfolgen beeinflussen will, muss am Titel ansetzen, nicht an den
Schlagwörtern. Ebenfalls entfernt: das Schlagwort `milch` beim Trinken-Tipp —
der Tipp behandelt Milch gar nicht.

**Einschränkung, die in den Tipps steht:** Das Handbuch richtet sich an
medizinisches Personal und ist überwiegend auf Kinder geschrieben. Erwachsene
kommen nur an wenigen Stellen ausdrücklich vor — die Mengenangabe „ältere Kinder
und Erwachsene so viel, wie sie mögen“ ist eine davon, ebenso die Aussage, dass
Austrocknung „at any age“ behandelbar ist. Der Warnzeichen-Tipp sagt
ausdrücklich, dass die Liste für Kinder aufgestellt ist.

**Die heikelste Stelle: das selbst angesetzte Rezept.** Die WHO schreibt:

> „A home-made solution containing 3g/l table salt (one level teaspoonful) and
> 18g/l of common sugar (sucrose) is effective but is not generally recommended
> because the recipe is often forgotten, the ingredients may not be available or
> too little may be given."

Also: **wirksam**, aber **nicht allgemein empfohlen** — und die drei genannten
Gründe sind allesamt praktischer Natur, keiner betrifft die Sicherheit des
Rezepts selbst. Einen der drei („das Rezept wird oft vergessen“) beseitigt eine
Offline-App genau dadurch, dass sie das Rezept dabeihat; die anderen beiden
bleiben und stehen deshalb im Tipp.

Aufgenommen wurde es trotzdem, weil die Alternative in einer Lage ohne Apotheke
nichts ist. Regel 4 verlangt im Zweifel den konservativeren Rat — einem
Austrocknenden eine belegte, wirksame Lösung vorzuenthalten ist nicht der
konservativere Rat, sondern der schlechtere. Der Vorrang der fertigen
ORS-Lösung steht ausdrücklich davor und ist durch einen Test gesichert, ebenso
die drei Mengenangaben.

**Das Teelöffel-Maß — der gefährlichste Einzelfehler dieses Pakets, behoben am
28.07.2026.** Das Handbuch von 2005 setzt 3 Gramm Kochsalz mit „one level
teaspoonful“ gleich. Ein gestrichener Teelöffel Salz wiegt deutlich mehr, und
die WHO selbst gibt in ihren Anleitungen für die Allgemeinheit inzwischen einen
**halben** Teelöffel je Liter an:

- „Information for the general public on oral rehydration solution“, Dokument
  WHO-EM/CSR/594/E: https://applications.emro.who.int/docs/WHOEMCSR594E-eng.pdf
  — wörtlich: „You can also make it at home by mixing the following: 1. Half a
  teaspoon of salt 2. Six teaspoons of sugar 3. One litre of clean and safe
  drinking-water or lightly salted rice water“ (PDF geladen, Volltext gelesen)
- „Cholera outbreaks“, Fragen und Antworten, Stand 10.03.2023:
  https://www.who.int/news-room/questions-and-answers/item/cholera-outbreaks
  — „mixing 1 litre safe water, 6 teaspoons sugar and half a teaspoon of salt“

Der Tipp hatte das **ganze** Teelöffel-Maß aus dem Handbuch übernommen und hätte
damit ohne Waage auf etwa die doppelte Salzmenge geführt — also genau in die
Richtung, vor der dasselbe Handbuch warnt (Abschnitt 4.5.1: zu salzige oder zu
süße Getränke ziehen Wasser aus dem Gewebe in den Darm, „their most serious
problem is convulsions“). Und ohne Waage ist in dieser Lage fast jeder.

Jetzt führt der Tipp das Haushaltsmaß der WHO voran — halber Teelöffel Salz,
sechs Teelöffel Zucker, ein Liter —, sagt ausdrücklich „nicht ein ganzer“ und
nennt die Gefahr beim Namen. Die Gramm-Angaben stehen als Zusatz für alle mit
Waage. Mehrere Tests sichern das ab.

**Was weiterhin offen ist:** Für den Zucker gibt es kein einheitliches
Haushaltsmaß — das Handbuch nennt 18 Gramm je Liter, die Anleitung für die
Allgemeinheit sechs Teelöffel. Beide Angaben stehen im Tipp und sind beide von
der WHO; welche vorgehen soll, ist noch zu entscheiden und steht in der
Roadmap.

**Ebenfalls ergänzt:** Der selbst angesetzten Lösung fehlt das Kalium, das die
fertige ORS-Lösung enthält (Anhang 2: 1,5 g/l Kaliumchlorid, „preventing serious
hypokalaemia“). Die Quelle nennt den Ersatz, und er braucht keine Apotheke:
„Foods rich in potassium, such as bananas, green coconut water and fresh fruit
juice are beneficial.“ Bananen stehen jetzt im Tipp. Dazu der Verweis auf
`wasser-abkochen` — in dieser Lage ist das Wasser der wahrscheinliche Auslöser
des Durchfalls, und die neun Wasser-Tipps und die fünf neuen kannten einander
vorher in keine Richtung.

**Drei weitere Befunde, alle behoben:**

- **Die Grenze des Trinkens fehlte.** Der Erkennen-Tipp beschrieb die schwere
  Austrocknung und endete mit „Dann folgt der Tod rasch, wenn nicht sofort
  Flüssigkeit ersetzt wird“ — die einzige Handlung im ganzen Fünferpaket war
  trinken. Die Quelle nimmt die schwere Austrocknung aber ausdrücklich aus:
  „dehydration … **except when it is severe** … can be safely and effectively
  treated … by … oral rehydration“. Und jemandem, der nicht klar bei Bewusstsein
  ist, darf man nichts zu trinken geben — eine Regel, die das Paket bei
  `erste-hilfe-hitzschlag-handeln` bereits führt und hier fehlte. Ergänzt,
  mitsamt Notruf und Seitenlage.
- **Was nach Erbrechen zu tun ist, fehlte vollständig** — die schmerzlichste
  Auslassung der ganzen Quelle. Wörtlich (Abschnitt 4.3.2): „If the child
  vomits, wait 5-10 minutes and then start giving ORS solution again, but more
  slowly (e.g. a spoonful every 2-3 minutes)“ und „Vomiting often occurs during
  the first hour or two … but this rarely prevents successful oral rehydration
  since most of the fluid is absorbed.“ Ohne diesen Satz hört der Helfer auf,
  sobald es wieder hochkommt — genau dann, wenn es am nötigsten wäre. Ergänzt,
  dazu ein Verweis aus `erste-hilfe-erbrechen-helfen`, der zum Durchfall bisher
  schwieg.
- **„weil sie schneller austrocknen“ war erfunden.** Das Handbuch sagt das
  nirgends; der Flüssigkeitsverlust wird für alle gleich in Prozent des
  Körpergewichts eingeteilt, und die Behandelbarkeit gilt „at any age“.
  Schlimmer war die Wirkung: Ein Erwachsener las daraus „gilt für mich weniger“.
  Die Quelle sagt das Gegenteil und nennt schwere Austrocknung durch wässrigen
  Durchfall bei Erwachsenen ausdrücklich. Ersetzt.

**Und ein schlichter Schreibfehler**, der zeigt, wie nötig das Gegenlesen ist:
Aus „unsalted soup“ war „ungesäuerte Suppe ohne Salz“ geworden — ein Wort, das
es so nicht gibt, und die Aussage doppelt gemoppelt. Jetzt „ungesalzene Suppe“.
Aus derselben Liste fehlte der **ungesalzene** Joghurtdrink; er steht jetzt
dabei.

**Ein Widerspruch, der nur im Paket entsteht:** Ein Tipp warnt vor gezuckerten
Getränken, der nächste schreibt 18 Gramm Zucker ins Rezept. Beide stammen aus
derselben Quelle und sind einzeln richtig. Gemessen liefert die Anfrage `zucker`
beide. Ohne einen Satz dazu liest sich das als Widerspruch — und im Zweifel
lässt jemand den Zucker weg, der zur Wirkung gehört. Das Rezept grenzt sich
deshalb ausdrücklich ab; ein Test hält das fest. **Keine Begründung über den
Wirkmechanismus** wurde ergänzt — die steht so nicht in der Quelle.

**Korrektur vom 28.07.2026:** Der vorstehende Satz war falsch. Anhang 2 des
Handbuchs liefert den Wirkmechanismus wörtlich: „Glucose is essential because,
when it is absorbed, it promotes the absorption of sodium and water in the small
intestine. This is true irrespective of the cause of the diarrhoea. Without
glucose, ORS solution would be ineffective.“ Der Tipp führt diesen Grund jetzt
an — er ist stärker als jede Behauptung, denn er sagt dem Leser genau, warum er
den Zucker nicht weglassen darf.

### Deutscher Feuerwehrverband (DFV)

- „Erste-Hilfe kompakt — Notfallstichwort: Rauchgasintoxikation", Empfehlungen
  des Bundesfeuerwehrarztes, Folge XXII, Hamburg/Berlin, Oktober 2012,
  Bundesfeuerwehrarzt Dr. med. Hans-Richard Paschen:
  https://www.feuerwehrverband.de/app/uploads/2020/05/DFV_Erste_Hilfe_kompakt_Rauchgasintoxikation.pdf
  (abgerufen am 28.07.2026)
- „Erste-Hilfe kompakt — Erste-Hilfe bei Brandgasinhalation", Empfehlungen des
  Bundesfeuerwehrarztes, Folge XXXI, Hannover/Berlin, April 2015, erstellt durch
  H. A. Adams, V. Hubrich und H. Desel:
  https://www.feuerwehrverband.de/app/uploads/2020/05/DFV_Erste_Hilfe_kompakt_Brandgasinhalation.pdf
  (abgerufen am 28.07.2026)

- „Erste-Hilfe kompakt — Notfallstichwort: Vergiftung (med. Intoxikation)",
  Empfehlungen des Bundesfeuerwehrarztes, Folge XXV, Hamburg/Berlin, Februar
  2013, Bundesfeuerwehrarzt Dr. med. Hans-Richard Paschen:
  https://www.feuerwehrverband.de/app/uploads/2020/05/DFV_Erste_Hilfe_kompakt_Vergiftung.pdf
  (abgerufen am 28.07.2026)

Verwendet für: `erste-hilfe-brandrauch-nicht-hineingehen`,
`erste-hilfe-brandrauch-sofort-raus`, `erste-hilfe-rauchvergiftung-erkennen`,
`erste-hilfe-rauchvergiftung-helfen`, `erste-hilfe-kohlenmonoxid-erkennen`,
`erste-hilfe-kohlenmonoxid-handeln` sowie für den ergänzten Ausnahme-Satz in
`erste-hilfe-eigenschutz` und die Abgrenzung in `erste-hilfe-kohlendioxid`.
Alle drei PDFs wurden heruntergeladen und der Text lokal herausgezogen und
gelesen — nicht über eine Zusammenfassung.

**Nachtrag nach dem Prüfdurchgang vom 28.07.2026 — der schwerste eigene Fehler
dieser Reihe.** In beiden Kohlenmonoxid-Tipps stand: „Kohlenmonoxid ist leichter
als Luft. Es sammelt sich also nicht wie Kohlendioxid am Boden." Der erste Satz
steht wörtlich im Rauchgas-Blatt. Der zweite war meine Schlussfolgerung, und sie
ist gestrichen.

Der Grund: Kohlenmonoxid ist nur rund drei Prozent leichter als Luft. Es
schichtet sich nicht, es durchmischt sich — es gibt **keine** sichere Stelle im
Raum. Der Satz stand direkt neben der Beschreibung eines Kohlendioxid-„Sees" am
Boden und las sich damit zwangsläufig als „unten ist es besser". Geschrieben
worden war er, um genau diesen Fehler zu verhindern — und baute ihn in der
Gegenrichtung wieder ein.

An seiner Stelle steht jetzt eine **bewusst restriktive Einordnung**, die so
nicht in der Quelle steht und hier als solche gekennzeichnet wird (wie beim
SODIS-Tipp): „Wo Kohlenmonoxid im Raum steht, kannst du nicht erkennen — verlass
dich auf keine Stelle im Raum. Sicher bist du erst draußen." Regel 4 verlangt im
Zweifel den konservativeren Rat; ein Satz, der irgendeine Stelle im Raum als
weniger gefährlich erscheinen lässt, ist das Gegenteil davon.

Ein Test hält das fest: `keinTippBehauptetWoKohlenmonoxidImRaumSteht` verbietet
jede Ortsaussage über Kohlenmonoxid, in beide Richtungen.

**Weitere Befunde desselben Durchgangs, alle behoben:**

- Der Abgrenzungssatz im Kohlendioxid-Tipp lautete „Was hier steht, gilt für es
  nicht" — eine Generalklausel, die Kohlenmonoxid ausgerechnet von „KEINEN
  eigenmächtigen Rettungsversuch" und vom Notruf freisprach. Gemeint war nur die
  Dichte. Jetzt steht dort ausdrücklich, dass beide Regeln für Kohlenmonoxid
  genauso gelten.
- „Verrauchte Räume betrittst du dabei nicht" war im CO-Tipp das einzige
  Betretungs-Kriterium. Im Umkehrschluss: kein Rauch zu sehen → ich darf hinein.
  Bei reinem Kohlenmonoxid — defekte Gastherme, Stromerzeuger, Grill in der
  Garage — sieht der Raum völlig normal aus. Ersetzt.
- „Zwei Gefahrenbereiche betrittst du dafür überhaupt nicht" machte die
  Aufzählung abschließend und erklärte alles Übrige stillschweigend für
  betretbar — ausgerechnet unter Ausschluss des unsichtbaren Falls. Die Liste
  ist geöffnet und nennt Kohlenmonoxid mit.
- „starker Husten und gereizte Augen" stand im selben Absatz wie „reizlos".
  Die Quelle nennt die Reizzeichen unter der gemeinsamen Überschrift
  „Rauchgas- und Kohlenstoffmonoxidvergiftung"; reines Kohlenmonoxid reizt
  nicht. Wer kein Husten hat, hätte Kohlenmonoxid ausgeschlossen. Getrennt.
- Die einzige Maßnahme, die die Ursache angeht, fehlte: „Öffnen Sie möglichst
  viele Fenster oder bringen Sie den Betroffenen an die frische Luft.
  Eigenschutz beachten!" (Vergiftung, S. 3). Die einzige Sauerstoff-Aussage im
  Tipp war ein Verbot. Ergänzt.
- Gemessen: Der beschreibende CO-Tipp stand bei **jedem** naheliegenden
  Stichwort oben (`co`, `ofen`, `grill`, `heizung`, `generator`, `holzkohle`,
  `abgase`, `notstrom`, `gasherd`, `kopfschmerzen`, `schwindel`), weil beide
  CO-Tipps punktgleich liegen und der alphabetische Ausgleich „u" vor „w"
  stellt. Er enthielt weder Notruf noch Eigenschutz. Jetzt beginnt er mit
  „sofort raus an die frische Luft und Notruf 112".
- Der Titel „Brandrauch: sofort raus, nichts mitnehmen" versprach eine
  Anweisung, die weder im Text noch in einer Quelle stand — und der Titel wiegt
  in der Suche am schwersten. Gekürzt; das belegte Zurücklaufen-Verbot steht
  jetzt im Text.
- Die verzögerte Wirkung stand nur im Nachbartipp. Sie ist in den
  Rauchvergiftungs-Erkennen-Tipp übernommen, denn im Ernstfall wird nur ein
  Tipp gelesen.
- Die Blackout-Wörter `stromausfall`, `notheizung`, `kamin`, `gastherme`,
  `kocher`, `gaskocher`, `stromerzeuger`, `auspuff` fanden gar nichts — also
  genau der Anlassfall der App. Als Schlagwörter ergänzt und im Test
  festgehalten. **`petroleum` fehlt weiterhin bewusst:** Petroleumöfen stehen in
  keiner der geprüften Quellen.

**Erledigt am 28.07.2026 — aber nicht so, wie geplant.** Zu **CO-Warnmeldern**
und zum **Verhalten bei CO-Alarm** sagen alle drei „Erste-Hilfe kompakt"-Ausgaben
nichts; sie sprechen ausschließlich von Rauchmeldern. Die Vergiftungs-Ausgabe
verweist dafür auf eine eigene Fachempfehlung des DFV vom Mai 2012. Die ist
nachgeschlagen und im Volltext gelesen — sie trägt zum Thema Warnmelder aber
nichts bei, weil sie sich an Einsatzkräfte richtet. Einzelheiten und die
stattdessen herangezogenen Quellen im Abschnitt „Kohlenmonoxid-Warnmelder"
weiter unten.

Diese Quelle schließt die Lücke, die beim Vergiftungs-Durchgang entstanden war:
Das Wort „Rauch" musste aus `erste-hilfe-vergiftung-atemwege` entfernt werden,
weil es dort selbst hinzugefügt war und den Tipp zum einzigen Treffer für `rauch`
machte — mit der Aufforderung, in einen brennenden Raum zu gehen. Jetzt steht das
Gegenteil belegt da: **„Betreten Sie keine verrauchten Räume ohne
umluftunabhängigen Atemschutz!"**

Die Ausgabe „Vergiftung" nennt als Ursachen für Kohlenmonoxid ausdrücklich
„Brände, defekte Öfen oder Gasherde, Betrieb von Holzkohlegrills oder
gasbetriebenen Heizgeräten in geschlossenen Räumen" sowie das Einatmen von
Abgasen — also genau die Handlungen, zu denen ein längerer Stromausfall verleitet.

**Bewusst nicht übernommen aus derselben Ausgabe:** Sie rät, bei geschlucktem
Gift „in kleinen Schlucken etwas zum Trinken" zu geben, um das Gift zu
verdünnen, und keine Milch. Die DRK-Anleitung, aus der die übrigen
Vergiftungs-Tipps stammen, sagt zum Trinken nichts. Nur die Verbote zu
übernehmen und den positiven Rat wegzulassen würde die Aussage der Quelle
verdrehen, und das Blatt ist von 2013. Offener Punkt in der Roadmap.

**Ein Widerspruch zu einer anderen Quelle im Paket, bewusst nicht selbst
entschieden:** Die Ausgabe zur Brandgasinhalation schreibt für den
Kreislaufstillstand nach Rauchgas-Einatmung, der Patient sei „zunächst zwei Mal
zu beatmen und erst danach mit der Herzdruckmassage zu beginnen", weil der
Stillstand hier durch Erstickung entstanden ist. Der Tipp
`erste-hilfe-nur-druecken` sagt für ungeübte Helfer bei Erwachsenen das
Gegenteil — belegt aus den **GRC-Leitlinien 2025**, also der aktuellen
Primärquelle für Wiederbelebung in Deutschland. Das DFV-Blatt ist von 2015 und
stützt sich auf Literatur von 2010/2011.

Eine neuere Primärquelle durch eine ältere Sekundärquelle zu ersetzen wäre
falsch herum, deshalb ist an der Wiederbelebung **nichts geändert** worden. Die
Frage dahinter ist aber echt — bei Erstickungs-bedingtem Stillstand zählt
Sauerstoff, und das Paket kennt diese Ausnahme für Kinder bereits. Ob sie auch
für Erwachsene nach Rauchgas gelten soll, gehört in den aktuellen GRC-Text
nachgeschlagen und ist ausdrücklich zu entscheiden, nicht in einer Nachtschicht.

Ebenfalls offen, und für dieses Projekt die schmerzhafteste Lücke:
**Wundversorgung ohne erreichbaren Arzt.** Alle deutschen Laienquellen enden
bei „schnellstmöglich zum Arzt"; für tagelange Isolation gibt es dort keine
Anleitung. Die **Abbindung durch Laien** stand hier ebenfalls als offen
(„Beleglage für eine potenziell verstümmelnde Maßnahme zu dünn") und ist seit
dem 28.07.2026 geklärt: Die ERC-Leitlinie Erste Hilfe 2025 verlangt sie
ausdrücklich — siehe den eigenen Quellenabschnitt weiter oben. Offen bleibt dort
nur, wie sich ein Tourniquet **behelfsmäßig** herstellen lässt; dazu schweigt
die Leitlinie. Ersticken stand hier ebenfalls und ist inzwischen abgedeckt.

Der Hinweis, dass der Notruf bei Stromausfall ausfallen kann, ist **nicht**
übernommen. Beim eigenen Nachprüfen am 28.07.2026 ließ sich die Aussage an den
angegebenen Stellen nicht wiederfinden: Die BBK-Übersichtsseite zum Stromausfall
sagt nur, Internet und Mobilfunk seien „gestört", und die zitierte
Bürgerinformation als PDF war über die angegebene Adresse nicht im Volltext
abrufbar. Solange die Fundstelle nicht selbst gelesen ist, kommt der Hinweis
nicht ins Paket — auch wenn er plausibel klingt.

### Kohlenmonoxid-Warnmelder

Bearbeitet am 28.07.2026. Ergebnis: die Tipps
`erste-hilfe-kohlenmonoxid-melder` und `erste-hilfe-kohlenmonoxid-alarm`.

**Die benannte Quelle trägt das Thema nicht.** Ausgangspunkt war der Verweis der
Vergiftungs-Ausgabe auf eine eigene DFV-Fachempfehlung. Die alte Adresse
`feuerwehrverband.de/fe-co-notfall.html` läuft ins Leere (HTTP 404); das Dokument
liegt heute unter
`https://www.feuerwehrverband.de/app/uploads/2020/05/DFV-Fachempfehlung_Einsatzstrategien_CO-Notfall.pdf`
und ist heruntergeladen, in Text umgewandelt und vollständig gelesen worden:

- „Fachempfehlung Nr. 04/2012 vom 15. Mai 2012 — Rahmenempfehlung zu Einsätzen
  bei Verdacht auf einen CO-Notfall innerhalb von Räumlichkeiten", erstellt von
  Leitendem Branddirektor Ulrich Tittelbach in Abstimmung mit dem Fachbereich
  Einsatz, Löschmittel und Umweltschutz sowie Bundesfeuerwehrarzt
  Dr. Hans-R. Paschen, 5 Seiten (abgerufen am 28.07.2026)

Sie richtet sich ausdrücklich an Feuerwehr und Rettungsdienst und handelt von
Messgeräten, umluftunabhängigem Atemschutz, Explosionsschutz, Notrufabfrage und
Einsatztaktik. **Zu Warnmeldern in Wohnungen steht dort kein Wort.** Übernommen
sind aus ihr deshalb nur drei Punkte, die auch ohne Ausrüstung gelten:

- Nur eine Messung kann eine gefährliche Konzentration ausschließen oder
  bestätigen (`…-alarm`).
- Querlüftung muss „schnellstens sichergestellt und nachhaltig betrieben" werden,
  und Lüftungsmaßnahmen sind „längerfristig aufrecht zu erhalten, da CO aus
  (Wohnungs-)Einrichtungen, Wandverkleidungen, Textilien usw. ausdiffundiert"
  (`…-alarm`, zweiter Zweig).
- Ein verdächtiges Heizgerät, ein Boiler oder ein Ofen ist außer Betrieb zu
  nehmen; vor der Wiederinbetriebnahme sind Heizungsbauer und Schornsteinfeger
  hinzuzuziehen (`…-alarm`).

Die Druckkammer-Angabe („idealerweise innerhalb von vier Stunden beginnen,
danach verschlechtert sich die Prognose") steht ebenfalls dort und deckt sich mit
der Darstellung der Initiative.

**Was das Thema stattdessen trägt.** Drei Meldungen des DFV und die Webseiten der
Initiative, der der DFV selbst angehört. Alle Seiten wurden geladen, im
Volltext gelesen und liegen unter `work/co/` (nicht versioniert):

- DFV, „Feuerwehr warnt vor Gefahr durch Kohlenmonoxid", 31.01.2017 —
  Handlungshinweise: nie in geschlossenen Räumen grillen, „dies gilt auch für das
  Abkühlen der Kohle"; das verursachende Gerät nur ausschalten oder entfernen,
  wenn es ohne Eigengefährdung geht; Notruf 112.
- DFV, „CO-Vergiftungsgefahr mit Langzeitschäden durch Kamine und Öfen",
  26.10.2021 — Montageorte, „CO-Melder sind kein Ersatz für Rauchwarnmelder", und
  die sechs Punkte für den Alarmfall.
- DFV, „Feuerwehrverband klärt auf: Rechtzeitige Warnung vor
  Kohlenmonoxid-Vergiftungen bieten nur CO-Melder", 06.05.2022 — Grills und
  Stromaggregate können „selbst bei geöffneten Fenstern, Türen oder dem
  Garagentor zum Tode führen"; auch der Balkon ist gefährlich, weil das Gas in
  die Wohnung ziehen kann; Aggregate gehören ausschließlich ins Freie,
  unabhängig vom Kraftstoff.
- DFV, „Guter Vorsatz mit lebensrettender Wirkung: Zum Jahresbeginn 2026
  CO-Melder installieren", 06.01.2026 — auch bei gewarteter Anlage keine
  hundertprozentige Sicherheit; Wandmontage in Atemhöhe statt an der Decke.
- Initiative zur Prävention von Kohlenmonoxid-Vergiftungen, „Schutz vor
  Kohlenmonoxid", „Kohlenmonoxid – Notfall" und „Kohlenmonoxid – Ursachen",
  `co-macht-ko.de` — Kaufkriterium DIN EN 50291 Teil 1 (Teil 2 für Wohnwagen und
  Boot), die Montagemaße, die Alarmschwellen, die Abgrenzung Melder/CO-Warner und
  die Reihenfolge im Alarmfall.

**Interessenlage, offengelegt:** Zu den Mitgliedern der Initiative gehören neben
DFV, vfdb, dem Schornsteinfegerhandwerk (ZIV) und zwei ärztlichen
Rettungsdienst-Verbänden auch **Hersteller von Kohlenmonoxid-Meldern**. Das ist
der Grund, warum der Melder-Tipp die Aussage „nur ein Melder warnt rechtzeitig"
mit den DFV-Meldungen belegt und die herstellernahe Quelle nur für Zahlen
heranzieht, die auf eine Norm zurückgehen.

**Was ausdrücklich NICHT übernommen ist:**

- Die **Norm DIN EN 50291-1 selbst** lag nicht vor. Die Alarmschwellen (30, 50,
  100, 300 ppm mit ihren Zeiten) sind der Aufbereitung der Initiative entnommen,
  die sie als normativ festgelegt bezeichnet. Der Tipp sagt das in einem eigenen
  Vorbehalts-Absatz und stellt die Bedienungsanleitung des gekauften Geräts über
  jede Angabe im Paket. Muster dafür ist `erste-hilfe-borreliose-behandeln`.
- Die 2017er-Meldung nennt die Wartung der Heizungsanlage „ein Mal pro Jahr
  Pflicht", die 2025er-Meldung spricht von „vorgeschriebenen Überprüfungen der
  Feuerstätten im Drei-Jahres-Intervall". Das sind verschiedene Vorgänge und
  landesrechtlich verschieden geregelt. **Es steht deshalb kein Intervall im
  Paket** — nur, dass regelmäßig geprüft und gewartet werden muss.
- Die **Langzeitfolgen-Zahlen** aus der 2021er-Meldung (Herzinfarkt bei zehn
  Prozent innerhalb von 56 Monaten, Langzeitsterblichkeit 8,4 gegen 1,6 Prozent)
  sind nicht übernommen. Sie stammen aus einer Pressemitteilung ohne Angabe der
  Studie, und sie ändern keine Handlung.
- Die 2025er-Meldung nennt 830 000 Mängel und rund 90 000 sicherheitstechnisch
  problematische Feuerstätten für 2024. Ebenfalls nicht übernommen: eine Zahl,
  die niemandem sagt, was zu tun ist.

**Ein Widerspruch in der Reihenfolge, bewusst aufgelöst.** Die DFV-Meldung von
2021 listet „Öffnen Sie Türen und Fenster" vor „Verlassen Sie umgehend das
Gebäude"; die Webseite der Initiative, aus der die Liste stammt, listet es
umgekehrt. Der Tipp folgt der Reihenfolge der Initiative — das Hinausgehen zuerst
—, weil sie die konservativere ist: Wer zuerst Fenster öffnet, bleibt im Raum.

**Zwei Punkte, die aus der Umkehrung der Quellen kommen und im Text als solche
kenntlich sind.** Erstens: Ein Melder, der schweigt, sagt nicht, dass kein
Kohlenmonoxid da ist — er sagt, dass seine Schwelle noch nicht erreicht ist. Das
folgt unmittelbar aus der Schwellentabelle und steht im Tipp mit dem Vorspann
„Was daraus folgt". Zweitens, und gefährlicher: Die Initiative schreibt, der
Melder setze sich automatisch zurück, sobald die Konzentration durch Lüften
sinkt. Ohne einen Satz dazu wäre „der Alarm ist aus" für viele der Anlass,
zurückzugehen. Der Alarm-Tipp sagt deshalb ausdrücklich, dass ein verstummter
Melder nichts über die Ursache aussagt. Ein Test hält beides fest:
`dieBeidenMelderTippsLassenKeineFalscheSicherheitZu`.

**Ortsaussagen.** Der Melder-Tipp nennt Montagehöhen — das ist eine Aussage über
das Gerät, nicht über das Gas. Damit daraus kein Rückschluss wird, steht direkt
dahinter, was die Initiative selbst schreibt: Kohlenmonoxid hat ungefähr dieselbe
Dichte wie Luft, verteilt sich anfangs in Höhe der Quelle und bewegt sich danach
„frei und nicht vorhersehbar im Raum". Der Satz „eine sichere Stelle im Raum gibt
es nicht" bleibt damit unangetastet, und der bestehende Test
`keinTippBehauptetWoKohlenmonoxidImRaumSteht` greift weiterhin.

**Gemessene Suche.** Vor dem Schreiben wurde der Ist-Zustand von 89 Wörtern
festgehalten, danach derselbe Satz erneut gemessen. Kein Notfall-Tipp hat seinen
ersten Platz verloren. Der Grund ist die Titelwahl: Beide neuen Titel beginnen
mit „W" und verlieren damit jeden Gleichstand gegen „Kohlenmonoxid: …" (`K`),
„Brandrauch: sofort raus" (`B`) und „Rauch im Haus: …" (`R`). Drei Fassungen sind
daran gescheitert und wurden verworfen:

- „Kohlenmonoxid-Melder: …" hätte bei `kohlenmonoxid` gewonnen, weil „-" vor „:"
  sortiert — und damit den Tipp verdrängt, der mit „sofort raus … Notruf 112"
  beginnt.
- „… die einzige rechtzeitige Warnung" hätte `warnung` gewonnen und den
  Haftungshinweis verdrängt.
- Ein Titel mit „CO-Melder" statt „Warnmelder" hätte `co` gewonnen, weil „CO" im
  Titel 6 Punkte holt, als Schlagwort nur 4.

Neu belegt sind `melder`, `warnmelder`, `alarm`, `batterie`, `evakuieren`,
`nachbarn`, `wohnwagen`, `pelletlager` und `en 50291` — vorher fand keines davon
etwas. Festgeschrieben in `dieCoMelderTippsVerdraengenKeinenNotfallTipp`.

### Bewusste Abweichungen von der Formulierung der Quelle

- `erste-hilfe-seitenlage-handgriffe` schreibt „Handrücken an die Wange", das DRK
  schreibt „Handoberfläche". Gemeint ist dasselbe; die hier gewählte Formulierung
  ist eindeutiger.
- `wasser-solare-entkeimung-sodis` enthält einen ausdrücklich als solche
  gekennzeichneten Einordnungssatz zu Mitteleuropa. Die WHO beschreibt das
  Verfahren für sonnenreiche, äquatornahe Regionen; die Übertragbarkeit auf
  Mitteleuropa ist eine eigene, konservative Bewertung und keine Quellenaussage.
  Sie steht deshalb im Text als Einordnung gekennzeichnet.

### Eine Beobachtung zum Datenmodell

Tipp-Texte sind reiner Fließtext; Zeilenumbrüche sind nicht erlaubt (sie zählen
als Steuerzeichen und machen das Feld unlesbar). Eine Schritt-für-Schritt-Anleitung
wie die stabile Seitenlage ist deshalb auf zwei Tipps aufgeteilt — einen für
Zweck und Voraussetzung, einen für die Handgriffe.

Sauberer wäre die Struktur von `guides.json` mit echten `steps`. Dem stand
entgegen, dass eine Bauanleitung dort eine Materialliste erzwang (mindestens ein
Eintrag), und eine Erste-Hilfe-Maßnahme braucht kein Material. **Entschieden am
28.07.2026 und umgesetzt: `materials` ist optional.** Damit können die
mehrteiligen Abläufe — stabile Seitenlage, Wiederbelebung, Ersticken — als
Anleitungen mit echten Schritten geführt werden statt als aufgeteilter
Fließtext. Umgestellt ist noch keiner; die Tipps stehen unverändert.

### Arbeitsweise: was fünf Prüfdurchgänge gezeigt haben

Diese Kategorie ist am 28.07.2026 fünfmal unabhängig gegen ihre Quellen geprüft
worden. Das Ergebnis ist eindeutig genug, um es festzuhalten:

**An den Übernahmen aus den Quellen wurde praktisch nichts Falsches gefunden.**
Zahlen, Schrittfolgen, Bedingungen, Verbote — alles stimmte. Falsch war fast
ausnahmslos, was beim Schreiben *hinzugefügt* wurde: erklärende Merksätze,
Beispiele, Begründungen, Einordnungen. Drei davon waren ernst:

- „Wer stark schwitzt, hat noch keinen Hitzschlag" — erfunden und falsch herum.
  Beim Anstrengungshitzschlag wird weiter geschwitzt; der Satz gab eine Erlaubnis
  zu warten, genau im Zeitfenster, in dem Kühlen noch rettet.
- „Als Druckpolster taugt alles Feste" — beschreibt einen Stein, ein Feuerzeug,
  ein Handy. In einer Überlebenslage die nächstliegenden Gegenstände.
- „Herausziehen würde die Blutung erst öffnen" — eine Begründung für ein
  *unbedingtes* Verbot, die nur einen Teil der Fälle abdeckt. Wer keine Blutung
  sieht, dreht sie um.

Daraus die Regel für künftige Ergänzungen: **kein Satz ohne Beleg.** Auch keine
Begründung, kein Beispiel, keine Faustregel — auch dann nicht, wenn es „doch
jeder weiß". Wo eine Einordnung nötig ist, wird sie im Text als solche
gekennzeichnet (wie beim SODIS-Tipp) und nicht der Quelle zugeschrieben.

**Ein drittes Muster, sichtbar geworden bei den Ersticken-Tipps:** Was beim
Aufteilen verloren geht. Quellen sind oft durchlaufende Abläufe („Fall 1 bis
5"); im Paket ist jeder Tipp **einzeln durchsuchbar**. Wer im Notfall ein
Stichwort sucht, landet auf einem Tipp, nicht auf der Kette. Damit fehlen genau
die Übergänge, die in der Quelle selbstverständlich sind — wann von einer
Maßnahme zur nächsten gewechselt wird, und ob der Notruf schon gewählt ist.
**Jeder Tipp muss für sich allein tragen.** Das ist eine Anforderung, die die
Quelle nicht kennt; sie muss beim Übertragen mitgedacht werden.

Dazu gehört auch die Frage nach dem Geltungsbereich: Eine Anleitung, die
stillschweigend nur Erwachsene meint, wird im Paket zu einem Tipp, den jemand
auf ein Kind anwendet. Wo die Quelle einen Geltungsbereich voraussetzt, muss der
Tipp ihn aussprechen.

Ein zweites Muster ist erst beim Prüfen sichtbar geworden: Quellen widersprechen
sich **im Paket**, obwohl jede für sich richtig ist. Beim DRK stehen „Wunden
nicht berühren" und „notfalls in die Wunde drücken" auf getrennten Seiten mit
eigenem Zusammenhang. In einer durchsuchbaren Tippliste prallen sie aufeinander,
und niemand kann wissen, welche Regel vorgeht. Solche Stellen brauchen einen
ausdrücklichen Vorrang-Satz im Tipp — belegt, nicht selbst entschieden.

**Ein viertes Muster, gefunden beim Vergiftungs-Durchgang: die Suche ist Teil
des Inhalts.** Welcher Tipp im Ernstfall gelesen wird, entscheidet nicht der
Verfasser, sondern die Trefferliste. Vier Funde, alle an der echten Suche über
das echte Paket gemessen:

- Die Anfrage `gas` lieferte zwei Tipps mit **gleicher Punktzahl**, die
  Gegenteiliges sagen: „so schnell wie möglich herausholen" und „keinesfalls
  hineingehen". Kein Tipp nannte das Unterscheidungsmerkmal. Beide tragen es
  jetzt.
- `kohlenmonoxid` fand nichts, aber `kohlen` führte zum Kohlendioxid-Tipp — und
  dessen Aussagen (schwerer als Luft, sammelt sich am Boden) gelten für
  Kohlenmonoxid **nicht**. Wer danach gebückt in einen Raum geht, um „unter dem
  Gas zu bleiben", ist ungeschützt. Der Tipp grenzt die beiden Gase jetzt
  ausdrücklich voneinander ab, solange die Lücke besteht.
- `rauch` hatte genau einen Treffer: „Vergiftung über die Atemwege" mit dem Satz
  „hol die Person heraus". Das Wort *Rauch* stand nicht in der Quelle, es war
  hinzugefügt — und schickte damit einen Laien in einen brennenden Raum. Wort
  entfernt.
- `beatmen` zeigte bei Punktgleichstand zuerst „Kontaktgifte: nicht beatmen",
  einen Vergiftungs-Tipp, mitten in einer Wiederbelebung. Der Titel heißt jetzt
  nach dem Stoff statt nach dem Verbot; die Anweisung steht unverändert im Text.

Daraus die Regel: **Ein neuer Tipp ist erst fertig, wenn gemessen ist, worauf
die naheliegenden Stichwörter führen** — die eigenen und die der Nachbartipps.
Ein Titel ist keine Überschrift, sondern das am stärksten gewichtete Suchfeld
(Titel 5, Schlagwort 3, Text 1). Eine Verneinung im Titel wird zur Antwort auf
die Frage, die sie verneint.

Die Funde stehen als Tests in
[`ZusammenspielTest.kt`](../../core/content/src/jvmTest/kotlin/org/compasszero/content/ZusammenspielTest.kt);
sie prüfen nicht Formulierungen, sondern die Gefahr dahinter. Zwei davon haben
sofort weitere Stellen gefunden, nach denen niemand gesucht hatte: „Prüfen, ob
die Person reagiert" und „Knochenbruch versorgen" forderten den Notruf, ohne die
Nummer zu nennen.

### Was das Datenmodell erzwingt

`SourceRef.detail` war bis zum 28.07.2026 eine Kann-Angabe: geprüft wurde nur,
dass mindestens eine Quelle da ist und der Name lesbar. „Dokument und
Abrufdatum" war damit Disziplin, keine erzwungene Zusage — und genau solche
Angaben verrotten still. **Entschieden am 28.07.2026 und umgesetzt:
`detail` ist Pflicht.** Ein Paket, dessen Quelle das Feld nicht trägt, lädt gar
nicht mehr; ein leeres `detail` endet als `source-detail-missing`. Alle 89
Tipps trugen die Angabe bereits, geprüft vor dem Umstellen — der Bestand musste
nicht angefasst werden.

### Offene Frage

Soll die Kategorie einen einleitenden Hinweis tragen, dass sie keinen
Erste-Hilfe-Kurs ersetzt? Das wäre keine Tatsachenbehauptung aus einer Quelle,
sondern eine Einordnung — deshalb hier nicht eigenmächtig ergänzt.

## Kategorie: Nahrung

Recherchiert am 28.07.2026. Erster Teil des Bereichs „Jagen, Fischen, Tiere
ausnehmen und zubereiten". Abgedeckt ist bisher **nur Wild und Wildfleisch**;
Fischen, Fallenstellen und die Zubereitung selbst fehlen noch und brauchen
eigene Quellen.

### Bundesinstitut für Risikobewertung (BfR)

- Stellungnahme Nr. 045/2018 vom 21.12.2018, „Wildfleisch: Gesundheitliche
  Bewertung von humanpathogenen Parasiten“ (49 Seiten, DOI
  10.17590/20181221-095937-0):
  https://www.bfr.bund.de/cm/343/wildfleisch-gesundheitliche-bewertung-von-humanpathogenen-parasiten.pdf
- Stellungnahme Nr. 047/2006 vom 28.06.2006, „Leitfaden für die sensorische
  Untersuchung und Beurteilung von Wild“ (18 Seiten):
  https://www.bfr.bund.de/cm/343/leitfaden_fuer_die_sensorische_untersuchung_und_beurteilung_von_wild.pdf
- Information Nr. 01/2006 vom 02.01.2006, „Tipps für Jäger zum Umgang mit
  Wildfleisch“ (5 Seiten):
  https://www.bfr.bund.de/cm/343/tipps_fuer_jaeger_zum_umgang_mit_wildfleisch.pdf

Alle drei sind als PDF geladen und im Volltext gelesen worden, nicht über eine
Zusammenfassung.

Verwendet für: `nahrung-wildfleisch-durchgaren`, `nahrung-trichinen-nur-hitze`,
`nahrung-trichinellose-anzeichen`, `nahrung-fuchsbandwurm-eier-nicht-fleisch`,
`nahrung-fleisch-faeulnis-erkennen`, `nahrung-fleisch-schimmel-und-stickigkeit`,
`nahrung-fleisch-schmutz-abtragen`, `nahrung-wild-rasch-ausnehmen`,
`nahrung-hasenpest-beim-verarbeiten`.

### Robert Koch-Institut (RKI)

- RKI-Ratgeber „Tularämie“, vollständig durchgesehene und aktualisierte Fassung
  vom Januar 2026, Abschnitt „Therapie“ aktualisiert im April 2026, Stand der
  Seite 16.04.2026:
  https://www.rki.de/DE/Aktuelles/Publikationen/RKI-Ratgeber/Ratgeber/Ratgeber_Tularaemie.html

Als HTML geladen und im Volltext gelesen.

Verwendet für: `nahrung-hasenpest-beim-verarbeiten`,
`nahrung-tularaemie-anzeichen`.

Der RKI-Ratgeber schließt die Lücke, die die BfR-Information von 2006 offen
ließ: Dort waren zwei Ausbrüche beschrieben, aber keine Schutzmaßnahme, die
sich in einen Tipp übersetzen ließ. Das RKI nennt sie ausdrücklich — Handschuhe
beim Abbalgen, Fleisch von Hasen und Kaninchen nur gut durchgegart,
Oberflächen- und Brunnenwasser in betroffenen Gegenden abkochen, Wasser meiden,
das durch Tierkadaver verunreinigt sein kann, strikte Händehygiene, Atemschutz
bei zu erwartender Aerosolentwicklung.

**Ein Widerspruch zwischen den beiden Quellen, aufgelöst:** Das BfR berichtet
2006 von einem Todesfall in einer hessischen Jagdgemeinschaft. Das RKI
schreibt, bei der in Europa vorkommenden Unterart *holarctica* komme es oft zur
Spontanheilung und Todesfälle seien auch ohne Behandlung sehr selten. Beides
steht nebeneinander im Tipp: „sehr selten" heißt nicht „nie", und die neuere,
für die menschliche Erkrankung zuständige Quelle gibt den Ton an.

**Bewusste Entscheidung zu den Antibiotika.** Der Tipp
`nahrung-tularaemie-anzeichen` nennt, was *nicht* wirkt — Penicilline und
verwandte Mittel sowie Sulfonamide. Das ist die schützende Hälfte der Aussage:
Wer im Ernstfall auf ein Penicillin-Präparat aus dem Schrank setzt, verliert
Zeit. Die wirksamen Gruppen sind ebenfalls genannt (Doxycyclin, Ciprofloxacin),
ausdrücklich ohne Dosierung, weil die Quelle keine für die Selbstanwendung
nennt. Dieselbe Linie fährt bereits der Tipp „Antibiotika-Reste aus dem
Schrank: was bei Ruhr nie wirkt". **Zur Prüfung offen**, ob das so bleiben
soll.

### Warum diese Kategorie überhaupt nötig ist

Drei der aufgenommenen Aussagen widersprechen dem, was in Überlebensratgebern
üblicherweise steht. Genau deshalb stehen sie drin:

- **Einfrieren macht Wildfleisch nicht sicher.** Zwei bei Wildtieren
  vorkommende Trichinella-Arten (*T. nativa*, *T. britovi*) überstehen die
  vorgeschriebene Gefrierbehandlung; die Internationale Trichinella-Kommission
  erkennt Gefrieren bei Wildtieren nicht als sichere Methode an. **Pökeln und
  Räuchern ebenfalls nicht** — die Datenlage zu Salzgehalt, aw- und pH-Wert
  reicht dafür nicht aus.
- **Fuchsbandwurm überträgt sich nicht über das Fleisch.** Für den Menschen
  sind ausschließlich die Eier infektiös; eine Übertragung über das Fleisch
  eines Zwischen- oder Fehlwirts ist nicht möglich. Das Risiko liegt beim
  Abbalgen und beim Kontakt mit Kot, nicht beim Essen.
- **Abspülen und Abwischen verschlimmern es.** Wasser vermindert den sichtbaren
  Schmutz, verteilt die Keime aber großflächig; Gras kann Sporen tragen.
  Richtig ist, die verschmutzte Schicht abzutragen.

### Nachgeschärfte Nuance: Beeren und Pilze

Die verbreitete Warnung, Waldbeeren und Pilze könnten Fuchsbandwurmeier tragen,
steht in der Quelle **nicht so**. Dort gilt sie für den kleinen Hundebandwurm
(*E. granulosus*); für den kleinen Fuchsbandwurm (*E. multilocularis*) heißt es
ausdrücklich, eine Übertragung über Erde, Nahrung oder Wasser sei zwar denkbar,
bisher aber **nicht beschrieben**. Der Tipp gibt genau diese Unterscheidung
wieder und macht aus der Möglichkeit keine Tatsache. Die Schlagwörter „beeren"
und „pilze" wurden beim Messen wieder entfernt — siehe unten.

### Gemessene Suche, und was sie geändert hat

Vor dem Einbau gemessen, danach erneut. Vier Befunde, alle an der echten Suche
über das echte Paket:

- `kot` führte auf den Fuchsbandwurm-Tipp statt auf „Wohin mit dem Stuhl, wenn
  keine Toilette geht". Schlagwort `kot` entfernt.
- `pilze` führte auf den Fuchsbandwurm-Tipp statt auf „Vergiftung erkennen und
  Erstes tun" — bei Gleichstand entscheidet der Titel alphabetisch, und `F`
  kommt vor `V`. Schlagwörter `pilze` und `beeren` entfernt.
- `fieber` führte auf „Anzeichen einer Trichinellose" statt auf „Durchfall: wann
  Hilfe nötig wird". Das mehrteilige Schlagwort „fieber nach fleisch" zerfiel in
  Einzelwörter und zog damit `fieber` auf sich. Entfernt, ebenso „gesicht
  geschwollen", das der Anaphylaxie in die Quere gekommen wäre.
- `fleisch` führte zuerst auf „Faulendes Fleisch erkennen" statt auf die Regel,
  die das Fleisch sicher macht. Titel geändert zu „Verdorbenes Fleisch
  erkennen"; damit steht `Fleisch von Wildtieren immer durchgaren` vorn.

Geprüft und in Ordnung: `rauch` führt weiterhin ausschließlich auf die
Brandrauch-Tipps — die Suche vergleicht Wortanfänge und faltet `ä` nicht zu `a`,
deshalb kollidiert „räuchern" nicht mit „Rauch". Ebenso unverändert geblieben
sind `hitze`, `kälte`, `kalt`, `wärme`, `kühlen`, `auskühlen`, `gefrieren`,
`salz`, `geruch`, `gas`, `wasser`, `trinken`, `essen`, `kochen`, `durchfall`,
`vergiftung` und `hände`. Festgeschrieben in `NotfallSucheTest.kt`
(`dieNahrungsTippsVerdraengenKeinenNotfallTipp`).

### Bewusste Abweichung: die 72 Grad ohne Thermometer

Die Quelle nennt 72 °C im Kern für 2 Minuten und sagt nichts darüber, wie man
das ohne Messgerät einhält. Für die Lage, für die diese App gebaut ist, ist das
eine echte Lücke. Der Tipp trägt deshalb eine ausdrücklich als Einordnung
gekennzeichnete Ergänzung (Fleisch klein teilen und durchkochen statt anbraten,
nichts essen, was innen rot oder rosa ist) — sie steht nicht in der Quelle und
ist als solche im Text benannt. Eine belegte Faustregel dafür wäre ein Gewinn
und fehlt noch.

### Bundesamt für Verbraucherschutz und Lebensmittelsicherheit (BVL)

- Webseite „Lebensmittelhygiene: Fisch“ (Hygienischer Umgang mit Fisch):
  https://www.bvl.bund.de/DE/Arbeitsbereiche/01_Lebensmittel/03_Verbraucher/03_UmgangLM/02_LMzubereiten/04_Fisch/lm_zubereitung_fisch_node.html

Verwendet für: `nahrung-fisch-durchgaren`, `nahrung-fisch-nicht-roh`,
`nahrung-fisch-frisch-erkennen`.

**Ein Fund, der eine offene Frage beantwortet:** Die BVL-Seite nennt eine
Garprobe, die ohne Thermometer auskommt — „so lange garen, bis er
undurchsichtig ist und sich mit einer Gabel leicht zerteilen lässt". Für Fisch
ist damit die Lücke geschlossen, die beim Wildfleisch noch offen ist (dort
stehen 72 °C ohne ein Verfahren, sie ohne Messgerät einzuhalten).

### Bundesinstitut für Risikobewertung (BfR) — akute Lebensmittelvergiftungen

- FAQ „Akute Lebensmittelvergiftungen — was sind mögliche Ursachen und wie
  lassen sie sich vermeiden?“ vom 18.08.2025:
  https://www.bfr.bund.de/assets/01_Ver%C3%B6ffentlichungen/FAQ_deutsch/akute-lebensmittelvergiftungen-was-sind-moegliche-ursachen-und-wie-lassen-sie-sich-vermeiden.pdf

Verwendet für: `nahrung-fisch-histamin`, `nahrung-einkochen-botulismus`.

Zwei Aussagen daraus sind für diese App besonders wichtig, weil sie der
Grundregel „Hitze macht es sicher" ausdrücklich widersprechen:

- **Histamin und andere biogene Amine sind hitzestabil.** Kochen oder Braten
  macht einen zu warm gelagerten, eiweißreichen Fisch nicht wieder gut. Das ist
  die Ausnahme von allem, was in dieser Kategorie sonst steht.
- **Botulinum-Sporen überstehen 100 °C.** Beim Einkochen zu Hause lässt sich
  die Fabrikbehandlung (121 °C für 3 Minuten) nicht erreichen; die Quelle nennt
  stattdessen das zweimalige Erhitzen auf 100 °C im Abstand von ein bis zwei
  Tagen. Aufgetriebene Konserven werden nicht geöffnet, sondern vernichtet.

Der Einkoch-Tipp gehört strenggenommen nicht zum Bereich „Jagen und Fischen“.
Er ist beim Lesen der Quelle aufgefallen und sofort aufgenommen worden, weil
Vorratshaltung zum Kern dieser App gehört und ein falsch eingekochtes Glas
Bohnen tödlich enden kann.

### Giftinformationszentrum Bonn

- Informationsseite „Petermännchen“ (Bereich Tiere / Fische), herausgegeben von
  der Informationszentrale gegen Vergiftungen NRW:
  https://gizbonn.de/giftzentrale-bonn/tiere/fische/petermaennchen

Verwendet für: `erste-hilfe-petermaennchen` (Kategorie Erste Hilfe, weil es
eine Versorgung ist und kein Lebensmittelthema).

Die Quelle trägt den zweiten Zweig bereits selbst: Sie beschreibt das heiße
Wasserbad ausdrücklich für den Fall, dass ärztliche Hilfe nicht sofort
erreichbar ist. Übernommen wurde auch die Warnung, die Wassertemperatur mit der
gesunden Hand prüfen zu lassen — die gestochene Stelle wird taub, und wer sich
auf sie verlässt, verbrüht sich.

### Offen in dieser Kategorie

- ~~**Tularämie (Hasenpest)**~~ — erledigt am 28.07.2026 mit zwei Tipps aus dem
  RKI-Ratgeber (siehe oben). `hase` und `hasenpest` führen auf den
  Schutz-Tipp, `tularämie` auf die Anzeichen.
- ~~**Zeckenstiche**~~ — erledigt am 28.07.2026 mit fünf eigenen Tipps, die
  aber in der Kategorie Erste Hilfe liegen und nicht hier: Zecke entfernen,
  vorbeugen, Wanderröte erkennen, Borreliose behandeln, FSME. Die Quellen dazu
  stehen weiter unten unter „Zeckenübertragene Krankheiten".
- ~~**Fischen**~~ — Grundlagen erledigt am 28.07.2026 (garen, roh nur nach
  Frost, Frische erkennen, Histamin). **Nicht abgedeckt:** Fangmethoden,
  Fischarten, die man meiden sollte, Schwermetalle, Süßwasserfisch-spezifische
  Parasiten wie der Fischbandwurm. Für den Fischbandwurm ist am 28.07.2026
  gezielt gesucht worden; gefunden wurden nur Sekundärquellen (Nachschlagewerke,
  Ratgeberseiten), keine belastbare Behörden- oder Leitlinienquelle. Deshalb
  steht dazu nichts im Paket — die allgemeine Regel „nicht roh, ausreichend
  garen" deckt den Fall ab.
- **Zerlegen und Zubereiten** ist nicht abgedeckt.
- Die BfR-Information von 2006 ist zur Hälfte EU-Vermarktungsrecht und für
  diese App belanglos; übernommen wurden nur die Angaben zum Ausnehmen und
  Herunterkühlen.

### Was in dieser Kategorie anders wiegt als in der Quelle

Der Sensorik-Leitfaden beurteilt Fleisch mit der Frage, ob es in den Verkehr
gebracht werden darf. Wer es liest, kann verwerfen und etwas anderes kaufen.
Für die Lage dieser App gilt das nicht — dort steht „wegwerfen" gegen „nichts
essen". Die Tipps übernehmen die Bewertung der Quelle trotzdem unverändert:
Fäulnis und tief sitzender Schimmel sind kein Qualitätsmangel, sondern eine
eigene Gefahr, und Regel 4 verlangt im Zweifel den konservativeren Rat. Was
sich dagegen belegen ließ, steht ausdrücklich drin — dass oberflächlicher
Schimmel abgetragen werden darf, dass farbige Flecken nicht als
gesundheitsschädlich gelten, und dass ein durch Auslüften verschwindender
Geruch bei Stickigkeit kein Grund zur Beanstandung ist.

## Zeckenübertragene Krankheiten (Kategorie Erste Hilfe)

Recherchiert am 28.07.2026. Fünf Tipps: Zecke entfernen, Stiche vermeiden und
Körper absuchen, Wanderröte erkennen, Borreliose im Frühstadium behandeln,
FSME.

### Robert Koch-Institut (RKI)

- RKI-Ratgeber „Lyme-Borreliose“, aktualisierte Fassung April 2019, Anpassung
  im Abschnitt Vorkommen April 2025, Stand der Seite 28.04.2025:
  https://www.rki.de/DE/Aktuelles/Publikationen/RKI-Ratgeber/Ratgeber/Ratgeber_LymeBorreliose.html
- RKI-Ratgeber „FSME (Frühsommer-Meningoenzephalitis)“, Stand der Seite
  07.02.2025:
  https://www.rki.de/DE/Aktuelles/Publikationen/RKI-Ratgeber/Ratgeber/Ratgeber_FSME.html

### Deutsche Dermatologische Gesellschaft (AWMF-Leitlinie)

- S2k-Leitlinie „Kutane Lyme Borreliose“, Registernummer 013-044, Version 3.0
  vom 24.06.2024, Kapitel 8 (Therapie) und Tabelle 5:
  https://register.awmf.org/assets/guidelines/013-044l_S2k_Kutane_Lyme_Borreliose_2024-06.pdf

Alle drei geladen und im Volltext gelesen.

### Warum hier zum ersten Mal Dosierungen im Paket stehen

Nach der Weisung vom 28.07.2026 sollen fehlende Angaben, insbesondere
Dosierungen, aus weiteren Quellen ergänzt werden. Der Tipp
`erste-hilfe-borreliose-behandeln` nennt deshalb Mittel, Tagesdosis und Dauer
aus Tabelle 5 der Leitlinie, einschließlich der abweichenden Regeln für Kinder
(Doxycyclin erst ab dem 9. Lebensjahr) sowie für Schwangerschaft und Stillzeit
(Amoxicillin statt Doxycyclin).

Der Tipp beginnt mit einem eigenen Absatz, der klarstellt, dass es keine
ärztliche Verordnung ist und ohne Gewähr gilt; der allgemeine Hinweis dazu
liegt zusätzlich als `hinweis-angaben-ohne-gewaehr` im Paket. Die Begründung,
warum die Angaben trotzdem hineingehören, steht im Tipp selbst: Eine
unbehandelte Borreliose hinterlässt bleibende Schäden, und ohne diese Angaben
geschieht in einer Lage ohne Arzt gar nichts.

Aus der Leitlinie mit übernommen sind drei praktische Punkte, die dort
ausdrücklich als häufige Gründe für ein Therapieversagen genannt werden — und
die man ohne sie garantiert falsch macht:

- Doxycyclin wird schlechter aufgenommen, wenn gleichzeitig Milch,
  Milchprodukte, calciumhaltige Fruchtsäfte, stark mineralhaltige Wässer,
  Magnesium- oder Aluminiumpräparate, Eisenpräparate oder medizinische Kohle im
  Magen sind. Zwei bis drei Stunden Abstand.
- Die Herxheimer-Reaktion: In den ersten 24 Stunden kann es schlechter werden,
  die Rötungen flammen auf. Das Mittel wird trotzdem weiter eingenommen.
- Dosis und Dauer müssen eingehalten werden, auch wenn es besser wird.

### Zwei Aussagen, die dem Alltagswissen widersprechen

- **Schnelles Entfernen schützt vor Borreliose, aber nicht sicher vor FSME.**
  Borrelien sitzen im Darm der nüchternen Zecke und wandern erst nach Beginn
  des Saugens in die Speicheldrüsen; deshalb ist das Risiko in den ersten
  Stunden gering. Die FSME-Viren dagegen gelangen mit dem Stich in die
  Blutbahn. Beide Aussagen stehen in den jeweiligen Ratgebern.
- **Die Zecke einschicken bringt nichts.** Ein Erregernachweis in der Zecke
  sagt nicht, ob eine Übertragung stattgefunden hat, und ein negativer Befund
  schließt nichts aus. Von einer vorbeugenden Antibiotikagabe nach einem
  Zeckenstich rät das RKI generell ab.

### Gemessene Suche

Vor und nach dem Einbau gemessen. Ein Befund: `zeckenstich` führte zuerst auf
den Erkennen-Tipp statt auf das Entfernen, weil „Zeckenstich" in dessen Titel
stand. Wer gerade eine Zecke im Bein hat, braucht aber die Handlung. Beide
Titel geändert, festgeschrieben in `NotfallSucheTest`. Unverändert geblieben
sind unter anderem `antibiotika`, `tabletten`, `fieber`, `kopfschmerzen`,
`wunde`, `stich`, `biss`, `kind`, `hilfe`, `arzt` und `wasser`.

Beim Einbau hat außerdem die Inhaltsprüfung angeschlagen: Der ausgeschriebene
AWMF-Name war länger als die erlaubten 120 Zeichen für einen Quellennamen. Auf
„Deutsche Dermatologische Gesellschaft (AWMF-Leitlinie)" gekürzt.

### Offen

Ein eigener Tipp zum Krim-Kongo-Fieber oder zu anderen, in Mitteleuropa
seltenen zeckenübertragenen Erkrankungen fehlt. Ebenso fehlt, was bei einem
Zeckenstich bei Haustieren zu tun ist.

## Kategorie: Taktisch

Angelegt am 28.07.2026 auf Auftrag vom selben Tag. Der Zuschnitt war
vorgegeben: **Kern ist nicht der Kampf, sondern das Nichtstattfinden** — Lagen
erkennen, Sichtbarkeit vermeiden, deeskalieren, Fluchtwege, Umgang mit
Verletzungen durch Gewalt.

### Quelle

Beide Abschnitte stammen aus derselben Leitlinie, die für den zweiten Zweig
„niemand kommt" ohnehin im Haus ist:

- **IFRC, „International first aid, resuscitation, and education guidelines
  2020", Seiten 38–40, Abschnitt „Contexts — Conflict context".** Enthält die
  Phasentabelle (nach Giannou & Baldan 2019) mit den drei Lagen und ihren
  Prioritäten, die Aussage, dass ein Helfer eine Lage nicht betreten darf, die
  ihn unmittelbar gefährdet, die Rechnung „Beschuss beenden schlägt sofortige
  Versorgung", die Feststellung, dass im aktiven Kampf die starke Blutung die
  wahrscheinlichste Todesursache ist, die Ausrüstungsliste und den Fall, in dem
  der Helfer den Verletzten nur noch anleiten kann.
- **Dieselbe Leitlinie, Seiten 120–121, Abschnitt „De-escalation techniques for
  violent behaviour".** Enthält die drei Grundfertigkeiten, die Liste der
  Anzeichen bevorstehender Gewalt und zwei Technik-Listen: die des British Red
  Cross und die neun Punkte nach Richmond (2012).

Beide Abschnitte wurden am 28.07.2026 im Volltext aus dem geladenen PDF gelesen,
nicht aus einer Zusammenfassung.

### Eine Übersetzungsentscheidung, die begründet gehört

Die Ausrüstungsliste nennt „three-sided bandages, tourniquets, deep wound
packing and other materials to stop Severe bleeding or to manage an
Amputation". „Three-sided bandage" hat zwei Lesarten: das **Dreiecktuch**
(dreiseitig geschnitten) oder der **auf drei Seiten verklebte Verband** für eine
offene Brustwunde. Entschieden wurde für das Dreiecktuch, weil der Satz die
Gegenstände ausdrücklich der starken Blutung und der Amputation zuordnet — die
offene Brustwunde kommt darin nicht vor, und der Verband dafür heißt in der
Fachsprache „three-sided dressing", nicht „bandage". Nach der Arbeitsregel bei
widersprüchlichen Lesarten ist damit entschieden statt nur vermerkt.

### Was bewusst nicht im Paket steht

- **Kein Ausstopfen von Wunden und kein behelfsmäßiges Abbindesystem.** Die
  Leitlinie nennt das Material, nicht die Handgriffe. Die Entscheidung gegen
  eine Bastelanleitung für Abbindesysteme ist am 28.07.2026 mit den gemessenen
  Ausfallraten begründet worden und gilt hier unverändert weiter; der
  Taktisch-Tipp verweist auf die Blutungs-Tipps statt eine eigene Technik zu
  erfinden.
- **Keine Technik zum Verbergen und keine Anleitung für Deckung.** Die
  Leitlinie sagt nur, dass eine Ausbildung dafür Wege zeigen soll, Deckung zu
  finden oder herzustellen (Beispiel: Rauch als Sichtschutz) — sie zeigt keine.
  Etwas dazuzuerfinden wäre genau der Fehler, den die Arbeitsregel verbietet.
- **Kein Ansprechen und Vertrauensaufbau als Anleitung.** Die Leitlinie ordnet
  das ausdrücklich einer Schulung zu („could be trained to"), nicht dem
  Selbststudium. Der einleitende Tipp sagt das so.

### Der zweite Zweig in dieser Kategorie

`taktisch-abbrechen` endet in der Quelle bei „Rettungsdienst, Polizei". Der
zweite Zweig steht dort und ist als Einordnung gekennzeichnet: Kommt keine
Verstärkung, bleibt der Abstand das einzige sicher wirkende Mittel. Die
Leitlinie sagt zu dieser Lage nichts; das ist Abwägung und steht auch so da.

### Gemessene Suche

Vor und nach dem Einbau mit der Wegwerf-Sonde gemessen (32 Wörter). Die Sonde
hat zwei echte Verdrängungen gefangen, die ohne Messung durchgegangen wären:

- **`flucht`** führte auf einmal auf `taktisch-abbrechen` statt auf
  `erste-hilfe-brandrauch-sofort-raus`. Ein brennender Raum lässt zwei bis vier
  Minuten Zeit; das darf kein Deeskalationstext verdrängen. Schlagwort
  „flucht" wieder entfernt.
- **`gefahr`** schob `erste-hilfe-vergiftung-atemwege` aus den ersten drei.
  Schlagwörter „gefahr" und „gefahrenzone" entfernt; die Wörter stehen weiter
  im Fließtext, nur ohne die Gewichtung eines Schlagworts.

Ebenfalls entfernt: „abstand halten" bei `taktisch-deeskalation`, weil es
`hygiene-ausscheidungen` (Zehn-Meter-Abstand zur Wasserstelle) vom ersten Platz
verdrängt hatte.

Nach diesen drei Korrekturen behält jedes zuvor gemessene Wort seinen ersten
Treffer. Verschoben haben sich nur die Plätze zwei und drei bei `sicherheit`,
`abstand` und `gefahr`. `sicherheit` führt weiterhin zuerst auf
`erste-hilfe-eigenschutz`.

## Übersetzer: Phrasenkatalog (content/phrases.json)

Angelegt am 28.07.2026 auf Entscheidung vom selben Tag: Der Katalog
übernimmt das „Emergency Multilingual Phrasebook" des British Red Cross.

### Quelle

**British Red Cross mit Unterstützung des National Health Service, „Emergency
Multilingual Phrasebook", Druckfassung vom 23.01.2004** (Datum aus den
Satzmarken der Druckdateien). 62 nummerierte Sätze für die Verständigung
zwischen medizinischem Personal und Patienten ohne gemeinsame Sprache,
ursprünglich in 36 Sprachen. Bezogen als je eine PDF-Datei pro Sprache über die
Spiegelung der Raunds & District Emergency Medical Services
(raems.com/multi.html), abgerufen am 28.07.2026; die Originalseiten von NHS
Confederation und weiteren Trägern waren an diesem Tag nicht abrufbar (401
bzw. 403). Alle sechs übernommenen Sprachdateien (Englisch, Deutsch,
Französisch, Spanisch, Italienisch, Polnisch) liegen unter
`work/quellen/brc-phrasebook/` und wurden im Volltext gelesen.

### LIZENZ UNGEKLÄRT — Sperre vor Veröffentlichung

Das Phrasebook steht unter KEINER freien Lizenz. Die Nutzungserlaubnis wird
direkt beim British Red Cross geklärt (Anschrift aus dem Faltblatt:
UK Office, 9 Grosvenor Crescent, London SW1X 7EJ, redcross.org.uk; Registered
Charity 220949). **Bis die Erlaubnis vorliegt, darf kein Paket mit
`content/phrases.json` das Haus verlassen** — nicht als APK-Asset einer
veröffentlichten Version, nicht als eigenes Paket, nicht im offenen Repo. Die
Entwicklung und der lokale Betrieb sind davon unberührt.

### Übernahme-Entscheidungen

- **Wortlaut je Sprache unverändert übernommen**, auch wo sich die
  Übersetzungen einer Nummer stilistisch unterscheiden (die spanische Fassung
  von Satz 5 bittet ums Aufschreiben, die deutsche fragt nur; die deutsche
  Fassung von Satz 2 lässt „in English" weg). Die Quelle hat je Sprache eigene
  Übersetzer benutzt; das wird nicht geglättet.
- **Normalisiert wurde nur Mechanik der PDF-Extraktion**, protokolliert in
  `work/phrasen-fixlog.txt` (80 Einträge): zerrissene Anfangsbuchstaben
  („T elefonnummer"), fehlende Leerzeichen nach polnischen Kommas, die
  Schmerzskala („)234567891 0(" → „) 2 3 4 5 6 7 8 9 10 („) und die im Druck
  hochgestellten Optionsnummern, die beim Extrahieren am Folgewort kleben
  („1doctor") — sie stehen jetzt einheitlich als „(1) ", weil das Zeigen auf
  nummerierte Optionen die Bedienung des Faltblatts ist.
- **Italienisch:** Die Extraktion hat die hochgestellten Optionsnummern in den
  Sätzen 22, 26, 27, 33, 34, 45, 46 ganz verschluckt (erkennbar an doppelten
  Leerzeichen an genau diesen Stellen). Wiederhergestellt aus der
  Parallelstruktur der übrigen fünf Sprachen — gleiche Optionsliste, gleiche
  Reihenfolge.
- **Ein Fußzeilenrest entfernt:** In Satz 31 (en) war beim Extrahieren das
  alleinstehende Wort „Movement" aus dem Rotkreuz-Vereinstext in den Satz
  gerutscht. Gegen das PDF geprüft und entfernt.
- **Gruppierung ist eigene Zutat.** Das Faltblatt ist eine flache Liste 1–62
  mit einer einzigen eigenen Zwischenüberschrift („Questions from patients",
  ab Satz 58). Die acht Gruppen samt Titeln in sechs Sprachen sind
  Navigations-Beschriftung von uns; nur Gruppe 8 trägt die Überschrift der
  Quelle. Die Satznummern der Quelle stehen je Gruppe in der Belegangabe.
- **Nichts weggelassen:** alle 62 Sätze sind übernommen, auch die
  UK-spezifischen (Satz 32 fragt nach Aufenthalt außerhalb Großbritanniens,
  Sätze 51/58/59 passen auf eine Notaufnahme). Sie kosten nichts und sind
  genau dann nützlich, wenn jemand im Ausland in eine Klinik gerät.

### Eigenheiten der Quelle, bewusst nicht geglättet

- **Satz 15 auf Polnisch fragt mit gedrehter Polarität:** „Czy oddycha
  Pan/Pani swobodnie?" (Atmen Sie frei?), während Englisch „Are you short of
  breath?" fragt. Ein Ja bedeutet also je nach Sprache das Gegenteil. Das ist
  der Wortlaut der Quelle; die Oberfläche zeigt immer beide Sprachen
  nebeneinander, damit beide Seiten dieselbe Frage sehen.
- **Satz 50 auf Polnisch ist als Frage formuliert** („Czy może Pan/Pani
  wstać.": Können Sie aufstehen), Englisch als Erlaubnis („You may get up
  now."). Ebenfalls Quelle.
- **Satz 38 auf Polnisch nennt konkret ein EKG**, wo Englisch allgemein „test
  your heart" sagt.

### Suche

Die Phrasen sind BEWUSST NICHT im Suchindex. Sätze wie „Haben Sie Schmerzen?"
oder „Haben Sie Blutungen?" würden mit hoher Gewichtung die
Notfall-Handlungs-Tipps verdrängen (gemessene Verdrängung war beim Einbau der
Kategorie Taktisch schon zweimal der Fall). Ob und wie der Katalog durchsuchbar
wird, ist eine eigene, zu messende Entscheidung.

## Trinken bei geschlucktem Gift (erste-hilfe-gift-geschluckt-trinken)

Weisung vom 28.07.2026: so viele Quellen wie möglich, daraus eine
belastbare Aussage. Gesammelt wurden am 28.07.2026 elf Quellen im Volltext
(alle unter `work/quellen/medizin-luecken/gift-trinken/`, Auswertung mit
Gegenüberstellungstabelle in `work/recherche-medizin-luecken/quellenlage.md`);
jedes im Tipp verwendete Zitat wurde vor der Übernahme zusätzlich selbst im
gesicherten Rohtext nachgeschlagen.

### Der Befund: drei belegte, einander widersprechende Linien

1. **Kleine Schlucke Wasser/Tee/Saft, keine Milch** — übereinstimmend
   Giftnotruf Berlin (Charité), GIZ Bonn, VIZ Freiburg, GIZ-Nord und BfR,
   dazu das ältere DFV-Blatt. Begründung zur Milch zweifach unabhängig
   (Bonn: „beschleunigt in vielen Fällen die Giftaufnahme durch den Darm";
   Freiburg: Fettgehalt kann die Aufnahme verstärken).
2. **Ohne Anweisung einer Fachstelle gar nichts geben** — DGUV Information
   204-007 (Stand März 2023, S. 97) und BVKJ (kinderaerzte-im-netz.de).
3. **Wasser ODER Milch bei Ätzendem** — US Poison Control (poison.org) und
   AAPCC; der NHS wiederum: gar nichts. Die US-Milch-Erlaubnis ist das
   direkte Gegenteil der deutschen Linie.

Einig sind sich ausnahmslos alle: kein Erbrechen auslösen, kein Salzwasser
(VIZ Freiburg: kann selbst eine Vergiftung auslösen), Bewusstlosen bzw.
nicht Wachen nichts einflößen (Berlin knüpft das Trinken bei Verätzung
ausdrücklich an „Wenn der Patient wach ist").

### Die Entscheidung, nach der Widerspruchsregel

Der Tipp folgt der Linie der deutschen Giftinformationszentren (1) und legt
die DGUV-Gegenposition (2) im Text offen. Begründung: Die
Giftinformationszentren SIND die Fachstellen, auf deren Anweisung die
DGUV-Linie verweist — ihre öffentlichen Vorab-Empfehlungen sind also genau
die Anweisung, die die DGUV einholen will. Und in der Grundannahme dieser
App (kein Anruf möglich) ergäbe „warten auf eine Anweisung" keine Handlung.
Die US-Position (3) wurde verworfen: Für ein deutschsprachiges Paket wiegen
die fünf regionalen Fachstellen schwerer als eine US-Empfehlung, die der
deutschen in der Milchfrage direkt widerspricht; der Widerspruch ist hier
dokumentiert und nicht verschwiegen.

Nicht erreichbar waren GGIZ Erfurt (Bot-Schutz), TOXBASE (kein öffentlicher
Zugang) und einzelne Altseiten; Einzelheiten in der Auswertungsdatei. Ein
Suchmaschinen-Schnipsel des GGIZ Erfurt wurde ausdrücklich NICHT als Beleg
verwendet, weil der Volltext nicht ladbar war.

### Gemessene Suche

Vorher/nachher mit der Sonde (23 Wörter): „verschluckt" führt unverändert auf
die Ersticken-Tipps — deshalb steht dieses Wort bewusst NICHT in den
Schlagwörtern des neuen Tipps. „säure", „lauge", „verätzung", „putzmittel",
„waschmittel", „giftnotruf", „salzwasser", „milch" führen auf den neuen Tipp.
„gift" führt jetzt zuerst auf ihn (vorher alphabetisch auf den
Kontaktgift-Tipp); Schlucken ist der häufigste Vergiftungsweg, die übrigen
Vergiftungs-Tipps stehen direkt dahinter. „vergiftung" und „giftig" führen
unverändert zuerst auf den Erkennen-Tipp.

## Bauanleitungen (content/guides.json)

Angelegt am 28.07.2026 mit fünf Anleitungen: Behelfs-Lean-to, Laubhütte,
Lagerfeuer sicher anlegen und über Nacht erhalten, Grundknoten und Lashings,
Flaschenzug. Die Entwürfe mit allen Belegzitaten liegen unter
`work/recherche-bauanleitungen/`; jedes der 58 Zitate wurde vor der Übernahme
maschinell gegen die gesicherten Quelltexte geprüft.

### Quellen

- **US Army, Field Manual FM 3-05.70 „Survival"** (17.05.2002), Kapitel 5
  „Shelters", Kapitel 7 „Firecraft", Appendix G „Ropes and Knots". Werk der
  US-Bundesregierung, gemeinfrei.
- **US Army, Field Manual FM 5-125 „Rigging Techniques, Procedures, and
  Applications"** (Change 1, 23.02.2001), Section III „Blocks and Tackle
  Systems". Ebenfalls gemeinfrei.
- **Deutscher Feuerwehrverband / vfdb, Merkblatt „Feuer im Freien"**
  (24.07.2025) — für die deutschen Sicherheitsabstände und die Rechtslage.
  Derselbe Herausgeber trägt schon die Kohlenmonoxid-Tipps.

### Eine Quelle wurde nach der Übernahme wieder entfernt

Der Flaschenzug hatte zunächst einen zehnten Schritt zur Rücklaufsperre
(Gardaknoten) mit einer Wikibooks-Knotenfibel als Beleg. Beim Prüfen der
Lizenz stand im Export: „Inhalt ist verfügbar unter der GNU Free
Documentation License." **GFDL ist mit der für die Inhalte gewählten
CC BY-SA nicht vereinbar**, deshalb sind der Schritt, die Quelle und der
zugehörige Materialeintrag (Karabinerpaar) wieder entfernt worden. Der
Flaschenzug funktioniert ohne Rücklaufsperre; eine Alternative aus einer
gemeinfreien Quelle nachzutragen bleibt offen.

### Bewusst nicht übernommen

Militärische und taktische Passagen (Tarnung, Feindbeobachtung), Poncho- und
Fallschirm-Varianten (setzen Militärausrüstung voraus), Sumpfbett sowie
Wüsten- und Schnee-Unterstände (eigene spätere Anleitungen), Knoten, die die
Quelle selbst als überholt bezeichnet, sowie Schießpulver und Autobatterie
als Zündmittel.

### Zwei eigene Zusätze, als solche gekennzeichnet

Der Feuer-Anleitung ist ein Zweig „wenn niemand kommt" angehängt (beide
Quellen enden bei „112 rufen" — bei Verbrennungen und bei außer Kontrolle
geratenem Feuer). Bei der Laubhütte steht ein eigener Sicherheitszusatz: kein
Feuer im oder am Unterstand, weil die Konstruktion aus reinem Trockenmaterial
besteht — die Quelle spricht das an dieser Stelle nicht an.

## Bekannte Lücke, bewusst nicht geglättet

Der Tipp zur längeren Kochzeit in großer Höhe wird oft — auch bei
US-Behörden wie CDC — als "WHO, 2015" zugeschrieben. Im WHO-Originaldokument
(Technical Brief WHO/FWC/WSH/15.02) findet sich diese Zeitangabe für große
Höhe jedoch nicht; dort heißt es nur, Abkochen wirke "even at high altitude",
ohne eigene Zeitangabe. Deshalb ist die Drei-Minuten-Regel hier korrekt der
EPA zugeordnet und nicht der WHO.

Für ein eigenständiges Tipp zu Warnzeichen verunreinigten Wassers (trüb,
Geruch, Fundort z. B. nahe Landwirtschaft/Industrie) wurde keine belastbare
BBK- oder WHO-Primärquelle gefunden, die das eigenständig und zitierfähig
behandelt. Bewusst nicht ergänzt, um keine unbelegte Faustregel in die App zu
schreiben — offener Punkt für eine spätere Recherchesitzung, siehe
[`../../ROADMAP.md`](../../ROADMAP.md).

Das Deutsche Rote Kreuz (drk.de) hat für dieses Thema keine eigenständige
Vorsorge-Seite für deutsche Haushalte; die Fundstellen dort behandeln
internationale Katastrophenhilfe im Ausland und wurden deshalb nicht als
Quelle verwendet.

---

# Quellen der Schicht vom 03.08.2026 (15 neue Einträge)

Recherchiert und im Volltext gelesen am 03.08.2026. Kopien der Volltexte liegen
unter `work/quellen/` (nicht im Git).

**Zur Lizenzspalte:** „frei" heißt, das Werk selbst dürfte weitergegeben werden
(Behördenwerk oder ausdrückliche Freigabe). „nur Beleg" heißt: Das Werk ist
urheberrechtlich geschützt und wurde **ausschließlich zur Prüfung von Zahlen und
Handgriffen** herangezogen — der Text im Paket ist eigener Text, es wurde kein
Wortlaut übernommen. Diese Handhabung folgt der Linie, die das Paket schon beim
WHO-Kinderhandbuch gewählt hat. **Kein Werk der Spalte „nur Beleg" wird mit dem
Paket ausgeliefert.**

| Eintrag im Paket | Quelle | Lizenz |
|---|---|---|
| `medizin-regelblutung-stark` (Arzneimenge) | Cochrane Database of Systematic Reviews, Bofill Rodriguez u. a., CD013651 (23.07.2020) | nur Beleg (© Cochrane/Wiley) |
| dgl. | BfArM, Mustertext Ibuprofen-Fachinformation, Muster-Nr. 8000197, Stand 23.07.2024 | **frei** (amtliches Werk) |
| dgl. | Gebrauchsinformation Ibuprofen 400 mg Filmtabletten, über beipackzettel.apocdn.net | nur Beleg |
| `erste-hilfe-eiseinbruch-*` | DLRG, „Selbstrettung bei Eisunfällen" und „Fremdrettung aus dem Eis", dlrg.de (Grundlage: Ausbilderhandbuch Rettungsschwimmen, 2. korr. Auflage 2012) | nur Beleg |
| dgl. | Tipton, Collier, Massey, Corbett, Harper: „Cold water immersion: kill or cure?", Repositorium der University of Portsmouth | nur Beleg |
| `medizin-verhuetung-ohne-nachschub` | CDC, „Effectiveness of Family Planning Methods", CS 242797, über stacks.cdc.gov | **frei** (US-Behördenwerk) |
| dgl. | CDC, „U.S. Medical Eligibility Criteria for Contraceptive Use, 2016", MMWR 65(3), Anhänge F und G | **frei** (US-Behördenwerk) |
| `erste-hilfe-lawine-*` | WSL-Institut für Schnee- und Lawinenforschung SLF, Merkblatt „Lawinenunfall (Schweiz)" des Kern-Ausbildungsteams, Fassung 2020 | nur Beleg |
| `wasser-regen-vom-dach` | University of Arizona Cooperative Extension, „Preparing Rainwater for Potable Use", az1863 (März 2021) | nur Beleg |
| `medizin-epilepsie-mittel-gehen-aus` | S2k-Leitlinie „Erster epileptischer Anfall und Epilepsien im Erwachsenenalter", AWMF 030-041, Fassung 09/2023 | nur Beleg |
| `taktisch-licht-kerzen-ziehen` | VITA, „Village Technology Handbook", Abschnitt Candle Making, über archive.org | nur Beleg |
| dgl. | Colebrook Historical Society, „Illumination in Colonial New England" | nur Beleg |
| dgl. | University of Delaware, Material Matters, Dombrovskaya (23.10.2023) | nur Beleg |
| `medizin-monatshygiene-ohne-vorrat` | UNFPA, „Menstrual Health and Hygiene Management during Emergencies" (11/2021) | nur Beleg |
| dgl. | International Rescue Committee, „The MHM in Emergencies Toolkit" | nur Beleg |
| `medizin-abgelaufene-medikamente` | Lyon, Taylor, Porter, Prasanna, Hussain: „Stability Profiles of Drug Products Extended beyond Labeled Expiration Dates", J Pharm Sci 95(7) 2006 (Auswertung des FDA/DoD Shelf Life Extension Program) | nur Beleg (© Wiley; Autoren der FDA) |
| dgl. | FDA, „Don't Be Tempted to Use Expired Medicines", fda.gov | **frei** (US-Behördenwerk) |
| `taktisch-sturm-verhalten` | BBK, „Blitzgescheit — Baustein zum Thema Gewitter und Sturm" (Stand 2017) und „Vorsorge und Handeln bei Sturm, Gewitter und Hagel" | nur Beleg (Bundesbehörde, Nutzungsrechte nicht ausdrücklich freigegeben) |
| `medizin-sterben-begleiten` | S3-Leitlinie Palliativmedizin, Langversion 2.2 (09/2020), AWMF 128-001OL, Kapitel 19 | nur Beleg |
| `medizin-psychopharmaka-gehen-aus` | S3-Leitlinie Schizophrenie, AWMF 038-009, Fassung 15.10.2025, über dgppn.de | nur Beleg |
| dgl. | ÄZQ, NVL-Patientenblatt „Antidepressiva — Was ist beim Absetzen zu beachten?", nvl-005 (2022) | nur Beleg |
| dgl. | Gebrauchsinformation Lithiumcarbonat (Quilonum retard 450 mg) | nur Beleg |
| `taktisch-baum-faellen` | DGUV Information 214-046 „Sichere Waldarbeiten" (Mai 2014) | nur Beleg |

## Vier Zahlen, die bewusst so und nicht anders im Paket stehen

1. **Ibuprofen 1200 mg je Tag statt der 2400 mg des SOF-Handbuchs.** Beide
   Angaben sind belegt und widersprechen sich nicht: 1200 mg ist die kleinste
   Menge mit nachgewiesener Wirkung auf die Blutungsmenge und zugleich die
   untere Grenze der amtlichen Spanne; 2400 mg ist deren obere Grenze. Nach
   Regel 4 („im Zweifel der konservativere Rat") steht die kleinere im Paket,
   die außerdem ohne Rezept zulässig ist.
2. **Der erste Regenschwall: 0,4 Liter je Quadratmeter.** Die Quelle rechnet mit
   10 Gallonen je 1000 Quadratfuß. Die Umrechnung auf Liter und Quadratmeter ist
   die dieses Pakets und im Tipp als solche gekennzeichnet.
3. **Rückfallzahlen bei Psychopharmaka gelten für Antipsychotika.** Für
   Antidepressiva und Lithium stehen bewusst keine Prozentzahlen im Paket — dafür
   gibt es keine eigene Quelle. Der Tipp sagt das ausdrücklich.
4. **Talgkerzen: etwa eine halbe Stunde am Stück.** Aus der Museums-Quelle der
   University of Delaware, nicht aus einer technischen Norm.

## Was in dieser Schicht bewusst NICHT geschrieben wurde

- **Zeit und Kalender halten ohne Uhr.** Geprüft: Weder FM 3-05.70 noch die
  Peace-Corps-Sammlung noch eine andere vorhandene Quelle beschreibt ein
  Verfahren. Eine Sonnenuhr ist nicht so einfach, wie sie aussieht — die
  Stundenlinien hängen von der geografischen Breite ab. Lücke ausgewiesen statt
  gefüllt.
- **Getreide mahlen.** Ebenfalls geprüft, ebenfalls keine freie Quelle. Bleibt
  ein echter Punkt für die Buchscans.
- **Die Tötungsstellen je Tierart** und **die Salpetermengen beim Pökeln** —
  unverändert draußen, siehe die früheren Abschnitte.

## Die zweiten Zweige nach Regel 36 (03.08.2026)

Regel 36 verlangt: Ein Inhalt, der beim Notruf oder beim „ab zum Arzt“ endet,
braucht einen zweiten Zweig für den Fall, dass niemand kommt. Fünf Tipps
hatten ihn noch nicht. Vier der fünf Zweige sind **Einordnung dieses Pakets**
und im Text genau so gekennzeichnet — sie führen nur zu Ende, was der Tipp
selbst schon sagt, und wo die ehrliche Antwort „man kann wenig tun“ lautet,
steht das auch so da.

| Tipp | Woher der Zweig kommt | Lizenz |
|---|---|---|
| `erste-hilfe-wirbelsaeule` | Einordnung dieses Pakets | — |
| `erste-hilfe-rauchvergiftung-erkennen` | Einordnung dieses Pakets | — |
| `erste-hilfe-unterzuckerung` | Einordnung dieses Pakets | — |
| `erste-hilfe-durchfall-anhaltend` | Einordnung dieses Pakets | — |
| `erste-hilfe-kontaktgift` | US-Umweltbehörde (EPA), „Recognition and Management of Pesticide Poisonings“, 6. Aufl., Kap. 3, Abschnitte „Skin Decontamination“ und „Eye Decontamination“ | frei (US-Behördenwerk) |

**Der fünfte ist die Ausnahme, und das mit Absicht.** Beim Kontaktgift wäre
eine reine Einordnung riskant gewesen: Ob man ein Mittel von der Haut spült
und womit, ist nichts, was man sich herleitet. Die EPA-Quelle sagt es genau
und sagt mehr, als eine Herleitung ergeben hätte — vor allem, dass das
Abwaschen NEBEN den lebensrettenden Maßnahmen läuft und nicht danach, und
welche Stellen übersehen werden (hinter den Ohren, unter den Nägeln, in den
Hautfalten). Nur drei Zusätze in diesem Tipp sind Einordnung und als solche
gekennzeichnet: die Plastiktüte als schlechterer Ersatz für Handschuhe, der
Wärmeerhalt beim Waschen und der Verweis auf die Herzdruckmassage.

**Was in diesen Zweigen bewusst NICHT steht:** keine Zahl zur Dauer der
Kohlenmonoxid-Ausleitung an frischer Luft (dafür gibt es hier keine Quelle,
und der Tipp sagt stattdessen, dass Sauerstoff in fachliche Hände gehört),
und keine Anleitung zum Notfallset bei Unterzuckerung — der Zweig verweist
das ausdrücklich in die Vorsorge, weil eine Anwendungsanleitung ohne Quelle
und ohne Einweisung nicht in ein Handbuch gehört.

## Die Nacht vom 10. auf den 11.08.2026 — vierzehn Pakete, und was dabei draußen blieb

Elf neue Agrikultur-Kapitel und drei Eingriffe in der Medizin. Alle
Einzelbelege stehen maschinenlesbar im Feld `sources` des jeweiligen
Eintrags — hier steht, was man dort nicht sieht: warum bestimmte Quellen
NICHT benutzt wurden.

### Woher der Stoff kam

Der Großteil stammt aus der Sammlung `usda-farmersbulletin` auf archive.org
und aus benachbarten USDA-Beständen. Bienen, Brunnen, Obstbäume, Beeren,
Zäune, Nutztiere, Seife und Wäsche, Zwiebeln, Winterfutter und Scheitholz
gehen auf Farmers' Bulletins und behördlich verfasste Rundfunkskripte
zurück, jeweils im Volltext gelesen und lokal unter `work/quellen/`
gesichert.

Die drei Medizin-Arbeiten haben andere Wurzeln: einen Übersichtsaufsatz und
eine Arbeit aus dem Deutschen Ärzteblatt für die Gerinnungshemmer, das
gemeinsame Feldhandbuch von PAHO, WHO, IKRK und IFRK für die Grabtiefe, und
eine Klinikuntersuchung für die Relaktation.

### Die Schutzfrist-Prüfung hat in dieser Nacht FÜNFMAL Inhalt gekostet

Für ältere Werke gilt in diesem Projekt das Sterbejahr des Verfassers plus
siebzig Jahre, nicht das Erscheinungsjahr. Das ist mehrfach hart geworden:

| Verfasser | Werk | Folge |
|---|---|---|
| George M. Darrow († 1983) | Johannisbeeren und Stachelbeeren | im Beeren-Kapitel ausgelassen |
| Arthur D. Holmes († 18.07.1956) | Fette im Haushalt | Frist läuft bis 31.12.2026 — knapp verfehlt |
| J. C. Walker († 1994) | Zwiebelkrankheiten, Steckzwiebeln | Lücke im Zwiebel-Kapitel, obwohl die Hauptquelle ausdrücklich dorthin verweist |
| Verne E. Davison (geb. 1904, kein Sterbejahr) | Fischteiche | Thema ganz verworfen |
| A. R. Lee, G. E. Howard (keine Daten) | Enten und Gänse | Thema ganz verworfen |

Dazu kommen die Nebenquellen, die aus demselben Grund draußen blieben:
T. E. Woodward (Silage), R. T. Hall (Brennholz im Krieg) und mehrere weitere.
**Beide offenen Grundsatzfragen dazu stehen in `LIZENZANFRAGEN.md` und sind
noch nicht entschieden.**

### Der Gegenbeweis, dass die Prüfung sich lohnt

Zwei namentliche Verfasser wurden geprüft und BENUTZT: Lydia Ray Balderston
(† 26.02.1951, Home Laundering) und W. R. Beattie (1870–1954, Zwiebeln).
Ein Name auf dem Heft ist also kein Ausschlussgrund, sondern ein
Prüfauftrag.

### Geschützte Quellen, nur als Beleg

Vier Quellen sind geschützt oder ungeklärt und wurden nach demselben
Verfahren behandelt wie seinerzeit DLRG und Tipton bei den
Eiseinbruch-Tipps — **nur als Beleg, Tatsachen in eigener Gliederung und
eigenen Worten, keine übernommene Formulierung:** das Deutsche Ärzteblatt
und der Antikoagulations-Aufsatz, das Bestattungshandbuch (sein Impressum
verlangt Erlaubnis für Vervielfältigung UND Übersetzung) und die
Relaktations-Untersuchung (keine Lizenzangabe auffindbar).

### Eine Quelle wurde gelesen und BEWUSST NICHT benutzt

Zur Frage, wie Aspirin bei Vorhofflimmern gegen echte Gerinnungshemmer
abschneidet, wurde eine offen lizenzierte Arbeit gefunden und gelesen
(PMC12819969). Ihr Ergebnis ist ein Null-Befund in einer Untergruppe — und
**die Verfasser warnen im eigenen Text ausdrücklich davor, das als
Gleichwertigkeit zu lesen.** Wer daraus „Aspirin reicht auch" machte,
schriebe einen Satz, der Menschen den Schlaganfall kosten kann. Die Lücke
bleibt deshalb im Gerinnungshemmer-Tipp benannt, statt mit einer Zahl
gefüllt zu werden, die nicht trägt.

### Zahlen, die nicht übernommen wurden, weil sie unleserlich waren

Vier Tabellen sind an der Texterkennung gescheitert und wurden NICHT
rekonstruiert: die Widder-Tabelle im Brunnen-Kapitel, Garvers Bock-Tabelle
bei den Nutztieren, die Dauer des vollständigen Ablagerns beim Scheitholz —
und, aus früherer Arbeit, die FAO-Ertragstabelle. Geraten wurde in keinem
Fall.

### Eine Berichtigung, die diese Belegpflege rechtfertigt

Im Tipp „Tote bergen: wie man die Arbeit einteilt" stand für den Abstand des
Grabes zur Wasserstelle eine **Übertragung aus der Latrinen-Regel: zehn
Meter.** Sie war als Übertragung gekennzeichnet — und genau deshalb fiel sie
auf, sobald eine echte Quelle da war. Belegt sind **200 bis 350 Meter**.
Ohne die Kennzeichnung hätte niemand gewusst, dass an dieser Stelle
nachzuprüfen ist.

## 11.08.2026 — Kochkiste (Bauanleitung)

Achtzehnte Bauanleitung: `kochkiste-bauen`. Ein Topf wird auf dem Feuer
angekocht und dann in einer dick gedämmten Kiste fertig gegart. Das ist der
sparsamste bekannte Weg, zähes Fleisch, Hülsenfrüchte und Brei ohne
Dauerfeuer weichzubekommen — und deshalb ein Kernstück für Lagen, in denen
Brennstoff knapp ist.

### Quellen

- **US-Landwirtschaftsministerium, Farmers' Bulletin 771 „Homemade Fireless
  Cookers and Their Use"** (Office of Home Economics, Februar 1917). Bauweise,
  Maße, Packstoffe und die Temperaturmessung. Volltext gelesen und lokal
  gesichert, abgerufen am 11.08.2026.
- **US-Ernährungsbehörde und US-Landwirtschaftsministerium, Food Leaflet
  Nr. 13 „Let the Fireless Cooker Help You Cook"** (1918). Zweites,
  unabhängiges Merkblatt derselben Zeit: Materialliste, Mindestdicke der
  Packung, Vorsichtsregeln.
- **Weltgesundheitsorganisation, „Five Keys to Safer Food Manual"** (2006).
  Nur für eine einzige Zahl: die Gefahrenzone 5 bis 60 Grad.

### Zur Schutzfrist

Beide Hefte sind Behördenschriften ohne benannten persönlichen Verfasser;
solche Werke sind in Deutschland 70 Jahre nach Erscheinen frei (§ 66 UrhG),
und 1917/1918 liegt weit davor. Der im Bulletin genannte Amtsleiter
C. F. Langworthy ist 1932 gestorben, also auch über die Personenfrist
hinaus frei. Auf „US-Bundeswerk, also gemeinfrei" allein wurde sich
**nicht** verlassen — diese Regel gilt in den USA, nicht in Deutschland.

### Die Sicherheitsaussage stammt NICHT aus den Quellen

Das Bulletin von 1917 hat gemessen: kochendes Wasser war nach vier Stunden
noch 78 Grad warm, nach acht Stunden 68 Grad. Die WHO zieht die Grenze der
Keim-Gefahrenzone bei 60 Grad. Beides zusammengelegt heißt: Eine gut
gebaute, volle Kiste liegt nur **acht Grad** über der Gefahrenzone. Eine
locker gepackte, halb leere oder zu große Kiste fällt hinein — und das Essen
steht dann stundenlang genau dort, wo sich Keime am schnellsten vermehren.
Diese Verbindung zieht das Handbuch selbst und sagt das im Text; die alten
Hefte kannten keine Keimzahlen und warnen an dieser Stelle nicht.

### Eine Empfehlung der Quellen wurde ausdrücklich widerrufen

Beide Hefte nennen **Asbest** als bestes Dämmmaterial und Asbestpappe als
Brandschutz. Das war 1917 Stand der Technik. Es wurde nicht stillschweigend
weggelassen, sondern steht als eigener Schritt mit Warnung in der Anleitung:
Asbest erzeugt Krebs, ist in der EU verboten, und alte Platten dürfen nicht
zersägt oder gebrochen werden. Wer die Originalhefte in die Hand bekommt,
soll wissen, warum hier abgewichen wird.

## 11.08.2026 — Abort (Bauanleitung, neue Gruppe „Hygiene und Abfall")

Neunzehnte Bauanleitung: `hygiene-abort-bauen`. Das feste, fliegendichte
Häuschen mit dichtem Behälter, für einen Ort, an dem Menschen Monate oder
Jahre bleiben. Dazu die erste Anleitung einer neuen Untergruppe.

### Quellen

- **US-Landwirtschaftsministerium, Farmers' Bulletin 463 „The Sanitary
  Privy"** (1911), von C. W. Stiles und L. L. Lumsden vom Hygienischen
  Laboratorium des US-Gesundheitsdienstes. Bauplan mit Maßen, Fliegenschutz,
  Entsorgungswege, die Temperatur- und Abstandszahlen. Volltext gelesen,
  abgerufen am 11.08.2026.
- **US-Landwirtschaftsministerium, Farmers' Bulletin 43 „Sewage Disposal on
  the Farm"** (1896), von Theobald Smith. Unabhängige zweite Schrift:
  Mindestabstand zum Brunnen, das Trockenerde-Verfahren, und die Begründung,
  warum tiefe Gruben schaden statt zu helfen.

### Zur Schutzfrist

Beide haben benannte Verfasser, also zählt das Todesjahr plus 70. Stiles ist
1941 gestorben (frei seit 2012), Smith 1934 (frei seit 2005). Auf
„US-Bundeswerk" wurde sich auch hier nicht verlassen.

### Eine dritte Quelle wurde deshalb NICHT benutzt

Für das Thema Fleisch pökeln lag „Killing Hogs and Curing Pork" (1917) bereit
und wäre inhaltlich gut gewesen. Verfasser ist F. G. Ashbrook, gestorben
**1966** — in Deutschland also bis 2036 geschützt. Das Heft wurde nicht
gelesen und nicht verwertet.

### Wo die beiden Quellen verschiedene Zahlen nennen

Der Mindestabstand zum Trinkwasser steht in beiden Heften, aber für
verschiedene Dinge: 30 Meter zwischen Brunnen und undichter Grube (1896),
90 Meter für vergrabene Flüssigkeit plus 60 cm Erde darüber (1911). Beide
Zahlen stehen in der Anleitung, mit dem Hinweis, welche wofür gilt, und dem
Rat, die größere zu nehmen. Für Kalkgestein und Karst sagt die Quelle von
1911 ausdrücklich, dass keine Abstandszahl mehr trägt — Verunreinigung kann
dort kilometerweit reichen. Auch das steht als Warnung im Text.

### Das Verhältnis zur bestehenden Latrinen-Regel

Der Tipp „Stuhl entsorgen ohne Toilette" nennt zehn Meter. Das ist kein
Widerspruch und wurde NICHT geändert: Dort geht es um eine Grube für Tage bis
wenige Wochen im Lager, hier um eine dauerhafte Anlage. Die Anleitung sagt
diesen Unterschied selbst und verweist auf den Tipp.

### Was das Handbuch hier von sich aus ergänzt

Der Fall „kein Brennstoff zum Erhitzen" kommt in beiden Heften nicht vor —
1896 und 1911 war Holz selbstverständlich da. Die Anleitung nennt dafür die
schlechtere Kette (lange stehen lassen, Kalk, tief und weit weg vergraben)
und kennzeichnet ausdrücklich, dass diese Einordnung nicht aus den Quellen
stammt und keine gleichwertige Lösung ist.

### Suchprobe

„plumpsklo" und „donnerbalken" führten nach dem ersten Einbau auf NICHTS —
die Anleitung kannte nur das Wort „Abort". Beide Alltagswörter stehen jetzt
im ersten Satz der Zusammenfassung, wo sie hingehören; danach führt jedes von
ihnen auf die Anleitung. „klo" führt weiterhin zuerst auf „Stuhl entsorgen
ohne Toilette" und erst dann auf die Anleitung, und das ist richtig herum:
Der Tipp sagt, was sofort zu tun ist, die Anleitung ist ein Bauvorhaben.

## 11.08.2026 — Dörre bauen (Bauanleitung)

Zwanzigste Bauanleitung: `nahrung-doerre-bauen`. Das Gerät zum Trocknen —
Horde, Hängerahmen über dem Herd, Schranktrockner — und die Betriebsregeln,
an denen es scheitert.

### Quellen

- **US-Landwirtschaftsministerium, Farmers' Bulletin 841 „Drying Fruits and
  Vegetables in the Home"** (Juni 1917). Ohne benannten Verfasser.
- **US-Landwirtschaftsministerium, Farmers' Bulletin 1918 „Drying Foods for
  Victory Meals"** (August 1942). Ebenfalls ohne benannten Verfasser.

Beides sind Behördenschriften ohne persönlichen Urheber und damit in
Deutschland 70 Jahre nach Erscheinen frei — 1917 und 1942 liegen beide
darüber hinaus. Zwei verschiedene Bulletins mit 25 Jahren Abstand, also zwei
unabhängige Belege und nicht zwei Auflagen derselben Schrift.

### Eine dritte, inhaltlich bessere Quelle wurde verworfen

Farmers' Bulletin 984 „Farm and Home Drying of Fruits and Vegetables"
(Fassung 1919) war schon heruntergelesen und ist die ausführlichste der
drei. Auf der Titelseite steht **Joseph S. Caldwell** als Verfasser. Sein
Todesjahr ließ sich nicht belegen, also ließ sich die Schutzfrist nicht
belegen. Die Schrift wurde deshalb NICHT verwertet — der Verzicht kostet
vor allem die Temperaturangaben je Obstsorte, die aber ohnehin in keine
Anleitung dieser Länge gepasst hätten.

### Wo die beiden Quellen verschiedene Zahlen nennen

Temperaturgrenze: 1917 nennt „in der Regel nicht über 60 bis 65 Grad, besser
deutlich darunter", 1942 einen Arbeitsbereich von etwa 52 bis 71 Grad, für
manches Gemüse aber nie über 65. Die Anleitung nennt 60 bis 65 Grad als
Obergrenze — die Schnittmenge beider Angaben. Die Umrechnung aus Fahrenheit
steht im Text als solche gekennzeichnet.

Nachhitzen vor dem Einlagern: 1917 nennt 60 Grad, 1942 zehn bis fünfzehn
Minuten bei 74 bis 82 Grad. Die Anleitung nimmt die höhere Angabe und sagt,
dass die ältere Quelle niedriger liegt.

### Was übernommen wurde, obwohl es unbequem ist

Beide Hefte empfehlen das Schwefeln von hellem Obst. Es steht in der
Anleitung — mit der Bedingung der Quelle selbst („im Freien arbeiten") und
einer eigenen Warnung dazu, sowie den beiden belegten Ersatzverfahren
(kurzes Dämpfen, Salzbad) für alle, die keinen Schwefel haben. Weggelassen
wurde es nicht, weil es ein echtes Verfahren ist und weil jemand, der die
alten Hefte findet, sonst ohne die Warnung dasteht.

### Was bewusst NICHT geschrieben wurde

Beide Quellen verweigern ausdrücklich eine Tabelle mit Trockenzeiten je
Lebensmittel — zu abhängig von Ware, Stückgröße, Gerät und Wetter. Diese
Lücke steht als Lücke in der Anleitung; geraten wurde nichts. An ihrer
Stelle steht die Trockenheitsprobe und die Fühlprobe für den Fall, dass kein
Thermometer da ist.

### Abgrenzung zum bestehenden Kapitel

„Ernte trocknen und trocken halten" (Agrikultur, Quelle Peace Corps) erklärt,
WARUM Trocknen wirkt und welche Restfeuchte welches Lagergut braucht. Die
neue Anleitung wiederholt das nicht, sondern verweist im ersten Absatz
darauf und bringt das Handwerk: Bau, Temperaturverlauf, Ausgleichen,
Nachhitzen, Lagern.

### Suchprobe

„doerren" führte nach dem ersten Einbau NICHT auf die neue Anleitung — der
Titel kennt nur „Dörre". „apfelringe", „dörrobst" und „suppengemüse"
führten auf nichts. Alle vier stehen jetzt im ersten Teil der
Zusammenfassung und führen auf die Anleitung. „doerren" führt weiterhin
zuerst auf „Vorrat für Monate: pökeln, räuchern, dörren" — das ist richtig
so, denn dort geht es um Fleisch.

## 11.08.2026 — Mistbeet und Frühbeet (Agrikultur-Kapitel)

Achtunddreißigstes Kapitel: `agrikultur-mistbeet`, Gruppe „Boden, Beet und
Pflege". Das Beet, das sich aus frischem Pferdemist selbst heizt, und der
Kasten daneben, der nur Sonnenwärme hält. Beides zusammen verlängert das
Gartenjahr an beiden Enden — ohne Brennstoff, der sonst zum Kochen fehlt.

### Quellen

- **US-Landwirtschaftsministerium, Farmers' Bulletin 818 „The Small Vegetable
  Garden"** (April 1917). Ohne benannten Verfasser, nur „Prepared under the
  Direction of the Bureau of Plant Industry" — der Bureauleiter ist kein
  Urheber. Damit 70 Jahre nach Erscheinen frei.
- **FAO, „A vegetable garden for all"**, Kapitel „Seedlings and transplants".
  Nur für zwei Stellen benutzt und dort ausdrücklich als solche
  gekennzeichnet: die Deckel- und Luftregel nach dem Keimen und der Zeitpunkt
  zum Verpflanzen.

### Dieses Kapitel steht auf EINER Hauptquelle, und das steht auch drin

Bau, Maße und die Temperaturschwelle vor der Aussaat stammen allein aus
Bulletin 818. Ein zweites Heft mit belegbarer Schutzfrist war nicht zu
finden. Der Grund ist bemerkenswert und lohnt die Notiz für später: Die
Gartenbau-Hefte dieser Behörde aus den Jahren 1900 bis 1930 stammen fast
alle von **W. R. Beattie** oder **James H. Beattie**, deren Todesjahre nicht
zu belegen waren. Betroffen und deshalb NICHT benutzt: FB 255 „The home
vegetable garden", FB 1044 „The city home garden", FB 1233 „Tomatoes for
canning", FB 354 „Onion culture", FB 1269 „Celery growing". Ebenso
ausgeschieden: FB 936 „The city and suburban vegetable garden" (H. U.
Conolly), FB 76 „Tomato growing" (Voorhees), FB 45 „Some insects injurious to
stored grain" (Chittenden), FB 960 „Neufchâtel and cream cheese" (Matheson
und Cammack) und FB 976 „Cooling milk and cream on the farm" (Gamble).

Die Einzelquelle ist im Kapitel benannt, im Abschnitt „Was dieses Kapitel
nicht hergibt".

### Ein Werkzeug ist aus diesem Tag entstanden

`work/werkzeuge/quellen_sichten.py` holt für jeden Kandidaten ZUERST die
Titelseite und meldet, ob dort ein Mensch als Verfasser steht. Anlass war,
dass der Katalog von archive.org bei FB 960, 976 und 1233 „United States.
Department of Agriculture" meldet, während auf der Titelseite ein Name steht.
Dreimal ist an diesem Tag Arbeit weggeworfen worden, bevor das Werkzeug
stand — einmal sogar erst nach dem vollständigen Lesen (FB 984, Caldwell).

### Was das Kapitel offen lässt

Drei Lücken stehen als Lücken im Text: kein Ersatz für Pferdemist (die Quelle
nennt keinen), keine Angabe, wie viele Wochen die Wärme hält, und keine
Pflanztermine (die der Quelle sind auf US-Klimazonen bezogen).

### Suchprobe und ein Beinahe-Unfall

Der erste Titel hieß „Mistbeet und Kaltkasten: Wärme ohne Brennstoff". Der
Titelwächter hat ihn abgelehnt, und die Notfall-Suchprüfung hat gezeigt, warum
das kein Formalismus ist: Mit diesem Titel führte „wärme" ZUERST auf das
Gartenkapitel statt auf „Wärme erhalten", und „kalt" verdrängte „Unterkühlung
Stadium I" aus den ersten acht Treffern. Ein Gartenkapitel hätte zwei
Notfall-Tipps überdeckt.

Titel jetzt „Mistbeet und Frühbeet: der Garten fängt Wochen früher an", und
das Wort „Kaltkasten" steht nur noch EINMAL im Text, als Zweitname. Nach der
Änderung führt „wärme" wieder zuerst auf „Wärme erhalten" und „kalt" wieder
auf die Unterkühlungs-Tipps.

„setzlinge" und „jungpflanzen" führten zunächst auf nichts beziehungsweise
nicht hierher; beide stehen jetzt im ersten Satz des Abschnitts über die drei
Geräte. „mistbeet", „frühbeet" und „garten früher" führen auf das Kapitel.

## 11.08.2026 — Sandfilter (Bauanleitung, neue Gruppe „Wasser aufbereiten")

Einundzwanzigste Bauanleitung: `wasser-sandfilter-bauen`, und die achte
Untergruppe. Ein Behälter mit Kies und Sand, durch den Wasser langsam
sickert; nach etwa einem Monat wächst obenauf eine unsichtbare Schicht aus
Kleinstlebewesen, die die eigentliche Reinigung leistet.

### Quelle

- **CAWST (Centre for Affordable Water and Sanitation Technology), „Biosand
  Filter Manual: Design, Construction, Installation, Operation and
  Maintenance", 2009.** Ausdrücklich offener Inhalt unter **Creative Commons
  Attribution 3.0** — die Lizenz steht im Heft selbst und erlaubt die
  Weiterverwendung mit Nennung. Volltext gelesen, gesichert unter
  `work/quellen/bauanleitungen/`.

Dasselbe Handbuch trägt schon den Tipp „Trinkwasser richtig lagern"; es lag
seit dem 02.08.2026 gelesen im Projekt, ohne dass die Bauanleitung daraus
je geschrieben worden wäre.

### Diese Anleitung steht auf EINER Quelle, und das steht im Text

Ein zweites, unabhängiges Handbuch mit derselben Bauweise wurde nicht
gefunden. Der letzte Schritt sagt das ausdrücklich und zieht die Folgerung:
Filterwasser wird IMMER zusätzlich entkeimt — die Anleitung verweist dafür
auf die vorhandenen Tipps zu Abkochen, Chlor und SODIS.

Das ist auch inhaltlich richtig und nicht nur ein Notbehelf: Die
Wirkungszahlen der Quelle streuen bei Viren zwischen 70 und über 99 von
hundert. Ein Filter, dessen Wirkung gegen Viren zwischen „fast alles" und
„fast nichts" liegen kann, darf nicht als letzter Schritt stehen.

### Was diese Anleitung an Fehlern abfängt

Drei Dinge, die eine falsch gebaute Anlage schlimmer machen als gar keine,
stehen deshalb als Warnungen im Text:

1. **Flusssand kann das Wasser schlechter machen, als es war.** Er bringt
   organisches Material mit, und das ist Futter für Erreger, die sich dann im
   Filter vermehren. Sonne und Chlor töten die Keime, entfernen das Futter
   aber nicht.
2. **Der Sand wird absichtlich NICHT sauber gewaschen.** Wer bis zur Klarheit
   wäscht, spült die feinsten Körner heraus; dann läuft das Wasser zu schnell
   durch und der Filter reinigt nicht mehr. Die Durchflussprobe (höchstens
   0,4 Liter je Minute) ist die Abnahmeprüfung.
3. **Nie Chlor in den Filter gießen.** Es tötet genau die Lebewesen, die die
   Reinigung leisten.

### Suchprobe

„sandfilter" und „filter bauen" führen auf die Anleitung. „schmutziges
wasser" führte zuerst auf NICHTS und steht jetzt im ersten Satz der
Zusammenfassung.

Der erste Titel hieß „Sandfilter bauen: **Wasser** reinigen ohne Brennstoff"
und wurde vom Titelwächter abgelehnt: Das Wort „Wasser" gehört dem Tipp
„Trübes Wasser absetzen und filtern", und die Anleitung hätte ihn von Platz
eins verdrängt. Titel jetzt „Sandfilter bauen: trinkbar machen ohne
Brennstoff"; danach steht die Rangfolge für „wasser" wieder wie vorher.

### Ein Einbaufehler, den die Prüfung gefangen hat

Beim Einfügen hat ein Skript in fünf Anführungszeichen den eingeschlossenen
Text durch ein Steuerzeichen ersetzt — aus „Wasser abkochen" wurde ein
unsichtbares Byte. Die Paketprüfung meldete `step-invalid` („unlesbar"). Ohne
diese Prüfung wären vier Verweise auf andere Tipps zu leeren
Anführungszeichen geworden, und im Text hätte gestanden: „Wie das geht, steht
in den Tipps ‚‘, ‚‘ und ‚‘."

## 11.08.2026 — Lasten heben und verankern (Bauanleitung)

Zweiundzwanzigste Bauanleitung: `seilwerk-heben-und-verankern`, Gruppe
„Seil, Schnur und Knoten". Sie schließt eine Lücke, die seit dem 28.07.2026
offen war: Das Paket erklärte den Flaschenzug, aber nicht, WORAN man ihn
hängt und WOGEGEN man zieht.

### Quelle

- **US-Heer, Field Manual FM 5-125 „Rigging Techniques, Procedures, and
  Applications"** (Change 1, 23.02.2001), Kapitel 4 „Anchors and Guy Lines"
  und Kapitel 5 Abschnitt I „Lifting Equipment". Werk der US-Bundesregierung.
  Volltext gelesen; dieselbe Vorschrift trägt bereits die Flaschenzug-
  Anleitung.

### Was NICHT übernommen wurde, und warum das die größte Lücke ist

Das Heft enthält Tabellen zur Haltekraft von Pfahlankern, zur Tragfähigkeit
von Fichtenstangen als Bockmast und Rechenformeln für den Erdanker. Sie
stehen in Fuß, Zoll und Pfund je Quadratfuß, hängen an amerikanischen Holz-
und Seilsorten und kamen aus der Texterkennung nur bruchstückhaft heraus.
**Sie wurden nicht umgerechnet und nicht geraten.**

Damit sagt die Anleitung nicht, wie schwer die Last sein darf. Das steht so
im letzten Schritt. Was sie stattdessen gibt, ist durchweg die vorsichtige
Seite: mehrere Pfähle statt eines dicken, Erdanker für alles Dauerhafte,
Stangenlänge höchstens sechzigmal die Dicke, Belastungsprobe mit
ansteigender Last und niemandem in der Gefahrenlinie.

### Vier Sätze, die aus der Quelle stammen und Leben retten können

1. Der Anker muss die **Bruchlast des Seils** aushalten, nicht die Last.
2. Am Anker wird **dicht über dem Boden** angeschlagen — höher entsteht ein
   Hebel, der ihn herauskippt.
3. **Nie an totem oder morschem Holz** anschlagen: Es bricht plötzlich, ohne
   das Knacken, an dem man sonst merkt, dass es zu viel wird.
4. Beim Zweibock **nie die Neigung verstellen, solange eine Last hängt.**

### Drei Prüfungen haben beim Einbau angeschlagen

- Der Querverweis-Wächter: Ich hatte den Titel der Flaschenzug-Anleitung aus
  dem Gedächtnis geschrieben („Flaschenzug bauen und einscheren"), und den
  gibt es nicht.
- Danach ein zweites Mal derselbe Wächter: Der Titel war jetzt wörtlich
  richtig, aber mit **Halbgeviertstrich statt Geviertstrich**. Ein Verweis,
  der sich um ein Zeichen unterscheidet, führt nirgendwohin — und im
  Fließtext sieht man den Unterschied nicht.
- Die Suchprobe: „balken bewegen" und „abspannen" führten auf nichts. Beide
  stehen jetzt in der Zusammenfassung, zusammen mit den Alltagsfällen (Motor
  ausbauen, Stein aus dem Weg heben, Dach aufrichten).

## 11.08.2026 — Brot backen (Agrikultur-Kapitel)

Neununddreißigstes Kapitel: `agrikultur-brot`, Gruppe „Ernte, Vorrat und
Verarbeitung". Das Paket erklärte, wie man Korn zu Mehl macht — und hörte
dort auf.

### Quellen

- **Farmers' Bulletin 807 „Bread and Bread Making in the Home"** (April 1917).
  Ohne benannten Verfasser auf der Titelseite, nur „Contribution from the
  States Relations Service, A. C. True, Director" — ein Amtsleiter ist kein
  Urheber. Damit 70 Jahre nach Erscheinen frei.
- **Farmers' Bulletin 565 „Corn Meal as a Food and Ways of Using It"**
  (Fassung 1921), von C. F. Langworthy (gestorben 1932) und Caroline L. Hunt
  (gestorben 1927). Beide über die Personenfrist hinaus frei. Langworthys
  Todesjahr war schon für die Kochkisten-Anleitung geprüft.

Beide lagen seit einer früheren Nacht ungenutzt unter `work/quellen/agrikultur/`.

### Zwei Sätze, die den Unterschied zu jedem Kochbuch ausmachen

**Die schnellste Temperatur ist die schlechteste.** Hefe arbeitet am
schnellsten bei etwa 30 Grad — aber die Bakterien im Teig auch, und die
machen das Brot sauer. Deshalb die Staffel: 30 Grad nur, wenn der ganze
Ablauf ohne Verzögerung durchläuft; im Haushalt 24 bis 27; über Nacht 20 bis
21, sicherer 18.

**Was die Hefe erzeugt, geht im Ofen weg — was Bakterien erzeugen, bleibt.**
Kohlendioxid und Alkohol verfliegen in der Hitze; sauer und ranzig backt man
nicht heraus. Das ist die Begründung für alle Sauberkeitsregeln des Kapitels
und für die Ablehnung des alten Brauchs, warmes Brot in Tücher zu wickeln.

### Das Triebmittel ohne Laden

Die Quelle nennt zwei Wege: ein Stück Teig vom letzten Backen aufheben, und
flüssige Hefe aus Kartoffeln (Rezept im Kapitel, hält kühl etwa zwei Wochen).

**Beide brauchen einen Anfang.** Wie man ganz ohne Hefe und ohne Teigrest
einen ersten Ansatz zieht, steht in keiner der beiden Schriften. Das Kapitel
sagt das ausdrücklich und stellt zugleich klar, dass der beschriebene
Teigrest NICHT das ist, was man heute Sauerteig nennt — die Quelle hebt
Hefeteig auf und beschreibt kein Ansäuern. Geraten wurde nichts.

### Weitere benannte Lücken

Kein Backofenbau, keine Zahlen zur Haltbarkeit des fertigen Brotes, und keine
Rezepte für andere Getreide (die des Bulletins hängen an amerikanischen
Mehlsorten; übernommen wurde nur, was sortenunabhängig ist).

### Ein Einbaufehler, den die Prüfung gefangen hat

Ein nachlaufendes Komma hinter einer Klammer machte aus dem Text eines
Abschnitts ein Tupel — in der JSON-Datei stand danach eine Liste, wo eine
Zeichenkette hingehört. Die Paketprüfung meldete `json-invalid` mit
Fundstelle. Ein Abschnitt, der als Liste geschrieben ist, wäre in der App
gar nicht erschienen.

### Suchprobe

„brot", „brot backen", „teig", „kneten" und „hefe selbst machen" führen auf
das Kapitel. „sauerteig" führte zuerst auf NICHTS — das Wort steht jetzt in
der Klarstellung, wo es hingehört: als Abgrenzung, nicht als Versprechen.

## 11.08.2026 — Floß bauen (Bauanleitung)

Dreiundzwanzigste Bauanleitung: `gelaende-floss-bauen`, Gruppe „Gelände und
Wasser überwinden" — die bis dahin einen einzigen Eintrag hatte.

### Quelle

- **US-Heer, FM 3-05.70 „Survival"** (17.05.2002), Kapitel 17 „Water
  Crossings", Abschnitte „Rafts" und „Log Raft". Dieselbe Vorschrift trägt
  bereits „Einen Fluss durchqueren".

### Warum das kein zweiter Aufguss ist

Die vorhandene Anleitung sagt, wie ein MENSCH hinüberkommt: Stelle wählen,
furten, Schwimmhilfen. Die Rafts-Abschnitte desselben Kapitels waren dabei
NICHT übernommen worden — und sie lösen ein anderes Problem: Wie kommt das
GEPÄCK trocken hinüber. Nasser Schlafsack und nasse Zündmittel entscheiden
oft mehr als die Querung selbst.

Vier Bauweisen mit den Tragzahlen der Quelle: Buschfloß etwa 115 kg,
Rollfloß etwa 35 kg, Ringfloß (nur eine Plane nötig), Stammfloß mit
Klemmbalken.

### Die Grenze, die als Warnung im Text steht

Alle Planenflöße tragen **nicht** das volle Gewicht eines Menschen. Die
Quelle ist dort eindeutig: Man schwimmt und schiebt sie vor sich her. Wer
sich darauf setzt, geht mitsamt der Ausrüstung unter. Dazu die zweite
Warnung: Nicht jedes Holz schwimmt — die Quelle nennt Palme als Beispiel für
Holz, das auch trocken sinkt.

### Suchprobe

„floss" und „gepäck trocken" führen auf die neue Anleitung. „fluss" und
„über den fluss" führen weiterhin ZUERST auf „Einen Fluss durchqueren", und
das ist richtig herum: Wer am Ufer steht, braucht zuerst die Stelle und die
Kältegrenze, nicht den Floßbau.

## 11.08.2026 — Ein Schwein schlachten (Agrikultur-Kapitel)

Vierzigstes Kapitel: `agrikultur-schwein-schlachten`, Gruppe „Schlachten und
Verwerten" — die bis dahin dünnste Gruppe mit zwei Einträgen.

### Quelle

- **B. Heller & Company, Chicago: „Heller's Secrets of Meat Curing and Sausage
  Making"**, achte, vollständig neu bearbeitete Auflage 1929 (erste Auflage
  1904). Auf der Titelseite steht **keine natürliche Person**, nur die Firma —
  anonymes Werk, damit 70 Jahre nach Erscheinen frei. Titelseite selbst
  gelesen, nicht dem Katalog geglaubt.

### Warum ausgerechnet ein Firmenheft

Für das Schweineschlachten war **keine** Behördenschrift mit belegbarer
Schutzfrist zu finden. Alle einschlägigen USDA-Hefte tragen benannte
Verfasser, deren Todesjahr offen ist: Ashbrook und Anthony („Killing Hogs and
Curing Pork", „Pork on the Farm"), Potts (Lamm), Black (Rind), Warner
(spätere Fassungen). Das Firmenheft war die einzige freie Quelle mit
brauchbarem Inhalt.

### Die Stelle, an der das Handbuch der Quelle ausdrücklich widerspricht

Die Quelle schreibt wörtlich, Schweine müssten vor dem Stechen **nicht**
betäubt werden, und hält das Stechen am hängenden, lebenden Tier sogar für
das bessere Verfahren.

Das ist **nicht übernommen**. Im Kapitel steht ein eigener Abschnitt, der
sagt: Es wird betäubt, immer — es ist in Deutschland verboten, ein
warmblütiges Tier ohne Betäubung zu töten, und es ist unnötiges Leiden. Die
Einordnung ist ausdrücklich als die dieses Handbuchs gekennzeichnet.

Dass die Quelle für die Betäubung selbst nur die Rinder-Anleitung hergibt
(Schlag mitten auf die Stirn, wo sich die Linien von den Hornansätzen zu den
Augen kreuzen), steht ebenso im Text — samt dem Satz, dass alles Weitere dort
NICHT steht und hier nicht erraten wird.

### Was sonst bewusst nicht übernommen wurde

- **Die Werbung.** Das Heft empfiehlt auf fast jeder Seite eigene Erzeugnisse
  (ein Brühmittel, ein Pökelmittel). Nichts davon steht im Kapitel; wo die
  Firma ihr Mittel anpreist, geht es um hartes Wasser und Schmutz auf der
  Haut, und das erledigen heißes Wasser und Schaben auch.
- **Die Pökelrezepte.** Hunderte Formeln mit Salpeter und Fertigmischungen.
  Bei Pökelsalzen entscheidet die Menge über Gift oder Genuss; ein einzelnes
  Firmenheft von 1929 ist dafür kein ausreichender Beleg.
- **Die Großbetriebs-Teile** (Brühtunnel, Kratzmaschinen, Laufschienen).

### Einzelquelle, und das steht im Text

Brühtemperatur, Stichtiefe und Kühltemperatur stehen ohne Gegenprobe. Das
Kapitel sagt selbst, wo das zu verschmerzen ist und wo nicht: Beim Abkühlen
nicht — Fleisch, das zu langsam kalt wird, verdirbt von innen, und außen
sieht man ihm nichts an.

### Ein Einbaufehler, den die Prüfung gefangen hat

Der erste Entwurf war versehentlich in **ASCII-Umschrift** geschrieben
(„Pruefung", „poekeln", „Bruehen"). Aufgefallen ist es dem
Querverweis-Wächter: Die zitierten Titel anderer Einträge passten nicht mehr.
Der Entwurf wurde verworfen und das Kapitel neu geschrieben; das
Einbau-Skript prüft jetzt selbst auf Umschrift, bevor es schreibt.

### Suchprobe

„schwein schlachten", „brühen", „ausbluten" und „borsten" führen auf das
Kapitel. „blutung" führt unverändert zuerst auf „Blutung stillen" — der
Notfall-Tipp wurde nicht verdrängt.

---

# OFFENER PUNKT VOR JEDER VERÖFFENTLICHUNG: die Begründung „US-Bundeswerk"

**Aufgefallen am 11.08.2026.** Das ist kein Einzelfall, sondern eine Frage,
die quer durch das ganze Paket geht, und sie muss ausdrücklich entschieden
werden.

## Der Befund

**162 Quellenangaben** im Paket tragen als Lizenzbegründung sinngemäß „Werk
einer US-Bundesbehörde, gemeinfrei". Bei rund zwanzig davon steht auf der
**Titelseite ein Mensch mit Namen**. Selbst nachgesehen und bestätigt:

| Eintrag | Quelle | Verfasser auf der Titelseite |
|---|---|---|
| `agrikultur-brunnen` | FB 1978 „Safe Water for the Farm" (1948) | Harry L. Garver |
| `agrikultur-brunnen` | FB 1448 „Farmstead Water Supply" (1933) | George M. Warren |
| `agrikultur-brunnen` | FB 1859 „Stock-Water Developments" (1940) | C. L. Hamilton |
| `agrikultur-brunnen` | FB 2237 „Water-Supply Sources" (**1978**) | Behörde, aber Jahr 1978 |
| `agrikultur-obstbaeume` | mehrere | Corbett, Gould, Yerkes, Fletcher |
| `agrikultur-bienen` | mehrere | Phillips, Demuth, Sechrist, Michael |
| `agrikultur-zaun` | mehrere | Kelley, Blew |
| `agrikultur-tiergesundheit` | mehrere | Bunyea, Gallagher |
| `agrikultur-roggen` | FB zu Roggen | Clyde E. Leighty |
| `agrikultur-duenger` | FB zu Stallmist | W. H. Beal |
| `agrikultur-beeren` | mehrere | Corbett |

Für die meisten dieser Namen ist **kein Todesjahr belegt**. Nach der Regel,
die dieses Projekt sich selbst gegeben hat, heißt das: **nicht nutzbar**,
nicht „frei".

## Warum das nicht dasselbe ist wie „das Paket verletzt Urheberrecht"

Fair muss man sagen: In den USA sind diese Hefte tatsächlich nicht
geschützt — Werke der Bundesregierung stehen nach 17 U.S.C. § 105 von
vornherein außerhalb des Urheberrechts. Ob ein deutsches Gericht daraus
folgert, dass auch hier nichts zu schützen ist, ist juristisch umstritten.
Es gibt gute Argumente dafür, dass ein Werk, das in seinem Ursprungsland nie
Schutz hatte, auch hier keinen genießt.

**Das Projekt hat sich aber ausdrücklich für den vorsichtigen Weg
entschieden** — die Merkzettel-Regel steht weiter oben in dieser
Datei: bei alten Werken zählt Todesjahr plus 70, und „US-Bundeswerk" allein
trägt nicht. An dieser Regel gemessen sind die oben genannten Einträge nicht
gedeckt.

## Was NICHT betroffen ist

Die große Mehrheit der 162 Stellen ist unproblematisch: Feldvorschriften des
US-Heers (FM 3-05.70, FM 5-125) und die vielen Farmers' Bulletins **ohne**
benannten Verfasser sind Behörden- oder anonyme Werke; für sie gilt
Erscheinungsjahr plus 70, und das ist bei allen abgelaufen. Ebenfalls sauber:
Stiles (gestorben 1941), Smith (1934), Langworthy (1932), Hunt (1927),
Voorhees (1911) — dort ist das Todesjahr belegt.

## Die drei Wege — die Entscheidung steht aus

1. **Beim vorsichtigen Weg bleiben.** Für jeden benannten Verfasser das
   Todesjahr belegen. Wo es sich nicht belegen lässt, die Quelle ersetzen
   oder den Eintrag zurückbauen. Das ist viel Arbeit und kostet Inhalt.
2. **Die Regel für US-Bundeswerke ausdrücklich ändern** — mit der Begründung
   aus § 105 — und das hier festhalten. Dann bleibt alles, wie es ist.
3. **Anwaltlich klären lassen**, bevor veröffentlicht wird.

Bis dahin: **Dieses Paket wird nicht veröffentlicht.** Der Punkt steht neben
der offenen Lizenzfrage zum Phrasenbuch des Britischen Roten Kreuzes.

## 11.08.2026 — Den Meiler bauen (Bauanleitung)

Vierundzwanzigste Bauanleitung: `werkzeug-holzkohle-meiler`. Holzkohle ist
der Brennstoff, ohne den Schmiedefeuer, Kalkbrennen und Löten nicht gehen.

### Quellen

- **US-Landwirtschaftsministerium, Forest Products Laboratory: „The
  Production of Charcoal in the Ordinary Pit-Kiln"** (Juni 1932). Kein
  persönlicher Verfasser auf der Titelseite, nur die Behörde.
- **Frederick Overman, „The Manufacture of Iron"**, dritte Auflage 1854,
  Kapitel „Charring of Wood". Verfasser 1803 bis 1852, Schutzfrist 1922
  abgelaufen. 78 Jahre älter als die erste Quelle und aus einem anderen
  Gewerbe — eine echte zweite Linie.

Eine dritte Quelle (Connecticut Bulletin 448 von 1941) wurde geprüft und
verworfen: zwei benannte Verfasser ohne belegtes Todesjahr. Das Sichtwerkzeug
hatte sie fälschlich als anonym gemeldet.

### Die erste Zahl, die am Seitenbild geprüft wurde

Die Meilergröße stand in der Texterkennung als „from 15 to ty-5 cords". Auf
der abfotografierten Seite steht **„from 15 to 45 cords"**. Das ist der erste
Anwendungsfall der Regel vom selben Tag: Zahlen werden am Bild geprüft, nicht
am erkannten Text.

### Was die Anleitung von sich aus ergänzt

**Die Kohlenmonoxid-Warnung.** Beide Quellen erwähnen das Gas mit keinem
Wort — 1854 und 1932 war die Gefahr nicht beschrieben. Ein Meiler erzeugt es,
und fertige Holzkohle erzeugt es beim Verbrennen im geschlossenen Raum. Die
Warnung steht im ersten Schritt und ist ausdrücklich als Einordnung dieses
Handbuchs gekennzeichnet, mit Verweis auf die vorhandenen CO-Tipps.

Ebenso von hier: alle metrischen Umrechnungen (die Quellen rechnen in Fuß,
Zoll, Fuhren und Bushel).

### Was die Anleitung ehrlich offen lässt

Der erste Schritt zitiert die Quelle von 1932 mit ihrem eigenen Eingeständnis:
Die richtige Führung eines Meilers verlange erhebliche Erfahrung, und es sei
schwierig, eine Anleitung zu geben, mit der eine unerfahrene Person Erfolg
hat. Dazu: keine Temperaturangaben in beiden Quellen.

### Der Sicherheitsfund beim Einbau

Der erste Titel hieß „Holzkohle brennen: der Meiler". Damit stand die
Anleitung bei der Suche nach **„holzkohle"** auf Platz eins — vor dem Tipp
„Kohlenmonoxid: unsichtbar und geruchlos". Wer dieses Wort tippt, steht
möglicherweise gerade in einem Raum mit einem glimmenden Grill; eine
Bauanleitung hilft ihm dort nicht.

Gefunden hat das die vorhandene Gas-Wort-Prüfung. Sie ist dabei allerdings
mit „Key … is missing in the map" abgestürzt, statt den Grund zu nennen —
sie ging davon aus, dass der erste Treffer immer ein Tipp ist. Die Prüfung
sagt jetzt im Klartext, was zu tun ist. Titel ist jetzt „Den Meiler bauen:
aus Holz wird Kohle"; danach steht der CO-Tipp bei „holzkohle" und
„holzkohle grill" wieder auf Platz eins.

## 11.08.2026 — Kalk löschen und verwenden (Agrikultur-Kapitel)

Einundvierzigstes Kapitel: `agrikultur-kalk`, Gruppe „Hof, Werkstatt und
Haushalt". Aus dem Zulieferbetrieb.

### Quellen

- **William Millar, „Plastering, Plain and Decorative"** (1897), Kapitel
  „Materials". Verfasser gestorben 1904, Schutzfrist 1974 abgelaufen.
- **National Lime Association, „Whitewash and Cold Water Paints"** (1955),
  Bulletin 304-G. Kein Mensch als Verfasser genannt — 70 Jahre nach
  Erscheinen frei, also seit 2025.

Verworfen: USDA-Heft „Making Lime on the Farm" (1938) — inhaltlich der beste
Treffer zum Brennen, aber mit benanntem Verfasser ohne belegtes Todesjahr.
Deshalb sagt das Kapitel im ersten Absatz ausdrücklich, dass es das BRENNEN
nicht erklärt.

### Eine Zahl aus der Quelle wurde bewusst weggelassen

Millar gibt für das Löschen an: 28 Pfund Kalk verbänden sich mit 91 Pfund
Wasser zu 37 Pfund Kalkhydrat. **Diese Zahlen gehen nicht auf** — 28 und 91
sind zusammen 119, nicht 37, und das im selben Satz genannte Verhältnis
„nahezu drei zu eins" passt zu keiner der drei. Ob die Texterkennung Ziffern
verdorben hat oder schon der Druck von 1897 fehlerhaft war, ließ sich nicht
entscheiden. Das Kapitel hat dafür einen eigenen Abschnitt: Eine Zahl, die
sich selbst widerspricht, wird weder übernommen noch zurechtgerechnet.

### Die Verätzungswarnung stammt von diesem Handbuch

Beide Quellen nennen den Stoff ätzend und beschreiben die heftige, heiße
Reaktion beim Löschen — aber **keine warnt vor der Gefahr für Haut und
Augen**. Der entsprechende Abschnitt ist deshalb ausdrücklich als Einordnung
dieses Handbuchs gekennzeichnet und verweist auf den Tipp „Verätzung: spülen,
bis der Schmerz nachlässt".

### Suchprobe und ein zweiter Beinahe-Unfall

Die Zwischenüberschrift hieß zuerst „Die **Gefahr**, die in keiner der beiden
Quellen steht". Zwischenüberschriften wiegen in der Suche so schwer wie
Schlagwörter — damit stand das Kalk-Kapitel bei „gefahr" vor dem Tipp
„Vergiftung über die Atemwege". Überschrift geändert; danach steht der
Notfall-Tipp wieder vorn, ebenso bei „verätzung".

„weißeln" führte auf nichts. Die Alltagswörter (tünchen, weißeln, kalken,
schlämmen) stehen jetzt im ersten Satz des Tünche-Abschnitts.

## Entscheidung zum offenen Punkt „US-Bundeswerk" (11.08.2026)

Die Entscheidung war hier zu treffen, mit einer Vorgabe: **Die App soll
veröffentlicht werden können, quelloffen.** Danach richtet sich, was folgt.

### Die Entscheidung: die Werke bleiben, die Begründung wird ersetzt

Die bisherige Begründung „Werk einer US-Bundesbehörde, also gemeinfrei" war
zu kurz — sie nennt US-Recht und begründet damit nichts für Deutschland. Die
tragfähige Begründung ist eine andere und steht ab jetzt hier:

**Der Schutzfristenvergleich.** Für Werke, deren Ursprungsland außerhalb der
EU liegt, gewährt Deutschland keinen längeren Schutz als das Ursprungsland
selbst (§ 121 Abs. 4 UrhG in Verbindung mit Artikel 7 Absatz 8 der Revidierten
Berner Übereinkunft). Werke der US-Bundesregierung sind nach 17 U.S.C. § 105
in den USA **von vornherein und dauerhaft ohne Urheberrecht** — die Schutzdauer
im Ursprungsland ist null. Damit ist sie auch hier null.

Das ist dieselbe Begründung, auf der große quelloffene Sammlungen ihre
Bestände an US-Behördenwerken führen. Sie trägt, und sie ist überprüfbar.

### Wo diese Begründung NICHT gilt — und das ist die eigentliche Arbeit

**§ 105 gilt nur für den BUND, nicht für die Bundesstaaten.** Werke von
`State Boards of Health`, `State Geological Surveys`, `State Departments of
Labor` und den Landes-Landwirtschaftshochschulen sind davon NICHT erfasst;
sie konnten sehr wohl geschützt sein. Für sie gilt die gewöhnliche Prüfung.

Betroffen sind unter anderem Quellen, die in den Belegmappen dieses Tages
aufgetaucht sind: „The Sanitary Privy" (Kansas State Board of Health, 1945),
„Specifications for a Pit Privy" (North Carolina, 1939), „Sanitary Standards
for the Felt Hatting Industry" (New Jersey Department of Labor, 1915), „The
Clays and Clay Industry of New Jersey" (1904). Jede einzelne braucht ihre
eigene Begründung — Erscheinungsjahr plus 70 bei fehlendem Verfasser, sonst
Todesjahr plus 70.

Ebenso NICHT erfasst sind alle privaten Werke: Heller (1929), National Lime
Association (1955), Fitz (1928), Binns, Overman, Millar, Child, Kephart,
Mairet, Randall, Mason, Matthews, Wissler, Grinnell, Leno, Morfit. Bei diesen
ist die Prüfung bereits geführt und in den jeweiligen Abschnitten belegt.

### Was daraus als Aufgabe folgt

1. **Jede Quellenangabe des Pakets bekommt eine Einordnung** in eine von drei
   Klassen: `US-Bundeswerk` (Schutzfristenvergleich), `anonym oder
   Körperschaft` (Erscheinen + 70) oder `benannter Verfasser` (Tod + 70, mit
   Jahr). Wo keine der drei belegbar ist, fliegt die Quelle raus.
2. Die alte Formel „gemeinfrei als Werk einer US-Bundesbehörde" wird überall
   durch die neue Begründung ersetzt.
3. Das Ergebnis kommt in eine eigene Datei neben das Paket, damit ein
   Außenstehender es prüfen kann, ohne den Quelltext zu lesen.

### Was weiterhin die Veröffentlichung blockiert

Nur noch ein Punkt: **das Phrasenbuch des Britischen Roten Kreuzes** im
Übersetzer. Das wird direkt beim Rechteinhaber geklärt. Bis dahin ist der
Übersetzer der einzige Teil, der einer Veröffentlichung im Weg steht — nicht
mehr das ganze Paket.

### Und die Trennung, die eine quelloffene Veröffentlichung braucht

**Programm und Inhalt sind zwei verschiedene Dinge.** Der Programmtext kann
unter eine gewöhnliche quelloffene Lizenz. Die Texte des Pakets sind
Eigenleistung — sie sind aus den Quellen NEU geschrieben, nicht übernommen;
Tatsachen selbst sind nicht geschützt. Sie können deshalb unter eine
Lizenz gestellt werden, die Weitergabe und Veränderung erlaubt, solange die
Quellenangaben mitlaufen. Welche genau, ist noch zu entscheiden; die
Belegliste in dieser Datei ist die Grundlage dafür.

## BERICHTIGUNG derselben Entscheidung, noch am 11.08.2026

Ich habe oben zu streng berichtet, und das gehört richtiggestellt, weil es
eine offene Entscheidung betrifft.

### Der Fehler in meiner Darstellung

Ich habe „diese Quelle ist geschützt" behandelt, als hieße das „diese Quelle
darf nicht benutzt werden". **Das stimmt nicht.** Es sind zwei verschiedene
Fragen:

* **Übernehmen wir geschützten AUSDRUCK?** Wörtlichen Text, eine enge
  Übersetzung, eine Abbildung, eine ganze Tabelle. Nur dann muss die Quelle
  frei sein.
* **Oder nehmen wir nur TATSACHEN und schreiben selbst?** Tatsachen, Zahlen,
  Maße und Arbeitsverfahren sind **nicht** urheberrechtlich geschützt. Wer
  sie aus einem geschützten Buch lernt und in eigenen Worten aufschreibt,
  braucht keine Lizenz. Die Quellenangabe verlangt dieses Projekt aus einem
  anderen Grund: damit jede Aussage nachprüfbar ist.

### Die Gegenprobe

Eine maschinelle Prüfung über das ganze Paket hat nach längeren wörtlich
übernommenen englischen Passagen gesucht — in Tipps, Anleitungen und
Kapiteln. **Ergebnis: null.** Der Text des Pakets ist durchgehend eigene
deutsche Prosa; wo zitiert wird, sind es kurze, ausgewiesene Wendungen.

Damit ist die Lage: **Der Rechtsstand einer Quelle blockiert die
Veröffentlichung des Textes nicht.** Auch die modernen Quellen (WHO, RKI,
AWMF-Leitlinien, Fachbücher wie Alton/Alton 2015, Hesperian, CAWST) sind als
Belege völlig unproblematisch — sie werden zitiert, nicht abgedruckt.

### Wo die Lizenzfrage bleibt, und da bleibt sie hart

1. **Abbildungen.** Eine Zeichnung IST geschützter Ausdruck. Deshalb gilt die
   Regel von heute unverändert: unsignierte Figur aus freier Quelle
   übernehmen, signierte nachzeichnen, moderne Abbildung gar nicht.
2. **Das Phrasenbuch des Britischen Roten Kreuzes** im Übersetzer. Dort
   werden Formulierungen als solche übernommen — das ist der Zweck eines
   Phrasenbuchs und damit genau der Fall, in dem es auf die Lizenz ankommt.
   **Das bleibt der Blockierer, und er ist beim Rechteinhaber zu klären.**
3. **Ganze Tabellen und vollständige Zahlenreihen** aus einer einzelnen
   Quelle. Eine Sammlung kann als solche geschützt sein, auch wenn die
   einzelnen Zahlen es nicht sind. Wo ein Eintrag praktisch die komplette
   Tabelle einer Quelle nachbaut, wird sie gekürzt oder aus zwei Quellen
   zusammengeführt.

### Was von der strengen Regel bleibt

Die Schutzfrist-Prüfung wird **nicht** abgeschafft — sie bleibt für
Abbildungen zwingend, und sie bleibt als Arbeitsweise sinnvoll: Wer nur mit
alten, freien Quellen arbeitet, kann im Zweifel auch den Volltext beilegen
und alles nachprüfbar machen. Die Belegmappen des Zulieferbetriebs arbeiten
weiter so.

Was sich ändert: Eine moderne Quelle wird nicht mehr verworfen, nur weil ihr
Verfasser lebt. Sie wird gelesen, ihre Tatsachen werden verwendet, und sie
wird genannt.

## 11.08.2026 — Gefäße aus Ton (Bauanleitung, neue Gruppe „Werkstoffe und Gefäße")

Fünfundzwanzigste Bauanleitung und neunte Untergruppe. Ohne Topf kein Kochen,
ohne Krug kein Wasserholen, ohne Vorratsgefäß kein Vorrat.

### Quelle

- **Charles F. Binns, „The Potter's Craft"**, zweite Auflage 1922 (erste
  1910). Verfasser 1857 bis 1934, Schutzfrist 2004 abgelaufen. Das Todesjahr ist
  doppelt belegt.

**Aus zwei Kapiteln wurde ausdrücklich NICHTS übernommen:** Binns schreibt im
Vorwort, dass „Clay-Working for Children" von Elsie Binns und der Abschnitt
über alkalische Glasuren von Maude Robinson stammt. Für diese beiden ist kein
Todesjahr geprüft — also kein Zitat daraus.

### Zwei weitere Quellen wurden geprüft und verworfen

- Das NPS-Unterrichtsblatt lag schon im Projekt: inhaltlich zu dünn (ein
  30-Minuten-Plan für Kleinkinder, die Gefäße werden dort nicht einmal
  gebrannt).
- Der EXARC-Aufsatz zum Grubenbrand wäre fachlich die beste Quelle für genau
  die Lücke gewesen, die dieses Kapitel offen lässt. Die Fußzeile der Website
  nennt CC BY 4.0, aber ob das rückwirkend für die alten Jahrbücher gilt,
  ließ sich nicht klären. Nicht verwendet.

### Die größte Lücke steht als eigener Schritt im Text

**Der Feldbrand fehlt.** Binns beschreibt ausschließlich einen gekauften
Studio-Ofen mit Kamin und käuflichen Messkegeln. Wie man ohne Ofen brennt —
in einer Grube oder im Holzhaufen —, steht dort nicht. Die einzige Erwähnung
ist historisch: gebrannt wurde im offenen Feuer ohne Schutz, und genau das
nennt die Quelle als Grund für die große Unregelmäßigkeit in Güte und Farbe.

Damit ist die Anleitung ehrlich halb: Sie bringt jemanden bis zum fertig
getrockneten Gefäß und sagt dann, dass der letzte Schritt fehlt. **Das ist
die wichtigste offene Aufgabe für dieses Thema.**

### Eine Warnung, die in der Quelle fehlt

Binns nennt Bleiweiß als gewöhnlichen Glasurstoff, ohne ein Wort zur Gefahr —
1910 war das üblich. Die Anleitung hat deshalb eine eigene Warnung: kein Blei
in Gefäßen, aus denen gegessen oder getrunken wird, und erst recht nicht in
Kochgeschirr. Ausdrücklich als Einordnung dieses Handbuchs gekennzeichnet.

### Suchprobe

„töpfern" führte auf NICHTS — das Wort kam im ganzen Eintrag nicht vor.
Zusammen mit „Schüssel", „Becher" und „Teller" steht es jetzt im ersten Satz.
„topf" führt weiterhin zuerst auf „Kochen ohne Topf: mit heißen Steinen", und
das ist richtig so.

## 12.08.2026 — Räucherkammer bauen und Fisch räuchern (Bauanleitung)

Sechsundzwanzigste Bauanleitung, in der vorhandenen Gruppe „Nahrung
beschaffen und haltbar machen". Aus der Belegmappe
`work/mappen/raeucherkammer/belege.md` des Zulieferbetriebs.

### Quelle

- **„A Practical Small Smokehouse for Fish: How to Construct and Operate
  It"**, U.S. Department of Commerce, Bureau of Fisheries, Circular 27,
  zweite Fassung, ausgegeben am 25.10.1917.
- Einordnung: **US-Bundeswerk**, also Schutzfristenvergleich (§ 121 Abs. 4
  UrhG mit Art. 7 Abs. 8 RBÜ). Zusätzlich trägt die Titelseite keinen
  persönlichen Verfasser; die Fußnote „based on experiments by J. B.
  Southall" nennt die Grundlage der Versuche, nicht den Verfasser der
  Schrift. Auch nach Erscheinen + 70 wäre die Frist damit längst abgelaufen.
- Volltext und Seitenbilder gelesen am 12.08.2026.

### Warum die Anleitung überhaupt geschrieben wurde

Die Vorgabe war streng: Ohne echten Mehrwert gegenüber „Vorrat für Monate:
pökeln, räuchern, dörren" wird KEINE geschrieben. Der Mehrwert ist da und
liegt nicht beim Verfahren, sondern beim Bauwerk:

- Der vorhandene Eintrag beschreibt eine Behelfslösung — „ein möglichst
  dichtes Gebäude mit Abzug im Dach" und eine Feuergrube. **Kein einziges
  Maß.** Hier stehen alle: 107 mal 107 Zentimeter im Grundriss, 213
  Zentimeter hoch, Tür 61 mal 152, Zugloch 15 Zentimeter, sechs Luftlöcher
  von 2,5 Zentimetern je Seite, Auflager 18 und 51 Zentimeter unter der Decke.
- **Das ausgelagerte Feuer fehlte ganz.** Bei warmem Wetter geht es nicht
  anders: Feuerkiste abseits, Rohr unter der Erde, Ofenrohrklappe mit
  verlängertem Griff als Hauptsteuerung, Rauchverteiler über der Mündung,
  Grenze 43 Grad.
- **Fisch fehlte ganz.** Der vorhandene Eintrag ist reines Fleisch (die
  Quelle dort ist ein SOCOM-Handbuch). Lake, Wässern, Vortrocknen und
  Räucherzeiten nach Fischart standen nirgends im Paket.

### Eine Zahl, die nur am Seitenbild lesbar war

Die Belegmappe musste die Bolzengröße offenlassen: Die Texterkennung hatte
aus der Stelle „four J-inch by 4-inch bolts -at 3'aeTi'" gemacht. Am
Seitenbild (Seite 2 des Hefts) steht klar **„four ¼-inch by 4-inch bolts at
each vertical edge"** — vier Bolzen von einem Viertelzoll Dicke und vier Zoll
Länge je senkrechter Kante. Genau dafür gibt es die Regel, Zahlen am Bild zu
prüfen und nicht am erkannten Text.

Ebenfalls erst am Seitenbild gefunden und übernommen: Der Rand des Zuglochs
ist umgebördelt, „to prevent cutting and scratching the hands and arms" — das
fehlte in der Belegmappe und ist die einzige Verletzungsgefahr, die die
Quelle selbst für den Bedienenden nennt.

### Die Zeichnung

Seite 9 des Hefts trägt einen technischen Schnitt der Kammer mit
ausgelagerter Feuerkiste, Rohr, Rauchverteiler und Dachlüfter. **Unsigniert**
— nach der Projektregel damit übernehmbar. Papierton und Vignettierung des
Scans herausgerechnet, deutsch beschriftet, als
`assets/raeucherkammer-ausgelagert.png` ins Paket. Die Rezeptur steht als
Funktion `raeucherkammer` in `tools/skizzen/stich_beschriften.py`.

Zwei Fehler des Zeichenwerkzeugs sind dabei aufgefallen und behoben: Der
Stich stand als grauer Kasten auf dem Blatt (der Scan ist zu den Rändern hin
dunkler — jetzt wird der Hintergrund geschätzt und herausgerechnet), und die
Führungslinien liefen bei zweizeiligen Marken mitten durch die Schrift, was
wie ein Durchstreichen aussah. Sie beginnen jetzt neben der Beschriftung.

### Was die Quelle NICHT sagt — als eigener Schritt gekennzeichnet

Das Heft ist von 1917 und schweigt zu Botulismus, zu mikrobiologischem
Verderb und dazu, dass kaltgeräucherter Fisch **nicht gegart** ist. Es nennt
auch kein Wort zu Kohlenmonoxid, obwohl in der Kammer planmäßig ein Feuer bei
gedrosselter Luft glimmt. Der letzte Schritt der Anleitung trägt das nach,
ausdrücklich als Einordnung dieses Handbuchs und nicht als Aussage der
Quelle, und verweist auf „Fisch durchgaren", „Fisch nicht roh essen",
„Einkochen: zweimal erhitzen gegen Botulismus" und „Kohlenmonoxid:
unsichtbar und geruchlos".

Ebenfalls im Text aufgelöst, weil es sonst wie ein Widerspruch wirkt: Beim
Fleisch verlangt das Paket GRÜNES Laubholz, hier TROCKENES Holz. Der Grund
ist der Zweck — kühler Rauch über Tage gegen Rauch über Stunden, bei dem der
Fisch zugleich gar wird.

### Verworfen

„The Curing of Meat and Meat Products on the Farm" (Cornell, Lesson 119,
1916) nennt auf der Titelseite den Verfasser K. J. Seulke; sein Todesjahr war
nicht zu ermitteln, also keine belegbare Schutzfrist. Der Text enthält
ohnehin keinen Abschnitt zum Räuchern.

### Suchprobe (Tiefe 8)

45 Wörter geprüft. **Kein Notfall-Tipp hat Platz eins verloren:** „rauch"
führt weiter zuerst auf „Brandrauch: nicht hineingehen", „feuer" auf
„Brandrauch: sofort raus", „brand", „flamme", „vergiftung", „gefahr",
„hitze", „salz", „roh", „durchgaren", „botulismus", „haken" und
„kohlenmonoxid" ebenso unverändert. Die neue Anleitung steht bei „fisch"
erst auf Platz 5 — hinter „Fisch durchgaren" und „Fisch nicht roh essen",
und das ist die richtige Reihenfolge.

Bei „räuchern" steht sie auf Platz 1, vor „Vorrat für Monate". Das ist
vertretbar: Ihr erster Schritt verweist im ersten Absatz auf den anderen
Eintrag, und der Titel sagt, dass es hier um den Bau geht.

Zwei Ausdrücke des Bauwerks führen nur auf sie und sonst nichts:
„Zugklappe" und „Stichflamme".

### Ein Werkzeugfehler nebenbei

`tools/inhalt/einbauen.py` beanstandete die Kennung `nahrung-raeucherkammer-
bauen` und den Bilddateinamen als „in Umschrift geschrieben". Beide sind mit
Absicht in ASCII, wie jede Kennung im Paket. Die Umschrift-Prüfung übergeht
jetzt die Felder `id`, `image`, `category`, `group` und `situations`; die
Prüfung auf Steuerzeichen und fremde Diakritika gilt dort weiterhin.

## 12.08.2026 — Stampflehmwand (Bauanleitung) und eine Berichtigung an den Belegmappen

Siebenundzwanzigste Bauanleitung, Gruppe „Unterkunft und Schlafen". Bis hierher
kannte das Paket nur Behelfsunterkünfte für Tage bis Wochen; das ist die erste
Anleitung für ein Bauwerk, das Jahrzehnte stehen soll.

### Die Berichtigung zuerst, weil sie mehr betrifft als diesen Eintrag

Die Belegmappe `work/mappen/lehmbau/` hat die beiden ergiebigsten Quellen
VERWORFEN — USDA Farmers' Bulletin 1500 („Rammed Earth Walls for Buildings")
und 1720 („Adobe or Sun-Dried Brick") — mit der Begründung, die Titelseiten
nennten persönliche Verfasser, deren Todesjahr nicht zu belegen sei.

**Das ist der falsche Prüfmaßstab für ein Bundeswerk.** Nach 17 U.S.C. § 105
haben Werke der US-Bundesregierung im Ursprungsland von vornherein kein
Urheberrecht — das hängt daran, dass Bundesbedienstete sie im Amt geschrieben
haben, NICHT daran, ob sie namentlich genannt sind. Beide Bulletins nennen ihre
Verfasser ausdrücklich mit Dienststellung („Bureau of Agricultural
Engineering"). Über den Schutzfristenvergleich (§ 121 Abs. 4 UrhG mit Art. 7
Abs. 8 RBÜ) sind sie damit auch hier frei — Todesjahre spielen keine Rolle.

Dieselbe Verwechslung steht in der Belegmappe `work/mappen/quellfassung/`, die
deshalb Warren 1933, Garver 1948 und Hamilton 1940 verworfen hat. Auch das
sind USDA-Bundeswerke. **Die dort gezogene Folgerung, das Kapitel „Einen
Brunnen anlegen und schützen" müsse seine Quellen noch einmal prüfen, ist
gegenstandslos.**

Und selbst wo die Schutzfrist wirklich nicht zu belegen wäre, bliebe die
Entscheidung vom 11.08.2026 (weiter oben in dieser Datei): Tatsachen, Zahlen
und Verfahren sind nicht geschützt. Hart bleibt die Frage nur bei Abbildungen,
beim Phrasenbuch und bei vollständig nachgebauten Tabellen.

**Für den Zulieferbetrieb heißt das:** Vor der Prüfung „Todesjahr + 70" kommt
die Frage, OB es ein US-Bundeswerk ist. Erst wenn nein, wird nach Verfassern
und Todesjahren gesucht. Die Mappen zu Lehmbau und Quellfassung haben diese
Reihenfolge umgedreht und dadurch brauchbare Quellen weggeworfen.

### Quellen dieses Eintrags

- **USDA Farmers' Bulletin 1500, „Rammed Earth Walls for Buildings"**,
  M. C. Betts und T. A. H. Miller, Bureau of Agricultural Engineering; 1926,
  überarbeitet Mai 1937. Volltext selbst geladen und ganz gelesen am
  12.08.2026 (`work/quellen/lehmbau/farmbul1500rev1937.txt`). Einordnung:
  US-Bundeswerk, Schutzfristenvergleich.
- **US Resettlement Administration, „Rammed Earth Construction"** (Merkblatt,
  ohne Jahr; die Behörde bestand nur 1935 bis 1937). Kein persönlicher
  Verfasser. Liefert die Mischung mit Prozentgrenzen und den Wasseranteil.

Die beiden widersprechen sich bei der Mischung nicht, sie sagen Verschiedenes:
Das Merkblatt gibt 3 Sand : 2 Lehm : 1 grober Zuschlag mit den Grenzen
höchstens 20 Prozent Lehm und mindestens 16 Prozent grober Zuschlag; das
Bulletin führt einen deutschen Verfasser mit 1 Ton : 1 scharfer Sand : 2 Stein
in Walnussgröße an. Beide stehen im Eintrag, als das was sie sind.

### Am Seitenbild geprüft

Wandstärken (46/36/30 Zentimeter), Feuchtebereich 7 bis 14 Prozent, die
Tagesleistung (drei Leute, rund 1,5 Kubikmeter Wand, dafür 3 Kubikmeter lose
Erde vorbereiten), der Zinkenabstand der Harke und das Verhältnis „eine
Schaufel gute Erde auf fünf bis sechs schlechte" — alles auf den Seiten 5 und 7
des Hefts nachgesehen, nicht im erkannten Text.

### Die Zeichnung

Figur 7 des Bulletins zeigt Eck- und Wandschalung. Unsigniert, aus einem freien
Bundeswerk, also übernehmbar. Die englische Beschriftung im Stich wurde
herausgenommen und durch deutsche ersetzt; Papierton und Vignettierung
herausgerechnet. Liegt als `assets/stampflehm-schalung.png` im Paket, Rezeptur
als Funktion `stampflehm_schalung` in `tools/skizzen/stich_beschriften.py`.

### Eine Warnung, die in der Quelle fehlt

Das Bulletin empfiehlt Kreosot für eingelegtes Holz und einen Anstrich aus
Steinkohlenteer, ohne ein Wort zur Gefahr — 1937 war das üblich. Beide gelten
heute als krebserzeugend. Der Eintrag sagt das ausdrücklich als Einordnung
dieses Handbuchs und nennt den harmlosen Ersatz. Dazu kommt ein eigener
Schlussschritt zu dem, was die Hefte gar nicht behandeln: Erdbeben, die fehlende
statische Grundlage der Wandstärken, und der eine Satz, der alles zusammenhält —
Stampflehm ist ein guter Baustoff, solange er trocken bleibt.

### Suchprobe (Tiefe 8)

Erster Durchgang: **„Lehmwand" führte auf NICHTS, und „Lehm" fand den Eintrag
nicht einmal unter den ersten acht** — auf Platz eins stand die Gallenkolik. Der
Titel hieß da noch „Stampflehmwand bauen: ein Haus aus der Erde, auf der es
steht"; die Suche zerlegt „Stampflehmwand" offenbar nicht in „Lehm" und „Wand".
Titel geändert in „Stampflehm: eine Lehmwand bauen, die Jahrzehnte steht", die
Alltagswörter Lehmwand, Lehmhaus und Erdhaus stehen jetzt im ersten Satz.
Danach: „lehm" und „lehmwand" führen zuerst auf den neuen Eintrag.

Kein Notfall-Tipp hat Platz eins verloren. Geprüft unter anderem: „wand" führt
weiter zuerst auf „Wanderröte", „teer" auf „Magengeschwür" (Teerstuhl!),
„ratten" auf „Vorräte vor Nagern schützen", „frost" auf „Grabenfuß", „glas" auf
„Zwei Wände", „erdbeben" auf den Erdbeben-Tipp.

### Ein zweiter Werkzeugfehler

Der Einbau lief durch, und danach war die ganze Testsuite rot: Die
Mengenangabe eines Materials war 45 Zeichen lang, erlaubt sind 40 (sie hängt an
`MAX_CATEGORY_LENGTH`, nicht an der Notizlänge). `tools/inhalt/einbauen.py`
prüfte bisher nur die Notiz. Es prüft jetzt auch `item`, `amount` und die
Werkzeugliste — damit dieser Fehler wieder vor dem Gradle-Lauf auffällt und
nicht danach.

## 12.08.2026 — Dach decken: Stroh, Reet oder Holzschindeln (Bauanleitung)

Achtundzwanzigste Bauanleitung, Gruppe „Unterkunft und Schlafen". Der Nachbar
zur Stampflehmwand: Wand ohne Dach ist kein Haus. Drei Deckungen in einem
Eintrag, weil der Leser zuerst wählen muss, womit er deckt.

### Quellen

- **J. C. Loudon, „An Encyclopaedia of Agriculture"**, zweite Auflage 1831,
  Abschnitt „Thatching" (§§ 3183–3197) und § 2487 zum Strohdeckmesser.
  Verfasser 1783–1843, Schutzfrist 1913 abgelaufen. Trägt alles zu Stroh und
  Reet.
- **USDA Farmers' Bulletin 1751, „Roof Coverings for Farm Buildings and Their
  Repair"**, Fassung 1948, A. D. Edgar und T. A. H. Miller, Bureau of
  Agricultural Engineering. Werk der US-Bundesregierung; Einordnung wie beim
  Stampflehm-Eintrag. Trägt die Schindelmaße, die Sichtlängen, das Nagelbild
  und die Neigungsgrenze.
- **ICS, „Building Trades Pocketbook"**, Scranton 1905, Verfasser ist die
  Organisation selbst. Trägt gespalten gegen gesägt und die Tagesleistung.
- **Encyclopaedia Britannica, 11. Auflage, Artikel „Thatch"** (unsigniert).
  Trägt Gesamtstärke, Haltbarkeit und den Hinweis aufs Stadtverbot.

### Dieselbe Verwechslung wie beim Lehmbau — und was sie hier gekostet hätte

Die Belegmappe hatte Farmers' Bulletin 1751 verworfen: „zwei benannte
natürliche Personen … ohne belegtes Todesjahr". Auch das ist ein
US-Bundeswerk, und die Mappe nennt selbst, was dadurch verlorenging — der
Nagelabstand. Es ist die inhaltlich beste Schindelquelle der ganzen Mappe.
Damit ist die Berichtigung von heute an zwei unabhängigen Mappen belegt und
kein Einzelfall.

### Eine Zahl, die der Texterkennung zum Opfer gefallen wäre

Für Dächer flacher als ein Viertel Neigung nennt das Bulletin die verringerten
Sichtlängen. Im erkannten Text steht „3%, 41^, and 5%". Am Seitenbild (Seite 2
des Hefts) steht klar **3¾, 4¼ und 5¾ Zoll**. Wer den mittleren Wert aus dem
Fließtext übernimmt, liest leicht 4½ — das wären 6 Millimeter zu viel
Sichtlänge auf einem ohnehin zu flachen Dach. Ebenfalls am Bild geprüft: die
Standard-Sichtlängen 5 / 5½ / 7½ Zoll, der Nagelabstand (zwei Nägel, höchstens
drei Viertel Zoll vom Rand, 1 bis 2 Zoll über der Unterkante der nächsten
Lage) und die Fugenversetzung von 1½ Zoll.

### Keine Zeichnung

Die Abbildungen des Bulletins sind Fotografien, keine Strichzeichnungen — als
Rasterbild im Handformat unbrauchbar. Loudons Abschnitt zum Strohdeckmesser
trägt keine Figurennummer. Dieser Eintrag bleibt deshalb ohne Bild; er ist der
erste seit dem Umstieg auf Quellzeichnungen, für den sich keine passende Figur
finden ließ.

### Ein Widerspruch, der keiner ist

Die ältere Quelle verlangt für Schindeldächer ausdrücklich KEINE dichte
Schalung und KEIN Papier darunter (Luft soll zirkulieren), die jüngere
empfiehlt in kaltem Klima genau das. Der Eintrag löst das auf, statt eine
Seite wegzulassen: Luft von unten hält die Schindel länger heil, ein dichtes
Dach hält die Wärme im Haus — wer heizen muss, nimmt das dichte Dach und weiß,
dass die Schindeln schneller altern.

### Zwei Warnungen, die in den Quellen fehlen

Kreosot (die ältere Quelle empfiehlt Tränken darin) gilt heute als
krebserzeugend — und die jüngere Quelle sagt ohnehin, dass diese Beizen kaum
schützen und im Wesentlichen Farbe sind. Und: Keine der Quellen erwähnt, dass
das Arbeiten auf dem Dach selbst gefährlich ist. Der Schlussschritt sagt es
ausdrücklich, weil ein Sturz aus Dachhöhe in einer Lage ohne Krankenhaus zu den
wahrscheinlichsten schweren Verletzungen dieses ganzen Handbuchs gehört.

Ebenfalls ausdrücklich als Lücke benannt: **Für Stroh- und Reetdächer nennt
keine der Quellen eine Mindestneigung.** Die bekannte Faustregel (steil, oft
45 Grad und mehr) steht nirgends in diesen Heften und wird deshalb NICHT als
Zahl übernommen — der Eintrag verweist stattdessen auf die alten Dächer der
eigenen Gegend.

### Suchprobe (Tiefe 8)

Kein Notfall-Tipp hat Platz eins verloren: „brand" führt weiter auf
„Brandrauch: sofort raus", „nagel" auf „Starrkrampf", „sturz" auf
„Bauchverletzung", „leiter" auf „Eiseinbruch: retten", „wind", „sturm",
„feuer" und „gefahr" unverändert.

Gefunden und behoben: **„dachdecken" in einem Wort führte auf NICHTS** — der
Titel schreibt „Dach decken" getrennt. Das Wort steht jetzt im ersten Satz.
Danach führt es auf den Eintrag. „strohdach", „reetdach", „traufe", „sparren"
und „schindel" führen jeweils zuerst dorthin.

## 12.08.2026 — Mit Feldstein mauern (Bauanleitung)

Neunundzwanzigste Bauanleitung, Gruppe „Unterkunft und Schlafen". Der dritte
Teil des Bauwerks: Die Lehmwand verlangt ein Fundament aus Mauerwerk, und wie
Mauerwerk geht, stand nirgends im Paket.

### Quellen

- **International Correspondence Schools, „Elements of Stone Masonry"**, aus
  „Elements of Stone and Brick Masonry", International Textbook Company,
  Scranton; eigene Urheberjahre 1907 und 1909, Sammelausgabe 1930. Als
  Verfasser ist die Organisation genannt, keine natürliche Person → Erscheinen
  + 70, längst abgelaufen.
- **D. H. Jacques, „The House"** (1866), Anhang „How to Build with Rough
  Stone". Verfasser 1825 bis 1877, Schutzfrist 1947 abgelaufen. Nur ein kurzer
  Abschnitt ist einschlägig.

### Der Eintrag sagt selbst, was er NICHT ist

Die Belegmappe heißt „Trockenmauer", aber **beide Quellen beschreiben
durchgehend VERMÖRTELTES Mauerwerk.** Der Mappe fehlen deshalb zwei
Kernstücke des echten Trockenmauerbaus, und sie sagt das auch: die
Anlaufschräge (dass die Mauer nach oben schmaler wird) und die Füllung
zwischen zwei Sichtseiten. Beides kommt in keiner der Quellen vor.

Ich habe den Eintrag deshalb NICHT „Trockenmauer" genannt, sondern „Mit
Feldstein mauern", und die Einschränkung steht im ERSTEN Schritt statt
irgendwo hinten: Verband, Läufer und Binder, große Steine unten und an den
Ecken und die Lagerfläche gelten auch ohne Mörtel — die Geometrie einer echten
Trockenmauer steht hier aber nicht. Wer nach „trockenmauer" sucht, findet den
Eintrag und liest im ersten Absatz, was ihm fehlt.

### Am Seitenbild geprüft

Auf Seite 55 des Hefts: „never less than 18 inches thick", „brought to a level
every 3 feet in height", „bond stone or headers … at intervals not exceeding
3 feet", „cement mortar composed of 2 parts of sand and 1 part of cement".
Auf Seite 18: „bonded about every 4 or 5 feet". Alle Zahlen des Eintrags
stammen von dort.

**Wichtig zur Einordnung dieser Zahlen, und das steht auch im Eintrag:** Sie
kommen aus einer städtischen Bauvorschrift für vermörtelte Gebäudemauern, nicht
aus einer Anleitung für freistehende Feldsteinmauern. Sie werden als das
ausgewiesen, was sie sind — das einzige Maß, das die Quellen hergeben.

### Die Zeichnung

Figur 21 des Hefts, eine Mauer aus rundem Feldstein mit Ecke. Ein Holzstich
ohne jede Beschriftung und ohne Signatur — deshalb war nichts zu entfernen,
nur deutsch zu beschriften. Liegt als `assets/feldsteinmauer.png` im Paket,
Rezeptur als Funktion `feldsteinmauer` in `tools/skizzen/stich_beschriften.py`.

### Zwei Warnungen, die in den Quellen fehlen

Keine der Quellen sagt ein Wort über das GEWICHT dieser Steine. Ein Eckstein
in der geforderten Größe wiegt schnell mehr, als ein Mensch heben sollte; der
Schlussschritt verweist auf „Lasten heben: Dreibock, Bockmast und
Ankerpunkte" und sagt ausdrücklich, dass man nicht mit den Fingern unter einen
Stein greift, den man absetzt. Ebenso fehlt die Frosttiefe für eine
freistehende Mauer — der Eintrag verweist auf die Lehmwand, wo sie steht.

### Suchprobe (Tiefe 8)

Kein Notfall-Tipp hat Platz eins verloren: „kalk" führt weiter auf „Kalk
löschen und verwenden" (und die Verätzung auf Platz zwei), „heben" auf
„Lasten heben", „quetschung" auf das Kompartmentsyndrom, „frost" auf den
Grabenfuß, „gefahr" unverändert. Neu und eindeutig: „feldstein",
„bruchstein", „binder", „steinmauer" und „trockenmauer" führen jeweils nur
oder zuerst auf den neuen Eintrag.

## 12.08.2026 — Ziegel selbst machen (Bauanleitung)

Dreißigste Bauanleitung, Gruppe „Werkstoffe und Gefäße". Schließt die
Baureihe dieser Nacht: Lehmwand, Dach, Feldsteinmauer — und jetzt der Ziegel,
den die Feldsteinmauer für ihre Ecken braucht und den die Lehmwand für
Schornstein und Feuerstelle verlangt.

### Quellen

- **Heinrich Ries und Henry B. Kümmel, „The Clays and Clay Industry of New
  Jersey"** (1904), Band VI des Abschlussberichts des State Geologist.
  Verfasser 1871–1951 und 1867–1945; maßgeblich das spätere Todesjahr,
  Schutzfrist Ende 2021 abgelaufen.
- **Heinrich Ries, „Clays: Their Occurrence, Properties, and Uses"**, zweite
  Auflage 1908. Liefert den Dachziegel-Abschnitt.

**Ausdrücklich zur Einordnung:** Das erste Werk ist KEIN Bundeswerk. Der State
Geologist von New Jersey ist ein Bundesstaat, und 17 U.S.C. § 105 gilt für ihn
NICHT — genau die Ausnahme, die in der Entscheidung vom 11.08. steht. Hier
trägt allein die abgelaufene Frist über die benannten Verfasser. Die
Belegmappe hat das richtig geprüft.

### Am Seitenbild geprüft — und der Ertrag war ungewöhnlich groß

Diese Quelle hat keine Textebene im PDF, und der Band hat 704 Seiten mit
eingehefteten Tafeln, sodass sich die Seitennummern verschieben. Die Seiten
mussten über eine Montage der Kopfzeilen gesucht werden (Rezept, falls es
wieder vorkommt: Kopfstreifen von zwanzig Seiten untereinandersetzen und die
gedruckte Nummer ablesen; die Tafeln verschieben den Versatz um etwa eine
Seite je drei).

Bestätigt auf den gedruckten Seiten 225, 227, 235 und 240: Ringgrube 20–25 Fuß
Durchmesser, 3 Fuß tief, Rad 6 Fuß, 5–6 Stunden, 25 000–30 000 Ziegel je
Füllung; Handformen 2500 Ziegel in zehn Stunden; Kegel 05/03 und 7/8; und der
ganze Meileraufbau (38–54 Lagen, Bogengänge 2 Fuß, Wand aus zwei Lagen,
Lehmbewurf, Platting).

**Vier Dinge standen NICHT in der Belegmappe und kamen erst am Seitenbild dazu
— eines davon ändert die Empfehlung:**

1. **Der Meiler taugt NUR für gewöhnliche Bauziegel und lässt sich NICHT auf
   hohe Temperatur bringen.** Wo Tone verwendet werden, die hohe Temperatur
   brauchen, WIRD DIE WARE NICHT HART. Ohne diesen Satz hätte der Eintrag
   einen Ofen empfohlen, der bei manchem Ton weiche Ziegel liefert.
2. Das Kohlige im Ton brennt bei schwacher Rotglut ab — **aber nur, wenn genug
   Luft da ist.** Das ist die zweite Hälfte der Regel gegen den schwarzen Kern.
3. **Die Einweichgrube** als einfachster Weg der Aufbereitung: über Nacht
   einweichen. Sie weicht auf, MISCHT aber nicht — in der Fabrik mischt danach
   die Maschine, von Hand muss man treten.
4. Handgeformte Ziegel sind **poröser** als maschinengeformte.

Dazu die Begründung, warum der Fallzugofen sich durchsetzte: nicht nur bessere
Regelung, sondern auch, dass die unteren Lagen weniger leicht aus der Form
gedrückt werden.

### Keine Zeichnung

Die Tafeln dieses Bandes sind Fotografien (Tafel XXVIII zeigt den Meiler von
außen und halbfertig). Als Rasterbild im Handformat unbrauchbar.

### Suchprobe (Tiefe 8)

Kein Notfall-Tipp hat Platz eins verloren. Bemerkenswert, weil es leicht hätte
schiefgehen können: „brennen" führt weiter zuerst auf die Blasenentzündung
(Brennen beim Wasserlassen), „ofen" auf „Kohlenmonoxid: unsichtbar und
geruchlos", „einsturz" auf „Mehrere Verletzte", „verschüttet" auf die Lawine,
„dachziegel" auf „Sturm: wie man sich verhält" — fliegende Dachziegel im Sturm
gehören dort auch hin. Neu und eindeutig: „backstein" und „mauerstein".

## 12.08.2026 — Gerben ohne Rinde: Hirngerbung und Alaungerbung (Agrikultur)

Zweiundvierzigstes Agrikultur-Kapitel, Gruppe „Schlachten und Verwerten".
Es setzt genau da an, wo das vorhandene Kapitel „Gerben mit Eichenrinde"
ausdrücklich aufhört: **Fell mit Haaren, ohne Handel.** Die Hirngerbung
braucht nichts, was nicht am Tier selbst ist; die Alaungerbung braucht Alaun
und Salz, dafür ist sie schnell und schont das Haar.

### Quellen

- **Clark Wissler, „Material Culture of the Blackfoot Indians"** (1910).
  Verfasser 1870–1947, Schutzfrist 2017 abgelaufen. **Aus dem
  Mokassin-Kapitel wurde nichts übernommen, was ausdrücklich als Beitrag von
  William C. Orchard gekennzeichnet ist** — für ihn ist kein Todesjahr belegt.
- **George Bird Grinnell, „The Cheyenne Indians", Band 1.**
- **Campbell Morfit, „The Arts of Tanning, Currying, and Leather Dressing"**
  (1852) für die Alaungerbung.

### Der wichtigste Satz steht in der Quelle beiläufig da

„Such skins were smoked before being made into clothing, so that the garment
might be easily softened after being wet." — **Geräuchert wird, damit sich das
Stück nach dem Nasswerden wieder weich reiben lässt.** Eine hirngegerbte, aber
ungeräucherte Haut wird nach dem ersten Regen wieder hart wie Pappe, und die
ganze Arbeit ist noch einmal zu machen. Das Kapitel macht daraus einen eigenen
Abschnitt mit Überschrift, weil es sonst untergeht.

### Wie die Suche wirklich arbeitet — endlich festgenagelt

Bei diesem Eintrag ließ sich klären, was am 12.08. schon beim Stampflehm
aufgefallen war: **Die Suche trifft über den WORTANFANG, nicht über
Teilzeichenketten.**

- „lehm" findet „Stampflehmwand" NICHT (steht in der Mitte).
- „hirn" findet „Hirngerbung" SEHR WOHL (steht am Anfang).

Das erklärt beide Beobachtungen des Tages mit einer Regel. **Für künftige
Einträge heißt das:** Ein Alltagswort muss am WORTANFANG vorkommen, sonst ist
es unauffindbar. Zusammensetzungen wie „Stampflehmwand" tragen nur ihr erstes
Glied in die Suche.

### Ein Zusammenstoß, der sich nicht auflösen ließ — und warum das vertretbar ist

Weil die Suche am Wortanfang trifft, holt **„hirn" jetzt dieses Kapitel auf
Platz eins, vor „Zeckenstich: FSME"**. Ich habe drei Dinge versucht: das Wort
aus dem Titel genommen, alle elf freistehenden „Hirn" im Text durch
„Hirnmasse" ersetzt und die Kennung von `agrikultur-gerben-hirn-alaun` auf
`agrikultur-gerben-ohne-rinde` geändert. Es bleibt bei Platz eins, denn der
Treffer kommt aus dem Fachwort **„Hirngerbung"** selbst — und das ist der
richtige deutsche Begriff und muss stehen bleiben.

**Warum es trotzdem vertretbar ist:** Jede Suche, die jemand im Ernstfall
wirklich eingibt, ist unverändert richtig. Nachgemessen:

| Suchwort | Platz eins |
|---|---|
| hirnhaut / hirnhautentzündung | Zeckenstich: FSME |
| gehirn | Gehirnerschütterung |
| gehirnerschütterung | Gehirnerschütterung |
| kopf | Blutung an Kopf und Rumpf |
| zecke | Zecke entfernen |
| schlaganfall | Schlaganfall |
| bewusstlos | Entscheidung nach der Atemkontrolle |

Betroffen ist allein der nackte Wortstamm „hirn". **Das gehört offen benannt,
und es darf anders entschieden werden** — dann bliebe nur, das Fachwort im
ganzen Kapitel zu vermeiden, und dafür ist der Preis höher als der Gewinn.

### Was das Kapitel bewusst NICHT behauptet

Keine der Quellen nennt eine MENGE Hirnmasse. Die verbreitete Faustregel,
jedes Tier habe genug davon für seine eigene Haut, steht in keiner von ihnen
und wird deshalb nicht als Zahl übernommen. Ebenso wenig stehen Zeiten für die
Hirngerbung dort — nur „über Nacht ziehen lassen".

Eigene Einordnung dazugekommen: die Hygiene (Hirnmasse, Leber und eine gärende
Kleiebrühe sind Nährböden; die Quellen von 1852 und 1910 sagen dazu nichts)
und das Kohlenmonoxid beim Räuchergestell über der Feuergrube.

## 12.08.2026 — Schornstein bauen und das Rohr richtig anschließen (Bauanleitung)

Einunddreißigste Bauanleitung, Gruppe „Feuer". Die Lehmwand verlangt
ausdrücklich, dass Schornstein und Feuerstelle NICHT aus Lehm gebaut werden;
die Feldsteinmauer und der Ziegel liefern das Material. Hier steht, wie es
zusammengefügt wird, ohne das Haus anzuzünden.

### Quelle

- **USDA Farmers' Bulletin 1649, „Construction of Chimneys and Fireplaces"**,
  ausgegeben November 1930, leicht überarbeitet April 1933. Als Verfasser
  nennt die Schrift eine Behörde („By the Bureau of Agricultural
  Engineering"), keine natürliche Person — also Erscheinen + 70, und zusätzlich
  US-Bundeswerk.

Die Belegmappe hat die Nachfolgeschrift (Bulletin 1889, 1941) verworfen, weil
sie zwei persönliche Verfasser nennt. **Dieselbe Verwechslung wie in drei
anderen Mappen** — auch 1889 ist ein Bundeswerk. Hier war es folgenlos: 1649
trägt den Eintrag vollständig, und 1889 wurde nicht gebraucht.

### Am Seitenbild geprüft — die Zahlen, an denen Häuser hängen

Auf den gedruckten Seiten 8, 9 und 10 nachgesehen, nicht im erkannten Text:
Ofenrohr mindestens 18 Zoll von Holz, 9 Zoll nur bei mindestens 1 Zoll
feuerfester Ummantelung, Blechhaube 2 Zoll über der oberen Rohrhälfte;
2 Zoll Luft zwischen Schornstein und jedem Holzbalken, gefüllt mit loser
Asche und ausdrücklich NICHT mit Mörtel oder Beton; Durchführung durch eine
Holzwand mit einer Blechhülse mindestens 12 Zoll größer als das Rohr oder
4 Zoll Mauerwerk; Fußleisten mit ⅛ Zoll feuerfester Zwischenlage; und die
Mörtelmischung (ein Sack Portlandzement von mindestens 94 Pfund, 9 Pfund
trockener Löschkalk, dreifaches Volumen Sand; ersatzweise ein halber Kubikfuß
Kalkbrei).

**Bewusst NICHT übernommen:** Die Zuggrößen-Tabelle des Hefts. Die
Belegmappe weist selbst darauf hin, dass die Texterkennung dort die
Bruchzahlen zerstört hat („914" statt „9¼", „814 by 8%"). Solche Zahlen ohne
Not zu übernehmen wäre genau der Fehler, gegen den die Regel gemacht ist.

### Die Zeichnung

Figur 8 zeigt links den falschen und rechts den richtigen Anschluss des
Rohres — ein Rohr, das in den Zug hineinragt, gegen eines, das bündig endet,
mit eingezeichneten Strömungslinien. Ein unsignierter Strich aus einem freien
Bundeswerk. Die Buchstaben A und B wurden entfernt und durch deutsche
Beschriftung ersetzt. Liegt als `assets/ofenrohr-anschluss.png` im Paket.

### Die Projekt-Tests haben einen Fehler gefunden, den meine eigene Probe erst danach gesehen hätte

Nach dem Einbau schlugen ZWEI vorhandene Tests fehl:
`NotfallSucheTest.dieCoMelderTippsVerdraengenKeinenNotfallTipp` und
`ZusammenspielTest.beiGasWoerternNenntDerErsteTrefferDieNummer`. Grund: Der
Titel hieß „Schornstein und **Ofenrohr** richtig bauen und anschließen", und
weil die Suche über den Wortanfang trifft, holte **„ofen" die Anleitung vor
den Tipp „Kohlenmonoxid: unsichtbar und geruchlos"** — also vor den Eintrag,
der die Notrufnummer trägt.

Der zweite Test sagt das sogar wörtlich: „Bei diesen Woertern muss an erster
Stelle der Notfall-Tipp mit der Nummer stehen. Titel des neuen Eintrags
aendern."

**Behoben:** Titel jetzt „Schornstein bauen und das Rohr richtig anschließen",
und in der Materialliste „Ofenrohr und Wanddurchführung" zu „Rohr und
Wanddurchführung". Danach beide Tests grün. Nachgemessen: „ofen" führt wieder
zuerst auf den Kohlenmonoxid-Tipp, „kamin" ebenfalls — und der neue Eintrag
bleibt über „schornstein", „ofenrohr" und „rauchabzug" auf Platz eins
auffindbar.

Das ist die Gegenprobe zu der Entscheidung beim Gerbe-Kapitel: Dort ließ sich
der Zusammenstoß nicht auflösen, hier schon. Wo es geht, wird geändert.

### Was die Quelle nicht sagt

Das Heft von 1933 redet fast nur vom BRAND. **Kohlenmonoxid kommt nicht vor** —
dabei ist ein undichter Zug genau der Weg, auf dem es in den Wohnraum kommt.
Der Schlussschritt trägt das nach und verweist auf die drei
Kohlenmonoxid-Tipps. Ebenso nachgetragen: dass die Querschnitte und Höhen für
ein Wohnhaus mit Heizkessel gedacht sind, während Abstände und Dichtheit für
jede Hütte gleich gelten.

## 12.08.2026 — Schuhe selbst machen (Bauanleitung)

Zweiunddreißigste Bauanleitung, Gruppe „Werkstoffe und Gefäße". Sie schließt
direkt an das Gerbe-Kapitel derselben Nacht an: Erst wird Leder gemacht, dann
etwas daraus. Und Schuhwerk ist das, woran Beweglichkeit hängt.

### Quellen

- **George Bird Grinnell, „The Cheyenne Indians", Band 1** (1923). Verfasser
  1849 bis 1938, Schutzfrist 2008 abgelaufen.
- **Clark Wissler, „Material Culture of the Blackfoot Indians"** (1910).
- **John Bedford Leno, „The Art of Boot and Shoemaking"** für die
  Lederbeurteilung und den Riemenschuh.

### Warum dieser Eintrag kein Schnittmuster hat — und das auch sagt

Wisslers Buch enthält bemaßte Mokassin-Schnittmuster (Fig. 78, 83–100). Sie
sind im Buch **ausdrücklich William C. Orchard zugeschrieben**, und für ihn
ist kein Todesjahr belegt. Also nicht verwendet — und weil ausgerechnet die
Schnittmuster darunterfallen, hat der Eintrag keines. Das steht so im letzten
Schritt, zusammen mit dem einzigen brauchbaren Ersatz: den ersten Schuh
bewusst als Probestück aus dem schlechtesten Leder machen.

### Das Wertvollste steht in einem Nebensatz

Die Nähfolge: **Die Sohle wird VON INNEN angenäht, der Mokassin ist danach auf
links, die Fersennaht bleibt offen, gewendet wird mit einem Stock** — und erst
dann wird die Ferse von innen genäht. Ohne diesen Absatz näht man die Naht
nach außen, und sie scheuert als Erstes auf.

Ebenso wertvoll und leicht zu übersehen: Die ÄLTERE Bauart (ein Stück mit
einem Lappen unter der ganzen Sohle, eine einzige Naht) galt bei denen, die
beide getragen haben, als **bequemer und viel haltbarer**. Der Grund ist im
Eintrag ergänzt, steht aber nicht in der Quelle: Bei ihr läuft keine Naht
unter der Fußsohle.

### Zwei Querverweise waren falsch zitiert — der Test hat sie gefangen

`QuerverweiseTest` meldete: „Blase am Fuß: geschlossen lassen, wenn möglich"
und „Grabenfuß: nass und kalt reicht aus" gibt es nicht. Richtig heißen sie
„… wenn es geht" und „Grabenfuß: nasse Füße zerstören ohne Frost". Beide
korrigiert. Das ist genau der Fall, für den dieser Test gebaut wurde — ein
Verweis ins Leere hätte niemand von Hand gefunden.

### Suchprobe (Tiefe 8)

Alle Notfall-Wörter halten Platz eins: „fuß" führt auf „Blase am Fuß", „füße"
und „stiefel" auf den Grabenfuß, „blase" auf die Blase, „naht" und „nähen" auf
das Wundennähen.

**Eine Verschiebung, die stehen bleibt und die bekannt sein soll:** Bei
„schuh" und „schuhe" steht jetzt die neue Anleitung vor „Blase am Fuß". Die
Projekt-Tests beanstanden das nicht — „schuh" gehört nicht zu den Wörtern, für
die dort ein Notfall-Tipp festgeschrieben ist, anders als beim Schornstein,
wo der Test sofort ansprang und der Titel geändert wurde. Und wer eine Blase
hat, sucht „blase", „fuß" oder „füße" — alle drei führen richtig. Ein Eintrag
über Schuhe, der bei „schuhe" nicht auffindbar ist, wäre der schlechtere
Tausch.

## 12.08.2026 — Mühlrad am Bach (Bauanleitung)

Dreiunddreißigste Bauanleitung, Gruppe „Werkzeug herstellen und instand
halten". Der erste Eintrag des Pakets über MASCHINENKRAFT: Bisher endete
alles bei Handarbeit („Korn zu Mehl: mahlen ohne Mühle").

### Quelle — und eine Besonderheit

- **„1928 Fitz Steel Overshoot Water Wheel Catalog"**, Fitz Water Wheel
  Company, Hanover, Pennsylvania. Kein persönlicher Verfasser; das
  Urheberrecht ist auf die Firma eingetragen. Erscheinen 1928 plus 70 —
  längst abgelaufen.

**Es ist ein VERKAUFSKATALOG**, und zwar für Stahlräder. Seine Aussagen über
hölzerne Räder sind eigennützig. Der Eintrag sagt das zweimal ausdrücklich:
einmal im ersten Schritt als Warnung an den Leser, einmal im Abschnitt über
Holz selbst. Die Messverfahren und Formeln sind davon unberührt — eine Formel
ist wahr oder falsch, unabhängig davon, wer sie druckt.

### Wieder eine Zahl am Seitenbild gerettet

Die Belegmappe gibt das Gewicht eines Kubikfußes Wasser mit **62½** Pfund an.
Auf Katalogseite 67 steht **62⅓**. Die Texterkennung hat den Bruch verändert.
Der Unterschied ist hier klein (62,5 gegen 62,33; der wirkliche Wert liegt bei
rund 62,4) — aber es ist derselbe Fehlertyp, der beim Dachdecken aus 4¼ ein
4½ gemacht hätte.

### Was der Eintrag daraus macht

Das Wertvollste ist nicht das Rad, sondern die **Messung**: ein Brett quer über
den Bach, eine waagerechte Kerbe, ein Pflock 1,20 Meter flussaufwärts auf
Höhe der Kerbenunterkante, und die Stauhöhe darüber ablesen. Damit weiß man
vor Wochen Arbeit, ob der Bach etwas hergibt.

Die Leistungsformel steht in beiden Einheiten: die der Quelle (Kubikfuß je
Minute × 62⅓ × Fallhöhe in Fuß ÷ 33 000) und als meine ausdrücklich
gekennzeichnete Umrechnung die Faustformel **Kilowatt ≈ 10 × Kubikmeter je
Sekunde × Meter Fallhöhe**, mit einem durchgerechneten Beispiel.

**Bewusst NICHT übernommen:** die Wehr-Tabelle des Katalogs, die von der
Stauhöhe zur Wassermenge führt. Eine vollständig nachgebaute Zahlentabelle aus
einer einzelnen Quelle ist etwas anderes als eine Tatsache — so steht es in
der Entscheidung vom 11.08. Der Eintrag sagt stattdessen, welche zwei Zahlen
man vor Ort messen und aufschreiben muss.

### Der Titelwächter hat zugeschlagen

Der Titel hieß zuerst „**Wasserrad**: was ein Bach hergibt …". `TitelwaechterTest`
meldete: „traegt damit das reservierte Wort ‚wasser' (als ‚wasserrad') im
Titel. Ein Titelwort wiegt 5 Punkte und verdraengt damit den Eintrag, dem das
Wort gehoert." Der Test bot beides an — Titel ändern oder den Eintrag in die
Liste aufnehmen und nachmessen.

**Titel geändert**, auf „Mühlrad am Bach: Leistung messen und Rad auslegen".
„wasser" gehört zu den wichtigsten Wörtern dieses Handbuchs; ein Mühlrad hat
dort nichts verloren. Nachgemessen: „wasser" führt unverändert auf „Trübes
Wasser vorbehandeln", und der neue Eintrag ist über „wasserrad", „mühlrad",
„gefälle" und „wehr" jeweils auf Platz eins auffindbar — das Wort „Wasserrad"
steht ja weiter im Text.

**Das ist heute der dritte Titel, den ein Projekt-Test korrigiert hat** (nach
Schornstein und den zwei Querverweisen bei den Schuhen). Die Tests sind das
schärfste Werkzeug in diesem Projekt.

### Die Gefahr, die der Eintrag herausstellt

Die Quelle erwähnt beiläufig, dass bei manchen Pumpenbauarten das antreibende
Bachwasser und das geförderte Quellwasser in Berührung kommen, und nennt das
eine ernsthafte Seuchengefahr. Daraus ist ein eigener Abschnitt mit Warnung
geworden: Antrieb über eine WELLE ist unbedenklich, ein gemeinsamer Wasserweg
nicht.

## 12.08.2026 — Filzen: aus Wolle wird Stoff (Bauanleitung)

Vierunddreißigste Bauanleitung, Gruppe „Werkstoffe und Gefäße". Der erste
Eintrag des Pakets über die Herstellung von STOFF. Bisher gab es nur
Kardieren und Garnmachen („Kardieren, Hecheln und Garn machen aus Wolle und
Flachs") und das Flicken — aber nichts, was aus Fasern eine Fläche macht.

### Quelle

- **State of New Jersey, Department of Labor, „Sanitary Standards for the
  Felt Hatting Industry" (1915).** Kein persönlicher Verfasser; das Vorwort
  ist vom Commissioner of Labor als Amtsleiter unterzeichnet, der Text
  verweist nur auf anonyme Untersucher der Behörde. Erscheinen 1915 plus 70,
  längst abgelaufen.

**Einordnung: KEIN Bundeswerk.** New Jersey ist ein Bundesstaat; § 105 gilt
dort nicht. Hier trägt allein die abgelaufene Frist. Die Belegmappe hat das
richtig geprüft.

### Warum eine Arbeitsschutz-Vorschrift eine gute Quelle für ein Handwerk ist

Das Heft will Fabriken sicherer machen und beschreibt dafür jeden Arbeitsgang
genau — samt Zeiten, Maßen und dem, was dabei schiefgeht. Das macht es
brauchbarer als manches Handwerksbuch. Und es liefert nebenbei den Satz, der
diesem Eintrag seinen Kern gibt: **Fell und Wolle filzen bereits, wenn sie im
feuchten Zustand bearbeitet werden — heißes Wasser beschleunigt es nur.**
Mehr braucht es nicht.

### Das Wichtigste ist eine Warnung, und sie steht im Eintrag als eigener Schritt

Das Filzhut-Gewerbe hat seine Arbeiter mit QUECKSILBER vergiftet — das
„Hutmacher-Schütteln" ist danach benannt. Der giftige Arbeitsgang
(„Carrotting") galt aber **ausschließlich FELL**, nicht Wolle. Und dieselbe
Quelle sagt ausdrücklich: **Wolle filzt sogar noch leichter als Fell.**

Damit ist die Regel für dieses Handbuch eindeutig und steht so im Eintrag:
Wolle, Wasser, Wärme, Arbeit — kein Quecksilber, keine Säure, kein Zusatz.
Wer in einer alten Anleitung auf solche Mittel stößt, überspringt sie. Das ist
derselbe Fall wie das Blei bei den Tonglasuren und das Kreosot beim Dach:
eine Quelle, die ein Gift beiläufig nennt, weil es zu ihrer Zeit üblich war.

### Was übernommen wurde und was nicht

Übernommen: die Schuppenstruktur der Faser als Erklärung, das Anfilzen von
Hand (heißes Wasser aufsprengen, in ein weiches Tuch wickeln, rund zehn
Minuten SEHR SANFT rollen), das kräftige Schrumpfen im Sacktuch (20 bis 30
Minuten, Lage immer wieder wechseln, damit alles gleichmäßig filzt), die
Fertigprobe (lässt sich von Hand nicht mehr auseinanderziehen, nur noch
schneiden) und der Hinweis, dass feuchte Stücke mehrere Tage warten können.

**Die Schrumpfzahlen sind das Wertvollste für die Planung:** Von rund 51 mal
56 Zentimetern auf rund 25 mal 38 in einem einzigen Arbeitsgang — ein Rückgang
auf etwa ein Drittel der Fläche. Wer das nicht einplant, legt zu wenig aus.

NICHT übernommen: alles zur Fellverarbeitung mit Quecksilber und Vitriolöl,
außer als Warnung. Und die Maschinen-Abschnitte, die eine Fabrik voraussetzen.

### Keine Zeichnung

Die Tafeln der Quelle sind Fotografien von Fabrikeinrichtungen —
Lüftungshauben, Schrumpfmaschinen, Carrotting-Kammern. Nichts davon zeigt
Handarbeit oder ließe sich auf Wollfilzerei ohne Maschinen übertragen.

### Suchprobe (Tiefe 8)

Kein Notfall-Tipp verdrängt: „gift" führt weiter auf „Gift geschluckt",
„vergiftung" auf „Vergiftung erkennen", „seife" auf „Hände waschen", „wasser"
unverändert auf die Wasseraufbereitung. Neu und eindeutig: „filz", „filzen"
und „quecksilber".

## 12.08.2026 — Färben: welche Beize welche Farbe macht (Bauanleitung)

Fünfunddreißigste Bauanleitung, Gruppe „Werkstoffe und Gefäße". Zusammen mit
dem Filz-Eintrag derselben Stunde deckt das Paket jetzt die Kette Wolle →
Stoff → Farbe ab.

### Quelle

- **Ethel M. Mairet, „A Book on Vegetable Dyes"** (1916), Hampshire House
  Workshops, Ditchling. Verfasserin 1872 bis 1952; Schutzfrist mit dem
  31.12.2022 abgelaufen — also erst seit gut drei Jahren frei. Der Fall zeigt,
  dass sich die Prüfung lohnt: Vier Jahre früher wäre dieses Buch nicht
  benutzbar gewesen.

### Die Hälfte der Quelle ist giftig — und steht deshalb nicht drin

Mairet behandelt sechs Beizen gleichberechtigt: Alaun, Eisen, ZINN, CHROM,
KUPFER — und in einem Baumwollrezept steht ARSEN als Zusatz. Das war 1916
üblich.

**Übernommen mit Mengen und Zeiten wurden nur ALAUN und EISEN.** Für die
anderen nennt der Eintrag, dass es sie gibt und dass sie die Farbe verändern,
aber KEINE Rezepte und KEINE Mengen. Begründet ist das im Eintrag selbst:
Chromsalze sind ätzend und krebserzeugend, Kupfer- und Zinnsalze giftig,
Arsen tödlich — und keiner dieser Stoffe wächst. Wer sie in einer Krise nicht
im Schrank hat, bekommt sie auch nicht.

Dasselbe Muster wie beim Blei in der Töpferei, beim Kreosot am Dach und beim
Quecksilber im Filz: eine Quelle nennt ein Gift beiläufig, weil es zu ihrer
Zeit selbstverständlich war. Es steht als Warnung im Eintrag, nicht als
Anleitung.

Nebenbei liefert die Quelle selbst ein Argument gegen Zinn, das ohne
Giftfrage auskommt: **Es macht die Wolle hart und spröde.**

### Der Satz, um den herum der Eintrag gebaut ist

„Cochineal, if mordanted with alum, will give a crimson colour; with iron,
purple." — **Derselbe Farbstoff gibt mit verschiedenen Beizen verschiedene
Farben.** Wer eine andere Farbe will, braucht oft keine andere Pflanze,
sondern eine andere Beize. Das steht im ersten Schritt, weil es das ganze
Handwerk erklärt.

### Zwei Tests haben wieder zugeschlagen

`TitelwaechterTest`: Der Titel hieß „Färben mit **Pflanzen** …" und trug damit
das reservierte Wort „pflanzen". Geändert auf „Färben: welche Beize welche
Farbe macht". Nachgemessen: „pflanzen" führt wieder zuerst auf „Pflanzen, die
man meiden muss".

`QuerverweiseTest`: Ein Verweis auf „Giftpflanzen: die häufigsten Irrtümer" —
den Titel gibt es nicht, er heißt „Drei Sätze über Giftpflanzen, die falsch
sind". Korrigiert.

**Damit haben die Projekt-Tests am 12.08. vier Titel korrigiert und drei tote
Querverweise gefunden.**

### Eine Falle beim Schreiben der Entwürfe, die dreimal Zeit gekostet hat

Im JSON-Entwurf darf das deutsche Schlusszeichen NICHT als `\"` geschrieben
werden — das ist ein maskiertes gerades Anführungszeichen, kein deutsches. Wer
es hinterher per Skript ersetzt, erwischt zwangsläufig auch die strukturellen
Anführungszeichen von JSON und zerlegt die Datei. **Beim Schreiben gleich das
richtige Zeichen setzen**, dann entfällt der ganze Reparaturschritt.

## 12.08.2026 — Kleidung zuschneiden: Passform ohne Schnittmuster (Bauanleitung)

Sechsunddreißigste Bauanleitung, Gruppe „Werkstoffe und Gefäße". Sie schließt
die Kleidungskette: Wolle → Filz/Garn → Farbe → **Zuschnitt**. Und sie liefert
nachträglich das, was der Schuh-Eintrag offenlassen musste — ein Verfahren,
das OHNE Schnittmuster auskommt.

### Quelle

- **„The Workwoman's Guide, by A Lady"** (1838), London. Kein persönlicher
  Verfasser: Die Titelseite nennt nur „By A Lady". Erscheinen 1838 plus 70,
  seit über hundert Jahren frei.

**Eine Falle, die die Belegmappe richtig erkannt hat und die man sich merken
sollte:** Die Archivkennung des Digitalisats lautet `workwomansguide00hale`
und legt einen Verfasser „Hale" nahe. Auf der Titelseite und im ganzen Vorwort
steht aber kein Name. **Die Katalogkennung ist keine Verfasserangabe** — genau
der Fehler, vor dem die Projektregeln warnen, und hier hätte er zu einer
unnötigen Todesjahr-Suche geführt.

### Warum ein Hausbuch von 1838 für uns brauchbarer ist als ein modernes

Papierschnitte waren damals nicht üblich. Was heute als Notlösung gilt — nach
Körpermaßen arbeiten und am Menschen stecken —, war dort das normale
Verfahren und wird deshalb Schritt für Schritt beschrieben.

**Die zwei Kunstgriffe, die den Eintrag tragen:**

1. **Vier Lagen auf einmal.** Der Stoff wird zweimal gefaltet, bis vier Lagen
   genau übereinanderliegen; die Linie wird mit dem Fingernagel EINGEKNIFFEN
   statt angezeichnet und durch alle vier Lagen zugleich geschnitten. Das
   ergibt von selbst ein symmetrisches Teil — ohne Papier, ohne Übertragen.
2. **Erst stecken, dann schneiden.** Der Stoff wird auf der Person
   festgesteckt, am Hals und am Arm ausgehöhlt, und erst DORT abgeschnitten,
   wo er auf das Nachbarteil trifft.

Papier empfiehlt die Quelle an genau EINER Stelle — nicht als Schnittmuster,
sondern damit die ausgehöhlten Kanten auf beiden Körperhälften gleich
ausfallen.

### Ein Maß, das man leicht falsch nimmt

Bei den fünf Körpermaßen für ein Kleid steht ausdrücklich: Schulter bis
Handgelenk **mit angewinkeltem Arm**. Wer am gestreckten Arm misst, näht einen
Ärmel, der beim Anwinkeln spannt und an der Ellbogeninnenseite aufreißt.

### Der Titelwächter, zum dritten Mal an diesem Tag

Der Titel hieß „**Stiche**, Zuschnitt und Passform ohne Schnittmuster".
`TitelwaechterTest` und `NotfallSucheTest` meldeten gemeinsam: „stich" ist
reserviert und gehört dem Tipp „Insektenstich im Mund" — einem echten Notfall.
Titel geändert auf „Kleidung zuschneiden: Passform ohne Schnittmuster", und
die Zwischenüberschriften von „Grundstiche" auf „Grundnähte". Danach führt
„stich" wieder zuerst auf den Insektenstich, und der Eintrag bleibt über
„kleidung", „zuschneiden", „passform" und „schnittmuster" auffindbar.

**Bilanz der Test-Funde an diesem Tag: fünf Titel korrigiert, drei tote
Querverweise gefunden.** In jedem einzelnen Fall wäre ein Notfall-Eintrag von
Platz eins verdrängt worden.

## 12.08.2026 — Das Vlies: Wollfett, Schur und wie man Wolle beurteilt (Agrikultur)

Dreiundvierzigstes Agrikultur-Kapitel, Gruppe „Tiere". Es setzt VOR dem an,
was „Kardieren, Hecheln und Garn machen aus Wolle und Flachs" schon abdeckt:
Dort wird das Vlies gewaschen, das bereits vom Tier herunter ist. Hier geht es
um das Fett IM Vlies, um das Waschen am lebenden Tier und um die Beurteilung
ohne Geräte.

### Quelle

- **Henry Stephens Randall, „The Practical Shepherd"** (1863). Verfasser 1811
  bis 1876, Schutzfrist längst abgelaufen.

### Der Fund, der zwei Einträge verbindet

Randall erklärt, wozu das Wollfett am Tier dient: Es SCHMIERT die Wollbüschel,
die sich bei jeder Bewegung gegeneinander verschieben. **Ohne diese Schmierung
würde die Reibung die SCHUPPEN der Faser abnutzen oder abbrechen — und genau
auf diesen Schuppen beruht die Fähigkeit zu filzen.**

Das ist derselbe Mechanismus, den die Filz-Quelle von 1915 aus der anderen
Richtung beschreibt: Dort machen die Schuppen den Filz, hier schützt das Fett
sie am lebenden Tier. Zwei Quellen aus verschiedenen Gewerben und aus einem
halben Jahrhundert Abstand, die dasselbe erklären. Beide Einträge verweisen
jetzt aufeinander.

Daraus folgt die Regel, die der Eintrag herausstellt: **Das Wollfett ist kein
Schmutz.** Es wird ausgewaschen, wenn verarbeitet wird — nicht früher. Mit
einer Einschränkung, die die Quelle selbst nennt: An GESCHORENER Wolle greift
es nach einigen Monaten an, wenn es nicht heraus ist.

### Warum nach dem Waschen gewartet wird

Zwischen Waschen und Schur muss Zeit vergehen, damit das Vlies wieder trocken
ist UND ERNEUT FETT AUFNIMMT. Ältere Züchter schoren binnen zehn Tagen, spätere
ließen mehr Zeit — mit dem ausdrücklichen Ziel, dass das Vlies wieder nahezu so
fettig ist wie vorher. Gewaschen wird der SCHMUTZ heraus, nicht das Fett.

### Die Gegenrede steht in derselben Quelle

Randall stellt die Kritiker des Waschens dar, und ihr Einwand ist für uns
wichtiger als der Streit von damals: **Es gebe keinen besseren Ort, sich
Klauenfäule oder Räude zu holen, als einen gemeinsamen Waschpferch.** Der
Eintrag verallgemeinert das ausdrücklich als eigene Einordnung: Jede
gemeinsame Einrichtung für mehrere Herden ist ein Übertragungsweg.

### Was der Eintrag ergänzt, weil die Quelle es nicht sagt

Ein Schaf in einem 1,20 Meter tiefen Bottich kann ERTRINKEN, und ein volles
Vlies zieht es nach unten. Die Quelle erwähnt nur, dass Lämmer sofort heraus
müssen, damit sie nicht zertrampelt werden. Die Warnung steht deshalb als
Einordnung im Schlussabschnitt.

Ebenso ergänzt: Das Verfahren ist für HERDEN gedacht. Bei drei Tieren lohnt
sich der Bottich nicht — dann wäscht man das geschorene Vlies. Der Wert des
Kapitels liegt für kleine Haltungen im Wissen über das Wollfett und in der
Beurteilung.

### Suchprobe (Tiefe 8)

Kein Notfall-Tipp verdrängt: „ertrinken" führt weiter zuerst auf „Ertrinken",
„waschen" auf „Strahlung: waschen und wechseln", „klauen" auf „Nutztiere:
Klauen, Euter, Würmer", „ansteckung" auf „Hasenpest beim Verarbeiten",
„schaf" auf „Schaf und Lamm schlachten". Neu und eindeutig: „vlies",
„wollfett" und „räude".

## 12.08.2026 — Handspindel: neuer Abschnitt im Garn-Kapitel, kein eigener Eintrag

Die Belegmappe `spinnen-handspindel-spinnrad` deckt ausdrücklich NUR die
freihändige Handspindel ab — das Spinnrad steht schon im Kapitel „Kardieren,
Hecheln und Garn machen aus Wolle und Flachs". Acht Sachaussagen sind für
einen eigenen Eintrag zu wenig, und thematisch gehören sie ohnehin dorthin.

**Deshalb als neunter ABSCHNITT in das vorhandene Kapitel eingefügt**, direkt
hinter „Wolle spinnen am großen Spinnrad": `Spinnen ohne Rad: die
Handspindel`. Die Zahl der Einträge ändert sich dadurch nicht — das ist
richtig so, es ist dasselbe Thema.

### Quellen

- **Otis Tufton Mason, „Woman's Share in Primitive Culture"** (1894),
  Kapitel III. Verfasser 1838 bis 1908.
- **Washington Matthews, „Navajo Weavers"** (1884), aus dem Third Annual
  Report of the Bureau of Ethnology. Verfasser 1843 bis 1905.

### Was der Abschnitt bringt

Die Spindel selbst: ein schlanker Stab durch die Mitte einer runden Scheibe.
Zwei Antriebsarten — im Stehen frei in der Luft gewirbelt (dafür braucht der
Schaft OBEN EINEN HAKEN) oder im Sitzen auf dem Oberschenkel angerollt. Dazu
das beidhändige Ausziehen und das Zwirnen nach demselben Verfahren.

Und zwei Stufen darunter, für den Fall, dass nicht einmal eine Spindel da ist:
**das Spinnen am STEIN** (beobachtet an grobem Ziegenhaar für Säcke und
Zeltbahnen) und **das bloße Rollen zwischen Handfläche und Oberschenkel** —
dieselbe Bewegung, mit der ein Schuster seinen Faden verdreht.

Die Beobachtung, die den Abschnitt trägt: Die Navajo kannten das Spinnrad
ihrer Nachbarn, hätten eines kaufen oder bauen können — und blieben beim
einfachen Werkzeug. **Ein Rad spinnt schneller, nicht besser.** Eine Spindel
lässt sich in einer Stunde machen, mitnehmen und im Gehen benutzen.

## 12.08.2026 — Ein selbst gefundener Fehler: der Filz-Titel hat den Spinnentier-Tipp verdrängt

Beim Suchtest für den Handspindel-Abschnitt fiel auf, was mir beim Filz-Eintrag
selbst entgangen war: Der Titel hieß „Filzen: aus Wolle wird Stoff, **ohne
Spinnen und Weben**". Weil die Suche über den Wortanfang trifft, holte
**„spinne" und „spinnen" den Filz-Eintrag vor „Spinnenbiss und Skorpionstich
in Europa"** — also vor einen Notfall-Tipp.

Die Projekt-Tests haben das NICHT gemeldet: „spinne" steht nicht auf ihrer
Liste reservierter Wörter. Gefunden wurde es nur, weil die Suchprobe für den
neuen Abschnitt zufällig „spinnen" enthielt und diesmal weit genug ausgegeben
wurde — beim Filz-Eintrag selbst war die Ausgabe vor dieser Zeile abgeschnitten.

**Titel geändert auf „Filzen: aus loser Wolle wird fester Stoff".** Danach
führen „spinne" und „spinnen" wieder zuerst auf den Notfall-Tipp, und der
Filz-Eintrag bleibt über „filz", „filzen" und „stoff" auf Platz eins.

Der Titelwechsel brach zwei Querverweise (im Färbe-Eintrag und im
Vlies-Kapitel) — `QuerverweiseTest` hat sie sofort gemeldet, beide
nachgezogen.

**Zwei Lehren daraus:**
1. Die Ausgabe der eigenen Suchprobe muss VOLLSTÄNDIG gelesen werden. Ein
   abgeschnittenes `head` ist keine Prüfung.
2. Die Liste der reservierten Wörter im `TitelwaechterTest` ist nicht
   vollständig — sie kennt nur, was jemand eingetragen hat. Wer einen Titel
   setzt, prüft zusätzlich selbst, ob ein Wortanfang darin einem Notfall-Tipp
   gehört.

## 12.08.2026 — Wenden und Garn zurückgewinnen: zwei Abschnitte im Flick-Kapitel

Wie bei der Handspindel: Die Belegmappe `kleidung-ausbessern-wenden` deckt
Flicken und Stopfen ab, was schon steht — ihr eigener Schwerpunkt ist das
WENDEN, und das fehlte. Sieben Sachaussagen sind für einen eigenen Eintrag zu
wenig und gehören thematisch in „Flicken und Stopfen: Kleider länger tragen".

**Zwei neue Abschnitte dort eingefügt**, vor dem Schlussabschnitt:
`Wenden: wenn Flicken nicht mehr reicht` und `Wolle zurückgewinnen aus
verschlissenem Strickzeug`.

### Quelle

- **„Make and Mend for Victory", Heft Nr. S-10** (1942), The Spool Cotton
  Company. Kein persönlicher Verfasser; das Urheberrecht ist auf die Firma
  eingetragen. Erscheinen 1942 plus 70, abgelaufen.

**Zum Charakter der Quelle:** Es ist Werbematerial eines Garnherstellers —
dieselbe Vorsicht wie beim Wasserrad-Katalog. Die Handgriffe sind davon
unberührt; dass sie Nähgarn, Bügeleisen und Maschine voraussetzen, steht als
Einordnung im Schlussabschnitt.

### Was neu dazukommt

**Wenden:** Wollstoff lässt sich oft umkehren, wenn die Außenseite
fadenscheinig ist — sogar bei deutlich verschiedener rechter und linker Seite.
Erst waschen, dann auftrennen. Beim Auftrennen ist die Zieh-Methode der
Schneid-Methode überlegen, weil KEINE FADENRESTE in der Naht bleiben. Und der
Schritt, den man nicht überspringen darf: nach dem Auftrennen jedes Teil
bügeln und **Fadenlauf sowie rechte und linke Seite markieren** — ohne
Nähte sieht man einem Stück Stoff nicht mehr an, wo oben war.

**Garn zurückgewinnen:** Aus Handgestricktem läuft das Garn in einem
durchgehenden Strang ab. Strang wickeln, an vier Stellen abbinden, in warmem
Wasser einweichen, ausdrücken — und **mit einem GEWICHT am unteren Ende
aufhängen**. Das Gewicht ist der eigentliche Punkt: Gebrauchtes Strickgarn
behält sonst seine Kringel, und man strickt hinterher mit Kräuselband.

### Eine Kleinigkeit mitgezogen

Die Schlussüberschrift des Kapitels hieß „Was diese Quelle nicht hergibt" —
mit der zweiten Quelle stimmte der Singular nicht mehr. Auf „Was diese Quellen
nicht hergeben" geändert und um die Einordnung zum Werbeheft ergänzt.

### Suchprobe (Tiefe 8)

Nichts verdrängt. „wenden" führt weiter zuerst auf „Boden vorbereiten: tief
lockern, nicht wenden", „flicken" und „stopfen" unverändert auf das
Flick-Kapitel. Neu und eindeutig: „auftrennen", „kragen" und „strickzeug"
führen jetzt dorthin.

## 12.08.2026 — Kalk brennen: aus Stein wird Branntkalk (Bauanleitung)

Siebenunddreißigste Bauanleitung, Gruppe „Werkstoffe und Gefäße". Sie liefert
den Eingangsstoff für das Kapitel „Kalk löschen und verwenden: Mörtel, Tünche,
Stallhygiene", das gebrannten Kalk bisher voraussetzte, ohne zu sagen, woher
er kommt.

### Zum VIERTEN Mal dieselbe Verwechslung in einer Belegmappe

Die Mappe `kalk-brennen-loeschen` hat „Making Lime on the Farm" (USDA
Farmers' Bulletin 1801, 1938) VERWORFEN, weil die Titelseite „By N. A.
Kessler, Bureau of Agricultural Engineering" nennt. Auch das ist ein
US-Bundeswerk — § 105 hängt am Dienstverhältnis, nicht an der Namensnennung.
Und ausgerechnet dieses Heft deckt das eigentliche Brennen ab, das in der
Mappe deshalb fehlt.

**Betroffene Mappen bisher: `lehmbau`, `quellfassung`, `dachdecken`,
`kalk-brennen-loeschen`.** Der Fehler ist systematisch, nicht zufällig.

### Am Seitenbild geprüft — und wieder eine Zahl gerettet

Die Texterkennung machte aus „on the SIXTH day of burning" ein unlesbares
Fragment, das sich leicht als „erster Tag" lesen lässt. Auf Seite 11 steht
klar: **am sechsten Tag** 518 °F oben und 840 °F unten am Rand; **sieben Tage
später** hell kirschrot, also rund 1500 °F. Der Unterschied ist erheblich —
er entscheidet darüber, ob man einen Kalkbrand für eine Sache von Tagen oder
von Wochen hält.

Ebenfalls am Bild geprüft: die Brenntemperatur 1400 bis 2200 °F, die Grenzen
für Verunreinigungen (rund 2 Prozent Kieselsäure unschädlich, Tonerde und
Eisen zusammen höchstens ein halbes bis ein Prozent) und der ganze Aufbau des
Stapels mit seinen Maßen.

### Der Satz, um den herum der Eintrag gebaut ist

„In burning, a high temperature may be used for a short time or a lower
temperature for a longer time. **The more nearly the amount of heat used
approaches the minimum required, the better the quality of the lime.**"

Also: lieber länger und kühler als kürzer und heißer. Der Fehler, den man
nicht mehr gutmachen kann, heißt TOTGEBRANNT — solcher Kalk lässt sich nicht
mehr löschen, sieht aber wie guter aus, und wenn er Jahre später im fertigen
Mörtel doch noch Wasser zieht, sprengt er ihn auf.

### Die Zeichnung

Figur 3 des Hefts, der Kalkstapel aus Kentucky mit Maßen. Unsigniert, aus
einem freien Bundeswerk. Die englische Beschriftung war diesmal MITTEN IN die
Zeichnung gesetzt („Seasoned logs 12' long" quer über die Holzlage) — beim
Entfernen bleiben dort helle Lücken. Das ist der Preis dafür, dass das Bild
deutsch beschriftet ist; die Struktur bleibt eindeutig lesbar.

### Der Titelwächter, zum vierten Mal an diesem Tag

Der Titel hieß „Kalk brennen: der Stapel als **Ofen**". `NotfallSucheTest` und
`ZusammenspielTest` meldeten dasselbe wie beim Schornstein: „ofen" gehört dem
Tipp „Kohlenmonoxid: unsichtbar und geruchlos", also dem Eintrag mit der
Notrufnummer. Geändert auf „Kalk brennen: aus Stein wird Branntkalk".

### Eine Verschiebung, die stehen bleibt

Bei „kalk" steht jetzt der neue Eintrag vor „Kalk löschen und verwenden", und
„Verätzung" rutscht von Platz zwei auf drei. Beides sind keine Verdrängungen
von Platz eins, und die Projekt-Tests beanstanden nichts. Der Eintrag verweist
im Schlussschritt ausdrücklich auf den Abschnitt über Haut und Augen —
**vor dem ersten Brand zu lesen, nicht danach.**

## 12.08.2026 — Scheren: zwei Abschnitte, die eine selbst benannte Lücke schließen

Das Kapitel „Das Vlies: Wollfett, Schur und wie man Wolle beurteilt" sagte in
seinem Schlussabschnitt ausdrücklich: „ERSTENS, KEINE ANLEITUNG ZUM SCHEREN
SELBST … Das bleibt offen." Die Belegmappe `schur-wolle-sortieren` schließt
genau das — **und zwar aus DERSELBEN Quelle** (Randall, „The Practical
Shepherd", 1863), die im Kapitel schon zitiert ist. Es war also keine Lücke
der Quelle, sondern eine der ersten Auswertung.

**Zwei neue Abschnitte:** `Scheren: der Handgriff und die Sorgfalt` und
`Das Vlies aufnehmen, binden und lagern`.

### Was dazukommt

Die vier Teile der Sorgfaltsregel: gleichmäßig und glatt schneiden, das Vlies
nicht zerreißen, dieselbe Stelle nicht zweimal schneiden, die Haut nicht
verletzen. Dazu der Satz, bei dem die Quelle unmissverständlich wird:
**wiederholte und schwere Schnitte sollten immer zur Entlassung des Scherers
führen.**

Und die zwei Fehler in entgegengesetzte Richtungen, die beide dem Tier
schaden:
— **Zu tief:** Am Euter der Mutterschafe hat der Verfasser WIEDERHOLT gesehen,
  wie eine Zitze abgeschnitten wurde — das macht das Tier dauerhaft unfähig,
  an dieser Seite Milch zu geben.
— **Zu dicht an der Haut:** Zeigt sich die Haut nackt und rot, ist sie
  Sonnenbrand ausgesetzt, und das Tier leidet schon bei mäßiger Kälte.

Praktisch verwertbar außerdem: nur so viele Tiere in den Pferch, wie die
Scherer in DREI STUNDEN schaffen; verschmutzte Tiere zuerst und abseits
vorbereiten; der Fänger HEBT das Schaf statt es zu schleifen; rund 25
Merinoschafe am Tag von Hand; das nächste Tier auf eine ANDERE Stelle setzen,
bevor das vorige Vlies aufgenommen wird; Vlies mit der Innenseite nach oben
aufnehmen; binden mit Flachs- oder Hanfschnur, NICHT mit Wollschnur.

### Zwei Stellen, die dadurch falsch wurden — und nachgezogen sind

Das ist der Punkt, den man beim Erweitern eines Kapitels leicht übersieht:

1. Die Einleitung sagte „DREI DINGE STEHEN HIER" und zählte drei auf. Jetzt
   sind es fünf.
2. Der Schlussabschnitt behauptete weiterhin, es gebe keine Anleitung zum
   Scheren. Ersetzt durch die verbleibende, kleinere Lücke: Die Quelle
   beschreibt **die Schere selbst** nicht — nicht ihr Aussehen, nicht das
   Schärfen, nicht die Reihenfolge am Tier. Wer noch nie geschoren hat, weiß
   danach, WORAUF es ankommt, aber nicht, WO er ansetzt.

**Merksatz für künftige Erweiterungen:** Wer einem Kapitel Abschnitte
hinzufügt, muss die Einleitung und den „Was fehlt"-Abschnitt mitlesen. Beide
enthalten Aussagen ÜBER das Kapitel, und die werden durch jede Erweiterung
falsch.

### Suchprobe (Tiefe 8)

Nichts verdrängt: „euter" führt weiter zuerst auf „Nutztiere: Klauen, Euter,
Würmer", „haut" auf den Angelhaken, „sonnenbrand" auf „Verbrennung
versorgen". Neu und eindeutig: „scheren", „schur", „schere" und „zitze"
führen jetzt zuerst auf das Vlies-Kapitel.

## 12.08.2026 — Flecken: ein Abschnitt im Seifen-Kapitel

Letzte gefüllte Belegmappe des Tages: `waschen-fleckenentfernung`, USDA
Farmers' Bulletin 1474 „Stain Removal from Fabrics: Home Methods". Kein
persönlicher Verfasser („Prepared by the Division of Textiles and Clothing"),
Bundeswerk. Als eigener Eintrag zu speziell — als Abschnitt in „Seife und
Wäsche ohne Nachschub" richtig.

### Übernommen wurde der sichere Kern

**Die Reihenfolge:** Wasser → Lösungsmittel → Bleiche. Vor jeder Chemikalie
prüfen, ob Wasser allein reicht; jedes Mittel zuerst an einer unauffälligen
Stelle. Der Grund steht dabei: Jeder Schritt greift die Faser stärker an als
der vorige.

**Was welche Faser zerstört** — das ist der Teil, den man auswendig können
sollte, weil er sich nicht rückgängig machen lässt: Starke Säuren zerstören
Baumwolle und Leinen (danach mit schwacher Lauge neutralisieren). Starke
Laugen lösen bei Wolle und Seide die Faser auf. Chlorbleiche zerstört Wolle
und Seide. Sehr heißes Wasser lässt Wolle und Seide vergilben.
**Merksatz daraus:** Tierische Fasern vertragen keine Lauge und keine Hitze,
pflanzliche keine Säure — und wer nicht weiß, was er vor sich hat, nimmt
Wasser.

**Die aufsaugenden Pulver** (Kreide, Magnesit, Walkerde, Maismehl): für alle
Fasern unbedenklich, gut bei frischen Flecken, unzuverlässig bei alten — und
festes Fett muss vorher mit einem warmen, NICHT heißen Bügeleisen angeweicht
werden.

### Nicht übernommen — zum wievielten Mal an diesem Tag

Das Heft nennt Benzol, Benzin, Aceton, Tetrachlorkohlenstoff, Oxalsäure,
Javellewasser, Kaliumpermanganat und Natriumhydrosulfit mit Mengen. **Keine
dieser Rezepturen steht im Paket.** Benzol und Tetrachlorkohlenstoff gelten
heute als krebserzeugend beziehungsweise leberschädigend; Oxalsäure ist ein
Gift, und die Quelle verlangt selbst, die Flasche mit „Poison" zu beschriften.

Was ÜBERNOMMEN wurde, ist die Warnung der Quelle selbst: Die Dämpfe aller
organischen Lösungsmittel schaden beim Einatmen — nur im Freien oder bei sehr
guter Lüftung. Ergänzt um den Hinweis, dass die meisten davon brennen.

Damit ist das Muster an diesem Tag fünfmal aufgetreten: Blei in der Töpferei,
Kreosot am Dach, Quecksilber im Filz, Chrom beim Färben, Lösungsmittel und
Oxalsäure beim Fleckenentfernen. **Alte Hausbücher nennen Gifte beiläufig,
weil sie zu ihrer Zeit im Schrank standen.**

### Und schon wieder zwei stale Stellen im erweiterten Kapitel

Derselbe Fehler wie beim Vlies-Kapitel, diesmal sofort gesucht:
1. Der Schlussabschnitt sagte „keine der FÜNF Quellen nennt eine Zahl" — es
   sind jetzt sechs. Auf „keine der Quellen" geändert.
2. Er sagte: „WIE MAN KALK BEKOMMT. … steht in keiner dieser Quellen." Das
   stimmt seit heute nicht mehr — das Brennen steht in „Kalk brennen: aus
   Stein wird Branntkalk". Der Satz verweist jetzt dorthin.

### Ein Werkzeug-Hinweis für die Zukunft

Der Quellennachweis war 537 Zeichen lang, erlaubt sind 500 — `einbauen.py`
hätte das vor dem Schreiben gemeldet, aber bei Abschnitten, die man direkt in
die JSON einfügt, läuft das Werkzeug nicht mit. **Wer Abschnitte von Hand
einfügt, prüft die Längen selbst** oder lässt sich vom Gradle-Lauf erwischen.

## 12.08.2026 — Die Lizenz-Formulierung: alle 99 Stellen umgestellt

Das war das offene Arbeitspaket aus der Rechte-Prüfung: 99 Quellennachweise
im Paket trugen noch die alte, zu kurze Begründung. Sie ist jetzt überall
ersetzt.

**Alt:** „Gemeinfrei als Werk einer US-Bundesbehörde (17 U.S.C. § 105)."
**Neu:** „Werk der US-Bundesregierung, über den Schutzfristenvergleich frei
(§ 121 Abs. 4 UrhG, 17 U.S.C. § 105)."

Der Unterschied ist kein Wortgeklingel: Die alte Fassung nannte nur
US-Recht und begründete damit für einen deutschen Nutzer nichts. Die neue
nennt die Norm, die hier tatsächlich greift.

### Warum das nicht blind ersetzt werden durfte

§ 105 gilt nur für den **Bund**, nicht für die Bundesstaaten — genau deshalb
war das als eigenes Paket vermerkt und nicht als Suchen-und-Ersetzen.
Vor dem Ersetzen wurden deshalb alle 99 Fundstellen nach dem Feld `name`
ihrer Quelle gruppiert. Ergebnis, vollständig:

| Anzahl | Herausgeber |
|---|---|
| 40 | US Army |
| 35 | US-Landwirtschaftsministerium (USDA) |
| 8 | US-Heer — John F. Kennedy Special Warfare Center and School |
| 4 | US-Arzneimittelbehörde (FDA) |
| 2 | US-Forstdienst (USDA Forest Service) |
| je 1 | CDC, U.S. Naval Observatory, NASA, SAMHSA, War Department |

**Alles Bundesstellen, kein einziger Landesträger darunter.** Damit war die
Ersetzung für alle 99 zulässig. Die vier Landes- und Hochschulquellen im
Paket (New Jersey Department of Labor, State Geologist New Jersey, Kansas
State Board of Health, University of Arizona Cooperative Extension) tragen
unverändert ihre eigene, individuell geführte Begründung nach Erscheinen + 70
beziehungsweise Tod + 70 — sie waren von der Ersetzung nie betroffen.

### Zwei Nebenbefunde

**Die Längengrenze schlägt zu.** Der neue Satz ist 42 Zeichen länger, und
Quellennachweise dürfen höchstens 500 Zeichen haben. Bei 8 Stellen hätte die
volle Fassung die Grenze gerissen (bis 524 Zeichen). Dort steht die
Kurzfassung ohne die Paragrafen: „Werk der US-Bundesregierung, über den
Schutzfristenvergleich frei." Die Aussage ist dieselbe, die Normen stehen
hier. Die längste Angabe im Paket liegt jetzt bei 499 Zeichen.

**Eine zweite Schreibweise war fast durchgerutscht.** Fünf Guides trugen die
Begründung in Klammern hinter der Fundstelle: „(gemeinfrei,
US-Bundesbehördenwerk)" — andere Wortstellung, gleiche Aussage. Ein
Suchen-und-Ersetzen auf den Hauptsatz hätte sie stehen lassen. Gefunden
wurden sie nur, weil nach dem Ersetzen noch einmal nach dem Wortstamm
„Bundesbeh" gesucht wurde. **Merksatz: Nach jedem Massenersetzen nach dem
Wortstamm suchen, nicht nach dem ersetzten Satz.**

## 12.08.2026 — Karte und Kompass: drei Tipps aus dem Feldhandbuch

Die Kachel „Orientierung" war mit 9 Einträgen die mit Abstand dünnste des
Pakets — und was drin stand, war ausschließlich Behelf: Norden finden mit
Stock, Uhr, Polarstern, Nadel. **Wie man eine Richtung HÄLT und wie weit man
gekommen ist, stand nirgends.** Genau daran scheitern Wanderungen im Ernstfall.

### Die Quelle

„FM 3-25.26 Map Reading and Land Navigation", US Army, Ausgabe 20. Juli 2001.
Volltext gesichert als `work/quellen/orientierung/army-fm3-25-26-2001-map-reading.txt`,
die PDF daneben für die Seitenbilder. Werk der US-Bundesregierung, über den
Schutzfristenvergleich frei. Kein persönlicher Verfasser.

Alle Zahlen wurden am Seitenbild gegengeprüft, nicht am erkannten Text:
Seite 5-11 (Schrittzahl) und Seite 9-6 (Versatz und Umgehung). Die
Texterkennung war diesmal sauber — die Zahlen stimmten alle.

### Was übernommen wurde

**„Entfernung schätzen: die eigene Schrittzahl eichen".** Ein Schritt ist rund
76 cm (30 Zoll), aber es zählt die eigene Zahl: eine bekannte Strecke abgehen,
100 bis 600 Meter, bei 600 durch sechs teilen. Die Messstrecke muss dem
späteren Gelände ÄHNELN. Und der Rat, der den Unterschied macht: nicht im Kopf
mitzählen, sondern bei jedem Hundertmeter etwas ablegen — Steinchen, Knoten,
Strich. Dazu die sechs Einflüsse, die die Zahl verfälschen; auffällig ist,
dass fast alle den Schritt KÜRZER machen. Wer nach seiner Schönwetterzahl
rechnet, glaubt also regelmäßig, weiter zu sein, als er ist.

**„Bewusst daneben zielen, damit man weiß, wohin man abbiegen muss".** Der
wertvollste Kniff des Kapitels und völlig unintuitiv. Liegt das Ziel an einer
langen Linie (Bach, Straße, Waldkante), zielt man absichtlich zehn Grad
daneben — dann weiß man an der Linie sicher, in welche Richtung man abbiegen
muss, statt zu raten. Faustzahl der Quelle: ein Grad verschiebt um etwa
18 Meter je 1000 Meter.

**„Um ein Hindernis herum, ohne die Richtung zu verlieren".** Vier Züge im
rechten Winkel; erster und dritter Zug gleich lang und entgegengesetzt, damit
sie sich aufheben. Dazu der Handgriff für den Marschkompass im Dunkeln über
die Leuchtbuchstaben E und W, ohne die Ringstellung anzufassen.

### Der Werkzeugfehler, der 40 Tests rot gemacht hat

`einbauen.py` konnte bis heute ausgerechnet Tipps nicht — 351 der 431 Einträge.
Beim Nachrüsten ist eine neue Tipp-Gruppe ohne das Pflichtfeld `category` ins
Paket gelangt. Folge: **Das ganze Paket lud nicht mehr**, und rund 40 Tests
schlugen mit derselben Meldung fehl. Der Fehler war eine Zeile, die Suche
danach nicht. Das Werkzeug prüft die Kategorie jetzt gegen die vorhandenen
Gruppen derselben Datei — und prüft bei Tipps auch Textlänge, Stichwortzahl
und Dringlichkeitsfelder.

### Vier verdrängte Notfall-Tipps — und wie sie gefunden wurden

Die erste Fassung hat vier Wörter von Platz eins verdrängt. Gemessen wurde
das nicht durch Hinsehen, sondern durch einen **Vorher-Nachher-Vergleich**:
das alte `tips.json` aus `git show HEAD:` in ein zweites Paketverzeichnis
gelegt und dieselbe Suchprobe zweimal gefahren.

| Wort | vorher Platz 1 | erste Fassung |
|---|---|---|
| meter | erste-hilfe-strahlung | Schrittzahl |
| strecke | erste-hilfe-verletzten-bewegen | Schrittzahl |
| grad | erste-hilfe-fieber-gefahr | Versatz |
| karte | erste-hilfe-bienen-wespenstich | Schrittzahl |

Am schwersten wog „grad": Wer 39 Grad am Fieberthermometer abliest und „grad"
tippt, muss zum Fieber-Tipp kommen, nicht zu einem Marschkniff. Behoben durch
Umbenennen („…auf hundert Meter" → „…eichen") und durch Streichen der
Stichwörter, die diese Allerweltswörter als eigenes Wort enthielten. Danach
stand überall wieder der alte Tipp auf Platz eins.

**Und eine fünfte hat der Projekt-Test gefunden, die in meiner Wortliste
fehlte:** „abstand" führte auf die Schrittzahl statt auf die Abstandsregel für
den Abort zum Wasser. Merksatz: Die eigene Suchprobe prüft, woran man denkt —
`NotfallSucheTest` prüft, woran man nicht gedacht hat. Beide fahren, nicht nur
eines.

### Ein Werkzeug für den Vorher-Nachher-Vergleich

Die Wegwerf-Klasse `SuchprobeTemp.kt` liest das Paketverzeichnis jetzt aus der
Umgebungsvariable `PROBE_PAKET`, wenn sie gesetzt ist. Wichtig dabei: Gradle
reicht `-D` NICHT an die Test-JVM durch (die erste Messung sah deshalb
täuschend nach „nichts verändert" aus, weil beide Läufe dasselbe Paket lasen).
Umgebungsvariablen werden durchgereicht — und beim Zurückschalten muss die
Variable `unset` werden, ein leerer Wert ist nicht dasselbe wie nicht gesetzt.

## 12.08.2026 — Zwei gezeichnete Blätter, und warum sie NICHT aus der Quelle stammen

Zu den zwei Marschverfahren gehören Bilder, sonst versteht sie niemand. Die
Quelle hat beide als Figuren (9-5 und 9-6). Übernommen wurde trotzdem keine.

**Grund eins, der harmlose:** Die Beschriftung steckt im Bild und ist
englisch. Man müsste sie überkleben, und überklebte Stellen sehen genau
danach aus.

**Grund zwei, der wichtige: Figur 9-6 widerspricht ihrem eigenen Text.** Der
Fließtext derselben Seite rechnet vor, dass der Ankunftspunkt bei zehn Grad
Versatz auf 1000 Metern rund **180 Meter** neben dem Ziel liegt. Die
Zeichnung beschriftet genau diese Strecke mit **„100 METERS"**. Nachgerechnet
stimmt der Text: 10 Grad mal 18 Meter je 1000 Meter sind 180 Meter. Die Zahl
in der Zeichnung ist falsch.

Das ist der Beleg für die Hausregel in ihrer schärfsten Form. Die Zahl wäre am
Seitenbild NICHT als Erkennungsfehler aufgefallen — sie steht dort sauber
gedruckt. Sie ist im Original falsch. Wer die Figur übernimmt, übernimmt den
Fehler mit, und zwar in einem Bild, das niemand mehr gegenprüft.

### Deshalb ein neues Werkzeug: `tools/skizzen/schema_zeichnen.py`

Es zeichnet SCHEMATISCHE Figuren — Wege, Winkel, Abstände — statt eine
Stichzeichnung einzusetzen. Es benutzt Blattton, Schrift und Fußzeile aus
`stich_beschriften.py` weiter, damit die Bilder zusammenpassen.

Die Trennlinie zwischen beiden Werkzeugen: **Gegenständliches kommt aus der
Quelle** (eine Räucherkammer freihändig zu zeichnen wäre schlechter als jeder
Stich), **Geometrie wird neu gezeichnet** (dort ist Freihand genauer, weil man
die Zahl selbst setzt).

Entstanden sind `bewusster-versatz.png` und `hindernis-umgehen.png`, beide
1100 mal 700 Pixel. Die Beschriftung liegt bei 17 Pixeln auf 1100 Breite —
Verhältnis 0,0155, also genau die Schwelle, die am 11.08. als Untergrenze für
die Lesbarkeit am Handy festgehalten wurde.

Zwei Sachen sind beim Zeichnen schiefgegangen und stehen als Kommentar im
Werkzeug: Rechts stehende Beschriftungen liefen aus dem Blatt heraus, und ein
Winkelbogen über zehn Grad ist so flach, dass er wie ein Fleck aussieht — die
Zahl steht jetzt im Keil zwischen den beiden Wegen statt an einem Bogen.

## 12.08.2026 — „Wo bin ich?": der Standort aus zwei Sichtlinien

Vierter Eintrag aus FM 3-25.26, Abschnitte 6-8 „Resection", 6-9 „Modified
Resection" und 11-1 „Orienting the Map". Damit ist die Kachel Orientierung von
9 auf 13 Einträge gewachsen, und sie deckt jetzt nicht mehr nur „Wo ist
Norden", sondern auch „Wo bin ICH" und „Wie halte ich die Richtung".

### Die Reihenfolge ist bewusst umgedreht

Die Quelle nennt zuerst den Weg mit Karte und Kompass und danach den Weg mit
der geraden Kante. **Im Eintrag steht der Weg OHNE Kompass zuerst.** Grund:
Der Kompassweg verlangt, die Peilung um den Unterschied zwischen magnetischem
Nordpol und Kartengitter zu korrigieren. Wer diesen Schritt vergisst, bekommt
Linien, die genau um diesen Winkel danebenliegen — und merkt es nicht. Der
Weg mit der geraden Kante kennt das Problem gar nicht: Karte ausrichten,
Kante über den bekannten Punkt legen, auf das echte Ding drehen, Linie ziehen.

Ausdrücklich als Einordnung gekennzeichnet, nicht als Aussage der Quelle:
Ein ungenauer Standort ist unendlich viel besser als gar keiner.

### Was aus der Quelle mitgenommen wurde und in vielen Anleitungen fehlt

**Drei Punkte statt zwei.** Die Quelle empfiehlt drei oder mehr für höhere
Genauigkeit. Der praktische Wert steckt nicht in der Genauigkeit selbst: Bei
zwei Linien sieht JEDES Ergebnis richtig aus, auch ein falsches. Drei Linien
ergeben ein kleines Dreieck, und dessen Größe ist die eingebaute Warnung.

**Der Fall auf einer Linie** („Modified Resection"): Wer schon weiß, dass er
auf einer Straße oder an einem Bach steht, braucht nur EINEN bekannten Punkt.
Das ist der häufigste Fall in der Praxis und der schnellste.

### Verdrängungsprobe: eine bewusste Ausnahme, begründet

Vorher-Nachher wie beim letzten Eintrag. Vier Wörter haben den Platz eins
gewechselt:

| Wort | vorher | jetzt | Bewertung |
|---|---|---|---|
| wo bin ich | orientierung-schattenstock | Kreuzpeilung | besser |
| standort | feuer-lagerfeuer-sicher | Kreuzpeilung | besser |
| position | feuer-ohne-zuendmittel | Kreuzpeilung | besser |
| karte | erste-hilfe-bienen-wespenstich | Kreuzpeilung | **bewusst so gelassen** |

Der letzte ist der einzige, bei dem ein Erste-Hilfe-Eintrag weicht, und er
wird hier festgehalten, damit ihn niemand später für ein Versehen hält.
Beim Schrittzähl-Eintrag wurde dieselbe Verdrängung noch zurückgenommen — dort
war sie unsinnig, weil es in dem Eintrag gar nicht um Karten ging. Hier ist es
umgekehrt: **Die Kreuzpeilung ist der karten-bezogenste Eintrag des ganzen
Pakets.** Wer „karte" tippt, will die Karte. Der Wespenstich-Eintrag (er
enthält das Wort wegen des Notfallausweises) steht weiterhin auf Platz zwei,
und wer wirklich gestochen wurde, tippt „wespe", „stich" oder „allergie" —
dort führt unverändert er.

`NotfallSucheTest` bleibt grün; keine der dort abgesicherten Garantien ist
berührt.

Dazu ein drittes gezeichnetes Blatt, `kreuzpeilung.png`, mit demselben
Werkzeug wie die beiden vorigen.

## 12.08.2026 — Höhenlinien: die eine Regel, die Tal und Grat unterscheidet

Fünfter Eintrag aus FM 3-25.26, Kapitel 10 „Elevation and Relief". Damit hat
die Orientierung 14 Einträge und eine zweite neue Gruppe, „Die Karte lesen".

### Gelesen wurde am Seitenbild, nicht am Text

Dieses Kapitel besteht fast nur aus Figuren mit kurzen Absätzen dazwischen. Die
Texterkennung der Archivfassung gibt davon **fast nur Bildunterschriften** her
— „Figure 10-17. Hill.", „Figure 10-18. Saddle." — und die Absätze dazwischen
fehlen oder sind zerrissen. Wer sich darauf verlässt, schreibt aus
Bildunterschriften ab.

Deshalb wurden die Seiten 134 bis 136 der PDF gerendert und gelesen
(`tools/inhalt/figuren.py seiten army-fm3-25-26 134 136 --lupe 2`). Erst dort
standen die Definitionen vollständig. **Merksatz: Wenn die Texterkennung
auffällig kurze Absätze zwischen vielen Bildunterschriften liefert, fehlt der
Text — nicht die Quelle ist dünn, sondern die Erkennung.**

### Die Regel, um die es geht

Tal und Grat sehen auf der Karte fast gleich aus: beide als Reihe von U oder V.
Die Quelle gibt den einen Unterschied:

* **Tal:** Das geschlossene Ende des U oder V zeigt IMMER zum hohen Boden,
  also flussaufwärts.
* **Grat:** Das geschlossene Ende zeigt vom hohen Boden WEG.

Daraus folgt der Nebeneffekt, der den Eintrag für dieses Handbuch besonders
wertvoll macht: **Man sieht der Karte an, wohin das Wasser fließt.** Wer Wasser
sucht, braucht dafür nichts als diese Regel.

Übernommen sind außerdem die fünf großen Formen (Kuppe, Sattel, Tal, Grat,
Mulde) und die zwei kleineren (Rinne, Nase) samt ihrer Merkbilder — Kuppe als
Ringe, Sattel als Sanduhr, Mulde als geschlossene Linie mit Strichen zur tiefen
Seite. Dazu die Zahl unten am Kartenrand, ohne die die Linien nichts über die
Steilheit aussagen.

### Wieder ein Titelwort zurückgenommen: „Senke"

Der Eintrag hieß im ersten Wurf die Vertiefung „SENKE" und hatte das auch als
Stichwort. Ergebnis der Vorher-Nachher-Probe: **„senke" führte vorher auf
`erste-hilfe-fieber-versorgen`** — weil die Suche Wortanfänge vergleicht und
„senke" der Anfang von „senken" ist. Fieber SENKEN. Ein Wort, das jemand mit
einem fiebernden Kind tippt.

Behoben durch das gleichwertige Wort **„Mulde"**, in Titelzeile des Abschnitts
und Stichwort. Danach führt „senke" wieder auf den Fieber-Eintrag, und der neue
Eintrag steht dort gar nicht mehr in den ersten acht.

Das ist an diesem Tag die sechste Verdrängung dieser Art. Alle sechs hatten
dieselbe Ursache: **ein Allerweltswort, das zufällig der Anfang eines
Notfallworts ist.** Die Liste bisher: meter, strecke, grad, karte, abstand,
senke.

### Ein viertes Blatt

`hoehenlinien-tal-grat.png`, wieder mit `schema_zeichnen.py`: zwei Felder
nebeneinander, links Tal, rechts Grat, gleiche Form, entgegengesetzte Richtung
der Spitze. Reine Geometrie, also neu gezeichnet — die Quellfiguren wären hier
ohnehin nicht zu gebrauchen gewesen, weil sie Höhenlinienbild und Landschaft
nebeneinanderstellen und englisch beschriftet sind.

## 12.08.2026 — Entfernungen schätzen, und die Täuschungen dabei

Sechster Eintrag aus FM 3-25.26, Abschnitt 5-3 d mit Tafel 5-1. Orientierung
steht damit bei 15 Einträgen.

### Zwei Verfahren, beide am Seitenbild geprüft

**Hundert Meter als Baustein:** bis 500 Meter direkt abzählen, darüber einen
Punkt auf halbem Weg nehmen und verdoppeln. Beide Zahlen am Bild bestätigt.

**Sehen und Hören auseinanderzählen:** Sekunden zwischen Blitz und Knall mal
**330 Meter**. Auch am Bild bestätigt (die Quelle nennt daneben 350 Meter als
Wert der Artillerie — der ist NICHT übernommen, weil er nur für deren eigene
Rechenverfahren gilt).

Die Quelle meint Waffenfeuer. Der zivile Anlass ist das Gewitter, und das steht
ausdrücklich als Einordnung dabei, nicht als Aussage der Quelle. **Was daraus
folgt — wie nah zu nah ist und was man dann tut — steht bewusst NICHT in diesem
Eintrag**, sondern wird auf „Blitzschlag: umgekehrte Reihenfolge bei mehreren
Getroffenen" verwiesen. Ein Orientierungs-Eintrag ist nicht der Ort für
Gewitterschutz.

### Die Tafel ist der eigentliche Wert

Fünfzehn Fälle, in denen sich fast jeder verschätzt, sortiert nach der
Richtung des Fehlers. Dahinter steckt ein Muster, das im Eintrag als
Einordnung benannt wird: **Je mehr und je klarer man sieht, desto näher wirkt
es. Je weniger und je trüber, desto weiter.** Wer das begriffen hat, braucht
die Listen nicht auswendig.

Der praktische Schluss daraus, ebenfalls als Einordnung gekennzeichnet: Im
Nebel und in der Dämmerung hält man alles für weiter weg, als es ist — auch
die Gefahr. Über See und Schneefeld hält man das andere Ufer für viel näher.

### Zwei alte Bekannte in einem Eintrag

**Die Anführungszeichen-Falle, zum zweiten Mal.** Im Entwurf stand
`„einundzwanzig, zweiundzwanzig, dreiundzwanzig"` mit einem GERADEN
Schlusszeichen. Das beendet die JSON-Zeichenkette mitten im Satz;
`einbauen.py` meldete einen Syntaxfehler in Zeile 14. Genau der Fehler, für den
das Werkzeug gebaut wurde — es hat ihn gemeldet, bevor irgendetwas geschrieben
wurde. **Deutsche Schlusszeichen immer als U+201C schreiben, nie als `"`.**

**Und die siebte und achte Verdrängung.** „wie weit" führte vorher auf
`erste-hilfe-wann-aufhoeren` (wann man eine Wiederbelebung abbricht), „weit"
auf `erste-hilfe-durchfall-essen`. Der Eintrag hieß im ersten Wurf „Wie weit
ist das? Schätzen, ohne hinzugehen".

Behoben durch denselben Griff wie immer: **„weit" raus aus Titel und
Stichwörtern, im Fließtext darf es bleiben.** Neuer Titel: „Entfernungen
schätzen, ohne hinzugehen". Danach stehen beide Erste-Hilfe-Einträge wieder
auf Platz eins.

**Der Preis ist diesmal ehrlich zu benennen:** „wie weit" ist die
natürlichste Art, diese Frage zu stellen, und der Eintrag steht dafür jetzt
NICHT MEHR in den ersten acht. Gefunden wird er über „entfernung", „schätzen",
„donner", „schall" und „hundertmeter". Das ist ein bewusster Tausch: lieber
ein Orientierungs-Eintrag, den man über ein anderes Wort findet, als ein
Wiederbelebungs-Eintrag, den jemand nicht findet.

Bemerkenswert am Rande: „donner" führt zuerst auf `erste-hilfe-blitzschlag`
und erst danach hierher. Genau diese Reihenfolge ist richtig.

## 12.08.2026 — Zielmarken: nicht in eine Richtung gehen, sondern zu einem Ding

Siebter und vorerst letzter Eintrag aus FM 3-25.26, Abschnitt 11-5.
Orientierung steht damit bei 16 Einträgen — von 9 am Morgen.

### Warum dieser Eintrag der praktischste der ganzen Reihe ist

Alle vorigen Einträge sagen, wie man eine Richtung BESTIMMT. Dieser sagt, wie
man sie BEHÄLT — und daran scheitert es in Wirklichkeit. Wer geradeaus zu gehen
versucht, läuft einen Bogen; das ist normal und keine Ungeschicklichkeit.

Der Kern in einem Satz: **Man geht nicht in eine Richtung, sondern zu einem
Ding.** Die Umwege dazwischen sind dann gleichgültig.

### Was aus der Quelle übernommen wurde und anderswo fehlt

**Die Marke wird unterwegs gewählt, NIE auf der Karte.** Die Quelle sagt das
ausdrücklich. Was auf der Karte auffällig aussieht, ist im Gelände oft nicht
wiederzuerkennen.

**Von mehreren die entfernteste und die höchste.** Die entfernteste, weil man
dann länger geht, ohne nachzumessen — weniger Gelegenheiten für Fehler. Die
höchste, weil eine niedrige Marke im Hintergrund verschwindet, sobald man sich
ihr nähert. Bedingung der Quelle: Die Marke muss auf dem ganzen Weg sichtbar
bleiben.

**Wenn vorn nichts taugt: nach hinten peilen.** Eine Marke im Rücken behalten
und gelegentlich zurückschauen, bis vorn wieder etwas auftaucht. Steht selten
in Ratgebern.

**Im Dunkeln braucht die Marke eine auffälligere FORM**, weil Farben
verschwinden und nur Silhouetten bleiben — und weil sich die Silhouette
scheinbar verändert, während man aus einem anderen Winkel auf sie schaut.

**Und der Fehler beim Umgehen, der fast immer gemacht wird:** Nur Schritte in
Richtung Ziel zählen als Fortschritt. Die seitwärts — hin und zurück — sind
kein Weg zum Ziel. Wer sie mitzählt, glaubt weiter zu sein, als er ist.

### Eine Verdrängung, die keine Notfall-Verdrängung war und trotzdem korrigiert wurde

Der Eintrag hieß zuerst „…ohne ständig den Kompass abzulesen". Damit stand er
für „kompass" auf Platz eins — vor `orientierung-behelfskompass`, der
Anleitung, wie man sich einen Kompass BAUT.

Kein Erste-Hilfe-Eintrag, die Regel griff also nicht. Trotzdem geändert, aus
einem einfachen Grund: **Wer in einem Überlebenshandbuch „kompass" tippt, hat
meistens keinen.** Dann ist die Bauanleitung die richtige erste Antwort und
nicht ein Kniff, der einen Kompass voraussetzt. Neuer Titel: „…ohne ständig
nachzumessen". Danach steht der Behelfskompass wieder oben, und der neue
Eintrag ist über „zielmarke", „marke", „peilen" und „geradeaus" zu finden.

**Das ist die Regel eine Stufe weiter gedacht:** Nicht nur „verdränge keinen
Notfall-Tipp", sondern „steht auf Platz eins die Antwort, die der Lage
entspricht, in der jemand das Wort tippt?"

### Nachtrag: das fünfte Blatt

`zielmarken.png` zeigt beides untereinander — oben der Bogen, den man ohne
Marke läuft, unten der Zickzack zwischen drei Marken, der trotzdem ankommt.
Das ist die Aussage des Eintrags in einem Bild: **Die Umwege zwischen den
Marken sind gleichgültig.** Wieder reine Geometrie, also neu gezeichnet.

## 12.08.2026 — FEMA-Leitfaden geprüft, EIN Absatz daraus übernommen

Nach dem Feldhandbuch wurde nach einer neuen Quelle für die Lücke „Wohnung
wird kalt" gesucht. Gefunden und geladen: **„Are You Ready? An In-depth Guide
to Citizen Preparedness"**, US-Katastrophenschutzbehörde FEMA, gesichert als
`work/quellen/taktisch/fema-are-you-ready-is22.txt` samt PDF. Werk der
US-Bundesregierung.

### Das Ergebnis der Prüfung ist überwiegend: NICHT übernehmen

Der Leitfaden ist breit und flach. Sein Winterkapitel sagt zu Unterkühlung,
Erfrierung und Kleidung genau das, was im Paket schon steht — teils
ausführlicher. **Daraus einen neuen Eintrag zu machen hieße, Vorhandenes zu
verdoppeln.** Das ist hier festgehalten, damit niemand dieselbe Quelle noch
einmal für dasselbe Thema durchsieht.

Ebenso geprüft und verworfen: ein eigener Eintrag „ein Zimmer warm halten".
Der Gedanke war gut, aber `taktisch-heizung-faellt-aus` deckt ihn bereits ab,
einschließlich des Satzes „Heize den KLEINSTEN Raum". Was ich zusätzlich
schreiben wollte — Zelt im Zimmer, Dämmung gegen den Fußboden, Fenster
abdichten — steht in KEINER der geprüften Quellen. Es wäre erfunden gewesen,
und erfunden wird nichts.

### Was übernommen wurde: drei Handgriffe und zwei Sätze

`taktisch-heizung-faellt-aus` hat einen Absatz bekommen, den die beiden alten
Quellen nicht hergaben:

* **Nachfüllen nur im Freien**, nie bei laufendem oder noch warmem Gerät.
* **Mindestens 90 Zentimeter Abstand** zu allem Brennbaren. Die Quelle sagt
  drei Fuß; am Seitenbild gegengeprüft. Im Eintrag steht ausdrücklich, dass
  das ein Mindestmaß ist und kein Richtwert.
* **Lüften auch bei zugelassenen Geräten** — die Zulassung sagt, dass das
  Gerät für Innenräume gebaut ist, nicht dass der Raum dicht bleiben darf.

Dazu zwei Sätze zum Brennstoffsparen: die ganze Wohnung kühler halten als
sonst, und einzelne Räume vorübergehend ganz von der Wärme abtrennen.

**Der Eintrag hat damit drei Quellen statt zwei.** Geprüft wurde deshalb
eigens, ob im Text eine Aussage über die ANZAHL der Quellen steht — das war an
diesem Tag schon dreimal die Fehlerquelle. Hier nicht: Der Text nennt sie
einzeln („die deutsche Quelle", „das ältere Militärhandbuch"), der neue Absatz
sagt selbst, woher er stammt.

## 12.08.2026 — Was jemand tippt, und was das Paket versteht

Kein neuer Inhalt, sondern eine Messung — und sie hat mehr gebracht als
mancher Eintrag. Geprüft wurde eine Liste von **69 alltagssprachlichen
Krankheits- und Verletzungswörtern**: also das, was jemand unter Stress
tatsächlich eintippt, nicht das Fachwort.

Ergebnis: **Für acht Wörter gab es den Inhalt, aber nicht das Wort.** Die Suche
fand nichts oder das Falsche, obwohl der richtige Eintrag im Paket steht.

| getipptes Wort | fand vorher | führt jetzt auf |
|---|---|---|
| Fallsucht | nichts | Krampfanfall: nichts festhalten … |
| Holzbock | nichts | Zeckenstich: Zecke sofort entfernen |
| Platzwunde | nichts | Wunde bedecken |
| Furunkel | nichts | Abszess eröffnen |
| Wundbrand | nur „keimfrei, desinfiziert…" | Totes Gewebe erkennen |
| Sonnenstich | nichts | Hitzschlag erkennen |
| Schürfwunde | Offene Brustwunde (!) | Ausspülen statt auswischen |
| Bluterguss | Augenverletzung | Schmerz behandeln ohne Tabletten |

„Schürfwunde" ist der schlimmste Fall: Wer sich das Knie aufschlägt und
„Schürfwunde" tippt, bekam eine Anleitung für eine offene BRUSTwunde. Das ist
kein fehlendes Wort mehr, das ist ein falscher Treffer im Notfall.

Behoben durch Schlagwörter an den vorhandenen Einträgen — **kein Wort wurde in
einen Titel gesetzt**, damit nichts verdrängt wird.

### Eine Einordnung, die dabeistehen muss

**Sonnenstich ist medizinisch nicht dasselbe wie Hitzschlag.** Das Wort führt
trotzdem auf „Hitzschlag erkennen", und zwar bewusst: Wer „Sonnenstich" tippt,
bekam vorher NICHTS. Ein Eintrag, der die gefährliche Form erkennen lehrt, ist
die konservative Antwort. **Ein eigener Eintrag zum Sonnenstich bleibt eine
echte Lücke** und ist unten vermerkt.

### Und ein Rückschlag, gleich wieder zurückgebaut

Das Schlagwort „blauer Fleck" hat prompt einen Treffer verdrängt: „fleck"
führte danach auf die Schmerzbehandlung statt auf
`nahrung-fleisch-schimmel-und-stickigkeit` — den Eintrag, an dem man
verdorbenes Fleisch erkennt. Wort gestrichen, „bluterguss" allein reicht. Das
ist inzwischen die neunte Verdrängung an einem Tag und die erste, die nicht
aus einem TITEL kam, sondern aus einem Schlagwort. **Merksatz: Auch
Schlagwörter verdrängen.**

### Echte Inhaltslücken, die die Messung nebenbei gefunden hat

Für diese Wörter gibt es weder Wort noch Inhalt. Das sind Kandidaten für
spätere Einträge, keine Suchprobleme:

* **Sonnenstich** (eigener Eintrag, siehe oben)
* **Keuchhusten** — bei Säuglingen lebensgefährlich
* **Windpocken und Masern** — in einer Lage mit vielen Menschen auf engem Raum
  durchaus einschlägig
* **Gerstenkorn** und **Hexenschuss** — harmlos, aber häufig, und beide
  beunruhigen ohne Not

Nicht lohnend und deshalb ausdrücklich abgehakt: Schluckauf, Aufstoßen.

## 12.08.2026 — Dieselbe Messung für Wasser, Nahrung und Haushalt

Die Wortprobe auf das nächste Wortfeld angewendet: 64 Alltagswörter aus
Wasser, Vorrat, Feuer und Haushalt. Die Ausbeute ist kleiner als bei den
Krankheitswörtern, aber ein Fund darunter ist der schlechteste des ganzen Tages.

| getipptes Wort | fand vorher | führt jetzt auf |
|---|---|---|
| **Brunnenwasser** | **Hasenpest beim Verarbeiten** | Wasserstelle beurteilen |
| Bachwasser | Wasserrad ausmessen | Wasserstelle beurteilen |
| Pfütze | Gießen im Garten | Wasserstelle beurteilen |
| Notvorrat | Schimmelgetreide/Aflatoxin | Was zehn Tage im Haus sein sollte |
| Hamstern | nichts | Was zehn Tage im Haus sein sollte |
| Abgestanden | nichts | Wasser belüften |
| Madig | nichts | Verdorbenes Fleisch erkennen |
| Gestank | nichts | Abfall im Lager |
| Beeren sammeln | Zeckenstiche vermeiden | Acht Zeichen, bei denen du nicht probierst |
| Wildkräuter | Beerensträucher (Anbau) | Acht Zeichen, bei denen du nicht probierst |

**„Brunnenwasser" führte auf „Hasenpest beim Verarbeiten".** Wer in einer
Krise wissen will, ob das Wasser aus dem Brunnen trinkbar ist, bekam eine
Anleitung zum Zerlegen von Hasen. Das ist noch schlechter als die Schürfwunde
von vorhin, denn Brunnenwasser ist in einem Stromausfall eine der ersten
Fragen überhaupt.

### Zwei Wörter wieder gestrichen

**„schal"** (für abgestandenes Wasser) ist im Deutschen zweideutig — es ist
auch der Schal um den Hals. Der bisherige erste Treffer war zwar Zufall
(„Schallgeschwindigkeit"), aber ein Wort, dessen zwei Bedeutungen in
verschiedene Kapitel führen, macht die Suche nicht besser. „abgestanden" allein
trifft die Sache.

**„wetzstein"** fand zunächst nichts, obwohl es die Anleitungen „Werkzeug
schärfen" und „Am Ölstein schärfen" gibt. Der Grund war eine
Werkzeug-Beschränkung: **Anleitungen haben gar kein Schlagwortfeld**, nur Tipps
haben eines.

Der Griff zum Format wäre hier falsch gewesen. Behoben wurde es im TEXT, und
zwar so, dass es ohnehin besser ist: In beiden Werkzeuglisten heißt der Stein
jetzt „Abziehstein oder Wetzstein" beziehungsweise „Ölstein oder Wetzstein".
Beides sind gebräuchliche deutsche Wörter für dieselbe Sache — der Zusatz
erklärt also nicht nur der Suche, sondern auch dem Leser etwas. „wetzstein"
führt jetzt auf beide Anleitungen.

**Merksatz daraus:** Wenn ein Wort fehlt und das Feld dafür nicht existiert,
gehört das Wort in den Text — nicht ein neues Feld ins Paketformat.

### Was die Messung sonst noch als Lücke gezeigt hat

`schimmel in der wohnung` und `rasieren` finden nichts, und dazu gibt es auch
keinen Inhalt. Schimmel in einer feuchten, ungeheizten Wohnung ist in einem
langen Ausfall ein echtes Thema; Rasieren ist keins.

### Und daraus ein Dauertest: `LaienwoerterTest`

Die beiden Messungen wären in einer Woche wieder hinfällig — ein neuer Eintrag
mit „Wunde" oder „Wasser" im Titel verdrängt die mühsam gesetzten Wörter still
wieder. Deshalb steht das Ergebnis jetzt als Projekt-Test neben
`NotfallSucheTest`:
`core/content/src/jvmTest/kotlin/org/compasszero/content/LaienwoerterTest.kt`.

Er prüft 21 Alltagswörter mit **Tiefe 3, nicht 8**. Begründung steht im Test:
Bei einem Alltagswort reicht es nicht, dass der Eintrag irgendwo unter den
ersten acht auftaucht — wer ihn im Ernstfall nicht auf den ersten Blick sieht,
sieht ihn nicht.

**Gegenprobe gemacht, und das ist der Teil, den man nicht auslassen darf:** Ein
Test, der nicht fehlschlagen kann, ist wertlos. Also wurde probeweise das
Schlagwort „brunnenwasser" wieder entfernt — der Test ist prompt rot geworden,
mit der richtigen Meldung. Danach zurückgesetzt, alles wieder grün.

Im Test steht außerdem, was zu tun ist, wenn er später einmal fehlschlägt: fast
nie die Erwartung ändern, sondern das Wort aus dem TITEL des neuen Eintrags
nehmen.

## 12.08.2026 — Dritte Wortprobe: Lage, Handwerk, Tiere — und ein eigener Fehler

69 weitere Wörter, diesmal aus der Lage („kein Strom", „Sirene", „wohin
gehen"), aus Handwerk und Tierhaltung. Der Anbau- und Handwerksteil ist
erstaunlich gut erschlossen — Huhn, Ziege, Melken, Aussaat, Kompost,
Sauerteig führen alle sauber. Gefunden wurden dafür vier Fehltreffer und, das
ist der unangenehme Teil, **ein Fehler, den ich heute selbst eingebaut habe.**

### Der eigene Fehler: „hund" führte auf einen Marschkniff

Der Eintrag „Entfernungen schätzen" hatte das Schlagwort **„hundertmeter"**.
Weil die Suche Wortanfänge vergleicht, fing es jede Anfrage nach **„hund"** ab
— und stand damit vor `medizin-tollwut`.

Wer von einem Hund gebissen wurde und „hund" tippt, bekam eine Anleitung zum
Abschätzen von Entfernungen. Das Schlagwort ist gestrichen; der Eintrag ist
über „entfernung", „schätzen", „donner" und „schall" weiterhin zu finden.

**Das ist die zehnte Verdrängung des Tages und die erste, die ICH verursacht
und selbst wieder gefunden habe** — und zwar nur, weil die Wortprobe ein
drittes Mal gefahren wurde. Daraus die eigentliche Lehre: Die Probe ist keine
einmalige Aufräumaktion. Sie muss nach jedem Schub neuer Schlagwörter wieder
laufen, weil jede Ergänzung neue Wortanfänge ins Feld bringt.

### Die drei anderen Fehltreffer

| getipptes Wort | fand vorher | führt jetzt auf |
|---|---|---|
| Einbrecher, Plünderer, Überfall | nichts | Gewalt: Anzeichen erkennen |
| alte Menschen, behindert, pflegebedürftig | Roggenanbau bzw. Krampfanfall | Wer auf Hilfsmittel angewiesen ist |
| kein Empfang, kein Netz | nichts bzw. Vogelnetze | Was der Ausfall alles mitnimmt |

„kein Netz" führte auf `nahrung-voegel-fangen` — Netze zum Vogelfang. Nicht
gefährlich, aber ein gutes Bild dafür, wie zufällig eine Wortsuche trifft,
wenn das gemeinte Wort nirgends steht.

### Lücken, die diese Runde gezeigt hat

* **Menschenmenge / Gedränge** — kein Wort, kein Inhalt. Eine Massenpanik ist
  in einer Evakuierung ein reales Risiko.
* **Fenster kaputt / Tür verriegeln** — das Haus notdürftig sichern und dicht
  machen. Passt zu „Gewalt vermeiden statt bekämpfen" und zum Winterausfall.
* **Butter und Käse aus Milch** — „butter machen" führt auf die Bienen,
  „käse machen" auf die Mühle. Es gibt Ziegenmilch und Milchsäuregärung, aber
  nichts zum Ausbuttern oder Dicklegen.
* **Stricken** — nur Flicken vorhanden. Zweitrangig.

`LaienwoerterTest` deckt jetzt 30 Wörter in drei Prüfungen ab; die Zeile für
„hund" trägt den Kommentar, warum es sie gibt.

## 12.08.2026 — Die vierte Wortprobe, und die ist die ernste

Die drei vorigen Proben haben Wörter geprüft. Diese prüft **ganze Sätze** —
das, was jemand tippt, während daneben ein Mensch am Boden liegt. 69
Formulierungen, von „atmet nicht" bis „hängender Mundwinkel".

**Sechzehn davon führten ins Leere oder ins Falsche.** Die drei schlimmsten:

| getippt | führte auf |
|---|---|
| **bekommt keine Luft** | Schafe halten, Seife sieden, Bienen |
| **blut spritzt** | Hühner schlachten |
| **hängender Mundwinkel** | nichts |

Das sind keine Schönheitsfehler. Wer in dieser Lage die Suche benutzt, hat
keine zweite Anfrage frei — er hat nicht einmal Zeit, das Ergebnis zu lesen und
zu merken, dass es nicht passt.

### Behoben, in dieser Reihenfolge nach Gefahr

* **Ersticken:** „keine Luft", „bekommt keine Luft", „würgt", „steckt im
  Hals", „erstickt gleich" → *Ersticken erkennen*
* **Blausucht:** „blau", „blaue Lippen", „blau im Gesicht", „Lippen blau" →
  *Ersticken erkennen*
* **Blutung:** „blut spritzt", „hoher Blutverlust", „spritzendes Blut" →
  *Blutung stillen*
* **Schlaganfall:** „hängender Mundwinkel", „kann nicht sprechen", „schiefer
  Mund" → *Schlaganfall erkennen*
* **Verätzung:** „Putzmittel getrunken", „Reiniger getrunken" → *Verätzung*
* **Allergie:** „Zunge dick", „Zunge geschwollen" → *Allergischer Schock*
* **Krampfanfall:** „schäumt", „verdreht die Augen" → *wann es dringend wird*
* **Kopf:** „Kopf aufgeschlagen", „Kopf angeschlagen" → *Gehirnerschütterung*
* **Pilze:** „Pilz gegessen" → *Vergiftung erkennen*

### Ein Fall, der sich NICHT beheben ließ, und warum

**„wird blau"** führt weiterhin zuerst auf `werkzeug-holzkohle-meiler`. Der
Grund ist nicht ein fehlendes Schlagwort, sondern der Titel jener Anleitung:
**„Den Meiler bauen: aus Holz WIRD Kohle."** Titelwörter wiegen am schwersten,
und „wird" steht dort drin. Dazu kommt „blau" zweimal im Text („wenn der Rauch
dünn und blau wird").

Dagegen hilft kein Schlagwort — dagegen hülfe nur, „wird" in einen Titel zu
setzen, und das wäre genau der Fehler, vor dem der ganze Tag warnt.

Der Erstickungs-Eintrag steht für „wird blau" auf **Platz zwei**, also auf dem
ersten Bildschirm. Und die eindeutigen Formulierungen — „blau", „blaue
Lippen", „blau im Gesicht" — führen sauber auf Platz eins. Der Fall ist
dokumentiert statt gebogen.

### Der Dauertest hat jetzt eine vierte Prüfung

`ganzeSaetzeAusDemErnstfallFuehrenZumRichtigenEintrag`, 18 Sätze. Im Test steht
auch, warum „wird blau" dort NICHT geprüft wird.

### Was die Probe an echten Lücken gezeigt hat

* **Knopfbatterie verschluckt** — „batterie verschluckt" findet nichts, und es
  gibt auch keinen Inhalt. Eine verschluckte Knopfzelle verätzt die
  Speiseröhre binnen Stunden; das gehört in ein solches Handbuch.
* **Fruchtwasser** — findet nichts, obwohl es Geburtseinträge gibt.
* **„sieht nichts mehr"** — führt auf Brot, Kompost, Milchsäuregärung.
  Plötzliche Sehstörung ist ein Schlaganfallzeichen.
* **„herz rast"** — führt nur auf den Stromunfall.
* **„durchgeschwitzt"**, **„kann nicht laufen"**, **„vom Dach gefallen"** —
  jeweils Treffer aus der Landwirtschaft.

Die sind NICHT behoben: Bei ihnen ist unklar, welcher vorhandene Eintrag der
richtige wäre, oder es fehlt der Inhalt ganz. Sie stehen im SNAPSHOT.

## 12.08.2026 — Fünfte Wortprobe: wie Angehörige es schildern

Die vierte Runde prüfte, was der Betroffene tippt. Diese prüft, was **jemand
daneben** tippt — und das ist der häufigste Fall überhaupt. 46 Formulierungen.

**Neunzehn Fehltreffer behoben.** Die schwersten:

| geschildert | führte vorher auf |
|---|---|
| **Fontanelle** | nichts — dabei ist die eingesunkene Fontanelle DAS Austrocknungszeichen beim Säugling |
| **baby trinkt nicht** | nichts |
| **kind fiebert**, **kind schlapp** | nichts |
| **autounfall** | nichts |
| **überfahren** | Hülsenfrüchte |
| **aus dem Wasser gezogen** | Garn machen, Kohlenmeiler |
| **Fruchtwasser** | nichts |
| **redet wirr** | nichts |
| **durchgeschwitzt** | nichts |

Neue Ziele, jeweils der Eintrag, der die Lage als Erstes klärt:

* Säugling und Kind → *Austrocknung erkennen* bzw. *Fieber: ab wann gefährlich*
* „redet wirr", „erkennt mich nicht", „schläft nur noch", „reagiert kaum"
  → *Bewusstsein prüfen*. Das ist der richtige erste Schritt, egal ob dahinter
  Unterzuckerung, Sepsis oder ein Schlaganfall steckt.
* Unfall mit Wucht (Auto, Leiter, Dach, überfahren) → *Eine frische Verletzung
  beurteilen: sechs Fragen*
* „doppelt sehen", „plötzlich blind" → *Schlaganfall erkennen*

### Eine bewusste Entscheidung, die begründet gehört

**„Herzrasen" führte auf *Angst beruhigen*.** Das ist in der Mehrzahl der Fälle
sogar richtig — Herzrasen ist meistens Aufregung. Es führt jetzt trotzdem
zuerst auf *Brustschmerzen: Verdacht auf Herzinfarkt*, und die Panik-Entwarnung
steht auf Platz zwei.

Grund: **Im Zweifel der konservativere Rat.** Wer aus Aufregung sucht, verliert
durch einen Blick auf die Herzinfarktzeichen nichts. Wer wegen eines
Herzinfarkts sucht und zuerst „das ist nur Aufregung" liest, verliert alles.

### Die strukturelle Grenze, die diese Runde sichtbar gemacht hat

Fünf Formulierungen sind **nicht** behoben worden, und zwar mit Absicht:
„will nicht trinken", „isst nichts", „wird immer schlechter", „seit Tagen
krank", „wird nicht besser". Sie bestehen ausschließlich aus Allerweltswörtern,
und die Suche vergleicht Wortanfänge — solche Anfragen landen zwangsläufig bei
dem Eintrag, der diese Wörter am häufigsten enthält (hier: Kompost, Beeren,
Hühner).

**Das ist keine Inhaltslücke, sondern eine Grenze des Verfahrens.** Sie ließe
sich nur durch eine andere Bewertung in der Suche selbst beheben, nicht durch
Schlagwörter. Festgehalten, damit niemand daran weiterschraubt und glaubt,
er habe etwas übersehen.

Dasselbe gilt für „sieht nichts mehr" (drei häufige Wörter) — der
Schlaganfall-Eintrag steht dort auf Platz zwei, „plötzlich blind" und „doppelt
sehen" führen dagegen sauber auf Platz eins.

`LaienwoerterTest` hat jetzt fünf Prüfungen und 68 Formulierungen.

## 12.08.2026 — Butter machen: die erste echte Inhaltslücke aus den Wortproben

Die dritte Wortprobe hatte gezeigt: „butter machen" führte auf die Bienen,
„käse machen" auf die Mühle. Milch gab es im Paket (Ziegenmilch,
Milchsäuregärung), aber nichts darüber, was man daraus macht. Das ist jetzt
zur Hälfte geschlossen — Butter steht, Käse ist vorbereitet.

### Die Quelle

„Farmers' Bulletin No. 241: Butter Making on the Farm", **Edwin H. Webster,
Leiter der Dairy Division im Bureau of Animal Industry**, Dezember 1905.
Volltext gesichert als
`work/quellen/agrikultur/usda-fb0241-1905-butter-making-on-the-farm.txt`.

Zur Rechtslage: Der Verfasser ist namentlich genannt — und genau hier greift
die Reihenfolge, die an diesem Tag schon viermal falsch herum angewendet
worden war. **Zuerst fragen, ob es ein Bundeswerk ist.** Der Begleitbrief des
Hefts weist Webster als Amtsleiter aus, der im Amt schreibt; das Heft ist ein
Werk der US-Bundesregierung. Zusätzlich ist es durch Alter frei (Webster 1871
bis 1928).

### Alle Zahlen am Seitenbild geprüft (Seiten 25 und 26)

* **30 bis 35 Minuten** ist die Zielzeit fürs Buttern. Schneller heißt zu warm,
  langsamer zu kühl.
* **10 bis 13 Grad im Sommer, 16 bis 18 Grad im Winter** — das sind die
  Temperaturen, mit denen man diese Zeit trifft (aus 50/55 °F und 60/65 °F).
* **Abbrechen, wenn die Körner so groß wie Bohnen oder Maiskörner sind** — und
  das ist der Punkt, den fast alle verpassen. Solange die Körner einzeln sind,
  lässt sich Buttermilch herauswaschen und Salz gleichmäßig einbringen.
  Eingeschlossene Buttermilch ist der Grund, warum Butter ranzig wird.
* **Waschwasser: gleiche Menge wie die abgelaufene Buttermilch, gleiche
  Temperatur**, dann vier- bis fünfmal langsam drehen.
* **Salz: rund 28 Gramm auf ein Pfund Butter**, also etwa zwei Prozent (aus
  „1 ounce for each pound").
* **Rahm hält vier bis fünf Tage unter 10 Grad**, wenn er sauber gewonnen ist.
* **Ansatz: Magermilch bei 21 bis 27 Grad**, binnen 24 Stunden feste Gallerte.

### Drei Verdrängungen — und zwei davon waren ernst

Ein einziges neues Kapitel hat drei Einträge von Platz eins geschoben:

| Wort | verdrängte | behoben durch |
|---|---|---|
| **kalt** | `erste-hilfe-unterkuehlung-stadium-eins` (fiel ganz aus den ersten acht) | alle „kalt"-Formen durch „kühl" ersetzt |
| **milch** | `erste-hilfe-flaschenkind-durchfall` | „Milch" aus dem TITEL genommen |
| salzen | `agrikultur-trocknen` | Überschrift „Waschen, salzen, kneten" → „Waschen, Salz einbringen, kneten" |

**Der Kältefall hat mich dreimal in die Irre geführt, und das gehört
aufgeschrieben:**

1. Erst alle „kalt"-Wörter im Fließtext ersetzt — Test blieb rot.
2. Dann „Kälte" gesucht (die Suche behandelt „ä" wie „a", also matcht „kalt"
   auch „Kälte") — auch schon ersetzt, Test blieb rot.
3. Gefunden wurde es erst mit einer Suche über den GESAMTEN Eintrag
   einschließlich der Überschriften: Es stand in der **Abschnitts-Überschrift**
   „Rahm gewinnen und kalt halten". Mein Ersetzen hatte nur `body` angefasst,
   nicht `heading`.

**Merksatz: Beim Jagen eines Wortes IMMER title, heading UND body durchsuchen.
Überschriften wiegen wie Titel.**

### Und noch eine Werkzeuglücke geschlossen

Das Kapitel hatte zuerst die Dringlichkeit `daheim` — ein Wert, den es nicht
gibt. Folge: **Das ganze Paket lud nicht mehr.** `einbauen.py` hatte den Wert
nicht geprüft; es kannte nur die ANZAHL der Dringlichkeitsfelder, nicht die
erlaubten Werte, und das auch nur für Tipps.

Behoben: Das Werkzeug sammelt jetzt alle im Paket vorkommenden Werte ein und
meldet jeden unbekannten — für Tipps, Anleitungen und Kapitel gleichermaßen.
Die Liste wird bewusst NICHT im Werkzeug gedoppelt, sondern aus dem Paket
gelesen; sonst veraltet sie beim nächsten neuen Wert.

Das ist an einem Tag die zweite Sorte Fehler, die das ganze Paket
unbrauchbar machte (die erste war eine Gruppe ohne `category`). Beide fangen
jetzt vor dem Schreiben ab.

### Was als Nächstes ansteht

Der Käse ist vorbereitet: „Farmers' Bulletin No. 166: Cheese Making on the
Farm", Henry E. Alvord, ebenfalls Leiter der Dairy Division, 1903 — Volltext
und PDF liegen schon unter `work/quellen/agrikultur/`.

## 12.08.2026 — Käse machen, und ein Fund im Begleitbrief

Zweite Hälfte der Milch-Lücke. Quelle: „Farmers' Bulletin No. 166: Cheese
Making on the Farm", zusammengestellt von **Henry E. Alvord, Leiter der Dairy
Division**, 1903.

### Der Fund, der wichtiger ist als das Kapitel

Im Begleitbrief des Hefts steht ein Satz, den man leicht überliest: Das
Bulletin sei **„composed principally of descriptions which have been taken
from the writings of well-known authorities"** — also eine ZUSAMMENSTELLUNG
fremder Beschreibungen.

Und am Ende des übernommenen Abschnitts steht die Herkunft ausdrücklich dabei:
**„(From a circular issued from the Minnesota Dairy School by Prof. T. L.
Haecker.)"**

Das ist genau der Fall, vor dem die Rechte-Prüfung dieses Projekts warnt:
**Eine Landwirtschaftshochschule eines Bundesstaates ist NICHT vom
US-Bundeswerk-Grundsatz erfasst.** Die USDA-Zusammenstellung ist ein
Bundeswerk; der zugrunde liegende Text ist es nicht.

**Warum der Eintrag trotzdem geschrieben werden konnte** — und diese
Begründung steht schon länger im SNAPSHOT, hier greift sie zum ersten Mal in
der Praxis: Übernommen sind ausschließlich TATSACHEN, ZAHLEN UND VERFAHREN.
Die sind nicht geschützt. Kein Satz ist übersetzt oder nachgebildet; der Text
ist eigene Leistung. Was dagegen NICHT übernommen wurde, sind die beiden
Abbildungen des Hefts — bei Bildern ist die Lage eine andere.

Im Quellennachweis des Kapitels steht die Herkunftskette ausdrücklich drin,
damit sie nicht verlorengeht.

**Offener Punkt für die Veröffentlichung:** Falls je Abbildungen oder
wörtliche Passagen aus diesem Heft gewünscht werden, muss vorher das Todesjahr
von T. L. Haecker belegt werden. Das ist hier NICHT geschehen und wird auch
nicht behauptet.

### Alle Zahlen an den Seitenbildern 6 und 8 geprüft

* **Lab bei 30 bis 32 Grad** — die Quelle macht daraus eine ausdrückliche
  Warnung (86 bis 90 °F).
* **Gerinnen beginnt nach 10 bis 12 Minuten.**
* **Die Fingerprobe:** Finger schräg hinein, Kerbe mit dem Daumen, langsam
  heben — bricht die Gallerte sauber, ohne Flocken am Finger, ist sie
  schnittreif.
* **Würfel so groß wie kleine Maiskörner**, dann langsam auf 37 bis 38 Grad
  und **etwa 40 Minuten** halten (98–100 °F).
* **Die Handprobe:** Bruch drücken, Hand öffnen — fällt er auseinander, ist er
  fest genug.
* **Pressen etwa 20 Stunden**, **Salzbad 2½ Tage** mit Wenden alle 12 Stunden,
  oder trocken zweimal täglich 3 bis 4 Tage.
* **Reifen bei 13 bis 18 Grad** (55–65 °F), Luft so feucht wie möglich,
  **zwei bis vier Monate**.

**Ein Erkennungsfehler ist dabei aufgefallen und beweist die Regel wieder:**
Die Texterkennung schreibt bei der Färbung „10 gallons", auf dem Seitenbild
steht **16**. Die Zahl wird zwar nicht verwendet (Färbung ist reine Optik und
bewusst weggelassen), aber sie zeigt zum wiederholten Mal, dass Ziffern in der
Erkennung stillschweigend falsch werden.

### Vier Verdrängungen, zwei davon ernst

Das Kapitel hieß zuerst „Käse machen: Bruch schneiden, pressen, reifen".

| Wort | verdrängte |
|---|---|
| **bruch** | `erste-hilfe-bruchheilung-dauer` — im Deutschen ist „Bruch" der Knochenbruch UND der Käsebruch |
| **pressen** | `medizin-haemorrhoiden` (und dahinter der Geburtsablauf) |
| schneiden | `agrikultur-obstbaeume` |
| reifen | `agrikultur-butter` (beides eigene Kapitel, unkritisch) |

Behoben durch einen Titel ohne beide Wörter — „Käse machen: gerinnen lassen,
formen, reifen" — und durch zwei umbenannte Abschnitts-Überschriften. Im
Fließtext dürfen „Bruch" und „pressen" stehen bleiben; dort wiegen sie kaum.

Damit ist an diesem Tag zum zweiten Mal ein Wort aufgefallen, das in zwei
Kapiteln etwas völlig anderes bedeutet — nach „senke" (Fieber senken /
Geländesenke) jetzt „Bruch". **Solche Doppeldeutigkeiten gehören nie in einen
Titel.**

## 12.08.2026 — Die tödlichsten Pflanzen Mitteleuropas fehlten ganz

Die sechste Wortprobe galt Pflanzen-, Pilz- und Tiernamen. Sie hat den
schwersten Inhaltsmangel des ganzen Tages gefunden.

| getippt | fand |
|---|---|
| **Tollkirsche** | „Asthmaspray ist leer" |
| **Fingerhut** | Bienenhaltung, Zuschnitt beim Nähen |
| **Eisenhut** | nichts |
| Maiglöckchen, Herbstzeitlose, Goldregen, Eibe, Pfaffenhütchen, Seidelbast, Aronstab, Bilsenkraut, Stechapfel, Efeu, Buchsbaum, Fliegenpilz | nichts |

**Der Eisenhut ist die giftigste Pflanze Mitteleuropas und steht in vielen
Bauerngärten.** Das Paket kannte ihn nicht.

Das war kein Suchproblem, sondern eine Inhaltslücke — und der vorhandene
Eintrag `nahrung-giftpflanzen-namentlich` sagt sogar selbst, woran es liegt:
Er stützt sich auf ein weltweit gültiges Handbuch, das zur Verbreitung meist
Nordamerika nennt. Für ein Paket namens `europe-de` ist das zu wenig.

### Die Quelle, und ihre Herkunftskette

„Risiko Vergiftungsunfälle bei Kindern", **Bundesinstitut für Risikobewertung
(BfR)**. Lag schon auf der Platte, unter
`work/quellen/medizin-luecken/gift-trinken/` — ein früherer Durchgang hatte sie
für die Gift-Einträge geladen und den Pflanzenteil nicht ausgewertet.

Die eigentliche Liste steht auf Seite 78 und ist ein im Anhang abgedrucktes
**Merkblatt des Giftnotruf Berlin** (Berliner Betrieb für Zentrale
Gesundheitliche Aufgaben). Also dasselbe Muster wie beim Käse-Bulletin: eine
Bundesbehörde druckt das Werk einer LANDESstelle ab. Übernommen sind
ausschließlich die Namen und ihre Einordnung — Tatsachen, keine Formulierungen.

### Am Seitenbild geprüft, und hier war das keine Formsache

Die Texterkennung hatte die Liste **gesperrt gesetzt und zerrissen**:
`B i l s e n k r a u t`, `H erb stz eitlo se`, `S eid elb ast- A rten`. Bei
einer Liste von Giftpflanzennamen ist ein Erkennungsfehler unmittelbar
gefährlich — ein falsch gelesener Name kann eine giftige Pflanze in die
Entwarnungsliste rutschen lassen. Deshalb wurde die ganze Seite gerendert und
Name für Name abgeglichen.

### Was der Eintrag bringt, das anderswo fehlt

**Vier Gruppen statt „giftig oder nicht"**: sehr giftig, giftig, gering
giftig, ungiftig — plus eine fünfte Gruppe quer dazu, die nur die Haut
angreift.

Drei Einzelheiten, die mehr wert sind als die Liste selbst:

* **Die Eibe:** Das rote Fruchtfleisch ist UNGIFTIG. Giftig sind der
  ZERBISSENE Same und die Nadeln; ganz verschluckte Samen gehen unverändert
  ab. Das steht so in der Quelle und räumt eine Angst aus, die an jeder
  Friedhofshecke entsteht.
* **Die rohe Gartenbohne** steht mitten in der Giftliste. Das trifft in einer
  Notlage mehr Menschen als jede Waldpflanze.
* **Die Vogelbeere** steht in der HARMLOSEN Gruppe. Der verbreitete Irrtum,
  sie sei tödlich, ist damit ausgeräumt.

Dazu die Herbstzeitlose als die Verwechslung, die beim Bärlauchsammeln tötet,
und der Handgriff der Quelle: **Wer zum Arzt muss, nimmt einen GANZEN ZWEIG
mit**, nicht nur ein Blatt oder eine Beere.

### Zwei Dinge bewusst NICHT übernommen

**Die Zungenprobe.** Die Quelle schlägt vor, den Saft eines geknickten Blattes
vorsichtig mit der Zunge zu berühren, um hautreizende Pflanzen zu erkennen.
Für bekannte Zimmerpflanzen mag das angehen; an einer unbekannten Pflanze im
Gelände ist es das Falsche, denn genau die tödlichen sehen harmlos aus. Im
Eintrag steht ausdrücklich, dass diese Probe hier nicht empfohlen wird — nach
demselben Muster wie bei Blei, Kreosot, Quecksilber und Oxalsäure heute.

**Jede Behandlung.** Der Eintrag nennt Pflanzen und verweist für alles Weitere
auf „Vergiftung erkennen" und „Gift geschluckt: was trinken, was nicht".

### Verdrängungen

Nur eine musste zurückgenommen werden: Der Eintrag hieß zuerst „…vier STUFEN",
und „stufe" führte danach nicht mehr auf `erste-hilfe-borreliose-behandeln`
(die Krankheitsstadien). Aus Stufen wurden Gruppen, überall.

„fingerhut" führt jetzt zuerst auf die Giftpflanze statt auf den Zuschnitt
beim Nähen — das ist die dritte Doppeldeutigkeit des Tages nach „senke" und
„bruch", und diesmal ist die neue Reihenfolge die richtige: Wer den
Nähfingerhut sucht, kommt mit dem zweiten Treffer aus; wer die Pflanze sucht,
womöglich nicht.

`LaienwoerterTest` prüft jetzt in sechs Prüfungen 85 Formulierungen, davon 17
Pflanzennamen.

## 12.08.2026 — Die Gegenprobe: essbares Grün bei Namen

Nach den Giftpflanzen die andere Hälfte derselben Lücke. Vorher fanden
„Sauerampfer", „Wegerich", „Sauerklee", „Wegwarte", „Taglilie" und
„Kaffeeersatz" NICHTS; „Klette" führte aufs Flussdurchqueren, „Portulak" auf
Beerensträucher, „Ampfer" aufs Pflanzenfärben.

Quelle ist das Handbuch, das im Paket ohnehin schon trägt: FM 3-05.70, Anhang
B „Edible and Medicinal Plants" mit den Einzelbeschreibungen. Übernommen sind
acht Pflanzen mit Aussehen, Standort, essbaren Teilen und den CAUTION-Kästen,
dazu die Namensliste der gemäßigten Zone.

**Dieselbe Ehrlichkeit wie beim alten Giftpflanzen-Eintrag steht auch hier
oben:** Die Quelle sagt nicht durchgehend, welche dieser Pflanzen HIER
wachsen. Die Auswahl ist meine, die Angaben sind ihre. Bei der Brennnessel
nennt sie Nordeuropa ausdrücklich — deshalb steht das dabei.

### Was den Eintrag brauchbar macht

Nicht die Namen, sondern die **Zubereitung**:

* **Brennnessel: 10 bis 15 Minuten kochen zerstört das Brennende.** Am
  Seitenbild geprüft. Das ist die Zahl, die aus einer Pflanze, die man meidet,
  ein Nahrungsmittel macht.
* **Löwenzahn: alle Teile essbar**, und die geröstete, gemahlene Wurzel gibt
  Kaffeeersatz.
* **Rohrkolben: der Wurzelstock ist voller Stärke** — zerstampfen, Stärke
  herauslösen, als Mehl verwenden. Das ist der einzige Sattmacher in der Liste.
* **Hagebutte:** hängt den ganzen Winter am Strauch, ausgezeichnete
  Vitamin-C-Quelle, getrocknet zu Mehl mahlbar.

Und die drei Warnkästen der Quelle stehen als solche gekennzeichnet dabei:
**Klette nicht mit Rhabarber verwechseln** (dessen Blätter sind giftig), **von
der Hagebutte nur das Äußere essen** (die Samen mancher Arten sind stachelig),
**vom Sauerklee nur kleine Mengen** (viel Oxalsäure).

Am Ende steht als Einordnung, welche vier in einer Notlage mehr wert sind als
der Rest: Löwenzahn, Brennnessel, Rohrkolben, Hagebutte.

### Die Reihenfolge steht im Eintrag selbst

Erst „Acht Zeichen, bei denen du gar nicht erst probierst", dann die
Giftpflanzen-Gruppen, dann diese Namen — und im Zweifel der Essbarkeitstest.
**Ein Name allein ist keine Bestimmung**, und das steht so drin.

### Zwei Wörter wurden bewusst zurückgegeben

* **„spitzwegerich"** führte danach hierher statt auf
  `medizin-heilpflanzen-grenzen`. Der Wegerich wird als Wundkraut benutzt, und
  die Warnung vor den Grenzen der Heilpflanzen ist die konservativere erste
  Antwort. Stichwort gestrichen; „wegerich" allein führt hierher.
* **„wildkräuter"** bleibt bei `nahrung-pflanzen-meiden` — erst die
  Ausschlusszeichen, dann die Namensliste.

Beide Entscheidungen stehen jetzt als Prüfzeilen IM Dauertest, mit der
Begründung als Kommentar. Der Test hat damit sieben Prüfungen und 98
Formulierungen.

**Angenommen, nicht behoben:** „distel" führt jetzt hierher statt auf „Zünden
ohne Streichhölzer" (Distelwolle als Zunder). Beide Treffer sind einen Blick
voneinander entfernt, keiner ist ein Notfall — das bleibt so.

## 12.08.2026 — Der Knollenblätterpilz, und warum er ein eigener Eintrag ist

Letzter offener Punkt aus der Pflanzen-Wortprobe. Das Paket streifte Pilze
bisher nur im Eintrag „Wo der Essbarkeitstest versagt". Quelle wieder die
BfR-Broschüre, Abschnitt 4.2 und der Steckbrief auf Seite 61.

### Die eine Tatsache, für die der Eintrag existiert

**Die trügerische Besserung.** Der Ablauf steht so in der Quelle:

1. Nach **etwa 4 bis 12 Stunden** heftiges, anhaltendes Erbrechen und
   Durchfall. Diese lange Pause ist schon die erste Falle — wer nach zwei
   Stunden nichts spürt, hält den Pilz für harmlos.
2. Danach eine **scheinbare Besserung**: Die Magen-Darm-Beschwerden hören auf.
   Es sieht aus, als sei es überstanden.
3. Dann versagen Leber und Nieren. **Nach Tagen** kann der Tod am Zerfall der
   Leber eintreten.

Die Zeitangabe wurde am Seitenbild geprüft (im Original steht „4 bis12" ohne
Leerzeichen — das ist der Satz der Broschüre, kein Erkennungsfehler).

Dazu die Zahl, die man nicht vergisst: **Weniger als ein einziger Pilz kann
ein Kind töten.** Und die Verwechslung, an der es hängt: Er wird häufig für
einen Champignon gehalten.

### Der zweite Zweig war hier Pflicht

Die Quelle endet bei „sofort in die Kinderklinik". Nach der Projektregel
bekommt ein solcher Inhalt einen Zweig für den Fall, dass niemand kommt — und
der musste hier ehrlich ausfallen:

**Es gibt keine Behandlung, die man zu Hause machen kann.** Was hilft, tut
eine Klinik in den ersten Stunden. Ohne sie ist eine schwere Vergiftung oft
tödlich, und daran ändert kein Hausmittel etwas. Sinnvoll bleibt: viel
trinken lassen (Austrocknung tötet schneller als die Leberschädigung) und
NICHT auf die Besserung hereinfallen — wer glaubt, es sei vorbei, hört auf zu
trinken und bricht den Weg zur Hilfe ab.

Das ist unangenehm zu schreiben. Es ist aber die Wahrheit, und ein Handbuch,
das hier Hoffnung erfindet, wäre schlechter als eines, das schweigt.

### Der Projekt-Test hat mich korrigiert, und er hatte recht

Der Eintrag hatte zuerst die Schlagwörter „pilze sammeln" und „pilze essen".
Damit stand er für **„pilze"** auf Platz eins — vor
`erste-hilfe-vergiftung-erkennen`. `NotfallSucheTest` hat das sofort gemeldet.

Die Entscheidung dahinter ist älter als dieser Eintrag und richtig: **Wer
„pilze" tippt, hat womöglich schon gegessen.** Dann ist die
Vergiftungserkennung die erste Antwort, nicht die Sammelkunde. Beide
Schlagwörter gestrichen; der Eintrag ist über „knollenblätterpilz",
„pilzvergiftung", „giftpilz", „champignon" und „pilz sammeln" zu finden.

**Das ist heute das dritte Mal, dass ein Projekt-Test einen Fehler gefunden
hat, den meine eigene Wortprobe nicht auf dem Zettel hatte** (nach „abstand"
und „kalt"). Die Lehre bleibt dieselbe: beide fahren, immer.

`LaienwoerterTest` hat jetzt acht Prüfungen und 104 Formulierungen; die
Pilz-Prüfung hält auch fest, dass „pilze" NICHT hierher führen darf, mit der
Begründung als Kommentar.

## 12.08.2026 — Die Knopfzelle: die Lücke, die vorhin noch unlösbar aussah

Am Nachmittag war die verschluckte Knopfbatterie als Lücke vermerkt worden,
mit dem Zusatz, dass keine gemeinfreie Volltextquelle zu finden sei und der
Weg über die GIZ führen müsse. **Das war voreilig: Die Quelle lag längst auf
der Platte.** Dieselbe BfR-Broschüre, aus der die Giftpflanzen und der
Knollenblätterpilz stammen, hat auf Seite 20 einen eigenen Abschnitt dazu.

**Merksatz daraus: Bevor man draußen sucht, das eigene Quellenverzeichnis
durchsuchen.** `work/quellen/` enthält über siebzig Ordner aus früheren
Durchgängen; was dort für ein Thema geladen wurde, deckt oft auch das Nachbarthema
ab.

### Was übernommen wurde, am Seitenbild geprüft

* **Die Speiseröhre ist die gefährliche Stelle.** Bleibt die Zelle dort
  stecken, muss sie SOFORT entfernt werden — das geht nur in einer Klinik.
* **Beschwerdefreiheit beweist nichts.** Viele Kinder haben keine
  Beschwerden, weil die Zelle rasch in den Magen weiterrutscht. Das sagt nur,
  dass sie nicht mehr oben steckt.
* **Im Magen wird es nach etwa 24 Stunden kritisch** — dann können austretende
  ätzende Bestandteile die Magenschleimhaut schädigen.
* **Ein Röntgenbild ist immer nötig.** Das ist der Satz, der den Eintrag
  trägt: Nur so sieht man, wo die Zelle liegt.
* Und der Hinweis, den man leicht übersieht: Zellen landen auch in **Nase und
  Ohr**.

### Die Grenze der Quelle steht IM Eintrag

Die Broschüre beschreibt Quecksilber- und Silberoxidzellen. **Über die großen,
flachen Lithium-Zellen von heute sagt sie nichts** — und deren Wirkung beruht
nicht auf auslaufendem Inhalt, sondern auf dem Strom im feuchten Gewebe.

Weil dieser Eintrag dazu keine belegte Angabe hat, steht dort ausdrücklich der
vorsichtigere Weg: **Jede verschluckte Knopfzelle wird behandelt, als säße sie
in der Speiseröhre, bis das Gegenteil festgestellt ist.** Das ist der Fall,
für den Regel 4 gemacht ist — im Zweifel der konservativere RAT, ohne die
Tatsachenlage zu verbiegen.

Bewusst NICHT übernommen wurde die beruhigende Aussage der Quelle, dass der
Verlauf „in aller Regel harmlos" sei. Sie stimmt für die dort beschriebenen
Zellen; als erster Satz in einem Handbuch würde sie jemanden zum Abwarten
bewegen.

### Zweiter Zweig

Ohne Röntgen und Endoskop ist die Lage weder festzustellen noch zu beheben.
Was bleibt und im Eintrag steht: **nicht zum Erbrechen bringen** (die Zelle
käme wieder durch die Speiseröhre), trinken lassen, den Stuhl beobachten — und
die Zeichen, an denen man erkennt, dass sie noch oben steckt: nicht schlucken
können, Sabbern, Würgen, Schmerz hinter dem Brustbein.

### Und wieder hat ein Projekt-Test korrigiert

Das Schlagwort **„batterie"** hat den Eintrag vor `erste-hilfe-kohlenmonoxid-melder`
geschoben — den Tipp, der sagt, dass man die Batterie des CO-Melders wechseln
muss. `NotfallSucheTest` hat das gemeldet. Schlagwort gestrichen; die
Zusammensetzungen („knopfbatterie", „uhrenbatterie") stören nicht, weil die
Suche Wortanfänge vergleicht.

**Das ist heute das vierte Mal, dass ein Projekt-Test einen Fehler gefunden
hat, den meine eigene Wortprobe nicht auf dem Zettel hatte.**

## 12.08.2026 — Masern, und die eine Maßnahme, die wirklich etwas ändert

Weitere Lücke aus der ersten Wortprobe geschlossen. Und wieder lag die Quelle
schon auf der Platte: **das WHO-Kinderhandbuch** („Pocket book of hospital
care for children", 2. Auflage 2013), im Volltext unter
`work/quellen/medizin-luecken/`. Ein früherer Durchgang hatte es geladen; die
Masern darin waren nie ausgewertet.

Das ist der zweite Beleg an einem Tag für denselben Merksatz: **erst das
eigene Quellenverzeichnis durchsuchen.**

### Warum Masern in dieses Handbuch gehören

Nicht als Kinderkrankheit, sondern als **Lagerkrankheit**. In einer
Notunterkunft mit vielen Menschen auf engem Raum laufen sie schnell durch und
machen viele Tote. Die Quelle nennt sie hochansteckend, mit schweren
Komplikationen und hoher Sterblichkeit.

### Vitamin A — die Angabe, die den Eintrag trägt

Die Quelle sagt es ohne Einschränkung: **an ALLE Kinder mit Masern.** Mengen
nach Alter, am Seitenbild geprüft (Seite 176):

* unter 6 Monaten: 50 000 IE
* 6 bis 11 Monate: 100 000 IE
* 1 bis 5 Jahre: 200 000 IE
* dritte Gabe 2 bis 4 Wochen nach der zweiten, wenn ein Augenzeichen besteht

**Das SCHEMA stand nicht im Masern-Kapitel**, sondern im Arzneimittelanhang auf
Seite 369: **„Once a day for 2 days".** Das war der Punkt, an dem ich nicht
weitergeschrieben, sondern nachgeschlagen habe — aus „gib Vitamin A" und „die
zweite Gabe" hätte man sich das Schema zusammenreimen können, und bei einer
hochdosierten Gabe an Säuglinge wird nichts zusammengereimt. Die Projektregel
verlangt für Dosierungen einen zusätzlichen primären Beleg; hier ist er die
Dosierungstafel derselben Quelle.

**Über 5 Jahre nennt die Quelle an dieser Stelle keine Menge. Es steht auch
keine im Eintrag.**

Und die ehrliche Einordnung dazu, gekennzeichnet als solche: Die Anweisung
stammt aus einem Handbuch für Gegenden mit schlechter Versorgung. Bei gut
ernährten Kindern ist der Nutzen kleiner; nach Wochen einseitiger Ernährung
ist er wieder groß — und für diese Lage steht der Eintrag hier.

### Was sonst übernommen wurde

**Die Erkennung als Dreisatz:** Fieber UND großflächiger fleckiger Ausschlag
UND eines von Husten, Schnupfen, roten Augen.

**Die zehn Notfallzeichen** aus der Quelle, darunter das eine, das man kennen
muss: **die trübe Hornhaut** — sie kündigt die Erblindung an.

**Die Zahl, die eine Behandlung begründet:** Über die Hälfte aller
Lungenentzündungen bei Masern hat eine bakterielle Zweitinfektion. Deshalb
Antibiotika bei Zeichen einer Lungenentzündung.

**Ein ausdrückliches Nicht:** Beim Masern-Krupp keine Kortisonmittel.

**Und die Absonderung:** mindestens vier Tage ab dem Ausschlag, bei
unterernährten oder abwehrgeschwächten Kindern so lange, wie die Krankheit
dauert.

### Zwei Verdrängungen zurückgenommen, zwei angenommen

Zurückgenommen: **„hochansteckend"** und **„husten schnupfen ausschlag"**
hatten `medizin-kranke-absondern` und `medizin-lungenentzuendung-kind` von
Platz eins geschoben. Schlagwörter gestrichen; beide Prüfungen stehen jetzt im
Dauertest.

Angenommen und hier festgehalten, damit es niemand für ein Versehen hält:
**„vitamin"** und **„schnupfen"** führen weiterhin zuerst auf die Masern, weil
beide Wörter im Fließtext des Eintrags stehen und dort oft. Die verdrängten
Einträge (`medizin-vitaminmangel-zeichen`, `medizin-lungenentzuendung-kind`)
stehen auf Platz zwei, also auf demselben Bildschirm. Das ist vertretbar —
anders als bei einem Wort, das gar nicht mehr führt.

## 12.08.2026 — Keuchhusten, und zwei Projekt-Tests, die es besser wussten

Letzter Eintrag aus dem WHO-Kinderhandbuch, Abschnitt 4.7.1. Damit sind
Masern und Keuchhusten beide erledigt.

### Warum der Eintrag existiert: der Säugling, der nicht keucht

Das kennzeichnende Geräusch fehlt bei jungen Säuglingen. **Bei ihnen folgt auf
den Husten eine Atempause oder eine Blaufärbung — oder die Atempause kommt
ganz ohne Husten.** Ein Säugling, der zwischendurch aufhört zu atmen, wird
deshalb oft gar nicht mit Keuchhusten in Verbindung gebracht.

Dazu der Verlauf, der die Erwartung ordnet: erste Woche von einer Erkältung
nicht zu unterscheiden, ab der zweiten die Anfälle — und die halten **drei
Monate und länger** an. Ansteckend ist das Kind **bis zu drei Wochen** nach
Beginn der Anfälle. Verdacht ab **zwei Wochen** starkem Husten.

### Das Verbot, das kaum jemand kennt

Die Quelle verbietet ohne Einschränkung: **keine hustenstillenden Mittel,
keine Beruhigungsmittel, keine Schleimlöser, keine Antiallergiemittel.** Das
ist genau das, wonach die meisten zuerst greifen — und bei einem Kind, das
ohnehin Atempausen hat, ist ein dämpfendes Mittel gefährlich.

### Und die Ehrlichkeit der Quelle zum Antibiotikum

Erythromycin 12,5 mg je Kilogramm viermal täglich über zehn Tage — **„This
does not shorten the illness but reduces the period of infectiousness."** Das
steht so im selben Satz und ist im Eintrag übernommen: Wer ein Antibiotikum
gibt und auf Besserung wartet, wartet vergeblich. Sinnvoll ist es, um die
anderen im Haus zu schützen.

Sauerstoffgabe und Magensonde sind bewusst nicht übernommen — beides setzt
Ausrüstung voraus, die hier niemand hat.

### Zwei Projekt-Tests haben angeschlagen, und beide hatten recht

**`TitelwaechterTest`:** Der erste Titel hieß „…wochenlange Anfälle, und warum
Hustenmittel schaden". Er trug damit das reservierte Wort **„husten"** (in
„Hustenmittel"). Die Nachmessung zeigte, dass er gleich DREI Einträge von
Platz eins geschoben hatte: „husten" gehört zu `erste-hilfe-ersticken-kann-husten`,
„anfälle" zur Epilepsie, „geben" zur Unterzuckerung. Neuer Titel:
**„Keuchhusten: wochenlang, und was dabei schadet."** Alle drei stehen wieder
oben, und alle drei sind jetzt im Dauertest verankert.

**`ZusammenspielTest`** — und der ist der wertvollere: Er meldete, der Eintrag
mache die Seitenlage von der Atmung abhängig, sage aber nicht, was als Atmung
zählt. **Schnappatmung wäre als Atmung durchgegangen.**

Das ist genau die Art Lücke, die man selbst nicht sieht: Ich hatte über den
Fall geschrieben, dass die Atmung aufhört, aber nicht über den Fall, dass sie
nur noch so AUSSIEHT. Behoben mit der Formulierung, die das Paket überall
sonst schon benutzt: „Atmet es nicht oder nicht normal — dazu zählt auch
seltene, langsame Schnappatmung —, dann ist das KEINE Atmung, und die
Seitenlage ist der falsche Schritt."

**Damit haben Projekt-Tests an diesem Tag fünfmal etwas gefunden, das weder
mir noch meiner Wortprobe aufgefallen war.** Die Reihenfolge, die sich bewährt
hat: eigene Wortprobe für die neuen Wörter, Projekt-Tests für alles andere,
und beides vor dem Commit.

## 12.08.2026 — Typhus: die Krankheit, die das Paket falsch beantwortet hat

Die dritte Wortprobe hatte gezeigt: **„typhus" führte auf
`medizin-blinddarm-verwechslung`.** Das ist nicht nur unpassend, sondern
irreführend — Typhus macht Bauchschmerzen, und wer den Blinddarm-Eintrag liest,
bekommt zwar eine Liste von Verwechslungen, aber keine Krankheit, die aus dem
Wasser kommt.

Und aus dem Wasser kommt sie: In einer Lage, in der die Abwasserentsorgung
ausfällt, ist Typhus eine der ersten Krankheiten, die auftauchen. Das Paket
hatte dazu nichts.

### Was den Eintrag brauchbar macht

**Das Schwierige ist, dass er anfangs wie irgendein Fieber aussieht** — kein
Husten, kein Ausschlag, oft nicht einmal Durchfall. Die Quelle beschreibt
deshalb nicht ein Leitsymptom, sondern ein Muster: schwer krank ohne
erkennbaren Grund, dazu druckempfindlicher oder aufgetriebener Bauch,
vergrößerte Leber und Milz, und Zeichen, dass der ganze Körper mitmacht
(Verwirrtheit, Schläfrigkeit, erbricht alles).

Drei Einzelheiten, die im Eintrag als solche gekennzeichnet sind:

* **Die rosafarbenen Flecken auf der Bauchdecke** nennt die Quelle
  ausdrücklich nur für helle Haut. Auf dunkler Haut fällt dieses Zeichen weg —
  das steht so im Eintrag, damit niemand es vermisst und daraus schließt.
* **Ein steifer Nacken spricht NICHT dagegen.** Die Quelle vermerkt eigens,
  dass Kinder mit Typhus gelegentlich einen steifen Nacken haben.
* **Bei Säuglingen sieht er umgekehrt aus:** nicht hohes Fieber, sondern
  Schock und UNTERtemperatur.

**Die Wendung, die tötet, und sie ist erkennbar:** Durchbruch des Darms mit
Blutung und Bauchfellentzündung — plötzliche starke Bauchschmerzen, Erbrechen,
sehr schmerzhafter Bauch bei Berührung, auffällige Blässe, Schock. Ohne
Operation endet das fast immer tödlich, und genau deshalb steht es hier: Wer
die Zeichen kennt, versucht den weiten Weg, statt zu warten.

### Antibiotikum

Ciprofloxacin 15 mg je Kilogramm zweimal täglich über 7 bis 10 Tage, am
Seitenbild geprüft, dazu der Prüfpunkt der Quelle: **bessert sich nichts
innerhalb von 48 Stunden, ist der Erreger vermutlich unempfindlich.** Die
Zweitlinien-Angaben mit Infusion sind nicht übernommen.

### Der dritte Zweig, den dieses Handbuch braucht

Bei „wenn niemand kommt" steht hier etwas, das bei den anderen Krankheiten
nicht steht: **Die Quelle des Wassers ändern.** Solange alle aus derselben
Quelle trinken, folgen die nächsten Fälle — das ist der einzige Schritt, der
die LAGE wendet und nicht nur den einen Kranken betrifft.

### Und wieder der Titelwächter

Der erste Titel hieß „Typhus: schleichendes Fieber, und die Wendung, die
tötet" und trug damit das reservierte Wort **„fieber"**. `NotfallSucheTest`
und `TitelwaechterTest` haben gleichzeitig angeschlagen. Neuer Titel ohne das
Wort; der Fieber-Eintrag steht wieder oben und ist im Dauertest verankert.

Damit ist an diesem Tag ZUM SECHSTEN MAL ein Projekt-Test eingesprungen. Das
Muster ist inzwischen eindeutig: **Neue Einträge greifen fast immer nach einem
Wort, das schon jemandem gehört.**

## 12.08.2026 — Hirnhautentzündung, und ein Fund über meine eigene Arbeit

Das Paket kannte die Hirnhautentzündung nur als Nebensatz in anderen Einträgen.
„hirnhautentzündung" führte auf `erste-hilfe-fsme` (die Zecken-Entzündung),
„meningitis" auf den Kopfschmerz-Eintrag. Beides falsch für die Krankheit, bei
der die Zeit zwischen „irgendwas stimmt nicht" und „zu spät" am kürzesten ist.

### Was den Eintrag trägt

Die Quelle stellt einen Satz voran, der auch im Eintrag oben steht: **Kein
einzelnes Zeichen reicht.** Es ist die Kombination — Fieber und Krampfanfälle
zusammen mit Nackenzeichen und verändertem Bewusstsein.

**Das eine Zeichen, das man ohne Gerät prüfen kann**, ist der Ausschlag, der
auf Druck NICHT verblasst. Die Quelle nennt ihn „non-blanching". Wie man das
prüft — ein Glas draufdrücken und hindurchschauen — steht im Eintrag als
Einordnung gekennzeichnet, weil die Quelle das Verfahren nicht beschreibt,
sondern nur die Eigenschaft.

Dazu die Zeichen des steigenden Hirndrucks (ungleiche Pupillen, überstreckte
Haltung, Lähmung, unregelmäßige Atmung) und die zwei Fallen:
**Ein fehlender Ausschlag beweist nichts**, und **bei Säuglingen fehlt der
steife Nacken oft** — dort zählen die vorgewölbte Fontanelle, die Reizbarkeit
und die Trinkverweigerung.

Nicht übernommen: Lumbalpunktion, die Dosierungen der Klinikantibiotika und
die Kortisonfrage. Nichts davon ist ohne Klinik anwendbar, und der Eintrag sagt
das auch so — inklusive des Satzes, dass es kein Hausmittel gibt und wer
etwas anderes verspricht, Zeit kostet.

### Der Fund, der wichtiger ist als der Eintrag

Bei der Verdrängungsprobe fiel auf: **„flecken" führte auf den
Keuchhusten-Eintrag** — nicht auf das Schimmelfleisch, dem es gehört.

Ursache war mein eigenes Schlagwort **„rote flecken im auge"** von vor zwei
Einträgen. Ich hatte es beim Keuchhusten eingebaut und in der Probe dazu nicht
nach „flecken" gesucht. **Also hat ein Eintrag von heute Nachmittag einen
Treffer still weggenommen, und ich habe es erst zwei Einträge später bemerkt.**

Das ist genau der Fall, für den im SNAPSHOT steht, dass die Wortprobe nach
JEDEM Schub zu wiederholen ist — und der Beweis, dass die Regel nicht reicht,
wenn man dabei nur die neuen Wörter prüft. **Wer ein Schlagwort mit einem
zusammengesetzten Allerweltswort setzt („rote flecken im auge"), muss auch das
EINZELWORT nachmessen.**

Behoben: Das Schlagwort heißt jetzt „geplatzte Äderchen im Auge" — dieselbe
Sache, ohne das Wort „flecken".

### Und eine Entscheidung, die ich heute zweimal anders getroffen habe

„flecken" gehörte am Nachmittag dem Schimmelfleisch-Eintrag, und ich hatte
dafür sogar ein Schlagwort („blauer Fleck") wieder gestrichen. Jetzt führt es
auf die Hirnhautentzündung, und das ist Absicht: **Der nicht wegdrückbare
Fleck ist die lebensbedrohliche Bedeutung des Wortes.** Der
Schimmelfleisch-Eintrag steht auf Platz zwei, also auf demselben Bildschirm.

Beide Zeilen stehen jetzt im Dauertest, mit der ganzen Geschichte als
Kommentar — damit der nächste Durchgang nicht wieder von vorn abwägt.

## 13.08.2026 — Schwer unterernährt: was in den ersten zwei Tagen tötet

Der vorhandene Eintrag „Nach langem Hunger: langsam anfangen" endet mit einem
ehrlichen Absatz darüber, was seine Quelle NICHT hergibt. Genau diese Lücke
füllt das WHO-Kinderhandbuch, Kapitel 7.

### Die Umkehrung, die den Eintrag rechtfertigt

Die Quelle ordnet die Behandlung in zehn Schritte — **und die ersten beiden
sind nicht Essen, sondern Unterzuckerung und Unterkühlung.** In dieser
Reihenfolge, vor allem anderen. Das ist der Punkt, den der alte Eintrag nicht
hatte: Was in den ersten zwei Tagen tötet, ist etwas anderes als der Hunger.

### Was übernommen wurde, alles ohne Klinik anwendbar

* **Kann man den Blutzucker nicht messen, wird angenommen, dass er zu niedrig
  ist** — und behandelt. Das ist die Regel für den Fall ohne Gerät, und sie
  steht so in der Quelle.
* **Ein gehäufter Teelöffel Zucker auf drei Esslöffel Wasser**, davon 50 ml.
  Am Seitenbild geprüft. Ein Rezept, das jeder machen kann.
* **Für das nicht ansprechbare Kind ohne Infusion:** einen Teelöffel Zucker
  mit ein, zwei Tropfen Wasser anfeuchten und UNTER DIE ZUNGE legen, alle
  20 Minuten wiederholen — mit dem Hinweis der Quelle, auf frühes Schlucken zu
  achten und dann erneut zu geben.
* **Alle zwei Stunden füttern, Tag und Nacht.** Das ist die Maßnahme, die
  alles trägt, und sie kostet nur jemanden, der wach bleibt.
* **Unterkühlung: unter 35 Grad unter der Achsel — oder das Thermometer zeigt
  gar nichts mehr an.** Der zweite Fall ist der wichtigere: Der Wert liegt
  unter dem Messbereich, und genau dann hält man es leicht für „kein Wert".
* Wiedererwärmen über Haut an Haut auf der Brust der Mutter, Kopf bedecken,
  Zugluft fern, nasse Sachen sofort wechseln, ein sehr krankes Kind nicht
  baden.
* **KEINE WÄRMFLASCHE.** Auf der papierdünnen Haut eines ausgezehrten Kindes
  verbrennt sie, ohne dass es sich wehrt.
* **Austrocknung wird bei diesen Kindern zu oft und zu schwer eingeschätzt** —
  die gewohnten Zeichen täuschen. Langsam auffüllen, über den Mund;
  **zu viel Flüssigkeit ist hier sehr gefährlich und kann zum Herzversagen
  führen.**
* **Kein Eisen in den ersten Tagen**, obwohl Blutarmut häufig ist: Es
  verschlimmert Infektionen. Erst wenn der Appetit zurück ist.

Zur Trinklösung steht der Vorbehalt der Quelle dabei: Die übliche Mischung hat
für schwer unterernährte Kinder zu viel Salz und zu wenig Kalium; ohne die
Spezialmischung nennt sie als Ausweg die übliche in HALBER Stärke.

### Und wieder die „kalt"-Falle, zum zweiten Mal an zwei Tagen

Das Schlagwort **„zu kalt und mager"** hat `erste-hilfe-unterkuehlung-stadium-eins`
aus den ersten acht Treffern für „kalt" geworfen — `NotfallSucheTest` hat es
gemeldet. Genau derselbe Fehler wie gestern beim Butter-Kapitel, nur diesmal
über ein Schlagwort statt über eine Überschrift.

**Das Wort „kalt" ist in diesem Paket faktisch vergeben.** Wer es in Titel,
Überschrift oder Schlagwort schreibt, nimmt es dem Unterkühlungs-Eintrag weg.
Im Fließtext ist es unproblematisch — dieser Eintrag kommt sogar ganz ohne
aus.

Ebenfalls zurückgegeben: **„wärmflasche"** gehört weiter zu
`erste-hilfe-erfrierungen-versorgen`. Beide Prüfungen stehen jetzt im
Dauertest.

## 17.08.2026 — Sonnenstich: die falsche Weiche, die im SNAPSHOT stand

Im SNAPSHOT stand seit der ersten Wortprobe: „Sonnenstich (eigener Eintrag;
die Wortweiche zeigt derzeit auf ‚Hitzschlag erkennen', medizinisch ist es
NICHT dasselbe)". Das war richtig erkannt und lange offen, weil eine deutsche
Quelle fehlte.

**Sie lag längst auf der Platte** — zum dritten Mal derselbe Fall.
`work/quellen/medizin-luecken/kinder-hitze/` enthält zwei Artikel des Magazins
„KinderKinder" der Deutschen Gesetzlichen Unfallversicherung, Ausgabe 02/2024.
Ein früherer Durchgang hatte sie für die Hitze-Einträge geladen; der Teil zum
Sonnenstich war nie ausgewertet.

### Warum der Eintrag nötig war

Der Unterschied ist keine Feinheit, sondern das ganze Krankheitsbild:

**Beim Sonnenstich ist der KÖRPER NICHT ÜBERHITZT.** Hochroter, heißer Kopf —
aber die Haut am Rumpf ist kühl oder normal warm und die Körpertemperatur
normal. Die Sonne hat auf Kopf und Nacken gebrannt, und nur dort sitzt der
Schaden.

Daraus folgt eine andere Behandlung als beim Hitzschlag: **Kopf ERHÖHT
lagern** (nicht flach, anders als bei den meisten Notfällen), **Kopf UND
Nacken** mit **lauwarmen** feuchten Tüchern kühlen — und **ausdrücklich keine
Kühlpads aus dem Eisfach**.

Die Merkhilfe am Ende des Eintrags fasst alle drei zusammen: **Blass und nass
heißt Erschöpfung. Rot und trocken am ganzen Körper heißt Hitzschlag. Roter
Kopf bei kühlem Körper heißt Sonnenstich.**

### Die Abgrenzung, die nicht in der Quelle steht

Die Quelle nennt beim Sonnenstich einen **steifen Nacken**. Zusammen mit
Kopfschmerz, Erbrechen und Verwirrtheit ist das dieselbe Zeichengruppe wie bei
der Hirnhautentzündung, die gestern dazugekommen ist.

Der Eintrag stellt beides nebeneinander und benennt den Unterschied — Fieber
ja oder nein — und sagt dann klar: **im Zweifel den schwereren Fall annehmen**
und nach `medizin-hirnhautentzuendung` verfahren. Als Einordnung
gekennzeichnet, weil die Quelle die Verwechslung nicht behandelt.

Das ist ein Nebeneffekt davon, zwei Einträge kurz nacheinander zu schreiben:
Die Verwechslung wäre keinem von beiden allein aufgefallen.

### Probe

Vorher-Nachher wie immer. Genau drei Änderungen, alle gewollt: „sonnenstich"
führt nicht mehr auf den Hitzschlag, „kühlpad" und „ohrensausen" sind neu
auffindbar. **„kopf", „nacken", „hitzschlag", „kopfschmerzen" und „krämpfe"
blieben unverändert** — bei einem Eintrag, der so viele Allerweltswörter
enthält, war das nicht selbstverständlich.

Im Dauertest verankert sind beide Richtungen: dass „sonnenstich" hierher
führt, und dass „hitzschlag" und „steifer nacken" NICHT hierher wandern.

## 17.08.2026 — Siebte Wortprobe: alte und volkstümliche Namen

Geprüft wurden 60 Ausdrücke, die ein älterer Mensch oder jemand ohne
medizinisches Vokabular benutzen würde. **Das Ergebnis spricht für das Paket:
Die meisten trugen auf Anhieb** — „aderpresse" fand das Abbinden, „fallsucht"
den Krampfanfall, „schwindsucht" die Tuberkulose, „wundstarrkrampf" den
Starrkrampf, „brechdurchfall" die Austrocknung.

**Fünf fanden gar nichts, obwohl der Inhalt da war:**

| getippt | fand vorher | führt jetzt auf |
|---|---|---|
| schlagfluss | nichts | Schlaganfall erkennen |
| herzkasper | nichts | Brustschmerzen: Verdacht auf Herzinfarkt |
| wassersucht | nichts | Herzschwäche: die Waage ist das Messgerät |
| schwindelanfall | nichts | Ohnmacht |
| **zuckerkrankheit** | **Abszess eröffnen** | Überzuckerung |

Der letzte ist der schlimmste und war kein Fehlen, sondern eine falsche
Weiche: „zuckerkrankheit" führte auf eine Anleitung zum Öffnen eines
Abszesses. Das Wort steht dort, weil Zuckerkranke schlechter heilen — aber wer
es tippt, sucht nicht das Skalpell.

Behoben wurde ausschließlich über Schlagwörter. **Kein neuer Inhalt, nur die
Weiche** — der Text stand jeweils schon da.

### Wer diese Wörter benutzt

Das ist der eigentliche Grund, warum die Runde sich gelohnt hat: „Schlagfluss",
„Herzkasper" und „Wassersucht" sagt heute vor allem, wer alt ist. Und wer alt
ist, ist im Ernstfall häufiger allein und häufiger derjenige, den es trifft.

### Was die Probe an echten Lücken bestätigt hat

Ohne Treffer und ohne vorhandenen Inhalt blieben: **Hexenschuss/Ischias**,
**Menschenmenge/Gedränge**, **Fenster kaputt / Tür aufbrechen / Plünderung**.
Alle drei standen schon als Lücke im SNAPSHOT; die Probe bestätigt sie und
findet keine weiteren. Für keinen der drei liegt eine Quelle auf der Platte —
das ist der nächste Schritt, nicht ein Formulierungsproblem.

## 17.08.2026 — Die Wiederbelebung beim Kind gegen den Volltext geprüft

Kein neuer Eintrag, sondern eine **Nachprüfung des wichtigsten vorhandenen**.

`erste-hilfe-wiederbelebung-kind` stützte sich auf „Reanimation 2025 —
Leitlinien kompakt" des Deutschen Rats für Wiederbelebung, also auf eine
ZUSAMMENFASSUNG. Regel 4 verlangt den Volltext, und der lag inzwischen auf der
Platte: die ERC-Leitlinie 2025 „Paediatric Life Support", 392.000 Zeichen,
unter `work/quellen/medizin-luecken/kinder-blutung/`.

**Ergebnis: deckungsgleich.** Geprüft wurden die vier Angaben, an denen im
Ernstfall alles hängt:

| im Eintrag | in der Leitlinie |
|---|---|
| zuerst fünf Beatmungen | „starting with five rescue breaths" |
| 30:2 ohne Kinderausbildung | „30:2 … for rescuers not specifically trained in PBLS" |
| 15:2 mit Kinderausbildung | „15:2 if you are specifically trained in PBLS" |
| etwa eine Sekunde je Beatmung, bis der Brustkorb sich hebt | „for about 1 s, sufficient to make the chest visibly rise" |

Die Quellenangabe des Eintrags nennt das jetzt: geprüft gegen den Volltext,
mit Datum. **Der Unterschied zwischen „steht in der Kurzfassung" und „steht in
der Leitlinie" ist genau der, den Regel 4 meint** — hier ging er gut aus, aber
das wusste vorher niemand.

Die Leitlinie nennt auch den Grund für die fünf Beatmungen, und er ist
ernüchternd: In Übungen schaffen nur 50 bis 72 Prozent der wenig geübten
Helfer zwei brauchbare Beatmungen von fünf Versuchen. Die Fünf ist also kein
Sollwert, sondern ein Ausgleich für die Fehlversuche.

**Ebenfalls geprüft und vorhanden:** das ausdrückliche Verbot, blind im Mund
auszuwischen (`Do not perform a blind finger sweep`) steht in allen drei
Ersticken-Einträgen des Pakets.

## 17.08.2026 — Anaphylaxie: eine Lücke zwischen zwei Altersstufen

Beim Abgleich gegen den ERC-Volltext gefunden, und das ist der bisher
handfesteste Fund dieser Art.

**Der Eintrag nannte:** 0,15 mg für 1–5 Jahre, 0,3 mg für 6–12 Jahre, 0,5 mg
„für Erwachsene".

**Die Leitlinie sagt:** 0,15 mg bei 1–5 Jahren, 0,3 mg bei 6–12 Jahren, **0,5 mg
bei älter als 12 Jahren.**

Dazwischen klaffte die Spanne **dreizehn bis siebzehn**. Wer einen
Fünfzehnjährigen vor sich hat und im Eintrag nur „Kinder bis zwölf" und
„Erwachsene" liest, greift zur Kinderdosis — und gibt bei einer Anaphylaxie zu
wenig. Der Eintrag sagt das jetzt ausdrücklich, samt der Begründung, warum die
Grenze wichtig ist.

**Zweiter Fund, dieselbe Stelle:** Der Eintrag sprach von „einer zweiten
Dosis". Die Leitlinie sagt „**repeat this dose every 5 min if symptoms
persist**" — also wiederholen, solange es nötig ist, nicht einmal nachlegen.
Das ist der Grund, warum sie außerdem verlangt, **zwei** Autoinjektoren mitzuführen;
auch das steht jetzt im Eintrag.

**Dritter, kleinerer:** Zur Lagerung stand nur „sitzen oder liegen bleiben".
Die Leitlinie unterscheidet: **flach bei Kreislaufeinbruch, sitzend wenn die
Atmung das Problem ist.** Beides ist jetzt genannt.

### Warum das hier steht und nicht nur im Eintrag

Alle drei Punkte stammen aus dem Unterschied zwischen der **Kurzfassung** des
Deutschen Rats für Wiederbelebung, auf die sich der Eintrag stützte, und dem
**Volltext** der ERC-Leitlinie. Die Kurzfassung ist nicht falsch — sie ist
kürzer, und genau in dieser Kürze verschwand die Altersgrenze.

Das ist der zweite Beleg an einem Abend für dieselbe Sache: Beim
Wiederbelebungs-Eintrag ging der Abgleich gut aus, hier nicht. **Der Volltext
ist keine Formalie.** Beide Einträge nennen jetzt den ERC-Volltext als Beleg,
mit Datum.

## 17.08.2026 — Was der Volltextabgleich sonst noch bestätigt hat

Die ERC-Leitlinie 2025 enthält eine **Änderungstabelle gegenüber 2021**. Sie
ist der schnellste Weg zu prüfen, ob ein Paket veraltet ist — dort steht
genau, was sich geändert hat.

Zwei Änderungen betreffen Inhalte des Pakets, und **beide sind bereits
richtig**:

* **Brustkorbstöße beim Säugling** (Ersticken): 2021 mit ZWEI FINGERN, seit
  2025 mit der **Zwei-Daumen-Technik**. Der Eintrag „Ersticken beim Säugling"
  sagt „mit beiden Daumen auf das Brustbein" — aktuell.
* **Herzdruckmassage beim Säugling**: dieselbe Umstellung auf zwei Daumen für
  alle Fälle. Der Eintrag sagt es ebenso, samt der Ausnahme für den Fall, dass
  es damit nicht kräftig genug gelingt.

Ebenfalls geprüft und deckungsgleich: Drucktiefe **mindestens ein Drittel des
Brustkorbdurchmessers**, bei Jugendlichen die Erwachsenenangabe 5–6 cm, und
**in keinem Alter mehr als 6 cm**; Frequenz 100–120 in der Minute.

Die Leitlinie nennt auch den Grund für die Drittel-Regel statt fester
Zentimeterwerte: Bei Kindern wird **regelmäßig zu flach gedrückt**, und feste
Zentimeterangaben führen in Übungen zu schlechteren Ergebnissen als das
Drittel.

**Bilanz des Abends:** drei lebenswichtige Einträge gegen den Volltext
geprüft, zwei bestätigt, einer korrigiert (die Altersgrenze bei der
Anaphylaxie). Das Verhältnis ist beruhigend — aber der eine Fund hätte im
Ernstfall gezählt.

## 17.08.2026 — Gedränge: die Lücke, für die es keine Quelle auf der Platte gab

Die letzte der drei Lücken, die die Wortproben immer wieder bestätigt haben
(„menschenmenge", „gedränge" — beide ohne jeden Treffer). Hier lag nichts
vorbereitet, die Quelle musste geholt werden.

### Warum nicht die erstbeste

Eine Suche liefert zu diesem Thema fast nur Nachrichtenartikel und
Ratgeberseiten. Die sind nach Regel 4 unbrauchbar — auch dann, wenn sie
Richtiges sagen. Genommen wurde stattdessen der Grundlagentext des Fachs:
**John J. Fruin, „The Causes and Prevention of Crowd Disasters", 1993**,
vorgetragen auf der ersten internationalen Tagung zur Sicherheit von
Menschenmengen. Er liegt jetzt als PDF UND als ausgelesener Volltext unter
`work/quellen/menschenmenge/` und wurde selbst gelesen.

### Der Satz, der den Eintrag rechtfertigt

**„So gut wie alle Toten in einer Menschenmenge sterben an Druck auf den
Brustkorb — nicht am Niedertrampeln, von dem in den Nachrichten die Rede
ist."**

Das ist kein Wortstreit, sondern der Unterschied zwischen richtigem und
falschem Verhalten. Wer „Trampeln" fürchtet, achtet auf Füße. Wer Druck
fürchtet, achtet auf seinen Brustkorb und auf die Dichte.

### Die Messwerte, die es greifbar machen

* **Sieben Menschen je Quadratmeter** — dann verhält sich die Menge fast wie
  eine Flüssigkeit, und Druckwellen können Menschen von den Füßen heben und
  **drei Meter weit** versetzen.
* Unter einem **drei Meter hohen** Haufen wirken auf die Untersten **3600 bis
  4000 Newton** auf den Brustkorb.
* **Fünf Menschen** brachten in einem nachgestellten Panikversuch **3430
  Newton** auf. Es braucht also keine Masse, um tödlichen Druck zu erzeugen.
* 1943 starben in einem Londoner Luftschutzeingang **173 Menschen**, nachdem
  auf einer Treppe eine einzige Person gefallen war.

### Was aus der Quelle stammt und was nicht

Fruin schreibt für Veranstalter, nicht für den Einzelnen. **Alle Ratschläge
für die eigene Person sind deshalb als Einordnung gekennzeichnet** — sie
folgen aus seinen Tatsachen, stehen aber nicht bei ihm: früh gehen, an den
Rand, Arme vor den Brustkorb, mitgehen statt dagegen, sich nicht bücken, im
Sturz auf die Seite.

Aus der Quelle stammen dagegen die zwei Verhütungsziele, und die stehen im
Eintrag, weil sie für eine selbst organisierte Ausgabe von Wasser oder Essen
gelten: **zu hohe Dichte vermeiden** und **nichts tun, was alle gleichzeitig
in Bewegung setzt.**

### Zwei Prüfer haben eingegriffen

**`TitelwaechterTest`** meldete etwas, das ich noch nie gesehen hatte: Der
Eintrag zitierte den Titel „Wiederbelebung Kind: zuerst beatmen" wörtlich und
schleppte damit das reservierte Wort **„beatmen"** in seinen eigenen Text —
womit er bei dieser Anfrage auf Rang 3 stand, obwohl er zum Beatmen nichts zu
sagen hat. **Ein ausgeschriebener Verweis ist also selbst ein Suchwort.**
Behoben, indem der Verweis ohne den fremden Titel formuliert wurde.

**Die Verdrängungsprobe** zeigte, dass der erste Titel („…sondern der Druck")
das Wort **„druck"** dem Blutdruckmessen weggenommen hätte. Wer „druck" tippt,
will häufiger den Blutdruck. Neuer Titel: „Gedränge: was in einer
Menschenmenge wirklich tötet."

**Angenommen:** „menschen" führt jetzt hierher statt auf den Trauer-Eintrag.
Bei einem Wort, das wörtlich das Thema ist, ist das vertretbar; der
Trauer-Eintrag steht auf Platz zwei.

## 17.08.2026 — Fehlanzeige: Haus notdürftig sichern

Die dritte offene Lücke aus den Wortproben („fenster kaputt", „tür
aufbrechen", „plünderung") bleibt offen, und zwar bewusst.

**Auf der Platte liegt nichts dazu.** Geprüft wurden die BBK-Belege
(`work/quellen/buecher/belege/`) — der Erdbeben-Text erwähnt weder Fenster
noch Dach noch Sichern.

**Die Websuche liefert fast ausschließlich Werbeseiten** von Firmen für
Notverglasung und Dachplanen. Solche Seiten sind nach Regel 4 keine Quelle,
auch wenn einzelne Angaben darin stimmen mögen. Eine Bundesbehörden-Fassung
(FEMA oder vergleichbar) war über die Suche nicht auffindbar.

**Also kein Eintrag.** Das Thema ist echt und gehört ins Handbuch — ein
beschädigtes Haus wetterdicht zu bekommen entscheidet darüber, ob man darin
bleiben kann. Aber es wird nicht aus Werbetexten geschrieben.

**Was der nächste Anlauf braucht:** eine Bauaufsichts- oder
Katastrophenschutz-Quelle im Volltext — etwa eine FEMA-Veröffentlichung zu
Notreparaturen oder ein Merkblatt einer Landesbaubehörde. Bis dahin bleibt die
Lücke stehen und ist hier begründet, damit sie niemand aus Zeitdruck mit
Halbwissen füllt.

## 17.08.2026 — Abbinden: zwei Angaben, die über die Wirkung entscheiden

Vierter Volltextabgleich des Abends, wieder gegen die ERC-Leitlinie 2025. Der
Eintrag `erste-hilfe-abbinden` war gut und stimmte in allem, was drinstand —
aber es fehlten vier Angaben, und zwei davon entscheiden, ob eine Abbindung
überhaupt wirkt.

**Erstens: NICHT ÜBER EIN GELENK.** Die Leitlinie sagt „above the injury, but
not over a joint". Der Eintrag sagte bisher nur „zwischen Wunde und Rumpf".
Über Ellenbogen oder Knie liegt die Schlagader geschützt zwischen den
Knochenenden — die Abbindung sitzt dann fest und die Blutung läuft weiter. Das
ist der Unterschied zwischen einer Abbindung, die wirkt, und einer, die nur
weh tut.

**Zweitens: WIE FEST.** Die Leitlinie nennt den Maßstab: „tighten until the
bleeding stops". Der Eintrag nannte keinen. Ohne Maßstab zieht ein Laie an,
bis es aussieht wie im Film — und das ist regelmäßig zu locker.

**Drittens, und es widerspricht der gewohnten Reihenfolge:** „Stopping
significant external bleeding … has priority over starting chest compressions
in an unresponsive child." Blutstillung VOR Herzdruckmassage. Wer drückt,
während das Blut herausläuft, pumpt es schneller heraus.

**Viertens:** „Manual pressure to the brachial or femoral artery might not be
effective." Der Druckpunkt am Oberarm und in der Leiste, den viele im
Erste-Hilfe-Kurs gelernt haben, wirkt möglicherweise nicht. Das steht jetzt
dabei — nicht um ihn zu verbieten, sondern damit sich niemand darauf verlässt
STATT auf direkten Druck oder Abbindung.

### Bilanz der vier Abgleiche

| Eintrag | Ergebnis |
|---|---|
| Wiederbelebung Kind | deckungsgleich |
| Ersticken Säugling / Herzdruckmassage Säugling | deckungsgleich, auch die Änderung 2025 auf zwei Daumen |
| Anaphylaxie | **Lücke**: Altersspanne 13–17 fehlte |
| Abbinden | **zwei Lücken**: Gelenkregel und Festigkeitsmaßstab |

Vier Einträge, zwei mit echten Lücken. Alle vier stützten sich vorher auf
Kurzfassungen. **Das Verhältnis rechtfertigt den Aufwand** — und es sagt
etwas über den Rest des Pakets: Wo eine Kurzfassung die Quelle ist, lohnt der
Volltext.

## 17.08.2026 — Systematisch gezählt: 52 Einträge stehen auf Kurzfassungen

Nachdem zwei von vier Volltextabgleichen echte Lücken ergeben hatten, war die
naheliegende Frage: **wie viele Einträge betrifft das überhaupt?** Gezählt
wurden alle Quellenangaben, die eine Kurzfassung nennen — Poster,
„Leitlinien kompakt", Merkblatt, Faltblatt, Zusammenfassung.

**Ergebnis: 52 Einträge, 64 Quellenangaben.**

| Anzahl | Herausgeber | Art |
|---|---|---|
| 27 | Deutscher Rat für Wiederbelebung / ERC | „Leitlinien kompakt" |
| 15 | Deutscher Rat für Wiederbelebung / ERC | Poster (BLS-Algorithmus) |
| 15 | Deutscher Feuerwehrverband | „Erste-Hilfe kompakt" |
| 2 | Wilderness Medical Society | Zusammenfassung |
| 2 | WSL-Institut für Schnee- und Lawinenforschung | Merkblatt |
| je 1 | Giftnotruf Berlin, Strahlenschutzkommission, BfR | Merkblatt |

### Was das bedeutet — und was nicht

**Es bedeutet nicht, dass 52 Einträge falsch sind.** Zwei der vier geprüften
waren deckungsgleich, und alle vier stimmten in dem, was sie sagten. Die
Lücken lagen jeweils in dem, was die Kurzfassung WEGGELASSEN hatte: eine
Altersgrenze, eine Gelenkregel, ein Festigkeitsmaßstab.

**Es bedeutet, dass hier eine Arbeitsliste liegt.** Bei einer Trefferquote von
zwei aus vier ist bei 52 Einträgen mit einer zweistelligen Zahl weiterer
Auslassungen zu rechnen — jede davon eine Angabe, die im Ernstfall fehlt.

### Der konkrete nächste Schritt

Für den größten Block (42 Einträge, Wiederbelebung und Erste Hilfe beim
Erwachsenen) fehlt der Volltext noch: Auf der Platte liegt nur die
**Kinder**-Leitlinie (`erc-pls-2025`). **Gebraucht wird die ERC-Leitlinie 2025
für Erwachsene (Basic Life Support und First Aid) im Volltext.** Damit ließen
sich 42 Einträge in einem Zug prüfen.

Die 15 Einträge auf „Erste-Hilfe kompakt" des Deutschen Feuerwehrverbands sind
ein eigener Fall: Diese Reihe IST das Original der Empfehlung, keine Kurzform
eines längeren Werks. Dort gibt es nichts nachzuschlagen.

## 17.08.2026 — Die Erwachsenen-Leitlinie geholt, und drei Trugbilder ergänzt

Nachdem 52 Einträge als kurzfassungsgestützt gezählt waren, wurde der fehlende
Volltext beschafft: **„ERC Guidelines 2025 — Adult Basic Life Support",
Resuscitation 215 (2025) 110771**, 32 Seiten, offen zugänglich. Liegt als PDF
und ausgelesener Text unter `work/quellen/wiederbelebung-erwachsene/`.

### Zuerst die Entwarnung

Die größte Änderung 2025 betrifft die REIHENFOLGE: Bisher wurde der
Herzstillstand erkannt (nicht ansprechbar UND nicht normal atmend) und DANN
gerufen. **Seit 2025 wird bei jedem, der nicht reagiert, SOFORT gerufen** — die
Atmung wird geprüft, während man auf die Annahme des Anrufs wartet.

**Das Paket lehrt diese Reihenfolge bereits richtig:** „Bewusstsein prüfen"
verweist auf Notruf UND Atemwege, „Notruf 112" steht vor „Atmung prüfen". Die
Kurzfassung hatte die Änderung also mitgenommen.

### Und dann drei Dinge, die sie nicht mitgenommen hatte

Die Leitlinie nennt sie ausdrücklich, weil an ihnen Herzstillstände übersehen
werden. Alle drei fehlten im Paket:

**HECHELN.** Bisher stand nur die langsame, mühsame Schnappatmung als „nicht
normal" im Paket. Die Leitlinie hat 2025 das schnelle, flache Hecheln
ausdrücklich dazugenommen.

**EIN KURZER KRAMPFANFALL ZU BEGINN.** Zu Beginn eines Herzstillstands kann es
kurz zucken wie bei einem epileptischen Anfall. Wer das für einen Anfall hält,
wartet ab. Die Leitlinie gibt die Regel dagegen: **Hört das Zucken auf, wird
die Atmung geprüft.** Sie nennt auch die Größenordnung des Problems —
Krampfanfälle machen 3 bis 4 Prozent aller Notrufe aus, davon sind 0,6 bis
2,1 Prozent Herzstillstände.

**ZUSAMMENBRUCH BEIM SPORT.** Anstrengung ist ein häufiger Auslöser, und wer
dabei zusammenbricht, atmet danach oft noch **fast regelmäßig** und hat
womöglich **die Augen offen**. Genau deswegen hat die ERC das Hecheln neu in
die Erkennung aufgenommen.

Alle drei stehen jetzt in „Atmung prüfen", mit dem Volltext als Beleg.

### Damit steht die Bilanz bei fünf Abgleichen

Drei deckungsgleich, **drei mit Lücken** (Anaphylaxie, Abbinden, Atmung
prüfen). Der Volltext hat sich jedes Mal gelohnt, an dem er etwas fand — und
die Fundstellen waren nie Fehler, sondern immer Auslassungen.

## 17.08.2026 — Zwei weitere Volltexte auf der Platte, Bilanz nach sieben Abgleichen

Beschafft und ausgelesen, beide offen zugänglich und jetzt lokal:

* **„ERC Guidelines 2025 — Adult Basic Life Support"**, Resuscitation 215
  (2025) 110771, 32 Seiten → `work/quellen/wiederbelebung-erwachsene/`
* **„ERC Guidelines 2025 — First Aid"**, Resuscitation 215 (2025) 110752,
  29 Seiten → `work/quellen/erste-hilfe-leitlinie/`

Zusammen mit der Kinder-Leitlinie liegen damit **alle drei Volltexte** vor,
auf die sich die 52 kurzfassungsgestützten Einträge beziehen.

### Bilanz nach sieben Abgleichen

| Eintrag | Ergebnis |
|---|---|
| Wiederbelebung Kind | deckungsgleich |
| Ersticken / Herzdruckmassage Säugling | deckungsgleich, auch die Änderung 2025 |
| Herzdruckmassage Erwachsene (Zahlen) | deckungsgleich |
| Reihenfolge Notruf vor Atemkontrolle | deckungsgleich (die große Änderung 2025) |
| Überdosis Opioide | deckungsgleich |
| **Anaphylaxie** | **Lücke: Altersspanne 13–17** |
| **Abbinden** | **Lücken: Gelenkregel, Festigkeitsmaßstab** |
| **Atmung prüfen** | **Lücken: Hecheln, Krampf zu Beginn, Sport** |
| **Herzdruckmassage (Unterlage)** | **Lücke: nicht vom Bett umlagern** |

**Vier Einträge hatten Auslassungen, keiner hatte einen Fehler.** Das ist das
Muster: Kurzfassungen sagen nichts Falsches, sie lassen weg — und was sie
weglassen, sind regelmäßig die Grenzfälle und die Maßstäbe.

### Was noch zu prüfen wäre

Die Erste-Hilfe-Leitlinie deckt weitere Paket-Themen ab, die noch nicht
abgeglichen sind: Ersticken (Stufenfolge Husten → Rückenschläge →
Bauchstöße), Asthma, Brustschmerz, Unterzuckerung, Schlaganfall-Erkennung,
Halswirbelsäule, offene Brustwunde, Gehirnerschütterung, Amputat, Ertrinken,
Unterkühlung, Hitze, Schlangenbiss — und die Seitenlage, die 2025 mit dem
Herzstillstand und der Schocklage **in einen einzigen Ablauf zusammengeführt**
wurde. Gerade dieser letzte Punkt lohnt sich, weil das Paket drei getrennte
Einträge dazu hat.

## 17.08.2026 — „Womit anfangen?" — die Reihenfolge der ersten Sekunden

Achter Volltextabgleich, und diesmal fehlte kein Detail, sondern ein ganzer
Eintrag.

Das Paket hat zwei Einträge zum Untersuchen — „Eine frische Verletzung
beurteilen: sechs Fragen" und „Von Kopf bis Fuß untersuchen: die Reihenfolge".
Beide betreffen das GRÜNDLICHE Untersuchen. **Was fehlte, war die Reihenfolge
der ersten Sekunden**: worauf man zuerst schaut, wenn man vor jemandem steht
und zu viel auf einmal sieht.

### Die Änderung 2025, um die es geht

Die Erste-Hilfe-Leitlinie stellt die Erstbeurteilung so auf: **„Pay immediate
attention to safety, responsiveness of the victim, and catastrophic
bleeding."** Die lebensbedrohliche Blutung steht damit **vor dem Atemweg** —
vor dem A des gewohnten ABC.

Der Grund ist Rechenkunst: Eine Schlagader am Bein leert einen Menschen in
wenigen Minuten; ein verlegter Atemweg braucht länger. Das ist keine
Kleinigkeit für den Laien, weil das gelernte „A-B-C" genau die andere
Reihenfolge nahelegt.

Übernommen ist außerdem die Bewusstseinsstaffel der Leitlinie (dort ACVPU):
**wach — verwirrt — reagiert auf Ansprache — reagiert auf Schmerz — reagiert
gar nicht**, mit dem Hinweis, dass NEU aufgetretene Verwirrtheit schon ein
Alarmzeichen ist.

Und die Regel, die den Eintrag zusammenhält: **Es wird nicht weitergegangen,
solange ein Punkt nicht erledigt ist.** Wer bei der Atmung ein Fehlen findet,
prüft den Kreislauf nicht mehr, sondern belebt wieder.

### Zur Suche

Gemessen wurde mit den Formulierungen, die jemand in Panik tippt.
„womit anfangen", „was mache ich zuerst", „wo fange ich an", „erster blick",
„abcde", „erstbeurteilung" führen alle hierher.

**„was zuerst" führt weiterhin woandershin** (auf das Behelfsmesser und die
Blinddarm-Verwechslung) und ließ sich nicht einfangen: Zwei so häufige Wörter
kommen in zu vielen Einträgen vor. Das ist dieselbe strukturelle Grenze, die
im SNAPSHOT schon bei „sieht nichts mehr" steht — und sie wird hier
festgehalten statt mit immer mehr Schlagwörtern erzwungen.

**Zurückgenommen:** Der erste Titel hieß „Die ersten Sekunden…" und nahm dem
Lawinen-Eintrag das Wort „sekunden" weg. Dort zählen sie wirklich.

## 17.08.2026 — Die Seitenlage gegen den Volltext 2025

Neunter Volltextabgleich, und der längst fällige: Die Leitlinie 2025 hat
Herzstillstand, Seitenlage und Schocklage **in EINEN Ablauf zusammengeführt**,
während das Paket drei getrennte Einträge dafür hat
(„Bewusstlos: atmet oder atmet nicht“, „Stabile Seitenlage: wann und wann
nicht“, „Stabile Seitenlage: Handgriffe“).

Quelle: „ERC Guidelines 2025 — First Aid“, Resuscitation 215 (2025) 110752,
Abschnitte „Recovery position“ (Empfehlung, Handgriffe, Begründung), Volltext
unter `work/quellen/erste-hilfe-leitlinie/`.

### Kein Fehler — aber fünf Lücken, und eine davon kostet Minuten

**ERSTENS, DIE WICHTIGSTE: Vor der Herzdruckmassage wird zurück auf den RÜcken
gedreht.** Die Leitlinie sagt es ausdrücklich („repositioned into a supine
position and, if required, CPR initiated“). Das Paket sagte nur „hört die
Atmung auf, beginnt sofort die Herzdruckmassage“ — auf der Seite. Auf der Seite
kann man nicht drücken. Wer das im Schreck versucht, verliert genau die Minute,
auf die es ankommt. Das ist der einzige Punkt des Abgleichs, der eine echte
Gefahr war, und er stand in keiner der beiden Kurzfassungen.

**ZWEITENS: Der nahe Arm darf gestreckt ODER angewinkelt liegen.** Neu 2025,
nach einem Versuch, der keinen Unterschied fand („either position may be
used“). Das Paket schrieb nur die angewinkelte Form vor. Wer nur eine Form
kennt, hält mitten im Ablauf an, weil der Arm nicht so liegt wie im Kurs.

**DRITTENS: Der Griff bei großem Größenunterschied.** Ebenfalls neu 2025:
Ist der Helfer viel kleiner als der Bewusstlose, wird ZUERST das Knie
aufgestellt und erst danach der ferne Arm geholt — dann muss man sich nicht
mehr über den ganzen Körper strecken. Genau daran scheitert es in der Praxis.

**VIERTENS: Griff knapp oberhalb des Knies, der Fuß bleibt am Boden.** Das
Paket sagte „am fernen Oberschenkel“. Der Unterschied ist nicht Wortklauberei:
Am Oberschenkel gegriffen entsteht kein Hebel.

**FÜNFTENS: Hüfte UND Knie im rechten Winkel.** Das Paket nannte nur den
Winkel in der Hüfte. Ohne das angewinkelte Knie kippt die Person zurück —
das ist das „Stabile“ an der stabilen Seitenlage.

Dazu übernommen: Die Lage gilt ausdrücklich für **Erwachsene UND Kinder**, und
die Hand unter der Wange wird so gerichtet, dass das **Gesicht leicht nach
unten** zeigt, damit Flüssigkeit herausläuft.

### Bewusst NICHT übernommen

Die Leitlinie erwähnt, dass in **einer einzelnen Beobachtungsstudie** bei
Opioid-Überdosis die halb sitzende Haltung der Seitenlage vorgezogen wurde.
Das ist zu dünn für eine eigene Anweisung, und die ERC selbst empfiehlt
weiterhin allgemein die Seitenlage. Eine Sonderregel hätte hier nur Zweifel
gestiftet, wo im Ernstfall keine Zeit für Zweifel ist. Festgehalten wird sie
trotzdem — damit der nächste Durchgang nicht denkt, sie sei übersehen worden.

### Was schon stimmte

Die Trauma-Ausnahme, die Schnappatmung-Ausnahme und die Trennung „wer
wiederbelebt wird, gehört nicht in die Seitenlage“ standen bereits richtig da.
Auch die Schocklage („auf den Rücken“) stimmt mit der Leitlinie überein.

### Zur Suche

Gemessen nach der Änderung: „knie aufstellen“, „auf die seite drehen“,
„wie drehe ich jemanden“, „person ist schwerer“, „wange“ führen auf die
Handgriffe. **Verdrängt wurde nichts:** „knie“ führt weiterhin zuerst auf
„Bauchverletzung: Knie anziehen“ (das Wort steht dort im Titel), einen eigenen
Knieverletzungs-Eintrag gibt es nicht.

Abgesichert im Test `dieSeitenlageNenntDenWegZurueckAufDenRuecken`.

## 17.08.2026 — Allein und am Ersticken: die Lücke, die das Paket am meisten anging

Zehnter Volltextabgleich, Thema Ersticken. Die Helfer-Kette des Pakets (husten
lassen → fünf Rückenschläge → fünf Bauchstöße, im Wechsel, kein blindes
Fingersuchen, bei Bewusstlosigkeit Herzdruckmassage) stimmt mit der
Erste-Hilfe-Leitlinie 2025 überein. **Kein Fehler, keine Auslassung.**

Gefunden wurde dafür ein Satz, der wie eine Randbemerkung aussieht und für
dieses Paket der wichtigste des ganzen Kapitels ist:

> „We have not identified any studies on a person being alone when choking and
> therefore lack data on the situation and what's feasible to do.“

Die Leitlinie sagt also ausdrücklich: **Zum Alleinsein beim Ersticken gibt es
keine Daten.** Genau das ist der Normalfall dieser App. Das Paket hatte dazu
keinen einzigen Eintrag — sechs Einträge zum Ersticken, alle setzen einen
Helfer voraus.

### Die Quelle, die es doch gibt

Pavitt MJ u. a., „Choking on a foreign body: a physiological study of the
effectiveness of abdominal thrust manoeuvres to increase thoracic pressure“,
*Thorax* 2017;72(6):576–578, doi 10.1136/thoraxjnl-2016-209540, offen
zugänglich unter CC BY-NC 4.0. Volltext gesichert unter
`work/quellen/ersticken-allein/`.

Vier gesunde Erwachsene, Druck im Speiseröhren- und Magenballon gemessen,
jeweils nach normalem Ausatmen bei geschlossener Stimmritze. Gemessene Werte
(cm H₂O, Speiseröhrendruck):

| Handgriff | Druck |
|---|---|
| **Stoß gegen die Stuhllehne** | **115 ± 27** |
| Bauchstoß nach hinten oben (Heimlich) | 57 ± 17 |
| Bauchstoß waagrecht nach hinten | 53 ± 11 |
| Selbst gegebener Bauchstoß | wie vom Helfer gegeben |
| Willkürlicher Husten (ein Teilnehmer) | 179 |

Drei Befunde daraus sind übernommen:

**ERSTENS: Der Stoß gegen eine Stuhllehne ist der stärkste gemessene
Handgriff** — doppelt so stark wie der Bauchstoß eines Helfers. Ausführung
wörtlich nach der Quelle: hohe Lehne, Oberkante am OBEREN Bauch, oberhalb des
Nabels und unterhalb der Rippen, dann mit Körpergewicht und Schwerkraft fallen
lassen.

**ZWEITENS: Der selbst gegebene Bauchstoß ist so kräftig wie der von einem
Helfer.** Damit fällt das Argument weg, man könne allein nichts ausrichten.
Die Quelle nennt dazu einen zweiten Grund, sofort selbst anzufangen, auch wenn
jemand im Raum ist: Es ist ein unmissverständliches Zeichen — sich an den Hals
zu greifen wird oft für einen Herzanfall gehalten.

**DRITTENS: Die Richtung ist gleichgültig** (53 gegen 57, kein bedeutsamer
Unterschied). Das ist in den vorhandenen Bauchstoß-Eintrag eingebaut worden,
weil es genau den Fall löst, in dem ein kleiner Helfer vor einem großen
Menschen steht: waagrecht ziehen genügt und verletzt seltener.

### Was NICHT übernommen wurde, und warum

Dieselbe Messreihe nennt für **Rückenschläge nur 7 cm H₂O** — gemessen an
einem einzigen Teilnehmer. Das ist **nicht** verwendet worden, und es steht
auch nichts gegen Rückenschläge im Paket. Zwei Gründe: Ein Schlag wirkt als
Stoß, nicht als anhaltender Druck — ein Druckmesswert bildet ihn also gar
nicht ab. Und ein Wert aus einem Menschen kippt keine Leitlinie. Die Leitlinie
2025 behält die Rückenschläge, das Paket auch.

Die Verfasser fordern außerdem, die australische Leitlinie zu ändern. Das ist
ihre Forderung, keine Tatsache, und steht deshalb nirgends im Paket.

### Der Husten steht weiter ganz vorn

Der höchste gemessene Wert der ganzen Reihe war der Husten (179). Der neue
Eintrag sagt deshalb ausdrücklich: Solange Husten geht, wird gehustet — kein
Handgriff ersetzt ihn. Das deckt sich mit der Reihenfolge der Leitlinie und
verhindert, dass jemand wegen der großen Zahl beim Stuhl anfängt, obwohl er
noch husten kann.

### Grenzen, die im Eintrag selbst stehen

Vier Menschen. Keiner stark übergewichtig — die Verfasser sagen selbst, dass
bei viel Bauchfett Kraft verlorengeht und die Stelle schwerer zu finden ist.
Gemessen wurde Druck, nicht Überleben. Und keiner der vier hatte wirklich
etwas im Hals. All das steht im Eintrag, nicht nur hier.

### Zur Suche

Der erste Titel hieß „Allein und keine Luft …“ und nahm dem Eintrag
„Ersticken erkennen“ den ersten Platz bei „luft“ und „keine luft“ weg — also
ausgerechnet bei den Wörtern, die ein HELFER tippt. Titel geändert auf
„Allein und niemand hinter dir …“; danach gemessen: „luft“ und „keine luft“
stehen wieder wie vorher, „ersticken“ führt unverändert zuerst auf die
Helfer-Kette.

Der neue Eintrag ist erster Treffer bei „stuhllehne“, „tischkante“, „allein“,
„bissen“ und „niemand da zum helfen“. Bei „verschluckt“ steht er vor der
Helfer-Kette — das ist so belassen: Beide Treffer gehören zum Thema, und der
Normalfall dieser App ist der Mensch ohne Helfer.

Abgesichert in `derAlleinErstickenTippNenntSeineGrenzen` und
`derAlleinFallBeimErstickenIstFindbarUndVerdraengtNichts` — der zweite prüft
ausdrücklich, dass die Helfer-Kette bei „ersticken“ vorn bleibt.

## 17.08.2026 — Der Rest der Erste-Hilfe-Leitlinie 2025, in einem Durchgang

Elfter bis sechzehnter Abgleich, alle gegen denselben Volltext
(`work/quellen/erste-hilfe-leitlinie/erc-first-aid-2025.txt`).

### Was schon vollständig stimmte — nichts zu tun

| Eintrag | Ergebnis |
|---|---|
| Brustschmerzen / Herzinfarkt | deckungsgleich (150–500 mg Kautablette, Allergie-Ausnahme, Nitro, Dableiben) |
| **Offene Brustwunde** | deckungsgleich, einschließlich des harten „nicht abdecken“ von 2025 |
| **Abgetrennter Körperteil** | deckungsgleich; der offengelegte Widerspruch feucht/trocken stimmt weiterhin |
| **Schlangenbiss** | deckungsgleich, alle fünf Verbote wortgetreu |
| Ersticken (Helfer-Kette) | deckungsgleich, siehe eigener Abschnitt oben |
| Gehirnerschütterung, Ertrinken, Unterkühlung-Vorbeugung | keine Abweichung gefunden |

Das ist bemerkenswert und gehört festgehalten: **Bei sechs von zehn geprüften
Themen gab es nichts nachzutragen.** Die Kurzfassungen sind nicht schlecht —
sie sind knapp.

### Vier Nachträge, die den Unterschied machen

**UNTERZUCKERUNG — zwei Lücken.** Erstens fehlte der Grenzwert: **unter
4,0 mmol/l bzw. 70 mg/dl**, und nach der Gabe wird noch einmal gemessen. Viele
Haushalte mit Diabetes haben ein Messgerät. Zweitens, und wichtiger: Der
Eintrag verwies auf ein „Notfallset“, sagte aber nicht, wie es benutzt wird.
Jetzt steht da, was die Leitlinie sagt — **Glukagon unter die Haut in die
Außenseite des Oberschenkels**, es gibt es auch als Nasenform. Wer nachts eine
Packungsbeilage lesen muss, hat schon verloren. Ebenfalls ergänzt:
Traubenzuckergel wird teils in die Backentasche gelegt, teils geschluckt.

**ASTHMA — die Vorschaltkammer.** Die Leitlinie nennt sie ausdrücklich
(„using a spacer device if one is available“). Im Anfall bringt kaum jemand
Sprühstoß und Einatmen zusammen; wer eine hat und nicht daran denkt,
verschenkt den größeren Teil der Wirkung.

**HITZSCHLAG — der größte Zugewinn dieses Durchgangs.** Der Eintrag hatte das
Eintauchen (1–26 °C, bis unter 39 °C) bereits richtig. Es fehlte alles, was
zum Tragen kommt, wenn man **kein Thermometer für den Körperkern** hat — also
in praktisch jedem Fall:

* **„Kühlen kommt vor Transportieren.“** Die Leitlinie schreibt diesen Merksatz
  ausdrücklich hin („cool first, transfer second“). Er dreht genau den Reflex
  um, den man hat.
* **Die 15-Minuten-Regel:** ohne Messmöglichkeit 15 Minuten kühlen oder bis
  Verwirrtheit und Krampf verschwunden sind, was zuerst eintritt.
* **Die Plane.** Die Leitlinie nennt als Behelf draußen ausdrücklich: die
  Person in eine Plane wickeln, Wasser (und Eis, wenn vorhanden) hinein, die
  Plane sanft schaukeln — dazu gibt es eine eigene Abbildung. Auch das
  Planschbecken am Gartenschlauch. Für ein Paket, in dem fast jeder eine Plane
  dabeihat, ist das eine der brauchbarsten Einzelheiten überhaupt.
* **Die Rangfolge der Kühlverfahren** aus dem Begründungsteil, mit zwei
  Gegen-die-Erwartung-Befunden: **lauwarmes Wasser (20–25 °C) wirkte besser
  als sehr kaltes (8–12 °C)** — man braucht also kein Eis —, und **Fächeln
  und Verdunstungskühlung stehen ganz unten**, zusammen mit dem bloßen
  In-den-Schatten-Legen. Das sind genau die Dinge, die man von selbst tut.

**HALSWIRBELSÄULE — die zwei benannten Griffe.** Der Eintrag sagte „Kopf oder
Nacken festhalten“. Die Leitlinie beschreibt beide Griffe Schritt für Schritt,
und der Unterschied ist keine Feinheit, sondern entscheidet, ob man es
überhaupt durchhält:

* **Ellbogen auf den Boden oder auf die eigenen Knie aufsetzen** — beide Griffe
  fangen damit an. Die Ellbogen tragen, nicht die Arme.
* **Kopfgriff:** Daumen über den Ohren, Finger darunter, **Ohren frei lassen**,
  damit die Person noch hören kann.
* **Schultergriff:** Hände auf die Schultermuskeln, Daumen nach unten auf der
  Vorderseite, Finger längs der Wirbelsäule, dann die **Unterarme nach innen**
  — der Kopf wird zwischen den Unterarmen auf Ohrhöhe gehalten, nicht von den
  Händen. Das ist der Griff, den man Stunden hält.

Der Eintrag sagt seit dem 28.07.2026: „Einer kniet dauerhaft am Kopf und hält,
und alle anderen richten sich darauf ein, dass es lange dauert.“ Jetzt steht
auch da, WIE. Nicht aus der Quelle und als solches gekennzeichnet ist nur die
Ablöse-Regel (der zweite legt an, bevor der erste loslässt).

Abgesichert im Test `dieNachtraegeAusDemVolltext2025StehenDrin`.

### Damit ist die Erste-Hilfe-Leitlinie 2025 durchgearbeitet

Offen aus der ursprünglichen Liste der 52 kurzfassungsgestützten Einträge
bleiben die Themen, die NICHT in dieser Leitlinie stehen — vor allem die
15 Einträge auf Grundlage der DFV-Merkblätter und die Poster-gestützten
Wiederbelebungs-Einzelheiten, die schon geprüft sind. Der nächste ergiebige
Volltext wäre die Kinder-Leitlinie (`work/quellen/medizin-luecken/kinder-blutung/`)
für die übrigen Kinder-Einträge.

## 17.08.2026 — Der Defibrillator: die zwei Sätze, die das Zögern auflösen

Abgleich der beiden AED-Einträge gegen die Volltexte der Leitlinien 2025 für
Erwachsene (`work/quellen/wiederbelebung-erwachsene/`) und für Kinder
(`work/quellen/medizin-luecken/kinder-blutung/`).

Der Eintrag „Defibrillator (AED): anwenden“ sagte bisher nur: einschalten, Pads
auf den nackten Brustkorb, Anweisungen befolgen. **Er sagte weder, WOHIN die
Pads gehören, noch irgendetwas über Kinder.** Beides steht ausführlich in den
Leitlinien.

### Die Pad-Lage beim Erwachsenen

Ein Pad **unterhalb des rechten Schlüsselbeins**, dicht rechts neben dem oberen
Ende des Brustbeins; das andere **mittig unter der linken Achsel**
(mittlere Achsellinie). Brustgewebe aussparen. Die andere Lage — vorn und
hinten — ist ausdrücklich Geschulten vorbehalten, weil sie verlangt, die
Person zu drehen.

### Der BH — und warum das in einer Überlebens-App steht

Die Leitlinie führt aus, dass **Frauen von Umstehenden seltener wiederbelebt
und seltener defibrilliert werden**, und benennt den Grund: die Scheu, den
Brustkorb freizumachen, samt der Furcht, es könnte als Übergriff ausgelegt
werden. Dazu gibt es eine klare Empfehlung, und die ist übernommen:

* Die Pads müssen auf die **nackte Haut** und an die richtige Stelle.
* Meistens genügt es, den **BH zu verschieben** — weder öffnen noch schneiden.
* Ein **Bügel** schadet nach heutigem Kenntnisstand nicht.
* **„Rescuers should not be concerned about exposing the person's chest“** —
  Lebensrettung geht vor Schamgefühl.

Das ist kein Randthema: Es ist ein belegtes Hindernis, das Menschen das Leben
kostet, und es löst sich durch einen Satz auf.

### Kinder — die Frage, die fast jeden lähmt

**Darf man einen AED bei einem Kind benutzen? Ja.** Und die Leitlinie sagt
ausdrücklich: **Hat das Gerät keine Anleitung für Kinder, wird es im
Erwachsenen-Betrieb benutzt.**

* **Kinder-Betrieb** (Schalter, Schlüssel oder eigener Stecker) bei Säuglingen
  und Kindern **unter 25 kg**, also etwa bis acht Jahre. Darüber
  Erwachsenen-Betrieb.
* **Pad-Lage unter 25 kg: vorn und hinten** — vorn mitten auf die Brust
  unmittelbar links neben das Brustbein, hinten mittig zwischen die
  Schulterblätter. Grund: Auf einem kleinen Brustkorb lägen zwei Pads
  nebeneinander zu dicht beisammen. Dabei werden die normalen
  Erwachsenen-Pads verwendet.
* Bei größeren Kindern und Jugendlichen geht beides; bei Jugendlichen
  Brustgewebe aussparen.

### Was daneben geprüft und deckungsgleich war

„Herzdruckmassage bei Kindern und Säuglingen“ stimmt Wort für Wort mit der
Kinder-Leitlinie 2025 überein: untere Hälfte des Brustbeins in jedem Alter,
Zwei-Daumen-Technik beim Säugling, eine oder zwei Hände ab einem Jahr **oder
wenn die Zwei-Daumen-Technik nicht kräftig genug gelingt**, 100–120 je Minute,
mindestens ein Drittel des Durchmessers, 5–6 cm bei Jugendlichen, nie über
6 cm, kein Aufstützen zwischen den Stößen. Auch „Wiederbelebung Kind: zuerst
beatmen“ ist unverändert richtig.

### Zur Suche

Gemessen: „pads“, „elektroden“ und „bh“ führen jeweils genau auf diesen
Eintrag, „defibrillator“ und „aed“ unverändert zuerst auf ihn. **Verdrängt
wurde nichts** — „kind“ führt weiter auf die Kinder-Einträge, „schock“
weiter auf den Kreislaufschock.

Gemessen und BEWUSST NICHT nachgebessert: „plane“ führt nicht auf die
Planenkühlung beim Hitzschlag. Wer überhitzt ist, sucht „hitzschlag“, nicht
„plane“ — und ein Schlagwort dafür würde die Einträge verdrängen, in denen
eine Plane wirklich das Thema ist.

Abgesichert im Test `derAedTippNimmtBeideZoegernMomente`.

## 17.08.2026 — Kinder-Normwerte: zwei Quellen, zwei Zahlenreihen

Die Grundwerte in „Eine frische Verletzung beurteilen“ … genauer: in
„Körperliche Untersuchung ohne Geräte“ stammten aus dem US-Sanitätshandbuch
TC 4-02.1 (2016). Die europäische Kinder-Leitlinie 2025 nennt eigene Werte, und
sie sind nach oben deutlich weiter.

Beispiel, an dem der Unterschied hängt: **Ein Zweijähriger mit einem Puls von
140** liegt nach der amerikanischen Fassung über dem Normbereich (70–120 für
1–6 Jahre) und nach der europäischen mitten darin (90–160 mit zwei Jahren).

**Beide Fassungen stehen jetzt im Eintrag**, nicht eine statt der anderen. Das
ist dasselbe Vorgehen wie beim Amputat (feucht gegen trocken): Ein
Widerspruch zwischen zwei ordentlichen Quellen wird offengelegt, nicht
stillschweigend aufgelöst. Wer nur eine Zahlenreihe kennt und die andere im
Umlauf ist, gerät sonst mitten im Notfall ins Zweifeln.

Dazu die Regel, wie man damit umgeht — ausdrücklich als Einordnung
gekennzeichnet: **Was außerhalb der ENGEREN Werte liegt, wird noch einmal
gemessen und aufgeschrieben; was außerhalb der WEITEREN liegt, ist ein
Alarmzeichen.** Und der Satz des Eintrags bleibt in Kraft: Die Reihe sagt mehr
als der Einzelwert.

Übernommen ist außerdem ein Satz aus Tafel 3 der Kinder-Leitlinie, der über
allen Zahlen steht: **Ein Kreislaufversagen kann bei einem Kind plötzlich und
ohne Vorzeichen eintreten.** Gute Werte sind keine Entwarnung.

Die europäischen Werte decken auch mehr Alter ab als die bisherigen: 1 Monat,
1, 2, 5, 10 und 18 Jahre, mit dem ausdrücklichen Hinweis, für Alter dazwischen
zwischen den Zeilen zu schätzen.

Abgesichert im Test `dieKinderNormwerteLegenIhrenUnterschiedOffen`.

## 17.08.2026 — Kein Aspirin für Kinder, und die Gürtelrose

Zwei Lücken, die beim Nachschlagen nach „Windpocken“ aufgefallen sind — das
Thema selbst fehlt weiterhin, aber auf dem Weg dorthin lagen zwei wichtigere
Dinge.

### Die Warnung, die im ganzen Paket fehlte

`nachschlagen.py reye` fand **nichts**. Das Paket hatte 453 Einträge, mehrere
davon zu Fieber und zu Kindern, und **keinen einzigen Satz darüber, dass ein
fieberndes Kind kein Aspirin bekommt.**

Quelle: WHO, „Pocket Book of Hospital Care for Children“, 2. Auflage 2013,
Kapitel 10 „Supportive care“ — seit langem lokal unter
`work/quellen/medizin-luecken/who-pocketbook-2013-fulltext.txt`. (Zum wievielten
Mal in dieser Sammlung: Die Quelle lag schon da.)

Wörtlich dort: Aspirin ist kein Fiebermittel erster Wahl, weil es mit dem
**Reye-Syndrom** in Verbindung steht — selten, aber schwer, und es trifft
**Leber und Gehirn**. Ausdrücklich zu meiden bei Kindern mit **Windpocken**,
Dengue-Fieber und anderen Erkrankungen mit Blutungsneigung.

Das ist die gefährlichste Sorte Lücke: Aspirin liegt in fast jeder
Hausapotheke, es ist das Mittel, das die Erwachsenen kennen, und der Griff
danach ist gut gemeint.

Mit übernommen, weil es an dieselbe Stelle gehört und ebenfalls fehlte:

* **Paracetamol** erst ab zwei Monaten, erst ab 39 °C und nur bei
  Beschwerden — die Quelle sagt ausdrücklich, dass ein **waches, lebhaftes
  Kind von einem Fiebermittel voraussichtlich nichts hat**. 15 mg je kg alle
  6 Stunden.
* **Ibuprofen** gleich wirksam und gleich sicher, reizt aber den Magen.
  10 mg je kg alle 6–8 Stunden.
* **Metamizol und Phenylbutazon** nennt die Quelle in einem Atemzug als giftig
  und wirkungsschwach.
* Pflege: leicht anziehen, warmer aber gut gelüfteter Raum, mehr trinken.

Die Mengen stehen mit dem Verweis auf „Alle Angaben ohne Gewähr“, wie im
ganzen Paket üblich.

### Neuer Eintrag: „Gürtelrose: nur eine Körperhälfte“

`nachschlagen.py guertelrose` fand ebenfalls nichts. Quelle: **US Special
Operations Forces Medical Handbook, 1. Auflage 2001**, Abschnitt „Skin: Herpes
Zoster (Shingles)“ — Werk der US-Bundesregierung, über den
Schutzfristenvergleich frei, Volltext unter `work/quellen/werke-frei/`.

Drei Dinge machen den Eintrag aus:

**DAS ERKENNUNGSZEICHEN:** Der Ausschlag bleibt auf EINER Körperhälfte und
hört an der Mittellinie auf, weil er dem Streifen eines einzelnen Nervs folgt.

**DER SCHMERZ KOMMT ZWEI BIS SIEBEN TAGE VOR DEM AUSSCHLAG**, und die Quelle
nennt selbst, womit das verwechselt wird: **Herzbeschwerden, akuter Bauch,
Bandscheibenvorfall**. Ein Warnsignal, das im Paket sonst nirgends steht — wer
brennende Schmerzen ohne sichtbare Ursache hat, soll die nächsten Tage
nachsehen.

**DAS AUGE.** Bläschen am Augenlid oder **auf der Nasenspitze** bedeuten, dass
derselbe Nervenast betroffen ist, der das Auge versorgt — laut Quelle bei
30 von 100 mit Beteiligung dieses Astes, und sie nennt die Folge beim Namen:
**Erblindung**. Das ist der eine Fall, in dem sich ein weiter Weg lohnt.

Dazu der Zeitpunkt der Behandlung: Die Quelle sagt ausdrücklich, dass eine
**schon in der Schmerzphase begonnene** Behandlung den Verlauf mildert und
verkürzt. Wer wartet, bis der Ausschlag „richtig“ da ist, verschenkt die
wirksamsten Tage.

**Nicht übernommen, weil nicht belegt:** ob und wie ansteckend eine Gürtelrose
ist. Die Quelle sagt dazu nichts, und geraten wird hier nicht. Das bleibt eine
offene Frage für eine zweite Quelle — zusammen mit den Windpocken selbst, die
weiterhin fehlen.

### Zur Suche

„guertelrose“, „zoster“, „nasenspitze“ und „koerperhaelfte“ führen genau auf
den neuen Eintrag, „windpocken“ auf ihn und auf den Fieber-Eintrag, „reye“ auf
den Fieber-Eintrag. **Verdrängt wurde nichts:** „ausschlag“ führt weiterhin
zuerst auf Fieber und Masern (Hirnhautentzündung-Muster), „auge“ auf die
Augenverletzungen, „schmerz“ und „seite“ unverändert. Der Titel wurde bewusst
so gewählt, dass er keines dieser Wörter enthält.

Abgesichert in `derFieberTippVerbietetAspirinFuerKinder` und
`derGuertelroseTippNenntDasAugeUndDenZeitpunkt`.

## 17.08.2026 — Windpocken, und was die Gürtelrose weitergibt

Quelle: **RKI-Ratgeber „Windpocken (Varizellen), Gürtelrose (Herpes zoster)“**,
rki.de, selbst geladen und im Volltext gelesen am 17.08.2026, Sicherung unter
`work/quellen/windpocken/`. (Der erste Suchtreffer war eine Zusammenfassung des
Werkzeugs — die zählt nach Regel 4 nicht. Erst die selbst geladene Seite.)

### Der neue Eintrag steht auf einer einzigen Folgerung

Das medizinisch Entscheidende an Windpocken ist für dieses Paket nicht die
Behandlung — die gibt es bei gesunden Kindern gar nicht —, sondern eine
Zeitangabe:

> **Ansteckend ist jemand 1–2 Tage, BEVOR der Ausschlag erscheint**, und bis
> alle Bläschen vollständig verkrustet sind (5–7 Tage nach Beginn).

Daraus folgt alles andere: **Wer erst trennt, wenn der Ausschlag da ist, trennt
zu spät.** Im Haushalt ist die Ansteckung dann schon geschehen. Deshalb sagt
der Eintrag ausdrücklich das Gegenteil dessen, was man von selbst tut: Nicht
das kranke Kind wird weggesperrt — **die gefährdete Person kommt aus dem
Raum.**

Und weil eine Notunterkunft der Regelfall dieser App ist, steht die Rechnung
dazu daneben: **über 90 von 100 Empfänglichen erkranken nach einer Aussetzung**
(Kontagionsindex nahe 1,0), das Virus trägt **mehrere Meter** mit dem Atem.
Abstand ist auf engem Raum keine Lösung.

Die drei Gefährdeten mit den Zahlen der Quelle: **Neugeborene** (Mutter erkrankt
5 Tage vor bis 2 Tage nach der Geburt → Sterblichkeit bis 30 %, höchstes Risiko
bei Erkrankung am 5.–10. Lebenstag), **Schwangere ohne durchgemachte
Windpocken** (SSW 5–24 → fetales Varizellensyndrom bei 1–2 %; dazu besonderes
Risiko für die Lungenentzündung) und **Abwehrgeschwächte**.

Weiter übernommen: der **„Sternenhimmel“** (Knötchen, Bläschen und Schorf
gleichzeitig — das Unterscheidungsmerkmal), Inkubationszeit 14–16 (10–21) Tage,
Fieber selten über 39 °C für 3–5 Tage, Beginn an Rumpf und Gesicht samt
Kopfhaut und Schleimhäuten, die **bakterielle Superinfektion der aufgekratzten
Haut als häufigste ernste Komplikation**, die Varizellenpneumonie (bis 20 % der
Erwachsenen, Beginn 3–5 Tage nach Krankheitsbeginn), die ZNS-Beteiligung bei
etwa 0,1 %, und die Wiederzulassung: **eine Woche nach Krankheitsbeginn UND
vollständig verkrustet** — beides zusammen.

Die Quelle führt das **Reye-Syndrom unter den Komplikationen der Windpocken**
auf. Damit schließt sich der Kreis zur Aspirin-Warnung von heute Nacht; der
Eintrag verweist darauf.

### Die offene Frage von vorhin ist beantwortet

Beim Gürtelrose-Eintrag stand hier noch: *„Nicht übernommen, weil nicht belegt:
ob und wie ansteckend eine Gürtelrose ist.“* Der RKI-Ratgeber deckt genau das
ab, und es ist jetzt nachgetragen:

* **Nur die Flüssigkeit in den Bläschen ist ansteckend**, nicht der Atem —
  darum viel geringere Kontagiosität als bei Windpocken.
* **Vollständiges Abdecken der Stellen senkt die Gefahr deutlich.** Das ist eine
  Maßnahme, die jeder überall umsetzen kann.
* **Weitergegeben werden Windpocken, nicht Gürtelrose** — an jeden, der sie
  noch nie hatte.
* Und die Entwarnung: **Von einer Gürtelrose der Mutter geht keine Gefahr für
  das ungeborene Kind aus.**

### Zur Suche — und ein Titel, der zurückgenommen wurde

Der erste Titel hieß „Windpocken: der Sternenhimmel und wer wegbleiben muss“.
Gemessen: **„stern“ führte damit zuerst auf die Windpocken statt auf den
Polarstern** — weil die Suche Wortanfänge vergleicht und der Titel das am
stärksten gewichtete Feld ist. Titel geändert auf „Windpocken: alle Stadien auf
einmal, und wer wegbleiben muss“; danach steht „stern“ wieder auf den
Orientierungs-Einträgen, und „sternenhimmel“ findet weiterhin die Windpocken
(über das Schlagwort).

Ebenso zurückgenommen: Das Schlagwort „schwangere und windpocken“ hatte die
Windpocken auf Platz 1 bei „schwangere“ geschoben. Umformuliert zu „windpocken
in der schwangerschaft“ — danach steht die alte Reihenfolge wieder.

**Der Titelwechsel hat einen alten Verweis stehen lassen**, und der
Querverweis-Wächter hat ihn gefangen: Der Gürtelrose-Eintrag zeigte noch auf
den alten Titel. Genau dafür ist dieser Test da.

Abgesichert in `derWindpockenTippZiehtDieRichtigeFolgerung`.

## 17.08.2026 — Hexenschuss und Ischias

`nachschlagen.py hexenschuss` und `ischias` fanden **nichts**. Bei einer der
häufigsten Beschwerden überhaupt — und in einer Lage, in der jeden Tag Wasser
und Holz getragen werden, ist ein ausgefallener Rücken der Ausfall einer
Arbeitskraft.

Quelle: **US Special Operations Forces Medical Handbook, 1. Auflage 2001**,
Abschnitt „Pain, Low Back“ von CDR Scott Flinn, MC, USN — Werk der
US-Bundesregierung, über den Schutzfristenvergleich frei, Volltext seit langem
unter `work/quellen/werke-frei/`. (Auch hier lag die Quelle schon da.)

### Der Eintrag steht gegen den Reflex

Der Schmerz sagt „hinlegen“. Die Quelle sagt in beiden Richtungen das
Gegenteil — einmal als Anweisung an den Helfer, einmal als Merksatz für den
Betroffenen:

> **Bettruhe ist nicht angezeigt**, außer sie ist wirklich unvermeidlich — sie
> führt nur dazu, dass der Körper abbaut. Liegen vermeiden, wenn es geht; es
> schwächt nur die Rückenmuskeln.

Dazu die Aussicht, die die Angst nimmt: **Der größte Teil aller Kreuzschmerzen
kommt von Überlastung, hört von selbst auf und ist nach vier bis sechs Wochen
vorbei.** Behandlung: kühlen, schrittweise wieder bewegen, entzündungshemmendes
Schmerzmittel (Ibuprofen 800 mg dreimal täglich mit Essen, mit dem üblichen
Vorbehalt und der Warnung der Quelle vor Magengeschwüren und Nieren- und
Leberschaden bei Dauergebrauch). **Cortison zum Einnehmen wird ausdrücklich
nicht empfohlen.**

### Der eine Fall mit einer Frist

**Verliert jemand mit Kreuzschmerzen — ohne Unfall — die Kontrolle über Blase
oder Darm**, ist das eine Einklemmung der Nervenbündel am unteren Ende des
Rückenmarks. Die Quelle nennt die Frist: **ohne operative Entlastung innerhalb
von 12 bis 24 Stunden kann der Schaden bleiben.**

Dazu das zweite Zeichen, das niemand von selbst prüft und das der Eintrag
deshalb ausdrücklich zu prüfen verlangt: **Taubheit genau dort, wo man auf
einem Sattel aufsitzt** — Schritt, Innenseiten der Oberschenkel, Gesäßspalte —
und der Verlust der Schließmuskelspannung.

Die übrigen Warnzeichen der Quelle sind ebenfalls übernommen: Ausstrahlung
über das Knie hinaus oder Beinschwäche (Bandscheibe auf einem Nerv), Fieber
über zwei Wochen, Nachtschmerz zusammen mit großem ungewolltem Gewichtsverlust
(die Quelle nennt rund neun Kilo), Schmerz über vier bis sechs Wochen, Bruch
nach Unfall — und der Punkt, den ein Laie nicht auf dem Schirm hat:
**Kreuzschmerzen bei Kindern und Jugendlichen sind ungewöhnlich.**

Ebenfalls aus der Quelle: was sich als Rückenschmerz tarnt — Nierenstein,
Nierenbeckenentzündung, Bauchspeicheldrüsenentzündung, Wehen und bei alten
Menschen eine Aussackung der Bauchschlagader. Und die Vorbeugung in einem Satz:
**beim Heben in den Knien beugen, nicht im Rücken.**

### Der zweite Zweig ist hier ehrlich unbefriedigend

Die Einklemmung mit Blasenlähmung lässt sich ohne Operation nicht beheben. Der
Eintrag sagt das so — und nennt trotzdem zwei Gründe, sie zu erkennen: Gibt es
irgendeine erreichbare Klinik, entscheidet dieser eine Tag; gibt es keine, weiß
man wenigstens, dass Liegen und Wärme hier nichts ausrichten und dass die Blase
geleert werden muss, damit sie nicht überläuft.

### Zur Suche, und der Wächter hat wieder gefangen

„hexenschuss“, „ischias“, „kreuzschmerzen“ und „bandscheibe“ führen genau auf
den Eintrag. **Verdrängt wurde nichts:** „schmerz“, „bein“, „heben“ und
„ruecken“ stehen unverändert.

Und wie schon beim Windpocken-Eintrag hat der **Querverweis-Wächter** einen
Fehler gefangen, bevor etwas committet war: Der Entwurf verwies auf
„Nierenstein: die Kolik, die kommt und geht“ — der Eintrag heißt aber
„Nierenstein: der Schmerz kommt und geht in Wellen“. Zwei gefangene Verweise in
einer Nacht; der Test verdient seinen Platz.

Abgesichert in `derHexenschussTippStelltSichGegenDenReflex`.

### Weiterhin offen

**Gerstenkorn:** Im SOF-Handbuch kommt es nur als Nebensatz in der Abgrenzung
zur Lidphlegmone vor — zu wenig für einen eigenen Eintrag. Es fehlt eine
Quelle, die es selbst behandelt.

## 17.08.2026 — Bucheckern: ein neuer Eintrag, und ein Fehler im alten

`nachschlagen.py bucheckern` fand keinen eigenen Eintrag — aber einen Treffer,
der schlimmer war als gar keiner: **Die Sammelliste „Essbares Grün mit Namen“
führte die Buchecker unter den essbaren Pflanzen, ohne jede Bedingung.**

Quelle: **Bundeszentrum für Ernährung (BZfE), „Bucheckern sammeln und
verarbeiten“**, Text von Heike Kreutz, bzfe.de, selbst geladen und im Volltext
gelesen am 17.08.2026, Sicherung unter `work/quellen/bucheckern/`.

### Der Fehler in der alten Liste

Die Liste stammt aus einer nordamerikanischen Überlebensquelle und nennt
„Buchecker“ in einer Aufzählung mit Wegwarte, Distel, Portulak und anderen.
Aber:

> **Roh sind sie leicht giftig** (u. a. Alkaloide und Oxalsäure) und können
> Bauchschmerzen, Übelkeit oder Erbrechen verursachen. **Erst durch Erhitzen —
> Rösten, Braten oder Überbrühen — werden diese Stoffe unschädlich.**

Das ist die gefährlichste Bauart eines Fehlers, die dieses Paket haben kann:
eine Liste, die jemanden **richtig sammeln** und **falsch essen** lässt. Der
Satz steht jetzt in BEIDEN Einträgen, und ein Test hält beide fest.

### Warum ein eigener Eintrag

**40 Prozent Fett.** Fett ist der Nährstoff, der in einer Notlage am
schnellsten fehlt und am schwersten zu beschaffen ist — und Bucheckern liegen
in Mastjahren zu Tausenden auf dem Boden. Die Quelle nennt auch die drei alten
Verwendungen aus Notzeiten: Nahrungsmittel, Ölgewinnung, Kaffeeersatz.

Weiter übernommen: Rotbuche weit verbreitet, Früchte erst **ab etwa 40 Jahren**
Baumalter, dreikantig und rund 1,5 cm, meist zwei in einer vierklappigen
stacheligen Hülle, **Sammelzeit September bis November**.

Und der Handgriff, der dem Eintrag seinen Titel gibt: **Überbrühen, und was
oben schwimmt, wird aussortiert** — das sind die hohlen, verdorbenen oder
alten. Man bekommt das Sortieren geschenkt, während man ohnehin überbrüht,
ohne Licht und ohne Erfahrung.

### Als Einordnung gekennzeichnet, nicht aus der Quelle

Das Überbrühwasser wird **weggeschüttet**; ohne Wasser tut es trockenes Rösten
(die Quelle nennt Rösten, Braten und Überbrühen gleichrangig); ein
behelfsmäßiger Weg zu Öl ohne Presse; und eine Mengenwarnung — wer lange wenig
gegessen hat, verträgt große Fettmengen schlecht, und Durchfall ist in einer
Notlage gefährlicher als Hunger.

### Herausgenommen, weil unbelegt

Der Entwurf enthielt einen Satz über **Eicheln** (roh bitter, wässern bis die
Bitterkeit heraus ist). Das steht so in KEINER der gelesenen Quellen, und
`nachschlagen.py eichen` zeigt: Das Paket hat überhaupt nichts über Eicheln als
Nahrung. Der Satz ist gestrichen worden statt aus dem Gedächtnis geschrieben.

**Offen bleibt damit: Eicheln als Nahrung** — in derselben Liste genannt
(„Eichen“), ebenfalls ohne Bedingung, und ohne eigenen Eintrag. Das braucht
eine Quelle.

### Zur Suche

„bucheckern“, „buche“, „überbrühen“ und „kaffeeersatz“ führen auf den neuen
Eintrag, danach jeweils auf die Sammelliste — genau die richtige Reihenfolge.
**Verdrängt wurde nichts:** „giftig“ führt weiter auf die Warnlisten, „essbar“
auf den Essbarkeitstest, „mehl“ auf das Mahlen. Der Titel wurde bewusst ohne
das Wort „giftig“ gewählt, damit er den Giftpflanzen-Einträgen nichts wegnimmt.

Abgesichert in `dieBucheckerStehtNirgendsOhneIhreBedingung`.

### Weiterhin offen bei den Pflanzen

**Giersch, Vogelmiere, Knoblauchsrauke** — drei der häufigsten essbaren
Wildpflanzen Mitteleuropas, alle drei fehlen. Der Grund ist derselbe wie beim
Gerstenkorn: keine freie Quelle gefunden, die sie selbst behandelt. Beim Giersch
kommt hinzu, dass die wirklich wichtige Frage die **Verwechslung mit dem
Schierling** ist — dafür reicht keine Kochseite, das braucht eine
botanisch belastbare Quelle.

## 17.08.2026 — Giersch, und die Familie mit den tödlichen Mitgliedern

Quellen, beide selbst geladen und im Volltext gelesen am 17.08.2026, gesichert
unter `work/quellen/wildkraeuter/`:

* **NABU**, Pflanzenporträt „Giersch — Drei-drei-drei, bist beim Giersch
  dabei!“ und „Gefährliche Doppelgänger: Vorsicht Verwechslungsgefahr!“
* **Bayerische Landesanstalt für Landwirtschaft (LfL)**, Unkraut-Steckbriefe
  „Giersch, Geißfuß“ und „Acker-Hundspetersilie“ — die botanisch belastbare
  Seite, die im letzten Eintrag hier noch als fehlend vermerkt war.

### Warum der Eintrag mit dem Erkennen anfängt und nicht mit dem Essen

Giersch ist in Mitteleuropa massenhaft und leicht zu ernten — und er gehört zu
den **Doldenblütlern**, der Familie mit den gefährlichsten Verwechslungen, die
es bei uns gibt. Der NABU nennt **rund dreißig heimische Arten** mit ähnlichen
weißen Dolden und die Rangfolge der giftigen: **der Gefleckte Schierling ist
der giftigste**, danach Riesen-Bärenklau, Hundspetersilie, Hecken-Kälberkropf.

### Die Regel, die den Eintrag trägt

> **„Drei-drei-drei, bist beim Giersch dabei.“** Drei Teile hat das Blatt, jeder
> davon ist noch einmal dreigeteilt, und **der Stängel ist dreikantig.**

Der NABU bezeichnet genau das als das Merkmal, das den Giersch **gegenüber
allen anderen Doldenblütlern** abgrenzt. Der dritte Punkt ist der beste, weil
er ohne Augen funktioniert: Man rollt den Stängel zwischen zwei Fingern und
fühlt Kanten statt eines runden Rohrs.

Aus den LfL-Steckbriefen kommt der Gegentest dazu, und er ist scharf:

| | Giersch | Acker-Hundspetersilie |
|---|---|---|
| Stängel | **dreikantig** | **glatt, oft violett überlaufen** |
| Blatt | 3×3 geteilt, **scharf gesägter Rand** | 2- bis 3-fach gefiedert, **glänzend dunkelgrün, dreieckig im Umriss** |
| Dolde | weiß, groß, 10–20-strahlig, Einzelblüten ~3 mm | weiß, klein, 10–20-strahlig, mittelgroß |
| Blütezeit | Mai–September | Juni–Oktober |

**Der Stängel ist damit das brauchbarste Merkmal überhaupt** — im wörtlichen
Sinn in der Hand. Der Eintrag sagt deshalb: Wer im Zweifel ist, greift an den
Stängel, bevor er an das Blatt geht.

### Die Entscheidung, die über allem steht

**Bei Doldenblütlern gilt der Essbarkeitstest NICHT.** Das ist als Einordnung
gekennzeichnet, aber es folgt zwingend: Der Test des Pakets prüft auf Reizung
und Unverträglichkeit — der Schierling vergiftet in kleinen Mengen, ohne vorher
zu warnen. **Wo eine Pflanzenfamilie tödliche Mitglieder hat, wird bestimmt und
nicht probiert.** Der Eintrag verweist dazu auf „Wo der Essbarkeitstest
versagt“.

Ebenfalls übernommen: die vier Bestimmungsfragen des NABU (Blüte, Geruch,
Stängel, Blätter) mit der Einschränkung der Quelle, dass **Geruch und Aussehen
stark vom Standort abhängen** — der Geruch allein entscheidet also nichts. Und
der Satz, der alles abschließt: **nur pflücken, was man sicher bestimmen kann.**

### Zur Suche — und ein Schlagwort, das zurückgenommen wurde

Der Entwurf hatte das Schlagwort „schierling verwechslung“. Gemessen: Damit
stand der **Giersch-Eintrag auf Platz 1 bei „schierling“** — vor
„Giftpflanzen mit Namen“, wo diese Frage hingehört. Umformuliert zu „womit kann
man giersch verwechseln“; danach führt „schierling“ wieder zuerst auf die
Giftpflanzen-Einträge und der Giersch steht auf Platz 3, was richtig ist.

„giersch“, „hundspetersilie“ und „dolde“ führen genau auf den neuen Eintrag.
**Verdrängt wurde nichts:** „giftig“, „essbar“ und „verwechslung“ stehen
unverändert.

Abgesichert in `derGierschTippStelltDasBestimmenVorDasSammeln`.

### Damit noch offen

**Vogelmiere und Knoblauchsrauke** (beide harmlos zu bestimmen, aber ohne
gelesene Quelle), **Eicheln als Nahrung**, **Gerstenkorn** und **Haus
notdürftig sichern**.

## 17.08.2026 — Vogelmiere: die Pflanze für die Lücke im Winter

Quellen, alle selbst geladen und im Volltext gelesen am 17.08.2026, gesichert
unter `work/quellen/wildkraeuter/`:

* **LfL**, Unkraut-Steckbrief „Vogelmiere“ (Stellaria media)
* **LfL**, Merkblatt „Essbare Wildkräuter — Vogelmiere“ von Jutta Kotzi, IAB,
  11/2022
* **LfL**, Artenporträt „Acker-Gauchheil“ (Anagallis arvensis) — für die
  Abgrenzung
* **NABU**, Pflanzenporträt „Vogelmiere“

### Warum gerade diese Pflanze

Sie **blüht fast ganzjährig**, weil sie nicht von der Tageslänge abhängt, und
ist laut NABU oft **eine der ersten blühenden Pflanzen nach dem Schnee**. Damit
füllt sie genau die Lücke, in der ein Vorrat zur Neige geht und noch nichts
wächst — Spätwinter und zeitiges Frühjahr. Keine andere Pflanze im Paket kann
das.

### Das eine Merkmal

**Der Stängel ist einreihig behaart.** Eine einzige schmale Haarlinie läuft an
ihm entlang — nicht ringsherum, nicht flaumig. Das ist mit bloßem Auge zu sehen
und mit den Fingern zu fühlen.

Dazu aus dem Steckbrief: gegenständige rundlich-eiförmige Blätter mit Spitze,
niederliegende 5–30 cm lange Stängel, die **an den Knoten wurzeln** (daher die
Teppiche) — und die Blüte mit dem Zähltrick: **fünf Kronblätter, so tief
gespalten, dass es zehn zu sein scheinen.**

### Die Abgrenzung — und was dabei NICHT behauptet wird

Der Acker-Gauchheil wächst ähnlich niederliegend und hat ebenfalls spitze,
ovale, gegenständige Blätter. Zwei Merkmale trennen ihn eindeutig, und beide
stehen im LfL-Porträt: **vierkantiger Stängel** (Vogelmiere: rund mit einer
Haarreihe) und **Blüten von 1–1,5 cm, meist hellrot, manchmal blau** (Vogelmiere:
winzig und weiß).

**Bewusst NICHT übernommen:** Eine Suchmaschinen-Zusammenfassung behauptete,
der Acker-Gauchheil sei schwach giftig. **Im gelesenen LfL-Porträt steht davon
nichts.** Also steht es auch nicht im Eintrag — die Abgrenzung trägt sich über
die Merkmale, nicht über eine ungeprüfte Giftigkeitsangabe. Das ist dieselbe
Regel, die heute Nacht schon den Eichel-Satz gekostet hat.

### Übernommen, weil es beim Sammeln hilft

Die Volksnamen **Mäusedarm, Hühnerdarm, Vogelkraut** (wer sie kennt, sucht nicht
die falsche Pflanze) und der Geschmack: **erinnert an junge Maiskolben**, saftig
und mild. Das ist praktisch — wer eine Wildpflanze isst, die nicht bitter und
nicht kräuterartig schmeckt, hat vermutlich die richtige.

### Zur Suche

„vogelmiere“, „huehnerdarm“, „gauchheil“ und „haarreihe“ führen genau auf den
Eintrag. **Verdrängt wurde nichts:** „winter“, „schnee“, „unkraut“ und
„staengel“ stehen unverändert; „giersch“ führt weiter zuerst auf den Giersch.

Abgesichert in `derVogelmiereTippNenntSeinErkennungsmerkmal`.

### Von den vier Pflanzen bleibt eine

Giersch, Vogelmiere und Bucheckern sind erledigt. **Knoblauchsrauke** fehlt
weiter — dafür ist heute Nacht keine gelesene Quelle zusammengekommen.

## 17.08.2026 — Eicheln: der Brotbaum, und die zweite Lücke in derselben Liste

Der offene Punkt von vorhin ist geschlossen. Quelle: **BZfE, „Eicheln als Mehl
oder Kaffeeersatz“**, Text von Heike Kreutz, bzfe.de, selbst geladen und im
Volltext gelesen am 17.08.2026, Sicherung unter `work/quellen/eicheln/`.

Damit hat dieselbe Sammelliste, die schon die Buchecker ohne Bedingung führte,
ihre **zweite** Lücke geschlossen bekommen — sie nannte auch „Eichen“ einfach
mit.

### Der Unterschied zu einer Giftpflanze, und warum er hier wichtig ist

> **Roh und direkt vom Baum sind Eicheln ungenießbar.** Bittere Gerbstoffe
> (Tannine) können Magen- und Darmbeschwerden verursachen, **aber sie lassen
> sich durch Wässern problemlos entfernen.**

Hier wird nichts entgiftet, sondern etwas HERAUSGEWASCHEN — und man kann dabei
zusehen. Das ergibt eine Probe, die kein Gerät braucht und die den Eintrag
trägt:

**Die Kerne kommen in lauwarmes Wasser, das sich bräunlich färbt. Das Wasser
wird mehrmals über ein bis zwei Tage gewechselt, bis es farblos bleibt.**
Solange sich das Wasser noch färbt, ist noch Bitterstoff drin. Man muss nicht
kosten und nicht schätzen.

Dazu aus der Quelle: Sammelzeit **September bis Ende Oktober**, nur reife,
unversehrte Früchte **ohne Verfärbungen und Madenlöcher**; **ein bis zwei Tage
an der Luft trocknen**, weil sich Schale und braune Samenhaut danach viel
leichter lösen; danach Eichelmehl (glutenfrei, historisch zum **Strecken von
Getreidemehl**) oder Kaffeeersatz (kräftig rösten, grob mahlen, ein gehäufter
Teelöffel je Tasse, innerhalb weniger Wochen aufbrauchen). Und die Unterscheidung
der beiden heimischen Arten: **Stieleiche — Eicheln an langen Stielen;
Traubeneiche — Eicheln traubenartig direkt am Zweig.**

### Drei Einordnungen, die aus dem Paket kommen und nicht aus der Quelle

* **Das Wässern kostet Wasser.** Deshalb lohnen sich Eicheln dort, wo Wasser da
  ist; wo es knapp ist, lohnen sich **Bucheckern** mehr, weil bei ihnen kurzes
  Überbrühen genügt. Die beiden Einträge verweisen aufeinander.
* **Das Brauchwasser muss nicht sauber sein** — es kommt nicht in den Menschen.
  Nur der letzte Durchgang und das Trocknen sollten sauber sein.
* **Das braune Wasser ist Gerbstoffbrühe.** Wer Leder machen will, schüttet es
  nicht weg — siehe „Gerben mit Eichenrinde“. Zwei Ziele, ein Aufwand.

### Zur Suche

„eicheln“, „eiche“, „brotbaum“ und „entbittern“ führen auf den neuen Eintrag.
**Verdrängt wurde nichts** — insbesondere steht „wasser“ unverändert auf den
Wasser-Einträgen, obwohl der neue Eintrag das Wort oft verwendet: Der Titel
wurde bewusst ohne „Wasser“ gewählt, weil das Wort einer ganzen Kategorie
gehört.

Abgesichert in `derEichelTippNenntDieProbeOhneGeraet`.

### Damit ist die Pflanzenliste abgearbeitet — bis auf eine

Giersch, Vogelmiere, Bucheckern und Eicheln sind erledigt. **Knoblauchsrauke**
bleibt offen; dafür ist in dieser Nacht keine gelesene Quelle
zusammengekommen. Ebenso offen: **Gerstenkorn** und **Haus notdürftig
sichern**.

## 17.08.2026 — Gerstenkorn und Hagelkorn

Der vorletzte offene Punkt der Liste. Quelle: **IQWiG,
gesundheitsinformation.de** — die Seiten „Gerstenkorn und Hagelkorn
(Augenlidentzündung)“ und „Was tun bei einem Gersten- oder Hagelkorn?“, selbst
geladen und im Volltext gelesen am 17.08.2026, Sicherung unter
`work/quellen/gerstenkorn/`.

### Die Unterscheidung braucht einen Finger

**Gerstenkorn:** bakteriell (meist Staphylokokken), entsteht schnell, **tut
weh**, Eiter in der Mitte, von außen oft als **gelber Punkt** sichtbar. Öffnet
sich nach **etwa einer Woche** meist von selbst.

**Hagelkorn:** verstopfte Talgdrüse, **keine Bakterien, kein Eiter**, wächst
langsamer, **tut meist nicht weh**, oft auf der Lidinnenseite. Braucht **Wochen
bis Monate** und geht manchmal gar nicht von selbst weg.

Die Probe steht so in der Quelle: **Tut die Schwellung auch auf Druck nicht
weh, spricht das für ein Hagelkorn.**

### Die eine Regel

**Nicht daran herumdrücken.** Die Quelle sagt es an zwei verschiedenen Stellen,
und der Grund steht daneben: Von dort kann sich die Entzündung ausbreiten, bis
in die **Augenhöhle**. Die Quelle nennt das ausdrücklich **sehr selten** — das
ist übernommen, damit der Eintrag keine Angst macht, die nicht hergehört.

### Eine Quelle, die ihre eigene Beweislage nennt

Bemerkenswert und deshalb wörtlich übernommen: Die Quelle zählt die
unterstützenden Maßnahmen auf und schreibt im selben Atemzug, dass es **keine
Studien gibt, die ihren Nutzen belegen**. Der Eintrag sagt das ebenso deutlich
— mit dem Zusatz, dass das nicht heißt, sie schadeten, sondern dass niemand
Wunder erwarten soll. Genau diese Sorte Ehrlichkeit soll das Paket haben.

Auch die kurze **feuchte** Lidrandreinigung ist ausdrücklich als unproblematisch
übernommen. **Bewusst nicht übernommen** wurde die anderswo verbreitete
Behauptung, feuchte Wärme sei zu meiden — die stand in einer
Suchmaschinen-Zusammenfassung einer anderen Seite, und die gelesene Quelle sagt
das Gegenteil. Dritter Fall dieser Nacht, in dem eine ungelesene Behauptung
liegengeblieben ist.

### Der zweite Zweig ist hier ungewöhnlich: er ist beruhigend

An der Behandlung ändert sich fast nichts, weil es fast keine gibt. Drei Dinge
sind ohne Arzt anders, und sie stehen als Einordnung im Eintrag: das **eigene
saubere Tuch** mit abgekochtem Wasser; die **Grenze zum Ernstfall** (das ganze
Auge rot und heiß, das Auge steht vor, die Bewegung des Auges tut weh, das
Sehen wird schlechter, Fieber — dann sitzt die Entzündung hinter dem Lid); und
die **Finger**, weil die Versuchung zum Aufdrücken genau dann am größten ist,
wenn niemand kommt.

### Zur Suche

„gerstenkorn“, „hagelkorn“ und „augenlid“ führen auf den Eintrag. **Verdrängt
wurde nichts:** „druecken“ führt weiterhin zuerst auf die Herzdruckmassage,
„auge“ auf die Augen-Einträge, „eiter“ auf den Abszess. Der Titel wurde
bewusst ohne „Auge“ und ohne „drücken“ gewählt.

Abgesichert in `derGerstenkornTippVerbietetDasDrueckenUndNenntDieGrenze`.

### Von der offenen Liste bleibt

**Knoblauchsrauke** und **Haus notdürftig sichern**.

## 17.08.2026 — Zurück ins beschädigte Gebäude

Der Punkt stand seit Wochen offen als „Haus notdürftig sichern — nur
kommerzielle Seiten gefunden“. Er ist gelöst worden, indem die FRAGE
neu gestellt wurde: Nicht „wie flicke ich ein Dach“, sondern **„darf ich überhaupt
wieder hinein, und in welcher Reihenfolge sehe ich nach“** — und dafür lag die
Quelle seit langem lokal.

Quelle: **FEMA, „Are You Ready?“ (IS-22), Teil 5 „Recovering from Disaster“**,
Abschnitte „Before You Enter Your Home“, „Going Inside Your Home“ und „Being
Wary of Wildlife“. Volltext seit langem unter
`work/quellen/taktisch/fema-are-you-ready-is22.txt`. Werk der
US-Bundesregierung, frei. (Zum wiederholten Mal in dieser Nacht: Die Quelle lag
schon da.)

### Zwei Regeln, die fast niemand kennt

**ERSTENS: In einem beschädigten Gebäude wird nicht mit offener Flamme
geleuchtet** — keine Kerze, keine Petroleumlampe, kein Feuerzeug —, solange
nicht sicher ist, dass kein Gas austritt. Das ist genau die Lage, in der jemand
ohne Strom nach einer Kerze greift.

**ZWEITENS, UND DAS IST DER FUND DES EINTRAGS: Einen vollgelaufenen Keller nur
etwa ein Drittel des Wassers pro Tag auspumpen.** Der Grund steht in der Quelle:
Solange der Boden ringsum vollgesogen ist, drückt er von außen gegen die
Kellerwände, und das Wasser im Keller hält von innen dagegen. Wer alles auf
einmal herauspumpt, nimmt den Gegendruck weg — **die Wände können einstürzen
und der Boden sich aufwölben.**

Wer also endlich eine Pumpe hat und aufatmen will, macht genau in diesem Moment
den teuersten Fehler. Eine Zahl, ein Satz Begründung, und ein Haus bleibt
stehen.

### Der Rest der Reihenfolge

Erst außen herum (Stromleitungen, Gasgeruch, Bauschäden). **Drei Fälle ohne
Betreten:** Gasgeruch; Hochwasser steht noch rings ums Gebäude; es hat gebrannt
und ist nicht freigegeben. Ein vierter kommt beim Hineingehen dazu: **Hitze oder
Rauch beim Betreten → sofort wieder hinaus** (ein Brand schwelt in
Zwischendecken weiter).

Drinnen: Gas → Strom → Bausubstanz („Sieht es so aus, als könnte das Gebäude
einstürzen, wird es sofort verlassen“) → Wasser und Abwasser (**nicht spülen**,
solange die Abwasserleitung nicht sicher heil ist) → Essen wegwerfen, was mit
Hochwasser Kontakt hatte → Schränke langsam öffnen.

Dazu zwei Dinge, an die niemand denkt: **Ein Tresor nach einem Brand wird nicht
geöffnet** — er hält die Hitze stundenlang, und der Inhalt kann beim Öffnen in
Flammen aufgehen. Und **Tiere haben sich in die oberen Stockwerke gerettet** und
bleiben dort, auch wenn das Wasser weg ist — nicht in die Enge treiben, nicht
anfassen, sondern einen Weg nach draußen freimachen.

### Was der zweite Zweig hier leisten muss

Die Quelle schickt im Zweifel einen Bausachverständigen. Kommt keiner, muss die
Frage trotzdem beantwortet werden. Der Eintrag nennt deshalb — ausdrücklich als
Einordnung — was ein Laie sehen kann: Risse DURCH tragende Wände statt nur
durch den Putz; Wände nicht mehr im Lot; **Türen und Fenster, die plötzlich
klemmen** (ein Rahmen verzieht sich, wenn das Gebäude arbeitet); durchgebogene
Decken; federnder Boden; rieselnder Staub oder Knacken im Gebälk.

Und die Abwägung, die alles zusammenhält: **Eine Nacht draußen im Kalten ist in
aller Regel überlebbar. Ein Einsturz ist es nicht.**

### Zur Suche

„gebaeude“, „tresor“, „auspumpen“ und „riss“ führen auf den Eintrag; bei
„hochwasser“, „erdbeben“ und „einsturz“ steht er auf Platz 2 hinter dem jeweils
richtigen Haupteintrag. **Verdrängt wurde nichts:** „haus“, „keller“ und „gas“
stehen unverändert.

Abgesichert in `derRueckkehrTippNenntDieZweiUnbekanntenRegeln`.

### Damit bleibt aus der Nacht-Liste nur noch

**Knoblauchsrauke.**

## 17.08.2026 — Knoblauchsrauke: der letzte offene Punkt der Nacht

Quelle: **LfL, Merkblatt „Essbare Wildkräuter — Knoblauchsrauke“** von Jutta
Kotzi, IAB, 11/2022, lfl.bayern.de, im Volltext gelesen am 17.08.2026,
Sicherung unter `work/quellen/wildkraeuter/`.

### Der Haken am Erkennungszeichen

Jeder sagt „sie riecht nach Knoblauch“. Die Quelle sagt genauer, WANN:

> Der typische Geruch entsteht **erst, wenn die Zellen durch Kauen oder
> Zerreiben verletzt werden**. Das enthaltene Senfölglycosid wird gespalten und
> das an Knoblauch erinnernde Senföl wird frei.

Damit ist erklärt, warum man an ihr vorbeigeht: **Sie riecht nicht von selbst.**
Der Eintrag macht daraus eine Handlung — Blatt nehmen, zwischen zwei Fingern
zerreiben, riechen.

### Zweijährig, also zwei Erscheinungsformen

Ebenfalls aus der Quelle und ebenfalls eine typische Verwechslungsfalle mit sich
selbst: **Im ersten Jahr nur nierenförmige Blätter am Boden**, im zweiten ein
aufrechter, **schwach kantiger Stängel bis 100 cm mit herzförmigen, gekerbten
Blättern**. Wer das nicht weiß, hält sie für zwei Pflanzen. Dazu weiße Blüten
in Trauben (April–Juni) und danach die **vom kahlen Stängel abstehenden Schoten
mit schwarzen Samen** — ein Merkmal, das auch nach der Blüte noch trägt.

Verwendbar sind **Blätter, Blüten und Samen**.

### Die Grenze des Geruchstests, als Einordnung gekennzeichnet

Der Eintrag sagt ausdrücklich: **Der Test sagt „das hier riecht nach
Knoblauch“, nicht „das hier ist essbar“.** Der Geruch bestätigt eine Pflanze,
deren Blätter und Wuchs schon passen — er ersetzt das Hinsehen nicht. Das ist
nötig, weil ein Geruchsmerkmal sonst schnell als allgemeine Prüfmethode
missverstanden wird, und die NABU-Quelle vom Giersch-Eintrag hält ausdrücklich
fest, dass Geruch und Aussehen stark vom Standort abhängen.

### Zur Suche

„knoblauchsrauke“, „zerreiben“, „wuerze“ und „schoten“ führen auf den Eintrag.
**Verdrängt wurde nichts:** „lauch“ führt weiter zuerst auf die Sammelliste,
„senf“ auf die Agrikultur-Einträge, „riecht“ auf den Darmverschluss (wo der
Geruch ein Krankheitszeichen ist).

Abgesichert in `derKnoblauchsraukeTippBegrenztDenGeruchstest`.

### Die Nacht-Liste ist damit leer

Giersch, Vogelmiere, Bucheckern, Eicheln, Knoblauchsrauke, Windpocken,
Gürtelrose, Hexenschuss, Gerstenkorn und „Haus notdürftig sichern“ sind
abgearbeitet. Was neu dazukam und offen bleibt, steht in den Abschnitten oben —
vor allem der Volltextabgleich der übrigen kurzfassungsgestützten Einträge, die
NICHT von den ERC-Leitlinien abgedeckt werden (v. a. die 15 DFV-Merkblätter).

## 17.08.2026 — Selbstprüfung am Ende der Nacht

Nach elf neuen und einem Dutzend geänderter Einträge ist der wichtigste davon
noch einmal Satz für Satz gegen den Quelltext gelesen worden — „Stabile
Seitenlage: Handgriffe“. Drei Ungenauigkeiten gefunden und behoben:

* **„Knie anheben“ → „Knie VORSICHTIG anheben“.** Die Quelle sagt „gently lift“;
  das war beim Übersetzen verlorengegangen.
* **„Gesicht leicht nach unten“ → „Gesicht ZUR SEITE UND NACH UNTEN“.** Die
  Quelle sagt „facing downwards to the side“. Die Seitwärts-Richtung ist der
  Punkt, an dem die Flüssigkeit abläuft.
* **Die Herzdruckmassage folgt nicht automatisch.** Der Satz las sich so, als
  werde nach dem Zurückdrehen immer gedrückt. Die Quelle sagt „and, if
  required, CPR initiated“. Jetzt steht da, dass am Rücken noch einmal geprüft
  wird, mit Verweis auf „Bewusstlos: atmet oder atmet nicht“.

Keiner der drei Punkte war falsch genug, um zu schaden — aber in einem Paket,
das mit „Qualität vor Zeit“ anfängt, ist „nicht falsch genug“ kein Maßstab.

### Und eine Messung, die eine Änderung VERHINDERT hat

Am Gerät fiel auf, dass die neuen Einträge lange Großbuchstaben-Passagen
enthalten. Bevor daran etwas geändert wurde, ist gemessen worden, wie lang
solche Passagen in den BESTEHENDEN Einträgen sind:

| | Anzahl | Median | Längste |
|---|---|---|---|
| bestehende Einträge | 80 | 81 Zeichen | 152 |
| die neuen von heute Nacht | 25 | 80 Zeichen | 139 |

**Die neuen liegen exakt im Rahmen des Hausstils.** Also nichts geändert. Eine
Umstellung hätte den Stil des Pakets uneinheitlich gemacht, ohne irgendetwas zu
verbessern — und hätte die Wächtertests gebrochen, die auf diesen
Großbuchstaben-Sätzen sitzen.

Geändert wurde nur ein einzelner Satz in der Knoblauchsrauke, der am Gerät
schlecht las („Aber, UND DAS IST DER PUNKT …“).

## 17.08.2026 — Geklärt: Beatmung zuerst nach Rauchgas?

Eine Frage, die seit Wochen als offener Punkt in der ROADMAP stand und die
bewusst nicht eigenmächtig entschieden worden war:

* Das DFV-Merkblatt „Brandgasinhalation“ (2015) sagt für den
  erstickungsbedingten Stillstand „zunächst zwei Mal beatmen“.
* Der Eintrag „Wiederbelebung Erwachsener: drücken, nicht beatmen“ sagt für
  ungeübte Helfer das Gegenteil.

Der Vermerk hielt fest, es wäre falsch herum, eine neuere Primärquelle durch
eine ältere Sekundärquelle zu ersetzen — aber die Frage sei echt, weil das
Paket die Ausnahme für Kinder bereits kennt. **Jetzt im ERC-Volltext 2025
nachgeschlagen.**

### Was dort steht

> „If you are not trained to provide rescue breaths, perform continuous chest
> compressions, without interruptions.“

Und in der Begründung:

> „The ERC supports the ILCOR recommendations that chest compressions are
> performed **for all adults** in cardiac arrest. … If they are not trained,
> able or willing they should deliver CCC.“

**Die Leitlinie macht die Ausnahme also nicht.** Die zwei Ausnahmen, die es
2025 belegt gibt, kennt das Paket bereits: **Kinder** (Kinder-Leitlinie) und
**Ertrinken** (Erste-Hilfe-Leitlinie, fünf Atemspenden zuerst, auch beim
Erwachsenen).

### Wie es gelöst wurde — ohne die Frage zu entscheiden

Der Eintrag nennt jetzt **beide** Sätze offen: den der Leitlinie und den des
Feuerwehr-Merkblatts. Der Grund, das ältere überhaupt zu erwähnen: Es ist im
Umlauf, und wer es kennt, glaubt sonst, dieses Paket verschweige etwas.

Und dann die Regel, die den Streit gar nicht braucht — sie steht so in der
Leitlinie selbst: **Wer beatmen kann und will, beatmet (30:2). Wer es nicht kann
oder sich nicht traut, drückt.** Beides ist richtiger als das Zögern dazwischen.

### Was ausdrücklich als Grenze dasteht

Die BLS-Leitlinie verweist für **Sonderlagen** — dazu zählt die Vergiftung —
auf ein eigenes Kapitel („Special Circumstances in Resuscitation“), **das für
dieses Paket nicht vorlag.** Der Eintrag sagt das und sagt, dass ein dortiger
abweichender Satz Vorrang hätte. Das ist die ehrliche Fassung; die Alternative
wäre gewesen, so zu tun, als sei die Sache abschließend geprüft.

Dazu der Satz, der bei Rauch und Gas ohnehin vor allem anderen steht: der
Selbstschutz — mit Verweis auf „Eigenschutz geht vor“ und „Rauch im Haus:
nicht hineingehen“.

Abgesichert in `derNurDrueckenTippLegtDenRauchgasStreitOffen`.
ROADMAP-Punkt auf erledigt gesetzt.

## 17.08.2026 — Der zweite Zweig: gemessen statt geschätzt

Die ROADMAP führt seit dem 28.07.2026 den Punkt „Was steht in einem Tipp, wenn
niemand kommt? **23** der Erste-Hilfe-Tipps enden bei ‚Notruf‘“. Diese Zahl ist
nachgemessen worden:

| | |
|---|---|
| Erste-Hilfe-Einträge gesamt | 156 |
| davon mit Verweis auf Notruf, Rettungsdienst oder Arzt | 82 |
| davon **ohne** zweiten Zweig | **20** |

Also drei weniger als im alten Vermerk — der Punkt ist in Arbeit, nicht liegen
geblieben.

### Nicht jeder der 20 braucht einen

Die Durchsicht zeigt zwei Sorten. **Erkennungs- und Zwischenschritte**
(„Bewusstsein prüfen“, „Ersticken erkennen“, „Vergiftung erkennen“, die
Erstickungs-Kette) reichen an einen Behandlungs-Eintrag weiter, und DER hat den
Zweig — bei der Erstickungs-Kette zum Beispiel „Ersticken beim Erwachsenen:
Bauchstöße“. Ein zweiter Zweig in jedem Zwischenschritt wäre Wiederholung, kein
Gewinn.

**Echte Lücken** sind die Einträge, deren eigene Behandlung an einer Hilfe
endet, die nicht kommt. Der klarste Fall war der **Grabenfuß**: ein Eintrag, der
tagelange Pflege ohne jede Klinik beschreibt — und dann bei „Stirbt Gewebe ab,
muss alles versucht werden, ärztliche Behandlung zu erreichen“ aufhört.

### Was jetzt dort steht

**Es wird nichts weggeschnitten.** Abgestorbenes Gewebe gibt sich mit der Zeit
selbst zu erkennen; wer vorher schneidet, trifft Lebendes in einem ohnehin
schlecht durchbluteten Fuß. Verwiesen wird auf „Totes Gewebe erkennen: vier
Prüfungen“ — der Eintrag, der ausdrücklich sagt, dass der Nutzen für einen
Laien im ERKENNEN liegt und nicht im Schneiden.

**Die Entscheidung fällt nicht am Fuß, sondern am Fieber.** Solange die Stelle
nur hässlich ist, ist es eine zähe Heilung; kommen Fieber, Schüttelfrost,
schneller Puls oder Verwirrtheit dazu, ist es keine Fußsache mehr — Verweis auf
den Sepsis-Eintrag. Das ist der Punkt, an dem ein weiter Weg zur Klinik sich
wieder lohnt, auch wenn er vorher aussichtslos schien.

Dazu die tägliche Arbeit (locker und TROCKEN verbinden, nichts einweichen), die
Gruppenentscheidung (wochenlang nicht gehfähig — das gehört laut ausgesprochen)
und der Satz, der als Einziger wirklich etwas ändert: **Ohne Nachschub wird die
Vorbeugung nicht unwichtiger, sondern zur täglichen Arbeit** — zweites Paar
Socken im Wechsel, das feuchte am Körper getrocknet, abends nackte Füße.

Abgesichert in `derGrabenfussTippBeantwortetDasEndeOhneArzt`.

### Die Liste für den nächsten Durchgang

Nach derselben Prüfung sind noch echte Kandidaten: **Lawine (verschüttet)** und
**Eiseinbruch (retten)** — in beiden ist der Begleiter die Rettung, wenn niemand
kommt —, **Unterkühlung Stadium II**, **mehrere Verletzte** (Sichtung ist gerade
dann nötig, wenn kein Rettungsdienst sortiert) und **Austrocknung erkennen**.
Der Rest der 20 sind Zwischenschritte und braucht keinen.

## 17.08.2026 — Der wichtigste zweite Zweig: schwere Austrocknung ohne Infusion

Beim Durchgehen der Liste von oben stellte sich heraus, dass zwei der fünf
Kandidaten gar keinen zweiten Zweig brauchen — **Lawine** und **Eiseinbruch**
sind von der ersten bis zur letzten Zeile Rettung durch die eigene Gruppe; sie
erwähnen den Rettungsdienst nur, um zu sagen, dass er nicht rechtzeitig da ist.
Dasselbe gilt für **mehrere Verletzte** und **Unterkühlung Stadium II**. Meine
Zählung von vorhin war insoweit zu grob; das ist hier korrigiert.

**Ein echter Fall blieb übrig, und er war der schwerste der ganzen Kategorie.**

### Was da stand

„Austrocknung bei Durchfall erkennen“ endete so:

> Das ist ein Notfall: Notruf 112. Trinken allein reicht dann nicht mehr; die
> WHO nimmt die schwere Austrocknung ausdrücklich von dem aus, was sich durch
> Trinken beheben lässt.

Das ist richtig — und es beschreibt die Lage MIT Klinik. Für jemanden ohne
Klinik heißt dieser Satz: **aufhören.** Genau das ist die gefährlichste Wirkung,
die ein wahrer Satz haben kann.

### Was dieselbe Quelle für den Fall ohne Infusion sagt

Tafel 13 des WHO-Kinderhandbuchs („Diarrhoea treatment plan C“) ist ein
Entscheidungsbaum, und er endet NICHT bei „ohne Infusion geht nichts“. Er fragt
der Reihe nach: Infusion sofort möglich? Klinik in 30 Minuten erreichbar?
Magensonde möglich? — und dann:

> **„Start rehydration by tube (or mouth) with ORS solution: give 20 ml/kg per h
> for 6 h (total, 120 ml/kg).“**

**Der Mund steht dort ausdrücklich neben der Sonde.** Bei schwerer Austrocknung,
wenn keine Infusion möglich ist und die Person schlucken kann, ist das die
Behandlung der WHO — nicht ein Notbehelf, den sich jemand ausgedacht hat.

Mit übernommen sind die drei Kontrollpunkte der Tafel: **alle 1–2 Stunden
nachsehen**; bei **wiederholtem Erbrechen oder pralleren Bauch langsamer geben,
nicht aufhören**; und wenn sich **nach 3 Stunden nichts bessert**, braucht es
eine Infusion — der Punkt, an dem ein weiter Weg zur Klinik sich wieder lohnt.

Die Mengen stehen mit dem üblichen Vorbehalt („Alle Angaben ohne Gewähr“) und
mit dem Hinweis, dass sie aus dem KINDER-Handbuch stammen; für Erwachsene nennt
dieselbe Quelle keine eigene Zahl, der Weg ist derselbe und man rechnet mit dem
Gewicht.

### Und die Grenze bleibt

**Wer nicht klar bei Bewusstsein ist, bekommt nichts in den Mund.** Für diese
Person gibt es ohne Infusion oder Magensonde keinen Weg. Das steht so im
Eintrag — und es ist der Grund, warum ganz oben steht, dass bei Durchfall von
Anfang an mehr getrunken wird, lange bevor ein Zeichen auftritt.

Abgesichert in `derAustrocknungsTippLaesstNiemandenAufgeben`.

## 17.08.2026 — der Einwand zur Wiederbelebung ohne Rettungsdienst: geklärt

Der Vermerk stand seit dem 28.07.2026 in der ROADMAP und war als „der
schwerste Einzelfall“ bezeichnet:

> Die Regel „ohne Übung nur drücken“ geht stillschweigend davon aus, dass
> Rettungskräfte kommen und übernehmen. Trifft das nicht zu, ändert sich die
> Abwägung möglicherweise — ebenso bei „wann darf man aufhören“.

Im ERC-Volltext 2025 nachgeschlagen. **Drei Ergebnisse, davon eines neu.**

### Erstens: Die Regel hängt nicht an der Übergabe

Der Text sagt „chest compressions are performed **for all adults** in cardiac
arrest“ und macht die Empfehlung an der Fähigkeit des Helfers fest, nicht an
der Aussicht auf Übergabe. Der Einwand trifft an dieser Stelle also nicht zu —
die Regel ist nicht deshalb so, weil jemand kommt.

### Zweitens: „Wann darf man aufhören“ war schon beantwortet

„Wiederbelebung: wie lange weitermachen“ hat den zweiten Zweig bereits, samt
dem Satz, dass die Entscheidung dann beim Helfer bleibt und erlaubt ist.

### Drittens, und das war neu: Die Leitlinie denkt den Helfer ohne Telefon mit

Das stand nirgends im Paket, und es ist der Regelfall dieser App:

> „In these circumstances, a lone rescuer has two options — shout for help or
> leave the person in cardiac arrest to alert local emergency services. …
> **There is currently no evidence addressing how long to continue CPR before
> leaving the person** … the ERC advises that this is done as quickly as
> possible.“

Übernommen in „Notruf 112“:

* **Wenn möglicherweise jemand in der Nähe ist:** rufen UND sofort anfangen —
  der bessere Weg, weil dabei keine Zeit verlorengeht.
* **Antwortet niemand:** unterbrechen und losgehen, dann so schnell wie
  möglich.
* **Und die offene Beweislage wörtlich:** Es gibt keine Untersuchung dazu, wie
  lange man vorher wiederbeleben soll. Wer hier eine Zahl nennt, hat sie
  erfunden — das steht so im Eintrag, weil eine erfundene Zahl schlimmer wäre
  als keine.

### Und die Folgerung für dieses Paket

Die ganze Abwägung hängt an einer Annahme: dass es irgendwo Hilfe GIBT, die man
holen kann. Fällt die weg, fällt die Abwägung mit — **wer niemanden holen kann,
geht nicht los.** Das Weglaufen kostet dann das Einzige, was wirkt.

Dazu der Rat, diese Frage VOR den Ernstfall zu ziehen: Wer vorher weiß, ob in
seiner Lage überhaupt jemand erreichbar ist, muss es nicht mit blutigen Händen
entscheiden.

Abgesichert in `derNotrufTippDenktDenEinzelnenHelferOhneTelefonMit`.
ROADMAP-Punkt auf erledigt gesetzt.

## 17.08.2026 — Unterkühlung bei den Kleinsten: das Maß, das nicht existiert

Aus der ROADMAP-Liste „Kinder-Lücken: Quellen gesichert, Einarbeitung offen“
(Stand 28.07.2026) ist der erste Punkt eingearbeitet. Quelle:
**Bundesarbeitsgemeinschaft Mehr Sicherheit für Kinder (BAG),
kindersicherheit.de, „Unterkühlung und Erfrieren“**, Stand 20.06.2024, im
Volltext gelesen, Sicherung unter
`work/quellen/medizin-luecken/kinder-unterkuehlung/`.

### Warum das eine echte Lücke war und keine Ergänzung

Die drei Unterkühlungs-Einträge des Pakets sind nach Stadien gebaut, und die
Stadien hängen an EINEM Zeichen: Zittern — und daran, wann es **aufhört**
(„Stadium I: Person zittert noch“, „Stadium II: das Zittern hört auf“).

> „Besonders Säuglinge können noch nicht durch Zittern Wärme erzeugen.“

**Für ein Baby gibt es dieses Zeichen also nie.** Wer die Stadien anwendet und
auf das Aufhören des Zitterns wartet, wartet auf etwas, das nicht kommt — und
hält ein Kind, das schon im zweiten Stadium ist, für unauffällig. Das ist die
gefährlichste Bauart Fehler, die dieses Paket haben kann: eine richtige Regel,
auf den falschen Menschen angewandt.

### Was an die Stelle tritt

**Blässe oder Blauverfärbung der Haut** — bei älteren Kindern zusätzlich
Kältegefühl und Zittern wie beim Erwachsenen. Und, wenn ein Thermometer da ist,
**die Messung im Po mit der Schwelle 36 °C**: bewusst niedrig, sie soll nicht
Alarm auslösen, sondern das Hinsehen.

Dazu: **Unterkühlungen entstehen bei Kindern viel schneller als bei
Erwachsenen**, und der häufigste Weg ist unspektakulär — **feuchte Kleidung bei
kaltem Wetter.**

### Zwei Erfrierungsursachen, an die niemand denkt

Die Quelle nennt sie beim Namen, und beide sind hausgemacht: **abschnürende
Schuhe** und **fehlerhaftes Angurten in Tragegestellen**. Ein getragenes Kind
bewegt sich nicht und sagt nichts. Übernommen ist auch die Grenze beim
Aufwärmen, die die Quelle am Kind selbst festmacht: **Die Wassertemperatur darf
für das Kind nie unangenehm oder schmerzhaft sein.**

Und das Alkoholverbot samt Begründung: kurzfristig etwas Wärme, aber erweiterte
Gefäße und vertiefte Atmung — die Körpertemperatur sinkt dadurch **weiter**.

### Der Titelwächter hat zweimal zugeschlagen

Der erste Titel hieß „Unterkühlung beim **Kind**: das Zittern fehlt“. Der Test
`keinNeuerTitelKapertEinReserviertesWort` hat ihn abgelehnt — „kind“ gehört
acht Notfall-Einträgen, und gemessen stand der neue Eintrag bei der Anfrage
„kind“ bereits auf Rang 3 und drückte „Herzdruckmassage bei Kindern und
Säuglingen“ aus den ersten fünf. Titel geändert auf **„Unterkühlung bei den
Kleinsten“**; danach steht „kind“ wieder genau wie vorher.

Und `keinAusgeschriebenerVerweisSchlepptEinReserviertesWortEin` hat gefangen,
dass der ausgeschriebene Verweis „Wiederbelebung Kind: zuerst beatmen“ das
reservierte Wort **„beatmen“** in den neuen Eintrag trug. Der Verweis ist jetzt
ohne Titelzitat formuliert.

Zwei gefangene Fehler in einem Eintrag — beide hätten die Suche still
verschlechtert, und beide wären von Hand nicht aufgefallen.

Abgesichert in `derKinderUnterkuehlungsTippNimmtDasZitternAlsMassstabWeg`.

### Von den vier Kinder-Lücken bleiben drei

Offen: **Hitze bei Kindern** (Trinkmengen), **Trinkwasserbedarf nach Alter**
(DGE) und **Blutverlust bei Kindern** — für den letzten hielt schon die
Recherche vom 28.07. fest, dass es keinen Laien-Schwellwert gibt.

## 17.08.2026 — Wasserbedarf nach Alter: die Zahl, die die Einteilung umdreht

Zweiter Punkt aus der Kinder-Liste. Quelle: **DGE, Referenzwerte für die
Nährstoffzufuhr, „Richtwerte für die Wasserzufuhr“**, dge.de, im Volltext
gelesen, Sicherung unter `work/quellen/medizin-luecken/kinder-trinkwasser/`.

Der Eintrag „Trinkwasser: Bedarf pro Tag“ sagte bisher nur „bei Kindern kann der
Bedarf höher liegen“ — ohne Zahl. Das ist zu ungenau, wenn Wasser wirklich
eingeteilt werden muss.

### Der Befund, der die Einteilung umdreht

**Je Kilogramm Körpergewicht braucht ein Säugling fast VIERMAL so viel Wasser
wie ein Erwachsener: 130 ml/kg gegen 35.** Kleinkind 95, Schulkind 60,
Jugendlicher 40.

In LITERN braucht ein kleines Kind weniger — aber es hat auch fast keinen
Vorrat im Körper. Daraus folgt der Satz, der im Eintrag steht: **Wer Wasser
einteilt, kürzt nicht beim Kleinsten zuerst.**

Und der zweite, der übersehen wird: **Eine Stillende hat den höchsten Wert der
ganzen Tabelle** — 3100 ml gesamt, 1710 ml als Getränk. Wenn sie zu wenig
bekommt, trifft es zwei.

Die vollständige Tabelle steht im Eintrag, jeweils mit dem, was man wirklich zu
trinken geben muss, und der Gesamtmenge einschließlich des Wassers im Essen.

### Wann die Werte nicht mehr gelten

Die Quelle nennt die Umstände mit erhöhtem Bedarf, und die Liste liest sich wie
eine Beschreibung der Lage, für die dieses Paket gemacht ist: hoher
Energieumsatz, Hitze, **trockene kalte Luft**, viel Kochsalz, viel Eiweiß,
Fieber, Erbrechen, Durchfall.

**Die trockene kalte Luft ist der Punkt, den man im Winter vergisst**, weil man
nicht schwitzt — deshalb steht sie im Eintrag hervorgehoben.

Ausdrücklich als Einordnung gekennzeichnet: Das sind Referenzwerte für ein
normales Leben, keine Überlebens-Mindestmengen. Die Planungsgröße von zwei
Litern je Person bleibt; die Tabelle sagt nicht, dass man mehr braucht, sondern
**wie man verteilt, wenn nicht für alle genug da ist.**

### Nebenbefund: der Eintrag war schlecht auffindbar

Gemessen: „tagesbedarf“ führte auf `wasser-pflanzenbeutel` statt auf
„Trinkwasser: Bedarf pro Tag“ — obwohl der Eintrag genau so heißt. Grund: Die
Suche vergleicht Wortanfänge, und „Tagesbedarf“ als ein Wort kam nur im anderen
Eintrag vor. Vier Schlagwörter ergänzt („tagesbedarf“, „wasserbedarf nach
alter“, „wie viel am tag trinken“, „wie viel muss ein kind trinken“). Danach
gemessen: „tagesbedarf“ und „wasserbedarf“ führen auf den richtigen Eintrag,
und **verdrängt wurde nichts** — „wasser“, „kind“ und „trinken“ stehen
unverändert.

Abgesichert in `derWasserbedarfsTippSagtWoNichtGekuerztWird`.

### Von den vier Kinder-Lücken bleiben zwei

**Hitze bei Kindern** (Trinkmengen) und **Blutverlust bei Kindern** — für den
zweiten hielt schon die Recherche vom 28.07. fest, dass es keinen
Laien-Schwellwert gibt.

## 17.08.2026 — Hitze bei den Kleinsten: die Zahl, die eine Vorratsplanung umwirft

Dritter Punkt aus der Kinder-Liste, und damit ist sie bis auf einen abgearbeitet.
Quelle: **DGUV, Magazin „KinderKinder“, „Kühler Kopf bei Hitze“** mit Aussagen
von Dr. Johannes Nießen (BIPAM/BZgA), im Volltext gelesen; Sicherung unter
`work/quellen/medizin-luecken/kinder-hitze/`.

### Warum es bei Kleinen so schnell geht — drei Gründe in einem Satz

Ihr eigenes Kühlsystem arbeitet noch nicht richtig (Schwitzen und Verdunsten);
sie haben einen **höheren Stoffwechsel**; und im Verhältnis zum Körpergewicht
**mehr Haut**. Die Quelle zieht daraus selbst den Schluss: besonders
empfindlich — **und sie trocknen schneller aus.**

### Die Zahl

> **Bei Temperaturen um die 30 Grad werden für Zwei- bis Sechsjährige täglich
> 1,5 bis 3 Liter Flüssigkeit empfohlen.**

Gegen die DGE-Referenzwerte von heute Nacht gehalten (1–4 Jahre: 820 ml als
Getränk, 4–7 Jahre: 940 ml) heißt das: **Hitze verdoppelt bis verdreifacht den
Bedarf eines kleinen Kindes.** Bei zwei kleinen Kindern sind das bis zu sechs
Liter am Tag, bevor irgendjemand sonst getrunken, gekocht oder sich gewaschen
hat. Wer für eine Hitzewoche plant und mit gewöhnlichen Mengen rechnet, plant
genau dort zu knapp, wo am wenigsten Spielraum ist.

### Das erste Zeichen läuft gegen die Erwartung

> „Erste Anzeichen für die Überhitzung eines Kleinkinds können ein stark
> gerötetes Gesicht, großer Durst und **kühle Haut** sein.“

Man greift also an ein Kind, das glüht, fühlt kühle Haut und hält es für
harmlos. Dazu die Probe ohne Gerät: **die Stelle zwischen den Schulterblättern
unterhalb des Nackens befühlen — warm, aber nicht verschwitzt.** Nicht Hände,
nicht Füße.

### Und drei Maßnahmen, die ohne Strom funktionieren

Die Quelle nennt auch Klimaanlage und Ventilator. Ohne Strom bleiben:
**Außenjalousien tagsüber geschlossen** (außen liegender Schatten schlägt jeden
Vorhang innen), **früh morgens weit öffnen und stoßlüften**, und **heller
Anstrich für Dächer und Wände**. Als Einordnung dazu: der kühlste Ort im Haus
ist meist der unterste.

Ebenfalls übernommen, weil es beim Trinken hilft: die Saftschorle **ein Teil
Saft auf drei Teile Wasser**, Melone und Gurke zählen mit, und alles muss
**griffbereit** stehen.

### Der Suchtest hat einen ernsten Fehler gefangen

Der Entwurf hatte das Schlagwort „schatten und lüften“. Damit stand der neue
Eintrag bei der Anfrage **„lüften“ auf Platz 1 — vor
„Kohlenmonoxid: was zu tun ist“.** Bei einer CO-Vergiftung ist Lüften die
lebensrettende Handlung; ein Hitze-Tipp davor ist genau die Sorte lautlose
Verschlechterung, gegen die der Test
`dieCoMelderTippsVerdraengenKeinenNotfallTipp` geschrieben wurde. Schlagwort
ersetzt, danach steht „lüften“ wieder richtig.

Zweite Rücknahme aus derselben Runde: Der erste Titel hieß „Hitze bei den
Kleinsten …“ und nahm damit „Hitze: vorbeugen …“ den ersten Platz bei „hitze“.
Titel geändert auf „Die Kleinsten im Sommer“; danach stimmt die Reihenfolge.

Abgesichert in `derHitzeTippFuerKleineNenntZahlUndDasFalscheZeichen`.

### Von den vier Kinder-Lücken bleibt eine

**Blutverlust bei Kindern** — und dafür hielt schon die Recherche vom
28.07.2026 fest, dass es **keinen Laien-Schwellwert** gibt, sondern nur
Prozent-Klassen und das Warnzeichen „Blutdruck fällt spät“. Das ist damit
weniger eine offene Aufgabe als ein festgehaltener Negativbefund.

## 17.08.2026 — Eingewachsener Zehennagel

Gefunden über einen Lückenabgleich gegen übliche Alltagsnotfälle: Von
dreiunddreißig geprüften Stichwörtern fehlten zwölf, und dieses war das
nützlichste davon — **wer nicht mehr gehen kann, ist in einer Krise ein anderer
Mensch.**

Quelle: **US Special Operations Forces Medical Handbook, 1. Auflage 2001**,
Teil 5, Kapitel 9 „Podiatry“, Abschnitt „Ingrown Toenail“ von CDR Raymond
Fritz — Werk der US-Bundesregierung, frei, Volltext seit langem lokal.

### Warum das kein Kleinkram ist

**Sobald die Haut aufgeht, wirkt die Nagelecke wie ein Fremdkörper** und
schleppt Erreger hinein. Und die Quelle nennt die Folge einer vernachlässigten,
chronischen Entzündung beim Namen: **eine Knochenentzündung.** Aus einem Zeh,
den man monatelang laufen lässt, wird eine Entzündung im Knochen darunter —
ohne Klinik nicht mehr zu beherrschen.

### Der Grund, warum dieser Eintrag überhaupt geschrieben werden konnte

Die Quelle nennt neben dem ärztlichen Vorgehen ausdrücklich eine einfache
Fassung: **„Lift side of nail corner and remove with small scissors.“** Damit
gibt es hier — anders als beim Abszess, wo das Paket zum Nichtstun rät — einen
Handgriff, der ohne Ausbildung machbar ist und den die Quelle selbst so
vorsieht.

Dazu die Nachbehandlung, die über Tage geht: Fuß hochlegen, **dreimal täglich
warme Umschläge**, und beim Einweichen **die lose abgestorbene Haut oder den
Schorf mit einem Waschlappen abnehmen**, damit der Eiter abfließt.

### Der Satz, der einen Rückfall verhindert

Die Quelle lässt nach **drei bis fünf Tagen** noch einmal nachsehen, und zwar
gezielt nach **kleinen, nadelförmigen Nagelsplittern**, die stehen geblieben
sind. Genau die sind der Grund, warum dieselbe Zehe drei Wochen später wieder
eitert. Das steht sonst nirgends und ist der Unterschied zwischen einmal und
fünfmal.

Und die Vorbeugung, die die Quelle in Großbuchstaben schreibt: **Nägel immer
gerade abschneiden.**

### Eine Warnung, die über diesen Eintrag hinausgeht

Die Quelle schreibt zur Betäubung ausdrücklich **„no epinephrine for digits“**.
Das ist übernommen, weil es für jeden gilt, der überhaupt ein Betäubungsmittel
hat: **An Fingern und Zehen niemals eines mit Adrenalin** — dort gibt es keinen
Umweg für das Blut.

### Als Einordnung gekennzeichnet

Dass die örtliche Betäubung meist fehlt und wie man ohne sie zurechtkommt
(warmes Bad vorher, jemand hält den Fuß); die Grenze, ab der es keine
Nagelsache mehr ist (Streifen den Fuß hinauf, Fieber, zunehmende Schwellung —
Verweis auf Wundrose und Sepsis); und dass bei Zuckerkranken alles strenger
gilt, weil Durchblutung und Gefühl schlechter sind.

### Zur Suche

„eingewachsen“, „nagel“ und „zeh“ führen auf den Eintrag. **Verdrängt wurde
nichts:** „eiter“ führt weiter zuerst auf den Abszess, „fuß“ auf die Blase,
„schuh“ auf die Schuhherstellung.

Abgesichert in `derNagelTippNenntFolgeHandgriffUndDenRueckfallgrund`.

### Was der Lückenabgleich sonst noch zeigte

Ebenfalls nicht abgedeckt und als Kandidaten festgehalten:
**Nagelbettentzündung am Finger** (dafür fand sich keine Quelle im Bestand),
**Fremdkörper im Ohr**, **Wadenkrampf**, **Madenwurm**, **Sonnenallergie**.
Nicht nötig: Fremdkörper im Auge (steht bereits ausführlich in
„Augenverletzung: abschirmen, nicht draufdrücken“).

## 17.08.2026 — Madenwurm: der Kreislauf, den man ohne Mittel unterbricht

Zweiter Punkt aus dem Alltagsnotfall-Abgleich. Quelle: **SOF Medical Handbook
2001, „ID: Enterobiasis (Pinworm)“** von LTC Glenn Wortmann — freies Werk,
Volltext seit langem lokal.

### Warum das in dieses Paket gehört

Die Quelle sagt: weltweit, **besonders in gemäßigten Klimazonen**, und **häufig
bei Kindern**. Ansteckung durch **Verschlucken der Eier**. Für eine
Notunterkunft heißt das: Ist einer betroffen, sind es bald mehrere. Das ist
keine Frage von Schmutz, sondern von vielen Menschen auf engem Raum — und der
Eintrag sagt das im ersten Satz, damit niemand aus Scham schweigt.

### Zwei Sätze, an denen die Behandlung fast immer scheitert

**Erstens: Die Gabe wird nach zwei Wochen wiederholt.** Die Quelle schreibt es
bei allen drei genannten Mitteln dazu. Grund: Das Mittel tötet die Würmer, nicht
die Eier.

**Zweitens: Alle im Haushalt werden behandelt**, nicht nur der, bei dem es
aufgefallen ist. Sonst steckt die Familie den Behandelten wieder an.

Mengen stehen bewusst NICHT im Eintrag: Sie hängen am Gewicht und am
tatsächlich vorhandenen Mittel. Verwiesen ist auf „Alle Angaben ohne Gewähr“.

### Der Teil, der den Eintrag trägt: ohne Mittel

Als Einordnung gekennzeichnet, weil die Quelle nur die Medikamente und das
heiße Waschen nennt. Zuerst die Entwarnung, die hierher gehört: **Madenwürmer
sind unangenehm, aber nicht gefährlich.** Was sie kosten, ist Schlaf — und in
einer Krise ist Schlaf keine Kleinigkeit.

Dann der Kreislauf, den man unterbricht: **Eier werden nachts an der Haut
abgelegt → es juckt → man kratzt im Schlaf → Eier unter den Nägeln → in den
Mund.** Sechs Ansatzpunkte, davon zwei entscheidend:

* **Fingernägel kurz schneiden, bei allen** — der wirksamste einzelne Handgriff.
* **Morgens als Erstes waschen**, bevor die frisch abgelegten Eier
  weiterwandern.
* Dazu Händewaschen, nachts etwas anziehen, das das Kratzen erschwert, heiß
  waschen — **und Bettzeug NICHT ausschütteln**, weil dabei die Eier durch den
  Raum fliegen und sich auf allem absetzen. Zusammenrollen statt schütteln.
* Wo nicht heiß gewaschen werden kann: lange in praller Sonne auslüften.

Der Eintrag sagt auch, was das kostet: **mehrere Wochen statt einer Tablette.**

### Zur Suche

„madenwurm“, „bettwaesche“ führen auf den Eintrag; bei „juckt“, „nachts“ und
„kratzen“ steht er hinter Krätze und Läusen, was richtig ist — die jucken auch
und sind häufiger sichtbar. **Verdrängt wurde nichts.**

Abgesichert in `derMadenwurmTippNenntBeideFehlerUndDenWegOhneMittel`.

## 17.08.2026 — Nagelbettentzündung am Finger: recherchiert, geschrieben, NICHT eingebaut

Dritter Punkt aus dem Alltagsnotfall-Abgleich — und der einzige, der nicht ins
Paket kam. **Nicht aus inhaltlichen Gründen: Das Paket ist an seiner
Wortgrenze angekommen** (siehe `ROADMAP.md`, „Das Wortbudget des Europa-Pakets
ist voll"). Der fertige Eintrag liegt als `work/wartend/tip-nagelbett.json`.
Diese Notiz steht hier, damit die Arbeit nicht zweimal gemacht wird.

### Warum die Lücke heikel war

`medizin-abszess-eroeffnen` verbietet ausdrücklich, an eine Beule über einem
Gelenk an Hand oder Fingern zu gehen — wegen Sehnenscheide und Gelenkraum. Das
Verbot ist richtig, ließ aber jemanden mit einem eiternden Finger ohne jede
Anleitung zurück. Im Bestand (`work/quellen/`) fand sich dazu nichts; der
einzige Treffer war eine Nebenbemerkung zum Herpes-Panaritium im SOF-Handbuch.

### Quellen, im Volltext gelesen am 17.08.2026

MSD Manual, Profi-Ausgabe (deutsch), gesichert unter `work/quellen/nagelbett/`:

* **„Akute Paronychie"**, Shari Lipner, Weill Cornell Medicine, vollständige
  Überprüfung Oktober 2025
* **„Panaritium"**, David R. Steinberg, Perelman School of Medicine, Mai 2024
* **„Herpetische Nagelbettinfektion"**, derselbe Autor, Mai 2024
* **„Infektiöse Beugertenosynovitis"**, derselbe Autor, Mai 2024

### Die drei Funde, die den Eintrag tragen

**Erstens, der Widerspruch löst sich auf.** Beim Eiter am Nagelrand wird das
Werkzeug zwischen Nagel und Nagelfalz geschoben; die Quelle schreibt: *„Eine
Hautinzision ist nicht erforderlich."* Man hebt die Hautfalte vom Nagel ab,
statt zu schneiden. **Genau deshalb gilt das Verbot aus dem Abszess-Tipp hier
nicht** — an dieser Fuge liegen weder Sehnenscheide noch Gelenk. Das ist der
ganze Grund, warum an dieser einen Stelle etwas geht, was sonst an der Hand
verboten ist.

**Zweitens, die Falle mit dem Zeitversatz.** Die Quelle stellt als Warnung
voran, vor jedem Einschnitt an eine Virusinfektion zu denken, *die nicht
eingeschnitten werden darf*. Unterscheiden lässt sie sich an der kaum oder gar
nicht druckempfindlichen Fingerkuppe und an Bläschen — **die aber meist erst
zwei bis drei Tage nach dem Schmerzbeginn erscheinen.** Also gerade dann nicht
sichtbar, wenn jemand zum Messer greift. Der Verlauf ist selbstlimitierend.

**Drittens, vier Zeichen ohne jedes Gerät** (Kanavel), die sagen, dass es in
die Beugesehnenscheide gezogen ist: gebeugte Ruhehaltung des Fingers,
gleichmäßige spindelförmige Schwellung über die ganze Länge, Druckschmerz
entlang der Beugeseite, und — das deutlichste — Schmerz beim vorsichtigen
**passiven** Strecken. Das ist der Punkt, an dem es keine Nagelsache mehr ist.

Dazu aus derselben Quelle: die Entstehung über Niednagel, Nagelfalzverletzung,
verlorenes Nagelhäutchen, Reizung durch Wasser und Waschmittel sowie Nagen und
Lutschen; warme Bäder als erste Behandlung; Gazestreifen für 24 bis 48 Stunden;
und dass bei Zuckerkrankheit und schlechter Durchblutung besonders eine
Entzündung am Zeh **das Bein bedrohen** kann.

### Stand der übrigen Kandidaten

**Wadenkrampf: kein Eintrag nötig** — geprüft und vollständig durch
`erste-hilfe-salzmangel` abgedeckt. **Madenwurm: erledigt** (siehe oben).
Offen bleiben **Fremdkörper im Ohr** und **Sonnenallergie**; für beide fand
sich bisher keine Quelle im Bestand.

## 17.08.2026 — Fremdkörper im Ohr: recherchiert und geschrieben, wartet auf Platz

Zweiter fertiger Eintrag, der nicht ins Paket kann, solange die Wortgrenze
nicht entschieden ist. Liegt als `work/wartend/tip-ohr-fremdkoerper.json`
(`work/` ist nicht im Repository). Diese Notiz steht hier, damit die Arbeit
nicht zweimal gemacht wird.

**Erst den Bestand durchsucht** (Merkzettel-Regel): Das SOF-Handbuch nennt
Fremdkörper im Ohr nur im Zusammenhang mit Explosionsverletzungen und
gerissenem Trommelfell („Do not attempt removal of foreign debris"), das
gedruckte Survival-Buch behandelt Gehörgangsentzündung, Mittelohrentzündung
und den Ohrenschmalzpfropf — nicht den Fremdkörper. Im Paket selbst gibt es
„Ohrenschmerz: Umschlag ja, Stäbchen nein", „Ohrentzündung: drei
verschiedene Sachen mit einem Namen" und „Fremdkörper in der Wunde", aber
nichts zu etwas, das im Gehörgang steckt. Die Lücke war echt.

**Quelle, im Volltext gelesen am 17.08.2026:** MSD Manual, Profi-Ausgabe,
„Entfernen eines Fremdkörpers aus dem äußeren Ohr"; gesichert unter
`work/quellen/ohr-fremdkoerper/`.

### Die drei Sätze, die den Eintrag tragen

**Erstens, gegen das Gefühl:** Nicht der Gegenstand ist der Schaden, sondern
das Herausholen. Die Quelle sagt, der **erste Versuch** sei die beste
Gelegenheit für eine verletzungsfreie Entfernung, und der Gehörgang schwelle
durch mehrere Versuche **schnell an**. Sie empfiehlt ausdrücklich, die Zahl
der Versuche **vorher** zu begrenzen.

**Zweitens, wann Spülen falsch ist** — vier Fälle: bei möglicher
Trommelfellperforation (Schwindel, Ohrgeräusche, Hörverlust, Blut hinter dem
Gegenstand), bei Samen und Pflanzlichem (**quillt auf**), bei Weichem, und
bei Knopfzelle oder Magnet (**Wasser verschlimmert den Schaden**).

**Drittens, der Handgriff ohne Ausrüstung:** Ein lebendes Insekt wird
**zuerst mit Öl abgetötet**, dann geholt. Öl hat fast jeder; die Qual hört
sofort auf, und ein totes Insekt eilt nicht mehr. Beim Ziehen an Körper,
Flügel oder Bein warnt die Quelle: bei zu viel Druck **zerfällt** es, und
dann liegen mehrere Teile drin statt eines.

Dazu die Knopfzelle als Notfall (Schaden **binnen Stunden**, Querverweis auf
den vorhandenen Schluck-Eintrag) und der Zug an der Ohrmuschel — bei
Erwachsenen nach oben und hinten, bei Kindern nach unten und hinten.

### Was noch aussteht

Die **Suchwirkung ist ungemessen** und lässt sich erst messen, wenn der
Eintrag im Paket liegt. Besonders zu prüfen: „Ohr" im Titel wiegt schwer und
könnte „Ohrenschmerz" oder „Ohrentzündung" verdrängen.

## 17.08.2026 — Lichtempfindlichkeit durch Medikamente: dritter Eintrag, der wartet

Aus der Lücke „Sonnenallergie" ist etwas Besseres geworden. Liegt als
`work/wartend/tip-lichtempfindlichkeit.json`.

**Der Weg dahin gehört dazu.** Zuerst geprüft, ob Sonnenbrand überhaupt fehlt —
er fehlt NICHT: „Verbrennung versorgen: Blasen bleiben zu" behandelt ihn
ausdrücklich samt der Grenze, ab der er angesehen gehört, und „Verbrennung:
mindestens zehn Minuten kühlen" trägt ihn als Schlagwort. Ein eigener
Sonnenbrand-Eintrag wäre eine Dopplung gewesen.

Beim Durchsuchen des Bestands nach „photosens" fiel dann etwas anderes auf:
Das SOF-Handbuch warnt an **acht Stellen** vor Lichtempfindlichkeit, fast immer
bei Doxycyclin, und hat dafür einen eigenen Anhang. Im Volltext-Auszug fehlte
die Tabelle (beim Umwandeln verlorengegangen); sie steht erst in der PDF-Seite
A-32 und ist jetzt als Text unter `work/quellen/lichtempfindlichkeit/`
gesichert.

### Warum das mehr trägt als „Sonnenallergie"

Die Liste enthält Mittel, die in einem Vorrat **wirklich** liegen:

* **Doxycyclin** — das Mittel, das bei Zeckenkrankheiten gegeben wird und in
  fast jedem Vorrat liegt; die Quelle warnt eigens („avoid the sun").
* **Ibuprofen** und Ketoprofen — das ist der überraschende Posten: ein Mittel
  aus der Hausapotheke, das jeder für harmlos hält.
* Ciprofloxacin und die übrigen Fluorchinolone, Sulfonamide, Tetracyclin,
  Minocyclin, Chloroquin, Chinin, Griseofulvin.
* Aus dem Alltag chronisch Kranker: **Hydrochlorothiazid** (Blutdruck),
  Omeprazol, Amiodaron.

### Die drei Zeichen, und die Abwägung am Schluss

Unterschieden wird vom gewöhnlichen Sonnenbrand an dreierlei: Die Reaktion
**passt nicht zur Zeit** in der Sonne; sie endet **messerscharf an der
Kleidung**; und sie **juckt**, statt nur zu brennen.

Der Zweig „wenn niemand kommt" nennt die Abwägung beim Namen, weil sie in
beide Richtungen falsch gemacht wird: **Ein Mittel gegen eine ernste Krankheit
wird nicht wegen einer Hautreaktion abgesetzt** — man deckt die Haut ab und
macht weiter. Nur was bloße Bequemlichkeit ist, darf weg; das Ibuprofen lässt
sich durch Paracetamol ersetzen, das in der Liste **nicht** steht.

### Was aussteht

Wie bei den beiden anderen wartenden Einträgen ist die **Suchwirkung
ungemessen**. Besonders zu prüfen, sobald eingebaut: ob dieser Eintrag den
Verbrennungs-Einträgen das Wort „sonnenbrand" streitig macht.

## 17.08.2026 — Suchprobe: was gesucht, aber nicht gefunden wird

Die notierte Lückenliste war abgearbeitet, also eine neue Probe gefahren — 46
Alltagsformulierungen durch die echte Suche, wie bei den Laienwörter-Runden im
August. **Das Ergebnis ist überwiegend beruhigend und in einem Punkt nicht.**

### Erst die Entwarnung: dreimal war die Lücke keine

* **Pseudokrupp** steht im Paket („Pseudokrupp: bellen und pfeifen beim
  Atmen"), obwohl der SNAPSHOT ihn noch als offen aus dem WHO-Handbuch führt.
* **Sonnenbrand** ist in den Verbrennungs-Einträgen abgedeckt, samt der
  Grenze, ab der er angesehen gehört.
* **Blase am Fuß** hat einen eigenen Eintrag.

Alle drei hätte ich ohne Nachsehen doppelt geschrieben.

### Wortlücken: Der Inhalt ist da, das Wort führt nicht hin

Diese Formulierungen finden nichts oder etwas Falsches, obwohl es den
passenden Eintrag gibt. Das sind **Schlagwörter**, keine neuen Einträge:

| getippt | landet bei | gemeint ist |
|---|---|---|
| „wund gelaufen" | Seife machen | Blase am Fuß |
| „aufgelaufen", „Druckstelle Schuh" | nichts / Kohl | Blase am Fuß |
| „Schürfwunde" | **Offene Brustwunde** | Wunde ausspülen und bedecken |
| „blaues Auge", „Veilchen" | nichts / Giftpflanzen | Augenverletzung |
| „Bluterguss" | Augenverletzung | Schmerz ohne Tabletten |
| „Splitter unter dem Nagel" | Kaninchen | Fremdkörper in der Wunde |
| „Zeh gebrochen" | Armbruch | Unterschenkel/Sprunggelenk |

„Schürfwunde" ist der ärgerlichste Fall: Er stand schon einmal auf der
Liste und führt weiterhin zuerst in eine **offene Brustwunde**.

### Eine echte Lücke: Verstauchung und Zerrung allgemein

„Zerrung" findet **gar nichts**, „Band gezerrt" ebenso wenig. Der vorhandene
Eintrag „Unterschenkel und Sprunggelenk: kann sie noch gehen?" beantwortet die
Frage Bruch oder Verstauchung **am Knöchel** und nennt Hochlegen und Kühlen in
einem Nebensatz — eine allgemeine Behandlung von Verstauchung und Zerrung
(Knie, Handgelenk, Muskel) gibt es nicht. Das ist der nächste Eintrag, der
geschrieben wird.

**Beides wartet auf dieselbe Entscheidung:** Schlagwörter kosten ebenfalls
Wortvorkommen, und davon sind nur noch rund 1 100 frei.

## 18.08.2026 — Verstauchung und Zerrung: vierter wartender Eintrag

Die echte Lücke aus der Suchprobe. Liegt als
`work/wartend/tip-verstauchung-zerrung.json`.

**Bestand zuerst:** Im SOF-Handbuch steht kein Abschnitt zur akuten
Verstauchung, wohl aber unter „Joint exam" die Untersuchung selbst — seitliche
Belastung und Schubladenzeichen im Vergleich mit der gesunden Seite als
Hinweis auf eine Band- oder Sehnenverletzung. Das ist die Probe, die im
Eintrag steht. (Die Volltextsuche nach `strain` liefert im Bestand vor allem
Agrarhefte — dort heißt es „die Milch strainen". Dieselbe Falle wie „felon" in
„lifelong".)

**Zweite Quelle, im Volltext gelesen:** MSD Manual, Profi-Ausgabe, „Überblick
über Verstauchungen und andere Weichteilverletzungen", gesichert unter
`work/quellen/verstauchung/`.

### Was den Eintrag trägt

**Erstens die Ehrlichkeit der Quelle selbst:** Sie nennt das PRICE-Schema und
schreibt im selben Satz, es sei **nicht durch eindeutige Nachweise belegt**.
Das steht so im Eintrag — es ist das Beste, was es gibt, und keine Gewissheit.

**Zweitens die Zahlen:** Kühlen 15–20 Minuten am Stück über 24–48 Stunden, nie
direkt auf die Haut; Hochlagern **über Herzhöhe** (ein Bein auf dem Stuhl liegt
nicht über Herzhöhe); nach 48 Stunden dreht es sich um, dann hilft **Wärme**.

**Drittens der Fehler, der am meisten kostet:** zu lange schonen. Die Quelle
sagt, eine leichte Verstauchung werde — wenn überhaupt — nur kurz
ruhiggestellt, **frühe Bewegung sei am besten**, und nennt Knie, Ellbogen und
Schulter als besonders steifheitsanfällig, ältere Menschen zuerst. Ein steifes
Knie nach vier Wochen Schonung ist der größere Schaden.

Dazu das Kompartmentsyndrom als seltene, gliedbedrohende Folge mit Verweis auf
den vorhandenen Eintrag, und im Zweig „wenn niemand kommt" die ehrliche
Grenze: Ein gerissenes Band wächst ohne Operation nicht wieder zusammen — man
plant mit dem schwächeren Gelenk, statt auf Heilung zu warten.

### Nebenbefund

Der SNAPSHOT führte Diphtherie und Krupp noch als offene Themen aus dem
WHO-Handbuch. Beide sind längst im Paket; die Zeile ist berichtigt. Wer ihr
gefolgt wäre, hätte sie ein zweites Mal geschrieben.

## 18.08.2026 — BERICHTIGUNG der Suchprobe von gestern

Die Wortlücken-Tabelle im Abschnitt „Suchprobe: was gesucht, aber nicht
gefunden wird" **war größtenteils falsch**, und zwar aus einem Fehler in der
Messung: Sie entstand mit `tools/inhalt/nachschlagen.py` und mit
ASCII-Umschriften („schuerfwunde"). **`nachschlagen.py` ist nicht die Suche der
App** — es ist ein Volltext-Werkzeug mit anderer Bewertung. Und die App faltet
ü→ue erst intern; wer „schuerfwunde" eintippt, sucht ein anderes Wort als
„Schürfwunde".

Mit dem **echten Suchindex** und richtig geschriebenen Wörtern nachgemessen:

| getippt | tatsächlicher erster Treffer | Urteil |
|---|---|---|
| „Schürfwunde" | **Ausspülen statt auswischen** | richtig, kein Fehler |
| „wundgelaufen" | **Blase am Fuß** | richtig |
| „Zeh gebrochen" | Arm und Schlüsselbein, dann Sprunggelenk | vertretbar |
| „Zerrung" | *nichts* | **echte Lücke** (Eintrag geschrieben) |
| „blaues Auge" | *nichts* | offen |

Die Schlagwörter waren also längst da: `blase-fuss` trägt „wundgelaufen",
`wunde-ausspuelen` trägt „schürfwunde" und „aufgeschürft".

### Was wirklich dahintersteckt

Die Treffer fehlen bei **mehrwortigen** Anfragen: „wund gelaufen" (getrennt),
„Splitter unter dem Nagel", „schürfwunde knie". Das ist keine Wortlücke,
sondern die **strukturelle Grenze**, die im SNAPSHOT längst steht: Die Suche
vergleicht Wortanfänge und verlangt alle Wörter; Formulierungen aus
Allerweltswörtern lassen sich damit nicht lenken. Dagegen hilft kein
Schlagwort, sondern nur eine andere Bewertung der Suche.

**Die Lehre für mich:** Eine Suchmessung gilt nur, wenn sie mit dem
`SearchIndex` des Pakets gemacht wird und mit den Wörtern, die ein Mensch
wirklich tippt — mit Umlauten. Alles andere misst das Messwerkzeug.

## 18.08.2026 — Schlag aufs Auge: fünfter wartender Eintrag

Aus dem letzten offenen Treffer der Suchprobe („blaues Auge" fand nichts).
Liegt als `work/wartend/tip-schlag-aufs-auge.json`.

**Erst geprüft, was schon da ist:** „Augenverletzung: abschirmen, nicht
draufdrücken" behandelt Fremdkörper, Verätzung und den offensichtlich
eröffneten Augapfel — im Text kommen aber weder Doppelbilder noch die
Augenhöhle, das Jochbein, die Pupillenform oder ein taubes Gesicht vor. Der
stumpfe Schlag auf die Augenhöhle fehlte.

**Quelle, im Bestand gefunden** (keine Websuche nötig): SOF Medical Handbook
2001, Augentrauma. Sie liefert genau die Untersuchung ohne Gerät.

### Die Zeichen, und warum die Reihenfolge stimmt

**Sehschärfe zuerst** — die Quelle nennt sie an erster Stelle, sie sinkt bei
fast allem Ernsten. Dann die **Blickfolge in alle Richtungen**: Einschränkungen
deuten auf einen eingeklemmten Muskel oder einen Bruch der Augenhöhle; das ist
der Griff, der den Bruch findet, und er kostet zwei Minuten. Dann
**Doppelbilder**, die **Pupillenform** (verzogen = Verdacht auf verdeckt
eröffneten Augapfel) und **Blut vor der Regenbogenhaut**.

**Die Regel mit dem Ausrufezeichen aus der Quelle:** Ein möglicherweise
eröffneter Augapfel wird NICHT betastet.

Zwei Sätze sind als Einordnung gekennzeichnet, weil sie nicht in der Quelle
stehen: das **taube Wange** als Zeichen des Bodenbruchs (der Nerv läuft dort
durch) und **nicht schnäuzen** — bei gebrochenem Boden presst das Luft aus der
Nebenhöhle ins Gewebe um das Auge.

### Zwei Fallen beim Schreiben, beide gefangen

Der Entwurf verwies auf **„Verstauchung und Zerrung"** — einen Eintrag, der
selbst noch wartet. Wäre nur ein Teil der wartenden eingebaut worden, wäre der
Verweis ins Leere gelaufen. Jetzt steht die Kühlregel ausgeschrieben da, und
der Eintrag steht für sich. Und der Verweis auf eine „Kopfverletzung" ging auf
einen Titel, den es nicht gibt; der Eintrag heißt „Gehirnerschütterung
erkennen".

`einbauen.py --pruefen-ohne-schreiben`: bestanden.

## 18.08.2026 — Wie vollständig ist das Paket eigentlich? Eine Standortbestimmung

Bevor weiter Einträge entstehen, einmal gemessen, wo das Paket steht — 30
Kernnotfälle durch den **echten Suchindex**, mit Umlauten, wie ein Mensch sie
tippt.

**29 von 30 landen richtig**, meist schon beim ersten Treffer: Zeckenstich,
Bienen- und Wespenstich, Schlangenbiss, Tollwut, Ohnmacht, Krampfanfall,
Unterzuckerung, Herzinfarkt, Schlaganfall, Nasenbluten, Verbrennung,
Stromunfall, Ertrinken, Erfrierung, Hitzschlag, Vergiftung, Durchfall,
Erbrechen, Fieber, Ersticken, Halsschmerzen, Zahnschmerz, Bauchschmerz,
Blasenentzündung, Notgeburt, Fehlgeburt, Angst und Panik.

**Das ist die eigentliche Auskunft dieser Runde:** Die akuten Alltagsnotfälle
sind abgedeckt. Was jetzt noch fehlt, liegt am Rand — und je weiter man an
den Rand geht, desto eher schreibt man etwas, das dünn belegt ist. Regel 1
sagt Qualität vor Menge; das gilt auch für die Anzahl der Einträge.

### Der eine Treffer, der danebenging

„Schlaflosigkeit" landet bei den Entzugs-Einträgen. **Schlaf ist als Thema
nicht im Paket** — kein Titel trägt das Wort, und weder „Was der Kopf in einer
langen Krise tut" noch „Wenn jemand zusammenbricht: vier Dinge, die helfen"
behandeln es. Der Stress-Eintrag nennt Ruhe als Teil der Erholung, nicht das
Problem „ich kann nicht schlafen, und morgen muss ich Entscheidungen treffen".

**Nicht geschrieben, und zwar bewusst:** Im Bestand steht dazu fast nichts —
zwei Nebensätze im SOF-Handbuch (Schlafmangel als Ursache von Kopfschmerz und
psychischen Beschwerden, „8 Stunden pro Nacht, regelmäßig" im Epilepsie-Teil).
Daraus einen Eintrag zu bauen hieße, den größeren Teil selbst zu erfinden und
als Einordnung zu kennzeichnen. Das wäre der falsche Handel. **Für Schlaf
braucht es eine eigene, ordentliche Quelle** — dann lohnt der Eintrag, denn
Schlafmangel verdirbt genau das Urteilsvermögen, auf das in einer Krise alles
ankommt.

## 18.08.2026 — Schlaf in der Krise: sechster wartender Eintrag

Die Lücke aus der Standortbestimmung, jetzt mit ordentlicher Quelle: IQWiG,
`gesundheitsinformation.de`, „Was tun bei Schlafproblemen?", im Volltext
gelesen und unter `work/quellen/schlaf/` gesichert. Liegt als
`work/wartend/tip-schlaf-in-der-krise.json`.

**Warum der Eintrag hierher gehört:** Schlafmangel verdirbt das
Urteilsvermögen — in einer Lage, in der jede Entscheidung zählt, ist das die
eigentliche Gefahr, nicht die Müdigkeit selbst.

### Die zwei Sätze, die dem Gefühl widersprechen

**Wer nicht schlafen kann, steht auf.** Die Reiz-Kontroll-Technik: Das Lager
soll für den Körper nur eines bedeuten. Wer eine Stunde wach darin liegt und
sich ärgert, bringt ihm das Gegenteil bei. Dazu die feste Aufstehzeit ohne
Schlummertaste — sie ist der Anker, die Einschlafzeit richtet sich danach.

**Weniger Zeit im Lager, nicht mehr.** Wer sechs Stunden schläft und acht
liegt, übt zwei Stunden Wachliegen. Die Quelle rechnet rückwärts von der
Aufstehzeit.

Dazu die Schlafhygiene (Alkohol, Kaffee, Tee 4–6 Stunden vorher; ruhig, dunkel,
temperiert; Ohrstöpsel) — und die **offene Forschungslage zu Nickerchen**, die
die Quelle ausdrücklich einräumt und die deshalb auch im Eintrag offen bleibt.

### Was Einordnung ist und nicht aus der Quelle stammt

Die Quelle rechnet mit einem Schlafzimmer. Für eine Matte in einer Halle
brauchte es die Übersetzung: **Wachen einteilen statt verteilen** (vier
Stunden am Stück sind mehr wert als zweimal zwei), **die Unterlage vor der
Decke**, **kein Alkohol als Schlafmittel** (mit Verweis auf den
Entzugs-Eintrag), das Erwartbare aussprechen — und der letzte Satz, der die
Sache ernst nimmt: Schlaf ist eine Aufgabe wie Wasserholen und muss in einer
erschöpften Gruppe zugeteilt werden.

Nicht wiederholt wurde die Muskelentspannung: Sie steht schon in „Angst und
Panik: drei Übungen ohne Mittel", auf das der Eintrag verweist.

`einbauen.py --pruefen-ohne-schreiben`: bestanden.


## 19.08.2026 — Wasser: die Trübung messen, und ein Brunnen nach Hochwasser

Auftrag für die Nacht: weiter an Artikeln. Wasser war mit 21 Einträgen
die dünnste Kategorie, und der Bestand hatte eine Lücke, die man erst sieht,
wenn man ihn hintereinander liest: In fünf Tipps steht „trübes Wasser zuerst
absetzen und filtern" — und in keinem steht, **woran man merkt, dass es klar
genug ist**.

### Die Quelle

**WHO, „Technical Notes on Drinking-Water, Sanitation and Hygiene in
Emergencies", Notiz 1 „Cleaning hand-dug wells", Fassung Juli 2013.** Erstellt
für die WHO vom WEDC der Universität Loughborough, Autoren Sam Godfrey und Bob
Reed, Reihenherausgeber Bob Reed. Vier Seiten, **am 19.08.2026 vollständig
gelesen** (als PDF geholt und in Text gewandelt, weil der Abruf über die
Webseite nur die Binärdatei lieferte).

Rechtlich: © WHO 2013, alle Rechte vorbehalten. Damit ist sie **Beleg, keine
Textquelle** — dieselbe Behandlung wie die BBK-Veröffentlichungen. Beide
Einträge sind eigenständig formuliert.

Die Reihe hat 15 Notizen und trägt noch mehr für dieses Paket: Nr. 5
(Aufbereitung am Gebrauchsort), Nr. 9 (Wassermenge in Notlagen), Nr. 11
(Chlor messen), Nr. 15 (Brunnen nach Salzwasser-Überflutung). Notiert für die
nächsten Runden.

### Eintrag 1: „Wie trüb ist zu trüb? Die Münze im Eimer"

Der Kern ist Kasten 1.3 der Notiz: ein Trübungstest **ohne Messgerät**. Dunkles
Gefäß von mindestens 50 cm Tiefe, eine matte Messing- oder Kupfermünze von
etwa 2,5 cm, langsam Wasser eingießen, bis die Münze verschwindet, dann die
Tiefe messen. Unter 32 cm: über 20 NTU. 32 bis 50 cm: 10 bis 20 NTU. Über
50 cm: unter 10 NTU.

Dazu aus Tabelle 1.1 die Begründung, warum das überhaupt zählt: Trübung über
5 NTU verlangt mehr Chlor, weil es erst die organische Substanz oxidiert; und
pH 6,8–7,2 senkt den Chlorbedarf. Und aus Schritt 2 der harte Satz: **niemals
trübes Wasser chloren**, weil Schwebstoffe die Keime schützen.

**Die Grenze, die im Eintrag offen ausgesprochen wird:** Der Test löst nur bis
„unter 10 NTU" auf — die Chlorung will unter 5. Er kann also beweisen, dass es
zu trüb ist, aber nicht, dass es klar genug ist. Das steht so im Eintrag, weil
die umgekehrte Lesart eine Sicherheit behaupten würde, die die Quelle nicht
hergibt.

### Eintrag 2: „Brunnen nach Hochwasser: reinigen, entkeimen, leerpumpen"

Die vier Schritte der Notiz, gekürzt auf das, was ohne Nothilfe-Ausrüstung
trägt. Mit Zahlen: 15 cm Kies auf den Grund, 300 g Kalkhypochlorit auf einen
20-Liter-Eimer, davon 10 Liter je Kubikmeter Brunnenwasser (doppelte Menge für
die Wände), mindestens 30 Minuten Einwirkzeit, nach dem Leerpumpen und
Wiederauffüllen weitere 30 Minuten, dann Restchlor unter 0,5 mg/l.

Die Volumenformel V = π·D²·h/4 steht im Eintrag als **Durchmesser mal
Durchmesser mal Wassertiefe mal 0,785** — dieselbe Rechnung, aber ohne
Formelzeichen, die auf einem Telefonbildschirm niemand abliest.

**Drei Sätze aus der Quelle, die den Eintrag tragen:**

1. **Chlorgas.** Kalkhypochlorit und Bleiche geben es ab, es ist eine ernste
   Gesundheitsgefahr. Die Quelle sagt: von außen arbeiten, mit langstieliger
   Bürste; nur mit Schutzkleidung, Atemschutz und starker Belüftung hinein.
2. **„It does not mean, however, the water is safe for drinking."** Restchlor
   unter 0,5 mg/l heißt: der Brunnen ist wie vorher — nicht: das Wasser ist
   trinkbar. Ohne diesen Satz wäre der ganze Eintrag gefährlich.
3. **Kein Restschutz, und nicht gegen alles.** Die Entkeimung hinterlässt
   nichts im Wasser, und gegen Cryptosporidium wirkt Chlor nicht. Also weiter
   abkochen, sauber schöpfen und lagern.

### Zweiter Zweig „niemand kommt"

Das Verfahren ist für Fachkräfte geschrieben. Der Eintrag endet deshalb mit dem
Fall, dass niemand mit Pumpe und Chlormessgerät kommt: ausräumen, oben
abdichten, Deckel drauf, Oberflächenwasser fernhalten — und danach jeden Liter
abkochen. Ausdrücklich als Einordnung dieses Pakets gekennzeichnet.

### Die Suche, gemessen am 19.08.2026

| getippt | erster Treffer |
|---|---|
| trüb, trübung | `wasser-truebung-messen-muenze` |
| münze, münztest, ntu, schwebstoffe | `wasser-truebung-messen-muenze` |
| brunnen, hochwasser, überschwemmung, flut | `wasser-brunnen-nach-hochwasser` |
| chlorgas, kalkhypochlorit, schachtbrunnen, grundwasser | `wasser-brunnen-nach-hochwasser` |

Und die Gegenprobe, dass der Neuzugang nichts verdrängt: `wasser` führt weiter
auf die Vorbehandlung, `chlor` auf den Chlortipp, `abkochen` aufs Abkochen.
Beides steht jetzt fest in `NotfallSucheTest`.

Suchbudget danach: **305 210 von 450 000 Wortvorkommen**, frei 144 790 — Platz
für rund 225 weitere Einträge. `packsign pack`: bestanden.


## 19.08.2026 — Wasser, zweite Runde: Chlor nachweisen, Leitung nach Ausfall, Filterarten

Zwei weitere Notizen derselben WHO-Reihe, beide am 19.08.2026 im Volltext
gelesen (als PDF geholt und in Text gewandelt):

* **Notiz 11 „Measuring chlorine levels in water supplies"**, Autor Bob Reed.
* **Notiz 5 „Emergency treatment of drinking-water at the point of use"**,
  Autoren Sam Kayaga und Bob Reed.

Beide © WHO 2013 — Beleg, keine Textquelle.

### „Freies Chlor: der Beweis, dass das Entkeimen gewirkt hat"

Der Gedanke, der den Eintrag trägt: Bei Abkochen, Sonnenlicht und Filtern weiß
man hinterher **nicht**, ob es gereicht hat. Beim Chlor weiß man es. Chlor
verbraucht sich erst an allem, was im Wasser ist; bleibt danach noch **freies
Chlor** übrig, ist es mit allem fertig geworden. Zielwert an der Entnahme:
**0,2 bis 0,5 mg/l** (Kasten 11.3).

Dazu drei Fehler, die alle drei in der Quelle stehen und die man nicht rät:

1. **Die 30 Minuten gelten ab 18 °C.** Kälteres Wasser braucht **länger**. Ein
   Bach im Frühjahr hat vier Grad.
2. **Festes Chlor nie direkt ins Wasser** — es löst und verteilt sich nicht.
   Erst mit wenig Wasser zu einem Brei anrühren.
3. **Nie vor einen langsamen Sandfilter chloren** — dort arbeiten Bakterien,
   und Chlor bringt sie um.

### „Die Leitung läuft wieder: trinken darfst du noch nicht"

Aus Kasten 11.4, und das ist der Fund der Runde: **Jedes Rohrnetz ist
undicht.** Unter Druck drückt es nach außen. Fällt der Druck weg, zieht es
durch dieselben Löcher an, was neben dem Rohr im Boden liegt — oft eine
Abwasserleitung. Die Quelle zieht daraus die harte Folgerung: Eine Versorgung,
die **nicht durchgehend** läuft, ist als verunreinigt anzunehmen, und ins Netz
zu chloren ist dann zwecklos. Was hilft, ist Aufbereiten am Gebrauchsort.

Für dieses Paket ist das genau der Fall, um den es geht: Strom weg, Pumpen aus,
Wasser kommt stockweise wieder. Der Eintrag nennt deshalb auch die Wege, über
die es die meisten erwischt, weil sie nicht ans Trinken denken — Zähneputzen,
Salat waschen, Eiswürfel.

### „Filter: was welcher kann, und was keiner kann"

Aus Notiz 5. Der Bestand kannte Filter nur als Sammelbegriff („Keramik-, Sand-
oder Membranfilter"). Der Unterschied entscheidet aber, ob danach noch
abgekocht werden muss: Membranfilter halten **auch Viren** zurück;
Keramikkerzen filtern mechanisch, und **nur die silberhaltigen** entkeimen
zusätzlich. Dazu die Pflege (Kerze unter fließendem Wasser abbürsten) und der
Satz, der für alle gilt: **kein Filter holt Chemikalien heraus.**

### Zwei Wächter haben angeschlagen — und beide hatten recht

**Der Titelwächter:** Der Leitungs-Eintrag hieß zuerst „Wenn die Leitung wieder
läuft: das erste Wasser ist verdächtig". Das Wort *Wasser* im Titel wiegt fünf
Punkte und hätte dem Eintrag, dem es gehört, den ersten Platz genommen. Neuer
Titel ohne das Wort: „Die Leitung läuft wieder: trinken darfst du noch nicht".

**Der Querverweis-Wächter:** Zwei ausgeschriebene Verweise zeigten ins Leere —
„Sandfilter bauen" heißt vollständig „Sandfilter bauen: trinkbar machen ohne
Brennstoff", und „Durchfall: der Flüssigkeitsverlust ist die Gefahr" gibt es
gar nicht; gemeint war „Austrocknung bei Durchfall erkennen". Beides
berichtigt.

### Eine Zusicherung, die ich selbst zurückgenommen habe

In Runde 1 stand `ersterTreffer("chlor", "wasser-chlor-entkeimung")`. Nach
Runde 2 schlug sie an — und die Ursache ist **kein Qualitätsproblem**:
Beide Tipps bekommen für „chlor" dieselbe Punktzahl, und bei Gleichstand
sortiert `besteTreffer()` nach dem **Titel**. „Freies Chlor…" steht
alphabetisch vor „Trinkwasser mit Chlor…".

Eine Zusicherung auf Platz eins hielte damit keine Suchqualität fest, sondern
einen Anfangsbuchstaben, und schlüge beim nächsten Titel irgendwo im Paket
grundlos an. Sie ist deshalb auf „beide werden gefunden" geändert, mit
Begründung im Test. Zwischenzeitlich hatte ich versucht, es über die
Schlagwörter zu drehen — das Ergebnis war, dass „chlorgehalt" **gar nichts**
mehr fand. Zurückgenommen: ein zweiter Platz ist besser als eine Lücke.

### Gemessen am 19.08.2026

| getippt | erster Treffer |
|---|---|
| freies chlor, restchlor, chlorgehalt, dpd | `wasser-freies-chlor-messen` |
| abkochgebot, leitung, wasserleitung, druckabfall, rohrbruch | `wasser-leitung-nach-ausfall` |
| filter, keramikfilter, membranfilter, viren | `wasser-filterarten` |

Gegenprobe unverändert: `wasser` führt auf die Vorbehandlung, `abkochen` aufs
Abkochen, `trüb` auf den Münztest, `brunnen` auf den Brunnen.

**Nebenbefund, nicht von dieser Runde verursacht:** `kein wasser` führt auf
`medizin-harnverhalt`, dann auf die vier Flüssigkeiten und „Durst ist kein
guter Ratgeber". Wer das tippt, meint fast sicher die Versorgung. Für eine
spätere Runde notiert — hier nicht angefasst, weil es eine Änderung an
bestehenden Einträgen wäre und die gehört gemessen, nicht nebenbei gemacht.

Suchbudget danach: **306 673 von 450 000**, frei 143 327. `packsign pack`:
bestanden.


## 19.08.2026 — Taktisch: der Kontrollpunkt, und die zweite Quelle dafür

Im ROADMAP steht diese Lücke seit dem 28.07.2026 mit einer ausdrücklichen
Warnung: „Sichtbarkeit vermeiden, Verhalten an Kontrollpunkten, Fluchtwege
planen, einen Unterschlupf absichern, Schutz vor Tieren. Die IFRC-Leitlinie
trägt davon nichts — **dafür braucht es eine zweite Quelle, sonst wird es
erfunden.**"

### Die zweite Quelle ist da

**David Lloyd Roberts, „Staying Alive — Safety and security guidelines for
humanitarian volunteers in conflict areas", IKRK Genf, Erstausgabe 1999,
überarbeitete Fassung 2005 (Ref. 0717/002).** 187 Seiten, am 19.08.2026 als
PDF geholt, in Text gewandelt und der einschlägige Abschnitt im Volltext
gelesen: „Check-points/Road-blocks", S. 97–100.

© IKRK — Beleg, keine Textquelle. Der Eintrag ist eigenständig formuliert und
zusätzlich **übertragen**: Die Quelle schreibt für Helfer mit Ausweis,
erkennbarem Fahrzeug und Organisation im Rücken. Was davon für eine
Zivilperson übrig bleibt, steht im Eintrag als eigener Abschnitt.

Das Buch trägt auch die übrigen offenen Punkte der Kategorie (Planung,
Sichtbarkeit, Verhalten in der Fläche). Für die nächsten Runden notiert.

### „Kontrollpunkt: langsam werden, Hände sichtbar, nichts anbieten"

Der Ablauf aus der Quelle, in der Reihenfolge, in der er passiert: vorher
langsam werden, Radio leise, nicht funken; Anweisungen befolgen; Fenster
herunter und in der Sprache des Ortes grüßen; Ausweis zeigen; ruhig bleiben,
wenn durchsucht wird; keine Eile zeigen.

**Der Satz, der den Eintrag trägt**, und der dem Gefühl widerspricht: NICHT
übertreiben. Zu viel reden, Zigaretten anbieten, sich anbiedern wirkt nicht
freundlich, sondern **ängstlich** — und Angst wird ausgenutzt.

Dazu das Praktische, das man vorher erledigt: nichts Begehrenswertes sichtbar
liegen lassen (aus einer Zigarette werden viele, die anderen kommen aus dem
Nichts), teure Uhr ab, Sonnenbrille ab. Und für die Nacht: früh abblenden,
am Posten Standlicht, **Innenlicht an**, damit man sieht, wer im Fahrzeug
sitzt — und der Grundsatz darüber, dass man nachts gar nicht fährt.

**Die wichtigste Regel gilt vor dem Posten, nicht an ihm:** Bei improvisierten
Sperren weit vorher anhalten und zusehen. Fährt anderer Verkehr durch? Wie
werden die Leute behandelt? Wer entgegenkommt, ist gerade durch — den fragen.
Und wenn es nicht gut aussieht, ist man weit genug weg, um umzudrehen.

### Gemessen am 19.08.2026

| getippt | erster Treffer |
|---|---|
| kontrollpunkt, kontrollposten, straßensperre, schranke, barriere | `taktisch-kontrollpunkt` |
| bewaffnete, posten, angehalten, durchfahrt | `taktisch-kontrollpunkt` |

**Zwei Wortlücken dabei gefunden und geschlossen:** „soldaten" führte auf
`erste-hilfe-grabenfuss`, „miliz" auf **gar nichts**. Wer das unter Druck
tippt, meint keine Fußpflege. Beide sind jetzt Schlagwörter des Eintrags,
nachgemessen und festgeschrieben.

Gegenprobe: `gewalt` führt weiter auf `taktisch-worum-es-geht`, `deeskalation`
auf `taktisch-deeskalation`, `flucht` und `gefahr` unverändert auf die
Notfall-Tipps.

Suchbudget: **307 250 von 450 000**, frei 142 750. `packsign pack`: bestanden.


## 19.08.2026 — Blindgänger und Fundmunition: ein blinder Fleck des Pakets

Beim Weiterlesen im IKRK-Buch aufgefallen und dann nachgemessen: Zu
**„blindgänger", „munition", „granate", „sprengkörper", „kampfmittel"** und
**„handgranate"** fand das Paket **gar nichts**. Nicht wenig — nichts.

Das ist für ein Handbuch, das sich an Menschen in Mitteleuropa richtet, eine
ernste Lücke: In Deutschland liegt Munition aus dem Zweiten Weltkrieg bis heute
im Boden und wird bei Bauarbeiten, nach Hochwasser und bei Waldbränden
regelmäßig gefunden. Und in jeder Lage, für die dieses Paket gemacht ist, ist
nicht detonierte Munition das, was nach dem Ende der Kampfhandlungen noch
weiter tötet.

### Quelle

Dasselbe Buch wie beim Kontrollpunkt: **Roberts, „Staying Alive", IKRK
1999/2005.** Am 19.08.2026 im Volltext gelesen wurden die Abschnitte zu
Sprengfallen, zu nicht detonierter Munition und zu Streumunition, S. 68–71.
© IKRK, nur als Beleg.

### Was den Eintrag trägt

**Der Grund für „nicht anfassen" ist unangenehm genau.** Abgefeuerte Munition
kann sehr instabil sein: Beim Zünder einer Granate kann ein kleiner Stoß
genügen; bei einer Handgranate kann es reichen, sie **aufzuheben**, damit der
Sicherungssplint herausfällt.

**Die wahre Geschichte aus dem Buch** steht im Eintrag, weil sie mehr erklärt
als jede Warnung: Ein Fahrer brachte eine scharfe Handgranate 300 km weit in
seiner Tasche mit; sie lag drei Monate in einem Büroschrank und wurde
herumgereicht. Der Splint war flachgedrückt statt aufgebogen.

**Sprengfallen** — ein harmlos aussehender Gegenstand, der auslöst, wenn jemand
ihn bewegt oder etwas scheinbar Ungefährliches damit tut. Daraus die zwei
unbequemen Sätze: verlassene Häuser nicht durchsuchen (auch nicht kurz, auch
nicht, um auf die Toilette zu gehen), und nichts aufheben, was interessant
aussieht.

**Streumunition und Kinder.** Ein Abwurf kann rund 100 × 50 m bedecken;
geschätzte 7 bis 10 von hundert Sprengkörpern gehen nicht los und wirken danach
wie Minen, die bei leichter Berührung auslösen. Ein verbreiteter Typ misst rund
20 × 6 cm und ist **gelb** — in Afghanistan hatten die Hilfspakete dieselbe
Farbe. Der Eintrag sagt Eltern deshalb, was sie den Kindern sagen sollen, und
zwar in der schärferen Form: nicht „vorsichtig sein", sondern „gar nicht
anfassen".

### Zweiter Zweig „niemand kommt"

In Deutschland ist der Kampfmittelräumdienst zuständig, die Polizei nimmt die
Meldung entgegen. Fällt beides aus, ändert das an der Regel nichts, sondern
macht sie wichtiger: Man kann so etwas nicht entschärfen. Was bleibt, ist die
Stelle kenntlich machen, den Weg drumherum legen und jeden warnen. Als
Einordnung dieses Pakets gekennzeichnet.

### Gemessen am 19.08.2026

Alle acht geprüften Wörter — blindgänger, fundmunition, munition, granate,
bombe, sprengkörper, kampfmittel, sprengfalle — führen jetzt auf
`taktisch-blindgaenger`, vorher auf nichts. Gegenprobe: `gewalt`, `gefahr`,
`flucht` und `kontrollpunkt` unverändert.

Suchbudget: **307 888 von 450 000**. `packsign pack`: bestanden.

### Was das Buch noch trägt — für spätere Runden

Minen (S. 41–50, samt „die gefährlichsten Gegenden"), Artillerie- und
Mörserbeschuss (S. 51–55), Scharfschützen (S. 56–60), Hinterhalt (S. 61–63),
CBRN (Kapitel 5), Planung und Nachbesprechung (S. 100–104). Davon ist im Paket
bisher **nichts** enthalten.


## 19.08.2026 — Minen: die fünf Gegenden, und eine Regel, die kaum jemand kennt

Dieselbe Quelle wie zuvor (Roberts, „Staying Alive", IKRK 1999/2005), Abschnitte
zur Minengefahr und „The most dangerous areas in terms of anti-personnel
mines", S. 41–49, am 19.08.2026 im Volltext gelesen.

### Warum das für ein mitteleuropäisches Paket überhaupt zählt

Verlegte Minen sind hier selten — das steht auch so im Eintrag. Aber die fünf
Gegenden sind nicht auf Minen beschränkt: Sie beschreiben, **wo etwas liegt,
das nicht liegen sollte**, und drei davon gelten unverändert für Blindgänger
und Fundmunition, die es hier sehr wohl gibt.

### Die fünf Gegenden

1. Alte Stellungen und Sperren — erkennbar an Patronenhülsen, Schutt,
   Stacheldraht, Bunkern, Erdaufwürfen.
2. Verlassene Häuser im Kampfgebiet.
3. **Das Unversehrte und Einladende in einem verlassenen Ort.** Der
   unangenehmste Punkt: Gelegt wird nicht an der Haustür — das wäre zu
   offensichtlich —, sondern am Seitenfenster, am Brunnen, unter dem schattigen
   Baum. Und die Frage, die den ganzen Abschnitt trägt: *Warum liegt dieses
   Ding hier eigentlich noch?*
4. Feldwege und Trampelpfade — alte Minen sind kaum zu sehen; auf befestigten
   Straßen bleiben.
5. Gärten und Äcker — auch in Obstgärten, Weinbergen, Gemüsebeeten.
   Ausgerechnet dort, wo man in einer Hungerlage hingeht.

Dazu der Satz über „geräumte" Gebiete: Niemand kann das mit Sicherheit sagen.
Im Garten eines als geräumt gemeldeten Hauses wurden drei Minen gefunden.

### Der Fund der Runde

**Kein Funk und kein Handy im Umkreis von 100 Metern** — die Funkfrequenz kann
einen Zünder auslösen. Die Quelle sagt ausdrücklich, dass das für Sprengfallen,
selbstgebaute Sprengsätze **und Blindgänger** gleichermaßen gilt.

Das ist keine Nebensache, sondern eine Umkehr des Naheliegenden: Wer etwas
findet und als Erstes zum Telefon greift, um Hilfe zu rufen, tut damit genau
das Falsche. Erst weit genug weggehen, dann anrufen.

Der Satz wurde deshalb **auch in den Blindgänger-Eintrag von vorhin
nachgetragen** — er gehört an beide Stellen, weil niemand erst den Minen-Tipp
liest, bevor er eine Granate findet.

### Und was tut man, wenn man schon drin ist

Im Fahrzeug: nicht wenden, nicht aussteigen, langsam **in der eigenen Spur
zurücksetzen**, während jemand aus dem Heckfenster einweist. Zu Fuß gilt
sinngemäß dasselbe — als Einordnung dieses Pakets gekennzeichnet, weil die
Quelle nur vom Fahrzeug spricht.

### Gemessen am 19.08.2026

`mine`, `minen`, `minenfeld`, `tretmine`, `stolperdraht`, `geräumt`, `feldweg`,
`trampelpfad`, `verlassenes haus`, `kampfgebiet` → alle auf
`taktisch-minen-gegenden`. Gegenprobe: `vitamine` bleibt unberührt, `munition`
und `blindgänger` führen weiter auf den Fundmunitions-Eintrag, `gewalt`,
`gefahr`, `flucht` und `kontrollpunkt` unverändert.

Suchbudget: **308 568 von 450 000**, frei 141 432. `packsign pack`: bestanden.


## 19.08.2026 — Beschuss: erst erklären, dann anweisen

Dieselbe Quelle (Roberts, „Staying Alive", IKRK 1999/2005), Abschnitte zu
Artillerie-, Raketen- und Granatwerferbeschuss, S. 51–56, am 19.08.2026 im
Volltext gelesen.

### Warum der Eintrag mit einer Erklärung anfängt und nicht mit einer Anweisung

Weil die beiden Gedanken, die jeder zuerst hat, beide falsch sind — und man
das nur einsieht, wenn man weiß, wie geschossen wird:

**„Das war weit weg."** Beim beobachteten Feuer weist jemand die Geschütze ein
wie einen Wagen in eine Parklücke: erst die Richtung, dann die Entfernung durch
**Eingabeln** — ein Schuss weit darüber, einer weit davor, anfangs 400 bis 800
Meter auseinander, und dieser Abstand wird halbiert, bis er bei etwa 100 Metern
liegt. Bis das Ziel getroffen ist, schlagen also mehrere Geschosse **vorher**
ein. Der erste Einschlag sagt nichts darüber, wo der nächste liegt.

**„Ich bin nicht gemeint."** Stimmt fast immer — und schützt nicht. Ein
Geschoss kann das eigene Haus zufällig treffen, als Teil eben dieser Korrektur.
Beim vorausberechneten Feuer kommt hinzu, dass gar kein bestimmtes Gebäude
gemeint ist: Man nimmt die Ortsmitte und trifft irgendetwas.

### Die Anweisungen, die daraus folgen

Nicht auf den nächsten Einschlag warten. Im Gebäude sofort in den Schutzraum,
alle mitnehmen — **„weiterzuarbeiten ist nicht tapfer, sondern leichtsinnig"**;
nach fünf bis zehn Minuten sieht man, ob es vorbei ist. Und das Zeichen, das
mehr wert ist als jede eigene Einschätzung: **was die Leute vor Ort tun.**

Im Freien und im Fahrzeug die zwei Fälle, die man auseinanderhalten muss:
sehr nah (50–100 m) → aussteigen, neben der Straße flach in Deckung, **nicht
unter das Fahrzeug** (schützt kaum, und das Fahrzeug kann selbst das Ziel
sein); weiter weg und seitlich → zügig weiterfahren und heraus.

Dazu die Frage, die man sich stellt, **bevor** etwas passiert: Wo würde ich
jetzt hin? Wer das erst beim ersten Einschlag überlegt, überlegt zu lange.

### Abgrenzung zum Bestand

„Die Regel der zwei Wände" sagt, **wohin** man geht. Dieser Eintrag sagt,
**wann** und **warum sofort**. Beide verweisen aufeinander, und die Suche ist
so eingestellt, dass `explosion` weiter auf die zwei Wände führt und `beschuss`
hierher.

### Gemessen am 19.08.2026

`beschuss`, `artillerie`, `granatwerfer`, `mörser`, `einschlag`, `einschläge`,
`raketen`, `deckung`, `knall` → alle auf `taktisch-beschuss-verhalten`.

**Eine Wortlücke gefunden und geschlossen:** `bombardierung` fand vorher
**gar nichts**. Jetzt Schlagwort, zusammen mit `beschossen` und `luftangriff`.

Suchbudget: **309 214 von 450 000**. `packsign pack`: bestanden.


## 19.08.2026 — Unterwegs: Marschtempo und Gehen bei Nacht

### Die Lücke stand seit dem 04.08.2026 schriftlich fest

In `work/quellen/buecher/auswertung/sas-handbuch.md` steht sie als **größte
verbliebene**: „ON THE MOVE: Planning, Moving in groups, Pace and progress,
Walking at night … `nachtmarsch`, `gehgeschwindigkeit` ohne Treffer … Es fehlen
realistische Tagesleistung, Marschrhythmus, Gruppentempo nach dem Langsamsten
und warum Gehen im Dunkeln anders funktioniert."

Dort steht auch, das Kapitel müsse noch gescannt werden. **Das stimmte nicht
mehr:** Es liegt in `Buch 04.08.2026 (7).txt` (Scan von 12:32 desselben Tages,
also nach dem Verfassen der Auswertung). Nachgesehen statt angenommen — sonst
wäre die Lücke an einer Scan-Anweisung hängengeblieben, die längst erledigt war.

Gegengemessen vor dem Schreiben: `nachtsicht`, `marschtempo`,
`gehgeschwindigkeit`, `rotlicht`, `nachtmarsch` — alle ohne Treffer.

### Quelle und Rechtslage

**SAS Survival Handbook (John Wiseman)**, Kapitel „On the Move", Buchseiten
377–381, aus der gedruckten Ausgabe eingescannt, am 19.08.2026 im Volltext
gelesen.

Geschützt (Wiseman/HarperCollins), Lizenzanfrage weiterhin offen. Es gilt der am
04.08.2026 festgelegte Grundsatz: **Tatsachen in eigenen Worten, eigene
Gliederung, keine Abbildungen.** Beide Einträge sind danach gebaut.

### „Marschtempo: drei Kilometer in der Stunde, bergauf zwei"

Die Planungszahl und der Drittel-Abzug bergauf. Dazu der Gang selbst
(gleichmäßig, pendelnd; **Hände nicht in die Taschen**, weil man sich sonst beim
Ausrutschen nicht fangen kann; bergauf kürzer, bergab nicht übertreten) und die
Pausen als Teil der Rechnung: **10 Minuten je 30–45 Minuten**, im Sitzen, mit
Blick auf alle.

Der Gruppenteil ist der wertvollere:
* Der Vorderste darf **nicht so schnell gehen, wie er kann**; nach jedem
  Hindernis wird gewartet.
* Aufgaben trennen: **einer sucht den Weg, ein anderer hält die Richtung** —
  denn wer vorn Hindernisse umgeht, verliert die Gesamtrichtung. Beide oft
  ablösen.
* Jeder ist für mindestens einen anderen zuständig; nach Flussquerung und
  schwierigem Stück wird durchgezählt.
* **Und der Satz, der alle überrascht: Nicht der Letzte geht verloren, sondern
  der Erste.** Nach Nachzüglern schaut man; auf den Vordersten nicht. Der
  Mechanismus steht im Eintrag, weil er sonst nicht glaubhaft ist. Gegenmittel:
  alle kennen die Strecke, und **Sammelpunkte** werden vorher benannt.

### „Nachts gehen: vierzig Minuten für die Augen"

**30 bis 40 Minuten** Dunkeladaption — und ein einziger Blick ins helle Licht
setzt sie zurück. Deshalb: bei unvermeidbarem Licht **ein Auge zuhalten**, für
die Karte **rotes Licht**.

Der Trick, den kaum jemand kennt: **nicht direkt hinsehen.** In einer dunklen
Masse ist mittig nichts auszumachen; Ränder und das, was am Rand des Blickfeldes
liegt, erscheinen deutlicher.

Dazu: im Wald ist es immer dunkler als im Offenen; langsam gehen und **jeden
Schritt prüfen**, bergab schlurfen; und die Ohren als das im Dunkeln bessere
Werkzeug — ein Fluss verrät am Geräusch, wie schnell er fließt.

**Einordnung dieses Pakets:** Das Handy als Lampe ist im Gelände schlecht — es
blendet, und danach ist man eine halbe Stunde schlechter dran als vorher.

### Gemessen am 19.08.2026

`marschtempo`, `gehgeschwindigkeit`, `langsamster`, `steigung`, `marsch` →
`orientierung-marschtempo`. `nachtmarsch`, `nachtsicht`, `rotlicht`,
`taschenlampe`, `stolpern` → `orientierung-nachts-gehen`.

Gegenprobe: `kompass`, `polarstern` und `entfernung` führen unverändert auf die
alten Einträge.

Suchbudget: **310 329 von 450 000**. `packsign pack`: bestanden.
Orientierung wächst damit von 16 auf 18 Einträge.


## 19.08.2026 — Das Auto: versinken und Bremsversagen

Aus dem SAS-Handbuch, Kapitel „Strategy", Buchseiten 53–54, am 19.08.2026
gelesen. Die Lücke stand seit dem 04.08.2026 in der Buchauswertung
(`autounfall`, `angeschnallt` ohne Treffer) — und sie ist für Mitteleuropa
wahrscheinlicher als alles andere, was diese Nacht dazugekommen ist.

### Die Texterkennung hat versagt, das Seitenbild nicht

Zwei Absätze — die Schutzhaltung und das Herausspringen — waren im OCR-Text
zerfallen (`aston pel, the Paes your OMtO the bay Ms`). **Statt zu raten, wurde
die Seite als Bild aus dem PDF geholt und abgelesen.** Dasselbe Verfahren, das
am 04.08.2026 schon bei den Notgeburt-Seiten geholfen hat.

Dabei kam eine Vorsichtsregel zum Vorschein, die im Text gar nicht mehr stand:
**neben dem Wasser parken, nicht darauf zu** — und wenn es nicht anders geht,
mit der Front zum Wasser den Rückwärtsgang und die Handbremse, mit dem Heck zum
Wasser den ersten Gang und die Handbremse.

### „Auto versinkt: das Fenster ist der Ausgang, nicht die Tür"

Der Kern ist eine Physikfrage, die über Leben entscheidet: **Die Tür geht nicht
auf, solange innen Luft ist** — von außen drückt das Wasser dagegen. Also raus
durch das **Fenster**, solange es geht.

Und wenn es dafür zu spät ist, kommt die Reihenfolge, die dem Gefühl komplett
widerspricht: **Fenster fest schließen**, Kinder aufstehen lassen und Babys zum
Dach heben, Gurte lösen, **Zentralverriegelung sofort entriegeln** (Wasser
legt die Elektrik lahm), Hand an den Griff — **aber die Türen noch nicht
öffnen**. Erst wenn der Wagen zur Ruhe gekommen und fast voll ist: tief
einatmen, Türen auf, ausatmend nach oben, und **wer durch dieselbe Tür geht,
hakt sich unter**.

Das Ausatmen beim Aufsteigen ist als Einordnung dieses Pakets gekennzeichnet —
die Quelle sagt es, begründet es aber nicht.

### „Bremsen weg: herunterschalten, Handbremse in Stößen"

Fuß vom Gas, Warnblinker, Bremspedal pumpen, herunterschalten, Handbremse —
**nicht durchreißen, sondern in sanften Stößen**, sonst blockiert es. Ausweichen
auf eine weiche Böschung oder eine Abzweigung, die **bergauf** führt. Wird es
nicht langsamer: an Hecke oder Mauer entlangschrammen; ein vorausfahrendes
Fahrzeug darf man zum Anhalten benutzen — aber **vorher warnen**.

Beim unvermeidbaren Zusammenstoß: **dabeibleiben und lenken**, und lieber in
etwas fahren, das nachgibt, als gegen einen Baum. Zum Airbag: nicht zu nah am
Lenkrad, und das Lenkrad so neigen, dass er auf den **Brustkorb** zeigt.

**Einordnung dieses Pakets:** Das Buch stammt aus der Zeit von Schaltgetriebe
und mechanischer Handbremse. Bei Automatik nimmt man die Fahrstufe mit
Motorbremswirkung, bei einer elektrischen Feststellbremse muss man den Schalter
**gezogen halten** — dann bremst sie geregelt, statt zu blockieren.

### Der Titelwächter hat zum zweiten Mal in dieser Nacht angeschlagen

Der Eintrag hieß zuerst **„Auto im Wasser: …"**. Damit stand das reservierte
Wort *Wasser* im Titel, wo es fünf Punkte wiegt — und die Messung bestätigte es
sofort: Die Eingabe `wasser` führte zuerst auf das versinkende Auto statt auf
die Wasseraufbereitung. In einer Lage, in der jemand *wasser* tippt, ist das
die falsche Seite.

Neuer Titel ohne das Wort: **„Auto versinkt: …"**. Die Stichwörter enthalten
`auto im wasser` weiterhin, also findet es trotzdem, wer es so sucht. Die
Gegenprobe steht jetzt fest im Test.

### Gemessen am 19.08.2026

`auto versinkt`, `ertrinken im auto`, `wagen im see`, `türverriegelung` →
`taktisch-auto-im-wasser`. `bremsen versagen`, `bremsversagen`, `handbremse`,
`herunterschalten`, `zusammenstoß`, `airbag` → `taktisch-bremsen-versagen`.
Gegenprobe: `wasser` und `hochwasser` unverändert.

Suchbudget: **311 319 von 450 000**. `packsign pack`: bestanden.


## 19.08.2026 — Wolken lesen

Aus dem SAS-Handbuch, Kapitel „Reading the Signs", Abschnitte „Weather signs"
und „Clouds", Buchseiten 364–369, am 19.08.2026 im Volltext gelesen. Geschützt
(Wiseman/HarperCollins) — Tatsachen in eigenen Worten, eigene Gliederung, keine
Abbildungen.

Vorher ohne Treffer: `wolken`, `wettervorhersage`, `wetterumschwung`, `halo`,
`gewitter kommt`.

### Warum das mehr ist als Wolkenkunde

Der Eintrag verzichtet auf die lateinischen Namen. Sie stehen in der Quelle, und
sie helfen niemandem, der bei aufziehendem Wetter in den Himmel sieht. Stattdessen
steht dort, **wie die Wolke aussieht** und **was in den nächsten Stunden
passiert**.

Die tragende Regel ist eine einzige: **Je höher die Wolken, desto besser das
Wetter.** Alles andere ist ihre Verfeinerung.

### Die drei Stellen, die den Eintrag tragen

1. **Der Hof um Sonne oder Mond.** Nur eine einzige Wolkenart macht ihn. **Wird
   der Ring größer, bleibt es schön; wird er kleiner, kommt Regen.** Das ist das
   einzige Zeichen im Eintrag, das man am selben Abend nachprüfen kann — und es
   verlangt zwei Blicke mit Abstand.
2. **Vier bis fünf Stunden.** Tiefe dunkle Decken bedeuten Regen oder Schnee
   innerhalb von vier bis fünf Stunden, und dann meist stundenlang. Eine Zahl,
   nach der man ein Lager aufschlagen oder weitergehen kann.
3. **Der Übergang vom Harmlosen zum Gewitter.** Weiße Haufen mit Abstand sind
   Schönwetter — dieselben Haufen, wenn sie in die Höhe wachsen und mehrere Köpfe
   bilden, sind die Ankündigung. Das Gewitter selbst erkennt man am flach
   auslaufenden Amboss.

### Einordnung dieses Pakets

Ausdrücklich dazugeschrieben: Diese Zeichen gelten für die nächsten Stunden,
nicht für morgen, und sie ersetzen keinen Wetterbericht — sie sind das, was
bleibt, wenn keiner mehr kommt. Und der Satz, der die ganze Sammlung
zusammenhält: **Fast jede Regel hier ist eine Regel über Veränderung.** Der Ring
wächst oder schrumpft, der Schleier verdichtet sich, der Haufen wächst. Ein
einzelner Blick ist keine Vorhersage.

### Gemessen am 19.08.2026

`wolken`, `wetter`, `wettervorhersage`, `halo`, `hof um den mond`,
`wetterumschwung`, `schäfchenwolken`, `amboss` → `taktisch-wolken-lesen`.

Gegenprobe, und sie ist wichtig: `gewitter` führt weiter auf
`erste-hilfe-blitzschlag` und `sturm` auf `taktisch-sturm-verhalten`. Im Notfall
zählt der Blitzschlag, nicht die Wolkenkunde.

Suchbudget: **311 864 von 450 000**. `packsign pack`: bestanden.


## 19.08.2026 — Hinterhalt

Roberts, „Staying Alive", IKRK 1999/2005, Abschnitte zum Hinterhalt (Vorbeugung,
Reaktion, das Antreffen eines gerade erfolgten Überfalls), S. 61–63, am
19.08.2026 im Volltext gelesen. © IKRK, nur Beleg.

Damit ist der Bedrohungsteil der Kategorie „Taktisch" abgedeckt, den das
ROADMAP am 28.07.2026 als offen benannt hatte.

### Warum der Eintrag mit einer Frage anfängt und nicht mit einer Anweisung

Weil die Quelle selbst das tut, und weil es stimmt: **Wenn ernsthaft möglich
ist, dass jemand gezielt wartet — muss die Fahrt dann überhaupt sein?**
Trotzdem loszufahren ist nicht mutig, sondern leichtsinnig. Alles, was danach
kommt, nennt die Quelle ausdrücklich „measures of last resort".

### Der Teil, den man üben kann, ohne dass je etwas passiert

Das Gelände lesen. Die Quelle leitet es sauber her: Wer einen Hinterhalt legt,
braucht **zwei** Dinge — Überraschung **und einen eigenen Fluchtweg**. Daraus
ergeben sich die Stellen von selbst:

* scharfe Kurve oben an einer steilen Steigung,
* schlechtes Straßenstück, das zum Langsamfahren zwingt,
* Wald oder dichtes Gelände daneben.

Und beiläufig mitdenken, **wo man selbst wegkäme**.

### Die Reaktionen, und warum sie sich widersprechen dürfen

* **Beschossenes Fahrzeug: durchfahren**, so schnell es geht, alle flach.
* **Fahrzeug steht: raus und auseinander**, in möglichst viele Richtungen, und
  weiterlaufen. Eine Gruppe ist ein Ziel, Einzelne sind viele Ziele.
* **Zweites Fahrzeug: sofort heraus** — nicht hineinfahren, um zu helfen.

Der dritte Punkt ist der, der einem am schwersten fällt und deshalb ausdrücklich
dasteht.

### Zweiter Zweig und Einordnung

Die Quelle schreibt für Helfer mit zwei Fahrzeugen, Funk und einem Zeichen am
Wagen. Für Zivilpersonen bleibt vor allem der erste Teil — die Frage, das
Geländelesen, das Merken von Fluchtwegen. Zu Fuß ändern sich nur die Mittel:
nicht dort gehen, wo man von oben und aus Deckung eingesehen wird.

### Gemessen am 19.08.2026

`hinterhalt`, `aufgelauert`, `konvoi`, `engstelle`, `kurve`,
`beschossen unterwegs` → `taktisch-hinterhalt`; `überfall` findet ihn neben
`taktisch-anzeichen-gewalt`, was richtig ist. `fluchtweg` führt jetzt hierher
statt auf den Baumfäll-Tipp — auch das ist richtig.

Gegenprobe unverändert: `gewalt`, `beschuss`, `kontrollpunkt`, `flucht`,
`gefahr`.

Suchbudget: **312 469 von 450 000**. `packsign pack`: bestanden.



## 19.08.2026 — Chemie: ein blinder Fleck, und ein Wächter, der noch fehlte

Vor dem Schreiben gemessen: `chemieunfall`, `kampfstoff`, `reizgas`,
`dekontamination`, `giftwolke`, `industrieunfall` — **alle ohne Treffer**.
Radiologisch war das Paket versorgt (Atomschlag, Jodtabletten, Trinkwasser
danach), chemisch gar nicht. Für Mitteleuropa ist das die falsche Verteilung:
Ein Tanklaster, ein Betrieb oder eine Bahnverladung ist hier um Größenordnungen
wahrscheinlicher als ein Kampfstoff.

Quelle: Roberts, „Staying Alive", IKRK 1999/2005, Kapitel zu chemischen,
biologischen und radiologischen Bedrohungen, S. 79–85, am 19.08.2026 im
Volltext gelesen. © IKRK, nur Beleg.

### „Giftwolke: gegen den Wind, nicht mit ihm"

Der erste Satz ist der wichtigste: **Man riecht es meist nicht.** Die meisten
Kampfstoffe sind praktisch geruchlos, Strahlung ist unsichtbar und ihre Zeichen
kommen verzögert. Wer sich auf die Sinne verlässt, handelt zu spät.

Deshalb die Anzeichenliste aus der Quelle — viele Betroffene auf einmal,
Übelkeit und Atemnot und Krämpfe, **Vögel und Insekten, die vom Himmel
fallen**, ungewöhnlicher Nebel oder ein öliger Film, und die vier Gerüche, die
dort ausdrücklich stehen: bittere Mandeln, Pfirsichkerne, frisch gemähtes Heu,
grünes Gras. **Je mehr davon zusammenkommt, desto sicherer.**

Und das Zeichen, das die Quelle als das deutlichste benennt: **wenn
Einsatzkräfte anfangen, Schutzausrüstung anzulegen.**

Die Sofortmaßnahmen samt der beiden unbequemen: **niemanden anfassen** (auch
keine Betroffenen — wer selbst zum Fall wird, hilft niemandem mehr) und das
**nasse Tuch** vor Mund und Nase, wenn keine Maske da ist. Dann die Richtung,
die dem Eintrag den Namen gibt: **gegen den Wind**, im Fahrzeug rund acht
Kilometer, und danach neu beurteilen — **der Wind dreht**.

### „Abgedichteter Raum: bei Chemie nach oben, sonst nach unten"

Der Eintrag steht da, weil er alles Gelernte umkehrt, und die Quelle sagt das
selbst: **Bei Chemie nach oben**, weil die meisten gefährlichen Gase schwerer
als Luft sind und sich unten sammeln — **bei Strahlung und Beschuss nach
unten**, weil dagegen Masse hilft.

Dazu das Abdichten (Band und Folie, Watte in Ritzen, nasses Tuch unter der Tür,
**Lüftung und Heizung aus**) und die Reinigung vor dem Betreten mit den drei
Angaben, die man sonst nirgends findet: **Spülen–Abwischen–Spülen**, **kaltes**
Wasser (warmes lässt den Stoff verdunsten, dann atmet man ein, was man
abwäscht), und bei Wassermangel **Talkum oder Mehl, dreißig Sekunden,
abbürsten**.

**Einordnung dieses Pakets:** Ein abgedichteter Raum ist dicht auf Stunden, nicht
auf Tage — die Luft wird knapp. Er überbrückt den Durchzug einer Wolke, er
ersetzt kein Weggehen.

### Der Wächter, der noch fehlte

Der zweite Eintrag hieß zuerst „… bei Chemie nach oben, **bei Strahlung** nach
unten". Die Suchmessung zeigte sofort: Damit stand er bei der Eingabe
`strahlung` **vor** `erste-hilfe-strahlung`. Im Ernstfall die falsche Seite.

**Der Titelwächter hat das nicht gemeldet — er kannte das Wort nicht.** Es stand
nicht in seiner Liste reservierter Wörter. Gefunden hat es die Messung.

Zwei Änderungen daraus:

1. Der Titel führt das Wort nicht mehr.
2. **`strahlung` steht jetzt in der Wächterliste**, mit seinen rechtmäßigen
   Trägern: `erste-hilfe-strahlung`, `strahlung-jodtabletten`,
   `taktisch-wohin-schutz`, `wasser-nach-atomschlag`,
   `nahrung-atomschlag-tiere`, `nahrung-atomschlag-pflanzen`. Beim Eintragen
   hat der Wächter diese Bestandsfälle prompt gemeldet — sie tragen das Wort zu
   Recht, und das steht jetzt schwarz auf weiß da.

### Gemessen am 19.08.2026

`giftwolke`, `chemieunfall`, `gasaustritt`, `kampfstoff`, `reizgas`,
`industrieunfall`, `tanklaster`, `gaswolke` → `taktisch-giftwolke`.
`abgedichteter raum`, `schutzraum`, `abdichten`, `fenster abkleben`,
`dekontamination`, `lüftung abschalten` → `taktisch-abgedichteter-raum`.

Gegenprobe festgeschrieben: `strahlung` und `atomschlag` führen auf
`erste-hilfe-strahlung`, `sirene` auf `taktisch-sirene-verstehen`,
`jodtabletten` auf die Jodtabletten.

Suchbudget: **313 444 von 450 000**. `packsign pack`: bestanden.



## 19.08.2026 — Kaltes Wasser: was der Bestand schon konnte, und was fehlte

### Zuerst das, was NICHT geschrieben wurde

Gemessen wurde nach „Eis eingebrochen", „ins Wasser gefallen" und
„Kälteschock". Die ersten beiden fanden nichts — der dritte fand
`erste-hilfe-eiseinbruch-selbstrettung`, **und der Eintrag ist besser als die
Buchvorlage**: Er kennt das Zeitfenster zwischen Kälteschock und dem
Steifwerden der Arme, die drei Minuten, und warum die Arme zuerst auskühlen.

**Also nicht geschrieben.** Zwei Suchwörter ohne Treffer sind kein Beweis für
eine Lücke; erst der dritte hat den Bestand sichtbar gemacht. Das ist genau der
Merksatz, den die Buchauswertung vom 04.08.2026 schon festhält: vor jeder
Scan-Anweisung wird gemessen.

### Was wirklich fehlte

Der Fall, in dem man **nicht herauskommt**: gekentertes Boot, Hochwasser, Sturz
ohne Eiskante zum Festhalten. Dazu stand nichts im Paket.

Quelle: **Alton, „The Ultimate Survival Medicine Guide"**, Skyhorse Publishing,
Kapitel zu Umwelteinflüssen, Buchseiten 106 f., aus der eingescannten
gedruckten Ausgabe am 19.08.2026 gelesen. Geschützt — Tatsachen in eigenen
Worten.

### „Nicht schwimmen, treiben: Kleidung an, Knie an die Brust"

Der Satz, den fast alle falsch haben: **Die Kleidung bleibt an.** Im Wasser
steht zwischen Stoff und Haut eine dünne Schicht, die der Körper anwärmt, und
die dämmt. Zumachen statt ausziehen — ausgezogen wird erst draußen, dann
allerdings sofort, weil sich die Rechnung an der Luft umkehrt.

Dazu: Knie an die Brust (schützt den Rumpf), zu mehreren einander zugewandt in
einen engen Kreis, jeden Zentimeter aus dem Wasser bringen, den man kann — und
die Entscheidung, die man nur einmal trifft: **nicht losschwimmen**, außer es
gibt ein trockenes Ziel, das man sicher erreicht.

### Und eine Ergänzung am Bestand

`taktisch-eis-und-schnee-gehen` sagte, wie man auf dünnem Eis geht, aber nicht,
**woran man es sieht**. Aus derselben Quelle nachgetragen, und es ist verdreht,
wie man es erwartet: **Liegt Schnee auf einer dünnen Stelle, wirkt sie dunkler;
blankes dünnes Eis wirkt heller.** Antwort auf beides: Wo die Fläche die Farbe
wechselt, wird nicht gegangen. Die Quelle steht jetzt als zweiter Beleg an
diesem Eintrag.

### Der Titel hat zum dritten Mal die Suche gekapert

Der Eintrag hieß zuerst „Im **kalten Wasser** treiben …" — und hat damit gleich
drei Anfragen an sich gezogen:

| getippt | ging zu | gehört zu |
|---|---|---|
| `wasser` | dem neuen Eintrag | `wasser-truebes-wasser-vorbehandeln` |
| `kalt` | dem neuen Eintrag | `erste-hilfe-unterkuehlung-stadium-eins` |
| `auskühlen` | dem neuen Eintrag | `erste-hilfe-waermeerhalt` |

Die letzten beiden **nicht über den Titel, sondern über Schlagwörter**, die mit
denselben Buchstaben anfangen (`kaltes wasser`, `auskühlen im wasser`). Die
Suche vergleicht Wortanfänge — das trifft man leicht, ohne es zu merken.

Titel und Schlagwörter geändert, alle drei Anfragen wieder an ihrem Platz, und
die Gegenproben stehen fest im Test.

### Gemessen am 19.08.2026

`ins wasser gefallen`, `über bord`, `gekentert`, `schwimmweste`,
`help-stellung`, `im wasser warten`, `treiben` → der neue Eintrag.
`eingebrochen` und `eiswasser` → weiter die Selbstrettung, die der dringlichere
Fall ist. `dünnes eis` → `taktisch-eis-und-schnee-gehen`.

Suchbudget: **313 931 von 450 000**. `packsign pack`: bestanden.


## 19.08.2026 — Verirrt: der wichtigste fehlende Eintrag, und ein Fehler in meiner Messweise

### Zuerst der Fehler, weil er die ganze Nacht betrifft

Lücken habe ich bis zu dieser Runde gesucht, indem ich **Titel und Fließtext**
aller Einträge nach einem Wort durchsucht habe. Das geht an den
**Schlagwörtern** vorbei — und damit an dem, wonach die Suche zuerst greift.

Aufgefallen ist es am Fettbrand: Die Textsuche sagte „nichts vorhanden", und ich
war schon dabei, einen Eintrag zu schreiben. Tatsächlich steht `fettbrand` als
Schlagwort in „Es brennt: die Reihenfolge, in der gehandelt wird" — samt
Fettexplosion und Brandklasse F. **Der Eintrag wäre eine Dublette geworden.**

Ab jetzt wird mit der Suche selbst gemessen, nicht mit einer Textsuche. Der
Unterschied ist keine Feinheit: Von zwölf geprüften Alltagslagen sahen mit der
Textsuche neun nach einer Lücke aus, mit der echten Suche waren es vier.

### Die echte Lücke, die dabei herauskam

**`verirrt` führte auf `orientierung-schattenstock`** — also auf die Anleitung,
wie man mit einem Stock Norden findet. Wer sich verlaufen hat, fragt aber nicht
nach Norden. Er fragt, was er JETZT tun soll. Für eine App, die Compass Zero
heißt, war das die wichtigste offene Stelle.

### Quelle

**US Army, „FM 3-05.70 Survival", Ausgabe 2002, Kapitel 1** (das Merkwort
SURVIVAL), am 19.08.2026 im Volltext gelesen. Werk der US-Bundesregierung und
damit gemeinfrei (17 U.S.C. § 105) — dieselbe Quelle, die schon den
Kaliumpermanganat-Eintrag trägt. Gelesen in einer Textfassung des Handbuchs;
die PDF-Ausgabe der FAS lieferte eine leere Antwort.

### „Verirrt: stehen bleiben, bevor du weitergehst"

Der Eintrag folgt der Reihenfolge der Quelle und lässt weg, was nur militärisch
ist (Feindlage, Gefangennahme).

**Der erste Schritt ist kein Schritt.** Die Quelle begründet es genau: Wer
schnell reagiert, ohne zu denken, macht den falschen Zug; in der Eile verliert
man Ausrüstung — und man verliert die Übersicht so weit, dass man **nicht mehr
weiß, in welche Richtung man überhaupt gehen soll**. Sich zu bewegen, nur um
sich zu bewegen, macht es schlechter.

**Furcht und Panik sind die größten Gegner.** Unbeherrscht zerstören sie die
Fähigkeit zu entscheiden, und man reagiert auf die **Einbildung** statt auf die
**Lage**. Dazu kosten sie Kraft.

Dann die Bestandsaufnahme in der Reihenfolge der Quelle — Umgebung, eigener
Zustand, Ausrüstung — und erst danach der Plan, nach Wasser, Schutz, Essen.

**Und die zwei Sätze zur Vorbeugung, die mehr wert sind als alles andere:** sich
unterwegs ständig selbst orientieren, und sich **nicht darauf verlassen, dass
ein anderer den Weg mitverfolgt**. In einer Gruppe tun das alle gleichzeitig —
und dann hat es keiner getan. Das passt genau zu dem Fund aus der
Marschtempo-Runde: Nicht der Letzte geht verloren, sondern der Erste.

### Eine Änderung am Bestand, gemessen und begründet

Der Schattenstock-Eintrag führte `verlaufen` und `verirrt` als Schlagwörter.
Solange es nichts anderes gab, war das vertretbar; jetzt schickt es an die
falsche Stelle. **Beide entfernt** — der Eintrag bleibt über `norden finden`,
`himmelsrichtung`, `schatten`, `ohne kompass` und `wo bin ich` erreichbar, und
`norden finden` führt weiterhin zuerst auf ihn.

### Gemessen am 19.08.2026

| getippt | vorher | jetzt |
|---|---|---|
| `verirrt` | `orientierung-schattenstock` | **`orientierung-verlaufen`** |
| `verlaufen` | `orientierung-schattenstock` | **`orientierung-verlaufen`** |
| `orientierung verloren` | nichts | **`orientierung-verlaufen`** |

Gegenprobe festgeschrieben: `wo bin ich` → `orientierung-kreuzpeilung`
(die Frage nach dem Standort), `norden finden` → `orientierung-schattenstock`,
`fettbrand` → `erste-hilfe-brand-reihenfolge`.

Suchbudget: **314 456 von 450 000**. `packsign pack`: bestanden.


## 19.08.2026 — Zeichen am Himmel, und eine Berichtigung am eigenen Eintrag von gestern

Quelle: SAS-Handbuch, Kapitel „Reading the Signs", Abschnitte „Weather signs"
und „Signs in the sky", Buchseiten 364–371, am 19.08.2026 im Volltext gelesen.
Geschützt (Wiseman/HarperCollins) — Tatsachen in eigenen Worten.

Vor dem Schreiben **mit der Suche** gemessen (nicht mehr per Textsuche, siehe
den Eintrag zum Verirren): `abendrot`, `morgenrot`, `talnebel`, `nebel im tal`,
`regenschatten`, `regenbogen`, `wo lager aufschlagen` — **alle ohne Treffer**.

### Was den Eintrag trägt

**Der Talnebel, weil er eine Uhrzeit hat.** Hebt sich der Morgennebel aus dem
Tal, ist schönes Wetter sicher. Steht er im Bergland **mittags immer noch**,
bleibt er den Tag über und geht am späten Nachmittag wahrscheinlich in Regen
über. Man weiß also spätestens zu Mittag, woran man ist — das ist mehr, als die
meisten Zeichen hergeben.

**Die klare Nacht, und was daraus für den Lagerplatz folgt.** Klar heißt
beständig — und zum Sommerende zugleich Frostgefahr, weil Wolken die Wärme wie
eine Decke halten. Daraus der Satz, der über die Nacht entscheidet: **Kalte Luft
ist schwer und sammelt sich in den Senken.** Wer sich einen Platz sucht, legt
sich nicht in die Mulde — auch wenn sie windgeschützt aussieht und gerade
deshalb einladend wirkt.

**Der Regenschatten mit seiner Umkehrung.** Berge zwingen Luft nach oben, sie
regnet sich an den Hängen ab, dahinter ist es trocken. Die Quelle sagt aber
ausdrücklich dazu, was **nicht** folgt: Aus dem Trockenen über den Grat zu
steigen führt nicht zwangsläufig ins Grüne. Genau diesen Satz braucht jemand,
der auf Wassersuche über einen Kamm will.

### Die Berichtigung

Im Eintrag „Wolken lesen" von heute Nacht stand: „**Nur EINE** Wolkenart macht
einen Ring um Sonne oder Mond, ein hoher Schleier aus Eisteilchen."

Beim Weiterlesen derselben Quelle zeigte sich: Das stimmt so nicht. Die Quelle
nennt den weißen **Hof** aus Eisteilchen (nur Cirrostratus) **und** einen
farbigen **Ring** dichter an der Sonne, der aus Wassertröpfchen entsteht. Beide
folgen derselben Regel — wächst er, wird es gut; schrumpft er, kommt Regen.

Der Satz ist berichtigt: weißer Ring aus Eis, farbiger Ring aus Tröpfchen,
gleiche Regel. **Eine Aussage, die der eigenen Quelle widerspricht, wird nicht
dadurch zulässig, dass sie vorsichtig klingt** — und hier war sie nicht einmal
vorsichtig, sondern nur ungenau.

### Gemessen am 19.08.2026

`abendrot`, `morgenrot`, `roter himmel`, `talnebel`, `nebel im tal`,
`regenschatten`, `regenbogen`, `corona`, `senke` → `taktisch-himmelszeichen`.

Abgrenzung festgeschrieben: `wolken` und `hof um den mond` führen weiter auf
„Wolken lesen", `gewitter` auf den Blitzschlag, `sturm` auf das Sturmverhalten.

Suchbudget: **315 026 von 450 000**. `packsign pack`: bestanden.

### Gemessene Lücke, die offen bleibt

**Eine stinkende Wunde.** `wunde stinkt` und `gangrän` finden nichts, und die
naheliegenden Einträge helfen nicht weiter: „Totes Gewebe erkennen" prüft vier
Dinge — Farbe, Festigkeit, Zucken, Durchblutung — und **nennt den Geruch
nicht**, weil die Quelle ihn in ihrer Tabelle nicht führt. Für einen Laien ist
der Geruch aber oft das Erste, was auffällt.

Hier wurden **bewusst keine Schlagwörter nachgetragen**: Sie würden auf einen
Eintrag zeigen, der die Frage nicht beantwortet. Das braucht eine Quelle, die
den Geruch als Zeichen führt — und die liegt nicht im Haus. Notiert, nicht
geraten.


## 19.08.2026 — Ein Kind, das nicht atmet, wurde nicht gefunden

Keine neue Quelle, sondern eine Berichtigung an vorhandenen Einträgen — und die
schwerwiegendste dieser Nacht.

### Wie es auffiel

Nachgestellt, was jemand unter Druck tippt: 25 Anfragen aus ganzen Wörtern statt
aus einzelnen Stichwörtern. **Sieben blieben komplett leer** — darunter „mein
kind atmet nicht", „hund hat mich gebissen" und „deckung suchen", obwohl es zu
jeder einen passenden Eintrag gibt. „person atmet nicht mehr" fand die
Atemkontrolle dagegen auf Anhieb. Ein Wort mehr im Satz entschied darüber, ob
das Handbuch antwortet oder schweigt.

### Drei Fehler, alle beim selben Fall

* **„kind atmet nicht" führte auf die Lungenentzündung.** Deren Titel „Wenn ein
  Kind zu schnell atmet" trägt „Kind" UND „atmet" im stärksten Suchfeld; die
  Wiederbelebung stand nicht einmal unter den ersten vier.
* **„kind bewusstlos" führte auf die Hirnhautentzündung** — das Wort
  „bewusstlos" kam im Wiederbelebungs-Eintrag gar nicht vor.
* **„kind reanimieren" fand nichts.** Im Eintrag steht „Reanimation", und die
  Suche kennt nur Wortanfänge: Das längere getippte Wort passt nicht auf das
  kürzere geschriebene.

### Was geändert wurde

**Der Titel heißt jetzt nach der Lage statt nach der Maßnahme:**
„Wiederbelebung Kind: zuerst beatmen" → **„Kind atmet nicht: zuerst beatmen"**.
Der Titel ist das am stärksten gewichtete Suchfeld — er gehört dem, wonach
gesucht wird, nicht dem Namen des Handgriffs. Dazu die fehlenden Wörter als
Schlagwörter (`atmet nicht`, `reagiert nicht`, `bewusstlos`, `reanimieren`,
`säugling`) und das ausgeschriebene Zitat im Keuchhusten-Eintrag nachgezogen.

Am Inhalt der Anweisung selbst wurde **nichts** geändert; die ERC-Leitlinie 2025
bleibt unverändert die Quelle.

### Gegenprobe, festgeschrieben

Ein Kind, das **zu schnell** atmet, bleibt bei der Lungenentzündung (24 zu 19
Punkten). „atmet nicht", „bewusstlos" und „person atmet nicht mehr" bleiben beim
Erwachsenen-Eintrag zur Atemkontrolle. In der App nachgesehen: „mein kind atmet
nicht" ergibt 26 Treffer, ganz oben der richtige.

Nebenbei berichtigt: ein gerades Anführungszeichen in der Quellenangabe zur
ERC-Leitlinie.


## 19.08.2026 — Das Wort des Laien und das Wort des Handbuchs

Keine neuen Quellen, sondern eine ganze Reihe von Einträgen, die es gab, die
aber unter dem Wort nicht zu finden waren, mit dem jemand danach sucht. Alles
mit der echten Suche gemessen, nicht per Textsuche.

### Was nicht gefunden wurde

| Eingetippt | Was kam | Was hätte kommen müssen |
|---|---|---|
| `blutung stoppen` | goldene Stunde, Schlangenbiss | Blutung stillen |
| `starke blutung` | Regelblutung | Blutung stillen |
| `unter strom` | Lagern ohne Strom | Stromunfall |
| `person im wasser` | Hexenschuss | Ertrinken |
| `fluchtrucksack` | **nichts** | Notgepäck |
| `ich habe angst` | Bluthochdruck | Angst und Panik |
| `kind hat angst` | Fieber, Pseudokrupp | Mit Kindern sprechen |
| `handy tot` | Unterkühlung III, Tote bergen | Was der Ausfall mitnimmt |
| `brunnenwasser trinken` | Hasenpest beim Verarbeiten | Brunnen nach Hochwasser |
| `regenwasser trinken` | Brunnen, Hitzschlag | Regen vom Dach |

### Das Muster dahinter

Drei verschiedene Ursachen, und keine davon war eine Wissenslücke:

1. **Das Handbuch benutzt das Fachwort, der Suchende das Alltagswort.**
   „stillen" gegen „stoppen", „Notgepäck" gegen „Fluchtrucksack".
2. **Ein Wort gibt es zweimal.** Strom als Versorgung und Strom als Unfall,
   Wasser als Vorrat und Wasser als Gefahr. Der Notfall muss vorn stehen — wer
   nur die Versorgung sucht, hat Zeit zum Weiterlesen.
3. **Zusammengesetzte Wörter treffen ihre Teile nicht.** Die Suche kennt
   Wortanfänge; „brunnenwasser" findet „Brunnen" nicht, und „Lagerfeuer" wird
   von „feuer" nicht gefunden.

### Gegenproben, festgeschrieben

Die Regelblutung behält `regelblutung`, `starke regelblutung`, `periode zu
stark`. Der Stromausfall behält `kein strom` und `strom ist weg`. Die
Angst-Übungen bleiben erster Treffer zu `angst` und `panik`, der Kinder-Eintrag
steht dahinter, nicht davor.


## 19.08.2026 — Wo die Nacht verbracht wird, und zwei Wörter fürs Marschtempo

Quelle: SAS-Handbuch, Kapitel „On the Move", Abschnitt „Upland travel",
Buchseiten 380–382, am 19.08.2026 im Volltext gelesen. Geschützt
(Wiseman/HarperCollins) — Tatsachen in eigenen Worten.

Vor dem Schreiben **mit der Suche** gemessen: `wo übernachten` fand **nichts**,
`unterschlupf` führte auf die **Tollwut**, `wo schlafen` auf Obstbäume und aufs
Gerben ohne Rinde, `kaltluft` und `gehtempo` fanden nichts. Die
Unterstand-Anleitungen gibt es längst — aber Anleitungen haben in diesem Format
keine Schlagwörter, also war über diese Wörter nichts zu erreichen.

### Was der Eintrag trägt

**Die Entscheidung geht gegen das Gefühl.** Nicht bis auf den Talgrund
absteigen, wenn Schutz und Wasser schon auf dem Weg dorthin zu finden sind: Das
spart die Kraft für Abstieg und Wiederaufstieg — und man liegt oben
wahrscheinlich wärmer, weil sich am Talboden **Kaltluftseen** sammeln. Das Tal
sieht windgeschützt und einladend aus, und genau das ist die Falle.

**Was die Entscheidung kippt, steht ausdrücklich dabei, in beide Richtungen:**
Wer Wasser und Baumaterial trägt, bleibt oben und sucht dort die geschützteste
Stelle. Wer auf einem sehr ausgesetzten Grat weder das eine noch das andere hat,
muss für die Nacht doch absteigen. Ein windgepeitschter Grat ohne Unterstand ist
schlechter als ein kaltes Tal mit einem.

**Und die Zeitregel:** den Platz suchen, BEVOR Licht und Kraft aufgebraucht
sind, nicht wenn. Beides schwindet langsam, und beides bemerkt man zu spät.

Der Absatz zu Wind, Boden und Nässe ist als **Einordnung dieses Pakets**
gekennzeichnet und steht nicht so in der Quelle.

### Zwei Artikel, die ich NICHT geschrieben habe

`nachts gehen`, `nachtsicht`, `rotlicht`, `augen gewöhnen` führen alle auf
„Nachts gehen" — vollständig abgedeckt, obwohl dieselbe Quelle dazu eine ganze
Seite hat. Und `gehtempo` fand nichts, obwohl „Marschtempo: drei Kilometer in
der Stunde" seit langem dasteht und alles enthält. **Zwei Schlagwörter statt
eines zweiten Eintrags.**

Stand: 413 Tipps. Gegenprobe festgeschrieben: Die Wetterzeichen behalten `senke`
und `lagerplatz`, denn dort steht das Warum.

### Nachtrag zur Runde davor

Bei Ohrenschmerzen, Blutdruckmitteln und „zucker zu hoch" lag dieselbe Ursache
zugrunde wie bei „kind reanimieren": Die Suche kennt nur **Wortanfänge**. Ein
längeres getipptes Wort passt nicht auf ein kürzeres geschriebenes — die
Mehrzahl findet die Einzahl nicht, und ein zusammengesetztes Wort trifft seine
Teile nicht. Sieben Schlagwörter nachgetragen; über- und Unterzuckerung wurden
ausdrücklich gegeneinander abgesichert.


## 19.08.2026 — Zu mehreren unterwegs, und drei Wörter beim Chlor

Quelle: SAS-Handbuch, Kapitel „On the Move", Abschnitt „Moving in groups",
Buchseiten 379–380, am 19.08.2026 im Volltext gelesen. Geschützt
(Wiseman/HarperCollins) — Tatsachen in eigenen Worten.

Vor dem Schreiben **mit der Suche** gemessen: `gruppe zusammenhalten`,
`vorausgehen`, `kundschafter`, `marschordnung` fanden **nichts**;
`wer geht vorne` führte auf Hühner und aufs Schafschlachten. Es gab drei
benachbarte Einträge — „Eine Gruppe einteilen" (Wache, Aufgaben, Buch), „Wenn
einer weggeht" (fünf Fragen) und „Marschtempo" — aber keinen zum Marschieren
selbst.

### Was den Eintrag trägt

**Getrennt wird meistens der Vorderste, nicht der Nachzügler.** Das ist der
Satz, um den herum der Eintrag gebaut ist, weil er der Erwartung widerspricht:
An den Nachzügler denken alle. Der Vorderste klettert über ein Hindernis, der
Zweite sieht ihn sich abmühen und entdeckt daneben einen leichteren Weg, der
Rest folgt dem Zweiten — und der Vorderste ist allein.

**Daraus folgt der wichtigste Punkt: vorher benannte Sammelpunkte.** Sie
beantworten die Frage, die sonst niemand beantworten kann — warten, suchen oder
weitergehen.

**Und die Aufgabenteilung, deren zweite Hälfte man übersieht:** Wer vorn geht,
sucht den Weg und ist mit dem nächsten Hindernis beschäftigt — dabei geht die
Gesamtrichtung verloren. Die gehört deshalb auf einen zweiten Kopf. Dazu: den
Vordersten oft ablösen (es ermüdet den Kopf, nicht die Beine), jeder ist
namentlich für mindestens einen anderen zuständig, und nachgezählt wird nach
jeder Flussquerung und jedem schwierigen Stück.

Der Schlussabsatz zum Unterschied zwischen Ausflug und Ernstfall ist als
**Einordnung dieses Pakets** gekennzeichnet.

### Nachtrag: drei Wörter, die gefährlich fehlten

`chlor dosierung` führte auf **Kaliumpermanganat**. Wie viel Chlor ins Wasser
gehört, ist die gefährlichste Frage des Wasserkapitels, und sie fand ihren
Eintrag nicht, weil dort nur `chlor` ohne `dosierung` stand. Dazu
`splitter im körper` (führte auf Vitaminmangel) und `eingeschneit` (fand
nichts). Acht Schlagwörter nachgetragen; Dosieren und Messen sind ausdrücklich
gegeneinander abgesichert, denn wer sie verwechselt, hält ungeprüftes Wasser
für entkeimt.

Stand: 414 Tipps.


## 19.08.2026 — Einem Wasserlauf folgen

Quelle: SAS-Handbuch, Kapitel „On the Move", Abschnitt „Planning", Unterabschnitt
„Following rivers", Buchseite 378, am 19.08.2026 im Volltext gelesen. Geschützt
(Wiseman/HarperCollins) — Tatsachen in eigenen Worten.

Vor dem Schreiben **mit der Suche** gemessen: `fluss folgen` führte auf „Nachts
gehen", `bach folgen` auf **Hühner**, `zivilisation finden` und `mäander` fanden
**nichts**. Der klassische Weg zurück zu Menschen stand nirgends.

### Was der Eintrag trägt

Ein Wasserlauf ist zweierlei: ein **Weg**, der irgendwohin führt — die meisten
Flüsse enden am Meer oder an einem großen Binnensee, und Siedlungen stehen seit
jeher am Wasser —, und eine **Versorgung** unterwegs. Dazu die Wildwechsel am
Ufer, auf denen es sich um ein Vielfaches leichter geht als durch Gestrüpp.

Und der Teil, den man vorher wissen muss: **wo man den Lauf verlässt.** Im
Oberlauf schneidet sich das Wasser in Schluchten ein — steile, felsige,
rutschige Ufer; dann auf die Höhe und die Bögen abschneiden, also der ungefähren
Richtung folgen statt dem Ufer. In der Ebene sind die **Innenseiten** der
Schleifen sumpfig und werden überschwemmt, erkennbar am üppigen, schilfartigen
Bewuchs — die Schleife wird abgekürzt.

Genannt ist auch die seltene Ausnahme: Manche Flüsse verschwinden plötzlich
unter der Erde.

Der Absatz zum Ufer als gefährlichstem Geländestreifen (Abstand zur Kante, nicht
bei Hochwasser, nicht nachts) ist als **Einordnung dieses Pakets** gekennzeichnet.

### Zur Suche

Der Titel trägt bewusst **„Bach" und nicht „Fluss"**: Das Wort Fluss gehört der
Anleitung „Einen Fluss durchqueren", und ein Titelwort wiegt 5 Punkte — es hätte
sie verdrängt. Die Fluss-Wörter stehen deshalb in den Schlagwörtern.
Festgeschrieben ist beides: `fluss folgen` führt hierher, `fluss`,
`fluss überqueren` und `fluss durchqueren` bleiben bei der Anleitung.

Nebenbei berichtigt: Der Eintrag zitierte „Zielmarken: die Richtung halten ohne
Kompass" — so heißt er nicht.

Stand: 415 Tipps.


## 19.08.2026 — Vorher hinsehen: was ein Aussichtspunkt zeigt und was nicht

Quelle: SAS-Handbuch, Kapitel „On the Move", Abschnitt „Planning" samt
„Maintaining direction", Buchseite 378, am 19.08.2026 im Volltext gelesen.
Geschützt (Wiseman/HarperCollins) — Tatsachen in eigenen Worten.

Vor dem Schreiben **mit der Suche** gemessen: `route planen` führte auf den
**Gartenplan**, `aussichtspunkt` und `vorher anschauen` fanden **nichts**, und
`totes gelände` führte auf **„Totes Gewebe erkennen"** — zwei völlig
verschiedene Sachen, die sich nur ein Wort teilen.

### Was der Eintrag trägt

**Die beiden Täuschungen eines Blicks von oben.** Erstens das TOTE GELÄNDE — die
Senke hinter der Kuppe, der Einschnitt hinter dem Waldrand. Genau dort liegt,
was einen aufhält; was man sieht, ist selten das Problem. Zweitens, dass
Entfernung Einzelheiten wegbügelt: Ein Hang, der von weitem machbar aussieht,
kann von Nahem eine Wand sein.

Daraus die Regel, die den Unterschied macht: **einen Weg UND eine
Ausweichmöglichkeit planen** — und den Punkt mitplanen, an dem umgekehrt wird.

**Und die zwei Regeln fürs Baumsteigen**, die in der Quelle ausdrücklich
mitstehen: nah am Stamm bleiben, jeden Ast prüfen, bevor man ihm sein Gewicht
gibt. Ein gebrochenes Bein an dieser Stelle bedeutet, dass niemand mehr geht.

Dazu: oben bleiben, bis der richtige Rücken für den Abstieg gefunden ist, und
Felsrippen wie dichtes Gestrüpp umgehen statt durchqueren.

Der Absatz darüber, wann sich der Aufstieg auf einen Aussichtspunkt lohnt und
wann nicht, ist als **Einordnung dieses Pakets** gekennzeichnet.

Nebenbei berichtigt: ein falsch zitierter Titel („Ein Hindernis umgehen und
wieder auf Kurs kommen" — der Eintrag heißt „Um ein Hindernis herum, ohne die
Richtung zu verlieren").

Stand: 416 Tipps. Gegenprobe festgeschrieben, besonders die, dass `totes gewebe`
weiter zur Wunde führt und nicht ins Gelände.


## 19.08.2026 — Die Warnzeichen selbst waren nicht auffindbar

Keine neue Quelle. Gemessen wurde diesmal etwas anderes als bisher: nicht die
Namen der Krankheiten, sondern **die Wörter, mit denen ein Laie beschreibt, was
er vor sich sieht.** Das war der schlechteste Befund des ganzen Durchgangs.

| Eingetippt | Was kam | Was hätte kommen müssen |
|---|---|---|
| `druck auf der brust` | Gedränge, Hirnhautentzündung | Verdacht auf Herzinfarkt |
| `schmerz in den arm` | Kompartmentsyndrom, **Nagelbett** | Verdacht auf Herzinfarkt |
| `verwaschene sprache` | **nichts** | Schlaganfall erkennen |
| `erbricht blut` | Schock, **Typhus** | Schwarzer, teeriger Stuhl |
| `zuckt am ganzen körper` | **Nagelbett** | Krampfanfall |
| `starke bauchschmerzen` | Fieber, Nebennierenschwäche | Bauchfellentzündung |
| `schlimmster kopfschmerz` | **nichts** | Kopfschmerz: welcher harmlos ist |

**In jedem einzelnen Fall gab es den richtigen Eintrag.** Er kannte nur das Wort
nicht.

Das ist ein anderes Muster als die Runden davor. Dort fehlten Synonyme
(„stoppen" statt „stillen"). Hier fehlte etwas Grundsätzlicheres: Ein
Notfall-Eintrag ist nach seiner **Diagnose** benannt und mit den Fachwörtern
verschlagwortet — aber wer ihn braucht, hat keine Diagnose. Er hat eine
**Beobachtung**: Druck auf der Brust, ein Arm, der schmerzt, eine Sprache, die
verwaschen klingt.

Dreizehn Schlagwörter nachgetragen und eines gestrichen: „festhalten" beim
Krampfanfall, weil es im Titel steht und die Grenze von zwanzig Schlagwörtern
erreicht war.

Gegenproben festgeschrieben: `gedränge` bleibt beim Gedränge,
`kompartmentsyndrom` bei sich selbst, `steifer nacken` bei der
Hirnhautentzündung.

**Was daraus für neue Einträge folgt:** Zu jedem Notfall-Eintrag gehört
mindestens ein Schlagwort in der Sprache dessen, der ihn braucht — nicht in der
des Handbuchs.


## 19.08.2026 — Warnzeichen, zweiter und dritter Durchgang

Weiter mit den Wörtern, mit denen ein Laie beschreibt, was er sieht.

| Eingetippt | Was kam | Was hätte kommen müssen |
|---|---|---|
| `roter streifen am arm` | **Beeren** | Sepsis |
| `haut ist gelb` | Schlachttier, **Läuse** | Gelbsucht erkennen |
| `urin ist dunkel` | **Kompost** | Gelbsucht erkennen |
| `dickes bein` | **Gerben ohne Rinde** | Thrombose |
| `zittert stark` | Eis-Selbstrettung | Unterkühlung Stadium I |
| `kein puls` | Kompartmentsyndrom | Bewusstlos: atmet oder atmet nicht |
| `fühlt sich sterbenskrank` | **nichts** | Sepsis |
| `wunde eitert` | **Tiergesundheit** | Wundrose |
| `lippen schwellen an` | Wiederernährung | Allergischer Schock |
| `kann kaum atmen` | **Überzuckerung** | Ersticken erkennen |

Der erste ist der unangenehmste: **Der rote Streifen, der von einer Wunde zum
Rumpf zieht**, ist eines der wenigen Zeichen, bei denen ein Laie sofort weiß,
dass es ernst wird. Das Wort stand als `roter streifen` im Sepsis-Eintrag — aber
mit `am arm` dahinter fand die Suche keinen Eintrag mehr, der alle drei Wörter
enthielt, und warf Beeren aus.

Zweiundzwanzig Schlagwörter nachgetragen, dreiundzwanzig Anfragen
festgeschrieben, jeweils mit Gegenprobe.

### Zwei Stellen, an denen ich es beim Notieren belassen habe

**`kein puls`** führt jetzt richtig — aber über einen Gleichstand, den die
alphabetische Reihenfolge der Titel entscheidet. Das ist absichtlich
festgeschrieben, damit es auffällt, wenn ein neuer Eintrag die Anfrage
übernimmt.

**`kann kaum atmen`** hat keinen richtigen Adressaten: Es gibt keinen Eintrag
„Atemnot einordnen". Die vorhandenen sind nach Ursache benannt — Spannung im
Brustkorb, Asthma, Lungenentzündung, allergischer Schock. Die Anfrage geht
deshalb auf „Ersticken erkennen", weil das die eine Ursache ist, die ein
Umstehender in Sekunden beheben kann. **Das ist ein Notbehelf, und er steht auch
so im Test.** Ein eigener Einordnungs-Eintrag wäre besser.


## 19.08.2026 — Wie ein Elternteil sucht, und wer jemanden pflegt

Vierter bis sechster Durchgang durch die Beschreibungen. Diesmal aus der Sicht
der wahrscheinlichsten Benutzerin dieses Handbuchs überhaupt: jemand mit einem
kranken Kind.

| Eingetippt | Was kam | Was hätte kommen müssen |
|---|---|---|
| `kind wacht nicht auf` | **Gürtelrose** | Bewusstsein prüfen |
| `kind ist ganz schlaff` | **Bienen** | Fieber: ab wann es gefährlich wird |
| `kind ist heiß` | Fremdkörper im **Ohr** | Fieber: ab wann es gefährlich wird |
| `kind hat sich verbrannt` | **Bienen** | Verbrennung: zehn Minuten kühlen |
| `kind gefallen` | **Gedränge** | Gehirnerschütterung erkennen |
| `kind hat kopf angeschlagen` | **Sonnenstich** | Gehirnerschütterung erkennen |
| `wund gelegen` | **Tiergesundheit** | Wer lange liegt: alle zwei Stunden drehen |
| `druckstellen` | **Axtstiel** | Wer lange liegt |
| `husten geht nicht weg` | **Rippenbruch** | Tuberkulose: Husten, der nicht aufhört |
| `stuhl ist weiß` | **Madenwurm** | Gelbsucht erkennen |
| `alter mensch gestürzt` | **Gedränge** | Beckenbruch |
| `hat sich übergeben` | Hitzschlag | Erbrechen: helfen |
| `wasser wird knapp` | Brunnen, Epilepsie-Mittel | Trinkwasser: Vorrat für zehn Tage |

Der Tuberkulose-Fall ist der lehrreichste: Der Eintrag heißt **wörtlich**
„Tuberkulose: Husten, der nicht aufhört". Ein anderes Wort für genau dieselbe
Sache — „geht nicht weg" statt „hört nicht auf" — und er war weg.

Und die **Mehrzahl-Falle** hat in dieser Nacht zum dritten Mal zugeschlagen:
`reanimieren` gegen „Reanimation", `ohrenschmerzen` gegen „Ohrenschmerz",
`druckstellen` gegen „Druckstelle". Sie steht jetzt im Merkzettel.

Vierundfünfzig Schlagwörter nachgetragen, neunundzwanzig Anfragen
festgeschrieben, jeweils mit Gegenprobe.

### Zwei Anfragen bewusst offen gelassen

`rationieren` — was wird rationiert? Wasser, Essen, Medikamente? Drei Einträge
liegen gleichauf, und keiner ist der richtige für alle drei.

`kann nicht mehr laufen` — nach einem Sturz etwas anderes als bei Schwäche oder
nach einem Schlaganfall. Ein einzelner Adressat wäre geraten.


## 19.08.2026 — Brustentzündung beim Stillen (gemessene Lücke geschlossen)

Quelle: **Weltgesundheitsorganisation, „Mastitis: Causes and Management",
WHO/FCH/CAH/00.13, Genf 2000.** Am 19.08.2026 über iris.who.int geladen
(Ausgabe `WHO_FCH_CAH_00.13_eng.pdf`, 50 Seiten) und im englischen Volltext
gelesen; benutzt sind Abschnitt 7 (Zeichen), 9 (Behandlung samt
Antibiotika-Tabelle 4) und 10 (Sicherheit des Weiterstillens). Die Schrift
erlaubt ausdrücklich freies Zitieren, Zusammenfassen und Übersetzen, nicht
jedoch den Verkauf oder eine kommerzielle Verwertung — hier ist sie in eigenen
Worten wiedergegeben. Ablage: `work/quellen/mastitis/`.

Die Lücke stand seit derselben Nacht als **gemessen und unbelegt** im Protokoll:
`mastitis` fand **nichts**, `brustentzündung` führte auf „Antibiotika: wann
keines gegeben wird". Erst als die Quelle vorlag, wurde geschrieben.

### Was der Eintrag trägt

**Die Regel, die dem Gefühl widerspricht:** Abstillen hilft nicht bei der
Genesung; die Quelle sagt, es bestehe das Risiko, dass es den Zustand
verschlimmert. Die Ursache ist gestaute Milch, und Milch, die nicht herauskommt,
staut sich weiter. **Die Milch aus der betroffenen Brust schadet dem Kind
nicht** — geprüft in sechs Untersuchungen an über zweihundert Kindern, auch bei
nachgewiesenen Staphylokokken, ohne einen einzigen Schaden. Ausnahme laut
Quelle: bei HIV-positiver Mutter nicht an der betroffenen Brust.

**Die Unterscheidung der vier Zustände** — pralle Brust, Milchstau, verstopfter
Milchgang, Entzündung — mit den Merkmalen, an denen sie auseinandergehen. Dabei
der Satz, der vor einem Trugschluss schützt: **Bei einem Drittel bis der Hälfte
der Frauen mit Brustentzündung fehlt das Fieber.**

**Was zuerst zu tun ist, und es ist nicht das Medikament:** Die Milch muss
heraus. Das nennt die Quelle den wichtigsten Teil der Behandlung — ohne bessere
Entleerung wird es trotz Antibiotikum schlechter oder kommt wieder.

**Wann ein Antibiotikum nötig ist**, in drei Fällen genau benannt, mit Mitteln,
Mengen und Dauer (10–14 Tage; kürzer bedeutet mehr Rückfälle). Das ist wichtig,
weil Antibiotika in einer Krise knapp sind. Penicillin und Ampicillin nennt die
Quelle ausdrücklich als nicht mehr geeignet.

**Beim Abszess** der Satz, der über die Behandlung entscheidet: Ein Antibiotikum
allein nützt wahrscheinlich nichts, solange der Eiter drin ist — die Kapsel
schirmt die Erreger ab.

Der Absatz für den Fall, dass niemand kommt, ist als **Einordnung dieses
Pakets** gekennzeichnet und verweist fürs Eröffnen auf den vorhandenen Eintrag.

### Zur Suche

Der Titel trägt **absichtlich weder „Brust" noch „Entzündung" noch „Stillen"**
als eigenes Wort: `brust` gehört den Brustschmerzen und der offenen Brustwunde,
`entzündung` der Sepsis, `stillen` teilen sich Blutstillung und „Stillen:
bekommt das Kind genug?". Ein Titelwort wiegt 5 Punkte und hätte jedem davon den
ersten Platz genommen. „Milchstau" benutzt sonst niemand — die Laienwörter
stehen in den Schlagwörtern. Nachgemessen und festgeschrieben.

Stand: 417 Tipps.


## 19.08.2026 — Letzte Durchgänge: Hygiene, Tote, und Fragen ohne Diagnose

| Eingetippt | Was kam | Was hätte kommen müssen |
|---|---|---|
| `leiche im haus` | Brandrauch, Vorratshaltung | Tote bergen: was auf einen zukommt |
| `haare voller läuse` | **Ziegenmilch** | Läuse: Kleidung ist das Nest |
| `müll wohin` | bewusster Versatz beim Gehen | Abfall im Lager |
| `husten geht nicht weg` | **Rippenbruch** | Tuberkulose: Husten, der nicht aufhört |
| `wem kann ich glauben` | Alkoholentzug, Wasserfilter | Falschmeldungen erkennen |
| `keine kraft mehr` | Gedränge, versagende Bremsen | Was der Kopf in einer langen Krise tut |
| `zu wenig milch` | ausgeschlagener **Zahn** | Stillen: bekommt das Kind genug? |

Der erste ist der, auf den es ankommt: In einer längeren Krise stirbt jemand im
Haus, und die Frage, was dann zu tun ist, stellt sich unter Schock. Der Eintrag
stand da und war unter diesen Worten nicht zu erreichen.

### Drei Anfragen, die offen bleiben — und warum

**`halte das nicht aus`** besteht fast nur aus Allerweltswörtern („halte", „das",
„nicht", „aus"), die in hunderten Einträgen stehen. Ohne eine Gewichtung nach
Seltenheit ist das nicht zu lösen. Nicht festgeschrieben, im Merkzettel notiert.

**`rationieren`** — was wird rationiert? Wasser, Essen, Medikamente? Drei
Einträge liegen gleichauf, und keiner ist für alle drei der richtige.

**`kann nicht mehr laufen`** ist nach einem Sturz etwas anderes als bei Schwäche
oder nach einem Schlaganfall. Ein einzelner Adressat wäre geraten.

### Und eine Eigenheit des Deutschen, die bleibt

`stillen` heißt beides — ein Kind stillen und eine Blutung stillen. Beide
Einträge tragen das Wort im Titel und liegen gleichauf. Das ist **absichtlich
nicht** entschieden worden: Wer das Wort allein tippt, bekommt beide zu sehen,
und das ist die ehrlichste Antwort.


## 19.08.2026 — Gefunden werden

| Eingetippt | Was kam | Was hätte kommen müssen |
|---|---|---|
| `auf sich aufmerksam machen` | **Kartoffeln** | Notsignale: alles in Dreiergruppen |
| `wie finden die mich` | Hülsenfrüchte | Notsignale |
| `rauchzeichen` | **nichts** | Notsignale (nennt Signalrauch ausdrücklich) |
| `hilfe rufen` | Sichtung und Ordnung | Notsignale |
| `suchtrupp` | Gruppe einteilen | Notsignale |

Fünf Schlagwörter nachgetragen, acht Anfragen festgeschrieben. Spiegel,
Bodenzeichen und Notruf behalten ihre eigenen Fragen.

### Gemessene Lücke, die offen bleibt: im Fahrzeug festsitzen

`im auto festsitzen`, `beim auto bleiben`, `auspuff frei`, `liegengeblieben`
führen ins Leere oder auf das versinkende Auto. Vorhanden sind Einträge zum
**Auto im Wasser**, zum **Auto im Sturm** und zum Auto als **Schutz vor Gasen** —
aber keiner zu dem Fall, der in Mitteleuropa am häufigsten vorkommt: Winter,
Straße dicht, Stunden im Wagen.

Dazu gehören mindestens zwei Dinge, die man wissen muss: der
Kohlenmonoxid-Punkt (bei laufendem Motor muss der Auspuff frei sein) und die
Frage, ob man beim Fahrzeug bleibt oder losgeht.

**Dafür liegt keine Quelle im Haus.** Die BBK-Schrift „Vorsorgen für Krisen und
Katastrophen" (2025) nennt das Auto nur als Schutz vor Gasen und Dämpfen; das
SAS-Handbuch behandelt Bremsversagen und Zusammenstoß, nicht das Festsitzen.
Notiert, nicht geraten — wie bei der Brustentzündung, bis eine Quelle vorlag.


## 19.08.2026 — Anleitungen sind nur über ihre Kurzfassung erreichbar

| Eingetippt | Was kam | Was hätte kommen müssen |
|---|---|---|
| `seil knüpfen` | **nichts** | Grundknoten und Lashings |
| `schnur selber machen` | **nichts** | Schnur, Bindematerial und ein Rucksack |
| `axt stiel gebrochen` | Schärfen | Einen Axtstiel erneuern |
| `dach abdichten` | **Ziegel brennen** | Dach decken |
| `ist das essbar` | Hülsenfrüchte | Acht Zeichen, bei denen du gar nicht erst probierst |

### Der lehrreichste Fall: das Wort „selber"

`schnur selber machen` fand nichts — und zwar, weil **„selber" im ganzen
Handbuch nirgends vorkam.** Es benutzt durchgehend „selbst".

Die Suche lässt eine Anfrage leer, sobald ein Wort darin nirgends steht. Das ist
die Regel gegen Tippfehler (`wasser hologramm` soll nicht die Wasser-Einträge
auswerfen), und hier hat sie ein völlig normales deutsches Wort getroffen. Ohne
Wörterbuch lässt sich „selber" nicht von „hologramm" unterscheiden; behoben
wurde es deshalb am Text, nicht an der Regel.

**Merksatz fürs Schreiben:** Wo es zwei gebräuchliche Wörter für dieselbe Sache
gibt, sollte mindestens eines im Text stehen — bei Anleitungen in der
Kurzfassung, denn die ist ihr einziges Suchfeld.

### Und eine Kollision, die absichtlich bleibt

`knoten` führt weiter auf den **medizinischen** Knoten (Abszess, Brust). Um das
zu drehen, müsste der Anleitungstitel „Grundknoten" zu „Knoten" werden — ein
Titelwort wiegt 5 Punkte und hätte den Abszess verdrängt. Beide Bedeutungen sind
echt, und die Anleitung ist über `seil knüpfen` erreichbar.


## 19.08.2026 — Schlussprobe und die letzten Runden

**Die fünfundzwanzig vom Anfang:** 7 von 25 Anfragen fanden zu Beginn der Nacht
gar nichts, am Ende keine mehr. Festgeschrieben als
`diePruefungVomAnfangDerNacht`.

Zuletzt behoben:

| Eingetippt | Was kam | Was hätte kommen müssen |
|---|---|---|
| `jemand ist bewusstlos` | **Kopfschmerz** | Bewusstlos: atmet oder atmet nicht |
| `mir ist sehr kalt` | Schock, **Behelfsklinge** | Unterkühlung Stadium I |
| `finger weiß und taub` | **Bienen** | Erfrierungen erkennen |
| `kreislauf bei hitze` | **Madenwurm** | Hitzschlag erkennen |
| `wehen wie oft` | **Vlies beurteilen** | Notgeburt: erkennen, ob Zeit bleibt |
| `zahn tut höllisch weh` | **nichts** | Zahnschmerz: was dahintersteckt |
| `jod wann nehmen` | Halsschmerzen | Jodtabletten: nur auf Ansage |
| `garten anlegen` | Brunnen | Den Garten planen |

### Das Wort, das nirgends steht — zum zweiten Mal

Nach `selber` jetzt `höllisch`. Beide sind völlig normale deutsche Wörter, die
im Handbuch nur zufällig nicht vorkamen — und eine Anfrage bleibt leer, sobald
ein Wort darin nirgends steht.

**Für künftige Einträge:** an die Wörter denken, mit denen jemand seinen Zustand
benennt, nicht nur an die, mit denen das Handbuch ihn beschreibt.


## 19.08.2026 — Die Wörter, mit denen ältere Menschen suchen

| Eingetippt | Was kam | Was hätte kommen müssen |
|---|---|---|
| `kohlenmonoxyd` | **nichts** | Kohlenmonoxid: unsichtbar und geruchlos |
| `gehirnschlag` | **nichts** | Schlaganfall erkennen |
| `grippaler infekt` | **Malaria** | Bronchitis |
| `magenverstimmung` | **nichts** | Erbrechen: helfen |

Der erste ist der gefährlichste: **Kohlenmonoxyd mit y** ist die alte
Schreibweise, und genau die Generation, die so schreibt, heizt im Stromausfall
mit Kohle, Gas und Notkocher. Das Gas tötet lautlos.

Beim Kohlenmonoxid-Eintrag war die Grenze von zwanzig Schlagwörtern schon
erreicht. Die alte Schreibweise steht deshalb **im Text** — an der ehrlicheren
Stelle ohnehin, denn dort gehört der Hinweis hin, dass dieselbe Sache früher
anders geschrieben wurde.

### Was schon vorher stimmte

`schlagfluss`, `herzkasper`, `zuckerkrankheit` und `schwindsucht` fanden ihre
Einträge bereits — jemand hat diese Wortsorte beim Schreiben mitgedacht. Das ist
jetzt festgeschrieben, damit es so bleibt.

### Und was weiterhin nichts findet

`gliederreißen` — dafür gibt es keinen Eintrag zur Grippe als solcher, nur zu
Bronchitis, Halsschmerzen und Fieber. Kein Schlagwort nachgetragen, weil es auf
einen Eintrag zeigen würde, der die Frage nicht beantwortet.


## 19.08.2026 — Das Paket heißt europe-de, nicht deutschland-de

Österreichische und schweizerische Wörter gemessen. Zwei Befunde sind ernst:

| Eingetippt | Was kam | Bemerkung |
|---|---|---|
| `notarzt` | **nichts** | Das Wort kam im ganzen Handbuch **null Mal** vor |
| `spital` | **nichts** | Handbuch sagt „Klinik" (34 Einträge), „Krankenhaus" (11) |
| `gelse` | **nichts** | österreichisch für Mücke |
| `erdäpfel` | **nichts** | österreichisch für Kartoffeln |
| `paradeiser`, `jause`, `sackerl`, `stiege`, `velo` | nichts | (kein Eintrag betroffen) |

**„Notarzt" ist der schwerste davon.** Es ist nicht einmal ein regionales Wort im
engen Sinn — es ist der gebräuchliche deutsche Begriff für den Arzt, der zum
Notfall kommt, und er stand nirgends. Wer ihn tippt, bekam eine leere Seite,
obwohl der Eintrag „Notruf 112" genau danebenlag.

Neun Schlagwörter nachgetragen (`notarzt`, `spital`, `rettung rufen`,
`krankenwagen`, `ins krankenhaus`, `gelse`, `gelsen`), dazu die **Erdäpfel** im
Kartoffelkapitel — an der richtigen Stelle im Text, weil Kapitel kein
Schlagwortfeld haben.

**Für künftige Einträge:** Das Paket richtet sich an Mitteleuropa. Wo Österreich
und die Schweiz ein eigenes Wort haben und es um einen Notfall geht, gehört es
dazu. Bei Gemüsenamen ist es Beiwerk; beim Spital und beim Notarzt nicht.


## 19.08.2026 — Die Fragen des Helfers

Wer daneben steht, fragt anders als der Betroffene — und seine Fragen führten
woandershin:

| Eingetippt | Was kam | Was hätte kommen müssen |
|---|---|---|
| `darf ich ihn bewegen` | **Trocknen** | Verletzten bewegen: wann gar nicht |
| `wen zuerst versorgen` | **Beute versorgen** | Mehrere Verletzte: erst alle ansprechen |
| `was sage ich am telefon` | Gartenplan | Notruf 112 |

Der erste ist der schwerste: Die Antwort auf „darf ich ihn bewegen" entscheidet
darüber, ob jemand gelähmt bleibt. Der zweite ist bitter komisch — „wen zuerst
versorgen" verlor gegen das Zerlegen eines erlegten Tieres.

### Am Gerät nachgesehen

App und Paket neu gebaut, auf dem Emulator installiert, und die Funde dort
eingetippt:

    "notarzt"              -> Notruf 112                    (vorher: nichts)
    "spital"               -> Notruf 112                    (vorher: nichts)
    "kohlenmonoxyd"        -> Kohlenmonoxid: unsichtbar     (vorher: nichts)
    "darf ich ihn bewegen" -> Verletzten bewegen            (vorher: Trocknen)
    "jemand ist bewusstlos"-> Bewusstlos: atmet oder nicht  (vorher: Kopfschmerz)
    "mein kind atmet nicht"-> Kind atmet nicht              (vorher: nichts)


## 19.08.2026 — Elf Einträge parallel, jeder aus genau einer benannten Quelle

Gefordert waren Tempo und Breite, vor allem bei Agrikultur, Bauanleitungen und
Taktik. Gearbeitet wurde an zwölf Strängen gleichzeitig — aber nach denselben
Regeln wie sonst auch: **eine benannte Quelle je Eintrag, im Volltext gelesen, nichts
aus dem Gedächtnis.** Vorher wurde mit der Suche gemessen, dass es die Themen
noch nicht gibt.

### Taktik

**Unbemerkt bleiben: Bewegung, Umriss, Glanz, Geräusch, Geruch**
US Army, „FM 3-05.70 Survival" (2002), Kapitel 20 „Movement" und 21
„Camouflage", am 19.08.2026 im Volltext gelesen. Gemeinfrei. Die Reihenfolge
nach Wichtigkeit stammt aus der Quelle; die Übertragung auf ein Haus im
Stromausfall (Licht am Fenster, Generator, Rauch am Tag, Hund, Wäscheleine) ist
als Einordnung gekennzeichnet.

**Scharfschütze: Versteck ist nicht dasselbe wie Schutz**
IKRK, David Lloyd Roberts, „Staying Alive" (1999, Fassung 2005), Abschnitt „The
Threat from Sniper and Rifle Fire", Seiten 56–60. Bewusst weggelassen: alles,
was Angriffswissen wäre — wie ein Schütze eine Stellung bezieht, sich tarnt oder
anpirscht. Der Eintrag ist reines Schutzwissen.

**Schüsse hören: der Knall verrät die Entfernung**
Dieselbe Quelle, Seiten 58–60. Der Unterschied zwischen Peitschenknall (sehr
nah) und Zischen oder Pfeifen (weiter weg) steht dort wörtlich.

**Entführung: nicht fliehen, ruhig bleiben, Anweisungen befolgen**
IKRK „Staying Alive", Seiten 149–153, und IFRC „Stay Safe" (3. Auflage 2009),
Seiten 185–193. **Die beiden Quellen widersprechen sich bei der Fluchtfrage** —
das IKRK verbietet Fluchtversuche, die IFRC lässt eine enge Ausnahme zu. Beides
steht wortgetreu im Eintrag, damit keine falsche Sicherheit entsteht.

**Festgehalten oder durchsucht: ruhig bleiben, nichts Plötzliches tun**
Dieselben beiden Quellen. Ausdrücklich vermerkt: Beide schreiben für Helfer mit
einer Organisation im Rücken, nicht für Einzelpersonen. Zum Punkt „keine Papiere
vernichten" fand sich nichts — deshalb weggelassen statt erfunden.

### Bauanleitungen

**Bogen und Pfeile bauen**
Primitive Pursuits, „Bow Making Part I" (2016), und US Army FM 3-05.70, Kapitel
8 und 12. **Die Quellen widersprechen sich beim Holz** — das Heer nimmt totes
trockenes Holz sofort, Primitive Pursuits legt frisches Holz wochenlang ab. Der
Widerspruch steht offen im Eintrag. Der Warnabsatz zum brechenden Bogen ist
vollständig als Einordnung gekennzeichnet, weil keine Quelle ihn hergibt.

**Lehmofen bauen: feste Kochstelle mit wenig Brennstoff**
VITA, „Village Technology Handbook" (3. Auflage 1988), Abschnitt „Cookers and
Stoves". Der Eintrag markiert eine wahrscheinliche Texterkennungsverzerrung der
Quelle („120cm x 129cm" statt vermutlich 120 mal 120) ausdrücklich als solche.

**Einen Brunnen von Hand graben: Schacht sichern und heben**
Peace Corps, „Wells Construction: Hand Dug and Hand Drilled" (1982). **Wichtiger
Befund: Diese Quelle kennt keine Gasprüfung.** Das ist im Eintrag als Lücke
ausgewiesen, mit Verweis auf die vorhandenen Einträge — statt eine Prüfung zu
erfinden.

### Agrikultur

**Maisgrieß und Maismehl: die Mahlgrade und was man daraus isst**
USDA Farmers' Bulletin 565 (1919), gemeinfrei. **Pellagra kommt im Eintrag
nicht vor** — gezielt im Volltext gesucht, kein Treffer, also nicht geschrieben.

**Milben und Läuse am Huhn erkennen und loswerden**
USDA Farmers' Bulletin 801 (1939), gemeinfrei. Die Quelle nennt zehn Mittel, die
heute verboten oder giftig sind (Nikotinsulfat, Quecksilbersalbe, Arsen-Tauchbad,
Kreosotöl, Karbolineum, Anthracenöl, Natriumfluorid, Natriumfluorsilikat,
Naphthalin, Karbolsäure). **Keines davon steht als Anwendung im Eintrag** — sie
werden nur genannt, damit man sie erkennt und nicht benutzt. Übrig bleibt der
mechanische Teil, der wirkt.

**Saubere Milch: Kuh melken, kühlen, Gefäße reinhalten**
USDA Farmers' Bulletin 602 (1914), gemeinfrei. Ausdrücklich vermerkt: Das
Verwerfen der ersten Striche kommt in dieser Quelle NICHT vor.

**Dreschen und Worfeln: von der Garbe zum sauberen Korn**
USDA Farmers' Bulletins 756 (1917) und 2145 (1959), gemeinfrei. Das Worfeln
selbst ist als eigene Ergänzung gekennzeichnet, weil beide Roggenhefte dazu
schweigen.


## 19.08.2026 — Gemessene Lücke: Tierkadaver beseitigen

`tote tiere entsorgen` führt auf die Beurteilung einer Wasserstelle. Es gibt
dazu nichts Eigenes, und **es bleibt vorerst dabei** — zweimal gesucht, zweimal
Fehlanzeige.

Geprüft wurden:

* `peacecorps-water-sanitation-manual.txt` — trotz des Dateinamens eine
  Fallstudiensammlung (Case Study CS-4, 1984) ohne eine einzige Maßangabe.
  Derselbe Befund steht schon in `work/quellen/wasser-auswertung.md`.
* `us-army-fm3-05.70-survival-2002.txt` — Treffer nur zum Häuten von Wild, zur
  Seebestattung und zum Katzenloch. Nichts zum Kadaver im Zusammenhang mit
  Wasserschutz.
* `work/quellen/grabtiefe/` — das PAHO/WHO-Feldhandbuch „Management of dead
  bodies after disasters". Es nennt 1,5 bis 3 Meter Tiefe und 200 bis 350 Meter
  Brunnenabstand, aber **ausschließlich für menschliche Tote**; das Wort
  „animal" kommt darin null Mal vor. Diese Zahlen stehen bereits im Eintrag
  „Tote bergen: wie man die Arbeit einteilt". Sie auf Tiere zu übertragen wäre
  eine Vermutung, keine Quelle.
* `work/quellen/latrine/` — beide USDA-Hefte behandeln menschliche Fäkalien.
* 32 Volltexte in `work/quellen/agrikultur/` — mehrere nennen die
  Kadaverbeseitigung bei Milzbrand, Schweinepest, Räude und Tularämie, aber
  durchweg nur qualitativ: „burned at once or buried deeply". **Keine einzige
  Zahl in der gesamten Sammlung.**

Was fehlt, ist eine tierseuchenrechtliche oder veterinärhygienische Quelle. Bis
die vorliegt, bleibt der Eintrag ungeschrieben — eine erfundene Grabtiefe neben
einem Brunnen wäre genau die Sorte Fehler, die dieses Handbuch nicht machen darf.


## 19.08.2026 — Ein Entwurf, den ich verworfen habe

Vorgelegt wurde ein Entwurf „Wäsche waschen ohne Maschine" mit der
Begründung, das Kapitel „Seife und Wäsche ohne Nachschub" sei zwar ausführlich, es fehle aber
die kurze Nachschlage-Fassung — mit Verweis auf das vorhandene Paar
`hygiene-seife-selbst` (Tipp) und `agrikultur-seife` (Kapitel).

**Nachgeprüft: Das Kapitel enthält bereits alles davon** — die fünf Minuten
Auskochen, die 43 Grad für Wolle und Seide, die 74 Grad über zwanzig Minuten bei
der Wäsche eines Kranken, die Sporenbildner, das Einweichen. Dieselbe Quelle
(USDA Farmers' Bulletin 1099, 1920), dieselben Zahlen.

Und die Suche findet es: `wäsche waschen` führt bereits an erster Stelle auf das
Kapitel. Es gibt also weder eine Inhalts- noch eine Findbarkeitslücke.

**Nicht eingebaut.** Ein zweiter Eintrag mit demselben Inhalt macht das Handbuch
nicht größer, sondern unübersichtlicher — und bei zwei Fassungen derselben Sache
weiß niemand mehr, welche gepflegt wird.

## 19.08.2026 — Zweite Runde: drei Einträge, zwei Bilder, zwei Berichtigungen

### Neu aufgenommen

**Die Karte einnorden** (`orientierung-karte-einnorden`). US Army FM 3-25.26
„Map Reading and Land Navigation" (20.07.2001), Abschnitte 6-2, 6-6 und
Kapitel 11; dazu TC 3-25.26 (15.11.2013), Abschnitte 6-2 und 6-10/6-11.
Gemessen vor dem Schreiben: `einnorden`, `missweisung` und `kompassnadel`
kamen im ganzen Paket nicht vor. Die Missweisungs-Beispiele der Quelle sind
amerikanisch (Fort Benning, Fort Richardson) und deshalb ausdrücklich als
**keine Zahl für Mitteleuropa** gekennzeichnet.

**Ziegen halten** (`agrikultur-ziegen-halten`). USDA Farmers' Bulletin 920
„Milk Goats" (1918, überarbeitet 1927), im Volltext gelesen. Zwei giftige
Alt-Mittel stehen nur als historischer Beleg da, ohne Rezeptur: die
Kupfersulfat-Tränke gegen Magenwürmer und die Klauenfäule-Paste aus Mennige
und Salpetersäure.

**Kartoffelkrankheiten** (`agrikultur-kartoffel-krankheiten`). Farmers'
Bulletin 15 (1894) trägt den Eintrag; Bulletin 1332 (1936) steuert nur noch
den Sack- und Fasshinweis bei, Bulletin 35 (1896) nichts Zusätzliches — der
Rest stand bereits in `agrikultur-saatgut` und `agrikultur-schaedlinge`. Der
Eintrag ist deshalb bewusst schmal.

### Zwei Berichtigungen an eigenen, älteren Einträgen

**Der Melkgriff.** Im Kapitel „Ziegenmilch" stand, keine der beiden Quellen
beschreibe das Melken so, dass man es ohne Vorbild nachmachen könne. Das ist
falsch. Bulletin 920 — eine der beiden Quellen dieses Kapitels — beschreibt
beide Griffe (Kuhgriff und Ausstreichen zwischen Zeigefinger und Daumen), die
Melkstellung von der Seite oder von hinten, zweimal täglich mit gleichen
Abständen und die Wartezeit bis zum vierten oder fünften Tag nach dem
Ablammen. Die Behauptung ist durch das tatsächliche Verfahren ersetzt.
**Eine Lücke zu behaupten ist genauso eine Tatsachenaussage wie eine Zahl** —
und sie war hier nicht geprüft, sondern übernommen.

**Die Maße des Schranktrockners.** Der erste Entwurf des Bildes trug als
Fußzeile die Maße aus Bulletin 841 (1917): 60 auf 40 Zentimeter, 90 hoch.
Die Zeichnung stammt aber aus Bulletin 1918 (1942) und ist an ihren
Zollmaßen als **andere, größere Bauart** zu erkennen — 30 auf 62 Zoll, also
76 auf 157 Zentimeter. Ein Leser mit einem Zollstock hätte den Widerspruch
gefunden. Die Fußzeile nennt jetzt die Maße der abgebildeten Bauart, und der
Text führt sie als zweite Bauart eigens ein.

### Bilder

`mist-franzoesisch-jauche.png` — Figur 2 aus Farmers' Bulletin 192
„Barnyard Manure" (Fassung 1906), ohne Stecherzeichen. Zwei Mistplätze im
Wechsel, Grube und Pumpe. Die Fußzeile nennt nur, was in dieser Quelle steht
(voll bei 2,4 bis 3 Meter Höhe) — der erste Entwurf trug die Zieltemperatur
aus dem Kompostkapitel, die aus einer anderen Quelle stammt und für dieses
Verfahren sogar in die falsche Richtung zeigt: Wässern SENKT hier die
Temperatur.

`doerre-schranktrockner-gross.png` — Figur 2 „Cabinet drier" aus Farmers'
Bulletin 1918 (1942). Unter der Figur steht nur eine Einrichtung als
Herkunft (College of Agriculture, Berkeley), kein Zeichnername.

### Fehlanzeigen dieser Runde

* **Saattiefe.** Kein einziges der 54 Hefte enthält eine Abbildung, die zeigt,
  wie tief ein Samenkorn bedeckt wird. Die Regel stammt ohnehin aus dem
  Peace-Corps-Werk. Ein ersatzweise genommenes Kartoffelbild hätte ein
  anderes Prinzip gezeigt als das, was das Kapitel lehrt.
* **Wasserstelle beurteilen.** Weder im SOF-Handbuch (der eigentlichen Quelle
  des Tipps) noch bei Peace Corps oder FM 3-05.70 gibt es dazu eine
  Abbildung. Das Thema wird in diesen Werken in Prosa behandelt, nicht
  gezeichnet.
* **Feuer im Freien.** Der Entwurf für einen eigenen Eintrag wurde NICHT
  aufgenommen: Fünf seiner sieben Punkte standen bereits in „Lagerfeuer
  sicher anlegen". Die zwei neuen (keine flüssigen Brandbeschleuniger,
  Holzstapel vor dem Anzünden umschichten) und drei weitere aus derselben
  Quelle sind stattdessen dort eingebaut worden, wo sie hingehören.

## 20.08.2026 — Bienen: keine übernehmbare Zeichnung, systematisch geprüft

Für das Kapitel „Bienenvolk halten: Kasten, Schwarm und Wachs" wurde eine
Zeichnung des Beutenaufbaus oder eines Rähmchens gesucht. **Es gibt keine.**
Acht Hefte geprüft, das Ergebnis steht hier, damit es niemand noch einmal
sucht:

| Heft | Befund |
|---|---|
| 447 „Bees" (Phillips) | durchgehend signiert, u. a. „J.H.S.", „C.H.S.", „R. E. Snodgrass"; Figur 2 trägt ein verstecktes „S." auf dem Fluglochblock |
| 503 „Comb Honey" (Demuth) | dieselbe Zeichnerlinie wie 447, gleiches Signaturproblem |
| 961 „Transferring Bees to Modern Hives" | Figuren 1–3 sind Halbton-Fotos; Figur 4 trägt „R.E.S." versteckt auf einem Brett |
| 1198 „Swarm Control" (Demuth) | durchgehend signiert |
| 695 „Outdoor Wintering of Bees" | enthält im ganzen Heft **keine einzige Abbildung** |
| 2074 „American Foulbrood" | nur Fotos erkrankter Zellen; die eine Strichzeichnung daraus ist bereits verwendet |
| 653 „Honey and Its Uses in the Home" | Haushaltsheft, keine Beute und kein Rähmchen |
| „Selecting and Operating Beekeeping Equipment" (1965) | nur Fotos von Großgeräten |

**Offen geblieben, ausdrücklich nicht eigenmächtig entschieden:** Figur 14
aus Heft 447 („Handling the frame", ein Rähmchen in zwei Händen) liess sich
auch bei sechsfacher Vergrösserung an keiner Stelle als signiert nachweisen.
Da aber im selben Heft mehrere Signaturen an unerwarteten Stellen versteckt
sind, wäre eine Übernahme eine Wette. **Das ist ausdrücklich zu entscheiden,
nicht in der Nachtschicht.**

Was bliebe, wenn die Figur gewollt ist: nachzeichnen statt übernehmen.

## Regen vom Dach: Rinne, Fallrohr, Fass (20.08.2026)

Für „Rinne, Fallrohr und Fass: Regen vom Dach auffangen" wurden drei
USDA-Hefte derselben Linie ausgewertet — Bulletin 941 (1918, Warren),
1448 (1933, Warren) und 1978 (1948, Garver). Jedes ersetzt laut eigenem
Vermerk das vorige. **Sie zählen deshalb als EINE Quelle, nicht als drei
unabhängige Bestätigungen** — das steht so auch in den Quellenangaben des
Eintrags, damit niemand die Zahl der Belege für Übereinstimmung hält.

Bulletin 941 lag als einziges noch nicht im Projekt und wurde nachgeladen:
`work/quellen/figuren/CAT87202481/farmbul0941.pdf` (archive.org-Kennung
`CAT87202481`, abgerufen 20.08.2026). Nur aus diesem Vorgänger stammen der
Frostschutz durchs Eingraben, das Regen-Wechsel-Ventil und das Fass als
kleine Zisterne — die Nachfolger haben diese Abschnitte gekürzt.

Die Faustzahl „ein Millimeter Regen auf einem Quadratmeter Dach gibt einen
Liter" ist damit zum zweiten Mal unabhängig belegt: Sie stand schon aus
Jones (1978) im Brunnen-Kapitel, und Garvers amerikanische Formel
(Quadratfuss mal Zoll mal 0,625 gleich Gallonen) ergibt umgerechnet
dasselbe. Neu hinzugekommen ist Garvers Abzug von mindestens einem Drittel
für Undichtigkeit, Dachwäsche und Verdunstung.

Die Schrift der University of Arizona (az1863, 2021) ist **kein** Werk einer
US-Bundesbehörde. Sie wurde deshalb wie schon beim Tipp „Regen vom Dach: der
erste Schwall muss weg" nur als geprüfter Beleg herangezogen, ohne
Wortlautübernahme.

## 21.08.2026 — Kampfstoff, gemessene Lücken und vier neue Themenfelder

Alle Belege dieser Nacht, die vorher noch nirgends in dieser Datei standen.
Die beiden US-Nahkampf-Handbücher (FM 3-25.150 und FM 21-150) stehen bereits
weiter oben und sind hier nicht wiederholt — sie tragen den weitaus größten
Teil der neuen Einträge.

**Lizenzlage:** Nicht jede dieser Quellen ist frei. Wo eine geschützte Quelle
steht, ist ausschließlich die TATSACHE übernommen und der Text selbst
geschrieben — Tatsachen, Zahlen und Arbeitsverfahren sind nicht geschützt.
Die einzige wörtliche Übernahme dieser Nacht ist ein Satz aus der
ProPK-Broschüre „Sicher wohnen“ und der Wortlaut von § 127 Absatz 1 Satz 1
StPO; beide sind als Zitat gekennzeichnet, der Gesetzestext ist zudem
amtliches Werk und damit gemeinfrei.

### Strafprozessordnung (StPO)

- Paragraf 127 Absatz 1 Satz 1 StPO „Vorläufige Festnahme“, Wortlaut wörtlich übernommen von gesetze-im-internet.de, gelesen am 21.08.2026. Amtliches Werk, gemeinfrei nach Paragraf 5 UrhG.

  Verwendet für: `taktisch-person-sichern-begleiten`.

### Deutsche Gesetzliche Unfallversicherung (DGUV)

- DGUV Regel 112-190 „Benutzung von Atemschutzgeräten“, Ausgabe November 2021, Abschnitte 4.5.1.3.12.3 (Bart/Brille/Kontaktlinsen und Dichtsitz), 4.5.1.3.17–4.5.1.3.20 (Partikel- vs. Gasfilter, Tabelle 8 Gasfiltertypen A/B/E/K/AX/SX), 4.5.1.3.21 (Lagerfrist, Gebrauchsdauer, Geruchs-/Geschmacksdurchbruch), publikationen.dguv.de Webcode p112190, im Volltext gelesen am 21.08.2026. © DGUV, urheberrechtlich geschützt, hier nur Tatsachen übernommen, kein Wortlaut.

  Verwendet für: `taktisch-atemschutz-gegen-gas`.

### Internationales Komitee vom Roten Kreuz (IKRK)

- David Lloyd Roberts, „Staying Alive“, IKRK Genf 1999, überarbeitete Fassung 2005 (Ref. 0717/002), Abschnitt „Personal Protective Equipment“, S. 83, am 21.08.2026 im Volltext gelesen: Bart oder mehrtägige Stoppeln unterbrechen die Dichtlinie des Atemschutzgeräts. © IKRK, nur Beleg.

  Verwendet für: `taktisch-atemschutz-gegen-gas`.

### Centers for Disease Control and Prevention (CDC)

- Fact Sheet „Facts About Riot Control Agents“ (Interim document), zuletzt inhaltlich geprüft 22.02.2006, archivierte Fassung cybercemetery.unt.edu/oilspill/20130227084908mp_/http://emergency.cdc.gov/agent/riotcontrol/factsheet.asp, im Volltext gelesen am 21.08.2026. Werk der US-Bundesregierung, gemeinfrei (17 U.S.C. § 105).

  Verwendet für: `erste-hilfe-traenengas-pfefferspray`.

### Tidwell RD, Wills BK (StatPearls / NCBI Bookshelf, NIH)

- „Tear Gas and Pepper Spray Toxicity“, StatPearls [Internet], Treasure Island (FL): StatPearls Publishing, Stand 14.05.2023, ncbi.nlm.nih.gov/books/NBK544263/, Abschnitte Mechanism of Action, Treatment/Management, Complications, im Volltext gelesen am 21.08.2026. Frei zugänglich über NCBI Bookshelf, nur Tatsachen übernommen.

  Verwendet für: `erste-hilfe-traenengas-pfefferspray`.

### Hon KL, Leung KKY, Leung AKC (Hong Kong Med J)

- „Health effects of tear gas exposure in children, infants, and fetuses“, Hong Kong Med J 2020;26:351–352, doi.org/10.12809/hkmj198171, im Volltext gelesen am 21.08.2026 (hkmj.org/system/files/hkmj198171.pdf). © 2020 Hong Kong Academy of Medicine, CC BY-NC-ND 4.0, nur Tatsachen übernommen.

  Verwendet für: `erste-hilfe-traenengas-pfefferspray`.

### Yeung MF, Tang WYM (Hong Kong Med J)

- „Clinicopathological effects of pepper (oleoresin capsicum) spray“, Hong Kong Med J 2015;21(6):542–552, doi.org/10.12809/hkmj154691, Abschnitt „Decontamination“, im Volltext gelesen am 21.08.2026 (hkmj.org/system/files/hkmj154691.pdf). Hong Kong Academy of Medicine, nur Tatsachen übernommen.

  Verwendet für: `erste-hilfe-traenengas-pfefferspray`.

### Tierärztliche Vereinigung für Tierschutz e.V. (TVT)

- Merkblatt Nr. 108 „Verhalten beim Aufeinandertreffen mit einem freilaufenden Hund“, bearbeitet von Dr. H. Jahn (Arbeitskreis 2, Kleintiere), Stand August 2006, tierschutz-tvt.de, im Volltext gelesen am 21.08.2026. Urheberrechtlich geschützt; hier ausschließlich in eigenen Worten sinngemäß wiedergegeben, kein Originalzitat.

  Verwendet für: `erste-hilfe-hund-angriff`.

### Infomed-Verlags-AG

- „pharma-kritik“, Jahrgang 24, Nummer 15 (Artikel PK66) „Bissverletzungen durch Säugetiere“, Redaktionsschluss 6. Februar 2003, Review Blum/Furrer/Hatz/Malinverni/Vogt, infomed.ch, abgerufen am 21.08.2026. Nur die Angaben zu Mundflora, Erregern und Infektionsrate übernommen; die dortige Empfehlung zum primären Wundverschluss NICHT übernommen, siehe „Nähen oder offen lassen: die Entscheidung“.

  Verwendet für: `erste-hilfe-hund-angriff`.

### Programm Polizeiliche Kriminalprävention der Länder und des Bundes (ProPK)

- Broschüre „SICHER WOHNEN – Einbruchschutz – Informationen Ihrer Polizei“, Stand 07/2024, S. 8f. (Frage „... man Einbrecher aufhalten muss?“: Konfrontation vermeiden, Notruf 110) und S. 13 (Notruf-Angaben, Nachbarschaftshilfe). polizei-beratung.de/fileadmin/Medien/001-BR-Sicher-wohnen.pdf, vollständig gelesen am 21.08.2026.

  Verwendet für: `taktisch-einbrecher-in-der-wohnung`.

- Faltblatt „Ungebetene Gäste – Einbruchschutz“, Herausgeber-Angabe Stand 12/2017, S. 4–8 (mechanische Sicherung, Türspion/Sperrbügel, Nachbarschaftshilfe, doppelt abschließen). polizei-beratung.de/fileadmin/Medien/010-FB-Ungebetene-Gaeste.pdf, vollständig gelesen am 21.08.2026.

  Verwendet für: `taktisch-einbrecher-in-der-wohnung`.

- Hinweise zum Verhalten nach einem Einbruch: Wohnung nicht wieder betreten, nichts verändern, um Spuren zu erhalten. polizei-beratung.de, vollständig gelesen am 21.08.2026.

  Verwendet für: `taktisch-absperrung-sperrzone`.

### Netzwerk „Zuhause sicher“ (Polizeibehörden, Kommunen, Handwerksorganisationen, Versicherungswirtschaft)

- „Das richtige Verhalten bei einem Einbruch: Eigenschutz geht vor!“, Interview mit Carolin Hackemack (Geschäftsführerin des Netzwerks), Szenarien 1–3. polizei-dein-partner.de, vollständig gelesen am 21.08.2026; Telefon-am-Bett-Tipp ergänzend zuhause-sicher.de/einbruchschutz/richtiges-verhalten/zuhause/, gelesen am 21.08.2026.

  Verwendet für: `taktisch-einbrecher-in-der-wohnung`.

### Deutsche Feuerwehren (Landesausbildungsstellen), Feuerwehr-Dienstvorschrift 500

- FwDV 500 „Einheiten im ABC-Einsatz“, Stand Januar 2022, Ziff. 1.5.3.2 (GAMS-Regel), Ziff. 1.5.3.5 (Gefahren-, Absperr- und Übergangsbereich mit Abstandsangaben) und Anlage 1 (Begriffsbestimmungen). Veröffentlicht u. a. über ibk-heyrothsberge.sachsen-anhalt.de, vollständig gelesen am 21.08.2026.

  Verwendet für: `taktisch-absperrung-sperrzone`.

### Bundesministerium der Justiz (Deutschland)

- Anlage 1 Abschnitt 1 Unterabschnitt 2 Nr. 1.1 (Hieb-/Stoßwaffen allgemein), Anlage 2 Abschnitt 1 Nr. 1.3.2 (Stahlruten/Totschläger/Schlagringe), § 42a Abs. 1 Nr. 2, § 52 Abs. 3 Nr. 1 und § 53 Abs. 1 Nr. 21b WaffG im Wortlaut. Gesetze-im-internet.de, tagesaktuelle Fassung, vollständig gelesen am 21.08.2026.

  Verwendet für: `taktisch-schlagstock-totschlaeger`.

### Bundesgerichtshof

- Beschluss vom 21.04.2022, Az. 3 StR 81/22 (Antragsschrift des Generalbundesanwalts, vom 3. Strafsenat bestätigt): Teleskopschlagstock ist Waffe nach § 1 Abs. 2 Nr. 2a i.V.m. Anlage 1 WaffG, aber kein Totschläger i.S. Anlage 2 mangels Biegsamkeit. Volltext hrr-strafrecht.de/hrr/3/22/3-81-22.php, vollständig gelesen am 21.08.2026.

  Verwendet für: `taktisch-schlagstock-totschlaeger`.

### Bundeskanzleramt der Republik Österreich (RIS)

- § 17 Abs. 1 Z 6 und § 50 Abs. 1 Z 2 Waffengesetz 1996 im Wortlaut. Ris.bka.gv.at, Gesetzesnummer 10006016, tagesaktuelle Fassung, abgerufen am 21.08.2026.

  Verwendet für: `taktisch-schlagstock-totschlaeger`.

### Oberster Gerichtshof (Österreich)

- 14 Os 56/20x, Entscheidung vom 21.07.2020: Definition „Totschläger“/„Stahlrute“ (Biegsamkeit als wesentliches Kriterium), Abgrenzung zu Tonfa/Mehrzweckeinsatzstock, Einzelfallprüfung bei Teleskopschlagstock. Wiedergabe jusguide.at, gelesen am 21.08.2026.

  Verwendet für: `taktisch-schlagstock-totschlaeger`.

### Bundesversammlung der Schweizerischen Eidgenossenschaft (Fedlex)

- Art. 4 Abs. 1 Bst. d und Art. 5 Abs. 2 Bst. b Waffengesetz (WG, SR 514.54) im Wortlaut: Schlagstöcke als namentlich genannte Waffe, ausdrückliche Ausnahme vom Übertragungs-/Erwerbsverbot. Fedlex.admin.ch, Stand 1.9.2023, im Browser gelesen am 21.08.2026 (Seite nur mit JavaScript lesbar).

  Verwendet für: `taktisch-schlagstock-totschlaeger`.

### National Institute of Justice (NIJ), U.S. Department of Justice

- National Law Enforcement Technology Center Bulletin „Positional Asphyxia – Sudden Death“, Juni 1995, Office of Justice Programs, ojp.gov/pdffiles/posasph.pdf, im Volltext gelesen am 21.08.2026: Physiologie der Bauchlage unter Druck, Risikofaktoren, Warnzeichen (Reglosigkeit nach einem Gerangel), Empfehlung Rückenlage statt Bauchlage. Werk der US-Bundesregierung, gemeinfrei.

  Verwendet für: `taktisch-gefesselt-koerper`.

### Europäisches Komitee zur Verhütung von Folter (CPT), Europarat

- „Means of restraint in psychiatric establishments for adults (Revised CPT standards)“, CPT/Inf(2017)6, Straßburg, 21.03.2017, rm.coe.int/16807001c3, im Volltext gelesen am 21.08.2026: Grundsatz 3.3 (Rückenlage mit nach unten positionierten Armen, Gurte dürfen Atmung und Verständigung nicht behindern) und Abschnitt 4 (Dauer). Europarat, nur als Beleg.

  Verwendet für: `taktisch-gefesselt-koerper`.

### Masri BA, Eisen A, Duncan CP, McEwen JA

- „Tourniquet-induced nerve compression injuries are caused by high pressure levels and gradients“, BMC Biomedical Engineering 2, 2020, Artikel 7, doi.org/10.1186/s42490-020-00041-5, im Volltext gelesen am 21.08.2026: Mechanismus (Druck UND Druckgefälle an den Rändern, nach Ochoa et al. 1972), schmale vs. breite Bänder, Radialnerv am empfindlichsten. Beschreibt chirurgische Blutsperren, keine Fesseln – CC-Lizenz, offen zugänglich.

  Verwendet für: `taktisch-gefesselt-koerper`.

### Jano F, MacKenzie K, Bilolikar VK, Goldberger D, Tuluca A

- „Acute Carpal Tunnel Syndrome Secondary to Handcuffs Necessitating Emergency Orthopedic Consultation and Operative Intervention“, JACEP Open 6(1), 2025, 100013, doi.org/10.1016/j.acepjo.2024.100013, im Volltext gelesen am 21.08.2026. Einzelner Fallbericht, von den Autoren selbst als erster dokumentierter Fall dieser Schwere bezeichnet. Open Access, CC BY 4.0.

  Verwendet für: `taktisch-gefesselt-koerper`.

### Deutsche Gesetzliche Unfallversicherung e.V. (DGUV)

- DGUV Regel 113-004 „Behälter, Silos und enge Räume – Teil 1: Arbeiten in Behältern, Silos und engen Räumen“, Ausgabe Februar 2019, publikationen.dguv.de, im Volltext gelesen am 21.08.2026: Begriffsbestimmung enge Räume und Sauerstoffmangel (Abschnitte 2.1 und 2.11), Ursachen im Anhang. Für Arbeitsschutz mit Messtechnik geschrieben, hier nur als Beleg für die Grundgefahr.

  Verwendet für: `taktisch-eingesperrt-enger-raum`.

### McLaren C, Null J, Quinn J

- „Heat Stress From Enclosed Vehicles: Moderate Ambient Temperatures Cause Significant Temperature Rise in Enclosed Vehicles“, Pediatrics 116(1), Juli 2005, e109–e112, doi:10.1542/peds.2004-2368, im Volltext gelesen am 21.08.2026: Messreihe zum Temperaturanstieg im Fahrzeuginnenraum (nicht zur Kinderphysiologie). Für den Schutz zurückgelassener Kinder verfasst; hier nur die Fahrzeugphysik verwendet.

  Verwendet für: `taktisch-eingesperrt-enger-raum`.

### St John Ambulance

- „Alcohol Poisoning Symptoms & First Aid“, sja.org.uk/first-aid-advice/alcohol-poisoning/, abgerufen am 21.08.2026

  Verwendet für: `erste-hilfe-alkoholvergiftung`.

### Deutsche Lebens-Rettungs-Gesellschaft (DLRG)

- „Die Baderegeln“, bundesweite DLRG-Kampagne, hier gelesen auf der Presseseite des DLRG-Landesverbands Nordrhein, nordrhein.dlrg.de, abgerufen am 21.08.2026

  Verwendet für: `erste-hilfe-baderegeln-freigewaesser`.

### Deutscher Alpenverein (DAV)

- Artikel „Notruf und Rettung in den Alpen“, alpenverein.de, Stand 6. Juli 2025: alpines Notsignal als sechsmal in der Minute gegebenes optisches oder akustisches Zeichen, danach eine Minute Pause und Wiederholung; Antwortsignal dreimal in der Minute, ebenfalls mit einer Minute Pause. Abgerufen am 21.08.2026.

  Verwendet für: `orientierung-alpines-notsignal`.

- Max Bolland, „So geht das: Notbiwak am Berg“, DAV Panorama, Ausgabe 4/2020, 13.07.2020 (zuletzt geändert 19.03.2026), alpenverein.de/artikel/notbiwak-am-berg. Nur Tatsachen übernommen, Wortlaut und Gliederung eigen. Vollständig gelesen und abgerufen am 21.08.2026. Urheberrechtlich geschützter Zeitschriftenartikel, keine gemeinfreie Quelle.

  Verwendet für: `taktisch-notbiwak-biwaksack`.

### Bergrettung Salzburg

- Seite „Alpines Notfallsignal“, bergrettung-salzburg.at: dieselbe Definition (sechsmal in der Minute, eine Minute Pause, Antwortsignal dreimal in der Minute) sowie zusätzlich Kamerablitze als mögliches Signalmittel neben Rufen, Pfeifen und Lampenblitzen. Kein Stand auf der Seite angegeben, abgerufen am 21.08.2026.

  Verwendet für: `orientierung-alpines-notsignal`.

### Umweltbundesamt (UBA)

- „FAQs zu Nitrat im Grund- und Trinkwasser“, umweltbundesamt.de, im Volltext gelesen am 21.08.2026. Daher der Grenzwert von 50 Milligramm Nitrat je Liter, die Erklärung zur Säuglingszyanose durch Nitrit-Bildung im weniger sauren Säuglingsmagen mit anschließender Methämoglobin-Bildung, und der Satz, dass die Landwirtschaft für den Großteil der Nitrateinträge ins Grundwasser verantwortlich ist (Jauche, Gülle, Mist, Gärreste, Mineraldünger).

  Verwendet für: `wasser-nitrat-eigener-brunnen`.

### The Stone Trust

- Brian Post, Peter Welch: „How To Build a Sturdy Dry Stone Wall“ (The FIVE Basic Rules Of Dry Stone Walling), The Stone Trust, Dummerston, Vermont, ©2018, thestonetrust.org. Gemeinnützige Organisation zur Erhaltung des Trockenmauer-Handwerks. Vollständig gelesen und abgerufen am 21.08.2026. Urheberrechtlich geschütztes Merkblatt, keine gemeinfreie Quelle; nur Tatsachen übernommen, Wortlaut und Gliederung eigen, keine Abbildung übernommen.

  Verwendet für: `taktisch-trockenmauer-bauen`.

## 21.08.2026 — Kampfstoff, gemessene Lücken und vier neue Themenfelder

Alle Belege dieser Nacht, die vorher noch nirgends in dieser Datei standen.
Die beiden US-Nahkampf-Handbücher (FM 3-25.150 und FM 21-150) stehen bereits
weiter oben und sind hier nicht wiederholt — sie tragen den weitaus größten
Teil der neuen Einträge.

**Lizenzlage:** Nicht jede dieser Quellen ist frei. Wo eine geschützte Quelle
steht, ist ausschließlich die TATSACHE übernommen und der Text selbst
geschrieben — Tatsachen, Zahlen und Arbeitsverfahren sind nicht geschützt.
Die einzige wörtliche Übernahme dieser Nacht ist ein Satz aus der
ProPK-Broschüre „Sicher wohnen“ und der Wortlaut von § 127 Absatz 1 Satz 1
StPO; beide sind als Zitat gekennzeichnet, der Gesetzestext ist zudem
amtliches Werk und damit gemeinfrei.

### US Army / US Special Operations Command

- „ST 31-91B Special Forces Medical Handbook“, 1982, Abschnitt 2-31 CHOLERA: explosiver Beginn, häufiger, wässriger, geruch- und farbloser Stuhl bis 1 Liter je Stunde, rasche Austrocknung mit Schock; Unschädlichmachung der Ausscheidungen unerlässlich, strenge Absonderung unnötig, Quarantäne unerwünscht. Volltext work/quellen/werke-frei/us-army-st31-91b-sf-medical-handbook-1982-fulltext.txt, gelesen 21.08.2026. US-Bundesregierung, frei nach § 121 Abs. 4 UrhG, 17 U.S.C. § 105.

  Verwendet für: `medizin-cholera`.

### Ärzte ohne Grenzen (MSF)

- MSF Medical Guidelines, Cholera-Leitlinie, Anhang 15 „Preparation and use of chlorine solutions“, medicalguidelines.msf.org, abgerufen am 21.08.2026: 0,2 % Chlorlösung für Böden, Flächen, Material, Schürzen, Stiefel, Geschirr sowie Eimer für Stuhl und Erbrochenes; 0,05 % für Hände und Wäsche; 15 Minuten Einwirkzeit; Verdünnungsformel für Flüssigbleiche (Prozent Chlor in Bleiche geteilt durch gewünschte Prozentzahl, minus eins ergibt Wasserteile je Bleicheteil).

  Verwendet für: `hygiene-chlor-flaechen`.

### TRBA 250 (Technische Regel für Biologische Arbeitsstoffe)

- „Biologische Arbeitsstoffe im Gesundheitswesen und in der Wohlfahrtspflege“, wiedergegeben auf abfallmanager-medizin.de, „Sichere Entsorgung von Spritzen“, abgerufen am 21.08.2026: kein Recapping (Schutzkappe nicht zurückstecken), Kanüle nicht verbiegen oder abknicken, durchstich- und bruchfeste, fest verschließbare Behälter nötig.

  Verwendet für: `hygiene-nadeln-entsorgen`.

### entsorgen.org

- „Spritzen entsorgen“, entsorgen.org, abgerufen am 21.08.2026: bruchfeste, verschließbare Behelfsbehälter aus dem Haushalt wie eine Blechdose für Kaffee oder Tee; Einwickeln allein reicht nicht; volle Behälter zu Arztpraxis oder Apotheke, ersatzweise fest verschlossen in die Restmülltonne.

  Verwendet für: `hygiene-nadeln-entsorgen`.

### Apotheken Umschau

- „So entsorgen Sie Ihren Diabetes-Müll“, apotheken-umschau.de, abgerufen am 21.08.2026: lose Nadeln und Lanzetten im Müllbeutel können andere verletzen; Apotheken bieten Sammelboxen; Nadelknipser als Werkzeug; leere Teststreifendosen lassen sich für gebrauchte Lanzetten und Streifen weiterverwenden.

  Verwendet für: `hygiene-nadeln-entsorgen`.

### Wikipedia (deutschsprachig)

- Artikel „Herbstzeitlose“: Abschnitte zur Giftigkeit für Tierarten, Colchicin-Gehalt nach Pflanzenteil, Erhalt beim Trocknen, Übergang ins Milch. Vollständig gelesen, abgerufen am 21.08.2026. CC BY-SA 4.0, Versionsgeschichte als Autorennachweis.

  Verwendet für: `nahrung-tier-gift-heu-weide`.

- Artikel „Jakobs-Greiskraut“ (dort auch als Jakobs-Kreuzkraut geführt): Abschnitte Inhaltsstoffe und Giftigkeit sowie Verwechslung mit anderen Pflanzen. Vollständig gelesen, abgerufen am 21.08.2026. CC BY-SA 4.0.

  Verwendet für: `nahrung-tier-gift-heu-weide`.

- Artikel „Eiben“ (Weiterleitung von „Eibe“): Abschnitt Inhaltsstoffe, giftige Pflanzenteile und dokumentierte tödliche Vergiftungsfälle bei Rindern und Pferden. Vollständig gelesen, abgerufen am 21.08.2026. CC BY-SA 4.0.

  Verwendet für: `nahrung-tier-gift-heu-weide`.

- Artikel „Glutinleim“ (Weiterleitung von „Knochenleim“ und „Hautleim“): Herkunft aus tierischen Abfällen durch Auskochen, Unterscheidung von Knochen-, Haut-, Fisch- und Hasenleim, Verwendung im Musikinstrumentenbau wegen der Lösbarkeit mit Wärme. Vollständig gelesen, abgerufen am 21.08.2026. CC BY-SA 4.0.

  Verwendet für: `nahrung-knochenleim-hautleim`.

- Artikel „Dengeln“: Verfahren, Werkzeuge (Dengelhammer mit 250–600 g Kopfgewicht, Dengelamboss, Dengelleier, Schlagdengler), Breite des Dengels von 3–7 mm, Zusammenspiel von flacher und spitzer Seite, Befestigung im Pflock oder Dengelstock. Vollständig gelesen, abgerufen am 21.08.2026. CC BY-SA 4.0, Versionsgeschichte als Autorennachweis.

  Verwendet für: `taktisch-sense-dengeln`.

### Wikipedia (englischsprachig)

- Artikel „Animal glue“, Abschnitte „Hide glue“ und „Production“: Herstellung durch Einweichen und Erhitzen auf rund 70 °C in mehreren Durchgängen, Verarbeitungstemperatur rund 60 °C, kurze offene Zeit von rund einer Minute, Wiederverwendbarkeit nach erneutem Erwärmen, Kühllagerung gegen Verkeimung. Vollständig gelesen, abgerufen am 21.08.2026. CC BY-SA 4.0, Versionsgeschichte als Autorennachweis.

  Verwendet für: `nahrung-knochenleim-hautleim`.
