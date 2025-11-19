package app.aihandmade.core.analyze

/**
 * Summary statistics computed for the preview image.
 */
data class AnalyzeResult(
    val width: Int,
    val height: Int,
    val edgeDensity: Double,
    val uniqueColorsQ: Int,
    val gradientSmoothness: Double,
    val pixelationScore: Double,
    val entropyScore: Double
)
