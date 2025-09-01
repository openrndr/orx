import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.scatter
import org.openrndr.math.Vector2
import org.openrndr.shape.Ellipse
import kotlin.math.cos
import kotlin.random.Random

/**
 * Demonstrates how to create an animated visualization of scattered points.
 *
 * The program creates an animated ellipse with increasing and decreasing height.
 * Then, scatters points inside it with a placementRadius of 20.0.
 *
 * The animation reveals that the scattering positions are somewhat stable between
 * animation frames.
 *
 * The ellipse's contour is revealed and hidden every other second.
 */
fun main() = application {
    configure {
        width = 720
        height = 540
    }
    program {
        extend {
            val shape = Ellipse(Vector2(width / 2.0, height / 2.0), 200.0, 150.0 + cos(seconds) * 125.0).shape
            val points = shape.scatter(20.0, random = Random(0))
            drawer.clear(ColorRGBa.BLACK)
            drawer.stroke = null
            drawer.fill = ColorRGBa.PINK
            drawer.circles(points, 4.0)

            if (seconds.mod(2.0) < 1.0) {
                drawer.stroke = ColorRGBa.PINK
                drawer.fill = null
                drawer.shape(shape)
            }
        }
    }
}
