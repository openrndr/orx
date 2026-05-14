package convexdecomposition

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.convexdecomposition.convexDecompose
import org.openrndr.extra.shapes.utilities.fromContours
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape

fun main() {
    application {
        program {

            val co = Circle(drawer.bounds.center, 200.0).contour
            val ci = Circle(drawer.bounds.center, 100.0).contour.reversed

            val c = Shape(listOf(co, ci))

            val convex = c.convexDecompose(0.5)

            extend {

                drawer.clear(ColorRGBa.PINK)
                drawer.contours(convex)

            }
        }
    }
}