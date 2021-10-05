package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.extra.fx.fx_color_mix
import org.openrndr.extra.fx.fx_color_tint
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description

class ColorMix : Filter(mppFilterShader(fx_color_mix, "color-mix"))

@Description("Tint")
class ColorTint : Filter(mppFilterShader(fx_color_tint, "color-tint")) {
    @ColorParameter("tint")
    var tint: ColorRGBa by parameters

    init {
        tint = ColorRGBa.PINK
    }
}
