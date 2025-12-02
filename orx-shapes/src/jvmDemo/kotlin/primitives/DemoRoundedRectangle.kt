package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.RoundedRectangle
import kotlin.math.cos

/**
 * Demonstrates the use of `RoundedRectangle()` to create a rectangle in which the corners
 * are rounded by the provided `radius`
 *
 * The radius is animated between 0.0 and 40.0 using the cosine of the current time in seconds.
 */
fun main() = application {
    program {
        extend {
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = ColorRGBa.PINK
            val radius = cos(seconds) * 20.0 + 20.0
            val rectangle = RoundedRectangle(50.0, 50.0, width - 100.0, height - 100.0, radius)
            drawer.contour(rectangle.contour)
        }
    }
}