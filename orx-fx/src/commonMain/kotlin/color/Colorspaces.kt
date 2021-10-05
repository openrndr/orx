package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter
import org.openrndr.extra.fx.fx_rgb_to_ycbcr
import org.openrndr.extra.fx.fx_ycbcr_to_rgb
import org.openrndr.extra.fx.mppFilterShader

class RgbToYCbcr : Filter(mppFilterShader(fx_rgb_to_ycbcr, "rgb-to-ycbcr"))
class YcbcrToRgb : Filter(mppFilterShader(fx_ycbcr_to_rgb, "ycbcr_to_rgb"))