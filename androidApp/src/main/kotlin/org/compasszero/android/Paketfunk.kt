package org.compasszero.android

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.File
import org.compasszero.transfer.Ablehnungsgrund
import org.compasszero.transfer.Antwort
import org.compasszero.transfer.Empfaenger
import org.compasszero.transfer.GrussErgebnis
import org.compasszero.transfer.LeseErgebnis
import org.compasszero.transfer.Rahmen
import org.compasszero.transfer.RahmenCodec
import org.compasszero.transfer.RahmenLeser
import org.compasszero.transfer.SendeAntwort
import org.compasszero.transfer.Sender
import org.compasszero.transfer.TransferFormat

/**
 * Der Paketaustausch von Geraet zu Geraet ueber Bluetooth.
 *
 * WAS HIER NICHT PASSIERT -- und das ist der Grund, warum diese Datei so kurz
 * ist: keine Zustandsmaschine, keine Groessengrenze, keine Pruefsumme. Das
 * alles steht in `core/transfer` und ist dort ohne Geraet pruefbar. Hier wird
 * nur ein Funkkanal davorgehaengt und der Ablauf getrieben.
 *
 * ES WIRD NICHT GESUCHT UND NICHT GEWORBEN. Gesprochen wird ausschliesslich
 * mit Geraeten, die in den Android-Einstellungen schon gekoppelt sind. Das
 * kostet einen Schritt beim ersten Mal und spart die Ortungsberechtigung
 * dauerhaft -- bis Android 11 gilt eine Geraetesuche dem System als Ortsangabe,
 * und eine App, die verspricht, den Standort nie zu kennen, darf danach nicht
 * fragen.
 *
 * EIN PAKET UEBER FUNK IST KEIN BESSERES PAKET. Was ankommt, landet im Eingang
 * und geht von dort durch dieselbe Signaturpruefung wie eine von Hand kopierte
 * Datei.
 */
object Paketfunk {

    /** Wohin Empfangenes zuerst faellt -- geprueft wird danach. */
    fun eingang(context: Context): File =
        File(context.filesDir, "eingang").apply { mkdirs() }

    private fun adapter(context: Context): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    /** Ob dieses Geraet ueberhaupt funken kann und der Funk eingeschaltet ist. */
    fun lage(context: Context): Funklage {
        val funk = adapter(context) ?: return Funklage.KeinBluetooth
        return try {
            if (funk.isEnabled) Funklage.Bereit else Funklage.Ausgeschaltet
        } catch (fehler: SecurityException) {
            Funklage.KeineErlaubnis
        }
    }

    /**
     * Die gekoppelten Geraete.
     *
     * Bewusst ALLE und nicht nur die, auf denen Compass Zero laeuft: Ob drueben
     * ein Gegenstueck horcht, weiss man erst beim Verbinden. Eine Liste, die
     * das vorher zu wissen vorgibt, muesste dafuer suchen -- und Suchen ist
     * genau das, was hier nicht stattfindet.
     */
    fun gekoppelte(context: Context): List<BluetoothDevice> =
        try {
            adapter(context)?.bondedDevices?.sortedBy { it.address }.orEmpty()
        } catch (fehler: SecurityException) {
            emptyList()
        }

    /** Der Anzeigename eines Geraets, ohne dass eine fehlende Erlaubnis stoert. */
    fun geraetename(geraet: BluetoothDevice): String =
        try {
            geraet.name ?: geraet.address
        } catch (fehler: SecurityException) {
            geraet.address
        }

    /**
     * Sendet eine Datei an ein gekoppeltes Geraet. Laeuft auf dem aufrufenden
     * Faden und gehoert deshalb NICHT auf den Hauptfaden.
     */
    fun sende(
        geraet: BluetoothDevice,
        datei: File,
        melde: (Long) -> Unit,
        abbrecher: Abbrecher,
    ): Funkergebnis {
        val groesse = datei.length()
        if (groesse <= 0) return Funkergebnis.Fehlgeschlagen("Die Datei ist leer.")
        if (groesse > TransferFormat.MAX_PAKET_BYTES) {
            return Funkergebnis.Fehlgeschlagen("Die Datei ist zu groß für den Austausch.")
        }
        val angebot = Rahmen.Angebot(
            groesse = groesse,
            pruefsumme = pruefsummeVon(datei),
            name = datei.name,
        )
        var draht: BluetoothSocket? = null
        return try {
            val verbindung = geraet.createRfcommSocketToServiceRecord(Funk.KENNUNG)
            draht = verbindung
            abbrecher.merke(verbindung)
            // connect() blockiert. Ein Abbruch waehrend dieser Zeit kommt als
            // Ausnahme heraus -- siehe unten.
            verbindung.connect()
            DateiQuelle(datei).use { quelle ->
                fahreSenden(verbindung, Sender(quelle, angebot), melde, abbrecher)
            }
        } catch (fehler: SecurityException) {
            Funkergebnis.Fehlgeschlagen("Die Bluetooth-Erlaubnis fehlt.")
        } catch (fehler: Exception) {
            if (abbrecher.gewuenscht) abgebrochen() else Funkergebnis.Fehlgeschlagen(klartext(fehler))
        } finally {
            abbrecher.merke(null as BluetoothSocket?)
            schliesse(draht)
        }
    }

    /**
     * Wartet auf ein sendendes Geraet und nimmt eine Datei entgegen.
     *
     * [frage] wird auf DIESEM Faden gerufen und muss blockieren, bis der Nutzer
     * entschieden hat. Ein Angebot ohne Rueckfrage anzunehmen hiesse, dass ein
     * gekoppeltes Geraet unbemerkt Dateien ablegen kann.
     */
    fun empfange(
        context: Context,
        frage: (Rahmen.Angebot) -> Boolean,
        melde: (Long) -> Unit,
        abbrecher: Abbrecher,
    ): Funkergebnis {
        var horcher: BluetoothServerSocket? = null
        var draht: BluetoothSocket? = null
        return try {
            val funk = adapter(context)
                ?: return Funkergebnis.Fehlgeschlagen("Dieses Gerät kann kein Bluetooth.")
            val warten = funk.listenUsingRfcommWithServiceRecord(Funk.DIENSTNAME, Funk.KENNUNG)
            horcher = warten
            abbrecher.merke(warten)
            // accept() blockiert, bis sich jemand meldet. Wer abbricht,
            // schliesst diesen Horchposten -- das ist der einzige Weg heraus.
            val verbindung = warten.accept()
            draht = verbindung
            abbrecher.merke(verbindung)
            // Der Horchposten wird sofort geschlossen: Es geht um EINE
            // Uebergabe, nicht um einen laufenden Dienst. Was nicht horcht,
            // kann auch nicht angesprochen werden.
            schliesseHorcher(horcher)
            horcher = null
            abbrecher.merke(null as BluetoothServerSocket?)
            fahreEmpfangen(context, verbindung, frage, melde, abbrecher)
        } catch (fehler: SecurityException) {
            Funkergebnis.Fehlgeschlagen("Die Bluetooth-Erlaubnis fehlt.")
        } catch (fehler: Exception) {
            if (abbrecher.gewuenscht) abgebrochen() else Funkergebnis.Fehlgeschlagen(klartext(fehler))
        } finally {
            abbrecher.merke(null as BluetoothServerSocket?)
            abbrecher.merke(null as BluetoothSocket?)
            schliesseHorcher(horcher)
            schliesse(draht)
        }
    }

    private fun fahreSenden(
        verbindung: BluetoothSocket,
        sender: Sender,
        melde: (Long) -> Unit,
        abbrecher: Abbrecher,
    ): Funkergebnis {
        val schreiber = Rahmenschreiber(verbindung.outputStream)
        val leser = RahmenLeser(StromQuelle(verbindung.inputStream))
        schreiber.schreibe(RahmenCodec.schreibeGruss())
        gruessen(leser)?.let { return it }

        var antwort = sender.beginne()
        while (true) {
            if (abbrecher.gewuenscht) {
                (sender.abbrich() as? SendeAntwort.Senden)?.let {
                    schreiber.schreibe(RahmenCodec.schreibe(it.rahmen))
                }
                return abgebrochen()
            }
            when (val jetzt = antwort) {
                is SendeAntwort.Senden -> {
                    val rahmen = jetzt.rahmen
                    schreiber.schreibe(RahmenCodec.schreibe(rahmen))
                    melde(sender.gesendeteBytes)
                    antwort = when (rahmen) {
                        // Nach dem Angebot spricht die Gegenseite; nach jedem
                        // Datenstueck sind wir wieder dran.
                        is Rahmen.Angebot -> antwortAbwarten(leser, sender) ?: return abriss()
                        is Rahmen.Fertig -> return Funkergebnis.Fertig(sender.gesendeteBytes, null)
                        is Rahmen.Abbruch -> return Funkergebnis.Abgebrochen("Abgebrochen.")
                        else -> sender.naechstes()
                    }
                }

                is SendeAntwort.Abgelehnt ->
                    return Funkergebnis.Abgelehnt(benenne(jetzt.grund))

                is SendeAntwort.Abgebrochen ->
                    return Funkergebnis.Abgebrochen(
                        "Die Gegenseite hat abgebrochen (${jetzt.grund}).",
                    )

                is SendeAntwort.Fehlgeschlagen -> {
                    jetzt.senden?.let { schreiber.schreibe(RahmenCodec.schreibe(it)) }
                    return Funkergebnis.Fehlgeschlagen("Übertragung gescheitert: ${jetzt.fehler}")
                }
            }
        }
    }

    private fun antwortAbwarten(leser: RahmenLeser, sender: Sender): SendeAntwort? =
        when (val gelesen = leser.liesRahmen()) {
            is LeseErgebnis.Ok -> sender.rahmenEmpfangen(gelesen.rahmen)
            LeseErgebnis.Ende -> null
            is LeseErgebnis.Fehler -> SendeAntwort.Fehlgeschlagen(gelesen.fehler, null)
        }

    private fun fahreEmpfangen(
        context: Context,
        verbindung: BluetoothSocket,
        frage: (Rahmen.Angebot) -> Boolean,
        melde: (Long) -> Unit,
        abbrecher: Abbrecher,
    ): Funkergebnis {
        val schreiber = Rahmenschreiber(verbindung.outputStream)
        val leser = RahmenLeser(StromQuelle(verbindung.inputStream))
        schreiber.schreibe(RahmenCodec.schreibeGruss())
        gruessen(leser)?.let { return it }

        var ziel: File? = null
        var empfaenger: Empfaenger? = null
        while (true) {
            if (abbrecher.gewuenscht) {
                (empfaenger?.abbrich() as? Antwort.Senden)?.let {
                    schreiber.schreibe(RahmenCodec.schreibe(it.rahmen))
                }
                return abgebrochen()
            }
            val gelesen = when (val stand = leser.liesRahmen()) {
                is LeseErgebnis.Ok -> stand.rahmen
                LeseErgebnis.Ende -> return abriss()
                is LeseErgebnis.Fehler ->
                    return Funkergebnis.Fehlgeschlagen("Übertragung gescheitert: ${stand.fehler}")
            }
            // Der Empfaenger entsteht erst mit dem Angebot: Vorher weiss
            // niemand, wie die Datei heissen soll, und eine Senke ohne Namen
            // legt schon eine Datei an, bevor jemand zugestimmt hat.
            val laufend = empfaenger ?: run {
                if (gelesen !is Rahmen.Angebot) {
                    return Funkergebnis.Fehlgeschlagen(
                        "Die Gegenseite fängt nicht mit einem Angebot an.",
                    )
                }
                val datei = File(eingang(context), sichererName(gelesen.name))
                ziel = datei
                Empfaenger(DateiSenke(datei), Sha256Pruefsumme()).also { empfaenger = it }
            }

            var antwort = laufend.rahmenEmpfangen(gelesen)
            while (true) {
                when (val jetzt = antwort) {
                    Antwort.Nichts -> {
                        melde(laufend.empfangeneBytes)
                        break
                    }

                    is Antwort.Senden -> {
                        schreiber.schreibe(RahmenCodec.schreibe(jetzt.rahmen))
                        break
                    }

                    is Antwort.Frage -> {
                        antwort = laufend.entscheide(
                            frage(jetzt.angebot),
                            Ablehnungsgrund.NutzerLehntAb,
                        )
                    }

                    is Antwort.Fertig -> return Funkergebnis.Fertig(jetzt.bytes, ziel)

                    is Antwort.Abgebrochen ->
                        return Funkergebnis.Abgebrochen(
                            "Die Gegenseite hat abgebrochen (${jetzt.grund}).",
                        )

                    is Antwort.Fehlgeschlagen -> {
                        jetzt.senden?.let { schreiber.schreibe(RahmenCodec.schreibe(it)) }
                        return Funkergebnis.Fehlgeschlagen(
                            "Übertragung gescheitert: ${jetzt.fehler}",
                        )
                    }
                }
            }
        }
    }

    private fun gruessen(leser: RahmenLeser): Funkergebnis? = when (val gruss = leser.liesGruss()) {
        GrussErgebnis.Ok -> null
        is GrussErgebnis.FremdeVersion -> Funkergebnis.Fehlgeschlagen(
            "Das andere Gerät spricht Fassung ${gruss.version}. Beide brauchen dieselbe Ausgabe.",
        )

        is GrussErgebnis.Fehler -> Funkergebnis.Fehlgeschlagen(
            "Am anderen Ende meldet sich kein Compass Zero.",
        )
    }

    private fun abriss() = Funkergebnis.Fehlgeschlagen("Die Verbindung ist abgerissen.")

    private fun abgebrochen() = Funkergebnis.Abgebrochen("Du hast abgebrochen.")

    /**
     * Macht aus dem Namen der Gegenseite einen Dateinamen, dem man trauen kann.
     *
     * DER NAME KOMMT VON EINEM FREMDEN GERAET. Ohne diese Stelle genuegte ein
     * Angebot namens "../../databases/x", um an einer anderen Stelle zu landen
     * als im Eingang. Deshalb: nur der letzte Namensteil, nur harmlose Zeichen,
     * und am Ende immer .czp -- der Lader nimmt nichts anderes.
     */
    fun sichererName(roh: String): String {
        val kurz = roh.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trimStart('.')
            .take(80)
        val kern = if (kurz.isBlank()) "empfangen" else kurz
        return if (kern.endsWith(".czp", ignoreCase = true)) kern else "$kern.czp"
    }

    private fun benenne(grund: Ablehnungsgrund): String = when (grund) {
        Ablehnungsgrund.NutzerLehntAb -> "Die Gegenseite hat abgelehnt."
        else -> "Die Gegenseite hat abgelehnt ($grund)."
    }

    // Was Android hier wirft, ist fuer den Nutzer unlesbar ("read failed,
    // socket might closed"). Der Grund ist fast immer derselbe: drueben horcht
    // niemand.
    private fun klartext(fehler: Exception): String {
        val roh = fehler.message.orEmpty()
        return when {
            roh.contains("read failed", ignoreCase = true) ||
                roh.contains("socket might closed", ignoreCase = true) ||
                roh.contains("Connection refused", ignoreCase = true) ->
                "Keine Verbindung. Wartet das andere Gerät wirklich auf ein Paket?"

            roh.isBlank() -> "Die Verbindung ist gescheitert (${fehler::class.simpleName})."
            else -> roh
        }
    }

    private fun schliesse(draht: BluetoothSocket?) {
        try {
            draht?.close()
        } catch (fehler: Exception) {
            // Beim Aufraeumen ist nichts mehr zu retten.
        }
    }

    private fun schliesseHorcher(horcher: BluetoothServerSocket?) {
        try {
            horcher?.close()
        } catch (fehler: Exception) {
            // Ebenso.
        }
    }
}

/**
 * Der Abbruch einer laufenden Uebertragung.
 *
 * EINE FLAGGE ALLEIN REICHT NICHT, und das war am 18.08.2026 am Geraet zu
 * sehen: Wer auf ein Paket wartet, steckt in `accept()`, und wer sendet,
 * steckt in `connect()`. Beide kehren nicht zurueck, ehe etwas passiert --
 * die Schleife, die eine Flagge lesen wuerde, laeuft zu diesem Zeitpunkt
 * ueberhaupt noch nicht. Der Abbruchknopf tat deshalb nichts.
 *
 * Der einzige Weg heraus ist, die Verbindung von aussen zu SCHLIESSEN. Das
 * loest im wartenden Faden eine Ausnahme aus -- die dann kein Fehler ist,
 * sondern der Abbruch. Deshalb merkt sich dieses Objekt beides: den Wunsch
 * und die Verbindungen, die ihn ausfuehren.
 */
class Abbrecher {

    private val flagge = java.util.concurrent.atomic.AtomicBoolean(false)

    @Volatile
    private var horcher: BluetoothServerSocket? = null

    @Volatile
    private var draht: BluetoothSocket? = null

    val gewuenscht: Boolean get() = flagge.get()

    fun brichAb() {
        flagge.set(true)
        schliesseStill { horcher?.close() }
        schliesseStill { draht?.close() }
    }

    internal fun merke(neuerHorcher: BluetoothServerSocket?) {
        horcher = neuerHorcher
    }

    internal fun merke(neuerDraht: BluetoothSocket?) {
        draht = neuerDraht
    }

    private fun schliesseStill(tu: () -> Unit) {
        try {
            tu()
        } catch (fehler: Exception) {
            // Beim Abbrechen ist ein Fehler beim Schliessen bedeutungslos.
        }
    }
}

enum class Funklage { Bereit, Ausgeschaltet, KeinBluetooth, KeineErlaubnis }

sealed interface Funkergebnis {
    class Fertig(val bytes: Long, val datei: File?) : Funkergebnis
    class Abgelehnt(val meldung: String) : Funkergebnis
    class Abgebrochen(val meldung: String) : Funkergebnis
    class Fehlgeschlagen(val meldung: String) : Funkergebnis
}
