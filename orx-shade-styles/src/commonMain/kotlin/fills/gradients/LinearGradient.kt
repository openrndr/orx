package org.openrndr.extra.shadestyles.fills.gradients

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import kotlin.reflect.KClass

open class LinearGradient<C>(
    colorType: KClass<C>,
    start: Vector2 = Vector2.ZERO,
    end: Vector2 = Vector2.ONE,
    colors: Array<Vector4>,
    points: Array<Double> = Array(colors.size) { it / (colors.size - 1.0) },
    structure: GradientBaseStructure,
) : GradientBase<C>(
    colorType,
    colors,
    points,
    structure
)
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {

    var start: Vector2 by Parameter()
    var end: Vector2 by Parameter()

    init {
        this.start = start
        this.end = end
    }

    companion object {
        val gradientFunction = """
            float gradientFunction(vec2 coord) {
            
            vec2 d0 = coord - p_start;
            vec2 dl = p_end - p_start;
            
            float d0l = length(d0);
            float dll = length(dl);
            float f = dot(d0, dl) / (dll * dll);

            return f;
        }
        """.trimIndent()
    }
}

class LinearGradientBuilder<C>(private val gradientBuilder: GradientBuilder<C>) : GradientShadeStyleBuilder<C>
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {

    /**
     * Specifies the start point of the linear gradient.
     * The coordinate values are normalized when using BOUNDS coordinates,
     * where (0,0) represents the top-left corner and (1,1) represents the bottom-right corner of the gradient's bounding box.
     * The default value is set to `Vector2(0.0, 0.5)`, which places the start point at the left middle edge of the bounding box.
     */
    var start = Vector2(0.0, 0.5)

    /**
     * Specifies the end point of the linear gradient.
     * The coordinate values are normalized when using BOUNDS coordinates, where (0,0) represents
     * the top-left corner and (1,1) represents the bottom-right corner of the gradient's bounding box.
     * The default value is set to `Vector2(1.0, 0.5)`, which places the end point at the right middle edge
     * of the bounding box.
     */
    var end = Vector2(1.0, 0.5)

    override fun build(): GradientBase<C> {
        val (stops, colors) = gradientBuilder.extractStepsUnzip()
        return LinearGradient<C>(gradientBuilder.colorType, start, end, colors, stops, gradientBuilder.structure())
    }
}
