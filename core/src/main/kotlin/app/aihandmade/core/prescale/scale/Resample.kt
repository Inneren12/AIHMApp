package app.aihandmade.core.prescale.scale

import app.aihandmade.core.image.RgbaImage
import app.aihandmade.core.prescale.filters.ColorUtils
import kotlin.math.max
import kotlin.math.min

fun scaleNearest(src: RgbaImage, targetW: Int, targetH: Int): RgbaImage {
    if (src.width == targetW && src.height == targetH) return src
    val dstPixels = IntArray(targetW * targetH)
    val xRatio = src.width.toDouble() / targetW
    val yRatio = src.height.toDouble() / targetH
    val srcPixels = src.pixels
    for (y in 0 until targetH) {
        val srcY = min(src.height - 1, max(0, ((y + 0.5) * yRatio - 0.5).toInt()))
        val rowOffset = y * targetW
        val srcRow = srcY * src.width
        for (x in 0 until targetW) {
            val srcX = min(src.width - 1, max(0, ((x + 0.5) * xRatio - 0.5).toInt()))
            dstPixels[rowOffset + x] = srcPixels[srcRow + srcX]
        }
    }
    return RgbaImage(targetW, targetH, dstPixels)
}

fun scaleBox(src: RgbaImage, targetW: Int, targetH: Int): RgbaImage {
    if (src.width == targetW && src.height == targetH) return src
    val dstPixels = IntArray(targetW * targetH)
    val xScale = src.width.toDouble() / targetW
    val yScale = src.height.toDouble() / targetH
    val srcPixels = src.pixels
    for (y in 0 until targetH) {
        val y0 = (y * yScale).toInt()
        val y1 = min(src.height, ((y + 1) * yScale).toInt())
        val rowOffset = y * targetW
        for (x in 0 until targetW) {
            val x0 = (x * xScale).toInt()
            val x1 = min(src.width, ((x + 1) * xScale).toInt())
            var sumA = 0.0
            var sumR = 0.0
            var sumG = 0.0
            var sumB = 0.0
            var count = 0
            for (yy in y0 until max(y1, y0 + 1)) {
                val base = yy * src.width
                for (xx in x0 until max(x1, x0 + 1)) {
                    val c = srcPixels[base + xx]
                    sumA += (c ushr 24) and 0xFF
                    sumR += (c ushr 16) and 0xFF
                    sumG += (c ushr 8) and 0xFF
                    sumB += c and 0xFF
                    count++
                }
            }
            val inv = 1.0 / count
            dstPixels[rowOffset + x] = ColorUtils.argb(
                ColorUtils.clamp(sumA * inv),
                ColorUtils.clamp(sumR * inv),
                ColorUtils.clamp(sumG * inv),
                ColorUtils.clamp(sumB * inv)
            )
        }
    }
    return RgbaImage(targetW, targetH, dstPixels)
}
