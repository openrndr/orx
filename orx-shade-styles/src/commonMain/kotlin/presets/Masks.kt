@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.ColorBuffer
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
 * A mask: a function `float mask(vec2 uv)` from 0 to 1 of a texture coordinate. Unlike a [ScalarPreset], which is
 * read at the pixel itself, a mask is read at the coordinate it is given, so warps before it move it.
 */
abstract class MaskPreset : Preset {
    abstract override val dynamicParameter: GlslFunction
    override val contract: GlslContract get() = CONTRACT

    companion object {
        val CONTRACT = GlslContract("float", "vec2")
    }
}

/**
 * 0 at [start] to 1 at [end]
 */
class LinearGradientMask : MaskPreset() {
    override val dynamicParameter = glslFunction<Double>(
        "vec2 uv", """
        vec2 d = p_end - p_start;
        return clamp(dot(uv - p_start, d) / max(dot(d, d), 1e-8), 0.0, 1.0);
        """.trimIndent()
    )

    @Vector2Parameter("start", 0.0, 1.0, order = 0)
    var start: Vector2 by dynamicParameter.Parameter("start", Vector2(0.0, 0.5))

    @Vector2Parameter("end", 0.0, 1.0, order = 1)
    var end: Vector2 by dynamicParameter.Parameter("end", Vector2(1.0, 0.5))
}

/**
 * 1 within [radius] from [center], to 0 over [softness]
 */
class RadialGradientMask : MaskPreset() {
    override val dynamicParameter = glslFunction<Double>(
        "vec2 uv", "1.0 - smoothstep(p_radius, p_radius + p_softness, distance(uv, p_center))"
    )

    @Vector2Parameter("center", 0.0, 1.0, order = 0)
    var center: Vector2 by dynamicParameter.Parameter("center", Vector2(0.5, 0.5))

    @DoubleParameter("radius", 0.0, 1.0, order = 1)
    var radius: Double by dynamicParameter.Parameter("radius", 0.2)

    @DoubleParameter("softness", 0.0, 1.0, order = 2)
    var softness: Double by dynamicParameter.Parameter("softness", 0.2)
}

/**
 * Fractal simplex noise, from 0 where the noise is below [threshold] to 1 over [softness], of coordinates scaled by
 * [scale] and moved by [offset], with [octaves] octaves
 */
class NoiseMask : MaskPreset() {
    override val dynamicParameter = glslFunction<Double>(
        "vec2 uv", """
        vec3 p = vec3(uv * p_scale, 0.0) + p_offset;
        float sum = 0.0;
        float amplitude = 0.5;
        for (int o = 0; o < p_octaves; ++o) {
            sum += snoise3(p) * amplitude;
            p *= 2.0;
            amplitude *= 0.5;
        }
        return smoothstep(p_threshold, p_threshold + p_softness, sum * 0.5 + 0.5);
        """.trimIndent(), listOf(snoise3Library)
    )

    @DoubleParameter("threshold", 0.0, 1.0, order = 0)
    var threshold: Double by dynamicParameter.Parameter("threshold", 0.4)

    @DoubleParameter("softness", 0.0, 1.0, order = 1)
    var softness: Double by dynamicParameter.Parameter("softness", 0.2)

    @DoubleParameter("scale", 0.1, 32.0, order = 2)
    var scale: Double by dynamicParameter.Parameter("scale", 4.0)

    @Vector3Parameter("offset", -10.0, 10.0, order = 3)
    var offset: Vector3 by dynamicParameter.Parameter("offset", Vector3.ZERO)

    @IntParameter("octaves", 1, 8, order = 4)
    var octaves: Int by dynamicParameter.Parameter("octaves", 4)
}

/**
 * The red channel of [texture]
 */
class TextureMask(texture: ColorBuffer) : MaskPreset() {
    override val dynamicParameter = glslFunction<Double>("vec2 uv", "texture(p_texture, uv).r")

    var texture: ColorBuffer by dynamicParameter.Parameter("texture", texture)
}

/**
 * 1 minus [mask]
 */
class InvertMask(mask: MaskPreset = RadialGradientMask()) : MaskPreset() {
    override val dynamicParameter: GlslFunction = glslFunction<Double>("vec2 uv", "1.0 - p_mask(uv)")

    @NestedParameter("mask")
    var mask: MaskPreset by dynamicParameter.presetSlot("mask", CONTRACT, mask)
}
