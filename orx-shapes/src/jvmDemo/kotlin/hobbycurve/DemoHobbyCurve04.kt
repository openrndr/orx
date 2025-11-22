package hobbycurve

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.ordering.hilbertOrder
import kotlin.random.Random

/**
 * Demonstrates the use of the `tensions` argument when creating a Hobby curve.
 *
 * The program starts by creating a random set of scattered points with enough separation between them.
 * The points are sorted using `hilbertOrder` to minimize the travel distance when visiting all the points.
 * Finally, we draw a set of 40 hobby translucent curves using those same points but with varying tensions.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            for (i in -20..20) {
                val t = i / 10.0
                val points = drawer.bounds.offsetEdges(-50.0)
                    .scatter(25.0, random = Random(0))
                    .hilbertOrder()

                drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
                drawer.fill = null
                drawer.contour(hobbyCurve(points, closed = false, tensions = { i, inAngle, outAngle ->
                    Pair(t, t)
                }))
            }
        }
    }
}