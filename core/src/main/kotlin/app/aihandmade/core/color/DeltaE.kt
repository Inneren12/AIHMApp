package app.aihandmade.core.color

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** `25^7`, precomputed so the CIEDE2000 chroma terms never call `pow`. */
private const val POW_25_7 = 6103515625.0

/**
 * The canonical **CIEDE2000** colour difference between two [Lab] colours (Sharma, Wu & Dalal 2005),
 * with `kL = kC = kH = 1`.
 *
 * This is the **single** ΔE used for reporting, catalog matching, and thresholds. Because it accepts
 * only [Lab], it is structurally impossible to apply it to OKLab (the root defect of the previous
 * engine). Hot-loop OKLab distance arrives separately as `deltaSqOk(OkLab, ...)` in a later commit.
 *
 * **Do NOT use this in per-pixel inner loops:** it costs 50+ floating-point operations and several
 * transcendental calls per evaluation. It is reserved for metrics and matching, not hot paths.
 *
 * Verified against the published 34-pair Sharma reference table (see `DeltaE2000SharmaTest`), the
 * accepted proof of a correct CIEDE2000 implementation.
 */
fun deltaE2000(x: Lab, y: Lab): Double {
    // Promote inputs to Double for accuracy.
    val L1 = x.L.toDouble(); val a1 = x.a.toDouble(); val b1 = x.b.toDouble()
    val L2 = y.L.toDouble(); val a2 = y.a.toDouble(); val b2 = y.b.toDouble()

    // 1. Mean chroma of the original (a, b) coordinates.
    val C1 = hypot(a1, b1)
    val C2 = hypot(a2, b2)
    val cBar = (C1 + C2) / 2.0

    // 2. G compensation factor — note ^7 via repeated multiply, not pow.
    val cBar7 = pow7(cBar)
    val g = 0.5 * (1.0 - sqrt(cBar7 / (cBar7 + POW_25_7)))

    // 3. Adjusted a' values.
    val a1p = (1.0 + g) * a1
    val a2p = (1.0 + g) * a2

    // 4. Adjusted chroma C'.
    val c1p = hypot(a1p, b1)
    val c2p = hypot(a2p, b2)

    // 5. Adjusted hue h' in degrees, with the chroma-zero guard (h' = 0 when C' == 0).
    val h1p = hueDegrees(b1, a1p, c1p)
    val h2p = hueDegrees(b2, a2p, c2p)

    // 6. Lightness difference.
    val dLp = L2 - L1

    // 7. Chroma difference.
    val dCp = c2p - c1p

    // 8. Hue difference dh' with the chroma-zero guard and the dh' wrap branches.
    val dhp: Double = when {
        c1p * c2p == 0.0 -> 0.0
        Math.abs(h2p - h1p) <= 180.0 -> h2p - h1p
        h2p - h1p > 180.0 -> (h2p - h1p) - 360.0
        else -> (h2p - h1p) + 360.0
    }

    // 9. Weighted hue difference dH'.
    val dHp = 2.0 * sqrt(c1p * c2p) * sin(radians(dhp / 2.0))

    // 10. Means of lightness and adjusted chroma.
    val lBar = (L1 + L2) / 2.0
    val cBarp = (c1p + c2p) / 2.0

    // 11. Mean hue h-bar', with the chroma-zero guard and the (distinct) h-bar wrap branches.
    val hBar: Double = when {
        c1p * c2p == 0.0 -> h1p + h2p
        Math.abs(h1p - h2p) <= 180.0 -> (h1p + h2p) / 2.0
        h1p + h2p < 360.0 -> (h1p + h2p + 360.0) / 2.0
        else -> (h1p + h2p - 360.0) / 2.0
    }

    // 12. Hue-dependent T.
    val t = 1.0 -
        0.17 * cos(radians(hBar - 30.0)) +
        0.24 * cos(radians(2.0 * hBar)) +
        0.32 * cos(radians(3.0 * hBar + 6.0)) -
        0.20 * cos(radians(4.0 * hBar - 63.0))

    // 13. Hue-rotation term dTheta.
    val dTheta = 30.0 * exp(-square((hBar - 275.0) / 25.0))

    // 14. Chroma-rotation Rc — ^7 via repeated multiply.
    val cBarp7 = pow7(cBarp)
    val rc = 2.0 * sqrt(cBarp7 / (cBarp7 + POW_25_7))

    // 15. Lightness weighting Sl.
    val sl = 1.0 + (0.015 * square(lBar - 50.0)) / sqrt(20.0 + square(lBar - 50.0))

    // 16. Chroma weighting Sc.
    val sc = 1.0 + 0.045 * cBarp

    // 17. Hue weighting Sh.
    val sh = 1.0 + 0.015 * cBarp * t

    // 18. Rotation term Rt — negative.
    val rt = -sin(radians(2.0 * dTheta)) * rc

    // 19. Final ΔE00.
    val termL = dLp / sl
    val termC = dCp / sc
    val termH = dHp / sh
    return sqrt(termL * termL + termC * termC + termH * termH + rt * termC * termH)
}

/** Hue angle in degrees from `(b, a')`, normalised to `[0, 360)`, returning 0 when chroma is 0. */
private fun hueDegrees(b: Double, ap: Double, cp: Double): Double {
    if (cp == 0.0) return 0.0
    var h = degrees(atan2(b, ap))
    if (h < 0.0) h += 360.0
    return h
}

/** `x^7` by repeated multiplication: square, square, multiply, multiply. */
private fun pow7(x: Double): Double {
    val x2 = x * x
    val x4 = x2 * x2
    return x4 * x2 * x
}

private fun square(x: Double): Double = x * x

private fun radians(deg: Double): Double = deg * (Math.PI / 180.0)

private fun degrees(rad: Double): Double = rad * (180.0 / Math.PI)
