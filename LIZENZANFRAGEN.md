# Lizenz- und Quellenanfragen

Stand: 28.07.2026. Diese Datei hält fest, wo eine Erlaubnis eingeholt wurde
oder noch aussteht. **Vor jeder Veröffentlichung ist sie zu prüfen** — sie ist
der Ort, an dem steht, was das Haus nicht verlassen darf.

Nichts hier ist eine Rechtsberatung. Wo eine Einschätzung steht, ist sie als
solche gekennzeichnet.

## Karte: zwei Lizenzen, die nicht in dieselbe Datei passen — für Max

Geprüft am 04.08.2026, beide Lizenztexte selbst geladen und im Volltext
gelesen. **Kein Rechtsrat, sondern der Befund.**

### Die Kartendaten sind ODbL, nicht CC BY-SA

Die gebaute Karte stammt aus OpenStreetMap. OSM steht unter der **Open
Database License 1.0**, nicht unter CC BY-SA. Die Roadmap hat am 28.07.2026
„CC BY-SA für die Inhaltspakete" festgelegt — für die Tipps stimmt das, für
die Karte nicht. Eine `.czk` ist eine **abgeleitete Datenbank** im Sinne der
ODbL: Sie muss unter ODbL weitergegeben werden und braucht den Hinweis
„© OpenStreetMap-Mitwirkende".

**Folge:** Die Kartendatei braucht eine eigene Lizenzangabe, getrennt von der
der Inhaltspakete. Das ist mit dem gewählten Aufbau kein Problem — die Karte
liegt ohnehin in einer eigenen Datei.

### Die Geländeform passt in keine der beiden

Für die noch fehlende sechste Datensorte kommen zwei Höhenmodelle in Frage.
Beide sind geprüft:

**GMTED2010 (USGS) — für Europa AUSGESCHIEDEN.** Auf den ersten Blick ideal:
US-Behördendaten, gemeinfrei, Abdeckung bis 84°N. Der Primärbeleg (Danielson &
Gesch 2011, USGS Open-File Report 2011‑1073, selbst geladen, Volltext unter
`work/karte/quellen/`) sagt aber, woher die Höhen im Einzelnen stammen:
unter 60°N aus SRTM, **nördlich von 60°N in Eurasien aus NGA-DTED‑1**, und
Löcher wurden „primarily from non-SRTM DTED, NED, and SPOT 5" gefüllt.
**SPOT 5 Reference3D ist co-produziert von Spot Image und IGN** — ein
kommerzielles Werk. Die Alpen haben bekanntermaßen SRTM-Löcher. Damit lässt
sich für Europa nicht sagen, welche Bildpunkte gemeinfrei sind und welche
nicht. Nach der Hausregel („bei jeder Quelle die Lizenz prüfen, nicht nur die
Fachlichkeit") fällt es damit aus.

**Copernicus WorldDEM‑30 / GLO‑90 (ESA) — nutzbar, aber mit Auflagen.**
Lizenztext gelesen (`work/karte/quellen/copdem-lizenz.pdf`). Artikel 4 gewährt
ausdrücklich Vervielfältigung, Verbreitung, öffentliche Wiedergabe und
Bearbeitung; Artikel 3 weltweit und unbefristet; Artikel 5 kostenlos.
Artikel 6 verlangt dafür einen **wörtlichen Quellenhinweis** und, dass jeder
Weiterempfänger den Haftungsausschluss mitbekommt. Artikel 9 stellt klar, dass
keine Rechte übergehen; der vorgeschriebene Hinweis endet auf „all rights
reserved".

**Das ist der Haken, und er ist Max' Entscheidung:** Ein Werk unter CC BY-SA
oder ODbL verlangt, dass das GANZE abgeleitete Werk unter derselben Lizenz
weitergegeben wird. Die Copernicus-Höhendaten können das nicht — sie bleiben
bei ihrem eigenen Vorbehalt. Beides in **einer** Datei zu mischen ginge
deshalb nicht sauber.

**Vorschlag (nicht umgesetzt, weil es eine Lizenzentscheidung ist):** Die
Geländeform reist als **eigene Datei** neben der Karte, mit eigenem
Lizenzhinweis, und wird von der App getrennt geladen. Dann steht jede Datei
unter genau einer Lizenz und keine erbt die Auflagen der anderen.

**Was Max entscheiden muss:**
1. Geländeform aus Copernicus mit den Auflagen aus Artikel 6 — ja oder nein?
2. Wenn ja: eigene Datei neben der Karte (Vorschlag) oder anders?
3. Die Lizenzangabe der Kartendatei auf ODbL umstellen — bestätigen.

## Offene Sperre: Phrasenkatalog des Übersetzers

Der Satzkatalog des Übersetzers übernimmt die 62 Sätze des „Emergency
Multilingual Phrasebook" des British Red Cross in sechs Sprachen. Das Werk
steht unter **keiner freien Lizenz**.

**SPERRE: Solange keine Erlaubnis vorliegt, darf kein Paket mit
`content/phrases.json` veröffentlicht werden** — nicht als APK-Asset, nicht
als eigenes Paket, nicht im offenen Repo. Entwicklung und lokaler Betrieb sind
davon unberührt.

**Umgesetzt:** Diese Fassung des Projekts wird ohne die Datei ausgeliefert.
Das Basispaket führt die Art `phrases` nicht im Manifest, der Übersetzer
meldet einen leeren Katalog. Kommt die Erlaubnis, wandern Datei und
Manifest-Eintrag zurück.

Angefragt am 28.07.2026 bei `contactus@redcross.org.uk`. Die Anfrage bittet um
Erlaubnis für genau diese App und bietet an, den Phrasenteil von der
CC-BY-SA-Lizenz auszunehmen und mit eigenem Hinweis zu versehen, damit der
Rechteinhaber die Kontrolle behält. Antwort steht aus.

### Geprüfte Alternative: Refugee Phrasebook (CC0) — ersetzt den Katalog NICHT

Geprüft am 03.08.2026. Das **Refugee Phrasebook** (Open Knowledge Foundation
Deutschland) steht unter **CC0**, ist also gemeinfrei und ohne Auflage nutzbar.
Alle sechs Sprachen des Pakets sind enthalten und praktisch vollständig
übersetzt (135 Phrasen, Spanisch 134).

**Trotzdem kann es den gesperrten Katalog nicht ersetzen:** Von unseren 62
Sätzen hat nur 2 eine Entsprechung. Der Grund ist grundsätzlich — die Werke sind
aus entgegengesetzter Richtung geschrieben. Das Refugee Phrasebook lässt den
**Betroffenen** sprechen („Ich muss zum Arzt"), unser Katalog den **Helfer**
(„Haben Sie Schmerzen? Zeigen Sie bitte, wo").

**Was es dafür kann:** die Gegenrichtung abdecken, die dem Paket ohnehin fehlt.
Läge dieser Teil in einer eigenen Datei, wäre ein Paket OHNE `phrases.json`
veröffentlichbar — ein Übersetzer, der nur eine Richtung kann, dafür aber frei
ist. Damit wäre die Sperre umgangen, ohne sie zu brechen.

**Das ist ein Umbau am Paketformat und an der Darstellung und braucht Max'
Entscheidung.** Der vollständige Befund samt Daten liegt in
`work/quellen/refugee-phrasebook/BEFUND.md`.

## Verschickte Anfragen

| Datum | Empfänger | Werk | Worum gebeten wird | Stand |
|---|---|---|---|---|
| 28.07.2026 | British Red Cross, `contactus@redcross.org.uk` | Emergency Multilingual Phrasebook | Erlaubnis, die 62 Sätze in dieser App zu nutzen; Phrasenteil außerhalb der freien Lizenz | offen |
| 28.07.2026 | Joseph und Amy Alton, Kontaktformular `doomandbloom.net/contact-2/` | The Ultimate Survival Medicine Guide | Erlaubnis für die Nutzung von Fakten in eigenen Worten; zusätzlich Bitte um fachlichen Blick eines Arztes auf medizinische Einträge | offen |
| 28.07.2026 | HarperCollins UK, `rig.public@harpercollins.co.uk` | SAS Survival Handbook (Wiseman) | Erlaubnis für Fakten in eigenen Worten; nachrangig Bitte um durchsuchbares PDF | offen |
| 28.07.2026 | DK / Penguin Random House, `dkpermissions@penguinrandomhouse.co.uk` | The Self-Sufficient Life (Seymour) | dasselbe | offen; DK nennt selbst bis zu zehn Wochen Bearbeitungszeit |
| 28.07.2026 | Hesperian Health Guides, `permissions@hesperian.org` | A Book for Midwives, Where There Is No Dentist | Ausnahme von der Digitalsperre für diese App; Fakten in eigenen Worten mit Quellenangabe | offen |
| 28.07.2026 | Médecins Sans Frontières, `media@london.msf.org` | Clinical Guidelines, Essential Drugs | Fakten in eigenen Worten mit Quellenangabe | offen; kein dedizierter Rechte-Kontakt auffindbar, Presse gebeten weiterzuleiten |

Der Wortlaut der Anfragen liegt außerhalb des Repos, weil dort persönliche
Kontaktdaten stehen; Kopien im Gesendet-Ordner des Postfachs.

## Geprüft und ausgeschieden

Diese Werke wurden geprüft und **dürfen nicht verwendet werden**. Wer sie
später erneut vorschlägt, findet hier den Grund:

| Werk | Grund |
|---|---|
| Hesperian, „Where There Is No Doctor" / „…No Dentist" | untersagt ausdrücklich jede Nutzung „in any digital format"; fachlich ideal, aber für eine App unbrauchbar |
| FAO-Publikationen | durchgängig Non-Commercial-Klauseln, mit CC BY-SA unvereinbar |
| WHO / UNICEF-Handbücher | ebenfalls NC-Klauseln |
| Wikibooks „Knotenfibel" | GNU Free Documentation License, mit CC BY-SA unvereinbar. War bereits als Beleg für den Gardaknoten im Flaschenzug eingebaut und wurde am 28.07.2026 wieder entfernt |
| Sammlung CD3WD | **kein Lizenzbeleg.** Enthält nachweislich geschützte Werke, darunter Seymours „Self-Sufficient Gardener". Jede Datei daraus einzeln prüfen |
| „The Ship Captain's Medical Guide" (UK) | trotz Crown Copyright gesperrt: „not freely available … may not be reproduced without permission of TSO". War der aussichtsreichste Kandidat und fällt aus |
| „Emergency War Surgery" (US DoD) | trägt trotz Bundesbehörde „ALL RIGHTS RESERVED" |
| AWMF-Leitlinien, RKI, BZgA | je eigener Rechtevorbehalt, geprüft am 28.07.2026 — als Beleg zitierfähig, aber nicht weitergebbar |

## Frei verwendbar, ohne Anfrage

| Werk | Lizenzlage |
|---|---|
| US Army FM 3-05.70 „Survival" | Werk der US-Bundesregierung, gemeinfrei. Trägt Unterstand, Feuer, Knoten |
| US Army FM 5-125 „Rigging Techniques" | gemeinfrei. Trägt den Flaschenzug |
| Peace Corps ICE-Handbücher mit Freigabeklausel im Impressum | Lebensmittel trocknen (T0020, 1984), Appropriate Community Technology (über Appropedia CC BY-SA 3.0), Wasser/Sanitär-Fallstudien (CS-4, 1984). **Nur die Bände mit eigener Klausel** — zwei weitere derselben Reihe haben keine und bleiben ungeklärt |
| **USDA Farmers' Bulletins** (historische Reihe des US-Landwirtschaftsministeriums) | Gemeinfrei, und zwar auf zwei Beinen. ERSTENS als Werk einer US-Bundesbehörde nach 17 U.S.C. §105 — wie beim SOF-Handbook eine ABLEITUNG aus der Urheberschaft, die Bulletins selbst tragen keinen Freigabevermerk. ZWEITENS durch Alter: Die verwendeten Hefte stammen von 1894 bis 1960. Dazu kommt ein Vermerk der abgebenden Einrichtung: Die Sammlung `usda-farmersbulletin` im Internet Archive führt zu jedem Heft „The contributing institution believes that this item is not in copyright" — selbst nachgesehen am 09.08.2026 an den Nummern 21, 192, 278, 756 und 2145. Trägt die Kapitel Kartoffel, Lagern, Saatgut und Roggen. **Nachgetragen am 09.08.2026: Die Reihe stand bis dahin NICHT in dieser Datei, obwohl sie seit dem 29.07. Inhalt trägt.** |
| **Special Operations Forces Medical Handbook** (US SOCOM, 2001) | **Gemeinfrei als Werk einer US-Bundesbehörde nach 17 U.S.C. §105 — das Dokument selbst enthält KEINEN Freigabevermerk.** Am 28.07.2026 gezielt nachgesehen: „public domain", „approved for public release", „distribution statement" kommen im Volltext null mal vor. Die Gemeinfreiheit ist also eine Ableitung aus der Urheberschaft (durchgängig US-Militärärzte, Herausgeber SOCOM), keine Aussage des Werkes. Wer später „laut Dokument gemeinfrei" schreibt, zitiert etwas, das dort nicht steht. Gesichert unter `work/quellen/werke-frei/`. Deckt sechs bis acht der neun Lückenthemen: Zahnmedizin, Infektionen, Atemwegsinfekte samt Isolierung, Läuse, Höhenkrankheit, Kopfschmerz, Augenverletzung, Ohrenschmerz, Hämorrhoiden. **DREI Rechtevorbehalte im Werk, alle selbst nachgeprüft — siehe eigener Abschnitt unten** |
| US Army ST 31-91B (1982) | älteres Schwesterwerk, gemeinfrei, ohne Merck-Ausnahme; schlechte OCR-Qualität, nur als Zweitbeleg |
| US Army TC 4-02.1 „First Aid" (2016/2018) | „Approved for public release; distribution is unlimited". Erste Hilfe, Verletztentransport |
| CDC-Materialien | „not subject to copyright, is in the public domain" (Seite „Use of Agency Materials"). Verlangt Namensnennung „Source: CDC" und einen Nicht-Endorsement-Hinweis — beides mit CC BY-SA vereinbar |
| NCCIH/NIH „Herbs at a Glance" | „This publication is not copyrighted and is in the public domain. Duplication is encouraged." |
| **BfArM-Mustertexte für Fachinformationen** (z. B. Ibuprofen, Muster-Nr. 8000197) | Amtliches Werk einer deutschen Bundesbehörde. Trägt die Dosierungsspanne im Regelblutungs-Tipp. Aufgenommen 03.08.2026 |
| **FDA-Verbraucherseiten** (z. B. „Don't Be Tempted to Use Expired Medicines") | Werk einer US-Bundesbehörde, gemeinfrei wie die übrigen FDA-Materialien. Trägt die Warnseite im Tipp zu abgelaufenen Medikamenten. Aufgenommen 03.08.2026 |
| **CDC-Empfängnisverhütung** („Effectiveness of Family Planning Methods" CS 242797; „U.S. Medical Eligibility Criteria for Contraceptive Use, 2016", MMWR 65/3) | fällt unter die CDC-Zeile oben. Trägt den gesamten Verhütungs-Tipp. Aufgenommen 03.08.2026 |

## OFFEN: „Intensive Vegetable Gardening" trägt fünf Kapitel und ist nicht ausdrücklich freigegeben

**Gefunden am 09.08.2026. Vor jeder Veröffentlichung zu klären.**

Das Peace-Corps-Handbuch „Intensive Vegetable Gardening for Profit and
Self-Sufficiency" (Publikation R0025) trägt derzeit FÜNF Agrikultur-Kapitel:
Kompost, Boden, Gießen, Aussaat und Fruchtfolge, dazu einen Teil von
Schädlinge. Es gehört aber NICHT zu den drei Bänden, die oben mit eigener
Freigabeklausel aufgeführt sind — es ist vermutlich einer der beiden, die dort
als „ungeklärt" stehen.

Was das Impressum selbst sagt, im Volltext nachgesehen
(`work/quellen/grosswerke/peacecorps-intensive-vegetable-gardening.txt`):

> „This publication was produced by the Peace Corps with funding from the
> U.S. Agency for International Development's Bureau of Food Security."

Was dort NICHT steht: kein Copyright-Vermerk, kein „all rights reserved", kein
Rechtevorbehalt, aber eben auch keine Freigabeklausel. Die Suche nach
„copyright", „permission", „all rights reserved", „may not be reproduced",
„reprinted", „public domain" und „courtesy" liefert im ganzen Werk genau EINEN
Treffer, und der ist eine allgemeine Aussage über die Reihe: „Some materials
are reprinted 'as is'".

**Einordnung:** Die Lage ist dieselbe wie beim SOF-Handbook — gemeinfrei als
ABLEITUNG aus der Urheberschaft einer US-Bundesbehörde, nicht als Aussage des
Werkes. Ein Unterschied zum bereits freigegebenen T0020 fällt allerdings auf:
Dort nennt das Impressum eine Firma als Verfasser (CHP International) UND trägt
eine Freigabeklausel. Hier nennt das Impressum die Behörde selbst als
Herstellerin und trägt keine. Das spricht eher FÜR als gegen die
Gemeinfreiheit, ist aber nicht dasselbe wie eine Zusage.

**Was zu tun ist:** entweder eine Anfrage an das Peace Corps wie bei den
anderen Werken, oder die fünf Kapitel auf USDA-Quellen umstellen, die
eindeutig sind. Max entscheidet. Bis dahin ist Entwicklung und lokaler Betrieb
unberührt — die Frage stellt sich erst bei der Weitergabe.

## OFFEN: USDA-Hefte mit namentlichem Verfasser, der nach 1955 gestorben ist

**Gefunden am 10.08.2026 beim Beeren-Kapitel. Vor jeder Veröffentlichung zu
klären.**

Die USDA Farmers' Bulletins gelten in diesem Projekt seit dem 09.08.2026 als
verwendbar, weil sie Werke einer US-Bundesbehörde sind. In den USA entsteht an
solchen Werken gar kein Urheberrecht, es gibt also auch keine Frist.

**Der offene Punkt:** Am 10.08.2026 kam die Merkzettel-Regel dazu, dass bei
alten Werken für Deutschland das STERBEJAHR des Verfassers plus siebzig Jahre
entscheidet. Bei einem Heft ohne namentlichen Verfasser stellt sich die Frage
nicht — eine Behörde stirbt nicht. Bei einem Heft MIT namentlichem Verfasser
kollidieren die beiden Überlegungen, sobald der Verfasser nach 1955 gestorben
ist.

**Der konkrete Fall:** Farmers' Bulletin 1398, „Currants and Gooseberries",
trägt im Text George M. Darrow als Verfasser. Darrow ist 1983 gestorben; nach
der Sterbejahr-Regel wäre das Heft in Deutschland bis 2053 geschützt.
Dasselbe gilt für die übrigen Erdbeer-Hefte von Darrow und für die
Beeren-Abschnitte in Bulletin 1870.

**Was daraus gefolgt ist:** Johannisbeeren und Stachelbeeren stehen NICHT im
Beeren-Kapitel. Es stützt sich nur auf Hefte, bei denen die Frage sich nicht
stellt — Corbett (gestorben 1940, Frist abgelaufen) und Hefte ohne
namentlichen Verfasser. Das ist im Schlussabschnitt des Kapitels offen
benannt.

**Wie das juristisch aufzulösen wäre** (Einordnung, keine Rechtsauskunft):
Der übliche Weg ist der Schutzfristenvergleich nach dem Berner Übereinkommen
— ein Werk, das im Ursprungsland gar nicht geschützt ist, wird in Deutschland
in der Regel auch nicht geschützt. Träfe das zu, wäre die ganze Sammlung
unabhängig vom Verfasser frei, und die Sterbejahr-Regel wäre für
US-Behördenwerke gegenstandslos.

**Was zu tun ist:** Max entscheidet, ob für USDA-Hefte die Behörden-Regel
oder die Sterbejahr-Regel gilt. Fällt die Entscheidung auf die
Behörden-Regel, können Johannisbeeren und Stachelbeeren aus Bulletin 1398
nachgetragen werden — das Heft liegt gelesen unter
`work/quellen/beeren/usda-johannisbeeren-1944.txt` bereit. Bis dahin ist
Entwicklung und lokaler Betrieb unberührt; die Frage stellt sich erst bei der
Weitergabe.

### Zweiter Fall derselben Frage, und dieser löst sich von allein

**Farmers' Bulletin 469 „Fats and Their Economical Use in the Home“ (1916),**
Arthur Dunham Holmes und Harold Locke Lang. Gelesen und gesichert unter
`work/quellen/seife/fatstheireconomi469holm.txt`, aber im Seifen-Kapitel
NICHT benutzt.

Holmes ist am **18.07.1956** gestorben. Die Schutzfrist läuft vom Ende des
Todesjahres, also bis zum **31.12.2026** — beim Schreiben des Kapitels am
10.08.2026 noch vier Monate zu früh. Zu Lang ist kein Sterbejahr belegt.

**Was zu tun ist: nichts, außer abzuwarten.** Ab dem 01.01.2027 ist das Heft
auch nach der strengeren Regel frei, jedenfalls für Holmes. Wer danach am
Seifen- oder Fett-Thema weiterarbeitet, kann es aufnehmen; Langs Sterbejahr
bleibt dann noch zu klären. Der Fall ist hier festgehalten, damit die Arbeit
von vier Monaten nicht verlorengeht.

**Gegenbeispiel aus demselben Kapitel, das zeigt, dass die Prüfung sich
lohnt:** Farmers' Bulletin 1099 „Home Laundering“ (1920) trägt ebenfalls
einen Namen — Lydia Ray Balderston. Sie ist am 26.02.1951 gestorben
(Wikidata Q108167889 nach der New York Times vom 27.02.1951), die Frist ist
abgelaufen, und das Heft ist ohne jeden Vorbehalt benutzt worden. Ein
namentlicher Verfasser ist also kein Ausschlussgrund, sondern ein
Prüfauftrag.

### Dritter Fall, und dieser reicht bis 2064

**Farmers' Bulletin 1060 „Onion Diseases and Their Control“ (Ausgabe 1947)
und Farmers' Bulletin 1955 „Onion-Set Production“ (Ausgabe 1946),** beide
von **J. C. Walker, 1893 bis 1994**. Gelesen und gesichert unter
`work/quellen/zwiebeln/`, im Zwiebel-Kapitel NICHT ausgewertet. Nach der
Sterbejahr-Regel wären sie in Deutschland **bis Ende 2064** geschützt.

Das wiegt schwerer als die beiden Fälle davor, weil Bulletin 354 „Onion
Culture“ — die Hauptquelle des Kapitels — den Leser für die Krankheiten
**ausdrücklich auf Bulletin 1060 verweist.** Die Quelle selbst sagt also,
wo weiterzulesen ist, und genau dorthin darf das Paket nicht folgen. Die
Lücke steht als Lücke im Kapitel.

**Dazu, kleiner:** „Longevity of Onion Seed in Relation to Storage
Conditions“ (1939), James H. Beattie und Victor R. Boswell. Zu beiden ist
kein Sterbejahr belegt. Deshalb enthält das Zwiebel-Kapitel keine Zahlen
zur Haltbarkeit von Zwiebelsamen, sondern nur die Aussage aus Bulletin 434,
dass die Keimkraft nach dem zweiten Jahr schnell nachlässt.

### Was das inzwischen bedeutet

Die Frage ist am 10.08.2026 **dreimal an einem Tag** auf dieselbe Weise
aufgetreten — Darrow (Johannisbeeren), Holmes (Fette), Walker (Zwiebeln) —
und hat jedes Mal Inhalt gekostet, der schon gelesen und ausgewertet war.
Solange sie offen ist, arbeitet das Projekt mit der strengeren Regel und
verliert dabei Stoff. **Es lohnt sich also, sie zu entscheiden, und zwar
eher früher als später.** Bis dahin ändert sich nichts an Entwicklung und
lokalem Betrieb; die Frage stellt sich erst bei der Weitergabe.

## Die drei Rechtevorbehalte im SOF Medical Handbook

Das Werk ist gemeinfrei, enthält aber drei Stellen mit fremdem Material. Sie
sind der Beweis dafür, dass §105 nicht alles im Werk deckt — wer daraus
schöpft, muss sie kennen:

1. **Laborwerte, Appendix A (S. A-43):** „All lab values are used by permission
   from The Merck Manual of Diagnosis and Therapy, Edition 17 … Copyright 1999
   by Merck & Co., Inc." — die Tabelle darf nicht übernommen werden.
2. **Bildtafeln zum Blutausstrich, Abschnitt „Appendices: Color Plates":**
   „All pictures taken with permission from Hoffbrand AV, and Pettit JE.
   Clinical Hematology. Gower Medical Publishing, London and New York. 1988".
   Betrifft die Figures 1–7 (neutrophile Granulozyten, Lymphozyten, Monozyten,
   Eosinophile, Basophile, Sichel- und Targetzellen, Thrombozyten). **Diese
   Tafeln dürfen weder übernommen noch als Vorlage nachgezeichnet werden** —
   eine nachgezeichnete Abbildung ist eine Bearbeitung, kein freier Fakt. Das
   ist für die Skizzen-Arbeit die wichtigere der drei Stellen.
3. **Reanimationsalgorithmen der American Heart Association:** gar nicht erst
   im Werk enthalten, „could not be reprinted here for copyright reasons".
   Keine Übernahmefrage, aber nicht wundern, wenn sie fehlen.

### Die eine Regel, die das alles zusammenfasst

**Aus dem SOF Medical Handbook wird ausschließlich TEXT übernommen. Sämtliche
Farbtafeln des Anhangs bleiben außen vor — auch als Vorlage zum
Nachzeichnen.** Eine nachgezeichnete Abbildung ist eine Bearbeitung, kein
freier Fakt.

Der Grund ist nicht nur der Hoffbrand/Pettit-Vorbehalt: Die Tafeln tragen
durchgängig namentliche Fremdattribution. Am 28.07.2026 nachgezählt: **19
Fundstellen „Slide Courtesy of …", insgesamt 25 mit „Courtesy of"** — genannt
werden unter anderem MAJ Dan Schissel, COL Naomi Aronson, LTC Glenn Wortmann,
MAJ Joseph Wilde, CDC/Dr. William A. Clark, die CDC Parasite Image Library
und, bei der Tafel zur Katzenkratzkrankheit, das **Textbook of Military
Medicine** — also ein veröffentlichtes Werk, keine Einzelperson.

So steht es als **eine** Regel da, statt dass jemand später Tafel für Tafel
prüfen muss, ob gerade ein Vorbehalt danebensteht.

**Der Text dagegen ist sauber.** Ebenfalls am 28.07. geprüft: „reprinted"
kommt nur im AHA-Hinweis vor, und die einzige Stelle mit „adapted from"
(Nähtechnik) verweist auf das 18D Skills and Training Manual — ein anderes
US-Militärhandbuch, damit ebenfalls gemeinfrei.

**Arbeitsregel, teuer gelernt am 28.07.2026:** Beim Prüfen eines Werkes auf
Rechtevorbehalte nach **`permission`** UND **`courtesy`** suchen, nicht nach
`copyright`. Der Bilder-Vorbehalt oben nennt das Wort „copyright" nicht und
wurde bei einer Suche danach übersehen; die Tafel-Attributionen nennen weder
„copyright" noch „permission". Beide Male hat erst die zweite, unabhängige
Gegenprüfung sie gefunden.

## Fachgesellschafts-Leitlinien und Fachinformationen: Beleg ja, Weitergabe nein

Ergänzt am 03.08.2026, nachdem in einer Schicht 23 Quellen dazukamen.

Leitlinien (AWMF, DGUV, SLF, DLRG), Fachzeitschriften und Gebrauchs- bzw.
Fachinformationen sind **urheberrechtlich geschützt**. Sie werden in diesem
Projekt genauso behandelt wie das WHO-Kinderhandbuch seit dem 02.08.2026:

- **Als Beleg herangezogen** — Zahlen, Grenzwerte und Handgriffe dort
  nachgeprüft.
- **Der Text im Paket ist eigener Text.** Kein Wortlaut, keine
  kapitelweise Nacherzählung, eigene Gliederung.
- **Kein solches Werk wird mit dem Paket ausgeliefert.** Die Volltexte liegen
  unter `work/quellen/` und sind gitignoriert.
- **Die Quellenangabe im Tipp sagt das ausdrücklich** („Nur als Beleg
  herangezogen, kein Wortlaut übernommen"), damit ein Leser die Grenze sieht.

Das ist dieselbe Linie wie bei den Büchern (siehe nächster Abschnitt) und
verlangt keine eigene Anfrage. **Wollte man eines dieser Werke mitliefern oder
daraus zitieren, wäre eine Anfrage nötig** — das ist derzeit bei keinem der Fall.

Die vollständige Liste mit Lizenzspalte steht in
`content/europe-de/QUELLEN.md`, Abschnitt „Quellen der Schicht vom 03.08.2026".

## Grundsatz für Wissen aus geschützten Büchern

Von Max am 28.07.2026 entschieden: Wissen aus seinen gekauften Büchern wird
genutzt, **in eigenen Worten umformuliert und als Erfahrungswissen
gekennzeichnet**. Fakten sind nicht urheberrechtlich schützbar, nur ihre
konkrete Ausdrucksform.

Zwei Grenzen, die dabei einzuhalten sind:

1. **Auch Auswahl und Anordnung sind geschützt.** Eine kapitelweise
   Nacherzählung wäre eine Bearbeitung, auch mit anderen Worten. Deshalb:
   eigene Gliederung behalten, Fakten je Thema aus mehreren Quellen
   zusammenführen.
2. **Bei Dosierungen, Grenzwerten und allem, was bei einem Zahlendreher
   tötet, zusätzlich eine Primärquelle.** Das ist keine Lizenzfrage, sondern
   Regel 1.

Die Bücher liegen nicht digital vor. Nutzbar ist nur, was Max fotografiert;
die Inhaltsverzeichnisse hat er am 28.07. geschickt — daraus stammt die
Lückenliste in `ROADMAP.md`.
