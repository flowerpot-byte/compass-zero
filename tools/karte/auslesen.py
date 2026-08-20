# -*- coding: utf-8 -*-
"""Zweiter Messgang: die echten Geometrien der sechs Sorten herausschreiben.

Der erste Messgang (zaehlen.py) hat nur Stuetzpunkte gezaehlt und dabei alle
Flaechen uebersehen, die in OSM als Relation zusammengesetzt sind -- also
gerade die grossen Seen und Waelder. Hier werden die Flaechen richtig
zusammengebaut und mit ihren Koordinaten geschrieben.

Ergebnis ist eine kompakte Zwischendatei, aus der jede weitere Messung liest,
ohne die 806 MB Rohdaten noch einmal anzufassen. Ein Durchgang ueber die
Rohdaten dauert Minuten; die Messungen danach sollen Sekunden dauern, damit
man mehrere Zoomtiefen wirklich vergleicht statt eine zu waehlen und sie zu
begruenden.

Satzaufbau, ohne Polster:
    uint8   Sorte
    uint8   Art      0 Linie, 1 Aussenring, 2 Punkt, 3 Innenring
    uint32  Anzahl Stuetzpunkte
    int32   Laenge je Stuetzpunkt, in Zehnmillionstel Grad
    int32   Breite je Stuetzpunkt

Aufruf:  python tools/karte/auslesen.py work/karte/austria-latest.osm.pbf \
                                        work/karte/austria.geom
         python tools/karte/auslesen.py work/karte/europe-latest.osm.pbf \
                                        work/karte/europe.geom \
                                        --index work/karte/knoten.idx

WANN --index NOETIG IST: Um aus einem Weg eine Linie zu machen, braucht der
Leser die Koordinate jedes Knotens, auf den der Weg zeigt. Standardmaessig
haelt er alle im Arbeitsspeicher. Fuer Oesterreich sind das 1,5 GB und damit
kein Thema; Europa hat rund das Vierzigfache an Knoten, und dann ist der
Speicher zu Ende -- nach Stunden, mitten im Lauf, ohne Ergebnis. Mit --index
liegt die Tabelle auf der Platte (`sparse_file_array`: je gespeichertem Knoten
zwoelf Bytes, also fuer Europa grob 30 GB) und der Speicherbedarf bleibt
klein.
"""
import os
import struct
import sys
import time

import osmium

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sorten

# Die Reihenfolge ist die Zahl im Dateiformat. Sie darf WACHSEN, aber nie
# umsortiert werden -- sonst zeigt eine aeltere Karte Wald, wo Wasser ist.
# "grenze" und "grenze-region" sind am 04.08.2026 hinten angehaengt worden.
SORTEN = ["wasser", "fluss", "bach", "wald", "offen", "sumpf", "gletscher",
          "siedlung", "weg-haupt", "weg-neben", "weg-pfad", "weg-fein",
          "punkt", "ort", "grenze", "grenze-region"]
SORTE_NR = {name: i for i, name in enumerate(SORTEN)}

LINIE, AUSSEN, PUNKT, INNEN = 0, 1, 2, 3

KOPF = struct.Struct("<BBI")
PUNKTKOPF = struct.Struct("<BH")

# Laenger als das braucht kein Ortsname auf einer Karte, und die Grenze haelt
# einen kaputten Datensatz davon ab, eine Kachel aufzublaehen.
NAME_MAX = 60


def schreibe(datei, sorte, art, punkte, punktart=0, name=""):
    """Schreibt einen Satz. Punkte tragen zusaetzlich Art und Namen."""
    if len(punkte) < 1:
        return 0
    datei.write(KOPF.pack(SORTE_NR[sorte], art, len(punkte)))
    roh = bytearray(len(punkte) * 8)
    hin = struct.pack_into
    for i, (x, y) in enumerate(punkte):
        hin("<ii", roh, i * 8, x, y)
    datei.write(roh)
    if art == PUNKT:
        rohname = name.strip()[:NAME_MAX].encode("utf-8")
        datei.write(PUNKTKOPF.pack(punktart, len(rohname)))
        datei.write(rohname)
    return len(punkte)


def ring_punkte(ring):
    aus = []
    for k in ring:
        try:
            aus.append((int(round(k.lon * 1e7)), int(round(k.lat * 1e7))))
        except osmium.InvalidLocationError:
            return None
    return aus


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 1
    quelle, ziel = sys.argv[1], sys.argv[2]

    # Der Knotenspeicher: im Arbeitsspeicher (Vorgabe) oder auf der Platte.
    speicher = "flex_mem"
    if "--index" in sys.argv:
        pfad = sys.argv[sys.argv.index("--index") + 1]
        os.makedirs(os.path.dirname(os.path.abspath(pfad)), exist_ok=True)
        # Eine alte Indexdatei ist schlimmer als keine: libosmium haengt an,
        # und dann stehen Koordinaten eines frueheren Laufs darin.
        if os.path.exists(pfad):
            os.remove(pfad)
        speicher = "sparse_file_array," + pfad
    print("Knotenspeicher: %s" % speicher)
    sys.stdout.flush()

    zahl = {s: 0 for s in SORTEN}
    punkte = {s: 0 for s in SORTEN}
    verworfen = 0
    t0 = time.time()

    with open(ziel, "wb") as aus:
        lauf = osmium.FileProcessor(quelle).with_locations(speicher).with_areas()
        for obj in lauf:
            art = obj.type_str()

            if art == "a":
                s = sorten.sorte_flaeche(dict(obj.tags))
                if not s:
                    continue
                gut = False
                for aussen in obj.outer_rings():
                    p = ring_punkte(aussen)
                    if p is None:
                        continue
                    punkte[s] += schreibe(aus, s, AUSSEN, p)
                    gut = True
                    for innen in obj.inner_rings(aussen):
                        q = ring_punkte(innen)
                        if q is not None:
                            punkte[s] += schreibe(aus, s, INNEN, q)
                if gut:
                    zahl[s] += 1
                else:
                    verworfen += 1

            elif art == "w":
                tags = dict(obj.tags)
                # Ein Weg kann zwei Sorten zugleich sein -- siehe sorte_linie.
                sorten_hier = [x for x in (sorten.sorte_linie(tags),
                                           sorten.sorte_grenze(tags)) if x]
                if not sorten_hier:
                    continue
                p = ring_punkte(obj.nodes)
                if p is None:
                    verworfen += 1
                    continue
                for s in sorten_hier:
                    zahl[s] += 1
                    punkte[s] += schreibe(aus, s, LINIE, p)

            elif art == "n":
                if len(obj.tags) == 0:
                    continue
                tags = dict(obj.tags)
                s, punktart = sorten.sorte_punkt(tags)
                if not s:
                    continue
                zahl[s] += 1
                punkte[s] += schreibe(
                    aus, s, PUNKT,
                    [(int(round(obj.location.lon * 1e7)),
                      int(round(obj.location.lat * 1e7)))],
                    sorten.ART_NR[punktart],
                    tags.get("name", ""))

    dauer = time.time() - t0
    print("Zwischendatei: %s" % ziel)
    print("Groesse:       %s Bytes (%.1f MB)"
          % (f"{os.path.getsize(ziel):,}".replace(",", " "),
             os.path.getsize(ziel) / 1e6))
    print("Dauer:         %.1f s" % dauer)
    print("Unvollstaendig und deshalb weggelassen: %d" % verworfen)
    print()
    print("%-12s %12s %14s" % ("Sorte", "Objekte", "Stuetzpunkte"))
    for s in SORTEN:
        print("%-12s %12s %14s"
              % (s, f"{zahl[s]:,}".replace(",", " "),
                 f"{punkte[s]:,}".replace(",", " ")))
    print("%-12s %12s %14s"
          % ("SUMME", f"{sum(zahl.values()):,}".replace(",", " "),
             f"{sum(punkte.values()):,}".replace(",", " ")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
