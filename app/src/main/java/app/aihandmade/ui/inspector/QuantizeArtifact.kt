package app.aihandmade.ui.inspector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.aihandmade.core.quant.buildPattern

/** Longest side of the debug stitch grid. Real adaptive sizing (DecisionEngine) is A3b. */
private const val DEBUG_STITCH_LONG_SIDE = 140

/** Decode the imported artifact, downscale to stitch dims, and run the quant engine. Heavy: call off
 *  the main thread. Returns null if the artifact can't be decoded. */
fun quantizeArtifactToPattern(artifactPath: String): PatternDebug? {
    val src = BitmapFactory.decodeFile(artifactPath) ?: return null
    return try {
        val (tw, th) = stitchDims(src.width, src.height, DEBUG_STITCH_LONG_SIDE)
        val scaled = Bitmap.createScaledBitmap(src, tw, th, true)
        try {
            val pixels = IntArray(tw * th)
            scaled.getPixels(pixels, 0, tw, 0, 0, tw, th)

            val result = buildPattern(pixels, tw, th)
            val swatches = (0 until result.palette.size).map { i ->
                val t = result.matches[i].thread
                PatternDebug.Swatch(
                    argb = 0xFF000000.toInt() or t.rgb,
                    code = t.code,
                    name = t.name,
                    count = result.counts[i],
                )
            }.sortedByDescending { it.count }

            PatternDebug(result.width, result.height, result.palette.size, swatches)
        } finally {
            if (scaled !== src) scaled.recycle()
        }
    } finally {
        src.recycle()
    }
}

internal fun stitchDims(w: Int, h: Int, longSide: Int): Pair<Int, Int> =
    if (w >= h) longSide to maxOf(1, (longSide.toLong() * h / w).toInt())
    else maxOf(1, (longSide.toLong() * w / h).toInt()) to longSide
