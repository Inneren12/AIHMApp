package app.aihandmade.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.aihandmade.ui.inspector.InspectorScreen
import app.aihandmade.ui.inspector.PipelineViewModel

object Destinations {
    const val INSPECTOR = "inspector"
}

@Composable
fun AiHandMadeNavHost(
    navController: NavHostController,
    pipelineViewModel: PipelineViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.INSPECTOR,
        modifier = modifier,
    ) {
        composable(Destinations.INSPECTOR) {
            InspectorScreen(viewModel = pipelineViewModel)
        }
    }
}
