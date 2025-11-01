package app.aihandmade.core.imports

import app.aihandmade.core.pipeline.PipelineStep
import app.aihandmade.core.pipeline.RunContext
import app.aihandmade.core.pipeline.Step
import app.aihandmade.core.pipeline.StepResult
import app.aihandmade.export.ArtifactMetadata
import app.aihandmade.export.Bitmap
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

class ImportStep : Step<ImportParams, ImportResult> {
    override val type: PipelineStep = PipelineStep.IMPORT

    suspend fun run(
        params: ImportParams,
        bitmap: Bitmap,
        context: RunContext,
    ): StepResult<ImportResult> {
        val canonicalParams = canonicalize(params)
        val span = context.logger.startSpan(type.name, canonicalParams)

        try {
            val width = bitmap.width
            val height = bitmap.height
            val megapixels = (width.toLong() * height.toLong()) / 1_000_000f

            val paramsHash = hashParams(canonicalParams)
            val artifactMetadata = ArtifactMetadata(
                paramsHash = paramsHash,
                meta = mapOf(
                    "params" to canonicalParams,
                    "width_px" to width,
                    "height_px" to height,
                )
            )

            val artifactRef = context.artifactStore.savePng(
                step = "01_import",
                name = "import_normalized",
                bitmap = bitmap,
                meta = artifactMetadata,
            )

            val metrics = mapOf(
                "width_px" to width,
                "height_px" to height,
                "megapixels" to megapixels,
            )

            val artifacts = mapOf(
                "import_normalized" to artifactRef.path.toString(),
            )

            val result = ImportResult(
                width = width,
                height = height,
                megapixels = megapixels,
            )

            context.logger.endSpan(span, metrics = metrics, artifacts = artifacts)

            return StepResult(
                value = result,
                metrics = metrics,
                artifacts = artifacts,
            )
        } catch (throwable: Throwable) {
            context.logger.endSpan(span, status = "error")
            throw throwable
        }
    }

    override suspend fun run(params: ImportParams, context: RunContext): StepResult<ImportResult> {
        throw UnsupportedOperationException("ImportStep requires a decoded Bitmap. Use run(params, bitmap, context).")
    }

    private fun canonicalize(params: ImportParams): String {
        val uri = params.uri
        return buildString {
            append('{')
            append("\"uri\":")
            if (uri == null) {
                append("null")
            } else {
                append('"')
                append(escapeJson(uri))
                append('"')
            }
            append('}')
        }
    }

    private fun escapeJson(value: String): String {
        val builder = StringBuilder(value.length)
        for (ch in value) {
            when (ch) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
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
        return builder.toString()
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
