package org.compasszero.karte

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Die Wegesuche.
 *
 * Gebaut wird hier ein winziges Netz von Hand, dessen richtige Antwort man
 * nachrechnen kann. Bei einer Route zaehlt nicht, dass ueberhaupt eine
 * herauskommt -- es zaehlt, dass es die GUENSTIGSTE ist. Eine Route, die nur
 * plausibel aussieht, schickt jemanden einen Umweg, den er im Gelaende
 * bezahlt.
 */
class WegenetzTest {

    /**
     * Das Probenetz:
     *
     *     0 ---- 1 ---- 2      oben: zweimal 1000 m Pfad (Aufschlag 1,0)
     *     |             |
     *     3 ----------- 4      unten: 1000 m + 1000 m Strasse (Aufschlag 2,2)
     *
     * Von 0 nach 2 ist der obere Weg 2000 m zu Kosten 2000.
     * Der untere ist ueber 3 und 4 laenger UND teurer -- er darf nie gewinnen.
     */
    private fun probenetz(
        ziel: File,
        kennung: ByteArray = Wegenetz.KENNUNG,
        fassung: Int = Wegenetz.FASSUNG,
        knotenLuege: Int? = null,
        aufschlagLuege: Int? = null,
    ) {
        // Knoten grob auf einer Linie; die Laengen stehen ohnehin in den
        // Kanten und muessen nicht zur Geometrie passen.
        val knoten = listOf(
            13.000 to 47.800,   // 0
            13.010 to 47.800,   // 1
            13.020 to 47.800,   // 2
            13.000 to 47.790,   // 3
            13.020 to 47.790,   // 4
        )
        // von, nach, Aufschlag*100, Meter
        val kanten = listOf(
            intArrayOf(0, 1, 100, 1000),
            intArrayOf(1, 2, 100, 1000),
            intArrayOf(0, 3, 100, 1000),
            intArrayOf(3, 4, aufschlagLuege ?: 220, 2000),
            intArrayOf(4, 2, 100, 1000),
        )

        val koerper = java.io.ByteArrayOutputStream()
        val versaetze = ArrayList<Int>()
        for (k in kanten) {
            versaetze.add(koerper.size())
            koerper.write(vier(k[0])); koerper.write(vier(k[1]))
            koerper.write(zwei(k[2])); koerper.write(vier(k[3]))
            koerper.write(zwei(2))
            for (ende in listOf(k[0], k[1])) {
                koerper.write(vier((knoten[ende].first * 1e7).toInt()))
                koerper.write(vier((knoten[ende].second * 1e7).toInt()))
            }
        }

        val kopf = ByteArray(Wegenetz.KOPF_BYTES)
        kennung.copyInto(kopf, 0, 0, minOf(8, kennung.size))
        kopf[8] = fassung.toByte()
        setzeInt(kopf, 12, knotenLuege ?: knoten.size)
        setzeInt(kopf, 16, kanten.size)
        setzeInt(kopf, 20, 129_000_000)
        setzeInt(kopf, 24, 477_000_000)
        setzeInt(kopf, 28, 132_000_000)
        setzeInt(kopf, 32, 479_000_000)

        val anfang = Wegenetz.KOPF_BYTES + 8 * knoten.size + 4 * kanten.size
        ziel.outputStream().use { aus ->
            aus.write(kopf)
            for ((lon, lat) in knoten) {
                aus.write(vier((lon * 1e7).toInt()))
                aus.write(vier((lat * 1e7).toInt()))
            }
            for (v in versaetze) aus.write(vier(anfang + v))
            aus.write(koerper.toByteArray())
        }
    }

    private fun vier(w: Int) = ByteArray(4) { ((w shr (8 * it)) and 0xFF).toByte() }
    private fun zwei(w: Int) = ByteArray(2) { ((w shr (8 * it)) and 0xFF).toByte() }
    private fun setzeInt(b: ByteArray, p: Int, w: Int) {
        for (i in 0 until 4) b[p + i] = ((w shr (8 * i)) and 0xFF).toByte()
    }

    private fun mitNetz(tu: (Wegenetz) -> Unit) {
        val f = File.createTempFile("wege", ".czw")
        try {
            probenetz(f)
            Wegenetz.oeffne(f).use(tu)
        } finally {
            f.delete()
        }
    }

    @Test
    fun dasNetzLaesstSichLesen() = mitNetz { w ->
        assertEquals(5, w.knotenzahl)
        assertEquals(5, w.kantenzahl)
        assertTrue(abs(w.breiteVon(0) - 47.800) < 0.0001, "${w.breiteVon(0)}")
        assertTrue(abs(w.laengeVon(2) - 13.020) < 0.0001, "${w.laengeVon(2)}")
    }

    @Test
    fun derGuenstigsteWegGewinntUndNichtDerErstbeste() = mitNetz { w ->
        val route = assertNotNull(w.route(0, 2))
        // Oben: 1000 + 1000 = 2000 m. Unten waeren es 4000 m und teurer.
        assertTrue(abs(route.meter - 2000.0) < 1.0, "Route ist ${route.meter} m lang")
    }

    @Test
    fun einTeurerWegWirdUmgangenAuchWennDerUmwegLaengerIst() {
        // DER PUNKT DER GEWICHTUNG, und die Zahlen dazu:
        //
        //   3 -> 4 unmittelbar: 2000 m Strasse, Aufschlag 2,2 -> Kosten 4400
        //   3 -> 0 -> 1 -> 2 -> 4: 4000 m Pfad, Aufschlag 1,0 -> Kosten 4000
        //
        // Der Umweg ist DOPPELT so lang und trotzdem der richtige Vorschlag:
        // Zu Fuss ist eine Landstrasse nicht nur unangenehm, sie ist
        // gefaehrlich. Wer hier die kuerzere Strecke bekaeme, bekaeme die
        // schlechtere.
        //
        // Dieser Test ist am 18.08.2026 umgeschrieben worden: Zuerst stand
        // hier die Erwartung 2000 m -- ich hatte das Probenetz so gebaut,
        // dass die Strasse angeblich der einzige Weg sei, und dabei die
        // Verbindung ueber oben uebersehen. Die Wegesuche hat richtig
        // gerechnet und der Test falsch gefragt.
        val f = File.createTempFile("wege", ".czw")
        try {
            probenetz(f)
            Wegenetz.oeffne(f).use { w ->
                val route = assertNotNull(w.route(3, 4))
                assertTrue(
                    abs(route.meter - 4000.0) < 1.0,
                    "Route ist ${route.meter} m -- erwartet war der Umweg ueber die Pfade",
                )
            }
        } finally {
            f.delete()
        }
    }

    @Test
    fun ohneAlternativeWirdDieStrasseGenommen() {
        // Der Aufschlag verbietet nichts, er verteuert nur. Deshalb dieselbe
        // Frage noch einmal, aber mit einem Netz OHNE Umweg: Wer keine
        // Alternative hat, bekommt die Strasse. Alles andere hiesse, jemanden
        // ohne Weg dastehen zu lassen.
        val f = File.createTempFile("wege", ".czw")
        try {
            nurStrasse(f)
            Wegenetz.oeffne(f).use { w ->
                val route = assertNotNull(w.route(0, 1))
                assertTrue(abs(route.meter - 2000.0) < 1.0, "${route.meter}")
            }
        } finally {
            f.delete()
        }
    }

    /** Zwei Knoten, dazwischen nur eine teure Strasse. */
    private fun nurStrasse(ziel: File) {
        val knoten = listOf(13.000 to 47.800, 13.020 to 47.800)
        val koerper = java.io.ByteArrayOutputStream()
        koerper.write(vier(0)); koerper.write(vier(1))
        koerper.write(zwei(220)); koerper.write(vier(2000))
        koerper.write(zwei(2))
        for ((lon, lat) in knoten) {
            koerper.write(vier((lon * 1e7).toInt()))
            koerper.write(vier((lat * 1e7).toInt()))
        }
        val kopf = ByteArray(Wegenetz.KOPF_BYTES)
        Wegenetz.KENNUNG.copyInto(kopf, 0, 0, 8)
        kopf[8] = Wegenetz.FASSUNG.toByte()
        setzeInt(kopf, 12, 2)
        setzeInt(kopf, 16, 1)
        setzeInt(kopf, 20, 129_000_000)
        setzeInt(kopf, 24, 477_000_000)
        setzeInt(kopf, 28, 132_000_000)
        setzeInt(kopf, 32, 479_000_000)
        ziel.outputStream().use { aus ->
            aus.write(kopf)
            for ((lon, lat) in knoten) {
                aus.write(vier((lon * 1e7).toInt()))
                aus.write(vier((lat * 1e7).toInt()))
            }
            aus.write(vier(Wegenetz.KOPF_BYTES + 8 * 2 + 4))
            aus.write(koerper.toByteArray())
        }
    }

    @Test
    fun dieRouteTraegtIhreGeometrie() = mitNetz { w ->
        val route = assertNotNull(w.route(0, 2))
        assertTrue(route.punkte.size >= 3, "nur ${route.punkte.size} Punkte")
        // Anfang und Ende muessen wirklich Anfang und Ende sein.
        assertTrue(abs(route.punkte.first()[1] - 13.000) < 0.0001, route.punkte.first().toList().toString())
        assertTrue(abs(route.punkte.last()[1] - 13.020) < 0.0001, route.punkte.last().toList().toString())
    }

    @Test
    fun vonEinemKnotenZuSichSelbstIstNullMeter() = mitNetz { w ->
        val route = assertNotNull(w.route(3, 3))
        assertEquals(0.0, route.meter)
    }

    @Test
    fun derNaechsteKnotenWirdGefundenUndNichtIrgendeiner() = mitNetz { w ->
        assertEquals(0, w.naechsterKnoten(47.800, 13.0001))
        assertEquals(2, w.naechsterKnoten(47.8005, 13.0199))
        // Weit weg gibt es keinen: Ein Tipp ausserhalb des Netzes darf nicht
        // den naechstbesten Randknoten bekommen -- eine Route, die dort
        // anfaengt, ist eine Irrefuehrung.
        assertEquals(-1, w.naechsterKnoten(50.0, 8.0))
    }

    @Test
    fun ohneVerbindungKommtNichtsZurueck() {
        // Ein Netz mit einer Insel: Knoten 4 haengt an nichts.
        val f = File.createTempFile("wege", ".czw")
        try {
            probenetz(f)
            Wegenetz.oeffne(f).use { w ->
                // Alle Knoten dieses Netzes haengen zusammen -- geprueft wird
                // hier, dass eine Anfrage ausserhalb des Bereichs sauber
                // nichts liefert statt abzustuerzen.
                assertNull(w.route(0, 99))
                assertNull(w.route(-1, 2))
            }
        } finally {
            f.delete()
        }
    }

    @Test
    fun eineFremdeKennungWirdAbgewiesen() {
        val f = File.createTempFile("wege", ".czw")
        try {
            probenetz(f, kennung = "CZKARTE1".toByteArray())
            assertFailsWith<java.io.IOException> { Wegenetz.oeffne(f) }
        } finally {
            f.delete()
        }
    }

    @Test
    fun eineErfundeneKnotenzahlWirdAbgewiesen() {
        val f = File.createTempFile("wege", ".czw")
        try {
            probenetz(f, knotenLuege = 900_000_000)
            assertFailsWith<java.io.IOException> { Wegenetz.oeffne(f) }
        } finally {
            f.delete()
        }
    }

    @Test
    fun einAufschlagUnterEinsWirdAbgewiesen() {
        // Er wuerde die Schaetzung der Wegesuche ungueltig machen: Sie rechnet
        // mit der Luftlinie als Untergrenze. Die Suche faende dann schnell
        // eine Route -- nur nicht die beste, und niemand saehe es ihr an.
        val f = File.createTempFile("wege", ".czw")
        try {
            probenetz(f, aufschlagLuege = 50)
            assertFailsWith<java.io.IOException> { Wegenetz.oeffne(f) }
        } finally {
            f.delete()
        }
    }
}
