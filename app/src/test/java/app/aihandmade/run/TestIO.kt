package app.aihandmade.run

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

/**
 * Test-only filesystem helpers for working with the runtime layout.
 */
object TestIO {
    private val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    fun readJsonl(path: Path): List<String> =
        if (path.exists()) Files.readAllLines(path) else emptyList()

    fun parseJson(line: String): JsonNode = mapper.readTree(line)

    fun <T> withTempDir(prefix: String = "runtime-test", block: (Path) -> T): T {
        val dir = Files.createTempDirectory(prefix)
        return try {
            block(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    fun writeBytes(path: Path, bytes: ByteArray) {
        path.parent?.createDirectories()
        path.writeBytes(bytes)
    }

    fun await(timeout: Duration, poll: Duration = Duration.ofMillis(50), condition: () -> Boolean) {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (condition()) {
                return
            }
            Thread.sleep(poll.toMillis())
        }
        if (!condition()) {
            throw IOException("Condition was not met within ${timeout.toMillis()} ms")
        }
    }
}
