package alphashape

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.alphashape.AlphaShape
import org.openrndr.math.Vector2
import kotlin.random.Random

/**
 * Demonstrates the use of [AlphaShape] to create a [org.openrndr.shape.ShapeContour] out
 * of a collection of random [Vector2] points. Unlike the convex hull, an Alpha shape can be concave.
 *
 * More details in [WikiPedia](https://en.wikipedia.org/wiki/Alpha_shape)
 */
fun main() = application {
    program {
        val points = List(40) {
            Vector2(
                Random.nextDouble(width * 0.25, width * 0.75),
                Random.nextDouble(height * 0.25, height * 0.75)
            )
        }
        val alphaShape = AlphaShape(points)
        val c = alphaShape.createContour()
        extend {
            drawer.fill = ColorRGBa.PINK
            drawer.contour(c)
            drawer.fill = ColorRGBa.WHITE
            drawer.circles(points, 4.0)
        }
    }
}