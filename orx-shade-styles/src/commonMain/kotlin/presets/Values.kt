@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.DynamicParameter
import org.openrndr.draw.GlslParameter
import org.openrndr.draw.TextureParameter
import org.openrndr.draw.glslParameter
import org.openrndr.draw.textureParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.math.Vector2

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
