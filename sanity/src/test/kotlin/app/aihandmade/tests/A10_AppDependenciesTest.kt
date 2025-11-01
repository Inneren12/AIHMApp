package app.aihandmade.tests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class A10_AppDependenciesTest {
  @Test
  fun appIncludesRequiredAndroidxDependencies() {
    val root = RepoRoot.locate()
    val buildGradle = Files.readString(root.resolve("app/build.gradle.kts"))

    assertTrue(
      buildGradle.contains("androidx.lifecycle:lifecycle-viewmodel-compose"),
      "Missing lifecycle-viewmodel-compose dependency",
    )

    assertTrue(
      buildGradle.contains("androidx.exifinterface:exifinterface"),
      "Missing androidx.exifinterface dependency",
    )
  }
}
