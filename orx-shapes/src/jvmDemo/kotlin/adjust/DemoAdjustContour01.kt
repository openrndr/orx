package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates how to use `adjustContour` to select and modify three vertices
 * in a circular contour. In OPENRNDR circles contain 4 cubic bézier
 * segments connecting 4 vertices.
 *
 * On every animation frame the circular contour is created and transformed
 * using sines, cosines and the variable `seconds` for an animated effect.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            var contour = Circle(drawer.bounds.center, 300.0).contour
            contour = adjustContour(contour) {
                selectVertex(0)
                vertex.moveBy(Vector2(cos(seconds) * 40.0, sin(seconds * 0.43) * 40.0))

                selectVertex(2)
                vertex.rotate(seconds * 45.0)

                selectVertex(1)
                vertex.scale(cos(seconds * 0.943) * 2.0)
            }
            drawer.stroke = ColorRGBa.RED
            drawer.contour(contour)
        }
    }
}
