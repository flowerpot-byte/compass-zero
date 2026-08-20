package org.compasszero.karte

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Eine gefundene Route: die Stellen, die man abgeht, und was sie kostet. */
class Route(
    /** Der Weg als Linienzug, Breite/Laenge je Punkt. */
    val punkte: List<DoubleArray>,
    /** Die wirkliche Laenge in Metern -- nicht die gewichtete. */
    val meter: Double,
)

/**
 * Das Wegenetz `.czw` und die Wegesuche darauf. Aufbau in
 * `docs/WEGE-FORMAT.md`.
 *
 * WAS BEIM OEFFNEN IM SPEICHER LANDET: Knoten, Kantenenden, Laengen und die
 * Nachbarschaft. Bei 76 000 Knoten und 93 000 Kanten sind das ein paar
 * Megabyte -- der Preis dafuer, dass eine Suche nicht bei jedem Schritt auf
 * die Platte greift.
 *
 * WAS AUF DER PLATTE BLEIBT: die Geometrie der Kanten. Sie wird erst gelesen,
 * wenn eine Route wirklich gezeichnet wird, und das sind ein paar hundert
 * Kanten statt aller.
 */
class Wegenetz private constructor(
    private val datei: RandomAccessFile,
    val knotenzahl: Int,
    val kantenzahl: Int,
    val west: Double,
    val sued: Double,
    val ost: Double,
    val nord: Double,
    private val knotenLon: IntArray,
    private val knotenLat: IntArray,
    private val kanteA: IntArray,
    private val kanteB: IntArray,
    private val kanteKosten: FloatArray,
    private val kanteMeter: IntArray,
    private val kanteVersatz: IntArray,
    private val nachbarAnfang: IntArray,
    private val nachbarKante: IntArray,
    /** Wo der Inhalt beginnt -- 0 ohne Umschlag, sonst hinter ihm. */
    private val versatz0: Long,
) : AutoCloseable {

    fun breiteVon(knoten: Int): Double = knotenLat[knoten] / 1e7
    fun laengeVon(knoten: Int): Double = knotenLon[knoten] / 1e7

    /**
     * Der naechste Knoten zu einer Stelle, oder -1.
     *
     * Reihum ueber alle Knoten. Bei 76 000 sind das ein paar Millisekunden --
     * ein Baum darueber waere schneller und eine Fehlerquelle mehr, und
     * gesucht wird nur, wenn jemand einen Punkt setzt.
     *
     * [hoechstens] begrenzt, wie weit der Knoten entfernt sein darf. Ohne das
     * bekaeme ein Tipp weit ausserhalb des Netzes den naechstbesten Knoten am
     * Rand -- und eine Route, die dort anfaengt, ist kein Vorschlag, sondern
     * eine Irrefuehrung.
     */
    fun naechsterKnoten(breite: Double, laenge: Double, hoechstens: Double = 2000.0): Int {
        var bester = -1
        var besteEntfernung = hoechstens
        for (i in 0 until knotenzahl) {
            val d = entfernung(breite, laenge, knotenLat[i] / 1e7, knotenLon[i] / 1e7)
            if (d < besteEntfernung) {
                besteEntfernung = d
                bester = i
            }
        }
        return bester
    }

    /**
     * Sucht den guenstigsten Weg zwischen zwei Knoten -- A-Stern.
     *
     * DIE SCHAETZUNG IST DIE LUFTLINIE, und sie darf die wirklichen Kosten
     * nie ueberschaetzen: Der kleinste Aufschlag im Netz ist 1,0, also ist
     * die Luftlinie in Metern immer kleiner oder gleich den Kosten. Waere sie
     * groesser, faende die Suche schnell eine Route -- nur nicht die beste,
     * und niemand saehe es der Linie an.
     *
     * Gibt null zurueck, wenn es keine Verbindung gibt. Das kommt vor: Eine
     * Insel im Wegenetz ist kein Fehler, sondern ein Gebiet ohne Anschluss.
     */
    fun route(von: Int, nach: Int): Route? {
        if (von < 0 || nach < 0 || von >= knotenzahl || nach >= knotenzahl) return null
        if (von == nach) return Route(listOf(doubleArrayOf(breiteVon(von), laengeVon(von))), 0.0)

        val bisher = DoubleArray(knotenzahl) { Double.MAX_VALUE }
        val vorgaenger = IntArray(knotenzahl) { -1 }
        val ueberKante = IntArray(knotenzahl) { -1 }
        val fertig = BooleanArray(knotenzahl)
        bisher[von] = 0.0

        val zielBreite = breiteVon(nach)
        val zielLaenge = laengeVon(nach)
        // Sortiert nach geschaetzten Gesamtkosten. Ein Knoten kann mehrfach
        // hineingelegt werden; wer schon fertig ist, wird beim Herausnehmen
        // uebersprungen -- das ist billiger, als in der Schlange zu suchen.
        val schlange = java.util.PriorityQueue<LongArray>(
            64,
            compareBy { java.lang.Double.longBitsToDouble(it[0]) },
        )
        schlange.add(
            longArrayOf(
                java.lang.Double.doubleToRawLongBits(
                    entfernung(breiteVon(von), laengeVon(von), zielBreite, zielLaenge),
                ),
                von.toLong(),
            ),
        )

        while (schlange.isNotEmpty()) {
            // poll() kann null geben, wenn die Schlange leer ist. Das kann
            // hier nicht passieren -- geprueft wird es trotzdem, statt es mit
            // zwei Ausrufezeichen zu behaupten.
            val naechster = schlange.poll() ?: break
            val hier = naechster[1].toInt()
            if (fertig[hier]) continue
            if (hier == nach) break
            fertig[hier] = true
            for (stelle in nachbarAnfang[hier] until nachbarAnfang[hier + 1]) {
                val kante = nachbarKante[stelle]
                val drueben = if (kanteA[kante] == hier) kanteB[kante] else kanteA[kante]
                if (fertig[drueben]) continue
                val neu = bisher[hier] + kanteMeter[kante] * kanteKosten[kante]
                if (neu >= bisher[drueben]) continue
                bisher[drueben] = neu
                vorgaenger[drueben] = hier
                ueberKante[drueben] = kante
                val schaetzung = neu + entfernung(
                    knotenLat[drueben] / 1e7, knotenLon[drueben] / 1e7,
                    zielBreite, zielLaenge,
                )
                schlange.add(
                    longArrayOf(
                        java.lang.Double.doubleToRawLongBits(schaetzung),
                        drueben.toLong(),
                    ),
                )
            }
        }

        if (vorgaenger[nach] < 0 && von != nach) return null

        // Rueckwaerts durchgehen und dabei die Geometrie einsammeln.
        val kettenKanten = ArrayList<Int>()
        val kettenKnoten = ArrayList<Int>()
        var lauf = nach
        while (lauf != von) {
            val kante = ueberKante[lauf]
            if (kante < 0) return null
            kettenKanten.add(kante)
            kettenKnoten.add(lauf)
            lauf = vorgaenger[lauf]
        }
        kettenKanten.reverse()
        kettenKnoten.reverse()

        val punkte = ArrayList<DoubleArray>()
        var meter = 0.0
        var stand = von
        for ((i, kante) in kettenKanten.withIndex()) {
            val roh = geometrie(kante)
            // Die Kante kann in beide Richtungen begangen werden; ihre
            // Geometrie liegt aber in einer festen. Wer sie verkehrt herum
            // anhaengt, bekommt eine Linie, die vor und zurueck springt.
            val vorwaerts = kanteA[kante] == stand
            val folge = if (vorwaerts) roh else roh.reversed()
            for ((k, punkt) in folge.withIndex()) {
                if (i > 0 && k == 0) continue
                punkte.add(punkt)
            }
            meter += kanteMeter[kante].toDouble()
            stand = kettenKnoten[i]
        }
        return Route(punkte, meter)
    }

    /** Die Stuetzpunkte einer Kante, Breite/Laenge -- von der Platte. */
    fun geometrie(kante: Int): List<DoubleArray> {
        synchronized(datei) {
            datei.seek(versatz0 + kanteVersatz[kante].toLong() + 14L)
            val anzahl = liesU16()
            if (anzahl < 2 || anzahl > MAX_PUNKTE) {
                throw IOException("Kante $kante hat $anzahl Punkte")
            }
            val roh = ByteArray(8 * anzahl)
            datei.readFully(roh)
            val aus = ArrayList<DoubleArray>(anzahl)
            for (i in 0 until anzahl) {
                aus.add(
                    doubleArrayOf(
                        leseInt(roh, i * 8 + 4) / 1e7,
                        leseInt(roh, i * 8) / 1e7,
                    ),
                )
            }
            return aus
        }
    }

    /**
     * Die beiden Enden einer Kante und ihre angegebene Laenge.
     *
     * Die Wegesuche kommt ohne diese Zugaenge aus -- sie hat die Felder
     * selbst. Das Werkzeug, das die Datei unterschreibt, braucht sie: Es
     * rechnet nach, ob die Geometrie einer Kante wirklich an ihren Knoten
     * beginnt und ob die angegebene Laenge zu ihr passt.
     */
    fun knotenA(kante: Int): Int = kanteA[kante]

    fun knotenB(kante: Int): Int = kanteB[kante]

    fun meterVon(kante: Int): Int = kanteMeter[kante]

    override fun close() = datei.close()

    private fun liesU16(): Int {
        val a = datei.read()
        val b = datei.read()
        if (a < 0 || b < 0) throw IOException("Datei endet zu frueh")
        return a or (b shl 8)
    }

    companion object {

        val KENNUNG = byteArrayOf(0x43, 0x5A, 0x57, 0x45, 0x47, 0x30, 0x30, 0x31) // "CZWEG001"
        const val FASSUNG = 1
        const val KOPF_BYTES = 40
        const val MAX_KNOTEN = 20_000_000
        const val MAX_KANTEN = 40_000_000
        const val MAX_PUNKTE = 65_535
        const val ERDRADIUS = 6_371_000.0

        /** Meter zwischen zwei Stellen. */
        fun entfernung(breite1: Double, laenge1: Double, breite2: Double, laenge2: Double): Double {
            val p1 = Math.toRadians(breite1)
            val p2 = Math.toRadians(breite2)
            val dp = Math.toRadians(breite2 - breite1)
            val dl = Math.toRadians(laenge2 - laenge1)
            val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
            return 2 * ERDRADIUS * asin(min(1.0, sqrt(a)))
        }

        fun oeffne(pfad: File, versatz0: Long = 0L): Wegenetz {
            val datei = RandomAccessFile(pfad, "r")
            try {
                val kopf = ByteArray(KOPF_BYTES)
                datei.seek(versatz0)
                datei.readFully(kopf)
                for (i in KENNUNG.indices) {
                    if (kopf[i] != KENNUNG[i]) throw IOException("keine .czw-Datei")
                }
                val fassung = kopf[8].toInt() and 0xFF
                if (fassung != FASSUNG) throw IOException("Wegefassung $fassung wird nicht gelesen")
                val knotenzahl = leseInt(kopf, 12)
                val kantenzahl = leseInt(kopf, 16)
                if (knotenzahl <= 0 || knotenzahl > MAX_KNOTEN) {
                    throw IOException("Knotenzahl $knotenzahl ist unmoeglich")
                }
                if (kantenzahl <= 0 || kantenzahl > MAX_KANTEN) {
                    throw IOException("Kantenzahl $kantenzahl ist unmoeglich")
                }

                val knotenRoh = ByteArray(8 * knotenzahl)
                datei.readFully(knotenRoh)
                val knotenLon = IntArray(knotenzahl)
                val knotenLat = IntArray(knotenzahl)
                for (i in 0 until knotenzahl) {
                    knotenLon[i] = leseInt(knotenRoh, i * 8)
                    knotenLat[i] = leseInt(knotenRoh, i * 8 + 4)
                }

                val versatzRoh = ByteArray(4 * kantenzahl)
                datei.readFully(versatzRoh)
                val kanteVersatz = IntArray(kantenzahl) { leseInt(versatzRoh, it * 4) }

                val kanteA = IntArray(kantenzahl)
                val kanteB = IntArray(kantenzahl)
                val kanteKosten = FloatArray(kantenzahl)
                val kanteMeter = IntArray(kantenzahl)
                val kopfRoh = ByteArray(14)
                for (i in 0 until kantenzahl) {
                    datei.seek(versatz0 + kanteVersatz[i].toLong())
                    datei.readFully(kopfRoh)
                    val a = leseInt(kopfRoh, 0)
                    val b = leseInt(kopfRoh, 4)
                    if (a < 0 || a >= knotenzahl || b < 0 || b >= knotenzahl) {
                        throw IOException("Kante $i zeigt auf einen Knoten, den es nicht gibt")
                    }
                    kanteA[i] = a
                    kanteB[i] = b
                    val aufschlag = (kopfRoh[8].toInt() and 0xFF) or ((kopfRoh[9].toInt() and 0xFF) shl 8)
                    // Ein Aufschlag unter 1,0 wuerde die Schaetzung der
                    // Wegesuche ungueltig machen -- sie rechnet mit der
                    // Luftlinie als Untergrenze.
                    if (aufschlag < 100) throw IOException("Kante $i hat einen Aufschlag unter 1,0")
                    kanteKosten[i] = aufschlag / 100f
                    kanteMeter[i] = leseInt(kopfRoh, 10)
                    if (kanteMeter[i] < 0) throw IOException("Kante $i hat eine negative Laenge")
                }

                // Nachbarschaft einmal ausrechnen: erst zaehlen, dann fuellen.
                val nachbarAnfang = IntArray(knotenzahl + 1)
                for (i in 0 until kantenzahl) {
                    nachbarAnfang[kanteA[i] + 1]++
                    nachbarAnfang[kanteB[i] + 1]++
                }
                for (i in 1..knotenzahl) nachbarAnfang[i] += nachbarAnfang[i - 1]
                val fuellstand = nachbarAnfang.copyOf()
                val nachbarKante = IntArray(2 * kantenzahl)
                for (i in 0 until kantenzahl) {
                    nachbarKante[fuellstand[kanteA[i]]++] = i
                    nachbarKante[fuellstand[kanteB[i]]++] = i
                }

                return Wegenetz(
                    datei, knotenzahl, kantenzahl,
                    leseInt(kopf, 20) / 1e7, leseInt(kopf, 24) / 1e7,
                    leseInt(kopf, 28) / 1e7, leseInt(kopf, 32) / 1e7,
                    knotenLon, knotenLat, kanteA, kanteB, kanteKosten, kanteMeter,
                    kanteVersatz, nachbarAnfang, nachbarKante, versatz0,
                )
            } catch (fehler: Throwable) {
                datei.close()
                throw fehler
            }
        }

        private fun leseInt(b: ByteArray, p: Int): Int =
            (b[p].toInt() and 0xFF) or
                ((b[p + 1].toInt() and 0xFF) shl 8) or
                ((b[p + 2].toInt() and 0xFF) shl 16) or
                ((b[p + 3].toInt() and 0xFF) shl 24)
    }
}
