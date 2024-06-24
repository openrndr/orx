@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.fx_luma_opacity
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Luma map ")
class LumaOpacity : Filter1to1(mppFilterShader(fx_luma_opacity, "luma-opacity")) {
    @DoubleParameter("foreground luma",0.0, 1.0)
    var foregroundLuma: Double by parameters

    @DoubleParameter("background luma", 0.0,1.0)
    var backgroundLuma: Double by parameters

    @DoubleParameter("background opacity", 0.0, 1.0, order = 0)
    var backgroundOpacity: Double by parameters

    @DoubleParameter("foreground opacity", 0.0, 1.0, order = 1)
    var foregroundOpacity: Double by parameters

    init {
        foregroundLuma = 1.0
        backgroundLuma = 0.0
        foregroundOpacity = 1.0
        backgroundOpacity = 0.0
    }
}