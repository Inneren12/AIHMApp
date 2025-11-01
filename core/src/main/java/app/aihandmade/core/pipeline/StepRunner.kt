package app.aihandmade.core.pipeline

import app.aihandmade.export.ArtifactMetadata
import app.aihandmade.export.Bitmap
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

/**
 * Executes a pipeline [Step] while taking care of logging and artifact persistence.
 */
class StepRunner {

    data class PngArtifact(
        val name: String,
        val bitmap: Bitmap,
        val meta: Map<String, Any?> = emptyMap(),
    )

    suspend fun <P, R> runStep(
        context: RunContext,
        step: Step<P, R>,
        params: P,
        pngProducer: ((StepResult<R>) -> List<PngArtifact>)? = null,
    ): StepResult<R> {
        val canonicalParams = canonicalize(params)
        val span = context.logger.startSpan(step.type.name, canonicalParams)

        return try {
            val result = step.run(params, context)

            val combinedArtifacts = LinkedHashMap<String, Any?>(result.artifacts)
            val pngArtifacts = pngProducer?.invoke(result).orEmpty()
            if (pngArtifacts.isNotEmpty()) {
                val paramsHash = hashParams(canonicalParams)
                val stepDirectory = formatStepDirectory(step.type)
                for (artifact in pngArtifacts) {
                    val metadata = ArtifactMetadata(
                        paramsHash = paramsHash,
                        meta = buildArtifactMeta(canonicalParams, artifact.meta),
                    )
                    val artifactRef = context.artifactStore.savePng(
                        step = stepDirectory,
                        name = artifact.name,
                        bitmap = artifact.bitmap,
                        meta = metadata,
                    )
                    combinedArtifacts[artifact.name] = artifactRef.path.toString()
                }
            }

            context.logger.endSpan(
                span = span,
                metrics = result.metrics,
                artifacts = combinedArtifacts,
                status = "ok",
            )

            result.copy(artifacts = combinedArtifacts)
        } catch (throwable: Throwable) {
            val errorMetrics = mapOf(
                "error_message" to (throwable.message ?: throwable::class.java.name),
                "stack" to throwable.stackTraceToString(),
            )
            context.logger.endSpan(span, metrics = errorMetrics, status = "error")

            when (throwable) {
                is IOException, is IllegalStateException -> throw throwable
                else -> throw IllegalStateException("Step ${step.type} failed", throwable)
            }
        }
    }

    private fun canonicalize(params: Any?): String = params?.toString() ?: "null"

    private fun buildArtifactMeta(
        canonicalParams: String,
        customMeta: Map<String, Any?>,
    ): Map<String, Any?> {
        if (customMeta.isEmpty()) {
            return mapOf("params" to canonicalParams)
        }
        val merged = LinkedHashMap<String, Any?>(customMeta.size + 1)
        merged.putAll(customMeta)
        merged.putIfAbsent("params", canonicalParams)
        return merged
    }

    private fun formatStepDirectory(step: PipelineStep): String {
        val index = step.ordinal + 1
        val name = step.name.lowercase(Locale.US)
        return String.format(Locale.US, "%02d_%s", index, name)
    }

    private fun hashParams(canonical: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(canonical.toByteArray(StandardCharsets.UTF_8))
        val builder = StringBuilder(hashed.size * 2)
        for (byte in hashed) {
            builder.append(String.format(Locale.US, "%02x", byte.toInt() and 0xFF))
        }
        return builder.toString()
    }
}
