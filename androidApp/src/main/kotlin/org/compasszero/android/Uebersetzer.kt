package org.compasszero.android

import android.app.Activity
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import org.compasszero.content.Phrase
import org.compasszero.content.PhraseGroup
import org.compasszero.content.Vergleichstext

// Fester Satzkatalog zum Zeigen: Wer verletzt in fremder Sprache Hilfe braucht,
// tippt keinen freien Text, sondern zeigt auf einen vorbereiteten Satz. Beide
// Sprachen stehen deshalb immer zusammen auf dem Bildschirm -- die eigene zum
// Finden, die fremde gross zum Hinhalten.
class Uebersetzer(
    private val gastgeber: Activity,
    private val paket: GeladenesPaket,
    private val neuBauen: () -> Unit,
) : Bereich {

    private enum class Ansicht { GRUPPEN, PHRASEN, DETAIL }

    override val name = "Übersetzer"
    override val bild = R.drawable.sym_uebersetzer

    private var ansicht = Ansicht.GRUPPEN
    private var ziel = ""
    private var offeneGruppe: String? = null
    private var offenePhrase: String? = null
    private var filter = ""
    // Waehrend gefiltert wird, steht in der obersten Liste kein Gruppen-, sondern
    // ein Satzeintrag. Diese Liste haelt fest, welcher -- sonst oeffnete ein
    // Tipp auf den dritten Treffer die dritte Gruppe.
    private var gefilterte: List<Pair<PhraseGroup, Phrase>> = emptyList()

    private lateinit var b: Bausteine
    private lateinit var suchfeld: EditText
    private lateinit var gruppenliste: ListView
    private lateinit var phrasenRueck: View
    private lateinit var phrasenkopf: TextView
    private lateinit var phrasenliste: ListView
    private lateinit var detailRueck: View
    private lateinit var detail: ScrollView
    private lateinit var detailblock: LinearLayout
    private lateinit var fusszeile: TextView
    private var gezeigteGruppen: List<PhraseGroup> = emptyList()
    private var gezeigtePhrasen: List<Phrase> = emptyList()

    // Die Sprache des Pakets ist die Sprache des Nutzers; der Katalog muss sie
    // enthalten, sonst gaebe es keine Seite zum Finden.
    private val eigene: String
        get() = eigeneSprache(paket.pack.manifest.language, paket.pack.phraseLanguages)

    private val fremde: List<String>
        get() = fremdeSprachen(eigene, paket.pack.phraseLanguages)

    override fun baue(b: Bausteine): View {
        this.b = b
        if (paket.pack.phrases.isEmpty()) {
            return leererKatalog("Dieses Inhaltspaket enthält keinen Phrasenkatalog.")
        }
        // Der Inhaltsschutz von core/content verlangt nur EINE Sprache, keine
        // zwei -- ein Katalog ohne Fremdsprache ist damit ein gueltiges Paket.
        // Ohne diese Pruefung ist die Fremdsprachenliste leer, und
        // "fremde.first()" darunter stuerzt die App beim Oeffnen dieses
        // Bereichs ab (NoSuchElementException). Ein Uebersetzer ohne zweite
        // Sprache ist nicht kaputt, sondern nutzlos -- also fehlt hier die
        // Fremdsprache, nicht der Bereich.
        if (fremde.isEmpty()) {
            return leererKatalog(
                "Dieser Phrasenkatalog enthält keine Fremdsprache zum Zeigen — nur " +
                    "„${sprachname(eigene)}“.",
            )
        }
        if (ziel.isBlank() || ziel !in fremde) ziel = fremde.first()

        val spalte = b.spalte()
        spalte.addView(sprachwahl(), b.breit())

        // Ohne Filter muss man acht Gruppen durchblaettern, um einen von 62
        // Saetzen zu finden. Der Satzkatalog steht bewusst NICHT im
        // Suchverzeichnis des Lexikons -- Saetze wie "Haben Sie Schmerzen?"
        // wuerden dort Notfall-Tipps verdraengen. Also bekommt er hier sein
        // eigenes Feld, das nur ihn durchsucht.
        suchfeld = EditText(gastgeber).apply {
            hint = "Satz suchen"
            textSize = b.stil.textGroesse
            typeface = b.stil.textSchrift
            setTextColor(b.stil.text)
            setHintTextColor(b.stil.gedaempft)
            background = b.randfeld()
            setPadding(b.stil.abstand / 2, b.stil.abstand / 2, b.stil.abstand / 2, b.stil.abstand / 2)
            setSingleLine()
            setText(filter)
            setSelection(filter.length)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    filter = s?.toString().orEmpty()
                    offeneGruppe = null
                    offenePhrase = null
                    zeigeGruppenliste()
                }

                override fun beforeTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, c: Int, d: Int) = Unit
            })
        }
        spalte.addView(
            suchfeld,
            b.breit().apply { topMargin = b.stil.abstand / 2; bottomMargin = b.stil.abstand / 2 },
        )

        gruppenliste = ListView(gastgeber).apply {
            divider = null
            setBackgroundColor(b.stil.hintergrund)
            setOnItemClickListener { _, _, pos, _ ->
                if (filter.isBlank()) {
                    gezeigteGruppen.getOrNull(pos)?.let { zeigeGruppe(it) }
                } else {
                    gefilterte.getOrNull(pos)?.let { (gruppe, phrase) ->
                        offeneGruppe = gruppe.id
                        gezeigtePhrasen = gruppe.phrases
                        phrasenkopf.text = titel(gruppe)
                        zeigePhrase(phrase)
                    }
                }
            }
        }
        spalte.addView(gruppenliste, b.dehnbar())

        phrasenRueck = rueckzeile()
        spalte.addView(phrasenRueck, b.breit())
        phrasenkopf = TextView(gastgeber).apply {
            textSize = b.stil.textGroesse
            typeface = b.stil.ueberschriftSchrift
            setTextColor(b.stil.text)
            setPadding(0, b.stil.abstand / 2, 0, b.stil.abstand / 2)
        }
        spalte.addView(phrasenkopf, b.breit())
        phrasenliste = ListView(gastgeber).apply {
            divider = null
            setBackgroundColor(b.stil.hintergrund)
            setOnItemClickListener { _, _, pos, _ ->
                gezeigtePhrasen.getOrNull(pos)?.let { zeigePhrase(it) }
            }
        }
        spalte.addView(phrasenliste, b.dehnbar())

        detailRueck = rueckzeile()
        spalte.addView(detailRueck, b.breit())
        detailblock = b.spalte().apply { setPadding(0, b.stil.abstand, 0, b.stil.abstand) }
        detail = ScrollView(gastgeber).apply {
            addView(detailblock)
            setBackgroundColor(b.stil.hintergrund)
        }
        spalte.addView(detail, b.dehnbar())

        // Nach DESIGN.md ist der feste Katalog Stufe eins und als ausbaubar
        // gekennzeichnet -- damit niemand mehr erwartet, als die App kann.
        // Mit Linie und Abstand darueber: Ohne beides klebte der Satz direkt
        // unter dem letzten Listeneintrag und las sich wie dessen Beschreibung.
        // Auf dem Bildschirm sah es aus, als haette die letzte Gruppe keine
        // Satzzahl.
        val (linie, linienmasse) = b.trennlinie()
        spalte.addView(linie, linienmasse)
        fusszeile = b.nebentext(
            "Fester Satzkatalog. Freie Übersetzung und das Lesen von Schildern " +
                "über die Kamera sind spätere Ausbaustufen.",
        ).apply { setPadding(0, b.stil.abstand / 2, 0, b.stil.abstand / 2) }
        spalte.addView(fusszeile, b.breit())

        stelleWiederHer()
        return spalte
    }

    private fun leererKatalog(text: String): View {
        val spalte = b.spalte().apply { setPadding(0, b.stil.abstand * 2, 0, b.stil.abstand) }
        spalte.addView(b.ueberschrift("Übersetzer"), b.breit())
        spalte.addView(b.fliesstext(text), b.breit())
        return ScrollView(gastgeber).apply {
            addView(spalte)
            setBackgroundColor(b.stil.hintergrund)
        }
    }

    // Die Zielsprache traegt ihren eigenen Namen, nicht den deutschen: Das
    // Gegenueber soll seine Sprache auf dem Bildschirm wiedererkennen koennen.
    private fun sprachwahl(): View {
        val reihe = b.reihe()
        for (code in fremde) {
            val gewaehlt = code == ziel
            reihe.addView(
                TextView(gastgeber).apply {
                    text = sprachname(code)
                    textSize = if (b.sparmodus) 17f else 14f
                    typeface = if (gewaehlt) {
                        Typeface.create(b.stil.textSchrift, Typeface.BOLD)
                    } else {
                        b.stil.textSchrift
                    }
                    gravity = Gravity.CENTER
                    setTextColor(if (gewaehlt) b.stil.text else b.stil.gedaempft)
                    if (gewaehlt) background = b.randfeld()
                    setSingleLine()
                    minHeight = (48 * b.dichte).toInt()
                    setPadding(b.stil.abstand / 2, 0, b.stil.abstand / 2, 0)
                    setOnClickListener {
                        ziel = code
                        // Der Sprachwechsel aendert jede sichtbare Zeile; der
                        // einfachste verlaessliche Weg ist derselbe wie beim
                        // Stilwechsel: alles neu.
                        neuBauen()
                    }
                },
                LinearLayout.LayoutParams(Bausteine.WRAP, Bausteine.WRAP).apply {
                    marginEnd = b.stil.abstand / 3
                },
            )
        }
        return HorizontalScrollView(gastgeber).apply {
            isHorizontalScrollBarEnabled = false
            addView(reihe, ViewGroup.LayoutParams(Bausteine.MATCH, Bausteine.WRAP))
        }
    }

    private fun rueckzeile(): View = TextView(gastgeber).apply {
        text = "‹  Zurück"
        textSize = b.stil.textGroesse
        typeface = Typeface.create(b.stil.textSchrift, Typeface.BOLD)
        setTextColor(b.stil.text)
        gravity = Gravity.CENTER_VERTICAL
        minHeight = (48 * b.dichte).toInt()
        setOnClickListener { zurueck() }
    }

    private fun stelleWiederHer() {
        val gruppe = offeneGruppe?.let { offen -> paket.pack.phrases.firstOrNull { it.id == offen } }
        val phrase = offenePhrase?.let { offen ->
            gruppe?.phrases?.firstOrNull { it.id == offen }
        }
        zeigeGruppenliste()
        if (gruppe != null) zeigeGruppe(gruppe)
        if (gruppe != null && phrase != null) zeigePhrase(phrase)
    }

    override fun aufAnfang() {
        ansicht = Ansicht.GRUPPEN
        offeneGruppe = null
        offenePhrase = null
    }

    override fun zurueck(): Boolean = when {
        offenePhrase != null -> {
            offenePhrase = null
            val gruppe = offeneGruppe?.let { offen -> paket.pack.phrases.firstOrNull { it.id == offen } }
            if (gruppe != null) zeigeGruppe(gruppe) else zeigeGruppenliste()
            true
        }

        offeneGruppe != null -> {
            offeneGruppe = null
            zeigeGruppenliste()
            true
        }

        else -> false
    }

    private fun zeigeGruppenliste() {
        ansicht = Ansicht.GRUPPEN
        offeneGruppe = null
        offenePhrase = null
        if (filter.isNotBlank()) {
            zeigeTreffer()
            return
        }
        gefilterte = emptyList()
        gezeigteGruppen = paket.pack.phrases
        val zeilen = gezeigteGruppen.map { gruppe ->
            val anzahl = gruppe.phrases.size
            "${titel(gruppe)}\n$anzahl ${if (anzahl == 1) "Satz" else "Sätze"}"
        }
        gruppenliste.adapter = zeilenAdapter(zeilen, zweiteZeileGedaempft = true)
        stelleAnsichtHer()
    }

    // Gesucht wird in BEIDEN Sprachen: in der eigenen, weil man den Satz dort
    // im Kopf hat, und in der Zielsprache, weil man manchmal etwas
    // wiedererkennt, das das Gegenueber gesagt hat. Unter jedem Treffer steht
    // seine Gruppe -- sonst weiss man nicht, wo man gelandet ist.
    private fun zeigeTreffer() {
        gezeigteGruppen = emptyList()
        gefilterte = paket.pack.phrases.flatMap { gruppe ->
            gruppe.phrases.filter { phrase ->
                val eigen = phrase.text[eigene].orEmpty()
                val fremd = phrase.text[ziel].orEmpty()
                Vergleichstext.enthaelt(eigen, filter) || Vergleichstext.enthaelt(fremd, filter)
            }.map { gruppe to it }
        }
        val zeilen = gefilterte.map { (gruppe, phrase) ->
            "${phrase.text[eigene].orEmpty()}\n${titel(gruppe)}"
        }
        gruppenliste.adapter = if (zeilen.isEmpty()) {
            zeilenAdapter(listOf("Kein Satz enthält „$filter“"), zweiteZeileGedaempft = false)
        } else {
            zeilenAdapter(zeilen, zweiteZeileGedaempft = true)
        }
        stelleAnsichtHer()
    }

    private fun zeigeGruppe(gruppe: PhraseGroup) {
        ansicht = Ansicht.PHRASEN
        offeneGruppe = gruppe.id
        offenePhrase = null
        phrasenkopf.text = titel(gruppe)
        gezeigtePhrasen = gruppe.phrases
        val zeilen = gezeigtePhrasen.map { "${it.text[eigene].orEmpty()}\n${it.text[ziel].orEmpty()}" }
        phrasenliste.adapter = zeilenAdapter(zeilen, zweiteZeileGedaempft = true)
        phrasenliste.setSelectionAfterHeaderView()
        stelleAnsichtHer()
    }

    private fun zeigePhrase(phrase: Phrase) {
        ansicht = Ansicht.DETAIL
        offenePhrase = phrase.id
        detailblock.removeAllViews()

        detailblock.addView(b.nebentext(sprachname(eigene)), b.breit())
        detailblock.addView(b.fliesstext(phrase.text[eigene].orEmpty()), b.breit())
        phrase.note[eigene]?.let { notiz ->
            // Eine Notiz, die vor etwas warnt, gehoert in den Signalton -- etwa
            // die polnische Zeile, deren Ja das Gegenteil des deutschen Ja ist.
            val ansicht = if (notiz.startsWith("Achtung")) b.warnung(notiz) else b.nebentext(notiz)
            detailblock.addView(ansicht, b.breit())
        }

        detailblock.addView(b.trennstrich(), b.strichbreit())
        detailblock.addView(
            b.nebentext(sprachname(ziel)).apply { setPadding(0, b.stil.abstand, 0, 0) },
            b.breit(),
        )
        // Die fremde Sprache ist die Zeile zum Hinhalten -- sie bekommt die
        // groesste Schrift auf diesem Bildschirm.
        detailblock.addView(
            TextView(gastgeber).apply {
                text = phrase.text[ziel].orEmpty()
                textSize = b.stil.titelGroesse
                typeface = b.stil.textSchrift
                setTextColor(b.stil.text)
                setPadding(0, b.stil.abstand / 3, 0, b.stil.abstand)
            },
            b.breit(),
        )

        val gruppe = paket.pack.phrases.firstOrNull { it.id == offeneGruppe }
        if (gruppe != null && gruppe.sources.isNotEmpty()) {
            detailblock.addView(b.trennstrich(), b.strichbreit())
            detailblock.addView(b.ueberschrift("Quellen"), b.breit())
            detailblock.addView(
                b.nebentext(gruppe.sources.joinToString("\n\n") { "${it.name}\n${it.detail}" }),
                b.breit(),
            )
        }
        stelleAnsichtHer()
        detail.scrollTo(0, 0)
    }

    private fun stelleAnsichtHer() {
        gruppenliste.visibility = if (ansicht == Ansicht.GRUPPEN) View.VISIBLE else View.GONE
        fusszeile.visibility = if (ansicht == Ansicht.GRUPPEN) View.VISIBLE else View.GONE
        phrasenRueck.visibility = if (ansicht == Ansicht.PHRASEN) View.VISIBLE else View.GONE
        phrasenkopf.visibility = if (ansicht == Ansicht.PHRASEN) View.VISIBLE else View.GONE
        phrasenliste.visibility = if (ansicht == Ansicht.PHRASEN) View.VISIBLE else View.GONE
        detailRueck.visibility = if (ansicht == Ansicht.DETAIL) View.VISIBLE else View.GONE
        detail.visibility = if (ansicht == Ansicht.DETAIL) View.VISIBLE else View.GONE
    }

    private fun zeilenAdapter(zeilen: List<String>, zweiteZeileGedaempft: Boolean) =
        object : ArrayAdapter<String>(gastgeber, 0, zeilen) {
            override fun getView(pos: Int, alt: View?, eltern: ViewGroup): View {
                val zeile = zeilen[pos]
                val umbruch = zeile.indexOf('\n')
                val spalte = (alt as? LinearLayout) ?: LinearLayout(gastgeber).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(gastgeber))
                    addView(TextView(gastgeber))
                }
                val erste = spalte.getChildAt(0) as TextView
                val zweite = spalte.getChildAt(1) as TextView
                erste.text = if (umbruch >= 0) zeile.substring(0, umbruch) else zeile
                erste.textSize = b.stil.listenGroesse
                erste.typeface = b.stil.textSchrift
                erste.setTextColor(b.stil.text)
                zweite.text = if (umbruch >= 0) zeile.substring(umbruch + 1) else ""
                zweite.textSize = b.stil.listenGroesse * 0.85f
                zweite.typeface = b.stil.textSchrift
                zweite.setTextColor(if (zweiteZeileGedaempft) b.stil.gedaempft else b.stil.text)
                zweite.visibility = if (zweite.text.isBlank()) View.GONE else View.VISIBLE
                spalte.setPadding(0, b.stil.abstand / 2, 0, b.stil.abstand / 2)
                return spalte
            }
        }

    private fun titel(gruppe: PhraseGroup) = gruppe.title[eigene] ?: gruppe.title.values.first()

    private fun sprachname(code: String) = when (code) {
        "de" -> "Deutsch"
        "en" -> "English"
        "fr" -> "Français"
        "es" -> "Español"
        "it" -> "Italiano"
        "pl" -> "Polski"
        else -> code
    }

    companion object {
        // Als reine Funktionen herausgezogen, damit sich der Fall "Katalog
        // ohne Fremdsprache" pruefen laesst, ohne eine Activity zu bauen.
        internal fun eigeneSprache(paketsprache: String, katalog: List<String>): String =
            paketsprache.takeIf { it in katalog } ?: katalog.firstOrNull().orEmpty()

        internal fun fremdeSprachen(eigene: String, katalog: List<String>): List<String> =
            katalog.filter { it != eigene }
    }
}
