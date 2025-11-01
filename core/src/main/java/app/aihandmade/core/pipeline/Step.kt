package app.aihandmade.core.pipeline

interface Step<P, R> {
    val type: PipelineStep

    suspend fun run(params: P, context: RunContext): StepResult<R>
}
