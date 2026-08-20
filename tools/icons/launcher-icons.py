"""Erzeugt die Launcher-Icons fuer Android aus design/logo/app-icon.svg.

Gebraucht werden nur die PNG-Groessen fuer Android vor Version 8; ab dort
zeichnet das System das adaptive Icon aus den Vektorgrafiken in
androidApp/src/main/res/drawable/. Gerastert wird mit einem Chromium-Browser,
weil der einen vollstaendigen SVG-Renderer mitbringt.

    python tools/icons/launcher-icons.py [--browser PFAD]
"""

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile

WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
QUELLE = os.path.join(WURZEL, "design", "logo", "app-icon.svg")
ZIEL = os.path.join(WURZEL, "androidApp", "src", "main", "res")

# Papierton wie in res/values/colors.xml. Das Buch ist schwarz und wuerde auf
# einem dunklen Hintergrundbild sonst verschwinden.
PAPIER = "#FAF9F6"

GROESSEN = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

KANDIDATEN = [
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    "/usr/bin/chromium",
    "/usr/bin/chromium-browser",
    "/usr/bin/google-chrome",
]


def finde_browser(vorgabe):
    if vorgabe:
        return vorgabe
    for pfad in KANDIDATEN:
        if os.path.isfile(pfad):
            return pfad
    gefunden = shutil.which("chromium") or shutil.which("google-chrome")
    if gefunden:
        return gefunden
    sys.exit("Kein Chromium gefunden. Pfad mit --browser angeben.")


def motiv():
    text = open(QUELLE, encoding="utf-8").read()
    innen = re.search(r"<svg[^>]*>(.*)</svg>", text, re.S)
    if not innen:
        sys.exit("app-icon.svg hat nicht die erwartete Form")
    return innen.group(1)


def seite(inhalt, kante):
    # Die abgerundete Ecke entspricht dem, was aeltere Launcher selbst nicht
    # zuschneiden -- deshalb steckt sie hier im Bild.
    return f"""<!doctype html>
<html><head><meta charset="utf-8"><style>
html,body{{margin:0;padding:0;background:transparent}}
svg{{display:block}}
</style></head><body>
<svg xmlns="http://www.w3.org/2000/svg" width="{kante}" height="{kante}" viewBox="0 0 512 512">
  <rect x="0" y="0" width="512" height="512" rx="96" fill="{PAPIER}"/>
  {inhalt}
</svg>
</body></html>
"""


def main():
    zerteiler = argparse.ArgumentParser()
    zerteiler.add_argument("--browser", default=None)
    argumente = zerteiler.parse_args()
    browser = finde_browser(argumente.browser)
    inhalt = motiv()

    with tempfile.TemporaryDirectory() as arbeit:
        for ordner, kante in GROESSEN.items():
            quelle = os.path.join(arbeit, f"{kante}.html")
            open(quelle, "w", encoding="utf-8").write(seite(inhalt, kante))
            ziel = os.path.join(ZIEL, ordner)
            os.makedirs(ziel, exist_ok=True)
            bild = os.path.join(ziel, "ic_launcher.png")
            subprocess.run(
                [
                    browser,
                    "--headless=new",
                    "--disable-gpu",
                    "--hide-scrollbars",
                    "--force-device-scale-factor=1",
                    "--default-background-color=00000000",
                    f"--window-size={kante},{kante}",
                    f"--screenshot={bild}",
                    "file:///" + quelle.replace("\\", "/"),
                ],
                check=True,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            print(f"{ordner}/ic_launcher.png  {kante}x{kante}")


if __name__ == "__main__":
    main()
