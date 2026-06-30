package app.aihandmade.ui.chart

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Reference cell + pad. MUST match ChartCanvas (pad is hard-coded 24.dp there; cell is passed below). */
private val REF_CELL = 15.dp
private val REF_PAD = 24.dp

private fun clampOffset(
    offset: Offset, scale: Float,
    contentW: Float, contentH: Float, viewportW: Float, viewportH: Float,
): Offset {
    val scaledW = contentW * scale
    val scaledH = contentH * scale
    val x = if (scaledW <= viewportW) (viewportW - scaledW) / 2f else offset.x.coerceIn(viewportW - scaledW, 0f)
    val y = if (scaledH <= viewportH) (viewportH - scaledH) / 2f else offset.y.coerceIn(viewportH - scaledH, 0f)
    return Offset(x, y)
}

@Composable
fun ZoomableChart(chart: ChartData, view: ChartView, overrideArgb: Map<Int, Int> = emptyMap(), modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val cols = chart.width
    val rows = chart.height

    BoxWithConstraints(modifier.clipToBounds()) {
        val viewportWpx = with(density) { maxWidth.toPx() }
        val viewportHpx = with(density) { maxHeight.toPx() }
        val contentWpx = with(density) { (REF_PAD + REF_CELL * cols.toFloat()).toPx() }
        val contentHpx = with(density) { (REF_PAD + REF_CELL * rows.toFloat()).toPx() }

        // fit-to-screen: smallest scale that makes the WHOLE grid fit both axes
        val minScale = minOf(viewportWpx / contentWpx, viewportHpx / contentHpx)
        val maxScale = (minScale * 12f).coerceAtLeast(1f)

        var scale by remember(chart) { mutableStateOf(minScale) }
        var offset by remember(chart) {
            mutableStateOf(clampOffset(Offset.Zero, minScale, contentWpx, contentHpx, viewportWpx, viewportHpx))
        }

        Box(
            Modifier.fillMaxSize().pointerInput(chart, minScale, maxScale) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(minScale, maxScale)
                    val k = newScale / scale
                    val moved = Offset(
                        x = centroid.x - (centroid.x - offset.x) * k + pan.x,
                        y = centroid.y - (centroid.y - offset.y) * k + pan.y,
                    )
                    scale = newScale
                    offset = clampOffset(moved, newScale, contentWpx, contentHpx, viewportWpx, viewportHpx)
                }
            }
        ) {
            ChartCanvas(
                chart = chart,
                view = view,
                overrideArgb = overrideArgb,
                cellDp = REF_CELL,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    transformOrigin = TransformOrigin(0f, 0f)
                },
            )
        }
    }
}
