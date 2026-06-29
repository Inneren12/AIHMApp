package app.aihandmade.ui.inspector

import kotlin.test.assertEquals
import org.junit.Test

class StitchDimsTest {

    @Test
    fun landscape_longSideConstrainsWidth() {
        val (w, h) = stitchDims(1920, 1080, 140)
        assertEquals(140, w)
        assertEquals(78, h) // 140 * 1080 / 1920 = 78 (integer division)
    }

    @Test
    fun portrait_longSideConstrainsHeight() {
        val (w, h) = stitchDims(1080, 1920, 140)
        assertEquals(78, w) // 140 * 1080 / 1920 = 78
        assertEquals(140, h)
    }

    @Test
    fun square_bothDimensionsEqualLongSide() {
        val (w, h) = stitchDims(100, 100, 140)
        assertEquals(140, w)
        assertEquals(140, h)
    }

    @Test
    fun tinyShortSideHeight_clampsToOne() {
        // 140 * 1 / 1000 = 0 → clamped to 1
        val (w, h) = stitchDims(1000, 1, 140)
        assertEquals(140, w)
        assertEquals(1, h)
    }

    @Test
    fun tinyShortSideWidth_clampsToOne() {
        // 140 * 1 / 1000 = 0 → clamped to 1
        val (w, h) = stitchDims(1, 1000, 140)
        assertEquals(1, w)
        assertEquals(140, h)
    }

    @Test
    fun exactlySquareBoundary_widthBranchTaken() {
        // w == h triggers the w >= h branch, both map to longSide
        val (w, h) = stitchDims(50, 50, 60)
        assertEquals(60, w)
        assertEquals(60, h)
    }
}
