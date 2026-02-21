package rectify

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.rectify.rectified
import kotlin.random.Random

/**
 * Demonstrates how calling `ShapeContour.position(t)` and `RectifiedContour.position(t)`
 * return different values.
 *
 * The program creates a random hobbyCurve `ShapeContour` and a `RectifiedContour` based on it.
 * Then animates a point traveling on each of them, by querying `.position(t)` and increasing
 * `t` at a constant speed.
 *
 * Observing the animation reveals that with `ShapeContour` the moving point moves faster or slower
 * depending on the length of the contour's segments, but with `RectifiedContour` the speed is constant.
 *
 * Use `RectifiedContour` when you need to evenly distribute points along a contour, or for smooth
 * animations along the contour, and `ShapeContour` by default or when performance is essential.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val points = drawer.bounds.scatter(50.0, distanceToEdge = 100.0, random = Random(0))
        val curve = hobbyCurve(points)
        val rectified = curve.rectified()
        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.stroke = ColorRGBa.PINK
            drawer.contour(curve)
            drawer.fill = ColorRGBa.RED
            drawer.circle(curve.position(seconds * 0.05), 10.0)
            drawer.fill = ColorRGBa.GREEN
            drawer.circle(rectified.position(seconds * 0.05), 10.0)

        }
    }
}
