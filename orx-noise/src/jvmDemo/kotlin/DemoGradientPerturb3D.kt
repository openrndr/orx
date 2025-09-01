import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.noise.gradientPerturbFractal
import org.openrndr.extra.noise.simplex
import org.openrndr.math.Vector3
import kotlin.math.absoluteValue

/**
 * Demonstrates how to generate a dynamically evolving visual
 * representation of fractal noise. The program uses 3D gradient perturbation and simplex noise
 * to produce a grayscale gradient on a color buffer.
 *
 * The visual output is created by iteratively computing the fractal gradient perturbation and simplex
 * noise for each pixel in the color buffer, applying a perturbation based on time, and rendering the
 * result as an image.
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
                        gradientPerturbFractal(300, frequency = 0.8, position = Vector3(x / 320.0, y / 240.0, seconds))
                    val d = simplex(300, p.x, p.y, p.z).absoluteValue
                    shad[x, y] = ColorRGBa(d, d, d, 1.0)
                }
            }
            shad.upload()
            drawer.image(cb)
        }
    }
}
