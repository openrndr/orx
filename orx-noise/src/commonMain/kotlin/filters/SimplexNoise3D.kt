@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.noise.filters

import org.openrndr.draw.Filter
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.parameters.*
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

/**
 * Filter that produces 3D Simplex Noise
 */
@Description("Simplex Noise")
class SimplexNoise3D : Filter(filterShaderFromCode(run {
    noise_simplex3D.preprocess()
}, "simplex-noise-3d")) {
    var seed: Vector3 by parameters

    /**
     * base noise scale, default is Vector3(1.0, 1.0, 1.0)
     */
    @Vector3Parameter("Scale", 0.0, 5.0)
    var scale: Vector3 by parameters

    /**
     * lacunarity is the amount by which scale is modulated per octave, default is Vector3(2.0, 2.0, 2.0)
     */
    @Vector3Parameter("Lacunarity", 0.0, 5.0)
    var lacunarity: Vector3 by parameters

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
    @BooleanParameter("Premultiplied alpha")
    var premultipliedAlpha: Boolean by parameters

    init {
        seed = Vector3.ZERO
        scale = Vector3.ONE
        lacunarity = Vector3(2.0, 2.0, 2.0)
        gain = Vector4.ONE / 2.0
        decay = Vector4.ONE / 2.0
        octaves = 4
        bias = Vector4.ONE / 2.0
        premultipliedAlpha = true
    }
}