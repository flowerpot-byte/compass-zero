package org.compasszero.android

import java.io.File

/**
 * Die hoechste je angenommene Fassung, je Paket-Kennung.
 *
 * WOZU: Ein Paket, das einmal gueltig unterschrieben war, bleibt fuer immer
 * gueltig unterschrieben. Wird ein Ueberlebenshinweis spaeter berichtigt --
 * beim Tote-bergen-Tipp stand einmal zehn Meter Abstand zur Wasserstelle, belegt
 * sind 200 bis 350 --, dann verifiziert die alte Fassung mit dem falschen Wert
 * weiterhin einwandfrei. Wer sie auf ein Geraet bringt, ueberschreibt die
 * richtige Angabe mit der falschen.
 *
 * WARUM NICHT EINFACH DIE FASSUNG DES GELADENEN PAKETS VERGLEICHEN: Weil die
 * verschwindet. Faellt die uebernommene Datei weg -- geloescht, beschaedigt,
 * ueber einen kuenftigen Zuruecksetzen-Knopf --, laeuft die App wieder auf der
 * Beigabe, und deren Fassung ist typisch 1. Danach liesse sich jede Fassung
 * zwischen 1 und der zuletzt angenommenen erneut einspielen, ohne dass etwas
 * anschlaegt. Genau davor schuetzt die Marke: Sie liegt NEBEN den Paketen, nicht
 * bei ihnen, und sie steigt nur.
 *
 * TEXTFORM, eine Zeile je Kennung: `kennung fassung`. Absichtlich lesbar --
 * wer wissen will, was das Geraet denkt, soll die Datei aufmachen koennen und
 * keinen Parser brauchen.
 *
 * WAS SIE NICHT LEISTET: Sie ueberlebt das Loeschen des PAKETS, nicht das
 * Loeschen der APP -- mit dem App-Verzeichnis ist auch sie weg. Und eine
 * Android-Sicherung kann eine alte Marke zurueckbringen. Das ist der einzige
 * Weg, auf dem die Marke sinken kann, und er steht hier, statt verschwiegen zu
 * werden.
 */
object Paketmarken {

    const val DATEINAME = "paketmarken.txt"

    /** Was beim Lesen der Markendatei herauskam. */
    sealed interface Stand {
        /** Gelesen. Fehlt die Datei, ist die Liste leer -- das ist kein Fehler. */
        class Gelesen(val marken: Map<String, Int>) : Stand

        /**
         * Die Datei ist da, aber nicht zu verstehen.
         *
         * Dann wird NICHT weitergemacht, als gaebe es keine Marke: Eine
         * unlesbare Sicherheitsmarke ist ein Grund innezuhalten. Sie wird auch
         * nicht ueberschrieben -- wer nachsehen will, soll sie noch vorfinden.
         */
        class Unlesbar(val grund: String) : Stand
    }

    fun lies(ordner: File): Stand {
        val datei = File(ordner, DATEINAME)
        if (!datei.exists()) return Stand.Gelesen(emptyMap())
        val roh = try {
            datei.readText()
        } catch (fehler: Exception) {
            return Stand.Unlesbar("Die Markendatei ließ sich nicht lesen.")
        }
        val marken = HashMap<String, Int>()
        for ((nummer, zeile) in roh.lines().withIndex()) {
            val text = zeile.trim()
            if (text.isEmpty()) continue
            val trenner = text.lastIndexOf(' ')
            if (trenner <= 0) {
                return Stand.Unlesbar("Zeile ${nummer + 1} hat kein Feld für die Fassung.")
            }
            val kennung = text.substring(0, trenner).trim()
            val fassung = text.substring(trenner + 1).trim().toIntOrNull()
            if (kennung.isEmpty() || fassung == null || fassung < 0) {
                return Stand.Unlesbar("Zeile ${nummer + 1} ist keine gültige Marke: \"$text\"")
            }
            // Steht eine Kennung doppelt drin, gilt die hoehere. Sinken darf
            // die Marke auch durch eine kaputte Datei nicht.
            val bisher = marken[kennung]
            marken[kennung] = if (bisher == null || fassung > bisher) fassung else bisher
        }
        return Stand.Gelesen(marken)
    }

    /** Die hoechste je angenommene Fassung, oder null, wenn es keine gibt. */
    fun hoechste(ordner: File, kennung: String): Int? =
        (lies(ordner) as? Stand.Gelesen)?.marken?.get(kennung)

    /**
     * Hebt die Marke -- niemals senkt sie.
     *
     * Wird erst aufgerufen, wenn ein Paket geprueft UND abgelegt ist. Andersherum
     * waere die Marke eine Behauptung ueber etwas, das nicht stattgefunden hat.
     *
     * Ist die Datei unlesbar, wird sie NICHT ueberschrieben: Der Aufrufer haette
     * an dieser Stelle ohnehin schon abgelehnt.
     */
    fun hebe(ordner: File, kennung: String, fassung: Int): Boolean {
        val stand = lies(ordner) as? Stand.Gelesen ?: return false
        val bisher = stand.marken[kennung]
        if (bisher != null && bisher >= fassung) return false
        val neu = LinkedHashMap(stand.marken)
        neu[kennung] = fassung
        schreibe(ordner, neu)
        return true
    }

    private fun schreibe(ordner: File, marken: Map<String, Int>) {
        val ziel = File(ordner, DATEINAME)
        val teil = File(ordner, "$DATEINAME.teil")
        val text = marken.entries
            .sortedBy { it.key }
            .joinToString("\n") { "${it.key} ${it.value}" } + "\n"
        teil.writeText(text)
        if (ziel.exists() && !ziel.delete()) error("alte Markendatei ließ sich nicht ersetzen")
        if (!teil.renameTo(ziel)) error("Markendatei ließ sich nicht ablegen")
    }
}
