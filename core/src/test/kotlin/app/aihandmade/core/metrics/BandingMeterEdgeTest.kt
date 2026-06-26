package app.aihandmade.core.metrics

import app.aihandmade.core.color.LabPlanes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BandingMeterEdgeTest {

    @Test
    fun emptyImagesThrowFromIntArrayOverload() {
        assertThrows(IllegalArgumentException::class.java) {
            bandingScore(intArrayOf(), intArrayOf(), 0, 0)
        }
    }

    @Test
    fun emptyLabPlanesThrow() {
        val empty = LabPlanes(FloatArray(0), FloatArray(0), FloatArray(0), 0, 0)
        assertThrows(IllegalArgumentException::class.java) {
            bandingScore(empty, empty)
        }
    }

    @Test
    fun flatReferenceReturnsZeroBecauseThereAreNoReferenceVaryingAdjacencies() {
        val ref = LabPlanes(
            floatArrayOf(50f, 50f, 50f, 50f),
            FloatArray(4),
            FloatArray(4),
            2,
            2,
        )
        val cand = LabPlanes(
            floatArrayOf(0f, 100f, 0f, 100f),
            FloatArray(4),
            FloatArray(4),
            2,
            2,
        )
        assertEquals(0.0, bandingScore(ref, cand), 1e-12)
    }
}
