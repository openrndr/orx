package org.openrndr.extra.fx.antialias

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode

/**
 * FXAA approximate antialiasing filter. Only works on LDR inputs
 */
class FXAA : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("antialias/fxaa.frag"))) {
    /**
     * luma threshold, default value is 0.5
     */
    var lumaThreshold: Double by parameters

    /**
     * max search span, default value is 8.0
     */
    var maxSpan: Double by parameters

    /**
     * direction reduce multiplier, default value is 0.0
     */
    var directionReduceMultiplier: Double by parameters

    /**
     * direction reduce minimum, default value is 0.0
     */
    var directionReduceMinimum: Double by parameters

    init {
        lumaThreshold = 0.5
        maxSpan = 8.0
        directionReduceMinimum = 0.0
        directionReduceMultiplier = 0.0
    }
}