package app.aihandmade.logging

import java.time.Instant
import java.util.UUID

/**
 * Represents an abstract logging API used by the AI HandMade runtime.
 */
interface Logger {
    /**
     * Opaque span identifier returned when a step execution starts.
     */
    data class Span(
        val id: String = UUID.randomUUID().toString(),
        val step: String,
        val paramsCanonical: String,
        val startedAt: Instant
    )

    /**
     * Marks the beginning of a pipeline step and returns a [Span] handle that should be
     * supplied when the span ends.
     */
    fun startSpan(step: String, paramsCanonical: String): Span

    /**
     * Emits an event describing the end of a span previously returned by [startSpan].
     *
     * @param metrics Optional metrics describing the span execution.
     * @param artifacts Optional artifacts produced by the span.
     * @param status Span completion status. Defaults to "ok".
     */
    fun endSpan(
        span: Span,
        metrics: Map<String, Any?> = emptyMap(),
        artifacts: Map<String, Any?> = emptyMap(),
        status: String = "ok"
    )

    /**
     * Writes a structured event message at the given [level].
     */
    fun writeEvent(level: String, message: String, fields: Map<String, Any?> = emptyMap())
}
