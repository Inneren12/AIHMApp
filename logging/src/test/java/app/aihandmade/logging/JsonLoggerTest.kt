package app.aihandmade.logging

import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class JsonLoggerTest {

    private val fixedInstant = Instant.parse("2024-01-01T00:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    @Test
    fun `writes events as JSON lines`() {
        val tempDir = Files.createTempDirectory("json-logger-test")
        tempDir.toFile().deleteOnExit()

        val manifestWriter = ManifestWriter("run-123", tempDir, clock)
        val logger = JsonLogger(runId = "run-123", outputDir = tempDir, manifestWriter = manifestWriter, clock = clock)

        val span = logger.startSpan("download", "{\"url\":\"example\"}")
        logger.endSpan(
            span = span,
            metrics = mapOf("duration_ms" to 123),
            artifacts = mapOf("file" to "output.txt"),
            status = "ok"
        )

        logger.writeEvent("info", "finished", mapOf("step" to "download"))

        val lines = Files.readAllLines(tempDir.resolve("events.jsonl"))
        assertEquals(3, lines.size)

        val json = Json { ignoreUnknownKeys = false }
        val startEvent = json.parseToJsonElement(lines[0]).jsonObject
        val endEvent = json.parseToJsonElement(lines[1]).jsonObject
        val logEvent = json.parseToJsonElement(lines[2]).jsonObject

        assertEquals("span_start", startEvent["event"]?.jsonPrimitive?.content)
        assertEquals("span_end", endEvent["event"]?.jsonPrimitive?.content)
        assertEquals("log", logEvent["event"]?.jsonPrimitive?.content)

        assertTrue(startEvent.containsKey("timestamp"))
        assertTrue(endEvent.containsKey("metrics"))
        assertTrue(logEvent.containsKey("fields"))
    }
}
