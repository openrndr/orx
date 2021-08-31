import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.shape.Circle
import org.openrndr.shape.ShapeContour
import org.openrndr.extra.shapes.drawers.bezierPatch
import org.openrndr.extra.shapes.drawers.bezierPatches

/**
 * Shows how to create a [bezierPatch] out of a
 * closed [ShapeContour] with 4 curved segments.
 *
 * Calling [Circle.contour] is one way of producing
 * such a contour with vertices at the cardinal points
 * but one can manually create any other 4-segment closed contour
 * to use in bezier patches.
 */
fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            val c0 = Circle(width / 3.0, height / 2.0, 150.0).contour
            val bp0 = bezierPatch(c0)

            val c1 = Circle(2.0*width / 3.0, height / 2.0, 150.0).contour
            val bp1 = bezierPatch(c1)


            extend {
                drawer.bezierPatches(listOf(bp0, bp1))
            }
        }
    }
}