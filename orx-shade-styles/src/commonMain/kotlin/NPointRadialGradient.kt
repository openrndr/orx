package org.openrndr.extra.shadestyles

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShadeStyle
import org.openrndr.extra.parameters.Description
import org.openrndr.math.Vector2

@Description("Multicolor radial gradient")
class NPointRadialGradient(
        colors: Array<ColorRGBa>,
        points: Array<Double> = Array(colors.size) { it / (colors.size - 1.0) },
        offset: Vector2 = Vector2.ZERO,
        rotation: Double = 0.0,
        length: Double = 1.0) : ShadeStyle()


{

    var colors: Array<ColorRGBa> by Parameter()

    // Sorted normalized values defining relative positions of colors
    var points: Array<Double> by Parameter()
    var offset: Vector2 by Parameter()
    var rotation: Double by Parameter()
    var length: Double by Parameter()

    init {
        this.colors = colors
        this.points = points
        this.offset = offset
        this.rotation = rotation
        this.length = length

        fragmentTransform = """
            vec2 coord = (c_boundsPosition.xy - 0.5 + p_offset/2.0) * 2.0;
            
            float cr = cos(radians(p_rotation));
            float sr = sin(radians(p_rotation));
            mat2 rm = mat2(cr, -sr, sr, cr); 
            vec2 rc = rm * coord;
            float f = clamp(p_length * length(rc), 0.0, 1.0);            

            int i=0;
            while(i < p_points_SIZE - 1 && f >= p_points[i+1]) { i++; }

            vec4 color0 = p_colors[i];
            color0.rgb *= color0.a;

            vec4 color1 = p_colors[i+1]; 
            color1.rgb *= color1.a;

            float g = (f - p_points[i]) / (p_points[i+1] - p_points[i]);
            vec4 gradient = mix(color0, color1, clamp(g, 0.0, 1.0)); 
            
            vec4 fn = vec4(x_fill.rgb, 1.0) * x_fill.a;            
            
            x_fill = fn * gradient;
            if (x_fill.a != 0) {
                x_fill.rgb /= x_fill.a;
            }
        """
    }
}
