package app.aihandmade.core.decision

import app.aihandmade.core.decision.model.Complexity
import app.aihandmade.core.decision.model.PipelineBranch
import app.aihandmade.core.decision.model.ProcessingPlan
import app.aihandmade.core.decision.model.SceneType

/**
 * Результаты анализа из S2 (упрощённые для гейтов).
 */
data class AnalyzeResult(
    val srcWidth: Int,
    val srcHeight: Int,
    val previewWidth: Int,
    val previewHeight: Int,
    val edgeDensity: Double,
    val uniqueColorsQ: Int,
    val gradientSmoothness: Double,
    val pixelationScore: Double,
    val entropyScore: Double
)

/**
 * Пользовательские/системные входы для планирования.
 */
data class DecisionInput(
    val sourceWidthPx: Int,
    val sourceHeightPx: Int,
    val physicalWidthInches: Double? = null,
    val fabricCount: Double? = null,
    val forcePixelStyle: Boolean = false,
    val recommendedLongSideRange: IntRange = 120..240,
    val clampStitchesRange: IntRange = 80..300
)

/**
 * Интерфейс движка принятия решения.
 */
interface DecisionEngine {
    /**
     * Построить первичный план обработки (без палитры/дизеринга).
     * @param analyze результаты S2
     * @param input пользовательские/системные опции
     * @param params параметризованные пороги
     */
    fun buildBasePlan(
        analyze: AnalyzeResult,
        input: DecisionInput,
        params: DecisionParams = DecisionParams()
    ): ProcessingPlan
}
