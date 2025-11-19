package app.aihandmade.core.masks

/** Collection of normalized masks computed during analysis. */
data class MaskSet(
    val width: Int,
    val height: Int,
    val edge: FloatArray,
    val flat: FloatArray,
    val texture: FloatArray,
    val skin: FloatArray? = null,
    val sky: FloatArray? = null
)
