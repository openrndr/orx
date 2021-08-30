import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.RoundedRectangle
import kotlin.math.cos

fun main() = application {
    program {
        // -- this block is for automation purposes only
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }
        extend {
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = ColorRGBa.PINK
            val radius = cos(seconds) * 20.0 + 20.0
            val rectangle = RoundedRectangle(50.0, 50.0, width - 100.0, height - 100.0, radius)
            drawer.contour(rectangle.contour)
        }
    }
}