package app.aihandmade.export

import java.nio.file.Path

/**
 * Stores artifacts produced during a pipeline run.
 */
interface ArtifactStore {
    /**
     * Persists the provided [bitmap] as a PNG artifact.
     */
    fun savePng(step: String, name: String, bitmap: Bitmap, meta: ArtifactMetadata): ArtifactRef
}

/**
 * Metadata describing a stored artifact.
 */
data class ArtifactMetadata(
    val paramsHash: String,
    val meta: Map<String, Any?> = emptyMap()
)

/**
 * Reference to a stored artifact on disk.
 */
data class ArtifactRef(
    val path: Path,
    val kind: String = "preview"
)

/**
 * In-memory bitmap using ARGB_8888 pixels.
 */
class Bitmap(
    val width: Int,
    val height: Int,
    val pixels: IntArray
) {
    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        require(pixels.size == width * height) {
            "Pixel array length ${'$'}{pixels.size} does not match dimensions ${'$'}width x ${'$'}height"
        }
    }

    operator fun get(x: Int, y: Int): Int {
        require(x in 0 until width) { "x=${'$'}x is out of bounds (width=${'$'}width)" }
        require(y in 0 until height) { "y=${'$'}y is out of bounds (height=${'$'}height)" }
        return pixels[y * width + x]
    }

    companion object {
        /**
         * Creates a [Bitmap] by filling pixels using the [initializer] lambda.
         */
        fun from(width: Int, height: Int, initializer: (x: Int, y: Int) -> Int): Bitmap {
            val pixels = IntArray(width * height)
            var index = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[index++] = initializer(x, y)
                }
            }
            return Bitmap(width, height, pixels)
        }
    }
}
