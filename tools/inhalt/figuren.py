# -*- coding: utf-8 -*-
"""Holt Abbildungen aus den Quellheften, aus denen auch der Text stammt.

WARUM: Die Skizzen des Pakets sind von Hand aus SVG zusammengesetzt, und Max
hat am 11.08.2026 gesagt, dass viele davon schlecht gemalt und schlecht zu
erkennen sind. Die Hefte, aus denen der Text kommt, enthalten dagegen echte
Stiche und technische Schnitte -- von Zeichnern gemacht, mit beschrifteten
Einzelteilen, und in einer Qualitaet, die freihaendig nicht zu erreichen ist.

DIE SCHUTZFRIST GILT FUER DIE ZEICHNUNG EIGENS. Eine Abbildung ist ein
eigenes Werk mit eigenem Urheber. In Farmers' Bulletin 463 steht unter den
Zeichnungen klein "L. H. WILDER" -- fuer diese Bilder zaehlt SEIN Todesjahr,
nicht das der Textverfasser. Deshalb meldet dieses Werkzeug jede Seite mit
dem Hinweis, dass die Signatur zu pruefen ist, und trifft die Entscheidung
NICHT selbst. Regel im Projekt:

  * Figur ohne Signatur  -> uebernehmbar, wenn der Text der Quelle frei ist.
  * Figur mit Signatur   -> nur nachzeichnen, nicht uebernehmen, solange das
                            Todesjahr des Zeichners nicht belegt ist.

Aufrufe:
    python tools/inhalt/figuren.py holen <archive-kennung> [<dateiname.pdf>]
        Laedt die PDF der Quelle nach work/quellen/figuren/<kennung>/.

    python tools/inhalt/figuren.py seiten <kennung> <von> <bis> [--lupe N]
        Rendert die Seiten als PNG. --lupe setzt die Vergroesserung
        (Standard 3; 5 fuer feine Schnitte).

    python tools/inhalt/figuren.py suchen <kennung> <wort> [...]
        Sagt, auf welchen Seiten die Woerter stehen -- so findet man die Seite
        zu "Fig. 5" oder "cross section", ohne alle Seiten zu rendern.
"""
import io
import json
import os
import sys
import time
import urllib.request

WURZEL = 'work/quellen/figuren'


def ausgeben(text):
    kodierung = getattr(sys.stdout, 'encoding', None) or 'utf-8'
    sys.stdout.write(text.encode(kodierung, 'replace').decode(kodierung, 'replace') + '\n')


def hole(url, versuche=3, timeout=240):
    for i in range(versuche):
        try:
            return urllib.request.urlopen(url, timeout=timeout).read()
        except Exception as fehler:
            if i == versuche - 1:
                raise
            ausgeben('  (Versuch %d: %s)' % (i + 1, fehler))
            time.sleep(4)


def ordner(kennung):
    p = os.path.join(WURZEL, kennung)
    if not os.path.isdir(p):
        os.makedirs(p)
    return p


def pdfpfad(kennung):
    p = ordner(kennung)
    for name in os.listdir(p):
        if name.endswith('.pdf'):
            return os.path.join(p, name)
    return None


def holen(kennung, dateiname=None):
    daten = json.loads(hole('https://archive.org/metadata/' + kennung).decode('utf-8', 'replace'))
    pdfs = [f['name'] for f in daten.get('files', []) if f['name'].endswith('.pdf')]
    if dateiname:
        pdfs = [n for n in pdfs if n == dateiname]
    if not pdfs:
        ausgeben('keine PDF bei %s -- vorhanden: %s'
                 % (kennung, ', '.join(f['name'] for f in daten.get('files', []))[:300]))
        return 1
    name = pdfs[0]
    roh = hole('https://archive.org/download/' + kennung + '/' + name)
    if roh[:5] != b'%PDF-':
        # Genau das ist am 11.08.2026 bei einem WHO-Dokument passiert: Die
        # angebliche PDF war eine 755 Byte grosse Fehlerseite in HTML.
        ausgeben('FEHLER: die Datei faengt nicht mit %%PDF an, sondern mit %r' % roh[:20])
        return 1
    ziel = os.path.join(ordner(kennung), name)
    open(ziel, 'wb').write(roh)
    ausgeben('%s  (%d KiB)' % (ziel, len(roh) // 1024))
    return 0


def seiten(kennung, von, bis, lupe=3):
    import pypdfium2 as pdfium
    pfad = pdfpfad(kennung)
    if not pfad:
        ausgeben('keine PDF da -- erst "holen" aufrufen')
        return 1
    dok = pdfium.PdfDocument(pfad)
    ausgeben('%s: %d Seiten' % (os.path.basename(pfad), len(dok)))
    for nr in range(von, min(bis, len(dok)) + 1):
        bild = dok[nr - 1].render(scale=lupe).to_pil()
        ziel = os.path.join(ordner(kennung), 'seite_%03d.png' % nr)
        bild.save(ziel)
        ausgeben('  %s  %dx%d' % (ziel, bild.size[0], bild.size[1]))
    ausgeben('ANSEHEN NICHT VERGESSEN, und zwar auf zweierlei:')
    ausgeben('  1. Ist die Figur ueberhaupt lesbar (die Vorlagen sind Scans).')
    ausgeben('  2. Steht klein unter der Zeichnung ein Zeichnername? Dann NICHT')
    ausgeben('     uebernehmen, sondern nachzeichnen.')
    return 0


def suchen(kennung, woerter):
    import pypdfium2 as pdfium
    pfad = pdfpfad(kennung)
    if not pfad:
        ausgeben('keine PDF da -- erst "holen" aufrufen')
        return 1
    dok = pdfium.PdfDocument(pfad)
    for nr in range(len(dok)):
        text = dok[nr].get_textpage().get_text_range()
        klein = text.lower()
        treffer = [w for w in woerter if w.lower() in klein]
        if treffer:
            ausgeben('Seite %3d: %s' % (nr + 1, ', '.join(treffer)))
    return 0


def main():
    if len(sys.argv) < 3:
        ausgeben(__doc__)
        return 2
    befehl, kennung = sys.argv[1], sys.argv[2]
    rest = sys.argv[3:]
    if befehl == 'holen':
        return holen(kennung, rest[0] if rest else None)
    if befehl == 'seiten':
        lupe = 3
        if '--lupe' in rest:
            i = rest.index('--lupe')
            lupe = int(rest[i + 1])
            rest = rest[:i] + rest[i + 2:]
        return seiten(kennung, int(rest[0]), int(rest[1]), lupe)
    if befehl == 'suchen':
        return suchen(kennung, rest)
    ausgeben('unbekannter Befehl: ' + befehl)
    return 2


if __name__ == '__main__':
    sys.exit(main())
