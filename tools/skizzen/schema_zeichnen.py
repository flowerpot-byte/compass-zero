# -*- coding: utf-8 -*-
"""Zeichnet SCHEMATISCHE Figuren -- Wege, Winkel, Abstaende -- und beschriftet
sie mit demselben Blattstil wie die eingesetzten Stiche.

WARUM NEBEN stich_beschriften.py: Jenes Werkzeug setzt eine echte
Stichzeichnung aus einer Quelle aufs Blatt. Das ist richtig, solange die Quelle
etwas GEGENSTAENDLICHES zeigt -- eine Raeucherkammer, eine Mauer, ein Ofenrohr.
Bei reiner Geometrie ist es falsch, und zwar aus zwei Gruenden:

1. Die Beschriftung im Stich ist Teil des Bildes und englisch. Man muesste sie
   ueberkleben, und ueberklebte Stellen sehen genau danach aus.
2. Am 12.08.2026 hat sich beim Nachpruefen herausgestellt, dass Figur 9-6 des
   FM 3-25.26 SICH SELBST WIDERSPRICHT: Der Flietext auf derselben Seite sagt,
   der Punkt liege rund 180 Meter neben dem Ziel, die Zeichnung beschriftet
   dieselbe Strecke mit "100 METERS". Die Rechnung im Text stimmt (10 Grad mal
   18 Meter je 1000 Meter), die Zahl in der Zeichnung nicht. Wer den Stich
   uebernimmt, uebernimmt den Fehler mit.

Deshalb wird Geometrie hier NEU gezeichnet: gleiche Aussage, richtige Zahl,
deutsche Beschriftung.

Aufruf:
    python tools/skizzen/schema_zeichnen.py <name> <zielpfad.png>
"""
import math
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from stich_beschriften import BLATT, LEISE, TINTE, schrift  # noqa: E402

WEG = (32, 60, 88)
NEBENWEG = (150, 60, 40)
HILFE = (168, 162, 150)


def pfeil(z, a, b, farbe, breite=3, spitze=13):
    z.line([a, b], fill=farbe, width=breite)
    winkel = math.atan2(b[1] - a[1], b[0] - a[0])
    for seite in (2.6, -2.6):
        z.line([b, (b[0] + spitze * math.cos(winkel + seite),
                    b[1] + spitze * math.sin(winkel + seite))], fill=farbe, width=breite)


def blatt(breite=1100, hoehe=700):
    b = Image.new("RGB", (breite, hoehe), BLATT)
    return b, ImageDraw.Draw(b)


def rahmen(z, blattbild, titel, untertitel, fusszeile, quelle):
    B, H = blattbild.size
    z.text((B // 2, 40), titel, font=schrift(28, True), fill=TINTE, anchor="mm")
    z.text((B // 2, 74), untertitel, font=schrift(17), fill=LEISE, anchor="mm")
    z.text((B // 2, H - 84), quelle, font=schrift(14), fill=(130, 130, 130), anchor="mm")
    kw = schrift(18)
    breite = z.textlength(fusszeile, font=kw)
    z.rectangle([((B - breite) / 2 - 24, H - 68), ((B + breite) / 2 + 24, H - 24)],
                fill=(242, 239, 232), outline=(201, 194, 182))
    z.text((B // 2, H - 46), fusszeile, font=kw, fill=TINTE, anchor="mm")


def versatz(ziel):
    """Der bewusste Versatz: absichtlich daneben zielen."""
    bild, z = blatt()
    start, klein = (95, 250), schrift(17)
    LX = 700                     # die Auffanglinie
    ende = (LX, 250 + int((LX - start[0]) * math.tan(math.radians(10))))

    z.line([(LX, 130), (LX, 520)], fill=(70, 110, 140), width=6)
    z.text((LX + 26, 140), "Bach, Straße, Waldkante:", font=klein, fill=(70, 110, 140))
    z.text((LX + 26, 163), "die Linie, die man nicht", font=klein, fill=(70, 110, 140))
    z.text((LX + 26, 186), "verfehlen kann", font=klein, fill=(70, 110, 140))

    # Der Weg, den man NICHT nimmt: genau aufs Ziel.
    for x in range(start[0], LX, 20):
        z.line([(x, 250), (x + 10, 250)], fill=HILFE, width=2)
    z.text((250, 224), "genau aufs Ziel gepeilt — und dann?", font=klein, fill=(140, 134, 122))

    # Der Weg, den man nimmt.
    pfeil(z, start, ende, WEG, breite=4)
    z.text((250, 330), "bewusst 10 Grad daneben", font=schrift(19, True), fill=WEG)

    # Der Winkel wird NICHT als Bogen gezeichnet: Bei zehn Grad ist der Bogen
    # so flach, dass er wie ein Fleck aussieht. Die Zahl steht stattdessen im
    # Keil zwischen beiden Wegen -- dort, wo der Keil schon breit genug ist.
    kx = start[0] + 230
    z.text((kx, (250 + 250 + (kx - start[0]) * math.tan(math.radians(10))) / 2 + 2),
           "10°", font=schrift(18, True), fill=TINTE, anchor="mm")

    # Ziel und Ankunftspunkt.
    z.rectangle([(LX - 22, 232), (LX + 22, 268)], outline=TINTE, width=3)
    z.text((LX, 250), "Z", font=schrift(20, True), fill=TINTE, anchor="mm")
    z.text((LX + 30, 250), "das Ziel: Brücke, Hütte,", font=klein, fill=TINTE, anchor="lm")
    z.text((LX + 30, 273), "Quelle", font=klein, fill=TINTE, anchor="lm")
    z.line([(LX - 14, ende[1] - 14), (LX + 14, ende[1] + 14)], fill=NEBENWEG, width=4)
    z.line([(LX - 14, ende[1] + 14), (LX + 14, ende[1] - 14)], fill=NEBENWEG, width=4)
    z.text((LX + 30, ende[1]), "hier kommt man an", font=klein, fill=NEBENWEG, anchor="lm")

    # Das Mass zwischen Ankunft und Ziel.
    mx = LX - 60
    z.line([(mx, 250), (mx, ende[1])], fill=NEBENWEG, width=2)
    for yy in (250, ende[1]):
        z.line([(mx - 8, yy), (mx + 8, yy)], fill=NEBENWEG, width=2)
    z.text((mx - 12, (250 + ende[1]) // 2), "rund 180 m", font=schrift(18, True),
           fill=NEBENWEG, anchor="rm")

    z.text((95, 430), "Am Bach steht fest: das Ziel liegt LINKS.",
           font=schrift(21, True), fill=TINTE)
    z.text((95, 465), "Kein Raten mehr — nur noch die Linie entlanggehen.",
           font=schrift(19), fill=LEISE)
    z.text((95, 512), "Ohne Versatz weiß man an dieser Stelle nicht, ob das Ziel links oder",
           font=klein, fill=LEISE)
    z.text((95, 536), "rechts liegt. Wer falsch rät, läuft mit jedem Schritt weiter weg.",
           font=klein, fill=LEISE)

    rahmen(z, bild, "Bewusst daneben zielen",
           "damit man an der Linie weiß, wohin man abbiegen muss",
           "1 Grad Versatz  ·  rund 18 m je 1000 m gegangener Strecke",
           "nach FM 3-25.26 „Map Reading and Land Navigation“, 2001, Abschnitt 9-4 e "
           "— Zeichnung neu gesetzt, siehe Werkzeugkommentar")
    bild.save(ziel)
    return bild.size


def hindernis(ziel):
    """Die vier rechtwinkligen Zuege um ein Hindernis herum."""
    bild, z = blatt()
    klein = schrift(17)

    # Das Hindernis.
    z.ellipse([(430, 212), (720, 392)], fill=(206, 214, 206), outline=(150, 162, 150), width=2)
    z.text((575, 288), "Sumpf, See, Dickicht,", font=klein, fill=(90, 100, 90), anchor="mm")
    z.text((575, 312), "Trümmer", font=klein, fill=(90, 100, 90), anchor="mm")

    y0, y1 = 185, 448
    # Der urspruengliche Kurs, gestrichelt weiter.
    for x in range(120, 1000, 20):
        z.line([(x, y0), (x + 10, y0)], fill=HILFE, width=2)
    z.text((120, y0 - 28), "der ursprüngliche Kurs, hier 90 Grad (Osten)",
           font=klein, fill=(140, 134, 122))

    pfeil(z, (120, y0), (400, y0), WEG, breite=4)
    pfeil(z, (400, y0), (400, y1), WEG, breite=4)          # Zug 1
    pfeil(z, (400, y1), (760, y1), WEG, breite=4)          # Zug 2
    pfeil(z, (760, y1), (760, y0), WEG, breite=4)          # Zug 3
    pfeil(z, (760, y0), (1000, y0), WEG, breite=4)         # Zug 4

    z.text((388, (y0 + y1) // 2), "1. Zug ", font=schrift(19, True), fill=WEG, anchor="rm")
    z.text((388, (y0 + y1) // 2 + 26), "180 Grad, 100 m ", font=klein, fill=WEG, anchor="rm")
    z.text((580, y1 + 24), "2. Zug: 90 Grad, 150 m", font=schrift(19, True), fill=WEG, anchor="mm")
    z.text((772, (y0 + y1) // 2), " 3. Zug", font=schrift(19, True), fill=NEBENWEG, anchor="lm")
    z.text((772, (y0 + y1) // 2 + 26), " 360 Grad, WIEDER 100 m", font=klein,
           fill=NEBENWEG, anchor="lm")
    z.text((880, y0 - 28), "4. Zug: 90 Grad", font=schrift(19, True), fill=WEG, anchor="mm")

    # Die beiden Zuege, die sich aufheben.
    z.line([(400, 508), (400, 538)], fill=NEBENWEG, width=2)
    z.line([(760, 508), (760, 538)], fill=NEBENWEG, width=2)
    z.line([(400, 523), (760, 523)], fill=NEBENWEG, width=2)
    z.text((580, 548), "1. und 3. Zug müssen GLEICH LANG sein — nur dann heben sie sich auf",
           font=schrift(18, True), fill=NEBENWEG, anchor="ma")

    z.text((550, 578), "Deshalb wird der 1. Zug mitgezählt. Wer ihn schätzt, kommt "
                       "versetzt wieder heraus und merkt es nicht.",
           font=klein, fill=LEISE, anchor="ma")

    rahmen(z, bild, "Um ein Hindernis herum",
           "vier Züge im rechten Winkel — die Richtung bleibt erhalten",
           "die Meterzahlen sind das Beispiel der Quelle, nicht die Vorschrift",
           "nach FM 3-25.26 „Map Reading and Land Navigation“, 2001, Abschnitt 9-4 d")
    bild.save(ziel)
    return bild.size


def kreuzpeilung(ziel):
    """Zwei Sichtlinien kreuzen sich im eigenen Standort."""
    bild, z = blatt()
    klein = schrift(17)

    # Das Kartenblatt.
    z.rectangle([(90, 120), (700, 545)], fill=(244, 240, 230), outline=(190, 182, 168), width=2)
    z.text((100, 130), "die Karte, nach dem Gelände ausgerichtet", font=schrift(15),
           fill=(150, 143, 130))

    A, B, S = (250, 190), (630, 260), (330, 470)

    for punkt, name in ((A, "Gipfel"), (B, "Kirchturm")):
        z.polygon([(punkt[0], punkt[1] - 14), (punkt[0] - 13, punkt[1] + 10),
                   (punkt[0] + 13, punkt[1] + 10)], fill=TINTE)
        z.text((punkt[0], punkt[1] - 24), name, font=schrift(18, True), fill=TINTE, anchor="ms")

    # Die beiden Sichtlinien, ueber den Schnittpunkt hinaus verlaengert.
    for punkt in (A, B):
        dx, dy = S[0] - punkt[0], S[1] - punkt[1]
        z.line([punkt, (punkt[0] + dx * 1.12, punkt[1] + dy * 1.12)], fill=WEG, width=3)

    z.ellipse([(S[0] - 11, S[1] - 11), (S[0] + 11, S[1] + 11)], fill=NEBENWEG)
    z.text((S[0] + 22, S[1] + 4), "hier stehe ich", font=schrift(20, True), fill=NEBENWEG)

    z.text((115, 300), "Linie 1:", font=schrift(17, True), fill=WEG)
    z.text((115, 322), "vom Gipfel", font=klein, fill=WEG)
    z.text((115, 344), "zurück", font=klein, fill=WEG)
    z.text((580, 400), "Linie 2:", font=schrift(17, True), fill=WEG)
    z.text((580, 422), "vom Turm", font=klein, fill=WEG)
    z.text((580, 444), "zurück", font=klein, fill=WEG)

    # Die rechte Spalte: wie die Linie entsteht.
    z.text((740, 150), "So entsteht eine Linie:", font=schrift(20, True), fill=TINTE)
    for i, zeile in enumerate([
            "Karte flach hinlegen und nach",
            "dem Gelände ausrichten — ab",
            "dann nicht mehr verdrehen.",
            "",
            "Gerade Kante über den Punkt",
            "auf der Karte legen und um ihn",
            "drehen, bis man an ihr entlang",
            "das echte Ding anvisiert.",
            "",
            "Linie ziehen. Zweiter Punkt:",
            "dasselbe noch einmal.",
            "",
            "Kein Kompass, keine Rechnung,",
            "keine Winkelscheibe nötig."]):
        z.text((740, 190 + i * 25), zeile, font=klein,
               fill=TINTE if i > 11 else LEISE)

    z.text((550, 574), "Mit DREI Punkten wird aus dem Kreuz ein kleines Dreieck — "
                       "und dessen Größe sagt, wie genau es war.",
           font=schrift(18, True), fill=TINTE, anchor="ma")

    rahmen(z, bild, "Wo bin ich? Zwei Sichtlinien kreuzen sich",
           "von bekannten Punkten zurück auf den eigenen Standort",
           "zwei Punkte reichen  ·  drei zeigen, wie genau es war",
           "nach FM 3-25.26 „Map Reading and Land Navigation“, 2001, Abschnitt 6-8")
    bild.save(ziel)
    return bild.size


def hoehenlinien(ziel):
    """Tal und Grat sehen gleich aus -- bis man auf die Spitze der U achtet."""
    bild, z = blatt()
    klein = schrift(17)

    def hoehenzug(x0, x1, mitte, nach_oben):
        """Vier Hoehenlinien; die Ausbuchtung zeigt nach oben oder nach unten."""
        for i, grund in enumerate((288, 336, 384, 432)):
            tiefe = 50 - i * 5
            punkte = []
            for k in range(0, 121):
                x = x0 + (x1 - x0) * k / 120.0
                d = (x - mitte) / 78.0
                aus = tiefe * math.exp(-d * d)
                punkte.append((x, grund - aus if nach_oben else grund + aus))
            z.line(punkte, fill=TINTE, width=2)

    # Links: das Tal. Rechts: der Grat.
    for links, ueber, nach_oben, farbe in ((70, "TAL", True, (70, 110, 140)),
                                           (585, "GRAT", False, (120, 90, 50))):
        x0, x1, mitte = links, links + 445, links + 222
        z.rectangle([(x0 - 8, 150), (x1 + 8, 500)], fill=(246, 243, 236),
                    outline=(214, 208, 196))
        z.text((mitte, 172), ueber, font=schrift(26, True), fill=TINTE, anchor="mm")
        z.text((mitte, 202), "hoher Boden ist OBEN", font=schrift(14), fill=LEISE, anchor="mm")
        hoehenzug(x0, x1, mitte, nach_oben)

        # Die Mittellinie: Bach im Tal, Kamm auf dem Grat.
        z.line([(mitte, 230), (mitte, 486)], fill=farbe, width=4)
        z.text((mitte + 14, 292) if nach_oben else (mitte + 14, 430),
               "Bach" if nach_oben else "Kamm", font=klein, fill=farbe, anchor="lm")

        # Der Pfeil auf die Spitze der Ausbuchtung -- innerhalb des Rahmens,
        # sonst laeuft die Beschriftung in die Ueberschrift hinein.
        spitze = (mitte, 288 - 50) if nach_oben else (mitte, 432 + 50)
        von = (mitte - 108, spitze[1] + (34 if nach_oben else -34))
        pfeil(z, von, (spitze[0] - 16, spitze[1]), NEBENWEG, breite=3)
        z.text((von[0] - 8, von[1]), "die Spitze", font=schrift(17, True),
               fill=NEBENWEG, anchor="rm")

    z.text((292, 520), "Spitze zeigt ZUM hohen Boden", font=schrift(19, True),
           fill=TINTE, anchor="ma")
    z.text((292, 546), "— also bachaufwärts", font=klein, fill=LEISE, anchor="ma")
    z.text((807, 520), "Spitze zeigt VOM hohen Boden weg", font=schrift(19, True),
           fill=TINTE, anchor="ma")
    z.text((807, 546), "— hangabwärts", font=klein, fill=LEISE, anchor="ma")

    z.text((550, 582), "Dieselbe Form, entgegengesetzte Richtung. Wer nur darauf achtet, "
                       "unterscheidet Tal und Grat sicher.", font=klein, fill=LEISE,
           anchor="ma")

    rahmen(z, bild, "Tal oder Grat? Die Spitze entscheidet",
           "auf der Karte sehen beide gleich aus — bis man auf die Richtung achtet",
           "Nebeneffekt: Man sieht der Karte an, wohin das Wasser fließt",
           "nach FM 3-25.26 „Map Reading and Land Navigation“, 2001, Abschnitt 10-3")
    bild.save(ziel)
    return bild.size


def zielmarken(ziel):
    """Ohne Marke laeuft man einen Bogen, mit Marken einen Zickzack ans Ziel."""
    bild, z = blatt()
    klein = schrift(17)

    def baum(x, y, h):
        z.line([(x, y), (x, y - h)], fill=(90, 78, 60), width=4)
        z.polygon([(x, y - h - 26), (x - 16, y - h + 8), (x + 16, y - h + 8)],
                  fill=(96, 118, 88))

    def fels(x, y):
        z.polygon([(x - 20, y), (x - 8, y - 26), (x + 10, y - 22), (x + 20, y)],
                  fill=(150, 146, 138))

    def turm(x, y):
        z.polygon([(x - 12, y), (x - 6, y - 62), (x + 6, y - 62), (x + 12, y)],
                  fill=(120, 110, 96))
        z.line([(x, y - 62), (x, y - 78)], fill=(120, 110, 96), width=3)

    for oben, ueberschrift in ((158, "OHNE MARKE"), (400, "MIT ZIELMARKEN")):
        z.text((95, oben - 26), ueberschrift, font=schrift(21, True), fill=TINTE)
        # Die gewollte Linie, immer gestrichelt.
        for x in range(150, 940, 20):
            z.line([(x, oben + 60), (x + 10, oben + 60)], fill=HILFE, width=2)
        z.ellipse([(140, oben + 50), (160, oben + 70)], fill=TINTE)
        z.text((150, oben + 92), "Start", font=klein, fill=LEISE, anchor="ma")
        z.rectangle([(930, oben + 44), (966, oben + 76)], outline=TINTE, width=3)
        z.text((948, oben + 60), "Z", font=schrift(20, True), fill=TINTE, anchor="mm")

    # Oben: der Bogen, den man ohne Marke laeuft.
    o = 158
    weg = [(150 + i * 8, o + 60 + (i * 8) ** 2 * 0.00019) for i in range(0, 99)]
    pfeil(z, weg[-6], weg[-1], NEBENWEG, breite=4)
    z.line(weg, fill=NEBENWEG, width=4)
    z.text((470, o + 190), "man weicht aus, umgeht, driftet — und merkt nichts davon",
           font=klein, fill=NEBENWEG, anchor="ma")
    z.text((878, o + 104), "hier kommt", font=klein, fill=NEBENWEG, anchor="ma")
    z.text((878, o + 126), "man an", font=klein, fill=NEBENWEG, anchor="ma")

    # Unten: der Zickzack zwischen den Marken.
    u = 400
    marken = [(410, "baum"), (650, "fels"), (860, "turm")]
    zacken = [(150, u + 60), (280, u + 84), (410, u + 60),
              (530, u + 38), (650, u + 60), (760, u + 80), (860, u + 60), (930, u + 60)]
    z.line(zacken, fill=WEG, width=4)
    pfeil(z, zacken[-2], zacken[-1], WEG, breite=4)
    for (x, art) in marken:
        {'baum': lambda: baum(x, u + 52, 34), 'fels': lambda: fels(x, u + 52),
         'turm': lambda: turm(x, u + 52)}[art]()
        z.ellipse([(x - 5, u + 55), (x + 5, u + 65)], fill=WEG)
    z.text((410, u + 96), "1. Marke", font=klein, fill=WEG, anchor="ma")
    z.text((650, u + 96), "2. Marke", font=klein, fill=WEG, anchor="ma")
    z.text((860, u + 96), "3. Marke", font=klein, fill=WEG, anchor="ma")
    z.text((550, u + 130), "An jeder Marke wird die Richtung neu genommen. Was dazwischen "
                           "passiert, ist gleichgültig.", font=klein, fill=LEISE, anchor="ma")

    z.text((550, 578), "Je weiter und je höher die Marke, desto seltener muss man messen.",
           font=schrift(18, True), fill=TINTE, anchor="ma")

    rahmen(z, bild, "Nicht in eine Richtung gehen, sondern zu einem Ding",
           "warum man ohne Zielmarke einen Bogen läuft",
           "Marken werden unterwegs gewählt, nie auf der Karte",
           "nach FM 3-25.26 „Map Reading and Land Navigation“, 2001, Abschnitt 11-5")
    bild.save(ziel)
    return bild.size


FIGUREN = {'zielmarken': zielmarken, 'hoehenlinien': hoehenlinien, 'versatz': versatz, 'hindernis': hindernis, 'kreuzpeilung': kreuzpeilung}

if __name__ == '__main__':
    if len(sys.argv) < 3 or sys.argv[1] not in FIGUREN:
        print(__doc__)
        print('Vorhanden: ' + ', '.join(sorted(FIGUREN)))
        sys.exit(2)
    print(FIGUREN[sys.argv[1]](sys.argv[2]))
