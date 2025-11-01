package app.aihandmade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.viewModel
import app.aihandmade.core.imports.ImportStep
import app.aihandmade.imports.ImportAdapter
import app.aihandmade.run.RunContextFactory
import app.aihandmade.ui.inspector.DefaultProjectIdGenerator
import app.aihandmade.ui.inspector.ImportRunner
import app.aihandmade.ui.inspector.InspectorRoute
import app.aihandmade.ui.inspector.PipelineViewModel
import app.aihandmade.ui.inspector.RunContextProvider
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiHandMadeApp()
        }
    }
}

@Composable
fun AiHandMadeApp() {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val runContextFactory = remember(applicationContext) {
        val storageRoot = File(applicationContext.filesDir, "aihandmade")
        RunContextFactory(storageRoot)
    }
    val importAdapter = remember(applicationContext) {
        ImportAdapter(applicationContext, ImportStep())
    }

    val viewModelFactory = remember(runContextFactory, importAdapter) {
        PipelineViewModel.provideFactory(
            projectIdGenerator = DefaultProjectIdGenerator(),
            runContextProvider = RunContextProvider { projectId -> runContextFactory.create(projectId) },
            importRunner = ImportRunner { params, runContext -> importAdapter.runStep(params, runContext) },
        )
    }

    val pipelineViewModel: PipelineViewModel = viewModel(factory = viewModelFactory)

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            InspectorRoute(viewModel = pipelineViewModel)
        }
    }
}
