package org.compasszero.android

import android.content.Context
import java.io.File
import org.compasszero.karte.GeoeffneteKarte
import org.compasszero.karte.Bilddatei
import org.compasszero.karte.Namensdatei
import org.compasszero.karte.Wegenetz
import org.compasszero.karte.Zusatzumschlag
import org.compasszero.security.PackFormat
import org.compasszero.karte.Hoehendatei
import org.compasszero.karte.Hoehenformat
import org.compasszero.karte.Kartenumschlag
import org.compasszero.security.PackVerdict
import org.compasszero.security.TrustStore
import org.compasszero.security.TrustedKey

/**
 * Sucht und oeffnet die Kartendatei.
 *
 * Zwei Wege, und der Unterschied zwischen ihnen ist kein Beiwerk, sondern eine
 * Sicherheitsaussage:
 *
 * 1. **Im Programm enthalten.** Die Karte liegt als Beigabe im APK. Damit
 *    steht sie unter der Unterschrift des APK selbst -- wer die Datei
 *    austauschen will, muesste das APK neu signieren, und das faellt beim
 *    Vergleich der Pruefsumme auf. Diese Karte gilt als geprueft.
 *
 * 2. **Danebengelegt.** Der Nutzer legt eine `.czk` in den App-Ordner auf dem
 *    Geraet. Dafuer braucht es keine Berechtigung, und genau deshalb steht
 *    hinter dieser Datei niemand. Sie wird gelesen, aber dauerhaft als NICHT
 *    GEPRUEFT ausgewiesen -- so verlangt es Regel 5.
 *
 * Der zweite Weg ist die Vorbereitung fuer grosse Karten: Ein Europa-Ueberblick
 * ist rund 300 MB und gehoert nicht ins APK. Bevor solche Karten verteilt
 * werden, braucht die `.czk` einen eigenen signierten Umschlag wie die
 * Inhaltspakete. Bis dahin ist der Weg fuer eigene Karten offen und ehrlich
 * gekennzeichnet.
 */
object Kartenlader {

    private const val BEIGABE = "karte.czk"

    // Derselbe eingebaute Schluessel wie beim Inhaltspaket. Eine Karte, die
    // damit unterschrieben ist, gilt als geprueft -- egal, auf welchem Weg sie
    // ins Geraet gekommen ist.
    private const val SCHLUESSEL_ENTWICKLUNG =
        "d7e8b6cbe42a1a5ab18ab4f95f8d98acd7668973e0e7534f960c640925859e65"

    private const val HOEHEN_BEIGABE = "hoehen.czh"

    class GeladeneKarte(
        /** Alle geoeffneten Kartendateien, feinste zuerst. */
        val offen: List<GeoeffneteKarte>,
        val namen: String,
        val geprueft: Boolean,
        val quelle: String,
        /** Die Gelaendeform, wenn eine danebenliegt. */
        val hoehen: Hoehendatei?,
        /** Das Satellitenbild, wenn eines danebenliegt. */
        val bilder: Bilddatei?,
        /** Das Namensverzeichnis, wenn eines danebenliegt. */
        val verzeichnis: Namensdatei?,
        /** Das Wegenetz fuer die Routenberechnung, wenn eines danebenliegt. */
        val wege: Wegenetz?,
        /**
         * Was der Umschlag ueber die vier Zusatzdateien sagt -- null heisst
         * "ohne Unterschrift", und das ist erlaubt und wird angezeigt.
         */
        val hoehenUrteil: PackVerdict?,
        val bilderUrteil: PackVerdict?,
        val verzeichnisUrteil: PackVerdict?,
        val wegeUrteil: PackVerdict?,
    ) {
        val dateien get() = offen.map { it.datei }
        val zoomKleinste get() = dateien.minOf { it.zoomKleinste }
        val zoomGroesste get() = dateien.maxOf { it.zoomGroesste }
        val kachelzahl get() = dateien.sumOf { it.kachelzahl }
    }

    /**
     * Sucht die Hoehendatei.
     *
     * Sie fehlt zu duerfen ist Absicht: Eine Karte ohne Gelaendeform ist eine
     * flache Karte, keine kaputte. Wer nur Mitteleuropa braucht, muss die
     * Hoehen nicht mitschleppen.
     *
     * SEIT DEM 18.08.2026 TRAEGT SIE EINE UNTERSCHRIFT. Bis dahin nicht, und
     * die Begruendung dafuer war eng gefasst: nur Zahlen, und eine gefaelschte
     * Schummerung fuehrt niemanden in die Irre, weil ihr niemand folgt. Nur
     * ist die Schummerung nicht alles, was aus diesen Zahlen wird -- die
     * Hoehenangabe unter dem Finger kommt aus derselben Datei, und danach
     * entscheidet jemand, ob er ueber einen Sattel geht oder aussenherum.
     * Max hat es deshalb umgedreht. Was daran haengt: `docs/HOEHEN-FORMAT.md`.
     */
    private fun hoehenLaden(context: Context): Pair<Hoehendatei, PackVerdict?>? {
        val eigene = eigenerOrdner(context)
            ?.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".czh", ignoreCase = true) }
            ?.minByOrNull { it.name.lowercase() }
        val quelle = eigene ?: hoehenBeigabe(context) ?: return null
        return zusatzOeffnen(quelle, PackFormat.HOEHEN_MAGIC, Hoehenformat.KENNUNG) { f, v ->
            Hoehendatei.oeffne(f, v)
        }
    }

    /**
     * Sucht die Satellitenbild-Datei.
     *
     * Sie fehlt zu duerfen ist der Normalfall: Bilder sind ein eigenes Paket,
     * das man laedt, wenn man es will. Ohne sie zeichnet die Karte wie bisher.
     *
     * KEINE BEIGABE IM APK, anders als bei Karte und Hoehen. Ein Bildpaket
     * deckt immer nur eine Gegend ab; es in die App zu legen hiesse, jedem
     * die Gegend eines anderen mitzugeben.
     */
    /**
     * Oeffnet eine Zusatzdatei und prueft dabei ihren Umschlag.
     *
     * KAPUTTE UNTERSCHRIFT HEISST: GAR NICHT OEFFNEN. Zusatzumschlag wirft
     * dann, und der Fang unten gibt null zurueck -- die App laeuft ohne diese
     * Datei weiter. Eine Datei ohne Umschlag kommt dagegen durch, mit
     * urteil = null; von Max am 18.08.2026 so entschieden, damit jeder seine
     * selbst gebauten Dateien benutzen kann.
     */
    private fun <T> zusatzOeffnen(
        datei: File,
        umschlagKennung: ByteArray,
        blankeKennung: ByteArray,
        oeffne: (File, Long) -> T,
    ): Pair<T, PackVerdict?>? = try {
        val befund = Zusatzumschlag.pruefe(datei, umschlagKennung, blankeKennung, vertrauensspeicher())
        oeffne(datei, befund.versatz) to befund.urteil
    } catch (fehler: Exception) {
        null
    }

    private fun bilderLaden(context: Context): Pair<Bilddatei, PackVerdict?>? {
        val eigene = eigenerOrdner(context)
            ?.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".czb", ignoreCase = true) }
            ?.minByOrNull { it.name.lowercase() }
            ?: return null
        // Eine kaputte Bilddatei darf die Karte nicht anhalten. Ohne
        // Untergrund ist sie karger, aber richtig.
        return zusatzOeffnen(eigene, PackFormat.BILD_MAGIC, Bilddatei.KENNUNG) { f, v ->
            Bilddatei.oeffne(f, v)
        }
    }

    /**
     * Sucht das Namensverzeichnis.
     *
     * Es fehlen zu duerfen ist der Normalfall: Die Suche nach Koordinaten
     * geht auch ohne, und wer nur die Karte anschauen will, braucht es nie.
     * Wer es haben will, baut es sich mit `tools/karte/namen_bauen.py` aus
     * seiner eigenen Karte.
     */
    private fun namenLaden(context: Context): Pair<Namensdatei, PackVerdict?>? {
        val eigene = eigenerOrdner(context)
            ?.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".czn", ignoreCase = true) }
            ?.minByOrNull { it.name.lowercase() }
            ?: return null
        return zusatzOeffnen(eigene, PackFormat.NAME_MAGIC, Namensdatei.KENNUNG) { f, v ->
            Namensdatei.oeffne(f, v)
        }
    }

    /**
     * Sucht das Wegenetz.
     *
     * Es fehlen zu duerfen ist der Normalfall: Ein Netz deckt immer nur eine
     * Gegend ab, und die Karte funktioniert ohne. Wer es haben will, baut es
     * sich mit `tools/karte/wege_bauen.py` aus den Rohdaten seiner Gegend.
     */
    private fun wegeLaden(context: Context): Pair<Wegenetz, PackVerdict?>? {
        val eigene = eigenerOrdner(context)
            ?.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".czw", ignoreCase = true) }
            ?.minByOrNull { it.name.lowercase() }
            ?: return null
        return zusatzOeffnen(eigene, PackFormat.WEGE_MAGIC, Wegenetz.KENNUNG) { f, v ->
            Wegenetz.oeffne(f, v)
        }
    }

    private fun hoehenBeigabe(context: Context): File? {
        val vorhanden = context.assets.list("")?.contains(HOEHEN_BEIGABE) ?: false
        if (!vorhanden) return null
        val ziel = File(context.filesDir, HOEHEN_BEIGABE)
        val merker = File(context.filesDir, "$HOEHEN_BEIGABE.sha256")
        val summe = pruefsumme(context, HOEHEN_BEIGABE)
        if (ziel.exists() && merker.exists() && merker.readText() == summe) return ziel
        val temp = File(context.filesDir, "$HOEHEN_BEIGABE.teil")
        context.assets.open(HOEHEN_BEIGABE).use { quelle ->
            temp.outputStream().use { quelle.copyTo(it) }
        }
        if (ziel.exists() && !ziel.delete()) return null
        if (!temp.renameTo(ziel)) return null
        merker.writeText(summe)
        return ziel
    }

    /**
     * Oeffnet ALLE gefundenen Kartendateien, nicht nur die erste.
     *
     * Ein Ueberblickspaket deckt z4-z10, ein Detailpaket z11-z14 -- erst
     * zusammen ergeben sie eine Karte, in die man durchzoomen kann. Vorher nahm
     * diese Stelle die erstbeste Datei; damit hatte man entweder keine
     * Uebersicht oder keine Einzelheiten.
     */
    /**
     * Die einmal geoeffnete Karte, damit sie nicht bei jedem Wechsel auf den
     * Kartenreiter neu geprueft wird.
     *
     * WARUM DAS SEIN MUSS: Beim Oeffnen einer unterschriebenen Karte wird die
     * GANZE Datei durchgerechnet -- anders laesst sich eine Unterschrift nicht
     * pruefen. Fuer Europa und Oesterreich zusammen sind das 783 MB. Diese
     * Rechnung lief bisher bei JEDEM Aufbau der Kartenansicht, also bei jedem
     * Tippen auf den Reiter, und sie lief auf dem Faden der Bedienoberflaeche:
     * Die App stand so lange still. Max am 06.08.2026: "die karte braucht sehr
     * lange zum laden und laesst die app sehr langsam wirken."
     *
     * AN DER SICHERHEIT AENDERT DAS NICHTS. Die Unterschrift deckt den Zustand
     * der Datei beim Oeffnen ab; die Kacheln werden danach ohnehin nach und
     * nach aus derselben, offen gehaltenen Datei nachgelesen. Ob diese Pruefung
     * einmal je Programmstart oder einmal je Reiterwechsel geschieht, aendert
     * am Geltungsbereich nichts -- nur an der Wartezeit.
     *
     * Der Merkzettel traegt Name, Groesse und Zeitstempel jeder beteiligten
     * Datei. Wird eine Karte ausgetauscht oder eine neue danebengelegt, passt
     * er nicht mehr, und es wird neu geoeffnet und neu geprueft.
     */
    private var gemerkt: Result<GeladeneKarte>? = null
    private var gemerkterStand: String? = null

    private fun standDerDateien(context: Context): String {
        val teile = ArrayList<String>()
        for (datei in eigeneKarten(context)) {
            teile.add("${datei.name}:${datei.length()}:${datei.lastModified()}")
        }
        val hoehe = eigenerOrdner(context)?.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".czh", ignoreCase = true) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
        for (datei in hoehe) {
            teile.add("${datei.name}:${datei.length()}:${datei.lastModified()}")
        }
        return teile.joinToString("|")
    }

    /**
     * Die schon geoeffnete Karte -- oder nichts, wenn erst geprueft werden muss.
     *
     * Damit kann der Aufrufer entscheiden, ob er sofort zeichnen kann oder
     * einen Hinweis anzeigen und in den Hintergrund gehen muss. Die Pruefung,
     * ob der Merkzettel noch passt, kostet nur ein paar Dateiabfragen.
     */
    fun bereitsGeprueft(context: Context): Result<GeladeneKarte>? {
        val vorhanden = gemerkt ?: return null
        if (!vorhanden.isSuccess) return null
        return if (gemerkterStand == standDerDateien(context)) vorhanden else null
    }

    fun laden(context: Context): Result<GeladeneKarte> {
        val stand = standDerDateien(context)
        val vorhanden = gemerkt
        if (vorhanden != null && vorhanden.isSuccess && gemerkterStand == stand) {
            return vorhanden
        }
        val frisch = ladenUngepuffert(context)
        if (frisch.isSuccess) {
            gemerkt = frisch
            gemerkterStand = stand
        }
        return frisch
    }

    private fun ladenUngepuffert(context: Context): Result<GeladeneKarte> = runCatching {
        // Zeitmessung fuer die Fehlersuche: `adb logcat -s compasszero`.
        // Sie bleibt drin, weil das Oeffnen einer grossen Karte der einzige
        // Vorgang in dieser App ist, der spuerbar dauert -- wer ihn spaeter
        // wieder beschleunigen will, braucht diese Zahlen und nicht eine
        // Vermutung.
        var uhr = System.currentTimeMillis()
        fun miss(was: String) {
            val jetzt = System.currentTimeMillis()
            android.util.Log.i("compasszero", "laden: $was ${jetzt - uhr} ms")
            uhr = jetzt
        }

        val vertrauen = vertrauensspeicher()
        val hoehen = hoehenLaden(context)
        val bilder = bilderLaden(context)
        val verzeichnis = namenLaden(context)
        val wege = wegeLaden(context)
        miss("Hoehendatei")
        // Die Beigabe ist IMMER dabei. Wer nur ein Detailpaket danebenlegt,
        // verlöre sonst die Uebersicht und saehe beim Herauszoomen nichts mehr.
        val gefunden = ArrayList<Pair<File, String>>()
        gefunden.add(beigabeHolen(context) to "im Programm enthalten")
        miss("Beigabe herauslegen")
        for (datei in eigeneKarten(context)) gefunden.add(datei to "selbst hinzugefügt")

        val offen = ArrayList<GeoeffneteKarte>()
        // Merkt sich, welche der geoeffneten Karten die Beigabe aus dem APK
        // ist. Gebraucht wird das gleich beim Sortieren.
        val istBeigabe = ArrayList<Boolean>()
        val beschreibung = ArrayList<String>()
        var allesGeprueft = true
        for ((datei, woher) in gefunden) {
            val karte = Kartenumschlag.oeffne(datei, vertrauen)
            miss("oeffnen ${datei.name} (${datei.length() / 1_000_000} MB)")
            offen.add(karte)
            istBeigabe.add(woher == "im Programm enthalten")
            beschreibung.add("${datei.name} (${herkunft(karte, woher)})")
            // Die Beigabe im APK steht unter der Unterschrift des APK selbst;
            // eine eigene braucht sie nicht. Alles andere muss unterschrieben
            // und bekannt sein, sonst gilt die ganze Karte als ungeprueft.
            val inOrdnung = karte.geprueft || (woher == "im Programm enthalten" && !karte.unterschrieben)
            if (!inOrdnung) allesGeprueft = false
        }
        // Feinste zuerst: Ueberschneiden sich zwei Dateien, gewinnt die
        // genauere. Bei GLEICHER feinster Stufe gewinnt die selbst
        // hinzugefuegte, und die Beigabe kommt nach hinten.
        //
        // Der Grund steht in einem Fehler vom 05.08.2026: Die eingebaute
        // Oesterreich-Uebersicht und die neue Europa-Uebersicht decken beide
        // z4 bis z10. Die Beigabe stand vorn und gewann damit fuer jede
        // Kachel, die sie hat -- ein graues Rechteck ueber Oesterreich, in dem
        // von Europa nichts zu sehen war, obwohl die Datei geladen und
        // geprueft war. Wer eine eigene Karte danebenlegt, will sie sehen;
        // die Beigabe fuellt nur, wo sonst nichts ist.
        val reihenfolge = offen.indices.sortedWith(
            compareByDescending<Int> { offen[it].datei.zoomGroesste }
                .thenBy { if (istBeigabe[it]) 1 else 0 },
        )
        val sortiert = reihenfolge.map { offen[it] }
        offen.clear()
        offen.addAll(sortiert)

        GeladeneKarte(
            offen,
            offen.joinToString(" + ") { "${it.datei.kachelzahl} Kacheln" },
            geprueft = allesGeprueft,
            quelle = beschreibung.joinToString(" · "),
            hoehen = hoehen?.first,
            bilder = bilder?.first,
            verzeichnis = verzeichnis?.first,
            wege = wege?.first,
            hoehenUrteil = hoehen?.second,
            bilderUrteil = bilder?.second,
            verzeichnisUrteil = verzeichnis?.second,
            wegeUrteil = wege?.second,
        )
    }

    private fun herkunft(offen: GeoeffneteKarte, woher: String): String =
        when (val urteil = offen.urteil) {
            null -> "$woher, ohne eigene Unterschrift"
            is PackVerdict.Trusted -> "$woher · Signatur geprüft: ${urteil.signer.name}"
            is PackVerdict.UnknownSigner ->
                "$woher · Signierer UNBEKANNT (${urteil.fingerprint.take(16)})"
            else -> "$woher · Signatur nicht auswertbar"
        }

    /**
     * Der Vertrauensspeicher fuer Karten und Zusatzdateien -- EINE Stelle.
     *
     * Er stand bis zum 18.08.2026 zweimal im Quelltext, mit demselben
     * Schluessel. Ein Vertrauensspeicher, den es doppelt gibt, ist die Sorte
     * Doppelung, bei der eine spaetere Aenderung an einer Stelle vergessen
     * wird -- und dann traut die eine Haelfte des Programms jemandem, dem die
     * andere nicht mehr traut.
     */
    private fun vertrauensspeicher() = TrustStore(
        listOf(TrustedKey("entwicklung", hexZuBytes(SCHLUESSEL_ENTWICKLUNG))),
    )

    private fun hexZuBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "ungerade Hex-Länge" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /**
     * Prueft eine einzelne Kartendatei und beschreibt das Ergebnis in einem
     * Satz.
     *
     * Gebraucht wird das beim Einlesen: Wer eine 432-MB-Datei ins Geraet
     * holt, will nicht die Kartenansicht oeffnen muessen, um zu erfahren, ob
     * sie taugt. Der Schluessel steht hier und nicht beim Aufrufer -- er
     * gehoert an genau eine Stelle.
     */
    fun pruefeKarte(datei: File): String {
        val vertrauen = vertrauensspeicher()
        return try {
            val karte = Kartenumschlag.oeffne(datei, vertrauen)
            val stufen = "Zoom ${karte.datei.zoomKleinste}–${karte.datei.zoomGroesste}"
            val kacheln = "${karte.datei.kachelzahl} Kacheln"
            when (val urteil = karte.urteil) {
                null -> "Gelesen, aber OHNE Unterschrift · $stufen · $kacheln"
                is PackVerdict.Trusted -> "Signatur geprüft: ${urteil.signer.name} · $stufen · $kacheln"
                is PackVerdict.UnknownSigner ->
                    "Signierer UNBEKANNT (${urteil.fingerprint.take(16)}) · $stufen · $kacheln"
                else -> "Signatur nicht auswertbar · $stufen · $kacheln"
            }
        } catch (fehler: Exception) {
            "Nicht lesbar: ${fehler.message ?: fehler::class.simpleName}"
        }
    }

    /** Wohin der Nutzer eine eigene Karte legen kann. */
    fun eigenerOrdner(context: Context): File? = context.getExternalFilesDir(null)

    /** Alle .czk im App-Ordner, in fester Reihenfolge. */
    private fun eigeneKarten(context: Context): List<File> {
        val ordner = eigenerOrdner(context) ?: return emptyList()
        return ordner.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".czk", ignoreCase = true) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    // Die Karte wird ueber wahlfreien Zugriff gelesen -- immer nur die paar
    // Kacheln, die gerade auf dem Schirm stehen. Eine Beigabe im APK ist aber
    // kein Dateiverzeichniseintrag, sondern ein Eintrag im ZIP. Sie wird
    // deshalb einmal herausgelegt, genau wie das Inhaltspaket, und mit
    // derselben Vorsicht: Der Merker wird ERST nach dem Umbenennen
    // geschrieben, sonst behauptet er eine Datei, die es nicht gibt.
    private fun beigabeHolen(context: Context): File {
        val ziel = File(context.filesDir, BEIGABE)
        val merker = File(context.filesDir, "$BEIGABE.sha256")
        val summe = pruefsumme(context, BEIGABE)
        if (ziel.exists() && merker.exists() && merker.readText() == summe) return ziel

        val temp = File(context.filesDir, "$BEIGABE.teil")
        context.assets.open(BEIGABE).use { quelle ->
            temp.outputStream().use { quelle.copyTo(it) }
        }
        if (ziel.exists() && !ziel.delete()) error("alte Karte ließ sich nicht ersetzen")
        if (!temp.renameTo(ziel)) error("Karte ließ sich nicht ablegen")
        merker.writeText(summe)
        return ziel
    }

    private fun pruefsumme(context: Context, name: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        context.assets.open(name).use { quelle ->
            val puffer = ByteArray(64 * 1024)
            while (true) {
                val gelesen = quelle.read(puffer)
                if (gelesen <= 0) break
                digest.update(puffer, 0, gelesen)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
