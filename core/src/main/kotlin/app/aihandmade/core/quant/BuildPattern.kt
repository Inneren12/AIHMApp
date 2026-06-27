package app.aihandmade.core.quant

import app.aihandmade.core.color.toOkLabPlanes

/** Everything the chart, floss list and export need from one photo. The cross-module handoff object. */
data class PatternResult(
    val width: Int,                 // stitches across
    val height: Int,                // stitches down
    val palette: Palette,           // the chosen colours (OKLab)
    val indexGrid: IntArray,        // size width*height, each value in 0 until palette.size
    val matches: List<DmcMatch>,    // size palette.size; matches[i] is colour i's nearest DMC thread
    val symbols: List<Char>,        // size palette.size; symbols[i] is colour i's chart glyph
    val counts: IntArray,           // size palette.size; counts[i] = stitches using colour i; sum == width*height
)

/** Auto-K colour ceiling: greedy grows the k0 floor up toward this before Kneedle scans [k0, K*]. */
private const val K_MAX_AUTO = 64

/**
 * Turn a prescaled image (packed-ARGB sRGB pixels at the target stitch dimensions) into a full
 * cross-stitch [PatternResult]: sample -> init -> greedy -> refine -> Kneedle (auto K) -> dither ->
 * DMC match -> symbolize -> counts. Does not prescale or run the DecisionEngine (earlier steps).
 * Auto K only (Kneedle picks K* >= k0 = 14); manual colour counts are a follow-up.
 */
fun buildPattern(pixels: IntArray, width: Int, height: Int, catalog: List<DmcThread> = DMC_CATALOG): PatternResult {
    require(width >= 1 && height >= 1) { "image must be non-empty" }

    // Palette from the real importance-weighted sampler; dither planes from the SAME pixels via the
    // SAME conversion samplePixels uses internally -> identical OKLab values, no sRGB round-trip.
    val samples = samplePixels(pixels, width, height, targetSamples = width * height)
    val planes = pixels.toOkLabPlanes(width, height)

    val p0 = initPalette(samples)
    val grown = greedyGrow(samples, p0, kTry = (K_MAX_AUTO - p0.size).coerceAtLeast(0))
    val refined = refinePalette(samples, grown)

    // Kneedle needs at least k0 colours to scan; below that, keep every refined colour.
    val k =
        if (refined.size <= K0_TARGET) {
            refined.size
        } else {
            selectK(samples, refined, k0 = K0_TARGET).kStar.coerceIn(1, refined.size)
        }

    // The only new logic: the palette Kneedle scored is the size-k prefix of `refined`. Don't re-refine.
    val palette = Palette(
        refined.L.copyOf(k), refined.a.copyOf(k), refined.b.copyOf(k),
        refined.anchorCount.coerceAtMost(k),
    )

    val indexGrid = ditherFloydSteinberg(planes, palette)
    val matches = matchPaletteToDmc(palette, catalog)
    val symbols = assignSymbols(palette)

    val counts = IntArray(palette.size)
    for (idx in indexGrid) counts[idx]++

    return PatternResult(width, height, palette, indexGrid, matches, symbols, counts)
}
