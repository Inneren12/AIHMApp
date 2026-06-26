package app.aihandmade.core.metrics

import app.aihandmade.core.color.LabPlanes
import app.aihandmade.core.color.Srgb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * CONTRACT TEST for metrics-3: banding / posterization score on the CIE-Lab L* channel.
 *
 * Implement so this compiles and passes UNCHANGED. You may add tests, but do not edit or weaken
 * anything here.
 *
 * The score is num/denom of two INTEGER adjacency counts, so it is reproducible to the bit. The
 * dithering test encodes the metric's whole purpose: dithering breaks flat plateaus and therefore
 * must NOT be scored as banding.
 */
class BandingMeterTest {

    private val w = 16
    private val h = 16

    // Smooth diagonal grey ramp via INTEGER division (floor) so it matches the reference calc exactly.
    private fun refGray(x: Int, y: Int): Int = (x + y) * 255 / 30
    private fun grey(g: Int): Int = Srgb.of(g, g, g).argb

    private val refPixels = IntArray(w * h) { i -> grey(refGray(i % w, i / w)) }

    // Harsh 4-level posterization -> staircase plateaus (banding).
    private val posterizedPixels = IntArray(w * h) { i ->
        val g = refGray(i % w, i / w); grey((g / 64) * 64)
    }

    // Reference + a +/-1 checkerboard dither -> breaks the plateaus (must score LOW).
    private val ditheredPixels = IntArray(w * h) { i ->
        val x = i % w; val y = i / w
        grey((refGray(x, y) + if ((x + y) % 2 == 0) 1 else -1).coerceIn(0, 255))
    }

    @Test
    fun posterizedRampMatchesReference() {
        // 418 of the 480 reference-varying adjacencies are flattened by the 4-level posterization.
        assertEquals(418.0 / 480.0, bandingScore(refPixels, posterizedPixels, w, h), 1e-9)
    }

    @Test
    fun identicalImagesAreZero() {
        assertEquals(0.0, bandingScore(refPixels, refPixels, w, h), 1e-12)
    }

    @Test
    fun ditheringScoresLowerThanPosterization() {
        // The whole point of the metric: dithering breaks flat runs, so it must not be penalised.
        val posterized = bandingScore(refPixels, posterizedPixels, w, h)
        val dithered = bandingScore(refPixels, ditheredPixels, w, h)
        assertTrue(dithered < posterized, "dithering must score lower than posterization")
    }

    @Test
    fun scoreIsInUnitInterval() {
        val s = bandingScore(refPixels, posterizedPixels, w, h)
        assertTrue(s in 0.0..1.0, "banding score must be within [0, 1]")
    }

    @Test
    fun mismatchedDimensionsThrow() {
        // pixel count not equal to width*height
        assertThrows(IllegalArgumentException::class.java) { bandingScore(refPixels, posterizedPixels, 7, 7) }
        // reference and candidate planes of different sizes
        val a = LabPlanes(FloatArray(2), FloatArray(2), FloatArray(2), 2, 1)
        val b = LabPlanes(FloatArray(1), FloatArray(1), FloatArray(1), 1, 1)
        assertThrows(IllegalArgumentException::class.java) { bandingScore(a, b) }
    }
}
