package org.compasszero.content

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.compasszero.security.Ed25519
import org.compasszero.security.PackWriter
import org.compasszero.security.TrustStore
import org.compasszero.security.TrustedKey

// Ein winziges Paket darf die App nicht in den Speichertod treiben. Der Fall stammt
// aus einem Pruefdurchgang: 8 KiB Datei, Millionen kleiner Werte, ueber hundert
// Megabyte Heap beim Dekodieren.
class AltgeraetSpeicherTest {

    private val seed = Ed25519.generateSeed()
    private val trust = TrustStore(listOf(TrustedKey("t", Ed25519.publicKeyFromSeed(seed))))

    @Test
    fun aufgeblaehteInhaltsdateiWirdVorDemDekodierenAbgelehnt() {
        val keywords = (1..400_000).joinToString(",") { "\"k\"" }
        val tips = """{"schema":1,"tips":[{"id":"t","title":"Titel","category":"wasser","body":"Text hier.","keywords":[$keywords],"sources":[{"name":"Quelle","detail":"Abschnitt 1"}]}]}"""
        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["tips"]}"""

        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.encodeToByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("content/tips.json"))
            zip.write(tips.encodeToByteArray())
            zip.closeEntry()
        }

        val dir = File.createTempFile("speicher", null).let { it.delete(); it.mkdirs(); it }
        val payloadFile = File(dir, "payload.zip").apply { writeBytes(payload.toByteArray()) }
        val pack = File(dir, "gross.czp")
        PackWriter.write(payloadFile, seed, pack)
        assertTrue(pack.length() < 200_000, "die Angriffsdatei ist winzig: ${pack.length()} Bytes")

        val outcome = PackReader.read(pack, trust)
        val result = outcome.result
        assertTrue(result != null)
        assertNull(result.pack, "aufgeblaehte Datei darf nicht geladen werden")
        assertTrue(
            result.problems.any { it.code == "json-too-many-elements" || it.code == "json-too-large" },
            "erwartet Ablehnung wegen Gestalt, war ${result.problems.map { it.code }}",
        )
    }

    @Test
    fun einVollesPoiPaketLaedtImKleinenSpeicher() {
        // Die Doku sichert eine POI-Zahl zu; hier wird gemessen, ob sie unter der
        // Speichergrenze alter Geraete wirklich erreichbar ist.
        val pois = (1..ContentLimits.MAX_POIS).joinToString(",") {
            """{"id":"poi-$it","kind":"water","lat":47.$it,"lon":12.5,"name":"Quelle $it","note":"ganzjaehrig ergiebig"}"""
        }
        val poisJson = """{"schema":1,"attribution":"Beispieldaten","pois":[$pois]}"""
        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["pois"]}"""

        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.encodeToByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("content/pois.json"))
            zip.write(poisJson.encodeToByteArray())
            zip.closeEntry()
        }

        val dir = File.createTempFile("poivoll", null).let { it.delete(); it.mkdirs(); it }
        val payloadFile = File(dir, "payload.zip").apply { writeBytes(payload.toByteArray()) }
        val pack = File(dir, "voll.czp")
        PackWriter.write(payloadFile, seed, pack)

        val result = PackReader.read(pack, trust).result
        assertTrue(result != null)
        val geladen = result.pack
        assertTrue(
            geladen != null,
            "volles POI-Paket muss laden, Probleme: " + result.problems.take(3).map { it.code },
        )
        assertEquals(ContentLimits.MAX_POIS, geladen.pois.size)
    }

    @Test
    fun ausdehnendeZeichenSprengenDenSpeicherNicht() {
        // Eine arabische Ligatur wird beim Vereinheitlichen zu achtzehn Zeichen.
        // Eine Datei voll davon war knapp 5 KB gross, lief durch alle Pruefungen
        // und riss danach beim Aufbau der Suche den Speicher.
        val ligatur = 0xFDFA.toChar().toString()
        val text = ligatur.repeat(60_000)
        val kapitel = (1..20).joinToString(",") {
            """{"id":"kapitel-$it","title":"Kapitel $it","sections":[{"heading":"Abschnitt","body":"$text"}],"sources":[{"name":"Quelle","detail":"Abschnitt 1"}]}"""
        }
        val agrar = """{"schema":1,"chapters":[$kapitel]}"""
        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"ar","title":"Testpaket","kinds":["agriculture"]}"""

        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.encodeToByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("content/agriculture.json"))
            zip.write(agrar.encodeToByteArray())
            zip.closeEntry()
        }

        val dir = File.createTempFile("ausdehnung", null).let { it.delete(); it.mkdirs(); it }
        val payloadFile = File(dir, "payload.zip").apply { writeBytes(payload.toByteArray()) }
        val pack = File(dir, "ausdehnung.czp")
        PackWriter.write(payloadFile, seed, pack)
        assertTrue(pack.length() < 500_000, "die Angriffsdatei ist klein: ${pack.length()} Bytes")

        val result = PackReader.read(pack, trust).result
        assertTrue(result != null)
        assertNull(result.pack, "aufblaehender Text darf nicht geladen werden")

        // Und selbst wenn so ein Text durchkaeme: der Indexbau darf nicht sterben.
        val notfall = LoadedPack(
            manifest = PackManifest(1, "org.compasszero.test", 1, "ar", "Testpaket", 0, listOf("tips")),
            tips = listOf(Tip("t", "Titel", "kategorie", text, emptyList(), listOf(SourceRef("Quelle", "Abschnitt 1")))),
        )
        SearchIndex.build(notfall)
    }

    @Test
    fun einNachDokuMaximalesTippPaketLaedt() {
        // Die Doku erlaubt 5000 Tipps mit je 20 Schlagwoertern. Wenn die
        // Elementgrenze das verhindert, ist eine der beiden Zahlen falsch.
        val schlagwoerter = (1..ContentLimits.MAX_KEYWORDS).joinToString(",") { "\"schlagwort$it\"" }
        val tipps = (1..ContentLimits.MAX_ITEMS_PER_FILE).joinToString(",") {
            """{"id":"tipp-$it","title":"Titel Nummer $it","category":"wasser","body":"Beispieltext Nummer $it.","keywords":[$schlagwoerter],"sources":[{"name":"Quelle","detail":"Abschnitt 1"}]}"""
        }
        val tippsJson = """{"schema":1,"tips":[$tipps]}"""
        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["tips"]}"""

        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.encodeToByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("content/tips.json"))
            zip.write(tippsJson.encodeToByteArray())
            zip.closeEntry()
        }

        val dir = File.createTempFile("maxtipps", null).let { it.delete(); it.mkdirs(); it }
        val payloadFile = File(dir, "payload.zip").apply { writeBytes(payload.toByteArray()) }
        val pack = File(dir, "maximal.czp")
        PackWriter.write(payloadFile, seed, pack)

        val result = PackReader.read(pack, trust).result
        assertTrue(result != null)
        val geladen = result.pack
        assertTrue(
            geladen != null,
            "nach Doku maximales Paket muss laden, Probleme: " + result.problems.take(3).map { it.code },
        )
        assertEquals(ContentLimits.MAX_ITEMS_PER_FILE, geladen.tips.size)
        SearchIndex.build(geladen)
    }

    @Test
    fun einFeldOhneLeerzeichenSprengtDenSpeicherNicht() {
        // Der Angriff aus dem Pruefbericht: ein einziges Feld aus 1,35 Millionen
        // Ligaturen ohne ein einziges Leerzeichen. Es presst sich auf 4,5 KB und
        // dehnt sich beim Vereinheitlichen auf das Achtzehnfache aus.
        val ligatur = 0xFDFA.toChar().toString()
        val koerper = ligatur.repeat(1_350_000)
        val tipps = """{"schema":1,"tips":[{"id":"t1","title":"Titel","category":"wasser","body":"$koerper","sources":[{"name":"Quelle","detail":"Abschnitt 1"}]}]}"""
        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"ar","title":"Testpaket","kinds":["tips"]}"""

        val payload = ByteArrayOutputStream()
        ZipOutputStream(payload).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.encodeToByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("content/tips.json"))
            zip.write(tipps.encodeToByteArray())
            zip.closeEntry()
        }

        val dir = File.createTempFile("ohneleer", null).let { it.delete(); it.mkdirs(); it }
        val payloadFile = File(dir, "payload.zip").apply { writeBytes(payload.toByteArray()) }
        val pack = File(dir, "ohneleer.czp")
        PackWriter.write(payloadFile, seed, pack)
        assertTrue(pack.length() < 50_000, "Angriffsdatei ist winzig: ${pack.length()} Bytes")

        val result = PackReader.read(pack, trust).result
        assertTrue(result != null)
        assertNull(result.pack, "ueberlanges Feld darf nicht geladen werden")
        assertTrue(
            result.problems.any { it.code == "body-invalid" },
            "erwartet body-invalid, war " + result.problems.map { it.code },
        )
    }

    @Test
    fun lauterVerschiedeneWoerterSprengenDenSpeicherNicht() {
        // Der schlimmste Fall fuer das Wortverzeichnis: jedes Wort kommt genau
        // einmal vor. Nicht die Zeichenzahl entscheidet dann ueber den Speicher,
        // sondern die Zahl verschiedener Woerter.
        val quelle = listOf(SourceRef("Beispielquelle", "Abschnitt 1"))
        var zaehler = 0
        fun woerter(anzahl: Int): String = buildString {
            repeat(anzahl) {
                if (it > 0) append(' ')
                append(wortNummer(zaehler++))
            }
        }

        val tipps = (1..2000).map {
            Tip("tipp-$it", "Titel ${woerter(3)}", "kategorie", woerter(180), listOf(woerter(1)), quelle)
        }
        val pack = LoadedPack(
            manifest = PackManifest(1, "org.compasszero.test", 1, "de", "Testpaket", 0, listOf("tips")),
            tips = tipps,
        )
        val zeichen = tipps.sumOf { it.title.length + it.body.length + it.keywords.sumOf { k -> k.length } }
        assertTrue(zeichen < ContentLimits.MAX_SUCHTEXT_ZEICHEN, "Testpaket muss die Grenze einhalten: $zeichen")

        val index = SearchIndex.build(pack)
        assertTrue(index.search("wort").isNotEmpty())
    }

    // Kurze, garantiert verschiedene Woerter.
    /**
     * Ein Wort je Nummer, und zwar wirklich fuer jede Nummer ein anderes.
     *
     * FUENF STELLEN, NICHT VIER: Mit vier Stellen zu je 26 Buchstaben gibt es
     * 456 976 verschiedene Woerter. Wer damit ein Paket mit mehr Vorkommen baut,
     * bekommt ab dieser Zahl WIEDERHOLUNGEN -- und misst dann nicht mehr den
     * schlimmsten Fall, sondern einen billigeren, ohne es zu merken. Genau das
     * war am 20.08.2026 passiert: Die Begruendung an der Grenze berief sich auf
     * eine Messung mit "lauter verschiedenen Woertern" bei 602 400 Vorkommen,
     * die es so nicht gegeben hatte. Fuenf Stellen ergeben 11,9 Millionen
     * Woerter und reichen weit ueber jede Grenze hinaus, die hier je gemessen
     * wird.
     */
    /**
     * Kurzes Wort fuer Testpakete mit KLEINEM Wortschatz.
     *
     * WOZU DIE KUERZERE FORM: Um die Vorkommensgrenze zu pruefen, braucht es
     * ueber 600 000 Woerter in einer Datei. Mit sechs Buchstaben je Wort waere
     * die Datei groesser als MAX_JSON_BYTES, und dann meldete der Test die
     * falsche Grenze. Vier Zeichen ergeben 17 576 verschiedene Woerter -- fuer
     * einen absichtlich kleinen Wortschatz reicht das weit.
     */
    private fun wortKurz(n: Int): String {
        val stellen = "abcdefghijklmnopqrstuvwxyz"
        var rest = n
        val sb = StringBuilder("w")
        repeat(3) {
            sb.append(stellen[rest % 26])
            rest /= 26
        }
        return sb.toString()
    }

    /**
     * Wie viele Buchstaben ein Testwort nach dem "w" traegt.
     *
     * DIESE ZAHL STEHT NUR EINMAL, und der Baukasten wie seine Selbstpruefung
     * lesen beide von hier. Stuenden sie getrennt, koennte jemand den Baukasten
     * verkleinern, ohne dass die Pruefung es merkt -- und die Pruefung deckte
     * dann genau den Fehler nicht mehr auf, gegen den sie geschrieben wurde.
     * Fuenf Stellen ergeben 11 881 376 verschiedene Woerter.
     */
    private val WORTNUMMER_STELLEN = 5

    private fun wortNummer(n: Int): String {
        val stellen = "abcdefghijklmnopqrstuvwxyz"
        var rest = n
        val sb = StringBuilder("w")
        repeat(WORTNUMMER_STELLEN) {
            sb.append(stellen[rest % 26])
            rest /= 26
        }
        return sb.toString()
    }

    @Test
    fun sichWiederholenderTextSprengtDenSpeicherNicht() {
        // Der Gegenfall zum Paket aus lauter einmaligen Woertern: hier gibt es nur
        // wenige verschiedene Woerter, aber sehr viele Vorkommen. Ein koreanisches
        // Paket am Textmaximum liegt genau in dieser Gegend.
        val quelle = listOf(SourceRef("Beispielquelle", "Abschnitt 1"))
        val koerper = buildString {
            var n = 0
            while (length < ContentLimits.MAX_BODY_LENGTH - 4) {
                if (isNotEmpty()) append(' ')
                append(('a' + (n % 26))).append(('a' + (n / 26 % 26)))
                n++
            }
        }
        val tipps = (1..62).map {
            Tip("tipp-$it", "Titel Nummer $it", "kategorie", koerper, emptyList(), quelle)
        }
        val zeichen = tipps.sumOf { it.title.length + it.body.length }
        assertTrue(
            zeichen < ContentLimits.MAX_SUCHTEXT_ZEICHEN,
            "Testpaket muss unter der Grenze liegen, war $zeichen",
        )

        val pack = LoadedPack(
            manifest = PackManifest(1, "org.compasszero.test", 1, "de", "Testpaket", 0, listOf("tips")),
            tips = tipps,
        )
        val index = SearchIndex.build(pack)
        assertTrue(index.search("aa").isNotEmpty())
    }

    // Baut ein agriculture-Paket aus lauter verschiedenen, kurzen Woertern — der
    // gleichzeitig schlimmste Fall fuer Vorkommen und Verschiedenheit im
    // Wortverzeichnis, siehe ContentLimits.MAX_SUCHINDEX_WORTVORKOMMEN.
    private fun agricultureJsonMitVorkommen(woerterJeAbschnitt: Int, wortschatz: Int = 0): String {
        val kapitel = 200
        val abschnitte = 10
        var zaehler = 0
        // wortschatz = 0 heisst: jedes Wort verschieden, der teuerste Fall fuer
        // das Wortverzeichnis. Ein Wert groesser null laesst die Woerter reihum
        // wiederkehren -- damit laesst sich die Zahl der VORKOMMEN treiben, ohne
        // den Wortschatz mitwachsen zu lassen. Nur so ist jede der beiden
        // Grenzen einzeln pruefbar.
        fun naechstesWort(): String {
            val n = if (wortschatz > 0) zaehler % wortschatz else zaehler
            zaehler++
            return if (wortschatz in 1..17_576) wortKurz(n) else wortNummer(n)
        }
        fun woerter(anzahl: Int): String = buildString {
            repeat(anzahl) {
                if (it > 0) append(' ')
                append(naechstesWort())
            }
        }
        return buildString {
            append("""{"schema":1,"chapters":[""")
            for (k in 1..kapitel) {
                if (k > 1) append(',')
                append("""{"id":"kapitel-$k","title":"Kapitel $k","sections":[""")
                for (a in 1..abschnitte) {
                    if (a > 1) append(',')
                    append("""{"heading":"Abschnitt","body":"""").append(woerter(woerterJeAbschnitt)).append(""""}""")
                }
                append("""],"sources":[{"name":"Beispielquelle","detail":"Abschnitt 1"}]}""")
            }
            append("]}")
        }.also { json ->
            // ZWEI STILLE FEHLSCHLAEGE, GEGEN DIE SICH DER BAUKASTEN SELBST
            // ABSICHERT -- beide sind wirklich passiert:
            //
            // 1. Reicht der Wortvorrat nicht, wiederholen sich die Woerter ab
            //    dort, ohne dass etwas auffaellt. Der Test misst dann einen
            //    billigeren Fall und behauptet trotzdem, jedes Wort sei
            //    verschieden. Genau das stand von Juli bis zum 20.08.2026 als
            //    "gemessen" in ContentLimits.kt.
            // 2. Wird die Datei groesser als MAX_JSON_BYTES, greift die
            //    Bytegrenze -- und der Test meldet eine ganz andere Grenze als
            //    die, die er pruefen sollte.
            if (wortschatz == 0) {
                var moeglich = 1
                repeat(WORTNUMMER_STELLEN) { moeglich *= 26 }
                check(zaehler <= moeglich) {
                    "Der Wortvorrat reicht nicht: $zaehler Woerter gebraucht, $moeglich moeglich. " +
                        "Ohne mehr Stellen in wortNummer misst dieser Test keine lauter " +
                        "verschiedenen Woerter mehr, sondern eine Mischung."
                }
            }
            check(json.length <= ContentLimits.MAX_JSON_BYTES) {
                "Das Testpaket ist ${json.length} Bytes gross, erlaubt sind " +
                    "${ContentLimits.MAX_JSON_BYTES}. Dann greift die Bytegrenze und der Test " +
                    "prueft nicht mehr, was er soll -- weniger Woerter je Abschnitt nehmen."
            }
        }
    }

    private val agricultureManifest =
        """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["agriculture"]}"""

    @Test
    fun einGrosserWortschatzUnterDerGrenzeLaedtUndIndiziert() {
        // 298 400 verschiedene Woerter (200 * 10 * 148 plus Ueberschriften/Titel)
        // — knapp unter ContentLimits.MAX_SUCHINDEX_VERSCHIEDENE_WOERTER.
        //
        // DIESER TEST IST DIE MESSUNG SELBST, nicht ihre Wiederholung: Der
        // Testlauf haelt nur 96 MB bereit (siehe build.gradle.kts), und hier
        // wird ein Paket dicht an der Grenze wirklich geladen UND indiziert.
        // Faellt er nach einer Anhebung der Grenze um, ist nicht der Test
        // kaputt — dann traegt die neue Zahl auf einem alten Geraet nicht.
        //
        // Und es ist der TEUERSTE Fall: In diesem Paket ist jedes Wort
        // verschieden, Wortschatz und Vorkommen sind also dieselbe Zahl. Das
        // echte Europa-Paket wiederholt jedes Wort im Schnitt fuenfzehnmal und
        // hat damit ein rund zwanzigmal kleineres Verzeichnis.
        // 450 000 Vorkommen am 17.08.2026, 500 000 am 20.08.2026 -- seit dem
        // 20.08.2026 zaehlt die Grenze die verschiedenen Woerter.
        val agricultureJson = agricultureJsonMitVorkommen(148)
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to agricultureManifest.encodeToByteArray(),
                "content/agriculture.json" to agricultureJson.encodeToByteArray(),
            ),
            emptySet(),
        )
        val pack = result.pack
        assertTrue(
            pack != null,
            "Paket unter der Wortschatz-Grenze muss laden: " + result.problems.take(3).map { it.code },
        )
        val index = SearchIndex.build(pack)
        assertTrue(index.search("waaaa").isNotEmpty())
    }

    // Der Fall, den es bis zum neunten Pruefdurchgang nicht gab: ALLE Grenzen
    // gleichzeitig ausgeschoepft. Jede fuer sich war gemessen, die Summe nie --
    // und genau daran ist ein Paket gestorben, das jede einzelne Regel einhielt.
    // Der Testlauf haelt absichtlich nur 96 MB bereit, die Speicherklasse alter
    // Zielgeraete.
    @Test
    fun einPaketAnAllenGrenzenLaedtUndIndiziert() {
        val kapitel = agricultureJsonMitVorkommen(115)
        val tipps = buildString {
            append("""{"schema":1,"tips":[""")
            for (i in 1..ContentLimits.MAX_ITEMS_PER_FILE) {
                if (i > 1) append(',')
                append("""{"id":"tipp-$i","title":"Tipp $i","category":"wasser","body":""")
                append(""""Beispieltext fuer Tipp Nummer $i mit Fliesstext dahinter.",""")
                append(""""sources":[{"name":"Beispielquelle","detail":"Abschnitt 1"}]}""")
            }
            append("]}")
        }
        val punkte = buildString {
            append("""{"schema":1,"attribution":"Beispieldaten","pois":[""")
            for (i in 1..ContentLimits.MAX_POIS) {
                if (i > 1) append(',')
                append("""{"id":"p$i","kind":"water","lat":47.4,"lon":13.1,"name":"Quelle $i"}""")
            }
            append("]}")
        }
        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de",""" +
            """"title":"Testpaket","kinds":["tips","agriculture","pois"]}"""

        val result = PackParser.parse(
            mapOf(
                "manifest.json" to manifest.encodeToByteArray(),
                "content/agriculture.json" to kapitel.encodeToByteArray(),
                "content/tips.json" to tipps.encodeToByteArray(),
                "content/pois.json" to punkte.encodeToByteArray(),
            ),
            emptySet(),
        )
        val pack = result.pack
        assertTrue(
            pack != null,
            "Paket an allen Grenzen muss laden: " + result.problems.take(3).map { it.code },
        )
        assertEquals(ContentLimits.MAX_ITEMS_PER_FILE, pack.tips.size)
        assertEquals(ContentLimits.MAX_CHAPTERS, pack.agriculture.size)
        assertEquals(ContentLimits.MAX_POIS, pack.pois.size)

        val index = SearchIndex.build(pack)
        assertTrue(index.search("waaaa").isNotEmpty(), "der Index kennt die Kapitelwoerter nicht")
        assertTrue(index.search("fliesstext").isNotEmpty(), "die Tipps fehlen im Index")
    }

    // Die Vorwarnstufe aus PackParser.pruefeSuchtextMenge: Ab 90 % der
    // Wortvorkommen-Grenze steht eine Warnung, aber das Paket muss trotzdem
    // laden -- siehe MERKZETTEL.md, "Die Wand am Ende des Wortbudgets". Am
    // 17.08.2026 lag ein fertiges, geprueftes Paket wochenlang auf Halde, weil
    // die Grenze ohne Vorwarnung riss.
    @Test
    fun suchbudgetWarntAbNeunzigProzentOhneDasLadenZuVerhindern() {
        // 682 400 Vorkommen bei nur rund 2 000 verschiedenen Woertern: reichlich
        // ueber 90 % der Vorkommensgrenze (675 000) und weit unter der
        // Wortschatz-Grenze, damit wirklich die gemeinte Warnung geprueft wird.
        // Am 21.08.2026 mit der Grenze von 600 000 auf 750 000 mitgewandert.
        val agricultureJson = agricultureJsonMitVorkommen(340, wortschatz = 2_000)
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to agricultureManifest.encodeToByteArray(),
                "content/agriculture.json" to agricultureJson.encodeToByteArray(),
            ),
            emptySet(),
        )
        assertTrue(
            result.pack != null,
            "eine Vorwarnung darf das Laden nicht verhindern: " + result.problems.map { it.code },
        )
        val warnung = result.problems.singleOrNull { it.code == "content-search-terms-near-limit" }
        assertTrue(warnung != null, "erwartet content-search-terms-near-limit, war " + result.problems.map { it.code })
        assertEquals(Severity.Warning, warnung!!.severity)

        val grenze = ContentLimits.MAX_SUCHINDEX_WORTVORKOMMEN.toLong()
        val treffer = Regex("""^(\d+) von (\d+) Wortvorkommen erreicht \(90 %\), noch (\d+) frei$""")
            .find(warnung.detail)
        assertTrue(treffer != null, "Text passt nicht zum erwarteten Format: ${warnung.detail}")
        val (wortvorkommenStr, grenzeStr, freiStr) = treffer!!.destructured
        assertEquals(grenze, grenzeStr.toLong(), "die genannte Grenze stimmt nicht")
        assertEquals(grenze - wortvorkommenStr.toLong(), freiStr.toLong(), "der freie Rest im Text stimmt nicht")
    }

    // Gegenprobe: deutlich unter der Schwelle bleibt das Paket ganz ohne
    // Probleme -- die Warnung darf nicht schon frueher kommen.
    @Test
    fun unterhalbNeunzigProzentBleibtEsOhneWarnung() {
        val agricultureJson = agricultureJsonMitVorkommen(100)
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to agricultureManifest.encodeToByteArray(),
                "content/agriculture.json" to agricultureJson.encodeToByteArray(),
            ),
            emptySet(),
        )
        assertTrue(result.pack != null, "Paket haette laden muessen: " + result.problems.map { it.code })
        assertTrue(
            result.problems.none { it.code == "content-search-terms-near-limit" },
            "Warnung kam zu frueh: " + result.problems.map { it.code },
        )
    }

    // Die Grenze, die den Speicher wirklich beschreibt: zu viele VERSCHIEDENE
    // Woerter. Ein verschiedenes Wort kostet rund das Achtfache eines weiteren
    // Vorkommens, weil es einen eigenen Eintrag im Wortverzeichnis braucht.
    // Hier liegt das Paket mit 304 400 verschiedenen Woertern ueber der Grenze,
    // aber mit denselben 304 400 Vorkommen weit UNTER der Vorkommensgrenze --
    // faellt der Test um, greift die falsche der beiden Grenzen.
    @Test
    fun einZuGrosserWortschatzWirdVorDemIndexbauAbgelehnt() {
        val agricultureJson = agricultureJsonMitVorkommen(151)
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to agricultureManifest.encodeToByteArray(),
                "content/agriculture.json" to agricultureJson.encodeToByteArray(),
            ),
            emptySet(),
        )
        assertNull(result.pack, "Paket ueber der Wortschatz-Grenze darf nicht geladen werden")
        val meldungen = result.problems.map { it.code }
        assertTrue(
            "content-too-many-search-words" in meldungen,
            "erwartet content-too-many-search-words, war $meldungen",
        )
        assertTrue(
            "content-too-many-search-terms" !in meldungen,
            "hier darf NICHT die Vorkommensgrenze greifen, war $meldungen",
        )
    }

    @Test
    fun zuVieleWortvorkommenWerdenVorDemIndexbauAbgelehnt() {
        // Der Fund des neunten Pruefdurchgangs: das Zeichenbudget schuetzt nicht
        // vor sehr vielen kurzen Woertern. Ein Paket mit 762 400 Wortvorkommen
        // (200 * 10 * 380 plus Ueberschriften/Titel) liegt ueber der
        // Grenze — und muss abgelehnt werden, bevor SearchIndex.build
        // ueberhaupt aufgerufen wird. Genau das war der Fehler: ein Paket, das
        // die dokumentierten Grenzen einhaelt, aber beim Indexaufbau real den
        // Speicher sprengt.
        //
        // Die Zahl haengt an ContentLimits.MAX_SUCHINDEX_WORTVORKOMMEN und muss
        // bei jeder Aenderung mitwandern, sonst prueft der Test nichts mehr.
        val agricultureJson = agricultureJsonMitVorkommen(380, wortschatz = 2_000)
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to agricultureManifest.encodeToByteArray(),
                "content/agriculture.json" to agricultureJson.encodeToByteArray(),
            ),
            emptySet(),
        )
        assertNull(result.pack, "Paket ueber der Wortvorkommen-Grenze darf nicht geladen werden")
        assertTrue(
            "content-too-many-search-terms" in result.problems.map { it.code },
            "erwartet content-too-many-search-terms, war " + result.problems.map { it.code },
        )
    }

    /**
     * Testwort mit frei waehlbarer Laenge: "w" plus n Buchstaben.
     *
     * WOZU EINE DRITTE FORM NEBEN wortKurz UND wortNummer: Die beiden alten
     * treiben je eine Groesse (Vorkommen bzw. Wortschatz) und legen die anderen
     * still fest. Fuer die Messungen unten braucht es beides zugleich am
     * Anschlag, und dafuer muss die Wortlaenge einstellbar sein: Sie entscheidet,
     * wie viele ZEICHEN auf ein Vorkommen kommen.
     */
    private fun wortMitLaenge(n: Int, buchstaben: Int): String {
        val stellen = "abcdefghijklmnopqrstuvwxyz"
        var rest = n
        val sb = StringBuilder("w")
        repeat(buchstaben) {
            sb.append(stellen[rest % 26])
            rest /= 26
        }
        return sb.toString()
    }

    /**
     * Baut ein Paket aus zwei Dateien, misst nach, dass es wirklich am Anschlag
     * liegt, und laedt und indiziert es dann bei 96 MB.
     *
     * WARUM ZWEI DATEIEN: Ein einzelnes agriculture.json mit mehreren Millionen
     * Zeichen waere groesser als MAX_JSON_BYTES (4 MiB). Dann griffe die
     * Bytegrenze, und die Messung meldete eine ganz andere Grenze als die
     * gemeinte -- derselbe stille Fehlschlag, gegen den sich der Baukasten
     * weiter oben schon absichert.
     */
    private fun messeAmBudget(buchstaben: Int, woerterJeAbschnitt: Int, woerterJeTipp: Int) {
        val wortschatz = 299_000
        var zaehler = 0
        fun woerter(anzahl: Int): String = buildString {
            repeat(anzahl) {
                if (it > 0) append(' ')
                append(wortMitLaenge(zaehler % wortschatz, buchstaben))
                zaehler++
            }
        }

        val kapitel = buildString {
            append("""{"schema":1,"chapters":[""")
            for (k in 1..ContentLimits.MAX_CHAPTERS) {
                if (k > 1) append(',')
                append("""{"id":"kapitel-$k","title":"Kapitel","sections":[""")
                for (a in 1..10) {
                    if (a > 1) append(',')
                    append("""{"heading":"Abschnitt","body":"""").append(woerter(woerterJeAbschnitt)).append(""""}""")
                }
                append("""],"sources":[{"name":"Beispielquelle","detail":"Abschnitt 1"}]}""")
            }
            append("]}")
        }
        val tipps = buildString {
            append("""{"schema":1,"tips":[""")
            for (i in 1..ContentLimits.MAX_ITEMS_PER_FILE) {
                if (i > 1) append(',')
                append("""{"id":"tipp-$i","title":"Tipp","category":"wasser","body":"""")
                append(woerter(woerterJeTipp))
                append("""","sources":[{"name":"Beispielquelle","detail":"Abschnitt 1"}]}""")
            }
            append("]}")
        }
        check(kapitel.length <= ContentLimits.MAX_JSON_BYTES) {
            "agriculture.json ist ${kapitel.length} Bytes gross, erlaubt sind " +
                "${ContentLimits.MAX_JSON_BYTES} -- dann greift die Bytegrenze statt der gemeinten."
        }
        check(tipps.length <= ContentLimits.MAX_JSON_BYTES) {
            "tips.json ist ${tipps.length} Bytes gross, erlaubt sind ${ContentLimits.MAX_JSON_BYTES}."
        }

        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de",""" +
            """"title":"Testpaket","kinds":["tips","agriculture"]}"""
        val result = PackParser.parse(
            mapOf(
                "manifest.json" to manifest.encodeToByteArray(),
                "content/agriculture.json" to kapitel.encodeToByteArray(),
                "content/tips.json" to tipps.encodeToByteArray(),
            ),
            emptySet(),
        )
        val pack = result.pack
        assertTrue(
            pack != null,
            "Paket am Budget muss laden: " + result.problems.take(3).map { it.code },
        )

        var zeichen = 0L
        var vorkommen = 0L
        val verschiedene = HashSet<String>()
        fun zaehle(text: String) {
            zeichen += text.length.toLong()
            for (wort in text.split(' ')) {
                if (wort.isEmpty()) continue
                vorkommen++
                verschiedene.add(wort)
            }
        }
        for (t in pack.tips) { zaehle(t.title); zaehle(t.body); t.keywords.forEach(::zaehle) }
        for (c in pack.agriculture) { zaehle(c.title); c.sections.forEach { zaehle(it.heading); zaehle(it.body) } }
        println("Budget-Messung: $zeichen Zeichen, $vorkommen Vorkommen, ${verschiedene.size} verschiedene")

        assertTrue(
            verschiedene.size >= ContentLimits.MAX_SUCHINDEX_VERSCHIEDENE_WOERTER * 9 / 10,
            "misst nicht am Anschlag: nur ${verschiedene.size} von " +
                "${ContentLimits.MAX_SUCHINDEX_VERSCHIEDENE_WOERTER} verschiedenen Woertern",
        )
        assertTrue(
            zeichen >= ContentLimits.MAX_SUCHTEXT_ZEICHEN * 9 / 10 ||
                vorkommen >= ContentLimits.MAX_SUCHINDEX_WORTVORKOMMEN * 9 / 10,
            "misst nicht am Anschlag: $zeichen Zeichen und $vorkommen Vorkommen",
        )

        val index = SearchIndex.build(pack)
        assertTrue(index.search("waaaa").isNotEmpty(), "der Index kennt die Woerter nicht")
    }

    // DIE MESSUNG ZU MAX_SUCHTEXT_ZEICHEN, angelegt am 21.08.2026.
    //
    // WARUM ES SIE VORHER NICHT GAB: Die Zeichengrenze stand seit Juli als
    // "gemessen" da, aber kein Test hat je ein Paket gebaut, das sie zusammen
    // mit dem vollen Wortschatz ausreizt. einPaketAnAllenGrenzenLaedtUndIndiziert
    // kommt nicht in die Naehe -- es fuellt agriculture mit kurzen Woertern und
    // bleibt bei rund einem Drittel des Zeichenbudgets.
    //
    // WAS DIE MESSUNG AM 21.08.2026 ERGEBEN HAT (96 MB Heap, Wortschatz 299 003):
    //   3 712 400 Zeichen / 467 200 Vorkommen -> laedt und indiziert
    //   4 472 400 Zeichen / 562 200 Vorkommen -> laedt und indiziert
    //   4 792 400 Zeichen / 602 200 Vorkommen -> laedt und indiziert
    //   5 320 400 Zeichen / 668 200 Vorkommen -> OutOfMemoryError
    //   5 792 400 Zeichen / 727 200 Vorkommen -> OutOfMemoryError
    // Die Wand steht also zwischen 4 792 400 und 5 320 400 Zeichen, SOLANGE der
    // Wortschatz voll ausgereizt ist. Die Grenze von 4 500 000 liegt rund ein
    // Sechstel unter dem letzten Stand, der noch getragen hat.
    @Test
    fun einPaketAmZeichenbudgetLaedtUndIndiziert() {
        messeAmBudget(buchstaben = 6, woerterJeAbschnitt = 150, woerterJeTipp = 51)
    }

    // Der andere schlimmste Fall: viele VORKOMMEN bei vollem Wortschatz, aber
    // kurzen Woertern. Die alten Vorkommens-Tests messen mit einem Wortschatz von
    // 2 000 -- dort kostet das Verzeichnis fast nichts. Erst beides zusammen
    // zeigt, ob MAX_SUCHINDEX_WORTVORKOMMEN traegt.
    @Test
    fun vieleVorkommenBeiVollemWortschatzLadenUndIndizieren() {
        messeAmBudget(buchstaben = 4, woerterJeAbschnitt = 190, woerterJeTipp = 70)
    }
}
