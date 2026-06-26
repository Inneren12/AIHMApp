package app.aihandmade.core.color

/**
 * A colour in **CIE L\*a\*b\* (D65)**.
 *
 * The colour space is part of the type **by design**: there is no bare `FloatArray`/`IntArray`
 * that silently carries colour, so a value in one space can never be fed into a function that
 * expects another. This specifically prevents the historical defect where **OKLab** coordinates
 * were passed into a **CIE-Lab** CIEDE2000 formula (whose constants are only valid for CIE-Lab) —
 * with these types that mistake is a compile error, not a runtime miscalculation.
 *
 * Ranges: [L] is in `[0, 100]`; [a] and [b] are roughly in `[-128, 127]`.
 *
 * Fields are stored as [Float] so dense structure-of-arrays storage stays compact in later commits;
 * the [deltaE2000] computation promotes to [Double] internally for accuracy.
 */
data class Lab(val L: Float, val a: Float, val b: Float)
