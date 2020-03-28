package org.openrndr.extra.fx.dither

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter


@Description("Crosshatch")
class Crosshatch: Filter(filterShaderFromUrl(filterFragmentUrl("dither/crosshatch.frag"))) {
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