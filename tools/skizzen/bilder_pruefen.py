# -*- coding: utf-8 -*-
"""Haelt Bildverweise, Bilddateien und Zeichenquellen zusammen.

WOZU: Beim Einhaengen einer Zeichnung koennen FUENF Dinge auseinanderlaufen, und
keines davon faellt beim Bauen auf:

1. Ein Verweis zeigt auf eine Datei, die es nicht gibt. Das MELDET der Parser
   zwar als Warnung, aber nur beim Laden eines Pakets, und eine Warnung geht in
   einem gruenen Lauf unter.
2. Eine PNG liegt im Paket, auf die niemand verweist. Sie wandert in jede
   Auslieferung mit und kostet Platz, den ein Handbuch ohne Netz nicht hat.
3. Eine PNG hat keine SVG-Quelle mehr in `design/skizzen/`. Dann laesst sie
   sich nie wieder aendern -- das PNG ist erzeugt, nicht gezeichnet.
4. Eine PNG ist AELTER als ihre SVG. Dann wurde die Zeichnung nachgebessert
   und nicht neu erzeugt: Die App zeigt die alte Fassung, und weil beide
   Dateien da sind, meldet nichts einen Fehler. Der Fall schleicht sich am
   leichtesten ein, wenn an einem Abend zwanzig Zeichnungen nachgebessert
   werden.
5. Eine Datei liegt DA, ist aber nicht in git. Dann ist auf diesem Rechner
   alles in Ordnung und im Repo fehlt sie. Genau das ist am 04.08.2026
   passiert: Zwei Zeichnungen waren eingehaengt, die JSON-Verweise auf sie
   committet, die Bilder selbst nie hinzugefuegt. Wer geklont haette, haette
   ein Paket mit zwei fehlenden Bildern bekommen. Aufgefallen ist es an
   "git status" und nicht an einer Pruefung -- deshalb steht es jetzt hier.
   HINWEIS: Eine Zeichnung, an der gerade gearbeitet wird, taucht hier
   ebenfalls auf. Das ist gewollt und kein Fehlalarm: Sie IST noch nicht im
   Repo. Vor dem Paketbau soll die Liste leer sein.

Am 04.08.2026 waren am Ende alle fuenf sauber (168 Verweise, 168 Dateien). Der Griff
steht hier, damit das so bleibt, ohne dass jemand daran denken muss.

Aufruf:  python tools/skizzen/bilder_pruefen.py
Rueckgabewert 0 heisst: alles passt zusammen.
"""
import glob
import io
import json
import os
import subprocess
import sys

PAKET = "content/europe-de/paket"
QUELLEN = "design/skizzen"


def verweise():
    aus = {}
    for datei, schluessel in (("tips.json", "tips"), ("guides.json", "guides"),
                              ("agriculture.json", "chapters")):
        pfad = os.path.join(PAKET, "content", datei)
        d = json.load(io.open(pfad, encoding="utf-8"))
        for e in d[schluessel]:
            if e.get("image"):
                aus.setdefault(e["image"], []).append("%s#%s" % (datei, e["id"]))
            for nr, teil in enumerate(e.get("steps", []) + e.get("sections", [])):
                if teil.get("image"):
                    aus.setdefault(teil["image"], []).append("%s#%s[%d]" % (datei, e["id"], nr))
    return aus


def main():
    benutzt = verweise()
    dateien = {"assets/" + os.path.basename(p)
               for p in glob.glob(os.path.join(PAKET, "assets", "*.png"))}
    quellen = {os.path.basename(p)[:-4] for p in glob.glob(os.path.join(QUELLEN, "*.svg"))}

    fehlend = sorted(set(benutzt) - dateien)
    ungenutzt = sorted(dateien - set(benutzt))
    ohne_quelle = sorted(n for n in dateien if os.path.basename(n)[:-4] not in quellen)

    # VIERTER FALL, und der schleicht sich am leichtesten ein: Die SVG wurde
    # nachgebessert, die PNG aber nicht neu erzeugt. Dann zeigt die App die
    # ALTE Zeichnung, waehrend im Quelltext die berichtigte steht -- und weil
    # beide Dateien da sind, meldet nichts einen Fehler. Zwei Sekunden Puffer,
    # weil Rastern und Schreiben nicht in derselben Sekunde passieren muessen.
    veraltet = []
    for n in sorted(dateien):
        name = os.path.basename(n)[:-4]
        svg = os.path.join(QUELLEN, name + ".svg")
        png = os.path.join(PAKET, "assets", name + ".png")
        if os.path.exists(svg) and os.path.getmtime(svg) > os.path.getmtime(png) + 2:
            veraltet.append(n)

    # SECHSTER FALL, am 17.08.2026 dazugekommen und der unangenehmste: Eine SVG
    # ist gezeichnet, aber nie gerastert worden. Sie liegt dann jahrelang neben
    # 200 fertigen Zeichnungen, heisst wie ein Eintrag und SIEHT fertig aus --
    # nur schaut niemand mehr hin, weil sie nirgends auftaucht.
    #
    # Warum das gefaehrlich ist und nicht bloss unordentlich: Wird der zugehoerige
    # Tipp berichtigt, zieht das die eingehaengten Zeichnungen mit, weil man sie
    # beim Nachlesen sieht. Die nie eingehaengte zieht nichts mit. Genau so ist
    # es passiert: "seitenlage-wann-nicht.svg" fuehrte von "Atmung setzt aus"
    # geradewegs auf "sofort Herzdruckmassage beginnen" -- ohne das Zurueckdrehen
    # auf den Ruecken. Im Tipp war der Satz laengst berichtigt. Haette jemand die
    # Zeichnung spaeter einfach gerastert, waere der Fehler als BILD
    # zurueckgekommen, und einem Ablaufplan glaubt man mehr als einem Absatz.
    nie_gerastert = sorted(
        n for n in quellen
        if not os.path.exists(os.path.join(PAKET, "assets", n + ".png"))
    )

    # Was liegt da, ist aber nicht in git? "git status --porcelain" nennt
    # unverfolgte Dateien mit "??". Ohne git (etwa in einem Export) faellt der
    # Punkt still aus, statt den ganzen Griff scheitern zu lassen.
    unverfolgt = []
    try:
        r = subprocess.run(["git", "status", "--porcelain", "--", PAKET + "/assets", QUELLEN],
                           capture_output=True, text=True, timeout=30)
        for zeile in r.stdout.splitlines():
            if zeile.startswith("??"):
                unverfolgt.append(zeile[3:].strip())
    except Exception:
        pass

    print("%d Verweise, %d Bilddateien, %d Zeichenquellen" % (len(benutzt), len(dateien), len(quellen)))
    schlimm = False
    if fehlend:
        schlimm = True
        print("\nVERWEIS OHNE DATEI -- das Bild fehlt im Paket:")
        for n in fehlend:
            print("   %s  (aus %s)" % (n, ", ".join(benutzt[n])))
    if ungenutzt:
        schlimm = True
        print("\nDATEI OHNE VERWEIS -- liegt im Paket, wird nie gezeigt:")
        for n in ungenutzt:
            print("   " + n)
    if ohne_quelle:
        schlimm = True
        print("\nPNG OHNE SVG-QUELLE -- laesst sich nicht mehr aendern:")
        for n in ohne_quelle:
            print("   " + n)
    if veraltet:
        schlimm = True
        print("\nPNG AELTER ALS IHRE SVG -- die App zeigt die alte Zeichnung:")
        for n in veraltet:
            print("   " + n)
    if nie_gerastert:
        schlimm = True
        print("\nSVG NIE GERASTERT -- gezeichnet, aber nie im Paket angekommen:")
        for n in nie_gerastert:
            print("   design/skizzen/%s.svg" % n)
        print("   (Eine solche Zeichnung wird nicht mitberichtigt, wenn ihr Tipp")
        print("    sich aendert -- vor dem Einhaengen gegen den heutigen Text lesen.)")
    if unverfolgt:
        schlimm = True
        print("\nLIEGT DA, ABER NICHT IN GIT -- fehlt jedem, der das Repo klont:")
        for n in unverfolgt:
            print("   " + n)
    if not schlimm:
        print("Alles passt zusammen.")
    return 1 if schlimm else 0


sys.exit(main())
