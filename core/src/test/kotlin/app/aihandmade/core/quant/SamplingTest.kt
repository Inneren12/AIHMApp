package app.aihandmade.core.quant

import app.aihandmade.core.color.Srgb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for quant sub-commit 1: image -> weighted OKLab sample set.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * There is no external ground truth for sampling, so the checks are: OKLab coordinates are bit-exact
 * with core/color's per-pixel path; the importance-weight formula matches off-line reference values
 * (computed over the same OKLab-L path) at three regimes (flat, hard edge, smooth gradient); selection
 * count clamps to the pixel count; and the result is deterministic for a fixed seed.
 */
class SamplingTest {

    private fun grey(g: Int): Int = Srgb.of(g, g, g).argb

    @Test
    fun oklabCoordinatesAreBitExactWithPerPixelPath() {
        // target >= pixel count -> every pixel sampled, output sorted ascending by index.
        val w = 4; val h = 3
        val pixels = IntArray(w * h) { i -> Srgb.of((i * 17) and 0xFF, (i * 5) and 0xFF, (i * 29) and 0xFF).argb }
        val s = samplePixels(pixels, w, h, targetSamples = w * h, seed = 1337L)
        assertEquals(w * h, s.size)
        for (k in 0 until w * h) {
            assertEquals(k, s.index[k], "indices must be 0..n-1 ascending when all pixels are sampled")
            val ok = Srgb(pixels[k]).toOkLab()
            assertEquals(ok.L, s.L[k], 0f, "OKLab L mismatch at $k")
            assertEquals(ok.a, s.a[k], 0f, "OKLab a mismatch at $k")
            assertEquals(ok.b, s.b[k], 0f, "OKLab b mismatch at $k")
        }
    }

    @Test
    fun weightFormulaTwoTone() {
        // 8x8 vertical hard edge: left grey=40, right grey=200. All pixels sampled (sorted).
        val w = 8; val h = 8
        val pixels = IntArray(w * h) { i -> grey(if (i % w < 4) 40 else 200) }
        val s = samplePixels(pixels, w, h, targetSamples = w * h, seed = 1337L)
        // flat interior (x=1,y=4): E=0, N=0, R=1 -> w = (1)(1+0.6)/(1) = 1.6
        assertEquals(1.6f, s.weight[4 * w + 1], 1e-4f, "flat-interior weight")
        // hard-edge column (x=3,y=4): E=1, N=1, R=0 -> w = (1.8)(1)/(1.5) = 1.2
        assertEquals(1.2f, s.weight[4 * w + 3], 1e-4f, "hard-edge weight")
    }

    @Test
    fun weightFormulaGradient() {
        // 8x8 horizontal ramp grey=x*32 -> smooth gradient (fractional E/N/R exercises normalization).
        val w = 8; val h = 8
        val pixels = IntArray(w * h) { i -> grey((i % w) * 32) }
        val s = samplePixels(pixels, w, h, targetSamples = w * h, seed = 1337L)
        // gradient interior (x=3,y=3): reference weight from the off-line OKLab-L computation.
        assertEquals(2.000691f, s.weight[3 * w + 3], 1e-4f, "gradient-interior weight")
    }

    @Test
    fun selectionCountClampsToPixelCount() {
        val w = 6; val h = 6
        val pixels = IntArray(w * h) { i -> grey(i * 3) }
        assertEquals(w * h, samplePixels(pixels, w, h, targetSamples = 10_000, seed = 1L).size)
        assertEquals(12, samplePixels(pixels, w, h, targetSamples = 12, seed = 1L).size)
    }

    @Test
    fun deterministicForSameSeed() {
        val w = 10; val h = 10
        val pixels = IntArray(w * h) { i -> grey((i * 7) and 0xFF) }
        val a = samplePixels(pixels, w, h, targetSamples = 30, seed = 42L)
        val b = samplePixels(pixels, w, h, targetSamples = 30, seed = 42L)
        assertEquals(a.size, b.size)
        assertTrue(a.index.contentEquals(b.index), "same seed -> identical selected indices")
        assertTrue(a.weight.contentEquals(b.weight), "same seed -> identical weights")
    }

    @Test
    fun invalidInputsThrow() {
        assertThrows(IllegalArgumentException::class.java) { samplePixels(IntArray(5), 2, 3, 4, 1L) } // 5 != 6
        assertThrows(IllegalArgumentException::class.java) { samplePixels(IntArray(6), 2, 3, 0, 1L) } // target < 1
    }
}
