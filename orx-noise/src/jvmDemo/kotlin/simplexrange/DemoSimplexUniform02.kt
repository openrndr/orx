package simplexrange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.math.simplexrange.SimplexRange2D
import org.openrndr.extra.math.simplexrange.SimplexRange3D
import org.openrndr.extra.noise.scatter
import kotlin.random.Random
import org.openrndr.extra.noise.simplexrange.uniformSimplex
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.math.floor

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
                val positions = (0 until 3).map { Polar((30.0 + it * 120.0), 180.0).cartesian }
                val colors = listOf(ColorRGBa.PINK, ColorRGBa.RED, ColorRGBa.BLUE).map { it.toLABa() }

                val positionsr = SimplexRange2D(positions[0], positions[1], positions[2])
                val colorsr = SimplexRange2D(colors[0], colors[1], colors[2])
                val freq = 1.0

                drawer.stroke = null


                val r = Random((seconds * freq).toInt())
                val points = Rectangle(0.0, 0.0, 360.0, 360.0).scatter(10.0, random = r)

                drawer.circles(points, 2.0)

                val power = cos(floor(seconds*freq)/freq) *16.0 + 16.0
                drawer.isolated {
                    drawer.translate(drawer.bounds.position(0.75,0.25) + Vector2(0.0, 40.0))

                    for (point in points) {
                        drawer.fill = colorsr.value(point.x/360.0, point.y/360.0).toRGBa()
                        drawer.circle(positionsr.value(point.x/360.0, point.y/360.0), 2.0)
                    }
                }

            }
        }
    }
}