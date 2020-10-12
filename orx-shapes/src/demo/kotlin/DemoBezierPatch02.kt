import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.math.bezier
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.drawComposition

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
                val c = Circle(width/2.0, height/2.0, 350.0).contour
                val bp = bezierPatch(c)

                for (i in 0..10) {
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.contour(bp.horizontal(i/10.0))
                    drawer.contour(bp.vertical(i/10.0))

                }
            }
        }
    }
}