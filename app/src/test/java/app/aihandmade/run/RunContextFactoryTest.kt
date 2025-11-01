package app.aihandmade.run

import app.aihandmade.storage.Paths
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

class RunContextFactoryTest {

    @Test
    fun `creates run context with directories and working logger`() {
        val tempRoot = Files.createTempDirectory("run-context-test").toFile()
        try {
            val projectId = "project-${System.currentTimeMillis()}"
            val factory = RunContextFactory(tempRoot)

            val context = factory.create(projectId)

            val projectDir = tempRoot.toPath()
                .resolve(Paths.PROJECTS)
                .resolve(projectId)
            assertTrue(projectDir.toFile().exists())

            val runDir = projectDir
                .resolve(Paths.RUNS)
                .resolve(context.runId)
            assertTrue(runDir.toFile().exists())

            val eventsFile = runDir.resolve(Paths.EVENTS).toFile()
            assertTrue(eventsFile.exists())

            context.logger.writeEvent("info", "test message")

            val contents = eventsFile.readText()
            assertTrue(contents.contains("test message"))
            assertTrue(contents.contains(context.runId))

            val artifactsDir = runDir.resolve(Paths.ARTIFACTS).toFile()
            assertTrue(artifactsDir.exists())
            assertTrue(artifactsDir.isDirectory)
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
