package app.aihandmade.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import app.aihandmade.ui.theme.AidaColors
import app.aihandmade.ui.theme.AidaType

@Composable
fun ChartTab(chart: ChartData, modifier: Modifier = Modifier) {
    var view by remember { mutableStateOf(ChartView.BOTH) }
    var overrides by remember(chart) { mutableStateOf<Map<Int, ThreadRef>>(emptyMap()) }
    var swapTarget by remember(chart) { mutableStateOf<Int?>(null) }
    val effective = remember(chart, overrides) {
        chart.cells.mapIndexed { i, c ->
            overrides[i]?.let { o -> c.copy(argb = o.argb, code = o.code, name = o.name) } ?: c
        }
    }
    val overrideArgb = remember(overrides) { overrides.mapValues { it.value.argb } }
    val flossSorted = remember(effective) { effective.withIndex().sortedByDescending { it.value.count } }

    Column(modifier = modifier.fillMaxSize().background(AidaColors.linen).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("Pattern chart", style = AidaType.sectionTitle)
        Text("${chart.width} × ${chart.height} stitches", style = AidaType.monoCaption)

        // view toggle
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(11.dp))
                .background(AidaColors.railTrack).border(1.dp, AidaColors.borderStrong, RoundedCornerShape(11.dp)).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ChartView.entries.forEach { v ->
                val active = v == view
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                        .background(if (active) AidaColors.accent else Color.Transparent)
                        .clickable { view = v }.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when (v) { ChartView.COLOR -> "Color"; ChartView.SYMBOLS -> "Symbols"; ChartView.BOTH -> "Both" },
                        style = AidaType.body.copy(color = if (active) Color.White else AidaColors.textBody),
                    )
                }
            }
        }

        // chart viewport: whole grid fits by default; pinch to zoom, drag to pan. Owns its gestures.
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 220.dp).padding(top = 14.dp)
                .clip(RoundedCornerShape(12.dp)).border(1.dp, AidaColors.border, RoundedCornerShape(12.dp))
                .background(AidaColors.surface).clipToBounds(),
        ) {
            ZoomableChart(chart = chart, view = view, overrideArgb = overrideArgb, modifier = Modifier.fillMaxSize())
        }
        Text("pinch to zoom · drag to pan · bold lines every 10 stitches",
            style = AidaType.chartCaption, modifier = Modifier.fillMaxWidth().padding(top = 7.dp))

        Text("FLOSS LIST", style = AidaType.groupLabel, modifier = Modifier.padding(top = 16.dp))
        Spacer(Modifier.height(9.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).heightIn(max = 240.dp)) {
            items(flossSorted) { iv -> FlossRow(iv.value) { swapTarget = iv.index } }
        }
        Spacer(Modifier.height(8.dp))
    }

    val target = swapTarget
    if (target != null) {
        SwapSheet(
            current = effective[target],
            onDismiss = { swapTarget = null },
            onPick = { ref ->
                overrides = overrides + (target to ref)
                swapTarget = null
            },
        )
    }
}

@Composable
private fun FlossRow(cell: ChartCell, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(10.dp))
            .background(AidaColors.surface).border(1.dp, AidaColors.border, RoundedCornerShape(10.dp))
            .clickable { onClick() }.padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(AidaColors.symbolChipBg),
            contentAlignment = Alignment.Center,
        ) { Text(cell.glyph.toString(), style = AidaType.body.copy(color = AidaColors.surface)) }
        Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(Color(cell.argb)).border(1.dp, AidaColors.gridThin, RoundedCornerShape(6.dp)))
        Column(modifier = Modifier.weight(1f)) {
            Text("DMC ${cell.code}", style = AidaType.dmcCode)
            Text(cell.name, style = AidaType.dmcName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(cell.count.toString(), style = AidaType.count)
    }
}
