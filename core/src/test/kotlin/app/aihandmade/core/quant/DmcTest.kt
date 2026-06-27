package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.Srgb
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb
import app.aihandmade.core.color.toOkLab
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for the DMC catalogue module: map each palette colour to its nearest DMC thread.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * matchPaletteToDmc converts each palette colour and each thread to CIE-Lab (thread rgb is opaque
 * 0xRRGGBB -> Srgb -> OkLab -> linear -> Lab; palette is OkLab -> linear -> Lab) and returns, per
 * palette colour, the thread with the smallest CIEDE2000 (ties -> smallest catalogue index).
 *
 * Pinned here: a palette built from the catalogue maps back to itself (dE ~ 0); arbitrary matches equal
 * the independently-recomputed CIEDE2000 argmin; ties resolve to the smallest catalogue index;
 * determinism; the real DMC_CATALOG is loaded (454 threads incl. black 310 and White); empty inputs throw.
 */
class DmcTest {

    private val cat = listOf(
        DmcThread("RED", "Red", 0xFF0000),
        DmcThread("GRN", "Green", 0x00FF00),
        DmcThread("BLU", "Blue", 0x0000FF),
        DmcThread("BLK", "Black", 0x000000),
        DmcThread("WHT", "White", 0xFFFFFF),
    )

    private fun okLabOf(rgb: Int) = Srgb(0xFF000000.toInt() or rgb).toOkLab()
    private fun threadLab(t: DmcThread) = okLabOf(t.rgb).toLinearRgb().toLab()

    private fun paletteOf(colours: List<OkLab>) = Palette(
        FloatArray(colours.size) { colours[it].L },
        FloatArray(colours.size) { colours[it].a },
        FloatArray(colours.size) { colours[it].b },
        anchorCount = 0,
    )

    /** independent CIEDE2000 nearest over [cat], for self-verification. */
    private fun nearest(c: OkLab): Pair<DmcThread, Double> {
        val lab = c.toLinearRgb().toLab()
        var best = cat[0]
        var bd = Double.POSITIVE_INFINITY
        for (t in cat) {
            val d = deltaE2000(lab, threadLab(t))
            if (d < bd) { bd = d; best = t }
        }
        return best to bd
    }

    @Test
    fun exactColoursMapToTheirThread() {
        // palette built FROM the catalogue: each palette colour is a thread's OkLab -> identity match.
        val pal = paletteOf(cat.map { okLabOf(it.rgb) })
        val matches = matchPaletteToDmc(pal, cat)
        assertEquals(cat.size, matches.size)
        for (i in cat.indices) {
            assertEquals(i, matches[i].paletteIndex)
            assertEquals(cat[i].code, matches[i].thread.code, "palette[$i] should map to thread ${cat[i].code}")
            assertTrue(matches[i].deltaE < 1e-3, "exact colour must match with dE ~ 0")
        }
    }

    @Test
    fun matchesAreTheCiede2000Argmin() {
        // arbitrary palette -> each match equals the independently recomputed CIEDE2000 nearest thread.
        val pal = paletteOf(listOf(okLabOf(0xCC2020), okLabOf(0x3060A0), okLabOf(0xC0C0C0), okLabOf(0x404040)))
        val matches = matchPaletteToDmc(pal, cat)
        for (i in 0 until pal.size) {
            val (exp, expDe) = nearest(OkLab(pal.L[i], pal.a[i], pal.b[i]))
            assertEquals(exp.code, matches[i].thread.code, "palette[$i] must map to its CIEDE2000-nearest thread")
            assertEquals(expDe, matches[i].deltaE, 1e-6, "reported dE must be the CIEDE2000 to the chosen thread")
        }
    }

    @Test
    fun intuitiveMatches() {
        val pal = paletteOf(listOf(okLabOf(0xCC2020), okLabOf(0x3060A0), okLabOf(0xC0C0C0), okLabOf(0x404040)))
        val codes = matchPaletteToDmc(pal, cat).map { it.thread.code }
        assertEquals(listOf("RED", "BLU", "WHT", "BLK"), codes)
    }

    @Test
    fun tieBreaksToSmallestCatalogueIndex() {
        // two threads with identical rgb -> identical dE; the smallest catalogue index must win.
        val dup = listOf(
            DmcThread("DUP_A", "Red A", 0xFF0000),
            DmcThread("DUP_B", "Red B", 0xFF0000),
            DmcThread("BLU", "Blue", 0x0000FF),
        )
        val pal = paletteOf(listOf(okLabOf(0xFF0000)))
        assertEquals("DUP_A", matchPaletteToDmc(pal, dup).first().thread.code, "tie must resolve to smallest index")
    }

    @Test
    fun deterministic() {
        val pal = paletteOf(listOf(okLabOf(0xCC2020), okLabOf(0x3060A0), okLabOf(0x808080)))
        val a = matchPaletteToDmc(pal, cat).map { it.thread.code to it.deltaE }
        val b = matchPaletteToDmc(pal, cat).map { it.thread.code to it.deltaE }
        assertEquals(a, b)
    }

    @Test
    fun realCatalogIsLoaded() {
        assertEquals(454, DMC_CATALOG.size, "the full DMC Cotton catalogue")
        assertTrue(DMC_CATALOG.any { it.code == "310" }, "black 310 present")
        assertTrue(DMC_CATALOG.any { it.code == "White" }, "White present")
        // every thread maps to itself against the full catalogue (sanity over real data, first 1 colour)
        val first = DMC_CATALOG.first()
        val pal = paletteOf(listOf(okLabOf(first.rgb)))
        assertEquals(first.code, matchPaletteToDmc(pal, DMC_CATALOG).first().thread.code)
    }

    @Test
    fun invalidInputsThrow() {
        val pal = paletteOf(listOf(okLabOf(0xFF0000)))
        assertThrows(IllegalArgumentException::class.java) { matchPaletteToDmc(pal, emptyList()) }
        val emptyPal = Palette(FloatArray(0), FloatArray(0), FloatArray(0), 0)
        assertThrows(IllegalArgumentException::class.java) { matchPaletteToDmc(emptyPal, cat) }
    }
}
