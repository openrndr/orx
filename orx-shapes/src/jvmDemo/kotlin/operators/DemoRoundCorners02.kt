package operators

import org.openrndr.application
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.extra.shapes.operators.roundCorners
import org.openrndr.shape.Rectangle

/**
 * Demonstrates how, with the current implementation of `roundCorners`, only pairs of consecutive linear segments
 * are rounded. If one of the segments in the pair is a quadratic or cubic Bezier, no rounding is applied.
 *
 * The program creates a list with two rectangular contours. In the second of them a vertex is rotated,
 * causing two segments to become curved.
 *
 * Next, rounded versions of both contours are stored in a new list.
 *
 * Finally, all 4 shapes are displayed for comparison.
 *
 */
fun main() = application {
    program {
        extend {
            val contours = listOf(
                Rectangle(100.0, 100.0, 100.0, 100.0).contour,
                adjustContour(Rectangle(400.0, 100.0, 100.0, 100.0).contour) {
                    selectVertex(0)
                    vertices.forEach { it.rotate(30.0) }
                }
            )

            val contoursRounded = contours.map {
                it.roundCorners(10.0)
            }

            drawer.contours(contours)
            drawer.translate(0.0, 150.0)
            drawer.contours(contoursRounded)
        }
    }
}