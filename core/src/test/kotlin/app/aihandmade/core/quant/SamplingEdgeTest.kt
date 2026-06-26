package app.aihandmade.core.quant

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SamplingEdgeTest {

    @Test
    fun zeroDimensionsThrow() {
        assertThrows(IllegalArgumentException::class.java) { samplePixels(IntArray(0), 0, 0, 1) }
        assertThrows(IllegalArgumentException::class.java) { samplePixels(IntArray(0), 0, 1, 1) }
        assertThrows(IllegalArgumentException::class.java) { samplePixels(IntArray(0), 1, 0, 1) }
    }

    @Test
    fun pixelCountMismatchThrows() {
        // toOkLabPlanes enforces pixels.size == width*height
        assertThrows(IllegalArgumentException::class.java) { samplePixels(IntArray(5), 2, 3, 4, 1L) }
    }

    @Test
    fun sampleSetRejectsMismatchedArraySizes() {
        assertThrows(IllegalArgumentException::class.java) {
            SampleSet(
                index = IntArray(2) { it },
                L = FloatArray(3),   // wrong size
                a = FloatArray(2),
                b = FloatArray(2),
                weight = FloatArray(2),
                sourceWidth = 4, sourceHeight = 4,
            )
        }
    }

    @Test
    fun sampleSetRejectsUnsortedIndices() {
        assertThrows(IllegalArgumentException::class.java) {
            SampleSet(
                index = intArrayOf(2, 0),   // not ascending
                L = FloatArray(2),
                a = FloatArray(2),
                b = FloatArray(2),
                weight = FloatArray(2),
                sourceWidth = 4, sourceHeight = 4,
            )
        }
    }

    @Test
    fun sampleSetRejectsDuplicateIndices() {
        assertThrows(IllegalArgumentException::class.java) {
            SampleSet(
                index = intArrayOf(1, 1),   // duplicate
                L = FloatArray(2),
                a = FloatArray(2),
                b = FloatArray(2),
                weight = FloatArray(2),
                sourceWidth = 4, sourceHeight = 4,
            )
        }
    }

    @Test
    fun sampleSetRejectsZeroSourceDimensions() {
        assertThrows(IllegalArgumentException::class.java) {
            SampleSet(
                index = IntArray(0),
                L = FloatArray(0), a = FloatArray(0), b = FloatArray(0), weight = FloatArray(0),
                sourceWidth = 0, sourceHeight = 1,
            )
        }
    }
}
