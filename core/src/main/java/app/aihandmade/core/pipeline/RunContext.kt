package app.aihandmade.core.pipeline

import app.aihandmade.export.ArtifactStore
import java.util.logging.Logger

data class RunContext(
    val runId: String,
    val logger: Logger,
    val artifactStore: ArtifactStore,
    val rngSeed: Long
)
