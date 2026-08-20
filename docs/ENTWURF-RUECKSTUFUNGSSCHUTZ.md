# Entwurf: Rückstufungs-Schutz

**Stand 10.08.2026. Das ist ein ENTWURF, kein Bauplan zum Sofort-Umsetzen.**
Er ist in einer Nachtschicht entstanden und ausdrücklich zur Durchsicht
gedacht. Gebaut ist nichts.

Die Entscheidung selbst ist am 28.07.2026 gefallen und steht in `RULES.md`,
Regel 5:

> Die App merkt sich je Paket-Kennung die höchste je angenommene Version.
> Die Marke steigt nur und überlebt das Löschen des Pakets. Ältere Versionen
> werden abgelehnt, mit ausdrücklichem Notausgang für den Fall einer
> defekten neuen Fassung; der Notausgang senkt die Marke nicht. Kein
> Zeitstempel als Anker.

## Wogegen das schützt

Ein Paket, das einmal gültig signiert war, bleibt für immer gültig signiert.
Wird ein Überlebenshinweis später korrigiert — etwa eine Grabtiefe von zehn
Metern auf zweihundert —, dann verifiziert die alte Fassung mit dem alten,
falschen Wert weiterhin einwandfrei. Wer sie auf ein Gerät bringt,
überschreibt die richtige Angabe mit der falschen, und die App hat keinen
Anlass, sich zu wehren.

**Das ist kein erfundenes Szenario.** Genau dieser Fall ist am 10.08.2026
eingetreten: Der Tipp „Tote bergen" nannte zehn Meter Abstand zur
Wasserstelle, belegt sind 200 bis 350. Ein Rückfall auf die alte Fassung
wäre in einer Seuchenlage lebensgefährlich.

## Die vier Fragen, die der Entwurf beantworten muss

### 1. Wo liegt die Marke, damit sie das Löschen des Pakets überlebt?

**Nicht neben dem Paket.** Läge sie als Datei neben `europe-de.czp`, genügte
Löschen und Neuinstallieren, um sie loszuwerden — dann schützt sie nichts.

**Vorschlag:** eine eigene kleine Datei im privaten App-Verzeichnis,
getrennt von den Paketen. Die App legt dort bereits Merker ab — siehe
`Kartenlader.kt`, wo `<Beigabe>.sha256` neben der Beigabe im `filesDir`
liegt. Das Muster ist also vorhanden und erprobt.

Konkret etwa `filesDir/paketmarken.txt`, je Zeile eine Paket-Kennung und die
höchste je angenommene Version. Textform, damit sie im Fehlerfall lesbar ist
und niemand einen Parser braucht, um zu verstehen, was das Gerät denkt.

**Was dabei zu bedenken ist und hier NICHT entschieden werden kann:**

* Wird die App deinstalliert, ist auch `filesDir` weg. Die Marke überlebt
  also das Löschen des PAKETS, nicht das Löschen der APP. Ob das genügt, ist
  eine Wertungsfrage und hier nicht zu entscheiden. Die Einschätzung dieses
  Entwurfs: Ja — wer die App neuinstalliert, fängt bewusst bei null an; wer
  ein Paket austauscht, tut das oft beiläufig.
* Ein Sicherungs-/Wiederherstellungsvorgang von Android könnte `filesDir`
  mitnehmen und damit auch die Marke. Das ist erwünscht, aber es heißt auch:
  Eine alte Sicherung kann eine alte Marke zurückbringen. **Das ist der
  einzige Weg, auf dem die Marke sinken kann**, und er gehört im Papier
  benannt, nicht verschwiegen.

### 2. Wann wird geprüft?

Beim Übernehmen eines Pakets, **nach** der Signaturprüfung und **vor** dem
Ersetzen des bisherigen Standes. Reihenfolge ist wichtig: Ein Paket mit
kaputter Signatur darf die Marke nicht einmal anfassen.

### 3. Wie sieht der Notausgang aus?

Der Fall, für den er da ist: Eine neue Fassung ist defekt — sie lädt nicht,
oder ein Kapitel fehlt —, und die einzige brauchbare Fassung, die noch
greifbar ist, ist eine ältere.

**Vorschlag:**

* Die App lehnt die ältere Fassung zunächst ab und sagt klar, WARUM: „Diese
  Fassung ist älter als eine, die auf diesem Gerät schon einmal angenommen
  wurde." Mit beiden Versionsangaben im Klartext.
* Daneben eine ausdrückliche Handlung — kein beiläufiges Wegtippen, sondern
  eine bewusste Bestätigung, die benennt, was sie bedeutet: dass
  möglicherweise korrigierte Angaben durch ältere ersetzt werden.
* **Die Marke sinkt dabei NICHT.** Die ältere Fassung wird benutzt, aber die
  höchste je gesehene Version bleibt stehen. Folge: Beim nächsten Mal fragt
  die App wieder. Das ist Absicht — der Notausgang ist eine Ausnahme für
  diesen Moment, kein Umschalten in einen anderen Betriebszustand.
* **Und die Warnung bleibt sichtbar**, solange die ältere Fassung in Gebrauch
  ist. Das entspricht der Linie, die beim Schlüssel-Widerruf gezogen wurde:
  lesbar lassen mit dauerhafter Warnung statt sperren.

### 4. Welche Tests sichern das ab?

Ohne diese Tests ist das Ganze nichts wert. Sie gehören in
`core/content` bzw. dorthin, wo die Übernahme sitzt:

1. **Neuere Version wird angenommen**, Marke steigt.
2. **Gleiche Version wird angenommen**, Marke unverändert.
3. **Ältere Version wird ohne Bestätigung ABGELEHNT.**
4. **Ältere Version mit Bestätigung wird benutzt — und die Marke bleibt
   trotzdem oben.** Das ist der Test, der den eigentlichen Fehler fängt.
5. **Marke überlebt das Löschen des Pakets:** Paket löschen, dieselbe ältere
   Fassung anbieten, muss weiter abgelehnt werden.
6. **Kaputte Signatur berührt die Marke nicht** — auch dann nicht, wenn die
   Version im Manifest höher ist.
7. **Unbekannte Paket-Kennung** wird ohne Marke angenommen und legt sie an.
8. **Beschädigte Markendatei** führt nicht zum Absturz. Was dann gilt, ist
   eine Entscheidung: sicher wäre „ablehnen bis geklärt", bequem wäre
   „behandeln wie keine Marke". **Meine Empfehlung: ablehnen mit klarer
   Meldung** — eine unlesbare Sicherheitsmarke ist ein Grund innezuhalten,
   nicht weiterzumachen.

## Was dieser Entwurf bewusst NICHT anfasst

* **Das Paketformat `.czp`.** Kein zusätzlicher Eintrag, keine Änderung an
  der festgenagelten ZIP-Prüfung. Die Marke ist reiner App-Zustand. Damit
  bleiben alle bereits verteilten Pakete gültig.
* **Zeitstempel.** Regel 5 schließt sie ausdrücklich als Anker aus, und zu
  Recht: Ein Gerät ohne Netz hat keine verlässliche Uhr.
* **Den Schlüssel-Widerruf.** Der ist der größere Bruder und braucht ein
  eigenes Papier — er führt eine neue Dateiart ein und gehört in eine
  Arbeitsphase, in der Rückfragen möglich sind.

## Offene Fragen

1. Genügt es, dass die Marke das Löschen des Pakets überlebt, aber nicht die
   Deinstallation der App?
2. Soll eine beschädigte Markendatei blockieren oder durchlassen?
3. Soll der Notausgang beim JEDEM Start erneut bestätigt werden müssen, oder
   einmal je Paketübernahme?
