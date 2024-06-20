@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.fx.dither

import org.openrndr.draw.Filter1to1
import org.openrndr.extra.fx.fx_crosshatch
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Crosshatch")
class Crosshatch : Filter1to1(mppFilterShader(fx_crosshatch, "crosshatch")) {
    @DoubleParameter("threshold 1", 0.0, 1.0)
    var t1: Double by parameters

    @DoubleParameter("threshold 2", 0.0, 1.0)
    var t2: Double by parameters

    @DoubleParameter("threshold 3", 0.0, 1.0)
    var t3: Double by parameters

    @DoubleParameter("threshold 4", 0.0, 1.0)
    var t4: Double by parameters

    init {
        t1 = 1.0
        t2 = 0.75
        t3 = 0.5
        t4 = 0.3
    }
}