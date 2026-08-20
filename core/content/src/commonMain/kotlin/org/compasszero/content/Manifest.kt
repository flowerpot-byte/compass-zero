package org.compasszero.content

import kotlinx.serialization.Serializable

@Serializable
class PackManifest(
    val schema: Int,
    val id: String,
    val version: Int,
    val language: String,
    val title: String,
    val created: Long = 0,
    val kinds: List<String>,
)

internal object ManifestCheck {

    val KNOWN_KINDS = setOf("tips", "guides", "agriculture", "pois", "phrases", "terms")

    fun validate(manifest: PackManifest, problems: ProblemLog): List<String> {
        val where = "manifest.json"
        if (manifest.schema != 1) {
            problems.fatal("schema-unsupported", where, "schema ${manifest.schema}")
        }
        if (manifest.id.length > ContentLimits.MAX_PACK_ID_LENGTH ||
            !ContentLimits.PACK_ID_PATTERN.matches(manifest.id)
        ) {
            problems.fatal("id-invalid", where, manifest.id.take(140))
        }
        if (manifest.version < 1) {
            problems.fatal("version-invalid", where, "version ${manifest.version}")
        }
        if (!ContentLimits.LANGUAGE_PATTERN.matches(manifest.language)) {
            problems.fatal("language-invalid", where, manifest.language.take(40))
        }
        if (manifest.title.length > ContentLimits.MAX_TITLE_LENGTH || !Texts.isUsable(manifest.title, mindestens = 1)) {
            problems.fatal("title-invalid", where, "unlesbar oder laenger als ${ContentLimits.MAX_TITLE_LENGTH}")
        }
        if (manifest.created < 0) {
            problems.fatal("created-invalid", where, "${manifest.created}")
        }
        if (manifest.kinds.size > ContentLimits.MAX_KINDS) {
            problems.fatal("kinds-too-many", where, "${manifest.kinds.size}")
            return emptyList()
        }
        if (manifest.kinds.size != manifest.kinds.distinct().size) {
            problems.fatal("kinds-duplicate", where, manifest.kinds.joinToString(",").take(120))
        }
        val known = manifest.kinds.distinct().filter { kind ->
            val ok = kind in KNOWN_KINDS
            if (!ok) problems.warn("kind-unknown", where, kind.take(40))
            ok
        }
        if (known.isEmpty()) {
            problems.fatal("kinds-empty", where, "no usable content kind")
        }
        return known
    }
}
