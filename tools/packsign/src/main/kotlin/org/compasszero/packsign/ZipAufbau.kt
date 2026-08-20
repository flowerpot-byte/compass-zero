package org.compasszero.packsign

import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Inflater

// Ein ZIP kann Bytes enthalten, die zu keinem Eintrag gehoeren: Zusatzfelder in
// den lokalen Koepfen, ein Kommentar am Ende, oder schlicht angehaengter Muell
// hinter dem Verzeichnis. Solche Bytes liest kein Werkzeug des Projekts je an,
// wuerden aber die Unterschrift des Maintainers tragen — ein bequemer Weg, um
// beliebige Daten unter fremdem Vertrauen weiterzureichen. Vor dem Signieren
// wird deshalb geprueft, dass der Aufbau keine solchen Nischen hat.
object ZipAufbau {

    private const val LOKALER_KOPF = 0x04034B50
    private const val VERZEICHNIS_KOPF = 0x02014B50
    private const val ENDE_KOPF = 0x06054B50
    private const val ENDE_LAENGE = 22

    // Sollwerte fuer alle Felder, die sonst frei waehlbar blieben. Ein Feld, das
    // niemand prueft, ist Platz fuer beliebige Bytes unter fremder Unterschrift:
    // bei zehntausend Eintraegen kaeme so ein Viertelmegabyte zusammen.
    private const val VERSION = 20
    private const val FLAGS = 0x800 // nur die Kennzeichnung fuer UTF-8-Namen
    private const val VERFAHREN = 8 // immer gepresst, wie der eigene Packer schreibt
    private const val ZEIT = 0x0000
    private const val DATUM = 0x2821 // 1. Januar 2000, fest und zeitzonenunabhaengig

    private class Eintrag(
        val versatz: Long,
        val name: String,
        val groesse: Long,
        val verfahren: Int,
        val pruefzahl: Long,
        val entpackt: Long,
    )

    // Liefert die Beanstandung, oder null wenn der Aufbau sauber ist.
    fun problem(bytes: ByteArray): String? {
        if (bytes.size < ENDE_LAENGE) return "Payload ist kein ZIP"

        val ende = bytes.size - ENDE_LAENGE
        if (lies32(bytes, ende) != ENDE_KOPF) {
            return "hinter dem ZIP-Verzeichnis stehen zusaetzliche Bytes"
        }
        if (lies16(bytes, ende + 20) != 0) return "das ZIP traegt einen Kommentar"
        if (lies16(bytes, ende + 4) != 0 || lies16(bytes, ende + 6) != 0) return "unerwartete Traegernummer am Ende"
        if (lies16(bytes, ende + 8) != lies16(bytes, ende + 10)) return "Eintragszahlen am Ende widersprechen sich"

        val verzeichnisStart = lies32u(bytes, ende + 16)
        val verzeichnisLaenge = lies32u(bytes, ende + 12)
        val anzahl = lies16(bytes, ende + 10)
        if (verzeichnisStart + verzeichnisLaenge != ende.toLong()) return "Luecke vor dem ZIP-Verzeichnis"

        val lokale = ArrayList<Eintrag>()
        var stelle = 0L
        while (stelle < verzeichnisStart) {
            if (stelle + 30 > verzeichnisStart) return "lokaler ZIP-Kopf unvollstaendig"
            val p = stelle.toInt()
            if (lies32(bytes, p) != LOKALER_KOPF) return "unerwartete Bytes zwischen den Eintraegen"
            if (lies16(bytes, p + 4) != VERSION) return "unerwartete Formatangabe im Eintrag"
            val flags = lies16(bytes, p + 6)
            if (flags != FLAGS) return "unerwartete Kennzeichnung im Eintrag"
            val verfahren = lies16(bytes, p + 8)
            if (verfahren != VERFAHREN) return "unerwartetes Packverfahren $verfahren"
            if (lies16(bytes, p + 10) != ZEIT || lies16(bytes, p + 12) != DATUM) {
                return "unerwarteter Zeitstempel im Eintrag"
            }
            val pruefzahl = lies32u(bytes, p + 14)
            val komprimiert = lies32u(bytes, p + 18)
            val entpackt = lies32u(bytes, p + 22)
            val nameLaenge = lies16(bytes, p + 26)
            val zusatzLaenge = lies16(bytes, p + 28)
            if (zusatzLaenge != 0) return "Eintrag mit Zusatzfeld im lokalen Kopf"
            if (stelle + 30 + nameLaenge > verzeichnisStart) return "Eintragsname reicht ueber das Verzeichnis hinaus"
            val name = bytes.decodeToString(p + 30, p + 30 + nameLaenge)
            lokale.add(Eintrag(stelle, name, komprimiert, verfahren, pruefzahl, entpackt))
            stelle += 30 + nameLaenge + komprimiert
        }
        if (stelle != verzeichnisStart) return "Luecke zwischen Eintraegen und Verzeichnis"

        val verzeichnis = ArrayList<Eintrag>()
        var eintrag = verzeichnisStart
        while (eintrag < ende) {
            if (eintrag + 46 > ende) return "ZIP-Verzeichniseintrag unvollstaendig"
            val p = eintrag.toInt()
            if (lies32(bytes, p) != VERZEICHNIS_KOPF) return "ZIP-Verzeichnis beschaedigt"
            if (lies16(bytes, p + 4) != VERSION || lies16(bytes, p + 6) != VERSION) {
                return "unerwartete Formatangabe im Verzeichnis"
            }
            if (lies16(bytes, p + 8) != FLAGS) return "unerwartete Kennzeichnung im Verzeichnis"
            if (lies16(bytes, p + 12) != ZEIT || lies16(bytes, p + 14) != DATUM) {
                return "unerwarteter Zeitstempel im Verzeichnis"
            }
            if (lies16(bytes, p + 34) != 0) return "unerwartete Traegernummer"
            if (lies16(bytes, p + 36) != 0 || lies32u(bytes, p + 38) != 0L) {
                return "unerwartete Dateirechte im Verzeichnis"
            }
            val verfahren = lies16(bytes, p + 10)
            val pruefzahl = lies32u(bytes, p + 16)
            val komprimiert = lies32u(bytes, p + 20)
            val entpackt = lies32u(bytes, p + 24)
            val nameLaenge = lies16(bytes, p + 28)
            val zusatzLaenge = lies16(bytes, p + 30)
            val kommentarLaenge = lies16(bytes, p + 32)
            val versatz = lies32u(bytes, p + 42)
            if (zusatzLaenge != 0) return "Eintrag mit Zusatzfeld im Verzeichnis"
            if (kommentarLaenge != 0) return "Eintrag mit Kommentar im Verzeichnis"
            if (eintrag + 46 + nameLaenge > ende) return "Verzeichniseintrag reicht ueber die Datei hinaus"
            verzeichnis.add(
                Eintrag(
                    versatz,
                    bytes.decodeToString(p + 46, p + 46 + nameLaenge),
                    komprimiert,
                    verfahren,
                    pruefzahl,
                    entpackt,
                ),
            )
            eintrag += 46 + nameLaenge
        }
        if (eintrag != ende.toLong()) return "ZIP-Verzeichnis passt nicht zur Datei"

        // Die App liest die lokalen Koepfe der Reihe nach, jedes andere Programm
        // liest das Verzeichnis. Laufen beide Sichten auseinander, zeigt dem
        // Maintainer jedes Pruefwerkzeug etwas anderes, als die App spaeter laedt —
        // und er unterschreibt einen Inhalt, den er nie gesehen hat.
        if (anzahl != verzeichnis.size) return "Verzeichnis meldet $anzahl Eintraege, enthaelt aber ${verzeichnis.size}"
        if (lokale.size != verzeichnis.size) {
            return "Verzeichnis kennt ${verzeichnis.size} Eintraege, im Paket liegen ${lokale.size}"
        }
        // Die Reihenfolge ist Teil der Zusage, dass sich ein Paket nachbauen laesst.
        for (i in 1 until lokale.size) {
            if (lokale[i - 1].name >= lokale[i].name) return "Eintraege stehen nicht in Namensreihenfolge"
        }
        for (i in lokale.indices) {
            val a = lokale[i]
            val b = verzeichnis[i]
            if (a.versatz != b.versatz) return "Verzeichnis zeigt fuer ${b.name} an eine andere Stelle"
            if (a.name != b.name) return "Verzeichnis nennt ${b.name}, dort liegt aber ${a.name}"
            if (a.groesse != b.groesse) return "Groessenangaben fuer ${b.name} widersprechen sich"
            // Verfahren, Pruefzahl und entpackte Groesse steuern, wie ein Programm
            // den Eintrag liest. Duerfen sie auseinanderlaufen, zeigt ein
            // Pruefwerkzeug andere Bytes an, als die App spaeter laedt.
            if (a.verfahren != b.verfahren) return "Packverfahren fuer ${b.name} widerspricht sich"
            if (a.pruefzahl != b.pruefzahl) return "Pruefzahl fuer ${b.name} widerspricht sich"
            if (a.entpackt != b.entpackt) return "entpackte Groesse fuer ${b.name} widerspricht sich"
        }
        for (e in lokale) {
            inhaltPruefen(bytes, e)?.let { return it }
        }
        return null
    }

    // Bis hierher wurde die angegebene Packgroesse geglaubt. Genau darin liegt
    // die letzte Nische: Fremdbytes hinter dem eigentlichen Datenstrom, aber
    // innerhalb der Angabe, faellt sonst kein eigener Code auf -- gerettet wuerde
    // das Werkzeug erst von der Laufzeitbibliothek, also von fremdem Verhalten.
    //
    // Deshalb wird jeder Eintrag testweise entpackt und nachgerechnet: entpackte
    // Groesse, Pruefzahl und -- das ist der Punkt -- wie viele Eingabebytes dabei
    // wirklich verbraucht wurden.
    private fun inhaltPruefen(bytes: ByteArray, e: Eintrag): String? {
        val start = (e.versatz + 30 + e.name.encodeToByteArray().size).toInt()
        val presse = Inflater(true)
        try {
            presse.setInput(bytes, start, e.groesse.toInt())
            val puffer = ByteArray(64 * 1024)
            val pruefzahl = CRC32()
            var entpackt = 0L
            while (!presse.finished()) {
                val menge = try {
                    presse.inflate(puffer)
                } catch (fehler: DataFormatException) {
                    return "${e.name}: Datenstrom nicht entpackbar"
                }
                if (menge == 0) {
                    if (presse.needsInput() || presse.needsDictionary()) {
                        return "${e.name}: Datenstrom endet zu frueh"
                    }
                    continue
                }
                entpackt += menge
                pruefzahl.update(puffer, 0, menge)
                if (entpackt > e.entpackt) return "${e.name}: entpackt mehr als angegeben"
            }
            if (entpackt != e.entpackt) return "${e.name}: entpackte Groesse stimmt nicht"
            if (pruefzahl.value != e.pruefzahl) return "${e.name}: Pruefzahl stimmt nicht"
            val verbraucht = presse.bytesRead
            if (verbraucht != e.groesse) {
                return "${e.name}: ${e.groesse - verbraucht} Byte hinter dem Datenstrom"
            }
        } finally {
            presse.end()
        }
        return null
    }

    private fun lies16(bytes: ByteArray, stelle: Int): Int =
        (bytes[stelle].toInt() and 0xFF) or ((bytes[stelle + 1].toInt() and 0xFF) shl 8)

    private fun lies32(bytes: ByteArray, stelle: Int): Int =
        (bytes[stelle].toInt() and 0xFF) or
            ((bytes[stelle + 1].toInt() and 0xFF) shl 8) or
            ((bytes[stelle + 2].toInt() and 0xFF) shl 16) or
            ((bytes[stelle + 3].toInt() and 0xFF) shl 24)

    // Als Long lesen: ZIP-Groessen sind vorzeichenlos, und mit Int wuerde eine
    // erfundene Riesengroesse in negative Werte kippen und beim Weiterrechnen
    // ueber das Feldende hinauszeigen.
    private fun lies32u(bytes: ByteArray, stelle: Int): Long =
        lies32(bytes, stelle).toLong() and 0xFFFFFFFFL
}
