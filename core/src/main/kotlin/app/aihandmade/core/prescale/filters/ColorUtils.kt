package app.aihandmade.core.prescale.filters

import kotlin.math.max
import kotlin.math.min

object ColorUtils {
    fun a(color: Int): Int = (color ushr 24) and 0xFF
    fun r(color: Int): Int = (color ushr 16) and 0xFF
    fun g(color: Int): Int = (color ushr 8) and 0xFF
    fun b(color: Int): Int = color and 0xFF

    fun argb(a: Int, r: Int, g: Int, b: Int): Int {
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun clamp(value: Double, minValue: Double = 0.0, maxValue: Double = 255.0): Int {
        return when {
            value < minValue -> minValue.toInt()
            value > maxValue -> maxValue.toInt()
            else -> value.toInt()
        }
    }

    fun clampFloat(value: Double, minValue: Double = 0.0, maxValue: Double = 1.0): Double {
        return max(minValue, min(maxValue, value))
    }

    fun yLinear(r: Int, g: Int, b: Int): Double {
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun toYCbCr(r: Int, g: Int, b: Int): Triple<Double, Double, Double> {
        val y = yLinear(r, g, b)
        val cb = (b - y) * 0.5389 // approximate factor to map [-127..127]
        val cr = (r - y) * 0.6350
        return Triple(y, cb, cr)
    }

    fun fromYCbCr(y: Double, cb: Double, cr: Double): Triple<Int, Int, Int> {
        val r = y + cr / 0.6350
        val b = y + cb / 0.5389
        val g = y - 0.2126 / 0.7152 * (r - y) - 0.0722 / 0.7152 * (b - y)
        return Triple(
            clamp(r),
            clamp(g),
            clamp(b)
        )
    }

    fun smoothstep(edge0: Double, edge1: Double, x: Double): Double {
        val t = clampFloat((x - edge0) / (edge1 - edge0))
        return t * t * (3 - 2 * t)
    }
}
