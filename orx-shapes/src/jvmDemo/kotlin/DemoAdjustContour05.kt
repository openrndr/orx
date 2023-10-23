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
                var contour =
                    Circle(drawer.bounds.center, 300.0).contour

                contour = adjustContour(contour) {
                    for (i in 0 until 4) {
                        selectEdge(i)
                        edge.sub(0.2, 0.8)
                    }
                }
                drawer.stroke = ColorRGBa.RED
                drawer.contour(contour)
            }
        }
    }
}