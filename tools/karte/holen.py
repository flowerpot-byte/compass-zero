# -*- coding: utf-8 -*-
"""Holt einen OSM-Auszug von Geofabrik -- fortsetzbar.

WARUM FORTSETZBAR: Europa sind 34,7 GB. Bei einer knappen halben Stunde
Uebertragung je Viertel ist ein Abbruch keine Ausnahme, sondern zu erwarten.
Ohne Fortsetzen faengt jeder Netzhaenger von vorn an, und der zweite Versuch
scheitert genauso wahrscheinlich wie der erste.

Geprueft wird am Ende die Laenge gegen die Angabe des Servers. Eine zu kurze
Datei ist schlimmer als gar keine: Der PBF-Leser liest sie bis zum Bruch
anstandslos und liefert eine halbe Karte, der man nichts ansieht.

Aufruf:
    python tools/karte/holen.py europe-latest.osm.pbf work/karte/europe.osm.pbf
    python tools/karte/holen.py europe/austria-latest.osm.pbf work/karte/at.pbf
"""
import os
import sys
import time

import certifi
import requests

WURZEL = "https://download.geofabrik.de/"

KOPFZEILEN = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "Accept": "*/*",
    "Accept-Language": "de,en;q=0.8",
}


def menschlich(bytes_):
    for einheit in ("B", "kB", "MB", "GB"):
        if bytes_ < 1024 or einheit == "GB":
            return "%.1f %s" % (bytes_, einheit)
        bytes_ /= 1024.0
    return str(bytes_)


def hole(pfad, ziel):
    adresse = WURZEL + pfad
    kopf = requests.head(adresse, headers=KOPFZEILEN, timeout=60,
                         verify=certifi.where(), allow_redirects=True)
    kopf.raise_for_status()
    gesamt = int(kopf.headers.get("Content-Length", 0))
    stand = kopf.headers.get("Last-Modified", "unbekannt")
    print("Quelle:  %s" % adresse)
    print("Groesse: %s Bytes (%s), Stand %s"
          % (f"{gesamt:,}".replace(",", " "), menschlich(gesamt), stand))

    os.makedirs(os.path.dirname(os.path.abspath(ziel)), exist_ok=True)
    schon = os.path.getsize(ziel) if os.path.exists(ziel) else 0
    if schon and schon == gesamt:
        print("Schon vollstaendig da, nichts zu tun.")
        return 0
    if schon > gesamt:
        print("Vorhandene Datei ist GROESSER als die Quelle -- sie wird ersetzt.")
        os.remove(ziel)
        schon = 0

    versuche = 0
    t0 = time.time()
    while schon < gesamt:
        kopfzeilen = dict(KOPFZEILEN)
        if schon:
            kopfzeilen["Range"] = "bytes=%d-" % schon
            print("Setze fort ab %s (%.1f %%)" % (menschlich(schon), 100.0 * schon / gesamt))
        try:
            with requests.get(adresse, headers=kopfzeilen, timeout=120,
                              verify=certifi.where(), stream=True) as antwort:
                if schon and antwort.status_code != 206:
                    # Der Server kann kein Fortsetzen. Dann von vorn, aber
                    # nicht stillschweigend -- sonst haengt eine halbe Datei
                    # an einer ganzen.
                    print("Server setzt nicht fort (%d) -- beginne neu."
                          % antwort.status_code)
                    antwort.close()
                    if os.path.exists(ziel):
                        os.remove(ziel)
                    schon = 0
                    continue
                antwort.raise_for_status()
                letzte = time.time()
                with open(ziel, "ab" if schon else "wb") as datei:
                    for brocken in antwort.iter_content(1024 * 1024):
                        datei.write(brocken)
                        schon += len(brocken)
                        if time.time() - letzte > 60:
                            letzte = time.time()
                            verbraucht = time.time() - t0
                            print("  %s von %s (%.1f %%), %.1f MB/s"
                                  % (menschlich(schon), menschlich(gesamt),
                                     100.0 * schon / gesamt,
                                     schon / 1e6 / max(verbraucht, 1)))
                            sys.stdout.flush()
        except (requests.RequestException, OSError) as fehler:
            versuche += 1
            if versuche > 20:
                print("ABBRUCH nach 20 Versuchen: %s" % fehler)
                return 1
            print("Fehler (%s) -- Versuch %d, warte 15 s" % (fehler, versuche))
            time.sleep(15)
            schon = os.path.getsize(ziel) if os.path.exists(ziel) else 0

    endgroesse = os.path.getsize(ziel)
    print("Fertig: %s Bytes in %.0f s" % (f"{endgroesse:,}".replace(",", " "),
                                          time.time() - t0))
    if endgroesse != gesamt:
        print("ACHTUNG: %d Bytes erwartet, %d bekommen -- Datei ist unbrauchbar."
              % (gesamt, endgroesse))
        return 1
    return 0


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 1
    return hole(sys.argv[1], sys.argv[2])


if __name__ == "__main__":
    sys.exit(main())
