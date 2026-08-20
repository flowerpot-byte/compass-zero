package org.compasszero.karte

import java.io.File
import java.io.RandomAccessFile

/**
 * Eine geoeffnete Hoehendatei `.czh`.
 *
 * Aufgebaut wie die Kartendatei -- Kopf, Verzeichnis, gepackte Kacheln -- und
 * aus demselben Grund: Im Speicher liegt nur das Verzeichnis, gelesen wird
 * immer nur, was gerade auf dem Schirm steht.
 *
 * Warum ueberhaupt eine ZWEITE Datei und keine weitere Schicht in der Karte:
 * Die Hoehen stehen unter einer anderen Lizenz als die Kartendaten. In
 * getrennten Dateien steht jede unter genau einer, und keine erbt die
 * Auflagen der anderen. Einzelheiten in `docs/HOEHEN-FORMAT.md`.
 */
class Hoehendatei private constructor(
    private val datei: RandomAccessFile,
    val kante: Int,
    val zoomKleinste: Int,
    val zoomGroesste: Int,
    private val schluessel: LongArray,
    private val versatz: LongArray,
    private val laenge: IntArray,
    /**
     * Wo der Inhalt beginnt -- 0 ohne Umschlag, sonst hinter ihm.
     *
     * Die Versaetze im Verzeichnis zaehlen ab dem INHALT und nicht ab dem
     * Dateianfang. Wer das vergisst, liest bei einer signierten Datei um die
     * Laenge des Umschlags verschoben: Die Kacheln sind dann kein Gelaende
     * mehr, und die Meldung dazu waere "Kachel unlesbar" statt "falsch
     * gerechnet".
     */
    private val versatz0: Long,
) : AutoCloseable {

    val kachelzahl: Int get() = schluessel.size

    /**
     * Liest ein Hoehenraster.
     *
     * Gibt es fuer die verlangte Zoomstufe keines, wird eine groebere Stufe
     * genommen -- eine Schummerung aus groeberen Hoehen ist weicher, aber
     * richtig; gar keine sieht dagegen aus wie flaches Land.
     *
     * **Das Ergebnis deckt dann eine groessere Flaeche ab als verlangt.** Die
     * zurueckgegebene Kachel traegt ihre EIGENE Zoomstufe und Nummer; wer sie
     * benutzt, muss daraus den Ausschnitt rechnen. Das ist Absicht: Eine
     * stillschweigend passend zugeschnittene Kachel waere hier nicht
     * nachpruefbar, und ein Schummerungsbild, das um eine halbe Kachel
     * verrutscht ist, sieht auf den ersten Blick richtig aus.
     */
    fun kachel(zoom: Int, x: Int, y: Int): Hoehenkachel? {
        var z = minOf(zoom, zoomGroesste)
        // MIT DER STUFE MUESSEN DIE KACHELNUMMERN MIT. Am 04.08.2026 stand
        // hier nur die gesenkte Stufe, und die Nummern blieben die der feinen:
        // gesucht wurde dann Kachel 2200/1430 auf Stufe 10, wo es nur bis 1023
        // geht. Ergebnis war keine Fehlermeldung, sondern gar keine
        // Schummerung -- und das sieht aus wie flaches Land.
        var tx = x shr (zoom - z).coerceAtLeast(0)
        var ty = y shr (zoom - z).coerceAtLeast(0)
        while (z > zoomKleinste - 1) {
            if (z < zoomKleinste) return null
            val stelle = suche(schluesselVon(z, tx, ty))
            if (stelle >= 0) {
                val n = laenge[stelle]
                val roh = ByteArray(n)
                synchronized(datei) {
                    datei.seek(versatz0 + versatz[stelle])
                    datei.readFully(roh)
                }
                val entpackt = Kartendatei.entpacke(roh, "hoehe $z/$tx/$ty")
                return Hoehenleser.lies(entpackt, z, tx, ty, kante)
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
     * Die App braucht das nicht -- sie fragt nach der Stelle, die sie
     * schummern will. Das Werkzeug, das die Datei unterschreibt, braucht es:
     * Es liest jede Kachel einmal, statt dem Verzeichnis zu glauben.
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

        fun oeffne(pfad: File, versatz0: Long = 0L): Hoehendatei {
            if (!pfad.isFile) throw Kartenfehler("Hoehendatei fehlt: $pfad")
            val datei = RandomAccessFile(pfad, "r")
            try {
                val kopf = ByteArray(Hoehenformat.KOPF_BYTES)
                datei.seek(versatz0)
                datei.readFully(kopf)
                for (i in Hoehenformat.KENNUNG.indices) {
                    if (kopf[i] != Hoehenformat.KENNUNG[i]) {
                        throw Kartenfehler("$pfad ist keine Hoehendatei")
                    }
                }
                val fassung = kopf[8].toInt() and 0xFF
                if (fassung != Hoehenformat.FASSUNG) {
                    throw Kartenfehler("Hoehenfassung $fassung ist unbekannt")
                }
                val kantenBits = kopf[9].toInt() and 0xFF
                if (kantenBits < 1 || kantenBits > 9) {
                    throw Kartenfehler("Kachelkante 2^$kantenBits ist unbrauchbar")
                }
                val kante = 1 shl kantenBits
                val zoomKleinste = kopf[10].toInt() and 0xFF
                val zoomGroesste = kopf[11].toInt() and 0xFF
                if (zoomKleinste > zoomGroesste || zoomGroesste > 22) {
                    throw Kartenfehler("Zoombereich $zoomKleinste..$zoomGroesste ist unsinnig")
                }
                val zahl = leseInt(kopf, 28)
                val anfang = leseLong(kopf, 32)
                val erwartet = Hoehenformat.KOPF_BYTES.toLong() +
                    Hoehenformat.EINTRAG_BYTES.toLong() * zahl
                // Die Grenzen werden im INHALT gerechnet, nicht in der Datei:
                // Bei einer signierten Datei liegt vor dem Inhalt noch der
                // Umschlag, und ein Verzeichnis, das sich daran misst, liesse
                // um dessen Laenge zu viel durch.
                val inhaltsende = datei.length() - versatz0
                if (zahl < 0 || anfang != erwartet || anfang > inhaltsende) {
                    throw Kartenfehler("Hoehenverzeichnis passt nicht zur Datei")
                }

                val roh = ByteArray(Hoehenformat.EINTRAG_BYTES * zahl)
                datei.readFully(roh)
                val schluessel = LongArray(zahl)
                val versatz = LongArray(zahl)
                val laenge = IntArray(zahl)
                val ende = inhaltsende
                for (i in 0 until zahl) {
                    val p = i * Hoehenformat.EINTRAG_BYTES
                    val z = roh[p].toInt() and 0xFF
                    val x = leseInt(roh, p + 1)
                    val y = leseInt(roh, p + 5)
                    val v = leseLong(roh, p + 9)
                    val n = leseInt(roh, p + 17)
                    if (z !in zoomKleinste..zoomGroesste ||
                        x < 0 || y < 0 || x >= (1 shl z) || y >= (1 shl z) ||
                        n <= 0 || n > Kartenformat.KACHEL_MAX_BYTES ||
                        v < anfang || v + n > ende
                    ) {
                        throw Kartenfehler("Hoehen-Verzeichniseintrag $i ist unbrauchbar")
                    }
                    schluessel[i] = schluesselVon(z, x, y)
                    if (i > 0 && schluessel[i] <= schluessel[i - 1]) {
                        throw Kartenfehler("Hoehenverzeichnis ist nicht sortiert")
                    }
                    versatz[i] = v
                    laenge[i] = n
                }
                return Hoehendatei(datei, kante, zoomKleinste, zoomGroesste,
                                   schluessel, versatz, laenge, versatz0)
            } catch (fehler: Throwable) {
                datei.close()
                throw fehler
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
