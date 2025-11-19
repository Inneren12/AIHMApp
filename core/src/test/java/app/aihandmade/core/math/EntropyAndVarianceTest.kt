package app.aihandmade.core.math

import org.junit.Assert.assertTrue
import org.junit.Test

class EntropyAndVarianceTest {
    @Test
    fun entropyHigherForCheckerboard() {
        val width = 8
        val height = 8
        val checker = IntArray(width * height) { idx -> if ((idx / width + idx % width) % 2 == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        val gray = FloatArray(width * height)
        ImageOps.toGrayscale(checker, gray)
        val quant = IntArray(width * height) { (gray[it] * 63f).toInt() }
        val entropy = FloatArray(width * height)
        ImageOps.entropyWindow(quant, width, height, 3, entropy)
        val avg = entropy.sum() / entropy.size

        val flat = IntArray(width * height) { 0xFF888888.toInt() }
        val grayFlat = FloatArray(width * height)
        ImageOps.toGrayscale(flat, grayFlat)
        val quantFlat = IntArray(width * height) { (grayFlat[it] * 63f).toInt() }
        val entropyFlat = FloatArray(width * height)
        ImageOps.entropyWindow(quantFlat, width, height, 3, entropyFlat)
        val avgFlat = entropyFlat.sum() / entropyFlat.size

        assertTrue(avg > avgFlat)
    }
}
