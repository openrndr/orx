import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.shape.LineSegment
import org.openrndr.shape.ShapeContour

/**
 * Shows how to
 * - create a [bezierPatch] out of 4 [LineSegment]
 * - create a sub-patch out of a [bezierPatch]
 * - create horizontal and vertical [ShapeContour]s out of [bezierPatch]es
 *
 * The created contours are horizontal and vertical in "bezier-patch space" but
 * are rendered deformed following the shape of the bezier patch.
 */
fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            // helper to get screen locations using normalized uv values
            fun pos(u: Double, v: Double) = drawer.bounds.position(u, v)
            val c0 = LineSegment(pos(0.1, 0.1), pos(0.9, 0.1))
            val c1 = LineSegment(pos(0.4, 0.3), pos(0.6, 0.4))
            val c2 = LineSegment(pos(0.4, 0.7), pos(0.6, 0.6))
            val c3 = LineSegment(pos(0.1, 0.9), pos(0.9, 0.9))

            val bp = bezierPatch(c0.segment, c1.segment, c2.segment, c3.segment)
            val bpSub = bp.sub(0.1, 0.1, 0.6,0.6)

            extend {
                drawer.clear(ColorRGBa.PINK)

                // Show the line segments that form the bezier patch
                drawer.stroke = ColorRGBa.YELLOW
                drawer.strokeWeight = 5.0
                drawer.lineSegments(listOf(c0, c1, c2, c3))

                drawer.strokeWeight = 1.0
                for (i in 0..50) {
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.contour(bp.horizontal(i / 50.0))
                    drawer.contour(bp.vertical(i / 50.0))

                    drawer.stroke = ColorRGBa.RED
                    drawer.contour(bpSub.horizontal(i / 50.0))
                    drawer.contour(bpSub.vertical(i / 50.0))
                }
            }
        }
    }
}