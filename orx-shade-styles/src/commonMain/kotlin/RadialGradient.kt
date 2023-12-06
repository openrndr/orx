@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.shadestyles

import org.openrndr.color.*
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.extra.color.phrases.ColorPhraseBook
import org.openrndr.extra.color.spaces.ColorOKLABa
import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector2

@Description("Radial gradient")
open class RadialGradientBase<C>(
        color0: C,
        color1: C,
        offset: Vector2 = Vector2.ZERO,
        rotation: Double = 0.0,
        length: Double = 1.0,
        exponent: Double = 1.0)
    : ShadeStyle()
where C : ConvertibleToColorRGBa, C : AlgebraicColor<C>, C: CastableToVector4 {
    @ColorParameter("start color", order = 0)
    var color0 : C by Parameter()
    @ColorParameter("end color", order = 1)
    var color1 : C by Parameter()
    var offset : Vector2 by Parameter()
    @DoubleParameter("rotation", -180.0, 180.0, order = 2)
    var rotation : Double by Parameter()
    @DoubleParameter("length", 0.0, 10.0)
    var length: Double by Parameter()
    @DoubleParameter("exponent", 0.01, 10.0, order = 3)
    var exponent: Double by Parameter()

    init {
        ColorPhraseBook.register()
        this.color0 = color0
        this.color1 = color1
        this.offset = offset
        this.rotation = rotation
        this.length = length
        this.exponent = exponent

        fragmentPreamble = """
            |#pragma import color.oklab_to_linear_rgb
            |#pragma import color.linear_rgb_to_srgb""".trimMargin().preprocess()
        fragmentTransform = """
            vec2 coord = (c_boundsPosition.xy - 0.5 + p_offset/2.0) * 2.0;
            
            float cr = cos(radians(p_rotation));
            float sr = sin(radians(p_rotation));
            mat2 rm = mat2(cr, -sr, sr, cr); 
            vec2 rc = rm * coord;
            float f =  clamp(p_length * length(rc), 0.0, 1.0);            

            vec4 color0 = p_color0;
            vec4 color1 = p_color1; 

            vec4 gradient = mix(color0, color1, pow(f, p_exponent));
            ${generateColorTransform(color0::class)}

            x_fill *= gradient;
        """
    }
}

class RadialGradient(
    color0: ColorRGBa,
    color1: ColorRGBa,
    offset: Vector2 = Vector2.ZERO,
    rotation: Double = 0.0,
    length: Double = 1.0,
    exponent: Double = 1.0
): RadialGradientBase<ColorRGBa>(color0, color1, offset, rotation, length, exponent)

class RadialGradientOKLab(
    color0: ColorOKLABa,
    color1: ColorOKLABa,
    offset: Vector2 = Vector2.ZERO,
    rotation: Double = 0.0,
    length: Double = 1.0,
    exponent: Double = 1.0
): RadialGradientBase<ColorOKLABa>(color0, color1, offset, rotation, length, exponent)

fun radialGradient(
        color0: ColorRGBa,
        color1: ColorRGBa,
        offset: Vector2 = Vector2.ZERO,
        rotation: Double = 0.0,
        length: Double = 1.0,
        exponent: Double = 1.0
): RadialGradient {
    return RadialGradient(color0, color1, offset, rotation, length, exponent)
}

fun radialGradient(
    color0: ColorOKLABa,
    color1: ColorOKLABa,
    offset: Vector2 = Vector2.ZERO,
    rotation: Double = 0.0,
    length: Double = 1.0,
    exponent: Double = 1.0
): RadialGradientOKLab {
    return RadialGradientOKLab(color0, color1, offset, rotation, length, exponent)
}


