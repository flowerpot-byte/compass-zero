# -*- coding: utf-8 -*-
"""Baut ein Land am Stueck: holen, auslesen, bauen, unterschreiben, pruefen.

Die einzelnen Schritte gibt es laengst; hier stehen sie in der richtigen
Reihenfolge und mit den Zwischendateien, die sie voneinander erwarten. Nichts
davon laeuft im Geraet -- das ist ein Bauwerkzeug fuer den Rechner, wie
`bilder_bauen.py` und `wege_bauen.py` auch.

FORTSETZBAR, und das ist der Punkt. Ein fertiger Zwischenschritt wird beim
naechsten Lauf uebersprungen. Genau wie in `bilder_gebiet_bauen.py`, und aus
demselben Grund: Der Lauf dauert Stunden, und ueber Stunden bricht immer
irgendwann etwas ab -- Netz, Strom, ein zu voller Datentraeger. Ohne
Fortsetzen faengt jeder Abbruch von vorn an, und der zweite Versuch scheitert
so wahrscheinlich wie der erste.

WORAN "FERTIG" ERKANNT WIRD -- daran haengt alles: an der fertigen Datei
selbst, nicht an einer Merkdatei daneben. Merkdateien und Wirklichkeit laufen
auseinander, sobald jemand von Hand aufraeumt. Damit eine abgebrochene Datei
nicht als fertig gilt, schreibt jeder Schritt hier auf einen Namen mit der
Endung `.unfertig` und benennt erst nach dem sauberen Ende um. Eine zu kurze
Zwischendatei ist schlimmer als gar keine: Der naechste Schritt liest sie
anstandslos bis zum Bruch und liefert eine halbe Karte, der man nichts
ansieht.

DIE SCHAETZUNG (`--nur-rechnen`) IST GEMESSEN, NICHT GERATEN. Grundlage sind
fuenf Laeufe, die in `work/karte` und ihren Baulogs stehen:

    Lauf                    PBF        Zwischendatei   Ergebnis    Bauzeit
    Montenegro  Zoom 11-14    34,1 MB    34,0 MB         23,0 MB    183 s
    Oesterreich Zoom 11-14   807,8 MB   634,7 MB        345,8 MB     --
    Oesterreich Zoom  4-10   807,8 MB   634,7 MB          6,3 MB     --
    Deutschland Zoom 11-14  4821,8 MB   (aus Europa)    2028,3 MB   4161 s
    Europa      Zoom  4-10  34824,4 MB  23202,2 MB       437,5 MB   5097 s

Daraus die Zusammenhaenge, die dieses Werkzeug benutzt:

  * Zwischendatei ~ 0,85 mal die PBF-Datei. Gemessen 1,00 (Montenegro),
    0,79 (Oesterreich), 0,67 (Europa) -- je groesser der Auszug, desto besser
    schneidet die PBF-Packung ab.
  * DETAILKARTE (Zoom 11 bis 14, Toleranz 4) ~ 0,55 mal die PBF-Datei.
    Gemessen 0,676 (Montenegro), 0,428 (Oesterreich), 0,421 (Deutschland).
    Oesterreich und Deutschland liegen fast aufeinander, obwohl das eine
    sechsmal so gross ist wie das andere -- auf den feinen Stufen ueberlebt
    fast jedes Objekt, und dann haengt die Karte an der Datenmenge, nicht an
    der Flaeche. Montenegro liegt hoeher, und dazu passt, dass auch seine
    Zwischendatei am schlechtesten packt. Wer eine schaerfere Zahl braucht,
    laesst `packmass.py` ueber die fertige Zwischendatei laufen: fuer
    Montenegro sagte es 30,2 MB vorher, gebaut wurden 23,0 MB -- der Rest ist
    Douglas-Peucker, den packmass nicht rechnet.
  * UEBERSICHTSKARTE (Zoom 4 bis 10, Toleranz 16) ~ 0,010 mal die PBF-Datei
    (gemessen 0,008 und 0,013). Hier ist die Streuung gross, weil auf groben
    Stufen fast alles wegfaellt und die Kachelzahl an der Flaeche haengt statt
    an den Daten. Deshalb steht bei der Uebersicht eine Spanne im Bericht.
  * Auslesen ~ 0,8 MB der PBF-Datei je Sekunde (gemessen 1,14 / 0,95 / 0,42).
  * Bauen: das GROESSERE von beidem -- 5 s je MB fertiger Karte, oder 0,025 s
    je MB Zwischendatei und Durchgang (Durchgaenge sind die Zoomstufen plus
    einer fuer die Grenzen der Karte). Der erste Term traegt bei kleinen
    Gebieten, der zweite bei grossen, wo das blosse Lesen der Zwischendatei
    alles andere schluckt. Gegengerechnet: Europa 4640 s geschaetzt gegen
    5097 s gemessen; Montenegro 115 s gegen 183 s.
  * Holen ~ 3,0 MB je Sekunde (Europa: 34,7 GB in 9982 s; Montenegro 0,7 MB/s,
    aber bei 34 MB zaehlt vor allem der Verbindungsaufbau). Das haengt an der
    Leitung und ist die unsicherste Zahl von allen.

DIE GROESSE STIMMT BESSER ALS DIE ZEIT. Die Eingangsgroesse wird nicht
geschaetzt, sondern beim Anbieter erfragt, und die Faktoren darauf streuen um
etwa ein Viertel. Die Zeit kann um das Doppelte danebenliegen: Sie haengt
daran, was sonst noch auf dem Rechner laeuft. Beim Montenegro-Lauf am
20.08.2026 brauchte Zoomstufe 13 einmal 419 s und beim ungestoerten
Wiederholen 48 s -- dieselbe Datei, dasselbe Ergebnis auf das Byte genau.

HARTE GRENZE: `packsign karte-signieren` liest die Datei im Ganzen in den
Speicher, damit geprueft und unterschrieben dieselben Bytes sind, und weist
alles ueber 1,5 GB ab. Deutschland musste deshalb in zwei Haelften. Das
Werkzeug warnt, wenn die Schaetzung darueber liegt -- vor dem Holen, nicht
danach.

KEIN EIGENER AUSZUG: Nicht jedes Gebiet hat bei Geofabrik eine eigene Datei;
47 der 250 stecken in einem Sammelpaket. Dann bricht dieses Werkzeug ab und
sagt, in welchem -- es baut nicht die halbe Nachbarschaft unter falschem
Namen.

Aufruf:
    python tools/karte/land_bauen.py Slowenien --nur-rechnen
    python tools/karte/land_bauen.py Slowenien --zoom 11 14
    python tools/karte/land_bauen.py Montenegro --zoom 4 10 \\
        --aus work/karte/montenegro-ueberblick.czk
"""
import argparse
import os
import shutil
import subprocess
import sys
import time

import certifi
import requests

HIER = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HIER)
import laender

WURZEL = os.path.dirname(os.path.dirname(HIER))
HOLEN = os.path.join(HIER, "holen.py")
AUSLESEN = os.path.join(HIER, "auslesen.py")
BAUEN = os.path.join(HIER, "bauen.py")

GEOFABRIK = "https://download.geofabrik.de/"

# Die gemessenen Faktoren aus dem Kopf dieser Datei, an einer Stelle.
ANTEIL_ZWISCHEN = 0.85
ANTEIL_DETAIL = (0.42, 0.68)
ANTEIL_UEBERSICHT = (0.008, 0.013)
HOLEN_MB_JE_S = 3.0
AUSLESEN_MB_JE_S = 0.8
BAUEN_S_JE_MB_DURCHGANG = 0.025
BAUEN_S_JE_MB_ERGEBNIS = 5.0
SIGNIEREN_S_JE_MB = 0.25

# Ab hier weigert sich packsign. Der Wert steht auch in SNAPSHOT.md.
SIGNATUR_GRENZE = 1.5 * 1024 ** 3

# Ab dieser PBF-Groesse legt `auslesen.py` seine Knotentabelle auf die Platte.
# Oesterreich (808 MB) brauchte 1,5 GB Arbeitsspeicher und ging bequem; bei
# Europa war der Speicher zu Ende, nach Stunden, mitten im Lauf, ohne Ergebnis.
INDEX_AB = 1024 ** 3


def menschlich(bytes_):
    for einheit in ("B", "kB", "MB", "GB"):
        if bytes_ < 1024 or einheit == "GB":
            return "%.1f %s" % (bytes_, einheit)
        bytes_ /= 1024.0
    return str(bytes_)


def dauer(sekunden):
    if sekunden < 90:
        return "%.0f s" % sekunden
    if sekunden < 5400:
        return "%.0f min" % (sekunden / 60.0)
    return "%.1f h" % (sekunden / 3600.0)


def pbf_groesse(pfad):
    """Fragt den Anbieter nach Laenge und Stand. Eine gemessene Eingangsgroesse
    ist mehr wert als jede Hochrechnung ueber die Flaeche."""
    antwort = requests.head(GEOFABRIK + pfad, timeout=60, verify=certifi.where(),
                            allow_redirects=True,
                            headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})
    antwort.raise_for_status()
    return (int(antwort.headers.get("Content-Length", 0)),
            antwort.headers.get("Last-Modified", "unbekannt"))


def packsign_pfad():
    ordner = os.path.join(WURZEL, "tools", "packsign", "build", "install", "packsign", "bin")
    for name in (("packsign.bat", "packsign") if os.name == "nt" else ("packsign",)):
        voll = os.path.join(ordner, name)
        if os.path.isfile(voll):
            return voll
    return None


def lauf(befehl, was):
    print("\n--- %s ---" % was)
    print("    " + " ".join(os.path.basename(t) if os.sep in t else t for t in befehl))
    sys.stdout.flush()
    t0 = time.time()
    ergebnis = subprocess.run(befehl, cwd=WURZEL)
    if ergebnis.returncode != 0:
        raise SystemExit("ABBRUCH: %s ist mit %d gescheitert."
                         % (was, ergebnis.returncode))
    return time.time() - t0


def schaetze(land, zmin, zmax):
    groesse, stand = pbf_groesse(land.auszug)
    mb = groesse / 1e6
    zwischen = groesse * ANTEIL_ZWISCHEN
    stufen = zmax - zmin + 1
    spanne = ANTEIL_DETAIL if zmax >= 11 else ANTEIL_UEBERSICHT
    je_stufe = (stufen / 4.0) if zmax >= 11 else (stufen / 7.0)
    ziel_klein = groesse * spanne[0] * je_stufe
    ziel_gross = groesse * spanne[1] * je_stufe
    ziel = (ziel_klein + ziel_gross) / 2.0
    t_holen = mb / HOLEN_MB_JE_S
    t_auslesen = mb / AUSLESEN_MB_JE_S
    # Das Groessere von beidem: bei kleinen Gebieten begrenzt das Schreiben,
    # bei grossen das Lesen der Zwischendatei.
    t_bauen = max(BAUEN_S_JE_MB_ERGEBNIS * (ziel / 1e6),
                  BAUEN_S_JE_MB_DURCHGANG * (zwischen / 1e6) * (stufen + 1))
    # Unterschreiben und Pruefen lesen die fertige Datei je einmal ganz durch.
    t_signieren = SIGNIEREN_S_JE_MB * ziel / 1e6
    return {"pbf": groesse, "stand": stand, "zwischen": zwischen,
            "ziel": ziel, "ziel_klein": ziel_klein, "ziel_gross": ziel_gross,
            "t_holen": t_holen, "t_auslesen": t_auslesen, "t_bauen": t_bauen,
            "t_signieren": t_signieren,
            "t_gesamt": t_holen + t_auslesen + t_bauen + t_signieren,
            "platte": groesse + zwischen + 2 * ziel
                      + (zwischen * 1.3 if groesse >= INDEX_AB else 0)}


def zeige_schaetzung(land, zmin, zmax, s, arbeit):
    print("Gebiet:        %s (%s), %s km2"
          % (land.name, land.code, "{:,}".format(land.flaeche).replace(",", " ")))
    print("Auszug:        %s" % land.auszug)
    print("               %s, Stand %s" % (menschlich(s["pbf"]), s["stand"]))
    print("Zoomstufen:    %d bis %d" % (zmin, zmax))
    print()
    print("  Schritt         Ergebnis                Dauer")
    print("  holen           %-22s  %s" % (menschlich(s["pbf"]), dauer(s["t_holen"])))
    print("  auslesen        %-22s  %s" % (menschlich(s["zwischen"]), dauer(s["t_auslesen"])))
    gross = "%s bis %s" % (menschlich(s["ziel_klein"]), menschlich(s["ziel_gross"]))
    print("  bauen           %-22s  %s" % (gross, dauer(s["t_bauen"])))
    print("  unterschreiben  %-22s  %s" % (menschlich(s["ziel"]), dauer(s["t_signieren"])))
    print("  ZUSAMMEN                                %s" % dauer(s["t_gesamt"]))
    print()
    frei = shutil.disk_usage(arbeit if os.path.isdir(arbeit) else WURZEL).free
    print("Platzbedarf:   etwa %s, frei sind %s"
          % (menschlich(s["platte"]), menschlich(frei)))
    if s["platte"] > frei:
        print("  ACHTUNG: das reicht nicht.")
    if s["pbf"] >= INDEX_AB:
        print("Knotentabelle: auf der Platte (--index), weil die PBF ueber 1 GB liegt.")
    if s["ziel_gross"] > SIGNATUR_GRENZE:
        # Die obere Kante der Spanne, nicht die Mitte: Wer erst nach Stunden
        # merkt, dass sich die Karte nicht unterschreiben laesst, hat die
        # Stunden umsonst gehabt.
        print("ACHTUNG: bis zu %s -- ueber der Grenze von 1,5 GB, ab der packsign"
              % menschlich(s["ziel_gross"]))
        print("         nicht mehr unterschreibt. Dann muss das Land geteilt")
        print("         werden, so wie Deutschland bei 51,1 Grad Nord.")
    print("Die Faktoren stehen im Kopf dieser Datei. Die Groessen streuen um")
    print("etwa ein Viertel, die Zeiten koennen um das Doppelte danebenliegen --")
    print("sie haengen daran, was sonst noch auf dem Rechner laeuft.")


def main():
    p = argparse.ArgumentParser(
        description="Baut aus einem Landesnamen eine fertige, signierte .czk.")
    p.add_argument("land", nargs="+", help="Name oder Gebietscode, z. B. Slowenien oder SI")
    p.add_argument("--zoom", nargs=2, type=int, default=[11, 14], metavar=("VON", "BIS"))
    p.add_argument("--toleranz", type=int,
                   help="Rastereinheiten; Vorgabe 4 fuer Detail, 16 fuer Uebersicht")
    p.add_argument("--aus", help="Zieldatei; Vorgabe work/karte/<land>-<art>.czk")
    p.add_argument("--arbeit", default=os.environ.get("COMPASS_KARTE_ARBEIT",
                                                      os.path.join("work", "karte")),
                   help="Ordner fuer Rohdaten und Zwischendateien")
    p.add_argument("--schluessel", default=os.path.join("work", "devkey", "entwicklung.secret"))
    p.add_argument("--vertrauen", default=os.path.join("work", "devkey", "trust.txt"))
    p.add_argument("--zuschneiden", action="store_true",
                   help="nur das Rechteck des Landes bauen -- fuer Auszuege, die "
                        "mehr enthalten als das Land (Malaysia, Irland, Indonesien)")
    p.add_argument("--nur-rechnen", action="store_true",
                   help="nichts holen und nichts bauen, nur rechnen und melden")
    a = p.parse_args()

    land = laender.finde(" ".join(a.land))
    if land is None:
        print("Unbekanntes Gebiet: %s" % " ".join(a.land))
        print("Was es gibt:  python tools/karte/laender.py")
        return 2

    zmin, zmax = a.zoom
    if not 0 <= zmin <= zmax <= 20:
        print("Zoomstufen unbrauchbar: %d bis %d" % (zmin, zmax))
        return 2

    if land.hinweis:
        print("Hinweis zu %s: %s\n" % (land.name, land.hinweis))

    if not land.auszug:
        print("%s hat bei Geofabrik KEINEN eigenen Auszug." % land.name)
        if land.steckt_in:
            print("Das Gebiet steckt in %s." % land.steckt_in)
            print()
            print("Dieses Werkzeug baut das nicht: Die Datei enthaelt mehr als")
            print("%s, und eine Karte mit dem Namen eines Landes, in der die" % land.name)
            print("Nachbarn mit drin sind, ist eine falsche Auskunft. Wer sie")
            print("trotzdem will, holt und baut sie von Hand und schneidet mit")
            print("  --ausschnitt %.4f %.4f %.4f %.4f"
                  % (land.kasten[1], land.kasten[0], land.kasten[3], land.kasten[2]))
            print("in tools/karte/bauen.py auf das Rechteck des Landes zu.")
        else:
            print("Es gibt ueberhaupt keinen Auszug, der das Gebiet ganz enthaelt.")
        return 1

    art = "detail" if zmax >= 11 else "ueberblick"
    toleranz = a.toleranz if a.toleranz is not None else (4 if zmax >= 11 else 16)
    arbeit = a.arbeit if os.path.isabs(a.arbeit) else os.path.join(WURZEL, a.arbeit)

    try:
        s = schaetze(land, zmin, zmax)
    except requests.RequestException as fehler:
        print("Der Anbieter antwortet nicht: %s" % fehler)
        return 1

    zeige_schaetzung(land, zmin, zmax, s, arbeit)
    if a.nur_rechnen:
        return 0

    # Alles, was ohne Netz und ohne Rechenzeit pruefbar ist, VOR dem Holen.
    ps = packsign_pfad()
    if ps is None:
        print("\npacksign ist nicht gebaut. Erst:  ./gradlew :tools:packsign:installDist")
        return 1
    schluessel = a.schluessel if os.path.isabs(a.schluessel) else os.path.join(WURZEL, a.schluessel)
    vertrauen = a.vertrauen if os.path.isabs(a.vertrauen) else os.path.join(WURZEL, a.vertrauen)
    if not os.path.isfile(schluessel):
        print("\nKein Schluessel unter %s." % schluessel)
        return 1
    if a.zuschneiden and land.ueber_datumsgrenze():
        print("\n%s liegt ueber der Datumsgrenze; --zuschneiden kann das nicht."
              % land.name)
        return 2

    os.makedirs(arbeit, exist_ok=True)
    pbf = os.path.join(arbeit, "%s-latest.osm.pbf" % land.schluessel)
    geom = os.path.join(arbeit, "%s.geom" % land.schluessel)
    ziel = a.aus if a.aus else os.path.join(arbeit, "%s-%s.czk" % (land.schluessel, art))
    if not os.path.isabs(ziel):
        ziel = os.path.join(WURZEL, ziel)
    signiert = ziel[:-4] + "-signiert.czk" if ziel.endswith(".czk") else ziel + "-signiert"

    print("\nArbeitsordner: %s" % arbeit)
    print("Ziel:          %s" % signiert)
    begonnen = time.time()
    zeiten = {}

    # 1. Holen. holen.py setzt selbst fort und prueft am Ende die Laenge.
    if os.path.isfile(pbf) and os.path.getsize(pbf) == s["pbf"]:
        print("\n--- holen --- liegt schon vollstaendig vor (%s)" % menschlich(s["pbf"]))
    else:
        zeiten["holen"] = lauf([sys.executable, HOLEN, land.auszug, pbf], "holen")

    # 2. Auslesen. Auf einen Namen mit .unfertig, damit ein Abbruch nicht als
    #    fertige Zwischendatei stehenbleibt.
    if os.path.isfile(geom) and os.path.getsize(geom) > 0:
        print("\n--- auslesen --- %s liegt schon vor (%s)"
              % (os.path.basename(geom), menschlich(os.path.getsize(geom))))
    else:
        roh = geom + ".unfertig"
        befehl = [sys.executable, AUSLESEN, pbf, roh]
        index = None
        if s["pbf"] >= INDEX_AB:
            index = os.path.join(arbeit, "%s-knoten.idx" % land.schluessel)
            befehl += ["--index", index]
        zeiten["auslesen"] = lauf(befehl, "auslesen")
        if not os.path.isfile(roh) or os.path.getsize(roh) == 0:
            raise SystemExit("ABBRUCH: auslesen hat keine Zwischendatei geliefert.")
        os.replace(roh, geom)
        if index and os.path.isfile(index):
            # Die Knotentabelle ist fuer Europa 60 GB gross und wird nach dem
            # Auslesen von niemandem mehr gebraucht.
            os.remove(index)

    # 3. Bauen.
    if os.path.isfile(ziel) and os.path.getsize(ziel) > 0:
        print("\n--- bauen --- %s liegt schon vor (%s)"
              % (os.path.basename(ziel), menschlich(os.path.getsize(ziel))))
    else:
        roh = ziel + ".unfertig"
        befehl = [sys.executable, BAUEN, geom, roh,
                  "--zoom", str(zmin), str(zmax), "--toleranz", str(toleranz)]
        if a.zuschneiden:
            w, sued, o, n = land.kasten
            befehl += ["--ausschnitt", str(sued), str(w), str(n), str(o)]
        zeiten["bauen"] = lauf(befehl, "bauen")
        if not os.path.isfile(roh) or os.path.getsize(roh) == 0:
            raise SystemExit("ABBRUCH: bauen hat keine Karte geliefert.")
        os.replace(roh, ziel)

    gebaut = os.path.getsize(ziel)
    if gebaut > SIGNATUR_GRENZE:
        print("\nDie Karte ist %s gross. packsign unterschreibt nur bis 1,5 GB."
              % menschlich(gebaut))
        print("Die unsignierte Karte liegt unter %s." % ziel)
        print("Teile das Land in zwei Baulaeufe mit --zuschneiden und je einem")
        print("halben Rechteck, so wie Deutschland bei 51,1 Grad Nord.")
        return 1

    # 4. Unterschreiben -- und danach pruefen, mit demselben Werkzeug, das auch
    #    die App benutzt. Eine Unterschrift, die niemand nachgeprueft hat, ist
    #    keine.
    fertig = False
    if os.path.isfile(signiert) and os.path.getsize(signiert) > gebaut:
        print("\n--- unterschreiben --- %s liegt schon vor, wird geprueft"
              % os.path.basename(signiert))
        fertig = subprocess.run([ps, "karte-pruefen", "--in", signiert,
                                 "--keys", vertrauen], cwd=WURZEL).returncode == 0
        if not fertig:
            print("    Die vorhandene Datei haelt der Pruefung nicht stand -- neu.")
    if not fertig:
        zeiten["unterschreiben"] = lauf(
            [ps, "karte-signieren", "--key", schluessel, "--in", ziel, "--out", signiert],
            "unterschreiben")
        zeiten["pruefen"] = lauf(
            [ps, "karte-pruefen", "--in", signiert, "--keys", vertrauen], "pruefen")

    gesamt = time.time() - begonnen
    print("\n" + "=" * 62)
    print("Karte:      %s" % signiert)
    print("Groesse:    %s Bytes (%s)"
          % ("{:,}".format(os.path.getsize(signiert)).replace(",", " "),
             menschlich(os.path.getsize(signiert))))
    print("Zoom:       %d bis %d, Toleranz %d" % (zmin, zmax, toleranz))
    print("Geschaetzt: %s -- gebraucht: %s" % (menschlich(s["ziel"]), menschlich(gebaut)))
    for schritt, t in zeiten.items():
        print("  %-14s %s" % (schritt, dauer(t)))
    print("Dauer:      %s (geschaetzt %s)" % (dauer(gesamt), dauer(s["t_gesamt"])))
    return 0


if __name__ == "__main__":
    sys.exit(main())
