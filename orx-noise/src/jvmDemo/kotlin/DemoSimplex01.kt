import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extra.noise.simplex
import org.openrndr.shape.contour

/**
 * Demonstrates how to use the `simplex` method to obtain noise values based on a seed and an x value.
 *
 * The program creates 20 horizontal contours with 40 steps each in which each 2D step and each 2D control point
 * is affected by noise.
 *
 * Time is used as a noise argument to produce an animated effect.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            drawer.stroke = null
            drawer.fill = ColorRGBa.PINK
            drawer.lineJoin = LineJoin.ROUND
            for (y in 0..height step 20) {
                val c = contour {
                    moveTo(0.0, 0.0)
                    for (x in 0..width step 40) {
                        val cx = simplex(y, x * 0.1 + seconds) * 10.0 + x
                        val cy = simplex(y + 3000, x * 0.1 + seconds) * 20.0
                        val px = simplex(y + 8000, x * 0.1 + seconds) * 10.0 + x
                        val py = simplex(y + 6000, x * 0.1 + seconds) * 20.0
                        continueTo(cx, cy, px, py)
                    }
                }
                val points = c.equidistantPositions(50)
                drawer.circles(points, 10.0)
                drawer.translate(0.0, 20.0)
            }
        }
    }
}