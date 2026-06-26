package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.Srgb
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for quant sub-commit 2: weighted OKLab sample set -> K0 initial palette.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * Checks: black/white anchors have bit-exact OKLab from the sample set; every fill is >= S_MIN dE2000
 * from every other palette colour (on CIE-Lab via core/color); size is bounded by k0 (except when
 * anchorCount exceeds k0); result is deterministic; and the requires fire on invalid inputs.
 */
class PaletteInitTest {

    private fun grey(g: Int) = Srgb.of(g, g, g).argb
    private fun allSamples(pixels: IntArray, w: Int, h: Int) =
        samplePixels(pixels, w, h, targetSamples = w * h, seed = 1337L)

    @Test
    fun blackAnchorIsBitExactDarkestMinChroma() {
        // 6x6 grey ramp; pixels[0]=pure black ensures it is the unambiguous darkest sample.
        // n=36 -> fraction=max(36/20,1)=1, so black = single position with minimum L.
        val w = 6; val h = 6
        val pixels = IntArray(w * h) { i -> grey(i * 7) } // 0..238, all < 256 (no wrap)
        pixels[0] = grey(0)    // force pure black
        pixels[35] = grey(255) // force pure white
        val s = allSamples(pixels, w, h)
        val p = initPalette(s)

        val darkPos = (0 until s.size).minByOrNull { s.L[it] }!!
        assertEquals(s.L[darkPos], p.L[0], 0f, "black anchor L must be bit-exact")
        assertEquals(s.a[darkPos], p.a[0], 0f, "black anchor a must be bit-exact")
        assertEquals(s.b[darkPos], p.b[0], 0f, "black anchor b must be bit-exact")
        assertTrue(p.anchorCount in 1..3)
    }

    @Test
    fun whiteAnchorIsBitExactLightestMinChroma() {
        // Same setup; n=36 -> fraction=1 -> white = single position with maximum L.
        val w = 6; val h = 6
        val pixels = IntArray(w * h) { i -> grey(i * 7) }
        pixels[0] = grey(0)
        pixels[35] = grey(255)
        val s = allSamples(pixels, w, h)
        val p = initPalette(s)

        val lightPos = (0 until s.size).maxByOrNull { s.L[it] }!!
        // White is anchor[1] only when it differs from black; guaranteed here since grey(0) != grey(255).
        assertTrue(p.anchorCount >= 2)
        assertEquals(s.L[lightPos], p.L[1], 0f, "white anchor L must be bit-exact")
        assertEquals(s.a[lightPos], p.a[1], 0f, "white anchor a must be bit-exact")
        assertEquals(s.b[lightPos], p.b[1], 0f, "white anchor b must be bit-exact")
    }

    @Test
    fun allFillsPassSpreadGate() {
        // Colourful 8x8 image gives diverse samples; the gate must hold for every accepted fill.
        val w = 8; val h = 8
        val pixels = IntArray(w * h) { i ->
            Srgb.of((i * 17) and 0xFF, (i * 37) and 0xFF, (i * 53) and 0xFF).argb
        }
        val s = allSamples(pixels, w, h)
        val p = initPalette(s)

        for (i in p.anchorCount until p.size) {
            val labI = OkLab(p.L[i], p.a[i], p.b[i]).toLinearRgb().toLab()
            for (j in 0 until p.size) {
                if (i == j) continue
                val labJ = OkLab(p.L[j], p.a[j], p.b[j]).toLinearRgb().toLab()
                val de = deltaE2000(labI, labJ)
                assertTrue(
                    de >= S_MIN,
                    "fill[$i] vs palette[$j]: dE2000=$de < S_MIN=$S_MIN"
                )
            }
        }
    }

    @Test
    fun sizeRespectsk0WhenSufficientAnchors() {
        // With k0 >= anchorCount the palette must not exceed k0.
        val w = 8; val h = 8
        val pixels = IntArray(w * h) { i ->
            Srgb.of((i * 13) and 0xFF, (i * 7) and 0xFF, (i * 23) and 0xFF).argb
        }
        val s = allSamples(pixels, w, h)
        assertTrue(initPalette(s, k0 = K0_TARGET).size <= K0_TARGET)
        assertTrue(initPalette(s, k0 = 5).size <= 5)
    }

    @Test
    fun anchorsAlwaysIncludedEvenWhenk0Small() {
        // When k0 < anchorCount, anchors must still all be present (no fills).
        val w = 6; val h = 6
        val pixels = IntArray(w * h) { i -> grey(i * 7) }
        pixels[0] = grey(0); pixels[35] = grey(255)
        val s = allSamples(pixels, w, h)
        val p = initPalette(s, k0 = 1)
        // anchorCount may be up to 3; palette must include all anchors regardless of k0.
        assertEquals(p.anchorCount, p.size, "only anchors when k0 < anchorCount")
        assertTrue(p.size >= p.anchorCount)
    }

    @Test
    fun deterministicSameInputsSameOutput() {
        val w = 8; val h = 8
        val pixels = IntArray(w * h) { i ->
            Srgb.of((i * 11) and 0xFF, (i * 23) and 0xFF, (i * 7) and 0xFF).argb
        }
        val s = allSamples(pixels, w, h)
        val p1 = initPalette(s)
        val p2 = initPalette(s)
        assertEquals(p1.size, p2.size)
        assertEquals(p1.anchorCount, p2.anchorCount)
        assertTrue(p1.L.contentEquals(p2.L), "same input -> identical L")
        assertTrue(p1.a.contentEquals(p2.a), "same input -> identical a")
        assertTrue(p1.b.contentEquals(p2.b), "same input -> identical b")
    }

    @Test
    fun invalidInputsThrow() {
        val s = samplePixels(IntArray(4) { grey(it * 60) }, 2, 2, 4, 1L)
        assertThrows(IllegalArgumentException::class.java) { initPalette(s, k0 = 0) }
        val empty = SampleSet(IntArray(0), FloatArray(0), FloatArray(0), FloatArray(0), FloatArray(0), 1, 1)
        assertThrows(IllegalArgumentException::class.java) { initPalette(empty) }
    }

    @Test
    fun singleSampleProducesValidPalette() {
        val s = samplePixels(intArrayOf(grey(128)), 1, 1, 1, 1337L)
        val p = initPalette(s)
        assertTrue(p.size >= 1)
        assertTrue(p.anchorCount in 1..3)
        // Arrays match in size.
        assertEquals(p.size, p.L.size)
        assertEquals(p.size, p.a.size)
        assertEquals(p.size, p.b.size)
    }
}
