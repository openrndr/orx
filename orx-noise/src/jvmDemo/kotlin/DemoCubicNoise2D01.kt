import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.cubicHermite3D

/**
 * Demonstrates how to render dynamic grayscale patterns using 3D cubic Hermite interpolation.
 * The program draws one point per pixel on the screen, calculating the color intensity of each point
 * based on a 3D cubic Hermite noise function.
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
                        val c = cubicHermite3D(100, (x + y) * 0.04, (x - y) * 0.04, seconds * 1.0) * 0.5 + 0.5
                        fill = ColorRGBa(c, c, c, 1.0)
                        point(x.toDouble(), y.toDouble())
                    }
                }
            }
        }
    }
}
