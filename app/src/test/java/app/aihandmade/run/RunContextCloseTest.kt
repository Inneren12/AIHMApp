package app.aihandmade.run

import app.aihandmade.storage.Paths
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.div
import kotlin.io.path.readLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises lifecycle behaviour including run_end emission on close.
 */
class RunContextCloseTest {

    @Test
    fun `close appends run_end event exactly once`() {
        val clock = Clock.fixed(Instant.parse("2025-03-04T05:06:07Z"), ZoneOffset.UTC)
        val generator = RunIdGenerator { "RID-CLOSE" }

        TestIO.withTempDir { tempDir ->
            val factory = RunContextFactory(tempDir.toFile(), generator, clock)
            val context = factory.create("close-project")
            val eventsPath = context.runDirectory / Paths.EVENTS

            context.logger.writeEvent("info", "before close")

            context.close()
            val linesAfterClose = eventsPath.readLines()
            assertTrue(linesAfterClose.last().contains("run_end"))
            val parsed = TestIO.parseJson(linesAfterClose.last())
            assertEquals("run_end", parsed.get("event").asText())
            assertEquals("RID-CLOSE", parsed.get("run_id").asText())

            val countAfterFirstClose = linesAfterClose.size
            context.close()
            val linesAfterSecondClose = eventsPath.readLines()
            assertEquals(countAfterFirstClose, linesAfterSecondClose.size)

            val manifestPath = context.runDirectory / Paths.MANIFEST
            assertTrue("manifest.json should exist after close", manifestPath.toFile().exists())
        }
    }
}
