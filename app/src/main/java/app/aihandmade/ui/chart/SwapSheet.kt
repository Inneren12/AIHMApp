package app.aihandmade.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import app.aihandmade.ui.theme.AidaColors
import app.aihandmade.ui.theme.AidaType

private fun bandColor(band: SwapBand): Color = when (band) {
    SwapBand.NEAR, SwapBand.SIMILAR -> AidaColors.accent
    SwapBand.NOTICEABLE -> AidaColors.warning
    SwapBand.VERY_DIFFERENT -> AidaColors.warningStrong
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapSheet(current: ChartCell, onDismiss: () -> Unit, onPick: (ThreadRef) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    val nearest = remember(current) { nearestThreads(current.argb, current.code) }
    val results = remember(search) { searchThreads(search) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = AidaColors.linen) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            item {
                Text("Swap floss", style = AidaType.sectionTitle.copy(fontSize = 18.sp))
                Spacer(Modifier.height(10.dp))
                // Currently card (dark)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(AidaColors.surfaceInk).padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(9.dp)).background(Color(current.argb)), contentAlignment = Alignment.Center) {
                        Text(current.glyph.toString(), style = AidaType.body.copy(color = AidaColors.linen))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("CURRENTLY", style = AidaType.countCaption.copy(color = AidaColors.textMuted))
                        Text("DMC ${current.code}", style = AidaType.dmcCode.copy(color = AidaColors.linen))
                        Text(current.name, style = AidaType.dmcName.copy(color = AidaColors.textMuted), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("6 NEAREST DMC MATCHES", style = AidaType.groupLabel)
                Spacer(Modifier.height(8.dp))
            }
            items(nearest) { cand -> NearestRow(cand) { onPick(cand.ref) } }
            item {
                Spacer(Modifier.height(16.dp))
                Text("FULL DMC CATALOG", style = AidaType.groupLabel)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = search, onValueChange = { search = it }, singleLine = true,
                    textStyle = AidaType.dmcCode.copy(color = AidaColors.textStrong),
                    cursorBrush = SolidColor(AidaColors.accent),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(AidaColors.surface).border(1.5.dp, AidaColors.borderStrong, RoundedCornerShape(10.dp))
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    decorationBox = { inner ->
                        Box {
                            if (search.isEmpty()) Text("Search code or name…", style = AidaType.dmcCode.copy(color = AidaColors.textMuted))
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
            items(results) { ref -> CatalogRow(ref) { onPick(ref) } }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun NearestRow(cand: ThreadCandidate, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(10.dp))
            .background(AidaColors.surface).border(1.dp, AidaColors.border, RoundedCornerShape(10.dp))
            .clickable { onClick() }.padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(Color(cand.ref.argb)).border(1.dp, AidaColors.gridThin, RoundedCornerShape(7.dp)))
        Column(Modifier.weight(1f)) {
            Text("${cand.ref.brand} ${cand.ref.code}", style = AidaType.dmcCode)
            Text(cand.ref.name, style = AidaType.dmcName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(String.format(Locale.US, "Δ%.1f", cand.delta), style = AidaType.count.copy(color = bandColor(bandOf(cand.delta))))
    }
}

@Composable
private fun CatalogRow(ref: ThreadRef, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 5.dp).clip(RoundedCornerShape(9.dp))
            .background(AidaColors.surface).border(1.dp, AidaColors.border, RoundedCornerShape(9.dp))
            .clickable { onClick() }.padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(Color(ref.argb)).border(1.dp, AidaColors.gridThin, RoundedCornerShape(6.dp)))
        Text(ref.code, style = AidaType.dmcCode, modifier = Modifier.width(54.dp))
        Text(ref.name, style = AidaType.dmcName.copy(color = AidaColors.textBody), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
