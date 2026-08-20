plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(21)
    jvm {
        testRuns["test"].executionTask.configure {
            maxHeapSize = "96m"
            systemProperty("compasszero.repoRoot", rootDir.absolutePath)
        }
    }
    androidTarget()
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
    sourceSets {
        // Dieselben Quellen fuer JVM und Android: die Kartendatei wird ueber
        // java.io gelesen und mit java.util.zip entpackt, mehr braucht es
        // nicht.
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
    namespace = "org.compasszero.karte"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
