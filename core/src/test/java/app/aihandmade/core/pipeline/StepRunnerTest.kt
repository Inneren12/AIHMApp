package app.aihandmade.core.pipeline

import app.aihandmade.export.ArtifactMetadata
import app.aihandmade.export.ArtifactRef
import app.aihandmade.export.ArtifactStore
import app.aihandmade.export.Bitmap
import app.aihandmade.logging.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StepRunnerTest {
    @Test
    fun `runStep stores artifacts and logs success`() {
        val tempDir = Files.createTempDirectory("step-runner-success")
        tempDir.toFile().deleteOnExit()

        val artifactStore = RecordingArtifactStore(tempDir)
        val logger = RecordingLogger()
        val context = RunContext(
            runId = "run-001",
            runDirectory = tempDir,
            logger = logger,
            artifactStore = artifactStore,
            rngSeed = 7L,
        )

        val step = SuccessfulStep()
        val params = SuccessfulParams(input = 5)

        val result = runBlocking {
            StepRunner.runStep(
                context = context,
                step = step,
                params = params,
                pngProducer = { stepResult ->
                    val bitmap = Bitmap.from(2, 1) { _, _ -> 0xFF00FF00.toInt() }
                    listOf(
                        StepRunner.PngArtifact(
                            name = "preview",
                            bitmap = bitmap,
                            meta = mapOf("doubled" to stepResult.value)
                        )
                    )
                }
            )
        }

        assertEquals(10, result.value)
        assertEquals(2, result.metrics.size)
        assertEquals(5, result.metrics["input"])
        assertEquals(10, result.metrics["output"])

        val savedArtifacts = artifactStore.savedArtifacts
        assertEquals(1, savedArtifacts.size)
        val saved = savedArtifacts.single()
        assertEquals("03_classify", saved.step)
        assertEquals("preview", saved.name)
        assertEquals(mapOf("doubled" to 10), saved.metadata.meta)
        assertTrue(saved.metadata.paramsHash.isNotBlank())

        val previewPath = saved.path.toString()
        assertEquals(previewPath, result.artifacts["preview"])
        assertEquals("existing", result.artifacts["raw"])

        val startedSpan = logger.startedSpans.single()
        assertEquals(step.type.name, startedSpan.step)
        assertEquals(params.toString(), startedSpan.paramsCanonical)

        val endedSpan = logger.endedSpans.single()
        assertEquals(startedSpan, endedSpan.span)
        assertEquals("ok", endedSpan.status)
        assertEquals(result.metrics, endedSpan.metrics)
        assertEquals(result.artifacts, endedSpan.artifacts)
    }

    @Test
    fun `runStep logs error and rethrows`() {
        val tempDir = Files.createTempDirectory("step-runner-error")
        tempDir.toFile().deleteOnExit()

        val artifactStore = RecordingArtifactStore(tempDir)
        val logger = RecordingLogger()
        val context = RunContext(
            runId = "run-002",
            runDirectory = tempDir,
            logger = logger,
            artifactStore = artifactStore,
            rngSeed = 11L,
        )

        val step = FailingStep()
        val params = SuccessfulParams(input = 3)

        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                StepRunner.runStep(context, step, params)
            }
        }
        assertEquals("boom", exception.message)

        assertTrue(artifactStore.savedArtifacts.isEmpty())

        val startedSpan = logger.startedSpans.single()
        assertEquals(step.type.name, startedSpan.step)

        val endedSpan = logger.endedSpans.single()
        assertEquals("error", endedSpan.status)
        assertEquals(startedSpan, endedSpan.span)
        val errorMessage = endedSpan.metrics["error_message"] as String
        assertEquals("boom", errorMessage)
        val stack = endedSpan.metrics["stack"] as String
        assertTrue(stack.contains("IllegalStateException"))
        assertTrue(stack.contains("boom"))
    }

    private class SuccessfulStep : Step<SuccessfulParams, Int> {
        override val type: PipelineStep = PipelineStep.CLASSIFY

        override suspend fun run(params: SuccessfulParams, context: RunContext): StepResult<Int> {
            return StepResult(
                value = params.input * 2,
                metrics = mapOf(
                    "input" to params.input,
                    "output" to params.input * 2,
                ),
                artifacts = mapOf("raw" to "existing"),
            )
        }
    }

    private class FailingStep : Step<SuccessfulParams, Int> {
        override val type: PipelineStep = PipelineStep.MASKS

        override suspend fun run(params: SuccessfulParams, context: RunContext): StepResult<Int> {
            throw IllegalStateException("boom")
        }
    }

    private data class SuccessfulParams(val input: Int)

    private class RecordingArtifactStore(private val baseDir: Path) : ArtifactStore {
        data class Saved(
            val step: String,
            val name: String,
            val metadata: ArtifactMetadata,
            val path: Path,
        )

        val savedArtifacts = mutableListOf<Saved>()

        override fun savePng(step: String, name: String, bitmap: Bitmap, meta: ArtifactMetadata): ArtifactRef {
            val path = baseDir.resolve("${step}_${name}.png")
            savedArtifacts += Saved(step, name, meta, path)
            return ArtifactRef(path)
        }
    }

    private class RecordingLogger : Logger {
        data class Ended(
            val span: Logger.Span,
            val metrics: Map<String, Any?>,
            val artifacts: Map<String, Any?>,
            val status: String,
        )

        val startedSpans = mutableListOf<Logger.Span>()
        val endedSpans = mutableListOf<Ended>()

        override fun startSpan(step: String, paramsCanonical: String): Logger.Span {
            val span = Logger.Span(
                id = UUID.randomUUID().toString(),
                step = step,
                paramsCanonical = paramsCanonical,
                startedAt = Instant.now(),
            )
            startedSpans += span
            return span
        }

        override fun endSpan(
            span: Logger.Span,
            metrics: Map<String, Any?>,
            artifacts: Map<String, Any?>,
            status: String,
        ) {
            endedSpans += Ended(span, metrics, artifacts, status)
        }

        override fun writeEvent(level: String, message: String, fields: Map<String, Any?>) {
            // No-op for tests
        }
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
