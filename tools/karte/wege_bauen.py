# -*- coding: utf-8 -*-
"""Baut aus OpenStreetMap-Rohdaten ein Wegenetz `.czw` fuer die Routenberechnung.

WARUM AUS DEN ROHDATEN UND NICHT AUS DER FERTIGEN KARTE: Die `.czk` speichert
Wege als Striche je Kachel -- an den Kachelgrenzen geschnitten, vereinfacht und
ohne Knotenkennungen. Woher ein Weg kommt und wo er einen anderen KREUZT,
steht dort nicht; zwei Striche, die sich auf dem Bild beruehren, koennen eine
Bruecke und ein Bach sein. Ein Wegenetz daraus zu erraten hiesse, Abzweigungen
zu erfinden, die es nicht gibt -- und eine erfundene Abzweigung schickt jemanden
in eine Richtung, aus der er umkehren muss.

In den Rohdaten steht beides ausdruecklich: dieselbe Knotenkennung in zwei
Wegen bedeutet eine echte Kreuzung, eine verschiedene bedeutet keine.

NICHTS DAVON LAEUFT IM GERAET. Das hier ist ein Bauwerkzeug fuer den Rechner.

Aufruf:
    python tools/karte/wege_bauen.py work/karte/austria-latest.osm.pbf \\
        --gebiet 12.6 47.4 13.6 48.0 --aus work/karte/salzburg.czw
    python tools/karte/wege_bauen.py <pbf> --gebiet ... --zaehlen
"""
import argparse
import math
import os
import struct
import sys

import osmium

KENNUNG = b"CZWEG001"
FASSUNG = 1
KOPF_BYTES = 40

# Was begehbar ist, und wie teuer es sich geht.
#
# DIE ZAHL IST EIN AUFSCHLAG AUF DIE LAENGE, kein Zeitwert: Ein Wanderweg
# zaehlt so lang, wie er ist; eine Landstrasse zaehlt das Anderthalbfache,
# weil man sie zu Fuss meiden will, ohne sie zu verbieten. Wer keinen anderen
# Weg hat, bekommt sie trotzdem.
WEGE = {
    "path": 1.0,
    "footway": 1.0,
    "steps": 1.6,          # Treppen sind kurz und trotzdem muehsam
    "bridleway": 1.1,
    "track": 1.1,
    "living_street": 1.2,
    "pedestrian": 1.0,
    "residential": 1.3,
    "unclassified": 1.3,
    "service": 1.4,
    "tertiary": 1.5,
    "secondary": 1.8,
    "primary": 2.2,        # zu Fuss unangenehm, aber begehbar
    "cycleway": 1.1,
}

# Ausdruecklich NICHT dabei: Autobahnen und Schnellstrassen. Sie sind zu Fuss
# nicht begehbar, und ein Weg darueber waere kein unbequemer Vorschlag,
# sondern ein lebensgefaehrlicher.
VERBOTEN = {"motorway", "motorway_link", "trunk", "trunk_link"}

ERDRADIUS = 6371000.0


def entfernung(lat1, lon1, lat2, lon2):
    """Meter zwischen zwei Stellen, Haversine."""
    p1 = math.radians(lat1)
    p2 = math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * ERDRADIUS * math.asin(math.sqrt(a))


class Wegsammler(osmium.SimpleHandler):
    """Erster Durchgang: welche Knoten kommen in mehr als einem Weg vor?"""

    def __init__(self, gebiet):
        super().__init__()
        self.west, self.sued, self.ost, self.nord = gebiet
        self.wege = []          # (art, [knotennummern])
        self.zaehler = {}       # knotennummer -> wie oft benutzt
        self.verworfen = 0

    def way(self, w):
        art = w.tags.get("highway")
        if art is None or art in VERBOTEN:
            return
        if art not in WEGE:
            return
        # Gesperrtes bleibt draussen. "private" ist kein Vorschlag fuer einen
        # Weg, den jemand im Ernstfall geht -- aber "no" heisst wirklich nein.
        if w.tags.get("foot") == "no" or w.tags.get("access") == "no":
            self.verworfen += 1
            return
        knoten = [n.ref for n in w.nodes]
        if len(knoten) < 2:
            return
        self.wege.append((art, knoten))
        for nummer in knoten:
            self.zaehler[nummer] = self.zaehler.get(nummer, 0) + 1
        # Anfang und Ende sind IMMER Knoten, auch wenn sie nur einmal
        # vorkommen: Sonst endet ein Weg im Nichts und der Graph verliert sein
        # letztes Stueck.
        self.zaehler[knoten[0]] = self.zaehler.get(knoten[0], 0) + 1
        self.zaehler[knoten[-1]] = self.zaehler.get(knoten[-1], 0) + 1


class Ortssammler(osmium.SimpleHandler):
    """Zweiter Durchgang: die Stellen der gebrauchten Knoten."""

    def __init__(self, gebraucht):
        super().__init__()
        self.gebraucht = gebraucht
        self.stellen = {}

    def node(self, n):
        if n.id in self.gebraucht:
            self.stellen[n.id] = (n.location.lat, n.location.lon)


def baue(pbf, gebiet, nur_zaehlen=False):
    print("Erster Durchgang: Wege sammeln ...")
    wege = Wegsammler(gebiet)
    wege.apply_file(pbf)
    print(f"  {len(wege.wege)} begehbare Wege, {len(wege.zaehler)} Knoten beruehrt, "
          f"{wege.verworfen} gesperrte verworfen")

    gebraucht = set()
    for _art, knoten in wege.wege:
        gebraucht.update(knoten)

    print("Zweiter Durchgang: Knotenstellen holen ...")
    orte = Ortssammler(gebraucht)
    orte.apply_file(pbf)
    print(f"  {len(orte.stellen)} Stellen gefunden")

    west, sued, ost, nord = gebiet

    def drin(nummer):
        stelle = orte.stellen.get(nummer)
        if stelle is None:
            return False
        lat, lon = stelle
        return west <= lon <= ost and sued <= lat <= nord

    # Kanten bilden: zwischen zwei Kreuzungsknoten liegt eine Kante mit der
    # ganzen Geometrie dazwischen.
    knotennummern = {}
    kanten = []
    for art, knoten in wege.wege:
        stueck = []
        for nummer in knoten:
            stueck.append(nummer)
            ist_kreuzung = wege.zaehler.get(nummer, 0) > 1
            if len(stueck) > 1 and (ist_kreuzung or nummer == knoten[-1]):
                if any(drin(k) for k in stueck):
                    kanten.append((art, list(stueck)))
                stueck = [nummer]
    for art, stueck in kanten:
        for nummer in (stueck[0], stueck[-1]):
            if nummer not in knotennummern and nummer in orte.stellen:
                knotennummern[nummer] = len(knotennummern)

    laenge_gesamt = 0.0
    fertige = []
    for art, stueck in kanten:
        if stueck[0] not in knotennummern or stueck[-1] not in knotennummern:
            continue
        punkte = [orte.stellen[k] for k in stueck if k in orte.stellen]
        if len(punkte) < 2:
            continue
        meter = sum(
            entfernung(punkte[i][0], punkte[i][1], punkte[i + 1][0], punkte[i + 1][1])
            for i in range(len(punkte) - 1)
        )
        if meter <= 0:
            continue
        laenge_gesamt += meter
        fertige.append((knotennummern[stueck[0]], knotennummern[stueck[-1]],
                        art, meter, punkte))

    print(f"\n  {len(knotennummern)} Knoten, {len(fertige)} Kanten, "
          f"{laenge_gesamt / 1000:.0f} km Wegenetz")
    nach_art = {}
    for _a, _b, art, meter, _p in fertige:
        nach_art[art] = nach_art.get(art, 0) + meter
    for art in sorted(nach_art, key=lambda k: -nach_art[k]):
        print(f"    {nach_art[art] / 1000:8.0f} km  {art}")

    return knotennummern, orte.stellen, fertige


def main():
    p = argparse.ArgumentParser()
    p.add_argument("pbf")
    p.add_argument("--gebiet", nargs=4, type=float, required=True,
                   metavar=("WEST", "SUED", "OST", "NORD"))
    p.add_argument("--aus")
    p.add_argument("--zaehlen", action="store_true")
    a = p.parse_args()

    if not os.path.isfile(a.pbf):
        sys.exit(f"{a.pbf} gibt es nicht. Rohdaten holt tools/karte/holen.py.")

    knoten, stellen, kanten = baue(a.pbf, a.gebiet, a.zaehlen)
    if a.zaehlen:
        return
    ziel = a.aus or os.path.splitext(a.pbf)[0] + ".czw"
    schreibe(ziel, knoten, stellen, kanten, a.gebiet)
    print(f"\n{ziel}: {os.path.getsize(ziel) / 1e6:.1f} MB")


def schreibe(ziel, knotennummern, stellen, kanten, gebiet):
    """Schreibt die Datei -- Aufbau in docs/WEGE-FORMAT.md."""
    umgekehrt = [None] * len(knotennummern)
    for nummer, i in knotennummern.items():
        umgekehrt[i] = nummer

    koerper = bytearray()
    kantenversatz = []
    for von, nach, art, meter, punkte in kanten:
        kantenversatz.append(len(koerper))
        koerper += struct.pack("<IIHI", von, nach, int(round(WEGE[art] * 100)),
                               int(round(meter)))
        koerper += struct.pack("<H", len(punkte))
        for lat, lon in punkte:
            koerper += struct.pack("<ii", int(round(lon * 1e7)), int(round(lat * 1e7)))

    west, sued, ost, nord = gebiet
    kopf = bytearray(KOPF_BYTES)
    kopf[0:8] = KENNUNG
    kopf[8] = FASSUNG
    struct.pack_into("<IIiiii", kopf, 12, len(umgekehrt), len(kanten),
                     int(round(west * 1e7)), int(round(sued * 1e7)),
                     int(round(ost * 1e7)), int(round(nord * 1e7)))

    with open(ziel, "wb") as f:
        f.write(kopf)
        for nummer in umgekehrt:
            lat, lon = stellen[nummer]
            f.write(struct.pack("<ii", int(round(lon * 1e7)), int(round(lat * 1e7))))
        anfang = KOPF_BYTES + 8 * len(umgekehrt) + 4 * len(kanten)
        for v in kantenversatz:
            f.write(struct.pack("<I", anfang + v))
        f.write(koerper)


if __name__ == "__main__":
    main()
