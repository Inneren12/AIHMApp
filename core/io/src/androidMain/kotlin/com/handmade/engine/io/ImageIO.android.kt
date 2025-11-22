package com.handmade.engine.io

import android.graphics.Bitmap
import android.graphics.BitmapFactory

actual class NativeImage actual constructor() {
    lateinit var bitmap: Bitmap
    actual val width: Int
        get() = bitmap.width
    actual val height: Int
        get() = bitmap.height
}

actual fun load(path: String): NativeImage {
    val bitmap = BitmapFactory.decodeFile(path)
        ?: throw IllegalArgumentException("Unable to decode image at $path")
    return NativeImage().apply { this.bitmap = bitmap }
}

actual fun exifRotate(img: NativeImage): NativeImage {
    // TODO: apply EXIF orientation corrections
    return img
}

actual fun toSRGB(img: NativeImage): NativeImage {
    // TODO: convert color space to sRGB if needed
    return img
}

actual fun makePreview(img: NativeImage, maxSide: Int): NativeImage {
    val bitmap = img.bitmap
    val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
    val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    return NativeImage().apply { this.bitmap = scaled }
}
