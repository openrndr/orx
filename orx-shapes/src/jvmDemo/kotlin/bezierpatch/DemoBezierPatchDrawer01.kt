package bezierpatch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.bezierpatches.bezierPatch
import org.openrndr.shape.Circle

/**
 * Demonstrates how to draw a bezier patch and its corresponding contours.
 * The bezier patch is generated from a circular shape and is assigned colors
 * for each control point. The patch is subdivided into horizontal and vertical
 * contours, which are rendered to visualize the structure of the bezier patch.
 *
 * The bezier patch constructor expects a contour with 4 segments, for example
 * a rectangular contour or a circle, which in OPENRNDR is made out of 4 segments.
 */
fun main() = application {
    program {
        extend {
            drawer.clear(ColorRGBa.PINK)
            val bp = bezierPatch(
                Circle(width / 2.0, height / 2.0, 200.0).contour
            ).withColors(
                listOf(
                    listOf(ColorRGBa.PINK, ColorRGBa.RED, ColorRGBa.BLACK, ColorRGBa.BLUE),
                    listOf(ColorRGBa.RED, ColorRGBa.BLACK, ColorRGBa.BLUE, ColorRGBa.GREEN),
                    listOf(ColorRGBa.PINK, ColorRGBa.RED, ColorRGBa.WHITE, ColorRGBa.GREEN),
                    listOf(ColorRGBa.BLACK, ColorRGBa.WHITE, ColorRGBa.BLACK, ColorRGBa.BLUE),
                )
            )

            drawer.bezierPatch(bp)

            drawer.fill = null
            drawer.contour(bp.contour)
            for (i in 0 until 10) {
                drawer.contour(bp.horizontal(i / 9.0))
            }
            for (i in 0 until 10) {
                drawer.contour(bp.vertical(i / 9.0))
            }
        }
    }
}
