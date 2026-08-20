# -*- coding: utf-8 -*-
"""Sucht im GANZEN Paket, nicht nur in den Titeln.

WARUM ES DIESES SKRIPT GIBT: Zweimal ist in diesem Projekt ein Tipp geschrieben
worden, den es schon gab -- und beide Male war die Dopplung nicht das Schlimme.

Am 29.07.2026 wurde nach "Amputation" gesucht, nichts gefunden und ein zweiter
Tipp geschrieben. Der vorhandene hiess "Abgetrennter Koerperteil". Die beiden
widersprachen sich fachlich: befeuchtetes Tuch gegen trocken halten.

Am 03.08.2026 entstand "Die erste Stunde nach der Geburt", obwohl es den Inhalt
laengst gab. Der neue Text empfahl die kraeftige Massage der Gebaermutter, die
der vorhandene Tipp einem Laien AUSDRUECKLICH VERBIETET. Eine bewusste
Sicherheitsentscheidung waere stillschweigend umgedreht worden.

Beide Male ist die Suche an den Titeln gescheitert: Die Aussage stand im
Fliesstext. Ein Merksatz dagegen steht seit dem 29.07. im Merkzettel und ist
trotzdem ein zweites Mal gebrochen worden -- also braucht es eine Pruefung und
keinen Merksatz.

Aufruf:
    python tools/inhalt/nachschlagen.py amputation
    python tools/inhalt/nachschlagen.py gebaermutter massage --lang
    python tools/inhalt/nachschlagen.py "fluss ueberqueren"   (wird zerlegt)

ZWEI DINGE, DIE DIESES SKRIPT NICHT BEHAUPTET:

*Es sucht keine Wortfolgen.* Eine Eingabe mit Leerzeichen wird in einzelne
Woerter zerlegt und jedes fuer sich gesucht. Grund: Am 04.08.2026 meldete
`nachschlagen.py "fluss ueberqueren"` mit voller Bestimmtheit, das Thema fehle
-- der Tipp heisst "Einen Fluss durchqueren". Die Wortfolge kam nicht vor, das
Thema sehr wohl. Ein Werkzeug, das gegen falsche Gewissheit gebaut ist, darf
nicht selbst welche erzeugen.

*Es findet keine Beugungsformen.* Verglichen werden Zeichenketten, nicht
Wortstaemme: "erfrieren" steckt nicht in "Erfrierungen". Bleibt ein Wort ohne
Treffer, wird deshalb automatisch mit einem kuerzeren Stamm nachgesucht und
das Ergebnis als solches gekennzeichnet.

*Und es ist NICHT die Suche der App.* Das ist der dritte Punkt, und er hat am
17.08.2026 zu einer falschen Meldung gefuehrt: Hier stand "Schuerfwunde" fuehrt
zuerst auf "Offene Brustwunde" -- in der App fuehrt es auf "Ausspuelen statt
auswischen", also richtig. Dieses Skript findet VORKOMMEN im Volltext und
sortiert nach eigenen Regeln; die App benutzt `SearchIndex` mit ganz anderer
Gewichtung (Titel 5, Schlagwort 3, Fliesstext 1) und vergleicht Wortanfaenge.

Daraus folgt die Arbeitsteilung:
  - "Gibt es das Thema schon irgendwo?"  -> dieses Skript.
  - "Was zeigt die App zuerst?"          -> nur ueber `SearchIndex` messen,
                                            und mit Umlauten so, wie ein Mensch
                                            sie tippt. Die App faltet ue erst
                                            intern; wer "schuerfwunde" eingibt,
                                            sucht ein anderes Wort.
"""
import glob
import json
import os
import re
import sys
import unicodedata

PAKET = os.path.join("content", "europe-de", "paket", "content")


def entfalte(text):
    """Macht Umlaute und Schreibweisen vergleichbar."""
    text = text.lower()
    for alt, neu in (("ä", "ae"), ("ö", "oe"), ("ü", "ue"), ("ß", "ss")):
        text = text.replace(alt, neu)
    return unicodedata.normalize("NFKD", text)


def eintraege():
    """Alle Eintraege aus allen Inhaltsdateien, mit ihrem ganzen Text."""
    aus = []
    for pfad in sorted(glob.glob(os.path.join(PAKET, "*.json"))):
        name = os.path.basename(pfad)
        try:
            daten = json.load(open(pfad, encoding="utf-8"))
        except ValueError as fehler:
            print("  (%s nicht lesbar: %s)" % (name, fehler))
            continue
        listen = []
        if isinstance(daten, dict):
            for schluessel in ("tips", "guides", "chapters", "phrases"):
                if isinstance(daten.get(schluessel), list):
                    listen.append((schluessel, daten[schluessel]))
        elif isinstance(daten, list):
            listen.append((name, daten))
        for art, liste in listen:
            for e in liste:
                if not isinstance(e, dict):
                    continue
                stuecke = []
                for feld in ("title", "summary", "body", "note", "detail"):
                    wert = e.get(feld)
                    if isinstance(wert, str):
                        stuecke.append(wert)
                for feld in ("keywords", "materials", "tools"):
                    wert = e.get(feld)
                    if isinstance(wert, list):
                        stuecke.extend(str(x) for x in wert)
                for schritt in e.get("steps", []) or []:
                    if isinstance(schritt, dict):
                        for feld in ("title", "text"):
                            if isinstance(schritt.get(feld), str):
                                stuecke.append(schritt[feld])
                # Die Agrikultur-Kapitel tragen ihren ganzen Text in "sections"
                # und NICHT in "body". Ohne diese Schleife durchsucht das
                # Werkzeug bei einem Kapitel nur den TITEL -- und meldet dann
                # "kein Treffer" fuer alles, was im Fliesstext steht. Gefunden
                # am 10.08.2026: "glucke" und "kotbrett" stehen im
                # Huehner-Kapitel und wurden trotzdem nicht gefunden.
                for abschnitt in e.get("sections", []) or []:
                    if isinstance(abschnitt, dict):
                        for feld in ("heading", "body", "text", "title"):
                            if isinstance(abschnitt.get(feld), str):
                                stuecke.append(abschnitt[feld])
                aus.append({
                    "datei": name,
                    "art": art,
                    "titel": e.get("title", e.get("id", "?")),
                    "kategorie": e.get("category", ""),
                    "text": "\n".join(stuecke),
                })
    return aus


def treffer_zu(alle, wort):
    muster = re.compile(re.escape(entfalte(wort)))
    aus = []
    for e in alle:
        stellen = [m.start() for m in muster.finditer(entfalte(e["text"]))]
        if stellen:
            aus.append((e, stellen))
    return muster, aus


# Wie weit beim Nachfassen gekuerzt werden darf.
#
# ENG BEGRENZT, UND DAS IST DER PUNKT: Der erste Anlauf am 04.08.2026 kuerzte
# bis auf fuenf Zeichen herunter. Damit meldete "rettungsinsel" einen Treffer,
# weil "rettu" in "Rettungsdienst" steckt -- das Thema Seenot fehlt im Paket
# aber wirklich. Aus dem falschen Negativ war ein falsches Positiv geworden,
# und das ist nicht besser, sondern schlechter: Ein "gibt es schon" fuehrt
# dazu, dass gar nicht erst geschrieben wird.
STAMM_MIN = 6
STAMM_MAX_KUERZUNG = 3


def suche(woerter, lang=False):
    alle = eintraege()
    print("Durchsucht: %d Eintraege aus %s\n"
          % (len(alle), os.path.join(PAKET, "*.json")))
    genau = 0
    for wort in woerter:
        print("=" * 62)
        print("SUCHE: %s" % wort)
        print("=" * 62)
        muster, treffer = treffer_zu(alle, wort)

        if treffer:
            genau += len(treffer)
        else:
            # Beugungsformen: "erfrieren" steckt nicht in "Erfrierungen".
            # Deshalb mit einem kuerzeren Stamm nachfassen, bevor behauptet
            # wird, das Thema fehle -- aber nur ein paar Zeichen weit.
            stamm = None
            kandidat = entfalte(wort)
            weggenommen = 0
            while (weggenommen < STAMM_MAX_KUERZUNG
                   and len(kandidat) - 1 >= STAMM_MIN and not treffer):
                kandidat = kandidat[:-1]
                weggenommen += 1
                muster, treffer = treffer_zu(alle, kandidat)
                if treffer:
                    stamm = kandidat
            if not treffer:
                print("  Kein Treffer -- auch nicht fuer einen kuerzeren Stamm.")
                print("  Das ist ein starker Hinweis, dass das Thema fehlt, aber")
                print("  KEIN Beweis: Ein Tipp kann es unter einem ganz anderen")
                print("  Wort fuehren. Probier das Alltagswort neben dem Fachwort")
                print("  (Hypertonie/Bluthochdruck, Epistaxis/Nasenbluten).\n")
                continue
            print("  KEIN genauer Treffer. Nur der Wortstamm \"%s\" kommt vor."
                  % stamm)
            print("  Das kann eine andere Beugungsform sein -- oder ein ganz")
            print("  anderes Wort, das zufaellig so anfaengt. Sieh selbst nach:\n")
        # Titeltreffer zuerst: Wer das Wort im Titel traegt, ist der Eintrag,
        # dem das Thema gehoert.
        treffer.sort(key=lambda p: (0 if muster.search(entfalte(p[0]["titel"]))
                                    else 1, -len(p[1])))
        for e, stellen in treffer:
            imTitel = " [IM TITEL]" if muster.search(entfalte(e["titel"])) else ""
            print("\n  %s%s" % (e["titel"], imTitel))
            print("    %s / %s%s  %dx"
                  % (e["datei"], e["art"],
                     (" / " + e["kategorie"]) if e["kategorie"] else "",
                     len(stellen)))
            wieviele = len(stellen) if lang else min(2, len(stellen))
            for s in stellen[:wieviele]:
                roh = e["text"]
                a = max(0, s - 110)
                b = min(len(roh), s + len(wort) + 150)
                stueck = " ".join(roh[a:b].split())
                print("      …%s…" % stueck)
        print()
    # Rueckgabewert 0 heisst: das WORT steht so im Paket. Ein Treffer, der nur
    # ueber den gekuerzten Stamm zustande kam, zaehlt ausdruecklich NICHT --
    # sonst meldete "rettungsinsel" wegen "Rettungsdienst" einen Erfolg.
    # Rueckgabewert 0 heisst: das WORT steht so im Paket. Ein Treffer, der nur
    # ueber den gekuerzten Stamm zustande kam, zaehlt ausdruecklich NICHT --
    # sonst meldete "rettungsinsel" wegen "Rettungsdienst" einen Erfolg, und
    # dann wird ein fehlendes Thema fuer vorhanden gehalten.
    return 0 if genau else 1


def main():
    roh = [a for a in sys.argv[1:] if not a.startswith("--")]
    # Eine Eingabe mit Leerzeichen wird ZERLEGT und nicht als Wortfolge
    # gesucht -- siehe Kopf dieser Datei. "fluss ueberqueren" hat sonst
    # gemeldet, das Thema fehle, waehrend der Tipp "Einen Fluss durchqueren"
    # danebenstand.
    woerter = []
    for stueck in roh:
        woerter.extend(w for w in stueck.split() if w)
    if len(woerter) > len(roh):
        print("Eingabe zerlegt in: %s" % ", ".join(woerter))
        print("(Wortfolgen werden nicht gesucht -- jedes Wort fuer sich.)\n")
    if not woerter:
        print(__doc__)
        return 2
    if not os.path.isdir(PAKET):
        print("Nicht im Projektwurzelverzeichnis? %s fehlt." % PAKET)
        return 2
    return suche(woerter, lang="--lang" in sys.argv)


if __name__ == "__main__":
    sys.exit(main())
