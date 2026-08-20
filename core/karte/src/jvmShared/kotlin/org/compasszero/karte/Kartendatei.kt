package org.compasszero.karte

import java.io.File
import java.io.RandomAccessFile
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Eine geoeffnete Kartendatei.
 *
 * Im Speicher liegt nur das Kachelverzeichnis, nie die Kacheln selbst. Eine
 * Uebersichtskarte von Europa ist rund 300 MB gross -- die passt auf keinem
 * Telefon in den Arbeitsspeicher, und sie muss es auch nicht: Gebraucht wird
 * immer nur, was gerade auf dem Schirm steht.
 */
class Kartendatei private constructor(
    private val datei: RandomAccessFile,
    /**
     * Wo die Karte in der Datei anfaengt.
     *
     * Null bei einer blanken `.czk`. Bei einer unterschriebenen Karte steht
     * davor der Umschlag, und alle Versaetze im Kachelverzeichnis zaehlen ab
     * dem Anfang der KARTE, nicht ab dem Anfang der DATEI -- sonst haenge das
     * Verzeichnis daran, ob eine Unterschrift davorsteht.
     */
    private val basis: Long,
    val zoomKleinste: Int,
    val zoomGroesste: Int,
    /** Grenzen der Karte in Zehnmillionstel Grad. */
    val westen: Int,
    val sueden: Int,
    val osten: Int,
    val norden: Int,
    private val schluessel: LongArray,
    private val versatz: LongArray,
    private val laenge: IntArray,
) : AutoCloseable {

    val kachelzahl: Int get() = schluessel.size

    /** Gibt die gepackten Bytes einer Kachel zurueck, oder null. */
    fun rohkachel(zoom: Int, x: Int, y: Int): ByteArray? {
        val stelle = suche(schluesselVon(zoom, x, y))
        if (stelle < 0) return null
        val n = laenge[stelle]
        if (n <= 0 || n > Kartenformat.KACHEL_MAX_BYTES) {
            throw Kartenfehler("Kachel $zoom/$x/$y meldet $n Bytes")
        }
        val roh = ByteArray(n)
        synchronized(datei) {
            datei.seek(basis + versatz[stelle])
            datei.readFully(roh)
        }
        return roh
    }

    /** Liest, entpackt und dekodiert eine Kachel. */
    fun kachel(zoom: Int, x: Int, y: Int): Kachel? {
        val roh = rohkachel(zoom, x, y) ?: return null
        return Kachelleser.lies(entpacke(roh, "$zoom/$x/$y"), zoom, x, y)
    }

    /** Alle vorhandenen Kacheln einer Zoomstufe, als (x, y). */
    fun kachelliste(zoom: Int): List<Pair<Int, Int>> {
        val aus = ArrayList<Pair<Int, Int>>()
        for (i in schluessel.indices) {
            if ((schluessel[i] ushr 56).toInt() == zoom) {
                aus.add(Pair(((schluessel[i] ushr 28) and 0xFFFFFFF).toInt(),
                             (schluessel[i] and 0xFFFFFFF).toInt()))
            }
        }
        return aus
    }

    override fun close() = datei.close()

    private fun suche(gesucht: Long): Int {
        var tief = 0
        var hoch = schluessel.size - 1
        while (tief <= hoch) {
            val mitte = (tief + hoch) ushr 1
            val wert = schluessel[mitte]
            when {
                wert < gesucht -> tief = mitte + 1
                wert > gesucht -> hoch = mitte - 1
                else -> return mitte
            }
        }
        return -1
    }

    companion object {

        private fun schluesselVon(zoom: Int, x: Int, y: Int): Long =
            (zoom.toLong() shl 56) or (x.toLong() shl 28) or y.toLong()

        /**
         * Oeffnet eine Karte.
         *
         * [basis] ist der Versatz, an dem die Karte in der Datei beginnt, und
         * [nutzlast] ihre Laenge. Beides ist null bzw. die Dateilaenge, wenn
         * die Datei nur die Karte enthaelt; bei einer unterschriebenen Karte
         * kommen die Werte aus dem Umschlag und sind selbst signaturgedeckt.
         */
        fun oeffne(pfad: File, basis: Long = 0L, nutzlast: Long = -1L): Kartendatei {
            if (!pfad.isFile) throw Kartenfehler("Kartendatei fehlt: $pfad")
            val datei = RandomAccessFile(pfad, "r")
            try {
                val kopf = ByteArray(Kartenformat.KOPF_BYTES)
                datei.seek(basis)
                datei.readFully(kopf)
                for (i in Kartenformat.KENNUNG.indices) {
                    if (kopf[i] != Kartenformat.KENNUNG[i]) {
                        throw Kartenfehler("$pfad ist keine Kartendatei")
                    }
                }
                val fassung = kopf[8].toInt() and 0xFF
                if (fassung != Kartenformat.FASSUNG) {
                    throw Kartenfehler("Kartenfassung $fassung ist unbekannt")
                }
                val rasterBits = kopf[9].toInt() and 0xFF
                if ((1 shl rasterBits) != Kartenformat.RASTER) {
                    throw Kartenfehler("Kachelraster 2^$rasterBits passt nicht")
                }
                val zoomKleinste = kopf[10].toInt() and 0xFF
                val zoomGroesste = kopf[11].toInt() and 0xFF
                if (zoomKleinste > zoomGroesste || zoomGroesste > 22) {
                    throw Kartenfehler("Zoombereich $zoomKleinste..$zoomGroesste ist unsinnig")
                }
                val westen = leseInt(kopf, 12)
                val sueden = leseInt(kopf, 16)
                val osten = leseInt(kopf, 20)
                val norden = leseInt(kopf, 24)
                val zahl = leseInt(kopf, 28)
                val anfang = leseLong(kopf, 32)

                // Das Ende der KARTE, nicht das der Datei: Hinter einer
                // unterschriebenen Karte darf nichts mehr stehen, und ein
                // Verzeichniseintrag darf nicht in einen Anhang hinter der
                // signaturgedeckten Nutzlast zeigen.
                val kartenende = if (nutzlast >= 0) nutzlast else datei.length() - basis
                if (kartenende <= 0 || basis + kartenende > datei.length()) {
                    throw Kartenfehler("Die angegebene Kartenlaenge passt nicht zur Datei")
                }

                val erwartet = Kartenformat.KOPF_BYTES.toLong() +
                    Kartenformat.EINTRAG_BYTES.toLong() * zahl
                if (zahl < 0 || anfang != erwartet || anfang > kartenende) {
                    throw Kartenfehler("Kachelverzeichnis passt nicht zur Datei")
                }

                val roh = ByteArray(Kartenformat.EINTRAG_BYTES * zahl)
                datei.readFully(roh)
                val schluessel = LongArray(zahl)
                val versatz = LongArray(zahl)
                val laenge = IntArray(zahl)
                val ende = kartenende
                for (i in 0 until zahl) {
                    val p = i * Kartenformat.EINTRAG_BYTES
                    val z = roh[p].toInt() and 0xFF
                    val x = leseInt(roh, p + 1)
                    val y = leseInt(roh, p + 5)
                    val v = leseLong(roh, p + 9)
                    val n = leseInt(roh, p + 17)
                    // Ein Eintrag, der aus der Datei hinauszeigt, ist der
                    // billigste Weg, einen Leser zum Absturz zu bringen.
                    if (z !in zoomKleinste..zoomGroesste ||
                        x < 0 || y < 0 || x >= (1 shl z) || y >= (1 shl z) ||
                        n <= 0 || n > Kartenformat.KACHEL_MAX_BYTES ||
                        v < anfang || v + n > ende
                    ) {
                        throw Kartenfehler("Verzeichniseintrag $i ist unbrauchbar")
                    }
                    schluessel[i] = schluesselVon(z, x, y)
                    if (i > 0 && schluessel[i] <= schluessel[i - 1]) {
                        throw Kartenfehler("Kachelverzeichnis ist nicht sortiert")
                    }
                    versatz[i] = v
                    laenge[i] = n
                }
                return Kartendatei(datei, basis, zoomKleinste, zoomGroesste,
                                   westen, sueden, osten, norden,
                                   schluessel, versatz, laenge)
            } catch (fehler: Throwable) {
                datei.close()
                throw fehler
            }
        }

        /**
         * Entpackt eine Kachel mit fester Obergrenze.
         *
         * Ohne Grenze ist eine kleine Datei, die sich zu Gigabytes aufblaeht,
         * eine Zeile Arbeit fuer den Angreifer und ein Absturz fuer das
         * Geraet.
         */
        fun entpacke(roh: ByteArray, wo: String): ByteArray {
            val entpacker = Inflater()
            try {
                entpacker.setInput(roh)
                var aus = ByteArray(minOf(roh.size.toLong() * 4 + 1024,
                                          Kartenformat.KACHEL_MAX_BYTES.toLong()).toInt())
                var gefuellt = 0
                while (!entpacker.finished()) {
                    if (gefuellt == aus.size) {
                        if (aus.size >= Kartenformat.KACHEL_MAX_BYTES) {
                            throw Kartenfehler("Kachel $wo ist entpackt zu gross")
                        }
                        aus = aus.copyOf(minOf(aus.size.toLong() * 2,
                                               Kartenformat.KACHEL_MAX_BYTES.toLong()).toInt())
                    }
                    val n = try {
                        entpacker.inflate(aus, gefuellt, aus.size - gefuellt)
                    } catch (fehler: DataFormatException) {
                        throw Kartenfehler("Kachel $wo ist beschaedigt")
                    }
                    if (n == 0) {
                        if (entpacker.needsInput() || entpacker.needsDictionary()) {
                            throw Kartenfehler("Kachel $wo bricht mitten ab")
                        }
                    }
                    gefuellt += n
                }
                return aus.copyOf(gefuellt)
            } finally {
                entpacker.end()
            }
        }

        private fun leseInt(roh: ByteArray, p: Int): Int =
            (roh[p].toInt() and 0xFF) or
                ((roh[p + 1].toInt() and 0xFF) shl 8) or
                ((roh[p + 2].toInt() and 0xFF) shl 16) or
                ((roh[p + 3].toInt() and 0xFF) shl 24)

        private fun leseLong(roh: ByteArray, p: Int): Long {
            var wert = 0L
            for (i in 7 downTo 0) {
                wert = (wert shl 8) or (roh[p + i].toLong() and 0xFF)
            }
            return wert
        }
    }
}
