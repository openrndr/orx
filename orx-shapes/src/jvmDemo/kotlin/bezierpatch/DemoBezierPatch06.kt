package bezierpatch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.shapes.bezierpatches.bezierPatch
import org.openrndr.math.Polar
import org.openrndr.shape.Segment2D

/**
 * Shows how to
 * - create a [bezierPatch] out of 4 curved Segment2D instances
 * - apply an image texture to the patch using a shadeStyle
 *
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        fun offset(n: Int) = Polar(n * 107.0, 100.0).cartesian

        // Take the window bounds, shift it inwards, then take 4 horizontal
        // LineSegment instances from that Rectangle
        val lineSegments = List(4) {
            drawer.bounds.offsetEdges(-100.0).horizontal(1.0 - it * 0.333)
        }
        // Map the 4 LineSegment instances to curved Segment2D instances by
        // offsetting 4 points in each.
        val bentSegments = lineSegments.mapIndexed { i, seg ->
            Segment2D(
                seg.position(0.0) + offset(i * 4 + 1),
                seg.position(0.333) + offset(i * 4 + 2),
                seg.position(0.666) + offset(i * 4 + 3),
                seg.position(1.0) + offset(i * 4 + 4)
            )
        }

        val bp = bezierPatch(bentSegments[0], bentSegments[1], bentSegments[2], bentSegments[3])

        val tex = loadImage("demo-data/images/peopleCity01.jpg")
        val style = shadeStyle {
            fragmentTransform = "x_fill = texture(p_tex, va_texCoord0.xy);"
            parameter("tex", tex)
        }
        extend {
            drawer.shadeStyle = style
            drawer.clear(ColorRGBa.PINK)
            drawer.bezierPatch(bp)
        }
    }
}
