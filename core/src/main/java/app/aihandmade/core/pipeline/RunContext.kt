package app.aihandmade.core.pipeline

import app.aihandmade.export.ArtifactStore
import app.aihandmade.logging.Logger
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runtime context passed to pipeline steps.
 */
data class RunContext(
    val runId: String,
    val runDirectory: Path,
    val logger: Logger,
    val artifactStore: ArtifactStore,
    val rngSeed: Long,
    private val onClose: () -> Unit = {},
) : AutoCloseable {

    private val closed = AtomicBoolean(false)

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            onClose()
        }
    }
}
