package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb

const val CLUSTER_MIN = 8

fun greedyGrow(
    samples: SampleSet,
    init: Palette,
    kTry: Int,
    clusterMin: Int = CLUSTER_MIN,
    binRadius: Int = CLUSTER_BIN_RADIUS,
): Palette {
    require(samples.size >= 1) { "samples must not be empty" }
    require(init.size >= 1) { "init palette must not be empty" }
    require(kTry >= 0) { "kTry must be >= 0" }
    require(clusterMin >= 1) { "clusterMin must be >= 1" }

    if (kTry == 0) return init

    val palL = init.L.toMutableList()
    val palA = init.a.toMutableList()
    val palB = init.b.toMutableList()

    // CIE-Lab cache for the CIEDE2000 add-gate (one entry per palette colour)
    val paletteLab = init.L.indices.mapTo(mutableListOf()) { i ->
        OkLab(init.L[i], init.a[i], init.b[i]).toLinearRgb().toLab()
    }

    val excluded = mutableSetOf<BinIndex>()
    var added = 0

    while (added < kTry) {
        val current = Palette(palL.toFloatArray(), palA.toFloatArray(), palB.toFloatArray(), init.anchorCount)
        val res = residual(samples, current)

        val masked = DoubleArray(samples.size) { i ->
            if (binOf(samples.L[i], samples.a[i], samples.b[i]) in excluded) 0.0
            else res.importance[i]
        }

        val sel = selectImportantBin(samples, masked)
        if (sel.impSum <= 0.0) break

        val cluster = collectCluster(samples, sel.bin, binRadius)
        if (cluster.size < clusterMin) {
            excluded += sel.bin
            continue
        }

        val medoidIdx = weightedMedoid(samples, cluster)
        val medoidLab = OkLab(samples.L[medoidIdx], samples.a[medoidIdx], samples.b[medoidIdx]).toLinearRgb().toLab()

        // CIEDE2000 add-gate: reject if too close to any existing palette entry
        if (paletteLab.minOf { deltaE2000(medoidLab, it) } < S_MIN) {
            excluded += sel.bin
            continue
        }

        palL += samples.L[medoidIdx]
        palA += samples.a[medoidIdx]
        palB += samples.b[medoidIdx]
        paletteLab += medoidLab
        added++
    }

    return Palette(palL.toFloatArray(), palA.toFloatArray(), palB.toFloatArray(), init.anchorCount)
}
