/*
Drawing shape contours aligned to the turtle's orientation.
*/
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.turtle.contour
import org.openrndr.extra.turtle.turtle
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

fun main() {
    application {
        program {
            // turtle returns List<ShapeContour>
            val contours = turtle(drawer.bounds.center + Vector2(-100.0, 100.0)) {
                forward(100.0)

                // let the turtle draw a full a circle
                val circle0 = Circle(Vector2.ZERO, 100.0)
                contour(circle0.contour)

                // let the turtle draw a half circle
                val circle1 = Circle(Vector2.ZERO, 50.0)
                contour(circle1.contour.sub(0.0, 0.5))

                // let the turtle draw another half circle
                val circle2 = Circle(Vector2.ZERO, 25.0)
                contour(circle2.contour.sub(0.0, 0.5))
            }
            extend {
                // draw the contours
                drawer.stroke = ColorRGBa.PINK
                drawer.contours(contours)
            }
        }
    }
}