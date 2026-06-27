package app.aihandmade.core.quant

import app.aihandmade.core.color.Lab
import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.deltaSqOk
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb

const val REFINE_PASSES = 2

fun refinePalette(samples: SampleSet, palette: Palette, passes: Int = REFINE_PASSES): Palette {
    require(samples.size >= 1) { "samples must be non-empty" }
    require(palette.size >= 1) { "palette must be non-empty" }
    require(passes >= 0) { "passes must be >= 0" }

    val palL = palette.L.copyOf()
    val palA = palette.a.copyOf()
    val palB = palette.b.copyOf()
    val size = palette.size
    val n = samples.size

    if (passes == 0) return Palette(palL, palA, palB, palette.anchorCount)

    val paletteLab = Array<Lab>(size) { c ->
        OkLab(palL[c], palA[c], palB[c]).toLinearRgb().toLab()
    }

    repeat(passes) {
        val nearest = IntArray(n) { i ->
            val si = OkLab(samples.L[i], samples.a[i], samples.b[i])
            var bestC = 0
            var bestSq = Float.POSITIVE_INFINITY
            for (c in 0 until size) {
                val sq = deltaSqOk(si, OkLab(palL[c], palA[c], palB[c]))
                if (sq < bestSq) {
                    bestSq = sq
                    bestC = c
                }
            }
            bestC
        }

        for (c in palette.anchorCount until size) {
            val cluster = (0 until n).filter { nearest[it] == c }.toIntArray()
            if (cluster.isEmpty()) continue

            val candIdx = weightedMedoid(samples, cluster)
            val candL = samples.L[candIdx]
            val candA = samples.a[candIdx]
            val candB = samples.b[candIdx]

            if (candL == palL[c] && candA == palA[c] && candB == palB[c]) continue

            val candLab = OkLab(candL, candA, candB).toLinearRgb().toLab()

            var minDelta = Double.POSITIVE_INFINITY
            for (cp in 0 until size) {
                if (cp == c) continue
                val d = deltaE2000(candLab, paletteLab[cp])
                if (d < minDelta) minDelta = d
            }

            if (minDelta >= S_MIN) {
                palL[c] = candL
                palA[c] = candA
                palB[c] = candB
                paletteLab[c] = candLab
            }
        }
    }

    return Palette(palL, palA, palB, palette.anchorCount)
}
