import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extra.noise.gradient
import org.openrndr.extra.noise.simplex3D
import org.openrndr.extra.noise.withVector2Output

/**
 * Demonstrates how to chain methods behind noise functions like `simplex3D` to
 * alter its output. By default `simplex3D` produces one double value, but
 * by calling `.withVector2Output()` it produces `Vector2` instances instead.
 *
 * The `.gradient()` method alters the output to return the direction of fastest
 * increase. Read more in [WikiPedia](https://en.wikipedia.org/wiki/Gradient).
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val n = simplex3D.withVector2Output().gradient()
        extend {
            drawer.stroke = null
            drawer.fill = ColorRGBa.PINK
            drawer.lineJoin = LineJoin.ROUND
            drawer.stroke = ColorRGBa.WHITE
            for (y in 0 until height step 20) {
                for (x in 0 until width step 20) {
                    val d = n(40, x * 0.003, y * 0.003,seconds) * 5.0
                    drawer.lineSegment(x * 1.0, y * 1.0, x * 1.0 + d.x, y * 1.0 + d.y)
                }
            }
        }
    }
}