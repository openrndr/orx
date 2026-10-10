package expand

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.expand.expand
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Segment2D

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val pts = drawer.bounds.scatter(20.0).hilbertOrder()
            val c = hobbyCurve(pts, false)

            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.shape(c.expand(30.0))
            }
        }
    }
}