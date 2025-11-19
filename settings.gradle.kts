import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "AiHandMadeApp"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":sanity")
include(":core")
include(":export")
include(":logging")
include(":storage")
include(":app")
