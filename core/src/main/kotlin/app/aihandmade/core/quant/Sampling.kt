package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.Srgb
import app.aihandmade.core.color.toOkLabPlanes
import app.aihandmade.core.color.toOkLab as srgbToOkLabColor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// Bridge so test code in this package can call srgb.toOkLab() without importing from color.
internal fun Srgb.toOkLab(): OkLab = srgbToOkLabColor()

const val DEFAULT_SEED: Long = 1337L

private const val BETA_EDGE = 0.8
private const val BETA_BAND = 0.6
private const val BETA_NOISE = 0.5

/**
 * A representative set of image pixels as OKLab points with importance weights (structure-of-arrays).
 * `index[k]` is the source pixel index (y*sourceWidth + x); arrays are parallel and ascending by index.
 */
class SampleSet(
    val index: IntArray,
    val L: FloatArray, val a: FloatArray, val b: FloatArray,
    val weight: FloatArray,
    val sourceWidth: Int, val sourceHeight: Int,
) {
    val size: Int get() = index.size
}

fun samplePixels(
    pixels: IntArray, width: Int, height: Int,
    targetSamples: Int, seed: Long = DEFAULT_SEED,
): SampleSet {
    require(width >= 1 && height >= 1)
    require(pixels.size == width * height)
    require(targetSamples >= 1)

    val planes = pixels.toOkLabPlanes(width, height)
    val lPlane = planes.L
    val n = width * height
    val w = width

    // Sobel edge magnitude over OKLab L (border pixels = 0)
    val edgeRaw = DoubleArray(n)
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val i = y * w + x
            val gx = (-lPlane[i - w - 1].toDouble() - 2.0 * lPlane[i - 1].toDouble() - lPlane[i + w - 1].toDouble()
                    + lPlane[i - w + 1].toDouble() + 2.0 * lPlane[i + 1].toDouble() + lPlane[i + w + 1].toDouble())
            val gy = (-lPlane[i - w - 1].toDouble() - 2.0 * lPlane[i - w].toDouble() - lPlane[i - w + 1].toDouble()
                    + lPlane[i + w - 1].toDouble() + 2.0 * lPlane[i + w].toDouble() + lPlane[i + w + 1].toDouble())
            edgeRaw[i] = sqrt(gx * gx + gy * gy)
        }
    }

    // Absolute 4-neighbour Laplacian over OKLab L (border pixels = 0)
    val noiseRaw = DoubleArray(n)
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val i = y * w + x
            val lap = (lPlane[i - 1].toDouble() + lPlane[i + 1].toDouble()
                    + lPlane[i - w].toDouble() + lPlane[i + w].toDouble()
                    - 4.0 * lPlane[i].toDouble())
            noiseRaw[i] = abs(lap)
        }
    }

    // Local variance in clamped 3x3 neighbourhood for ALL pixels
    val varRaw = DoubleArray(n)
    for (y in 0 until height) {
        for (x in 0 until width) {
            var sum = 0.0
            var sumSq = 0.0
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val xx = (x + dx).coerceIn(0, width - 1)
                    val yy = (y + dy).coerceIn(0, height - 1)
                    val v = lPlane[yy * w + xx].toDouble()
                    sum += v
                    sumSq += v * v
                }
            }
            val mean = sum / 9.0
            varRaw[y * w + x] = max(0.0, sumSq / 9.0 - mean * mean)
        }
    }

    fun maxNormalize(raw: DoubleArray): DoubleArray {
        val maxVal = raw.maxOrNull() ?: 0.0
        return if (maxVal <= 1e-6) DoubleArray(raw.size)
        else DoubleArray(raw.size) { (raw[it] / maxVal).coerceIn(0.0, 1.0) }
    }

    val normEdge = maxNormalize(edgeRaw)
    val normNoise = maxNormalize(noiseRaw)
    val normVar = maxNormalize(varRaw)

    val weight = FloatArray(n) { i ->
        ((1.0 + BETA_EDGE * normEdge[i]) * (1.0 + BETA_BAND * (1.0 - normVar[i]))
                / (1.0 + BETA_NOISE * normNoise[i])).toFloat()
    }

    // Uniform random subset via partial Fisher-Yates, deterministic by seed
    val m = min(targetSamples, n)
    val idx = IntArray(n) { it }
    val rng = kotlin.random.Random(seed)
    for (i in 0 until m) {
        val j = i + rng.nextInt(n - i)
        val tmp = idx[i]; idx[i] = idx[j]; idx[j] = tmp
    }
    val selected = idx.copyOf(m).also { it.sort() }

    return SampleSet(
        index = selected,
        L = FloatArray(m) { planes.L[selected[it]] },
        a = FloatArray(m) { planes.a[selected[it]] },
        b = FloatArray(m) { planes.b[selected[it]] },
        weight = FloatArray(m) { weight[selected[it]] },
        sourceWidth = width,
        sourceHeight = height,
    )
}
