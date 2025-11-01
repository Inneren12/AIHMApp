package app.aihandmade.ui.inspector

import app.aihandmade.core.imports.ImportParams
import app.aihandmade.core.imports.ImportResult
import app.aihandmade.core.pipeline.RunContext
import app.aihandmade.core.pipeline.StepResult
import app.aihandmade.export.ArtifactMetadata
import app.aihandmade.export.ArtifactRef
import app.aihandmade.export.ArtifactStore
import app.aihandmade.export.Bitmap
import app.aihandmade.logging.Logger
import java.nio.file.Paths
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PipelineViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `import success updates state and closes context`() = runTest(dispatcher) {
        val contextProvider = RecordingRunContextProvider()
        val importRunner = RecordingImportRunner(
            StepResult(
                value = ImportResult(width = 640, height = 480, megapixels = 0.3f),
                artifacts = mapOf("import_normalized" to "/tmp/artifacts/import_normalized.png"),
            )
        )
        val generator = ProjectIdGenerator { "project-123" }
        val viewModel = PipelineViewModel(generator, contextProvider, importRunner, dispatcher)

        viewModel.import("content://import/image")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isImporting)
        assertEquals("/tmp/artifacts/import_normalized.png", state.previewPath)
        assertEquals(640, state.widthPx)
        assertEquals(480, state.heightPx)
        assertEquals(0.3f, state.megapixels)
        assertNull(state.errorMessage)
        assertEquals(listOf("project-123"), contextProvider.createdProjectIds)
        assertEquals(1, contextProvider.closedCount)
        assertEquals("content://import/image", importRunner.lastParams?.uri)
    }

    @Test
    fun `import failure exposes error and closes context`() = runTest(dispatcher) {
        val contextProvider = RecordingRunContextProvider()
        val importRunner = RecordingImportRunner(error = IllegalStateException("boom"))
        val viewModel = PipelineViewModel(
            projectIdGenerator = ProjectIdGenerator { "project-321" },
            runContextProvider = contextProvider,
            importRunner = importRunner,
            dispatcher = dispatcher,
        )

        viewModel.import("content://bad/image")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isImporting)
        assertEquals("boom", state.errorMessage)
        assertNull(state.previewPath)
        assertEquals(1, contextProvider.closedCount)
    }

    private class RecordingRunContextProvider : RunContextProvider {
        val createdProjectIds = mutableListOf<String>()
        var closedCount: Int = 0

        override fun create(projectId: String): RunContext {
            createdProjectIds += projectId
            return RunContext(
                runId = "RID",
                runDirectory = Paths.get("/tmp/run"),
                logger = object : Logger {
                    override fun startSpan(step: String, paramsCanonical: String): Logger.Span =
                        Logger.Span(step = step, paramsCanonical = paramsCanonical, startedAt = Instant.EPOCH)

                    override fun endSpan(
                        span: Logger.Span,
                        metrics: Map<String, Any?>,
                        artifacts: Map<String, Any?>,
                        status: String,
                    ) = Unit

                    override fun writeEvent(level: String, message: String, fields: Map<String, Any?>) = Unit
                },
                artifactStore = object : ArtifactStore {
                    override fun savePng(
                        step: String,
                        name: String,
                        bitmap: Bitmap,
                        meta: ArtifactMetadata,
                    ): ArtifactRef {
                        return ArtifactRef(path = Paths.get("/tmp/artifacts/$name.png"))
                    }
                },
                rngSeed = 0L,
                onClose = { closedCount++ },
            )
        }
    }

    private class RecordingImportRunner(
        private val result: StepResult<ImportResult>? = null,
        private val error: Throwable? = null,
    ) : ImportRunner {
        var lastParams: ImportParams? = null

        override suspend fun run(params: ImportParams, runContext: RunContext): StepResult<ImportResult> {
            lastParams = params
            error?.let { throw it }
            return result ?: error("Result must be provided when no error is set")
        }
    }
}
