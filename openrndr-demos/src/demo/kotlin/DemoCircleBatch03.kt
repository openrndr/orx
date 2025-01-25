import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.hsv
import org.openrndr.extra.noise.simplex
import kotlin.math.abs

/*
This program demonstrates dynamic circle batches
 */

fun main() = application {
    program {
        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.circles {
                this.fill = ColorRGBa.PINK
                for (i in 0 until 10000) {
                    val hue = simplex(i * 403, i * 149.0 + 0.2 * seconds) * 180.0 + 180.0
                    fill = hsv(hue, 0.5, 0.3).toRGBa()
                    stroke = hsv(hue + 180.0, 0.5, 1.0).toRGBa()
                    val x = simplex(i * 337, i * 43.0 + 0.1 * seconds) * width / 2.0 + width / 2.0
                    val y = simplex(i * 439, i * 39.0 - 0.1 * seconds) * height / 2.0 + height / 2.0
                    val radius = simplex(i * 139, i * 51.0 + seconds * 0.43) * 20.0 + 20.0
                    strokeWeight = abs(simplex(i * 139, i * 51.0 + seconds * 0.43) * radius * 0.5)
                    circle(x, y, radius)
                }
            }
        }
    }
}