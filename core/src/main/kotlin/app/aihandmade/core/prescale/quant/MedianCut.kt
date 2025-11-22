package app.aihandmade.core.prescale.quant

import app.aihandmade.core.prescale.filters.ColorUtils
import kotlin.math.max

private data class ColorBox(
    val indices: IntArray,
    val level: Int
)

fun medianCutQuantize(pixels: IntArray, k: Int): Pair<IntArray, IntArray> {
    if (pixels.isEmpty() || k <= 0) return IntArray(0) to IntArray(0)
    var boxes = mutableListOf(ColorBox(indices = IntArray(pixels.size) { it }, level = 0))
    while (boxes.size < k) {
        val box = boxes.maxByOrNull { rangeForBox(pixels, it).third } ?: break
        val range = rangeForBox(pixels, box)
        if (box.indices.size <= 1 || range.third == 0) break
        val channel = range.first
        val sorted = box.indices.sortedWith(compareBy { componentForChannel(pixels[it], channel) }).toIntArray()
        val mid = sorted.size / 2
        if (mid == 0 || mid == sorted.size) break
        val box1 = ColorBox(sorted.copyOfRange(0, mid), box.level + 1)
        val box2 = ColorBox(sorted.copyOfRange(mid, sorted.size), box.level + 1)
        boxes = (boxes - box).toMutableList()
        boxes.add(box1)
        boxes.add(box2)
    }

    val palette = IntArray(boxes.size)
    boxes.forEachIndexed { idx, box ->
        palette[idx] = averageColor(pixels, box.indices)
    }

    val indexed = IntArray(pixels.size)
    for (i in pixels.indices) {
        indexed[i] = nearestColorIndex(pixels[i], palette)
    }
    return palette to indexed
}

private fun componentForChannel(color: Int, channel: Int): Int {
    return when (channel) {
        0 -> (color ushr 16) and 0xFF
        1 -> (color ushr 8) and 0xFF
        else -> color and 0xFF
    }
}

private fun rangeForBox(pixels: IntArray, box: ColorBox): Triple<Int, Int, Int> {
    var minR = 255
    var maxR = 0
    var minG = 255
    var maxG = 0
    var minB = 255
    var maxB = 0
    for (idx in box.indices) {
        val c = pixels[idx]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        if (r < minR) minR = r
        if (r > maxR) maxR = r
        if (g < minG) minG = g
        if (g > maxG) maxG = g
        if (b < minB) minB = b
        if (b > maxB) maxB = b
    }
    val rangeR = maxR - minR
    val rangeG = maxG - minG
    val rangeB = maxB - minB
    return when {
        rangeR >= rangeG && rangeR >= rangeB -> Triple(0, box.level, rangeR)
        rangeG >= rangeR && rangeG >= rangeB -> Triple(1, box.level, rangeG)
        else -> Triple(2, box.level, rangeB)
    }
}

private fun averageColor(pixels: IntArray, indices: IntArray): Int {
    var sumR = 0.0
    var sumG = 0.0
    var sumB = 0.0
    for (idx in indices) {
        val c = pixels[idx]
        sumR += (c ushr 16) and 0xFF
        sumG += (c ushr 8) and 0xFF
        sumB += c and 0xFF
    }
    val count = max(1, indices.size)
    return ColorUtils.argb(
        255,
        ColorUtils.clamp(sumR / count),
        ColorUtils.clamp(sumG / count),
        ColorUtils.clamp(sumB / count)
    )
}

private fun nearestColorIndex(color: Int, palette: IntArray): Int {
    var bestIdx = 0
    var bestDist = Double.MAX_VALUE
    val r0 = (color ushr 16) and 0xFF
    val g0 = (color ushr 8) and 0xFF
    val b0 = color and 0xFF
    for (i in palette.indices) {
        val c = palette[i]
        val dr = r0 - ((c ushr 16) and 0xFF)
        val dg = g0 - ((c ushr 8) and 0xFF)
        val db = b0 - (c and 0xFF)
        val dist = dr * dr + dg * dg + db * db
        if (dist < bestDist) {
            bestDist = dist.toDouble()
            bestIdx = i
        }
    }
    return bestIdx
}

fun applyPalette(palette: IntArray, indexed: IntArray): IntArray {
    val out = IntArray(indexed.size)
    for (i in indexed.indices) {
        out[i] = palette[indexed[i]]
    }
    return out
}

fun bayerMatrix(n: Int): FloatArray {
    require(n == 4 || n == 8) { "Only 4 or 8 supported" }
    val base4 = arrayOf(
        intArrayOf(0, 8, 2, 10),
        intArrayOf(12, 4, 14, 6),
        intArrayOf(3, 11, 1, 9),
        intArrayOf(15, 7, 13, 5)
    )
    val matrix = if (n == 4) base4 else expandBayer(base4)
    val result = FloatArray(n * n)
    val scale = n * n
    for (y in 0 until n) {
        for (x in 0 until n) {
            result[y * n + x] = matrix[y][x].toFloat() / (scale.toFloat())
        }
    }
    return result
}

private fun expandBayer(base: Array<IntArray>): Array<IntArray> {
    val n = 8
    val result = Array(n) { IntArray(n) }
    for (y in 0 until n) {
        for (x in 0 until n) {
            val quadrantY = y / 4
            val quadrantX = x / 4
            val baseVal = base[y % 4][x % 4]
            result[y][x] = baseVal * 4 + bayerOffset(quadrantX, quadrantY)
        }
    }
    return result
}

private fun bayerOffset(qx: Int, qy: Int): Int {
    return when (qy * 2 + qx) {
        0 -> 0
        1 -> 2
        2 -> 3
        else -> 1
    }
}
