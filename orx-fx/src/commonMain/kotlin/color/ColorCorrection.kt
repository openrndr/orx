@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.color

import org.openrndr.draw.*
import org.openrndr.extra.fx.fx_color_correction
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Color correction")
class ColorCorrection : Filter1to1(mppFilterShader(fx_color_correction, "color-correction")) {
    @DoubleParameter("brightness", -1.0, 1.0, order = 0)
    var brightness: Double by parameters

    @DoubleParameter("contrast", -1.0, 1.0, order = 1)
    var contrast: Double by parameters

    @DoubleParameter("saturation", -1.0, 1.0, order = 2)
    var saturation: Double by parameters

    @DoubleParameter("hue shift", -180.0, 180.0, order = 3)
    var hueShift: Double by parameters

    @DoubleParameter("gamma", 0.0, 5.0, order = 4)
    var gamma: Double by parameters

    @DoubleParameter("opacity", 0.0, 1.0, order = 5)
    var opacity: Double by parameters

    @BooleanParameter("clamp", order = 6)
    var clamped: Boolean by parameters

    init {
        contrast = 0.0
        brightness = 0.0
        saturation = 0.0
        hueShift = 0.0
        gamma = 1.0
        opacity = 1.0
        clamped = true
    }
}