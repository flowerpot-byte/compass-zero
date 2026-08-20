plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvmToolchain(21)
    jvm {
        testRuns["test"].executionTask.configure {
            maxHeapSize = "96m"
            // Der Funktest faehrt das echte Inhaltspaket aus dem Repo durch.
            systemProperty("compasszero.repoRoot", rootDir.absolutePath)
        }
    }
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:security"))
            implementation(project(":core:content"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // Der Ende-zu-Ende-Test faehrt ein echtes signiertes Paket durch den
        // Empfangsweg und laesst es danach reguler pruefen.
        jvmTest.dependencies {
            implementation(project(":core:security"))
            implementation(project(":core:content"))
        }
    }
}
