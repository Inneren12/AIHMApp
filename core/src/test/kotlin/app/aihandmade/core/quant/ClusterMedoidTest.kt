package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaOk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for quant sub-commit 3c: cluster collection + weighted medoid.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * The medoid runs in OKLab-Euclidean (the quantiser's hot space) via core/color `deltaOk`. There is
 * NO CIEDE2000 here and NO local distance maths. collectCluster is pure bin arithmetic (Chebyshev
 * neighbourhood on the 24^3 grid from 3b).
 */
class ClusterMedoidTest {

    private fun ok(L: Float, a: Float, bb: Float) = OkLab(L, a, bb)

    // ---------- weightedMedoid ----------

    @Test
    fun medoidReferenceUniqueMin() {
        // L-only cluster, weights [1,2,1,1]; costs [0.25,0.20,0.35,0.40] -> index 1.
        val s = SampleSet(
            index = intArrayOf(0, 1, 2, 3),
            L = floatArrayOf(0.50f, 0.55f, 0.60f, 0.45f),
            a = floatArrayOf(0f, 0f, 0f, 0f),
            b = floatArrayOf(0f, 0f, 0f, 0f),
            weight = floatArrayOf(1f, 2f, 1f, 1f),
            sourceWidth = 4, sourceHeight = 1,
        )
        assertEquals(1, weightedMedoid(s, intArrayOf(0, 1, 2, 3)))
    }

    @Test
    fun medoidMinimisesWeightedDistanceSelfCheck() {
        val s = SampleSet(
            index = intArrayOf(0, 1, 2, 3, 4),
            L = floatArrayOf(0.30f, 0.32f, 0.28f, 0.31f, 0.70f),
            a = floatArrayOf(0.10f, 0.08f, 0.12f, 0.09f, -0.10f),
            b = floatArrayOf(-0.05f, -0.04f, -0.06f, -0.05f, 0.10f),
            weight = floatArrayOf(1f, 2f, 1f, 1.5f, 1f),
            sourceWidth = 5, sourceHeight = 1,
        )
        val cluster = intArrayOf(0, 1, 2, 3, 4)
        val medoid = weightedMedoid(s, cluster)
        // independently recompute the weighted argmin (tie -> smallest index) via core/color deltaOk
        var bestIdx = -1
        var bestCost = Double.POSITIVE_INFINITY
        for (j in cluster) {
            var cost = 0.0
            for (i in cluster) {
                cost += s.weight[i].toDouble() *
                    deltaOk(ok(s.L[j], s.a[j], s.b[j]), ok(s.L[i], s.a[i], s.b[i])).toDouble()
            }
            if (cost < bestCost - 1e-9) {
                bestCost = cost; bestIdx = j
            }
        }
        assertEquals(bestIdx, medoid, "medoid must be the weighted argmin")
        assertEquals(3, medoid, "reference: index 3")
    }

    @Test
    fun medoidTieBreaksToSmallestIndex() {
        // s0 == s1 exactly -> equal cost -> smallest index 0.
        val s = SampleSet(
            index = intArrayOf(0, 1, 2),
            L = floatArrayOf(0.50f, 0.50f, 0.90f),
            a = floatArrayOf(0f, 0f, 0f),
            b = floatArrayOf(0f, 0f, 0f),
            weight = floatArrayOf(1f, 1f, 1f),
            sourceWidth = 3, sourceHeight = 1,
        )
        assertEquals(0, weightedMedoid(s, intArrayOf(0, 1, 2)))
    }

    @Test
    fun singletonClusterReturnsItself() {
        val s = SampleSet(
            index = intArrayOf(0, 1),
            L = floatArrayOf(0.3f, 0.7f), a = floatArrayOf(0f, 0f), b = floatArrayOf(0f, 0f),
            weight = floatArrayOf(1f, 1f), sourceWidth = 2, sourceHeight = 1,
        )
        assertEquals(1, weightedMedoid(s, intArrayOf(1)))
    }

    @Test
    fun medoidRejectsEmptyCluster() {
        val s = SampleSet(
            index = intArrayOf(0), L = floatArrayOf(0.5f), a = floatArrayOf(0f), b = floatArrayOf(0f),
            weight = floatArrayOf(1f), sourceWidth = 1, sourceHeight = 1,
        )
        assertThrows(IllegalArgumentException::class.java) { weightedMedoid(s, IntArray(0)) }
    }

    // ---------- collectCluster ----------

    @Test
    fun collectClusterGathersChebyshevNeighbourhood() {
        // center (12,12,12), radius 2; s3 at bin (4,..) is out, the rest are in. Result ascending.
        val s = SampleSet(
            index = intArrayOf(0, 1, 2, 3, 4),
            L = floatArrayOf(0.50f, 0.55f, 0.60f, 0.20f, 0.50f),
            a = floatArrayOf(0f, 0f, 0f, 0f, 0.10f),
            b = floatArrayOf(0f, 0f, 0f, 0f, 0f),
            weight = floatArrayOf(1f, 1f, 1f, 1f, 1f),
            sourceWidth = 5, sourceHeight = 1,
        )
        assertArrayEquals(intArrayOf(0, 1, 2, 4), collectCluster(s, BinIndex(12, 12, 12), binRadius = 2))
    }

    @Test
    fun collectClusterRadiusZeroIsSingleBin() {
        val s = SampleSet(
            index = intArrayOf(0, 1),
            L = floatArrayOf(0.50f, 0.55f), // bins (12,..) and (13,..)
            a = floatArrayOf(0f, 0f), b = floatArrayOf(0f, 0f),
            weight = floatArrayOf(1f, 1f), sourceWidth = 2, sourceHeight = 1,
        )
        assertArrayEquals(intArrayOf(0), collectCluster(s, BinIndex(12, 12, 12), binRadius = 0))
    }

    @Test
    fun deterministic() {
        val s = SampleSet(
            index = intArrayOf(0, 1, 2),
            L = floatArrayOf(0.5f, 0.55f, 0.6f), a = floatArrayOf(0f, 0f, 0f), b = floatArrayOf(0f, 0f, 0f),
            weight = floatArrayOf(1f, 2f, 1f), sourceWidth = 3, sourceHeight = 1,
        )
        assertEquals(weightedMedoid(s, intArrayOf(0, 1, 2)), weightedMedoid(s, intArrayOf(0, 1, 2)))
        assertArrayEquals(
            collectCluster(s, BinIndex(12, 12, 12), binRadius = 1),
            collectCluster(s, BinIndex(12, 12, 12), binRadius = 1),
        )
    }
}
