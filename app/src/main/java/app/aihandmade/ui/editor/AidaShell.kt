package app.aihandmade.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import app.aihandmade.ui.chart.ChartTab
import app.aihandmade.ui.inspector.InspectorScreen
import app.aihandmade.ui.inspector.PipelineViewModel
import app.aihandmade.ui.theme.AidaColors
import app.aihandmade.ui.theme.AidaType

private enum class Tab(val label: String) {
    PHOTO("Photo"), SIZE("Size"), COLORS("Colors"), CHART("Chart"), EXPORT("Export"), INSPECTOR("Debug")
}

@Composable
fun AidaShell(viewModel: PipelineViewModel) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(Tab.INSPECTOR) }  // start on Inspector so a photo can be loaded

    Column(Modifier.fillMaxSize().background(AidaColors.linen)) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("aida", style = AidaType.wordmark)
                Text("PHOTO → PATTERN", style = AidaType.kicker)
            }
        }
        // step rail (5 named steps) + Inspector tab appended (temporary)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Tab.entries.forEachIndexed { i, t ->
                val active = t == tab
                val isDebug = t == Tab.INSPECTOR
                Column(
                    Modifier.weight(1f).clickable { tab = t },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape)
                            .background(if (active) AidaColors.accent else AidaColors.railTrack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (isDebug) "·" else (i + 1).toString(),
                            style = AidaType.body.copy(color = if (active) Color.White else AidaColors.textMuted, fontWeight = FontWeight.SemiBold))
                    }
                    Text(t.label, style = AidaType.groupLabel.copy(color = if (active) AidaColors.accent else AidaColors.textMuted),
                        modifier = Modifier.padding(top = 5.dp))
                }
            }
        }

        Box(Modifier.fillMaxSize().background(AidaColors.linen)) {
            when (tab) {
                Tab.INSPECTOR -> InspectorScreen(viewModel = viewModel)
                Tab.CHART -> {
                    val chart = state.pattern?.chart
                    if (chart != null) ChartTab(chart)
                    else Placeholder("Load a photo in the Debug tab, then come back to Chart.")
                }
                Tab.PHOTO -> Placeholder("Photo step — coming soon.")
                Tab.SIZE -> Placeholder("Size step — coming soon.")
                Tab.COLORS -> Placeholder("Colors step — coming soon.")
                Tab.EXPORT -> Placeholder("Export step — coming soon.")
            }
        }
    }
}

@Composable
private fun Placeholder(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = AidaType.body.copy(color = AidaColors.textSecondary))
    }
}
