package app.aihandmade.core.color

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows

/**
 * CONTRACT TEST for core/color commit 3 (SoA planes + bulk image->planes conversion).
 *
 * Implement the types/functions so this compiles and passes UNCHANGED. You may add tests, but do
 * not edit or weaken anything here.
 *
 * The core invariant is **bit-exact equality with the per-pixel path**: a plane value must equal the
 * commit-2 per-pixel `Srgb.toOkLab()` / `Srgb.toLab()` result for the same pixel, exactly. This both
 * verifies correctness and enforces the rule that the bulk path reuses the SAME transform kernel
 * (no copied matrix) — a duplicated matrix with any rounding difference would break exact equality.
 */
class ColorPlanesTest {

    // width=2, height=3, distinct colours so the flat order is observable.
    private val w = 2
    private val h = 3
    private val pixels = intArrayOf(
        Srgb.of(255, 0, 0).argb,    // idx 0  (x0,y0)
        Srgb.of(0, 255, 0).argb,    // idx 1  (x1,y0)
        Srgb.of(0, 0, 255).argb,    // idx 2  (x0,y1)
        Srgb.of(255, 255, 255).argb,// idx 3  (x1,y1)
        Srgb.of(0, 0, 0).argb,      // idx 4  (x0,y2)
        Srgb.of(128, 128, 128).argb,// idx 5  (x1,y2)
    )

    @Test
    fun okLabPlanesMatchPerPixelExactly() {
        val p = pixels.toOkLabPlanes(w, h)
        for (i in pixels.indices) {
            val expected = Srgb(pixels[i]).toOkLab()
            assertEquals(expected.L, p.L[i], 0f, "OkLab plane L mismatch at $i")
            assertEquals(expected.a, p.a[i], 0f, "OkLab plane a mismatch at $i")
            assertEquals(expected.b, p.b[i], 0f, "OkLab plane b mismatch at $i")
        }
    }

    @Test
    fun labPlanesMatchPerPixelExactly() {
        val p = pixels.toLabPlanes(w, h)
        for (i in pixels.indices) {
            val expected = Srgb(pixels[i]).toLab()
            assertEquals(expected.L, p.L[i], 0f, "Lab plane L mismatch at $i")
            assertEquals(expected.a, p.a[i], 0f, "Lab plane a mismatch at $i")
            assertEquals(expected.b, p.b[i], 0f, "Lab plane b mismatch at $i")
        }
    }

    @Test
    fun planesCarryDimensionsAndSizes() {
        val ok = pixels.toOkLabPlanes(w, h)
        assertEquals(w, ok.width); assertEquals(h, ok.height); assertEquals(w * h, ok.size)
        assertEquals(w * h, ok.L.size); assertEquals(w * h, ok.a.size); assertEquals(w * h, ok.b.size)
        val lab = pixels.toLabPlanes(w, h)
        assertEquals(w, lab.width); assertEquals(h, lab.height); assertEquals(w * h, lab.size)
        assertEquals(w * h, lab.L.size); assertEquals(w * h, lab.a.size); assertEquals(w * h, lab.b.size)
    }

    @Test
    fun flatOrderIsRowMajorByIndex() {
        // index = y*width + x ; pixel at (x1,y0) is index 1, (x0,y1) is index w.
        val ok = pixels.toOkLabPlanes(w, h)
        val green = Srgb.of(0, 255, 0).toOkLab()   // placed at idx 1
        val blue = Srgb.of(0, 0, 255).toOkLab()    // placed at idx w (=2)
        assertEquals(green.a, ok.a[1], 0f)
        assertEquals(blue.b, ok.b[w], 0f)
    }

    @Test
    fun mismatchedDimensionsThrow() {
        assertThrows(IllegalArgumentException::class.java) { pixels.toOkLabPlanes(2, 2) } // 4 != 6
        assertThrows(IllegalArgumentException::class.java) { pixels.toLabPlanes(5, 1) }   // 5 != 6
    }
}
