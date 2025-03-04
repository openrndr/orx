package org.openrndr.extra.shadestyles.fills.gradients

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.extra.shaderphrases.sdf.sdStarPhrase
import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import kotlin.math.PI
import kotlin.reflect.KClass

open class StellarGradient<C>(
    colorType: KClass<C>,
    center: Vector2 = Vector2(0.5, 0.5),
    radius: Double = 1.0,
    sharpness: Double = 0.5,
    rotation: Double = 0.0,
    sides: Int = 3,
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

    var sides: Int by Parameter()
    var radius: Double by Parameter()
    var center: Vector2 by Parameter()
    var sharpness: Double by Parameter()
    var rotation: Double by Parameter()

    init {
        this.sides = sides
        this.center = center
        this.radius = radius
        this.sharpness = sharpness
        this.rotation = rotation
    }

    companion object {
        val gradientFunction = """$sdStarPhrase
            float gradientFunction(vec2 coord) {
                vec2 d0 = coord - p_center;
                d0 = rotate2D(d0, p_rotation);
                float f = sdStar(d0 / p_radius, 1.0, p_sides, p_sharpness);
                float f0 = sdStar(vec2(0.0), 1.0, p_sides, p_sharpness);
                f -= f0;
                f /= 0.5 * 1.0 * (1.0 + cos($PI / float(p_sides)));
                return f;
            }
            """.trimIndent()
    }
}

class StellarGradientBuilder<C>(private val gradientBuilder: GradientBuilder<C>) : GradientShadeStyleBuilder<C>
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {

    /**
     * Specifies the center point of the gradient.
     * The center is defined in normalized coordinates where (0, 0) represents the top-left corner
     * and (1, 1) represents the bottom-right corner of the gradient's bounding box.
     * The default value is `Vector2(0.5, 0.5)`, which corresponds to the center of the gradient.
     */
    var center = Vector2(0.5, 0.5)
    var radius = 0.5

    /**
     * Specifies the number of sides for the star pattern used in the gradient.
     * This property controls the symmetry and appearance of the resulting gradient.
     * Higher values produce shapes with more sides, contributing to more intricate patterns,
     * while lower values result in simpler, less detailed designs.
     * The default value is set to `6`.
     */
    var sides = 6

    /**
     * Determines the sharpness of the star shape. Maximum value is `1.0` which will produce pointy stars.
     * Values closer to `0.0` result in smoother, star shapes. A value of `0.0` will result in a regular polygon shape.
     * The default value is `0.5`.
     */
    var sharpness = 0.5

    /**
     * Specifies the rotation angle of the gradient in degrees.
     * This property adjusts the overall rotation of the gradient around its center point.
     * By default, the value is set to `0.0` degrees, indicating no rotation.
     * Modifying this value allows the gradient's orientation to be tilted, enabling various aesthetic effects.
     */
    var rotation = 0.0

    override fun build(): GradientBase<C> {
        val (stops, colors) = gradientBuilder.extractStepsUnzip()
        return StellarGradient(
            gradientBuilder.colorType,
            center,
            radius,
            sharpness,
            rotation,
            sides,
            colors,
            stops,
            gradientBuilder.structure()
        )
    }
}
