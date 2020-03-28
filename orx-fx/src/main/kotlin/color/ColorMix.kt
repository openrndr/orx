package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description

class ColorMix : Filter(filterShaderFromUrl(filterFragmentUrl("color/color-mix.frag")))

@Description("Tint")
class ColorTint : Filter(filterShaderFromUrl(filterFragmentUrl("color/color-tint.frag"))) {
    @ColorParameter("tint")
    var tint: ColorRGBa by parameters

    init {
        tint = ColorRGBa.PINK
    }

}
