package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.extra.fx.fx_set_background
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Set background")
class SetBackground : Filter(mppFilterShader(fx_set_background, "set-background")) {
    @ColorParameter("background color")
    var background: ColorRGBa by parameters

    @DoubleParameter("background opacity", 0.0, 1.0)
    var backgroundOpacity: Double by parameters

    init {
        background = ColorRGBa.GRAY
        backgroundOpacity = 1.0
    }
}
