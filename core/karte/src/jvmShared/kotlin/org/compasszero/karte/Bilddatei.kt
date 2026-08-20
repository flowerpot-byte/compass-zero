package org.compasszero.karte

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Eine Kachel des Satellitenbildes, so wie sie in der Datei liegt.
 *
 * [roh] ist ein fertiges JPEG. Es wird hier ABSICHTLICH NICHT dekodiert: Das
 * braucht auf jeder Plattform eine andere Bibliothek, und dieser Teil des
 * Programms soll ohne Geraet pruefbar bleiben.
 *
 * [zoom], [x] und [y] sind die der GELIEFERTEN Kachel und nicht die der
 * verlangten. Wer eine Stufe anfordert, die es nicht gibt, bekommt eine
 * groebere -- und muss daraus den Ausschnitt rechnen. Dasselbe gilt bei der
 * Hoehendatei, und aus demselben Grund: Ein stillschweigend zugeschnittenes
 * Bild waere hier nicht nachpruefbar, und ein Untergrund, der um eine halbe
 * Kachel verrutscht ist, sieht auf den ersten Blick richtig aus.
 */
class Bildkachel(val zoom: Int, val x: Int, val y: Int, val roh: ByteArray)

/**
 * Liest eine Satellitenbild-Datei `.czb`. Aufbau in `docs/BILD-FORMAT.md`.
 *
 * Wie bei `Kartendatei` und `Hoehendatei`: Das Verzeichnis liegt im Speicher,
 * die Kacheln bleiben auf dem Datentraeger. Bei einer Region auf Zoom 13 sind
 * das einige tausend Eintraege -- ein paar Zehntel Megabyte -- gegenueber
 * hunderten Megabyte Bilddaten, die niemand auf einmal braucht.
 */
class Bilddatei private constructor(
    private val datei: RandomAccessFile,
    val kante: Int,
    val zoomKleinste: Int,
    val zoomGroesste: Int,
    /** Frueheste und spaeteste Aufnahme als JJJJMMTT, 0 wenn unbekannt. */
    val aufnahmeVon: Int,
    val aufnahmeBis: Int,
    val west: Double,
    val sued: Double,
    val ost: Double,
    val nord: Double,
    private val schluessel: LongArray,
    private val versatz: LongArray,
    private val laenge: IntArray,
    /**
     * Wo der Inhalt beginnt -- 0 ohne Umschlag, sonst hinter ihm.
     *
     * Die Versaetze im Verzeichnis zaehlen ab dem INHALT und nicht ab dem
     * Dateianfang. Wer das vergisst, liest bei einer signierten Datei um 110
     * Bytes verschoben: Die Kacheln sind dann keine Bilder mehr, und die
     * Meldung dazu waere "kein JPEG" statt "falsch gerechnet".
     */
    private val versatz0: Long,
) : AutoCloseable {

    val kachelzahl: Int get() = schluessel.size

    /**
     * Holt das Bild fuer eine Kachel, notfalls von einer groeberen Stufe.
     *
     * Gibt null zurueck, wenn es fuer diese Stelle ueberhaupt kein Bild gibt.
     * Das ist der Normalfall am Rand eines Pakets und kein Fehler: Die Karte
     * zeichnet dann einfach ohne Untergrund weiter.
     */
    fun kachel(zoom: Int, x: Int, y: Int): Bildkachel? {
        var z = minOf(zoom, zoomGroesste)
        // Mit der Stufe muessen die Kachelnummern mit -- derselbe Fehler wie
        // am 04.08.2026 bei der Hoehendatei, wo die Nummern der feinen Stufe
        // auf einer groben gesucht wurden und einfach nichts herauskam.
        var tx = x shr (zoom - z).coerceAtLeast(0)
        var ty = y shr (zoom - z).coerceAtLeast(0)
        while (z >= zoomKleinste) {
            val stelle = suche(schluesselVon(z, tx, ty))
            if (stelle >= 0) {
                val n = laenge[stelle]
                if (n <= 0 || n > MAX_KACHEL_BYTES) {
                    throw IOException("Kachelgroesse $n ist unmoeglich")
                }
                val roh = ByteArray(n)
                synchronized(datei) {
                    datei.seek(versatz0 + versatz[stelle])
                    datei.readFully(roh)
                }
                return Bildkachel(z, tx, ty, roh)
            }
            z--
            tx = tx shr 1
            ty = ty shr 1
        }
        return null
    }

    /**
     * Alle vorhandenen Kacheln einer Zoomstufe, als (x, y).
     *
     * Die App braucht das nicht -- sie fragt nach der Stelle, die sie zeichnen
     * will. Das Werkzeug, das die Datei unterschreibt, braucht es: Es liest
     * jede Kachel einmal, statt dem Verzeichnis zu glauben.
     */
    fun kachelliste(zoom: Int): List<Pair<Int, Int>> {
        val aus = ArrayList<Pair<Int, Int>>()
        for (s in schluessel) {
            if ((s ushr 56).toInt() != zoom) continue
            aus.add(Pair(((s ushr 28) and 0xFFFFFFF).toInt(), (s and 0xFFFFFFF).toInt()))
        }
        return aus
    }

    override fun close() = datei.close()

    private fun schluesselVon(z: Int, x: Int, y: Int): Long =
        (z.toLong() shl 56) or (x.toLong() shl 28) or y.toLong()

    private fun suche(gesucht: Long): Int {
        var von = 0
        var bis = schluessel.size - 1
        while (von <= bis) {
            val mitte = (von + bis) ushr 1
            val hier = schluessel[mitte]
            when {
                hier < gesucht -> von = mitte + 1
                hier > gesucht -> bis = mitte - 1
                else -> return mitte
            }
        }
        return -1
    }

    companion object {

        val KENNUNG = byteArrayOf(0x43, 0x5A, 0x42, 0x49, 0x4C, 0x44, 0x30, 0x31) // "CZBILD01"
        const val FASSUNG = 1
        const val KOPF_BYTES = 48
        const val EINTRAG_BYTES = 21

        /**
         * Obergrenzen. Sie greifen VOR jeder Speicheranforderung -- eine
         * erfundene Laengenangabe darf kein Feld dieser Groesse anfordern,
         * sondern muss die Datei abweisen.
         */
        const val MAX_KACHEL_BYTES = 2 * 1024 * 1024
        const val MAX_KACHELN = 2_000_000
        // 14, nicht 13: Bei 48 Grad Nord zeigt Zoom 13 nur 12,8 Meter je
        // Bildpunkt, waehrend Sentinel-2 10 liefert -- ein Fuenftel der
        // Aufloesung ginge verloren. Ab 15 waere es reine Vergroesserung.
        // Naeher steht es in docs/BILD-FORMAT.md.
        const val MAX_ZOOM = 14

        fun oeffne(pfad: File, versatz0: Long = 0L): Bilddatei {
            val datei = RandomAccessFile(pfad, "r")
            try {
                val kopf = ByteArray(KOPF_BYTES)
                datei.seek(versatz0)
                datei.readFully(kopf)
                for (i in KENNUNG.indices) {
                    if (kopf[i] != KENNUNG[i]) throw IOException("keine .czb-Datei")
                }
                val fassung = kopf[8].toInt() and 0xFF
                if (fassung != FASSUNG) throw IOException("Bildfassung $fassung wird nicht gelesen")
                val kante = 1 shl (kopf[9].toInt() and 0xFF)
                val zoomKleinste = kopf[10].toInt() and 0xFF
                val zoomGroesste = kopf[11].toInt() and 0xFF
                if (zoomKleinste > zoomGroesste || zoomGroesste > MAX_ZOOM) {
                    throw IOException("Zoombereich $zoomKleinste bis $zoomGroesste ist unmoeglich")
                }
                val west = leseInt(kopf, 12) / 1e7
                val sued = leseInt(kopf, 16) / 1e7
                val ost = leseInt(kopf, 20) / 1e7
                val nord = leseInt(kopf, 24) / 1e7
                val anzahl = leseInt(kopf, 28)
                if (anzahl < 0 || anzahl > MAX_KACHELN) {
                    throw IOException("Kachelzahl $anzahl ist unmoeglich")
                }
                val anfang = leseLong(kopf, 32)
                val von = leseInt(kopf, 40)
                val bis = leseInt(kopf, 44)

                val verzeichnis = ByteArray(EINTRAG_BYTES * anzahl)
                datei.readFully(verzeichnis)
                val schluessel = LongArray(anzahl)
                val versatz = LongArray(anzahl)
                val laenge = IntArray(anzahl)
                var letzter = Long.MIN_VALUE
                for (i in 0 until anzahl) {
                    val p = i * EINTRAG_BYTES
                    val z = verzeichnis[p].toInt() and 0xFF
                    val x = leseInt(verzeichnis, p + 1)
                    val y = leseInt(verzeichnis, p + 5)
                    val s = (z.toLong() shl 56) or (x.toLong() shl 28) or y.toLong()
                    // DIE ORDNUNG WIRD GEPRUEFT, nicht angenommen: Die Suche
                    // ist binaer, und auf einem unsortierten Verzeichnis
                    // findet sie nicht etwa nichts, sondern die falsche
                    // Kachel -- ein Bild an der falschen Stelle im Gelaende.
                    if (s <= letzter) throw IOException("Kachelverzeichnis ist nicht sortiert")
                    letzter = s
                    schluessel[i] = s
                    versatz[i] = leseLong(verzeichnis, p + 9)
                    laenge[i] = leseInt(verzeichnis, p + 17)
                    if (versatz[i] < anfang) throw IOException("Kachel liegt vor dem Inhalt")
                }
                return Bilddatei(
                    datei, kante, zoomKleinste, zoomGroesste, von, bis,
                    west, sued, ost, nord, schluessel, versatz, laenge, versatz0,
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

        private fun leseLong(b: ByteArray, p: Int): Long {
            var wert = 0L
            for (i in 7 downTo 0) wert = (wert shl 8) or (b[p + i].toLong() and 0xFF)
            return wert
        }
    }
}
