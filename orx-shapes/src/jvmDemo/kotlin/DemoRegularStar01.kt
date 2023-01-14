import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.regularStar
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    program {
        extend {
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.WHITE
            val radius0 = cos(seconds + 2) * 50.0 + 130.0
            val radius1 = sin(seconds + 2) * 50.0 + 130.0

            val star = regularStar(5, radius0, radius1)

            drawer.translate(width / 2.0, height / 2.0)
            drawer.rotate(seconds * 45.0)
            drawer.contour(star)
        }
    }
}