package app.aihandmade.core.quant

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BinSelectionEdgeTest {

    private val samples = SampleSet(
        index = intArrayOf(0, 1, 2, 3, 4, 5),
        L = floatArrayOf(0.50f, 0.50f, 0.20f, 0.80f, 0.30f, 0.30f),
        a = floatArrayOf(0.00f, 0.00f, 0.00f, 0.00f, 0.10f, 0.00f),
        b = floatArrayOf(0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f),
        weight = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f),
        sourceWidth = 6,
        sourceHeight = 1,
    )

    @Test
    fun binOfRejectsNonFiniteCoordinates() {
        assertThrows(IllegalArgumentException::class.java) { binOf(Float.NaN, 0f, 0f) }
        assertThrows(IllegalArgumentException::class.java) { binOf(0f, Float.POSITIVE_INFINITY, 0f) }
        assertThrows(IllegalArgumentException::class.java) { binOf(0f, 0f, Float.NEGATIVE_INFINITY) }
    }

    @Test
    fun selectImportantBinRejectsInvalidImportanceValues() {
        assertThrows(IllegalArgumentException::class.java) {
            selectImportantBin(samples, doubleArrayOf(Double.NaN, 1.0, 1.0, 1.0, 1.0, 1.0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            selectImportantBin(samples, doubleArrayOf(Double.POSITIVE_INFINITY, 1.0, 1.0, 1.0, 1.0, 1.0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            selectImportantBin(samples, doubleArrayOf(-1.0, 1.0, 1.0, 1.0, 1.0, 1.0))
        }
    }
}
