import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.shape.Circle

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
            extend {
                drawer.clear(ColorRGBa.PINK)
                val bp = bezierPatch(Circle(width / 2.0, height / 2.0, 350.0).contour)

                for (i in 0..50) {
                    drawer.stroke = ColorRGBa.BLACK.opacify(1.0)
                }

                for (j in 1 until 50 step 2) {
                    for (i in 1 until 50 step 2) {
                        val p = bp.position(i / 50.0, j / 50.0)
                        val g2 = bp.gradient(i / 50.0, j / 50.0).normalized
                        val g = g2.perpendicular()
                        drawer.lineSegment(p, p + g2 * 10.0)
                        drawer.lineSegment(p, p - g2 * 10.0)
                        drawer.lineSegment(p, p + g * 10.0)
                        drawer.lineSegment(p, p - g * 10.0)
                    }
                }
            }
        }
    }
}