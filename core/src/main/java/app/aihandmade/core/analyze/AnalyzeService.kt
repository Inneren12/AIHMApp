package app.aihandmade.core.analyze

import app.aihandmade.core.math.ImageOps
import app.aihandmade.core.masks.MaskSet
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/** Service computing analysis metrics and base masks. */
class DefaultAnalyzeService : AnalyzeService {
    override fun analyzeAndMasks(
        pixelsArgb: IntArray,
        width: Int,
        height: Int,
        params: AnalyzeParams
    ): Pair<AnalyzeResult, MaskSet> {
        val (scaled, w, h) = ImageOps.downscaleArea(pixelsArgb, width, height, params.previewMax)
        val size = w * h
        val gray = FloatArray(size)
        ImageOps.toGrayscale(scaled, gray)

        val gx = FloatArray(size)
        val gy = FloatArray(size)
        val mag = FloatArray(size)
        ImageOps.sobel(gray, w, h, gx, gy, mag)

        val edgeThreshold = ImageOps.quantile(mag, size, params.edgeQuantile)
        var edgeCount = 0
        for (m in mag) if (m >= edgeThreshold) edgeCount++
        val edgeDensity = edgeCount.toDouble() / size

        val uniqueColors = HashSet<Int>()
        for (c in scaled) uniqueColors.add(ImageOps.quantizeRgb(c, params.quantLevelsPerChannel))

        val lap = FloatArray(size)
        ImageOps.laplacian(gray, w, h, lap)
        val tLow = ImageOps.quantile(mag, size, 0.5)
        val smoothMask = mutableListOf<Float>()
        for (idx in mag.indices) {
            val m = mag[idx]
            if (m >= tLow) {
                smoothMask.add(abs(lap[idx]))
            }
        }
        val smoothCount = smoothMask.size
        val p99Lap = if (smoothCount > 0) ImageOps.quantile(smoothMask.toFloatArray(), smoothCount, 0.99) else 1f
        var smoothGood = 0
        if (smoothCount > 0) {
            val norm = p99Lap.takeIf { it > 1e-6f } ?: 1f
            for (value in smoothMask) {
                if (value / norm < 0.2f) smoothGood++
            }
        }
        val gradientSmoothness = if (smoothCount == 0) 1.0 else smoothGood.toDouble() / smoothCount

        val pixelationScore = computePixelation(gray, w, h, params.pixelationMinRun)

        val quantY = IntArray(size) { idx ->
            val v = (gray[idx] * 63f).toInt().coerceIn(0, 63)
            v
        }
        val entropy = FloatArray(size)
        ImageOps.entropyWindow(quantY, w, h, params.entropyWindow, entropy)
        val hMax = ln(64.0) / ln(2.0)
        val entropyScore = entropy.sum() / size / hMax

        val variance = FloatArray(size)
        ImageOps.varianceWindow(gray, w, h, params.varianceWindow, variance)

        val nms = FloatArray(size)
        ImageOps.nonMaxSuppression(mag, gx, gy, w, h, params.nmsEps, nms)
        val edgeNorm = ImageOps.normalizeRobust(nms, size, 99.0)
        for (i in nms.indices) nms[i] = ImageOps.clamp01(nms[i] / edgeNorm)

        val varianceScale = ImageOps.normalizeRobust(variance, size, 99.0)
        val flat = FloatArray(size)
        for (i in 0 until size) {
            flat[i] = ImageOps.clamp01(1f - variance[i] / varianceScale)
        }
        val flatBlur = FloatArray(size)
        ImageOps.blur3x3(flat, w, h, flatBlur)

        val entropyScale = ImageOps.normalizeRobust(entropy, size, 99.0)
        val texture = FloatArray(size)
        for (i in 0 until size) texture[i] = ImageOps.clamp01(entropy[i] / entropyScale)

        val result = AnalyzeResult(
            width = w,
            height = h,
            edgeDensity = edgeDensity,
            uniqueColorsQ = uniqueColors.size,
            gradientSmoothness = gradientSmoothness,
            pixelationScore = pixelationScore,
            entropyScore = entropyScore
        )
        val masks = MaskSet(
            width = w,
            height = h,
            edge = nms,
            flat = flatBlur,
            texture = texture,
            skin = null,
            sky = null
        )
        return result to masks
    }

    /** Analyze from byte buffer containing ARGB pixels with given stride in bytes. */
    fun analyzeAndMasks(
        pixels: ByteArray,
        width: Int,
        height: Int,
        stride: Int,
        params: AnalyzeParams = AnalyzeParams()
    ): Pair<AnalyzeResult, MaskSet> {
        val ints = IntArray(height * width)
        var idx = 0
        var offset = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val base = offset + x * 4
                val a = pixels[base].toInt() and 0xFF
                val r = pixels[base + 1].toInt() and 0xFF
                val g = pixels[base + 2].toInt() and 0xFF
                val b = pixels[base + 3].toInt() and 0xFF
                ints[idx++] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            offset += stride
        }
        return analyzeAndMasks(ints, width, height, params)
    }

    private fun computePixelation(gray: FloatArray, width: Int, height: Int, minRun: Int): Double {
        val eps = 2f / 255f
        val delta = 12f / 255f
        var rowPixels = 0
        var colPixels = 0

        fun countRun(values: FloatArray, length: Int): Int {
            var count = 0
            var start = 0
            var minV = values[0]
            var maxV = values[0]
            var i = 1
            while (i < length) {
                val v = values[i]
                if (v < minV) minV = v
                if (v > maxV) maxV = v
                if (maxV - minV > eps) {
                    val end = i - 1
                    if (end - start + 1 >= minRun) {
                        val before = if (start > 0) values[start - 1] else values[start]
                        val after = if (end + 1 < length) values[end + 1] else values[end]
                        if (abs(before - values[start]) >= delta && abs(after - values[end]) >= delta) {
                            count += end - start + 1
                        }
                    }
                    start = i
                    minV = v
                    maxV = v
                }
                i++
            }
            val end = length - 1
            if (end - start + 1 >= minRun) {
                val before = if (start > 0) values[start - 1] else values[start]
                val after = if (end + 1 < length) values[end + 1] else values[end]
                if (abs(before - values[start]) >= delta && abs(after - values[end]) >= delta) {
                    count += end - start + 1
                }
            }
            return count
        }

        for (y in 0 until height) {
            val row = FloatArray(width) { x -> gray[y * width + x] }
            rowPixels += countRun(row, width)
        }
        for (x in 0 until width) {
            val col = FloatArray(height) { y -> gray[y * width + x] }
            colPixels += countRun(col, height)
        }
        val rowScore = rowPixels.toDouble() / (width * height)
        val colScore = colPixels.toDouble() / (width * height)
        return min(1.0, max(0.0, 0.5 * (rowScore + colScore)))
    }
}
