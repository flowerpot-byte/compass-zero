package org.compasszero.security

import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object PackWriter {

    // Schreibt erst daneben und benennt zum Schluss um: ein abgebrochener Lauf
    // hinterlaesst niemals eine halbe Paketdatei am Zielpfad.
    fun write(payload: File, seed: ByteArray, out: File) {
        require(payload.isFile) { "payload file not found: $payload" }
        require(payload.length() <= PackFormat.MAX_PAYLOAD_BYTES) {
            "payload exceeds ${PackFormat.MAX_PAYLOAD_BYTES} bytes"
        }
        write(payload.readBytes(), seed, out)
    }

    // Die Fassung, die genau die Bytes unterschreibt, die der Aufrufer geprueft
    // hat. Wer den Payload vorher untersucht, soll ihn nicht ein zweites Mal von
    // der Platte lesen lassen muessen: zwischen den beiden Lesevorgaengen liesse
    // sich die Datei austauschen, und unterschrieben waere dann etwas, das nie
    // jemand angesehen hat.
    /**
     * Schreibt einen Umschlag, dessen Unterschrift die PRUEFSUMME des Inhalts
     * deckt, nicht den Inhalt selbst.
     *
     * WARUM ES DIESE ZWEITE FASSUNG GIBT: Reines Ed25519 braucht zwei
     * Durchgaenge ueber die Nachricht und muss sie deshalb vollstaendig im
     * Speicher halten. Bei einem Inhaltspaket von acht Megabyte faellt das
     * nicht auf. Am 04.08.2026 hat es bei einer Kartendatei von 346 MB das
     * Telefon zerlegt: Der Puffer wuchs auf 128 MB und der Speicher war zu
     * Ende, noch bevor die Karte ueberhaupt geoeffnet war.
     *
     * Unterschrieben wird deshalb `Vorspann || SHA-256(Inhalt)`. Die
     * Pruefsumme entsteht im Durchlauf, der Speicherbedarf ist konstant, und
     * die Aussage bleibt dieselbe: Wer den Inhalt aendert, aendert die
     * Pruefsumme, und die Unterschrift passt nicht mehr.
     */
    fun writeMitPruefsumme(
        payload: ByteArray,
        seed: ByteArray,
        out: File,
        magic: ByteArray,
    ) {
        require(seed.size == Ed25519.SEED_SIZE) { "seed must be ${Ed25519.SEED_SIZE} bytes" }
        val payloadSize = payload.size.toLong()
        require(payloadSize <= PackFormat.MAX_PAYLOAD_BYTES) { "payload exceeds ${PackFormat.MAX_PAYLOAD_BYTES} bytes" }

        val signerKey = Ed25519.publicKeyFromSeed(seed)
        val prefix = PackFormat.signedPortion(PackFormat.VERSION, signerKey, payloadSize, magic)
        val summe = Digests.sha256(payload)
        val signature = summe.inputStream().use { Ed25519.sign(seed, prefix, it) }
        val header = PackFormat.buildHeader(signerKey, signature, payloadSize, magic)

        val temp = File(out.absoluteFile.parentFile, out.name + ".unfertig")
        try {
            temp.outputStream().use { target ->
                target.write(header)
                target.write(payload)
            }
            // Gegenprobe an der geschriebenen Datei, im selben Verfahren.
            check(
                RandomAccessFile(temp, "r").use { handle ->
                    val gelesen = ByteArray(PackFormat.HEADER_SIZE)
                    handle.seek(0)
                    handle.readFully(gelesen)
                    val geprueft = PackFormat.parseHeader(gelesen, handle.length(), magic)
                    if (geprueft !is HeaderResult.Ok) return@use false
                    val strom = OffsetInputStream(handle, PackFormat.HEADER_SIZE.toLong(), payloadSize)
                    val nachher = Digests.sha256Stream(strom)
                    nachher.contentEquals(summe) &&
                        summe.inputStream().use { Ed25519.verify(signerKey, signature, prefix, it) }
                },
            ) { "written map does not verify against its own key" }
            Files.move(temp.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temp.delete()
        }
    }

    fun write(payload: ByteArray, seed: ByteArray, out: File, magic: ByteArray = PackFormat.MAGIC) {
        require(seed.size == Ed25519.SEED_SIZE) { "seed must be ${Ed25519.SEED_SIZE} bytes" }
        val payloadSize = payload.size.toLong()
        require(payloadSize <= PackFormat.MAX_PAYLOAD_BYTES) { "payload exceeds ${PackFormat.MAX_PAYLOAD_BYTES} bytes" }

        val signerKey = Ed25519.publicKeyFromSeed(seed)
        val prefix = PackFormat.signedPortion(PackFormat.VERSION, signerKey, payloadSize, magic)
        val signature = payload.inputStream().use { Ed25519.sign(seed, prefix, it) }
        val header = PackFormat.buildHeader(signerKey, signature, payloadSize, magic)

        val temp = File(out.absoluteFile.parentFile, out.name + ".unfertig")
        try {
            temp.outputStream().use { target ->
                target.write(header)
                target.write(payload)
            }
            check(selfCheck(temp, signerKey, signature, payloadSize, magic)) {
                "written pack does not verify against its own key"
            }
            Files.move(temp.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temp.delete()
        }
    }

    // Das erzeugte Paket wird gegen den eigenen Schluessel geprueft. Aendert sich die
    // Quelldatei waehrend des Schreibens, faellt das hier auf und nicht erst offline
    // auf dem Geraet eines Nutzers.
    private fun selfCheck(
        pack: File,
        signerKey: ByteArray,
        signature: ByteArray,
        payloadSize: Long,
        magic: ByteArray,
    ): Boolean =
        RandomAccessFile(pack, "r").use { handle ->
            val header = ByteArray(PackFormat.HEADER_SIZE)
            handle.seek(0)
            handle.readFully(header)
            val parsed = PackFormat.parseHeader(header, handle.length(), magic)
            if (parsed !is HeaderResult.Ok) return false
            if (parsed.header.payloadSize != payloadSize) return false
            if (!parsed.header.signerKey.contentEquals(signerKey)) return false
            if (!parsed.header.signature.contentEquals(signature)) return false
            val prefix = PackFormat.signedPortion(parsed.header.version, signerKey, payloadSize, magic)
            val payload = OffsetInputStream(handle, PackFormat.HEADER_SIZE.toLong(), payloadSize)
            Ed25519.verify(signerKey, signature, prefix, payload)
        }
}
