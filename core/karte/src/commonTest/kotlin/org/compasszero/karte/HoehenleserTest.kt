package org.compasszero.karte

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HoehenleserTest {

    private class Schreiber {
        val bytes = ArrayList<Byte>()

        fun roh(w: Int) = apply { bytes.add((w and 0xFF).toByte()) }

        fun int16(w: Int) = apply {
            bytes.add(((w shr 8) and 0xFF).toByte())
            bytes.add((w and 0xFF).toByte())
        }

        fun varint(w: Int) = apply {
            var r = w
            while (r >= 128) {
                bytes.add(((r and 127) or 128).toByte()); r = r ushr 7
            }
            bytes.add(r.toByte())
        }

        fun zigzag(w: Int) = varint((w shl 1) xor (w shr 31))

        fun fertig() = bytes.toByteArray()
    }

    /** Ein kleines Gelaende: 3x3, Grundhoehe 400 m, Schrittweite 1 m. */
    private fun beispiel(): ByteArray {
        val s = Schreiber()
        s.roh(Hoehenformat.FASSUNG)
        s.int16(400)
        s.roh(1)
        // Zeile 0: 405, 410, 408
        s.zigzag(5); s.zigzag(5); s.zigzag(-2)
        // Zeile 1: erster Wert gegen Zeile 0 Spalte 0 (405) -> 415
        s.zigzag(10); s.zigzag(-5); s.zigzag(-5)
        // Zeile 2: gegen 415 -> 395
        s.zigzag(-20); s.zigzag(2); s.zigzag(3)
        return s.fertig()
    }

    @Test
    fun einHoehenrasterWirdRichtigAufgebaut() {
        val kachel = Hoehenleser.lies(beispiel(), 10, 550, 358, 3)
        assertEquals(3, kachel.kante)
        assertEquals(listOf(405, 410, 408), (0..2).map { kachel.hoehe(it, 0) })
        assertEquals(listOf(415, 410, 405), (0..2).map { kachel.hoehe(it, 1) })
        assertEquals(listOf(395, 397, 400), (0..2).map { kachel.hoehe(it, 2) })
        assertEquals(395, kachel.kleinste)
        assertEquals(415, kachel.groesste)
        assertEquals(10, kachel.zoom)
        assertEquals(550, kachel.kachelX)
    }

    /**
     * Der Zeilenumbruch ist die Stelle, an der so ein Format gern kippt: Wer
     * den ersten Wert einer Zeile gegen das ENDE der vorigen Zeile rechnet,
     * bekommt ein Gelaende, das an jedem Zeilenrand quer ueber die Kachel
     * springt -- und das sieht im Schummerungsbild aus wie eine Terrasse.
     */
    @Test
    fun derZeilenanfangZaehltGegenDieZeileDarueber() {
        val kachel = Hoehenleser.lies(beispiel(), 10, 0, 0, 3)
        // Zeile 1 Spalte 0 ist 405 + 10 = 415, nicht 408 + 10 = 418.
        assertEquals(415, kachel.hoehe(0, 1))
    }

    @Test
    fun eineSchrittweiteVervielfachtDieUnterschiede() {
        val s = Schreiber()
        s.roh(Hoehenformat.FASSUNG)
        s.int16(1000)
        s.roh(10)
        s.zigzag(5); s.zigzag(-3)
        s.zigzag(2); s.zigzag(1)
        val kachel = Hoehenleser.lies(s.fertig(), 8, 0, 0, 2)
        assertEquals(1050, kachel.hoehe(0, 0))
        assertEquals(1020, kachel.hoehe(1, 0))
        assertEquals(1070, kachel.hoehe(0, 1))
        assertEquals(1080, kachel.hoehe(1, 1))
    }

    @Test
    fun eineUnbekannteSchrittweiteWirdAbgelehnt() {
        val s = Schreiber()
        s.roh(Hoehenformat.FASSUNG)
        s.int16(0)
        s.roh(7)
        val fehler = assertFailsWith<Kartenfehler> { Hoehenleser.lies(s.fertig(), 8, 0, 0, 2) }
        assertTrue(fehler.message!!.contains("Schrittweite"))
    }

    @Test
    fun eineHoeheAusserhalbDesErdreliefsWirdAbgelehnt() {
        val s = Schreiber()
        s.roh(Hoehenformat.FASSUNG)
        s.int16(8000)
        s.roh(10)
        s.zigzag(200)          // 8000 + 2000 = 10 000 m, hoeher als der Everest
        val fehler = assertFailsWith<Kartenfehler> { Hoehenleser.lies(s.fertig(), 8, 0, 0, 1) }
        assertTrue(fehler.message!!.contains("Erdrelief"))
    }

    @Test
    fun eineUnsinnigeGrundhoeheWirdAbgelehnt() {
        val s = Schreiber()
        s.roh(Hoehenformat.FASSUNG)
        s.int16(-30000)
        s.roh(1)
        assertFailsWith<Kartenfehler> { Hoehenleser.lies(s.fertig(), 8, 0, 0, 1) }
    }

    @Test
    fun eineUnbekannteFassungWirdAbgelehnt() {
        val roh = beispiel()
        roh[0] = 9
        assertFailsWith<Kartenfehler> { Hoehenleser.lies(roh, 10, 0, 0, 3) }
    }

    @Test
    fun eineAbgeschnitteneHoehenkachelWirdAbgelehnt() {
        val ganz = beispiel()
        for (laenge in 1 until ganz.size) {
            assertFailsWith<Kartenfehler>("Laenge $laenge ging durch") {
                Hoehenleser.lies(ganz.copyOf(laenge), 10, 0, 0, 3)
            }
        }
    }

    @Test
    fun bytesHinterDerHoehenkachelWerdenGemeldet() {
        val ganz = beispiel()
        val fehler = assertFailsWith<Kartenfehler> {
            Hoehenleser.lies(ganz.copyOf(ganz.size + 2), 10, 0, 0, 3)
        }
        assertTrue(fehler.message!!.contains("unerklaerte"))
    }

    @Test
    fun eineUnsinnigeKachelkanteWirdAbgelehnt() {
        assertFailsWith<Kartenfehler> { Hoehenleser.lies(beispiel(), 10, 0, 0, 0) }
        assertFailsWith<Kartenfehler> { Hoehenleser.lies(beispiel(), 10, 0, 0, 4096) }
    }
}
