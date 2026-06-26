package app.aihandmade.core.quant

import app.aihandmade.core.color.Lab
import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

const val K0_TARGET = 14
const val S_MIN = 3.5
const val NEUTRAL_CHROMA_MAX = 0.02
const val NEUTRAL_TARGET_L = 0.55
const val DARK_LIGHT_FRACTION = 20

/** A palette as OKLab colours (structure-of-arrays). The first [anchorCount] entries are the
 *  protected anchors (black, white, neutral); the rest are spread-gated fills. */
class Palette(
    val L: FloatArray, val a: FloatArray, val b: FloatArray,
    val anchorCount: Int,
) {
    val size: Int get() = L.size
    init {
        require(L.size == a.size && a.size == b.size) { "palette arrays must have equal size" }
        require(anchorCount in 0..size) { "anchorCount out of range" }
    }
}

fun initPalette(samples: SampleSet, k0: Int = K0_TARGET): Palette {
    require(samples.size >= 1) { "samples must be non-empty" }
    require(k0 >= 1) { "k0 must be >= 1" }

    val n = samples.size

    fun chroma(p: Int): Double {
        val a = samples.a[p].toDouble()
        val b = samples.b[p].toDouble()
        return sqrt(a * a + b * b)
    }

    val fraction = max(n / DARK_LIGHT_FRACTION, 1)
    val sortedByLAsc = (0 until n).sortedWith(compareBy({ samples.L[it] }, { it }))

    val black = sortedByLAsc.take(fraction)
        .minWithOrNull(compareBy({ chroma(it) }, { it }))!!

    val white = sortedByLAsc.takeLast(fraction)
        .minWithOrNull(compareBy({ chroma(it) }, { it }))!!

    val neutralPool = (0 until n).filter {
        chroma(it) <= NEUTRAL_CHROMA_MAX
    }.ifEmpty { (0 until n).toList() }
    val neutral = neutralPool.minWithOrNull(
        compareBy({ abs(samples.L[it].toDouble() - NEUTRAL_TARGET_L) }, { it })
    )!!

    fun sampleToLab(p: Int): Lab =
        OkLab(samples.L[p], samples.a[p], samples.b[p]).toLinearRgb().toLab()

    val nominatedAnchors = listOf(black, white, neutral).distinct()

    val anchorPositions = mutableListOf<Int>()
    val anchorLabs = mutableListOf<Lab>()

    for (p in nominatedAnchors) {
        val lab = sampleToLab(p)
        if (anchorLabs.all { existing -> deltaE2000(lab, existing) >= S_MIN }) {
            anchorPositions.add(p)
            anchorLabs.add(lab)
        }
    }
    val anchorCount = anchorPositions.size

    val paletteL = mutableListOf<Float>()
    val paletteA = mutableListOf<Float>()
    val paletteB = mutableListOf<Float>()
    val paletteLab = mutableListOf<Lab>()

    for (p in anchorPositions) {
        paletteL.add(samples.L[p])
        paletteA.add(samples.a[p])
        paletteB.add(samples.b[p])
    }
    paletteLab.addAll(anchorLabs)

    val nominatedAnchorSet = nominatedAnchors.toHashSet()
    val candidates = (0 until n)
        .filter { it !in nominatedAnchorSet }
        .sortedWith(compareByDescending<Int> { samples.weight[it] }.thenBy { it })

    for (p in candidates) {
        if (paletteL.size >= k0) break
        val candidateLab = sampleToLab(p)
        if (paletteLab.all { existing -> deltaE2000(candidateLab, existing) >= S_MIN }) {
            paletteL.add(samples.L[p])
            paletteA.add(samples.a[p])
            paletteB.add(samples.b[p])
            paletteLab.add(candidateLab)
        }
    }

    return Palette(
        L = paletteL.toFloatArray(),
        a = paletteA.toFloatArray(),
        b = paletteB.toFloatArray(),
        anchorCount = anchorCount,
    )
}
