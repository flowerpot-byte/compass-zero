package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PackParserTest {

    private val manifestJson =
        """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["tips","guides","agriculture","pois"]}"""
    private val tipsJson =
        """{"schema":1,"tips":[{"id":"tipp-eins","title":"Titel Eins","category":"wasser","body":"Beispieltext.","keywords":["beispiel"],"sources":[{"name":"Quelle A","detail":"Abschnitt 1"}]}]}"""
    private val guidesJson =
        """{"schema":1,"guides":[{"id":"anleitung-eins","title":"Anleitung Eins","category":"werkzeug","summary":"Kurz.","materials":[{"item":"Beispielholz"}],"steps":[{"text":"Erster Schritt.","image":"assets/bild.png"}],"difficulty":1,"sources":[{"name":"Quelle B","detail":"Abschnitt 1"}]}]}"""
    private val agricultureJson =
        """{"schema":1,"chapters":[{"id":"kapitel-eins","title":"Kapitel Eins","sections":[{"heading":"Abschnitt","body":"Text."}],"sources":[{"name":"Quelle C","detail":"Abschnitt 1"}]}]}"""
    private val poisJson =
        """{"schema":1,"attribution":"Beispieldaten","pois":[{"id":"poi-1","kind":"water","lat":47.0,"lon":12.0}]}"""

    private fun files(vararg overrides: Pair<String, String?>): Map<String, ByteArray> {
        val base = mutableMapOf(
            "manifest.json" to manifestJson,
            "content/tips.json" to tipsJson,
            "content/guides.json" to guidesJson,
            "content/agriculture.json" to agricultureJson,
            "content/pois.json" to poisJson,
        )
        for ((name, value) in overrides) {
            if (value == null) base.remove(name) else base[name] = value
        }
        return base.mapValues { it.value.encodeToByteArray() }
    }

    private val assets = setOf("assets/bild.png")

    @Test
    fun fullPackLoads() {
        val result = PackParser.parse(files(), assets)
        val pack = assertNotNull(result.pack)
        assertEquals(emptyList(), result.problems)
        assertEquals(listOf("tipp-eins"), pack.tips.map { it.id })
        assertEquals(listOf("anleitung-eins"), pack.guides.map { it.id })
        assertEquals(listOf("kapitel-eins"), pack.agriculture.map { it.id })
        assertEquals(listOf("poi-1"), pack.pois.map { it.id })
        assertEquals("Beispieldaten", pack.poiAttribution)
        assertEquals("org.compasszero.test", pack.manifest.id)
    }

    @Test
    fun missingManifestIsFatal() {
        val result = PackParser.parse(files("manifest.json" to null), assets)
        assertNull(result.pack)
        assertEquals(listOf("manifest-missing"), result.problems.map { it.code })
    }

    @Test
    fun declaredButMissingContentFileIsFatal() {
        val result = PackParser.parse(files("content/pois.json" to null), assets)
        assertNull(result.pack)
        assertTrue(result.problems.any { it.code == "content-missing" && it.where == "content/pois.json" })
    }

    @Test
    fun problemsFromSeveralFilesAreAllCollected() {
        val result = PackParser.parse(
            files(
                "content/tips.json" to "kein json",
                "content/pois.json" to """{"schema":1,"attribution":" ","pois":[]}""",
            ),
            assets,
        )
        assertNull(result.pack)
        val codes = result.problems.map { it.code }
        assertTrue("json-invalid" in codes)
        assertTrue("attribution-invalid" in codes)
    }

    @Test
    fun undeclaredContentFileIsIgnoredWithWarning() {
        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["tips"]}"""
        val result = PackParser.parse(
            files(
                "manifest.json" to manifest,
                "content/guides.json" to null,
                "content/agriculture.json" to null,
                "content/pois.json" to null,
                "content/extra.json" to """{"schema":1}""",
            ),
            emptySet(),
        )
        val pack = assertNotNull(result.pack)
        assertEquals(emptyList(), pack.guides)
        assertTrue(result.problems.any { it.code == "file-ignored" && it.where == "content/extra.json" })
    }

    @Test
    fun unknownKindYieldsWarningButNoMissingFile() {
        val manifest = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Testpaket","kinds":["tips","hologram"]}"""
        val result = PackParser.parse(
            files(
                "manifest.json" to manifest,
                "content/guides.json" to null,
                "content/agriculture.json" to null,
                "content/pois.json" to null,
            ),
            emptySet(),
        )
        assertNotNull(result.pack)
        val codes = result.problems.map { it.code }
        assertTrue("kind-unknown" in codes)
        assertTrue("content-missing" !in codes)
    }

    @Test
    fun assetCrossChecks() {
        val missing = PackParser.parse(files(), emptySet())
        assertNotNull(missing.pack)
        assertTrue(missing.problems.any { it.code == "asset-missing" && it.where == "assets/bild.png" })

        val unused = PackParser.parse(files(), assets + "assets/verwaist.png")
        assertNotNull(unused.pack)
        assertTrue(unused.problems.any { it.code == "asset-unused" && it.where == "assets/verwaist.png" })
    }

    // Ohne `detail` faellt die Quellenangabe schon beim Lesen durch, nicht erst
    // bei der Pruefung — das Feld hat keinen Vorgabewert mehr.
    @Test
    fun quelleOhneBelegangabeLaedtNicht() {
        val ohneDetail = """{"schema":1,"tips":[{"id":"t","title":"Titel","category":"wasser",""" +
            """"body":"Beispieltext.","sources":[{"name":"Quelle A"}]}]}"""
        val result = PackParser.parse(files("content/tips.json" to ohneDetail), assets)
        assertNull(result.pack)
        assertTrue("json-invalid" in result.problems.map { it.code }, result.problems.map { it.code }.toString())

        val leeresDetail = """{"schema":1,"tips":[{"id":"t","title":"Titel","category":"wasser",""" +
            """"body":"Beispieltext.","sources":[{"name":"Quelle A","detail":""}]}]}"""
        val leer = PackParser.parse(files("content/tips.json" to leeresDetail), assets)
        assertNull(leer.pack)
        assertTrue("source-detail-missing" in leer.problems.map { it.code }, leer.problems.map { it.code }.toString())
    }

    // Eine Anleitung ohne Materialliste ist gueltig: nicht jeder Ablauf braucht
    // Material, die Schritte sind der Inhalt.
    @Test
    fun anleitungOhneMaterialListeLaedt() {
        val ohneMaterial = """{"schema":1,"guides":[{"id":"anleitung-eins","title":"Anleitung Eins",""" +
            """"category":"werkzeug","summary":"Kurz.","steps":[{"text":"Erster Schritt.","image":"assets/bild.png"}],""" +
            """"difficulty":1,"sources":[{"name":"Quelle B","detail":"Abschnitt 1"}]}]}"""
        val result = PackParser.parse(files("content/guides.json" to ohneMaterial), assets)
        val pack = assertNotNull(result.pack, result.problems.map { it.code }.toString())
        assertEquals(emptyList(), pack.guides.single().materials)
    }

    @Test
    fun fatalInOneKindRejectsWholePack() {
        val badTips = """{"schema":1,"tips":[{"id":"x","title":"T","category":"c","body":"B","sources":[]}]}"""
        val result = PackParser.parse(files("content/tips.json" to badTips), assets)
        assertNull(result.pack)
        assertTrue(result.problems.any { it.code == "sources-missing" })
    }
}
