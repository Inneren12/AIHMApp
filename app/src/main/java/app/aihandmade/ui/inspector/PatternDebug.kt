package app.aihandmade.ui.inspector

/** Debug projection of a PatternResult for the Inspector — no core types leak into UI state. */
data class PatternDebug(
    val widthStitches: Int,
    val heightStitches: Int,
    val colourCount: Int,
    val swatches: List<Swatch>,
) {
    data class Swatch(val argb: Int, val code: String, val name: String, val count: Int)
}
