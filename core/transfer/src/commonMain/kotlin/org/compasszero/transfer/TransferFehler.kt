package org.compasszero.transfer

// Aufzaehlung statt freiem Text, damit die Oberflaeche je Ursache eine eigene,
// uebersetzte Meldung zeigen kann — dieselbe Linie wie DamageKind beim Paket.
enum class TransferFehler {
    GrussUnvollstaendig,
    GrussUnbekannt,
    RahmenZuGross,
    RahmenUnbekannt,
    RahmenLaengeFalsch,
    RahmenUnvollstaendig,
    RahmenAusserDerReihe,
    AngebotUnvollstaendig,
    NameZuLang,
    NameUnlesbar,
    GroesseUnmoeglich,
    GrundUnbekannt,
    ZuVieleDaten,
    ZuWenigDaten,
    PruefsummeFalsch,
    VerbindungAbgerissen,
    // Der Datentraeger nimmt nichts mehr an: voll, entfernt, nicht beschreibbar.
    // Eigener Wert, damit die Oberflaeche "kein Platz mehr" sagen kann statt
    // "Paket beschaedigt" — die Ursache liegt hier auf dem eigenen Geraet.
    ZielNichtBeschreibbar,
    // Das eigene Paket laesst sich nicht mehr lesen: entfernte Speicherkarte,
    // geloeschte Datei, fehlerhafte Quelle. Eigener Wert, damit die Oberflaeche
    // nicht dem Gegenueber anlastet, was auf dieser Seite schiefging.
    QuelleNichtLesbar,
}

// Die Zahlenwerte gehen ueber die Leitung und sind damit Teil des Formats. Sie
// duerfen sich nie aendern, auch nicht beim Umsortieren der Aufzaehlung.
enum class Ablehnungsgrund(val code: Int) {
    KeinPlatz(1),
    NutzerLehntAb(2),
    ZuGross(3),
    Beschaeftigt(4),
}

enum class Abbruchgrund(val code: Int) {
    NutzerBricht(1),
    Zeitueberschreitung(2),
    Protokollfehler(3),
    Lesefehler(4),
}
