package org.compasszero.packsign

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.compasszero.security.PackNames

object DeterministicZip {

    // Fester Zeitstempel und feste Reihenfolge: derselbe Inhaltsordner ergibt Byte
    // fuer Byte dasselbe Paket. Nur so kann jemand ein veroeffentlichtes Paket
    // selbst nachbauen und mit dem heruntergeladenen vergleichen.
    private val FIXED_TIME: LocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0)
    private val OS_JUNK = setOf("Thumbs.db", "desktop.ini", ".DS_Store")
    private const val MAX_TIEFE = 32

    // Sammelt die zu packenden Dateien in fester Reihenfolge. Symbolische
    // Verknuepfungen und Windows-Junctions werden nicht verfolgt, damit keine
    // Dateien von ausserhalb des Inhaltsordners ins signierte Paket wandern.
    // Harte Verknuepfungen sind vom echten Pfad nicht unterscheidbar; dagegen
    // schuetzt nur, dass jede Datei im Paket auch verwendet werden muss.
    fun sammle(dir: File): List<Pair<File, String>> {
        require(dir.isDirectory) { "kein Verzeichnis: $dir" }
        val wurzel = try {
            dir.toPath().toRealPath()
        } catch (e: java.io.IOException) {
            throw IllegalArgumentException("Verzeichnis nicht lesbar: ${e.message}")
        }
        return collect(wurzel, dir, dir, ArrayList()).sortedBy { it.second }
    }

    // Erst daneben schreiben, dann verschieben: scheitert das Lesen einer Datei
    // mitten im Lauf, bleibt sonst ein Torso am Zielpfad liegen.
    fun write(dir: File, out: File): List<String> {
        val entwurf = File(out.absoluteFile.parentFile, out.name + ".unfertig")
        try {
            val namen = schreibe(dir, entwurf)
            java.nio.file.Files.move(
                entwurf.toPath(),
                out.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            )
            return namen
        } finally {
            entwurf.delete()
        }
    }

    private fun schreibe(dir: File, out: File): List<String> {
        val files = sammle(dir)
        require(files.isNotEmpty()) { "Verzeichnis ist leer: $dir" }
        for ((_, name) in files) {
            val problem = PackNames.problem(name)
            require(problem == null) { "$name: $problem" }
        }
        require(files.any { it.second == "manifest.json" }) { "manifest.json fehlt im Inhaltsverzeichnis" }

        ZipOutputStream(FileOutputStream(out)).use { zip ->
            zip.setLevel(Deflater.BEST_COMPRESSION)
            for ((file, name) in files) {
                val inhalt = file.readBytes()
                val entry = ZipEntry(name)
                entry.setTimeLocal(FIXED_TIME)
                // Groessen und Pruefzahl vorab bestimmen, damit sie im Kopf des
                // Eintrags stehen und nicht in einem Nachspann dahinter. Nur so
                // laesst sich spaeter nachrechnen, dass im Paket keine Bytes
                // stecken, die zu keinem Eintrag gehoeren.
                entry.size = inhalt.size.toLong()
                entry.crc = java.util.zip.CRC32().apply { update(inhalt) }.value
                entry.compressedSize = gepressteGroesse(inhalt)
                zip.putNextEntry(entry)
                zip.write(inhalt)
                zip.closeEntry()
            }
        }
        return files.map { it.second }
    }

    // Dieselbe Einstellung, die ZipOutputStream danach verwendet.
    private fun gepressteGroesse(inhalt: ByteArray): Long {
        val presse = Deflater(Deflater.BEST_COMPRESSION, true)
        try {
            presse.setInput(inhalt)
            presse.finish()
            val puffer = ByteArray(64 * 1024)
            var gesamt = 0L
            while (!presse.finished()) {
                gesamt += presse.deflate(puffer)
            }
            return gesamt
        } finally {
            presse.end()
        }
    }

    private fun collect(
        wurzelPfad: java.nio.file.Path,
        root: File,
        dir: File,
        into: MutableList<Pair<File, String>>,
        tiefe: Int = 0,
    ): MutableList<Pair<File, String>> {
        require(tiefe <= MAX_TIEFE) { "Verzeichnis zu tief verschachtelt: $dir" }
        val children = dir.listFiles() ?: return into
        for (child in children) {
            // Echter Pfad statt Symlink-Abfrage: Windows-Junctions gelten nicht als
            // symbolische Verknuepfung, wuerden also Dateien von ausserhalb des
            // Inhaltsordners ins signierte Paket holen.
            val echt = try {
                child.toPath().toRealPath()
            } catch (_: java.io.IOException) {
                continue
            }
            // Nicht still ueberspringen: eine verlinkte Datei, die lautlos
            // verschwindet, laesst eine Anleitung ins Leere zeigen.
            require(echt.startsWith(wurzelPfad)) {
                "${child.name}: zeigt aus dem Inhaltsordner heraus"
            }
            when {
                child.isDirectory -> collect(wurzelPfad, root, child, into, tiefe + 1)
                child.isFile && child.name !in OS_JUNK ->
                    into.add(child to child.relativeTo(root).invariantSeparatorsPath)
            }
        }
        return into
    }
}
