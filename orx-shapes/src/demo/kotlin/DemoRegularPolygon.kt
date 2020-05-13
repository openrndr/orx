import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.RoundedRectangle
import org.openrndr.extra.shapes.regularPolygon
import org.openrndr.extra.shapes.regularStar
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
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
            val radius0 = cos(seconds) * 50.0 + 130.0
            val star = regularPolygon(6, radius = radius0)

            drawer.translate(width/2.0, height / 2.0)
            drawer.rotate(seconds * 45.0)
            drawer.contour(star)
        }
    }
}