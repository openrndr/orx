package org.openrndr.extra.fx.edges

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Luma threshold ")
class LumaSobel : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("edges/luma-sobel.frag"))) {

    @ColorParameter("background color")
    var backgroundColor: ColorRGBa by parameters

    @ColorParameter("edge color")
    var edgeColor: ColorRGBa by parameters

    @DoubleParameter("background opacity", 0.0, 1.0)
    var backgroundOpacity:Double by parameters

    @DoubleParameter("edge opacity", 0.0, 1.0)
    var edgeOpacity:Double by parameters

    init {
        backgroundColor = ColorRGBa.BLACK
        edgeColor = ColorRGBa.WHITE
        edgeOpacity = 1.0
        backgroundOpacity = 1.0
    }
}
