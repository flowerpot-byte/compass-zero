package org.compasszero.android

import java.io.File

/**
 * Sinnbilder, die ein selbst gesetzter Punkt tragen darf.
 *
 * EINE FESTE LISTE UND KEIN FREIER TEXT: Der Wert steht in einer Datei, die
 * als einzige im Programm nicht signiert ist. Was von dort kommt, wird gegen
 * diese Liste geprueft; alles Unbekannte wird zu KEINS. So kann eine
 * beschaedigte oder untergeschobene Datei hoechstens ein Sinnbild verlieren.
 */
object Sinnbild {
    const val KEINS = ""
    const val WASSER = "wasser"
    const val UNTERKUNFT = "unterkunft"
    const val NAHRUNG = "nahrung"
    const val GEFAHR = "gefahr"
    const val TREFFPUNKT = "treffpunkt"
    const val VORRAT = "vorrat"
    const val UEBERGANG = "uebergang"

    /** Reihenfolge der Auswahl in der Oberflaeche. */
    val ALLE = listOf(KEINS, WASSER, UNTERKUNFT, NAHRUNG, GEFAHR, TREFFPUNKT, VORRAT, UEBERGANG)

    val NAMEN = mapOf(
        KEINS to "ohne",
        WASSER to "Wasser",
        UNTERKUNFT to "Unterkunft",
        NAHRUNG to "Nahrung",
        GEFAHR to "Gefahr",
        TREFFPUNKT to "Treffpunkt",
        VORRAT to "Vorrat",
        UEBERGANG to "Übergang",
    )

    fun gueltig(wert: String): String = if (wert in ALLE) wert else KEINS
}

/**
 * Ein selbst gesetzter Punkt auf der Karte.
 *
 * Er darf seit dem 17.08.2026 ein Sinnbild tragen -- Max wollte die Wegpunkte
 * uebersichtlicher haben. Was dabei NICHT aufgegeben wird, ist die Trennung,
 * um die es der urspruenglichen Fassung ging: Die Punkte aus der Kartendatei
 * sind geprueft und signiert, diese hier sind eine Behauptung des Nutzers.
 * Sie duerfen einander nie aehnlich sehen -- wer im Ernstfall eine Quelle
 * sucht, muss auf einen Blick erkennen, woher der Punkt kommt.
 *
 * Deshalb liegt der Unterschied jetzt nicht mehr im Vorhandensein eines
 * Sinnbilds, sondern in der FORM: Ein eigener Punkt steht immer in einem
 * Ring in der Signalfarbe, den ein Kartenpunkt nie bekommt; das Sinnbild
 * steckt darin. Wer das aendert, hebt die Trennung auf.
 */
data class Eigenpunkt(
    val nummer: Long,
    val laenge: Double,
    val breite: Double,
    val name: String,
    val sinnbild: String = Sinnbild.KEINS,
)

/**
 * Ein selbst gezeichneter Weg: eine Kette von Stuetzstellen.
 *
 * Ein Weg mit weniger als zwei Stellen ist keine Linie. Er wird trotzdem
 * zugelassen und gespeichert -- wer beim Zeichnen unterbrochen wird, soll den
 * Anfang wiederfinden und weiterzeichnen koennen, statt bei null anzufangen.
 */
data class Eigenweg(
    val nummer: Long,
    val name: String,
    val stellen: List<Pair<Double, Double>>,
)

/**
 * Alles, was der Nutzer selbst auf die Karte gelegt hat.
 *
 * WARUM EIN EIGENES FORMAT UND KEIN JSON: Diese Datei ist die einzige im
 * ganzen Programm, die NICHT signiert ist und trotzdem gelesen werden muss --
 * sie entsteht ja erst auf dem Geraet. Sie muss zwei Dinge koennen, die ein
 * verschachteltes Format schlecht kann:
 *
 * 1. ZEILENWEISE UEBERLEBEN. Ist eine Zeile unbrauchbar, gehen genau dieser
 *    eine Punkt verloren und sonst nichts. Bei JSON macht ein einziges
 *    falsches Zeichen die GANZE Datei unlesbar, und damit waeren alle
 *    Wegpunkte weg -- in einer Lage, in der man sie braucht.
 * 2. VON HAND ZU REPARIEREN SEIN. Wer die Datei mit einem Texteditor oeffnet,
 *    sieht Zahlen und Namen und kann eine kaputte Zeile loeschen.
 *
 * Aufbau, Felder durch Tabulator getrennt:
 *
 *     P  <nummer>  <laenge>  <breite>  <name>
 *     W  <nummer>  <name>    <laenge,breite> <laenge,breite> ...
 */
class Eigenkarte(
    punkte: List<Eigenpunkt> = emptyList(),
    wege: List<Eigenweg> = emptyList(),
) {

    private val eigenePunkte = punkte.toMutableList()
    private val eigeneWege = wege.toMutableList()

    val punkte: List<Eigenpunkt> get() = eigenePunkte
    val wege: List<Eigenweg> get() = eigeneWege

    val leer: Boolean get() = eigenePunkte.isEmpty() && eigeneWege.isEmpty()

    /**
     * Die naechste freie Nummer.
     *
     * Sie zaehlt von der hoechsten vergebenen aufwaerts und NICHT von der
     * Anzahl: Wer den zweiten von drei Punkten loescht und dann einen neuen
     * setzt, bekaeme sonst die Nummer eines noch vorhandenen.
     */
    private fun naechsteNummer(): Long {
        val hoechste = maxOf(
            eigenePunkte.maxOfOrNull { it.nummer } ?: 0L,
            eigeneWege.maxOfOrNull { it.nummer } ?: 0L,
        )
        return hoechste + 1
    }

    fun setzePunkt(
        laenge: Double,
        breite: Double,
        name: String,
        sinnbild: String = Sinnbild.KEINS,
    ): Eigenpunkt {
        val punkt = Eigenpunkt(
            naechsteNummer(), laenge, breite, saubereZeile(name), Sinnbild.gueltig(sinnbild),
        )
        eigenePunkte.add(punkt)
        return punkt
    }

    /**
     * Die Punkte in der Reihenfolge, in der sie in einer Liste stehen sollen.
     *
     * Nach Sinnbild gruppiert und nicht nach der Reihenfolge des Setzens: Wer
     * im Gelaende Wasser sucht, will die Wasserstellen beieinander haben.
     * Innerhalb einer Gruppe nach Namen, damit dieselbe Liste zweimal gleich
     * aussieht. Punkte OHNE Sinnbild stehen hinten -- sie sagen am wenigsten.
     *
     * Steht hier und nicht in der Oberflaeche, damit die Regel pruefbar ist.
     */
    fun punkteGeordnet(): List<Eigenpunkt> = eigenePunkte.sortedWith(
        compareBy(
            { if (it.sinnbild == Sinnbild.KEINS) Int.MAX_VALUE else Sinnbild.ALLE.indexOf(it.sinnbild) },
            { it.name.lowercase() },
        ),
    )

    /** Setzt das Sinnbild eines Punktes. Unbekanntes wird zu KEINS. */
    fun setzeSinnbild(nummer: Long, sinnbild: String): Boolean {
        val stelle = eigenePunkte.indexOfFirst { it.nummer == nummer }
        if (stelle < 0) return false
        eigenePunkte[stelle] = eigenePunkte[stelle].copy(sinnbild = Sinnbild.gueltig(sinnbild))
        return true
    }

    fun legeWegAn(name: String, stellen: List<Pair<Double, Double>>): Eigenweg {
        val weg = Eigenweg(naechsteNummer(), saubereZeile(name), stellen.toList())
        eigeneWege.add(weg)
        return weg
    }

    fun loeschePunkt(nummer: Long): Boolean = eigenePunkte.removeAll { it.nummer == nummer }

    fun loescheWeg(nummer: Long): Boolean = eigeneWege.removeAll { it.nummer == nummer }

    fun benennePunkt(nummer: Long, name: String): Boolean {
        val stelle = eigenePunkte.indexOfFirst { it.nummer == nummer }
        if (stelle < 0) return false
        eigenePunkte[stelle] = eigenePunkte[stelle].copy(name = saubereZeile(name))
        return true
    }

    fun benenneWeg(nummer: Long, name: String): Boolean {
        val stelle = eigeneWege.indexOfFirst { it.nummer == nummer }
        if (stelle < 0) return false
        eigeneWege[stelle] = eigeneWege[stelle].copy(name = saubereZeile(name))
        return true
    }

    fun alsText(): String {
        val bau = StringBuilder()
        for (p in eigenePunkte) {
            // Das Sinnbild steht HINTER dem Namen und nicht davor. Beides zusammen
            // haelt die Datei in beide Richtungen lesbar: Eine aeltere Fassung des
            // Programms nimmt Feld 4 als Namen und uebergeht den Rest, eine neuere
            // liest bei einer alten Datei einfach kein Sinnbild.
            bau.append("P\t").append(p.nummer).append('\t')
                .append(zahl(p.laenge)).append('\t').append(zahl(p.breite)).append('\t')
                .append(p.name)
            if (p.sinnbild != Sinnbild.KEINS) bau.append('\t').append(p.sinnbild)
            bau.append('\n')
        }
        for (w in eigeneWege) {
            bau.append("W\t").append(w.nummer).append('\t').append(w.name).append('\t')
            bau.append(w.stellen.joinToString(" ") { "${zahl(it.first)},${zahl(it.second)}" })
            bau.append('\n')
        }
        return bau.toString()
    }

    companion object {

        /**
         * Sieben Nachkommastellen sind rund ein Zentimeter. Mehr waere gelogene
         * Genauigkeit, weniger waere bei einem von Hand gesetzten Punkt schon
         * sichtbar daneben.
         */
        private fun zahl(wert: Double): String = String.format(java.util.Locale.ROOT, "%.7f", wert)

        /**
         * Tabulator und Zeilenumbruch sind die Trennzeichen des Formats. Kaemen
         * sie im Namen vor, zerlegte sich die Zeile beim naechsten Lesen falsch.
         * Sie werden deshalb beim Setzen ersetzt und nicht erst beim Schreiben:
         * Was gespeichert wird, soll dem entsprechen, was angezeigt wurde.
         */
        private fun saubereZeile(text: String): String =
            text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()

        /**
         * Liest, was lesbar ist.
         *
         * Eine kaputte Zeile wird uebersprungen, nicht gemeldet. Der Nutzer
         * kann daraufhin ohnehin nichts tun, und alles andere ist heil. Die
         * Alternative -- gar nichts laden -- waere der schlechtere Ausgang.
         */
        fun ausText(text: String): Eigenkarte {
            val punkte = mutableListOf<Eigenpunkt>()
            val wege = mutableListOf<Eigenweg>()
            for (zeile in text.lineSequence()) {
                if (zeile.isBlank()) continue
                val feld = zeile.split('\t')
                when (feld[0]) {
                    "P" -> {
                        if (feld.size < 5) continue
                        val nummer = feld[1].toLongOrNull() ?: continue
                        val laenge = feld[2].toDoubleOrNull() ?: continue
                        val breite = feld[3].toDoubleOrNull() ?: continue
                        if (!gueltig(laenge, breite)) continue
                        val sinnbild = if (feld.size >= 6) Sinnbild.gueltig(feld[5]) else Sinnbild.KEINS
                        punkte.add(Eigenpunkt(nummer, laenge, breite, feld[4], sinnbild))
                    }
                    "W" -> {
                        if (feld.size < 4) continue
                        val nummer = feld[1].toLongOrNull() ?: continue
                        val stellen = feld[3].split(' ')
                            .filter { it.isNotBlank() }
                            .mapNotNull { stueck ->
                                val teile = stueck.split(',')
                                if (teile.size != 2) return@mapNotNull null
                                val laenge = teile[0].toDoubleOrNull() ?: return@mapNotNull null
                                val breite = teile[1].toDoubleOrNull() ?: return@mapNotNull null
                                if (!gueltig(laenge, breite)) return@mapNotNull null
                                laenge to breite
                            }
                        wege.add(Eigenweg(nummer, feld[2], stellen))
                    }
                }
            }
            return Eigenkarte(punkte, wege)
        }

        /**
         * Eine Stelle ausserhalb der Erde ist keine Stelle. Solche Werte
         * entstehen nicht durch Bedienung, sondern nur durch eine beschaedigte
         * Datei -- gezeichnet wuerden sie irgendwo im Nichts.
         */
        private fun gueltig(laenge: Double, breite: Double): Boolean =
            laenge.isFinite() && breite.isFinite() &&
                laenge >= -180.0 && laenge <= 180.0 &&
                breite >= -90.0 && breite <= 90.0
    }
}

/**
 * Legt die eigenen Punkte und Wege im App-Ordner ab.
 *
 * Geschrieben wird ueber eine Nebendatei, die anschliessend umbenannt wird --
 * dasselbe Vorgehen wie beim Auslegen des Inhaltspakets. Ein Absturz mitten im
 * Schreiben laesst damit die vorige, vollstaendige Fassung stehen, statt eine
 * halbe zurueckzulassen.
 */
class Eigenkartendatei(private val ordner: File) {

    private val datei = File(ordner, DATEINAME)
    private val neben = File(ordner, "$DATEINAME.teil")

    fun lade(): Eigenkarte {
        if (!datei.isFile) return Eigenkarte()
        return try {
            Eigenkarte.ausText(datei.readText())
        } catch (fehler: java.io.IOException) {
            // Lieber mit leerer Karte weiterlaufen als gar nicht starten: Die
            // Karte selbst und das ganze Lexikon haengen nicht hieran.
            Eigenkarte()
        }
    }

    fun sichere(karte: Eigenkarte) {
        neben.writeText(karte.alsText())
        if (datei.exists() && !datei.delete()) error("alte eigene Punkte ließen sich nicht ersetzen")
        if (!neben.renameTo(datei)) error("eigene Punkte ließen sich nicht ablegen")
    }

    companion object {
        const val DATEINAME = "eigene-punkte.txt"
    }
}
