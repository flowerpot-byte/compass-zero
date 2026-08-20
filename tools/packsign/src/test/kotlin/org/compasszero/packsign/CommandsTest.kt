package org.compasszero.packsign

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandsTest {

    private fun tempDir(): File = File.createTempFile("cmd", null).let { it.delete(); it.mkdirs(); it }

    private fun run(vararg args: String): Pair<Int, String> {
        val out = StringBuilder()
        val code = Commands.run(arrayOf(*args), out)
        return code to out.toString()
    }

    private fun contentDir(): File = tempDir().also { dir ->
        File(dir, "manifest.json").writeBytes(
            """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["tips"]}""".encodeToByteArray()
        )
        File(dir, "content").mkdirs()
        File(dir, "content/tips.json").writeBytes(
            """{"schema":1,"tips":[{"id":"tipp-eins","title":"Titel","category":"wasser","body":"Text.","sources":[{"name":"Quelle","detail":"Abschnitt 1"}]}]}""".encodeToByteArray()
        )
    }


    // Ein echtes, vollstaendiges PNG. Ein blosser Dateikopf genuegt nicht mehr:
    // das Werkzeug rechnet die Abschnittskette bis zum Endabschnitt nach.
    private fun echtesPng(): ByteArray {
        val bild = java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val aus = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.write(bild, "png", aus)
        return aus.toByteArray()
    }

    // Genau das Muster, das die Pruefung frueher durchgelassen hat: ein
    // einwandfreies Bild, hinter dessen Ende Fremddaten kleben.
    @Test
    fun signVerweigertBytesHinterDemBildende() {
        val dir = tempDir()
        val schluessel = File(dir, "keys")
        assertEquals(0, run("keygen", "--dir", schluessel.path, "--name", "m").first)

        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["guides"]}"""
        val guides = """{"schema":1,"guides":[{"id":"g1","title":"Titel","category":"wasser","summary":"Kurzfassung.",""" +
            """"materials":[{"item":"Sand"}],"steps":[{"text":"Schritt.","image":"assets/bild.png"}],"difficulty":1,""" +
            """"sources":[{"name":"Quelle","detail":"Abschnitt 1"}]}]}"""
        val payload = File(dir, "payload.zip")
        schreibeZip(
            payload,
            listOf(
                "manifest.json" to manifest.encodeToByteArray(),
                "content/guides.json" to guides.encodeToByteArray(),
                "assets/bild.png" to (echtesPng() + ByteArray(200_000) { 0x42 }),
            ),
        )

        val paket = File(dir, "paket.czp")
        val (code, ausgabe) = run(
            "sign", "--key", File(schluessel, "m.secret").path,
            "--in", payload.path, "--out", paket.path,
        )
        assertTrue(code != 0, "Fremddaten hinter dem Bildende wurden unterschrieben: $ausgabe")
        assertTrue(ausgabe.contains("hinter dem Bildende"), "die Beanstandung benennt die Ursache nicht: $ausgabe")
    }

    // Der Weg, den ein Zulieferer nehmen wuerde: fertige payload.zip statt
    // Inhaltsordner. Genau hier kam die Bildpruefung frueher nicht mehr vor.
    @Test
    fun signVerweigertFremddatenUnterAssets() {
        val dir = tempDir()
        val schluessel = File(dir, "keys")
        assertEquals(0, run("keygen", "--dir", schluessel.path, "--name", "m").first)

        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["guides"]}"""
        val guides = """{"schema":1,"guides":[{"id":"g1","title":"Titel","category":"wasser","summary":"Kurzfassung.",""" +
            """"materials":[{"item":"Sand"}],"steps":[{"text":"Schritt.","image":"assets/bild.png"}],"difficulty":1,""" +
            """"sources":[{"name":"Quelle","detail":"Abschnitt 1"}]}]}"""
        val payload = File(dir, "payload.zip")
        schreibeZip(
            payload,
            listOf(
                "manifest.json" to manifest.encodeToByteArray(),
                "content/guides.json" to guides.encodeToByteArray(),
                // Kein Bild, sondern beliebige Fremddaten.
                "assets/bild.png" to ByteArray(4096) { 0x41 },
            ),
        )

        val paket = File(dir, "paket.czp")
        val (code, ausgabe) = run(
            "sign", "--key", File(schluessel, "m.secret").path,
            "--in", payload.path, "--out", paket.path,
        )
        assertTrue(code != 0, "Fremddaten unter assets/ wurden unterschrieben, Ausgabe: $ausgabe")
        assertTrue(ausgabe.contains("bild.png"), "die Beanstandung nennt die Datei nicht: $ausgabe")
        assertTrue(!paket.exists(), "das abgelehnte Paket liegt trotzdem da")
    }

    // Ein echtes Bild auf demselben Weg muss durchgehen -- sonst prueft der Test
    // oben nur, dass sign ueberhaupt etwas ablehnt.
    @Test
    fun signNimmtEchteBilderAufDemselbenWeg() {
        val dir = tempDir()
        val schluessel = File(dir, "keys")
        assertEquals(0, run("keygen", "--dir", schluessel.path, "--name", "m").first)

        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["guides"]}"""
        val guides = """{"schema":1,"guides":[{"id":"g1","title":"Titel","category":"wasser","summary":"Kurzfassung.",""" +
            """"materials":[{"item":"Sand"}],"steps":[{"text":"Schritt.","image":"assets/bild.png"}],"difficulty":1,""" +
            """"sources":[{"name":"Quelle","detail":"Abschnitt 1"}]}]}"""
        val payload = File(dir, "payload.zip")
        schreibeZip(
            payload,
            listOf(
                "manifest.json" to manifest.encodeToByteArray(),
                "content/guides.json" to guides.encodeToByteArray(),
                "assets/bild.png" to echtesPng(),
            ),
        )

        val paket = File(dir, "paket.czp")
        val (code, ausgabe) = run(
            "sign", "--key", File(schluessel, "m.secret").path,
            "--in", payload.path, "--out", paket.path,
        )
        assertEquals(0, code, "ein echtes Bild wurde abgelehnt: $ausgabe")
        assertTrue(paket.isFile)
    }

    // Wie ein uebliches Packprogramm: Groessen stehen im Kopf des Eintrags, nicht
    // in einem Nachspann dahinter.
    private fun schreibeZip(ziel: File, eintraege: List<Pair<String, ByteArray>>) {
        // Genau wie der eigene Packer: gepresst, Groessen im Kopf, fester
        // Zeitstempel. Nur so pruefen die Tests den Angriff und nicht den Aufbau.
        java.util.zip.ZipOutputStream(ziel.outputStream()).use { zip ->
            zip.setLevel(java.util.zip.Deflater.BEST_COMPRESSION)
            for ((name, inhalt) in eintraege.sortedBy { it.first }) {
                val eintrag = java.util.zip.ZipEntry(name)
                eintrag.setTimeLocal(java.time.LocalDateTime.of(2000, 1, 1, 0, 0))
                eintrag.size = inhalt.size.toLong()
                eintrag.crc = java.util.zip.CRC32().apply { update(inhalt) }.value
                eintrag.compressedSize = gepresst(inhalt)
                zip.putNextEntry(eintrag)
                zip.write(inhalt)
                zip.closeEntry()
            }
        }
    }

    private fun gepresst(inhalt: ByteArray): Long {
        val presse = java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION, true)
        try {
            presse.setInput(inhalt)
            presse.finish()
            val puffer = ByteArray(64 * 1024)
            var gesamt = 0L
            while (!presse.finished()) gesamt += presse.deflate(puffer)
            return gesamt
        } finally {
            presse.end()
        }
    }

    @Test
    fun unknownCommandPrintsUsage() {
        val (code, text) = run("unsinn")
        assertEquals(2, code)
        assertTrue("keygen" in text && "verify" in text)
    }

    @Test
    fun keygenWritesKeyPairAndRefusesOverwrite() {
        val dir = tempDir()
        val (code, text) = run("keygen", "--dir", dir.absolutePath, "--name", "testschluessel")
        assertEquals(0, code)
        assertTrue(File(dir, "testschluessel.secret").isFile)
        assertTrue(File(dir, "testschluessel.public").isFile)
        assertTrue("niemals" in text.lowercase())

        val (again, _) = run("keygen", "--dir", dir.absolutePath, "--name", "testschluessel")
        assertEquals(2, again)
    }

    @Test
    fun fullFlowKeygenPackSignVerify() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val payload = File(tempDir(), "payload.zip")
        val (packCode, _) = run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)
        assertEquals(0, packCode)

        val pack = File(tempDir(), "test.czp")
        val (signCode, _) = run(
            "sign", "--key", File(keys, "m.secret").absolutePath,
            "--in", payload.absolutePath, "--out", pack.absolutePath,
        )
        assertEquals(0, signCode)

        val keyList = File(keys, "vertraut.txt")
        keyList.writeText("Maintainer=" + File(keys, "m.public").readText().trim() + "\n")
        val (verifyCode, verifyText) = run("verify", "--in", pack.absolutePath, "--keys", keyList.absolutePath)
        assertEquals(0, verifyCode)
        assertTrue("Maintainer" in verifyText)
        assertTrue("manifest.json" in verifyText)
        assertTrue("SHA-256" in verifyText || "sha256" in verifyText.lowercase())
    }

    @Test
    fun verifyFailsOnTamperAndOnUnknownKey() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val payload = File(tempDir(), "payload.zip")
        run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)
        val pack = File(tempDir(), "test.czp")
        run("sign", "--key", File(keys, "m.secret").absolutePath, "--in", payload.absolutePath, "--out", pack.absolutePath)

        val (unknownCode, unknownText) = run("verify", "--in", pack.absolutePath)
        assertEquals(1, unknownCode)
        assertTrue("unbekannt" in unknownText.lowercase())

        val bytes = pack.readBytes()
        bytes[bytes.size - 30] = (bytes[bytes.size - 30].toInt() xor 1).toByte()
        pack.writeBytes(bytes)
        val keyList = File(keys, "vertraut.txt")
        keyList.writeText("Maintainer=" + File(keys, "m.public").readText().trim() + "\n")
        val (tamperCode, tamperText) = run("verify", "--in", pack.absolutePath, "--keys", keyList.absolutePath)
        assertEquals(1, tamperCode)
        assertTrue("manipuliert" in tamperText.lowercase() || "ungueltig" in tamperText.lowercase())
    }

    @Test
    fun packRefusesContentThatWouldFailInTheApp() {
        val dir = tempDir().also {
            File(it, "manifest.json").writeBytes("kein json".encodeToByteArray())
        }
        val (code, text) = run("pack", "--in", dir.absolutePath, "--out", File(tempDir(), "x.zip").absolutePath)
        assertEquals(2, code)
        assertTrue("manifest" in text.lowercase())
    }

    @Test
    fun signRefusesPayloadThatTheAppWouldReject() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val key = File(keys, "m.secret").absolutePath

        // Ein von Hand gebauter Payload umgeht das Gate in pack. Genau so entstuende
        // sonst ein signiertes Paket mit fehlender Quelle.
        val payload = File(tempDir(), "handgebaut.zip")
        schreibeZip(payload, listOf(
            "manifest.json" to """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["tips"]}""".encodeToByteArray(),
            "content/tips.json" to """{"schema":1,"tips":[{"id":"beere","title":"Beeren","category":"nahrung","body":"Alle roten Beeren sind essbar.","sources":[]}]}""".encodeToByteArray(),
        ))
        val target = File(tempDir(), "boese.czp")
        val (code, text) = run("sign", "--key", key, "--in", payload.absolutePath, "--out", target.absolutePath)
        assertEquals(2, code)
        assertTrue("sources-missing" in text || "quelle" in text.lowercase())
        assertTrue(!target.exists(), "kein Paket bei abgelehntem Inhalt")
    }

    @Test
    fun signRefusesGarbagePayload() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val payload = File(tempDir(), "muell.zip").apply { writeBytes(ByteArray(200) { (it * 5).toByte() }) }
        val target = File(tempDir(), "muell.czp")
        val (code, _) = run(
            "sign", "--key", File(keys, "m.secret").absolutePath,
            "--in", payload.absolutePath, "--out", target.absolutePath,
        )
        assertEquals(2, code)
        assertTrue(!target.exists())
    }

    @Test
    fun signRefusesPublicKeyFileAndShowsFingerprint() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val payload = File(tempDir(), "payload.zip")
        run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)

        val (code, text) = run(
            "sign", "--key", File(keys, "m.public").absolutePath,
            "--in", payload.absolutePath, "--out", File(tempDir(), "falsch.czp").absolutePath,
        )
        assertEquals(2, code)
        assertTrue("public" in text.lowercase())

        val (okCode, okText) = run(
            "sign", "--key", File(keys, "m.secret").absolutePath,
            "--in", payload.absolutePath, "--out", File(tempDir(), "gut.czp").absolutePath,
        )
        assertEquals(0, okCode)
        assertTrue("Fingerabdruck" in okText, "Signierer muss den verwendeten Schluessel sehen")
    }

    @Test
    fun missingOutputDirectoryIsReportedNotThrown() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val payload = File(tempDir(), "payload.zip")
        run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)

        val (packCode, packText) = run(
            "pack", "--in", contentDir().absolutePath,
            "--out", File(tempDir(), "gibtsnicht/tiefer/p.zip").absolutePath,
        )
        assertEquals(2, packCode)
        assertTrue(packText.isNotBlank())

        val (signCode, signText) = run(
            "sign", "--key", File(keys, "m.secret").absolutePath,
            "--in", payload.absolutePath, "--out", File(tempDir(), "fehlt/x.czp").absolutePath,
        )
        assertEquals(2, signCode)
        assertTrue(signText.isNotBlank())
    }

    @Test
    fun unknownOptionsAreRejected() {
        val (code, text) = run("verify", "--kesy", "liste.txt", "--in", "x.czp")
        assertEquals(2, code)
        assertTrue("kesy" in text)
    }

    @Test
    fun repeatedOptionIsRejected() {
        assertEquals(2, run("pack", "--in", "a", "--in", "b", "--out", "c").first)
    }

    @Test
    fun keygenRejectsBadNames() {
        val dir = tempDir()
        assertEquals(2, run("keygen", "--dir", dir.absolutePath, "--name", "Gross").first)
        assertEquals(2, run("keygen", "--dir", dir.absolutePath, "--name", "../flucht").first)
    }

    @Test
    fun brokenKeyFileIsReported() {
        val keys = tempDir()
        val payload = File(tempDir(), "payload.zip")
        run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)
        val bad = File(keys, "kurz.secret").apply { writeText("aabb\n") }
        val (code, text) = run(
            "sign", "--key", bad.absolutePath,
            "--in", payload.absolutePath, "--out", File(tempDir(), "x.czp").absolutePath,
        )
        assertEquals(2, code)
        assertTrue(text.isNotBlank())
    }

    @Test
    fun missingOptionsAreUsageErrors() {
        assertEquals(2, run("keygen", "--dir").first)
        assertEquals(2, run("sign", "--key", "fehlt.secret").first)
        assertEquals(2, run("verify").first)
        assertEquals(2, run().first)
    }

    @Test
    fun duplicateKeyInTrustListIsReported() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val hex = File(keys, "m.public").readText().trim()
        val list = File(keys, "doppelt.txt").apply { writeText("Eins=$hex\nZwei=$hex\n") }
        val (code, text) = run("verify", "--in", File(keys, "egal.czp").absolutePath, "--keys", list.absolutePath)
        assertEquals(2, code)
        assertTrue("doppelt" in text.lowercase() || "duplicate" in text.lowercase())
    }

    @Test
    fun brokenKeyListIsReported() {
        val dir = tempDir()
        val list = File(dir, "liste.txt").apply { writeText("kaputte zeile ohne gleichheitszeichen\n") }
        val (code, text) = run("verify", "--in", File(dir, "egal.czp").absolutePath, "--keys", list.absolutePath)
        assertEquals(2, code)
        assertTrue("zeile" in text.lowercase())
    }

    @Test
    fun signLehntNichtAngemeldeteInhaltsdateienAb() {
        // Schmuggelpfad: eine Datei, die im Manifest nicht steht, wird von der App
        // nie geparst — wuerde also ungeprueft die Unterschrift des Maintainers tragen.
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val dir = contentDir().also {
            File(it, "content/guides.json").writeBytes(
                """{"schema":1,"guides":[{"id":"g","title":"Anleitung","category":"bau","summary":"Kurz.","materials":[{"item":"Holz"}],"steps":[{"text":"Schritt eins."}],"difficulty":1,"sources":[]}]}""".encodeToByteArray()
            )
        }
        val payload = File(tempDir(), "payload.zip")
        val (packCode, packText) = run("pack", "--in", dir.absolutePath, "--out", payload.absolutePath)
        assertEquals(2, packCode, "pack muss die nicht angemeldete Datei melden: " + packText)

        // Auch wenn jemand pack ueberspringt und den Payload selbst baut:
        val handPayload = File(tempDir(), "hand.zip")
        schreibeZip(handPayload, listOf("manifest.json", "content/tips.json", "content/guides.json").map {
            it to File(dir, it).readBytes()
        })
        val target = File(tempDir(), "schmuggel.czp")
        val (signCode, signText) = run(
            "sign", "--key", File(keys, "m.secret").absolutePath,
            "--in", handPayload.absolutePath, "--out", target.absolutePath,
        )
        assertEquals(2, signCode)
        assertTrue("guides" in signText)
        assertTrue(!target.exists(), "kein Paket bei ungeprueftem Inhalt")
    }

    @Test
    fun signVerweigertGleicheQuelleUndZiel() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val payload = File(tempDir(), "payload.zip")
        run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)
        val vorher = payload.readBytes()
        val (code, _) = run(
            "sign", "--key", File(keys, "m.secret").absolutePath,
            "--in", payload.absolutePath, "--out", payload.absolutePath,
        )
        assertEquals(2, code)
        assertTrue(payload.readBytes().contentEquals(vorher), "Quelldatei wurde zerstoert")
    }

    @Test
    fun keineZwischendateiBleibtLiegen() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val ziel = tempDir()
        val payload = File(tempDir(), "hand.zip")
        schreibeZip(payload, listOf("manifest.json" to "kein json".encodeToByteArray()))
        val target = File(ziel, "x.czp")
        assertEquals(2, run("sign", "--key", File(keys, "m.secret").absolutePath, "--in", payload.absolutePath, "--out", target.absolutePath).first)
        assertEquals(emptyList(), ziel.listFiles()!!.map { it.name })
    }

    @Test
    fun vieleHinweiseDuerfenDenSchmuggelpfadNichtOeffnen() {
        // Ueberlaeuft die Meldungsliste, verschwindet sonst der Hinweis auf die
        // nicht angemeldete Datei — und das Werkzeug signiert sie blind mit.
        val dir = tempDir()
        val pois = (1..600).joinToString(",") {
            """{"id":"poi-$it","kind":"zukunftsart","lat":47.5,"lon":12.5}"""
        }
        File(dir, "manifest.json").writeBytes(
            """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["pois"]}""".encodeToByteArray()
        )
        File(dir, "content").mkdirs()
        File(dir, "content/pois.json").writeBytes(
            ("""{"schema":1,"attribution":"Beispieldaten","pois":[""" + pois + "]}").encodeToByteArray()
        )
        File(dir, "content/tips.json").writeBytes(
            """{"schema":1,"tips":[{"id":"beere","title":"Beeren","category":"nahrung","body":"Rohes Wasser ist unbedenklich.","sources":[]}]}""".encodeToByteArray()
        )

        val (code, text) = run("pack", "--in", dir.absolutePath, "--out", File(tempDir(), "p.zip").absolutePath)
        assertEquals(2, code, "muss ablehnen, Ausgabe war: " + text)
    }

    @Test
    fun unbenutzteBilderWerdenNichtMitsigniert() {
        val dir = contentDir().also {
            File(it, "assets").mkdirs()
            File(it, "assets/nutzlast.png").writeBytes(byteArrayOf(0x4D, 0x5A, 0x90.toByte(), 0x00))
        }
        val (code, text) = run("pack", "--in", dir.absolutePath, "--out", File(tempDir(), "p.zip").absolutePath)
        assertEquals(2, code)
        assertTrue("nutzlast" in text)
    }

    @Test
    fun signUeberschreibtKeinVorhandenesPaket() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val payload = File(tempDir(), "payload.zip")
        run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)
        val target = File(tempDir(), "fertig.czp")
        assertEquals(0, run("sign", "--key", File(keys, "m.secret").absolutePath, "--in", payload.absolutePath, "--out", target.absolutePath).first)
        val vorher = target.readBytes()

        val (code, text) = run("sign", "--key", File(keys, "m.secret").absolutePath, "--in", payload.absolutePath, "--out", target.absolutePath)
        assertEquals(2, code)
        assertTrue("existiert" in text)
        assertTrue(target.readBytes().contentEquals(vorher))
    }

    @Test
    fun packMeldetZuGrosseInhaltsdatei() {
        val dir = contentDir()
        File(dir, "content/riesig.json").writeBytes(ByteArray(5 * 1024 * 1024) { 0x20 })
        val (code, text) = run("pack", "--in", dir.absolutePath, "--out", File(tempDir(), "p.zip").absolutePath)
        assertEquals(2, code)
        assertTrue("riesig" in text)
    }

    @Test
    fun signLehntBytesAbDieZuKeinemEintragGehoeren() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val key = File(keys, "m.secret").absolutePath

        // Fall 1: Zusatzfeld im lokalen ZIP-Kopf
        val mitZusatz = File(tempDir(), "zusatz.zip")
        val inhalt = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["tips"]}""".encodeToByteArray()
        java.util.zip.ZipOutputStream(mitZusatz.outputStream()).use { zip ->
            zip.setLevel(java.util.zip.Deflater.BEST_COMPRESSION)
            val eintrag = java.util.zip.ZipEntry("manifest.json")
            eintrag.setTimeLocal(java.time.LocalDateTime.of(2000, 1, 1, 0, 0))
            eintrag.size = inhalt.size.toLong()
            eintrag.crc = java.util.zip.CRC32().apply { update(inhalt) }.value
            eintrag.compressedSize = gepresst(inhalt)
            eintrag.extra = ByteArray(4096) { 0x41 }
            zip.putNextEntry(eintrag)
            zip.write(inhalt)
            zip.closeEntry()
        }
        val ziel1 = File(tempDir(), "zusatz.czp")
        val (code1, text1) = run("sign", "--key", key, "--in", mitZusatz.absolutePath, "--out", ziel1.absolutePath)
        assertEquals(2, code1)
        assertTrue("Zusatzfeld" in text1, text1)
        assertTrue(!ziel1.exists())

        // Fall 2: Muell hinter dem ZIP-Verzeichnis
        val payload = File(tempDir(), "payload.zip")
        run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)
        val mitAnhang = File(tempDir(), "anhang.zip")
        mitAnhang.writeBytes(payload.readBytes() + ByteArray(50_000) { 0x42 })
        val ziel2 = File(tempDir(), "anhang.czp")
        val (code2, text2) = run("sign", "--key", key, "--in", mitAnhang.absolutePath, "--out", ziel2.absolutePath)
        assertEquals(2, code2)
        assertTrue("zusaetzliche Bytes" in text2, text2)
        assertTrue(!ziel2.exists())

        // Gegenprobe: ein sauber gepacktes Paket geht durch.
        val ziel3 = File(tempDir(), "sauber.czp")
        assertEquals(0, run("sign", "--key", key, "--in", payload.absolutePath, "--out", ziel3.absolutePath).first)
    }

    @Test
    fun packUeberschreibtKeineVorhandeneDatei() {
        val vorhanden = File(tempDir(), "wichtig.zip").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val (code, text) = run("pack", "--in", contentDir().absolutePath, "--out", vorhanden.absolutePath)
        assertEquals(2, code)
        assertTrue("existiert" in text)
        assertEquals(3, vorhanden.length().toInt())
    }

    @Test
    fun signLehntWiderspruechlichesZipVerzeichnisAb() {
        // Der gefaehrlichste Fall: jedes Pruefwerkzeug liest das Verzeichnis, die
        // App liest die lokalen Koepfe. Laufen beide auseinander, unterschreibt der
        // Maintainer einen Inhalt, den er nie gesehen hat.
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val payload = File(tempDir(), "payload.zip")
        run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)

        val bytes = payload.readBytes()

        // Fall 1: Verzeichnis meldet eine falsche Eintragszahl.
        val falscheAnzahl = bytes.copyOf()
        falscheAnzahl[falscheAnzahl.size - 22 + 10] = 0
        falscheAnzahl[falscheAnzahl.size - 22 + 11] = 0
        val datei1 = File(tempDir(), "anzahl.zip").apply { writeBytes(falscheAnzahl) }
        val (code1, text1) = run("sign", "--key", File(keys, "m.secret").absolutePath, "--in", datei1.absolutePath, "--out", File(tempDir(), "a.czp").absolutePath)
        assertEquals(2, code1)
        assertTrue("Eintrag" in text1 || "Eintraege" in text1, text1)

        // Fall 2: Verzeichnis zeigt fuer einen Eintrag an eine falsche Stelle.
        val falscherVersatz = bytes.copyOf()
        val verzeichnisStart = lies32(falscherVersatz, falscherVersatz.size - 22 + 16)
        falscherVersatz[verzeichnisStart + 42] = 99
        val datei2 = File(tempDir(), "versatz.zip").apply { writeBytes(falscherVersatz) }
        val (code2, text2) = run("sign", "--key", File(keys, "m.secret").absolutePath, "--in", datei2.absolutePath, "--out", File(tempDir(), "b.czp").absolutePath)
        assertEquals(2, code2)

        // Gegenprobe: das unveraenderte Paket geht durch.
        assertEquals(0, run("sign", "--key", File(keys, "m.secret").absolutePath, "--in", payload.absolutePath, "--out", File(tempDir(), "gut.czp").absolutePath).first)
    }

    @Test
    fun signStuerztBeiErfundenenGroessenNichtAb() {
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val payload = File(tempDir(), "payload.zip")
        run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)
        val bytes = payload.readBytes()
        // Riesige komprimierte Groesse im ersten lokalen Kopf.
        for (i in 0 until 4) bytes[18 + i] = if (i == 3) 0x7F else -1
        val datei = File(tempDir(), "riesig.zip").apply { writeBytes(bytes) }
        val (code, text) = run("sign", "--key", File(keys, "m.secret").absolutePath, "--in", datei.absolutePath, "--out", File(tempDir(), "r.czp").absolutePath)
        assertEquals(2, code)
        assertTrue(text.isNotBlank())
    }

    private fun lies32(bytes: ByteArray, stelle: Int): Int =
        (bytes[stelle].toInt() and 0xFF) or
            ((bytes[stelle + 1].toInt() and 0xFF) shl 8) or
            ((bytes[stelle + 2].toInt() and 0xFF) shl 16) or
            ((bytes[stelle + 3].toInt() and 0xFF) shl 24)

    @Test
    fun signLehntWidersprechendePackangabenAb() {
        // Der Prueffall aus dem Bericht: lokaler Kopf und Verzeichnis nennen
        // verschiedene Packverfahren. Jedes Pruefwerkzeug liest das Verzeichnis,
        // die App liest den lokalen Kopf — beide sehen andere Bytes.
        val keys = tempDir()
        run("keygen", "--dir", keys.absolutePath, "--name", "m")
        val payload = File(tempDir(), "payload.zip")
        run("pack", "--in", contentDir().absolutePath, "--out", payload.absolutePath)
        val original = payload.readBytes()
        val verzeichnisStart = lies32(original, original.size - 22 + 16)

        for ((versatz, was) in listOf(10 to "Packverfahren", 16 to "Pruefzahl", 24 to "entpackte Groesse")) {
            val bytes = original.copyOf()
            bytes[verzeichnisStart + versatz] = (bytes[verzeichnisStart + versatz].toInt() xor 0x01).toByte()
            val datei = File(tempDir(), "widerspruch.zip").apply { writeBytes(bytes) }
            val (code, text) = run(
                "sign", "--key", File(keys, "m.secret").absolutePath,
                "--in", datei.absolutePath, "--out", File(tempDir(), "w.czp").absolutePath,
            )
            assertEquals(2, code, "muss $was beanstanden, Ausgabe: $text")
            assertTrue("widerspricht" in text || "Packverfahren" in text, text)
        }
    }

    @Test
    fun packLehntDateienUnterAssetsAbDieKeinBildSind() {
        // Ein echtes Bild, kein blosser Dateikopf: das Werkzeug rechnet die
        // Abschnittskette bis zum Endabschnitt nach.
        val png = echtesPng()
        val dir = contentDir().also {
            File(it, "assets").mkdirs()
            File(it, "assets/bild.png").writeBytes(png)
            // Anleitung, die das Bild verwendet — sonst greift schon die
            // Ablehnung wegen unbenutzter Bilder.
            File(it, "manifest.json").writeBytes(
                """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["tips","guides"]}""".encodeToByteArray()
            )
            File(it, "content/guides.json").writeBytes(
                """{"schema":1,"guides":[{"id":"g","title":"Anleitung","category":"bau","summary":"Kurz.","materials":[{"item":"Holz"}],"steps":[{"text":"Schritt eins.","image":"assets/bild.png"}],"difficulty":1,"sources":[{"name":"Quelle","detail":"Abschnitt 1"}]}]}""".encodeToByteArray()
            )
        }
        assertEquals(0, run("pack", "--in", dir.absolutePath, "--out", File(tempDir(), "gut.zip").absolutePath).first)

        // Dieselbe Datei, aber mit Programmkennung statt Bilddaten.
        File(dir, "assets/bild.png").writeBytes(byteArrayOf(0x4D, 0x5A, 0x90.toByte(), 0x00) + ByteArray(400))
        val (code, text) = run("pack", "--in", dir.absolutePath, "--out", File(tempDir(), "boese.zip").absolutePath)
        assertEquals(2, code)
        assertTrue("kein PNG" in text, text)
    }
}
