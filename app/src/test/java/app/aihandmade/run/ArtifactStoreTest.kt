package app.aihandmade.run

import app.aihandmade.export.ArtifactMetadata
import app.aihandmade.export.ArtifactStoreFs
import app.aihandmade.export.Bitmap
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.div
import kotlin.io.path.readText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the filesystem-backed artifact store to ensure nested writes succeed.
 */
class ArtifactStoreTest {

    @Test
    fun `persists artifact bytes and metadata`() {
        val clock = Clock.fixed(Instant.parse("2025-06-07T08:09:10Z"), ZoneOffset.UTC)

        TestIO.withTempDir { tempDir ->
            val store = ArtifactStoreFs(
                runId = "RID-ART",
                rootDir = tempDir,
                clock = clock
            )

            val bitmap = Bitmap.from(2, 2) { _, _ -> 0xFF00FF00.toInt() }
            val metadata = ArtifactMetadata(paramsHash = "hash123", meta = mapOf("note" to "demo"))

            val ref = store.savePng("step-a", "preview", bitmap, metadata)

            val expectedImage = tempDir / "artifacts" / "step-a" / "preview_hash123.png"
            val expectedMetadata = tempDir / "artifacts" / "step-a" / "preview_hash123.meta.json"

            assertEquals(expectedImage, ref.path)
            assertTrue(Files.exists(ref.path))
            assertTrue(Files.exists(expectedMetadata))

            val metadataContents = expectedMetadata.readText()
            assertTrue(metadataContents.contains("RID-ART"))
            assertTrue(metadataContents.contains("step-a"))
        }
    }
}
