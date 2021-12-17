package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter
import org.openrndr.extra.fx.fx_invert
import org.openrndr.extra.fx.fx_sepia
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Invert")
class Invert : Filter(mppFilterShader(fx_invert, "invert")) {
    @DoubleParameter("amount", 0.0, 1.0)
    var amount: Double by parameters

    init {
        amount = 1.0
    }
}