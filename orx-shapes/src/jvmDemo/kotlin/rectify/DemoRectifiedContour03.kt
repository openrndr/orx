package rectify

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.rectify.rectified
import kotlin.random.Random

/**
 * Demonstrates how to query a hobby contour at regular intervals and draw
 * 100 evenly spaced circles on it.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val points = drawer.bounds.scatter(80.0, distanceToEdge = 100.0, random = Random(0))
        val curve = hobbyCurve(points, closed = true)
        val rectified = curve.rectified()
        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.fill = null
            drawer.stroke = ColorRGBa.GRAY
            drawer.contour(curve)

            val points = (0 until 100).map {
                rectified.position(it / 100.0)
            }
            drawer.circles(points, 5.0)
        }
    }
}
