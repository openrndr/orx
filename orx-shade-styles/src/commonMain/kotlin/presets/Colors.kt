@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.GlslFunction
import org.openrndr.draw.glslFunction
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.NestedParameter
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4

/**
 * A color operation: a function `vec4 color(vec4 c, vec2 uv)` that maps the color `c` at texture coordinate `uv` to
 * a new color. The operations below change the rgb channels as they are stored and keep alpha.
 */
abstract class ColorPreset : Preset {
    abstract override val dynamicParameter: GlslFunction
    override val contract: GlslContract get() = CONTRACT

    companion object {
        val CONTRACT = GlslContract("vec4", "vec4", "vec2")
    }
}

/**
 * Color grading: [exposure] in stops, [contrast] around mid gray, [saturation] around the luma and [gamma]
 */
class Grade : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        vec3 rgb = c.rgb * exp2(p_exposure);
        rgb = (rgb - 0.5) * p_contrast + 0.5;
        float luma = dot(rgb, vec3(0.2126, 0.7152, 0.0722));
        rgb = mix(vec3(luma), rgb, p_saturation);
        rgb = pow(max(rgb, vec3(0.0)), vec3(1.0 / p_gamma));
        return vec4(rgb, c.a);
        """.trimIndent()
    )

    @DoubleParameter("exposure", -4.0, 4.0, order = 0)
    var exposure: Double by dynamicParameter.Parameter("exposure", 0.0)

    @DoubleParameter("contrast", 0.0, 2.0, order = 1)
    var contrast: Double by dynamicParameter.Parameter("contrast", 1.0)

    @DoubleParameter("saturation", 0.0, 2.0, order = 2)
    var saturation: Double by dynamicParameter.Parameter("saturation", 1.0)

    @DoubleParameter("gamma", 0.1, 4.0, order = 3)
    var gamma: Double by dynamicParameter.Parameter("gamma", 1.0)
}

/**
 * Multiplies the colors by [color], by [amount]
 */
class Tint : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", "vec4(mix(c.rgb, c.rgb * p_color.rgb, p_amount), c.a)"
    )

    @ColorParameter("color", order = 0)
    var color: ColorRGBa by dynamicParameter.Parameter("color", ColorRGBa.fromHex("#f0c080"))

    @DoubleParameter("amount", 0.0, 1.0, order = 1)
    var amount: Double by dynamicParameter.Parameter("amount", 1.0)
}

/**
 * Reduces every channel to [levels] levels
 */
class Posterize : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        float n = max(float(p_levels), 2.0);
        return vec4(min(floor(c.rgb * n) / (n - 1.0), vec3(1.0)), c.a);
        """.trimIndent()
    )

    @IntParameter("levels", 2, 32)
    var levels: Int by dynamicParameter.Parameter("levels", 4)
}

/**
 * Inverts the colors, by [amount]
 */
class Invert : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", "vec4(mix(c.rgb, 1.0 - c.rgb, p_amount), c.a)"
    )

    @DoubleParameter("amount", 0.0, 1.0)
    var amount: Double by dynamicParameter.Parameter("amount", 1.0)
}

/**
 * Darkens toward the edges: by [amount] outside [radius] from [center], over a band of [softness]
 */
class Vignette : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        float d = distance(uv, p_center);
        float v = 1.0 - smoothstep(p_radius, p_radius + p_softness, d);
        return vec4(c.rgb * mix(1.0, v, p_amount), c.a);
        """.trimIndent()
    )

    @Vector2Parameter("center", 0.0, 1.0, order = 0)
    var center: Vector2 by dynamicParameter.Parameter("center", Vector2(0.5, 0.5))

    @DoubleParameter("radius", 0.0, 1.0, order = 1)
    var radius: Double by dynamicParameter.Parameter("radius", 0.4)

    @DoubleParameter("softness", 0.0, 1.0, order = 2)
    var softness: Double by dynamicParameter.Parameter("softness", 0.4)

    @DoubleParameter("amount", 0.0, 1.0, order = 3)
    var amount: Double by dynamicParameter.Parameter("amount", 1.0)
}

/**
 * Applies [first], then [second]
 */
class ChainColor(first: ColorPreset = Grade(), second: ColorPreset = Vignette()) : ColorPreset() {
    override val dynamicParameter: GlslFunction = glslFunction<Vector4>("vec4 c, vec2 uv", "p_second(p_first(c, uv), uv)")

    @NestedParameter("first", order = 0)
    var first: ColorPreset by dynamicParameter.presetSlot("first", CONTRACT, first)

    @NestedParameter("second", order = 1)
    var second: ColorPreset by dynamicParameter.presetSlot("second", CONTRACT, second)
}
