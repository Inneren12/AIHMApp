package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.OkLabPlanes
import app.aihandmade.core.color.deltaSqOk
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for quant sub-commit 6: colour Floyd-Steinberg dithering in OKLab.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * ditherFloydSteinberg walks pixels in raster order, adds the diffused error to each pixel's OkLab,
 * picks the nearest palette colour by OKLab-Euclidean (deltaSqOk, ties -> smallest index), writes that
 * index, and diffuses the residual OkLab error to the right (7/16), down-left (3/16), down (5/16) and
 * down-right (1/16). Pure OKLab working space — no CIEDE2000, no Lab conversion.
 *
 * Pinned here: exact-match input does not dither (equals plain nearest); two concrete Floyd-Steinberg
 * outputs; right-diffusion actually flips a neighbour; a flat midtone dithers and preserves its average;
 * indices are valid; determinism.
 */
class DitherTest {

    private val blackWhite =
        Palette(floatArrayOf(0f, 1f), floatArrayOf(0f, 0f), floatArrayOf(0f, 0f), anchorCount = 0)

    private fun planes(L: FloatArray, width: Int, height: Int) =
        OkLabPlanes(
            L,
            FloatArray(L.size) { 0f },
            FloatArray(L.size) { 0f },
            width,
            height,
        )

    private fun plainNearest(img: OkLabPlanes, palette: Palette): IntArray = IntArray(img.L.size) { idx ->
        var best = 0
        var bestSq = Float.POSITIVE_INFINITY
        for (c in 0 until palette.size) {
            val sq = deltaSqOk(OkLab(img.L[idx], img.a[idx], img.b[idx]),
                OkLab(palette.L[c], palette.a[c], palette.b[c]))
            if (sq < bestSq) { bestSq = sq; best = c }
        }
        best
    }

    @Test
    fun exactPaletteMatchDoesNotDither() {
        // pixels are exactly palette colours -> zero error everywhere -> output equals plain nearest.
        val img = planes(floatArrayOf(0f, 1f, 0f, 1f), 2, 2)
        val out = ditherFloydSteinberg(img, 2, 2, blackWhite)
        assertArrayEquals(intArrayOf(0, 1, 0, 1), out)
        assertArrayEquals(plainNearest(img, blackWhite), out)
    }

    @Test
    fun concrete2x2MidGray() {
        val out = ditherFloydSteinberg(planes(floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f), 2, 2), 2, 2, blackWhite)
        assertArrayEquals(intArrayOf(0, 1, 1, 0), out)
    }

    @Test
    fun rightDiffusionFlipsNeighbour() {
        // px0=0.6 -> white(1), residual -0.4 diffuses right; px1=0.55 alone would be white, but the
        // pushed error drives it to black(0). Plain nearest is [1,1]; Floyd-Steinberg is [1,0].
        val img = planes(floatArrayOf(0.6f, 0.55f), 2, 1)
        val out = ditherFloydSteinberg(img, 2, 1, blackWhite)
        assertArrayEquals(intArrayOf(1, 0), out)
        assertArrayEquals(intArrayOf(1, 1), plainNearest(img, blackWhite))
    }

    @Test
    fun flatMidGrayDithersAndPreservesAverage() {
        val out = ditherFloydSteinberg(planes(FloatArray(16) { 0.5f }, 4, 4), 4, 4, blackWhite)
        assertTrue(out.any { it == 0 } && out.any { it == 1 }, "a flat midtone must dither, not flatten")
        assertEquals(8, out.count { it == 1 }, "FS preserves the average: half the cells pick white")
    }

    @Test
    fun outputIndicesAreValid() {
        val three =
            Palette(floatArrayOf(0f, 0.5f, 1f), floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 0f), anchorCount = 0)
        val out = ditherFloydSteinberg(planes(FloatArray(16) { it / 15f }, 4, 4), 4, 4, three)
        assertEquals(16, out.size)
        assertTrue(out.all { it in 0 until three.size })
    }

    @Test
    fun deterministic() {
        val img = planes(FloatArray(16) { 0.5f }, 4, 4)
        assertArrayEquals(ditherFloydSteinberg(img, 4, 4, blackWhite), ditherFloydSteinberg(img, 4, 4, blackWhite))
    }

    @Test
    fun invalidInputsThrow() {
        val img = planes(FloatArray(4) { 0.5f }, 2, 2)
        assertThrows(IllegalArgumentException::class.java) { ditherFloydSteinberg(img, 0, 2, blackWhite) }
        assertThrows(IllegalArgumentException::class.java) { ditherFloydSteinberg(img, 2, 3, blackWhite) }
        val emptyP = Palette(FloatArray(0), FloatArray(0), FloatArray(0), 0)
        assertThrows(IllegalArgumentException::class.java) { ditherFloydSteinberg(img, 2, 2, emptyP) }
    }
}
