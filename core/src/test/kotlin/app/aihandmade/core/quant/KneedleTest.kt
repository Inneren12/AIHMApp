package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for quant sub-commit 5: Kneedle auto-K selection.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * selectK builds the palette prefix K = k0..kTry, measures de95 = 95th-percentile per-sample nearest
 * error in CIEDE2000 ON CIE-Lab (OkLab -> linear -> Lab -> deltaE2000), forms the cumulative
 * error-reduction curve F, median-smooths it, normalises both axes and picks the knee by the maximal
 * deviation D = y - x. Fallbacks, in priority order: early_quality (de95 already <= target),
 * low_gain (KNEE_LOW_GAIN_STREAK consecutive gains < KNEE_TAU_GAIN), then k_max.
 *
 * Pinned here: de95 equals an independent Lab-CIEDE2000 p95 (this is the correctness fix — the metric
 * must NOT be CIEDE2000 on OkLab coordinates); de95 is monotone non-increasing; F telescopes to
 * de95[k0]-de95[k]; D == y-x; the knee / early_quality / k_max picks on concrete curves; determinism.
 */
class KneedleTest {

    private fun lab(L: Float, a: Float, b: Float) = OkLab(L, a, b).toLinearRgb().toLab()

    private fun samplesOf(ls: FloatArray): SampleSet =
        SampleSet(IntArray(ls.size) { it }, ls, FloatArray(ls.size) { 0f }, FloatArray(ls.size) { 0f },
            FloatArray(ls.size) { 1f }, ls.size, 1)

    private fun paletteOf(ls: FloatArray): Palette =
        Palette(ls, FloatArray(ls.size) { 0f }, FloatArray(ls.size) { 0f }, anchorCount = 0)

    /** three tight clusters at L=0.2/0.5/0.8 (10 samples each). */
    private fun threeClusterSamples(): SampleSet {
        val bases = floatArrayOf(0.2f, 0.5f, 0.8f)
        val ls = FloatArray(bases.size * 10)
        var idx = 0
        for (base in bases) for (j in 0 until 10) { ls[idx++] = base + 0.005f * (j - 4.5f) / 4.5f }
        return samplesOf(ls)
    }
    // first three colours cover the clusters; the last two sit far from every sample (no further gain).
    private fun threeClusterPalette() = paletteOf(floatArrayOf(0.2f, 0.5f, 0.8f, 0.95f, 0.05f))

    private fun singleClusterSamples(): SampleSet {
        val ls = FloatArray(10) { j -> 0.5f + 0.002f * (j - 4.5f) / 4.5f }
        return samplesOf(ls)
    }
    private fun singleClusterPalette() = paletteOf(floatArrayOf(0.5f, 0.95f, 0.9f, 0.05f))

    private fun uniformSamples(): SampleSet = samplesOf(FloatArray(20) { j -> j / 19f })
    private fun uniformPalette() = paletteOf(floatArrayOf(0.5f, 0.0f, 1.0f))

    @Test
    fun de95MatchesLabCiede2000P95() {
        // Independent recomputation at K=kTry: per-sample min CIEDE2000 over the full palette, p95
        // (linear-interpolated). Pins both the percentile and — crucially — that the distance is
        // CIEDE2000 on CIE-Lab, not on OkLab coordinates.
        val s = threeClusterSamples()
        val p = threeClusterPalette()
        val res = selectK(s, p, k0 = 1, kTry = p.size)
        val palLab = (0 until p.size).map { lab(p.L[it], p.a[it], p.b[it]) }
        val nearest = DoubleArray(s.size) { i ->
            val sl = lab(s.L[i], s.a[i], s.b[i])
            palLab.minOf { deltaE2000(sl, it) }
        }
        nearest.sort()
        val pos = 0.95 * (nearest.size - 1)
        val lo = pos.toInt(); val hi = minOf(nearest.size - 1, lo + 1); val w = pos - lo
        val expected = (nearest[lo] * (1.0 - w) + nearest[hi] * w).toFloat()
        val last = res.rows.last { it.k == p.size }
        assertEquals(expected, last.de95, 1e-3f, "de95 must be Lab-CIEDE2000 p95")
    }

    @Test
    fun de95IsMonotoneNonIncreasing() {
        val res = selectK(threeClusterSamples(), threeClusterPalette(), k0 = 1, kTry = 5)
        for (i in 1 until res.rows.size) {
            assertTrue(res.rows[i].de95 <= res.rows[i - 1].de95 + 1e-4f,
                "de95 must not rise as K grows (k=${res.rows[i].k})")
        }
    }

    @Test
    fun cumulativeGainTelescopes() {
        val res = selectK(threeClusterSamples(), threeClusterPalette(), k0 = 1, kTry = 5)
        val de950 = res.rows.first().de95
        for (row in res.rows) {
            assertEquals(de950 - row.de95, row.cumGain, 1e-3f, "F[k] must equal de95[k0]-de95[k] (k=${row.k})")
        }
        for (i in 1 until res.rows.size) {
            assertEquals(res.rows[i - 1].de95 - res.rows[i].de95, res.rows[i].gain, 1e-3f)
        }
        assertEquals(0f, res.rows.first().gain, 0f)
    }

    @Test
    fun deviationIsYminusX() {
        val k0 = 1; val kTry = 5
        val res = selectK(threeClusterSamples(), threeClusterPalette(), k0, kTry)
        val f0 = res.rows.first().cumGain
        val fEnd = res.rows.last().cumGain
        val denomY = if (kotlin.math.abs(fEnd - f0) < 1e-6f) 1f else (fEnd - f0)
        for (row in res.rows) {
            val x = if (kTry == k0) 1f else (row.k - k0).toFloat() / maxOf(1, kTry - k0)
            val y = if (kotlin.math.abs(denomY) < 1e-6f) 0f else (row.cumGain - f0) / denomY
            assertEquals(y - x, row.deviation, 1e-3f, "D must equal y-x (k=${row.k})")
        }
    }

    @Test
    fun picksKneeOnElbow() {
        val res = selectK(threeClusterSamples(), threeClusterPalette(), k0 = 1, kTry = 5)
        assertEquals(3, res.kStar, "knee of the 3-cluster curve is at K=3")
        assertEquals("knee", res.reason)
    }

    @Test
    fun picksEarlyQualityWhenAlreadyGoodEnough() {
        // A single tight cluster: one colour already drives de95 below target, so there is no knee and
        // early_quality must win at K=1 (this also pins early_quality OVER low_gain in priority).
        val res = selectK(singleClusterSamples(), singleClusterPalette(), k0 = 1, kTry = 4)
        assertEquals(1, res.kStar)
        assertEquals("early_quality", res.reason)
    }

    @Test
    fun fallsBackToKmaxWhenNoKneeAndAboveTarget() {
        val res = selectK(uniformSamples(), uniformPalette(), k0 = 1, kTry = 3)
        assertEquals(3, res.kStar)
        assertEquals("k_max", res.reason)
    }

    @Test
    fun deterministic() {
        val a = selectK(threeClusterSamples(), threeClusterPalette(), k0 = 1, kTry = 5)
        val b = selectK(threeClusterSamples(), threeClusterPalette(), k0 = 1, kTry = 5)
        assertEquals(a.kStar, b.kStar)
        assertEquals(a.reason, b.reason)
        assertEquals(a.rows.map { it.deviation }, b.rows.map { it.deviation })
    }

    @Test
    fun invalidInputsThrow() {
        val s = threeClusterSamples(); val p = threeClusterPalette()
        val emptyS = SampleSet(IntArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), 1, 1)
        val emptyP = Palette(FloatArray(0), FloatArray(0), FloatArray(0), 0)
        assertThrows(IllegalArgumentException::class.java) { selectK(emptyS, p, k0 = 1, kTry = 5) }
        assertThrows(IllegalArgumentException::class.java) { selectK(s, emptyP, k0 = 1, kTry = 0) }
        assertThrows(IllegalArgumentException::class.java) { selectK(s, p, k0 = 0, kTry = 5) }   // k0 < 1
        assertThrows(IllegalArgumentException::class.java) { selectK(s, p, k0 = 4, kTry = 3) }   // kTry < k0
        assertThrows(IllegalArgumentException::class.java) { selectK(s, p, k0 = 1, kTry = 6) }   // kTry > size
    }
}
