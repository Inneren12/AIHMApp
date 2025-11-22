package com.handmade.engine.decision

import com.handmade.engine.domain.AnalyzeResult
import com.handmade.engine.domain.Branch
import com.handmade.engine.domain.CraftType
import com.handmade.engine.domain.DitheringMode
import com.handmade.engine.domain.MaskSet
import com.handmade.engine.domain.PatternSpec
import com.handmade.engine.domain.ProcessingPlan
import com.handmade.engine.domain.QuantAlgo
import com.handmade.engine.domain.SceneType
import com.handmade.engine.domain.SimplifyPreset
import com.handmade.engine.domain.UserOverrides

interface Gate {
    fun apply(input: EngineInput): EngineOutput
}

data class EngineInput(
    val analysis: AnalyzeResult,
    val masks: MaskSet,
    val craftType: CraftType,
    val overrides: UserOverrides? = null,
)

data class EngineOutput(
    val candidates: List<ProcessingPlan>,
)

data class PlanContext(
    val analysis: AnalyzeResult,
    val masks: MaskSet,
    val craftType: CraftType,
    val overrides: UserOverrides?,
    val basePlans: List<ProcessingPlan>,
)

data class ScoredPlan(
    val plan: ProcessingPlan,
    val score: Double,
    val reason: String? = null,
)

interface MlPlanAdvisor {
    fun suggestPlans(context: PlanContext): List<ScoredPlan>
}

class DecisionEngineImpl(
    private val gates: List<Gate> = emptyList(),
    private val mlAdvisor: MlPlanAdvisor? = null,
) {
    fun run(input: EngineInput): EngineOutput {
        val basePlan = ProcessingPlan(
            spec = PatternSpec(
                sceneType = SceneType.PHOTO,
                craftType = input.craftType,
                branch = Branch.PHOTO_CONTINUOUS,
                gridWidth = 240,
                gridHeight = 160,
                paletteLimit = 32,
                dithering = DitheringMode.FLOYD_STEINBERG,
                quantAlgo = QuantAlgo.MEDIAN_CUT,
                simplifyPreset = SimplifyPreset.REALISTIC,
            ),
            notes = mapOf(
                "mode" to "AUTO",
                "quality" to "quality-first baseline",
            ),
        )

        val gatedOutput = gates.fold(EngineOutput(listOf(basePlan))) { _, gate ->
            gate.apply(input)
        }

        mlAdvisor?.let { advisor ->
            val scoredPlans = advisor.suggestPlans(
                PlanContext(
                    analysis = input.analysis,
                    masks = input.masks,
                    craftType = input.craftType,
                    overrides = input.overrides,
                    basePlans = gatedOutput.candidates,
                ),
            )
            val best = scoredPlans.maxByOrNull { it.score }
            if (best != null) {
                return EngineOutput(listOf(best.plan))
            }
        }

        return gatedOutput
    }
}
