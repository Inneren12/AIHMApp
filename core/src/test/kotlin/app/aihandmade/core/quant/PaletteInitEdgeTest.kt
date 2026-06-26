package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.Srgb
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Edge-case tests for [initPalette] anchor perceptual de-duplication.
 *
 * Anchors are gated by both sample index AND CIEDE2000 spread — a near-duplicate anchor (different
 * index, but dE2000 < S_MIN from an already-accepted anchor) must be silently dropped, preserving
 * the priority order black → white → neutral.
 */
class PaletteInitEdgeTest {

    /**
     * Constructs a SampleSet where black (pos 0, OKLab L=0.10) and the neutral candidate
     * (pos 1, OKLab L=0.15) are different sample indices but dE2000 ≈ 1.25 in CIE-Lab — well
     * below S_MIN=3.5. Without perceptual gating both would appear as anchors.
     *
     * With n=3 and DARK_LIGHT_FRACTION=20: fraction=max(3/20,1)=1.
     *   black  = pos 0 (min L, min chroma)
     *   white  = pos 2 (max L, min chroma)
     *   neutral = pos 1 or pos 2 (|0.15−0.55|=|0.95−0.55|=0.40, tie → smaller index → pos 1)
     *
     * So three distinct indices are nominated; pos 0 and pos 1 are perceptually indistinct.
     * The spread gate must drop pos 1.
     */
    @Test
    fun anchorsArePerceptuallyDeduplicated() {
        val s = SampleSet(
            index = intArrayOf(0, 1, 2),
            L = floatArrayOf(0.10f, 0.15f, 0.95f),
            a = floatArrayOf(0f, 0f, 0f),
            b = floatArrayOf(0f, 0f, 0f),
            weight = floatArrayOf(1f, 1f, 1f),
            sourceWidth = 3,
            sourceHeight = 1,
        )

        val p = initPalette(s, k0 = 14)

        // Every anchor pair must be >= S_MIN apart.
        for (i in 0 until p.anchorCount) {
            val labI = OkLab(p.L[i], p.a[i], p.b[i]).toLinearRgb().toLab()
            for (j in i + 1 until p.anchorCount) {
                val labJ = OkLab(p.L[j], p.a[j], p.b[j]).toLinearRgb().toLab()
                val de = deltaE2000(labI, labJ)
                assertTrue(
                    de >= S_MIN,
                    "anchor[$i] vs anchor[$j]: dE2000=$de < S_MIN=$S_MIN — perceptual de-dup failed"
                )
            }
        }
    }

    /**
     * Verify that the near-duplicate anchor candidates pos 0 and pos 1 in the test above are indeed
     * closer than S_MIN in dE2000, confirming the test is a genuine near-duplicate scenario.
     */
    @Test
    fun nearDuplicatePairIsIndeedBelowSMin() {
        val lab0 = OkLab(0.10f, 0f, 0f).toLinearRgb().toLab()
        val lab1 = OkLab(0.15f, 0f, 0f).toLinearRgb().toLab()
        val de = deltaE2000(lab0, lab1)
        assertTrue(
            de < S_MIN,
            "Expected dE2000($lab0, $lab1) < $S_MIN but got $de — test premise is invalid"
        )
    }

    /**
     * Tinted candidate (a=b=0.019) passes the old per-component guard (each value ≤ 0.02) but its
     * radial chroma sqrt(0.019²+0.019²) ≈ 0.0269 > NEUTRAL_CHROMA_MAX. The truly neutral pos 2
     * (a=b=0) is slightly farther from L=0.55 but must be selected under the correct radial filter.
     *
     * n=4, fraction=1:
     *   pos 0  L=0.05 a=0     b=0     → black
     *   pos 1  L=0.55 a=0.019 b=0.019 → closest to L target, tinted; radial chroma > cap
     *   pos 2  L=0.56 a=0     b=0     → truly neutral, |0.56−0.55|=0.01
     *   pos 3  L=0.98 a=0     b=0     → white
     */
    @Test
    fun neutralCandidateUsesRadialChromaNotPerComponentThreshold() {
        val s = SampleSet(
            index = intArrayOf(0, 1, 2, 3),
            L = floatArrayOf(0.05f, 0.55f, 0.56f, 0.98f),
            a = floatArrayOf(0f, 0.019f, 0f, 0f),
            b = floatArrayOf(0f, 0.019f, 0f, 0f),
            weight = floatArrayOf(1f, 1f, 1f, 1f),
            sourceWidth = 4,
            sourceHeight = 1,
        )

        val p = initPalette(s, k0 = 14)

        // The tinted candidate must NOT be among the anchors.
        for (i in 0 until p.anchorCount) {
            assertTrue(
                !(p.a[i] == 0.019f && p.b[i] == 0.019f),
                "tinted candidate (a=b=0.019, radial chroma > NEUTRAL_CHROMA_MAX) must not be anchor[$i]"
            )
        }
        // The radially neutral candidate (pos 2, L=0.56) must be the neutral anchor.
        val trueNeutralIsAnchor = (0 until p.anchorCount).any { p.L[it] == 0.56f && p.a[it] == 0f && p.b[it] == 0f }
        assertTrue(trueNeutralIsAnchor, "radially neutral candidate (pos 2, L=0.56) must be chosen as anchor")
    }

    /**
     * A rejected anchor candidate (neutral, pos 1: near-dup of black, pos 0) given a very high
     * weight must not re-enter the palette as a fill. Fills are restricted to positions that were
     * never nominated as anchors, whether or not they were subsequently accepted.
     *
     * Layout (n=5, fraction=1):
     *   pos 0  L=0.10 a=0    b=0    weight=1    → black (accepted)
     *   pos 1  L=0.15 a=0    b=0    weight=1000 → neutral (nominated, rejected: dE2000≈1.25 from black)
     *   pos 2  L=0.95 a=0    b=0    weight=1    → white (accepted)
     *   pos 3  L=0.50 a=0.3  b=0    weight=2    → regular fill candidate
     *   pos 4  L=0.70 a=0    b=0.3  weight=2    → regular fill candidate
     *
     * neutral tie-break: |0.15−0.55|=|0.95−0.55|=0.40 → smaller index → pos 1.
     */
    @Test
    fun rejectedAnchorWithHighWeightDoesNotAppearAsFill() {
        val s = SampleSet(
            index = intArrayOf(0, 1, 2, 3, 4),
            L = floatArrayOf(0.10f, 0.15f, 0.95f, 0.50f, 0.70f),
            a = floatArrayOf(0f, 0f, 0f, 0.3f, 0f),
            b = floatArrayOf(0f, 0f, 0f, 0f, 0.3f),
            weight = floatArrayOf(1f, 1000f, 1f, 2f, 2f),
            sourceWidth = 5,
            sourceHeight = 1,
        )

        val p = initPalette(s, k0 = 14)

        // pos 1 (OKLab 0.15, 0, 0) must not appear anywhere in the palette.
        for (i in 0 until p.size) {
            assertTrue(
                !(p.L[i] == 0.15f && p.a[i] == 0f && p.b[i] == 0f),
                "rejected anchor candidate (pos 1) must not appear in palette at index $i"
            )
        }
    }

    @Test
    fun anchorsAlwaysIncludedEvenWhenk0Small() {
        val w = 6; val h = 6
        val pixels = IntArray(w * h) { i -> Srgb.of(i * 7, i * 7, i * 7).argb }
        pixels[0] = Srgb.of(0, 0, 0).argb; pixels[35] = Srgb.of(255, 255, 255).argb
        val s = samplePixels(pixels, w, h, targetSamples = w * h, seed = 1337L)
        val p = initPalette(s, k0 = 1)
        assertEquals(p.anchorCount, p.size, "only anchors when k0 < anchorCount")
        assertTrue(p.size >= p.anchorCount)
    }

    @Test
    fun singleSampleProducesValidPalette() {
        val s = samplePixels(intArrayOf(Srgb.of(128, 128, 128).argb), 1, 1, 1, 1337L)
        val p = initPalette(s)
        assertTrue(p.size >= 1)
        assertTrue(p.anchorCount in 1..3)
        assertEquals(p.size, p.L.size)
        assertEquals(p.size, p.a.size)
        assertEquals(p.size, p.b.size)
    }
}
