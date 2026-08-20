import com.android.build.api.artifact.SingleArtifact

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "org.compasszero.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.compasszero"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    // Der Signaturschluessel liegt AUSSERHALB des Projektordners und wird ueber
    // eine Datei daneben gefunden. Zwei Gruende:
    //
    // 1. Nichts Geheimes kann versehentlich mitversioniert werden -- weder der
    //    Schluessel noch sein Passwort taucht irgendwo unter git auf.
    // 2. Wer das Projekt auscheckt, kann trotzdem bauen: Fehlt die Datei, wird
    //    NICHT abgebrochen, sondern unsigniert gebaut. Ein fehlender Schluessel
    //    ist kein Fehler, sondern der Normalfall fuer alle ausser dem Herausgeber.
    //
    // Der Schluessel ist unersetzlich: Geht er verloren, laesst sich eine einmal
    // veroeffentlichte App nie wieder aktualisieren -- Android verweigert jede
    // Version, die mit einem anderen Schluessel unterschrieben ist. Deshalb
    // gehoert er gesichert, nicht nur gespeichert.
    val schluesselOrdner = File(System.getProperty("user.home"), "compass-zero-signatur")
    val schluesselDatei = File(schluesselOrdner, "compass-zero.jks")
    val passwortDatei = File(schluesselOrdner, "passwort.txt")
    val signaturVorhanden = schluesselDatei.isFile && passwortDatei.isFile

    signingConfigs {
        if (signaturVorhanden) {
            create("herausgabe") {
                val passwort = passwortDatei.readText().trim()
                storeFile = schluesselDatei
                storePassword = passwort
                keyAlias = "compasszero"
                keyPassword = passwort
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (signaturVorhanden) {
                signingConfig = signingConfigs.getByName("herausgabe")
            }
        }
    }

    // Die Krypto-Bibliothek bringt Nachschlagetabellen fuer das
    // Post-Quanten-Signaturverfahren Picnic mit: drei Dateien, zusammen 1,2 MB
    // im APK und damit mehr als ein Viertel der ganzen App. Gebraucht wird aus
    // der Bibliothek ausschliesslich Ed25519 (core/security/Ed25519.kt, drei
    // Klassen). Ausgeschlossen werden nur diese Tabellen, nicht die Bibliothek
    // -- die Signaturpruefung ist das Kernstueck und wird nicht angetastet.
    packaging {
        resources {
            excludes += "/org/bouncycastle/pqc/crypto/picnic/*.properties"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
    sourceSets["test"].kotlin.srcDir("src/test/kotlin")

    // Der Stil besteht aus Farbwerten und Schriften; beide kommen aus
    // android.graphics und sind im reinen JVM-Test nur Attrappen. Ohne diese
    // Zeile wirft jeder Zugriff darauf, und die Zusagen des Stils liessen sich
    // gar nicht pruefen.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    // Das Inhaltspaket liegt signiert im APK. Erneutes Packen wuerde die
    // Signatur brechen, weil sie ueber die Bytes der Datei laeuft.
    androidResources {
        // Beide werden ueber wahlfreien Zugriff gelesen und dafuer einmal aus
        // den Beigaben herausgelegt. Komprimiert abgelegt waeren sie zweimal
        // im APK -- einmal gepackt, einmal ausgepackt im App-Ordner.
        noCompress += "czp"
        noCompress += "czk"
        noCompress += "czh"
    }
}

dependencies {
    implementation(project(":core:content"))
    implementation(project(":core:karte"))
    implementation(project(":core:transfer"))
    testImplementation(kotlin("test"))
}

// Was die App verlangen DARF. Geprueft wird das zusammengefuehrte Manifest und
// nicht die Quelldatei: eine Bibliothek kann eine Berechtigung mitbringen, ohne
// dass sie je in AndroidManifest.xml auftaucht. Diese Liste ist die
// Entscheidung, nicht das Manifest.
//
// KEINE ZEILE FUERS NETZ, und das bleibt so: android.permission.INTERNET steht
// hier nicht und darf hier nie stehen. Ohne sie kann die App auf
// Betriebssystemebene nicht ins Netz, und das ist von jedem nachpruefbar.
//
// Die drei Bluetooth-Rechte hat Max am 18.08.2026 einzeln entschieden, fuer den
// Paketaustausch von Geraet zu Geraet. Sie kosten die Aussage "keine einzige
// Berechtigung"; ausdruecklich NICHT dabei sind BLUETOOTH_SCAN,
// BLUETOOTH_ADVERTISE und jedes Ortungsrecht -- die App sucht keine Geraete und
// macht sich nicht sichtbar, gekoppelt wird in den Android-Einstellungen.
//
// Wer hier etwas ergaenzt, aendert eine Zusage der App und nicht eine
// Einstellung. Das ist der Sinn dieser Liste.
val ERLAUBTE_BERECHTIGUNGEN = setOf(
    "android.permission.BLUETOOTH",
    "android.permission.BLUETOOTH_ADMIN",
    "android.permission.BLUETOOTH_CONNECT",
)

abstract class PruefeBerechtigungen : DefaultTask() {

    @get:InputFile
    abstract val manifest: RegularFileProperty

    @get:Input
    abstract val erlaubt: SetProperty<String>

    @TaskAction
    fun pruefe() {
        val verlangt = Regex("""<uses-permission[^>]*android:name="([^"]+)"""")
            .findAll(manifest.get().asFile.readText())
            .map { it.groupValues[1] }
            .toSet()
        val ungewollt = verlangt - erlaubt.get()
        if (ungewollt.isNotEmpty()) {
            throw GradleException(
                "Das zusammengefuehrte Manifest verlangt Berechtigungen, die hier nicht " +
                    "vorgesehen sind: ${ungewollt.sorted().joinToString(", ")}",
            )
        }
    }
}

androidComponents {
    onVariants { variante ->
        val name = variante.name.replaceFirstChar { it.uppercase() }
        val pruefung = tasks.register<PruefeBerechtigungen>("pruefe${name}Berechtigungen") {
            manifest.set(variante.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            erlaubt.set(ERLAUBTE_BERECHTIGUNGEN)
        }
        // Die Aufgaben je Variante entstehen erst nach diesem Block; deshalb
        // wird die Abhaengigkeit eingehaengt, sobald es sie gibt.
        tasks.matching { it.name == "assemble$name" || it.name == "check" }
            .configureEach { dependsOn(pruefung) }
    }
}

// Das signierte Inhaltspaket wird vor jedem App-Bau aus work/build/ in die
// Assets kopiert.
//
// WARUM ALS AUFGABE UND NICHT VON HAND: Bis zum 29.07.2026 stand dieser
// Schritt nur als Merksatz auf Papier. Er wurde zweimal vergessen, und
// beide Male zeigte der Emulator einen alten Inhaltsstand, waehrend Tests und
// Signatur gruen waren -- die schlimmste Art von Fehler, weil die Pruefung
// gelingt und trotzdem das Falsche geprueft wird.
//
// Fehlt work/build/europe-de.czp (etwa nach einem frischen Auschecken), wird
// NICHT abgebrochen: Der Bau laeuft mit dem vorhandenen Asset weiter und meldet
// das nur. Sonst waere ein App-Bau ohne vorheriges Packen unmoeglich.
val paketQuelle = rootProject.layout.projectDirectory.file("work/build/europe-de.czp")
val paketZiel = layout.projectDirectory.file("src/main/assets/europe-de.czp")

val paketUebernehmen = tasks.register("paketUebernehmen") {
    val quelle = paketQuelle.asFile
    val ziel = paketZiel.asFile
    // Die Quelle wird nur angemeldet, WENN es sie gibt. Frueher stand hier
    // inputs.file(quelle).optional(true) -- das reicht seit Gradle 8 nicht mehr:
    // Eine benannte, aber fehlende Eingabedatei ist dort ein Fehler
    // ("An input file was expected to be present but it doesn't exist"), und
    // optional(true) faengt nur den Fall ab, dass gar keine benannt wurde.
    // Damit brach genau der Fall ab, den der Kommentar oben ausschliesst: ein
    // frisch geklontes Verzeichnis, in dem work/ noch leer ist. Gefunden am
    // 17.08.2026 beim Bauversuch aus einem frischen Klon, wo alle drei
    // Uebernahme-Aufgaben scheiterten.
    inputs.files(provider { if (quelle.exists()) listOf(quelle) else emptyList<java.io.File>() })
    outputs.file(ziel)
    doLast {
        if (!quelle.exists()) {
            logger.lifecycle(
                "Kein frisch signiertes Paket unter ${quelle.path} -- es bleibt beim " +
                    "vorhandenen Asset. Zum Erneuern: packsign pack/sign ausfuehren.",
            )
            return@doLast
        }
        if (ziel.exists() && quelle.readBytes().contentEquals(ziel.readBytes())) return@doLast
        quelle.copyTo(ziel, overwrite = true)
        logger.lifecycle("Inhaltspaket uebernommen: ${quelle.length()} Bytes")
    }
}

// Dieselbe Uebernahme fuer die Karte, aus demselben Grund: Wer die Karte neu
// baut und danach nur assembleDebug aufruft, bekaeme sonst die vorige.
val karteQuelle = rootProject.layout.projectDirectory.file("work/karte/oesterreich-ueberblick.czk")
val karteZiel = layout.projectDirectory.file("src/main/assets/karte.czk")

val karteUebernehmen = tasks.register("karteUebernehmen") {
    val quelle = karteQuelle.asFile
    val ziel = karteZiel.asFile
    // Die Quelle wird nur angemeldet, WENN es sie gibt. Frueher stand hier
    // inputs.file(quelle).optional(true) -- das reicht seit Gradle 8 nicht mehr:
    // Eine benannte, aber fehlende Eingabedatei ist dort ein Fehler
    // ("An input file was expected to be present but it doesn't exist"), und
    // optional(true) faengt nur den Fall ab, dass gar keine benannt wurde.
    // Damit brach genau der Fall ab, den der Kommentar oben ausschliesst: ein
    // frisch geklontes Verzeichnis, in dem work/ noch leer ist. Gefunden am
    // 17.08.2026 beim Bauversuch aus einem frischen Klon, wo alle drei
    // Uebernahme-Aufgaben scheiterten.
    inputs.files(provider { if (quelle.exists()) listOf(quelle) else emptyList<java.io.File>() })
    outputs.file(ziel)
    doLast {
        if (!quelle.exists()) {
            logger.lifecycle(
                "Keine frisch gebaute Karte unter ${quelle.path} -- es bleibt beim " +
                    "vorhandenen Asset. Zum Erneuern: tools/karte/bauen.py ausfuehren.",
            )
            return@doLast
        }
        if (ziel.exists() && quelle.readBytes().contentEquals(ziel.readBytes())) return@doLast
        quelle.copyTo(ziel, overwrite = true)
        logger.lifecycle("Karte uebernommen: ${quelle.length()} Bytes")
    }
}

// AN preBuild UND NICHT AN mergeAssets. Haengt die Uebernahme nur am
// Zusammenfuehren der Assets, bricht der Bau mit "uses this output of task
// without declaring an explicit or implicit dependency" ab: Auch die
// Lint-Aufgaben lesen den Assets-Ordner, und sie laufen nicht ueber
// mergeAssets. preBuild liegt vor allen Aufgaben der Android-Kette.
val hoehenQuelle = rootProject.layout.projectDirectory.file("work/karte/oesterreich-hoehen.czh")
val hoehenZiel = layout.projectDirectory.file("src/main/assets/hoehen.czh")

val hoehenUebernehmen = tasks.register("hoehenUebernehmen") {
    val quelle = hoehenQuelle.asFile
    val ziel = hoehenZiel.asFile
    // Die Quelle wird nur angemeldet, WENN es sie gibt. Frueher stand hier
    // inputs.file(quelle).optional(true) -- das reicht seit Gradle 8 nicht mehr:
    // Eine benannte, aber fehlende Eingabedatei ist dort ein Fehler
    // ("An input file was expected to be present but it doesn't exist"), und
    // optional(true) faengt nur den Fall ab, dass gar keine benannt wurde.
    // Damit brach genau der Fall ab, den der Kommentar oben ausschliesst: ein
    // frisch geklontes Verzeichnis, in dem work/ noch leer ist. Gefunden am
    // 17.08.2026 beim Bauversuch aus einem frischen Klon, wo alle drei
    // Uebernahme-Aufgaben scheiterten.
    inputs.files(provider { if (quelle.exists()) listOf(quelle) else emptyList<java.io.File>() })
    outputs.file(ziel)
    doLast {
        if (!quelle.exists()) {
            logger.lifecycle("Keine Hoehendatei unter ${quelle.path} -- die Karte bleibt flach.")
            return@doLast
        }
        if (ziel.exists() && quelle.readBytes().contentEquals(ziel.readBytes())) return@doLast
        quelle.copyTo(ziel, overwrite = true)
        logger.lifecycle("Hoehendatei uebernommen: ${quelle.length()} Bytes")
    }
}

tasks.named("preBuild") {
    dependsOn(paketUebernehmen)
    dependsOn(karteUebernehmen)
    dependsOn(hoehenUebernehmen)
}
