package app.aihandmade.tests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class A03_CoreContractsFilesTest {
  @Test fun corePipelineContractsExist() {
    val root = RepoRoot.locate()
    val base = root.resolve("core/src/main/java/app/aihandmade/core/pipeline")
    listOf(
      "PipelineStep.kt", "Step.kt", "RunContext.kt", "StepResult.kt"
    ).forEach { f ->
      assertTrue(Files.exists(base.resolve(f)), "MISSING: core/pipeline/$f")
    }
  }
}
