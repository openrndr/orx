import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.shape.Circle
import org.openrndr.shape.ShapeContour

/**
 * Shows how to create a [bezierPatch] out of a
 * closed [ShapeContour] with 4 curved segments.
 *
 * Calling [Circle.contour] is one way of producing
 * such a contour with vertices at the cardinal points
 * but one can manually create any other 4-segment closed contour
 * to use in bezier patches.
 */
suspend fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            val c = Circle(width / 2.0, height / 2.0, 350.0).contour
            val bp = bezierPatch(c)

            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.stroke = ColorRGBa.BLACK

                for (i in 0..10) {
                    drawer.contour(bp.horizontal(i / 10.0))
                    drawer.contour(bp.vertical(i / 10.0))
                }
            }
        }
    }
}