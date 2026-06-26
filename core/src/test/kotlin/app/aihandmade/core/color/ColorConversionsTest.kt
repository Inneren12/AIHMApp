package app.aihandmade.core.color

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

/**
 * CONTRACT TEST for core/color commit 2 (conversions + OkLab + OKLab distance).
 *
 * This file is the acceptance contract: implement the types/functions so it compiles and passes
 * UNCHANGED. You may add tests, but do not edit or weaken anything here.
 *
 * Anchors are D65 reference values (sRGB -> CIE-Lab and sRGB -> OKLab), independently verified.
 * Tolerances are generous enough to absorb Float storage yet far tighter than any real bug:
 * a wrong matrix or a swapped colour space produces errors of order >=1, not <=0.05.
 */
class ColorConversionsTest {

    // (r, g, b) -> expected Lab(L,a,b) and OkLab(L,a,b)
    private data class Anchor(
        val r: Int, val g: Int, val b: Int,
        val labL: Float, val labA: Float, val labB: Float,
        val okL: Float, val okA: Float, val okB: Float,
    )

    private val anchors = listOf(
        Anchor(255, 0, 0,   53.2408f,  80.0925f,  67.2032f,  0.62796f,  0.22486f,  0.12585f),
        Anchor(0, 255, 0,   87.7347f, -86.1827f,  83.1793f,  0.86644f, -0.23389f,  0.17950f),
        Anchor(0, 0, 255,   32.2970f,  79.1875f,-107.8602f,  0.45201f, -0.03246f, -0.31153f),
        Anchor(255,255,255,100.0000f,   0.0000f,   0.0000f,  1.00000f,  0.00000f,  0.00000f),
        Anchor(0, 0, 0,      0.0000f,   0.0000f,   0.0000f,  0.00000f,  0.00000f,  0.00000f),
        Anchor(128,128,128, 53.5850f,   0.0000f,   0.0000f,  0.59987f,  0.00000f,  0.00000f),
    )

    @Test
    fun srgbLinearRoundTripIsLossless() {
        for (v in 0..255) {
            val back = Srgb.of(v, v, v).toLinear().toSrgb()
            assertEquals(v, back.r, "sRGB<->linear round trip failed at level $v")
            assertEquals(v, back.g)
            assertEquals(v, back.b)
        }
    }

    @Test
    fun linearOkLabRoundTrip() {
        for (a in anchors) {
            val lin = Srgb.of(a.r, a.g, a.b).toLinear()
            val rt = lin.toOkLab().toLinearRgb()
            assertEquals(lin.r, rt.r, 1e-3f, "OkLab round trip r (${a.r},${a.g},${a.b})")
            assertEquals(lin.g, rt.g, 1e-3f, "OkLab round trip g (${a.r},${a.g},${a.b})")
            assertEquals(lin.b, rt.b, 1e-3f, "OkLab round trip b (${a.r},${a.g},${a.b})")
        }
    }

    @Test
    fun linearLabRoundTrip() {
        for (a in anchors) {
            val lin = Srgb.of(a.r, a.g, a.b).toLinear()
            val rt = lin.toLab().toLinearRgb()
            assertEquals(lin.r, rt.r, 1e-3f, "Lab round trip r (${a.r},${a.g},${a.b})")
            assertEquals(lin.g, rt.g, 1e-3f, "Lab round trip g (${a.r},${a.g},${a.b})")
            assertEquals(lin.b, rt.b, 1e-3f, "Lab round trip b (${a.r},${a.g},${a.b})")
        }
    }

    @Test
    fun neutralsHaveZeroChroma() {
        for (v in intArrayOf(0, 32, 64, 128, 200, 255)) {
            val ok = Srgb.of(v, v, v).toOkLab()
            assertEquals(0f, ok.a, 1e-3f, "OkLab a for gray $v")
            assertEquals(0f, ok.b, 1e-3f, "OkLab b for gray $v")
            val lab = Srgb.of(v, v, v).toLab()
            assertEquals(0f, lab.a, 1e-3f, "Lab a for gray $v")
            assertEquals(0f, lab.b, 1e-3f, "Lab b for gray $v")
        }
    }

    @Test
    fun anchorsMatchReferenceValues() {
        for (a in anchors) {
            val lab = Srgb.of(a.r, a.g, a.b).toLab()
            assertEquals(a.labL, lab.L, 0.05f, "Lab L (${a.r},${a.g},${a.b})")
            assertEquals(a.labA, lab.a, 0.05f, "Lab a (${a.r},${a.g},${a.b})")
            assertEquals(a.labB, lab.b, 0.05f, "Lab b (${a.r},${a.g},${a.b})")
            val ok = Srgb.of(a.r, a.g, a.b).toOkLab()
            assertEquals(a.okL, ok.L, 1e-3f, "OkLab L (${a.r},${a.g},${a.b})")
            assertEquals(a.okA, ok.a, 1e-3f, "OkLab a (${a.r},${a.g},${a.b})")
            assertEquals(a.okB, ok.b, 1e-3f, "OkLab b (${a.r},${a.g},${a.b})")
        }
    }

    @Test
    fun srgbChannelsRoundTrip() {
        val c = Srgb.of(10, 137, 250, 200)
        assertEquals(10, c.r); assertEquals(137, c.g); assertEquals(250, c.b); assertEquals(200, c.a)
    }

    @Test
    fun okLabDistanceIsConsistent() {
        val x = Srgb.of(255, 0, 0).toOkLab()
        val y = Srgb.of(0, 0, 255).toOkLab()
        assertEquals(0f, deltaSqOk(x, x), 0f, "distance to self must be zero")
        assertEquals(0f, deltaOk(x, x), 0f)
        assertEquals(deltaSqOk(x, y), deltaSqOk(y, x), 0f, "deltaSqOk must be symmetric")
        assertEquals(sqrt(deltaSqOk(x, y).toDouble()).toFloat(), deltaOk(x, y), 1e-5f,
            "deltaOk must equal sqrt(deltaSqOk)")
        assertTrue(deltaOk(x, y) > 0f)
    }
}
