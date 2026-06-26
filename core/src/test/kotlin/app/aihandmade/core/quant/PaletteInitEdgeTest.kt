package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.deltaE2000
import app.aihandmade.core.color.toLab
import app.aihandmade.core.color.toLinearRgb
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
}
