package org.compasszero.android

import android.content.Context
import java.io.File
import org.compasszero.content.LoadedPack
import org.compasszero.content.PackReader
import org.compasszero.content.SearchIndex
import org.compasszero.security.OpenedPack
import org.compasszero.security.PackVerdict
import org.compasszero.security.TrustStore
import org.compasszero.security.TrustedKey

// Eingebauter oeffentlicher Schluessel. Nur Pakete mit dieser Unterschrift
// gelten als geprueft; ein fremd signiertes Paket wird zwar gelesen, aber die
// Oberflaeche muss es dauerhaft als ungeprueft kennzeichnen.
private const val SCHLUESSEL_ENTWICKLUNG =
    "d7e8b6cbe42a1a5ab18ab4f95f8d98acd7668973e0e7534f960c640925859e65"

private const val PAKETNAME = "europe-de.czp"

// Wohin ein uebernommenes Paket kommt. Bewusst NICHT ueber PAKETNAME: Die
// Beigabe bleibt liegen, damit es immer etwas gibt, worauf zurueckgefallen
// werden kann.
private const val UEBERNOMMEN = "uebernommen.czp"

class GeladenesPaket(
    val pack: LoadedPack,
    val index: SearchIndex,
    val verdict: PackVerdict,
    val datei: File,
    // Das geoeffnete Paket bleibt liegen, weil Bilder erst beim Anzeigen
    // gelesen werden. Nur ueber diesen Weg haengt an ihnen die Pruefsumme aus
    // dem signierten Durchlauf -- die Datei selbst aufzumachen waere derselbe
    // Inhalt ohne jeden Manipulationsschutz.
    val geoeffnet: OpenedPack,
    // Ob dieses Paket von einem anderen Geraet kam. Nur fuer die Anzeige --
    // die Vertrauensfrage beantwortet allein `verdict`, und die faellt fuer
    // ein empfangenes Paket nach denselben Regeln wie fuer die Beigabe.
    val vonAussen: Boolean = false,
)

/** Was aus einem empfangenen Paket geworden ist. */
sealed interface Uebernahme {
    class Angenommen(val name: String, val fassung: Int) : Uebernahme
    class Abgelehnt(val grund: String) : Uebernahme

    /**
     * Aelter als die hoechste je angenommene Fassung -- abgelehnt, aber mit
     * Notausgang.
     *
     * Der Fall, fuer den er da ist: Eine neue Fassung ist defekt, und die
     * einzige brauchbare, die noch greifbar ist, ist eine aeltere. Dann darf
     * sie benutzt werden -- aber nur nach einer ausdruecklichen Bestaetigung,
     * die benennt, was sie bedeutet. Die Marke sinkt dabei NICHT: Beim
     * naechsten Mal fragt die App wieder. Der Notausgang ist die Ausnahme fuer
     * diesen Moment, kein Umschalten in einen anderen Betriebszustand.
     */
    class Rueckstufung(val grund: String, val marke: Int, val angeboten: Int) : Uebernahme
}

object Paketlader {

    /**
     * Laedt das Inhaltspaket, das die App benutzen soll.
     *
     * [uebernommen] ist der Dateiname eines von einem anderen Geraet
     * empfangenen Pakets, oder leer. Es wird VORGEZOGEN -- aber nur, wenn es
     * sich hier, beim Laden, erneut als geprueft erweist. Die Pruefung beim
     * Annehmen zaehlt nicht als Freibrief: Zwischen Annehmen und Start kann
     * die Datei ausgetauscht worden sein, und diese Stelle ist die letzte vor
     * dem Anzeigen.
     *
     * Faellt das empfangene Paket durch, wird STILL auf die Beigabe
     * zurueckgefallen statt die App anzuhalten. Ein Handbuch, das sich wegen
     * einer kaputten Zusatzdatei nicht mehr oeffnen laesst, waere im Ernstfall
     * der schlimmere Fehler -- die Oberflaeche sagt danach, dass die Beigabe
     * laeuft.
     */
    fun laden(context: Context, uebernommen: String = ""): Result<GeladenesPaket> = runCatching {
        val beigabe = ausAssetsKopieren(context)
        if (uebernommen.isNotBlank()) {
            val eigenes = File(context.filesDir, uebernommen)
            if (eigenes.exists()) {
                val versuch = runCatching { oeffne(eigenes, vonAussen = true) }.getOrNull()
                if (versuch != null && istGeprueft(versuch.verdict)) {
                    merkeGeladenes(context.filesDir, versuch)
                    return@runCatching versuch
                }
            }
        }
        val gelaufen = oeffne(beigabe, vonAussen = false)
        merkeGeladenes(context.filesDir, gelaufen)
        gelaufen
    }

    // Auch die Beigabe setzt eine Marke. Ohne das haette ein Geraet, auf dem
    // noch nie ein Paket uebernommen wurde, gar keine -- und die Beigabe liesse
    // sich durch eine aeltere, gueltig unterschriebene Fassung ersetzen.
    // Nur GEPRUEFTE Pakete setzen sie; ein fremd signiertes darf die Marke
    // nicht anheben.
    private fun merkeGeladenes(ordner: File, geladen: GeladenesPaket) {
        if (!istGeprueft(geladen.verdict)) return
        merkeFassung(ordner, geladen.pack.manifest.version, geladen.pack.manifest.id)
    }

    private fun oeffne(datei: File, vonAussen: Boolean): GeladenesPaket {
        val gelesen = PackReader.read(datei, vertrauensspeicher())
        val ergebnis = gelesen.result ?: error(urteilstext(gelesen.verdict))
        val pack = ergebnis.pack
            ?: error("Paket beanstandet: " + ergebnis.problems.joinToString { it.code })
        val offen = gelesen.pack ?: error("Paket wurde gelesen, aber nicht geöffnet")
        return GeladenesPaket(
            pack, SearchIndex.build(pack), gelesen.verdict, datei, offen, vonAussen,
        )
    }

    private fun vertrauensspeicher() = TrustStore(
        listOf(TrustedKey("entwicklung", hexZuBytes(SCHLUESSEL_ENTWICKLUNG))),
    )

    /**
     * Prueft ein empfangenes Paket und legt es an seinen Platz -- oder nicht.
     *
     * VIER HUERDEN, und jede einzelne lehnt ab:
     *
     * 1. Die Unterschrift muss halten und von einem Schluessel stammen, den
     *    diese App kennt. Ein Paket ueber Funk ist kein besseres Paket.
     * 2. Es muss DASSELBE Paket sein -- gleiche Kennung. Sonst tauscht ein
     *    fremdes Handbuch das eigene aus.
     * 3. Es muss NEUER sein. Das ist der Rueckstufungs-Schutz: Ohne ihn
     *    genuegt es, jemandem eine alte, echt unterschriebene Fassung
     *    unterzuschieben, um eine Berichtigung rueckgaengig zu machen. Bei
     *    einem Handbuch, in dem Saetze berichtigt werden, weil sie falsch
     *    waren, ist das der gefaehrlichste Angriff ueberhaupt.
     * 4. Der Inhalt muss sich lesen lassen, sonst stuende beim naechsten Start
     *    ein Paket bereit, das die App nicht oeffnen kann.
     *
     * Erst danach wird die Datei umgelegt: zuerst ".teil", dann umbenannt.
     */
    fun uebernimmEmpfangenes(
        context: Context,
        quelle: File,
        jetziges: LoadedPack,
        rueckstufungBestaetigt: Boolean = false,
    ): Uebernahme = uebernimmEmpfangenes(
        context.filesDir, quelle, jetziges, rueckstufungBestaetigt,
    )

    /**
     * Dieselbe Uebernahme, aber auf einem Ordner statt auf einem Context.
     *
     * Nur deshalb getrennt: So laesst sich der ganze Vorgang samt Marke ohne
     * Geraet pruefen. Der Context traegt hier nichts bei ausser `filesDir`.
     */
    fun uebernimmEmpfangenes(
        ordner: File,
        quelle: File,
        jetziges: LoadedPack,
        rueckstufungBestaetigt: Boolean = false,
    ): Uebernahme {
        val gelesen = try {
            PackReader.read(quelle, vertrauensspeicher())
        } catch (fehler: Exception) {
            return Uebernahme.Abgelehnt("Die Datei ließ sich nicht lesen.")
        }
        val neu = if (istGeprueft(gelesen.verdict)) gelesen.result?.pack else null
        val entschieden = entscheide(
            ordner = ordner,
            verdict = gelesen.verdict,
            neueKennung = neu?.manifest?.id,
            neueFassung = neu?.manifest?.version,
            jetzige = jetziges,
            rueckstufungBestaetigt = rueckstufungBestaetigt,
        )
        if (entschieden !is Uebernahme.Angenommen) return entschieden

        val ziel = File(ordner, UEBERNOMMEN)
        val teil = File(ordner, "$UEBERNOMMEN.teil")
        return try {
            quelle.inputStream().use { ein -> teil.outputStream().use { ein.copyTo(it) } }
            if (ziel.exists() && !ziel.delete()) error("altes Paket ließ sich nicht ersetzen")
            if (!teil.renameTo(ziel)) error("Paket ließ sich nicht ablegen")
            quelle.delete()
            // ERST JETZT die Marke. Vorher waere sie eine Behauptung ueber ein
            // Paket, das gar nicht liegt. Und sie steigt nur -- eine bestaetigte
            // Rueckstufung laesst sie ausdruecklich oben.
            merkeFassung(ordner, entschieden.fassung, neu!!.manifest.id)
            entschieden
        } catch (fehler: Exception) {
            teil.delete()
            Uebernahme.Abgelehnt(fehler.message ?: "Das Paket ließ sich nicht ablegen.")
        }
    }

    /**
     * Die reine Entscheidung, ohne eine einzige Datei anzufassen.
     *
     * Getrennt, damit die gefaehrlichste Zeile pruefbar ist: dass ein Paket mit
     * kaputter Unterschrift die Marke nicht einmal beruehrt. Hier wird nichts
     * geschrieben -- wer diese Funktion aufruft, kann die Marke nicht verderben.
     */
    internal fun entscheide(
        ordner: File,
        verdict: PackVerdict,
        neueKennung: String?,
        neueFassung: Int?,
        jetzige: LoadedPack,
        rueckstufungBestaetigt: Boolean,
    ): Uebernahme {
        if (!istGeprueft(verdict)) return Uebernahme.Abgelehnt(urteilstext(verdict))
        if (neueKennung == null || neueFassung == null) {
            return Uebernahme.Abgelehnt("Der Inhalt des Pakets ist beanstandet worden.")
        }
        if (neueKennung != jetzige.manifest.id) {
            return Uebernahme.Abgelehnt(
                "Das ist ein anderes Paket ($neueKennung). Ausgetauscht wird nur dasselbe.",
            )
        }
        val stand = Paketmarken.lies(ordner)
        if (stand is Paketmarken.Stand.Unlesbar) {
            // Lieber anhalten als weitermachen, als gaebe es keine Marke. Eine
            // unlesbare Sicherheitsmarke ist kein Grund, den Schutz fallen zu
            // lassen -- sonst genuegte es, sie zu verderben.
            return Uebernahme.Abgelehnt(
                "Die Merkliste der angenommenen Fassungen ist beschädigt (${stand.grund}) " +
                    "Bis das geklärt ist, wird kein Paket übernommen.",
            )
        }
        // Fehlt die Marke noch -- frische Installation, altes Geraet --, gilt
        // ersatzweise die Fassung des geladenen Pakets. Der Schutz darf durch
        // die neue Merkliste nicht SCHWAECHER werden als vorher.
        val ausDerListe = (stand as Paketmarken.Stand.Gelesen).marken[neueKennung] ?: 0
        val marke = maxOf(ausDerListe, jetzige.manifest.version)
        if (neueFassung < marke) {
            if (!rueckstufungBestaetigt) {
                return Uebernahme.Rueckstufung(
                    grund = "Fassung $neueFassung ist älter als die höchste, die auf diesem " +
                        "Gerät schon einmal angenommen wurde ($marke). Berichtigte Angaben " +
                        "würden durch ältere ersetzt.",
                    marke = marke,
                    angeboten = neueFassung,
                )
            }
        }
        return Uebernahme.Angenommen(UEBERNOMMEN, neueFassung)
    }

    /**
     * Haelt fest, dass diese Fassung angenommen wurde.
     *
     * Wird auch beim Laden aufgerufen: Sonst bekaeme ein Geraet, auf dem nie
     * etwas uebernommen wurde, nie eine Marke -- und die Beigabe koennte durch
     * eine aeltere, gueltig unterschriebene Fassung ersetzt werden.
     */
    fun merkeFassung(ordner: File, fassung: Int, kennung: String) {
        runCatching { Paketmarken.hebe(ordner, kennung, fassung) }
    }

    // Die Pruefung arbeitet auf einer Datei, weil sie den Inhalt in einem
    // einzigen Durchlauf liest. Assets sind keine Dateien, also wird das Paket
    // beim ersten Start einmal in den App-Ordner gelegt.
    //
    // WARUM HIER GEHASHT WIRD UND NICHT DIE LAENGE VERGLICHEN: Vorher stand
    // hier ein Vergleich von assets.open(...).available() mit ziel.length().
    // Am 02.08.2026 hat das im Emulator dazu gefuehrt, dass ein frisch
    // eingespieltes Paket NICHT uebernommen wurde -- die App zeigte weiter den
    // alten Inhalt, und erst das Loeschen der App-Daten half. InputStream
    // .available() ist ausdruecklich keine Dateigroesse, sondern die Zahl der
    // Bytes, die ohne Blockieren gelesen werden koennen; bei einem
    // komprimierten Asset ist das etwas anderes. Ein Handbuch, das nach einem
    // Update stillschweigend den alten Stand zeigt, ist die gefaehrlichste
    // Art von Fehler, die dieses Projekt haben kann.
    //
    // Die Pruefsumme kostet einen Durchlauf ueber gut ein Megabyte beim Start
    // und ist dafuer eindeutig. Sie liegt als eigene kleine Datei daneben.
    private fun ausAssetsKopieren(context: Context): File {
        val ziel = File(context.filesDir, PAKETNAME)
        val merker = File(context.filesDir, "$PAKETNAME.sha256")
        val summe = pruefsummeDesAssets(context)
        if (ziel.exists() && merker.exists() && merker.readText() == summe) return ziel

        val temp = File(context.filesDir, "$PAKETNAME.teil")
        context.assets.open(PAKETNAME).use { quelle ->
            temp.outputStream().use { quelle.copyTo(it) }
        }
        if (ziel.exists() && !ziel.delete()) error("altes Paket ließ sich nicht ersetzen")
        if (!temp.renameTo(ziel)) error("Paket ließ sich nicht ablegen")
        // Der Merker wird ERST nach dem erfolgreichen Umbenennen geschrieben.
        // Bricht der Vorgang vorher ab, ist er alt oder fehlt -- dann wird beim
        // naechsten Start erneut kopiert. Andersherum waere der Merker eine
        // Behauptung ueber eine Datei, die es nicht gibt.
        merker.writeText(summe)
        return ziel
    }

    private fun pruefsummeDesAssets(context: Context): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        context.assets.open(PAKETNAME).use { quelle ->
            val puffer = ByteArray(64 * 1024)
            while (true) {
                val gelesen = quelle.read(puffer)
                if (gelesen <= 0) break
                digest.update(puffer, 0, gelesen)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun urteilstext(verdict: PackVerdict): String = when (verdict) {
        is PackVerdict.Trusted -> "Signatur geprüft: ${verdict.signer.name}"
        is PackVerdict.UnknownSigner -> "Unbekannter Signierer (${verdict.fingerprint.take(16)})"
        PackVerdict.BadSignature -> "Signatur ungültig"
        PackVerdict.Aborted -> "Prüfung abgebrochen"
        is PackVerdict.Unsupported -> "Paketfassung ${verdict.version} wird nicht unterstützt"
        is PackVerdict.Damaged -> "Paket beschädigt"
    }

    fun istGeprueft(verdict: PackVerdict): Boolean = verdict is PackVerdict.Trusted

    private fun hexZuBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "ungerade Hex-Länge" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
