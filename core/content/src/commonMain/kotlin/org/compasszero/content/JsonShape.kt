package org.compasszero.content

class ShapeLimits(
    val maxDepth: Int = ContentLimits.MAX_JSON_DEPTH,
    val maxElements: Int = ContentLimits.MAX_JSON_ELEMENTS,
) {
    companion object {
        val DEFAULT = ShapeLimits()
    }
}

// Prueft die Gestalt einer JSON-Datei, bevor der Parser sie anfasst. Grund: alle
// inhaltlichen Mengengrenzen greifen erst nach dem Dekodieren, und bis dahin ist
// der Speicher schon voll. Eine 8-MiB-Datei mit Millionen winziger Elemente
// braucht ueber hundert Megabyte Heap — auf alten Geraeten ein sofortiger Absturz.
// Dieser Durchlauf kostet nur einen Zeichendurchgang ohne Speicherbedarf.
internal object JsonShape {

    // Liefert Fehlercode und Detail, oder null wenn die Gestalt tragbar ist.
    fun problem(text: String, limits: ShapeLimits = ShapeLimits.DEFAULT): Pair<String, String>? {
        var depth = 0
        var elements = 0
        var inString = false
        var escaped = false
        var stringStart = -1
        var lastString: String? = null
        val keysPerLevel = ArrayList<HashSet<String>>()
        val isObject = ArrayList<Boolean>()

        for (i in text.indices) {
            val c = text[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> {
                        inString = false
                        lastString = text.substring(stringStart, i)
                    }
                }
                continue
            }
            when (c) {
                '"' -> {
                    inString = true
                    stringStart = i + 1
                    elements++
                }
                '{', '[' -> {
                    depth++
                    elements++
                    if (depth > limits.maxDepth) {
                        return "json-too-deep" to "nesting deeper than ${limits.maxDepth}"
                    }
                    keysPerLevel.add(HashSet())
                    isObject.add(c == '{')
                    lastString = null
                }
                '}', ']' -> {
                    if (depth == 0) return "json-invalid" to "unbalanced brackets"
                    // Der schliessende Klammertyp muss zum oeffnenden passen, sonst
                    // zaehlt der Durchlauf mit einem Zustand weiter, den es nicht gibt.
                    if (isObject.last() != (c == '}')) return "json-invalid" to "mismatched brackets"
                    depth--
                    keysPerLevel.removeAt(keysPerLevel.size - 1)
                    isObject.removeAt(isObject.size - 1)
                    lastString = null
                }
                ':' -> {
                    val key = lastString
                    if (key != null && isObject.isNotEmpty() && isObject.last()) {
                        // Verglichen wird der entschluesselte Name. Sonst gelten
                        // "body" und "body" als verschiedene Schluessel — der
                        // Parser sieht aber denselben und nimmt den letzten Wert.
                        // Genau damit liesse sich ein gepruefter Text umdeuten.
                        val entschluesselt = unescape(key)
                            ?: return "json-invalid" to "broken escape in key"
                        if (!keysPerLevel.last().add(entschluesselt)) {
                            return "json-duplicate-key" to entschluesselt.take(80)
                        }
                    }
                    lastString = null
                }
                ',' -> {
                    elements++
                    lastString = null
                }
                else -> if (!c.isWhitespace()) lastString = null
            }
            if (elements > limits.maxElements) {
                return "json-too-many-elements" to "more than ${limits.maxElements} elements"
            }
        }

        if (inString) return "json-invalid" to "unterminated string"
        if (depth != 0) return "json-invalid" to "unbalanced brackets"
        return null
    }

    // Loest die JSON-Maskierungen auf. null bedeutet: kaputte Maskierung, der
    // Parser wuerde die Datei ohnehin ablehnen.
    private fun unescape(raw: String): String? {
        if ('\\' !in raw) return raw
        val out = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '\\') {
                out.append(c)
                i++
                continue
            }
            if (i + 1 >= raw.length) return null
            when (val next = raw[i + 1]) {
                '"', '\\', '/' -> { out.append(next); i += 2 }
                'b' -> { out.append(8.toChar()); i += 2 }
                'f' -> { out.append(12.toChar()); i += 2 }
                'n' -> { out.append('\n'); i += 2 }
                'r' -> { out.append('\r'); i += 2 }
                't' -> { out.append('\t'); i += 2 }
                'u' -> {
                    if (i + 5 >= raw.length) return null
                    // Von Hand pruefen: die Zahlenumwandlung der Standardbibliothek
                    // schluckt Vorzeichen und fremde Ziffernzeichen, die kein
                    // JSON-Parser annimmt.
                    var code = 0
                    for (k in 0 until 4) {
                        val ziffer = hexWert(raw[i + 2 + k]) ?: return null
                        code = (code shl 4) or ziffer
                    }
                    out.append(code.toChar())
                    i += 6
                }
                else -> return null
            }
        }
        return out.toString()
    }

    private fun hexWert(c: Char): Int? = when (c) {
        in '0'..'9' -> c - '0'
        in 'a'..'f' -> c - 'a' + 10
        in 'A'..'F' -> c - 'A' + 10
        else -> null
    }
}
