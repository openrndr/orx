package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Pal TV Effect")
class Pal : Filter(filterShaderFromUrl(filterFragmentUrl("color/pal.frag"))) {
    @DoubleParameter("amount", 0.0, 1.0)
    var amount: Double by parameters
    @DoubleParameter("pixelation", 0.0, 1.0)
    var pixelation: Double by parameters
    @DoubleParameter("filter_gain", 0.0, 10.0)
    var filter_gain: Double by parameters
    @DoubleParameter("filter_invgain", 0.0, 10.0)
    var filter_invgain: Double by parameters
    init {
        amount = 1.0
        pixelation = 0.0
        filter_gain = 1.0
        filter_invgain = 1.6
    }
}