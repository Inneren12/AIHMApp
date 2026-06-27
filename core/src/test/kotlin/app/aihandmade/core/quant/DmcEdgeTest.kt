package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.Srgb
import app.aihandmade.core.color.toOkLab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Edge / hardening tests for [matchPaletteToDmc]: input validation (finite palette coordinates,
 * well-formed catalogue threads) and the deterministic smallest-index tie-break.
 *
 * These complement the immutable [DmcTest] contract and only ADD coverage — they never weaken it.
 */
class DmcEdgeTest {

    private val cat = listOf(
        DmcThread("RED", "Red", 0xFF0000),
        DmcThread("GRN", "Green", 0x00FF00),
        DmcThread("BLU", "Blue", 0x0000FF),
    )

    private fun okLabOf(rgb: Int) = Srgb(0xFF000000.toInt() or rgb).toOkLab()

    private fun paletteOf(colours: List<OkLab>) = Palette(
        FloatArray(colours.size) { colours[it].L },
        FloatArray(colours.size) { colours[it].a },
        FloatArray(colours.size) { colours[it].b },
        anchorCount = 0,
    )

    /** Build a one-colour palette directly from raw OKLab coordinates (lets us inject NaN/Infinity). */
    private fun rawPalette(L: Float, a: Float, b: Float) =
        Palette(floatArrayOf(L), floatArrayOf(a), floatArrayOf(b), anchorCount = 0)

    @Test
    fun nanPaletteCoordinateThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            matchPaletteToDmc(rawPalette(Float.NaN, 0f, 0f), cat)
        }
        assertThrows(IllegalArgumentException::class.java) {
            matchPaletteToDmc(rawPalette(0.5f, Float.NaN, 0f), cat)
        }
        assertThrows(IllegalArgumentException::class.java) {
            matchPaletteToDmc(rawPalette(0.5f, 0f, Float.NaN), cat)
        }
    }

    @Test
    fun infinitePaletteCoordinateThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            matchPaletteToDmc(rawPalette(Float.POSITIVE_INFINITY, 0f, 0f), cat)
        }
        assertThrows(IllegalArgumentException::class.java) {
            matchPaletteToDmc(rawPalette(0.5f, Float.NEGATIVE_INFINITY, 0f), cat)
        }
    }

    @Test
    fun negativeThreadRgbThrows() {
        val bad = listOf(DmcThread("BAD", "Bad", -1))
        assertThrows(IllegalArgumentException::class.java) {
            matchPaletteToDmc(paletteOf(listOf(okLabOf(0xFF0000))), bad)
        }
    }

    @Test
    fun threadRgbAboveWhiteThrows() {
        // 0x01FF0000 carries an alpha/high bit beyond 0xRRGGBB.
        val bad = listOf(DmcThread("BAD", "Bad", 0xFF0000 or 0x01000000))
        assertThrows(IllegalArgumentException::class.java) {
            matchPaletteToDmc(paletteOf(listOf(okLabOf(0xFF0000))), bad)
        }
    }

    @Test
    fun blankThreadCodeThrows() {
        val bad = listOf(DmcThread("  ", "Red", 0xFF0000))
        assertThrows(IllegalArgumentException::class.java) {
            matchPaletteToDmc(paletteOf(listOf(okLabOf(0xFF0000))), bad)
        }
    }

    @Test
    fun blankThreadNameThrows() {
        val bad = listOf(DmcThread("RED", "", 0xFF0000))
        assertThrows(IllegalArgumentException::class.java) {
            matchPaletteToDmc(paletteOf(listOf(okLabOf(0xFF0000))), bad)
        }
    }

    @Test
    fun tieBreakPrefersSmallestCatalogueIndex() {
        // Two threads with identical RGB -> dE is identical; the smallest index must win.
        val dupCat = listOf(
            DmcThread("DUP_A", "Red A", 0xFF0000),
            DmcThread("DUP_B", "Red B", 0xFF0000),
            DmcThread("BLU", "Blue", 0x0000FF),
        )
        val pal = paletteOf(listOf(okLabOf(0xFF0000)))
        val match = matchPaletteToDmc(pal, dupCat).first()
        assertEquals("DUP_A", match.thread.code, "tie must resolve to the smallest catalogue index")
    }
}
