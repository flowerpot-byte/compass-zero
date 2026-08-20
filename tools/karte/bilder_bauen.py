# -*- coding: utf-8 -*-
"""Baut aus Sentinel-2-Aufnahmen eine Satellitenbild-Datei `.czb`.

Der Aufbau der Datei steht in `docs/BILD-FORMAT.md`. Hier steht, wie sie
entsteht: im offenen Katalog die wolkenaermsten Aufnahmen fuer das Gebiet
suchen, ihr Echtfarbenbild kachelweise ins Kartenraster umrechnen und jede
Kachel als JPEG ablegen.

WOHER DIE BILDER KOMMEN UND WARUM GERADE VON DORT: Der Rechtshinweis zu
Copernicus Sentinel gewaehrt ausdruecklich Vervielfaeltigung, Verbreitung und
Bearbeitung und verlangt dafuer einen Satz -- "Contains modified Copernicus
Sentinel data [Jahr]". Der Katalog von Element 84 liefert die Aufnahmen ohne
Konto und ohne Schluessel; das Copernicus Data Space Ecosystem verlangt
dagegen eine Anmeldung, und ein Bauwerkzeug, das ein Konto braucht, kann
niemand nachvollziehen, der das Projekt nachbaut. Fertig entwoelkte Mosaike
(EOX cloudless) waeren bequemer und sind nicht verwendbar: nicht-kommerziell
nur unter CC BY-NC-SA, Sub-Lizenzieren untersagt. Einzelheiten in
`work/quellen/satellit/LIZENZ.md`.

NICHTS DAVON LAEUFT IM GERAET. Das hier ist ein Bauwerkzeug fuer den Rechner;
die App selbst hat keinen Netzzugang und bekommt keinen.

Aufruf:
    python tools/karte/bilder_bauen.py --gebiet 12.6 47.4 13.6 48.0 \
        --zoom 9 13 --aus work/karte/salzburg.czb
    python tools/karte/bilder_bauen.py --gebiet ... --nur-suchen
"""
import argparse
import io
import json
import math
import os
import struct
import sys
import time
import urllib.request

os.environ.setdefault("GDAL_DISABLE_READDIR_ON_OPEN", "EMPTY_DIR")
os.environ.setdefault("CPL_VSIL_CURL_ALLOWED_EXTENSIONS", ".tif")
os.environ.setdefault("GDAL_HTTP_MAX_RETRY", "5")
os.environ.setdefault("GDAL_HTTP_RETRY_DELAY", "2")

import numpy as np
import rasterio
from PIL import Image
from rasterio.enums import Resampling
from rasterio.transform import from_bounds
from rasterio.vrt import WarpedVRT

KATALOG = "https://earth-search.aws.element84.com/v1/search"
SAMMLUNG = "sentinel-2-c1-l2a"

KENNUNG = b"CZBILD01"
FASSUNG = 1
KACHELKANTE = 256          # Bildpunkte je Kachelkante
KANTE_POTENZ = 8           # 2 hoch 8
KOPF_BYTES = 48
EINTRAG_BYTES = 21

# Ueber dieser Wolkendecke wird eine Aufnahme gar nicht erst betrachtet. Ein
# Satellitenbild voller Wolken ist im Gelaende schlimmer als keines: Es sieht
# aus wie Schnee oder wie eine helle Flaeche und fuehrt in die Irre.
WOLKEN_HOECHSTENS = 8.0


def kachel_x(lon, z):
    return int((lon + 180.0) / 360.0 * (1 << z))


def kachel_y(lat, z):
    r = math.radians(lat)
    return int((1.0 - math.asinh(math.tan(r)) / math.pi) / 2.0 * (1 << z))


def kachel_grenzen(x, y, z):
    """Die Kachel in Web-Mercator-Metern, so wie GDAL sie erwartet."""
    umfang = 20037508.342789244
    kante = 2 * umfang / (1 << z)
    links = -umfang + x * kante
    oben = umfang - y * kante
    return links, oben - kante, links + kante, oben


def suche(gebiet, von, bis, hoechstens, je_blatt=3):
    anfrage = {
        "collections": [SAMMLUNG],
        "bbox": list(gebiet),
        "datetime": f"{von}T00:00:00Z/{bis}T00:00:00Z",
        "query": {"eo:cloud_cover": {"lt": hoechstens}},
        "limit": 100,
    }
    r = urllib.request.Request(
        KATALOG,
        data=json.dumps(anfrage).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(r, timeout=120) as a:
        d = json.load(a)
    treffer = []
    for f in d.get("features", []):
        bild = f["assets"].get("visual") or f["assets"].get("TCI")
        if not bild:
            continue
        treffer.append(
            {
                "id": f["id"],
                "wolken": f["properties"].get("eo:cloud_cover", 100.0),
                "datum": f["properties"].get("datetime", "")[:10],
                "url": bild["href"],
            }
        )
    # JE KARTENBLATT NUR WENIGE AUFNAHMEN.
    #
    # Der Katalog liefert fuer einen Sommer leicht 25 Aufnahmen desselben
    # Gebiets. Alle zu benutzen macht das Bild nicht besser -- sie zeigen
    # dieselbe Landschaft --, kostet aber je Kachel einen Netzzugriff pro
    # Aufnahme. Gemessen am 18.08.2026: 15 Sekunden je Kachel mit 25
    # Aufnahmen. Sentinel-2 liefert in festen Kartenblaettern (T33UUP,
    # T32TQT ...); von jedem werden die wolkenaermsten genommen.
    #
    # DER PREIS DAVON IST SICHTBAR und gehoert gesagt: Nachbarblaetter
    # stammen dann aus verschiedenen Wochen, und an ihrer Naht springt die
    # Farbe des Waldes. Fuer eine Karte, auf der man sich zurechtfindet, ist
    # das hinnehmbar; fuer ein schoenes Bild waere es das nicht.
    nach_blatt = {}
    for s in treffer:
        blatt = s["id"].split("_")[1] if "_" in s["id"] else s["id"]
        nach_blatt.setdefault(blatt, []).append(s)

    # JE BLATT MEHRERE AUFNAHMEN, UND WARUM DAS SEIN MUSS: Eine Aufnahme
    # deckt ihr Kartenblatt nicht immer ganz ab -- der Satellit zieht einen
    # Streifen, und dessen Kante laeuft schraeg durch das Blatt. Wo sie
    # verlaeuft, steht in der Aufnahme nichts, und wenn nur diese eine
    # Aufnahme des Blattes benutzt wird, bleibt dort ein schwarzer Keil.
    #
    # Gemessen am 19.08.2026 an der fertigen Deutschland-Datei: 3984 von
    # 49 561 Kacheln auf Zoom 13 hatten schwarze Stellen -- acht Prozent, und
    # ueber Hamburg mitten in der Stadt.
    #
    # Die naechstbesten Aufnahmen desselben Blattes kommen deshalb als
    # NACHRUECKER mit. Sie kosten fast nichts, weil sie erst gelesen werden,
    # wenn nach der besten noch Loecher offen sind -- siehe kachel_bild.
    gewaehlt = []
    for blatt, liste in nach_blatt.items():
        liste.sort(key=lambda s: s["wolken"])
        for rang, s in enumerate(liste[:je_blatt]):
            s["rang"] = rang
            gewaehlt.append(s)
    # Innerhalb einer Stufe die Wolkenaermste zuletzt: Sie wird als Letzte in
    # die Kachel kopiert und ueberschreibt, was truebere dort gelassen haben.
    gewaehlt.sort(key=lambda s: (s["rang"], -s["wolken"]))
    return gewaehlt


def baue(gebiet, zoom_von, zoom_bis, ziel, aufnahmen, fortschritt=True):
    west, sued, ost, nord = gebiet
    verzeichnis = []
    inhalte = []

    # Die Aufnahmen werden EINMAL geoeffnet und fuer alle Kacheln
    # wiederverwendet. Jede Kachel neu zu oeffnen hiesse, den Kopf der Datei
    # ueber das Netz noch einmal zu holen -- bei tausend Kacheln ist das der
    # Unterschied zwischen Minuten und Stunden.
    offen = []
    for a in aufnahmen:
        try:
            quelle = rasterio.open("/vsicurl/" + a["url"])
            # Die Ausdehnung EINMAL in Mercator-Meter umrechnen. Damit laesst
            # sich vor jeder Kachel in einer Zeile entscheiden, ob diese
            # Aufnahme ueberhaupt in Frage kommt -- ohne das wuerde fuer jede
            # Kachel jede Aufnahme angefasst, und das ist der Unterschied
            # zwischen Minuten und einem Nachmittag.
            with WarpedVRT(quelle, crs="EPSG:3857") as v:
                rand = v.bounds
            offen.append((a, quelle, rand))
        except Exception as fehler:
            print(f"  Aufnahme {a['id']} nicht lesbar: {fehler}")
    if not offen:
        sys.exit("Keine einzige Aufnahme liess sich oeffnen.")

    # NUR DIE FEINSTE STUFE KOMMT AUS DER QUELLE, alle groeberen entstehen
    # daraus durch Halbieren.
    #
    # Der naheliegende Weg -- jede Stufe einzeln aus dem Originalbild lesen --
    # war am 18.08.2026 gemessen unbrauchbar: vier Kacheln auf Zoom 11
    # brauchten 417 Sekunden, weil GDAL fuer jede grobe Kachel eine riesige
    # Flaeche des 10-Meter-Bildes ueber das Netz zieht. Aus vier fertigen
    # Kacheln eine grobe zu rechnen kostet dagegen nichts und sieht besser
    # aus: Was zusammengefasst wird, ist schon geglaettet.
    leer = 0
    ebene = {}
    x0, x1 = kachel_x(west, zoom_bis), kachel_x(ost, zoom_bis)
    y0, y1 = kachel_y(nord, zoom_bis), kachel_y(sued, zoom_bis)
    gesamt = (x1 - x0 + 1) * (y1 - y0 + 1)
    begonnen = time.time()
    for i, x in enumerate(range(x0, x1 + 1)):
        for y in range(y0, y1 + 1):
            bild = kachel_bild(offen, x, y, zoom_bis)
            if bild is None:
                leer += 1
                continue
            ebene[(x, y)] = bild
        if fortschritt:
            dauer = time.time() - begonnen
            je = dauer / max(1, len(ebene))
            rest = je * (gesamt - (i + 1) * (y1 - y0 + 1))
            print(f"  Zoom {zoom_bis}: {len(ebene)} von {gesamt} Kacheln, "
                  f"{dauer:5.0f} s, noch etwa {rest / 60:4.0f} min",
                  end="\r", flush=True)
    if fortschritt:
        print(f"  Zoom {zoom_bis}: {len(ebene)} von {gesamt} Kacheln aus der "
              f"Quelle, {time.time() - begonnen:.0f} s" + " " * 20)

    for z in range(zoom_bis, zoom_von - 1, -1):
        for (x, y), bild in sorted(ebene.items()):
            puffer = io.BytesIO()
            # Qualitaet 82: darueber waechst die Datei schneller als das Bild
            # besser wird, darunter franst die Waldkante aus.
            bild.save(puffer, format="JPEG", quality=82, optimize=True)
            roh = puffer.getvalue()
            verzeichnis.append((z, x, y, len(roh)))
            inhalte.append(roh)
        if z > zoom_von:
            ebene = halbiere(ebene)
            if fortschritt:
                print(f"  Zoom {z - 1}: {len(ebene)} Kacheln aus der Stufe darueber")

    schreibe(ziel, gebiet, zoom_von, zoom_bis, verzeichnis, inhalte, aufnahmen)
    return len(verzeichnis), leer


def halbiere(ebene):
    """Fasst je vier Kacheln zu einer der naechstgroeberen Stufe zusammen."""
    grob = {}
    for (x, y) in ebene:
        grob.setdefault((x // 2, y // 2), True)
    aus = {}
    for (gx, gy) in grob:
        blatt = Image.new("RGB", (KACHELKANTE, KACHELKANTE), (0, 0, 0))
        halb = KACHELKANTE // 2
        for dx in (0, 1):
            for dy in (0, 1):
                kind = ebene.get((gx * 2 + dx, gy * 2 + dy))
                if kind is None:
                    continue
                blatt.paste(
                    kind.resize((halb, halb), Image.LANCZOS),
                    (dx * halb, dy * halb),
                )
        aus[(gx, gy)] = blatt
    return aus


def kachel_bild(offen, x, y, z):
    """Rechnet eine Kachel aus den Aufnahmen zusammen, oder None wenn leer.

    DAS VRT WIRD AUF DIE KACHEL GELEGT, statt aus einem grossen VRT ein
    Fenster zu schneiden. Der zweite Weg lag naeher und geht nicht: Ein
    WarpedVRT erlaubt keine Lesung ueber seinen Rand hinaus, und genau das
    braeuchte jede Kachel am Rand einer Aufnahme. Der Fehler kam als
    Ausnahme, wurde verschluckt, und heraus kamen lauter leere Kacheln --
    am 18.08.2026 erst beim Nachsehen bemerkt, weil das Werkzeug dabei
    keinen Ton von sich gab.
    """
    links, unten, rechts, oben = kachel_grenzen(x, y, z)
    ziel = np.zeros((3, KACHELKANTE, KACHELKANTE), dtype=np.uint8)
    getroffen = False
    # IN STUFEN, und das ist der Punkt: Zuerst nur die beste Aufnahme jedes
    # Blattes (Stufe 0). Bleibt danach ein Loch -- weil die Schwadkante durch
    # die Kachel laeuft --, ruecken die naechstbesten nach. Fuer neun von zehn
    # Kacheln bleibt es bei Stufe 0, und nur die uebrigen kosten zusaetzliche
    # Netzzugriffe. Ohne diese Aufteilung waere jede Kachel dreimal so teuer.
    stufen = sorted({a.get("rang", 0) for a, _q, _r in offen})
    for stufe in stufen:
        if getroffen and (ziel.max(axis=0) > 0).all():
            break            # nichts mehr offen
        for _a, quelle, rand in [e for e in offen if e[0].get("rang", 0) == stufe]:
            _kachel_stufe(_a, quelle, rand, links, unten, rechts, oben,
                          ziel, stufe)
            getroffen = getroffen or ziel.max() > 0
    if not getroffen or ziel.max() == 0:
        return None
    return Image.fromarray(np.transpose(ziel, (1, 2, 0)), mode="RGB")


def _kachel_stufe(a, quelle, rand, links, unten, rechts, oben, ziel, stufe):
    """Liest EINE Aufnahme in die Kachel, so weit sie dort etwas beitraegt."""
    # Beruehrt diese Aufnahme die Kachel ueberhaupt?
    if rand.right <= links or rand.left >= rechts:
        return
    if rand.top <= unten or rand.bottom >= oben:
        return
    try:
        with WarpedVRT(
            quelle,
            crs="EPSG:3857",
            transform=from_bounds(links, unten, rechts, oben,
                                  KACHELKANTE, KACHELKANTE),
            width=KACHELKANTE,
            height=KACHELKANTE,
            resampling=Resampling.bilinear,
        ) as vrt:
            teil = vrt.read(indexes=[1, 2, 3])
    except Exception as fehler:
        print(f"    Aufnahme {a['id']}: {fehler}")
        return
    # Schwarz heisst "hier steht nichts" -- ausserhalb der Aufnahme, und
    # bei Sentinel-2 auch in den Randstreifen. Nur belegte Bildpunkte
    # werden uebernommen, damit eine Aufnahme die andere nicht mit ihrem
    # schwarzen Rand zudeckt.
    belegt = teil.max(axis=0) > 0
    if not belegt.any():
        return
    if stufe == 0:
        ziel[:, belegt] = teil[:, belegt]
    else:
        # NACHRUECKER FUELLEN NUR LOECHER. Sie duerfen nicht ueberschreiben,
        # was die beste Aufnahme schon gesetzt hat -- sonst zieht eine
        # truebere Aufnahme ihre Wolken ueber ein sauberes Bild.
        luecke = belegt & (ziel.max(axis=0) == 0)
        if not luecke.any():
            return
        ziel[:, luecke] = teil[:, luecke]


def schreibe(ziel, gebiet, zoom_von, zoom_bis, verzeichnis, inhalte, aufnahmen):
    west, sued, ost, nord = gebiet
    daten = sorted(a["datum"] for a in aufnahmen if a["datum"])
    von = int(daten[0].replace("-", "")) if daten else 0
    bis = int(daten[-1].replace("-", "")) if daten else 0

    # AUFSTEIGEND NACH (ZOOM, X, Y) -- daran haengt die binaere Suche im
    # Leser. Gebaut wird von der feinsten Stufe abwaerts, also kommen die
    # Eintraege in genau der falschen Reihenfolge herein. Am 18.08.2026 hat
    # das der Gegencheck gefunden, der eine wirklich gebaute Datei mit dem
    # Leser oeffnet; auf einem unsortierten Verzeichnis findet eine binaere
    # Suche nicht etwa nichts, sondern die falsche Kachel -- ein Bild an der
    # falschen Stelle im Gelaende.
    zusammen = sorted(zip(verzeichnis, inhalte), key=lambda p: (p[0][0], p[0][1], p[0][2]))
    verzeichnis = [v for v, _ in zusammen]
    inhalte = [i for _, i in zusammen]

    anfang = KOPF_BYTES + EINTRAG_BYTES * len(verzeichnis)
    kopf = bytearray(KOPF_BYTES)
    kopf[0:8] = KENNUNG
    kopf[8] = FASSUNG
    kopf[9] = KANTE_POTENZ
    kopf[10] = zoom_von
    kopf[11] = zoom_bis
    struct.pack_into(
        "<iiiiIQII",
        kopf,
        12,
        int(round(west * 1e7)),
        int(round(sued * 1e7)),
        int(round(ost * 1e7)),
        int(round(nord * 1e7)),
        len(verzeichnis),
        anfang,
        von,
        bis,
    )

    with open(ziel, "wb") as f:
        f.write(kopf)
        versatz = anfang
        for (z, x, y, laenge) in verzeichnis:
            f.write(struct.pack("<BIIQI", z, x, y, versatz, laenge))
            versatz += laenge
        for roh in inhalte:
            f.write(roh)


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--gebiet", nargs=4, type=float, required=True,
                   metavar=("WEST", "SUED", "OST", "NORD"))
    p.add_argument("--zoom", nargs=2, type=int, default=[9, 13])
    p.add_argument("--von", default="2025-05-01")
    p.add_argument("--bis", default="2025-09-30")
    p.add_argument("--wolken", type=float, default=WOLKEN_HOECHSTENS)
    p.add_argument("--aus", default="work/karte/satellit.czb")
    p.add_argument("--nur-suchen", action="store_true")
    p.add_argument("--je-blatt", type=int, default=3,
                   help="Aufnahmen je Kartenblatt; die zweite und dritte "
                        "fuellen nur Loecher an der Schwadkante")
    a = p.parse_args()

    if a.zoom[1] > 14:
        sys.exit(
            "Zoom 14 ist die Obergrenze.\n\n"
            "Nachgerechnet bei 48 Grad Nord: Zoom 13 zeigt 12,8 Meter je "
            "Bildpunkt, Zoom 14 zeigt 6,4 -- und Sentinel-2 liefert 10. Auf "
            "Zoom 13 wird also ein Fuenftel der vorhandenen Aufloesung "
            "weggeworfen, auf Zoom 14 ist alles drin, was in den Daten "
            "steckt. Ab Zoom 15 (3,2 Meter) wird nur noch vergroessert: "
            "vierfache Datei, kein einziger neuer Bildpunkt."
        )

    print(f"Suche Aufnahmen fuer {a.gebiet}, {a.von} bis {a.bis}, "
          f"unter {a.wolken} Prozent Wolken ...")
    treffer = suche(a.gebiet, a.von, a.bis, a.wolken, a.je_blatt)
    if not treffer:
        sys.exit("Keine Aufnahme gefunden. Zeitraum weiten oder Wolkengrenze anheben.")
    print(f"{len(treffer)} Aufnahmen:")
    for s in sorted(treffer, key=lambda s: s["wolken"])[:12]:
        print(f"  {s['datum']}  {s['wolken']:5.1f} % Wolken  {s['id']}")
    if a.nur_suchen:
        return

    os.makedirs(os.path.dirname(a.aus) or ".", exist_ok=True)
    begonnen = time.time()
    anzahl, leer = baue(a.gebiet, a.zoom[0], a.zoom[1], a.aus, treffer)
    groesse = os.path.getsize(a.aus)
    print(f"\n{a.aus}: {anzahl} Kacheln, {groesse / 1e6:.1f} MB, "
          f"{leer} leere uebersprungen, {time.time() - begonnen:.0f} s")
    print("Namensnennung, die mitgehen muss: "
          "Contains modified Copernicus Sentinel data "
          f"{sorted(s['datum'] for s in treffer)[0][:4]}")


if __name__ == "__main__":
    main()
