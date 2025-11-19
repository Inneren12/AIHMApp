package app.aihandmade.core.analyze

/** Parameters controlling scene analysis and mask generation. */
data class AnalyzeParams(
    val previewMax: Int = 1024,
    val quantLevelsPerChannel: Int = 64,
    val entropyWindow: Int = 9,
    val varianceWindow: Int = 5,
    val pixelationMinRun: Int = 4,
    val edgeQuantile: Double = 0.85,
    val nmsEps: Float = 1e-6f
)
