package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.shape.Circle
import kotlin.math.cos

/**
 * Demonstrates an `adjustContour` animated effect where edge 0 of a contour
 * is replaced by a point sampled on that edge. The specific edge point oscillates between
 * 0.0 (at the start) and 1.0 (at the end) using a cosine and the `seconds` variable.
 *
 * The base contour used for the effect alternates every second
 * between a rectangular and a circular contour.
 *
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            var contour = if (seconds.mod(2.0) < 1.0) {
                drawer.bounds.scaledBy(0.5, 0.5, 0.5).contour
            } else {
                Circle(drawer.bounds.center, 300.0).contour
            }
            contour = adjustContour(contour) {
                selectEdge(0)
                edge.replaceWith(cos(seconds) * 0.5 + 0.5)
            }
            drawer.stroke = ColorRGBa.RED
            drawer.contour(contour)
        }
    }
}
