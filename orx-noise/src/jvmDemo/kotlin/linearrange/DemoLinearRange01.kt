package linearrange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.linearrange.uniform
import org.openrndr.extra.math.linearrange.rangeTo
import kotlin.random.Random

/**
 * Demonstrates how to create a linear range with two [org.openrndr.shape.Rectangle]s.
 *
 * This range is then sampled at 100 random locations using the `uniform` method to get and render interpolated
 * rectangles. The random seed changes once per second.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val rect1 = drawer.bounds.offsetEdges(-300.0, -50.0)
            val rect2 = drawer.bounds.offsetEdges(-50.0, -300.0)
            val range =  rect1 .. rect2
            extend {
                drawer.fill = ColorRGBa.WHITE.opacify(0.9)
                val r = Random(seconds.toInt())
                for (i in 0 until 100) {
                    drawer.rectangle(range.uniform(r))
                }
            }
        }
    }
}