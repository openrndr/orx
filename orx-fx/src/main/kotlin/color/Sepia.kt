package org.openrndr.extra.fx.color

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Sepia")
class Sepia : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("color/sepia.frag"))) {
    @DoubleParameter("amount", 0.0, 1.0)
    var amount: Double by parameters

    init {
        amount = 0.5
    }
}