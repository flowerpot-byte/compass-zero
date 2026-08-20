package org.compasszero.security

import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

class OpenedPack internal constructor(
    val verdict: PackVerdict,
    val entries: List<PackEntry> = emptyList(),
    val contentFiles: Map<String, ByteArray> = emptyMap(),
    private val datei: File? = null,
) {
    // Bilder bleiben im Paket. Ihr Hash stammt aus dem signierten Durchlauf und ist
    // die Grundlage, um spaeter geladene Bilddaten gegen Manipulation zu pruefen.
    val assets: List<PackEntry> get() = entries.filter { it.name.startsWith("assets/") }

    // Bild nachladen. Nur ueber ein geoeffnetes Paket erreichbar, damit der
    // Pruefsummen-Anker nicht umgangen werden kann: eine Datei und ein selbst
    // gebauter Eintrag ohne Signaturpruefung sind kein gueltiger Ausgangspunkt.
    fun readAsset(entry: PackEntry, limits: PackLimits = PackLimits.DEFAULT): AssetRead {
        val quelle = datei ?: return AssetRead.Damaged(Damage(DamageKind.Unreadable, "kein gepruefter Paketinhalt"))
        if (entries.none { it.name == entry.name && it.sha256 == entry.sha256 }) {
            return AssetRead.Damaged(Damage(DamageKind.HashMismatch, "Eintrag stammt nicht aus diesem Paket"))
        }
        return PackVerifier.readAsset(quelle, entry, limits)
    }
}

sealed interface AssetRead {
    class Ok(val bytes: ByteArray) : AssetRead
    data object Missing : AssetRead
    class Damaged(val damage: Damage) : AssetRead
}

class PackVerifier(private val trust: TrustStore) {

    internal companion object {

        // Bilder bleiben im Paket und werden erst beim Anzeigen gelesen. Anker ist
        // die Pruefsumme aus dem signierten Durchlauf: Nur wenn die gelesenen Bytes
        // genau dazu passen, kommen sie heraus. Damit gilt der Manipulationsschutz
        // auch fuer Bilder, ohne sie dauerhaft im Speicher zu halten.
        fun readAsset(file: File, entry: PackEntry, limits: PackLimits): AssetRead {
            if (!entry.name.startsWith("assets/") || PackNames.problem(entry.name) != null) {
                return AssetRead.Damaged(Damage(DamageKind.EntryNameForbidden, entry.name.take(120)))
            }
            val handle = try {
                RandomAccessFile(file, "r")
            } catch (e: IOException) {
                return AssetRead.Damaged(Damage(DamageKind.Unreadable, e.message?.take(200) ?: "unlesbar"))
            }
            return handle.use { open ->
                val size = open.length() - PackFormat.HEADER_SIZE
                if (size <= 0) return@use AssetRead.Damaged(Damage(DamageKind.TooShort, "file shorter than header"))
                val payload = OffsetInputStream(open, PackFormat.HEADER_SIZE.toLong(), size)
                // Die erwartete Groesse ist signaturgedeckt: mehr als das darf gar
                // nicht erst in den Speicher gelesen werden.
                when (val found = PackEntries.readOne(payload, entry.name, entry.size, limits)) {
                    is EntryBytes.Missing -> AssetRead.Missing
                    is EntryBytes.Damaged -> AssetRead.Damaged(found.damage)
                    is EntryBytes.Ok -> {
                        if (Hex.encode(Digests.sha256(found.bytes)) != entry.sha256) {
                            AssetRead.Damaged(Damage(DamageKind.HashMismatch, entry.name.take(120)))
                        } else {
                            AssetRead.Ok(found.bytes)
                        }
                    }
                }
            }
        }
    }

    // Ein einziger Lesedurchlauf: dieselben Bytes fliessen gleichzeitig in die
    // Signaturpruefung und in den Entpacker. Damit kann zwischen "geprueft" und
    // "gelesen" nichts mehr ausgetauscht werden — auch nicht in derselben Datei.
    fun open(file: File, limits: PackLimits = PackLimits.DEFAULT): OpenedPack {
        val handle = try {
            RandomAccessFile(file, "r")
        } catch (e: IOException) {
            return damaged(DamageKind.Unreadable, message(e))
        }
        return try {
            handle.use { read(it, limits, file) }
        } catch (e: IOException) {
            damaged(DamageKind.Unreadable, message(e))
        }
    }

    private fun read(handle: RandomAccessFile, limits: PackLimits, datei: File): OpenedPack {
        val header = ByteArray(PackFormat.HEADER_SIZE)
        val fileSize = try {
            handle.seek(0)
            handle.readFully(header)
            handle.length()
        } catch (_: EOFException) {
            return damaged(DamageKind.TooShort, "file shorter than header")
        }

        val parsed = when (val result = PackFormat.parseHeader(header, fileSize)) {
            is HeaderResult.Damaged -> return damaged(result.kind, result.detail)
            is HeaderResult.Unsupported -> return OpenedPack(PackVerdict.Unsupported(result.version))
            is HeaderResult.Ok -> result.header
        }

        val prefix = PackFormat.signedPortion(parsed.version, parsed.signerKey, parsed.payloadSize)
        val check = Ed25519.startVerify(parsed.signerKey, prefix)
            ?: return OpenedPack(PackVerdict.BadSignature)

        val payload = OffsetInputStream(handle, PackFormat.HEADER_SIZE.toLong(), parsed.payloadSize)
        val signing = SigningInputStream(payload, check)
        val walk = PackEntries.walk(signing, limits)
        if (walk.aborted) return OpenedPack(PackVerdict.Aborted)

        // Auch nach einem Containerfehler muss der Rest durch die Pruefung laufen,
        // sonst kann ein Angreifer den Signaturvergleich mit Muell umgehen. Diese
        // Bytes zaehlen gegen dieselbe Gesamtgrenze: sonst koennte ein Paket mit
        // zwei winzigen Eintraegen ein altes Geraet zwingen, zwei Gigabyte von der
        // Karte zu lesen, bevor ueberhaupt ein Urteil faellt.
        val uebrig = try {
            signing.drain(limits.maxTotalBytes - walk.geleseneBytes)
        } catch (e: IOException) {
            return damaged(DamageKind.Unreadable, message(e))
        }
        if (!uebrig) return damaged(DamageKind.PackTooLarge, "Anhang ueberschreitet die Gesamtgrenze")
        if (Thread.currentThread().isInterrupted) return OpenedPack(PackVerdict.Aborted)

        if (!check.matches(parsed.signature)) return OpenedPack(PackVerdict.BadSignature)
        walk.damage?.let { return OpenedPack(PackVerdict.Damaged(it)) }

        val known = trust.find(parsed.signerKey)
        val verdict = if (known != null) {
            PackVerdict.Trusted(known)
        } else {
            PackVerdict.UnknownSigner(Digests.fingerprint(parsed.signerKey))
        }
        return OpenedPack(verdict, walk.entries, walk.contentFiles, datei)
    }

    private fun damaged(kind: DamageKind, detail: String) = OpenedPack(PackVerdict.Damaged(Damage(kind, detail)))

    private fun message(e: Exception): String = e.message?.take(200) ?: e::class.java.simpleName
}

// Reicht Bytes durch und fuettert dabei die Signaturpruefung.
internal class SigningInputStream(
    private val source: InputStream,
    private val check: Ed25519Verification,
) : InputStream() {

    private val single = ByteArray(1)

    override fun read(): Int {
        val read = read(single, 0, 1)
        return if (read < 0) -1 else single[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = source.read(b, off, len)
        if (read > 0) check.update(b, off, read)
        return read
    }

    // Liest den Rest ein, damit die Signaturpruefung alle Bytes sieht. Liefert
    // false, wenn dabei mehr Bytes anfallen als erlaubt.
    fun drain(erlaubt: Long): Boolean {
        val buffer = ByteArray(64 * 1024)
        var gelesen = 0L
        while (true) {
            val read = read(buffer, 0, buffer.size)
            if (read < 0) return true
            gelesen += read
            if (gelesen > erlaubt) return false
            if (Thread.currentThread().isInterrupted) return true
        }
    }
}

// Liest einen festen Byte-Bereich aus einem offenen Datei-Handle.
internal class OffsetInputStream(
    private val handle: RandomAccessFile,
    private val start: Long,
    private val length: Long,
) : InputStream() {

    private var position = 0L

    override fun read(): Int {
        val single = ByteArray(1)
        val read = read(single, 0, 1)
        return if (read < 0) -1 else single[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        val remaining = length - position
        if (remaining <= 0) return -1
        val want = minOf(len.toLong(), remaining).toInt()
        handle.seek(start + position)
        val read = handle.read(b, off, want)
        if (read > 0) position += read
        return read
    }
}
