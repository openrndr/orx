package hull

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hull.convexHull
import org.openrndr.extra.shapes.hull.convexHullSet
import org.openrndr.shape.ShapeContour

/**
 * Demonstrates the use of the `convexHullSet` method to create a convex hull peeling
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            var pts = drawer.bounds.offsetEdges(-50.0).scatter(8.0)

            val hulls = mutableListOf<ShapeContour>()
            while (pts.size > 3) {
                val hull = pts.convexHull()
                hulls.add(hull)
                val hullSet = pts.convexHullSet()
                pts = pts - hullSet
            }
            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.contours(hulls)
            }
        }
    }
}