package tunni

import contour
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.extra.shapes.tunni.tunniLine
import org.openrndr.shape.Circle
import kotlin.math.cos

fun main() {
    application {
        program {
            extend {
                val c = Circle(drawer.bounds.center, 200.0).contour
                val c2 = adjustContour(c) {
                    selectEdges { it -> true }
                    for (e in edges) {
                        val tl = e.tunniLine
                        e.withTunniLine(tl.position(0.5) + tl.normal * cos(seconds)*200.0)
                    }
                }
                for (s in c2.segments) {
                    drawer.stroke = ColorRGBa.WHITE
                    drawer.lineSegment(s.tunniLine)
                }
                drawer.contour(c2)
            }
        }
    }
}