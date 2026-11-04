@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.GlslFunction
import org.openrndr.draw.glslFunction
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.extra.parameters.Vector3Parameter
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

/**
 * A source: a function `vec4 source(vec2 uv)` that generates the color at texture coordinate `uv`. In an
 * [EffectChain] a source replaces the image, in a [Layer] it is blended over the image.
 */
abstract class SourcePreset : Preset {
    abstract override val dynamicParameter: GlslFunction
    override val contract: GlslContract get() = CONTRACT

    companion object {
        val CONTRACT = GlslContract("vec4", "vec2")
    }
}

/**
 * The same [color] everywhere
 */
class SolidSource(color: ColorRGBa = ColorRGBa.WHITE) : SourcePreset() {
    override val dynamicParameter = glslFunction<Vector4>("vec2 uv", "p_color")

    @ColorParameter("color")
    var color: ColorRGBa by dynamicParameter.Parameter("color", color)
}

/**
 * [from] at [start] to [to] at [end], in texture coordinates
 */
class LinearGradientSource : SourcePreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec2 uv", """
        vec2 d = p_end - p_start;
        float t = clamp(dot(uv - p_start, d) / max(dot(d, d), 1e-8), 0.0, 1.0);
        return mix(p_from, p_to, t);
        """.trimIndent()
    )

    @ColorParameter("from", order = 0)
    var from: ColorRGBa by dynamicParameter.Parameter("from", ColorRGBa.BLACK)

    @ColorParameter("to", order = 1)
    var to: ColorRGBa by dynamicParameter.Parameter("to", ColorRGBa.WHITE)

    @Vector2Parameter("start", 0.0, 1.0, order = 2)
    var start: Vector2 by dynamicParameter.Parameter("start", Vector2(0.0, 0.5))

    @Vector2Parameter("end", 0.0, 1.0, order = 3)
    var end: Vector2 by dynamicParameter.Parameter("end", Vector2(1.0, 0.5))
}

/**
 * [from] at [center] to [to] at [radius] from it, in texture coordinates
 */
class RadialGradientSource : SourcePreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec2 uv", "mix(p_from, p_to, clamp(distance(uv, p_center) / max(p_radius, 1e-8), 0.0, 1.0))"
    )

    @ColorParameter("from", order = 0)
    var from: ColorRGBa by dynamicParameter.Parameter("from", ColorRGBa.WHITE)

    @ColorParameter("to", order = 1)
    var to: ColorRGBa by dynamicParameter.Parameter("to", ColorRGBa.BLACK)

    @Vector2Parameter("center", 0.0, 1.0, order = 2)
    var center: Vector2 by dynamicParameter.Parameter("center", Vector2(0.5, 0.5))

    @DoubleParameter("radius", 0.0, 2.0, order = 3)
    var radius: Double by dynamicParameter.Parameter("radius", 0.5)
}

/**
 * A checkerboard of [xCells] by [yCells] cells in colors [even] and [odd]
 */
class CheckersSource : SourcePreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec2 uv", """
        vec2 cell = floor(uv * vec2(float(p_xCells), float(p_yCells)));
        return mod(cell.x + cell.y, 2.0) < 0.5 ? p_even : p_odd;
        """.trimIndent()
    )

    @ColorParameter("even", order = 0)
    var even: ColorRGBa by dynamicParameter.Parameter("even", ColorRGBa.WHITE)

    @ColorParameter("odd", order = 1)
    var odd: ColorRGBa by dynamicParameter.Parameter("odd", ColorRGBa.BLACK)

    @IntParameter("x cells", 1, 128, order = 2)
    var xCells: Int by dynamicParameter.Parameter("xCells", 8)

    @IntParameter("y cells", 1, 128, order = 3)
    var yCells: Int by dynamicParameter.Parameter("yCells", 8)
}

/**
 * Fractal simplex noise from [from] to [to], of texture coordinates scaled by [scale] and moved by [offset], with
 * [octaves] octaves
 */
class NoiseSource : SourcePreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec2 uv", """
        vec3 p = vec3(uv * p_scale, 0.0) + p_offset;
        float sum = 0.0;
        float amplitude = 0.5;
        for (int o = 0; o < p_octaves; ++o) {
            sum += snoise3(p) * amplitude;
            p *= 2.0;
            amplitude *= 0.5;
        }
        return mix(p_from, p_to, clamp(sum * 0.5 + 0.5, 0.0, 1.0));
        """.trimIndent(), listOf(snoise3Library)
    )

    @ColorParameter("from", order = 0)
    var from: ColorRGBa by dynamicParameter.Parameter("from", ColorRGBa.BLACK)

    @ColorParameter("to", order = 1)
    var to: ColorRGBa by dynamicParameter.Parameter("to", ColorRGBa.WHITE)

    @DoubleParameter("scale", 0.1, 32.0, order = 2)
    var scale: Double by dynamicParameter.Parameter("scale", 4.0)

    @Vector3Parameter("offset", -10.0, 10.0, order = 3)
    var offset: Vector3 by dynamicParameter.Parameter("offset", Vector3.ZERO)

    @IntParameter("octaves", 1, 8, order = 4)
    var octaves: Int by dynamicParameter.Parameter("octaves", 4)
}
