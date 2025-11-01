package app.aihandmade.run

import app.aihandmade.storage.Paths
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that the factory produces unique runs when accessed concurrently.
 */
class RunContextConcurrencyTest {

    @Test(timeout = 10_000)
    fun `creates unique run directories across threads`() {
        TestIO.withTempDir { tempDir ->
            val factory = RunContextFactory(tempDir.toFile())
            val projectId = "concurrent"
            val executor = Executors.newFixedThreadPool(16)

            val futures = (0 until 20).map {
                executor.submit(Callable {
                    factory.create(projectId)
                })
            }

            val contexts = futures.map { it.get(5, TimeUnit.SECONDS) }
            try {
                val runIds = contexts.map { it.runId }
                assertEquals(runIds.size, runIds.toSet().size)

                contexts.forEach { context ->
                    val runDir = context.runDirectory
                    assertTrue(Files.exists(runDir))
                    assertTrue((runDir / Paths.ARTIFACTS).isDirectory())
                    assertTrue((runDir / Paths.TMP).isDirectory())
                    assertTrue((runDir / Paths.EVENTS).isRegularFile())
                }
            } finally {
                contexts.forEach { it.close() }
                executor.shutdownNow()
            }
        }
    }
}
