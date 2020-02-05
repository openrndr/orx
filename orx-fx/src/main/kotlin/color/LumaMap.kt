package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Luma map ")
class LumaMap : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/luma-map.frag"))) {
    @ColorParameter("foreground color")
    var foreground: ColorRGBa by parameters

    @ColorParameter("background color")
    var background: ColorRGBa by parameters

    @DoubleParameter("background opacity", 0.0, 1.0)
    var backgroundOpacity: Double by parameters

    @DoubleParameter("foreground opacity", 0.0, 1.0)
    var foregroundOpacity: Double by parameters

    init {
        foreground = ColorRGBa.WHITE
        background = ColorRGBa.BLACK
        foregroundOpacity = 1.0
        backgroundOpacity = 1.0
    }
}