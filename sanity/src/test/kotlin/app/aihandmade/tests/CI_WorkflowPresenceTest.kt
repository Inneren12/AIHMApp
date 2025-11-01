package app.aihandmade.tests

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertTrue

class CI_WorkflowPresenceTest {
  @Test fun workflowExistsAndRunsTests() {
    val root = RepoRoot.locate()
    val wf = root.resolve(".github/workflows/android-ci.yml")
    assertTrue(Files.exists(wf), "MISSING: .github/workflows/android-ci.yml")
    val text = Files.readString(wf)
    // Базовые ожидания: сборка и тесты есть в workflow
    assertTrue(text.contains("./gradlew build") || text.contains("./gradlew assemble"),
      "CI workflow lacks build step")
    assertTrue(text.contains("./gradlew test"),
      "CI workflow lacks test step")
  }
}
