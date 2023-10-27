import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.contour
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
                    selectEdges(0, 2)

                    for (e in edges) {
                        e.replaceWith(contour {
                            moveTo(e.startPosition)
                            lineTo(e.position(0.5) + e.normal(0.5) * cos(seconds) * 150.0)
                            lineTo(e.endPosition)
                        })
                    }
                    selectEdges(0, 1)

                    for (e in edges) {
                        e.replaceWith(contour {
                            moveTo(e.startPosition)
                            val t = 0.5
                            lineTo(e.position(t) + e.normal(t) * cos(seconds) * 50.0)
                            lineTo(e.endPosition)
                        })
                    }

                    selectEdges(0, 1)
                    for (e in edges) {
                        e.replaceWith(contour {
                            moveTo(e.startPosition)
                            val t = 0.5
                            lineTo(e.position(t) + e.normal(t) * sin(seconds) * 50.0)
                            lineTo(e.endPosition)
                        })
                    }
                }

                drawer.stroke = ColorRGBa.RED
                drawer.contour(contour)
            }
        }
    }
}