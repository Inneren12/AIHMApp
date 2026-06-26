package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.Srgb
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb
import app.aihandmade.core.color.toOkLab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for quant sub-commit 2: initial palette K0 (anchors + spread-gated fill).
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 */
class PaletteInitTest {

    private val w = 6
    private val h = 6
    private val pixels = IntArray(w * h) { i ->
        when (i) {
            0 -> Srgb.of(0, 0, 0).argb
            1 -> Srgb.of(255, 255, 255).argb
            2 -> Srgb.of(128, 128, 128).argb
            else -> Srgb.of((i * 53) and 0xFF, (i * 101) and 0xFF, (i * 151) and 0xFF).argb
        }
    }
    private val samples = samplePixels(pixels, w, h, targetSamples = w * h, seed = 1337L)

    private fun labOf(p: Palette, k: Int) = OkLab(p.L[k], p.a[k], p.b[k]).toLinearRgb().toLab()

    @Test
    fun anchorsAreBlackWhiteAndNeutral() {
        val pal = initPalette(samples)
        assertEquals(3, pal.anchorCount, "black + white + neutral")
        val black = Srgb(pixels[0]).toOkLab()
        val white = Srgb(pixels[1]).toOkLab()
        val grey = Srgb(pixels[2]).toOkLab()
        assertEquals(black.L, pal.L[0], 0f); assertEquals(black.a, pal.a[0], 0f); assertEquals(black.b, pal.b[0], 0f)
        assertEquals(white.L, pal.L[1], 0f); assertEquals(white.a, pal.a[1], 0f); assertEquals(white.b, pal.b[1], 0f)
        assertEquals(grey.L, pal.L[2], 0f); assertEquals(grey.a, pal.a[2], 0f); assertEquals(grey.b, pal.b[2], 0f)
    }

    @Test
    fun fillsRespectSpreadGate() {
        val pal = initPalette(samples)
        for (j in pal.anchorCount until pal.size) {
            val lj = labOf(pal, j)
            for (i in 0 until pal.size) {
                if (i == j) continue
                assertTrue(deltaE2000(lj, labOf(pal, i)) >= 3.5 - 1e-3, "fill $j too close to colour $i")
            }
        }
    }

    @Test
    fun sizeDoesNotExceedK0() {
        assertTrue(initPalette(samples).size <= 14, "default K0 = 14")
        assertTrue(initPalette(samples, k0 = 5).size <= 5, "custom K0 = 5")
    }

    @Test
    fun deterministic() {
        val a = initPalette(samples)
        val b = initPalette(samples)
        assertEquals(a.anchorCount, b.anchorCount)
        assertTrue(a.L.contentEquals(b.L) && a.a.contentEquals(b.a) && a.b.contentEquals(b.b),
            "init is deterministic")
    }

    @Test
    fun invalidInputsThrow() {
        val empty = SampleSet(IntArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), 1, 1)
        assertThrows(IllegalArgumentException::class.java) { initPalette(empty) }
        assertThrows(IllegalArgumentException::class.java) { initPalette(samples, k0 = 0) }
    }
}
