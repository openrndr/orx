package convexdecomposition

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.convexdecomposition.convexDecompose
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.ordering.hilbertOrder
import org.openrndr.extra.shapes.utilities.fromContours
import org.openrndr.math.Polar
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {


            val pts = drawer.bounds.offsetEdges(-50.0).scatter(70.0, random = Random(0)).sortedBy {
                Polar.fromVector(it - drawer.bounds.center).theta
            }
            val c = hobbyCurve(pts, true).shape

            val convex = c.convexDecompose(3.5)

            extend {

                drawer.clear(ColorRGBa.PINK)
                drawer.contours(convex)
                //drawer.contours(convex.shuffled().take(20))

            }
        }
    }
}