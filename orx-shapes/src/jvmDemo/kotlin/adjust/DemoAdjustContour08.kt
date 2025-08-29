package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.math.Vector2
import org.openrndr.shape.contour

/**
 * Demonstrates how to adjust and manipulate the vertices and edges of a contour.
 *
 * This method shows two approaches for transforming contours:
 *
 * 1. Adjusting vertices directly by selecting specific vertices in a contour and modifying their control points.
 * 2. Adjusting edges of a contour by transforming their control points.
 *
 * For each approach, a red line is drawn representing the transformed contour.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            // Adjust a contour by transforming its vertices
            var contour = contour {
                moveTo(drawer.bounds.position(0.5, 0.1) - Vector2(300.0, 0.0))
                lineTo(drawer.bounds.position(0.5, 0.1) + Vector2(300.0, 0.0))
            }

            contour = adjustContour(contour) {
                selectVertex(0)
                vertex.moveControlOutBy(Vector2(0.0, 100.0))

                selectVertex(1)
                vertex.moveControlInBy(Vector2(0.0, -100.0))

            }
            drawer.stroke = ColorRGBa.RED
            drawer.contour(contour)

            // Achieve the same effect by transforming the control points of its edge
            contour = contour {
                moveTo(drawer.bounds.position(0.5, 0.2) - Vector2(300.0, 0.0))
                lineTo(drawer.bounds.position(0.5, 0.2) + Vector2(300.0, 0.0))
            }

            contour = adjustContour(contour) {
                selectEdge(0)
                edge.moveControl0By(Vector2(0.0, 100.0))
                edge.moveControl1By(Vector2(0.0, -100.0))

            }
            drawer.stroke = ColorRGBa.RED
            drawer.contour(contour)

        }
    }
}
