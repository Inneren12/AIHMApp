package app.aihandmade.tests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class A09_ImportStepFilesTest {
  @Test fun importStepAndAdapterExist() {
    val root = RepoRoot.locate()
    val coreBase = root.resolve("core/src/main/java/app/aihandmade/core/imports")
    val appBase  = root.resolve("app/src/main/java/app/aihandmade/imports")
    listOf("ImportModels.kt","ImportStep.kt").forEach { f ->
      assertTrue(Files.exists(coreBase.resolve(f)), "MISSING: core/imports/$f")
    }
    assertTrue(Files.exists(appBase.resolve("ImportAdapter.kt")),
      "MISSING: app/imports/ImportAdapter.kt")
  }
}
