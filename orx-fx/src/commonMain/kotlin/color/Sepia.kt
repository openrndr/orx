@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.fx_sepia
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Sepia")
class Sepia : Filter1to1(mppFilterShader(fx_sepia, "sepia")) {
    @DoubleParameter("amount", 0.0, 1.0)
    var amount: Double by parameters

    init {
        amount = 0.5
    }
}