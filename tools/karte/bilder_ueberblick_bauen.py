# -*- coding: utf-8 -*-
"""Baut den groben Satellitenbild-Ueberblick `.czb` aus NASA-Kacheln.

WARUM NICHT AUS SENTINEL-2 WIE ALLES ANDERE: Weil es nicht geht. Am 19.08.2026
nachgemessen: Ein Ueberblick ueber Europa auf Zoom 9 lief auf rund 300 Minuten
JE NEUNTEL des Gebiets hinaus. Der Grund ist keine Bequemlichkeit, sondern
Rechnerei -- eine grobe Kachel deckt viele Sentinel-Aufnahmen ab, und
`bilder_bauen.py` muss aus jeder davon lesen. Der Aufwand haengt damit an der
FLAECHE, nicht an der Zoomstufe: Europa grob kostet ungefaehr so viel wie
Europa fein, und das sind Tage.

WAS STATTDESSEN: Die NASA gibt ueber GIBS fertige Kacheln in genau dem Raster
heraus, das die App benutzt -- ein Abruf, ein JPEG, rund eine Sekunde. Fuer
Europa auf Zoom 4 bis 8 sind das gut zweitausend Kacheln statt Hunderttausender
Lesezugriffe.

WELCHE SCHICHT UND WARUM: `BlueMarble_NextGeneration`. Sie ist aus Aufnahmen
eines ganzen Monats WOLKENFREI zusammengesetzt. Die tagesaktuellen Schichten
(MODIS, VIIRS) waeren feiner, zeigen aber die Wolken des gewaehlten Tages -- und
ein Satellitenbild voller Wolken ist im Gelaende schlimmer als keines: Es sieht
aus wie Schnee oder wie eine helle Flaeche und fuehrt in die Irre. Dieselbe
Begruendung steht in `bilder_bauen.py` fuer die Wolkengrenze.

RECHTE: NASA-Bilder sind gemeinfrei; die NASA bittet um Namensnennung. Der
Ueberblick traegt deshalb eine EIGENE Nennung, die nichts mit Copernicus zu tun
hat. Einzelheiten und der genaue Wortlaut in `work/quellen/satellit/LIZENZ.md`.
Solange der Satz nicht freigegeben ist, ist diese Datei nicht zur
Veroeffentlichung bestimmt.

NICHTS DAVON LAEUFT IM GERAET. Bauwerkzeug fuer den Rechner.

Aufruf:
    python tools/karte/bilder_ueberblick_bauen.py --gebiet -11 34 32 71 \\
        --zoom 4 8 --aus work/karte/europa-ueberblick.czb
"""
import argparse
import concurrent.futures as futures
import math
import os
import struct
import sys
import time
import urllib.error
import urllib.request

KENNUNG = b"CZBILD01"
FASSUNG = 1
KANTE_POTENZ = 8           # 256 Bildpunkte, wie bei den feinen Dateien
KOPF_BYTES = 48
EINTRAG_BYTES = 21

SCHICHT = "BlueMarble_NextGeneration"
SATZ = "GoogleMapsCompatible_Level8"
DATUM = "2004-08-01"       # Blue Marble ist ein fester Datensatz, kein Tagesbild
GRENZE = 8                 # feiner gibt es diese Schicht nicht
VORLAGE = ("https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/"
           "{schicht}/default/{datum}/{satz}/{z}/{y}/{x}.jpg")


def kachel_x(lon, z):
    return int((lon + 180.0) / 360.0 * (1 << z))


def kachel_y(lat, z):
    r = math.radians(lat)
    return int((1.0 - math.asinh(math.tan(r)) / math.pi) / 2.0 * (1 << z))


def hole(z, x, y, versuche=4):
    url = VORLAGE.format(schicht=SCHICHT, datum=DATUM, satz=SATZ, z=z, x=x, y=y)
    for versuch in range(versuche):
        try:
            with urllib.request.urlopen(url, timeout=60) as a:
                roh = a.read()
            # Nur ein sauberes JPEG wird uebernommen. Ein Fehlertext mit
            # 200er Antwort waere sonst eine Kachel, die auf dem Geraet als
            # kaputtes Bild auftaucht.
            if roh[:3] == b"\xff\xd8\xff" and roh[-2:] == b"\xff\xd9":
                return roh
            return None
        except urllib.error.HTTPError as f:
            if f.code == 404:
                return None            # ausserhalb der Schicht, kein Fehler
            time.sleep(1.5 * (versuch + 1))
        except Exception:
            time.sleep(1.5 * (versuch + 1))
    return None


def schreibe(ziel, gebiet, kacheln):
    west, sued, ost, nord = gebiet
    kacheln.sort(key=lambda k: (k[0] << 56) | (k[1] << 28) | k[2])
    anfang = KOPF_BYTES + EINTRAG_BYTES * len(kacheln)
    kopf = bytearray(KOPF_BYTES)
    kopf[0:8] = KENNUNG
    kopf[8] = FASSUNG
    kopf[9] = KANTE_POTENZ
    kopf[10] = min(k[0] for k in kacheln)
    kopf[11] = max(k[0] for k in kacheln)
    struct.pack_into("<iiii", kopf, 12,
                     int(west * 1e7), int(sued * 1e7),
                     int(ost * 1e7), int(nord * 1e7))
    struct.pack_into("<i", kopf, 28, len(kacheln))
    struct.pack_into("<q", kopf, 32, anfang)
    # Aufnahmezeitraum: Blue Marble ist ein Monatsbild, kein Tagesbild.
    struct.pack_into("<ii", kopf, 40, 20040801, 20040831)

    with open(ziel, "wb") as f:
        f.write(kopf)
        stelle = anfang
        for z, x, y, roh in kacheln:
            e = bytearray(EINTRAG_BYTES)
            e[0] = z
            struct.pack_into("<ii", e, 1, x, y)
            struct.pack_into("<q", e, 9, stelle)
            struct.pack_into("<i", e, 17, len(roh))
            f.write(e)
            stelle += len(roh)
        for _, _, _, roh in kacheln:
            f.write(roh)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--gebiet", nargs=4, type=float, required=True,
                   metavar=("WEST", "SUED", "OST", "NORD"))
    p.add_argument("--zoom", nargs=2, type=int, default=[3, 8])
    p.add_argument("--aus", required=True)
    p.add_argument("--gleichzeitig", type=int, default=8)
    a = p.parse_args()

    if a.zoom[1] > GRENZE:
        sys.exit(f"Diese Schicht reicht bis Zoom {GRENZE}. Feiner kommt aus "
                 "bilder_bauen.py (Sentinel-2).")

    auftraege = []
    for z in range(a.zoom[0], a.zoom[1] + 1):
        x0, x1 = kachel_x(a.gebiet[0], z), kachel_x(a.gebiet[2], z)
        y0, y1 = kachel_y(a.gebiet[3], z), kachel_y(a.gebiet[1], z)
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                auftraege.append((z, x, y))
    print(f"{len(auftraege)} Kacheln, Zoom {a.zoom[0]} bis {a.zoom[1]}, "
          f"Schicht {SCHICHT}")

    kacheln = []
    fehlend = 0
    begonnen = time.time()
    with futures.ThreadPoolExecutor(max_workers=a.gleichzeitig) as pool:
        laufend = {pool.submit(hole, z, x, y): (z, x, y) for z, x, y in auftraege}
        for i, fertig in enumerate(futures.as_completed(laufend), 1):
            z, x, y = laufend[fertig]
            roh = fertig.result()
            if roh is None:
                fehlend += 1
            else:
                kacheln.append((z, x, y, roh))
            if i % 50 == 0 or i == len(auftraege):
                dauer = time.time() - begonnen
                print("  %d von %d, %.0f s, noch etwa %.0f min"
                      % (i, len(auftraege), dauer,
                         dauer / i * (len(auftraege) - i) / 60),
                      end="\r", flush=True)

    print()
    if not kacheln:
        sys.exit("Keine einzige Kachel geholt.")
    os.makedirs(os.path.dirname(a.aus) or ".", exist_ok=True)
    schreibe(a.aus, a.gebiet, kacheln)
    print("%s: %d Kacheln (%d fehlten), %.1f MB, %.0f s"
          % (a.aus, len(kacheln), fehlend,
             os.path.getsize(a.aus) / 1024 / 1024, time.time() - begonnen))


if __name__ == "__main__":
    main()
