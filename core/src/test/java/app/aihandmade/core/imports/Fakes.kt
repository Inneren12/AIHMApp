package app.aihandmade.core.imports

import app.aihandmade.export.ArtifactMetadata
import app.aihandmade.export.ArtifactRef
import app.aihandmade.export.ArtifactStore
import app.aihandmade.export.Bitmap
import app.aihandmade.logging.Logger
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/** Simple in-memory buffer used by tests to avoid Android bitmap dependencies. */
class ImageBuffer(
    val width: Int,
    val height: Int,
    private val pixels: IntArray,
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
        require(pixels.size == width * height) {
            "Pixel count ${'$'}{pixels.size} does not match ${'$'}width x ${'$'}height"
        }
    }

    fun toBitmap(): Bitmap = Bitmap(width, height, pixels.copyOf())

    companion object {
        fun from(width: Int, height: Int, initializer: (x: Int, y: Int) -> Int): ImageBuffer {
            val data = IntArray(width * height)
            var index = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    data[index++] = initializer(x, y)
                }
            }
            return ImageBuffer(width, height, data)
        }
    }
}

class FakeArtifactStore(private val artifactPath: Path) : ArtifactStore {
    var lastStep: String? = null
        private set
    var lastName: String? = null
        private set
    var lastBitmap: Bitmap? = null
        private set
    var lastMetadata: ArtifactMetadata? = null
        private set

    override fun savePng(step: String, name: String, bitmap: Bitmap, meta: ArtifactMetadata): ArtifactRef {
        lastStep = step
        lastName = name
        lastBitmap = bitmap
        lastMetadata = meta
        return ArtifactRef(artifactPath)
    }
}

class FakeLogger : Logger {
    val startedSpans = CopyOnWriteArrayList<Logger.Span>()
    val endedSpans = CopyOnWriteArrayList<Logger.Span>()

    override fun startSpan(step: String, paramsCanonical: String): Logger.Span {
        return Logger.Span(
            id = UUID.randomUUID().toString(),
            step = step,
            paramsCanonical = paramsCanonical,
            startedAt = Instant.now(),
        ).also { startedSpans += it }
    }

    override fun endSpan(
        span: Logger.Span,
        metrics: Map<String, Any?>,
        artifacts: Map<String, Any?>,
        status: String,
    ) {
        endedSpans += span
    }

    override fun writeEvent(level: String, message: String, fields: Map<String, Any?>) {
        // No-op for tests.
    }
}
