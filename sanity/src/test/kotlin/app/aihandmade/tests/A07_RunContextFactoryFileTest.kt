package app.aihandmade.tests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class A07_RunContextFactoryFileTest {
  @Test fun runContextFactoryExists() {
    val root = RepoRoot.locate()
    val f = root.resolve("app/src/main/java/app/aihandmade/run/RunContextFactory.kt")
    assertTrue(Files.exists(f), "MISSING: app/run/RunContextFactory.kt")
  }
}
