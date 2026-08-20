# -*- coding: utf-8 -*-
"""Holt Staats- und Landesgrenzen ueber die RELATIONEN statt ueber die Wege.

WARUM ES DIESES WERKZEUG GIBT
-----------------------------
`auslesen.py` nimmt eine Grenze nur dann mit, wenn der WEG selbst
`boundary=administrative` und `admin_level` traegt. Fuer Oesterreich ging das
auf -- daran wurde es gemessen. Fuer Deutschland nicht: Dort ist ein Wegstueck
an der Staatsgrenze meist als Gemeindegrenze beschriftet (`admin_level=8`),
und dass es zugleich Staatsgrenze ist, steht nur in der Relation.

Das Ergebnis war auf dem Geraet sichtbar und Max hat es am 05.08.2026 gemeldet:
Auf der kleinsten Zoomstufe fehlten die Laendergrenzen. Nachgemessen in der
gebauten Kachel ueber Mitteleuropa: 303 Grenzobjekte, davon 247 kuerzer als
eine Strichperiode, Median 6 Bildpunkte von 1024. Kuesten und Skandinavien
waren durchgehend, das Binnenland war Konfetti.

Die Zaehlung ueber die Europa-Rohdaten (tools/karte/grenzen_zaehlen.py,
11 575 s) zeigt beide Wege nebeneinander:

    WEGE mit admin_level=2:        11 786
    RELATIONEN mit admin_level=2:      79   (die Laender selbst)
    RELATIONEN mit admin_level=4:     576   (Bundeslaender und dergleichen)

Die 79 Relationen sind die Wahrheit; die 11 786 Wege sind der Teil davon, den
jemand zusaetzlich beschriftet hat.

DIE FALLE, DIE DAGEGEN SPRACH -- und wie sie umgangen wird: Eine Grenze gehoert
zu ZWEI Laendern, die deutsch-oesterreichische also zur Aussenlinie beider.
Ueber die Relationen bekaeme man sie doppelt. Deshalb werden hier erst alle
Weg-Nummern in eine MENGE gesammelt und danach jeder Weg genau einmal
geschrieben. Staatsgrenze schlaegt Landesgrenze: Wo beides zusammenfaellt,
zaehlt die staerkere Auskunft.

ZWEI DURCHGAENGE, GETRENNT AUFRUFBAR
------------------------------------
Ein Durchgang durch die 34 GB dauert Stunden. Deshalb sind sie getrennt, und
der erste legt sein Ergebnis ab -- wer den zweiten wiederholen muss, muss den
ersten nicht wiederholen.

    1. python tools/karte/grenzen_holen.py relationen work/karte/europe-latest.osm.pbf \
           work/karte/grenzwege.txt
    2. python tools/karte/grenzen_holen.py wege work/karte/europe-latest.osm.pbf \
           work/karte/grenzwege.txt work/karte/grenzen.geom \
           --index work/karte/knoten.idx

Der zweite Durchgang benutzt den Knotenindex des ersten Auslesens WIEDER. Das
spart den teuersten Teil des Laufs -- aber nur, wenn man ihn richtig anfasst:

    NICHT `with_locations(...)` benutzen. Am 06.08.2026 an Oesterreich
    ausprobiert: Die Indexdatei wuchs dabei von 1,41 auf 2,80 GB, obwohl ein
    Filter alle Knoten fernhalten sollte. pyosmium haengt den Knotenspeicher
    VOR den Filter, also werden alle Knoten ein zweites Mal angehaengt. Bei
    den 60 GB des Europa-Index waeren das 60 GB Muell und ein Index, dessen
    Eintraege nicht mehr durchgehend nach Nummer sortiert sind -- die
    Suche darin ist eine binaere Suche und braucht genau das.

Deshalb wird die Tabelle hier direkt geoeffnet und mit `get()` befragt. Es
wird nie `set()` aufgerufen, also kann nichts angehaengt werden.
"""
import os
import sys
import time

import osmium

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from auslesen import LINIE, schreibe

# Welche Relationsstufe welche Sorte ergibt -- dieselbe Zuordnung wie in
# sorten.py, damit beide Wege dasselbe bedeuten.
STUFEN = {"2": "grenze", "4": "grenze-region"}


def relationen(pbf, ziel):
    """Sammelt die Weg-Nummern aller Staats- und Landesgrenzen."""
    gefunden = {"grenze": set(), "grenze-region": set()}
    t0 = time.time()
    gezaehlt = 0
    lauf = osmium.FileProcessor(pbf).with_filter(
        osmium.filter.EntityFilter(osmium.osm.RELATION))
    for obj in lauf:
        gezaehlt += 1
        tags = obj.tags
        if tags.get("boundary") != "administrative":
            continue
        sorte = STUFEN.get(tags.get("admin_level"))
        if not sorte:
            continue
        for mitglied in obj.members:
            if mitglied.type == "w":
                gefunden[sorte].add(mitglied.ref)
        if gezaehlt % 200000 == 0:
            print("  %d Relationen gesehen, %d + %d Wege gemerkt (%.0f s)"
                  % (gezaehlt, len(gefunden["grenze"]),
                     len(gefunden["grenze-region"]), time.time() - t0))
            sys.stdout.flush()

    # Staatsgrenze schlaegt Landesgrenze.
    gefunden["grenze-region"] -= gefunden["grenze"]

    with open(ziel, "w") as f:
        for sorte in ("grenze", "grenze-region"):
            for nummer in sorted(gefunden[sorte]):
                f.write("%s %d\n" % (sorte, nummer))
    print("Relationen gelesen: %d" % gezaehlt)
    print("Staatsgrenze:  %d Wege" % len(gefunden["grenze"]))
    print("Landesgrenze:  %d Wege" % len(gefunden["grenze-region"]))
    print("Liste:         %s" % ziel)
    print("Dauer:         %.0f s" % (time.time() - t0))
    return 0


def wege(pbf, liste, ziel, index):
    """Schreibt die Geometrie der gemerkten Wege in eine Zwischendatei."""
    zuordnung = {}
    with open(liste) as f:
        for zeile in f:
            sorte, nummer = zeile.split()
            zuordnung[int(nummer)] = sorte
    print("Gesuchte Wege: %d" % len(zuordnung))
    sys.stdout.flush()

    if not os.path.exists(index):
        print("Der Knotenindex fehlt: %s" % index)
        return 1
    groesse_vorher = os.path.getsize(index)
    tabelle = osmium.index.create_map("sparse_file_array," + index)

    zahl = {s: 0 for s in ("grenze", "grenze-region")}
    punkte = dict(zahl)
    fehlend = 0
    t0 = time.time()
    gesehen = 0
    with open(ziel, "wb") as aus:
        lauf = (osmium.FileProcessor(pbf)
                .with_filter(osmium.filter.EntityFilter(osmium.osm.WAY)))
        for obj in lauf:
            gesehen += 1
            if gesehen % 20000000 == 0:
                print("  %d Wege gesehen, %d Grenzstuecke geschrieben (%.0f s)"
                      % (gesehen, zahl["grenze"] + zahl["grenze-region"],
                         time.time() - t0))
                sys.stdout.flush()
            sorte = zuordnung.get(obj.id)
            if not sorte:
                continue
            p = []
            gut = True
            for knoten in obj.nodes:
                try:
                    ort = tabelle.get(knoten.ref)
                except Exception:
                    gut = False
                    break
                if not ort.valid():
                    gut = False
                    break
                p.append((int(round(ort.lon * 1e7)), int(round(ort.lat * 1e7))))
            if not gut or len(p) < 2:
                fehlend += 1
                continue
            zahl[sorte] += 1
            punkte[sorte] += schreibe(aus, sorte, LINIE, p)

    print("Zwischendatei: %s" % ziel)
    print("Groesse:       %.1f MB" % (os.path.getsize(ziel) / 1e6))
    for s in ("grenze", "grenze-region"):
        print("  %-14s %8d Wege %12d Stuetzpunkte" % (s, zahl[s], punkte[s]))
    print("Unvollstaendig und deshalb weggelassen: %d" % fehlend)
    print("Dauer:         %.0f s" % (time.time() - t0))
    # Die Probe aufs Exempel: Der Index muss auf das Byte gleich geblieben
    # sein. Waechst er, ist wieder etwas angehaengt worden, und dann ist er
    # fuer den naechsten Lauf unbrauchbar.
    nachher = os.path.getsize(index)
    print("Knotenindex:   %d Bytes vorher, %d nachher -- %s"
          % (groesse_vorher, nachher,
             "unveraendert" if nachher == groesse_vorher else "ANGEWACHSEN!"))
    return 0 if nachher == groesse_vorher else 1


def main():
    if len(sys.argv) < 4:
        print(__doc__)
        return 1
    was = sys.argv[1]
    if was == "relationen":
        return relationen(sys.argv[2], sys.argv[3])
    if was == "wege":
        if "--index" not in sys.argv:
            print("Der zweite Durchgang braucht --index.")
            return 1
        index = sys.argv[sys.argv.index("--index") + 1]
        return wege(sys.argv[2], sys.argv[3], sys.argv[4], index)
    print(__doc__)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
