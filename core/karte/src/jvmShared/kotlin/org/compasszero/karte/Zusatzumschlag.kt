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
 * Was der Umschlag über eine Zusatzdatei sagt.
 *
 * [urteil] ist null, wenn gar kein Umschlag da ist -- die Datei liegt dann
 * blank vor und niemand steht für sie ein. Das ist erlaubt und wird angezeigt,
 * nicht abgewiesen: Am 18.08.2026 so entschieden, aus demselben Grund wie bei
 * Karten -- wer sich sein Wegenetz selbst aus den Rohdaten baut, soll es
 * benutzen können.
 */
class Umschlagbefund(val versatz: Long, val laenge: Long, val urteil: PackVerdict?)

/**
 * Prüft den Umschlag um Satellitenbild, Namensverzeichnis und Wegenetz.
 *
 * EINE STELLE FÜR ALLE DREI, und das ist der Punkt. Dreimal dieselbe
 * Prüfung zu schreiben hieße, dass sie dreimal richtig sein muss -- und dass
 * eine spätere Berichtigung an zwei Stellen vergessen wird. Was sich je
 * Format unterscheidet, ist allein die Kennung.
 *
 * Der Ablauf ist derselbe wie bei `Kartenumschlag` und aus denselben Gründen:
 *
 *  * **Unterschrieben ist die Prüfsumme, nicht die Datei.** Reines Ed25519
 *    bräuchte zwei Durchgänge und die ganze Datei im Speicher; daran ist am
 *    04.08.2026 das Öffnen einer 346-MB-Karte gescheitert.
 *  * **Die Kennung steckt im unterschriebenen Teil.** Ohne sie ließe sich ein
 *    Wegenetz als Namensverzeichnis unterschieben, und die Unterschrift
 *    passte rechnerisch.
 *  * **Kaputte Unterschrift heißt: gar nicht öffnen.** Das ist der
 *    Unterschied zu einer unbekannt signierten Datei. Dort weiß man nur
 *    nicht, wer sie gemacht hat; hier weiß man, dass sie nicht mehr die ist,
 *    die jemand unterschrieben hat.
 */
object Zusatzumschlag {

    /**
     * Sieht nach, ob ein Umschlag da ist, und prüft ihn.
     *
     * [blankeKennung] ist die Kennung der Datei selbst (etwa `CZBILD01`). Sie
     * wird gebraucht, um "kein Umschlag" von "kaputt" zu unterscheiden: Was
     * weder mit dem einen noch mit dem anderen anfängt, ist keine Datei
     * dieser Art.
     */
    fun pruefe(
        pfad: File,
        umschlagKennung: ByteArray,
        blankeKennung: ByteArray,
        vertrauen: TrustStore,
    ): Umschlagbefund {
        if (!pfad.isFile) throw Kartenfehler("Datei fehlt: $pfad")
        val anfang = ByteArray(8)
        val dateigroesse: Long
        try {
            RandomAccessFile(pfad, "r").use { handle ->
                dateigroesse = handle.length()
                if (dateigroesse < anfang.size) throw Kartenfehler("$pfad ist zu kurz")
                handle.readFully(anfang)
            }
        } catch (fehler: IOException) {
            throw Kartenfehler("$pfad ist nicht lesbar: ${fehler.message}")
        }

        if (beginntMit(anfang, blankeKennung)) {
            // Kein Umschlag. Erlaubt, aber niemand steht dafür ein.
            return Umschlagbefund(0L, dateigroesse, null)
        }
        if (!beginntMit(anfang, umschlagKennung)) {
            throw Kartenfehler("$pfad hat eine fremde Kennung")
        }

        val kopf = ByteArray(PackFormat.HEADER_SIZE)
        try {
            RandomAccessFile(pfad, "r").use { it.readFully(kopf) }
        } catch (fehler: IOException) {
            throw Kartenfehler("Umschlag nicht lesbar: ${fehler.message}")
        }

        val gelesen = when (
            val ergebnis = PackFormat.parseHeader(kopf, dateigroesse, umschlagKennung)
        ) {
            is HeaderResult.Damaged ->
                throw Kartenfehler("Umschlag beschaedigt: ${ergebnis.kind} ${ergebnis.detail}")
            is HeaderResult.Unsupported ->
                throw Kartenfehler("Umschlagfassung ${ergebnis.version} wird nicht unterstuetzt")
            is HeaderResult.Ok -> ergebnis.header
        }

        val vorspann = PackFormat.signedPortion(
            gelesen.version, gelesen.signerKey, gelesen.payloadSize, umschlagKennung,
        )

        val summe = try {
            RandomAccessFile(pfad, "r").use { handle ->
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                handle.seek(PackFormat.HEADER_SIZE.toLong())
                val puffer = ByteArray(1024 * 1024)
                var uebrig = gelesen.payloadSize
                while (uebrig > 0) {
                    val will = minOf(uebrig, puffer.size.toLong()).toInt()
                    val kam = handle.read(puffer, 0, will)
                    if (kam <= 0) throw Kartenfehler("Datei endet vor der angekuendigten Laenge")
                    digest.update(puffer, 0, kam)
                    uebrig -= kam
                }
                digest.digest()
            }
        } catch (fehler: IOException) {
            throw Kartenfehler("Datei nicht lesbar: ${fehler.message}")
        }

        val passt = summe.inputStream().use {
            Ed25519.verify(gelesen.signerKey, gelesen.signature, vorspann, it)
        }
        if (!passt) {
            throw Kartenfehler(
                "Die Datei ist MANIPULIERT oder beschaedigt -- die Unterschrift passt nicht",
            )
        }

        val bekannt = vertrauen.find(gelesen.signerKey)
        val urteil = if (bekannt != null) {
            PackVerdict.Trusted(bekannt)
        } else {
            PackVerdict.UnknownSigner(Digests.fingerprint(gelesen.signerKey))
        }
        return Umschlagbefund(PackFormat.HEADER_SIZE.toLong(), gelesen.payloadSize, urteil)
    }

    private fun beginntMit(anfang: ByteArray, kennung: ByteArray): Boolean {
        if (anfang.size < kennung.size) return false
        for (i in kennung.indices) if (anfang[i] != kennung[i]) return false
        return true
    }
}
