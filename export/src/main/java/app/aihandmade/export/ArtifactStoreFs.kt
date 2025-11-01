package app.aihandmade.export

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Clock
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Filesystem-backed implementation of [ArtifactStore].
 */
class ArtifactStoreFs(
    private val runId: String,
    private val rootDir: Path,
    private val clock: Clock = Clock.systemUTC(),
    private val pngWriter: PngWriter = PngWriter(),
    private val json: Json = Json { prettyPrint = true }
) : ArtifactStore {
    override fun savePng(step: String, name: String, bitmap: Bitmap, meta: ArtifactMetadata): ArtifactRef {
        require(step.isNotBlank()) { "step must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }

        val artifactsDir = rootDir.resolve("artifacts")
        val stepDir = artifactsDir.resolve(step)
        stepDir.createDirectories()

        val baseName = "${sanitizeName(name)}_${meta.paramsHash}"
        val imagePath = stepDir.resolve("${baseName}.png")
        val metadataPath = stepDir.resolve("${baseName}.meta.json")

        val pngBytes = pngWriter.write(bitmap)
        Files.write(imagePath, pngBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        val timestamp = DateTimeFormatter.ISO_INSTANT.format(clock.instant())
        val metadataJson = JsonObject(
            mapOf(
                "run_id" to JsonPrimitive(runId),
                "step" to JsonPrimitive(step),
                "params_hash" to JsonPrimitive(meta.paramsHash),
                "timestamp" to JsonPrimitive(timestamp),
                "meta" to buildMetaObject(meta.meta)
            )
        )

        val serialized = json.encodeToString(metadataJson)
        Files.writeString(
            metadataPath,
            serialized,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )

        return ArtifactRef(imagePath)
    }

    private fun buildMetaObject(meta: Map<String, Any?>): JsonObject {
        val content = meta.mapValues { (_, value) -> value.toJsonElement() }
        return JsonObject(content)
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> {
            val entries = this.entries.associate { (key, value) ->
                key.toString() to value.toJsonElement()
            }
            JsonObject(entries)
        }
        is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
        is Array<*> -> JsonArray(this.map { it.toJsonElement() })
        is IntArray -> JsonArray(this.map { JsonPrimitive(it) })
        is LongArray -> JsonArray(this.map { JsonPrimitive(it) })
        is DoubleArray -> JsonArray(this.map { JsonPrimitive(it) })
        is FloatArray -> JsonArray(this.map { JsonPrimitive(it) })
        is ShortArray -> JsonArray(this.map { JsonPrimitive(it.toInt()) })
        is ByteArray -> JsonArray(this.map { JsonPrimitive(it.toInt()) })
        is BooleanArray -> JsonArray(this.map { JsonPrimitive(it) })
        else -> JsonPrimitive(this.toString())
    }

    private fun sanitizeName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return "artifact"
        }
        val sanitized = buildString(trimmed.length) {
            for (char in trimmed) {
                append(
                    when (char) {
                        in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_' -> char
                        else -> '_'
                    }
                )
            }
        }
        return if (sanitized.isBlank()) "artifact" else sanitized
    }
}
