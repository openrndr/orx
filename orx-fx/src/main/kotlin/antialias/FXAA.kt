package org.openrndr.extra.fx.antialias

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentUrl
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

/**
 * FXAA approximate antialiasing filter. Only works on LDR inputs
 */
@Description("FXAA")
class FXAA : Filter(filterShaderFromUrl(filterFragmentUrl("antialias/fxaa.frag"))) {
    /**
     * luma threshold, default value is 0.5
     */
    @DoubleParameter("luma threshold", 0.0, 1.0)
    var lumaThreshold: Double by parameters

    /**
     * max search span, default value is 8.0
     */
    @DoubleParameter("max search span", 1.0, 16.0)
    var maxSpan: Double by parameters

    /**
     * direction reduce multiplier, default value is 0.0
     */
    @DoubleParameter("direction reduce multiplier", 0.0, 1.0)
    var directionReduceMultiplier: Double by parameters

    /**
     * direction reduce minimum, default value is 0.0
     */
    @DoubleParameter("direction reduce minium", 0.0, 1.0)
    var directionReduceMinimum: Double by parameters

    init {
        lumaThreshold = 0.5
        maxSpan = 8.0
        directionReduceMinimum = 0.0
        directionReduceMultiplier = 0.0
    }
}