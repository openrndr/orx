package tunni

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.tunni.tunniLine
import org.openrndr.extra.shapes.tunni.tunniPoint
import org.openrndr.extra.shapes.tunni.withTunniLine
import org.openrndr.shape.Circle
import kotlin.math.cos

/**
 * Demonstrates how to use `Segment2D.withTunniLine()` and how to visualize
 * Tunni points.
 *
 * Tunni lines are a concept devised by Eduardo Tunni and Fontlab Ltd.,
 * described at https://github.com/OliverLeenders/Tunni-Lines
 *
 * This program creates a circular contour `c`, then processes each of
 * its four segments one by one: it visualizes their Tunni point,
 * draws the segment's control points as a line and
 * finally draws a copy of the segment with its control points
 * altered with `withTunniLine()` along the segment's normal.
 */
fun main() = application {
    program {
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.fill = ColorRGBa.BLACK
            val c = Circle(drawer.bounds.center, 200.0).contour

            drawer.fill = null
            for (s in c.segments) {
                val tp = s.tunniPoint
                drawer.circle(tp, 10.0)

                val sc = s.cubic
                drawer.lineSegment(sc.control[0], sc.control[1])

                //drawer.segment(s.withTunniPoint(tp + (mouse.position - drawer.bounds.center)/2.0))
                drawer.segment(s.withTunniLine(s.tunniLine.position(0.5) + s.tunniLine.normal * cos(seconds) * 40.0))

            }
            drawer.contour(c)
        }
    }
}
