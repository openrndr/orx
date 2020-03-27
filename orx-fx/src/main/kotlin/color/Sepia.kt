package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Sepia")
class Sepia : Filter(filterShaderFromUrl(filterFragmentUrl("color/sepia.frag"))) {
    @DoubleParameter("amount", 0.0, 1.0)
    var amount: Double by parameters

    init {
        amount = 0.5
    }
}