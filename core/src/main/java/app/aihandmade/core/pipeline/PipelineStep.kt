package app.aihandmade.core.pipeline

enum class PipelineStep {
    IMPORT,
    AUTOSUGGEST,
    CLASSIFY,
    PREPROCESS,
    MASKS,
    QUANTIZE,
    GRID,
    POST,
    EXPORT
}
