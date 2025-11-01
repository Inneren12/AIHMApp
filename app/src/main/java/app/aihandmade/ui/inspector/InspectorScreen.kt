package app.aihandmade.ui.inspector

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun InspectorRoute(
    viewModel: PipelineViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    InspectorScreen(
        state = state,
        onImport = viewModel::import,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorScreen(
    state: PipelineUiState,
    onImport: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.toString()?.let(onImport)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Inspector") },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = { launcher.launch("image/*") },
                enabled = !state.isImporting,
            ) {
                Text(text = "Import Image")
            }

            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Export ZIP")
            }

            if (state.isImporting) {
                RowPlaceholder {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(text = "Importing…")
                }
            }

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            ArtifactRow(previewPath = state.previewPath)
            MetricsCard(state)
        }
    }
}

@Composable
private fun ArtifactRow(previewPath: String?, modifier: Modifier = Modifier) {
    val artifacts = remember(previewPath) {
        listOfNotNull(previewPath)
    }
    if (artifacts.isEmpty()) {
        ElevatedCard(
            modifier = modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Select an image to see the preview",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(artifacts) { path ->
            ArtifactThumbnail(path)
        }
    }
}

@Composable
private fun ArtifactThumbnail(path: String, modifier: Modifier = Modifier) {
    val imageBitmap = remember(path) { loadImageBitmap(path) }
    Card(
        modifier = modifier.size(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Imported preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Preview unavailable",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun MetricsCard(state: PipelineUiState, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Metrics", style = MaterialTheme.typography.titleMedium)
            MetricRow(label = "Width", value = state.widthPx?.let { "$it px" } ?: "—")
            MetricRow(label = "Height", value = state.heightPx?.let { "$it px" } ?: "—")
            MetricRow(label = "Megapixels", value = state.megapixels?.let { formatMegapixels(it) } ?: "—")
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    RowPlaceholder {
        Text(text = label, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.size(12.dp))
        Text(text = value)
    }
}

@Composable
private fun RowPlaceholder(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

private fun loadImageBitmap(path: String): ImageBitmap? {
    val bitmap = BitmapFactory.decodeFile(path) ?: return null
    return bitmap.asImageBitmap()
}

private fun formatMegapixels(value: Float): String {
    val rounded = (value * 100).roundToInt() / 100f
    return "$rounded MP"
}

@Preview
@Composable
private fun InspectorScreenPreview() {
    MaterialTheme {
        Surface {
            InspectorScreen(
                state = PipelineUiState(
                    previewPath = null,
                    widthPx = 1024,
                    heightPx = 768,
                    megapixels = 0.79f,
                ),
                onImport = {},
            )
        }
    }
}
