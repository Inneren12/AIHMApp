package app.aihandmade.run

import app.aihandmade.storage.Paths
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.io.path.div
import kotlin.io.use
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Provides a loose schema check ensuring lifecycle events expose essential fields.
 */
class EventSchemaSmokeTest {

    @Test
    fun `run start event exposes standard fields`() {
        val clock = Clock.fixed(Instant.parse("2025-04-05T06:07:08Z"), ZoneOffset.UTC)
        val generator = RunIdGenerator { "RID-SCHEMA" }

        TestIO.withTempDir { tempDir ->
            RunContextFactory(tempDir.toFile(), generator, clock).create("schema-project").use { context ->
                val events = TestIO.readJsonl(context.runDirectory / Paths.EVENTS)
                assertTrue(events.isNotEmpty())
                val first = TestIO.parseJson(events.first())
                assertTrue(first.hasNonNull("event"))
                assertTrue(first.hasNonNull("timestamp"))
                assertTrue(first.hasNonNull("run_id"))
            }
        }
    }
}
