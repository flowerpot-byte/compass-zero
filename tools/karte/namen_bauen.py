# -*- coding: utf-8 -*-
"""Baut aus einer fertigen Kartendatei ein Namensverzeichnis `.czn`.

WARUM AUS DER KARTE UND NICHT AUS OPENSTREETMAP: Die Namen stehen laengst in
der `.czk` -- Orte, Gipfel, Quellen, Brunnen, Huetten, Krankenhaeuser,
Apotheken. Nur liegen sie je Kachel verstreut, und eine Suche muesste dafuer
die ganze Datei durchpfluegen; bei `deutschland-nord.czk` sind das 770 MB.
Aus den vorhandenen Karten zu bauen heisst ausserdem: Wer eine Karte hat,
kann sich das Verzeichnis selbst bauen, ohne irgendetwas herunterzuladen.

Der Aufbau der Datei steht in `docs/NAMEN-FORMAT.md`.

Aufruf:
    python tools/karte/namen_bauen.py work/karte/deutschland-nord.czk \
        --aus work/karte/deutschland-nord.czn
    python tools/karte/namen_bauen.py <karte> --zaehlen
"""
import argparse
import os
import struct
import sys
import unicodedata
import zlib

KENNUNG = b"CZNAME01"
FASSUNG = 1
KOPF_BYTES = 32
KARTE_KOPF = 40
KARTE_EINTRAG = 21
RASTER = 4096

SORTE_PUNKT = 12
SORTE_ORT = 13

# Dieselbe Reihenfolge wie in core/karte/Kartenformat.kt. Sie ist Teil des
# Dateiformats und darf nur wachsen, nie umsortiert werden.
PUNKTARTEN = [
    "unbekannt",
    "quelle", "brunnen", "trinkwasser", "wasserturm",
    "gipfel", "sattel", "pass", "hoehle",
    "huette", "unterstand", "aussicht",
    "krankenhaus", "apotheke", "notruftelefon",
    "grossstadt", "stadt", "dorf", "weiler", "einzellage",
]


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


def zigzag(w):
    return (w >> 1) ^ -(w & 1)


def schreibe_varint(aus, wert):
    while True:
        b = wert & 0x7F
        wert >>= 7
        if wert:
            aus.append(b | 0x80)
        else:
            aus.append(b)
            return


def buchstabe_oder_ziffer(zeichen):
    """Was der Leser als Buchstabe oder Ziffer gelten laesst.

    NICHT str.isalnum(). Python nimmt dort auch Zeichen wie "3" hoch --
    U+00B3, hochgestellte Drei -- an; Javas isLetterOrDigit nicht. Ein
    Schluessel mit so einem Zeichen steht dann in der Datei, und keine
    Anfrage bildet ihn je nach: Der Leser macht daraus ein Leerzeichen.
    Gefunden am 18.08.2026 an "Pharma-Ko3" (hochgestellt) im Namensverzeichnis
    fuer Norddeutschland -- packsign hat sich geweigert, es zu unterschreiben.
    """
    art = unicodedata.category(zeichen)
    return art.startswith("L") or art == "Nd"


def ist_steuerzeichen(zeichen):
    """Javas Character.isISOControl: 0x00-0x1F und 0x7F-0x9F."""
    n = ord(zeichen)
    return n <= 0x1F or 0x7F <= n <= 0x9F


def falte(text):
    """Der Suchschluessel: klein, ohne Umlaute, ohne Zierde.

    ER STEHT MIT IN DER DATEI, und das ist Absicht. Wuerde ihn der Leser auf
    dem Geraet selbst bilden, muessten zwei Umsetzungen -- eine in Python,
    eine in Kotlin -- fuer alle Zeiten dasselbe tun. Laufen sie auseinander,
    findet die Suche einzelne Namen nicht mehr, und niemand merkt, welche.
    """
    ersatz = {"ä": "ae", "ö": "oe", "ü": "ue", "ß": "ss",
              "á": "a", "à": "a", "â": "a", "å": "a", "ã": "a",
              "é": "e", "è": "e", "ê": "e", "ë": "e",
              "í": "i", "ì": "i", "î": "i", "ï": "i",
              "ó": "o", "ò": "o", "ô": "o", "õ": "o", "ø": "o",
              "ú": "u", "ù": "u", "û": "u",
              "ç": "c", "ñ": "n", "š": "s", "ž": "z", "č": "c", "ř": "r"}
    aus = []
    for zeichen in text.lower():
        if zeichen in ersatz:
            aus.append(ersatz[zeichen])
            continue
        # Alles andere ohne diakritische Zeichen; was dann noch kein
        # Buchstabe oder Ziffer ist, wird zum Leerzeichen -- so findet
        # "st veit" auch "St. Veit".
        zerlegt = unicodedata.normalize("NFD", zeichen)
        blank = "".join(c for c in zerlegt
                        if unicodedata.category(c) != "Mn" and not ist_steuerzeichen(c))
        if blank and all(buchstabe_oder_ziffer(c) for c in blank):
            aus.append(blank)
        else:
            aus.append(" ")
    return " ".join("".join(aus).split())


def lies_kachel(roh):
    """Gibt (sorte, punktart, name, px, py) je benanntem Objekt."""
    i = 1
    anzahl_namen, i = varint(roh, i)
    namen = []
    for _ in range(anzahl_namen):
        laenge, i = varint(roh, i)
        namen.append(roh[i:i + laenge].decode("utf-8", "replace"))
        i += laenge
    schichten, i = varint(roh, i)
    for _ in range(schichten):
        sorte = roh[i]
        i += 1
        objekte, i = varint(roh, i)
        for _ in range(objekte):
            i += 1  # Art
            punktart, i = varint(roh, i)
            nummer, i = varint(roh, i)
            n, i = varint(roh, i)
            px = py = 0
            erstes = None
            for k in range(n):
                dx, i = varint(roh, i)
                dy, i = varint(roh, i)
                px += zigzag(dx)
                py += zigzag(dy)
                if k == 0:
                    erstes = (px, py)
            if nummer == 0 or sorte not in (SORTE_PUNKT, SORTE_ORT) or erstes is None:
                continue
            name = namen[nummer - 1] if nummer - 1 < len(namen) else None
            if name:
                yield sorte, punktart, name, erstes[0], erstes[1]


def nach_grad(kachel_x, kachel_y, zoom, px, py):
    import math
    n = 1 << zoom
    x = kachel_x + px / RASTER
    y = kachel_y + py / RASTER
    lon = x / n * 360.0 - 180.0
    lat = math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * y / n))))
    return lon, lat


# Eine unterschriebene Karte traegt einen Umschlag davor. Seine Kennung ist
# "CZK1", die der blanken Karte "CZKARTE1" -- sie unterscheiden sich erst im
# vierten Byte, deshalb wird ganz verglichen und nicht ueber einen Anfang.
UMSCHLAG_KENNUNG = b"CZK1"
UMSCHLAG_BYTES = 110


def kartenanfang(f):
    """Wo die eigentliche Karte beginnt: 0 oder hinter dem Umschlag.

    HIER WIRD KEINE UNTERSCHRIFT GEPRUEFT, und das ist Absicht. Dieses
    Werkzeug baut ein Verzeichnis aus einer Datei, die schon auf dem eigenen
    Rechner liegt; die Vertrauensfrage stellt das Geraet, wenn es die Karte
    oeffnet. Ein Werkzeug, das hier "geprueft" meldete, wuerde eine Sicherheit
    behaupten, die es nicht herstellt.
    """
    f.seek(0)
    anfang = f.read(4)
    if anfang == UMSCHLAG_KENNUNG:
        return UMSCHLAG_BYTES
    return 0


def sammle(pfad, fortschritt=True):
    gefunden = {}
    with open(pfad, "rb") as f:
        versatz0 = kartenanfang(f)
        f.seek(versatz0)
        kopf = f.read(KARTE_KOPF)
        if kopf[:8] != b"CZKARTE1":
            sys.exit("Das ist keine .czk-Datei.")
        anzahl = struct.unpack_from("<I", kopf, 28)[0]
        verzeichnis = f.read(KARTE_EINTRAG * anzahl)
        eintraege = [
            struct.unpack_from("<BIIQI", verzeichnis, k * KARTE_EINTRAG)
            for k in range(anzahl)
        ]
        # Die FEINSTE Stufe zuerst: Dort steht ein Ort an seiner genauesten
        # Stelle. Was auf einer groberen Stufe noch einmal auftaucht, ist
        # derselbe Ort mit ungenauerer Lage und wird verworfen.
        eintraege.sort(key=lambda e: -e[0])
        for nummer, (z, x, y, versatz, laenge) in enumerate(eintraege):
            f.seek(versatz0 + versatz)
            try:
                roh = zlib.decompress(f.read(laenge))
            except zlib.error:
                continue
            for sorte, punktart, name, px, py in lies_kachel(roh):
                schluessel = falte(name)
                if not schluessel:
                    continue
                art = 0 if sorte == SORTE_ORT else punktart
                kennung = (schluessel, sorte, art)
                if kennung in gefunden:
                    continue
                lon, lat = nach_grad(x, y, z, px, py)
                gefunden[kennung] = (schluessel, name, lon, lat, sorte, art)
            if fortschritt and nummer % 500 == 0:
                print(f"  {nummer} von {anzahl} Kacheln, {len(gefunden)} Namen",
                      end="\r", flush=True)
    if fortschritt:
        print(f"  {anzahl} Kacheln gelesen, {len(gefunden)} Namen" + " " * 20)
    return list(gefunden.values())


def schreibe(ziel, eintraege, grenzen):
    # NACH DEM SCHLUESSEL SORTIERT, und zwar nach seinen BYTES -- der Leser
    # vergleicht ebenfalls Bytes. Nach Zeichenketten-Regeln der jeweiligen
    # Sprache zu sortieren waere hier der Fehler: Python und Kotlin ordnen
    # dann verschieden, und die binaere Suche findet die Haelfte nicht.
    eintraege.sort(key=lambda e: (e[0].encode("utf-8"), e[1]))

    koerper = bytearray()
    versaetze = []
    for schluessel, name, lon, lat, sorte, art in eintraege:
        versaetze.append(len(koerper))
        s = schluessel.encode("utf-8")
        n = name.encode("utf-8")
        schreibe_varint(koerper, len(s))
        koerper += s
        schreibe_varint(koerper, len(n))
        koerper += n
        koerper += struct.pack("<iiBB", int(round(lon * 1e7)), int(round(lat * 1e7)),
                               sorte, min(art, 255))

    anfang = KOPF_BYTES + 4 * len(eintraege)
    kopf = bytearray(KOPF_BYTES)
    kopf[0:8] = KENNUNG
    kopf[8] = FASSUNG
    struct.pack_into("<Iiiii", kopf, 12, len(eintraege), *grenzen)

    with open(ziel, "wb") as f:
        f.write(kopf)
        for v in versaetze:
            f.write(struct.pack("<I", anfang + v))
        f.write(koerper)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("karte")
    p.add_argument("--aus")
    p.add_argument("--zaehlen", action="store_true")
    a = p.parse_args()

    with open(a.karte, "rb") as f:
        f.seek(kartenanfang(f))
        kopf = f.read(KARTE_KOPF)
    grenzen = struct.unpack_from("<iiii", kopf, 12)

    print(f"Lese {a.karte} ...")
    eintraege = sammle(a.karte)
    if not eintraege:
        sys.exit("Keine benannten Orte oder Punkte gefunden.")

    nach_art = {}
    for _s, _n, _lo, _la, sorte, art in eintraege:
        name = "Ort" if sorte == SORTE_ORT else (
            PUNKTARTEN[art] if art < len(PUNKTARTEN) else "unbekannt")
        nach_art[name] = nach_art.get(name, 0) + 1
    for name in sorted(nach_art, key=lambda k: -nach_art[k]):
        print(f"  {nach_art[name]:>7}  {name}")
    if a.zaehlen:
        return

    ziel = a.aus or os.path.splitext(a.karte)[0] + ".czn"
    schreibe(ziel, eintraege, grenzen)
    print(f"\n{ziel}: {len(eintraege)} Namen, "
          f"{os.path.getsize(ziel) / 1e6:.1f} MB")


if __name__ == "__main__":
    main()
