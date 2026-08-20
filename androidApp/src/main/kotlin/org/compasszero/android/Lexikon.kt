package org.compasszero.android

import android.app.Activity
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import org.compasszero.content.BuildGuide
import org.compasszero.content.Chapter
import org.compasszero.content.ContentGroup
import org.compasszero.content.ContentKind
import org.compasszero.content.SourceRef
import org.compasszero.content.Tip

// Das Nachschlagewerk: Kacheln als Einstieg, Suche darueber, Detailansicht
// darunter. Die drei Wissensarten des Pakets liegen im Datenmodell getrennt und
// treffen sich erst hier.
class Lexikon(
    private val gastgeber: Activity,
    private val paket: GeladenesPaket,
    private val gemerkt: Gemerkt,
    private val markeZeigen: (Boolean) -> Unit,
) : Bereich {

    private enum class Ansicht { KACHELN, GRUPPEN, LISTE, DETAIL }

    private class Kachel(val schluessel: String, val name: String, val bild: Int)

    // Eine Zeile der Liste. Traegt "gruppe" eine Ueberschrift, steht sie als
    // Zwischenzeile darueber -- so bleibt die Liste eine Liste und braucht keine
    // zweite Datenstruktur.
    private class Eintrag(
        val art: ContentKind,
        val id: String,
        val titel: String,
        val bereich: String,
        val gruppe: String = "",
    )

    override val name = "Lexikon"
    override val bild = R.drawable.sym_lexikon

    private var ansicht = Ansicht.KACHELN
    // Merkt sich, was zuletzt zu sehen war. Beim Neubau der Oberflaeche (etwa
    // nach dem Umschalten des Sparmodus) steht hier null, damit die erste
    // Ansicht wieder aufgeht statt hart zu erscheinen.
    private var letzteAnsicht: Ansicht? = null
    private var offeneKategorie: String? = null
    // Welche Untergruppe gerade offen ist. Null heisst: die ganze
    // Kategorie, also der Weg ueber "Alles anzeigen".
    private var offeneGruppe: String? = null
    private var offenerEintrag: Eintrag? = null

    // Die Lupe wird erst gebaut, wenn zum ersten Mal eine Skizze angetippt
    // wird -- wer nie eine anschaut, bezahlt nichts dafuer.
    private var bildschau: Bildschau? = null
    private var suchtext = ""

    private lateinit var b: Bausteine
    private lateinit var suchfeld: EditText
    private lateinit var loeschKnopf: TextView
    private lateinit var kacheln: ScrollView
    private lateinit var gitter: LinearLayout
    private lateinit var listRueck: View
    private lateinit var listenkopf: TextView
    private lateinit var liste: ListView
    private lateinit var detailRueck: View
    private lateinit var detail: ScrollView
    private lateinit var detailblock: LinearLayout
    private var gezeigt: List<Eintrag> = emptyList()

    override fun baue(b: Bausteine): View {
        this.b = b
        letzteAnsicht = null
        val spalte = b.spalte()

        loeschKnopf = TextView(gastgeber).apply {
            text = "×"
            textSize = b.stil.textGroesse * 1.3f
            typeface = b.stil.textSchrift
            setTextColor(b.stil.gedaempft)
            gravity = Gravity.CENTER
            visibility = if (suchtext.isNotBlank()) View.VISIBLE else View.GONE
            setPadding(b.stil.abstand / 2, 0, b.stil.abstand / 2, 0)
            setOnClickListener { suchfeld.setText("") }
        }
        suchfeld = EditText(gastgeber).apply {
            hint = "Suchen, z. B. Blutung oder Wasser"
            textSize = b.stil.textGroesse
            typeface = b.stil.textSchrift
            setTextColor(b.stil.text)
            setHintTextColor(b.stil.gedaempft)
            background = b.randfeld()
            setPadding(
                b.stil.abstand / 2,
                b.stil.abstand / 2,
                b.stil.abstand * 3 / 2,
                b.stil.abstand / 2,
            )
            setSingleLine()
            setText(suchtext)
            setSelection(suchtext.length)
            // Waehrend getippt wird, nimmt die Tastatur das halbe Bild. Die
            // Wortmarke weicht dann -- zwei Treffer mehr sind mehr wert.
            setOnFocusChangeListener { _, hatFokus -> markeZeigen(!hatFokus) }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    suchtext = s?.toString().orEmpty()
                    loeschKnopf.visibility = if (suchtext.isNotBlank()) View.VISIBLE else View.GONE
                    offenerEintrag = null
                    if (suchtext.isBlank()) {
                        zeigeKacheln()
                    } else {
                        offeneKategorie = null
                        zeigeSuche(suchtext)
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) = Unit
            })
        }
        val suchrahmen = FrameLayout(gastgeber).apply {
            addView(suchfeld, FrameLayout.LayoutParams(Bausteine.MATCH, Bausteine.WRAP))
            addView(
                loeschKnopf,
                FrameLayout.LayoutParams(Bausteine.WRAP, Bausteine.MATCH, Gravity.END or Gravity.CENTER_VERTICAL),
            )
        }
        spalte.addView(
            suchrahmen,
            b.breit().apply { topMargin = b.stil.abstand / 2; bottomMargin = b.stil.abstand / 2 },
        )

        gitter = b.spalte()
        kacheln = ScrollView(gastgeber).apply {
            addView(gitter)
            setBackgroundColor(b.stil.hintergrund)
        }
        spalte.addView(kacheln, b.dehnbar())

        listRueck = rueckzeile()
        spalte.addView(listRueck, b.breit())

        listenkopf = TextView(gastgeber).apply {
            textSize = b.stil.textGroesse
            typeface = b.stil.ueberschriftSchrift
            setTextColor(b.stil.text)
            setPadding(0, b.stil.abstand / 2, 0, b.stil.abstand / 2)
        }
        spalte.addView(listenkopf, b.breit())

        liste = ListView(gastgeber).apply {
            divider = ColorDrawable(b.stil.trennlinie)
            dividerHeight = b.strichhoehe()
            setBackgroundColor(b.stil.hintergrund)
            setOnItemClickListener { _, _, pos, _ ->
                gezeigt.getOrNull(pos)?.let { zeigeDetail(it) }
            }
        }
        spalte.addView(liste, b.dehnbar())

        detailRueck = rueckzeile()
        spalte.addView(detailRueck, b.breit())

        detailblock = b.spalte().apply { setPadding(0, b.stil.abstand, 0, b.stil.abstand) }
        detail = ScrollView(gastgeber).apply {
            addView(detailblock)
            setBackgroundColor(b.stil.hintergrund)
        }
        spalte.addView(detail, b.dehnbar())

        stelleWiederHer()
        return spalte
    }

    // Erst die Liste darunter, dann der offene Eintrag -- sonst fuehrt die
    // Zurueck-Taste aus dem Eintrag in eine leere Liste.
    private fun stelleWiederHer() {
        val eintrag = offenerEintrag
        zeigeListenstand()
        if (eintrag != null) zeigeDetail(eintrag)
    }

    private fun zeigeListenstand() {
        val kachel = offeneKategorie?.let { offen -> kachelliste().firstOrNull { it.schluessel == offen } }
        val gruppe = offeneGruppe
        when {
            // War eine Untergruppe offen, muss genau die wiederkommen -- sonst
            // steht man nach dem Zurueck aus einem Tipp wieder ganz oben.
            kachel != null && gruppe != null -> {
                val wahl = gruppenDerKategorie(kachel.schluessel)
                    .firstOrNull { (it.id ?: "") == gruppe }
                if (wahl != null) zeigeGruppe(kachel, wahl) else zeigeAlle(kachel)
            }
            kachel != null -> zeigeKategorie(kachel)
            suchtext.isNotBlank() -> zeigeSuche(suchtext)
            else -> zeigeKacheln()
        }
    }

    // Erneutes Antippen von "Lexikon" in der unteren Leiste soll denselben
    // frischen Start ergeben wie der allererste Aufruf.
    override fun aufAnfang() {
        ansicht = Ansicht.KACHELN
        offeneKategorie = null
        offeneGruppe = null
        offenerEintrag = null
        suchtext = ""
    }

    // Der wachgehaltene Bildschirm haengt am Fenster der Activity, nicht an
    // dieser Ansicht -- er muss deshalb ausdruecklich verschwinden, wenn ein
    // anderer Bereich uebernimmt oder die App in den Hintergrund geht. Sonst
    // bliebe der Bildschirm an, waehrend laengst die Karte offen ist.
    override fun anhalten() {
        gastgeber.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // Kommt die App aus dem Hintergrund zurueck, waehrend hier noch derselbe
    // Erste-Hilfe-Eintrag offen ist, wurde die Ansicht nicht neu gebaut -- die
    // Flagge muss also von Hand wiederkommen, nicht nur ueber baue().
    override fun fortsetzen() {
        if (::b.isInitialized) aktualisiereBildschirmwach()
    }

    override fun zurueck(): Boolean = when {
        // Die Bildschau liegt ueber allem, also raeumt die Zurueck-Taste sie
        // zuerst weg -- sonst schloesse sie den Eintrag unter einem Bild, das
        // stehen bleibt.
        bildschau?.schliesse() == true -> true

        offenerEintrag != null -> {
            offenerEintrag = null
            zeigeListenstand()
            true
        }

        // Aus der Tippliste geht es zurueck in die Auswahl der Untergruppen --
        // nur wer ueber "Alles anzeigen" gekommen ist oder eine Kategorie ohne
        // Untergruppen offen hat, landet gleich wieder bei den Kacheln.
        ansicht == Ansicht.LISTE && offeneGruppe != null -> {
            val kachel = offeneKategorie?.let { offen ->
                kachelliste().firstOrNull { it.schluessel == offen }
            }
            offeneGruppe = null
            if (kachel != null) zeigeGruppen(kachel) else zeigeKacheln()
            true
        }

        offeneKategorie != null -> {
            zeigeKacheln()
            true
        }

        suchtext.isNotBlank() -> {
            suchfeld.setText("")
            true
        }

        else -> false
    }

    // Startzustand des Lexikons. Im Ernstfall weiss niemand, welches Wort er
    // tippen soll -- die Kacheln sind der Weg ohne Suchbegriff.
    private fun zeigeKacheln() {
        ansicht = Ansicht.KACHELN
        offeneKategorie = null
        offenerEintrag = null
        gitter.removeAllViews()

        var reihe: LinearLayout? = null
        val felder = kachelliste()
        felder.forEachIndexed { nr, kachel ->
            if (nr % 2 == 0) {
                reihe = b.reihe()
                gitter.addView(reihe, b.breit())
            }
            reihe?.addView(kachelfeld(kachel), LinearLayout.LayoutParams(0, Bausteine.MATCH, 1f))
        }
        if (felder.size % 2 == 1) {
            reihe?.addView(View(gastgeber), LinearLayout.LayoutParams(0, Bausteine.MATCH, 1f))
        }
        hinweiszeile()?.let {
            gitter.addView(b.trennstrich(), b.strichbreit())
            gitter.addView(it, b.breit())
        }
        stelleAnsichtHer()
    }

    // Die sechs Dringlichkeitsfelder stehen fest. Faellt ein Eintrag durch --
    // etwa aus einem aelteren Paket, das das Feld noch nicht kennt --, bekommt
    // er eine Sammelkachel, sonst waere er ohne den passenden Suchbegriff nicht
    // mehr auffindbar.
    private fun kachelliste(): List<Kachel> {
        val fest = festeKacheln()
        val uebrig = paket.pack.tips.any { it.category !in gedeckteKategorien() }
        return if (uebrig) fest + Kachel(REST, "Weiteres", R.drawable.sym_lexikon) else fest
    }

    // Die festen Kacheln stehen an EINER Stelle, und die Menge der gedeckten
    // Kategorien wird daraus ABGELEITET.
    //
    // Vorher waren es zwei Listen, die von Hand gleichgehalten werden mussten.
    // Am 29.07.2026 kam die Kachel "Orientierung" dazu, und die zweite Liste
    // wurde vergessen. Folge: Alle sechs Orientierungs-Tipps standen DOPPELT.
    //
    // AM 04.08.2026 STANDEN HIER FUER EIN PAAR STUNDEN SECHS
    // DRINGLICHKEITSKACHELN (Jetzt sofort, Verletzt, Krank, Am Leben bleiben,
    // Unterwegs, Lage und Gefahr). Sie wurden am Geraet gesehen und
    // zurueckgewiesen -- damit sind sie weg, und zwar ohne Diskussion: In
    // diesem Projekt schlaegt das Urteil aus der Rueckmeldung jede Zaehlung.
    //
    // Genau dafuer war der Umbau rueckholbar gebaut. Das Feld `situations`
    // steht weiter im Paket und stoert dort niemanden; wer die Ansicht
    // zurueckhaben will, braucht nur diese Funktion und eintraegeDerKategorie()
    // wieder umzustellen. Der Befund, der dahinterstand, bleibt allerdings
    // bestehen: "Erste Hilfe" traegt 142 Eintraege. Was daran unuebersichtlich
    // war, muss anders geloest werden -- die Zwischenueberschriften innerhalb
    // einer Kategorie sind dafuer der Ansatz, sie sind seit demselben Tag nach
    // Dringlichkeit sortiert.
    private fun festeKacheln(): List<Kachel> = listOf(
        Kachel("erste-hilfe", "Erste Hilfe", R.drawable.sym_erste_hilfe),
        Kachel("medizin", "Medizin", R.drawable.sym_medizin),
        Kachel("wasser", "Wasser", R.drawable.sym_wasser),
        Kachel("nahrung", "Nahrung", R.drawable.sym_nahrung),
        Kachel(BAUANLEITUNGEN, "Bauanleitungen", R.drawable.sym_bauanleitungen),
        Kachel(AGRIKULTUR, "Agrikultur", R.drawable.sym_agrikultur),
        Kachel("orientierung", "Orientierung", R.drawable.sym_orientierung),
        Kachel("taktisch", "Taktisch", R.drawable.sym_taktisch),
    )

    // Abgeleitet und nicht noch einmal aufgezaehlt -- siehe festeKacheln().
    // HINWEIS kommt dazu, weil der Haftungshinweis eine eigene Kategorie hat,
    // aber bewusst keine Kachel: Er steht unter dem Kachelgitter.
    private fun gedeckteKategorien(): Set<String> =
        festeKacheln().map { it.schluessel }.toSet() + HINWEIS

    // Als eigenes, klar umrandetes Feld gebaut -- derselbe duenne Rand wie beim
    // Suchfeld --, mit fester Mindesthoehe, damit alle Kacheln einer Reihe
    // gleich hoch wirken, egal wie lang Name oder Zaehltext ausfallen. Der Name
    // traegt die Aufmerksamkeit, die Anzahl bleibt gedaempft und kleiner.
    private fun kachelfeld(kachel: Kachel): View {
        val anzahl = eintraegeDerKategorie(kachel.schluessel).size
        val leer = anzahl == 0
        // EINE Kachel traegt Vollton, und zwar die Erste Hilfe. Bis zum
        // 29.07.2026 waren alle acht gleich hell -- eine ordentliche, aber
        // hierarchielose Flaeche, auf der das Auge nirgends zuerst hinfaellt.
        // Die vorgegebene Vorlage setzt genau dagegen einen schwarzen Block
        // als Anker.
        //
        // Die Wahl ist nicht nur gestalterisch: Wer diese App im Ernstfall
        // aufmacht, sucht in den allermeisten Faellen genau das. Ein Blick soll
        // reichen.
        //
        // IM SPARMODUS NICHT. Dort ist der Grund ohnehin schwarz, ein
        // schwarzer Block waere unsichtbar -- und der Sparmodus wird nicht
        // mitgestaltet.
        val betont = !b.sparmodus && !leer && kachel.schluessel == "erste-hilfe"
        val feld = LinearLayout(gastgeber).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = b.kachelflaeche(leer, betont)
            minimumHeight = ((if (b.sparmodus) 132 else 116) * b.dichte).toInt()
            setPadding(b.stil.abstand / 2, b.stil.abstand, b.stil.abstand / 2, b.stil.abstand)
            if (!leer) {
                setOnClickListener { zeigeKategorie(kachel) }
                b.antippbewegung(this)
            }
        }
        feld.addView(
            ImageView(gastgeber).apply {
                setImageDrawable(
                    b.symbol(kachel.bild, if (b.sparmodus) 44 else 40, leer, aufDunkel = betont),
                )
            },
            LinearLayout.LayoutParams(Bausteine.WRAP, Bausteine.WRAP).apply {
                bottomMargin = b.stil.abstand / 2
            },
        )
        // Der Name traegt die Aufmerksamkeit und steht deshalb in der Serife --
        // dieselbe Schrift wie die Wortmarke und die Ueberschriften. Die Anzahl
        // darunter ist ein Messwert und tritt in fester Zeichenbreite zurueck.
        feld.addView(
            TextView(gastgeber).apply {
                text = kachel.name
                textSize = if (b.sparmodus) 17f else 16f
                gravity = Gravity.CENTER
                typeface = if (b.sparmodus) b.stil.textSchrift else b.stil.ueberschriftSchrift
                setTextColor(
                    when {
                        betont -> b.stil.hintergrund
                        leer -> b.stil.gedaempft
                        else -> b.stil.text
                    },
                )
            },
        )
        feld.addView(
            b.kennwert(
                if (leer) "noch keine Einträge" else zahlwort(anzahl),
                if (b.sparmodus) 13f else 9f,
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, b.stil.abstand / 4, 0, 0)
                // Auf dem Vollton muss auch der Messwert hell werden, sonst
                // verschwindet er. Nicht ganz so hell wie der Name -- er bleibt
                // die Nebenangabe.
                if (betont) setTextColor(b.stil.flaeche)
            },
            LinearLayout.LayoutParams(Bausteine.MATCH, Bausteine.WRAP),
        )
        return feld
    }

    // Der Haftungshinweis darf nicht hinter einem Suchbegriff verschwinden. Bis
    // die Erstanzeige beim ersten Start gebaut ist, steht er hier fest unter den
    // Kacheln.
    private fun hinweiszeile(): View? {
        val hinweis = paket.pack.tips.firstOrNull { it.category == HINWEIS } ?: return null
        return TextView(gastgeber).apply {
            text = hinweis.title
            textSize = if (b.sparmodus) 16f else 13f
            typeface = b.stil.textSchrift
            setTextColor(b.stil.signal)
            setPadding(0, b.stil.abstand, 0, b.stil.abstand)
            setOnClickListener { zeigeDetail(Eintrag(ContentKind.Tip, hinweis.id, hinweis.title, "Hinweis")) }
        }
    }

    // Eine Zeile der Gruppenauswahl.
    // Kennung der Zeile "Alles anzeigen". Ein Wert, den keine echte Gruppe
    // tragen kann.
    private val ALLES = " alles"

    private class Gruppenwahl(val id: String?, val titel: String, val anzahl: Int)

    /**
     * Antippen einer Kategorie.
     *
     * Bis zum 05.08.2026 fuehrte das sofort in eine Liste aller Eintraege der
     * Kategorie -- bei Erster Hilfe 144 Titel mit 26 Zwischenueberschriften.
     * Die Rueckmeldung dazu: unuebersichtlich. Die Einteilung war da, aber sie
     * war eine Ueberschrift in einer langen Liste und keine Ebene, an der man
     * abbiegen kann.
     *
     * Deshalb liegt jetzt eine Auswahl dazwischen. Sie kommt nur, wenn es
     * ueberhaupt mehr als eine Gruppe gibt -- bei einer einzigen waere ein
     * Zwischenschritt mit einem Eintrag nur ein zusaetzlicher Fingertipp.
     */
    private fun zeigeKategorie(kachel: Kachel) {
        offeneKategorie = kachel.schluessel
        offenerEintrag = null
        if (gruppenDerKategorie(kachel.schluessel).size > 1) {
            offeneGruppe = null
            zeigeGruppen(kachel)
        } else {
            zeigeAlle(kachel)
        }
    }

    /** Die ganze Kategorie am Stueck, wie bisher. */
    private fun zeigeAlle(kachel: Kachel) {
        offeneKategorie = kachel.schluessel
        offeneGruppe = null
        offenerEintrag = null
        val eintraege = eintraegeDerKategorie(kachel.schluessel)
        listenkopf.text = "${kachel.name} · ${zahlwort(eintraege.size)}"
        // In der Kategorie steht der Bereich schon in der Ueberschrift; ihn unter
        // jeden Titel zu schreiben, ist nur Laerm.
        setzeListe(eintraege, mitBereich = false)
        ansicht = Ansicht.LISTE
        stelleAnsichtHer()
    }

    /**
     * Die Untergruppen einer Kategorie, in der Reihenfolge des Pakets.
     *
     * Die ist nach Dringlichkeit gesetzt und nicht alphabetisch: Was in
     * Minuten entschieden wird, steht oben. Gruppen ohne einen einzigen
     * Eintrag tauchen nicht auf.
     */
    private fun gruppenDerKategorie(schluessel: String): List<Gruppenwahl> = when (schluessel) {
        // Bei Bauanleitungen IST die Kategorie die Untergruppe -- ein zweites
        // Feld daneben waere dieselbe Angabe zweimal.
        BAUANLEITUNGEN -> gruppenwahl(gruppenpaare(paket.pack.guideGroups), paket.pack.guides.map { it.category })
        AGRIKULTUR -> gruppenwahl(gruppenpaare(paket.pack.agricultureGroups), paket.pack.agriculture.map { it.group })
        in TIPP_KATEGORIEN -> gruppenwahl(
            paket.pack.tipGroups.filter { it.category == schluessel }.map { it.id to it.title },
            paket.pack.tips.filter { it.category == schluessel }.map { it.group },
        )
        else -> emptyList()
    }

    private fun gruppenpaare(gruppen: List<ContentGroup>) = gruppen.map { it.id to it.title }

    /**
     * Zaehlt je Gruppe die Eintraege und wirft leere Gruppen weg.
     *
     * [gruppen] steht in der Reihenfolge des Pakets -- die ist nach
     * Dringlichkeit gesetzt und nicht alphabetisch. [zuordnung] traegt fuer
     * jeden Eintrag des Bereichs seine Gruppenkennung, in derselben Zahl wie
     * die Eintraege. Was das Paket nicht kennt, faellt nicht heraus, sondern
     * landet unter "Weitere".
     */
    private fun gruppenwahl(gruppen: List<Pair<String, String>>, zuordnung: List<String>): List<Gruppenwahl> {
        val anzahl = zuordnung.groupingBy { it }.eachCount()
        val out = ArrayList<Gruppenwahl>()
        for ((id, titel) in gruppen) {
            val n = anzahl[id] ?: 0
            if (n > 0) out.add(Gruppenwahl(id, titel, n))
        }
        val bekannt = gruppen.map { it.first }.toSet()
        val rest = zuordnung.count { it !in bekannt }
        if (rest > 0) out.add(Gruppenwahl(null, "Weitere", rest))
        return out
    }

    private fun zeigeGruppen(kachel: Kachel) {
        offeneKategorie = kachel.schluessel
        offeneGruppe = null
        offenerEintrag = null
        val gruppen = gruppenDerKategorie(kachel.schluessel)
        val gesamt = eintraegeDerKategorie(kachel.schluessel).size
        listenkopf.text = "${kachel.name} · ${gruppen.size} Bereiche"
        setzeGruppenliste(kachel, gruppen, gesamt)
        ansicht = Ansicht.GRUPPEN
        stelleAnsichtHer()
    }

    private fun zeigeGruppe(kachel: Kachel, wahl: Gruppenwahl) {
        offeneKategorie = kachel.schluessel
        offeneGruppe = wahl.id ?: ""
        offenerEintrag = null
        val eintraege = when (kachel.schluessel) {
            BAUANLEITUNGEN -> {
                val bekannt = paket.pack.guideGroups.map { it.id }.toSet()
                paket.pack.guides
                    .filter { gehoert(it.category, wahl, bekannt) }
                    .map { Eintrag(ContentKind.Guide, it.id, it.title, "Bauanleitung") }
            }
            AGRIKULTUR -> {
                val bekannt = paket.pack.agricultureGroups.map { it.id }.toSet()
                paket.pack.agriculture
                    .filter { gehoert(it.group, wahl, bekannt) }
                    .map { Eintrag(ContentKind.Agriculture, it.id, it.title, "Agrikultur") }
            }
            else -> {
                val bekannt = paket.pack.tipGroups.map { it.id }.toSet()
                paket.pack.tips
                    .filter { it.category == kachel.schluessel && gehoert(it.group, wahl, bekannt) }
                    .map { Eintrag(ContentKind.Tip, it.id, it.title, kategoriename(it.category)) }
            }
        }
        listenkopf.text = "${wahl.titel} · ${zahlwort(eintraege.size)}"
        setzeListe(eintraege, mitBereich = false)
        ansicht = Ansicht.LISTE
        stelleAnsichtHer()
    }

    // "Weitere" (id == null) sammelt genau das ein, was keiner bekannten Gruppe
    // angehoert -- sonst waeren diese Eintraege in der Bereichsansicht unerreichbar.
    private fun gehoert(kennung: String, wahl: Gruppenwahl, bekannt: Set<String>) =
        if (wahl.id == null) kennung !in bekannt else kennung == wahl.id

    private fun zeigeSuche(anfrage: String) {
        val alle = alleEintraege().associateBy { it.art to it.id }
        val gefunden = paket.index.search(anfrage, limit = 40).mapNotNull { alle[it.kind to it.id] }
        listenkopf.text = when (gefunden.size) {
            0 -> "Kein Treffer für „$anfrage“"
            1 -> "1 Treffer"
            else -> "${gefunden.size} Treffer"
        }
        setzeListe(gefunden, mitBereich = true)
        ansicht = Ansicht.LISTE
        stelleAnsichtHer()
    }

    // Duenne Trennlinien scheiden die Zeilen; der Titel bleibt gut lesbar, die
    // Kategorie darunter tritt kleiner und gedaempft zurueck. Die Zwischen-
    // ueberschrift der Themengruppe steht in derselben Zeile ganz oben -- die
    // Liste bleibt dadurch eine Liste und die Zeilen bleiben wiederverwendbar.
    //
    // DIE UEBERSCHRIFT MUSS STAERKER SEIN ALS DIE EINTRAEGE, DIE SIE ORDNET.
    // Bis zum 04.08.2026 war sie kleiner (0,8-fach) und gedaempft, die Titel
    // darunter gross und schwarz. Damit war die Gliederung schwaecher als der
    // Inhalt, und "Erste Hilfe" mit 142 Eintraegen las sich als Wand aus
    // Titeln -- genau die Rueckmeldung vom 04.08.2026, die Einteilung im
    // Lexikon sei unuebersichtlich. Am Geraet nachgesehen und dort bestaetigt,
    // nicht aus dem Quelltext geschlossen.
    /**
     * Die Auswahl der Untergruppen.
     *
     * Bewusst eine schlichte Liste und kein Kachelraster: Erste Hilfe hat 26
     * Gruppen, Medizin 17. Als Kacheln waere das wieder eine Scrollstrecke,
     * nur mit mehr Weissraum -- der Zwischenschritt soll das Suchen kuerzen
     * und nicht verlaengern. Die Anzahl steht hinter jedem Namen, damit man
     * sieht, was einen erwartet.
     *
     * Ganz oben steht "Alles anzeigen": Der bisherige Weg durch die
     * vollstaendige Liste bleibt erhalten, fuer alle, die lieber blaettern.
     */
    private fun setzeGruppenliste(kachel: Kachel, gruppen: List<Gruppenwahl>, gesamt: Int) {
        val alles = Gruppenwahl(ALLES, "Alles anzeigen", gesamt)
        val zeilen = listOf(alles) + gruppen
        gezeigt = emptyList()
        liste.adapter = object : ArrayAdapter<Gruppenwahl>(gastgeber, 0, zeilen) {
            override fun getView(pos: Int, alt: View?, eltern: ViewGroup): View {
                val zeile = (alt as? LinearLayout) ?: b.spalte().apply {
                    setPadding(0, b.stil.abstand, 0, b.stil.abstand)
                    addView(TextView(gastgeber), b.breit())
                    addView(TextView(gastgeber), b.breit())
                }
                val wahl = zeilen[pos]
                (zeile.getChildAt(0) as TextView).apply {
                    text = wahl.titel
                    textSize = b.stil.listenGroesse * 1.05f
                    typeface = b.stil.ueberschriftSchrift
                    setTextColor(b.stil.text)
                }
                (zeile.getChildAt(1) as TextView).apply {
                    text = zahlwort(wahl.anzahl)
                    textSize = b.stil.listenGroesse * 0.7f
                    typeface = b.stil.textSchrift
                    setTextColor(b.stil.gedaempft)
                    setPadding(0, b.stil.abstand / 6, 0, 0)
                }
                return zeile
            }
        }
        liste.setOnItemClickListener { _, _, pos, _ ->
            val wahl = zeilen[pos]
            if (wahl.id == ALLES) zeigeAlle(kachel) else zeigeGruppe(kachel, wahl)
        }
        liste.setSelectionAfterHeaderView()
    }

    private fun setzeListe(eintraege: List<Eintrag>, mitBereich: Boolean) {
        gezeigt = eintraege
        // Die Gruppenauswahl setzt einen eigenen Klick-Umgang. Er muss hier
        // zurueckgesetzt werden, sonst oeffnet ein Tipp in der Liste danach
        // wieder eine Gruppe.
        liste.setOnItemClickListener { _, _, pos, _ ->
            gezeigt.getOrNull(pos)?.let { zeigeDetail(it) }
        }
        liste.adapter = object : ArrayAdapter<Eintrag>(gastgeber, 0, eintraege) {
            override fun getView(pos: Int, alt: View?, eltern: ViewGroup): View {
                val zeile = (alt as? LinearLayout) ?: b.spalte().apply {
                    setPadding(0, b.stil.abstand, 0, b.stil.abstand)
                    addView(gruppenstrich(), b.strichbreit())
                    addView(gruppenzeile(), b.breit())
                    addView(TextView(gastgeber), b.breit())
                    if (mitBereich) addView(TextView(gastgeber), b.breit())
                }
                val eintrag = eintraege[pos]
                val mitKopf = eintrag.gruppe.isNotEmpty()
                zeile.getChildAt(0).visibility = if (mitKopf) View.VISIBLE else View.GONE
                (zeile.getChildAt(1) as TextView).apply {
                    text = eintrag.gruppe
                    visibility = if (mitKopf) View.VISIBLE else View.GONE
                }
                (zeile.getChildAt(2) as TextView).apply {
                    text = eintrag.titel
                    textSize = b.stil.listenGroesse
                    typeface = b.stil.textSchrift
                    setTextColor(b.stil.text)
                }
                if (mitBereich) {
                    (zeile.getChildAt(3) as TextView).apply {
                        text = eintrag.bereich
                        textSize = b.stil.listenGroesse * 0.7f
                        typeface = b.stil.textSchrift
                        setTextColor(b.stil.gedaempft)
                        setPadding(0, b.stil.abstand / 6, 0, 0)
                    }
                }
                return zeile
            }
        }
        liste.setSelectionAfterHeaderView()
    }

    // Der Strich ueber einer Ueberschrift ist krisper als jeder Abstand: Er
    // sagt "hier faengt etwas Neues an", und zwar auch dann noch, wenn die
    // Ueberschrift gerade oben aus dem Bild geschoben wird.
    private fun gruppenstrich() = View(gastgeber).apply {
        setBackgroundColor(b.stil.text)
    }

    private fun gruppenzeile() = TextView(gastgeber).apply {
        textSize = b.stil.listenGroesse * 1.05f
        typeface = b.stil.ueberschriftSchrift
        setTextColor(b.stil.text)
        setPadding(0, b.stil.abstand * 3 / 4, 0, b.stil.abstand / 2)
    }

    private fun zeigeDetail(eintrag: Eintrag) {
        offenerEintrag = eintrag
        detailblock.removeAllViews()
        detailblock.addView(b.ueberschrift(eintrag.titel), b.breit())
        detailblock.addView(b.nebentext(eintrag.bereich), b.breit())
        when (eintrag.art) {
            ContentKind.Tip -> paket.pack.tips.firstOrNull { it.id == eintrag.id }?.let { baueTipp(it) }
            ContentKind.Guide -> paket.pack.guides.firstOrNull { it.id == eintrag.id }?.let { baueAnleitung(it) }
            ContentKind.Agriculture -> paket.pack.agriculture.firstOrNull { it.id == eintrag.id }?.let { baueKapitel(it) }
        }
        ansicht = Ansicht.DETAIL
        stelleAnsichtHer()
        detail.scrollTo(0, 0)
    }

    // Die Zeichnung steht VOR dem Text und nicht dahinter. Ein Tipp-Text ist
    // hier oft zweitausend Zeichen lang; ein Bild darunter faende niemand, der
    // es im Ernstfall braucht. Oben ist es das, was man zuerst sieht -- und
    // genau dafuer ist eine Zeichnung da.
    /**
     * Die Titel aller Eintraege, einmal gebaut.
     *
     * Gebraucht fuer die Verweise: Ein in Anfuehrungszeichen gesetzter Text
     * wird nur dann anklickbar, wenn er WIRKLICH ein Eintragstitel ist. Im
     * Paket stehen dieselben Zeichen auch um gewoehnliche Zitate.
     */
    private val nachTitel: Map<String, Eintrag> by lazy {
        alleEintraege().associateBy { it.titel }
    }

    /**
     * Alle Formen aller Fachwoerter, kleingeschrieben, mit ihrer Erklaerung.
     *
     * Kleingeschrieben, weil dasselbe Wort am Satzanfang gross steht und
     * mittendrin klein -- gesucht wird im Text ohne Ruecksicht darauf.
     */
    private val begriffe: Map<String, String> by lazy {
        val aus = HashMap<String, String>()
        for (term in paket.pack.terms) {
            aus[term.wort.lowercase()] = term.erklaerung
            for (form in term.auch) aus[form.lowercase()] = term.erklaerung
        }
        aus
    }

    private fun verweise() = Bausteine.Verweise(
        kennt = { nachTitel.containsKey(it) },
        oeffne = { titel -> nachTitel[titel]?.let { zeigeDetail(it) } },
        begriffe = begriffe,
        zeigeBegriff = { wort, erklaerung -> zeigeBegriff(wort, erklaerung) },
    )

    /**
     * Zeigt die Erklaerung eines Fachworts.
     *
     * Ein eigenes Fenster und keine Kurzmeldung: Zwei Saetze sind zu lang fuer
     * eine Einblendung, die nach drei Sekunden verschwindet, und wer im
     * Ernstfall nachschlaegt, will lesen koennen, ohne sich zu hetzen.
     */
    private fun zeigeBegriff(wort: String, erklaerung: String) {
        android.app.AlertDialog.Builder(gastgeber)
            .setTitle(wort)
            .setMessage(erklaerung)
            .setPositiveButton("Verstanden") { fenster, _ -> fenster.dismiss() }
            .show()
    }

    private fun baueTipp(tipp: Tip) {
        bildplatz(tipp.image)?.let {
            detailblock.addView(it, b.breit().apply { bottomMargin = b.stil.abstand / 2 })
        }
        detailblock.addView(b.artikel(tipp.body, verweise()), b.breit())
        baueQuellen(tipp.sources)
    }

    private fun baueAnleitung(anleitung: BuildGuide) {
        detailblock.addView(b.artikel(anleitung.summary, verweise()), b.breit())
        val aufwand = when (anleitung.difficulty) {
            1 -> "Einfach"
            2 -> "Mittel"
            else -> "Schwer"
        }
        detailblock.addView(b.nebentext("Aufwand: $aufwand"), b.breit())
        // Jedes Material bekommt eine eigene Zeile, und die faengt mit dem DING
        // an, nicht mit der Menge. Bis zum 02.08.2026 stand alles in einem
        // Absatz in der Reihenfolge Menge - Ding - Anmerkung, verbunden durch
        // Gedankenstriche. Das las sich als "eines - Metallstueck - Der mit
        // Abstand beste Werkstoff": Die Zeile begann mit einem Wort, das ohne
        // das folgende Ding gar nichts bedeutet, und weil alle Materialien in
        // EINEM Textfeld standen, war ausserdem nicht zu sehen, wo eines
        // aufhoert und das naechste anfaengt.
        //
        // Die Anmerkung steht jetzt als Nebentext darunter -- kleiner und
        // gedaempft. Damit traegt die Gliederung sich selbst, ohne Aufzaehlungs-
        // zeichen, die auf einem schmalen Bildschirm nur Platz kosten.
        if (anleitung.materials.isNotEmpty()) {
            detailblock.addView(b.ueberschrift("Material"), b.breit())
            for (m in anleitung.materials) {
                // Mittelpunkt statt Komma: Die Bezeichnung enthaelt oft selbst
                // Kommas ("Schnur, Sehne oder Draht"), und dann liest sich ein
                // angehaengtes ", ein bis zwei Meter" wie ein weiteres Material.
                val kopf = if (m.amount.isBlank()) m.item else "${m.item} · ${m.amount}"
                detailblock.addView(b.fliesstext(kopf), b.breit())
                if (m.note.isNotBlank()) detailblock.addView(b.nebentext(m.note), b.breit())
            }
        }
        if (anleitung.tools.isNotEmpty()) {
            detailblock.addView(b.ueberschrift("Werkzeug"), b.breit())
            for (werkzeug in anleitung.tools) {
                detailblock.addView(b.fliesstext(werkzeug), b.breit())
            }
        }
        detailblock.addView(b.ueberschrift("Schritte"), b.breit())
        anleitung.steps.forEachIndexed { nr, schritt ->
            detailblock.addView(b.artikel("${nr + 1}. ${schritt.text}"), b.breit())
            if (schritt.warning.isNotBlank()) {
                detailblock.addView(b.warnung(schritt.warning), b.breit())
            }
            bildplatz(schritt.image)?.let { detailblock.addView(it, b.breit()) }
        }
        baueQuellen(anleitung.sources)
    }

    private fun baueKapitel(kapitel: Chapter) {
        for (abschnitt in kapitel.sections) {
            detailblock.addView(b.ueberschrift(abschnitt.heading), b.breit())
            detailblock.addView(b.artikel(abschnitt.body), b.breit())
            bildplatz(abschnitt.image)?.let { detailblock.addView(it, b.breit()) }
        }
        baueQuellen(kapitel.sources)
    }

    // Der Quellenblock traegt spuerbar mehr Abstand nach oben als die uebrigen
    // Trennstriche -- er ist kein weiterer Absatz, sondern ein eigener Teil.
    // Getrennt wird er durch den Punktstreifen: Woher eine Angabe stammt, ist
    // in diesem Handbuch kein Kleingedrucktes, sondern ein eigener Abschnitt.
    private fun baueQuellen(quellen: List<SourceRef>) {
        detailblock.addView(
            b.rasterstreifen(),
            b.rasterbreit().apply { topMargin = b.stil.abstand; bottomMargin = b.stil.abstand / 2 },
        )
        detailblock.addView(b.ueberschrift("Quellen"), b.breit())
        detailblock.addView(b.nebentext(quellen.joinToString("\n\n") { "${it.name}\n${it.detail}" }), b.breit())
    }

    // Skizzen liegen im Paket und werden beim Anzeigen gegen die Pruefsumme aus
    // dem signierten Durchlauf gehalten. Was dabei schiefgeht, wird benannt und
    // nicht verschwiegen: Ein Schritt, der sich auf eine Skizze beruft, darf
    // nicht so aussehen, als sei er vollstaendig.
    //
    // Im Sparmodus bleiben Skizzen aus -- er hat einen eigenen Renderpfad ohne
    // Bilder. Dass sie es gibt, steht trotzdem da, sonst sucht niemand danach.
    private fun bildplatz(bild: String): View? {
        if (bild.isBlank()) return null
        if (b.sparmodus) return b.nebentext("Skizze vorhanden — im Sparmodus ausgeblendet")
        return when (val gelesen = Skizzen.laden(paket, bild)) {
            is Skizzen.Ergebnis.Da -> ImageView(gastgeber).apply {
                setImageBitmap(gelesen.bild)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_START
                setPadding(0, b.stil.abstand / 2, 0, b.stil.abstand / 2)
                // Antippen oeffnet die Lupe. In der Spalte des Artikels ist
                // die kleinste Beschriftung einer Skizze rund 14 Punkte hoch
                // -- lesbar wird sie erst gross und gedreht, siehe Bildlupe.
                contentDescription = "Skizze $bild — antippen zum Vergrößern"
                setOnClickListener {
                    val schau = bildschau ?: Bildschau(gastgeber, b.stil, b.dichte).also { neu -> bildschau = neu }
                    schau.zeige(gelesen.bild, offenerEintrag?.titel ?: "Skizze")
                }
            }

            Skizzen.Ergebnis.Fehlt -> b.nebentext("Skizze „$bild“ ist in diesem Paket nicht enthalten")
            is Skizzen.Ergebnis.Beschaedigt ->
                b.warnung("Skizze „$bild“ nicht verwendbar (${gelesen.grund}) — dieses Paket ist beschädigt")
        }
    }

    private fun stelleAnsichtHer() {
        kacheln.visibility = if (ansicht == Ansicht.KACHELN) View.VISIBLE else View.GONE
        val alsListe = ansicht == Ansicht.LISTE || ansicht == Ansicht.GRUPPEN
        listRueck.visibility = if (alsListe) View.VISIBLE else View.GONE
        liste.visibility = if (alsListe) View.VISIBLE else View.GONE
        listenkopf.visibility = if (alsListe) View.VISIBLE else View.GONE
        detailRueck.visibility = if (ansicht == Ansicht.DETAIL) View.VISIBLE else View.GONE
        detail.visibility = if (ansicht == Ansicht.DETAIL) View.VISIBLE else View.GONE
        aktualisiereBildschirmwach()

        // Nur beim tatsaechlichen Wechsel bewegen. zeigeSuche laeuft bei jedem
        // Tastendruck und laesst die Ansicht dabei auf LISTE stehen -- dort
        // waere es Flackern statt Bewegung.
        if (ansicht != letzteAnsicht) {
            letzteAnsicht = ansicht
            when (ansicht) {
                Ansicht.KACHELN -> b.aufgehen(kacheln)
                Ansicht.GRUPPEN -> b.aufgehen(liste)
                Ansicht.LISTE -> b.aufgehen(liste)
                Ansicht.DETAIL -> b.aufgehen(detail)
            }
        }
    }

    // Wer eine Herzdruckmassage oder eine andere Erste-Hilfe-Anleitung liest,
    // soll nicht nach dreissig Sekunden vor einem schwarzen Bildschirm stehen.
    // Deshalb bleibt der Bildschirm an, aber NUR solange genau so ein Eintrag
    // offen ist -- alles andere (Lexikon-Kacheln, ein Kapitel Agrikultur, eine
    // Bauanleitung) zehrt weiter normal am Akku. Im Sparmodus bleibt es aus:
    // Der ist fuer den knappen Akku da, und ein wachgehaltener Bildschirm waere
    // sein Gegenteil.
    private fun aktualisiereBildschirmwach() {
        val soll = ansicht == Ansicht.DETAIL && zeigtOffeneErsteHilfe() &&
            gemerkt.bildschirmBeiErsteHilfeAn && !b.sparmodus
        if (soll) {
            gastgeber.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            gastgeber.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun zeigtOffeneErsteHilfe(): Boolean {
        val eintrag = offenerEintrag ?: return false
        if (eintrag.art != ContentKind.Tip) return false
        return paket.pack.tips.firstOrNull { it.id == eintrag.id }?.category == "erste-hilfe"
    }

    // Deutliche Zurueck-Zeile fuer Trefferliste und Detailansicht -- dieselbe
    // Tippflaeche wie eine Schaltflaeche, derselbe Weg wie die Hardware-Taste.
    // Nur das kleine "‹" im Listenkopf war zu leicht zu uebersehen.
    private fun rueckzeile(): View {
        val block = b.spalte()
        val zeile = b.reihe().apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = (48 * b.dichte).toInt()
            isClickable = true
            isFocusable = true
            setOnClickListener { zurueck() }
            b.antippbewegung(this)
        }
        zeile.addView(
            TextView(gastgeber).apply {
                text = "‹  Zurück"
                textSize = b.stil.textGroesse
                typeface = Typeface.create(b.stil.textSchrift, Typeface.BOLD)
                setTextColor(b.stil.text)
            },
        )
        block.addView(zeile, b.breit())
        block.addView(b.trennstrich(), b.strichbreit())
        return block
    }

    private fun alleEintraege(): List<Eintrag> =
        paket.pack.tips.map { Eintrag(ContentKind.Tip, it.id, it.title, kategoriename(it.category)) } +
            paket.pack.guides.map { Eintrag(ContentKind.Guide, it.id, it.title, "Bauanleitung") } +
            paket.pack.agriculture.map { Eintrag(ContentKind.Agriculture, it.id, it.title, "Agrikultur") }

    private fun eintraegeDerKategorie(schluessel: String): List<Eintrag> = when (schluessel) {
        BAUANLEITUNGEN -> mitUeberschriften(
            gruppenpaare(paket.pack.guideGroups),
            paket.pack.guides.map { it.category to Eintrag(ContentKind.Guide, it.id, it.title, "Bauanleitung") },
        )
        AGRIKULTUR -> mitUeberschriften(
            gruppenpaare(paket.pack.agricultureGroups),
            paket.pack.agriculture.map { it.group to Eintrag(ContentKind.Agriculture, it.id, it.title, "Agrikultur") },
        )
        REST -> gruppiert(paket.pack.tips.filter { it.category !in gedeckteKategorien() })
        else -> gruppiert(paket.pack.tips.filter { it.category == schluessel })
    }

    // Ordnet die Tipps nach den Themengruppen des Pakets. Die Reihenfolge der
    // Gruppen steht im Paket und ist nach Dringlichkeit gesetzt, nicht
    // alphabetisch -- was in Minuten entschieden wird, steht oben. Innerhalb
    // einer Gruppe bleibt die Reihenfolge der Datei.
    //
    // Tipps ohne Gruppe fallen nicht heraus, sie haengen unter einer eigenen
    // Ueberschrift hinten an: ein aelteres Paket kennt noch keine Gruppen, und
    // ein Eintrag, der aus der Ansicht verschwindet, waere nur noch ueber die
    // Suche zu finden.
    private fun gruppiert(tipps: List<Tip>): List<Eintrag> = mitUeberschriften(
        paket.pack.tipGroups.map { it.id to it.title },
        tipps.map {
            it.group to Eintrag(ContentKind.Tip, it.id, it.title, kategoriename(it.category))
        },
    )

    /**
     * Setzt die Zwischenueberschriften. [zeilen] ist je Eintrag ein Paar aus
     * seiner Gruppenkennung und der fertigen Zeile; die Ueberschrift traegt
     * jeweils der erste Eintrag einer Gruppe.
     */
    private fun mitUeberschriften(
        gruppen: List<Pair<String, String>>,
        zeilen: List<Pair<String, Eintrag>>,
    ): List<Eintrag> {
        val reihenfolge = gruppen.withIndex().associate { (nr, g) -> g.first to nr }
        val ohneGruppe = reihenfolge.size
        val nachGruppe = zeilen.groupBy { if (it.first in reihenfolge) it.first else "" }
        val out = ArrayList<Eintrag>(zeilen.size)
        for ((kennung, mitglieder) in nachGruppe.entries.sortedBy { reihenfolge[it.key] ?: ohneGruppe }) {
            val ueberschrift = gruppen.firstOrNull { it.first == kennung }?.second
                ?: if (nachGruppe.size > 1) "Weitere" else ""
            mitglieder.forEachIndexed { nr, (_, zeile) ->
                out.add(
                    Eintrag(
                        zeile.art, zeile.id, zeile.titel, zeile.bereich,
                        gruppe = if (nr == 0) ueberschrift else "",
                    ),
                )
            }
        }
        return out
    }

    private fun kategoriename(schluessel: String) = when (schluessel) {
        "erste-hilfe" -> "Erste Hilfe"
        "medizin" -> "Medizin"
        "wasser" -> "Wasser"
        "nahrung" -> "Nahrung"
        "orientierung" -> "Orientierung"
        "taktisch" -> "Taktisch"
        HINWEIS -> "Hinweis"
        else -> schluessel
    }

    private companion object {
        const val BAUANLEITUNGEN = "bauanleitungen"
        const val AGRIKULTUR = "agrikultur"
        const val HINWEIS = "hinweis"
        const val REST = "weiteres"

        // Die Kategorien, deren Untergruppen aus den Tipp-Gruppen kommen.
        // Bauanleitungen und Agrikultur bringen ihre eigenen mit.
        val TIPP_KATEGORIEN = setOf("erste-hilfe", "medizin", "wasser", "nahrung", "taktisch", "orientierung")

        fun zahlwort(anzahl: Int) = if (anzahl == 1) "1 Eintrag" else "$anzahl Einträge"
    }
}
