package app.aihandmade.ui.chart

/** Index-aligned grid projection for the Chart screen. cells[i] corresponds to palette index i;
 *  indexGrid values (row-major, y*width + x) index directly into cells. Not a data class — it holds
 *  an IntArray and is compared by reference (set once per pipeline run). */
class ChartData(
    val width: Int,
    val height: Int,
    val indexGrid: IntArray,
    val cells: List<ChartCell>,
) {
    init {
        require(width >= 1 && height >= 1) { "chart must be non-empty" }
        val sizeLong = width.toLong() * height.toLong()
        require(sizeLong <= Int.MAX_VALUE) { "chart size too large" }
        val size = sizeLong.toInt()
        require(indexGrid.size == size) { "indexGrid size must equal width * height" }
        require(cells.isNotEmpty()) { "cells must not be empty" }
        require(indexGrid.all { it in cells.indices }) { "indexGrid contains invalid cell index" }
    }
}

data class ChartCell(val argb: Int, val code: String, val name: String, val glyph: Char, val count: Int)
