package org.compasszero.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PoisTest {

    private fun poi(
        id: String = "poi-1",
        kind: String = "water",
        lat: Double = 47.5,
        lon: Double = 12.9,
        name: String = "Beispielquelle",
        note: String = "",
        elevation: Int? = 800,
    ) = Poi(id, kind, lat, lon, name, note, elevation)

    private fun run(vararg pois: Poi, schema: Int = 1, attribution: String = "Beispiel-Datensatz"): Pair<List<Poi>, List<PackProblem>> {
        val log = ProblemLog()
        val kept = PoisCheck.validate(PoisFile(schema, attribution, pois.toList()), log)
        return kept to log.all
    }

    @Test
    fun validPoiIsKept() {
        val (kept, problems) = run(poi())
        assertEquals(1, kept.size)
        assertEquals(emptyList(), problems)
    }

    @Test
    fun allKnownKindsPass() {
        val pois = PoisCheck.KNOWN_KINDS.mapIndexed { i, kind -> poi(id = "poi-$i", kind = kind) }
        val (kept, problems) = run(*pois.toTypedArray())
        assertEquals(PoisCheck.KNOWN_KINDS.size, kept.size)
        assertEquals(emptyList(), problems)
    }

    @Test
    fun unknownKindIsSkippedWithWarning() {
        val (kept, problems) = run(poi(), poi(id = "poi-2", kind = "zukunftsart"))
        assertEquals(listOf("poi-1"), kept.map { it.id })
        assertEquals(listOf("kind-unknown"), problems.map { it.code })
        assertTrue(problems.single().severity == Severity.Warning)
    }

    @Test
    fun brokenCoordinatesAreFatal() {
        assertTrue(run(poi(lat = 91.0)).second.any { it.code == "coords-invalid" })
        assertTrue(run(poi(lat = -91.0)).second.any { it.code == "coords-invalid" })
        assertTrue(run(poi(lon = 180.5)).second.any { it.code == "coords-invalid" })
        assertTrue(run(poi(lat = Double.NaN)).second.any { it.code == "coords-invalid" })
        assertTrue(run(poi(lon = Double.POSITIVE_INFINITY)).second.any { it.code == "coords-invalid" })
    }

    @Test
    fun attributionIsMandatory() {
        val (_, problems) = run(poi(), attribution = " ")
        assertTrue(problems.any { it.code == "attribution-invalid" })
    }

    @Test
    fun boundsAndDuplicates() {
        assertTrue(run(poi(), poi()).second.any { it.code == "id-duplicate" })
        assertTrue(run(poi(name = "x".repeat(ContentLimits.MAX_NAME_LENGTH + 1))).second.any { it.code == "name-invalid" })
        assertTrue(run(poi(note = "x".repeat(ContentLimits.MAX_NOTE_LENGTH + 1))).second.any { it.code == "note-invalid" })
        assertTrue(run(poi(elevation = 9500)).second.any { it.code == "elevation-invalid" })
        assertTrue(run(poi(elevation = -12000)).second.any { it.code == "elevation-invalid" })
    }

    @Test
    fun poiCountLimitIsEnforced() {
        val zuViele = (0..ContentLimits.MAX_POIS).map { poi(id = "poi-$it") }
        val (kept, problems) = run(*zuViele.toTypedArray())
        assertEquals(emptyList(), kept)
        assertTrue(problems.any { it.code == "too-many-items" })
    }

    @Test
    fun poiWithBrokenIdIsNotKept() {
        val (kept, problems) = run(poi(), poi(id = "Kaputte ID"))
        assertEquals(listOf("poi-1"), kept.map { it.id })
        assertTrue(problems.any { it.code == "id-invalid" })
    }

    @Test
    fun schemaGate() {
        val (kept, problems) = run(poi(), schema = 2)
        assertEquals(emptyList<Poi>(), kept)
        assertEquals(listOf("schema-unsupported"), problems.map { it.code })
    }

    @Test
    fun poiNameUndNotizMuessenLesbarSein() {
        val unsichtbar = 0x200B.toChar().toString().repeat(3)
        assertTrue(run(poi(name = unsichtbar)).second.any { it.code == "name-invalid" })
        assertTrue(run(poi(note = unsichtbar)).second.any { it.code == "note-invalid" })
        assertTrue(run(poi(name = "Quelle" + 0x0007.toChar())).second.any { it.code == "name-invalid" })
        assertEquals(emptyList(), run(poi(name = "Quelle am Weg", note = "ganzjaehrig")).second)
    }
}
