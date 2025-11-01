plugins {
    alias(libs.plugins.kotlin.jvm)
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
