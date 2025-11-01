package app.aihandmade.tests

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertTrue
import kotlin.text.RegexOption

class CI_WorkflowPresenceTest {
  @Test
  fun workflowExistsAndRunsTests() {
    val root = RepoRoot.locate()
    val wf = root.resolve(".github/workflows/android-ci.yml")
    assertTrue(Files.exists(wf), "MISSING: .github/workflows/android-ci.yml")
    val text = Files.readString(wf)

    assertTrue(
      text.contains("./gradlew build") || text.contains("./gradlew assemble"),
      "CI workflow lacks build step",
    )
    assertTrue(
      text.contains("./gradlew test") || Regex(
        "run:\s*\|[\s\S]*gradlew.*test",
        RegexOption.DOTALL,
      ).containsMatchIn(text),
      "CI workflow lacks test step",
    )
  }
}
