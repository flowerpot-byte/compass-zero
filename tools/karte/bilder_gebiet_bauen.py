# -*- coding: utf-8 -*-
"""Baut ein grosses Gebiet in Teilstuecken und fuegt sie zusammen.

WARUM NICHT EIN LAUF: `bilder_bauen.py` haelt die feinste Zoomstufe
VOLLSTAENDIG im Speicher, weil es daraus die groeberen Stufen mittelt. Fuer
Salzburg auf Zoom 14 sind das rund 1900 Kacheln und etwa 370 MB -- geht. Fuer
Deutschland auf Zoom 13 waeren es 61 000 Kacheln und ueber 12 GB. Der Rechner
faengt an zu tauschen, und nach Stunden ist nichts fertig.

Dieses Werkzeug zerlegt das Gebiet deshalb in ein Gitter, baut jedes Feld
einzeln und fuegt am Ende alles mit `bilder_zusammenfuegen.py` zu einer Datei.

DER ZWEITE GEWINN IST WICHTIGER ALS DER ERSTE: Es laesst sich fortsetzen. Ein
fertiges Feld wird beim naechsten Lauf uebersprungen. Bei einem Lauf ueber
viele Stunden ist das der Unterschied zwischen "eine Stunde verloren" und
"alles verloren" -- und ueber Nacht bricht immer irgendwann etwas ab.

Aufruf:
    python tools/karte/bilder_gebiet_bauen.py --gebiet 5.8 47.2 15.1 55.1 \\
        --zoom 10 13 --gitter 6 5 --name deutschland \\
        --aus work/karte/deutschland-bilder.czb
    ... --nur-rechnen     zeigt nur, wie viele Felder und Kacheln es waeren
"""
import argparse
import math
import os
import subprocess
import sys
import time

HIER = os.path.dirname(os.path.abspath(__file__))
BAUEN = os.path.join(HIER, "bilder_bauen.py")
FUEGEN = os.path.join(HIER, "bilder_zusammenfuegen.py")


def kachel_x(lon, z):
    return int((lon + 180.0) / 360.0 * (1 << z))


def kachel_y(lat, z):
    r = math.radians(lat)
    return int((1.0 - math.asinh(math.tan(r)) / math.pi) / 2.0 * (1 << z))


def kachelzahl(gebiet, z):
    w, s, o, n = gebiet
    return ((kachel_x(o, z) - kachel_x(w, z) + 1)
            * (kachel_y(s, z) - kachel_y(n, z) + 1))


def felder(gebiet, spalten, zeilen):
    """Zerlegt das Gebiet in ein Gitter, von Nord nach Sued und West nach Ost."""
    w, s, o, n = gebiet
    breite = (o - w) / spalten
    hoehe = (n - s) / zeilen
    aus = []
    for zeile in range(zeilen):
        for spalte in range(spalten):
            aus.append((
                round(w + spalte * breite, 6),
                round(n - (zeile + 1) * hoehe, 6),
                round(w + (spalte + 1) * breite, 6),
                round(n - zeile * hoehe, 6),
            ))
    return aus


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--gebiet", nargs=4, type=float, required=True,
                   metavar=("WEST", "SUED", "OST", "NORD"))
    p.add_argument("--zoom", nargs=2, type=int, required=True)
    p.add_argument("--gitter", nargs=2, type=int, default=[4, 4],
                   metavar=("SPALTEN", "ZEILEN"))
    p.add_argument("--name", required=True, help="Namensteil der Teildateien")
    p.add_argument("--aus", required=True)
    p.add_argument("--teile", default="work/karte/bildteile")
    p.add_argument("--von", default="2025-05-01")
    p.add_argument("--bis", default="2025-09-30")
    p.add_argument("--wolken", type=float, default=8.0)
    p.add_argument("--nur-rechnen", action="store_true")
    a = p.parse_args()

    stuecke = felder(a.gebiet, a.gitter[0], a.gitter[1])
    gesamt = kachelzahl(a.gebiet, a.zoom[1])
    groesstes = max(kachelzahl(f, a.zoom[1]) for f in stuecke)

    print("Gebiet %s, Zoom %d bis %d" % (a.gebiet, a.zoom[0], a.zoom[1]))
    print("%d Felder (%d x %d)" % (len(stuecke), a.gitter[0], a.gitter[1]))
    print("etwa %d Kacheln auf der feinsten Stufe, groesstes Feld %d"
          % (gesamt, groesstes))
    # 196 kB je Kachel im Speicher: 256 mal 256 Bildpunkte mal 3 Farben.
    print("groesstes Feld braucht etwa %.1f GB Arbeitsspeicher"
          % (groesstes * 196608 / 1024 ** 3))
    print("und etwa %.0f MB auf der Platte (14 kB je Kachel, alle Stufen)"
          % (gesamt * 1.34 * 14 / 1024))
    if a.nur_rechnen:
        return

    os.makedirs(a.teile, exist_ok=True)
    fertige = []
    begonnen = time.time()
    for i, feld in enumerate(stuecke, 1):
        ziel = os.path.join(a.teile, "%s-%02d.czb" % (a.name, i))
        if os.path.isfile(ziel) and os.path.getsize(ziel) > 0:
            print("[%d/%d] %s liegt schon vor" % (i, len(stuecke),
                                                  os.path.basename(ziel)))
            fertige.append(ziel)
            continue
        print("[%d/%d] %s  %s" % (i, len(stuecke), os.path.basename(ziel), feld))
        sys.stdout.flush()
        befehl = [sys.executable, BAUEN,
                  "--gebiet", *[str(v) for v in feld],
                  "--zoom", str(a.zoom[0]), str(a.zoom[1]),
                  "--von", a.von, "--bis", a.bis,
                  "--wolken", str(a.wolken),
                  "--aus", ziel]
        ergebnis = subprocess.run(befehl)
        if ergebnis.returncode != 0 or not os.path.isfile(ziel):
            # NICHT ABBRECHEN. Ein Feld ohne wolkenfreie Aufnahme oder mit
            # einem Netzaussetzer darf nicht das ganze Land kosten; beim
            # naechsten Lauf wird genau dieses Feld noch einmal versucht.
            print("    -> kein Ergebnis, wird uebersprungen")
            continue
        fertige.append(ziel)
        vergangen = time.time() - begonnen
        print("    fertig nach %.0f min, im Schnitt %.1f min je Feld"
              % (vergangen / 60, vergangen / 60 / i))

    if not fertige:
        sys.exit("Kein einziges Feld gebaut.")
    print("\n%d von %d Feldern gebaut, jetzt zusammenfuegen ..."
          % (len(fertige), len(stuecke)))
    subprocess.run([sys.executable, FUEGEN, *fertige, "--aus", a.aus], check=True)


if __name__ == "__main__":
    main()
