# -*- coding: utf-8 -*-
"""Baut aus der Zwischendatei eine fertige Kartendatei `.czk`.

Der Aufbau der Datei steht in `docs/KARTEN-FORMAT.md`. Hier steht, wie sie
entsteht: je Zoomstufe die sichtbaren Sorten waehlen, auf das Kachelraster
rechnen, mit Douglas-Peucker vereinfachen, an den Kachelgrenzen schneiden,
Abstaende als Zickzack-Varint schreiben und jede Kachel einzeln packen.

Aufruf:
    python tools/karte/bauen.py work/karte/austria.geom \
           work/karte/austria-ueberblick.czk --zoom 4 10 --toleranz 16

Die Toleranz steht in Rastereinheiten; 16 davon sind ein Bildpunkt.
"""
import argparse
import os
import struct
import sys
import time
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sorten
from auslesen import AUSSEN, INNEN, LINIE, PUNKT, SORTEN

RASTER = 4096
RAND = 64          # Ueberstand je Kachel, damit Linien an der Naht nicht abreissen
KENNUNG = b"CZKARTE1"

KOPF = struct.Struct("<BBI")
PUNKTKOPF = struct.Struct("<BH")
DATEIKOPF = struct.Struct("<8sBBBBiiiiIQ")
EINTRAG = struct.Struct("<BIIQI")

# Ab welcher Zoomstufe eine Sorte gezeigt wird. Steht auch in
# docs/KARTEN-FORMAT.md; hier ist die Durchsetzung.
# FLUSS UND WEG-HAUPT STEHEN SEIT DEM 18.08.2026 AUF 8 STATT 5 UND 6.
#
# Max: "auf der karte ist viel zu viel los als das man sich irgenwie zurecht
# findet." Nachgemessen in der Uebersichtskarte auf Zoom 6: Fluesse sind 50,6
# Prozent aller Stuetzpunkte, Hauptstrassen 25,1, Grenzen 15,3 -- zusammen
# ueber neunzig. Auf einer Uebersicht von halb Europa hilft beim Zurechtfinden
# aber die Kueste, der grosse See, die Staatsgrenze, der Ort. Nicht jeder Bach
# und nicht jede Bundesstrasse.
#
# Die Zahl war nicht willkuerlich: Am Geraet liess sich das NICHT beheben. Ein
# Laengenfilter haette, wieder nachgemessen, alle Fluesse geloescht statt der
# kleinen -- die Geometrie liegt auf dieser Stufe in kurzen Stuecken, das
# laengste Flussobjekt misst 433 von 4096 Rastereinheiten. Deshalb hier, in
# den Daten.
AB_ZOOM = {
    "wasser": 4, "fluss": 8, "bach": 11, "gletscher": 6, "wald": 7,
    "sumpf": 9, "offen": 11, "siedlung": 7, "weg-haupt": 8,
    "weg-neben": 10, "weg-pfad": 11, "weg-fein": 99, "punkt": 10, "ort": 4,
    # Die Staatsgrenze traegt die Form des Landes und gehoert auf jede Stufe.
    # Die Landesgrenze erst, wenn man Laender unterscheiden will.
    "grenze": 4, "grenze-region": 7,
}

# Was auf dieser Stufe kleiner waere als ein Bildpunkt, faellt weg.
MINDESTMASS = 16

# ... AUSSER bei Grenzen. Das ist der Fehler, den Max am 05.08.2026 als
# "auf der kleinsten zoomstufe keine laendergrenzen" gemeldet hat.
#
# Eine Grenze ist in OpenStreetMap keine Linie, sondern eine KETTE aus vielen
# kurzen Wegstuecken -- an jeder Gemeindegrenze, die daran stoesst, faengt ein
# neues an. Auf Zoom 4 sind 16 Rastereinheiten rund zehn Kilometer; damit fiel
# fast jedes einzelne Kettenglied durch den Filter. Uebrig blieben nur die
# wenigen langen Stuecke an Kuesten und Fluessen. Deutschlands Landgrenzen
# waren Konfetti: gemessen in der fertigen Kachel ueber Mitteleuropa 303
# Grenzobjekte, davon 247 kuerzer als eine Strichperiode, Median 6 von 1024
# Bildpunkten.
#
# Bei einem Tuempel ist die Regel richtig: Was kleiner als ein Bildpunkt ist,
# sieht man nicht. Bei einem Kettenglied ist sie falsch -- ein fehlendes Glied
# macht die Kette nicht kleiner, sondern kaputt. Und eine Staatsgrenze ist die
# wichtigste Linie einer Uebersichtskarte: An ihr haengen Sprache,
# Notrufnummer und Recht.
OHNE_MINDESTMASS = {"grenze", "grenze-region"}

# Ab welcher Zoomstufe eine PUNKTART gezeigt wird.
#
# WARUM DAS NICHT UEBER DIE SORTE GEHT: Beim ersten Bau lagen alle 27 451
# Ortsnamen Oesterreichs auf der Zoomstufe 4 -- und dort ist ganz Oesterreich
# EINE Kachel. Herausgekommen ist eine Kachel mit 20 559 Namen, von denen auf
# dem Schirm vielleicht zwanzig lesbar gewesen waeren. Aufgefallen ist das
# nicht in den Tests, sondern erst beim Lesen der echten Datei.
#
# Der Grundsatz: Auf jeder Stufe steht, wonach man sich auf DIESER Stufe
# richtet. Aus dem Flugzeug sucht man Wien, im Tal die Quelle.
AB_ZOOM_PUNKTART = {
    "grossstadt": 4,
    "stadt": 7,
    "dorf": 10,
    "weiler": 12,
    "einzellage": 13,

    # Gipfel erst spaet. OpenStreetMap traegt jede Kuppe als Gipfel ein --
    # allein in Oesterreich 21 010 Stueck. Bei Zoomstufe 10 lagen davon rund
    # hundert auf jeder Kachel, und die Karte war unter Namen begraben; am
    # Bildschirm gesehen, nicht in einer Zahl. Zum Peilen mit dem Kompass
    # braucht man Gipfel erst, wenn man sie auch sieht.
    "gipfel": 12,
    "sattel": 12,
    # Der Pass ist etwas anderes als der Gipfel: Er ist ein WEG durch ein
    # Gebirge und gehoert zur Planung, nicht zur Nahsicht.
    "pass": 10,

    # ZEHN, NICHT ELF -- und der Grund ist die Aufteilung der Karten, nicht die
    # Dichte. Eine Uebersichtskarte hoert bei Zoom 10 auf; alles mit einer
    # Schwelle darueber steht in ihr ueberhaupt nicht. Gemessen in der fertigen
    # Europakarte (200 Kacheln der Stufe 10): 146 Punkte insgesamt, und davon
    # nur zwei Arten -- Pass und Krankenhaus. Quelle, Brunnen, Huette,
    # Unterstand und Apotheke kamen kein einziges Mal vor.
    #
    # Auf dem Geraet hiess das: Von den fuenf Ebenen-Schaltern taten vier
    # nichts, weil es nichts zum Ein- oder Ausschalten gab. Max am 06.08.2026:
    # "irgendwie machen die overlay knoepfe noch nicht wirklich viel".
    #
    # Die Dichte traegt das. Im Oesterreich-Detail liegen auf einer Kachel der
    # Stufe 11 im Schnitt 4,9 Quellen, 2,8 Brunnen, 2,7 Apotheken, 1,8
    # Unterstaende und 0,7 Huetten; eine Kachel der Stufe 10 deckt vier davon,
    # macht rund 52 Punkte je Kachel. Das ist die dichteste Gegend, die wir
    # haben, und 52 Punkte auf 256 Rasterpunkten Kante sind gut auseinander.
    #
    # Gipfel und Sattel bleiben bei 12: Dort ist die Dichte das Argument, und
    # es ist gemessen (siehe oben).
    "huette": 10,
    "unterstand": 10,
    "krankenhaus": 10,
    "apotheke": 10,

    "quelle": 10,
    "brunnen": 10,
    "trinkwasser": 12,
    "wasserturm": 12,
    "hoehle": 12,
    "notruftelefon": 12,
    "aussicht": 13,

    "unbekannt": 12,
}


# --- Kodierung ------------------------------------------------------------

def varint(ziel, wert):
    while wert >= 128:
        ziel.append((wert & 127) | 128)
        wert >>= 7
    ziel.append(wert)


def zigzag(ziel, wert):
    varint(ziel, (wert << 1) ^ (wert >> 63))


# --- Geometrie ------------------------------------------------------------

def douglas_peucker(punkte, toleranz):
    n = len(punkte)
    if n <= 2:
        return punkte
    bleibt = bytearray(n)
    bleibt[0] = bleibt[n - 1] = 1
    stapel = [(0, n - 1)]
    t2 = float(toleranz) * float(toleranz)
    while stapel:
        a, b = stapel.pop()
        if b <= a + 1:
            continue
        ax, ay = punkte[a]
        bx, by = punkte[b]
        sx, sy = bx - ax, by - ay
        laenge2 = float(sx * sx + sy * sy)
        weiteste = -1.0
        k = -1
        for i in range(a + 1, b):
            px, py = punkte[i][0] - ax, punkte[i][1] - ay
            if laenge2 == 0.0:
                d2 = float(px * px + py * py)
            else:
                quer = float(px * sy - py * sx)
                d2 = quer * quer / laenge2
            if d2 > weiteste:
                weiteste, k = d2, i
        if weiteste > t2:
            bleibt[k] = 1
            stapel.append((a, k))
            stapel.append((k, b))
    return [punkte[i] for i in range(n) if bleibt[i]]


def schneide_linie(punkte, links, oben, rechts, unten):
    """Liang-Barsky je Abschnitt; gibt zusammenhaengende Stuecke zurueck."""
    stuecke = []
    laufend = []
    for i in range(len(punkte) - 1):
        x0, y0 = punkte[i]
        x1, y1 = punkte[i + 1]
        dx, dy = x1 - x0, y1 - y0
        t0, t1 = 0.0, 1.0
        drin = True
        for p, q in ((-dx, x0 - links), (dx, rechts - x0),
                     (-dy, y0 - oben), (dy, unten - y0)):
            if p == 0:
                if q < 0:
                    drin = False
                    break
                continue
            r = q / p
            if p < 0:
                if r > t1:
                    drin = False
                    break
                if r > t0:
                    t0 = r
            else:
                if r < t0:
                    drin = False
                    break
                if r < t1:
                    t1 = r
        if not drin:
            if len(laufend) >= 2:
                stuecke.append(laufend)
            laufend = []
            continue
        a = (int(round(x0 + t0 * dx)), int(round(y0 + t0 * dy)))
        b = (int(round(x0 + t1 * dx)), int(round(y0 + t1 * dy)))
        if laufend and laufend[-1] == a:
            laufend.append(b)
        else:
            if len(laufend) >= 2:
                stuecke.append(laufend)
            laufend = [a, b]
        if t1 < 1.0:
            stuecke.append(laufend)
            laufend = []
    if len(laufend) >= 2:
        stuecke.append(laufend)
    return stuecke


def schneide_ring(punkte, links, oben, rechts, unten):
    """Sutherland-Hodgman gegen ein Rechteck. Das Ergebnis bleibt ein Ring."""
    def kante(liste, drin, schnitt):
        aus = []
        if not liste:
            return aus
        vorher = liste[-1]
        for jetzt in liste:
            if drin(jetzt):
                if not drin(vorher):
                    aus.append(schnitt(vorher, jetzt))
                aus.append(jetzt)
            elif drin(vorher):
                aus.append(schnitt(vorher, jetzt))
            vorher = jetzt
        return aus

    def waagrecht(grenze):
        def s(a, b):
            if b[1] == a[1]:
                return (a[0], grenze)
            t = (grenze - a[1]) / float(b[1] - a[1])
            return (int(round(a[0] + t * (b[0] - a[0]))), grenze)
        return s

    def senkrecht(grenze):
        def s(a, b):
            if b[0] == a[0]:
                return (grenze, a[1])
            t = (grenze - a[0]) / float(b[0] - a[0])
            return (grenze, int(round(a[1] + t * (b[1] - a[1]))))
        return s

    p = list(punkte)
    p = kante(p, lambda k: k[0] >= links, senkrecht(links))
    p = kante(p, lambda k: k[0] <= rechts, senkrecht(rechts))
    p = kante(p, lambda k: k[1] >= oben, waagrecht(oben))
    p = kante(p, lambda k: k[1] <= unten, waagrecht(unten))
    return p


# --- Zwischendatei lesen --------------------------------------------------

def im_ausschnitt(saetze, kasten):
    """Laesst nur durch, was einen Ausschnitt beruehrt.

    WOFUER: Eine Detailkarte fuer ganz Europa waere 13 bis 24 GB gross --
    unbrauchbar auf einem Handy. Gebraucht wird sie aber ohnehin nur dort, wo
    man wirklich hingeht. Mit diesem Filter entsteht aus DERSELBEN
    Zwischendatei eine Karte fuer eine Gegend statt fuer einen Kontinent, ohne
    dass etwas neu ausgelesen werden muesste.

    ES WIRD NACH UMFASSENDEM RECHTECK GEFILTERT, nicht nach einzelnen Punkten.
    Ein Fluss, der quer durch den Ausschnitt laeuft, kann seine Stuetzpunkte
    weit ausserhalb haben; wer nur Punkte im Kasten sucht, wirft ihn weg und
    hat ein Gewaesser, das an der Kante beginnt und endet. Geprueft wird
    deshalb, ob sich die Rechtecke UEBERSCHNEIDEN. Das laesst am Rand etwas
    mehr durch, als noetig waere -- und genau das ist richtig: `bauen.py`
    schneidet ohnehin an den Kachelgrenzen, und ein Objekt zu viel kostet
    Platz, ein Objekt zu wenig kostet eine Auskunft.
    """
    sued, west, nord, ost = kasten
    for satz in saetze:
        koord = satz[2]
        if not koord:
            continue
        lons = koord[0::2]
        lats = koord[1::2]
        if max(lons) < west or min(lons) > ost:
            continue
        if max(lats) < sued or min(lats) > nord:
            continue
        yield satz


def lies_saetze(pfad):
    """Liest die Zwischendatei satzweise, im Strom.

    NICHT am Stueck einlesen. Fuer Oesterreich sind es 635 MB und das ginge
    noch; fuer Europa werden rund 27 GB erwartet, und die passen in keinen
    Arbeitsspeicher. Der Puffer der Datei uebernimmt das Sammeln -- die
    einzelnen Lesevorgaenge hier holen sich ihre Bytes aus ihm, nicht von der
    Platte.
    """
    kopfgroesse = KOPF.size
    with open(pfad, "rb", buffering=8 * 1024 * 1024) as f:
        while True:
            kopf = f.read(kopfgroesse)
            if len(kopf) < kopfgroesse:
                return
            s, a, k = KOPF.unpack(kopf)
            roh = f.read(k * 8)
            if len(roh) < k * 8:
                raise SystemExit(
                    "Die Zwischendatei bricht mitten in einem Satz ab -- "
                    "vermutlich wurde sie noch geschrieben oder der Lauf, der "
                    "sie erzeugt hat, ist abgebrochen.")
            koord = memoryview(roh).cast("i")
            punktart, name = 0, ""
            if a == PUNKT:
                punktkopf = f.read(PUNKTKOPF.size)
                if len(punktkopf) < PUNKTKOPF.size:
                    raise SystemExit("Die Zwischendatei bricht in einem Punkt ab.")
                punktart, laenge = PUNKTKOPF.unpack(punktkopf)
                name = f.read(laenge).decode("utf-8", "replace")
            yield s, a, koord, punktart, name


# --- Bauen ----------------------------------------------------------------

def auf_raster(koord, zoom):
    """Grad in Rasterpunkte der Zoomstufe (Web-Mercator), ohne numpy."""
    import math
    breite = (1 << zoom) * RASTER
    aus = []
    for i in range(0, len(koord), 2):
        lon = koord[i] / 1e7
        lat = koord[i + 1] / 1e7
        if lat > 85.05112878:
            lat = 85.05112878
        elif lat < -85.05112878:
            lat = -85.05112878
        gx = (lon + 180.0) / 360.0 * breite
        s = math.sin(math.radians(lat))
        gy = (0.5 - math.log((1 + s) / (1 - s)) / (4 * math.pi)) * breite
        aus.append((int(round(gx)), int(round(gy))))
    return aus


def kachelbereich(punkte, zoom):
    grenze = (1 << zoom) - 1
    xs = [p[0] for p in punkte]
    ys = [p[1] for p in punkte]
    x0 = max(0, min(xs) // RASTER)
    x1 = min(grenze, max(xs) // RASTER)
    y0 = max(0, min(ys) // RASTER)
    y1 = min(grenze, max(ys) // RASTER)
    return x0, y0, x1, y1


def baue_stufe(saetze, zoom, toleranz, kacheln):
    """Traegt alle Objekte einer Zoomstufe in die Kacheln ein."""
    gezeigt = 0
    for sorte, art, koord, punktart, name in saetze:
        name_sorte = SORTEN[sorte]
        if zoom < AB_ZOOM.get(name_sorte, 99):
            continue

        punkte = auf_raster(koord, zoom)

        if art == PUNKT:
            artname = sorten.ARTEN[punktart] if punktart < len(sorten.ARTEN) else "unbekannt"
            if zoom < AB_ZOOM_PUNKTART.get(artname, 12):
                continue
            x, y = punkte[0]
            schluessel = (x // RASTER, y // RASTER)
            eintrag = kacheln.setdefault(schluessel, {})
            eintrag.setdefault(sorte, []).append(
                (art, [(x % RASTER, y % RASTER)], punktart, name))
            gezeigt += 1
            continue

        xs = [p[0] for p in punkte]
        ys = [p[1] for p in punkte]
        if name_sorte not in OHNE_MINDESTMASS and \
           (max(xs) - min(xs)) < MINDESTMASS and (max(ys) - min(ys)) < MINDESTMASS:
            continue

        punkte = douglas_peucker(punkte, toleranz)
        ring = art in (AUSSEN, INNEN)
        if len(punkte) < (4 if ring else 2):
            continue
        gezeigt += 1

        x0, y0, x1, y1 = kachelbereich(punkte, zoom)
        # Ein Objekt, das ueber viele Kacheln laeuft, wuerde hier quadratisch
        # teuer. Das kommt bei einem einzelnen Fluss vor und ist selten genug,
        # um es einfach zu lassen.
        for tx in range(x0, x1 + 1):
            for ty in range(y0, y1 + 1):
                links = tx * RASTER - RAND
                oben = ty * RASTER - RAND
                rechts = tx * RASTER + RASTER + RAND
                unten = ty * RASTER + RASTER + RAND
                if ring:
                    teil = schneide_ring(punkte, links, oben, rechts, unten)
                    if len(teil) < 4:
                        continue
                    stuecke = [teil]
                else:
                    stuecke = schneide_linie(punkte, links, oben, rechts, unten)
                if not stuecke:
                    continue
                eintrag = kacheln.setdefault((tx, ty), {})
                ziel = eintrag.setdefault(sorte, [])
                for stueck in stuecke:
                    ziel.append((art,
                                 [(px - tx * RASTER, py - ty * RASTER)
                                  for px, py in stueck],
                                 0, ""))
    return gezeigt


def packe_kachel(schichten):
    namen = []
    nummer = {}
    for objekte in schichten.values():
        for _, _, _, name in objekte:
            if name and name not in nummer:
                nummer[name] = len(namen)
                namen.append(name)

    aus = bytearray()
    aus.append(1)
    varint(aus, len(namen))
    for name in namen:
        roh = name.encode("utf-8")
        varint(aus, len(roh))
        aus += roh
    varint(aus, len(schichten))
    for sorte in sorted(schichten):
        objekte = schichten[sorte]
        aus.append(sorte)
        varint(aus, len(objekte))
        for art, punkte, punktart, name in objekte:
            aus.append(art)
            varint(aus, punktart)
            varint(aus, (nummer[name] + 1) if name else 0)
            varint(aus, len(punkte))
            vx = vy = 0
            for px, py in punkte:
                zigzag(aus, px - vx)
                zigzag(aus, py - vy)
                vx, vy = px, py
    return zlib.compress(bytes(aus), 9)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("quelle")
    p.add_argument("ziel")
    p.add_argument("--zoom", nargs=2, type=int, default=[4, 10])
    p.add_argument("--toleranz", type=int, default=16)
    p.add_argument(
        "--ausschnitt", nargs=4, type=float, metavar=("SUED", "WEST", "NORD", "OST"),
        help="Nur diesen Bereich bauen, in Grad. Reihenfolge wie bei hoehen_holen.py.",
    )
    p.add_argument(
        "--zusatz", action="append", default=[], metavar="DATEI",
        help="Weitere Zwischendatei, die mit eingebaut wird. Mehrfach moeglich.",
    )
    p.add_argument(
        "--ohne", default="", metavar="SORTE,SORTE",
        help="Diese Sorten aus der HAUPTquelle weglassen -- fuer den Fall, dass "
             "eine Zusatzdatei sie vollstaendiger liefert.",
    )
    a = p.parse_args()

    verboten = set()
    for name in a.ohne.split(","):
        name = name.strip()
        if not name:
            continue
        if name not in SORTEN:
            p.error("unbekannte Sorte: %s" % name)
        verboten.add(SORTEN.index(name))

    zmin, zmax = a.zoom
    # Der Ausschnitt in derselben Einheit wie die Zwischendatei:
    # Hundertmillionstel Grad als ganze Zahl.
    kasten = None
    if a.ausschnitt:
        sued, west, nord, ost = a.ausschnitt
        kasten = (int(sued * 1e7), int(west * 1e7), int(nord * 1e7), int(ost * 1e7))

    def alle_saetze():
        for satz in lies_saetze(a.quelle):
            # Wird eine Sorte aus einer Zusatzdatei vollstaendiger geliefert,
            # muss die alte Fassung raus -- sonst laege beides uebereinander.
            if satz[0] in verboten:
                continue
            yield satz
        for weitere in a.zusatz:
            for satz in lies_saetze(weitere):
                yield satz

    def saetze():
        roh = alle_saetze()
        return im_ausschnitt(roh, kasten) if kasten else roh

    # Die Zwischendatei bekommt die Prozessnummer in den Namen. Am 05.08.2026
    # liefen versehentlich ZWEI Baulaeufe auf dasselbe Ziel: Beide schrieben in
    # dieselbe "*.teil", der erste war fertig, setzte die Karte zusammen und
    # loeschte die Zwischendatei -- der zweite ueberschrieb daraufhin die
    # fertige Karte mit null Bytes und brach ab, weil seine Zwischendatei weg
    # war. Knapp zwei Stunden Rechenzeit und ein fertiges Ergebnis dahin.
    # Mit der Prozessnummer im Namen koennen sich zwei Laeufe nicht mehr
    # gegenseitig die Datei wegziehen.
    zwischen = "%s.%d.teil" % (a.ziel, os.getpid())
    verzeichnis = []
    grenzen = [2 ** 31, 2 ** 31, -2 ** 31, -2 ** 31]

    t0 = time.time()
    with open(zwischen, "wb") as roh:
        for zoom in range(zmin, zmax + 1):
            tz = time.time()
            kacheln = {}
            gezeigt = baue_stufe(saetze(), zoom, a.toleranz, kacheln)
            for (tx, ty) in sorted(kacheln):
                gepackt = packe_kachel(kacheln[(tx, ty)])
                verzeichnis.append((zoom, tx, ty, roh.tell(), len(gepackt)))
                roh.write(gepackt)
            print("  Zoom %2d: %7d Objekte, %6d Kacheln, %8.2f MB  (%.0f s)"
                  % (zoom, gezeigt, len(kacheln),
                     sum(e[4] for e in verzeichnis if e[0] == zoom) / 1e6,
                     time.time() - tz))
            sys.stdout.flush()

    # Die Grenzen der Karte aus der Zwischendatei, nicht aus den Kacheln --
    # Kachelgrenzen sind gerundet und wuerden die Karte groesser aussehen
    # lassen, als sie ist.
    for _, _, koord, _, _ in saetze():
        for i in range(0, len(koord), 2):
            lon, lat = koord[i], koord[i + 1]
            if lon < grenzen[0]:
                grenzen[0] = lon
            if lat < grenzen[1]:
                grenzen[1] = lat
            if lon > grenzen[2]:
                grenzen[2] = lon
            if lat > grenzen[3]:
                grenzen[3] = lat

    verzeichnis.sort()
    anfang = DATEIKOPF.size + EINTRAG.size * len(verzeichnis)
    with open(a.ziel, "wb") as ziel, open(zwischen, "rb") as roh:
        ziel.write(DATEIKOPF.pack(KENNUNG, 1, 12, zmin, zmax,
                                  grenzen[0], grenzen[1], grenzen[2], grenzen[3],
                                  len(verzeichnis), anfang))
        versatz = anfang
        for zoom, tx, ty, _, laenge in verzeichnis:
            ziel.write(EINTRAG.pack(zoom, tx, ty, versatz, laenge))
            versatz += laenge
        for zoom, tx, ty, alt, laenge in verzeichnis:
            roh.seek(alt)
            ziel.write(roh.read(laenge))
    os.remove(zwischen)

    groesse = os.path.getsize(a.ziel)
    print()
    print("Karte:    %s" % a.ziel)
    print("Zoom:     %d bis %d, Toleranz %d Rastereinheiten (%.2f Bildpunkte)"
          % (zmin, zmax, a.toleranz, a.toleranz / 16.0))
    if a.ausschnitt:
        print("Ausschnitt: %.4f bis %.4f Grad Nord, %.4f bis %.4f Grad Ost"
              % (a.ausschnitt[0], a.ausschnitt[2], a.ausschnitt[1], a.ausschnitt[3]))
    print("Kacheln:  %s" % f"{len(verzeichnis):,}".replace(",", " "))
    print("Groesse:  %s Bytes (%.1f MB)"
          % (f"{groesse:,}".replace(",", " "), groesse / 1e6))
    print("Dauer:    %.0f s" % (time.time() - t0))
    return 0


if __name__ == "__main__":
    sys.exit(main())
