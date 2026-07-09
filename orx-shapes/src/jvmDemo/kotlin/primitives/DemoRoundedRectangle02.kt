package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.RoundedRectangle
import kotlin.math.cos

/**
 * Demonstrates the use of `RoundedRectangle()` to create a rectangle in which the corners
 * are rounded by four changing `radii`
 *
 * The radii are animated between 0.0 and 100.0 using the cosine of the current time in seconds
 * and the corner index, demonstrating that corners can have differing radii.
 */
fun main() = application {
    program {
        extend {
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = ColorRGBa.PINK
            val radii = List(4) {
                cos(seconds + it) * 100.0 + 100.0
            }
            val rectangle = RoundedRectangle(50.0, 50.0, width - 100.0, height - 100.0, radii)
            drawer.contour(rectangle.contour)
        }
    }
}