pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "compass-zero"

include(":core:security")
include(":core:content")
include(":core:transfer")
include(":core:karte")
include(":tools:packsign")
include(":androidApp")
