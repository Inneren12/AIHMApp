package app.aihandmade.core.metrics

import app.aihandmade.core.color.Lab
import app.aihandmade.core.color.LabPlanes
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLabPlanes
import kotlin.math.ceil
import kotlin.math.floor

data class DeltaEStats(val mean: Double, val median: Double, val p95: Double, val max: Double)

fun deltaEStats(reference: LabPlanes, candidate: LabPlanes): DeltaEStats {
    require(reference.width == candidate.width && reference.height == candidate.height) {
        "reference and candidate must have equal dimensions"
    }
    val size = reference.size
    require(size >= 1) { "images must be non-empty" }

    val deltas = DoubleArray(size) { i ->
        deltaE2000(
            Lab(reference.L[i], reference.a[i], reference.b[i]),
            Lab(candidate.L[i], candidate.a[i], candidate.b[i]),
        )
    }

    val mean = deltas.sum() / size
    deltas.sort()
    return DeltaEStats(
        mean = mean,
        median = percentile(deltas, 0.5),
        p95 = percentile(deltas, 0.95),
        max = deltas[size - 1],
    )
}

fun deltaEStats(reference: IntArray, candidate: IntArray, width: Int, height: Int): DeltaEStats {
    val ref = reference.toLabPlanes(width, height)
    val cand = candidate.toLabPlanes(width, height)
    return deltaEStats(ref, cand)
}

private fun percentile(sortedAsc: DoubleArray, p: Double): Double {
    val n = sortedAsc.size
    if (n == 1) return sortedAsc[0]
    val rank = p * (n - 1)
    val lo = floor(rank).toInt()
    val hi = ceil(rank).toInt()
    val frac = rank - lo
    return sortedAsc[lo] + frac * (sortedAsc[hi] - sortedAsc[lo])
}
