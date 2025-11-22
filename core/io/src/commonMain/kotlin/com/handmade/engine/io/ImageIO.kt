package com.handmade.engine.io

import com.handmade.engine.domain.SourceImage
import com.handmade.engine.domain.generateImageRef

expect class NativeImage() {
    val width: Int
    val height: Int
}

expect fun load(path: String): NativeImage
expect fun exifRotate(img: NativeImage): NativeImage
expect fun toSRGB(img: NativeImage): NativeImage
expect fun makePreview(img: NativeImage, maxSide: Int = 1024): NativeImage

object ImageIO {
    fun toSourceImage(img: NativeImage, preview: NativeImage): SourceImage {
        val originalRef = generateImageRef("orig")
        val previewRef = generateImageRef("preview")
        return SourceImage(
            original = originalRef,
            preview = previewRef,
            width = 0,
            height = 0,
        )
    }
}
