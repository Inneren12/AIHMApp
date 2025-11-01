package app.aihandmade.img

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

object ImageDecoder {
    @JvmStatic
    @Throws(IOException::class)
    fun decode(context: Context, uri: Uri, maxSidePx: Int = 4096): Bitmap {
        require(maxSidePx > 0) { "maxSidePx must be greater than 0 (was $maxSidePx)" }

        val resolver = context.contentResolver
        val bounds = readImageBounds(resolver, uri)
        val sampleSize = calculateInSampleSize(bounds.first, bounds.second, maxSidePx)

        val decodedBitmap = openStream(resolver, uri).use { stream ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = sampleSize
            }
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IOException("Failed to decode bitmap for URI: $uri")

        val oriented = applyExifOrientation(resolver, uri, decodedBitmap)
        return ensureMaxSide(oriented, maxSidePx)
    }

    private fun readImageBounds(resolver: ContentResolver, uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(resolver, uri).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) {
            throw IOException("Unable to determine image bounds for URI: $uri")
        }
        return width to height
    }

    private fun openStream(resolver: ContentResolver, uri: Uri) =
        resolver.openInputStream(uri) ?: throw IOException("Unable to open input stream for URI: $uri")

    private fun calculateInSampleSize(width: Int, height: Int, maxSidePx: Int): Int {
        var inSampleSize = 1
        val longestSide = max(width, height)
        if (longestSide <= maxSidePx) {
            return inSampleSize
        }
        while (longestSide / inSampleSize > maxSidePx) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun applyExifOrientation(resolver: ContentResolver, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = resolveExifOrientation(resolver, uri)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (transformed != bitmap) {
            bitmap.recycle()
        }
        return transformed
    }

    private fun resolveExifOrientation(resolver: ContentResolver, uri: Uri): Int {
        return try {
            openStream(resolver, uri).use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (ignored: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun ensureMaxSide(bitmap: Bitmap, maxSidePx: Int): Bitmap {
        val longestSide = max(bitmap.width, bitmap.height)
        if (longestSide <= maxSidePx) {
            return bitmap
        }
        val scale = maxSidePx.toFloat() / longestSide
        val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }
}
