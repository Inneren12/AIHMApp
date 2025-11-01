package app.aihandmade.tests

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class A01_VersionCatalogTest {
  @Test
  fun requiredAliasesExist() {
    val root = RepoRoot.locate()
    val text = Files.readString(root.resolve("gradle/libs.versions.toml"))

    listOf(
      "kotlin =",
      "junit-jupiter =",
      "junit-jupiter-api",
      "junit-jupiter-engine",
    ).forEach { key ->
      assertTrue(text.contains(key), "MISSING in libs.versions.toml: $key")
    }
  }
}
