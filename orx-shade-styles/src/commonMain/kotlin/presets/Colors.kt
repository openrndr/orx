@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.GlslFunction
import org.openrndr.draw.glslFunction
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.extra.parameters.NestedParameter
import org.openrndr.extra.parameters.OptionParameter
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

/** the colormaps of [Colormap] */
enum class ColormapType {
    TURBO,
    SPECTRAL,
    GRAYSCALE
}

/**
 * Maps the luma of the colors to a [colormap]: the luma between [minValue] and [maxValue], raised to [curve], picks
 * the color. [amount] mixes the mapped colors with the original ones.
 */
class Colormap : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        float luma = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
        float v = pow(clamp((luma - p_minValue) / (p_maxValue - p_minValue), 0.0, 1.0), p_curve);
        vec3 rgb = p_colormap == 0 ? turbo_colormap(v) : (p_colormap == 1 ? spectral_zucconi6(v) : vec3(v));
        return vec4(mix(c.rgb, rgb, p_amount), c.a);
        """.trimIndent(), listOf(turboColormapLibrary, spectralZucconi6Library)
    )

    @OptionParameter("colormap", order = 0)
    var colormap: ColormapType = ColormapType.TURBO
        set(value) {
            field = value
            dynamicParameter.parameter("colormap", value.ordinal)
        }

    @DoubleParameter("min value", 0.0, 1.0, order = 1)
    var minValue: Double by dynamicParameter.Parameter("minValue", 0.0)

    @DoubleParameter("max value", 0.0, 1.0, order = 2)
    var maxValue: Double by dynamicParameter.Parameter("maxValue", 1.0)

    @DoubleParameter("curve", 0.1, 4.0, order = 3)
    var curve: Double by dynamicParameter.Parameter("curve", 1.0)

    @DoubleParameter("amount", 0.0, 1.0, order = 4)
    var amount: Double by dynamicParameter.Parameter("amount", 1.0)

    init {
        dynamicParameter.parameter("colormap", colormap.ordinal)
    }
}

/**
 * Adjusts colors in OKLCh: adds [lightness], scales the chroma by [chroma] and turns the hue by [hue] degrees. The
 * colors are read and written as sRGB.
 */
class OklchAdjust : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        vec3 lab = oklab_from_linear(oklab_srgb_to_linear(c.rgb));
        float chroma = length(lab.yz) * p_chroma;
        float hue = atan(lab.z, lab.y) + radians(p_hue);
        lab = vec3(lab.x + p_lightness, chroma * cos(hue), chroma * sin(hue));
        return vec4(oklab_linear_to_srgb(oklab_to_linear(lab)), c.a);
        """.trimIndent(), listOf(oklabLibrary)
    )

    @DoubleParameter("lightness", -1.0, 1.0, order = 0)
    var lightness: Double by dynamicParameter.Parameter("lightness", 0.0)

    @DoubleParameter("chroma", 0.0, 2.0, order = 1)
    var chroma: Double by dynamicParameter.Parameter("chroma", 1.0)

    @DoubleParameter("hue", -180.0, 180.0, order = 2)
    var hue: Double by dynamicParameter.Parameter("hue", 0.0)
}

/**
 * Levels: maps [inBlack]..[inWhite] to 0..1, applies [gamma], then maps 0..1 to [outBlack]..[outWhite]
 */
class Levels : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        vec3 v = clamp((c.rgb - p_inBlack) / max(p_inWhite - p_inBlack, 1e-5), 0.0, 1.0);
        v = pow(v, vec3(1.0 / p_gamma));
        return vec4(mix(vec3(p_outBlack), vec3(p_outWhite), v), c.a);
        """.trimIndent()
    )

    @DoubleParameter("in black", 0.0, 1.0, order = 0)
    var inBlack: Double by dynamicParameter.Parameter("inBlack", 0.0)

    @DoubleParameter("in white", 0.0, 1.0, order = 1)
    var inWhite: Double by dynamicParameter.Parameter("inWhite", 1.0)

    @DoubleParameter("gamma", 0.1, 4.0, order = 2)
    var gamma: Double by dynamicParameter.Parameter("gamma", 1.0)

    @DoubleParameter("out black", 0.0, 1.0, order = 3)
    var outBlack: Double by dynamicParameter.Parameter("outBlack", 0.0)

    @DoubleParameter("out white", 0.0, 1.0, order = 4)
    var outWhite: Double by dynamicParameter.Parameter("outWhite", 1.0)
}

/**
 * Blends the colors of [source] over the colors with [blend]
 */
class Layer(source: SourcePreset = NoiseSource(), blend: BlendPreset = LayerBlend()) : ColorPreset() {
    override val dynamicParameter: GlslFunction =
        glslFunction<Vector4>("vec4 c, vec2 uv", "p_blend(c, p_source(uv))")

    @NestedParameter("source", order = 0)
    var source: SourcePreset by dynamicParameter.presetSlot("source", SourcePreset.CONTRACT, source)

    @NestedParameter("blend", order = 1)
    var blend: BlendPreset by dynamicParameter.presetSlot("blend", BlendPreset.CONTRACT, blend)
}
