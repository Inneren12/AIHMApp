import org.gradle.api.Project

plugins {
    // Reserved for future root plugins.
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
        configureKotlinToolchain(this@subprojects, 21)
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        configureKotlinToolchain(this@subprojects, 21)
    }
}
