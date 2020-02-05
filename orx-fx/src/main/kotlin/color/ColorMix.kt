package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description

class ColorMix : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/color-mix.frag")))

@Description("Tint")
class ColorTint : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/color-tint.frag"))) {
    @ColorParameter("tint")
    var tint: ColorRGBa by parameters

    init {
        tint = ColorRGBa.PINK
    }

}
