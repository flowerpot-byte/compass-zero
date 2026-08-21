# -*- coding: utf-8 -*-
"""Prueft den COMMITTETEN Stand, nicht den Arbeitsstand.

WOZU: Die Testreihe liest die Dateien auf der Platte. Sie kann deshalb gruen
sein, waehrend im Commit etwas anderes steht. Genau das ist am 21.08.2026
passiert: `git add <bilder>` gefolgt von einem `git commit` ohne Pfadangabe
hat nur die Bilder erfasst, nicht die Inhaltsdatei, die sie nennt. Im Repo
lagen zwei Bilder, auf die nichts zeigte -- auf der Platte war alles richtig,
und kein Test schlug an.

Prueft zwei Richtungen:
  * ein Bild im Commit, das keine Inhaltsdatei nennt
  * ein genanntes Bild, das im Commit fehlt (schlimmer -- das Paket laedt nicht)

Aufruf:  python tools/inhalt/commit_stimmig.py [commit]
"""
import re
import subprocess
import sys

STAND = sys.argv[1] if len(sys.argv) > 1 else "HEAD"
WURZEL = "content/europe-de/paket"


def zeig(pfad):
    return subprocess.run(["git", "show", "%s:%s" % (STAND, pfad)],
                          stdout=subprocess.PIPE, text=True,
                          encoding="utf-8", errors="replace").stdout


genannt = set()
for datei in ("tips", "guides", "agriculture"):
    genannt |= set(re.findall(r"assets/[A-Za-z0-9._-]+",
                              zeig("%s/content/%s.json" % (WURZEL, datei))))

vorhanden = set()
for zeile in subprocess.run(["git", "ls-tree", "-r", "--name-only", STAND,
                             "%s/assets" % WURZEL],
                            stdout=subprocess.PIPE, text=True).stdout.split():
    vorhanden.add("assets/" + zeile.split("/")[-1])

fehlt = sorted(genannt - vorhanden)
unbenutzt = sorted(vorhanden - genannt)

print("Stand %s: %d Bilder, %d davon genannt" % (STAND, len(vorhanden), len(genannt & vorhanden)))
if fehlt:
    print("\nGENANNT, ABER NICHT IM COMMIT (%d) -- damit laedt das Paket nicht:" % len(fehlt))
    for f in fehlt:
        print("   ", f)
if unbenutzt:
    print("\nIM COMMIT, ABER NIRGENDS GENANNT (%d):" % len(unbenutzt))
    for f in unbenutzt:
        print("   ", f)
if fehlt or unbenutzt:
    sys.exit(1)
print("stimmig.")
