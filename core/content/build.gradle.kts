plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(21)
    jvm {
        testRuns["test"].executionTask.configure {
            maxHeapSize = "96m"
            systemProperty("compasszero.repoRoot", rootDir.absolutePath)
            // Die Tests lesen das echte Inhaltspaket ueber die Eigenschaft
            // darueber. Gradle kann das nicht sehen und haelt den Task nach
            // einer reinen Inhaltsaenderung sonst fuer aktuell -- er meldet
            // dann gruen, ohne einen einzigen Test gestartet zu haben.
            inputs.dir(rootDir.resolve("content"))
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
    }
    androidTarget()
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        // Dieselben Quellen fuer JVM und Android: Paketleser und
        // Textaufbereitung der Suche brauchen nur java.io und java.text.
        val jvmShared by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(project(":core:security"))
            }
        }
        jvmMain.get().dependsOn(jvmShared)
        androidMain.get().dependsOn(jvmShared)
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "org.compasszero.content"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
