package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter
import org.openrndr.extra.fx.*
import org.openrndr.extra.fx.fx_rgb_to_oklab
import org.openrndr.extra.fx.fx_rgb_to_ycbcr
import org.openrndr.extra.fx.fx_ycbcr_to_rgb
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.extras.color.phrases.ColorPhraseBook

class RgbToYCbcr : Filter(mppFilterShader(fx_rgb_to_ycbcr, "rgb-to-ycbcr"))
class YcbcrToRgb : Filter(mppFilterShader(fx_ycbcr_to_rgb, "ycbcr_to_rgb"))

class RgbToOkLab : Filter(mppFilterShader(run {
    ColorPhraseBook.register()
    fx_rgb_to_oklab.preprocess()
}, "rgb-to-oklab"))

class OkLabToRgb : Filter(mppFilterShader(run {
    ColorPhraseBook.register()
    fx_oklab_to_rgb.preprocess()
}, "oklab-to-rgb"))
