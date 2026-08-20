package org.compasszero.android

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EigenkarteTest {

    @Test
    fun einPunktUeberstehtSchreibenUndLesen() {
        val karte = Eigenkarte()
        karte.setzePunkt(13.0432109, 47.8012345, "Quelle hinterm Steinbruch")

        val zurueck = Eigenkarte.ausText(karte.alsText())

        assertEquals(1, zurueck.punkte.size)
        val p = zurueck.punkte[0]
        assertEquals("Quelle hinterm Steinbruch", p.name)
        assertEquals(13.0432109, p.laenge, 1e-7)
        assertEquals(47.8012345, p.breite, 1e-7)
    }

    @Test
    fun einWegUeberstehtSchreibenUndLesen() {
        val karte = Eigenkarte()
        karte.legeWegAn("Rückweg über die Alm", listOf(13.1 to 47.8, 13.2 to 47.85, 13.25 to 47.9))

        val zurueck = Eigenkarte.ausText(karte.alsText())

        assertEquals(1, zurueck.wege.size)
        assertEquals("Rückweg über die Alm", zurueck.wege[0].name)
        assertEquals(3, zurueck.wege[0].stellen.size)
        assertEquals(13.25, zurueck.wege[0].stellen[2].first, 1e-7)
    }

    // Der Grund fuer das zeilenweise Format. Faellt eine Zeile aus, darf nur
    // dieser eine Eintrag fehlen -- nicht die ganze Sammlung.
    @Test
    fun eineKaputteZeileKostetNurSichSelbst() {
        val text = buildString {
            append("P\t1\t13.0\t47.0\tQuelle\n")
            append("P\tzwei\tvoellig\tkaputt\tMuell\n")
            append("P\t3\t13.5\t47.5\tHöhle\n")
        }

        val karte = Eigenkarte.ausText(text)

        assertEquals(2, karte.punkte.size)
        assertEquals(listOf("Quelle", "Höhle"), karte.punkte.map { it.name })
    }

    @Test
    fun stellenAusserhalbDerErdeWerdenVerworfen() {
        val karte = Eigenkarte.ausText("P\t1\t999.0\t47.0\tUnfug\nP\t2\t13.0\t47.0\tEcht\n")

        assertEquals(1, karte.punkte.size)
        assertEquals("Echt", karte.punkte[0].name)
    }

    // Tabulator und Umbruch sind die Trennzeichen. Kaemen sie durch, zerlegte
    // sich die Zeile beim naechsten Start falsch und der Name waere zerrissen.
    @Test
    fun trennzeichenImNamenZerlegenDieZeileNicht() {
        val karte = Eigenkarte()
        karte.setzePunkt(13.0, 47.0, "Quelle\tam\nBach")

        val zurueck = Eigenkarte.ausText(karte.alsText())

        assertEquals(1, zurueck.punkte.size)
        assertEquals("Quelle am Bach", zurueck.punkte[0].name)
    }

    // Wer den mittleren von drei Punkten loescht und einen neuen setzt, bekaeme
    // bei einer Zaehlung nach Anzahl die Nummer eines noch vorhandenen.
    @Test
    fun eineNummerWirdNieZweimalVergeben() {
        val karte = Eigenkarte()
        val ersteR = karte.setzePunkt(13.0, 47.0, "eins")
        val zweiter = karte.setzePunkt(13.1, 47.1, "zwei")
        karte.setzePunkt(13.2, 47.2, "drei")

        karte.loeschePunkt(zweiter.nummer)
        val neuer = karte.setzePunkt(13.3, 47.3, "vier")

        assertNotEquals(ersteR.nummer, neuer.nummer)
        val nummern = karte.punkte.map { it.nummer }
        assertEquals(nummern.size, nummern.toSet().size, "eine Nummer wurde doppelt vergeben")
    }

    @Test
    fun punkteUndWegeTeilenSichDenNummernkreis() {
        val karte = Eigenkarte()
        val punkt = karte.setzePunkt(13.0, 47.0, "Punkt")
        val weg = karte.legeWegAn("Weg", listOf(13.0 to 47.0))

        assertNotEquals(punkt.nummer, weg.nummer)
    }

    @Test
    fun umbenennenUndLoeschenGreifen() {
        val karte = Eigenkarte()
        val p = karte.setzePunkt(13.0, 47.0, "alt")

        assertTrue(karte.benennePunkt(p.nummer, "neu"))
        assertEquals("neu", karte.punkte[0].name)
        assertTrue(karte.loeschePunkt(p.nummer))
        assertTrue(karte.leer)
    }

    @Test
    fun leereDateiErgibtLeereKarte() {
        val ordner = File(System.getProperty("java.io.tmpdir"), "cz-eigen-${System.nanoTime()}")
        ordner.mkdirs()
        try {
            assertTrue(Eigenkartendatei(ordner).lade().leer)
        } finally {
            ordner.deleteRecursively()
        }
    }

    @Test
    fun gesichertesWirdWiederGefunden() {
        val ordner = File(System.getProperty("java.io.tmpdir"), "cz-eigen-${System.nanoTime()}")
        ordner.mkdirs()
        try {
            val ablage = Eigenkartendatei(ordner)
            val karte = Eigenkarte()
            karte.setzePunkt(13.0432109, 47.8012345, "Quelle")
            karte.legeWegAn("Heimweg", listOf(13.0 to 47.0, 13.1 to 47.1))
            ablage.sichere(karte)

            val zurueck = ablage.lade()

            assertEquals(1, zurueck.punkte.size)
            assertEquals(1, zurueck.wege.size)
            assertEquals("Quelle", zurueck.punkte[0].name)
            assertEquals("Heimweg", zurueck.wege[0].name)
        } finally {
            ordner.deleteRecursively()
        }
    }

    // Beim Sichern darf nie ein halber Stand entstehen. Geprueft wird, dass
    // nach dem Schreiben keine Nebendatei liegenbleibt -- sie waere das Zeichen
    // eines abgebrochenen Vorgangs.
    @Test
    fun nachDemSichernLiegtKeineNebendateiHerum() {
        val ordner = File(System.getProperty("java.io.tmpdir"), "cz-eigen-${System.nanoTime()}")
        ordner.mkdirs()
        try {
            val ablage = Eigenkartendatei(ordner)
            val karte = Eigenkarte()
            karte.setzePunkt(13.0, 47.0, "Quelle")
            ablage.sichere(karte)

            assertEquals(
                listOf(Eigenkartendatei.DATEINAME),
                ordner.list()!!.sorted(),
            )
        } finally {
            ordner.deleteRecursively()
        }
    }

    @Test
    fun zweimalSichernErsetztStattAnzuhaengen() {
        val ordner = File(System.getProperty("java.io.tmpdir"), "cz-eigen-${System.nanoTime()}")
        ordner.mkdirs()
        try {
            val ablage = Eigenkartendatei(ordner)
            val karte = Eigenkarte()
            karte.setzePunkt(13.0, 47.0, "Quelle")
            ablage.sichere(karte)
            ablage.sichere(karte)

            assertEquals(1, ablage.lade().punkte.size)
        } finally {
            ordner.deleteRecursively()
        }
    }

    // --- Sinnbilder an Wegpunkten (17.08.2026) -------------------------------

    @Test
    fun einSinnbildUeberlebtSpeichernUndLesen() {
        val karte = Eigenkarte()
        karte.setzePunkt(13.0, 47.0, "Quelle Nordhang", Sinnbild.WASSER)
        val wieder = Eigenkarte.ausText(karte.alsText())
        assertEquals(Sinnbild.WASSER, wieder.punkte[0].sinnbild)
        assertEquals("Quelle Nordhang", wieder.punkte[0].name)
    }

    @Test
    fun eineAltePunktzeileOhneSinnbildLaedtWeiter() {
        // Genau das Format, das vor dem 17.08.2026 geschrieben wurde: fuenf
        // Felder, kein Sinnbild. Max hat solche Punkte auf dem Handy.
        val alt = "P\t1\t13.0000000\t47.0000000\tAlter Punkt\n"
        val karte = Eigenkarte.ausText(alt)
        assertEquals(1, karte.punkte.size)
        assertEquals("Alter Punkt", karte.punkte[0].name)
        assertEquals(Sinnbild.KEINS, karte.punkte[0].sinnbild)
    }

    @Test
    fun einePunktzeileOhneSinnbildBekommtAuchKeinesGeschrieben() {
        // Damit eine aeltere Fassung des Programms die Datei unveraendert liest:
        // ohne Sinnbild bleibt es bei fuenf Feldern.
        val karte = Eigenkarte()
        karte.setzePunkt(13.0, 47.0, "Ohne")
        assertEquals(5, karte.alsText().trim().split('	').size)
    }

    @Test
    fun einErfundenesSinnbildWirdZuKeins() {
        // Die Datei ist die einzige unsignierte im Programm. Was von dort kommt,
        // wird gegen die feste Liste geprueft.
        val karte = Eigenkarte.ausText("P\t1\t13.0\t47.0\tName\trakete\n")
        assertEquals(Sinnbild.KEINS, karte.punkte[0].sinnbild)
    }

    @Test
    fun dasSinnbildLaesstSichNachtraeglichSetzenUndWiederEntfernen() {
        val karte = Eigenkarte()
        val p = karte.setzePunkt(13.0, 47.0, "Punkt")
        assertTrue(karte.setzeSinnbild(p.nummer, Sinnbild.GEFAHR))
        assertEquals(Sinnbild.GEFAHR, karte.punkte[0].sinnbild)
        assertTrue(karte.setzeSinnbild(p.nummer, Sinnbild.KEINS))
        assertEquals(Sinnbild.KEINS, karte.punkte[0].sinnbild)
    }

    @Test
    fun dieListeGruppiertNachSinnbildUndHaengtDieOhneHinten() {
        val karte = Eigenkarte()
        karte.setzePunkt(13.0, 47.0, "Zelt", Sinnbild.UNTERKUNFT)
        karte.setzePunkt(13.1, 47.0, "namenlos")
        karte.setzePunkt(13.2, 47.0, "Bach", Sinnbild.WASSER)
        karte.setzePunkt(13.3, 47.0, "Abzweig", Sinnbild.UEBERGANG)
        karte.setzePunkt(13.4, 47.0, "Quelle", Sinnbild.WASSER)

        val namen = karte.punkteGeordnet().map { it.name }
        // Wasser vor Unterkunft vor Uebergang -- die Reihenfolge aus
        // Sinnbild.ALLE; innerhalb des Wassers alphabetisch; "ohne" zuletzt.
        assertEquals(listOf("Bach", "Quelle", "Zelt", "Abzweig", "namenlos"), namen)
    }

    @Test
    fun dieOrdnungAendertDenBestandNicht() {
        // punkteGeordnet() ist eine Ansicht, keine Umsortierung: Die Datei soll
        // sich nicht aendern, bloss weil jemand die Liste geoeffnet hat.
        val karte = Eigenkarte()
        karte.setzePunkt(13.0, 47.0, "Zweiter", Sinnbild.WASSER)
        karte.setzePunkt(13.1, 47.0, "Erster")
        val vorher = karte.alsText()
        karte.punkteGeordnet()
        assertEquals(vorher, karte.alsText())
    }
}
