@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.GlslFunction
import org.openrndr.draw.glslFunction
import org.openrndr.extra.parameters.BooleanParameter
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

/**
 * Moves, rotates and scales the image around [pivot]: the image is scaled by [scale], rotated by [rotation] degrees
 * and moved by [translation]
 */
class Affine : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>(
        "vec2 uv", """
        float a = -radians(p_rotation);
        vec2 d = uv - p_pivot - p_translation;
        d = mat2(cos(a), sin(a), -sin(a), cos(a)) * d;
        return p_pivot + d / p_scale;
        """.trimIndent()
    )

    @Vector2Parameter("translation", -1.0, 1.0, order = 0)
    var translation: Vector2 by dynamicParameter.Parameter("translation", Vector2.ZERO)

    @DoubleParameter("rotation", -180.0, 180.0, order = 1)
    var rotation: Double by dynamicParameter.Parameter("rotation", 0.0)

    @Vector2Parameter("scale", 0.1, 4.0, order = 2)
    var scale: Vector2 by dynamicParameter.Parameter("scale", Vector2.ONE)

    @Vector2Parameter("pivot", 0.0, 1.0, order = 3)
    var pivot: Vector2 by dynamicParameter.Parameter("pivot", Vector2(0.5, 0.5))
}

/**
 * Mirrors the image into [segments] wedges around [center], turned by [rotation] degrees
 */
class Kaleidoscope : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>(
        "vec2 uv", """
        vec2 d = uv - p_center;
        float r = length(d);
        float rotation = radians(p_rotation);
        float wedge = 6.2831853 / float(max(p_segments, 1));
        float a = mod(atan(d.y, d.x) - rotation, wedge);
        a = min(a, wedge - a) + rotation;
        return p_center + r * vec2(cos(a), sin(a));
        """.trimIndent()
    )

    @IntParameter("segments", 1, 32, order = 0)
    var segments: Int by dynamicParameter.Parameter("segments", 6)

    @DoubleParameter("rotation", -180.0, 180.0, order = 1)
    var rotation: Double by dynamicParameter.Parameter("rotation", 0.0)

    @Vector2Parameter("center", 0.0, 1.0, order = 2)
    var center: Vector2 by dynamicParameter.Parameter("center", Vector2(0.5, 0.5))
}

/**
 * Reads every pixel at the center of its cell in a grid of [xCells] by [yCells] cells
 */
class Pixelate : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>(
        "vec2 uv", """
        vec2 cells = max(vec2(float(p_xCells), float(p_yCells)), vec2(1.0));
        return (floor(uv * cells) + 0.5) / cells;
        """.trimIndent()
    )

    @IntParameter("x cells", 1, 512, order = 0)
    var xCells: Int by dynamicParameter.Parameter("xCells", 64)

    @IntParameter("y cells", 1, 512, order = 1)
    var yCells: Int by dynamicParameter.Parameter("yCells", 64)
}

/**
 * Displaces x by a cosine of y, or y by a cosine of x when [vertical]. [phase] is in half periods, [segments]
 * quantizes the wave into steps when it is larger than 0.
 */
class Wave : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>(
        "vec2 uv", """
        float t = p_vertical ? uv.x : uv.y;
        if (p_segments > 0) {
            t = floor(t * float(p_segments)) / float(p_segments);
        }
        float offset = p_amplitude * cos(t * 3.1415926535 * p_frequency + p_phase * 3.1415926535);
        return p_vertical ? uv + vec2(0.0, offset) : uv + vec2(offset, 0.0);
        """.trimIndent()
    )

    @DoubleParameter("amplitude", 0.0, 1.0, order = 0)
    var amplitude: Double by dynamicParameter.Parameter("amplitude", 0.1)

    @DoubleParameter("frequency", 0.0, 64.0, order = 1)
    var frequency: Double by dynamicParameter.Parameter("frequency", 1.0)

    @DoubleParameter("phase", -1.0, 1.0, order = 2)
    var phase: Double by dynamicParameter.Parameter("phase", 0.0)

    @IntParameter("segments", 0, 256, order = 3)
    var segments: Int by dynamicParameter.Parameter("segments", 0)

    @BooleanParameter("vertical", order = 4)
    var vertical: Boolean by dynamicParameter.Parameter("vertical", false)
}

/**
 * Bulges the image (positive [strength]) or pinches it (negative [strength]) along axes turned by [rotation]
 * degrees, then scales it by [scale]
 */
class Fisheye : WarpPreset() {
    override val dynamicParameter = glslFunction<Vector2>(
        "vec2 uv", """
        float phi = radians(p_rotation);
        mat2 rm = mat2(cos(phi), sin(phi), -sin(phi), cos(phi));
        vec2 coords = rm * ((uv - 0.5) * 2.0);
        vec2 offset = vec2(
            (1.0 - coords.y * coords.y) * p_strength * coords.x,
            (1.0 - coords.x * coords.x) * p_strength * coords.y
        );
        return ((uv - offset) - 0.5) * p_scale + 0.5;
        """.trimIndent()
    )

    @DoubleParameter("strength", -1.0, 1.0, order = 0)
    var strength: Double by dynamicParameter.Parameter("strength", 0.1)

    @DoubleParameter("scale", 0.0, 2.0, order = 1)
    var scale: Double by dynamicParameter.Parameter("scale", 1.0)

    @DoubleParameter("rotation", -180.0, 180.0, order = 2)
    var rotation: Double by dynamicParameter.Parameter("rotation", 0.0)
}
