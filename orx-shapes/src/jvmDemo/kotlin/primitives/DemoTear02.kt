package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.primitives.Tear
import org.openrndr.shape.Circle

/**
 * Demonstrates the use of `Tear()` to create drop-like shapes out of a Vector2 point and a Circle.
 *
 * The tear locations are calculated using the `Rectangle.scatter()` function. Locations near the
 * center of the window are filtered out.
 *
 * The radii of each tear is randomly chosen between three values. The orientation of each tear
 * is calculated by getting the normalized difference between the tear and the center of the window,
 * making them look as being emitted at the center of the window.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val points = drawer.bounds.scatter(40.0, distanceToEdge = 80.0).filter {
            it.distanceTo(drawer.bounds.center) > 80.0
        }

        val tears = points.map {
            val radius = listOf(5.0, 10.0, 20.0).random()
            val offset = (it - drawer.bounds.center).normalized * radius
            Tear(it - offset, Circle(it + offset, radius))
        }

        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.BLACK
            drawer.contours(tears.map { it.contour })
        }
    }
}
