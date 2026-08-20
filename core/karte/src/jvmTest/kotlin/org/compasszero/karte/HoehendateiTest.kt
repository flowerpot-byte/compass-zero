package org.compasszero.karte

import java.io.File
import java.util.zip.Deflater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HoehendateiTest {

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
    private fun kachelbytes(grund: Int): ByteArray {
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
        return b.toByteArray()
    }

    private fun schreibe(ziel: File, kacheln: List<Triple<Int, Int, Int>>, grund: Int = 500) {
        val inhalte = kacheln.map { packe(kachelbytes(grund)) }
        val anfang = Hoehenformat.KOPF_BYTES + Hoehenformat.EINTRAG_BYTES * kacheln.size
        val kopf = ByteArray(Hoehenformat.KOPF_BYTES)
        Hoehenformat.KENNUNG.copyInto(kopf, 0)
        kopf[8] = Hoehenformat.FASSUNG.toByte()
        kopf[9] = 2                       // Kante 4
        kopf[10] = kacheln.minOf { it.first }.toByte()
        kopf[11] = kacheln.maxOf { it.first }.toByte()
        for (i in 0 until 4) kopf[28 + i] = (kacheln.size shr (8 * i)).toByte()
        for (i in 0 until 8) kopf[32 + i] = (anfang.toLong() shr (8 * i)).toByte()

        ziel.outputStream().use { aus ->
            aus.write(kopf)
            var versatz = anfang.toLong()
            for (i in kacheln.indices) {
                val (z, x, y) = kacheln[i]
                aus.write(z)
                for (k in 0 until 4) aus.write((x shr (8 * k)) and 0xFF)
                for (k in 0 until 4) aus.write((y shr (8 * k)) and 0xFF)
                for (k in 0 until 8) aus.write(((versatz shr (8 * k)) and 0xFF).toInt())
                for (k in 0 until 4) aus.write((inhalte[i].size shr (8 * k)) and 0xFF)
                versatz += inhalte[i].size
            }
            for (inhalt in inhalte) aus.write(inhalt)
        }
    }

    private fun temp(name: String) =
        File.createTempFile("hoehen-$name", ".czh").also { it.deleteOnExit() }

    @Test
    fun eineHoehendateiWirdGelesen() {
        val datei = temp("gut")
        schreibe(datei, listOf(Triple(8, 134, 89), Triple(8, 135, 89)))
        Hoehendatei.oeffne(datei).use { hoehen ->
            assertEquals(4, hoehen.kante)
            assertEquals(2, hoehen.kachelzahl)
            val kachel = hoehen.kachel(8, 135, 89)
            assertTrue(kachel != null)
            assertEquals(500, kachel!!.hoehe(0, 0))
            assertEquals(530, kachel.hoehe(3, 0))
            assertEquals(8, kachel.zoom)
            assertEquals(135, kachel.kachelX)
        }
    }

    /**
     * Der Rueckgriff auf eine groebere Stufe muss die KACHELNUMMERN mitrechnen.
     *
     * Am 04.08.2026 tat er das nicht: Die Stufe wurde gesenkt, die Nummern
     * blieben die der feinen Stufe, und gesucht wurde eine Kachel, die es auf
     * der groben gar nicht geben kann. Das gab keine Fehlermeldung, sondern
     * gar keine Schummerung -- und eine Karte ohne Schummerung sieht aus wie
     * flaches Land, nicht wie ein Fehler.
     */
    @Test
    fun einRueckgriffAufEineGroebereStufeRechnetDieNummernMit() {
        val datei = temp("rueckgriff")
        schreibe(datei, listOf(Triple(10, 549, 356)))
        Hoehendatei.oeffne(datei).use { hoehen ->
            // Die Kartenkachel 12/2196/1424 liegt in der Hoehenkachel 10/549/356.
            val kachel = hoehen.kachel(12, 2196, 1424)
            assertTrue(kachel != null, "kein Rueckgriff auf die groebere Stufe")
            assertEquals(10, kachel!!.zoom)
            assertEquals(549, kachel.kachelX)
            assertEquals(356, kachel.kachelY)
        }
    }

    @Test
    fun eineKachelAusserhalbGibtNull() {
        val datei = temp("leer")
        schreibe(datei, listOf(Triple(8, 134, 89)))
        Hoehendatei.oeffne(datei).use { hoehen ->
            assertNull(hoehen.kachel(8, 200, 200))
        }
    }

    @Test
    fun eineFremdeDateiWirdAbgelehnt() {
        val datei = temp("fremd")
        datei.writeBytes(ByteArray(200) { 0x33 })
        assertFailsWith<Kartenfehler> { Hoehendatei.oeffne(datei) }
    }

    @Test
    fun einAbgeschnittenesVerzeichnisWirdAbgelehnt() {
        val datei = temp("kurz")
        schreibe(datei, listOf(Triple(8, 134, 89), Triple(8, 135, 89)))
        val ganz = datei.readBytes()
        datei.writeBytes(ganz.copyOf(ganz.size - 12))
        assertFailsWith<Kartenfehler> { Hoehendatei.oeffne(datei) }
    }
}
