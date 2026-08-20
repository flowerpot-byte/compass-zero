# -*- coding: utf-8 -*-
"""Erster Messgang: Was ueberlebt den Filter?

Die Roadmap verlangt ausdruecklich, die Pipeline an EINEM Land zu messen und
erst dann auf Europa hochzurechnen -- "eine Zahl aus einer Messung ist mehr
wert als eine aus einer Ueberlegung". Das hier ist der erste Teil davon.

Gemessen wird ohne jede Vereinfachung und ohne Dateiformat: nur, wie viele
Objekte und wie viele Stuetzpunkte von den sechs Datensorten uebrig bleiben.
Diese Zahl haengt an keiner spaeteren Entscheidung -- sie gilt fuer jedes
Format gleich und ist deshalb die belastbarste, die es hier gibt.

Aufruf:  python tools/karte/zaehlen.py work/karte/austria-latest.osm.pbf
"""
import collections
import os
import sys
import time

import osmium

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sorten


def geschlossen(weg):
    knoten = weg.nodes
    return len(knoten) > 3 and knoten[0].ref == knoten[-1].ref


def zaehle(pfad):
    objekte = collections.Counter()
    stuetzpunkte = collections.Counter()
    roh = collections.Counter()

    t0 = time.time()
    for obj in osmium.FileProcessor(pfad):
        art = obj.type_str()
        roh[art] += 1

        if art == "n":
            if len(obj.tags) == 0:
                continue
            roh["knoten-mit-tags"] += 1
            s = sorten.sorte_punkt(dict(obj.tags))
            if s:
                objekte[s] += 1
                stuetzpunkte[s] += 1

        elif art == "w":
            roh["weg-stuetzpunkte"] += len(obj.nodes)
            tags = dict(obj.tags)
            s = None
            if geschlossen(obj):
                s = sorten.sorte_flaeche(tags)
            if s is None:
                s = sorten.sorte_linie(tags)
            if s:
                objekte[s] += 1
                stuetzpunkte[s] += len(obj.nodes)

        elif art == "r":
            tags = dict(obj.tags)
            if tags.get("type") == "multipolygon":
                s = sorten.sorte_flaeche(tags)
                if s:
                    objekte[s + "-relation"] += 1

    return roh, objekte, stuetzpunkte, time.time() - t0


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    pfad = sys.argv[1]
    groesse = os.path.getsize(pfad)

    roh, objekte, stuetzpunkte, dauer = zaehle(pfad)

    print("Datei:        %s" % pfad)
    print("Groesse:      %s Bytes (%.2f GB)" % (f"{groesse:,}".replace(",", " "), groesse / 1e9))
    print("Dauer:        %.1f s" % dauer)
    print()
    print("ROHBESTAND")
    print("  Knoten:            %12s" % f"{roh['n']:,}".replace(",", " "))
    print("  davon mit Tags:    %12s" % f"{roh['knoten-mit-tags']:,}".replace(",", " "))
    print("  Wege:              %12s" % f"{roh['w']:,}".replace(",", " "))
    print("  Weg-Stuetzpunkte:  %12s" % f"{roh['weg-stuetzpunkte']:,}".replace(",", " "))
    print("  Relationen:        %12s" % f"{roh['r']:,}".replace(",", " "))
    print()
    print("AUSGEWAEHLT (die sechs Sorten, ohne Vereinfachung)")
    gesamt_obj = 0
    gesamt_pkt = 0
    for s in sorted(set(list(objekte) + list(stuetzpunkte))):
        o = objekte.get(s, 0)
        p = stuetzpunkte.get(s, 0)
        if not s.endswith("-relation"):
            gesamt_obj += o
            gesamt_pkt += p
        print("  %-16s %10s Objekte %14s Stuetzpunkte"
              % (s, f"{o:,}".replace(",", " "), f"{p:,}".replace(",", " ")))
    print()
    print("  SUMME            %10s Objekte %14s Stuetzpunkte"
          % (f"{gesamt_obj:,}".replace(",", " "), f"{gesamt_pkt:,}".replace(",", " ")))
    print()
    anteil_obj = 100.0 * gesamt_obj / max(1, roh["n"] + roh["w"])
    anteil_pkt = 100.0 * gesamt_pkt / max(1, roh["weg-stuetzpunkte"] + roh["knoten-mit-tags"])
    print("  Anteil an allen Knoten+Wegen:        %.2f %%" % anteil_obj)
    print("  Anteil an allen Stuetzpunkten:       %.2f %%" % anteil_pkt)
    return 0


if __name__ == "__main__":
    sys.exit(main())
