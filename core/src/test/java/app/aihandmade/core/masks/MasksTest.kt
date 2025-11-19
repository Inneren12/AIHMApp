package app.aihandmade.core.masks

import app.aihandmade.core.analyze.AnalyzeParams
import app.aihandmade.core.analyze.DefaultAnalyzeService
import org.junit.Assert.assertTrue
import org.junit.Test

class MasksTest {
    private val service = DefaultAnalyzeService()

    @Test
    fun thinDiagonalLineStaysThinAfterNms() {
        val size = 32
        val img = IntArray(size * size) { 0xFF000000.toInt() }
        for (i in 2 until size - 2) {
            img[i * size + i] = 0xFFFFFFFF.toInt()
        }
        val (_, masks) = service.analyzeAndMasks(img, size, size, AnalyzeParams(previewMax = size))
        val strong = masks.edge.count { it > 0.5f }
        assertTrue(strong in 20..60) // roughly one-pixel thickness along diagonal
        assertTrue(masks.edge.all { it in 0f..1f })
    }
}
