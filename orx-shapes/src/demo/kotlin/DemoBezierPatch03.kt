import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.Screenshots
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.extra.shapes.distort
import org.openrndr.extra.shapes.regularStarRounded
import org.openrndr.math.transforms.transform
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
            extend(Screenshots())
            extend {
                drawer.clear(ColorRGBa.PINK)
                val bp = bezierPatch(Circle(width / 2.0, height / 2.0, 350.0).contour)

                for (i in 0..50) {
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.contour(bp.horizontal(i / 50.0))
                    drawer.contour(bp.vertical(i / 50.0))
                }
                drawer.fill = ColorRGBa.PINK
                for (j in 1 until 10) {
                    for (i in 1 until 10) {
                        val r = regularStarRounded(7, 30.0, 40.0, 0.5, 0.5).transform(
                                transform {
                                    translate(j * width / 10.0, i * height / 10.0)
                                }
                        )
                        val dr = bp.distort(r, drawer.bounds)
                        drawer.contour(dr)
                    }
                }
            }
        }
    }
}