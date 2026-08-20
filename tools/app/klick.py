# -*- coding: utf-8 -*-
"""Bedient die App am Emulator ueber SICHTBAREN TEXT statt ueber geratene
Koordinaten.

WARUM ES DIESES SKRIPT GIBT: In der Nacht zum 03.08.2026 sind mehrere
Pruefungen ins Leere gelaufen, weil die Tippkoordinaten aus einem
Bildschirmfoto abgeschaetzt waren. Die untere Leiste liegt bei y=2228 und
nicht bei 2160 -- der Unterschied faellt auf einem verkleinerten Bild nicht
auf, und ein Tipp daneben sieht wie "nichts passiert" aus. Schlimmer: Ein
Screenshot danach zeigt einen Zustand, den man fuer das Ergebnis haelt.

Aufrufe:
    python tools/app/klick.py liste
        Gibt jeden sichtbaren Text mit seinem Mittelpunkt aus.
    python tools/app/klick.py tippe "Einstellungen" [N]
        Sucht den Text (Teilstring, Gross-/Kleinschreibung egal) und tippt
        auf seine Mitte. Bricht ab, wenn es ihn nicht gibt -- dann ist die
        Annahme falsch und nicht die App. Gibt es MEHRERE Treffer, werden
        sie alle aufgelistet; mit N waehlt man den n-ten (ab 0).
    python tools/app/klick.py warte "Sparmodus" 10
    python tools/app/klick.py scrolle [runter|hoch] [Anzahl]
        Wischt in der aktuellen Ansicht und MELDET, ob sich etwas bewegt
        hat. Ein stummer Scrollbefehl hat am 11.08.2026 eine Sichtpruefung
        ins Leere laufen lassen.
        Wartet bis zu 10 Sekunden, bis der Text auftaucht.
    python tools/app/klick.py bild ZIEL.png
        Bildschirmfoto.

Alle Pfade auf dem Geraet muessen mit MSYS_NO_PATHCONV=1 laufen oder ueber
dieses Skript gehen: Git Bash wandelt "/sdcard/..." sonst in einen
Windows-Pfad um.
"""
import os
import html
import re
import subprocess
import sys
import time


# Das Android-SDK wird ueber ANDROID_HOME gefunden -- dieselbe Variable, die
# auch der Gradle-Bau braucht. Ist sie nicht gesetzt, wird das Werkzeug im
# PATH gesucht.
def sdk_werkzeug(name, unterordner):
    wurzel = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if wurzel:
        pfad = os.path.join(wurzel, unterordner, name + (".exe" if os.name == "nt" else ""))
        if os.path.isfile(pfad):
            return pfad
    return name


ADB = sdk_werkzeug("adb", "platform-tools")
PAKET = "org.compasszero"

# Die Ausgabe traegt Text aus der App, und der enthaelt Zeichen, die die
# Windows-Voreinstellung cp1252 nicht kennt. Am 17.08.2026 ist "liste" auf der
# Kartenansicht mitten in der Aufzaehlung an einem echten Minuszeichen (U+2212)
# abgestuerzt -- die Ansicht sah danach aus, als haette sie vier Elemente. Ein
# Werkzeug zum Nachsehen darf nicht am Nachzusehenden scheitern.
for strom in (sys.stdout, sys.stderr):
    try:
        strom.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, ValueError):
        pass


def adb(*args, binaer=False):
    umgebung = dict(os.environ, MSYS_NO_PATHCONV="1")
    p = subprocess.run([ADB, *args], capture_output=True, env=umgebung)
    return p.stdout if binaer else p.stdout.decode("utf-8", "replace")


def geraet_da():
    """Prueft, ob ueberhaupt ein Emulator angeschlossen ist.

    In der Nacht zum 03.08.2026 ist der Emulator nach Stunden verschwunden.
    Der Durchlauf meldete daraufhin "Konnte nicht auf die Kachelansicht
    zurueck" -- eine Meldung, die nach einem Fehler in der APP klingt und auf
    eine falsche Faehrte fuehrt. Deshalb wird das jetzt zuerst geprueft und
    klar benannt.
    """
    zeilen = [z for z in adb("devices").splitlines()[1:] if z.strip()]
    return any("	device" in z for z in zeilen)


def baum():
    if not geraet_da():
        raise SystemExit(
            "KEIN GERAET ANGESCHLOSSEN. Der Emulator laeuft nicht -- das ist "
            "kein Fehler der App. Starten mit: "
            "$ANDROID_HOME/emulator/emulator -avd compasszero"
        )
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    return adb("shell", "cat", "/sdcard/ui.xml")


def elemente():
    s = baum()
    gefunden = []
    for m in re.finditer(r'text="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', s):
        # uiautomator schreibt den Text als XML-ATTRIBUT: Zeilenumbrueche
        # werden zu &#10;, kaufmaennische Und zu &amp;, Anfuehrungszeichen zu
        # &quot;. Wer das roh ausgibt, sieht "&#10;" mitten im Satz und haelt
        # es fuer kaputten Inhalt -- am 11.08.2026 genau so passiert.
        # Die Zeichen stehen NICHT in den Paketdaten, sie sind Verpackung.
        text = html.unescape(m.group(1))
        if not text.strip():
            continue
        x1, y1, x2, y2 = (int(g) for g in m.groups()[1:])
        gefunden.append((text, (x1 + x2) // 2, (y1 + y2) // 2))
    return gefunden


def suche(nadel, nummer=0):
    """Alle Treffer sammeln statt blind den ersten zu nehmen.

    In der Nacht zum 03.08.2026 hat das Werkzeug beim Oeffnen eines Suchtreffers
    den EINGETIPPTEN Suchtext im Eingabefeld erwischt statt den Treffer
    darunter -- beide enthalten dasselbe Wort. Das sah aus, als sei nichts
    passiert. Deshalb: bei mehreren Treffern wird gewarnt, und man kann den
    n-ten waehlen.
    """
    nadel = nadel.lower()
    treffer = [(t, x, y) for t, x, y in elemente() if nadel in t.lower()]
    if not treffer:
        return None
    if len(treffer) > 1 and nummer == 0:
        print("ACHTUNG: %d Treffer fuer \"%s\" -- genommen wird der erste."
              % (len(treffer), nadel))
        for i, (t, _, y) in enumerate(treffer):
            print("   [%d] y=%4d  %s" % (i, y, t[:60]))
    if nummer >= len(treffer):
        return None
    return treffer[nummer]


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    befehl = sys.argv[1]

    if befehl == "liste":
        for text, x, y in elemente():
            print("%5d %5d  %s" % (x, y, text[:70]))
        return 0

    if befehl == "tippe":
        nummer = int(sys.argv[3]) if len(sys.argv) > 3 else 0
        treffer = suche(sys.argv[2], nummer)
        if treffer is None:
            print('NICHT GEFUNDEN: "%s"' % sys.argv[2])
            print("Sichtbar waere gewesen:")
            for text, _, _ in elemente()[:20]:
                print("   " + text[:70])
            return 1
        text, x, y = treffer
        adb("shell", "input", "tap", str(x), str(y))
        print('getippt auf "%s" bei %d,%d' % (text[:50], x, y))
        return 0

    if befehl == "warte":
        nadel = sys.argv[2]
        grenze = float(sys.argv[3]) if len(sys.argv) > 3 else 10.0
        ende = time.time() + grenze
        while time.time() < ende:
            if suche(nadel):
                print('da: "%s"' % nadel)
                return 0
            time.sleep(1)
        print('NICHT ERSCHIENEN in %.0fs: "%s"' % (grenze, nadel))
        return 1

    if befehl == "scrolle":
        # WARUM DIESER BEFEHL EIGENS MELDET, OB SICH ETWAS BEWEGT HAT:
        # In der Nacht auf den 11.08.2026 lief eine Sichtpruefung ins Leere,
        # weil "adb shell input swipe" die Detailansicht NICHT bewegte und
        # dabei stumm blieb. Sechzehn Wischer, kein Fehler, kein Fortschritt.
        # Ein Scrollbefehl, der schweigt, wenn nichts passiert, ist schlimmer
        # als keiner -- man haelt den Abzug fuer das Ergebnis.
        richtung = sys.argv[2] if len(sys.argv) > 2 else "runter"
        male = int(sys.argv[3]) if len(sys.argv) > 3 else 3
        if richtung not in ("runter", "hoch"):
            print('Richtung muss "runter" oder "hoch" sein.')
            return 2
        # DER FEHLER, DER ES ZUERST NICHT TUN LIESS: Feste Koordinaten. Der
        # scrollbare Bereich der Detailansicht endet bei rund y=1280 -- ein
        # Wischer bei y=1500 liegt DARUNTER und erreicht ihn nie. Deshalb
        # werden die Grenzen jetzt aus dem Abzug gelesen.
        vorher = baum()
        m = re.search(r'scrollable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',
                      vorher)
        if not m:
            m = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*scrollable="true"',
                          vorher)
        if not m:
            print("KEIN scrollbarer Bereich in dieser Ansicht.")
            return 1
        x1, y1, x2, y2 = (int(g) for g in m.groups())
        mitte_x = (x1 + x2) // 2
        hoch_y = y1 + int((y2 - y1) * 0.2)
        tief_y = y2 - int((y2 - y1) * 0.2)
        # Langsam wischen (600 ms) statt zu schnippen: Ein Fling scrollt
        # unkontrolliert weit, ein langsamer Zug bewegt um die Strecke.
        von_y, nach_y = (tief_y, hoch_y) if richtung == "runter" else (hoch_y, tief_y)
        bewegt = 0
        for _ in range(male):
            adb("shell", "input", "touchscreen", "swipe",
                str(mitte_x), str(von_y), str(mitte_x), str(nach_y), "600")
            time.sleep(0.6)
            jetzt = baum()
            if jetzt != vorher:
                bewegt += 1
                vorher = jetzt
        if bewegt == 0:
            print("NICHTS BEWEGT: %d Wischer %s, der Abzug blieb gleich."
                  % (male, richtung))
            print("Die Ansicht ist entweder nicht scrollbar oder schon am Ende.")
            return 1
        print("gescrollt: %d von %d Wischern haben etwas bewegt (%s)"
              % (bewegt, male, richtung))
        return 0

    if befehl == "bild":
        daten = adb("exec-out", "screencap", "-p", binaer=True)
        with open(sys.argv[2], "wb") as f:
            f.write(daten)
        print("Bild: %s (%d Bytes)" % (sys.argv[2], len(daten)))
        return 0

    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main())
