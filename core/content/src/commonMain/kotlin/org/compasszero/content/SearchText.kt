package org.compasszero.content

// Derselbe sichtbare Text kann unterschiedlich gespeichert sein: "ä" als ein
// Zeichen oder als "a" plus Trennzeichen, "fi" als Ligatur, Zeichen in voller
// oder halber Breite. Ohne Vereinheitlichung waere ein Paket, das durch ein
// Werkzeug gelaufen ist, ploetzlich nicht mehr auffindbar.
//
// Die Vereinheitlichung kann Text verlaengern — eine arabische Ligatur wird zu
// achtzehn Zeichen. Deshalb gibt es nur die begrenzte Fassung: sie liefert null,
// sobald die Ausgabe die Grenze reisst, statt den Speicher zu fuellen.
internal expect fun vereinheitlicheBegrenzt(text: String, maxAusgabe: Int): String?

// Kategorie eines vollstaendigen Codepunkts. Ueber der Grundebene reicht die
// Zeichen-Abfrage nicht: dort steckt jedes Zeichen in zwei Haelften, und eine
// Pruefung je Haelfte wuerde unsichtbare Zeichen als sichtbar durchwinken.
internal expect fun istSichtbarerCodepunkt(codePunkt: Int): Boolean

internal expect fun istVerboteneKategorie(codePunkt: Int): Boolean

internal expect fun istMarkierung(codePunkt: Int): Boolean
