package org.compasszero.content

// Dieselbe Aufbereitung, die das Suchverzeichnis benutzt, aber fuer Listen, die
// klein genug sind, um sie einfach durchzugehen -- der Phrasenkatalog etwa hat
// ein paar Dutzend Saetze.
//
// Warum das hier steht und nicht in der Oberflaeche nachgebaut wird: Wer
// Grossschreibung und Umlaute ein zweites Mal von Hand behandelt, bekommt zwei
// Ergebnisse fuer dieselbe Eingabe. In einem Notfallhandbuch faellt so etwas
// erst auf, wenn jemand etwas nicht findet.
object Vergleichstext {

    // Gibt null zurueck, wenn der Text sich nicht aufbereiten laesst (etwa weil
    // er beim Vereinheitlichen ueber jedes Mass waechst). Der Aufrufer behandelt
    // das wie "passt nicht".
    fun form(text: String): String? = Tokenizer.suchform(text)

    // Enthaelt der Text die Anfrage, unabhaengig von Gross- und Kleinschreibung
    // und von Umlautschreibung? Bewusst eine Teilzeichenkette und kein
    // Wortanfang: In einem Satzkatalog sucht man mitten im Satz, und die Liste
    // ist klein genug, dass die Trefferzahl nicht ausufert.
    fun enthaelt(text: String, anfrage: String): Boolean {
        val gesucht = form(anfrage) ?: return false
        if (gesucht.isEmpty()) return true
        val worin = form(text) ?: return false
        return worin.contains(gesucht)
    }
}
