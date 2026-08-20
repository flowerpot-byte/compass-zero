package org.compasszero.security

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

class PackLimits(
    val maxEntries: Int = 10_000,
    // Ein Eintrag wird beim Lesen vollstaendig im Speicher gehalten. Auf alten
    // Geraeten liegt der ganze Heap oft bei 96–128 MB, deshalb bewusst klein.
    val maxEntryBytes: Long = 8L * 1024 * 1024,
    val maxTotalBytes: Long = 512L * 1024 * 1024,
    val maxContentBytes: Long = 24L * 1024 * 1024,
) {
    companion object {
        val DEFAULT = PackLimits()
    }
}

// Der Bauweg ist absichtlich nicht oeffentlich: ein selbst gebauter Eintrag
// haette keine Verbindung zu einem geprueften Paket, und genau diese Verbindung
// ist beim Nachladen von Bildern der einzige Schutz.
class PackEntry internal constructor(val name: String, val size: Long, val sha256: String)

internal class WalkResult(
    val entries: List<PackEntry>,
    val contentFiles: Map<String, ByteArray>,
    val damage: Damage?,
    val aborted: Boolean = false,
    val geleseneBytes: Long = 0,
)

internal sealed interface EntryBytes {
    class Ok(val bytes: ByteArray) : EntryBytes
    data object Missing : EntryBytes
    class Damaged(val damage: Damage) : EntryBytes
}

internal object PackEntries {

    private const val CHUNK = 64 * 1024

    // Liest genau einen Eintrag heraus. Nur fuer Bilder gedacht, deren Pruefsumme
    // der Aufrufer gegen den signierten Durchlauf haelt. Alle Grenzen des Pakets
    // gelten auch hier: sonst koennte eine ausgetauschte Datei die App bei jedem
    // Bildantippen fuer Minuten blockieren.
    fun readOne(payload: InputStream, name: String, erwarteteGroesse: Long, limits: PackLimits): EntryBytes {
        val buffer = ByteArray(CHUNK)
        val obergrenze = minOf(erwarteteGroesse, limits.maxEntryBytes)
        var gesamt = 0L
        var eintraege = 0
        try {
            ZipInputStream(NoCloseInputStream(payload)).use { zip ->
                while (true) {
                    if (Thread.currentThread().isInterrupted) {
                        return EntryBytes.Damaged(Damage(DamageKind.Unreadable, "abgebrochen"))
                    }
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    if (++eintraege > limits.maxEntries) {
                        return EntryBytes.Damaged(Damage(DamageKind.TooManyEntries, "$eintraege"))
                    }
                    if (entry.name != name) {
                        // Uebersprungene Eintraege werden trotzdem entpackt, also
                        // zaehlen sie gegen die Gesamtgrenze.
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            gesamt += read
                            if (gesamt > limits.maxTotalBytes) {
                                return EntryBytes.Damaged(Damage(DamageKind.PackTooLarge, "$gesamt"))
                            }
                            if (Thread.currentThread().isInterrupted) {
                                return EntryBytes.Damaged(Damage(DamageKind.Unreadable, "abgebrochen"))
                            }
                        }
                        continue
                    }
                    val out = ByteArrayOutputStream()
                    var size = 0L
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        size += read
                        gesamt += read
                        if (size > obergrenze) {
                            return EntryBytes.Damaged(Damage(DamageKind.EntryTooLarge, name))
                        }
                        if (gesamt > limits.maxTotalBytes) {
                            return EntryBytes.Damaged(Damage(DamageKind.PackTooLarge, "$gesamt"))
                        }
                        out.write(buffer, 0, read)
                    }
                    return EntryBytes.Ok(out.toByteArray())
                }
            }
        } catch (e: ZipException) {
            return EntryBytes.Damaged(Damage(DamageKind.ContainerBroken, message(e)))
        } catch (_: EOFException) {
            return EntryBytes.Damaged(Damage(DamageKind.ContainerBroken, "unexpected end"))
        } catch (e: IllegalArgumentException) {
            return EntryBytes.Damaged(Damage(DamageKind.EntryNameForbidden, message(e)))
        } catch (e: IOException) {
            return EntryBytes.Damaged(Damage(DamageKind.Unreadable, message(e)))
        }
        return EntryBytes.Missing
    }

    fun isContentName(name: String): Boolean =
        name == "manifest.json" || name.startsWith("content/")

    // Laeuft den ZIP-Strom genau einmal durch. Inhaltsdateien landen im Speicher,
    // Bilder werden nur gezaehlt und gehasht. Bei Problemen wird nicht geworfen,
    // sondern ein Schaden gemeldet — der Aufrufer muss den Strom danach leerlesen,
    // damit die Signaturpruefung alle Bytes sieht.
    fun walk(payload: InputStream, limits: PackLimits): WalkResult {
        val entries = ArrayList<PackEntry>()
        val contentFiles = LinkedHashMap<String, ByteArray>()
        val seen = HashSet<String>()
        var total = 0L
        var contentTotal = 0L
        val buffer = ByteArray(CHUNK)

        ZipInputStream(NoCloseInputStream(payload)).use { zip ->
            while (true) {
                val entry = try {
                    zip.nextEntry ?: break
                } catch (e: ZipException) {
                    return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.ContainerBroken, message(e)))
                } catch (_: EOFException) {
                    return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.ContainerBroken, "unexpected end"))
                } catch (e: IllegalArgumentException) {
                    // ZipInputStream wirft das bei ungueltigem UTF-8 im Eintragsnamen.
                    return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.EntryNameForbidden, message(e)))
                } catch (e: IOException) {
                    return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.Unreadable, message(e)))
                }

                if (entry.isDirectory) continue
                val name = entry.name
                PackNames.problem(name)?.let {
                    return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.EntryNameForbidden, it))
                }
                if (!seen.add(name)) {
                    return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.DuplicateEntry, name))
                }
                if (entries.size >= limits.maxEntries) {
                    return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.TooManyEntries, "${entries.size + 1}"))
                }

                val isContent = isContentName(name)
                val digest = MessageDigest.getInstance("SHA-256")
                val collected = if (isContent) ByteArrayOutputStream() else null
                var size = 0L

                while (true) {
                    if (Thread.currentThread().isInterrupted) {
                        return WalkResult(entries, contentFiles, null, aborted = true, geleseneBytes = total)
                    }
                    val read = try {
                        zip.read(buffer)
                    } catch (e: ZipException) {
                        return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.ContainerBroken, message(e)))
                    } catch (_: EOFException) {
                        return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.ContainerBroken, "unexpected end"))
                    } catch (e: IllegalArgumentException) {
                        return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.ContainerBroken, message(e)))
                    } catch (e: IOException) {
                        return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.Unreadable, message(e)))
                    }
                    if (read < 0) break

                    // Gezaehlt werden die wirklich entpackten Bytes; die im ZIP
                    // deklarierten Groessen sind bei Angriffen frei erfunden.
                    size += read
                    total += read
                    if (size > limits.maxEntryBytes) {
                        return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.EntryTooLarge, name))
                    }
                    if (total > limits.maxTotalBytes) {
                        return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.PackTooLarge, "$total"))
                    }
                    digest.update(buffer, 0, read)
                    if (collected != null) {
                        contentTotal += read
                        if (contentTotal > limits.maxContentBytes) {
                            return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.PackTooLarge, "content $contentTotal"))
                        }
                        collected.write(buffer, 0, read)
                    }
                }

                entries.add(PackEntry(name, size, Hex.encode(digest.digest())))
                if (collected != null) contentFiles[name] = collected.toByteArray()
            }
        }

        if (entries.isEmpty()) {
            return WalkResult(entries, contentFiles, geleseneBytes = total, damage = Damage(DamageKind.NoEntries))
        }
        return WalkResult(entries, contentFiles, null, geleseneBytes = total)
    }

    private fun message(e: Exception): String = e.message?.take(200) ?: e::class.java.simpleName
}

// ZipInputStream soll seinen Inflater freigeben duerfen, ohne die darunter liegende
// Quelle zu schliessen — die wird danach noch fuer die Signaturpruefung leergelesen.
internal class NoCloseInputStream(private val source: InputStream) : InputStream() {
    override fun read(): Int = source.read()
    override fun read(b: ByteArray, off: Int, len: Int): Int = source.read(b, off, len)
    override fun available(): Int = source.available()
    override fun close() = Unit
}
