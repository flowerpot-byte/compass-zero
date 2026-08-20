# -*- coding: utf-8 -*-
"""Gegenprobe: Erkennt das umgebaute Werkzeug die Faelle noch, fuer die es da ist?"""
import io, os, sys, tempfile
sys.path.insert(0, os.path.join('tools', 'skizzen'))
import pruefen

FAELLE = [
 ("Groesse an der Gruppe, laeuft rechts raus  (MUSS melden)",
  '<svg xmlns="http://www.w3.org/2000/svg" width="900" height="200" viewBox="0 0 900 200">'
  '<g font-family="Georgia, serif" font-size="21" fill="#111">'
  '<text x="776" y="100">Schl\u00fcsselbein</text></g></svg>', True),
 ("Dieselbe Zeile weiter links  (darf NICHT melden)",
  '<svg xmlns="http://www.w3.org/2000/svg" width="900" height="200" viewBox="0 0 900 200">'
  '<g font-family="Georgia, serif" font-size="19" fill="#111">'
  '<text x="750" y="100">Schl\u00fcsselbein</text></g></svg>', False),
 ("text-anchor=middle, mittig und passend  (darf NICHT melden)",
  '<svg xmlns="http://www.w3.org/2000/svg" width="800" height="200" viewBox="0 0 800 200">'
  '<g font-size="25"><text x="400" y="100" text-anchor="middle">'
  'Zwei L\u00f6cher, unter der Erde verbunden</text></g></svg>', False),
 ("text-anchor=middle, aber zu weit rechts  (MUSS melden)",
  '<svg xmlns="http://www.w3.org/2000/svg" width="800" height="200" viewBox="0 0 800 200">'
  '<g font-size="25"><text x="760" y="100" text-anchor="middle">'
  'Zwei L\u00f6cher, unter der Erde verbunden</text></g></svg>', True),
 ("verschobene Gruppe schiebt den Text hinaus  (MUSS melden)",
  '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="200" viewBox="0 0 600 200">'
  '<g transform="translate(400, 0)" font-size="20"><text x="150" y="100">'
  'Randbeschriftung</text></g></svg>', True),
 ("Gruppe verschoben, Text bleibt drin  (darf NICHT melden)",
  '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="200" viewBox="0 0 600 200">'
  '<g transform="translate(100, 0)" font-size="20"><text x="50" y="100">'
  'Randbeschriftung</text></g></svg>', False),
 ("Gruppe geschlossen, danach normaler Text  (darf NICHT melden)",
  '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="200" viewBox="0 0 600 200">'
  '<g transform="translate(400, 0)" font-size="20"><text x="20" y="60">kurz</text></g>'
  '<g font-size="20"><text x="30" y="150">Zweite Zeile</text></g></svg>', False),
 ("gar keine Schriftgroesse  (MUSS melden)",
  '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="200" viewBox="0 0 600 200">'
  '<text x="30" y="100">ohne Groesse</text></svg>', True),
 ("gedrehte Gruppe  (MUSS melden, weil nicht schaetzbar)",
  '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="200" viewBox="0 0 600 200">'
  '<g transform="rotate(20)" font-size="20"><text x="30" y="100">gedreht</text></g></svg>', True),
 ("doppelter Bindestrich im Kommentar  (MUSS melden)",
  '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="200" viewBox="0 0 600 200">'
  '<!-- ein -- zwei --><text x="30" y="100" font-size="20">kurz</text></svg>', True),
]

ordner = tempfile.mkdtemp()
schlecht = 0
for name, inhalt, soll in FAELLE:
    pfad = os.path.join(ordner, 'probe.svg')
    io.open(pfad, 'w', encoding='utf-8', newline='').write(inhalt)
    fehler = pruefen.pruefe(pfad)
    ist = bool(fehler)
    ok = (ist == soll)
    if not ok:
        schlecht += 1
    print(('ok    ' if ok else 'FALSCH'), name)
    for f in fehler:
        print('        ->', f)
print()
print('%d von %d Faellen falsch' % (schlecht, len(FAELLE)))
sys.exit(1 if schlecht else 0)
