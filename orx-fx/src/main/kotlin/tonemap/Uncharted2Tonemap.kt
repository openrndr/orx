package org.openrndr.extra.fx.tonemap

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

/**
 * Uncharted 2 tonemap filter
 */
@Description("Uncharted 2 tonemap")
class Uncharted2Tonemap : Filter(filterShaderFromUrl(filterFragmentUrl("tonemap/uncharted2-tonemap.frag"))) {

    @DoubleParameter("exposure bias", 0.0, 128.0)
    var exposureBias:Double by parameters
    init {
        exposureBias = 2.0
    }
}