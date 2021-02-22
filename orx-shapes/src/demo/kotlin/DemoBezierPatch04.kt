import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.shape.Circle

/**
 * Shows how to get positions and gradient values of those positions
 * from a [bezierPatch]
 *
 * You can think of bezierPatch.position() as requesting points
 * in a wavy flag (the bezier patch) using normalized uv coordinates.
 */
fun main() {
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

            val bp = bezierPatch(
                Circle(drawer.bounds.center, 350.0).contour
                //Rectangle.fromCenter(drawer.bounds.center, 550.0).contour
            )

            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.stroke = ColorRGBa.BLACK

                for (j in 1 until 50 step 2) {
                    for (i in 1 until 50 step 2) {
                        val u = i / 50.0
                        val v = j / 50.0
                        val pos = bp.position(u, v)
                        val grad = bp.gradient(u, v).normalized * 10.0
                        val perpendicular = grad.perpendicular()
                        drawer.lineSegment(pos - grad, pos + grad)
                        drawer.lineSegment(pos - perpendicular, pos + perpendicular)
                        //drawer.circle(pos + grad, 3.0)
                    }
                }
            }
        }
    }
}