package org.compasszero.security

class PackHeader(
    val version: Int,
    val signerKey: ByteArray,
    val signature: ByteArray,
    val payloadSize: Long,
)

sealed interface HeaderResult {
    class Ok(val header: PackHeader) : HeaderResult
    class Damaged(val kind: DamageKind, val detail: String = "") : HeaderResult
    class Unsupported(val version: Int) : HeaderResult
}

object PackFormat {

    /** `CZP1` — Inhaltspaket. */
    val MAGIC = byteArrayOf(0x43, 0x5A, 0x50, 0x31)

    /**
     * `CZK1` — Kartendatei.
     *
     * Derselbe Umschlag, andere Kennung. Beides zu vermischen waere ein Fehler:
     * Die Kennung steht im unterschriebenen Teil, also deckt eine Unterschrift
     * ueber ein Inhaltspaket keine Kartendatei gleicher Groesse -- und
     * umgekehrt. Ohne diese Trennung liesse sich ein Paket als Karte
     * unterschieben und die App wuerde es oeffnen, weil die Unterschrift
     * rechnerisch passt.
     */
    val KARTE_MAGIC = byteArrayOf(0x43, 0x5A, 0x4B, 0x31)

    /**
     * `CZB1` Satellitenbilder, `CZN1` Namensverzeichnis, `CZW1` Wegenetz.
     *
     * Dieselbe Ueberlegung wie oben, dreimal: Jede Dateiart bekommt ihre
     * eigene Kennung IM unterschriebenen Teil. Sonst liesse sich ein
     * Wegenetz als Namensverzeichnis unterschieben -- die Unterschrift
     * passte rechnerisch, und die App wuerde es oeffnen.
     *
     * WARUM DAS HIER NICHT NUR EINE FORMALITAET IST: Bei diesen dreien
     * richtet eine Faelschung mehr an als bei einer Karte. Ein erfundener
     * Eintrag "Krankenhaus" schickt jemanden an eine Stelle, wo keines ist;
     * eine erfundene Kante schickt ihn ueber eine Bruecke, die es nicht gibt.
     * Und man sieht es der Datei nicht an -- anders als bei einem falschen
     * Satz im Text stutzt bei einer Linie auf der Karte niemand.
     *
     * Die vierten Bytes unterscheiden sich von den blanken Kennungen der
     * Dateien selbst (`CZBILD01`, `CZNAME01`, `CZWEG001`), damit der Leser am
     * Anfang erkennt, ob ein Umschlag da ist -- genau wie bei `CZKA` gegen
     * `CZK1`.
     */
    val BILD_MAGIC = byteArrayOf(0x43, 0x5A, 0x42, 0x31)
    val NAME_MAGIC = byteArrayOf(0x43, 0x5A, 0x4E, 0x31)
    val WEGE_MAGIC = byteArrayOf(0x43, 0x5A, 0x57, 0x31)

    /**
     * `CZH1` Gelaendeform.
     *
     * Sie ist am 18.08.2026 dazugekommen, auf ausdrueckliche Entscheidung hin.
     * Die Begruendung, warum sie zuerst OHNE Unterschrift blieb, war eng
     * gefasst und stimmt fuer sich genommen weiter: Eine gefaelschte
     * Schummerung fuehrt niemanden in die Irre, weil man ihr nicht folgt.
     *
     * Nur ist die Schummerung nicht mehr alles, was in der Datei steht. Aus
     * denselben Hoehen kommt die Hoehenangabe unter dem Finger, und danach
     * entscheidet jemand, ob er ueber einen Sattel geht oder aussenherum. Und
     * wenn ohnehin drei andere Formate einen Umschlag bekommen, ist der vierte
     * kein Aufwand mehr, sondern nur noch eine Auslassung.
     *
     * Vierter Byte `1` gegen `O` der blanken Kennung `CZHOEHE1` -- wie bei
     * allen anderen, damit der Leser am Anfang sieht, ob ein Umschlag da ist.
     */
    val HOEHEN_MAGIC = byteArrayOf(0x43, 0x5A, 0x48, 0x31)

    const val VERSION = 1
    const val HEADER_SIZE = 110
    const val KEY_SIZE = 32
    const val SIGNATURE_SIZE = 64
    // Obergrenze weit unter 2^31, damit Groessen ueberall als Int-sichere Longs handhabbar bleiben.
    const val MAX_PAYLOAD_BYTES = 2_000_000_000L

    fun parseHeader(header: ByteArray, fileSize: Long, magic: ByteArray = MAGIC): HeaderResult {
        if (header.size < HEADER_SIZE) return HeaderResult.Damaged(DamageKind.TooShort, "file shorter than header")
        for (i in magic.indices) {
            if (header[i] != magic[i]) return HeaderResult.Damaged(DamageKind.MagicMismatch)
        }
        val version = readU16(header, 4)
        if (version != VERSION) return HeaderResult.Unsupported(version)
        val signerKey = header.copyOfRange(6, 6 + KEY_SIZE)
        val signature = header.copyOfRange(38, 38 + SIGNATURE_SIZE)
        val payloadSize = readU64(header, 102)
            ?: return HeaderResult.Damaged(DamageKind.PayloadTooLarge, "payload size out of range")
        if (payloadSize > MAX_PAYLOAD_BYTES) return HeaderResult.Damaged(DamageKind.PayloadTooLarge, "$payloadSize")
        if (payloadSize != fileSize - HEADER_SIZE) {
            return HeaderResult.Damaged(DamageKind.SizeMismatch, "header $payloadSize, file ${fileSize - HEADER_SIZE}")
        }
        return HeaderResult.Ok(PackHeader(version, signerKey, signature, payloadSize))
    }

    fun buildHeader(
        signerKey: ByteArray,
        signature: ByteArray,
        payloadSize: Long,
        magic: ByteArray = MAGIC,
    ): ByteArray {
        require(signerKey.size == KEY_SIZE) { "signer key must be $KEY_SIZE bytes" }
        require(signature.size == SIGNATURE_SIZE) { "signature must be $SIGNATURE_SIZE bytes" }
        val out = ByteArray(HEADER_SIZE)
        magic.copyInto(out, 0)
        writeU16(out, 4, VERSION)
        signerKey.copyInto(out, 6)
        signature.copyInto(out, 38)
        writeU64(out, 102, payloadSize)
        return out
    }

    // Die Version wird uebergeben, nicht aus der Konstante genommen: sobald es eine
    // zweite Formatversion gibt, darf eine v1-Signatur keine v2-Datei decken.
    // Dasselbe gilt fuer die Kennung: eine Unterschrift ueber ein Inhaltspaket
    // darf keine Kartendatei derselben Groesse decken.
    fun signedPortion(
        version: Int,
        signerKey: ByteArray,
        payloadSize: Long,
        magic: ByteArray = MAGIC,
    ): ByteArray {
        require(signerKey.size == KEY_SIZE) { "signer key must be $KEY_SIZE bytes" }
        val out = ByteArray(46)
        magic.copyInto(out, 0)
        writeU16(out, 4, version)
        signerKey.copyInto(out, 6)
        writeU64(out, 38, payloadSize)
        return out
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun writeU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value ushr 8).toByte()
        bytes[offset + 1] = value.toByte()
    }

    // Liefert null, wenn Bit 63 gesetzt ist — solche Werte sind als Long nicht positiv darstellbar.
    private fun readU64(bytes: ByteArray, offset: Int): Long? {
        var value = 0L
        for (i in 0 until 8) {
            value = (value shl 8) or (bytes[offset + i].toLong() and 0xFF)
        }
        return if (value < 0) null else value
    }

    private fun writeU64(bytes: ByteArray, offset: Int, value: Long) {
        for (i in 0 until 8) {
            bytes[offset + i] = (value ushr ((7 - i) * 8)).toByte()
        }
    }
}
