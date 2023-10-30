import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.math.Vector2
import org.openrndr.shape.contour
import kotlin.math.cos

fun main() {
    application {
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
                    val tr = cos(seconds) * 0.5 + 0.5

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
}