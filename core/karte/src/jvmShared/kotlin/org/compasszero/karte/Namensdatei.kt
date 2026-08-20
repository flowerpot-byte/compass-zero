package org.compasszero.karte

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/** Ein gefundener Name samt Stelle. */
class Namenstreffer(
    val name: String,
    val laenge: Double,
    val breite: Double,
    /** Sorte aus dem Kartenformat: ORT oder PUNKT. */
    val sorte: Int,
    /** Punktart aus dem Kartenformat; bei einem Ort 0. */
    val art: Int,
) {
    /**
     * Wie die Art vor einem Menschen heisst.
     *
     * Die Namen im Dateiformat sind Kennungen und keine Beschriftung --
     * "huette", "grossstadt", "notruftelefon". In einer Trefferliste
     * nebeneinander gestellt sehen sie aus wie ein Fehler, und "hoehle"
     * neben "Ort" liest sich, als haette die App die Haelfte vergessen.
     */
    val artName: String
        get() = if (sorte == Kartenformat.ORT) {
            "Ort"
        } else {
            when (Kartenformat.punktartName(art)) {
                "quelle" -> "Quelle"
                "brunnen" -> "Brunnen"
                "trinkwasser" -> "Trinkwasser"
                "wasserturm" -> "Wasserturm"
                "gipfel" -> "Gipfel"
                "sattel" -> "Sattel"
                "pass" -> "Pass"
                "hoehle" -> "Höhle"
                "huette" -> "Hütte"
                "unterstand" -> "Unterstand"
                "aussicht" -> "Aussicht"
                "krankenhaus" -> "Krankenhaus"
                "apotheke" -> "Apotheke"
                "notruftelefon" -> "Notruftelefon"
                "grossstadt" -> "Großstadt"
                "stadt" -> "Stadt"
                "dorf" -> "Dorf"
                "weiler" -> "Weiler"
                "einzellage" -> "Einzellage"
                else -> "Punkt"
            }
        }
}

/**
 * Das Namensverzeichnis `.czn` -- Orte, Gipfel, Quellen, Huetten,
 * Krankenhaeuser. Aufbau in `docs/NAMEN-FORMAT.md`.
 *
 * WARUM ES DIESE DATEI GIBT: Die Namen stehen laengst in der Karte, aber je
 * Kachel verstreut. Eine Suche muesste dafuer die ganze Datei durchpfluegen --
 * bei einer Detailkarte 770 MB. Hier liegen sie einmal, nach Suchschluessel
 * sortiert, und eine Suche kostet ein gutes Dutzend Sprünge.
 *
 * DER SUCHSCHLUESSEL STEHT MIT IN DER DATEI und wird hier NICHT gebildet.
 * Wuerde ihn das Werkzeug beim Bauen und der Leser beim Suchen jeweils selbst
 * berechnen, muessten zwei Umsetzungen -- eine in Python, eine hier -- fuer
 * alle Zeiten dasselbe tun. Laufen sie auseinander, findet die Suche einzelne
 * Namen nicht mehr, und niemand merkt, welche.
 */
class Namensdatei private constructor(
    private val datei: RandomAccessFile,
    val anzahl: Int,
    val west: Double,
    val sued: Double,
    val ost: Double,
    val nord: Double,
    private val versatz: IntArray,
    /** Wo der Inhalt beginnt -- 0 ohne Umschlag, sonst hinter ihm. */
    private val versatz0: Long,
) : AutoCloseable {

    /**
     * Sucht alle Namen, die mit [anfrage] beginnen.
     *
     * NUR DER ANFANG, nicht irgendwo im Wort: Wer "burg" tippt, meint
     * Burghausen und nicht Salzburg -- und eine Suche, die beides liefert,
     * schuettet die Liste zu. Die Faltung macht dabei aus "St. Veit" und
     * "st veit" dieselbe Anfrage.
     */
    fun suche(anfrage: String, hoechstens: Int = 40): List<Namenstreffer> {
        val schluessel = falte(anfrage)
        if (schluessel.isEmpty()) return emptyList()
        val gesucht = schluessel.toByteArray(Charsets.UTF_8)

        // Die erste Stelle finden, deren Schluessel nicht kleiner ist.
        var von = 0
        var bis = anzahl
        while (von < bis) {
            val mitte = (von + bis) ushr 1
            if (vergleicheSchluessel(schluesselVon(mitte), gesucht) < 0) von = mitte + 1 else bis = mitte
        }

        val aus = ArrayList<Namenstreffer>()
        var i = von
        while (i < anzahl && aus.size < hoechstens) {
            val hier = schluesselVon(i)
            if (!beginntMit(hier, gesucht)) break
            aus.add(lies(i))
            i++
        }
        return aus
    }

    /**
     * Der Eintrag an einer Stelle, 0 bis [anzahl] minus 1.
     *
     * Fuer das Werkzeug, das die Datei unterschreibt: Es geht einmal durch
     * alle Eintraege, statt der Datei zu glauben.
     */
    fun eintrag(nummer: Int): Namenstreffer {
        if (nummer < 0 || nummer >= anzahl) throw IOException("Eintrag $nummer gibt es nicht")
        return lies(nummer)
    }

    /** Der Suchschluessel an einer Stelle, roh -- so, wie die Suche ihn vergleicht. */
    fun schluessel(nummer: Int): ByteArray {
        if (nummer < 0 || nummer >= anzahl) throw IOException("Eintrag $nummer gibt es nicht")
        return schluesselVon(nummer)
    }

    override fun close() = datei.close()

    private fun schluesselVon(nummer: Int): ByteArray {
        synchronized(datei) {
            datei.seek(versatz0 + versatz[nummer].toLong())
            val laenge = liesVarint()
            if (laenge <= 0 || laenge > MAX_SCHLUESSEL) {
                throw IOException("Schluessellaenge $laenge ist unmoeglich")
            }
            val roh = ByteArray(laenge)
            datei.readFully(roh)
            return roh
        }
    }

    private fun lies(nummer: Int): Namenstreffer {
        synchronized(datei) {
            datei.seek(versatz0 + versatz[nummer].toLong())
            val schluessellaenge = liesVarint()
            datei.skipBytes(schluessellaenge)
            val namenslaenge = liesVarint()
            if (namenslaenge <= 0 || namenslaenge > MAX_NAME) {
                throw IOException("Namenslaenge $namenslaenge ist unmoeglich")
            }
            val name = ByteArray(namenslaenge)
            datei.readFully(name)
            val rest = ByteArray(10)
            datei.readFully(rest)
            return Namenstreffer(
                name = String(name, Charsets.UTF_8),
                laenge = leseInt(rest, 0) / 1e7,
                breite = leseInt(rest, 4) / 1e7,
                sorte = rest[8].toInt() and 0xFF,
                art = rest[9].toInt() and 0xFF,
            )
        }
    }

    private fun liesVarint(): Int {
        var wert = 0
        var schub = 0
        while (true) {
            val b = datei.read()
            if (b < 0) throw IOException("Datei endet mitten in einer Zahl")
            wert = wert or ((b and 0x7F) shl schub)
            if (b and 0x80 == 0) return wert
            schub += 7
            if (schub > 28) throw IOException("Zahl ist zu lang")
        }
    }

    companion object {

        val KENNUNG = byteArrayOf(0x43, 0x5A, 0x4E, 0x41, 0x4D, 0x45, 0x30, 0x31) // "CZNAME01"
        const val FASSUNG = 1
        const val KOPF_BYTES = 32
        const val MAX_EINTRAEGE = 5_000_000
        const val MAX_SCHLUESSEL = 240
        const val MAX_NAME = 240

        /**
         * Faltet eine Anfrage genauso, wie `tools/karte/namen_bauen.py` die
         * Schluessel faltet. NUR FUER DIE ANFRAGE -- die Schluessel in der
         * Datei werden nie neu gefaltet, sondern gelesen.
         */
        fun falte(text: String): String {
            val ersatz = mapOf(
                'ä' to "ae", 'ö' to "oe", 'ü' to "ue", 'ß' to "ss",
                'á' to "a", 'à' to "a", 'â' to "a", 'å' to "a", 'ã' to "a",
                'é' to "e", 'è' to "e", 'ê' to "e", 'ë' to "e",
                'í' to "i", 'ì' to "i", 'î' to "i", 'ï' to "i",
                'ó' to "o", 'ò' to "o", 'ô' to "o", 'õ' to "o", 'ø' to "o",
                'ú' to "u", 'ù' to "u", 'û' to "u",
                'ç' to "c", 'ñ' to "n", 'š' to "s", 'ž' to "z", 'č' to "c", 'ř' to "r",
            )
            val aus = StringBuilder()
            for (zeichen in text.lowercase()) {
                val fertig = ersatz[zeichen]
                if (fertig != null) {
                    aus.append(fertig)
                    continue
                }
                val blank = java.text.Normalizer.normalize(zeichen.toString(), java.text.Normalizer.Form.NFD)
                    .filter { !it.isISOControl() && Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
                if (blank.isNotEmpty() && blank.all { it.isLetterOrDigit() }) aus.append(blank) else aus.append(' ')
            }
            return aus.toString().split(" ").filter { it.isNotEmpty() }.joinToString(" ")
        }

        fun oeffne(pfad: File, versatz0: Long = 0L): Namensdatei {
            val datei = RandomAccessFile(pfad, "r")
            try {
                val kopf = ByteArray(KOPF_BYTES)
                datei.seek(versatz0)
                datei.readFully(kopf)
                for (i in KENNUNG.indices) {
                    if (kopf[i] != KENNUNG[i]) throw IOException("keine .czn-Datei")
                }
                val fassung = kopf[8].toInt() and 0xFF
                if (fassung != FASSUNG) throw IOException("Namensfassung $fassung wird nicht gelesen")
                val anzahl = leseInt(kopf, 12)
                if (anzahl < 0 || anzahl > MAX_EINTRAEGE) {
                    throw IOException("Anzahl $anzahl ist unmoeglich")
                }
                val roh = ByteArray(4 * anzahl)
                datei.readFully(roh)
                val versatz = IntArray(anzahl)
                var letzter = -1
                for (i in 0 until anzahl) {
                    versatz[i] = leseInt(roh, i * 4)
                    // Aufsteigend geprueft, nicht angenommen: Die Suche ist
                    // binaer und faende auf einer verdrehten Tabelle nicht
                    // etwa nichts, sondern den falschen Ort.
                    if (versatz[i] <= letzter) throw IOException("Versatztabelle ist nicht sortiert")
                    letzter = versatz[i]
                }
                return Namensdatei(
                    datei, anzahl,
                    leseInt(kopf, 16) / 1e7, leseInt(kopf, 20) / 1e7,
                    leseInt(kopf, 24) / 1e7, leseInt(kopf, 28) / 1e7,
                    versatz, versatz0,
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

        /**
         * Bytevergleich ohne Vorzeichen -- so sortiert auch das Werkzeug.
         *
         * Oeffentlich, damit packsign vor dem Unterschreiben mit GENAU diesem
         * Vergleich nachsehen kann, ob die Datei sortiert ist. Ein zweiter,
         * nachgebauter Vergleich waere die Fehlerquelle, die er finden soll.
         */
        fun vergleicheSchluessel(a: ByteArray, b: ByteArray): Int {
            val bis = minOf(a.size, b.size)
            for (i in 0 until bis) {
                val links = a[i].toInt() and 0xFF
                val rechts = b[i].toInt() and 0xFF
                if (links != rechts) return links - rechts
            }
            return a.size - b.size
        }

        private fun beginntMit(hier: ByteArray, anfang: ByteArray): Boolean {
            if (hier.size < anfang.size) return false
            for (i in anfang.indices) if (hier[i] != anfang[i]) return false
            return true
        }
    }
}
