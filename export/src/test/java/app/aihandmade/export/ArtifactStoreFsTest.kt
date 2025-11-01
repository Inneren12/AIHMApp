package app.aihandmade.export

import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ArtifactStoreFsTest {
    private val json = Json { ignoreUnknownKeys = false }

    @Test
    fun savePngStoresImageAndMetadata() {
        val tempDir = Files.createTempDirectory("artifact-store-test")
        try {
            val clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)
            val store = ArtifactStoreFs(
                runId = "run-1",
                rootDir = tempDir,
                clock = clock
            )

            val bitmap = Bitmap.from(16, 16) { x, y ->
                val alpha = 0xFF shl 24
                val red = (x * 16) shl 16
                val green = (y * 16) shl 8
                val blue = (x + y) * 8
                alpha or red or green or blue
            }

            val metadata = ArtifactMetadata(
                paramsHash = "abcdef",
                meta = mapOf(
                    "label" to "test",
                    "score" to 0.95,
                    "flags" to listOf(true, false),
                    "nested" to mapOf("value" to 42)
                )
            )

            val ref = store.savePng("01_import", "preview", bitmap, metadata)

            val expectedPath = tempDir
                .resolve("artifacts")
                .resolve("01_import")
                .resolve("preview_abcdef.png")
            assertEquals(expectedPath, ref.path)
            assertEquals("preview", ref.kind)
            assertTrue(Files.exists(ref.path))

            val pngBytes = Files.readAllBytes(ref.path)
            assertTrue("PNG signature is invalid", pngBytes.size > 8 && pngBytes.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE))

            val metadataPath = ref.path.resolveSibling("preview_abcdef.meta.json")
            assertTrue(Files.exists(metadataPath))

            val metadataJson = json.parseToJsonElement(Files.readString(metadataPath)).jsonObject
            assertEquals("run-1", metadataJson.getValue("run_id").jsonPrimitive.content)
            assertEquals("01_import", metadataJson.getValue("step").jsonPrimitive.content)
            assertEquals("abcdef", metadataJson.getValue("params_hash").jsonPrimitive.content)
            assertEquals("2024-01-01T00:00:00Z", metadataJson.getValue("timestamp").jsonPrimitive.content)

            val metaObject = metadataJson.getValue("meta").jsonObject
            assertEquals("test", metaObject.getValue("label").jsonPrimitive.content)
            assertEquals(0.95, metaObject.getValue("score").jsonPrimitive.double, 0.0001)
            val flags = metaObject.getValue("flags").jsonArray
            assertEquals(true, flags[0].jsonPrimitive.boolean)
            assertEquals(false, flags[1].jsonPrimitive.boolean)
            assertEquals(42, metaObject.getValue("nested").jsonObject.getValue("value").jsonPrimitive.int)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    companion object {
        private val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
    }
}
