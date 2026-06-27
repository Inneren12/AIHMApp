package app.aihandmade.core.quant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KneedleEdgeTest {

    private fun samplesOf(ls: FloatArray): SampleSet =
        SampleSet(IntArray(ls.size) { it }, ls, FloatArray(ls.size) { 0f }, FloatArray(ls.size) { 0f },
            FloatArray(ls.size) { 1f }, ls.size, 1)

    @Test
    fun rejectsNanSampleCoordinate() {
        val s = SampleSet(
            IntArray(3) { it },
            floatArrayOf(0.5f, Float.NaN, 0.5f),
            FloatArray(3) { 0f }, FloatArray(3) { 0f },
            FloatArray(3) { 1f }, 3, 1
        )
        val p = Palette(floatArrayOf(0.5f, 0.6f), FloatArray(2) { 0f }, FloatArray(2) { 0f }, anchorCount = 0)
        assertThrows(IllegalArgumentException::class.java) { selectK(s, p, k0 = 1, kTry = 2) }
    }

    @Test
    fun rejectsInfinitePaletteCoordinate() {
        val s = samplesOf(floatArrayOf(0.2f, 0.5f, 0.8f))
        val p = Palette(
            floatArrayOf(0.5f, Float.POSITIVE_INFINITY),
            FloatArray(2) { 0f }, FloatArray(2) { 0f },
            anchorCount = 0
        )
        assertThrows(IllegalArgumentException::class.java) { selectK(s, p, k0 = 1, kTry = 2) }
    }

    /**
     * Verifies that the baseline row at k0 (which always has gain=0) is excluded from the
     * low-gain streak. With the fix, the streak counts only real gain steps (idx >= 1), so
     * kStar lands at k0 + KNEE_LOW_GAIN_STREAK. Without the fix the baseline's gain=0 would
     * start the streak one row too early, producing kStar = k0 + KNEE_LOW_GAIN_STREAK - 1.
     *
     * Setup: all samples at OkLab(0.5, 0, 0). Palette[0] is at OkLab(0.5, 0.1, 0), which is
     * far enough (>> KNEE_DE95_TARGET) that no early_quality fires. Palette[1..3] have increasing
     * chroma and are therefore farther from the samples than palette[0]; nearest distances never
     * improve after K=1, giving gain=0 at every subsequent K. The flat curve has no knee.
     */
    @Test
    fun lowGainDoesNotCountBaselineRow() {
        val s = SampleSet(
            IntArray(10) { it },
            FloatArray(10) { 0.5f }, FloatArray(10) { 0f }, FloatArray(10) { 0f },
            FloatArray(10) { 1f }, 10, 1
        )
        val p = Palette(
            FloatArray(4) { 0.5f },
            floatArrayOf(0.10f, 0.20f, 0.30f, 0.40f),
            FloatArray(4) { 0f },
            anchorCount = 0
        )
        val res = selectK(s, p, k0 = 1, kTry = 4)

        // de95 is constant and well above KNEE_DE95_TARGET (no early_quality)
        for (row in res.rows) {
            assertTrue(row.de95 > KNEE_DE95_TARGET,
                "de95=${row.de95} must exceed KNEE_DE95_TARGET at K=${row.k}")
        }
        // All real gain steps (idx >= 1) produce gain = 0 (palette[1..3] never improve nearest)
        for (i in 1 until res.rows.size) {
            assertEquals(0f, res.rows[i].gain, 1e-4f, "gain must be 0 at K=${res.rows[i].k}")
        }
        // Flat curve: deviation at the baseline row is 0, no valid knee
        assertEquals(0f, res.rows[0].deviation, 1e-4f)

        // low_gain fires at k0 + KNEE_LOW_GAIN_STREAK (3 real steps, not counting baseline)
        assertEquals("low_gain", res.reason)
        assertEquals(1 + KNEE_LOW_GAIN_STREAK, res.kStar,
            "kStar must be k0 + KNEE_LOW_GAIN_STREAK; baseline gain=0 must not count")
    }
}
