package org.compasszero.transfer

sealed interface Rahmen {

    // Das Angebot traegt absichtlich wenig: alles, was vor der Signaturpruefung
    // ankommt, ist eine Behauptung des Senders. Kennung, Version und Sprache
    // liest die App spaeter aus dem geprueften Paket selbst.
    //
    // Die Pruefsumme ist ein Detektor fuer Uebertragungsfehler, kein
    // Sicherheitsmerkmal — der Sender waehlt sie selbst. Sicherheit kommt allein
    // aus der Signatur im Paket.
    //
    // nameErsetzt heisst: Das Gegenueber hat einen unlesbaren Namen geschickt und
    // wir zeigen einen Platzhalter. Daran darf keine Uebertragung scheitern — der
    // Name ist Anzeige, die Sicherheitsentscheidung faellt erst bei der
    // Signaturpruefung. Die Oberflaeche soll den Platzhalter aber kenntlich machen.
    class Angebot(
        val groesse: Long,
        val pruefsumme: ByteArray,
        val name: String,
        val nameErsetzt: Boolean = false,
    ) : Rahmen

    data object Annahme : Rahmen

    class Ablehnung(val grund: Ablehnungsgrund) : Rahmen

    // Der Codec kopiert die Bytes nicht: wer liest, uebergibt ein frisches Feld.
    // Ein Kopiervorgang je Rahmen waere auf alten Geraeten spuerbar.
    class Daten(val bytes: ByteArray) : Rahmen

    data object Fertig : Rahmen

    class Abbruch(val grund: Abbruchgrund) : Rahmen
}

sealed interface RahmenErgebnis {
    class Ok(val rahmen: Rahmen) : RahmenErgebnis
    class Fehler(val fehler: TransferFehler) : RahmenErgebnis
}

sealed interface GrussErgebnis {
    data object Ok : GrussErgebnis
    data class FremdeVersion(val version: Int) : GrussErgebnis
    data class Fehler(val fehler: TransferFehler) : GrussErgebnis
}
