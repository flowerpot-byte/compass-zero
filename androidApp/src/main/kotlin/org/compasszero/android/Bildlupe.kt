package org.compasszero.android

import kotlin.math.max
import kotlin.math.min

/**
 * Die Rechnerei hinter der Bildlupe: Einpassung, Drehung, Grenzen.
 *
 * WARUM ES DIE LUPE GIBT (Max am 04.08.2026): "Bei den Artikeln gibt es noch
 * viel zu wenig wirklich sinnvolle Zeichnungen, vor allem bei Bauanleitungen
 * und Knoten -- kann man wenig oder schlecht erkennen." Der zweite Teil des
 * Satzes ist der schwerere. Am 04.08.2026 am Geraet nachgemessen: Eine Skizze
 * ist 900 Bildpunkte breit und steht im Artikel in einer Spalte von rund 1000
 * Bildpunkten. Eine Beschriftung, die in der Zeichnung 13 Punkte hoch ist,
 * kommt damit bei etwa 14 Punkten heraus -- auf einem Telefon in der Hand ist
 * das an der Grenze. NOCH EINE ZEICHNUNG MEHR AENDERT DARAN NICHTS: es ist
 * dieselbe Groesse. Deshalb zuerst das Vergroessern, dann weitere Bilder.
 *
 * DIE DREHUNG IST DER ERSTE SCHRITT, DAS ZOOMEN DER ZWEITE. Fast jede Skizze
 * im Paket ist querformatig (900 breit, 560 bis 680 hoch), das Telefon ist
 * hochkant. Nachgerechnet fuer 900 mal 650 auf 1080 mal 2160:
 *   im Artikel (Spalte rund 990 breit)   Massstab 1,10
 *   bildschirmfuellend aufrecht          Massstab 1,20  -- also so gut wie nichts
 *   bildschirmfuellend gedreht           Massstab 1,66  -- gut die Haelfte mehr
 * BEGRENZT WIRD IN BEIDEN FAELLEN DIE BREITE und nicht die Hoehe; wer das
 * uebersieht, erwartet vom Drehen das Doppelte und bekommt ein Drittel. Die
 * Haelfte ist trotzdem der Unterschied zwischen "ahnbar" und "lesbar", und sie
 * kostet keine Geste. Deshalb oeffnet die Lupe eine Querzeichnung gleich
 * gedreht -- und nicht erst, wenn jemand das Telefon kippt: Im Ernstfall haelt
 * man mit einer Hand das Telefon und mit der anderen den Verband. Alles
 * darueber hinaus macht das Zoomen, bis zum Achtfachen.
 *
 * HIER STEHT NUR RECHNUNG UND KEIN EINZIGER VIEW -- so laesst sie sich ohne
 * laufende App pruefen, genau wie bei `Artikeltext`. Die Ansicht dazu ist
 * `Bildschau`.
 */
object Bildlupe {

    /**
     * Der Massstab, bei dem das ganze Bild in die Flaeche passt.
     *
     * Bewusst ohne Begrenzung nach oben: Ein kleines Bild auf einem grossen
     * Schirm DARF vergroessert werden. Eine Skizze ist gezeichnet und nicht
     * fotografiert, sie wird beim Vergroessern nicht koerniger, nur weicher.
     */
    fun einpassung(bildBreite: Int, bildHoehe: Int, flaecheBreite: Int, flaecheHoehe: Int): Float {
        if (bildBreite <= 0 || bildHoehe <= 0 || flaecheBreite <= 0 || flaecheHoehe <= 0) return 1f
        return min(flaecheBreite.toFloat() / bildBreite, flaecheHoehe.toFloat() / bildHoehe)
    }

    /**
     * Lohnt sich die Drehung um 90 Grad?
     *
     * Nur wenn sie SPUERBAR mehr bringt. Ein Bild, das gedreht drei Prozent
     * groesser wird, waere gedreht ein Aergernis und keine Hilfe: Wer es
     * ansieht, muss den Kopf legen, und dafuer muss etwas herausspringen.
     */
    fun drehenLohnt(bildBreite: Int, bildHoehe: Int, flaecheBreite: Int, flaecheHoehe: Int): Boolean {
        val aufrecht = einpassung(bildBreite, bildHoehe, flaecheBreite, flaecheHoehe)
        val gedreht = einpassung(bildHoehe, bildBreite, flaecheBreite, flaecheHoehe)
        return gedreht >= aufrecht * LOHNT_AB
    }

    /**
     * Haelt den Massstab zwischen "ganz sichtbar" und dem Hoechstwert.
     *
     * NACH UNTEN IST DIE EINPASSUNG DIE GRENZE: Kleiner als ganz sichtbar
     * ergibt kein Bild, sondern eine Briefmarke in einer leeren Flaeche, und
     * beim Loslassen einer Zwei-Finger-Geste rutscht man sonst regelmaessig
     * dorthin.
     */
    fun begrenzeMassstab(massstab: Float, einpassung: Float): Float =
        min(max(massstab, einpassung), einpassung * HOECHSTENS)

    /**
     * Haelt die Verschiebung so, dass kein Rand ins Leere laeuft.
     *
     * Ist der Inhalt kleiner als die Flaeche, gibt es nichts zu schieben --
     * dann steht er mittig, und zwar unabhaengig davon, wohin gewischt wurde.
     * Ist er groesser, darf man ihn genau so weit schieben, bis seine Kante an
     * der Kante der Flaeche steht.
     */
    fun begrenzeVerschiebung(verschiebung: Float, inhalt: Float, flaeche: Float): Float {
        if (inhalt <= flaeche) return (flaeche - inhalt) / 2f
        return min(max(verschiebung, flaeche - inhalt), 0f)
    }

    // Ab diesem Zugewinn wird gedreht geoeffnet. 1,25 heisst: ein Viertel
    // groesser. Bei einer 900 mal 650 grossen Skizze auf einem gewoehnlichen
    // Telefon sind es 1,38 -- der Wert liegt also nicht weit unter dem
    // tatsaechlichen Fall und wurde bewusst nicht tiefer gesetzt: Wer den Kopf
    // legen soll, muss etwas dafuer bekommen.
    const val LOHNT_AB = 1.25f

    // Wie weit ueber die Einpassung hinaus vergroessert werden darf. Acht
    // reicht, um die kleinste Beschriftung einer Skizze (13 Punkte auf 900
    // Breite) bildschirmfuellend zu lesen.
    const val HOECHSTENS = 8f
}
