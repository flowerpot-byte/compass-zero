package org.compasszero.karte

/**
 * Ein Höhenraster für eine Kachel.
 *
 * [kante] Stützstellen je Seite, [meter] die Höhen zeilenweise von Nord nach
 * Süd. Der Aufbau der Datei steht in `docs/HOEHEN-FORMAT.md`.
 */
class Hoehenkachel(
    val zoom: Int,
    val kachelX: Int,
    val kachelY: Int,
    val kante: Int,
    val meter: ShortArray,
) {
    fun hoehe(spalte: Int, zeile: Int): Int = meter[zeile * kante + spalte].toInt()

    val kleinste: Int get() = meter.min().toInt()
    val groesste: Int get() = meter.max().toInt()
}

object Hoehenformat {

    val KENNUNG = byteArrayOf(
        'C'.code.toByte(), 'Z'.code.toByte(), 'H'.code.toByte(), 'O'.code.toByte(),
        'E'.code.toByte(), 'H'.code.toByte(), 'E'.code.toByte(), '1'.code.toByte(),
    )

    const val FASSUNG = 1
    const val KOPF_BYTES = 40
    const val EINTRAG_BYTES = 21

    /** Grösste Kachelkante, die angenommen wird: 512 × 512 Stützstellen. */
    const val KANTE_MAX = 512

    /**
     * Die tiefste und die höchste Stelle, die vorkommen dürfen.
     *
     * Das Tote Meer liegt bei −430 m, der Mount Everest bei 8849 m. Werte
     * ausserhalb sind kein Gelände, sondern ein Datenfehler oder eine
     * gefälschte Datei -- und eine Schummerung, die aus einer erfundenen
     * Spannweite gerechnet wird, macht aus flachem Land eine Wand.
     */
    const val METER_MIN = -500
    const val METER_MAX = 9000

    val SCHRITTWEITEN = intArrayOf(1, 2, 5, 10)
}

object Hoehenleser {

    fun lies(roh: ByteArray, zoom: Int, kachelX: Int, kachelY: Int, kante: Int): Hoehenkachel {
        if (kante <= 0 || kante > Hoehenformat.KANTE_MAX) {
            throw Kartenfehler("Kachelkante $kante ist unbrauchbar")
        }
        val leser = Bytesleser(roh)
        val fassung = leser.byteWert()
        if (fassung != Hoehenformat.FASSUNG) {
            throw Kartenfehler("Höhenkachelaufbau $fassung ist unbekannt")
        }
        val grund = leser.int16()
        if (grund < Hoehenformat.METER_MIN || grund > Hoehenformat.METER_MAX) {
            throw Kartenfehler("Grundhöhe $grund liegt ausserhalb des Erdreliefs")
        }
        val schritt = leser.byteWert()
        if (schritt !in Hoehenformat.SCHRITTWEITEN.toList()) {
            throw Kartenfehler("Schrittweite $schritt ist nicht vorgesehen")
        }

        val meter = ShortArray(kante * kante)
        var zeilenanfang = grund
        for (zeile in 0 until kante) {
            // Der erste Wert einer Zeile zaehlt gegen denselben Punkt der
            // Zeile darueber, nicht gegen das Ende der vorigen Zeile: Am
            // Zeilenumbruch springt das Gelaende sonst quer ueber die Kachel.
            var wert = zeilenanfang + leser.zigzag() * schritt
            pruefe(wert)
            meter[zeile * kante] = wert.toShort()
            zeilenanfang = wert
            for (spalte in 1 until kante) {
                wert += leser.zigzag() * schritt
                pruefe(wert)
                meter[zeile * kante + spalte] = wert.toShort()
            }
        }
        if (!leser.amEnde()) {
            throw Kartenfehler("hinter der Höhenkachel stehen ${leser.rest()} unerklaerte Bytes")
        }
        return Hoehenkachel(zoom, kachelX, kachelY, kante, meter)
    }

    private fun pruefe(wert: Int) {
        if (wert < Hoehenformat.METER_MIN || wert > Hoehenformat.METER_MAX) {
            throw Kartenfehler("Höhe $wert liegt ausserhalb des Erdreliefs")
        }
    }
}
