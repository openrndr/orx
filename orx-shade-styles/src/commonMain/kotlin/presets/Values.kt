@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.DynamicParameter
import org.openrndr.draw.GlslParameter
import org.openrndr.draw.TextureParameter
import org.openrndr.draw.glslParameter
import org.openrndr.draw.textureParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.Vector3Parameter
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * A float that can vary per pixel, for slots that read it as `p_<name>`
 */
abstract class ScalarPreset : Preset {
    override val contract: GlslContract get() = CONTRACT

    companion object {
        val CONTRACT = GlslContract("float")
    }
}

/**
 * A vec2 that can vary per pixel, for slots that read it as `p_<name>`
 */
abstract class Vector2Preset : Preset {
    override val contract: GlslContract get() = CONTRACT

    companion object {
        val CONTRACT = GlslContract("vec2")
    }
}

/**
 * The same [value] for every pixel
 */
class ConstantScalar(value: Double = 0.0) : ScalarPreset() {
    override val dynamicParameter: GlslParameter = glslParameter<Double>("p_value")

    @DoubleParameter("value", 0.0, 1.0)
    var value: Double by dynamicParameter.Parameter("value", value)
}

/**
 * The same [value] for every pixel
 */
class ConstantVector2(value: Vector2 = Vector2.ZERO) : Vector2Preset() {
    override val dynamicParameter: GlslParameter = glslParameter<Vector2>("p_value")

    @Vector2Parameter("value", -1.0, 1.0)
    var value: Vector2 by dynamicParameter.Parameter("value", value)
}

/**
 * A texture parameter of [texture] at [coordinate], made again when the texture or coordinate changes
 */
private class TextureParameterCache(private val make: (ColorBuffer, String) -> TextureParameter) {
    private var cached: TextureParameter? = null

    fun get(texture: ColorBuffer, coordinate: String): TextureParameter {
        val current = cached
        if (current != null && current.texture === texture && current.coordinate == coordinate) return current
        return make(texture, coordinate).also { cached = it }
    }
}

/**
 * The red channel of [texture] at [coordinate], which by default covers what is drawn, or the input of a filter
 */
class TextureScalar(
    var texture: ColorBuffer,
    var coordinate: String = TextureParameter.DEFAULT_COORDINATE
) : ScalarPreset() {
    private val cache = TextureParameterCache { t, c -> textureParameter<Double>(t, c) }
    override val dynamicParameter: DynamicParameter get() = cache.get(texture, coordinate)
}

/**
 * The red and green channels of [texture] at [coordinate], which by default covers what is drawn, or the input of a
 * filter
 */
class TextureVector2(
    var texture: ColorBuffer,
    var coordinate: String = TextureParameter.DEFAULT_COORDINATE
) : Vector2Preset() {
    private val cache = TextureParameterCache { t, c -> textureParameter<Vector2>(t, c) }
    override val dynamicParameter: DynamicParameter get() = cache.get(texture, coordinate)
}

/**
 * 0 at [start] to 1 at [end], in texture coordinates
 */
class LinearGradientScalar : ScalarPreset() {
    override val dynamicParameter: GlslParameter = glslParameter<Double>(
        "clamp(dot(c_textureCoordinate - p_start, p_end - p_start) / max(dot(p_end - p_start, p_end - p_start), 1e-8), 0.0, 1.0)"
    )

    @Vector2Parameter("start", 0.0, 1.0, order = 0)
    var start: Vector2 by dynamicParameter.Parameter("start", Vector2(0.0, 0.5))

    @Vector2Parameter("end", 0.0, 1.0, order = 1)
    var end: Vector2 by dynamicParameter.Parameter("end", Vector2(1.0, 0.5))
}

/**
 * 1 at [center] to 0 at [radius] from it, in texture coordinates
 */
class RadialGradientScalar : ScalarPreset() {
    override val dynamicParameter: GlslParameter = glslParameter<Double>(
        "1.0 - clamp(distance(c_textureCoordinate, p_center) / max(p_radius, 1e-8), 0.0, 1.0)"
    )

    @Vector2Parameter("center", 0.0, 1.0, order = 0)
    var center: Vector2 by dynamicParameter.Parameter("center", Vector2(0.5, 0.5))

    @DoubleParameter("radius", 0.0, 2.0, order = 1)
    var radius: Double by dynamicParameter.Parameter("radius", 0.5)
}

/**
 * Fractal simplex noise in 0..1, of texture coordinates scaled by [scale] and moved by [offset], with [octaves]
 * octaves
 */
class NoiseScalar : ScalarPreset() {
    override val dynamicParameter: GlslParameter = glslParameter<Double>(
        """
        vec3 p = vec3(c_textureCoordinate * p_scale, 0.0) + p_offset;
        float sum = 0.0;
        float amplitude = 0.5;
        for (int o = 0; o < p_octaves; ++o) {
            sum += snoise3(p) * amplitude;
            p *= 2.0;
            amplitude *= 0.5;
        }
        return clamp(sum * 0.5 + 0.5, 0.0, 1.0);
        """.trimIndent(), listOf(snoise3Library)
    )

    @DoubleParameter("scale", 0.1, 32.0, order = 0)
    var scale: Double by dynamicParameter.Parameter("scale", 4.0)

    @Vector3Parameter("offset", -10.0, 10.0, order = 1)
    var offset: Vector3 by dynamicParameter.Parameter("offset", Vector3.ZERO)

    @IntParameter("octaves", 1, 8, order = 2)
    var octaves: Int by dynamicParameter.Parameter("octaves", 4)
}
