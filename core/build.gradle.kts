plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.export)
    implementation(projects.logging)

    testImplementation(libs.junit)
}
