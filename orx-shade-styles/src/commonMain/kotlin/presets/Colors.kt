@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.GlslFunction
import org.openrndr.draw.glslFunction
import org.openrndr.extra.color.presets.CORAL
import org.openrndr.extra.color.presets.NAVY
import org.openrndr.extra.parameters.BooleanParameter
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

/**
 * Sepia toning, by [amount]
 */
class Sepia : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        float a = p_amount;
        return vec4(
            min(1.0, c.r * (1.0 - 0.607 * a) + c.g * (0.769 * a) + c.b * (0.189 * a)),
            min(1.0, c.r * (0.349 * a) + c.g * (1.0 - 0.314 * a) + c.b * (0.168 * a)),
            min(1.0, c.r * (0.272 * a) + c.g * (0.534 * a) + c.b * (1.0 - 0.869 * a)),
            c.a
        );
        """.trimIndent()
    )

    @DoubleParameter("amount", 0.0, 1.0)
    var amount: Double by dynamicParameter.Parameter("amount", 0.5)
}

/**
 * Maps the luma of the colors from [background] to [foreground], interpolated in OKLab when [labInterpolation]
 */
class Duotone : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        float t = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
        vec3 rgb;
        if (p_labInterpolation) {
            vec3 bg = oklab_from_linear(oklab_srgb_to_linear(p_background.rgb));
            vec3 fg = oklab_from_linear(oklab_srgb_to_linear(p_foreground.rgb));
            rgb = oklab_linear_to_srgb(oklab_to_linear(mix(bg, fg, t)));
        } else {
            rgb = mix(p_background.rgb, p_foreground.rgb, t);
        }
        return vec4(rgb, c.a);
        """.trimIndent(), listOf(oklabLibrary)
    )

    @ColorParameter("background", order = 0)
    var background: ColorRGBa by dynamicParameter.Parameter("background", ColorRGBa.NAVY)

    @ColorParameter("foreground", order = 1)
    var foreground: ColorRGBa by dynamicParameter.Parameter("foreground", ColorRGBa.CORAL)

    @BooleanParameter("LAB interpolation", order = 2)
    var labInterpolation: Boolean by dynamicParameter.Parameter("labInterpolation", true)
}

/**
 * [foreground] where the luma is at least [threshold], [background] elsewhere
 */
class LumaThreshold : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        float luma = dot(c.rgb, vec3(1.0 / 3.0));
        return mix(p_background * p_backgroundOpacity, p_foreground * p_foregroundOpacity, step(p_threshold, luma));
        """.trimIndent()
    )

    @DoubleParameter("threshold", 0.0, 1.0, order = 0)
    var threshold: Double by dynamicParameter.Parameter("threshold", 0.5)

    @ColorParameter("foreground", order = 1)
    var foreground: ColorRGBa by dynamicParameter.Parameter("foreground", ColorRGBa.WHITE)

    @DoubleParameter("foreground opacity", 0.0, 1.0, order = 2)
    var foregroundOpacity: Double by dynamicParameter.Parameter("foregroundOpacity", 1.0)

    @ColorParameter("background", order = 3)
    var background: ColorRGBa by dynamicParameter.Parameter("background", ColorRGBa.BLACK)

    @DoubleParameter("background opacity", 0.0, 1.0, order = 4)
    var backgroundOpacity: Double by dynamicParameter.Parameter("backgroundOpacity", 1.0)
}

/** the masks of [Dither] */
enum class DitherPattern {
    XOR,
    XOR_CHANNELS,
    ADD,
    ADD_CHANNELS
}

/**
 * Ordered dithering to [levels] levels per channel with a [pattern] from "arithmetic dither". The pattern is
 * aligned to the pixels of the output.
 */
class Dither : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        vec4 u = c;
        if (u.a > 0.0) {
            u.rgb /= u.a;
        }
        ivec2 p = ivec2(gl_FragCoord.xy);
        vec4 d = vec4(
            a_dither(p_pattern, p_levels, u.r, p, 0),
            a_dither(p_pattern, p_levels, u.g, p, 1),
            a_dither(p_pattern, p_levels, u.b, p, 2),
            a_dither(p_pattern, p_levels, u.a, p, 3)
        );
        d.rgb *= d.a;
        return d;
        """.trimIndent(), listOf(aDitherLibrary)
    )

    @OptionParameter("pattern", order = 0)
    var pattern: DitherPattern = DitherPattern.ADD_CHANNELS
        set(value) {
            field = value
            dynamicParameter.parameter("pattern", value.ordinal)
        }

    @IntParameter("levels", 1, 16, order = 1)
    var levels: Int by dynamicParameter.Parameter("levels", 4)

    init {
        dynamicParameter.parameter("pattern", pattern.ordinal)
    }
}

/**
 * Film grain. [time] animates the grain at [rate] changes per second; [strength] and [pitch] set the strength and
 * size of the grain, [liftRatio] mixes multiplied grain (0) with added grain (1). Colored grain is desaturated by
 * [colorLevel]. The grain is aligned to the pixels of the output.
 */
class FilmGrain : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        vec2 x = gl_FragCoord.xy;
        float t = p_time;
        vec3 grain;
        if (p_useColor) {
            float rg = film_grain_source(vec3(x, floor(p_rate * t)), p_strength, p_pitch);
            float gg = film_grain_source(vec3(x, floor(p_rate * (t + 9.0))), p_strength, p_pitch);
            float bg = film_grain_source(vec3(x, floor(p_rate * (t - 9.0))), p_strength, p_pitch);
            vec3 colorGrain = vec3(rg, gg, bg);
            grain = mix(vec3(dot(colorGrain, vec3(0.2126, 0.7152, 0.0722))), colorGrain, p_colorLevel);
        } else {
            grain = vec3(film_grain_source(vec3(x, floor(p_rate * t)), p_strength / sqrt(2.0), p_pitch));
        }
        return vec4(max(mix(c.rgb * grain, c.rgb + (grain - 1.0), p_liftRatio), 0.0), c.a);
        """.trimIndent(), listOf(filmGrainLibrary)
    )

    /** time in seconds, default is 0.0 */
    var time: Double by dynamicParameter.Parameter("time", 0.0)

    @BooleanParameter("use color", order = 0)
    var useColor: Boolean by dynamicParameter.Parameter("useColor", false)

    @DoubleParameter("lift ratio", 0.0, 1.0, order = 1)
    var liftRatio: Double by dynamicParameter.Parameter("liftRatio", 0.5)

    @DoubleParameter("strength", 0.0, 1.0, order = 2)
    var strength: Double by dynamicParameter.Parameter("strength", 1.0)

    @DoubleParameter("rate", 0.0, 60.0, order = 3)
    var rate: Double by dynamicParameter.Parameter("rate", 1.0)

    @DoubleParameter("pitch", 0.0, 1.0, order = 4)
    var pitch: Double by dynamicParameter.Parameter("pitch", 1.0)

    @DoubleParameter("color level", 0.0, 1.0, order = 5)
    var colorLevel: Double by dynamicParameter.Parameter("colorLevel", 1.0)
}

/**
 * A halftone of the luma: black where the luma times a pattern of cosine lines is below [threshold], white
 * elsewhere, or the other way around when [invert]. The pattern, lines at [freq0] with a cosine wobble of [freq1] and
 * [gain1], turned by [rotation] degrees, is antialiased by sampling it 7 by 7 times per pixel.
 */
class LumaHalftone : ColorPreset() {
    override val dynamicParameter = glslFunction<Vector4>(
        "vec4 c, vec2 uv", """
        vec2 texel = vec2(abs(dFdx(uv).x) + abs(dFdy(uv).x), abs(dFdx(uv).y) + abs(dFdy(uv).y));
        float ca = cos(radians(p_rotation));
        float sa = sin(radians(p_rotation));
        float aspect = texel.y / texel.x;
        mat2 rm = mat2(1.0, 0.0, 0.0, aspect) * mat2(vec2(ca, sa), vec2(-sa, ca)) * mat2(1.0, 0.0, 0.0, 1.0 / aspect);
        vec4 u = c;
        if (u.a != 0.0) {
            u.rgb /= u.a;
        }
        float l = dot(vec3(1.0 / 3.0), u.rgb);
        if (p_invert) {
            l = 1.0 - l;
        }
        vec2 delta = texel / 6.0;
        float sum = 0.0;
        for (int v = -3; v <= 3; ++v) {
            for (int w = -3; w <= 3; ++w) {
                vec2 cuv = rm * (uv + delta * vec2(w, v) - vec2(0.5)) + vec2(0.5);
                float m = fract(p_phase0 + cuv.x * p_freq0 + cos(cuv.y * p_freq1 + p_phase1 * 3.141592653) * p_gain1);
                sum += step(p_threshold, l * m);
            }
        }
        float t = sum / 49.0;
        if (p_invert) {
            t = 1.0 - t;
        }
        return vec4(vec3(t), 1.0) * c.a;
        """.trimIndent()
    )

    @DoubleParameter("threshold", 0.0, 1.0, order = 0)
    var threshold: Double by dynamicParameter.Parameter("threshold", 0.5)

    @DoubleParameter("rotation", -180.0, 180.0, order = 1)
    var rotation: Double by dynamicParameter.Parameter("rotation", 0.0)

    @DoubleParameter("freq0", 0.0, 200.0, order = 2)
    var freq0: Double by dynamicParameter.Parameter("freq0", 10.0)

    @DoubleParameter("freq1", 0.0, 200.0, order = 3)
    var freq1: Double by dynamicParameter.Parameter("freq1", 20.0)

    @DoubleParameter("gain1", 0.0, 1.0, order = 4)
    var gain1: Double by dynamicParameter.Parameter("gain1", 0.1)

    @DoubleParameter("phase0", -1.0, 1.0, order = 5)
    var phase0: Double by dynamicParameter.Parameter("phase0", 0.0)

    @DoubleParameter("phase1", -1.0, 1.0, order = 6)
    var phase1: Double by dynamicParameter.Parameter("phase1", 0.0)

    @BooleanParameter("invert", order = 7)
    var invert: Boolean by dynamicParameter.Parameter("invert", true)
}

/**
 * Applies [color] where [mask] is 1, keeps the colors where it is 0, and mixes in between
 */
class Masked(color: ColorPreset = Grade(), mask: MaskPreset = RadialGradientMask()) : ColorPreset() {
    override val dynamicParameter: GlslFunction =
        glslFunction<Vector4>("vec4 c, vec2 uv", "mix(c, p_color(c, uv), clamp(p_mask(uv), 0.0, 1.0))")

    @NestedParameter("color", order = 0)
    var color: ColorPreset by dynamicParameter.presetSlot("color", CONTRACT, color)

    @NestedParameter("mask", order = 1)
    var mask: MaskPreset by dynamicParameter.presetSlot("mask", MaskPreset.CONTRACT, mask)
}
