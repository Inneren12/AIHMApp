package app.aihandmade.core.image

data class RgbaImage(
    val width: Int,
    val height: Int,
    val pixels: IntArray
)

fun createEmptyImage(width: Int, height: Int, color: Int = 0xFF000000.toInt()): RgbaImage {
    return RgbaImage(width, height, IntArray(width * height) { color })
}
