# -*- coding: utf-8 -*-
"""Holt Copernicus-GLO-90-Kacheln fuer ein Rechteck.

QUELLE UND LIZENZ: Copernicus DEM GLO-90, bereitgestellt von der ESA. Der
Lizenztext liegt unter `work/karte/quellen/copdem-lizenz.pdf` und ist am
04.08.2026 im Volltext gelesen worden. Artikel 4 gewaehrt Vervielfaeltigung,
Verbreitung, oeffentliche Wiedergabe und Bearbeitung; Artikel 5 kostenlos;
Artikel 3 weltweit und unbefristet. Artikel 6 verlangt dafuer diesen
Hinweis, woertlich, bei jeder Weitergabe:

    © DLR e.V. 2010-2014 and © Airbus Defence and Space GmbH 2014-2018
    provided under COPERNICUS by the European Union and ESA;
    all rights reserved.

und bei bearbeiteten Daten -- also bei allem, was dieses Werkzeug erzeugt:

    produced using Copernicus WorldDEM-30 © DLR e.V. 2010-2014 and
    © Airbus Defence and Space GmbH 2014-2018 provided under COPERNICUS by
    the European Union and ESA; all rights reserved.

**Diese Hinweise sind keine Hoeflichkeit, sondern Bedingung der Nutzung.**
Sie stehen deshalb auch in `HINWEIS.txt` neben jeder erzeugten Hoehendatei
und in der App.

Die Daten liegen als offener Datensatz bei Amazon; es braucht keine
Anmeldung und keinen Schluessel.

Aufruf:
    python tools/karte/hoehen_holen.py 46 9 50 18 work/karte/hoehen
    (Sued, West, Nord, Ost -- ganze Grad)
"""
import os
import sys
import time

import certifi
import requests

WURZEL = "https://copernicus-dem-90m.s3.amazonaws.com"

KOPFZEILEN = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "Accept": "*/*",
}

HINWEIS = (
    "Hoehendaten: produced using Copernicus WorldDEM-30\n"
    "(c) DLR e.V. 2010-2014 and (c) Airbus Defence and Space GmbH 2014-2018\n"
    "provided under COPERNICUS by the European Union and ESA; all rights reserved.\n"
    "\n"
    "Die Organisationen, die das Copernicus-Programm von Gesetzes wegen oder\n"
    "im Auftrag betreiben, haften nicht fuer Schaeden aus der Nutzung dieser\n"
    "Daten.\n"
)


def kachelname(breite, laenge):
    ns = "N" if breite >= 0 else "S"
    ew = "E" if laenge >= 0 else "W"
    return "Copernicus_DSM_COG_30_%s%02d_00_%s%03d_00_DEM" % (
        ns, abs(breite), ew, abs(laenge))


def hole(sued, west, nord, ost, ziel):
    os.makedirs(ziel, exist_ok=True)
    with open(os.path.join(ziel, "HINWEIS.txt"), "w", encoding="utf-8") as f:
        f.write(HINWEIS)

    fehlend = []
    geholt = 0
    bytes_gesamt = 0
    t0 = time.time()
    for breite in range(sued, nord):
        for laenge in range(west, ost):
            name = kachelname(breite, laenge)
            pfad = os.path.join(ziel, name + ".tif")
            if os.path.exists(pfad) and os.path.getsize(pfad) > 0:
                bytes_gesamt += os.path.getsize(pfad)
                geholt += 1
                continue
            adresse = "%s/%s/%s.tif" % (WURZEL, name, name)
            try:
                r = requests.get(adresse, headers=KOPFZEILEN, timeout=120,
                                 verify=certifi.where())
            except requests.RequestException as fehler:
                print("  %s FEHLER %s" % (name, fehler))
                fehlend.append(name)
                continue
            if r.status_code == 404:
                # Ueber offener See gibt es keine Kachel. Das ist normal und
                # kein Fehler -- Europa ist kein Rechteck.
                fehlend.append(name)
                continue
            if r.status_code != 200:
                print("  %s HTTP %d" % (name, r.status_code))
                fehlend.append(name)
                continue
            with open(pfad, "wb") as f:
                f.write(r.content)
            geholt += 1
            bytes_gesamt += len(r.content)
            print("  %s  %.1f MB" % (name, len(r.content) / 1e6))
            sys.stdout.flush()

    print()
    print("Kacheln geholt: %d, zusammen %.1f MB, %.0f s"
          % (geholt, bytes_gesamt / 1e6, time.time() - t0))
    print("Ohne Kachel (meist offene See): %d" % len(fehlend))
    return 0 if geholt else 1


def main():
    if len(sys.argv) < 6:
        print(__doc__)
        return 1
    sued, west, nord, ost = (int(x) for x in sys.argv[1:5])
    return hole(sued, west, nord, ost, sys.argv[5])


if __name__ == "__main__":
    sys.exit(main())
