package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.invert
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

/**
 * Demonstrates how to use a Circle's `invert()` method, to map a point
 * (in this case, the mouse position) to another point along the same
 * ray from the center, but at a distance that is inversely proportional
 * to the original distance.
 *
 * If the distance from the center of the circle to the point being inverted
 * is zero, an `IllegalArgumentException` is thrown. Since the mouse position
 * is rounded to whole numbers, we ensure that exception will not happen by slightly
 * offsetting the center of the circle.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val c = Circle(drawer.bounds.center + Vector2(1E-2, 1E-2), 100.0)
        extend {
            val invertedPos = c.invert(mouse.position)

            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.circle(c)
            drawer.circle(invertedPos,10.0)
        }
    }
}