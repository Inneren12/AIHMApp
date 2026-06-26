package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaOk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for quant sub-commit 3a: per-sample residual + importance.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * `residual(samples, palette)` computes, for every sample, the OKLab-Euclidean distance to the
 * nearest palette colour (via core/color `deltaOk`), the importance error*weight, the total
 * importance, and the median / p95 of the errors (linear-interpolation percentile).
 *
 * The hot loop is OKLab-Euclidean by design (this is the quantiser's inner space). There is NO
 * CIEDE2000 here and NO local distance maths — only core/color `deltaOk` / `deltaSqOk`.
 *
 * Most checks are self-verifying (they recompute the expectation from the inputs with core/color),
 * plus a few concrete reference numbers as anchors.
 */
class ResidualTest {

    // Hand-built sample set; palette[0]==s0, palette[1]==s2, palette[2]==s3 exactly.
    private val samples = SampleSet(
        index = intArrayOf(0, 1, 2, 3, 4),
        L = floatArrayOf(0.20f, 0.50f, 0.80f, 0.50f, 0.30f),
        a = floatArrayOf(0.00f, 0.10f, -0.05f, 0.00f, 0.08f),
        b = floatArrayOf(0.00f, -0.05f, 0.10f, 0.00f, 0.02f),
        weight = floatArrayOf(1f, 2f, 1.5f, 3f, 1f),
        sourceWidth = 5,
        sourceHeight = 1,
    )
    private val palette = Palette(
        L = floatArrayOf(0.20f, 0.80f, 0.50f),
        a = floatArrayOf(0.00f, -0.05f, 0.00f),
        b = floatArrayOf(0.00f, 0.10f, 0.00f),
        anchorCount = 3,
    )

    private fun ok(L: Float, a: Float, bb: Float) = OkLab(L, a, bb)

    @Test
    fun errorsAreNearestOkDistance() {
        val r = residual(samples, palette)
        assertEquals(samples.size, r.errors.size, "one error per sample")
        for (i in 0 until samples.size) {
            val si = ok(samples.L[i], samples.a[i], samples.b[i])
            var expected = Float.POSITIVE_INFINITY
            for (c in 0 until palette.size) {
                val d = deltaOk(si, ok(palette.L[c], palette.a[c], palette.b[c]))
                if (d < expected) expected = d
            }
            assertEquals(expected, r.errors[i], 1e-6f, "error[$i] must equal nearest OKLab distance")
        }
    }

    @Test
    fun importanceIsErrorTimesWeight() {
        val r = residual(samples, palette)
        for (i in 0 until samples.size) {
            assertEquals(
                r.errors[i].toDouble() * samples.weight[i].toDouble(),
                r.importance[i], 0.0, "importance[$i] = error * weight"
            )
        }
    }

    @Test
    fun impTotalIsSumOfImportance() {
        val r = residual(samples, palette)
        var sum = 0.0
        for (i in 0 until samples.size) sum += r.importance[i]
        assertEquals(sum, r.impTotal, 1e-12, "impTotal = sum of importance")
    }

    @Test
    fun zeroErrorWhenSampleEqualsPaletteColour() {
        val r = residual(samples, palette)
        // s0==P0, s2==P1, s3==P2 -> exact zero error and zero importance.
        assertEquals(0f, r.errors[0], 0f); assertEquals(0.0, r.importance[0], 0.0)
        assertEquals(0f, r.errors[2], 0f); assertEquals(0.0, r.importance[2], 0.0)
        assertEquals(0f, r.errors[3], 0f); assertEquals(0.0, r.importance[3], 0.0)
    }

    @Test
    fun percentilesUseLinearInterpolation() {
        val r = residual(samples, palette)
        // errors = [0, 0.1118034, 0, 0, 0.1296148]; sorted = [0,0,0,0.1118034,0.1296148].
        // p50: pos=2.0 -> 0.0 ; p95: pos=3.8 -> 0.1118034*0.2 + 0.1296148*0.8.
        assertEquals(0.0f, r.deMedian, 1e-6f, "p50 median of errors")
        assertEquals(0.1260525f, r.deP95, 1e-6f, "p95 of errors (linear interp)")
    }

    @Test
    fun concreteImportanceReference() {
        val r = residual(samples, palette)
        assertEquals(0.2236068, r.importance[1], 1e-6, "imp[1] = err(s1)*2")
        assertEquals(0.1296148, r.importance[4], 1e-6, "imp[4] = err(s4)*1")
        assertEquals(0.3532216, r.impTotal, 1e-6, "impTotal")
        assertTrue(r.importance[1] > r.importance[4], "s1 has higher importance than s4")
    }

    @Test
    fun invalidInputsThrow() {
        val emptyPalette = Palette(FloatArray(0), FloatArray(0), FloatArray(0), 0)
        assertThrows(IllegalArgumentException::class.java) { residual(samples, emptyPalette) }
        val emptySamples = SampleSet(IntArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), 1, 1)
        assertThrows(IllegalArgumentException::class.java) { residual(emptySamples, palette) }
    }
}
