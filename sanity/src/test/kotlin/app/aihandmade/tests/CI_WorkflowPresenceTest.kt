package app.aihandmade.tests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.text.RegexOption

class CI_WorkflowPresenceTest {
  @Test
  fun workflowExistsAndRunsTests() {
    val root = RepoRoot.locate()
    val wf = root.resolve(".github/workflows/android-ci.yml")
    assertTrue(Files.exists(wf), "MISSING: .github/workflows/android-ci.yml")
    val text = Files.readString(wf)

    val hasBuild = text.contains("./gradlew build") || text.contains("./gradlew assemble") || Regex(
      "run:\\s*\\|[\\s\\S]*gradlew.*\\b(build|assemble)\\b",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    ).containsMatchIn(text)

    val hasTest = text.contains("./gradlew test") || Regex(
      "run:\\s*\\|[\\s\\S]*gradlew.*\\btest\\b",
      setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    ).containsMatchIn(text)

    assertTrue(hasBuild, "workflow missing Gradle build step")
    assertTrue(hasTest, "workflow missing Gradle test step")
  }
}
