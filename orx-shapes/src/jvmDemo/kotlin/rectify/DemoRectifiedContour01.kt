package rectify

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.rectify.rectified
import kotlin.random.Random

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
