import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.regularPolygon
import org.openrndr.math.map
import kotlin.math.cos

suspend fun main() = application {
    program {
        // -- this block is for automation purposes only
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        extend {
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.WHITE

            for (sides in 0 until 8) {
                val radius0 = cos(seconds + sides) * 20.0 + 40.0
                val star = regularPolygon(sides + 3, radius = radius0)

                drawer.isolated {
                    translate(
                            (sides % 4.0).map(0.0, 3.0,
                                    width * 0.2, width * 0.8),
                            (sides / 4).toDouble().map(0.0, 1.0,
                                    height * 0.3, height * 0.7))
                    rotate(seconds * 45.0)
                    contour(star)
                }
            }
        }
    }
}