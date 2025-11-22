package com.handmade.engine.domain

import kotlin.math.absoluteValue
import kotlin.random.Random

enum class SceneType {
    PHOTO,
    ILLUSTRATION,
    PORTRAIT,
    UNKNOWN,
}

enum class CraftType {
    CROSS_STITCH,
    EMBROIDERY,
    NEEDLEPOINT,
}

enum class Branch {
    PHOTO_CONTINUOUS,
    GRAPHIC_CLEAN,
    MINIMALIST,
}

enum class DitheringMode {
    NONE,
    FLOYD_STEINBERG,
    ORDERED,
}

enum class QuantAlgo {
    MEDIAN_CUT,
    K_MEANS,
    OCTREE,
}

enum class SimplifyPreset {
    REALISTIC,
    CLEAN_EDGES,
    BOLD_SHAPES,
}

data class ImageRef(val id: String)

data class SourceImage(
    val original: ImageRef,
    val preview: ImageRef,
    val width: Int,
    val height: Int,
)

data class PatternSpec(
    val sceneType: SceneType,
    val craftType: CraftType,
    val branch: Branch,
    val gridWidth: Int,
    val gridHeight: Int,
    val paletteLimit: Int,
    val dithering: DitheringMode,
    val quantAlgo: QuantAlgo,
    val simplifyPreset: SimplifyPreset,
)

data class ProcessingPlan(
    val spec: PatternSpec,
    val notes: Map<String, String> = emptyMap(),
)

data class UserOverrides(
    val targetPalette: Int? = null,
    val requestedGridWidth: Int? = null,
    val requestedGridHeight: Int? = null,
    val preferredDithering: DitheringMode? = null,
    val simplifyPreset: SimplifyPreset? = null,
)

data class AnalyzeResult(
    val source: SourceImage? = null,
    val metadata: Map<String, String> = emptyMap(),
)

data class MaskSet(
    val masks: Map<String, ImageRef> = emptyMap(),
)

typealias ThreadColorId = String

data class PatternCell(
    val colorId: ThreadColorId,
    val isBackstitch: Boolean = false,
)

data class PatternGrid(
    val width: Int,
    val height: Int,
    val cells: List<PatternCell>,
)

data class QualityReport(
    val qColor: Double,
    val qEdge: Double,
    val qIslands: Double,
    val qTotal: Double,
    val details: Map<String, String> = emptyMap(),
)

interface QualityEvaluator {
    fun evaluate(plan: ProcessingPlan, analyze: AnalyzeResult): QualityReport
}

fun generateImageRef(label: String): ImageRef = ImageRef("$label-${Random.nextLong().absoluteValue}")
