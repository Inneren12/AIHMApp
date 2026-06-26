plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)

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
