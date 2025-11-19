plugins {
    kotlin("jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(projects.export)
    implementation(projects.logging)

    testImplementation(libs.junit)
}

tasks.test {
    useJUnitPlatform()
}
