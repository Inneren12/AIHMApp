package app.aihandmade.ui.inspector

import app.aihandmade.ui.chart.ChartData

/** Debug projection of a PatternResult for the Inspector — no core types leak into UI state. */
data class PatternDebug(
    val widthStitches: Int,
    val heightStitches: Int,
    val colourCount: Int,
    val sceneType: String,
    val pipeline: String,
    val swatches: List<Swatch>,
    val chart: ChartData,
) {
    data class Swatch(val argb: Int, val code: String, val name: String, val count: Int)
}
