package linearrange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.math.linearrange.rangeTo
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates how to create a `LinearRange2D` out of two `LinearRange1D` instances.
 * The first range interpolates a horizontal rectangle into a vertical one.
 * The second range interpolates two smaller squares of equal size, one placed
 * higher along the y-axis and another one lower.
 *
 * A grid of such rectangles is displayed, animating the `u` and `v` parameters based on
 * `seconds`, `x` and `y` indices. The second range results in a vertical wave effect.
 *
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val range0 = Rectangle.fromCenter(Vector2(36.0, 36.0), 72.0, 18.0)..
                    Rectangle.fromCenter(Vector2(36.0, 36.0), 18.0, 72.0)
            val range1 = Rectangle.fromCenter(Vector2(36.0, 0.0), 9.0, 9.0)..
                    Rectangle.fromCenter(Vector2(36.0, 72.0), 9.0, 9.0)

            val range = range0..range1
            extend {
                drawer.fill = ColorRGBa.PINK.opacify(0.9)
                drawer.stroke = null
                for (y in 0 until height step 72) {
                    for (x in 0 until width step 72) {
                        val u = cos(seconds* 2.0 + x * 0.01) * 0.5 + 0.5
                        val v = sin(seconds * 1.03 + y * 0.01) * 0.5 + 0.5
                        drawer.isolated {
                            drawer.translate(x.toDouble(), y.toDouble())
                            drawer.rectangle(range.value(u, v))
                        }
                    }
                }
            }
        }
    }
}