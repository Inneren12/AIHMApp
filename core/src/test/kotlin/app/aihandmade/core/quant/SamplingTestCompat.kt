package app.aihandmade.core.quant

import app.aihandmade.core.color.OkLab
import app.aihandmade.core.color.Srgb
import app.aihandmade.core.color.toOkLab as srgbToOkLabColor

internal fun Srgb.toOkLab(): OkLab = srgbToOkLabColor()
