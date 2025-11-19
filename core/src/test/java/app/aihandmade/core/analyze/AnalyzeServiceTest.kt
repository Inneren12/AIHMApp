package app.aihandmade.core.analyze

import app.aihandmade.core.testfixtures.ImageGenerators
import app.aihandmade.core.testfixtures.assertClose
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeServiceTest {
    private val service = DefaultAnalyzeService()

    @Test
    fun solidColorMetrics() {
        val img = ImageGenerators.solid(32, 32, 0xFF00FF00.toInt())
        val (res, masks) = service.analyzeAndMasks(img, 32, 32)
        assertClose(res.edgeDensity, 0.0, 1e-3)
        assertClose(res.uniqueColorsQ.toDouble(), 1.0, 0.0)
        assertTrue(res.gradientSmoothness > 0.9)
        assertClose(res.pixelationScore, 0.0, 1e-3)
        assertTrue(res.entropyScore < 0.05)
        assertTrue(masks.edge.all { it < 1e-3f })
        assertTrue(masks.flat.all { it > 0.9f })
        assertTrue(masks.texture.all { it < 0.1f })
    }

    @Test
    fun horizontalGradientSmooth() {
        val img = ImageGenerators.horizontalGradient(64, 32)
        val (res, _) = service.analyzeAndMasks(img, 64, 32)
        assertTrue(res.edgeDensity < 0.2)
        assertTrue(res.gradientSmoothness > 0.9)
        assertTrue(res.pixelationScore < 0.1)
    }

    @Test
    fun pixelatedGradientHasHighPixelation() {
        val img = ImageGenerators.pixelatedGradient(64, 64)
        val (res, masks) = service.analyzeAndMasks(img, 64, 64)
        assertTrue(res.pixelationScore > 0.6)
        assertTrue(masks.flat.average() > 0.4f)
    }

    @Test
    fun checkerboardIsTextured() {
        val img = ImageGenerators.checkerboard(64, 64)
        val (res, masks) = service.analyzeAndMasks(img, 64, 64)
        assertTrue(res.edgeDensity > 0.6)
        assertTrue(res.entropyScore > 0.7)
        assertTrue(masks.texture.average() > 0.6f)
    }

    @Test
    fun noiseIsHighEntropy() {
        val img = ImageGenerators.noise(64, 64)
        val (res, masks) = service.analyzeAndMasks(img, 64, 64)
        assertTrue(res.entropyScore > 0.6)
        assertTrue(masks.flat.average() < 0.5f)
    }
}

private fun FloatArray.average(): Float = if (isEmpty()) 0f else sum() / size
