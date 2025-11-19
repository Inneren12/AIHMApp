package app.aihandmade.core.analyze

import app.aihandmade.core.masks.MaskSet

/**
 * Performs preview downscale and computes both metrics and masks.
 */
interface AnalyzeService {
    fun analyzeAndMasks(
        pixelsArgb: IntArray,
        width: Int,
        height: Int,
        params: AnalyzeParams = AnalyzeParams()
    ): Pair<AnalyzeResult, MaskSet>
}
