package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaOk

const val CLUSTER_BIN_RADIUS = 2

private fun requireValidBinIndex(bin: BinIndex) {
    require(bin.l in 0 until B_L && bin.a in 0 until B_A && bin.b in 0 until B_B) {
        "bin index out of range"
    }
}

/** Sample indices (ascending) whose OKLab bin is within Chebyshev [binRadius] of [center]. */
fun collectCluster(samples: SampleSet, center: BinIndex, binRadius: Int = CLUSTER_BIN_RADIUS): IntArray {
    require(binRadius >= 0) { "binRadius must be >= 0" }
    requireValidBinIndex(center)
    val result = mutableListOf<Int>()
    for (i in 0 until samples.size) {
        val bi = binOf(samples.L[i], samples.a[i], samples.b[i])
        val chebyshev = maxOf(
            kotlin.math.abs(bi.l - center.l),
            kotlin.math.abs(bi.a - center.a),
            kotlin.math.abs(bi.b - center.b),
        )
        if (chebyshev <= binRadius) result.add(i)
    }
    return result.toIntArray()
}

/**
 * Weighted medoid of a cluster: the sample j (from [cluster]) minimising
 *   sum over i in cluster of  weight[i] * deltaOk(sample_j, sample_i).
 * Ties break to the smallest sample index. Returns the index into the sample set.
 */
fun weightedMedoid(samples: SampleSet, cluster: IntArray): Int {
    require(cluster.isNotEmpty()) { "cluster must be non-empty" }
    for (idx in cluster) {
        require(idx in 0 until samples.size) { "cluster index out of range" }
        require(samples.L[idx].isFinite() && samples.a[idx].isFinite() && samples.b[idx].isFinite()) {
            "cluster sample coordinates must be finite"
        }
        require(samples.weight[idx].isFinite() && samples.weight[idx] >= 0f) {
            "cluster sample weights must be finite and non-negative"
        }
    }
    for (k in 1 until cluster.size) {
        require(cluster[k - 1] < cluster[k]) { "cluster indices must be strictly ascending" }
    }
    var bestIdx = cluster[0]
    var bestCost = Double.POSITIVE_INFINITY
    for (j in cluster) {
        val jLab = OkLab(samples.L[j], samples.a[j], samples.b[j])
        var cost = 0.0
        for (i in cluster) {
            cost += samples.weight[i].toDouble() *
                deltaOk(jLab, OkLab(samples.L[i], samples.a[i], samples.b[i])).toDouble()
        }
        if (cost < bestCost - 1e-9) {
            bestCost = cost
            bestIdx = j
        } else if (cost < bestCost + 1e-9 && j < bestIdx) {
            bestIdx = j
        }
    }
    return bestIdx
}
