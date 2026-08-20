package org.compasszero.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Der Dateiname aus einem Funkangebot -- die einzige Stelle des Austauschs, die
 * sich ohne Geraet und ohne Gegenstelle pruefen laesst, und zugleich die
 * gefaehrlichste.
 *
 * DER NAME KOMMT VON EINEM FREMDEN GERAET, lange bevor irgendeine Unterschrift
 * geprueft ist. Alles andere am Empfang haelt `core/transfer` fest; hier haengt
 * daran, ob eine empfangene Datei im Eingang landet oder anderswo.
 */
class PaketfunkTest {

    @Test
    fun einNormalerNameBleibtWieErIst() {
        assertEquals("europe-de.czp", Paketfunk.sichererName("europe-de.czp"))
    }

    @Test
    fun einNameOhneEndungBekommtSie() {
        // Der Lader nimmt nur .czp. Ohne diese Zeile laege die Datei im
        // Eingang und wuerde nie gesehen.
        assertEquals("paket.czp", Paketfunk.sichererName("paket"))
        // ANGEHAENGT UND NICHT ERSETZT: "paket.txt" wird "paket.txt.czp". So
        // bleibt sichtbar, wie die Datei drueben hiess -- und wer sie im
        // Eingang sucht, erkennt sie wieder. Ein Ersetzen der Endung waere
        // huebscher und wuerde Auskunft wegwerfen.
        assertEquals("paket.txt.czp", Paketfunk.sichererName("paket.txt"))
    }

    @Test
    fun pfadeWerdenAbgeschnitten() {
        // Der Angriff, um den es geht: aus dem Eingang herausschreiben.
        for (unfug in listOf(
            "../../databases/kaputt.czp",
            "/data/data/org.compasszero/files/kaputt.czp",
            "..\\..\\kaputt.czp",
            "ordner/unterordner/kaputt.czp",
        )) {
            val name = Paketfunk.sichererName(unfug)
            assertEquals("kaputt.czp", name, "\"$unfug\" wurde nicht entschaerft")
            assertFalse(name.contains('/'), "Schraegstrich in \"$name\"")
            assertFalse(name.contains('\\'), "Rueckstrich in \"$name\"")
            assertFalse(name.contains(".."), "Punktsprung in \"$name\"")
        }
    }

    @Test
    fun einNameAusLauterPunktenWirdNichtZuEinerVersteckenDatei() {
        // ".." und ".czp" wuerden beide zu etwas, das kein Name ist: ein
        // Verzeichnissprung und eine versteckte Datei ohne Rumpf.
        assertEquals("empfangen.czp", Paketfunk.sichererName(".."))
        assertEquals("empfangen.czp", Paketfunk.sichererName("."))
        assertEquals("czp", Paketfunk.sichererName(".czp").substringBefore('.'))
    }

    @Test
    fun ungewoehnlicheZeichenWerdenErsetztUndNichtVerworfen() {
        // Ersetzen statt ablehnen: An einem Anzeigenamen darf keine
        // Uebertragung scheitern -- der Name ist Beschriftung, die
        // Sicherheitsentscheidung faellt an der Unterschrift.
        val name = Paketfunk.sichererName("paket ä\"'`;|&\$(rm -rf).czp")
        assertTrue(name.endsWith(".czp"), name)
        assertTrue(name.all { it.isLetterOrDigit() || it in "._-" }, "unerlaubtes Zeichen in \"$name\"")
    }

    @Test
    fun einSehrLangerNameWirdGekuerzt() {
        val name = Paketfunk.sichererName("a".repeat(500) + ".czp")
        assertTrue(name.length <= 84, "Name ist ${name.length} Zeichen lang")
        assertTrue(name.endsWith(".czp"), name)
    }

    @Test
    fun einLeererNameFuehrtZuEinemBrauchbaren() {
        assertEquals("empfangen.czp", Paketfunk.sichererName(""))
        assertEquals("empfangen.czp", Paketfunk.sichererName("///"))
    }

    /**
     * Die geschaetzte Dauer. Sie soll jemanden davon abhalten, eine 400-MB-Karte
     * ueber Funk zu schicken -- deshalb muss sie bei grossen Dateien laut
     * werden und darf bei kleinen nicht laecherlich wirken.
     */
    @Test
    fun dieDauerIstEineGroessenordnungUndKeineNull() {
        assertTrue(Funk.dauerSekunden(1) >= 1, "eine winzige Datei darf nicht 0 Sekunden dauern")
        // Die drei Groessen aus dem Entwurf, und die Anzeige muss zu ihnen
        // passen -- daran haengt, ob jemand eine Karte ueber Funk schickt:
        //   Inhaltspaket   10,4 MB -> 69 Sekunden (Entwurf: 71 s)
        //   Ueberblickskarte 437 MB -> 49 Minuten (Entwurf: 50 min)
        //   Detailkarte      2 GB   -> 3,7 Stunden (Entwurf: 3,9 h)
        assertTrue(Funk.dauertext(10_400_000).contains("Sekunden"), Funk.dauertext(10_400_000))
        assertTrue(Funk.dauertext(437_000_000).contains("Minuten"), Funk.dauertext(437_000_000))
        assertTrue(Funk.dauertext(2_000_000_000).contains("Stunden"), Funk.dauertext(2_000_000_000))
    }
}
