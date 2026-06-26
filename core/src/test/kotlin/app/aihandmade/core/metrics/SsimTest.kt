package app.aihandmade.core.metrics

import app.aihandmade.core.color.LabPlanes
import app.aihandmade.core.color.Srgb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for metrics-2: mean SSIM on the perceptual lightness (CIE-Lab L*) channel.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * The reference MSSIM was computed off-line over the SAME path production uses (ARGB -> Float L*
 * plane from core/color `toLabPlanes`) and was cross-validated two independent ways (a naive
 * per-window double loop and the summed-area-table method), which agreed to 1e-12.
 */
class SsimTest {

    private val w = 16
    private val h = 16

    private fun q(v: Int): Int = (v / 64) * 64

    // Deterministic 16x16 reference and a harshly-quantised candidate (same formulas as the reference calc).
    private val refPixels = IntArray(w * h) { i ->
        val x = i % w; val y = i / w
        Srgb.of((x * 16) and 0xFF, (y * 16) and 0xFF, ((x + y) * 8) and 0xFF).argb
    }
    private val candPixels = IntArray(w * h) { i ->
        val x = i % w; val y = i / w
        Srgb.of(q((x * 16) and 0xFF), q((y * 16) and 0xFF), q(((x + y) * 8) and 0xFF)).argb
    }

    @Test
    fun controlledPairMatchesReference() {
        assertEquals(0.878810, ssim(refPixels, candPixels, w, h), 1e-4)
    }

    @Test
    fun identicalImagesAreOne() {
        assertEquals(1.0, ssim(refPixels, refPixels, w, h), 1e-9)
    }

    @Test
    fun ssimIsSymmetric() {
        assertEquals(
            ssim(refPixels, candPixels, w, h),
            ssim(candPixels, refPixels, w, h),
            1e-12,
        )
    }

    @Test
    fun ssimIsInValidRangeAndBelowOneForDegraded() {
        val s = ssim(refPixels, candPixels, w, h)
        assertTrue(s in -1.0..1.0, "SSIM must be within [-1, 1]")
        assertTrue(s < 1.0, "a quantised image must be less structurally similar than identical")
    }

    @Test
    fun mismatchedDimensionsThrow() {
        // pixel count not equal to width*height
        assertThrows(IllegalArgumentException::class.java) { ssim(refPixels, candPixels, 7, 7) }
        // reference and candidate planes of different sizes
        val a = LabPlanes(FloatArray(2), FloatArray(2), FloatArray(2), 2, 1)
        val b = LabPlanes(FloatArray(1), FloatArray(1), FloatArray(1), 1, 1)
        assertThrows(IllegalArgumentException::class.java) { ssim(a, b) }
    }
}
