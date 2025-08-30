import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.noise.gradientPerturbFractal
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector2
import kotlin.math.absoluteValue

/**
 * Demonstrates how to generate a dynamic fractal-based visual effect
 * using 2D gradient perturbation and simplex noise.
 *
 * This method initializes a color buffer to create an image and applies fractal gradient noise to set
 * each pixel's brightness, producing a dynamic visual texture. The fractal effect is achieved by layering multiple
 * levels of noise, and each pixel's color intensity is based on the noise function results.
 * The output is continuously updated to produce animated patterns.
 *
 * CPU-based.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        val cb = colorBuffer(width, height)
        val shad = cb.shadow
        extend {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val p =
                        gradientPerturbFractal(300, frequency = 0.8, position = Vector2(seconds + x / 320.0, y / 240.0))
                    val d = simplex(300, p.x, p.y + seconds, seconds).absoluteValue
                    shad[x, y] = ColorRGBa(d, d, d, 1.0)
                }
            }
            shad.upload()
            drawer.image(cb)
        }
    }
}