@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles

import org.openrndr.color.AlgebraicColor
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ConvertibleToColorRGBa
import org.openrndr.draw.ShadeStyle
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.extra.color.phrases.ColorPhraseBook
import org.openrndr.extra.color.spaces.ColorOKLABa
import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector2

@Deprecated("use gradient {} instead")
@Description("Multicolor linear gradient")
open class NPointLinearGradientBase<C>(
    colors: Array<C>,
    points: Array<Double> = Array(colors.size) { it / (colors.size - 1.0) },
    offset: Vector2 = Vector2.ZERO,
    rotation: Double = 0.0
) : ShadeStyle()
        where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C: CastableToVector4 {

    var colors: Array<C> by Parameter()

    // Sorted normalized values defining relative positions of colors
    var points: Array<Double> by Parameter()
    var offset: Vector2 by Parameter()
    var rotation: Double by Parameter()

    init {
        ColorPhraseBook.register()
        this.colors = colors
        this.points = points
        this.offset = offset
        this.rotation = rotation

        fragmentPreamble = """
            |#pragma import color.oklab_to_linear_rgb
            |#pragma import color.linear_rgb_to_srgb""".trimMargin().preprocess()
        fragmentTransform = """
            vec2 coord = (c_boundsPosition.xy - 0.5 + p_offset);
            
            float cr = cos(radians(p_rotation));
            float sr = sin(radians(p_rotation));
            mat2 rm = mat2(cr, -sr, sr, cr);
            vec2 rc = rm * coord;
            float f = clamp(rc.y + 0.5, 0.0, 1.0);            
 
            int i=0;
            while(i < p_points_SIZE - 1 && f >= p_points[i+1]) { i++; }
            
            vec4 color0 = p_colors[i];
            vec4 color1 = p_colors[i+1]; 
            
            float g = (f - p_points[i]) / (p_points[i+1] - p_points[i]);
            vec4 gradient = mix(color0, color1, clamp(g, 0.0, 1.0)); 
            
            ${generateColorTransform(colors[0]::class, "gradient")}
            
            x_fill *= gradient;
            if (x_fill.a != 0) {
                x_fill.rgb /= x_fill.a;
            }

        """
    }
}

class NPointLinearGradient(
    colors: Array<ColorRGBa>,
    points: Array<Double> = Array(colors.size) { it / (colors.size - 1.0) },
    offset: Vector2 = Vector2.ZERO,
    rotation: Double = 0.0
) : NPointLinearGradientBase<ColorRGBa>(colors, points, offset, rotation)

class NPointLinearGradientOKLab(
    colors: Array<ColorOKLABa>,
    points: Array<Double> = Array(colors.size) { it / (colors.size - 1.0) },
    offset: Vector2 = Vector2.ZERO,
    rotation: Double = 0.0
) : NPointLinearGradientBase<ColorOKLABa>(colors, points, offset, rotation)


