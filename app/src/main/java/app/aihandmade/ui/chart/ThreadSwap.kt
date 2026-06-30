package app.aihandmade.ui.chart

import app.aihandmade.core.color.Lab
import app.aihandmade.core.color.Srgb
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb
import app.aihandmade.core.color.toOkLab
import app.aihandmade.core.quant.DMC_CATALOG

/** A brand-qualified thread reference. brand defaults to "DMC" (only brand today); kept explicit so
 *  inventory storage and future non-DMC catalogs slot in without reworking call sites. */
data class ThreadRef(val brand: String, val code: String, val name: String, val argb: Int)

data class ThreadCandidate(val ref: ThreadRef, val delta: Double)

/** Honest "how different is this swap" bands on CIEDE2000. */
enum class SwapBand { NEAR, SIMILAR, NOTICEABLE, VERY_DIFFERENT }

fun bandOf(delta: Double): SwapBand = when {
    delta < 1.5 -> SwapBand.NEAR
    delta < 4.0 -> SwapBand.SIMILAR
    delta < 8.0 -> SwapBand.NOTICEABLE
    else -> SwapBand.VERY_DIFFERENT
}

/** Full DMC catalog as brand-qualified refs (opaque ARGB), computed once. */
val DMC_THREAD_REFS: List<ThreadRef> = DMC_CATALOG.map {
    ThreadRef("DMC", it.code, it.name, 0xFF000000.toInt() or it.rgb)
}

// EXACT chain matchPaletteToDmc uses for a thread colour → Lab (not the shorter Srgb.toLab()).
private fun labOf(argb: Int): Lab = Srgb(argb).toOkLab().toLinearRgb().toLab()

private val DMC_THREAD_LABS: List<Lab> = DMC_THREAD_REFS.map { labOf(it.argb) }

/** 6 nearest DMC threads to [argb] by CIEDE2000, excluding [excludeCode], ascending. */
fun nearestThreads(argb: Int, excludeCode: String, n: Int = 6): List<ThreadCandidate> {
    val queryLab = labOf(argb)
    val out = ArrayList<ThreadCandidate>(DMC_THREAD_REFS.size)
    for (i in DMC_THREAD_REFS.indices) {
        val ref = DMC_THREAD_REFS[i]
        if (ref.code == excludeCode) continue
        out.add(ThreadCandidate(ref, deltaE2000(queryLab, DMC_THREAD_LABS[i])))
    }
    out.sortBy { it.delta }
    return if (out.size > n) ArrayList(out.subList(0, n)) else out
}

/** Catalog search by code or name (case-insensitive substring); first [limit] of all when blank. */
fun searchThreads(query: String, limit: Int = 80): List<ThreadRef> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return DMC_THREAD_REFS.take(limit)
    val out = ArrayList<ThreadRef>(limit)
    for (ref in DMC_THREAD_REFS) {
        if (ref.code.lowercase().contains(q) || ref.name.lowercase().contains(q)) {
            out.add(ref)
            if (out.size >= limit) break
        }
    }
    return out
}
