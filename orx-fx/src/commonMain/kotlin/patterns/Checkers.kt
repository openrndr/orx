package org.openrndr.extra.fx.patterns

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.fx_checkers
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Checkers pattern")
class Checkers : Filter1to1(mppFilterShader(fx_checkers, "checkers")) {
    var background: ColorRGBa by parameters
    var foreground: ColorRGBa by parameters

    @DoubleParameter("size", 0.0, 1.0)
    var size: Double by parameters

    @DoubleParameter("opacity", 0.0, 1.0)
    var opacity: Double by parameters

    init {
        size = 1.0 / 64.0
        opacity = 1.0
        foreground = ColorRGBa.WHITE.shade(0.9)
        background = ColorRGBa.WHITE.shade(0.8)
    }
}