# Bildabsagen -- wo schon gesucht wurde und nichts zu holen war

Ein Blatt zu verwerfen kostet fast so viel Arbeit wie eins zu bauen: Quelle
finden, im Volltext lesen, Rechte in zwei Stufen pruefen, Abbildungen einzeln
ansehen. Diese Liste haelt das Ergebnis fest, **damit dieselbe Suche nicht ein
zweites Mal bezahlt wird.**

Wer einen Eintrag hier findet, faengt nicht bei null an: Entweder es gibt
wirklich nichts, oder es steht dabei, was ein brauchbares Blatt haette.

Die Regeln selbst gehoeren nicht hierher -- die Rechtepruefung in zwei Stufen
(ist das WERK frei? und traegt die EINZELNE Abbildung einen eigenen Nachweis?),
der Umgang mit Signaturen und der mit fremdsprachigem Text im Bild. Hier steht
nur, was ihre Anwendung im Einzelfall ergeben hat.

---

## Abgelehnt, weil die Rechte nicht reichen

**gelaende-schwere-last-zu-zweit** ("Haltung beim Heben selbst") -- sechs
Quellen geprueft, alle raus:
- OSHA-eTool: nur Fotos identifizierbarer Personen.
- NIOSH 2007-131: traegt das NIOSH-Logo, verfasst hat es aber **Cal/OSHA** --
  eine Landesbehoerde, kein Bundeswerk.
- NIOSH 2007-122 "Simple Solutions": im Impressum eine namentlich genannte
  externe Illustratorin, dazu Cal/OSHA-Fotonachweise im selben Heft.
- Susan-Harwood-Foerderdatei auf osha.gov: laut eigenem Vermerk kein Werk der
  Bundesregierung, nur von ihr bezahlt.
- NIOSH 94-110 (echt gemeinfrei): enthaelt nur Flussdiagramme und
  Arbeitsblaetter, keine Koerperhaltung.
- Hochschul-Auszug der 1981er NIOSH-Studie: zu klein aufgeloest, zeigt
  Rechenvariablen statt Hebehaltung.

**agrikultur-tiergesundheit**, Abschnitt Klaue/Moderhinke -- Bulletin 2206 ist
ein Fotoheft aus den 1960/70ern ohne Stiche. Das Gallagher-Bulletin waere
bebildert, ist aber in der Beleglage dieses Kapitels ausdruecklich auf das
Thema Blaehsucht beschraenkt.

**agrikultur-tiergesundheit**, Abschnitt Euter/Mastitis -- die einzige
passende Figur traegt "(adapted from H. C. Wilkie)".

**agrikultur-pilzzucht** -- Figur 19 ("Shelf beds in a cellar") waere
inhaltlich besser gewesen als die verwendete Figur 5, traegt aber ein
handgezeichnetes Monogramm unter der Zeichnung.

## Zurueckgestellt, nicht verworfen

**werkzeug-waagerecht-senkrecht** ("Der rechte Winkel: das Dreieck mit festem
Seitenverhaeltnis") -- Ausschnitt aus USDA Farmers' Bulletin 1480 (1926),
Fig. 1, S. 9. Rechte und Beschriftung in Ordnung, Zoll-Masze sauber
umgerechnet. Trotzdem raus: Der Ausschnitt zeigt zwei dicke Fluchtlinien, drei
lose Buchstaben und die handgeschriebenen Masze der Quelle durcheinander. Wer
das Dreieck 3-4-5 noch nicht kennt, versteht es hier nicht -- und wer es kennt,
braucht kein Bild.
*Brauchbar waere hier ausnahmsweise kein Stich, sondern ein selbst
gezeichnetes Schema* (`tools/skizzen/schema_zeichnen.py`): drei Seiten, drei
Maszahlen, ein rechter Winkel. Reine Geometrie ist nichts, was aus einer
Quelle stammen muesste.



**werkstoffe-gerben-ohne-rinde** ("Die Werkzeuge, und woraus sie sein
koennen") -- Schaber und Blackfoot-Flescher nach Clark Wissler, "Material
Culture of the Blackfoot Indians" (1910), Fig. 32 und 34a. Quelle und Rechte
sind in Ordnung, die Beschriftung stimmt. Das Blatt lag zunaechst auf zwei
grauen Kaesten statt auf dem Blattgrund; das ist mit
`tools/skizzen/grund_angleichen.py` behoben. Dabei kam allerdings zum
Vorschein, was das Grau verdeckt hatte: Im unteren Ausschnitt schimmert die
Rueckseite des Scans durch (Geisterschrift, links oben im leeren Teil).
*Zum Fertigmachen fehlt:* dieses Durchscheinen entfernen -- ueber
zusammenhaengende Flaechen, ohne eine Linie der Zeichnung anzutasten.

## Abgelehnt, weil das Bild nichts zeigt, was der Satz nicht sagt

**agrikultur-zwiebeln** ("Das Zwiebellager") -- Foto gestapelter Lattenkisten,
USDA Farmers' Bulletin 354, Figur 18. Das Blatt war fertig beschriftet und
wurde trotzdem verworfen: Der **Luftspalt** zwischen den Stapeln ist das
einzige, worauf es ankommt, und genau er ist auf dem gerasterten Foto nicht zu
sehen. Untertitel ("gestapelt mit Luftspalt") und Beschriftung ("Kisten dicht
aufeinander gestapelt") widersprachen sich deshalb.
*Brauchbar waere:* ein Schnitt oder eine Zeichnung, auf der der Abstand
zwischen zwei Kisten sichtbar ist.

**agrikultur-tiergesundheit** ("Wuermer") -- Magenwurm-Zeichnung nach USDA
1330, Figur 24. Zwei duenne Striche. Die beiden Beschriftungen unterschieden
Maennchen und Weibchen -- eine Frage, die im Ernstfall niemand stellt.
*Brauchbar waere:* eine Abbildung, die die Wuermer **im geoeffneten Labmagen**
zeigt, also die Wiedererkennung beim Aufbrechen eines toten Tieres.

**werkzeug-leiter-anlegen** ("Der richtige Winkel: die Ein-zu-vier-Regel") --
OSHA FS-3660 (Herstellerfoto mit eigener Beschriftung; die zweite Figur ist
eine moderne Flach-Illustration ohne jede Feinzeichnung, Stilbruch zum Rest
des Pakets). Ein Winkel-Schaubild waere ideal, in beiden Merkblaettern gibt es
keins.

**werkstoffe-filzen-wolle** ("Filzen: aus loser Wolle wird fester Stoff") --
einzige Quelle ist State of New Jersey, Dept. of Labor, "Sanitary Standards
for the Felt Hatting Industry" (1915, archive.org `sanitarystandard00newj`,
81 Blaetter, komplett gesichtet). Fast jede fotografische Tafel traegt einen
Firmenvermerk ("Courtesy of John B. Stetson Co.", "Courtesy of Donner & Co.",
"Courtesy of C. M. Hedden Co.", "Courtesy of E. V. Connett & Co.", "Courtesy
of Hattery Hat Co." usw. -- durchgaengiges Muster, keine Ausnahme gefunden).
Der inhaltlich beste Treffer, Plate I ("Fur Fibres ... Before and After
Carrotting", zeigt die Schuppenstruktur der Faser), traegt selbst den
Firmenvermerk "D. Van Nostrand Co." UND zeigt zusaetzlich das
quecksilberhaltige Carrotting-Verfahren, das der Eintrag ausdruecklich als
"bei Wolle nie noetig" bezeichnet -- waere also auch inhaltlich falsch
gewesen. Die zwei einzigen firmenfreien Tafeln (Plate XX, Plate XXVI) sind
Lueftungshauben-Spezifikationen ohne Bezug zum Filzvorgang.
*Brauchbar waere:* eine Zeichnung oder ein Foto des Anfilzens/Schrumpfens von
Hand (Sprengen, Einwickeln, Rollen) ohne Firmenvermerk -- die Quelle
beschreibt genau das in Worten (Abschnitt "Hand Starting"), zeigt es aber an
keiner Stelle bildlich.

## Portraetfotos wirklicher Menschen: grundsaetzlich nicht

Das Paket besteht aus Stichzeichnungen von Gegenstaenden und Handgriffen.
Fotos, auf denen ein bestimmter Mensch erkennbar und benannt ist, kommen nicht
hinein -- auch dann nicht, wenn das Urheberrecht laengst abgelaufen ist. Zwei
Gruende, und der zweite allein genuegt schon:

1. Ein Portraet zeigt fast nie den Handgriff. Es zeigt einen Menschen, der ihn
   gerade ausfuehrt. Das ist Schmuck, kein Unterricht.
2. Diese Aufnahmen sind unter Verhaeltnissen entstanden, unter denen die
   Abgebildeten nichts zu entscheiden hatten. Ein Ueberlebenshandbuch braucht
   sie nicht, um seine Sache zu erklaeren.

**werkstoffe-schuhe-mokassin** ("Der Mokassin mit Sohle") -- Tafel "Little
Creek Woman Working on a Moccasin" aus George Bird Grinnell, "The Cheyenne
Indians" Bd. 1 (1923). Rechtlich einwandfrei und ohne Signatur, inhaltlich
aber nur zwei Zeiger ("die Hand beim Naehen", "der Mokassin, noch in Arbeit"),
und die Fusszeile musste selbst einraeumen, dass kein Schnittmuster zu sehen
ist. Dieselben Grinnell-Tafeln waren zuvor schon fuer das Gerben verworfen
worden ("Thinning a Hide").
*Brauchbar waere:* eine Zeichnung der Naehfolge oder ein bemasstes
Schnittmuster. Das einzige bekannte stammt von Wissler/Orchard und ist im
Eintragstext bereits wegen ungeklaertem Todesjahr des Zeichners ausgeschlossen.

**Anmerkung fuer spaetere Laeufe:** Ein *Bericht* eines anderen Laufs ist kein
Praezedenzfall. Massgeblich ist, was im Paket gelandet ist. Zum Flachs liegt
dort eine Roestgrube, kein Portraet.

## Abgelehnt wegen fremdsprachigem Text, der Information traegt

**werkzeug-verbogen-richten** ("verschlagener Kopf", Schritt ZURUECKSCHLEIFEN)
-- TM 9-867, Seite 35, Figur "MUSHROOMED HEAD" (RA PD 87252): zeigt genau das
gesuchte Vorher/Nachher (Kopf verschlagen, gestrichelte Linie zeigt die
urspruengliche Form) und waere inhaltlich die treffendste Abbildung gewesen.
Die englischen Bildlegenden ("MUSHROOMED HEAD", "HANDLE", "DOTTED LINE SHOWS
ORIGINAL SHAPE") sitzen als Pfeile MIT Text im selben freien Bildraum wie die
Zeichnung selbst (diagonal angeordnetes Werkzeug, Legenden in der oberen
Bildhaelfte danaben) -- ein rechteckiger Zuschnitt kann sie nicht
wegschneiden, ohne entweder Text stehenzulassen oder die Zeichnung
anzuschneiden. Verwendet wurde stattdessen die sauber freistellbare
Nachbarabbildung "Removing Mushroomed End From Machinists' Chisels"
(RA PD 87256, Seite 37) fuer denselben Schritt.
*Brauchbar waere:* dieselbe Figur, falls irgendwo eine textfreie Fassung
existiert, oder eine gleichwertige Vorher/Nachher-Zeichnung ohne eingebaute
Legenden.

**werkzeug-leiter-anlegen** -- OSHA FS-3661, Figur 1 ("Single-Cleat Ladder"):
untrennbar eingebaute englische Masse ("24 ft.", "16 in. minimum"). Zeigt
ausserdem den Leiter**bau**, nicht das Anlegen.

**abort-anlage** -- Bautafel mit Holzliste: die Masse standen ausschliesslich
auf Englisch im Bild, ohne deutsche Entsprechung daneben. (Zum Vergleich: das
Astschnitt-Blatt durfte bleiben, weil unsere deutsche Beschriftung direkt
daneben dasselbe sagt.)

## Abgelehnt, weil es schlicht keine Abbildung gibt

**werkstoffe-faerben-pflanzen** ("Färben: welche Beize welche Farbe macht")
-- einzige Quelle ist Ethel M. Mairet, "A Book on Vegetable Dyes" (1916,
archive.org `bookonvegetabled00mairrich`, 182 Seiten). Das Buch enthaelt
KEINE einzige Abbildung: Volltextsuche nach "Plate", "Frontispiece", "Fig.",
"woodcut", "illustrated" ergab null Treffer, das Inhaltsverzeichnis fuehrt
kein "List of Illustrations", und die ersten Buchseiten (Titelblattbereich,
als Einzelbilder angesehen) sind reiner Schriftsatz. Gepruefte Kandidaten
Faerbekessel (Schritt Alaun-/Eisenbeize) und Aufhaengen der Straenge --
Letzteres kommt im Text gar nicht vor, die Wolle wird ausgedrueckt und in
einem Leinenbeutel gelagert, nicht aufgehaengt.
*Brauchbar waere:* jede illustrierte Quelle zum Faerbekessel/Beizvorgang;
diese hier scheidet komplett aus, nicht nur eine Einzelfigur.

**agrikultur-gerben-ohne-rinde**, Abschnitt "Räuchern" (das Gestell "wie ein
Schwitzhaus", mit Grube und Schwelfeuer darunter) -- keine der beiden Quellen
(Clark Wissler, "Material Culture of the Blackfoot Indians", 1910; George
Bird Grinnell, "The Cheyenne Indians" Bd. 1, 1923) zeichnet dieses Gestell.
Wissler beschreibt es nur im Fliesstext, ohne Figurnummer ("spread over a
frame similar to that of a sweat house"). Grinnells "frame of the
sweat-house" (S. 209/210) ist ein ANDERES Gestell -- die zeremonielle
Schwitzhuette (Weidenruten, im Medizin-Zelt-Kontext), nicht das Raeuchergestell
fuer Haeute. Geprueft und verworfen: Grinnells Fototafeln "Thinning a Hide"
(nach S. 176) und "Fleshing a Hide" (nach S. 224) zeigen die Arbeit selbst,
aber als Foto einer einzelnen, im Bild nicht genannten Frau -- passt nicht in
ein Paket aus Stichzeichnungen von Gegenstaenden, keine Portraetfotos realer
Personen. *Brauchbar waere:* eine reine Konstruktionszeichnung des
Rauchgestells ohne Person darin.
