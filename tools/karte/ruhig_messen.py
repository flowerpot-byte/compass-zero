# -*- coding: utf-8 -*-
"""Misst, was der Schalter "Ruhig" aus der Karte nimmt.

WARUM ES DIESES SKRIPT GIBT: Am 17.08.2026 ist die Wirkung des Schalters am
Geraet gemessen worden -- bei Zoom 6. Dort gibt es die weggelassenen Sorten
gar nicht, also kam "0 Prozent" heraus, und das war keine Messung, sondern
ein Missverstaendnis. Am 18.08. dann derselbe Stolperstein noch einmal: Die
mitgelieferte Karte reicht nur bis Zoom 10, Pfade beginnen bei 11. An ihr
laesst sich der Schalter ueberhaupt nicht messen.

Deshalb wird hier direkt in einer DETAILKARTE gezaehlt, auf den Zoomstufen,
auf denen die betroffenen Sorten wirklich liegen. Gezaehlt werden Objekte und
Stuetzpunkte je Sorte -- Stuetzpunkte sind das ehrlichere Mass, weil ein
langer Fluss mehr Bild belegt als ein kurzer Feldweg.

Aufruf:
    python tools/karte/ruhig_messen.py work/karte/deutschland-sued-detail.czk
    python tools/karte/ruhig_messen.py <datei> --zoom 12 --kacheln 40

Der Aufbau der Datei steht in docs/KARTEN-FORMAT.md.
"""
import argparse
import struct
import sys
import zlib

KENNUNG = b"CZKARTE1"
KOPF = 40
EINTRAG = 21

SORTEN = {
    0: "Wasserflaeche", 1: "Fluss", 2: "Bach", 3: "Wald", 4: "Offenland",
    5: "Sumpf", 6: "Gletscher", 7: "Siedlung", 8: "Hauptstrasse",
    9: "Nebenstrasse", 10: "Pfad/Feldweg", 11: "feiner Weg", 12: "Punkt",
    13: "Ortsname", 14: "Grenze", 15: "Regionsgrenze",
}

# Was der Schalter weglaesst. WEG_PFAD (10) kam am 18.08.2026 dazu, von Max
# entschieden -- die Zahl unten sagt, was das kostet.
RUHIG_WEG = {11, 10, 2, 15}


def varint(daten, i):
    wert = 0
    schub = 0
    while True:
        b = daten[i]
        i += 1
        wert |= (b & 0x7F) << schub
        if not b & 0x80:
            return wert, i
        schub += 7


def zaehle_kachel(roh):
    """Gibt {sorte: (objekte, stuetzpunkte)} fuer eine entpackte Kachel."""
    i = 1  # Fassung des Kachelaufbaus
    anzahl_namen, i = varint(roh, i)
    for _ in range(anzahl_namen):
        laenge, i = varint(roh, i)
        i += laenge
    schichten, i = varint(roh, i)
    aus = {}
    for _ in range(schichten):
        sorte = roh[i]
        i += 1
        objekte, i = varint(roh, i)
        punkte = 0
        for _ in range(objekte):
            i += 1  # Art
            # Die Punktart steht ZWISCHEN Art und Namensnummer. Sie fehlte am
            # 18.08.2026 in docs/KARTEN-FORMAT.md; der Leser lief deshalb aus
            # dem Tritt und stuerzte am Kachelende ab.
            _punktart, i = varint(roh, i)
            _name, i = varint(roh, i)
            n, i = varint(roh, i)
            punkte += n
            for _ in range(n * 2):
                _d, i = varint(roh, i)
        o, p = aus.get(sorte, (0, 0))
        aus[sorte] = (o + objekte, p + punkte)
    return aus


def main():
    p = argparse.ArgumentParser()
    p.add_argument("datei")
    p.add_argument("--zoom", type=int, default=0, help="nur diese Stufe (0 = alle ab 11)")
    p.add_argument("--kacheln", type=int, default=60, help="wie viele Kacheln je Stufe")
    a = p.parse_args()

    with open(a.datei, "rb") as f:
        kopf = f.read(KOPF)
        if kopf[:8] != KENNUNG:
            sys.exit("Das ist keine .czk-Datei.")
        zoom_klein, zoom_gross = kopf[10], kopf[11]
        kachelzahl = struct.unpack_from("<I", kopf, 28)[0]
        print(f"{a.datei}: Zoom {zoom_klein}-{zoom_gross}, {kachelzahl} Kacheln")

        verzeichnis = f.read(EINTRAG * kachelzahl)
        eintraege = []
        for k in range(kachelzahl):
            z, x, y, versatz, laenge = struct.unpack_from("<BIIQI", verzeichnis, k * EINTRAG)
            eintraege.append((z, x, y, versatz, laenge))

        stufen = [a.zoom] if a.zoom else sorted({z for z, *_ in eintraege if z >= 11})
        if not stufen:
            sys.exit(
                "Diese Karte hat keine Stufe ab 11 -- die betroffenen Sorten gibt es "
                "dort gar nicht. An ihr ist der Schalter nicht messbar."
            )

        for stufe in stufen:
            dieser = [e for e in eintraege if e[0] == stufe]
            # Die groessten Kacheln zuerst: dort steht am meisten drin, und
            # eine leere Waldkachel sagt ueber die Wirkung nichts aus.
            dieser.sort(key=lambda e: -e[4])
            probe = dieser[: a.kacheln]
            gesamt = {}
            for _z, _x, _y, versatz, laenge in probe:
                f.seek(versatz)
                roh = zlib.decompress(f.read(laenge))
                for sorte, (o, pt) in zaehle_kachel(roh).items():
                    go, gp = gesamt.get(sorte, (0, 0))
                    gesamt[sorte] = (go + o, gp + pt)

            alle_o = sum(o for o, _ in gesamt.values())
            alle_p = sum(pt for _, pt in gesamt.values())
            weg_o = sum(o for s, (o, _) in gesamt.items() if s in RUHIG_WEG)
            weg_p = sum(pt for s, (_, pt) in gesamt.items() if s in RUHIG_WEG)
            pfad_o, pfad_p = gesamt.get(10, (0, 0))

            print(f"\nZoom {stufe}, {len(probe)} der {len(dieser)} Kacheln (die vollsten):")
            for sorte in sorted(gesamt, key=lambda s: -gesamt[s][1]):
                o, pt = gesamt[sorte]
                marke = "  <- Ruhig nimmt das weg" if sorte in RUHIG_WEG else ""
                anteil = 100.0 * pt / alle_p if alle_p else 0
                print(f"  {SORTEN.get(sorte, sorte):>14}: {o:>8} Objekte, {pt:>9} Punkte ({anteil:5.1f} %){marke}")
            if alle_p:
                print(f"  ---> Ruhig nimmt {100.0 * weg_p / alle_p:.1f} % der Stuetzpunkte "
                      f"und {100.0 * weg_o / alle_o:.1f} % der Objekte weg.")
                print(f"       Davon allein Pfade/Feldwege: {100.0 * pfad_p / alle_p:.1f} % "
                      f"der Punkte ({pfad_o} Objekte).")


if __name__ == "__main__":
    main()
