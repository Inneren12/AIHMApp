package app.aihandmade.core.quant

/**
 * Chart glyphs, ordered heaviest-ink first. Darker palette colours take earlier (heavier) glyphs.
 * Curated for visual distinctness; it may be retuned for the app's chart font later, but it must stay
 * duplicate-free and ink-weight-ordered.
 */
val SYMBOL_POOL: List<Char> = listOf(
    '●', '◆', '■', '▲', '▼', '★', '⬢', '✚', '✖', '❖', '◉', '◐', '◧', '◭', '⊕', '⊗',
    '▣', '▩', '◫', '⊞', '✦', '✱', '○', '◇', '□', '△', '▽', '☆', '⊙', '⬡', '⬠', '◌',
)

/**
 * One distinct chart glyph per palette colour. Colours are ranked by lightness (OKLab `L`, ascending;
 * ties by ascending palette index) and given glyphs from [SYMBOL_POOL] in that order — the darkest
 * colour gets [SYMBOL_POOL]`[0]`, the next-darkest the next, and so on. The glyph for palette colour
 * `i` is at `result[i]`.
 */
fun assignSymbols(palette: Palette): List<Char> {
    require(palette.size >= 1) { "palette must have at least one colour" }
    require(palette.size <= SYMBOL_POOL.size) { "palette larger than symbol pool" }
    for (i in 0 until palette.size) {
        require(palette.L[i].isFinite()) {
            "palette lightness values must be finite"
        }
    }

    val order = (0 until palette.size).sortedWith(compareBy({ palette.L[it] }, { it }))
    val out = CharArray(palette.size)
    for (k in order.indices) out[order[k]] = SYMBOL_POOL[k]
    return out.toList()
}
