package rectify

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.rectify.rectified
import kotlin.random.Random

/**
 * Demonstrates how calling `ShapeContour.position(t)` and `RectifiedContour.position(t)`
 * returns different values for the same `t`, and how a sub-contour with a fixed `t`-length
 * (0.01 in this case), returns longer and shorter contours depending on the position
 * along a `ShapeContour`, but constant length contours for `RectifiedContour`.
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
            drawer.strokeWeight = 4.0

            drawer.stroke = ColorRGBa.RED
            drawer.contour(curve.sub(seconds * 0.1, seconds * 0.1 + 0.01))

            drawer.stroke = ColorRGBa.GREEN
            drawer.contour(rectified.sub(seconds * 0.1, seconds * 0.1 + 0.01))
        }
    }
}
