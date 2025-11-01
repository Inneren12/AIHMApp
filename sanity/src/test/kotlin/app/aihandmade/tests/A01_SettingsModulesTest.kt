package app.aihandmade.tests

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertTrue

class A01_SettingsModulesTest {
  @Test fun modulesIncluded() {
    val root = RepoRoot.locate()
    val text = Files.readString(root.resolve("settings.gradle.kts"))
    fun mustContain(sn:String) = assertTrue(text.contains(sn),
      "MISSING in settings.gradle.kts: $sn")
    mustContain("""include(":app")""")
    mustContain("""include(":core")""")
    mustContain("""include(":logging")""")
    mustContain("""include(":storage")""")
    mustContain("""include(":export")""")
    // sanity module должен быть включён (A1)
    assertTrue(text.contains("""include(":sanity")"""),
      "MISSING include(:sanity) for test module")
  }
}
