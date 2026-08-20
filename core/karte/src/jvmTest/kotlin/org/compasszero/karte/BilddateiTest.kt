package org.compasszero.karte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Der Leser der Satellitenbild-Datei.
 *
 * Gebaut wird hier jedes Mal eine echte kleine `.czb` und nicht eine aus
 * `work/` genommen: Ein Test, der eine Datei von aussen braucht, laeuft nach
 * einem frischen Klon nicht -- und ein Test, der dann stillschweigend
 * uebersprungen wird, ist schlimmer als keiner.
 */
class BilddateiTest {

    private fun schreibeDatei(
        ziel: File,
        kacheln: List<Triple<Int, Int, Int>>,
        inhalt: (Int) -> ByteArray = { ByteArray(8) { b -> (it + b).toByte() } },
        kennung: ByteArray = Bilddatei.KENNUNG,
        fassung: Int = Bilddatei.FASSUNG,
        anzahlLuege: Int? = null,
        laengeLuege: Int? = null,
    ) {
        val inhalte = kacheln.indices.map(inhalt)
        val anfang = Bilddatei.KOPF_BYTES + Bilddatei.EINTRAG_BYTES * kacheln.size
        val kopf = ByteArray(Bilddatei.KOPF_BYTES)
        kennung.copyInto(kopf, 0, 0, minOf(8, kennung.size))
        kopf[8] = fassung.toByte()
        kopf[9] = 8
        kopf[10] = kacheln.minOf { it.first }.toByte()
        kopf[11] = kacheln.maxOf { it.first }.toByte()
        setzeInt(kopf, 12, 126_000_000)
        setzeInt(kopf, 16, 474_000_000)
        setzeInt(kopf, 20, 136_000_000)
        setzeInt(kopf, 24, 480_000_000)
        setzeInt(kopf, 28, anzahlLuege ?: kacheln.size)
        setzeLong(kopf, 32, anfang.toLong())
        setzeInt(kopf, 40, 20_250_812)
        setzeInt(kopf, 44, 20_250_919)

        ziel.outputStream().use { aus ->
            aus.write(kopf)
            var versatz = anfang.toLong()
            kacheln.forEachIndexed { i, (z, x, y) ->
                val e = ByteArray(Bilddatei.EINTRAG_BYTES)
                e[0] = z.toByte()
                setzeInt(e, 1, x)
                setzeInt(e, 5, y)
                setzeLong(e, 9, versatz)
                setzeInt(e, 17, laengeLuege ?: inhalte[i].size)
                aus.write(e)
                versatz += inhalte[i].size
            }
            inhalte.forEach { aus.write(it) }
        }
    }

    private fun setzeInt(b: ByteArray, p: Int, w: Int) {
        for (i in 0 until 4) b[p + i] = ((w shr (8 * i)) and 0xFF).toByte()
    }

    private fun setzeLong(b: ByteArray, p: Int, w: Long) {
        for (i in 0 until 8) b[p + i] = ((w shr (8 * i)) and 0xFF).toByte()
    }

    private fun mitDatei(name: String, tu: (File) -> Unit) {
        val f = File.createTempFile(name, ".czb")
        try {
            tu(f)
        } finally {
            f.delete()
        }
    }

    @Test
    fun eineKachelKommtSoZurueckWieSieGeschriebenWurde() = mitDatei("bild") { f ->
        schreibeDatei(f, listOf(Triple(12, 2196, 1427)))
        Bilddatei.oeffne(f).use { d ->
            assertEquals(256, d.kante)
            assertEquals(1, d.kachelzahl)
            assertEquals(12, d.zoomKleinste)
            assertEquals(12, d.zoomGroesste)
            assertEquals(20_250_812, d.aufnahmeVon)
            assertEquals(20_250_919, d.aufnahmeBis)
            val k = assertNotNull(d.kachel(12, 2196, 1427))
            assertEquals(12, k.zoom)
            assertEquals(2196, k.x)
            assertEquals(1427, k.y)
            assertEquals(8, k.roh.size)
        }
    }

    @Test
    fun dasGebietStehtImKopf() = mitDatei("bild") { f ->
        schreibeDatei(f, listOf(Triple(12, 2196, 1427)))
        Bilddatei.oeffne(f).use { d ->
            assertTrue(d.west in 12.59..12.61, "West war ${d.west}")
            assertTrue(d.nord in 47.99..48.01, "Nord war ${d.nord}")
        }
    }

    @Test
    fun wasEsNichtGibtWirdVonEinerGroeberenStufeGeholt() = mitDatei("bild") { f ->
        // Nur Stufe 10 vorhanden. Wer Stufe 13 verlangt, soll den groberen
        // Untergrund bekommen statt gar keinen -- ein unscharfes Bild ist
        // brauchbar, ein fehlendes nicht.
        schreibeDatei(f, listOf(Triple(10, 274, 178)))
        Bilddatei.oeffne(f).use { d ->
            val k = assertNotNull(d.kachel(13, 274 shl 3, 178 shl 3))
            assertEquals(10, k.zoom, "es haette die grobe Stufe kommen muessen")
            assertEquals(274, k.x)
            assertEquals(178, k.y)
        }
    }

    @Test
    fun ausserhalbDesPaketsKommtNichts() = mitDatei("bild") { f ->
        schreibeDatei(f, listOf(Triple(10, 274, 178)))
        Bilddatei.oeffne(f).use { d ->
            // Eine ganz andere Ecke der Welt: kein Bild, und das ist am Rand
            // eines Pakets der Normalfall und kein Fehler.
            assertNull(d.kachel(10, 5, 5))
        }
    }

    @Test
    fun einUnsortiertesVerzeichnisWirdAbgewiesen() = mitDatei("bild") { f ->
        // Die Suche ist binaer. Auf einem unsortierten Verzeichnis findet sie
        // nicht etwa nichts, sondern die FALSCHE Kachel -- ein Bild an der
        // falschen Stelle im Gelaende. Das muss beim Oeffnen auffallen.
        schreibeDatei(f, listOf(Triple(12, 2200, 1430), Triple(12, 2196, 1427)))
        assertFailsWith<java.io.IOException> { Bilddatei.oeffne(f) }
    }

    @Test
    fun eineErfundeneKachelgroesseWirdAbgewiesen() = mitDatei("bild") { f ->
        schreibeDatei(f, listOf(Triple(12, 2196, 1427)), laengeLuege = 900_000_000)
        Bilddatei.oeffne(f).use { d ->
            assertFailsWith<java.io.IOException> { d.kachel(12, 2196, 1427) }
        }
    }

    @Test
    fun eineFremdeKennungWirdAbgewiesen() = mitDatei("bild") { f ->
        schreibeDatei(f, listOf(Triple(12, 2196, 1427)), kennung = "CZKARTE1".toByteArray())
        assertFailsWith<java.io.IOException> { Bilddatei.oeffne(f) }
    }

    @Test
    fun eineFremdeFassungWirdAbgewiesen() = mitDatei("bild") { f ->
        schreibeDatei(f, listOf(Triple(12, 2196, 1427)), fassung = 9)
        assertFailsWith<java.io.IOException> { Bilddatei.oeffne(f) }
    }

    @Test
    fun eineErfundeneKachelzahlWirdAbgewiesen() = mitDatei("bild") { f ->
        // Vor jeder Speicheranforderung, nicht danach: Ein Verzeichnis mit
        // zwei Milliarden Eintraegen darf kein Feld dieser Groesse anfordern.
        schreibeDatei(f, listOf(Triple(12, 2196, 1427)), anzahlLuege = 1_500_000_000)
        assertFailsWith<java.io.IOException> { Bilddatei.oeffne(f) }
    }
}
