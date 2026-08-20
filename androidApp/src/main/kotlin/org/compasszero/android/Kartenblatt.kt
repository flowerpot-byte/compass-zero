package org.compasszero.android

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import org.compasszero.karte.Bilddatei
import org.compasszero.karte.Hoehendatei
import org.compasszero.karte.Kachel
import org.compasszero.karte.Kartendatei
import org.compasszero.karte.Kartenfehler
import org.compasszero.karte.Kartenformat
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sinh

/**
 * Die Karte selbst: zeichnet Kacheln, laesst sich schieben, zoomen und auf
 * Wunsch in Blickrichtung drehen.
 *
 * Ortung gibt es nicht und wird es nicht geben -- die App fragt keine
 * Berechtigung. Wo man steht, weiss man selbst; wohin man schaut, sagt der
 * Magnetfeldsensor.
 */
@SuppressLint("ViewConstructor")
class Kartenblatt(
    gastgeber: Context,
    /**
     * Alle geoeffneten Kartendateien.
     *
     * Eine Uebersicht deckt z4-z10, ein Detailpaket z11-z14 -- zusammen
     * ergeben sie EINE Karte, in die man durchzoomen kann. Die App nahm
     * zuerst nur die erstbeste Datei; damit hatte man entweder keine
     * Uebersicht oder keine Einzelheiten, und die in der Roadmap vorgesehene
     * Aufteilung ging nicht auf.
     */
    private val karten: List<Kartendatei>,
    private val stil: Kartenstil,
    private val dichte: Float,
    /** Die Gelaendeform, falls eine Hoehendatei danebenliegt. */
    private val hoehen: Hoehendatei? = null,
    private val bilder: Bilddatei? = null,
) : View(gastgeber) {

    init {
        require(karten.isNotEmpty()) { "ohne Kartendatei gibt es nichts zu zeichnen" }
    }

    private val zoomKleinste = karten.minOf { it.zoomKleinste }
    private val zoomGroesste = karten.maxOf { it.zoomGroesste }
    private val westen = karten.minOf { it.westen }
    private val osten = karten.maxOf { it.osten }
    private val sueden = karten.minOf { it.sueden }
    private val norden = karten.maxOf { it.norden }

    /**
     * Die Ebenen, die sich einzeln ein- und ausschalten lassen.
     *
     * Die Karte traegt zu jedem Punkt seine ART -- Quelle, Krankenhaus,
     * Huette und so weiter. Bis zum 05.08.2026 wurden alle gezeichnet oder
     * keiner. Wer eine Wasserstelle sucht, will aber nicht dieselbe Karte
     * sehen wie jemand, der ein Krankenhaus sucht.
     *
     * Die Zusammenfassung folgt der Frage, die man im Ernstfall stellt, nicht
     * der Ordnung der Datenquelle: "Wo ist Wasser", "Wo ist Hilfe", "Wo komme
     * ich unter", "Wie ist das Gelaende".
     */
    enum class Ebene(val anzeige: String, val arten: Set<String>) {
        WASSER("Wasser", setOf("quelle", "brunnen", "trinkwasser", "wasserturm")),
        HILFE("Hilfe", setOf("krankenhaus", "apotheke", "notruftelefon")),
        SCHUTZ("Schutz", setOf("huette", "unterstand", "hoehle")),
        GELAENDE("Gelände", setOf("gipfel", "sattel", "pass", "aussicht")),
        ORTE("Orte", setOf("grossstadt", "stadt", "dorf", "weiler", "einzellage")),

        /**
         * Das Einzige, was nicht aus der signierten Kartendatei stammt: was der
         * Nutzer selbst gesetzt hat. Deshalb steht hier keine Punktart -- es
         * gibt keine Art, die aus den Karten hierher fiele.
         */
        EIGEN("Meine", emptySet()),
    }

    /** Welche Ebenen gerade sichtbar sind. Zu Beginn alle. */
    private val sichtbar = Ebene.values().toMutableSet()

    fun ebeneAn(ebene: Ebene): Boolean = ebene in sichtbar

    fun schalteEbene(ebene: Ebene) {
        if (!sichtbar.remove(ebene)) sichtbar.add(ebene)
        invalidate()
    }

    /**
     * Zu welcher Ebene eine Punktart gehoert -- oder zu keiner.
     *
     * Eine Art, die zu KEINER Ebene gehoert (etwa "unbekannt"), wird immer
     * gezeigt: Sie stumm verschwinden zu lassen waere schlimmer, als sie zu
     * zeigen -- ein Punkt auf der Karte ist eine Auskunft, sein Fehlen sieht
     * aus wie "da ist nichts".
     */
    private fun ebeneVon(punktartName: String): Ebene? = ebeneJeArt[punktartName]

    /**
     * Welche Ebenen im aktuellen Bild ueberhaupt etwas zu zeigen haetten.
     *
     * WARUM DAS GEBRAUCHT WIRD: Ein Schalter, der nichts schaltet, sieht kaputt
     * aus. Rueckmeldung vom 06.08.2026: "irgendwie machen die overlay knoepfe
     * noch nicht wirklich viel". Das traf zu, und der Grund lag nicht am
     * Schalter -- in der Europa-Uebersicht gab es zu vier der fuenf Ebenen
     * schlicht keine Punkte (gemessen: 200 Kacheln der Stufe 10 enthielten 146
     * Punkte, und davon nur Pass und Krankenhaus).
     *
     * Die Daten sind nachgezogen, aber das Grundproblem bleibt: Auf einer
     * Uebersicht ueber halb Europa gibt es keine einzelne Quelle zu sehen, und
     * das ist richtig so. Dann muss der Schalter das SAGEN, statt stumm nichts
     * zu tun. Gezaehlt wird, was liegen WUERDE -- auch bei ausgeschalteter
     * Ebene, sonst koennte man nie erkennen, dass sich das Einschalten lohnt.
     */
    private val vorhanden = HashSet<Ebene>()
    private val gefunden = HashSet<Ebene>()

    /** Wird gerufen, wenn sich geaendert hat, welche Ebenen etwas zu zeigen haben. */
    var beiEbenen: (() -> Unit)? = null

    fun ebeneHatInhalt(ebene: Ebene): Boolean = ebene in vorhanden

    /**
     * Ab welcher Zoomstufe ein Ort ueberhaupt gezeigt wird.
     *
     * Die Kartendatei liefert alle Orte schon ab Zoom 4 -- vom
     * Millionen-Ballungsraum bis zur Einzellage. Auf dem ganzen Kontinent
     * gezeichnet ergibt das im Ruhrgebiet und in der Po-Ebene einen
     * schwarzen Klumpen, in dem kein Name mehr lesbar ist. Die Rueckmeldung
     * dazu war am 05.08.2026 schlicht: unuebersichtlich, wirkt wie im Bau.
     *
     * Also nach Groesse staffeln. Die Schwellen sind so gesetzt, dass auf
     * jeder Stufe ungefaehr gleich viele Namen im Bild stehen: Wer
     * herauszoomt, verliert die kleinen Orte und behaelt die, an denen man
     * sich ueber weite Strecken orientiert.
     *
     * Es geht dabei NUR um Ortschaften. Quellen, Huetten und Krankenhaeuser
     * werden nicht ausgeduennt -- die stehen erst ab Zoom 10 in der Datei und
     * sind genau das, wonach jemand sucht.
     */
    private fun ortPasstZumZoom(name: String): Boolean {
        val abZoom = when (name) {
            "grossstadt" -> 4
            "stadt" -> 6
            "dorf" -> 9
            "weiler" -> 11
            "einzellage" -> 12
            else -> return true
        }
        return kachelzoom() >= abZoom
    }

    /**
     * Eine Karte, die zu diesem Feld etwas beitraegt.
     *
     * [teilung] sagt, aus welcher Stufe der Beitrag WIRKLICH stammt: 1 heisst
     * "echte Kachel dieser Stufe", 4 heisst "ein Viertelfeld einer Kachel zwei
     * Stufen darueber, vergroessert nachgezeichnet". [teilX]/[teilY] ist das
     * Feld darin. Gebraucht wird das auch fuer die Beschriftung: Die Punkte
     * der Quellkachel liegen in DEREN Raster, nicht im Raster der angezeigten
     * Stufe.
     */
    private class Schicht(
        val kachel: Kachel,
        val teilung: Int,
        val teilX: Int,
        val teilY: Int,
        val rang: Int,
    )

    /** Ein fertiges Kachelbild und die Karten, aus denen es entstanden ist. */
    private class Eintrag(val bild: Bitmap, val schichten: List<Schicht>)

    /** Mitte der Ansicht in Grad. */
    var mitteLon: Double = 0.0
        private set
    var mitteLat: Double = 0.0
        private set

    /** Zoom mit Nachkommastellen -- zwischen zwei Kachelstufen wird skaliert. */
    private var zoomStufe: Float = zoomKleinste.toFloat()

    /** Blickrichtung in Grad; die Karte dreht sich dagegen. */
    private var blickrichtung: Float = 0f

    var drehtMitBlickrichtung: Boolean = false
        set(wert) {
            field = wert
            if (!wert) blickrichtung = 0f
            invalidate()
        }

    var beiZustand: ((String) -> Unit)? = null

    private var maler = Kachelmaler(stil, KACHELKANTE, ruhig = false)

    /**
     * Ruhige Karte an oder aus.
     *
     * Anders als die Ebenen-Schalter, die nur Punkte ein- und ausblenden,
     * steckt das hier in den fertig gezeichneten Kachelbildern. Der
     * Zwischenspeicher muss deshalb geleert werden -- sonst bliebe die alte
     * Zeichnung stehen, bis die Kachel zufaellig aus dem Speicher faellt.
     */
    var ruhigeKarte: Boolean = false
        set(wert) {
            if (field == wert) return
            field = wert
            neuerMaler()
        }

    /**
     * Ob das Satellitenbild als Untergrund gezeichnet wird.
     *
     * Wie bei der ruhigen Karte steckt das in den fertigen Kachelbildern und
     * nicht in einer Ebene darueber -- der Zwischenspeicher muss deshalb weg.
     */
    var zeigtBilder: Boolean = false
        set(wert) {
            if (field == wert) return
            field = wert
            neuerMaler()
        }

    /** Ob ueberhaupt ein Bildpaket danebenliegt. */
    val hatBilder: Boolean get() = bilder != null

    /** Sorten der gezeichneten Karte, die weggelassen werden. */
    var weggelassen: Set<Int> = emptySet()
        set(wert) {
            if (field == wert) return
            field = wert
            neuerMaler()
        }

    private fun neuerMaler() {
        // Flaechen weg, sobald ein Foto darunter liegt: Der gezeichnete Wald
        // wuerde es sonst zudecken.
        maler = Kachelmaler(
            stil,
            KACHELKANTE,
            ruhig = ruhigeKarte,
            flaechenlos = zeigtBilder && bilder != null,
            weglassen = weggelassen,
        )
        for (eintrag in speicher.values) eintrag.bild.recycle()
        speicher.clear()
        unterwegs.clear()
        ortsfelder.clear()
        invalidate()
    }
    private val speicher = object : LinkedHashMap<Long, Eintrag>(16, 0.75f, true) {
        override fun removeEldestEntry(aeltester: MutableMap.MutableEntry<Long, Eintrag>?): Boolean {
            if (size <= AUFGEHOBEN) return false
            aeltester?.value?.bild?.recycle()
            return true
        }
    }
    private val unterwegs = HashSet<Long>()

    /**
     * Stellen, an denen KEINE Karte etwas hat.
     *
     * WARUM DAS GEBRAUCHT WIRD -- gefunden beim Nachrechnen der Ruckler, die
     * am 06.08.2026 gemeldet wurden: Ein Kachelplatz ohne Daten wurde nirgends
     * vermerkt. Bei jedem Bildaufbau wurde er deshalb neu beauftragt, der
     * Hintergrundfaden suchte ihn in jeder Kartendatei erneut, fand nichts,
     * und beim naechsten Bild ging dasselbe von vorn los.
     *
     * Auf Zoom 4 faellt das ins Gewicht: Die Europakarte hat auf dieser Stufe
     * 22 Kacheln, der Bildschirm fragt aber ueber hundert Plaetze ab. Vier von
     * fuenf Auftraegen waren also Arbeit fuer nichts -- und sie liefen auf
     * demselben Faden wie die Kacheln, die man wirklich sehen will.
     */
    private val leerErkannt = object : LinkedHashMap<Long, Boolean>(16, 0.75f, true) {
        override fun removeEldestEntry(aeltester: MutableMap.MutableEntry<Long, Boolean>?): Boolean =
            size > 1024
    }

    /**
     * Schon entpackte Kacheln, damit eine grobe Kachel nicht fuer jedes ihrer
     * Felder neu gelesen werden muss.
     *
     * Beim Uebereinanderlegen liefert eine Uebersichtskachel bis zu 64 Feldern
     * der feinen Stufe den Untergrund. Ohne diesen Speicher wurde sie 64 Mal
     * von der Platte geholt, entpackt und zerlegt.
     */
    private val kachelspeicher = object : LinkedHashMap<Long, Kachel>(16, 0.75f, true) {
        override fun removeEldestEntry(aeltester: MutableMap.MutableEntry<Long, Kachel>?): Boolean =
            size > 24
    }

    /** Einmal gebaut statt bei jedem Punkt neu -- `values()` legt jedes Mal ein Feld an. */
    private val ebeneJeArt: Map<String, Ebene> = HashMap<String, Ebene>().apply {
        for (ebene in Ebene.values()) for (art in ebene.arten) put(art, ebene)
    }
    private var arbeiter: HandlerThread? = null
    private var auftrag: Handler? = null
    private val hierher = Handler(Looper.getMainLooper())

    private val schrift = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 12f * dichte
    }
    private val schriftrand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 12f * dichte
        style = Paint.Style.STROKE
        strokeWidth = 3f * dichte
    }
    private val zeichenpinsel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sinnbild = android.graphics.Path()
    private val bildpinsel = Paint(Paint.FILTER_BITMAP_FLAG)
    private val belegt = ArrayList<Rect>()

    /**
     * Schon besetzte Felder fuer die Ausduennung der Ortschaften.
     *
     * WARUM ES DAS BRAUCHT, mit Messung: Bei Zoom 4 sind ALLE Punkte in den
     * Kacheln Grossstaedte -- nachgewiesen am 05.08.2026, indem die Schwelle
     * fuer "grossstadt" kurz auf unerreichbar gesetzt wurde: Der Schirm war
     * schlagartig leer. Es gibt dort also keine Doerfer, die man weglassen
     * koennte. Die rund zweitausend OSM-Eintraege mit place=city ergeben im
     * Ruhrgebiet und in der Po-Ebene trotzdem einen schwarzen Klumpen, in dem
     * kein Name mehr lesbar ist.
     *
     * Nach Rang laesst sich das nicht loesen: Das Kartenformat kennt keine
     * Einwohnerzahl, und "city" ist in manchen Laendern grosszuegig vergeben.
     * Also raeumlich -- hoechstens ein Ort je Feld. Das ist die uebliche
     * Antwort der Kartografie und braucht keine zusaetzlichen Daten: Beim
     * Herauszoomen decken die Felder mehr Landflaeche ab, es bleiben also
     * weniger Orte stehen; beim Hineinzoomen kommen sie von selbst zurueck.
     *
     * Nur fuer Ortschaften. Quellen, Huetten und Krankenhaeuser werden NICHT
     * ausgeduennt -- die sind selten, und sie sind das, wonach jemand sucht.
     *
     * Die Felder liegen in der WELT, nicht auf dem Schirm -- warum, steht bei
     * ihrer Berechnung in `maleZeichen`.
     */
    private val ortsfelder = HashSet<Long>()
    private val einPlatz = Rect()

    private var letztesX = 0f
    private var letztesY = 0f
    private var schiebt = false

    private val zoomgriff = ScaleGestureDetector(
        gastgeber,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(griff: ScaleGestureDetector): Boolean {
                zoomeUmBrennpunkt(
                    ln(griff.scaleFactor.toDouble()).toFloat() / LN2,
                    griff.focusX,
                    griff.focusY,
                )
                return true
            }
        },
    )

    init {
        // Mitte der Karte aus ihren eigenen Grenzen -- eine Karte, die beim
        // Aufschlagen ins Leere zeigt, sieht aus wie eine leere Karte.
        mitteLon = (westen + osten) / 2.0 / 1e7
        mitteLat = (sueden + norden) / 2.0 / 1e7
        setzeZoom(zoomKleinste.toFloat())
        isClickable = true
    }

    fun setzeBlickrichtung(grad: Float) {
        if (!drehtMitBlickrichtung) return
        // Nur bei spuerbarer Aenderung neu zeichnen: Der Sensor liefert
        // dauernd Werte, und jedes Zeichnen kostet Strom.
        if (abs(grad - blickrichtung) < 1.5f) return
        blickrichtung = grad
        invalidate()
    }

    fun zoomeUm(stufen: Float) = setzeZoom(zoomStufe + stufen)

    /**
     * Springt auf eine Stelle. Gibt false zurueck, wenn sie ausserhalb der
     * geladenen Karten liegt.
     *
     * DER SPRUNG FINDET TROTZDEM STATT. Wer eine Koordinate eintippt, will
     * dorthin -- auch wenn dort keine Karte liegt. Ein Fadenkreuz auf leerem
     * Grund ist eine Auskunft ("hier habe ich nichts"), ein verweigerter
     * Sprung ist keine.
     */
    fun springeZu(breite: Double, laenge: Double, stufe: Float? = null): Boolean {
        mitteLat = breite
        mitteLon = laenge
        if (stufe != null) setzeZoom(stufe) else invalidate()
        return laenge * 1e7 >= westen && laenge * 1e7 <= osten &&
            breite * 1e7 >= sueden && breite * 1e7 <= norden
    }

    /**
     * Zeigt die ganze Karte -- so gross, wie sie in die Ansicht passt.
     *
     * Nicht einfach die kleinste Zoomstufe: Eine Karte von Oesterreich auf
     * Stufe 4 ist ein Fleck in der Mitte eines leeren Blattes. Wer eine Karte
     * aufschlaegt, will sie sehen, nicht suchen.
     */
    fun zeigeAufGanzeKarte() {
        mitteLon = (westen + osten) / 2.0 / 1e7
        mitteLat = (sueden + norden) / 2.0 / 1e7
        if (width <= 0 || height <= 0) {
            setzeZoom(zoomKleinste.toFloat())
            return
        }
        val (x1, y1) = weltpunkt(westen / 1e7, norden / 1e7, 0)
        val (x2, y2) = weltpunkt(osten / 1e7, sueden / 1e7, 0)
        val breiteBei0 = abs(x2 - x1) * dichte
        val hoeheBei0 = abs(y2 - y1) * dichte
        // Ein Rand von einem Zehntel, damit die Karte nicht am Rahmen klebt.
        val passtBreit = ln(width * 0.9 / max(breiteBei0, 1e-9)) / ln(2.0)
        val passtHoch = ln(height * 0.9 / max(hoeheBei0, 1e-9)) / ln(2.0)
        setzeZoom(min(passtBreit, passtHoch).toFloat())
    }

    private var schonEingepasst = false

    override fun onSizeChanged(breite: Int, hoehe: Int, alteBreite: Int, alteHoehe: Int) {
        super.onSizeChanged(breite, hoehe, alteBreite, alteHoehe)
        if (!schonEingepasst && breite > 0 && hoehe > 0) {
            schonEingepasst = true
            zeigeAufGanzeKarte()
        }
    }

    private fun setzeZoom(neu: Float) {
        zoomStufe = min(
            max(neu, (zoomKleinste - WEITER_RAUS).toFloat()),
            zoomGroesste + 0.99f,
        )
        meldeZustand()
        invalidate()
    }

    /**
     * Ob im Bild gerade eine vergroesserte Uebersicht steht statt echter
     * Einzelheiten.
     *
     * DAS GEHOERT HINGESCHRIEBEN. Seit die Karte fehlende Stufen aus einer
     * groeberen Karte hochrechnet, sieht ein Gebiet ohne Detailpaket nicht
     * mehr leer aus, sondern wie eine Karte -- nur eben mit stark
     * vereinfachten Linien. Das ist besser als Weiss, aber es darf nicht
     * aussehen wie vermessene Einzelheiten. Wer im Ernstfall einem Weg folgt,
     * muss wissen, ob dessen Verlauf auf 20 oder auf 200 Meter genau ist.
     */
    private var vergroessert = false

    /**
     * Die Himmelsrichtung kommt aus dem Vorzeichen, nicht aus einer Annahme.
     *
     * Vorher standen "N" und "O" fest im Text. Suedlich des Aequators las sich
     * das als "-33.8688°N", westlich von Greenwich als "-8.6110°O" -- und das
     * betrifft nicht nur ferne Gegenden: Irland, Portugal, Westfrankreich,
     * Island und ganz Grossbritannien liegen im mitgelieferten Europablatt
     * westlich des Nullmeridians. Wer eine Lage abliest und weitergibt, gibt
     * damit das Vorzeichen und den Buchstaben weiter, die einander
     * widersprechen. Gefunden am 17.08.2026 beim Durchsehen der Karte.
     */
    private fun himmelsrichtung(wert: Double, negativ: String, positiv: String): String =
        if (wert < 0) negativ else positiv

    private fun meldeZustand() {
        val zusatz = if (vergroessert) " · Übersicht vergrößert" else ""
        beiZustand?.invoke(
            "Zoom %.1f · %.4f°%s %.4f°%s%s".format(
                zoomStufe,
                abs(mitteLat),
                himmelsrichtung(mitteLat, "S", "N"),
                abs(mitteLon),
                himmelsrichtung(mitteLon, "W", "O"),
                zusatz,
            ),
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val faden = HandlerThread("kartenkacheln", android.os.Process.THREAD_PRIORITY_BACKGROUND)
        faden.start()
        arbeiter = faden
        auftrag = Handler(faden.looper)
    }

    override fun onDetachedFromWindow() {
        auftrag = null
        arbeiter?.quit()
        arbeiter = null
        for (eintrag in speicher.values) eintrag.bild.recycle()
        speicher.clear()
        unterwegs.clear()
        super.onDetachedFromWindow()
    }

    /**
     * Der Mittelpunkt aller aufliegenden Finger.
     *
     * WARUM NICHT `ereignis.x` -- das war der Fehler, der am 06.08.2026 als
     * "wenn ich auf dem handy zoome springt die karte hin und her" gemeldet
     * wurde: `ereignis.x` ist immer der ERSTE Finger. Beim Zoomen liegen zwei
     * auf, und sobald einer davon abhebt, ruecken die Nummern nach -- der
     * verbliebene Finger ist ploetzlich "der erste" und steht mehrere
     * Zentimeter woanders. Der naechste Wisch rechnet dann eine Strecke, die
     * niemand zurueckgelegt hat, und die Karte springt.
     *
     * Der Mittelpunkt hat diese Naht nicht: Er ist fuer einen wie fuer zwei
     * Finger dasselbe, und er bewegt sich genau so, wie sich die Hand bewegt.
     * Nebenbei kann man damit auch mit zwei Fingern schieben.
     *
     * Beim Abheben eines Fingers zaehlt dieser NICHT mehr mit -- er ist im
     * Ereignis noch enthalten, gehoert aber schon nicht mehr dazu.
     */
    private fun brennpunkt(ereignis: MotionEvent): Pair<Float, Float> {
        val abheber = if (ereignis.actionMasked == MotionEvent.ACTION_POINTER_UP) {
            ereignis.actionIndex
        } else {
            -1
        }
        var summeX = 0f
        var summeY = 0f
        var zahl = 0
        for (i in 0 until ereignis.pointerCount) {
            if (i == abheber) continue
            summeX += ereignis.getX(i)
            summeY += ereignis.getY(i)
            zahl++
        }
        if (zahl == 0) return Pair(ereignis.x, ereignis.y)
        return Pair(summeX / zahl, summeY / zahl)
    }

    private fun merkeBrennpunkt(ereignis: MotionEvent) {
        val (bx, by) = brennpunkt(ereignis)
        letztesX = bx
        letztesY = by
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ereignis: MotionEvent): Boolean {
        zoomgriff.onTouchEvent(ereignis)
        when (ereignis.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                merkeBrennpunkt(ereignis)
                schiebt = true
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            // Ein Finger kommt dazu oder geht weg. Der Mittelpunkt springt
            // dabei, OHNE dass die Hand die Karte geschoben haette -- also
            // wird er nur neu gemerkt und nichts verschoben. Genau dieser
            // Sprung landete vorher als Wischstrecke auf der Karte.
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP ->
                merkeBrennpunkt(ereignis)

            // Waehrend des Zoomens wird NICHT mehr ausgesetzt: Der Mittelpunkt
            // bewegt sich beim Auseinanderziehen kaum, und was er sich bewegt,
            // ist echtes Schieben. Vorher blieb der Bezugspunkt waehrend der
            // ganzen Zoomgeste stehen und war danach veraltet.
            MotionEvent.ACTION_MOVE -> if (schiebt) {
                val (bx, by) = brennpunkt(ereignis)
                verschiebe(bx - letztesX, by - letztesY)
                letztesX = bx
                letztesY = by
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                schiebt = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    /**
     * Zeichnet das Sinnbild einer Ebene und gibt seinen halben Platzbedarf
     * zurueck -- daran haengt, wo der Name darunter anfaengt.
     *
     * WARUM SINNBILDER UND NICHT PUNKTE: Rueckmeldung vom 06.08.2026: "ich
     * finde noch unuebersichtlich welche punkte was bedeuten". Das trifft zu
     * -- eine Quelle, eine Huette und ein Krankenhaus waren derselbe Kreis.
     * Die Farbe trug die einzige Unterscheidung, und die kennt nur zwei Werte:
     * Wasser und alles andere.
     *
     * FUENF FORMEN, NICHT NEUNZEHN. Das Kartenformat kennt neunzehn
     * Punktarten, aber neunzehn Zeichen kann sich niemand merken, und auf
     * neun Bildpunkten sind sie nicht zu unterscheiden. Gezeichnet wird
     * deshalb nach EBENE -- also nach genau der Einteilung, die schon auf den
     * Schaltern steht. Wer "Wasser" ausschaltet und sieht, welche Zeichen
     * verschwinden, hat die Legende damit selbst in der Hand.
     *
     * Die Formen sind so gewaehlt, dass sie ohne Legende lesbar sind: Tropfen,
     * Kreuz, Haus, Bergspitze. Die Farbe bleibt, was sie war -- der Signalton
     * gehoert weiter allein dem Wasser, und die Form traegt die Auskunft auch
     * dann, wenn im Sparmodus alles grau ist.
     */
    private fun maleSinnbild(leinwand: Canvas, ebene: Ebene?, px: Float, py: Float): Float {
        val r = 4.2f * dichte
        zeichenpinsel.style = Paint.Style.FILL
        when (ebene) {
            Ebene.WASSER -> {
                // Tropfen: unten rund, oben spitz.
                sinnbild.reset()
                sinnbild.moveTo(px, py - r)
                sinnbild.quadTo(px + r, py - r * 0.1f, px + r * 0.62f, py + r * 0.5f)
                sinnbild.quadTo(px, py + r * 1.15f, px - r * 0.62f, py + r * 0.5f)
                sinnbild.quadTo(px - r, py - r * 0.1f, px, py - r)
                sinnbild.close()
                leinwand.drawPath(sinnbild, zeichenpinsel)
            }

            Ebene.HILFE -> {
                // Kreuz -- das eine Zeichen, das ueberall dasselbe bedeutet.
                val d = r * 0.34f
                leinwand.drawRect(px - d, py - r, px + d, py + r, zeichenpinsel)
                leinwand.drawRect(px - r, py - d, px + r, py + d, zeichenpinsel)
            }

            Ebene.SCHUTZ -> {
                // Haus: Dach ueber einem Koerper. Der Koerper ist der
                // Unterschied zur Bergspitze -- ein blosses Dreieck waere auf
                // dieser Groesse dasselbe Zeichen.
                sinnbild.reset()
                sinnbild.moveTo(px, py - r)
                sinnbild.lineTo(px + r, py - r * 0.1f)
                sinnbild.lineTo(px + r * 0.66f, py - r * 0.1f)
                sinnbild.lineTo(px + r * 0.66f, py + r * 0.85f)
                sinnbild.lineTo(px - r * 0.66f, py + r * 0.85f)
                sinnbild.lineTo(px - r * 0.66f, py - r * 0.1f)
                sinnbild.lineTo(px - r, py - r * 0.1f)
                sinnbild.close()
                leinwand.drawPath(sinnbild, zeichenpinsel)
            }

            Ebene.GELAENDE -> {
                // Bergspitze: die Kartenkonvention fuer einen Gipfel.
                sinnbild.reset()
                sinnbild.moveTo(px, py - r)
                sinnbild.lineTo(px + r, py + r * 0.8f)
                sinnbild.lineTo(px - r, py + r * 0.8f)
                sinnbild.close()
                leinwand.drawPath(sinnbild, zeichenpinsel)
            }

            // Ortschaften und alles, was zu keiner Ebene gehoert, bleiben der
            // schlichte Punkt: Sie tragen ihren Namen daneben, und ein
            // erfundenes Zeichen wuerde etwas behaupten, das nicht dasteht.
            // Kleiner als die Sinnbilder, und das mit Absicht: Ein Ortspunkt
            // markiert nur die Stelle, die Auskunft steht als Name daneben.
            // Auf schwarzem Grund war er vorher der lauteste Fleck im Bild.
            else -> {
                leinwand.drawCircle(px, py, 2.0f * dichte, zeichenpinsel)
                return 2.0f * dichte
            }
        }
        return r
    }

    /**
     * Welche Stelle der Erde liegt unter diesem Punkt auf dem Schirm?
     *
     * Der Weg ist derselbe wie beim Zeichnen, nur rueckwaerts: erst die
     * Drehung herausrechnen, dann den Abstand zur Bildmitte in Kachelpunkte
     * umrechnen und auf die Mitte draufschlagen.
     */
    private fun geoUnter(px: Float, py: Float): Pair<Double, Double> {
        val stufe = kachelzoom()
        val massstab = bildpunkteJeKachelpunkt(stufe)
        val bogen = Math.toRadians(blickrichtung.toDouble())
        val ox = px - width / 2f
        val oy = py - height / 2f
        val ux = ox * cos(bogen) - oy * sin(bogen)
        val uy = ox * sin(bogen) + oy * cos(bogen)
        val (cx, cy) = weltpunkt(mitteLon, mitteLat, stufe)
        return Pair(
            laengeVon(cx + ux / massstab, stufe),
            breiteVon(cy + uy / massstab, stufe),
        )
    }

    /** Schiebt die Mitte so, dass diese Stelle wieder unter diesem Punkt liegt. */
    private fun haltePunkt(lon: Double, lat: Double, px: Float, py: Float) {
        val stufe = kachelzoom()
        val massstab = bildpunkteJeKachelpunkt(stufe)
        val bogen = Math.toRadians(blickrichtung.toDouble())
        val ox = px - width / 2f
        val oy = py - height / 2f
        val ux = ox * cos(bogen) - oy * sin(bogen)
        val uy = ox * sin(bogen) + oy * cos(bogen)
        val (wx, wy) = weltpunkt(lon, lat, stufe)
        mitteLon = laengeVon(wx - ux / massstab, stufe)
        mitteLat = breiteVon(wy - uy / massstab, stufe)
    }

    /**
     * Zoomt um den Punkt zwischen den Fingern statt um die Bildmitte.
     *
     * Der zweite Teil desselben Fehlers: Bisher wuchs die Karte immer aus der
     * Bildmitte heraus. Wer am linken Rand zwei Finger auseinanderzieht,
     * erwartet aber, dass genau die Stelle zwischen seinen Fingern stehen
     * bleibt -- sonst wandert die Landschaft unter der Hand weg, und das
     * fuehlt sich an, als spraenge die Karte.
     */
    private fun zoomeUmBrennpunkt(stufen: Float, px: Float, py: Float) {
        if (width == 0 || height == 0) {
            setzeZoom(zoomStufe + stufen)
            return
        }
        val (lon, lat) = geoUnter(px, py)
        setzeZoom(zoomStufe + stufen)
        haltePunkt(lon, lat, px, py)
        meldeZustand()
        invalidate()
    }

    private fun verschiebe(dx: Float, dy: Float) {
        // Der Wisch liegt im gedrehten Bild; er muss zurueckgedreht werden,
        // sonst laeuft die Karte schraeg zum Finger.
        val bogen = Math.toRadians(blickrichtung.toDouble())
        val wx = dx * cos(bogen) - dy * sin(bogen)
        val wy = dx * sin(bogen) + dy * cos(bogen)

        val stufe = kachelzoom()
        val massstab = bildpunkteJeKachelpunkt(stufe)
        val (cx, cy) = weltpunkt(mitteLon, mitteLat, stufe)
        val neuX = cx - wx / massstab
        val neuY = cy - wy / massstab
        mitteLon = laengeVon(neuX, stufe)
        mitteLat = breiteVon(neuY, stufe)
        meldeZustand()
        invalidate()
    }

    private fun kachelzoom(): Int =
        min(max(floor(zoomStufe).toInt(), zoomKleinste), zoomGroesste)

    private fun bildpunkteJeKachelpunkt(stufe: Int): Float {
        val zwischen = Math.pow(2.0, (zoomStufe - stufe).toDouble()).toFloat()
        return zwischen * dichte
    }

    /**
     * Was der Nutzer selbst gesetzt hat. Wird von aussen gefuellt und getauscht.
     */
    var eigenes: Eigenkarte = Eigenkarte()
        set(wert) {
            field = wert
            invalidate()
        }

    /**
     * Die eigenen Sachen sehen ABSICHTLICH anders aus als alles aus der
     * Kartendatei -- nicht bloss andersfarbig, sondern anders GEFORMT:
     *
     * Punkte sind ein offener Ring mit Kreuz, kein gefuelltes Sinnbild. Ein
     * Sinnbild behauptet "hier ist eine Quelle"; der Ring behauptet nur "hier
     * habe ich etwas markiert". Wer im Ernstfall Wasser sucht, muss auf einen
     * Blick unterscheiden koennen, ob die Auskunft aus dem geprueften
     * Kartenwerk stammt oder von ihm selbst.
     *
     * Wege sind GESTRICHELT. Eine durchgezogene Linie sieht aus wie ein Weg,
     * der da ist. Ein selbst gezeichneter Weg ist eine Absicht, kein Pfad im
     * Gelaende -- und wer ihm nachts folgt, muss das wissen.
     */
    private val eigenStrich = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFB3382C.toInt()
        strokeWidth = 2.4f * dichte
    }
    private val eigenRand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 5.0f * dichte
    }

    /** Gefuellte Fassung derselben Signalfarbe -- fuer die Sinnbilder im Ring. */
    private val eigenFuellung = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFB3382C.toInt()
    }
    private val eigenZeichen = android.graphics.Path()
    private val halbscheibe = android.graphics.RectF()
    private val eigenLinie = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFB3382C.toInt()
        strokeWidth = 3.0f * dichte
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = android.graphics.DashPathEffect(
            floatArrayOf(9f * dichte, 6f * dichte),
            0f,
        )
    }
    private val eigenLinieRand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 6.0f * dichte
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = android.graphics.DashPathEffect(
            floatArrayOf(9f * dichte, 6f * dichte),
            0f,
        )
    }
    private val eigenSchrift = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7A2018.toInt()
        textSize = 12f * dichte
        textAlign = Paint.Align.CENTER
    }
    private val eigenSchriftRand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 3f * dichte
        textSize = 12f * dichte
        textAlign = Paint.Align.CENTER
    }
    private val eigenPfad = android.graphics.Path()

    /** Im Merkmodus steht ein Fadenkreuz fest in der Bildmitte. */
    var zeigtFadenkreuz: Boolean = false
        set(wert) {
            field = wert
            invalidate()
        }

    /** Die Stellen eines Wegs, der gerade entsteht -- noch nicht gespeichert. */
    var wegImBau: List<Pair<Double, Double>>? = null
        set(wert) {
            field = wert
            invalidate()
        }

    /**
     * Eine berechnete Route, je Punkt {Breite, Laenge}.
     *
     * ACHTUNG AUF DIE REIHENFOLGE: Die eigenen Wege liegen als (Laenge,
     * Breite) vor, eine Route als (Breite, Laenge) -- so gibt sie die
     * Wegesuche zurueck, und so steht sie in jeder Koordinatenangabe. Wer die
     * beiden verwechselt, bekommt eine Linie im Golf von Aden.
     */
    var route: List<DoubleArray>? = null
        set(wert) {
            field = wert
            invalidate()
        }

    // Die Route ist breiter als ein eigener Weg und liegt unter ihm: Sie ist
    // ein Vorschlag, kein Eintrag. Der dunkle Rand darunter haelt sie auch
    // ueber hellem Satellitenbild lesbar.
    private val routeRand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xB0101010.toInt()
        strokeWidth = 8f * dichte
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val routeLinie = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // GESTRICHELT, UND ZWAR AUS ZWEI GRUENDEN.
        //
        // Erstens: Eine Route ist kein Ding im Gelaende, sondern ein
        // Vorschlag. Dieselbe Sprache benutzt die Karte schon bei den
        // Grenzen -- gestrichelt heisst "keine Sache, ueber die man
        // stolpert".
        //
        // Zweitens: Eine durchgezogene blaue Linie sah auf dem Geraet aus wie
        // ein Fluss. Blau ist auf dieser Karte das Wasser, und wer einer
        // Route folgt, die er fuer einen Bach haelt, sucht am Ende beides
        // nicht.
        color = 0xFF3AA0FF.toInt()
        strokeWidth = 5f * dichte
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.ROUND
        pathEffect = android.graphics.DashPathEffect(
            floatArrayOf(11f * dichte, 6f * dichte), 0f,
        )
    }
    private val routePfad = android.graphics.Path()

    private val kreuzStrich = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF1A1A1A.toInt()
        strokeWidth = 1.6f * dichte
    }
    private val kreuzRand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 4.0f * dichte
    }

    /**
     * Das Fadenkreuz zeigt, wo ein Punkt landen wuerde -- und zwar mit einer
     * Luecke in der Mitte. Ein durchgezogenes Kreuz verdeckt genau die Stelle,
     * die man beurteilen will.
     */
    private fun maleFadenkreuz(leinwand: Canvas) {
        if (!zeigtFadenkreuz) return
        val mx = width / 2f
        val my = height / 2f
        val innen = 4f * dichte
        val aussen = 14f * dichte
        for (farbe in arrayOf(kreuzRand, kreuzStrich)) {
            leinwand.drawLine(mx - aussen, my, mx - innen, my, farbe)
            leinwand.drawLine(mx + innen, my, mx + aussen, my, farbe)
            leinwand.drawLine(mx, my - aussen, mx, my - innen, farbe)
            leinwand.drawLine(mx, my + innen, mx, my + aussen, farbe)
        }
        leinwand.drawCircle(mx, my, 1.6f * dichte, kreuzStrich)
    }

    /**
     * Rechnet eine Stelle der Erde auf den Schirm um -- denselben Weg, den auch
     * die Sinnbilder gehen: erst der Abstand zur Bildmitte in Bildpunkten,
     * dann dieselbe Drehung.
     */
    private fun aufSchirm(
        laenge: Double,
        breite: Double,
        stufe: Int,
        massstab: Float,
        cx: Double,
        cy: Double,
        drehung: Double,
    ): Pair<Float, Float> {
        val (wx, wy) = weltpunkt(laenge, breite, stufe)
        val ox = ((wx - cx) * massstab).toFloat()
        val oy = ((wy - cy) * massstab).toFloat()
        val px = width / 2f + (ox * cos(drehung) - oy * sin(drehung)).toFloat()
        val py = height / 2f + (ox * sin(drehung) + oy * cos(drehung)).toFloat()
        return px to py
    }

    /**
     * Das Sinnbild EINES SELBST GESETZTEN Punktes, immer innerhalb des Rings.
     *
     * Der Ring bleibt das Erkennungsmerkmal: Punkte aus der Kartendatei sind
     * geprueft und signiert, diese hier sind eine Behauptung des Nutzers, und
     * beide duerfen nie gleich aussehen. Ein Kartenpunkt bekommt nie einen Ring
     * -- deshalb darf das Zeichen darin ruhig einem Kartenzeichen aehneln.
     *
     * Die Formen sind grob und gross gehalten: Auf sieben Bildpunkten
     * entscheidet der Umriss, nicht die Einzelheit.
     */
    private fun maleWegpunktzeichen(
        leinwand: Canvas,
        sinnbild: String,
        px: Float,
        py: Float,
        r: Float,
    ) {
        when (sinnbild) {
            Sinnbild.WASSER -> {
                // Tropfen: oben spitz, unten rund.
                eigenZeichen.reset()
                eigenZeichen.moveTo(px, py - r)
                eigenZeichen.quadTo(px + r * 0.95f, py + r * 0.1f, px, py + r)
                eigenZeichen.quadTo(px - r * 0.95f, py + r * 0.1f, px, py - r)
                eigenZeichen.close()
                leinwand.drawPath(eigenZeichen, eigenFuellung)
            }

            Sinnbild.UNTERKUNFT -> {
                // Haus: Dach ueber einem Koerper.
                eigenZeichen.reset()
                eigenZeichen.moveTo(px, py - r)
                eigenZeichen.lineTo(px + r, py - r * 0.1f)
                eigenZeichen.lineTo(px - r, py - r * 0.1f)
                eigenZeichen.close()
                leinwand.drawPath(eigenZeichen, eigenFuellung)
                leinwand.drawRect(px - r * 0.6f, py - r * 0.1f, px + r * 0.6f, py + r, eigenFuellung)
            }

            Sinnbild.NAHRUNG -> {
                // Schale: die UNTERE HALBSCHEIBE, nichts weiter.
                //
                // Erster Anlauf war eine gezeichnete Schale mit Bogen; auf dem
                // Geraet nachgesehen wurde daraus ein waagerechter Balken, und
                // der war von "Vorrat" nicht zu unterscheiden. Bei neun
                // Bildpunkten traegt nur der Umriss, nicht die Zeichnung.
                halbscheibe.set(px - r, py - r * 0.75f, px + r, py + r * 1.25f)
                leinwand.drawArc(halbscheibe, 0f, 180f, true, eigenFuellung)
            }

            Sinnbild.GEFAHR -> {
                // Dreieck mit der Spitze nach oben -- das Warnzeichen.
                //
                // Auch hier ein zweiter Anlauf: Das Ausrufezeichen aus Balken
                // und Punkt verschmolz auf dem Geraet zu einem Klecks. Ein
                // Dreieck hat einen Umriss, der auch klein noch traegt. Vom
                // Haus unterscheidet es sich durch den fehlenden Koerper.
                eigenZeichen.reset()
                eigenZeichen.moveTo(px, py - r)
                eigenZeichen.lineTo(px + r, py + r * 0.75f)
                eigenZeichen.lineTo(px - r, py + r * 0.75f)
                eigenZeichen.close()
                leinwand.drawPath(eigenZeichen, eigenFuellung)
            }

            Sinnbild.TREFFPUNKT -> {
                // Zielscheibe: Ring mit Kern.
                zeichenpinsel.style = Paint.Style.STROKE
                zeichenpinsel.strokeWidth = r * 0.34f
                zeichenpinsel.color = 0xFFB3382C.toInt()
                leinwand.drawCircle(px, py, r * 0.72f, zeichenpinsel)
                leinwand.drawCircle(px, py, r * 0.22f, eigenFuellung)
            }

            Sinnbild.VORRAT -> {
                // Kiste: ein VOLLER Block, ohne Band.
                //
                // Das Band quer darueber machte aus der Kiste zwei Striche und
                // damit einen Zwilling der Schale. Der einzige gefuellte
                // Vierecks-Umriss im ganzen Satz ist unverwechselbar, und mehr
                // braucht es nicht.
                leinwand.drawRect(px - r * 0.85f, py - r * 0.85f, px + r * 0.85f, py + r * 0.85f, eigenFuellung)
            }

            Sinnbild.UEBERGANG -> {
                // Pfeil nach rechts: hinueber.
                eigenZeichen.reset()
                eigenZeichen.moveTo(px + r, py)
                eigenZeichen.lineTo(px - r * 0.15f, py - r * 0.85f)
                eigenZeichen.lineTo(px - r * 0.15f, py - r * 0.3f)
                eigenZeichen.lineTo(px - r, py - r * 0.3f)
                eigenZeichen.lineTo(px - r, py + r * 0.3f)
                eigenZeichen.lineTo(px - r * 0.15f, py + r * 0.3f)
                eigenZeichen.lineTo(px - r * 0.15f, py + r * 0.85f)
                eigenZeichen.close()
                leinwand.drawPath(eigenZeichen, eigenFuellung)
            }

            // RUECKFALL, und der ist Absicht: Dieses when laeuft ueber
            // Zeichenketten, nicht ueber eine Aufzaehlung -- der Kotlin-Compiler
            // kann also nicht erzwingen, dass jedes Sinnbild gezeichnet wird.
            // Ohne diesen Zweig wuerde ein neu hinzugefuegtes Sinnbild in der
            // Auswahl erscheinen, sich speichern lassen und auf der Karte
            // NICHTS zeichnen: ein leerer Ring, den niemand als Fehler erkennt.
            // Lieber das Fadenkreuz als gar nichts. Dass die Liste vollstaendig
            // ist, haelt SinnbildTest fest.
            else -> {
                leinwand.drawLine(px - r, py, px + r, py, eigenStrich)
                leinwand.drawLine(px, py - r, px, py + r, eigenStrich)
            }
        }
    }

    private fun maleEigenes(
        leinwand: Canvas,
        stufe: Int,
        massstab: Float,
        cx: Double,
        cy: Double,
        drehung: Double,
    ) {
        // Die Route ZUERST, damit eigene Wege und Punkte darueber liegen.
        // Was man selbst eingetragen hat, darf ein Vorschlag nicht zudecken.
        route?.let { punkte ->
            if (punkte.size >= 2) {
                routePfad.reset()
                for ((i, punkt) in punkte.withIndex()) {
                    val (px, py) = aufSchirm(punkt[1], punkt[0], stufe, massstab, cx, cy, drehung)
                    if (i == 0) routePfad.moveTo(px, py) else routePfad.lineTo(px, py)
                }
                leinwand.drawPath(routePfad, routeRand)
                leinwand.drawPath(routePfad, routeLinie)
            }
        }

        // Der Weg im Bau wird IMMER gezeigt, auch bei ausgeschalteter Ebene:
        // Wer gerade zeichnet, muss sehen, was er zeichnet.
        wegImBau?.let { stellen ->
            if (stellen.size >= 2) {
                eigenPfad.reset()
                for ((i, stelle) in stellen.withIndex()) {
                    val (px, py) = aufSchirm(stelle.first, stelle.second, stufe, massstab, cx, cy, drehung)
                    if (i == 0) eigenPfad.moveTo(px, py) else eigenPfad.lineTo(px, py)
                }
                leinwand.drawPath(eigenPfad, eigenLinieRand)
                leinwand.drawPath(eigenPfad, eigenLinie)
            }
            val ecke = 3.5f * dichte
            for (stelle in stellen) {
                val (px, py) = aufSchirm(stelle.first, stelle.second, stufe, massstab, cx, cy, drehung)
                leinwand.drawCircle(px, py, ecke, eigenRand)
                leinwand.drawCircle(px, py, ecke, eigenStrich)
            }
        }

        if (Ebene.EIGEN !in sichtbar || eigenes.leer) return

        for (weg in eigenes.wege) {
            if (weg.stellen.size < 2) continue
            eigenPfad.reset()
            for ((i, stelle) in weg.stellen.withIndex()) {
                val (px, py) = aufSchirm(stelle.first, stelle.second, stufe, massstab, cx, cy, drehung)
                if (i == 0) eigenPfad.moveTo(px, py) else eigenPfad.lineTo(px, py)
            }
            // Der helle Rand zuerst: Ohne ihn verschwindet die Linie ueber
            // hellem Fels und ueber Wasser.
            leinwand.drawPath(eigenPfad, eigenLinieRand)
            leinwand.drawPath(eigenPfad, eigenLinie)
        }

        val ring = 5.5f * dichte
        val arm = 3.2f * dichte
        for (punkt in eigenes.punkte) {
            val (px, py) = aufSchirm(punkt.laenge, punkt.breite, stufe, massstab, cx, cy, drehung)
            if (px < -60 || py < -60 || px > width + 60 || py > height + 60) continue
            leinwand.drawCircle(px, py, ring, eigenRand)
            leinwand.drawCircle(px, py, ring, eigenStrich)
            if (punkt.sinnbild == Sinnbild.KEINS) {
                leinwand.drawLine(px - arm, py, px + arm, py, eigenStrich)
                leinwand.drawLine(px, py - arm, px, py + arm, eigenStrich)
            } else {
                maleWegpunktzeichen(leinwand, punkt.sinnbild, px, py, arm)
            }
            if (punkt.name.isNotEmpty()) {
                val y = py - ring - 4f * dichte
                leinwand.drawText(punkt.name, px, y, eigenSchriftRand)
                leinwand.drawText(punkt.name, px, y, eigenSchrift)
            }
        }
    }

    override fun onDraw(leinwand: Canvas) {
        leinwand.drawColor(stil.hintergrund)
        if (width == 0 || height == 0) return

        val stufe = kachelzoom()
        val massstab = bildpunkteJeKachelpunkt(stufe)
        val kachelBreite = KACHELKANTE * massstab
        val (cx, cy) = weltpunkt(mitteLon, mitteLat, stufe)

        val gespeichert = leinwand.save()
        if (blickrichtung != 0f) {
            leinwand.rotate(-blickrichtung, width / 2f, height / 2f)
        }

        // Bei gedrehter Karte reicht der sichtbare Bereich ueber die Ecken
        // hinaus. Die halbe Bilddiagonale deckt jede Drehung ab.
        val reichweite = if (blickrichtung != 0f) {
            Math.hypot(width.toDouble(), height.toDouble()).toFloat() / 2f
        } else {
            max(width, height) / 2f
        }
        val halbeBreite = if (blickrichtung != 0f) reichweite else width / 2f
        val halbeHoehe = if (blickrichtung != 0f) reichweite else height / 2f

        val linkeKachel = floor((cx - halbeBreite / massstab) / KACHELKANTE).toInt()
        val rechteKachel = ceil((cx + halbeBreite / massstab) / KACHELKANTE).toInt()
        val obereKachel = floor((cy - halbeHoehe / massstab) / KACHELKANTE).toInt()
        val untereKachel = ceil((cy + halbeHoehe / massstab) / KACHELKANTE).toInt()
        val letzte = (1 shl stufe) - 1

        belegt.clear()
        ortsfelder.clear()
        val sichtbar = ArrayList<Eintrag>()
        val versatzX = ArrayList<Float>()
        val versatzY = ArrayList<Float>()

        for (ty in max(0, obereKachel)..min(letzte, untereKachel)) {
            for (tx in max(0, linkeKachel)..min(letzte, rechteKachel)) {
                val links = ((tx * KACHELKANTE - cx) * massstab + width / 2f).toFloat()
                val oben = ((ty * KACHELKANTE - cy) * massstab + height / 2f).toFloat()
                val eintrag = hole(stufe, tx, ty)
                if (eintrag == null) {
                    zeichneNotbehelf(leinwand, stufe, tx, ty, links, oben, kachelBreite)
                    continue
                }
                leinwand.drawBitmap(
                    eintrag.bild,
                    null,
                    android.graphics.RectF(links, oben, links + kachelBreite, oben + kachelBreite),
                    bildpinsel,
                )
                sichtbar.add(eintrag)
                versatzX.add(links)
                versatzY.add(oben)
            }
        }

        leinwand.restoreToCount(gespeichert)

        // Beschriftungen und Punktzeichen liegen ueber der gedrehten Karte,
        // aber selbst waagrecht: Ein Ortsname auf dem Kopf ist kein Ortsname.
        //
        // ZWEI DURCHGAENGE, UND DIE REIHENFOLGE IST DER PUNKT: Wo zwei Namen
        // uebereinanderliegen, gewinnt der zuerst gezeichnete. Im ersten
        // Durchgang kommen deshalb die Ortsnamen, im zweiten die Punkte. Ohne
        // die Trennung entschied die Reihenfolge in der Datei -- und weil die
        // Punkte dort vor den Orten stehen, hat ein namenloser Huegel
        // regelmaessig ein Dorf verdeckt.
        // Steht irgendwo im Bild eine vergroesserte Uebersicht? Nur bei
        // Aenderung melden -- die Standzeile bei jedem Bildaufbau neu zu
        // setzen waere Arbeit ohne Auskunft.
        val jetztGrob = sichtbar.any { (it.schichten.lastOrNull()?.teilung ?: 1) > 1 }
        if (jetztGrob != vergroessert) {
            vergroessert = jetztGrob
            meldeZustand()
        }

        // Die Beschriftung kommt aus ALLEN Schichten, sonst haette die deutsche
        // Haelfte einer Grenzkachel Wege, aber keine Ortsnamen. Feinste zuerst:
        // Steht derselbe Ort in zwei Karten, gewinnt der genauere Eintrag, und
        // der zweite faellt ueber das Ortsraster von selbst weg.
        val drehung = Math.toRadians(-blickrichtung.toDouble())
        gefunden.clear()
        for (welche in intArrayOf(Kartenformat.ORT, Kartenformat.PUNKT)) {
            for (i in sichtbar.indices) {
                for (schicht in sichtbar[i].schichten.asReversed()) {
                    maleZeichen(
                        leinwand,
                        schicht,
                        versatzX[i] - schicht.teilX * kachelBreite,
                        versatzY[i] - schicht.teilY * kachelBreite,
                        massstab,
                        drehung,
                        welche,
                    )
                }
            }
        }

        maleEigenes(leinwand, stufe, massstab, cx, cy, drehung)
        maleFadenkreuz(leinwand)
        if (!eigenes.leer) gefunden.add(Ebene.EIGEN)

        // Nur bei Aenderung melden: Die Schalter bei jedem Bildaufbau neu zu
        // beschriften waere Arbeit ohne Auskunft.
        if (gefunden != vorhanden) {
            vorhanden.clear()
            vorhanden.addAll(gefunden)
            beiEbenen?.invoke()
        }
    }

    private fun maleZeichen(
        leinwand: Canvas,
        schicht: Schicht,
        links: Float,
        oben: Float,
        massstab: Float,
        drehung: Double,
        welcheSorte: Int,
    ) {
        val kachel = schicht.kachel
        val mx = width / 2f
        val my = height / 2f
        // Ein Rasterpunkt der Quellkachel in Bildpunkten. Stammt das Bild aus
        // einer groeberen Stufe, ist er um teilung groesser.
        val jePunkt = (KACHELKANTE.toFloat() / Kartenformat.RASTER) * massstab * schicht.teilung
        for (i in 0 until kachel.objekte) {
            if (kachel.art[i].toInt() != Kartenformat.Art.PUNKT) continue
            val sorte = kachel.sorte[i].toInt()
            if (sorte != welcheSorte) continue
            val artname = Kartenformat.punktartName(kachel.punktart[i].toInt())
            if (!ortPasstZumZoom(artname)) continue

            val a = kachel.anfang[i]
            val rohX = links + kachel.x[a] * jePunkt
            val rohY = oben + kachel.y[a] * jePunkt
            // Denselben Weg drehen, den die Karte gegangen ist.
            val ox = rohX - mx
            val oy = rohY - my
            val px = mx + (ox * cos(drehung) - oy * sin(drehung)).toFloat()
            val py = my + (ox * sin(drehung) + oy * cos(drehung)).toFloat()
            if (px < -40 || py < -40 || px > width + 40 || py > height + 40) continue

            // Erst zaehlen, dann filtern: Der Schalter soll auch dann
            // erkennbar bleiben, wenn seine Ebene gerade ausgeschaltet ist.
            val ebene = ebeneVon(artname)
            if (ebene != null) {
                gefunden.add(ebene)
                if (ebene !in sichtbar) continue
            }

            if (sorte == Kartenformat.ORT) {
                // DAS FELD LIEGT IN DER WELT, NICHT AUF DEM SCHIRM.
                //
                // Zuerst wurde aus px/py gerechnet. Damit wanderte das ganze
                // Raster beim Schieben MIT der Hand ueber die Landkarte: In
                // jedem Bild gewann ein anderer Ort sein Feld, Namen sprangen
                // auf und verschwanden wieder. Rueckmeldung vom 05.08.2026:
                // "die namen veraendern sich sehr schnell und krass wenn ich
                // die karte bewege." Eine Karte, deren Beschriftung beim
                // Schieben flackert, ist nicht zu lesen.
                //
                // Jetzt haengt das Feld am Ort selbst -- an seiner Lage in der
                // Welt, in Rastereinheiten der angezeigten Stufe. Beim
                // Schieben aendert sich daran nichts, also gewinnt immer
                // derselbe Ort, und die Namen stehen still. Innerhalb einer
                // Zoomstufe bleibt das Raster ebenfalls fest; erst der
                // Uebergang auf die naechste Stufe bringt neue Namen -- und
                // dort erwartet man Veraenderung.
                val weltX = (kachel.kachelX.toLong() * Kartenformat.RASTER + kachel.x[a]) *
                    schicht.teilung
                val weltY = (kachel.kachelY.toLong() * Kartenformat.RASTER + kachel.y[a]) *
                    schicht.teilung
                val feld = ((weltX / FELD_RASTER) shl 32) or
                    ((weltY / FELD_RASTER) and 0xFFFFFFFFL)
                if (!ortsfelder.add(feld)) continue
            }

            val name = kachel.namenVon(i)
            zeichenpinsel.color = if (sorte == Kartenformat.ORT) {
                stil.punktfarbeOrt
            } else {
                stil.punktfarbe(kachel.punktart[i].toInt())
            }
            val punkt = maleSinnbild(leinwand, ebene, px, py)

            if (name.isNullOrEmpty()) continue
            val breite = schrift.measureText(name)
            val hoehe = schrift.textSize
            einPlatz.set(
                (px - breite / 2f - 2).toInt(),
                (py + punkt + 1).toInt(),
                (px + breite / 2f + 2).toInt(),
                (py + punkt + hoehe + 2).toInt(),
            )
            // Ein Name, der am Bildrand mitten im Wort abgeschnitten wird, sieht
            // aus wie ein Fehler -- "ner Pfannock", "cherhaus", "Turrach".
            // Genau das hat die Karte am 05.08.2026 unfertig wirken lassen.
            // Wer nicht ganz hineinpasst, wird gar nicht erst gemalt; der Punkt
            // bleibt trotzdem stehen, die Auskunft geht also nicht verloren.
            if (einPlatz.left < 0 || einPlatz.right > width ||
                einPlatz.top < 0 || einPlatz.bottom > height
            ) {
                continue
            }

            // Namen, die sich ueberdecken, sind schlimmer als fehlende Namen:
            // Uebereinander gedruckt ist keiner mehr lesbar.
            var frei = true
            for (anderer in belegt) {
                if (Rect.intersects(anderer, einPlatz)) {
                    frei = false
                    break
                }
            }
            if (!frei) continue
            belegt.add(Rect(einPlatz))

            val grundlinie = py + punkt + hoehe
            if (stil.beschriftungRand != android.graphics.Color.TRANSPARENT) {
                schriftrand.color = stil.beschriftungRand
                leinwand.drawText(name, px, grundlinie, schriftrand)
            }
            schrift.color = stil.beschriftung
            leinwand.drawText(name, px, grundlinie, schrift)
        }
    }

    /**
     * Baut das Bild fuer eine Kachelstelle, indem es ALLE Karten uebereinander
     * legt, die dazu etwas beitragen koennen.
     *
     * WARUM ES NICHT REICHT, EINE KARTE ZU NEHMEN -- der Fehler, der am
     * 05.08.2026 mit "ab zoomstufe 12 die karte an vielen stellen nur noch
     * weiss" gemeldet wurde, gemessen an der genannten Stelle (48.15 N, 12.78
     * O, Zoom 13, Burghausen an der Salzach):
     *
     *   Kachel 4385/2841 -> z11 aus dem Oesterreich-Detail (Teilung 4)
     *   Kachel 4386/2842 -> z12 aus dem Oesterreich-Detail (Teilung 2)
     *   Kachel 4387/2843 -> z13 aus dem Oesterreich-Detail (Teilung 1)
     *
     * Jede Stelle wurde also bedient -- aber immer aus dem Detailpaket, weil
     * das die erste Datei war, die an dieser Stelle ueberhaupt eine Kachel
     * hatte. Nur enthaelt dessen grobe Kachel bloss die oesterreichische
     * Seite: Das Paket ist an der Landesgrenze abgeschnitten. Die deutsche
     * Haelfte blieb leer, und die Europakarte, die dort Daten hat, wurde nie
     * gefragt.
     *
     * Weiss ist die gefaehrlichste Antwort, die eine Karte geben kann. Sie
     * behauptet "hier ist nichts" -- gemeint ist "hier weiss ich nichts
     * Genaueres". Der Unterschied entscheidet darueber, ob jemand einen Weg
     * sucht, den es gibt.
     *
     * Also: JE KARTE die feinste Stufe, die sie zu diesem Feld hat; dann alle
     * Beitraege von grob nach fein uebereinander. Wo das feine Paket Daten
     * hat, deckt es die Uebersicht mit seinen Flaechen zu; wo es keine hat,
     * bleibt die Uebersicht stehen. Genau so war die Aufteilung in Uebersicht
     * und Detail von Anfang an gedacht.
     *
     * Gibt eine Karte die verlangte Stufe nicht her, wird ihr Ausschnitt
     * vergroessert NEU gezeichnet -- keine Bildvergroesserung, sondern dieselben
     * Linienzuege in gross: groeber vereinfacht, aber scharf und richtig.
     * Hoechstens vier Stufen weit; danach verspricht ein Strich mehr, als er
     * halten kann.
     */
    private val quellfeld = Rect()
    private val zielfeld = android.graphics.RectF()

    /**
     * Fuellt eine noch nicht fertige Kachelstelle mit dem, was schon gezeichnet
     * ist -- notfalls groeber oder feiner als verlangt.
     *
     * WARUM: Rueckmeldung vom 06.08.2026: "die karte wird bei jedem neuen zoom
     * lvl kurz weiss." Das lag nicht an fehlenden Daten, sondern daran, dass
     * jede Zoomstufe ihre EIGENEN Kacheln hat. Beim Wechsel war der Speicher
     * fuer die neue Stufe leer, und bis die ersten fertig gebaut waren, stand
     * nichts da.
     *
     * Der Nachbar ist aber da: Beim Hineinzoomen liegt die groebere Stufe noch
     * im Speicher, beim Herauszoomen die feinere. Beides laesst sich sofort
     * hinlegen -- verzerrt und ungenau, aber es ist die Landschaft, die man
     * gerade angesehen hat, und sie wird ersetzt, sobald die richtige Kachel
     * fertig ist. Ein Blick auf eine leicht unscharfe Karte ist besser als
     * einer auf ein weisses Feld.
     */
    private fun zeichneNotbehelf(
        leinwand: Canvas,
        stufe: Int,
        tx: Int,
        ty: Int,
        links: Float,
        oben: Float,
        kante: Float,
    ) {
        // Groeber: ein Ausschnitt der Vorfahrenkachel, vergroessert.
        for (k in 1..GROB_MAX) {
            val z = stufe - k
            if (z < zoomKleinste - WEITER_RAUS) break
            val eintrag = speicher[schluesselVon(z, tx shr k, ty shr k)] ?: continue
            val teil = 1 shl k
            val ix = tx - ((tx shr k) shl k)
            val iy = ty - ((ty shr k) shl k)
            val s = KACHELKANTE / teil
            if (s < 1) break
            quellfeld.set(ix * s, iy * s, (ix + 1) * s, (iy + 1) * s)
            zielfeld.set(links, oben, links + kante, oben + kante)
            leinwand.drawBitmap(eintrag.bild, quellfeld, zielfeld, bildpinsel)
            return
        }
        // Feiner: die vier Kinder, verkleinert. Fehlt eines, bleibt sein
        // Viertel eben leer -- besser ein Viertel als das ganze Feld.
        val z = stufe + 1
        if (z > zoomGroesste) return
        val halb = kante / 2f
        for (dy in 0..1) {
            for (dx in 0..1) {
                val eintrag = speicher[schluesselVon(z, tx * 2 + dx, ty * 2 + dy)] ?: continue
                zielfeld.set(
                    links + dx * halb,
                    oben + dy * halb,
                    links + (dx + 1) * halb,
                    oben + (dy + 1) * halb,
                )
                leinwand.drawBitmap(eintrag.bild, null, zielfeld, bildpinsel)
            }
        }
    }

    /**
     * Liest eine Kachel -- und hebt sie auf.
     *
     * Der Schluessel traegt die Nummer der Kartendatei mit: Zwei Karten
     * koennen dieselbe Kachelnummer haben und meinen Verschiedenes.
     */
    private fun geleseneKachel(rang: Int, datei: Kartendatei, zoom: Int, x: Int, y: Int): Kachel? {
        val schluessel = (rang.toLong() shl 60) or schluesselVon(zoom, x, y)
        synchronized(kachelspeicher) { kachelspeicher[schluessel] }?.let { return it }
        val frisch = try {
            datei.kachel(zoom, x, y)
        } catch (fehler: Kartenfehler) {
            null
        } ?: return null
        synchronized(kachelspeicher) { kachelspeicher[schluessel] = frisch }
        return frisch
    }

    private fun baue(zoom: Int, x: Int, y: Int): Eintrag? {
        val schichten = ArrayList<Schicht>()
        for ((rang, datei) in karten.withIndex()) {
            for (k in 0..GROB_MAX) {
                val quellzoom = zoom - k
                if (quellzoom < datei.zoomKleinste) break
                if (quellzoom > datei.zoomGroesste) continue
                val qx = x shr k
                val qy = y shr k
                // Eine kaputte Kachel darf die anderen Karten nicht verdecken.
                val kachel = geleseneKachel(rang, datei, quellzoom, qx, qy) ?: continue
                schichten.add(Schicht(kachel, 1 shl k, x - (qx shl k), y - (qy shl k), rang))
                break
            }
        }
        if (schichten.isEmpty()) return null
        // Grob zuerst, fein darueber. Bei gleicher Stufe gewinnt die Karte,
        // die der Aufrufer vorn eingereiht hat -- sie kommt zuletzt und liegt
        // damit oben.
        schichten.sortWith(
            compareBy<Schicht> { it.kachel.zoom }.thenByDescending { it.rang },
        )

        val bild = maler.leeresBild()
        // DAS FOTO ZUERST, die Zeichnung darueber. Wer die Reihenfolge
        // umdreht, hat ein Satellitenbild und keine Karte mehr.
        if (zeigtBilder) {
            try {
                bilder?.kachel(zoom, x, y)?.let { maler.maleBild(bild, it, zoom, x, y) }
            } catch (fehler: Exception) {
                // Eine kaputte Bildkachel darf die Karte nicht anhalten.
            }
        }
        for (schicht in schichten) {
            maler.maleAuf(bild, schicht.kachel, schicht.teilung, schicht.teilX, schicht.teilY)
        }

        // Schummerung erst ab Zoom 8. Die Hoehendatei deckt bisher nur
        // Oesterreich; bei Zoom 4 bis 7 laege sonst ein hartkantiges graues
        // Rechteck mitten in Europa, das wie ein Anzeigefehler aussieht und
        // nicht wie Gelaende. Ab Zoom 8 fuellt der beschummerte Bereich den
        // Schirm, und die Kante liegt ausserhalb.
        // KEINE SCHUMMERUNG UEBER EINEM FOTO. Das Bild traegt seine
        // Gelaendeform selbst -- Licht und Schatten stehen darin. Ein zweiter,
        // gerechneter Schatten darueber macht daraus einen grauen Schleier.
        if (stil.schummert && zoom >= 8 && !(zeigtBilder && bilder != null)) {
            // Eine fehlende oder kaputte Hoehenkachel darf die Karte nicht
            // anhalten: Ohne Schummerung ist sie flach, aber richtig.
            try {
                val hoehe = hoehen?.kachel(zoom, x, y)
                // NICHT MEHR ALS ZWEI STUFEN HOCHRECHNEN. Die Hoehendaten
                // enden bei Zoom 10 und sind 90 Meter grob. Auf Zoom 13 achtfach
                // vergroessert ergeben sie einen weichen grauen Schleier ueber
                // der ganzen Karte -- genau das, was auf dem Bildschirmfoto
                // vom 05.08.2026 verwaschen aussah. Ein Schatten, der nichts
                // mehr ueber das Gelaende sagt, verdeckt nur die Linien, die
                // etwas sagen.
                if (hoehe != null && zoom - hoehe.zoom <= 2) {
                    maler.schummere(bild, zoom, x, y, hoehe)
                }
            } catch (fehler: Kartenfehler) {
                // still weiterzeichnen
            }
        }
        return Eintrag(bild, schichten)
    }

    private fun hole(zoom: Int, x: Int, y: Int): Eintrag? {
        val schluessel = schluesselVon(zoom, x, y)
        val vorhanden = speicher[schluessel]
        if (vorhanden != null) return vorhanden
        // Eine Stelle, an der schon einmal keine Karte etwas hatte, wird nicht
        // in jedem Bild neu beauftragt.
        if (leerErkannt.containsKey(schluessel)) return null
        if (!unterwegs.add(schluessel)) return null
        val ziel = auftrag ?: run {
            unterwegs.remove(schluessel)
            return null
        }
        ziel.post {
            val gebaut = try {
                baue(zoom, x, y)
            } catch (fehler: Kartenfehler) {
                // Eine kaputte Kachel darf die Karte nicht anhalten. Sie bleibt
                // leer, der Rest der Karte laeuft weiter.
                null
            }
            hierher.post {
                unterwegs.remove(schluessel)
                if (gebaut == null) {
                    leerErkannt[schluessel] = true
                } else {
                    speicher.put(schluessel, gebaut)?.bild?.recycle()
                    invalidate()
                }
            }
        }
        return null
    }

    companion object {

        private const val KACHELKANTE = 256
        private const val AUFGEHOBEN = 64
        private val LN2 = ln(2.0).toFloat()

        /** Wie viele Stufen hoechstens aus einer groeberen Kachel hochgerechnet werden. */
        private const val GROB_MAX = 4

        /**
         * Wie weit man ueber die kleinste Stufe der Karte hinaus herauszoomen
         * darf.
         *
         * Rueckmeldung vom 06.08.2026: "ich will noch weiter rauszoomen
         * koennen." Auf einem Telefon im Hochformat passt Europa bei Stufe 4
         * nicht ins Bild. Gezeichnet wird dann weiter aus den Kacheln der
         * Stufe 4, nur kleiner -- die Karte wird also nicht groeber, sondern
         * nur weiter.
         *
         * Zwei Stufen und nicht mehr: Bei drei waere eine Kachel noch 84
         * Bildpunkte gross, die Beschriftung unleserlich, und der Bildschirm
         * fragte ueber hundertachtzig Kachelplaetze ab.
         */
        private const val WEITER_RAUS = 2

        fun schluesselVon(zoom: Int, x: Int, y: Int): Long =
            (zoom.toLong() shl 56) or (x.toLong() shl 28) or y.toLong()

        /**
         * Kantenlaenge eines Ortsfeldes in Rastereinheiten der Anzeigestufe.
         *
         * 736 Rastereinheiten sind 46 Kachelbildpunkte (das Raster hat 4096 je
         * Kachel, das Kachelbild 256) und damit rund 46 dp auf dem Schirm --
         * gerade so viel, dass zwei Ortsnamen nebeneinander noch Luft haben.
         */
        private const val FELD_RASTER = 736L

        /** Grad in Kachelpunkte (256 je Kachel) der Zoomstufe. */
        fun weltpunkt(lon: Double, lat: Double, zoom: Int): Pair<Double, Double> {
            val breite = (1 shl zoom) * 256.0
            val x = (lon + 180.0) / 360.0 * breite
            val begrenzt = min(max(lat, -85.05112878), 85.05112878)
            val s = sin(Math.toRadians(begrenzt))
            val y = (0.5 - ln((1 + s) / (1 - s)) / (4 * Math.PI)) * breite
            return Pair(x, y)
        }

        fun laengeVon(x: Double, zoom: Int): Double {
            val breite = (1 shl zoom) * 256.0
            var grad = x / breite * 360.0 - 180.0
            while (grad > 180.0) grad -= 360.0
            while (grad < -180.0) grad += 360.0
            return grad
        }

        fun breiteVon(y: Double, zoom: Int): Double {
            val breite = (1 shl zoom) * 256.0
            val n = Math.PI * (1 - 2 * y / breite)
            return Math.toDegrees(atan(sinh(n)))
        }
    }
}
