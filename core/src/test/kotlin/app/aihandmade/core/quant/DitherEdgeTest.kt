package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLabPlanes
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DitherEdgeTest {

    private val bw = Palette(floatArrayOf(0f, 1f), floatArrayOf(0f, 0f), floatArrayOf(0f, 0f), anchorCount = 0)

    private fun img1(L: Float = 0.5f, a: Float = 0f, b: Float = 0f) =
        OkLabPlanes(floatArrayOf(L), floatArrayOf(a), floatArrayOf(b), 1, 1)

    // --- overflow guard -----------------------------------------------------------------------

    @Test
    fun oversizedDimensionsThrow() {
        // 100_000 × 100_000 = 10^10 > Int.MAX_VALUE — must throw before any buffer allocation.
        assertThrows(IllegalArgumentException::class.java) {
            ditherFloydSteinberg(img1(), 100_000, 100_000, bw)
        }
    }

    @Test
    fun maxIntTimesTwo_overflows() {
        assertThrows(IllegalArgumentException::class.java) {
            ditherFloydSteinberg(img1(), Int.MAX_VALUE, 2, bw)
        }
    }

    // --- finite image validation -------------------------------------------------------------

    @Test
    fun nanInImageLThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            ditherFloydSteinberg(img1(L = Float.NaN), 1, 1, bw)
        }
    }

    @Test
    fun nanInImageAThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            ditherFloydSteinberg(img1(a = Float.NaN), 1, 1, bw)
        }
    }

    @Test
    fun nanInImageBThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            ditherFloydSteinberg(img1(b = Float.NaN), 1, 1, bw)
        }
    }

    @Test
    fun positiveInfinityInImageThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            ditherFloydSteinberg(img1(L = Float.POSITIVE_INFINITY), 1, 1, bw)
        }
    }

    @Test
    fun negativeInfinityInImageThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            ditherFloydSteinberg(img1(L = Float.NEGATIVE_INFINITY), 1, 1, bw)
        }
    }

    @Test
    fun finiteValuesNotRejected() {
        // Large-but-finite values must not be rejected by the finite check.
        val out = ditherFloydSteinberg(img1(L = Float.MAX_VALUE), 1, 1, bw)
        assertTrue(out[0] in 0..1)
    }

    // --- finite palette validation -----------------------------------------------------------

    @Test
    fun nanInPaletteLThrows() {
        val p = Palette(floatArrayOf(Float.NaN), floatArrayOf(0f), floatArrayOf(0f), anchorCount = 0)
        assertThrows(IllegalArgumentException::class.java) { ditherFloydSteinberg(img1(), 1, 1, p) }
    }

    @Test
    fun nanInPaletteAThrows() {
        val p = Palette(floatArrayOf(0f), floatArrayOf(Float.NaN), floatArrayOf(0f), anchorCount = 0)
        assertThrows(IllegalArgumentException::class.java) { ditherFloydSteinberg(img1(), 1, 1, p) }
    }

    @Test
    fun nanInPaletteBThrows() {
        val p = Palette(floatArrayOf(0f), floatArrayOf(0f), floatArrayOf(Float.NaN), anchorCount = 0)
        assertThrows(IllegalArgumentException::class.java) { ditherFloydSteinberg(img1(), 1, 1, p) }
    }

    @Test
    fun infiniteInPaletteThrows() {
        val p = Palette(floatArrayOf(Float.POSITIVE_INFINITY), floatArrayOf(0f), floatArrayOf(0f), anchorCount = 0)
        assertThrows(IllegalArgumentException::class.java) { ditherFloydSteinberg(img1(), 1, 1, p) }
    }

    // --- geometry validation -----------------------------------------------------------------

    @Test
    fun samePixelCountDifferentStoredGeometryThrows() {
        // Stored as 4×1, called as 2×2: same total pixel count but different 2D layout.
        // Floyd-Steinberg error diffusion depends on geometry, so this must be rejected.
        val img = OkLabPlanes(
            FloatArray(4) { 0.5f },
            FloatArray(4) { 0f },
            FloatArray(4) { 0f },
            4,
            1,
        )
        assertThrows(IllegalArgumentException::class.java) {
            ditherFloydSteinberg(img, 2, 2, bw)
        }
    }
}
