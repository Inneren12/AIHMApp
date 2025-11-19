package app.aihandmade.core.decision

import app.aihandmade.core.decision.model.Complexity
import app.aihandmade.core.decision.model.PipelineBranch
import app.aihandmade.core.decision.model.SceneType
import app.aihandmade.core.decision.rules.DecisionEngineImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DecisionEngineTest {

    private val engine: DecisionEngine = DecisionEngineImpl()

    @Test
    fun testG0_PhysicalSizing() {
        val analyze = AnalyzeResult(
            srcWidth = 2000,
            srcHeight = 1000,
            previewWidth = 1000,
            previewHeight = 500,
            edgeDensity = 0.05,
            uniqueColorsQ = 1000,
            gradientSmoothness = 0.5,
            pixelationScore = 0.1,
            entropyScore = 0.4
        )

        val input = DecisionInput(
            sourceWidthPx = 2000,
            sourceHeightPx = 1000,
            physicalWidthInches = 10.0,
            fabricCount = 16.0,
            clampStitchesRange = 80..300
        )

        val plan = engine.buildBasePlan(analyze, input)

        assertEquals(160, plan.targetWidthStitches)
        assertEquals(80, plan.targetHeightStitches)
    }

    @Test
    fun testG0_AutoRange() {
        val analyze = AnalyzeResult(
            srcWidth = 2000,
            srcHeight = 1000,
            previewWidth = 1000,
            previewHeight = 500,
            edgeDensity = 0.05,
            uniqueColorsQ = 1000,
            gradientSmoothness = 0.5,
            pixelationScore = 0.1,
            entropyScore = 0.4
        )

        val input = DecisionInput(
            sourceWidthPx = 2000,
            sourceHeightPx = 1000
        )

        val plan = engine.buildBasePlan(analyze, input, DecisionParams(defaultPickLongSide = 180))

        assertEquals(180, plan.targetWidthStitches)
        assertEquals(90, plan.targetHeightStitches)
    }

    @Test
    fun testG1_PixelArt_ByPixelation() {
        val analyze = baseAnalyze(pixelationScore = 0.70, uniqueColorsQ = 500, edgeDensity = 0.05)
        val input = DecisionInput(sourceWidthPx = 1000, sourceHeightPx = 1000)

        val plan = engine.buildBasePlan(analyze, input)

        assertEquals(SceneType.PIXEL_ART, plan.sceneType)
    }

    @Test
    fun testG1_Photo_ByColors() {
        val analyze = baseAnalyze(pixelationScore = 0.10, uniqueColorsQ = 8000, edgeDensity = 0.08)
        val input = DecisionInput(sourceWidthPx = 1200, sourceHeightPx = 800)

        val plan = engine.buildBasePlan(analyze, input)

        assertEquals(SceneType.PHOTO, plan.sceneType)
    }

    @Test
    fun testG1_Discrete_FewColors_WithEdges() {
        val analyze = baseAnalyze(pixelationScore = 0.15, uniqueColorsQ = 1200, edgeDensity = 0.10)
        val input = DecisionInput(sourceWidthPx = 800, sourceHeightPx = 1200)

        val plan = engine.buildBasePlan(analyze, input)

        assertEquals(SceneType.DISCRETE, plan.sceneType)
    }

    @Test
    fun testG2_Complexity_Bands() {
        val input = DecisionInput(sourceWidthPx = 1000, sourceHeightPx = 1000)

        val simple = engine.buildBasePlan(baseAnalyze(edgeDensity = 0.02, entropyScore = 0.20), input)
        val medium = engine.buildBasePlan(baseAnalyze(edgeDensity = 0.08, entropyScore = 0.45), input)
        val complex = engine.buildBasePlan(baseAnalyze(edgeDensity = 0.18, entropyScore = 0.65), input)

        assertEquals(Complexity.SIMPLE, simple.complexity)
        assertEquals(Complexity.MEDIUM, medium.complexity)
        assertEquals(Complexity.COMPLEX, complex.complexity)
    }

    @Test
    fun testG3_Pipeline_ForcePixel() {
        val analyze = baseAnalyze(pixelationScore = 0.10, uniqueColorsQ = 1000, edgeDensity = 0.05)
        val input = DecisionInput(sourceWidthPx = 1000, sourceHeightPx = 1000, forcePixelStyle = true)

        val plan = engine.buildBasePlan(analyze, input)

        assertEquals(PipelineBranch.PIXEL_PIPE, plan.pipeline)
        assertTrue(plan.gates.g3PixelEnabled)
    }

    @Test
    fun testBuildBasePlan_ComposesReasons() {
        val analyze = baseAnalyze(pixelationScore = 0.60, uniqueColorsQ = 1000, edgeDensity = 0.07)
        val input = DecisionInput(sourceWidthPx = 1000, sourceHeightPx = 1000)

        val plan = engine.buildBasePlan(analyze, input)

        assertFalse(plan.reasons.isEmpty())
        assertTrue(plan.reasons.any { it.startsWith("G0") })
        assertTrue(plan.reasons.any { it.startsWith("G1") })
        assertTrue(plan.reasons.any { it.startsWith("G2") })
        assertTrue(plan.reasons.any { it.startsWith("G3") })
    }

    private fun baseAnalyze(
        srcWidth: Int = 1000,
        srcHeight: Int = 1000,
        previewWidth: Int = 500,
        previewHeight: Int = 500,
        edgeDensity: Double = 0.05,
        uniqueColorsQ: Int = 1000,
        gradientSmoothness: Double = 0.5,
        pixelationScore: Double = 0.1,
        entropyScore: Double = 0.4
    ): AnalyzeResult = AnalyzeResult(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        previewWidth = previewWidth,
        previewHeight = previewHeight,
        edgeDensity = edgeDensity,
        uniqueColorsQ = uniqueColorsQ,
        gradientSmoothness = gradientSmoothness,
        pixelationScore = pixelationScore,
        entropyScore = entropyScore
    )
}
