package org.openrndr.extra.shadestyles.fills.gradients

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.extra.shaderphrases.sdf.sdEllipsePhrase
import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import kotlin.math.PI
import kotlin.reflect.KClass

class EllipticalGradient<C>(
    colorType: KClass<C>,
    center: Vector2 = Vector2(0.5, 0.5),
    radiusX: Double = 1.0,
    radiusY: Double = 1.0,
    rotation: Double = 0.0,
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

    var radiusX: Double by Parameter()
    var radiusY: Double by Parameter()
    var center: Vector2 by Parameter()
    var rotation: Double by Parameter()

    init {
        this.radiusX = radiusX
        this.radiusY = radiusY
        this.center = center
        this.rotation = rotation
    }

    companion object {
        val gradientFunction = """$sdEllipsePhrase
        float gradientFunction(vec2 coord) {
            vec2 d0 = coord - p_center;
            d0 = rotate2D(d0, p_rotation);
            
            float minRadius = min(p_radiusX, p_radiusY);
            float maxRadius = max(p_radiusX, p_radiusY);
            
            if (minRadius < 1E-3) {
                return 1.0;
            } else  if (abs(p_radiusX - p_radiusY) < 1E-4) {
                
                float d0l = length(d0);
                float f = d0l;// / p_radiusX;
                return f / p_radiusX;
            } else {
                                
                float f = sdEllipse(d0, vec2(p_radiusX, p_radiusY) ) / minRadius;
                float f0 = sdEllipse(vec2(0.001, 0.001), vec2(p_radiusX, p_radiusY)) / minRadius;
                f -= f0;
                return f;
            }
       }
        """.trimIndent()
    }
}

class EllipticalGradientBuilder<C>(private val gradientBuilder: GradientBuilder<C>) : GradientShadeStyleBuilder<C>
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {


    /**
     * Specifies the center point for the radial gradient.
     *
     * The `center` represents the normalized coordinates within the bounds of the gradient's area.
     * When using BOUNDS coordinates a value of `Vector2(0.5, 0.5)` corresponds to the geometric center of the gradient's
     * bounds. The coordinates are normalized, where (0,0) is the top-left corner and (1,1) is the bottom-right corner.
     * This value determines the starting position for the radial gradient effect.
     */
    var center = Vector2(0.5, 0.5)


    /**
     * Specifies the radius of the radial gradient.
     *
     * The `radius` determines the extent of the gradient from the center point outward.
     *
     * When using BOUNDS coordinates it is expressed as a normalized value where `0.0` represents no radius
     * (a single point at the center) and `1.0` corresponds to the full extent to the edge of the gradient's bounding area.
     * Adjusting this value alters the size and spread of the gradient.
     */
    var radiusX = 0.5
    var radiusY = 0.5

    var rotation = 0.0
    /**
     * Specifies the focal center point for the radial gradient.
     *
     * The `focalCenter` defines an additional center point for the radial gradient,
     * allowing for more complex and visually distinct gradient effects compared to the default center.
     * If not explicitly set, it defaults to the same value as the `center`.
     *
     * This property can be used to create focused or offset gradient patterns by positioning
     * the focal center differently relative to the main center point. The coordinates can
     * be normalized within the bounds, where (0,0) represents the top-left corner and (1,1)
     * represents the bottom-right corner.
     */
    var focalCenter: Vector2? = null
    var focalRadius: Double? = null

    override fun build(): GradientBase<C> {
        val (stops, colors) = gradientBuilder.extractStepsUnzip()
        return EllipticalGradient(
            gradientBuilder.colorType,
            center,
            radiusX,
            radiusY,
            rotation,
            colors,
            stops,
            gradientBuilder.structure()
        )
    }
}
