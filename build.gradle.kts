plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
}

// Eine leere Kotlin-Datei ist gueltiger Kotlin. Sie kompiliert fehlerfrei und
// enthaelt keinen einzigen Test -- der Build bleibt gruen, waehrend die
// Absicherung weg ist.
//
// Am 28.07.2026 ist genau das passiert: Ein Bearbeitungsschritt hat
// EuropeDePaketTest.kt und QuerverweiseTest.kt auf 0 Byte gekuerzt (eine Datei
// zum Schreiben oeffnen leert sie, bevor der Lesevorgang laeuft). Beide
// bewachen das echte Auslieferungspaket -- die Kernzahlen der Wiederbelebung
// und saemtliche Querverweise zwischen den Tipps. Aufgefallen ist es nur, weil
// die Testanzahl nachgezaehlt wurde.
//
// Diese Pruefung haengt vor jedem Bauen und faellt bei jeder leeren Quelldatei.
val keineLeerenQuelldateien =
    tasks.register("keineLeerenQuelldateien") {
        group = "verification"
        description = "Faellt, wenn eine Kotlin-Quelldatei leer ist (stiller Verlust von Tests)."
        val quellen = fileTree(rootDir) {
            include("**/src/**/*.kt")
            exclude("**/build/**")
        }
        inputs.files(quellen)
        doLast {
            val leer = quellen.files.filter { it.length() == 0L }.sorted()
            check(leer.isEmpty()) {
                "Leere Kotlin-Dateien gefunden — sie kompilieren, enthalten aber nichts:\n" +
                    leer.joinToString("\n") { "  " + it.relativeTo(rootDir) }
            }
        }
    }

subprojects {
    tasks.matching { it.name == "check" }.configureEach { dependsOn(keineLeerenQuelldateien) }
}
