# -*- coding: utf-8 -*-
"""Fuegt mehrere Satellitenbild-Dateien `.czb` zu einer einzigen zusammen.

WARUM ES DAS BRAUCHT: Die App oeffnet genau EINE `.czb` -- anders als bei den
Karten, wo sie mehrere Dateien uebereinanderlegt. Wer feine Bilder ueber der
eigenen Gegend und grobe ueber dem ganzen Erdteil haben will, kann also nicht
drei Dateien nebeneinanderlegen; er braucht eine, die alles enthaelt. Das
Dateiformat kann das ohne Weiteres: Sein Verzeichnis ist nach (Zoom, x, y)
sortiert und sagt nirgends, dass alle Stufen dieselbe Flaeche abdecken muessen.

WARUM NICHT EINFACH EIN GROSSER LAUF: `bilder_bauen.py` haelt die feinste
Zoomstufe vollstaendig im Speicher, um daraus die groeberen zu mitteln. Bei
einem Land sind das Zehntausende Kacheln und mehrere Gigabyte. In Teilstuecken
zu bauen und hinterher zusammenzufuegen ist deshalb nicht nur bequemer, es ist
der einzige Weg, der auf einem gewoehnlichen Rechner durchlaeuft -- und ein
abgebrochener Teillauf kostet nur sein eigenes Stueck, nicht alles.

BEI DOPPELTEN KACHELN GEWINNT DIE ZUERST GENANNTE DATEI. Das ist Absicht und
die Reihenfolge damit eine Entscheidung: Wer

    bilder_zusammenfuegen.py fein.czb grob.czb --aus alles.czb

schreibt, bekommt in der Ueberlappung die feine Aufnahme.

Aufruf:
    python tools/karte/bilder_zusammenfuegen.py A.czb B.czb C.czb --aus alles.czb
    python tools/karte/bilder_zusammenfuegen.py *.czb --aus alles.czb --zeigen
"""
import argparse
import os
import struct
import sys

KENNUNG = b"CZBILD01"
FASSUNG = 1
KOPF_BYTES = 48
EINTRAG_BYTES = 21
UMSCHLAG_KENNUNG = b"CZB1"
UMSCHLAG_BYTES = 110


def anfang_der_datei(f):
    """0 ohne Umschlag, sonst hinter ihm.

    Eine unterschriebene Datei traegt den Umschlag davor; ihre Versaetze zaehlen
    ab dem Inhalt. Wer das vergisst, liest um 110 Bytes verschoben.
    """
    f.seek(0)
    return UMSCHLAG_BYTES if f.read(4) == UMSCHLAG_KENNUNG else 0


def lies(pfad):
    """Gibt (Kopfangaben, Liste von (z, x, y, rohbytes)) zurueck."""
    with open(pfad, "rb") as f:
        v0 = anfang_der_datei(f)
        f.seek(v0)
        kopf = f.read(KOPF_BYTES)
        if kopf[:8] != KENNUNG:
            sys.exit(f"{pfad} ist keine .czb-Datei")
        if kopf[8] != FASSUNG:
            sys.exit(f"{pfad} hat Fassung {kopf[8]}, erwartet {FASSUNG}")
        kante_potenz = kopf[9]
        west, sued, ost, nord = struct.unpack_from("<iiii", kopf, 12)
        anzahl = struct.unpack_from("<i", kopf, 28)[0]
        von, bis = struct.unpack_from("<ii", kopf, 40)
        verzeichnis = f.read(EINTRAG_BYTES * anzahl)
        if len(verzeichnis) != EINTRAG_BYTES * anzahl:
            sys.exit(f"{pfad}: Verzeichnis ist kuerzer als angekuendigt")

        kacheln = []
        for i in range(anzahl):
            p = i * EINTRAG_BYTES
            z = verzeichnis[p]
            x, y = struct.unpack_from("<ii", verzeichnis, p + 1)
            versatz = struct.unpack_from("<q", verzeichnis, p + 9)[0]
            laenge = struct.unpack_from("<i", verzeichnis, p + 17)[0]
            f.seek(v0 + versatz)
            roh = f.read(laenge)
            if len(roh) != laenge:
                sys.exit(f"{pfad}: Kachel {z}/{x}/{y} ist abgeschnitten")
            kacheln.append((z, x, y, roh))
    return (kante_potenz, west, sued, ost, nord, von, bis), kacheln


def schreibe(ziel, kante_potenz, rahmen, aufnahmen, kacheln):
    west, sued, ost, nord = rahmen
    von, bis = aufnahmen
    # AUFSTEIGEND SORTIEREN, und zwar nach demselben Schluessel, den der Leser
    # bildet: (z << 56) | (x << 28) | y. Der Leser sucht binaer und weist eine
    # unsortierte Datei ab -- am 18.08.2026 hat genau das den Sortierfehler im
    # Bauwerkzeug gefunden.
    kacheln.sort(key=lambda k: (k[0] << 56) | (k[1] << 28) | k[2])

    anfang = KOPF_BYTES + EINTRAG_BYTES * len(kacheln)
    zoomstufen = [k[0] for k in kacheln]
    kopf = bytearray(KOPF_BYTES)
    kopf[0:8] = KENNUNG
    kopf[8] = FASSUNG
    kopf[9] = kante_potenz
    kopf[10] = min(zoomstufen)
    kopf[11] = max(zoomstufen)
    struct.pack_into("<iiii", kopf, 12, west, sued, ost, nord)
    struct.pack_into("<i", kopf, 28, len(kacheln))
    struct.pack_into("<q", kopf, 32, anfang)
    struct.pack_into("<ii", kopf, 40, von, bis)

    with open(ziel, "wb") as f:
        f.write(kopf)
        stelle = anfang
        for z, x, y, roh in kacheln:
            eintrag = bytearray(EINTRAG_BYTES)
            eintrag[0] = z
            struct.pack_into("<ii", eintrag, 1, x, y)
            struct.pack_into("<q", eintrag, 9, stelle)
            struct.pack_into("<i", eintrag, 17, len(roh))
            f.write(eintrag)
            stelle += len(roh)
        for _, _, _, roh in kacheln:
            f.write(roh)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("dateien", nargs="+", help="Quelldateien, feinste zuerst")
    p.add_argument("--aus", required=True)
    p.add_argument("--zeigen", action="store_true",
                   help="nur berichten, was zusammenkaeme, nichts schreiben")
    a = p.parse_args()

    zusammen = {}
    kante_potenz = None
    west = sued = ost = nord = None
    von = bis = 0
    doppelt = 0

    for pfad in a.dateien:
        if not os.path.isfile(pfad):
            sys.exit(f"Datei fehlt: {pfad}")
        angaben, kacheln = lies(pfad)
        kp, w, s, o, n, v, b = angaben
        if kante_potenz is None:
            kante_potenz = kp
        elif kp != kante_potenz:
            sys.exit(f"{pfad} hat Kachelkante 2^{kp}, die erste 2^{kante_potenz} "
                     "-- das laesst sich nicht mischen")

        west = w if west is None else min(west, w)
        sued = s if sued is None else min(sued, s)
        ost = o if ost is None else max(ost, o)
        nord = n if nord is None else max(nord, n)
        # Aufnahmezeitraum ueber alle Teile: frueheste und spaeteste.
        if v:
            von = v if not von else min(von, v)
        if b:
            bis = max(bis, b)

        neu = 0
        for z, x, y, roh in kacheln:
            schluessel = (z, x, y)
            if schluessel in zusammen:
                doppelt += 1
                continue
            zusammen[schluessel] = roh
            neu += 1
        print("%-42s %7d Kacheln, davon %7d neu" % (os.path.basename(pfad),
                                                    len(kacheln), neu))

    if not zusammen:
        sys.exit("Nichts zusammenzufuegen.")

    nach_zoom = {}
    umfang = 0
    for (z, x, y), roh in zusammen.items():
        nach_zoom[z] = nach_zoom.get(z, 0) + 1
        umfang += len(roh)
    print()
    for z in sorted(nach_zoom):
        print("   Zoom %2d: %7d Kacheln" % (z, nach_zoom[z]))
    print("   zusammen %d Kacheln, %.1f MB Bilddaten, %d doppelte uebergangen"
          % (len(zusammen), umfang / 1024 / 1024, doppelt))

    if a.zeigen:
        return

    kacheln = [(z, x, y, roh) for (z, x, y), roh in zusammen.items()]
    schreibe(a.aus, kante_potenz, (west, sued, ost, nord), (von, bis), kacheln)
    print("\n%s: %d Kacheln, %.1f MB"
          % (a.aus, len(kacheln), os.path.getsize(a.aus) / 1024 / 1024))


if __name__ == "__main__":
    main()
