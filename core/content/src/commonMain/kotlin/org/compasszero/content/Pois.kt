package org.compasszero.content

import kotlinx.serialization.Serializable

@Serializable
class Poi(
    val id: String,
    val kind: String,
    val lat: Double,
    val lon: Double,
    val name: String = "",
    val note: String = "",
    val elevation: Int? = null,
)

@Serializable
class PoisFile(val schema: Int, val attribution: String, val pois: List<Poi>)

internal object PoisCheck {

    val KNOWN_KINDS = setOf("water", "viewpoint", "waypoint", "shelter")

    // Hoehenangaben ausserhalb der realen Erdspanne sind Datenfehler.
    private val ELEVATION_RANGE = -500..9000

    fun validate(file: PoisFile, problems: ProblemLog): List<Poi> {
        val where = "content/pois.json"
        if (file.schema != 1) {
            problems.fatal("schema-unsupported", where, "schema ${file.schema}")
            return emptyList()
        }
        if (file.attribution.length > ContentLimits.MAX_NOTE_LENGTH || !Texts.isUsable(file.attribution)) {
            problems.fatal("attribution-invalid", where, "poi data needs a usable attribution")
        }
        if (file.pois.size > ContentLimits.MAX_POIS) {
            problems.fatal("too-many-items", where, "${file.pois.size}")
            return emptyList()
        }
        Checks.uniqueIds(file.pois.map { it.id }, where, problems)
        val kept = ArrayList<Poi>(file.pois.size)
        for (poi in file.pois) {
            val at = "$where#${poi.id.take(100)}"
            if (poi.id.length > ContentLimits.MAX_ID_LENGTH || !ContentLimits.ID_PATTERN.matches(poi.id)) {
                problems.fatal("id-invalid", at, poi.id.take(100))
                continue
            }
            if (poi.kind !in KNOWN_KINDS) {
                problems.warn("kind-unknown", at, poi.kind.take(40))
                continue
            }
            if (!poi.lat.isFinite() || !poi.lon.isFinite() ||
                poi.lat < -90.0 || poi.lat > 90.0 || poi.lon < -180.0 || poi.lon > 180.0
            ) {
                problems.fatal("coords-invalid", at, "${poi.lat},${poi.lon}")
                continue
            }
            // Ein Punkt mit unsichtbarem Namen waere auf der Karte ein stummer
            // Eintrag, den niemand deuten kann.
            // Der Name eines Ortes darf ein Zeichen lang sein -- eine Quelle in
            // Japan heisst schlicht so.
            Checks.optionalText(poi.name, ContentLimits.MAX_NAME_LENGTH, "name-invalid", at, problems, mindestens = 1)
            Checks.optionalText(poi.note, ContentLimits.MAX_NOTE_LENGTH, "note-invalid", at, problems)
            if (poi.elevation != null && poi.elevation !in ELEVATION_RANGE) {
                problems.fatal("elevation-invalid", at, "${poi.elevation}")
            }
            kept.add(poi)
        }
        return kept
    }
}
