package app.aihandmade.ui.inspector

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.aihandmade.core.imports.ImportResult
import app.aihandmade.core.pipeline.StepResult
import app.aihandmade.run.RunContextFactory
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PipelineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var tempDir: File
    private lateinit var application: Application

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("pipeline-viewmodel-test").toFile()
        application = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun importUpdatesStateWithPreviewAndMetrics() = runTest(mainDispatcherRule.dispatcher) {
        val artifactFile = tempDir.resolve("preview.png")
        val fakeExecutor = ImportExecutor { _, _, _ ->
            StepResult(
                value = ImportResult(width = 1280, height = 720, megapixels = 0.92f),
                artifacts = mapOf("import_normalized" to artifactFile.absolutePath),
            )
        }

        val viewModel = PipelineViewModel(
            application = application,
            runContextFactory = RunContextFactory(tempDir),
            importExecutor = fakeExecutor,
            dispatcher = mainDispatcherRule.dispatcher,
            projectIdProvider = { "test-project" },
        )

        val sampleUri = Uri.parse("content://example/image")

        viewModel.import(sampleUri)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        val preview = assertNotNull(state.preview)
        assertEquals(artifactFile.absolutePath, preview.artifactPath)
        assertEquals(1280, preview.width)
        assertEquals(720, preview.height)
        assertEquals(0.92f, preview.megapixels, 0.0001f)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
