package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.OkLabPlanes
import app.aihandmade.core.color.deltaSqOk

fun ditherFloydSteinberg(image: OkLabPlanes, palette: Palette): IntArray {
    require(palette.size >= 1) { "palette must not be empty" }
    val width = image.width
    val height = image.height
    val out = IntArray(image.L.size)

    var curL = FloatArray(width); var curA = FloatArray(width); var curB = FloatArray(width)
    var nextL = FloatArray(width); var nextA = FloatArray(width); var nextB = FloatArray(width)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val idx = y * width + x
            val L = image.L[idx] + curL[x]
            val a = image.a[idx] + curA[x]
            val b = image.b[idx] + curB[x]

            var best = 0
            var bestSq = Float.POSITIVE_INFINITY
            for (c in 0 until palette.size) {
                val sq = deltaSqOk(OkLab(L, a, b), OkLab(palette.L[c], palette.a[c], palette.b[c]))
                if (sq < bestSq) { bestSq = sq; best = c }
            }
            out[idx] = best

            val eL = L - palette.L[best]
            val ea = a - palette.a[best]
            val eb = b - palette.b[best]

            if (x + 1 < width) { curL[x + 1] += eL * 7f / 16f; curA[x + 1] += ea * 7f / 16f; curB[x + 1] += eb * 7f / 16f }
            if (x - 1 >= 0) { nextL[x - 1] += eL * 3f / 16f; nextA[x - 1] += ea * 3f / 16f; nextB[x - 1] += eb * 3f / 16f }
            nextL[x] += eL * 5f / 16f; nextA[x] += ea * 5f / 16f; nextB[x] += eb * 5f / 16f
            if (x + 1 < width) { nextL[x + 1] += eL * 1f / 16f; nextA[x + 1] += ea * 1f / 16f; nextB[x + 1] += eb * 1f / 16f }
        }
        val tmpL = curL; curL = nextL; nextL = tmpL
        val tmpA = curA; curA = nextA; nextA = tmpA
        val tmpB = curB; curB = nextB; nextB = tmpB
        nextL.fill(0f); nextA.fill(0f); nextB.fill(0f)
    }

    return out
}
