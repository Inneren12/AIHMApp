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
 * CONTRACT TEST for quant sub-commit 4: constrained Lloyd k-medoid palette refinement.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * Refinement assigns each sample to its nearest palette colour (OKLab-Euclidean), then moves each
 * NON-anchor colour to the weighted medoid of its assigned samples — accepting the move only if it
 * keeps every pair >= S_MIN in CIEDE2000. Anchors never move; size never changes; error never rises.
 *
 * Invariants pinned here: passes=0 no-op; anchors fixed; size preserved; whole palette >= S_MIN;
 * residual importance does not increase (even when a non-sample colour beats every sample medoid);
 * moved colours are real samples; determinism; plus a concrete
 * "move an off-centre colour to its cluster medoid" scenario.
 */
class RefinePaletteTest {

    private fun lab(L: Float, a: Float, b: Float) = OkLab(L, a, b).toLinearRgb().toLab()

    /** anchors black(0.05)/white(0.95); one off-centre non-anchor at 0.45; a mid cluster around 0.50. */
    private fun midClusterSamples(): SampleSet {
        val midL = floatArrayOf(0.48f, 0.50f, 0.52f, 0.50f, 0.49f, 0.51f)
        val n = 24 + midL.size
        val L = FloatArray(n) { i -> when { i < 12 -> 0.05f; i < 24 -> 0.95f; else -> midL[i - 24] } }
        return SampleSet(IntArray(n) { it }, L, FloatArray(n) { 0f }, FloatArray(n) { 0f }, FloatArray(n) { 1f }, n, 1)
    }

    private fun offCentrePalette() =
        Palette(floatArrayOf(0.05f, 0.95f, 0.45f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), anchorCount = 2)

    @Test
    fun passesZeroIsNoOp() {
        val p = offCentrePalette()
        val out = refinePalette(midClusterSamples(), p, passes = 0)
        assertTrue(out.L.contentEquals(p.L) && out.a.contentEquals(p.a) && out.b.contentEquals(p.b))
    }

    @Test
    fun anchorsAreFixed() {
        val p = offCentrePalette()
        val out = refinePalette(midClusterSamples(), p, passes = 2)
        for (i in 0 until p.anchorCount) {
            assertEquals(p.L[i], out.L[i], 0f, "anchor $i (L) must not move")
            assertEquals(p.a[i], out.a[i], 0f)
            assertEquals(p.b[i], out.b[i], 0f)
        }
        assertEquals(p.anchorCount, out.anchorCount)
    }

    @Test
    fun sizeIsPreserved() {
        val p = offCentrePalette()
        val out = refinePalette(midClusterSamples(), p, passes = 2)
        assertEquals(p.size, out.size, "refinement moves colours, never adds or removes")
    }

    @Test
    fun allPairsRespectSpreadGate() {
        val out = refinePalette(midClusterSamples(), offCentrePalette(), passes = 3)
        for (i in 0 until out.size) {
            val li = lab(out.L[i], out.a[i], out.b[i])
            for (j in i + 1 until out.size) {
                assertTrue(deltaE2000(li, lab(out.L[j], out.a[j], out.b[j])) >= S_MIN - 1e-3,
                    "palette[$i] vs [$j] must stay >= S_MIN")
            }
        }
    }

    @Test
    fun errorDoesNotIncrease() {
        val s = midClusterSamples()
        val p = offCentrePalette()
        val out = refinePalette(s, p, passes = 3)
        assertTrue(residual(s, out).impTotal <= residual(s, p).impTotal + 1e-9,
            "refinement must not increase residual importance")
    }

    @Test
    fun centroidColourDoesNotMoveToWorseMedoid() {
        // A non-sample colour at the centroid of a symmetric cluster represents that cluster better
        // than ANY single sample (every vertex-medoid has higher total weighted distance). The error
        // gate must therefore reject the medoid move and leave the colour in place.
        val l0 = 0.6f
        val s = SampleSet(
            IntArray(3) { it },
            floatArrayOf(l0, l0, l0),
            floatArrayOf(0f, -0.0866f, 0.0866f),
            floatArrayOf(0.1f, -0.05f, -0.05f),
            floatArrayOf(1f, 1f, 1f),
            3, 1
        )
        val p = Palette(floatArrayOf(0.05f, 0.95f, l0), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), anchorCount = 2)
        val out = refinePalette(s, p, passes = 2)
        assertEquals(l0, out.L[2], 0f, "centroid colour must not move to a worse vertex medoid")
        assertEquals(0f, out.a[2], 0f); assertEquals(0f, out.b[2], 0f)
        assertTrue(residual(s, out).impTotal <= residual(s, p).impTotal + 1e-9,
            "rejecting the worse medoid keeps residual flat")
    }

    @Test
    fun movedColoursAreRealSamples() {
        val s = midClusterSamples()
        val out = refinePalette(s, offCentrePalette(), passes = 2)
        for (c in 2 until out.size) { // non-anchors
            var found = false
            for (i in 0 until s.size) {
                if (s.L[i] == out.L[c] && s.a[i] == out.a[c] && s.b[i] == out.b[c]) { found = true; break }
            }
            assertTrue(found, "non-anchor colour $c must equal some sample (a medoid)")
        }
    }

    @Test
    fun movesOffCentreColourToClusterMedoid() {
        // non-anchor starts at 0.45; the mid cluster's medoid is 0.50 -> it must move there.
        val out = refinePalette(midClusterSamples(), offCentrePalette(), passes = 2)
        assertEquals(0.50f, out.L[2], 0f, "off-centre colour must move to the cluster medoid (0.50)")
        assertEquals(0f, out.a[2], 0f); assertEquals(0f, out.b[2], 0f)
    }

    @Test
    fun deterministic() {
        val a = refinePalette(midClusterSamples(), offCentrePalette(), passes = 2)
        val b = refinePalette(midClusterSamples(), offCentrePalette(), passes = 2)
        assertTrue(a.L.contentEquals(b.L) && a.a.contentEquals(b.a) && a.b.contentEquals(b.b))
    }

    @Test
    fun invalidInputsThrow() {
        val s = midClusterSamples()
        val p = offCentrePalette()
        val emptyS = SampleSet(IntArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), 1, 1)
        val emptyP = Palette(FloatArray(0), FloatArray(0), FloatArray(0), 0)
        assertThrows(IllegalArgumentException::class.java) { refinePalette(emptyS, p, passes = 1) }
        assertThrows(IllegalArgumentException::class.java) { refinePalette(s, emptyP, passes = 1) }
        assertThrows(IllegalArgumentException::class.java) { refinePalette(s, p, passes = -1) }
    }
}
