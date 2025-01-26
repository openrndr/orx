package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.shape.Circle
import kotlin.math.cos

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
