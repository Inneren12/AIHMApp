package app.aihandmade.run

import java.util.UUID

/**
 * Generates unique run identifiers.
 */
fun interface RunIdGenerator {
    fun nextId(): String
}

/**
 * Default [RunIdGenerator] that produces random UUID-based identifiers.
 */
class UlidGenerator : RunIdGenerator {
    override fun nextId(): String = UUID.randomUUID().toString().replace("-", "")
}
