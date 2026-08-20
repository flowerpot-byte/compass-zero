package org.compasszero.android

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Der Koordinatenleser.
 *
 * Eine Koordinate wird im Ernstfall abgelesen und abgetippt -- von einem
 * Zettel, aus einem Funkspruch, von einem anderen Geraet. Die Fassung, die
 * jemand gerade zur Hand hat, ist genau die, die er eintippt. Deshalb steht
 * hier jede Schreibweise, die dabei herauskommen kann.
 */
class KoordinatenTest {

    private fun pruefe(eingabe: String, breite: Double, laenge: Double) {
        val ort = assertNotNull(Koordinaten.lies(eingabe), "\"$eingabe\" wurde nicht gelesen")
        assertTrue(
            abs(ort.breite - breite) < 0.001 && abs(ort.laenge - laenge) < 0.001,
            "\"$eingabe\" ergab ${ort.breite}/${ort.laenge}, erwartet $breite/$laenge",
        )
    }

    @Test
    fun dezimalMitLeerzeichenUndKomma() {
        pruefe("47.8 13.05", 47.8, 13.05)
        pruefe("47.8, 13.05", 47.8, 13.05)
        pruefe("47.8;13.05", 47.8, 13.05)
    }

    @Test
    fun dezimalkommaWieAufDeutschenGeraeten() {
        // Ein deutsches Handy schreibt 47,8 -- und wer das abtippt, hat recht.
        pruefe("47,8 13,05", 47.8, 13.05)
    }

    @Test
    fun himmelsrichtungVorneUndHinten() {
        pruefe("47.8N 13.05E", 47.8, 13.05)
        pruefe("N 47.8 E 13.05", 47.8, 13.05)
        // Auf Deutsch heisst Ost "O". Wer die App auf Deutsch bedient,
        // schreibt sie auch so.
        pruefe("47.8N 13.05O", 47.8, 13.05)
    }

    @Test
    fun dieHimmelsrichtungDrehtDieReihenfolge() {
        // Steht die Laenge vorn, sagt der Buchstabe es -- und die App dreht.
        pruefe("13.05E 47.8N", 47.8, 13.05)
        pruefe("O 13.05 N 47.8", 47.8, 13.05)
    }

    @Test
    fun suedenUndWestenSindNegativ() {
        pruefe("33.9S 18.4E", -33.9, 18.4)
        pruefe("40.7N 74.0W", 40.7, -74.0)
        pruefe("-33.9 -70.6", -33.9, -70.6)
    }

    @Test
    fun gradMinutenSekunden() {
        pruefe("47°48'00\"N 13°03'00\"E", 47.8, 13.05)
        pruefe("47° 48' N, 13° 3' E", 47.8, 13.05)
        // Ohne Gradzeichen -- auf einem Handy kommt das oft gar nicht mit.
        pruefe("47 48 00 N 13 03 00 E", 47.8, 13.05)
    }

    @Test
    fun gradUndDezimalminuten() {
        // Die Schreibweise aus dem Funk und von den meisten Handgeraeten.
        pruefe("47° 48.0' N 13° 3.0' E", 47.8, 13.05)
    }

    @Test
    fun dasVorzeichenGehoertZumGanzenWinkel() {
        // -47°30' sind 47,5 Grad nach Sueden und nicht 46,5. Wer das falsch
        // rechnet, landet 110 Kilometer daneben.
        pruefe("-47 30 0 -13 0 0", -47.5, -13.0)
    }

    @Test
    fun unmoeglicheWerteWerdenAbgelehnt() {
        // Lieber nichts finden als irgendwohin springen.
        assertNull(Koordinaten.lies("91 13"), "Breite 91 gibt es nicht")
        assertNull(Koordinaten.lies("47 181"), "Laenge 181 gibt es nicht")
        assertNull(Koordinaten.lies("47 70 0 N 13 0 0 E"), "70 Minuten gibt es nicht")
    }

    @Test
    fun unsinnGibtNichtsZurueck() {
        for (unfug in listOf("", "   ", "Salzburg", "47", "47 13 05 99 12", "N N")) {
            assertNull(Koordinaten.lies(unfug), "\"$unfug\" haette nichts ergeben muessen")
        }
    }

    @Test
    fun zweiAngabenDerselbenAchseSindEinTippfehler() {
        // "N 47 N 13" ist keine Stelle. Sie zu erraten hiesse, sich eine
        // auszudenken.
        assertNull(Koordinaten.lies("N 47 N 13"))
        assertNull(Koordinaten.lies("E 13 O 47"))
    }

    @Test
    fun ohneAnhaltspunktGiltBreiteZuerst() {
        // So steht es auf jeder Karte und in jedem Funkverkehr. "13 47" wird
        // NICHT stillschweigend gedreht, nur weil Salzburg plausibler waere
        // als ein Punkt in Somalia.
        pruefe("13 47", 13.0, 47.0)
    }

    @Test
    fun dieRueckgabeIstLesbarUndLaesstSichWiederEinlesen() {
        // NICHT auf den Punkt als Dezimalzeichen festgenagelt: Die Ausgabe
        // folgt der Sprache des Geraets, und auf einem deutschen steht dort
        // "47,8000°N" -- das ist richtig so und dieselbe Form wie in der
        // Standzeile unter der Karte.
        //
        // Was hier zaehlt, ist der Kreis: Was die App schreibt, muss sie auch
        // wieder lesen koennen. Sonst kann man eine abgelesene Stelle nicht
        // zurueckgeben.
        for ((breite, laenge) in listOf(47.8 to 13.05, -33.9 to -18.4, 0.0 to 0.0)) {
            val text = Koordinaten.schreibe(breite, laenge)
            assertTrue("°" in text && ("N" in text || "S" in text), text)
            val zurueck = assertNotNull(Koordinaten.lies(text), "\"$text\" liest sich nicht zurueck")
            assertTrue(
                abs(zurueck.breite - breite) < 0.001 && abs(zurueck.laenge - laenge) < 0.001,
                "\"$text\" ergab ${zurueck.breite}/${zurueck.laenge}",
            )
        }
    }
}
