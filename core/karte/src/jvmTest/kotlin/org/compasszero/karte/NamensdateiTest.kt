package org.compasszero.karte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Das Namensverzeichnis.
 *
 * Gebaut wird hier jedes Mal eine echte kleine `.czn`, damit die Pruefungen
 * nach einem frischen Klon laufen. Ob WERKZEUG und LESER dieselbe Vorstellung
 * vom Format haben, prueft `EchteNamensdateiTest` an einer wirklich gebauten
 * Datei -- bei der Bilddatei hat genau dieser Gegencheck den Sortierfehler
 * gefunden.
 */
class NamensdateiTest {

    private fun schreibe(
        ziel: File,
        eintraege: List<Triple<String, String, Pair<Double, Double>>>,
        kennung: ByteArray = Namensdatei.KENNUNG,
        fassung: Int = Namensdatei.FASSUNG,
        anzahlLuege: Int? = null,
        versaetzeVerdrehen: Boolean = false,
    ) {
        val koerper = java.io.ByteArrayOutputStream()
        val versaetze = ArrayList<Int>()
        for ((schluessel, name, ort) in eintraege) {
            versaetze.add(koerper.size())
            val s = schluessel.toByteArray(Charsets.UTF_8)
            val n = name.toByteArray(Charsets.UTF_8)
            varint(koerper, s.size); koerper.write(s)
            varint(koerper, n.size); koerper.write(n)
            // ERST LAENGE, DANN BREITE -- so schreibt es auch das Werkzeug.
            // Vertauscht ergibt das eine Stelle, die es gibt: 47 Grad Ost,
            // 13 Grad Nord liegt im Golf von Aden.
            koerper.write(vierByte((ort.first * 1e7).toInt()))
            koerper.write(vierByte((ort.second * 1e7).toInt()))
            koerper.write(Kartenformat.ORT)
            koerper.write(0)
        }
        val anfang = Namensdatei.KOPF_BYTES + 4 * eintraege.size
        val kopf = ByteArray(Namensdatei.KOPF_BYTES)
        kennung.copyInto(kopf, 0, 0, minOf(8, kennung.size))
        kopf[8] = fassung.toByte()
        setzeInt(kopf, 12, anzahlLuege ?: eintraege.size)
        setzeInt(kopf, 16, 90_000_000)
        setzeInt(kopf, 20, 460_000_000)
        setzeInt(kopf, 24, 170_000_000)
        setzeInt(kopf, 28, 490_000_000)
        ziel.outputStream().use { aus ->
            aus.write(kopf)
            val fertig = if (versaetzeVerdrehen) versaetze.reversed() else versaetze
            for (v in fertig) aus.write(vierByte(anfang + v))
            aus.write(koerper.toByteArray())
        }
    }

    private fun varint(aus: java.io.ByteArrayOutputStream, wert: Int) {
        var w = wert
        while (true) {
            val b = w and 0x7F
            w = w ushr 7
            if (w != 0) aus.write(b or 0x80) else { aus.write(b); return }
        }
    }

    private fun vierByte(w: Int) = ByteArray(4) { ((w shr (8 * it)) and 0xFF).toByte() }

    private fun setzeInt(b: ByteArray, p: Int, w: Int) {
        for (i in 0 until 4) b[p + i] = ((w shr (8 * i)) and 0xFF).toByte()
    }

    private val beispiel = listOf(
        Triple("bad reichenhall", "Bad Reichenhall", 12.88 to 47.73),
        Triple("berchtesgaden", "Berchtesgaden", 13.00 to 47.63),
        Triple("hallein", "Hallein", 13.10 to 47.68),
        Triple("salzburg", "Salzburg", 13.05 to 47.80),
        Triple("st veit", "St. Veit", 13.15 to 47.77),
        Triple("salzburg flughafen", "Salzburg Flughafen", 13.00 to 47.79),
    ).sortedBy { it.first }

    private fun mitDatei(tu: (Namensdatei) -> Unit) {
        val f = File.createTempFile("namen", ".czn")
        try {
            schreibe(f, beispiel)
            Namensdatei.oeffne(f).use(tu)
        } finally {
            f.delete()
        }
    }

    @Test
    fun einVollerNameWirdGefunden() = mitDatei { d ->
        val treffer = d.suche("Salzburg")
        assertTrue(treffer.any { it.name == "Salzburg" }, treffer.map { it.name }.toString())
    }

    @Test
    fun einAnfangGenuegt() = mitDatei { d ->
        val treffer = d.suche("berch").map { it.name }
        assertEquals(listOf("Berchtesgaden"), treffer)
    }

    @Test
    fun derAnfangFindetAlleDieSoBeginnen() = mitDatei { d ->
        // "salzburg" muss beide liefern -- den Ort und den Flughafen.
        val treffer = d.suche("salzburg").map { it.name }.sorted()
        assertEquals(listOf("Salzburg", "Salzburg Flughafen"), treffer)
    }

    @Test
    fun umlauteUndPunkteStoerenNicht() = mitDatei { d ->
        // "St. Veit" steht als "st veit" im Schluessel. Wer den Punkt tippt,
        // muss es trotzdem finden -- und wer ihn weglaesst, auch.
        assertEquals(listOf("St. Veit"), d.suche("St. Veit").map { it.name })
        assertEquals(listOf("St. Veit"), d.suche("st veit").map { it.name })
        assertEquals(listOf("St. Veit"), d.suche("ST VEIT").map { it.name })
    }

    @Test
    fun mittendrinWirdNichtGesucht() = mitDatei { d ->
        // "burg" darf NICHT Salzburg liefern: Wer "burg" tippt, meint
        // Burghausen. Eine Suche, die beides liefert, schuettet die Liste zu.
        assertTrue(d.suche("burg").isEmpty(), d.suche("burg").map { it.name }.toString())
    }

    @Test
    fun wasEsNichtGibtGibtNichts() = mitDatei { d ->
        assertTrue(d.suche("Kapstadt").isEmpty())
        assertTrue(d.suche("").isEmpty())
        assertTrue(d.suche("   ").isEmpty())
    }

    @Test
    fun dieStelleKommtRichtigZurueck() = mitDatei { d ->
        val salzburg = d.suche("salzburg").first { it.name == "Salzburg" }
        assertTrue(kotlin.math.abs(salzburg.breite - 47.80) < 0.001, "${salzburg.breite}")
        assertTrue(kotlin.math.abs(salzburg.laenge - 13.05) < 0.001, "${salzburg.laenge}")
        assertEquals("Ort", salzburg.artName)
    }

    @Test
    fun eineFremdeKennungWirdAbgewiesen() {
        val f = File.createTempFile("namen", ".czn")
        try {
            schreibe(f, beispiel, kennung = "CZKARTE1".toByteArray())
            assertFailsWith<java.io.IOException> { Namensdatei.oeffne(f) }
        } finally {
            f.delete()
        }
    }

    @Test
    fun eineErfundeneAnzahlWirdAbgewiesen() {
        val f = File.createTempFile("namen", ".czn")
        try {
            schreibe(f, beispiel, anzahlLuege = 900_000_000)
            assertFailsWith<java.io.IOException> { Namensdatei.oeffne(f) }
        } finally {
            f.delete()
        }
    }

    @Test
    fun eineVerdrehteVersatztabelleWirdAbgewiesen() {
        // Die Suche ist binaer. Auf einer verdrehten Tabelle faende sie nicht
        // etwa nichts, sondern den falschen Ort -- und wer danach losgeht,
        // geht in die falsche Richtung.
        val f = File.createTempFile("namen", ".czn")
        try {
            schreibe(f, beispiel, versaetzeVerdrehen = true)
            assertFailsWith<java.io.IOException> { Namensdatei.oeffne(f) }
        } finally {
            f.delete()
        }
    }

    @Test
    fun dieFaltungLaesstNurBuchstabenUndZiffernStehen() {
        // Gefunden am 18.08.2026 an einem echten Namen aus Norddeutschland:
        // "Pharma-Ko³". Das Werkzeug hat die hochgestellte Drei
        // durchgelassen -- Pythons isalnum() nimmt sie an --, der Leser macht
        // ein Leerzeichen daraus. Der Schluessel stand damit in der Datei und
        // war durch keine Anfrage zu bilden.
        assertEquals("pharma ko", Namensdatei.falte("Pharma-Ko³"))
        // Gegenprobe: gewoehnliche Ziffern bleiben stehen.
        assertEquals("haus 3", Namensdatei.falte("Haus 3"))
    }

    @Test
    fun dieFaltungLaesstIhrEigenesErgebnisInRuhe() {
        // Der Punkt der ganzen Sache: Jeder Schluessel in der Datei muss ein
        // Festpunkt dieser Faltung sein. Waere er es nicht, gaebe es Namen,
        // die dastehen und trotzdem nicht zu tippen sind. Genau das prueft
        // packsign vor dem Unterschreiben an jedem einzelnen Eintrag.
        for (probe in listOf("St. Veit", "Pharma-Ko³", "Größenwahn", "  a  b  ", "Bad Reichenhall")) {
            val einmal = Namensdatei.falte(probe)
            assertEquals(einmal, Namensdatei.falte(einmal), probe)
        }
    }
}
