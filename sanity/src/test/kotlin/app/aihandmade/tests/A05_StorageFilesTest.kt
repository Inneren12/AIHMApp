package app.aihandmade.tests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class A05_StorageFilesTest {
  @Test fun storageHelpersExist() {
    val root = RepoRoot.locate()
    val base = root.resolve("storage/src/main/java/app/aihandmade/storage")
    listOf("Paths.kt", "RunDirs.kt").forEach { f ->
      assertTrue(Files.exists(base.resolve(f)), "MISSING: storage/$f")
    }
  }
}
