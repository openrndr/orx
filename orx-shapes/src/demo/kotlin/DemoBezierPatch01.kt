import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.shape.LineSegment

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
                val c0 = LineSegment(200.0, 100.0, width-200.0, 100.0).contour.segments.first()
                val c1 = LineSegment(100.0, 150.0, width-100.0, 150.0).contour.segments.first()
                val c2 = LineSegment(100.0, height-150.0, width-100.0, height-150.0).contour.segments.first()
                val c3 = LineSegment(200.0, height-100.0, width-200.0, height-100.0).contour.segments.first()


                val bp = bezierPatch(c0, c1, c2, c3)
                val bpsub = bp.sub(0.0, 0.0, 0.5, 0.5)
                for (i in 0..50) {
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.contour(bp.horizontal(i/50.0))
                    drawer.contour(bp.vertical(i/50.0))
                    drawer.stroke = ColorRGBa.RED
                    drawer.contour(bpsub.horizontal(i/50.0))
                    drawer.contour(bpsub.vertical(i/50.0))
                }
            }
        }
    }
}