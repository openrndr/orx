package org.openrndr.extra.shadestyles.fills.gradients

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.draw.ShadeStyle
import org.openrndr.extra.color.phrases.linearRgbToOklabPhrase
import org.openrndr.extra.color.phrases.oklabToLinearRgbPhrase
import org.openrndr.extra.shadestyles.fills.FillFit
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.generateColorTransform
import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import kotlin.math.PI
import kotlin.reflect.KClass


class GradientBaseStructure(
    val gradientFunction: String,
    val domainWarpFunction: String,
    val levelWarpFunction: String
)

open class GradientBase<C>(
    colorType: KClass<C>,
    colors: Array<Vector4>,
    points: Array<Double> = Array(colors.size) { it / (colors.size - 1.0) },
    structure: GradientBaseStructure
) : ShadeStyle()
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C : CastableToVector4 {

    var quantization: Int by Parameter()
    var colors: Array<Vector4> by Parameter()
    var points: Array<Double> by Parameter()
    var spreadMethod: Int by Parameter()
    var fillUnits: Int by Parameter()
    var fillFit: Int by Parameter()

    init {
        this.quantization = 0
        this.colors = colors
        this.points = points
        this.fillUnits = FillUnits.BOUNDS.ordinal
        this.spreadMethod = SpreadMethod.PAD.ordinal
        this.fillFit = FillFit.STRETCH.ordinal
        fragmentPreamble = """
            |vec2 rotate2D(vec2 x, float angle){
            |   float rad = angle / 180.0 * $PI;
            |   mat2 m = mat2(cos(rad),-sin(rad), sin(rad),cos(rad));
            |   return m * x;
            |}
            |$oklabToLinearRgbPhrase
            |$linearRgbToOklabPhrase
            |${structure.gradientFunction}
            |${structure.domainWarpFunction}
            |${structure.levelWarpFunction}
            |""".trimMargin()

        fragmentTransform = """
            vec2 coord = vec2(0.0);
            
            if (p_fillUnits == 0) { // BOUNDS
                coord = c_boundsPosition.xy;
                if (p_fillFit == 1) { // COVER
                    float mx = max(c_boundsSize.x, c_boundsSize.y);
                    float ar = mx / min(c_boundsSize.x, c_boundsSize.y);
                    if (c_boundsSize.x == mx) {
                        coord.y = (coord.y - 0.5) * ar + 0.5;   
                    } else {
                        coord.x = (coord.x - 0.5) * ar + 0.5;
                    }
                } else if (p_fillFit == 2) { // CONTAIN
                    float mx = max(c_boundsSize.x, c_boundsSize.y); 
                    float ar = min(c_boundsSize.x, c_boundsSize.y) / mx;
                    if (c_boundsSize.y == mx) {
                        coord.y = (coord.y - 0.5) * ar + 0.5;   
                    } else {
                        coord.x = (coord.x - 0.5) * ar + 0.5;
                    }
                }                
            } else if (p_fillUnits == 1) { // WORLD
                coord = v_worldPosition.xy;            
            } else if (p_fillUnits == 2) { // VIEW
                coord = v_viewPosition.xy;
            } else if (p_fillUnits == 3) { // SCREEN
                coord = c_screenPosition.xy;
                coord.y = u_viewDimensions.y - coord.y;
            }

            coord = domainWarp(coord);

            float f = gradientFunction(coord);
            f = levelWarp(coord, f);
            
            if (p_quantization != 0) {
                f = floor(f * float(p_quantization)) / (float(p_quantization) - 1.0);
            }
            
            if (p_spreadMethod == 0) { // PAD
                f = clamp(f, 0.0, 1.0);    
            } else if (p_spreadMethod == 1) { // REFLECT
                f = 2.0 * abs(f / 2.0 - floor(f / 2.0 + 0.5));
            } else if (p_spreadMethod == 2) { // REPEAT
                f = mod(f, 1.0);
            }

            int i = 0;
            while (i < p_points_SIZE - 1 && f >= p_points[i+1]) { i++; }
            
            vec4 color0 = p_colors[i];
            vec4 color1 = p_colors[i+1]; 
            
            float g = (f - p_points[i]) / (p_points[i+1] - p_points[i]);
            vec4 gradient = mix(color0, color1, clamp(g, 0.0, 1.0)); 
            
            ${generateColorTransform(colorType)}
            x_fill *= gradient;
        """
    }
}

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
