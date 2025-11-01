package app.aihandmade.run

import app.aihandmade.storage.Paths
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.div
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Validates error handling when filesystem layout is already occupied by files.
 */
class RunContextFailureTest {

    @Test
    fun `throws meaningful exception when runs path is a file`() {
        val projectId = "broken-project"

        TestIO.withTempDir { tempDir ->
            val projectsDir = tempDir / Paths.PROJECTS
            val projectDir = projectsDir / projectId
            Files.createDirectories(projectDir)

            val runsFile = projectDir / Paths.RUNS
            Files.writeString(runsFile, "not a directory", StandardCharsets.UTF_8)

            val factory = RunContextFactory(tempDir.toFile())

            try {
                factory.create(projectId)
                fail("Expected factory.create to throw")
            } catch (ex: IOException) {
                assertTrue(ex.message?.contains("directory") == true)
            }
        }
    }
}
