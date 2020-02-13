package org.openrndr.extra.shadestyles

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2

fun linearGradient(color0: ColorRGBa, color1: ColorRGBa, offset : Vector2 = Vector2.ZERO, rotation:Double = 0.0) : ShadeStyle {
    return shadeStyle {
        fragmentTransform = """
            vec2 coord = (c_boundsPosition.xy - vec2(0.5) + offset);
            float cr = cos(p_rotation);
            float sr = sin(p_rotation);
            mat2 rm = mat2(cr, -sr, sr, cr);
            float f = clamp((rm * coord).y + 0.5, 0.0, 1.0);            
                        
            vec4 gradient = p_color0 * (1.0-f) + p_color1 * f;
            
            vec4 fn = vec4(x_fill.rgb, 1.0) * x_fill.a;            
            
            x_fill = fn * gradient;
            if (x_fill.a !=0) {
                x_fill.rgb /= x_fill.a;
            }
        """
        parameter("offset", offset)
        parameter("color0", color0.alphaMultiplied)
        parameter("color1", color1.alphaMultiplied)
        parameter("rotation", Math.toRadians(rotation) )
    }
}

fun radialGradient(color0: ColorRGBa, color1: ColorRGBa, offset: Vector2 = Vector2.ZERO, rotation:Double = 0.0) : ShadeStyle {
    return shadeStyle {
        fragmentTransform = """
            vec2 coord = (c_boundsPosition.xy - vec2(0.5) + p_offset/2.0) * 2.0;
            
            float cr = cos(p_rotation);
            float sr = sin(p_rotation);
            mat2 rm = mat2(cr, -sr, sr, cr);
            float f =  clamp(length(rm * coord), 0.0, 1.0);            
                        
            vec4 gradient = p_color0 * (1.0-f) + p_color1 * f;
            
            vec4 fn = vec4(x_fill.rgb, 1.0) * x_fill.a;            
            
            x_fill = fn * gradient;
            if (x_fill.a !=0) {
                x_fill.rgb /= x_fill.a;
            }
        """
        parameter("offset", offset)
        parameter("color0", color0.alphaMultiplied)
        parameter("color1", color1.alphaMultiplied)
        parameter("rotation", Math.toRadians(rotation) )
    }
}

fun angularGradient(color0: ColorRGBa, color1: ColorRGBa, offset: Vector2 = Vector2.ZERO, rotation:Double = 0.0) : ShadeStyle {
    return shadeStyle {
        fragmentTransform = """
            vec2 coord = (c_boundsPosition.xy - vec2(0.5) + p_offset/2.0) * 2.0;
            
            float cr = cos(p_rotation);
            float sr = sin(p_rotation);
            mat2 rm = mat2(cr, -sr, sr, cr);
            vec2 rc = rm * coord;
            float f = (atan(rc.y, rc.x) + 3.1415926536) / (2.0 * 3.1415926536);            
                        
            vec4 gradient = p_color0 * (1.0-f) + p_color1 * f;
            
            vec4 fn = vec4(x_fill.rgb, 1.0) * x_fill.a;            
            
            x_fill = fn * gradient;
            if (x_fill.a !=0) {
                x_fill.rgb /= x_fill.a;
            }
        """
        parameter("offset", offset)
        parameter("color0", color0.alphaMultiplied)
        parameter("color1", color1.alphaMultiplied)
        parameter("rotation", Math.toRadians(rotation) )
    }
}

fun halfAngularGradient(color0: ColorRGBa, color1: ColorRGBa, offset: Vector2 = Vector2.ZERO, rotation:Double = 0.0) : ShadeStyle {
    return shadeStyle {
        fragmentTransform = """
            vec2 coord = (c_boundsPosition.xy - vec2(0.5) + p_offset/2.0) * 2.0;
            
            float cr = cos(p_rotation);
            float sr = sin(p_rotation);
            mat2 rm = mat2(cr, -sr, sr, cr);
            vec2 rc = rm * coord;
            float f = abs(atan(rc.y, rc.x)) / (3.1415926536);
            
            //float f = abs(atan(rc.y/rc.x))  / (3.1415926536/2.0);
            //float f = (atan(rc.y/rc.x) + 3.1415926536/2.0) / (3.1415926536);            
                        
            vec4 gradient = p_color0 * (1.0-f) + p_color1 * f;
            vec4 fn = vec4(x_fill.rgb, 1.0) * x_fill.a;            
            x_fill = fn * gradient;
            if (x_fill.a !=0) {
                x_fill.rgb /= x_fill.a;
            }
        """
        parameter("offset", offset)
        parameter("color0", color0.alphaMultiplied)
        parameter("color1", color1.alphaMultiplied)
        parameter("rotation", Math.toRadians(rotation) )
    }
}