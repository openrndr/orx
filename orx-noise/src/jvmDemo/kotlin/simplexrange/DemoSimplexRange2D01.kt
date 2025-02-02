package simplexrange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.math.linearrange.rangeTo
import org.openrndr.extra.math.simplexrange.SimplexRange3D
import kotlin.random.Random
import org.openrndr.extra.noise.simplexrange.uniform
import org.openrndr.extra.noise.linearrange.*

/**
 * This demo creates a dynamic graphical output utilizing simplex and
 * linear interpolation-based color ranges.
 *
 * Functionalities:
 * - Defines a list of base colors converted to LAB color space for smooth interpolation.
 * - Constructs a 3D simplex range and a 2D linear range for color sampling.
 * - Randomly populates two sections of the screen with rectangles filled with colors
 *   sampled from simplex and linear ranges respectively.
 * - Draws a vertical divider line in the middle of the application window.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                val colors = listOf(ColorRGBa.BLACK, ColorRGBa.RED, ColorRGBa.GREEN, ColorRGBa.BLUE).map { it.toLABa() }
                drawer.stroke = null
                val sr = SimplexRange3D(colors[0], colors[1], colors[2], colors[3])
                val lr = (colors[0]..colors[1])..(colors[2]..colors[3])

                val r = Random((seconds * 2).toInt())

                // Draw the simplex sampling on the left
                drawer.rectangles {
                    for (y in 0 until 40) {
                        for (x in 0 until 20) {
                            fill = sr.uniform(r).toRGBa()
                            rectangle(x * width / 40.0, y * height / 40.0, width / 40.0, height / 40.0)
                        }
                    }
                }

                // Draw the bilinear sampling on the right
                drawer.rectangles {
                    for (y in 0 until 40) {
                        for (x in 20 until 40) {
                            fill = lr.uniform(r).toRGBa()
                            rectangle(x * width / 40.0, y * height / 40.0, width / 40.0, height / 40.0)
                        }
                    }
                }
                drawer.stroke = ColorRGBa.BLACK
                drawer.lineSegment(drawer.bounds.vertical(0.5))
            }
        }
    }
}