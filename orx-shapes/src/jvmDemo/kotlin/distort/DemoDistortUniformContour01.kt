package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.Circle
import kotlin.math.cos

/**
 * Demonstrates how to use
 * [`RectifiedContour.distort`](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.distort/distort.html)
 * to distort a contour by writing a function that takes
 * the normalized `t` value and the `position`, and returns a `Vector2`.
 *
 * In this example, a circular rectified contour is transformed by adding to the current
 * position the normal (perpendicular vector) multiplied by an oscillating cosine.
 * Since the `t` value goes from 0.0 to 1.0, the `cos(t * PI * 14)` expression
 * increases and decreases 7 times, creating a 7-point rounded star as a result.
 */
fun main() {
    application {
        program {
            val circle = Circle(drawer.bounds.center, 100.0).contour.rectified()
            val c0 = circle.contour
            val c1 = circle.distort { t, position ->
                position + circle.normal(t) * cos(t * Math.PI * 14.0) * 20.0
            }
            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK.shade(0.5)
                drawer.contour(c0)
                drawer.stroke = ColorRGBa.PINK
                drawer.contour(c1)
            }
        }
    }
}