package org.compasszero.transfer

sealed interface LeseErgebnis {
    class Ok(val rahmen: Rahmen) : LeseErgebnis

    // Die Gegenseite hat die Verbindung sauber geschlossen, zwischen zwei
    // Rahmen. Etwas anderes als ein Ende mitten im Rahmen.
    data object Ende : LeseErgebnis

    class Fehler(val fehler: TransferFehler) : LeseErgebnis
}

// Zerlegt einen Byte-Strom in Rahmen. Der Leser gehoert in den gemeinsamen Teil
// und nicht in die Plattformschicht: Er ist die Stelle, an der die Groessengrenze
// wirkt, bevor irgendetwas belegt wird. Laege er im Funk-Treiber, haette jede
// Plattform ihre eigene Fassung dieser Grenze — und eine davon wuerde sie
// vergessen.
class RahmenLeser(private val quelle: Datenquelle) {

    private val kopf = ByteArray(TransferFormat.KOPF_SIZE)
    private val gruss = ByteArray(TransferFormat.GRUSS_SIZE)

    fun liesGruss(): GrussErgebnis {
        if (!lieseGenau(gruss, TransferFormat.GRUSS_SIZE)) {
            return GrussErgebnis.Fehler(TransferFehler.GrussUnvollstaendig)
        }
        return RahmenCodec.liesGruss(gruss)
    }

    fun liesRahmen(): LeseErgebnis {
        when (lieseKopf()) {
            KopfStand.Ende -> return LeseErgebnis.Ende
            KopfStand.Abgeschnitten -> return LeseErgebnis.Fehler(TransferFehler.RahmenUnvollstaendig)
            KopfStand.Ok -> Unit
        }
        val typ = kopf[0].toInt() and 0xFF
        val laenge = leseU32(kopf, 1)
        // Vor jeder Belegung: Ein Laengenfeld von zwei Milliarden darf kein Feld
        // von zwei Milliarden Bytes anfordern, sondern muss die Verbindung beenden.
        RahmenCodec.pruefeNutzlastLaenge(laenge)?.let { return LeseErgebnis.Fehler(it) }

        val nutzlast = ByteArray(laenge)
        if (laenge > 0 && !lieseGenau(nutzlast, laenge)) {
            return LeseErgebnis.Fehler(TransferFehler.RahmenUnvollstaendig)
        }
        return when (val ergebnis = RahmenCodec.lies(typ, nutzlast)) {
            is RahmenErgebnis.Ok -> LeseErgebnis.Ok(ergebnis.rahmen)
            is RahmenErgebnis.Fehler -> LeseErgebnis.Fehler(ergebnis.fehler)
        }
    }

    private enum class KopfStand { Ok, Ende, Abgeschnitten }

    private fun lieseKopf(): KopfStand {
        var gefuellt = 0
        while (gefuellt < TransferFormat.KOPF_SIZE) {
            val gelesen = quelle.lies(kopf, gefuellt, TransferFormat.KOPF_SIZE - gefuellt)
            if (gelesen <= 0) return if (gefuellt == 0) KopfStand.Ende else KopfStand.Abgeschnitten
            gefuellt += gelesen
        }
        return KopfStand.Ok
    }

    // Ein Strom darf jederzeit weniger liefern als verlangt; das ist kein Fehler,
    // sondern der Normalfall bei Funkverbindungen. Erst ein Ende mitten im Rahmen
    // ist einer.
    private fun lieseGenau(ziel: ByteArray, laenge: Int): Boolean {
        var gefuellt = 0
        while (gefuellt < laenge) {
            val gelesen = quelle.lies(ziel, gefuellt, laenge - gefuellt)
            if (gelesen <= 0) return false
            gefuellt += gelesen
        }
        return true
    }

    private fun leseU32(bytes: ByteArray, offset: Int): Int {
        var wert = 0
        for (i in 0 until 4) wert = (wert shl 8) or (bytes[offset + i].toInt() and 0xFF)
        return wert
    }
}
