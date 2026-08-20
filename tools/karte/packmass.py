# -*- coding: utf-8 -*-
"""Dritter Messgang: Wie viele Bytes kostet die Karte wirklich?

Hier steht der Entwurf des eigenen Kartenformats -- nicht als Beschreibung,
sondern als Rechnung. Gemessen wird je Zoomstufe:

  1. Wie viele Objekte und Stuetzpunkte auf dieser Stufe ueberhaupt gezeigt
     werden (nicht alles gehoert auf jede Stufe -- ein Waldweg auf der
     Europakarte macht sie schwarz und nuetzt niemandem).
  2. Wie viele Stuetzpunkte das Raster der Stufe uebrig laesst. Wer auf ein
     Gitter rundet, dem fallen benachbarte Punkte von selbst zusammen; das ist
     die staerkste Vereinfachung und kostet keine Rechenzeit im Geraet.
  3. Wie viele Bytes die Zickzack-Varint-Kodierung der Abstaende braucht.
     Die Laenge eines Varint haengt nur am Betrag, deshalb ist sie ausrechenbar
     und muss nicht erzeugt werden -- das Ergebnis ist exakt, nicht geschaetzt.
  4. Was Deflate davon noch wegnimmt, gemessen an einer echten Stichprobe.

WARUM NICHT GESCHAETZT WIRD: Die Roadmap sagt es fuer diesen Punkt
ausdruecklich -- eine Zahl aus einer Messung ist mehr wert als eine aus einer
Ueberlegung. Und die Frage, die daran haengt (passt Europa in ein paar hundert
Megabyte?), traegt die ganze Kartenarbeit.

Aufruf:  python tools/karte/packmass.py work/karte/austria.geom
"""
import os
import struct
import sys
import time
import zlib

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from auslesen import SORTEN, LINIE, AUSSEN, PUNKT, INNEN

# Kachelraster: 4096 Einheiten je Kachelkante. Bei 256 Bildpunkten Anzeige
# sind das 16 Einheiten je Bildpunkt -- fein genug, dass das Runden nie
# sichtbar wird, und grob genug, dass die meisten Abstaende in ein einziges
# Byte passen.
RASTER = 4096

# Ab dieser Ausdehnung lohnt ein Objekt auf einer Stufe. 16 Einheiten sind
# genau ein Bildpunkt: was kleiner ist, waere ein Fleck ohne Aussage.
MINDESTMASS = 16

# Welche Sorte ab welcher Zoomstufe gezeigt wird.
#
# Das ist die eigentliche Entwurfsentscheidung dieser Datei. Der Grundsatz:
# Auf jeder Stufe steht das, wonach man sich auf DIESER Stufe richtet. Auf der
# Uebersicht sind das Kueste, Fluesse, Gebirge und Staedte; unterwegs sind es
# Baeche, Pfade und Quellen.
AB_ZOOM = {
    "wasser": 4,
    "fluss": 5,
    "bach": 11,
    "gletscher": 6,
    "wald": 7,
    "sumpf": 9,
    "offen": 11,
    "siedlung": 7,
    "weg-haupt": 6,
    "weg-neben": 10,
    "weg-pfad": 11,
    "weg-fein": 99,      # nie: Gehsteige und Zufahrten fuehren nirgendwohin
    "punkt": 10,
    "ort": 4,
}

KOPF = struct.Struct("<BBI")

# Punktsaetze tragen seit der Erweiterung um Ortsnamen zwei Bytes Punktart und
# Namenslaenge hinter den Koordinaten, dann den Namen. Wer die ueberliest,
# faengt den naechsten Satz mitten im Namen an -- das Feld verschiebt sich um
# ein paar Bytes und die Datei sieht danach aus wie Muell, ohne dass ein
# Fehler faellt. `bauen.py` kennt diese Bytes laengst; hier fehlten sie.
PUNKTKOPF = struct.Struct("<BH")
PUNKT_ART = 2


def lies(pfad):
    """Liest die Zwischendatei in flache Felder.

    Flach heisst: alle Koordinaten in EINEM Feld, dazu ein Feld mit den
    Anfaengen. So laesst sich jede spaetere Rechnung ueber alle 60 Millionen
    Punkte auf einmal machen statt in einer Schleife ueber vier Millionen
    Objekte -- der Unterschied ist Minuten gegen Stunden.
    """
    roh = np.fromfile(pfad, dtype=np.uint8)
    sorte, art, anfang, laenge = [], [], [], []
    x, y = [], []
    i = 0
    n = len(roh)
    while i < n:
        s, a, k = KOPF.unpack_from(roh, i)
        i += KOPF.size
        werte = roh[i:i + k * 8].view(np.int32)
        x.append(werte[0::2])
        y.append(werte[1::2])
        sorte.append(s)
        art.append(a)
        laenge.append(k)
        i += k * 8
        if a == PUNKT_ART:
            _, namenslaenge = PUNKTKOPF.unpack_from(roh, i)
            i += PUNKTKOPF.size + namenslaenge
    anfang = np.concatenate([[0], np.cumsum(laenge)])[:-1]
    return (np.array(sorte, dtype=np.uint8),
            np.array(art, dtype=np.uint8),
            np.array(anfang, dtype=np.int64),
            np.array(laenge, dtype=np.int64),
            np.concatenate(x), np.concatenate(y))


def gitter(lon7, lat7, zoom):
    """Rechnet Grad in Rasterpunkte der Zoomstufe um (Web-Mercator)."""
    breite = 2 ** zoom * RASTER
    lon = lon7.astype(np.float64) / 1e7
    lat = np.clip(lat7.astype(np.float64) / 1e7, -85.05112878, 85.05112878)
    gx = (lon + 180.0) / 360.0 * breite
    s = np.sin(np.radians(lat))
    gy = (0.5 - np.log((1 + s) / (1 - s)) / (4 * np.pi)) * breite
    return np.round(gx).astype(np.int64), np.round(gy).astype(np.int64)


def varint_bytes(werte):
    """Laenge des Zickzack-Varint je Wert -- ausgerechnet, nicht erzeugt."""
    zz = np.abs(werte).astype(np.uint64) * 2
    laenge = np.ones(len(zz), dtype=np.int64)
    grenze = np.uint64(128)
    for _ in range(4):
        laenge += (zz >= grenze)
        grenze = grenze * np.uint64(128)
    return laenge


def varint(ziel, wert):
    while wert >= 128:
        ziel.append((wert & 127) | 128)
        wert >>= 7
    ziel.append(wert)


def zigzag(ziel, wert):
    varint(ziel, (wert << 1) ^ (wert >> 63))


def messe_stufe(daten, zoom, stichprobe=400):
    sorte, art, anfang, laenge, lon7, lat7 = daten

    sichtbar = np.zeros(len(sorte), dtype=bool)
    for name, ab in AB_ZOOM.items():
        if zoom >= ab:
            sichtbar |= (sorte == SORTEN.index(name))
    if not sichtbar.any():
        return None

    gx, gy = gitter(lon7, lat7, zoom)

    # Erster Punkt jedes Objekts -- dort bricht die Abstandskette ab.
    ist_erster = np.zeros(len(gx), dtype=bool)
    ist_erster[anfang] = True

    dx = np.empty_like(gx)
    dy = np.empty_like(gy)
    dx[1:] = gx[1:] - gx[:-1]
    dy[1:] = gy[1:] - gy[:-1]
    dx[0] = gx[0]
    dy[0] = gy[0]
    # Der erste Punkt eines Objekts wird gegen seine Kachelecke geschrieben,
    # nicht gegen den letzten Punkt des vorigen Objekts.
    dx[ist_erster] = gx[ist_erster] % RASTER
    dy[ist_erster] = gy[ist_erster] % RASTER

    # Auf dem Raster zusammengefallene Punkte fallen weg. Der erste bleibt
    # immer stehen, sonst verliert das Objekt seinen Anker.
    bleibt = (dx != 0) | (dy != 0) | ist_erster

    obj_punkte = np.add.reduceat(bleibt.astype(np.int64), anfang)

    # Ausdehnung je Objekt, um zu Kleines wegzulassen.
    maxx = np.maximum.reduceat(gx, anfang)
    minx = np.minimum.reduceat(gx, anfang)
    maxy = np.maximum.reduceat(gy, anfang)
    miny = np.minimum.reduceat(gy, anfang)
    gross_genug = ((maxx - minx) >= MINDESTMASS) | ((maxy - miny) >= MINDESTMASS)
    ist_punkt = (art == PUNKT)
    ist_ring = (art == AUSSEN) | (art == INNEN)
    genug_punkte = np.where(ist_ring, obj_punkte >= 4,
                            np.where(ist_punkt, obj_punkte >= 1, obj_punkte >= 2))

    nimm = sichtbar & genug_punkte & (gross_genug | ist_punkt)

    behalten = np.repeat(nimm, laenge) & bleibt
    stuetz = int(behalten.sum())
    if stuetz == 0:
        return None

    # Bytes: Abstaende plus je Objekt ein Kopf (Art und Punktzahl).
    bytes_geom = int(varint_bytes(dx[behalten]).sum() +
                     varint_bytes(dy[behalten]).sum())
    bytes_kopf = int(3 * nimm.sum())

    # Kachelwechsel: ein Objekt, das ueber eine Kachelgrenze laeuft, wird dort
    # geteilt. Jede Teilung kostet einen neuen Kopf und einen Randpunkt.
    kachel = (gx // RASTER) * 100000 + (gy // RASTER)
    wechsel = np.zeros(len(kachel), dtype=bool)
    wechsel[1:] = kachel[1:] != kachel[:-1]
    wechsel[anfang] = False
    schnitte = int((wechsel & np.repeat(nimm, laenge)).sum())
    bytes_schnitt = schnitte * 7

    roh = bytes_geom + bytes_kopf + bytes_schnitt

    # Deflate an einer echten Stichprobe messen statt einen Faktor zu raten.
    # Die Stichprobe wird wirklich kodiert -- ein Feld aus Ganzzahlen fester
    # Breite liesse sich viel besser packen als Varints und wuerde die Quote
    # schoenrechnen.
    ausgewaehlt = np.flatnonzero(nimm)
    schritt = max(1, len(ausgewaehlt) // stichprobe)
    probe = bytearray()
    for i in ausgewaehlt[::schritt][:stichprobe]:
        a, l = int(anfang[i]), int(laenge[i])
        m = bleibt[a:a + l]
        probe.append(int(art[i]))
        varint(probe, int(m.sum()))
        for wx, wy in zip(dx[a:a + l][m], dy[a:a + l][m]):
            zigzag(probe, int(wx))
            zigzag(probe, int(wy))
    quote = len(zlib.compress(bytes(probe), 6)) / max(1, len(probe))

    return {
        "zoom": zoom,
        "objekte": int(nimm.sum()),
        "stuetzpunkte": stuetz,
        "roh": roh,
        "gepackt": int(roh * quote),
        "quote": quote,
        "schnitte": schnitte,
    }


# Sorten, die in einem gueltigen Auszug ganz fehlen duerfen. Die Pruefung
# unten sucht abgebrochene Zwischendateien; eine leere Sorte ist dafuer nur
# dann ein Zeichen, wenn es sie ueberall gibt.
#   weg-fein   wird von `sorten.py` nur unter Bedingungen vergeben.
#   gletscher  hat kein flaches Land.
#   grenze-region gibt es nur, wo jemand die Landesgrenzen an die WEGE
#                 geschrieben hat. Montenegro hat davon null -- gemessen am
#                 20.08.2026 an einem vollstaendigen, gebauten und signierten
#                 Auszug. Vorher hat diese Pruefung genau daran falschen
#                 Alarm geschlagen und die Messung verweigert.
DARF_LEER_SEIN = {"weg-fein", "gletscher", "grenze-region"}


def zwischendatei_taugt(daten):
    """Sagt laut, WAS gemessen wird -- und bricht ab, wenn Sorten fehlen.

    WARUM ES DIESE PRUEFUNG GIBT: Am 04.08.2026 lief die Messung auf einer
    Zwischendatei, die noch geschrieben wurde und erst die Punkte enthielt.
    Sie lief sauber durch und gab plausible Zahlen aus -- 2,6 MB fuer
    Oesterreich. Der einzige sichtbare Hinweis war, dass alle elf Zoomstufen
    dieselbe Objektzahl meldeten; eine Karte, bei der Stufe 4 und Stufe 14
    gleich viel enthalten, kann es nicht geben. Auf so einen Blick darf sich
    keine Zahl verlassen, die eine Bauentscheidung traegt.
    """
    sorte, art, _, laenge, _, _ = daten
    print()
    print("%-12s %12s %14s  %s" % ("Sorte", "Objekte", "Stuetzpunkte", "Arten"))
    fehlend = []
    namen = {LINIE: "Linie", AUSSEN: "Aussenring", PUNKT: "Punkt", INNEN: "Innenring"}
    for i, name in enumerate(SORTEN):
        treffer = (sorte == i)
        n = int(treffer.sum())
        arten = sorted({namen[int(a)] for a in np.unique(art[treffer])})
        print("%-12s %12s %14s  %s"
              % (name, f"{n:,}".replace(",", " "),
                 f"{int(laenge[treffer].sum()):,}".replace(",", " "),
                 ", ".join(arten) if arten else "--"))
        if n == 0 and name not in DARF_LEER_SEIN:
            fehlend.append(name)
    if fehlend:
        print()
        print("ABBRUCH: keine Objekte der Sorte(n) %s. Die Zwischendatei ist "
              "unvollstaendig -- vermutlich wird sie noch geschrieben."
              % ", ".join(fehlend))
        return False
    return True


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    pfad = sys.argv[1]
    t0 = time.time()
    daten = lies(pfad)
    print("Zwischendatei gelesen: %s Objekte, %s Stuetzpunkte, %.1f s"
          % (f"{len(daten[0]):,}".replace(",", " "),
             f"{len(daten[4]):,}".replace(",", " "), time.time() - t0))
    if not zwischendatei_taugt(daten):
        return 1
    print()
    print("%4s %12s %14s %12s %12s %7s" %
          ("Zoom", "Objekte", "Stuetzpunkte", "roh MB", "gepackt MB", "Quote"))
    summe = 0
    zeilen = []
    for z in range(4, 15):
        e = messe_stufe(daten, z)
        if not e:
            continue
        zeilen.append(e)
        summe += e["gepackt"]
        print("%4d %12s %14s %12.2f %12.2f %6.2f"
              % (e["zoom"],
                 f"{e['objekte']:,}".replace(",", " "),
                 f"{e['stuetzpunkte']:,}".replace(",", " "),
                 e["roh"] / 1e6, e["gepackt"] / 1e6, e["quote"]))
    print()
    ueber = sum(e["gepackt"] for e in zeilen if e["zoom"] <= 10)
    detail = sum(e["gepackt"] for e in zeilen if e["zoom"] >= 11)
    print("Uebersicht z4-z10:  %8.1f MB" % (ueber / 1e6))
    print("Detail     z11-z14: %8.1f MB" % (detail / 1e6))
    print("Zusammen:           %8.1f MB" % (summe / 1e6))
    return 0


if __name__ == "__main__":
    sys.exit(main())
