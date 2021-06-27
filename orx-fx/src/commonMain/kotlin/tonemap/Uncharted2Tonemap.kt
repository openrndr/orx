package org.openrndr.extra.fx.tonemap

import org.openrndr.draw.Filter
import org.openrndr.extra.fx.fx_uncharted2_tonemap
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

/**
 * Uncharted 2 tonemap filter
 */
@Description("Uncharted 2 tonemap")
class Uncharted2Tonemap : Filter(mppFilterShader(fx_uncharted2_tonemap, "uncharted2-tonemap")) {
    @DoubleParameter("exposure bias", 0.0, 128.0)
    var exposureBias:Double by parameters
    init {
        exposureBias = 2.0
    }
}