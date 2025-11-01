package app.aihandmade.tests

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertTrue

class A02_AppGradleComposeTest {
  @Test fun composeAndJvmTargetConfigured() {
    val root = RepoRoot.locate()
    val g = Files.readString(root.resolve("app/build.gradle.kts"))
    assertTrue(g.contains("buildFeatures { compose = true }"),
      "MISSING compose buildFeature in :app")
    assertTrue(g.contains("composeOptions"),
      "MISSING composeOptions in :app")
    assertTrue(g.contains("jvmTarget = \"17\""),
      "MISSING jvmTarget=17 in :app")
  }
}
