package org.compasszero.android

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.hardware.SensorManager
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// Der Kartenbereich: die Offline-Karte und darunter der Kompass. Beide
// brauchen weder Ortung noch Berechtigung noch Netz -- die Karte liegt als
// Datei im Geraet, die Richtung kommt aus dem Magnetfeldsensor.
class Karte(private val gastgeber: Activity, private val gemerkt: Gemerkt) : Bereich {

    override val name = "Karte"
    override val bild = R.drawable.sym_karte

    override val gebaut = true

    private val kompass = Kompass(gastgeber.getSystemService(Context.SENSOR_SERVICE) as SensorManager)

    private lateinit var b: Bausteine
    private var rose: Rose? = null
    private var anzeige: TextView? = null
    private var hinweis: TextView? = null
    private var blatt: Kartenblatt? = null
    private var standzeile: TextView? = null

    /**
     * Ob die ruhige Karte gewaehlt ist -- gehoert HIERHER und nicht ins
     * Kartenblatt.
     *
     * Das Blatt wird bei jedem Reiterwechsel neu gebaut; ein Schalter, der
     * dort sitzt, steht danach wieder auf aus. Genau das ist beim ersten
     * Anlauf passiert: "Ruhig" einschalten, ins Lexikon, zurueck -- und die
     * Karte war wieder voll. Dieses Bereichsobjekt lebt dagegen so lange wie
     * die App.
     *
     * Seit dem 17.08.2026 haelt der Schalter auch einen NEUSTART aus -- der
     * Anfangswert kommt aus `Gemerkt`, nicht mehr aus dem Quelltext.
     */
    private var ruhigGewaehlt = gemerkt.ruhigeKarte

    /** Ob das Satellitenbild gezeigt wird -- wie "Ruhig" ueber den Neustart hinweg. */
    private var bilderGewaehlt = gemerkt.satellitenbild

    /**
     * Ob die Ebenenschalter ausgeklappt sind.
     *
     * Zugeklappt beim Start: Wer die Karte oeffnet, will die Karte sehen und
     * nicht acht Schalter. Der Zustand lebt so lange wie dieser Bereich, wird
     * aber ABSICHTLICH NICHT ueber den Neustart gemerkt -- er ist eine
     * Handbewegung und keine Einstellung.
     */
    private var ebenenOffen = false

    /**
     * Start und Ziel einer Route -- Knotennummern im Wegenetz.
     *
     * Sie leben so lange wie dieser Bereich und nicht wie das Blatt: Wer ins
     * Lexikon wechselt und zurueckkommt, soll seine Route noch haben.
     */
    /**
     * Welche Gruppen der Zeichnung ausgeschaltet sind.
     *
     * Ueber den NAMEN und nicht ueber die Sortennummern: Kommt eine Sorte
     * dazu, gehoert sie zu einer Gruppe und nicht zu einer Zahl in einer
     * Ablage.
     */
    private val weggelasseneGruppen: MutableSet<String> by lazy {
        zeichnungsgruppen().keys.filterNot { gemerkt.zeichnungAn(it) }.toMutableSet()
    }

    private fun zeichnungsgruppen(): Map<String, Set<Int>> = linkedMapOf(
        "Straßen und Wege" to Kachelmaler.WEGE,
        "Gewässer" to Kachelmaler.GEWAESSER,
        "Grenzen" to Kachelmaler.GRENZEN,
        "Flächen (Wald, Siedlung)" to Kachelmaler.FLAECHEN,
    )

    private var routeVon: Int? = null
    private var routeNach: Int? = null

    /** Setzt die Zahl im Ebenenknopf neu, wenn ein Schalter umgelegt wurde. */
    private var ebenenNeuZeichnen: (() -> Unit)? = null

    override fun baue(b: Bausteine): View {
        this.b = b
        val spalte = b.spalte().apply { setPadding(0, b.stil.abstand, 0, b.stil.abstand) }

        baueKarte(b, spalte)

        spalte.addView(b.ueberschrift("Kompass"), b.breit())

        if (!b.sparmodus) {
            rose = Rose(gastgeber, b).also {
                spalte.addView(
                    it,
                    LinearLayout.LayoutParams(Bausteine.MATCH, (200 * b.dichte).toInt()).apply {
                        topMargin = b.stil.abstand / 2
                        bottomMargin = b.stil.abstand / 2
                    },
                )
            }
        }

        anzeige = TextView(gastgeber).apply {
            textSize = if (b.sparmodus) 52f else 40f
            typeface = b.stil.ueberschriftSchrift
            setTextColor(b.stil.text)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        spalte.addView(anzeige, b.breit())

        hinweis = TextView(gastgeber).apply {
            textSize = b.stil.textGroesse * 0.9f
            typeface = b.stil.textSchrift
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, b.stil.abstand / 2, 0, b.stil.abstand)
        }
        spalte.addView(hinweis, b.breit())

        spalte.addView(b.rasterstreifen(), b.rasterbreit().apply { bottomMargin = b.stil.abstand })

        // Die Fehlweisung steht dauerhaft da und nicht in einem Wegklick-Kasten.
        // Ein Kompass, der falsch zeigt, sieht aus wie einer, der richtig zeigt.
        spalte.addView(
            b.fliesstext(
                "Der Kompass misst das Erdmagnetfeld. Metall, Fahrzeuge, Elektronik, " +
                    "Kopfhörer und Magnetverschlüsse an Taschen lenken ihn ab — oft um " +
                    "viele Grad, ohne dass man es der Anzeige ansieht. Halte das Gerät " +
                    "flach und geh ein paar Schritte von allem Metallischen weg. Springt " +
                    "die Nadel oder steht hier „ungenau“, beschreibe mit dem Gerät " +
                    "langsam eine liegende Acht in der Luft; das stellt den Sensor neu ein.",
            ),
            b.breit(),
        )
        spalte.addView(
            b.fliesstext(
                "Angezeigt wird die missweisende Nordrichtung, also die zum magnetischen " +
                    "Nordpol. Die weicht je nach Ort von der Kartennordrichtung ab. In " +
                    "Mitteleuropa ist der Unterschied klein, in hohen Breiten groß.",
            ),
            b.breit(),
        )

        zeichneStand()
        kompass.beiAenderung = { gastgeber.runOnUiThread { zeichneStand() } }
        kompass.starten(b.sparmodus)

        return ScrollView(gastgeber).apply {
            addView(spalte)
            setBackgroundColor(b.stil.hintergrund)
        }
    }

    /**
     * Baut den Kartenteil -- und laesst die Oberflaeche dabei nicht stehen.
     *
     * WARUM DAS NOETIG IST: Beim Oeffnen einer unterschriebenen Karte wird die
     * ganze Datei durchgerechnet, sonst laesst sich die Unterschrift nicht
     * pruefen. Bei einer Europakarte und einem Detailpaket sind das zusammen
     * 783 MB. Das lief hier auf dem Faden der Bedienoberflaeche, und die App
     * stand so lange still -- Max am 06.08.2026: "die karte braucht sehr lange
     * zum laden und laesst die app sehr langsam wirken."
     *
     * Beim zweiten Mal ist es umsonst, weil der Kartenlader die geoeffnete
     * Karte merkt; dann wird sie sofort gezeigt und nichts blinkt auf. Nur der
     * erste Aufbau nach dem Programmstart wartet -- und sagt jetzt auch, worauf.
     */
    private fun baueKarte(b: Bausteine, spalte: LinearLayout) {
        spalte.addView(b.ueberschrift("Karte"), b.breit())

        val behaelter = b.spalte()
        spalte.addView(behaelter, b.breit())

        val schonDa = Kartenlader.bereitsGeprueft(gastgeber)
        if (schonDa != null) {
            fuelleKarte(b, behaelter, schonDa)
            return
        }

        behaelter.addView(
            b.nebentext("Karte wird geprüft — bei großen Karten dauert das einen Moment."),
            b.breit(),
        )
        Thread {
            val geladen = Kartenlader.laden(gastgeber)
            gastgeber.runOnUiThread {
                // Wer den Reiter inzwischen verlassen hat, bekommt nichts mehr
                // angehaengt -- die Ansicht gehoert dann nicht mehr zum Bild.
                if (behaelter.parent == null) return@runOnUiThread
                behaelter.removeAllViews()
                fuelleKarte(b, behaelter, geladen)
            }
        }.also { it.isDaemon = true }.start()
    }

    private fun fuelleKarte(
        b: Bausteine,
        spalte: LinearLayout,
        geladen: Result<Kartenlader.GeladeneKarte>,
    ) {
        val karte = geladen.getOrNull()
        if (karte == null) {
            // Eine fehlende Karte ist kein Absturz und kein leerer Kasten: Der
            // Rest des Bereichs -- der Kompass -- traegt weiter, und hier steht,
            // was fehlt und wohin die Datei gehoert.
            spalte.addView(
                b.warnung(
                    "Keine Kartendatei gefunden. " +
                        (geladen.exceptionOrNull()?.message ?: ""),
                ),
                b.breit(),
            )
            val ordner = Kartenlader.eigenerOrdner(gastgeber)
            if (ordner != null) {
                spalte.addView(
                    b.nebentext("Eine eigene Karte (.czk) gehört hierher:\n$ordner"),
                    b.breit(),
                )
            }
            return
        }

        val kartenstil = if (b.sparmodus) {
            Kartenstil.sparmodus(b.dichte)
        } else {
            Kartenstil.normal(b.dichte)
        }
        val neuesBlatt = Kartenblatt(gastgeber, karte.dateien, kartenstil, b.dichte, karte.hoehen, karte.bilder)
        blatt = neuesBlatt

        // 0,46 statt 0,52: Seit der Ebenenleiste stehen ZWEI Knopfzeilen unter
        // der Karte. Mit 0,52 lag die zweite unter dem Bildrand und war nur
        // durch Scrollen zu finden -- ein Schalter, den man nicht sieht,
        // ist kein Schalter.
        val hoehe = (gastgeber.resources.displayMetrics.heightPixels * 0.46f).toInt()

        // Der Merkknopf liegt AUF der Karte, nicht in der Knopfzeile darunter.
        // In der Zeile waere er der fuenfte, und bei fuenf Knoepfen schneidet
        // Android die Beschriftungen ab -- gemessen wurde "NORD" ohne Pfeil und
        // "MERKE" ohne N. Das ist derselbe Platzmangel, der hier schon
        // "NORDUN / G" erzeugt hat, nur diesmal als Beschnitt statt Umbruch.
        // Auf der Karte stoert er nichts, ist immer sichtbar und kann so lang
        // beschriftet sein, wie er sein muss.
        val kartenfeld = android.widget.FrameLayout(gastgeber)
        kartenfeld.addView(
            neuesBlatt,
            android.widget.FrameLayout.LayoutParams(Bausteine.MATCH, Bausteine.MATCH),
        )
        spalte.addView(kartenfeld, LinearLayout.LayoutParams(Bausteine.MATCH, hoehe))

        val leiste = b.reihe().apply {
            setPadding(0, b.stil.abstand / 2, 0, 0)
        }
        leiste.addView(knopf(b, "−") { neuesBlatt.zoomeUm(-1f) })
        leiste.addView(knopf(b, "+") { neuesBlatt.zoomeUm(1f) })
        leiste.addView(knopf(b, "Ganz") { neuesBlatt.zeigeAufGanzeKarte() })
        // Kurze Beschriftung: "Nordung" lief auf dem Emulator ueber zwei
        // Zeilen und wurde in der Mitte getrennt ("NORDUN / G").
        val drehknopf = knopf(b, "Nord ↑") { }
        drehknopf.setOnClickListener {
            neuesBlatt.drehtMitBlickrichtung = !neuesBlatt.drehtMitBlickrichtung
            drehknopf.text = if (neuesBlatt.drehtMitBlickrichtung) "Blick ↑" else "Nord ↑"
            zeichneStand()
        }
        leiste.addView(drehknopf)
        spalte.addView(leiste, b.breit())

        val merkknopf = Button(gastgeber).apply {
            text = "Merken"
            maxLines = 1
            textSize = b.stil.textGroesse * 0.85f
            typeface = b.stil.textSchrift
            setTextColor(b.stil.text)
            val rand = (8 * b.dichte).toInt()
            layoutParams = android.widget.FrameLayout.LayoutParams(
                Bausteine.WRAP,
                Bausteine.WRAP,
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = rand
                marginEnd = rand
            }
            sparknopf(this, b)
        }
        kartenfeld.addView(merkknopf)

        // Die Merkleiste steht NICHT als dritte Zeile da, sondern tritt an die
        // Stelle der Ebenenleiste. Unter der Karte ist Platz fuer genau zwei
        // Knopfzeilen; eine dritte lag beim Ebenen-Einbau schon einmal unter
        // dem Bildrand und war nur durch Scrollen zu finden.
        val merkleiste = b.reihe().apply {
            setPadding(0, b.stil.abstand / 4, 0, 0)
            visibility = View.GONE
        }

        // Zweite Leiste: die Ebenen. Sie stehen in einer eigenen Zeile, weil
        // fuenf Schalter neben vier Knoepfen auf einem schmalen Geraet zu
        // Zeilenumbruechen mitten im Wort fuehren -- derselbe Fehler, der
        // "Nordung" einmal als "NORDUN / G" gezeigt hat.
        //
        // DER ZUSTAND STEHT AM SCHALTER, nicht nur in seiner Farbe. Zuerst war
        // der Unterschied allein die Textfarbe -- gedaempftes Grau gegen
        // Schwarz, beides auf demselben grauen Knopf. Max am 05.08.2026: "ich
        // weiss nicht ob die overlays funktionieren weil die tasten kein
        // visuelles feedback geben ob sie an sind." Er hatte recht, und der
        // Fehler wiegt schwer: Wer nicht sieht, dass die Ebene "Wasser"
        // ausgeschaltet ist, haelt eine Karte ohne Quellen fuer eine Gegend
        // ohne Quellen.
        //
        // Der gefuellte Punkt ist an, der leere aus. Das ist die uebliche
        // Anzeige fuer einen Schalter, sie braucht keine Farbe und keine
        // Flaeche, und sie ist auch dann noch lesbar, wenn der Schirm im
        // Sonnenlicht steht oder die Augen muede sind. Die Farbe kommt dazu,
        // sie traegt die Auskunft aber nicht allein.
        // ZWEI AUSKUENFTE AN EINEM KNOPF, und sie duerfen sich nicht in die
        // Quere kommen:
        //
        //   Das Zeichen sagt, ob die Ebene EINGESCHALTET ist -- gefuellter
        //   Punkt an, leerer Punkt aus. Es aendert sich bei JEDEM Antippen.
        //   Die Null dahinter sagt, dass in diesem Bild nichts davon liegt.
        //
        // Der erste Versuch am 06.08.2026 hat den Strich "—" ANSTELLE des
        // Zeichens gesetzt. Damit war die zweite Auskunft da, aber die erste
        // weg: Wer auf einen leeren Schalter tippte, sah gar nichts
        // passieren -- weder auf der Karte (dort liegt ja nichts) noch am
        // Knopf. Max danach: "also die overlays kann man jetzt einfach nicht
        // anzeigen lassen." Ein Knopf, der eine Beruehrung nicht quittiert,
        // ist schlimmer als einer, der zu wenig erklaert.
        // DIE SCHALTER STEHEN NICHT MEHR IN DER LEISTE, SONDERN IN EINER
        // AUSWAHL DAHINTER.
        //
        // Max am 18.08.2026: "die steuerung kommt mir uebrigens noch extrem
        // unuebersichtlich vor." Mit dem achten Schalter (Satellit) gaben
        // 1080 Bildpunkte noch 135 je Stueck her, und die Beschriftungen
        // standen als "Wass...", "Gelaen...", "Satellit" da -- ein Schalter,
        // dessen Namen man raten muss, ist keiner.
        //
        // Zwei Reihen zu vier waren der erste Versuch und gingen auch nicht
        // auf: Die zweite Reihe lag unter der Bereichsleiste, und der Knopf
        // sah aus, als tue er nichts. Statt daran weiterzuschieben, liegen
        // die Ebenen jetzt in einer Auswahl -- volle Zeilen, volle Namen,
        // kein Platzproblem, und die Leiste hat nur noch einen Knopf.
        val ebenenleiste = b.reihe().apply {
            setPadding(0, b.stil.abstand / 4, 0, 0)
        }
        // Name, Zustand und was beim Antippen geschieht -- mehr braucht die
        // Auswahl nicht. Keine Knopf-Ansichten: Die Auswahl baut ihre Zeilen
        // selbst, und ein Knopf, der nirgends haengt, waere nur Ballast.
        val wahl = ArrayList<Triple<String, () -> Boolean, () -> Unit>>()
        val malen = ArrayList<() -> Unit>()
        for (ebene in Kartenblatt.Ebene.values()) {
            // Den gemerkten Zustand herstellen, BEVOR der Schalter gezeichnet
            // wird -- sonst steht am Knopf etwas anderes als auf der Karte.
            // Als Voreinstellung gilt, was das frische Blatt mitbringt; so
            // gewinnt eine neu hinzugekommene Ebene ihren eigenen Anfangswert
            // und nicht ein "aus" aus einer alten Ablage.
            if (gemerkt.ebeneAn(ebene, neuesBlatt.ebeneAn(ebene)) != neuesBlatt.ebeneAn(ebene)) {
                neuesBlatt.schalteEbene(ebene)
            }
            wahl.add(
                Triple(
                    // Die Null sagt weiter, dass in diesem Bild nichts davon
                    // liegt. Sie ist die zweite Auskunft neben dem Haken und
                    // darf nicht wegfallen -- sonst tippt jemand auf eine
                    // Ebene, sieht nichts passieren und sucht den Fehler.
                    ebene.anzeige + (if (neuesBlatt.ebeneHatInhalt(ebene)) "" else "  (hier nichts)"),
                    { neuesBlatt.ebeneAn(ebene) },
                    {
                        neuesBlatt.schalteEbene(ebene)
                        gemerkt.setzeEbene(ebene, neuesBlatt.ebeneAn(ebene))
                    },
                ),
            )
        }
        // RUHIG steht bei den Ebenen-Schaltern, weil es dasselbe tut: Es nimmt
        // etwas aus dem Bild. Der Unterschied ist, dass es die Grundkarte
        // betrifft und nicht die Punkte -- deshalb steht es am Ende der Reihe
        // und nicht mittendrin.
        neuesBlatt.ruhigeKarte = ruhigGewaehlt
        wahl.add(
            Triple(
                "Ruhig  (weniger auf der Karte)",
                { ruhigGewaehlt },
                {
                    ruhigGewaehlt = !ruhigGewaehlt
                    gemerkt.ruhigeKarte = ruhigGewaehlt
                    neuesBlatt.ruhigeKarte = ruhigGewaehlt
                    standzeile?.text = if (ruhigGewaehlt) {
                        "Ruhige Karte: Zufahrten, Gehsteige und Pfade, Bäche und " +
                            "Regionsgrenzen bleiben weg; auf der Übersicht auch die Flüsse"
                    } else {
                        "Volle Karte: alles wird gezeigt"
                    }
                },
            ),
        )

        // Der Satellit steht NUR DA, WENN EIN BILDPAKET DANEBENLIEGT. Ein
        // Schalter, der nichts schalten kann, ist eine Verabredung, die die
        // App nicht einhaelt -- und im Ernstfall sucht niemand nach dem Grund.
        // DIE GEZEICHNETE KARTE SELBST -- Strassen, Gewaesser, Grenzen,
        // Flaechen. Max am 18.08.2026: "selbst mit allen ebenen aus ist immer
        // noch extrem viele linien auf der karte zu sehen. was ist das man
        // soll es ausschalten koennen." Die Schalter darueber steuern nur die
        // PUNKTE; die Zeichnung hatte ueberhaupt keinen. Ueber einem
        // Satellitenbild ist das der Unterschied zwischen einem Foto und
        // einem Foto unter einem Netz aus Strichen.
        for ((name, sorten) in zeichnungsgruppen()) {
            wahl.add(
                Triple(
                    name,
                    { name !in weggelasseneGruppen },
                    {
                        if (name in weggelasseneGruppen) {
                            weggelasseneGruppen.remove(name)
                        } else {
                            weggelasseneGruppen.add(name)
                        }
                        gemerkt.setzeZeichnung(name, name !in weggelasseneGruppen)
                        neuesBlatt.weggelassen = weggelasseneGruppen
                            .flatMap { zeichnungsgruppen()[it].orEmpty() }
                            .toSet()
                    },
                ),
            )
        }
        neuesBlatt.weggelassen = weggelasseneGruppen
            .flatMap { zeichnungsgruppen()[it].orEmpty() }
            .toSet()

        if (neuesBlatt.hatBilder) {
            neuesBlatt.zeigtBilder = bilderGewaehlt
            wahl.add(
                Triple(
                    "Satellitenbild",
                    { bilderGewaehlt },
                    {
                        bilderGewaehlt = !bilderGewaehlt
                        gemerkt.satellitenbild = bilderGewaehlt
                        neuesBlatt.zeigtBilder = bilderGewaehlt
                        standzeile?.text = if (bilderGewaehlt) {
                            "Satellitenbild: Flächen bleiben weg, damit das Foto zu sehen ist"
                        } else {
                            "Gezeichnete Karte"
                        }
                    },
                ),
            )
        }

        // Der Knopf, hinter dem sie liegen. Er sagt im Ruhezustand, wie viele
        // Ebenen an sind -- sonst muesste man ihn oeffnen, um zu sehen, ob
        // ueberhaupt etwas aus ist.
        val ebenenwahl = ebenenknopf(b, "")
        fun beschrifte() {
            val an = wahl.count { it.second() }
            ebenenwahl.text = "Ebenen  ($an von ${wahl.size} an)"
            ebenenwahl.setTextColor(b.stil.text)
            ebenenwahl.alpha = 1f
        }
        beschrifte()
        ebenenwahl.setOnClickListener {
            val namen = wahl.map { it.first }.toTypedArray()
            val stand = BooleanArray(wahl.size) { wahl[it].second() }
            // IM SPARMODUS DER DUNKLE DIALOG. Der helle des Systems ist auf
            // einem schwarzen Bildschirm ein Blitz -- in einer App, die
            // nachts Augen und Akku schonen soll, genau das Gegenteil von
            // dem, was der Sparmodus verspricht.
            android.app.AlertDialog.Builder(
                gastgeber,
                if (b.sparmodus) {
                    android.R.style.Theme_Material_Dialog_Alert
                } else {
                    android.R.style.Theme_Material_Light_Dialog_Alert
                },
            )
                .setTitle("Was auf der Karte liegt")
                .setMultiChoiceItems(namen, stand) { _, nummer, _ ->
                    // Sofort wirksam und nicht erst beim Schliessen: Wer eine
                    // Ebene abschaltet, will sehen, was verschwindet.
                    wahl[nummer].third()
                    beschrifte()
                    for (male in malen) male()
                }
                .setPositiveButton("Fertig", null)
                .show()
        }
        ebenenleiste.addView(ebenenwahl, b.breit())
        ebenenNeuZeichnen = { beschrifte() }

        // Die Karte meldet, sobald sich geaendert hat, was ueberhaupt im Bild
        // liegt -- beim Zoomen und beim Schieben.
        neuesBlatt.beiEbenen = { for (male in malen) male() }
        spalte.addView(ebenenleiste, b.breit())
        karte.wege?.let { netz ->
            spalte.addView(routenleiste(b, neuesBlatt, netz), b.breit())
        }

        baueMerkleiste(b, neuesBlatt, merkknopf, merkleiste, ebenenleiste) { for (male in malen) male() }
        spalte.addView(merkleiste, b.breit())

        // DIE STANDZEILE ZUERST, DAS SUCHFELD DARUNTER. Andersherum stand das
        // Feld zwischen Karte und Standzeile und hat deren Zeile aus dem Bild
        // geschoben -- die Koordinatenanzeige unter der Karte war weg. Ein
        // neues Feld darf nicht das verdraengen, wofuer die Karte da ist.
        standzeile = b.nebentext("").also { spalte.addView(it, b.breit()) }
        spalte.addView(suchleiste(b, neuesBlatt, karte.verzeichnis), b.breit())
        neuesBlatt.beiZustand = { text -> standzeile?.text = text }

        // Eine Karte, die von aussen ins Geraet gelangt ist, traegt keine
        // Unterschrift. Das steht dauerhaft da und laesst sich nicht wegtippen
        // -- wer sich auf eine Karte verlaesst, muss wissen, woher sie kommt.
        if (karte.geprueft) {
            spalte.addView(
                b.nebentext("Karte: ${karte.quelle} · " +
                    "Zoom ${karte.zoomKleinste}–${karte.zoomGroesste} · " +
                    "${karte.kachelzahl} Kacheln"),
                b.breit(),
            )
        } else {
            spalte.addView(
                b.warnung(
                    "Diese Karte ist NICHT GEPRÜFT. " +
                        "${karte.quelle}. Es lässt sich nicht feststellen, wer sie " +
                        "gemacht hat — eine erfundene Karte kann eine Quelle zeigen, " +
                        "wo keine ist.",
                ),
                b.breit(),
            )
        }

        // JEDE ZUSATZDATEI SAGT SELBST, WORAN MAN IST -- dieselbe Sprache
        // wie bei der Karte. Bis zum 18.08.2026 stand hier nur ein pauschaler
        // Hinweis, weil es die Pruefung noch nicht gab.
        //
        // Warum das bei diesen besonders zaehlt: Ein erfundener
        // Eintrag "Krankenhaus" schickt jemanden an eine Stelle, wo keines
        // ist; eine erfundene Kante ueber eine Bruecke, die es nicht gibt.
        // Man sieht es der Datei nicht an.
        //
        // Eine Datei mit KAPUTTER Unterschrift steht hier gar nicht: Der
        // Lader oeffnet sie nicht.
        val zusatz = buildList {
            if (karte.hoehen != null) add("Geländeform" to karte.hoehenUrteil)
            if (karte.bilder != null) add("Satellitenbild" to karte.bilderUrteil)
            if (karte.verzeichnis != null) add("Namensverzeichnis" to karte.verzeichnisUrteil)
            if (karte.wege != null) add("Wegenetz" to karte.wegeUrteil)
        }
        val ohneUnterschrift = zusatz.filter { it.second == null }.map { it.first }
        val mitUnterschrift = zusatz.filter { it.second != null }
        for ((name, urteil) in mitUnterschrift) {
            spalte.addView(
                b.nebentext("$name: ${Paketlader.urteilstext(urteil!!)}"),
                b.breit(),
            )
        }
        if (ohneUnterschrift.isNotEmpty()) {
            val was = when (ohneUnterschrift.size) {
                1 -> ohneUnterschrift[0]
                2 -> "${ohneUnterschrift[0]} und ${ohneUnterschrift[1]}"
                else -> ohneUnterschrift.dropLast(1).joinToString(", ") +
                    " und " + ohneUnterschrift.last()
            }
            spalte.addView(
                b.warnung(
                    "Ohne Unterschrift: $was. Es lässt sich nicht feststellen, wer diese " +
                        "Dateien gemacht hat — benutze sie nur, wenn du weißt, woher sie " +
                        "kommen. Ein erfundenes Verzeichnis kann ein Krankenhaus zeigen, " +
                        "wo keines ist, und ein erfundenes Wegenetz einen Weg, den es " +
                        "nicht gibt.",
                ),
                b.breit(),
            )
        }

        spalte.addView(
            b.fliesstext(
                "Die Karte kennt deinen Standort nicht und wird ihn nie kennen — die App " +
                    "fragt keine Ortungsberechtigung. Schieben mit einem Finger, zoomen " +
                    "mit zwei. „Blick ↑“ dreht die Karte so, dass oben ist, wohin " +
                    "du schaust; dann gilt der Hinweis zur Fehlweisung unten auch für die " +
                    "Karte.",
            ),
            b.breit(),
        )

        // Die Lizenz der Kartendaten steht am Kartenbereich und nicht in einem
        // Untermenue: Die Open Database License verlangt den Hinweis bei jeder
        // Weitergabe, und eine Karte ohne Herkunftsangabe ist ausserdem eine
        // Karte, der man nicht ansieht, wie alt sie ist.
        // Beide Lizenzhinweise sind Bedingung der Nutzung, nicht Höflichkeit:
        // die ODbL verlangt die Nennung bei jeder Weitergabe, und Artikel 6 der
        // Copernicus-Lizenz schreibt den Wortlaut vor.
        val herkunft = StringBuilder("Kartendaten © OpenStreetMap-Mitwirkende, ODbL 1.0")
        if (karte.hoehen != null) {
            herkunft.append(
                "\nGeländeform: produced using Copernicus WorldDEM-30 " +
                    "© DLR e.V. 2010-2014 and © Airbus Defence and Space GmbH 2014-2018 " +
                    "provided under COPERNICUS by the European Union and ESA; " +
                    "all rights reserved.",
            )
        }
        // Der Satz, den der Copernicus-Rechtshinweis verlangt, sobald
        // Sentinel-Bilder weitergegeben oder gezeigt werden. "modified", weil
        // beim Bau zugeschnitten, umgerechnet und neu gepackt wird. Das Jahr
        // kommt aus der Datei und nicht aus dem Quelltext -- es gehoert zur
        // Aufnahme und nicht zur App.
        karte.bilder?.let { bilder ->
            val jahr = (bilder.aufnahmeVon / 10000).takeIf { it in 2015..2100 }
            herkunft.append(
                "\nContains modified Copernicus Sentinel data" +
                    (jahr?.let { " $it" } ?: ""),
            )
        }
        spalte.addView(b.nebentext(herkunft.toString()), b.breit())

        spalte.addView(b.rasterstreifen(), b.rasterbreit().apply {
            topMargin = b.stil.abstand
            bottomMargin = b.stil.abstand
        })
    }

    /**
     * Ein Schalter der Ebenenleiste.
     *
     * Ohne Grossbuchstaben und etwas kleiner als die Zoomknoepfe. Mit der
     * Android-Vorgabe (Grossbuchstaben, volle Textgroesse) passten fuenf
     * Woerter nicht nebeneinander und brachen mitten im Wort um: "WASS / ER",
     * "GELÄN / DE". Denselben Fehler hatte schon "NORDUN / G" -- er faellt nur
     * am Geraet auf, nie beim Uebersetzen.
     */
    /**
     * Start und Ziel setzen, Route rechnen, Route weg.
     *
     * DIE KARTENMITTE IST DER PUNKT, nicht ein Fingertipp. Das Fadenkreuz
     * steht schon da, das Schieben und Zoomen kennt jeder -- ein neuer
     * Wischgriff waere eine zweite Bedienung fuer dieselbe Sache. Wer ein
     * Ziel setzen will, schiebt es unter das Kreuz.
     *
     * Die Leiste erscheint NUR, wenn ein Wegenetz danebenliegt.
     */
    private fun routenleiste(
        b: Bausteine,
        blatt: Kartenblatt,
        netz: org.compasszero.karte.Wegenetz,
    ): View {
        val reihe = b.reihe().apply { setPadding(0, b.stil.abstand / 4, 0, 0) }

        fun rechne() {
            val vonKnoten = routeVon
            val nachKnoten = routeNach
            if (vonKnoten == null || nachKnoten == null) return
            val route = try {
                netz.route(vonKnoten, nachKnoten)
            } catch (fehler: Exception) {
                null
            }
            if (route == null) {
                blatt.route = null
                standzeile?.text =
                    "Von hier führt kein Weg dorthin — jedenfalls keiner, der im " +
                    "Wegenetz steht. Zwischen zwei Tälern ohne Verbindung kommt das vor."
                return
            }
            blatt.route = route.punkte
            val km = route.meter / 1000.0
            // Zu Fuss vier Kilometer in der Stunde. Das ist ein grober Wert und
            // steht auch so da -- im Gebirge, mit Gepaeck oder im Dunkeln ist
            // es weniger, und eine Zahl mit Minutenangabe waere hier eine
            // Behauptung, auf die sich niemand verlassen darf.
            val stunden = km / 4.0
            standzeile?.text = if (km < 1.0) {
                "Route: %.0f Meter".format(route.meter)
            } else {
                "Route: %.1f km, zu Fuß grob %s".format(km, dauertext(stunden))
            }
        }

        fun knopf(beschriftung: String, tu: () -> Unit) =
            ebenenknopf(b, beschriftung).apply {
                setTextColor(b.stil.text)
                alpha = 1f
                setOnClickListener { tu() }
            }

        reihe.addView(
            knopf("Start hier") {
                val knoten = netz.naechsterKnoten(blatt.mitteLat, blatt.mitteLon)
                if (knoten < 0) {
                    standzeile?.text = "Hier ist kein Weg in der Nähe."
                } else {
                    routeVon = knoten
                    standzeile?.text = "Start gesetzt. Jetzt das Ziel unter das Kreuz schieben."
                    rechne()
                }
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        reihe.addView(
            knopf("Ziel hier") {
                val knoten = netz.naechsterKnoten(blatt.mitteLat, blatt.mitteLon)
                when {
                    knoten < 0 -> standzeile?.text = "Hier ist kein Weg in der Nähe."
                    routeVon == null -> standzeile?.text = "Erst den Start setzen."
                    else -> {
                        routeNach = knoten
                        rechne()
                    }
                }
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        reihe.addView(
            knopf("Route weg") {
                routeVon = null
                routeNach = null
                blatt.route = null
                standzeile?.text = "Route gelöscht."
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        // Eine schon berechnete Route ueberlebt den Reiterwechsel.
        if (routeVon != null && routeNach != null) rechne()
        return reihe
    }

    private fun dauertext(stunden: Double): String = when {
        stunden < 1.0 -> "%.0f Minuten".format(stunden * 60)
        else -> "%.1f Stunden".format(stunden)
    }

    /**
     * Das Suchfeld ueber der Standzeile.
     *
     * Heute versteht es Koordinaten. Max am 18.08.2026 will damit auch nach
     * Orten, Krankenhaeusern und Quellen suchen koennen -- dafuer braucht es
     * ein Namensverzeichnis neben der Karte, und das kommt als eigenes
     * Stueck. Die Zeile sagt deshalb geradeheraus, was sie heute kann, statt
     * eine Suche vorzutaeuschen, die nichts findet.
     */
    private fun suchleiste(b: Bausteine, blatt: Kartenblatt, verzeichnis: org.compasszero.karte.Namensdatei?): View {
        val reihe = b.reihe().apply { setPadding(0, b.stil.abstand / 4, 0, 0) }
        val feld = android.widget.EditText(gastgeber).apply {
            hint = if (verzeichnis == null) {
                "Land oder Koordinaten"
            } else {
                "Ort, Land oder Koordinaten"
            }
            textSize = b.stil.textGroesse * 0.9f
            typeface = b.stil.textSchrift
            setTextColor(b.stil.text)
            setHintTextColor(b.stil.gedaempft)
            background = b.randfeld()
            maxLines = 1
            // Kein automatisches Grossschreiben und keine Rechtschreibhilfe:
            // Beides macht aus "47.8n" etwas anderes, als der Nutzer tippt.
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(
                b.stil.abstand / 2, b.stil.abstand / 2,
                b.stil.abstand / 2, b.stil.abstand / 2,
            )
        }
        fun suche() {
            val text = feld.text?.toString().orEmpty()
            // ERST KOORDINATEN, DANN NAMEN. Eine Eingabe wie "47.8 13.05"
            // koennte theoretisch auch ein Ortsname sein; als Koordinate ist
            // sie aber eindeutig, und Eindeutiges geht vor.
            val ort = Koordinaten.lies(text)
            if (ort == null) {
                if (text.isBlank()) {
                    standzeile?.text = if (verzeichnis == null) {
                        "Koordinaten eingeben, etwa 47.8 13.05 oder 47°48'N 13°03'E"
                    } else {
                        "Ortsnamen oder Koordinaten eingeben"
                    }
                    return
                }
                // LAENDER VOR ORTEN, und nur bei vollem Namen. In den
                // Kartendaten stehen keine Laendernamen -- eine Staatsgrenze
                // ist dort eine Linie ohne Beschriftung. Ohne diese Tabelle
                // faende die Suche "Frankreich" nie.
                val land = Laender.suche(text)
                if (land != null) {
                    val drauf = blatt.springeZu(land.breite, land.laenge, 7f)
                    standzeile?.text = if (drauf) {
                        "${land.name} — ${land.hauptstadt}, " +
                            Koordinaten.schreibe(land.breite, land.laenge)
                    } else {
                        "${land.name} — ${land.hauptstadt}: dort liegt keine geladene Karte"
                    }
                    tastaturWeg(feld)
                    return
                }
                if (verzeichnis == null) {
                    standzeile?.text =
                        "„$text“ ist weder eine Koordinate noch ein Land, und ein " +
                        "Namensverzeichnis liegt nicht daneben. Es lässt sich mit " +
                        "namen_bauen.py aus der eigenen Karte bauen — siehe Anleitung."
                    return
                }
                val treffer = try {
                    verzeichnis.suche(text)
                } catch (fehler: Exception) {
                    emptyList()
                }
                if (treffer.isEmpty()) {
                    standzeile?.text = "„$text“ steht nicht im Namensverzeichnis."
                    return
                }
                if (treffer.size == 1) {
                    springeAufTreffer(blatt, treffer.first())
                    tastaturWeg(feld)
                    return
                }
                zeigeTreffer(b, blatt, feld, treffer)
                return
            }
            // Auf eine Stufe, auf der man etwas erkennt -- aber nicht feiner
            // als die Karte hergibt.
            val draufgesprungen = blatt.springeZu(ort.breite, ort.laenge, 12f)
            standzeile?.text = if (draufgesprungen) {
                Koordinaten.schreibe(ort.breite, ort.laenge)
            } else {
                Koordinaten.schreibe(ort.breite, ort.laenge) +
                    " — dort liegt keine geladene Karte"
            }
            tastaturWeg(feld)
        }
        feld.setOnEditorActionListener { _, _, _ ->
            suche()
            true
        }
        reihe.addView(
            feld,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        reihe.addView(
            ebenenknopf(b, "Suchen").apply {
                // DIE FARBE MUSS HIER GESETZT WERDEN. `ebenenknopf` faerbt
                // nicht selbst -- die Ebenen tun das in ihrer eigenen
                // Zeichenfunktion, je nachdem ob sie an sind. Ohne diese
                // Zeile nimmt der Knopf die dunkle Standardfarbe des Systems
                // und ist auf schwarzem Grund unsichtbar: ein leerer Kasten
                // neben dem Suchfeld, am 18.08.2026 genau so am Geraet
                // gesehen.
                setTextColor(b.stil.text)
                setPadding(b.stil.abstand / 2, paddingTop, b.stil.abstand / 2, paddingBottom)
                setOnClickListener { suche() }
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        return reihe
    }

    /**
     * Die Trefferliste, wenn ein Name mehrfach vorkommt.
     *
     * Sie sagt zu jedem Treffer die ART dazu -- "Quelle", "Krankenhaus",
     * "Ort". Ohne das steht dreimal derselbe Name da, und man tippt aufs
     * Geratewohl.
     */
    private fun zeigeTreffer(
        b: Bausteine,
        blatt: Kartenblatt,
        feld: android.widget.EditText,
        treffer: List<org.compasszero.karte.Namenstreffer>,
    ) {
        val zeilen = treffer.map { "${it.name}  (${it.artName})" }.toTypedArray()
        android.app.AlertDialog.Builder(
            gastgeber,
            if (b.sparmodus) {
                android.R.style.Theme_Material_Dialog_Alert
            } else {
                android.R.style.Theme_Material_Light_Dialog_Alert
            },
        )
            .setTitle("${treffer.size} Treffer")
            .setItems(zeilen) { _, nummer ->
                springeAufTreffer(blatt, treffer[nummer])
                tastaturWeg(feld)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun springeAufTreffer(blatt: Kartenblatt, treffer: org.compasszero.karte.Namenstreffer) {
        val drauf = blatt.springeZu(treffer.breite, treffer.laenge, 13f)
        standzeile?.text = if (drauf) {
            "${treffer.name} (${treffer.artName}) — " +
                Koordinaten.schreibe(treffer.breite, treffer.laenge)
        } else {
            "${treffer.name} (${treffer.artName}) — dort liegt keine geladene Karte"
        }
    }

    // Die Tastatur weg: Sie verdeckt sonst genau die Stelle, zu der gerade
    // gesprungen wurde.
    private fun tastaturWeg(feld: android.widget.EditText) {
        (gastgeber.getSystemService(Context.INPUT_METHOD_SERVICE)
            as? android.view.inputmethod.InputMethodManager)
            ?.hideSoftInputFromWindow(feld.windowToken, 0)
        feld.clearFocus()
    }

    private fun ebenenknopf(b: Bausteine, beschriftung: String): Button =
        Button(gastgeber).apply {
            text = beschriftung
            isAllCaps = false
            // Etwas kleiner als zuerst: Vor der Beschriftung steht jetzt ein
            // Zustandspunkt, und "● Gelände" braucht mehr Platz als "GELÄNDE".
            // Ein Umbruch mitten im Wort ist genau der Eindruck, den diese
            // Karte nicht mehr machen soll.
            //
            // IM SPARMODUS DEUTLICH KLEINER, weil dort die Grundschrift groesser
            // ist: Mit 0,8 lief "● Gelände 0" ueber den Knopfrand hinaus und war
            // am Bildschirm nur noch als Punkt zu sehen -- fuenf Schalter
            // nebeneinander geben auf 1080 Bildpunkten rund 200 je Stueck her.
            // Die Kuerzung am Ende ist die Notbremse, falls eine Uebersetzung
            // laenger ausfaellt: lieber "Gelän…" als ein leerer Knopf.
            textSize = b.stil.textGroesse * (if (b.sparmodus) 0.55f else 0.8f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            typeface = b.stil.textSchrift
            setPadding(0, paddingTop, 0, paddingBottom)
            layoutParams = LinearLayout.LayoutParams(0, Bausteine.WRAP, 1f)
            sparknopf(this, b)
        }

    /**
     * Gibt einem Knopf im Sparmodus einen eigenen Grund.
     *
     * DER FEHLER, DEN DAS BEHEBT: Im Sparmodus ist die Schrift hell -- der
     * Knopf behielt aber den hellgrauen Standardgrund von Android. Helles auf
     * Hellem: Auf dem Bildschirm war "Gelände 0" ueberhaupt nicht mehr zu
     * sehen, und auch "GANZ" und "NORD" waren kaum zu lesen. Max am 06.08.2026:
     * "vorallem im notfallmodus ist die karte unuebersichtlich."
     *
     * Schwarz mit dünnem hellem Rahmen ist hier ausserdem das Richtige und
     * nicht nur das Lesbare: Auf einem OLED-Schirm kostet eine schwarze Flaeche
     * keinen Strom, eine hellgraue schon -- und ein Knopf ist eine grosse
     * Flaeche.
     */
    private fun sparknopf(knopf: Button, b: Bausteine) {
        if (!b.sparmodus) return
        knopf.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.BLACK)
            setStroke((1.5f * b.dichte).toInt(), b.stil.gedaempft)
            cornerRadius = 2f * b.dichte
        }
    }

    /**
     * Eigene Punkte und Wege setzen.
     *
     * WARUM NICHT AUF DIE KARTE TIPPEN: Die Karte deutet jede Beruehrung schon
     * als Schieben oder Zoomen. Ein Tipp, der zusaetzlich einen Punkt setzt,
     * kollidiert damit und erzeugt Punkte, die niemand wollte -- in einer Lage,
     * in der man auf dem Handy herumwischt, waehrend man geht. Stattdessen
     * steht ein Fadenkreuz fest in der Bildmitte: Man schiebt die Karte
     * darunter und drueckt dann. Das ist genauer, denn man SIEHT vorher, wo der
     * Punkt landet, und es geht mit einer Hand.
     *
     * WARUM DER NAME ERST HINTERHER KOMMT: Ein Punkt wird sofort gespeichert
     * und heisst zunaechst nur "Punkt 3". Wer erst einen Namen eintippen muss,
     * verliert den Punkt, wenn er dabei unterbrochen wird -- und tippen kann
     * man mit klammen Fingern schlecht. Benennen laesst sich alles hinterher
     * in der Liste.
     */
    private fun baueMerkleiste(
        b: Bausteine,
        blatt: Kartenblatt,
        merkknopf: Button,
        merkleiste: LinearLayout,
        ebenenleiste: LinearLayout,
        ebenenNeuZeichnen: () -> Unit,
    ) {
        val ablage = Eigenkartendatei(gastgeber.filesDir)
        var eigenes = ablage.lade()
        blatt.eigenes = eigenes

        var merkt = false
        var wegImBau: MutableList<Pair<Double, Double>>? = null

        val setzKnopf = knopf(b, "Punkt") { }
        val wegKnopf = knopf(b, "Weg") { }
        val listeKnopf = knopf(b, "Liste") { }
        merkleiste.addView(setzKnopf)
        merkleiste.addView(wegKnopf)
        merkleiste.addView(listeKnopf)

        fun sichere() {
            try {
                ablage.sichere(eigenes)
            } catch (fehler: Exception) {
                // Ein misslungenes Sichern darf nicht stumm bleiben: Der Nutzer
                // glaubt sonst, sein Punkt sei sicher, und er ist es nicht.
                standzeile?.text = "Konnte nicht gespeichert werden — Speicher voll?"
            }
        }

        fun zeichneKnoepfe() {
            merkknopf.text = if (merkt) "Fertig" else "Merken"
            merkleiste.visibility = if (merkt) View.VISIBLE else View.GONE
            ebenenleiste.visibility = if (merkt) View.GONE else View.VISIBLE
            blatt.zeigtFadenkreuz = merkt
            val baut = wegImBau
            setzKnopf.isEnabled = baut == null
            setzKnopf.alpha = if (baut == null) 1f else 0.4f
            wegKnopf.text = when {
                baut == null -> "Weg"
                baut.size < 2 -> "Stelle"
                else -> "Stelle (${baut.size})"
            }
            listeKnopf.text = if (baut == null) "Liste" else "Schluss"
        }

        merkknopf.setOnClickListener {
            merkt = !merkt
            if (!merkt && wegImBau != null) {
                // Beim Verlassen einen angefangenen Weg nicht verwerfen: Was
                // gezeichnet wurde, war Arbeit.
                val stellen = wegImBau!!
                if (stellen.size >= 2) {
                    eigenes.legeWegAn("Weg ${eigenes.wege.size + 1}", stellen)
                    sichere()
                    blatt.eigenes = eigenes
                }
                wegImBau = null
                blatt.wegImBau = null
            }
            zeichneKnoepfe()
            ebenenNeuZeichnen()
        }

        setzKnopf.setOnClickListener {
            val punkt = eigenes.setzePunkt(
                blatt.mitteLon,
                blatt.mitteLat,
                "Punkt ${eigenes.punkte.size + 1}",
            )
            sichere()
            blatt.eigenes = eigenes
            standzeile?.text = "„${punkt.name}“ gesetzt — umbenennen unter Liste"
            ebenenNeuZeichnen()
        }

        wegKnopf.setOnClickListener {
            val stellen = wegImBau ?: mutableListOf<Pair<Double, Double>>().also {
                wegImBau = it
                standzeile?.text = "Weg begonnen — Karte schieben, dann „Stelle“"
            }
            stellen.add(blatt.mitteLon to blatt.mitteLat)
            blatt.wegImBau = stellen.toList()
            zeichneKnoepfe()
        }

        listeKnopf.setOnClickListener {
            val baut = wegImBau
            if (baut != null) {
                if (baut.size >= 2) {
                    eigenes.legeWegAn("Weg ${eigenes.wege.size + 1}", baut)
                    sichere()
                    blatt.eigenes = eigenes
                    standzeile?.text = "Weg gespeichert (${baut.size} Stellen)"
                } else {
                    standzeile?.text = "Ein Weg braucht mindestens zwei Stellen"
                }
                wegImBau = null
                blatt.wegImBau = null
                zeichneKnoepfe()
                ebenenNeuZeichnen()
                return@setOnClickListener
            }
            zeigeListe(b, blatt, eigenes, { sichere() }, ebenenNeuZeichnen)
        }

        zeichneKnoepfe()
    }

    /** Die eigenen Sachen ansehen, umbenennen, loeschen. */
    private fun zeigeListe(
        b: Bausteine,
        blatt: Kartenblatt,
        eigenes: Eigenkarte,
        sichere: () -> Unit,
        ebenenNeuZeichnen: () -> Unit,
    ) {
        val eintraege = ArrayList<Pair<String, () -> Unit>>()
        // Die Liste nennt das Sinnbild statt nur "(Punkt)" -- damit sieht man
        // schon hier, was ein Punkt sein soll, und muss ihn nicht erst oeffnen.
        //
        // Die Reihenfolge steht in Eigenkarte.punkteGeordnet(), damit sie
        // pruefbar ist -- hier wird sie nur benutzt.
        for (p in eigenes.punkteGeordnet()) {
            val art = if (p.sinnbild == Sinnbild.KEINS) "Punkt" else Sinnbild.NAMEN[p.sinnbild]
            eintraege.add("${p.name}  ($art)" to { frageNameOderLoeschen(b, p.name, p.sinnbild) { neu, bild ->
                if (neu == null) {
                    eigenes.loeschePunkt(p.nummer)
                } else {
                    eigenes.benennePunkt(p.nummer, neu)
                    eigenes.setzeSinnbild(p.nummer, bild)
                }
                sichere()
                blatt.eigenes = eigenes
                ebenenNeuZeichnen()
            } })
        }
        for (w in eigenes.wege.toList()) {
            eintraege.add("${w.name}  (Weg, ${w.stellen.size})" to { frageNameOderLoeschen(b, w.name, null) { neu, _ ->
                if (neu == null) eigenes.loescheWeg(w.nummer) else eigenes.benenneWeg(w.nummer, neu)
                sichere()
                blatt.eigenes = eigenes
                ebenenNeuZeichnen()
            } })
        }

        if (eintraege.isEmpty()) {
            android.app.AlertDialog.Builder(gastgeber)
                .setTitle("Meine Punkte")
                .setMessage(
                    "Noch nichts gemerkt.\n\nKarte so schieben, dass das Fadenkreuz " +
                        "auf der Stelle liegt, dann „Punkt“ drücken.",
                )
                .setPositiveButton("Gut", null)
                .show()
            return
        }

        android.app.AlertDialog.Builder(gastgeber)
            .setTitle("Meine Punkte und Wege")
            .setItems(eintraege.map { it.first }.toTypedArray()) { _, welcher ->
                eintraege[welcher].second()
            }
            .setNegativeButton("Zurück", null)
            .show()
    }

    /**
     * Umbenennen oder loeschen. Das Loeschen fragt bewusst NICHT noch einmal
     * nach: Es steht hinter zwei Tippern in einem Untermenue, und ein selbst
     * gesetzter Punkt ist wiederherstellbar, indem man ihn neu setzt.
     */
    private fun frageNameOderLoeschen(
        b: Bausteine,
        jetziger: String,
        jetzigesSinnbild: String?,
        fertig: (String?, String) -> Unit,
    ) {
        val rand = (12 * gastgeber.resources.displayMetrics.density).toInt()
        val spalte = android.widget.LinearLayout(gastgeber).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(rand * 2, rand, rand * 2, 0)
        }
        val feld = android.widget.EditText(gastgeber).apply {
            setText(jetziger)
            setSelection(jetziger.length)
            typeface = b.stil.textSchrift
        }
        spalte.addView(feld)

        // Das Sinnbild gehoert in DENSELBEN Dialog wie der Name. Ein eigener
        // Menuepunkt dafuer waere ein Tipper mehr fuer etwas, das man ohnehin
        // beim Anlegen mitentscheidet.
        var gewaehlt = jetzigesSinnbild ?: Sinnbild.KEINS
        if (jetzigesSinnbild != null) {
            spalte.addView(
                b.nebentext("Sinnbild — es steht im Ring, damit ein eigener Punkt nie wie einer aus der Karte aussieht"),
            )
            val reihe = android.widget.LinearLayout(gastgeber).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
            }
            val knoepfe = HashMap<String, Button>()
            fun male() {
                for ((wert, knopf) in knoepfe) {
                    val an = wert == gewaehlt
                    knopf.setTypeface(knopf.typeface, if (an) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                    knopf.alpha = if (an) 1f else 0.45f
                }
            }
            for (wert in Sinnbild.ALLE) {
                val knopf = Button(gastgeber).apply {
                    text = Sinnbild.NAMEN[wert]
                    maxLines = 1
                    textSize = b.stil.textGroesse * 0.72f
                    setOnClickListener { gewaehlt = wert; male() }
                }
                knoepfe[wert] = knopf
                reihe.addView(knopf)
            }
            male()
            spalte.addView(
                android.widget.HorizontalScrollView(gastgeber).apply { addView(reihe) },
            )
        }

        android.app.AlertDialog.Builder(gastgeber)
            .setTitle("Umbenennen")
            .setView(spalte)
            .setPositiveButton("Übernehmen") { _, _ ->
                val neu = feld.text.toString().trim()
                if (neu.isNotEmpty()) fertig(neu, gewaehlt)
            }
            .setNeutralButton("Löschen") { _, _ -> fertig(null, gewaehlt) }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun knopf(b: Bausteine, beschriftung: String, tut: () -> Unit): Button =
        Button(gastgeber).apply {
            text = beschriftung
            // EINE ZEILE, IMMER. Im Sparmodus ist die Schrift groesser, und
            // "NORD ↑" rutschte dort mit dem Pfeil in eine zweite Zeile --
            // derselbe Umbruchfehler, der "Nordung" schon einmal als
            // "NORDUN / G" gezeigt hat. Ein Knopf, der zweizeilig wird,
            // schiebt ausserdem die ganze Reihe auseinander.
            maxLines = 1
            textSize = if (b.sparmodus) b.stil.textGroesse * 0.8f else b.stil.textGroesse
            typeface = b.stil.textSchrift
            setTextColor(b.stil.text)
            setOnClickListener { tut() }
            layoutParams = LinearLayout.LayoutParams(0, Bausteine.WRAP, 1f)
            sparknopf(this, b)
        }

    override fun anhalten() = kompass.anhalten()

    override fun fortsetzen() {
        if (::b.isInitialized) kompass.starten(b.sparmodus)
    }

    private fun zeichneStand() {
        val stand = kompass.stand
        val feld = anzeige ?: return
        val zeile = hinweis ?: return
        when (stand) {
            Kompass.Stand.KeinSensor -> {
                feld.text = "—"
                zeile.text = "Dieses Gerät hat keinen Magnetfeldsensor."
                zeile.setTextColor(b.stil.signal)
            }

            Kompass.Stand.NochKeineMessung -> {
                feld.text = "…"
                zeile.text = "Messe"
                zeile.setTextColor(b.stil.gedaempft)
            }

            Kompass.Stand.ZuSchraeg -> {
                feld.text = "—"
                zeile.text = "Gerät flach halten"
                zeile.setTextColor(b.stil.signal)
            }

            is Kompass.Stand.Richtung -> {
                val grad = stand.grad.toInt()
                feld.text = "$grad°  ${Kompass.himmelsrichtung(stand.grad)}"
                blatt?.setzeBlickrichtung(stand.grad)
                if (stand.verlaesslich) {
                    zeile.text = "missweisend Nord"
                    zeile.setTextColor(b.stil.gedaempft)
                } else {
                    zeile.text = "ungenau — Acht in der Luft beschreiben"
                    zeile.setTextColor(b.stil.signal)
                }
            }
        }
        rose?.setzeGrad(stand)
    }

    // Die Rose dreht sich, nicht die Nadel: Der Zeiger steht fest oben und
    // bedeutet "wohin du schaust", darunter wandert die Skala. So liest man es
    // auf einer Karte auch, und man muss nicht ueberlegen, ob der Pfeil den Weg
    // oder den Norden meint.
    private class Rose(gastgeber: Activity, private val b: Bausteine) : View(gastgeber) {

        private var grad = 0f
        private var zeigen = false

        private val strich = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * b.dichte
            color = b.stil.text
        }
        private val duenn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * b.dichte
            color = b.stil.gedaempft
        }
        private val schrift = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = b.stil.text
            textAlign = Paint.Align.CENTER
            textSize = 15f * b.dichte
            typeface = b.stil.ueberschriftSchrift
        }
        private val nordfarbe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = b.stil.signal
            textAlign = Paint.Align.CENTER
            textSize = 15f * b.dichte
            typeface = b.stil.ueberschriftSchrift
        }
        private val zeiger = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = b.stil.signal
            style = Paint.Style.FILL
        }

        fun setzeGrad(stand: Kompass.Stand) {
            val neu = stand as? Kompass.Stand.Richtung
            zeigen = neu != null
            if (neu != null) grad = neu.grad
            invalidate()
        }

        override fun onDraw(leinwand: Canvas) {
            val mx = width / 2f
            val my = height / 2f
            val r = minOf(mx, my) - 14f * b.dichte

            // Der feste Zeiger oben. Er steht auch dann da, wenn keine Messung
            // vorliegt -- sonst wirkt die Rose kaputt statt unvermessen.
            val spitze = Path().apply {
                moveTo(mx, my - r - 11f * b.dichte)
                lineTo(mx - 8f * b.dichte, my - r + 3f * b.dichte)
                lineTo(mx + 8f * b.dichte, my - r + 3f * b.dichte)
                close()
            }
            leinwand.drawPath(spitze, zeiger)
            leinwand.drawCircle(mx, my, r, duenn)
            if (!zeigen) return

            leinwand.save()
            leinwand.rotate(-grad, mx, my)
            var winkel = 0
            while (winkel < 360) {
                val lang = winkel % 45 == 0
                val bogen = Math.toRadians(winkel - 90.0)
                val innen = if (lang) r - 13f * b.dichte else r - 7f * b.dichte
                leinwand.drawLine(
                    mx + (innen * Math.cos(bogen)).toFloat(),
                    my + (innen * Math.sin(bogen)).toFloat(),
                    mx + (r * Math.cos(bogen)).toFloat(),
                    my + (r * Math.sin(bogen)).toFloat(),
                    if (lang) strich else duenn,
                )
                winkel += 15
            }
            for ((wert, name) in listOf(0 to "N", 90 to "O", 180 to "S", 270 to "W")) {
                val bogen = Math.toRadians(wert - 90.0)
                val abstand = r - 32f * b.dichte
                val x = mx + (abstand * Math.cos(bogen)).toFloat()
                val y = my + (abstand * Math.sin(bogen)).toFloat() + schrift.textSize / 3f
                // Die Beschriftung dreht mit der Rose zurueck, damit sie lesbar
                // bleibt, statt auf dem Kopf zu stehen.
                leinwand.save()
                leinwand.rotate(grad, x, y)
                leinwand.drawText(name, x, y, if (wert == 0) nordfarbe else schrift)
                leinwand.restore()
            }
            leinwand.restore()
        }
    }
}
