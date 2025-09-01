package operators

import org.openrndr.application
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.extra.shapes.operators.roundCorners
import org.openrndr.shape.Rectangle

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