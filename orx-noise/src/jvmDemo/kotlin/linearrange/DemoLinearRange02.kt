package linearrange

import org.openrndr.application
import org.openrndr.color.ColorLCHUVa
import org.openrndr.extra.math.linearrange.rangeTo
import org.openrndr.extra.noise.linearrange.hash
import org.openrndr.shape.Circle

/**
 * Demonstrates how to create a linear range with two [org.openrndr.shape.Circle]s.
 *
 * This range is then sampled at 100 random locations using the `hash` method to get and render interpolated
 * circles. The random seed changes once per second.
 *
 * Colors are calculated based on the index of each circle.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val circle1 = Circle(drawer.bounds.position(0.3, 0.3), 50.0)
            val circle2 = Circle(drawer.bounds.position(0.7, 0.7), 200.0)
            val range = circle1..circle2
            extend {
                for (i in 0 until 100) {
                    drawer.fill = ColorLCHUVa(i * 1.0, i * 1.0, i * 30.0).toRGBa().opacify(0.6)
                    drawer.circle(range.hash(seconds.toInt(), i))
                }
            }
        }
    }
}