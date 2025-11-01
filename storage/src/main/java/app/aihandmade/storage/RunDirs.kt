package app.aihandmade.storage

import java.io.File

/**
 * Representation of the generated run directory structure.
 */
data class RunDir(
    val root: File,
    val eventsFile: File,
    val manifestFile: File,
    val artifactsDir: File,
)

/**
 * Ensures the project directory exists under the provided root and returns it.
 */
fun createProject(rootDir: File, projectId: String): File {
    val projectsDir = rootDir.resolve(Paths.PROJECTS)
    ensureDirectory(projectsDir)

    val projectDir = projectsDir.resolve(projectId)
    ensureDirectory(projectDir)

    return projectDir
}

/**
 * Ensures the run directory structure for the given run id exists and returns the created files.
 */
fun createRun(projectDir: File, runId: String): RunDir {
    val runsDir = projectDir.resolve(Paths.RUNS)
    ensureDirectory(runsDir)

    val runDir = runsDir.resolve(runId)
    ensureDirectory(runDir)

    val eventsFile = ensureFile(runDir.resolve(Paths.EVENTS))
    val manifestFile = ensureFile(runDir.resolve(Paths.MANIFEST))
    val artifactsDir = runDir.resolve(Paths.ARTIFACTS)
    ensureDirectory(artifactsDir)

    return RunDir(
        root = runDir,
        eventsFile = eventsFile,
        manifestFile = manifestFile,
        artifactsDir = artifactsDir,
    )
}

private fun ensureDirectory(directory: File) {
    if (!directory.exists()) {
        directory.mkdirs()
    }
}

private fun ensureFile(file: File): File {
    if (!file.exists()) {
        file.parentFile?.let(::ensureDirectory)
        file.createNewFile()
    }
    return file
}
