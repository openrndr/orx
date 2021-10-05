package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.extra.fx.fx_luma_threshold
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Luma threshold ")
class LumaThreshold : Filter(mppFilterShader(fx_luma_threshold, "luma-threshold")) {
    @DoubleParameter("threshold value", 0.0, 1.0)
    var threshold: Double by parameters

    @ColorParameter("foreground color")
    var foreground: ColorRGBa by parameters

    @ColorParameter("background color")
    var background: ColorRGBa by parameters

    @DoubleParameter("background opacity", 0.0, 1.0)
    var backgroundOpacity: Double by parameters

    @DoubleParameter("foreground opacity", 0.0, 1.0)
    var foregroundOpacity: Double by parameters

    init {
        threshold = 0.5
        foreground = ColorRGBa.WHITE
        background = ColorRGBa.BLACK
        foregroundOpacity = 1.0
        backgroundOpacity = 1.0
    }
}