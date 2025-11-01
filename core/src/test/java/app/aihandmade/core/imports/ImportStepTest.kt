package app.aihandmade.core.imports

import app.aihandmade.core.pipeline.RunContext
import app.aihandmade.export.ArtifactMetadata
import app.aihandmade.export.ArtifactRef
import app.aihandmade.export.ArtifactStore
import app.aihandmade.export.Bitmap
import app.aihandmade.logging.Logger
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportStepTest {
    @Test
    fun `stores bitmap artifact and reports metrics`() {
        val tempDir = Files.createTempDirectory("import-step-test")
        tempDir.toFile().deleteOnExit()

        val artifactPath = tempDir.resolve("artifacts/01_import/import_normalized_hash.png")
        val fakeStore = FakeArtifactStore(ArtifactRef(artifactPath))
        val fakeLogger = RecordingLogger()

        val context = RunContext(
            runId = "run-123",
            runDirectory = tempDir,
            logger = fakeLogger,
            artifactStore = fakeStore,
            rngSeed = 42L,
        )

        val step = ImportStep()
        val params = ImportParams(uri = "content://demo/image")
        val bitmap = Bitmap.from(4, 3) { x, y -> (0xFF shl 24) or (x shl 16) or (y shl 8) }

        val result = runBlocking { step.run(params, bitmap, context) }

        assertEquals(4, result.value.width)
        assertEquals(3, result.value.height)
        assertEquals(result.value.megapixels, (4 * 3) / 1_000_000f, 1e-7f)

        assertEquals(4, result.metrics["width_px"])
        assertEquals(3, result.metrics["height_px"])
        val megapixelsMetric = result.metrics["megapixels"] as Float
        assertEquals(result.value.megapixels, megapixelsMetric, 0f)

        val artifact = result.artifacts["import_normalized"] as String
        assertEquals(artifactPath.toString(), artifact)

        assertEquals("01_import", fakeStore.lastStep)
        assertEquals("import_normalized", fakeStore.lastName)
        assertTrue(fakeStore.lastMetadata.paramsHash.isNotBlank())
        assertEquals(4, fakeStore.lastMetadata.meta["width_px"])
        assertEquals(3, fakeStore.lastMetadata.meta["height_px"])

        assertTrue(fakeLogger.startedSteps.contains("IMPORT"))
    }

    private fun <T> runBlocking(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        })
        return outcome!!.getOrThrow()
    }

    private class FakeArtifactStore(private val ref: ArtifactRef) : ArtifactStore {
        lateinit var lastStep: String
        lateinit var lastName: String
        lateinit var lastBitmap: Bitmap
        lateinit var lastMetadata: ArtifactMetadata

        override fun savePng(step: String, name: String, bitmap: Bitmap, meta: ArtifactMetadata): ArtifactRef {
            lastStep = step
            lastName = name
            lastBitmap = bitmap
            lastMetadata = meta
            return ref
        }
    }

    private class RecordingLogger : Logger {
        val startedSteps = CopyOnWriteArrayList<String>()

        override fun startSpan(step: String, paramsCanonical: String): Logger.Span {
            startedSteps += step
            return Logger.Span(step = step, paramsCanonical = paramsCanonical, startedAt = java.time.Instant.now())
        }

        override fun endSpan(
            span: Logger.Span,
            metrics: Map<String, Any?>,
            artifacts: Map<String, Any?>,
            status: String,
        ) {
            // no-op
        }

        override fun writeEvent(level: String, message: String, fields: Map<String, Any?>) {
            // no-op
        }
    }
}
