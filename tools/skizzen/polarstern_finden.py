# -*- coding: utf-8 -*-
"""Erzeugt design/skizzen/polarstern-finden.svg -- Endfassung.

Vier Anlaeufe hat es gebraucht:
  1. alles in einem Sternfeld -- vier Beschriftungen uebereinander
  2. vier Tafeln, aber die Sterne einzeln geraten: der Wagen sah aus wie ein
     Splitter, das W wie ein Zickzack
  3. Sternbilder in eigenen Koordinaten gebaut und als Ganzes gedreht -- Formen
     stimmten, Beschriftungen lagen auf den Linien
  4. das gezeichnete W war 0,68 x HB hoch, die Strecke aber 5 x HB: Das Bild
     behauptete ein Verhaeltnis, das es selbst nicht einhielt

Jetzt: Hoehe des W ist exakt HB, Strecke exakt 5 x HB, Abstand der Zeigersterne
exakt DA, Strecke exakt 5 x DA. Beide Zahlen stehen so im Tipp.
"""
import math, io, os, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

W, H = 900, 946
out = []


def roh(s):
    out.append("  " + s + "\n")


def txt(x, y, s, groesse=21, farbe="#111", anker="start"):
    roh(f'<text x="{x:.0f}" y="{y:.0f}" font-family="Georgia, serif" '
        f'font-size="{groesse}" fill="{farbe}" text-anchor="{anker}">{s}</text>')


def stern(x, y, r=6, farbe="#111"):
    roh(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{r}" fill="{farbe}"/>')


def ring(x, y, r, farbe, breite):
    roh(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{r}" fill="none" '
        f'stroke="{farbe}" stroke-width="{breite}"/>')


def kreis_gestrichelt(x, y, r):
    roh(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{r}" fill="none" '
        f'stroke="#c9c4b4" stroke-width="3" stroke-dasharray="7 7"/>')


def linie(a, b, farbe="#111", breite=3, strich=None):
    d = f' stroke-dasharray="{strich}"' if strich else ""
    roh(f'<line x1="{a[0]:.0f}" y1="{a[1]:.0f}" x2="{b[0]:.0f}" '
        f'y2="{b[1]:.0f}" stroke="{farbe}" stroke-width="{breite}"{d}/>')


def pfad(d, farbe, breite):
    roh(f'<path d="{d}" fill="none" stroke="{farbe}" stroke-width="{breite}"/>')


def zug(punkte, farbe="#c9c4b4", breite=3, zu=False):
    art = "polygon" if zu else "polyline"
    roh(f'<{art} points="' + " ".join(f"{x:.0f},{y:.0f}" for x, y in punkte) +
        f'" fill="none" stroke="{farbe}" stroke-width="{breite}"/>')


def dreh(v, phi):
    c, s = math.cos(phi), math.sin(phi)
    return (v[0] * c - v[1] * s, v[0] * s + v[1] * c)


def einpassen(punkte, rechteck):
    xs = [x for x, _ in punkte]
    ys = [y for _, y in punkte]
    bw, bh = max(xs) - min(xs), max(ys) - min(ys)
    x0, y0, x1, y1 = rechteck
    if bw > x1 - x0 or bh > y1 - y0:
        print(f"  WARNUNG: Figur {bw:.0f}x{bh:.0f} passt nicht in "
              f"{x1 - x0:.0f}x{y1 - y0:.0f}")
    dx = x0 + (x1 - x0 - bw) / 2 - min(xs)
    dy = y0 + (y1 - y0 - bh) / 2 - min(ys)
    return [(x + dx, y + dy) for x, y in punkte]


def mass(a, b, quer, abstand, farbe):
    """Masszeichen laengs a-b, seitlich um 'abstand' versetzt."""
    v = (quer[0] * abstand, quer[1] * abstand)
    linie((a[0] + v[0], a[1] + v[1]), (b[0] + v[0], b[1] + v[1]), farbe, 3)
    for punkt in (a, b):
        linie((punkt[0] + v[0] * 0.55, punkt[1] + v[1] * 0.55),
              (punkt[0] + v[0] * 1.45, punkt[1] + v[1] * 1.45), farbe, 3)
    return ((a[0] + b[0]) / 2 + v[0], (a[1] + b[1]) / 2 + v[1])


out.append(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" '
           f'viewBox="0 0 {W} {H}">\n')
out.append("""  <!-- Zum Tipp "Nachts nach dem Polarstern gehen".
       Der Text muss sagen: "Linie vom unteren zum oberen Zeigerstern, um das
       FUENFFACHE ihres Abstands verlaengern" und "Winkel der flachen Seite des
       W halbieren, fuenffache Hoehe des W". Solche Saetze liest man dreimal
       und sieht sie trotzdem nicht. Im Bild ist es eine Linie mit fuenf
       Teilstrichen, die man nachzaehlen kann.
       Jedes Sternbild ist in eigenen Koordinaten mit festen Proportionen
       gebaut und danach als Ganzes gedreht und ins Feld eingepasst. Dadurch
       steckt die Fuenffach-Regel in der Geometrie und kann nicht verrutschen:
       Abstand der Zeigersterne = DA, Strecke = 5 x DA; Hoehe des W = HB,
       Strecke = 5 x HB.
       Vier Fassungen hat es gebraucht. In der vierten war das gezeichnete W
       nur 0,68 x HB hoch — das Bild behauptete also ein Verhaeltnis, das es
       selbst nicht einhielt. pruefen.py meldete dabei jedes Mal "ok" oder nur
       einen Randhinweis; das Skript prueft Struktur, nicht Verstaendlichkeit.
       Gesehen hat jeden dieser Fehler erst das gerenderte Bild. -->\n""")
roh(f'<rect width="{W}" height="{H}" fill="#faf9f6"/>')
txt(40, 46, "Den Polarstern finden: zwei Wege, ein Ziel", 28)

# ================= Tafel A: Grosser Wagen =================
txt(40, 100, "\u00dcber den Gro\u00dfen Wagen", 23, "#1d6b3a")

DA = 40.0
lokal_a = [(0.0, 0.0),                      # Merak   (Zeigerstern, aussen)
           (0.0, -DA),                      # Dubhe   (Zeigerstern)
           (1.30 * DA, -0.86 * DA),         # Megrez
           (1.36 * DA, 0.22 * DA),          # Phecda
           (2.42 * DA, -0.74 * DA),         # Alioth
           (3.44 * DA, -0.40 * DA),         # Mizar
           (4.40 * DA, 0.26 * DA),          # Alkaid
           (0.0, -6 * DA)]                  # Polarstern: 5 x DA ueber Dubhe
merak, dubhe, megrez, phecda, alioth, mizar, alkaid, pol_a = einpassen(
    [dreh(v, math.radians(-32)) for v in lokal_a], (74, 132, 438, 368))

u = ((pol_a[0] - dubhe[0]) / (5 * DA), (pol_a[1] - dubhe[1]) / (5 * DA))
p = (-u[1], u[0])

linie(merak, pol_a, "#d65a1a", 3, "9 7")
for i in range(6):
    s = (dubhe[0] + u[0] * i * DA, dubhe[1] + u[1] * i * DA)
    linie((s[0] - p[0] * 10, s[1] - p[1] * 10), (s[0] + p[0] * 10, s[1] + p[1] * 10),
          "#d65a1a", 3)
mitte_k = mass(merak, dubhe, p, -26, "#d65a1a")

zug([dubhe, merak, phecda, megrez], zu=True)
zug([megrez, alioth, mizar, alkaid])
for s in (phecda, megrez, alioth, mizar, alkaid):
    stern(*s)
for s in (dubhe, merak):
    stern(s[0], s[1], 9, "#d65a1a")
stern(pol_a[0], pol_a[1], 8, "#a3231a")
ring(pol_a[0], pol_a[1], 17, "#a3231a", 4)
txt(pol_a[0] + 28, pol_a[1] + 8, "Polarstern", 23, "#a3231a")

mitte_a = (dubhe[0] + u[0] * 2.5 * DA, dubhe[1] + u[1] * 2.5 * DA)
linie(mitte_a, (258, 202), "#d65a1a", 2)
txt(268, 196, "f\u00fcnfmal", 21, "#d65a1a")
txt(268, 222, "diesen Abstand", 21, "#d65a1a")
linie(mitte_k, (104, 384), "#d65a1a", 2)
txt(40, 406, "dieser Abstand: die beiden dicken", 21, "#d65a1a")
txt(40, 432, "Sterne auf der Kante OHNE Griff", 21, "#d65a1a")
txt(alkaid[0] - 4, alkaid[1] + 32, "Gro\u00dfer Wagen", 21, "#111", "middle")

# ================= Tafel B: Kassiopeia =================
linie((466, 88), (466, 462), "#c9c4b4", 3)
txt(492, 100, "\u00dcber die Kassiopeia", 23, "#1d6b3a")

# Lokal zeigt -y zum Pol. Pol-nahe Sterne bei -0.5*HB, pol-ferne bei +0.5*HB:
# damit ist die HOEHE des W exakt HB. Der Scheitel des flachen Winkels (der
# mittlere pol-ferne Stern) sitzt bei x=0, der Polarstern genau 5 Hoehen
# darueber -- die Halbierende ist also die Senkrechte durch diesen Scheitel.
HB = 40.0
lokal_b = [(-2.70 * HB, 0.5 * HB), (-1.45 * HB, -0.5 * HB), (0.0, 0.5 * HB),
           (1.45 * HB, -0.5 * HB), (2.70 * HB, 0.5 * HB),
           (0.0, 0.5 * HB),                 # Scheitel
           (0.0, 0.5 * HB - 5 * HB),        # Polarstern
           (-3.10 * HB, 0.5 * HB), (-3.10 * HB, -0.5 * HB)]  # Massklammer, links
gelegt = einpassen([dreh(v, math.radians(16)) for v in lokal_b], (596, 132, 856, 368))
kas, scheitel, pol_b, mass_u, mass_o = gelegt[:5], gelegt[5], gelegt[6], gelegt[7], gelegt[8]

ub = ((pol_b[0] - scheitel[0]) / (5 * HB), (pol_b[1] - scheitel[1]) / (5 * HB))
pb = (-ub[1], ub[0])
linie(scheitel, pol_b, "#1d6b3a", 3, "9 7")
for i in range(6):
    s = (scheitel[0] + ub[0] * i * HB, scheitel[1] + ub[1] * i * HB)
    linie((s[0] - pb[0] * 10, s[1] - pb[1] * 10), (s[0] + pb[0] * 10, s[1] + pb[1] * 10),
          "#1d6b3a", 3)
mitte_h = mass(mass_u, mass_o, pb, 0, "#1d6b3a")
zug(kas)
for s in kas:
    stern(*s)
# der Winkel, der halbiert wird
a1 = (scheitel[0] + (kas[1][0] - scheitel[0]) * 0.36,
      scheitel[1] + (kas[1][1] - scheitel[1]) * 0.36)
a2 = (scheitel[0] + (kas[3][0] - scheitel[0]) * 0.36,
      scheitel[1] + (kas[3][1] - scheitel[1]) * 0.36)
sp = (scheitel[0] + ub[0] * 40, scheitel[1] + ub[1] * 40)
pfad(f"M {a1[0]:.0f} {a1[1]:.0f} Q {sp[0]:.0f} {sp[1]:.0f} {a2[0]:.0f} {a2[1]:.0f}",
     "#1d6b3a", 3)
stern(pol_b[0], pol_b[1], 8, "#a3231a")
ring(pol_b[0], pol_b[1], 17, "#a3231a", 4)
txt(pol_b[0] - 28, pol_b[1] + 8, "Polarstern", 23, "#a3231a", "end")

mitte_b = (scheitel[0] + ub[0] * 2.5 * HB, scheitel[1] + ub[1] * 2.5 * HB)
linie(mitte_b, (584, 202), "#1d6b3a", 2)
txt(574, 196, "f\u00fcnfmal", 21, "#1d6b3a", "end")
txt(574, 222, "diese H\u00f6he", 21, "#1d6b3a", "end")
txt(mitte_h[0] - 14, mitte_h[1] + 8, "H\u00f6he", 21, "#1d6b3a", "end")
txt(mitte_h[0] - 14, mitte_h[1] + 34, "des W", 21, "#1d6b3a", "end")
linie(scheitel, (700, 366), "#1d6b3a", 2)
txt(700, 388, "diesen Winkel halbieren", 21, "#1d6b3a", "middle")
txt(kas[0][0] + 10, kas[0][1] - 20, "Kassiopeia", 21)
txt(492, 406, "Die flachere Seite des W zeigt zum Pol.", 21, "#1d6b3a")
txt(492, 432, "Ihr Scheitel ist der Ansatzpunkt.", 21, "#1d6b3a")

# ================= Trennlinie =================
linie((40, 486), (860, 486), "#c9c4b4", 3)

# ================= Tafel C =================
txt(40, 528, "Warum man beide kennt", 23, "#1d6b3a")
m = (176.0, 664.0)
kreis_gestrichelt(m[0], m[1], 82)
stern(m[0], m[1], 8, "#a3231a")
txt(m[0] + 16, m[1] + 6, "Pol", 21, "#a3231a")
wagen = (m[0] - 0.64 * 82, m[1] + 0.77 * 82)
kassi = (m[0] + 0.64 * 82, m[1] - 0.77 * 82)
stern(wagen[0], wagen[1], 9)
stern(kassi[0], kassi[1], 9)
txt(wagen[0] - 12, wagen[1] + 28, "Wagen", 21, "#111", "end")
txt(kassi[0] + 14, kassi[1] - 12, "Kassiopeia", 21)
pfad(f"M {m[0] + 102:.0f} {m[1] - 14:.0f} A 104 104 0 0 0 "
     f"{m[0] + 70:.0f} {m[1] - 76:.0f}", "#d65a1a", 4)
pfad(f"M {m[0] + 70:.0f} {m[1] - 76:.0f} l 20 1 M {m[0] + 70:.0f} "
     f"{m[1] - 76:.0f} l 2 20", "#d65a1a", 4)
txt(40, 800, "Sie stehen einander gegen\u00fcber und drehen", 21)
txt(40, 826, "sich gegen den Uhrzeigersinn um den Pol.", 21)
txt(40, 852, "Steht eines tief oder im Dunst, steht das", 21)
txt(40, 878, "andere hoch.", 21)

# ================= Tafel D =================
linie((466, 510), (466, 890), "#c9c4b4", 3)
txt(492, 528, "Wo am Himmel man ansetzt", 23, "#1d6b3a")
boden = 712.0
linie((540, boden), (760, boden), "#111", 4)
txt(540, boden + 28, "Horizont", 21)
r = 132
ex = 540 + r * math.cos(math.radians(50))
ey = boden - r * math.sin(math.radians(50))
linie((540, boden), (ex, ey), "#a3231a", 4)
stern(ex, ey, 8, "#a3231a")
txt(ex + 14, ey - 4, "Polarstern", 21, "#a3231a")
linie((540, boden), (540, boden - 150), "#c9c4b4", 3, "6 6")
txt(516, boden - 160, "senkrecht \u00fcber dir", 19, "#c9c4b4")
pfad(f"M {540 + 58} {boden:.0f} A 58 58 0 0 1 "
     f"{540 + 58 * math.cos(math.radians(50)):.0f} "
     f"{boden - 58 * math.sin(math.radians(50)):.0f}", "#a3231a", 3)
txt(540 + 70, boden - 26, "rund 50\u00b0", 21, "#a3231a")
txt(492, 800, "Der Polarstern steht so hoch \u00fcber dem", 21)
txt(492, 826, "Horizont, wie weit man n\u00f6rdlich des", 21)
txt(492, 852, "\u00c4quators ist \u2014 in Mitteleuropa rund 50\u00b0.", 21)
txt(492, 878, "So sucht man nur einen Streifen ab.", 21)

# ================= Fusszeile =================
linie((40, 900), (860, 900), "#c9c4b4", 3)
txt(40, 928, "Hat man ihn: vom Polarstern senkrecht zum Boden \u2014 dort ist Norden.",
    21, "#1d6b3a")

out.append("</svg>\n")

# Ziel liegt im Repo, ausgehend von diesem Skript (tools/skizzen/...).
WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ZIEL = os.path.join(WURZEL, "design", "skizzen", "polarstern-finden.svg")
open(ZIEL, "w", encoding="utf-8").write("".join(out))
print("geschrieben:", ZIEL)
print(f"Tafel A: Strecke/Zeigerabstand = "
      f"{math.dist(dubhe, pol_a) / math.dist(merak, dubhe):.2f} (soll 5.00)")
print(f"Tafel B: Strecke/H\u00f6he des W  = "
      f"{math.dist(scheitel, pol_b) / math.dist(mass_u, mass_o):.2f} (soll 5.00)")
