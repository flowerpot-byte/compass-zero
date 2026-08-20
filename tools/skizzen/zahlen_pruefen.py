"""Haelt jede Zahl aus einer Zeichnung gegen den Text des Tipps, an dem sie haengt.

Warum es das gibt: Eine Zeichnung kann eine Zahl behaupten, die in keiner
Quelle steht — beim Kerzenziehen stand jahrelang "10. Mal", obwohl der Tipp
keine Zahl von Tauchgaengen nennt. So etwas sieht man beim Ansehen nicht,
weil die Zahl plausibel aussieht. Gefunden wird es nur durch Vergleichen.

Aufruf:
    python tools/skizzen/zahlen_pruefen.py            alle Zeichnungen
    python tools/skizzen/zahlen_pruefen.py kerzen     nur passende Namen

Ausgegeben werden nur Zahlen, die im Tipptext NICHT vorkommen — weder als
Ziffer noch als ausgeschriebenes Wort. Achsenbeschriftungen und
Schrittnummern tauchen dabei regelmaessig auf; sie sind harmlos, muessen aber
einmal angesehen werden. Der Rueckgabewert ist immer 0: Das hier ist ein
Werkzeug zum Hinsehen, keine Schranke im Bau.
"""
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

WURZEL = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
TIPPS = os.path.join(WURZEL, "content", "europe-de", "paket", "content", "tips.json")
SKIZZEN = os.path.join(WURZEL, "design", "skizzen")

# Zahlen, die eine Quelle ueblicherweise ausschreibt statt sie zu ziffern.
AUSGESCHRIEBEN = {
    "1": ["ein", "eine", "einmal", "erste", "erster", "erstes"],
    "2": ["zwei", "zweimal", "zweite", "beide"],
    "3": ["drei", "dreimal", "dritte"],
    "4": ["vier", "viermal", "vierte"],
    "5": ["fünf", "fuenf", "fünfmal"],
    "6": ["sechs", "sechsmal"],
    "7": ["sieben"],
    "8": ["acht"],
    "9": ["neun"],
    "10": ["zehn", "zehnmal"],
    "12": ["zwölf", "zwoelf"],
    "13": ["dreizehn"],
    "14": ["vierzehn"],
    "15": ["fünfzehn", "fuenfzehn"],
    "16": ["sechzehn"],
    "17": ["siebzehn"],
    "18": ["achtzehn"],
    "19": ["neunzehn"],
    "20": ["zwanzig"],
    "21": ["einundzwanzig"],
    "24": ["vierundzwanzig"],
    "28": ["achtundzwanzig"],
    "30": ["dreißig", "dreissig"],
    "48": ["achtundvierzig"],
    "50": ["fünfzig", "fuenfzig"],
    "60": ["sechzig"],
    "70": ["siebzig"],
    "72": ["zweiundsiebzig"],
    "90": ["neunzig"],
    "100": ["hundert"],
    "1000": ["tausend"],
    "3000": ["dreitausend"],
}


def sichtbarer_text(pfad):
    """Nur die Textknoten, nicht die Attribute — sonst hat man alle Koordinaten."""
    baum = ET.parse(pfad)
    stuecke = []
    for knoten in baum.iter():
        if knoten.tag.endswith("text") or knoten.tag.endswith("tspan"):
            if knoten.text:
                stuecke.append(knoten.text)
    return " ".join(stuecke)


def main():
    muster = sys.argv[1].lower() if len(sys.argv) > 1 else ""
    daten = json.load(open(TIPPS, encoding="utf-8"))
    tipps = daten["tips"] if isinstance(daten, dict) and "tips" in daten else daten

    je_bild = {}
    for tipp in tipps:
        if tipp.get("image"):
            je_bild.setdefault(os.path.basename(tipp["image"]), []).append(tipp)

    geprueft = auffaellig = 0
    for png in sorted(je_bild):
        name = os.path.splitext(png)[0]
        if muster and muster not in name.lower():
            continue
        svg = os.path.join(SKIZZEN, name + ".svg")
        if not os.path.exists(svg):
            print(f"?? {name}: keine Skizze gefunden")
            continue
        gefuehrt = je_bild[png]
        roh = " ".join(t["body"] + " " + t["title"] for t in gefuehrt).lower()
        # "2 400 Meter" und "2400 Meter" sind dieselbe Zahl: Trenner zwischen
        # Ziffern entfernen, sonst meldet das Werkzeug Treffer, die keine sind.
        text = roh + " " + re.sub(r"(?<=\d)[\s.  ](?=\d)", "", roh)
        try:
            zahlen = sorted(set(re.findall(r"\d+", sichtbarer_text(svg))), key=int)
        except ET.ParseError as fehler:
            print(f"!! {name}: nicht lesbar ({fehler})")
            continue
        geprueft += 1
        offen = [
            z for z in zahlen
            if z not in text and not any(w in text for w in AUSGESCHRIEBEN.get(z, []))
        ]
        if offen:
            auffaellig += 1
            wer = ", ".join(t["id"] for t in gefuehrt)
            print(f"{name}: {' '.join(offen)}")
            print(f"    haengt an {wer}")

    print()
    print(f"{geprueft} Zeichnungen geprueft, {auffaellig} mit ungedeckten Zahlen.")
    print("Achsenbeschriftungen und Schrittnummern sind hier normal — jede andere")
    print("Zahl gehoert angesehen, bevor sie im Paket bleibt.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
