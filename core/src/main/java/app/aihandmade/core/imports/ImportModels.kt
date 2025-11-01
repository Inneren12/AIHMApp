package app.aihandmade.core.imports

/** Parameters for the import pipeline step. */
data class ImportParams(val uri: String?)

/** Result produced by the import pipeline step. */
data class ImportResult(
    val width: Int,
    val height: Int,
    val megapixels: Float,
)
