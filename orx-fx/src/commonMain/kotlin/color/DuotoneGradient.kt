package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_duotone_gradient
import org.openrndr.extra.parameters.BooleanParameter

import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.extra.color.phrases.ColorPhraseBook
import org.openrndr.extra.color.presets.CORAL
import org.openrndr.extra.color.presets.NAVY

@Description("Duotone Gradient")
class DuotoneGradient : Filter(filterShaderFromCode(run {
    ColorPhraseBook.register()
    fx_duotone_gradient.preprocess()
}, "duotone-gradient")) {

    @ColorParameter("background 0", order = 0)
    var backgroundColor0: ColorRGBa by parameters

    @ColorParameter("foreground 0", order = 1)
    var foregroundColor0: ColorRGBa by parameters

    @ColorParameter("background 1", order = 2)
    var backgroundColor1: ColorRGBa by parameters

    @ColorParameter("foreground 1", order = 3)
    var foregroundColor1: ColorRGBa by parameters

    @BooleanParameter("LAB interpolation", order = 4)
    var labInterpolation: Boolean by parameters

    @DoubleParameter("rotation", -180.0, 180.0, order = 5)
    var rotation: Double by parameters

    init {
        backgroundColor0 = ColorRGBa.NAVY
        foregroundColor0 = ColorRGBa.CORAL
        backgroundColor1 = ColorRGBa.BLACK
        foregroundColor1 = ColorRGBa.WHITE
        rotation = 0.0
        labInterpolation = true
    }
}