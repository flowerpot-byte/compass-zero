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
                if (versuch != null && istGeprueft(versuch.verdict)) return@runCatching versuch
            }
        }
        oeffne(beigabe, vonAussen = false)
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
    fun uebernimmEmpfangenes(context: Context, quelle: File, jetziges: LoadedPack): Uebernahme {
        val gelesen = try {
            PackReader.read(quelle, vertrauensspeicher())
        } catch (fehler: Exception) {
            return Uebernahme.Abgelehnt("Die Datei ließ sich nicht lesen.")
        }
        if (!istGeprueft(gelesen.verdict)) {
            return Uebernahme.Abgelehnt(urteilstext(gelesen.verdict))
        }
        val neu = gelesen.result?.pack
            ?: return Uebernahme.Abgelehnt("Der Inhalt des Pakets ist beanstandet worden.")
        if (neu.manifest.id != jetziges.manifest.id) {
            return Uebernahme.Abgelehnt(
                "Das ist ein anderes Paket (${neu.manifest.id}). Ausgetauscht wird nur dasselbe.",
            )
        }
        if (neu.manifest.version <= jetziges.manifest.version) {
            return Uebernahme.Abgelehnt(
                "Fassung ${neu.manifest.version} ist nicht neuer als die vorhandene " +
                    "${jetziges.manifest.version}. Eine ältere Fassung wird nicht eingespielt.",
            )
        }
        val ziel = File(context.filesDir, UEBERNOMMEN)
        val teil = File(context.filesDir, "$UEBERNOMMEN.teil")
        return try {
            quelle.inputStream().use { ein -> teil.outputStream().use { ein.copyTo(it) } }
            if (ziel.exists() && !ziel.delete()) error("altes Paket ließ sich nicht ersetzen")
            if (!teil.renameTo(ziel)) error("Paket ließ sich nicht ablegen")
            quelle.delete()
            Uebernahme.Angenommen(UEBERNOMMEN, neu.manifest.version)
        } catch (fehler: Exception) {
            teil.delete()
            Uebernahme.Abgelehnt(fehler.message ?: "Das Paket ließ sich nicht ablegen.")
        }
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
