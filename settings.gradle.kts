pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.0.20"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            version("kotlin", "2.0.20")
            version("junit-jupiter", "5.10.2")
            version("compose-bom", "2024.09.02")
            version("compose-compiler", "1.7.0")
            version("androidx-activity-compose", "1.9.2")
            version("androidx-navigation-compose", "2.8.0")
            version("androidx-lifecycle", "2.8.6")
            version("kotlinx-coroutines", "1.9.0")

            plugin("android.application", "com.android.application").version("8.5.2")
            plugin("kotlin.android", "org.jetbrains.kotlin.android").versionRef("kotlin")
            plugin("kotlin.compose", "org.jetbrains.kotlin.plugin.compose").versionRef("kotlin")
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlin-serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")

            library("junit-jupiter-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit-jupiter")
            library("junit-jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit-jupiter")
            library("kotlinx-serialization-json", "org.jetbrains.kotlinx", "kotlinx-serialization-json").version("1.7.3")
            library("junit", "org.junit.vintage", "junit-vintage-engine").versionRef("junit-jupiter")
            library("androidx.compose.bom", "androidx.compose", "compose-bom").versionRef("compose-bom")
            library("androidx.compose.ui", "androidx.compose.ui", "ui").withoutVersion()
            library("androidx.compose.material3", "androidx.compose.material3", "material3").withoutVersion()
            library("androidx.compose.ui.tooling", "androidx.compose.ui", "ui-tooling").withoutVersion()
            library("androidx.compose.foundation", "androidx.compose.foundation", "foundation").withoutVersion()
            library("androidx.activity.compose", "androidx.activity", "activity-compose").versionRef("androidx-activity-compose")
            library("androidx.navigation.compose", "androidx.navigation", "navigation-compose").versionRef("androidx-navigation-compose")
            library("androidx.lifecycle.viewmodel.ktx", "androidx.lifecycle", "lifecycle-viewmodel-ktx").versionRef("androidx-lifecycle")
            library("androidx.lifecycle.viewmodel.compose", "androidx.lifecycle", "lifecycle-viewmodel-compose").versionRef("androidx-lifecycle")
            library("androidx.exifinterface", "androidx.exifinterface", "exifinterface").version("1.3.7")
            library("kotlinx.coroutines.test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef("kotlinx-coroutines")
        }
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
