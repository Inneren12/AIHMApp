package app.aihandmade.tests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class A03_AppModuleSettingsTest {
  @Test
  fun androidXEnabledInGradleProperties() {
    val root = RepoRoot.locate()
    val text = Files.readString(root.resolve("gradle.properties"))

    val hasAndroidXFlag = text
      .lineSequence()
      .map { it.trim() }
      .any { it.equals("android.useAndroidX=true", ignoreCase = false) }

    assertTrue(
      hasAndroidXFlag,
      "android.useAndroidX must be true in gradle.properties",
    )
  }
}
