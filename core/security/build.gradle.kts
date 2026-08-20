plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(21)
    jvm {
        testRuns["test"].executionTask.configure {
            maxHeapSize = "96m"
        }
    }
    androidTarget()
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
    sourceSets {
        // Android und die JVM teilen denselben Unterbau: java.io, java.security,
        // java.util.zip und Bouncy Castle gibt es auf beiden. Getrennt bleibt
        // nur, was auf dem Geraet niemand braucht -- siehe jvmMain.
        val jvmShared by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.bouncycastle.provider)
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
    namespace = "org.compasszero.security"
    compileSdk = 35
    defaultConfig {
        // Auf dem Geraet wird nur gelesen und geprueft. Das Schreiben von
        // Paketen (PackWriter, java.nio.file) bleibt in jvmMain -- sonst
        // haenge die App an Android 8 aufwaerts, und die Zielgruppe hat
        // aeltere Geraete.
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
