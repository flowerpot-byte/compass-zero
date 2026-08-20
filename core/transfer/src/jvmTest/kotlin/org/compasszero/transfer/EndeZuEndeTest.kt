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
import org.compasszero.security.Digests
import org.compasszero.security.Ed25519
import org.compasszero.security.PackFormat
import org.compasszero.security.PackVerdict
import org.compasszero.security.PackVerifier
import org.compasszero.security.PackWriter
import org.compasszero.security.TrustStore
import org.compasszero.security.TrustedKey

// Beweist die Kernzusage der Uebertragung: Was ankommt, wird genau so geprueft
// wie eine von Hand kopierte Datei. Der Transportweg schafft kein Vertrauen und
// nimmt auch keines weg.
class EndeZuEndeTest {

    private val seed = Ed25519.generateSeed()
    private val publicKey = Ed25519.publicKeyFromSeed(seed)
    private val vertrauen = TrustStore(listOf(TrustedKey("Testschluessel", publicKey)))

    private fun tempDir(): File = File.createTempFile("cztransfer", null).let {
        it.delete(); it.mkdirs(); it
    }

    private fun echtesPaket(dir: File): File {
        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Test","kinds":[]}"""
        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.encodeToByteArray())
            zip.closeEntry()
        }
        val payloadDatei = File(dir, "payload.bin").apply { writeBytes(payload.toByteArray()) }
        val paket = File(dir, "quelle.czp")
        PackWriter.write(payloadDatei, seed, paket)
        return paket
    }

    // Mitten in die Nutzlast, also hinter den Kopf: dort deckt die Signatur
    // jedes Byte ab, und die Stelle bleibt gueltig, wenn das Testpaket waechst.
    private fun kippStelle(bytes: ByteArray): Int = (PackFormat.HEADER_SIZE + bytes.size) / 2

    // Faehrt die Bytes in zufaellig geschnittenen Stuecken durch die
    // Zustandsmaschine, so wie sie ueber Funk ankaemen.
    private fun uebertrage(bytes: ByteArray, angesagteSumme: ByteArray, ziel: File, zufall: Random): Antwort {
        val empfaenger = Empfaenger(DateiSenke(ziel), Sha256Pruefsumme())
        empfaenger.rahmenEmpfangen(Rahmen.Angebot(bytes.size.toLong(), angesagteSumme, "paket.czp"))
        empfaenger.entscheide(true)

        var gesendet = 0
        while (gesendet < bytes.size) {
            val stueck = minOf(zufall.nextInt(1, 5_000), bytes.size - gesendet)
            val antwort = empfaenger.rahmenEmpfangen(Rahmen.Daten(bytes.copyOfRange(gesendet, gesendet + stueck)))
            if (antwort is Antwort.Fehlgeschlagen) return antwort
            gesendet += stueck
        }
        // Abschliessen und Verwerfen erledigt die Zustandsmaschine selbst -- der
        // Aufrufer darf das nicht nachholen muessen.
        return empfaenger.rahmenEmpfangen(Rahmen.Fertig)
    }

    @Test
    fun uebertragenesPaketWirdGenausoBeurteiltWieDieDatei() {
        val dir = tempDir()
        val quelle = echtesPaket(dir)
        val bytes = quelle.readBytes()
        val ziel = File(dir, "empfangen.czp")

        val ende = uebertrage(bytes, Digests.sha256(bytes), ziel, Random(1))
        assertIs<Antwort.Fertig>(ende)
        assertEquals(bytes.size.toLong(), ziel.length())

        val direkt = PackVerifier(vertrauen).open(quelle).verdict
        val uebertragen = PackVerifier(vertrauen).open(ziel).verdict
        assertIs<PackVerdict.Trusted>(direkt)
        assertIs<PackVerdict.Trusted>(uebertragen)
        assertEquals(direkt.signer.name, uebertragen.signer.name)
    }

    // Ein Kippbit unterwegs ist ein Uebertragungsfehler. Er faellt schon im
    // Transport auf, und die halbe Datei bleibt nicht liegen.
    @Test
    fun veraendertesPaketFaelltAlsUebertragungsfehlerAuf() {
        val dir = tempDir()
        val bytes = echtesPaket(dir).readBytes()
        val echteSumme = Digests.sha256(bytes)
        val ziel = File(dir, "empfangen.czp")

        val verdreht = bytes.copyOf()
        verdreht[kippStelle(bytes)]++

        val ende = uebertrage(verdreht, echteSumme, ziel, Random(2))
        val fehler = assertIs<Antwort.Fehlgeschlagen>(ende)
        assertEquals(TransferFehler.PruefsummeFalsch, fehler.fehler)
        assertTrue(!ziel.exists(), "die verworfene Datei liegt noch da")
    }

    // Der ernste Fall: Der Angreifer schickt ein veraendertes Paket UND die dazu
    // passende Pruefsumme. Der Transport merkt nichts — merken muss es die
    // Signaturpruefung. Genau deshalb schafft der Transportweg kein Vertrauen.
    @Test
    fun sauberUebertragenesFaelschungBleibtEineFaelschung() {
        val dir = tempDir()
        val bytes = echtesPaket(dir).readBytes()
        val ziel = File(dir, "empfangen.czp")

        val gefaelscht = bytes.copyOf()
        gefaelscht[kippStelle(bytes)]++

        val ende = uebertrage(gefaelscht, Digests.sha256(gefaelscht), ziel, Random(3))
        assertIs<Antwort.Fertig>(ende)

        val urteil = PackVerifier(vertrauen).open(ziel).verdict
        assertIs<PackVerdict.BadSignature>(urteil)
    }

    @Test
    fun abgeschnittenesPaketWirdNichtAbgelegt() {
        val dir = tempDir()
        val bytes = echtesPaket(dir).readBytes()
        val ziel = File(dir, "empfangen.czp")

        val empfaenger = Empfaenger(DateiSenke(ziel), Sha256Pruefsumme())
        empfaenger.rahmenEmpfangen(Rahmen.Angebot(bytes.size.toLong(), Digests.sha256(bytes), "paket.czp"))
        empfaenger.entscheide(true)
        empfaenger.rahmenEmpfangen(Rahmen.Daten(bytes.copyOfRange(0, bytes.size / 2)))

        val ende = empfaenger.rahmenEmpfangen(Rahmen.Fertig)
        val fehler = assertIs<Antwort.Fehlgeschlagen>(ende)
        assertEquals(TransferFehler.ZuWenigDaten, fehler.fehler)
        assertTrue(!ziel.exists(), "das halbe Paket liegt noch da")
    }
}
