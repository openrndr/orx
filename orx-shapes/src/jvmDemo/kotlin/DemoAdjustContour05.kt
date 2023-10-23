import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.shape.Circle

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
                    selectEdges(0, 1, 2, 3)
                    edges.forEachIndexed { index, it ->
                        it.replaceWith(0.5)
//                        if (index == seconds.mod(4.0).toInt()) {
//                            it.replaceWith(0.5)
//                        } else {
//                            val v = cos(seconds) * 0.15 + 0.25
//                            it.sub(0.5 - v, 0.5 + v)
//                        }
                    }
                }
                drawer.stroke = ColorRGBa.RED
                drawer.contour(contour)
            }
        }
    }
}