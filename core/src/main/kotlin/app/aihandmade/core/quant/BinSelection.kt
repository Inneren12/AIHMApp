package app.aihandmade.core.quant

const val B_L = 24
const val B_A = 24
const val B_B = 24

/** The 24^3 OKLab bin a colour falls into. */
data class BinIndex(val l: Int, val a: Int, val b: Int)

/** Maps an OKLab coordinate to its bin. a/b are shifted by +0.5 before scaling; all axes clamp to [0,23]. */
fun binOf(okL: Float, okA: Float, okB: Float): BinIndex {
    require(okL.isFinite() && okA.isFinite() && okB.isFinite()) {
        "OKLab coordinates must be finite"
    }
    val l = (okL.coerceIn(0f, 0.9999f) * B_L).toInt().coerceIn(0, B_L - 1)
    val a = ((okA + 0.5f).coerceIn(0f, 0.9999f) * B_A).toInt().coerceIn(0, B_A - 1)
    val b = ((okB + 0.5f).coerceIn(0f, 0.9999f) * B_B).toInt().coerceIn(0, B_B - 1)
    return BinIndex(l, a, b)
}

data class BinSelection(val bin: BinIndex, val impSum: Double)

/** The occupied OKLab bin with the greatest total importance. Ties break to the smallest (l, then a, then b). */
fun selectImportantBin(samples: SampleSet, importance: DoubleArray): BinSelection {
    require(samples.size >= 1) { "samples must not be empty" }
    require(importance.size == samples.size) { "importance size must match sample count" }
    for (v in importance) {
        require(v.isFinite() && v >= 0.0) { "importance values must be finite and non-negative" }
    }

    val hist = HashMap<BinIndex, Double>()
    for (i in 0 until samples.size) {
        // QuantTrace: data-dependent bound `i < samples.size`; log every 1000 inner iterations.
        if (i % 1000 == 0) qtrace("selectImportantBin inner i=$i n=${samples.size} bins=${hist.size}")
        val bin = binOf(samples.L[i], samples.a[i], samples.b[i])
        hist[bin] = (hist[bin] ?: 0.0) + importance[i]
    }

    var bestBin: BinIndex? = null
    var bestImp = Double.NEGATIVE_INFINITY

    for ((bin, imp) in hist) {
        val better = when {
            imp > bestImp -> true
            imp < bestImp -> false
            else -> {
                val prev = bestBin!!
                bin.l < prev.l || (bin.l == prev.l && bin.a < prev.a) || (bin.l == prev.l && bin.a == prev.a && bin.b < prev.b)
            }
        }
        if (better) {
            bestBin = bin
            bestImp = imp
        }
    }

    return BinSelection(bestBin!!, bestImp)
}
