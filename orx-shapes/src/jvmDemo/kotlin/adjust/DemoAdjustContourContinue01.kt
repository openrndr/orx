package adjust

import kotlinx.coroutines.yield
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContourSequence
import org.openrndr.extra.shapes.tunni.tunniLine
import org.openrndr.extra.shapes.tunni.tunniPoint
import org.openrndr.extra.shapes.tunni.withTunniLine
import org.openrndr.launch
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.math.cos

/**
 * Demonstrates how to adjust and animate contour segments and vertices.
 *
 * The method initially creates a contour by offsetting the edges of the window's bounds. A process is
 * defined to sequence through various transformations on the contour, such as selecting edges, selecting
 * vertices, rotating points, or modifying segment attributes based on mathematical transformations.
 *
 * The adjusted contour and its modified segments and vertices are iterated through a sequence
 * and updated in real time. Rendering involves visualizing the contour, its control points, the
 * Tunni lines, Tunni points, as well as the selected segments and points with distinct styles
 * for better visualization.
 *
 * The complex animation sequence is implemented using coroutines. Two loops in the code alternate
 * between rotating vertices and adjusting Tunni lines while the `extend` function takes care of
 * rendering the composition in its current state.
 *
 * The core elements to study to in this demo are `adjustContourSequence` and `launch`.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {

        var res = drawer.bounds.offsetEdges(-200.0).contour
        var selectedSegments = emptyList<Segment2D>()
        var selectedPoints = emptyList<Vector2>()

        val contourSeq = adjustContourSequence(res) {

            sequence {
                for (i in 0 until 1000) {
                    selectEdges()
                    selectVertices((i * 7).mod(4))
                    for (v in vertices) {
                        for (j in 0 until 30) {
                            v.rotate(45.0 / 30.0)
                            yield(status)
                        }
                    }

                    selectVertices((i * 3).mod(4))
                    for (v in vertices) {
                        yield(status)
                    }


                    selectVertices()
                    selectEdges(i.mod(4))
                    for (j in 0 until 30) {
                        for (e in edges) {
                            e.withTunniLine(
                                e.tunniLine.position(0.5) +
                                        e.tunniLine.normal * cos(i.toDouble() + e.segmentIndex()) * 50.0 / 30.0
                            )
                            yield(status)
                        }
                    }
                }
            }
        }

        launch {
            for (i in contourSeq) {
                res = i.contour
                selectedPoints = i.selectedPoints
                selectedSegments = i.selectedSegments
                yield()
            }
        }


        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.stroke = ColorRGBa.BLACK
            drawer.contour(res)

            drawer.stroke = ColorRGBa.RED

            for (s in res.segments) {
                drawer.lineSegment(s.start, s.cubic.control[0])
                drawer.lineSegment(s.end, s.cubic.control[1])
            }
            drawer.fill = ColorRGBa.BLACK
            drawer.stroke = null
            drawer.circles(res.segments.map { it.start }, 5.0)

            drawer.stroke = ColorRGBa.GRAY
            for (s in res.segments) {
                drawer.lineSegment(s.tunniLine)
                drawer.fill = ColorRGBa.CYAN
                drawer.circle(s.tunniPoint, 5.0)
            }

            drawer.stroke = ColorRGBa.MAGENTA
            drawer.strokeWeight = 3.0
            for (s in selectedSegments) {
                drawer.segment(s)
            }
            for (p in selectedPoints) {
                drawer.circle(p, 5.0)
            }
        }
    }
}
