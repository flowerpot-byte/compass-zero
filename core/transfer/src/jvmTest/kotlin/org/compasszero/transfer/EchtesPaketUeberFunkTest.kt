package org.compasszero.transfer

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.compasszero.content.PackReader
import org.compasszero.security.Digests
import org.compasszero.security.Ed25519
import org.compasszero.security.PackVerdict
import org.compasszero.security.PackWriter
import org.compasszero.security.TrustStore
import org.compasszero.security.TrustedKey

// Das ganze Versprechen der App in einem Test: die echten Inhalte aus dem Repo,
// signiert, ueber beide Zustandsmaschinen uebertragen, geprueft, geladen -- und
// am anderen Ende muss der Erste-Hilfe-Tipp lesbar herauskommen.
//
// Die vorhandenen Tests decken jeweils ein Stueck ab: der Parser die Dateien,
// der Kettentest das Packen und Signieren, der Ende-zu-Ende-Test den Empfang mit
// erfundenem Inhalt. Was nie zusammen gelaufen ist, ist nicht geprueft.
class EchtesPaketUeberFunkTest {

    private val seed = Ed25519.generateSeed()
    private val publicKey = Ed25519.publicKeyFromSeed(seed)
    private val vertrauen = TrustStore(listOf(TrustedKey("Maintainer", publicKey)))

    private fun repoRoot(): File {
        val fromProperty = System.getProperty("compasszero.repoRoot")
        if (fromProperty != null) return File(fromProperty)
        var dir = File(".").absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("settings.gradle.kts nicht gefunden")
        }
        return dir
    }

    private fun tempDir(): File = File.createTempFile("funk", null).let {
        it.delete(); it.mkdirs(); it
    }

    // Baut aus dem echten Paketordner ein signiertes .czp.
    private fun echtesPaket(dir: File): File {
        val quelle = File(repoRoot(), "content/europe-de/paket")
        assertTrue(quelle.isDirectory, "Paketordner fehlt: $quelle")
        val eintraege = quelle.walkTopDown().filter { it.isFile }
            .map { quelle.toPath().relativize(it.toPath()).toString().replace('\\', '/') to it.readBytes() }
            .sortedBy { it.first }
            .toList()
        assertTrue(eintraege.size >= 2, "erwartet waren Manifest und Inhalt, gefunden: ${eintraege.map { it.first }}")

        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            for ((name, bytes) in eintraege) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        val payloadDatei = File(dir, "payload.zip").apply { writeBytes(payload.toByteArray()) }
        val paket = File(dir, "basispaket.czp")
        PackWriter.write(payloadDatei, seed, paket)
        return paket
    }

    @Test
    fun dasEchteBasispaketUeberstehtDieUebertragungUndBleibtLesbar() {
        val dir = tempDir()
        val quelle = echtesPaket(dir)
        val bytes = quelle.readBytes()
        val ziel = File(dir, "empfangen.czp")
        val zufall = Random(4)

        // Sender und Empfaenger laufen gegeneinander, wie ueber Funk.
        val senke = DateiSenke(ziel)
        val empfaenger = Empfaenger(senke, Sha256Pruefsumme())
        val sender = Sender(
            DateiQuelle(quelle),
            Rahmen.Angebot(bytes.size.toLong(), Digests.sha256(bytes), "Basispaket Deutsch"),
            puffergroesse = zufall.nextInt(500, 4_000),
        )

        val angebot = (sender.beginne() as SendeAntwort.Senden).rahmen
        assertIs<Antwort.Frage>(empfaenger.rahmenEmpfangen(angebot))
        assertIs<Rahmen.Annahme>((empfaenger.entscheide(true) as Antwort.Senden).rahmen)

        var antwort = sender.rahmenEmpfangen(Rahmen.Annahme)
        var rahmenZahl = 0
        while (true) {
            val rahmen = (antwort as SendeAntwort.Senden).rahmen
            val beimEmpfaenger = empfaenger.rahmenEmpfangen(rahmen)
            if (rahmen is Rahmen.Fertig) {
                val fertig = assertIs<Antwort.Fertig>(beimEmpfaenger)
                assertEquals(bytes.size.toLong(), fertig.bytes)
                break
            }
            assertEquals(Antwort.Nichts, beimEmpfaenger)
            rahmenZahl++
            antwort = sender.naechstes()
        }
        assertTrue(rahmenZahl >= 3, "erwartet waren mehrere Datenrahmen, gezaehlt: $rahmenZahl")
        assertTrue(ziel.readBytes().contentEquals(bytes), "die uebertragenen Bytes weichen ab")

        // Und jetzt der Punkt: Das Empfangene muss sich pruefen UND lesen lassen.
        val ergebnis = PackReader.read(ziel, vertrauen)
        assertIs<PackVerdict.Trusted>(ergebnis.verdict)
        val geladen = ergebnis.result?.pack
        assertTrue(geladen != null, "das uebertragene Paket laedt nicht: ${ergebnis.result?.problems?.take(3)}")
        assertEquals("org.compasszero.base.de", geladen.manifest.id)

        // Stichprobe am Inhalt, der im Ernstfall zaehlt.
        val druck = geladen.tips.firstOrNull { it.id == "erste-hilfe-herzdruckmassage" }
        assertTrue(druck != null, "der Tipp zur Herzdruckmassage fehlt nach der Uebertragung")
        assertTrue(
            druck.body.contains("mindestens fünf und höchstens sechs Zentimeter"),
            "die Drucktiefe hat die Uebertragung nicht ueberstanden",
        )
        assertTrue(geladen.tips.size >= 56, "es fehlen Tipps: ${geladen.tips.size}")
    }

    // Ein Kippbit unterwegs muss auch beim echten Paket als Uebertragungsfehler
    // auffallen -- und nichts darf liegen bleiben.
    @Test
    fun einKippbitImEchtenPaketFaelltAuf() {
        val dir = tempDir()
        val bytes = echtesPaket(dir).readBytes()
        val ziel = File(dir, "empfangen.czp")

        val verdreht = bytes.copyOf()
        val stelle = bytes.size / 2
        verdreht[stelle]++

        val empfaenger = Empfaenger(DateiSenke(ziel), Sha256Pruefsumme())
        empfaenger.rahmenEmpfangen(Rahmen.Angebot(bytes.size.toLong(), Digests.sha256(bytes), "paket.czp"))
        empfaenger.entscheide(true)
        var gesendet = 0
        var letzte: Antwort = Antwort.Nichts
        while (gesendet < verdreht.size) {
            val stueck = minOf(8_000, verdreht.size - gesendet)
            letzte = empfaenger.rahmenEmpfangen(Rahmen.Daten(verdreht.copyOfRange(gesendet, gesendet + stueck)))
            gesendet += stueck
        }
        if (letzte !is Antwort.Fehlgeschlagen) letzte = empfaenger.rahmenEmpfangen(Rahmen.Fertig)

        val fehler = assertIs<Antwort.Fehlgeschlagen>(letzte)
        assertEquals(TransferFehler.PruefsummeFalsch, fehler.fehler)
        assertTrue(!ziel.exists(), "das verdorbene Paket blieb liegen")
    }
}

// Liest eine Datei stueckweise, wie es die Sendeseite auf dem Geraet taete.
private class DateiQuelle(datei: File) : Datenquelle {
    private val strom = datei.inputStream().buffered()
    override fun lies(puffer: ByteArray, offset: Int, laenge: Int): Int {
        val gelesen = strom.read(puffer, offset, laenge)
        return if (gelesen < 0) 0 else gelesen
    }
}
