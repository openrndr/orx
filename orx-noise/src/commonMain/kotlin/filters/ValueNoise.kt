package org.openrndr.extra.noise.filters

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.parameters.*
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4

/**
 * Filter that produces value noise
 */
@Description("Value Noise")
class ValueNoise : Filter(filterShaderFromCode(run {
    noise_value.preprocess()
}, "value-noise")) {
    @Vector2Parameter("Seed", 0.0, 10000.0)
    var seed: Vector2 by parameters

    /**
     * base noise scale, default is Vector2(1.0, 1.0)
     */
    @Vector2Parameter("Scale", 0.0, 5.0)
    var scale: Vector2 by parameters

    /**
     * lacunarity is the amount by which scale is modulated per octave, default is Vector2(2.0, 2.0)
     */
    @Vector2Parameter("Lacunarity", 0.0, 5.0)
    var lacunarity: Vector2 by parameters

    /**
     * gain is the base intensity per channel, default is Vector2(1.0, 1.0, 1.0, 1.0)
     */
    @Vector4Parameter("Gain", 0.0, 1.0)
    var gain: Vector4 by parameters

    /**
     * decay is the amount by which gain is modulated per octave, default is Vector4(0.5, 0.5, 0.5, 0.5)
     */
    @Vector4Parameter("Decay", 0.0, 1.0)
    var decay: Vector4 by parameters

    /**
     * the number of octaves of noise to generate, default is 4
     */
    @IntParameter("Octaves", 1, 8)
    var octaves: Int by parameters

    /**
     * the value to add to the resulting noise
     */
    @Vector4Parameter("Bias", -1.0, 1.0)
    var bias: Vector4 by parameters

    /**
     * should the output colors be multiplied by the alpha channel, default is true
     */
    var premultipliedAlpha: Boolean by parameters

    init {
        seed = Vector2.ZERO
        scale = Vector2.ONE
        lacunarity = Vector2(2.0, 2.0)
        gain = Vector4.ONE
        decay = Vector4.ONE / 2.0
        octaves = 4
        bias = Vector4.ZERO
        premultipliedAlpha = true
    }
}