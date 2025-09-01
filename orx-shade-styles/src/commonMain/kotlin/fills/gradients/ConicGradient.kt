package org.openrndr.extra.shadestyles.fills.gradients

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import kotlin.math.PI
import kotlin.reflect.KClass

open class ConicGradient<C>(
    colorType: KClass<C>,
    center: Vector2 = Vector2(0.5, 0.5),
    rotation: Double = 0.0,
    angle: Double = 0.0,
    startAngle: Double = 0.0,
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

    var angle: Double by Parameter()
    var startAngle: Double by Parameter()
    var center: Vector2 by Parameter()
    var rotation: Double by Parameter()

    init {
        this.center = center
        this.startAngle = startAngle
        this.angle = angle
        this.rotation = rotation
    }

    companion object {
        val gradientFunction = """
        float gradientFunction(vec2 coord) {
            vec2 d0 = coord - p_center;
            float angle = atan(d0.y, d0.x);
            angle += ${PI};
            angle /= ${2.0 * PI};
            angle += p_rotation / 360.0;
            angle = mod(angle, 1.0);
            angle *= p_angle / 360.0;
            angle += $PI * p_startAngle / 180.0;
            return angle;
       }
        """.trimIndent()
    }
}

class ConicGradientBuilder<C>(private val gradientBuilder: GradientBuilder<C>) : GradientShadeStyleBuilder<C>
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {

    /**
     * Specifies the center point for the gradient.
     * When using BOUNDS coordinates, the coordinates are normalized, where (0,0) represents the top-left corner and (1,1)
     * represents the bottom-right corner. The default value is set to `Vector2(0.5, 0.5)`, which corresponds to the center
     * of the gradient's bounding box.
     */
    var center = Vector2(0.5, 0.5)

    /**
     * Defines the angular extent of the conic gradient in degrees.
     * By default, it is set to 360.0 degrees, representing a full circular gradient.
     * Adjusting this value can control the gradient's angular sweep, with values ranging between 0 and 360.
     * Negative values or values exceeding 360 might have no effect or be clamped depending on implementation.
     */
    var angle: Double = 360.0

    /**
     * Specifies the starting angle of the conic gradient in degrees.
     * This value determines the initial orientation of the gradient's angular sweep.
     * By default, it is set to `0.0` degrees, which aligns with a standard reference point.
     * You can adjust this value to rotate the gradient's starting position around the center.
     */
    var startAngle: Double = 0.0

    /**
     * Defines the rotation angle of the conic gradient in degrees.
     * This value applies a global rotation to the gradient, rotating it around its center point.
     * By default, it is set to `0.0` degrees, meaning no rotation is applied.
     * Modifying this value allows for tilting the gradient's angular orientation to achieve
     * specific visual effects or alignments.
     */
    var rotation: Double = 0.0

    override fun build(): GradientBase<C> {
        val (stops, colors) = gradientBuilder.extractStepsUnzip()
        return ConicGradient<C>(
            gradientBuilder.colorType,
            center,
            rotation,
            angle,
            startAngle,
            colors,
            stops,
            gradientBuilder.structure()
        )
    }
}
