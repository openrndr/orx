package org.openrndr.extra.fx.color

import org.openrndr.draw.*
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Color correction")
class ColorCorrection : Filter(Shader.createFromCode(filterVertexCode, filterFragmentCode("color/color-correction.frag"))) {
    @DoubleParameter("brightness", -1.0, 1.0, order = 0)
    var brightness: Double by parameters

    @DoubleParameter("contrast", -1.0, 1.0, order = 1)
    var contrast: Double by parameters

    @DoubleParameter("saturation", -1.0, 1.0, order = 2)
    var saturation: Double by parameters

    @DoubleParameter("hue shift", -180.0, 180.0, order = 3)
    var hueShift: Double by parameters

    init {
        contrast = 0.0
        brightness = 0.0
        saturation = 0.0
        hueShift = 0.0
    }
}