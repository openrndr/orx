package org.openrndr.extra.noise.filters

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.parameters.*
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.math.Vector4

/**
 * Hash noise filter that produces white-noise-like noise.
 */
@Description("Hash Noise")
class HashNoise : Filter(filterShaderFromCode(run {
    noise_hash.preprocess()
}, "hash-noise")) {
    /**
     * noise gain per channel, default is Vector4(1.0, 1.0, 1.0, 0.0)
     */
    @Vector4Parameter("Gain")
    var gain: Vector4 by parameters

    /**
     * noise bias per channel, default is Vector4(0.0, 0.0, 0.0, 1.0)
     */
    @Vector4Parameter("Bias")
    var bias: Vector4 by parameters

    /**
     * is the noise monochrome, default is true
     */
    @BooleanParameter("Monochrome")
    var monochrome: Boolean by parameters

    /**
     * noise seed, feed it with time to animate
     */
    @DoubleParameter("Seed", 0.0, 1000.0)
    var seed: Double by parameters

    init {
        monochrome = true
        gain = Vector4(1.0, 1.0, 1.0, 0.0)
        bias = Vector4(0.0, 0.0, 0.0, 1.0)
        seed = 0.0
    }
}


