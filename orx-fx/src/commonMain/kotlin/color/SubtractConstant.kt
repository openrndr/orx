package org.openrndr.extra.fx.color

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.fx_subtract_constant
import org.openrndr.extra.fx.mppFilterShader

class SubtractConstant : Filter1to1(mppFilterShader(fx_subtract_constant, "subtract-constant")) {
    var constant: ColorRGBa by parameters

    init {
        constant = ColorRGBa(1.0, 1.0, 1.0, 0.0)
    }
}
