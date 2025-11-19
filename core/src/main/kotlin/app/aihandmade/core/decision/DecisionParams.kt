package app.aihandmade.core.decision

/**
 * Параметры порогов и политик для гейтов G0..G3.
 */
data class DecisionParams(
    val versionTag: String = "GATE-V1-2025-11-19",
    val pixelationHigh: Double = 0.55,
    val uniqueColorsPhotoMin: Int = 4000,
    val uniqueColorsDiscreteMax: Int = 2500,
    val edgeForDiscreteMin: Double = 0.06,
    val edgeLow: Double = 0.03,
    val edgeHigh: Double = 0.12,
    val entropyLow: Double = 0.30,
    val entropyHigh: Double = 0.60,
    val defaultPickLongSide: Int = 180,
    val roundToMultiple: Int = 2
)
