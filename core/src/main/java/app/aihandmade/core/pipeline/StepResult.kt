package app.aihandmade.core.pipeline

data class StepResult<T>(
    val value: T,
    val metrics: Map<String, Any?> = emptyMap(),
    val artifacts: Map<String, Any?> = emptyMap()
)
