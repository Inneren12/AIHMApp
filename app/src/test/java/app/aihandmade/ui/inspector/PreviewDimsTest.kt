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
}
