package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaSqOk
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/** Per-sample residual of a [Palette] over a [SampleSet], in OKLab-Euclidean space. */
class Residual(
    val errors: FloatArray,      // errors[i] = OKLab-Euclidean distance from sample i to its nearest palette colour
    val importance: DoubleArray, // importance[i] = errors[i] * weight[i]
    val impTotal: Double,        // sum of importance
    val deMedian: Float,         // 50th percentile of errors (linear interpolation)
    val deP95: Float,            // 95th percentile of errors (linear interpolation)
)

fun residual(samples: SampleSet, palette: Palette): Residual {
    require(samples.size >= 1) { "samples must be non-empty" }
    require(palette.size >= 1) { "palette must be non-empty" }

    val n = samples.size
    val errors = FloatArray(n)
    val importance = DoubleArray(n)

    for (i in 0 until n) {
        // QuantTrace: data-dependent bound `i < n`; log every 1000 so a stuck scan is visible.
        if (i % 1000 == 0) qtrace("residual inner i=$i n=$n paletteSize=${palette.size}")
        val si = OkLab(samples.L[i], samples.a[i], samples.b[i])
        var minSq = Float.POSITIVE_INFINITY
        for (c in 0 until palette.size) {
            val sq = deltaSqOk(si, OkLab(palette.L[c], palette.a[c], palette.b[c]))
            if (sq < minSq) minSq = sq
        }
        errors[i] = if (minSq == 0f) 0f else sqrt(minSq.toDouble()).toFloat()
        importance[i] = errors[i].toDouble() * samples.weight[i].toDouble()
    }

    var impTotal = 0.0
    for (v in importance) impTotal += v

    val sortedErrors = errors.copyOf().also { it.sort() }

    fun percentile(sorted: FloatArray, p: Double): Float {
        val pos = p * (sorted.size - 1)
        val lo = floor(pos).toInt()
        val hi = min(sorted.size - 1, lo + 1)
        val w = pos - lo
        return (sorted[lo] * (1.0 - w) + sorted[hi] * w).toFloat()
    }

    return Residual(
        errors = errors,
        importance = importance,
        impTotal = impTotal,
        deMedian = percentile(sortedErrors, 0.5),
        deP95 = percentile(sortedErrors, 0.95),
    )
}
