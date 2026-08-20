package org.compasszero.android

import android.app.Activity
import android.content.pm.PackageManager
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import java.io.File
import java.security.MessageDigest
import org.compasszero.security.PackVerdict

// Einstellungen und Verbindung. Was hier steht, soll der Nutzer nachpruefen
// koennen, ohne uns zu glauben: welche Berechtigungen die App wirklich
// angefordert hat, welches Paket geladen ist und mit welcher Pruefsumme.
class Einstellungen(
    private val gastgeber: Activity,
    private val paket: GeladenesPaket,
    private val sparmodusAn: () -> Boolean,
    private val sparmodusUmschalten: () -> Unit,
    private val gemerkt: Gemerkt,
    private val neuAufbauen: () -> Unit,
) : Bereich {

    override val name = "Einstellungen"
    override val bild = R.drawable.sym_einstellungen

    // Die Anleitung liegt als eigene Seite hinter dieser hier. Sie gehoert
    // nicht in die untere Leiste: Dort stehen vier Bereiche, und wer eine
    // Anleitung braucht, sucht sie da, wo auch die Pruefsummen stehen.
    private val anleitung = Anleitung(gastgeber) { neuAufbauen() }

    // Der Paketaustausch haelt einen laufenden Vorgang fest und lebt deshalb
    // so lange wie dieser Bereich -- die Seite wird bei jedem Reiterwechsel
    // neu gebaut, eine Uebertragung darf davon nichts merken.
    private val austausch = Austausch(gastgeber, paket, gemerkt) { neuAufbauen() }
    private var anleitungOffen = false

    private var pruefsumme: String? = null
    // Rueckmeldung des Einlesens. Bleibt stehen, bis die Seite neu gebaut
    // wird -- wer eine halbe Minute auf eine 432-MB-Datei gewartet hat,
    // soll das Ergebnis nicht suchen muessen.
    private var einlesestand: String? = null
    private var standfeld: android.widget.TextView? = null

    // Solange die Anleitung offen ist, verbraucht sie die Zurueck-Taste. Ohne
    // das springt sie beim ersten Druck zum Lexikon, und die halb gelesene
    // Seite ist weg.
    override fun zurueck(): Boolean {
        if (anleitung.zurueck()) return true
        if (!anleitungOffen) return false
        anleitungOffen = false
        neuAufbauen()
        return true
    }

    override fun aufAnfang() {
        anleitungOffen = false
        anleitung.aufAnfang()
    }

    override fun baue(b: Bausteine): View {
        if (anleitungOffen) {
            return anleitung.baue(b) {
                anleitungOffen = false
                neuAufbauen()
            }
        }
        val spalte = b.spalte().apply { setPadding(0, 0, 0, b.stil.abstand) }

        spalte.addView(b.ueberschrift("Anleitung"), b.breit())
        spalte.addView(
            Button(gastgeber).apply {
                text = "Karten, Ausgaben, Echtheit"
                textSize = b.stil.textGroesse
                typeface = b.stil.textSchrift
                setTextColor(b.stil.text)
                background = b.randfeld()
                setPadding(b.stil.abstand, b.stil.abstand / 2, b.stil.abstand, b.stil.abstand / 2)
                setOnClickListener {
                    anleitungOffen = true
                    neuAufbauen()
                }
            },
            b.breit(),
        )
        spalte.addView(
            b.nebentext(
                "Wie weitere Karten ins Gerät kommen, woher es sie gibt, wie eine neuere " +
                    "Ausgabe eingespielt wird und woran du erkennst, ob an dieser App " +
                    "etwas verändert wurde.",
            ),
            b.breit(),
        )

        spalte.addView(b.trennstrich(), b.strichbreit())
        spalte.addView(b.ueberschrift("Karte einlesen"), b.breit())
        spalte.addView(
            Button(gastgeber).apply {
                text = "Kartendatei auswählen"
                textSize = b.stil.textGroesse
                typeface = b.stil.textSchrift
                setTextColor(b.stil.text)
                background = b.randfeld()
                setPadding(b.stil.abstand, b.stil.abstand / 2, b.stil.abstand, b.stil.abstand / 2)
                setOnClickListener { leseKarteEin() }
            },
            b.breit(),
        )
        standfeld = b.nebentext(einlesestand ?: "").also { spalte.addView(it, b.breit()) }
        spalte.addView(
            b.nebentext(
                "Eine Karte kommt als Datei mit der Endung .czk ins Gerät. Hier wird sie in " +
                    "den Ordner der App kopiert und ihre Unterschrift geprüft. Danach die " +
                    "Karte einmal neu öffnen — die Kartenansicht liest ihren Bestand beim " +
                    "Aufbau. Der ganze Weg steht in der Anleitung.",
            ),
            b.breit(),
        )

        spalte.addView(b.ueberschrift("Darstellung"), b.breit())
        spalte.addView(
            Button(gastgeber).apply {
                text = if (sparmodusAn()) "Sparmodus ausschalten" else "Sparmodus einschalten"
                textSize = b.stil.textGroesse
                typeface = b.stil.textSchrift
                setTextColor(b.stil.text)
                background = b.randfeld()
                setPadding(b.stil.abstand, b.stil.abstand / 2, b.stil.abstand, b.stil.abstand / 2)
                setOnClickListener { sparmodusUmschalten() }
            },
            b.breit(),
        )
        spalte.addView(
            b.nebentext(
                "Der Sparmodus ist kein abgedunkeltes Aussehen, sondern eine eigene " +
                    "Darstellung: reines Schwarz, große Schrift, keine Zierde. Auf einem " +
                    "OLED-Bildschirm kostet ein schwarzer Bildpunkt keinen Strom.",
            ),
            b.breit(),
        )

        spalte.addView(b.trennstrich(), b.strichbreit())
        spalte.addView(b.ueberschrift("Kein Netzzugang"), b.breit())
        spalte.addView(b.fliesstext(berechtigungstext()), b.breit())
        spalte.addView(
            b.nebentext(
                "Diese Liste kommt vom Betriebssystem, nicht aus unserem Text. Dieselbe " +
                    "Angabe steht in den Systemeinstellungen unter „Apps“.",
            ),
            b.breit(),
        )

        spalte.addView(b.trennstrich(), b.strichbreit())
        spalte.addView(b.ueberschrift("Inhaltspaket"), b.breit())
        spalte.addView(b.fliesstext(paketangaben()), b.breit())
        spalte.addView(b.nebentext("SHA-256 der Paketdatei:"), b.breit())
        spalte.addView(b.kennzahl(pruefsummeInVierergruppen()), b.breit())
        spalte.addView(
            b.nebentext(
                "Diese Prüfsumme lässt sich mit einer vorher notierten vergleichen, ohne " +
                    "dass dafür ein Netz nötig ist.",
            ),
            b.breit(),
        )
        if (!Paketlader.istGeprueft(paket.verdict)) {
            spalte.addView(b.warnung(warnungZurSignatur()), b.breit())
        }

        spalte.addView(b.trennstrich(), b.strichbreit())
        spalte.addView(b.ueberschrift("Pakete austauschen"), b.breit())
        austausch.baue(b, spalte)

        spalte.addView(b.trennstrich(), b.strichbreit())
        spalte.addView(b.ueberschrift("Ist diese App noch die echte?"), b.breit())
        spalte.addView(
            b.fliesstext(
                "Unten steht der Fingerabdruck der Unterschrift, mit der DIESE " +
                    "Installation gebaut wurde. Vergleiche ihn mit dem, der zur Ausgabe " +
                    "veröffentlicht wurde: Stimmt er überein, ist die App unverändert; " +
                    "stimmt er nicht, stammt sie von jemand anderem.",
            ),
            b.breit(),
        )
        spalte.addView(b.nebentext("SHA-256 der Unterschrift dieser App:"), b.breit())
        spalte.addView(b.kennzahl(appFingerabdruck()), b.breit())
        spalte.addView(
            b.nebentext(
                "Der Vergleich braucht kein Netz: Es genügt, die Zeichenfolge einmal " +
                    "abgeschrieben oder abfotografiert zu haben. Die drei anderen " +
                    "Prüfungen und der Weg für eine neuere Ausgabe stehen in der " +
                    "Anleitung ganz oben.",
            ),
            b.breit(),
        )

        spalte.addView(b.trennstrich(), b.strichbreit())
        spalte.addView(b.ueberschrift("Über diese App"), b.breit())
        spalte.addView(b.fliesstext(appangaben()), b.breit())
        hinweistext()?.let {
            spalte.addView(b.trennstrich(), b.strichbreit())
            spalte.addView(b.ueberschrift(it.first), b.breit())
            spalte.addView(b.fliesstext(it.second), b.breit())
        }

        return ScrollView(gastgeber).apply {
            addView(spalte)
            setBackgroundColor(b.stil.hintergrund)
        }
    }

    // Nicht "wir fordern keine an", sondern was das System tatsaechlich
    // eingetragen hat. Steht hier je eine Zeile, ist die Behauptung widerlegt.
    private fun zeigeStand(text: String) {
        einlesestand = text
        standfeld?.text = text
    }

    /**
     * Holt eine Kartendatei ins Geraet.
     *
     * Der ganze Weg in einem Stueck: auswaehlen, kopieren, pruefen. Kopiert
     * wird auf einem eigenen Faden -- 432 MB ueber den Hauptfaden zu ziehen,
     * haelt die Oberflaeche eine halbe Minute an, und Android beendet eine
     * App, die zu lange nicht antwortet.
     *
     * Zuerst in eine Datei mit dem Zusatz ".teil" und erst danach umbenennen.
     * Wer waehrend des Kopierens abbricht, hat sonst eine halbe Karte im
     * Ordner, die beim naechsten Start als kaputt gemeldet wird -- oder,
     * schlimmer, als richtig gilt und Loecher hat.
     */
    private fun leseKarteEin() {
        val haupt = gastgeber as? MainActivity ?: return
        haupt.waehleDatei { quelle ->
            if (quelle == null) {
                zeigeStand("Abgebrochen.")
                return@waehleDatei
            }
            val ordner = Kartenlader.eigenerOrdner(gastgeber)
            if (ordner == null) {
                zeigeStand("Kein Ordner für eigene Karten verfügbar.")
                return@waehleDatei
            }
            val name = dateiname(quelle)
            zeigeStand("Lese „$name“ ein …")
            val zurueck = android.os.Handler(android.os.Looper.getMainLooper())
            Thread {
                val ziel = File(ordner, name)
                val teil = File(ordner, "$name.teil")
                val ergebnis = try {
                    var bytes = 0L
                    gastgeber.contentResolver.openInputStream(quelle).use { ein ->
                        requireNotNull(ein) { "Datei ließ sich nicht öffnen" }
                        teil.outputStream().use { aus ->
                            val puffer = ByteArray(1 shl 16)
                            while (true) {
                                val gelesen = ein.read(puffer)
                                if (gelesen <= 0) break
                                aus.write(puffer, 0, gelesen)
                                bytes += gelesen
                            }
                        }
                    }
                    if (ziel.exists() && !ziel.delete()) error("alte Datei ließ sich nicht ersetzen")
                    if (!teil.renameTo(ziel)) error("Datei ließ sich nicht ablegen")
                    val mb = bytes / 1_000_000.0
                    "„$name“ · %.0f MB · %s".format(mb, Kartenlader.pruefeKarte(ziel))
                } catch (fehler: Exception) {
                    teil.delete()
                    "Fehlgeschlagen: ${fehler.message ?: fehler::class.simpleName}"
                }
                zurueck.post { zeigeStand(ergebnis) }
            }.start()
        }
    }

    /**
     * Der Anzeigename der gewaehlten Datei, auf .czk endend.
     *
     * Der Verweis des Systems traegt keinen Pfad, sondern eine Kennung. Den
     * Namen muss man erfragen; kommt keiner, wird einer vergeben -- eine
     * Karte ohne Endung wuerde der Lader spaeter uebersehen.
     */
    private fun dateiname(quelle: android.net.Uri): String {
        var name: String? = null
        try {
            gastgeber.contentResolver.query(quelle, null, null, null, null)?.use { zeiger ->
                val spalte = zeiger.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (spalte >= 0 && zeiger.moveToFirst()) name = zeiger.getString(spalte)
            }
        } catch (fehler: Exception) {
            // Kein Name zu bekommen ist kein Grund abzubrechen.
        }
        val sauber = (name ?: "eingelesen.czk")
            .substringAfterLast('/')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (sauber.endsWith(".czk", ignoreCase = true)) sauber else "$sauber.czk"
    }

    private fun berechtigungstext(): String {
        val angefordert = try {
            @Suppress("DEPRECATION")
            gastgeber.packageManager
                .getPackageInfo(gastgeber.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                ?.toList()
                .orEmpty()
        } catch (fehler: PackageManager.NameNotFoundException) {
            return "Die Berechtigungen ließen sich nicht abfragen: ${fehler.message}"
        }
        if (angefordert.isEmpty()) {
            return "Diese App hat keine einzige Berechtigung angefordert — auch nicht die " +
                "für den Internetzugang. Ohne sie kann sie auf Betriebssystemebene nicht " +
                "ins Netz."
        }
        // Die Liste kommt vom System. Der Satz darunter sagt, was NICHT drinsteht
        // -- das ist die Angabe, auf die es ankommt, und sie laesst sich an
        // derselben Liste nachpruefen.
        return "Angeforderte Berechtigungen:\n" + angefordert.joinToString("\n") { "· $it" } +
            "\n\nNicht dabei ist die für den Internetzugang. Ohne sie kann die App auf " +
            "Betriebssystemebene nicht ins Netz — und ebenso wenig steht hier ein " +
            "Ortungsrecht, eine Kamera oder ein Mikrofon. Wofür Bluetooth da ist, steht " +
            "in der Anleitung ganz oben."
    }

    private fun paketangaben(): String {
        val m = paket.pack.manifest
        val teile = mutableListOf(
            "${m.title} (${m.id}, Fassung ${m.version}, Sprache ${m.language})",
            Paketlader.urteilstext(paket.verdict),
        )
        val bestand = mutableListOf<String>()
        if (paket.pack.tips.isNotEmpty()) bestand += "${paket.pack.tips.size} Tipps"
        if (paket.pack.guides.isNotEmpty()) bestand += "${paket.pack.guides.size} Bauanleitungen"
        if (paket.pack.agriculture.isNotEmpty()) bestand += "${paket.pack.agriculture.size} Kapitel"
        if (paket.pack.pois.isNotEmpty()) bestand += "${paket.pack.pois.size} Kartenpunkte"
        val saetze = paket.pack.phrases.sumOf { it.phrases.size }
        if (saetze > 0) {
            bestand += "$saetze Übersetzer-Sätze in ${paket.pack.phraseLanguages.size} Sprachen"
        }
        teile += bestand.joinToString(", ")
        teile += "${paket.datei.length() / 1024} kB"
        return teile.joinToString("\n")
    }

    private fun warnungZurSignatur(): String = when (paket.verdict) {
        is PackVerdict.Trusted -> ""
        is PackVerdict.UnknownSigner ->
            "Dieses Paket ist mit einem Schlüssel unterschrieben, den diese App nicht " +
                "kennt. Die Angaben darin sind ungeprüft."

        else ->
            "Die Unterschrift dieses Pakets hält nicht. Verlass dich nicht auf die " +
                "Angaben darin."
    }

    private fun appangaben(): String {
        val fassung = try {
            @Suppress("DEPRECATION")
            gastgeber.packageManager.getPackageInfo(gastgeber.packageName, 0).versionName
        } catch (fehler: PackageManager.NameNotFoundException) {
            null
        }
        return listOfNotNull(
            fassung?.let { "Fassung $it" },
            "Offline-Nachschlagewerk. Kein Netzverkehr, keine Datensammlung, keine " +
                "Diagnosedaten — unter keinen Umständen.",
        ).joinToString("\n")
    }

    // Der Haftungshinweis liegt als Tipp im Paket. Er gehoert an eine Stelle, an
    // der man ihn findet, ohne nach ihm zu suchen.
    private fun hinweistext(): Pair<String, String>? =
        paket.pack.tips.firstOrNull { it.category == "hinweis" }?.let { it.title to it.body }

    /**
     * Der SHA-256-Fingerabdruck der Unterschrift, mit der DIESE Installation
     * gebaut wurde.
     *
     * Es ist dieselbe Zahl, die `apksigner verify --print-certs` als
     * "certificate SHA-256 digest" ausgibt -- damit laesst sich das, was auf
     * dem Geraet liegt, gegen das vergleichen, was veroeffentlicht wurde.
     *
     * Zwei Wege, weil die alte Abfrage seit Android 9 abgeloest ist und die
     * App bis Android 5 hinunter laeuft. Faellt beides aus, steht hier ein
     * Satz statt einer erfundenen Zahl: Eine Pruefsumme, die nicht stimmt,
     * waere schlimmer als keine.
     */
    private fun appFingerabdruck(): String {
        val roh = try {
            val pm = gastgeber.packageManager
            val name = gastgeber.packageName
            val unterschriften: Array<android.content.pm.Signature>? =
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    val info = pm.getPackageInfo(name, PackageManager.GET_SIGNING_CERTIFICATES)
                    val sig = info.signingInfo
                    when {
                        sig == null -> null
                        sig.hasMultipleSigners() -> sig.apkContentsSigners
                        else -> sig.signingCertificateHistory
                    }
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(name, PackageManager.GET_SIGNATURES).signatures
                }
            val erste = unterschriften?.firstOrNull() ?: return "nicht feststellbar"
            MessageDigest.getInstance("SHA-256").digest(erste.toByteArray())
                .joinToString("") { "%02x".format(it) }
        } catch (fehler: Exception) {
            return "nicht feststellbar"
        }
        return roh.chunked(4).chunked(8).joinToString("\n") { it.joinToString(" ") }
    }

    private fun pruefsummeInVierergruppen(): String {
        val roh = pruefsumme ?: berechnePruefsumme(paket.datei).also { pruefsumme = it }
        return roh.chunked(4).chunked(8).joinToString("\n") { it.joinToString(" ") }
    }

    private companion object {

        fun berechnePruefsumme(datei: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            datei.inputStream().use { strom ->
                val puffer = ByteArray(64 * 1024)
                while (true) {
                    val gelesen = strom.read(puffer)
                    if (gelesen <= 0) break
                    digest.update(puffer, 0, gelesen)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
