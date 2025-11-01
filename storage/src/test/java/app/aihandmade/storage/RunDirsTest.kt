package app.aihandmade.storage

import java.nio.file.Files
import app.aihandmade.storage.Paths.EVENTS
import app.aihandmade.storage.Paths.MANIFEST
import app.aihandmade.storage.createProject
import app.aihandmade.storage.createRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunDirsTest {

    @Test
    fun `creates run directories and files`() {
        val rootDir = Files.createTempDirectory("storage-test").toFile()

        val projectDir = createProject(rootDir, "project-123")
        assertTrue(projectDir.exists())
        assertEquals("project-123", projectDir.name)

        val runId = "20240101T120000Z_abcd"
        val runDir = createRun(projectDir, runId)

        assertTrue(runDir.root.exists())
        assertEquals(runId, runDir.root.name)
        assertTrue(runDir.eventsFile.exists())
        assertEquals(EVENTS, runDir.eventsFile.name)
        assertTrue(runDir.manifestFile.exists())
        assertEquals(MANIFEST, runDir.manifestFile.name)
        assertTrue(runDir.artifactsDir.exists())
        assertTrue(runDir.artifactsDir.isDirectory)
    }
}
