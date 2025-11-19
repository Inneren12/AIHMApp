package app.aihandmade.core.testfixtures

import kotlin.random.Random

/** Utilities producing small synthetic RGBA scenes for tests. */
object ImageGenerators {
    fun solid(width: Int, height: Int, color: Int): IntArray = IntArray(width * height) { color }

    fun horizontalGradient(width: Int, height: Int, from: Int = 0xFF000000.toInt(), to: Int = 0xFFFFFFFF.toInt()): IntArray {
        val arr = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val t = x.toFloat() / (width - 1).coerceAtLeast(1)
                val r = lerp((from shr 16) and 0xFF, (to shr 16) and 0xFF, t)
                val g = lerp((from shr 8) and 0xFF, (to shr 8) and 0xFF, t)
                val b = lerp(from and 0xFF, to and 0xFF, t)
                arr[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return arr
    }

    fun pixelatedGradient(width: Int, height: Int, smallW: Int = 8, smallH: Int = 8): IntArray {
        val small = horizontalGradient(smallW, smallH)
        val arr = IntArray(width * height)
        for (y in 0 until height) {
            val sy = (y * smallH) / height
            for (x in 0 until width) {
                val sx = (x * smallW) / width
                arr[y * width + x] = small[sy * smallW + sx]
            }
        }
        return arr
    }

    fun checkerboard(width: Int, height: Int, cell: Int = 8, c1: Int = 0xFFFFFFFF.toInt(), c2: Int = 0xFF000000.toInt()): IntArray {
        val arr = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val useFirst = ((x / cell) + (y / cell)) % 2 == 0
                arr[y * width + x] = if (useFirst) c1 else c2
            }
        }
        return arr
    }

    fun noise(width: Int, height: Int, seed: Int = 42): IntArray {
        val rnd = Random(seed)
        val arr = IntArray(width * height)
        for (i in arr.indices) {
            val v = rnd.nextInt(256)
            arr[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        return arr
    }

    private fun lerp(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).toInt()
}
