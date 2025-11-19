plugins {
    kotlin("jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    sourceSets {
        val test by getting {
            kotlin.setSrcDirs(listOf("src/test/kotlin"))
        }
    }
}

dependencies {
    implementation(projects.export)
    implementation(projects.logging)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}
