package org.openrndr.extra.shadestyles

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShadeStyle
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2

@Description("Angular gradient")
class AngularGradient(
        color0: ColorRGBa,
        color1: ColorRGBa,
        offset: Vector2 = Vector2.ZERO,
        rotation: Double = 0.0,
        exponent: Double = 1.0) : ShadeStyle() {

    @ColorParameter("start color", order = 0)
    var color0 : ColorRGBa by Parameter()
    @ColorParameter("end color", order = 1)
    var color1 : ColorRGBa by Parameter()
    @ColorParameter("offset", order = 2)
    var offset : Vector2 by Parameter()
    @ColorParameter("rotation", order = 3)
    var rotation : Double by Parameter()
    @DoubleParameter("exponent", 0.01, 10.0, order = 4)
    var exponent: Double by Parameter()

    init {
        this.color0 = color0
        this.color1 = color1
        this.offset = offset
        this.rotation = rotation
        this.exponent = exponent

        fragmentTransform = """
            vec2 coord = (c_boundsPosition.xy - 0.5 + p_offset/2.0) * 2.0;
            
            float cr = cos(radians(p_rotation));
            float sr = sin(radians(p_rotation));
            mat2 rm = mat2(cr, -sr, sr, cr);
            vec2 rc = rm * coord;
            float f = (atan(rc.y, rc.x) + 3.1415926536) / (2.0 * 3.1415926536);            

            vec4 color0 = p_color0;
            color0.rgb *= color0.a;

            vec4 color1 = p_color1; 
            color1.rgb *= color1.a;

            vec4 gradient = mix(color0, color1, pow(f, p_exponent));

            vec4 fn = vec4(x_fill.rgb, 1.0) * x_fill.a;            
            
            x_fill = fn * gradient;
            if (x_fill.a != 0) {
                x_fill.rgb /= x_fill.a;
            }
        """
    }
}

fun angularGradient(
        color0: ColorRGBa,
        color1: ColorRGBa,
        offset: Vector2 = Vector2.ZERO,
        rotation: Double = 0.0,
        exponent: Double = 1.0
): ShadeStyle {
    return AngularGradient(color0, color1, offset, rotation, exponent)
}
