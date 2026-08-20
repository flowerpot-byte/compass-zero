# -*- coding: utf-8 -*-
"""Zieht alle Quellenangaben des Pakets zusammen und ordnet sie ein.

WOZU: Am 11.08.2026 ist aufgefallen, dass 162 Quellenangaben ihre Lizenz mit
"Werk einer US-Bundesbehoerde, gemeinfrei" begruenden -- eine Formel, die
US-Recht nennt und fuer Deutschland nichts begruendet. Die tragfaehige
Begruendung ist der Schutzfristenvergleich; sie gilt aber NUR fuer Werke des
BUNDES, nicht fuer Bundesstaaten und nicht fuer private Werke.

DIE WICHTIGERE UNTERSCHEIDUNG, die am 11.08.2026 zunaechst gefehlt hat und
die das Bild geradezieht -- es sind ZWEI Fragen, nicht eine:

  Frage A: Uebernehmen wir aus dieser Quelle geschuetzten AUSDRUCK?
           Also woertlichen Text, eine enge Uebersetzung, eine Abbildung,
           eine ganze Tabelle. Nur DANN muss die Quelle frei sein.

  Frage B: Oder nehmen wir nur TATSACHEN und schreiben selbst?
           Tatsachen, Zahlen, Masze und Arbeitsverfahren sind nicht
           geschuetzt. Wer sie aus einem geschuetzten Buch lernt und in
           eigenen Worten aufschreibt, braucht keine Lizenz -- nur die
           Quellenangabe, und die verlangt dieses Projekt ohnehin.

Eine maschinelle Gegenprobe am 11.08.2026 hat im ganzen Paket NULL woertlich
uebernommene englische Passagen gefunden. Der Text des Pakets ist eigene
Leistung. Deshalb blockiert der Rechtsstand einer Quelle die Veroeffentlichung
des TEXTES nicht -- auch nicht bei modernen Werken wie WHO-, RKI- oder
AWMF-Papieren oder Fachbuechern.

WO DIE LIZENZ SEHR WOHL ZAEHLT, und darauf zielt dieses Werkzeug:
  * Abbildungen, die wir uebernehmen (eine Zeichnung ist eigener Ausdruck),
  * laengere woertliche Zitate,
  * das Phrasenbuch im Uebersetzer -- dort werden Formulierungen als solche
    uebernommen, das ist der Zweck eines Phrasenbuchs.

Fuer diese Faelle braucht jede Quelle eine Einordnung in genau eine von drei
Klassen:

  bund     Werk einer US-BUNDESbehoerde. Schutzdauer im Ursprungsland null
           (17 U.S.C. 105), damit ueber den Schutzfristenvergleich auch hier
           null.
  anonym   Kein Mensch als Verfasser auf der Titelseite (Behoerde, Firma,
           Verband, Koerperschaft). Erscheinungsjahr + 70.
  person   Benannter Verfasser. Todesjahr + 70, und das Todesjahr muss in der
           Angabe stehen.

Was in keine der drei passt, ist NICHT verwendbar und muss ersetzt oder
zurueckgebaut werden.

Dieses Werkzeug ordnet nur VOR: Es liest die Begruendungstexte, die schon in
den Quellenangaben stehen, und schlaegt eine Klasse vor. Die Entscheidung
bleibt beim Menschen -- deshalb schreibt es eine Liste zum Nachsehen und
aendert nichts.

Aufruf:
    python tools/inhalt/lizenzliste.py            Uebersicht auf dem Schirm
    python tools/inhalt/lizenzliste.py --datei    schreibt LIZENZEN.md
"""
import io
import json
import re
import sys
from collections import defaultdict

PAKET = 'content/europe-de/paket/content/'
DATEIEN = [('tips.json', 'tips'), ('guides.json', 'guides'),
           ('agriculture.json', 'chapters'), ('phrases.json', 'phrases')]

# Behoerden des BUNDES. Der Schutzfristenvergleich traegt nur bei diesen.
BUND = re.compile(
    r'US-Landwirtschaftsministerium|USDA|Farmers\' Bulletin|US-Heer|US Army|'
    r'Field Manual|FM \d|Forest Products Laboratory|Forest Service|'
    r'Bureau of Animal Industry|Bureau of Plant Industry|Bureau of Fisheries|'
    r'Farm Security Administration|Resettlement Administration|'
    r'States Relations Service|Office of Experiment Stations|'
    r'US-Gesundheitsdienst|Public Health|SOCOM|Special Operations|Peace Corps|'
    r'US-Friedenskorps|Bundesregierung|Bundesbeh', re.I)

# Bundesstaaten und Landeseinrichtungen -- ausdruecklich NICHT vom Bund.
LAND = re.compile(
    r'State Board|State Geological|State Department|Department of Labor|'
    r'Agricultural Experiment Station|Cooperative Extension|'
    r'University of|College of Agriculture|Kansas|North Carolina|New Jersey|'
    r'Texas Water|Ohio Geological|Connecticut', re.I)

TODESJAHR = re.compile(r'gestorben (\d{4})|\b(\d{4}) bis (\d{4})\b|†\s*(\d{4})|'
                       r'Todesjahr[^\d]{0,20}(\d{4})|(\d{4})\s*[-–]\s*(\d{4})')
ALTE_FORMEL = re.compile(r'gemeinfrei als Werk|Werk der US-Bundesregierung|'
                         r'Werk einer US-Bundesbeh|nicht urheberrechtlich gesch', re.I)


def ausgeben(text):
    kodierung = getattr(sys.stdout, 'encoding', None) or 'utf-8'
    sys.stdout.write(text.encode(kodierung, 'replace').decode(kodierung, 'replace') + '\n')


def sammeln():
    """Alle Quellenangaben, gebuendelt nach (name, detail)."""
    quellen = defaultdict(list)
    for datei, schluessel in DATEIEN:
        try:
            daten = json.load(io.open(PAKET + datei, encoding='utf-8'))
        except FileNotFoundError:
            continue
        for eintrag in daten.get(schluessel, []):
            for q in eintrag.get('sources', []):
                quellen[(q.get('name', ''), q.get('detail', ''))].append(
                    (datei.replace('.json', ''), eintrag.get('id', '?')))
    return quellen


def einordnen(name, detail):
    """Schlaegt eine Klasse vor und nennt den Grund."""
    text = name + ' ' + detail
    jahr = TODESJAHR.search(text)
    if jahr:
        gefunden = [g for g in jahr.groups() if g]
        return 'person', 'Todesjahr genannt: ' + '/'.join(gefunden)
    if LAND.search(text) and not BUND.search(text):
        return 'PRUEFEN', 'Landes- oder Hochschuleinrichtung — der Schutzfristenvergleich traegt hier NICHT'
    if BUND.search(text):
        return 'bund', 'Bundesbehoerde'
    if re.search(r'[Kk]ein.{0,30}Verfasser|ohne benannten Verfasser|anonym', text):
        return 'anonym', 'ausdruecklich ohne benannten Verfasser'
    return 'PRUEFEN', 'keine Einordnung erkennbar'


def main():
    quellen = sammeln()
    klassen = defaultdict(list)
    alte_formel = 0
    for (name, detail), stellen in sorted(quellen.items()):
        klasse, grund = einordnen(name, detail)
        if ALTE_FORMEL.search(detail):
            alte_formel += 1
        klassen[klasse].append((name, detail, grund, stellen))

    ausgeben('%d verschiedene Quellenangaben an %d Stellen'
             % (len(quellen), sum(len(s) for s in quellen.values())))
    ausgeben('%d davon tragen noch die alte Formel und muessen neu begruendet werden'
             % alte_formel)
    ausgeben('')
    for klasse in ('bund', 'anonym', 'person', 'PRUEFEN'):
        eintraege = klassen.get(klasse, [])
        ausgeben('%-8s %4d' % (klasse, len(eintraege)))
    ausgeben('')
    ausgeben('=== ZU PRUEFEN (jede einzeln, von Hand):')
    for name, detail, grund, stellen in klassen.get('PRUEFEN', []):
        ausgeben('  * %s' % (name[:78] or '(ohne Namen)'))
        ausgeben('      Grund: %s' % grund)
        ausgeben('      steht bei: %s' % ', '.join(
            '%s/%s' % s for s in stellen[:4]) + (' …' if len(stellen) > 4 else ''))

    if '--datei' in sys.argv:
        with io.open('content/europe-de/LIZENZEN.md', 'w', encoding='utf-8', newline='\n') as f:
            f.write('# Lizenz-Einordnung aller Quellen des Pakets\n\n')
            f.write('Erzeugt von `tools/inhalt/lizenzliste.py`. Drei Klassen, und was in '
                    'keine passt, ist nicht verwendbar.\n\n')
            f.write('* **bund** — Werk einer US-Bundesbehörde. In den USA nach 17 U.S.C. § 105 '
                    'nie geschützt; über den Schutzfristenvergleich (§ 121 Abs. 4 UrhG, '
                    'Art. 7 Abs. 8 RBÜ) auch hier nicht.\n')
            f.write('* **anonym** — kein Mensch als Verfasser genannt. Erscheinungsjahr + 70.\n')
            f.write('* **person** — benannter Verfasser. Todesjahr + 70, Jahr steht dabei.\n\n')
            for klasse in ('bund', 'anonym', 'person', 'PRUEFEN'):
                eintraege = klassen.get(klasse, [])
                f.write('\n## %s (%d)\n\n' % (klasse, len(eintraege)))
                for name, detail, grund, stellen in eintraege:
                    f.write('### %s\n\n' % (name or '(ohne Namen)'))
                    f.write('%s\n\n' % detail)
                    f.write('*Einordnung:* %s — *steht bei:* %s\n\n'
                            % (grund, ', '.join('`%s/%s`' % s for s in stellen)))
        ausgeben('')
        ausgeben('geschrieben: content/europe-de/LIZENZEN.md')
    return 0


if __name__ == '__main__':
    sys.exit(main())
