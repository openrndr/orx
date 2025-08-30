package bezierpatch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.extra.shapes.bezierpatches.bezierPatch
import org.openrndr.math.Vector3
import kotlin.random.Random

/**
 * Demonstrates how to create and render a bezier patch with randomized control points
 * and colors. The bezier patch is derived from a scaled-down copy of the
 * drawer bounds, converted to a contour and deformed using `adjustContour`.
 *
 * The bezier patch uses 16 randomly generated colors chunked into 4 lists with 4 colors each.
 *
 */
fun main() = application {
    program {
        val r = Random(1213)
        val bp = bezierPatch(
            adjustContour(drawer.bounds.offsetEdges(-50.0).contour) {
                vertices.forEach {
                    it.rotate(Double.uniform(10.0, 30.0, r))
                }
            }
        ).withColors(
            List(16) {
                ColorRGBa.fromVector(Vector3.uniform(0.0, 1.0, r))
            }.chunked(4)
        )

        extend {
            drawer.clear(ColorRGBa.PINK)

            // Render the colored patch
            drawer.bezierPatch(bp)

            // Render horizontal and vertical lines in the patch
            drawer.stroke = ColorRGBa.BLACK.opacify(0.3)
            for (i in 0 until 20) {
                drawer.contour(bp.horizontal(i / 19.0))
            }
            for (i in 0 until 10) {
                drawer.contour(bp.vertical(i / 9.0))
            }

            // Render the contour of the patch
            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.strokeWeight = 3.0
            drawer.contour(bp.contour)
        }
    }
}
