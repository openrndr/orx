package hobbycurve

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.ordering.hilbertOrder
import kotlin.random.Random

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            for (i in -20..20) {
                val t = i / 10.0
                val points = drawer.bounds.offsetEdges(-50.0).scatter(25.0, random = Random(0)).hilbertOrder()
                drawer.stroke = ColorRGBa.WHITE.opacify(0.5)
                drawer.fill = null
                drawer.contour(hobbyCurve(points, closed = false, tensions = { i, inAngle, outAngle ->
                    Pair(t, t)
                }))
            }
        }
    }
}