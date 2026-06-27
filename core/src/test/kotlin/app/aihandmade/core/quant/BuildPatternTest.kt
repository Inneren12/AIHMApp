package app.aihandmade.core.quant

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for buildPattern: stitch the verified engine stages into one PatternResult.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * buildPattern takes a prescaled image as packed-ARGB sRGB pixels (already at the target stitch
 * dimensions) and runs sampling -> init -> greedy -> refine -> Kneedle (auto K) -> dither -> DMC match
 * -> symbolize -> counts. The palette is built by the real importance-weighted samplePixels; the dither
 * planes come from the SAME pixels via the same sRGB->OkLab conversion, so there is no round-trip. This
 * pins the *stitching* contract, not the numeric output of any stage: the result is internally
 * consistent (grid sized to the image, indices valid, every per-colour list aligned to the palette,
 * counts recomputed from the grid and summing to the area), deterministic, and rejects an empty image.
 */
class BuildPatternTest {

    /** A varied sRGB image (packed ARGB): R ramps across x, G across y, B adds a little structure. */
    private fun image(w: Int, h: Int): IntArray {
        val px = IntArray(w * h)
        for (i in px.indices) {
            val x = i % w; val y = i / w
            val r = 20 + 215 * x / (w - 1)
            val g = 30 + 200 * y / (h - 1)
            val b = 60 + 40 * ((x + y) % 4)
            px[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return px
    }

    private fun solid(w: Int, h: Int, argb: Int): IntArray = IntArray(w * h) { argb }

    @Test
    fun dimensionsAndGridSize() {
        val r = buildPattern(image(12, 8), 12, 8)
        assertEquals(12, r.width)
        assertEquals(8, r.height)
        assertEquals(12 * 8, r.indexGrid.size)
    }

    @Test
    fun everyPerColourListAlignsToPalette() {
        val r = buildPattern(image(12, 8), 12, 8)
        assertTrue(r.palette.size >= 1, "a non-empty image yields at least one colour")
        assertEquals(r.palette.size, r.matches.size)
        assertEquals(r.palette.size, r.symbols.size)
        assertEquals(r.palette.size, r.counts.size)
        for (i in r.matches.indices) assertEquals(i, r.matches[i].paletteIndex, "match $i indexes its colour")
    }

    @Test
    fun gridIndicesAreValid() {
        val r = buildPattern(image(12, 8), 12, 8)
        assertTrue(r.indexGrid.all { it in 0 until r.palette.size }, "every cell indexes a real palette colour")
    }

    @Test
    fun symbolsAreDistinct() {
        val r = buildPattern(image(12, 8), 12, 8)
        assertEquals(r.symbols.size, r.symbols.toSet().size, "no two colours share a glyph")
    }

    @Test
    fun countsMatchGridAndSumToArea() {
        val r = buildPattern(image(12, 8), 12, 8)
        val recomputed = IntArray(r.palette.size)
        for (idx in r.indexGrid) recomputed[idx]++
        assertArrayEquals(recomputed, r.counts, "counts are the per-colour cell tallies")
        assertEquals(12 * 8, r.counts.sum(), "every stitch is counted exactly once")
    }

    @Test
    fun deterministic() {
        val a = buildPattern(image(12, 8), 12, 8)
        val b = buildPattern(image(12, 8), 12, 8)
        assertEquals(a.palette.size, b.palette.size)
        assertArrayEquals(a.indexGrid, b.indexGrid)
        assertArrayEquals(a.counts, b.counts)
        assertEquals(a.symbols, b.symbols)
        assertEquals(a.matches.map { it.thread.code }, b.matches.map { it.thread.code })
    }

    @Test
    fun rejectsEmptyImage() {
        assertThrows(IllegalArgumentException::class.java) {
            buildPattern(IntArray(0), 0, 0)
        }
    }

    // --- regressions: integration edge cases ------------------------------------------------------

    @Test
    fun solidImageStaysConsistent() {
        val gray = (0xFF shl 24) or (0x80 shl 16) or (0x80 shl 8) or 0x80
        val r = buildPattern(solid(8, 8, gray), 8, 8)
        assertTrue(r.palette.size in 1..K0_TARGET, "a uniform image does not explode into many colours")
        assertEquals(8 * 8, r.indexGrid.size)
        assertTrue(r.indexGrid.all { it in 0 until r.palette.size })
        assertEquals(8 * 8, r.counts.sum())
    }

    @Test
    fun lowColourImageSkipsKneedleWhenBelowK0() {
        // A two-tone checkerboard cannot spread-separate into 14 colours, so refined.size < K0 and
        // Kneedle must be skipped — buildPattern must still return a valid, consistent result.
        val w = 8; val h = 8
        val px = IntArray(w * h)
        for (i in px.indices) {
            val dark = ((i % w) + (i / w)) % 2 == 0
            val v = if (dark) 0x30 else 0xD0
            px[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        val r = buildPattern(px, w, h)
        assertTrue(r.palette.size in 1..K0_TARGET, "low-colour image stays at or below the k0 floor")
        assertEquals(w * h, r.indexGrid.size)
        assertTrue(r.indexGrid.all { it in 0 until r.palette.size })
        assertEquals(w * h, r.counts.sum())
    }
}
