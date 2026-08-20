package org.compasszero.content

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal val PackJson = Json {
    ignoreUnknownKeys = true
}

internal inline fun <reified T> decodeGuarded(bytes: ByteArray, where: String, problems: ProblemLog): T? {
    if (bytes.size > ContentLimits.MAX_JSON_BYTES) {
        problems.fatal("json-too-large", where, "${bytes.size} bytes")
        return null
    }
    // Streng dekodieren: kaputte Bytes wuerden sonst still zu Ersatzzeichen und
    // mitten im Ueberlebenstext stehen, ohne dass jemand es merkt.
    val text = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        problems.fatal("json-not-utf8", where, "invalid byte sequence")
        return null
    }
    JsonShape.problem(text)?.let { (code, detail) ->
        problems.fatal(code, where, detail)
        return null
    }
    return try {
        PackJson.decodeFromString<T>(text)
    } catch (e: SerializationException) {
        problems.fatal("json-invalid", where, e.message?.take(300) ?: "unreadable")
        null
    } catch (e: IllegalArgumentException) {
        problems.fatal("json-invalid", where, e.message?.take(300) ?: "unreadable")
        null
    }
}
