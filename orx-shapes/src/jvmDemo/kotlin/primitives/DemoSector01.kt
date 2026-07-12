package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.ColorHSLUVa
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.shapes.primitives.Sector

/**
 * Demonstrates creating and animating `Sector` shapes (pie/pizza slices) that form
 * a complete circle.
 *
 * Uses simplex noise to generate smoothly varying random values that control each sector's size.
 * The sector sizes continuously change while always adding up to 360 degrees (a full circle).
 * The entire composition slowly rotates over time.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            drawer.clear(ColorRGBa.PINK)

            // Generate 8 smoothly animating values using simplex noise
            val weights = List(8) { simplex(1234, seconds * 0.2 + it) + 1.1 }
            val sumOfWeights = weights.sum()

            // The starting angle slowly rotates the entire composition
            var theta = seconds * 5.0

            weights.forEach { weight ->
                // Normalize each weight so all sectors together form a complete 360° circle
                val delta = 360.0 * weight / sumOfWeights
                val s = Sector(
                    drawer.bounds.center,
                    50.0 + delta * 3.5, // Radius varies based on sector angular delta
                    theta,
                    theta + delta
                )
                drawer.fill = ColorHSLUVa(theta, 0.8, 0.6).toRGBa()
                drawer.contour(s.contour)

                // Move to the next sector's starting angle
                theta += delta
            }
        }
    }
}
