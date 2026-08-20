package org.compasszero.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import org.compasszero.karte.Bildkachel
import org.compasszero.karte.Hoehenkachel
import org.compasszero.karte.Kachel
import org.compasszero.karte.Kartenformat

/**
 * Zeichnet eine Kachel einmal in ein Bild und hebt es auf.
 *
 * WARUM UEBER EIN BILD UND NICHT DIREKT: Eine Kachel traegt bis zu ein paar
 * tausend Linienzuege. Sie bei jedem Bildaufbau neu zu zeichnen hiesse, beim
 * Schieben der Karte fuenfzigmal in der Sekunde dieselbe Arbeit zu tun. Auf
 * einem Geraet von 2014 ist das der Unterschied zwischen einer Karte, die
 * folgt, und einer, die ruckelt. Gezeichnet wird deshalb einmal je Kachel;
 * beim Schieben wird nur noch das fertige Bild versetzt.
 *
 * Beschriftungen und Punktzeichen gehoeren NICHT ins Bild. Sie wuerden beim
 * Vergroessern mitverwaschen und beim Drehen auf dem Kopf stehen. Sie werden
 * ueber das Bild gelegt, in voller Schaerfe und immer waagrecht.
 */
class Kachelmaler(
    private val stil: Kartenstil,
    /** Kantenlaenge des Kachelbildes in Bildpunkten. */
    private val kante: Int = 256,
    /**
     * Ruhige Karte: laesst die beiden dichtesten Sorten weg.
     *
     * Max am 17.08.2026: "ich finde ist die karte mit informationen
     * ueberladen". Was die Flaeche wirklich zustellt, sind Baeche und
     * Regionsgrenzen -- ein feines blaues Netz und ein gestricheltes Gitter
     * ueber allem, beides in Gegenden ohne jeden Orientierungswert.
     *
     * WAS BEWUSST BLEIBT: Wege, Pfade, Fluesse und Staatsgrenzen. Danach
     * findet man sich zurecht, und eine Karte, die den Pfad weglaesst, damit
     * sie aufgeraeumt aussieht, ist in dieser App der falsche Handel.
     */
    private val ruhig: Boolean = false,
    /**
     * Ob ein Satellitenbild unter der Zeichnung liegt. Dann werden die
     * Flaechen weggelassen -- sonst deckt die Zeichnung das Bild zu.
     */
    private val flaechenlos: Boolean = false,
    /**
     * Sorten, die gar nicht gezeichnet werden.
     *
     * WARUM ES DAS GIBT (Max am 18.08.2026): "selbst mit allen ebenen aus ist
     * immer noch extrem viele linien auf der karte zu sehen. was ist das man
     * soll es ausschalten koennen." Die Ebenen-Schalter steuerten bis dahin
     * nur die PUNKTE -- Quellen, Huetten, Orte. Die gezeichnete Karte selbst,
     * also Strassen, Wege, Gewaesser und Grenzen, hatte ueberhaupt keinen
     * Schalter; sie war einfach immer da.
     *
     * Ueber einem Satellitenbild ist das der Unterschied zwischen einem Foto
     * und einem Foto unter einem Netz aus Strichen.
     */
    private val weglassen: Set<Int> = emptySet(),
) {


    private val pinsel = stil.neuerPinsel()

    // Eigener Pinsel fuers Foto: gefiltert, damit eine hochgerechnete grobe
    // Kachel weich statt klotzig wird, und ohne die Kappen- und
    // Verbindungseinstellungen der Linien, die hier nichts zu suchen haben.
    private val bildpinsel = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    private val pfad = Path()

    // Ein Strich, den es im Gelaende nicht gibt, wird gestrichelt gezeichnet.
    // Das ist die Kartenkonvention und zugleich das einzige Mittel, eine
    // Grenze von einem Weg zu unterscheiden, ohne auf die Farbe angewiesen zu
    // sein -- im Sparmodus ist alles grau.
    //
    // ZWEI MUSTER, NICHT EINES. Bis zum 05.08.2026 waren Staats- und
    // Landesgrenze derselbe feine Strich in zwei aehnlichen Violettoenen. Auf
    // dem Geraet war das Ergebnis, dass Max die Laendergrenzen schlicht
    // vermisste -- sie WAREN da (in der gebauten Europakarte stehen auf Zoom 4
    // bereits 1275 Grenzobjekte), aber sie waren neben den kraeftigen braunen
    // Strassen nicht als Grenze zu erkennen.
    //
    // Jetzt trennt sie das, was sie auf jeder gedruckten Karte trennt: die
    // Staatsgrenze als Strich-Punkt-Linie, die Landesgrenze als feiner Strich.
    // Das Muster traegt die Auskunft, nicht die Farbe -- im Sparmodus ist
    // alles grau, und dort bleibt der Unterschied trotzdem lesbar.
    private val strichelungStaat = android.graphics.DashPathEffect(
        floatArrayOf(
            11f * (kante / 256f), 4f * (kante / 256f),
            2.5f * (kante / 256f), 4f * (kante / 256f),
        ),
        0f,
    )
    private val strichelungRegion = android.graphics.DashPathEffect(
        floatArrayOf(5f * (kante / 256f), 4f * (kante / 256f)), 0f,
    )

    private fun strichelung(sorte: Int): android.graphics.DashPathEffect? = when (sorte) {
        Kartenformat.GRENZE -> strichelungStaat
        Kartenformat.GRENZE_REGION -> strichelungRegion
        else -> null
    }

    private val schummerpinsel = Paint(Paint.FILTER_BITMAP_FLAG)

    /**
     * Legt die Schummerung ueber ein fertiges Kachelbild.
     *
     * Multipliziert, nicht ueberblendet: Die Farben der Karte bleiben, was sie
     * sind, und werden an den Schattenhaengen nur dunkler. Ueberblenden wuerde
     * Wald und Wiese in dieselbe graue Richtung ziehen.
     *
     * [hoehe] darf eine groebere Kachel sein als das Feld -- dann wird der
     * passende Ausschnitt gerechnet. Siehe `Hoehendatei.kachel`.
     *
     * [zoom], [kachelX], [kachelY] beschreiben das FELD, das das Bild zeigt --
     * nicht die Kachel, aus der die Linienzuege stammen. Seit eine grobe
     * Kachel vergroessert nachgezeichnet werden kann, sind das zwei
     * verschiedene Dinge, und ein Schatten an der falschen Stelle waere
     * schlimmer als kein Schatten.
     */
    fun schummere(bild: Bitmap, zoom: Int, kachelX: Int, kachelY: Int, hoehe: Hoehenkachel) {
        val stufen = zoom - hoehe.zoom
        if (stufen < 0) return
        val teil = 1 shl stufen
        // Wo das Feld innerhalb der Hoehenkachel liegt.
        val versatzX = kachelX - (hoehe.kachelX shl stufen)
        val versatzY = kachelY - (hoehe.kachelY shl stufen)
        if (versatzX < 0 || versatzY < 0 || versatzX >= teil || versatzY >= teil) return

        val n = hoehe.kante
        // Die Kachel selbst liegt auf 1..n-2; 0 und n-1 sind der Rand, der nur
        // fuer die Neigung da ist. Ohne ihn haette der aeusserste Punkt keinen
        // Nachbarn, seine Neigung waere null, und jede Kachel wuerde an ihrer
        // Kante aufhellen -- ein sichtbares Gitter ueber der ganzen Karte.
        val innen = n - 2
        val ausschnitt = maxOf(2, innen / teil)
        val x0 = 1 + versatzX * innen / teil
        val y0 = 1 + versatzY * innen / teil

        // EINE REIHE MEHR AUF JEDER SEITE, und gezeichnet wird nur der Kern.
        // Sonst hat die bilineare Vergroesserung am aeussersten halben
        // Bildpunkt keinen Nachbarn und klemmt ihn -- dieselbe Naht wie oben,
        // nur eine Stufe spaeter und viel feiner.
        val gerechnet = ausschnitt + 2
        val licht = IntArray(gerechnet * gerechnet)
        for (zeile in 0 until gerechnet) {
            for (spalte in 0 until gerechnet) {
                val sx = (x0 + spalte - 1).coerceIn(1, n - 2)
                val sy = (y0 + zeile - 1).coerceIn(1, n - 2)
                val links = hoehe.hoehe(sx - 1, sy)
                val rechts = hoehe.hoehe(sx + 1, sy)
                val oben = hoehe.hoehe(sx, sy - 1)
                val unten = hoehe.hoehe(sx, sy + 1)
                // Beleuchtung aus Nordwest, wie es auf gedruckten Karten
                // ueblich ist -- von einer anderen Seite kippt das Relief fuer
                // das Auge um und Taeler sehen aus wie Ruecken.
                val neigungX = (rechts - links).toFloat()
                val neigungY = (unten - oben).toFloat()
                val roh = (neigungX * LICHT_X + neigungY * LICHT_Y + LICHT_Z) /
                    Math.sqrt(
                        (neigungX * neigungX + neigungY * neigungY + 1.0).toDouble(),
                    ).toFloat()
                val wert = (HELL_MIN + (HELL_MAX - HELL_MIN) *
                    ((roh + 1f) / 2f).coerceIn(0f, 1f)).toInt()
                licht[zeile * gerechnet + spalte] = 0xFF000000.toInt() or
                    (wert shl 16) or (wert shl 8) or wert
            }
        }

        val schatten = Bitmap.createBitmap(licht, gerechnet, gerechnet, Bitmap.Config.ARGB_8888)
        val leinwand = Canvas(bild)
        schummerpinsel.xfermode = android.graphics.PorterDuffXfermode(
            android.graphics.PorterDuff.Mode.MULTIPLY,
        )
        leinwand.drawBitmap(
            schatten,
            android.graphics.Rect(1, 1, gerechnet - 1, gerechnet - 1),
            android.graphics.Rect(0, 0, kante, kante),
            schummerpinsel,
        )
        schatten.recycle()
    }

    /**
     * Zeichnet eine Kachel in ein frisches Bild.
     *
     * [teilung] groesser als 1 heisst: Gezeichnet wird nur ein AUSSCHNITT
     * dieser Kachel, naemlich das Feld ([teilX], [teilY]) eines Rasters von
     * teilung x teilung -- und zwar formatfuellend, also vergroessert.
     *
     * WOFUER: Der Europa-Ueberblick endet bei Zoom 10. Wer weiter hineinzoomte,
     * sah ausserhalb Oesterreichs ab Zoom 11 nichts als Weiss (Max am
     * 05.08.2026: "ab zoomstufe 12 die karte an vielen stellen nur noch
     * weiss"). Das ist die schlechteste aller Antworten: Eine leere Karte sagt
     * "hier ist nichts", obwohl "hier weiss ich nichts Genaueres" gemeint ist
     * -- und wer sich im Ernstfall darauf verlaesst, sucht einen Weg, den es
     * gibt, an einer Stelle, wo die Karte nur schweigt.
     *
     * Statt eines vergroesserten Kachelbildes -- das waere ein verwaschener
     * Klumpen -- werden die Linienzuege der groberen Kachel NEU gezeichnet.
     * Ergebnis: gestochen scharfe Striche, nur eben groeber vereinfacht. Das
     * ist die ehrliche Darstellung dessen, was die Daten hergeben.
     */
    fun male(kachel: Kachel, teilung: Int = 1, teilX: Int = 0, teilY: Int = 0): Bitmap {
        val bild = leeresBild()
        maleAuf(bild, kachel, teilung, teilX, teilY)
        return bild
    }

    /**
     * Ein leeres Kachelbild im Grundton.
     *
     * RGB_565 statt ARGB_8888: halber Speicher, und die Karte hat keine
     * Durchsichtigkeit noetig. Bei 64 aufgehobenen Kacheln sind das 8 statt
     * 16 MB.
     */
    /**
     * Legt das Satellitenbild als unterste Lage in die Kachel.
     *
     * Die gelieferte Bildkachel kann von einer GROEBEREN Stufe stammen -- der
     * Leser sagt ehrlich, welche er hat, statt stillschweigend zuzuschneiden.
     * Der passende Ausschnitt wird deshalb hier gerechnet. Faellt das weg,
     * liegt das Bild um ein Vielfaches verschoben unter der Zeichnung und
     * sieht auf den ersten Blick richtig aus.
     */
    fun maleBild(ziel: Bitmap, kachel: Bildkachel, zoom: Int, x: Int, y: Int) {
        val foto = try {
            BitmapFactory.decodeByteArray(kachel.roh, 0, kachel.roh.size)
        } catch (fehler: Exception) {
            null
        } ?: return
        try {
            val stufen = (zoom - kachel.zoom).coerceIn(0, 8)
            val teilung = 1 shl stufen
            val teilX = x - (kachel.x shl stufen)
            val teilY = y - (kachel.y shl stufen)
            if (teilX < 0 || teilY < 0 || teilX >= teilung || teilY >= teilung) return
            val breite = foto.width / teilung
            val hoehe = foto.height / teilung
            if (breite <= 0 || hoehe <= 0) return
            val aus = Rect(teilX * breite, teilY * hoehe, (teilX + 1) * breite, (teilY + 1) * hoehe)
            val hin = Rect(0, 0, kante, kante)
            Canvas(ziel).drawBitmap(foto, aus, hin, bildpinsel)
        } finally {
            // Das Bitmap wird nicht behalten: Bei einem Bildschirm voller
            // Kacheln haengen sonst schnell zwanzig entpackte Fotos im
            // Speicher, und auf einem alten Geraet ist das der Absturz.
            foto.recycle()
        }
    }

    fun leeresBild(): Bitmap {
        val bild = Bitmap.createBitmap(kante, kante, Bitmap.Config.RGB_565)
        Canvas(bild).drawColor(stil.hintergrund)
        return bild
    }

    /**
     * Zeichnet eine Kachel in ein VORHANDENES Bild, ohne es zu leeren.
     *
     * Damit lassen sich mehrere Karten uebereinanderlegen: erst die grobe
     * Uebersicht, dann das feine Paket darueber. Wo das feine Paket Daten hat,
     * deckt es die grobe Zeichnung mit seinen Flaechen zu; wo es keine hat,
     * bleibt die Uebersicht stehen. Genau das braucht eine Karte, die aus
     * Paketen mit verschiedenen Gebieten zusammengesetzt ist.
     */
    fun maleAuf(bild: Bitmap, kachel: Kachel, teilung: Int = 1, teilX: Int = 0, teilY: Int = 0) {
        val leinwand = Canvas(bild)
        if (teilung > 1) {
            leinwand.translate(-teilX.toFloat() * kante, -teilY.toFloat() * kante)
        }
        val massstab = kante.toFloat() * teilung / Kartenformat.RASTER

        for (sorte in Kartenstil.REIHENFOLGE) {
            if (sorte in weglassen) continue
            if (ruhig && sorte in RUHIG_WEG) continue
            if (ruhig && kachel.zoom <= UEBERSICHT_BIS && sorte in RUHIG_UEBERSICHT) continue
            // KEINE FLAECHEN UEBER EINEM SATELLITENBILD. Der gezeichnete Wald
            // ist eine gruene Flaeche und deckt das Foto darunter vollstaendig
            // zu -- man haette ein Bildpaket geladen und saehe die Zeichnung
            // wie zuvor. Die Linien bleiben: Wege und Grenzen sieht man auf
            // einem Satellitenbild kaum, und genau die sucht man dort.
            if (!flaechenlos && stil.fuellt(sorte)) {
                maleFlaechen(leinwand, kachel, sorte, massstab)
            }
            if (stil.zeichnetStrich(sorte)) maleStriche(leinwand, kachel, sorte, massstab)
        }
    }

    private fun maleFlaechen(leinwand: Canvas, kachel: Kachel, sorte: Int, massstab: Float) {
        pinsel.style = Paint.Style.FILL
        pinsel.color = stil.fuellfarbe(sorte)
        pinsel.pathEffect = null
        var i = 0
        while (i < kachel.objekte) {
            if (kachel.sorte[i].toInt() != sorte ||
                kachel.art[i].toInt() != Kartenformat.Art.AUSSENRING
            ) {
                i++
                continue
            }
            // Ein Aussenring und die Innenringe, die ihm unmittelbar folgen,
            // gehoeren zusammen. Mit der Gerade-Ungerade-Regel werden die
            // Innenringe damit zu Loechern. Getrennt je Aussenring, denn ueber
            // mehrere Flaechen hinweg wuerde die Regel zwei einander
            // ueberlappende Waelder gegenseitig ausloeschen.
            pfad.reset()
            pfad.fillType = Path.FillType.EVEN_ODD
            legeRing(kachel, i, massstab)
            var k = i + 1
            while (k < kachel.objekte &&
                kachel.sorte[k].toInt() == sorte &&
                kachel.art[k].toInt() == Kartenformat.Art.INNENRING
            ) {
                legeRing(kachel, k, massstab)
                k++
            }
            leinwand.drawPath(pfad, pinsel)
            i = k
        }
    }

    private fun legeRing(kachel: Kachel, objekt: Int, massstab: Float) {
        val a = kachel.anfang[objekt]
        val n = kachel.laenge[objekt]
        if (n < 3) return
        pfad.moveTo(kachel.x[a] * massstab, kachel.y[a] * massstab)
        for (k in 1 until n) {
            pfad.lineTo(kachel.x[a + k] * massstab, kachel.y[a + k] * massstab)
        }
        pfad.close()
    }

    private fun maleStriche(leinwand: Canvas, kachel: Kachel, sorte: Int, massstab: Float) {
        pinsel.style = Paint.Style.STROKE
        pinsel.color = stil.strichfarbe(sorte)
        pinsel.strokeWidth = stil.strichbreite(sorte, kachel.zoom) * (kante / 256f)
        pinsel.pathEffect = if (stil.gestrichelt(sorte)) strichelung(sorte) else null
        pfad.reset()
        pfad.fillType = Path.FillType.WINDING
        var etwas = false
        for (i in 0 until kachel.objekte) {
            if (kachel.sorte[i].toInt() != sorte) continue
            val art = kachel.art[i].toInt()
            if (art == Kartenformat.Art.PUNKT) continue
            val a = kachel.anfang[i]
            val n = kachel.laenge[i]
            if (n < 2) continue
            pfad.moveTo(kachel.x[a] * massstab, kachel.y[a] * massstab)
            for (k in 1 until n) {
                pfad.lineTo(kachel.x[a + k] * massstab, kachel.y[a + k] * massstab)
            }
            etwas = true
        }
        if (etwas) leinwand.drawPath(pfad, pinsel)
    }

    /**
     * NACH LAENGE ZU FILTERN GEHT NICHT -- hier stand am 18.08.2026 ein
     * Versuch, und er war falsch.
     *
     * Die Idee: Auf Max' Bildschirmfoto war Deutschland bei Zoom 6 eine
     * blaue Flaeche, und Fluesse sind dort gemessen 50,6 Prozent der
     * Zeichnung. Also nur noch die langen zeichnen -- der Rhein bleibt, der
     * Zufluss von zwei Kilometern faellt weg.
     *
     * WARUM ES NICHT GEHT: Die Geometrie liegt auf dieser Stufe in kurzen
     * Stuecken. Nachgemessen in der beigelegten Karte: 1547 Flussobjekte auf
     * Zoom 6, und das LAENGSTE misst 433 von 4096 Rastereinheiten. Eine
     * Schwelle von einem Achtel der Kachel loescht damit nicht die kleinen
     * Fluesse, sondern ALLE -- restlos, und ohne dass es jemandem auffaellt,
     * weil die Karte danach nur aufgeraeumt aussieht.
     *
     * Das ist der gefaehrlichste Fehler, den eine Karte haben kann: Sie sieht
     * besser aus und zeigt weniger, als sie weiss. Wer nach Wasser sucht,
     * findet keines mehr.
     *
     * Was stattdessen geht, steht im Kopf dieser Klasse: eine ganze Sorte auf
     * groben Stufen weglassen, sichtbar geschaltet -- eine Auslassung, die
     * der Nutzer kennt, statt einer, die er nicht sehen kann.
     */
    // NICHT MEHR PRIVAT: Die vier Gruppen unten stehen auch in der
    // Ebenenauswahl, und dieselbe Liste an zwei Stellen zu fuehren hiesse,
    // dass eine neue Sorte irgendwann nur in einer davon auftaucht.
    companion object {
        /**
         * Was die ruhige Karte weglaesst -- siehe Kopf der Klasse.
         *
         * WEG_FEIN steht hier, TUT ABER NICHTS -- und das ist am 18.08.2026
         * nachgemessen worden, nachdem hier lange das Gegenteil behauptet
         * wurde ("der grosse Posten: Zufahrten, Gehsteige, Radwege"). In
         * `tools/karte/bauen.py` steht die Zoomschwelle dieser Sorte auf 99;
         * sie wird also in keine Karte gebaut. In einer Stichprobe ueber
         * `deutschland-sued-detail.czk` kommt sie auf keiner Stufe vor.
         *
         * Die Sorte bleibt trotzdem in dieser Liste: Sollte sie je gebaut
         * werden, gehoert sie genau hierher.
         *
         * WAS "RUHIG" WIRKLICH WEGNIMMT, gemessen mit
         * `tools/karte/ruhig_messen.py` an den vollsten Kacheln je Stufe:
         *
         *   Zoom 11: 15,7 % der Stuetzpunkte, 28,3 % der Objekte
         *   Zoom 12: 14,9 % der Stuetzpunkte, 27,1 % der Objekte
         *   Zoom 13: 13,4 % der Stuetzpunkte, 30,2 % der Objekte
         *   Zoom 14: 10,2 % der Stuetzpunkte, 23,3 % der Objekte
         *
         * Davon tragen die Pfade allein 8,7 bis 13,0 Prozentpunkte. OHNE SIE
         * BLIEBEN RUND ZWEI PROZENT -- der Schalter war bis zum 18.08.2026
         * auf einer echten Karte fast wirkungslos, und niemandem ist es
         * aufgefallen, weil bei Zoom 6 gemessen worden war, wo es keine
         * dieser Sorten gibt.
         *
         * WEG_PFAD kam am 18.08.2026 dazu, auf Max' Entscheidung. Vorher stand
         * hier das Gegenteil: Pfade seien Wege, auf denen man wirklich geht,
         * und sie wegzulassen waere der falsche Handel. Das war eine
         * Vermutung darueber, was jemand braucht -- Max hat sie am Geraet
         * gesehen und anders entschieden. "Ruhig" ist die Uebersicht, und
         * wer den Steig sucht, schaltet zurueck.
         */
        val RUHIG_WEG = setOf(
            Kartenformat.WEG_FEIN,
            Kartenformat.WEG_PFAD,
            Kartenformat.BACH,
            Kartenformat.GRENZE_REGION,
        )

        /**
         * Was "Ruhig" auf den UEBERSICHTSSTUFEN zusaetzlich weglaesst.
         *
         * Gemessen in der beigelegten Karte auf Zoom 6: Fluesse sind 50,6
         * Prozent aller Stuetzpunkte, Hauptstrassen 25,1, Grenzen 15,3. Max
         * am 18.08.2026: "auf der karte ist viel zu viel los als das man sich
         * irgenwie zurecht findet." Der groesste einzelne Posten ist damit
         * benannt, und auf einer Uebersicht von halb Europa hilft das
         * Flussnetz beim Zurechtfinden nicht -- die Kueste, die grossen Seen
         * und die Staatsgrenzen tun es.
         *
         * BIS ZOOM 9, danach nicht mehr: Wer soweit hineingeht, sucht Wasser
         * und nicht Uebersicht.
         */
        /** Die vier Gruppen, die sich einzeln abschalten lassen. */
        val WEGE = setOf(
            Kartenformat.WEG_HAUPT, Kartenformat.WEG_NEBEN,
            Kartenformat.WEG_PFAD, Kartenformat.WEG_FEIN,
        )
        val GEWAESSER = setOf(
            Kartenformat.WASSER, Kartenformat.FLUSS,
            Kartenformat.BACH, Kartenformat.GLETSCHER,
        )
        val GRENZEN = setOf(Kartenformat.GRENZE, Kartenformat.GRENZE_REGION)
        val FLAECHEN = setOf(
            Kartenformat.WALD, Kartenformat.OFFEN,
            Kartenformat.SUMPF, Kartenformat.SIEDLUNG,
        )

        val RUHIG_UEBERSICHT = setOf(Kartenformat.FLUSS)
        const val UEBERSICHT_BIS = 9

        // Licht aus Nordwest, 45 Grad ueber dem Horizont -- die Richtung, die
        // auf gedruckten Karten seit jeher benutzt wird. Von der anderen Seite
        // kippt das Relief fuer das Auge um: Taeler sehen aus wie Ruecken.
        const val LICHT_X = -0.5f
        const val LICHT_Y = -0.5f
        const val LICHT_Z = 0.7071f

        // Wie stark die Schummerung eingreift. 255 hiesse unveraendert; unter
        // 150 wuerde die Karte an Schatthaengen unlesbar.
        const val HELL_MIN = 170f
        const val HELL_MAX = 255f
    }
}
