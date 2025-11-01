package app.aihandmade.tests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class A01_SettingsModulesTest {
  @Test
  fun modulesIncluded() {
    val root = RepoRoot.locate()
    val text = Files.readString(root.resolve("settings.gradle.kts"))

    val includes = Regex("include\\(([^)]+)\\)")
      .findAll(text)
      .flatMap { match ->
        match.groupValues[1]
          .split(',')
          .map { it.trim().trim('"', '\'') }
          .filter { it.isNotEmpty() }
      }
      .toSet()

    listOf("app", "core", "logging", "storage", "export").forEach { module ->
      assertTrue(
        Files.exists(root.resolve(module)),
        "Missing module directory: $module",
      )
    }

    assertTrue(
      ":sanity" in includes,
      "MISSING include(:sanity) for test module",
    )
  }
}
