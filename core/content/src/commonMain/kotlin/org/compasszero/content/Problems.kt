package org.compasszero.content

enum class Severity { Fatal, Warning }

class PackProblem(
    val severity: Severity,
    val code: String,
    val where: String,
    val detail: String,
) {
    override fun toString(): String = "$severity[$code] $where: $detail"
}

internal class ProblemLog(max: Int = ContentLimits.MAX_PROBLEMS) {

    // Getrennte Listen, damit viele Warnungen keine fatale Meldung verdraengen:
    // sonst bekaeme der Paketautor eine Zaehlung statt des Fundstuecks. Die
    // Fundreihenfolge bleibt ueber die mitgezaehlte Nummer erhalten.
    private val maxFatal = max * 3 / 5
    private val maxWarning = max - maxFatal
    private val fatals = ArrayList<Pair<Int, PackProblem>>()
    private val warnings = ArrayList<Pair<Int, PackProblem>>()
    private var naechste = 0
    private var fatalSeen = false
    private var droppedFatal = 0
    private var droppedWarning = 0
    private var letzteFatal = 0
    private var letzteWarnung = 0

    val all: List<PackProblem>
        get() {
            val gesammelt = ArrayList<Pair<Int, PackProblem>>(fatals.size + warnings.size + 2)
            gesammelt.addAll(fatals)
            gesammelt.addAll(warnings)
            if (droppedFatal > 0) {
                gesammelt.add(
                    letzteFatal to
                        PackProblem(Severity.Fatal, "too-many-problems", "pack", "$droppedFatal weitere Fehler nicht angezeigt"),
                )
            }
            if (droppedWarning > 0) {
                // Nur Hinweise verworfen: der Vermerk darf selbst kein Fehler sein,
                // sonst lehnt das Werkzeug ein Paket ab, das die App laedt.
                gesammelt.add(
                    letzteWarnung to
                        PackProblem(Severity.Warning, "too-many-problems", "pack", "$droppedWarning weitere Hinweise nicht angezeigt"),
                )
            }
            return gesammelt.sortedBy { it.first }.map { it.second }
        }

    val hasFatal: Boolean get() = fatalSeen

    fun fatal(code: String, where: String, detail: String) {
        fatalSeen = true
        if (fatals.size >= maxFatal) {
            droppedFatal++
            letzteFatal = naechste++
            return
        }
        fatals.add(naechste++ to PackProblem(Severity.Fatal, code, where, detail.take(200)))
    }

    fun warn(code: String, where: String, detail: String) {
        if (warnings.size >= maxWarning) {
            droppedWarning++
            letzteWarnung = naechste++
            return
        }
        warnings.add(naechste++ to PackProblem(Severity.Warning, code, where, detail.take(200)))
    }
}
