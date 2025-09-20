package hobbycurve

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.alphashape.AlphaShape
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.math.Vector2
import kotlin.random.Random

/**
 * This demo creates a list of random 2D points, finds the alpha shape contour for those points,
 * and finally makes that contour smooth by calling `hobbyCurve()`.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val points = List(40) {
            Vector2(
                Random.nextDouble(width * 0.25, width * 0.75),
                Random.nextDouble(height * 0.25, height * 0.75)
            )
        }
        val alphaShape = AlphaShape(points)
        val c = alphaShape.createContour()
        val hobby = c.hobbyCurve()
        extend {
            drawer.fill = ColorRGBa.PINK
            drawer.contour(hobby)

            drawer.fill = ColorRGBa.WHITE
            drawer.circles(points, 4.0)
        }
    }
}