package app.aihandmade.tests

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertTrue

class A06_ExportFilesTest {
  @Test fun exportArtifactsApiExists() {
    val root = RepoRoot.locate()
    val base = root.resolve("export/src/main/java/app/aihandmade/export")
    listOf("ArtifactStore.kt","ArtifactStoreFs.kt","PngWriter.kt").forEach { f ->
      assertTrue(Files.exists(base.resolve(f)), "MISSING: export/$f")
    }
  }
}
