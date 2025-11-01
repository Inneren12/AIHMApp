package app.aihandmade.tests

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertTrue
import kotlin.text.RegexOption

class A02_AppGradleComposeTest {
  @Test
  fun composeAndJvmTargetConfigured() {
    val root = RepoRoot.locate()
    val g = Files.readString(root.resolve("app/build.gradle.kts"))

    val hasComposeFeature = Regex(
      """buildFeatures\s*\{\s*[^}]*compose\s*=\s*true""",
      RegexOption.DOTALL,
    ).containsMatchIn(g)
    val hasComposeOptions = Regex(
      """composeOptions\s*\{\s*[^}]*kotlinCompilerExtensionVersion""",
      RegexOption.DOTALL,
    ).containsMatchIn(g)
    val hasJvmTarget = Regex("""jvmTarget\s*=\s*[\"']17[\"']""")
      .containsMatchIn(g)

    assertTrue(hasComposeFeature, "MISSING compose buildFeature in :app")
    assertTrue(hasComposeOptions, "MISSING composeOptions in :app")
    assertTrue(hasJvmTarget, "MISSING jvmTarget=17 in :app")
  }
}
