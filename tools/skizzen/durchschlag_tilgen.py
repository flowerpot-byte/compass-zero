# -*- coding: utf-8 -*-
"""Entfernt Durchschlag von der Rueckseite eines Scans in einem leeren Feld.

Alte Buecher sind duenn bedruckt: Auf der Vorderseite schimmert die Schrift der
Rueckseite durch. Solange das Blatt auf grauem Scan-Papier liegt, faellt das
nicht auf -- hebt man den Grund auf den Blattton (`grund_angleichen.py`), steht
die Geisterschrift ploetzlich sichtbar da.

Getilgt wird NUR in einem Bereich, den der Aufrufer angibt, und NUR wenn dort
keine Zeichnung liegt. Genau das prueft das Werkzeug selbst, bevor es etwas
anfasst: Findet es im Bereich Pixel, die so dunkel sind wie echte Tinte, bricht
es ab. So kann es keine Linie ausloeschen -- der haeufigste und schlimmste
Fehler bei so einer Bereinigung.

Aufruf:
  python tools/skizzen/durchschlag_tilgen.py <blatt.png> <ziel.png> x0,y0,x1,y1 [...]
"""
import collections
import sys

from PIL import Image

# So dunkel ist echte Zeichnung. Wird das im Bereich gefunden: abbrechen.
TINTE_BIS = 150
# Alles darueber gilt im geprueften Bereich als Durchschlag und wird geglaettet.
SCHWELLE = 190


def blattgrund(bild):
    haeufig = collections.Counter(bild.convert("RGB").getdata()).most_common(1)
    return haeufig[0][0]


def pruefen(px, kasten):
    """Liegt im Bereich echte Zeichnung? Dann wird nichts angefasst."""
    dunkel = 0
    for y in range(kasten[1], kasten[3]):
        for x in range(kasten[0], kasten[2]):
            if max(px[x, y][:3]) <= TINTE_BIS:
                dunkel += 1
    return dunkel


def tilgen(px, kasten, grund):
    getilgt = 0
    for y in range(kasten[1], kasten[3]):
        for x in range(kasten[0], kasten[2]):
            if max(px[x, y][:3]) < grund[0]:
                px[x, y] = grund
                getilgt += 1
    return getilgt


def main():
    if len(sys.argv) < 4:
        raise SystemExit(__doc__)
    quelle, ziel = sys.argv[1], sys.argv[2]
    bild = Image.open(quelle).convert("RGB")
    px = bild.load()
    grund = blattgrund(bild)
    print("Blattgrund: %s" % (grund,))

    bereiche = []
    for angabe in sys.argv[3:]:
        teile = tuple(int(t) for t in angabe.split(","))
        if len(teile) != 4:
            raise SystemExit("Bereich braucht vier Zahlen: x0,y0,x1,y1")
        bereiche.append(teile)

    # ERST alle Bereiche pruefen, DANN erst tilgen: Sonst waere ein Blatt bei
    # zwei Bereichen schon halb bearbeitet, wenn der zweite abgelehnt wird.
    for kasten in bereiche:
        dunkel = pruefen(px, kasten)
        if dunkel:
            raise SystemExit(
                "ABBRUCH: Im Bereich %s liegen %d Pixel echter Zeichnung "
                "(dunkler als %d). Bereich enger fassen -- getilgt wird nur in "
                "leeren Feldern." % (kasten, dunkel, TINTE_BIS))
        print("   %s  frei von Zeichnung" % (kasten,))

    for kasten in bereiche:
        n = tilgen(px, kasten, grund)
        print("   %s  %d Pixel geglaettet" % (kasten, n))
    bild.convert("P", palette=Image.ADAPTIVE, colors=32).save(ziel, optimize=True)
    print("geschrieben:", ziel)


main()
