package org.compasszero.android

import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import org.compasszero.transfer.Rahmen

/**
 * Die Bedienung des Paketaustauschs: senden, warten, abbrechen.
 *
 * WAS HIER NICHT PASSIERT: keine Vertrauensentscheidung und keine
 * Uebertragungslogik. Diese Klasse zeigt an, fragt und bricht ab; alles andere
 * liegt in `Paketfunk` und `Paketlader`.
 *
 * DIE DAUER STEHT VOR DEM SENDEN, nicht danach. Ein Inhaltspaket ist in gut
 * einer Minute drueben, eine Karte braucht Stunden -- wer das erst erfaehrt,
 * wenn der Balken kriecht, hat die falsche Datei gewaehlt und es zu spaet
 * gemerkt.
 */
class Austausch(
    private val gastgeber: Activity,
    private val paket: GeladenesPaket,
    private val gemerkt: Gemerkt,
    private val neuAufbauen: () -> Unit,
) {

    private var stand: String = ""
    private var standfeld: TextView? = null
    private var abbruch: Abbrecher? = null
    private val zurueck = Handler(Looper.getMainLooper())

    val laeuft: Boolean get() = abbruch != null

    fun baue(b: Bausteine, spalte: LinearLayout) {
        val lage = Paketfunk.lage(gastgeber)

        if (laeuft) {
            spalte.addView(b.fliesstext(stand), b.breit())
            spalte.addView(knopf(b, "Abbrechen") { abbruch?.brichAb() }, b.breit())
            spalte.addView(
                b.nebentext(
                    "Beide Geräte müssen beieinander bleiben, bis es fertig ist. Ein " +
                        "Abbruch lässt nichts Halbes zurück.",
                ),
                b.breit(),
            )
            return
        }

        spalte.addView(
            b.fliesstext(
                "Ein Inhaltspaket geht von Gerät zu Gerät, ohne Netz. Gekoppelt wird in " +
                    "den Android-Einstellungen — diese App sucht keine Geräte und macht " +
                    "sich nicht sichtbar.",
            ),
            b.breit(),
        )

        when (lage) {
            Funklage.KeinBluetooth -> {
                spalte.addView(b.nebentext("Dieses Gerät kann kein Bluetooth."), b.breit())
                return
            }

            Funklage.Ausgeschaltet -> {
                // KEIN KNOPF, DER BLUETOOTH EINSCHALTET. Das ist eine
                // Systemeinstellung, und sie gehoert dem Nutzer.
                spalte.addView(
                    b.nebentext("Bluetooth ist aus. Schalte es in den Android-Einstellungen ein."),
                    b.breit(),
                )
                return
            }

            Funklage.KeineErlaubnis, Funklage.Bereit -> Unit
        }

        spalte.addView(knopf(b, "Paket senden") { frageErlaubnis { waehleGeraet() } }, b.breit())
        spalte.addView(
            knopf(b, "Auf ein Paket warten") { frageErlaubnis { warteAufPaket() } },
            b.breit(),
        )
        standfeld = b.nebentext(stand).also { spalte.addView(it, b.breit()) }
        spalte.addView(
            b.nebentext(
                "Gesendet wird das Inhaltspaket dieser App (${groessentext(paket.datei.length())}, " +
                    "${Funk.dauertext(paket.datei.length())}). Karten sind dafür zu groß — sie " +
                    "gehören über Kabel oder Speicherkarte; warum, steht in der Anleitung.",
            ),
            b.breit(),
        )
        if (paket.vonAussen) {
            spalte.addView(
                b.nebentext(
                    "Dieses Gerät benutzt ein Paket, das es von einem anderen Gerät " +
                        "bekommen hat (Fassung ${paket.pack.manifest.version}).",
                ),
                b.breit(),
            )
        }
    }

    /**
     * Ab Android 12 ist BLUETOOTH_CONNECT eine Berechtigung, die zur Laufzeit
     * erfragt wird. Gefragt wird ERST HIER und nicht beim Start: Wer die App
     * nur als Handbuch benutzt, soll nie einen Dialog sehen.
     */
    private fun frageErlaubnis(dann: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT < 31) {
            dann()
            return
        }
        val recht = android.Manifest.permission.BLUETOOTH_CONNECT
        if (gastgeber.checkSelfPermission(recht) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            dann()
            return
        }
        (gastgeber as? MainActivity)?.frageBerechtigung(recht) { erteilt ->
            if (erteilt) {
                dann()
            } else {
                zeige("Ohne die Bluetooth-Erlaubnis geht der Austausch nicht.")
            }
        } ?: zeige("Die Erlaubnis ließ sich nicht erfragen.")
    }

    private fun waehleGeraet() {
        val geraete = Paketfunk.gekoppelte(gastgeber)
        if (geraete.isEmpty()) {
            zeige(
                "Kein gekoppeltes Gerät. Koppelt die beiden Geräte zuerst in den " +
                    "Android-Einstellungen miteinander.",
            )
            return
        }
        val namen = geraete.map { Paketfunk.geraetename(it) }.toTypedArray()
        AlertDialog.Builder(gastgeber)
            .setTitle("An welches Gerät?")
            .setItems(namen) { _, gewaehlt -> sendeAn(geraete[gewaehlt], namen[gewaehlt]) }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun sendeAn(geraet: android.bluetooth.BluetoothDevice, name: String) {
        val datei = paket.datei
        val schalter = Abbrecher()
        abbruch = schalter
        zeige("Verbinde mit „$name“ …")
        neuAufbauen()
        Thread({
            val ergebnis = Paketfunk.sende(
                geraet = geraet,
                datei = datei,
                melde = { bytes -> fortschritt("Sende an „$name“", bytes, datei.length()) },
                abbrecher = schalter,
            )
            fertig(
                when (ergebnis) {
                    is Funkergebnis.Fertig -> "Gesendet: ${groessentext(ergebnis.bytes)} an „$name“."
                    is Funkergebnis.Abgelehnt -> ergebnis.meldung
                    is Funkergebnis.Abgebrochen -> ergebnis.meldung
                    is Funkergebnis.Fehlgeschlagen -> ergebnis.meldung
                },
            )
        }, "paket-senden").start()
    }

    private fun warteAufPaket() {
        val schalter = Abbrecher()
        abbruch = schalter
        zeige("Warte auf ein Gerät … Auf dem anderen Gerät jetzt „Paket senden“ antippen.")
        neuAufbauen()
        Thread({
            val ergebnis = Paketfunk.empfange(
                context = gastgeber,
                frage = ::frageAngebot,
                melde = { bytes -> fortschritt("Empfange", bytes, 0) },
                abbrecher = schalter,
            )
            val meldung = when (ergebnis) {
                is Funkergebnis.Fertig -> ergebnis.datei?.let { pruefeUndUebernimm(it) }
                    ?: "Empfangen, aber die Datei ist verschwunden."

                is Funkergebnis.Abgelehnt -> ergebnis.meldung
                is Funkergebnis.Abgebrochen -> ergebnis.meldung
                is Funkergebnis.Fehlgeschlagen -> ergebnis.meldung
            }
            fertig(meldung)
        }, "paket-empfangen").start()
    }

    /**
     * Fragt den Nutzer, ob das angebotene Paket hereinkommen darf -- und
     * blockiert den Funkfaden so lange.
     *
     * OHNE DIESE RUECKFRAGE koennte ein gekoppeltes Geraet unbemerkt Dateien
     * ablegen. Der angezeigte Name kommt von drueben und ist eine Behauptung;
     * er steht deshalb in Anfuehrungszeichen und nicht als Tatsache da.
     */
    private fun frageAngebot(angebot: Rahmen.Angebot): Boolean {
        val sperre = java.util.concurrent.CountDownLatch(1)
        var ja = false
        zurueck.post {
            AlertDialog.Builder(gastgeber)
                .setTitle("Paket annehmen?")
                .setMessage(
                    "Ein Gerät bietet „${angebot.name}“ an (${groessentext(angebot.groesse)}, " +
                        "${Funk.dauertext(angebot.groesse)}).\n\nAngenommen wird es erst " +
                        "geprüft: Es kommt nur zum Einsatz, wenn seine Unterschrift hält und " +
                        "es neuer ist als das vorhandene.",
                )
                .setPositiveButton("Annehmen") { _, _ -> ja = true; sperre.countDown() }
                .setNegativeButton("Ablehnen") { _, _ -> sperre.countDown() }
                .setOnCancelListener { sperre.countDown() }
                .show()
        }
        sperre.await()
        return ja
    }

    /** Die Signaturpruefung nach dem Empfang -- dieselbe wie bei der Beigabe. */
    private fun pruefeUndUebernimm(datei: File): String =
        when (val urteil = Paketlader.uebernimmEmpfangenes(gastgeber, datei, paket.pack)) {
            is Uebernahme.Angenommen -> {
                gemerkt.eigenesPaket = urteil.name
                "Angenommen und geprüft: Fassung ${urteil.fassung}. Beim nächsten Start " +
                    "der App ist sie da."
            }

            is Uebernahme.Abgelehnt -> {
                // Was nicht durchkommt, bleibt nicht liegen. Eine abgelehnte
                // Datei im Eingang waere ein Angebot an den naechsten Fehler.
                datei.delete()
                "Nicht übernommen: ${urteil.grund}"
            }
        }

    private fun fortschritt(was: String, bytes: Long, ganz: Long) {
        val text = if (ganz > 0) {
            "$was … ${groessentext(bytes)} von ${groessentext(ganz)}"
        } else {
            "$was … ${groessentext(bytes)}"
        }
        zurueck.post {
            stand = text
            standfeld?.text = text
        }
    }

    private fun fertig(meldung: String) {
        zurueck.post {
            abbruch = null
            stand = meldung
            neuAufbauen()
        }
    }

    private fun zeige(text: String) {
        stand = text
        standfeld?.text = text
    }

    private fun knopf(b: Bausteine, beschriftung: String, beiTipp: () -> Unit) =
        Button(gastgeber).apply {
            text = beschriftung
            textSize = b.stil.textGroesse
            typeface = b.stil.textSchrift
            setTextColor(b.stil.text)
            background = b.randfeld()
            setPadding(b.stil.abstand, b.stil.abstand / 2, b.stil.abstand, b.stil.abstand / 2)
            setOnClickListener { beiTipp() }
        }

    private fun groessentext(bytes: Long): String = when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "${bytes / 1_000} kB"
        else -> "$bytes Byte"
    }
}
