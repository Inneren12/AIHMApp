package app.aihandmade.core.quant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * CONTRACT TEST for the weightedMedoid O(n^2) cap+subsample fix.
 *
 * Implement so this compiles and passes UNCHANGED. Do not edit or weaken anything here.
 * The existing ClusterMedoidTest / ClusterMedoidEdgeTest MUST also stay byte-identical and green:
 * for any cluster of size <= MEDOID_EXACT_CAP the medoid must be computed EXACTLY as before (the cap
 * path is a no-op below the cap). This file only adds the large-cluster guarantees.
 */
class WeightedMedoidCapTest {

    // Sizes chosen to straddle the cap. If MEDOID_EXACT_CAP is retuned outside (40, 20000), these
    // fire to say "update the test sizes" rather than silently exercising the wrong path.
    private val smallSize = 40
    private val hugeSize = 20_000

    @Test
    fun capConstantStraddledByTestSizes() {
        assertTrue(smallSize <= MEDOID_EXACT_CAP) { "test assumes smallSize <= cap; retune test" }
        assertTrue(hugeSize > MEDOID_EXACT_CAP) { "test assumes hugeSize > cap; retune test" }
    }

    /** Build a SampleSet of [n] ascending samples; colour per index via [lab]; uniform weight 1.0. */
    private fun sampleSet(n: Int, lab: (Int) -> Triple<Float, Float, Float>): SampleSet {
        val index = IntArray(n) { it }
        val L = FloatArray(n)
        val a = FloatArray(n)
        val b = FloatArray(n)
        val w = FloatArray(n) { 1f }
        for (i in 0 until n) {
            val (li, ai, bi) = lab(i)
            L[i] = li; a[i] = ai; b[i] = bi
        }
        return SampleSet(index, L, a, b, w, sourceWidth = n, sourceHeight = 1)
    }

    /**
     * Huge cluster: a ~5% CONTIGUOUS block of far outliers near L=0.9, the rest a tight blob at L=0.5.
     * With uniform weights the weighted medoid (min sum of deltaOk to all members) must be a BLOB
     * point, never an outlier. A contiguous (non-periodic) outlier block makes this independent of how
     * the subset is drawn. Deterministic spread (no RNG in the test) keeps points distinct.
     */
    private fun blobPlusOutliers(n: Int): SampleSet {
        val outliers = n / 20
        return sampleSet(n) { i ->
            if (i < outliers) {
                Triple(0.90f + (i % 5) * 0.002f, 0.10f, 0.10f)     // far outlier
            } else {
                Triple(0.50f + ((i % 7) - 3) * 0.001f, 0f, 0f)      // tight blob around L=0.5
            }
        }
    }

    @Test
    fun hugeClusterReturnsBlobMedoidQuickly() {
        val s = blobPlusOutliers(hugeSize)
        val cluster = IntArray(hugeSize) { it }                    // all indices, strictly ascending

        val medoid = assertTimeoutPreemptively<Int>(Duration.ofSeconds(10)) {
            weightedMedoid(s, cluster)
        }

        assertTrue(medoid in 0 until hugeSize)                     // a real cluster member
        assertTrue(s.L[medoid] < 0.6f) {                           // a blob point, not an outlier
            "expected a blob medoid near L=0.5 but got L=" + s.L[medoid]
        }
    }

    @Test
    fun hugeClusterMedoidIsDeterministic() {
        val s = blobPlusOutliers(hugeSize)
        val cluster = IntArray(hugeSize) { it }
        assertEquals(weightedMedoid(s, cluster), weightedMedoid(s, cluster))
    }

    /**
     * Small cluster (<= cap) takes the EXACT path. Uniform weights + collinear points on the L axis:
     * the medoid (min sum of |dL|) is the geometric middle, index 2 — invariant to deltaOk vs deltaSqOk.
     * This pins that the cap path is a no-op below the cap.
     */
    @Test
    fun smallClusterExactMedoidIsGeometricMiddle() {
        val s = sampleSet(5) { i -> Triple(0.1f + i * 0.2f, 0f, 0f) }  // L = 0.1,0.3,0.5,0.7,0.9
        val cluster = intArrayOf(0, 1, 2, 3, 4)
        assertEquals(2, weightedMedoid(s, cluster))
    }
}
