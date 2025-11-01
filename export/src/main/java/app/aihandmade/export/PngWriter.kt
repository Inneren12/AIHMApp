package app.aihandmade.export

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Minimal PNG encoder supporting ARGB_8888 bitmaps.
 */
class PngWriter {
    fun write(bitmap: Bitmap): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(PNG_SIGNATURE)

        val ihdr = createIHDR(bitmap.width, bitmap.height)
        writeChunk(output, CHUNK_IHDR, ihdr)

        val idat = createIDAT(bitmap)
        writeChunk(output, CHUNK_IDAT, idat)

        writeChunk(output, CHUNK_IEND, ByteArray(0))

        return output.toByteArray()
    }

    private fun createIHDR(width: Int, height: Int): ByteArray {
        val buffer = ByteBuffer.allocate(13)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(width)
        buffer.putInt(height)
        buffer.put(8) // Bit depth
        buffer.put(6) // Color type: RGBA
        buffer.put(0) // Compression method
        buffer.put(0) // Filter method
        buffer.put(0) // Interlace method
        return buffer.array()
    }

    private fun createIDAT(bitmap: Bitmap): ByteArray {
        val rowLength = bitmap.width * 4 + 1
        val raw = ByteArray(rowLength * bitmap.height)
        var offset = 0
        var pixelIndex = 0
        for (y in 0 until bitmap.height) {
            raw[offset++] = 0 // Filter type: None
            for (x in 0 until bitmap.width) {
                val argb = bitmap.pixels[pixelIndex++]
                raw[offset++] = ((argb shr 16) and 0xFF).toByte() // R
                raw[offset++] = ((argb shr 8) and 0xFF).toByte() // G
                raw[offset++] = (argb and 0xFF).toByte() // B
                raw[offset++] = ((argb ushr 24) and 0xFF).toByte() // A
            }
        }

        val deflater = Deflater(Deflater.BEST_SPEED)
        try {
            deflater.setInput(raw)
            deflater.finish()
            val buffer = ByteArray(raw.size)
            val compressed = ByteArrayOutputStream()
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                compressed.write(buffer, 0, count)
            }
            return compressed.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun writeChunk(output: ByteArrayOutputStream, type: ByteArray, data: ByteArray) {
        val lengthBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        lengthBuffer.putInt(data.size)
        output.write(lengthBuffer.array())
        output.write(type)
        output.write(data)

        val crc = CRC32()
        crc.update(type)
        crc.update(data)
        val crcBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        crcBuffer.putInt(crc.value.toInt())
        output.write(crcBuffer.array())
    }

    companion object {
        private val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        private val CHUNK_IHDR = "IHDR".toByteArray(Charsets.US_ASCII)
        private val CHUNK_IDAT = "IDAT".toByteArray(Charsets.US_ASCII)
        private val CHUNK_IEND = "IEND".toByteArray(Charsets.US_ASCII)
    }
}
