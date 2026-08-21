# -*- coding: utf-8 -*-
"""Hebt den Papierton eingesetzter Scan-Ausschnitte auf den Blattgrund.

Ein aus einem Scan geschnittener Stich bringt sein eigenes, meist dunkleres
Papier mit. Auf dem cremefarbenen Blatt sieht das aus wie ein aufgeklebtes
Kaestchen, waehrend alle anderen Blaetter die Zeichnung freistehend zeigen.

Es wird NICHTS wegretuschiert. Innerhalb des angegebenen Bereichs wird jeder
Helligkeitswert um einen Betrag angehoben, der beim Papierton voll wirkt und
zur Tinte hin auf null auslaeuft. Dunkle Linien bleiben also, wo sie sind,
feine Schraffuren behalten ihren Abstand zum Papier.

WARUM DIE BEREICHE VON HAND KOMMEN: Eine erste Fassung suchte sie selbst und
griff dabei auch nach dem Kasten der Fusszeile -- einem Gestaltungselement,
das genau denselben Grauton hat. Ein Werkzeug, das die Gestaltung verstellt,
ist schlimmer als der Fehler, den es beheben soll. Die Bereiche misst man mit
einem Blick auf das Blatt; raten muss hier niemand.

Aufruf:
  python tools/skizzen/grund_angleichen.py <blatt.png> <ziel.png> x0,y0,x1,y1 [...]
"""
import collections
import sys

from PIL import Image

# Unterhalb dieses Werts ist ein Pixel Zeichnung und wird nicht angehoben.
TINTE_BIS = 120


def blattgrund(bild):
    haeufig = collections.Counter(bild.convert("RGB").getdata()).most_common(1)
    return max(haeufig[0][0])


def papierton(px, kasten):
    """Haeufigster heller Wert im Kasten -- das ist sein Papier."""
    werte = collections.Counter()
    for y in range(kasten[1], kasten[3]):
        for x in range(kasten[0], kasten[2]):
            m = max(px[x, y][:3])
            if m >= 200:
                werte[m] += 1
    if not werte:
        raise SystemExit("Kein Papier im Bereich %s gefunden." % (kasten,))
    return werte.most_common(1)[0][0]


def anheben(px, kasten, papier, grund):
    hub = grund - papier
    if hub <= 0:
        return 0
    spanne = float(papier - TINTE_BIS)
    beruehrt = 0
    for y in range(kasten[1], kasten[3]):
        for x in range(kasten[0], kasten[2]):
            r, g, b = px[x, y][:3]
            m = max(r, g, b)
            if m <= TINTE_BIS:
                continue
            anteil = min(1.0, (m - TINTE_BIS) / spanne)
            zu = int(hub * anteil + 0.5)
            px[x, y] = (min(grund, r + zu), min(grund, g + zu), min(grund, b + zu))
            beruehrt += 1
    return beruehrt


def main():
    if len(sys.argv) < 4:
        raise SystemExit(__doc__)
    quelle, ziel = sys.argv[1], sys.argv[2]
    bild = Image.open(quelle).convert("RGB")
    px = bild.load()
    grund = blattgrund(bild)
    print("Blattgrund: %d" % grund)
    for angabe in sys.argv[3:]:
        kasten = tuple(int(t) for t in angabe.split(","))
        if len(kasten) != 4:
            raise SystemExit("Bereich braucht vier Zahlen: x0,y0,x1,y1")
        papier = papierton(px, kasten)
        n = anheben(px, kasten, papier, grund)
        print("   %s  Papier %d -> %d, %d Pixel angehoben"
              % (str(kasten), papier, grund, n))
    bild.convert("P", palette=Image.ADAPTIVE, colors=32).save(ziel, optimize=True)
    print("geschrieben:", ziel)


main()
