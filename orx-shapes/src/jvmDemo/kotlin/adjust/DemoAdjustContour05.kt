package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.shape.Circle
import kotlin.math.cos

/**
 * Demonstrates animated modifications to a circular contour using `adjustContour`.
 *
 * The application creates a circular contour and dynamically alters its edges
 * based on the current time in seconds. Each edge of the contour is selected
 * and transformed through a series of operations:
 *
 * - The currently active edge (based on time modulo 4) is replaced with a point at 0.5.
 * - All other edges are reshaped by reducing their length dynamically, with the reduction
 *   calculated using a cosine function involving the current time in seconds.
 *
 * The resulting contour is then drawn with a red stroke color.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            var contour =
                Circle(drawer.bounds.center, 300.0).contour

            contour = adjustContour(contour) {
                selectEdges(0, 1, 2, 3)
                edges.forEachIndexed { index, it ->
                    if (index == seconds.mod(4.0).toInt()) {
                        it.replaceWith(0.5)
                    } else {
                        val v = cos(seconds) * 0.15 + 0.25
                        it.sub(0.5 - v, 0.5 + v)
                    }
                }
            }
            drawer.stroke = ColorRGBa.RED
            drawer.contour(contour)
        }
    }
}
