package simplexrange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import kotlin.random.Random
import org.openrndr.extra.noise.simplexrange.uniformSimplex
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
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
                val freq = 1.0

                drawer.stroke = null

                val power = cos(floor(seconds*freq)/freq) *16.0 + 16.0
                drawer.isolated {
                    drawer.translate(drawer.bounds.position(0.25,0.25) + Vector2(0.0, 40.0))
                    val rp = Random((seconds * freq).toInt())
                    val rc = Random((seconds * freq).toInt())
                    for (i in 0 until 32 * 32) {
                        drawer.fill = colors.uniformSimplex(rc, 1, power).toRGBa()
                        drawer.circle(positions.uniformSimplex(rp, 1, power), 2.0)
                    }
                }
                drawer.isolated {
                    drawer.translate(drawer.bounds.position(0.75,0.75) + Vector2(0.0, 40.0))
                    val rp = Random((seconds * freq).toInt())
                    val rc = Random((seconds * freq).toInt())
                    for (i in 0 until 32 * 32) {
                        drawer.fill = colors.uniformSimplex(rc, 2, power).toRGBa()
                        drawer.circle(positions.uniformSimplex(rp, 2, power), 2.0)
                    }
                }


                drawer.isolated {
                    drawer.stroke = null
                    val rc = Random((seconds * freq).toInt())
                    drawer.translate(drawer.bounds.position(0.5,0.0) + Vector2(20.0, 20.0))

                    for (i in 0 until 32 * 32) {
                        val x = i.mod(32)
                        val y = i / 32
                        drawer.fill = colors.uniformSimplex(rc, 1, power).toRGBa()
                        drawer.rectangle(x * 10.0, y * 10.0, 10.0, 10.0)
                    }

                }

                drawer.isolated {
                    drawer.stroke = null
                    val rc = Random((seconds * freq).toInt())

                    drawer.translate(drawer.bounds.position(0.0,0.5) + Vector2(20.0, 20.0))

                    for (i in 0 until 32 * 32) {
                        val x = i.mod(32)
                        val y = i / 32
                        drawer.fill = colors.uniformSimplex(rc, 2, power).toRGBa()
                        drawer.rectangle(x * 10.0, y * 10.0, 10.0, 10.0)
                    }

                }

            }
        }
    }
}