package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.math.Vector2
import org.openrndr.shape.contour
import kotlin.math.cos

/**
 * Demonstrates how to create and manipulate a contour dynamically using the `adjustContour` function.
 *
 * The program initializes a simple linear contour and applies transformations to it on each animation frame:
 * - The only edge of the contour is split into many equal parts.
 * - A value between 0 and 1 is calculated based on the cosine of the current time in seconds.
 * - That value is used to calculate an anchor point and to select all vertices to its right
 * - The selected vertices are rotated around an anchor, as if rolling a straight line into a spiral.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            var contour = contour {
                moveTo(drawer.bounds.center - Vector2(300.0, 0.0))
                lineTo(drawer.bounds.center + Vector2(300.0, 0.0))
            }

            contour = adjustContour(contour) {
                selectEdge(0)
                edge.splitIn(128)
                val tr = cos(seconds + 2.0) * 0.5 + 0.5

                selectVertices { i, v -> v.t >= tr }
                val anchor = contour.position(tr)

                for (v in vertices) {
                    v.rotate((v.t - tr) * 2000.0, anchor)
                    v.scale(0.05, anchor)
                }
            }

            drawer.stroke = ColorRGBa.RED
            drawer.contour(contour)
        }
    }
}
