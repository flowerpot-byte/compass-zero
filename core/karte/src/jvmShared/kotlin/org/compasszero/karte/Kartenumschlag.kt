package org.compasszero.karte

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import org.compasszero.security.Digests
import org.compasszero.security.Ed25519
import org.compasszero.security.HeaderResult
import org.compasszero.security.PackFormat
import org.compasszero.security.PackVerdict
import org.compasszero.security.TrustStore

/**
 * Eine geoeffnete Karte samt Urteil ueber ihre Herkunft.
 *
 * `urteil` ist `null`, wenn die Datei gar keinen Umschlag trug. Das ist kein
 * Fehler -- eine Karte, die als Beigabe im APK liegt, steht unter der
 * Unterschrift des APK und braucht keine eigene. Fuer eine Datei, die von
 * aussen kommt, ist es dagegen die wichtigste Auskunft, die es ueber sie gibt.
 */
class GeoeffneteKarte(
    val datei: Kartendatei,
    val urteil: PackVerdict?,
) : AutoCloseable {

    val unterschrieben: Boolean get() = urteil != null

    val geprueft: Boolean get() = urteil is PackVerdict.Trusted

    override fun close() = datei.close()
}

/**
 * Oeffnet eine Kartendatei -- mit oder ohne Unterschrift.
 *
 * **Die Unterschrift wird bei JEDEM Oeffnen vollstaendig nachgerechnet.** Das
 * kostet bei einer grossen Karte spuerbar Zeit, und die Versuchung ist gross,
 * das Urteil in einer Merkerdatei abzulegen und beim naechsten Mal zu glauben.
 * Genau das waere aber die Luecke: Ein Merker ueber Groesse und
 * Aenderungszeit laesst sich von jemandem faelschen, der die Kartendatei
 * austauschen kann -- und vor genau dem soll die Unterschrift schuetzen. Wer
 * nur einmal prueft, prueft in Wahrheit nie.
 */
object Kartenumschlag {

    fun oeffne(pfad: File, vertrauen: TrustStore): GeoeffneteKarte {
        if (!pfad.isFile) throw Kartenfehler("Kartendatei fehlt: $pfad")
        val anfang = ByteArray(4)
        try {
            RandomAccessFile(pfad, "r").use { handle ->
                if (handle.length() < Kartenformat.KOPF_BYTES) {
                    throw Kartenfehler("$pfad ist zu kurz fuer eine Karte")
                }
                handle.readFully(anfang)
            }
        } catch (fehler: IOException) {
            throw Kartenfehler("$pfad ist nicht lesbar: ${fehler.message}")
        }

        // Die beiden Kennungen unterscheiden sich erst im VIERTEN Byte:
        // "CZKA..." ist die blanke Karte, "CZK1" der Umschlag. Beide werden
        // deshalb ganz verglichen und nicht ueber ein Praefix.
        if (passt(anfang, PackFormat.KARTE_MAGIC)) {
            return mitUmschlag(pfad, vertrauen)
        }
        if (passt(anfang, Kartenformat.KENNUNG)) {
            return GeoeffneteKarte(Kartendatei.oeffne(pfad), null)
        }
        throw Kartenfehler("$pfad ist weder eine Karte noch eine unterschriebene Karte")
    }

    private fun passt(anfang: ByteArray, kennung: ByteArray): Boolean {
        for (i in 0 until 4) if (anfang[i] != kennung[i]) return false
        return true
    }

    private fun mitUmschlag(pfad: File, vertrauen: TrustStore): GeoeffneteKarte {
        val kopf = ByteArray(PackFormat.HEADER_SIZE)
        val dateigroesse: Long
        try {
            RandomAccessFile(pfad, "r").use { handle ->
                dateigroesse = handle.length()
                handle.readFully(kopf)
            }
        } catch (fehler: IOException) {
            throw Kartenfehler("Umschlag nicht lesbar: ${fehler.message}")
        }

        val gelesen = when (
            val ergebnis = PackFormat.parseHeader(kopf, dateigroesse, PackFormat.KARTE_MAGIC)
        ) {
            is HeaderResult.Damaged ->
                throw Kartenfehler("Umschlag beschaedigt: ${ergebnis.kind} ${ergebnis.detail}")
            is HeaderResult.Unsupported ->
                throw Kartenfehler("Umschlagfassung ${ergebnis.version} wird nicht unterstuetzt")
            is HeaderResult.Ok -> ergebnis.header
        }

        val vorspann = PackFormat.signedPortion(
            gelesen.version, gelesen.signerKey, gelesen.payloadSize, PackFormat.KARTE_MAGIC,
        )

        // Unterschrieben ist die PRUEFSUMME der Karte, nicht die Karte selbst.
        // Reines Ed25519 braucht zwei Durchgaenge und muesste die ganze Datei
        // im Speicher halten -- am 04.08.2026 ist genau daran das Oeffnen einer
        // 346-MB-Karte gescheitert: Der Puffer wuchs auf 128 MB, das Telefon
        // hatte keinen mehr. Die Pruefsumme entsteht im Durchlauf.
        val summe = try {
            RandomAccessFile(pfad, "r").use { handle ->
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                handle.seek(PackFormat.HEADER_SIZE.toLong())
                // Ein Megabyte am Stueck. Der Speicher eines Android-Geraets
                // haengt hinter einer Zwischenschicht (FUSE), die je
                // LESEVORGANG kostet und nicht je Byte -- grosse Happen sind
                // dort billiger als viele kleine.
                //
                // EHRLICH DAZU, weil im Verlauf etwas anderes behauptet wurde:
                // Diese Zeile hat KEINEN gemessenen Fehler behoben. Sie
                // entstand am 06.08.2026 aus der Vermutung, das Oeffnen der
                // Karten daure ueber 25 Sekunden. Die Messung im Programm hat
                // das widerlegt -- 437 MB brauchen 912 ms, 345 MB brauchen
                // 1323 ms, zusammen also gut zwei Sekunden. Die 25 Sekunden
                // waren ein Messfehler von aussen: Fehlgeschlagene
                // Bildschirmauszuege wurden als "noch nicht fertig" gezaehlt.
                // Der groessere Puffer bleibt, weil er richtig ist; aber er ist
                // kein Fix, und wer hier spaeter sucht, soll das wissen.
                val puffer = ByteArray(1024 * 1024)
                var uebrig = gelesen.payloadSize
                while (uebrig > 0) {
                    val will = minOf(uebrig, puffer.size.toLong()).toInt()
                    val kam = handle.read(puffer, 0, will)
                    if (kam <= 0) throw Kartenfehler("Karte endet vor der angekuendigten Laenge")
                    digest.update(puffer, 0, kam)
                    uebrig -= kam
                }
                digest.digest()
            }
        } catch (fehler: IOException) {
            throw Kartenfehler("Karte nicht lesbar: ${fehler.message}")
        }

        // Eine Karte mit kaputter Unterschrift wird NICHT geoeffnet. Das ist
        // der Unterschied zu einer unbekannt signierten: Dort weiss man nur
        // nicht, wer sie gemacht hat. Hier weiss man, dass sie nicht mehr die
        // ist, die jemand unterschrieben hat.
        val passt = summe.inputStream().use {
            Ed25519.verify(gelesen.signerKey, gelesen.signature, vorspann, it)
        }
        if (!passt) {
            throw Kartenfehler("Die Karte ist MANIPULIERT oder beschaedigt -- Unterschrift passt nicht")
        }

        val bekannt = vertrauen.find(gelesen.signerKey)
        val urteil = if (bekannt != null) {
            PackVerdict.Trusted(bekannt)
        } else {
            PackVerdict.UnknownSigner(Digests.fingerprint(gelesen.signerKey))
        }
        val karte = Kartendatei.oeffne(pfad, PackFormat.HEADER_SIZE.toLong(), gelesen.payloadSize)
        return GeoeffneteKarte(karte, urteil)
    }
}
