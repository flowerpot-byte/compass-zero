# -*- coding: utf-8 -*-
"""Sieht nach, wie Verwaltungsgrenzen in den Rohdaten stehen.

Es gibt zwei Wege, und sie schliessen einander nicht aus:

  (a) Der Weg selbst traegt `boundary=administrative` samt `admin_level`.
  (b) Nur die Relation traegt es, die Mitglieds-Wege sind blank.

Fuer die Karte macht das einen Unterschied, den man nicht raten sollte: Nimmt
man die Relationen, bekommt man jede Grenze DOPPELT -- die deutsch-
oesterreichische Grenze gehoert zur Aussenlinie beider Laender. Nimmt man die
Wege und sie sind blank, bekommt man gar nichts.

Aufruf:  python tools/karte/grenzen_zaehlen.py work/karte/austria-latest.osm.pbf
"""
import collections
import sys
import time

import osmium


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1

    wege = collections.Counter()
    relationen = collections.Counter()
    wege_punkte = collections.Counter()
    blanke_mitglieder = 0

    t0 = time.time()
    for obj in osmium.FileProcessor(sys.argv[1]):
        art = obj.type_str()
        if art == "w":
            tags = dict(obj.tags)
            if tags.get("boundary") == "administrative":
                stufe = tags.get("admin_level", "?")
                wege[stufe] += 1
                wege_punkte[stufe] += len(obj.nodes)
        elif art == "r":
            tags = dict(obj.tags)
            if tags.get("boundary") == "administrative":
                stufe = tags.get("admin_level", "?")
                relationen[stufe] += 1
                relationen[stufe + "-typ:" + tags.get("type", "?")] += 1
                blanke_mitglieder += len(obj.members)

    print("Dauer: %.0f s" % (time.time() - t0))
    print()
    print("WEGE mit boundary=administrative")
    for stufe in sorted(wege, key=lambda s: (len(s), s)):
        print("  admin_level %-3s %8d Wege %12d Stuetzpunkte"
              % (stufe, wege[stufe], wege_punkte[stufe]))
    if not wege:
        print("  keine -- die Wege sind blank, es geht nur ueber die Relationen")
    print()
    print("RELATIONEN mit boundary=administrative")
    for schluessel in sorted(relationen):
        print("  %-20s %8d" % (schluessel, relationen[schluessel]))
    print()
    print("Mitglieder in diesen Relationen insgesamt: %d" % blanke_mitglieder)
    return 0


if __name__ == "__main__":
    sys.exit(main())
