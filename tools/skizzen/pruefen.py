# -*- coding: utf-8 -*-
"""Prueft alle Zeichnungen unter design/skizzen/ auf die Fehler, die beim
Rendern erst als rote Fehlerseite auffallen.

WARUM ES DIESES SKRIPT GIBT: Der doppelte Bindestrich in einem XML-Kommentar
ist in Compass Zero dreimal aufgetreten (28. und 29.07.2026), obwohl die Regel
im Merkzettel steht. Ein Merksatz, den man dreimal bricht, ist kein Merksatz,
sondern eine fehlende Pruefung. Chromium zeigt dann eine Fehlerseite statt der
Zeichnung -- wer nur "ok" aus dem Renderer liest und nicht hinsieht, haelt die
kaputte Datei fuer fertig.

Aufruf:  python tools/skizzen/pruefen.py
Rueckgabe: 0 wenn alles sauber, 1 sonst.
"""
import glob
import os
import re
import sys
import xml.dom.minidom

ORDNER = os.path.join("design", "skizzen")


# Geschaetzte Breite je Zeichen, als Vielfaches der Schriftgroesse.
#
# WARUM NICHT PAUSCHAL: Ein fester Faktor ueber alle Zeichen ist zu ungenau.
# Am 03.08.2026 hat er einmal falsch angeschlagen ("Beine spreizen -- das
# zieht den Bund fest" hat viele schmale Buchstaben und passte doch) und
# einmal einen echten Ueberlauf durchgelassen ("keine Luft mehr -- keine
# Daemmung"). Mit Klassen statt eines Mittelwerts liegen beide richtig.
SCHMAL = set("iIl|.,;:'!ftj()[]")
BREIT = set("mMwW@")
GROSS = set("ABCDEFGHJKLNOPQRSTUVXYZÄÖÜ")


def textbreite(text, groesse):
    summe = 0.0
    for z in text:
        if z in SCHMAL:
            summe += 0.30
        elif z in BREIT:
            summe += 0.85
        elif z in GROSS:
            summe += 0.62
        elif z == " ":
            summe += 0.26
        else:
            summe += 0.50
    return summe * groesse


def _translate(wert):
    """Liest translate(dx, dy) aus einem transform-Wert.

    Alles andere (rotate, scale, matrix) wird als "unbekannt" gemeldet, statt
    so zu tun, als waere es null -- eine falsche Null waere schlimmer als ein
    Hinweis.
    """
    wert = (wert or "").strip()
    if not wert:
        return 0.0, 0.0, None
    treffer = re.fullmatch(r"translate\(\s*([-\d.]+)[\s,]+([-\d.]+)\s*\)", wert)
    if treffer:
        return float(treffer.group(1)), float(treffer.group(2)), None
    treffer = re.fullmatch(r"translate\(\s*([-\d.]+)\s*\)", wert)
    if treffer:
        return float(treffer.group(1)), 0.0, None
    return 0.0, 0.0, wert


def pruefe_texte(knoten, breite, hoehe, dx=0.0, dy=0.0, groesse=0.0, anker="start",
                 unbekannt=None):
    """Laeuft den Baum ab und meldet Beschriftungen ausserhalb der Flaeche.

    Verschiebung, Schriftgroesse und Ausrichtung werden vererbt, so wie sie
    auch beim Rendern vererbt werden.
    """
    fehler = []
    eigen_dx, eigen_dy, eigen_unbekannt = _translate(knoten.getAttribute("transform"))
    unbekannt = eigen_unbekannt or unbekannt
    dx += eigen_dx
    dy += eigen_dy
    if knoten.getAttribute("font-size"):
        groesse = float(knoten.getAttribute("font-size"))
    if knoten.getAttribute("text-anchor"):
        anker = knoten.getAttribute("text-anchor")

    if knoten.tagName == "text":
        inhalt = "".join(
            k.data for k in knoten.childNodes if k.nodeType == k.TEXT_NODE
        ).strip()
        if inhalt and knoten.getAttribute("x"):
            x = float(knoten.getAttribute("x")) + dx
            y = float(knoten.getAttribute("y") or 0) + dy
            if unbekannt:
                fehler.append(
                    'HINWEIS: "%s" steht unter transform="%s" -- die Lage laesst '
                    "sich so nicht schaetzen, bitte im gerenderten Bild nachsehen"
                    % (inhalt[:40], unbekannt)
                )
            elif groesse <= 0:
                fehler.append(
                    '"%s" hat weder selbst noch von seiner Gruppe eine '
                    "Schriftgroesse -- die Breite laesst sich nicht schaetzen"
                    % inhalt[:40]
                )
            else:
                w = textbreite(inhalt, groesse)
                if anker == "middle":
                    links, rechts = x - w / 2, x + w / 2
                elif anker == "end":
                    links, rechts = x - w, x
                else:
                    links, rechts = x, x + w
                # WAS DIESE PRUEFUNG NICHT KANN, und das gehoert hierher, damit
                # niemand ihr mehr glaubt, als sie leistet: Sie schaetzt aus
                # Zeichenklassen und liegt damit etwa fuenf Prozent daneben.
                # Zwei echte Faelle vom 03.08.2026 lagen beide INNERHALB dieser
                # Spanne -- einer passte knapp, der andere war knapp
                # abgeschnitten. Kein Zeichenzaehler trennt die beiden
                # zuverlaessig. Diese Pruefung fangt die GROBEN Ueberlaeufe.
                # Knappe Faelle sieht nur, wer das gerenderte Bild anschaut.
                if rechts > breite * 1.02:
                    fehler.append(
                        '"%s" reicht geschaetzt bis x=%.0f und damit ueber die '
                        "Breite %.0f hinaus" % (inhalt[:40], rechts, breite)
                    )
                elif rechts > breite * 0.985:
                    fehler.append(
                        'HINWEIS: "%s" endet geschaetzt bei x=%.0f, also dicht an '
                        "der Kante (Breite %.0f) -- im gerenderten Bild nachsehen"
                        % (inhalt[:40], rechts, breite)
                    )
                if links < -breite * 0.02:
                    fehler.append(
                        '"%s" beginnt geschaetzt bei x=%.0f und damit links '
                        "ausserhalb der Flaeche" % (inhalt[:40], links)
                    )
                if y > hoehe or y < 0:
                    fehler.append(
                        '"%s" liegt bei y=%.0f und damit ausserhalb der Flaeche '
                        "(Hoehe %.0f)" % (inhalt[:40], y, hoehe)
                    )

    for kind in knoten.childNodes:
        if kind.nodeType == kind.ELEMENT_NODE:
            fehler.extend(
                pruefe_texte(kind, breite, hoehe, dx, dy, groesse, anker, unbekannt)
            )
    return fehler


def pruefe(pfad):
    fehler = []
    with open(pfad, "r", encoding="utf-8") as f:
        text = f.read()

    # 1. Doppelter Bindestrich im Kommentar. XML verbietet ihn, weil "--" den
    #    Kommentar beenden koennte. Der Text wird als Gedankenstrich gemeint
    #    und bricht die ganze Datei.
    for treffer in re.finditer(r"<!--(.*?)-->", text, re.S):
        if "--" in treffer.group(1):
            zeile = text[:treffer.start()].count("\n") + 1
            fehler.append(
                "Zeile %d: doppelter Bindestrich im Kommentar -- in XML verboten, "
                "durch einen Gedankenstrich ersetzen" % zeile
            )

    # 2. Wohlgeformtheit insgesamt. Faengt fehlende Anfuehrungszeichen,
    #    nicht geschlossene Elemente und kaputte Entities.
    try:
        xml.dom.minidom.parseString(text)
    except Exception as e:  # noqa: BLE001 -- jede Parserklasse ist hier ein Fehler
        fehler.append("nicht wohlgeformt: %s" % e)

    # 3. Beschriftung, die aus der Zeichenflaeche laeuft.
    #
    # UMBAU AM 03.08.2026, und der Grund gehoert hierher: Die alte Fassung
    # arbeitete mit Textmustern. Sie suchte font-size AM <text> -- stand die
    # Groesse an der umgebenden Gruppe, lief die Pruefung stillschweigend gar
    # nicht und meldete "ok". Genau so ist "Schluesselbein" rechts aus dem Bild
    # gelaufen, ohne dass etwas anschlug. Und ihre Schaetzung der Verschiebung
    # zaehlte <g> und </g> in einer Zeichenkette; in mehreren Zeichnungen kam
    # dabei eine Verschiebung heraus, die es gar nicht gab -- Fehlalarme mit
    # x-Werten weit jenseits der Zeichenflaeche.
    #
    # Jetzt wird der Baum durchlaufen, den der Parser oben ohnehin schon baut.
    # Verschiebung, Schriftgroesse und Ausrichtung erbt jedes Element von seiner
    # Gruppe, genau wie beim Rendern. Damit entfaellt beides: das stille
    # Nichtpruefen und der Fehlalarm.
    kopf = re.search(r'<svg[^>]*viewBox="0 0 ([\d.]+) ([\d.]+)"', text)
    if kopf:
        breite, hoehe = float(kopf.group(1)), float(kopf.group(2))
        try:
            baum = xml.dom.minidom.parseString(text).documentElement
        except Exception:  # noqa: BLE001 -- schon oben gemeldet
            baum = None
        if baum is not None:
            fehler.extend(pruefe_texte(baum, breite, hoehe))

    return fehler


def main():
    dateien = sorted(glob.glob(os.path.join(ORDNER, "*.svg")))
    if not dateien:
        print("Keine Zeichnungen unter %s gefunden." % ORDNER)
        return 1
    schlimm = 0
    hinweise = 0
    for pfad in dateien:
        meldungen = pruefe(pfad)
        # Hinweise sind keine Fehler. Sie betreffen Beschriftungen, die
        # rechnerisch dicht an der Kante enden -- innerhalb der
        # Schaetzungenauigkeit. Sie sollen zum Bild schicken, nicht den Lauf
        # rot faerben; sonst gewoehnt man sich an rote Laeufe und uebersieht
        # den echten Fehler daneben.
        echte = [m for m in meldungen if not m.startswith("HINWEIS:")]
        weich = [m for m in meldungen if m.startswith("HINWEIS:")]
        if echte:
            schlimm += 1
            print("FEHLER %s" % pfad)
        elif weich:
            hinweise += 1
            print("hinweis %s" % pfad)
        else:
            print("ok     %s" % pfad)
        for m in echte + weich:
            print("   %s" % m)
    print(
        "\n%d Zeichnungen, %d mit Fehlern, %d mit Hinweisen."
        % (len(dateien), schlimm, hinweise)
    )
    return 1 if schlimm else 0


if __name__ == "__main__":
    sys.exit(main())
