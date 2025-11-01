package app.aihandmade.tests

import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertTrue

class A08_ImageDecoderFileTest {
  @Test fun imageDecoderExists() {
    val root = RepoRoot.locate()
    val f = root.resolve("app/src/main/java/app/aihandmade/img/ImageDecoder.kt")
    assertTrue(Files.exists(f), "MISSING: app/img/ImageDecoder.kt")
  }
}
