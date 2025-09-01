/*
Drawing a square using the turtle interface.
*/

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.turtle.turtle

fun main() = application {
    program {
        // turtle returns List<ShapeContour>
        val contours = turtle(drawer.bounds.center) {
            penUp()
            forward(50.0)
            rotate(90.0)
            forward(50.0)
            penDown()

            rotate(90.0)
            forward(100.0)

            rotate(90.0)
            forward(100.0)

            rotate(90.0)
            forward(100.0)

            rotate(90.0)
            forward(100.0)
        }

        extend {
            // draw the contours
            drawer.stroke = ColorRGBa.PINK
            drawer.contours(contours)
        }
    }
}
