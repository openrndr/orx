package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.presets.DARK_CYAN
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.extra.shapes.adjust.extensions.averageTangents
import org.openrndr.extra.shapes.adjust.extensions.switchTangents
import org.openrndr.extra.shapes.tunni.tunniLine
import org.openrndr.extra.shapes.tunni.tunniPoint
import kotlin.math.sqrt

/**
 * Demonstrates how to manipulate a contour by adjusting and transforming its vertices
 * and edges, and subsequently visualizing the result using different drawing styles.
 *
 * The program creates a rectangular contour derived by shrinking the bounds of the drawing area.
 * It then applies multiple transformations to selected vertices. These transformations include:
 *
 * - Averaging tangents for selected vertices
 * - Scaling and rotating vertex positions based on the horizontal mouse position
 * - Switching tangents for specific vertices
 *
 * The resulting contour is drawn in black. Additionally:
 *
 * - Control line segments are visualized in red, connecting segment endpoints to control points.
 * - Vertices are numbered and highlighted with black-filled circles.
 * - Tunni lines, which represent optimized control line placements, are visualized in cyan.
 * - Tunni points, marking the Tunni line's control, are emphasized with yellow-filled circles.
 *
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.fontMap = font

            var contour = drawer.bounds.offsetEdges(-200.0).contour

            drawer.fill = null

            contour = adjustContour(contour) {
                selectVertices(0, 1)
                for (v in vertices) {
                    v.averageTangents()
                    v.scale(sqrt(2.0))
                    v.rotate(mouse.position.x - 45.0)
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

            // Draw points and numbers
            drawer.fill = ColorRGBa.BLACK
            drawer.stroke = null
            contour.segments.forEachIndexed { i, it ->
                drawer.text(i.toString(), it.start + 10.0)
                drawer.circle(it.start, 5.0)
            }

            drawer.fill = ColorRGBa.YELLOW
            drawer.stroke = ColorRGBa.CYAN
            for (s in contour.segments) {
                drawer.strokeWeight = 3.0
                drawer.lineSegment(s.tunniLine)
                drawer.strokeWeight = 1.0
                drawer.circle(s.tunniPoint, 5.0)
            }
        }
    }
}
