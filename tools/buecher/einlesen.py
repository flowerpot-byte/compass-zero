"""Holt gescannte Buchseiten vom Telefon und macht sie durchsuchbar.

Die Scanner-App auf dem Telefon liefert reine Bild-PDFs. Dieses Werkzeug holt
sie ueber das Kabel, laesst Tesseract darueberlaufen und legt PDF und Volltext
unter work/quellen/buecher/ ab -- am selben Ort wie alle anderen Rohquellen
und wie diese nicht versioniert.

    python tools/buecher/einlesen.py --liste     zeigt, was auf dem Telefon liegt
    python tools/buecher/einlesen.py             holt und liest alles Neue ein
    python tools/buecher/einlesen.py --nur alton holt nur passende Dateinamen

Bereits eingelesene Dateien werden uebersprungen; ein zweiter Lauf kostet
also nichts. Wer eine Datei neu einlesen will, loescht ihre .txt.
"""

import argparse
import io
import os
import re
import subprocess
import sys
import tempfile

TESSERACT = os.environ.get(
    "TESSERACT", r"C:\Program Files\Tesseract-OCR\tesseract.exe")
QUELLORDNER = "/sdcard/Download"


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

WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ZIEL = os.path.join(WURZEL, "work", "quellen", "buecher")


def telefon():
    """Seriennummer des angeschlossenen Telefons -- der Emulator zaehlt nicht."""
    lauf = subprocess.run([ADB, "devices"], capture_output=True, text=True)
    geraete = [
        z.split("\t")[0] for z in lauf.stdout.splitlines()[1:]
        if "\tdevice" in z and not z.startswith("emulator-")
    ]
    if not geraete:
        sys.exit(
            "Kein Telefon gefunden. Per Kabel anschliessen, USB-Debugging "
            "einschalten und die Rueckfrage auf dem Telefon bestaetigen."
        )
    if len(geraete) > 1:
        sys.exit(f"Mehrere Geraete: {geraete}. Nur eines anschliessen.")
    return geraete[0]


def pdfs_auf_telefon(seriennummer):
    lauf = subprocess.run(
        [ADB, "-s", seriennummer, "shell", f'ls -la "{QUELLORDNER}"'],
        capture_output=True, text=True,
    )
    posten = []
    for zeile in lauf.stdout.splitlines():
        if not zeile.lower().endswith(".pdf"):
            continue
        teile = zeile.split(None, 7)
        if len(teile) < 8:
            continue
        try:
            groesse = int(teile[4])
        except ValueError:
            continue
        posten.append((teile[7].strip(), groesse, teile[5]))
    return sorted(posten, key=lambda p: p[2], reverse=True)


def hole(seriennummer, name, ziel):
    lauf = subprocess.run(
        [ADB, "-s", seriennummer, "pull", f"{QUELLORDNER}/{name}", ziel],
        capture_output=True, text=True,
    )
    return lauf.returncode == 0


def erkenne_seite(rohbild, sprache):
    from PIL import Image
    with tempfile.TemporaryDirectory() as arbeit:
        quelle = os.path.join(arbeit, "seite.png")
        try:
            bild = Image.open(io.BytesIO(rohbild))
        except Exception:
            return ""
        # Graustufen reichen Tesseract und sind schneller als Farbe.
        bild.convert("L").save(quelle, "PNG")
        ziel = os.path.join(arbeit, "text")
        lauf = subprocess.run(
            [TESSERACT, quelle, ziel, "-l", sprache, "--psm", "3"],
            capture_output=True, text=True,
        )
        if lauf.returncode != 0:
            return ""
        with open(ziel + ".txt", encoding="utf-8", errors="replace") as datei:
            return datei.read()


def lies_ein(pdfpfad, sprache):
    from pypdf import PdfReader
    leser = PdfReader(pdfpfad)
    teile = []
    for nr, seite in enumerate(leser.pages, 1):
        vorhanden = (seite.extract_text() or "").strip()
        if len(vorhanden) > 80:
            teile.append(f"===== Seite {nr} =====\n{vorhanden}")
            continue
        erkannt = []
        try:
            bilder = list(seite.images)
        except Exception:
            bilder = []
        for bild in bilder:
            text = erkenne_seite(bild.data, sprache).strip()
            if text:
                erkannt.append(text)
        teile.append(f"===== Seite {nr} =====\n" + "\n".join(erkannt))
        if nr % 20 == 0:
            print(f"      {nr}/{len(leser.pages)} Seiten", flush=True)
    ganz = "\n\n".join(teile) + "\n"
    woerter = len(re.findall(r"[A-Za-zÄÖÜäöüß]{2,}", ganz))
    return ganz, len(leser.pages), woerter


def main():
    z = argparse.ArgumentParser()
    z.add_argument("--liste", action="store_true", help="nur anzeigen, was auf dem Telefon liegt")
    # Vorgabe "Buch", und zwar mit Absicht: Im Download-Ordner eines Telefons
    # liegen Vertraege, Meldebescheinigungen und Fahrkarten. Ohne Filter zoege
    # dieses Werkzeug sie alle in den Projektordner. Wer wirklich alles will,
    # muss --nur "" schreiben und weiss dann, was er tut.
    z.add_argument("--nur", default="Buch",
                   help='nur Dateinamen mit diesem Text (Vorgabe "Buch")')
    z.add_argument("--sprache", default="eng", help="eng (Vorgabe) oder deu")
    a = z.parse_args()

    if not os.path.isfile(TESSERACT):
        sys.exit(f"Tesseract fehlt: {TESSERACT}")
    os.makedirs(ZIEL, exist_ok=True)

    seriennummer = telefon()
    alle = pdfs_auf_telefon(seriennummer)
    posten = [p for p in alle if a.nur.lower() in p[0].lower()] if a.nur else alle
    if not posten:
        print(f"Keine passende PDF in {QUELLORDNER}. Gefunden wurden {len(alle)} PDF(s), "
              f"aber keine mit \"{a.nur}\" im Namen.")
        return

    print(f"Telefon {seriennummer}: {len(alle)} PDF(s) im Ordner, "
          f"{len(posten)} davon mit \"{a.nur}\" im Namen:\n")
    for name, groesse, datum in posten:
        fertig = os.path.isfile(os.path.join(ZIEL, os.path.splitext(name)[0] + ".txt"))
        print(f"  {'[schon eingelesen]' if fertig else '[neu]             '} "
              f"{groesse/1024/1024:6.1f} MB  {datum}  {name}")
    if a.liste:
        return
    print()

    for name, _, _ in posten:
        rumpf = os.path.splitext(name)[0]
        textziel = os.path.join(ZIEL, rumpf + ".txt")
        if os.path.isfile(textziel):
            continue
        pdfziel = os.path.join(ZIEL, name)
        print(f"  {name}: hole ...", flush=True)
        if not hole(seriennummer, name, pdfziel):
            print("      konnte nicht geholt werden")
            continue
        print("      lese ein ...", flush=True)
        text, seiten, woerter = lies_ein(pdfziel, a.sprache)
        io.open(textziel, "w", encoding="utf-8", newline="\n").write(text)
        print(f"      fertig: {seiten} Seiten, {woerter} Woerter -> {os.path.basename(textziel)}\n")

    print(f"Alles unter {ZIEL}")


if __name__ == "__main__":
    main()
