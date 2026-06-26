package app.aihandmade.core.metrics

import app.aihandmade.core.color.LabPlanes
import app.aihandmade.core.color.toLabPlanes

/** Core: banding/posterization score in [0,1] from the L* channels of two equal-sized planes. */
fun bandingScore(reference: LabPlanes, candidate: LabPlanes): Double {
    require(reference.width == candidate.width && reference.height == candidate.height) {
        "reference and candidate must have equal dimensions"
    }
    val width = reference.width
    val height = reference.height
    require(width >= 1 && height >= 1) { "images must be non-empty" }

    val refL = reference.L
    val candL = candidate.L

    var denom = 0
    var num = 0

    for (y in 0 until height)
        for (x in 0 until width - 1) {
            val i = y * width + x; val j = i + 1
            if (refL[i] != refL[j]) { denom++; if (candL[i] == candL[j]) num++ }
        }

    for (y in 0 until height - 1)
        for (x in 0 until width) {
            val i = y * width + x; val j = i + width
            if (refL[i] != refL[j]) { denom++; if (candL[i] == candL[j]) num++ }
        }

    return if (denom == 0) 0.0 else num.toDouble() / denom.toDouble()
}

/** Convenience: convert both ARGB images via core/color `toLabPlanes`, then delegate to the core. */
fun bandingScore(reference: IntArray, candidate: IntArray, width: Int, height: Int): Double =
    bandingScore(reference.toLabPlanes(width, height), candidate.toLabPlanes(width, height))
