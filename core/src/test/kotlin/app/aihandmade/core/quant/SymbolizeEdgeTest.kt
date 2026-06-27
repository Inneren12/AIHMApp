package app.aihandmade.core.quant

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Edge cases for Symbolize: assignSymbols ranks colours by OKLab L only, so non-finite lightness
 * values would make the ordering ambiguous. Guard against NaN and infinities.
 */
class SymbolizeEdgeTest {

    @Test
    fun rejectsNaNLightness() {
        assertThrows(IllegalArgumentException::class.java) {
            assignSymbols(Palette(floatArrayOf(Float.NaN), floatArrayOf(0f), floatArrayOf(0f), 0))
        }
    }

    @Test
    fun rejectsInfiniteLightness() {
        assertThrows(IllegalArgumentException::class.java) {
            assignSymbols(Palette(floatArrayOf(Float.POSITIVE_INFINITY), floatArrayOf(0f), floatArrayOf(0f), 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            assignSymbols(Palette(floatArrayOf(Float.NEGATIVE_INFINITY), floatArrayOf(0f), floatArrayOf(0f), 0))
        }
    }
}
