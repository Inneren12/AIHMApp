package app.aihandmade.tests

import java.nio.file.*

object RepoRoot {
  fun locate(): Path {
    var p = Paths.get("").toAbsolutePath()
    var steps = 0
    while (p != null && steps < 10) {
      if (Files.exists(p.resolve("settings.gradle.kts"))) return p
      p = p.parent; steps++
    }
    throw IllegalStateException("Cannot find repo root (settings.gradle.kts)")
  }
}
