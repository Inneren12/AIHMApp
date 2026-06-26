package app.aihandmade.core.color

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Validates the overflow-safe dimension checks added to OkLabPlanes/LabPlanes and the bulk converters. */
class ColorPlanesDimensionTest {

    @Test
    fun negativeWidthThrows() {
        assertThrows(IllegalArgumentException::class.java) { intArrayOf().toOkLabPlanes(-1, 0) }
        assertThrows(IllegalArgumentException::class.java) { intArrayOf().toLabPlanes(-1, 0) }
    }

    @Test
    fun negativeHeightThrows() {
        assertThrows(IllegalArgumentException::class.java) { intArrayOf().toOkLabPlanes(0, -1) }
        assertThrows(IllegalArgumentException::class.java) { intArrayOf().toLabPlanes(0, -1) }
    }

    @Test
    fun overflowDimensionsThrowBeforeConversion() {
        // Int.MAX_VALUE * 2 overflows Int — must throw on dimension check, not inside the loop.
        val empty = intArrayOf()
        assertThrows(IllegalArgumentException::class.java) { empty.toOkLabPlanes(Int.MAX_VALUE, 2) }
        assertThrows(IllegalArgumentException::class.java) { empty.toLabPlanes(Int.MAX_VALUE, 2) }
    }

    @Test
    fun planeConstructorRejectsNegativeDimensions() {
        assertThrows(IllegalArgumentException::class.java) {
            OkLabPlanes(FloatArray(0), FloatArray(0), FloatArray(0), -1, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LabPlanes(FloatArray(0), FloatArray(0), FloatArray(0), 1, -1)
        }
    }

    @Test
    fun planeConstructorRejectsOverflowDimensions() {
        assertThrows(IllegalArgumentException::class.java) {
            OkLabPlanes(FloatArray(0), FloatArray(0), FloatArray(0), Int.MAX_VALUE, 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LabPlanes(FloatArray(0), FloatArray(0), FloatArray(0), Int.MAX_VALUE, 2)
        }
    }

    @Test
    fun mismatchedDimensionsStillThrow() {
        val pixels = intArrayOf(
            Srgb.of(255, 0, 0).argb,
            Srgb.of(0, 255, 0).argb,
            Srgb.of(0, 0, 255).argb,
            Srgb.of(255, 255, 255).argb,
            Srgb.of(0, 0, 0).argb,
            Srgb.of(128, 128, 128).argb,
        )
        assertThrows(IllegalArgumentException::class.java) { pixels.toOkLabPlanes(2, 2) } // 4 != 6
        assertThrows(IllegalArgumentException::class.java) { pixels.toLabPlanes(5, 1) }   // 5 != 6
    }
}
