package app.aihandmade.run

import app.aihandmade.core.pipeline.RunContext
import app.aihandmade.export.ArtifactStoreFs
import app.aihandmade.logging.JsonLogger
import app.aihandmade.logging.ManifestWriter
import app.aihandmade.storage.createProject
import app.aihandmade.storage.createRun
import java.io.File
import java.util.UUID

class RunContextFactory(
    private val storageRoot: File
) {

    fun create(projectId: String): RunContext {
        val runId = generateRunId()
        val projectDir = createProject(storageRoot, projectId)
        val runDir = createRun(projectDir, runId)

        val manifestWriter = ManifestWriter(
            runId = runId,
            outputDir = runDir.root.toPath()
        )

        val logger = JsonLogger(
            runId = runId,
            outputDir = runDir.root.toPath(),
            manifestWriter = manifestWriter
        )

        val artifactStore = ArtifactStoreFs(
            runId = runId,
            rootDir = runDir.root.toPath()
        )

        val rngSeed = System.currentTimeMillis()

        return RunContext(
            runId = runId,
            logger = logger,
            artifactStore = artifactStore,
            rngSeed = rngSeed
        )
    }

    private fun generateRunId(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        return uuid.substring(0, 8)
    }
}
