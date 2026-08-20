package org.compasszero.packsign

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Die Kopffelder sind lueckenlos festgenagelt -- die letzte Nische war die
// angegebene Packgroesse selbst. Fremdbytes hinter dem Datenstrom, aber
// innerhalb der Angabe, fielen bisher keinem eigenen Code auf; gerettet haette
// das Werkzeug erst die Laufzeitbibliothek, also fremdes Verhalten.
class ZipAufbauTest {

    private fun presse(inhalt: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        try {
            deflater.setInput(inhalt)
            deflater.finish()
            val aus = ByteArrayOutputStream()
            val puffer = ByteArray(64 * 1024)
            while (!deflater.finished()) {
                val menge = deflater.deflate(puffer)
                aus.write(puffer, 0, menge)
            }
            return aus.toByteArray()
        } finally {
            deflater.end()
        }
    }

    // Baut ein ZIP von Hand, damit sich die angegebene Packgroesse von der
    // tatsaechlichen unterscheiden laesst.
    private fun zipMit(name: String, inhalt: ByteArray, anhang: ByteArray = ByteArray(0)): ByteArray {
        val nameBytes = name.encodeToByteArray()
        val gepresst = presse(inhalt) + anhang
        val pruefzahl = CRC32().apply { update(inhalt) }.value
        val aus = ByteArrayOutputStream()

        fun schreibe16(wert: Int) {
            aus.write(wert and 0xFF)
            aus.write((wert ushr 8) and 0xFF)
        }

        fun schreibe32(wert: Long) {
            for (i in 0 until 4) aus.write(((wert ushr (i * 8)) and 0xFF).toInt())
        }

        // Lokaler Kopf
        schreibe32(0x04034B50)
        schreibe16(20)
        schreibe16(0x800)
        schreibe16(8)
        schreibe16(0x0000)
        schreibe16(0x2821)
        schreibe32(pruefzahl)
        schreibe32(gepresst.size.toLong())
        schreibe32(inhalt.size.toLong())
        schreibe16(nameBytes.size)
        schreibe16(0)
        aus.write(nameBytes)
        aus.write(gepresst)
        val verzeichnisStart = aus.size().toLong()

        // Verzeichniseintrag
        schreibe32(0x02014B50)
        schreibe16(20)
        schreibe16(20)
        schreibe16(0x800)
        schreibe16(8)
        schreibe16(0x0000)
        schreibe16(0x2821)
        schreibe32(pruefzahl)
        schreibe32(gepresst.size.toLong())
        schreibe32(inhalt.size.toLong())
        schreibe16(nameBytes.size)
        schreibe16(0)
        schreibe16(0)
        schreibe16(0)
        schreibe16(0)
        schreibe32(0)
        schreibe32(0)
        aus.write(nameBytes)
        val verzeichnisLaenge = aus.size() - verzeichnisStart

        // Endstueck
        schreibe32(0x06054B50)
        schreibe16(0)
        schreibe16(0)
        schreibe16(1)
        schreibe16(1)
        schreibe32(verzeichnisLaenge)
        schreibe32(verzeichnisStart)
        schreibe16(0)
        return aus.toByteArray()
    }

    private val inhalt = """{"schema":1,"tips":[]}""".encodeToByteArray()

    @Test
    fun sauberesZipGehtDurch() {
        assertEquals(null, ZipAufbau.problem(zipMit("manifest.json", inhalt)))
    }

    // Der Befund: 1 050 Fremdbytes innerhalb der angegebenen Packgroesse wurden
    // fuer in Ordnung gehalten.
    @Test
    fun fremdbytesHinterDemDatenstromFallenAuf() {
        val anhang = ByteArray(1_050) { 0x42 }
        val problem = ZipAufbau.problem(zipMit("manifest.json", inhalt, anhang))
        assertTrue(problem != null, "Fremdbytes innerhalb der Packgroesse wurden angenommen")
        assertTrue(
            problem.contains("hinter dem Datenstrom"),
            "die Beanstandung benennt die Ursache nicht: $problem",
        )
    }

    @Test
    fun einZerstoerterDatenstromFaelltAuf() {
        val gebaut = zipMit("manifest.json", inhalt)
        // Mitten in die gepressten Daten greifen, ohne eine Laenge zu aendern.
        val kaputt = gebaut.copyOf()
        val stelle = 30 + "manifest.json".length + 3
        kaputt[stelle] = (kaputt[stelle].toInt() xor 0xFF).toByte()
        assertTrue(ZipAufbau.problem(kaputt) != null, "ein zerstoerter Datenstrom wurde angenommen")
    }
}
