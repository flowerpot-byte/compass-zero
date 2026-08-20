package org.compasszero.android

/**
 * Laender als Sprungziel fuer die Suche.
 *
 * WARUM ES DIESE LISTE ALS QUELLTEXT GIBT und nicht im Namensverzeichnis:
 * In den Kartendaten stehen keine Laendernamen. Eine Staatsgrenze ist dort
 * eine Linie und traegt keine Beschriftung -- OpenStreetMap fuehrt Laender als
 * Beziehungen, und die kommen beim Bau der Karte gar nicht erst mit. Ohne
 * diese Tabelle findet die Suche "Frankreich" also nie, egal wie gut das
 * Verzeichnis ist.
 *
 * GESPRUNGEN WIRD AUF DIE HAUPTSTADT, und die App sagt das auch dazu
 * ("Österreich — Wien"). Der Mittelpunkt eines Landes waere die andere
 * Moeglichkeit und die schlechtere: Er liegt bei Norwegen im Gebirge, bei
 * Griechenland im Meer, und er ist eine Zahl, die niemand nachpruefen kann.
 * Eine Hauptstadt ist eine Stelle, die es wirklich gibt.
 *
 * WAS DIESE LISTE NICHT IST: eine Aussage darueber, welche Laender es gibt
 * oder wem ein Gebiet gehoert. Sie ist ein Verzeichnis von Sprungzielen fuer
 * eine Karte, mehr nicht.
 */
object Laender {

    class Land(val name: String, val hauptstadt: String, val breite: Double, val laenge: Double)

    /**
     * Die Sprungziele. Europa und seine Nachbarn -- so weit, wie die Karten
     * dieses Projekts reichen.
     *
     * Zusaetzliche Schreibweisen stehen als weitere Zeilen mit derselben
     * Stelle: "Deutschland" und "Germany", "Oesterreich" und "Austria". Wer
     * eine Karte auf einem fremden Geraet bedient, tippt die Fassung, die er
     * kennt.
     */
    private val LISTE = listOf(
        Land("Deutschland", "Berlin", 52.520, 13.405),
        Land("Germany", "Berlin", 52.520, 13.405),
        Land("Österreich", "Wien", 48.208, 16.373),
        Land("Austria", "Wien", 48.208, 16.373),
        Land("Schweiz", "Bern", 46.948, 7.447),
        Land("Switzerland", "Bern", 46.948, 7.447),
        Land("Liechtenstein", "Vaduz", 47.141, 9.521),
        Land("Frankreich", "Paris", 48.857, 2.352),
        Land("France", "Paris", 48.857, 2.352),
        Land("Italien", "Rom", 41.903, 12.496),
        Land("Italia", "Rom", 41.903, 12.496),
        Land("Spanien", "Madrid", 40.417, -3.704),
        Land("España", "Madrid", 40.417, -3.704),
        Land("Portugal", "Lissabon", 38.722, -9.139),
        Land("Andorra", "Andorra la Vella", 42.507, 1.521),
        Land("Monaco", "Monaco", 43.738, 7.424),
        Land("San Marino", "San Marino", 43.936, 12.447),
        Land("Belgien", "Brüssel", 50.851, 4.352),
        Land("Belgium", "Brüssel", 50.851, 4.352),
        Land("Niederlande", "Amsterdam", 52.373, 4.892),
        Land("Nederland", "Amsterdam", 52.373, 4.892),
        Land("Luxemburg", "Luxemburg", 49.611, 6.131),
        Land("Dänemark", "Kopenhagen", 55.676, 12.568),
        Land("Denmark", "Kopenhagen", 55.676, 12.568),
        Land("Norwegen", "Oslo", 59.913, 10.752),
        Land("Norge", "Oslo", 59.913, 10.752),
        Land("Schweden", "Stockholm", 59.329, 18.069),
        Land("Sverige", "Stockholm", 59.329, 18.069),
        Land("Finnland", "Helsinki", 60.170, 24.938),
        Land("Suomi", "Helsinki", 60.170, 24.938),
        Land("Island", "Reykjavík", 64.147, -21.942),
        Land("Iceland", "Reykjavík", 64.147, -21.942),
        Land("Irland", "Dublin", 53.350, -6.260),
        Land("Ireland", "Dublin", 53.350, -6.260),
        Land("Großbritannien", "London", 51.507, -0.128),
        Land("Vereinigtes Königreich", "London", 51.507, -0.128),
        Land("United Kingdom", "London", 51.507, -0.128),
        Land("Polen", "Warschau", 52.230, 21.011),
        Land("Polska", "Warschau", 52.230, 21.011),
        Land("Tschechien", "Prag", 50.088, 14.420),
        Land("Česko", "Prag", 50.088, 14.420),
        Land("Slowakei", "Bratislava", 48.146, 17.107),
        Land("Ungarn", "Budapest", 47.498, 19.040),
        Land("Slowenien", "Ljubljana", 46.056, 14.508),
        Land("Kroatien", "Zagreb", 45.815, 15.982),
        Land("Bosnien und Herzegowina", "Sarajevo", 43.857, 18.413),
        Land("Serbien", "Belgrad", 44.787, 20.449),
        Land("Montenegro", "Podgorica", 42.441, 19.263),
        Land("Nordmazedonien", "Skopje", 41.998, 21.426),
        Land("Albanien", "Tirana", 41.328, 19.818),
        Land("Griechenland", "Athen", 37.984, 23.728),
        Land("Bulgarien", "Sofia", 42.698, 23.322),
        Land("Rumänien", "Bukarest", 44.427, 26.103),
        Land("Moldau", "Chișinău", 47.011, 28.864),
        Land("Ukraine", "Kiew", 50.450, 30.523),
        Land("Belarus", "Minsk", 53.902, 27.562),
        Land("Estland", "Tallinn", 59.437, 24.754),
        Land("Lettland", "Riga", 56.949, 24.105),
        Land("Litauen", "Vilnius", 54.687, 25.280),
        Land("Malta", "Valletta", 35.899, 14.514),
        Land("Zypern", "Nikosia", 35.186, 33.383),
        Land("Türkei", "Ankara", 39.933, 32.860),
        Land("Russland", "Moskau", 55.756, 37.617),
    )

    private val NACH_SCHLUESSEL: Map<String, Land> =
        LISTE.associateBy { org.compasszero.karte.Namensdatei.falte(it.name) }

    /**
     * Sucht ein Land -- NUR bei vollem Namen.
     *
     * Kein Anfangsvergleich: "s" wuerde sonst die Schweiz liefern, "d"
     * Deutschland, und ein Sprung ueber tausend Kilometer waere die Antwort
     * auf einen halb getippten Ortsnamen. Wer ein Land meint, schreibt es
     * aus.
     */
    fun suche(anfrage: String): Land? =
        NACH_SCHLUESSEL[org.compasszero.karte.Namensdatei.falte(anfrage)]

    /** Alle Eintraege -- fuer Pruefungen und Uebersichten. */
    fun alle(): List<Land> = LISTE
}
