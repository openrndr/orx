@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles.presets

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.GlslFunction
import org.openrndr.draw.glslFunction
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.OptionParameter
import org.openrndr.extra.parameters.Vector2Parameter
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4

/**
 * A color stop of a gradient: [color] at [offset], from 0 to 1 along the gradient
 */
data class ColorStop(val offset: Double, val color: ColorRGBa)

/** what a gradient does beyond its first and last stop */
enum class GradientSpread {
    /** the colors of the first and last stop continue */
    PAD,

    /** the gradient repeats mirrored */
    REFLECT,

    /** the gradient repeats */
    REPEAT
}

/** the color space that the colors of a gradient are interpolated in */
enum class GradientInterpolation {
    /** sRGB, as SVG and CSS interpolate */
    SRGB,

    /** linear RGB */
    LINEAR_RGB,

    /** OKLab */
    OKLAB
}

/**
 * The base of gradients with color stops: the gradient function `t` of a coordinate picks a color from the stops.
 * The code of the gradient depends on the number of stops, a gradient with another number of stops compiles another
 * shader.
 */
abstract class StopsGradientPaint(stops: List<ColorStop>) : SourcePreset() {
    /** the color stops, sorted by offset when they are set */
    var stops: List<ColorStop> = sortStops(stops)
        set(value) {
            field = sortStops(value)
        }

    @OptionParameter("spread", order = 100)
    var spread: GradientSpread = GradientSpread.PAD

    @OptionParameter("interpolation", order = 101)
    var interpolation: GradientInterpolation = GradientInterpolation.SRGB

    /** GLSL code that sets `float t` from `vec2 uv` */
    protected abstract val gradientCode: String

    /** sets the parameters of the gradient on [function] */
    protected abstract fun setParameters(function: GlslFunction)

    private val functions = mutableMapOf<Int, GlslFunction>()

    override val dynamicParameter: GlslFunction
        get() {
            val stops = stops.ifEmpty { listOf(ColorStop(0.0, ColorRGBa.TRANSPARENT)) }
            val function = functions.getOrPut(stops.size) { buildFunction(stops.size) }
            for ((i, stop) in stops.withIndex()) {
                function.parameter("o$i", stop.offset)
                function.parameter("c$i", stop.color)
            }
            function.parameter("spread", spread.ordinal)
            function.parameter("interpolation", interpolation.ordinal)
            setParameters(function)
            return function
        }

    private fun buildFunction(count: Int): GlslFunction {
        val code = buildString {
            appendLine(gradientCode)
            appendLine("if (p_spread == 0) { t = clamp(t, 0.0, 1.0); }")
            appendLine("else if (p_spread == 1) { t = 1.0 - abs(mod(t, 2.0) - 1.0); }")
            appendLine("else { t = fract(t); }")
            appendLine("vec4 c = paint_to_space(p_c0, p_interpolation);")
            for (i in 1 until count) {
                appendLine(
                    "if (t > p_o${i - 1}) { c = t >= p_o$i ? paint_to_space(p_c$i, p_interpolation) : " +
                            "mix(paint_to_space(p_c${i - 1}, p_interpolation), paint_to_space(p_c$i, p_interpolation), " +
                            "(t - p_o${i - 1}) / max(p_o$i - p_o${i - 1}, 1e-6)); }"
                )
            }
            append("return paint_from_space(c, p_interpolation);")
        }
        return glslFunction<Vector4>("vec2 uv", code, listOf(gradientSpacesLibrary))
    }

    private companion object {
        fun sortStops(stops: List<ColorStop>): List<ColorStop> {
            // offsets that are smaller than a previous offset are set to it, as in SVG
            var last = 0.0
            return stops.map { stop ->
                val offset = stop.offset.coerceIn(0.0, 1.0).coerceAtLeast(last)
                last = offset
                stop.copy(offset = offset)
            }
        }
    }
}

/** GLSL library that converts linear RGB colors to and from the interpolation spaces of [GradientInterpolation] */
val gradientSpacesLibrary = org.openrndr.draw.glslLibrary(
    "paint_spaces", """
vec4 paint_to_space(vec4 c, int space) {
    if (space == 0) {
        return vec4(oklab_linear_to_srgb(c.rgb), c.a);
    } else if (space == 2) {
        return vec4(oklab_from_linear(c.rgb), c.a);
    }
    return c;
}

vec4 paint_from_space(vec4 c, int space) {
    if (space == 0) {
        return vec4(oklab_srgb_to_linear(c.rgb), c.a);
    } else if (space == 2) {
        return vec4(oklab_to_linear(c.rgb), c.a);
    }
    return c;
}
""", oklabLibrary
)

/**
 * A linear gradient from [start] (offset 0) to [end] (offset 1), like SVG's linearGradient
 */
class LinearGradientPaint(
    start: Vector2 = Vector2(0.0, 0.0),
    end: Vector2 = Vector2(1.0, 0.0),
    stops: List<ColorStop> = listOf(ColorStop(0.0, ColorRGBa.BLACK), ColorStop(1.0, ColorRGBa.WHITE)),
    spread: GradientSpread = GradientSpread.PAD
) : StopsGradientPaint(stops) {
    @Vector2Parameter("start", 0.0, 1.0, order = 0)
    var start: Vector2 = start

    @Vector2Parameter("end", 0.0, 1.0, order = 1)
    var end: Vector2 = end

    init {
        this.spread = spread
    }

    override val gradientCode = """
        vec2 d = p_end - p_start;
        float t = dot(uv - p_start, d) / max(dot(d, d), 1e-12);
    """.trimIndent()

    override fun setParameters(function: GlslFunction) {
        function.parameter("start", start)
        function.parameter("end", end)
    }
}

/**
 * A radial gradient, like SVG's radialGradient: offset 0 is the circle at [focal] with radius [focalRadius], offset
 * 1 the circle at [center] with [radius], the circles in between are interpolated
 */
class RadialGradientPaint(
    center: Vector2 = Vector2(0.5, 0.5),
    radius: Double = 0.5,
    focal: Vector2 = center,
    focalRadius: Double = 0.0,
    stops: List<ColorStop> = listOf(ColorStop(0.0, ColorRGBa.WHITE), ColorStop(1.0, ColorRGBa.BLACK)),
    spread: GradientSpread = GradientSpread.PAD
) : StopsGradientPaint(stops) {
    @Vector2Parameter("center", 0.0, 1.0, order = 0)
    var center: Vector2 = center

    @DoubleParameter("radius", 0.0, 2.0, order = 1)
    var radius: Double = radius

    @Vector2Parameter("focal", 0.0, 1.0, order = 2)
    var focal: Vector2 = focal

    @DoubleParameter("focal radius", 0.0, 2.0, order = 3)
    var focalRadius: Double = focalRadius

    init {
        this.spread = spread
    }

    // the largest t for which uv is on the circle at mix(focal, center, t) with radius mix(focalRadius, radius, t)
    override val gradientCode = """
        vec2 cd = p_center - p_focal;
        float dr = p_radius - p_focalRadius;
        vec2 pd = uv - p_focal;
        float qa = dot(cd, cd) - dr * dr;
        float qb = dot(pd, cd) + p_focalRadius * dr;
        float qc = dot(pd, pd) - p_focalRadius * p_focalRadius;
        float t;
        if (abs(qa) < 1e-9) {
            t = qc / (2.0 * qb);
        } else {
            float disc = qb * qb - qa * qc;
            if (disc < 0.0) {
                return vec4(0.0);
            }
            float s = sqrt(disc);
            t = (qb + s) / qa;
            if (p_focalRadius + t * dr < 0.0) {
                t = (qb - s) / qa;
            }
        }
        if (p_focalRadius + t * dr < 0.0) {
            return vec4(0.0);
        }
    """.trimIndent()

    override fun setParameters(function: GlslFunction) {
        function.parameter("center", center)
        function.parameter("radius", radius)
        function.parameter("focal", focal)
        function.parameter("focalRadius", focalRadius)
    }
}
