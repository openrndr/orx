@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.GlslFunction
import org.openrndr.draw.glslFunction
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.NestedParameter
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.extra.parameters.Vector3Parameter
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * A warp: a function `vec2 warp(vec2 uv)` that maps a coordinate to the coordinate to read from
 */
abstract class WarpPreset : Preset {
    abstract override val dynamicParameter: GlslFunction
    override val contract: GlslContract get() = CONTRACT

    companion object {
        val CONTRACT = GlslContract("vec2", "vec2")
    }
}

/**
 * Displaces coordinates by fractal simplex noise, with [octaves] octaves that each scale by [lacunarity] and are
 * weighted by [decay]. The noise is sampled on a circle of [radius] in the third dimension, [phase] moves along it,
 * which loops for phases that differ by 2.
 */
class NoiseWarp : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>(
        "vec2 uv", """
        vec3 q = vec3(uv * 2.0 - 1.0, 1.0);
        vec3 xseed = vec3(p_seed.xy, p_seed.z + p_radius * cos(p_phase * 3.1415926535));
        vec3 yseed = vec3(p_seed.yx, p_seed.z + p_radius * sin(p_phase * 3.1415926535));
        vec3 px = (q + xseed) * p_scale;
        vec3 py = (q + yseed + vec3(100.37, 40.51, 9.43)) * p_scale;
        float tx = 0.0;
        float ty = 0.0;
        float g = p_gain;
        for (int o = 0; o < p_octaves; ++o) {
            tx += snoise3(px) * g;
            ty += snoise3(py) * g;
            px = px * p_lacunarity + 100.0;
            py = py * p_lacunarity + 100.0;
            g *= p_decay;
        }
        return uv + vec2(tx, ty);
        """.trimIndent(), listOf(snoise3Library)
    )

    @Vector3Parameter("seed", -10.0, 10.0, order = 0)
    var seed: Vector3 by dynamicParameter.Parameter("seed", Vector3.ZERO)

    /** base noise scale, default is 1.0 */
    @DoubleParameter("scale", 0.01, 8.0, order = 1)
    var scale: Double by dynamicParameter.Parameter("scale", 1.0)

    @DoubleParameter("phase", -2.0, 2.0, order = 2)
    var phase: Double by dynamicParameter.Parameter("phase", 0.0)

    @DoubleParameter("radius", 0.0, 2.0, order = 3)
    var radius: Double by dynamicParameter.Parameter("radius", 1.0)

    /** the amount by which the scale changes per octave, default is 2.0 */
    @DoubleParameter("lacunarity", 1.0, 4.0, order = 4)
    var lacunarity: Double by dynamicParameter.Parameter("lacunarity", 2.0)

    /** the displacement of the first octave, default is 0.5 */
    @DoubleParameter("gain", 0.0, 1.0, order = 5)
    var gain: Double by dynamicParameter.Parameter("gain", 0.5)

    /** the amount by which the gain changes per octave, default is 0.5 */
    @DoubleParameter("decay", 0.0, 1.0, order = 6)
    var decay: Double by dynamicParameter.Parameter("decay", 0.5)

    /** the number of octaves, default is 4 */
    @IntParameter("octaves", 1, 10, order = 7)
    var octaves: Int by dynamicParameter.Parameter("octaves", 4)
}

/**
 * Displaces coordinates along sines: x by the sine of y and y by the sine of x
 */
class Ripple : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>(
        "vec2 uv", "uv + p_amount * sin(uv.yx * p_frequency + p_phase)"
    )

    @DoubleParameter("amount", 0.0, 0.1, order = 0)
    var amount: Double by dynamicParameter.Parameter("amount", 0.02)

    @DoubleParameter("frequency", 0.0, 100.0, order = 1)
    var frequency: Double by dynamicParameter.Parameter("frequency", 30.0)

    @DoubleParameter("phase", -10.0, 10.0, order = 2)
    var phase: Double by dynamicParameter.Parameter("phase", 0.0)
}

/**
 * Rotates coordinates around [center] by [angle] radians, less further from the center, as set by [falloff]
 */
class Swirl : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>(
        "vec2 uv", """
        vec2 d = uv - p_center;
        float a = p_angle * exp(-dot(d, d) * p_falloff);
        return p_center + mat2(cos(a), sin(a), -sin(a), cos(a)) * d;
        """.trimIndent()
    )

    @Vector2Parameter("center", 0.0, 1.0, order = 0)
    var center: Vector2 by dynamicParameter.Parameter("center", Vector2(0.5, 0.5))

    @DoubleParameter("angle", -6.3, 6.3, order = 1)
    var angle: Double by dynamicParameter.Parameter("angle", 2.0)

    @DoubleParameter("falloff", 0.0, 32.0, order = 2)
    var falloff: Double by dynamicParameter.Parameter("falloff", 8.0)
}

/**
 * Warps with [first], then with [second]
 */
class ChainWarp(first: WarpPreset = Ripple(), second: WarpPreset = Swirl()) : WarpPreset() {
    override val dynamicParameter: GlslFunction = glslFunction<Vector2>("vec2 uv", "p_second(p_first(uv))")

    @NestedParameter("first", order = 0)
    var first: WarpPreset by dynamicParameter.presetSlot("first", CONTRACT, first)

    @NestedParameter("second", order = 1)
    var second: WarpPreset by dynamicParameter.presetSlot("second", CONTRACT, second)
}
