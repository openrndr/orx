//package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.math.Vector2
import org.openrndr.shape.contour

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            extend {
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
}