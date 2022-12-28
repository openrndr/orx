package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.fx.fx_duotone
import org.openrndr.extra.fx.fx_posterize
import org.openrndr.extra.parameters.BooleanParameter

import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.extra.color.phrases.ColorPhraseBook
import org.openrndr.extra.color.presets.CORAL
import org.openrndr.extra.color.presets.DARK_GRAY
import org.openrndr.extra.color.presets.NAVY

@Description("Posterize")
class Posterize : Filter1to1(filterShaderFromCode(fx_posterize, "posterize")) {

    @IntParameter("levels", 2, 32, order = 0)
    var levels: Int by parameters

    @IntParameter("window", 1, 8, order = 0)
    var window: Int by parameters

    init {
        levels = 4
        window = 1
    }
}