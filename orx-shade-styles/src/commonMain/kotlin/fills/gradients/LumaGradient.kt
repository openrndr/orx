package org.openrndr.extra.shadestyles.fills.gradients

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector4
import kotlin.reflect.KClass

open class LumaGradient<C>(
    colorType: KClass<C>,
    minLevel: Double,
    maxLevel: Double,
    colors: Array<Vector4>,
    points: Array<Double> = Array(colors.size) { it / (colors.size - 1.0) },
    structure: GradientBaseStructure

) : GradientBase<C>(
    colorType,
    colors,
    points,
    structure
)
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {

    var minLevel: Double by Parameter()
    var maxLevel: Double by Parameter()

    init {
        this.minLevel = minLevel
        this.maxLevel = maxLevel
    }

    companion object {
        val gradientFunction = """
            float gradientFunction(vec2 coord) {
                float f =  0.2126 * g_fill.r + 0.7152 * g_fill.g + 0.0722 * g_fill.b;
                f = (f - p_minLevel) / (p_maxLevel - p_minLevel);
                return f;
            }
            """.trimIndent()
    }
}

class LumaGradientBuilder<C>(private val gradientBuilder: GradientBuilder<C>) : GradientShadeStyleBuilder<C>
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {

    /**
     * Specifies the minimum luminance level for manipulating the gradient's color representation.
     * This value determines the lower bound of the luminance range applied to the gradient transformation.
     * It is used in combination with `maxLevel` to define the range of luminance levels that
     * affect the final gradient appearance.
     *
     * The default value is `0.0`, which represents the minimum possible luminance level.
     */
    var minLevel = 0.0


    /**
     * Specifies the maximum luminance level for manipulating the gradient's color representation.
     * This value determines the upper bound of the luminance range applied to the gradient transformation.
     * It is used in combination with `minLevel` to define the range of luminance levels that affect
     * the final gradient appearance.
     *
     * The default value is `1.0`, which represents the maximum possible luminance level.
     */
    var maxLevel = 1.0

    override fun build(): GradientBase<C> {
        val (stops, colors) = gradientBuilder.extractStepsUnzip()
        return LumaGradient(
            gradientBuilder.colorType,
            minLevel,
            maxLevel,
            colors,
            stops,
            gradientBuilder.structure()
        )
    }
}
