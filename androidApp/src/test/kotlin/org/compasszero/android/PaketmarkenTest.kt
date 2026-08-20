package org.compasszero.android

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.compasszero.content.LoadedPack
import org.compasszero.content.PackManifest
import org.compasszero.security.PackVerdict
import org.compasszero.security.TrustedKey

/**
 * Der Rueckstufungs-Schutz, Regel 5.
 *
 * WOGEGEN: Ein Paket, das einmal gueltig unterschrieben war, bleibt es fuer
 * immer. Wird ein Hinweis spaeter berichtigt -- der Tote-bergen-Tipp nannte
 * zehn Meter Abstand zur Wasserstelle, belegt sind 200 bis 350 --, verifiziert
 * die alte Fassung mit dem falschen Wert weiterhin einwandfrei.
 *
 * DIE LUECKE, DIE HIER GESCHLOSSEN WIRD: Vorher verglich die Uebernahme nur
 * gegen das GERADE GELADENE Paket. Faellt die uebernommene Datei weg, laeuft
 * die App wieder auf der Beigabe (Fassung 1) -- und danach liesse sich jede
 * Fassung dazwischen erneut einspielen, ohne dass etwas anschlaegt.
 *
 * Die acht Faelle stehen so im Entwurfspapier.
 */
class PaketmarkenTest {

    private val ordner: File = File(
        System.getProperty("java.io.tmpdir"),
        "compasszero-marken-${System.nanoTime()}",
    ).apply { mkdirs() }

    @AfterTest
    fun raeumAuf() {
        ordner.deleteRecursively()
    }

    private val kennung = "org.compasszero.europe-de"

    private fun paketMit(fassung: Int, id: String = kennung) = LoadedPack(
        manifest = PackManifest(
            schema = 1, id = id, version = fassung, language = "de",
            title = "Testpaket", kinds = listOf("tips"),
        ),
        tips = emptyList(), guides = emptyList(), agriculture = emptyList(),
        pois = emptyList(), phrases = emptyList(), terms = emptyList(),
    )

    private val geprueft = PackVerdict.Trusted(TrustedKey("entwicklung", ByteArray(32)))

    private fun entscheide(
        angeboten: Int,
        geladen: Int,
        bestaetigt: Boolean = false,
        urteil: PackVerdict = geprueft,
        angeboteneKennung: String = kennung,
    ) = Paketlader.entscheide(
        ordner = ordner,
        verdict = urteil,
        neueKennung = angeboteneKennung,
        neueFassung = angeboten,
        jetzige = paketMit(geladen),
        rueckstufungBestaetigt = bestaetigt,
    )

    // 1. Neuere Fassung wird angenommen, die Marke steigt.
    @Test
    fun neuereFassungWirdAngenommenUndDieMarkeSteigt() {
        Paketmarken.hebe(ordner, kennung, 5)
        val urteil = entscheide(angeboten = 6, geladen = 5)
        assertTrue(urteil is Uebernahme.Angenommen, "6 nach 5 muss durchgehen")
        Paketmarken.hebe(ordner, kennung, 6)
        assertEquals(6, Paketmarken.hoechste(ordner, kennung))
    }

    // 2. Gleiche Fassung wird angenommen, die Marke bleibt, wo sie ist.
    @Test
    fun gleicheFassungWirdAngenommenUndDieMarkeBleibt() {
        Paketmarken.hebe(ordner, kennung, 5)
        val urteil = entscheide(angeboten = 5, geladen = 5)
        assertTrue(urteil is Uebernahme.Angenommen, "dieselbe Fassung darf erneut abgelegt werden")
        Paketmarken.hebe(ordner, kennung, 5)
        assertEquals(5, Paketmarken.hoechste(ordner, kennung))
    }

    // 3. Aeltere Fassung wird OHNE Bestaetigung abgelehnt.
    @Test
    fun aeltereFassungWirdOhneBestaetigungAbgelehnt() {
        Paketmarken.hebe(ordner, kennung, 9)
        val urteil = entscheide(angeboten = 4, geladen = 9)
        assertTrue(urteil is Uebernahme.Rueckstufung, "4 nach 9 darf nicht stillschweigend gehen")
        assertEquals(9, urteil.marke)
        assertEquals(4, urteil.angeboten)
    }

    // 4. Der eigentliche Fehlerfaenger: Aeltere Fassung MIT Bestaetigung wird
    //    benutzt -- und die Marke bleibt trotzdem oben. Sonst waere der
    //    Notausgang ein Schalter, mit dem sich der Schutz dauerhaft abstellen
    //    liesse.
    @Test
    fun aeltereFassungMitBestaetigungWirdBenutztAberDieMarkeBleibtOben() {
        Paketmarken.hebe(ordner, kennung, 9)
        val urteil = entscheide(angeboten = 4, geladen = 9, bestaetigt = true)
        assertTrue(urteil is Uebernahme.Angenommen, "mit Bestaetigung muss es gehen")
        assertEquals(4, urteil.fassung)
        // Genau das ist der Punkt: Die Uebernahme hebt die Marke nur, sie senkt
        // sie nie. Nach dem Einspielen der 4 steht weiter die 9.
        Paketmarken.hebe(ordner, kennung, urteil.fassung)
        assertEquals(9, Paketmarken.hoechste(ordner, kennung))
        // Und beim naechsten Mal wird wieder gefragt.
        assertTrue(entscheide(angeboten = 4, geladen = 4) is Uebernahme.Rueckstufung)
    }

    // 5. Die Marke ueberlebt das Loeschen des Pakets. Das ist die Luecke, um
    //    die es ging: Ohne die Datei faellt die App auf die Beigabe zurueck,
    //    und deren Fassung ist typisch 1.
    @Test
    fun dieMarkeUeberlebtDasLoeschenDesPakets() {
        Paketmarken.hebe(ordner, kennung, 9)
        val paket = File(ordner, "uebernommen.czp").apply { writeText("nur eine Attrappe") }
        assertTrue(paket.delete(), "die Attrappe muss sich loeschen lassen")
        // Geladen ist jetzt die Beigabe mit Fassung 1 -- trotzdem darf die 4
        // nicht durchrutschen.
        val urteil = entscheide(angeboten = 4, geladen = 1)
        assertTrue(urteil is Uebernahme.Rueckstufung, "die Marke muss ohne das Paket weitergelten")
        assertEquals(9, urteil.marke)
    }

    // 6. Eine kaputte Unterschrift beruehrt die Marke nicht -- auch dann nicht,
    //    wenn die Fassung im Manifest hoeher ist.
    @Test
    fun kaputteUnterschriftBeruehrtDieMarkeNicht() {
        Paketmarken.hebe(ordner, kennung, 5)
        val vorher = File(ordner, Paketmarken.DATEINAME).readText()
        val urteil = entscheide(angeboten = 99, geladen = 5, urteil = PackVerdict.BadSignature)
        assertTrue(urteil is Uebernahme.Abgelehnt)
        assertEquals(5, Paketmarken.hoechste(ordner, kennung))
        assertEquals(vorher, File(ordner, Paketmarken.DATEINAME).readText())
    }

    // 7. Eine unbekannte Paket-Kennung bekommt keine Marke -- sie wird
    //    abgelehnt, weil nur DASSELBE Paket ausgetauscht wird.
    @Test
    fun unbekannteKennungBekommtKeineMarke() {
        val urteil = entscheide(angeboten = 3, geladen = 1, angeboteneKennung = "org.fremd.paket")
        assertTrue(urteil is Uebernahme.Abgelehnt)
        assertNull(Paketmarken.hoechste(ordner, "org.fremd.paket"))
        // Und fuer ein Paket ohne Marke gilt ersatzweise die geladene Fassung:
        // eine 3 nach einer 1 ist neuer und geht durch.
        assertTrue(entscheide(angeboten = 3, geladen = 1) is Uebernahme.Angenommen)
        Paketmarken.hebe(ordner, kennung, 3)
        assertEquals(3, Paketmarken.hoechste(ordner, kennung))
    }

    // 8. Eine beschaedigte Markendatei stuerzt nicht ab -- und laesst nichts
    //    durch. Sicher schlaegt bequem: Wer die Marke verdirbt, soll damit
    //    nicht den Schutz abschalten koennen.
    @Test
    fun beschaedigteMarkendateiHaeltAnStattDurchzulassen() {
        File(ordner, Paketmarken.DATEINAME).writeText("das ist keine marke\nund das auch nicht\n")
        val stand = Paketmarken.lies(ordner)
        assertTrue(stand is Paketmarken.Stand.Unlesbar, "kaputte Zeilen muessen auffallen")
        val urteil = entscheide(angeboten = 99, geladen = 1)
        assertTrue(urteil is Uebernahme.Abgelehnt, "bei kaputter Marke wird nichts uebernommen")
        assertTrue(urteil.grund.contains("beschädigt"))
        // Die kaputte Datei bleibt liegen, statt ueberschrieben zu werden --
        // wer nachsehen will, soll sie noch vorfinden.
        assertTrue(File(ordner, Paketmarken.DATEINAME).readText().startsWith("das ist keine"))
    }

    // Dazu die Grundregel der Marke selbst.
    @Test
    fun dieMarkeSteigtNurUndKenntMehrerePakete() {
        assertTrue(Paketmarken.hebe(ordner, kennung, 4))
        assertTrue(Paketmarken.hebe(ordner, kennung, 7))
        assertTrue(!Paketmarken.hebe(ordner, kennung, 5), "eine kleinere Zahl darf nichts aendern")
        assertEquals(7, Paketmarken.hoechste(ordner, kennung))
        Paketmarken.hebe(ordner, "org.compasszero.alpen", 2)
        assertEquals(7, Paketmarken.hoechste(ordner, kennung))
        assertEquals(2, Paketmarken.hoechste(ordner, "org.compasszero.alpen"))
    }

    @Test
    fun eineFehlendeMarkendateiIstKeinFehler() {
        val stand = Paketmarken.lies(ordner)
        assertTrue(stand is Paketmarken.Stand.Gelesen)
        assertTrue(stand.marken.isEmpty())
        assertNull(Paketmarken.hoechste(ordner, kennung))
    }
}
