package org.compasszero.android

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.StateListAnimator
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

// Alles, was mehr als ein Bereich braucht. Der Stil steckt hier drin, damit
// keine Ansicht ihre eigenen Groessen und Farben erfindet -- beim Umschalten in
// den Sparmodus wird alles neu gebaut, und was sich selbst faerbt, bleibt hell.
class Bausteine(private val gastgeber: Activity, val stil: Stil, val sparmodus: Boolean) {

    val dichte: Float = gastgeber.resources.displayMetrics.density

    fun ueberschrift(text: String) = TextView(gastgeber).apply {
        this.text = text
        textSize = stil.titelGroesse * 0.72f
        typeface = stil.ueberschriftSchrift
        setTextColor(stil.text)
        setPadding(0, stil.abstand, 0, stil.abstand / 3)
    }

    fun fliesstext(text: String) = TextView(gastgeber).apply {
        this.text = text
        textSize = stil.textGroesse
        typeface = stil.textSchrift
        setTextColor(stil.text)
        setPadding(0, 0, 0, stil.abstand / 2)
    }

    // Ein ganzer Eintragstext, gesetzt statt hingeschuettet. Die Gliederung
    // kommt aus dem Text selbst -- siehe Artikeltext, dort steht auch, warum es
    // das gibt.
    //
    // ZURUECKBAUEN heisst: hier wieder `fliesstext(text)` zurueckgeben. Am
    // Inhaltspaket ist dafuer nichts zu tun.
    /**
     * Ein Verweis von einem Eintrag auf einen anderen.
     *
     * WARUM ES DAS GIBT: Die Eintraege verweisen staendig aufeinander -- "siehe
     * „Ausspuelen statt auswischen“" steht dutzendfach im Paket, und ein Test
     * erzwingt sogar, dass jeder zitierte Titel wirklich existiert. Nur konnte
     * man ihm nicht folgen: Man musste den Namen lesen, zurueckgehen, suchen,
     * tippen. Max am 06.08.2026: "hyerlinks waeren sinnvoll wenn in einem der
     * artikel auf einen anderen verwiesen wird."
     *
     * [kennt] entscheidet, ob ein in Anfuehrungszeichen gesetzter Text ein
     * Eintragstitel ist -- Anfuehrungszeichen stehen im Paket auch um blosse
     * Zitate ("gut eingestellt"), und die duerfen nicht klickbar werden.
     */
    class Verweise(
        val kennt: (String) -> Boolean,
        val oeffne: (String) -> Unit,
        /**
         * Fachwoerter: Gibt zu einem Wort seine Erklaerung zurueck, sonst null.
         *
         * Die Formen stehen im Paket (content/terms.json) samt Beugungen; hier
         * wird nur nachgeschlagen. Ein Wort ohne Erklaerung bleibt normaler
         * Text.
         */
        val begriffe: Map<String, String> = emptyMap(),
        val zeigeBegriff: (String, String) -> Unit = { _, _ -> },
    )

    fun artikel(text: String, verweise: Verweise? = null): LinearLayout {
        val block = spalte()
        for (zeile in Artikeltext.zerlege(text)) {
            val view = when (zeile.art) {
                Artikeltext.Art.UEBERSCHRIFT -> zwischentitel(zeile.text)
                Artikeltext.Art.AUFZAEHLUNG ->
                    artikelzeile(zeile.text, einzug = zeile.einzug, verweise = verweise)
                Artikeltext.Art.FLIESSTEXT ->
                    artikelzeile(zeile.text, kicker = zeile.kicker, verweise = verweise)
            }
            block.addView(
                view,
                breit().apply {
                    // Zwischen Absaetzen ein spuerbarer Sprung, innerhalb eines
                    // Absatzes nur ein kleiner -- sonst zerfaellt eine
                    // Aufzaehlung in lauter Einzelsaetze.
                    bottomMargin = if (zeile.absatzende) stil.abstand * 3 / 4 else stil.abstand / 4
                },
            )
        }
        return block
    }

    // Eine Zeile, die im Text ganz aus Grossbuchstaben besteht, ist als
    // Zwischenueberschrift gemeint. Sie bekommt die Ueberschriftenschrift und
    // Luft darueber -- das ist die Stufe, an der das Auge beim Blaettern haengt.
    private fun zwischentitel(text: String) = TextView(gastgeber).apply {
        this.text = text
        textSize = stil.textGroesse
        typeface = Typeface.create(stil.ueberschriftSchrift, Typeface.BOLD)
        setTextColor(stil.text)
        setLineSpacing(0f, ZEILENABSTAND)
        setPadding(0, stil.abstand / 2, 0, 0)
    }

    // Zeilenabstand deutlich ueber dem Voreingestellten: Bei langen Bloecken ist
    // ein enger Satz das, was die Zeile verlieren laesst.
    //
    // "kicker" hebt den Anfang hervor, "einzug" haengt die Folgezeilen unter den
    // TEXT statt unter den Gedankenstrich -- ohne das sieht eine dreizeilige
    // Aufzaehlung aus wie Fliesstext mit einem Strich davor.
    private fun artikelzeile(
        text: String,
        kicker: Int = 0,
        einzug: Int = 0,
        verweise: Verweise? = null,
    ) =
        TextView(gastgeber).apply {
            val stellen = if (verweise == null) emptyList() else verweisstellen(text, verweise)
            val begriffe =
                if (verweise == null) emptyList() else begriffsstellen(text, verweise, stellen)
            this.text = if (kicker > 0 || einzug > 0 || stellen.isNotEmpty() || begriffe.isNotEmpty()) {
                SpannableString(text).apply {
                    if (kicker > 0) {
                        setSpan(StyleSpan(Typeface.BOLD), 0, kicker, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    if (einzug > 0) {
                        val breite = (einzug * stil.textGroesse * dichte * 0.6f).toInt()
                        setSpan(
                            LeadingMarginSpan.Standard(0, breite),
                            0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                    // Fachwoerter zuerst: Ein Verweistitel darf sie ueberschreiben,
                    // nicht umgekehrt -- in „Wunde: was verboten ist“ soll der
                    // ganze Titel springen und nicht ein Wort darin erklaeren.
                    for ((von, bis) in begriffe) {
                        val wort = text.substring(von, bis)
                        val erklaerung = verweise!!.begriffe[wort.lowercase()] ?: continue
                        setSpan(
                            object : android.text.style.ClickableSpan() {
                                override fun onClick(widget: View) =
                                    verweise.zeigeBegriff(wort, erklaerung)
                                override fun updateDrawState(farbe: android.text.TextPaint) {
                                    // Gedaempft statt in Textfarbe: Ein Fachwort
                                    // fuehrt nicht woandershin, es erklaert sich
                                    // nur. Der Unterschied zum Verweis muss man
                                    // nicht lernen -- man sieht ihn.
                                    farbe.isUnderlineText = true
                                    farbe.color = stil.gedaempft
                                }
                            },
                            von, bis, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                    for ((von, bis) in stellen) {
                        val titel = text.substring(von, bis)
                        // UNTERSTRICHEN, NICHT EINGEFAERBT. Die Signalfarbe
                        // gehoert in diesem Programm allein den Stellen, an
                        // denen ein Fehler gefaehrlich ist -- ein Verweis ist
                        // keine solche Stelle. Der Unterstrich sagt dasselbe
                        // und nimmt der Farbe nichts weg.
                        setSpan(
                            object : android.text.style.ClickableSpan() {
                                override fun onClick(widget: View) = verweise!!.oeffne(titel)
                                override fun updateDrawState(farbe: android.text.TextPaint) {
                                    farbe.isUnderlineText = true
                                    farbe.color = stil.text
                                }
                            },
                            von, bis, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                }
            } else {
                text
            }
            if (stellen.isNotEmpty() || begriffe.isNotEmpty()) {
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
                // Ohne das faerbt Android die ganze Zeile beim Antippen ein.
                highlightColor = android.graphics.Color.TRANSPARENT
            }
            textSize = stil.textGroesse
            typeface = stil.textSchrift
            setTextColor(stil.text)
            setLineSpacing(0f, ZEILENABSTAND)
        }


    /**
     * Findet die Fachwoerter im Text.
     *
     * Nur das ERSTE Vorkommen je Wort wird ausgezeichnet: Stuende "Bauchfell"
     * in einem Tipp viermal unterstrichen, sieht der Absatz aus wie ein
     * Formular. Einmal genuegt -- wer die Erklaerung gelesen hat, braucht sie
     * im selben Text nicht noch einmal angeboten.
     *
     * Stellen, die schon zu einem Verweistitel gehoeren, bleiben frei: In
     * "Wunde: was verboten ist" soll der ganze Titel springen und nicht ein
     * Wort darin etwas erklaeren.
     */
    private fun begriffsstellen(
        text: String,
        verweise: Verweise,
        schonBelegt: List<Pair<Int, Int>>,
    ): List<Pair<Int, Int>> {
        if (verweise.begriffe.isEmpty()) return emptyList()
        val aus = ArrayList<Pair<Int, Int>>()
        val schonGenannt = HashSet<String>()
        // Laengste Formen zuerst, sonst gewinnt "Sehne" gegen "Sehnen".
        for (form in verweise.begriffe.keys.sortedByDescending { it.length }) {
            var i = 0
            while (true) {
                val treffer = text.indexOf(form, i, ignoreCase = true)
                if (treffer < 0) break
                val ende = treffer + form.length
                val davor = if (treffer == 0) ' ' else text[treffer - 1]
                val danach = if (ende >= text.length) ' ' else text[ende]
                val ganzesWort = !davor.isLetter() && !danach.isLetter()
                val frei = schonBelegt.none { treffer < it.second && ende > it.first } &&
                    aus.none { treffer < it.second && ende > it.first }
                if (ganzesWort && frei && schonGenannt.add(form)) {
                    aus.add(Pair(treffer, ende))
                    break
                }
                i = ende
            }
        }
        return aus.sortedBy { it.first }
    }

    /**
     * Findet die Stellen, an denen ein Eintragstitel in Anfuehrungszeichen steht.
     *
     * Gesucht wird nach dem deutschen Paar „ … “. Was dazwischen steht, gilt
     * nur dann als Verweis, wenn es WIRKLICH ein Titel ist -- im Paket stehen
     * auch gewoehnliche Zitate in denselben Zeichen.
     */
    private fun verweisstellen(text: String, verweise: Verweise): List<Pair<Int, Int>> {
        val aus = ArrayList<Pair<Int, Int>>()
        var i = 0
        while (true) {
            val auf = text.indexOf('„', i)
            if (auf < 0) break
            val zu = text.indexOf('“', auf + 1)
            if (zu < 0) break
            val inhalt = text.substring(auf + 1, zu)
            if (verweise.kennt(inhalt)) aus.add(Pair(auf + 1, zu))
            i = zu + 1
        }
        return aus
    }

    // Duenne Linie zum Abtrennen. Am 03.08.2026 gebaut, weil die Fusszeile im
    // Uebersetzer ohne Trennung direkt unter dem letzten Listeneintrag klebte
    // und dadurch aussah wie dessen Beschreibung -- die letzte Gruppe schien
    // keine Satzzahl zu haben.
    //
    // ACHTUNG, HIER STECKT EINE FALLE: Ein blankes View mit WRAP_CONTENT wird
    // NICHT einen Punkt hoch, sondern so hoch wie der ganze verfuegbare Platz.
    // View.onMeasure gibt bei AT_MOST die volle Spec-Groesse zurueck und nicht
    // die Mindesthoehe. Beim ersten Versuch hat die Linie dadurch die komplette
    // Gruppenliste verdeckt -- ein grauer Block statt des Uebersetzers. Deshalb
    // gibt es die Hoehe hier fest vor, und deshalb kommen View und Masse als
    // Paar aus derselben Funktion: Wer nur trennlinie() nimmt und selbst
    // breit() dazusetzt, baut den Fehler wieder ein.
    fun trennlinie(): Pair<View, LinearLayout.LayoutParams> {
        val linie = View(gastgeber).apply { setBackgroundColor(stil.trennlinie) }
        val masse = LinearLayout.LayoutParams(MATCH, maxOf(1, dichte.toInt()))
        return linie to masse
    }

    fun nebentext(text: String) = TextView(gastgeber).apply {
        this.text = text
        textSize = stil.textGroesse * 0.82f
        typeface = stil.textSchrift
        setTextColor(stil.gedaempft)
        setPadding(0, 0, 0, stil.abstand / 2)
    }

    fun warnung(text: String) = TextView(gastgeber).apply {
        this.text = text
        textSize = stil.textGroesse
        typeface = Typeface.create(stil.textSchrift, Typeface.BOLD)
        setTextColor(stil.signal)
        setPadding(0, 0, 0, stil.abstand / 2)
    }

    // Feste Zeichenbreite, damit sich eine Pruefsumme Zeichen fuer Zeichen mit
    // einer notierten vergleichen laesst.
    fun kennzahl(text: String) = TextView(gastgeber).apply {
        this.text = text
        textSize = stil.textGroesse * 0.78f
        typeface = Typeface.MONOSPACE
        setTextColor(stil.text)
        setTextIsSelectable(true)
        setPadding(0, 0, 0, stil.abstand / 2)
    }

    fun trennstrich() = View(gastgeber).apply { setBackgroundColor(stil.trennlinie) }

    fun strichhoehe() = maxOf(1, dichte.toInt())

    fun strichbreit() = LinearLayout.LayoutParams(MATCH, strichhoehe())

    // Ein Eingabefeld muss als Eingabefeld erkennbar sein. Ohne Rand sieht das
    // Suchfeld aus wie eine Zeile Text, in die niemand tippt.
    fun randfeld() = GradientDrawable().apply {
        setColor(stil.hintergrund)
        setStroke(strichhoehe() * 2, stil.trennlinie)
        cornerRadius = 2f * dichte
    }

    // Eine Kachel ist ein Gegenstand auf dem Blatt, kein leerer Kasten: sie
    // bekommt eine eigene Flaeche und nur eine haarfeine Kante. Eine Kachel ohne
    // Inhalt bleibt beim blossen Rahmen -- der Unterschied zwischen "hier ist
    // etwas" und "hier ist noch nichts" soll man sehen, bevor man liest.
    //
    // Im Sparmodus bleibt es beim Rahmen: eine zweite Flaeche waere dort
    // bezahlter Strom fuer Zierde.
    fun kachelflaeche(leer: Boolean, betont: Boolean = false): Drawable {
        if (sparmodus || leer) return randfeld()
        return GradientDrawable().apply {
            setColor(if (betont) stil.marke else stil.flaeche)
            if (!betont) setStroke(strichhoehe(), stil.trennlinie)
            cornerRadius = 3f * dichte
        }
    }

    // Das Punktraster der Wortmarke als Flaechenmuster. Ein einziger Punkt wird
    // in ein winziges Bild gezeichnet und gekachelt -- das kostet auch auf einem
    // alten Geraet nichts und bringt das einzige grafische Zeichen der Marke auf
    // jede Seite.
    fun punktraster(): Drawable? {
        if (sparmodus) return null
        val kante = maxOf(4, (5 * dichte).toInt())
        val bild = Bitmap.createBitmap(kante, kante, Bitmap.Config.ARGB_8888)
        val stift = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = stil.raster }
        Canvas(bild).drawCircle(kante / 2f, kante / 2f, maxOf(1f, dichte * 0.85f), stift)
        return BitmapDrawable(gastgeber.resources, bild).apply {
            setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
    }

    // Ein Streifen aus diesem Raster statt eines Trennstrichs. Er trennt nicht
    // nur, er sagt auch, dass hier ein anderer Teil anfaengt.
    fun rasterstreifen(hoeheDp: Int = 6): View = View(gastgeber).apply {
        val muster = punktraster()
        if (muster == null) setBackgroundColor(stil.trennlinie) else background = muster
        minimumHeight = (hoeheDp * dichte).toInt()
    }

    fun rasterbreit(hoeheDp: Int = 6) = LinearLayout.LayoutParams(MATCH, (hoeheDp * dichte).toInt())

    // Zahlen und Zustandsangaben in fester Zeichenbreite mit weiten Abstaenden:
    // Sie sollen wie Messwerte aussehen und nicht wie Fliesstext, und sie sollen
    // sich nicht mit den Titeln um Aufmerksamkeit streiten.
    fun kennwert(text: String, groesse: Float) = TextView(gastgeber).apply {
        this.text = if (sparmodus) text else text.uppercase()
        textSize = groesse
        typeface = Typeface.MONOSPACE
        letterSpacing = if (sparmodus) 0f else 0.08f
        setTextColor(stil.gedaempft)
    }

    // Die Vektorgrafiken sind schwarz gezeichnet und tragen den orangen
    // Signalton dort, wo er hingehoert. Nur der Sparmodus faerbt sie ein: auf
    // reinem Schwarz waeren sie sonst unsichtbar.
    fun symbol(id: Int, groesseDp: Int, gedimmt: Boolean = false, aufDunkel: Boolean = false): Drawable {
        val bild = gastgeber.resources.getDrawable(id, gastgeber.theme).mutate()
        if (sparmodus) bild.setTint(stil.text)
        // Auf einer schwarzen Flaeche ist die schwarz gezeichnete Grafik
        // unsichtbar. Eingefaerbt wird auf die Hintergrundfarbe und nicht auf
        // reines Weiss, damit der Ton derselbe bleibt wie auf der Seite.
        if (aufDunkel && !sparmodus) bild.setTint(stil.hintergrund)
        if (gedimmt) bild.alpha = 90
        val kante = (groesseDp * dichte).toInt()
        bild.setBounds(0, 0, kante, kante)
        return bild
    }

    // Kurze Rueckmeldung beim Antippen: Das Element geht ein Stueck zurueck,
    // solange der Finger liegt. Ueber den StateListAnimator des Systems, nicht
    // ueber einen eigenen Beruehrungshorcher -- so bleibt die Bedienung fuer die
    // Bedienungshilfen unveraendert, und ausserhalb des Drucks laeuft nichts.
    //
    // Im Sparmodus wird der Animator ausdruecklich entfernt statt nur nicht
    // gesetzt: Ansichten werden beim Umschalten neu gebaut, aber ein
    // wiederverwendetes Element haette den alten sonst behalten.
    fun antippbewegung(sicht: View) {
        if (stil.bewegungMs == 0L) {
            sicht.stateListAnimator = null
            return
        }
        sicht.stateListAnimator = StateListAnimator().apply {
            addState(intArrayOf(android.R.attr.state_pressed), skalieren(sicht, 0.97f))
            addState(IntArray(0), skalieren(sicht, 1f))
        }
    }

    private fun skalieren(sicht: View, auf: Float) = AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(sicht, View.SCALE_X, auf),
            ObjectAnimator.ofFloat(sicht, View.SCALE_Y, auf),
        )
        duration = stil.bewegungMs
    }

    // Weiches Aufgehen beim Seitenwechsel: ein kurzer Weg von unten und aus der
    // Durchsichtigkeit heraus. Nur beim tatsaechlichen Wechsel aufrufen -- bei
    // jedem Tastendruck im Suchfeld waere es Flackern statt Bewegung.
    fun aufgehen(sicht: View) {
        if (stil.bewegungMs == 0L) return
        sicht.alpha = 0f
        sicht.translationY = 8f * dichte
        sicht.animate().alpha(1f).translationY(0f).setDuration(stil.bewegungMs).start()
    }

    fun spalte() = LinearLayout(gastgeber).apply { orientation = LinearLayout.VERTICAL }

    fun reihe() = LinearLayout(gastgeber).apply { orientation = LinearLayout.HORIZONTAL }

    fun breit() = LinearLayout.LayoutParams(MATCH, WRAP)

    fun dehnbar() = LinearLayout.LayoutParams(MATCH, 0, 1f)

    companion object {

        // Zeilenabstand im Artikelsatz. Deutlich ueber dem
        // Voreingestellten (etwa 1,2): Bei langen Bloecken ist ein enger Satz
        // das, was die Zeile verlieren laesst.
        const val ZEILENABSTAND = 1.45f
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }

}
