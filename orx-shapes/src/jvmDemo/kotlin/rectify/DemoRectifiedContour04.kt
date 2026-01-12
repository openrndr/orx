package rectify

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.Polar
import org.openrndr.shape.Circle
import org.openrndr.shape.Segment2D

/**
 * Demonstrates the use of `RectifiedContour.position()` and
 * `RectifiedContour.normal()`.
 *
 * The program creates a circle out of 5 Polar points with a fixed radius
 * but a slightly randomized theta value. This results in a circle with
 * varying segment lengths.
 *
 * If we use `Circle` to create a circular contour, the contour contains
 * 4 segments of equal length and 4 points evenly spaced. The
 * difference between `ShapeContour` and `RectifiedContour` is not
 * obvious in this case.
 *
 * When the points are not evenly distributed like with the hobby curve
 * below, commenting out `.rectified()` will reveal how the two
 * approaches result in different arrangements.
 *
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        //val c = Circle(drawer.bounds.center, 50.0).contour
        val c = hobbyCurve(
            List(5) {
                val theta = it * 72.0 + Double.uniform(-24.0, 24.0)
                Polar(theta, 50.0).cartesian + drawer.bounds.center
            }, true
        )

        val rc = c.rectified()
        val normals = List(200) {
            val t = it / 200.0
            val p = rc.position(t)
            val n = rc.normal(t)
            Segment2D(p, p + n * 200.0)
        }
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.segments(normals)
        }
    }
}