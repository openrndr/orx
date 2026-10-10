package expand

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.expand.expand
import org.openrndr.shape.Segment2D

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {

                val p0 = drawer.bounds.position(0.2, 0.2)
                val p1 = drawer.bounds.position(0.8, 0.8)

                val c0 = drawer.bounds.position(0.25, 0.75)
                val c1 = drawer.bounds.position(0.5, 0.75)

                drawer.stroke = ColorRGBa.WHITE
                val c = Segment2D(p0, c0, c1, p1).contour
                drawer.contour(c)
                drawer.fill = null

                drawer.shape(c.expand(30.0))
            }
        }
    }
}