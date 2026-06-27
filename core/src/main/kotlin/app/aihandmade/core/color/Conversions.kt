package app.aihandmade.core.color

/**
 * Colour types and conversions for the `core/color` module.
 *
 * The governing principle is that **the colour space is part of the type**: no bare
 * `FloatArray`/`IntArray` ever carries colour. [Srgb] is the 8-bit I/O boundary, [LinearRgb] is the
 * scene-linear conversion **hub** (every cross-space path goes through it), and [OkLab] is the
 * hot-loop space. [Lab] (CIE L\*a\*b\*, D65) is from commit 1.
 *
 * Conversions **do not clamp** and **do not round** — round-trips stay near loss-free. The only
 * clamp in the whole module is the final gamut clip inside [LinearRgb.toSrgb]. [cbrt] is
 * sign-preserving everywhere (the old engine clamped negatives to zero; this does not).
 *
 * Internal arithmetic may promote to [Double] for accuracy, but stored fields stay [Float].
 */

/**
 * An 8-bit **sRGB** colour at the I/O boundary, packed as `0xAARRGGBB`.
 *
 * This is the only place 8-bit integers carry colour. Everything downstream works in [LinearRgb],
 * [OkLab], or [Lab].
 */
@JvmInline
value class Srgb(val argb: Int) {
    /** Red channel, `0..255`. */
    val r: Int get() = (argb ushr 16) and 0xFF

    /** Green channel, `0..255`. */
    val g: Int get() = (argb ushr 8) and 0xFF

    /** Blue channel, `0..255`. */
    val b: Int get() = argb and 0xFF

    /** Alpha channel, `0..255`. */
    val a: Int get() = (argb ushr 24) and 0xFF

    companion object {
        /** Packs 8-bit channels (`0..255`) into an [Srgb], alpha defaulting to opaque. */
        fun of(r: Int, g: Int, b: Int, a: Int = 255): Srgb =
            Srgb(((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF))
    }
}

/**
 * Scene-linear sRGB (D65) — the conversion **hub**. Every cross-space conversion routes through this
 * type, so each matrix lives in exactly one place. Channels are unclamped and may fall outside
 * `[0, 1]` for out-of-gamut colours; clipping happens only in [toSrgb].
 */
data class LinearRgb(val r: Float, val g: Float, val b: Float)

/**
 * A colour in **OKLab** (Björn Ottosson) — the hot-loop space. Distances are cheap here
 * ([deltaSqOk]/[deltaOk]); it must never reach the CIE-Lab CIEDE2000 path, which the type wall
 * enforces.
 */
data class OkLab(val L: Float, val a: Float, val b: Float)

// --- sRGB transfer (per channel, 0..1) -------------------------------------------------------

private fun srgbToLinear(c: Double): Double =
    if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)

private fun linearToSrgb(c: Double): Double =
    if (c <= 0.0031308) c * 12.92 else 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055

/** Sign-preserving cube root. `Math.cbrt(-8) == -2`, unlike a clamp-to-zero shortcut. */
private fun cbrt(x: Double): Double = Math.cbrt(x)

/** D65 reference white for the XYZ/CIE-Lab path. */
private const val Xn = 0.95047
private const val Yn = 1.0
private const val Zn = 1.08883

/** CIE-Lab thresholds (the exact rational forms). */
private const val EPS = 216.0 / 24389.0
private const val KAPPA = 24389.0 / 27.0

/** CIE-Lab forward companding `f(t)`. */
private fun labF(t: Double): Double = if (t > EPS) cbrt(t) else (KAPPA * t + 16.0) / 116.0

/** Inverse of [labF]: `f^-1(t) = t^3` when `t^3 > eps`, else `(116t - 16) / kappa`. */
private fun labFInv(t: Double): Double {
    val t3 = t * t * t
    return if (t3 > EPS) t3 else (116.0 * t - 16.0) / KAPPA
}

// --- conversions ------------------------------------------------------------------------------

/** 8-bit sRGB → scene-linear sRGB. No clamping. */
fun Srgb.toLinear(): LinearRgb = LinearRgb(
    srgbToLinear(r / 255.0).toFloat(),
    srgbToLinear(g / 255.0).toFloat(),
    srgbToLinear(b / 255.0).toFloat(),
)

/**
 * Scene-linear sRGB → 8-bit sRGB. The **only** place clamping happens: out-of-gamut channels are
 * clipped to `0..255` after companding.
 */
fun LinearRgb.toSrgb(): Srgb {
    val ri = clip8(linearToSrgb(r.toDouble()))
    val gi = clip8(linearToSrgb(g.toDouble()))
    val bi = clip8(linearToSrgb(b.toDouble()))
    return Srgb.of(ri, gi, bi)
}

private fun clip8(c: Double): Int {
    val v = Math.round(c * 255.0).toInt()
    return if (v < 0) 0 else if (v > 255) 255 else v
}

/** linear sRGB (D65) -> OKLab. Defined ONCE; used per-pixel and in bulk. */
private inline fun <R> okLabKernel(r: Double, g: Double, b: Double, emit: (L: Float, a: Float, b: Float) -> R): R {
    val l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
    val m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
    val s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
    val l_ = cbrt(l); val m_ = cbrt(m); val s_ = cbrt(s)
    val L = 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_
    val A = 1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_
    val B = 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_
    return emit(L.toFloat(), A.toFloat(), B.toFloat())
}

/** Scene-linear sRGB (D65) → OKLab (Ottosson). */
fun LinearRgb.toOkLab(): OkLab = okLabKernel(r.toDouble(), g.toDouble(), b.toDouble()) { L, a, b -> OkLab(L, a, b) }

/** OKLab → scene-linear sRGB (exact inverse of [LinearRgb.toOkLab]). */
fun OkLab.toLinearRgb(): LinearRgb {
    val L = this.L.toDouble(); val A = this.a.toDouble(); val B = this.b.toDouble()

    val l_ = L + 0.3963377774 * A + 0.2158037573 * B
    val m_ = L - 0.1055613458 * A - 0.0638541728 * B
    val s_ = L - 0.0894841775 * A - 1.2914855480 * B

    val l = l_ * l_ * l_
    val m = m_ * m_ * m_
    val s = s_ * s_ * s_

    val r = +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    val g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    val b = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

    return LinearRgb(r.toFloat(), g.toFloat(), b.toFloat())
}

/** linear sRGB (D65) -> CIE-Lab. Defined ONCE; used per-pixel and in bulk. */
private inline fun <R> labKernel(r: Double, g: Double, b: Double, emit: (L: Float, a: Float, b: Float) -> R): R {
    val X = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b
    val Y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
    val Z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b
    val fx = labF(X / Xn); val fy = labF(Y / Yn); val fz = labF(Z / Zn)
    return emit((116.0 * fy - 16.0).toFloat(), (500.0 * (fx - fy)).toFloat(), (200.0 * (fy - fz)).toFloat())
}

/** Scene-linear sRGB (D65) → CIE-Lab (D65). */
fun LinearRgb.toLab(): Lab = labKernel(r.toDouble(), g.toDouble(), b.toDouble()) { L, a, b -> Lab(L, a, b) }

/** CIE-Lab (D65) → scene-linear sRGB (exact inverse of [LinearRgb.toLab]). */
fun Lab.toLinearRgb(): LinearRgb {
    val L = this.L.toDouble(); val a = this.a.toDouble(); val b = this.b.toDouble()

    val fy = (L + 16.0) / 116.0
    val fx = fy + a / 500.0
    val fz = fy - b / 200.0

    val X = labFInv(fx) * Xn
    val Y = labFInv(fy) * Yn
    val Z = labFInv(fz) * Zn

    val r = 3.2404542 * X - 1.5371385 * Y - 0.4985314 * Z
    val g = -0.9692660 * X + 1.8760108 * Y + 0.0415560 * Z
    val bb = 0.0556434 * X - 0.2040259 * Y + 1.0572252 * Z

    return LinearRgb(r.toFloat(), g.toFloat(), bb.toFloat())
}

/** Convenience: 8-bit sRGB → OKLab via the [LinearRgb] hub. */
fun Srgb.toOkLab(): OkLab = toLinear().toOkLab()

/** Convenience: 8-bit sRGB → CIE-Lab via the [LinearRgb] hub. */
fun Srgb.toLab(): Lab = toLinear().toLab()

// --- OKLab distance ---------------------------------------------------------------------------

/**
 * Squared Euclidean distance in OKLab — the cheap hot-loop metric, **no** `sqrt`. Takes only
 * [OkLab] so an OKLab value can never reach the CIE-Lab [deltaE2000] path.
 */
fun deltaSqOk(x: OkLab, y: OkLab): Float {
    val dL = x.L - y.L
    val da = x.a - y.a
    val db = x.b - y.b
    return dL * dL + da * da + db * db
}

/** Euclidean distance in OKLab, `sqrt(deltaSqOk)`. */
fun deltaOk(x: OkLab, y: OkLab): Float = Math.sqrt(deltaSqOk(x, y).toDouble()).toFloat()

// --- SoA planes -------------------------------------------------------------------------------

/** Validates dimensions and returns width*height without Int overflow. */
private fun checkedPlaneSize(width: Int, height: Int): Int {
    require(width >= 0 && height >= 0) { "width/height must be non-negative" }
    val expected = width.toLong() * height.toLong()
    require(expected <= Int.MAX_VALUE) { "width*height exceeds Int.MAX_VALUE" }
    return expected.toInt()
}

/**
 * OKLab colour as structure-of-arrays. Flat row-major layout: index = y * width + x.
 * Plain class (not data class) — FloatArray identity equality would be misleading.
 */
class OkLabPlanes(
    val L: FloatArray, val a: FloatArray, val b: FloatArray,
    val width: Int, val height: Int,
) {
    constructor(L: FloatArray, a: FloatArray, b: FloatArray) : this(L, a, b, L.size, 1)

    val size: Int = checkedPlaneSize(width, height)
    init { require(L.size == size && a.size == size && b.size == size) { "plane size != width*height" } }
}

/**
 * CIE-Lab colour as structure-of-arrays. Flat row-major layout: index = y * width + x.
 * Plain class (not data class) — FloatArray identity equality would be misleading.
 */
class LabPlanes(
    val L: FloatArray, val a: FloatArray, val b: FloatArray,
    val width: Int, val height: Int,
) {
    val size: Int = checkedPlaneSize(width, height)
    init { require(L.size == size && a.size == size && b.size == size) { "plane size != width*height" } }
}

/** Converts a packed-ARGB [IntArray] to [OkLabPlanes] (row-major). Allocates three [FloatArray]s only. */
fun IntArray.toOkLabPlanes(width: Int, height: Int): OkLabPlanes {
    val n = checkedPlaneSize(width, height)
    require(size == n) { "pixels.size != width*height" }
    val L = FloatArray(n); val A = FloatArray(n); val B = FloatArray(n)
    for (i in 0 until n) {
        val argb = this[i]
        // .toFloat().toDouble() mirrors LinearRgb's Float storage so values are bit-exact with the per-pixel path.
        val r = srgbToLinear(((argb ushr 16) and 0xFF) / 255.0).toFloat().toDouble()
        val g = srgbToLinear(((argb ushr 8) and 0xFF) / 255.0).toFloat().toDouble()
        val b = srgbToLinear((argb and 0xFF) / 255.0).toFloat().toDouble()
        okLabKernel(r, g, b) { ll, aa, bb -> L[i] = ll; A[i] = aa; B[i] = bb }
    }
    return OkLabPlanes(L, A, B, width, height)
}

/** Converts a packed-ARGB [IntArray] to [LabPlanes] (row-major). Allocates three [FloatArray]s only. */
fun IntArray.toLabPlanes(width: Int, height: Int): LabPlanes {
    val n = checkedPlaneSize(width, height)
    require(size == n) { "pixels.size != width*height" }
    val L = FloatArray(n); val A = FloatArray(n); val B = FloatArray(n)
    for (i in 0 until n) {
        val argb = this[i]
        val r = srgbToLinear(((argb ushr 16) and 0xFF) / 255.0).toFloat().toDouble()
        val g = srgbToLinear(((argb ushr 8) and 0xFF) / 255.0).toFloat().toDouble()
        val b = srgbToLinear((argb and 0xFF) / 255.0).toFloat().toDouble()
        labKernel(r, g, b) { ll, aa, bb -> L[i] = ll; A[i] = aa; B[i] = bb }
    }
    return LabPlanes(L, A, B, width, height)
}
