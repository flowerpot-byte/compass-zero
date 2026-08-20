package org.compasszero.packsign

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Faehrt das echte Inhaltspaket aus dem Repo durch die vollstaendige
// Verteilkette: packen, signieren, pruefen. Genau das passiert vor einer
// Veroeffentlichung, und genau das war bis zum 28.07.2026 nie gelaufen -- der
// Parser-Test allein sah die Dateien, nicht das Paket.
//
// Beim ersten Lauf ist sofort aufgefallen, dass sich das Paket gar nicht packen
// liess: QUELLEN.md lag im Paketordner, und dort sind nur manifest.json,
// content/ und assets/ erlaubt. Seitdem liegt der packbare Teil in einem
// eigenen Unterordner.
class EchtesPaketKetteTest {

    private fun repoRoot(): File {
        val fromProperty = System.getProperty("compasszero.repoRoot")
        if (fromProperty != null) return File(fromProperty)
        var dir = File(".").absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("settings.gradle.kts nicht gefunden")
        }
        return dir
    }

    private fun tempDir(): File = File.createTempFile("kette", null).let {
        it.delete(); it.mkdirs(); it
    }

    private fun run(vararg args: String): Pair<Int, String> {
        val out = StringBuilder()
        val code = Commands.run(arrayOf(*args), out)
        return code to out.toString()
    }

    @Test
    fun dasEchteBasispaketLaeuftDurchDieGanzeKette() {
        val quelle = File(repoRoot(), "content/europe-de/paket")
        assertTrue(quelle.isDirectory, "Paketordner fehlt: $quelle")

        val dir = tempDir()
        val schluessel = File(dir, "keys")
        val payload = File(dir, "payload.zip")
        val paket = File(dir, "basispaket.czp")

        val (packCode, packText) = run("pack", "--in", quelle.absolutePath, "--out", payload.absolutePath)
        assertEquals(0, packCode, "das echte Paket laesst sich nicht packen: $packText")

        assertEquals(0, run("keygen", "--dir", schluessel.absolutePath, "--name", "probe").first)
        val (signCode, signText) = run(
            "sign",
            "--key", File(schluessel, "probe.secret").absolutePath,
            "--in", payload.absolutePath,
            "--out", paket.absolutePath,
        )
        assertEquals(0, signCode, "das echte Paket laesst sich nicht signieren: $signText")
        assertTrue(paket.isFile)

        val vertraut = File(dir, "vertraut.txt")
        vertraut.writeText("probe=" + File(schluessel, "probe.public").readText().trim() + "\n")
        val (verifyCode, verifyText) = run("verify", "--in", paket.absolutePath, "--keys", vertraut.absolutePath)
        assertEquals(0, verifyCode, "das eigene Paket verifiziert nicht: $verifyText")
        assertTrue(verifyText.contains("Signatur GUELTIG"), verifyText)
        // Der Inhalt muss vollstaendig drin sein, nicht nur das Manifest.
        assertTrue(verifyText.contains("content/tips.json"), verifyText)
        assertTrue(verifyText.contains("manifest.json"), verifyText)
    }

    // Das Paket ist deterministisch: derselbe Ordner ergibt Byte-gleiche
    // Pakete. Ohne diese Zusage koennte niemand nachbauen, was er installiert hat.
    @Test
    fun zweimalPackenErgibtDieselbenBytes() {
        val quelle = File(repoRoot(), "content/europe-de/paket")
        val dir = tempDir()
        val erste = File(dir, "eins.zip")
        val zweite = File(dir, "zwei.zip")
        assertEquals(0, run("pack", "--in", quelle.absolutePath, "--out", erste.absolutePath).first)
        assertEquals(0, run("pack", "--in", quelle.absolutePath, "--out", zweite.absolutePath).first)
        assertTrue(erste.readBytes().contentEquals(zweite.readBytes()), "zwei Laeufe ergeben verschiedene Pakete")
    }

    // Die Quellendokumentation gehoert neben das Paket, nicht hinein -- sonst
    // laesst es sich nicht packen. Der Test haelt beides fest: dass die Datei da
    // ist, und dass sie ausserhalb des packbaren Teils liegt.
    @Test
    fun dieQuellendokumentationLiegtNebenDemPaketNichtDarin() {
        val ordner = File(repoRoot(), "content/europe-de")
        assertTrue(File(ordner, "QUELLEN.md").isFile, "die Quellendokumentation fehlt")
        assertTrue(!File(ordner, "paket/QUELLEN.md").exists(), "QUELLEN.md liegt im packbaren Teil")
    }
}
