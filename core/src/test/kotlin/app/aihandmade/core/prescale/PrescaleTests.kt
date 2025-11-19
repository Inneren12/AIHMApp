package app.aihandmade.core.prescale

import app.aihandmade.core.decision.model.Complexity
import app.aihandmade.core.decision.model.GateSnapshot
import app.aihandmade.core.decision.model.PipelineBranch
import app.aihandmade.core.decision.model.ProcessingPlan
import app.aihandmade.core.decision.model.SceneType
import app.aihandmade.core.image.RgbaImage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.math.pow
import kotlin.math.sqrt

class PrescaleTests {

    private fun planFor(pipeline: PipelineBranch, width: Int = 128, height: Int = 80) = ProcessingPlan(
        targetWidthStitches = width,
        targetHeightStitches = height,
        sceneType = SceneType.PHOTO,
        complexity = Complexity.MEDIUM,
        pipeline = pipeline,
        reasons = emptyList(),
        gates = GateSnapshot(false, 0..0, SceneType.PHOTO, Complexity.MEDIUM, false)
    )

    @Test
    fun REALISTIC_EdgesPreserved() {
        val width = 64
        val height = 32
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val base = if (x < width / 2) 0 else 255
                pixels[y * width + x] = (0xFF shl 24) or (base shl 16) or (base shl 8) or base
            }
        }
        val rnd = Random(0)
        val noisy = IntArray(pixels.size) { idx ->
            val base = pixels[idx]
            val n = (rnd.nextGaussian() * 12).toInt()
            val c = ((base and 0xFF) + n).coerceIn(0, 255)
            (0xFF shl 24) or (c shl 16) or (c shl 8) or c
        }
        val src = RgbaImage(width, height, noisy)
        val plan = planFor(PipelineBranch.PHOTO_PIPE, width, height)
        val processed = prescale(src, plan)

        val sobelBefore = sobelEnergy(src, width, height, width / 2 - 2, width / 2 + 2)
        val sobelAfter = sobelEnergy(processed, width, height, width / 2 - 2, width / 2 + 2)
        val retention = sobelAfter / sobelBefore

        val noiseBefore = flatStdDev(src, 0, width / 2 - 4, height)
        val noiseAfter = flatStdDev(processed, 0, width / 2 - 4, height)

        assertTrue(retention >= 0.9, "Edges should be preserved")
        assertTrue(noiseAfter <= noiseBefore * 0.75, "Noise should be reduced by at least 25%")
    }

    @Test
    fun REALISTIC_NoHalos() {
        val width = 64
        val height = 32
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val base = (x * 255 / (width - 1))
                var value = base
                if (x == width / 2) value = 0
                pixels[y * width + x] = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
            }
        }
        val src = RgbaImage(width, height, pixels)
        val plan = planFor(PipelineBranch.PHOTO_PIPE, width, height)
        val processed = prescale(src, plan)

        val overshoot = haloOvershoot(src, processed, width / 2)
        assertTrue(overshoot <= 5.0, "Unsharp mask should not introduce halos > 5 levels")
    }

    @Test
    fun DISCRETE_Deblocking() {
        val width = 64
        val height = 64
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val block = ((x / 8 + y / 8) % 2)
                val base = 120 + block * 40
                val withEdge = if (x == 10) 255 else base
                pixels[y * width + x] = (0xFF shl 24) or (withEdge shl 16) or (withEdge shl 8) or withEdge
            }
        }
        val src = RgbaImage(width, height, pixels)
        val plan = planFor(PipelineBranch.DISCRETE_PIPE, width, height)
        val processed = prescale(src, plan, params = PrescaleParams(deblockStrength = 0.5))

        val boundaryVarBefore = boundaryVariance(src, 8)
        val boundaryVarAfter = boundaryVariance(processed, 8)
        assertTrue(boundaryVarAfter <= boundaryVarBefore * 0.7, "Deblocking should reduce boundary variance")

        val stepEdgeBefore = sobelEnergy(src, width, height, 9, 11)
        val stepEdgeAfter = sobelEnergy(processed, processed.width, processed.height, 9, 11)
        assertTrue(stepEdgeAfter >= stepEdgeBefore * 0.8, "Non-block edges should remain sharp")
    }

    @Test
    fun DISCRETE_KeepSteps() {
        val width = 32
        val height = 16
        val steps = listOf(30, 80, 130, 200)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = (x * steps.size) / width
                val v = steps[idx]
                pixels[y * width + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
        }
        val src = RgbaImage(width, height, pixels)
        val plan = planFor(PipelineBranch.DISCRETE_PIPE, width, height)
        val processed = prescale(src, plan)

        val uniqueBefore = pixels.toSet().size
        val uniqueAfter = processed.pixels.toSet().size
        assertTrue(uniqueAfter >= uniqueBefore, "Step levels should be preserved")
    }

    @Test
    fun PIXEL_GridAndPalette() {
        val width = 256
        val height = 160
        val rnd = Random(1)
        val pixels = IntArray(width * height) {
            val r = rnd.nextInt(256)
            val g = rnd.nextInt(256)
            val b = rnd.nextInt(256)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val src = RgbaImage(width, height, pixels)
        val plan = planFor(PipelineBranch.PIXEL_PIPE, 128, 80)
        val processed = prescale(src, plan, params = PrescaleParams(pixelKPre = 32, pixelUseOrderedDither = false))
        assertEquals(128, processed.width)
        assertEquals(80, processed.height)
        assertTrue(processed.pixels.toSet().size <= 32, "Palette should be limited without dithering")
    }

    @Test
    fun PIXEL_DitherOptional() {
        val width = 96
        val height = 64
        val rnd = Random(2)
        val pixels = IntArray(width * height) {
            val r = rnd.nextInt(256)
            val g = rnd.nextInt(256)
            val b = rnd.nextInt(256)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val src = RgbaImage(width, height, pixels)
        val plan = planFor(PipelineBranch.PIXEL_PIPE, 48, 32)

        val paramsNoDither = PrescaleParams(pixelKPre = 16, pixelUseOrderedDither = false)
        val paramsDither = PrescaleParams(pixelKPre = 16, pixelUseOrderedDither = true, pixelBayerSize = 8)

        val processedNo = prescale(src, plan, params = paramsNoDither)
        val processedDither = prescale(src, plan, params = paramsDither)

        val downscaled = prescale(src, plan.copy(pipeline = PipelineBranch.DISCRETE_PIPE))

        val rmseNo = rmse(downscaled, processedNo)
        val rmseDither = rmse(downscaled, processedDither)

        assertTrue(rmseDither < rmseNo, "Dithering should reduce RMSE to target")
        assertTrue(processedDither.pixels.toSet().size >= processedNo.pixels.toSet().size, "Dither can introduce more colors")
    }

    private fun sobelEnergy(img: RgbaImage, width: Int, height: Int, x0: Int, x1: Int): Double {
        val gx = arrayOf(intArrayOf(-1, 0, 1), intArrayOf(-2, 0, 2), intArrayOf(-1, 0, 1))
        val gy = arrayOf(intArrayOf(1, 2, 1), intArrayOf(0, 0, 0), intArrayOf(-1, -2, -1))
        var energy = 0.0
        for (y in 1 until height - 1) {
            for (x in maxOf(1, x0) until minOf(width - 1, x1)) {
                var sx = 0.0
                var sy = 0.0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val c = img.pixels[(y + ky) * width + (x + kx)]
                        val v = (c and 0xFF)
                        sx += gx[ky + 1][kx + 1] * v
                        sy += gy[ky + 1][kx + 1] * v
                    }
                }
                energy += sqrt(sx * sx + sy * sy)
            }
        }
        return energy
    }

    private fun flatStdDev(img: RgbaImage, x0: Int, x1: Int, height: Int): Double {
        val values = mutableListOf<Int>()
        for (y in 0 until height) {
            for (x in x0 until x1) {
                val c = img.pixels[y * img.width + x]
                values.add(c and 0xFF)
            }
        }
        val mean = values.average()
        val variance = values.sumOf { (it - mean).pow(2.0) } / values.size
        return sqrt(variance)
    }

    private fun haloOvershoot(original: RgbaImage, processed: RgbaImage, lineX: Int): Double {
        var maxOver = 0.0
        for (y in 0 until original.height) {
            val baseLeft = original.pixels[y * original.width + lineX - 1] and 0xFF
            val baseRight = original.pixels[y * original.width + lineX + 1] and 0xFF
            val procLeft = processed.pixels[y * processed.width + lineX - 1] and 0xFF
            val procRight = processed.pixels[y * processed.width + lineX + 1] and 0xFF
            maxOver = maxOf(maxOver, (procLeft - baseLeft).toDouble())
            maxOver = maxOf(maxOver, (procRight - baseRight).toDouble())
        }
        return maxOver
    }

    private fun boundaryVariance(img: RgbaImage, step: Int): Double {
        val diffs = mutableListOf<Double>()
        for (y in 0 until img.height) {
            for (x in 1 until img.width) {
                if (x % step == 0) {
                    val left = img.pixels[y * img.width + x - 1] and 0xFF
                    val right = img.pixels[y * img.width + x] and 0xFF
                    diffs.add((right - left).toDouble())
                }
            }
        }
        val mean = diffs.average()
        val variance = diffs.sumOf { (it - mean).pow(2.0) } / diffs.size
        return variance
    }

    private fun rmse(reference: RgbaImage, other: RgbaImage): Double {
        val w = other.width
        val h = other.height
        var sum = 0.0
        for (i in 0 until w * h) {
            val c0 = reference.pixels.getOrElse(i) { 0 }
            val c1 = other.pixels[i]
            val dr = ((c0 ushr 16) and 0xFF) - ((c1 ushr 16) and 0xFF)
            val dg = ((c0 ushr 8) and 0xFF) - ((c1 ushr 8) and 0xFF)
            val db = (c0 and 0xFF) - (c1 and 0xFF)
            sum += dr * dr + dg * dg + db * db
        }
        return sqrt(sum / (w * h * 3.0))
    }
}
