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

// TEMPORARY STAGE TRACING (QuantTrace) — locate the non-terminating stage on a real 12MP photo.
// :core is a pure-JVM module, so android.util.Log is NOT on the compile classpath; println is used
// instead. On Android, stdout is mirrored to logcat (tag "System.out"), so `adb logcat | grep QuantTrace`
// surfaces these exactly like a Log tag would. Remove once the stuck stage is identified.
internal fun qtrace(msg: String) = println("QuantTrace $msg")

/**
 * Turn a prescaled image (packed-ARGB sRGB pixels at the target stitch dimensions) into a full
 * cross-stitch [PatternResult]: sample -> init -> greedy -> refine -> Kneedle (auto K) -> dither ->
 * DMC match -> symbolize -> counts. Does not prescale or run the DecisionEngine (earlier steps).
 * Auto K only (Kneedle picks K* >= k0 = 14); manual colour counts are a follow-up.
 *
 * Palette sampling and dithering both derive OKLab from the same packed sRGB pixels through the same
 * core conversion path, so there is no sRGB round-trip and both stages see identical OKLab values.
 */
fun buildPattern(pixels: IntArray, width: Int, height: Int, catalog: List<DmcThread> = DMC_CATALOG): PatternResult {
    require(width >= 1 && height >= 1) { "image must be non-empty" }

    val sizeLong = width.toLong() * height.toLong()
    require(sizeLong <= Int.MAX_VALUE) { "image size too large" }
    val size = sizeLong.toInt()
    require(pixels.size == size) { "pixels size must equal width * height" }

    // --- QuantTrace: elapsed-ms entry/exit around each stage (TEMPORARY) ---
    val tBuild = System.nanoTime()
    fun ms(sinceNanos: Long): Long = (System.nanoTime() - sinceNanos) / 1_000_000
    qtrace("buildPattern start width=$width height=$height size=$size")

    // Palette from the real importance-weighted sampler; dither planes from the SAME pixels via the
    // SAME conversion samplePixels uses internally -> identical OKLab values, no sRGB round-trip.
    var t = System.nanoTime()
    qtrace("samplePixels start targetSamples=$size")
    val samples = samplePixels(pixels, width, height, targetSamples = size)
    qtrace("samplePixels end sampleCount=${samples.size} elapsedMs=${ms(t)}")

    val planes = pixels.toOkLabPlanes(width, height)

    // Auto-K can never outgrow the chart-glyph pool, or assignSymbols would have nothing to hand out.
    val kCap = minOf(K_MAX_AUTO, SYMBOL_POOL.size)

    t = System.nanoTime()
    qtrace("initPalette start")
    val p0 = initPalette(samples)
    qtrace("initPalette end k0=${p0.size} anchors=${p0.anchorCount} elapsedMs=${ms(t)}")

    val kTry = (kCap - p0.size).coerceAtLeast(0)
    t = System.nanoTime()
    qtrace("greedyGrow start kTry=$kTry initSize=${p0.size}")
    val grown = greedyGrow(samples, p0, kTry = kTry)
    qtrace("greedyGrow end size=${grown.size} elapsedMs=${ms(t)}")

    t = System.nanoTime()
    qtrace("refinePalette start passes=$REFINE_PASSES paletteSize=${grown.size}")
    val refined = refinePalette(samples, grown)
    qtrace("refinePalette end size=${refined.size} elapsedMs=${ms(t)}")

    // Kneedle needs at least k0 colours to scan; below that, keep every refined colour.
    t = System.nanoTime()
    qtrace("selectK start refinedSize=${refined.size} k0Target=$K0_TARGET")
    val k =
        if (refined.size <= K0_TARGET) {
            qtrace("selectK skipped refinedSize<=k0Target chosenK=${refined.size}")
            refined.size
        } else {
            selectK(samples, refined, k0 = K0_TARGET).kStar.coerceIn(1, refined.size)
        }
    qtrace("selectK end chosenK=$k elapsedMs=${ms(t)}")

    // The only new logic: the palette Kneedle scored is the size-k prefix of `refined`. Don't re-refine.
    val palette = Palette(
        refined.L.copyOf(k), refined.a.copyOf(k), refined.b.copyOf(k),
        refined.anchorCount.coerceAtMost(k),
    )
    require(palette.size <= SYMBOL_POOL.size) { "palette larger than symbol pool" }

    t = System.nanoTime()
    qtrace("ditherFloydSteinberg start paletteSize=${palette.size} pixels=$size")
    val indexGrid = ditherFloydSteinberg(planes, palette)
    qtrace("ditherFloydSteinberg end elapsedMs=${ms(t)}")

    val matches = matchPaletteToDmc(palette, catalog)
    val symbols = assignSymbols(palette)

    val counts = IntArray(palette.size)
    for (idx in indexGrid) counts[idx]++

    qtrace("buildPattern end paletteSize=${palette.size} totalMs=${ms(tBuild)}")
    return PatternResult(width, height, palette, indexGrid, matches, symbols, counts)
}
