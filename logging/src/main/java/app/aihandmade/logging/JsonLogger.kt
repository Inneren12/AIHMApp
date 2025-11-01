package app.aihandmade.logging

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.createDirectories
import kotlin.text.Charsets
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Writes structured events into a JSON lines file (events.jsonl).
 */
class JsonLogger(
    private val runId: String,
    private val outputDir: Path,
    private val manifestWriter: ManifestWriter? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val json: Json = Json { encodeDefaults = false }
) : Logger {

    private val lock = ReentrantLock()
    private val eventFile: Path
    private val timestampFormatter = DateTimeFormatter.ISO_INSTANT

    init {
        outputDir.createDirectories()
        eventFile = outputDir.resolve("events.jsonl")
    }

    override fun startSpan(step: String, paramsCanonical: String): Logger.Span {
        val span = Logger.Span(step = step, paramsCanonical = paramsCanonical, startedAt = clock.instant())
        manifestWriter?.recordStep(step, paramsCanonical)
        val payload = buildJsonObject {
            put("event", JsonPrimitive("span_start"))
            put("run_id", JsonPrimitive(runId))
            put("span_id", JsonPrimitive(span.id))
            put("step", JsonPrimitive(step))
            put("params_canonical", JsonPrimitive(paramsCanonical))
            putTimestamp(span.startedAt)
        }
        writeEventObject(payload)
        return span
    }

    override fun endSpan(
        span: Logger.Span,
        metrics: Map<String, Any?>,
        artifacts: Map<String, Any?>,
        status: String
    ) {
        val payload = buildJsonObject {
            put("event", JsonPrimitive("span_end"))
            put("run_id", JsonPrimitive(runId))
            put("span_id", JsonPrimitive(span.id))
            put("step", JsonPrimitive(span.step))
            put("status", JsonPrimitive(status))
            putTimestamp(clock.instant())
            if (metrics.isNotEmpty()) {
                put("metrics", metrics.toJsonElement())
            }
            if (artifacts.isNotEmpty()) {
                put("artifacts", artifacts.toJsonElement())
            }
        }
        writeEventObject(payload)
    }

    override fun writeEvent(level: String, message: String, fields: Map<String, Any?>) {
        val now = clock.instant()
        val payload = buildJsonObject {
            put("event", JsonPrimitive("log"))
            put("run_id", JsonPrimitive(runId))
            put("level", JsonPrimitive(level))
            put("message", JsonPrimitive(message))
            putTimestamp(now)
            if (fields.isNotEmpty()) {
                put("fields", fields.toJsonElement())
            }
        }
        writeEventObject(payload)
    }

    private fun JsonObjectBuilder.putTimestamp(instant: Instant) {
        put("timestamp", JsonPrimitive(timestampFormatter.format(instant)))
    }

    private fun Map<String, Any?>.toJsonElement(): JsonElement {
        val content = mutableMapOf<String, JsonElement>()
        for ((key, value) in this) {
            content[key] = value.toJsonElement()
        }
        return JsonObject(content)
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Map<*, *> -> {
            val content = mutableMapOf<String, JsonElement>()
            for ((key, value) in this) {
                if (key is String) {
                    content[key] = value.toJsonElement()
                }
            }
            JsonObject(content)
        }
        is Iterable<*> -> buildJsonArray {
            for (item in this@toJsonElement) {
                add(item.toJsonElement())
            }
        }
        is Array<*> -> buildJsonArray {
            for (item in this@toJsonElement) {
                add(item.toJsonElement())
            }
        }
        else -> JsonPrimitive(this.toString())
    }

    private fun writeEventObject(payload: JsonObject) {
        val serialized = json.encodeToString(payload)
        lock.withLock {
            Files.newBufferedWriter(
                eventFile,
                Charsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            ).use { writer ->
                writer.write(serialized)
                writer.write("\n")
            }
        }
    }
}
