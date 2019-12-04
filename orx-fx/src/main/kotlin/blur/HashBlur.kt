package org.openrndr.extra.fx.blur

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.extra.fx.filterFragmentCode

class HashBlur : Filter(Shader.createFromCode(Filter.filterVertexCode,
        filterFragmentCode("blur/hash-blur.frag"))) {
    /**
     * Blur radius in pixels, default is 5.0
     */
    var radius: Double by parameters

    /**
     * Time/seed, this should be fed with seconds, default is 0.0
     */
    var time: Double by parameters

    /**
     * Number of samples, default is 30
     */
    var samples: Int by parameters

    /**
     * Post-blur gain, default is 1.0
     */
    var gain: Double by parameters

    init {
        radius = 5.0
        time = 0.0
        samples = 30
        gain = 1.0
    }
}