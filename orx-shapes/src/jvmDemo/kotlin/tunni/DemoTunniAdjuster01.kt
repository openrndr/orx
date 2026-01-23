package tunni

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.extra.shapes.tunni.tunniLine
import org.openrndr.extra.shapes.tunni.withTunniLine
import org.openrndr.shape.Circle
import kotlin.math.cos

/**
 * Demonstrates how to use `adjustContour` in combination of
 * `ContourAdjusterEdge.withTunniLine()`.
 *
 * Tunni lines are a concept devised by Eduardo Tunni and Fontlab Ltd.,
 * described at https://github.com/OliverLeenders/Tunni-Lines
 *
 * This program creates a circular contour `c` and renders it in pink for reference.
 *
 * Then uses `adjustContour` to alter the 4 edges of that contour, shifting their
 * control points outwards and inwards along the normal using `withTunniLine()`
 * and the cosine of the current time in seconds, then renders the resulting deformed contour.
 */
fun main() = application {
    program {
        extend {
            val c = Circle(drawer.bounds.center, 200.0).contour
            val c2 = adjustContour(c) {
                selectEdges { it -> true }
                for (e in edges) {
                    val tl = e.tunniLine
                    e.withTunniLine(tl.position(0.5) + tl.normal * cos(seconds) * 200.0)
                }
            }
            for (s in c2.segments) {
                drawer.stroke = ColorRGBa.WHITE
                drawer.lineSegment(s.tunniLine)
            }
            drawer.contour(c2)

            drawer.fill = null
            drawer.stroke = ColorRGBa.PINK.opacify(0.5)
            drawer.contour(c)
        }
    }
}
