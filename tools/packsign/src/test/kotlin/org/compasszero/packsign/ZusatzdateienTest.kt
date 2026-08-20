package org.compasszero.packsign

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Deflater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.compasszero.karte.Hoehenformat
import org.compasszero.karte.Kartenformat
import org.compasszero.karte.Namensdatei
import org.compasszero.karte.Wegenetz

/**
 * Das Unterschreiben und Pruefen von Satellitenbild, Namensverzeichnis,
 * Wegenetz und Gelaendeform.
 *
 * WAS HIER WIRKLICH GEPRUEFT WIRD: nicht, dass eine Unterschrift entsteht --
 * das taete auch ein Werkzeug, das jede Datei blind durchwinkt. Geprueft wird,
 * dass es Dateien ABWEIST, denen man den Fehler nicht ansieht: ein
 * Namensverzeichnis in falscher Reihenfolge, eine Kante mit gelogener Laenge,
 * eine Bildkachel mit angehaengten Bytes, ein Hoehenraster mit Fremdbytes
 * dahinter. Alle oeffnen sich einwandfrei; die App wuerde sie nehmen und im
 * Ernstfall jemanden falsch schicken.
 *
 * Die Proben werden hier gebaut und nicht aus `work/` geholt: Nach einem
 * frischen Klon gibt es dort nichts.
 */
class ZusatzdateienTest {

    private fun tempDir(): File = File.createTempFile("zusatz", null).let { it.delete(); it.mkdirs(); it }

    private fun run(vararg args: String): Pair<Int, String> {
        val out = StringBuilder()
        val code = Commands.run(arrayOf(*args), out)
        return code to out.toString()
    }

    /** Legt einen Schluessel an und gibt (secret, Liste mit dem oeffentlichen) zurueck. */
    private fun schluessel(dir: File, name: String): Pair<File, File> {
        assertEquals(0, run("keygen", "--dir", dir.path, "--name", name).first)
        val liste = File(dir, "$name-liste.txt")
        liste.writeText("Maintainer=" + File(dir, "$name.public").readText().trim() + "\n")
        return File(dir, "$name.secret") to liste
    }

    // ---- Proben bauen ------------------------------------------------------

    private fun vier(w: Int) = ByteArray(4) { ((w shr (8 * it)) and 0xFF).toByte() }
    private fun zwei(w: Int) = ByteArray(2) { ((w shr (8 * it)) and 0xFF).toByte() }
    private fun acht(w: Long) = ByteArray(8) { ((w shr (8 * it)) and 0xFF).toByte() }

    private fun grad(wert: Double) = vier((wert * 1e7).toInt())

    private fun winzigesJpeg(): ByteArray {
        val bild = java.awt.image.BufferedImage(8, 8, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val aus = ByteArrayOutputStream()
        javax.imageio.ImageIO.write(bild, "jpg", aus)
        return aus.toByteArray()
    }

    /** Vier Kacheln auf einer Stufe. [anhang] klebt hinter dem Bildende. */
    private fun bilddatei(ziel: File, anhang: ByteArray = ByteArray(0)) {
        val zoom = 10
        val kacheln = listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
        val roh = kacheln.mapIndexed { i, _ ->
            if (i == 0) winzigesJpeg() + anhang else winzigesJpeg()
        }
        val anfang = 48 + 21 * kacheln.size
        val kopf = ByteArrayOutputStream()
        kopf.write("CZBILD01".toByteArray(Charsets.US_ASCII))
        kopf.write(1); kopf.write(8); kopf.write(zoom); kopf.write(zoom)
        kopf.write(grad(12.9)); kopf.write(grad(47.7)); kopf.write(grad(13.2)); kopf.write(grad(47.9))
        kopf.write(vier(kacheln.size))
        kopf.write(acht(anfang.toLong()))
        kopf.write(vier(20250919)); kopf.write(vier(20250919))

        val verzeichnis = ByteArrayOutputStream()
        var stelle = anfang.toLong()
        for ((i, k) in kacheln.withIndex()) {
            verzeichnis.write(zoom)
            verzeichnis.write(vier(k.first)); verzeichnis.write(vier(k.second))
            verzeichnis.write(acht(stelle)); verzeichnis.write(vier(roh[i].size))
            stelle += roh[i].size
        }
        ziel.outputStream().use { aus ->
            aus.write(kopf.toByteArray())
            aus.write(verzeichnis.toByteArray())
            for (bytes in roh) aus.write(bytes)
        }
    }

    private fun varint(aus: ByteArrayOutputStream, wert: Int) {
        var w = wert
        while (true) {
            val b = w and 0x7F
            w = w ushr 7
            if (w != 0) aus.write(b or 0x80) else { aus.write(b); return }
        }
    }

    /** Drei Namen. Bei [verdreht] steht die Reihenfolge kopf, die Versaetze nicht. */
    private fun namensdatei(ziel: File, verdreht: Boolean = false) {
        val eintraege = listOf(
            Triple("berchtesgaden", "Berchtesgaden", 13.00 to 47.63),
            Triple("hallein", "Hallein", 13.10 to 47.68),
            Triple("salzburg", "Salzburg", 13.05 to 47.80),
        ).let { if (verdreht) it.reversed() else it }

        val koerper = ByteArrayOutputStream()
        val versaetze = ArrayList<Int>()
        for ((schluessel, name, ort) in eintraege) {
            versaetze.add(koerper.size())
            val s = schluessel.toByteArray(Charsets.UTF_8)
            val n = name.toByteArray(Charsets.UTF_8)
            varint(koerper, s.size); koerper.write(s)
            varint(koerper, n.size); koerper.write(n)
            koerper.write(grad(ort.first)); koerper.write(grad(ort.second))
            koerper.write(Kartenformat.ORT); koerper.write(0)
        }
        val anfang = 32 + 4 * eintraege.size
        val kopf = ByteArrayOutputStream()
        kopf.write("CZNAME01".toByteArray(Charsets.US_ASCII))
        kopf.write(1); kopf.write(0); kopf.write(0); kopf.write(0)
        kopf.write(vier(eintraege.size))
        kopf.write(grad(12.9)); kopf.write(grad(47.6)); kopf.write(grad(13.2)); kopf.write(grad(47.9))
        ziel.outputStream().use { aus ->
            aus.write(kopf.toByteArray())
            for (v in versaetze) aus.write(vier(anfang + v))
            aus.write(koerper.toByteArray())
        }
    }

    /**
     * Zwei Kanten mit echter Geometrie. [laengenzuschlag] verfaelscht die
     * angegebene Laenge der ersten Kante -- die Linie bleibt, die Zahl luegt.
     */
    private fun wegenetz(ziel: File, laengenzuschlag: Int = 0) {
        val knoten = listOf(
            13.000 to 47.800,
            13.010 to 47.800,
            13.020 to 47.805,
        )
        val kanten = listOf(0 to 1, 1 to 2)
        val koerper = ByteArrayOutputStream()
        val versaetze = ArrayList<Int>()
        for ((i, kante) in kanten.withIndex()) {
            val (a, b) = kante
            val meter = Wegenetz.entfernung(
                knoten[a].second, knoten[a].first, knoten[b].second, knoten[b].first,
            )
            versaetze.add(koerper.size())
            koerper.write(vier(a)); koerper.write(vier(b))
            koerper.write(zwei(100))
            koerper.write(vier(Math.round(meter).toInt() + if (i == 0) laengenzuschlag else 0))
            koerper.write(zwei(2))
            for (ende in listOf(a, b)) {
                koerper.write(grad(knoten[ende].first)); koerper.write(grad(knoten[ende].second))
            }
        }
        val anfang = 40 + 8 * knoten.size + 4 * kanten.size
        val kopf = ByteArrayOutputStream()
        kopf.write("CZWEG001".toByteArray(Charsets.US_ASCII))
        kopf.write(1); kopf.write(0); kopf.write(0); kopf.write(0)
        kopf.write(vier(knoten.size)); kopf.write(vier(kanten.size))
        kopf.write(grad(12.9)); kopf.write(grad(47.7)); kopf.write(grad(13.2)); kopf.write(grad(47.9))
        // Der Kopf ist 40 Byte lang, beschrieben sind 36. Ohne diese vier
        // faengt die Knotentabelle zu frueh an und alles dahinter verrutscht.
        kopf.write(vier(0))
        ziel.outputStream().use { aus ->
            aus.write(kopf.toByteArray())
            for ((lon, lat) in knoten) { aus.write(grad(lon)); aus.write(grad(lat)) }
            for (v in versaetze) aus.write(vier(anfang + v))
            aus.write(koerper.toByteArray())
        }
    }

    /**
     * Zwei Kacheln mit 4x4-Raster. [anhang] haengt Bytes hinter das Raster --
     * die kommen erst zum Vorschein, wenn jemand die Kachel wirklich liest.
     */
    private fun hoehendatei(ziel: File, anhang: Int = 0) {
        val kacheln = listOf(Triple(8, 134, 89), Triple(8, 135, 89))
        val inhalte = kacheln.mapIndexed { i, _ ->
            packe(hoehenkachel(500, if (i == 0) anhang else 0))
        }
        val anfang = Hoehenformat.KOPF_BYTES + Hoehenformat.EINTRAG_BYTES * kacheln.size
        val kopf = ByteArray(Hoehenformat.KOPF_BYTES)
        Hoehenformat.KENNUNG.copyInto(kopf, 0)
        kopf[8] = Hoehenformat.FASSUNG.toByte()
        kopf[9] = 2 // Kante 4
        kopf[10] = kacheln.minOf { it.first }.toByte()
        kopf[11] = kacheln.maxOf { it.first }.toByte()
        for (i in 0 until 4) kopf[28 + i] = (kacheln.size shr (8 * i)).toByte()
        for (i in 0 until 8) kopf[32 + i] = (anfang.toLong() shr (8 * i)).toByte()

        ziel.outputStream().use { aus ->
            aus.write(kopf)
            var stelle = anfang.toLong()
            for ((i, kachel) in kacheln.withIndex()) {
                val (z, x, y) = kachel
                aus.write(z)
                aus.write(vier(x)); aus.write(vier(y))
                aus.write(acht(stelle)); aus.write(vier(inhalte[i].size))
                stelle += inhalte[i].size
            }
            for (inhalt in inhalte) aus.write(inhalt)
        }
    }

    private fun packe(roh: ByteArray): ByteArray {
        val packer = Deflater(9)
        packer.setInput(roh)
        packer.finish()
        val aus = ByteArray(roh.size * 4 + 64)
        val n = packer.deflate(aus)
        packer.end()
        return aus.copyOf(n)
    }

    /** Ein 4x4-Raster mit einem Hang von West nach Ost. */
    private fun hoehenkachel(grund: Int, anhang: Int): ByteArray {
        val b = ArrayList<Byte>()
        fun roh(w: Int) = b.add((w and 0xFF).toByte())
        fun varint(w: Int) {
            var r = w
            while (r >= 128) {
                b.add(((r and 127) or 128).toByte()); r = r ushr 7
            }
            b.add(r.toByte())
        }
        fun zigzag(w: Int) = varint((w shl 1) xor (w shr 31))
        roh(Hoehenformat.FASSUNG)
        roh((grund shr 8) and 0xFF)
        roh(grund and 0xFF)
        roh(1)
        for (zeile in 0 until 4) {
            zigzag(if (zeile == 0) 0 else 5)
            repeat(3) { zigzag(10) }
        }
        repeat(anhang) { roh(0x42) }
        return b.toByteArray()
    }

    // ---- Der gute Fall -----------------------------------------------------

    @Test
    fun alleVierWerdenSigniertUndWiedererkannt() {
        val dir = tempDir()
        val (geheim, liste) = schluessel(dir, "m")
        val proben = listOf(
            Triple("bild", File(dir, "p.czb").also { bilddatei(it) }, "4 Kacheln"),
            Triple("namen", File(dir, "p.czn").also { namensdatei(it) }, "3 Namen"),
            Triple("wege", File(dir, "p.czw").also { wegenetz(it) }, "2 Kanten"),
            Triple("hoehen", File(dir, "p.czh").also { hoehendatei(it) }, "2 Kacheln"),
        )
        for ((art, quelle, merkmal) in proben) {
            val ziel = File(dir, "${art}-signiert" + quelle.extension.let { ".$it" })
            val (code, text) = run(
                "$art-signieren", "--key", geheim.path, "--in", quelle.path, "--out", ziel.path,
            )
            assertEquals(0, code, "$art: $text")
            assertTrue(ziel.length() > quelle.length(), "$art: der Umschlag fehlt")

            val (pruefCode, pruefText) = run("$art-pruefen", "--in", ziel.path, "--keys", liste.path)
            assertEquals(0, pruefCode, "$art: $pruefText")
            assertTrue("GUELTIG" in pruefText, "$art: $pruefText")
            // Der Kurzbericht muss aus der Datei kommen, nicht aus dem Umschlag.
            assertTrue(merkmal in pruefText, "$art: $pruefText")
        }
    }

    @Test
    fun ohneUnterschriftWirdEsGesagtUndNichtVerschwiegen() {
        // Blanke Dateien sind erlaubt -- wer sich sein Wegenetz selbst baut,
        // soll es benutzen koennen. Aber es muss dastehen.
        val dir = tempDir()
        val (_, liste) = schluessel(dir, "m")
        val blank = File(dir, "p.czw").also { wegenetz(it) }
        val (code, text) = run("wege-pruefen", "--in", blank.path, "--keys", liste.path)
        assertEquals(1, code, text)
        assertTrue("KEINE Unterschrift" in text, text)
    }

    // ---- Was nicht unterschrieben werden darf ------------------------------

    @Test
    fun einVerdrehtesNamensverzeichnisWirdNichtSigniert() {
        // Die Suche ist binaer. Auf dieser Datei findet sie nicht etwa nichts,
        // sondern den falschen Ort -- und der Leser sieht nichts davon, weil
        // die Versatztabelle in Ordnung ist.
        val dir = tempDir()
        val (geheim, _) = schluessel(dir, "m")
        val quelle = File(dir, "p.czn").also { namensdatei(it, verdreht = true) }
        val ziel = File(dir, "raus.czn")
        val (code, text) = run("namen-signieren", "--key", geheim.path, "--in", quelle.path, "--out", ziel.path)
        assertEquals(2, code, text)
        assertTrue("Vorgaenger" in text, text)
        assertFalse(ziel.exists(), "es darf nichts entstanden sein")
    }

    @Test
    fun eineGelogeneKantenlaengeWirdNichtSigniert() {
        // 400 Meter Aufschlag auf eine Kante von rund 750 Metern: die Route
        // waere zeichnerisch richtig und die angesagte Laenge falsch.
        val dir = tempDir()
        val (geheim, _) = schluessel(dir, "m")
        val quelle = File(dir, "p.czw").also { wegenetz(it, laengenzuschlag = 400) }
        val ziel = File(dir, "raus.czw")
        val (code, text) = run("wege-signieren", "--key", geheim.path, "--in", quelle.path, "--out", ziel.path)
        assertEquals(2, code, text)
        assertTrue("gemessen" in text, text)
        assertFalse(ziel.exists(), "es darf nichts entstanden sein")
    }

    @Test
    fun eineKachelMitAngehaengtenBytesWirdNichtSigniert() {
        // Dasselbe Muster, das bei den Bildern im Inhaltspaket schon einmal
        // durchgerutscht ist: ein einwandfreies Bild, hinter dessen Ende
        // Fremddaten kleben und mitunterschrieben wuerden.
        val dir = tempDir()
        val (geheim, _) = schluessel(dir, "m")
        val quelle = File(dir, "p.czb").also { bilddatei(it, anhang = ByteArray(4096) { 0x42 }) }
        val ziel = File(dir, "raus.czb")
        val (code, text) = run("bild-signieren", "--key", geheim.path, "--in", quelle.path, "--out", ziel.path)
        assertEquals(2, code, text)
        assertTrue("Kachel" in text, text)
        assertFalse(ziel.exists(), "es darf nichts entstanden sein")
    }

    @Test
    fun eineDateiImFalschenUmschlagWirdNichtSigniert() {
        // Ohne eigene Kennung je Art liesse sich ein Wegenetz als
        // Namensverzeichnis unterschieben -- die Unterschrift passte
        // rechnerisch. Das Werkzeug faellt schon vorher darueber.
        val dir = tempDir()
        val (geheim, _) = schluessel(dir, "m")
        val quelle = File(dir, "p.czw").also { wegenetz(it) }
        val ziel = File(dir, "raus.czn")
        val (code, text) = run("namen-signieren", "--key", geheim.path, "--in", quelle.path, "--out", ziel.path)
        assertEquals(2, code, text)
        assertFalse(ziel.exists(), "es darf nichts entstanden sein")
    }

    @Test
    fun eineHoehenkachelMitAnhangWirdNichtSigniert() {
        // Die Bytes hinter dem Raster stehen IN der gepackten Kachel. Das
        // Verzeichnis geht auf, die Datei oeffnet sich, und erst wer die
        // Kachel wirklich liest, stolpert darueber. Genau dafuer liest das
        // Werkzeug jede einzelne.
        val dir = tempDir()
        val (geheim, _) = schluessel(dir, "m")
        val quelle = File(dir, "p.czh").also { hoehendatei(it, anhang = 7) }
        val ziel = File(dir, "raus.czh")
        val (code, text) = run("hoehen-signieren", "--key", geheim.path, "--in", quelle.path, "--out", ziel.path)
        assertEquals(2, code, text)
        assertFalse(ziel.exists(), "es darf nichts entstanden sein")
    }

    // ---- Was beim Pruefen auffallen muss -----------------------------------

    @Test
    fun eineFremdeUnterschriftGiltNicht() {
        val dir = tempDir()
        val (fremd, _) = schluessel(dir, "fremd")
        val (_, meineListe) = schluessel(dir, "m")
        val quelle = File(dir, "p.czn").also { namensdatei(it) }
        val ziel = File(dir, "fremd.czn")
        assertEquals(
            0,
            run("namen-signieren", "--key", fremd.path, "--in", quelle.path, "--out", ziel.path).first,
        )
        val (code, text) = run("namen-pruefen", "--in", ziel.path, "--keys", meineListe.path)
        assertEquals(1, code, text)
        assertTrue("UNBEKANNT" in text, text)
    }

    @Test
    fun einUmgekipptesByteFaelltAuf() {
        val dir = tempDir()
        val (geheim, liste) = schluessel(dir, "m")
        val quelle = File(dir, "p.czb").also { bilddatei(it) }
        val ziel = File(dir, "s.czb")
        assertEquals(
            0,
            run("bild-signieren", "--key", geheim.path, "--in", quelle.path, "--out", ziel.path).first,
        )
        val bytes = ziel.readBytes()
        bytes[bytes.size - 40] = (bytes[bytes.size - 40].toInt() xor 0x01).toByte()
        ziel.writeBytes(bytes)

        val (code, text) = run("bild-pruefen", "--in", ziel.path, "--keys", liste.path)
        assertEquals(1, code, text)
        assertTrue("MANIPULIERT" in text, text)
    }
}
