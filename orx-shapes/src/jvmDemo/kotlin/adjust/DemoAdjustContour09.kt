package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.extra.shapes.adjust.extensions.averageTangents
import org.openrndr.extra.shapes.adjust.extensions.switchTangents
import org.openrndr.extra.shapes.tunni.tunniLine
import org.openrndr.extra.shapes.tunni.tunniPoint
import kotlin.math.sqrt

fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            drawer.clear(ColorRGBa.WHITE)
            var contour = drawer.bounds.offsetEdges(-200.0).contour

            drawer.fill = null

            contour = adjustContour(contour) {
                selectVertices(0, 1)
                for (v in vertices) {
                    v.averageTangents()
                    v.scale(sqrt(2.0))
                    v.rotate(45.0)
                }

                selectVertices(2)
                for (v in vertices) {
                    v.switchTangents()
                }
            }
            drawer.stroke = ColorRGBa.BLACK
            drawer.contour(contour)

            drawer.stroke = ColorRGBa.RED

            for (s in contour.segments) {
                drawer.lineSegment(s.start, s.cubic.control[0])
                drawer.lineSegment(s.end, s.cubic.control[1])
            }
            drawer.fill = ColorRGBa.BLACK
            drawer.stroke = null
            drawer.circles(contour.segments.map { it.start }, 5.0)

            drawer.stroke = ColorRGBa.GRAY
            for (s in contour.segments) {
                drawer.lineSegment(s.tunniLine)
                drawer.fill = ColorRGBa.CYAN
                drawer.circle(s.tunniPoint, 5.0)
            }
        }
    }
}
