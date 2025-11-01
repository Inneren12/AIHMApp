import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

plugins {
    // Root project intentionally has no plugins.
}

fun configureKotlinToolchain(project: Project, version: Int) {
    project.extensions.findByName("kotlin")?.let { extension ->
        val method = extension.javaClass.methods.firstOrNull { candidate ->
            candidate.name == "jvmToolchain" && candidate.parameterCount == 1 &&
                candidate.parameterTypes.firstOrNull() == Int::class.javaPrimitiveType
        }
        method?.invoke(extension, version)
    }
}

subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        configureKotlinToolchain(this@subprojects, 17)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        reports.junitXml.required.set(true)
    }
}
