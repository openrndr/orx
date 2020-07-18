package org.openrndr.extra.shadestyles

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShadeStyle
import org.openrndr.extra.parameters.Description
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

@Description("N-Point gradient")
class NPointGradient(
        numPoints: Int,
        colors: Array<ColorRGBa>,
        points: Array<Vector2> = arrayOf(Vector2.ZERO)) : ShadeStyle() {

    var numPoints: Int by Parameter()
    var colors: Array<ColorRGBa> by Parameter()
    var points: Array<Vector2> by Parameter()

    init {
        this.numPoints = numPoints
        this.colors = colors
        this.points = points

        fragmentTransform = """
                float sum = 0;
                vec4 rgba = vec4(0.0);
                for(int i=0; i<p_numPoints; i++) {
                    float dist = 1.0 / (1.0 + distance(p_points[i], c_screenPosition));
                    sum += dist;
                    rgba += p_colors[i] * dist;
                }
                x_fill = rgba/sum;
        """
    }
}
