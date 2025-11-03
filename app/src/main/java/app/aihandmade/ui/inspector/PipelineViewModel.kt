package app.aihandmade.ui.inspector

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.aihandmade.core.imports.ImportParams
import app.aihandmade.core.imports.ImportResult
import app.aihandmade.core.imports.ImportStep
import app.aihandmade.core.pipeline.RunContext
import app.aihandmade.core.pipeline.StepResult
import app.aihandmade.imports.ImportAdapter
import app.aihandmade.run.RunContextFactory
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PipelineViewModel(
    application: Application,
    private val runContextFactory: RunContextFactory = RunContextFactory(application.filesDir),
    private val importExecutor: ImportExecutor = DefaultImportExecutor(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    projectIdProvider: () -> String = { DEFAULT_PROJECT_ID },
) : AndroidViewModel(application) {

    private val projectIdRef = AtomicReference<String>()
    private val projectIdSupplier = projectIdProvider

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

    fun import(uri: Uri) {
        currentJob?.cancel()
        val job = viewModelScope.launch(dispatcher) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val projectId = projectIdRef.updateAndGet { existing -> existing ?: projectIdSupplier() }
            val context = getApplication<Application>()

            val previousPreview = _uiState.value.preview
            try {
                runContextFactory.create(projectId).use { runContext ->
                    val result = importExecutor.execute(context, uri, runContext)
                    val artifactPath = result.artifacts.values.filterIsInstance<String>().firstOrNull()
                        ?: throw IllegalStateException("Import step did not produce an artifact path")

                    _uiState.value = ImportUiState(
                        isLoading = false,
                        preview = ImportPreview(
                            artifactPath = artifactPath,
                            width = result.value.width,
                            height = result.value.height,
                            megapixels = result.value.megapixels,
                        ),
                        errorMessage = null,
                    )
                }
            } catch (throwable: Throwable) {
                _uiState.value = ImportUiState(
                    isLoading = false,
                    preview = previousPreview,
                    errorMessage = throwable.message ?: throwable.toString(),
                )
            }
        }

        job.invokeOnCompletion {
            if (currentJob === job) {
                currentJob = null
            }
        }
        currentJob = job
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val DEFAULT_PROJECT_ID = "inspector"
    }
}

data class ImportPreview(
    val artifactPath: String,
    val width: Int,
    val height: Int,
    val megapixels: Float,
)

data class ImportUiState(
    val isLoading: Boolean = false,
    val preview: ImportPreview? = null,
    val errorMessage: String? = null,
)

fun interface ImportExecutor {
    suspend fun execute(context: Context, uri: Uri, runContext: RunContext): StepResult<ImportResult>
}

class DefaultImportExecutor(
    private val importStep: ImportStep = ImportStep(),
) : ImportExecutor {
    override suspend fun execute(
        context: Context,
        uri: Uri,
        runContext: RunContext,
    ): StepResult<ImportResult> {
        val params = ImportParams(uri.toString())
        val adapter = ImportAdapter(context, importStep)
        return adapter.runStep(params, runContext)
    }
}
