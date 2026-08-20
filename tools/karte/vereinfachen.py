# -*- coding: utf-8 -*-
"""Vierter Messgang: Was bringt echtes Vereinfachen?

packmass.py rundet die Koordinaten nur auf das Kachelraster. Das laesst
benachbarte Punkte zusammenfallen, mehr nicht -- eine zappelige Waldgrenze
behaelt dabei fast alle ihre Zacken, auch wenn sie auf der Uebersichtskarte
nur noch zwei Bildpunkte lang ist.

Das Ergebnis von packmass.py zeigt genau das: Stufe 13 kostet 0,83-mal so viel
wie Stufe 14. Bei einer sauber vereinfachten Kachelpyramide muesste jede Stufe
rund ein Viertel der naechstfeineren kosten, denn sie deckt die vierfache
Flaeche mit derselben Bildpunktzahl ab. Der Abstand zwischen 0,25 und 0,83 ist
die fehlende Vereinfachung -- und damit der groesste Hebel, den es hier gibt.

Gemessen wird mit Douglas-Peucker: Von einer Linie bleibt nur, was weiter als
die Toleranz von der Verbindungslinie ihrer Nachbarn abweicht.

WARUM AN EINER STICHPROBE: Douglas-Peucker laeuft je Linie und laesst sich
nicht ueber alle 75 Millionen Punkte auf einmal rechnen; ein voller Durchgang
dauert Stunden. Gemessen wird deshalb jedes zwanzigste Objekt und das Ergebnis
mit zwanzig malgenommen. Die Stichprobe ist systematisch, nicht zufaellig --
sie trifft dadurch alle Sorten und Gegenden im selben Verhaeltnis.

Aufruf:  python tools/karte/vereinfachen.py work/karte/austria.geom
"""
import os
import sys
import time
import zlib

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from auslesen import SORTEN, AUSSEN, PUNKT, INNEN
from packmass import (AB_ZOOM, MINDESTMASS, RASTER, gitter, lies, varint,
                      zigzag)

SCHRITT = 20          # jedes zwanzigste Objekt
TOLERANZEN = (4, 8, 16)   # in Rastereinheiten; 16 Einheiten sind ein Bildpunkt


def douglas_peucker(x, y, toleranz):
    """Gibt eine Maske der Punkte zurueck, die bleiben."""
    n = len(x)
    bleibt = np.zeros(n, dtype=bool)
    if n <= 2:
        bleibt[:] = True
        return bleibt
    bleibt[0] = bleibt[n - 1] = True
    stapel = [(0, n - 1)]
    t2 = float(toleranz) * float(toleranz)
    while stapel:
        a, b = stapel.pop()
        if b <= a + 1:
            continue
        ax, ay = float(x[a]), float(y[a])
        bx, by = float(x[b]), float(y[b])
        sx, sy = bx - ax, by - ay
        px = x[a + 1:b].astype(np.float64) - ax
        py = y[a + 1:b].astype(np.float64) - ay
        laenge2 = sx * sx + sy * sy
        if laenge2 == 0.0:
            # Anfang und Ende fallen zusammen (geschlossener Ring): dann zaehlt
            # der reine Abstand zum Anfangspunkt.
            abstand2 = px * px + py * py
        else:
            quer = px * sy - py * sx
            abstand2 = quer * quer / laenge2
        i = int(np.argmax(abstand2))
        if abstand2[i] > t2:
            k = a + 1 + i
            bleibt[k] = True
            stapel.append((a, k))
            stapel.append((k, b))
    return bleibt


def messe_stufe(daten, zoom, toleranz):
    sorte, art, anfang, laenge, lon7, lat7 = daten

    sichtbar = np.zeros(len(sorte), dtype=bool)
    for name, ab in AB_ZOOM.items():
        if zoom >= ab:
            sichtbar |= (sorte == SORTEN.index(name))
    ausgewaehlt = np.flatnonzero(sichtbar)[::SCHRITT]
    if len(ausgewaehlt) == 0:
        return None

    gx, gy = gitter(lon7, lat7, zoom)

    bytes_gesamt = 0
    punkte_vorher = 0
    punkte_nachher = 0
    objekte = 0
    strom = bytearray()

    for i in ausgewaehlt:
        a, l = int(anfang[i]), int(laenge[i])
        x, y = gx[a:a + l], gy[a:a + l]
        if int(art[i]) == PUNKT:
            objekte += 1
            punkte_vorher += 1
            punkte_nachher += 1
            strom.append(int(art[i]))
            varint(strom, 1)
            zigzag(strom, int(x[0]) % RASTER)
            zigzag(strom, int(y[0]) % RASTER)
            continue

        breite = int(x.max() - x.min())
        hoehe = int(y.max() - y.min())
        if breite < MINDESTMASS and hoehe < MINDESTMASS:
            continue

        m = douglas_peucker(x, y, toleranz)
        rest = int(m.sum())
        ring = int(art[i]) in (AUSSEN, INNEN)
        if rest < (4 if ring else 2):
            continue

        objekte += 1
        punkte_vorher += l
        punkte_nachher += rest

        xs, ys = x[m], y[m]
        strom.append(int(art[i]))
        varint(strom, rest)
        vx, vy = int(xs[0]) % RASTER, int(ys[0]) % RASTER
        zigzag(strom, vx)
        zigzag(strom, vy)
        for k in range(1, rest):
            zigzag(strom, int(xs[k]) - int(xs[k - 1]))
            zigzag(strom, int(ys[k]) - int(ys[k - 1]))

    if objekte == 0:
        return None
    roh = len(strom)
    gepackt = len(zlib.compress(bytes(strom), 6))
    return {
        "zoom": zoom,
        "objekte": objekte * SCHRITT,
        "vorher": punkte_vorher * SCHRITT,
        "nachher": punkte_nachher * SCHRITT,
        "roh": roh * SCHRITT,
        "gepackt": gepackt * SCHRITT,
        "quote": gepackt / max(1, roh),
    }


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    daten = lies(sys.argv[1])
    print("Zwischendatei: %s Objekte, %s Stuetzpunkte"
          % (f"{len(daten[0]):,}".replace(",", " "),
             f"{len(daten[4]):,}".replace(",", " ")))
    print("Stichprobe: jedes %d. Objekt, Ergebnis mal %d gerechnet"
          % (SCHRITT, SCHRITT))

    for toleranz in TOLERANZEN:
        print()
        print("=== Toleranz %d Rastereinheiten (%.2f Bildpunkte) ==="
              % (toleranz, toleranz / 16.0))
        print("%4s %12s %14s %14s %7s %12s"
              % ("Zoom", "Objekte", "Punkte vorher", "danach", "Rest", "gepackt MB"))
        zeilen = []
        for z in range(4, 15):
            t0 = time.time()
            e = messe_stufe(daten, z, toleranz)
            if not e:
                continue
            zeilen.append(e)
            print("%4d %12s %14s %14s %6.1f%% %12.1f   (%.0f s)"
                  % (e["zoom"],
                     f"{e['objekte']:,}".replace(",", " "),
                     f"{e['vorher']:,}".replace(",", " "),
                     f"{e['nachher']:,}".replace(",", " "),
                     100.0 * e["nachher"] / max(1, e["vorher"]),
                     e["gepackt"] / 1e6, time.time() - t0))
        ueber = sum(e["gepackt"] for e in zeilen if e["zoom"] <= 10)
        detail = sum(e["gepackt"] for e in zeilen if e["zoom"] >= 11)
        print("  Uebersicht z4-z10:  %8.1f MB" % (ueber / 1e6))
        print("  Detail     z11-z14: %8.1f MB" % (detail / 1e6))
    return 0


if __name__ == "__main__":
    sys.exit(main())
