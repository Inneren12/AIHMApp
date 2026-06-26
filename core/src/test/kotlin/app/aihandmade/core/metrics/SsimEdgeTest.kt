package app.aihandmade.core.metrics

import app.aihandmade.core.color.LabPlanes
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SsimEdgeTest {

    @Test
    fun emptyImagesThrowFromIntArrayOverload() {
        assertThrows(IllegalArgumentException::class.java) {
            ssim(intArrayOf(), intArrayOf(), 0, 0)
        }
    }

    @Test
    fun emptyLabPlanesThrow() {
        val empty = LabPlanes(FloatArray(0), FloatArray(0), FloatArray(0), 0, 0)
        assertThrows(IllegalArgumentException::class.java) {
            ssim(empty, empty)
        }
    }
}
