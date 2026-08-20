# -*- coding: utf-8 -*-
"""Baut einen fertigen Eintrag aus einer JSON-Datei ins Paket ein.

WARUM ES DIESES WERKZEUG GIBT: Am 11.08.2026 sind acht Eintraege einzeln mit
handgeschriebenen Python-Skripten eingebaut worden. Sechs davon sind beim
ersten Versuch gescheitert, und JEDES MAL an derselben Sorte Fehler:

  * ein gerades Anfuehrungszeichen als deutsches Schlusszeichen beendete
    mitten im Satz die Python-Zeichenkette,
  * ein nachlaufendes Komma hinter einer Klammer machte aus einem
    Abschnittstext ein Tupel, und in der Datei stand danach eine LISTE, wo
    eine Zeichenkette hingehoert,
  * ein Suchen-und-Ersetzen ueber die Anfuehrungszeichen ersetzte in fuenf
    Zitaten den eingeschlossenen Text durch ein Steuerzeichen,
  * ein ganzer Entwurf war versehentlich in ASCII-Umschrift geschrieben
    ("Pruefung" statt "Pruefung").

Keiner dieser Fehler hat mit dem INHALT zu tun. Sie kosten trotzdem jedes Mal
einen vollen Durchlauf durch die Testsuite. Deshalb wird der Inhalt jetzt als
JSON geschrieben -- dort gibt es keine Zeichenketten-Syntax, die zerbrechen
kann -- und dieses Werkzeug prueft ihn, bevor irgendetwas angefasst wird.

Aufruf:
    python tools/inhalt/einbauen.py <entwurf.json>
    python tools/inhalt/einbauen.py <entwurf.json> --pruefen-ohne-schreiben

Die Entwurfsdatei enthaelt genau ein Objekt:
    {"art": "tip"|"guide"|"chapter"|"gruppe", ...}
Bei "tip", "guide" und "chapter" stehen daneben die Felder des Eintrags, so wie
sie in tips.json, guides.json bzw. agriculture.json stehen sollen.
Bei "gruppe" zusaetzlich {"datei": "tips"|"guides"|"agriculture", "id":..., "title":...}.

NACHTRAG 12.08.2026: "tip" kam spaeter dazu. Bis dahin konnte das Werkzeug
ausgerechnet die groesste der drei Dateien nicht -- 351 der 431 Eintraege sind
Tipps. Was fehlte, war nicht Bequemlichkeit: Ein von Hand eingesetzter
Quellennachweis war 537 Zeichen lang (erlaubt sind 500) und ist erst im
Gradle-Lauf aufgefallen. Die Pruefungen hier gelten jetzt fuer alle drei Arten.
"""
import io
import json
import re
import sys
import unicodedata

PAKET = 'content/europe-de/paket/content/'
ZAEHLTEST = 'core/content/src/jvmTest/kotlin/org/compasszero/content/EuropeDePaketTest.kt'

# Aus ContentLimits.kt. Wer sie dort aendert, muss sie hier nachziehen -- die
# Pruefung im Paket bleibt die massgebliche, dies ist nur die fruehe Warnung.
GRENZEN = {
    'amount': 40,     # MAX_CATEGORY_LENGTH -- daran haengt die Mengenangabe
    'body': 64000,    # MAX_BODY_LENGTH -- der Fliesstext eines Tipps
    'keyword': 40,    # MAX_KEYWORD_LENGTH
    'name': 120,      # MAX_NAME_LENGTH
    'note': 500,      # MAX_NOTE_LENGTH, gilt auch fuer detail und warning
    'summary': 1000,  # MAX_SUMMARY_LENGTH
    'step': 4000,     # MAX_STEP_LENGTH
    'title': 200,     # MAX_TITLE_LENGTH
    'tool': 80,       # MAX_TOOL_LENGTH
}

# Woerter, die es im Deutschen nur mit Umlaut gibt. Steht eines davon in der
# ASCII-Schreibweise im Text, ist der Entwurf in Umschrift geschrieben.
UMSCHRIFT = re.compile(
    r'\b(?:pruef\w*|poekel\w*|raeucher\w*|doerr\w*|toet\w*|betaeub\w*|brueh\w*|'
    r'kuehl\w*|fuer|ueber\w*|waehrend|laesst|haelt|heiss\w*|fuesse|groesse|'
    r'spaeter|zurueck|moeglich|naechst\w*|waerme|kaelte|hoehe|grosse)\b', re.I)


def ausgeben(text):
    kodierung = getattr(sys.stdout, 'encoding', None) or 'utf-8'
    sys.stdout.write(text.encode(kodierung, 'replace').decode(kodierung, 'replace') + '\n')


def lade(name):
    return json.load(io.open(PAKET + name + '.json', encoding='utf-8'))


def sichere(name, daten):
    io.open(PAKET + name + '.json', 'w', encoding='utf-8', newline='\n').write(
        json.dumps(daten, ensure_ascii=False, indent=2) + '\n')


def texte(eintrag):
    """Alle Zeichenketten des Eintrags mit ihrem Fundort."""
    ergebnis = []

    def geh(wert, pfad):
        if isinstance(wert, str):
            ergebnis.append((pfad, wert))
        elif isinstance(wert, dict):
            for k, v in wert.items():
                geh(v, pfad + '.' + k)
        elif isinstance(wert, list):
            for i, v in enumerate(wert):
                geh(v, pfad + '[' + str(i) + ']')
    geh(eintrag, '')
    return ergebnis


# Kennungen, Gruppennamen und Dateinamen sind mit Absicht in ASCII -- das ganze
# Paket haelt es so ("nahrung-doerre-bauen", "doerrfleisch-streifenmasse.png").
# Ohne diese Ausnahme meldet die Umschrift-Pruefung genau diese Felder, und zwar
# jedes Mal zu Recht aussehend und trotzdem falsch.
ASCII_FELDER = re.compile(r'\.(id|image|category|group|situations)(\[\d+\])?$')


def pruefe(eintrag, art):
    fehler = []

    for pfad, wert in texte(eintrag):
        for zeichen in wert:
            if ord(zeichen) < 32 and zeichen != '\n':
                fehler.append('%s enthaelt das Steuerzeichen %r' % (pfad, zeichen))
                break
        # GROSSGESCHRIEBENE Woerter sind ausgenommen: In Versalien wird das ss
        # fuer ein scharfes s korrekt geschrieben ("HEISSEN"), und das Handbuch
        # setzt viel in Versalien. Ohne diese Ausnahme meldete das Werkzeug am
        # 11.08.2026 einen richtigen Text als Umschrift.
        treffer = sorted({t for t in UMSCHRIFT.findall(wert) if not t.isupper()})
        if treffer and not ASCII_FELDER.search(pfad):
            fehler.append('%s ist in Umschrift geschrieben: %s' % (pfad, ', '.join(treffer)))
        # Buchstaben mit Diakritika, die es im Deutschen nicht gibt -- dieselbe
        # Pruefung wie im Paket, nur frueher.
        fremd = [c for c in wert if c in 'ÀÁÂÃÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕØÙÚÛÝÞàáâãåæçèéêëìíîïðñòóôõøùúûýþ']
        if fremd:
            fehler.append('%s enthaelt %s -- im Deutschen gibt es diesen Buchstaben nicht'
                          % (pfad, ' '.join(repr(c) for c in sorted(set(fremd)))))

    def zu_lang(pfad, wert, grenze):
        if len(wert) > grenze:
            fehler.append('%s ist %d Zeichen lang, erlaubt sind %d' % (pfad, len(wert), grenze))

    zu_lang('title', eintrag.get('title', ''), GRENZEN['title'])
    for i, q in enumerate(eintrag.get('sources', [])):
        zu_lang('sources[%d].name' % i, q.get('name', ''), GRENZEN['name'])
        zu_lang('sources[%d].detail' % i, q.get('detail', ''), GRENZEN['note'])

    if art == 'guide':
        zu_lang('summary', eintrag.get('summary', ''), GRENZEN['summary'])
        for i, s in enumerate(eintrag.get('steps', [])):
            if not isinstance(s.get('text'), str):
                fehler.append('steps[%d].text ist kein Text, sondern %s'
                              % (i, type(s.get('text')).__name__))
                continue
            zu_lang('steps[%d].text' % i, s['text'], GRENZEN['step'])
            if 'warning' in s:
                zu_lang('steps[%d].warning' % i, s['warning'], GRENZEN['note'])
        for i, m in enumerate(eintrag.get('materials', [])):
            # Die Mengenangabe haengt an MAX_CATEGORY_LENGTH und ist damit
            # viel kuerzer, als sie aussieht: 40 Zeichen. Am 12.08.2026 hat
            # genau das eine ganze Testsuite rot gemacht ("rund 3 Kubikmeter
            # je Tagewerk von drei Leuten", 45 Zeichen) -- und zwar erst im
            # Gradle-Lauf, weil hier nur die Notiz geprueft wurde.
            zu_lang('materials[%d].item' % i, m.get('item', ''), GRENZEN['name'])
            zu_lang('materials[%d].amount' % i, m.get('amount', ''), GRENZEN['amount'])
            zu_lang('materials[%d].note' % i, m.get('note', ''), GRENZEN['note'])
        for i, w in enumerate(eintrag.get('tools', [])):
            zu_lang('tools[%d]' % i, w, GRENZEN['tool'])
    elif art == 'tip':
        zu_lang('body', eintrag.get('body', ''), GRENZEN['body'])
        for i, s in enumerate(eintrag.get('keywords', [])):
            zu_lang('keywords[%d]' % i, s, GRENZEN['keyword'])
        if len(eintrag.get('keywords', [])) > 20:
            fehler.append('mehr als 20 Stichwoerter')
        lagen = eintrag.get('situations', [])
        if not lagen:
            fehler.append('keine situations -- der Tipp taucht dann in keiner Lage auf')
        elif len(lagen) > 6:
            fehler.append('mehr als 6 situations')
        if not isinstance(eintrag.get('body'), str):
            fehler.append('body ist kein Text, sondern %s'
                          % type(eintrag.get('body')).__name__)
    elif art == 'chapter':
        for i, a in enumerate(eintrag.get('sections', [])):
            if not isinstance(a.get('body'), str):
                fehler.append('sections[%d].body ist kein Text, sondern %s -- fast immer ein '
                              'nachlaufendes Komma hinter der Klammer'
                              % (i, type(a.get('body')).__name__))
            if not isinstance(a.get('heading'), str):
                fehler.append('sections[%d].heading ist kein Text' % i)

    # Kennungsfelder muessen ASCII bleiben. Die Umschrift-Pruefung oben nimmt
    # sie aus (sonst meldete sie "doerre" in jeder zweiten Kennung) -- damit
    # war aber gar nichts mehr da, was einen Umlaut IN einer Kennung bemerkt
    # haette. Am 20.08.2026 hat ein Hilfswerkzeug aus "agrikultur-poekelmengen"
    # eines mit Umlaut gemacht; gefunden hat es erst der Kotlin-Waechter, also
    # nach dem Einbau.
    for pfad, wert in texte(eintrag):
        if not ASCII_FELDER.search(pfad):
            continue
        fremd = sorted({c for c in wert if ord(c) > 127})
        if fremd:
            fehler.append('%s ist ein Kennungsfeld und muss ASCII sein, '
                          'enthaelt aber %s'
                          % (pfad, ' '.join(repr(c) for c in fremd)))

    # Pflichtfelder. Am 20.08.2026 ging eine Anleitung OHNE "category" und
    # "difficulty" durch diese Pruefung und liess danach das ganze Paket nicht
    # mehr laden ("Fields [category, difficulty] are required"). Der Entwurf
    # hatte sie schlicht nicht gesetzt, und hier fiel es nicht auf, weil nur
    # Laengen geprueft wurden.
    #
    # Statt eine Liste zu pflegen, die beim naechsten neuen Feld veraltet:
    # Pflicht ist, was JEDER vorhandene Eintrag derselben Art hat.
    bestand = {'tip': ('tips', 'tips'), 'guide': ('guides', 'guides'),
               'chapter': ('agriculture', 'chapters')}.get(art)
    if bestand:
        try:
            vorhandene = lade(bestand[0])[bestand[1]]
        except Exception:
            vorhandene = []
        if vorhandene:
            pflicht = set(vorhandene[0])
            for x in vorhandene[1:]:
                pflicht &= set(x)
            fehlt = sorted(pflicht - set(eintrag))
            if fehlt:
                fehler.append('Pflichtfelder fehlen: %s -- die haben ALLE %d '
                              'vorhandenen Eintraege dieser Art'
                              % (', '.join(fehlt), len(vorhandene)))

    # Die Dringlichkeitsfelder sind eine geschlossene Liste in Checks.kt. Ein
    # erfundener Wert laesst das GANZE Paket nicht mehr laden -- am 12.08.2026
    # hat "daheim" in einem Kapitel genau das ausgeloest. Statt die Liste hier
    # zu doppeln (und beim naechsten neuen Wert zu vergessen), wird gegen die
    # Werte geprueft, die im Paket schon vorkommen.
    erlaubt = set()
    for datei in ('tips', 'guides', 'agriculture'):
        try:
            daten = lade(datei)
        except Exception:
            continue
        for x in daten.get('tips') or daten.get('guides') or daten.get('chapters') or []:
            erlaubt.update(x.get('situations', []))
    unbekannt = [w for w in eintrag.get('situations', []) if w not in erlaubt]
    if erlaubt and unbekannt:
        fehler.append('unbekannte situations: %s -- vorhanden sind: %s'
                      % (', '.join(unbekannt), ', '.join(sorted(erlaubt))))
    if not eintrag.get('situations'):
        fehler.append('keine situations -- der Eintrag taucht dann in keiner Lage auf')

    if not eintrag.get('sources'):
        fehler.append('kein Quellennachweis -- ein Eintrag ohne Quelle kommt nicht ins Paket')
    return fehler


def zaehler_hochsetzen(art, neu, titel, kennung, datum):
    quelle = io.open(ZAEHLTEST, encoding='utf-8').read()
    feld = {'tip': 'pack.tips.size', 'guide': 'pack.guides.size'}.get(
        art, 'pack.agriculture.size')
    muster = re.compile(r'( *)assertEquals\((\d+), ' + re.escape(feld) + r'\)')
    treffer = muster.search(quelle)
    if not treffer:
        return 'Zaehlschranke fuer %s nicht gefunden -- von Hand nachziehen' % feld
    einzug, alt = treffer.group(1), int(treffer.group(2))
    if alt + 1 != neu:
        return ('Zaehlschranke steht auf %d, das Paket haette jetzt %d -- nicht angefasst'
                % (alt, neu))
    # Der Kommentar sagt, WELCHER Eintrag die Zahl hochgesetzt hat. Ohne ihn ist
    # eine hochgezaehlte Schranke nicht von einer durchgewinkten unterscheidbar.
    kommentar = ('%s// %d -> %d am %s: "%s"\n%s// (%s).\n'
                 % (einzug, alt, neu, datum, ohne_umlaute(titel), einzug, kennung))
    quelle = quelle.replace(treffer.group(0), kommentar + treffer.group(0).replace(
        str(alt), str(neu), 1), 1)
    io.open(ZAEHLTEST, 'w', encoding='utf-8', newline='\n').write(quelle)
    return 'Zaehlschranke %d -> %d' % (alt, neu)


def ohne_umlaute(text):
    """Fuer den Kommentar im Test -- die Quelldatei bleibt in ASCII."""
    ersatz = {'ä': 'ae', 'ö': 'oe', 'ü': 'ue', 'Ä': 'Ae', 'Ö': 'Oe', 'Ü': 'Ue', 'ß': 'ss'}
    for a, b in ersatz.items():
        text = text.replace(a, b)
    return ''.join(c for c in unicodedata.normalize('NFKD', text) if ord(c) < 128)


def main():
    if len(sys.argv) < 2:
        ausgeben(__doc__)
        return 2
    entwurf = json.load(io.open(sys.argv[1], encoding='utf-8'))
    nur_pruefen = '--pruefen-ohne-schreiben' in sys.argv
    art = entwurf.pop('art')
    datum = entwurf.pop('datum', '11.08.2026')

    if art == 'gruppe':
        datei = entwurf.pop('datei')
        daten = lade(datei)
        if any(g['id'] == entwurf['id'] for g in daten['groups']):
            ausgeben('Gruppe %s gibt es schon' % entwurf['id'])
            return 1
        # Eine Tipp-Gruppe braucht zwingend eine Kategorie, eine Anleitungs- oder
        # Kapitelgruppe nicht. Am 12.08.2026 ist eine Gruppe ohne dieses Feld ins
        # Paket gelangt: Danach LUD DAS GANZE PAKET NICHT MEHR, und 40 Tests
        # schlugen mit derselben Meldung fehl. Der Fehler war einzeilig, die
        # Suche danach nicht. Deshalb wird die Kategorie hier gegen die
        # vorhandenen Gruppen derselben Datei geprueft.
        vorhanden = {g.get('category') for g in daten['groups'] if g.get('category')}
        if vorhanden and not entwurf.get('category'):
            ausgeben('FEHLER: Gruppen in %s.json brauchen eine "category". '
                     'Vorhanden: %s' % (datei, ', '.join(sorted(vorhanden))))
            return 1
        if entwurf.get('category') and entwurf['category'] not in vorhanden:
            ausgeben('FEHLER: Kategorie "%s" gibt es in %s.json nicht. Vorhanden: %s'
                     % (entwurf['category'], datei, ', '.join(sorted(vorhanden))))
            return 1
        daten.setdefault('groups', []).append(entwurf)
        if not nur_pruefen:
            sichere(datei, daten)
        ausgeben('Gruppe "%s" in %s.json, jetzt %d Gruppen'
                 % (entwurf['title'], datei, len(daten['groups'])))
        return 0

    datei = {'tip': 'tips', 'guide': 'guides'}.get(art, 'agriculture')
    schluessel = {'tip': 'tips', 'guide': 'guides'}.get(art, 'chapters')
    daten = lade(datei)

    if any(x['id'] == entwurf['id'] for x in daten[schluessel]):
        ausgeben('FEHLER: %s gibt es schon' % entwurf['id'])
        return 1

    gruppen = {g['id'] for g in daten.get('groups', [])}
    zugehoerig = entwurf.get('category') if art == 'guide' else entwurf.get('group', '')
    if gruppen and zugehoerig and zugehoerig not in gruppen:
        ausgeben('FEHLER: Gruppe "%s" gibt es nicht. Vorhanden: %s'
                 % (zugehoerig, ', '.join(sorted(gruppen))))
        return 1

    fehler = pruefe(entwurf, art)
    if fehler:
        ausgeben('NICHT EINGEBAUT -- %d Beanstandung(en):' % len(fehler))
        for f in fehler:
            ausgeben('  * ' + f)
        return 1

    daten[schluessel].append(entwurf)
    if nur_pruefen:
        ausgeben('Pruefung bestanden (nichts geschrieben). %s waere Nummer %d.'
                 % (entwurf['id'], len(daten[schluessel])))
        return 0

    sichere(datei, daten)
    anzahl = len(daten[schluessel])
    ausgeben('%s eingebaut: %s' % (art, entwurf['id']))
    ausgeben('  %s.json enthaelt jetzt %d Eintraege' % (datei, anzahl))
    ausgeben('  ' + zaehler_hochsetzen(art, anzahl, entwurf['title'], entwurf['id'], datum))
    ausgeben('  Naechste Schritte: Tests, Suchprobe, QUELLEN.md, Commit.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
