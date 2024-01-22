package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            extend {
                var contour = Circle(drawer.bounds.center, 300.0).contour
                contour = adjustContour(contour) {
                    selectVertex(0)
                    vertex.remove()
                    selectVertex(0)
                    vertex.moveBy(Vector2(cos(seconds) * 40.0, sin(seconds * 0.43) * 40.0))
                    vertex.scale(cos(seconds*2.0)*2.0)


                }
                drawer.stroke = ColorRGBa.RED
                drawer.contour(contour)
            }
        }
    }
}