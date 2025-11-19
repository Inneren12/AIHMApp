package app.aihandmade.core.decision.util

import kotlin.math.abs
import kotlin.math.roundToInt

object MathUtil {
    fun roundToMultiple(value: Int, multiple: Int): Int {
        require(multiple >= 1) { "Multiple must be >= 1" }
        if (multiple == 1) return value
        val remainder = abs(value % multiple)
        val half = multiple / 2.0
        return if (remainder >= half) {
            if (value >= 0) value + (multiple - remainder) else value - (multiple - remainder)
        } else {
            if (value >= 0) value - remainder else value + remainder
        }
    }

    fun roundToNearestInt(value: Double): Int = value.roundToInt()

    fun clampEach(width: Int, height: Int, range: IntRange): Pair<Int, Int> =
        clamp(value = width, range = range) to clamp(value = height, range = range)

    fun clamp(value: Int, range: IntRange): Int = value.coerceIn(range.first, range.last)
}
