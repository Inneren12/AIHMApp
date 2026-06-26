package app.aihandmade.core.color

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

/**
 * Conformance test for [deltaE2000] against the published 34-pair reference table from
 * Sharma, Wu & Dalal, "The CIEDE2000 Color-Difference Formula: Implementation Notes,
 * Supplementary Test Data, and Mathematical Observations" (Color Res. Appl., 2005).
 *
 * Reproducing this table to 4 decimals is the accepted proof that a CIEDE2000 implementation is
 * correct (it exercises every hue-wrap branch and the chroma-zero guards). Tolerance 1e-4 absorbs
 * the table's 4-decimal rounding and the `Float` storage of [Lab]; the formula itself is exact.
 */
class DeltaE2000SharmaTest {

    private data class Case(
        val l1: Float, val a1: Float, val b1: Float,
        val l2: Float, val a2: Float, val b2: Float,
        val expected: Double,
    )

    @TestFactory
    fun matchesSharmaReferenceTable(): List<DynamicTest> =
        SHARMA.mapIndexed { i, p ->
            dynamicTest("Sharma pair ${i + 1} -> ${p.expected}") {
                val got = deltaE2000(Lab(p.l1, p.a1, p.b1), Lab(p.l2, p.a2, p.b2))
                assertEquals(p.expected, got, TOLERANCE, "CIEDE2000 mismatch on Sharma pair ${i + 1}")
            }
        }

    private companion object {
        const val TOLERANCE = 1e-4

        val SHARMA = listOf(
            Case(50.0000f, 2.6772f, -79.7751f, 50.0000f, 0.0000f, -82.7485f, 2.0425),
            Case(50.0000f, 3.1571f, -77.2803f, 50.0000f, 0.0000f, -82.7485f, 2.8615),
            Case(50.0000f, 2.8361f, -74.0200f, 50.0000f, 0.0000f, -82.7485f, 3.4412),
            Case(50.0000f, -1.3802f, -84.2814f, 50.0000f, 0.0000f, -82.7485f, 1.0000),
            Case(50.0000f, -1.1848f, -84.8006f, 50.0000f, 0.0000f, -82.7485f, 1.0000),
            Case(50.0000f, -0.9009f, -85.5211f, 50.0000f, 0.0000f, -82.7485f, 1.0000),
            Case(50.0000f, 0.0000f, 0.0000f, 50.0000f, -1.0000f, 2.0000f, 2.3669),
            Case(50.0000f, -1.0000f, 2.0000f, 50.0000f, 0.0000f, 0.0000f, 2.3669),
            Case(50.0000f, 2.4900f, -0.0010f, 50.0000f, -2.4900f, 0.0009f, 7.1792),
            Case(50.0000f, 2.4900f, -0.0010f, 50.0000f, -2.4900f, 0.0010f, 7.1792),
            Case(50.0000f, 2.4900f, -0.0010f, 50.0000f, -2.4900f, 0.0011f, 7.2195),
            Case(50.0000f, 2.4900f, -0.0010f, 50.0000f, -2.4900f, 0.0012f, 7.2195),
            Case(50.0000f, -0.0010f, 2.4900f, 50.0000f, 0.0009f, -2.4900f, 4.8045),
            Case(50.0000f, -0.0010f, 2.4900f, 50.0000f, 0.0010f, -2.4900f, 4.8045),
            Case(50.0000f, -0.0010f, 2.4900f, 50.0000f, 0.0011f, -2.4900f, 4.7461),
            Case(50.0000f, 2.5000f, 0.0000f, 50.0000f, 0.0000f, -2.5000f, 4.3065),
            Case(50.0000f, 2.5000f, 0.0000f, 73.0000f, 25.0000f, -18.0000f, 27.1492),
            Case(50.0000f, 2.5000f, 0.0000f, 61.0000f, -5.0000f, 29.0000f, 22.8977),
            Case(50.0000f, 2.5000f, 0.0000f, 56.0000f, -27.0000f, -3.0000f, 31.9030),
            Case(50.0000f, 2.5000f, 0.0000f, 58.0000f, 24.0000f, 15.0000f, 19.4535),
            Case(50.0000f, 2.5000f, 0.0000f, 50.0000f, 3.1736f, 0.5854f, 1.0000),
            Case(50.0000f, 2.5000f, 0.0000f, 50.0000f, 3.2972f, 0.0000f, 1.0000),
            Case(50.0000f, 2.5000f, 0.0000f, 50.0000f, 1.8634f, 0.5757f, 1.0000),
            Case(50.0000f, 2.5000f, 0.0000f, 50.0000f, 3.2592f, 0.3350f, 1.0000),
            Case(60.2574f, -34.0099f, 36.2677f, 60.4626f, -34.1751f, 39.4387f, 1.2644),
            Case(63.0109f, -31.0961f, -5.8663f, 62.8187f, -29.7946f, -4.0864f, 1.2630),
            Case(61.2901f, 3.7196f, -5.3901f, 61.4292f, 2.2480f, -4.9620f, 1.8731),
            Case(35.0831f, -44.1164f, 3.7933f, 35.0232f, -40.0716f, 1.5901f, 1.8645),
            Case(22.7233f, 20.0904f, -46.6940f, 23.0331f, 14.9730f, -42.5619f, 2.0373),
            Case(36.4612f, 47.8580f, 18.3852f, 36.2715f, 50.5065f, 21.2231f, 1.4146),
            Case(90.8027f, -2.0831f, 1.4410f, 91.1528f, -1.6435f, 0.0447f, 1.4441),
            Case(90.9257f, -0.5406f, -0.9208f, 88.6381f, -0.8985f, -0.7239f, 1.5381),
            Case(6.7747f, -0.2908f, -2.4247f, 5.8714f, -0.0985f, -2.2286f, 0.6377),
            Case(2.0776f, 0.0795f, -1.1350f, 0.9033f, -0.0636f, -0.5514f, 0.9082),
        )
    }
}
