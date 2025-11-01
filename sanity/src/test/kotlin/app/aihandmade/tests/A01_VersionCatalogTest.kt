package app.aihandmade.tests

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertTrue

class A01_VersionCatalogTest {
  @Test fun requiredAliasesExist() {
    val root = RepoRoot.locate()
    val text = Files.readString(root.resolve("gradle/libs.versions.toml"))
    listOf(
      "versions.kotlin", "versions.junit-jupiter",
      "junit-jupiter-api", "junit-jupiter-engine"
    ).forEach { key ->
      assertTrue(text.contains(key), "MISSING in libs.versions.toml: $key")
    }
  }
}
