package app.aihandmade.core.quant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for Symbolize: assign one distinct chart glyph to every palette colour.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * assignSymbols ranks the palette colours by lightness (OKLab L, ascending; ties by ascending index)
 * and hands out glyphs from SYMBOL_POOL in that order, so the darkest colour takes the heaviest symbol
 * (SYMBOL_POOL[0]), the next-darkest the next, and so on. The glyph for palette colour i is result[i].
 *
 * Pinned here: one glyph per colour; all glyphs distinct; the darkest colour gets SYMBOL_POOL[0] and
 * symbols follow pool order along ascending lightness; ties break by index; determinism; the pool is
 * distinct and ample; empty or oversize palettes throw.
 */
class SymbolizeTest {

    private fun palette(ls: FloatArray) =
        Palette(ls, FloatArray(ls.size) { 0f }, FloatArray(ls.size) { 0f }, anchorCount = 0)

    @Test
    fun oneSymbolPerColour() {
        val s = assignSymbols(palette(floatArrayOf(0.2f, 0.5f, 0.8f, 0.4f)))
        assertEquals(4, s.size)
    }

    @Test
    fun allSymbolsDistinct() {
        val s = assignSymbols(palette(FloatArray(20) { it / 20f }))
        assertEquals(s.size, s.toSet().size, "no two colours may share a glyph")
    }

    @Test
    fun symbolsFollowPoolOrderAlongLightness() {
        val ls = floatArrayOf(0.9f, 0.1f, 0.5f, 0.3f)
        val s = assignSymbols(palette(ls))
        val order = ls.indices.sortedWith(compareBy({ ls[it] }, { it }))
        for (k in order.indices) {
            assertEquals(SYMBOL_POOL[k], s[order[k]], "rank $k along ascending lightness")
        }
        val darkest = ls.indices.minByOrNull { ls[it] }!!
        assertEquals(SYMBOL_POOL[0], s[darkest], "darkest colour takes the heaviest glyph")
    }

    @Test
    fun tieBreaksByIndex() {
        // colours 0 and 1 share a lightness; colour 2 is darkest.
        val s = assignSymbols(palette(floatArrayOf(0.5f, 0.5f, 0.2f)))
        assertEquals(SYMBOL_POOL[0], s[2], "darkest -> heaviest")
        assertEquals(SYMBOL_POOL[1], s[0], "tie resolves to the lower index first")
        assertEquals(SYMBOL_POOL[2], s[1])
    }

    @Test
    fun deterministic() {
        val p = palette(floatArrayOf(0.3f, 0.7f, 0.1f, 0.9f, 0.5f))
        assertEquals(assignSymbols(p), assignSymbols(p))
    }

    @Test
    fun poolIsDistinctAndAmple() {
        assertEquals(SYMBOL_POOL.size, SYMBOL_POOL.toSet().size, "pool has no duplicate glyphs")
        assertTrue(SYMBOL_POOL.size >= 24, "enough symbols for realistic palettes")
    }

    @Test
    fun rejectsEmptyAndOversizePalettes() {
        assertThrows(IllegalArgumentException::class.java) {
            assignSymbols(Palette(FloatArray(0), FloatArray(0), FloatArray(0), 0))
        }
        val tooMany = SYMBOL_POOL.size + 1
        assertThrows(IllegalArgumentException::class.java) {
            assignSymbols(palette(FloatArray(tooMany) { it.toFloat() }))
        }
    }
}
