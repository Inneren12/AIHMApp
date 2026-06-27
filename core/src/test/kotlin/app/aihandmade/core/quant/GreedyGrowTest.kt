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
 * CONTRACT TEST for quant sub-commit 3d: greedy medoid growth (the assembled loop).
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * There is no external ground truth for a quantiser, so this pins behavioural INVARIANTS:
 *   - kTry=0 is a no-op;
 *   - the init palette is preserved as a prefix (greedy only ADDS);
 *   - growth is bounded by kTry;
 *   - the whole result is mutually >= S_MIN in CIEDE2000 (the add-gate holds globally);
 *   - every added colour is a real sample (a true medoid, not a synthetic centroid);
 *   - determinism;
 *   - residual importance does not increase;
 * plus two concrete scenarios (covers uncovered regions; skips an undersized-cluster bin).
 *
 * The add-gate is CIEDE2000 on Lab (via core/color); the hot loop underneath is OKLab-Euclidean.
 */
class GreedyGrowTest {

    private fun lab(L: Float, a: Float, b: Float) = OkLab(L, a, b).toLinearRgb().toLab()

    /** 36 achromatic samples in three L-clusters: 0.30 (x12), 0.50 (x12), 0.70 (x12). */
    private fun threeClusters(): SampleSet {
        val n = 36
        val L = FloatArray(n) { i -> when (i / 12) { 0 -> 0.30f; 1 -> 0.50f; else -> 0.70f } }
        return SampleSet(IntArray(n) { it }, L, FloatArray(n) { 0f }, FloatArray(n) { 0f }, FloatArray(n) { 1f }, n, 1)
    }

    /** init palette covering only the 0.30 cluster. */
    private fun initOne() = Palette(floatArrayOf(0.30f), floatArrayOf(0f), floatArrayOf(0f), 1)

    @Test
    fun kTryZeroReturnsInit() {
        val out = greedyGrow(threeClusters(), initOne(), kTry = 0, clusterMin = 2, binRadius = 1)
        val init = initOne()
        assertEquals(init.size, out.size, "kTry=0 must not add colours")
        assertTrue(out.L.contentEquals(init.L) && out.a.contentEquals(init.a) && out.b.contentEquals(init.b))
    }

    @Test
    fun initColoursArePreservedAsPrefix() {
        val init = initOne()
        val out = greedyGrow(threeClusters(), init, kTry = 3, clusterMin = 2, binRadius = 1)
        for (i in 0 until init.size) {
            assertEquals(init.L[i], out.L[i], 0f, "init colour $i (L) must be preserved")
            assertEquals(init.a[i], out.a[i], 0f)
            assertEquals(init.b[i], out.b[i], 0f)
        }
        assertEquals(init.anchorCount, out.anchorCount, "anchorCount carried through")
    }

    @Test
    fun growthIsBoundedByKTry() {
        val init = initOne()
        val out = greedyGrow(threeClusters(), init, kTry = 2, clusterMin = 2, binRadius = 1)
        assertTrue(out.size <= init.size + 2, "must not add more than kTry colours")
        assertTrue(out.size >= init.size, "must not lose colours")
    }

    @Test
    fun allPairsRespectSpreadGate() {
        val out = greedyGrow(threeClusters(), initOne(), kTry = 5, clusterMin = 2, binRadius = 1)
        for (i in 0 until out.size) {
            val li = lab(out.L[i], out.a[i], out.b[i])
            for (j in i + 1 until out.size) {
                val lj = lab(out.L[j], out.a[j], out.b[j])
                assertTrue(deltaE2000(li, lj) >= S_MIN - 1e-3, "palette[$i] vs [$j] must be >= S_MIN")
            }
        }
    }

    @Test
    fun addedColoursAreRealSamples() {
        val s = threeClusters()
        val init = initOne()
        val out = greedyGrow(s, init, kTry = 5, clusterMin = 2, binRadius = 1)
        for (k in init.size until out.size) {
            var found = false
            for (i in 0 until s.size) {
                if (s.L[i] == out.L[k] && s.a[i] == out.a[k] && s.b[i] == out.b[k]) { found = true; break }
            }
            assertTrue(found, "added colour $k must equal some sample (true medoid)")
        }
    }

    @Test
    fun deterministic() {
        val a = greedyGrow(threeClusters(), initOne(), kTry = 3, clusterMin = 2, binRadius = 1)
        val b = greedyGrow(threeClusters(), initOne(), kTry = 3, clusterMin = 2, binRadius = 1)
        assertEquals(a.size, b.size)
        assertTrue(a.L.contentEquals(b.L) && a.a.contentEquals(b.a) && a.b.contentEquals(b.b))
    }

    @Test
    fun residualDoesNotIncrease() {
        val s = threeClusters()
        val init = initOne()
        val out = greedyGrow(s, init, kTry = 3, clusterMin = 2, binRadius = 1)
        val before = residual(s, init).impTotal
        val after = residual(s, out).impTotal
        assertTrue(after <= before + 1e-9, "adding colours must not increase residual importance")
    }

    @Test
    fun coversUncoveredRegions() {
        // A(0.30) covered by init; greedy should add representatives for 0.50 and 0.70.
        val out = greedyGrow(threeClusters(), initOne(), kTry = 3, clusterMin = 2, binRadius = 1)
        assertEquals(3, out.size, "init(1) + two new cluster reps")
        var has05 = false; var has07 = false
        for (i in 0 until out.size) {
            if (out.L[i] == 0.50f) has05 = true
            if (out.L[i] == 0.70f) has07 = true
        }
        assertTrue(has05 && has07, "must add colours for the 0.50 and 0.70 clusters")
    }

    @Test
    fun skipsUndersizedClusterBins() {
        // 12 @ 0.30 (covered), 2 @ 0.50, 1 outlier @ 0.95. clusterMin=2 -> outlier bin (size 1) is skipped.
        val n = 15
        val L = FloatArray(n) { i -> when { i < 12 -> 0.30f; i < 14 -> 0.50f; else -> 0.95f } }
        val s = SampleSet(IntArray(n) { it }, L, FloatArray(n) { 0f }, FloatArray(n) { 0f }, FloatArray(n) { 1f }, n, 1)
        val out = greedyGrow(s, Palette(floatArrayOf(0.30f), floatArrayOf(0f), floatArrayOf(0f), 1), kTry = 3, clusterMin = 2, binRadius = 1)
        var hasOutlier = false; var has05 = false
        for (i in 0 until out.size) {
            if (out.L[i] == 0.95f) hasOutlier = true
            if (out.L[i] == 0.50f) has05 = true
        }
        assertTrue(!hasOutlier, "single-sample outlier bin must be skipped, not added")
        assertTrue(has05, "the 0.50 cluster (>= clusterMin) must be added")
    }

    @Test
    fun invalidInputsThrow() {
        val s = threeClusters()
        val init = initOne()
        val emptyS = SampleSet(IntArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), 1, 1)
        val emptyInit = Palette(FloatArray(0), FloatArray(0), FloatArray(0), 0)
        assertThrows(IllegalArgumentException::class.java) { greedyGrow(emptyS, init, kTry = 1) }
        assertThrows(IllegalArgumentException::class.java) { greedyGrow(s, emptyInit, kTry = 1) }
        assertThrows(IllegalArgumentException::class.java) { greedyGrow(s, init, kTry = -1) }
        assertThrows(IllegalArgumentException::class.java) { greedyGrow(s, init, kTry = 1, clusterMin = 0) }
        assertThrows(IllegalArgumentException::class.java) { greedyGrow(s, init, kTry = 1, binRadius = -1) }
    }
}
