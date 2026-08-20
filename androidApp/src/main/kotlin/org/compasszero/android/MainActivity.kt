package org.compasszero.android

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var paket: GeladenesPaket
    private lateinit var gemerkt: Gemerkt
    private var stil = Stil.normal(1f)
    private var sparmodus = false
    private var aktiv = 0
    // Ob die App gerade vorn ist. Gebraucht wird das nur an einer Stelle: Das
    // Paket ist meist erst NACH onResume geladen, und was dort haette
    // anlaufen sollen, muss dann nachgeholt werden -- aber nur dann.
    private var sichtbar = false
    // Der allererste Aufbau geht nicht auf: Beim Start soll die App dastehen,
    // nicht erscheinen.
    private var bereitsGebaut = false

    private lateinit var bereiche: List<Bereich>
    private lateinit var rahmen: FrameLayout
    private lateinit var wurzel: LinearLayout
    private lateinit var marke: View
    private lateinit var inhalt: FrameLayout

    // Das Laden gehoert NICHT auf den Hauptfaden. Es kopiert das Paket aus den
    // Beigaben, bildet eine Pruefsumme darueber, prueft die Unterschrift und
    // zerlegt gut ein Megabyte JSON. Solange das in onCreate stand, wuchs die
    // Startzeit mit jedem Kapitel, und auf einem lange laufenden Emulator wurde
    // daraus reproduzierbar ein "App reagiert nicht" -- Android schlaegt Alarm,
    // wenn der Hauptfaden fuenf Sekunden nicht antwortet.
    override fun onCreate(zustand: Bundle?) {
        super.onCreate(zustand)
        // VOR dem ersten Stil: Wer den Sparmodus eingeschaltet hat, soll ihn
        // nicht erst nach dem Laden bekommen. Ein kurz aufblitzender heller
        // Bildschirm ist im Dunkeln kein Schoenheitsfehler.
        gemerkt = Gemerkt(this)
        sparmodus = gemerkt.sparmodus
        stil = if (sparmodus) {
            Stil.sparmodus(resources.displayMetrics.density)
        } else {
            Stil.normal(resources.displayMetrics.density)
        }
        setContentView(ladeanzeige())

        // Der Anwendungskontext, nicht diese Activity: Der Faden ueberlebt sonst
        // eine Drehung des Geraets und haelt einen toten Bildschirm fest.
        val anwendung = applicationContext
        val uebernommen = gemerkt.eigenesPaket
        Thread({
            val geladen = Paketlader.laden(anwendung, uebernommen)
            runOnUiThread { uebernimm(geladen) }
        }, "paketlader").start()
    }

    private fun uebernimm(geladen: Result<GeladenesPaket>) {
        // Waehrend geladen wurde, kann die Activity beendet worden sein. Dann
        // gibt es nichts mehr zu bauen, und setContentView wuerde auf einem
        // Fenster arbeiten, das es nicht mehr gibt.
        if (isFinishing) return
        val fehler = geladen.exceptionOrNull()
        if (fehler != null) {
            setContentView(fehleranzeige(fehler.message ?: "Paket ließ sich nicht laden"))
            return
        }
        paket = geladen.getOrThrow()
        // Ein uebernommenes Paket, das beim Laden durchgefallen ist, wird
        // vergessen. Ohne diese Zeile versucht die App es bei jedem Start
        // erneut und faellt jedes Mal still zurueck -- der Nutzer saehe nie,
        // dass etwas nicht stimmt.
        if (gemerkt.eigenesPaket.isNotBlank() && !paket.vonAussen) gemerkt.eigenesPaket = ""

        bereiche = listOf(
            Lexikon(this, paket, ::zeigeMarke),
            Karte(this, gemerkt),
            Uebersetzer(this, paket) { baueOberflaeche() },
            Einstellungen(this, paket, { sparmodus }, ::sparmodusUmschalten, gemerkt) {
                baueOberflaeche()
            },
        )

        wurzel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
        }
        // Der aeussere Rahmen haelt Status- und Navigationsleiste frei. Ohne ihn
        // liegt die Ueberschrift unter der Uhr und die Bereichsleiste unter den
        // Systemtasten -- beides auf dem Geraet sofort sichtbar.
        rahmen = FrameLayout(this).apply {
            fitsSystemWindows = true
            addView(wurzel, FrameLayout.LayoutParams(MATCH, MATCH))
        }
        setContentView(rahmen)
        baueOberflaeche()
        // Das Laden endet in aller Regel NACH onResume. Wer dort einen Sensor
        // anwirft, kaeme also nie dran -- deshalb hier nachholen, aber nur,
        // wenn die App gerade wirklich vorn ist.
        if (sichtbar) bereiche[aktiv].fortsetzen()
    }

    // Was waehrend des Ladens auf dem Bildschirm steht. Es behauptet NICHTS
    // ueber das Paket -- die Kopfzeile sagt spaeter, ob die Unterschrift haelt,
    // und bis dahin ist jede Aussage darueber ungedeckt.
    private fun ladeanzeige(): View {
        val text = TextView(this).apply {
            text = "Inhalt wird geprüft …"
            textSize = 16f
            typeface = stil.textSchrift
            setTextColor(stil.gedaempft)
            gravity = Gravity.CENTER
        }
        return FrameLayout(this).apply {
            setBackgroundColor(stil.hintergrund)
            addView(
                text,
                FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER),
            )
        }
    }

    fun sparmodusUmschalten() {
        sparmodus = !sparmodus
        gemerkt.sparmodus = sparmodus
        stil = if (sparmodus) {
            Stil.sparmodus(resources.displayMetrics.density)
        } else {
            Stil.normal(resources.displayMetrics.density)
        }
        baueOberflaeche()
    }

    private fun baueOberflaeche() {
        val b = Bausteine(this, stil, sparmodus)
        wurzel.removeAllViews()
        wurzel.setBackgroundColor(stil.hintergrund)
        wurzel.setPadding(stil.abstand, stil.abstand, stil.abstand, 0)
        rahmen.setBackgroundColor(stil.hintergrund)

        marke = kopfzeile(b)
        wurzel.addView(marke, vollbreit())
        wurzel.addView(herkunftszeile(), b.breit())

        inhalt = FrameLayout(this)
        wurzel.addView(inhalt, b.dehnbar())
        inhalt.addView(bereiche[aktiv].baue(b))
        if (bereitsGebaut) b.aufgehen(inhalt)
        bereitsGebaut = true

        wurzel.addView(b.trennstrich(), b.strichbreit())
        wurzel.addView(bereichsleiste(b), b.breit())
    }

    // Ein Tipp auf den schon aktiven Bereich wechselt nicht, sondern setzt ihn
    // zurueck -- sonst fuehrt aus dem Lexikon kein Weg zu den Kacheln, ausser
    // wiederholt die Zurueck-Taste zu druecken.
    private fun wechsle(nummer: Int) {
        if (nummer == aktiv) {
            bereiche[nummer].aufAnfang()
        } else {
            // Der verlassene Bereich haelt an, was er laufen laesst. Sonst
            // misst der Kompass weiter, waehrend jemand im Lexikon liest.
            bereiche[aktiv].anhalten()
            aktiv = nummer
        }
        baueOberflaeche()
    }

    // Im Hintergrund laeuft nichts weiter. Das ist bei einer App, die im
    // Ernstfall tagelang auf demselben Akku halten soll, keine Feinheit.
    override fun onPause() {
        super.onPause()
        sichtbar = false
        if (::bereiche.isInitialized) bereiche[aktiv].anhalten()
    }

    override fun onResume() {
        super.onResume()
        sichtbar = true
        if (::bereiche.isInitialized) bereiche[aktiv].fortsetzen()
    }

    fun zeigeMarke(sichtbar: Boolean) {
        marke.visibility = if (sichtbar) View.VISIBLE else View.GONE
    }

    // Die Wortmarke ist Zierde, kein Bedienelement -- im Sparmodus weicht sie
    // dem blossen Schriftzug. Das Punktraster "ZERO" liegt als Vektorgrafik vor;
    // "Compass" setzt die Marke selbst als Serifentext, also tun wir es auch.
    //
    // Im Normalmodus steht die Marke in einer eigenen Flaeche, die bis an beide
    // Blattraender und bis unter die Statusleiste laeuft, und darunter ein
    // Streifen aus demselben Punktraster wie die Marke. Beides zusammen ist die
    // Kopfzone: eine Marke ueber einem Trennstrich ist noch keine.
    private fun kopfzeile(b: Bausteine): View {
        if (sparmodus) {
            return TextView(this).apply {
                text = "COMPASS ZERO"
                textSize = stil.titelGroesse
                typeface = stil.ueberschriftSchrift
                setTextColor(stil.text)
            }
        }
        val block = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val band = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Linksbuendig, nicht mittig: Die organische Flaeche kommt von
            // rechts herein, und eine mittige Marke laege darunter. Am
            // 29.07. hat die Form die Wortmarke verschluckt.
            gravity = Gravity.START
            setPadding(stil.abstand, stil.abstand, stil.abstand, stil.abstand * 3 / 4)
            // ALS HINTERGRUND, NICHT ALS VORDERGRUND. android.view.View zeichnet
            // den Vordergrund in onDrawForeground(), und das laeuft NACH
            // dispatchDraw() -- der Vordergrund liegt also UEBER den Kindern. Am
            // 29.07. hat deshalb das Punktraster die Wortmarke "ZERO"
            // zugedeckt, bis sie nicht mehr lesbar war. Der Hintergrund wird
            // vor den Kindern gezeichnet und ist die richtige Ebene.
            // Die Grundfarbe steckt als unterste Lage mit drin, weil ein
            // setBackgroundColor den Hintergrund sonst ersetzen wuerde.
            background = LayerDrawable(
                arrayOf(ColorDrawable(stil.flaeche), Kopfgrafik(stil.marke, stil.raster)),
            )
        }
        band.addView(
            TextView(this).apply {
                text = "Compass"
                textSize = stil.titelGroesse
                typeface = stil.ueberschriftSchrift
                setTextColor(stil.text)
                gravity = Gravity.START
            },
        )
        val hoehe = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            stil.titelGroesse * 1.35f,
            resources.displayMetrics,
        ).toInt()
        band.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.wortmarke_zero)
                adjustViewBounds = true
            },
            LinearLayout.LayoutParams(WRAP, hoehe),
        )
        block.addView(band, LinearLayout.LayoutParams(MATCH, WRAP))
        block.addView(b.rasterstreifen(7), b.rasterbreit(7))
        return block
    }

    // Zieht ein Kind ueber die Innenraender der Wurzel hinaus bis an den
    // Bildschirmrand. Die Kopfzone soll ein Blattkopf sein und keine Karte, die
    // im Textspiegel schwimmt.
    private fun vollbreit() = LinearLayout.LayoutParams(MATCH, WRAP).apply {
        if (sparmodus) return@apply
        leftMargin = -stil.abstand
        rightMargin = -stil.abstand
        topMargin = -stil.abstand
        bottomMargin = stil.abstand / 2
    }

    // Woher der Inhalt kommt, steht auf jedem Bildschirm. Haelt die Unterschrift
    // nicht, faellt die Zeile in den Signalton -- sie ist dann keine Fussnote
    // mehr, sondern eine Warnung.
    private fun herkunftszeile(): View {
        val geprueft = Paketlader.istGeprueft(paket.verdict)
        val roh = Paketlader.urteilstext(paket.verdict) + "  ·  " + bestandstext()
        return TextView(this).apply {
            // Feste Zeichenbreite und weite Abstaende: Die Zeile ist ein Befund
            // ueber das Paket, kein Satz. Haelt die Unterschrift nicht, faellt
            // sie in den Signalton und ist keine Fussnote mehr, sondern eine
            // Warnung.
            text = if (sparmodus) roh else roh.uppercase()
            textSize = if (sparmodus) 15f else 11f
            typeface = if (sparmodus) stil.textSchrift else Typeface.MONOSPACE
            if (!sparmodus) letterSpacing = 0.06f
            setTextColor(if (geprueft) stil.gedaempft else stil.signal)
            setPadding(0, 0, 0, stil.abstand / 2)
            setOnClickListener { wechsle(bereiche.indexOfFirst { it is Einstellungen }) }
        }
    }

    private fun bestandstext(): String {
        val anzahl = paket.pack.tips.size + paket.pack.guides.size + paket.pack.agriculture.size
        return if (anzahl == 1) "1 Eintrag" else "$anzahl Einträge"
    }

    // Vier Bereiche stehen im Entwurf fest. Was noch nicht gebaut ist, ist hier
    // sichtbar und ausdruecklich gekennzeichnet, statt etwas vorzutaeuschen. Der
    // aktive Bereich hebt sich klar ab -- groesseres Icon, fette Schrift; alles
    // andere tritt zurueck, "noch nicht gebaut" ist nur eine winzige Fussnote.
    private fun bereichsleiste(b: Bausteine): View {
        val leiste = b.reihe().apply {
            setPadding(0, stil.abstand / 2, 0, stil.abstand / 2)
        }
        bereiche.forEachIndexed { nummer, bereich ->
            val hier = nummer == aktiv
            val knopf = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                setOnClickListener { wechsle(nummer) }
                layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
                b.antippbewegung(this)
            }
            knopf.addView(
                ImageView(this).apply {
                    setImageDrawable(b.symbol(bereich.bild, if (hier) 27 else 22, !hier))
                },
            )
            knopf.addView(
                TextView(this).apply {
                    text = bereich.name
                    textSize = if (sparmodus) 13f else 11f
                    gravity = Gravity.CENTER
                    typeface = if (hier) Typeface.create(stil.textSchrift, Typeface.BOLD) else stil.textSchrift
                    setTextColor(if (hier) stil.text else stil.gedaempft)
                    setPadding(0, stil.abstand / 4, 0, 0)
                },
            )
            if (!bereich.gebaut) {
                knopf.addView(
                    TextView(this).apply {
                        text = "noch nicht gebaut"
                        textSize = if (sparmodus) 9f else 8f
                        gravity = Gravity.CENTER
                        typeface = stil.textSchrift
                        setTextColor(stil.gedaempft)
                    },
                )
            }
            leiste.addView(knopf)
        }
        return leiste
    }

    /**
     * Die Flaeche fuer Ebenen, die ueber allem liegen -- heute die Bildlupe.
     *
     * SIE MUSS DER RAHMEN SEIN und nicht `android.R.id.content`: Der Rahmen
     * ist der View mit `fitsSystemWindows`, und er VERBRAUCHT die Randmasse
     * der Status- und Navigationsleiste. Eine Ebene daneben bekommt davon
     * nichts mehr ab und liegt dann unter der Uhr und den Systemtasten -- am
     * 04.08.2026 genau so im Bildschirmfoto gesehen, nachdem die Lupe zuerst
     * an `content` hing.
     */
    /**
     * Die Dateiauswahl des Systems fuer eine Karte.
     *
     * MainActivity ist eine schlichte Activity, keine ComponentActivity --
     * der neue Ergebnis-Weg von AndroidX steht hier nicht zur Verfuegung.
     * Deshalb der klassische Weg ueber startActivityForResult, und die
     * Rueckmeldung liegt so lange in einem Feld.
     *
     * ACTION_OPEN_DOCUMENT und nicht GET_CONTENT: Nur das erste gibt einen
     * dauerhaft lesbaren Verweis auf eine Datei, die auch auf einer SD-Karte
     * oder in der Ablage eines anderen Programms liegen darf. Eine
     * Berechtigung braucht es dafuer nicht -- der Nutzer waehlt selbst, und
     * genau diese Datei wird freigegeben.
     */
    private var beiDatei: ((android.net.Uri?) -> Unit)? = null

    fun waehleDatei(beiErgebnis: (android.net.Uri?) -> Unit) {
        beiDatei = beiErgebnis
        val absicht = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            // Kein enger Typ: .czk kennt kein Geraet, und eine Auswahl, in der
            // die gesuchte Datei ausgegraut ist, ist schlimmer als eine, in
            // der zu viel steht.
            type = "*/*"
        }
        try {
            startActivityForResult(absicht, WAHL_DATEI)
        } catch (fehler: android.content.ActivityNotFoundException) {
            beiDatei = null
            beiErgebnis(null)
        }
    }

    override fun onActivityResult(anfrage: Int, ergebnis: Int, daten: android.content.Intent?) {
        super.onActivityResult(anfrage, ergebnis, daten)
        if (anfrage != WAHL_DATEI) return
        val rueckmeldung = beiDatei
        beiDatei = null
        rueckmeldung?.invoke(if (ergebnis == RESULT_OK) daten?.data else null)
    }

    fun ueberlagerung(): ViewGroup = rahmen

    /**
     * Erfragt eine Laufzeit-Berechtigung und meldet das Ergebnis zurueck.
     *
     * MainActivity ist eine schlichte Activity, kein ComponentActivity -- der
     * neue Ergebnis-Weg von AndroidX steht hier nicht zur Verfuegung. Deshalb
     * derselbe klassische Weg wie bei der Dateiauswahl, mit der Rueckmeldung
     * in einem Feld.
     */
    private var beiBerechtigung: ((Boolean) -> Unit)? = null

    fun frageBerechtigung(recht: String, beiErgebnis: (Boolean) -> Unit) {
        // Bis Android 5.1 gibt es kein Fragen: Was im Manifest steht, ist beim
        // Installieren erteilt. "Erteilt" ist hier also die Wahrheit und nicht
        // eine Annahme -- und requestPermissions gibt es dort noch gar nicht.
        if (android.os.Build.VERSION.SDK_INT < 23) {
            beiErgebnis(true)
            return
        }
        beiBerechtigung = beiErgebnis
        requestPermissions(arrayOf(recht), WAHL_RECHT)
    }

    override fun onRequestPermissionsResult(
        anfrage: Int,
        rechte: Array<out String>,
        ergebnisse: IntArray,
    ) {
        super.onRequestPermissionsResult(anfrage, rechte, ergebnisse)
        if (anfrage != WAHL_RECHT) return
        val rueckmeldung = beiBerechtigung
        beiBerechtigung = null
        val erteilt = ergebnisse.isNotEmpty() &&
            ergebnisse[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
        rueckmeldung?.invoke(erteilt)
    }

    // Der Nachfolger dieser Methode gibt es erst ab Android 13; die App laeuft ab
    // Android 5. Bis der Mindeststand steigt, ist das hier der Weg.
    @Deprecated("Ab Android 13 ersetzt durch OnBackInvokedCallback")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when {
            // Solange geladen wird, gibt es noch keine Bereiche. Ohne diese
            // Zeile stuerzt die Zurueck-Taste waehrend des Ladens ab.
            !::bereiche.isInitialized -> super.onBackPressed()
            bereiche[aktiv].zurueck() -> Unit
            aktiv != 0 -> wechsle(0)
            else -> super.onBackPressed()
        }
    }

    private fun fehleranzeige(meldung: String): View = TextView(this).apply {
        text = "Das Inhaltspaket ließ sich nicht öffnen.\n\n$meldung"
        setPadding(48, 96, 48, 48)
        textSize = 18f
    }

    private companion object {
        // Kennzahl der Dateiauswahl. Beliebig, aber eindeutig.
        const val WAHL_DATEI = 4711

        // Kennzahl der Berechtigungsabfrage. Ebenso beliebig, ebenso eindeutig.
        const val WAHL_RECHT = 4712

        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }

}
