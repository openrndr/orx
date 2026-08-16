package deform

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.deform.deform
import org.openrndr.shape.Circle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                val c = Circle(drawer.bounds.center, 200.0).contour

                val nc = c.deform(mouse.position, 1.0, 10.0, 100.0)

                drawer.stroke = ColorRGBa.PINK

                drawer.fill = null
                drawer.contour(nc)

            }
        }
    }
}