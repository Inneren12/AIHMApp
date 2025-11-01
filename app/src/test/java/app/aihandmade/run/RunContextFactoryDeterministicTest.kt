package app.aihandmade.run

import app.aihandmade.storage.Paths
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlin.io.use
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ensures deterministic identifiers and directory layout when dependencies are fixed.
 */
class RunContextFactoryDeterministicTest {

    @Test
    fun `creates predictable run structure with fixed clock and generator`() {
        val fixedClock = Clock.fixed(Instant.parse("2025-01-02T03:04:05Z"), ZoneOffset.UTC)
        val generator = RunIdGenerator { "RID-0001" }
        val projectId = "demo-project"

        TestIO.withTempDir { tempDir ->
            val factory = RunContextFactory(tempDir.toFile(), generator, fixedClock)

            factory.create(projectId).use { context ->
                assertEquals("RID-0001", context.runId)

                val expectedRunDir = tempDir /
                    Paths.PROJECTS /
                    projectId /
                    Paths.RUNS /
                    "20250102" /
                    "RID-0001"

                assertEquals(expectedRunDir.pathString, context.runDirectory.pathString)
                assertTrue(Files.isDirectory(context.runDirectory))
                assertTrue((context.runDirectory / Paths.ARTIFACTS).isDirectory())
                assertTrue((context.runDirectory / Paths.TMP).isDirectory())
                assertTrue((context.runDirectory / Paths.EVENTS).isRegularFile())

                val events = TestIO.readJsonl(context.runDirectory / Paths.EVENTS)
                assertTrue("events.jsonl should contain at least one entry", events.isNotEmpty())

                val firstEvent = TestIO.parseJson(events.first())
                assertEquals("run_start", firstEvent.get("event").asText())
                assertEquals("RID-0001", firstEvent.get("run_id").asText())
                assertEquals("2025-01-02T03:04:05Z", firstEvent.get("timestamp").asText())
            }
        }
    }
}
