# -*- coding: utf-8 -*-
"""Baut aus Copernicus-GLO-90-Kacheln eine Hoehendatei `.czh`.

Der Aufbau steht in `docs/HOEHEN-FORMAT.md`. Zur Lizenz und zu den
vorgeschriebenen Hinweisen siehe `tools/karte/hoehen_holen.py` -- sie werden
hier in die erzeugte Datei danebengeschrieben, damit sie mit ihr reisen.

Aufruf:
    python tools/karte/hoehen_bauen.py work/karte/hoehen \
           work/karte/oesterreich-hoehen.czh --zoom 4 10 --kante 64
"""
import argparse
import glob
import math
import os
import struct
import sys
import time
import zlib

import numpy as np
import rasterio

KENNUNG = b"CZHOEHE1"
DATEIKOPF = struct.Struct("<8sBBBBiiiiIQ")
EINTRAG = struct.Struct("<BIIQI")

METER_MIN, METER_MAX = -500, 9000
SCHRITTWEITEN = (1, 2, 5, 10)


def varint(ziel, wert):
    while wert >= 128:
        ziel.append((wert & 127) | 128)
        wert >>= 7
    ziel.append(wert)


def zigzag(ziel, wert):
    varint(ziel, (wert << 1) ^ (wert >> 63))


class Hoehenquelle:
    """Haelt die Quellkacheln offen und liest Hoehen an Punkten.

    Die Kacheln werden EINMAL vollstaendig in den Speicher gelesen und dann
    behalten. Eine Kachel ist 1200 x 1200 Werte, also rund 3 MB -- bei ein
    paar Dutzend passt das bequem, und der Unterschied zum Nachschlagen auf
    der Platte ist Minuten gegen Stunden.
    """

    def __init__(self, ordner):
        self.felder = {}
        self.grenzen = []
        for pfad in sorted(glob.glob(os.path.join(ordner, "*.tif"))):
            with rasterio.open(pfad) as quelle:
                daten = quelle.read(1)
                self.felder[pfad] = (daten, ~quelle.transform, quelle.bounds)
                self.grenzen.append((quelle.bounds, pfad))
        if not self.felder:
            raise SystemExit("Keine Hoehenkacheln in %s" % ordner)

    def lies(self, lon, lat):
        """Hoehe in Metern, oder None ausserhalb der vorhandenen Kacheln."""
        for grenze, pfad in self.grenzen:
            if grenze.left <= lon < grenze.right and grenze.bottom <= lat < grenze.top:
                daten, rueck, _ = self.felder[pfad]
                spalte, zeile = rueck * (lon, lat)
                s, z = int(spalte), int(zeile)
                if 0 <= z < daten.shape[0] and 0 <= s < daten.shape[1]:
                    wert = float(daten[z, s])
                    if np.isnan(wert):
                        return None
                    return wert
                return None
        return None


def kachelgrenzen(zoom, x, y):
    n = 1 << zoom
    lon1 = x / n * 360.0 - 180.0
    lon2 = (x + 1) / n * 360.0 - 180.0
    lat1 = math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * y / n))))
    lat2 = math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * (y + 1) / n))))
    return lon1, lat2, lon2, lat1


def kachel_bauen(quelle, zoom, x, y, kante):
    """Gibt die gepackten Bytes zurueck, oder None wenn kein Land drin ist.

    DAS GITTER REICHT EINE STUETZSTELLE UEBER DIE KACHEL HINAUS. Ohne diesen
    Rand hat der aeusserste Punkt keinen Nachbarn, seine Neigung wird auf null
    gerechnet, und die Kachel hellt an ihrer Kante auf -- nebeneinander ergibt
    das ein sichtbares Gitter ueber der ganzen Karte. Am 04.08.2026 im
    Bildschirmfoto gesehen, nicht in einer Zahl.

    Die Kachel selbst liegt damit auf den Stuetzstellen 1 bis kante-2; 0 und
    kante-1 sind der Rand und werden nur fuer die Neigung gebraucht.
    """
    lon1, sued, lon2, nord = kachelgrenzen(zoom, x, y)
    n = 1 << zoom
    innen = kante - 2
    werte = np.zeros((kante, kante), dtype=np.float64)
    getroffen = 0
    for zeile in range(kante):
        # Gleichmaessig im Kachelraster, nicht in Grad: sonst verzerrt die
        # Schummerung nach Norden hin. Der Rand liegt ausserhalb 0..1.
        anteil = (zeile - 0.5) / innen
        ymerc = (y + anteil) / n
        ymerc = min(max(ymerc, 1e-9), 1 - 1e-9)
        lat = math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * ymerc))))
        for spalte in range(kante):
            lon = lon1 + (lon2 - lon1) * ((spalte - 0.5) / innen)
            hoehe = quelle.lies(lon, lat)
            if hoehe is None:
                werte[zeile, spalte] = np.nan
            else:
                werte[zeile, spalte] = hoehe
                getroffen += 1
    if getroffen == 0:
        return None

    # Loecher schliessen: Ein Schummerungsbild mit Loechern sieht aus wie ein
    # Fehler im Geraet, nicht wie fehlende Daten. Gefuellt wird mit dem
    # Mittelwert der vorhandenen Werte -- gemeint ist "flach", nicht "Loch".
    fehlt = np.isnan(werte)
    if fehlt.any():
        werte[fehlt] = float(np.nanmean(werte))

    ganz = np.clip(np.round(werte), METER_MIN, METER_MAX).astype(np.int32)
    grund = int(ganz.min())
    spanne = int(ganz.max()) - grund
    # Die groebste Schrittweite waehlen, die noch unter einem Prozent der
    # Spannweite bleibt -- feiner sieht man in einer Schummerung nicht.
    schritt = 1
    for kandidat in SCHRITTWEITEN:
        if kandidat <= max(1, spanne // 100):
            schritt = kandidat
    ganz = grund + ((ganz - grund) // schritt) * schritt

    aus = bytearray()
    aus.append(1)
    aus += struct.pack(">h", grund)
    aus.append(schritt)
    zeilenanfang = grund
    for zeile in range(kante):
        wert = int(ganz[zeile, 0])
        zigzag(aus, (wert - zeilenanfang) // schritt)
        zeilenanfang = wert
        vorher = wert
        for spalte in range(1, kante):
            jetzt = int(ganz[zeile, spalte])
            zigzag(aus, (jetzt - vorher) // schritt)
            vorher = jetzt
    return zlib.compress(bytes(aus), 9)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("quelle")
    p.add_argument("ziel")
    p.add_argument("--zoom", nargs=2, type=int, default=[4, 10])
    p.add_argument("--kante", type=int, default=64)
    a = p.parse_args()

    if a.kante & (a.kante - 1) or a.kante < 2 or a.kante > 512:
        raise SystemExit("Kante muss eine Zweierpotenz zwischen 2 und 512 sein")

    hoehen = Hoehenquelle(a.quelle)
    west = min(g.left for g, _ in hoehen.grenzen)
    ost = max(g.right for g, _ in hoehen.grenzen)
    sued = min(g.bottom for g, _ in hoehen.grenzen)
    nord = max(g.top for g, _ in hoehen.grenzen)
    print("Quellkacheln: %d, %.2f..%.2f O, %.2f..%.2f N"
          % (len(hoehen.felder), west, ost, sued, nord))

    zmin, zmax = a.zoom
    zwischen = a.ziel + ".teil"
    verzeichnis = []
    t0 = time.time()
    with open(zwischen, "wb") as roh:
        for zoom in range(zmin, zmax + 1):
            tz = time.time()
            n = 1 << zoom
            x0 = max(0, int((west + 180.0) / 360.0 * n))
            x1 = min(n - 1, int((ost + 180.0) / 360.0 * n))
            def ykachel(lat):
                s = math.sin(math.radians(max(-85.05, min(85.05, lat))))
                return int((0.5 - math.log((1 + s) / (1 - s)) / (4 * math.pi)) * n)
            y0 = max(0, ykachel(nord))
            y1 = min(n - 1, ykachel(sued))
            gebaut = 0
            for y in range(y0, y1 + 1):
                for x in range(x0, x1 + 1):
                    gepackt = kachel_bauen(hoehen, zoom, x, y, a.kante)
                    if gepackt is None:
                        continue
                    verzeichnis.append((zoom, x, y, roh.tell(), len(gepackt)))
                    roh.write(gepackt)
                    gebaut += 1
            print("  Zoom %2d: %5d Kacheln, %8.2f MB  (%.0f s)"
                  % (zoom, gebaut,
                     sum(e[4] for e in verzeichnis if e[0] == zoom) / 1e6,
                     time.time() - tz))
            sys.stdout.flush()

    verzeichnis.sort()
    anfang = DATEIKOPF.size + EINTRAG.size * len(verzeichnis)
    kantenbits = int(math.log2(a.kante))
    with open(a.ziel, "wb") as ziel, open(zwischen, "rb") as roh:
        ziel.write(DATEIKOPF.pack(
            KENNUNG, 1, kantenbits, zmin, zmax,
            int(round(west * 1e7)), int(round(sued * 1e7)),
            int(round(ost * 1e7)), int(round(nord * 1e7)),
            len(verzeichnis), anfang))
        versatz = anfang
        for zoom, x, y, _, laenge in verzeichnis:
            ziel.write(EINTRAG.pack(zoom, x, y, versatz, laenge))
            versatz += laenge
        for zoom, x, y, alt, laenge in verzeichnis:
            roh.seek(alt)
            ziel.write(roh.read(laenge))
    os.remove(zwischen)

    # Der Lizenzhinweis reist mit der Datei, nicht nur im Quelltext.
    hinweis = os.path.join(os.path.dirname(os.path.abspath(a.ziel)), "HOEHEN-HINWEIS.txt")
    with open(hinweis, "w", encoding="utf-8") as f:
        f.write(
            "produced using Copernicus WorldDEM-30\n"
            "(c) DLR e.V. 2010-2014 and (c) Airbus Defence and Space GmbH 2014-2018\n"
            "provided under COPERNICUS by the European Union and ESA;\n"
            "all rights reserved.\n")

    groesse = os.path.getsize(a.ziel)
    print()
    print("Hoehendatei: %s" % a.ziel)
    print("Kacheln:     %s" % f"{len(verzeichnis):,}".replace(",", " "))
    print("Kante:       %d Stuetzstellen" % a.kante)
    print("Groesse:     %s Bytes (%.1f MB)"
          % (f"{groesse:,}".replace(",", " "), groesse / 1e6))
    print("Dauer:       %.0f s" % (time.time() - t0))
    return 0


if __name__ == "__main__":
    sys.exit(main())
