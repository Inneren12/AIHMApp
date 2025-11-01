package app.aihandmade.logging

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.io.path.createDirectories
import kotlin.text.Charsets
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Persists a manifest describing a run and the steps executed.
 */
class ManifestWriter(
    private val runId: String,
    private val outputDir: Path,
    private val clock: Clock = Clock.systemUTC(),
    private val json: Json = Json { prettyPrint = true }
) {
    private val steps = mutableListOf<StepRecord>()
    private val createdAt: Instant = clock.instant()

    data class StepRecord(val name: String, val paramsCanonical: String, val paramsHash: String)

    /** Records a step with its canonical parameters. */
    fun recordStep(step: String, paramsCanonical: String) {
        val digest = paramsCanonical.toByteArray(Charsets.UTF_8)
        val hash = hashBytes(digest)
        synchronized(steps) {
            steps.add(StepRecord(step, paramsCanonical, hash))
        }
    }

    /**
     * Writes the manifest.json file with the collected step information.
     */
    fun write(): Path {
        outputDir.createDirectories()
        val manifestFile = outputDir.resolve("manifest.json")
        val formatter = DateTimeFormatter.ISO_INSTANT

        val stepsArray = synchronized(steps) {
            JsonArray(steps.map { record ->
                JsonObject(
                    mapOf(
                        "step" to JsonPrimitive(record.name),
                        "params_canonical" to JsonPrimitive(record.paramsCanonical),
                        "params_hash" to JsonPrimitive(record.paramsHash)
                    )
                )
            })
        }

        val root = JsonObject(
            mapOf(
                "run_id" to JsonPrimitive(runId),
                "timestamp" to JsonPrimitive(formatter.format(createdAt)),
                "steps" to stepsArray
            )
        )

        val serialized = json.encodeToString(root)
        Files.writeString(
            manifestFile,
            serialized,
            Charsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
        return manifestFile
    }

    private fun hashBytes(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(bytes)
        return buildString(hashed.size * 2) {
            for (b in hashed) {
                append(String.format(Locale.US, "%02x", b.toInt() and 0xFF))
            }
        }
    }
}
