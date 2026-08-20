# -*- coding: utf-8 -*-
"""Vergleicht zwei APK und sagt, ob ihr INHALT reproduziert.

WARUM ES DIESES SKRIPT GIBT: Zwei Builds derselben Quellen liefern nie zwei
byteweise gleiche APK -- und wer das von Hand mit `fc` oder `sha256sum` prueft,
bekommt "unterschiedlich" und sucht danach an der falschen Stelle. Am
17.08.2026 nachgemessen: Von 21 361 703 Bytes wichen genau 610 ab, und die
lagen alle im APK-Signaturblock. Alle 55 Archiveintraege stimmten in
Pruefsumme UND Zeitstempel ueberein, die v1-Signatur (META-INF/CERT.RSA)
sogar byteweise.

Der Grund ist harmlos: Die v2-Signatur hat einen Zufallsanteil in der
Auffuellung, deshalb sehen zwei Signaturen ueber DENSELBEN Inhalt verschieden
aus. Genau diesen Fall deckt eine Pruefung durch Dritte ab -- verglichen wird
der signierte Inhalt, nicht die Signaturbytes.

Dieses Skript nimmt einem das Auseinanderhalten ab: Es meldet
Inhaltsunterschiede als Fehler und Unterschiede innerhalb des Signaturblocks
als das, was sie sind.

Aufruf:
    python tools/app/apk_vergleichen.py ERSTE.apk ZWEITE.apk

So entstehen die zwei APK (zwischen den Laeufen wird das Bauverzeichnis
geloescht, sonst vergleicht man ein Ergebnis mit sich selbst):

    rm -rf androidApp/build
    ./gradlew.bat :androidApp:assembleRelease
    cp androidApp/build/outputs/apk/release/androidApp-release.apk /pfad/lauf1.apk
    rm -rf androidApp/build
    ./gradlew.bat :androidApp:assembleRelease --rerun-tasks --no-build-cache
    cp androidApp/build/outputs/apk/release/androidApp-release.apk /pfad/lauf2.apk

Rueckgabewert 0 heisst: Der Inhalt reproduziert. 1 heisst: Es weicht etwas ab,
das nicht die Signatur ist -- dann steht in der Ausgabe, welcher Eintrag.
"""
import struct
import sys
import zipfile

MAGIC = b"APK Sig Block 42"


def signaturblock(daten):
    """Anfang und Ende des APK-Signaturblocks, oder None.

    Aufbau laut Android: [Groesse 8 Byte][Inhalt][Groesse 8 Byte][Kennung 16
    Byte]. Die Kennung steht also am ENDE. Die Groesse davor zaehlt ab dem
    zweiten Groessenfeld, deshalb kommen fuer den Anfang noch einmal 8 Byte
    dazu.
    """
    stelle = daten.rfind(MAGIC)
    if stelle < 8:
        return None
    groesse = struct.unpack("<Q", daten[stelle - 8:stelle])[0]
    anfang = stelle + len(MAGIC) - groesse - 8
    ende = stelle + len(MAGIC)
    if anfang < 0 or anfang >= ende:
        return None
    return anfang, ende


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    pfad_a, pfad_b = sys.argv[1], sys.argv[2]
    roh_a = open(pfad_a, "rb").read()
    roh_b = open(pfad_b, "rb").read()

    with zipfile.ZipFile(pfad_a) as za, zipfile.ZipFile(pfad_b) as zb:
        ia = {i.filename: i for i in za.infolist()}
        ib = {i.filename: i for i in zb.infolist()}

        nur_a = sorted(set(ia) - set(ib))
        nur_b = sorted(set(ib) - set(ia))
        gemeinsam = sorted(set(ia) & set(ib))
        pruefsumme = [n for n in gemeinsam if ia[n].CRC != ib[n].CRC]
        zeitstempel = [n for n in gemeinsam if ia[n].date_time != ib[n].date_time]

        print("Groesse:      %d / %d" % (len(roh_a), len(roh_b)))
        print("Eintraege:    %d / %d" % (len(ia), len(ib)))

        schlimm = False
        for name, liste in (("NUR IM ERSTEN", nur_a), ("NUR IM ZWEITEN", nur_b),
                            ("ANDERE PRUEFSUMME", pruefsumme),
                            ("ANDERER ZEITSTEMPEL", zeitstempel)):
            if liste:
                schlimm = True
                print("\n%s (%d):" % (name, len(liste)))
                for n in liste[:20]:
                    print("   " + n)
                if len(liste) > 20:
                    print("   ... und %d weitere" % (len(liste) - 20))

        if not schlimm:
            print("Inhalt:       alle Eintraege gleich in Pruefsumme und Zeitstempel")

    # Wo weichen die Rohdateien ab, und liegt das alles in der Signatur?
    if roh_a == roh_b:
        print("Rohdatei:     byteweise identisch (auch die Signatur)")
        return 1 if schlimm else 0

    kurz = min(len(roh_a), len(roh_b))
    stellen = [i for i in range(kurz) if roh_a[i] != roh_b[i]]
    erste, letzte = (stellen[0], stellen[-1]) if stellen else (None, None)
    print("\nabweichende Bytes: %d" % (len(stellen) + abs(len(roh_a) - len(roh_b))))
    if erste is not None:
        print("Bereich:      %d bis %d" % (erste, letzte))

    block = signaturblock(roh_a)
    if block and erste is not None and block[0] <= erste and letzte < block[1]:
        print("Lage:         vollstaendig im Signaturblock (%d bis %d)" % block)
        print("\nDER INHALT REPRODUZIERT. Verschieden ist nur die Signatur, und")
        print("das ist bei RSA mit Zufallsanteil in der Auffuellung normal.")
    else:
        schlimm = True
        print("Lage:         NICHT nur im Signaturblock -- hier ist wirklich")
        print("              etwas anders. Signaturblock: %s" % (block,))

    return 1 if schlimm else 0


sys.exit(main())
