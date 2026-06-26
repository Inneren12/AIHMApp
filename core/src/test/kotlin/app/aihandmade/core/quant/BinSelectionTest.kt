package app.aihandmade.core.quant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for quant sub-commit 3b: OKLab 24^3 binning + importance bin selection.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here. No colour maths and no distance maths here at all — this is pure binning/argmax on
 * the OKLab coordinates already carried by the sample set.
 */
class BinSelectionTest {

    @Test
    fun binOfMapsKnownPoints() {
        assertEquals(BinIndex(12, 12, 12), binOf(0.50f, 0.00f, 0.00f))
        assertEquals(BinIndex(4, 12, 12), binOf(0.20f, 0.00f, 0.00f))
        assertEquals(BinIndex(19, 12, 12), binOf(0.80f, 0.00f, 0.00f))
        assertEquals(BinIndex(7, 14, 12), binOf(0.30f, 0.10f, 0.00f))
    }

    @Test
    fun binOfClampsToValidRange() {
        // a/b are shifted by +0.5; everything is coerced into [0, 23] on each axis.
        assertEquals(BinIndex(0, 0, 0), binOf(0.0f, -0.5f, -0.5f))
        assertEquals(BinIndex(23, 23, 23), binOf(0.999f, 0.49f, 0.49f))
        assertEquals(BinIndex(0, 0, 0), binOf(-1f, -1f, -1f))
        assertEquals(BinIndex(23, 23, 23), binOf(2f, 2f, 2f))
        val bi = binOf(0.37f, -0.13f, 0.21f)
        assertTrue(bi.l in 0..23 && bi.a in 0..23 && bi.b in 0..23)
    }

    // Hand-built set:
    //   s0,s1 -> bin (12,12,12) total imp 3.0
    //   s2    -> bin ( 4,12,12)       imp 5.0
    //   s3    -> bin (19,12,12)       imp 5.0   (ties s2 at 5.0)
    //   s4    -> bin ( 7,14,12)       imp 0.0
    //   s5    -> bin ( 7,12,12)       imp 1.0
    private val samples = SampleSet(
        index = intArrayOf(0, 1, 2, 3, 4, 5),
        L = floatArrayOf(0.50f, 0.50f, 0.20f, 0.80f, 0.30f, 0.30f),
        a = floatArrayOf(0.00f, 0.00f, 0.00f, 0.00f, 0.10f, 0.00f),
        b = floatArrayOf(0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f),
        weight = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f),
        sourceWidth = 6,
        sourceHeight = 1,
    )
    private val importance = doubleArrayOf(1.0, 2.0, 5.0, 5.0, 0.0, 1.0)

    @Test
    fun selectsMaxImportanceBinTieBrokenBySmallestIndex() {
        val sel = selectImportantBin(samples, importance)
        // max impSum = 5.0, tied between (4,12,12) and (19,12,12); smallest l wins -> (4,12,12).
        assertEquals(BinIndex(4, 12, 12), sel.bin, "tie must break to the smallest (l,a,b)")
        assertEquals(5.0, sel.impSum, 1e-12, "selected impSum")
    }

    @Test
    fun selectedBinHasMaximalImportance() {
        // self-check: recompute the histogram and confirm no occupied bin beats the selection.
        val sel = selectImportantBin(samples, importance)
        val hist = HashMap<BinIndex, Double>()
        for (i in 0 until samples.size) {
            val bin = binOf(samples.L[i], samples.a[i], samples.b[i])
            hist[bin] = (hist[bin] ?: 0.0) + importance[i]
        }
        for ((bin, imp) in hist) {
            assertTrue(sel.impSum >= imp - 1e-12, "selected impSum must be >= bin $bin ($imp)")
        }
        assertEquals(sel.impSum, hist[sel.bin]!!, 1e-12, "reported impSum matches the bin's total")
    }

    @Test
    fun uniqueMaxIsSelected() {
        val imp2 = doubleArrayOf(1.0, 2.0, 5.0, 9.0, 0.0, 1.0) // (19,12,12) now unique max at 9.0
        val sel = selectImportantBin(samples, imp2)
        assertEquals(BinIndex(19, 12, 12), sel.bin)
        assertEquals(9.0, sel.impSum, 1e-12)
    }

    @Test
    fun deterministic() {
        val a = selectImportantBin(samples, importance)
        val b = selectImportantBin(samples, importance)
        assertEquals(a.bin, b.bin)
        assertEquals(a.impSum, b.impSum, 0.0)
    }

    @Test
    fun invalidInputsThrow() {
        val emptySamples = SampleSet(IntArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), 1, 1)
        assertThrows(IllegalArgumentException::class.java) { selectImportantBin(emptySamples, DoubleArray(0)) }
        // importance length must match sample count
        assertThrows(IllegalArgumentException::class.java) { selectImportantBin(samples, doubleArrayOf(1.0, 2.0)) }
    }
}
