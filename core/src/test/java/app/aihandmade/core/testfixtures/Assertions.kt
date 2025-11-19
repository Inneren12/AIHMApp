package app.aihandmade.core.testfixtures

import kotlin.math.abs
import org.junit.Assert.assertTrue

fun assertClose(actual: Double, expected: Double, tol: Double = 1e-3, message: String = "") {
    assertTrue("$message expected=$expected actual=$actual", abs(actual - expected) <= tol)
}

fun assertClose(actual: Float, expected: Float, tol: Float = 1e-3f, message: String = "") {
    assertTrue("$message expected=$expected actual=$actual", abs(actual - expected) <= tol)
}
