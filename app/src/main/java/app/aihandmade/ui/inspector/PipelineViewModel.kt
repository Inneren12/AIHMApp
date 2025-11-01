package app.aihandmade.ui.inspector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.aihandmade.core.imports.ImportParams
import app.aihandmade.core.imports.ImportResult
import app.aihandmade.core.pipeline.RunContext
import app.aihandmade.core.pipeline.StepResult
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Describes the current pipeline UI state for the inspector screen. */
data class PipelineUiState(
    val isImporting: Boolean = false,
    val previewPath: String? = null,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val megapixels: Float? = null,
    val errorMessage: String? = null,
)

fun interface ProjectIdGenerator {
    fun nextId(): String
}

fun interface RunContextProvider {
    fun create(projectId: String): RunContext
}

fun interface ImportRunner {
    suspend fun run(params: ImportParams, runContext: RunContext): StepResult<ImportResult>
}

class PipelineViewModel(
    private val projectIdGenerator: ProjectIdGenerator,
    private val runContextProvider: RunContextProvider,
    private val importRunner: ImportRunner,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(PipelineUiState())
    val state: StateFlow<PipelineUiState> = _state.asStateFlow()

    fun import(uri: String) {
        if (uri.isBlank()) {
            _state.update {
                it.copy(
                    errorMessage = "Import URI is empty",
                    isImporting = false,
                )
            }
            return
        }

        viewModelScope.launch(dispatcher) {
            _state.update { it.copy(isImporting = true, errorMessage = null) }
            val projectId = projectIdGenerator.nextId()
            var runContext: RunContext? = null
            try {
                runContext = runContextProvider.create(projectId)
                val params = ImportParams(uri = uri)
                val stepResult = importRunner.run(params, runContext)
                val previewPath = stepResult.artifacts[ARTIFACT_PREVIEW_KEY] as? String

                _state.update {
                    it.copy(
                        isImporting = false,
                        previewPath = previewPath,
                        widthPx = stepResult.value.width,
                        heightPx = stepResult.value.height,
                        megapixels = stepResult.value.megapixels,
                        errorMessage = null,
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                _state.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = throwable.message ?: "Import failed",
                    )
                }
            } finally {
                runContext?.close()
            }
        }
    }

    companion object {
        private const val ARTIFACT_PREVIEW_KEY = "import_normalized"

        fun provideFactory(
            projectIdGenerator: ProjectIdGenerator = DefaultProjectIdGenerator(),
            runContextProvider: RunContextProvider,
            importRunner: ImportRunner,
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PipelineViewModel(
                    projectIdGenerator = projectIdGenerator,
                    runContextProvider = runContextProvider,
                    importRunner = importRunner,
                    dispatcher = dispatcher,
                )
            }
        }
    }
}

class DefaultProjectIdGenerator : ProjectIdGenerator {
    override fun nextId(): String = "import-${UUID.randomUUID()}"
}
