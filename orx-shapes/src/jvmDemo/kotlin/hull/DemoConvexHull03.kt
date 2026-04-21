package hull

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hull.convexHull
import org.openrndr.extra.shapes.hull.convexHullSet
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.Triangle
import org.openrndr.shape.triangulate
import kotlin.random.Random

/**
 * Demonstrates the use of the `convexHullSet` method to create a convex hull peeling. Each peel is triangulated using an ear clipping triangulator.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            var pts = drawer.bounds.offsetEdges(-50.0).scatter(30.0, random = Random(0))
            val ogpts = pts
            val hulls = mutableListOf<ShapeContour>()

            while (pts.size > 3) {
                val hull = pts.convexHull()
                hulls.add(hull)
                val hullSet = pts.convexHullSet()
                pts = pts - hullSet
            }

            val triangles = hulls.windowed(2, 1).map {
                val outer = it[0]
                val inner = it[1].reversed

                val shape = Shape(listOf(outer, inner))
                triangulate(shape).windowed(3, 3).map {
                    Triangle(it[0], it[1], it[2]).contour
                }
            }

            extend {
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE

                drawer.strokeWeight = 0.25
                for ((index, t) in triangles.withIndex()) {
                    drawer.fill = ColorRGBa.RED.shiftHue<OKHSV>(index * 30.0)
                    drawer.contours(t)
                }

                drawer.fill = ColorRGBa.RED.shiftHue<OKHSV>(hulls.size * 30.0)
                drawer.contour(hulls.last())

                drawer.fill = ColorRGBa.BLACK
                drawer.circles(ogpts, 3.0)
            }
        }
    }
}