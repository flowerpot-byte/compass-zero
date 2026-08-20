package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManifestTest {

    private fun manifest(
        schema: Int = 1,
        id: String = "org.compasszero.base.de",
        version: Int = 1,
        language: String = "de",
        title: String = "Basispaket",
        created: Long = 0,
        kinds: List<String> = listOf("tips"),
    ) = PackManifest(schema, id, version, language, title, created, kinds)

    private fun codesOf(m: PackManifest): List<String> {
        val log = ProblemLog()
        ManifestCheck.validate(m, log)
        return log.all.map { it.code }
    }

    @Test
    fun validManifestHasNoProblems() {
        assertEquals(emptyList(), codesOf(manifest(kinds = listOf("tips", "guides", "agriculture", "pois"))))
    }

    @Test
    fun validateReturnsKnownKindsOnly() {
        val log = ProblemLog()
        val kinds = ManifestCheck.validate(manifest(kinds = listOf("tips", "hologram")), log)
        assertEquals(listOf("tips"), kinds)
        assertEquals(listOf("kind-unknown"), log.all.map { it.code })
        assertTrue(log.all.single().severity == Severity.Warning)
    }

    @Test
    fun brokenFieldsAreFatal() {
        assertTrue("schema-unsupported" in codesOf(manifest(schema = 2)))
        assertTrue("id-invalid" in codesOf(manifest(id = "Kein_gueltiges-ID!")))
        assertTrue("id-invalid" in codesOf(manifest(id = "einzelwort")))
        assertTrue("version-invalid" in codesOf(manifest(version = 0)))
        assertTrue("language-invalid" in codesOf(manifest(language = "Deutsch!")))
        assertTrue("title-invalid" in codesOf(manifest(title = "  ")))
        assertTrue("created-invalid" in codesOf(manifest(created = -5)))
        assertTrue("kinds-empty" in codesOf(manifest(kinds = emptyList())))
        assertTrue("kinds-duplicate" in codesOf(manifest(kinds = listOf("tips", "tips"))))
        assertTrue("kinds-empty" in codesOf(manifest(kinds = listOf("hologram"))))
    }

    @Test
    fun decodeGuardedParsesManifest() {
        val log = ProblemLog()
        val json = """{"schema":1,"id":"org.compasszero.base.de","version":3,"language":"de","title":"Basispaket","kinds":["tips"],"zukunft":true}"""
        val m = decodeGuarded<PackManifest>(json.encodeToByteArray(), "manifest.json", log)
        assertNotNull(m)
        assertEquals(3, m.version)
        assertEquals(0, m.created)
        assertEquals(emptyList(), log.all)
    }

    @Test
    fun decodeGuardedFlagsGarbage() {
        val log = ProblemLog()
        assertNull(decodeGuarded<PackManifest>("kein json".encodeToByteArray(), "manifest.json", log))
        assertEquals(listOf("json-invalid"), log.all.map { it.code })
        assertTrue(log.hasFatal)
    }

    @Test
    fun decodeGuardedFlagsMissingFields() {
        val log = ProblemLog()
        assertNull(decodeGuarded<PackManifest>("""{"schema":1}""".encodeToByteArray(), "manifest.json", log))
        assertEquals(listOf("json-invalid"), log.all.map { it.code })
    }

    @Test
    fun decodeGuardedRejectsBrokenUtf8() {
        val log = ProblemLog()
        val broken = byteArrayOf(0x7B, 0x22, 0x61, 0x22, 0x3A, 0x22) + byteArrayOf(-1, -2) + byteArrayOf(0x22, 0x7D)
        assertNull(decodeGuarded<PackManifest>(broken, "manifest.json", log))
        assertEquals(listOf("json-not-utf8"), log.all.map { it.code })
    }

    @Test
    fun decodeGuardedRejectsDuplicateKeys() {
        val log = ProblemLog()
        val json = """{"schema":1,"id":"org.compasszero.test","version":1,"language":"de","title":"Erst","title":"Dann","kinds":["tips"]}"""
        assertNull(decodeGuarded<PackManifest>(json.encodeToByteArray(), "manifest.json", log))
        assertEquals(listOf("json-duplicate-key"), log.all.map { it.code })
    }

    @Test
    fun decodeGuardedRejectsMassiveElementCount() {
        val log = ProblemLog()
        val many = """{"schema":1,"kinds":[""" +
            (1..ContentLimits.MAX_JSON_ELEMENTS).joinToString(",") { "\"k\"" } + "]}"
        assertNull(decodeGuarded<PackManifest>(many.encodeToByteArray(), "manifest.json", log))
        assertEquals(listOf("json-too-many-elements"), log.all.map { it.code })
    }

    @Test
    fun tooManyKindsAreRejectedEarly() {
        val log = ProblemLog()
        val kinds = ManifestCheck.validate(manifest(kinds = List(50) { "art-$it" }), log)
        assertEquals(emptyList(), kinds)
        assertEquals(listOf("kinds-too-many"), log.all.map { it.code })
    }

    @Test
    fun problemListStaysBounded() {
        val log = ProblemLog(max = 10)
        repeat(100) { log.warn("test", "irgendwo", "x") }
        // Die Gesamtgrenze gilt fuer beide Listen zusammen, nicht je Liste.
        assertTrue(log.all.size <= 11, "war ${log.all.size}")
        assertEquals("too-many-problems", log.all.last().code)
        assertTrue(log.all.all { it.detail.length <= 200 })
    }

    @Test
    fun meldungenBehaltenDieFundreihenfolge() {
        val log = ProblemLog()
        log.warn("erster-hinweis", "a", "x")
        log.fatal("fehler", "b", "x")
        log.warn("zweiter-hinweis", "c", "x")
        assertEquals(listOf("erster-hinweis", "fehler", "zweiter-hinweis"), log.all.map { it.code })
    }

    @Test
    fun vieleWarnungenVerdraengenKeinenFehler() {
        // Sonst bekommt der Paketautor eine Zaehlung statt der Fundstelle.
        val log = ProblemLog(max = 5)
        repeat(50) { log.warn("hinweis", "irgendwo", "x") }
        log.fatal("coords-invalid", "content/pois.json#poi-7", "91.0,12.0")
        assertTrue(log.hasFatal)
        val fehler = log.all.filter { it.severity == Severity.Fatal }
        assertTrue(fehler.any { it.code == "coords-invalid" && it.where.endsWith("poi-7") })
    }

    @Test
    fun verworfeneWarnungenSindKeineFehler() {
        // Ohne echten Fehler darf der Hinweis auf verworfene Warnungen kein
        // Fatal sein — sonst lehnt das Werkzeug ein Paket ab, das die App laedt.
        val log = ProblemLog(max = 5)
        repeat(50) { log.warn("hinweis", "irgendwo", "x") }
        assertTrue(!log.hasFatal)
        assertEquals(emptyList(), log.all.filter { it.severity == Severity.Fatal }.map { it.code })
    }

    @Test
    fun decodeGuardedFlagsOversizeAndDepth() {
        val log = ProblemLog()
        val big = ByteArray(ContentLimits.MAX_JSON_BYTES + 1)
        assertNull(decodeGuarded<PackManifest>(big, "manifest.json", log))
        val deep = ("[".repeat(100) + "]".repeat(100)).encodeToByteArray()
        assertNull(decodeGuarded<PackManifest>(deep, "manifest.json", log))
        assertEquals(listOf("json-too-large", "json-too-deep"), log.all.map { it.code })
    }
}
