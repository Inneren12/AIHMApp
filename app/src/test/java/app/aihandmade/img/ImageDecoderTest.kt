package app.aihandmade.img

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class ImageDecoderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `decode downscales to requested max side`() {
        val source = Bitmap.createBitmap(8000, 4000, Bitmap.Config.ARGB_8888)
        val file = writeBitmapToTempFile(source, "downscale.jpg")

        val decoded = ImageDecoder.decode(context, Uri.fromFile(file), 4096)

        assertEquals(Bitmap.Config.ARGB_8888, decoded.config)
        assertTrue(maxOf(decoded.width, decoded.height) <= 4096)
    }

    @Test
    fun `decode respects exif orientation`() {
        val source = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888)
        val file = writeBitmapToTempFile(source, "orientation.jpg")

        ExifInterface(file).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val decoded = ImageDecoder.decode(context, Uri.fromFile(file), 4096)

        assertEquals(600, decoded.width)
        assertEquals(1200, decoded.height)
    }

    private fun writeBitmapToTempFile(bitmap: Bitmap, name: String): File {
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        return file
    }
}
