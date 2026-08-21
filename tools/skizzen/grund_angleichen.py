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
# Ab hier gilt ein Pixel als Papier und darf den Ton mitbestimmen.
PAPIER_AB = 200


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


def papierfeld(px, kasten, felder=8):
    """Papierton je Rasterzelle -- ein Scan ist an einer Ecke oft dunkler.

    Eine einzige Zahl fuer den ganzen Ausschnitt laesst bei so einem Verlauf
    eine Kante stehen: In der Mitte stimmt der Ton, an den Raendern nicht.
    Deshalb wird der Ton in einem groben Raster gemessen und dazwischen
    weich uebergeblendet.
    """
    links, oben, rechts, unten = kasten
    bw = max(1, (rechts - links) // felder)
    bh = max(1, (unten - oben) // felder)
    feld = []
    for j in range(felder):
        zeile = []
        for i in range(felder):
            x0 = links + i * bw
            y0 = oben + j * bh
            x1 = rechts if i == felder - 1 else x0 + bw
            y1 = unten if j == felder - 1 else y0 + bh
            werte = collections.Counter()
            for y in range(y0, y1, 2):
                for x in range(x0, x1, 2):
                    m = max(px[x, y][:3])
                    if m >= PAPIER_AB:
                        werte[m] += 1
            zeile.append(werte.most_common(1)[0][0] if werte else None)
        feld.append(zeile)
    # Zellen ohne Papier (voll von Zeichnung) vom Nachbarn erben.
    bekannt = [w for z in feld for w in z if w is not None]
    ersatz = sorted(bekannt)[len(bekannt) // 2] if bekannt else None
    if ersatz is None:
        return None, 0, 0
    for j in range(felder):
        for i in range(felder):
            if feld[j][i] is None:
                feld[j][i] = ersatz
    return feld, bw, bh


def ton_an(feld, bw, bh, kasten, x, y, felder=8):
    """Weich ueberblendeter Papierton an einer Stelle."""
    fx = (x - kasten[0]) / float(bw) - 0.5
    fy = (y - kasten[1]) / float(bh) - 0.5
    i0 = max(0, min(felder - 1, int(fx)))
    j0 = max(0, min(felder - 1, int(fy)))
    i1 = min(felder - 1, i0 + 1)
    j1 = min(felder - 1, j0 + 1)
    tx = max(0.0, min(1.0, fx - i0))
    ty = max(0.0, min(1.0, fy - j0))
    oben = feld[j0][i0] * (1 - tx) + feld[j0][i1] * tx
    unten = feld[j1][i0] * (1 - tx) + feld[j1][i1] * tx
    return oben * (1 - ty) + unten * ty


def anheben(px, kasten, papier, grund):
    feld, bw, bh = papierfeld(px, kasten)
    if feld is None:
        return 0
    beruehrt = 0
    for y in range(kasten[1], kasten[3]):
        for x in range(kasten[0], kasten[2]):
            r, g, b = px[x, y][:3]
            m = max(r, g, b)
            if m <= TINTE_BIS:
                continue
            ton = ton_an(feld, bw, bh, kasten, x, y)
            hub = grund - ton
            if hub <= 0:
                continue
            anteil = min(1.0, (m - TINTE_BIS) / max(1.0, ton - TINTE_BIS))
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
