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

    // --- geometry note -----------------------------------------------------------------------
    //
    // Item 2 of the review asks for: require(image.width == width && image.height == height).
    //
    // This check cannot be added without breaking DitherTest (an immutable contract test).
    // DitherTest's `planes(L)` helper uses the 3-arg OkLabPlanes secondary constructor which
    // stores (width=L.size, height=1). DitherTest then passes explicit width/height that don't
    // match those stored values (e.g., planes(4 pixels) called with 2×2).
    //
    // To add the geometry check, either:
    //   (a) modify DitherTest to pass a properly-dimensioned OkLabPlanes, OR
    //   (b) remove the secondary constructor and create a test-only factory that accepts explicit
    //       dimensions.
    // Until one of those is done, the function validates only the flat pixel count (L.size ==
    // width*height), not the 2D geometry stored in OkLabPlanes.
    //
    // The test below documents current accepted behaviour.

    @Test
    fun samePixelCountDifferentStoredGeometry_currentlyAccepted() {
        // Stored as 4×1, called as 2×2: same pixel count, different 2D geometry.
        // Currently not rejected — see geometry note above.
        val img = OkLabPlanes(FloatArray(4) { 0.5f }, FloatArray(4) { 0f }, FloatArray(4) { 0f }, 4, 1)
        val out = ditherFloydSteinberg(img, 2, 2, bw)
        assertTrue(out.size == 4 && out.all { it in 0..1 })
    }
}
