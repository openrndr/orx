import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.valueQuintic3D

/**
 * Demonstrates how to render grayscale noise patterns dynamically using 3D quintic noise.
 *
 * The program draws one point per pixel on the screen, calculating the color intensity of
 * each point based on a 3D quintic noise function. The noise value is influenced by the
 * pixel's 2D coordinates and animated over time.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        extend {
            drawer.points {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val c = valueQuintic3D(100, (x + y) * 0.04, (x - y) * 0.04, seconds * 0.1) * 0.5 + 0.5
                        fill = ColorRGBa(c, c, c, 1.0)
                        point(x.toDouble(), y.toDouble())
                    }
                }
            }
        }
    }
}
