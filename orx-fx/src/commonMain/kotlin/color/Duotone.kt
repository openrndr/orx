@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_duotone
import org.openrndr.extra.parameters.BooleanParameter

import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.extra.color.phrases.ColorPhraseBook
import org.openrndr.extra.color.presets.CORAL
import org.openrndr.extra.color.presets.DARK_GRAY
import org.openrndr.extra.color.presets.NAVY

@Description("Duotone")
class Duotone : Filter1to1(filterShaderFromCode(run {
    ColorPhraseBook.register()
    fx_duotone.preprocess()
}, "duotone")) {

    @ColorParameter("background", order = 0)
    var backgroundColor: ColorRGBa by parameters

    @ColorParameter("foreground", order = 1)
    var foregroundColor: ColorRGBa by parameters

    @BooleanParameter("LAB interpolation", order = 2)
    var labInterpolation: Boolean by parameters

    init {
        backgroundColor = ColorRGBa.NAVY
        foregroundColor = ColorRGBa.CORAL
        labInterpolation = true
    }
}