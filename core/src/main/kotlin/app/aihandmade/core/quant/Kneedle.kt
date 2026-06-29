package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb
import kotlin.math.abs
import kotlin.math.max

const val KNEE_TAU: Float = 0.03f
const val KNEE_TAU_GAIN: Float = 0.05f
const val KNEE_DE95_TARGET: Float = 3.0f
const val KNEE_MEDIAN_WINDOW: Int = 3
const val KNEE_LOW_GAIN_STREAK: Int = 3

data class KneedleRow(val k: Int, val de95: Float, val gain: Float, val cumGain: Float, val deviation: Float)
data class KneedleResult(val rows: List<KneedleRow>, val kStar: Int, val reason: String)

fun selectK(samples: SampleSet, palette: Palette, k0: Int, kTry: Int = palette.size): KneedleResult {
    require(samples.size >= 1) { "samples must be non-empty" }
    require(palette.size >= 1) { "palette must be non-empty" }
    require(k0 >= 1) { "k0 must be >= 1" }
    require(kTry >= k0) { "kTry must be >= k0" }
    require(kTry <= palette.size) { "kTry must be <= palette.size" }
    for (i in 0 until samples.size) {
        require(samples.L[i].isFinite() && samples.a[i].isFinite() && samples.b[i].isFinite()) {
            "sample OKLab coordinates must be finite"
        }
    }
    for (c in 0 until kTry) {
        require(palette.L[c].isFinite() && palette.a[c].isFinite() && palette.b[c].isFinite()) {
            "palette OKLab coordinates must be finite"
        }
    }

    val n = samples.size

    val paletteLab = Array(kTry) { c ->
        OkLab(palette.L[c], palette.a[c], palette.b[c]).toLinearRgb().toLab()
    }
    val sampleLab = Array(n) { i ->
        OkLab(samples.L[i], samples.a[i], samples.b[i]).toLinearRgb().toLab()
    }

    val nearest = FloatArray(n) { Float.POSITIVE_INFINITY }
    val de95AtK = FloatArray(kTry + 1)

    for (k in 1..kTry) {
        val palColor = paletteLab[k - 1]
        for (i in 0 until n) {
            val d = deltaE2000(sampleLab[i], palColor).toFloat()
            if (d < nearest[i]) nearest[i] = d
        }
        if (k >= k0) {
            de95AtK[k] = p95(nearest)
        }
    }

    val count = kTry - k0 + 1
    val de95 = FloatArray(count)
    val rawGain = FloatArray(count)
    val rawCumGain = FloatArray(count)

    for (idx in 0 until count) {
        val k = k0 + idx
        de95[idx] = de95AtK[k]
        rawGain[idx] = if (idx == 0) 0f else de95[idx - 1] - de95[idx]
        rawCumGain[idx] = if (idx == 0) 0f else rawCumGain[idx - 1] + rawGain[idx]
    }

    val smoothed = medianSmooth(rawCumGain, KNEE_MEDIAN_WINDOW)

    val f0 = smoothed[0]
    val fEnd = smoothed[count - 1]
    val denomY = if (abs(fEnd - f0) < 1e-6f) 1f else (fEnd - f0)
    val kRange = max(1, kTry - k0)

    val rows = ArrayList<KneedleRow>(count)
    for (idx in 0 until count) {
        val k = k0 + idx
        val x = if (kTry == k0) 1f else (k - k0).toFloat() / kRange
        val y = if (abs(denomY) < 1e-6f) 0f else (smoothed[idx] - f0) / denomY
        rows.add(KneedleRow(k = k, de95 = de95[idx], gain = rawGain[idx], cumGain = smoothed[idx], deviation = y - x))
    }

    var bestIdx = 0
    for (idx in 1 until rows.size) {
        if (rows[idx].deviation > rows[bestIdx].deviation) bestIdx = idx
    }
    val kneeOk = rows[bestIdx].deviation >= KNEE_TAU

    val earlyQualityK = rows.firstOrNull { it.de95 <= KNEE_DE95_TARGET }?.k
    val lowGainK = findLowGainK(rows)

    val (kStar, reason) = when {
        kneeOk -> rows[bestIdx].k to "knee"
        earlyQualityK != null -> earlyQualityK to "early_quality"
        lowGainK != null -> lowGainK to "low_gain"
        else -> rows.last().k to "k_max"
    }

    // QuantTrace: the candidate K scan is the bounded range [k0..kTry]; log every candidate tried
    // (its de95) and the chosen kStar so the auto-K decision is visible in logcat.
    qtrace("selectK candidates k0=$k0 kTry=$kTry tried=${rows.joinToString(",") { "${it.k}:de95=${"%.2f".format(it.de95)}" }}")
    qtrace("selectK chosen kStar=$kStar reason=$reason")

    return KneedleResult(rows, kStar, reason)
}

private fun p95(values: FloatArray): Float {
    val sorted = values.copyOf()
    sorted.sort()
    val n = sorted.size
    val pos = 0.95 * (n - 1)
    val lo = pos.toInt()
    val hi = minOf(n - 1, lo + 1)
    val w = pos - lo
    return (sorted[lo] * (1.0 - w) + sorted[hi] * w).toFloat()
}

private fun medianSmooth(values: FloatArray, window: Int): FloatArray {
    val n = values.size
    val result = values.copyOf()
    if (n <= 2 || window < 2) return result
    val half = window / 2
    for (i in 1 until n - 1) {
        val lo = maxOf(0, i - half)
        val hi = minOf(n - 1, i + half)
        val buf = values.copyOfRange(lo, hi + 1)
        buf.sort()
        result[i] = buf[buf.size / 2]
    }
    return result
}

private fun findLowGainK(rows: List<KneedleRow>): Int? {
    var streak = 0
    for (idx in 1 until rows.size) {   // skip the baseline row (k0): its gain is a synthetic 0
        if (rows[idx].gain < KNEE_TAU_GAIN) {
            streak++
            if (streak >= KNEE_LOW_GAIN_STREAK) return rows[idx].k
        } else {
            streak = 0
        }
    }
    return null
}
