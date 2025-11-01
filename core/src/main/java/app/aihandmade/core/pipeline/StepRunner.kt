package app.aihandmade.core.pipeline

import app.aihandmade.export.ArtifactMetadata
import app.aihandmade.export.Bitmap
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.Locale

object StepRunner {
    data class PngArtifact(
        val name: String,
        val bitmap: Bitmap,
        val meta: Map<String, Any?> = emptyMap()
    )

    suspend fun <P, R> runStep(
        context: RunContext,
        step: Step<P, R>,
        params: P,
        pngProducer: ((StepResult<R>) -> List<PngArtifact>)? = null,
    ): StepResult<R> {
        val canonicalParams = canonicalizeParams(params)
        val span = context.logger.startSpan(step.type.name, canonicalParams)

        try {
            val result = step.run(params, context)
            val artifactsForLogging = LinkedHashMap<String, Any?>(result.artifacts)

            pngProducer?.let { producer ->
                val pngArtifacts = producer(result)
                if (pngArtifacts.isNotEmpty()) {
                    val paramsHash = hashParams(canonicalParams)
                    val stepId = formatStepId(step.type)
                    for (artifact in pngArtifacts) {
                        val metadata = ArtifactMetadata(
                            paramsHash = paramsHash,
                            meta = artifact.meta
                        )
                        val artifactRef = context.artifactStore.savePng(
                            step = stepId,
                            name = artifact.name,
                            bitmap = artifact.bitmap,
                            meta = metadata,
                        )
                        artifactsForLogging[artifact.name] = artifactRef.path.toString()
                    }
                }
            }

            val finalResult = result.copy(artifacts = artifactsForLogging.toMap())
            context.logger.endSpan(span, metrics = finalResult.metrics, artifacts = finalResult.artifacts)
            return finalResult
        } catch (ioException: IOException) {
            context.logger.endSpan(
                span,
                metrics = errorMetrics(ioException),
                status = "error",
            )
            throw ioException
        } catch (illegalState: IllegalStateException) {
            context.logger.endSpan(
                span,
                metrics = errorMetrics(illegalState),
                status = "error",
            )
            throw illegalState
        } catch (throwable: Throwable) {
            context.logger.endSpan(
                span,
                metrics = errorMetrics(throwable),
                status = "error",
            )
            throw IllegalStateException("Step ${'$'}{step.type} failed", throwable)
        }
    }

    private fun errorMetrics(throwable: Throwable): Map<String, Any?> {
        val writer = StringWriter()
        PrintWriter(writer).use { printWriter ->
            throwable.printStackTrace(printWriter)
        }
        return mapOf(
            "error_message" to (throwable.message ?: throwable::class.java.name),
            "stack" to writer.toString(),
        )
    }

    private fun hashParams(canonicalParams: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(canonicalParams.toByteArray(StandardCharsets.UTF_8))
        val builder = StringBuilder(hashed.size * 2)
        for (byte in hashed) {
            builder.append(String.format(Locale.US, "%02x", byte.toInt() and 0xFF))
        }
        return builder.toString()
    }

    private fun formatStepId(step: PipelineStep): String {
        val index = step.ordinal + 1
        val name = step.name.lowercase(Locale.US)
        return String.format(Locale.US, "%02d_%s", index, name)
    }

    private fun canonicalizeParams(params: Any?): String {
        val builder = StringBuilder()
        appendCanonical(builder, params)
        return builder.toString()
    }

    private fun appendCanonical(builder: StringBuilder, value: Any?) {
        when (value) {
            null -> builder.append("null")
            is String -> appendEscapedString(builder, value)
            is Number, is Boolean -> builder.append(value.toString())
            is Enum<*> -> appendEscapedString(builder, value.name)
            is Map<*, *> -> appendCanonicalMap(builder, value)
            is Iterable<*> -> appendCanonicalIterable(builder, value)
            is Array<*> -> appendCanonicalIterable(builder, value.asList())
            is IntArray -> appendCanonicalIterable(builder, value.toList())
            is LongArray -> appendCanonicalIterable(builder, value.toList())
            is DoubleArray -> appendCanonicalIterable(builder, value.toList())
            is FloatArray -> appendCanonicalIterable(builder, value.toList())
            is ShortArray -> appendCanonicalIterable(builder, value.toList())
            is ByteArray -> appendCanonicalIterable(builder, value.toList())
            is BooleanArray -> appendCanonicalIterable(builder, value.toList())
            else -> appendEscapedString(builder, value.toString())
        }
    }

    private fun appendCanonicalMap(builder: StringBuilder, map: Map<*, *>) {
        builder.append('{')
        val entries = map.entries.sortedBy { it.key.toString() }
        var first = true
        for ((key, value) in entries) {
            if (!first) {
                builder.append(',')
            }
            first = false
            appendEscapedString(builder, key.toString())
            builder.append(':')
            appendCanonical(builder, value)
        }
        builder.append('}')
    }

    private fun appendCanonicalIterable(builder: StringBuilder, iterable: Iterable<*>) {
        builder.append('[')
        var first = true
        for (element in iterable) {
            if (!first) {
                builder.append(',')
            }
            first = false
            appendCanonical(builder, element)
        }
        builder.append(']')
    }

    private fun appendEscapedString(builder: StringBuilder, value: String) {
        builder.append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> builder.append("\\\\")
                '\"' -> builder.append("\\\"")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> {
                    if (ch < ' ') {
                        builder.append(String.format(Locale.US, "\\u%04x", ch.code))
                    } else {
                        builder.append(ch)
                    }
                }
            }
        }
        builder.append('"')
    }
}
