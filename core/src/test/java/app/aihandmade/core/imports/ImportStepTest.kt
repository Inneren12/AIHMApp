package app.aihandmade.core.imports

import app.aihandmade.core.pipeline.RunContext
import java.nio.file.Files
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportStepTest {
    @Test
    fun `stores bitmap artifact and reports metrics`() {
        val tempDir = Files.createTempDirectory("import-step-test")
        tempDir.toFile().deleteOnExit()

        val artifactPath = tempDir.resolve("artifacts/01_import/import_normalized.png")
        val fakeStore = FakeArtifactStore(artifactPath)
        val fakeLogger = FakeLogger()

        val context = RunContext(
            runId = "run-123",
            runDirectory = tempDir,
            logger = fakeLogger,
            artifactStore = fakeStore,
            rngSeed = 42L,
        )

        val step = ImportStep()
        val params = ImportParams(uri = "content://demo/image")
        val image = ImageBuffer.from(4, 3) { x, y -> (0xFF shl 24) or (x shl 16) or (y shl 8) }
        val bitmap = image.toBitmap()

        val result = runBlocking { step.run(params, bitmap, context) }

        assertEquals(4, result.value.width)
        assertEquals(3, result.value.height)
        assertEquals(result.value.megapixels, (4 * 3) / 1_000_000f, 1e-7f)

        assertEquals(4, result.metrics["width_px"])
        assertEquals(3, result.metrics["height_px"])
        val megapixelsMetric = result.metrics["megapixels"] as Float
        assertEquals(result.value.megapixels, megapixelsMetric, 1e-7f)

        assertTrue(result.artifacts.isNotEmpty())
        val artifact = result.artifacts["import_normalized"] as? String
        assertTrue(!artifact.isNullOrBlank())

        assertEquals("01_import", fakeStore.lastStep)
        assertEquals("import_normalized", fakeStore.lastName)
        val metadata = requireNotNull(fakeStore.lastMetadata)
        assertTrue(metadata.paramsHash.isNotBlank())
        assertEquals(4, metadata.meta["width_px"])
        assertEquals(3, metadata.meta["height_px"])

        val startedSpan = fakeLogger.startedSpans.single()
        assertEquals("IMPORT", startedSpan.step)
        assertTrue(fakeLogger.endedSpans.contains(startedSpan))
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
}
