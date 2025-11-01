package app.aihandmade.tests

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertTrue

class A04_LoggingFilesTest {
  @Test fun loggingFilesPresent() {
    val root = RepoRoot.locate()
    val base = root.resolve("logging/src/main/java/app/aihandmade/logging")
    listOf("Logger.kt","JsonLogger.kt","ManifestWriter.kt").forEach { f ->
      assertTrue(Files.exists(base.resolve(f)), "MISSING: logging/$f")
    }
  }
}
