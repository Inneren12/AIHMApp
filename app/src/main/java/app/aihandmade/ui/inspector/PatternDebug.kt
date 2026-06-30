package app.aihandmade.ui.inspector

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

/** Index-aligned grid projection for the Chart screen. cells[i] corresponds to palette index i;
 *  indexGrid values (row-major, y*width + x) index directly into cells. Not a data class — it holds
 *  an IntArray and is compared by reference (set once per pipeline run). */
class ChartData(
    val width: Int,
    val height: Int,
    val indexGrid: IntArray,
    val cells: List<ChartCell>,
)

data class ChartCell(val argb: Int, val code: String, val name: String, val glyph: Char, val count: Int)
