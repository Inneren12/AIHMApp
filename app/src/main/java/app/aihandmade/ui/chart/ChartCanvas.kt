package app.aihandmade.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aihandmade.ui.theme.AidaColors
import app.aihandmade.ui.theme.AidaType

enum class ChartView { COLOR, SYMBOLS, BOTH }

private fun luminance(argb: Int): Float {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

@Composable
fun ChartCanvas(
    chart: ChartData,
    view: ChartView,
    overrideArgb: Map<Int, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    cellDp: Dp = 15.dp,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val cols = chart.width
    val rows = chart.height

    val cellPx = with(density) { cellDp.toPx() }
    val padPx = with(density) { 24.dp.toPx() }
    val thinPx = with(density) { 1.dp.toPx() }
    val boldPx = with(density) { 1.6.dp.toPx() }
    val wDp = with(density) { (padPx + cols * cellPx).toDp() }
    val hDp = with(density) { (padPx + rows * cellPx).toDp() }
    val glyphSizeSp = with(density) { (cellPx - with(density) { 4.dp.toPx() }).toSp() }
    val rulerStyle = AidaType.groupLabel.copy(fontSize = 10.sp, color = AidaColors.textMuted)

    // Pre-measure each distinct glyph in both contrast variants (only what we'll draw).
    val glyphLayouts: Map<Pair<Char, Boolean>, TextLayoutResult> = remember(chart, view, cellPx) {
        if (view == ChartView.COLOR) emptyMap()
        else {
            val styleDark = TextStyle(fontFamily = AidaType.monoFamily, fontSize = glyphSizeSp, color = AidaColors.symbolDark)
            val styleLight = TextStyle(fontFamily = AidaType.monoFamily, fontSize = glyphSizeSp, color = AidaColors.symbolLight)
            val distinct = chart.cells.map { it.glyph }.toSet()
            buildMap {
                for (g in distinct) {
                    put(g to true, measurer.measure(AnnotatedString(g.toString()), styleDark))
                    if (view == ChartView.BOTH) put(g to false, measurer.measure(AnnotatedString(g.toString()), styleLight))
                }
            }
        }
    }

    Canvas(modifier = modifier.size(wDp, hDp)) {
        drawRect(AidaColors.chartCanvasBg, size = this.size)
        drawRect(AidaColors.chartCellBg, topLeft = Offset(padPx, padPx), size = Size(cols * cellPx, rows * cellPx))

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = chart.indexGrid[r * cols + c]
                val cell = chart.cells[idx]
                val argb = overrideArgb[idx] ?: cell.argb
                val x = padPx + c * cellPx
                val y = padPx + r * cellPx
                if (view != ChartView.SYMBOLS) {
                    drawRect(Color(argb), topLeft = Offset(x, y), size = Size(cellPx, cellPx))
                }
                if (view != ChartView.COLOR) {
                    val dark = view == ChartView.SYMBOLS || luminance(argb) > 0.55f
                    val layout = glyphLayouts[cell.glyph to dark] ?: glyphLayouts[cell.glyph to true]
                    if (layout != null) {
                        val gx = x + (cellPx - layout.size.width) / 2f
                        val gy = y + (cellPx - layout.size.height) / 2f
                        drawText(layout, topLeft = Offset(gx, gy))
                    }
                }
            }
        }

        // thin grid
        for (c in 0..cols) {
            val x = padPx + c * cellPx
            drawLine(AidaColors.gridThin, Offset(x, padPx), Offset(x, padPx + rows * cellPx), thinPx)
        }
        for (r in 0..rows) {
            val y = padPx + r * cellPx
            drawLine(AidaColors.gridThin, Offset(padPx, y), Offset(padPx + cols * cellPx, y), thinPx)
        }
        // bold grid every 10
        var c = 0
        while (c <= cols) {
            val x = padPx + c * cellPx
            drawLine(AidaColors.gridBold, Offset(x, padPx), Offset(x, padPx + rows * cellPx), boldPx)
            c += 10
        }
        var r = 0
        while (r <= rows) {
            val y = padPx + r * cellPx
            drawLine(AidaColors.gridBold, Offset(padPx, y), Offset(padPx + cols * cellPx, y), boldPx)
            r += 10
        }

        // rulers every 10
        var cc = 0
        while (cc <= cols) {
            val lab = measurer.measure(AnnotatedString(cc.toString()), rulerStyle)
            val x = padPx + cc * cellPx - lab.size.width / 2f
            drawText(lab, topLeft = Offset(x, padPx / 2f - lab.size.height / 2f))
            cc += 10
        }
        var rr = 0
        while (rr <= rows) {
            val lab = measurer.measure(AnnotatedString(rr.toString()), rulerStyle)
            val x = padPx - lab.size.width - with(density) { 4.dp.toPx() }
            drawText(lab, topLeft = Offset(x, padPx + rr * cellPx - lab.size.height / 2f))
            rr += 10
        }
    }
}
