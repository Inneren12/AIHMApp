package app.aihandmade.ui.inspector

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewDimsTest {

    @Test fun landscapeDownscaledToLongSideOnWidth() {
        val (w, h) = previewDims(4000, 3000, 512)
        assertEquals(512, w); assertEquals(384, h) // 512*3000/4000
    }

    @Test fun portraitDownscaledToLongSideOnHeight() {
        val (w, h) = previewDims(3000, 4000, 512)
        assertEquals(384, w); assertEquals(512, h)
    }

    @Test fun squareDownscaled() {
        val (w, h) = previewDims(1000, 1000, 512)
        assertEquals(512, w); assertEquals(512, h)
    }

    @Test fun smallSourceIsNotUpscaled() {
        val (w, h) = previewDims(300, 200, 512)
        assertEquals(300, w); assertEquals(200, h)
    }

    @Test fun extremeAspectClampsShortSideToOne() {
        val (w, h) = previewDims(5120, 10, 512)
        assertEquals(512, w); assertEquals(1, h) // 512*10/5120 = 1
    }

    // Inspector prep-dim bounding: prepLongSide = max(512, targetLongSide * 3)
    // For target 180x180 → prepLongSide = max(512, 540) = 540

    @Test fun prepBoundsLargeSource() {
        // 4096×4096 source, target 180×180 → prepLongSide=540; must NOT return 4096
        val targetLong = 180
        val prepLongSide = maxOf(512, targetLong * 3) // 540
        val (w, h) = previewDims(4096, 4096, prepLongSide)
        assertEquals(540, w); assertEquals(540, h)
    }

    @Test fun prepDoesNotUpscaleSmallSource() {
        // 300×200 source is already below prepLongSide=540; must be returned as-is
        val targetLong = 180
        val prepLongSide = maxOf(512, targetLong * 3) // 540
        val (w, h) = previewDims(300, 200, prepLongSide)
        assertEquals(300, w); assertEquals(200, h)
    }
}
