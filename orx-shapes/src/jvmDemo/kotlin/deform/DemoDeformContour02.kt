package deform

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extra.shapes.deform.deform
import org.openrndr.shape.Circle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {


            var c = Circle(drawer.bounds.center, 200.0).contour

            mouse.buttonDown.listen {
                c = c.deform(it.position, 1.0, 10.0, 100.0)
            }

            extend {


                val nc = c.deform(mouse.position, 1.0, 10.0, 100.0)

                drawer.stroke = ColorRGBa.PINK
                drawer.drawStyle.lineJoin = LineJoin.ROUND

                drawer.fill = null
                drawer.contour(nc)

                drawer.circles(nc.segments.map { it.start }, 4.0)

                drawer.circle(mouse.position, 10.0)
            }
        }
    }
}