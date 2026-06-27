package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb

const val CLUSTER_MIN = 8

/**
 * Grows [init] by up to [kTry] colours chosen greedily from [samples].
 *
 * Each candidate medoid is accepted only if its CIEDE2000 distance to every colour already in the
 * palette is >= [S_MIN] (the same threshold used by `initPalette`). This gate is evaluated over
 * the growing palette, so every newly added colour is at least [S_MIN] away from all previously
 * accepted colours.
 *
 * The initial palette is preserved verbatim as a prefix of the result. Consequently, global
 * pairwise [S_MIN] separation of the final palette is **only guaranteed** when [init] already
 * satisfies that separation internally — the function does not validate the init palette's spread.
 *
 * When the selected bin's cluster is smaller than [clusterMin], or its medoid fails the spread
 * gate, that specific bin is permanently added to an exclusion set so the loop never re-selects
 * it. Only the selected bin itself is excluded; neighbouring bins in the same cluster are not.
 *
 * @param samples  non-empty set of OKLab samples with importance weights
 * @param init     starting palette (carried through unchanged as a prefix); must be non-empty
 * @param kTry     maximum number of colours to add; 0 returns [init] immediately
 * @param clusterMin minimum cluster size (in samples) for a bin to spawn a colour
 * @param binRadius  Chebyshev radius around the selected bin used by [collectCluster]
 */
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
    require(binRadius >= 0) { "binRadius must be >= 0" }

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
