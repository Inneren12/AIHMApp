package app.aihandmade.imports

import android.content.Context
import android.net.Uri
import app.aihandmade.core.imports.ImportParams
import app.aihandmade.core.imports.ImportResult
import app.aihandmade.core.imports.ImportStep
import app.aihandmade.core.pipeline.RunContext
import app.aihandmade.core.pipeline.StepResult
import app.aihandmade.img.ImageDecoder
import app.aihandmade.export.Bitmap as CoreBitmap

class ImportAdapter(
    private val context: Context,
    private val step: ImportStep,
    private val maxSidePx: Int = 4096,
) {
    suspend fun runStep(params: ImportParams, runContext: RunContext): StepResult<ImportResult> {
        val uri = params.uri?.let(Uri::parse)
            ?: throw IllegalArgumentException("Import URI must be provided")

        val decoded = ImageDecoder.decode(context, uri, maxSidePx)
        val coreBitmap = decoded.toCoreBitmap()
        decoded.recycle()

        return step.run(params, coreBitmap, runContext)
    }

    private fun android.graphics.Bitmap.toCoreBitmap(): CoreBitmap {
        val pixelCount = width * height
        val pixels = IntArray(pixelCount)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return CoreBitmap(width, height, pixels)
    }
}
