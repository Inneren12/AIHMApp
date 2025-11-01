package app.aihandmade.core.pipeline

import app.aihandmade.core.imports.FakeArtifactStore
import app.aihandmade.export.Bitmap
import app.aihandmade.logging.JsonLogger
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StepRunnerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `emits ok status and stores artifacts`() {
        val tempDir = Files.createTempDirectory("step-runner-success")
        tempDir.toFile().deleteOnExit()
        val artifactPath = tempDir.resolve("artifacts/02_autosuggest/preview.png")
        val artifactStore = FakeArtifactStore(artifactPath)
        val logger = JsonLogger(
            runId = "run-001",
            outputDir = tempDir,
            clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC),
        )

        val context = RunContext(
            runId = "run-001",
            runDirectory = tempDir,
            logger = logger,
            artifactStore = artifactStore,
            rngSeed = 123L,
        )

        val stepRunner = StepRunner()
        val step = object : Step<DummyParams, String> {
            override val type: PipelineStep = PipelineStep.AUTOSUGGEST

            override suspend fun run(params: DummyParams, context: RunContext): StepResult<String> {
                return StepResult(
                    value = "done",
                    metrics = mapOf("iterations" to 5),
                    artifacts = mapOf("log" to "completed"),
                )
            }
        }

        val bitmap = Bitmap(2, 2, intArrayOf(-1, -1, -1, -1))
        val params = DummyParams("demo")

        val result = runBlocking {
            stepRunner.runStep(
                context = context,
                step = step,
                params = params,
                pngProducer = { stepResult ->
                    listOf(
                        StepRunner.PngArtifact(
                            name = "preview",
                            bitmap = bitmap,
                            meta = mapOf("result" to stepResult.value),
                        )
                    )
                }
            )
        }

        assertEquals("done", result.value)
        assertEquals("completed", result.artifacts["log"])
        val savedArtifact = result.artifacts["preview"] as String
        assertTrue(savedArtifact.endsWith("preview.png"))

        assertEquals("02_autosuggest", artifactStore.lastStep)
        assertEquals("preview", artifactStore.lastName)
        assertNotNull(artifactStore.lastBitmap)
        val metadata = requireNotNull(artifactStore.lastMetadata)
        assertTrue(metadata.paramsHash.isNotBlank())
        assertEquals("done", metadata.meta["result"])
        assertEquals(params.toString(), metadata.meta["params"])

        val events = readEvents(tempDir)
        assertEquals(2, events.size)
        val endEvent = events[1]
        assertEquals("span_end", endEvent["event"]?.jsonPrimitive?.content)
        assertEquals("ok", endEvent["status"]?.jsonPrimitive?.content)
        val artifactsJson = endEvent["artifacts"]?.jsonObject
        assertEquals(savedArtifact, artifactsJson?.get("preview")?.jsonPrimitive?.content)
        val metricsJson = endEvent["metrics"]?.jsonObject
        val iterationsValue = metricsJson?.get("iterations")?.jsonPrimitive?.content?.toInt()
        assertEquals(5, iterationsValue)
    }

    @Test
    fun `emits error status when step throws`() {
        val tempDir = Files.createTempDirectory("step-runner-error")
        tempDir.toFile().deleteOnExit()
        val artifactStore = FakeArtifactStore(tempDir.resolve("artifacts/05_post/preview.png"))
        val logger = JsonLogger(
            runId = "run-002",
            outputDir = tempDir,
            clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC),
        )

        val context = RunContext(
            runId = "run-002",
            runDirectory = tempDir,
            logger = logger,
            artifactStore = artifactStore,
            rngSeed = 456L,
        )

        val stepRunner = StepRunner()
        val failingStep = object : Step<DummyParams, String> {
            override val type: PipelineStep = PipelineStep.POST

            override suspend fun run(params: DummyParams, context: RunContext): StepResult<String> {
                throw IllegalStateException("boom")
            }
        }

        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                stepRunner.runStep(
                    context = context,
                    step = failingStep,
                    params = DummyParams("failure"),
                )
            }
        }
        assertEquals("boom", exception.message)

        val events = readEvents(tempDir)
        assertEquals(2, events.size)
        val endEvent = events[1]
        assertEquals("error", endEvent["status"]?.jsonPrimitive?.content)
        val metricsJson = endEvent["metrics"]?.jsonObject
        val errorMessage = metricsJson?.get("error_message")?.jsonPrimitive?.content
        assertEquals("boom", errorMessage)
        val stackValue = metricsJson?.get("stack")?.jsonPrimitive?.content
        assertTrue(stackValue?.contains("IllegalStateException") == true)
    }

    private fun readEvents(directory: Path) = Files.readAllLines(directory.resolve("events.jsonl")).map {
        json.parseToJsonElement(it).jsonObject
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

    private data class DummyParams(val value: String)
}
