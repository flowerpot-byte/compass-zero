package org.compasszero.karte

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Prueft den Kachelleser gegen selbst gebaute Kacheln.
 *
 * Der Schreiber hier ist absichtlich ein zweiter, unabhaengiger Weg zu
 * denselben Bytes -- er ist die Uebersetzung von `docs/KARTEN-FORMAT.md` nach
 * Kotlin, waehrend `tools/karte/bauen.py` dieselbe Beschreibung nach Python
 * uebersetzt. Stimmen beide ueberein, ist die Beschreibung eindeutig. Wuerde
 * der Test den Erzeuger aus dem Werkzeug borgen, prueften sich zwei Haelften
 * derselben Annahme gegenseitig.
 */
class KachelleserTest {

    private class Schreiber {
        val bytes = ArrayList<Byte>()

        fun roh(wert: Int) = apply { bytes.add((wert and 0xFF).toByte()) }

        fun varint(wert: Int) = apply {
            var rest = wert
            while (rest >= 128) {
                bytes.add(((rest and 127) or 128).toByte())
                rest = rest ushr 7
            }
            bytes.add(rest.toByte())
        }

        fun zigzag(wert: Int) = varint((wert shl 1) xor (wert shr 31))

        fun text(wort: String) = apply {
            val roh = wort.encodeToByteArray()
            varint(roh.size)
            roh.forEach { bytes.add(it) }
        }

        fun fertig(): ByteArray = bytes.toByteArray()
    }

    /** Eine Kachel mit einer Linie, einem Ring und einem benannten Punkt. */
    private fun beispiel(): ByteArray {
        val s = Schreiber()
        s.roh(Kartenformat.FASSUNG)
        s.varint(2)
        s.text("Innsbruck")
        s.text("Zuckerhütl")
        s.varint(2)

        // Schicht 1: ein Fluss als Linie
        s.roh(Kartenformat.FLUSS)
        s.varint(1)
        s.roh(Kartenformat.Art.LINIE)
        s.varint(0)          // Punktart
        s.varint(0)          // ohne Namen
        s.varint(3)
        s.zigzag(100); s.zigzag(200)
        s.zigzag(50); s.zigzag(-30)
        s.zigzag(-10); s.zigzag(-170)

        // Schicht 2: ein Gipfel mit Namen und ein Ring
        s.roh(Kartenformat.PUNKT)
        s.varint(2)
        s.roh(Kartenformat.Art.PUNKT)
        s.varint(5)          // gipfel
        s.varint(2)          // Name Nummer 1 -> "Zuckerhütl"
        s.varint(1)
        s.zigzag(2048); s.zigzag(1024)
        s.roh(Kartenformat.Art.AUSSENRING)
        s.varint(0)
        s.varint(1)          // Name Nummer 0 -> "Innsbruck"
        s.varint(4)
        s.zigzag(0); s.zigzag(0)
        s.zigzag(400); s.zigzag(0)
        s.zigzag(0); s.zigzag(400)
        s.zigzag(-400); s.zigzag(-400)
        return s.fertig()
    }

    @Test
    fun kachelWirdVollstaendigGelesen() {
        val kachel = Kachelleser.lies(beispiel(), 12, 2200, 1430)

        assertEquals(12, kachel.zoom)
        assertEquals(2200, kachel.kachelX)
        assertEquals(1430, kachel.kachelY)
        assertEquals(3, kachel.objekte)
        assertEquals(8, kachel.stuetzpunkte)

        assertEquals(Kartenformat.FLUSS, kachel.sorte[0].toInt())
        assertEquals(Kartenformat.Art.LINIE, kachel.art[0].toInt())
        assertNull(kachel.namenVon(0))
        assertEquals(3, kachel.laenge[0])
        // Die Abstaende summieren sich auf: 100, 150, 140.
        assertEquals(100, kachel.x[0])
        assertEquals(150, kachel.x[1])
        assertEquals(140, kachel.x[2])
        assertEquals(200, kachel.y[0])
        assertEquals(170, kachel.y[1])
        assertEquals(0, kachel.y[2])

        assertEquals(Kartenformat.PUNKT, kachel.sorte[1].toInt())
        assertEquals(Kartenformat.Art.PUNKT, kachel.art[1].toInt())
        assertEquals("gipfel", Kartenformat.punktartName(kachel.punktart[1].toInt()))
        assertEquals("Zuckerhütl", kachel.namenVon(1))
        assertEquals(2048, kachel.x[3])
        assertEquals(1024, kachel.y[3])

        assertEquals(Kartenformat.Art.AUSSENRING, kachel.art[2].toInt())
        assertEquals("Innsbruck", kachel.namenVon(2))
        assertEquals(4, kachel.laenge[2])
        // Der Ring schliesst sich: der letzte Punkt liegt wieder am Anfang.
        val a = kachel.anfang[2]
        assertEquals(kachel.x[a], kachel.x[a + 3])
        assertEquals(kachel.y[a], kachel.y[a + 3])
    }

    @Test
    fun umlauteImNamenUeberlebenDieUebertragung() {
        val kachel = Kachelleser.lies(beispiel(), 12, 0, 0)
        assertTrue(kachel.namen.contains("Zuckerhütl"))
    }

    @Test
    fun unbekannterKachelaufbauWirdAbgelehnt() {
        val roh = beispiel()
        roh[0] = 7
        val fehler = assertFailsWith<Kartenfehler> { Kachelleser.lies(roh, 12, 0, 0) }
        assertTrue(fehler.message!!.contains("7"))
    }

    @Test
    fun unbekannteSorteWirdAbgelehnt() {
        val s = Schreiber()
        s.roh(Kartenformat.FASSUNG)
        s.varint(0)
        s.varint(1)
        s.roh(99)
        assertFailsWith<Kartenfehler> { Kachelleser.lies(s.fertig(), 12, 0, 0) }
    }

    @Test
    fun unbekannteArtWirdAbgelehnt() {
        val s = Schreiber()
        s.roh(Kartenformat.FASSUNG)
        s.varint(0)
        s.varint(1)
        s.roh(Kartenformat.WALD)
        s.varint(1)
        s.roh(9)
        assertFailsWith<Kartenfehler> { Kachelleser.lies(s.fertig(), 12, 0, 0) }
    }

    @Test
    fun abgeschnitteneKachelWirdAbgelehnt() {
        val ganz = beispiel()
        // Jeder Abschnitt muss auffliegen, nicht nur der letzte.
        for (laenge in 1 until ganz.size) {
            val stueck = ganz.copyOf(laenge)
            try {
                Kachelleser.lies(stueck, 12, 0, 0)
                throw AssertionError("Kachel mit $laenge von ${ganz.size} Bytes ging durch")
            } catch (erwartet: Kartenfehler) {
                // richtig so
            }
        }
    }

    @Test
    fun bytesHinterDerKachelWerdenGemeldet() {
        val ganz = beispiel()
        val mitAnhang = ganz.copyOf(ganz.size + 3)
        val fehler = assertFailsWith<Kartenfehler> { Kachelleser.lies(mitAnhang, 12, 0, 0) }
        assertTrue(fehler.message!!.contains("unerklaerte"))
    }

    @Test
    fun eineErfundeneObjektzahlBelegtKeinenSpeicher() {
        val s = Schreiber()
        s.roh(Kartenformat.FASSUNG)
        s.varint(0)
        s.varint(1)
        s.roh(Kartenformat.WALD)
        s.varint(2_000_000_000)
        val fehler = assertFailsWith<Kartenfehler> { Kachelleser.lies(s.fertig(), 12, 0, 0) }
        assertTrue(fehler.message!!.contains("Objektzahl"))
    }

    @Test
    fun einUeberlangesVarintWirdAbgelehnt() {
        val s = Schreiber()
        s.roh(Kartenformat.FASSUNG)
        // Zehn Fortsetzungsbytes: der Wert liefe still ueber und wuerde negativ.
        repeat(10) { s.roh(0xFF) }
        s.roh(0x01)
        assertFailsWith<Kartenfehler> { Kachelleser.lies(s.fertig(), 12, 0, 0) }
    }

    @Test
    fun eineNamensnummerAusserhalbDerListeWirdAbgelehnt() {
        val s = Schreiber()
        s.roh(Kartenformat.FASSUNG)
        s.varint(1)
        s.text("Wien")
        s.varint(1)
        s.roh(Kartenformat.ORT)
        s.varint(1)
        s.roh(Kartenformat.Art.PUNKT)
        s.varint(0)
        s.varint(5)          // es gibt nur einen Namen
        assertFailsWith<Kartenfehler> { Kachelleser.lies(s.fertig(), 12, 0, 0) }
    }

    @Test
    fun einObjektOhneStuetzpunktWirdAbgelehnt() {
        val s = Schreiber()
        s.roh(Kartenformat.FASSUNG)
        s.varint(0)
        s.varint(1)
        s.roh(Kartenformat.WALD)
        s.varint(1)
        s.roh(Kartenformat.Art.LINIE)
        s.varint(0)
        s.varint(0)
        s.varint(0)
        val fehler = assertFailsWith<Kartenfehler> { Kachelleser.lies(s.fertig(), 12, 0, 0) }
        assertTrue(fehler.message!!.contains("Stuetzpunkt"))
    }

    @Test
    fun negativeAbstaendeKommenRichtigAn() {
        val s = Schreiber()
        s.roh(Kartenformat.FASSUNG)
        s.varint(0)
        s.varint(1)
        s.roh(Kartenformat.BACH)
        s.varint(1)
        s.roh(Kartenformat.Art.LINIE)
        s.varint(0)
        s.varint(0)
        s.varint(4)
        // Ein Bach, der ueber den linken und oberen Kachelrand hinauslaeuft.
        s.zigzag(-64); s.zigzag(-64)
        s.zigzag(1); s.zigzag(0)
        s.zigzag(-1); s.zigzag(4200)
        s.zigzag(0); s.zigzag(-1)
        val kachel = Kachelleser.lies(s.fertig(), 12, 0, 0)
        assertEquals(listOf(-64, -63, -64, -64), kachel.x.toList())
        assertEquals(listOf(-64, -64, 4136, 4135), kachel.y.toList())
    }

    @Test
    fun eineLeereKachelIstGueltig() {
        val s = Schreiber()
        s.roh(Kartenformat.FASSUNG)
        s.varint(0)
        s.varint(0)
        val kachel = Kachelleser.lies(s.fertig(), 5, 16, 11)
        assertEquals(0, kachel.objekte)
        assertEquals(0, kachel.stuetzpunkte)
    }
}
