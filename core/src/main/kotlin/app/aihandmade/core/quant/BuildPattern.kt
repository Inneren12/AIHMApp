package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.OkLabPlanes
import app.aihandmade.core.color.toLinearRgb
import app.aihandmade.core.color.toSrgb

/** Everything the chart, floss list and export need from one photo. The cross-module handoff object. */
data class PatternResult(
    val width: Int,                 // stitches across (== image.width)
    val height: Int,                // stitches down  (== image.height)
    val palette: Palette,           // the chosen colours (OKLab)
    val indexGrid: IntArray,        // size width*height, each value in 0 until palette.size
    val matches: List<DmcMatch>,    // size palette.size; matches[i] is colour i's nearest DMC thread
    val symbols: List<Char>,        // size palette.size; symbols[i] is colour i's chart glyph
    val counts: IntArray,           // size palette.size; counts[i] = stitches using colour i; sum == width*height
)

/**
 * Turn a prescaled working-space image into a full cross-stitch [PatternResult]: sample -> init ->
 * greedy -> refine -> Kneedle (auto K) -> dither -> DMC match -> symbolize -> counts.
 *
 * The image is assumed to be in working space already at the target stitch dimensions (W×H): this
 * function does not prescale or run the DecisionEngine — those are earlier pipeline steps. Auto K
 * only (Kneedle picks K* ≥ k0 = 14); manual colour counts are a follow-up. Determinism is inherited
 * from the seeded sampling stage.
 */
/** Auto-K colour ceiling: greedy growth grows the k0 floor up toward this before Kneedle scans [k0, K*]. */
private const val K_MAX_AUTO = 64

fun buildPattern(image: OkLabPlanes, catalog: List<DmcThread> = DMC_CATALOG): PatternResult {
    require(image.width >= 1 && image.height >= 1) { "image must be non-empty" }

    // Stitch dimensions are already final, so every pixel is a sample.
    val samples = samplePixels(image.toPackedSrgb(), image.width, image.height, targetSamples = image.size)
    val p0 = initPalette(samples)
    val grown = greedyGrow(samples, p0, kTry = (K_MAX_AUTO - p0.size).coerceAtLeast(0))
    val refined = refinePalette(samples, grown)

    val kStar = selectK(samples, refined, k0 = K0_TARGET).kStar
    val k = kStar.coerceIn(1, refined.size)

    // The only new logic: the palette Kneedle scored is the size-k prefix of `refined`. Don't re-refine.
    val palette = Palette(
        refined.L.copyOf(k), refined.a.copyOf(k), refined.b.copyOf(k),
        refined.anchorCount.coerceAtMost(k),
    )

    val indexGrid = ditherFloydSteinberg(image, palette)
    val matches = matchPaletteToDmc(palette, catalog)
    val symbols = assignSymbols(palette)

    val counts = IntArray(palette.size)
    for (idx in indexGrid) counts[idx]++

    return PatternResult(image.width, image.height, palette, indexGrid, matches, symbols, counts)
}

/** Pack working-space OKLab planes back to ARGB so the seeded sampler can consume them. */
private fun OkLabPlanes.toPackedSrgb(): IntArray =
    IntArray(size) { i -> OkLab(L[i], a[i], b[i]).toLinearRgb().toSrgb().argb }
