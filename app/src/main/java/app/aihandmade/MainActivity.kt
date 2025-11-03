package app.aihandmade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import app.aihandmade.navigation.AiHandMadeNavHost
import app.aihandmade.ui.inspector.PipelineViewModel

class MainActivity : ComponentActivity() {
    private val pipelineViewModel: PipelineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiHandMadeApp(pipelineViewModel = pipelineViewModel)
        }
    }
}

@Composable
fun AiHandMadeApp(pipelineViewModel: PipelineViewModel) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            AiHandMadeNavHost(
                navController = navController,
                pipelineViewModel = pipelineViewModel,
            )
        }
    }
}

