package org.compasszero.security

object PackNames {

    const val MAX_LENGTH = 200
    private val PATTERN = Regex("[A-Za-z0-9._/-]+")

    fun problem(name: String): String? {
        if (name.length > MAX_LENGTH) return "entry name too long"
        if (!PATTERN.matches(name)) return "forbidden characters in entry name"
        val segments = name.split('/')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) {
            return "entry name is not a plain relative path: $name"
        }
        // Jeder Teil muss mit Buchstabe oder Ziffer beginnen. Damit fallen Namen
        // ohne Dateinamen (".json") und versteckte Dateien (".gitkeep") heraus —
        // sie entstehen nicht beim Zusammenstellen von Inhalten, sondern beim
        // Basteln am Paket.
        if (segments.any { !it[0].isLetterOrDigit() }) {
            return "entry name segment must start with a letter or digit: $name"
        }
        val allowed = name == "manifest.json" || name.startsWith("content/") || name.startsWith("assets/")
        if (!allowed) return "entry outside allowed folders: $name"
        return null
    }
}
