package app.aihandmade.core.imports

import app.aihandmade.core.pipeline.PipelineStep
import app.aihandmade.core.pipeline.RunContext
import app.aihandmade.core.pipeline.Step
import app.aihandmade.core.pipeline.StepResult
import app.aihandmade.export.ArtifactMetadata
import app.aihandmade.export.Bitmap
import java.io.IOException
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

        return try {
            val width = bitmap.width
            val height = bitmap.height
            val megapixels = (width.toLong() * height.toLong()) / 1_000_000f

            val paramsHash = hashParams(canonicalParams)
            val artifactMetadata = ArtifactMetadata(
                paramsHash = paramsHash,
                meta = linkedMapOf(
                    "params" to canonicalParams,
                    "width_px" to width,
                    "height_px" to height,
                    "megapixels" to megapixels,
                ),
            )

            val artifactRef = context.artifactStore.savePng(
                step = STEP_DIRECTORY,
                name = ARTIFACT_NAME,
                bitmap = bitmap,
                meta = artifactMetadata,
            )

            val metrics = mapOf(
                "width_px" to width,
                "height_px" to height,
                "megapixels" to megapixels,
            )

            val artifacts = mapOf(
                ARTIFACT_NAME to artifactRef.path.toString(),
            )

            val result = ImportResult(
                width = width,
                height = height,
                megapixels = megapixels,
            )

            context.logger.endSpan(
                span = span,
                metrics = metrics,
                artifacts = artifacts,
                status = "ok",
            )

            StepResult(
                value = result,
                metrics = metrics,
                artifacts = artifacts,
            )
        } catch (throwable: Throwable) {
            val errorMetrics = mapOf(
                "error_message" to (throwable.message ?: throwable::class.java.name),
                "stack" to throwable.stackTraceToString(),
            )
            context.logger.endSpan(
                span = span,
                metrics = errorMetrics,
                artifacts = emptyMap(),
                status = "error",
            )

            when (throwable) {
                is IOException, is IllegalStateException -> throw throwable
                else -> throw IllegalStateException("Step ${type.name} failed", throwable)
            }
        }
    }

    override suspend fun run(params: ImportParams, context: RunContext): StepResult<ImportResult> {
        throw UnsupportedOperationException(
            "ImportStep requires a decoded Bitmap. Use run(params, bitmap, context).",
        )
    }

    private fun canonicalize(params: ImportParams): String = params.toString()

    private fun hashParams(canonical: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(canonical.toByteArray(StandardCharsets.UTF_8))
        val builder = StringBuilder(hashed.size * 2)
        for (byte in hashed) {
            builder.append(String.format(Locale.US, "%02x", byte.toInt() and 0xFF))
        }
        return builder.toString()
    }

    companion object {
        private const val STEP_DIRECTORY = "01_import"
        private const val ARTIFACT_NAME = "import_normalized"
    }
}
