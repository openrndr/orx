package org.openrndr.extra.fx.blur

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode

/**
 * Exact Gaussian blur, implemented as a single pass filter
 */
class GaussianBlur : Filter(Shader.createFromCode(Filter.filterVertexCode,
        filterFragmentCode("blur/gaussian-blur.frag"))) {

    /**
     * The sample window, default value is 5
     */
    var window: Int by parameters

    /**
     * Spread multiplier, default value is 1.0
     */
    var spread: Double by parameters

    /**
     * Blur kernel sigma, default value is 1.0
     */
    var sigma: Double by parameters

    /**
     * Post-blur gain, default value is 1.0
     */
    var gain: Double by parameters

    init {
        window = 5
        spread = 1.0
        sigma = 1.0
        gain = 1.0
    }
}