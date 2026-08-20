plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

// Der Kettentest packt und signiert das echte Inhaltspaket aus dem Repo.
tasks.test {
    systemProperty("compasszero.repoRoot", rootDir.absolutePath)
}

dependencies {
    implementation(project(":core:security"))
    implementation(project(":core:content"))
    implementation(project(":core:karte"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.compasszero.packsign.MainKt")
}
