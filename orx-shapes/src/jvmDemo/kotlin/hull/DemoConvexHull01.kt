package hull

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hull.convexHull
import org.openrndr.extra.shapes.ordering.hilbertOrder

/**
 * Demonstrates the use of the `convexHull` method to create convex hulls of a growing set of points.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val pts = drawer.bounds.scatter(5.0).hilbertOrder()

            val hulls = (3 .. pts.size).map {
                pts.take(it).convexHull()
            }

            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.contours(hulls)
            }
        }
    }
}