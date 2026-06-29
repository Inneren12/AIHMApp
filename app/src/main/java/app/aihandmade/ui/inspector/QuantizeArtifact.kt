package app.aihandmade.ui.inspector

import android.graphics.BitmapFactory
import app.aihandmade.core.analyze.DefaultAnalyzeService
import app.aihandmade.core.decision.DecisionInput
import app.aihandmade.core.decision.rules.DecisionEngineImpl
import app.aihandmade.core.image.RgbaImage
import app.aihandmade.core.prescale.prescale
import app.aihandmade.core.prescale.scale.scaleBox
import app.aihandmade.core.quant.buildPattern

/** Longest side of the analyze preview. Metrics are calibrated for preview scale; bigger = slower. */
private const val ANALYZE_PREVIEW_LONG_SIDE = 512

/**
 * Decode the imported artifact and run the real pipeline: analyze → DecisionEngine → prescale →
 * downscale to stitch dims → buildPattern. Prescale runs on a bounded debug-resolution source
 * (max(512, targetLongSide*3)) to avoid OOM in the inspector; the full source is never passed to
 * the PHOTO bilateral/unsharp path. Call off the main thread.
 * Returns null if the artifact can't be decoded.
 */
fun quantizeArtifactToPattern(artifactPath: String): PatternDebug? {
    val bmp = BitmapFactory.decodeFile(artifactPath) ?: return null
    val srcW = bmp.width
    val srcH = bmp.height
    val srcPixels = IntArray(srcW * srcH)
    try {
        bmp.getPixels(srcPixels, 0, srcW, 0, 0, srcW, srcH)
    } finally {
        bmp.recycle()
    }
    val srcImg = RgbaImage(srcW, srcH, srcPixels)

    // 1) analyze on a preview downscale (metrics + masks; masks are discarded — see header note 2).
    val (pvW, pvH) = previewDims(srcW, srcH, ANALYZE_PREVIEW_LONG_SIDE)
    val preview = scaleBox(srcImg, pvW, pvH)
    val (analysis, _unusedMasks) =
        DefaultAnalyzeService().analyzeAndMasks(preview.pixels, preview.width, preview.height)

    // 2) shim the analyze result into the decision result (5 metrics 1:1; sizes mapped explicitly).
    val decisionAnalyze = app.aihandmade.core.decision.AnalyzeResult(
        srcWidth = srcW,
        srcHeight = srcH,
        previewWidth = analysis.width,
        previewHeight = analysis.height,
        edgeDensity = analysis.edgeDensity,
        uniqueColorsQ = analysis.uniqueColorsQ,
        gradientSmoothness = analysis.gradientSmoothness,
        pixelationScore = analysis.pixelationScore,
        entropyScore = analysis.entropyScore,
    )
    val plan = DecisionEngineImpl().buildBasePlan(
        decisionAnalyze,
        DecisionInput(sourceWidthPx = srcW, sourceHeightPx = srcH),
    )

    // 3) Bound the prescale input to avoid inspector OOM on full-resolution phone photos.
    //    PHOTO bilateral/unsharp allocates multiple width*height buffers; cap at prepLongSide.
    val prepLongSide = maxOf(
        ANALYZE_PREVIEW_LONG_SIDE,
        maxOf(plan.targetWidthStitches, plan.targetHeightStitches) * 3,
    )
    val (prepW, prepH) = previewDims(srcW, srcH, prepLongSide)
    val prepSrc = if (prepW == srcW && prepH == srcH) srcImg else scaleBox(srcImg, prepW, prepH)
    val prepped = prescale(prepSrc, plan, masks = null)
    val stitched = scaleBox(prepped, plan.targetWidthStitches, plan.targetHeightStitches)

    // 4) quantize the stitch-dim image.
    val result = buildPattern(stitched.pixels, stitched.width, stitched.height)
    val swatches = (0 until result.palette.size).map { i ->
        val t = result.matches[i].thread
        PatternDebug.Swatch(
            argb = 0xFF000000.toInt() or t.rgb,
            code = t.code,
            name = t.name,
            count = result.counts[i],
        )
    }.sortedByDescending { it.count }

    return PatternDebug(
        widthStitches = result.width,
        heightStitches = result.height,
        colourCount = result.palette.size,
        sceneType = plan.sceneType.name,
        pipeline = plan.pipeline.name,
        swatches = swatches,
    )
}

/** Downscale-to-longSide that never upscales (a small source is left as-is). */
internal fun previewDims(w: Int, h: Int, longSide: Int): Pair<Int, Int> =
    if (w <= longSide && h <= longSide) {
        w to h
    } else if (w >= h) {
        longSide to maxOf(1, (longSide.toLong() * h / w).toInt())
    } else {
        maxOf(1, (longSide.toLong() * w / h).toInt()) to longSide
    }
