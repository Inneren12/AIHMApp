package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLabPlanes
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
 * buildPattern takes a prescaled working-space image (already at the target stitch dimensions) and
 * runs sampling -> init -> greedy -> refine -> Kneedle (auto K) -> dither -> DMC match -> symbolize
 * -> counts, returning everything the chart and floss list need. This pins the *stitching* contract,
 * not the numeric output of any single stage: the result is internally consistent (grid sized to the
 * image, indices valid, every per-colour list aligned to the palette, counts recomputed from the grid
 * and summing to the area), deterministic, and rejects an empty image.
 */
class BuildPatternTest {

    /** A small varied OKLab image: L ramps across x, a varies across y, b adds a little structure. */
    private fun image(w: Int, h: Int): OkLabPlanes {
        val n = w * h
        val L = FloatArray(n); val a = FloatArray(n); val b = FloatArray(n)
        for (i in 0 until n) {
            val x = i % w; val y = i / w
            L[i] = 0.15f + 0.70f * (x.toFloat() / (w - 1))
            a[i] = -0.10f + 0.20f * (y.toFloat() / (h - 1))
            b[i] = 0.05f * (((x + y) % 3) - 1)
        }
        return OkLabPlanes(L, a, b, w, h)
    }

    @Test
    fun dimensionsAndGridSize() {
        val r = buildPattern(image(12, 8))
        assertEquals(12, r.width)
        assertEquals(8, r.height)
        assertEquals(12 * 8, r.indexGrid.size)
    }

    @Test
    fun everyPerColourListAlignsToPalette() {
        val r = buildPattern(image(12, 8))
        assertTrue(r.palette.size >= 1, "a non-empty image yields at least one colour")
        assertEquals(r.palette.size, r.matches.size)
        assertEquals(r.palette.size, r.symbols.size)
        assertEquals(r.palette.size, r.counts.size)
        for (i in r.matches.indices) assertEquals(i, r.matches[i].paletteIndex, "match $i indexes its colour")
    }

    @Test
    fun gridIndicesAreValid() {
        val r = buildPattern(image(12, 8))
        assertTrue(r.indexGrid.all { it in 0 until r.palette.size }, "every cell indexes a real palette colour")
    }

    @Test
    fun symbolsAreDistinct() {
        val r = buildPattern(image(12, 8))
        assertEquals(r.symbols.size, r.symbols.toSet().size, "no two colours share a glyph")
    }

    @Test
    fun countsMatchGridAndSumToArea() {
        val r = buildPattern(image(12, 8))
        val recomputed = IntArray(r.palette.size)
        for (idx in r.indexGrid) recomputed[idx]++
        assertArrayEquals(recomputed, r.counts, "counts are the per-colour cell tallies")
        assertEquals(12 * 8, r.counts.sum(), "every stitch is counted exactly once")
    }

    @Test
    fun deterministic() {
        val a = buildPattern(image(12, 8))
        val b = buildPattern(image(12, 8))
        assertEquals(a.palette.size, b.palette.size)
        assertArrayEquals(a.indexGrid, b.indexGrid)
        assertArrayEquals(a.counts, b.counts)
        assertEquals(a.symbols, b.symbols)
        assertEquals(a.matches.map { it.thread.code }, b.matches.map { it.thread.code })
    }

    @Test
    fun rejectsEmptyImage() {
        assertThrows(IllegalArgumentException::class.java) {
            buildPattern(OkLabPlanes(FloatArray(0), FloatArray(0), FloatArray(0), 0, 0))
        }
    }
}
