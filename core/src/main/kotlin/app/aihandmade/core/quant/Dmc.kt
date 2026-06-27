package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.Srgb
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb
import app.aihandmade.core.color.toOkLab

/** A DMC Cotton floss colour. rgb is opaque 0xRRGGBB. */
data class DmcThread(val code: String, val name: String, val rgb: Int)

/** The nearest thread for one palette colour. */
data class DmcMatch(val paletteIndex: Int, val thread: DmcThread, val deltaE: Double)

/**
 * Map each palette colour to the nearest DMC Cotton floss colour by **CIEDE2000 on CIE-Lab** — the
 * final perceptual "which real thread looks closest" decision (single thread; blends deferred).
 *
 * Both sides convert to CIE-Lab through core/color only: a thread's opaque rgb goes
 * `Srgb -> OkLab -> linear -> Lab`; a palette colour goes `OkLab -> linear -> Lab`. The thread Labs
 * are precomputed once and reused across every palette colour (palette x catalogue is tiny here).
 *
 * Deterministic: for each palette colour the result is the `argmin` of [deltaE2000] over the
 * catalogue, with ties resolved to the smallest catalogue index. Different palette colours may map
 * to the same thread; distinct assignment is a later concern.
 *
 * Inputs are validated up front: the palette must be non-empty with finite OKLab coordinates, and
 * the catalogue must be non-empty with well-formed threads (non-blank code/name, opaque `0xRRGGBB`
 * rgb). This fails fast instead of letting NaN/Infinity poison the Lab/deltaE maths (silently
 * returning `catalog[0]`) or letting stray alpha/high bits be OR-ed into the ARGB value.
 */
fun matchPaletteToDmc(palette: Palette, catalog: List<DmcThread> = DMC_CATALOG): List<DmcMatch> {
    require(palette.size >= 1) { "palette must have at least one colour" }
    require(catalog.isNotEmpty()) { "catalog must not be empty" }

    for (i in 0 until palette.size) {
        require(palette.L[i].isFinite() && palette.a[i].isFinite() && palette.b[i].isFinite()) {
            "palette OKLab coordinates must be finite"
        }
    }

    for (t in catalog) {
        require(t.code.isNotBlank()) { "thread code must not be blank" }
        require(t.name.isNotBlank()) { "thread name must not be blank" }
        require(t.rgb in 0x000000..0xFFFFFF) { "thread rgb must be 0xRRGGBB" }
    }

    val threadLabs = catalog.map {
        val argb = 0xFF000000.toInt() or it.rgb
        Srgb(argb).toOkLab().toLinearRgb().toLab()
    }

    return List(palette.size) { i ->
        val palLab = OkLab(palette.L[i], palette.a[i], palette.b[i]).toLinearRgb().toLab()
        var best = 0
        var bestDe = deltaE2000(palLab, threadLabs[0])
        for (j in 1 until threadLabs.size) {
            val d = deltaE2000(palLab, threadLabs[j])
            if (d < bestDe) { bestDe = d; best = j }
        }
        DmcMatch(i, catalog[best], bestDe)
    }
}
