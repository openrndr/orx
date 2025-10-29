package hobbycurve

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.math.Vector2

/**
 * Demonstrates how to use the hobbyCurve function to render a smooth closed contour
 * passing through a predefined set of points.
 *
 * See Hobby, John. D., “Smooth, Easy to Compute Interpolating Splines”, Discrete and Computational Geometry, 1986, vol. 1
 */
fun main() = application {
    program {
        extend {
            val points = listOf(
                Vector2(150.0, 350.0),
                Vector2(325.0, 100.0),
                Vector2(500.0, 350.0),
                Vector2(325.0, 250.0)
            )

            drawer.stroke = ColorRGBa.BLACK
            drawer.fill = ColorRGBa.PINK
            drawer.contour(hobbyCurve(points, closed = true))

            drawer.fill = ColorRGBa.WHITE
            drawer.circles(points, 4.0)
        }
    }
}