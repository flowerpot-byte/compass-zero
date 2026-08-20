# -*- coding: utf-8 -*-
"""Fuegt die kurzen Grenz-Wegstuecke zu langen Ketten zusammen.

WARUM DAS NOETIG IST
--------------------
Eine Staatsgrenze ist in OpenStreetMap keine Linie, sondern eine KETTE aus
vielen kurzen Wegstuecken: An jeder Gemeindegrenze, die daran stoesst, faengt
ein neues an. In der Kachel ueber Mitteleuropa (Zoom 4) sind das 30 537
einzelne Stuecke. Einzeln weitergereicht macht das drei Sorten Aerger:

  1. Der Kleinobjekt-Filter des Kartenbaus verschluckt sie. Auf Zoom 4 sind
     seine 16 Rastereinheiten rund zehn Kilometer -- gemessen wuerden 97 % der
     Stuecke wegfallen. Das war der Grund, warum am 05.08.2026 auf der
     kleinsten Zoomstufe keine Laendergrenzen zu sehen waren.
  2. Das Vereinfachen (Douglas-Peucker) kann an einem 2-km-Stueck nichts
     sparen. An einer 900-km-Kette spart es sehr viel.
  3. Das Strichmuster faengt bei JEDEM Stueck von vorn an. Aus einer
     Strich-Punkt-Linie wuerden 30 000 einzelne Tupfen.

Zusammengefuegt wird jede Grenze zu einer Handvoll langer Linien. Damit
loesen sich alle drei Punkte von selbst, und die Karte wird kleiner statt
groesser.

WIE ZUSAMMENGEFUEGT WIRD: Zwei Stuecke gehoeren aneinander, wenn ein Endpunkt
des einen GENAU auf einem Endpunkt des anderen liegt -- in OSM teilen sich
benachbarte Wege denselben Knoten, die Koordinaten sind also Bit fuer Bit
gleich. Wo drei Laender zusammenstossen, treffen mehrere Stuecke auf einen
Punkt; dann wird eines genommen und die anderen bilden ihre eigene Kette. Das
ist fuer eine Zeichnung richtig -- gezeichnet wird am Ende jeder Strich genau
einmal, egal wie er gruppiert ist.

Aufruf:
    python tools/karte/grenzen_ketten.py work/karte/grenzen.geom \\
           work/karte/grenzen-ketten.geom
"""
import collections
import os
import struct
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from auslesen import LINIE, SORTEN, schreibe

KOPF = struct.Struct("<BBI")
PUNKTKOPF = struct.Struct("<BH")
PUNKT = 2


def lies(pfad):
    """Liest die Zwischendatei und gibt je Sorte die Punktlisten zurueck."""
    aus = collections.defaultdict(list)
    with open(pfad, "rb", buffering=8 * 1024 * 1024) as f:
        while True:
            kopf = f.read(KOPF.size)
            if len(kopf) < KOPF.size:
                return aus
            s, art, k = KOPF.unpack(kopf)
            roh = f.read(k * 8)
            if art == PUNKT:
                _, laenge = PUNKTKOPF.unpack(f.read(PUNKTKOPF.size))
                f.read(laenge)
                continue
            z = memoryview(roh).cast("i")
            aus[s].append([(z[i], z[i + 1]) for i in range(0, len(z), 2)])


def verkette(wege):
    """Fuegt Wegstuecke an gemeinsamen Endpunkten zu Ketten zusammen."""
    enden = collections.defaultdict(list)
    for i, w in enumerate(wege):
        enden[w[0]].append(i)
        enden[w[-1]].append(i)
    benutzt = [False] * len(wege)

    def nimm(punkt, ausser):
        """Ein noch unbenutztes Stueck, das an diesem Punkt haengt."""
        for j in enden.get(punkt, ()):
            if j != ausser and not benutzt[j]:
                return j
        return None

    ketten = []
    for i in range(len(wege)):
        if benutzt[i]:
            continue
        benutzt[i] = True
        kette = list(wege[i])
        # Hinten verlaengern.
        while True:
            j = nimm(kette[-1], i)
            if j is None:
                break
            benutzt[j] = True
            stueck = wege[j]
            if stueck[0] == kette[-1]:
                kette.extend(stueck[1:])
            else:
                kette.extend(reversed(stueck[:-1]))
        # Vorn verlaengern.
        while True:
            j = nimm(kette[0], i)
            if j is None:
                break
            benutzt[j] = True
            stueck = wege[j]
            if stueck[-1] == kette[0]:
                kette[:0] = stueck[:-1]
            else:
                kette[:0] = list(reversed(stueck[1:]))
        ketten.append(kette)
    return ketten


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 1
    quelle, ziel = sys.argv[1], sys.argv[2]
    t0 = time.time()
    nach_sorte = lies(quelle)
    with open(ziel, "wb") as aus:
        for s in sorted(nach_sorte):
            wege = nach_sorte[s]
            punkte_vorher = sum(len(w) for w in wege)
            ketten = verkette(wege)
            laengen = sorted(len(k) for k in ketten)
            geschrieben = 0
            for kette in ketten:
                if len(kette) >= 2:
                    geschrieben += schreibe(aus, SORTEN[s], LINIE, kette)
            print("%-14s %7d Stuecke -> %6d Ketten  "
                  "(laengste %d Punkte, Mitte %d)  %d -> %d Stuetzpunkte"
                  % (SORTEN[s], len(wege), len(ketten), laengen[-1],
                     laengen[len(laengen) // 2], punkte_vorher, geschrieben))
    print("Ziel:  %s (%.1f MB)" % (ziel, os.path.getsize(ziel) / 1e6))
    print("Dauer: %.0f s" % (time.time() - t0))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
