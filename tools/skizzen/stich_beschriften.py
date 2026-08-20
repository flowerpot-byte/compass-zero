# -*- coding: utf-8 -*-
"""Setzt eine gereinigte Stichzeichnung aus einer Quelle auf ein Blatt und
beschriftet sie deutsch.

WARUM ES DAS GIBT: Die Skizzen des Pakets waren aus einfachen Formen von Hand
zusammengesetzt. Max hat am 11.08.2026 genau das beanstandet -- wenig Detail,
und weil jede Form von Hand gesetzt ist, ist jede Form auch von Hand falsch
setzbar. Die Hefte, aus denen der Text stammt, enthalten dagegen echte
Stichzeichnungen. Das Detail kommt dann vom Stecher, die Beschriftung von uns.

DREI DINGE, DIE HIER SCHON SCHIEFGEGANGEN SIND und deshalb im Code stehen:

1. Der eingesetzte Stich hat einen WEISSEN Hintergrund und ueberdeckt beim
   Einfuegen alles, was vorher gezeichnet wurde. Deshalb wird der Stich ZUERST
   gesetzt und erst danach beschriftet.
2. Umlaute gehen verloren, wenn das Skript als Text an PowerShell uebergeben
   wird. Deshalb liegt es als Datei in UTF-8 und wird als Datei aufgerufen.
3. Beschriftung, die ueber dem Bild landet, ist unlesbar. Die Beschriftung
   steht deshalb in den Raendern, und das Bild wird so weit verkleinert, dass
   die Raender frei bleiben.
"""
import sys

from PIL import Image, ImageDraw, ImageFont

BLATT = (250, 248, 244)
TINTE = (20, 20, 20)
LEISE = (105, 105, 105)
LINIE = (90, 107, 122)


def schrift(groesse, fett=False):
    pfad = r"C:\Windows\Fonts\georgiab.ttf" if fett else r"C:\Windows\Fonts\georgia.ttf"
    try:
        return ImageFont.truetype(pfad, groesse)
    except OSError:
        return ImageFont.load_default()


def speichern(blatt, ziel, farben=32):
    """Legt das Blatt mit fester Farbtafel ab. Ein Blatt aus Papierton, Tinte,
    Führungslinien und einem Scan braucht keine 16 Millionen Farben: mit 32
    fällt kein Unterschied auf, die Datei wird aber rund ein Drittel so groß.
    Das zählt, weil das Paket vollständig auf dem Handy liegt.

    ACHTUNG: Die älteren Figuren oben speichern noch direkt mit blatt.save()
    und liefern deshalb RGB, obwohl die zugehoerigen PNG im Paket als Farbtafel
    abgelegt sind -- sie wurden seinerzeit von Hand umgewandelt. Wer sie neu
    erzeugt, bekommt also größere Dateien als die eingecheckten. Das ist
    bekannt und bewusst nicht im Alleingang geändert worden.
    """
    blatt.convert("RGB").quantize(colors=farben, method=Image.MEDIANCUT).save(
        ziel, optimize=True)
    return blatt.size


def blatt_mit_stich(stichpfad, breite=1100, hoehe=700, rand=290, oben=200):
    """Legt den Stich mittig auf ein Blatt und laesst links und rechts Rand."""
    # Das Weiss des Scans wird auf den Papierton des Blattes gezogen, sonst
    # steht der Stich als heller Kasten auf dem Blatt. Grauwerte bleiben, damit
    # die Punktierung des Stichs nicht verlorengeht.
    grau = Image.open(stichpfad).convert("L")
    stich = Image.merge("RGB", [
        grau.point([min(255, int(v * BLATT[k] / 255)) for v in range(256)])
        for k in range(3)
    ])
    hoechstbreite = breite - 2 * rand + 260
    if stich.width > hoechstbreite:
        neu = (hoechstbreite, int(stich.height * hoechstbreite / stich.width))
        stich = stich.resize(neu, Image.LANCZOS)
    blatt = Image.new("RGB", (breite, hoehe), BLATT)
    x = (breite - stich.width) // 2
    blatt.paste(stich, (x, oben))
    return blatt, (x, oben, stich.width, stich.height)


def beschrifte(blatt, kasten, titel, untertitel, marken, fusszeile, quelle,
               schriftgroesse=15):
    """schriftgroesse bezieht sich auf die Blattbreite, nicht auf das spaetere
    Bild. Massstab: Auf einem 900 Pixel breiten Blatt sind 14 Pixel gerade noch
    am Handy zu lesen -- das Verhaeltnis 0,0155 muss also erhalten bleiben. Ein
    hohes, fast quadratisches Blatt wird auf dem Schirm staerker verkleinert
    als ein breites, und dann fallen 15 Pixel unter diese Schwelle."""
    z = ImageDraw.Draw(blatt)
    B, H = blatt.size
    x, y, sb, sh = kasten

    z.text((B // 2, 36), titel, font=schrift(27, True), fill=TINTE, anchor="mm")
    z.text((B // 2, 66), untertitel, font=schrift(15), fill=LEISE, anchor="mm")

    klein = schrift(schriftgroesse)
    zeilenhoehe = schriftgroesse + 5
    for zeilen, tx, ty, zx, zy, rechts in marken:
        # Die Fuehrungslinie beginnt NEBEN der Beschriftung, nicht an ihrem
        # Anfang -- sonst laeuft sie bei zweizeiligen Marken mitten durch die
        # Schrift und sieht aus wie ein Durchstreichen.
        breiteste = max(z.textlength(zeile, font=klein) for zeile in zeilen)
        anfang = tx - breiteste - 10 if rechts else tx + breiteste + 10
        z.line([(anfang, ty), (x + zx, y + zy)], fill=LINIE, width=1)
        hoch = (len(zeilen) - 1) * zeilenhoehe // 2
        for i, zeile in enumerate(zeilen):
            z.text((tx, ty - hoch + i * zeilenhoehe), zeile, font=klein, fill=TINTE,
                   anchor="rm" if rechts else "lm")

    z.text((B // 2, y + sh + 26), quelle, font=schrift(13), fill=(130, 130, 130), anchor="mm")

    kw = schrift(16)
    breite = z.textlength(fusszeile, font=kw)
    z.rectangle([((B - breite) / 2 - 22, H - 62), ((B + breite) / 2 + 22, H - 22)],
                fill=(242, 239, 232), outline=(201, 194, 182))
    z.text((B // 2, H - 42), fusszeile, font=kw, fill=TINTE, anchor="mm")
    return blatt


def meiler_lang(stichpfad, ziel):
    blatt, kasten = blatt_mit_stich(stichpfad)
    x, y, sb, sh = kasten
    marken = [
        (["Zwei Reihen Pfosten halten", "den Wall seitlich zusammen;",
          "Abstand höchstens 1,20 m"], 40, 150, int(sb * 0.18), int(sh * 0.22), False),
        (["Scheitholz liegend", "zwischen die Pfosten", "geschichtet"],
         40, 520, int(sb * 0.30), int(sh * 0.72), False),
        (["Erddecke über allem"], 1060, 150, int(sb * 0.72), int(sh * 0.10), True),
        (["Abzugsöffnungen", "oben auf dem First"], 1060, 520, int(sb * 0.86), int(sh * 0.18), True),
    ]
    beschrifte(
        blatt, kasten,
        "Der längliche Meiler",
        "Wall aus Scheitholz zwischen zwei Pfostenreihen",
        marken,
        "Länge 12 bis 15 m  ·  fasst 12 bis 14 Fuhren Holz",
        "nach der Stichzeichnung in Overman, „The Manufacture of Iron“, 1854, Figur 28",
    )
    blatt.save(ziel)
    return blatt.size


def raeucherkammer(stichpfad, ziel):
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1500, hoehe=1200, rand=460, oben=190)
    x, y, sb, sh = kasten
    marken = [
        (["Lüfter im Dach"], 40, 250, int(sb * 0.44), int(sh * 0.03), False),
        (["Stangen zum Aufhängen"], 40, 400, int(sb * 0.40), int(sh * 0.17), False),
        (["Tür (beide Seiten offen)"], 40, 590, int(sb * 0.08), int(sh * 0.36), False),
        (["Rauchverteiler:", "unten offen, gelocht"],
         40, 840, int(sb * 0.42), int(sh * 0.70), False),
        (["Abnehmbarer Deckel"], 1460, 250, int(sb * 0.64), int(sh * 0.06), True),
        (["Horden statt Haken"], 1460, 470, int(sb * 0.56), int(sh * 0.34), True),
        (["Rauch steigt ringsum auf"], 1460, 700, int(sb * 0.50), int(sh * 0.60), True),
        (["Rohr im Boden,", "Feuer liegt außerhalb"],
         1460, 990, int(sb * 0.82), int(sh * 0.97), True),
    ]
    beschrifte(
        blatt, kasten,
        "Räucherkammer mit ausgelagertem Feuer",
        "für Kalträuchern: die Kammer bleibt unter 43 Grad",
        marken,
        "innen rund 107 x 107 cm, 213 cm hoch",
        "nach der Zeichnung in „A Practical Small Smokehouse for Fish“, "
        "U.S. Bureau of Fisheries, 1917, Seite 9",
        schriftgroesse=23,
    )
    blatt.save(ziel)
    return blatt.size


def stampflehm_schalung(stichpfad, ziel):
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1500, hoehe=900, rand=460, oben=170)
    x, y, sb, sh = kasten
    marken = [
        (["Bohlen, 5 cm stark"], 40, 250, int(sb * 0.30), int(sh * 0.30), False),
        (["Ständer außen, Klammern", "halten die Bohlen"],
         40, 430, int(sb * 0.17), int(sh * 0.45), False),
        (["Zugstange quer hindurch"], 40, 620, int(sb * 0.09), int(sh * 0.75), False),
        (["Endbrett mit Leiste:", "formt die Verzahnung", "zum nächsten Abschnitt"],
         1460, 260, int(sb * 0.90), int(sh * 0.14), True),
        (["rund 90 cm hoch,", "so lang wie greifbar"], 1460, 520, int(sb * 0.93), int(sh * 0.55), True),
    ]
    beschrifte(
        blatt, kasten,
        "Schalung für eine Stampflehmwand",
        "gerades Stück; an Ecken greift dieselbe Schalung um die Ecke",
        marken,
        "Wandstärke unten 46 cm  ·  Erde in Lagen von 10 bis 13 cm einstampfen",
        "nach Figur 7 in „Rammed Earth Walls for Buildings“, "
        "USDA Farmers' Bulletin 1500, Fassung 1937",
        schriftgroesse=23,
    )
    blatt.save(ziel)
    return blatt.size


def feldsteinmauer(stichpfad, ziel):
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1500, hoehe=800, rand=460, oben=190)
    x, y, sb, sh = kasten
    marken = [
        (["Läufer und Binder", "wechseln sich ab"],
         40, 300, int(sb * 0.55), int(sh * 0.16), False),
        (["Die größten und besten", "Steine gehören nach unten"],
         40, 560, int(sb * 0.14), int(sh * 0.86), False),
        (["... und ebenso an die Ecke"], 1460, 300, int(sb * 0.42), int(sh * 0.60), True),
        (["Rundes Feldgestein hält schlecht:", "darum muss die Mauer dick sein"],
         1460, 560, int(sb * 0.80), int(sh * 0.45), True),
    ]
    beschrifte(
        blatt, kasten,
        "Mauer aus rundem Feldstein",
        "die Ecke ist die Stelle, an der eine solche Mauer steht oder fällt",
        marken,
        "mindestens 46 cm dick  ·  alle 1,2 bis 1,5 m ein durchgehender Binder",
        "nach Figur 21 in „Elements of Stone Masonry“, "
        "International Correspondence Schools, 1907/1909",
        schriftgroesse=23,
    )
    blatt.save(ziel)
    return blatt.size


def ofenrohr_anschluss(stichpfad, ziel):
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1500, hoehe=760, rand=430, oben=190)
    x, y, sb, sh = kasten
    marken = [
        (["FALSCH: das Rohr ragt", "in den Zug hinein —", "der Rauch staut sich"],
         40, 330, int(sb * 0.20), int(sh * 0.48), False),
        (["RICHTIG: das Rohr endet", "bündig mit der Zugwand —", "freier Weg nach oben"],
         1460, 330, int(sb * 0.78), int(sh * 0.42), True),
    ]
    beschrifte(
        blatt, kasten,
        "Ofenrohr am Schornstein anschließen",
        "der häufigste Fehler und seine Behebung",
        marken,
        "waagerecht anschließen  ·  kein Spalt ringsum  ·  Fuge dicht verschmieren",
        "nach Figur 8 in „Construction of Chimneys and Fireplaces“, "
        "USDA Farmers' Bulletin 1649, Fassung 1933",
        schriftgroesse=23,
    )
    blatt.save(ziel)
    return blatt.size


def kalkstapel(stichpfad, ziel):
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1500, hoehe=780, rand=460, oben=190)
    x, y, sb, sh = kasten
    marken = [
        (["Grüne Stämme unten: sie brennen", "schlecht und halten den Stapel", "vom Boden ab"],
         40, 300, int(sb * 0.14), int(sh * 0.88), False),
        (["Abgelagerte Stämme", "quer darüber"], 40, 560, int(sb * 0.22), int(sh * 0.73), False),
        (["Stein und Holz im Wechsel,", "nach oben schmaler"],
         1460, 300, int(sb * 0.60), int(sh * 0.33), True),
        (["Angezündet wird in der Mitte —", "der Stapel brennt nach außen"],
         1460, 560, int(sb * 0.46), int(sh * 0.92), True),
    ]
    beschrifte(
        blatt, kasten,
        "Kalkstapel: der Ofen aus Holz und Stein",
        "gebrannt wird ohne festen Ofen, der Stapel wird danach abgetragen",
        marken,
        "Grundfläche 3,7 bis 5 m breit  ·  Stapel rund 2,4 m hoch  ·  Lücken 60 cm",
        "nach Figur 3 in „Making Lime on the Farm“, "
        "USDA Farmers' Bulletin 1801, 1938",
        schriftgroesse=23,
    )
    blatt.save(ziel)
    return blatt.size


def geschirr_schlitten(stichpfad, ziel):
    # Die Quelle laesst ihren eigenen Pfeil zum Schnellverschluss stehen: er
    # zeigt genauer auf die Schnalle, als eine Führungslinie von außen es
    # könnte, ohne quer über den Gurt zu laufen.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1400, hoehe=880, rand=430, oben=175)
    x, y, sb, sh = kasten
    marken = [
        (["Schulterriemen, verstellbar —", "hält den Gurt auf Hüfthöhe"],
         40, 250, int(sb * 0.12), int(sh * 0.10), False),
        (["Karabinerhaken zum", "Einhängen des Seils"],
         40, 560, int(sb * 0.09), int(sh * 0.43), False),
        (["Schnellverschluss: damit", "kommt man sofort heraus"],
         1360, 210, int(sb * 0.45), int(sh * 0.03), True),
        (["Metallring vorn", "und hinten am Gurt"],
         1360, 360, int(sb * 0.87), int(sh * 0.33), True),
        (["Zugseil, rund 2,75 m", "(Quelle: 9 Fuß)"],
         1360, 680, int(sb * 0.90), int(sh * 0.72), True),
    ]
    beschrifte(
        blatt, kasten,
        "Zuggeschirr für einen Menschen",
        "die Last hängt an Hüfte und Schulter, nicht am Arm",
        marken,
        "Zugseil rund 2,75 m  ·  Schnappverschluss an jedem Ende",
        "nach Figur 4-36 b in „Basic Cold Weather Manual“, "
        "Department of the Army, FM 31-70, 1968, Seite 98",
        schriftgroesse=22,
    )
    return speichern(blatt, ziel)


def zisterne_schnitt(stichpfad, ziel):
    # Die Marke am Zulauf sagt ausdrücklich, wo das Sieb NICHT hingehört:
    # Genau daran scheitert die Sache in der Praxis, und die Zeichnung der
    # Quelle zeigt zufällig beides -- das gesiebte Zulaufrohr am Behälter und
    # das blanke Fallrohr darüber.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1700, hoehe=1400, rand=480, oben=170)
    x, y, sb, sh = kasten
    marken = [
        (["Zulauf mit Sieb — hier,", "nicht oben am Fallrohr"],
         40, 240, int(sb * 0.16), int(sh * 0.04), False),
        (["Zulaufrohr", "durch die Wand"], 40, 520, int(sb * 0.16), int(sh * 0.26), False),
        (["Überlauf nahe", "der Oberkante"], 40, 740, int(sb * 0.25), int(sh * 0.39), False),
        (["zur Pumpe"], 1660, 620, int(sb * 0.82), int(sh * 0.35), True),
        (["Filterwand — nur", "diese Bauart hat eine,", "die Anleitung nicht"],
         1660, 880, int(sb * 0.62), int(sh * 0.57), True),
        (["Sieb am Fuß", "des Saugrohrs"], 1660, 1150, int(sb * 0.80), int(sh * 0.92), True),
    ]
    beschrifte(
        blatt, kasten,
        "Zisterne im Schnitt",
        "eingegraben gegen Frost, lichtdicht gegen Algen",
        marken,
        "Betonwand rund 20 cm (Quelle: 8 Zoll)  ·  innen begehbar zum Reinigen",
        "nach Figur 22 in „Safe Water for the Farm“, "
        "USDA Farmers' Bulletin 1978, 1948, Seite 44",
        schriftgroesse=27,
    )
    return speichern(blatt, ziel)


def bett_schlafstelle(stichpfad, ziel):
    # Sehr breite Figur (rund 2,4 zu 1). Deshalb wird sie bewusst kleiner
    # gesetzt, als das Blatt hergaebe: Sonst reicht der Rand nicht fuer die
    # Marken, und die Schrift laeuft ins Bild.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1900, hoehe=800, rand=560, oben=170)
    x, y, sb, sh = kasten
    marken = [
        (["Bindung an der Ecke"], 40, 300, int(sb * 0.20), int(sh * 0.37), False),
        (["Eckpfosten, tief", "genug eingegraben"], 40, 560, int(sb * 0.155), int(sh * 0.62), False),
        (["Laub oder Gras darauf"], 1860, 250, int(sb * 0.52), int(sh * 0.13), True),
        (["Liegestangen quer"], 1860, 400, int(sb * 0.26), int(sh * 0.32), True),
        (["Eckpfosten"], 1860, 560, int(sb * 0.765), int(sh * 0.62), True),
    ]
    beschrifte(
        blatt, kasten,
        "Schlafstelle über dem Boden",
        "vier Pfosten, ein gebundener Rahmen, Querstangen darauf",
        marken,
        "Die Quelle nennt keine Maße — Höhe und Länge richten sich nach dem, was da ist",
        "nach Figur 5-10 in „FM 3-05.70 Survival“, "
        "Department of the Army, 2002, Seite 5-15",
        schriftgroesse=30,
    )
    return speichern(blatt, ziel)


def kornsieb_kasten(stichpfad, ziel):
    # Die Vorlage zeigt MEHR, als die Anleitung beschreibt: Trichter, Schieber
    # und die zwei Bremslamellen kommen im Text nicht vor. Sie sind trotzdem
    # beschriftet -- ein stillschweigend mitgezeichnetes Bauteil ist schlimmer
    # als ein benanntes -- und die Fusszeile sagt es ausdruecklich.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1700, hoehe=1050, rand=520, oben=170)
    x, y, sb, sh = kasten
    marken = [
        (["Drahtgeflecht:", "die Siebfläche"], 40, 330, int(sb * 0.35), int(sh * 0.30), False),
        (["Rahmen des Kastens"], 40, 640, int(sb * 0.169), int(sh * 0.65), False),
        (["Trichter zum Einfüllen"], 1660, 250, int(sb * 0.68), int(sh * 0.075), True),
        (["Schieber regelt,", "wie viel nachläuft"], 1660, 430, int(sb * 0.535), int(sh * 0.172), True),
        (["Zwei Lamellen bremsen", "das Korn auf dem Weg"], 1660, 700, int(sb * 0.485), int(sh * 0.477), True),
    ]
    beschrifte(
        blatt, kasten,
        "Kornsieb als geneigter Kasten",
        "das Korn läuft durch, das Grobe bleibt oben liegen",
        marken,
        "Trichter, Schieber und Lamellen hat nur diese Bauart — die Anleitung beschreibt sie nicht",
        "nach Figur 243 in „An Encyclopaedia of Agriculture“, "
        "John Claudius Loudon, 1831, Seite 378",
        schriftgroesse=27,
    )
    return speichern(blatt, ziel)


def kerbleiter(stichpfad, ziel):
    # Hochformat: Die Figur ist neunmal so hoch wie breit; ausgeschnitten ist
    # nur der obere Abschnitt, sonst waere auf einem Handy nichts mehr zu
    # erkennen.
    #
    # WICHTIG: Der Text der Anleitung sagt, der Stamm werde AUF BEIDEN SEITEN
    # versetzt gekerbt. In dieser Seitenansicht ist davon nur EINE Reihe zu
    # sehen -- die zweite liegt auf der abgewandten Seite des runden Stamms.
    # Die Marken duerfen deshalb nicht so tun, als zeige die Zeichnung den
    # Wechsel; die Fusszeile sagt es stattdessen ausdruecklich.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1000, hoehe=1620, rand=330, oben=170)
    x, y, sb, sh = kasten
    marken = [
        (["Oberes Stammende"], 40, 230, int(sb * 0.55), int(sh * 0.03), False),
        (["Der schräge Schnitt", "läuft von oben herein"],
         40, 560, int(sb * 0.30), int(sh * 0.26), False),
        (["Die Trittfläche liegt", "waagerecht"], 40, 900, int(sb * 0.22), int(sh * 0.42), False),
        (["Die nächste Kerbe", "derselben Reihe"], 960, 1120, int(sb * 0.30), int(sh * 0.87), True),
    ]
    beschrifte(
        blatt, kasten,
        "Kerbleiter aus einem Stamm",
        "die Quelle nennt weder Stammdicke noch Holzart, nur „Stange oder Stamm“",
        marken,
        "Die zweite Kerbenreihe sitzt versetzt auf der abgewandten Seite — hier nicht zu sehen",
        "nach Figur 170 in „Shelters, Shacks, and Shanties“, "
        "Charles Scribner's Sons, 1914, Seite 142",
        schriftgroesse=30,
    )
    return speichern(blatt, ziel)


def schubkarre(stichpfad, ziel):
    # Die Quelle zeichnet ein FERTIGES SPEICHENRAD -- also genau das, was die
    # Anleitung ausdruecklich nicht hergibt. Ohne den Zusatz an der Marke
    # wirkte das Bild wie ein Beleg fuer etwas, das der Text als Luecke
    # benennt. Deshalb steht es an der Marke UND in der Fusszeile.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1800, hoehe=880, rand=420, oben=165)
    x, y, sb, sh = kasten
    marken = [
        (["Kasten: vorn niedrig,", "hinten höher"], 40, 260, int(sb * 0.28), int(sh * 0.08), False),
        (["Achse, im Rahmen", "gelagert"], 40, 560, int(sb * 0.213), int(sh * 0.50), False),
        (["Rad — dieses baut", "die Anleitung NICHT"], 40, 700, int(sb * 0.14), int(sh * 0.56), False),
        (["Hintere Kastenecke"], 1760, 240, int(sb * 0.586), int(sh * 0.06), True),
        (["Griffende zum Schieben"], 1760, 480, int(sb * 0.95), int(sh * 0.25), True),
        (["Standbein — im Text", "nicht beschrieben"],
         1760, 700, int(sb * 0.567), int(sh * 0.96), True),
    ]
    beschrifte(
        blatt, kasten,
        "Erd- und Steinkarre",
        "Kasten, Rahmen und Achse von Hand — das Rad nicht",
        marken,
        "Seitenansicht: dass der Kasten vorn spitz zuläuft, ist von oben zu sehen, hier nicht",
        "nach Figur 247 in „An Encyclopaedia of Agriculture“, "
        "J. C. Loudon, 1831, Seite 379",
        schriftgroesse=28,
    )
    return speichern(blatt, ziel)


def brettertuer(stichpfad, ziel):
    # Die Quelle nennt A und C ausdruecklich waagerecht; B ist zweiteilig und
    # laeuft schraeg, gehoert aber zum VERSCHLUSS, nicht zur Aussteifung. Genau
    # das sagen die Marken -- ein schraeges Teil auf einem Tuerblatt sieht sonst
    # aus wie eine Strebe gegen das Durchhaengen, und die hat diese Tuer nicht.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1300, hoehe=1420, rand=470, oben=170)
    x, y, sb, sh = kasten
    marken = [
        (["Leiste A, waagerecht"], 40, 280, int(sb * 0.30), int(sh * 0.13), False),
        (["Holzangel"], 40, 480, int(sb * 0.09), int(sh * 0.30), False),
        (["Leiste B, zweiteilig —", "gehört zum Verschluss,", "nicht zur Aussteifung"],
         40, 800, int(sb * 0.46), int(sh * 0.58), False),
        (["Leiste C, waagerecht"], 40, 1120, int(sb * 0.35), int(sh * 0.87), False),
        (["Der hölzerne Riegel"], 1260, 540, int(sb * 0.66), int(sh * 0.43), True),
        (["Fänger am Rahmen"], 1260, 720, int(sb * 0.87), int(sh * 0.45), True),
    ]
    beschrifte(
        blatt, kasten,
        "Brettertür von innen",
        "Bretter nebeneinander, dahinter die Leisten — und der Holzriegel",
        marken,
        "Die Quelle nennt kein Maß für das Türblatt — nach der eigenen Öffnung bauen",
        "nach Figur 209 in „Shelters, Shacks, and Shanties“, "
        "Charles Scribner's Sons, 1914, Tafel bei Seite 152",
        schriftgroesse=26,
    )
    return speichern(blatt, ziel)


def huhn_griff(stichpfad, ziel):
    # Die Zeichnung zeigt nur die HALTUNG. Der Schnitt selbst liegt innen im
    # Rachen und ist hier nicht zu sehen -- das sagt die Fusszeile, damit
    # niemand aus dem Bild einen Schnittpunkt abliest, den es darin nicht gibt.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1500, hoehe=1120, rand=430, oben=170)
    x, y, sb, sh = kasten
    marken = [
        (["Kopfüber gehalten,", "die Füße oben"], 1460, 260, int(sb * 0.78), int(sh * 0.08), True),
        (["Griff am Kiefergelenk"], 40, 560, int(sb * 0.28), int(sh * 0.72), False),
        (["Die andere Hand", "führt das Messer"], 40, 850, int(sb * 0.16), int(sh * 0.90), False),
        (["Messer am Ansatz"], 1460, 700, int(sb * 0.40), int(sh * 0.75), True),
    ]
    beschrifte(
        blatt, kasten,
        "Der Griff beim Töten",
        "eine Hand hält den Kopf, die andere das Messer",
        marken,
        "Der Schnitt liegt innen im Rachen — zu sehen ist hier nur die Haltung",
        "nach Figur 3 in „How to Kill and Bleed Market Poultry“, "
        "USDA Bureau of Chemistry, 1915, Seite 8",
        schriftgroesse=24,
    )
    return speichern(blatt, ziel)


def huhn_adern(stichpfad, ziel):
    # Hochformat, das Tier hängt kopfueber. Eine vierte, duenn gestrichelte
    # Linie neben den beiden Adern erklaert keine der Quellen -- sie bleibt
    # deshalb unbeschriftet, statt geraten zu werden.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1000, hoehe=1900, rand=350, oben=170)
    x, y, sb, sh = kasten
    marken = [
        (["Ader auf der einen", "Halsseite"], 40, 300, int(sb * 0.49), int(sh * 0.15), False),
        (["Querader", "verbindet", "beide Seiten"], 40, 980, int(sb * 0.61), int(sh * 0.55), False),
        (["Ader auf der", "anderen Seite"], 960, 300, int(sb * 0.73), int(sh * 0.10), True),
        (["Schnabelspitze"], 960, 1300, int(sb * 0.83), int(sh * 0.92), True),
    ]
    beschrifte(
        blatt, kasten,
        "Wo die Halsadern liegen",
        "das Tier hängt kopfüber, die Federn sind beiseitegeschoben",
        marken,
        "Außenansicht — der Schnitt selbst liegt innen im Rachen und ist hier nicht zu sehen",
        "nach Figur 1 in „How to Kill and Bleed Market Poultry“, "
        "USDA Bureau of Chemistry, 1915, Seite 4",
        schriftgroesse=30,
    )
    return speichern(blatt, ziel)


def hocker_schraegbeine(stichpfad, ziel):
    # Der seltene Fall, dass ein Bild etwas traegt, was im Text NIRGENDS
    # steht: Die Anleitung sagt selbst, dass die Quelle fuer die Schraege der
    # Beine weder einen Winkel noch eine Begruendung liefert -- sichtbar ist
    # sie nur hier. Wer nur den Text liest, treibt die Beine senkrecht ein.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1500, hoehe=860, rand=430, oben=170)
    x, y, sb, sh = kasten
    marken = [
        (["Stammscheibe,", "rund 30 cm breit"], 40, 280, int(sb * 0.35), int(sh * 0.10), False),
        (["Bein schräg", "nach außen"], 40, 520, int(sb * 0.19), int(sh * 0.55), False),
        (["Blockdicke", "rund 7,6 cm"], 1460, 300, int(sb * 0.55), int(sh * 0.28), True),
        (["Zweites Bein,", "ebenso schräg"], 1460, 560, int(sb * 0.65), int(sh * 0.65), True),
    ]
    beschrifte(
        blatt, kasten,
        "Hocker mit schrägen Beinen",
        "die Schräge steht in keinem Satz der Quelle — nur in dieser Zeichnung",
        marken,
        "Blockdicke rund 7,6 cm  ·  Bohrloch rund 3,2 cm  ·  das dritte Bein liegt verdeckt",
        "nach Figur 43 in „Log Cabins and Cottages“, "
        "William Sydney Wicks, 1908, Seite 38",
        schriftgroesse=24,
    )
    return speichern(blatt, ziel)


def bewaessern_absturz(stichpfad, ziel):
    # Fusszeile bewusst OHNE Zahl: Die Masse auf der Quellseite gehoeren zu
    # einem Rechenbeispiel des Textes, nicht nachweisbar zu genau diesem
    # Bauwerk. Heute Nacht sind drei Bilder daran gescheitert, dass in der
    # Fusszeile fremde Zahlen standen.
    blatt, kasten = blatt_mit_stich(stichpfad, breite=1600, hoehe=1170, rand=470, oben=170)
    x, y, sb, sh = kasten
    marken = [
        (["Seitenwände", "aus Bohlen"], 40, 300, int(sb * 0.36), int(sh * 0.22), False),
        (["Geneigter Boden", "führt hinunter"], 40, 620, int(sb * 0.45), int(sh * 0.45), False),
        (["Unterer Kasten", "fängt den Sturz"], 40, 880, int(sb * 0.22), int(sh * 0.74), False),
        (["Hier kommt der", "obere Graben an"], 1560, 340, int(sb * 0.88), int(sh * 0.28), True),
    ]
    beschrifte(
        blatt, kasten,
        "Absturz aus Bohlen im Graben",
        "wo das Gelände zu steil wird, fällt das Wasser in einer Stufe",
        marken,
        "Die Quelle nennt Maße nur für ihr Rechenbeispiel, nicht für dieses Bauwerk",
        "nach Figur 5 in „How to Build Small Irrigation Ditches“, "
        "USDA Farmers' Bulletin 158, 1902, Seite 19",
        schriftgroesse=26,
    )
    return speichern(blatt, ziel)


FIGUREN = {
    'meiler-lang': meiler_lang,
    'raeucherkammer': raeucherkammer,
    'stampflehm-schalung': stampflehm_schalung,
    'feldsteinmauer': feldsteinmauer,
    'ofenrohr-anschluss': ofenrohr_anschluss,
    'kalkstapel': kalkstapel,
    'geschirr-schlitten': geschirr_schlitten,
    'zisterne-schnitt': zisterne_schnitt,
    'bett-schlafstelle': bett_schlafstelle,
    'kornsieb-kasten': kornsieb_kasten,
    'kerbleiter': kerbleiter,
    'schubkarre': schubkarre,
    'brettertuer': brettertuer,
    'huhn-griff': huhn_griff,
    'huhn-adern': huhn_adern,
    'hocker-schraegbeine': hocker_schraegbeine,
    'bewaessern-absturz': bewaessern_absturz,
}

if __name__ == '__main__':
    name = sys.argv[1] if len(sys.argv) > 1 else 'meiler-lang'
    if name not in FIGUREN:
        print('unbekannt: %s -- bekannt sind %s' % (name, ', '.join(FIGUREN)))
        sys.exit(2)
    quelle, ziel = sys.argv[2], sys.argv[3]
    print('%s  %sx%s' % (ziel, *FIGUREN[name](quelle, ziel)))
