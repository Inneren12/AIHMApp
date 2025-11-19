package app.aihandmade.core.prescale

import app.aihandmade.core.decision.model.PipelineBranch
import app.aihandmade.core.decision.model.ProcessingPlan
import app.aihandmade.core.image.RgbaImage
import app.aihandmade.core.masks.MaskSet
import app.aihandmade.core.prescale.filters.ColorUtils
import app.aihandmade.core.prescale.quant.applyPalette
import app.aihandmade.core.prescale.quant.bayerMatrix
import app.aihandmade.core.prescale.quant.medianCutQuantize
import app.aihandmade.core.prescale.scale.scaleBox
import app.aihandmade.core.prescale.scale.scaleNearest
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class PrescaleParams(
    // REALISTIC
    val bilateralRadius: Int = 2,
    val bilateralSigmaSpatial: Double = 1.6,
    val bilateralSigmaRange: Double = 0.08,
    val unsharpRadius: Int = 1,
    val unsharpAmount: Double = 0.12,
    val unsharpEdgeMin: Double = 0.2,
    val unsharpEdgeMax: Double = 0.85,

    // DISCRETE
    val deblockStrength: Double = 0.4,
    val discreteDownscale: String = "box",

    // PIXEL
    val pixelKPre: Int = 32,
    val pixelUseOrderedDither: Boolean = false,
    val pixelBayerSize: Int = 8
)

interface PreScaler {
    fun prescale(src: RgbaImage, plan: ProcessingPlan, masks: MaskSet? = null, params: PrescaleParams = PrescaleParams()): RgbaImage
}

class DefaultPreScaler : PreScaler {
    override fun prescale(src: RgbaImage, plan: ProcessingPlan, masks: MaskSet?, params: PrescaleParams): RgbaImage {
        return when (plan.pipeline) {
            PipelineBranch.PHOTO_PIPE -> realistic(src, masks, params)
            PipelineBranch.DISCRETE_PIPE -> discrete(src, plan, params)
            PipelineBranch.PIXEL_PIPE -> pixel(src, plan, params)
        }
    }
}

fun prescale(image: RgbaImage, plan: ProcessingPlan, masks: MaskSet? = null, params: PrescaleParams = PrescaleParams()): RgbaImage {
    return DefaultPreScaler().prescale(image, plan, masks, params)
}

private fun realistic(src: RgbaImage, masks: MaskSet?, params: PrescaleParams): RgbaImage {
    val width = src.width
    val height = src.height
    val edgeMask = masks?.edge ?: FloatArray(width * height)
    val yPlane = DoubleArray(width * height)
    val aArr = IntArray(width * height)
    val rArr = IntArray(width * height)
    val gArr = IntArray(width * height)
    val bArr = IntArray(width * height)
    for (i in src.pixels.indices) {
        val c = src.pixels[i]
        aArr[i] = ColorUtils.a(c)
        val r = ColorUtils.r(c)
        val g = ColorUtils.g(c)
        val b = ColorUtils.b(c)
        rArr[i] = r
        gArr[i] = g
        bArr[i] = b
        yPlane[i] = ColorUtils.yLinear(r, g, b) / 255.0
    }

    val smoothedY = bilateralFilter(yPlane, width, height, params.bilateralRadius, params.bilateralSigmaSpatial, params.bilateralSigmaRange, edgeMask)
    val adjustedY = DoubleArray(yPlane.size)
    for (i in yPlane.indices) {
        val delta = (smoothedY[i] - yPlane[i]) * (1.0 - edgeMask[i].toDouble())
        adjustedY[i] = ColorUtils.clampFloat(yPlane[i] + delta)
    }

    val blurredY = boxBlur(adjustedY, width, height, params.unsharpRadius)
    val resultPixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val idx = y * width + x
            val edge = edgeMask[idx].toDouble()
            val wEdge = ColorUtils.smoothstep(params.unsharpEdgeMin, params.unsharpEdgeMax, edge) *
                (1 - ColorUtils.smoothstep(params.unsharpEdgeMax, 1.0, edge))
            val detail = adjustedY[idx] - blurredY[idx]
            val newY = ColorUtils.clampFloat(adjustedY[idx] + params.unsharpAmount * wEdge * detail)
            val scale = newY * 255.0 - ColorUtils.yLinear(rArr[idx], gArr[idx], bArr[idx])
            val r = ColorUtils.clamp(rArr[idx] + scale)
            val g = ColorUtils.clamp(gArr[idx] + scale)
            val b = ColorUtils.clamp(bArr[idx] + scale)
            resultPixels[idx] = ColorUtils.argb(aArr[idx], r, g, b)
        }
    }
    return RgbaImage(width, height, resultPixels)
}

private fun bilateralFilter(src: DoubleArray, width: Int, height: Int, radius: Int, sigmaSpatial: Double, sigmaRange: Double, edgeMask: FloatArray): DoubleArray {
    val dst = DoubleArray(src.size)
    val spatialWeights = DoubleArray((2 * radius + 1) * (2 * radius + 1))
    var idx = 0
    for (dy in -radius..radius) {
        for (dx in -radius..radius) {
            val dist2 = (dx * dx + dy * dy).toDouble()
            spatialWeights[idx++] = exp(-dist2 / (2 * sigmaSpatial * sigmaSpatial))
        }
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            val center = src[y * width + x]
            var sum = 0.0
            var weightSum = 0.0
            idx = 0
            for (dy in -radius..radius) {
                val yy = y + dy
                if (yy < 0 || yy >= height) {
                    idx += 2 * radius + 1
                    continue
                }
                for (dx in -radius..radius) {
                    val xx = x + dx
                    if (xx < 0 || xx >= width) {
                        idx++
                        continue
                    }
                    val value = src[yy * width + xx]
                    val rangeWeight = exp(-(center - value).pow(2.0) / (2 * sigmaRange * sigmaRange))
                    val w = spatialWeights[idx] * rangeWeight
                    sum += value * w
                    weightSum += w
                    idx++
                }
            }
            dst[y * width + x] = if (weightSum > 0.0) sum / weightSum else center
        }
    }
    return dst
}

private fun boxBlur(src: DoubleArray, width: Int, height: Int, radius: Int): DoubleArray {
    if (radius <= 0) return src.copyOf()
    val temp = DoubleArray(src.size)
    val dst = DoubleArray(src.size)
    val kernelSize = 2 * radius + 1
    val inv = 1.0 / kernelSize
    // horizontal
    for (y in 0 until height) {
        var acc = 0.0
        val rowStart = y * width
        for (x in -radius until width + radius) {
            val addIdx = min(width - 1, max(0, x + radius))
            val subIdx = min(width - 1, max(0, x - radius - 1))
            acc += src[rowStart + addIdx]
            if (x - radius - 1 >= 0) acc -= src[rowStart + subIdx]
            if (x >= 0 && x < width) {
                temp[rowStart + x] = acc * inv
            }
        }
    }
    // vertical
    val invVert = 1.0 / kernelSize
    for (x in 0 until width) {
        var acc = 0.0
        for (y in -radius until height + radius) {
            val addY = min(height - 1, max(0, y + radius))
            val subY = min(height - 1, max(0, y - radius - 1))
            acc += temp[addY * width + x]
            if (y - radius - 1 >= 0) acc -= temp[subY * width + x]
            if (y >= 0 && y < height) {
                dst[y * width + x] = acc * invVert
            }
        }
    }
    return dst
}

private fun discrete(src: RgbaImage, plan: ProcessingPlan, params: PrescaleParams): RgbaImage {
    val width = src.width
    val height = src.height
    val pixels = src.pixels
    val newPixels = IntArray(pixels.size)
    val yPlane = DoubleArray(pixels.size)
    for (i in pixels.indices) {
        val c = pixels[i]
        val r = ColorUtils.r(c)
        val g = ColorUtils.g(c)
        val b = ColorUtils.b(c)
        yPlane[i] = ColorUtils.yLinear(r, g, b)
        newPixels[i] = c
    }
    val strength = params.deblockStrength
    val yAdjusted = yPlane.copyOf()
    // vertical boundaries
    for (x in 0 until width) {
        if (x == 0 || x % 8 != 0) continue
        for (y in 0 until height) {
            val idxCenter = y * width + x
            val leftIdx = y * width + x - 1
            val avgLeft = averageRangeY(yPlane, width, height, x - 2, x + 1, y)
            val avgRight = averageRangeY(yPlane, width, height, x - 1, x + 2, y)
            val yl = yPlane[leftIdx]
            val yr = yPlane[idxCenter]
            yAdjusted[leftIdx] = yl + (avgLeft - yl) * strength
            yAdjusted[idxCenter] = yr + (avgRight - yr) * strength
        }
    }
    // horizontal boundaries
    for (y in 0 until height) {
        if (y == 0 || y % 8 != 0) continue
        for (x in 0 until width) {
            val idxCenter = y * width + x
            val topIdx = (y - 1) * width + x
            val avgTop = averageRangeYVertical(yPlane, width, height, x, y - 2, y + 1)
            val avgBottom = averageRangeYVertical(yPlane, width, height, x, y - 1, y + 2)
            val yt = yPlane[topIdx]
            val yb = yPlane[idxCenter]
            yAdjusted[topIdx] = yt + (avgTop - yt) * strength
            yAdjusted[idxCenter] = yb + (avgBottom - yb) * strength
        }
    }

    for (i in pixels.indices) {
        val delta = yAdjusted[i] - yPlane[i]
        val c = pixels[i]
        val r = ColorUtils.clamp(ColorUtils.r(c) + delta)
        val g = ColorUtils.clamp(ColorUtils.g(c) + delta)
        val b = ColorUtils.clamp(ColorUtils.b(c) + delta)
        newPixels[i] = ColorUtils.argb(ColorUtils.a(c), r, g, b)
    }

    val targetW = plan.targetWidthStitches
    val targetH = plan.targetHeightStitches
    if (width <= targetW && height <= targetH) {
        return RgbaImage(width, height, newPixels)
    }
    val downscaled = when (params.discreteDownscale.lowercase()) {
        "nearest" -> scaleNearest(RgbaImage(width, height, newPixels), targetW, targetH)
        else -> scaleBox(RgbaImage(width, height, newPixels), targetW, targetH)
    }
    return downscaled
}

private fun averageRangeY(yPlane: DoubleArray, width: Int, height: Int, x0: Int, x1: Int, y: Int): Double {
    var sum = 0.0
    var count = 0
    val yy = min(height - 1, max(0, y))
    for (x in x0..x1) {
        if (x < 0 || x >= width) continue
        sum += yPlane[yy * width + x]
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

private fun averageRangeYVertical(yPlane: DoubleArray, width: Int, height: Int, x: Int, y0: Int, y1: Int): Double {
    var sum = 0.0
    var count = 0
    val xx = min(width - 1, max(0, x))
    for (y in y0..y1) {
        if (y < 0 || y >= height) continue
        sum += yPlane[y * width + xx]
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

private fun pixel(src: RgbaImage, plan: ProcessingPlan, params: PrescaleParams): RgbaImage {
    val targetW = plan.targetWidthStitches
    val targetH = plan.targetHeightStitches
    val downscaled = scaleNearest(src, targetW, targetH)
    val paletteAndIdx = medianCutQuantize(downscaled.pixels, params.pixelKPre)
    val palette = paletteAndIdx.first
    val indexed = paletteAndIdx.second
    val bayer = if (params.pixelUseOrderedDither) bayerMatrix(params.pixelBayerSize) else null
    val outPixels = IntArray(indexed.size)
    val w = downscaled.width
    val h = downscaled.height
    for (y in 0 until h) {
        for (x in 0 until w) {
            val idx = y * w + x
            val original = downscaled.pixels[idx]
            val r = ColorUtils.r(original)
            val g = ColorUtils.g(original)
            val b = ColorUtils.b(original)
            var rr = r.toDouble()
            var gg = g.toDouble()
            var bb = b.toDouble()
            if (bayer != null) {
                val n = params.pixelBayerSize
                val d = bayer[(y % n) * n + (x % n)] - 0.5f
                val shift = d / params.pixelKPre
                rr = ColorUtils.clampFloat((rr / 255.0 + shift)) * 255.0
                gg = ColorUtils.clampFloat((gg / 255.0 + shift)) * 255.0
                bb = ColorUtils.clampFloat((bb / 255.0 + shift)) * 255.0
            }
            val adjustedColor = ColorUtils.argb(255, ColorUtils.clamp(rr), ColorUtils.clamp(gg), ColorUtils.clamp(bb))
            val nearest = nearestPaletteIndex(adjustedColor, palette)
            outPixels[idx] = palette[nearest]
        }
    }
    val finalPalette = if (bayer == null) palette else palette
    val finalIndexed = if (bayer == null) indexed else IntArray(indexed.size) { nearestPaletteIndex(outPixels[it], palette) }
    val finalPixels = if (bayer == null) applyPalette(finalPalette, finalIndexed) else outPixels
    return RgbaImage(downscaled.width, downscaled.height, finalPixels)
}

private fun nearestPaletteIndex(color: Int, palette: IntArray): Int {
    var bestIdx = 0
    var bestDist = Double.MAX_VALUE
    val r0 = ColorUtils.r(color)
    val g0 = ColorUtils.g(color)
    val b0 = ColorUtils.b(color)
    for (i in palette.indices) {
        val c = palette[i]
        val dr = r0 - ColorUtils.r(c)
        val dg = g0 - ColorUtils.g(c)
        val db = b0 - ColorUtils.b(c)
        val dist = (dr * dr + dg * dg + db * db).toDouble()
        if (dist < bestDist) {
            bestDist = dist
            bestIdx = i
        }
    }
    return bestIdx
}
