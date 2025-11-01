package app.aihandmade.storage

import java.io.File
import java.io.IOException

/**
 * Representation of the generated run directory structure.
 */
data class RunDir(
    val root: File,
    val eventsFile: File,
    val manifestFile: File,
    val artifactsDir: File,
    val tmpDir: File,
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
    val tmpDir = runDir.resolve(Paths.TMP)
    ensureDirectory(tmpDir)

    return RunDir(
        root = runDir,
        eventsFile = eventsFile,
        manifestFile = manifestFile,
        artifactsDir = artifactsDir,
        tmpDir = tmpDir,
    )
}

private fun ensureDirectory(directory: File) {
    if (directory.exists()) {
        if (!directory.isDirectory) {
            throw IOException("Expected directory at ${directory.absolutePath} but found a file")
        }
        return
    }
    if (!directory.mkdirs()) {
        if (!directory.exists()) {
            throw IOException("Failed to create directory ${directory.absolutePath}")
        }
    }
}

private fun ensureFile(file: File): File {
    if (file.exists()) {
        if (file.isDirectory) {
            throw IOException("Expected file at ${file.absolutePath} but found a directory")
        }
        return file
    }
    file.parentFile?.let(::ensureDirectory)
    if (!file.createNewFile()) {
        if (!file.exists()) {
            throw IOException("Failed to create file ${file.absolutePath}")
        }
    }
    return file
}
