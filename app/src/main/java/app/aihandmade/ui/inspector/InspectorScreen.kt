package app.aihandmade.ui.inspector

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun InspectorScreen(
    viewModel: PipelineViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::import)
    }

    InspectorContent(
        state = state,
        onSelectImage = { imagePicker.launch("image/*") },
        modifier = modifier,
    )
}

@Composable
private fun InspectorContent(
    state: ImportUiState,
    onSelectImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewBitmap = remember(state.preview?.artifactPath) {
        state.preview?.artifactPath?.let { path ->
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Inspector",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Button(onClick = onSelectImage, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Select photo")
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        state.errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        val preview = state.preview
        if (preview == null) {
            Text(
                text = "Select a photo to preview its metrics.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            if (previewBitmap != null) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "Imported preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else {
                Text(
                    text = "Preview unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            MetricsSection(preview = preview)
            state.pattern?.let { PatternSection(it) }
            state.patternError?.let { error ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pattern failed:\n$error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            if (state.pattern == null && state.patternError == null && state.errorMessage == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("Quantizing…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))

        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Export ZIP")
        }
    }
}

@Composable
private fun MetricsSection(preview: ImportPreview) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricRow(label = "Width", value = "${preview.width} px")
            MetricRow(label = "Height", value = "${preview.height} px")
            MetricRow(
                label = "Megapixels",
                value = String.format("%.2f MP", preview.megapixels),
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PatternSection(pattern: PatternDebug) {
    OutlinedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Pattern (debug)", style = MaterialTheme.typography.titleMedium)
            MetricRow("Stitches", "${pattern.widthStitches} × ${pattern.heightStitches}")
            MetricRow("Colours", "${pattern.colourCount} DMC")
            MetricRow("Scene", pattern.sceneType)
            MetricRow("Branch", pattern.pipeline)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pattern.swatches.forEach { s ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(s.argb)),
                        )
                        Text(s.code, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                        Text(s.count.toString(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PatternSectionPreview() {
    PatternSection(
        PatternDebug(
            widthStitches = 140,
            heightStitches = 98,
            colourCount = 3,
            sceneType = "PHOTO",
            pipeline = "PHOTO_PIPE",
            swatches = listOf(
                PatternDebug.Swatch(0xFF1C3A2E.toInt(), "500", "Blue Green VY DK", 4200),
                PatternDebug.Swatch(0xFFE8C8A0.toInt(), "3774", "Desert Sand MD", 3100),
                PatternDebug.Swatch(0xFFB04030.toInt(), "919", "Red Copper", 1820),
            ),
            chart = ChartData(0, 0, IntArray(0), emptyList()),
        ),
    )
}
