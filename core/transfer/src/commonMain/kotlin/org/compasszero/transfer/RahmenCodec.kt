package org.compasszero.transfer

import org.compasszero.content.Texts

object RahmenCodec {

    fun schreibeGruss(): ByteArray {
        val out = ByteArray(TransferFormat.GRUSS_SIZE)
        TransferFormat.MAGIC.copyInto(out, 0)
        out[4] = (TransferFormat.VERSION ushr 8).toByte()
        out[5] = TransferFormat.VERSION.toByte()
        return out
    }

    fun liesGruss(bytes: ByteArray): GrussErgebnis {
        if (bytes.size < TransferFormat.GRUSS_SIZE) return GrussErgebnis.Fehler(TransferFehler.GrussUnvollstaendig)
        for (i in TransferFormat.MAGIC.indices) {
            if (bytes[i] != TransferFormat.MAGIC[i]) return GrussErgebnis.Fehler(TransferFehler.GrussUnbekannt)
        }
        val version = leseU16(bytes, 4)
        // Eine fremde Fassung wird benannt, nie geraten: was wir nicht verstehen,
        // transportieren wir nicht.
        if (version != TransferFormat.VERSION) return GrussErgebnis.FremdeVersion(version)
        return GrussErgebnis.Ok
    }

    // Wird auf das Laengenfeld angewendet, bevor irgendetwas belegt wird. Ein
    // negativer Wert entsteht aus einem Feld mit gesetztem obersten Bit.
    fun pruefeNutzlastLaenge(laenge: Int): TransferFehler? =
        if (laenge < 0 || laenge > TransferFormat.MAX_RAHMEN_NUTZLAST) TransferFehler.RahmenZuGross else null

    fun schreibe(rahmen: Rahmen): ByteArray = when (rahmen) {
        is Rahmen.Angebot -> {
            val name = rahmen.name.encodeToByteArray()
            require(name.size <= TransferFormat.MAX_NAME_BYTES) { "Name zu lang" }
            require(rahmen.pruefsumme.size == TransferFormat.HASH_SIZE) { "Pruefsumme muss ${TransferFormat.HASH_SIZE} Byte haben" }
            // Was der Leseweg ablehnt, darf der Schreibweg nicht erzeugen: sonst
            // sieht der Nutzer "Gegenueber spricht Unsinn", obwohl der Fehler auf
            // der eigenen Seite liegt.
            require(rahmen.groesse in 1..TransferFormat.MAX_PAKET_BYTES) { "Groesse ausserhalb des Erlaubten" }
            require(Texts.isUsable(rahmen.name)) { "Name ist nicht lesbar" }
            val nutzlast = ByteArray(8 + TransferFormat.HASH_SIZE + 2 + name.size)
            schreibeU64(nutzlast, 0, rahmen.groesse)
            rahmen.pruefsumme.copyInto(nutzlast, 8)
            schreibeU16(nutzlast, 8 + TransferFormat.HASH_SIZE, name.size)
            name.copyInto(nutzlast, 10 + TransferFormat.HASH_SIZE)
            baue(TransferFormat.TYP_ANGEBOT, nutzlast)
        }
        is Rahmen.Annahme -> baue(TransferFormat.TYP_ANNAHME, ByteArray(0))
        is Rahmen.Ablehnung -> baue(TransferFormat.TYP_ABLEHNUNG, byteArrayOf(rahmen.grund.code.toByte()))
        is Rahmen.Daten -> {
            require(rahmen.bytes.isNotEmpty()) { "Datenrahmen ohne Inhalt" }
            require(rahmen.bytes.size <= TransferFormat.MAX_RAHMEN_NUTZLAST) { "Datenrahmen zu gross" }
            baue(TransferFormat.TYP_DATEN, rahmen.bytes)
        }
        is Rahmen.Fertig -> baue(TransferFormat.TYP_FERTIG, ByteArray(0))
        is Rahmen.Abbruch -> baue(TransferFormat.TYP_ABBRUCH, byteArrayOf(rahmen.grund.code.toByte()))
    }

    fun lies(typ: Int, nutzlast: ByteArray): RahmenErgebnis {
        // Der Rueckhalt gehoert hierher, nicht in eine Hilfsfunktion, die der
        // Aufrufer vergessen kann. Wer Rahmen liest, kommt hier vorbei.
        pruefeNutzlastLaenge(nutzlast.size)?.let { return RahmenErgebnis.Fehler(it) }
        return liesGeprueft(typ, nutzlast)
    }

    private fun liesGeprueft(typ: Int, nutzlast: ByteArray): RahmenErgebnis = when (typ) {
        TransferFormat.TYP_ANGEBOT -> liesAngebot(nutzlast)
        TransferFormat.TYP_ANNAHME -> ohneNutzlast(nutzlast, Rahmen.Annahme)
        TransferFormat.TYP_FERTIG -> ohneNutzlast(nutzlast, Rahmen.Fertig)
        TransferFormat.TYP_ABLEHNUNG -> liesGrund(nutzlast)?.let { code ->
            Ablehnungsgrund.entries.firstOrNull { it.code == code }
                ?.let { RahmenErgebnis.Ok(Rahmen.Ablehnung(it)) }
                ?: RahmenErgebnis.Fehler(TransferFehler.GrundUnbekannt)
        } ?: RahmenErgebnis.Fehler(TransferFehler.RahmenLaengeFalsch)
        TransferFormat.TYP_ABBRUCH -> liesGrund(nutzlast)?.let { code ->
            Abbruchgrund.entries.firstOrNull { it.code == code }
                ?.let { RahmenErgebnis.Ok(Rahmen.Abbruch(it)) }
                ?: RahmenErgebnis.Fehler(TransferFehler.GrundUnbekannt)
        } ?: RahmenErgebnis.Fehler(TransferFehler.RahmenLaengeFalsch)
        // Ein Datenrahmen ohne Inhalt bringt die Uebertragung nicht voran; beliebig
        // viele davon hielten das Geraet endlos beschaeftigt.
        TransferFormat.TYP_DATEN ->
            if (nutzlast.isEmpty()) RahmenErgebnis.Fehler(TransferFehler.RahmenLaengeFalsch)
            else RahmenErgebnis.Ok(Rahmen.Daten(nutzlast))
        else -> RahmenErgebnis.Fehler(TransferFehler.RahmenUnbekannt)
    }

    private fun liesAngebot(nutzlast: ByteArray): RahmenErgebnis {
        val kopf = 8 + TransferFormat.HASH_SIZE + 2
        if (nutzlast.size < kopf) return RahmenErgebnis.Fehler(TransferFehler.AngebotUnvollstaendig)
        val groesse = leseU64(nutzlast, 0)
        if (groesse == null || groesse <= 0 || groesse > TransferFormat.MAX_PAKET_BYTES) {
            return RahmenErgebnis.Fehler(TransferFehler.GroesseUnmoeglich)
        }
        val namensLaenge = leseU16(nutzlast, 8 + TransferFormat.HASH_SIZE)
        if (namensLaenge > TransferFormat.MAX_NAME_BYTES) return RahmenErgebnis.Fehler(TransferFehler.NameZuLang)
        // Genau so lang wie angesagt, kein Byte mehr: eine Nische hinter dem Namen
        // waere ein bequemer Weg, unbeachtete Daten mitzuschicken.
        if (nutzlast.size != kopf + namensLaenge) return RahmenErgebnis.Fehler(TransferFehler.AngebotUnvollstaendig)
        // Kaputte Bytes werden hier zu U+FFFD und fallen damit in dieselbe Pruefung
        // wie unsichtbare Zeichen: der Name ist dann unlesbar, nicht "fast lesbar".
        val gelesen = nutzlast.decodeToString(kopf, nutzlast.size)
        // Der Name ist Anzeige, kein Datenkanal. An einem unlesbaren Namen darf
        // kein Paketaustausch scheitern -- er wird ersetzt und als ersetzt
        // gekennzeichnet, damit die Oberflaeche nichts Erfundenes als Angabe des
        // Senders ausgibt.
        val lesbar = Texts.isUsable(gelesen)
        val name = if (lesbar) gelesen else TransferFormat.ERSATZNAME
        val pruefsumme = nutzlast.copyOfRange(8, 8 + TransferFormat.HASH_SIZE)
        return RahmenErgebnis.Ok(Rahmen.Angebot(groesse, pruefsumme, name, nameErsetzt = !lesbar))
    }

    private fun ohneNutzlast(nutzlast: ByteArray, rahmen: Rahmen): RahmenErgebnis =
        if (nutzlast.isEmpty()) RahmenErgebnis.Ok(rahmen)
        else RahmenErgebnis.Fehler(TransferFehler.RahmenLaengeFalsch)

    private fun liesGrund(nutzlast: ByteArray): Int? =
        if (nutzlast.size == 1) nutzlast[0].toInt() and 0xFF else null

    private fun baue(typ: Int, nutzlast: ByteArray): ByteArray {
        val out = ByteArray(TransferFormat.KOPF_SIZE + nutzlast.size)
        out[0] = typ.toByte()
        schreibeU32(out, 1, nutzlast.size)
        nutzlast.copyInto(out, TransferFormat.KOPF_SIZE)
        return out
    }

    private fun leseU16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun schreibeU16(bytes: ByteArray, offset: Int, wert: Int) {
        bytes[offset] = (wert ushr 8).toByte()
        bytes[offset + 1] = wert.toByte()
    }

    private fun schreibeU32(bytes: ByteArray, offset: Int, wert: Int) {
        for (i in 0 until 4) bytes[offset + i] = (wert ushr ((3 - i) * 8)).toByte()
    }

    // Liefert null, wenn Bit 63 gesetzt ist — solche Werte sind als Long nicht
    // positiv darstellbar und ergaeben eine Schleife, die nie endet.
    private fun leseU64(bytes: ByteArray, offset: Int): Long? {
        var wert = 0L
        for (i in 0 until 8) {
            wert = (wert shl 8) or (bytes[offset + i].toLong() and 0xFF)
        }
        return if (wert < 0) null else wert
    }

    private fun schreibeU64(bytes: ByteArray, offset: Int, wert: Long) {
        for (i in 0 until 8) bytes[offset + i] = (wert ushr ((7 - i) * 8)).toByte()
    }
}
