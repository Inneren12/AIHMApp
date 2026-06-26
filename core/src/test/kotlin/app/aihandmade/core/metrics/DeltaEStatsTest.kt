package app.aihandmade.core.metrics

import app.aihandmade.core.color.LabPlanes
import app.aihandmade.core.color.Srgb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for metrics-1: per-pixel CIEDE2000 error statistics (mean / median / p95 / max).
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * Reference stats were computed off-line over the SAME path production uses: ARGB -> Float Lab planes
 * (core/color `toLabPlanes`) -> per-pixel `deltaE2000` (core/color) -> the percentile algorithm in the
 * spec. The Sharma-anchored test proves the metric truly uses CIEDE2000 (not a home-grown delta).
 */
class DeltaEStatsTest {

    // 10-pixel reference image (5x2) and a "quantised" candidate.
    private val refPixels = intArrayOf(
        Srgb.of(255, 0, 0).argb, Srgb.of(100, 150, 200).argb, Srgb.of(50, 50, 50).argb,
        Srgb.of(200, 100, 50).argb, Srgb.of(16, 176, 112).argb, Srgb.of(255, 255, 255).argb,
        Srgb.of(0, 0, 0).argb, Srgb.of(128, 128, 128).argb, Srgb.of(32, 128, 208).argb,
        Srgb.of(144, 64, 160).argb,
    )
    private val candPixels = intArrayOf(
        Srgb.of(255, 0, 0).argb, Srgb.of(105, 150, 200).argb, Srgb.of(60, 55, 52).argb,
        Srgb.of(180, 110, 40).argb, Srgb.of(24, 168, 120).argb, Srgb.of(248, 248, 248).argb,
        Srgb.of(8, 8, 8).argb, Srgb.of(136, 128, 128).argb, Srgb.of(40, 136, 216).argb,
        Srgb.of(136, 72, 168).argb,
    )
    private val w = 5
    private val h = 2

    @Test
    fun controlledPairMatchesReferenceStats() {
        val s = deltaEStats(refPixels, candPixels, w, h)
        assertEquals(3.080451, s.mean, 1e-4, "mean")
        assertEquals(2.965929, s.median, 1e-4, "median")
        assertEquals(6.930160, s.p95, 1e-4, "p95")
        assertEquals(9.109813, s.max, 1e-4, "max")
    }

    @Test
    fun identicalImagesAreZero() {
        val s = deltaEStats(refPixels, refPixels, w, h)
        assertEquals(0.0, s.mean, 1e-9)
        assertEquals(0.0, s.median, 1e-9)
        assertEquals(0.0, s.p95, 1e-9)
        assertEquals(0.0, s.max, 1e-9)
    }

    @Test
    fun reproducesCiede2000FromLabPlanes() {
        // 1x1 planes built directly from Lab; for a single pixel every stat equals that pixel's dE.
        // Values are published Sharma CIEDE2000 references -> proves the metric uses CIEDE2000.
        data class Case(val l1: FloatArray, val l2: FloatArray, val expected: Double)
        val cases = listOf(
            Case(floatArrayOf(50f, 2.5f, 0f), floatArrayOf(73f, 25f, -18f), 27.1492),
            Case(floatArrayOf(50f, 2.5f, 0f), floatArrayOf(56f, -27f, -3f), 31.9030),
            Case(floatArrayOf(50f, 0f, 0f), floatArrayOf(50f, -1f, 2f), 2.3669),
        )
        for (c in cases) {
            val ref = LabPlanes(floatArrayOf(c.l1[0]), floatArrayOf(c.l1[1]), floatArrayOf(c.l1[2]), 1, 1)
            val cand = LabPlanes(floatArrayOf(c.l2[0]), floatArrayOf(c.l2[1]), floatArrayOf(c.l2[2]), 1, 1)
            val s = deltaEStats(ref, cand)
            assertEquals(c.expected, s.max, 1e-3, "max vs Sharma")
            assertEquals(c.expected, s.mean, 1e-3, "mean vs Sharma")
            assertEquals(c.expected, s.median, 1e-3, "median vs Sharma")
            assertEquals(c.expected, s.p95, 1e-3, "p95 vs Sharma")
        }
    }

    @Test
    fun statisticsAreOrdered() {
        val s = deltaEStats(refPixels, candPixels, w, h)
        assertTrue(s.max >= s.p95, "max >= p95")
        assertTrue(s.p95 >= s.median, "p95 >= median")
        assertTrue(s.median >= 0.0, "median >= 0")
        assertTrue(s.mean >= 0.0 && s.max >= s.mean, "0 <= mean <= max")
    }

    @Test
    fun mismatchedDimensionsThrow() {
        // pixel count not equal to width*height
        assertThrows(IllegalArgumentException::class.java) { deltaEStats(refPixels, candPixels, 3, 3) }
        // reference and candidate planes of different sizes
        val a = LabPlanes(FloatArray(2), FloatArray(2), FloatArray(2), 2, 1)
        val b = LabPlanes(FloatArray(1), FloatArray(1), FloatArray(1), 1, 1)
        assertThrows(IllegalArgumentException::class.java) { deltaEStats(a, b) }
    }
}
