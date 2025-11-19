package app.aihandmade.core.decision.model

data class ProcessingPlan(
    val targetWidthStitches: Int,
    val targetHeightStitches: Int,
    val sceneType: SceneType,
    val complexity: Complexity,
    val pipeline: PipelineBranch,
    val reasons: List<String>,
    val gates: GateSnapshot
)

data class GateSnapshot(
    val g0ComputedFromPhysical: Boolean,
    val g0Range: IntRange,
    val g1: SceneType,
    val g2: Complexity,
    val g3PixelEnabled: Boolean
)
