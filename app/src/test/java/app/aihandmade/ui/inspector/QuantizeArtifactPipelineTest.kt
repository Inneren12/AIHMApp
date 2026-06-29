package app.aihandmade.ui.inspector

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class QuantizeArtifactPipelineTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var tempFile: File? = null

    @After
    fun tearDown() {
        tempFile?.delete()
    }

    @Test
    fun quantizeArtifactToPattern_returnsConsistentPatternDebug() {
        // 64x48 gradient bitmap — varied enough for analyse to classify the scene.
        val w = 64; val h = 48
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val r = (x * 255 / w) and 0xFF
                val g = (y * 255 / h) and 0xFF
                val b = ((x + y) * 255 / (w + h)) and 0xFF
                bmp.setPixel(x, y, (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }
        val file = File(context.cacheDir, "pipeline-test.jpg")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bmp.recycle()
        tempFile = file

        val result = quantizeArtifactToPattern(file.absolutePath)

        assertNotNull("result must not be null", result)
        checkNotNull(result)
        assertTrue("sceneType must be non-empty", result.sceneType.isNotEmpty())
        assertTrue("pipeline must be non-empty", result.pipeline.isNotEmpty())
        assertTrue("must have at least one swatch", result.swatches.isNotEmpty())
        assertEquals("colourCount must match swatches size", result.colourCount, result.swatches.size)
        val totalCounts = result.swatches.sumOf { it.count }
        assertEquals(
            "stitch counts must sum to widthStitches × heightStitches",
            result.widthStitches * result.heightStitches,
            totalCounts,
        )
        assertTrue("colourCount must be in reasonable range", result.colourCount in 1..32)
    }
}
