package app.aihandmade.core.quant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * EDGE/INTEGRATION tests for buildPattern's input guards and structural invariants that sit beside the
 * contract test. Pins overflow-safe sizing, the pixels<->dimensions agreement, and the rule that the
 * auto-K palette can never outgrow the chart-glyph pool (so assignSymbols always has a glyph to hand out).
 */
class BuildPatternEdgeTest {

    /** Same varied sRGB ramp the contract test uses, so palette size is realistic. */
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

    @Test
    fun rejectsPixelCountMismatch() {
        assertThrows(IllegalArgumentException::class.java) {
            buildPattern(IntArray(3), 2, 2)
        }
    }

    @Test
    fun rejectsOversizedDimensionsBeforeOverflow() {
        assertThrows(IllegalArgumentException::class.java) {
            buildPattern(IntArray(1), 100_000, 100_000)
        }
    }

    @Test
    fun paletteNeverExceedsSymbolPool() {
        val r = buildPattern(image(24, 24), 24, 24)
        assertTrue(r.palette.size <= SYMBOL_POOL.size, "auto-K palette stays within the chart-glyph pool")
        assertEquals(r.palette.size, r.symbols.size)
        assertEquals(r.symbols.size, r.symbols.toSet().size, "no two colours share a glyph")
    }
}
