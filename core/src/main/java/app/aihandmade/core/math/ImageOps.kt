package app.aihandmade.core.math

import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Utility image operations used by analysis. */
object ImageOps {
    fun downscaleArea(src: IntArray, width: Int, height: Int, maxSide: Int): Triple<IntArray, Int, Int> {
        if (max(width, height) <= maxSide) return Triple(src.copyOf(), width, height)
        val scale = max(width, height).toDouble() / maxSide
        val newW = ceil(width / scale).toInt()
        val newH = ceil(height / scale).toInt()
        val out = IntArray(newW * newH)
        val xRatio = width.toDouble() / newW
        val yRatio = height.toDouble() / newH
        for (y in 0 until newH) {
            val yStart = (y * yRatio).toInt()
            val yEnd = min(((y + 1) * yRatio).toInt(), height)
            for (x in 0 until newW) {
                val xStart = (x * xRatio).toInt()
                val xEnd = min(((x + 1) * xRatio).toInt(), width)
                var rSum = 0.0
                var gSum = 0.0
                var bSum = 0.0
                var count = 0
                for (yy in yStart until yEnd) {
                    val idxRow = yy * width
                    for (xx in xStart until xEnd) {
                        val c = src[idxRow + xx]
                        rSum += c shr 16 and 0xFF
                        gSum += c shr 8 and 0xFF
                        bSum += c and 0xFF
                        count++
                    }
                }
                val r = (rSum / count).roundToInt().coerceIn(0, 255)
                val g = (gSum / count).roundToInt().coerceIn(0, 255)
                val b = (bSum / count).roundToInt().coerceIn(0, 255)
                out[y * newW + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Triple(out, newW, newH)
    }

    fun toGrayscale(src: IntArray, out: FloatArray) {
        for (i in src.indices) {
            val c = src[i]
            val r = (c shr 16 and 0xFF) / 255.0f
            val g = (c shr 8 and 0xFF) / 255.0f
            val b = (c and 0xFF) / 255.0f
            out[i] = (0.2126f * r + 0.7152f * g + 0.0722f * b)
        }
    }

    private fun quantBits(levels: Int): Int = when (levels) {
        256 -> 0
        128 -> 1
        64 -> 2
        32 -> 3
        16 -> 4
        8 -> 5
        4 -> 6
        2 -> 7
        else -> max(0, (8 - ln(levels.toDouble()) / ln(2.0)).roundToInt())
    }

    fun quantizeChannel(value: Int, levels: Int): Int = value ushr quantBits(levels)

    fun quantizeRgb(color: Int, levels: Int): Int {
        val bits = quantBits(levels)
        val r = quantizeChannel(color shr 16 and 0xFF, levels)
        val g = quantizeChannel(color shr 8 and 0xFF, levels)
        val b = quantizeChannel(color and 0xFF, levels)
        val shift = max(1, 8 - bits)
        return (r shl (2 * shift)) or (g shl shift) or b
    }

    fun sobel(gray: FloatArray, width: Int, height: Int, gx: FloatArray, gy: FloatArray, mag: FloatArray) {
        for (y in 1 until height - 1) {
            val yOff = y * width
            for (x in 1 until width - 1) {
                val idx = yOff + x
                val a00 = gray[idx - width - 1]
                val a01 = gray[idx - width]
                val a02 = gray[idx - width + 1]
                val a10 = gray[idx - 1]
                val a12 = gray[idx + 1]
                val a20 = gray[idx + width - 1]
                val a21 = gray[idx + width]
                val a22 = gray[idx + width + 1]
                val gxVal = (-a00 - 2 * a10 - a20 + a02 + 2 * a12 + a22)
                val gyVal = (-a00 - 2 * a01 - a02 + a20 + 2 * a21 + a22)
                gx[idx] = gxVal
                gy[idx] = gyVal
                mag[idx] = hypot(gxVal.toDouble(), gyVal.toDouble()).toFloat()
            }
        }
    }

    fun laplacian(gray: FloatArray, width: Int, height: Int, out: FloatArray) {
        for (y in 1 until height - 1) {
            val yOff = y * width
            for (x in 1 until width - 1) {
                val idx = yOff + x
                val center = gray[idx]
                val sum = gray[idx - width] + gray[idx + width] + gray[idx - 1] + gray[idx + 1]
                out[idx] = sum - 4 * center
            }
        }
    }

    fun quantile(values: FloatArray, count: Int, q: Double): Float {
        if (count == 0) return 0f
        val target = ((count - 1) * q).toInt()
        val arr = values.copyOf()
        nthElement(arr, 0, count - 1, target)
        return arr[target]
    }

    fun percentile(values: FloatArray, count: Int, p: Double): Float = quantile(values, count, p / 100.0)

    private fun nthElement(arr: FloatArray, left: Int, right: Int, n: Int) {
        var l = left
        var r = right
        while (true) {
            if (l == r) return
            var pivotIndex = (l + r) ushr 1
            pivotIndex = partition(arr, l, r, pivotIndex)
            when {
                n == pivotIndex -> return
                n < pivotIndex -> r = pivotIndex - 1
                else -> l = pivotIndex + 1
            }
        }
    }

    private fun partition(arr: FloatArray, left: Int, right: Int, pivotIndex: Int): Int {
        val pivotValue = arr[pivotIndex]
        arr[pivotIndex] = arr[right]
        arr[right] = pivotValue
        var storeIndex = left
        for (i in left until right) {
            if (arr[i] < pivotValue) {
                val tmp = arr[storeIndex]
                arr[storeIndex] = arr[i]
                arr[i] = tmp
                storeIndex++
            }
        }
        val tmp = arr[right]
        arr[right] = arr[storeIndex]
        arr[storeIndex] = tmp
        return storeIndex
    }

    fun clamp01(value: Float): Float = when {
        value < 0f -> 0f
        value > 1f -> 1f
        else -> value
    }

    fun normalizeRobust(values: FloatArray, count: Int, percentile: Double): Float {
        if (count == 0) return 1f
        val p = percentile(values, count, percentile)
        return if (p <= 1e-6f) 1f else p
    }

    fun entropyWindow(quantized: IntArray, width: Int, height: Int, window: Int, out: FloatArray) {
        val r = window / 2
        val hist = IntArray(64)
        for (y in 0 until height) {
            val yStart = max(0, y - r)
            val yEnd = min(height - 1, y + r)
            var x = 0
            while (x < width) {
                hist.fill(0)
                val xStart = max(0, x - r)
                val xEnd = min(width - 1, x + r)
                var count = 0
                for (yy in yStart..yEnd) {
                    val rowOff = yy * width
                    for (xx in xStart..xEnd) {
                        hist[quantized[rowOff + xx]]++
                        count++
                    }
                }
                var entropy = 0.0
                for (h in hist) {
                    if (h > 0) {
                        val p = h.toDouble() / count
                        entropy -= p * ln(p)
                    }
                }
                out[y * width + x] = (entropy / ln(2.0)).toFloat()
                x++
            }
        }
    }

    fun varianceWindow(gray: FloatArray, width: Int, height: Int, window: Int, out: FloatArray) {
        val r = window / 2
        for (y in 0 until height) {
            val yStart = max(0, y - r)
            val yEnd = min(height - 1, y + r)
            for (x in 0 until width) {
                val xStart = max(0, x - r)
                val xEnd = min(width - 1, x + r)
                var sum = 0.0
                var sumSq = 0.0
                var count = 0
                for (yy in yStart..yEnd) {
                    val rowOff = yy * width
                    for (xx in xStart..xEnd) {
                        val v = gray[rowOff + xx].toDouble()
                        sum += v
                        sumSq += v * v
                        count++
                    }
                }
                val mean = sum / count
                out[y * width + x] = (sumSq / count - mean * mean).toFloat()
            }
        }
    }

    fun nonMaxSuppression(
        mag: FloatArray,
        gx: FloatArray,
        gy: FloatArray,
        width: Int,
        height: Int,
        eps: Float,
        out: FloatArray
    ) {
        for (y in 1 until height - 1) {
            val yOff = y * width
            for (x in 1 until width - 1) {
                val idx = yOff + x
                val angle = atan2(gy[idx].toDouble(), gx[idx].toDouble())
                val sector = ((angle + Math.PI) * 4 / Math.PI).toInt() and 3
                val m = mag[idx]
                val (n1, n2) = when (sector) {
                    0 -> mag[idx - 1] to mag[idx + 1]
                    1 -> mag[idx - width + 1] to mag[idx + width - 1]
                    2 -> mag[idx - width] to mag[idx + width]
                    else -> mag[idx - width - 1] to mag[idx + width + 1]
                }
                out[idx] = if (m + eps >= n1 && m + eps >= n2) m else 0f
            }
        }
    }

    fun blur3x3(src: FloatArray, width: Int, height: Int, out: FloatArray) {
        for (y in 0 until height) {
            val yOff = y * width
            for (x in 0 until width) {
                var sum = 0f
                var count = 0
                for (yy in max(0, y - 1)..min(height - 1, y + 1)) {
                    val rowOff = yy * width
                    for (xx in max(0, x - 1)..min(width - 1, x + 1)) {
                        sum += src[rowOff + xx]
                        count++
                    }
                }
                out[yOff + x] = sum / count
            }
        }
    }
}
