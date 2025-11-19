package app.aihandmade.core.decision.rules

import app.aihandmade.core.decision.AnalyzeResult
import app.aihandmade.core.decision.DecisionEngine
import app.aihandmade.core.decision.DecisionInput
import app.aihandmade.core.decision.DecisionParams
import app.aihandmade.core.decision.model.Complexity
import app.aihandmade.core.decision.model.PipelineBranch
import app.aihandmade.core.decision.model.ProcessingPlan
import app.aihandmade.core.decision.model.SceneType
import app.aihandmade.core.decision.util.MathUtil
import kotlin.math.max
import kotlin.math.min

class DecisionEngineImpl : DecisionEngine {

    override fun buildBasePlan(
        analyze: AnalyzeResult,
        input: DecisionInput,
        params: DecisionParams
    ): ProcessingPlan {
        validateInput(analyze, input)

        val reasons = mutableListOf<String>()

        val g0Result = runG0(analyze, input, params, reasons)
        val sceneType = runG1(analyze, params, reasons)
        val complexity = runG2(analyze, params, reasons)
        val pipelineResult = runG3(input, sceneType, reasons)

        return ProcessingPlan(
            targetWidthStitches = g0Result.width,
            targetHeightStitches = g0Result.height,
            sceneType = sceneType,
            complexity = complexity,
            pipeline = pipelineResult.pipeline,
            reasons = reasons.toList(),
            gates = app.aihandmade.core.decision.model.GateSnapshot(
                g0ComputedFromPhysical = g0Result.fromPhysical,
                g0Range = input.clampStitchesRange,
                g1 = sceneType,
                g2 = complexity,
                g3PixelEnabled = pipelineResult.pixelEnabled
            )
        )
    }

    private fun validateInput(analyze: AnalyzeResult, input: DecisionInput) {
        require(analyze.srcWidth > 0 && analyze.srcHeight > 0) { "AnalyzeResult source sizes must be positive" }
        require(input.sourceWidthPx > 0 && input.sourceHeightPx > 0) { "DecisionInput source sizes must be positive" }
        require(analyze.srcWidth * analyze.srcHeight > 0) { "AnalyzeResult aspect ratio must be > 0" }
        require(input.sourceWidthPx * input.sourceHeightPx > 0) { "DecisionInput aspect ratio must be > 0" }
    }

    private data class G0Result(val width: Int, val height: Int, val fromPhysical: Boolean)

    private data class PipelineResult(val pipeline: PipelineBranch, val pixelEnabled: Boolean)

    private fun runG0(
        analyze: AnalyzeResult,
        input: DecisionInput,
        params: DecisionParams,
        reasons: MutableList<String>
    ): G0Result {
        val clampRange = input.clampStitchesRange
        val srcMin = min(input.sourceWidthPx, input.sourceHeightPx).toDouble()
        val srcMax = max(input.sourceWidthPx, input.sourceHeightPx).toDouble()

        val fromPhysical = input.physicalWidthInches != null && input.fabricCount != null

        val rawLongSide: Int
        val pickLabel: String
        if (fromPhysical) {
            rawLongSide = MathUtil.roundToNearestInt(input.physicalWidthInches!! * input.fabricCount!!)
            pickLabel = "physical ${formatDouble(input.physicalWidthInches)}in * ${formatDouble(input.fabricCount)}"
        } else {
            val clampedDefault = MathUtil.clamp(params.defaultPickLongSide, clampRange)
            rawLongSide = MathUtil.clamp(clampedDefault, input.recommendedLongSideRange)
            pickLabel = "auto default=${params.defaultPickLongSide} in ${input.recommendedLongSideRange}"
        }

        val clampedLong = MathUtil.clamp(rawLongSide, clampRange)
        val rawShort = MathUtil.roundToNearestInt(clampedLong * (srcMin / srcMax))

        val roundedLong = MathUtil.roundToMultiple(clampedLong, params.roundToMultiple)
        val roundedShort = MathUtil.roundToMultiple(rawShort, params.roundToMultiple)

        val orientedWidth: Int
        val orientedHeight: Int
        if (input.sourceWidthPx >= input.sourceHeightPx) {
            orientedWidth = roundedLong
            orientedHeight = roundedShort
        } else {
            orientedWidth = roundedShort
            orientedHeight = roundedLong
        }

        val (finalWidth, finalHeight) = MathUtil.clampEach(orientedWidth, orientedHeight, clampRange)

        reasons.add(
            "G0 size: ${finalWidth}x${finalHeight} stitches (via ${if (fromPhysical) "physical" else "auto"}, clamp=${clampRange}, pick=${pickLabel}, roundTo=${params.roundToMultiple})"
        )

        return G0Result(finalWidth, finalHeight, fromPhysical)
    }

    private fun runG1(
        analyze: AnalyzeResult,
        params: DecisionParams,
        reasons: MutableList<String>
    ): SceneType {
        val pixelation = analyze.pixelationScore
        val uniqueColors = analyze.uniqueColorsQ
        val edge = analyze.edgeDensity
        val entropy = analyze.entropyScore

        val sceneType: SceneType
        val reason: String
        when {
            pixelation >= params.pixelationHigh -> {
                sceneType = SceneType.PIXEL_ART
                reason = "G1 pixelation high (pixelation=${formatDouble(pixelation)} >= ${formatDouble(params.pixelationHigh)})"
            }

            uniqueColors >= params.uniqueColorsPhotoMin -> {
                sceneType = SceneType.PHOTO
                reason = "G1 many unique colors (unique=${uniqueColors} >= ${params.uniqueColorsPhotoMin})"
            }

            uniqueColors <= params.uniqueColorsDiscreteMax && edge >= params.edgeForDiscreteMin -> {
                sceneType = SceneType.DISCRETE
                reason = "G1 few colors & enough edges (colors=${uniqueColors} <= ${params.uniqueColorsDiscreteMax}, edge=${formatDouble(edge)} >= ${formatDouble(params.edgeForDiscreteMin)})"
            }

            else -> {
                val edgeMedium = edge in params.edgeLow..params.edgeHigh
                if (entropy >= params.entropyHigh && edgeMedium) {
                    sceneType = SceneType.PHOTO
                    reason = "G1 fallback: entropy high & medium edges (entropy=${formatDouble(entropy)}, edge=${formatDouble(edge)})"
                } else {
                    sceneType = SceneType.DISCRETE
                    reason = "G1 fallback: default to discrete (entropy=${formatDouble(entropy)}, edge=${formatDouble(edge)})"
                }
            }
        }

        reasons.add(reason)
        return sceneType
    }

    private fun runG2(
        analyze: AnalyzeResult,
        params: DecisionParams,
        reasons: MutableList<String>
    ): Complexity {
        val edge = analyze.edgeDensity
        val entropy = analyze.entropyScore
        val complexity = when {
            edge <= params.edgeLow && entropy <= params.entropyLow -> Complexity.SIMPLE
            edge >= params.edgeHigh || entropy >= params.entropyHigh -> Complexity.COMPLEX
            else -> Complexity.MEDIUM
        }

        reasons.add(
            "G2 complexity: edge=${formatDouble(edge)}, entropy=${formatDouble(entropy)} -> ${complexity.name}"
        )
        return complexity
    }

    private fun runG3(
        input: DecisionInput,
        sceneType: SceneType,
        reasons: MutableList<String>
    ): PipelineResult {
        val pixelEnabled = input.forcePixelStyle || sceneType == SceneType.PIXEL_ART
        val pipeline = when {
            pixelEnabled -> PipelineBranch.PIXEL_PIPE
            sceneType == SceneType.PHOTO -> PipelineBranch.PHOTO_PIPE
            else -> PipelineBranch.DISCRETE_PIPE
        }

        reasons.add(
            "G3 pipeline: forcePixel=${input.forcePixelStyle}, scene=${sceneType.name}, pixel=${pixelEnabled} -> ${pipeline.name}"
        )

        return PipelineResult(pipeline, pixelEnabled)
    }

    private fun formatDouble(value: Double?): String = String.format("%.3f", value)
}
