# -*- coding: utf-8 -*-
"""Schreibt STAND.txt neben die APK: Datum, Zahlen, Pruefsummen.

WOZU: Wer die Dateien spaeter auf dem Telefon hat, soll ohne Nachfragen sehen
koennen, WELCHER Stand das ist und ob die Datei heil uebertragen wurde.
"""
import hashlib
import io
import json
import os
import subprocess
import time

WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ORDNER = os.path.join(WURZEL, "work", "fuers-handy")


def summe(pfad):
    h = hashlib.sha256()
    with open(pfad, "rb") as f:
        for stueck in iter(lambda: f.read(1 << 20), b""):
            h.update(stueck)
    return h.hexdigest()


def zaehle():
    aus = {}
    for datei, schluessel in (("tips", "tips"), ("guides", "guides"), ("agriculture", "chapters")):
        p = os.path.join(WURZEL, "content", "europe-de", "paket", "content", datei + ".json")
        aus[datei] = len(json.loads(io.open(p, encoding="utf-8").read())[schluessel])
    return aus


def stand():
    try:
        return subprocess.run(["git", "-C", WURZEL, "log", "-1", "--format=%h %ad", "--date=format:%d.%m.%Y %H:%M"],
                              stdout=subprocess.PIPE, text=True).stdout.strip()
    except Exception:
        return "unbekannt"


zahlen = zaehle()
zeilen = [
    "COMPASS ZERO -- Stand dieser Dateien",
    "",
    "Gebaut am:      " + time.strftime("%d.%m.%Y um %H:%M"),
    "Letzte Aenderung im Projekt: " + stand(),
    "",
    "Inhalt:         %d Eintraege" % sum(zahlen.values()),
    "                %d Tipps, %d Anleitungen, %d Kapitel"
    % (zahlen["tips"], zahlen["guides"], zahlen["agriculture"]),
    "",
    "DATEIEN UND PRUEFSUMMEN (SHA-256)",
    "",
]
for name in sorted(os.listdir(ORDNER)):
    p = os.path.join(ORDNER, name)
    if not os.path.isfile(p) or not name.lower().endswith((".apk", ".czk", ".czb", ".czh")):
        continue
    zeilen.append("  %s" % name)
    zeilen.append("    %d MB" % (os.path.getsize(p) // 1048576))
    zeilen.append("    %s" % summe(p))
    zeilen.append("")

zeilen += [
    "SO PRUEFST DU EINE DATEI NACH DEM KOPIEREN",
    "",
    "  Auf dem Rechner in der Eingabeaufforderung:",
    "      certutil -hashfile <datei> SHA256",
    "  Kommt dieselbe Zeichenfolge heraus wie oben, ist die Datei heil.",
    "",
]
io.open(os.path.join(ORDNER, "STAND.txt"), "w", encoding="utf-8", newline="\r\n").write("\n".join(zeilen))
print("STAND.txt geschrieben: %d Eintraege" % sum(zahlen.values()))
